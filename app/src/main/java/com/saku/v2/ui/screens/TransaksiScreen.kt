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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import com.saku.v2.ui.components.IndonesianLocale
import com.saku.v2.ui.components.SakuEmptyState
import com.saku.v2.ui.components.SakuTintedIconBadge
import com.saku.v2.ui.components.formatIndonesianTime
import com.saku.v2.ui.components.formatMonthYear
import com.saku.v2.ui.components.formatRelativeDateHeader
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
fun TransaksiScreen(
    state: SakuAppState,
    mediaManager: MediaManagerContract,
    onOpenNewTransaction: (TransactionType) -> Unit,
    onOpenTransactionDetail: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    val privacyBlur = state.preferences.privacyBlur
    val zoneId = remember { ZoneId.systemDefault() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedAccountIdFilter by remember { mutableStateOf<String?>(null) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val accountsById = remember(state.accounts) { state.accounts.associateBy { it.id } }
    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }
    val goalsById = remember(state.savingsGoals) { state.savingsGoals.associateBy { it.id } }

    val monthIncome = remember(state, selectedMonth) {
        SakuStateCalculator.monthlyIncome(state, selectedMonth, zoneId)
    }
    val monthExpense = remember(state, selectedMonth) {
        SakuStateCalculator.monthlyExpense(state, selectedMonth, zoneId)
    }
    val netMonth = monthIncome - monthExpense

    val topSpendingCategories = remember(state, selectedMonth, categoriesById) {
        SakuStateCalculator.categorySpending(state, selectedMonth, zoneId)
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .mapNotNull { (catId, spent) ->
                categoriesById[catId]?.let { cat -> cat to spent }
            }
    }

    val filteredTransactions = remember(
        state.transactions,
        selectedMonth,
        searchQuery,
        selectedTypeFilter,
        selectedAccountIdFilter,
        accountsById,
        categoriesById,
        goalsById
    ) {
        val query = searchQuery.trim().lowercase(IndonesianLocale)
        state.transactions
            .asSequence()
            .filter { tx ->
                YearMonth.from(Instant.ofEpochMilli(tx.timestampEpochMs).atZone(zoneId)) == selectedMonth
            }
            .filter { tx ->
                selectedTypeFilter == null || tx.type == selectedTypeFilter
            }
            .filter { tx ->
                selectedAccountIdFilter == null ||
                    tx.accountId == selectedAccountIdFilter ||
                    tx.toAccountId == selectedAccountIdFilter
            }
            .filter { tx ->
                if (query.isEmpty()) {
                    true
                } else {
                    val accName = accountsById[tx.accountId]?.name.orEmpty()
                    val destName = tx.toAccountId?.let { accountsById[it]?.name }.orEmpty()
                    val catName = tx.categoryId?.let { categoriesById[it]?.name }.orEmpty()
                    val goalName = tx.savingsGoalId?.let { goalsById[it]?.name }.orEmpty()
                    tx.note.lowercase(IndonesianLocale).contains(query) ||
                        accName.lowercase(IndonesianLocale).contains(query) ||
                        destName.lowercase(IndonesianLocale).contains(query) ||
                        catName.lowercase(IndonesianLocale).contains(query) ||
                        goalName.lowercase(IndonesianLocale).contains(query)
                }
            }
            .sortedByDescending { it.timestampEpochMs }
            .toList()
    }

    val groupedByDate = remember(filteredTransactions, zoneId) {
        filteredTransactions.groupBy { tx ->
            Instant.ofEpochMilli(tx.timestampEpochMs).atZone(zoneId).toLocalDate()
        }
    }

    val today = remember { LocalDate.now(zoneId) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = statusBarTop + 14.dp,
            bottom = 124.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + "+ Catat" pill button
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary
                )

                LiquidGlassButton(
                    onClick = { onOpenNewTransaction(TransactionType.EXPENSE) },
                    isPrimary = true
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Catat",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                }
            }
        }

        // Month switcher
        item(key = "month_switcher") {
            LiquidGlassCard(
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Bulan sebelumnya",
                            tint = colors.textPrimary
                        )
                    }
                    Text(
                        text = formatMonthYear(selectedMonth),
                        style = MaterialTheme.typography.titleMedium.tabularNums,
                        color = colors.textPrimary
                    )
                    IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Bulan berikutnya",
                            tint = colors.textPrimary
                        )
                    }
                }
            }
        }

        // Search + Filter Chips
        item(key = "search_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    placeholder = {
                        Text(
                            text = "Cari catatan...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = colors.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Bersihkan pencarian",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.cardBorderSubtle,
                        focusedContainerColor = colors.glassSurfaceFallback,
                        unfocusedContainerColor = colors.glassSurfaceFallback
                    )
                )

                val typeOptions = remember {
                    listOf(
                        null to "Semua",
                        TransactionType.EXPENSE to "Keluar",
                        TransactionType.INCOME to "Masuk",
                        TransactionType.TRANSFER to "Pindah"
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(typeOptions, key = { (typeVal, _) -> typeVal?.name ?: "ALL" }) { (typeVal, label) ->
                        FilterPill(
                            label = label,
                            selected = selectedTypeFilter == typeVal,
                            onClick = { selectedTypeFilter = typeVal }
                        )
                    }

                    item(key = "account_filter") {
                        Box {
                            val selectedAccountName = selectedAccountIdFilter?.let { accountsById[it]?.name }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (selectedAccountIdFilter != null) colors.primaryContainer
                                        else colors.glassSurfaceFallback
                                    )
                                    .border(
                                        width = 0.75.dp,
                                        color = if (selectedAccountIdFilter != null) colors.primary else colors.cardBorderSubtle,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable { showAccountMenu = true }
                                    .padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedAccountName ?: "Akun",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedAccountIdFilter != null) colors.primary else colors.textSecondary,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Filter akun",
                                    tint = if (selectedAccountIdFilter != null) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false },
                                containerColor = colors.sheetSurface
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Semua Akun") },
                                    onClick = {
                                        selectedAccountIdFilter = null
                                        showAccountMenu = false
                                    }
                                )
                                state.accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (account.isArchived) "${account.name} (Arsip)" else account.name
                                            )
                                        },
                                        onClick = {
                                            selectedAccountIdFilter = account.id
                                            showAccountMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Monthly Cashflow & Budget Breakdown Card
        item(key = "monthly_breakdown") {
            LiquidGlassCard(
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Ringkasan Cashflow & Budget",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CashflowSummaryCol(
                        label = "Masuk",
                        amount = monthIncome,
                        tint = colors.income,
                        masked = privacyBlur,
                        modifier = Modifier.weight(1f)
                    )
                    CashflowSummaryCol(
                        label = "Keluar",
                        amount = monthExpense,
                        tint = colors.expense,
                        masked = privacyBlur,
                        modifier = Modifier.weight(1f)
                    )
                    CashflowSummaryCol(
                        label = "Selisih",
                        amount = netMonth,
                        tint = if (netMonth >= 0L) colors.primary else colors.expense,
                        masked = privacyBlur,
                        modifier = Modifier.weight(1f)
                    )
                }

                val monthlyBudget = state.preferences.monthlyBudget
                if (monthlyBudget > 0L) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val ratio = (monthExpense.toFloat() / monthlyBudget.toFloat())
                    val pct = (ratio * 100f).roundToInt()
                    val barColor = when {
                        ratio >= 0.9f -> colors.expense
                        ratio >= 0.7f -> colors.warning
                        else -> colors.primary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pemakaian Budget Bulanan",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "$pct% \u2022 ${SakuStateCalculator.formatRupiah(monthlyBudget, compact = true, masked = privacyBlur)}",
                            style = MaterialTheme.typography.labelMedium.tabularNums,
                            color = barColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.cardBorderSubtle)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(barColor)
                        )
                    }
                }

                if (topSpendingCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = colors.cardBorderSubtle)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pengeluaran per Kategori",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        topSpendingCategories.forEach { (category, spent) ->
                            val catColor = Color(category.colorHex)
                            val limit = category.monthlyLimit
                            val ratio = if (limit != null && limit > 0L) {
                                (spent.toFloat() / limit.toFloat())
                            } else {
                                if (monthExpense > 0L) (spent.toFloat() / monthExpense.toFloat()) else 0f
                            }
                            val fillColor = when {
                                limit != null && ratio >= 1f -> colors.expense
                                limit != null && ratio >= 0.8f -> colors.warning
                                else -> liftAccentForTheme(catColor, colors.isDark)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SakuTintedIconBadge(
                                            iconKey = category.iconKey,
                                            tint = catColor,
                                            containerColor = catColor.copy(alpha = 0.14f),
                                            size = 26.dp
                                        )
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textPrimary
                                        )
                                    }

                                    val limitSuffix = if (limit != null && limit > 0L) {
                                        " / ${SakuStateCalculator.formatRupiah(limit, compact = true, masked = privacyBlur)}"
                                    } else {
                                        ""
                                    }
                                    Text(
                                        text = "${SakuStateCalculator.formatRupiah(spent, compact = true, masked = privacyBlur)}$limitSuffix",
                                        style = MaterialTheme.typography.labelMedium.tabularNums,
                                        color = if (limit != null && ratio >= 1f) colors.expense else colors.textPrimary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.cardBorderSubtle)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(fillColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Date-grouped Transactions List
        if (groupedByDate.isEmpty()) {
            item(key = "empty_state") {
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    val hasActiveFilters = searchQuery.isNotBlank() ||
                        selectedTypeFilter != null ||
                        selectedAccountIdFilter != null
                    SakuEmptyState(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = if (hasActiveFilters) "Tidak ada hasil cocok" else "Belum ada transaksi bulan ini",
                        subtitle = if (hasActiveFilters) {
                            "Ubah kata kunci pencarian atau filter tipe/akun untuk melihat transaksi lainnya."
                        } else {
                            "Ketuk tombol + Catat untuk menambahkan transaksi di ${formatMonthYear(selectedMonth)}."
                        }
                    )
                }
            }
        } else {
            groupedByDate.forEach { (date, dayTransactions) ->
                item(key = "group_$date") {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatRelativeDateHeader(date, today),
                                style = MaterialTheme.typography.labelLarge.tabularNums,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "${dayTransactions.size} transaksi",
                                style = MaterialTheme.typography.labelSmall.tabularNums,
                                color = colors.textMuted
                            )
                        }

                        LiquidGlassCard(
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            dayTransactions.forEachIndexed { index, tx ->
                                val meta = resolveTxDisplayMeta(
                                    tx = tx,
                                    accountsById = accountsById,
                                    categoriesById = categoriesById,
                                    goalsById = goalsById,
                                    colors = colors
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenTransactionDetail(tx) }
                                        .padding(vertical = 10.dp),
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
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = meta.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = meta.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (meta.linkedGoalName != null) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(colors.incomeContainer)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Savings,
                                                    contentDescription = null,
                                                    tint = colors.income,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = meta.linkedGoalName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.income,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    if (!tx.receiptFileName.isNullOrBlank()) {
                                        val thumbFile = remember(tx.receiptFileName) {
                                            mediaManager.resolveMediaFile(tx.receiptFileName)
                                        }
                                        AsyncImage(
                                            model = thumbFile,
                                            contentDescription = "Struk",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(10.dp))
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
                                            text = formatIndonesianTime(tx.timestampEpochMs, zoneId),
                                            style = MaterialTheme.typography.labelSmall.tabularNums,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                if (index < dayTransactions.lastIndex) {
                                    HorizontalDivider(color = colors.cardBorderSubtle)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = SakuTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.primaryContainer else colors.glassSurfaceFallback)
            .border(
                width = 0.75.dp,
                color = if (selected) colors.primary else colors.cardBorderSubtle,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun CashflowSummaryCol(
    label: String,
    amount: Long,
    tint: Color,
    masked: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.sheetSurface.copy(alpha = if (colors.isDark) 0.48f else 0.70f))
            .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Text(
            text = SakuStateCalculator.formatRupiah(
                amount = amount,
                compact = true,
                masked = masked
            ),
            style = MaterialTheme.typography.titleSmall.tabularNums,
            color = if (masked) colors.textPrimary else tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
