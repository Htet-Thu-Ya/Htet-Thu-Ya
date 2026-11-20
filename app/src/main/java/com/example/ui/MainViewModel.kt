package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.betting.AutoSettleSummary
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.BetOrder
import com.example.data.model.BettingConfig
import com.example.data.model.BoardConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LedgerTransaction
import com.example.data.model.LiveFootballFixture
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WalletConfig
import com.example.data.model.WalletRequest
import com.example.data.network.FootballFeedService
import com.example.data.repository.HtetThuYaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: HtetThuYaRepository,
    val currentUserId: String,
    private val currentUserEmail: String?,
    private val isEmailVerified: Boolean,
    val displayName: String,
    private val feedService: FootballFeedService = FootballFeedService()
) : ViewModel() {

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _liveFeed = MutableStateFlow<UiState<List<LiveFootballFixture>>>(UiState.Loading)
    val liveFeed: StateFlow<UiState<List<LiveFootballFixture>>> = _liveFeed.asStateFlow()

    private val isBootstrappedOwner =
        isEmailVerified && currentUserEmail.equals(HtetThuYaRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)

    private var autoSyncedSession = false

    val adminMember: StateFlow<AdminMember?> = repository.observeAdminMember(currentUserId)
        .catch { error ->
            if (isPureCancellation(error)) throw error
            Log.w(TAG, "Admin membership stream error", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val isAdmin: StateFlow<Boolean> = adminMember
        .map { member -> isBootstrappedOwner || member != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), isBootstrappedOwner)

    val userAccount: StateFlow<UiState<UserAccount?>> = repository.observeUserAccount(currentUserId)
        .toUiState("Failed to load wallet")

    val matches: StateFlow<UiState<List<SportMatch>>> = repository.observePublicMatches(currentUserId)
        .toUiState("Failed to load matches")

    val lotteryDraws: StateFlow<UiState<List<LotteryDraw>>> = repository.observePublicLotteryDraws(currentUserId)
        .toUiState("Failed to load 3D rounds")

    val userOrders: StateFlow<UiState<List<BetOrder>>> = repository.observeUserOrders(currentUserId)
        .toUiState("Failed to load orders")

    val userWalletRequests: StateFlow<UiState<List<WalletRequest>>> = repository.observeUserWalletRequests(currentUserId)
        .toUiState("Failed to load requests")

    val userTransactions: StateFlow<UiState<List<LedgerTransaction>>> = repository.observeUserTransactions(currentUserId)
        .toUiState("Failed to load ledger")

    val leaderboard: StateFlow<UiState<List<LeaderboardEntry>>> = repository.observeLeaderboard(currentUserId)
        .toUiState("Failed to load standings")

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

    // Admin-only live streams (only attached when user is actually an Admin!)
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

    val adminInvites: StateFlow<UiState<List<AdminInvite>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAdminInvites(currentUserId).map<List<AdminInvite>, UiState<List<AdminInvite>>> { UiState.Success(it) }
                .catch { emit(UiState.Error(unwrapFirestoreMessage(it, "Failed to load invite codes"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val pendingAdminBadgeCount: StateFlow<Int> = combine(allOrders, allWalletRequests) { ordState, reqState ->
        val pendingOrders = (ordState as? UiState.Success)?.data?.count { it.status == "pending" } ?: 0
        val pendingReqs = (reqState as? UiState.Success)?.data?.count { it.status == "pending" } ?: 0
        pendingOrders + pendingReqs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

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
        viewModelScope.launch {
            combine(isAdmin, boardConfig) { admin, cfg -> admin to cfg }
                .collect { (admin, cfg) ->
                    if (admin && !autoSyncedSession) {
                        autoSyncedSession = true
                        val today = feedService.todayDateIso()
                        if (cfg.autoSyncDailyMatches && cfg.lastSyncedDate != today) {
                            repository.syncDailyMatchesFromFeed(feedService, listOf(0, 1), cfg)
                        } else if (cfg.autoSyncLiveResults) {
                            repository.syncLiveMatchResults(feedService)
                        }
                    }
                }
        }
    }

    fun refreshLiveFootballFeed() {
        viewModelScope.launch {
            _liveFeed.value = UiState.Loading
            val res = feedService.fetchFixturesForDays(listOf(-1, 0, 1))
            res.onSuccess { fixtures ->
                _liveFeed.value = UiState.Success(fixtures)
            }.onFailure { err ->
                _liveFeed.value = UiState.Error(err.localizedMessage ?: "တိုက်ရိုက်ရလဒ် ဆွဲယူမရပါ")
            }
        }
    }

    fun setAdminMode(enabled: Boolean) {
        if (enabled && !isAdmin.value) {
            emitMessage("Admin ခွင့်ပြုချက် မရှိသေးပါ")
            return
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
                val feedRes = feedService.fetchFixturesForDays(listOf(-1, 0, 1))
                feedRes.onSuccess { fixtures ->
                    _liveFeed.value = UiState.Success(fixtures)
                }
                var msg = "ဒေတာအသစ်များ Refresh လုပ်ပြီးပါပြီ"
                if (isAdmin.value) {
                    var syncedMatches = 0
                    if (boardConfig.value.autoSyncLiveResults) {
                        syncedMatches = repository.syncLiveMatchResults(feedService).getOrDefault(0)
                    }
                    if (bettingConfig.value.autoSettleOrders) {
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

    fun placeOrder(
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        pickKeys: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) = runAction("အော်ဒါတင်ပြီးပါပြီ (Admin စစ်ဆေးပေးပါမည်)") {
        val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
        val res = repository.placeOrder(name, orderType, selections, matchIds, drawRound, stakeKs, multiplier, pickKeys)
        if (res.isSuccess) onSuccess()
        res.map { }
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
        onSuccess: () -> Unit = {}
    ) = runAction("တောင်းဆိုမှု ပေးပို့ပြီးပါပြီ") {
        val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
        val res = repository.submitWalletRequest(name, requestType, amountKs, channel, referenceNote, walletConfig.value)
        if (res.isSuccess) onSuccess()
        res
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

    // --- Admin Commands ---
    fun acceptOrder(orderId: String, note: String) = runAction("အော်ဒါ လက်ခံအတည်ပြုပြီးပါပြီ") {
        repository.acceptOrder(orderId, note)
    }

    fun settleOrderWon(
        order: BetOrder,
        note: String,
        customCommissionPercent: Double? = null,
        customGrossPayoutKs: Long? = null
    ) = runAction("အနိုင်ရ အော်ဒါ ငွေရှင်းပြီးပါပြီ") {
        repository.settleOrderWon(order, note, bettingConfig.value, customCommissionPercent, customGrossPayoutKs)
    }

    fun settleOrderLost(order: BetOrder, note: String) = runAction("အော်ဒါ စာရင်းပိတ်ပြီးပါပြီ") {
        repository.settleOrderLost(order, note)
    }

    fun rejectOrder(order: BetOrder, note: String) = runAction("အော်ဒါ ပယ်ဖျက်ပြီး ငွေပြန်အမ်းပြီးပါပြီ") {
        repository.rejectOrder(order, note)
    }

    fun processWalletRequest(req: WalletRequest, approve: Boolean, adminNote: String) =
        runAction(if (approve) "ငွေစာရင်း အတည်ပြုပြီးပါပြီ" else "တောင်းဆိုမှု ပယ်ဖျက်ပြီးပါပြီ") {
            repository.processWalletRequest(req, approve, adminNote, walletConfig.value)
        }

    fun saveWalletConfig(
        depositPercent: Double,
        depositMode: String,
        withdrawPercent: Double,
        withdrawMode: String
    ) = runAction("ငွေသွင်း / ငွေထုတ် ရာခိုင်နှုန်း သိမ်းဆည်းပြီးပါပြီ") {
        repository.saveWalletConfig(depositPercent, depositMode, withdrawPercent, withdrawMode)
    }

    fun saveBettingConfig(
        winCommissionPercent: Double,
        winCommissionBasis: String,
        autoSettleOrders: Boolean
    ) = runAction("အနိုင်အကောက် (%) နှင့် အလိုအလျောက်စနစ် သိမ်းဆည်းပြီးပါပြီ") {
        repository.saveBettingConfig(winCommissionPercent, winCommissionBasis, autoSettleOrders)
    }

    fun autoSettleFinishedOrdersNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.autoSettleFinishedOrders(bettingConfig.value)
                res.onSuccess { summary ->
                    _events.emit(formatAutoSettleMessage(summary))
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
                val res = repository.autoSettleFinishedOrders(bettingConfig.value, targetUserId)
                res.onSuccess { summary ->
                    val prefix = if (customerName.isNotBlank()) "$customerName: " else ""
                    _events.emit(prefix + formatAutoSettleMessage(summary))
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
                val res = repository.autoSettleSingleOrder(order, bettingConfig.value)
                res.onSuccess { msg ->
                    _events.emit(msg)
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
        val res = repository.saveBoardConfig(enabled, cur.autoSyncLiveResults, cur.lastSyncedDate)
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
        val res = repository.saveBoardConfig(cur.autoSyncDailyMatches, enabled, cur.lastSyncedDate)
        if (res.isSuccess && enabled) {
            repository.syncLiveMatchResults(feedService)
        }
        res
    }

    fun syncDailyMatchesNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.syncDailyMatchesFromFeed(feedService, listOf(0, 1), boardConfig.value)
                res.onSuccess { count ->
                    if (count > 0) {
                        _events.emit("ယနေ့/မနက်ဖြန် ဘောလုံးပွဲစဉ် $count ပွဲ အလိုအလျောက်ထည့်သွင်းပြီးပါပြီ")
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

    fun syncLiveMatchResultsNow() {
        viewModelScope.launch {
            _busy.value = true
            try {
                val res = repository.syncLiveMatchResults(feedService)
                res.onSuccess { updated ->
                    val settledCount = if (bettingConfig.value.autoSettleOrders) {
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
        onSuccess: () -> Unit = {}
    ) = runAction("ပွဲစဉ်အသစ် ထည့်သွင်းပြီးပါပြီ") {
        val res = repository.saveNewMatch(league, homeTeam, awayTeam, kickoffLabel, handicapLine, goalLine, bodyOdds, goalOdds)
        if (res.isSuccess) onSuccess()
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
        awayScore: Long
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
                    awayScore = awayScore
                )
                res.onSuccess {
                    if (status == "finished" && bettingConfig.value.autoSettleOrders) {
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
        repository.deleteMatch(matchId)
    }

    fun saveNewLotteryDraw(roundTitle: String, closeTimeLabel: String, payoutMultiplier: Double, onSuccess: () -> Unit = {}) =
        runAction("၃လုံးထီ အကြိမ်သစ် ဖွင့်လှစ်ပြီးပါပြီ") {
            val res = repository.saveNewLotteryDraw(roundTitle, closeTimeLabel, payoutMultiplier)
            if (res.isSuccess) onSuccess()
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
                    if (status == "finished" && winningNumber.length == 3 && bettingConfig.value.autoSettleOrders) {
                        val settled = repository.autoSettleFinishedOrders(bettingConfig.value).getOrNull()?.settledCount ?: 0
                        if (settled > 0) {
                            _events.emit("၃လုံးထီသိမ်းဆည်းပြီး အော်ဒါ $settled ခု အလိုအလျောက် ငွေရှင်းပြီးပါပြီ")
                            return@onSuccess
                        }
                    }
                    _events.emit("၃လုံးထီ အချက်အလက် ပြင်ဆင်ပြီးပါပြီ")
                }.onFailure { err ->
                    _events.emit(err.localizedMessage ?: "Operation failed")
                }
            } finally {
                _busy.value = false
            }
        }
    }

    fun deleteLotteryDraw(drawId: String) = runAction("၃လုံးထီ အကြိမ် ဖျက်ပြီးပါပြီ") {
        repository.deleteLotteryDraw(drawId)
    }

    fun deleteWalletRequest(requestId: String) = runAction("ငွေစာရင်းအဟောင်း ဖျက်ပြီးပါပြီ") {
        repository.deleteWalletRequest(requestId)
    }

    fun clearProcessedWalletRequests(requestIds: List<String>) = runAction("ပြီးစီးပြီးသား ငွေသွင်း/ထုတ် အဟောင်းများ ဖျက်ပြီးပါပြီ") {
        repository.clearProcessedWalletRequests(requestIds).map { }
    }

    fun createAdminInvite(label: String, onCodeCreated: (String) -> Unit = {}) =
        runAction("သူငယ်ချင်း Admin Invite Code ဖန်တီးပြီးပါပြီ") {
            val res = repository.createAdminInvite(label)
            res.onSuccess { onCodeCreated(it) }
            res.map { }
        }

    fun toggleAdminInvite(code: String, active: Boolean) = runAction("Invite Code အခြေအနေ ပြောင်းလဲပြီးပါပြီ") {
        repository.toggleAdminInvite(code, active)
    }

    fun removeAdminMember(adminUid: String) = runAction("Admin စာရင်းမှ ဖယ်ရှားပြီးပါပြီ") {
        repository.removeAdminMember(adminUid)
    }

    fun adjustUserBalance(targetUid: String, deltaKs: Long, note: String) = runAction("Customer လက်ကျန်ငွေ ပြင်ဆင်ပြီးပါပြီ") {
        repository.adjustUserBalance(targetUid, deltaKs, note)
    }

    fun giftRewardPoints(targetUid: String, targetName: String, deltaPoints: Long, note: String) =
        runAction(
            if (deltaPoints >= 0L) "Customer ထံ လက်ဆောင်ပွိုင့် ပေးပို့ပြီးပါပြီ"
            else "Customer ထံမှ ပွိုင့်ပြန်နှုတ်ပြီးပါပြီ"
        ) {
            repository.giftRewardPoints(targetUid, targetName, deltaPoints, note)
        }

    fun seedSampleBoard() = runAction("နမူနာ ပွဲစဉ်များနှင့် ၃လုံးထီ ထည့်သွင်းပြီးပါပြီ") {
        repository.seedSampleBoard()
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
    }
}
