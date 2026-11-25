package com.saku.v2.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

object SakuStateCalculator {

    private val idSymbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    /**
     * Calculates the current balance of [accountId] from its initial balance and all ledger entries.
     */
    fun accountBalance(state: SakuAppState, accountId: String): Long {
        val account = state.accounts.firstOrNull { it.id == accountId } ?: return 0L
        var balance = account.initialBalance
        for (tx in state.transactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    if (tx.accountId == accountId) balance += tx.amount
                }
                TransactionType.EXPENSE -> {
                    if (tx.accountId == accountId) balance -= tx.amount
                }
                TransactionType.TRANSFER -> {
                    if (tx.accountId == accountId) balance -= tx.amount
                    if (tx.toAccountId == accountId) balance += tx.amount
                }
            }
        }
        return balance
    }

    /**
     * Sum of balances across all non-archived accounts.
     * Invariant under transfers between active accounts because outgoing (-amount)
     * and incoming (+amount) cancel out cleanly.
     */
    fun totalBalance(state: SakuAppState): Long {
        return state.accounts
            .asSequence()
            .filter { !it.isArchived }
            .sumOf { accountBalance(state, it.id) }
    }

    /**
     * Total non-savings income recorded during [yearMonth].
     */
    fun monthlyIncome(
        state: SakuAppState,
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return state.transactions
            .asSequence()
            .filter {
                it.type == TransactionType.INCOME &&
                    it.categoryId != SYSTEM_CAT_SAVINGS_WITHDRAW &&
                    yearMonthOf(it.timestampEpochMs, zoneId) == yearMonth
            }
            .sumOf { it.amount }
    }

    /**
     * Total consumption expense recorded during [yearMonth] (excludes savings deposits).
     */
    fun monthlyExpense(
        state: SakuAppState,
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return state.transactions
            .asSequence()
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.categoryId != SYSTEM_CAT_SAVINGS_DEPOSIT &&
                    yearMonthOf(it.timestampEpochMs, zoneId) == yearMonth
            }
            .sumOf { it.amount }
    }

    /**
     * Per-category expense totals for [yearMonth] (excludes system savings transfers).
     */
    fun categorySpending(
        state: SakuAppState,
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<String, Long> {
        val result = linkedMapOf<String, Long>()
        for (tx in state.transactions) {
            val catId = tx.categoryId
            if (tx.type == TransactionType.EXPENSE &&
                catId != null &&
                catId != SYSTEM_CAT_SAVINGS_DEPOSIT &&
                yearMonthOf(tx.timestampEpochMs, zoneId) == yearMonth
            ) {
                result[catId] = (result[catId] ?: 0L) + tx.amount
            }
        }
        return result
    }

    /**
     * Current saved balance for [goalId], computed as linked deposits minus withdrawals (>= 0).
     */
    fun savingsGoalSavedAmount(state: SakuAppState, goalId: String): Long {
        var deposits = 0L
        var withdrawals = 0L
        for (tx in state.transactions) {
            if (tx.savingsGoalId == goalId) {
                when (tx.type) {
                    TransactionType.EXPENSE -> deposits += tx.amount
                    TransactionType.INCOME -> withdrawals += tx.amount
                    TransactionType.TRANSFER -> Unit
                }
            }
        }
        return (deposits - withdrawals).coerceAtLeast(0L)
    }

    /**
     * Aggregate saved amount across all savings goals.
     */
    fun totalSavingsAmount(state: SakuAppState): Long {
        return state.savingsGoals.sumOf { savingsGoalSavedAmount(state, it.id) }
    }

    /**
     * Formats an IDR integer [amount] using Indonesian thousands dots (e.g. `Rp 12.450.000`),
     * compact notation (`Rp 12,4 jt`), or privacy mask (`Rp ••••••`).
     */
    fun formatRupiah(
        amount: Long,
        compact: Boolean = false,
        masked: Boolean = false
    ): String {
        if (masked) return "Rp \u2022\u2022\u2022\u2022\u2022\u2022"

        val prefix = if (amount < 0L) "-Rp " else "Rp "
        val absAmount = abs(amount)

        if (!compact) {
            val formatted = DecimalFormat("#,##0", idSymbols).format(absAmount)
            return "$prefix$formatted"
        }

        return when {
            absAmount >= 1_000_000_000L -> {
                "$prefix${formatCompactDecimal(absAmount / 1_000_000_000.0)} M"
            }
            absAmount >= 1_000_000L -> {
                "$prefix${formatCompactDecimal(absAmount / 1_000_000.0)} jt"
            }
            absAmount >= 1_000L -> {
                "$prefix${formatCompactDecimal(absAmount / 1_000.0)} rb"
            }
            else -> "$prefix$absAmount"
        }
    }

    private fun formatCompactDecimal(value: Double): String {
        val formatter = DecimalFormat("0.#", idSymbols)
        return formatter.format(value)
    }

    private fun yearMonthOf(epochMs: Long, zoneId: ZoneId): YearMonth {
        return YearMonth.from(Instant.ofEpochMilli(epochMs).atZone(zoneId))
    }
}
