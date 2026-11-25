package com.saku.v2

import android.app.Application
import com.saku.v2.data.BackupManagerContract
import com.saku.v2.data.DriveSyncContract
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.NoOpBackupManager
import com.saku.v2.data.NoOpDriveSync
import com.saku.v2.data.NoOpMediaManager
import com.saku.v2.data.NoOpWidgetUpdater
import com.saku.v2.data.WidgetUpdaterContract
import com.saku.v2.data.backup.ZipBackupManager
import com.saku.v2.data.media.WebpImageManager
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.store.SakuStore
import com.saku.v2.data.sync.DriveSyncManager
import com.saku.v2.platform.ewallet.QuickLogNotificationHelper
import com.saku.v2.widget.SakuWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class SakuApplication : Application() {

    companion object {
        private const val AUTO_SYNC_DEBOUNCE_MS = 10_000L
    }

    @Volatile
    var mediaManager: MediaManagerContract = NoOpMediaManager

    @Volatile
    var backupManager: BackupManagerContract = NoOpBackupManager

    @Volatile
    var driveSync: DriveSyncContract = NoOpDriveSync

    @Volatile
    var widgetUpdater: WidgetUpdaterContract = NoOpWidgetUpdater

    lateinit var driveSyncManager: DriveSyncManager
        private set

    lateinit var store: SakuStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val autoSyncLock = Any()
    private var autoSyncJob: Job? = null

    @Volatile
    private var lastAutoSyncedDataHash: Int? = null

    override fun onCreate() {
        super.onCreate()
        val ctx = applicationContext

        val webpImageManager = WebpImageManager(ctx)
        val zipBackupManager = ZipBackupManager(ctx, mediaDir = webpImageManager.mediaDir)
        val syncManager = DriveSyncManager(ctx)
        val sakuWidgetUpdater = SakuWidgetUpdater(ctx)

        mediaManager = webpImageManager
        backupManager = zipBackupManager
        driveSyncManager = syncManager
        driveSync = syncManager
        widgetUpdater = sakuWidgetUpdater

        QuickLogNotificationHelper.ensureChannel(ctx)

        store = SakuStore(
            context = ctx,
            onStatePersisted = { state ->
                widgetUpdater.updateAllWidgets(state)
                scheduleAutoSyncIfNeeded(state)
            }
        )
    }

    private fun scheduleAutoSyncIfNeeded(state: SakuAppState) {
        if (!::store.isInitialized || !store.isLoaded.value) return
        val meta = state.driveSyncMeta
        if (!meta.autoSyncEnabled || meta.connectedEmail.isNullOrBlank()) return

        val dataHash = semanticContentHash(state)
        if (lastAutoSyncedDataHash == null) {
            lastAutoSyncedDataHash = dataHash
            return
        }
        if (dataHash == lastAutoSyncedDataHash) return

        synchronized(autoSyncLock) {
            autoSyncJob?.cancel()
            autoSyncJob = appScope.launch {
                delay(AUTO_SYNC_DEBOUNCE_MS)
                val snapshot = store.state.value
                val currentMeta = snapshot.driveSyncMeta
                if (!currentMeta.autoSyncEnabled || currentMeta.connectedEmail.isNullOrBlank()) {
                    return@launch
                }
                val currentHash = semanticContentHash(snapshot)
                var tempZip: File? = null
                try {
                    val zipFile = backupManager.exportToTempFile(snapshot).getOrNull() ?: return@launch
                    tempZip = zipFile
                    val syncedAt = driveSync.uploadZip(zipFile).getOrNull() ?: return@launch
                    lastAutoSyncedDataHash = currentHash
                    store.mutateState { latest ->
                        latest.copy(
                            driveSyncMeta = latest.driveSyncMeta.copy(lastSyncedEpochMs = syncedAt)
                        )
                    }
                } finally {
                    tempZip?.delete()
                }
            }
        }
    }

    private fun semanticContentHash(state: SakuAppState): Int {
        var result = state.accounts.hashCode()
        result = 31 * result + state.categories.hashCode()
        result = 31 * result + state.transactions.hashCode()
        result = 31 * result + state.savingsGoals.hashCode()
        result = 31 * result + state.preferences.hashCode()
        return result
    }
}
