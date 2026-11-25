package com.saku.v2.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saku.v2.data.model.Account
import com.saku.v2.data.model.AccountType
import com.saku.v2.data.model.Category
import com.saku.v2.data.model.DriveSyncMeta
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.TransactionType
import com.saku.v2.ui.components.SakuTintedIconBadge
import com.saku.v2.ui.liquidglass.LiquidGlassButton
import com.saku.v2.ui.liquidglass.LiquidGlassToggle
import com.saku.v2.ui.theme.SakuTheme
import com.saku.v2.ui.theme.tabularNums
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ACCOUNT_ICONS = listOf("wallet", "ewallet", "bank", "payments", "savings", "shield")
private val CATEGORY_ICONS = listOf(
    "restaurant",
    "commute",
    "shopping_bag",
    "receipt_long",
    "movie",
    "favorite",
    "category",
    "payments",
    "card_giftcard",
    "trending_up",
    "home",
    "shield",
    "flight",
    "wallet"
)

private data class SupportedEWalletBrand(
    val name: String,
    val subtitle: String,
    val packages: Set<String>
)

private val SUPPORTED_EWALLET_BRANDS = listOf(
    SupportedEWalletBrand("GoPay / Gojek", "com.gojek.app, com.gopay.app", setOf("com.gojek.app", "com.gopay.app")),
    SupportedEWalletBrand("OVO", "ovo.id", setOf("ovo.id")),
    SupportedEWalletBrand("DANA", "id.dana", setOf("id.dana")),
    SupportedEWalletBrand("ShopeePay", "com.shopee.id", setOf("com.shopee.id")),
    SupportedEWalletBrand("LinkAja", "com.linkaja.android", setOf("com.linkaja.android", "com.telkom.mwallet")),
    SupportedEWalletBrand("BCA / myBCA", "com.bca, myBCA", setOf("com.bca", "com.bca.mybca.omni.android")),
    SupportedEWalletBrand("BRImo", "id.co.bri.brimo", setOf("id.co.bri.brimo")),
    SupportedEWalletBrand("Livin' Mandiri", "id.bmri.livin", setOf("id.bmri.livin")),
    SupportedEWalletBrand("SeaBank", "id.co.bankbkemobile.digitalbank", setOf("id.co.bankbkemobile.digitalbank")),
    SupportedEWalletBrand("Bank Jago", "com.jago.digitalBanking", setOf("com.jago.digitalBanking"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsManagementSheet(
    state: SakuAppState,
    onAddAccount: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        iconKey: String,
        colorHex: Long
    ) -> Unit,
    onUpdateAccount: (Account) -> Unit,
    onArchiveAccount: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val privacyBlur = state.preferences.privacyBlur

    var showEditor by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.EWALLET) }
    var balanceText by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("ewallet") }
    var colorHex by remember { mutableLongStateOf(SAKU_MINERAL_SWATCHES.first()) }

    fun openAddForm() {
        editingAccount = null
        name = ""
        type = AccountType.EWALLET
        balanceText = ""
        iconKey = "ewallet"
        colorHex = SAKU_MINERAL_SWATCHES.first()
        showEditor = true
    }

    fun openEditForm(account: Account) {
        editingAccount = account
        name = account.name
        type = account.type
        balanceText = account.initialBalance.coerceAtLeast(0L).toString()
        iconKey = account.iconKey
        colorHex = account.colorHex
        showEditor = true
    }

    val activeAccounts = remember(state.accounts) { state.accounts.filter { !it.isArchived } }
    val archivedAccounts = remember(state.accounts) { state.accounts.filter { it.isArchived } }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SakuSheetHeader(
                title = "Kelola Sumber Dana",
                subtitle = "${activeAccounts.size} aktif • ${archivedAccounts.size} diarsipkan",
                onDismiss = onDismiss
            )

            if (!showEditor) {
                LiquidGlassButton(
                    onClick = ::openAddForm,
                    backdrop = null,
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
                        text = "Tambah Akun / E-Wallet",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.glassSurfaceFallback)
                        .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingAccount == null) "Akun Baru" else "Edit Akun",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Akun") },
                        placeholder = { Text("Contoh: Blu, Mandiri, Kas Harian") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(16.dp),
                        colors = sakuOutlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Jenis Akun",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val types = listOf(
                                AccountType.CASH to "Tunai",
                                AccountType.EWALLET to "E-Wallet",
                                AccountType.BANK to "Bank",
                                AccountType.CUSTOM to "Lainnya"
                            )
                            items(types) { (t, label) ->
                                SakuQuickChip(
                                    label = label,
                                    isSelected = type == t,
                                    onClick = {
                                        type = t
                                        if (editingAccount == null) {
                                            iconKey = defaultIconForAccountType(t)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    SakuNominalTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it.filter(Char::isDigit).take(13) },
                        label = "Saldo Awal",
                        placeholder = "0"
                    )

                    SakuMineralColorPicker(
                        selectedColorHex = colorHex,
                        onSelectColor = { colorHex = it }
                    )

                    SakuIconKeyPicker(
                        iconKeys = ACCOUNT_ICONS,
                        selectedIconKey = iconKey,
                        accentColor = Color(colorHex),
                        onSelectIcon = { iconKey = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassButton(
                            onClick = { showEditor = false },
                            backdrop = null,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Batal",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }

                        val parsedBalance = balanceText.filter(Char::isDigit).toLongOrNull() ?: 0L
                        LiquidGlassButton(
                            onClick = {
                                val currentEditing = editingAccount
                                if (currentEditing == null) {
                                    onAddAccount(name.trim(), type, parsedBalance, iconKey, colorHex)
                                } else {
                                    onUpdateAccount(
                                        currentEditing.copy(
                                            name = name.trim(),
                                            type = type,
                                            initialBalance = parsedBalance,
                                            iconKey = iconKey,
                                            colorHex = colorHex
                                        )
                                    )
                                }
                                showEditor = false
                            },
                            backdrop = null,
                            isPrimary = true,
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Simpan",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onPrimary
                            )
                        }
                    }
                }
            }

            if (activeAccounts.isNotEmpty()) {
                Text(
                    text = "AKUN AKTIF",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeAccounts.forEach { account ->
                        AccountRowCard(
                            account = account,
                            balance = SakuStateCalculator.accountBalance(state, account.id),
                            privacyBlur = privacyBlur,
                            canArchive = activeAccounts.size > 1,
                            onEdit = { openEditForm(account) },
                            onToggleArchive = { onArchiveAccount(account.id, true) }
                        )
                    }
                }
            }

            if (archivedAccounts.isNotEmpty()) {
                Text(
                    text = "DIARSIPKAN",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    archivedAccounts.forEach { account ->
                        AccountRowCard(
                            account = account,
                            balance = SakuStateCalculator.accountBalance(state, account.id),
                            privacyBlur = privacyBlur,
                            canArchive = true,
                            onEdit = { openEditForm(account) },
                            onToggleArchive = { onArchiveAccount(account.id, false) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountRowCard(
    account: Account,
    balance: Long,
    privacyBlur: Boolean,
    canArchive: Boolean,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val colors = SakuTheme.colors
    val accentColor = resolveMineralAccent(account.colorHex, colors.isDark, colors.primary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.glassSurfaceFallback)
            .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SakuTintedIconBadge(
                iconKey = account.iconKey,
                tint = accentColor,
                containerColor = accentColor.copy(alpha = if (colors.isDark) 0.24f else 0.14f),
                size = 38.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${accountTypeLabel(account.type)} • ${SakuStateCalculator.formatRupiah(balance, masked = privacyBlur)}",
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Akun",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onToggleArchive,
                enabled = canArchive
            ) {
                Icon(
                    imageVector = if (account.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                    contentDescription = if (account.isArchived) "Aktifkan Akun" else "Arsipkan Akun",
                    tint = if (canArchive) colors.textSecondary else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesManagementSheet(
    state: SakuAppState,
    onAddCategory: (
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long,
        monthlyLimit: Long?
    ) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showAddForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("category") }
    var colorHex by remember { mutableLongStateOf(SAKU_MINERAL_SWATCHES.first()) }

    val filteredCategories = remember(state.categories, selectedType) {
        state.categories.filter { it.type == selectedType }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SakuSheetHeader(
                title = "Kelola Kategori",
                subtitle = "Atur kategori pemasukan, pengeluaran & batas bulanan",
                onDismiss = onDismiss
            )

            // Type switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.glassSurfaceFallback)
                    .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Pengeluaran",
                    TransactionType.INCOME to "Pemasukan"
                ).forEach { (type, label) ->
                    val selected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.primary else Color.Transparent)
                            .clickable { selectedType = type }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }

            if (!showAddForm) {
                LiquidGlassButton(
                    onClick = {
                        name = ""
                        limitText = ""
                        iconKey = if (selectedType == TransactionType.EXPENSE) "category" else "payments"
                        colorHex = SAKU_MINERAL_SWATCHES.first()
                        showAddForm = true
                    },
                    backdrop = null,
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
                        text = if (selectedType == TransactionType.EXPENSE) {
                            "Tambah Kategori Pengeluaran"
                        } else {
                            "Tambah Kategori Pemasukan"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.glassSurfaceFallback)
                        .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Kategori Baru (${if (selectedType == TransactionType.EXPENSE) "Pengeluaran" else "Pemasukan"})",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Kategori") },
                        placeholder = { Text("Contoh: Kopi, Freelance, Langganan") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(16.dp),
                        colors = sakuOutlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedType == TransactionType.EXPENSE) {
                        SakuNominalTextField(
                            value = limitText,
                            onValueChange = { limitText = it.filter(Char::isDigit).take(13) },
                            label = "Batas Bulanan (Opsional)",
                            placeholder = "500000"
                        )
                    }

                    SakuMineralColorPicker(
                        selectedColorHex = colorHex,
                        onSelectColor = { colorHex = it }
                    )

                    SakuIconKeyPicker(
                        iconKeys = CATEGORY_ICONS,
                        selectedIconKey = iconKey,
                        accentColor = Color(colorHex),
                        onSelectIcon = { iconKey = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassButton(
                            onClick = { showAddForm = false },
                            backdrop = null,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Batal",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }

                        LiquidGlassButton(
                            onClick = {
                                val parsedLimit = limitText.filter(Char::isDigit).toLongOrNull()
                                    ?.takeIf { it > 0L && selectedType == TransactionType.EXPENSE }
                                onAddCategory(name.trim(), selectedType, iconKey, colorHex, parsedLimit)
                                showAddForm = false
                            },
                            backdrop = null,
                            isPrimary = true,
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Simpan",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onPrimary
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredCategories.forEach { category ->
                    CategoryRowCard(
                        category = category,
                        privacyBlur = state.preferences.privacyBlur,
                        onDelete = { onDeleteCategory(category.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryRowCard(
    category: Category,
    privacyBlur: Boolean,
    onDelete: () -> Unit
) {
    val colors = SakuTheme.colors
    val accentColor = resolveMineralAccent(category.colorHex, colors.isDark, colors.primary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.glassSurfaceFallback)
            .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SakuTintedIconBadge(
                iconKey = category.iconKey,
                tint = accentColor,
                containerColor = accentColor.copy(alpha = if (colors.isDark) 0.24f else 0.14f),
                size = 38.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val limit = category.monthlyLimit
                if (limit != null && limit > 0L) {
                    Text(
                        text = "Batas: ${SakuStateCalculator.formatRupiah(limit, masked = privacyBlur)} / bln",
                        style = MaterialTheme.typography.bodySmall.tabularNums,
                        color = colors.textSecondary
                    )
                } else if (category.isSystem) {
                    Text(
                        text = "Kategori sistem otomatis",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        }

        if (category.isSystem) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Sistem",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary
                )
            }
        } else {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Hapus Kategori",
                    tint = colors.expense,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyBudgetSheet(
    currentBudget: Long,
    onSaveBudget: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var budgetText by remember(currentBudget) { mutableStateOf(currentBudget.toString()) }

    val parsedBudget = budgetText.filter(Char::isDigit).toLongOrNull() ?: 0L

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
                title = "Budget Bulanan",
                subtitle = "Batas total pengeluaran konsumsi setiap bulan",
                onDismiss = onDismiss
            )

            SakuNominalTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter(Char::isDigit).take(13) },
                label = "Nominal Budget Bulanan",
                placeholder = "5000000"
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(
                    "2,5 jt" to 2_500_000L,
                    "5 jt" to 5_000_000L,
                    "7,5 jt" to 7_500_000L,
                    "10 jt" to 10_000_000L,
                    "15 jt" to 15_000_000L
                )
                items(presets) { (label, amount) ->
                    SakuQuickChip(
                        label = label,
                        isSelected = parsedBudget == amount,
                        onClick = { budgetText = amount.toString() }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.transferContainer)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = colors.transfer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Setoran tabungan tidak dihitung sebagai pengeluaran konsumsi sehingga progres budget tetap akurat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textPrimary
                )
            }

            LiquidGlassButton(
                onClick = {
                    onSaveBudget(parsedBudget)
                    onDismiss()
                },
                backdrop = null,
                isPrimary = true,
                enabled = parsedBudget > 0L,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Simpan Budget Bulanan",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EWalletSettingsSheet(
    state: SakuAppState,
    isNotificationListenerEnabled: Boolean,
    onUpdateSettings: (enabled: Boolean, packages: Set<String>) -> Unit,
    onOpenSystemNotificationAccess: () -> Unit,
    onTriggerDemoQuickLog: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var enabled by remember(state.preferences.eWalletListenerEnabled) {
        mutableStateOf(state.preferences.eWalletListenerEnabled)
    }
    var selectedPackages by remember(state.preferences.enabledEWalletPackages) {
        mutableStateOf(state.preferences.enabledEWalletPackages)
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SakuSheetHeader(
                title = "Deteksi Notifikasi E-Wallet",
                subtitle = "Isi otomatis form Quick-Log tanpa menyimpan transaksi tanpa izin Anda",
                onDismiss = onDismiss
            )

            // System Permission Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isNotificationListenerEnabled) colors.incomeContainer else colors.warningContainer
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isNotificationListenerEnabled) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.WarningAmber
                    },
                    contentDescription = null,
                    tint = if (isNotificationListenerEnabled) colors.income else colors.warning,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNotificationListenerEnabled) {
                            "Izin Akses Notifikasi Aktif"
                        } else {
                            "Izin Akses Notifikasi Belum Aktif"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isNotificationListenerEnabled) {
                            "Layanan pembaca notifikasi siap menerima pembayaran e-wallet & bank."
                        } else {
                            "Aktifkan akses notifikasi Android untuk mendeteksi transaksi masuk/keluar."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            if (!isNotificationListenerEnabled) {
                LiquidGlassButton(
                    onClick = onOpenSystemNotificationAccess,
                    backdrop = null,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Buka Pengaturan Izin Notifikasi",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                }
            }

            // Master Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.glassSurfaceFallback)
                    .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
                    .clickable {
                        val next = !enabled
                        enabled = next
                        onUpdateSettings(next, selectedPackages)
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Aktifkan Deteksi Quick-Log",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Hanya memproses notifikasi pembayaran valid (mengabaikan OTP & promo)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                LiquidGlassToggle(
                    checked = enabled,
                    onCheckedChange = { next ->
                        enabled = next
                        onUpdateSettings(next, selectedPackages)
                    },
                    backdrop = null
                )
            }

            Text(
                text = "APLIKASI YANG DIPANTAU",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.glassSurfaceFallback)
                    .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(18.dp))
            ) {
                SUPPORTED_EWALLET_BRANDS.forEachIndexed { index, brand ->
                    val isChecked = brand.packages.any { it in selectedPackages }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val nextPackages = if (isChecked) {
                                    selectedPackages - brand.packages
                                } else {
                                    selectedPackages + brand.packages
                                }
                                selectedPackages = nextPackages
                                onUpdateSettings(enabled, nextPackages)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = brand.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = brand.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val nextPackages = if (checked) {
                                    selectedPackages + brand.packages
                                } else {
                                    selectedPackages - brand.packages
                                }
                                selectedPackages = nextPackages
                                onUpdateSettings(enabled, nextPackages)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.primary,
                                checkmarkColor = colors.onPrimary
                            )
                        )
                    }
                    if (index < SUPPORTED_EWALLET_BRANDS.lastIndex) {
                        HorizontalDivider(color = colors.cardBorderSubtle, thickness = 0.7.dp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassButton(
                    onClick = {
                        onTriggerDemoQuickLog()
                        onDismiss()
                    },
                    backdrop = null,
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Uji Simulasi",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary
                    )
                }

                LiquidGlassButton(
                    onClick = {
                        onUpdateSettings(enabled, selectedPackages)
                        onDismiss()
                    },
                    backdrop = null,
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Selesai",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveSyncGuideSheet(
    driveMeta: DriveSyncMeta,
    onStartGoogleSignIn: () -> Unit,
    onSyncUpload: () -> Unit,
    onSyncDownload: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val connectedEmail = driveMeta.connectedEmail

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SakuSheetHeader(
                title = "Google Drive Sync & Panduan OAuth",
                subtitle = "Cadangan otomatis ke appDataFolder pribadi yang terisolasi",
                onDismiss = onDismiss
            )

            // Current Connection Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (!connectedEmail.isNullOrBlank()) colors.incomeContainer else colors.glassSurfaceFallback
                    )
                    .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = if (!connectedEmail.isNullOrBlank()) colors.income else colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = connectedEmail ?: "Belum terhubung ke akun Google",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = driveMeta.lastSyncedEpochMs?.let { epoch ->
                                val zoned = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault())
                                val formatted = zoned.format(
                                    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
                                )
                                "Sinkronisasi terakhir: $formatted"
                            } ?: "Arsip: saku_v2_sync.zip (appDataFolder)",
                            style = MaterialTheme.typography.bodySmall.tabularNums,
                            color = colors.textSecondary
                        )
                    }
                }

                if (connectedEmail.isNullOrBlank()) {
                    LiquidGlassButton(
                        onClick = {
                            onStartGoogleSignIn()
                            onDismiss()
                        },
                        backdrop = null,
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            tint = colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Hubungkan Akun Google",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onPrimary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassButton(
                            onClick = {
                                onSyncUpload()
                                onDismiss()
                            },
                            backdrop = null,
                            isPrimary = true,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Upload Sync",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onPrimary
                            )
                        }
                        LiquidGlassButton(
                            onClick = {
                                onSyncDownload()
                                onDismiss()
                            },
                            backdrop = null,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Restore Drive",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onDisconnect()
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LinkOff,
                            contentDescription = null,
                            tint = colors.expense,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Putuskan Akun Google",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.expense
                        )
                    }
                }
            }

            // OAuth Setup Guide Steps
            Text(
                text = "PANDUAN KONFIGURASI OAUTH",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )

            GuideStepCard(
                stepNumber = "1",
                title = "Aktifkan Google Drive API",
                body = "Di Google Cloud Console, buat proyek lalu aktifkan Google Drive API. Tambahkan scope drive.appdata pada layar OAuth Consent Screen."
            )
            GuideStepCard(
                stepNumber = "2",
                title = "Daftarkan Android OAuth Client ID",
                body = "Buat kredensial OAuth 2.0 Client ID bertipe Android dengan package name com.saku.v2 serta sidik jari SHA-1 keystore debug atau release."
            )
            GuideStepCard(
                stepNumber = "3",
                title = "Tambahkan Penguji & Uji Sync",
                body = "Jika status aplikasi masih Testing, masukkan email Google Anda ke daftar Test Users. Saku hanya mengakses file saku_v2_sync.zip di appDataFolder tersembunyi."
            )

            LiquidGlassButton(
                onClick = onDismiss,
                backdrop = null,
                isPrimary = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tutup Panduan",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GuideStepCard(
    stepNumber: String,
    title: String,
    body: String
) {
    val colors = SakuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.glassSurfaceFallback)
            .border(1.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium.tabularNums,
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

private fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.CASH -> "Tunai"
    AccountType.EWALLET -> "E-Wallet"
    AccountType.BANK -> "Bank"
    AccountType.CUSTOM -> "Lainnya"
}

private fun defaultIconForAccountType(type: AccountType): String = when (type) {
    AccountType.CASH -> "wallet"
    AccountType.EWALLET -> "ewallet"
    AccountType.BANK -> "bank"
    AccountType.CUSTOM -> "category"
}
