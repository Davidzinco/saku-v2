package com.saku.v2

import com.saku.v2.data.backup.ZipBackupManager
import com.saku.v2.data.model.Account
import com.saku.v2.data.model.AccountType
import com.saku.v2.data.model.AppPreferences
import com.saku.v2.data.model.Category
import com.saku.v2.data.model.DriveSyncMeta
import com.saku.v2.data.model.SYSTEM_CAT_SAVINGS_DEPOSIT
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SavingsGoal
import com.saku.v2.data.model.ThemeMode
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipBackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun sakuAppState_jsonRoundTrip_preservesAllFieldsAndIgnoresUnknownKeys() {
        val original = buildRichTestState()

        val encoded = ZipBackupManager.json.encodeToString(SakuAppState.serializer(), original)
        val decoded = ZipBackupManager.json.decodeFromString(SakuAppState.serializer(), encoded)
        assertEquals(original, decoded)

        // Ensure forward compatibility when older/newer backup contains unknown fields
        val withExtraKey = encoded.replaceFirst("{", "{\"futureField\": 42,")
        val decodedWithExtra = ZipBackupManager.json.decodeFromString(SakuAppState.serializer(), withExtraKey)
        assertEquals(original, decodedWithExtra)
    }

    @Test
    fun zipRoundTrip_exportToStreamAndImportFromStream_preservesStateAndReferencedMediaWebp() {
        val sourceMediaDir = tempFolder.newFolder("source_media")
        val targetMediaDir = tempFolder.newFolder("target_media")

        val receiptBytes = byteArrayOf(0x52, 0x49, 0x46, 0x46, 0x01, 0x02, 0x03, 0x04)
        val coverBytes = byteArrayOf(0x57, 0x45, 0x42, 0x50, 0x10, 0x20, 0x30, 0x40)

        File(sourceMediaDir, "receipt_tx1.webp").writeBytes(receiptBytes)
        File(sourceMediaDir, "cover_goal1.webp").writeBytes(coverBytes)
        File(sourceMediaDir, "orphan_unused.webp").writeBytes(byteArrayOf(0x09, 0x09))

        // Existing stale media in target should be replaced atomically on successful import
        File(targetMediaDir, "stale_old.webp").writeBytes(byteArrayOf(0x7F))

        val original = buildRichTestState()
        val zipOut = ByteArrayOutputStream()
        val exportResult = ZipBackupManager.exportToStream(zipOut, original) { fileName ->
            File(sourceMediaDir, fileName)
        }
        assertTrue(exportResult.isSuccess)

        val zipBytes = zipOut.toByteArray()
        val entryNames = readZipEntryNames(zipBytes)
        assertTrue(ZipBackupManager.STATE_ENTRY_NAME in entryNames)
        assertTrue("media/receipt_tx1.webp" in entryNames)
        assertTrue("media/cover_goal1.webp" in entryNames)
        assertFalse("media/orphan_unused.webp" in entryNames)

        val importResult = ZipBackupManager.importFromStream(
            inputStream = ByteArrayInputStream(zipBytes),
            mediaTargetDir = targetMediaDir
        )
        assertTrue(importResult.isSuccess)
        assertEquals(original, importResult.getOrNull())

        val restoredReceipt = File(targetMediaDir, "receipt_tx1.webp")
        val restoredCover = File(targetMediaDir, "cover_goal1.webp")
        val staleFile = File(targetMediaDir, "stale_old.webp")
        assertTrue(restoredReceipt.exists())
        assertTrue(restoredCover.exists())
        assertFalse(staleFile.exists())
        assertArrayEquals(receiptBytes, restoredReceipt.readBytes())
        assertArrayEquals(coverBytes, restoredCover.readBytes())
    }

    @Test
    fun zipRoundTrip_skipsMissingMediaFilesGracefully() {
        val emptyMediaDir = tempFolder.newFolder("empty_source_media")
        val targetMediaDir = tempFolder.newFolder("target_media_missing")
        val original = buildRichTestState()

        val zipOut = ByteArrayOutputStream()
        val exportResult = ZipBackupManager.exportToStream(zipOut, original) { fileName ->
            File(emptyMediaDir, fileName)
        }
        assertTrue(exportResult.isSuccess)

        val importResult = ZipBackupManager.importFromStream(
            inputStream = ByteArrayInputStream(zipOut.toByteArray()),
            mediaTargetDir = targetMediaDir
        )
        assertTrue(importResult.isSuccess)
        assertEquals(original, importResult.getOrNull())
    }

    @Test
    fun importFromStream_rejectsZipSlipTraversal_andPreservesExistingMedia() {
        val parentDir = tempFolder.newFolder("sandbox_root")
        val targetMediaDir = File(parentDir, "saku_media").apply { mkdirs() }
        val existingFile = File(targetMediaDir, "existing_receipt.webp").apply {
            writeBytes(byteArrayOf(0x11, 0x22, 0x33))
        }
        val escapedFile = File(parentDir, "evil.txt")
        assertFalse(escapedFile.exists())

        val validJson = ZipBackupManager.json.encodeToString(
            SakuAppState.serializer(),
            buildRichTestState()
        )
        val maliciousZip = buildRawZip(
            ZipBackupManager.STATE_ENTRY_NAME to validJson.toByteArray(Charsets.UTF_8),
            "../../evil.txt" to "pwned".toByteArray(Charsets.UTF_8)
        )

        val result = ZipBackupManager.importFromStream(
            inputStream = ByteArrayInputStream(maliciousZip),
            mediaTargetDir = targetMediaDir
        )

        assertTrue(result.isFailure)
        assertFalse(escapedFile.exists())
        assertTrue(existingFile.exists())
        assertArrayEquals(byteArrayOf(0x11, 0x22, 0x33), existingFile.readBytes())
    }

    @Test
    fun importFromStream_rejectsNestedMediaTraversalAndBackslashZipSlip() {
        val targetMediaDir = tempFolder.newFolder("target_traversal")
        val validJson = ZipBackupManager.json.encodeToString(
            SakuAppState.serializer(),
            buildRichTestState()
        ).toByteArray(Charsets.UTF_8)

        val nestedTraversalZip = buildRawZip(
            ZipBackupManager.STATE_ENTRY_NAME to validJson,
            "media/../../../escape.webp" to byteArrayOf(0x01, 0x02)
        )
        assertTrue(
            ZipBackupManager.importFromStream(
                ByteArrayInputStream(nestedTraversalZip),
                targetMediaDir
            ).isFailure
        )

        val backslashTraversalZip = buildRawZip(
            ZipBackupManager.STATE_ENTRY_NAME to validJson,
            "media\\..\\..\\evil.txt" to byteArrayOf(0x03, 0x04)
        )
        assertTrue(
            ZipBackupManager.importFromStream(
                ByteArrayInputStream(backslashTraversalZip),
                targetMediaDir
            ).isFailure
        )
    }

    @Test
    fun importFromStream_rejectsMissingOrInvalidStateJson_withoutTouchingActiveMedia() {
        val targetMediaDir = tempFolder.newFolder("target_validation")
        val existing = File(targetMediaDir, "keep_me.webp").apply {
            writeBytes(byteArrayOf(0x44, 0x55))
        }

        val missingStateZip = buildRawZip(
            "media/new_unvalidated.webp" to byteArrayOf(0x77, 0x88.toByte())
        )
        val missingResult = ZipBackupManager.importFromStream(
            ByteArrayInputStream(missingStateZip),
            targetMediaDir
        )
        assertTrue(missingResult.isFailure)
        assertTrue(existing.exists())
        assertFalse(File(targetMediaDir, "new_unvalidated.webp").exists())

        val corruptStateZip = buildRawZip(
            ZipBackupManager.STATE_ENTRY_NAME to "{corrupted_json".toByteArray(Charsets.UTF_8),
            "media/new_unvalidated.webp" to byteArrayOf(0x77, 0x88.toByte())
        )
        val corruptResult = ZipBackupManager.importFromStream(
            ByteArrayInputStream(corruptStateZip),
            targetMediaDir
        )
        assertTrue(corruptResult.isFailure)
        assertTrue(existing.exists())
        assertFalse(File(targetMediaDir, "new_unvalidated.webp").exists())
    }

    private fun buildRichTestState(): SakuAppState {
        return SakuAppState(
            schemaVersion = 1,
            updatedAtEpochMs = 1_763_942_400_000L,
            accounts = listOf(
                Account("acc_bca", "BCA", AccountType.BANK, 5_000_000L, "bank", 0xFF1E4D45, isArchived = false),
                Account("acc_gopay", "GoPay", AccountType.EWALLET, 450_000L, "ewallet", 0xFF2A6B57, isArchived = false),
                Account("acc_old", "Dompet Lama", AccountType.CASH, 0L, "wallet", 0xFF5C6B5E, isArchived = true)
            ),
            categories = listOf(
                Category("cat_food", "Makan & Minum", TransactionType.EXPENSE, "restaurant", 0xFFB8563F, monthlyLimit = 1_500_000L),
                Category("cat_salary", "Gaji", TransactionType.INCOME, "payments", 0xFF1E4D45),
                Category(SYSTEM_CAT_SAVINGS_DEPOSIT, "Setor Tabungan", TransactionType.EXPENSE, "savings", 0xFF2A6B57, isSystem = true)
            ),
            transactions = listOf(
                Transaction(
                    id = "tx_1",
                    type = TransactionType.EXPENSE,
                    amount = 65_000L,
                    accountId = "acc_gopay",
                    categoryId = "cat_food",
                    note = "Kopi & Roti",
                    timestampEpochMs = 1_763_940_000_000L,
                    receiptFileName = "receipt_tx1.webp"
                ),
                Transaction(
                    id = "tx_2",
                    type = TransactionType.TRANSFER,
                    amount = 250_000L,
                    accountId = "acc_bca",
                    toAccountId = "acc_gopay",
                    note = "Top up mingguan",
                    timestampEpochMs = 1_763_930_000_000L
                ),
                Transaction(
                    id = "tx_3",
                    type = TransactionType.EXPENSE,
                    amount = 1_000_000L,
                    accountId = "acc_bca",
                    categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT,
                    savingsGoalId = "goal_1",
                    note = "Setor bulanan",
                    timestampEpochMs = 1_763_920_000_000L
                )
            ),
            savingsGoals = listOf(
                SavingsGoal(
                    id = "goal_1",
                    name = "Dana Darurat",
                    targetAmount = 20_000_000L,
                    coverFileName = "cover_goal1.webp",
                    deadlineEpochMs = 1_795_478_400_000L,
                    colorHex = 0xFF1E4D45,
                    iconKey = "shield",
                    createdAtEpochMs = 1_760_000_000_000L
                )
            ),
            preferences = AppPreferences(
                themeMode = ThemeMode.DARK,
                privacyBlur = true,
                monthlyBudget = 6_500_000L,
                eWalletListenerEnabled = true,
                enabledEWalletPackages = setOf("com.gojek.app", "id.dana")
            ),
            driveSyncMeta = DriveSyncMeta(
                connectedEmail = "saku.user@gmail.com",
                lastSyncedEpochMs = 1_763_942_111_000L,
                autoSyncEnabled = true
            )
        )
    }

    private fun buildRawZip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun readZipEntryNames(zipBytes: ByteArray): Set<String> {
        val names = linkedSetOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                names += entry.name
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return names
    }
}
