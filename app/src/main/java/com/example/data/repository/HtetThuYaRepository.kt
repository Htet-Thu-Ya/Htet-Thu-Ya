package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.BetOrder
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LedgerTransaction
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UserAccount
import com.example.data.model.WalletRequest
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

    suspend fun ensureUserInitialized(userId: String, rawDisplayName: String): Result<Unit> {
        val userPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val safeName = rawDisplayName.trim().ifBlank { "Member" }.take(60)
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
                        "balanceKs" to 0L,
                        "rewardPoints" to 0L,
                        "activeStakeKs" to 0L,
                        "totalWonKs" to 0L,
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
                            "role" to "owner",
                            "addedBy" to uid,
                            "inviteCodeUsed" to "OWNER",
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
                        .sortedByDescending { it.balanceKs }
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

    // --- CUSTOMER MUTATIONS ---

    suspend fun placeOrder(
        customerName: String,
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double
    ): Result<String> {
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val orderPath = "orders/$orderId"
        return try {
            val uid = requireUserId()
            require(orderType in listOf("parlay", "body_goal", "three_d")) { "Invalid order type" }
            require(selections.isNotEmpty() && selections.size <= 15) { "Select 1 to 15 picks" }
            require(stakeKs in 100L..10_000_000L) { "Stake must be between 100 and 10,000,000 Ks" }
            require(multiplier in 1.0..1000.0) { "Invalid multiplier" }

            val safeName = customerName.trim().ifBlank { "Member" }.take(60)
            val safeSelections = selections.map { it.take(120) }
            val safeMatchIds = matchIds.take(15).map { it.take(128) }
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
                        "drawRound" to safeRound,
                        "stakeKs" to stakeKs,
                        "multiplier" to multiplier,
                        "potentialPayoutKs" to potentialPayout,
                        "actualPayoutKs" to 0L,
                        "status" to "pending",
                        "adminNote" to "",
                        "settledBy" to "",
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
        referenceNote: String
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
                            "title" to "ငွေထုတ်တောင်းဆိုခြင်း ($safeChannel)",
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
            val addedBy = inviteSnap.getString("createdBy") ?: uid
            val safeName = displayName.trim().ifBlank { "Co-Admin" }.take(60)
            db.collection("admins").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "role" to "admin",
                    "addedBy" to addedBy,
                    "inviteCodeUsed" to code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
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

    suspend fun settleOrderWon(order: BetOrder, adminNote: String): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            val adminUid = requireUserId()
            require(order.id.isNotBlank())
            val orderRef = db.collection("orders").document(order.id)
            val userRef = db.collection("users").document(order.userId)
            val lbRef = db.collection("leaderboard").document(order.userId)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")

            val payout = order.potentialPayoutKs
            val bonusPts = (order.stakeKs / 100L).coerceAtLeast(10L)

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
                        "adminNote" to adminNote.trim().ifBlank { "အနိုင်ရ - ငွေရှင်းပြီး" }.take(200),
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
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to order.userId,
                        "txType" to "order_win",
                        "direction" to "credit",
                        "amountKs" to payout,
                        "title" to "${orderTypeLabel(order.orderType)} အနိုင်ရရှိငွေ",
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

    suspend fun processWalletRequest(req: WalletRequest, approve: Boolean, adminNote: String): Result<Unit> {
        val path = "wallet_requests/${req.id}"
        return try {
            val adminUid = requireUserId()
            require(req.id.isNotBlank())
            val reqRef = db.collection("wallet_requests").document(req.id)
            val userRef = db.collection("users").document(req.userId)
            val txRef = db.collection("transactions").document("tx_${UUID.randomUUID().toString().replace("-", "").take(16)}")
            val nextStatus = if (approve) "approved" else "rejected"

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val curBalance = userSnap.getLong("balanceKs") ?: 0L

                tx.update(
                    reqRef,
                    mapOf(
                        "status" to nextStatus,
                        "adminNote" to adminNote.trim().ifBlank { if (approve) "အတည်ပြုပြီး" else "ပယ်ဖျက်ပြီး" }.take(200),
                        "processedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (req.requestType == "topup" && approve) {
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
                            "txType" to "topup",
                            "direction" to "credit",
                            "amountKs" to req.amountKs,
                            "title" to "ငွေသွင်းအတည်ပြုပြီး (${req.channel})",
                            "balanceAfterKs" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (req.requestType == "withdraw" && !approve) {
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
        payoutMultiplier: Double
    ): Result<Unit> {
        val drawId = "d_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "lottery_draws/$drawId"
        return try {
            val adminUid = requireUserId()
            require(roundTitle.isNotBlank()) { "ထီအကြိမ်အမည် ဖြည့်စွက်ပါ" }
            db.collection("lottery_draws").document(drawId).set(
                mapOf(
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

    suspend fun createAdminInvite(label: String): Result<String> {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        val code = "HTY-$suffix"
        val path = "admin_invites/$code"
        return try {
            val adminUid = requireUserId()
            val safeLabel = label.trim().ifBlank { "Co-Admin Friend" }.take(60)
            db.collection("admin_invites").document(code).set(
                mapOf(
                    "code" to code,
                    "label" to safeLabel,
                    "active" to true,
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
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

    private fun orderTypeLabel(orderType: String): String = when (orderType) {
        "parlay" -> "မောင်း"
        "body_goal" -> "ဘော်ဒီ/ဂိုးပေါင်း"
        "three_d" -> "သုံးလုံးထီ"
        else -> orderType
    }
}
