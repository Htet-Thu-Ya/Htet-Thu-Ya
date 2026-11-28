package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.example.data.network.ApkDownloadHelper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.signOut
import com.example.data.model.UiState
import com.example.data.repository.HtetThuYaRepository
import com.example.ui.MainViewModel
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.formatKs
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ApkUpdatePromptDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatHubScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.CustomerLotteryHubWrapper
import com.example.ui.screens.CustomerRoute
import com.example.ui.screens.DiceGameScreen
import com.example.ui.screens.DragonTigerGameScreen
import com.example.ui.screens.ExpressOddsOrderScreen
import com.example.ui.screens.IncomingCallDialog
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LotteryHubTab
import com.example.ui.screens.LuckyWheelGameScreen
import com.example.ui.screens.OrdersHistoryScreen
import com.example.ui.screens.PointExchangeScreen
import com.example.ui.screens.QualityRatingScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SettingsAndRulesScreen
import com.example.ui.screens.SixAnimalsGameScreen
import com.example.ui.screens.SportsOrderScreen
import com.example.ui.screens.ThreeDLotteryScreen
import com.example.ui.screens.TwoDLotteryScreen
import com.example.ui.screens.WalletLedgerScreen
import com.example.ui.screens.rememberCallPermissionLauncher
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.HtetThuYaTheme
import com.example.ui.theme.InkDark
import com.example.ui.theme.PitchEmeraldDark
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var incomingAppLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingAppLinkUri = intent?.dataString
        enableEdgeToEdge()
        setContent {
            HtetThuYaTheme {
                AppNavigation(
                    incomingAppLinkUri = incomingAppLinkUri,
                    onConsumeIncomingAppLink = { incomingAppLinkUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { incomingAppLinkUri = it }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private const val ROUTE_HOME = "customer_home"

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    incomingAppLinkUri: String? = null,
    onConsumeIncomingAppLink: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedRoot(
                user = user,
                incomingAppLinkUri = incomingAppLinkUri,
                onConsumeIncomingAppLink = onConsumeIncomingAppLink
            )
        }
    }
}

@Composable
private fun AuthenticatedRoot(
    user: FirebaseUser,
    incomingAppLinkUri: String?,
    onConsumeIncomingAppLink: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val vm: MainViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = HtetThuYaRepository(db)
                MainViewModel(
                    repository = repo,
                    currentUserId = user.uid,
                    currentUserEmail = user.email,
                    isEmailVerified = user.isEmailVerified,
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Member",
                    appContext = app.applicationContext
                )
            }
        }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.updateOnlinePresence(true)
                Lifecycle.Event.ON_STOP -> vm.updateOnlinePresence(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.updateOnlinePresence(false)
        }
    }

    LaunchedEffect(vm) {
        vm.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isAdmin by vm.isAdmin.collectAsStateWithLifecycle()
    val isOwnerAdmin by vm.isOwnerAdmin.collectAsStateWithLifecycle()
    val adminMember by vm.adminMember.collectAsStateWithLifecycle()
    val currentAdminPermissions by vm.currentAdminPermissions.collectAsStateWithLifecycle()
    val isAdminMode by vm.isAdminMode.collectAsStateWithLifecycle()
    val adminPinVerified by vm.adminPinVerified.collectAsStateWithLifecycle()
    val currentPublicIp by vm.currentPublicIp.collectAsStateWithLifecycle()
    val isForceLoggedOut by vm.isForceLoggedOut.collectAsStateWithLifecycle()
    val appLanguage by vm.appLanguage.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    val accountState by vm.userAccount.collectAsStateWithLifecycle()
    val matchesState by vm.matches.collectAsStateWithLifecycle()
    val drawsState by vm.lotteryDraws.collectAsStateWithLifecycle()
    val live2DStatus by vm.live2DStatus.collectAsStateWithLifecycle()
    val userOrdersState by vm.userOrders.collectAsStateWithLifecycle()
    val userMiniGameRoundsState by vm.userMiniGameRounds.collectAsStateWithLifecycle()
    val userWalletRequestsState by vm.userWalletRequests.collectAsStateWithLifecycle()
    val userTransactionsState by vm.userTransactions.collectAsStateWithLifecycle()
    val leaderboardState by vm.leaderboard.collectAsStateWithLifecycle()
    val pendingAdminBadge by vm.pendingAdminBadgeCount.collectAsStateWithLifecycle()

    val allOrdersState by vm.allOrders.collectAsStateWithLifecycle()
    val allWalletRequestsState by vm.allWalletRequests.collectAsStateWithLifecycle()
    val allUsersState by vm.allUsers.collectAsStateWithLifecycle()
    val adminMembersState by vm.adminMembers.collectAsStateWithLifecycle()
    val adminInvitesState by vm.adminInvites.collectAsStateWithLifecycle()
    val auditLogsState by vm.auditLogs.collectAsStateWithLifecycle()
    val paymentAccounts by vm.paymentAccounts.collectAsStateWithLifecycle()
    val walletConfig by vm.walletConfig.collectAsStateWithLifecycle()
    val boardConfig by vm.boardConfig.collectAsStateWithLifecycle()
    val bettingConfig by vm.bettingConfig.collectAsStateWithLifecycle()
    val systemGameConfig by vm.systemGameConfig.collectAsStateWithLifecycle()
    val securityConfig by vm.securityConfig.collectAsStateWithLifecycle()
    val liveFeedState by vm.liveFeed.collectAsStateWithLifecycle()

    val brandingConfig by vm.brandingConfig.collectAsStateWithLifecycle()
    val miniGameControlConfig by vm.miniGameControlConfig.collectAsStateWithLifecycle()
    val effectiveLive2DStatus by vm.effectiveLive2DStatus.collectAsStateWithLifecycle()
    val lottery2D3DConfig by vm.lottery2D3DConfig.collectAsStateWithLifecycle()
    val lotteryCalendars by vm.lotteryCalendars.collectAsStateWithLifecycle()
    val api2DHistory by vm.api2DHistory.collectAsStateWithLifecycle()
    val supportAgents by vm.supportAgents.collectAsStateWithLifecycle()
    val allMiniGameRoundsState by vm.allMiniGameRounds.collectAsStateWithLifecycle()
    val allTransactionsState by vm.allTransactions.collectAsStateWithLifecycle()

    val appUpdateConfig by vm.appUpdateConfig.collectAsStateWithLifecycle()
    val availableApkUpdate by vm.availableApkUpdate.collectAsStateWithLifecycle()
    val updateDismissed by vm.updateDismissed.collectAsStateWithLifecycle()
    val userDirectMessagesState by vm.userDirectMessages.collectAsStateWithLifecycle()
    val allDirectMessagesState by vm.allDirectMessages.collectAsStateWithLifecycle()
    val groupMessagesState by vm.groupMessages.collectAsStateWithLifecycle()
    val appReviewsState by vm.appReviews.collectAsStateWithLifecycle()
    val activeCall by vm.activeCall.collectAsStateWithLifecycle()
    val incomingCall by vm.incomingCall.collectAsStateWithLifecycle()
    var manualOpenUpdateDialog by remember { mutableStateOf(false) }
    var showSplashOverlay by remember { mutableStateOf(true) }
    var startupRulesDismissed by remember { mutableStateOf(false) }
    var pendingInviteCode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(incomingAppLinkUri, isAdmin) {
        val rawUri = incomingAppLinkUri ?: return@LaunchedEffect
        when (val parsed = ApkDownloadHelper.parseIncomingAppLink(rawUri)) {
            is ApkDownloadHelper.ParsedAppLink.AdminPortal -> {
                if (isAdmin) {
                    vm.setAdminMode(true)
                    onConsumeIncomingAppLink()
                } else {
                    delay(900L)
                    snackbarHostState.showSnackbar("Admin Portal Link ဝင်ရောက်ရန် Admin Invite Code ထည့်သွင်းပါ")
                    onConsumeIncomingAppLink()
                }
            }
            is ApkDownloadHelper.ParsedAppLink.AdminInvite -> {
                if (isAdmin) {
                    vm.setAdminMode(true)
                    onConsumeIncomingAppLink()
                } else {
                    delay(900L)
                    pendingInviteCode = parsed.code
                    vm.setAdminMode(false)
                    onConsumeIncomingAppLink()
                }
            }
            is ApkDownloadHelper.ParsedAppLink.CustomerShare -> {
                vm.setAdminMode(false)
                onConsumeIncomingAppLink()
            }
            null -> onConsumeIncomingAppLink()
        }
    }

    LaunchedEffect(Unit) {
        delay(1400L)
        showSplashOverlay = false
    }

    LaunchedEffect(isForceLoggedOut) {
        if (isForceLoggedOut) {
            snackbarHostState.showSnackbar("Admin မှ သင့် Session ကို Force Logout ပြုလုပ်လိုက်ပါသည်")
            signOut(credentialManager, scope)
        }
    }

    val rootContext = LocalContext.current
    DisposableEffect(securityConfig.screenshotProtectionEnabled, rootContext) {
        val window = (rootContext as? Activity)?.window
        if (window != null) {
            if (securityConfig.screenshotProtectionEnabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val requestCallWithPermission = rememberCallPermissionLauncher { deniedMsg ->
        scope.launch {
            snackbarHostState.showSnackbar(deniedMsg)
        }
    }

    val balanceKs = (accountState as? UiState.Success)?.data?.balanceKs ?: 0L
    val currentUserAccount = (accountState as? UiState.Success)?.data
    val navController = rememberNavController()

    val ipAllowedForAdmin = remember(securityConfig, currentPublicIp, isOwnerAdmin) {
        !securityConfig.ipWhitelistEnabled ||
            securityConfig.allowedIps.isEmpty() ||
            isOwnerAdmin ||
            (currentPublicIp != null && securityConfig.allowedIps.any { it.trim() == currentPublicIp?.trim() })
    }
    val needsAdminPin = remember(securityConfig, adminMember, adminPinVerified, isOwnerAdmin) {
        !isOwnerAdmin &&
            securityConfig.requireAdminPin &&
            (adminMember?.pinHash?.isNotBlank() == true) &&
            !adminPinVerified
    }

    HtetThuYaTheme(themePreset = brandingConfig.themePreset) {
        Scaffold(
            containerColor = DeepPitchBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding() * 0f)
            ) {
            if (isAdmin && isAdminMode) {
                BackHandler {
                    vm.setAdminMode(false)
                }
                when {
                    !ipAllowedForAdmin -> {
                        AlertDialog(
                            onDismissRequest = { vm.setAdminMode(false) },
                            title = { Text("IP Whitelist ကန့်သတ်ချက်", fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    "လက်ရှိ IP (${currentPublicIp ?: "Checking..."}) သည် ခွင့်ပြုထားသော Admin IP စာရင်းတွင် မပါဝင်သေးပါ။ Main Admin မှ IP ထည့်သွင်းပေးမှသာ Admin Panel ဝင်နိုင်ပါမည်။"
                                )
                            },
                            confirmButton = {
                                Button(onClick = { vm.refreshPublicIp() }) {
                                    Text("IP ပြန်စစ်မည်")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { vm.setAdminMode(false) }) {
                                    Text("Customer Mode")
                                }
                            }
                        )
                    }
                    needsAdminPin -> {
                        var pinInput by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { vm.setAdminMode(false) },
                            title = { Text("Second Admin 2FA PIN ထည့်ပါ", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Admin Panel သို့ ဝင်ရောက်ရန် သတ်မှတ်ထားသော 2FA PIN ကို ရိုက်ထည့်ပါ။")
                                    OutlinedTextField(
                                        value = pinInput,
                                        onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(8) },
                                        label = { Text("Admin PIN (4-8 digits)") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_pin_prompt_input")
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { vm.verifyAdminPin(pinInput) },
                                    enabled = pinInput.length >= 4,
                                    modifier = Modifier.testTag("admin_pin_prompt_confirm")
                                ) {
                                    Text("အတည်ပြု ဝင်မည်")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { vm.setAdminMode(false) }) {
                                    Text("ပိတ်မည်")
                                }
                            }
                        )
                    }
                    else -> {
                        AdminDashboardScreen(
                            currentUserId = user.uid,
                            defaultDisplayName = vm.displayName,
                            isOwnerAdmin = isOwnerAdmin,
                            adminMember = adminMember,
                            permissions = currentAdminPermissions,
                            allOrdersState = allOrdersState,
                            allWalletRequestsState = allWalletRequestsState,
                            matchesState = matchesState,
                            drawsState = drawsState,
                            allUsersState = allUsersState,
                            adminMembersState = adminMembersState,
                            adminInvitesState = adminInvitesState,
                            allDirectMessagesState = allDirectMessagesState,
                            groupMessagesState = groupMessagesState,
                            appReviewsState = appReviewsState,
                            auditLogsState = auditLogsState,
                            allMiniGameRoundsState = allMiniGameRoundsState,
                            allTransactionsState = allTransactionsState,
                            paymentAccounts = paymentAccounts,
                            supportAgents = supportAgents,
                            appUpdateConfig = appUpdateConfig,
                            walletConfig = walletConfig,
                            boardConfig = boardConfig,
                            bettingConfig = bettingConfig,
                            systemGameConfig = systemGameConfig,
                            securityConfig = securityConfig,
                            brandingConfig = brandingConfig,
                            miniGameControlConfig = miniGameControlConfig,
                            live2DStatus = effectiveLive2DStatus,
                            lottery2D3DConfig = lottery2D3DConfig,
                            currentPublicIp = currentPublicIp,
                            busy = busy,
                            onSwitchToCustomer = { vm.setAdminMode(false) },
                            onRefreshAll = vm::refreshAllData,
                            onAcceptOrder = vm::acceptOrder,
                            onSettleWin = vm::settleOrderWon,
                            onSettleLoss = vm::settleOrderLost,
                            onRejectOrder = vm::rejectOrder,
                            onDeleteOrder = vm::deleteOrder,
                            onClearCompletedOrders = vm::clearCompletedOrders,
                            onClearFinishedMatches = vm::clearFinishedMatches,
                            onToggleHideOrder = { ord -> vm.setOrderHidden(ord.id, !ord.isHidden) },
                            onEditOrder = vm::editOrderDetails,
                            onProcessWalletRequest = vm::processWalletRequest,
                            onDeleteWalletRequest = vm::deleteWalletRequest,
                            onEditWalletRequest = vm::editWalletRequest,
                            onClearProcessedWalletRequests = vm::clearProcessedWalletRequests,
                            onEditTransaction = vm::editTransaction,
                            onDeleteTransaction = vm::deleteTransaction,
                            onClearAllTransactions = vm::clearAllTransactions,
                            onSaveWalletConfig = vm::saveWalletConfig,
                            onSaveBettingConfig = { pct, basis, autoSettle ->
                                vm.saveBettingConfig(pct, basis, autoSettle)
                            },
                            onToggleAutoAcceptOrders = vm::toggleAutoAcceptOrders,
                            onSaveSystemGameConfig = vm::saveSystemGameConfig,
                            onSaveSecurityConfig = vm::saveSecurityConfig,
                            onSaveBrandingConfig = vm::saveBrandingConfig,
                            onSaveMiniGameControlConfig = vm::saveMiniGameControlConfig,
                            onSetCustomerMiniGameDirective = vm::setCustomerMiniGameDirective,
                            onSaveSupportAgentProfile = vm::saveSupportAgentProfile,
                            onRefreshPublicIp = vm::refreshPublicIp,
                            onTriggerGlobalForceLogout = vm::triggerGlobalForceLogout,
                            onSavePaymentAccount = { id, p, n, num, note, act ->
                                vm.savePaymentAccount(id, p, n, num, note, act)
                            },
                            onDeletePaymentAccount = vm::deletePaymentAccount,
                            onAutoSettleNow = vm::autoSettleFinishedOrdersNow,
                            onAutoSettleCustomer = vm::autoSettleCustomerOrdersNow,
                            onAutoSettleOrder = vm::autoSettleSingleOrderNow,
                            onSaveNewMatch = { l, h, a, k, hand, goal, bOdds, gOdds, boardSrc, oneExpKyay, fffKyay, xBetOdds ->
                                vm.saveNewMatch(l, h, a, k, hand, goal, bOdds, gOdds, boardSrc, oneExpKyay, fffKyay, xBetOdds)
                            },
                            onUpdateMatchDetails = { id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds ->
                                vm.updateMatchDetails(id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds)
                            },
                            onDeleteMatch = vm::deleteMatch,
                            onSaveNewDraw = { t, c, m, drawType -> vm.saveNewLotteryDraw(t, c, m, drawType) },
                            onUpdateLotteryDraw = vm::updateLotteryDraw,
                            onDeleteLotteryDraw = vm::deleteLotteryDraw,
                            onSeedSampleBoard = vm::seedSampleBoard,
                            onSeedSampleExpressBoard = vm::seedSampleExpressBoard,
                            onSyncDailyMatchesNow = { d -> vm.syncDailyMatchesNow(d) },
                            onSyncLiveResultsNow = vm::syncLiveMatchResultsNow,
                            onToggleAutoDailyMatches = vm::toggleAutoSyncDailyMatches,
                            onToggleAutoLiveResults = vm::toggleAutoSyncLiveResults,
                            onSyncOneExpressNow = { d -> vm.syncOneExpressMatchesNow(d) },
                            onSync555Now = { d -> vm.sync555MatchesNow(d) },
                            onToggleAutoOneExpress = vm::toggleAutoSyncOneExpress,
                            onToggleAuto555 = vm::toggleAutoSync555,
                            onSaveAllowedLeagues = vm::saveAllowedLeagues,
                            onSaveBoardSheetUrl = vm::saveBoardSheetUrl,
                            onSaveBoardSheetSettings = vm::saveBoardSheetSettings,
                            onSyncMatchesFromSheet = vm::syncMatchesFromGoogleSheet,
                            onSyncMatchesFromSheetSilent = vm::syncMatchesFromGoogleSheet,
                            onPushToGoogleSheetWebhook = { vm.pushCurrentBoardToGoogleSheetWebhook(false) },
                            onSaveActiveMatchDateFilter = vm::saveActiveMatchDateFilter,
                            onAllocateAdminFloat = vm::allocateAdminFloat,
                            onTransferUserBalanceToAdminFloat = vm::transferUserBalanceToAdminFloat,
                            onSaveNewMatchWithDate = { l, h, a, k, hand, goal, bOdds, gOdds, boardSrc, oneExpKyay, fffKyay, xBetOdds, matchDate ->
                                vm.saveNewMatch(l, h, a, k, hand, goal, bOdds, gOdds, boardSrc, oneExpKyay, fffKyay, xBetOdds, matchDate)
                            },
                            onUpdateMatchDetailsWithDate = { id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds, matchDate ->
                                vm.updateMatchDetails(id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds, matchDate)
                            },
                            onImportCsvText = vm::importMatchesFromCsvText,
                            onUpdateMatchFromSheetRow = vm::updateMatchFromSheetRow,
                            onSync2D3DFromApiNow = vm::sync2D3DFromApiNow,
                            onSaveLottery2D3DConfig = vm::saveLottery2D3DConfig,
                            onTriggerManual2D3DNotification = vm::triggerManual2D3DNotification,
                            onSave2DCalendarDay = vm::save2DCalendarDay,
                            onSave3DCalendarMonth = vm::save3DCalendarMonth,
                            onCreateAdminInvite = { label, email, pin, perms ->
                                vm.createAdminInvite(label, email, pin, perms)
                            },
                            onToggleAdminInvite = vm::toggleAdminInvite,
                            onEditAdminInvite = vm::saveAdminInviteEdit,
                            onDeleteAdminInvite = vm::deleteAdminInvite,
                            onUpdateAdminPermissions = vm::updateAdminMemberPermissions,
                            onSetAdminStatus = vm::setAdminMemberStatus,
                            onResetAdminPin = vm::resetAdminMemberPin,
                            onForceLogoutAdmin = vm::triggerForceLogoutAdmin,
                            onRemoveAdminMember = vm::removeAdminMember,
                            onAdjustUserBalance = vm::adjustUserBalance,
                            onGiftRewardPoints = vm::giftRewardPoints,
                            onToggleLockUser = vm::setCustomerLocked,
                            onToggleHideUser = { acc ->
                                vm.setCustomerHidden(acc.userId, acc.displayName.ifBlank { "Member" }, !acc.isHidden)
                            },
                            onForceLogoutUser = vm::triggerForceLogoutUser,
                            onDeleteAuditLog = vm::deleteAuditLog,
                            onClearAllAuditLogs = vm::clearAllAuditLogs,
                            onSendAdminDirectMessage = vm::sendAdminDirectMessage,
                            onDeleteDirectMessage = vm::deleteDirectMessage,
                            onSendGroupMessage = { text -> vm.sendGroupMessage(text, asAdmin = true) },
                            onDeleteGroupMessage = vm::deleteGroupMessage,
                            onSaveAppUpdateConfig = vm::saveAppUpdateConfig,
                            onDeleteQualityReview = vm::deleteQualityReview,
                            onCallCustomer = { uid, name, callType ->
                                requestCallWithPermission(callType) {
                                    vm.startAdminCallToCustomer(uid, name, callType)
                                }
                            }
                        )
                    }
                }
            } else {
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME
                ) {
                    composable(ROUTE_HOME) {
                        CustomerHomeScreen(
                            accountState = accountState,
                            displayName = vm.displayName,
                            isAdmin = isAdmin,
                            pendingAdminCount = pendingAdminBadge,
                            systemGameConfig = systemGameConfig,
                            brandingConfig = brandingConfig,
                            appLanguage = appLanguage,
                            availableApkUpdate = availableApkUpdate,
                            apkDownloadUrl = availableApkUpdate?.downloadUrl
                                ?: appUpdateConfig?.downloadUrl
                                ?: com.example.data.model.AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL,
                            pendingInviteCode = pendingInviteCode,
                            onConsumePendingInviteCode = { pendingInviteCode = null },
                            onOpenUpdateDialog = { manualOpenUpdateDialog = true },
                            onNavigate = { route -> navController.navigate(route.name) },
                            onSwitchToAdmin = { vm.setAdminMode(true) },
                            onRedeemInviteCode = { code -> vm.redeemAdminInvite(code) },
                            onStartVoiceCall = {
                                requestCallWithPermission("voice") {
                                    vm.startCustomerCallToAdmin("voice")
                                }
                            },
                            onStartVideoCall = {
                                requestCallWithPermission("video") {
                                    vm.startCustomerCallToAdmin("video")
                                }
                            },
                            onRefresh = vm::refreshAllData,
                            onSignOut = {
                                signOut(credentialManager, scope)
                            }
                        )
                    }
                    composable(CustomerRoute.PARLAY.name) {
                        SportsOrderScreen(
                            isParlay = true,
                            matchesState = matchesState,
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            boardConfig = boardConfig,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult, kickoffMs, scanAtMs, kickoffText ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys, kickoffMs, scanAtMs, kickoffText) {
                                    navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                        popUpTo(ROUTE_HOME)
                                    }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.BODY_GOAL.name) {
                        SportsOrderScreen(
                            isParlay = false,
                            matchesState = matchesState,
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            boardConfig = boardConfig,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult, kickoffMs, scanAtMs, kickoffText ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys, kickoffMs, scanAtMs, kickoffText) {
                                    navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                        popUpTo(ROUTE_HOME)
                                    }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.ONE_EXPRESS.name) {
                        ExpressOddsOrderScreen(
                            boardMode = "one_express",
                            initialIsParlay = false,
                            matchesState = matchesState,
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            boardConfig = boardConfig,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult, kickoffMs, scanAtMs, kickoffText ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys, kickoffMs, scanAtMs, kickoffText) {
                                    navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                        popUpTo(ROUTE_HOME)
                                    }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.ONE_EXPRESS_PARLAY.name) {
                        ExpressOddsOrderScreen(
                            boardMode = "one_express",
                            initialIsParlay = true,
                            matchesState = matchesState,
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            boardConfig = boardConfig,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult, kickoffMs, scanAtMs, kickoffText ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys, kickoffMs, scanAtMs, kickoffText) {
                                    navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                        popUpTo(ROUTE_HOME)
                                    }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.FIVE_FIVE_FIVE.name) {
                        ExpressOddsOrderScreen(
                            boardMode = "five_five_five",
                            initialIsParlay = false,
                            matchesState = matchesState,
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            boardConfig = boardConfig,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult, kickoffMs, scanAtMs, kickoffText ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys, kickoffMs, scanAtMs, kickoffText) {
                                    navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                        popUpTo(ROUTE_HOME)
                                    }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.FIVE_FIVE_FIVE_PARLAY.name) {
                        ExpressOddsOrderScreen(
                            boardMode = "five_five_five",
                            initialIsParlay = true,
                            matchesState = matchesState,
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            boardConfig = boardConfig,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult, kickoffMs, scanAtMs, kickoffText ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys, kickoffMs, scanAtMs, kickoffText) {
                                    navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                        popUpTo(ROUTE_HOME)
                                    }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.LOTTERY_HUB.name) {
                        val drawsList = (drawsState as? UiState.Success)?.data.orEmpty()
                        CustomerLotteryHubWrapper(
                            initialTab = LotteryHubTab.LIVE,
                            initialBetType = if (!systemGameConfig.hideTwoD) "2d" else "3d",
                            live2DStatus = effectiveLive2DStatus,
                            lottery2D3DConfig = lottery2D3DConfig,
                            lotteryCalendars = lotteryCalendars,
                            api2DHistory = api2DHistory,
                            draws = drawsList,
                            hideTwoD = systemGameConfig.hideTwoD,
                            hideThreeD = systemGameConfig.hideThreeD,
                            onRefreshLive2D = vm::refreshLive2DStatus,
                            onOpenQualityRating = { navController.navigate(CustomerRoute.QUALITY.name) },
                            onBack = { navController.popBackStack() },
                            twoDBetContent = {
                                TwoDLotteryScreen(
                                    drawsState = drawsState,
                                    live2DStatus = effectiveLive2DStatus,
                                    balanceKs = balanceKs,
                                    systemGameConfig = systemGameConfig,
                                    busy = busy,
                                    embeddedInHub = true,
                                    onRefreshLive2D = vm::refreshLive2DStatus,
                                    onBack = { navController.popBackStack() },
                                    onSubmit2DOrder = { sel, round, stake, mult ->
                                        vm.placeOrder("two_d", sel, emptyList(), round, stake, mult) {
                                            navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                                popUpTo(ROUTE_HOME)
                                            }
                                        }
                                    }
                                )
                            },
                            threeDBetContent = {
                                ThreeDLotteryScreen(
                                    drawsState = drawsState,
                                    balanceKs = balanceKs,
                                    systemGameConfig = systemGameConfig,
                                    busy = busy,
                                    embeddedInHub = true,
                                    onBack = { navController.popBackStack() },
                                    onSubmit3DOrder = { sel, round, stake, mult ->
                                        vm.placeOrder("three_d", sel, emptyList(), round, stake, mult) {
                                            navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                                popUpTo(ROUTE_HOME)
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                    composable(CustomerRoute.TWO_D.name) {
                        val drawsList = (drawsState as? UiState.Success)?.data.orEmpty()
                        CustomerLotteryHubWrapper(
                            initialTab = LotteryHubTab.BET,
                            initialBetType = "2d",
                            live2DStatus = effectiveLive2DStatus,
                            lottery2D3DConfig = lottery2D3DConfig,
                            lotteryCalendars = lotteryCalendars,
                            api2DHistory = api2DHistory,
                            draws = drawsList,
                            hideTwoD = systemGameConfig.hideTwoD,
                            hideThreeD = systemGameConfig.hideThreeD,
                            onRefreshLive2D = vm::refreshLive2DStatus,
                            onOpenQualityRating = { navController.navigate(CustomerRoute.QUALITY.name) },
                            onBack = { navController.popBackStack() },
                            twoDBetContent = {
                                TwoDLotteryScreen(
                                    drawsState = drawsState,
                                    live2DStatus = effectiveLive2DStatus,
                                    balanceKs = balanceKs,
                                    systemGameConfig = systemGameConfig,
                                    busy = busy,
                                    embeddedInHub = true,
                                    onRefreshLive2D = vm::refreshLive2DStatus,
                                    onBack = { navController.popBackStack() },
                                    onSubmit2DOrder = { sel, round, stake, mult ->
                                        vm.placeOrder("two_d", sel, emptyList(), round, stake, mult) {
                                            navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                                popUpTo(ROUTE_HOME)
                                            }
                                        }
                                    }
                                )
                            },
                            threeDBetContent = {
                                ThreeDLotteryScreen(
                                    drawsState = drawsState,
                                    balanceKs = balanceKs,
                                    systemGameConfig = systemGameConfig,
                                    busy = busy,
                                    embeddedInHub = true,
                                    onBack = { navController.popBackStack() },
                                    onSubmit3DOrder = { sel, round, stake, mult ->
                                        vm.placeOrder("three_d", sel, emptyList(), round, stake, mult) {
                                            navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                                popUpTo(ROUTE_HOME)
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                    composable(CustomerRoute.THREE_D.name) {
                        val drawsList = (drawsState as? UiState.Success)?.data.orEmpty()
                        CustomerLotteryHubWrapper(
                            initialTab = LotteryHubTab.BET,
                            initialBetType = "3d",
                            live2DStatus = effectiveLive2DStatus,
                            lottery2D3DConfig = lottery2D3DConfig,
                            lotteryCalendars = lotteryCalendars,
                            api2DHistory = api2DHistory,
                            draws = drawsList,
                            hideTwoD = systemGameConfig.hideTwoD,
                            hideThreeD = systemGameConfig.hideThreeD,
                            onRefreshLive2D = vm::refreshLive2DStatus,
                            onOpenQualityRating = { navController.navigate(CustomerRoute.QUALITY.name) },
                            onBack = { navController.popBackStack() },
                            twoDBetContent = {
                                TwoDLotteryScreen(
                                    drawsState = drawsState,
                                    live2DStatus = effectiveLive2DStatus,
                                    balanceKs = balanceKs,
                                    systemGameConfig = systemGameConfig,
                                    busy = busy,
                                    embeddedInHub = true,
                                    onRefreshLive2D = vm::refreshLive2DStatus,
                                    onBack = { navController.popBackStack() },
                                    onSubmit2DOrder = { sel, round, stake, mult ->
                                        vm.placeOrder("two_d", sel, emptyList(), round, stake, mult) {
                                            navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                                popUpTo(ROUTE_HOME)
                                            }
                                        }
                                    }
                                )
                            },
                            threeDBetContent = {
                                ThreeDLotteryScreen(
                                    drawsState = drawsState,
                                    balanceKs = balanceKs,
                                    systemGameConfig = systemGameConfig,
                                    busy = busy,
                                    embeddedInHub = true,
                                    onBack = { navController.popBackStack() },
                                    onSubmit3DOrder = { sel, round, stake, mult ->
                                        vm.placeOrder("three_d", sel, emptyList(), round, stake, mult) {
                                            navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                                popUpTo(ROUTE_HOME)
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                    composable(CustomerRoute.DICE_GAME.name) {
                        DiceGameScreen(
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            miniGameControlConfig = miniGameControlConfig,
                            roundsState = userMiniGameRoundsState,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onPlayDice = { opt, exact, stake, onDone ->
                                vm.playDiceGame(opt, exact, stake) { res ->
                                    res.onSuccess { (dice, round) -> onDone(dice, round) }
                                        .onFailure { err ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(err.localizedMessage ?: "Dice game failed")
                                            }
                                        }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.SIX_ANIMALS.name) {
                        SixAnimalsGameScreen(
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            miniGameControlConfig = miniGameControlConfig,
                            roundsState = userMiniGameRoundsState,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onPlaySixAnimals = { idx, labels, stake, onDone ->
                                vm.playSixAnimalsGame(idx, labels, stake) { res ->
                                    res.onSuccess { (rolls, round) -> onDone(rolls, round) }
                                        .onFailure { err ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(err.localizedMessage ?: "6 Animals game failed")
                                            }
                                        }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.DRAGON_TIGER.name) {
                        DragonTigerGameScreen(
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            miniGameControlConfig = miniGameControlConfig,
                            roundsState = userMiniGameRoundsState,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onPlayDragonTiger = { pick, stake, onDone ->
                                vm.playDragonTigerGame(pick, stake) { res ->
                                    res.onSuccess { (roll, round) -> onDone(roll, round) }
                                        .onFailure { err ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(err.localizedMessage ?: "Dragon Tiger failed")
                                            }
                                        }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.LUCKY_WHEEL.name) {
                        LuckyWheelGameScreen(
                            balanceKs = balanceKs,
                            systemGameConfig = systemGameConfig,
                            miniGameControlConfig = miniGameControlConfig,
                            roundsState = userMiniGameRoundsState,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onPlayLuckyWheel = { stake, onDone ->
                                vm.playLuckyWheelGame(stake) { res ->
                                    res.onSuccess { (segIdx, round) -> onDone(segIdx, round) }
                                        .onFailure { err ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(err.localizedMessage ?: "Lucky Wheel failed")
                                            }
                                        }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.ACTIVE_ORDERS.name) {
                        OrdersHistoryScreen(
                            activeOnly = true,
                            ordersState = userOrdersState,
                            matchesState = matchesState,
                            onRefreshLiveScores = vm::syncLiveMatchResultsNow,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(CustomerRoute.PAST_ORDERS.name) {
                        OrdersHistoryScreen(
                            activeOnly = false,
                            ordersState = userOrdersState,
                            matchesState = matchesState,
                            onRefreshLiveScores = vm::syncLiveMatchResultsNow,
                            onBack = { navController.popBackStack() },
                            onDeleteOrder = vm::deleteOrder,
                            onClearCompletedOrders = vm::clearCompletedOrders
                        )
                    }
                    composable(CustomerRoute.LEDGER.name) {
                        WalletLedgerScreen(
                            accountState = accountState,
                            requestsState = userWalletRequestsState,
                            transactionsState = userTransactionsState,
                            walletConfig = walletConfig,
                            brandingConfig = brandingConfig,
                            supportAgents = supportAgents,
                            paymentAccounts = paymentAccounts,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitWalletRequest = { type, amt, ch, ref, receiptB64 ->
                                vm.submitWalletRequest(type, amt, ch, ref, receiptB64)
                            }
                        )
                    }
                    composable(CustomerRoute.RESULTS.name) {
                        ResultsScreen(
                            matchesState = matchesState,
                            drawsState = drawsState,
                            liveFeedState = liveFeedState,
                            onRefreshLiveFeed = vm::refreshLiveFootballFeed,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(CustomerRoute.LEADERBOARD.name) {
                        LeaderboardScreen(
                            leaderboardState = leaderboardState,
                            currentUserId = user.uid,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(CustomerRoute.POINT_EXCHANGE.name) {
                        PointExchangeScreen(
                            accountState = accountState,
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onExchange = { pts -> vm.exchangePoints(pts) }
                        )
                    }
                    composable(CustomerRoute.SETTINGS.name) {
                        SettingsAndRulesScreen(
                            account = currentUserAccount,
                            displayName = vm.displayName,
                            currentLanguage = appLanguage,
                            systemGameConfig = systemGameConfig,
                            brandingConfig = brandingConfig,
                            miniGameControlConfig = miniGameControlConfig,
                            apkDownloadUrl = availableApkUpdate?.downloadUrl
                                ?: appUpdateConfig?.downloadUrl
                                ?: com.example.data.model.AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL,
                            isAdmin = isAdmin,
                            onSelectLanguage = vm::setAppLanguage,
                            onUpdateSecurityPin = vm::updateCustomerSecurityPin,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(CustomerRoute.CHAT.name) {
                        ChatHubScreen(
                            currentUserId = user.uid,
                            isAdmin = isAdmin,
                            directMessagesState = userDirectMessagesState,
                            groupMessagesState = groupMessagesState,
                            brandingConfig = brandingConfig,
                            supportAgents = supportAgents,
                            onSendDirectMessage = vm::sendCustomerDirectMessage,
                            onSendGroupMessage = { text -> vm.sendGroupMessage(text, asAdmin = false) },
                            onDeleteDirectMessage = vm::deleteDirectMessage,
                            onDeleteGroupMessage = vm::deleteGroupMessage,
                            onStartVoiceCall = {
                                requestCallWithPermission("voice") {
                                    vm.startCustomerCallToAdmin("voice")
                                }
                            },
                            onStartVideoCall = {
                                requestCallWithPermission("video") {
                                    vm.startCustomerCallToAdmin("video")
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(CustomerRoute.QUALITY.name) {
                        QualityRatingScreen(
                            currentUserId = user.uid,
                            isAdmin = isAdmin,
                            reviewsState = appReviewsState,
                            busy = busy,
                            onSubmitReview = vm::submitQualityReview,
                            onDeleteReview = vm::deleteQualityReview,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            val updateToShow = availableApkUpdate
            if (!(isAdmin && isAdminMode) && updateToShow != null && (!updateDismissed || manualOpenUpdateDialog)) {
                ApkUpdatePromptDialog(
                    update = updateToShow,
                    onDismiss = {
                        manualOpenUpdateDialog = false
                        vm.dismissUpdateDialog()
                    }
                )
            }

            val currentCall = activeCall
            val incoming = incomingCall
            if (currentCall != null) {
                ActiveCallScreen(
                    callState = currentCall,
                    onToggleMute = vm::toggleCallMute,
                    onToggleSpeaker = vm::toggleCallSpeaker,
                    onToggleCamera = vm::toggleCallCamera,
                    onSwitchCamera = vm::switchCallCamera,
                    onHangup = vm::hangupActiveCall,
                    onAttachLocalRenderer = vm::attachLocalVideoRenderer,
                    onDetachLocalRenderer = vm::detachLocalVideoRenderer,
                    onAttachRemoteRenderer = vm::attachRemoteVideoRenderer,
                    onDetachRemoteRenderer = vm::detachRemoteVideoRenderer
                )
            } else if (incoming != null) {
                IncomingCallDialog(
                    session = incoming,
                    onAccept = {
                        requestCallWithPermission(incoming.callType) {
                            vm.answerIncomingCall(incoming)
                        }
                    },
                    onDecline = { vm.declineIncomingCall(incoming) }
                )
            }

            if (showSplashOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepPitchBg)
                        .clickable { showSplashOverlay = false }
                        .testTag("startup_splash_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        BrandLogoAvatar(brandingConfig = brandingConfig, size = 112.dp)
                        Text(
                            text = brandingConfig.appName.ifBlank { "လိုတရ" },
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "စိတ်ချယုံကြည်ရသော အွန်လိုင်းကစားဝိုင်း",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (
                !startupRulesDismissed &&
                brandingConfig.showRulesOnStartup &&
                !(isAdmin && isAdminMode) &&
                currentCall == null &&
                incoming == null
            ) {
                AlertDialog(
                    onDismissRequest = { startupRulesDismissed = true },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BrandLogoAvatar(brandingConfig = brandingConfig, size = 36.dp)
                            Text(
                                text = "${brandingConfig.appName.ifBlank { "လိုတရ" }} စည်းကမ်းချက်များ",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = brandingConfig.rulesText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (brandingConfig.maxContactMainAdminKs > 0L) {
                                HorizontalDivider()
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Main Admin ဆက်သွယ်ရန် ကန့်သတ်ပမာဏ: ${formatKs(brandingConfig.maxContactMainAdminKs)}",
                                            color = InkDark,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        if (brandingConfig.mainAdminContactNote.isNotBlank()) {
                                            Text(
                                                text = brandingConfig.mainAdminContactNote,
                                                color = InkDark,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { startupRulesDismissed = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            modifier = Modifier.testTag("startup_rules_accept_button")
                        ) {
                            Text("သဘောတူပြီး ဝင်ရောက်မည်", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
    }
}

