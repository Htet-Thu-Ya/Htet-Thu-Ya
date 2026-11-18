package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.BetOrder
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LedgerTransaction
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WalletRequest
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
    val displayName: String
) : ViewModel() {

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val isBootstrappedOwner =
        isEmailVerified && currentUserEmail.equals(HtetThuYaRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)

    init {
        viewModelScope.launch {
            val result = repository.ensureUserInitialized(currentUserId, displayName)
            result.onFailure { err ->
                _events.emit(err.localizedMessage ?: "Account setup error")
            }
        }
    }

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

    fun setAdminMode(enabled: Boolean) {
        if (enabled && !isAdmin.value) {
            emitMessage("Admin ခွင့်ပြုချက် မရှိသေးပါ")
            return
        }
        _isAdminMode.value = enabled
    }

    fun placeOrder(
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        onSuccess: () -> Unit = {}
    ) = runAction("အော်ဒါတင်ပြီးပါပြီ (Admin စစ်ဆေးပေးပါမည်)") {
        val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
        val res = repository.placeOrder(name, orderType, selections, matchIds, drawRound, stakeKs, multiplier)
        if (res.isSuccess) onSuccess()
        res.map { }
    }

    fun submitWalletRequest(
        requestType: String,
        amountKs: Long,
        channel: String,
        referenceNote: String,
        onSuccess: () -> Unit = {}
    ) = runAction("တောင်းဆိုမှု ပေးပို့ပြီးပါပြီ") {
        val name = (userAccount.value as? UiState.Success)?.data?.displayName ?: displayName
        val res = repository.submitWalletRequest(name, requestType, amountKs, channel, referenceNote)
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

    fun settleOrderWon(order: BetOrder, note: String) = runAction("အနိုင်ရ အော်ဒါ ငွေရှင်းပြီးပါပြီ") {
        repository.settleOrderWon(order, note)
    }

    fun settleOrderLost(order: BetOrder, note: String) = runAction("အော်ဒါ စာရင်းပိတ်ပြီးပါပြီ") {
        repository.settleOrderLost(order, note)
    }

    fun rejectOrder(order: BetOrder, note: String) = runAction("အော်ဒါ ပယ်ဖျက်ပြီး ငွေပြန်အမ်းပြီးပါပြီ") {
        repository.rejectOrder(order, note)
    }

    fun processWalletRequest(req: WalletRequest, approve: Boolean, note: String) =
        runAction(if (approve) "ငွေစာရင်း အတည်ပြုပြီးပါပြီ" else "တောင်းဆိုမှု ပယ်ဖျက်ပြီးပါပြီ") {
            repository.processWalletRequest(req, approve, note)
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

    fun updateMatchStatusAndScore(matchId: String, status: String, homeScore: Long, awayScore: Long) =
        runAction("ပွဲစဉ် ရလဒ်ပြင်ဆင်ပြီးပါပြီ") {
            repository.updateMatchStatusAndScore(matchId, status, homeScore, awayScore)
        }

    fun saveNewLotteryDraw(roundTitle: String, closeTimeLabel: String, payoutMultiplier: Double, onSuccess: () -> Unit = {}) =
        runAction("၃လုံးထီ အကြိမ်သစ် ဖွင့်လှစ်ပြီးပါပြီ") {
            val res = repository.saveNewLotteryDraw(roundTitle, closeTimeLabel, payoutMultiplier)
            if (res.isSuccess) onSuccess()
            res
        }

    fun publishLotteryResult(drawId: String, winningNumber: String, status: String) =
        runAction("၃လုံးထီ ရလဒ် ထုတ်ပြန်ပြီးပါပြီ") {
            repository.publishLotteryResult(drawId, winningNumber, status)
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
