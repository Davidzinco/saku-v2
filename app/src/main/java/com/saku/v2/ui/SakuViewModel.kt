package com.saku.v2.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saku.v2.SakuApplication
import com.saku.v2.data.BackupManagerContract
import com.saku.v2.data.DriveSyncContract
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.model.Account
import com.saku.v2.data.model.AccountType
import com.saku.v2.data.model.Category
import com.saku.v2.data.model.DriveSyncMeta
import com.saku.v2.data.model.SYSTEM_CAT_SAVINGS_DEPOSIT
import com.saku.v2.data.model.SYSTEM_CAT_SAVINGS_WITHDRAW
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.SavingsGoal
import com.saku.v2.data.model.ThemeMode
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import com.saku.v2.data.store.PendingQuickLog
import com.saku.v2.data.store.QuickLogEventBus
import com.saku.v2.data.store.SakuStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class SakuViewModel(application: Application) : AndroidViewModel(application) {

    private val app: SakuApplication = application as SakuApplication
    private val store: SakuStore get() = app.store
    private val backupManager: BackupManagerContract get() = app.backupManager
    private val driveSync: DriveSyncContract get() = app.driveSync

    val mediaManager: MediaManagerContract get() = app.mediaManager
    val state: StateFlow<SakuAppState> get() = store.state
    val pendingQuickLog: StateFlow<PendingQuickLog?> = QuickLogEventBus.pending

    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()

    fun addTransaction(
        type: TransactionType,
        amount: Long,
        accountId: String,
        toAccountId: String? = null,
        categoryId: String? = null,
        savingsGoalId: String? = null,
        note: String = "",
        timestampEpochMs: Long = System.currentTimeMillis(),
        receiptFileName: String? = null
    ) {
        if (amount <= 0L) {
            emitMessage("Nominal harus lebih dari 0")
            return
        }
        if (type == TransactionType.TRANSFER && (toAccountId.isNullOrBlank() || toAccountId == accountId)) {
            emitMessage("Pilih rekening tujuan yang berbeda")
            return
        }

        val tx = Transaction(
            id = "tx_${UUID.randomUUID()}",
            type = type,
            amount = amount,
            accountId = accountId,
            toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
            categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
            savingsGoalId = savingsGoalId,
            note = note.trim(),
            timestampEpochMs = timestampEpochMs,
            receiptFileName = receiptFileName
        )

        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    transactions = (listOf(tx) + current.transactions)
                        .sortedByDescending { it.timestampEpochMs }
                )
            }
            emitMessage("Transaksi tersimpan")
        }
    }

    fun updateTransaction(transaction: Transaction) {
        if (transaction.amount <= 0L) {
            emitMessage("Nominal harus lebih dari 0")
            return
        }
        viewModelScope.launch {
            val next = store.mutateState { current ->
                val replaced = current.transactions.map { existing ->
                    if (existing.id == transaction.id) transaction else existing
                }
                current.copy(transactions = replaced.sortedByDescending { it.timestampEpochMs })
            }
            cleanupOrphanMedia(next)
            emitMessage("Transaksi diperbarui")
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val next = store.mutateState { current ->
                current.copy(
                    transactions = current.transactions.filterNot { it.id == transactionId }
                )
            }
            cleanupOrphanMedia(next)
            emitMessage("Transaksi dihapus")
        }
    }

    fun depositToSavings(
        goalId: String,
        amount: Long,
        fromAccountId: String,
        note: String = ""
    ) {
        if (amount <= 0L) {
            emitMessage("Nominal setoran harus lebih dari 0")
            return
        }
        val goalExists = state.value.savingsGoals.any { it.id == goalId }
        if (!goalExists) {
            emitMessage("Target tabungan tidak ditemukan")
            return
        }
        val tx = Transaction(
            id = "tx_${UUID.randomUUID()}",
            type = TransactionType.EXPENSE,
            amount = amount,
            accountId = fromAccountId,
            categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT,
            savingsGoalId = goalId,
            note = note.trim().ifEmpty { "Setor Tabungan" },
            timestampEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    transactions = (listOf(tx) + current.transactions)
                        .sortedByDescending { it.timestampEpochMs }
                )
            }
            emitMessage("Setoran tabungan berhasil")
        }
    }

    fun withdrawFromSavings(
        goalId: String,
        amount: Long,
        toAccountId: String,
        note: String = ""
    ) {
        if (amount <= 0L) {
            emitMessage("Nominal penarikan harus lebih dari 0")
            return
        }
        val currentSaved = SakuStateCalculator.savingsGoalSavedAmount(state.value, goalId)
        if (amount > currentSaved) {
            emitMessage("Saldo tabungan tidak mencukupi")
            return
        }
        val tx = Transaction(
            id = "tx_${UUID.randomUUID()}",
            type = TransactionType.INCOME,
            amount = amount,
            accountId = toAccountId,
            categoryId = SYSTEM_CAT_SAVINGS_WITHDRAW,
            savingsGoalId = goalId,
            note = note.trim().ifEmpty { "Tarik Tabungan" },
            timestampEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    transactions = (listOf(tx) + current.transactions)
                        .sortedByDescending { it.timestampEpochMs }
                )
            }
            emitMessage("Penarikan tabungan berhasil")
        }
    }

    fun addSavingsGoal(
        name: String,
        targetAmount: Long,
        coverFileName: String? = null,
        deadlineEpochMs: Long? = null,
        colorHex: Long,
        iconKey: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || targetAmount <= 0L) {
            emitMessage("Lengkapi nama dan target tabungan")
            return
        }
        val goal = SavingsGoal(
            id = "goal_${UUID.randomUUID()}",
            name = cleanName,
            targetAmount = targetAmount,
            coverFileName = coverFileName,
            deadlineEpochMs = deadlineEpochMs,
            colorHex = colorHex,
            iconKey = iconKey,
            createdAtEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(savingsGoals = current.savingsGoals + goal)
            }
            emitMessage("Target \"$cleanName\" ditambahkan")
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        if (goal.name.isBlank() || goal.targetAmount <= 0L) {
            emitMessage("Nama dan nominal target tidak valid")
            return
        }
        viewModelScope.launch {
            val next = store.mutateState { current ->
                current.copy(
                    savingsGoals = current.savingsGoals.map { existing ->
                        if (existing.id == goal.id) goal else existing
                    }
                )
            }
            cleanupOrphanMedia(next)
            emitMessage("Target tabungan diperbarui")
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            val next = store.mutateState { current ->
                current.copy(
                    savingsGoals = current.savingsGoals.filterNot { it.id == goalId },
                    transactions = current.transactions.map { tx ->
                        if (tx.savingsGoalId == goalId) tx.copy(savingsGoalId = null) else tx
                    }
                )
            }
            cleanupOrphanMedia(next)
            emitMessage("Target tabungan dihapus")
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        initialBalance: Long,
        iconKey: String,
        colorHex: Long
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            emitMessage("Nama akun tidak boleh kosong")
            return
        }
        val account = Account(
            id = "acc_${UUID.randomUUID()}",
            name = cleanName,
            type = type,
            initialBalance = initialBalance,
            iconKey = iconKey,
            colorHex = colorHex
        )
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(accounts = current.accounts + account)
            }
            emitMessage("Akun \"$cleanName\" ditambahkan")
        }
    }

    fun updateAccount(account: Account) {
        if (account.name.isBlank()) {
            emitMessage("Nama akun tidak boleh kosong")
            return
        }
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    accounts = current.accounts.map { existing ->
                        if (existing.id == account.id) account else existing
                    }
                )
            }
            emitMessage("Akun diperbarui")
        }
    }

    fun archiveAccount(accountId: String, archived: Boolean = true) {
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    accounts = current.accounts.map { existing ->
                        if (existing.id == accountId) existing.copy(isArchived = archived) else existing
                    }
                )
            }
            emitMessage(if (archived) "Akun diarsipkan" else "Akun diaktifkan kembali")
        }
    }

    fun addCategory(
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Long? = null
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            emitMessage("Nama kategori tidak boleh kosong")
            return
        }
        val category = Category(
            id = "cat_${UUID.randomUUID()}",
            name = cleanName,
            type = type,
            iconKey = iconKey,
            colorHex = colorHex,
            monthlyLimit = monthlyLimit?.takeIf { it > 0L },
            isSystem = false
        )
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(categories = current.categories + category)
            }
            emitMessage("Kategori \"$cleanName\" ditambahkan")
        }
    }

    fun deleteCategory(categoryId: String) {
        val target = state.value.categories.firstOrNull { it.id == categoryId } ?: return
        if (target.isSystem) {
            emitMessage("Kategori sistem tidak dapat dihapus")
            return
        }
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    categories = current.categories.filterNot { it.id == categoryId }
                )
            }
            emitMessage("Kategori dihapus")
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    preferences = current.preferences.copy(themeMode = themeMode)
                )
            }
        }
    }

    fun togglePrivacyBlur() {
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    preferences = current.preferences.copy(
                        privacyBlur = !current.preferences.privacyBlur
                    )
                )
            }
        }
    }

    fun updateMonthlyBudget(monthlyBudget: Long) {
        if (monthlyBudget < 0L) {
            emitMessage("Anggaran bulanan tidak valid")
            return
        }
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    preferences = current.preferences.copy(monthlyBudget = monthlyBudget)
                )
            }
            emitMessage("Anggaran bulanan diperbarui")
        }
    }

    fun updateEWalletSettings(
        enabled: Boolean,
        enabledPackages: Set<String> = state.value.preferences.enabledEWalletPackages
    ) {
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    preferences = current.preferences.copy(
                        eWalletListenerEnabled = enabled,
                        enabledEWalletPackages = enabledPackages
                    )
                )
            }
        }
    }

    fun exportBackupZip(uri: Uri) {
        if (_syncInProgress.value) return
        viewModelScope.launch {
            _syncInProgress.value = true
            try {
                val result = backupManager.exportToUri(uri, state.value)
                result.fold(
                    onSuccess = { emitMessage("Cadangan ZIP berhasil diekspor") },
                    onFailure = { emitMessage("Gagal mengekspor: ${it.localizedMessage ?: "error"}") }
                )
            } finally {
                _syncInProgress.value = false
            }
        }
    }

    fun importBackupZip(uri: Uri) {
        if (_syncInProgress.value) return
        viewModelScope.launch {
            _syncInProgress.value = true
            try {
                val result = backupManager.importFromUri(uri)
                result.fold(
                    onSuccess = { imported ->
                        val saved = store.replaceState(imported)
                        cleanupOrphanMedia(saved)
                        emitMessage("Cadangan ZIP berhasil dipulihkan")
                    },
                    onFailure = { emitMessage("Gagal memulihkan: ${it.localizedMessage ?: "error"}") }
                )
            } finally {
                _syncInProgress.value = false
            }
        }
    }

    fun syncUploadDrive() {
        if (_syncInProgress.value) return
        viewModelScope.launch {
            _syncInProgress.value = true
            var tempFile: File? = null
            try {
                val zipResult = backupManager.exportToTempFile(state.value)
                val zipFile = zipResult.getOrElse {
                    emitMessage("Gagal menyiapkan arsip: ${it.localizedMessage ?: "error"}")
                    return@launch
                }
                tempFile = zipFile

                val uploadResult = driveSync.uploadZip(zipFile)
                uploadResult.fold(
                    onSuccess = { syncedEpochMs ->
                        val email = driveSync.getConnectedAccountEmail()
                            ?: state.value.driveSyncMeta.connectedEmail
                        store.mutateState { current ->
                            current.copy(
                                driveSyncMeta = current.driveSyncMeta.copy(
                                    connectedEmail = email,
                                    lastSyncedEpochMs = syncedEpochMs
                                )
                            )
                        }
                        emitMessage("Sinkronisasi ke Google Drive berhasil")
                    },
                    onFailure = {
                        emitMessage("Unggah Drive gagal: ${it.localizedMessage ?: "error"}")
                    }
                )
            } finally {
                tempFile?.delete()
                _syncInProgress.value = false
            }
        }
    }

    fun syncDownloadDrive() {
        if (_syncInProgress.value) return
        viewModelScope.launch {
            _syncInProgress.value = true
            val tempFile = File.createTempFile("saku_drive_dl_", ".zip", app.cacheDir)
            try {
                val dlResult = driveSync.downloadZip(tempFile)
                dlResult.onFailure {
                    emitMessage("Unduh Drive gagal: ${it.localizedMessage ?: "error"}")
                    return@launch
                }

                val importResult = backupManager.importFromFile(tempFile)
                importResult.fold(
                    onSuccess = { imported ->
                        val email = driveSync.getConnectedAccountEmail()
                            ?: state.value.driveSyncMeta.connectedEmail
                        val remoteTime = driveSync.fetchRemoteModifiedTime().getOrNull()
                            ?: System.currentTimeMillis()
                        val merged = imported.copy(
                            driveSyncMeta = imported.driveSyncMeta.copy(
                                connectedEmail = email,
                                lastSyncedEpochMs = remoteTime
                            )
                        )
                        val saved = store.replaceState(merged)
                        cleanupOrphanMedia(saved)
                        emitMessage("Data berhasil dipulihkan dari Google Drive")
                    },
                    onFailure = {
                        emitMessage("Gagal membaca arsip Drive: ${it.localizedMessage ?: "error"}")
                    }
                )
            } finally {
                tempFile.delete()
                _syncInProgress.value = false
            }
        }
    }

    fun onDriveSignedIn(email: String) {
        viewModelScope.launch {
            store.mutateState { current ->
                current.copy(
                    driveSyncMeta = current.driveSyncMeta.copy(connectedEmail = email)
                )
            }
            emitMessage("Terhubung sebagai $email")
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            driveSync.signOut()
            store.mutateState { current ->
                current.copy(driveSyncMeta = DriveSyncMeta())
            }
            emitMessage("Koneksi Google Drive diputuskan")
        }
    }

    fun consumeQuickLog(): PendingQuickLog? = QuickLogEventBus.consume()

    fun dismissQuickLog() {
        QuickLogEventBus.dismiss()
    }

    fun triggerDemoQuickLog() {
        QuickLogEventBus.emit(
            PendingQuickLog(
                id = "qlog_${UUID.randomUUID()}",
                sourceApp = "GoPay",
                suggestedAccountName = "GoPay",
                amount = 42_500L,
                type = TransactionType.EXPENSE,
                suggestedNote = "QRIS Kopi Kenangan",
                timestampEpochMs = System.currentTimeMillis()
            )
        )
    }

    fun showMessage(message: String) {
        emitMessage(message)
    }

    private suspend fun cleanupOrphanMedia(appState: SakuAppState) {
        val referenced = buildSet {
            appState.transactions.mapNotNullTo(this) { it.receiptFileName }
            appState.savingsGoals.mapNotNullTo(this) { it.coverFileName }
        }
        mediaManager.cleanupOrphanMedia(referenced)
    }

    private fun emitMessage(message: String) {
        _uiMessage.tryEmit(message)
    }
}
