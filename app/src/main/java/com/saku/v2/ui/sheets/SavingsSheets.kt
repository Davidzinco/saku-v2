package com.saku.v2.ui.sheets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.SavingsGoal
import com.saku.v2.ui.components.SakuTintedIconBadge
import com.saku.v2.ui.components.sakuIconForKey
import com.saku.v2.ui.liquidglass.LiquidGlassButton
import com.saku.v2.ui.theme.SakuTheme
import com.saku.v2.ui.theme.tabularNums
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val SAKU_MINERAL_SWATCHES: List<Long> = listOf(
    0xFF1E4D45L, // Deep Forest Pine
    0xFF2A6B57L, // Muted Jade
    0xFF3D5473L, // Architectural Slate
    0xFFB8563FL, // Warm Terracotta
    0xFFA87633L, // Muted Ochre
    0xFF544B73L, // Mineral Plum
    0xFF6B5B4DL, // Warm Umber
    0xFF5C6B5EL  // Sage Olive
)

internal val SAKU_GOAL_ICONS: List<String> = listOf(
    "savings",
    "shield",
    "flight",
    "home",
    "favorite",
    "card_giftcard",
    "shopping_bag",
    "commute",
    "trending_up"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalFormSheet(
    initialGoal: SavingsGoal?,
    mediaManager: MediaManagerContract,
    onSave: (
        name: String,
        targetAmount: Long,
        coverFileName: String?,
        deadlineEpochMs: Long?,
        colorHex: Long,
        iconKey: String,
        existingGoal: SavingsGoal?
    ) -> Unit,
    onDelete: ((SavingsGoal) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(initialGoal?.id) { mutableStateOf(initialGoal?.name ?: "") }
    var targetAmountText by remember(initialGoal?.id) {
        mutableStateOf(initialGoal?.targetAmount?.toString() ?: "")
    }
    var coverFileName by remember(initialGoal?.id) { mutableStateOf(initialGoal?.coverFileName) }
    var deadlineEpochMs by remember(initialGoal?.id) { mutableStateOf(initialGoal?.deadlineEpochMs) }
    var colorHex by remember(initialGoal?.id) {
        mutableLongStateOf(initialGoal?.colorHex ?: SAKU_MINERAL_SWATCHES.first())
    }
    var iconKey by remember(initialGoal?.id) { mutableStateOf(initialGoal?.iconKey ?: "savings") }

    var isImportingCover by remember { mutableStateOf(false) }
    var coverError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImportingCover = true
                coverError = null
                mediaManager.importCoverWebp(uri).fold(
                    onSuccess = { importedFileName -> coverFileName = importedFileName },
                    onFailure = { coverError = "Gagal mengimpor foto sampul" }
                )
                isImportingCover = false
            }
        }
    }

    val parsedTarget = targetAmountText.filter(Char::isDigit).toLongOrNull() ?: 0L
    val canSave = name.isNotBlank() && parsedTarget > 0L && !isImportingCover

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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SakuSheetHeader(
                title = if (initialGoal == null) "Target Tabungan Baru" else "Edit Target Tabungan",
                subtitle = "Atur nominal, foto sampul WebP, dan tenggat waktu",
                onDismiss = onDismiss
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Target") },
                placeholder = { Text("Contoh: Dana Darurat, Liburan Kyoto") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(16.dp),
                colors = sakuOutlinedFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            SakuNominalTextField(
                value = targetAmountText,
                onValueChange = { targetAmountText = it.filter(Char::isDigit).take(13) },
                label = "Nominal Target",
                placeholder = "10000000"
            )

            // Quick presets for target amount
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "2,5 jt" to 2_500_000L,
                    "5 jt" to 5_000_000L,
                    "10 jt" to 10_000_000L,
                    "25 jt" to 25_000_000L
                )
                items(presets) { (label, amount) ->
                    SakuQuickChip(
                        label = label,
                        isSelected = parsedTarget == amount,
                        onClick = { targetAmountText = amount.toString() }
                    )
                }
            }

            // Cover Photo (WebP) Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Foto Sampul (Opsional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )

                val resolvedFile = remember(coverFileName) {
                    coverFileName?.takeIf { it.isNotBlank() }?.let { mediaManager.resolveMediaFile(it) }
                }

                if (resolvedFile != null && resolvedFile.exists()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(18.dp))
                    ) {
                        AsyncImage(
                            model = resolvedFile,
                            contentDescription = "Pratinjau sampul target",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.62f))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Ganti",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.expense.copy(alpha = 0.90f))
                                    .clickable { coverFileName = null }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Hapus",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.glassSurfaceFallback)
                            .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isImportingCover) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isImportingCover) {
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
                                text = if (isImportingCover) "Mengompresi gambar ke WebP..." else "Pilih Foto Sampul dari Galeri",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Otomatis dikonversi ke WebP hemat ruang",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                coverError?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.expense
                    )
                }
            }

            // Optional Deadline Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tenggat Waktu (Opsional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.glassSurfaceFallback)
                        .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = deadlineEpochMs?.let { formatIndonesianDate(it) }
                                ?: "Tanpa tenggat waktu",
                            style = MaterialTheme.typography.bodyMedium.tabularNums,
                            color = if (deadlineEpochMs != null) colors.textPrimary else colors.textSecondary
                        )
                    }
                    if (deadlineEpochMs != null) {
                        IconButton(
                            onClick = { deadlineEpochMs = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hapus tenggat",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Editorial Mineral Swatches
            SakuMineralColorPicker(
                selectedColorHex = colorHex,
                onSelectColor = { colorHex = it }
            )

            // Icon Picker
            SakuIconKeyPicker(
                iconKeys = SAKU_GOAL_ICONS,
                selectedIconKey = iconKey,
                accentColor = Color(colorHex),
                onSelectIcon = { iconKey = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            LiquidGlassButton(
                onClick = {
                    onSave(
                        name.trim(),
                        parsedTarget,
                        coverFileName,
                        deadlineEpochMs,
                        colorHex,
                        iconKey,
                        initialGoal
                    )
                    onDismiss()
                },
                backdrop = null,
                isPrimary = true,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (initialGoal == null) "Simpan Target" else "Perbarui Target",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimary
                )
            }

            if (initialGoal != null && onDelete != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = colors.expense,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hapus Target Ini",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.expense
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadlineEpochMs ?: (System.currentTimeMillis() + 30L * 86_400_000L)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        deadlineEpochMs = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal", color = colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && initialGoal != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.sheetSurface,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text("Hapus Target Tabungan?") },
            text = {
                Text("Target \"${initialGoal.name}\" akan dihapus, namun transaksi riwayat setoran dan penarikan tetap tersimpan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initialGoal)
                        onDismiss()
                    }
                ) {
                    Text("Hapus", color = colors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = colors.textSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsTransferSheet(
    goal: SavingsGoal,
    isDeposit: Boolean,
    state: SakuAppState,
    onConfirm: (goalId: String, amount: Long, accountId: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val privacyBlur = state.preferences.privacyBlur

    val savedAmount = remember(state.transactions, goal.id) {
        SakuStateCalculator.savingsGoalSavedAmount(state, goal.id)
    }
    val activeAccounts = remember(state.accounts) {
        state.accounts.filter { !it.isArchived }
    }

    var amountText by remember(goal.id, isDeposit) { mutableStateOf("") }
    var selectedAccountId by remember(goal.id, activeAccounts) {
        mutableStateOf(activeAccounts.firstOrNull()?.id ?: "")
    }
    var note by remember(goal.id, isDeposit) { mutableStateOf("") }

    val parsedAmount = amountText.filter(Char::isDigit).toLongOrNull() ?: 0L
    val exceedsSaved = !isDeposit && parsedAmount > savedAmount
    val selectedAccountBalance = remember(state, selectedAccountId) {
        if (selectedAccountId.isNotBlank()) {
            SakuStateCalculator.accountBalance(state, selectedAccountId)
        } else {
            0L
        }
    }
    val lowAccountBalanceWarning = isDeposit &&
        selectedAccountId.isNotBlank() &&
        parsedAmount > 0L &&
        parsedAmount > selectedAccountBalance

    val canConfirm = parsedAmount > 0L &&
        selectedAccountId.isNotBlank() &&
        !exceedsSaved

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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SakuSheetHeader(
                title = if (isDeposit) "Setor Tabungan" else "Tarik Tabungan",
                subtitle = "${goal.name} • Terkumpul ${SakuStateCalculator.formatRupiah(savedAmount, masked = privacyBlur)}",
                onDismiss = onDismiss
            )

            // Transaction link explanation banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDeposit) colors.incomeContainer else colors.transferContainer)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (isDeposit) colors.income else colors.transfer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isDeposit) {
                        "Setor \u2192 Pengeluaran: transaksi pengeluaran otomatis dicatat untuk mengurangi saldo sumber dana."
                    } else {
                        "Tarik \u2192 Pemasukan: transaksi pemasukan otomatis dicatat untuk menambah saldo akun tujuan."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary
                )
            }

            SakuNominalTextField(
                value = amountText,
                onValueChange = { amountText = it.filter(Char::isDigit).take(13) },
                label = if (isDeposit) "Nominal Setoran" else "Nominal Penarikan",
                placeholder = "250000",
                isError = exceedsSaved,
                supportingError = if (exceedsSaved) {
                    "Melebihi dana tabungan tersimpan (${SakuStateCalculator.formatRupiah(savedAmount, masked = privacyBlur)})"
                } else null
            )

            if (lowAccountBalanceWarning) {
                val accName = activeAccounts.firstOrNull { it.id == selectedAccountId }?.name ?: "akun"
                Text(
                    text = "Catatan: saldo $accName saat ini ${SakuStateCalculator.formatRupiah(selectedAccountBalance, masked = privacyBlur)}.",
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = colors.warning
                )
            }

            // Quick Nominal Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chips = buildList {
                    add("100rb" to 100_000L)
                    add("250rb" to 250_000L)
                    add("500rb" to 500_000L)
                    if (!isDeposit && savedAmount > 0L) {
                        add("Semua" to savedAmount)
                    } else {
                        add("1jt" to 1_000_000L)
                    }
                }
                items(chips) { (label, value) ->
                    SakuQuickChip(
                        label = label,
                        isSelected = parsedAmount == value && value > 0L,
                        onClick = { amountText = value.toString() }
                    )
                }
            }

            // Account Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isDeposit) "Sumber Dana" else "Akun Tujuan",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )

                if (activeAccounts.isEmpty()) {
                    Text(
                        text = "Tidak ada akun aktif. Aktifkan akun di Pengaturan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.expense
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeAccounts.forEach { account ->
                            val isSelected = account.id == selectedAccountId
                            val balance = SakuStateCalculator.accountBalance(state, account.id)
                            val accColor = resolveMineralAccent(
                                colorHex = account.colorHex,
                                isDark = colors.isDark,
                                fallback = colors.primary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) colors.primaryContainer.copy(alpha = 0.7f)
                                        else colors.glassSurfaceFallback
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) colors.primary else colors.cardBorderSubtle,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedAccountId = account.id }
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SakuTintedIconBadge(
                                        iconKey = account.iconKey,
                                        tint = accColor,
                                        containerColor = accColor.copy(alpha = if (colors.isDark) 0.24f else 0.14f),
                                        size = 38.dp
                                    )
                                    Column {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Saldo: ${SakuStateCalculator.formatRupiah(balance, masked = privacyBlur)}",
                                            style = MaterialTheme.typography.bodySmall.tabularNums,
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Catatan (Opsional)") },
                placeholder = {
                    Text(if (isDeposit) "Sisihan tabungan ${goal.name}" else "Pencairan ${goal.name}")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                colors = sakuOutlinedFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            LiquidGlassButton(
                onClick = {
                    onConfirm(goal.id, parsedAmount, selectedAccountId, note.trim())
                    onDismiss()
                },
                backdrop = null,
                isPrimary = true,
                tint = if (isDeposit) colors.income else colors.transfer,
                enabled = canConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isDeposit) "Konfirmasi Setor" else "Konfirmasi Tarik",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun SakuSheetHeader(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = colors.textSecondary
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Tutup",
                tint = colors.textSecondary
            )
        }
    }
}

@Composable
internal fun SakuNominalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false,
    supportingError: String? = null
) {
    val colors = SakuTheme.colors
    val numeric = value.filter(Char::isDigit).toLongOrNull() ?: 0L

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            prefix = {
                Text(
                    text = "Rp ",
                    style = MaterialTheme.typography.titleMedium.tabularNums,
                    color = colors.textSecondary
                )
            },
            textStyle = MaterialTheme.typography.titleMedium.tabularNums,
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = sakuOutlinedFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (supportingError != null) {
            Text(
                text = supportingError,
                style = MaterialTheme.typography.bodySmall,
                color = colors.expense
            )
        } else if (numeric > 0L) {
            Text(
                text = "= ${SakuStateCalculator.formatRupiah(numeric)}",
                style = MaterialTheme.typography.labelMedium.tabularNums,
                color = colors.primary
            )
        }
    }
}

@Composable
internal fun SakuQuickChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = SakuTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) colors.primary else colors.glassSurfaceFallback)
            .border(
                width = 1.dp,
                color = if (isSelected) colors.primary else colors.cardBorderSubtle,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.tabularNums,
            color = if (isSelected) colors.onPrimary else colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun SakuMineralColorPicker(
    selectedColorHex: Long,
    onSelectColor: (Long) -> Unit
) {
    val colors = SakuTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Warna Aksen",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(SAKU_MINERAL_SWATCHES) { hex ->
                val isSelected = hex == selectedColorHex
                val swatchColor = Color(hex)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.5.dp, colors.textPrimary, CircleShape)
                            } else {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            }
                        )
                        .clickable { onSelectColor(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SakuIconKeyPicker(
    iconKeys: List<String>,
    selectedIconKey: String,
    accentColor: Color,
    onSelectIcon: (String) -> Unit
) {
    val colors = SakuTheme.colors
    val displayAccent = remember(accentColor, colors.isDark) {
        accentColor.liftForDarkSurface(colors.isDark)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Ikon",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(iconKeys) { key ->
                val isSelected = key == selectedIconKey
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) displayAccent.copy(alpha = if (colors.isDark) 0.24f else 0.16f)
                            else colors.glassSurfaceFallback
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) displayAccent else colors.cardBorderSubtle,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectIcon(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = sakuIconForKey(key),
                        contentDescription = key,
                        tint = if (isSelected) displayAccent else colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

internal fun Color.liftForDarkSurface(isDark: Boolean): Color {
    if (!isDark || luminance() >= 0.26f) return this
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[1] = hsv[1].coerceAtMost(0.46f)
    hsv[2] = hsv[2].coerceAtLeast(0.76f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

internal fun resolveMineralAccent(colorHex: Long, isDark: Boolean, fallback: Color): Color {
    if (colorHex == 0L) return fallback
    return Color(colorHex).liftForDarkSurface(isDark)
}

@Composable
internal fun sakuOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SakuTheme.colors.primary,
    unfocusedBorderColor = SakuTheme.colors.cardBorderSubtle,
    focusedLabelColor = SakuTheme.colors.primary,
    unfocusedLabelColor = SakuTheme.colors.textSecondary,
    focusedTextColor = SakuTheme.colors.textPrimary,
    unfocusedTextColor = SakuTheme.colors.textPrimary,
    cursorColor = SakuTheme.colors.primary,
    errorBorderColor = SakuTheme.colors.expense,
    errorLabelColor = SakuTheme.colors.expense,
    errorTextColor = SakuTheme.colors.textPrimary
)

internal fun formatIndonesianDate(epochMs: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")))
}
