package com.saku.v2.data.backup

import android.content.Context
import android.net.Uri
import com.saku.v2.data.BackupManagerContract
import com.saku.v2.data.media.WebpImageManager
import com.saku.v2.data.model.SakuAppState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports and restores full Saku V2 ZIP archives (`saku_state.json` + `media/<file>.webp`)
 * with strict zip-slip protection and staging-validated atomic media replacement.
 */
class ZipBackupManager(
    context: Context,
    private val mediaDir: File = File(
        (context.applicationContext ?: context).filesDir,
        WebpImageManager.MEDIA_DIR_NAME
    ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupManagerContract {

    private val appContext: Context = context.applicationContext ?: context

    companion object {
        const val STATE_ENTRY_NAME = "saku_state.json"
        const val MEDIA_ENTRY_PREFIX = "media/"
        private const val MAX_STATE_JSON_BYTES = 32L * 1024L * 1024L
        private const val MAX_MEDIA_FILE_BYTES = 25L * 1024L * 1024L

        val json: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

        /**
         * Pure JVM-testable ZIP export helper writing `saku_state.json` and referenced media files.
         */
        fun exportToStream(
            outputStream: OutputStream,
            state: SakuAppState,
            mediaProvider: (String) -> File?
        ): Result<Unit> {
            return try {
                val referencedFiles = buildSet {
                    state.transactions.mapNotNullTo(this) { sanitizeFileName(it.receiptFileName) }
                    state.savingsGoals.mapNotNullTo(this) { sanitizeFileName(it.coverFileName) }
                }.sorted()

                val jsonBytes = json.encodeToString(SakuAppState.serializer(), state)
                    .toByteArray(Charsets.UTF_8)

                ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                    zos.putNextEntry(ZipEntry(STATE_ENTRY_NAME))
                    zos.write(jsonBytes)
                    zos.closeEntry()

                    for (fileName in referencedFiles) {
                        val file = mediaProvider(fileName)
                        if (file != null && file.exists() && file.isFile) {
                            zos.putNextEntry(ZipEntry("$MEDIA_ENTRY_PREFIX$fileName"))
                            FileInputStream(file).use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
                Result.success(Unit)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

        /**
         * Pure JVM-testable ZIP import helper enforcing zip-slip checks, JSON validation,
         * and atomic replacement of `mediaTargetDir` only after validation succeeds.
         */
        fun importFromStream(
            inputStream: InputStream,
            mediaTargetDir: File
        ): Result<SakuAppState> {
            val resolvedTargetDir = mediaTargetDir.absoluteFile
            val stagingParent = (resolvedTargetDir.parentFile ?: resolvedTargetDir).apply { mkdirs() }
            val stagingDir = File(stagingParent, ".saku_zip_staging_${UUID.randomUUID()}")
            val stagedMediaDir = File(stagingDir, "media")
            val oldMediaBackupDir = File(stagingDir, "old_media_backup")

            return try {
                if (!stagingDir.mkdirs() || !stagedMediaDir.mkdirs() || !oldMediaBackupDir.mkdirs()) {
                    return Result.failure(IOException("Gagal menyiapkan direktori staging backup"))
                }

                val canonicalStagingPath = stagingDir.canonicalPath
                var stateJsonBytes: ByteArray? = null

                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val safeNormalizedEntry = validateZipEntryPath(
                            entryName = entry.name,
                            stagingDir = stagingDir,
                            canonicalStagingPath = canonicalStagingPath
                        )

                        if (!entry.isDirectory) {
                            when {
                                safeNormalizedEntry == STATE_ENTRY_NAME -> {
                                    stateJsonBytes = readEntryLimited(zis, MAX_STATE_JSON_BYTES)
                                }

                                safeNormalizedEntry.startsWith(MEDIA_ENTRY_PREFIX) -> {
                                    val relativeMedia = safeNormalizedEntry.removePrefix(MEDIA_ENTRY_PREFIX)
                                    if (relativeMedia.isNotBlank() &&
                                        !relativeMedia.contains('/') &&
                                        relativeMedia.endsWith(".webp", ignoreCase = true)
                                    ) {
                                        val stagedFile = File(stagedMediaDir, relativeMedia)
                                        FileOutputStream(stagedFile).use { fos ->
                                            copyStreamLimited(zis, fos, MAX_MEDIA_FILE_BYTES)
                                        }
                                    }
                                }
                            }
                        }

                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val rawJson = stateJsonBytes?.toString(Charsets.UTF_8)
                    ?: return Result.failure(
                        IllegalArgumentException("Arsip backup tidak valid: $STATE_ENTRY_NAME tidak ditemukan")
                    )

                val parsedState = json.decodeFromString(SakuAppState.serializer(), rawJson)

                replaceMediaAtomically(
                    targetDir = resolvedTargetDir,
                    stagedMediaDir = stagedMediaDir,
                    backupDir = oldMediaBackupDir
                )

                Result.success(parsedState)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(t)
            } finally {
                stagingDir.deleteRecursively()
            }
        }

        private fun validateZipEntryPath(
            entryName: String,
            stagingDir: File,
            canonicalStagingPath: String
        ): String {
            val normalized = entryName.replace('\\', '/')
            if (normalized.isBlank() ||
                normalized.startsWith("/") ||
                DRIVE_PREFIX_REGEX.containsMatchIn(normalized) ||
                normalized.split('/').any { it == ".." }
            ) {
                throw SecurityException("Entri ZIP tidak aman (zip-slip terdeteksi): $entryName")
            }

            val candidate = File(stagingDir, normalized)
            val canonicalCandidate = candidate.canonicalPath
            if (canonicalCandidate != canonicalStagingPath &&
                !canonicalCandidate.startsWith(canonicalStagingPath + File.separator)
            ) {
                throw SecurityException("Entri ZIP keluar dari direktori staging: $entryName")
            }

            return normalized
        }

        private fun readEntryLimited(input: InputStream, maxBytes: Long): ByteArray {
            val baos = ByteArrayOutputStream()
            copyStreamLimited(input, baos, maxBytes)
            return baos.toByteArray()
        }

        private fun copyStreamLimited(input: InputStream, output: OutputStream, maxBytes: Long) {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var totalRead = 0L
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                totalRead += read
                if (totalRead > maxBytes) {
                    throw SecurityException("Ukuran entri ZIP melebihi batas aman")
                }
                output.write(buffer, 0, read)
            }
            output.flush()
        }

        private fun replaceMediaAtomically(
            targetDir: File,
            stagedMediaDir: File,
            backupDir: File
        ) {
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw IOException("Gagal membuat direktori media tujuan")
            }

            val existingWebp = targetDir.listFiles()
                ?.filter { it.isFile && it.extension.equals("webp", ignoreCase = true) }
                .orEmpty()

            val backedUpFiles = mutableListOf<Pair<File, File>>()
            val placedFiles = mutableListOf<File>()

            try {
                for (existing in existingWebp) {
                    val backupFile = File(backupDir, existing.name)
                    moveFile(existing, backupFile)
                    backedUpFiles += existing to backupFile
                }

                val stagedWebp = stagedMediaDir.listFiles()
                    ?.filter { it.isFile && it.extension.equals("webp", ignoreCase = true) }
                    .orEmpty()

                for (staged in stagedWebp) {
                    val destFile = File(targetDir, staged.name)
                    moveFile(staged, destFile)
                    placedFiles += destFile
                }
            } catch (t: Throwable) {
                for (placed in placedFiles) {
                    placed.delete()
                }
                for ((original, backup) in backedUpFiles) {
                    if (backup.exists()) {
                        runCatching { moveFile(backup, original) }
                    }
                }
                throw t
            }
        }

        private fun moveFile(source: File, target: File) {
            if (target.exists()) {
                target.delete()
            }
            if (!source.renameTo(target)) {
                source.copyTo(target, overwrite = true)
                source.delete()
            }
        }

        private fun sanitizeFileName(rawName: String?): String? {
            if (rawName.isNullOrBlank()) return null
            val safe = File(rawName).name
            return safe.takeIf { it.isNotBlank() && it != "." && it != ".." }
        }

        private val DRIVE_PREFIX_REGEX = Regex("^[A-Za-z]:")
    }

    override suspend fun exportToUri(uri: Uri, state: SakuAppState): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val outputStream = appContext.contentResolver.openOutputStream(uri)
                    ?: return@withContext Result.failure(IOException("Tidak dapat membuka file tujuan backup"))
                outputStream.use { out ->
                    exportToStream(out, state) { fileName -> resolveMediaFileIfSafe(fileName) }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
    }

    override suspend fun importFromUri(uri: Uri): Result<SakuAppState> {
        return withContext(ioDispatcher) {
            try {
                val inputStream = appContext.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IOException("Tidak dapat membuka file arsip backup"))
                inputStream.use { input ->
                    importFromStream(input, mediaDir)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
    }

    override suspend fun exportToTempFile(state: SakuAppState): Result<File> {
        return withContext(ioDispatcher) {
            val tempFile = File.createTempFile("saku_v2_backup_", ".zip", appContext.cacheDir)
            try {
                val exportResult = FileOutputStream(tempFile).use { out ->
                    exportToStream(out, state) { fileName -> resolveMediaFileIfSafe(fileName) }
                }
                if (exportResult.isFailure) {
                    tempFile.delete()
                    Result.failure(exportResult.exceptionOrNull() ?: IOException("Gagal menulis arsip sementara"))
                } else {
                    Result.success(tempFile)
                }
            } catch (ce: CancellationException) {
                tempFile.delete()
                throw ce
            } catch (t: Throwable) {
                tempFile.delete()
                Result.failure(t)
            }
        }
    }

    override suspend fun importFromFile(file: File): Result<SakuAppState> {
        return withContext(ioDispatcher) {
            try {
                if (!file.exists() || !file.isFile) {
                    return@withContext Result.failure(IOException("File backup tidak ditemukan"))
                }
                FileInputStream(file).use { input ->
                    importFromStream(input, mediaDir)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
    }

    private fun resolveMediaFileIfSafe(fileName: String): File? {
        val safeName = sanitizeFileName(fileName) ?: return null
        return File(mediaDir, safeName)
    }
}
