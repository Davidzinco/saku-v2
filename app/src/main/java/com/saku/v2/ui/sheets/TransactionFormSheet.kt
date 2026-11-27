package com.saku.v2.ui.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.model.Account
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import com.saku.v2.data.store.PendingQuickLog
import com.saku.v2.ui.components.SakuTintedIconBadge
import com.saku.v2.ui.components.findBestMatchingAccount
import com.saku.v2.ui.components.formatIndonesianShortDate
import com.saku.v2.ui.theme.SakuTheme
import com.saku.v2.ui.theme.tabularNums
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    state: SakuAppState,
    mediaManager: MediaManagerContract,
    initialTransaction: Transaction? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    initialQuickLog: PendingQuickLog? = null,
    onSaveNew: (
        type: TransactionType,
        amount: Long,
        accountId: String,
        toAccountId: String?,
        categoryId: String?,
        savingsGoalId: String?,
        note: String,
        timestampEpochMs: Long,
        receiptFileName: String?
    ) -> Unit,
    onUpdateExisting: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zoneId = remember { ZoneId.systemDefault() }

    val activeAccounts = remember(state.accounts) {
        state.accounts.filter { !it.isArchived }
    }
    val selectableAccounts = remember(state.accounts, initialTransaction) {
        val archivedUsed = initialTransaction?.let { tx ->
            state.accounts.filter { it.isArchived && (it.id == tx.accountId || it.id == tx.toAccountId) }
        }.orEmpty()
        (activeAccounts + archivedUsed).distinctBy { it.id }
    }

    val formSeedKey = remember(initialTransaction?.id, initialQuickLog?.id, initialType) {
        "${initialTransaction?.id ?: "new"}_${initialQuickLog?.id ?: "plain"}_${initialType.name}"
    }

    var txType by remember(formSeedKey) {
        mutableStateOf(initialTransaction?.type ?: initialQuickLog?.type ?: initialType)
    }
    var amountDigits by remember(formSeedKey) {
        mutableStateOf(
            initialTransaction?.amount?.toString()
                ?: initialQuickLog?.amount?.takeIf { it > 0L }?.toString()
                ?: ""
        )
    }
    var selectedAccountId by remember(formSeedKey, selectableAccounts) {
        mutableStateOf(
            initialTransaction?.accountId
                ?: findBestMatchingAccount(activeAccounts, initialQuickLog?.suggestedAccountName)?.id
                ?: selectableAccounts.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedToAccountId by remember(formSeedKey, selectableAccounts) {
        mutableStateOf(
            initialTransaction?.toAccountId
                ?: selectableAccounts.firstOrNull { it.id != selectedAccountId }?.id
        )
    }
    var selectedCategoryId by remember(formSeedKey, state.categories) {
        val initialCat = initialTransaction?.categoryId
        val fallbackCat = state.categories.firstOrNull { !it.isSystem && it.type == txType }?.id
        mutableStateOf(initialCat ?: fallbackCat)
    }
    var note by remember(formSeedKey) {
        mutableStateOf(initialTransaction?.note ?: initialQuickLog?.suggestedNote.orEmpty())
    }
    var receiptFileName by remember(formSeedKey) {
        mutableStateOf(initialTransaction?.receiptFileName)
    }
    var isImportingReceipt by remember { mutableStateOf(false) }
    var receiptError by remember { mutableStateOf<String?>(null) }

    val initialEpochMs = initialTransaction?.timestampEpochMs
        ?: initialQuickLog?.timestampEpochMs
        ?: System.currentTimeMillis()
    val initialZoned = remember(formSeedKey) {
        Instant.ofEpochMilli(initialEpochMs).atZone(zoneId)
    }
    var selectedDate by remember(formSeedKey) { mutableStateOf(initialZoned.toLocalDate()) }
    var selectedTime by remember(formSeedKey) { mutableStateOf(initialZoned.toLocalTime()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val availableCategories = remember(state.categories, txType, initialTransaction?.categoryId) {
        state.categories.filter { cat ->
            cat.type == txType && (!cat.isSystem || cat.id == initialTransaction?.categoryId)
        }
    }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImportingReceipt = true
                receiptError = null
                mediaManager.importReceiptWebp(uri).fold(
                    onSuccess = { fileName -> receiptFileName = fileName },
                    onFailure = { receiptError = "Gagal memproses foto struk" }
                )
                isImportingReceipt = false
            }
        }
    }

    val parsedAmount = amountDigits.toLongOrNull() ?: 0L
    val isTransferValid = txType != TransactionType.TRANSFER ||
        (!selectedToAccountId.isNullOrBlank() && selectedToAccountId != selectedAccountId)
    val canSubmit = parsedAmount > 0L && selectedAccountId.isNotBlank() && isTransferValid && !isImportingReceipt

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetSurface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction != null) "Edit Transaksi" else "Catat Transaksi",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = colors.textSecondary
                    )
                }
            }

            if (initialQuickLog != null && initialTransaction == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primaryContainer)
                        .border(0.75.dp, colors.primary.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Dari notifikasi ${initialQuickLog.sourceApp} \u2022 Periksa nominal & akun, belum tersimpan otomatis",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary
                    )
                }
            }

            // Segmented mode pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.canvasGradientMid)
                    .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    TransactionType.EXPENSE to "Keluar",
                    TransactionType.INCOME to "Masuk",
                    TransactionType.TRANSFER to "Pindah Saldo"
                )
                modes.forEach { (mode, label) ->
                    val selected = txType == mode
                    val activeFill = when (mode) {
                        TransactionType.EXPENSE -> colors.expense
                        TransactionType.INCOME -> colors.income
                        TransactionType.TRANSFER -> colors.transfer
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) activeFill else Color.Transparent)
                            .clickable {
                                txType = mode
                                if (mode != TransactionType.TRANSFER) {
                                    val stillValid = state.categories.any {
                                        it.id == selectedCategoryId && it.type == mode
                                    }
                                    if (!stillValid) {
                                        selectedCategoryId = state.categories
                                            .firstOrNull { !it.isSystem && it.type == mode }?.id
                                    }
                                } else if (selectedToAccountId == null || selectedToAccountId == selectedAccountId) {
                                    selectedToAccountId = selectableAccounts
                                        .firstOrNull { it.id != selectedAccountId }?.id
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.onPrimary else colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Linked savings info pill when editing a savings transaction
            if (initialTransaction?.savingsGoalId != null) {
                val linkedGoal = state.savingsGoals.firstOrNull { it.id == initialTransaction.savingsGoalId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.incomeContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = null,
                        tint = colors.income,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Terhubung ke tabungan ${linkedGoal?.name ?: "Target"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.income
                    )
                }
            }

            // Large numeric Rp input + quick nominal chips
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nominal",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { raw ->
                        amountDigits = raw.filter { it.isDigit() }.take(13)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = {
                        Text(
                            text = "Rp ",
                            style = MaterialTheme.typography.headlineMedium.tabularNums,
                            color = colors.textSecondary
                        )
                    },
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium.tabularNums,
                            color = colors.textMuted
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.tabularNums.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = RupiahGroupingVisualTransformation,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.cardBorderSubtle,
                        focusedContainerColor = colors.canvasGradientStart,
                        unfocusedContainerColor = colors.canvasGradientStart
                    )
                )

                val quickNominals = listOf(
                    25_000L to "25rb",
                    50_000L to "50rb",
                    100_000L to "100rb",
                    250_000L to "250rb"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickNominals.forEach { (nominal, label) ->
                        val isSelected = parsedAmount == nominal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) colors.primaryContainer else colors.canvasGradientMid
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (isSelected) colors.primary else colors.cardBorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { amountDigits = nominal.toString() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.tabularNums,
                                color = if (isSelected) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Account selector
            AccountPickerRow(
                label = if (txType == TransactionType.TRANSFER) "Dari Akun" else "Sumber Dana",
                accounts = selectableAccounts,
                selectedAccountId = selectedAccountId,
                state = state,
                onSelect = { chosen ->
                    selectedAccountId = chosen.id
                    if (txType == TransactionType.TRANSFER && selectedToAccountId == chosen.id) {
                        selectedToAccountId = selectableAccounts.firstOrNull { it.id != chosen.id }?.id
                    }
                }
            )

            // Destination account selector when TRANSFER
            if (txType == TransactionType.TRANSFER) {
                val targetOptions = selectableAccounts.filter { it.id != selectedAccountId }
                if (targetOptions.isEmpty()) {
                    Text(
                        text = "Tambahkan minimal 2 akun aktif untuk melakukan transfer saldo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.expense
                    )
                } else {
                    AccountPickerRow(
                        label = "Ke Akun",
                        accounts = targetOptions,
                        selectedAccountId = selectedToAccountId.orEmpty(),
                        state = state,
                        onSelect = { selectedToAccountId = it.id }
                    )
                }
            }

            // Category selector for EXPENSE / INCOME
            if (txType != TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.forEach { category ->
                            val selected = selectedCategoryId == category.id
                            val catTint = Color(category.colorHex)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) catTint.copy(alpha = 0.16f)
                                        else colors.canvasGradientMid
                                    )
                                    .border(
                                        width = if (selected) 1.dp else 0.75.dp,
                                        color = if (selected) catTint else colors.cardBorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedCategoryId = category.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SakuTintedIconBadge(
                                    iconKey = category.iconKey,
                                    tint = catTint,
                                    containerColor = catTint.copy(alpha = 0.14f),
                                    size = 24.dp
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) colors.textPrimary else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Date & time selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tanggal & Jam",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                val today = remember { LocalDate.now(zoneId) }
                val yesterday = remember(today) { today.minusDays(1) }
                val customDateLabel = formatIndonesianShortDate(
                    selectedDate.atTime(LocalTime.NOON).atZone(zoneId).toInstant().toEpochMilli(),
                    zoneId
                )
                val timeLabel = remember(selectedTime) {
                    selectedTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DateOptionPill(
                        label = "Hari ini",
                        selected = selectedDate == today,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedDate = today }
                    )
                    DateOptionPill(
                        label = "Kemarin",
                        selected = selectedDate == yesterday,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedDate = yesterday }
                    )
                    Row(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedDate != today && selectedDate != yesterday) colors.primaryContainer
                                else colors.canvasGradientMid
                            )
                            .border(
                                width = 0.75.dp,
                                color = if (selectedDate != today && selectedDate != yesterday) colors.primary else colors.cardBorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = if (selectedDate != today && selectedDate != yesterday) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (selectedDate != today && selectedDate != yesterday) customDateLabel else "Pilih Tgl",
                            style = MaterialTheme.typography.labelMedium.tabularNums,
                            color = if (selectedDate != today && selectedDate != yesterday) colors.primary else colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(0.95f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.canvasGradientMid)
                            .border(
                                width = 0.75.dp,
                                color = colors.cardBorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Pilih jam",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelMedium.tabularNums,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Note field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Catatan (opsional)") },
                placeholder = { Text("Kopi pagi, bayar tagihan, transfer rutin...") },
                maxLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.cardBorderSubtle,
                    focusedContainerColor = colors.canvasGradientStart,
                    unfocusedContainerColor = colors.canvasGradientStart
                )
            )

            // WebP Receipt Photo section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Foto Struk (WebP)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                if (receiptFileName != null) {
                    val file = remember(receiptFileName) {
                        mediaManager.resolveMediaFile(receiptFileName!!)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.canvasGradientMid)
                            .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Preview struk",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    receiptPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = colors.sheetSurface.copy(alpha = 0.90f),
                                    contentColor = colors.textPrimary
                                )
                            ) {
                                Text("Ganti", style = MaterialTheme.typography.labelMedium)
                            }
                            IconButton(
                                onClick = { receiptFileName = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.sheetSurface.copy(alpha = 0.90f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Hapus foto struk",
                                    tint = colors.expense,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.canvasGradientMid)
                            .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isImportingReceipt) {
                                receiptPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isImportingReceipt) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isImportingReceipt) "Mengompres ke WebP..." else "Lampirkan Foto Struk",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Otomatis dikompres hemat ruang dalam format WebP",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                if (receiptError != null) {
                    Text(
                        text = receiptError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.expense
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val epochMs = selectedDate.atTime(selectedTime).atZone(zoneId).toInstant().toEpochMilli()
                    if (initialTransaction != null) {
                        onUpdateExisting(
                            initialTransaction.copy(
                                type = txType,
                                amount = parsedAmount,
                                accountId = selectedAccountId,
                                toAccountId = if (txType == TransactionType.TRANSFER) selectedToAccountId else null,
                                categoryId = if (txType == TransactionType.TRANSFER) null else selectedCategoryId,
                                note = note.trim(),
                                timestampEpochMs = epochMs,
                                receiptFileName = receiptFileName
                            )
                        )
                    } else {
                        onSaveNew(
                            txType,
                            parsedAmount,
                            selectedAccountId,
                            if (txType == TransactionType.TRANSFER) selectedToAccountId else null,
                            if (txType == TransactionType.TRANSFER) null else selectedCategoryId,
                            null,
                            note.trim(),
                            epochMs,
                            receiptFileName
                        )
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.35f),
                    disabledContentColor = colors.onPrimary.copy(alpha = 0.7f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTransaction != null) "Simpan Perubahan" else "Simpan Transaksi",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            selectedDate = Instant.ofEpochMilli(utcMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = colors.sheetSurface,
            titleContentColor = colors.textPrimary,
            title = {
                Text(
                    text = "Pilih Jam",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AccountPickerRow(
    label: String,
    accounts: List<Account>,
    selectedAccountId: String,
    state: SakuAppState,
    onSelect: (Account) -> Unit
) {
    val colors = SakuTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            accounts.forEach { account ->
                val selected = account.id == selectedAccountId
                val accTint = Color(account.colorHex)
                val balance = SakuStateCalculator.accountBalance(state, account.id)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) accTint.copy(alpha = 0.16f) else colors.canvasGradientMid
                        )
                        .border(
                            width = if (selected) 1.dp else 0.75.dp,
                            color = if (selected) accTint else colors.cardBorderSubtle,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(account) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
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
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = SakuStateCalculator.formatRupiah(
                                amount = balance,
                                compact = true,
                                masked = state.preferences.privacyBlur
                            ),
                            style = MaterialTheme.typography.labelSmall.tabularNums,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateOptionPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = SakuTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.primaryContainer else colors.canvasGradientMid)
            .border(
                width = 0.75.dp,
                color = if (selected) colors.primary else colors.cardBorderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.primary else colors.textSecondary,
            maxLines = 1
        )
    }
}

private object RupiahGroupingVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val n = digits.length
        if (n == 0) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val sb = StringBuilder(n + n / 3)
        val origToTrans = IntArray(n + 1)
        val transToOrig = ArrayList<Int>(n + n / 3 + 1)

        for (i in 0 until n) {
            if (i > 0 && (n - i) % 3 == 0) {
                sb.append('.')
                transToOrig.add(i)
            }
            origToTrans[i] = sb.length
            sb.append(digits[i])
            transToOrig.add(i)
        }
        origToTrans[n] = sb.length
        transToOrig.add(n)

        val formatted = sb.toString()
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, n)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrig[offset.coerceIn(0, formatted.length)]
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
