package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.CustomerRoute
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.OrdersHistoryScreen
import com.example.ui.screens.PointExchangeScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SportsOrderScreen
import com.example.ui.screens.ThreeDLotteryScreen
import com.example.ui.screens.WalletLedgerScreen
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.HtetThuYaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HtetThuYaTheme {
                AppNavigation()
            }
        }
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
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedRoot(user = user)
        }
    }
}

@Composable
private fun AuthenticatedRoot(user: FirebaseUser) {
    val context = LocalContext.current
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
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Member"
                )
            }
        }
    )

    LaunchedEffect(vm) {
        vm.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isAdmin by vm.isAdmin.collectAsStateWithLifecycle()
    val isAdminMode by vm.isAdminMode.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val accountState by vm.userAccount.collectAsStateWithLifecycle()
    val matchesState by vm.matches.collectAsStateWithLifecycle()
    val drawsState by vm.lotteryDraws.collectAsStateWithLifecycle()
    val userOrdersState by vm.userOrders.collectAsStateWithLifecycle()
    val userWalletRequestsState by vm.userWalletRequests.collectAsStateWithLifecycle()
    val userTransactionsState by vm.userTransactions.collectAsStateWithLifecycle()
    val leaderboardState by vm.leaderboard.collectAsStateWithLifecycle()
    val pendingAdminBadge by vm.pendingAdminBadgeCount.collectAsStateWithLifecycle()

    val allOrdersState by vm.allOrders.collectAsStateWithLifecycle()
    val allWalletRequestsState by vm.allWalletRequests.collectAsStateWithLifecycle()
    val allUsersState by vm.allUsers.collectAsStateWithLifecycle()
    val adminMembersState by vm.adminMembers.collectAsStateWithLifecycle()
    val adminInvitesState by vm.adminInvites.collectAsStateWithLifecycle()
    val walletConfig by vm.walletConfig.collectAsStateWithLifecycle()
    val boardConfig by vm.boardConfig.collectAsStateWithLifecycle()
    val bettingConfig by vm.bettingConfig.collectAsStateWithLifecycle()
    val liveFeedState by vm.liveFeed.collectAsStateWithLifecycle()

    val balanceKs = (accountState as? UiState.Success)?.data?.balanceKs ?: 0L
    val navController = rememberNavController()

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
                AdminDashboardScreen(
                    currentUserId = user.uid,
                    allOrdersState = allOrdersState,
                    allWalletRequestsState = allWalletRequestsState,
                    matchesState = matchesState,
                    drawsState = drawsState,
                    allUsersState = allUsersState,
                    adminMembersState = adminMembersState,
                    adminInvitesState = adminInvitesState,
                    walletConfig = walletConfig,
                    boardConfig = boardConfig,
                    bettingConfig = bettingConfig,
                    busy = busy,
                    onSwitchToCustomer = { vm.setAdminMode(false) },
                    onRefreshAll = vm::refreshAllData,
                    onAcceptOrder = vm::acceptOrder,
                    onSettleWin = vm::settleOrderWon,
                    onSettleLoss = vm::settleOrderLost,
                    onRejectOrder = vm::rejectOrder,
                    onDeleteOrder = vm::deleteOrder,
                    onClearCompletedOrders = vm::clearCompletedOrders,
                    onProcessWalletRequest = vm::processWalletRequest,
                    onDeleteWalletRequest = vm::deleteWalletRequest,
                    onClearProcessedWalletRequests = vm::clearProcessedWalletRequests,
                    onSaveWalletConfig = vm::saveWalletConfig,
                    onSaveBettingConfig = vm::saveBettingConfig,
                    onAutoSettleNow = vm::autoSettleFinishedOrdersNow,
                    onAutoSettleCustomer = vm::autoSettleCustomerOrdersNow,
                    onSaveNewMatch = { l, h, a, k, hand, goal, bOdds, gOdds ->
                        vm.saveNewMatch(l, h, a, k, hand, goal, bOdds, gOdds)
                    },
                    onUpdateMatchDetails = vm::updateMatchDetails,
                    onDeleteMatch = vm::deleteMatch,
                    onSaveNewDraw = { t, c, m -> vm.saveNewLotteryDraw(t, c, m) },
                    onUpdateLotteryDraw = vm::updateLotteryDraw,
                    onDeleteLotteryDraw = vm::deleteLotteryDraw,
                    onSeedSampleBoard = vm::seedSampleBoard,
                    onSyncDailyMatchesNow = vm::syncDailyMatchesNow,
                    onSyncLiveResultsNow = vm::syncLiveMatchResultsNow,
                    onToggleAutoDailyMatches = vm::toggleAutoSyncDailyMatches,
                    onToggleAutoLiveResults = vm::toggleAutoSyncLiveResults,
                    onCreateAdminInvite = { label -> vm.createAdminInvite(label) },
                    onToggleAdminInvite = vm::toggleAdminInvite,
                    onRemoveAdminMember = vm::removeAdminMember,
                    onAdjustUserBalance = vm::adjustUserBalance,
                    onGiftRewardPoints = vm::giftRewardPoints
                )
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
                            onNavigate = { route -> navController.navigate(route.name) },
                            onSwitchToAdmin = { vm.setAdminMode(true) },
                            onRedeemInviteCode = { code -> vm.redeemAdminInvite(code) },
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
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys) {
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
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitOrder = { type, sel, mIds, pKeys, round, stake, mult ->
                                vm.placeOrder(type, sel, mIds, round, stake, mult, pKeys) {
                                    navController.navigate(CustomerRoute.ACTIVE_ORDERS.name) {
                                        popUpTo(ROUTE_HOME)
                                    }
                                }
                            }
                        )
                    }
                    composable(CustomerRoute.THREE_D.name) {
                        ThreeDLotteryScreen(
                            drawsState = drawsState,
                            balanceKs = balanceKs,
                            busy = busy,
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
                    composable(CustomerRoute.ACTIVE_ORDERS.name) {
                        OrdersHistoryScreen(
                            activeOnly = true,
                            ordersState = userOrdersState,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(CustomerRoute.PAST_ORDERS.name) {
                        OrdersHistoryScreen(
                            activeOnly = false,
                            ordersState = userOrdersState,
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
                            busy = busy,
                            onBack = { navController.popBackStack() },
                            onSubmitWalletRequest = { type, amt, ch, ref ->
                                vm.submitWalletRequest(type, amt, ch, ref)
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
                }
            }
        }
    }
}
