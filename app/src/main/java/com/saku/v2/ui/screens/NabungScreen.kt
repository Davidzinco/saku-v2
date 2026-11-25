package com.saku.v2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.SavingsGoal
import com.saku.v2.ui.components.SakuEmptyState
import com.saku.v2.ui.components.SakuTintedIconBadge
import com.saku.v2.ui.liquidglass.LiquidGlassButton
import com.saku.v2.ui.liquidglass.LiquidGlassCard
import com.saku.v2.ui.sheets.resolveMineralAccent
import com.saku.v2.ui.theme.SakuTheme
import com.saku.v2.ui.theme.tabularNums
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NabungScreen(
    state: SakuAppState,
    mediaManager: MediaManagerContract,
    onOpenCreateGoal: () -> Unit,
    onOpenEditGoal: (SavingsGoal) -> Unit,
    onOpenDeposit: (SavingsGoal) -> Unit,
    onOpenWithdraw: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    val privacyBlur = state.preferences.privacyBlur
    var goalPendingDelete by remember { mutableStateOf<SavingsGoal?>(null) }

    val goalSnapshots = remember(state.savingsGoals, state.transactions) {
        state.savingsGoals.map { goal ->
            val saved = SakuStateCalculator.savingsGoalSavedAmount(state, goal.id)
            val reached = goal.targetAmount > 0L && saved >= goal.targetAmount
            GoalSnapshot(goal = goal, saved = saved, isReached = reached)
        }.sortedWith(
            compareBy<GoalSnapshot> { it.isReached }
                .thenBy { it.goal.deadlineEpochMs ?: Long.MAX_VALUE }
                .thenByDescending { it.goal.createdAtEpochMs }
        )
    }

    val totalSaved = remember(state.savingsGoals, state.transactions) {
        SakuStateCalculator.totalSavingsAmount(state)
    }
    val totalTarget = remember(state.savingsGoals) {
        state.savingsGoals.sumOf { it.targetAmount.coerceAtLeast(0L) }
    }
    val reachedCount = goalSnapshots.count { it.isReached }
    val activeCount = goalSnapshots.size - reachedCount

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "status_spacer") {
            Spacer(modifier = Modifier.statusBarsPadding())
        }

        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nabung",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target tabungan & alokasi impian",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                LiquidGlassButton(
                    onClick = onOpenCreateGoal,
                    isPrimary = true,
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Target Baru",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item(key = "hero_summary") {
            SavingsHeroCard(
                totalSaved = totalSaved,
                totalTarget = totalTarget,
                activeCount = activeCount,
                reachedCount = reachedCount,
                privacyBlur = privacyBlur
            )
        }

        if (goalSnapshots.isEmpty()) {
            item(key = "empty_goals") {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    SakuEmptyState(
                        icon = Icons.Rounded.Savings,
                        title = "Belum ada target tabungan",
                        subtitle = "Buat target pertama untuk dana darurat, perjalanan, atau kebutuhan khusus."
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LiquidGlassButton(
                        onClick = onOpenCreateGoal,
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Buat Target Baru",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        } else {
            items(
                items = goalSnapshots,
                key = { it.goal.id }
            ) { snapshot ->
                SavingsGoalCard(
                    snapshot = snapshot,
                    privacyBlur = privacyBlur,
                    mediaManager = mediaManager,
                    onClickCard = { onOpenEditGoal(snapshot.goal) },
                    onDeposit = { onOpenDeposit(snapshot.goal) },
                    onWithdraw = { onOpenWithdraw(snapshot.goal) },
                    onRequestDelete = { goalPendingDelete = snapshot.goal }
                )
            }
        }

        item(key = "transaction_link_footnote") {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.transferContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = colors.transfer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Sinkronisasi Otomatis ke Ledger",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Setiap Setor tabungan otomatis tercatat sebagai pengeluaran dari sumber dana pilihan, dan Tarik tabungan tercatat sebagai pemasukan di Transaksi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    goalPendingDelete?.let { targetGoal ->
        AlertDialog(
            onDismissRequest = { goalPendingDelete = null },
            containerColor = colors.sheetSurface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    text = "Hapus Target Tabungan?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Target \"${targetGoal.name}\" akan dihapus. Riwayat transaksi sebelumnya tetap aman tersimpan di ledger.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteGoal(targetGoal)
                        goalPendingDelete = null
                    }
                ) {
                    Text(
                        text = "Hapus",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.expense
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { goalPendingDelete = null }) {
                    Text(
                        text = "Batal",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                }
            }
        )
    }
}

private data class GoalSnapshot(
    val goal: SavingsGoal,
    val saved: Long,
    val isReached: Boolean
)

@Composable
private fun SavingsHeroCard(
    totalSaved: Long,
    totalTarget: Long,
    activeCount: Int,
    reachedCount: Int,
    privacyBlur: Boolean
) {
    val colors = SakuTheme.colors
    val progress = if (totalTarget > 0L) {
        (totalSaved.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = if (totalTarget > 0L) {
        ((totalSaved.toDouble() / totalTarget.toDouble()) * 100.0).roundToInt().coerceIn(0, 999)
    } else {
        0
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL TABUNGAN",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.incomeContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium.tabularNums,
                    color = colors.income,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = SakuStateCalculator.formatRupiah(totalSaved, masked = privacyBlur),
            style = MaterialTheme.typography.displayMedium.tabularNums,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (totalTarget > 0L) {
                "dari total target ${SakuStateCalculator.formatRupiah(totalTarget, masked = privacyBlur)}"
            } else {
                "Tambahkan target untuk memantau progres"
            },
            style = MaterialTheme.typography.bodySmall.tabularNums,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.cardBorderSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.income)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeroStatPill(
                label = "Aktif",
                value = "$activeCount target",
                containerColor = colors.primaryContainer,
                contentColor = colors.primary,
                modifier = Modifier.weight(1f)
            )
            HeroStatPill(
                label = "Tercapai",
                value = "$reachedCount target",
                containerColor = colors.incomeContainer,
                contentColor = colors.income,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroStatPill(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor.copy(alpha = if (colors.isDark) 0.72f else 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.tabularNums,
            color = contentColor
        )
    }
}

@Composable
private fun SavingsGoalCard(
    snapshot: GoalSnapshot,
    privacyBlur: Boolean,
    mediaManager: MediaManagerContract,
    onClickCard: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val colors = SakuTheme.colors
    val goal = snapshot.goal
    val saved = snapshot.saved
    val rawAccentColor = remember(goal.colorHex, colors.primary) {
        if (goal.colorHex != 0L) Color(goal.colorHex) else colors.primary
    }
    val accentColor = remember(goal.colorHex, colors.isDark, colors.primary) {
        resolveMineralAccent(goal.colorHex, colors.isDark, colors.primary)
    }
    val progress = if (goal.targetAmount > 0L) {
        (saved.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = if (goal.targetAmount > 0L) {
        ((saved.toDouble() / goal.targetAmount.toDouble()) * 100.0).roundToInt().coerceIn(0, 999)
    } else {
        0
    }

    val coverFile = remember(goal.coverFileName) {
        goal.coverFileName?.takeIf { it.isNotBlank() }?.let { mediaManager.resolveMediaFile(it) }
    }
    val hasCover = coverFile != null && coverFile.exists()

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = onClickCard
    ) {
        if (hasCover) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
            ) {
                AsyncImage(
                    model = coverFile,
                    contentDescription = goal.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.28f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.42f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SakuTintedIconBadge(
                        iconKey = goal.iconKey,
                        tint = Color.White,
                        containerColor = rawAccentColor.copy(alpha = 0.88f),
                        size = 38.dp
                    )

                    GoalDeleteIconChip(
                        tint = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.36f),
                        onClick = onRequestDelete
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (colors.isDark) 0.24f else 0.13f),
                                accentColor.copy(alpha = if (colors.isDark) 0.08f else 0.03f)
                            )
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SakuTintedIconBadge(
                        iconKey = goal.iconKey,
                        tint = accentColor,
                        containerColor = accentColor.copy(alpha = if (colors.isDark) 0.24f else 0.15f),
                        size = 42.dp
                    )

                    GoalDeleteIconChip(
                        tint = colors.textSecondary,
                        containerColor = colors.glassSurfaceFallback.copy(alpha = 0.6f),
                        onClick = onRequestDelete
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (snapshot.isReached) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.income,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Target tercapai",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.income
                            )
                        }
                    } else if (goal.deadlineEpochMs != null) {
                        DeadlineBadge(deadlineEpochMs = goal.deadlineEpochMs)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (snapshot.isReached) colors.incomeContainer
                            else accentColor.copy(alpha = if (colors.isDark) 0.24f else 0.13f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium.tabularNums,
                        color = if (snapshot.isReached) colors.income else accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = SakuStateCalculator.formatRupiah(saved, masked = privacyBlur),
                        style = MaterialTheme.typography.titleLarge.tabularNums,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "/ ${SakuStateCalculator.formatRupiah(goal.targetAmount, masked = privacyBlur)}",
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.cardBorderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (snapshot.isReached) colors.income else accentColor)
                    )
                }

                if (!snapshot.isReached && !privacyBlur) {
                    val remaining = (goal.targetAmount - saved).coerceAtLeast(0L)
                    Text(
                        text = "Kurang ${SakuStateCalculator.formatRupiah(remaining)}",
                        style = MaterialTheme.typography.labelMedium.tabularNums,
                        color = colors.textMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassButton(
                    onClick = onDeposit,
                    isPrimary = true,
                    tint = colors.income,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Setor",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary,
                        maxLines = 1
                    )
                }

                LiquidGlassButton(
                    onClick = onWithdraw,
                    isPrimary = false,
                    enabled = saved > 0L,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = if (saved > 0L) colors.textPrimary else colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tarik",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (saved > 0L) colors.textPrimary else colors.textMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalDeleteIconChip(
    tint: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(0.6.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Hapus Target",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DeadlineBadge(deadlineEpochMs: Long) {
    val colors = SakuTheme.colors
    val zone = ZoneId.systemDefault()
    val deadlineText = remember(deadlineEpochMs) {
        val date = Instant.ofEpochMilli(deadlineEpochMs).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val daysLeft = ChronoUnit.DAYS.between(today, date)
        val formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
        when {
            daysLeft < 0 -> "Lewat tenggat • $formattedDate"
            daysLeft == 0L -> "Jatuh tempo hari ini"
            daysLeft <= 30L -> "$daysLeft hari lagi • $formattedDate"
            else -> "Target $formattedDate"
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Event,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = deadlineText,
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
