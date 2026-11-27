package com.saku.v2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Commute
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saku.v2.data.model.Account
import com.saku.v2.data.model.AccountType
import com.saku.v2.data.model.Category
import com.saku.v2.data.model.SavingsGoal
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import com.saku.v2.ui.theme.SakuColorTokens
import com.saku.v2.ui.theme.SakuTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun sakuIconForKey(key: String): ImageVector = when (key.lowercase(Locale.ROOT)) {
    "wallet" -> Icons.Rounded.AccountBalanceWallet
    "ewallet" -> Icons.Rounded.Smartphone
    "bank" -> Icons.Rounded.AccountBalance
    "restaurant" -> Icons.Rounded.Restaurant
    "commute" -> Icons.Rounded.Commute
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "receipt_long" -> Icons.AutoMirrored.Rounded.ReceiptLong
    "movie" -> Icons.Rounded.Movie
    "favorite" -> Icons.Rounded.Favorite
    "category" -> Icons.Rounded.Category
    "payments" -> Icons.Rounded.Payments
    "card_giftcard" -> Icons.Rounded.CardGiftcard
    "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
    "savings" -> Icons.Rounded.Savings
    "flight" -> Icons.Rounded.Flight
    "home" -> Icons.Rounded.Home
    "shield" -> Icons.Rounded.Shield
    "swap_horiz" -> Icons.Rounded.SwapHoriz
    "laptop" -> Icons.Rounded.Laptop
    "school" -> Icons.Rounded.School
    "car" -> Icons.Rounded.DirectionsCar
    else -> Icons.Rounded.Category
}

internal fun liftAccentForTheme(color: Color, isDark: Boolean): Color {
    val lum = color.luminance()
    return if (isDark && lum < 0.25f) {
        val lift = ((0.30f - lum) / 0.30f).coerceIn(0.22f, 0.48f)
        lerp(color, Color(0xFFF1EFEA), lift)
    } else {
        color
    }
}

@Composable
fun SakuTintedIconBadge(
    iconKey: String,
    tint: Color,
    containerColor: Color,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    @Suppress("UNUSED_VARIABLE")
    val unusedContainer = containerColor
    val solidBg = liftAccentForTheme(tint, colors.isDark).copy(alpha = 1f)
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(solidBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = sakuIconForKey(iconKey),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun SakuEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    val badgeShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(badgeShape)
                .background(colors.primaryContainer)
                .border(0.75.dp, colors.primary.copy(alpha = 0.16f), badgeShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

internal val IndonesianLocale: Locale = Locale.forLanguageTag("id-ID")

private val HeaderDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", IndonesianLocale)

private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", IndonesianLocale)

private val DateTimeFormatterId: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy • HH:mm", IndonesianLocale)

private val TimeOnlyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", IndonesianLocale)

private val MonthYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", IndonesianLocale)

internal fun formatIndonesianHeaderDate(
    epochMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val raw = Instant.ofEpochMilli(epochMs).atZone(zoneId).format(HeaderDateFormatter)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(IndonesianLocale) else it.toString() }
}

internal fun formatIndonesianShortDate(
    epochMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String = Instant.ofEpochMilli(epochMs).atZone(zoneId).format(ShortDateFormatter)

internal fun formatIndonesianDateTime(
    epochMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String = Instant.ofEpochMilli(epochMs).atZone(zoneId).format(DateTimeFormatterId)

internal fun formatIndonesianTime(
    epochMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String = Instant.ofEpochMilli(epochMs).atZone(zoneId).format(TimeOnlyFormatter)

internal fun formatMonthYear(yearMonth: YearMonth): String {
    val raw = yearMonth.atDay(1).format(MonthYearFormatter)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(IndonesianLocale) else it.toString() }
}

internal fun formatRelativeDateHeader(
    date: LocalDate,
    today: LocalDate = LocalDate.now()
): String = when (date) {
    today -> "Hari Ini"
    today.minusDays(1) -> "Kemarin"
    else -> date.format(ShortDateFormatter)
}

internal fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "Tunai"
    AccountType.EWALLET -> "E-Wallet"
    AccountType.BANK -> "Bank"
    AccountType.CUSTOM -> "Lainnya"
}

internal fun transactionTypeLabel(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "Keluar"
    TransactionType.INCOME -> "Masuk"
    TransactionType.TRANSFER -> "Transfer"
}

internal fun findBestMatchingAccount(
    accounts: List<Account>,
    suggestedName: String?
): Account? {
    val active = accounts.filter { !it.isArchived }
    if (active.isEmpty()) return null
    val query = suggestedName?.trim()?.lowercase(IndonesianLocale).orEmpty()
    if (query.isNotEmpty()) {
        active.firstOrNull { it.name.trim().lowercase(IndonesianLocale) == query }?.let { return it }
        active.firstOrNull {
            val candidate = it.name.trim().lowercase(IndonesianLocale)
            candidate.contains(query) || query.contains(candidate)
        }?.let { return it }
    }
    return active.firstOrNull { it.type == AccountType.EWALLET } ?: active.first()
}

internal data class TxDisplayMeta(
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val tint: Color,
    val containerColor: Color,
    val amountColor: Color,
    val amountPrefix: String,
    val linkedGoalName: String?
)

internal fun resolveTxDisplayMeta(
    tx: Transaction,
    accountsById: Map<String, Account>,
    categoriesById: Map<String, Category>,
    goalsById: Map<String, SavingsGoal>,
    colors: SakuColorTokens
): TxDisplayMeta {
    val sourceAccount = accountsById[tx.accountId]
    val destAccount = tx.toAccountId?.let { accountsById[it] }
    val category = tx.categoryId?.let { categoriesById[it] }
    val linkedGoal = tx.savingsGoalId?.let { goalsById[it] }

    val sourceName = sourceAccount?.name ?: "Akun"
    val destName = destAccount?.name ?: "Akun Tujuan"

    return when (tx.type) {
        TransactionType.TRANSFER -> {
            val transferRoute = "$sourceName \u2192 $destName"
            TxDisplayMeta(
                title = tx.note.ifBlank { "Transfer Saldo" },
                subtitle = transferRoute,
                iconKey = "swap_horiz",
                tint = colors.transfer,
                containerColor = colors.transferContainer,
                amountColor = colors.transfer,
                amountPrefix = "",
                linkedGoalName = linkedGoal?.name
            )
        }
        TransactionType.INCOME -> {
            val catName = category?.name ?: "Pemasukan"
            val tint = category?.let { Color(it.colorHex) } ?: colors.income
            TxDisplayMeta(
                title = tx.note.ifBlank { catName },
                subtitle = "$sourceName • $catName",
                iconKey = category?.iconKey ?: "trending_up",
                tint = tint,
                containerColor = colors.incomeContainer,
                amountColor = colors.income,
                amountPrefix = "+",
                linkedGoalName = linkedGoal?.name
            )
        }
        TransactionType.EXPENSE -> {
            val catName = category?.name ?: "Pengeluaran"
            val tint = category?.let { Color(it.colorHex) } ?: colors.expense
            TxDisplayMeta(
                title = tx.note.ifBlank { catName },
                subtitle = "$sourceName • $catName",
                iconKey = category?.iconKey ?: "category",
                tint = tint,
                containerColor = colors.expenseContainer,
                amountColor = colors.expense,
                amountPrefix = "-",
                linkedGoalName = linkedGoal?.name
            )
        }
    }
}
