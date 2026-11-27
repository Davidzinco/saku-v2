package com.saku.v2.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.saku.v2.data.MediaManagerContract
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.Transaction
import com.saku.v2.data.model.TransactionType
import com.saku.v2.ui.components.SakuTintedIconBadge
import com.saku.v2.ui.components.formatIndonesianDateTime
import com.saku.v2.ui.components.transactionTypeLabel
import com.saku.v2.ui.theme.SakuTheme
import com.saku.v2.ui.theme.tabularNums

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    state: SakuAppState,
    mediaManager: MediaManagerContract,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SakuTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var revealAmount by remember(transaction.id, state.preferences.privacyBlur) {
        mutableStateOf(!state.preferences.privacyBlur)
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFullscreenReceipt by remember { mutableStateOf(false) }

    val sourceAccount = remember(state.accounts, transaction.accountId) {
        state.accounts.firstOrNull { it.id == transaction.accountId }
    }
    val destAccount = remember(state.accounts, transaction.toAccountId) {
        transaction.toAccountId?.let { id -> state.accounts.firstOrNull { it.id == id } }
    }
    val category = remember(state.categories, transaction.categoryId) {
        transaction.categoryId?.let { id -> state.categories.firstOrNull { it.id == id } }
    }
    val linkedGoal = remember(state.savingsGoals, transaction.savingsGoalId) {
        transaction.savingsGoalId?.let { id -> state.savingsGoals.firstOrNull { it.id == id } }
    }

    val (badgeBg, badgeTint, prefix) = when (transaction.type) {
        TransactionType.EXPENSE -> Triple(colors.expenseContainer, colors.expense, "-")
        TransactionType.INCOME -> Triple(colors.incomeContainer, colors.income, "+")
        TransactionType.TRANSFER -> Triple(colors.transferContainer, colors.transfer, "")
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = transactionTypeLabel(transaction.type),
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeTint
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup",
                        tint = colors.textSecondary
                    )
                }
            }

            // Amount display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedAmount = SakuStateCalculator.formatRupiah(
                    amount = transaction.amount,
                    masked = !revealAmount
                )
                Text(
                    text = if (revealAmount) "$prefix$formattedAmount" else formattedAmount,
                    style = MaterialTheme.typography.displayMedium.tabularNums.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (revealAmount) badgeTint else colors.textPrimary
                )
                if (state.preferences.privacyBlur) {
                    IconButton(onClick = { revealAmount = !revealAmount }) {
                        Icon(
                            imageVector = if (revealAmount) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "Tampilkan atau sembunyikan nominal",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            // Linked savings badge if any
            if (linkedGoal != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.incomeContainer)
                        .border(0.75.dp, colors.income.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = null,
                        tint = colors.income,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Terhubung ke target tabungan: ${linkedGoal.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.income
                    )
                }
            }

            // Detail metadata card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.canvasGradientStart)
                    .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (transaction.type == TransactionType.TRANSFER) {
                    Text(
                        text = "Alur Transfer",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val srcTint = sourceAccount?.let { Color(it.colorHex) } ?: colors.transfer
                            SakuTintedIconBadge(
                                iconKey = sourceAccount?.iconKey ?: "wallet",
                                tint = srcTint,
                                containerColor = srcTint.copy(alpha = 0.14f),
                                size = 34.dp
                            )
                            Text(
                                text = sourceAccount?.name ?: "Akun Asal",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "ke",
                            tint = colors.transfer,
                            modifier = Modifier.size(18.dp)
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dstTint = destAccount?.let { Color(it.colorHex) } ?: colors.transfer
                            SakuTintedIconBadge(
                                iconKey = destAccount?.iconKey ?: "wallet",
                                tint = dstTint,
                                containerColor = dstTint.copy(alpha = 0.14f),
                                size = 34.dp
                            )
                            Text(
                                text = destAccount?.name ?: "Akun Tujuan",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                } else {
                    DetailRow(
                        label = "Akun",
                        value = sourceAccount?.name ?: "Akun",
                        iconKey = sourceAccount?.iconKey ?: "wallet",
                        tint = sourceAccount?.let { Color(it.colorHex) } ?: colors.primary
                    )
                    HorizontalDivider(color = colors.cardBorderSubtle)
                    DetailRow(
                        label = "Kategori",
                        value = category?.name ?: "Tanpa Kategori",
                        iconKey = category?.iconKey ?: "category",
                        tint = category?.let { Color(it.colorHex) } ?: badgeTint
                    )
                }

                HorizontalDivider(color = colors.cardBorderSubtle)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Waktu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        text = formatIndonesianDateTime(transaction.timestampEpochMs),
                        style = MaterialTheme.typography.titleSmall.tabularNums,
                        color = colors.textPrimary
                    )
                }

                if (transaction.note.isNotBlank()) {
                    HorizontalDivider(color = colors.cardBorderSubtle)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Catatan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = transaction.note,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            // Receipt image preview if attached
            if (!transaction.receiptFileName.isNullOrBlank()) {
                val receiptFile = remember(transaction.receiptFileName) {
                    mediaManager.resolveMediaFile(transaction.receiptFileName)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Foto Struk",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.canvasGradientMid)
                            .border(0.75.dp, colors.cardBorderSubtle, RoundedCornerShape(18.dp))
                            .clickable { showFullscreenReceipt = true }
                    ) {
                        AsyncImage(
                            model = receiptFile,
                            contentDescription = "Foto struk transaksi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.sheetSurface.copy(alpha = 0.90f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = "Perbesar foto struk",
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.expense
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = colors.expense.copy(alpha = 0.45f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { onEdit(transaction) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Transaksi", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = colors.sheetSurface,
            title = {
                Text(
                    text = "Hapus transaksi ini?",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Saldo akun${if (linkedGoal != null) " dan progres target tabungan" else ""} akan otomatis disesuaikan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(transaction)
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

    if (showFullscreenReceipt && !transaction.receiptFileName.isNullOrBlank()) {
        val receiptFile = remember(transaction.receiptFileName) {
            mediaManager.resolveMediaFile(transaction.receiptFileName)
        }
        Dialog(
            onDismissRequest = { showFullscreenReceipt = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { showFullscreenReceipt = false }
                    .padding(16.dp)
            ) {
                AsyncImage(
                    model = receiptFile,
                    contentDescription = "Foto struk ukuran penuh",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
                IconButton(
                    onClick = { showFullscreenReceipt = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup pratinjau",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    iconKey: String,
    tint: Color
) {
    val colors = SakuTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SakuTintedIconBadge(
                iconKey = iconKey,
                tint = tint,
                containerColor = tint.copy(alpha = 0.14f),
                size = 28.dp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
        }
    }
}
