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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saku.v2.data.model.SakuAppState
import com.saku.v2.data.model.SakuStateCalculator
import com.saku.v2.data.model.ThemeMode
import com.saku.v2.ui.liquidglass.LiquidGlassButton
import com.saku.v2.ui.liquidglass.LiquidGlassCard
import com.saku.v2.ui.liquidglass.LiquidGlassToggle
import com.saku.v2.ui.theme.SakuTheme
import com.saku.v2.ui.theme.tabularNums
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PengaturanScreen(
    state: SakuAppState,
    syncInProgress: Boolean,
    isNotificationListenerEnabled: Boolean,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onTogglePrivacyBlur: () -> Unit,
    onOpenAccountsManagement: () -> Unit,
    onOpenCategoriesManagement: () -> Unit,
    onOpenBudgetSettings: () -> Unit,
    onOpenEWalletSettings: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onTriggerDemoQuickLog: () -> Unit,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
    onConnectOrUploadDrive: () -> Unit,
    onDownloadDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onOpenDriveGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    val prefs = state.preferences
    val driveMeta = state.driveSyncMeta
    val activeAccountCount = remember(state.accounts) { state.accounts.count { !it.isArchived } }
    val customCategoryCount = remember(state.categories) { state.categories.count { !it.isSystem } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "status_spacer") {
            Spacer(modifier = Modifier.statusBarsPadding())
        }

        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary
                )
                Text(
                    text = "Tampilan, deteksi Quick-Log, dan pencadangan data",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // Section 1: Tampilan & Privasi
        item(key = "appearance_privacy") {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionEyebrow(title = "TAMPILAN & PRIVASI")
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tema Mineral",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                ThemeModeSegmentedControl(
                    selectedMode = prefs.themeMode,
                    onSelectMode = onSelectThemeMode
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.cardBorderSubtle, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onTogglePrivacyBlur),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconTile(
                            icon = Icons.Rounded.VisibilityOff,
                            tint = colors.primary,
                            containerColor = colors.primaryContainer
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Privacy Mode",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Samarkan nominal saldo di Beranda, Tabungan & Widget",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    LiquidGlassToggle(
                        checked = prefs.privacyBlur,
                        onCheckedChange = { onTogglePrivacyBlur() }
                    )
                }
            }
        }

        // Section 2: Keuangan
        item(key = "finance_settings") {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionEyebrow(title = "KEUANGAN")
                Spacer(modifier = Modifier.height(8.dp))

                SettingsNavigationRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    tint = colors.primary,
                    containerColor = colors.primaryContainer,
                    title = "Kelola Sumber Dana / Akun",
                    subtitle = "$activeAccountCount akun aktif",
                    onClick = onOpenAccountsManagement
                )

                HorizontalDivider(
                    color = colors.cardBorderSubtle,
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                SettingsNavigationRow(
                    icon = Icons.Rounded.Category,
                    tint = colors.transfer,
                    containerColor = colors.transferContainer,
                    title = "Kelola Kategori",
                    subtitle = "$customCategoryCount kategori tersedia",
                    onClick = onOpenCategoriesManagement
                )

                HorizontalDivider(
                    color = colors.cardBorderSubtle,
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                SettingsNavigationRow(
                    icon = Icons.Rounded.PieChart,
                    tint = colors.warning,
                    containerColor = colors.warningContainer,
                    title = "Budget Bulanan",
                    subtitle = SakuStateCalculator.formatRupiah(
                        amount = prefs.monthlyBudget,
                        masked = prefs.privacyBlur
                    ),
                    onClick = onOpenBudgetSettings
                )
            }
        }

        // Section 3: Deteksi Notifikasi E-Wallet
        item(key = "ewallet_quicklog") {
            val isFullyActive = isNotificationListenerEnabled && prefs.eWalletListenerEnabled
            val statusLabel = when {
                isFullyActive -> "Aktif"
                isNotificationListenerEnabled -> "Siap • Nonaktif"
                else -> "Perlu Izin"
            }
            val statusTint = when {
                isFullyActive -> colors.income
                isNotificationListenerEnabled -> colors.transfer
                else -> colors.warning
            }
            val statusContainer = when {
                isFullyActive -> colors.incomeContainer
                isNotificationListenerEnabled -> colors.transferContainer
                else -> colors.warningContainer
            }

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionEyebrow(title = "DETEKSI NOTIFIKASI E-WALLET")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(statusContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusTint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SettingsIconTile(
                        icon = if (isFullyActive) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                        tint = statusTint,
                        containerColor = statusContainer
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Quick-Log Transaksi E-Wallet & Bank",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Membaca notifikasi pembayaran untuk mengisi pratinjau Quick-Log tanpa menyimpan otomatis. ${prefs.enabledEWalletPackages.size} aplikasi dipantau.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassButton(
                        onClick = onOpenEWalletSettings,
                        isPrimary = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Atur E-Wallet",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    LiquidGlassButton(
                        onClick = onOpenNotificationAccessSettings,
                        isPrimary = !isNotificationListenerEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (isNotificationListenerEnabled) "Izin Sistem" else "Beri Izin",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!isNotificationListenerEnabled) colors.onPrimary else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LiquidGlassButton(
                    onClick = onTriggerDemoQuickLog,
                    isPrimary = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Uji Simulasi Quick-Log",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section 4: Backup & Google Drive Sync
        item(key = "backup_and_drive") {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionEyebrow(title = "BACKUP & GOOGLE DRIVE SYNC")
                    if (syncInProgress) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = colors.primary
                            )
                            Text(
                                text = "Memproses...",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Backup Lokal (.zip)",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ekspor atau pulihkan seluruh transaksi, target tabungan, dan gambar WebP dalam satu arsip ZIP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LiquidGlassButton(
                        onClick = onExportZip,
                        isPrimary = false,
                        enabled = !syncInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileUpload,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Export Backup (.zip)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }

                    LiquidGlassButton(
                        onClick = onImportZip,
                        isPrimary = false,
                        enabled = !syncInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            tint = colors.transfer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Restore dari (.zip)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.cardBorderSubtle, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Google Drive (appDataFolder)",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onOpenDriveGuide,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Panduan Setup OAuth",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val connectedEmail = driveMeta.connectedEmail
                if (!connectedEmail.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.incomeContainer.copy(alpha = 0.72f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = colors.income,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = connectedEmail,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = driveMeta.lastSyncedEpochMs?.let {
                                    "Terakhir sinkron: ${formatSyncTimestamp(it)}"
                                } ?: "Terhubung • Belum pernah upload sync",
                                style = MaterialTheme.typography.bodySmall.tabularNums,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LiquidGlassButton(
                            onClick = onConnectOrUploadDrive,
                            isPrimary = true,
                            enabled = !syncInProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Upload Sync",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassButton(
                                onClick = onDownloadDrive,
                                isPrimary = false,
                                enabled = !syncInProgress,
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Restore dari Drive",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            LiquidGlassButton(
                                onClick = onDisconnectDrive,
                                isPrimary = false,
                                enabled = !syncInProgress,
                                modifier = Modifier
                                    .weight(0.85f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LinkOff,
                                    contentDescription = null,
                                    tint = colors.expense,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Putuskan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.expense,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.glassSurfaceFallback.copy(alpha = 0.6f))
                            .border(0.8.dp, colors.cardBorderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sinkronkan arsip ZIP terisolasi ke folder aplikasi pribadi di Google Drive Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LiquidGlassButton(
                        onClick = onConnectOrUploadDrive,
                        isPrimary = true,
                        enabled = !syncInProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
                }
            }
        }

        item(key = "footer") {
            Text(
                text = "Saku • Pengelola Keuangan Pribadi",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionEyebrow(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = SakuTheme.colors.textSecondary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ThemeModeSegmentedControl(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit
) {
    val colors = SakuTheme.colors
    val options = listOf(
        ThemeMode.LIGHT to "Terang",
        ThemeMode.DARK to "Gelap",
        ThemeMode.SYSTEM to "Sistem"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.glassSurfaceFallback.copy(alpha = 0.65f))
            .border(0.8.dp, colors.cardBorderSubtle, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (mode, label) ->
            val selected = mode == selectedMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) colors.primary else Color.Transparent)
                    .clickable { onSelectMode(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.onPrimary else colors.textSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsIconTile(
    icon: ImageVector,
    tint: Color,
    containerColor: Color
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    tint: Color,
    containerColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = SakuTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconTile(
                icon = icon,
                tint = tint,
                containerColor = containerColor
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatSyncTimestamp(epochMs: Long): String {
    val zoned = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    return zoned.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")))
}
