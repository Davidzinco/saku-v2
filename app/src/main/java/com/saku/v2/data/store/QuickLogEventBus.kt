package com.saku.v2.data.store

import com.saku.v2.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Parsed e-wallet transaction suggestion waiting to prefill the quick-log sheet.
 */
data class PendingQuickLog(
    val id: String,
    val sourceApp: String,
    val suggestedAccountName: String?,
    val amount: Long,
    val type: TransactionType,
    val suggestedNote: String,
    val timestampEpochMs: Long
)

/**
 * Process-local event bus bridging e-wallet notifications / quick-log triggers into the UI.
 */
object QuickLogEventBus {
    private val _pending = MutableStateFlow<PendingQuickLog?>(null)
    val pending: StateFlow<PendingQuickLog?> = _pending.asStateFlow()

    fun emit(log: PendingQuickLog) {
        _pending.value = log
    }

    fun consume(): PendingQuickLog? {
        return _pending.getAndUpdate { null }
    }

    fun dismiss() {
        _pending.value = null
    }
}
