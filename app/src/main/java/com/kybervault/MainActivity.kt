package com.kybervault

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kybervault.i18n.LocalStrings
import com.kybervault.i18n.getStrings
import com.kybervault.security.BiometricHelper
import com.kybervault.security.SecurityHardening
import com.kybervault.ui.VaultViewModel
import com.kybervault.ui.components.InfoDialog
import com.kybervault.ui.components.SaveWarningDialog
import com.kybervault.ui.components.SecurityBanner
import com.kybervault.ui.components.SettingsDialog
import com.kybervault.ui.components.PinDialog
import com.kybervault.ui.components.PinMode
import com.kybervault.ui.components.TotpVerifyDialog
import com.kybervault.ui.screens.ExchangeScreen
import com.kybervault.ui.screens.KeyGenScreen
import com.kybervault.ui.screens.MessageScreen
import com.kybervault.ui.theme.KyberVaultTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("kybervault_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("flag_secure", true)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        try { startService(Intent(this, WipeService::class.java)) } catch (_: Exception) {}

        // Anti-tapjacking, anti-overlay
        SecurityHardening.hardenWindow(this)

        // Initial threat scan
        val threatReport = SecurityHardening.scan(this)

        enableEdgeToEdge()
        setContent {
            val viewModel: VaultViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val strings = getStrings(uiState.language)
            KyberVaultTheme(dyslexicFont = uiState.dyslexicFont) {
                CompositionLocalProvider(LocalStrings provides strings) {
                    KyberVaultMainScreen(viewModel, uiState, threatReport, this@MainActivity)
                }
            }
        }
    }
}

enum class VaultTab(val icon: ImageVector) {
    KEYGEN(Icons.Filled.VpnKey),
    EXCHANGE(Icons.Filled.SwapHoriz),
    MESSAGE(Icons.Filled.Chat)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KyberVaultMainScreen(
    viewModel: VaultViewModel,
    uiState: com.kybervault.ui.VaultUiState,
    initialThreatReport: SecurityHardening.ThreatReport,
    activity: FragmentActivity
) {
    val S = LocalStrings.current
    val pagerState = rememberPagerState(pageCount = { VaultTab.entries.size })
    val coroutineScope = rememberCoroutineScope()
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var threatReport by remember { mutableStateOf(initialThreatReport) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPinVerify by remember { mutableStateOf(false) }
    var showTotpVerify by remember { mutableStateOf(false) }

    // Auth gate priority: biometric > TOTP > PIN > direct
    fun requireAuth(action: () -> Unit) {
        when {
            uiState.requireBiometric && BiometricHelper.isAvailable(activity) -> {
                BiometricHelper.authenticate(activity,
                    title = "Authenticate",
                    subtitle = "Verify identity to access key storage",
                    negativeButtonText = S.cancel,
                    onSuccess = action,
                    onFailure = { })
            }
            uiState.requireTotp && uiState.hasTotp -> {
                pendingAuthAction = action
                showTotpVerify = true
            }
            uiState.requirePin && uiState.hasPin -> {
                pendingAuthAction = action
                showPinVerify = true
            }
            else -> action()
        }
    }

    // Periodic debugger re-check every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5_000)
            if (SecurityHardening.isDebuggerAttached() && !threatReport.isDebuggerAttached) {
                threatReport = SecurityHardening.scan(activity)
                bannerDismissed = false
            }
        }
    }

    val tabLabels = listOf(S.tabKeygen, S.tabExchange, S.tabMessage)

    LaunchedEffect(uiState.flagSecure) {
        if (uiState.flagSecure) activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    if (showPinVerify && pendingAuthAction != null) {
        PinDialog(
            mode = PinMode.VERIFY,
            verifyPin = viewModel::verifyPin,
            onPinConfirmed = { showPinVerify = false; pendingAuthAction?.invoke(); pendingAuthAction = null },
            onDismiss = { showPinVerify = false; pendingAuthAction = null },
            onVerifyFailed = { showPinVerify = false; pendingAuthAction = null }
        )
    }

    if (showTotpVerify && pendingAuthAction != null) {
        TotpVerifyDialog(
            verifyCode = viewModel::verifyTotp,
            onVerified = { showTotpVerify = false; pendingAuthAction?.invoke(); pendingAuthAction = null },
            onDismiss = { showTotpVerify = false; pendingAuthAction = null }
        )
    }

    if (uiState.showSaveWarningDialog) {
        SaveWarningDialog(
            onConfirm = {
                viewModel.dismissSaveWarning()
                requireAuth { viewModel.saveKeyToStorage(uiState.activeAlias) }
            },
            onDismiss = { viewModel.dismissSaveWarning() }
        )
    }
    if (showInfoDialog) { InfoDialog(onDismiss = { showInfoDialog = false }) }
    if (showSettingsDialog) {
        SettingsDialog(
            flagSecure = uiState.flagSecure, onFlagSecureChange = viewModel::setFlagSecure,
            wipeOnExit = uiState.wipeOnExit, onWipeOnExitChange = viewModel::setWipeOnExit,
            hideCopyWarning = uiState.hideCopyWarning, onHideCopyWarningChange = viewModel::setHideCopyWarning,
            displayEncoding = uiState.displayEncoding, onDisplayEncodingChange = viewModel::setDisplayEncoding,
            useWireFormat = uiState.useWireFormat, onUseWireFormatChange = viewModel::setUseWireFormat,
            dyslexicFont = uiState.dyslexicFont, onDyslexicFontChange = viewModel::setDyslexicFont,
            language = uiState.language, onLanguageChange = viewModel::setLanguage,
            requireBiometric = uiState.requireBiometric, onRequireBiometricChange = viewModel::setRequireBiometric,
            biometricAvailable = BiometricHelper.isAvailable(activity),
            requirePin = uiState.requirePin,
            onSetPin = viewModel::setPin, onDisablePin = viewModel::disablePin,
            requireTotp = uiState.requireTotp,
            onEnableTotp = viewModel::enableTotp, onDisableTotp = viewModel::disableTotp,
            onDismiss = { showSettingsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(S.appName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, S.info, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, S.settings, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface) {
                VaultTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tabLabels[index]) },
                        label = { Text(tabLabels[index]) },
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!bannerDismissed || threatReport.hasCriticalThreats) {
                SecurityBanner(report = threatReport, onDismiss = { bannerDismissed = true })
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (VaultTab.entries[page]) {
                    VaultTab.KEYGEN -> KeyGenScreen(
                        uiState = uiState,
                        onGenerate = viewModel::generateKeyPair,
                        onSetMode = viewModel::setEncryptionMode,
                        onTogglePrivateKeys = viewModel::togglePrivateKeyVisibility,
                        onSaveRequested = viewModel::showSaveWarning,
                        onLoadRequested = { alias ->
                            requireAuth { viewModel.loadKeyFromStorage(alias) }
                        },
                        onWipeRamOnly = viewModel::wipeRamOnly,
                        onWipeRamAndStorage = viewModel::wipeRamAndStorage,
                        onHideCopyWarning = { viewModel.setHideCopyWarning(true) },
                    )
                    VaultTab.EXCHANGE -> ExchangeScreen(
                        uiState = uiState,
                        onImportRecipientKey = viewModel::importRecipientKey,
                        onSend = viewModel::sendKemExchange,
                        onReceive = { ct, kp, xp -> viewModel.receiveKemExchange(ct, kp, xp) },
                        onGenerateSessionKey = viewModel::generateSessionKey,
                        onSetSessionKeyManual = viewModel::setSessionKeyManual,
                        onHideCopyWarning = { viewModel.setHideCopyWarning(true) },
                    )
                    VaultTab.MESSAGE -> MessageScreen(
                        uiState = uiState,
                        onEncrypt = viewModel::encryptAes,
                        onDecrypt = viewModel::decryptAes,
                        onHideCopyWarning = { viewModel.setHideCopyWarning(true) },
                    )
                }
            }
        }
    }
}