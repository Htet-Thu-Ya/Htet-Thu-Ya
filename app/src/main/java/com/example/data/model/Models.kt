package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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
    val drawRound: String = "",
    val stakeKs: Long = 0L,
    val multiplier: Double = 1.0,
    val potentialPayoutKs: Long = 0L,
    val actualPayoutKs: Long = 0L,
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
