package com.saku.v2.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId

class SakuStateCalculatorTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun accountBalance_and_totalBalance_invariantUnderTransfer() {
        val accA = Account("a", "BCA", AccountType.BANK, 2_000_000L, "bank", 0xFF1E4D45)
        val accB = Account("b", "GoPay", AccountType.EWALLET, 500_000L, "ewallet", 0xFF2A6B57)
        val stateBefore = SakuAppState(
            accounts = listOf(accA, accB),
            categories = emptyList(),
            transactions = listOf(
                Transaction("t1", TransactionType.INCOME, 1_000_000L, "a", timestampEpochMs = 1_700_000_000_000L),
                Transaction("t2", TransactionType.EXPENSE, 250_000L, "b", timestampEpochMs = 1_700_000_100_000L)
            ),
            savingsGoals = emptyList()
        )
        val totalBefore = SakuStateCalculator.totalBalance(stateBefore)
        assertEquals(3_250_000L, totalBefore)

        val transfer = Transaction(
            id = "t3",
            type = TransactionType.TRANSFER,
            amount = 750_000L,
            accountId = "a",
            toAccountId = "b",
            timestampEpochMs = 1_700_000_200_000L
        )
        val stateAfter = stateBefore.copy(transactions = stateBefore.transactions + transfer)

        assertEquals(2_250_000L, SakuStateCalculator.accountBalance(stateAfter, "a"))
        assertEquals(1_000_000L, SakuStateCalculator.accountBalance(stateAfter, "b"))
        assertEquals(totalBefore, SakuStateCalculator.totalBalance(stateAfter))
    }

    @Test
    fun monthlyIncome_expense_and_categorySpending_excludeSystemSavingsTransfers() {
        val now = 1_763_942_400_000L // 2025-11-24 UTC
        val ym = YearMonth.from(java.time.Instant.ofEpochMilli(now).atZone(zone))
        val state = SakuAppState(
            accounts = listOf(Account("a", "BCA", AccountType.BANK, 5_000_000L, "bank", 0xFF1E4D45)),
            categories = emptyList(),
            transactions = listOf(
                Transaction("t1", TransactionType.INCOME, 4_000_000L, "a", categoryId = "cat_salary", timestampEpochMs = now),
                Transaction("t2", TransactionType.EXPENSE, 150_000L, "a", categoryId = "cat_food", timestampEpochMs = now),
                Transaction("t3", TransactionType.EXPENSE, 50_000L, "a", categoryId = "cat_food", timestampEpochMs = now),
                Transaction("t4", TransactionType.EXPENSE, 1_000_000L, "a", categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT, savingsGoalId = "g1", timestampEpochMs = now),
                Transaction("t5", TransactionType.INCOME, 200_000L, "a", categoryId = SYSTEM_CAT_SAVINGS_WITHDRAW, savingsGoalId = "g1", timestampEpochMs = now)
            ),
            savingsGoals = listOf(SavingsGoal("g1", "Darurat", 10_000_000L, null, null, 0xFF1E4D45, "shield", now))
        )

        assertEquals(4_000_000L, SakuStateCalculator.monthlyIncome(state, ym, zone))
        assertEquals(200_000L, SakuStateCalculator.monthlyExpense(state, ym, zone))
        assertEquals(mapOf("cat_food" to 200_000L), SakuStateCalculator.categorySpending(state, ym, zone))
        assertEquals(800_000L, SakuStateCalculator.savingsGoalSavedAmount(state, "g1"))
        assertEquals(800_000L, SakuStateCalculator.totalSavingsAmount(state))
    }

    @Test
    fun formatRupiah_formatsStandardCompactAndMasked() {
        assertEquals("Rp 12.450.000", SakuStateCalculator.formatRupiah(12_450_000L))
        assertEquals("-Rp 50.000", SakuStateCalculator.formatRupiah(-50_000L))
        assertEquals("Rp 12,4 jt", SakuStateCalculator.formatRupiah(12_400_000L, compact = true))
        assertEquals("Rp 5 jt", SakuStateCalculator.formatRupiah(5_000_000L, compact = true))
        assertEquals("Rp 150 rb", SakuStateCalculator.formatRupiah(150_000L, compact = true))
        assertEquals("Rp \u2022\u2022\u2022\u2022\u2022\u2022", SakuStateCalculator.formatRupiah(12_450_000L, masked = true))
    }

    @Test
    fun seedData_roundTripsSerializationAndHasConsistentBalances() {
        val seed = SakuSeedData.createDefaultState(nowEpochMs = 1_763_942_400_000L)
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val encoded = json.encodeToString(SakuAppState.serializer(), seed)
        val decoded = json.decodeFromString(SakuAppState.serializer(), encoded)

        assertEquals(seed, decoded)
        assertTrue(seed.accounts.size >= 5)
        assertTrue(seed.savingsGoals.size == 2)
        assertTrue(seed.transactions.size >= 6)
        assertTrue(SakuStateCalculator.totalBalance(seed) > 0L)
        assertTrue(SakuStateCalculator.totalSavingsAmount(seed) > 0L)
    }
}
