package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.betting.SettlementCalculator
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.AdminPermissions
import com.example.data.model.AppQualityReview
import com.example.data.model.AppUpdateConfig
import com.example.data.model.AuditLogEntry
import com.example.data.model.BetOrder
import com.example.data.model.BettingConfig
import com.example.data.model.BoardConfig
import com.example.data.model.BrandingConfig
import com.example.data.model.DirectChatMessage
import com.example.data.model.GroupChatMessage
import com.example.data.model.LedgerTransaction
import com.example.data.model.LotteryDraw
import com.example.data.model.MiniGameControlConfig
import com.example.data.model.MiniGameRound
import com.example.data.model.PaymentAccount
import com.example.data.model.SecurityConfig
import com.example.data.model.SportMatch
import com.example.data.model.SupportAgentProfile
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WalletConfig
import com.example.data.model.WalletRequest
import com.example.data.network.FootballFeedService
import com.example.data.util.MyanmarTimeUtil
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.FullScreenReceiptDialog
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.LeagueHideBarHeader
import com.example.ui.components.LoTaYaBrandLogo
import com.example.ui.components.OnlineStatusDot
import com.example.ui.components.OrderSlipCard
import com.example.ui.components.ReceiptImageThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.components.TeamLogoAvatar
import com.example.ui.components.formatKs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark

private enum class AdminTabKey {
    ORDERS,
    SETTLEMENT,
    WALLET,
    BOARD,
    ONE_EXPRESS,
    FIVE_FIVE_FIVE,
    MINI_GAMES,
    CUSTOMERS,
    FINANCE,
    SUB_ADMINS,
    MY_PROFILE,
    BRANDING,
    SYSTEM_SETTINGS,
    SECURITY,
    AUDIT_LOGS,
    CHAT,
    UPDATE_QUALITY
}

@Composable
fun AdminDashboardScreen(
    currentUserId: String,
    defaultDisplayName: String = "Admin",
    isOwnerAdmin: Boolean = true,
    permissions: AdminPermissions = AdminPermissions.ALL_GRANTED,
    allOrdersState: UiState<List<BetOrder>>,
    allWalletRequestsState: UiState<List<WalletRequest>>,
    matchesState: UiState<List<SportMatch>>,
    drawsState: UiState<List<LotteryDraw>>,
    allUsersState: UiState<List<UserAccount>>,
    adminMembersState: UiState<List<AdminMember>>,
    adminInvitesState: UiState<List<AdminInvite>>,
    allDirectMessagesState: UiState<List<DirectChatMessage>>,
    groupMessagesState: UiState<List<GroupChatMessage>>,
    appReviewsState: UiState<List<AppQualityReview>>,
    auditLogsState: UiState<List<AuditLogEntry>> = UiState.Success(emptyList()),
    allMiniGameRoundsState: UiState<List<MiniGameRound>> = UiState.Success(emptyList()),
    allTransactionsState: UiState<List<LedgerTransaction>> = UiState.Success(emptyList()),
    paymentAccounts: List<PaymentAccount> = emptyList(),
    supportAgents: List<SupportAgentProfile> = emptyList(),
    appUpdateConfig: AppUpdateConfig?,
    walletConfig: WalletConfig,
    boardConfig: BoardConfig,
    bettingConfig: BettingConfig,
    systemGameConfig: SystemGameConfig = SystemGameConfig(),
    securityConfig: SecurityConfig = SecurityConfig(),
    brandingConfig: BrandingConfig = BrandingConfig(),
    miniGameControlConfig: MiniGameControlConfig = MiniGameControlConfig(),
    currentPublicIp: String? = null,
    busy: Boolean,
    onSwitchToCustomer: () -> Unit,
    onRefreshAll: () -> Unit,
    onAcceptOrder: (String, String) -> Unit,
    onSettleWin: (BetOrder, String, Double?, Long?) -> Unit,
    onSettleLoss: (BetOrder, String) -> Unit,
    onRejectOrder: (BetOrder, String) -> Unit,
    onDeleteOrder: (String) -> Unit,
    onClearCompletedOrders: (List<String>) -> Unit,
    onToggleHideOrder: (BetOrder) -> Unit = {},
    onEditOrder: (String, List<String>, String, Long, Double, Long, String, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onProcessWalletRequest: (WalletRequest, Boolean, String) -> Unit,
    onDeleteWalletRequest: (String) -> Unit,
    onEditWalletRequest: (String, Long, Long, String, String, String, String, Boolean, String?) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onClearProcessedWalletRequests: (List<String>) -> Unit,
    onEditTransaction: (String, String, Long, String, Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteTransaction: (String) -> Unit = {},
    onSaveWalletConfig: (Double, String, Double, String) -> Unit,
    onSaveBettingConfig: (Double, String, Boolean) -> Unit,
    onToggleAutoAcceptOrders: (Boolean) -> Unit = {},
    onSaveSystemGameConfig: (SystemGameConfig) -> Unit = {},
    onSaveSecurityConfig: (SecurityConfig) -> Unit = {},
    onSaveBrandingConfig: (BrandingConfig) -> Unit = {},
    onSaveMiniGameControlConfig: (MiniGameControlConfig) -> Unit = {},
    onSetCustomerMiniGameDirective: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveSupportAgentProfile: (SupportAgentProfile) -> Unit = {},
    onRefreshPublicIp: () -> Unit = {},
    onTriggerGlobalForceLogout: () -> Unit = {},
    onSavePaymentAccount: (id: String?, providerName: String, accountName: String, accountNumber: String, note: String, active: Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeletePaymentAccount: (String) -> Unit = {},
    onAutoSettleNow: () -> Unit,
    onAutoSettleCustomer: (String, String) -> Unit,
    onAutoSettleOrder: (BetOrder) -> Unit,
    onSaveNewMatch: (String, String, String, String, String, String, Double, Double, String, String, String) -> Unit,
    onUpdateMatchDetails: (String, String, String, String, String, String, String, Double, Double, String, Long, Long, String?, String?) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onSaveNewDraw: (roundTitle: String, closeTimeLabel: String, payoutMultiplier: Double, drawType: String) -> Unit,
    onUpdateLotteryDraw: (String, String, String, Double, String, String) -> Unit,
    onDeleteLotteryDraw: (String) -> Unit,
    onSeedSampleBoard: () -> Unit,
    onSeedSampleExpressBoard: (String) -> Unit = {},
    onSyncDailyMatchesNow: () -> Unit,
    onSyncLiveResultsNow: () -> Unit,
    onToggleAutoDailyMatches: (Boolean) -> Unit,
    onToggleAutoLiveResults: (Boolean) -> Unit,
    onSyncOneExpressNow: () -> Unit = {},
    onSync555Now: () -> Unit = {},
    onToggleAutoOneExpress: (Boolean) -> Unit = {},
    onToggleAuto555: (Boolean) -> Unit = {},
    onSaveAllowedLeagues: (List<String>) -> Unit = {},
    onCreateAdminInvite: (label: String, assignedEmail: String, initialPin: String, permissions: AdminPermissions) -> Unit,
    onToggleAdminInvite: (String, Boolean) -> Unit,
    onEditAdminInvite: (String, String, String, Boolean, String, String?, AdminPermissions) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteAdminInvite: (String) -> Unit = {},
    onUpdateAdminPermissions: (String, String, AdminPermissions) -> Unit = { _, _, _ -> },
    onSetAdminStatus: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onResetAdminPin: (String, String, String) -> Unit = { _, _, _ -> },
    onForceLogoutAdmin: (String, String) -> Unit = { _, _ -> },
    onRemoveAdminMember: (String) -> Unit,
    onAdjustUserBalance: (String, Long, String) -> Unit,
    onGiftRewardPoints: (String, String, Long, String) -> Unit,
    onToggleLockUser: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onToggleHideUser: (UserAccount) -> Unit = {},
    onForceLogoutUser: (String, String) -> Unit = { _, _ -> },
    onDeleteAuditLog: (String) -> Unit = {},
    onClearAllAuditLogs: (List<String>) -> Unit = {},
    onSendAdminDirectMessage: (String, String) -> Unit,
    onDeleteDirectMessage: (String) -> Unit,
    onSendGroupMessage: (String) -> Unit,
    onDeleteGroupMessage: (String) -> Unit,
    onSaveAppUpdateConfig: (Int, String, String, String, Boolean) -> Unit,
    onDeleteQualityReview: (String) -> Unit,
    onCallCustomer: (customerUid: String, customerName: String, callType: String) -> Unit = { _, _, _ -> }
) {
    val visibleTabs = remember(isOwnerAdmin, permissions) {
        buildList {
            if (isOwnerAdmin || permissions.canManageOrders) {
                add(AdminTabKey.ORDERS)
                add(AdminTabKey.SETTLEMENT)
            }
            if (isOwnerAdmin || permissions.canApproveWallet || permissions.canEditWalletRates) {
                add(AdminTabKey.WALLET)
            }
            if (isOwnerAdmin || permissions.canManageBoard) {
                add(AdminTabKey.BOARD)
                add(AdminTabKey.ONE_EXPRESS)
                add(AdminTabKey.FIVE_FIVE_FIVE)
            }
            if (isOwnerAdmin) {
                add(AdminTabKey.MINI_GAMES)
            }
            if (isOwnerAdmin || permissions.canViewUsers) {
                add(AdminTabKey.CUSTOMERS)
            }
            if (isOwnerAdmin) {
                add(AdminTabKey.FINANCE)
                add(AdminTabKey.SUB_ADMINS)
            }
            add(AdminTabKey.MY_PROFILE)
            if (isOwnerAdmin) {
                add(AdminTabKey.BRANDING)
                add(AdminTabKey.SYSTEM_SETTINGS)
                add(AdminTabKey.SECURITY)
            }
            add(AdminTabKey.AUDIT_LOGS)
            if (isOwnerAdmin || permissions.canChat) {
                add(AdminTabKey.CHAT)
            }
            if (isOwnerAdmin || permissions.canEditGameConfig) {
                add(AdminTabKey.UPDATE_QUALITY)
            }
        }
    }

    var activeTabKey by remember { mutableStateOf(visibleTabs.firstOrNull() ?: AdminTabKey.AUDIT_LOGS) }
    LaunchedEffect(visibleTabs) {
        if (!visibleTabs.contains(activeTabKey)) {
            activeTabKey = visibleTabs.firstOrNull() ?: AdminTabKey.AUDIT_LOGS
        }
    }
    val selectedTabIndex = visibleTabs.indexOf(activeTabKey).coerceAtLeast(0)

    var chatCustomerUid by remember { mutableStateOf<String?>(null) }
    var inspectingUserId by remember { mutableStateOf<String?>(null) }
    var settlingWinOrder by remember { mutableStateOf<BetOrder?>(null) }
    var adjustingUser by remember { mutableStateOf<UserAccount?>(null) }
    var giftingUser by remember { mutableStateOf<UserAccount?>(null) }

    val orders = (allOrdersState as? UiState.Success)?.data ?: emptyList()
    val walletReqs = (allWalletRequestsState as? UiState.Success)?.data ?: emptyList()
    val matches = (matchesState as? UiState.Success)?.data ?: emptyList()
    val draws = (drawsState as? UiState.Success)?.data ?: emptyList()
    val users = (allUsersState as? UiState.Success)?.data ?: emptyList()
    val admins = (adminMembersState as? UiState.Success)?.data ?: emptyList()
    val invites = (adminInvitesState as? UiState.Success)?.data ?: emptyList()
    val directMsgs = (allDirectMessagesState as? UiState.Success)?.data ?: emptyList()
    val groupMsgs = (groupMessagesState as? UiState.Success)?.data ?: emptyList()
    val reviews = (appReviewsState as? UiState.Success)?.data ?: emptyList()
    val auditLogs = (auditLogsState as? UiState.Success)?.data ?: emptyList()
    val miniGameRounds = (allMiniGameRoundsState as? UiState.Success)?.data ?: emptyList()
    val allTransactions = (allTransactionsState as? UiState.Success)?.data ?: emptyList()
    val usersById = remember(users) { users.associateBy { it.userId } }

    val pendingOrderCount = orders.count { it.status == "pending" }
    val pendingWalletCount = walletReqs.count {
        it.status == "pending" || (isOwnerAdmin && it.status == "awaiting_main_approval")
    }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF3E0E12), Color(0xFF1F1612))
                        )
                    )
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        BrandLogoAvatar(brandingConfig = brandingConfig, size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "${brandingConfig.appName.ifBlank { "လိုတရ" }} Admin",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = if (isOwnerAdmin) ChampionshipGold else Color(0xFF0284C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isOwnerAdmin) "MAIN ADMIN" else "SECOND ADMIN",
                                        color = if (isOwnerAdmin) InkDark else Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                if (isOwnerAdmin) "စနစ်တစ်ခုလုံး စီမံခန့်ခွဲမှု Dashboard"
                                else "ခွင့်ပြုထားသော ကဏ္ဍများ (Scoped Dashboard)",
                                color = Color(0xFFE5D2D4),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshAll,
                            enabled = !busy,
                            modifier = Modifier.testTag("admin_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampionshipGold)
                        }
                        Surface(
                            color = PitchEmeraldDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable(onClick = onSwitchToCustomer)
                                .testTag("switch_to_customer_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Customer", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DeepPitchSurface,
                contentColor = ChampionshipGold,
                edgePadding = 12.dp
            ) {
                visibleTabs.forEach { tabKey ->
                    val (label, tag) = when (tabKey) {
                        AdminTabKey.ORDERS -> "အော်ဒါ ($pendingOrderCount)" to "admin_tab_orders"
                        AdminTabKey.SETTLEMENT -> "စာရင်းချုပ်" to "admin_tab_settlement"
                        AdminTabKey.WALLET -> "ငွေသွင်း/ထုတ် ($pendingWalletCount)" to "admin_tab_wallet"
                        AdminTabKey.BOARD -> "ပွဲစဉ် / ထီ" to "admin_tab_board"
                        AdminTabKey.ONE_EXPRESS -> "1xBET ကြေး" to "admin_tab_one_express"
                        AdminTabKey.FIVE_FIVE_FIVE -> "555 Mix ကြေး" to "admin_tab_five_five_five"
                        AdminTabKey.MINI_GAMES -> "ဂိမ်းခန်းထိန်းချုပ်" to "admin_tab_mini_games"
                        AdminTabKey.CUSTOMERS -> "Customer (${users.size})" to "admin_tab_customers"
                        AdminTabKey.FINANCE -> "ဘဏ္ဍာရေး/Bank" to "admin_tab_finance"
                        AdminTabKey.SUB_ADMINS -> "Second Admin" to "admin_tab_sub_admins"
                        AdminTabKey.MY_PROFILE -> "မိမိ Profile/Bank" to "admin_tab_my_profile"
                        AdminTabKey.BRANDING -> "Logo / App Brand" to "admin_tab_branding"
                        AdminTabKey.SYSTEM_SETTINGS -> "ဂိမ်း/စနစ်" to "admin_tab_system_settings"
                        AdminTabKey.SECURITY -> "လုံခြုံရေး" to "admin_tab_security"
                        AdminTabKey.AUDIT_LOGS -> "Audit Log (${auditLogs.size})" to "admin_tab_audit_logs"
                        AdminTabKey.CHAT -> "Chat (${directMsgs.size + groupMsgs.size})" to "admin_tab_chat"
                        AdminTabKey.UPDATE_QUALITY -> "Update / အဆင့် (${reviews.size})" to "admin_tab_update_quality"
                    }
                    Tab(
                        selected = activeTabKey == tabKey,
                        onClick = { activeTabKey = tabKey },
                        text = { Text(label) },
                        modifier = Modifier.testTag(tag)
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (activeTabKey) {
                    AdminTabKey.ORDERS -> AdminOrdersTab(
                        orders = orders,
                        matches = matches,
                        boardConfig = boardConfig,
                        usersById = usersById,
                        bettingConfig = bettingConfig,
                        isOwnerAdmin = isOwnerAdmin,
                        canEditGameConfig = isOwnerAdmin || permissions.canEditGameConfig,
                        canManageBoard = isOwnerAdmin || permissions.canManageBoard,
                        busy = busy,
                        onOpenOneExpressBoard = { activeTabKey = AdminTabKey.ONE_EXPRESS },
                        onOpen555Board = { activeTabKey = AdminTabKey.FIVE_FIVE_FIVE },
                        onSaveBettingConfig = onSaveBettingConfig,
                        onToggleAutoAcceptOrders = onToggleAutoAcceptOrders,
                        onAutoSettleNow = onAutoSettleNow,
                        onAutoSettleCustomer = onAutoSettleCustomer,
                        onAutoSettleOrder = onAutoSettleOrder,
                        onInspectCustomer = { uid -> inspectingUserId = uid },
                        onAccept = { onAcceptOrder(it.id, "Admin လက်ခံပြီး") },
                        onRequestSettleWin = { order -> settlingWinOrder = order },
                        onLose = { onSettleLoss(it, "စာရင်းပိတ်ပြီး") },
                        onReject = { onRejectOrder(it, "ပယ်ဖျက်ပြီး - ငွေပြန်အမ်းပြီး") },
                        onDeleteOrder = onDeleteOrder,
                        onClearCompletedOrders = onClearCompletedOrders,
                        onToggleHideOrder = onToggleHideOrder,
                        onEditOrder = onEditOrder
                    )
                    AdminTabKey.SETTLEMENT -> AdminSettlementTab(
                        orders = orders,
                        usersById = usersById,
                        busy = busy,
                        isOwnerAdmin = isOwnerAdmin,
                        onAutoSettleAll = onAutoSettleNow,
                        onAutoSettleCustomer = onAutoSettleCustomer,
                        onInspectCustomer = { uid -> inspectingUserId = uid },
                        onAutoSettleOrder = onAutoSettleOrder,
                        onAcceptOrder = { onAcceptOrder(it.id, "Admin လက်ခံပြီး") },
                        onRequestSettleWin = { order -> settlingWinOrder = order },
                        onSettleLoss = { onSettleLoss(it, "စာရင်းပိတ်ပြီး") },
                        onRejectOrder = { onRejectOrder(it, "ပယ်ဖျက်ပြီး - ငွေပြန်အမ်းပြီး") },
                        onDeleteOrder = onDeleteOrder,
                        onToggleHideOrder = onToggleHideOrder,
                        onEditOrder = onEditOrder
                    )
                    AdminTabKey.WALLET -> AdminWalletRequestsTab(
                        requests = walletReqs,
                        walletConfig = walletConfig,
                        systemGameConfig = systemGameConfig,
                        isOwnerAdmin = isOwnerAdmin,
                        canApproveWallet = isOwnerAdmin || permissions.canApproveWallet,
                        canEditWalletRates = isOwnerAdmin || permissions.canEditWalletRates,
                        busy = busy,
                        onProcess = onProcessWalletRequest,
                        onEditRequest = onEditWalletRequest,
                        onDeleteRequest = onDeleteWalletRequest,
                        onClearProcessed = onClearProcessedWalletRequests,
                        onSaveWalletConfig = onSaveWalletConfig
                    )
                    AdminTabKey.BOARD -> AdminBoardTab(
                        matches = matches,
                        draws = draws,
                        boardConfig = boardConfig,
                        busy = busy,
                        onSaveNewMatch = onSaveNewMatch,
                        onUpdateMatchDetails = onUpdateMatchDetails,
                        onDeleteMatch = onDeleteMatch,
                        onSaveNewDraw = onSaveNewDraw,
                        onUpdateLotteryDraw = onUpdateLotteryDraw,
                        onDeleteLotteryDraw = onDeleteLotteryDraw,
                        onSeedSampleBoard = onSeedSampleBoard,
                        onSyncDailyMatchesNow = onSyncDailyMatchesNow,
                        onSyncLiveResultsNow = onSyncLiveResultsNow,
                        onToggleAutoDailyMatches = onToggleAutoDailyMatches,
                        onToggleAutoLiveResults = onToggleAutoLiveResults,
                        onSaveAllowedLeagues = onSaveAllowedLeagues
                    )
                    AdminTabKey.ONE_EXPRESS -> AdminExpressBoardTab(
                        boardMode = "one_express",
                        matches = matches,
                        boardConfig = boardConfig,
                        busy = busy,
                        onSyncNow = onSyncOneExpressNow,
                        onSyncLiveResultsNow = onSyncLiveResultsNow,
                        onAutoSettleNow = onAutoSettleNow,
                        onToggleAutoSync = onToggleAutoOneExpress,
                        onToggleAutoLiveResults = onToggleAutoLiveResults,
                        onSaveAllowedLeagues = onSaveAllowedLeagues,
                        onSeedSampleBoard = { onSeedSampleExpressBoard("one_express") },
                        onSaveNewMatch = onSaveNewMatch,
                        onUpdateMatchDetails = onUpdateMatchDetails,
                        onDeleteMatch = onDeleteMatch
                    )
                    AdminTabKey.FIVE_FIVE_FIVE -> AdminExpressBoardTab(
                        boardMode = "five_five_five",
                        matches = matches,
                        boardConfig = boardConfig,
                        busy = busy,
                        onSyncNow = onSync555Now,
                        onSyncLiveResultsNow = onSyncLiveResultsNow,
                        onAutoSettleNow = onAutoSettleNow,
                        onToggleAutoSync = onToggleAuto555,
                        onToggleAutoLiveResults = onToggleAutoLiveResults,
                        onSaveAllowedLeagues = onSaveAllowedLeagues,
                        onSeedSampleBoard = { onSeedSampleExpressBoard("five_five_five") },
                        onSaveNewMatch = onSaveNewMatch,
                        onUpdateMatchDetails = onUpdateMatchDetails,
                        onDeleteMatch = onDeleteMatch
                    )
                    AdminTabKey.MINI_GAMES -> AdminMiniGameControlTab(
                        config = miniGameControlConfig,
                        rounds = miniGameRounds,
                        users = users,
                        busy = busy,
                        onSaveConfig = onSaveMiniGameControlConfig,
                        onSetCustomerDirective = onSetCustomerMiniGameDirective
                    )
                    AdminTabKey.CUSTOMERS -> AdminCustomersTab(
                        users = users,
                        orders = orders,
                        busy = busy,
                        canAdjustBalance = isOwnerAdmin || permissions.canAdjustBalance,
                        canLockUsers = isOwnerAdmin || permissions.canLockUsers,
                        canChat = isOwnerAdmin || permissions.canChat,
                        canManageOrders = isOwnerAdmin || permissions.canManageOrders,
                        onAutoSettleCustomer = onAutoSettleCustomer,
                        onInspectCustomer = { u -> inspectingUserId = u.userId },
                        onOpenAdjustBalance = { u -> adjustingUser = u },
                        onOpenGiftPoints = { u -> giftingUser = u },
                        onToggleLockUser = { u ->
                            onToggleLockUser(u.userId, u.displayName.ifBlank { "Member" }, !u.isLocked)
                        },
                        onToggleHideUser = onToggleHideUser,
                        onForceLogoutUser = { u ->
                            onForceLogoutUser(u.userId, u.displayName.ifBlank { "Member" })
                        },
                        onOpenChatWithCustomer = { u ->
                            chatCustomerUid = u.userId
                            if (visibleTabs.contains(AdminTabKey.CHAT)) {
                                activeTabKey = AdminTabKey.CHAT
                            }
                        },
                        onCallCustomer = onCallCustomer
                    )
                    AdminTabKey.FINANCE -> AdminFinancialOverviewTab(
                        users = users,
                        orders = orders,
                        walletRequests = walletReqs,
                        transactions = allTransactions,
                        paymentAccounts = paymentAccounts,
                        isOwnerAdmin = isOwnerAdmin,
                        busy = busy,
                        onSavePaymentAccount = onSavePaymentAccount,
                        onDeletePaymentAccount = onDeletePaymentAccount,
                        onEditWalletRequest = onEditWalletRequest,
                        onDeleteWalletRequest = onDeleteWalletRequest,
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = onDeleteTransaction
                    )
                    AdminTabKey.SUB_ADMINS -> AdminSubAdminsTab(
                        currentUserId = currentUserId,
                        admins = admins,
                        invites = invites,
                        supportAgents = supportAgents,
                        appDisplayName = brandingConfig.appName.ifBlank { "လိုတရ" },
                        apkDownloadUrl = appUpdateConfig?.downloadUrl?.ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL }
                            ?: AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL,
                        onCreateInvite = onCreateAdminInvite,
                        onToggleInvite = onToggleAdminInvite,
                        onEditInvite = onEditAdminInvite,
                        onDeleteInvite = onDeleteAdminInvite,
                        onUpdateAdminPermissions = onUpdateAdminPermissions,
                        onSetAdminStatus = onSetAdminStatus,
                        onResetAdminPin = onResetAdminPin,
                        onForceLogoutAdmin = onForceLogoutAdmin,
                        onRemoveAdmin = onRemoveAdminMember
                    )
                    AdminTabKey.MY_PROFILE -> AdminAgentSelfProfileTab(
                        currentUserId = currentUserId,
                        isOwnerAdmin = isOwnerAdmin,
                        defaultDisplayName = defaultDisplayName,
                        supportAgents = supportAgents,
                        paymentAccounts = paymentAccounts,
                        busy = busy,
                        onSaveSupportAgentProfile = onSaveSupportAgentProfile,
                        onSavePaymentAccount = onSavePaymentAccount,
                        onDeletePaymentAccount = onDeletePaymentAccount
                    )
                    AdminTabKey.BRANDING -> AdminBrandingTab(
                        brandingConfig = brandingConfig,
                        busy = busy,
                        onSaveBrandingConfig = onSaveBrandingConfig
                    )
                    AdminTabKey.SYSTEM_SETTINGS -> AdminSystemSettingsTab(
                        config = systemGameConfig,
                        busy = busy,
                        onSaveSystemGameConfig = onSaveSystemGameConfig
                    )
                    AdminTabKey.SECURITY -> AdminSecurityControlsTab(
                        securityConfig = securityConfig,
                        currentPublicIp = currentPublicIp,
                        busy = busy,
                        onRefreshIp = onRefreshPublicIp,
                        onSaveSecurityConfig = onSaveSecurityConfig,
                        onTriggerGlobalForceLogout = onTriggerGlobalForceLogout
                    )
                    AdminTabKey.AUDIT_LOGS -> AdminAuditLogsTab(
                        logs = auditLogs,
                        isOwnerOrCanViewAll = isOwnerAdmin || permissions.canViewAllLogs,
                        isOwnerAdmin = isOwnerAdmin,
                        onDeleteAuditLog = onDeleteAuditLog,
                        onClearAllAuditLogs = onClearAllAuditLogs
                    )
                    AdminTabKey.CHAT -> AdminChatTab(
                        currentUserId = currentUserId,
                        users = users,
                        allDirectMessages = directMsgs,
                        groupMessages = groupMsgs,
                        initialCustomerUid = chatCustomerUid,
                        onClearInitialCustomer = { chatCustomerUid = null },
                        onSendAdminDirectMessage = onSendAdminDirectMessage,
                        onDeleteDirectMessage = onDeleteDirectMessage,
                        onSendGroupMessage = onSendGroupMessage,
                        onDeleteGroupMessage = onDeleteGroupMessage,
                        onCallCustomer = onCallCustomer
                    )
                    AdminTabKey.UPDATE_QUALITY -> AdminUpdateAndQualityTab(
                        currentUserId = currentUserId,
                        appUpdateConfig = appUpdateConfig,
                        reviews = reviews,
                        busy = busy,
                        onSaveAppUpdateConfig = onSaveAppUpdateConfig,
                        onDeleteReview = onDeleteQualityReview
                    )
                }
            }
        }
    }

    inspectingUserId?.let { uid ->
        val inspectedUser = usersById[uid] ?: UserAccount(
            userId = uid,
            displayName = orders.firstOrNull { it.userId == uid }?.customerName ?: "Member"
        )
        val custOrders = remember(orders, uid) { orders.filter { it.userId == uid } }
        val custReqs = remember(walletReqs, uid) { walletReqs.filter { it.userId == uid } }
        AdminCustomerDetailDialog(
            user = inspectedUser,
            customerOrders = custOrders,
            customerWalletReqs = custReqs,
            busy = busy,
            canAdjustBalance = isOwnerAdmin || permissions.canAdjustBalance,
            canLockUsers = isOwnerAdmin || permissions.canLockUsers,
            isOwnerAdmin = isOwnerAdmin,
            onDismiss = { inspectingUserId = null },
            onRefresh = onRefreshAll,
            onAutoSettleCustomer = {
                onAutoSettleCustomer(uid, inspectedUser.displayName.ifBlank { "Member" })
            },
            onAutoSettleOrder = onAutoSettleOrder,
            onOpenAdjustBalance = { adjustingUser = inspectedUser },
            onOpenGiftPoints = { giftingUser = inspectedUser },
            onToggleLockUser = {
                onToggleLockUser(uid, inspectedUser.displayName.ifBlank { "Member" }, !inspectedUser.isLocked)
            },
            onForceLogoutUser = {
                onForceLogoutUser(uid, inspectedUser.displayName.ifBlank { "Member" })
            },
            onAcceptOrder = { onAcceptOrder(it.id, "Admin လက်ခံပြီး") },
            onRequestSettleWin = { order -> settlingWinOrder = order },
            onSettleLoss = { onSettleLoss(it, "စာရင်းပိတ်ပြီး") },
            onRejectOrder = { onRejectOrder(it, "ပယ်ဖျက်ပြီး - ငွေပြန်အမ်းပြီး") },
            onDeleteOrder = onDeleteOrder,
            onProcessWalletRequest = onProcessWalletRequest,
            onDeleteWalletRequest = onDeleteWalletRequest,
            onOpenChat = {
                inspectingUserId = null
                chatCustomerUid = uid
                if (visibleTabs.contains(AdminTabKey.CHAT)) {
                    activeTabKey = AdminTabKey.CHAT
                }
            },
            onStartVoiceCall = {
                inspectingUserId = null
                onCallCustomer(uid, inspectedUser.displayName.ifBlank { "Member" }, "voice")
            },
            onStartVideoCall = {
                inspectingUserId = null
                onCallCustomer(uid, inspectedUser.displayName.ifBlank { "Member" }, "video")
            }
        )
    }

    settlingWinOrder?.let { target ->
        SettleWinDialog(
            order = target,
            bettingConfig = bettingConfig,
            onDismiss = { settlingWinOrder = null },
            onConfirm = { customPct, grossKs, note ->
                settlingWinOrder = null
                onSettleWin(target, note, customPct, grossKs)
            }
        )
    }

    adjustingUser?.let { target ->
        AdjustUserBalanceDialog(
            user = target,
            onDismiss = { adjustingUser = null },
            onAdjust = { delta, note ->
                adjustingUser = null
                onAdjustUserBalance(target.userId, delta, note)
            }
        )
    }

    giftingUser?.let { target ->
        GiftRewardPointsDialog(
            user = target,
            onDismiss = { giftingUser = null },
            onGift = { deltaPts, note ->
                giftingUser = null
                onGiftRewardPoints(target.userId, target.displayName.ifBlank { "Member" }, deltaPts, note)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminOrdersTab(
    orders: List<BetOrder>,
    matches: List<SportMatch>,
    boardConfig: BoardConfig,
    usersById: Map<String, UserAccount>,
    bettingConfig: BettingConfig,
    isOwnerAdmin: Boolean,
    canEditGameConfig: Boolean,
    canManageBoard: Boolean,
    busy: Boolean,
    onOpenOneExpressBoard: () -> Unit,
    onOpen555Board: () -> Unit,
    onSaveBettingConfig: (Double, String, Boolean) -> Unit,
    onToggleAutoAcceptOrders: (Boolean) -> Unit,
    onAutoSettleNow: () -> Unit,
    onAutoSettleCustomer: (String, String) -> Unit,
    onAutoSettleOrder: (BetOrder) -> Unit,
    onInspectCustomer: (String) -> Unit,
    onAccept: (BetOrder) -> Unit,
    onRequestSettleWin: (BetOrder) -> Unit,
    onLose: (BetOrder) -> Unit,
    onReject: (BetOrder) -> Unit,
    onDeleteOrder: (String) -> Unit,
    onClearCompletedOrders: (List<String>) -> Unit,
    onToggleHideOrder: (BetOrder) -> Unit,
    onEditOrder: (String, List<String>, String, Long, Double, Long, String, String) -> Unit
) {
    var filter by remember { mutableStateOf("pending") }
    var groupByCustomer by remember { mutableStateOf(true) }
    var showHiddenOrders by remember { mutableStateOf(false) }
    var showCommissionSettings by remember { mutableStateOf(false) }
    var showOrdersList by remember { mutableStateOf(true) }
    var editingOrder by remember { mutableStateOf<BetOrder?>(null) }
    var deletingOrder by remember { mutableStateOf<BetOrder?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    val collapsedCustomers = remember { mutableStateMapOf<String, Boolean>() }

    var commissionPctText by remember { mutableStateOf(bettingConfig.winCommissionPercent.toString()) }
    var commissionBasis by remember { mutableStateOf(bettingConfig.winCommissionBasis) }
    var autoSettleEnabled by remember { mutableStateOf(bettingConfig.autoSettleOrders) }

    LaunchedEffect(bettingConfig) {
        commissionPctText = if (bettingConfig.winCommissionPercent % 1.0 == 0.0) {
            bettingConfig.winCommissionPercent.toInt().toString()
        } else {
            bettingConfig.winCommissionPercent.toString()
        }
        commissionBasis = bettingConfig.winCommissionBasis
        autoSettleEnabled = bettingConfig.autoSettleOrders
    }

    val hiddenCount = remember(orders) { orders.count { it.isHidden } }
    val baseOrders = remember(orders, showHiddenOrders) {
        if (showHiddenOrders) orders else orders.filter { !it.isHidden }
    }

    val settledIds = remember(baseOrders) {
        baseOrders.filter { it.status in listOf("won", "lost", "rejected") }.map { it.id }
    }

    val filtered = remember(baseOrders, filter) {
        when (filter) {
            "pending" -> baseOrders.filter { it.status == "pending" }
            "accepted" -> baseOrders.filter { it.status == "accepted" }
            "settled" -> baseOrders.filter { it.status in listOf("won", "lost", "rejected") }
            else -> baseOrders
        }
    }

    val groupedByUser = remember(filtered) {
        filtered.groupBy { it.userId }.entries.toList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canManageBoard) {
            item {
                val oneExpressOpenCount = remember(matches) {
                    matches.count { it.boardSource == "one_express" && it.status == "open" }
                }
                val fffOpenCount = remember(matches) {
                    matches.count { it.boardSource == "five_five_five" && it.status == "open" }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        onClick = onOpenOneExpressBoard,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2A3B)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("admin_box_one_express")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "1xBET ကြေး",
                                    color = Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "$oneExpressOpenCount ပွဲဖွင့် • Auto ${if (boardConfig.autoSyncOneExpress) "ON" else "OFF"}",
                                color = Color(0xFFE0F2FE),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "နှိပ်၍ ပြုပြင်/Sync မည် ->",
                                color = Color(0xFF7DD3FC),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Card(
                        onClick = onOpen555Board,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B210B)),
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("admin_box_five_five_five")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "555 Mix ဘောလုံးကြေး",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "$fffOpenCount ပွဲဖွင့် • Auto ${if (boardConfig.autoSyncFiveFiveFive) "ON" else "OFF"}",
                                color = Color(0xFFFEF3C7),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "နှိပ်၍ ပြုပြင်/Sync မည် ->",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        if (canEditGameConfig) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCommissionSettings = !showCommissionSettings },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Percent, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "အနိုင်အကောက် (%) & အလိုအလျောက်စနစ်",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val basisLbl = if (commissionBasis == "profit") "အမြတ်ငွေပေါ်" else "စုစုပေါင်းအလျော်ပေါ်"
                                Text(
                                    "လက်ရှိ: ${commissionPctText.ifBlank { "0" }}% ($basisLbl) • Auto: ${if (autoSettleEnabled) "ON" else "OFF"}",
                                    color = Color(0xFFB4C8BE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showCommissionSettings) "ဝှက်မည်" else "ပြမည်",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = if (showCommissionSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle commission settings",
                                tint = ChampionshipGold
                            )
                        }
                    }

                    if (showCommissionSettings) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = commissionPctText,
                                onValueChange = { commissionPctText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                                label = { Text("အကောက် %") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.width(110.dp).testTag("win_commission_percent_input")
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0.0 to "0%", 5.0 to "5%", 10.0 to "10%").forEach { (preset, lbl) ->
                                    FilterChip(
                                        selected = (commissionPctText.toDoubleOrNull() ?: 0.0) == preset,
                                        onClick = { commissionPctText = preset.toInt().toString() },
                                        label = { Text(lbl) }
                                    )
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = commissionBasis == "payout",
                                onClick = { commissionBasis = "payout" },
                                label = { Text("စုစုပေါင်းအလျော်ငွေမှ နုတ်") }
                            )
                            FilterChip(
                                selected = commissionBasis == "profit",
                                onClick = { commissionBasis = "profit" },
                                label = { Text("အမြတ်ငွေပေါ်မှ နုတ်") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ပွဲပြီး / ထီထွက်လျှင် အလိုအလျောက် အလျော်အစားလုပ်မည်",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Switch(
                                checked = autoSettleEnabled,
                                onCheckedChange = { autoSettleEnabled = it },
                                modifier = Modifier.testTag("auto_settle_orders_switch")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    val pct = (commissionPctText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 50.0)
                                    onSaveBettingConfig(pct, commissionBasis, autoSettleEnabled)
                                },
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                                modifier = Modifier.weight(1f).testTag("save_betting_config_button")
                            ) {
                                Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onAutoSettleNow,
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                                modifier = Modifier.weight(1f).testTag("auto_settle_now_button")
                            ) {
                                Text("Auto ရှင်းမည်")
                            }
                        }
                    }
                }
            }
        }
        } else {
            item {
                Button(
                    onClick = onAutoSettleNow,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("auto_settle_now_button")
                ) {
                    Text("ပွဲပြီး / ထီထွက် အော်ဒါများ Auto ရှင်းမည်")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, PitchEmeraldDark.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            "အော်ဒါ အလိုအလျောက် လက်ခံစနစ် (Auto-Accept)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (bettingConfig.autoAcceptOrders) "Customer တင်လိုက်သည်နှင့် ချက်ချင်း အတည်ပြုပါမည်"
                            else "Admin မှ ကိုယ်တိုင်စစ်ဆေး လက်ခံမည့်စနစ်",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = bettingConfig.autoAcceptOrders,
                        onCheckedChange = onToggleAutoAcceptOrders,
                        enabled = !busy,
                        modifier = Modifier.testTag("auto_accept_orders_switch")
                    )
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "pending" to "အသစ် (${baseOrders.count { it.status == "pending" }})",
                    "accepted" to "လက်ခံပြီး (${baseOrders.count { it.status == "accepted" }})",
                    "settled" to "ပြီးဆုံး (${settledIds.size})",
                    "all" to "အားလုံး (${baseOrders.size})"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("admin_orders_filter_$key")
                    )
                }
                if (hiddenCount > 0 || showHiddenOrders) {
                    FilterChip(
                        selected = showHiddenOrders,
                        onClick = { showHiddenOrders = !showHiddenOrders },
                        label = { Text(if (showHiddenOrders) "Hide ထားသည်များပါပြ ($hiddenCount)" else "Hide ($hiddenCount)") },
                        modifier = Modifier.testTag("admin_orders_toggle_hidden")
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = groupByCustomer,
                    onClick = { groupByCustomer = true },
                    label = { Text("Customer အလိုက်ခွဲကြည့်မည်") },
                    modifier = Modifier.testTag("orders_view_by_customer")
                )
                FilterChip(
                    selected = !groupByCustomer,
                    onClick = { groupByCustomer = false },
                    label = { Text("အော်ဒါစာရင်းလိုက်") },
                    modifier = Modifier.testTag("orders_view_flat")
                )
            }
        }

        if (settledIds.isNotEmpty() && (filter == "settled" || filter == "all")) {
            item {
                OutlinedButton(
                    onClick = { showClearAllConfirm = true },
                    border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().testTag("admin_clear_settled_orders_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ပြီးဆုံးပြီးသား အော်ဒါအားလုံး ဖျက်မည် (${settledIds.size})", color = AdminCrimson)
                }
            }
        }

        item {
            HideBarSectionHeader(
                title = "အော်ဒါစာရင်းဇယား",
                countBadge = "${filtered.size} ခု",
                isVisible = showOrdersList,
                onToggle = { showOrdersList = !showOrdersList },
                testTag = "hide_bar_admin_orders_list"
            )
        }

        if (showOrdersList) {
        if (filtered.isEmpty()) {
            item {
                Text(
                    "ဤကဏ္ဍတွင် Customer အော်ဒါ မရှိသေးပါ",
                    color = Color(0xFFB4C8BE),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else if (groupByCustomer) {
            groupedByUser.forEach { (uid, customerOrders) ->
                val userAccount = usersById[uid]
                val custName = userAccount?.displayName?.ifBlank { null }
                    ?: customerOrders.firstOrNull()?.customerName?.ifBlank { null }
                    ?: "Member"
                val custEmail = userAccount?.email.orEmpty()
                val totalStake = customerOrders.sumOf { it.stakeKs }
                val hasActiveForUser = customerOrders.any { it.status in listOf("pending", "accepted") }

                val isCollapsed = collapsedCustomers[uid] == true
                item(key = "header_$uid") {
                    val allOrdersForUser = remember(orders, uid) { orders.filter { it.userId == uid } }
                    val custSummary = remember(uid, userAccount, allOrdersForUser) {
                        SettlementCalculator.summarizeCustomer(uid, userAccount, allOrdersForUser)
                    }
                    Surface(
                        color = DeepPitchElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { collapsedCustomers[uid] = !isCollapsed }
                            .testTag("customer_order_group_$uid")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = ChampionshipGold)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = custName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (custEmail.isNotBlank()) {
                                            Text(
                                                text = custEmail,
                                                color = ChampionshipGold,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${customerOrders.size} ခု • ထိုးကြေး ${formatKs(totalStake)} • နိုင် ${custSummary.wonCount} / ရှုံး ${custSummary.lostCount}",
                                            color = Color(0xFFC5D7CE),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { onInspectCustomer(uid) }) {
                                        Text("အသေးစိတ်", color = ChampionshipGold)
                                    }
                                    IconButton(
                                        onClick = { collapsedCustomers[uid] = !isCollapsed },
                                        modifier = Modifier.testTag("toggle_collapse_customer_orders_$uid")
                                    ) {
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                            contentDescription = if (isCollapsed) "Expand" else "Hide",
                                            tint = ChampionshipGold
                                        )
                                    }
                                }
                            }
                            if (hasActiveForUser && !isCollapsed) {
                                Button(
                                    onClick = { onAutoSettleCustomer(uid, custName) },
                                    enabled = !busy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PitchEmeraldDark,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("group_auto_settle_$uid")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("$custName အော်ဒါများ Auto ရှင်းမည်")
                                }
                            }
                        }
                    }
                }

                if (!isCollapsed) {
                    items(customerOrders, key = { it.id }) { order ->
                        OrderSlipCard(
                            order = order,
                            isAdminView = true,
                            busy = busy,
                            canDeleteAnyOrder = isOwnerAdmin,
                            onAccept = onAccept,
                            onAutoSettleOrder = onAutoSettleOrder,
                            onSettleWin = onRequestSettleWin,
                            onSettleLoss = onLose,
                            onReject = onReject,
                            onDeleteOrder = { target -> deletingOrder = target },
                            onToggleHideOrder = onToggleHideOrder,
                            onEditOrder = { target -> editingOrder = target }
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { order ->
                OrderSlipCard(
                    order = order,
                    isAdminView = true,
                    busy = busy,
                    canDeleteAnyOrder = isOwnerAdmin,
                    onAccept = onAccept,
                    onAutoSettleOrder = onAutoSettleOrder,
                    onSettleWin = onRequestSettleWin,
                    onSettleLoss = onLose,
                    onReject = onReject,
                    onDeleteOrder = { target -> deletingOrder = target },
                    onToggleHideOrder = onToggleHideOrder,
                    onEditOrder = { target -> editingOrder = target }
                )
            }
        }
        }
    }

    editingOrder?.let { target ->
        EditOrderDialog(
            order = target,
            onDismiss = { editingOrder = null },
            onSave = { selections, round, stake, mult, actual, status, note ->
                val id = target.id
                editingOrder = null
                onEditOrder(id, selections, round, stake, mult, actual, status, note)
            }
        )
    }

    deletingOrder?.let { target ->
        ConfirmDeleteDialog(
            title = if (target.status in listOf("won", "lost", "rejected")) "ပြီးဆုံးအော်ဒါ ဖျက်မည်လား?" else "အော်ဒါ ဖျက်မည်လား?",
            message = "${target.customerName} (${formatKs(target.stakeKs)}) အော်ဒါမှတ်တမ်းကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingOrder = null },
            onConfirm = {
                val id = target.id
                deletingOrder = null
                onDeleteOrder(id)
            }
        )
    }

    if (showClearAllConfirm) {
        ConfirmDeleteDialog(
            title = "ပြီးဆုံးပြီးသား အော်ဒါအားလုံး ဖျက်မည်လား?",
            message = "ပြီးဆုံးပြီးသော အော်ဒါမှတ်တမ်း ${settledIds.size} ခုလုံးကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                showClearAllConfirm = false
                onClearCompletedOrders(settledIds)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminWalletRequestsTab(
    requests: List<WalletRequest>,
    walletConfig: WalletConfig,
    systemGameConfig: SystemGameConfig,
    isOwnerAdmin: Boolean,
    canApproveWallet: Boolean,
    canEditWalletRates: Boolean,
    busy: Boolean,
    onProcess: (WalletRequest, Boolean, String) -> Unit,
    onEditRequest: (String, Long, Long, String, String, String, String, Boolean, String?) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteRequest: (String) -> Unit,
    onClearProcessed: (List<String>) -> Unit,
    onSaveWalletConfig: (Double, String, Double, String) -> Unit
) {
    var editingRequest by remember { mutableStateOf<WalletRequest?>(null) }
    var deletingRequest by remember { mutableStateOf<WalletRequest?>(null) }
    var zoomingReceiptBase64 by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showWalletConfig by remember { mutableStateOf(false) }
    var showRequestsList by remember { mutableStateOf(true) }

    var depositPctText by remember { mutableStateOf(walletConfig.depositPercent.toString()) }
    var depositMode by remember { mutableStateOf(walletConfig.depositMode) }
    var withdrawPctText by remember { mutableStateOf(walletConfig.withdrawPercent.toString()) }
    var withdrawMode by remember { mutableStateOf(walletConfig.withdrawMode) }

    LaunchedEffect(walletConfig) {
        depositPctText = if (walletConfig.depositPercent % 1.0 == 0.0) {
            walletConfig.depositPercent.toInt().toString()
        } else {
            walletConfig.depositPercent.toString()
        }
        depositMode = walletConfig.depositMode
        withdrawPctText = if (walletConfig.withdrawPercent % 1.0 == 0.0) {
            walletConfig.withdrawPercent.toInt().toString()
        } else {
            walletConfig.withdrawPercent.toString()
        }
        withdrawMode = walletConfig.withdrawMode
    }

    val processedIds = remember(requests) {
        requests.filter { it.status in listOf("approved", "rejected") }.map { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canEditWalletRates) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWalletConfig = !showWalletConfig }
                                .testTag("toggle_wallet_config_card"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Percent, contentDescription = null, tint = ChampionshipGold)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "ငွေသွင်း / ငွေထုတ် ရာခိုင်နှုန်း သတ်မှတ်ချက်",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val dSign = when (depositMode) { "bonus" -> "+"; "fee" -> "-"; else -> "" }
                                    val wSign = when (withdrawMode) { "bonus" -> "+"; "fee" -> "-"; else -> "" }
                                    Text(
                                        "သွင်း: $dSign${depositPctText.ifBlank { "0" }}% • ထုတ်: $wSign${withdrawPctText.ifBlank { "0" }}%",
                                        color = Color(0xFFB4C8BE),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (showWalletConfig) "ဝှက်မည်" else "ပြမည်",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = if (showWalletConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle wallet config",
                                    tint = ChampionshipGold
                                )
                            }
                        }

                        if (showWalletConfig) {
                            Text(
                                "ငွေသွင်းလျှင် အပိုဆု (+) သို့မဟုတ် ဝန်ဆောင်ခ (-)၊ ငွေထုတ်လျှင် ဝန်ဆောင်ခ (-) သို့မဟုတ် အပိုဆု (+) ကို စိတ်ကြိုက်သတ်မှတ်နိုင်ပါသည်။",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )

                            Text("ငွေသွင်း (Deposit %)", color = ChampionshipGold, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = depositPctText,
                                    onValueChange = { depositPctText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                                    label = { Text("% နှုန်း") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.width(100.dp).testTag("deposit_percent_input")
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("bonus" to "+ အပိုဆု", "fee" to "- ဝန်ဆောင်ခ", "none" to "0% မယူ").forEach { (m, lbl) ->
                                        FilterChip(
                                            selected = depositMode == m,
                                            onClick = { depositMode = m },
                                            label = { Text(lbl) }
                                        )
                                    }
                                }
                            }

                            Text("ငွေထုတ် (Withdraw %)", color = ChampionshipGold, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = withdrawPctText,
                                    onValueChange = { withdrawPctText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                                    label = { Text("% နှုန်း") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.width(100.dp).testTag("withdraw_percent_input")
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("fee" to "- ဝန်ဆောင်ခ", "bonus" to "+ အပိုဆု", "none" to "0% မယူ").forEach { (m, lbl) ->
                                        FilterChip(
                                            selected = withdrawMode == m,
                                            onClick = { withdrawMode = m },
                                            label = { Text(lbl) }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val dPct = (depositPctText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 50.0)
                                    val wPct = (withdrawPctText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 50.0)
                                    onSaveWalletConfig(dPct, depositMode, wPct, withdrawMode)
                                },
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                                modifier = Modifier.fillMaxWidth().testTag("save_wallet_config_button")
                            ) {
                                Text("ရာခိုင်နှုန်း သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (systemGameConfig.dualApprovalThresholdKs > 0L) {
            item {
                Surface(
                    color = DeepPitchElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dual Approval စည်းမျဉ်း: ${formatKs(systemGameConfig.dualApprovalThresholdKs)} နှင့်အထက် ငွေထုတ်မှုများကို Main Admin မှ ထပ်ဆင့်အတည်ပြုပေးရပါမည်။",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        if (processedIds.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = { showClearAllConfirm = true },
                    border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("clear_old_wallet_requests_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ပြီးစီးပြီးသား အဟောင်းများ ဖျက်မည် (${processedIds.size})", color = AdminCrimson)
                }
            }
        }
        item {
            HideBarSectionHeader(
                title = "ငွေသွင်း / ငွေထုတ် တောင်းဆိုမှုဇယား",
                countBadge = "${requests.size} ခု",
                isVisible = showRequestsList,
                onToggle = { showRequestsList = !showRequestsList },
                testTag = "hide_bar_admin_wallet_requests"
            )
        }

        if (showRequestsList) {
        if (requests.isEmpty()) {
            item {
                Text("ငွေသွင်း / ငွေထုတ် တောင်းဆိုမှု မရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(requests, key = { it.id }) { req ->
                val displayNetKs = if (req.status == "pending") {
                    if (req.requestType == "topup") walletConfig.computeNetTopup(req.amountKs)
                    else walletConfig.computeNetWithdraw(req.amountKs)
                } else {
                    req.netAmountKs
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${req.customerName} • ${if (req.requestType == "topup") "ငွေသွင်း" else "ငွေထုတ်"}",
                                color = InkDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                StatusBadge(req.status)
                                if (isOwnerAdmin || canApproveWallet) {
                                    IconButton(
                                        onClick = { editingRequest = req },
                                        modifier = Modifier.testTag("edit_wallet_request_${req.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Request & Receipt", tint = PitchEmeraldDark)
                                    }
                                }
                                if (isOwnerAdmin || req.status in listOf("approved", "rejected")) {
                                    IconButton(
                                        onClick = { deletingRequest = req },
                                        modifier = Modifier.testTag("delete_wallet_request_${req.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Request", tint = AdminCrimson)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${formatKs(req.amountKs)} (${req.channel})",
                            color = PitchEmeraldDark,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        if (displayNetKs > 0L && displayNetKs != req.amountKs) {
                            Text(
                                text = "အသားတင် ပမာဏ: ${formatKs(displayNetKs)}",
                                color = AdminCrimson,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("Ref: ${req.referenceNote}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        Text(formatTimestamp(req.createdAt), color = InkMuted, style = MaterialTheme.typography.labelMedium)
                        if (req.receiptImageBase64.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReceiptImageThumbnail(
                                    base64 = req.receiptImageBase64,
                                    onClick = { zoomingReceiptBase64 = req.receiptImageBase64 },
                                    size = 56.dp
                                )
                                Text(
                                    text = "ပေးပို့ထားသော ပြေစာဓာတ်ပုံ (နှိပ်၍ အကျယ်ကြည့်/ပြင်နိုင်)",
                                    color = PitchEmeraldDark,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (req.status == "awaiting_main_approval" && !isOwnerAdmin) {
                            Spacer(Modifier.height(8.dp))
                            Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Main Admin အတည်ပြုချက် စောင့်ဆိုင်းနေသည် (${req.adminNote})",
                                    color = Color(0xFF92400E),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        val canActOnReq = canApproveWallet && (
                            req.status == "pending" || (isOwnerAdmin && req.status == "awaiting_main_approval")
                        )
                        if (canActOnReq) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onProcess(req, true, "အတည်ပြုပြီး") },
                                    enabled = !busy,
                                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (req.status == "awaiting_main_approval") "Main Admin အတည်ပြု" else "အတည်ပြု")
                                }
                                OutlinedButton(
                                    onClick = { onProcess(req, false, "ပယ်ဖျက်ပြီး") },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ပယ်ဖျက်", color = AdminCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    editingRequest?.let { req ->
        EditWalletRequestDialog(
            req = req,
            onDismiss = { editingRequest = null },
            onSave = { amt, net, ch, ref, st, note, hid, receiptB64 ->
                val id = req.id
                editingRequest = null
                onEditRequest(id, amt, net, ch, ref, st, note, hid, receiptB64)
            }
        )
    }

    zoomingReceiptBase64?.let { b64 ->
        FullScreenReceiptDialog(
            base64 = b64,
            onDismiss = { zoomingReceiptBase64 = null }
        )
    }

    deletingRequest?.let { req ->
        ConfirmDeleteDialog(
            title = "ငွေစာရင်းအဟောင်း ဖျက်မည်လား?",
            message = "${req.customerName} (${formatKs(req.amountKs)}) မှတ်တမ်းကို ဖျက်ပစ်ပါမည်။ Customer ၏ လက်ရှိလက်ကျန်ငွေ မပြောင်းလဲပါ။",
            onDismiss = { deletingRequest = null },
            onConfirm = {
                val id = req.id
                deletingRequest = null
                onDeleteRequest(id)
            }
        )
    }

    if (showClearAllConfirm) {
        ConfirmDeleteDialog(
            title = "ပြီးစီးပြီးသား အဟောင်းများ အားလုံးဖျက်မည်လား?",
            message = "အတည်ပြုပြီး / ပယ်ဖျက်ပြီး ငွေသွင်း၊ ငွေထုတ် မှတ်တမ်း ${processedIds.size} ခုလုံးကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                showClearAllConfirm = false
                onClearProcessed(processedIds)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminBoardTab(
    matches: List<SportMatch>,
    draws: List<LotteryDraw>,
    boardConfig: BoardConfig,
    busy: Boolean,
    onSaveNewMatch: (String, String, String, String, String, String, Double, Double, String, String, String) -> Unit,
    onUpdateMatchDetails: (String, String, String, String, String, String, String, Double, Double, String, Long, Long, String?, String?) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onSaveNewDraw: (roundTitle: String, closeTimeLabel: String, payoutMultiplier: Double, drawType: String) -> Unit,
    onUpdateLotteryDraw: (String, String, String, Double, String, String) -> Unit,
    onDeleteLotteryDraw: (String) -> Unit,
    onSeedSampleBoard: () -> Unit,
    onSyncDailyMatchesNow: () -> Unit,
    onSyncLiveResultsNow: () -> Unit,
    onToggleAutoDailyMatches: (Boolean) -> Unit,
    onToggleAutoLiveResults: (Boolean) -> Unit,
    onSaveAllowedLeagues: (List<String>) -> Unit = {}
) {
    val standardMatches = remember(matches) {
        matches.filter { it.boardSource.isBlank() || it.boardSource == "standard" }
    }
    var showAddMatch by remember { mutableStateOf(false) }
    var editingMatch by remember { mutableStateOf<SportMatch?>(null) }
    var deletingMatch by remember { mutableStateOf<SportMatch?>(null) }
    var showAddDraw by remember { mutableStateOf(false) }
    var editingDraw by remember { mutableStateOf<LotteryDraw?>(null) }
    var deletingDraw by remember { mutableStateOf<LotteryDraw?>(null) }
    var showAutoSyncCard by remember { mutableStateOf(false) }
    var showDrawsSection by remember { mutableStateOf(true) }
    var showMatchesSection by remember { mutableStateOf(true) }
    val adminExpandedLeagues = remember { mutableStateMapOf<String, Boolean>() }
    var adminNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            adminNowMs = System.currentTimeMillis()
        }
    }
    val groupedStandardMatches = remember(standardMatches) {
        standardMatches.groupBy { it.league.ifBlank { "Football" } }
    }
    var selectedLeagues by remember(boardConfig.allowedLeagues) {
        mutableStateOf(boardConfig.allowedLeagues.toSet())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAutoSyncCard = !showAutoSyncCard }
                            .testTag("toggle_auto_sync_board_card"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "နေ့စဉ်ပွဲစဉ် & တိုက်ရိုက်ရလဒ် အလိုအလျောက်စနစ်",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Auto ပွဲ: ${if (boardConfig.autoSyncDailyMatches) "ON" else "OFF"} • Live ရလဒ်: ${if (boardConfig.autoSyncLiveResults) "ON" else "OFF"}",
                                    color = Color(0xFFB4C8BE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showAutoSyncCard) "ဝှက်မည်" else "ပြမည်",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = if (showAutoSyncCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle auto sync card",
                                tint = ChampionshipGold
                            )
                        }
                    }

                    if (showAutoSyncCard) {
                        Button(
                            onClick = onSyncDailyMatchesNow,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                            modifier = Modifier.fillMaxWidth().testTag("sync_daily_matches_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ယနေ့ ပွဲစဉ်များ အလိုအလျောက်ထည့်မည်")
                        }
                        Button(
                            onClick = onSyncLiveResultsNow,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier.fillMaxWidth().testTag("sync_live_results_button")
                        ) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("တိုက်ရိုက်ရလဒ်များ ချိတ်ဆက်မည်", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("ပွဲရှိသည့်နေ့တိုင်း အလိုအလျောက်ထည့်မည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                                if (boardConfig.lastSyncedDate.isNotBlank()) {
                                    Text("နောက်ဆုံးထည့်ပြီးရက်: ${boardConfig.lastSyncedDate}", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Switch(
                                checked = boardConfig.autoSyncDailyMatches,
                                onCheckedChange = onToggleAutoDailyMatches,
                                modifier = Modifier.testTag("auto_daily_matches_switch")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "တိုက်ရိုက်ရလဒ် အလိုအလျောက်ချိတ်ဆက်မည်",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Switch(
                                checked = boardConfig.autoSyncLiveResults,
                                onCheckedChange = onToggleAutoLiveResults,
                                modifier = Modifier.testTag("auto_live_results_switch")
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Text(
                            text = if (selectedLeagues.isEmpty()) "ဖွင့်လှစ်မည့် ဖလား/လိဂ်: အားလုံး (All)"
                            else "ရွေးချယ်ထားသော လိဂ် (${selectedLeagues.size})",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedLeagues.isEmpty(),
                                onClick = { selectedLeagues = emptySet() },
                                label = { Text("အားလုံး (All)") }
                            )
                            FootballFeedService.DEFAULT_LEAGUE_OPTIONS.forEach { leagueName ->
                                val picked = selectedLeagues.contains(leagueName)
                                FilterChip(
                                    selected = picked,
                                    onClick = {
                                        selectedLeagues = if (picked) selectedLeagues - leagueName else selectedLeagues + leagueName
                                    },
                                    label = { Text(leagueName) }
                                )
                            }
                        }
                        Button(
                            onClick = { onSaveAllowedLeagues(selectedLeagues.toList()) },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier.fillMaxWidth().testTag("save_allowed_leagues_button")
                        ) {
                            Text("ဖလား/လိဂ် ရွေးချယ်မှု သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onSeedSampleBoard,
                            modifier = Modifier.fillMaxWidth().testTag("seed_sample_board_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(6.dp))
                            Text("နမူနာ ဘောလုံးပွဲစဉ်နှင့် ၃လုံးထီ အလိုအလျောက်ထည့်မည်", color = ChampionshipGold)
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddMatch = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                    modifier = Modifier.weight(1f).testTag("admin_add_match_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ပွဲစဉ်သစ်")
                }
                Button(
                    onClick = { showAddDraw = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                    modifier = Modifier.weight(1f).testTag("admin_add_draw_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("2D / 3D ထီသစ်")
                }
            }
        }

        item {
            HideBarSectionHeader(
                title = "၂လုံး / ၃လုံးထီ အကြိမ်များ",
                countBadge = "${draws.size}",
                isVisible = showDrawsSection,
                onToggle = { showDrawsSection = !showDrawsSection },
                testTag = "hide_bar_admin_draws"
            )
        }
        if (showDrawsSection) {
            items(draws, key = { it.id }) { draw ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    modifier = Modifier.fillMaxWidth().clickable { editingDraw = draw }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = if (draw.drawType == "2d") Color(0xFFCCFBF1) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = draw.drawType.uppercase(),
                                        color = InkDark,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(draw.roundTitle, color = InkDark, fontWeight = FontWeight.Bold)
                            }
                            Text("${draw.closeTimeLabel} • ${draw.payoutMultiplier.toInt()}x", color = InkMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (draw.winningNumber.isNotBlank()) {
                                Text(draw.winningNumber, color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                            StatusBadge(draw.status)
                            IconButton(
                                onClick = { deletingDraw = draw },
                                modifier = Modifier.testTag("delete_draw_${draw.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Draw", tint = AdminCrimson)
                            }
                        }
                    }
                }
            }
        }

        item {
            HideBarSectionHeader(
                title = "ဘောလုံးပွဲစဉ်များ (ပင်မဇယား)",
                countBadge = "${standardMatches.size}",
                isVisible = showMatchesSection,
                onToggle = { showMatchesSection = !showMatchesSection },
                testTag = "hide_bar_admin_matches"
            )
        }
        if (showMatchesSection) {
            if (standardMatches.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ဖလား/လိဂ် (${groupedStandardMatches.size}) • MMT",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                groupedStandardMatches.keys.forEach { adminExpandedLeagues[it] = true }
                            }) {
                                Text("အားလုံးဖွင့်", color = ChampionshipGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = {
                                groupedStandardMatches.keys.forEach { adminExpandedLeagues[it] = false }
                            }) {
                                Text("အားလုံးဝှက်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            val defaultExpand = groupedStandardMatches.size <= 3
            groupedStandardMatches.forEach { (leagueName, leagueMatches) ->
                val isExpanded = adminExpandedLeagues[leagueName] ?: defaultExpand
                item(key = "admin_std_league_$leagueName") {
                    LeagueHideBarHeader(
                        leagueName = leagueName,
                        matchCount = leagueMatches.size,
                        isExpanded = isExpanded,
                        onToggle = { adminExpandedLeagues[leagueName] = !isExpanded }
                    )
                }
                if (isExpanded) {
                    items(leagueMatches, key = { it.id }) { match ->
                        val resolvedKickoff = MyanmarTimeUtil.resolveMatchKickoffEpochMs(match, adminNowMs)
                        val isExpiredOpen = match.status == "open" && resolvedKickoff in 1L..adminNowMs
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PearlCard),
                            modifier = Modifier.fillMaxWidth().clickable { editingMatch = match }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TeamLogoAvatar(logoUrl = match.homeLogoUrl, teamName = match.homeTeam, size = 22.dp)
                                        Text(
                                            text = "${match.homeTeam} vs ${match.awayTeam}",
                                            color = InkDark,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        TeamLogoAvatar(logoUrl = match.awayLogoUrl, teamName = match.awayTeam, size = 22.dp)
                                    }
                                    Text("${match.league} • ${match.kickoffLabel} (MMT)", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                                    Text("${match.handicapLine} • ဂိုး ${match.goalLine}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                                    if (isExpiredOpen) {
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "အချိန်ကျော်/ပိတ်သင့် (MMT)",
                                                color = AdminCrimson,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        StatusBadge(match.status)
                                        if (match.status == "finished" || match.liveClock.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${match.homeScore} - ${match.awayScore} ${match.liveClock}".trim(),
                                                color = if (match.status == "closed") AdminCrimson else InkDark,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { deletingMatch = match },
                                        modifier = Modifier.testTag("delete_match_${match.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Match", tint = AdminCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMatch) {
        AddMatchDialog(
            boardMode = "standard",
            onDismiss = { showAddMatch = false },
            onSave = { l, h, a, k, hand, goal, bOdds, gOdds, oneExpKyay, fffKyay ->
                showAddMatch = false
                onSaveNewMatch(l, h, a, k, hand, goal, bOdds, gOdds, "standard", oneExpKyay, fffKyay)
            }
        )
    }
    editingMatch?.let { m ->
        EditMatchResultDialog(
            match = m,
            boardMode = m.boardSource.ifBlank { "standard" },
            onDismiss = { editingMatch = null },
            onUpdate = { l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay ->
                editingMatch = null
                onUpdateMatchDetails(m.id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay)
            },
            onRequestDelete = {
                editingMatch = null
                deletingMatch = m
            }
        )
    }
    deletingMatch?.let { m ->
        ConfirmDeleteDialog(
            title = "ပွဲစဉ် ဖျက်မည်လား?",
            message = "${m.homeTeam} vs ${m.awayTeam} (${m.league}) ပွဲစဉ်ကို အပြီးတိုင် ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingMatch = null },
            onConfirm = {
                val id = m.id
                deletingMatch = null
                onDeleteMatch(id)
            }
        )
    }
    if (showAddDraw) {
        AddLotteryDrawDialog(
            onDismiss = { showAddDraw = false },
            onSave = { t, c, mult, drawType ->
                showAddDraw = false
                onSaveNewDraw(t, c, mult, drawType)
            }
        )
    }
    editingDraw?.let { d ->
        PublishLotteryResultDialog(
            draw = d,
            onDismiss = { editingDraw = null },
            onPublish = { title, closeLabel, mult, num, st ->
                editingDraw = null
                onUpdateLotteryDraw(d.id, title, closeLabel, mult, num, st)
            },
            onRequestDelete = {
                editingDraw = null
                deletingDraw = d
            }
        )
    }
    deletingDraw?.let { d ->
        ConfirmDeleteDialog(
            title = "၃လုံးထီ အကြိမ် ဖျက်မည်လား?",
            message = "${d.roundTitle} ကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingDraw = null },
            onConfirm = {
                val id = d.id
                deletingDraw = null
                onDeleteLotteryDraw(id)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminCustomersTab(
    users: List<UserAccount>,
    orders: List<BetOrder>,
    busy: Boolean,
    canAdjustBalance: Boolean,
    canLockUsers: Boolean,
    canChat: Boolean,
    canManageOrders: Boolean,
    onAutoSettleCustomer: (String, String) -> Unit,
    onInspectCustomer: (UserAccount) -> Unit,
    onOpenAdjustBalance: (UserAccount) -> Unit,
    onOpenGiftPoints: (UserAccount) -> Unit,
    onToggleLockUser: (UserAccount) -> Unit,
    onToggleHideUser: (UserAccount) -> Unit,
    onForceLogoutUser: (UserAccount) -> Unit,
    onOpenChatWithCustomer: (UserAccount) -> Unit,
    onCallCustomer: (customerUid: String, customerName: String, callType: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showHiddenUsers by remember { mutableStateOf(false) }
    var showCustomersList by remember { mutableStateOf(true) }

    val ordersByUser = remember(orders) { orders.groupBy { it.userId } }
    val hiddenCount = remember(users) { users.count { it.isHidden } }

    val filteredUsers = remember(users, searchQuery, showHiddenUsers) {
        val visibleList = if (showHiddenUsers) users else users.filter { !it.isHidden }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) visibleList
        else visibleList.filter {
            it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampionshipGold) },
                label = { Text("Customer အမည် သို့မဟုတ် Gmail ရှာရန်") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ChampionshipGold
                ),
                modifier = Modifier.fillMaxWidth().testTag("customer_search_input")
            )
        }
        item {
            val onlineCount = remember(users) { users.count { it.isOnline } }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HideBarSectionHeader(
                    title = "Customer စာရင်းဇယား",
                    countBadge = "${filteredUsers.size} ဦး • Online: $onlineCount",
                    isVisible = showCustomersList,
                    onToggle = { showCustomersList = !showCustomersList },
                    testTag = "hide_bar_admin_customers"
                )
                if (hiddenCount > 0 || showHiddenUsers) {
                    FilterChip(
                        selected = showHiddenUsers,
                        onClick = { showHiddenUsers = !showHiddenUsers },
                        label = { Text(if (showHiddenUsers) "Hide ပါပြ ($hiddenCount)" else "Hide ($hiddenCount)") },
                        modifier = Modifier.testTag("toggle_hidden_users_filter")
                    )
                }
            }
        }
        if (showCustomersList) {
        if (filteredUsers.isEmpty()) {
            item {
                Text("Customer အကောင့် မတွေ့ရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(filteredUsers, key = { it.userId }) { user ->
                val userOrders = ordersByUser[user.userId].orEmpty()
                val activeCount = userOrders.count { it.status in listOf("pending", "accepted") }
                val custSummary = remember(user, userOrders) {
                    SettlementCalculator.summarizeCustomer(user.userId, user, userOrders)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInspectCustomer(user) }
                        .testTag("customer_card_${user.userId}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PitchEmeraldDark)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            user.displayName.ifBlank { "Member" },
                                            color = InkDark,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        OnlineStatusDot(isOnline = user.isOnline, lastSeenTs = user.lastSeenAt)
                                        if (user.isLocked) {
                                            Surface(color = AdminCrimson, shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    "LOCKED",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (user.isHidden) {
                                            Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    "HIDDEN",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            tint = PitchEmeraldDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = user.email.ifBlank { "Gmail မသိမ်းဆည်းရသေးပါ" },
                                            color = if (user.email.isNotBlank()) PitchEmeraldDark else InkMuted,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleHideUser(user) },
                                    modifier = Modifier.testTag("toggle_hide_user_${user.userId}")
                                ) {
                                    Icon(
                                        imageVector = if (user.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (user.isHidden) "Show Customer" else "Hide Customer",
                                        tint = InkMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatKs(user.balanceKs),
                                        color = PitchEmeraldDark,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${user.rewardPoints} Pts",
                                        color = InkDark,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        val netText = when {
                            custSummary.customerNetKs > 0L -> "+${formatKs(custSummary.customerNetKs)} နိုင်"
                            custSummary.customerNetKs < 0L -> "-${formatKs(kotlin.math.abs(custSummary.customerNetKs))} ရှုံး"
                            else -> "0 Ks"
                        }
                        Text(
                            "နိုင် ${custSummary.wonCount} (${formatKs(custSummary.totalWonPayoutKs)}) • ရှုံး ${custSummary.lostCount} (${formatKs(custSummary.totalLostStakeKs)}) • အသားတင်: $netText",
                            color = if (custSummary.customerNetKs >= 0L) PitchEmeraldDark else AdminCrimson,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Active: ${formatKs(user.activeStakeKs)} • အော်ဒါ: ${userOrders.size} (လက်ရှိ $activeCount) • စာရင်းဝင်: ${formatTimestamp(user.createdAt)}",
                            color = InkMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onInspectCustomer(user) },
                                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inspect_customer_${user.userId}")
                            ) {
                                Text("လောင်းကြေး / အသေးစိတ်", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (canManageOrders) {
                                Button(
                                    onClick = { onAutoSettleCustomer(user.userId, user.displayName.ifBlank { "Member" }) },
                                    enabled = activeCount > 0 && !busy,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E), contentColor = Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("customer_card_auto_settle_${user.userId}")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Auto ရှင်း ($activeCount)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (canAdjustBalance) {
                                OutlinedButton(
                                    onClick = { onOpenAdjustBalance(user) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("adjust_balance_${user.userId}")
                                ) {
                                    Text("ငွေ (+/-)", color = InkDark, maxLines = 1)
                                }
                            }
                            Button(
                                onClick = { onOpenGiftPoints(user) },
                                colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gift_points_${user.userId}")
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ပွိုင့်", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            if (canChat) {
                                Button(
                                    onClick = { onOpenChatWithCustomer(user) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_customer_${user.userId}")
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Chat", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                        if (canLockUsers) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onToggleLockUser(user) },
                                    modifier = Modifier.weight(1f).testTag("toggle_lock_${user.userId}")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (user.isLocked) "Lock ပြန်ဖွင့်" else "Account ပိတ်", color = AdminCrimson, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onForceLogoutUser(user) },
                                    modifier = Modifier.weight(1f).testTag("force_logout_${user.userId}")
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Force Out", color = AdminCrimson, maxLines = 1)
                                }
                            }
                        }
                        if (canChat) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onCallCustomer(user.userId, user.displayName.ifBlank { "Member" }, "voice")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("customer_voice_call_${user.userId}")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = PitchEmeraldDark, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Voice Call", color = PitchEmeraldDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onCallCustomer(user.userId, user.displayName.ifBlank { "Member" }, "video")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("customer_video_call_${user.userId}")
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF0369A1), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Video Call", color = Color(0xFF0369A1), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

