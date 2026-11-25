package com.saku.v2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.saku.v2.MainActivity
import com.saku.v2.R
import com.saku.v2.SakuApplication
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuSeedData
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.TransactionType
import com.saku.v2.platform.ewallet.SakuIntentActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Home screen summary widget displaying total balance and up to 3 latest transactions.
 */
class SakuWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val store = (context.applicationContext as? SakuApplication)?.store
        if (store == null) {
            updateAppWidgets(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds,
                state = SakuSeedData.createDefaultState()
            )
            return
        }

        if (store.isLoaded.value) {
            updateAppWidgets(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds,
                state = store.state.value
            )
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(2_500L) {
                    store.isLoaded.first { it }
                }
                updateAppWidgets(
                    context = context,
                    appWidgetManager = appWidgetManager,
                    appWidgetIds = appWidgetIds,
                    state = store.state.value
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val REQ_OPEN_APP = 2001
        private const val REQ_QUICK_ADD = 2002

        private const val COLOR_INCOME = 0xFF2A6B57.toInt()
        private const val COLOR_EXPENSE = 0xFFB8563F.toInt()
        private const val COLOR_TRANSFER = 0xFF3D5473.toInt()
        private const val COLOR_MUTED = 0xFF5A5F66.toInt()

        private data class RowIds(
            val containerId: Int,
            val titleId: Int,
            val metaId: Int,
            val amountId: Int
        )

        private val txRowSlots = listOf(
            RowIds(R.id.widget_tx_1, R.id.widget_tx_1_title, R.id.widget_tx_1_meta, R.id.widget_tx_1_amount),
            RowIds(R.id.widget_tx_2, R.id.widget_tx_2_title, R.id.widget_tx_2_meta, R.id.widget_tx_2_amount),
            RowIds(R.id.widget_tx_3, R.id.widget_tx_3_title, R.id.widget_tx_3_meta, R.id.widget_tx_3_amount)
        )

        fun updateAppWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            state: SakuAppState
        ) {
            val masked = state.preferences.privacyBlur
            val totalBalance = SakuStateCalculator.totalBalance(state)
            val formattedBalance = SakuStateCalculator.formatRupiah(
                amount = totalBalance,
                masked = masked
            )

            val accountsById = state.accounts.associateBy { it.id }
            val categoriesById = state.categories.associateBy { it.id }
            val recentTransactions = state.transactions
                .sortedByDescending { it.timestampEpochMs }
                .take(txRowSlots.size)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                REQ_OPEN_APP,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                action = SakuIntentActions.ACTION_QUICK_ADD
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                REQ_QUICK_ADD,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_saku_summary)
                views.setTextViewText(R.id.widget_total_balance, formattedBalance)
                views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_btn_quick_add, quickAddPendingIntent)

                if (recentTransactions.isEmpty()) {
                    views.setViewVisibility(R.id.widget_tx_empty, View.VISIBLE)
                    txRowSlots.forEach { slot ->
                        views.setViewVisibility(slot.containerId, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_tx_empty, View.GONE)
                    txRowSlots.forEachIndexed { index, slot ->
                        val tx = recentTransactions.getOrNull(index)
                        if (tx == null) {
                            views.setViewVisibility(slot.containerId, View.GONE)
                        } else {
                            views.setViewVisibility(slot.containerId, View.VISIBLE)

                            val categoryName = tx.categoryId?.let { categoriesById[it]?.name }
                            val fromAccount = accountsById[tx.accountId]?.name ?: "Akun"
                            val toAccount = tx.toAccountId?.let { accountsById[it]?.name }

                            val title = tx.note.takeIf { it.isNotBlank() }
                                ?: categoryName
                                ?: when (tx.type) {
                                    TransactionType.TRANSFER -> "Transfer Saldo"
                                    TransactionType.INCOME -> "Pemasukan"
                                    TransactionType.EXPENSE -> "Pengeluaran"
                                }

                            val meta = when (tx.type) {
                                TransactionType.TRANSFER -> {
                                    if (!toAccount.isNullOrBlank()) "$fromAccount → $toAccount" else fromAccount
                                }
                                else -> {
                                    if (tx.note.isNotBlank() && !categoryName.isNullOrBlank()) {
                                        "$fromAccount • $categoryName"
                                    } else {
                                        fromAccount
                                    }
                                }
                            }

                            val amountText = if (masked) {
                                "Rp \u2022\u2022\u2022\u2022\u2022\u2022"
                            } else {
                                when (tx.type) {
                                    TransactionType.INCOME -> "+${SakuStateCalculator.formatRupiah(tx.amount)}"
                                    TransactionType.EXPENSE -> "-${SakuStateCalculator.formatRupiah(tx.amount)}"
                                    TransactionType.TRANSFER -> SakuStateCalculator.formatRupiah(tx.amount)
                                }
                            }

                            val amountColor = if (masked) {
                                COLOR_MUTED
                            } else {
                                when (tx.type) {
                                    TransactionType.INCOME -> COLOR_INCOME
                                    TransactionType.EXPENSE -> COLOR_EXPENSE
                                    TransactionType.TRANSFER -> COLOR_TRANSFER
                                }
                            }

                            views.setTextViewText(slot.titleId, title)
                            views.setTextViewText(slot.metaId, meta)
                            views.setTextViewText(slot.amountId, amountText)
                            views.setTextColor(slot.amountId, amountColor)
                        }
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
