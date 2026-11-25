package com.saku.v2.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

const val SYSTEM_CAT_SAVINGS_DEPOSIT = "cat_savings_deposit"
const val SYSTEM_CAT_SAVINGS_WITHDRAW = "cat_savings_withdraw"

val DEFAULT_EWALLET_PACKAGES: Set<String> = setOf(
    "com.gojek.app",
    "id.dana",
    "ovo.id",
    "com.shopee.id",
    "com.linkaja.android"
)

@Serializable
data class SakuAppState(
    val schemaVersion: Int = 1,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val driveSyncMeta: DriveSyncMeta = DriveSyncMeta()
)

@Serializable
enum class AccountType {
    CASH,
    EWALLET,
    BANK,
    CUSTOM
}

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val iconKey: String,
    val colorHex: Long,
    val isArchived: Boolean = false
)

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

@Serializable
data class Category(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val colorHex: Long,
    val monthlyLimit: Long? = null,
    val isSystem: Boolean = false
)

@Serializable
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val savingsGoalId: String? = null,
    val note: String = "",
    val timestampEpochMs: Long,
    val receiptFileName: String? = null
)

@Serializable
data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Long,
    val coverFileName: String? = null,
    val deadlineEpochMs: Long? = null,
    val colorHex: Long,
    val iconKey: String,
    val createdAtEpochMs: Long
)

@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

@Serializable
data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val privacyBlur: Boolean = false,
    val monthlyBudget: Long = 5_000_000L,
    val eWalletListenerEnabled: Boolean = false,
    val enabledEWalletPackages: Set<String> = DEFAULT_EWALLET_PACKAGES
)

@Serializable
data class DriveSyncMeta(
    val connectedEmail: String? = null,
    val lastSyncedEpochMs: Long? = null,
    val autoSyncEnabled: Boolean = false
)

object SakuSeedData {
    private const val DAY_MS = 86_400_000L
    private const val HOUR_MS = 3_600_000L

    fun createDefaultState(nowEpochMs: Long = System.currentTimeMillis()): SakuAppState {
        val zone = ZoneId.systemDefault()
        val nowZoned = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
        val monthStartMs = nowZoned.withDayOfMonth(1).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

        fun recentInMonth(daysAgo: Long, hoursAgo: Long = 0L): Long {
            val candidate = nowEpochMs - (daysAgo * DAY_MS) - (hoursAgo * HOUR_MS)
            return candidate.coerceAtLeast(monthStartMs + HOUR_MS)
        }

        val accounts = listOf(
            Account(
                id = "acc_cash",
                name = "Tunai",
                type = AccountType.CASH,
                initialBalance = 450_000L,
                iconKey = "wallet",
                colorHex = 0xFF5C6B5E
            ),
            Account(
                id = "acc_gopay",
                name = "GoPay",
                type = AccountType.EWALLET,
                initialBalance = 680_000L,
                iconKey = "ewallet",
                colorHex = 0xFF2A6B57
            ),
            Account(
                id = "acc_ovo",
                name = "OVO",
                type = AccountType.EWALLET,
                initialBalance = 320_000L,
                iconKey = "ewallet",
                colorHex = 0xFF544B73
            ),
            Account(
                id = "acc_dana",
                name = "DANA",
                type = AccountType.EWALLET,
                initialBalance = 510_000L,
                iconKey = "ewallet",
                colorHex = 0xFF3D5473
            ),
            Account(
                id = "acc_bca",
                name = "BCA",
                type = AccountType.BANK,
                initialBalance = 4_850_000L,
                iconKey = "bank",
                colorHex = 0xFF1E4D45
            )
        )

        val categories = listOf(
            Category(
                id = "cat_food",
                name = "Makan & Minum",
                type = TransactionType.EXPENSE,
                iconKey = "restaurant",
                colorHex = 0xFFB8563F,
                monthlyLimit = 1_800_000L
            ),
            Category(
                id = "cat_transport",
                name = "Transportasi",
                type = TransactionType.EXPENSE,
                iconKey = "commute",
                colorHex = 0xFF3D5473,
                monthlyLimit = 750_000L
            ),
            Category(
                id = "cat_shopping",
                name = "Belanja",
                type = TransactionType.EXPENSE,
                iconKey = "shopping_bag",
                colorHex = 0xFFA87633,
                monthlyLimit = 1_000_000L
            ),
            Category(
                id = "cat_bills",
                name = "Tagihan",
                type = TransactionType.EXPENSE,
                iconKey = "receipt_long",
                colorHex = 0xFF6B5B4D,
                monthlyLimit = 1_200_000L
            ),
            Category(
                id = "cat_entertainment",
                name = "Hiburan",
                type = TransactionType.EXPENSE,
                iconKey = "movie",
                colorHex = 0xFF544B73,
                monthlyLimit = 500_000L
            ),
            Category(
                id = "cat_health",
                name = "Kesehatan",
                type = TransactionType.EXPENSE,
                iconKey = "favorite",
                colorHex = 0xFF2A6B57,
                monthlyLimit = 500_000L
            ),
            Category(
                id = "cat_other_expense",
                name = "Lainnya",
                type = TransactionType.EXPENSE,
                iconKey = "category",
                colorHex = 0xFF5A5F66
            ),
            Category(
                id = "cat_salary",
                name = "Gaji",
                type = TransactionType.INCOME,
                iconKey = "payments",
                colorHex = 0xFF1E4D45
            ),
            Category(
                id = "cat_bonus",
                name = "Bonus",
                type = TransactionType.INCOME,
                iconKey = "card_giftcard",
                colorHex = 0xFF2A6B57
            ),
            Category(
                id = "cat_other_income",
                name = "Lainnya",
                type = TransactionType.INCOME,
                iconKey = "trending_up",
                colorHex = 0xFF4A6B65
            ),
            Category(
                id = SYSTEM_CAT_SAVINGS_DEPOSIT,
                name = "Setor Tabungan",
                type = TransactionType.EXPENSE,
                iconKey = "savings",
                colorHex = 0xFF2A6B57,
                isSystem = true
            ),
            Category(
                id = SYSTEM_CAT_SAVINGS_WITHDRAW,
                name = "Tarik Tabungan",
                type = TransactionType.INCOME,
                iconKey = "savings",
                colorHex = 0xFF3D5473,
                isSystem = true
            )
        )

        val savingsGoals = listOf(
            SavingsGoal(
                id = "goal_emergency",
                name = "Dana Darurat",
                targetAmount = 20_000_000L,
                coverFileName = null,
                deadlineEpochMs = nowEpochMs + (180L * DAY_MS),
                colorHex = 0xFF1E4D45,
                iconKey = "shield",
                createdAtEpochMs = nowEpochMs - (45L * DAY_MS)
            ),
            SavingsGoal(
                id = "goal_kyoto",
                name = "Liburan Kyoto",
                targetAmount = 15_000_000L,
                coverFileName = null,
                deadlineEpochMs = nowEpochMs + (270L * DAY_MS),
                colorHex = 0xFF3D5473,
                iconKey = "flight",
                createdAtEpochMs = nowEpochMs - (20L * DAY_MS)
            )
        )

        val transactions = listOf(
            Transaction(
                id = "tx_seed_1",
                type = TransactionType.INCOME,
                amount = 8_500_000L,
                accountId = "acc_bca",
                categoryId = "cat_salary",
                note = "Gaji Bulanan",
                timestampEpochMs = recentInMonth(daysAgo = 5, hoursAgo = 2)
            ),
            Transaction(
                id = "tx_seed_2",
                type = TransactionType.EXPENSE,
                amount = 2_000_000L,
                accountId = "acc_bca",
                categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT,
                savingsGoalId = "goal_emergency",
                note = "Sisihan Dana Darurat",
                timestampEpochMs = recentInMonth(daysAgo = 4, hoursAgo = 5)
            ),
            Transaction(
                id = "tx_seed_3",
                type = TransactionType.EXPENSE,
                amount = 1_250_000L,
                accountId = "acc_bca",
                categoryId = SYSTEM_CAT_SAVINGS_DEPOSIT,
                savingsGoalId = "goal_kyoto",
                note = "Tabungan Tiket Kyoto",
                timestampEpochMs = recentInMonth(daysAgo = 4, hoursAgo = 3)
            ),
            Transaction(
                id = "tx_seed_4",
                type = TransactionType.TRANSFER,
                amount = 500_000L,
                accountId = "acc_bca",
                toAccountId = "acc_gopay",
                note = "Top up GoPay mingguan",
                timestampEpochMs = recentInMonth(daysAgo = 3, hoursAgo = 4)
            ),
            Transaction(
                id = "tx_seed_5",
                type = TransactionType.EXPENSE,
                amount = 420_000L,
                accountId = "acc_bca",
                categoryId = "cat_bills",
                note = "Internet & Listrik",
                timestampEpochMs = recentInMonth(daysAgo = 2, hoursAgo = 6)
            ),
            Transaction(
                id = "tx_seed_6",
                type = TransactionType.EXPENSE,
                amount = 85_000L,
                accountId = "acc_gopay",
                categoryId = "cat_food",
                note = "Makan siang & Kopi",
                timestampEpochMs = recentInMonth(daysAgo = 1, hoursAgo = 5)
            ),
            Transaction(
                id = "tx_seed_7",
                type = TransactionType.EXPENSE,
                amount = 48_000L,
                accountId = "acc_ovo",
                categoryId = "cat_transport",
                note = "Grab & Parkir MRT",
                timestampEpochMs = recentInMonth(daysAgo = 0, hoursAgo = 4)
            ),
            Transaction(
                id = "tx_seed_8",
                type = TransactionType.EXPENSE,
                amount = 165_000L,
                accountId = "acc_dana",
                categoryId = "cat_shopping",
                note = "Kebutuhan rumah tangga",
                timestampEpochMs = recentInMonth(daysAgo = 0, hoursAgo = 1)
            )
        )

        return SakuAppState(
            schemaVersion = 1,
            updatedAtEpochMs = nowEpochMs,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            savingsGoals = savingsGoals,
            preferences = AppPreferences(),
            driveSyncMeta = DriveSyncMeta()
        )
    }
}
