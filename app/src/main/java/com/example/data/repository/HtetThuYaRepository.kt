package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.betting.AutoSettleDecision
import com.example.data.betting.AutoSettleEngine
import com.example.data.betting.AutoSettleSummary
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
import com.example.data.model.CallIceCandidate
import com.example.data.model.CallSession
import com.example.data.model.DirectChatMessage
import com.example.data.model.GroupChatMessage
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LedgerTransaction
import com.example.data.model.LiveFootballFixture
import com.example.data.model.LotteryDraw
import com.example.data.model.MiniGameControlConfig
import com.example.data.model.MiniGameRound
import com.example.data.model.PaymentAccount
import com.example.data.model.SecurityConfig
import com.example.data.model.SportMatch
import com.example.data.model.SupportAgentProfile
import com.example.data.model.SystemGameConfig
import com.example.data.model.UserAccount
import com.example.data.model.WalletConfig
import com.example.data.model.WalletRequest
import com.example.data.network.FootballFeedService
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToLong

class HtetThuYaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "onimusha.033310@gmail.com"
        private val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.wrapFirestoreCatch(operationType: OperationType, path: String, expectedUid: String): Flow<T> {
        return catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
                handleFirestoreError(exceptionToLog, operationType, path)
            }
            throw error
        }
    }

    suspend fun ensureUserInitialized(
        userId: String,
        rawDisplayName: String,
        rawEmail: String = ""
    ): Result<Unit> {
        val userPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val safeName = rawDisplayName.trim().ifBlank { "Member" }.take(60)
            val safeEmail = rawEmail.trim().take(120)
            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val batch = db.batch()
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "email" to safeEmail,
                        "balanceKs" to 0L,
                        "rewardPoints" to 0L,
                        "activeStakeKs" to 0L,
                        "totalWonKs" to 0L,
                        "isOnline" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "isLocked" to false,
                        "forceLogoutAtMs" to 0L,
                        "securityPinHash" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    lbRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "rewardPoints" to 0L,
                        "totalWonKs" to 0L,
                        "visibility" to "public",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            } else {
                val existingEmail = userSnap.getString("email").orEmpty()
                val updateMap = mutableMapOf<String, Any>(
                    "isOnline" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeEmail.isNotBlank() && existingEmail != safeEmail) {
                    updateMap["email"] = safeEmail
                }
                userRef.update(updateMap).await()

                val lbSnap = lbRef.get().await()
                if (!lbSnap.exists()) {
                    val pts = userSnap.getLong("rewardPoints") ?: 0L
                    val won = userSnap.getLong("totalWonKs") ?: 0L
                    lbRef.set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to safeName,
                            "rewardPoints" to pts,
                            "totalWonKs" to won,
                            "visibility" to "public",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // Auto-register bootstrapped owner into admins collection if applicable
            val current = auth.currentUser
            if (current != null && current.isEmailVerified && current.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)) {
                val adminRef = db.collection("admins").document(uid)
                val adminSnap = adminRef.get().await()
                if (!adminSnap.exists()) {
                    adminRef.set(
                        mapOf(
                            "userId" to uid,
                            "displayName" to safeName,
                            "email" to safeEmail,
                            "role" to "owner",
                            "status" to "active",
                            "addedBy" to uid,
                            "inviteCodeUsed" to "OWNER",
                            "pinHash" to "",
                            "forceLogoutAtMs" to 0L,
                            "permissions" to AdminPermissions.ALL_GRANTED.toMap(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    fun observeAdminMember(userId: String): Flow<AdminMember?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(AdminMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .wrapFirestoreCatch(OperationType.GET, path, uid)
        )
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .wrapFirestoreCatch(OperationType.GET, path, uid)
        )
    }

    fun observePublicMatches(userId: String): Flow<List<SportMatch>> = flow {
        val path = "matches"
        emitAll(
            db.collection("matches")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SportMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, userId)
        )
    }

    fun observePublicLotteryDraws(userId: String): Flow<List<LotteryDraw>> = flow {
        val path = "lottery_draws"
        emitAll(
            db.collection("lottery_draws")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LotteryDraw::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, userId)
        )
    }

    fun observeUserOrders(userId: String): Flow<List<BetOrder>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(BetOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeUserWalletRequests(userId: String): Flow<List<WalletRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "wallet_requests"
        emitAll(
            db.collection("wallet_requests")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeUserTransactions(userId: String): Flow<List<LedgerTransaction>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "transactions"
        emitAll(
            db.collection("transactions")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LedgerTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeLeaderboard(userId: String): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<LeaderboardEntry> { it.totalWonKs }.thenByDescending { it.rewardPoints })
                }
                .wrapFirestoreCatch(OperationType.LIST, path, userId)
        )
    }

    fun observeWalletConfig(userId: String): Flow<WalletConfig> = flow {
        val path = "app_settings/wallet_config"
        emitAll(
            db.collection("app_settings").document("wallet_config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(WalletConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: WalletConfig()
                    } else WalletConfig()
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeBoardConfig(userId: String): Flow<BoardConfig> = flow {
        val path = "app_settings/board_config"
        emitAll(
            db.collection("app_settings").document("board_config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(BoardConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: BoardConfig()
                    } else BoardConfig()
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeBettingConfig(userId: String): Flow<BettingConfig> = flow {
        val path = "app_settings/betting_config"
        emitAll(
            db.collection("app_settings").document("betting_config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(BettingConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: BettingConfig()
                    } else BettingConfig()
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeAppUpdateConfig(userId: String): Flow<AppUpdateConfig?> = flow {
        val path = "app_settings/app_update"
        emitAll(
            db.collection("app_settings").document("app_update")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(AppUpdateConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeDirectMessagesForUser(userId: String): Flow<List<DirectChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "direct_messages"
        emitAll(
            db.collection("direct_messages")
                .whereEqualTo("threadUserId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeAllDirectMessages(userId: String): Flow<List<DirectChatMessage>> = flow {
        val path = "direct_messages"
        emitAll(
            db.collection("direct_messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, userId)
        )
    }

    fun observeGroupMessages(userId: String): Flow<List<GroupChatMessage>> = flow {
        val path = "group_messages"
        emitAll(
            db.collection("group_messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(Long.MAX_VALUE / 2, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, userId)
        )
    }

    fun observeAppReviews(userId: String): Flow<List<AppQualityReview>> = flow {
        val path = "app_reviews"
        emitAll(
            db.collection("app_reviews")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppQualityReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, userId)
        )
    }

    // --- ADMIN STREAMS ---
    fun observeAllOrders(userId: String): Flow<List<BetOrder>> = flow {
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BetOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "orders", userId)
        )
    }

    fun observeAllWalletRequests(userId: String): Flow<List<WalletRequest>> = flow {
        emitAll(
            db.collection("wallet_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "wallet_requests", userId)
        )
    }

    fun observeAllUsers(userId: String): Flow<List<UserAccount>> = flow {
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "users", userId)
        )
    }

    fun observeAdminMembers(userId: String): Flow<List<AdminMember>> = flow {
        emitAll(
            db.collection("admins")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.role != "owner" }
                }
                .wrapFirestoreCatch(OperationType.LIST, "admins", userId)
        )
    }

    fun observeAdminInvites(userId: String): Flow<List<AdminInvite>> = flow {
        emitAll(
            db.collection("admin_invites")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "admin_invites", userId)
        )
    }

    fun observePaymentAccounts(userId: String, isAdminViewer: Boolean): Flow<List<PaymentAccount>> = flow {
        val query = if (isAdminViewer) {
            db.collection("payment_accounts")
        } else {
            db.collection("payment_accounts").whereEqualTo("active", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(PaymentAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.providerName }
                }
                .wrapFirestoreCatch(OperationType.LIST, "payment_accounts", userId)
        )
    }

    fun observeAuditLogs(userId: String, canViewAll: Boolean): Flow<List<AuditLogEntry>> = flow {
        val uid = requireUserId()
        val query = if (canViewAll) {
            db.collection("audit_logs")
        } else {
            db.collection("audit_logs").whereEqualTo("actorId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(AuditLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "audit_logs", userId)
        )
    }

    fun observeSystemGameConfig(userId: String): Flow<SystemGameConfig> = flow {
        val path = "app_settings/system_config"
        emitAll(
            db.collection("app_settings").document("system_config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(SystemGameConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: SystemGameConfig()
                    } else SystemGameConfig()
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeSecurityConfig(userId: String): Flow<SecurityConfig> = flow {
        val path = "app_settings/security_config"
        emitAll(
            db.collection("app_settings").document("security_config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(SecurityConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: SecurityConfig()
                    } else SecurityConfig()
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeUserMiniGameRounds(userId: String): Flow<List<MiniGameRound>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        emitAll(
            db.collection("mini_game_rounds")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MiniGameRound::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "mini_game_rounds", uid)
        )
    }

    fun observeAllMiniGameRounds(userId: String): Flow<List<MiniGameRound>> = flow {
        emitAll(
            db.collection("mini_game_rounds")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MiniGameRound::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "mini_game_rounds", userId)
        )
    }

    fun observeAllTransactions(userId: String): Flow<List<LedgerTransaction>> = flow {
        emitAll(
            db.collection("transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LedgerTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, "transactions", userId)
        )
    }

    fun observeBrandingConfig(userId: String): Flow<BrandingConfig> = flow {
        val path = "app_settings/branding_config"
        emitAll(
            db.collection("app_settings").document("branding_config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(BrandingConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: BrandingConfig()
                    } else BrandingConfig()
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeMiniGameControlConfig(userId: String): Flow<MiniGameControlConfig> = flow {
        val path = "app_settings/mini_game_control"
        emitAll(
            db.collection("app_settings").document("mini_game_control")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(MiniGameControlConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: MiniGameControlConfig()
                    } else MiniGameControlConfig()
                }
                .wrapFirestoreCatch(OperationType.GET, path, userId)
        )
    }

    fun observeSupportAgents(userId: String): Flow<List<SupportAgentProfile>> = flow {
        emitAll(
            db.collection("support_agents")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SupportAgentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<SupportAgentProfile> { it.role != "owner" }.thenBy { it.displayName })
                }
                .wrapFirestoreCatch(OperationType.LIST, "support_agents", userId)
        )
    }

    // --- CUSTOMER MUTATIONS ---

    suspend fun placeOrder(
        customerName: String,
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        pickKeys: List<String> = emptyList(),
        autoAccept: Boolean = false,
        matchKickoffMs: Long = 0L,
        scanAtMs: Long = 0L
    ): Result<String> {
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val orderPath = "orders/$orderId"
        return try {
            val uid = requireUserId()
            require(orderType in listOf("parlay", "body_goal", "three_d", "two_d")) { "Invalid order type" }
            require(selections.isNotEmpty() && selections.size <= 30) { "Select 1 to 30 picks" }
            require(stakeKs in 100L..10_000_000L) { "Stake must be between 100 and 10,000,000 Ks" }
            require(multiplier in 1.0..1000.0) { "Invalid multiplier" }

            val safeName = customerName.trim().ifBlank { "Member" }.take(60)
            val safeSelections = selections.map { it.take(120) }
            val safeMatchIds = matchIds.take(15).map { it.take(128) }
            val safePickKeys = pickKeys.take(15).map { it.take(20) }
            val safeRound = drawRound.take(60)
            val potentialPayout = (stakeKs * multiplier).roundToLong().coerceAtLeast(stakeKs)

            val userRef = db.collection("users").document(uid)
            val orderRef = db.collection("orders").document(orderId)
            val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val txRef = db.collection("transactions").document(txId)

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val curBalance = userSnap.getLong("balanceKs") ?: 0L
                val curActive = userSnap.getLong("activeStakeKs") ?: 0L
                if (curBalance < stakeKs) {
                    throw IllegalStateException("လက်ကျန်ငွေ မလုံလောက်ပါ (လိုအပ်ငွေ: $stakeKs Ks)")
                }
                val newBalance = curBalance - stakeKs
                val newActive = curActive + stakeKs

                tx.update(
                    userRef,
                    mapOf(
                        "balanceKs" to newBalance,
                        "activeStakeKs" to newActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    orderRef,
                    mapOf(
                        "userId" to uid,
                        "customerName" to safeName,
                        "orderType" to orderType,
                        "selections" to safeSelections,
                        "matchIds" to safeMatchIds,
                        "pickKeys" to safePickKeys,
                        "drawRound" to safeRound,
                        "stakeKs" to stakeKs,
                        "multiplier" to multiplier,
                        "potentialPayoutKs" to potentialPayout,
                        "actualPayoutKs" to 0L,
                        "commissionPercent" to 0.0,
                        "commissionKs" to 0L,
                        "status" to if (autoAccept) "accepted" else "pending",
                        "adminNote" to "",
                        "settledBy" to "",
                        "isHidden" to false,
                        "matchKickoffMs" to matchKickoffMs.coerceAtLeast(0L),
                        "scanAtMs" to scanAtMs.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "order_stake",
                        "direction" to "debit",
                        "amountKs" to stakeKs,
                        "title" to "${orderTypeLabel(orderType)} အော်ဒါတင်ခြင်း (${safeSelections.first()})",
                        "balanceAfterKs" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                orderId
            }.await()
            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderPath)
            Result.failure(e)
        }
    }

    suspend fun submitWalletRequest(
        customerName: String,
        requestType: String,
        amountKs: Long,
        channel: String,
        referenceNote: String,
        walletConfig: WalletConfig = WalletConfig()
    ): Result<Unit> {
        val reqId = "wr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val reqPath = "wallet_requests/$reqId"
        return try {
            val uid = requireUserId()
            require(requestType in listOf("topup", "withdraw")) { "Invalid request type" }
            require(amountKs in 500L..10_000_000L) { "ပမာဏသည် 500 Ks မှ 10,000,000 Ks အတွင်း ဖြစ်ရပါမည်" }
            val safeChannel = channel.trim().take(40)
            val safeRef = referenceNote.trim().take(120)
            require(safeChannel.isNotBlank()) { "ငွေပေးချေမှုနည်းလမ်း ရွေးချယ်ပါ" }
            require(safeRef.isNotBlank()) { "အထောက်အထား / ဖုန်းနံပါတ် ထည့်သွင်းပါ" }
            val safeName = customerName.trim().ifBlank { "Member" }.take(60)

            val ratePercent = if (requestType == "topup") walletConfig.depositPercent else walletConfig.withdrawPercent
            val rateMode = if (ratePercent <= 0.0) "none" else if (requestType == "topup") walletConfig.depositMode else walletConfig.withdrawMode
            val netAmountKs = if (requestType == "topup") walletConfig.computeNetTopup(amountKs) else walletConfig.computeNetWithdraw(amountKs)

            val reqRef = db.collection("wallet_requests").document(reqId)
            val payload = mapOf(
                "userId" to uid,
                "customerName" to safeName,
                "requestType" to requestType,
                "amountKs" to amountKs,
                "channel" to safeChannel,
                "referenceNote" to safeRef,
                "status" to "pending",
                "adminNote" to "",
                "processedBy" to "",
                "ratePercent" to ratePercent.coerceIn(0.0, 100.0),
                "rateMode" to if (rateMode in listOf("none", "bonus", "fee")) rateMode else "none",
                "netAmountKs" to netAmountKs.coerceIn(0L, 100_000_000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (requestType == "withdraw") {
                val userRef = db.collection("users").document(uid)
                val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")
                db.runTransaction { tx ->
                    val userSnap = tx.get(userRef)
                    val curBalance = userSnap.getLong("balanceKs") ?: 0L
                    if (curBalance < amountKs) {
                        throw IllegalStateException("လက်ကျန်ငွေ မလုံလောက်ပါ")
                    }
                    val newBalance = curBalance - amountKs
                    tx.update(
                        userRef,
                        mapOf(
                            "balanceKs" to newBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(reqRef, payload)
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to uid,
                            "txType" to "withdraw",
                            "direction" to "debit",
                            "amountKs" to amountKs,
                            "title" to "ငွေထုတ်တောင်းဆိုခြင်း ($safeChannel • ရမည့်ငွေ ${netAmountKs} Ks)",
                            "balanceAfterKs" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }.await()
            } else {
                reqRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reqPath)
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank() && ID_REGEX.matches(orderId)) { "Invalid order ID" }
            db.collection("orders").document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearCompletedOrders(orderIds: List<String>): Result<Int> {
        val path = "orders"
        return try {
            requireUserId()
            val validIds = orderIds.filter { it.isNotBlank() && ID_REGEX.matches(it) }
            if (validIds.isEmpty()) return Result.success(0)
            validIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(db.collection("orders").document(id))
                }
                batch.commit().await()
            }
            Result.success(validIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun exchangePoints(pointsToExchange: Long): Result<Long> {
        val path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(pointsToExchange >= 10L) { "အနည်းဆုံး 10 ပွိုင့်မှ စတင်လဲလှယ်နိုင်ပါသည်" }
            val creditKs = pointsToExchange * 10L

            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val lbSnap = tx.get(lbRef)
                val curPoints = userSnap.getLong("rewardPoints") ?: 0L
                val curBalance = userSnap.getLong("balanceKs") ?: 0L
                if (curPoints < pointsToExchange) {
                    throw IllegalStateException("ပွိုင့် မလုံလောက်ပါ (လက်ရှိ: $curPoints Pts)")
                }
                val newPoints = curPoints - pointsToExchange
                val newBalance = curBalance + creditKs

                tx.update(
                    userRef,
                    mapOf(
                        "balanceKs" to newBalance,
                        "rewardPoints" to newPoints,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "rewardPoints" to newPoints,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "point_exchange",
                        "direction" to "credit",
                        "amountKs" to creditKs,
                        "title" to "ပွိုင့် $pointsToExchange Pts ကို $creditKs Ks သို့ လဲလှယ်ခြင်း",
                        "balanceAfterKs" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                creditKs
            }.await()
            Result.success(creditKs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOnlinePresence(isOnline: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerSecurityPin(pinHash: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid"
        return try {
            val verifiedUid = requireUserId()
            db.collection("users").document(verifiedUid).update(
                mapOf(
                    "securityPinHash" to pinHash.take(128),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun playMiniGameRound(
        customerName: String,
        gameType: String,
        betLabel: String,
        outcomeLabel: String,
        stakeKs: Long,
        payoutKs: Long
    ): Result<MiniGameRound> {
        val roundId = "rnd_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "mini_game_rounds/$roundId"
        return try {
            val uid = requireUserId()
            require(gameType in listOf("dice", "six_animals")) { "Invalid game type" }
            require(stakeKs in 100L..1_000_000L) { "လောင်းကြေးသည် 100 Ks မှ 1,000,000 Ks အတွင်း ဖြစ်ရပါမည်" }
            require(payoutKs in 0L..(stakeKs * 100L)) { "Invalid payout" }

            val userRef = db.collection("users").document(uid)
            val roundRef = db.collection("mini_game_rounds").document(roundId)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            val userSnap = userRef.get().await()
            val curBalance = userSnap.getLong("balanceKs") ?: 0L
            val curWon = userSnap.getLong("totalWonKs") ?: 0L
            val locked = userSnap.getBoolean("isLocked") == true
            if (locked) throw IllegalStateException("သင့်အကောင့် ယာယီပိတ်ထားပါသည်")
            if (curBalance < stakeKs) {
                throw IllegalStateException("လက်ကျန်ငွေ မလုံလောက်ပါ (လိုအပ်ငွေ: $stakeKs Ks)")
            }

            val newBalance = (curBalance - stakeKs + payoutKs).coerceAtLeast(0L)
            val newWon = curWon + payoutKs
            val safeName = customerName.trim().ifBlank { "Member" }.take(60)
            val safeBet = betLabel.trim().ifBlank { gameType }.take(120)
            val safeOutcome = outcomeLabel.trim().ifBlank { "-" }.take(120)

            val batch = db.batch()
            batch.set(
                roundRef,
                mapOf(
                    "userId" to uid,
                    "customerName" to safeName,
                    "gameType" to gameType,
                    "betLabel" to safeBet,
                    "outcomeLabel" to safeOutcome,
                    "stakeKs" to stakeKs,
                    "payoutKs" to payoutKs,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "balanceKs" to newBalance,
                    "totalWonKs" to newWon,
                    "lastRoundId" to roundId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val gameLabel = if (gameType == "dice") "အံစာတုံး" else "၆ကောင်ဂျင်"
            if (payoutKs > 0L) {
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "mini_game_win",
                        "direction" to "credit",
                        "amountKs" to payoutKs,
                        "title" to "$gameLabel အနိုင် ($safeBet • $safeOutcome)".take(120),
                        "balanceAfterKs" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "mini_game_stake",
                        "direction" to "debit",
                        "amountKs" to stakeKs,
                        "title" to "$gameLabel ($safeBet • $safeOutcome)".take(120),
                        "balanceAfterKs" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            Result.success(
                MiniGameRound(
                    id = roundId,
                    userId = uid,
                    customerName = safeName,
                    gameType = gameType,
                    betLabel = safeBet,
                    outcomeLabel = safeOutcome,
                    stakeKs = stakeKs,
                    payoutKs = payoutKs
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun redeemAdminInvite(displayName: String, rawCode: String): Result<Unit> {
        val code = rawCode.trim().uppercase()
        val invitePath = "admin_invites/$code"
        return try {
            val uid = requireUserId()
            require(code.length in 4..32 && ID_REGEX.matches(code)) { "Invite Code ပုံစံ မမှန်ကန်ပါ" }

            val inviteSnap = db.collection("admin_invites").document(code).get().await()
            if (!inviteSnap.exists() || inviteSnap.getBoolean("active") != true) {
                throw IllegalArgumentException("Invite Code မရှိပါ သို့မဟုတ် ပိတ်ထားပါသည်")
            }
            val assignedEmail = inviteSnap.getString("assignedEmail").orEmpty().trim()
            val curEmail = auth.currentUser?.email.orEmpty().trim()
            if (assignedEmail.isNotBlank() && !curEmail.equals(assignedEmail, ignoreCase = true)) {
                throw IllegalArgumentException("ဤ Invite Code သည် $assignedEmail အတွက်သာ ဖြစ်ပါသည်")
            }

            val addedBy = inviteSnap.getString("createdBy") ?: uid
            val safeName = displayName.trim().ifBlank { "Second Admin" }.take(60)
            val initialPinHash = inviteSnap.getString("initialPinHash").orEmpty()
            @Suppress("UNCHECKED_CAST")
            val defaultPerms = (inviteSnap.get("defaultPermissions") as? Map<String, Boolean>)
                ?: AdminPermissions.PRESET_WALLET_CASHIER.toMap()

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to safeName,
                "email" to curEmail.take(120),
                "role" to "admin",
                "status" to "active",
                "addedBy" to addedBy,
                "inviteCodeUsed" to code,
                "pinHash" to initialPinHash.take(128),
                "forceLogoutAtMs" to 0L,
                "permissions" to defaultPerms,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("admins").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, invitePath)
            Result.failure(e)
        }
    }

    // --- ADMIN ACTIONS ---

    suspend fun acceptOrder(orderId: String, adminNote: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            val adminUid = requireUserId()
            require(orderId.isNotBlank() && ID_REGEX.matches(orderId))
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to "accepted",
                    "adminNote" to adminNote.trim().take(200),
                    "settledBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun settleOrderWon(
        order: BetOrder,
        adminNote: String,
        bettingConfig: BettingConfig = BettingConfig(),
        customCommissionPercent: Double? = null,
        customGrossPayoutKs: Long? = null
    ): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val adminUid = requireUserId()
            require(order.id.isNotBlank())
            val orderRef = db.collection("orders").document(order.id)
            val userRef = db.collection("users").document(order.userId)
            val lbRef = db.collection("leaderboard").document(order.userId)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            val grossPayout = (customGrossPayoutKs ?: order.potentialPayoutKs).coerceIn(1L, 1_000_000_000L)
            val appliedPct = (customCommissionPercent ?: bettingConfig.winCommissionPercent).coerceIn(0.0, 50.0)
            val (commissionKs, payout) = bettingConfig.computeCommissionAndNet(order.stakeKs, grossPayout, appliedPct)
            val bonusPts = (order.stakeKs / 100L).coerceAtLeast(10L)
            val noteSuffix = if (commissionKs > 0L) " (အကောက် ${appliedPct}% -${commissionKs} Ks)" else ""
            val finalNote = (adminNote.trim().ifBlank { "အနိုင်ရ - ငွေရှင်းပြီး" } + noteSuffix).take(200)

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val lbSnap = tx.get(lbRef)
                val curBalance = userSnap.getLong("balanceKs") ?: 0L
                val curActive = userSnap.getLong("activeStakeKs") ?: 0L
                val curWon = userSnap.getLong("totalWonKs") ?: 0L
                val curPts = userSnap.getLong("rewardPoints") ?: 0L

                val newBalance = curBalance + payout
                val newActive = (curActive - order.stakeKs).coerceAtLeast(0L)
                val newWon = curWon + payout
                val newPts = curPts + bonusPts

                tx.update(
                    orderRef,
                    mapOf(
                        "status" to "won",
                        "actualPayoutKs" to payout,
                        "commissionPercent" to appliedPct,
                        "commissionKs" to commissionKs,
                        "adminNote" to finalNote,
                        "settledBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "balanceKs" to newBalance,
                        "activeStakeKs" to newActive,
                        "totalWonKs" to newWon,
                        "rewardPoints" to newPts,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "rewardPoints" to newPts,
                            "totalWonKs" to newWon,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        lbRef,
                        mapOf(
                            "userId" to order.userId,
                            "displayName" to order.customerName.ifBlank { "Member" }.take(60),
                            "rewardPoints" to newPts,
                            "totalWonKs" to newWon,
                            "visibility" to "public",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                val txTitle = if (commissionKs > 0L) {
                    "${orderTypeLabel(order.orderType)} အနိုင်ရရှိငွေ (အကောက် -${commissionKs} Ks)"
                } else {
                    "${orderTypeLabel(order.orderType)} အနိုင်ရရှိငွေ"
                }
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to order.userId,
                        "txType" to "order_win",
                        "direction" to "credit",
                        "amountKs" to payout,
                        "title" to txTitle.take(120),
                        "balanceAfterKs" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun settleOrderPartialReturn(order: BetOrder, returnKs: Long, adminNote: String): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val adminUid = requireUserId()
            require(order.id.isNotBlank())
            val safeReturn = returnKs.coerceIn(0L, order.stakeKs)
            val orderRef = db.collection("orders").document(order.id)
            val userRef = db.collection("users").document(order.userId)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val curBalance = userSnap.getLong("balanceKs") ?: 0L
                val curActive = userSnap.getLong("activeStakeKs") ?: 0L
                val newBalance = curBalance + safeReturn
                val newActive = (curActive - order.stakeKs).coerceAtLeast(0L)

                tx.update(
                    orderRef,
                    mapOf(
                        "status" to "lost",
                        "actualPayoutKs" to safeReturn,
                        "commissionPercent" to 0.0,
                        "commissionKs" to 0L,
                        "adminNote" to adminNote.trim().ifBlank { "တစ်ဝက်ရှုံး ပြန်အမ်းငွေ" }.take(200),
                        "settledBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "balanceKs" to newBalance,
                        "activeStakeKs" to newActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (safeReturn > 0L) {
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to order.userId,
                            "txType" to "order_refund",
                            "direction" to "credit",
                            "amountKs" to safeReturn,
                            "title" to "${orderTypeLabel(order.orderType)} ပြန်အမ်းငွေ",
                            "balanceAfterKs" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun settleOrderLost(order: BetOrder, adminNote: String): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val adminUid = requireUserId()
            require(order.id.isNotBlank())
            val orderRef = db.collection("orders").document(order.id)
            val userRef = db.collection("users").document(order.userId)

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val curActive = userSnap.getLong("activeStakeKs") ?: 0L
                val newActive = (curActive - order.stakeKs).coerceAtLeast(0L)

                tx.update(
                    orderRef,
                    mapOf(
                        "status" to "lost",
                        "actualPayoutKs" to 0L,
                        "adminNote" to adminNote.trim().ifBlank { "စာရင်းပိတ်ပြီး" }.take(200),
                        "settledBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "activeStakeKs" to newActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectOrder(order: BetOrder, adminNote: String): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val adminUid = requireUserId()
            require(order.id.isNotBlank())
            val orderRef = db.collection("orders").document(order.id)
            val userRef = db.collection("users").document(order.userId)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val curBalance = userSnap.getLong("balanceKs") ?: 0L
                val curActive = userSnap.getLong("activeStakeKs") ?: 0L
                val newBalance = curBalance + order.stakeKs
                val newActive = (curActive - order.stakeKs).coerceAtLeast(0L)

                tx.update(
                    orderRef,
                    mapOf(
                        "status" to "rejected",
                        "actualPayoutKs" to 0L,
                        "adminNote" to adminNote.trim().ifBlank { "ပယ်ဖျက်ပြီး - ငွေပြန်အမ်းပြီး" }.take(200),
                        "settledBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "balanceKs" to newBalance,
                        "activeStakeKs" to newActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to order.userId,
                        "txType" to "order_refund",
                        "direction" to "credit",
                        "amountKs" to order.stakeKs,
                        "title" to "အော်ဒါပယ်ဖျက် ငွေပြန်အမ်းခြင်း",
                        "balanceAfterKs" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun processWalletRequest(
        req: WalletRequest,
        approve: Boolean,
        adminNote: String,
        walletConfig: WalletConfig = WalletConfig(),
        isOwner: Boolean = true,
        dualApprovalThresholdKs: Long = 300_000L
    ): Result<Unit> {
        val path = "wallet_requests/${req.id}"
        return try {
            val adminUid = requireUserId()
            require(req.id.isNotBlank())
            val reqRef = db.collection("wallet_requests").document(req.id)
            val userRef = db.collection("users").document(req.userId)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            val needsDualApproval = approve &&
                !isOwner &&
                req.status == "pending" &&
                req.requestType == "withdraw" &&
                dualApprovalThresholdKs > 0L &&
                req.amountKs >= dualApprovalThresholdKs

            val nextStatus = when {
                needsDualApproval -> "awaiting_main_approval"
                approve -> "approved"
                else -> "rejected"
            }

            val authoritativeNetKs = if (req.requestType == "topup") {
                walletConfig.computeNetTopup(req.amountKs)
            } else {
                walletConfig.computeNetWithdraw(req.amountKs)
            }

            db.runTransaction { tx ->
                val reqSnap = tx.get(reqRef)
                val curReqStatus = reqSnap.getString("status") ?: req.status
                if (curReqStatus !in listOf("pending", "awaiting_main_approval")) {
                    throw IllegalStateException("ဤတောင်းဆိုမှုကို စာရင်းရှင်းပြီးသား ဖြစ်ပါသည်")
                }
                if (curReqStatus == "awaiting_main_approval" && !isOwner) {
                    throw IllegalStateException("Main Admin ၏ အတည်ပြုချက်သာ လိုအပ်ပါသည်")
                }

                val userSnap = tx.get(userRef)
                val curBalance = userSnap.getLong("balanceKs") ?: 0L

                val noteText = when {
                    needsDualApproval -> adminNote.trim().ifBlank { "ပမာဏကြီးသဖြင့် Main Admin အတည်ပြုချက် တင်ပြထားသည်" }
                    approve -> adminNote.trim().ifBlank { "အတည်ပြုပြီး" }
                    else -> adminNote.trim().ifBlank { "ပယ်ဖျက်ပြီး" }
                }.take(200)

                tx.update(
                    reqRef,
                    mapOf(
                        "status" to nextStatus,
                        "netAmountKs" to authoritativeNetKs.coerceIn(0L, 100_000_000L),
                        "adminNote" to noteText,
                        "processedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (nextStatus == "approved" && req.requestType == "topup") {
                    val creditKs = authoritativeNetKs
                    val newBalance = curBalance + creditKs
                    val rateText = if (walletConfig.depositPercent > 0.0 && walletConfig.depositMode != "none") {
                        val sign = if (walletConfig.depositMode == "bonus") "+" else "-"
                        " ($sign${walletConfig.depositPercent}%)"
                    } else ""
                    tx.update(
                        userRef,
                        mapOf(
                            "balanceKs" to newBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to req.userId,
                            "txType" to "topup",
                            "direction" to "credit",
                            "amountKs" to creditKs,
                            "title" to "ငွေသွင်းအတည်ပြုပြီး (${req.channel}$rateText)",
                            "balanceAfterKs" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (nextStatus == "rejected" && req.requestType == "withdraw") {
                    val newBalance = curBalance + req.amountKs
                    tx.update(
                        userRef,
                        mapOf(
                            "balanceKs" to newBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to req.userId,
                            "txType" to "order_refund",
                            "direction" to "credit",
                            "amountKs" to req.amountKs,
                            "title" to "ငွေထုတ်ပယ်ဖျက် - လက်ကျန်ငွေပြန်အမ်းပြီး",
                            "balanceAfterKs" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveNewMatch(
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double,
        goalOdds: Double
    ): Result<Unit> {
        val matchId = "m_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "matches/$matchId"
        return try {
            val adminUid = requireUserId()
            require(league.isNotBlank() && homeTeam.isNotBlank() && awayTeam.isNotBlank()) { "အသင်းနှင့် လိဂ်အမည် ဖြည့်စွက်ပါ" }
            db.collection("matches").document(matchId).set(
                mapOf(
                    "league" to league.trim().take(60),
                    "homeTeam" to homeTeam.trim().take(60),
                    "awayTeam" to awayTeam.trim().take(60),
                    "kickoffLabel" to kickoffLabel.trim().ifBlank { "Tonight 21:00" }.take(40),
                    "handicapLine" to handicapLine.trim().ifBlank { "Home -0.5" }.take(40),
                    "goalLine" to goalLine.trim().ifBlank { "2.5" }.take(20),
                    "bodyOdds" to bodyOdds.coerceIn(1.05, 50.0),
                    "goalOdds" to goalOdds.coerceIn(1.05, 50.0),
                    "status" to "open",
                    "homeScore" to 0L,
                    "awayScore" to 0L,
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMatchDetails(
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
    ): Result<Unit> {
        val path = "matches/$matchId"
        return try {
            requireUserId()
            require(matchId.isNotBlank() && ID_REGEX.matches(matchId)) { "Invalid match ID" }
            require(league.isNotBlank() && homeTeam.isNotBlank() && awayTeam.isNotBlank()) { "အသင်းနှင့် လိဂ်အမည် ဖြည့်စွက်ပါ" }
            require(status in listOf("open", "closed", "finished"))
            db.collection("matches").document(matchId).update(
                mapOf(
                    "league" to league.trim().take(60),
                    "homeTeam" to homeTeam.trim().take(60),
                    "awayTeam" to awayTeam.trim().take(60),
                    "kickoffLabel" to kickoffLabel.trim().ifBlank { "Tonight 21:00" }.take(40),
                    "handicapLine" to handicapLine.trim().ifBlank { "Home -0.5" }.take(40),
                    "goalLine" to goalLine.trim().ifBlank { "2.5" }.take(20),
                    "bodyOdds" to bodyOdds.coerceIn(1.0, 50.0),
                    "goalOdds" to goalOdds.coerceIn(1.0, 50.0),
                    "status" to status,
                    "homeScore" to homeScore.coerceIn(0L, 99L),
                    "awayScore" to awayScore.coerceIn(0L, 99L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMatch(matchId: String): Result<Unit> {
        val path = "matches/$matchId"
        return try {
            requireUserId()
            require(matchId.isNotBlank() && ID_REGEX.matches(matchId)) { "Invalid match ID" }
            db.collection("matches").document(matchId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveNewLotteryDraw(
        roundTitle: String,
        closeTimeLabel: String,
        payoutMultiplier: Double,
        drawType: String = "3d"
    ): Result<Unit> {
        val drawId = "d_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "lottery_draws/$drawId"
        return try {
            val adminUid = requireUserId()
            require(roundTitle.isNotBlank()) { "ထီအကြိမ်အမည် ဖြည့်စွက်ပါ" }
            require(drawType in listOf("2d", "3d"))
            db.collection("lottery_draws").document(drawId).set(
                mapOf(
                    "drawType" to drawType,
                    "roundTitle" to roundTitle.trim().take(80),
                    "closeTimeLabel" to closeTimeLabel.trim().ifBlank { "3:30 PM" }.take(40),
                    "winningNumber" to "",
                    "payoutMultiplier" to payoutMultiplier.coerceIn(1.0, 1000.0),
                    "status" to "open",
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateLotteryDraw(
        drawId: String,
        roundTitle: String,
        closeTimeLabel: String,
        payoutMultiplier: Double,
        winningNumber: String,
        status: String
    ): Result<Unit> {
        val path = "lottery_draws/$drawId"
        return try {
            requireUserId()
            require(drawId.isNotBlank() && ID_REGEX.matches(drawId)) { "Invalid draw ID" }
            require(roundTitle.isNotBlank()) { "ထီအကြိမ်အမည် ဖြည့်စွက်ပါ" }
            require(status in listOf("open", "closed", "finished"))
            val clean = winningNumber.trim().take(10)
            db.collection("lottery_draws").document(drawId).update(
                mapOf(
                    "roundTitle" to roundTitle.trim().take(80),
                    "closeTimeLabel" to closeTimeLabel.trim().ifBlank { "3:30 PM" }.take(40),
                    "payoutMultiplier" to payoutMultiplier.coerceIn(1.0, 1000.0),
                    "winningNumber" to clean,
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLotteryDraw(drawId: String): Result<Unit> {
        val path = "lottery_draws/$drawId"
        return try {
            requireUserId()
            require(drawId.isNotBlank() && ID_REGEX.matches(drawId)) { "Invalid draw ID" }
            db.collection("lottery_draws").document(drawId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteWalletRequest(requestId: String): Result<Unit> {
        val path = "wallet_requests/$requestId"
        return try {
            requireUserId()
            require(requestId.isNotBlank() && ID_REGEX.matches(requestId)) { "Invalid request ID" }
            db.collection("wallet_requests").document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearProcessedWalletRequests(requestIds: List<String>): Result<Int> {
        val path = "wallet_requests"
        return try {
            requireUserId()
            val validIds = requestIds.filter { it.isNotBlank() && ID_REGEX.matches(it) }
            if (validIds.isEmpty()) return Result.success(0)
            validIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(db.collection("wallet_requests").document(id))
                }
                batch.commit().await()
            }
            Result.success(validIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createAdminInvite(
        label: String,
        assignedEmail: String = "",
        initialPinHash: String = "",
        permissions: AdminPermissions = AdminPermissions.PRESET_WALLET_CASHIER
    ): Result<String> {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        val code = "LTR-$suffix"
        val path = "admin_invites/$code"
        return try {
            val adminUid = requireUserId()
            val safeLabel = label.trim().ifBlank { "Second Admin" }.take(60)
            val payload = mutableMapOf<String, Any>(
                "code" to code,
                "label" to safeLabel,
                "active" to true,
                "assignedEmail" to assignedEmail.trim().take(120),
                "initialPinHash" to initialPinHash.take(128),
                "defaultPermissions" to permissions.toMap(),
                "createdBy" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("admin_invites").document(code).set(payload).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleAdminInvite(code: String, active: Boolean): Result<Unit> {
        val path = "admin_invites/$code"
        return try {
            requireUserId()
            db.collection("admin_invites").document(code).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAdminMemberPermissions(adminUid: String, permissions: AdminPermissions): Result<Unit> {
        val path = "admins/$adminUid"
        return try {
            requireUserId()
            db.collection("admins").document(adminUid).update(
                mapOf(
                    "permissions" to permissions.toMap(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setAdminMemberStatus(adminUid: String, status: String): Result<Unit> {
        val path = "admins/$adminUid"
        return try {
            requireUserId()
            require(status in listOf("active", "suspended"))
            db.collection("admins").document(adminUid).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resetAdminMemberPin(adminUid: String, pinHash: String): Result<Unit> {
        val path = "admins/$adminUid"
        return try {
            requireUserId()
            db.collection("admins").document(adminUid).update(
                mapOf(
                    "pinHash" to pinHash.take(128),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun triggerForceLogoutAdmin(adminUid: String): Result<Unit> {
        val path = "admins/$adminUid"
        return try {
            requireUserId()
            db.collection("admins").document(adminUid).update(
                mapOf(
                    "forceLogoutAtMs" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun triggerForceLogoutUser(userUid: String): Result<Unit> {
        val path = "users/$userUid"
        return try {
            requireUserId()
            db.collection("users").document(userUid).update(
                mapOf(
                    "forceLogoutAtMs" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setCustomerLocked(userUid: String, locked: Boolean): Result<Unit> {
        val path = "users/$userUid"
        return try {
            requireUserId()
            db.collection("users").document(userUid).update(
                mapOf(
                    "isLocked" to locked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeAdminMember(adminUid: String): Result<Unit> {
        val path = "admins/$adminUid"
        return try {
            requireUserId()
            db.collection("admins").document(adminUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun savePaymentAccount(
        accountId: String?,
        providerName: String,
        accountName: String,
        accountNumber: String,
        note: String,
        active: Boolean,
        ownerAdminId: String = "",
        ownerAdminName: String = ""
    ): Result<Unit> {
        val id = accountId?.takeIf { it.isNotBlank() && ID_REGEX.matches(it) }
            ?: "pay_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "payment_accounts/$id"
        return try {
            val adminUid = requireUserId()
            require(providerName.isNotBlank() && accountName.isNotBlank() && accountNumber.isNotBlank()) {
                "ဘဏ်/Pay အမည်၊ အကောင့်အမည် နှင့် နံပါတ် ဖြည့်စွက်ပါ"
            }
            val ref = db.collection("payment_accounts").document(id)
            val snap = ref.get().await()
            val safeOwnerId = ownerAdminId.trim().take(128)
            val safeOwnerName = ownerAdminName.trim().take(60)
            if (snap.exists()) {
                val updateMap = mutableMapOf<String, Any>(
                    "providerName" to providerName.trim().take(40),
                    "accountName" to accountName.trim().take(80),
                    "accountNumber" to accountNumber.trim().take(60),
                    "note" to note.trim().take(120),
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeOwnerId.isNotBlank()) updateMap["ownerAdminId"] = safeOwnerId
                if (safeOwnerName.isNotBlank()) updateMap["ownerAdminName"] = safeOwnerName
                ref.update(updateMap).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "providerName" to providerName.trim().take(40),
                    "accountName" to accountName.trim().take(80),
                    "accountNumber" to accountNumber.trim().take(60),
                    "note" to note.trim().take(120),
                    "active" to active,
                    "createdBy" to adminUid,
                    "ownerAdminId" to safeOwnerId,
                    "ownerAdminName" to safeOwnerName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePaymentAccount(accountId: String): Result<Unit> {
        val path = "payment_accounts/$accountId"
        return try {
            requireUserId()
            require(accountId.isNotBlank() && ID_REGEX.matches(accountId))
            db.collection("payment_accounts").document(accountId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSystemGameConfig(config: SystemGameConfig): Result<Unit> {
        val path = "app_settings/system_config"
        return try {
            val adminUid = requireUserId()
            val minStake = config.minStakeKs.coerceIn(100L, 1_000_000L)
            val maxStake = config.maxStakeKs.coerceIn(minStake, 50_000_000L)
            db.collection("app_settings").document("system_config").set(
                mapOf(
                    "enabledGames" to config.enabledGames.take(12),
                    "dualApprovalThresholdKs" to config.dualApprovalThresholdKs.coerceIn(0L, 100_000_000L),
                    "minStakeKs" to minStake,
                    "maxStakeKs" to maxStake,
                    "twoDPayoutMultiplier" to config.twoDPayoutMultiplier.coerceIn(1.0, 200.0),
                    "threeDPayoutMultiplier" to config.threeDPayoutMultiplier.coerceIn(1.0, 1000.0),
                    "dicePayoutMultiplier" to config.dicePayoutMultiplier.coerceIn(1.0, 100.0),
                    "sixAnimalsPayoutMultiplier" to config.sixAnimalsPayoutMultiplier.coerceIn(1.0, 100.0),
                    "bonusPromoText" to config.bonusPromoText.trim().take(200),
                    "maintenanceMode" to config.maintenanceMode,
                    "maintenanceMessage" to config.maintenanceMessage.trim().take(300),
                    "hideTwoD" to config.hideTwoD,
                    "hideThreeD" to config.hideThreeD,
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSecurityConfig(config: SecurityConfig): Result<Unit> {
        val path = "app_settings/security_config"
        return try {
            val adminUid = requireUserId()
            db.collection("app_settings").document("security_config").set(
                mapOf(
                    "requireAdminPin" to config.requireAdminPin,
                    "ipWhitelistEnabled" to config.ipWhitelistEnabled,
                    "allowedIps" to config.allowedIps.map { it.trim() }.filter { it.isNotBlank() }.take(50),
                    "globalForceLogoutAtMs" to config.globalForceLogoutAtMs.coerceAtLeast(0L),
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordAuditLog(
        actorName: String,
        actorRole: String,
        action: String,
        category: String,
        targetId: String = "",
        targetName: String = "",
        details: String = ""
    ) {
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        runCatching {
            val uid = auth.currentUser?.uid ?: return
            db.collection("audit_logs").document(logId).set(
                mapOf(
                    "actorId" to uid,
                    "actorName" to actorName.trim().ifBlank { "Admin" }.take(60),
                    "actorRole" to if (actorRole == "owner") "owner" else "admin",
                    "action" to action.trim().ifBlank { "Action" }.take(80),
                    "category" to if (category in listOf("wallet", "order", "user", "admin", "board", "settings", "security")) category else "order",
                    "targetId" to targetId.trim().take(128),
                    "targetName" to targetName.trim().take(80),
                    "details" to details.trim().take(300),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun adjustUserBalance(targetUid: String, deltaKs: Long, note: String): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            val userRef = db.collection("users").document(targetUid)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val cur = snap.getLong("balanceKs") ?: 0L
                val next = (cur + deltaKs).coerceAtLeast(0L)
                tx.update(
                    userRef,
                    mapOf(
                        "balanceKs" to next,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to targetUid,
                        "txType" to "admin_adjust",
                        "direction" to if (deltaKs >= 0) "credit" else "debit",
                        "amountKs" to kotlin.math.abs(deltaKs),
                        "title" to note.trim().ifBlank { "Admin ငွေစာရင်းပြင်ဆင်မှု" }.take(120),
                        "balanceAfterKs" to next,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun giftRewardPoints(
        targetUid: String,
        displayName: String,
        deltaPoints: Long,
        note: String
    ): Result<Unit> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank() && ID_REGEX.matches(targetUid)) { "Invalid target user ID" }
            require(deltaPoints != 0L) { "ပွိုင့်ပမာဏ ထည့်သွင်းပါ" }
            val userRef = db.collection("users").document(targetUid)
            val lbRef = db.collection("leaderboard").document(targetUid)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val lbSnap = tx.get(lbRef)
                val curPts = snap.getLong("rewardPoints") ?: 0L
                val curBalance = snap.getLong("balanceKs") ?: 0L
                val curWon = snap.getLong("totalWonKs") ?: 0L
                if (deltaPoints < 0L && curPts <= 0L) {
                    throw IllegalStateException("Customer တွင် ပြန်နှုတ်စရာ ပွိုင့် 0 Pts သာ ရှိပါသည်")
                }
                val nextPts = (curPts + deltaPoints).coerceIn(0L, 100_000_000L)
                val actualDelta = nextPts - curPts
                tx.update(
                    userRef,
                    mapOf(
                        "rewardPoints" to nextPts,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "rewardPoints" to nextPts,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        lbRef,
                        mapOf(
                            "userId" to targetUid,
                            "displayName" to displayName.ifBlank { "Member" }.take(60),
                            "rewardPoints" to nextPts,
                            "totalWonKs" to curWon,
                            "visibility" to "public",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                val sign = if (actualDelta >= 0) "+" else ""
                val defaultNote = if (actualDelta >= 0) "Admin လက်ဆောင်ပွိုင့်" else "Admin မှ ပွိုင့်ပြန်နှုတ်ခြင်း"
                val cleanNote = note.trim().ifBlank { defaultNote }
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to targetUid,
                        "txType" to "point_gift",
                        "direction" to if (actualDelta >= 0) "credit" else "debit",
                        "amountKs" to 0L,
                        "title" to "$cleanNote ($sign$actualDelta Pts)".take(120),
                        "balanceAfterKs" to curBalance,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveWalletConfig(
        depositPercent: Double,
        depositMode: String,
        withdrawPercent: Double,
        withdrawMode: String
    ): Result<Unit> {
        val path = "app_settings/wallet_config"
        return try {
            val adminUid = requireUserId()
            require(depositMode in listOf("none", "bonus", "fee"))
            require(withdrawMode in listOf("none", "bonus", "fee"))
            db.collection("app_settings").document("wallet_config").set(
                mapOf(
                    "depositPercent" to depositPercent.coerceIn(0.0, 100.0),
                    "depositMode" to depositMode,
                    "withdrawPercent" to withdrawPercent.coerceIn(0.0, 100.0),
                    "withdrawMode" to withdrawMode,
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBoardConfig(
        autoSyncDailyMatches: Boolean,
        autoSyncLiveResults: Boolean,
        lastSyncedDate: String,
        allowedLeagues: List<String> = emptyList()
    ): Result<Unit> {
        val path = "app_settings/board_config"
        return try {
            val adminUid = requireUserId()
            db.collection("app_settings").document("board_config").set(
                mapOf(
                    "autoSyncDailyMatches" to autoSyncDailyMatches,
                    "autoSyncLiveResults" to autoSyncLiveResults,
                    "lastSyncedDate" to lastSyncedDate.take(20),
                    "allowedLeagues" to allowedLeagues.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(30),
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBettingConfig(
        winCommissionPercent: Double,
        winCommissionBasis: String,
        autoSettleOrders: Boolean,
        autoAcceptOrders: Boolean = false
    ): Result<Unit> {
        val path = "app_settings/betting_config"
        return try {
            val adminUid = requireUserId()
            require(winCommissionBasis in listOf("payout", "profit"))
            db.collection("app_settings").document("betting_config").set(
                mapOf(
                    "winCommissionPercent" to winCommissionPercent.coerceIn(0.0, 50.0),
                    "winCommissionBasis" to winCommissionBasis,
                    "autoSettleOrders" to autoSettleOrders,
                    "autoAcceptOrders" to autoAcceptOrders,
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBrandingConfig(config: BrandingConfig): Result<Unit> {
        val path = "app_settings/branding_config"
        return try {
            val adminUid = requireUserId()
            val safeName = config.appName.trim().ifBlank { "လိုတရ" }.take(40)
            val safeVariant = config.logoVariant.takeIf { it in listOf("crest", "gold_badge", "emerald_shield", "custom_url") } ?: "crest"
            val safeTheme = config.themePreset.takeIf { it in listOf("emerald_gold", "royal_blue", "crimson_gold", "obsidian_amber") } ?: "emerald_gold"
            db.collection("app_settings").document("branding_config").set(
                mapOf(
                    "appName" to safeName,
                    "logoVariant" to safeVariant,
                    "customLogoUrl" to config.customLogoUrl.trim().take(300),
                    "themePreset" to safeTheme,
                    "showRulesOnStartup" to config.showRulesOnStartup,
                    "rulesText" to config.rulesText.trim().take(3000),
                    "maxContactMainAdminKs" to config.maxContactMainAdminKs.coerceIn(0L, 100_000_000L),
                    "mainAdminContactNote" to config.mainAdminContactNote.trim().take(300),
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMiniGameControlConfig(config: MiniGameControlConfig): Result<Unit> {
        val path = "app_settings/mini_game_control"
        return try {
            val adminUid = requireUserId()
            val cleanAllowedTotals = config.diceAllowedTotals.filter { it in 3..18 }.distinct().take(20)
            val cleanTotalQuotas = config.diceTotalDailyQuotas.mapValues { (_, v) -> v.coerceIn(0L, 100_000L) }
            val cleanAnimalQuotas = config.animalDailyQuotas.mapValues { (_, v) -> v.coerceIn(0L, 100_000L) }
            val cleanGlobalDirective = config.globalDirective.takeIf { it in listOf("auto", "force_win", "force_loss") } ?: "auto"
            val cleanUserDirectives = config.userDirectives.mapValues { (_, v) ->
                v.takeIf { it in listOf("auto", "force_win", "force_loss") } ?: "auto"
            }.filterValues { it != "auto" }

            db.collection("app_settings").document("mini_game_control").set(
                mapOf(
                    "diceStandardMult" to config.diceStandardMult.coerceIn(1.0, 50.0),
                    "diceTripleMult" to config.diceTripleMult.coerceIn(1.0, 100.0),
                    "diceExactMult" to config.diceExactMult.coerceIn(1.0, 100.0),
                    "sixAnimalsMult" to config.sixAnimalsMult.coerceIn(1.0, 30.0),
                    "diceAllowedTotals" to cleanAllowedTotals,
                    "diceMaxBigPerDay" to config.diceMaxBigPerDay.coerceIn(0L, 100_000L),
                    "diceMaxSmallPerDay" to config.diceMaxSmallPerDay.coerceIn(0L, 100_000L),
                    "diceMaxOddPerDay" to config.diceMaxOddPerDay.coerceIn(0L, 100_000L),
                    "diceMaxEvenPerDay" to config.diceMaxEvenPerDay.coerceIn(0L, 100_000L),
                    "diceTotalDailyQuotas" to cleanTotalQuotas,
                    "animalDailyQuotas" to cleanAnimalQuotas,
                    "globalDirective" to cleanGlobalDirective,
                    "userDirectives" to cleanUserDirectives,
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSupportAgentProfile(profile: SupportAgentProfile): Result<Unit> {
        val targetUid = profile.adminUid.trim()
        val path = "support_agents/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank() && ID_REGEX.matches(targetUid)) { "Invalid admin UID" }
            val safeRole = if (profile.role == "owner") "owner" else "admin"
            val safePhone = profile.phone.trim().take(40)
            val safeMsg = profile.messagingHandle.trim().take(80)
            val safeNote = profile.note.trim().take(200)
            db.collection("support_agents").document(targetUid).set(
                mapOf(
                    "adminUid" to targetUid,
                    "displayName" to profile.displayName.trim().ifBlank { "Admin" }.take(60),
                    "role" to safeRole,
                    "phone" to safePhone,
                    "messagingHandle" to safeMsg,
                    "paymentSummary" to profile.paymentSummary.trim().take(160),
                    "note" to safeNote,
                    "active" to profile.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            runCatching {
                val adminDoc = db.collection("admins").document(targetUid).get().await()
                if (adminDoc.exists()) {
                    db.collection("admins").document(targetUid).update(
                        mapOf(
                            "contactPhone" to safePhone,
                            "contactMessaging" to safeMsg,
                            "contactNote" to safeNote.take(160),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAdminInviteEdit(
        oldCode: String,
        newCode: String,
        label: String,
        active: Boolean,
        assignedEmail: String,
        initialPinHash: String?,
        permissions: AdminPermissions
    ): Result<Unit> {
        val cleanOld = oldCode.trim().uppercase()
        val cleanNew = newCode.trim().uppercase()
        val path = "admin_invites/$cleanNew"
        return try {
            val adminUid = requireUserId()
            require(cleanOld.isNotBlank() && ID_REGEX.matches(cleanOld)) { "Invalid existing invite code" }
            require(cleanNew.length in 4..32 && ID_REGEX.matches(cleanNew)) { "Invite Code သည် အက္ခရာ/ဂဏန်း 4-32 လုံး ဖြစ်ရပါမည်" }
            val safeLabel = label.trim().ifBlank { "Second Admin" }.take(60)
            val safeEmail = assignedEmail.trim().take(120)

            val oldRef = db.collection("admin_invites").document(cleanOld)
            val oldSnap = oldRef.get().await()
            val preservedPinHash = initialPinHash?.take(128) ?: oldSnap.getString("initialPinHash").orEmpty()
            val preservedCreator = oldSnap.getString("createdBy").orEmpty().ifBlank { adminUid }

            if (cleanOld == cleanNew) {
                oldRef.update(
                    mapOf(
                        "label" to safeLabel,
                        "active" to active,
                        "assignedEmail" to safeEmail,
                        "initialPinHash" to preservedPinHash,
                        "defaultPermissions" to permissions.toMap(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val newRef = db.collection("admin_invites").document(cleanNew)
                val newSnap = newRef.get().await()
                require(!newSnap.exists()) { "Code '$cleanNew' ရှိနှင့်ပြီးသား ဖြစ်နေပါသည်" }
                val batch = db.batch()
                batch.set(
                    newRef,
                    mapOf(
                        "code" to cleanNew,
                        "label" to safeLabel,
                        "active" to active,
                        "assignedEmail" to safeEmail,
                        "initialPinHash" to preservedPinHash,
                        "defaultPermissions" to permissions.toMap(),
                        "createdBy" to preservedCreator,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.delete(oldRef)
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdminInvite(code: String): Result<Unit> {
        val clean = code.trim().uppercase()
        val path = "admin_invites/$clean"
        return try {
            requireUserId()
            require(clean.isNotBlank() && ID_REGEX.matches(clean))
            db.collection("admin_invites").document(clean).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setUserHidden(userUid: String, hidden: Boolean): Result<Unit> {
        val path = "users/$userUid"
        return try {
            requireUserId()
            require(userUid.isNotBlank() && ID_REGEX.matches(userUid))
            db.collection("users").document(userUid).update(
                mapOf(
                    "isHidden" to hidden,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setOrderHidden(orderId: String, hidden: Boolean): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank() && ID_REGEX.matches(orderId))
            db.collection("orders").document(orderId).update(
                mapOf(
                    "isHidden" to hidden,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun editOrderDetails(
        orderId: String,
        selections: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        actualPayoutKs: Long,
        status: String,
        adminNote: String
    ): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            val adminUid = requireUserId()
            require(orderId.isNotBlank() && ID_REGEX.matches(orderId))
            require(status in listOf("pending", "accepted", "won", "lost", "rejected"))
            val safeStake = stakeKs.coerceIn(100L, 10_000_000L)
            val safeMult = multiplier.coerceIn(1.0, 1000.0)
            val safePot = (safeStake * safeMult).roundToLong().coerceIn(safeStake, 1_000_000_000L)
            val safeActual = actualPayoutKs.coerceIn(0L, 1_000_000_000L)
            val safeSelections = selections.map { it.trim().take(120) }.filter { it.isNotBlank() }.take(30)
            require(safeSelections.isNotEmpty()) { "ရွေးချယ်ချက် အနည်းဆုံး ၁ ခု ရှိရပါမည်" }
            db.collection("orders").document(orderId).update(
                mapOf(
                    "selections" to safeSelections,
                    "drawRound" to drawRound.trim().take(60),
                    "stakeKs" to safeStake,
                    "multiplier" to safeMult,
                    "potentialPayoutKs" to safePot,
                    "actualPayoutKs" to safeActual,
                    "status" to status,
                    "adminNote" to adminNote.trim().take(200),
                    "settledBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun editWalletRequest(
        reqId: String,
        amountKs: Long,
        netAmountKs: Long,
        channel: String,
        referenceNote: String,
        status: String,
        adminNote: String,
        isHidden: Boolean
    ): Result<Unit> {
        val path = "wallet_requests/$reqId"
        return try {
            val adminUid = requireUserId()
            require(reqId.isNotBlank() && ID_REGEX.matches(reqId))
            require(status in listOf("pending", "awaiting_main_approval", "approved", "rejected"))
            val safeAmount = amountKs.coerceIn(500L, 10_000_000L)
            val safeNet = netAmountKs.coerceIn(0L, 100_000_000L)
            val safeChannel = channel.trim().ifBlank { "Wallet" }.take(40)
            val safeRef = referenceNote.trim().ifBlank { "-" }.take(120)
            db.collection("wallet_requests").document(reqId).update(
                mapOf(
                    "amountKs" to safeAmount,
                    "netAmountKs" to safeNet,
                    "channel" to safeChannel,
                    "referenceNote" to safeRef,
                    "status" to status,
                    "adminNote" to adminNote.trim().take(200),
                    "processedBy" to adminUid,
                    "isHidden" to isHidden,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun editTransaction(
        txId: String,
        title: String,
        amountKs: Long,
        direction: String,
        isHidden: Boolean
    ): Result<Unit> {
        val path = "transactions/$txId"
        return try {
            requireUserId()
            require(txId.isNotBlank() && ID_REGEX.matches(txId))
            require(direction in listOf("credit", "debit"))
            val safeTitle = title.trim().ifBlank { "Transaction" }.take(120)
            val safeAmount = amountKs.coerceIn(0L, 100_000_000L)
            db.collection("transactions").document(txId).update(
                mapOf(
                    "title" to safeTitle,
                    "amountKs" to safeAmount,
                    "direction" to direction,
                    "isHidden" to isHidden,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(txId: String): Result<Unit> {
        val path = "transactions/$txId"
        return try {
            requireUserId()
            require(txId.isNotBlank() && ID_REGEX.matches(txId))
            db.collection("transactions").document(txId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAuditLog(logId: String): Result<Unit> {
        val path = "audit_logs/$logId"
        return try {
            requireUserId()
            require(logId.isNotBlank() && ID_REGEX.matches(logId))
            db.collection("audit_logs").document(logId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAllAuditLogs(logIds: List<String>): Result<Int> {
        val path = "audit_logs"
        return try {
            requireUserId()
            val validIds = logIds.filter { it.isNotBlank() && ID_REGEX.matches(it) }
            if (validIds.isEmpty()) return Result.success(0)
            validIds.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(db.collection("audit_logs").document(id))
                }
                batch.commit().await()
            }
            Result.success(validIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun autoSettleFinishedOrders(
        bettingConfig: BettingConfig,
        targetUserId: String? = null
    ): Result<AutoSettleSummary> {
        val path = if (targetUserId != null) "orders/auto_settle_$targetUserId" else "orders/auto_settle"
        return try {
            requireUserId()
            val pendingOrders = if (targetUserId != null) {
                db.collection("orders")
                    .whereEqualTo("userId", targetUserId)
                    .get()
                    .await()
                    .toObjects(BetOrder::class.java)
                    .filter { it.status in listOf("pending", "accepted") }
            } else {
                db.collection("orders")
                    .whereIn("status", listOf("pending", "accepted"))
                    .get()
                    .await()
                    .toObjects(BetOrder::class.java)
            }
            if (pendingOrders.isEmpty()) {
                return Result.success(AutoSettleSummary())
            }

            val matches = db.collection("matches").get().await().toObjects(SportMatch::class.java)
            val matchesById = matches.associateBy { it.id }
            val draws = db.collection("lottery_draws").get().await().toObjects(LotteryDraw::class.java)

            var wonCount = 0
            var lostCount = 0
            var pushCount = 0

            for (order in pendingOrders) {
                when (val decision = AutoSettleEngine.evaluate(order, matchesById, draws, bettingConfig)) {
                    is AutoSettleDecision.Win -> {
                        val res = settleOrderWon(
                            order = order,
                            adminNote = decision.note,
                            bettingConfig = bettingConfig,
                            customGrossPayoutKs = decision.grossPayoutKs
                        )
                        if (res.isSuccess) wonCount++
                    }
                    is AutoSettleDecision.Loss -> {
                        val res = settleOrderLost(order, decision.note)
                        if (res.isSuccess) lostCount++
                    }
                    is AutoSettleDecision.PushRefund -> {
                        val res = rejectOrder(order, decision.note)
                        if (res.isSuccess) pushCount++
                    }
                    is AutoSettleDecision.PartialReturn -> {
                        val res = settleOrderPartialReturn(order, decision.returnKs, decision.note)
                        if (res.isSuccess) lostCount++
                    }
                    AutoSettleDecision.NotReady -> Unit
                }
            }

            Result.success(
                AutoSettleSummary(
                    settledCount = wonCount + lostCount + pushCount,
                    wonCount = wonCount,
                    lostCount = lostCount,
                    pushCount = pushCount
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun autoSettleSingleOrder(
        order: BetOrder,
        bettingConfig: BettingConfig
    ): Result<String> {
        val path = "orders/${order.id}"
        return try {
            requireUserId()
            if (order.status !in listOf("pending", "accepted")) {
                return Result.success("ဤအော်ဒါသည် စာရင်းရှင်းလင်းပြီးသား ဖြစ်ပါသည်")
            }
            val matches = db.collection("matches").get().await().toObjects(SportMatch::class.java)
            val matchesById = matches.associateBy { it.id }
            val draws = db.collection("lottery_draws").get().await().toObjects(LotteryDraw::class.java)

            when (val decision = AutoSettleEngine.evaluate(order, matchesById, draws, bettingConfig)) {
                is AutoSettleDecision.Win -> {
                    val res = settleOrderWon(
                        order = order,
                        adminNote = decision.note,
                        bettingConfig = bettingConfig,
                        customGrossPayoutKs = decision.grossPayoutKs
                    )
                    res.getOrElse { throw it }
                    Result.success("${order.customerName} (${orderTypeLabel(order.orderType)}) အလိုအလျောက် အနိုင်ရှင်းပြီးပါပြီ")
                }
                is AutoSettleDecision.Loss -> {
                    val res = settleOrderLost(order, decision.note)
                    res.getOrElse { throw it }
                    Result.success("${order.customerName} (${orderTypeLabel(order.orderType)}) အလိုအလျောက် အရှုံးစာရင်းပိတ်ပြီးပါပြီ")
                }
                is AutoSettleDecision.PushRefund -> {
                    val res = rejectOrder(order, decision.note)
                    res.getOrElse { throw it }
                    Result.success("${order.customerName} (${orderTypeLabel(order.orderType)}) သရေ (Push) ငွေပြန်အမ်းပြီးပါပြီ")
                }
                is AutoSettleDecision.PartialReturn -> {
                    val res = settleOrderPartialReturn(order, decision.returnKs, decision.note)
                    res.getOrElse { throw it }
                    Result.success("${order.customerName} (${orderTypeLabel(order.orderType)}) တစ်ဝက်ရှုံး ${decision.returnKs} Ks ပြန်အမ်းပြီးပါပြီ")
                }
                AutoSettleDecision.NotReady -> {
                    val reason = if (order.orderType == "three_d") {
                        "3D ထွက်ဂဏန်း (${order.drawRound.ifBlank { "လက်ရှိအကြိမ်" }}) ရလဒ် 'ပြီးဆုံး' မဖြစ်သေးပါ"
                    } else {
                        val pendingMatchNames = order.matchIds.mapNotNull { id ->
                            val m = matchesById[id]
                            when {
                                m == null -> null
                                m.status != "finished" -> "${m.homeTeam} vs ${m.awayTeam}"
                                else -> null
                            }
                        }.distinct()
                        if (pendingMatchNames.isNotEmpty()) {
                            "${pendingMatchNames.joinToString(", ")} ပွဲစဉ် ရလဒ် 'ပြီးဆုံး' မဖြစ်သေးပါ (ပွဲစဉ် Tab မှ ရလဒ်ထည့်ပါ)"
                        } else {
                            "ပွဲစဉ်ရလဒ် အချက်အလက် မပြည့်စုံသေးပါ — Live ရလဒ်ဆွဲယူပါ သို့မဟုတ် ကိုယ်တိုင် နိုင်/ရှုံး ရှင်းပေးပါ"
                        }
                    }
                    Result.success(reason)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncDailyMatchesFromFeed(
        feedService: FootballFeedService,
        dayOffsets: List<Int> = listOf(0, 1),
        currentConfig: BoardConfig? = null
    ): Result<Int> {
        val path = "matches/daily_sync"
        return try {
            val adminUid = requireUserId()
            val allowedLeagues = currentConfig?.allowedLeagues.orEmpty()
            val feedResult = feedService.fetchFixturesForDays(dayOffsets, allowedLeagues = allowedLeagues)
            val fixtures = feedResult.getOrElse { throw it }
            if (fixtures.isEmpty()) {
                return Result.success(0)
            }
            val existingSnap = db.collection("matches").get().await()
            val existingById = existingSnap.documents.associateBy { it.id }

            var syncedCount = 0
            fixtures.chunked(200).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { fix ->
                    val docId = "ext_${fix.externalId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(80)
                    val docRef = db.collection("matches").document(docId)
                    val prev = existingById[docId]
                    if (prev == null || !prev.exists()) {
                        batch.set(
                            docRef,
                            mapOf(
                                "league" to fix.league,
                                "homeTeam" to fix.homeTeam,
                                "awayTeam" to fix.awayTeam,
                                "homeLogoUrl" to fix.homeLogoUrl,
                                "awayLogoUrl" to fix.awayLogoUrl,
                                "kickoffLabel" to fix.kickoffLabel,
                                "handicapLine" to fix.handicapLine,
                                "goalLine" to fix.goalLine,
                                "bodyOdds" to fix.bodyOdds,
                                "goalOdds" to fix.goalOdds,
                                "status" to fix.status,
                                "homeScore" to fix.homeScore,
                                "awayScore" to fix.awayScore,
                                "visibility" to "public",
                                "externalId" to fix.externalId,
                                "matchDate" to fix.matchDate,
                                "liveClock" to fix.liveClock,
                                "createdBy" to adminUid,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        syncedCount++
                    } else {
                        batch.update(
                            docRef,
                            mapOf(
                                "homeLogoUrl" to fix.homeLogoUrl,
                                "awayLogoUrl" to fix.awayLogoUrl,
                                "kickoffLabel" to fix.kickoffLabel,
                                "status" to fix.status,
                                "homeScore" to fix.homeScore,
                                "awayScore" to fix.awayScore,
                                "liveClock" to fix.liveClock,
                                "matchDate" to fix.matchDate,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        syncedCount++
                    }
                }
                batch.commit().await()
            }

            if (currentConfig != null) {
                db.collection("app_settings").document("board_config").set(
                    mapOf(
                        "autoSyncDailyMatches" to currentConfig.autoSyncDailyMatches,
                        "autoSyncLiveResults" to currentConfig.autoSyncLiveResults,
                        "lastSyncedDate" to feedService.todayDateIso(),
                        "allowedLeagues" to currentConfig.allowedLeagues.take(30),
                        "updatedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(syncedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncLiveMatchResults(feedService: FootballFeedService): Result<Int> {
        val path = "matches/live_sync"
        return try {
            requireUserId()
            val fixtures = feedService.fetchFixturesForDays(listOf(-1, 0, 1)).getOrElse { throw it }
            if (fixtures.isEmpty()) return Result.success(0)

            val byExternalId = fixtures.associateBy { it.externalId }
            val byNormalizedTeams = fixtures.associateBy {
                "${FootballFeedService.normalizeTeam(it.homeTeam)}_${FootballFeedService.normalizeTeam(it.awayTeam)}"
            }

            val existingMatches = db.collection("matches").get().await()
                .toObjects(SportMatch::class.java)

            val updates = mutableListOf<Pair<String, LiveFootballFixture>>()
            existingMatches.forEach { m ->
                val matched = byExternalId[m.externalId]
                    ?: byNormalizedTeams["${FootballFeedService.normalizeTeam(m.homeTeam)}_${FootballFeedService.normalizeTeam(m.awayTeam)}"]
                if (matched != null) {
                    val changed = m.homeScore != matched.homeScore ||
                        m.awayScore != matched.awayScore ||
                        m.status != matched.status ||
                        m.liveClock != matched.liveClock ||
                        m.kickoffLabel != matched.kickoffLabel ||
                        (matched.homeLogoUrl.isNotBlank() && m.homeLogoUrl != matched.homeLogoUrl) ||
                        (matched.awayLogoUrl.isNotBlank() && m.awayLogoUrl != matched.awayLogoUrl)
                    if (changed) {
                        updates.add(m.id to matched)
                    }
                }
            }

            updates.chunked(200).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (docId, fix) ->
                    batch.update(
                        db.collection("matches").document(docId),
                        mapOf(
                            "homeLogoUrl" to fix.homeLogoUrl,
                            "awayLogoUrl" to fix.awayLogoUrl,
                            "status" to fix.status,
                            "homeScore" to fix.homeScore,
                            "awayScore" to fix.awayScore,
                            "liveClock" to fix.liveClock,
                            "kickoffLabel" to fix.kickoffLabel,
                            "externalId" to fix.externalId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(updates.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun seedSampleBoard(): Result<Unit> {
        return try {
            val adminUid = requireUserId()
            val batch = db.batch()
            val sampleMatches = listOf(
                mapOf(
                    "league" to "Premier League",
                    "homeTeam" to "Arsenal",
                    "awayTeam" to "Chelsea",
                    "kickoffLabel" to "Tonight 21:00",
                    "handicapLine" to "Home -0.5",
                    "goalLine" to "2.5/3",
                    "bodyOdds" to 1.92,
                    "goalOdds" to 1.90,
                    "status" to "open",
                    "homeScore" to 0L,
                    "awayScore" to 0L,
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "league" to "Premier League",
                    "homeTeam" to "Liverpool",
                    "awayTeam" to "Man City",
                    "kickoffLabel" to "Tonight 23:30",
                    "handicapLine" to "Home -0+50",
                    "goalLine" to "3.0",
                    "bodyOdds" to 1.95,
                    "goalOdds" to 1.88,
                    "status" to "open",
                    "homeScore" to 0L,
                    "awayScore" to 0L,
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "league" to "La Liga",
                    "homeTeam" to "Real Madrid",
                    "awayTeam" to "Barcelona",
                    "kickoffLabel" to "01:30 AM",
                    "handicapLine" to "Home -0.5",
                    "goalLine" to "3-30",
                    "bodyOdds" to 1.90,
                    "goalOdds" to 1.94,
                    "status" to "open",
                    "homeScore" to 0L,
                    "awayScore" to 0L,
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "league" to "Serie A",
                    "homeTeam" to "Inter Milan",
                    "awayTeam" to "AC Milan",
                    "kickoffLabel" to "Finished",
                    "handicapLine" to "Home -0.5",
                    "goalLine" to "2.5",
                    "bodyOdds" to 1.90,
                    "goalOdds" to 1.90,
                    "status" to "finished",
                    "homeScore" to 2L,
                    "awayScore" to 1L,
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            sampleMatches.forEachIndexed { idx, data ->
                batch.set(db.collection("matches").document("sample_match_${idx + 1}"), data)
            }
            batch.set(
                db.collection("lottery_draws").document("sample_draw_open"),
                mapOf(
                    "roundTitle" to "3D Round - Nov 16",
                    "closeTimeLabel" to "3:30 PM Close",
                    "winningNumber" to "",
                    "payoutMultiplier" to 550.0,
                    "status" to "open",
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("lottery_draws").document("sample_draw_prev"),
                mapOf(
                    "roundTitle" to "3D Round - Nov 01",
                    "closeTimeLabel" to "Closed",
                    "winningNumber" to "555",
                    "payoutMultiplier" to 550.0,
                    "status" to "finished",
                    "visibility" to "public",
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "matches/sample_board")
            Result.failure(e)
        }
    }

    suspend fun saveAppUpdateConfig(
        latestVersionCode: Int,
        latestVersionName: String,
        downloadUrl: String,
        releaseNotes: String,
        forceUpdate: Boolean
    ): Result<Unit> {
        val path = "app_settings/app_update"
        return try {
            val adminUid = requireUserId()
            val safeUrl = downloadUrl.trim().ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL }.take(300)
            require(safeUrl.startsWith("https://")) { "Download URL သည် https:// ဖြင့် စတင်ရပါမည်" }
            val safeName = latestVersionName.trim().ifBlank { "2.0" }.take(30)
            val safeCode = latestVersionCode.coerceIn(1, 1_000_000)
            db.collection("app_settings").document("app_update").set(
                mapOf(
                    "latestVersionCode" to safeCode,
                    "latestVersionName" to safeName,
                    "downloadUrl" to safeUrl,
                    "releaseNotes" to releaseNotes.trim().take(500),
                    "forceUpdate" to forceUpdate,
                    "updatedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        threadUserId: String,
        senderName: String,
        senderRole: String,
        text: String
    ): Result<Unit> {
        val msgId = "dm_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "direct_messages/$msgId"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "မက်ဆေ့ချ် စာသား ထည့်သွင်းပါ" }
            require(senderRole in listOf("customer", "admin"))
            require(threadUserId.isNotBlank() && ID_REGEX.matches(threadUserId))
            val safeName = senderName.trim().ifBlank { "Member" }.take(60)
            db.collection("direct_messages").document(msgId).set(
                mapOf(
                    "threadUserId" to threadUserId,
                    "senderId" to uid,
                    "senderName" to safeName,
                    "senderRole" to senderRole,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDirectMessage(msgId: String): Result<Unit> {
        val path = "direct_messages/$msgId"
        return try {
            requireUserId()
            require(msgId.isNotBlank() && ID_REGEX.matches(msgId))
            db.collection("direct_messages").document(msgId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun sendGroupMessage(
        senderName: String,
        senderRole: String,
        text: String
    ): Result<Unit> {
        val msgId = "gm_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "group_messages/$msgId"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "မက်ဆေ့ချ် စာသား ထည့်သွင်းပါ" }
            require(senderRole in listOf("customer", "admin"))
            val safeName = senderName.trim().ifBlank { "Member" }.take(60)
            db.collection("group_messages").document(msgId).set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to safeName,
                    "senderRole" to senderRole,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGroupMessage(msgId: String): Result<Unit> {
        val path = "group_messages/$msgId"
        return try {
            requireUserId()
            require(msgId.isNotBlank() && ID_REGEX.matches(msgId))
            db.collection("group_messages").document(msgId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun submitQualityReview(
        displayName: String,
        stars: Int,
        qualityTier: String,
        comment: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val path = "app_reviews/$uid"
        return try {
            val verifiedUid = requireUserId()
            val clampedStars = stars.coerceIn(1, 5)
            val safeTier = qualityTier.trim().ifBlank { qualityTierLabelForStars(clampedStars) }.take(40)
            val safeName = displayName.trim().ifBlank { "Member" }.take(60)
            val safeComment = comment.trim().take(300)
            db.collection("app_reviews").document(verifiedUid).set(
                mapOf(
                    "userId" to verifiedUid,
                    "displayName" to safeName,
                    "stars" to clampedStars,
                    "qualityTier" to safeTier,
                    "comment" to safeComment,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteQualityReview(targetUserId: String): Result<Unit> {
        val path = "app_reviews/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank() && ID_REGEX.matches(targetUserId))
            db.collection("app_reviews").document(targetUserId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun qualityTierLabelForStars(stars: Int): String = when (stars) {
        5 -> "အထူးကောင်းမွန် (5★)"
        4 -> "ကောင်းမွန် (4★)"
        3 -> "သင့်တင့် (3★)"
        2 -> "တိုးတက်ရန်လို (2★)"
        else -> "အားနည်း (1★)"
    }

    // --- VOICE & VIDEO CALL SIGNALING ---

    fun observeCallSessionsForUser(userId: String): Flow<List<CallSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "call_sessions"
        emitAll(
            db.collection("call_sessions")
                .whereEqualTo("threadUserId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    fun observeAllCallSessions(userId: String): Flow<List<CallSession>> = flow {
        val path = "call_sessions"
        emitAll(
            db.collection("call_sessions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreCatch(OperationType.LIST, path, userId)
        )
    }

    fun observeCallSession(callId: String): Flow<CallSession?> = flow {
        require(callId.isNotBlank() && ID_REGEX.matches(callId))
        val uid = requireUserId()
        val path = "call_sessions/$callId"
        emitAll(
            db.collection("call_sessions").document(callId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .wrapFirestoreCatch(OperationType.GET, path, uid)
        )
    }

    fun observeCallCandidates(callId: String): Flow<List<CallIceCandidate>> = flow {
        require(callId.isNotBlank() && ID_REGEX.matches(callId))
        val uid = requireUserId()
        val path = "call_sessions/$callId/candidates"
        emitAll(
            db.collection("call_sessions").document(callId)
                .collection("candidates")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallIceCandidate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreCatch(OperationType.LIST, path, uid)
        )
    }

    suspend fun createCallSession(
        callId: String,
        threadUserId: String,
        callerName: String,
        callerRole: String,
        calleeName: String,
        callType: String,
        offerSdp: String
    ): Result<Unit> {
        val path = "call_sessions/$callId"
        return try {
            val uid = requireUserId()
            require(callId.isNotBlank() && ID_REGEX.matches(callId))
            require(threadUserId.isNotBlank() && ID_REGEX.matches(threadUserId))
            require(callerRole in listOf("customer", "admin"))
            require(callType in listOf("voice", "video"))
            require(offerSdp.isNotBlank() && offerSdp.length <= 30_000) { "Call offer SDP invalid" }

            db.collection("call_sessions").document(callId).set(
                mapOf(
                    "threadUserId" to threadUserId,
                    "callerId" to uid,
                    "callerName" to callerName.trim().ifBlank { "Member" }.take(60),
                    "callerRole" to callerRole,
                    "calleeName" to calleeName.trim().take(60),
                    "callType" to callType,
                    "status" to "ringing",
                    "offerSdp" to offerSdp,
                    "answerSdp" to "",
                    "answeredBy" to "",
                    "answeredByName" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun answerCallSession(
        callId: String,
        answerSdp: String,
        answeredByName: String
    ): Result<Unit> {
        val path = "call_sessions/$callId"
        return try {
            val uid = requireUserId()
            require(callId.isNotBlank() && ID_REGEX.matches(callId))
            require(answerSdp.isNotBlank() && answerSdp.length <= 30_000) { "Call answer SDP invalid" }
            db.collection("call_sessions").document(callId).update(
                mapOf(
                    "status" to "accepted",
                    "answerSdp" to answerSdp,
                    "answeredBy" to uid,
                    "answeredByName" to answeredByName.trim().ifBlank { "Member" }.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCallStatus(callId: String, status: String): Result<Unit> {
        val path = "call_sessions/$callId"
        return try {
            requireUserId()
            require(callId.isNotBlank() && ID_REGEX.matches(callId))
            require(status in listOf("rejected", "ended"))
            db.collection("call_sessions").document(callId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addCallIceCandidate(
        callId: String,
        sdpMid: String,
        sdpMLineIndex: Int,
        candidate: String
    ): Result<Unit> {
        val candId = "cand_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "call_sessions/$callId/candidates/$candId"
        return try {
            val uid = requireUserId()
            require(callId.isNotBlank() && ID_REGEX.matches(callId))
            require(candidate.isNotBlank() && candidate.length <= 2048)
            db.collection("call_sessions").document(callId)
                .collection("candidates").document(candId)
                .set(
                    mapOf(
                        "senderId" to uid,
                        "sdpMid" to sdpMid.take(64),
                        "sdpMLineIndex" to sdpMLineIndex.coerceIn(0, 32),
                        "candidate" to candidate,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun orderTypeLabel(orderType: String): String = when (orderType) {
        "parlay" -> "မောင်း"
        "body_goal" -> "ဘော်ဒီ/ဂိုးပေါင်း"
        "two_d" -> "၂လုံးထီ"
        "three_d" -> "သုံးလုံးထီ"
        else -> orderType
    }
}
