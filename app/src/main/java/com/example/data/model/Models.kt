package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToLong

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val balanceKs: Long = 0L,
    val rewardPoints: Long = 0L,
    val activeStakeKs: Long = 0L,
    val totalWonKs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val rewardPoints: Long = 0L,
    val totalWonKs: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = "admin",
    val addedBy: String = "",
    val inviteCodeUsed: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminInvite(
    @DocumentId val id: String = "",
    val code: String = "",
    val label: String = "",
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SportMatch(
    @DocumentId val id: String = "",
    val league: String = "",
    val homeTeam: String = "",
    val awayTeam: String = "",
    val kickoffLabel: String = "",
    val handicapLine: String = "",
    val goalLine: String = "",
    val bodyOdds: Double = 1.90,
    val goalOdds: Double = 1.90,
    val status: String = "open",
    val homeScore: Long = 0L,
    val awayScore: Long = 0L,
    val visibility: String = "public",
    val externalId: String = "",
    val matchDate: String = "",
    val liveClock: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LotteryDraw(
    @DocumentId val id: String = "",
    val roundTitle: String = "",
    val closeTimeLabel: String = "",
    val winningNumber: String = "",
    val payoutMultiplier: Double = 550.0,
    val status: String = "open",
    val visibility: String = "public",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BetOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val orderType: String = "body_goal",
    val selections: List<String> = emptyList(),
    val matchIds: List<String> = emptyList(),
    val pickKeys: List<String> = emptyList(),
    val drawRound: String = "",
    val stakeKs: Long = 0L,
    val multiplier: Double = 1.0,
    val potentialPayoutKs: Long = 0L,
    val actualPayoutKs: Long = 0L,
    val commissionPercent: Double = 0.0,
    val commissionKs: Long = 0L,
    val status: String = "pending",
    val adminNote: String = "",
    val settledBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val requestType: String = "topup",
    val amountKs: Long = 0L,
    val channel: String = "",
    val referenceNote: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    val processedBy: String = "",
    val ratePercent: Double = 0.0,
    val rateMode: String = "none",
    val netAmountKs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LedgerTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "order_stake",
    val direction: String = "debit",
    val amountKs: Long = 0L,
    val title: String = "",
    val balanceAfterKs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletConfig(
    val depositPercent: Double = 0.0,
    val depositMode: String = "bonus",
    val withdrawPercent: Double = 0.0,
    val withdrawMode: String = "fee",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun computeNetTopup(requestedKs: Long): Long {
        if (depositPercent <= 0.0 || depositMode == "none") return requestedKs
        val delta = ((requestedKs * depositPercent) / 100.0).toLong()
        return when (depositMode) {
            "bonus" -> (requestedKs + delta).coerceAtLeast(0L)
            "fee" -> (requestedKs - delta).coerceAtLeast(0L)
            else -> requestedKs
        }
    }

    fun computeNetWithdraw(requestedKs: Long): Long {
        if (withdrawPercent <= 0.0 || withdrawMode == "none") return requestedKs
        val delta = ((requestedKs * withdrawPercent) / 100.0).toLong()
        return when (withdrawMode) {
            "fee" -> (requestedKs - delta).coerceAtLeast(0L)
            "bonus" -> (requestedKs + delta).coerceAtLeast(0L)
            else -> requestedKs
        }
    }
}

@Keep
@IgnoreExtraProperties
data class BoardConfig(
    val autoSyncDailyMatches: Boolean = false,
    val autoSyncLiveResults: Boolean = false,
    val lastSyncedDate: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BettingConfig(
    val winCommissionPercent: Double = 0.0,
    val winCommissionBasis: String = "payout", // "payout" or "profit"
    val autoSettleOrders: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun computeCommissionAndNet(
        stakeKs: Long,
        grossPayoutKs: Long,
        overridePercent: Double? = null
    ): Pair<Long, Long> {
        val pct = (overridePercent ?: winCommissionPercent).coerceIn(0.0, 50.0)
        if (pct <= 0.0 || grossPayoutKs <= 0L) return 0L to grossPayoutKs
        val baseKs = if (winCommissionBasis == "profit") {
            (grossPayoutKs - stakeKs).coerceAtLeast(0L)
        } else {
            grossPayoutKs
        }
        val commissionKs = ((baseKs * pct) / 100.0).roundToLong().coerceIn(0L, grossPayoutKs)
        val netPayoutKs = (grossPayoutKs - commissionKs).coerceAtLeast(0L)
        return commissionKs to netPayoutKs
    }
}

data class LiveFootballFixture(
    val externalId: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffLabel: String,
    val matchDate: String,
    val handicapLine: String,
    val goalLine: String,
    val bodyOdds: Double,
    val goalOdds: Double,
    val status: String, // open, closed, finished
    val liveClock: String,
    val homeScore: Long,
    val awayScore: Long
)

@Keep
@IgnoreExtraProperties
data class AppUpdateConfig(
    val latestVersionCode: Int = 2,
    val latestVersionName: String = "2.0",
    val downloadUrl: String = DEFAULT_APK_DOWNLOAD_URL,
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_APK_DOWNLOAD_URL =
            "https://github.com/Htet-Thu-Ya/Htet-Thu-Ya/releases/download/latest/HtetThuYa.apk"
    }
}

@Keep
@IgnoreExtraProperties
data class DirectChatMessage(
    @DocumentId val id: String = "",
    val threadUserId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "customer",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "customer",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppQualityReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val stars: Int = 5,
    val qualityTier: String = "အထူးကောင်းမွန်",
    val comment: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GitHubReleaseInfo(
    val tagName: String,
    val updatedAtEpochMs: Long,
    val downloadUrl: String,
    val releaseNotes: String
)

data class AvailableApkUpdate(
    val versionLabel: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val forceUpdate: Boolean,
    val source: String
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val threadUserId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerRole: String = "customer",
    val calleeName: String = "",
    val callType: String = "voice",
    val status: String = "ringing",
    val offerSdp: String = "",
    val answerSdp: String = "",
    val answeredBy: String = "",
    val answeredByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallIceCandidate(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val candidate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ActiveCallUiState(
    val callId: String,
    val threadUserId: String,
    val callType: String,
    val peerName: String,
    val peerRole: String,
    val isOutgoing: Boolean,
    val status: String, // "calling", "connecting", "connected", "ended"
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val connectedAtEpochMs: Long? = null,
    val hasRemoteVideo: Boolean = false
)


