package com.saku.v2

import com.saku.v2.data.model.Account
import com.saku.v2.data.model.AccountType
import com.saku.v2.data.model.Category
import com.saku.v2.data.model.SYSTEM_CAT_SAVINGS_DEPOSIT
import com.saku.v2.data.model.SYSTEM_CAT_SAVINGS_WITHDRAW
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.SavingsGoal
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class SakuFinancialRulesTest {

    private val jakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")

    @Test
    fun accountBalance_handlesIncomeExpenseAndTransfers() {
        val cash = Account("acc_cash", "Tunai", AccountType.CASH, 200_000L, "wallet", 0xFF5C6B5E)
        val gopay = Account("acc_gopay", "GoPay", AccountType.EWALLET, 500_000L, "ewallet", 0xFF2A6B57)
        val bca = Account("acc_bca", "BCA", AccountType.BANK, 3_000_000L, "bank", 0xFF1E4D45)

        val state = SakuAppState(
            accounts = listOf(cash, gopay, bca),
            transactions = listOf(
                Transaction("t1", TransactionType.INCOME, 1_500_000L, "acc_bca", categoryId = "cat_salary", timestampEpochMs = 1_000L),
                Transaction("t2", TransactionType.EXPENSE, 75_000L, "acc_cash", categoryId = "cat_food", timestampEpochMs = 2_000L),
                Transaction("t3", TransactionType.TRANSFER, 400_000L, "acc_bca", toAccountId = "acc_gopay", timestampEpochMs = 3_000L),
                Transaction("t4", TransactionType.TRANSFER, 150_000L, "acc_gopay", toAccountId = "acc_cash", timestampEpochMs = 4_000L),
                Transaction("t5", TransactionType.EXPENSE, 60_000L, "acc_gopay", categoryId = "cat_food", timestampEpochMs = 5_000L)
            )
        )

        // cash: 200k - 75k + 150k = 275k
        assertEquals(275_000L, SakuStateCalculator.accountBalance(state, "acc_cash"))
        // gopay: 500k + 400k - 150k - 60k = 690k
        assertEquals(690_000L, SakuStateCalculator.accountBalance(state, "acc_gopay"))
        // bca: 3000k + 1500k - 400k = 4100k
        assertEquals(4_100_000L, SakuStateCalculator.accountBalance(state, "acc_bca"))
        // unknown account returns 0
        assertEquals(0L, SakuStateCalculator.accountBalance(state, "acc_unknown"))
    }

    @Test
    fun totalBalance_isInvariantUnderTransfers_andExcludesArchivedAccounts() {
        val bca = Account("acc_bca", "BCA", AccountType.BANK, 5_000_000L, "bank", 0xFF1E4D45)
        val ovo = Account("acc_ovo", "OVO", AccountType.EWALLET, 300_000L, "ewallet", 0xFF544B73)
        val oldAcc = Account("acc_old", "Lama", AccountType.CUSTOM, 999_000L, "wallet", 0xFF5A5F66, isArchived = true)

        val initialState = SakuAppState(
            accounts = listOf(bca, ovo, oldAcc),
            transactions = listOf(
                Transaction("t1", TransactionType.INCOME, 1_000_000L, "acc_bca", timestampEpochMs = 1_000L),
                Transaction("t2", TransactionType.EXPENSE, 100_000L, "acc_ovo", timestampEpochMs = 2_000L)
            )
        )

        val totalBefore = SakuStateCalculator.totalBalance(initialState)
        assertEquals(6_200_000L, totalBefore)
        assertEquals(999_000L, SakuStateCalculator.accountBalance(initialState, "acc_old"))

        val afterTransfers = initialState.copy(
            transactions = initialState.transactions + listOf(
                Transaction("tr1", TransactionType.TRANSFER, 1_250_000L, "acc_bca", toAccountId = "acc_ovo", timestampEpochMs = 3_000L),
                Transaction("tr2", TransactionType.TRANSFER, 450_000L, "acc_ovo", toAccountId = "acc_bca", timestampEpochMs = 4_000L)
            )
        )

        assertEquals(5_200_000L, SakuStateCalculator.accountBalance(afterTransfers, "acc_bca"))
        assertEquals(1_000_000L, SakuStateCalculator.accountBalance(afterTransfers, "acc_ovo"))
        assertEquals(totalBefore, SakuStateCalculator.totalBalance(afterTransfers))
    }

    @Test
    fun savingsGoal_depositWithdrawAndDeletion_updateSavedAmountAndAccountBalanceAccurately() {
        val bca = Account("acc_bca", "BCA", AccountType.BANK, 10_000_000L, "bank", 0xFF1E4D45)
        val cash = Account("acc_cash", "Tunai", AccountType.CASH, 200_000L, "wallet", 0xFF5C6B5E)
        val goalEmergency = SavingsGoal("goal_1", "Dana Darurat", 20_000_000L, null, null, 0xFF1E4D45, "shield", 1_000L)
        val goalTravel = SavingsGoal("goal_2", "Liburan", 8_000_000L, null, null, 0xFF3D5473, "flight", 1_000L)

        val deposit1 = Transaction(
            id = "tx_dep_1",
            type = TransactionType.EXPENSE,
            amount = 2_000_000L,
            accountId = "acc_bca",
            categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT,
            savingsGoalId = "goal_1",
            timestampEpochMs = 2_000L
        )
        val deposit2 = Transaction(
            id = "tx_dep_2",
            type = TransactionType.EXPENSE,
            amount = 1_500_000L,
            accountId = "acc_bca",
            categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT,
            savingsGoalId = "goal_1",
            timestampEpochMs = 3_000L
        )
        val depositGoal2 = Transaction(
            id = "tx_dep_3",
            type = TransactionType.EXPENSE,
            amount = 750_000L,
            accountId = "acc_bca",
            categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT,
            savingsGoalId = "goal_2",
            timestampEpochMs = 3_500L
        )
        val withdraw1 = Transaction(
            id = "tx_wd_1",
            type = TransactionType.INCOME,
            amount = 500_000L,
            accountId = "acc_cash",
            categoryId = SYSTEM_CAT_SAVINGS_WITHDRAW,
            savingsGoalId = "goal_1",
            timestampEpochMs = 4_000L
        )
        val unrelatedTransfer = Transaction(
            id = "tx_tr_ignore",
            type = TransactionType.TRANSFER,
            amount = 300_000L,
            accountId = "acc_bca",
            toAccountId = "acc_cash",
            savingsGoalId = "goal_1",
            timestampEpochMs = 4_500L
        )

        val stateWithSavings = SakuAppState(
            accounts = listOf(bca, cash),
            savingsGoals = listOf(goalEmergency, goalTravel),
            transactions = listOf(deposit1, deposit2, depositGoal2, withdraw1, unrelatedTransfer)
        )

        assertEquals(3_000_000L, SakuStateCalculator.savingsGoalSavedAmount(stateWithSavings, "goal_1"))
        assertEquals(750_000L, SakuStateCalculator.savingsGoalSavedAmount(stateWithSavings, "goal_2"))
        assertEquals(3_750_000L, SakuStateCalculator.totalSavingsAmount(stateWithSavings))
        assertEquals(5_450_000L, SakuStateCalculator.accountBalance(stateWithSavings, "acc_bca"))
        assertEquals(1_000_000L, SakuStateCalculator.accountBalance(stateWithSavings, "acc_cash"))

        // Deleting deposit2 should reduce goal_1 saved amount by 1.5M and restore BCA balance by 1.5M
        val stateAfterDeletingDeposit = stateWithSavings.copy(
            transactions = stateWithSavings.transactions.filterNot { it.id == "tx_dep_2" }
        )
        assertEquals(1_500_000L, SakuStateCalculator.savingsGoalSavedAmount(stateAfterDeletingDeposit, "goal_1"))
        assertEquals(2_250_000L, SakuStateCalculator.totalSavingsAmount(stateAfterDeletingDeposit))
        assertEquals(6_950_000L, SakuStateCalculator.accountBalance(stateAfterDeletingDeposit, "acc_bca"))

        // If withdrawal exceeds remaining deposits, saved amount clamps safely at 0
        val stateOverWithdrawn = stateAfterDeletingDeposit.copy(
            transactions = stateAfterDeletingDeposit.transactions + Transaction(
                id = "tx_wd_2",
                type = TransactionType.INCOME,
                amount = 2_500_000L,
                accountId = "acc_cash",
                categoryId = SYSTEM_CAT_SAVINGS_WITHDRAW,
                savingsGoalId = "goal_1",
                timestampEpochMs = 5_000L
            )
        )
        assertEquals(0L, SakuStateCalculator.savingsGoalSavedAmount(stateOverWithdrawn, "goal_1"))
    }

    @Test
    fun monthlyIncomeAndExpense_excludeSystemSavingsCategoriesAndRespectMonthBoundaries() {
        val novStartMs = ZonedDateTime.of(2026, 11, 1, 0, 15, 0, 0, jakartaZone)
            .toInstant().toEpochMilli()
        val novMidMs = ZonedDateTime.of(2026, 11, 15, 12, 0, 0, 0, jakartaZone)
            .toInstant().toEpochMilli()
        val octEndMs = ZonedDateTime.of(2026, 10, 31, 23, 59, 0, 0, jakartaZone)
            .toInstant().toEpochMilli()
        val decStartMs = ZonedDateTime.of(2026, 12, 1, 0, 5, 0, 0, jakartaZone)
            .toInstant().toEpochMilli()

        val targetMonth = YearMonth.of(2026, 11)
        val state = SakuAppState(
            accounts = listOf(Account("acc_bca", "BCA", AccountType.BANK, 5_000_000L, "bank", 0xFF1E4D45)),
            categories = listOf(
                Category("cat_salary", "Gaji", TransactionType.INCOME, "payments", 0xFF1E4D45),
                Category("cat_food", "Makan", TransactionType.EXPENSE, "restaurant", 0xFFB8563F),
                Category("cat_transport", "Transport", TransactionType.EXPENSE, "commute", 0xFF3D5473)
            ),
            transactions = listOf(
                Transaction("t1", TransactionType.INCOME, 9_000_000L, "acc_bca", categoryId = "cat_salary", timestampEpochMs = novStartMs),
                Transaction("t2", TransactionType.INCOME, 500_000L, "acc_bca", categoryId = SYSTEM_CAT_SAVINGS_WITHDRAW, savingsGoalId = "g1", timestampEpochMs = novMidMs),
                Transaction("t3", TransactionType.EXPENSE, 120_000L, "acc_bca", categoryId = "cat_food", timestampEpochMs = novMidMs),
                Transaction("t4", TransactionType.EXPENSE, 80_000L, "acc_bca", categoryId = "cat_food", timestampEpochMs = novMidMs + 1000L),
                Transaction("t5", TransactionType.EXPENSE, 45_000L, "acc_bca", categoryId = "cat_transport", timestampEpochMs = novMidMs + 2000L),
                Transaction("t6", TransactionType.EXPENSE, 2_000_000L, "acc_bca", categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT, savingsGoalId = "g1", timestampEpochMs = novMidMs),
                Transaction("t7", TransactionType.TRANSFER, 300_000L, "acc_bca", toAccountId = "acc_cash", timestampEpochMs = novMidMs),
                // Adjacent months must be excluded
                Transaction("t8", TransactionType.INCOME, 1_000_000L, "acc_bca", categoryId = "cat_salary", timestampEpochMs = octEndMs),
                Transaction("t9", TransactionType.EXPENSE, 900_000L, "acc_bca", categoryId = "cat_food", timestampEpochMs = decStartMs)
            )
        )

        assertEquals(9_000_000L, SakuStateCalculator.monthlyIncome(state, targetMonth, jakartaZone))
        assertEquals(245_000L, SakuStateCalculator.monthlyExpense(state, targetMonth, jakartaZone))
        assertEquals(
            mapOf("cat_food" to 200_000L, "cat_transport" to 45_000L),
            SakuStateCalculator.categorySpending(state, targetMonth, jakartaZone)
        )
        assertEquals(YearMonth.from(Instant.ofEpochMilli(octEndMs).atZone(jakartaZone)), YearMonth.of(2026, 10))
    }

    @Test
    fun formatRupiah_formatsFullCompactAndMaskedProperly() {
        // Full format
        assertEquals("Rp 0", SakuStateCalculator.formatRupiah(0L))
        assertEquals("Rp 1.500", SakuStateCalculator.formatRupiah(1_500L))
        assertEquals("Rp 12.450.000", SakuStateCalculator.formatRupiah(12_450_000L))
        assertEquals("-Rp 75.000", SakuStateCalculator.formatRupiah(-75_000L))

        // Compact format
        assertEquals("Rp 500", SakuStateCalculator.formatRupiah(500L, compact = true))
        assertEquals("Rp 1,5 rb", SakuStateCalculator.formatRupiah(1_500L, compact = true))
        assertEquals("Rp 150 rb", SakuStateCalculator.formatRupiah(150_000L, compact = true))
        assertEquals("Rp 5 jt", SakuStateCalculator.formatRupiah(5_000_000L, compact = true))
        assertEquals("Rp 12,4 jt", SakuStateCalculator.formatRupiah(12_400_000L, compact = true))
        assertEquals("Rp 1,2 M", SakuStateCalculator.formatRupiah(1_200_000_000L, compact = true))
        assertEquals("-Rp 2,5 jt", SakuStateCalculator.formatRupiah(-2_500_000L, compact = true))

        // Masked format
        val expectedMask = "Rp \u2022\u2022\u2022\u2022\u2022\u2022"
        assertEquals(expectedMask, SakuStateCalculator.formatRupiah(0L, masked = true))
        assertEquals(expectedMask, SakuStateCalculator.formatRupiah(12_450_000L, compact = false, masked = true))
        assertEquals(expectedMask, SakuStateCalculator.formatRupiah(-999_000L, compact = true, masked = true))
    }
}
