package com.saku.v2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saku.v2.data.store.QuickLogEventBus
import com.saku.v2.platform.ewallet.SakuIntentActions
import com.saku.v2.ui.MainScreen
import com.saku.v2.ui.SakuViewModel
import com.saku.v2.ui.theme.SakuTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private var externalQuickAddToken by mutableIntStateOf(0)
    private var notificationListenerEnabled by mutableStateOf(false)
    private var activeViewModel: SakuViewModel? = null

    private val exportZipLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            activeViewModel?.exportBackupZip(uri)
        }
    }

    private val importZipLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            activeViewModel?.importBackupZip(uri)
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val app = application as SakuApplication
        val signInResult = app.driveSyncManager.handleSignInResult(result.data)
        signInResult.fold(
            onSuccess = { email ->
                activeViewModel?.onDriveSignedIn(email)
            },
            onFailure = { error ->
                activeViewModel?.showMessage(
                    error.localizedMessage ?: "Gagal masuk ke akun Google"
                )
            }
        )
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            activeViewModel?.showMessage(
                "Izin notifikasi diperlukan untuk menampilkan saran Quick-Log"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationListenerEnabled = SakuIntentActions.isNotificationListenerEnabled(this)
        handleIncomingIntent(intent)

        setContent {
            val viewModel: SakuViewModel = viewModel()
            activeViewModel = viewModel

            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.preferences.eWalletListenerEnabled) {
                if (state.preferences.eWalletListenerEnabled) {
                    requestNotificationPermissionIfNeeded()
                }
            }

            SakuTheme(themeMode = state.preferences.themeMode) {
                MainScreen(
                    viewModel = viewModel,
                    externalOpenQuickAddToken = externalQuickAddToken,
                    isNotificationListenerEnabled = notificationListenerEnabled,
                    onExportZip = {
                        val fileName = "saku-v2-backup-${LocalDate.now()}.zip"
                        exportZipLauncher.launch(fileName)
                    },
                    onImportZip = {
                        importZipLauncher.launch(
                            arrayOf("application/zip", "application/x-zip-compressed", "*/*")
                        )
                    },
                    onConnectGoogleDrive = {
                        val app = application as SakuApplication
                        googleSignInLauncher.launch(app.driveSyncManager.createSignInIntent())
                    },
                    onOpenNotificationAccessSettings = {
                        requestNotificationPermissionIfNeeded()
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationListenerEnabled = SakuIntentActions.isNotificationListenerEnabled(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            SakuIntentActions.ACTION_QUICK_ADD -> {
                externalQuickAddToken += 1
                intent.action = null
            }
            SakuIntentActions.ACTION_QUICK_LOG_EWALLET -> {
                val parsed = SakuIntentActions.parsePendingQuickLog(intent)
                if (parsed != null) {
                    QuickLogEventBus.emit(parsed)
                }
                intent.action = null
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
