package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.betting.AutoSettleSummary
import com.example.data.call.WebRtcCallManager
import com.example.data.model.ActiveCallUiState
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.AdminPermissions
import com.example.data.model.AppLanguage
import com.example.data.model.AppQualityReview
import com.example.data.model.AppUpdateConfig
import com.example.data.model.AuditLogEntry
import com.example.data.model.AvailableApkUpdate
import com.example.data.model.BetOrder
import com.example.data.model.BettingConfig
import com.example.data.model.BoardConfig
import com.example.data.model.BrandingConfig
import com.example.data.model.CallSession
import com.example.data.model.DirectChatMessage
import com.example.data.model.GitHubReleaseInfo
import com.example.data.model.GroupChatMessage
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LedgerTransaction
import com.example.data.model.Live2DStatus
import com.example.data.model.LiveFootballFixture
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
import com.example.data.notifications.ChatNotificationHelper
import com.example.data.repository.HtetThuYaRepository
import com.example.data.util.MyanmarTimeUtil
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import java.security.MessageDigest
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import kotlin.math.roundToLong
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: HtetThuYaRepository,
    val currentUserId: String,
    private val currentUserEmail: String?,
    private val isEmailVerified: Boolean,
    val displayName: String,
    private val feedService: FootballFeedService = FootballFeedService(),
    private val appContext: Context? = null
) : ViewModel() {

    private val callManager: WebRtcCallManager? = appContext?.let { WebRtcCallManager(it) }
    private val sessionStartedAtMs: Long = System.currentTimeMillis()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _liveFeed = MutableStateFlow<UiState<List<LiveFootballFixture>>>(UiState.Loading)
    val liveFeed: StateFlow<UiState<List<LiveFootballFixture>>> = _liveFeed.asStateFlow()

    private val _live2DStatus = MutableStateFlow<Live2DStatus?>(null)
    val live2DStatus: StateFlow<Live2DStatus?> = _live2DStatus.asStateFlow()

    private val _githubRelease = MutableStateFlow<GitHubReleaseInfo?>(null)
    private val _updateDismissed = MutableStateFlow(false)
    val updateDismissed: StateFlow<Boolean> = _updateDismissed.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.MY)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _adminPinVerified = MutableStateFlow(false)
    val adminPinVerified: StateFlow<Boolean> = _adminPinVerified.asStateFlow()

    private val _currentPublicIp = MutableStateFlow<String?>(null)
    val currentPublicIp: StateFlow<String?> = _currentPublicIp.asStateFlow()

    private val isBootstrappedOwner =
        isEmailVerified && currentUserEmail.equals(HtetThuYaRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)

    private var autoSyncedSession = false
    private var autoSyncedOneExpressSession = false
    private var autoSyncedFiveFiveFiveSession = false

    val adminMember: StateFlow<AdminMember?> = repository.observeAdminMember(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Admin membership stream error", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val isOwnerAdmin: StateFlow<Boolean> = adminMember
        .map { member ->
            isBootstrappedOwner || (member != null && member.role == "owner" && member.isActiveMember())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), isBootstrappedOwner)

    val isAdmin: StateFlow<Boolean> = adminMember
        .map { member ->
            isBootstrappedOwner || (member != null && member.isActiveMember())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), isBootstrappedOwner)

    val isSecondAdmin: StateFlow<Boolean> = combine(isAdmin, isOwnerAdmin) { admin, owner ->
        admin && !owner
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val currentAdminPermissions: StateFlow<AdminPermissions> = combine(adminMember, isOwnerAdmin) { member, owner ->
        if (owner) AdminPermissions.ALL_GRANTED
        else member?.resolvedPermissions(false) ?: AdminPermissions()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        if (isBootstrappedOwner) AdminPermissions.ALL_GRANTED else AdminPermissions()
    )

    val userAccount: StateFlow<UiState<UserAccount?>> = repository.observeUserAccount(currentUserId)
        .toUiState("Failed to load wallet")

    val matches: StateFlow<UiState<List<SportMatch>>> = repository.observePublicMatches(currentUserId)
        .toUiState("Failed to load matches")

    val lotteryDraws: StateFlow<UiState<List<LotteryDraw>>> = repository.observePublicLotteryDraws(currentUserId)
        .toUiState("Failed to load lottery draws")

    val userOrders: StateFlow<UiState<List<BetOrder>>> = repository.observeUserOrders(currentUserId)
        .toUiState("Failed to load orders")

    val userWalletRequests: StateFlow<UiState<List<WalletRequest>>> = repository.observeUserWalletRequests(currentUserId)
        .toUiState("Failed to load requests")

    val userTransactions: StateFlow<UiState<List<LedgerTransaction>>> = repository.observeUserTransactions(currentUserId)
        .toUiState("Failed to load ledger")

    val userMiniGameRounds: StateFlow<UiState<List<MiniGameRound>>> = repository.observeUserMiniGameRounds(currentUserId)
        .toUiState("Failed to load mini-games")

    val leaderboard: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeLeaderboard(currentUserId)
        .toUiState("Failed to load standings")

    val paymentAccounts: StateFlow<List<PaymentAccount>> = isAdmin
        .flatMapLatest { admin ->
            repository.observePaymentAccounts(currentUserId, isAdminViewer = admin)
                .catch { error ->
                    if (isPureCancellation(error)) throw error
                    Log.w(TAG, "Payment accounts stream error", error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val walletConfig: StateFlow<WalletConfig> = repository.observeWalletConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Wallet config stream error", error)
            emit(WalletConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WalletConfig())

    val boardConfig: StateFlow<BoardConfig> = repository.observeBoardConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Board config stream error", error)
            emit(BoardConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BoardConfig())

    val bettingConfig: StateFlow<BettingConfig> = repository.observeBettingConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Betting config stream error", error)
            emit(BettingConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BettingConfig())

    val systemGameConfig: StateFlow<SystemGameConfig> = repository.observeSystemGameConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "System config stream error", error)
            emit(SystemGameConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SystemGameConfig())

    val securityConfig: StateFlow<SecurityConfig> = repository.observeSecurityConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Security config stream error", error)
            emit(SecurityConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SecurityConfig())

    val brandingConfig: StateFlow<BrandingConfig> = repository.observeBrandingConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Branding config stream error", error)
            emit(BrandingConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BrandingConfig())

    val miniGameControlConfig: StateFlow<MiniGameControlConfig> = repository.observeMiniGameControlConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Mini-game control config stream error", error)
            emit(MiniGameControlConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MiniGameControlConfig())

    val supportAgents: StateFlow<List<SupportAgentProfile>> = repository.observeSupportAgents(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Support agents stream error", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val appUpdateConfig: StateFlow<AppUpdateConfig?> = repository.observeAppUpdateConfig(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "App update config stream error", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val availableApkUpdate: StateFlow<AvailableApkUpdate?> = combine(
        appUpdateConfig,
        _githubRelease
    ) { fsCfg, ghRel ->
        val fsNewer = fsCfg != null && fsCfg.latestVersionCode > BuildConfig.VERSION_CODE
        val ghNewer = ghRel != null && ghRel.updatedAtEpochMs > (BuildConfig.BUILD_EPOCH_MS + 180_000L)
        fun sanitizeReleaseNotes(raw: String, label: String): String {
            val cleaned = raw.lines()
                .filterNot { line ->
                    val low = line.lowercase(java.util.Locale.US)
                    low.contains("htet thu ya") ||
                        low.contains("htet-thu-ya") ||
                        low.contains("direct android apk") ||
                        low.contains("github.com") ||
                        low.contains("github auto") ||
                        low.contains("customer & admin")
                }
                .joinToString("\n")
                .trim()
            return cleaned.ifBlank {
                "🔥 စနစ်သစ် ($label) အဆင်သင့်ဖြစ်ပါပြီ!\n• 1xBET နှင့် 555 Mix တိုက်ရိုက်ကြေးနှုန်းများ ပိုမိုမြန်ဆန်တိကျ\n• တိုက်ရိုက်ပွဲစဉ်ရလဒ်နှင့် ငွေသွင်း/ငွေထုတ် ပိုမိုချောမွေ့\n• အကောင်းဆုံးစွမ်းဆောင်ရည်အတွက် ယခုပဲ Update တင်လိုက်ပါ!"
            }
        }
        when {
            fsNewer && fsCfg != null -> {
                val lbl = "v${fsCfg.latestVersionName} (Build ${fsCfg.latestVersionCode})"
                AvailableApkUpdate(
                    versionLabel = lbl,
                    releaseNotes = sanitizeReleaseNotes(fsCfg.releaseNotes, lbl),
                    downloadUrl = com.example.data.network.ApkDownloadHelper.toDirectDownloadUrl(
                        fsCfg.downloadUrl.ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL }
                    ),
                    forceUpdate = fsCfg.forceUpdate,
                    source = "Admin Push"
                )
            }
            ghNewer && ghRel != null -> {
                val lbl = if (ghRel.tagName.equals("latest", ignoreCase = true) || ghRel.tagName.isBlank()) {
                    "ဗားရှင်းအသစ်"
                } else {
                    "ဗားရှင်း ${ghRel.tagName}"
                }
                AvailableApkUpdate(
                    versionLabel = lbl,
                    releaseNotes = sanitizeReleaseNotes(ghRel.releaseNotes, lbl),
                    downloadUrl = com.example.data.network.ApkDownloadHelper.toDirectDownloadUrl(ghRel.downloadUrl),
                    forceUpdate = false,
                    source = "Auto Update"
                )
            }
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val isForceLoggedOut: StateFlow<Boolean> = combine(
        userAccount,
        adminMember,
        securityConfig
    ) { accState, member, secCfg ->
        val acc = (accState as? UiState.Success)?.data
        val userForced = (acc?.forceLogoutAtMs ?: 0L) > sessionStartedAtMs
        val adminForced = (member?.forceLogoutAtMs ?: 0L) > sessionStartedAtMs
        val globalForced = secCfg.globalForceLogoutAtMs > sessionStartedAtMs && !isBootstrappedOwner
        userForced || adminForced || globalForced
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val userDirectMessages: StateFlow<UiState<List<DirectChatMessage>>> =
        repository.observeDirectMessagesForUser(currentUserId)
            .toUiState("Failed to load direct messages")

    val allDirectMessages: StateFlow<UiState<List<DirectChatMessage>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAllDirectMessages(currentUserId)
                .map<List<DirectChatMessage>, UiState<List<DirectChatMessage>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load chat threads"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val groupMessages: StateFlow<UiState<List<GroupChatMessage>>> =
        repository.observeGroupMessages(currentUserId)
            .toUiState("Failed to load group chat")

    val appReviews: StateFlow<UiState<List<AppQualityReview>>> =
        repository.observeAppReviews(currentUserId)
            .toUiState("Failed to load quality ratings")

    // Admin-only live streams
    val allOrders: StateFlow<UiState<List<BetOrder>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAllOrders(currentUserId).map<List<BetOrder>, UiState<List<BetOrder>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load all orders"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allWalletRequests: StateFlow<UiState<List<WalletRequest>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAllWalletRequests(currentUserId).map<List<WalletRequest>, UiState<List<WalletRequest>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load wallet requests"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allUsers: StateFlow<UiState<List<UserAccount>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAllUsers(currentUserId).map<List<UserAccount>, UiState<List<UserAccount>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load customers"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val adminMembers: StateFlow<UiState<List<AdminMember>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAdminMembers(currentUserId).map<List<AdminMember>, UiState<List<AdminMember>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load admin team"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    // Strictly Owner Admin list permission on admin_invites
    val adminInvites: StateFlow<UiState<List<AdminInvite>>> = isOwnerAdmin
        .flatMapLatest { owner ->
            if (!owner) flowOf(UiState.Success(emptyList()))
            else repository.observeAdminInvites(currentUserId).map<List<AdminInvite>, UiState<List<AdminInvite>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load invite codes"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val auditLogs: StateFlow<UiState<List<AuditLogEntry>>> = combine(
        isAdmin,
        isOwnerAdmin,
        currentAdminPermissions.map { it.canViewAllLogs }.distinctUntilChanged()
    ) { admin, owner, canViewAll ->
        if (!admin) null else (owner || canViewAll)
    }.flatMapLatest { canViewAllOpt ->
        if (canViewAllOpt == null) flowOf(UiState.Success(emptyList()))
        else repository.observeAuditLogs(currentUserId, canViewAll = canViewAllOpt)
            .map<List<AuditLogEntry>, UiState<List<AuditLogEntry>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load audit logs"))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allMiniGameRounds: StateFlow<UiState<List<MiniGameRound>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAllMiniGameRounds(currentUserId)
                .map<List<MiniGameRound>, UiState<List<MiniGameRound>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load mini-game rounds"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val allTransactions: StateFlow<UiState<List<LedgerTransaction>>> = isOwnerAdmin
        .flatMapLatest { owner ->
            if (!owner) flowOf(UiState.Success(emptyList()))
            else repository.observeAllTransactions(currentUserId)
                .map<List<LedgerTransaction>, UiState<List<LedgerTransaction>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load ledger transactions"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val pendingAdminBadgeCount: StateFlow<Int> = combine(
        allOrders,
        allWalletRequests,
        isOwnerAdmin
    ) { ordState, reqState, owner ->
        val pendingOrders = (ordState as? UiState.Success)?.data?.count { it.status == "pending" } ?: 0
        val pendingReqs = (reqState as? UiState.Success)?.data?.count {
            it.status == "pending" || (owner && it.status == "awaiting_main_approval")
        } ?: 0
        pendingOrders + pendingReqs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    private val _activeCall = MutableStateFlow<ActiveCallUiState?>(null)
    val activeCall: StateFlow<ActiveCallUiState?> = _activeCall.asStateFlow()

    private val _dismissedCallIds = MutableStateFlow<Set<String>>(emptySet())
    private val callCandidateJobs = mutableListOf<Job>()
    private val processedCandidateIds = mutableSetOf<String>()
    private val pendingLocalIceCandidates = mutableListOf<Triple<String, Int, String>>()
    private val callSyncLock = Any()
    @Volatile
    private var canSendLocalIce = false

    private val seenNotificationMsgIds = mutableSetOf<String>()
    private var initialDirectWarmupDone = false
    private var initialAdminDirectWarmupDone = false
    private var initialGroupWarmupDone = false

    private val callSessionsStream: Flow<List<CallSession>> = isAdmin
        .flatMapLatest { admin ->
            val flow = if (admin) {
                repository.observeAllCallSessions(currentUserId)
            } else {
                repository.observeCallSessionsForUser(currentUserId)
            }
            flow.catch { error ->
                if (isPureCancellation(error)) throw error
                Log.w(TAG, "Call sessions stream error", error)
                emit(emptyList())
            }
        }

    val incomingCall: StateFlow<CallSession?> = combine(
        callSessionsStream,
        _activeCall,
        _dismissedCallIds
    ) { sessions, active, dismissed ->
        if (active != null) return@combine null
        val cutoffEpochMs = System.currentTimeMillis() - 300_000L
        sessions.firstOrNull { session ->
            session.status == "ringing" &&
                session.callerId != currentUserId &&
                !dismissed.contains(session.id) &&
                ((session.createdAt?.seconds?.times(1000L)) ?: Long.MAX_VALUE) >= cutoffEpochMs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        viewModelScope.launch {
            val result = repository.ensureUserInitialized(
                userId = currentUserId,
                rawDisplayName = displayName,
                rawEmail = currentUserEmail ?: ""
            )
            result.onFailure { err ->
                _events.emit(err.localizedMessage ?: "Account setup error")
            }
        }
        refreshLiveFootballFeed()
        refreshLive2DStatus()
        refreshPublicIp()
        checkForApkUpdate()

        viewModelScope.launch {
            combine(isAdmin, boardConfig, matches) { admin, cfg, matchState ->
                Triple(admin, cfg, matchState)
            }.collect { (admin, cfg, matchState) ->
                if (!admin) return@collect
                val loadedMatches = (matchState as? UiState.Success)?.data ?: return@collect
                val today = feedService.todayDateIso()
                val canManageBoard = currentAdminPermissions.value.canManageBoard
                if (!canManageBoard) return@collect

                if (!autoSyncedSession) {
                    autoSyncedSession = true
                    if (cfg.autoSyncDailyMatches && cfg.lastSyncedDate != today) {
                        repository.syncDailyMatchesFromFeed(feedService, listOf(0, 1), cfg)
                    } else if (cfg.autoSyncLiveResults) {
                        repository.syncLiveMatchResults(feedService)
                    }
                }
                if (!autoSyncedOneExpressSession && cfg.autoSyncOneExpress) {
                    autoSyncedOneExpressSession = true
                    val hasOneExpressBoard = loadedMatches.any {
                        it.boardSource == "one_express" && (it.matchDate == today || it.status == "open")
                    }
                    val hasNonStandardOneXBetOdds = loadedMatches.any {
                        it.boardSource == "one_express" &&
                            it.status == "open" &&
                            it.externalId.startsWith("1xbet_") &&
                            (it.bodyOdds != com.example.data.betting.KyayFormatter.LOTAYA_STANDARD_BODY_ODDS ||
                                it.goalOdds != com.example.data.betting.KyayFormatter.LOTAYA_STANDARD_GOAL_ODDS)
                    }
                    if (!hasOneExpressBoard || cfg.lastSyncedDate != today || hasNonStandardOneXBetOdds) {
                        repository.syncBoardMatchesFromFeed("one_express", feedService, listOf(0, 1), cfg)
                    }
                }
                if (!autoSyncedFiveFiveFiveSession && cfg.autoSyncFiveFiveFive) {
                    autoSyncedFiveFiveFiveSession = true
                    val has555Board = loadedMatches.any {
                        it.boardSource == "five_five_five" && (it.matchDate == today || it.status == "open")
                    }
                    if (!has555Board || cfg.lastSyncedDate != today) {
                        repository.syncBoardMatchesFromFeed("five_five_five", feedService, listOf(0, 1), cfg)
                    }
                }
            }
        }

        // Periodic Admin background live-result closing/update & auto-settle loop
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(180_000L)
                if (isAdmin.value && currentAdminPermissions.value.canManageBoard && boardConfig.value.autoSyncLiveResults) {
                    repository.syncLiveMatchResults(feedService)
                    if (bettingConfig.value.autoSettleOrders && currentAdminPermissions.value.canManageOrders) {
                        repository.autoSettleFinishedOrders(bettingConfig.value)
                    }
                }
            }
        }

        // Chatbox local notification watchers
        if (appContext != null) {
            ChatNotificationHelper.ensureChannel(appContext)
            viewModelScope.launch {
                userDirectMessages.collect { state ->
                    val list = (state as? UiState.Success)?.data ?: return@collect
                    if (!initialDirectWarmupDone) {
                        list.forEach { seenNotificationMsgIds.add("dm_${it.id}") }
                        initialDirectWarmupDone = true
                    } else {
                        list.forEach { msg ->
                            val key = "dm_${msg.id}"
                            if (seenNotificationMsgIds.add(key) && msg.senderId != currentUserId) {
                                ChatNotificationHelper.showIncomingChatNotification(
                                    context = appContext,
                                    title = "${msg.senderName} • Admin",
                                    body = msg.text,
                                    notificationId = msg.id.hashCode()
                                )
                            }
                        }
                    }
                }
            }
            viewModelScope.launch {
                allDirectMessages.collect { state ->
                    if (!isAdmin.value) return@collect
                    val list = (state as? UiState.Success)?.data ?: return@collect
                    if (!initialAdminDirectWarmupDone) {
                        list.forEach { seenNotificationMsgIds.add("adm_${it.id}") }
                        initialAdminDirectWarmupDone = true
                    } else {
                        list.forEach { msg ->
                            val key = "adm_${msg.id}"
                            if (seenNotificationMsgIds.add(key) && msg.senderId != currentUserId && msg.senderRole == "customer") {
                                ChatNotificationHelper.showIncomingChatNotification(
                                    context = appContext,
                                    title = "Customer: ${msg.senderName}",
                                    body = msg.text,
                                    notificationId = ("adm_" + msg.id).hashCode()
                                )
                            }
                        }
                    }
                }
            }
            viewModelScope.launch {
                groupMessages.collect { state ->
                    val list = (state as? UiState.Success)?.data ?: return@collect
                    if (!initialGroupWarmupDone) {
                        list.forEach { seenNotificationMsgIds.add("gm_${it.id}") }
                        initialGroupWarmupDone = true
                    } else {
                        list.forEach { msg ->
                            val key = "gm_${msg.id}"
                            if (seenNotificationMsgIds.add(key) && msg.senderId != currentUserId) {
                                val prefix = if (msg.senderRole == "admin") "${msg.senderName} (Admin)" else msg.senderName
                                ChatNotificationHelper.showIncomingChatNotification(
                                    context = appContext,
                                    title = "Group Chat • $prefix",
                                    body = msg.text,
                                    notificationId = ("gm_" + msg.id).hashCode()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun setAppLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
    }

    fun updateOnlinePresence(isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateOnlinePresence(isOnline)
        }
    }

    fun updateCustomerSecurityPin(rawPin: String, onSuccess: () -> Unit = {}) {
        val clean = rawPin.trim()
        if (clean.isNotBlank() && clean.length !in 4..8) {
            emitMessage("PIN သည် ဂဏန်း 4 လုံးမှ 8 လုံးအတွင်း ဖြစ်ရပါမည်")
            return
        }
        val hash = if (clean.isBlank()) "" else sha256(clean)
        runAction(if (clean.isBlank()) "လုံခြုံရေး PIN ဖယ်ရှားပြီးပါပြီ" else "လုံခြုံရေး PIN သတ်မှတ်ပြီးပါပြီ") {
            val res = repository.updateCustomerSecurityPin(hash)
            if (res.isSuccess) onSuccess()
            res
        }
    }

    fun verifyAdminPin(rawPin: String): Boolean {
        val expectedHash = adminMember.value?.pinHash.orEmpty()
        if (expectedHash.isBlank()) {
            _adminPinVerified.value = true
            return true
        }
        val ok = sha256(rawPin.trim()) == expectedHash
        if (ok) _adminPinVerified.value = true
        else emitMessage("Admin PIN မှားယွင်းနေပါသည်")
        return ok
    }

    fun isIpAllowedForAdmin(): Boolean {
        val sec = securityConfig.value
        if (!sec.ipWhitelistEnabled || sec.allowedIps.isEmpty()) return true
        if (isBootstrappedOwner) return true
        val myIp = _currentPublicIp.value ?: return false
        return sec.allowedIps.any { it.trim() == myIp.trim() }
    }

    fun needsAdminPinPrompt(): Boolean {
        if (!securityConfig.value.requireAdminPin) return false
        val expectedHash = adminMember.value?.pinHash.orEmpty()
        if (expectedHash.isBlank()) return false
        return !_adminPinVerified.value
    }

    fun refreshPublicIp() {
        viewModelScope.launch {
            _currentPublicIp.value = feedService.fetchPublicIpAddress()
        }
    }

    fun refreshLive2DStatus() {
        viewModelScope.launch {
            _live2DStatus.value = feedService.fetchLive2DResult()
        }
    }

    fun checkForApkUpdate() {
        viewModelScope.launch {
            _githubRelease.value = feedService.fetchLatestGitHubApkRelease()
        }
    }

    fun dismissUpdateDialog() {
        _updateDismissed.value = true
    }

    fun refreshLiveFootballFeed() {
        viewModelScope.launch {
            _liveFeed.value = UiState.Loading
            val res = feedService.fetchFixturesForDays(
                dayOffsets = listOf(-1, 0, 1),
                allowedLeagues = boardConfig.value.allowedLeagues
            )
            res.onSuccess { fixtures ->
                _liveFeed.value = UiState.Success(fixtures)
            }.onFailure { err ->
                _liveFeed.value = UiState.Error(err.localizedMessage ?: "တိုက်ရိုက်ရလဒ် ဆွဲယူမရပါ")
            }
        }
    }

    fun setAdminMode(enabled: Boolean) {
        if (enabled) {
            if (!isAdmin.value) {
                val member = adminMember.value
                if (member != null && !member.isActiveMember()) {
                    emitMessage("သင့် Second Admin အကောင့်အား Main Admin မှ ယာယီပိတ် (Suspend) ထားပါသည်")
                } else {
                    emitMessage("Admin ခွင့်ပြုချက် မရှိသေးပါ")
                }
                return
            }
            refreshPublicIp()
            if (!isIpAllowedForAdmin()) {
                emitMessage("IP Whitelist ကန့်သတ်ချက်ကြောင့် ဤ IP (${_currentPublicIp.value ?: "Unknown"}) မှ Admin Panel ဝင်ခွင့်မရှိပါ")
                return
            }
        }
        _isAdminMode.value = enabled
    }

    fun refreshAllData() {
        viewModelScope.launch {
            _busy.value = true
            try {
                repository.ensureUserInitialized(
                    userId = currentUserId,
                    rawDisplayName = displayName,
                    rawEmail = currentUserEmail ?: ""
                )
                val feedRes = feedService.fetchFixturesForDays(
                    dayOffsets = listOf(-1, 0, 1),
                    allowedLeagues = boardConfig.value.allowedLeagues
                )
                feedRes.onSuccess { fixtures ->
                    _liveFeed.value = UiState.Success(fixtures)
                }
                _live2DStatus.value = feedService.fetchLive2DResult()
                _currentPublicIp.value = feedService.fetchPublicIpAddress()
                _githubRelease.value = feedService.fetchLatestGitHubApkRelease()
                var msg = "ဒေတာအသစ်များ Refresh လုပ်ပြီးပါပြီ"
                if (isAdmin.value) {
                    var syncedMatches = 0
                    if (boardConfig.value.autoSyncLiveResults && currentAdminPermissions.value.canManageBoard) {
                        syncedMatches = repository.syncLiveMatchResults(feedService).getOrDefault(0)
                    }
                    if (bettingConfig.value.autoSettleOrders && currentAdminPermissions.value.canManageOrders) {
                        val settled = repository.autoSettleFinishedOrders(bettingConfig.value)
                            .getOrNull()?.settledCount ?: 0
                        if (syncedMatches > 0 || settled > 0) {
                            msg = "Refresh ပြီးပါပြီ (ရလဒ် $syncedMatches ပွဲ၊ အလိုအလျောက်ရှင်း $settled ခု)"
                        }
                    } else if (syncedMatches > 0) {
                        msg = "Refresh ပြီးပါပြီ (တိုက်ရိုက်ရလဒ် $syncedMatches ပွဲ အပ်ဒိတ်)"
                    }
                }
                _events.emit(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(e.localizedMessage ?: "Refresh မအောင်မြင်ပါ")
            } finally {
                _busy.value = false
            }
        }
    }

    // --- CUSTOMER ORDERS & MINI-GAMES ---

    private fun formatAutoReopenNotice(reopenAtMs: Long, prefix: String): String {
        if (reopenAtMs <= 0L) return "$prefix ခေတ္တပိတ်ထားပါသည်"
        val remSec = ((reopenAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        val hrs = remSec / 3600L
        val mins = (remSec % 3600L) / 60L
        val secs = remSec % 60L
        val hhmm = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Yangon")
        }.format(java.util.Date(reopenAtMs))
        val countdown = if (hrs > 0) "${hrs}နာရီ ${mins}မိနစ်" else "${mins}မိနစ် ${secs}စက္ကန့်"
        return "$prefix ခေတ္တပိတ်ထားပါသည် ($hhmm တွင် အလိုအလျောက် ပြန်ဖွင့်မည် • $countdown ကျန်)"
    }

    fun placeOrder(
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        pickKeys: List<String> = emptyList(),
        matchKickoffMs: Long = 0L,
        scanAtMs: Long = 0L,
        matchKickoffText: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val sys = systemGameConfig.value
        val nowMs = System.currentTimeMillis()
        if (sys.maintenanceMode) {
            emitMessage(sys.maintenanceMessage.ifBlank { "စနစ်ပြုပြင်နေပါသည်" })
            return
        }
        val isFootballOrder = orderType in listOf("parlay", "body_goal", "one_express", "five_five_five")
        if (isFootballOrder && sys.isFootballPausedNow(nowMs)) {
            emitMessage(formatAutoReopenNotice(sys.footballReopenAtMs, "ဘောလုံးပွဲ လောင်းကြေး"))
            return
        }
        val isLotteryOrder = orderType in listOf("two_d", "three_d")
        if (isLotteryOrder && sys.isGamesPausedNow(nowMs)) {
            emitMessage(formatAutoReopenNotice(sys.gamesReopenAtMs, "ဂိမ်းနှင့် ထီကစားခွင့်"))
            return
        }
        val acc = (userAccount.value as? UiState.Success)?.data
        if (acc?.isLocked == true) {
            emitMessage("သင့်အကောင့် ယာယီပိတ်ထားပါသဖြင့် လောင်းကြေးတင်၍ မရပါ")
            return
        }
        if (stakeKs < sys.minStakeKs || stakeKs > sys.maxStakeKs) {
            emitMessage("လောင်းကြေးသည် ${sys.minStakeKs} Ks မှ ${sys.maxStakeKs} Ks အတွင်း ဖြစ်ရပါမည်")
            return
        }
        var effectiveKickoffMs = matchKickoffMs
        var effectiveScanAtMs = scanAtMs
        if (isFootballOrder && matchIds.isNotEmpty()) {
            val loadedMatches = (matches.value as? UiState.Success)?.data.orEmpty().associateBy { it.id }
            for (mId in matchIds.distinct()) {
                val m = loadedMatches[mId]
                if (m != null && !MyanmarTimeUtil.isMatchBettable(m, nowMs)) {
                    emitMessage("${m.homeTeam} vs ${m.awayTeam} ပွဲစတင်ပြီး ဖြစ်သဖြင့် လောင်း၍မရပါ")
                    return
                }
            }
            if (effectiveKickoffMs <= 0L) {
                val computed = matchIds.mapNotNull { loadedMatches[it] }
                    .map { MyanmarTimeUtil.resolveMatchKickoffEpochMs(it, nowMs) }
                    .filter { it > 0L }
                    .minOrNull() ?: 0L
                if (computed > 0L) {
                    effectiveKickoffMs = computed
                    if (effectiveScanAtMs <= 0L) {
                        effectiveScanAtMs = computed + (135L * 60_000L)
                    }
                }
            }
        }
        val autoAcc = bettingConfig.value.autoAcceptOrders
        val successMsg = if (autoAcc) {
            "အော်ဒါတင်ပြီး အလိုအလျောက် လက်ခံအတည်ပြုပြီးပါပြီ"
        } else {
            "အော်ဒါတင်ပြီးပါပြီ (Admin စစ်ဆေးပေးပါမည်)"
        }
        runAction(successMsg) {
            val name = acc?.displayName ?: displayName
            val res = repository.placeOrder(
                customerName = name,
                orderType = orderType,
                selections = selections,
                matchIds = matchIds,
                drawRound = drawRound,
                stakeKs = stakeKs,
                multiplier = multiplier,
                pickKeys = pickKeys,
                autoAccept = autoAcc,
                matchKickoffMs = effectiveKickoffMs,
                scanAtMs = effectiveScanAtMs,
                matchKickoffText = matchKickoffText
            )
            if (res.isSuccess) onSuccess()
            res.map { }
        }
    }

    private fun todayStartEpochSec(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yangon"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000L
    }

    private fun todaysMiniGameRounds(): List<MiniGameRound> {
        val startSec = todayStartEpochSec()
        val adminRounds = (allMiniGameRounds.value as? UiState.Success)?.data.orEmpty()
        val userRounds = (userMiniGameRounds.value as? UiState.Success)?.data.orEmpty()
        return (adminRounds + userRounds)
            .distinctBy { it.id }
            .filter { (it.createdAt?.seconds ?: Long.MAX_VALUE) >= startSec }
    }

    fun playDiceGame(
        betOption: String,
        exactNumber: Int,
        stakeKs: Long,
        onResult: (Result<Pair<List<Int>, MiniGameRound>>) -> Unit
    ) {
        val sys = systemGameConfig.value
        val nowMs = System.currentTimeMillis()
        if (sys.maintenanceMode || !sys.isGameEnabled("dice")) {
            onResult(Result.failure(IllegalStateException("အံစာတုံးဂိမ်း ခေတ္တပိတ်ထားပါသည်")))
            return
        }
        if (sys.isGamesPausedNow(nowMs)) {
            onResult(Result.failure(IllegalStateException(formatAutoReopenNotice(sys.gamesReopenAtMs, "ဂိမ်းကစားခွင့်"))))
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val ctrl = miniGameControlConfig.value
                val normOpt = betOption.trim().lowercase()
                val stdMult = ctrl.diceStandardMult.coerceIn(1.1, 50.0)
                val tripleMult = ctrl.diceTripleMult.coerceIn(1.1, 100.0)
                val exactMult = ctrl.diceExactMult.coerceIn(1.1, 100.0)

                val todayDiceRounds = todaysMiniGameRounds().filter { it.gameType == "dice" }
                var bigHits = 0L
                var smallHits = 0L
                var oddHits = 0L
                var evenHits = 0L
                val totalHits = mutableMapOf<Int, Long>()
                val sumRegex = Regex("""=\s*(\d+)""")
                for (r in todayDiceRounds) {
                    val sum = sumRegex.find(r.outcomeLabel)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: continue
                    val wasTriple = r.outcomeLabel.contains("Triple", ignoreCase = true)
                    totalHits[sum] = (totalHits[sum] ?: 0L) + 1L
                    if (!wasTriple) {
                        if (sum in 11..17) bigHits++
                        else if (sum in 4..10) smallHits++
                        if (sum % 2 == 1) oddHits++ else evenHits++
                    }
                }

                fun computeDiceOutcome(d1: Int, d2: Int, d3: Int): Pair<String, Long> {
                    val sum = d1 + d2 + d3
                    val isTriple = (d1 == d2 && d2 == d3)
                    return when (normOpt) {
                        "big" -> "ကြီး (11-17)" to if (!isTriple && sum in 11..17) (stakeKs * stdMult).roundToLong() else 0L
                        "small" -> "သေး (4-10)" to if (!isTriple && sum in 4..10) (stakeKs * stdMult).roundToLong() else 0L
                        "odd" -> "မ (Odd)" to if (!isTriple && sum % 2 == 1) (stakeKs * stdMult).roundToLong() else 0L
                        "even" -> "စုံ (Even)" to if (!isTriple && sum % 2 == 0) (stakeKs * stdMult).roundToLong() else 0L
                        "triple" -> "သုံးလုံးတူ (Triple)" to if (isTriple) (stakeKs * tripleMult).roundToLong() else 0L
                        "exact" -> "ပေါင်းရလဒ် $exactNumber" to if (sum == exactNumber) (stakeKs * exactMult).roundToLong() else 0L
                        else -> "ကြီး (11-17)" to if (!isTriple && sum in 11..17) (stakeKs * stdMult).roundToLong() else 0L
                    }
                }

                fun isQuotaOk(d1: Int, d2: Int, d3: Int): Boolean {
                    val sum = d1 + d2 + d3
                    val isTriple = (d1 == d2 && d2 == d3)
                    if (ctrl.diceAllowedTotals.isNotEmpty() && sum !in ctrl.diceAllowedTotals) return false
                    val sumCap = ctrl.diceTotalDailyQuotas["$sum"] ?: 0L
                    if (sumCap > 0L && (totalHits[sum] ?: 0L) >= sumCap) return false
                    if (!isTriple) {
                        if (sum in 11..17 && ctrl.diceMaxBigPerDay > 0L && bigHits >= ctrl.diceMaxBigPerDay) return false
                        if (sum in 4..10 && ctrl.diceMaxSmallPerDay > 0L && smallHits >= ctrl.diceMaxSmallPerDay) return false
                        if (sum % 2 == 1 && ctrl.diceMaxOddPerDay > 0L && oddHits >= ctrl.diceMaxOddPerDay) return false
                        if (sum % 2 == 0 && ctrl.diceMaxEvenPerDay > 0L && evenHits >= ctrl.diceMaxEvenPerDay) return false
                    }
                    return true
                }

                val allCombos = buildList {
                    for (a in 1..6) for (b in 1..6) for (c in 1..6) add(Triple(a, b, c))
                }
                val directive = ctrl.userDirectives[currentUserId] ?: ctrl.globalDirective
                val quotaCombos = allCombos.filter { (a, b, c) -> isQuotaOk(a, b, c) }
                val recentCustomerDice = ((userMiniGameRounds.value as? UiState.Success)?.data ?: emptyList())
                    .filter { it.gameType == "dice" }
                val chosen = com.example.data.betting.MiniGameEngine.selectDiceRoll(
                    allCombos = allCombos,
                    quotaCombos = quotaCombos,
                    directive = directive,
                    stealthHouseWinPct = ctrl.stealthHouseWinPct,
                    betOption = betOption,
                    exactNumber = exactNumber,
                    stakeKs = stakeKs,
                    recentCustomerRounds = recentCustomerDice,
                    computePayout = { a, b, c -> computeDiceOutcome(a, b, c).second }
                )
                val d1 = chosen.first
                val d2 = chosen.second
                val d3 = chosen.third
                val dice = listOf(d1, d2, d3)
                val sum = d1 + d2 + d3
                val isTriple = (d1 == d2 && d2 == d3)
                val (label, rawPayout) = computeDiceOutcome(d1, d2, d3)
                val payoutKs = rawPayout.coerceIn(0L, stakeKs * 100L)

                val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
                val outcomeLabel = "$d1 + $d2 + $d3 = $sum" + if (isTriple) " (Triple!)" else ""
                val res = repository.playMiniGameRound(
                    customerName = name,
                    gameType = "dice",
                    betLabel = label,
                    outcomeLabel = outcomeLabel,
                    stakeKs = stakeKs,
                    payoutKs = payoutKs
                )
                onResult(res.map { dice to it })
            } finally {
                _busy.value = false
            }
        }
    }

    fun playSixAnimalsGame(
        selectedAnimalIndices: Set<Int>,
        animalLabels: List<String>,
        stakePerAnimalKs: Long,
        onResult: (Result<Pair<List<Int>, MiniGameRound>>) -> Unit
    ) {
        val sys = systemGameConfig.value
        val nowMs = System.currentTimeMillis()
        if (sys.maintenanceMode || !sys.isGameEnabled("six_animals")) {
            onResult(Result.failure(IllegalStateException("၆ကောင်ဂျင် ခေတ္တပိတ်ထားပါသည်")))
            return
        }
        if (sys.isGamesPausedNow(nowMs)) {
            onResult(Result.failure(IllegalStateException(formatAutoReopenNotice(sys.gamesReopenAtMs, "ဂိမ်းကစားခွင့်"))))
            return
        }
        val cleanSelection = selectedAnimalIndices.filter { it in 0..5 }.toSet()
        if (cleanSelection.isEmpty()) {
            onResult(Result.failure(IllegalStateException("အကောင် အနည်းဆုံး ၁ ကောင် ရွေးချယ်ပါ")))
            return
        }
        val totalStakeKs = stakePerAnimalKs * cleanSelection.size
        if (totalStakeKs !in 100L..1_000_000L) {
            onResult(Result.failure(IllegalStateException("စုစုပေါင်းထိုးကြေးသည် 100 Ks မှ 1,000,000 Ks အတွင်း ဖြစ်ရပါမည်")))
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val ctrl = miniGameControlConfig.value
                val animalKeys = listOf("deer", "tiger", "elephant", "rooster", "prawn", "fish")
                val todayAnimalRounds = todaysMiniGameRounds().filter { it.gameType == "six_animals" }
                val hitsPerIdx = IntArray(6)
                for (r in todayAnimalRounds) {
                    val tokens = r.outcomeLabel.split("•").map { it.trim() }
                    for (tok in tokens) {
                        val idx = animalLabels.indexOfFirst { lbl ->
                            lbl.isNotBlank() && tok.contains(lbl.takeWhile { it != ' ' }, ignoreCase = true)
                        }
                        if (idx in 0..5) hitsPerIdx[idx]++
                    }
                }

                fun isComboWithinQuota(r1: Int, r2: Int, r3: Int): Boolean {
                    val roll = listOf(r1, r2, r3)
                    for (idx in 0..5) {
                        val cap = ctrl.animalDailyQuotas[animalKeys[idx]] ?: 0L
                        if (cap > 0L) {
                            val add = roll.count { it == idx }
                            if (add > 0 && hitsPerIdx[idx] + add > cap) return false
                        }
                    }
                    return true
                }

                val allCombos = buildList {
                    for (a in 0..5) for (b in 0..5) for (c in 0..5) add(Triple(a, b, c))
                }
                val quotaCombos = allCombos.filter { (a, b, c) -> isComboWithinQuota(a, b, c) }
                val directive = ctrl.userDirectives[currentUserId] ?: ctrl.globalDirective
                val recentCustomerAnimals = ((userMiniGameRounds.value as? UiState.Success)?.data ?: emptyList())
                    .filter { it.gameType == "six_animals" }
                val baseBonus = (ctrl.sixAnimalsMult - 1.0).coerceIn(0.2, 29.0)
                val chosen = com.example.data.betting.MiniGameEngine.selectSixAnimalsRollMultiPick(
                    allCombos = allCombos,
                    quotaCombos = quotaCombos,
                    selectedAnimalIndices = cleanSelection,
                    stakePerAnimalKs = stakePerAnimalKs,
                    baseBonus = baseBonus,
                    directive = directive,
                    stealthHouseWinPct = ctrl.stealthHouseWinPct,
                    recentCustomerRounds = recentCustomerAnimals
                )
                val rolled = listOf(chosen.first, chosen.second, chosen.third)
                val rawPayout = com.example.data.betting.MiniGameEngine.computeSixAnimalsMultiPickPayout(
                    roll = chosen,
                    selectedAnimalIndices = cleanSelection,
                    stakePerAnimalKs = stakePerAnimalKs,
                    baseBonus = baseBonus
                )
                val payoutKs = rawPayout.coerceIn(0L, totalStakeKs * 100L)

                val pickedName = cleanSelection.sorted().joinToString(" + ") { idx ->
                    animalLabels.getOrElse(idx) { "Pick #${idx + 1}" }
                }.take(190)
                val outcomeNames = rolled.joinToString(" • ") { idx -> animalLabels.getOrElse(idx) { "#$idx" } }
                val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName

                val res = repository.playMiniGameRound(
                    customerName = name,
                    gameType = "six_animals",
                    betLabel = pickedName,
                    outcomeLabel = outcomeNames,
                    stakeKs = totalStakeKs,
                    payoutKs = payoutKs
                )
                onResult(res.map { rolled to it })
            } finally {
                _busy.value = false
            }
        }
    }

    fun playDragonTigerGame(
        pick: String, // "DRAGON", "TIGER", "TIE"
        stakeKs: Long,
        onResult: (Result<Pair<com.example.data.betting.MiniGameEngine.DragonTigerRoll, MiniGameRound>>) -> Unit
    ) {
        val sys = systemGameConfig.value
        val nowMs = System.currentTimeMillis()
        if (sys.maintenanceMode || !sys.isGameEnabled("dragon_tiger")) {
            onResult(Result.failure(IllegalStateException("နဂါး vs ကျား ဂိမ်း ခေတ္တပိတ်ထားပါသည်")))
            return
        }
        if (sys.isGamesPausedNow(nowMs)) {
            onResult(Result.failure(IllegalStateException(formatAutoReopenNotice(sys.gamesReopenAtMs, "ဂိမ်းကစားခွင့်"))))
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val ctrl = miniGameControlConfig.value
                val normPick = pick.trim().uppercase()
                val directive = ctrl.userDirectives[currentUserId] ?: ctrl.globalDirective
                val recentCustomerDt = ((userMiniGameRounds.value as? UiState.Success)?.data ?: emptyList())
                    .filter { it.gameType == "dragon_tiger" }
                val roll = com.example.data.betting.MiniGameEngine.selectDragonTigerOutcome(
                    pick = normPick,
                    directive = directive,
                    stealthHouseWinPct = ctrl.stealthHouseWinPct,
                    stakeKs = stakeKs,
                    recentCustomerRounds = recentCustomerDt
                )
                val winner = when {
                    roll.dragonRank > roll.tigerRank -> "DRAGON"
                    roll.tigerRank > roll.dragonRank -> "TIGER"
                    else -> "TIE"
                }
                val dtMult = ctrl.dragonTigerMult.coerceIn(1.1, 50.0)
                val payoutKs = when {
                    normPick == "TIE" && winner == "TIE" -> (stakeKs * 8.0).roundToLong()
                    normPick == winner -> (stakeKs * dtMult).roundToLong()
                    else -> 0L
                }.coerceIn(0L, stakeKs * 100L)

                fun cardText(rank: Int, suit: String): String {
                    val rLabel = when (rank) {
                        1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> rank.toString()
                    }
                    return "$rLabel$suit"
                }
                val betLabel = when (normPick) {
                    "DRAGON" -> "နဂါး (x$dtMult)"
                    "TIGER" -> "ကျား (x$dtMult)"
                    else -> "သရေ Tie (x8)"
                }
                val winText = when (winner) {
                    "DRAGON" -> "နဂါးနိုင်"
                    "TIGER" -> "ကျားနိုင်"
                    else -> "သရေ"
                }
                val outcomeLabel = "🐉 ${cardText(roll.dragonRank, roll.dragonSuit)} vs 🐯 ${cardText(roll.tigerRank, roll.tigerSuit)} • $winText"
                val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName

                val res = repository.playMiniGameRound(
                    customerName = name,
                    gameType = "dragon_tiger",
                    betLabel = betLabel,
                    outcomeLabel = outcomeLabel,
                    stakeKs = stakeKs,
                    payoutKs = payoutKs
                )
                onResult(res.map { roll to it })
            } finally {
                _busy.value = false
            }
        }
    }

    fun playLuckyWheelGame(
        stakeKs: Long,
        onResult: (Result<Pair<Int, MiniGameRound>>) -> Unit
    ) {
        val sys = systemGameConfig.value
        val nowMs = System.currentTimeMillis()
        if (sys.maintenanceMode || !sys.isGameEnabled("lucky_wheel")) {
            onResult(Result.failure(IllegalStateException("ကံစမ်းဘီးလှည့် ဂိမ်း ခေတ္တပိတ်ထားပါသည်")))
            return
        }
        if (sys.isGamesPausedNow(nowMs)) {
            onResult(Result.failure(IllegalStateException(formatAutoReopenNotice(sys.gamesReopenAtMs, "ဂိမ်းကစားခွင့်"))))
            return
        }
        viewModelScope.launch {
            _busy.value = true
            try {
                val ctrl = miniGameControlConfig.value
                val segments = com.example.data.betting.MiniGameEngine.buildLuckyWheelSegments(ctrl.luckyWheelMult)
                val directive = ctrl.userDirectives[currentUserId] ?: ctrl.globalDirective
                val recentCustomerWheel = ((userMiniGameRounds.value as? UiState.Success)?.data ?: emptyList())
                    .filter { it.gameType == "lucky_wheel" }
                val chosenIndex = com.example.data.betting.MiniGameEngine.selectLuckyWheelSegmentIndex(
                    segments = segments,
                    directive = directive,
                    stealthHouseWinPct = ctrl.stealthHouseWinPct,
                    stakeKs = stakeKs,
                    recentCustomerRounds = recentCustomerWheel
                )
                val seg = segments.getOrElse(chosenIndex) { segments.first() }
                val payoutKs = (stakeKs * seg.multiplier).roundToLong().coerceIn(0L, stakeKs * 100L)
                val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName

                val res = repository.playMiniGameRound(
                    customerName = name,
                    gameType = "lucky_wheel",
                    betLabel = "Spin 🎡 (${stakeKs} Ks)",
                    outcomeLabel = "🎯 ${seg.label}",
                    stakeKs = stakeKs,
                    payoutKs = payoutKs
                )
                onResult(res.map { seg.index to it })
            } finally {
                _busy.value = false
            }
        }
    }

    fun deleteOrder(orderId: String) = runAction("အော်ဒါမှတ်တမ်း ဖျက်ပြီးပါပြီ") {
        repository.deleteOrder(orderId)
    }

    fun clearCompletedOrders(orderIds: List<String>) = runAction("ပြီးဆုံးပြီးသား အော်ဒါများ ဖျက်ပြီးပါပြီ") {
        repository.clearCompletedOrders(orderIds).map { }
    }

    fun submitWalletRequest(
        requestType: String,
        amountKs: Long,
        channel: String,
        referenceNote: String,
        receiptImageBase64: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val acc = (userAccount.value as? UiState.Success)?.data
        if (acc?.isLocked == true) {
            emitMessage("သင့်အကောင့် ယာယီပိတ်ထားပါသဖြင့် ငွေသွင်း/ထုတ် ပြုလုပ်၍ မရပါ")
            return
        }
        runAction("တောင်းဆိုမှု ပေးပို့ပြီးပါပြီ") {
            val name = acc?.displayName ?: displayName
            val res = repository.submitWalletRequest(
                customerName = name,
                requestType = requestType,
                amountKs = amountKs,
                channel = channel,
                referenceNote = referenceNote,
                walletConfig = walletConfig.value,
                receiptImageBase64 = receiptImageBase64
            )
            if (res.isSuccess) onSuccess()
            res
        }
    }

    fun exchangePoints(points: Long) = runAction("ပွိုင့်လဲလှယ်ပြီးပါပြီ") {
        repository.exchangePoints(points).map { }
    }

    fun redeemAdminInvite(code: String, onSuccess: () -> Unit = {}) = runAction("Admin ဝင်ရောက်ခွင့် ရရှိပါပြီ") {
        val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
        val res = repository.redeemAdminInvite(name, code)
        if (res.isSuccess) {
            _isAdminMode.value = true
            onSuccess()
        }
        res
    }

    // --- ADMIN COMMANDS WITH AUDIT TRAIL ---

    private fun actorInfo(): Pair<String, String> {
        val role = if (isOwnerAdmin.value) "owner" else "admin"
        val name = adminMember.value?.displayName?.ifBlank { null } ?: displayName
        return name to role
    }

    private fun logAuditAsync(
        action: String,
        category: String,
        targetId: String = "",
        targetName: String = "",
        details: String = ""
    ) {
        val (actorName, actorRole) = actorInfo()
        viewModelScope.launch {
            repository.recordAuditLog(actorName, actorRole, action, category, targetId, targetName, details)
        }
    }

    fun acceptOrder(orderId: String, note: String) = runAction("အော်ဒါ လက်ခံအတည်ပြုပြီးပါပြီ") {
        val res = repository.acceptOrder(orderId, note)
        if (res.isSuccess) logAuditAsync("Accepted order", "order", orderId, "", note)
        res
    }

    fun settleOrderWon(
        order: BetOrder,
        note: String,
        customCommissionPercent: Double? = null,
        customGrossPayoutKs: Long? = null
    ) = runAction("အနိုင်ရ အော်ဒါ ငွေရှင်းပြီးပါပြီ") {
        val res = repository.settleOrderWon(order, note, bettingConfig.value, customCommissionPercent, customGrossPayoutKs)
        if (res.isSuccess) logAuditAsync("Settled order WON", "order", order.id, order.customerName, "Stake ${order.stakeKs} Ks")
        res
    }

    fun settleOrderLost(order: BetOrder, note: String) = runAction("အော်ဒါ စာရင်းပိတ်ပြီးပါပြီ") {
        val res = repository.settleOrderLost(order, note)
        if (res.isSuccess) logAuditAsync("Settled order LOST", "order", order.id, order.customerName, "Stake ${order.stakeKs} Ks")
        res
    }

    fun rejectOrder(order: BetOrder, note: String) = runAction("အော်ဒါ ပယ်ဖျက်ပြီး ငွေပြန်အမ်းပြီးပါပြီ") {
        val res = repository.rejectOrder(order, note)
        if (res.isSuccess) logAuditAsync("Rejected & refunded order", "order", order.id, order.customerName, note)
        res
    }

    fun processWalletRequest(req: WalletRequest, approve: Boolean, adminNote: String) {
        val isOwner = isOwnerAdmin.value
        val threshold = systemGameConfig.value.dualApprovalThresholdKs
        val willBeDual = approve && !isOwner && req.status == "pending" && req.requestType == "withdraw" && threshold > 0L && req.amountKs >= threshold
        val msg = when {
            willBeDual -> "ပမာဏကြီးသဖြင့် Main Admin အတည်ပြုချက် တင်ပြပြီးပါပြီ"
            approve -> "ငွေစာရင်း အတည်ပြုပြီးပါပြီ"
            else -> "တောင်းဆိုမှု ပယ်ဖျက်ပြီးပါပြီ"
        }
        runAction(msg) {
            val res = repository.processWalletRequest(
                req = req,
                approve = approve,
                adminNote = adminNote,
                walletConfig = walletConfig.value,
                isOwner = isOwner,
                dualApprovalThresholdKs = threshold
            )
            if (res.isSuccess) {
                val act = when {
                    willBeDual -> "Escalated withdraw to Main Admin"
                    approve -> "Approved ${req.requestType}"
                    else -> "Rejected ${req.requestType}"
                }
                logAuditAsync(act, "wallet", req.id, req.customerName, "${req.amountKs} Ks (${req.channel})")
            }
            res
        }
    }

    fun saveWalletConfig(
        depositPercent: Double,
        depositMode: String,
        withdrawPercent: Double,
        withdrawMode: String
    ) = runAction("ငွေသွင်း / ငွေထုတ် ရာခိုင်နှုန်း သိမ်းဆည်းပြီးပါပြီ") {
        val res = repository.saveWalletConfig(depositPercent, depositMode, withdrawPercent, withdrawMode)
        if (res.isSuccess) logAuditAsync("Updated wallet rates", "settings", "wallet_config", "", "Dep $depositPercent%, Wdr $withdrawPercent%")
        res
    }

    fun saveBettingConfig(
        winCommissionPercent: Double,
        winCommissionBasis: String,
        autoSettleOrders: Boolean,
        autoAcceptOrders: Boolean = bettingConfig.value.autoAcceptOrders
    ) = runAction("အနိုင်အကောက် (%) နှင့် အလိုအလျောက်စနစ် သိမ်းဆည်းပြီးပါပြီ") {
        val res = repository.saveBettingConfig(winCommissionPercent, winCommissionBasis, autoSettleOrders, autoAcceptOrders)
        if (res.isSuccess) {
            logAuditAsync("Updated betting config", "settings", "betting_config", "", "$winCommissionPercent% ($winCommissionBasis), AutoAccept=$autoAcceptOrders")
        }
        res
    }

    fun toggleAutoAcceptOrders(enabled: Boolean) = runAction(
        if (enabled) "အော်ဒါ အလိုအလျောက် လက်ခံစနစ် ဖွင့်ပြီးပါပြီ"
        else "အော်ဒါ ကိုယ်တိုင်စစ်ဆေး လက်ခံစနစ် ပြောင်းလဲပြီးပါပြီ"
    ) {
        val cur = bettingConfig.value
        val res = repository.saveBettingConfig(
            winCommissionPercent = cur.winCommissionPercent,
            winCommissionBasis = cur.winCommissionBasis,
            autoSettleOrders = cur.autoSettleOrders,
            autoAcceptOrders = enabled
        )
        if (res.isSuccess) {
            logAuditAsync("Toggled auto-accept orders", "settings", "betting_config", "", "AutoAccept=$enabled")
        }
        res
    }

    fun saveSystemGameConfig(config: SystemGameConfig) = runAction("ဂိမ်းနှင့် စနစ်ဆိုင်ရာ ဆက်တင်များ သိမ်းဆည်းပြီးပါပြီ") {
        val res = repository.saveSystemGameConfig(config)
        if (res.isSuccess) {
            logAuditAsync(
                action = "Updated system & game settings",
                category = "settings",
                targetId = "system_config",
                details = "Maint=${config.maintenanceMode}, DualLimit=${config.dualApprovalThresholdKs} Ks, Hide2D=${config.hideTwoD}, Hide3D=${config.hideThreeD}"
            )
        }
        res
    }

    fun saveSecurityConfig(config: SecurityConfig) = runAction("လုံခြုံရေး ထိန်းချုပ်မှု ဆက်တင်များ သိမ်းဆည်းပြီးပါပြီ") {
        val res = repository.saveSecurityConfig(config)
        if (res.isSuccess) {
            logAuditAsync(
                action = "Updated security controls",
                category = "security",
                targetId = "security_config",
                details = "PIN=${config.requireAdminPin}, IPWhitelist=${config.ipWhitelistEnabled}"
            )
        }
        res
    }

    fun saveBrandingConfig(config: BrandingConfig) = runAction("APK နာမည်၊ Logo၊ Theme နှင့် စည်းကမ်းချက် သိမ်းဆည်းပြီးပါပြီ") {
        val res = repository.saveBrandingConfig(config)
        if (res.isSuccess) {
            logAuditAsync("Updated app branding & rules", "settings", "branding_config", config.appName, "Theme=${config.themePreset}, MaxMain=${config.maxContactMainAdminKs}")
        }
        res
    }

    fun saveMiniGameControlConfig(config: MiniGameControlConfig) = runAction("ဂိမ်းခန်း အဆနှင့် ထိန်းချုပ်မှု သိမ်းဆည်းပြီးပါပြီ") {
        val res = repository.saveMiniGameControlConfig(config)
        if (res.isSuccess) {
            logAuditAsync("Updated mini-game control", "settings", "mini_game_control", "", "Directive=${config.globalDirective}, DiceStd=x${config.diceStandardMult}")
        }
        res
    }

    fun setCustomerMiniGameDirective(targetUid: String, customerName: String, directive: String) {
        val cur = miniGameControlConfig.value
        val updatedMap = cur.userDirectives.toMutableMap()
        if (directive == "auto") updatedMap.remove(targetUid) else updatedMap[targetUid] = directive
        val dirLabel = when (directive) {
            "stealth_house" -> "ဒိုင်အနိုင် (Stealth)"
            "force_win" -> "Win"
            "force_loss" -> "Loss"
            else -> "Auto"
        }
        runAction("$customerName ဂိမ်းထိန်းချုပ်မှု ($dirLabel) သတ်မှတ်ပြီးပါပြီ") {
            val res = repository.saveMiniGameControlConfig(cur.copy(userDirectives = updatedMap))
            if (res.isSuccess) logAuditAsync("Set mini-game directive", "settings", targetUid, customerName, directive)
            res
        }
    }

    fun saveSupportAgentProfile(profile: SupportAgentProfile) = runAction("ဆက်သွယ်ရန် အချက်အလက် သိမ်းဆည်းပြီးပါပြီ") {
        val res = repository.saveSupportAgentProfile(profile)
        if (res.isSuccess) logAuditAsync("Updated admin contact profile", "admin", profile.adminUid, profile.displayName, profile.phone)
        res
    }

    fun triggerGlobalForceLogout() = runAction("Session အားလုံးကို Force Logout လုပ်ပြီးပါပြီ") {
        val cur = securityConfig.value
        val updated = cur.copy(globalForceLogoutAtMs = System.currentTimeMillis())
        val res = repository.saveSecurityConfig(updated)
        if (res.isSuccess) logAuditAsync("Triggered global force logout", "security", "global")
        res
    }

    fun savePaymentAccount(
        accountId: String?,
        providerName: String,
        accountName: String,
        accountNumber: String,
        note: String,
        active: Boolean,
        ownerAdminId: String = "",
        ownerAdminName: String = ""
    ) = runAction("ငွေပေးချေမှု အကောင့် သိမ်းဆည်းပြီးပါပြီ") {
        val effectiveOwnerId = if (ownerAdminId.isNotBlank()) ownerAdminId else if (!isOwnerAdmin.value) currentUserId else ""
        val effectiveOwnerName = if (ownerAdminName.isNotBlank()) ownerAdminName else if (!isOwnerAdmin.value) (adminMember.value?.displayName ?: displayName) else ""
        val res = repository.savePaymentAccount(
            accountId = accountId,
            providerName = providerName,
            accountName = accountName,
            accountNumber = accountNumber,
            note = note,
            active = active,
            ownerAdminId = effectiveOwnerId,
            ownerAdminName = effectiveOwnerName
        )
        if (res.isSuccess) logAuditAsync("Saved payment account", "wallet", accountId ?: "new", providerName, "$accountName • $accountNumber")
        res
    }

    fun deletePaymentAccount(accountId: String) = runAction("ငွေပေးချေမှု အကောင့် ဖျက်ပြီးပါပြီ") {
        val res = repository.deletePaymentAccount(accountId)
        if (res.isSuccess) logAuditAsync("Deleted payment account", "wallet", accountId)
        res
    }

    fun autoSettleFinishedOrdersNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.autoSettleFinishedOrders(bettingConfig.value, feedService = feedService)
                res.onSuccess { summary ->
                    _events.emit(formatAutoSettleMessage(summary))
                    if (summary.settledCount > 0) {
                        logAuditAsync("Auto settled orders", "order", "batch", "", "Settled=${summary.settledCount}")
                    }
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "အလိုအလျောက် အလျော်အစား တွက်ချက်မှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun autoSettleCustomerOrdersNow(targetUserId: String, customerName: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.autoSettleFinishedOrders(bettingConfig.value, targetUserId, feedService = feedService)
                res.onSuccess { summary ->
                    val prefix = if (customerName.isNotBlank()) "$customerName: " else ""
                    _events.emit(prefix + formatAutoSettleMessage(summary))
                    if (summary.settledCount > 0) {
                        logAuditAsync("Auto settled customer orders", "order", targetUserId, customerName, "Settled=${summary.settledCount}")
                    }
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "အလိုအလျောက် အလျော်အစား တွက်ချက်မှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun autoSettleSingleOrderNow(order: BetOrder) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.autoSettleSingleOrder(order, bettingConfig.value, feedService = feedService)
                res.onSuccess { msg ->
                    _events.emit(msg)
                    logAuditAsync("Auto settled single order", "order", order.id, order.customerName, msg)
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "အလိုအလျောက် စာရင်းရှင်းလင်းမှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    private fun formatAutoSettleMessage(summary: AutoSettleSummary): String {
        return if (summary.settledCount > 0) {
            "အလိုအလျောက် ရှင်းလင်းပြီး ${summary.settledCount} ခု (နိုင် ${summary.wonCount}, ရှုံး ${summary.lostCount}, သရေ ${summary.pushCount})"
        } else {
            "ပွဲပြီး/ဂဏန်းထွက် အော်ဒါအသစ် ရှင်းလင်းရန် မရှိသေးပါ"
        }
    }

    fun toggleAutoSyncDailyMatches(enabled: Boolean) = runAction(
        if (enabled) "ပွဲရှိသည့်နေ့တိုင်း အလိုအလျောက်ထည့်ခြင်း ဖွင့်ပြီးပါပြီ"
        else "အလိုအလျောက်ပွဲစဉ်ထည့်ခြင်း ပိတ်ပြီးပါပြီ"
    ) {
        val cur = boardConfig.value
        val res = repository.saveBoardConfig(
            autoSyncDailyMatches = enabled,
            autoSyncLiveResults = cur.autoSyncLiveResults,
            lastSyncedDate = cur.lastSyncedDate,
            allowedLeagues = cur.allowedLeagues,
            autoSyncOneExpress = cur.autoSyncOneExpress,
            autoSyncFiveFiveFive = cur.autoSyncFiveFiveFive,
            googleSheetCsvUrl = cur.googleSheetCsvUrl
        )
        if (res.isSuccess && enabled) {
            repository.syncDailyMatchesFromFeed(feedService, listOf(0, 1), cur.copy(autoSyncDailyMatches = true))
        }
        res
    }

    fun toggleAutoSyncLiveResults(enabled: Boolean) = runAction(
        if (enabled) "တိုက်ရိုက်ရလဒ် အလိုအလျောက်ချိတ်ဆက်ခြင်း ဖွင့်ပြီးပါပြီ"
        else "တိုက်ရိုက်ရလဒ် အလိုအလျောက်ချိတ်ဆက်ခြင်း ပိတ်ပြီးပါပြီ"
    ) {
        val cur = boardConfig.value
        val res = repository.saveBoardConfig(
            autoSyncDailyMatches = cur.autoSyncDailyMatches,
            autoSyncLiveResults = enabled,
            lastSyncedDate = cur.lastSyncedDate,
            allowedLeagues = cur.allowedLeagues,
            autoSyncOneExpress = cur.autoSyncOneExpress,
            autoSyncFiveFiveFive = cur.autoSyncFiveFiveFive,
            googleSheetCsvUrl = cur.googleSheetCsvUrl
        )
        if (res.isSuccess && enabled) {
            repository.syncLiveMatchResults(feedService)
        }
        res
    }

    fun toggleAutoSyncOneExpress(enabled: Boolean) = runAction(
        if (enabled) "1xBET အလိုအလျောက် ကြေးထည့်စနစ် ဖွင့်ပြီးပါပြီ"
        else "1xBET အလိုအလျောက် ကြေးထည့်စနစ် ပိတ်ပြီးပါပြီ"
    ) {
        val cur = boardConfig.value
        val res = repository.saveBoardConfig(
            autoSyncDailyMatches = cur.autoSyncDailyMatches,
            autoSyncLiveResults = cur.autoSyncLiveResults,
            lastSyncedDate = cur.lastSyncedDate,
            allowedLeagues = cur.allowedLeagues,
            autoSyncOneExpress = enabled,
            autoSyncFiveFiveFive = cur.autoSyncFiveFiveFive,
            googleSheetCsvUrl = cur.googleSheetCsvUrl
        )
        if (res.isSuccess && enabled) {
            repository.syncBoardMatchesFromFeed("one_express", feedService, listOf(0, 1), cur.copy(autoSyncOneExpress = true))
        }
        res
    }

    fun toggleAutoSync555(enabled: Boolean) = runAction(
        if (enabled) "555 Mix အလိုအလျောက် ကြေးထည့်စနစ် ဖွင့်ပြီးပါပြီ"
        else "555 Mix အလိုအလျောက် ကြေးထည့်စနစ် ပိတ်ပြီးပါပြီ"
    ) {
        val cur = boardConfig.value
        val res = repository.saveBoardConfig(
            autoSyncDailyMatches = cur.autoSyncDailyMatches,
            autoSyncLiveResults = cur.autoSyncLiveResults,
            lastSyncedDate = cur.lastSyncedDate,
            allowedLeagues = cur.allowedLeagues,
            autoSyncOneExpress = cur.autoSyncOneExpress,
            autoSyncFiveFiveFive = enabled,
            googleSheetCsvUrl = cur.googleSheetCsvUrl
        )
        if (res.isSuccess && enabled) {
            repository.syncBoardMatchesFromFeed("five_five_five", feedService, listOf(0, 1), cur.copy(autoSyncFiveFiveFive = true))
        }
        res
    }

    fun saveAllowedLeagues(leagues: List<String>) = runAction("ဖွင့်လှစ်မည့် ဘောလုံးဖလား/လိဂ် စာရင်း သိမ်းဆည်းပြီးပါပြီ") {
        val cur = boardConfig.value
        val res = repository.saveBoardConfig(
            autoSyncDailyMatches = cur.autoSyncDailyMatches,
            autoSyncLiveResults = cur.autoSyncLiveResults,
            lastSyncedDate = cur.lastSyncedDate,
            allowedLeagues = leagues,
            autoSyncOneExpress = cur.autoSyncOneExpress,
            autoSyncFiveFiveFive = cur.autoSyncFiveFiveFive,
            googleSheetCsvUrl = cur.googleSheetCsvUrl
        )
        if (res.isSuccess) {
            logAuditAsync("Updated allowed leagues", "board", "board_config", "", "${leagues.size} leagues")
        }
        res
    }

    fun saveBoardSheetUrl(csvUrl: String) = runAction("Google Sheet Link သိမ်းဆည်းပြီးပါပြီ") {
        val cur = boardConfig.value
        val normalized = com.example.data.network.FootballFeedService.normalizeGoogleSheetCsvUrl(csvUrl)
        val res = repository.saveBoardConfig(
            autoSyncDailyMatches = cur.autoSyncDailyMatches,
            autoSyncLiveResults = cur.autoSyncLiveResults,
            lastSyncedDate = cur.lastSyncedDate,
            allowedLeagues = cur.allowedLeagues,
            autoSyncOneExpress = cur.autoSyncOneExpress,
            autoSyncFiveFiveFive = cur.autoSyncFiveFiveFive,
            googleSheetCsvUrl = normalized
        )
        if (res.isSuccess) {
            logAuditAsync("Saved Google Sheet CSV URL", "board", "board_config")
        }
        res
    }

    fun syncMatchesFromGoogleSheet(boardSource: String, csvUrl: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val normalizedUrl = com.example.data.network.FootballFeedService.normalizeGoogleSheetCsvUrl(csvUrl)
                val cur = boardConfig.value
                if (normalizedUrl != cur.googleSheetCsvUrl && normalizedUrl.isNotBlank()) {
                    repository.saveBoardConfig(
                        autoSyncDailyMatches = cur.autoSyncDailyMatches,
                        autoSyncLiveResults = cur.autoSyncLiveResults,
                        lastSyncedDate = cur.lastSyncedDate,
                        allowedLeagues = cur.allowedLeagues,
                        autoSyncOneExpress = cur.autoSyncOneExpress,
                        autoSyncFiveFiveFive = cur.autoSyncFiveFiveFive,
                        googleSheetCsvUrl = normalizedUrl
                    )
                }
                val fetchRes = feedService.fetchGoogleSheetCsvMatches(normalizedUrl)
                val csvText = fetchRes.getOrElse { err ->
                    _events.emit(err.localizedMessage ?: "Google Sheet ဖတ်မရပါ")
                    return@launch
                }
                val rows = com.example.data.betting.MatchSheetCsvCodec.parseCsvOrTsv(csvText, boardSource)
                if (rows.isEmpty()) {
                    _events.emit("Google Sheet ထဲတွင် မှန်ကန်သော ပွဲစဉ်ဒေတာ မတွေ့ပါ")
                    return@launch
                }
                val upsertRes = repository.upsertMatchesFromSheet(rows, boardSource)
                upsertRes.onSuccess { count ->
                    _events.emit("Google Sheet မှ ပွဲစဉ် $count ပွဲ Update / ထည့်သွင်းပြီးပါပြီ")
                    logAuditAsync("Synced matches from Google Sheet", "board", boardSource, "", "Count=$count")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Google Sheet ဒေတာ သိမ်းဆည်းမှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun importMatchesFromCsvText(boardSource: String, csvText: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val rows = com.example.data.betting.MatchSheetCsvCodec.parseCsvOrTsv(csvText, boardSource)
                if (rows.isEmpty()) {
                    _events.emit("CSV / Excel စာသားပုံစံ မမှန်ကန်သေးပါ (ဥပမာ: League,Home,Away,Time,Handicap,Goal)")
                    return@launch
                }
                val upsertRes = repository.upsertMatchesFromSheet(rows, boardSource)
                upsertRes.onSuccess { count ->
                    _events.emit("Sheet / Excel မှ ပွဲစဉ် $count ပွဲ ထည့်သွင်း/ပြင်ဆင်ပြီးပါပြီ")
                    logAuditAsync("Imported CSV/Excel matches", "board", boardSource, "", "Count=$count")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Sheet CSV ထည့်သွင်းမှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun clearFinishedMatches(matchIds: List<String>) = runAction("ပြီးဆုံးပြီးသား ပွဲစဉ်များ ရှင်းလင်းပြီးပါပြီ") {
        val res = repository.deleteMatchesBatch(matchIds)
        if (res.isSuccess) {
            logAuditAsync("Cleared finished matches", "board", "batch", "", "Count=${res.getOrDefault(0)}")
        }
        res.map { }
    }

    fun syncDailyMatchesNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.syncDailyMatchesFromFeed(feedService, listOf(0, 1), boardConfig.value)
                res.onSuccess { count ->
                    if (count > 0) {
                        _events.emit("ယနေ့/မနက်ဖြန် ဘောလုံးပွဲစဉ် $count ပွဲ အလိုအလျောက်ထည့်သွင်းပြီးပါပြီ")
                        logAuditAsync("Synced daily matches", "board", "matches", "", "Count=$count")
                    } else {
                        _events.emit("ယနေ့အတွက် ပွဲစဉ်အသစ် မတွေ့ရှိသေးပါ")
                    }
                    refreshLiveFootballFeed()
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "ပွဲစဉ်ဆွဲယူမှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun syncOneExpressMatchesNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.syncBoardMatchesFromFeed("one_express", feedService, listOf(0, 1), boardConfig.value)
                res.onSuccess { outcome ->
                    if (outcome.syncedCount > 0) {
                        _events.emit("1xBET ပွဲကြေး ${outcome.syncedCount} ပွဲ အလိုအလျောက်ထည့်သွင်းပြီးပါပြီ (လိုတရ APK ကြေး x1.90)")
                        logAuditAsync(
                            "Synced 1xBET board",
                            "board",
                            "one_express",
                            "",
                            "Count=${outcome.syncedCount}"
                        )
                    } else {
                        _events.emit("1xBET အတွက် ပွဲစဉ်အသစ် မတွေ့ရှိသေးပါ")
                    }
                    refreshLiveFootballFeed()
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "1xBET ပွဲကြေးဆွဲယူမှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun sync555MatchesNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.syncBoardMatchesFromFeed("five_five_five", feedService, listOf(0, 1), boardConfig.value)
                res.onSuccess { outcome ->
                    if (outcome.syncedCount > 0) {
                        _events.emit("555 Mix ပွဲကြေး ${outcome.syncedCount} ပွဲ အလိုအလျောက်ထည့်သွင်းပြီးပါပြီ")
                        logAuditAsync("Synced 555 Mix board", "board", "five_five_five", "", "Count=${outcome.syncedCount}")
                    } else {
                        _events.emit("555 Mix အတွက် ပွဲစဉ်အသစ် မတွေ့ရှိသေးပါ")
                    }
                    refreshLiveFootballFeed()
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "555 Mix ပွဲကြေးဆွဲယူမှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun syncLiveMatchResultsNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.syncLiveMatchResults(feedService)
                res.onSuccess { updated ->
                    val settledCount = if (bettingConfig.value.autoSettleOrders && currentAdminPermissions.value.canManageOrders) {
                        repository.autoSettleFinishedOrders(bettingConfig.value).getOrNull()?.settledCount ?: 0
                    } else 0
                    val msg = buildString {
                        if (updated > 0) append("တိုက်ရိုက်/ပွဲပြီး ရလဒ် $updated ပွဲ အပ်ဒိတ်လုပ်ပြီးပါပြီ")
                        else append("တိုက်ရိုက်ရလဒ်များ စစ်ဆေးပြီးပါပြီ")
                        if (settledCount > 0) append(" • အော်ဒါ $settledCount ခု အလိုအလျောက် ရှင်းလင်းပြီး")
                    }
                    _events.emit(msg)
                    refreshLiveFootballFeed()
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "တိုက်ရိုက်ရလဒ် ချိတ်ဆက်မှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun saveNewMatch(
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double,
        goalOdds: Double,
        boardSource: String = "standard",
        oneExpressKyay: String = "",
        fiveFiveFiveKyay: String = "",
        oneXBetOdds: OneXBetOddsSpec? = null,
        onSuccess: () -> Unit = {}
    ) = runAction("ပွဲစဉ်အသစ် ထည့်သွင်းပြီးပါပြီ") {
        val res = repository.saveNewMatch(
            league = league,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            kickoffLabel = kickoffLabel,
            handicapLine = handicapLine,
            goalLine = goalLine,
            bodyOdds = bodyOdds,
            goalOdds = goalOdds,
            boardSource = boardSource,
            oneExpressKyay = oneExpressKyay,
            fiveFiveFiveKyay = fiveFiveFiveKyay,
            oneXBetOdds = oneXBetOdds
        )
        if (res.isSuccess) {
            logAuditAsync("Added football match", "board", "", "$homeTeam vs $awayTeam", "$league ($boardSource)")
            onSuccess()
        }
        res
    }

    fun updateMatchDetails(
        matchId: String,
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double,
        goalOdds: Double,
        status: String,
        homeScore: Long,
        awayScore: Long,
        oneExpressKyay: String? = null,
        fiveFiveFiveKyay: String? = null,
        oneXBetOdds: OneXBetOddsSpec? = null
    ) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.updateMatchDetails(
                    matchId = matchId,
                    league = league,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    kickoffLabel = kickoffLabel,
                    handicapLine = handicapLine,
                    goalLine = goalLine,
                    bodyOdds = bodyOdds,
                    goalOdds = goalOdds,
                    status = status,
                    homeScore = homeScore,
                    awayScore = awayScore,
                    oneExpressKyay = oneExpressKyay,
                    fiveFiveFiveKyay = fiveFiveFiveKyay,
                    oneXBetOdds = oneXBetOdds
                )
                res.onSuccess {
                    logAuditAsync("Updated match", "board", matchId, "$homeTeam vs $awayTeam", "$status ($homeScore-$awayScore)")
                    if (status == "finished" && bettingConfig.value.autoSettleOrders && currentAdminPermissions.value.canManageOrders) {
                        val settled = repository.autoSettleFinishedOrders(bettingConfig.value).getOrNull()?.settledCount ?: 0
                        if (settled > 0) {
                            _events.emit("ပွဲစဉ်သိမ်းဆည်းပြီး အော်ဒါ $settled ခု အလိုအလျောက် ငွေရှင်းပြီးပါပြီ")
                            return@onSuccess
                        }
                    }
                    _events.emit("ပွဲစဉ်အချက်အလက် ပြင်ဆင်ပြီးပါပြီ")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Operation failed")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun deleteMatch(matchId: String) = runAction("ပွဲစဉ်အဟောင်း ဖျက်ပြီးပါပြီ") {
        val res = repository.deleteMatch(matchId)
        if (res.isSuccess) logAuditAsync("Deleted match", "board", matchId)
        res
    }

    fun saveNewLotteryDraw(
        roundTitle: String,
        closeTimeLabel: String,
        payoutMultiplier: Double,
        drawType: String = "3d",
        onSuccess: () -> Unit = {}
    ) = runAction(if (drawType == "2d") "၂လုံးထီ အကြိမ်သစ် ဖွင့်လှစ်ပြီးပါပြီ" else "၃လုံးထီ အကြိမ်သစ် ဖွင့်လှစ်ပြီးပါပြီ") {
        val res = repository.saveNewLotteryDraw(roundTitle, closeTimeLabel, payoutMultiplier, drawType)
        if (res.isSuccess) {
            logAuditAsync("Created ${drawType.uppercase()} draw", "board", "", roundTitle, "x$payoutMultiplier")
            onSuccess()
        }
        res
    }

    fun updateLotteryDraw(
        drawId: String,
        roundTitle: String,
        closeTimeLabel: String,
        payoutMultiplier: Double,
        winningNumber: String,
        status: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.updateLotteryDraw(drawId, roundTitle, closeTimeLabel, payoutMultiplier, winningNumber, status)
                res.onSuccess {
                    logAuditAsync("Updated lottery draw", "board", drawId, roundTitle, "$status ($winningNumber)")
                    if (status == "finished" && winningNumber.length in 2..3 && bettingConfig.value.autoSettleOrders && currentAdminPermissions.value.canManageOrders) {
                        val settled = repository.autoSettleFinishedOrders(bettingConfig.value).getOrNull()?.settledCount ?: 0
                        if (settled > 0) {
                            _events.emit("ထီသိမ်းဆည်းပြီး အော်ဒါ $settled ခု အလိုအလျောက် ငွေရှင်းပြီးပါပြီ")
                            return@onSuccess
                        }
                    }
                    _events.emit("ထီ အချက်အလက် ပြင်ဆင်ပြီးပါပြီ")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Operation failed")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun deleteLotteryDraw(drawId: String) = runAction("ထီ အကြိမ် ဖျက်ပြီးပါပြီ") {
        val res = repository.deleteLotteryDraw(drawId)
        if (res.isSuccess) logAuditAsync("Deleted lottery draw", "board", drawId)
        res
    }

    fun deleteWalletRequest(requestId: String) = runAction("ငွေစာရင်းအဟောင်း ဖျက်ပြီးပါပြီ") {
        repository.deleteWalletRequest(requestId)
    }

    fun clearProcessedWalletRequests(requestIds: List<String>) = runAction("ပြီးစီးပြီးသား ငွေသွင်း/ထုတ် အဟောင်းများ ဖျက်ပြီးပါပြီ") {
        repository.clearProcessedWalletRequests(requestIds).map { }
    }

    fun createAdminInvite(
        label: String,
        assignedEmail: String = "",
        initialPin: String = "",
        permissions: AdminPermissions = AdminPermissions.PRESET_WALLET_CASHIER,
        onCodeCreated: (String) -> Unit = {}
    ) = runAction("Second Admin Invite Code ဖန်တီးပြီးပါပြီ") {
        val pinHash = if (initialPin.trim().isNotBlank()) sha256(initialPin.trim()) else ""
        val res = repository.createAdminInvite(label, assignedEmail, pinHash, permissions)
        res.onSuccess { code ->
            logAuditAsync("Created Second Admin invite", "admin", code, label, assignedEmail)
            onCodeCreated(code)
        }
        res.map { }
    }

    fun toggleAdminInvite(code: String, active: Boolean) = runAction("Invite Code အခြေအနေ ပြောင်းလဲပြီးပါပြီ") {
        val res = repository.toggleAdminInvite(code, active)
        if (res.isSuccess) logAuditAsync(if (active) "Activated invite" else "Disabled invite", "admin", code)
        res
    }

    fun saveAdminInviteEdit(
        oldCode: String,
        newCode: String,
        label: String,
        active: Boolean,
        assignedEmail: String,
        newPin: String?,
        permissions: AdminPermissions
    ) = runAction("Invite Code ပြင်ဆင်ပြီးပါပြီ") {
        val pinHash = when {
            newPin == null -> null
            newPin.trim().isBlank() -> ""
            else -> sha256(newPin.trim())
        }
        val res = repository.saveAdminInviteEdit(oldCode, newCode, label, active, assignedEmail, pinHash, permissions)
        if (res.isSuccess) logAuditAsync("Edited Second Admin invite", "admin", newCode, label, "Old=$oldCode")
        res
    }

    fun deleteAdminInvite(code: String) = runAction("Invite Code ဖျက်ပြီးပါပြီ") {
        val res = repository.deleteAdminInvite(code)
        if (res.isSuccess) logAuditAsync("Deleted Second Admin invite", "admin", code)
        res
    }

    fun setCustomerHidden(userUid: String, userName: String, hidden: Boolean) =
        runAction(if (hidden) "$userName အချက်အလက်ကို Hide ထားပြီးပါပြီ" else "$userName အချက်အလက်ကို ပြန်ဖော်ပြီးပါပြီ") {
            val res = repository.setUserHidden(userUid, hidden)
            if (res.isSuccess) logAuditAsync(if (hidden) "Hid Customer" else "Unhid Customer", "user", userUid, userName)
            res
        }

    fun setOrderHidden(orderId: String, hidden: Boolean) =
        runAction(if (hidden) "အော်ဒါကို Hide ထားပြီးပါပြီ" else "အော်ဒါကို ပြန်ဖော်ပြီးပါပြီ") {
            val res = repository.setOrderHidden(orderId, hidden)
            if (res.isSuccess) logAuditAsync(if (hidden) "Hid Order" else "Unhid Order", "order", orderId)
            res
        }

    fun editOrderDetails(
        orderId: String,
        selections: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        actualPayoutKs: Long,
        status: String,
        adminNote: String
    ) = runAction("အော်ဒါအချက်အလက် ပြင်ဆင်ပြီးပါပြီ") {
        val res = repository.editOrderDetails(orderId, selections, drawRound, stakeKs, multiplier, actualPayoutKs, status, adminNote)
        if (res.isSuccess) logAuditAsync("Edited order", "order", orderId, "", "Stake=$stakeKs, Status=$status")
        res
    }

    fun editWalletRequest(
        reqId: String,
        amountKs: Long,
        netAmountKs: Long,
        channel: String,
        referenceNote: String,
        status: String,
        adminNote: String,
        isHidden: Boolean,
        receiptImageBase64: String? = null
    ) = runAction("ငွေသွင်း/ထုတ် မှတ်တမ်း ပြင်ဆင်ပြီးပါပြီ") {
        val res = repository.editWalletRequest(
            reqId = reqId,
            amountKs = amountKs,
            netAmountKs = netAmountKs,
            channel = channel,
            referenceNote = referenceNote,
            status = status,
            adminNote = adminNote,
            isHidden = isHidden,
            receiptImageBase64 = receiptImageBase64
        )
        if (res.isSuccess) logAuditAsync("Edited wallet request", "wallet", reqId, "", "$amountKs Ks ($status)")
        res
    }

    fun editTransaction(
        txId: String,
        title: String,
        amountKs: Long,
        direction: String,
        isHidden: Boolean
    ) = runAction("ငွေစာရင်းမှတ်တမ်း ပြင်ဆင်ပြီးပါပြီ") {
        val res = repository.editTransaction(txId, title, amountKs, direction, isHidden)
        if (res.isSuccess) logAuditAsync("Edited ledger transaction", "wallet", txId, title, "$amountKs Ks ($direction)")
        res
    }

    fun deleteTransaction(txId: String) = runAction("ငွေစာရင်းမှတ်တမ်း ဖျက်ပြီးပါပြီ") {
        val res = repository.deleteTransaction(txId)
        if (res.isSuccess) logAuditAsync("Deleted ledger transaction", "wallet", txId)
        res
    }

    fun deleteAuditLog(logId: String) = runAction("Audit မှတ်တမ်း ဖျက်ပြီးပါပြီ") {
        repository.deleteAuditLog(logId)
    }

    fun clearAllAuditLogs(logIds: List<String>) = runAction("Audit မှတ်တမ်းအားလုံး ရှင်းလင်းပြီးပါပြီ") {
        repository.clearAllAuditLogs(logIds).map { }
    }

    fun updateAdminMemberPermissions(adminUid: String, adminName: String, permissions: AdminPermissions) =
        runAction("Second Admin ခွင့်ပြုချက်များ ပြင်ဆင်ပြီးပါပြီ") {
            val res = repository.updateAdminMemberPermissions(adminUid, permissions)
            if (res.isSuccess) logAuditAsync("Updated Admin permissions", "admin", adminUid, adminName)
            res
        }

    fun setAdminMemberStatus(adminUid: String, adminName: String, suspend: Boolean) =
        runAction(if (suspend) "Second Admin ကို ယာယီပိတ် (Suspend) ပြီးပါပြီ" else "Second Admin ကို ပြန်ဖွင့်ပြီးပါပြီ") {
            val status = if (suspend) "suspended" else "active"
            val res = repository.setAdminMemberStatus(adminUid, status)
            if (res.isSuccess) logAuditAsync(if (suspend) "Suspended Admin" else "Reactivated Admin", "admin", adminUid, adminName)
            res
        }

    fun resetAdminMemberPin(adminUid: String, adminName: String, newPin: String) {
        val clean = newPin.trim()
        if (clean.isNotBlank() && clean.length !in 4..8) {
            emitMessage("PIN သည် ဂဏန်း 4-8 လုံး ဖြစ်ရပါမည်")
            return
        }
        val hash = if (clean.isBlank()) "" else sha256(clean)
        runAction("Second Admin PIN ပြန်လည်သတ်မှတ်ပြီးပါပြီ") {
            val res = repository.resetAdminMemberPin(adminUid, hash)
            if (res.isSuccess) logAuditAsync("Reset Second Admin PIN", "security", adminUid, adminName)
            res
        }
    }

    fun triggerForceLogoutAdmin(adminUid: String, adminName: String) = runAction("$adminName ကို Force Logout လုပ်ပြီးပါပြီ") {
        val res = repository.triggerForceLogoutAdmin(adminUid)
        if (res.isSuccess) logAuditAsync("Forced Admin logout", "security", adminUid, adminName)
        res
    }

    fun triggerForceLogoutUser(userUid: String, userName: String) = runAction("$userName ကို Force Logout လုပ်ပြီးပါပြီ") {
        val res = repository.triggerForceLogoutUser(userUid)
        if (res.isSuccess) logAuditAsync("Forced Customer logout", "security", userUid, userName)
        res
    }

    fun setCustomerLocked(userUid: String, userName: String, locked: Boolean) =
        runAction(if (locked) "$userName အကောင့်ကို ယာယီပိတ်ပြီးပါပြီ" else "$userName အကောင့်ကို ပြန်ဖွင့်ပြီးပါပြီ") {
            val res = repository.setCustomerLocked(userUid, locked)
            if (res.isSuccess) logAuditAsync(if (locked) "Locked Customer" else "Unlocked Customer", "user", userUid, userName)
            res
        }

    fun removeAdminMember(adminUid: String) = runAction("Admin စာရင်းမှ ဖယ်ရှားပြီးပါပြီ") {
        val res = repository.removeAdminMember(adminUid)
        if (res.isSuccess) logAuditAsync("Removed Admin member", "admin", adminUid)
        res
    }

    fun adjustUserBalance(targetUid: String, deltaKs: Long, note: String) = runAction("Customer လက်ကျန်ငွေ ပြင်ဆင်ပြီးပါပြီ") {
        val res = repository.adjustUserBalance(targetUid, deltaKs, note)
        if (res.isSuccess) logAuditAsync("Adjusted balance", "wallet", targetUid, "", "$deltaKs Ks ($note)")
        res
    }

    fun giftRewardPoints(targetUid: String, targetName: String, deltaPoints: Long, note: String) =
        runAction(
            if (deltaPoints >= 0L) "Customer ထံ လက်ဆောင်ပွိုင့် ပေးပို့ပြီးပါပြီ"
            else "Customer ထံမှ ပွိုင့်ပြန်နှုတ်ပြီးပါပြီ"
        ) {
            val res = repository.giftRewardPoints(targetUid, targetName, deltaPoints, note)
            if (res.isSuccess) logAuditAsync("Modified reward points", "user", targetUid, targetName, "$deltaPoints Pts")
            res
        }

    fun seedSampleBoard() = runAction("နမူနာ ပွဲစဉ်များနှင့် ၃လုံးထီ ထည့်သွင်းပြီးပါပြီ") {
        repository.seedSampleBoard()
    }

    fun seedSampleExpressBoard(boardSource: String) {
        val label = if (boardSource == "one_express") "1xBET" else "555 Mix"
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.seedSampleBoardForSource(boardSource)
                res.onSuccess { count ->
                    _events.emit("$label နမူနာပွဲကြေး $count ပွဲ ထည့်သွင်းပြီးပါပြီ")
                    logAuditAsync("Seeded $label sample board", "board", boardSource, "", "Count=$count")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "$label နမူနာပွဲ ထည့်သွင်းမှု မအောင်မြင်ပါ")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    // --- APK Update, Chat & Quality Rating ---
    fun saveAppUpdateConfig(
        latestVersionCode: Int,
        latestVersionName: String,
        downloadUrl: String,
        releaseNotes: String,
        forceUpdate: Boolean
    ) = runAction("APK Update အချက်အလက် Customer များထံ ပေးပို့ပြီးပါပြီ") {
        val res = repository.saveAppUpdateConfig(latestVersionCode, latestVersionName, downloadUrl, releaseNotes, forceUpdate)
        if (res.isSuccess) logAuditAsync("Published APK update", "settings", "app_update", "v$latestVersionName ($latestVersionCode)")
        res
    }

    fun sendCustomerDirectMessage(text: String) {
        val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
        viewModelScope.launch {
            repository.sendDirectMessage(currentUserId, name, "customer", text)
                .onFailure { err -> _events.emit(err.localizedMessage ?: "မက်ဆေ့ချ် ပေးပို့မရပါ") }
        }
    }

    fun sendAdminDirectMessage(threadUserId: String, text: String) {
        val adminName = adminMember.value?.displayName?.ifBlank { null } ?: "$displayName (Admin)"
        viewModelScope.launch {
            repository.sendDirectMessage(threadUserId, adminName, "admin", text)
                .onFailure { err -> _events.emit(err.localizedMessage ?: "မက်ဆေ့ချ် ပေးပို့မရပါ") }
        }
    }

    fun deleteDirectMessage(msgId: String) = runAction("မက်ဆေ့ချ် ဖျက်ပြီးပါပြီ") {
        repository.deleteDirectMessage(msgId)
    }

    fun sendGroupMessage(text: String, asAdmin: Boolean = false) {
        val role = if (asAdmin && isAdmin.value) "admin" else "customer"
        val senderName = if (role == "admin") {
            adminMember.value?.displayName?.ifBlank { null } ?: "$displayName (Admin)"
        } else {
            (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
        }
        viewModelScope.launch {
            repository.sendGroupMessage(senderName, role, text)
                .onFailure { err -> _events.emit(err.localizedMessage ?: "Group မက်ဆေ့ချ် ပေးပို့မရပါ") }
        }
    }

    fun deleteGroupMessage(msgId: String) = runAction("Group မက်ဆေ့ချ် ဖျက်ပြီးပါပြီ") {
        repository.deleteGroupMessage(msgId)
    }

    fun submitQualityReview(stars: Int, qualityTier: String, comment: String) =
        runAction("အရည်အသွေး အဆင့်သတ်မှတ်ချက် ပေးပို့ပြီးပါပြီ") {
            val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
            repository.submitQualityReview(name, stars, qualityTier, comment)
        }

    fun deleteQualityReview(targetUserId: String) = runAction("သုံးသပ်ချက် ဖျက်ပြီးပါပြီ") {
        repository.deleteQualityReview(targetUserId)
    }

    // --- VOICE & VIDEO CALLING ---

    fun startCustomerCallToAdmin(callType: String) {
        val myName = (userAccount.value as? UiState.Success)?.data?.displayName?.ifBlank { null } ?: displayName
        startOutgoingCall(
            threadUserId = currentUserId,
            callerName = myName,
            callerRole = "customer",
            peerName = "လိုတရ Admin",
            peerRole = "admin",
            callType = callType
        )
    }

    fun startAdminCallToCustomer(customerUid: String, customerName: String, callType: String) {
        if (!isAdmin.value) {
            emitMessage("Admin ခွင့်ပြုချက် မရှိသေးပါ")
            return
        }
        val adminName = adminMember.value?.displayName?.ifBlank { null } ?: "$displayName (Admin)"
        startOutgoingCall(
            threadUserId = customerUid,
            callerName = adminName,
            callerRole = "admin",
            peerName = customerName.ifBlank { "Member" },
            peerRole = "customer",
            callType = callType
        )
    }

    private fun startOutgoingCall(
        threadUserId: String,
        callerName: String,
        callerRole: String,
        peerName: String,
        peerRole: String,
        callType: String
    ) {
        val mgr = callManager
        if (mgr == null) {
            emitMessage("Call စနစ် အဆင်သင့်မဖြစ်သေးပါ")
            return
        }
        if (_activeCall.value != null) {
            emitMessage("ဖုန်းခေါ်ဆိုမှု လုပ်ဆောင်နေဆဲ ဖြစ်ပါသည်")
            return
        }
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val isVideo = callType == "video"
        synchronized(callSyncLock) {
            canSendLocalIce = false
            pendingLocalIceCandidates.clear()
            processedCandidateIds.clear()
        }
        _dismissedCallIds.update { it + callId }
        _activeCall.value = ActiveCallUiState(
            callId = callId,
            threadUserId = threadUserId,
            callType = callType,
            peerName = peerName,
            peerRole = peerRole,
            isOutgoing = true,
            status = "calling",
            isMuted = false,
            isSpeakerOn = isVideo,
            isCameraEnabled = isVideo,
            isFrontCamera = true
        )

        mgr.startSession(
            isVideo = isVideo,
            listener = createCallListener(callId),
            onReady = {
                mgr.createOffer(
                    onSuccess = { offerSdp ->
                        viewModelScope.launch {
                            val res = repository.createCallSession(
                                callId = callId,
                                threadUserId = threadUserId,
                                callerName = callerName,
                                callerRole = callerRole,
                                calleeName = peerName,
                                callType = callType,
                                offerSdp = offerSdp
                            )
                            if (res.isSuccess) {
                                val toFlush: List<Triple<String, Int, String>>
                                synchronized(callSyncLock) {
                                    canSendLocalIce = true
                                    toFlush = pendingLocalIceCandidates.toList()
                                    pendingLocalIceCandidates.clear()
                                }
                                toFlush.forEach { (mid, idx, cand) ->
                                    launch { repository.addCallIceCandidate(callId, mid, idx, cand) }
                                }
                                observeActiveCallSignals(callId, isOutgoing = true)
                            } else {
                                cleanupLocalCall(res.exceptionOrNull()?.localizedMessage ?: "ဖုန်းခေါ်ဆိုမှု မအောင်မြင်ပါ")
                            }
                        }
                    },
                    onError = { err ->
                        viewModelScope.launch {
                            cleanupLocalCall(err)
                        }
                    }
                )
            },
            onError = { err ->
                viewModelScope.launch {
                    cleanupLocalCall(err)
                }
            }
        )
    }

    fun answerIncomingCall(session: CallSession) {
        val mgr = callManager ?: return
        if (_activeCall.value != null) return
        val isVideo = session.callType == "video"
        val myName = if (session.callerRole == "customer" && isAdmin.value) {
            adminMember.value?.displayName?.ifBlank { null } ?: "$displayName (Admin)"
        } else {
            (userAccount.value as? UiState.Success)?.data?.displayName?.ifBlank { null } ?: displayName
        }

        _dismissedCallIds.update { it + session.id }
        synchronized(callSyncLock) {
            canSendLocalIce = true
            pendingLocalIceCandidates.clear()
            processedCandidateIds.clear()
        }
        _activeCall.value = ActiveCallUiState(
            callId = session.id,
            threadUserId = session.threadUserId,
            callType = session.callType,
            peerName = session.callerName.ifBlank { "Member" },
            peerRole = session.callerRole,
            isOutgoing = false,
            status = "connecting",
            isMuted = false,
            isSpeakerOn = isVideo,
            isCameraEnabled = isVideo,
            isFrontCamera = true
        )

        observeActiveCallSignals(session.id, isOutgoing = false)

        mgr.startSession(
            isVideo = isVideo,
            listener = createCallListener(session.id),
            onReady = {
                mgr.acceptOfferAndCreateAnswer(
                    offerSdp = session.offerSdp,
                    onSuccess = { answerSdp ->
                        viewModelScope.launch {
                            val res = repository.answerCallSession(session.id, answerSdp, myName)
                            if (res.isFailure) {
                                cleanupLocalCall(res.exceptionOrNull()?.localizedMessage ?: "ဖုန်းလက်ခံ၍ မရပါ")
                            }
                        }
                    },
                    onError = { err ->
                        viewModelScope.launch {
                            repository.updateCallStatus(session.id, "rejected")
                            cleanupLocalCall(err)
                        }
                    }
                )
            },
            onError = { err ->
                viewModelScope.launch {
                    repository.updateCallStatus(session.id, "rejected")
                    cleanupLocalCall(err)
                }
            }
        )
    }

    fun declineIncomingCall(session: CallSession) {
        _dismissedCallIds.update { it + session.id }
        viewModelScope.launch {
            repository.updateCallStatus(session.id, "rejected")
        }
    }

    fun hangupActiveCall() {
        val current = _activeCall.value ?: return
        cleanupLocalCall(null)
        viewModelScope.launch(NonCancellable) {
            repository.updateCallStatus(current.callId, "ended")
        }
    }

    fun toggleCallMute() {
        val cur = _activeCall.value ?: return
        val next = !cur.isMuted
        callManager?.setMuted(next)
        _activeCall.value = cur.copy(isMuted = next)
    }

    fun toggleCallSpeaker() {
        val cur = _activeCall.value ?: return
        val next = !cur.isSpeakerOn
        callManager?.setSpeakerEnabled(next)
        _activeCall.value = cur.copy(isSpeakerOn = next)
    }

    fun toggleCallCamera() {
        val cur = _activeCall.value ?: return
        if (cur.callType != "video") return
        val next = !cur.isCameraEnabled
        callManager?.setCameraEnabled(next)
        _activeCall.value = cur.copy(isCameraEnabled = next)
    }

    fun switchCallCamera() {
        val cur = _activeCall.value ?: return
        if (cur.callType != "video") return
        callManager?.switchCamera { isFront ->
            _activeCall.update { state -> state?.copy(isFrontCamera = isFront) }
        }
    }

    fun attachLocalVideoRenderer(renderer: SurfaceViewRenderer) {
        callManager?.attachLocalRenderer(renderer)
    }

    fun detachLocalVideoRenderer(renderer: SurfaceViewRenderer) {
        callManager?.detachLocalRenderer(renderer)
    }

    fun attachRemoteVideoRenderer(renderer: SurfaceViewRenderer) {
        callManager?.attachRemoteRenderer(renderer)
    }

    fun detachRemoteVideoRenderer(renderer: SurfaceViewRenderer) {
        callManager?.detachRemoteRenderer(renderer)
    }

    private fun createCallListener(callId: String) = object : WebRtcCallManager.Listener {
        override fun onLocalIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
            val sendNow = synchronized(callSyncLock) {
                if (canSendLocalIce) {
                    true
                } else {
                    pendingLocalIceCandidates.add(Triple(sdpMid, sdpMLineIndex, candidate))
                    false
                }
            }
            if (sendNow) {
                viewModelScope.launch {
                    repository.addCallIceCandidate(callId, sdpMid, sdpMLineIndex, candidate)
                }
            }
        }

        override fun onPeerConnected() {
            _activeCall.update { cur ->
                if (cur != null && cur.callId == callId) {
                    cur.copy(
                        status = "connected",
                        connectedAtEpochMs = cur.connectedAtEpochMs ?: System.currentTimeMillis()
                    )
                } else cur
            }
        }

        override fun onRemoteVideoAvailable(available: Boolean) {
            _activeCall.update { cur ->
                if (cur != null && cur.callId == callId) {
                    cur.copy(hasRemoteVideo = available)
                } else cur
            }
        }

        override fun onPeerConnectionFailed(reason: String) {
            viewModelScope.launch {
                val active = _activeCall.value
                if (active != null && active.callId == callId) {
                    repository.updateCallStatus(callId, "ended")
                    cleanupLocalCall(reason)
                }
            }
        }
    }

    private fun observeActiveCallSignals(callId: String, isOutgoing: Boolean) {
        callCandidateJobs.forEach { it.cancel() }
        callCandidateJobs.clear()

        val sessionJob = viewModelScope.launch {
            repository.observeCallSession(callId)
                .catch { error ->
                    if (isPureCancellation(error)) throw error
                    Log.w(TAG, "observeCallSession error", error)
                }
                .collect { session ->
                    if (session == null) return@collect
                    val active = _activeCall.value ?: return@collect
                    if (active.callId != callId) return@collect

                    if (isOutgoing && session.answeredByName.isNotBlank() && active.peerName != session.answeredByName) {
                        _activeCall.update { it?.copy(peerName = session.answeredByName) }
                    }
                    if (isOutgoing && session.status == "accepted" && session.answerSdp.isNotBlank()) {
                        _activeCall.update { cur ->
                            if (cur != null && cur.status == "calling") cur.copy(status = "connecting") else cur
                        }
                        callManager?.setRemoteAnswer(session.answerSdp)
                    }
                    if (session.status == "rejected" || session.status == "ended") {
                        val msg = if (session.status == "rejected") {
                            "တစ်ဖက်မှ ဖုန်းခေါ်ဆိုမှု ငြင်းပယ်လိုက်ပါသည်"
                        } else {
                            "ဖုန်းခေါ်ဆိုမှု ပြီးဆုံးပါပြီ"
                        }
                        cleanupLocalCall(msg)
                    }
                }
        }

        val iceJob = viewModelScope.launch {
            repository.observeCallCandidates(callId)
                .catch { error ->
                    if (isPureCancellation(error)) throw error
                    Log.w(TAG, "observeCallCandidates error", error)
                }
                .collect { candidates ->
                    candidates.forEach { cand ->
                        if (cand.senderId != currentUserId) {
                            val isNew = synchronized(callSyncLock) { processedCandidateIds.add(cand.id) }
                            if (isNew) {
                                callManager?.addRemoteIceCandidate(
                                    sdpMid = cand.sdpMid,
                                    sdpMLineIndex = cand.sdpMLineIndex,
                                    candidateStr = cand.candidate
                                )
                            }
                        }
                    }
                }
        }

        callCandidateJobs.add(sessionJob)
        callCandidateJobs.add(iceJob)
    }

    private fun cleanupLocalCall(eventMessage: String?) {
        callCandidateJobs.forEach { it.cancel() }
        callCandidateJobs.clear()
        synchronized(callSyncLock) {
            canSendLocalIce = false
            pendingLocalIceCandidates.clear()
            processedCandidateIds.clear()
        }
        callManager?.release()
        _activeCall.value = null
        if (!eventMessage.isNullOrBlank()) {
            emitMessage(eventMessage)
        }
    }

    override fun onCleared() {
        val activeId = _activeCall.value?.callId
        if (activeId != null) {
            kotlinx.coroutines.CoroutineScope(NonCancellable).launch {
                runCatching { repository.updateCallStatus(activeId, "ended") }
            }
        }
        callManager?.release()
        super.onCleared()
    }

    private fun runAction(successMsg: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = block()
                res.onSuccess { _events.emit(successMsg) }
                res.onFailure { err -> _events.emit(err.localizedMessage ?: "Operation failed") }
            } finally {
                _busy.value = false
            }
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch { _events.emit(msg) }
    }

    private fun <T> Flow<T>.toUiState(fallback: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                if (isPureCancellation(error)) throw error
                val msg = unwrapFirestoreMessage(error, fallback)
                Log.w(TAG, fallback, error)
                emit(UiState.Error(msg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)
    }

    private fun isPureCancellation(error: Throwable): Boolean {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return firestoreError == null && error is CancellationException
    }

    private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return (firestoreError ?: error).message ?: fallback
    }

    private companion object {
        const val TAG = "MainVM"
        const val STOP_TIMEOUT_MS = 5000L

        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
