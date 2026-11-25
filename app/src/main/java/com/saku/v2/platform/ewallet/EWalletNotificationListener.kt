package com.saku.v2.platform.ewallet

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.saku.v2.SakuApplication
import com.saku.v2.data.store.PendingQuickLog
import com.saku.v2.data.store.QuickLogEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Listens for e-wallet and bank notifications and emits non-auto-saving Quick-Log suggestions.
 */
class EWalletNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val recentLock = Any()
    private val recentEvents = LinkedHashMap<String, Long>()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        if (sbn.isOngoing ||
            (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        ) {
            return
        }

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val shortText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bodyText = when {
            bigText.isNotBlank() && shortText.isNotBlank() && !bigText.contains(shortText) -> "$shortText $bigText"
            bigText.isNotBlank() -> bigText
            else -> shortText
        }
        if (title.isBlank() && bodyText.isBlank()) return

        val app = applicationContext as? SakuApplication ?: return
        val store = app.store

        if (store.isLoaded.value) {
            processNotification(
                packageName = packageName,
                title = title,
                bodyText = bodyText,
                postTime = sbn.postTime,
                preferences = store.state.value.preferences
            )
        } else {
            scope.launch {
                withTimeoutOrNull(2_000L) {
                    store.isLoaded.first { it }
                }
                if (store.isLoaded.value) {
                    processNotification(
                        packageName = packageName,
                        title = title,
                        bodyText = bodyText,
                        postTime = sbn.postTime,
                        preferences = store.state.value.preferences
                    )
                }
            }
        }
    }

    private fun processNotification(
        packageName: String,
        title: String,
        bodyText: String,
        postTime: Long,
        preferences: com.saku.v2.data.model.AppPreferences
    ) {
        if (!preferences.eWalletListenerEnabled) return
        if (packageName !in preferences.enabledEWalletPackages) return

        val parsed = EWalletNotificationParser.parse(packageName, title, bodyText) ?: return
        val now = System.currentTimeMillis()
        val fingerprint = "$packageName|${parsed.type.name}|${parsed.amount}|${parsed.suggestedNote}"
        if (isDuplicateWithinWindow(fingerprint, now)) return

        val quickLog = PendingQuickLog(
            id = "qlog_${UUID.randomUUID()}",
            sourceApp = parsed.sourceApp,
            suggestedAccountName = parsed.suggestedAccountName,
            amount = parsed.amount,
            type = parsed.type,
            suggestedNote = parsed.suggestedNote,
            timestampEpochMs = postTime.takeIf { it > 0L } ?: now
        )

        QuickLogEventBus.emit(quickLog)
        QuickLogNotificationHelper.postQuickLogNotification(applicationContext, quickLog)
    }

    private fun isDuplicateWithinWindow(fingerprint: String, nowMs: Long): Boolean {
        synchronized(recentLock) {
            val iterator = recentEvents.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (nowMs - entry.value > DEDUP_WINDOW_MS) {
                    iterator.remove()
                }
            }
            val previous = recentEvents[fingerprint]
            if (previous != null && nowMs - previous <= DEDUP_WINDOW_MS) {
                return true
            }
            recentEvents[fingerprint] = nowMs
            while (recentEvents.size > MAX_RECENT_CACHE) {
                val oldestKey = recentEvents.entries.firstOrNull()?.key ?: break
                recentEvents.remove(oldestKey)
            }
            return false
        }
    }

    companion object {
        private const val DEDUP_WINDOW_MS = 15_000L
        private const val MAX_RECENT_CACHE = 24
    }
}
