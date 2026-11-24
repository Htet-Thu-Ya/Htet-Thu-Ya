package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToLong

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppLanguage(val code: String, val label: String) {
    MY("my", "မြန်မာ"),
    EN("en", "English")
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
    @get:PropertyName("isOnline") @PropertyName("isOnline")
    val isOnline: Boolean = false,
    val lastSeenAt: Timestamp? = null,
    @get:PropertyName("isLocked") @PropertyName("isLocked")
    val isLocked: Boolean = false,
    @get:PropertyName("isHidden") @PropertyName("isHidden")
    val isHidden: Boolean = false,
    val forceLogoutAtMs: Long = 0L,
    val securityPinHash: String = "",
    val lastRoundId: String = "",
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

data class AdminPermissions(
    val canApproveWallet: Boolean = false,
    val canManageOrders: Boolean = false,
    val canViewUsers: Boolean = true,
    val canLockUsers: Boolean = false,
    val canManageBoard: Boolean = false,
    val canChat: Boolean = true,
    val canEditWalletRates: Boolean = false,
    val canEditGameConfig: Boolean = false,
    val canViewAllLogs: Boolean = false,
    val canAdjustBalance: Boolean = false
) {
    fun toMap(): Map<String, Boolean> = mapOf(
        "canApproveWallet" to canApproveWallet,
        "canManageOrders" to canManageOrders,
        "canViewUsers" to canViewUsers,
        "canLockUsers" to canLockUsers,
        "canManageBoard" to canManageBoard,
        "canChat" to canChat,
        "canEditWalletRates" to canEditWalletRates,
        "canEditGameConfig" to canEditGameConfig,
        "canViewAllLogs" to canViewAllLogs,
        "canAdjustBalance" to canAdjustBalance
    )

    companion object {
        val ALL_GRANTED = AdminPermissions(
            canApproveWallet = true,
            canManageOrders = true,
            canViewUsers = true,
            canLockUsers = true,
            canManageBoard = true,
            canChat = true,
            canEditWalletRates = true,
            canEditGameConfig = true,
            canViewAllLogs = true,
            canAdjustBalance = true
        )

        val PRESET_WALLET_CASHIER = AdminPermissions(
            canApproveWallet = true,
            canViewUsers = true,
            canChat = true
        )

        val PRESET_ORDER_OPERATOR = AdminPermissions(
            canManageOrders = true,
            canManageBoard = true,
            canViewUsers = true,
            canChat = true
        )

        val PRESET_SUPPORT_ONLY = AdminPermissions(
            canViewUsers = true,
            canLockUsers = true,
            canChat = true
        )

        fun fromMap(map: Map<String, Boolean>?): AdminPermissions {
            if (map == null) return AdminPermissions()
            return AdminPermissions(
                canApproveWallet = map["canApproveWallet"] == true,
                canManageOrders = map["canManageOrders"] == true,
                canViewUsers = map["canViewUsers"] == true,
                canLockUsers = map["canLockUsers"] == true,
                canManageBoard = map["canManageBoard"] == true,
                canChat = map["canChat"] == true,
                canEditWalletRates = map["canEditWalletRates"] == true,
                canEditGameConfig = map["canEditGameConfig"] == true,
                canViewAllLogs = map["canViewAllLogs"] == true,
                canAdjustBalance = map["canAdjustBalance"] == true
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AdminMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "admin", // "owner" or "admin"
    val status: String = "active", // "active" or "suspended"
    val addedBy: String = "",
    val inviteCodeUsed: String = "",
    val pinHash: String = "",
    val forceLogoutAtMs: Long = 0L,
    val contactPhone: String = "",
    val contactMessaging: String = "",
    val contactNote: String = "",
    val permissions: Map<String, Boolean> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedPermissions(isOwner: Boolean): AdminPermissions {
        return if (isOwner || role == "owner") AdminPermissions.ALL_GRANTED
        else AdminPermissions.fromMap(permissions)
    }

    fun isActiveMember(): Boolean = status != "suspended"
}

@Keep
@IgnoreExtraProperties
data class AdminInvite(
    @DocumentId val id: String = "",
    val code: String = "",
    val label: String = "",
    val active: Boolean = true,
    val assignedEmail: String = "",
    val initialPinHash: String = "",
    val defaultPermissions: Map<String, Boolean> = emptyMap(),
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
    val homeLogoUrl: String = "",
    val awayLogoUrl: String = "",
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
    val boardSource: String = "standard", // "standard", "one_express", "five_five_five"
    val oneExpressKyay: String = "",
    val fiveFiveFiveKyay: String = "",
    val autoUpdatedAtMs: Long = 0L,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LotteryDraw(
    @DocumentId val id: String = "",
    val drawType: String = "3d", // "2d" or "3d"
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
    val orderType: String = "body_goal", // "parlay", "body_goal", "one_express", "five_five_five", "three_d", "two_d"
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
    @get:PropertyName("isHidden") @PropertyName("isHidden")
    val isHidden: Boolean = false,
    val matchKickoffMs: Long = 0L,
    val matchKickoffText: String = "",
    val scanAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MiniGameRound(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val gameType: String = "dice", // "dice" or "six_animals"
    val betLabel: String = "",
    val outcomeLabel: String = "",
    val stakeKs: Long = 0L,
    val payoutKs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
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
    val status: String = "pending", // "pending", "awaiting_main_approval", "approved", "rejected"
    val adminNote: String = "",
    val processedBy: String = "",
    val ratePercent: Double = 0.0,
    val rateMode: String = "none",
    val netAmountKs: Long = 0L,
    val receiptImageBase64: String = "",
    @get:PropertyName("isHidden") @PropertyName("isHidden")
    val isHidden: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentAccount(
    @DocumentId val id: String = "",
    val providerName: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val note: String = "",
    val active: Boolean = true,
    val createdBy: String = "",
    val ownerAdminId: String = "",
    val ownerAdminName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportAgentProfile(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val displayName: String = "",
    val role: String = "admin",
    val phone: String = "",
    val messagingHandle: String = "",
    val paymentSummary: String = "",
    val note: String = "",
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorRole: String = "admin",
    val action: String = "",
    val category: String = "order",
    val targetId: String = "",
    val targetName: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
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
    @get:PropertyName("isHidden") @PropertyName("isHidden")
    val isHidden: Boolean = false,
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
    val autoSyncOneExpress: Boolean = false,
    val autoSyncFiveFiveFive: Boolean = false,
    val lastSyncedDate: String = "",
    val allowedLeagues: List<String> = emptyList(),
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BettingConfig(
    val winCommissionPercent: Double = 0.0,
    val winCommissionBasis: String = "payout",
    val autoSettleOrders: Boolean = false,
    val autoAcceptOrders: Boolean = false,
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

@Keep
@IgnoreExtraProperties
data class SystemGameConfig(
    val enabledGames: List<String> = listOf("parlay", "body_goal", "two_d", "three_d", "dice", "six_animals"),
    val dualApprovalThresholdKs: Long = 300_000L,
    val minStakeKs: Long = 500L,
    val maxStakeKs: Long = 5_000_000L,
    val twoDPayoutMultiplier: Double = 80.0,
    val threeDPayoutMultiplier: Double = 550.0,
    val dicePayoutMultiplier: Double = 1.95,
    val sixAnimalsPayoutMultiplier: Double = 2.0,
    val bonusPromoText: String = "",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "စနစ်ပြုပြင်ထိန်းသိမ်းမှု ပြုလုပ်နေပါသည် — ခေတ္တစောင့်ဆိုင်းပေးပါ။",
    val hideTwoD: Boolean = false,
    val hideThreeD: Boolean = false,
    val footballPaused: Boolean = false,
    val footballReopenAtMs: Long = 0L,
    val gamesPaused: Boolean = false,
    val gamesReopenAtMs: Long = 0L,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isGameEnabled(gameKey: String): Boolean = enabledGames.isEmpty() || enabledGames.contains(gameKey)

    fun isFootballPausedNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        return footballPaused && (footballReopenAtMs <= 0L || nowMs < footballReopenAtMs)
    }

    fun isGamesPausedNow(nowMs: Long = System.currentTimeMillis()): Boolean {
        return gamesPaused && (gamesReopenAtMs <= 0L || nowMs < gamesReopenAtMs)
    }
}

@Keep
@IgnoreExtraProperties
data class SecurityConfig(
    val requireAdminPin: Boolean = false,
    val ipWhitelistEnabled: Boolean = false,
    val allowedIps: List<String> = emptyList(),
    val globalForceLogoutAtMs: Long = 0L,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BrandingConfig(
    val appName: String = "လိုတရ",
    val logoVariant: String = "crest", // "crest", "gold_badge", "emerald_shield", "custom_url"
    val customLogoUrl: String = "",
    val themePreset: String = "emerald_gold", // "emerald_gold", "royal_blue", "crimson_gold", "obsidian_amber"
    val showRulesOnStartup: Boolean = true,
    val rulesText: String = DEFAULT_RULES_TEXT,
    val maxContactMainAdminKs: Long = 500_000L,
    val mainAdminContactNote: String = "သတ်မှတ်ငွေပမာဏထက် ကျော်လွန်သော ငွေသွင်း/ငွေထုတ်/လောင်းကြေးများအတွက် Main Admin နှင့် တိုက်ရိုက်ဆက်သွယ်ပေးပါ။",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_RULES_TEXT =
            "၁။ လောင်းကြေးအော်ဒါများကို ပွဲမစမီ အတည်ပြုပေးမည်ဖြစ်ပြီး ပွဲစပြီးနောက် စနစ်မှ အလိုအလျောက် စကန်ဖတ် စစ်ဆေးပါမည်။\n" +
            "၂။ ငွေသွင်း/ငွေထုတ် ပြုလုပ်ရာတွင် မှန်ကန်သော Reference ID နှင့် ငွေပမာဏကိုသာ တင်သွင်းပါ။\n" +
            "၃။ သတ်မှတ်ထားသော အများဆုံးငွေပမာဏထက် ကျော်လွန်ပါက Main Admin ထံ တိုက်ရိုက်ဆက်သွယ်၍ အတည်ပြုချက် ရယူရပါမည်။"
    }
}

@Keep
@IgnoreExtraProperties
data class MiniGameControlConfig(
    val diceStandardMult: Double = 1.95,
    val diceTripleMult: Double = 24.0,
    val diceExactMult: Double = 6.0,
    val sixAnimalsMult: Double = 2.0,
    val diceAllowedTotals: List<Int> = emptyList(),
    val diceMaxBigPerDay: Long = 0L,
    val diceMaxSmallPerDay: Long = 0L,
    val diceMaxOddPerDay: Long = 0L,
    val diceMaxEvenPerDay: Long = 0L,
    val diceTotalDailyQuotas: Map<String, Long> = emptyMap(),
    val animalDailyQuotas: Map<String, Long> = emptyMap(),
    val globalDirective: String = "auto", // "auto", "stealth_house", "force_win", "force_loss"
    val stealthHouseWinPct: Int = 78,
    val userDirectives: Map<String, String> = emptyMap(), // userId -> "auto" | "stealth_house" | "force_win" | "force_loss"
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class LiveFootballFixture(
    val externalId: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeLogoUrl: String = "",
    val awayLogoUrl: String = "",
    val kickoffLabel: String,
    val matchDate: String,
    val handicapLine: String,
    val goalLine: String,
    val bodyOdds: Double,
    val goalOdds: Double,
    val status: String,
    val liveClock: String,
    val homeScore: Long,
    val awayScore: Long
)

data class Live2DStatus(
    val twod: String = "--",
    val setText: String = "--",
    val valueText: String = "--",
    val liveTime: String = "",
    val morning1201: String = "--",
    val evening430: String = "--"
)

@Keep
@IgnoreExtraProperties
data class AppUpdateConfig(
    val latestVersionCode: Int = 4,
    val latestVersionName: String = "4.0",
    val downloadUrl: String = DEFAULT_APK_DOWNLOAD_URL,
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_APK_DOWNLOAD_URL =
            "https://github.com/Htet-Thu-Ya/Htet-Thu-Ya/releases/download/latest/LoTaYa.apk"
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
    val status: String,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val connectedAtEpochMs: Long? = null,
    val hasRemoteVideo: Boolean = false
)
