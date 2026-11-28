package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.model.Live2DStatus
import com.example.data.model.Lottery2D3DControlConfig
import com.example.data.model.Lottery2DDayRecord
import com.example.data.model.Lottery3DMonthEntry
import com.example.data.model.LotteryDraw
import com.example.data.model.MiniGameControlConfig
import com.example.data.model.MiniGameRound
import com.example.data.model.OneXBetOddsSpec
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
import com.example.ui.components.AdminCalendarDateBar
import com.example.ui.components.AdminLeagueHideBarSection
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.ConfirmAutoSyncDialog
import com.example.ui.components.FullScreenReceiptDialog
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.LeagueHideBarHeader
import com.example.ui.components.LeagueQuickSearchSuggestionsRow
import com.example.ui.components.LoTaYaBrandLogo
import com.example.ui.components.OnlineStatusDot
import com.example.ui.components.OrderSlipCard
import com.example.ui.components.ReceiptImageThumbnail
import com.example.ui.components.SportsLeagueFilterDialog
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
    SHEET_TABLE,
    LOTTERY_2D3D_ROOM,
    MINI_GAMES,
    CUSTOMERS,
    CHAT,
    SUB_ADMINS,
    FINANCE,
    MY_PROFILE,
    BRANDING,
    SYSTEM_SETTINGS,
    SECURITY,
    UPDATE_QUALITY,
    AUDIT_LOGS
}

private fun maskEmailForSubAdmin(email: String): String {
    val trimmed = email.trim()
    if (trimmed.isBlank()) return ""
    val atIdx = trimmed.indexOf('@')
    if (atIdx <= 1) return "***${trimmed.substring(atIdx.coerceAtLeast(0))}"
    return "${trimmed.substring(0, 2)}***${trimmed.substring(atIdx)}"
}

@Composable
fun AdminDashboardScreen(
    currentUserId: String,
    defaultDisplayName: String = "Admin",
    isOwnerAdmin: Boolean = true,
    adminMember: AdminMember? = null,
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
    live2DStatus: Live2DStatus? = null,
    lottery2D3DConfig: Lottery2D3DControlConfig = Lottery2D3DControlConfig(),
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
    onClearFinishedMatches: (List<String>) -> Unit = {},
    onToggleHideOrder: (BetOrder) -> Unit = {},
    onEditOrder: (String, List<String>, String, Long, Double, Long, String, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onProcessWalletRequest: (WalletRequest, Boolean, String) -> Unit,
    onDeleteWalletRequest: (String) -> Unit,
    onEditWalletRequest: (String, Long, Long, String, String, String, String, Boolean, String?) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onClearProcessedWalletRequests: (List<String>) -> Unit,
    onEditTransaction: (String, String, Long, String, Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteTransaction: (String) -> Unit = {},
    onClearAllTransactions: (List<String>) -> Unit = {},
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
    onSaveNewMatch: (String, String, String, String, String, String, Double, Double, String, String, String, com.example.data.model.OneXBetOddsSpec?) -> Unit,
    onUpdateMatchDetails: (String, String, String, String, String, String, String, Double, Double, String, Long, Long, String?, String?, com.example.data.model.OneXBetOddsSpec?) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onSaveNewDraw: (roundTitle: String, closeTimeLabel: String, payoutMultiplier: Double, drawType: String) -> Unit,
    onUpdateLotteryDraw: (String, String, String, Double, String, String) -> Unit,
    onDeleteLotteryDraw: (String) -> Unit,
    onSeedSampleBoard: () -> Unit,
    onSeedSampleExpressBoard: (String) -> Unit = {},
    onSyncDailyMatchesNow: (String?) -> Unit,
    onSyncLiveResultsNow: () -> Unit,
    onToggleAutoDailyMatches: (Boolean) -> Unit,
    onToggleAutoLiveResults: (Boolean) -> Unit,
    onSyncOneExpressNow: (String?) -> Unit = {},
    onSync555Now: (String?) -> Unit = {},
    onToggleAutoOneExpress: (Boolean) -> Unit = {},
    onToggleAuto555: (Boolean) -> Unit = {},
    onSaveAllowedLeagues: (List<String>) -> Unit = {},
    onSaveBoardSheetUrl: (String) -> Unit = {},
    onSaveBoardSheetSettings: (String, String, Boolean) -> Unit = { s, _, _ -> onSaveBoardSheetUrl(s) },
    onSyncMatchesFromSheet: (String, String) -> Unit = { _, _ -> },
    onSyncMatchesFromSheetSilent: (String, String, Boolean) -> Unit = { src, url, _ -> onSyncMatchesFromSheet(src, url) },
    onPushToGoogleSheetWebhook: () -> Unit = {},
    onSaveActiveMatchDateFilter: (String) -> Unit = {},
    onAllocateAdminFloat: (String, String, Long) -> Unit = { _, _, _ -> },
    onTransferUserBalanceToAdminFloat: (String, String, Long) -> Unit = { _, _, _ -> },
    onSaveNewMatchWithDate: ((String, String, String, String, String, String, Double, Double, String, String, String, com.example.data.model.OneXBetOddsSpec?, String) -> Unit)? = null,
    onUpdateMatchDetailsWithDate: ((String, String, String, String, String, String, String, Double, Double, String, Long, Long, String?, String?, com.example.data.model.OneXBetOddsSpec?, String) -> Unit)? = null,
    onImportCsvText: (String, String) -> Unit = { _, _ -> },
    onUpdateMatchFromSheetRow: (SportMatch, String, Long, Long, String, String, String, String, String, String, Double, Double, Double, Double, Double, Double, Double) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onSync2D3DFromApiNow: () -> Unit = {},
    onSaveLottery2D3DConfig: (Lottery2D3DControlConfig) -> Unit = {},
    onTriggerManual2D3DNotification: (String, String) -> Unit = { _, _ -> },
    onSave2DCalendarDay: (Int, Int, String, Lottery2DDayRecord) -> Unit = { _, _, _, _ -> },
    onSave3DCalendarMonth: (Int, String, Lottery3DMonthEntry) -> Unit = { _, _, _ -> },
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
    onSaveAppUpdateConfig: (Int, String, String, String, Boolean, String, String, String, String, String) -> Unit,
    onDeleteQualityReview: (String) -> Unit,
    onCallCustomer: (customerUid: String, customerName: String, callType: String) -> Unit = { _, _, _ -> }
) {
    val visibleTabs = remember(isOwnerAdmin, permissions) {
        buildList {
            if (isOwnerAdmin || permissions.canManageOrders) {
                add(AdminTabKey.ORDERS)
            }
            if (isOwnerAdmin) {
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
            if (isOwnerAdmin || permissions.canManageBoard || permissions.canManageBoardSheet) {
                add(AdminTabKey.SHEET_TABLE)
            }
            if (isOwnerAdmin || permissions.canManage2D3DRoom) {
                add(AdminTabKey.LOTTERY_2D3D_ROOM)
            }
            if (isOwnerAdmin) {
                add(AdminTabKey.MINI_GAMES)
            }
            if (isOwnerAdmin || permissions.canViewUsers) {
                add(AdminTabKey.CUSTOMERS)
            }
            if (isOwnerAdmin || permissions.canChat) {
                add(AdminTabKey.CHAT)
            }
            if (isOwnerAdmin) {
                add(AdminTabKey.SUB_ADMINS)
                add(AdminTabKey.FINANCE)
            }
            add(AdminTabKey.MY_PROFILE)
            if (isOwnerAdmin) {
                add(AdminTabKey.BRANDING)
                add(AdminTabKey.SYSTEM_SETTINGS)
                add(AdminTabKey.SECURITY)
            }
            if (isOwnerAdmin || permissions.canEditGameConfig) {
                add(AdminTabKey.UPDATE_QUALITY)
            }
            add(AdminTabKey.AUDIT_LOGS)
        }
    }

    var didAutoRouteSubAdmin by remember(currentUserId, isOwnerAdmin) { mutableStateOf(false) }
    var activeTabKey by remember {
        mutableStateOf(
            if (!isOwnerAdmin && permissions.canManageBoardSheet && visibleTabs.contains(AdminTabKey.SHEET_TABLE)) {
                AdminTabKey.SHEET_TABLE
            } else {
                visibleTabs.firstOrNull() ?: AdminTabKey.AUDIT_LOGS
            }
        )
    }
    LaunchedEffect(visibleTabs, isOwnerAdmin, permissions.canManageBoardSheet) {
        if (!isOwnerAdmin && permissions.canManageBoardSheet && !didAutoRouteSubAdmin && visibleTabs.contains(AdminTabKey.SHEET_TABLE)) {
            activeTabKey = AdminTabKey.SHEET_TABLE
            didAutoRouteSubAdmin = true
        } else if (!visibleTabs.contains(activeTabKey)) {
            activeTabKey = visibleTabs.firstOrNull() ?: AdminTabKey.AUDIT_LOGS
        }
    }
    val selectedTabIndex = visibleTabs.indexOf(activeTabKey).coerceAtLeast(0)
    val maskCustomerPii = !isOwnerAdmin && securityConfig.maskCustomerPiiForSubAdmins

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
    val adminsById = remember(admins) { admins.associateBy { it.userId } }
    val matchesById = remember(matches) { matches.associateBy { it.id } }

    val pendingOrderCount = orders.count { it.status == "pending" }
    val pendingWalletCount = walletReqs.count {
        it.status == "pending" || (isOwnerAdmin && it.status == "awaiting_main_approval")
    }

    Scaffold(
        containerColor = DeepPitchBg,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF3E0E12), Color(0xFF1F1612))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        BrandLogoAvatar(brandingConfig = brandingConfig, size = 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    "${brandingConfig.appName.ifBlank { "လိုတရ" }} Admin",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = if (isOwnerAdmin) ChampionshipGold else Color(0xFF0284C7),
                                    shape = RoundedCornerShape(5.dp)
                                ) {
                                    Text(
                                        text = if (isOwnerAdmin) "MAIN" else "SECOND",
                                        color = if (isOwnerAdmin) InkDark else Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (!isOwnerAdmin && adminMember != null) {
                                Text(
                                    text = "Float: ${formatKs(adminMember.walletBalanceKs)}",
                                    color = Color(0xFF38BDF8),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshAll,
                            enabled = !busy,
                            modifier = Modifier.size(38.dp).testTag("admin_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampionshipGold, modifier = Modifier.size(20.dp))
                        }
                        Surface(
                            color = PitchEmeraldDark,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable(onClick = onSwitchToCustomer)
                                .testTag("switch_to_customer_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Customer", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
                .padding(padding)
                .navigationBarsPadding()
                .imePadding(),
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
                        AdminTabKey.SETTLEMENT -> "ရှင်းလင်းရေး/စာရင်းချုပ်" to "admin_tab_settlement"
                        AdminTabKey.WALLET -> "ငွေသွင်း/ထုတ် ($pendingWalletCount)" to "admin_tab_wallet"
                        AdminTabKey.BOARD -> "ပွဲစဉ် / ထီ" to "admin_tab_board"
                        AdminTabKey.ONE_EXPRESS -> "1xBET ကြေး" to "admin_tab_one_express"
                        AdminTabKey.FIVE_FIVE_FIVE -> "555 Mix ကြေး" to "admin_tab_five_five_five"
                        AdminTabKey.SHEET_TABLE -> "Sheet ဇယား (A–P)" to "admin_tab_sheet_table"
                        AdminTabKey.LOTTERY_2D3D_ROOM -> "2D/3D ထိန်းချုပ်ခန်း" to "admin_tab_2d3d_room"
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
                        maskCustomerPii = maskCustomerPii,
                        canEditGameConfig = isOwnerAdmin || permissions.canEditGameConfig,
                        canManageBoard = isOwnerAdmin || permissions.canManageBoard,
                        busy = busy,
                        onOpenOneExpressBoard = { activeTabKey = AdminTabKey.ONE_EXPRESS },
                        onOpen555Board = { activeTabKey = AdminTabKey.FIVE_FIVE_FIVE },
                        onSyncLiveResultsNow = onSyncLiveResultsNow,
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
                        matches = matches,
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
                        onEditOrder = onEditOrder,
                        onSyncLiveResultsNow = onSyncLiveResultsNow,
                        onClearCompletedOrders = onClearCompletedOrders,
                        onClearFinishedMatches = onClearFinishedMatches,
                        onUpdateMatchDetails = onUpdateMatchDetails,
                        onDeleteMatch = onDeleteMatch
                    )
                    AdminTabKey.WALLET -> AdminWalletRequestsTab(
                        requests = walletReqs,
                        walletConfig = walletConfig,
                        systemGameConfig = systemGameConfig,
                        isOwnerAdmin = isOwnerAdmin,
                        adminMember = adminMember,
                        maskCustomerPii = maskCustomerPii,
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
                        onSaveAllowedLeagues = onSaveAllowedLeagues,
                        onSaveSheetUrl = onSaveBoardSheetUrl,
                        onSyncMatchesFromSheet = onSyncMatchesFromSheet,
                        onImportCsvText = onImportCsvText,
                        onSaveActiveMatchDateFilter = onSaveActiveMatchDateFilter,
                        onPushToWebhook = onPushToGoogleSheetWebhook,
                        onSaveNewMatchWithDate = onSaveNewMatchWithDate,
                        onUpdateMatchDetailsWithDate = onUpdateMatchDetailsWithDate
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
                        onSaveSheetUrl = onSaveBoardSheetUrl,
                        onSyncMatchesFromSheet = onSyncMatchesFromSheet,
                        onImportCsvText = onImportCsvText,
                        onSeedSampleBoard = { onSeedSampleExpressBoard("one_express") },
                        onSaveNewMatch = onSaveNewMatch,
                        onUpdateMatchDetails = onUpdateMatchDetails,
                        onDeleteMatch = onDeleteMatch,
                        onOpenSheetTable = {
                            if (visibleTabs.contains(AdminTabKey.SHEET_TABLE)) {
                                activeTabKey = AdminTabKey.SHEET_TABLE
                            }
                        },
                        onSaveActiveMatchDateFilter = onSaveActiveMatchDateFilter,
                        onPushToWebhook = onPushToGoogleSheetWebhook,
                        onSaveNewMatchWithDate = onSaveNewMatchWithDate,
                        onUpdateMatchDetailsWithDate = onUpdateMatchDetailsWithDate
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
                        onSaveSheetUrl = onSaveBoardSheetUrl,
                        onSyncMatchesFromSheet = onSyncMatchesFromSheet,
                        onImportCsvText = onImportCsvText,
                        onSeedSampleBoard = { onSeedSampleExpressBoard("five_five_five") },
                        onSaveNewMatch = onSaveNewMatch,
                        onUpdateMatchDetails = onUpdateMatchDetails,
                        onDeleteMatch = onDeleteMatch,
                        onOpenSheetTable = {
                            if (visibleTabs.contains(AdminTabKey.SHEET_TABLE)) {
                                activeTabKey = AdminTabKey.SHEET_TABLE
                            }
                        },
                        onSaveActiveMatchDateFilter = onSaveActiveMatchDateFilter,
                        onPushToWebhook = onPushToGoogleSheetWebhook,
                        onSaveNewMatchWithDate = onSaveNewMatchWithDate,
                        onUpdateMatchDetailsWithDate = onUpdateMatchDetailsWithDate
                    )
                    AdminTabKey.SHEET_TABLE -> AdminMatchSheetTableTab(
                        matches = matches,
                        boardConfig = boardConfig,
                        busy = busy,
                        onSaveSheetUrl = onSaveBoardSheetUrl,
                        onSaveBoardSheetSettings = onSaveBoardSheetSettings,
                        onSyncMatchesFromSheet = onSyncMatchesFromSheet,
                        onSyncMatchesFromSheetSilent = onSyncMatchesFromSheetSilent,
                        onPushToWebhook = onPushToGoogleSheetWebhook,
                        onSaveActiveMatchDateFilter = onSaveActiveMatchDateFilter,
                        onImportCsvText = onImportCsvText,
                        onUpdateMatchFromSheetRow = onUpdateMatchFromSheetRow,
                        onSaveNewMatch = onSaveNewMatch,
                        onSaveNewMatchWithDate = onSaveNewMatchWithDate
                    )
                    AdminTabKey.LOTTERY_2D3D_ROOM -> Admin2D3DControlRoomTab(
                        config = lottery2D3DConfig,
                        live2DStatus = live2DStatus,
                        busy = busy,
                        onSyncFromApiNow = onSync2D3DFromApiNow,
                        onSaveConfig = onSaveLottery2D3DConfig,
                        onTriggerManualNoti = onTriggerManual2D3DNotification,
                        onSave2DCalendarDay = onSave2DCalendarDay,
                        onSave3DCalendarMonth = onSave3DCalendarMonth
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
                        adminsById = adminsById,
                        isOwnerAdmin = isOwnerAdmin,
                        orders = orders,
                        busy = busy,
                        maskCustomerPii = maskCustomerPii,
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
                        onDeleteTransaction = onDeleteTransaction,
                        onClearAllTransactions = onClearAllTransactions
                    )
                    AdminTabKey.SUB_ADMINS -> AdminSubAdminsTab(
                        currentUserId = currentUserId,
                        admins = admins,
                        invites = invites,
                        supportAgents = supportAgents,
                        paymentAccounts = paymentAccounts,
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
                        onAllocateAdminFloat = onAllocateAdminFloat,
                        onRemoveAdmin = onRemoveAdminMember
                    )
                    AdminTabKey.MY_PROFILE -> AdminAgentSelfProfileTab(
                        currentUserId = currentUserId,
                        isOwnerAdmin = isOwnerAdmin,
                        defaultDisplayName = defaultDisplayName,
                        supportAgents = supportAgents,
                        paymentAccounts = paymentAccounts,
                        adminMember = adminMember,
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
                        currentUserId = currentUserId,
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
            user = if (maskCustomerPii && inspectedUser.email.isNotBlank()) {
                inspectedUser.copy(email = maskEmailForSubAdmin(inspectedUser.email))
            } else {
                inspectedUser
            },
            customerOrders = custOrders,
            customerWalletReqs = custReqs,
            matchesById = matchesById,
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
            matchedAdminMember = adminsById[target.userId],
            isOwnerAdmin = isOwnerAdmin,
            onDismiss = { adjustingUser = null },
            onAdjust = { delta, note ->
                adjustingUser = null
                onAdjustUserBalance(target.userId, delta, note)
            },
            onAdjustFloat = { delta ->
                adjustingUser = null
                onAllocateAdminFloat(target.userId, target.displayName.ifBlank { "Second Admin" }, delta)
            },
            onTransferPlayBalanceToFloat = { amt ->
                adjustingUser = null
                onTransferUserBalanceToAdminFloat(target.userId, target.displayName.ifBlank { "Second Admin" }, amt)
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
    maskCustomerPii: Boolean = false,
    canEditGameConfig: Boolean,
    canManageBoard: Boolean,
    busy: Boolean,
    onOpenOneExpressBoard: () -> Unit,
    onOpen555Board: () -> Unit,
    onSyncLiveResultsNow: () -> Unit = {},
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
    var showAutoAcceptCard by remember { mutableStateOf(true) }
    var showOrderFilters by remember { mutableStateOf(true) }
    var showCommissionSettings by remember { mutableStateOf(false) }
    var showOrdersList by remember { mutableStateOf(true) }
    var editingOrder by remember { mutableStateOf<BetOrder?>(null) }
    var deletingOrder by remember { mutableStateOf<BetOrder?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    val collapsedCustomers = remember { mutableStateMapOf<String, Boolean>() }
    val collapsedSubGroups = remember { mutableStateMapOf<String, Boolean>() }
    val matchesById = remember(matches) { matches.associateBy { it.id } }

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
            HideBarSectionHeader(
                title = "အော်ဒါ Auto-Accept စနစ်",
                countBadge = if (bettingConfig.autoAcceptOrders) "ON" else "OFF",
                isVisible = showAutoAcceptCard,
                onToggle = { showAutoAcceptCard = !showAutoAcceptCard },
                testTag = "hide_bar_admin_orders_auto_accept"
            )
        }
        if (showAutoAcceptCard) {
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
        }

        item {
            HideBarSectionHeader(
                title = "စစ်ထုတ်မှု & ပြသပုံ (Filters)",
                countBadge = filter.uppercase(),
                isVisible = showOrderFilters,
                onToggle = { showOrderFilters = !showOrderFilters },
                testTag = "hide_bar_admin_orders_filters"
            )
        }
        if (showOrderFilters) {
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
        }

        item {
            OutlinedButton(
                onClick = onSyncLiveResultsNow,
                enabled = !busy,
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth().testTag("admin_orders_refresh_live_goals_button")
            ) {
                Icon(Icons.Default.LiveTv, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("တိုက်ရိုက်ဂိုးရလဒ် Refresh", color = ChampionshipGold, fontWeight = FontWeight.Bold)
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
                val rawCustEmail = userAccount?.email.orEmpty()
                val custEmail = if (maskCustomerPii && rawCustEmail.isNotBlank()) {
                    maskEmailForSubAdmin(rawCustEmail)
                } else {
                    rawCustEmail
                }
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
                    val morning2DOrders = customerOrders.filter {
                        it.orderType == "two_d" && (
                            it.drawRound.contains("12:01") ||
                                it.drawRound.contains("နေ့") ||
                                it.drawRound.contains("Morning", ignoreCase = true)
                            )
                    }
                    val evening2DOrders = customerOrders.filter {
                        it.orderType == "two_d" && !(
                            it.drawRound.contains("12:01") ||
                                it.drawRound.contains("နေ့") ||
                                it.drawRound.contains("Morning", ignoreCase = true)
                            )
                    }
                    val otherOrders = customerOrders.filter { it.orderType != "two_d" }
                    val subSections = listOf(
                        Triple("morning_2d", "နေ့ဘက် (12:01 PM)", morning2DOrders),
                        Triple("evening_2d", "ညဘက် (4:30 PM)", evening2DOrders),
                        Triple("other", "3D / ဘောလုံး & ဂိမ်း", otherOrders)
                    ).filter { it.third.isNotEmpty() }

                    subSections.forEach { (subKey, subTitle, subOrders) ->
                        val subMapKey = "${uid}_$subKey"
                        val isSubCollapsed = collapsedSubGroups[subMapKey] == true
                        val subStake = subOrders.sumOf { it.stakeKs }
                        item(key = "sub_header_$subMapKey") {
                            Surface(
                                color = Color(0xFF102B24),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PitchEmeraldDark.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp)
                                    .clickable { collapsedSubGroups[subMapKey] = !isSubCollapsed }
                                    .testTag("toggle_sub_group_$subMapKey")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$subTitle • ${subOrders.size} ခု (${formatKs(subStake)})",
                                        color = ChampionshipGold,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = if (isSubCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                        contentDescription = if (isSubCollapsed) "Expand" else "Hide",
                                        tint = ChampionshipGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (!isSubCollapsed) {
                            items(subOrders, key = { it.id }) { order ->
                                OrderSlipCard(
                                    order = order,
                                    matchesById = matchesById,
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
            }
        } else {
            items(filtered, key = { it.id }) { order ->
                OrderSlipCard(
                    order = order,
                    matchesById = matchesById,
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
    adminMember: AdminMember? = null,
    maskCustomerPii: Boolean = false,
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
        if (!isOwnerAdmin && adminMember != null) {
            item {
                val isLowFloat = adminMember.walletBalanceKs <= 50_000L
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(
                        1.5.dp,
                        if (isLowFloat) AdminCrimson.copy(alpha = 0.85f) else PitchEmeraldDark.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("second_admin_wallet_float_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Second Admin Float လက်ကျန်ငွေ",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = formatKs(adminMember.walletBalanceKs),
                            color = if (isLowFloat) Color(0xFFFCA5A5) else ChampionshipGold,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "စုစုပေါင်းထည့်ပေးငွေ: ${formatKs(adminMember.totalAllocatedKs)} • ထည့်ပေးပြီး: ${formatKs(adminMember.totalTopupsGivenKs)} • ထုတ်ပေးပြီး: ${formatKs(adminMember.totalWithdrawalsTakenKs)}",
                            color = Color(0xFFC5D7CE),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (isLowFloat) {
                            Text(
                                text = "Float ငွေလက်ကျန်နည်းနေပါပြီ — Main Admin ထံမှ Float ထပ်မံတောင်းခံပါ။",
                                color = Color(0xFFFCA5A5),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
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
    onSaveNewMatch: (String, String, String, String, String, String, Double, Double, String, String, String, com.example.data.model.OneXBetOddsSpec?) -> Unit,
    onUpdateMatchDetails: (String, String, String, String, String, String, String, Double, Double, String, Long, Long, String?, String?, com.example.data.model.OneXBetOddsSpec?) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onSaveNewDraw: (roundTitle: String, closeTimeLabel: String, payoutMultiplier: Double, drawType: String) -> Unit,
    onUpdateLotteryDraw: (String, String, String, Double, String, String) -> Unit,
    onDeleteLotteryDraw: (String) -> Unit,
    onSeedSampleBoard: () -> Unit,
    onSyncDailyMatchesNow: (String?) -> Unit,
    onSyncLiveResultsNow: () -> Unit,
    onToggleAutoDailyMatches: (Boolean) -> Unit,
    onToggleAutoLiveResults: (Boolean) -> Unit,
    onSaveAllowedLeagues: (List<String>) -> Unit = {},
    onSaveSheetUrl: (String) -> Unit = {},
    onSyncMatchesFromSheet: (String, String) -> Unit = { _, _ -> },
    onImportCsvText: (String, String) -> Unit = { _, _ -> },
    onSaveActiveMatchDateFilter: (String) -> Unit = {},
    onPushToWebhook: () -> Unit = {},
    onSaveNewMatchWithDate: ((String, String, String, String, String, String, Double, Double, String, String, String, com.example.data.model.OneXBetOddsSpec?, String) -> Unit)? = null,
    onUpdateMatchDetailsWithDate: ((String, String, String, String, String, String, String, Double, Double, String, Long, Long, String?, String?, com.example.data.model.OneXBetOddsSpec?, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val validAllowedLeagues = remember(boardConfig.allowedLeagues) {
        boardConfig.allowedLeagues
            .filter { lg -> FootballFeedService.DEFAULT_LEAGUE_OPTIONS.any { it.equals(lg, ignoreCase = true) } }
            .map { it.trim().lowercase(java.util.Locale.US) }
            .filter { it.isNotBlank() }
            .toSet()
    }
    val standardMatches = remember(matches, validAllowedLeagues) {
        val raw = matches.filter { it.boardSource.isBlank() || it.boardSource == "standard" }
        if (validAllowedLeagues.isEmpty()) raw
        else raw.filter { m -> FootballFeedService.matchesAllowedLeague(m.league, validAllowedLeagues) }
    }
    var adminSearchQuery by remember { mutableStateOf("") }
    var selectedAdminLeagues by remember { mutableStateOf<Set<String>?>(null) }
    var showLeagueFilterDialog by remember { mutableStateOf(false) }
    val todayIsoInit = remember { MyanmarTimeUtil.todayIsoDate() }
    var adminDateFilter by remember(boardConfig.activeMatchDateFilter) {
        val raw = boardConfig.activeMatchDateFilter.trim()
        mutableStateOf(if (raw.isNotBlank() && raw < todayIsoInit) "" else raw)
    }
    LaunchedEffect(boardConfig.activeMatchDateFilter) {
        val raw = boardConfig.activeMatchDateFilter.trim()
        val currentToday = MyanmarTimeUtil.todayIsoDate()
        adminDateFilter = if (raw.isNotBlank() && raw < currentToday) "" else raw
    }
    val standardLeagueCounts = remember(standardMatches, adminDateFilter) {
        standardMatches
            .filter { adminDateFilter.isBlank() || it.matchDate == adminDateFilter }
            .groupingBy { FootballFeedService.canonicalLeagueName(it.league) }
            .eachCount()
    }
    val filteredStandardMatches = remember(standardMatches, adminSearchQuery, adminDateFilter, selectedAdminLeagues) {
        val q = adminSearchQuery.trim()
        val localFilterLower = selectedAdminLeagues?.map { it.trim().lowercase(java.util.Locale.US) }?.toSet()
        standardMatches.filter { m ->
            val leagueOk = q.isNotBlank() ||
                localFilterLower == null ||
                (localFilterLower.isNotEmpty() && FootballFeedService.matchesAllowedLeague(m.league, localFilterLower))
            val dateOk = adminDateFilter.isBlank() || m.matchDate == adminDateFilter
            val qOk = q.isBlank() || FootballFeedService.matchesSearchQuery(m.league, m.homeTeam, m.awayTeam, q)
            leagueOk && dateOk && qOk
        }
    }
    var showAddMatch by remember { mutableStateOf(false) }
    var showSheetDialog by remember { mutableStateOf(false) }
    var showSyncConfirm by remember { mutableStateOf(false) }
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
    val groupedStandardMatches = remember(filteredStandardMatches) {
        filteredStandardMatches
            .groupBy { FootballFeedService.canonicalLeagueName(it.league) }
            .toSortedMap(compareBy({ FootballFeedService.leagueSortOrderIndex(it) }, { it }))
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
                            onClick = { showSyncConfirm = true },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                            modifier = Modifier.fillMaxWidth().testTag("sync_daily_matches_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            val dateTag = if (adminDateFilter.isBlank()) "ယနေ့" else adminDateFilter
                            Text("$dateTag ပွဲစဉ်များ အလိုအလျောက်ထည့်မည်", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        AdminLeagueHideBarSection(
                            selectedLeagues = boardConfig.allowedLeagues,
                            busy = busy,
                            accentColor = ChampionshipGold,
                            testTagPrefix = "admin_leagues_standard",
                            onSaveAllowedLeagues = onSaveAllowedLeagues
                        )
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showSheetDialog = true },
                        enabled = !busy,
                        border = BorderStroke(1.dp, ChampionshipGold),
                        modifier = Modifier.weight(1f).testTag("sheet_sync_button_standard")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sheet / Excel Sync", color = ChampionshipGold, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    val directSheetUrl = boardConfig.googleSheetEditUrl.ifBlank {
                        FootballFeedService.normalizeGoogleSheetEditUrl(boardConfig.googleSheetCsvUrl)
                    }
                    OutlinedButton(
                        onClick = {
                            if (directSheetUrl.isNotBlank()) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(directSheetUrl)))
                                }.onFailure {
                                    Toast.makeText(context, "Google Sheet ဖွင့်၍မရပါ", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                showSheetDialog = true
                            }
                        },
                        enabled = !busy,
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1f).testTag("open_google_sheet_standard")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Google Sheet ဖွင့်", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HideBarSectionHeader(
                    title = "ဘောလုံးပွဲစဉ်များ (ပင်မဇယား)",
                    countBadge = "${filteredStandardMatches.size}",
                    isVisible = showMatchesSection,
                    onToggle = { showMatchesSection = !showMatchesSection },
                    testTag = "hide_bar_admin_matches"
                )
                if (showMatchesSection) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = adminSearchQuery,
                            onValueChange = { adminSearchQuery = it },
                            placeholder = { Text("အသင်း / လိဂ် ရှာရန်...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (adminSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { adminSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark
                            ),
                            modifier = Modifier.weight(1f).testTag("admin_search_standard")
                        )
                        Surface(
                            color = if (selectedAdminLeagues != null) ChampionshipGold else DeepPitchElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selectedAdminLeagues != null) ChampionshipGold else Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showLeagueFilterDialog = true }
                                .testTag("admin_filter_standard")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "42 လိဂ် ရွေးရန်",
                                    tint = if (selectedAdminLeagues != null) InkDark else ChampionshipGold
                                )
                            }
                        }
                    }
                    if (selectedAdminLeagues != null || validAllowedLeagues.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeNames = selectedAdminLeagues?.toList()
                                ?: boardConfig.allowedLeagues.filter { lg ->
                                    FootballFeedService.DEFAULT_LEAGUE_OPTIONS.any { it.equals(lg, ignoreCase = true) }
                                }
                            val labelText = when {
                                activeNames.isEmpty() -> "လိဂ် 0 ခု"
                                activeNames.size == 1 -> "ရွေးထားသောလိဂ်: ${activeNames.first()}"
                                else -> "ရွေးထားသောလိဂ် (${activeNames.size})"
                            }
                            Text(
                                text = labelText,
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    if (selectedAdminLeagues != null) {
                                        selectedAdminLeagues = null
                                    } else {
                                        onSaveAllowedLeagues(emptyList())
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("အားလုံးပြန်ကြည့်", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (adminSearchQuery.isNotBlank()) {
                        LeagueQuickSearchSuggestionsRow(
                            query = adminSearchQuery,
                            leagueMatchCounts = standardLeagueCounts,
                            accentColor = ChampionshipGold,
                            onPickLeague = { picked ->
                                selectedAdminLeagues = setOf(picked)
                                adminExpandedLeagues[picked] = true
                                if (adminDateFilter.isNotBlank()) {
                                    val hasOnDate = standardMatches.any {
                                        it.matchDate == adminDateFilter &&
                                            FootballFeedService.canonicalLeagueName(it.league)
                                                .equals(picked, ignoreCase = true)
                                    }
                                    if (!hasOnDate && standardMatches.any {
                                            FootballFeedService.canonicalLeagueName(it.league)
                                                .equals(picked, ignoreCase = true)
                                        }
                                    ) {
                                        adminDateFilter = ""
                                    }
                                }
                                adminSearchQuery = ""
                            }
                        )
                    }
                    AdminCalendarDateBar(
                        selectedDateIso = adminDateFilter,
                        pinnedDateIso = boardConfig.activeMatchDateFilter,
                        busy = busy,
                        accentColor = ChampionshipGold,
                        testTagPrefix = "standard_date",
                        onSelectDate = { adminDateFilter = it },
                        onPinDate = onSaveActiveMatchDateFilter
                    )
                }
            }
        }
        if (showMatchesSection) {
            if (filteredStandardMatches.isNotEmpty()) {
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
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ရွေးချယ်ထားသော လိဂ်/ရက်စွဲတွင် ပွဲစဉ်မရှိသေးပါ",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (selectedAdminLeagues != null || adminSearchQuery.isNotBlank() || adminDateFilter.isNotBlank()) {
                                TextButton(onClick = {
                                    selectedAdminLeagues = null
                                    adminSearchQuery = ""
                                    adminDateFilter = ""
                                }) {
                                    Text("အားလုံးပြန်ကြည့်မည်", color = ChampionshipGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            val defaultExpand = groupedStandardMatches.size <= 3 || selectedAdminLeagues != null || validAllowedLeagues.isNotEmpty()
            groupedStandardMatches.forEach { (leagueName, leagueMatches) ->
                val isExpanded = if (adminSearchQuery.isNotBlank() || selectedAdminLeagues != null) true else (adminExpandedLeagues[leagueName] ?: defaultExpand)
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
            initialMatchDate = adminDateFilter.ifBlank { MyanmarTimeUtil.todayIsoDate() },
            onDismiss = { showAddMatch = false },
            onSave = { l, h, a, k, hand, goal, bOdds, gOdds, oneExpKyay, fffKyay, xBetOdds ->
                showAddMatch = false
                onSaveNewMatch(l, h, a, k, hand, goal, bOdds, gOdds, "standard", oneExpKyay, fffKyay, xBetOdds)
            },
            onSaveWithDate = { l, h, a, k, hand, goal, bOdds, gOdds, oneExpKyay, fffKyay, xBetOdds, mDate ->
                showAddMatch = false
                if (onSaveNewMatchWithDate != null) {
                    onSaveNewMatchWithDate(l, h, a, k, hand, goal, bOdds, gOdds, "standard", oneExpKyay, fffKyay, xBetOdds, mDate)
                } else {
                    onSaveNewMatch(l, h, a, k, hand, goal, bOdds, gOdds, "standard", oneExpKyay, fffKyay, xBetOdds)
                }
            }
        )
    }
    editingMatch?.let { m ->
        EditMatchResultDialog(
            match = m,
            boardMode = m.boardSource.ifBlank { "standard" },
            onDismiss = { editingMatch = null },
            onUpdate = { l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds ->
                editingMatch = null
                onUpdateMatchDetails(m.id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds)
            },
            onUpdateWithDate = { l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds, mDate ->
                editingMatch = null
                if (onUpdateMatchDetailsWithDate != null) {
                    onUpdateMatchDetailsWithDate(m.id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds, mDate)
                } else {
                    onUpdateMatchDetails(m.id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore, oneExpKyay, fffKyay, xBetOdds)
                }
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
    if (showSyncConfirm) {
        ConfirmAutoSyncDialog(
            boardTitle = "Standard ဘောလုံးပွဲစဉ်များ",
            targetDateIso = adminDateFilter.ifBlank { null },
            onDismiss = { showSyncConfirm = false },
            onConfirm = {
                showSyncConfirm = false
                onSyncDailyMatchesNow(adminDateFilter.ifBlank { null })
            }
        )
    }

    if (showSheetDialog) {
        MatchSheetSyncDialog(
            boardMode = "standard",
            boardConfig = boardConfig,
            matchesToExport = filteredStandardMatches,
            busy = busy,
            onDismiss = { showSheetDialog = false },
            onSaveSheetUrl = onSaveSheetUrl,
            onSyncFromSheetUrl = { url ->
                onSyncMatchesFromSheet("standard", url)
                showSheetDialog = false
            },
            onImportCsvText = { csvText ->
                onImportCsvText("standard", csvText)
                showSheetDialog = false
            },
            onPushToWebhook = onPushToWebhook
        )
    }
    if (showLeagueFilterDialog) {
        val allDefaults = FootballFeedService.DEFAULT_LEAGUE_OPTIONS
        SportsLeagueFilterDialog(
            totalCount = standardMatches.size,
            availableLeagues = standardMatches.map { FootballFeedService.canonicalLeagueName(it.league) }.distinct(),
            selectedLeague = null,
            onSelectLeague = { lg ->
                selectedAdminLeagues = if (lg == null) null else setOf(lg)
            },
            selectedLeagues = selectedAdminLeagues ?: allDefaults.toSet(),
            onSelectLeagues = { nextSet ->
                selectedAdminLeagues = if (nextSet.size >= allDefaults.size) null else nextSet
            },
            onDismiss = { showLeagueFilterDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminCustomersTab(
    users: List<UserAccount>,
    adminsById: Map<String, AdminMember> = emptyMap(),
    isOwnerAdmin: Boolean = true,
    orders: List<BetOrder>,
    busy: Boolean,
    maskCustomerPii: Boolean = false,
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
    val expandedCustomerIds = remember { mutableStateMapOf<String, Boolean>() }

    val ordersByUser = remember(orders) { orders.groupBy { it.userId } }
    val hiddenCount = remember(users) { users.count { it.isHidden } }

    val filteredUsers = remember(users, searchQuery, showHiddenUsers, maskCustomerPii) {
        val visibleList = if (showHiddenUsers) users else users.filter { !it.isHidden }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) visibleList
        else visibleList.filter {
            it.displayName.lowercase().contains(q) || (!maskCustomerPii && it.email.lowercase().contains(q))
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hiddenCount > 0 || showHiddenUsers) {
                        FilterChip(
                            selected = showHiddenUsers,
                            onClick = { showHiddenUsers = !showHiddenUsers },
                            label = { Text(if (showHiddenUsers) "Hide ပါပြ ($hiddenCount)" else "Hide ($hiddenCount)") },
                            modifier = Modifier.testTag("toggle_hidden_users_filter")
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (showCustomersList && filteredUsers.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    filteredUsers.forEach { expandedCustomerIds[it.userId] = true }
                                },
                                modifier = Modifier.testTag("customers_expand_all_button")
                            ) {
                                Text("အားလုံးဖွင့်", color = ChampionshipGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = {
                                    filteredUsers.forEach { expandedCustomerIds[it.userId] = false }
                                },
                                modifier = Modifier.testTag("customers_collapse_all_button")
                            ) {
                                Text("အားလုံးဝှက်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        if (showCustomersList) {
        if (filteredUsers.isEmpty()) {
            item {
                Text("Customer အကောင့် မတွေ့ရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            val defaultCustomerExpanded = filteredUsers.size <= 2
            items(filteredUsers, key = { it.userId }) { user ->
                val userOrders = ordersByUser[user.userId].orEmpty()
                val activeCount = userOrders.count { it.status in listOf("pending", "accepted") }
                val matchedAdmin = adminsById[user.userId]
                val isSecondAdminUser = matchedAdmin != null && matchedAdmin.role != "owner"
                val custSummary = remember(user, userOrders) {
                    SettlementCalculator.summarizeCustomer(user.userId, user, userOrders)
                }
                val isExpanded = expandedCustomerIds[user.userId] ?: defaultCustomerExpanded
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_card_${user.userId}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Compact Per-Customer Hide Bar Header (always visible)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedCustomerIds[user.userId] = !isExpanded }
                                .testTag("hide_bar_customer_${user.userId}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                        if (isSecondAdminUser) {
                                            Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    "2ND ADMIN",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
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
                                    if (isSecondAdminUser && matchedAdmin != null) {
                                        Text(
                                            text = "Second Admin Float: ${formatKs(matchedAdmin.walletBalanceKs)}",
                                            color = Color(0xFF0369A1),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!isExpanded) {
                                        Text(
                                            text = "အော်ဒါ: ${userOrders.size} (လက်ရှိ $activeCount) • နှိပ်၍ အသေးစိတ်ကြည့်ရန်",
                                            color = InkMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse customer details" else "Expand customer details",
                                    tint = PitchEmeraldDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (isExpanded) {
                            HorizontalDivider(color = InkMuted.copy(alpha = 0.16f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = PitchEmeraldDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                val shownEmail = if (maskCustomerPii && user.email.isNotBlank()) {
                                    maskEmailForSubAdmin(user.email)
                                } else {
                                    user.email
                                }
                                Text(
                                    text = shownEmail.ifBlank { "Gmail မသိမ်းဆည်းရသေးပါ" },
                                    color = if (shownEmail.isNotBlank()) PitchEmeraldDark else InkMuted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                        Text(
                                            if (isOwnerAdmin && isSecondAdminUser) "Float / ငွေ (+/-)" else "ငွေ (+/-)",
                                            color = if (isOwnerAdmin && isSecondAdminUser) Color(0xFF0369A1) else InkDark,
                                            fontWeight = if (isOwnerAdmin && isSecondAdminUser) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
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
}

