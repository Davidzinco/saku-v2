package com.saku.v2.platform.ewallet

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.saku.v2.MainActivity
import com.saku.v2.R
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.TransactionType
import com.saku.v2.data.store.PendingQuickLog
import java.util.UUID

/**
 * Shared intent action constants, quick-log extra keys, and system listener checks.
 */
object SakuIntentActions {
    const val ACTION_QUICK_ADD = "com.saku.v2.ACTION_QUICK_ADD"
    const val ACTION_QUICK_LOG_EWALLET = "com.saku.v2.ACTION_QUICK_LOG_EWALLET"

    const val EXTRA_QL_ID = "extra_ql_id"
    const val EXTRA_QL_SOURCE_APP = "extra_ql_source_app"
    const val EXTRA_QL_ACCOUNT_NAME = "extra_ql_account_name"
    const val EXTRA_QL_AMOUNT = "extra_ql_amount"
    const val EXTRA_QL_TYPE = "extra_ql_type"
    const val EXTRA_QL_NOTE = "extra_ql_note"
    const val EXTRA_QL_TIMESTAMP = "extra_ql_timestamp"

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val expectedComponent = ComponentName(context, EWalletNotificationListener::class.java)
        return enabledListeners.split(':').any { rawEntry ->
            val trimmed = rawEntry.trim()
            if (trimmed.isEmpty()) return@any false
            val component = ComponentName.unflattenFromString(trimmed)
            component == expectedComponent || component?.packageName == context.packageName
        }
    }

    fun parsePendingQuickLog(intent: Intent?): PendingQuickLog? {
        if (intent == null || intent.action != ACTION_QUICK_LOG_EWALLET) return null
        val amount = intent.getLongExtra(EXTRA_QL_AMOUNT, 0L)
        if (amount <= 0L) return null

        val sourceApp = intent.getStringExtra(EXTRA_QL_SOURCE_APP).orEmpty().ifBlank { "E-Wallet" }
        val accountName = intent.getStringExtra(EXTRA_QL_ACCOUNT_NAME)?.takeIf { it.isNotBlank() }
        val typeName = intent.getStringExtra(EXTRA_QL_TYPE).orEmpty()
        val type = runCatching { TransactionType.valueOf(typeName) }
            .getOrDefault(TransactionType.EXPENSE)
        val note = intent.getStringExtra(EXTRA_QL_NOTE).orEmpty()
        val id = intent.getStringExtra(EXTRA_QL_ID)?.takeIf { it.isNotBlank() }
            ?: "qlog_${UUID.randomUUID()}"
        val timestamp = intent.getLongExtra(EXTRA_QL_TIMESTAMP, 0L)
            .takeIf { it > 0L } ?: System.currentTimeMillis()

        return PendingQuickLog(
            id = id,
            sourceApp = sourceApp,
            suggestedAccountName = accountName,
            amount = amount,
            type = type,
            suggestedNote = note,
            timestampEpochMs = timestamp
        )
    }
}

/**
 * Posts high-priority Quick-Log suggestions without ever saving transactions automatically.
 */
object QuickLogNotificationHelper {

    const val CHANNEL_ID = "saku_quick_log"
    private const val CHANNEL_NAME = "Quick-Log E-Wallet"
    private const val FOREST_PINE_COLOR = 0xFF1E4D45.toInt()

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Saran pencatatan transaksi dari notifikasi e-wallet dan bank tanpa simpan otomatis."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun postQuickLogNotification(context: Context, quickLog: PendingQuickLog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        ensureChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = SakuIntentActions.ACTION_QUICK_LOG_EWALLET
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(SakuIntentActions.EXTRA_QL_ID, quickLog.id)
            putExtra(SakuIntentActions.EXTRA_QL_SOURCE_APP, quickLog.sourceApp)
            putExtra(SakuIntentActions.EXTRA_QL_ACCOUNT_NAME, quickLog.suggestedAccountName)
            putExtra(SakuIntentActions.EXTRA_QL_AMOUNT, quickLog.amount)
            putExtra(SakuIntentActions.EXTRA_QL_TYPE, quickLog.type.name)
            putExtra(SakuIntentActions.EXTRA_QL_NOTE, quickLog.suggestedNote)
            putExtra(SakuIntentActions.EXTRA_QL_TIMESTAMP, quickLog.timestampEpochMs)
        }

        val requestCode = (quickLog.id.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val signedAmount = when (quickLog.type) {
            TransactionType.INCOME -> "+${SakuStateCalculator.formatRupiah(quickLog.amount)}"
            TransactionType.EXPENSE -> "-${SakuStateCalculator.formatRupiah(quickLog.amount)}"
            TransactionType.TRANSFER -> SakuStateCalculator.formatRupiah(quickLog.amount)
        }
        val contentText = buildString {
            append(signedAmount)
            if (quickLog.suggestedNote.isNotBlank()) {
                append(" • ")
                append(quickLog.suggestedNote)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(FOREST_PINE_COLOR)
            .setContentTitle("Catat Transaksi • ${quickLog.sourceApp}")
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$contentText\nKetuk untuk periksa nominal & simpan ke Saku."
                )
            )
            .setContentIntent(pendingIntent)
            .addAction(
                0,
                "Catat Sekarang",
                pendingIntent
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        runCatching {
            notificationManager.notify(requestCode, notification)
        }
    }
}
