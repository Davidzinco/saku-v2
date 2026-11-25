package com.saku.v2.data

import android.net.Uri
import com.saku.v2.data.model.SakuAppState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Handles WebP compression, EXIF orientation normalization, and local media file resolution.
 */
interface MediaManagerContract {
    suspend fun importReceiptWebp(uri: Uri): Result<String>
    suspend fun importCoverWebp(uri: Uri): Result<String>
    fun resolveMediaFile(fileName: String): File
    suspend fun cleanupOrphanMedia(referencedFiles: Set<String>)
}

/**
 * Handles exporting and importing full Saku V2 ZIP archives (`saku_state.json` + media/).
 */
interface BackupManagerContract {
    suspend fun exportToUri(uri: Uri, state: SakuAppState): Result<Unit>
    suspend fun importFromUri(uri: Uri): Result<SakuAppState>
    suspend fun exportToTempFile(state: SakuAppState): Result<File>
    suspend fun importFromFile(file: File): Result<SakuAppState>
}

/**
 * Handles Google Drive appDataFolder synchronization for Saku V2 ZIP backups.
 */
interface DriveSyncContract {
    val signedInEmail: StateFlow<String?>
    suspend fun getConnectedAccountEmail(): String?
    suspend fun uploadZip(zipFile: File): Result<Long>
    suspend fun downloadZip(destFile: File): Result<Unit>
    suspend fun fetchRemoteModifiedTime(): Result<Long?>
    suspend fun signOut()
}

/**
 * Pushes updated state summaries to home screen AppWidgets.
 */
interface WidgetUpdaterContract {
    fun updateAllWidgets(state: SakuAppState)
}

object NoOpMediaManager : MediaManagerContract {
    override suspend fun importReceiptWebp(uri: Uri): Result<String> =
        Result.failure(UnsupportedOperationException("MediaManager not wired"))

    override suspend fun importCoverWebp(uri: Uri): Result<String> =
        Result.failure(UnsupportedOperationException("MediaManager not wired"))

    override fun resolveMediaFile(fileName: String): File = File(fileName)

    override suspend fun cleanupOrphanMedia(referencedFiles: Set<String>) = Unit
}

object NoOpBackupManager : BackupManagerContract {
    override suspend fun exportToUri(uri: Uri, state: SakuAppState): Result<Unit> =
        Result.failure(UnsupportedOperationException("BackupManager not wired"))

    override suspend fun importFromUri(uri: Uri): Result<SakuAppState> =
        Result.failure(UnsupportedOperationException("BackupManager not wired"))

    override suspend fun exportToTempFile(state: SakuAppState): Result<File> =
        Result.failure(UnsupportedOperationException("BackupManager not wired"))

    override suspend fun importFromFile(file: File): Result<SakuAppState> =
        Result.failure(UnsupportedOperationException("BackupManager not wired"))
}

object NoOpDriveSync : DriveSyncContract {
    override val signedInEmail: StateFlow<String?> = MutableStateFlow(null)

    override suspend fun getConnectedAccountEmail(): String? = null

    override suspend fun uploadZip(zipFile: File): Result<Long> =
        Result.failure(UnsupportedOperationException("DriveSync not wired"))

    override suspend fun downloadZip(destFile: File): Result<Unit> =
        Result.failure(UnsupportedOperationException("DriveSync not wired"))

    override suspend fun fetchRemoteModifiedTime(): Result<Long?> =
        Result.success(null)

    override suspend fun signOut() = Unit
}

object NoOpWidgetUpdater : WidgetUpdaterContract {
    override fun updateAllWidgets(state: SakuAppState) = Unit
}
