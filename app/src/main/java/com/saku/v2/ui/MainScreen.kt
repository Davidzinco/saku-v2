package com.saku.v2.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.saku.v2.data.model.SavingsGoal
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import com.saku.v2.data.store.PendingQuickLog
import com.saku.v2.ui.liquidglass.AuroraBackground
import com.saku.v2.ui.liquidglass.LiquidGlassBottomBar
import com.saku.v2.ui.liquidglass.SakuBottomTabItem
import com.saku.v2.ui.screens.BerandaScreen
import com.saku.v2.ui.screens.NabungScreen
import com.saku.v2.ui.screens.PengaturanScreen
import com.saku.v2.ui.screens.TransaksiScreen
import com.saku.v2.ui.sheets.AccountsManagementSheet
import com.saku.v2.ui.sheets.CategoriesManagementSheet
import com.saku.v2.ui.sheets.DriveSyncGuideSheet
import com.saku.v2.ui.sheets.EWalletSettingsSheet
import com.saku.v2.ui.sheets.MonthlyBudgetSheet
import com.saku.v2.ui.sheets.SavingsGoalFormSheet
import com.saku.v2.ui.sheets.SavingsTransferSheet
import com.saku.v2.ui.sheets.TransactionDetailSheet
import com.saku.v2.ui.sheets.TransactionFormSheet
import com.saku.v2.ui.theme.SakuTheme
import kotlinx.coroutines.launch

private data class TransactionFormRequest(
    val initialTransaction: Transaction? = null,
    val initialType: TransactionType = TransactionType.EXPENSE,
    val quickLog: PendingQuickLog? = null
)

@Composable
fun MainScreen(
    viewModel: SakuViewModel,
    externalOpenQuickAddToken: Int = 0,
    isNotificationListenerEnabled: Boolean = false,
    onExportZip: () -> Unit = {},
    onImportZip: () -> Unit = {},
    onConnectGoogleDrive: () -> Unit = {},
    onOpenNotificationAccessSettings: () -> Unit = {}
) {
    val colors = SakuTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingQuickLog by viewModel.pendingQuickLog.collectAsStateWithLifecycle()
    val syncInProgress by viewModel.syncInProgress.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })

    val canvasBackdrop = rememberLayerBackdrop()
    val shellBackdrop = rememberLayerBackdrop()

    var txFormRequest by remember { mutableStateOf<TransactionFormRequest?>(null) }
    var detailTransaction by remember { mutableStateOf<Transaction?>(null) }

    var showGoalForm by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var transferRequest by remember { mutableStateOf<Pair<SavingsGoal, Boolean>?>(null) }

    var showAccountsSheet by remember { mutableStateOf(false) }
    var showCategoriesSheet by remember { mutableStateOf(false) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var showEWalletSheet by remember { mutableStateOf(false) }
    var showDriveGuideSheet by remember { mutableStateOf(false) }

    var lastHandledQuickAddToken by rememberSaveable { mutableIntStateOf(0) }
    var lastOpenedQuickLogId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.uiMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(externalOpenQuickAddToken) {
        if (externalOpenQuickAddToken > lastHandledQuickAddToken) {
            lastHandledQuickAddToken = externalOpenQuickAddToken
            showAccountsSheet = false
            showCategoriesSheet = false
            showBudgetSheet = false
            showEWalletSheet = false
            showDriveGuideSheet = false
            txFormRequest = TransactionFormRequest(initialType = TransactionType.EXPENSE)
        }
    }

    LaunchedEffect(pendingQuickLog?.id) {
        val current = pendingQuickLog
        if (current != null && current.id != lastOpenedQuickLogId) {
            lastOpenedQuickLogId = current.id
            showEWalletSheet = false
            txFormRequest = TransactionFormRequest(
                initialType = current.type,
                quickLog = current
            )
        }
    }

    val tabs = remember {
        listOf(
            SakuBottomTabItem("Beranda", Icons.Rounded.Home),
            SakuBottomTabItem("Transaksi", Icons.AutoMirrored.Rounded.ReceiptLong),
            SakuBottomTabItem("Nabung", Icons.Rounded.Savings),
            SakuBottomTabItem("Pengaturan", Icons.Rounded.Settings)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1 + 2: Ambient aurora + HorizontalPager captured in shellBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(shellBackdrop)
        ) {
            AuroraBackground(backdrop = canvasBackdrop) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> BerandaScreen(
                            state = state,
                            pendingQuickLog = pendingQuickLog,
                            mediaManager = viewModel.mediaManager,
                            onTogglePrivacyBlur = viewModel::togglePrivacyBlur,
                            onOpenNewTransaction = { type ->
                                txFormRequest = TransactionFormRequest(initialType = type)
                            },
                            onOpenQuickLogForm = { log ->
                                txFormRequest = TransactionFormRequest(
                                    initialType = log.type,
                                    quickLog = log
                                )
                            },
                            onDismissQuickLog = viewModel::dismissQuickLog,
                            onOpenTransactionDetail = { tx ->
                                detailTransaction = tx
                            },
                            onNavigateToTransaksi = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onNavigateToNabung = {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            },
                            onNavigateToPengaturan = {
                                coroutineScope.launch { pagerState.animateScrollToPage(3) }
                            }
                        )

                        1 -> TransaksiScreen(
                            state = state,
                            mediaManager = viewModel.mediaManager,
                            onOpenNewTransaction = { type ->
                                txFormRequest = TransactionFormRequest(initialType = type)
                            },
                            onOpenTransactionDetail = { tx ->
                                detailTransaction = tx
                            }
                        )

                        2 -> NabungScreen(
                            state = state,
                            mediaManager = viewModel.mediaManager,
                            onOpenCreateGoal = {
                                editingGoal = null
                                showGoalForm = true
                            },
                            onOpenEditGoal = { goal ->
                                editingGoal = goal
                                showGoalForm = true
                            },
                            onOpenDeposit = { goal ->
                                transferRequest = goal to true
                            },
                            onOpenWithdraw = { goal ->
                                transferRequest = goal to false
                            },
                            onDeleteGoal = { goal ->
                                viewModel.deleteSavingsGoal(goal.id)
                            }
                        )

                        3 -> PengaturanScreen(
                            state = state,
                            syncInProgress = syncInProgress,
                            isNotificationListenerEnabled = isNotificationListenerEnabled,
                            onSelectThemeMode = viewModel::updateThemeMode,
                            onTogglePrivacyBlur = viewModel::togglePrivacyBlur,
                            onOpenAccountsManagement = { showAccountsSheet = true },
                            onOpenCategoriesManagement = { showCategoriesSheet = true },
                            onOpenBudgetSettings = { showBudgetSheet = true },
                            onOpenEWalletSettings = { showEWalletSheet = true },
                            onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                            onTriggerDemoQuickLog = viewModel::triggerDemoQuickLog,
                            onExportZip = onExportZip,
                            onImportZip = onImportZip,
                            onConnectOrUploadDrive = {
                                if (state.driveSyncMeta.connectedEmail.isNullOrBlank()) {
                                    onConnectGoogleDrive()
                                } else {
                                    viewModel.syncUploadDrive()
                                }
                            },
                            onDownloadDrive = viewModel::syncDownloadDrive,
                            onDisconnectDrive = viewModel::disconnectDrive,
                            onOpenDriveGuide = { showDriveGuideSheet = true }
                        )
                    }
                }
            }
        }

        // Floating Snackbar above the dock (kept outside shellBackdrop)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 92.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(16.dp),
                containerColor = colors.sheetSurface,
                contentColor = colors.textPrimary,
                actionColor = colors.primary,
                modifier = Modifier.border(
                    width = 0.75.dp,
                    color = colors.cardBorderSubtle,
                    shape = RoundedCornerShape(16.dp)
                )
            )
        }

        // Layer 3: Floating Glass Dock outside shellBackdrop so it refracts scrolling pages cleanly
        LiquidGlassBottomBar(
            pagerState = pagerState,
            tabs = tabs,
            backdrop = shellBackdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        )
    }

    // Active Sheets
    txFormRequest?.let { request ->
        TransactionFormSheet(
            state = state,
            mediaManager = viewModel.mediaManager,
            initialTransaction = request.initialTransaction,
            initialType = request.initialType,
            initialQuickLog = request.quickLog,
            onSaveNew = { type, amount, accountId, toAccountId, categoryId, savingsGoalId, note, timestampEpochMs, receiptFileName ->
                viewModel.addTransaction(
                    type = type,
                    amount = amount,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    savingsGoalId = savingsGoalId,
                    note = note,
                    timestampEpochMs = timestampEpochMs,
                    receiptFileName = receiptFileName
                )
                if (request.quickLog != null) {
                    viewModel.dismissQuickLog()
                }
                txFormRequest = null
            },
            onUpdateExisting = { updatedTx ->
                viewModel.updateTransaction(updatedTx)
                txFormRequest = null
            },
            onDismiss = {
                if (request.quickLog != null) {
                    viewModel.dismissQuickLog()
                }
                txFormRequest = null
            }
        )
    }

    val liveDetailTx = detailTransaction?.let { target ->
        state.transactions.firstOrNull { it.id == target.id }
    }
    if (liveDetailTx != null) {
        TransactionDetailSheet(
            transaction = liveDetailTx,
            state = state,
            mediaManager = viewModel.mediaManager,
            onEdit = { tx ->
                detailTransaction = null
                txFormRequest = TransactionFormRequest(initialTransaction = tx)
            },
            onDelete = { tx ->
                viewModel.deleteTransaction(tx.id)
                detailTransaction = null
            },
            onDismiss = { detailTransaction = null }
        )
    }

    if (showGoalForm) {
        SavingsGoalFormSheet(
            initialGoal = editingGoal,
            mediaManager = viewModel.mediaManager,
            onSave = { name, targetAmount, coverFileName, deadlineEpochMs, colorHex, iconKey, existingGoal ->
                if (existingGoal == null) {
                    viewModel.addSavingsGoal(
                        name = name,
                        targetAmount = targetAmount,
                        coverFileName = coverFileName,
                        deadlineEpochMs = deadlineEpochMs,
                        colorHex = colorHex,
                        iconKey = iconKey
                    )
                } else {
                    viewModel.updateSavingsGoal(
                        existingGoal.copy(
                            name = name,
                            targetAmount = targetAmount,
                            coverFileName = coverFileName,
                            deadlineEpochMs = deadlineEpochMs,
                            colorHex = colorHex,
                            iconKey = iconKey
                        )
                    )
                }
                showGoalForm = false
                editingGoal = null
            },
            onDelete = if (editingGoal != null) {
                { goal ->
                    viewModel.deleteSavingsGoal(goal.id)
                    showGoalForm = false
                    editingGoal = null
                }
            } else {
                null
            },
            onDismiss = {
                showGoalForm = false
                editingGoal = null
            }
        )
    }

    transferRequest?.let { (goal, isDeposit) ->
        SavingsTransferSheet(
            goal = goal,
            isDeposit = isDeposit,
            state = state,
            onConfirm = { goalId, amount, accountId, note ->
                if (isDeposit) {
                    viewModel.depositToSavings(
                        goalId = goalId,
                        amount = amount,
                        fromAccountId = accountId,
                        note = note
                    )
                } else {
                    viewModel.withdrawFromSavings(
                        goalId = goalId,
                        amount = amount,
                        toAccountId = accountId,
                        note = note
                    )
                }
                transferRequest = null
            },
            onDismiss = { transferRequest = null }
        )
    }

    if (showAccountsSheet) {
        AccountsManagementSheet(
            state = state,
            onAddAccount = viewModel::addAccount,
            onUpdateAccount = viewModel::updateAccount,
            onArchiveAccount = viewModel::archiveAccount,
            onDismiss = { showAccountsSheet = false }
        )
    }

    if (showCategoriesSheet) {
        CategoriesManagementSheet(
            state = state,
            onAddCategory = viewModel::addCategory,
            onDeleteCategory = viewModel::deleteCategory,
            onDismiss = { showCategoriesSheet = false }
        )
    }

    if (showBudgetSheet) {
        MonthlyBudgetSheet(
            currentBudget = state.preferences.monthlyBudget,
            onSaveBudget = { newBudget ->
                viewModel.updateMonthlyBudget(newBudget)
                showBudgetSheet = false
            },
            onDismiss = { showBudgetSheet = false }
        )
    }

    if (showEWalletSheet) {
        EWalletSettingsSheet(
            state = state,
            isNotificationListenerEnabled = isNotificationListenerEnabled,
            onUpdateSettings = viewModel::updateEWalletSettings,
            onOpenSystemNotificationAccess = onOpenNotificationAccessSettings,
            onTriggerDemoQuickLog = viewModel::triggerDemoQuickLog,
            onDismiss = { showEWalletSheet = false }
        )
    }

    if (showDriveGuideSheet) {
        DriveSyncGuideSheet(
            driveMeta = state.driveSyncMeta,
            onStartGoogleSignIn = onConnectGoogleDrive,
            onSyncUpload = viewModel::syncUploadDrive,
            onSyncDownload = viewModel::syncDownloadDrive,
            onDisconnect = viewModel::disconnectDrive,
            onDismiss = { showDriveGuideSheet = false }
        )
    }
}
