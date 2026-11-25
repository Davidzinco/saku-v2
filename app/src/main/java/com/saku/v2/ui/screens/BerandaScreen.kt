package com.saku.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import com.saku.v2.data.store.PendingQuickLog
import com.saku.v2.ui.components.SakuEmptyState
import com.saku.v2.ui.components.SakuTintedIconBadge
import com.saku.v2.ui.components.formatIndonesianHeaderDate
import com.saku.v2.ui.components.formatIndonesianShortDate
import com.saku.v2.ui.components.formatIndonesianTime
import com.saku.v2.ui.components.liftAccentForTheme
import com.saku.v2.ui.components.resolveTxDisplayMeta
import com.saku.v2.ui.liquidglass.LiquidGlassButton
import com.saku.v2.ui.liquidglass.LiquidGlassCard
import com.saku.v2.ui.theme.SakuTheme
import com.saku.v2.ui.theme.tabularNums
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun BerandaScreen(
    state: SakuAppState,
    pendingQuickLog: PendingQuickLog?,
    mediaManager: MediaManagerContract,
    onTogglePrivacyBlur: () -> Unit,
    onOpenNewTransaction: (TransactionType) -> Unit,
    onOpenQuickLogForm: (PendingQuickLog) -> Unit,
    onDismissQuickLog: () -> Unit,
    onOpenTransactionDetail: (Transaction) -> Unit,
    onNavigateToTransaksi: () -> Unit,
    onNavigateToNabung: () -> Unit,
    onNavigateToPengaturan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    val privacyBlur = state.preferences.privacyBlur
    val currentMonth = remember { YearMonth.now() }
    val zoneId = remember { ZoneId.systemDefault() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val totalBalance = remember(state) { SakuStateCalculator.totalBalance(state) }
    val monthIncome = remember(state, currentMonth) {
        SakuStateCalculator.monthlyIncome(state, currentMonth, zoneId)
    }
    val monthExpense = remember(state, currentMonth) {
        SakuStateCalculator.monthlyExpense(state, currentMonth, zoneId)
    }
    val netCashflow = monthIncome - monthExpense
    val totalSavings = remember(state) { SakuStateCalculator.totalSavingsAmount(state) }

    val activeAccounts = remember(state.accounts) {
        state.accounts.filter { !it.isArchived }
    }
    val accountsById = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val goalsById = remember(state.savingsGoals) { state.savingsGoals.associateBy { it.id } }

    val recentTransactions = remember(state.transactions) {
        state.transactions
            .sortedByDescending { it.timestampEpochMs }
            .take(5)
    }

    val featuredGoal = remember(state.savingsGoals, state.transactions) {
        val withSaved = state.savingsGoals.map { goal ->
            goal to SakuStateCalculator.savingsGoalSavedAmount(state, goal.id)
        }
        withSaved.firstOrNull { (goal, saved) -> saved < goal.targetAmount }
            ?: withSaved.firstOrNull()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = statusBarTop + 14.dp,
            bottom = 124.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Header
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Saku",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = formatIndonesianHeaderDate(),
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = colors.textSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDriveConnected = !state.driveSyncMeta.connectedEmail.isNullOrBlank()
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.glassSurfaceFallback)
                            .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(999.dp))
                            .clickable(onClick = onNavigateToPengaturan)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDriveConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                            contentDescription = "Status Google Drive",
                            tint = if (isDriveConnected) colors.income else colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isDriveConnected) "Tersinkron" else "Drive",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDriveConnected) colors.income else colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.glassSurfaceFallback)
                            .border(0.75.dp, colors.cardBorderSubtle, CircleShape)
                            .clickable(onClick = onTogglePrivacyBlur),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (privacyBlur) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "Privacy Mode",
                            tint = if (privacyBlur) colors.primary else colors.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // Hero Liquid Glass Balance Card
        item(key = "hero_balance") {
            LiquidGlassCard(
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL SALDO",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
                        color = colors.textSecondary
                    )

                    val isPositiveNet = netCashflow >= 0L
                    val deltaBg = if (isPositiveNet) colors.incomeContainer else colors.expenseContainer
                    val deltaTint = if (isPositiveNet) colors.income else colors.expense
                    val deltaPrefix = if (netCashflow > 0L) "+" else ""
                    val deltaText = if (privacyBlur) {
                        "Rp \u2022\u2022\u2022\u2022"
                    } else {
                        "$deltaPrefix${SakuStateCalculator.formatRupiah(netCashflow, compact = true)}"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(deltaBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Cashflow $deltaText",
                            style = MaterialTheme.typography.labelSmall.tabularNums,
                            color = deltaTint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = SakuStateCalculator.formatRupiah(
                        amount = totalBalance,
                        masked = privacyBlur
                    ),
                    style = MaterialTheme.typography.displayLarge.tabularNums.copy(fontSize = 32.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (activeAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.cardBorderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeAccounts, key = { it.id }) { account ->
                            val balance = SakuStateCalculator.accountBalance(state, account.id)
                            val accTint = Color(account.colorHex)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        colors.sheetSurface.copy(alpha = if (colors.isDark) 0.52f else 0.72f)
                                    )
                                    .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 11.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SakuTintedIconBadge(
                                    iconKey = account.iconKey,
                                    tint = accTint,
                                    containerColor = accTint.copy(alpha = 0.14f),
                                    size = 28.dp
                                )
                                Column {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = SakuStateCalculator.formatRupiah(
                                            amount = balance,
                                            masked = privacyBlur
                                        ),
                                        style = MaterialTheme.typography.labelLarge.tabularNums,
                                        color = colors.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3 Interactive Liquid Glass Action Buttons
        item(key = "quick_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassButton(
                    onClick = { onOpenNewTransaction(TransactionType.EXPENSE) },
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = colors.expense,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Keluar",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                LiquidGlassButton(
                    onClick = { onOpenNewTransaction(TransactionType.INCOME) },
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = colors.income,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Masuk",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                LiquidGlassButton(
                    onClick = { onOpenNewTransaction(TransactionType.TRANSFER) },
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = null,
                        tint = colors.transfer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Pindah",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Contextual Quick-Log Banner
        if (pendingQuickLog != null) {
            item(key = "quick_log_banner") {
                LiquidGlassCard(
                    shape = RoundedCornerShape(22.dp),
                    tintColor = colors.primaryContainer.copy(alpha = if (colors.isDark) 0.72f else 0.84f),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Quick-Log \u2022 ${pendingQuickLog.sourceApp}",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary
                            )
                        }
                        IconButton(
                            onClick = onDismissQuickLog,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Tutup Quick-Log",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = SakuStateCalculator.formatRupiah(
                                    amount = pendingQuickLog.amount,
                                    masked = privacyBlur
                                ),
                                style = MaterialTheme.typography.titleLarge.tabularNums,
                                color = colors.textPrimary
                            )
                            Text(
                                text = pendingQuickLog.suggestedNote.ifBlank { "Transaksi e-wallet terdeteksi" },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        LiquidGlassButton(
                            onClick = { onOpenQuickLogForm(pendingQuickLog) },
                            isPrimary = true
                        ) {
                            Text(
                                text = "Catat",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2-Column Asymmetric Bento Row
        item(key = "bento_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Bento: Cashflow & Budget
                val budget = state.preferences.monthlyBudget
                val budgetRatio = if (budget > 0L) (monthExpense.toFloat() / budget.toFloat()) else 0f
                val clampedRatio = budgetRatio.coerceIn(0f, 1f)
                val barTint = when {
                    budgetRatio >= 0.9f -> colors.expense
                    budgetRatio >= 0.7f -> colors.warning
                    else -> colors.primary
                }

                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(16.dp),
                    onClick = onNavigateToTransaksi,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Cashflow & Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = SakuStateCalculator.formatRupiah(
                            amount = monthExpense,
                            compact = true,
                            masked = privacyBlur
                        ),
                        style = MaterialTheme.typography.titleLarge.tabularNums,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = if (budget > 0L) {
                            "dari ${SakuStateCalculator.formatRupiah(budget, compact = true, masked = privacyBlur)}"
                        } else {
                            "Budget belum diatur"
                        },
                        style = MaterialTheme.typography.labelSmall.tabularNums,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.cardBorderSubtle)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (budget > 0L) clampedRatio else 0f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(barTint)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Masuk",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = SakuStateCalculator.formatRupiah(
                                    amount = monthIncome,
                                    compact = true,
                                    masked = privacyBlur
                                ),
                                style = MaterialTheme.typography.labelSmall.tabularNums,
                                color = colors.income
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Keluar",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = SakuStateCalculator.formatRupiah(
                                    amount = monthExpense,
                                    compact = true,
                                    masked = privacyBlur
                                ),
                                style = MaterialTheme.typography.labelSmall.tabularNums,
                                color = colors.expense
                            )
                        }
                    }
                }

                // Right Bento: Progres Nabung
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(16.dp),
                    onClick = onNavigateToNabung,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Progres Nabung",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = SakuStateCalculator.formatRupiah(
                            amount = totalSavings,
                            compact = true,
                            masked = privacyBlur
                        ),
                        style = MaterialTheme.typography.titleLarge.tabularNums,
                        color = colors.income,
                        maxLines = 1
                    )
                    Text(
                        text = "${state.savingsGoals.size} target aktif",
                        style = MaterialTheme.typography.labelSmall.tabularNums,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (featuredGoal != null) {
                        val (goal, saved) = featuredGoal
                        val goalRatio = if (goal.targetAmount > 0L) {
                            (saved.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val pct = (goalRatio * 100f).roundToInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp)
                            )
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall.tabularNums,
                                color = colors.income
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.cardBorderSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(goalRatio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(liftAccentForTheme(Color(goal.colorHex), colors.isDark))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${SakuStateCalculator.formatRupiah(saved, compact = true, masked = privacyBlur)} / ${
                                SakuStateCalculator.formatRupiah(goal.targetAmount, compact = true, masked = privacyBlur)
                            }",
                            style = MaterialTheme.typography.labelSmall.tabularNums,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Belum ada target",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Ketuk untuk buat target",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        // Transaksi Terbaru Glass Card
        item(key = "recent_transactions") {
            LiquidGlassCard(
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaksi Terbaru",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Semua",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.primaryContainer)
                            .clickable(onClick = onNavigateToTransaksi)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (recentTransactions.isEmpty()) {
                    SakuEmptyState(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = "Belum ada transaksi",
                        subtitle = "Catat pemasukan, pengeluaran, atau transfer pertamamu lewat tombol di atas."
                    )
                } else {
                    val today = remember { LocalDate.now(zoneId) }
                    recentTransactions.forEachIndexed { index, tx ->
                        val meta = resolveTxDisplayMeta(
                            tx = tx,
                            accountsById = accountsById,
                            categoriesById = categoriesById,
                            goalsById = goalsById,
                            colors = colors
                        )
                        val txDate = Instant.ofEpochMilli(tx.timestampEpochMs).atZone(zoneId).toLocalDate()
                        val timeText = if (txDate == today) {
                            "Hari Ini \u2022 ${formatIndonesianTime(tx.timestampEpochMs, zoneId)}"
                        } else {
                            formatIndonesianShortDate(tx.timestampEpochMs, zoneId)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenTransactionDetail(tx) }
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SakuTintedIconBadge(
                                iconKey = meta.iconKey,
                                tint = meta.tint,
                                containerColor = meta.containerColor,
                                size = 40.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = meta.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (!tx.receiptFileName.isNullOrBlank()) {
                                        Icon(
                                            imageVector = Icons.Rounded.Image,
                                            contentDescription = "Memiliki foto struk",
                                            tint = colors.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = meta.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val formatted = SakuStateCalculator.formatRupiah(
                                    amount = tx.amount,
                                    masked = privacyBlur
                                )
                                Text(
                                    text = if (privacyBlur) formatted else "${meta.amountPrefix}$formatted",
                                    style = MaterialTheme.typography.titleSmall.tabularNums,
                                    color = if (privacyBlur) colors.textPrimary else meta.amountColor
                                )
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.labelSmall.tabularNums,
                                    color = colors.textMuted
                                )
                            }
                        }

                        if (index < recentTransactions.lastIndex) {
                            HorizontalDivider(color = colors.cardBorderSubtle)
                        }
                    }
                }
            }
        }
    }
}
