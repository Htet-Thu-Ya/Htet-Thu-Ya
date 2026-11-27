package com.example.data.betting

import com.example.data.model.BetOrder
import com.example.data.model.BettingConfig
import com.example.data.model.Live2DStatus
import com.example.data.model.Lottery2D3DControlConfig
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.util.MyanmarTimeUtil
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

sealed interface AutoSettleDecision {
    data class Win(val grossPayoutKs: Long, val note: String) : AutoSettleDecision
    data class PushRefund(val note: String) : AutoSettleDecision
    data class PartialReturn(val returnKs: Long, val note: String) : AutoSettleDecision
    data class Loss(val note: String) : AutoSettleDecision
    data object NotReady : AutoSettleDecision
}

data class AutoSettleSummary(
    val settledCount: Int = 0,
    val wonCount: Int = 0,
    val lostCount: Int = 0,
    val pushCount: Int = 0
)

object AutoSettleEngine {

    private data class LineSpec(
        val baseGoal: Double,
        val kyaySignedPct: Double? // e.g. +0.50 for "-0+50", -0.30 for "3-30"
    )

    fun evaluate(
        order: BetOrder,
        matchesById: Map<String, SportMatch>,
        draws: List<LotteryDraw>,
        bettingConfig: BettingConfig = BettingConfig(),
        live2DConfig: Lottery2D3DControlConfig? = null,
        live2DStatus: Live2DStatus? = null,
        nowMs: Long = MyanmarTimeUtil.nowMs()
    ): AutoSettleDecision {
        if (order.status !in listOf("pending", "accepted")) return AutoSettleDecision.NotReady
        return when (order.orderType) {
            "three_d" -> evaluateLottery(order, draws, digitCount = 3, label = "3D", live2DConfig = null, live2DStatus = null, nowMs = nowMs)
            "two_d" -> evaluateLottery(order, draws, digitCount = 2, label = "2D", live2DConfig = live2DConfig, live2DStatus = live2DStatus, nowMs = nowMs)
            "body_goal", "parlay", "one_express", "five_five_five" -> evaluateSports(order, matchesById)
            else -> AutoSettleDecision.NotReady
        }
    }

    private fun evaluateLottery(
        order: BetOrder,
        draws: List<LotteryDraw>,
        digitCount: Int,
        label: String,
        live2DConfig: Lottery2D3DControlConfig?,
        live2DStatus: Live2DStatus?,
        nowMs: Long
    ): AutoSettleDecision {
        val round = order.drawRound.trim()
        val isMorningRound = round.contains("12:01") || round.contains("နေ့လည်") || round.contains("မနက်")
        val isEveningRound = round.contains("4:30") || round.contains("16:30") || round.contains("ညနေ")

        val draw = draws.firstOrNull { d ->
            d.status == "finished" &&
                d.winningNumber.trim().length == digitCount &&
                (round.isBlank() ||
                    d.roundTitle.equals(round, ignoreCase = true) ||
                    (isMorningRound && d.roundTitle.contains("12:01")) ||
                    (isEveningRound && (d.roundTitle.contains("4:30") || d.roundTitle.contains("16:30"))))
        }

        val resolvedWinNum: String
        val resolvedMultiplier: Double
        val sessionTag: String
        if (draw != null) {
            resolvedWinNum = draw.winningNumber.trim()
            resolvedMultiplier = draw.payoutMultiplier
            sessionTag = draw.roundTitle.ifBlank { label }
        } else if (digitCount == 2) {
            val todayIso = MyanmarTimeUtil.todayIsoDate(nowMs)
            val embeddedDate = Regex("""\d{4}-\d{2}-\d{2}""").find(round)?.value
            val createdDate = order.createdAt?.toDate()?.time?.let { MyanmarTimeUtil.formatMatchDateIso(it) }
            val targetDate = embeddedDate ?: createdDate ?: todayIso
            // Only auto-settle from today's live feed if targetDate matches today
            if (targetDate > todayIso) return AutoSettleDecision.NotReady
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yangon")).apply { timeInMillis = nowMs }
            val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val twoDigitRegex = Regex("""^\d{2}$""")
            val isManualRoom = live2DConfig != null && !live2DConfig.autoMode

            val candidate = when {
                isMorningRound -> {
                    val mVal = if (isManualRoom) {
                        live2DConfig?.m1201Twod?.trim().orEmpty()
                    } else {
                        live2DStatus?.morning1201?.trim().orEmpty()
                            .ifBlank { live2DConfig?.m1201Twod?.trim().orEmpty() }
                    }
                    val timePassed = isManualRoom || targetDate < todayIso || minutesOfDay >= (12 * 60 + 1)
                    if (timePassed && targetDate == todayIso && twoDigitRegex.matches(mVal)) mVal else ""
                }
                isEveningRound -> {
                    val eVal = if (isManualRoom) {
                        live2DConfig?.e430Twod?.trim().orEmpty()
                    } else {
                        live2DStatus?.evening430?.trim().orEmpty()
                            .ifBlank { live2DConfig?.e430Twod?.trim().orEmpty() }
                    }
                    val timePassed = isManualRoom || targetDate < todayIso || minutesOfDay >= (16 * 60 + 30)
                    if (timePassed && targetDate == todayIso && twoDigitRegex.matches(eVal)) eVal else ""
                }
                else -> {
                    if (isManualRoom && targetDate == todayIso) {
                        val lVal = live2DConfig?.liveTwod?.trim().orEmpty()
                        if (twoDigitRegex.matches(lVal)) lVal else ""
                    } else ""
                }
            }
            if (!twoDigitRegex.matches(candidate)) return AutoSettleDecision.NotReady
            resolvedWinNum = candidate
            resolvedMultiplier = order.multiplier.takeIf { it > 1.0 } ?: 80.0
            sessionTag = if (isMorningRound) "2D 12:01 PM" else if (isEveningRound) "2D 4:30 PM" else label
        } else {
            return AutoSettleDecision.NotReady
        }

        val regex = Regex("""(\d{$digitCount})""")
        val stakeRegex = Regex("""\((\d[\d,]*)\s*Ks\)""")

        var matchedCount = 0
        var matchedGrossKs = 0.0
        val defaultStakePerPick = if (order.selections.isNotEmpty()) {
            order.stakeKs.toDouble() / order.selections.size.toDouble()
        } else {
            order.stakeKs.toDouble()
        }

        order.selections.forEach { sel ->
            val pickedNum = regex.find(sel)?.groupValues?.get(1) ?: return@forEach
            val pickStake = stakeRegex.find(sel)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull() ?: defaultStakePerPick
            if (pickedNum == resolvedWinNum) {
                matchedCount++
                matchedGrossKs += pickStake * resolvedMultiplier
            }
        }

        return if (matchedCount > 0) {
            val gross = matchedGrossKs.roundToLong().coerceAtLeast(order.stakeKs)
            AutoSettleDecision.Win(
                grossPayoutKs = gross,
                note = "အလိုအလျောက် $sessionTag ထွက်ဂဏန်း $resolvedWinNum အနိုင်"
            )
        } else {
            AutoSettleDecision.Loss(
                note = "အလိုအလျောက် $sessionTag ထွက်ဂဏန်း $resolvedWinNum ရှုံး"
            )
        }
    }

    private fun evaluateSports(
        order: BetOrder,
        matchesById: Map<String, SportMatch>
    ): AutoSettleDecision {
        if (order.matchIds.isEmpty() || order.selections.isEmpty()) return AutoSettleDecision.NotReady
        if (order.matchIds.size != order.selections.size) return AutoSettleDecision.NotReady

        var combinedReturnFactor = 1.0
        var hasLoss = false
        val legSummaries = mutableListOf<String>()

        for (i in order.selections.indices) {
            val matchId = order.matchIds[i]
            val match = matchesById[matchId] ?: return AutoSettleDecision.NotReady
            if (match.status != "finished") return AutoSettleDecision.NotReady

            val pickSide = resolvePickSide(order.pickKeys.getOrNull(i), order.selections[i], match)
                ?: return AutoSettleDecision.NotReady

            val legScoreRatio = when (pickSide) {
                "w1" -> if (match.homeScore > match.awayScore) 1.0 else -1.0
                "draw" -> if (match.homeScore == match.awayScore) 1.0 else -1.0
                "w2" -> if (match.awayScore > match.homeScore) 1.0 else -1.0
                "home", "away" -> evaluateHandicapLeg(match, pickSide)
                "over", "under" -> evaluateGoalLineLeg(match, pickSide)
                else -> null
            } ?: return AutoSettleDecision.NotReady

            val legOdds = when (pickSide) {
                "w1" -> match.win1Odds
                "draw" -> match.drawOdds
                "w2" -> match.win2Odds
                "over" -> if (match.boardSource == "one_express") match.overOdds else match.goalOdds
                "under" -> if (match.boardSource == "one_express") match.underOdds else match.goalOdds
                "home", "away" -> match.bodyOdds
                else -> match.goalOdds
            }.coerceAtLeast(1.0)

            legSummaries.add("${match.homeTeam} ${match.homeScore}-${match.awayScore} ${match.awayTeam}")

            if (legScoreRatio <= -0.999) {
                hasLoss = true
                combinedReturnFactor = 0.0
                break
            } else {
                // Convert leg score ratio (-1..+1) into cash return multiplier
                // +1.0 -> legOdds, +0.5 -> 1 + (legOdds - 1)*0.5, 0.0 -> 1.0, -0.5 -> 0.5
                val legReturnMult = if (legScoreRatio >= 0.0) {
                    1.0 + (legOdds - 1.0) * legScoreRatio
                } else {
                    (1.0 + legScoreRatio).coerceAtLeast(0.0)
                }
                combinedReturnFactor *= legReturnMult
            }
        }

        val summaryTag = legSummaries.joinToString(", ").take(110)

        if (hasLoss || combinedReturnFactor <= 0.001) {
            return AutoSettleDecision.Loss("အလိုအလျောက် စာရင်းပိတ် ($summaryTag)")
        }

        // Preserve exact potentialPayoutKs when all legs are full wins so manual & auto match 1:1
        val fullWinTolerance = abs(combinedReturnFactor - order.multiplier) <= 0.02
        val grossPayout = if (fullWinTolerance && combinedReturnFactor > 1.0) {
            order.potentialPayoutKs
        } else {
            (order.stakeKs * combinedReturnFactor).roundToLong()
        }

        return when {
            grossPayout > order.stakeKs -> AutoSettleDecision.Win(
                grossPayoutKs = grossPayout,
                note = "အလိုအလျောက် အနိုင် ($summaryTag)"
            )
            grossPayout == order.stakeKs -> AutoSettleDecision.PushRefund(
                note = "အလိုအလျောက် သရေ (Push) ငွေပြန်အမ်း ($summaryTag)"
            )
            grossPayout > 0L -> AutoSettleDecision.PartialReturn(
                returnKs = grossPayout,
                note = "အလိုအလျောက် တစ်ဝက်ရှုံး ပြန်အမ်းငွေ ($summaryTag)"
            )
            else -> AutoSettleDecision.Loss("အလိုအလျောက် စာရင်းပိတ် ($summaryTag)")
        }
    }

    internal fun resolvePickSide(pickKey: String?, selection: String, match: SportMatch): String? {
        if (!pickKey.isNullOrBlank() && pickKey in listOf("home", "away", "over", "under", "w1", "draw", "w2")) {
            return pickKey
        }
        val lower = selection.lowercase()
        return when {
            lower.contains("ဂိုးပေါ်") || lower.contains("over (") || lower.contains("ဂိုးပေါင်း over") -> "over"
            lower.contains("ဂိုးအောက်") || lower.contains("under (") || lower.contains("ဂိုးပေါင်း under") -> "under"
            lower.contains("(w1)") -> "w1"
            lower.contains("(သရေ)") || lower.contains("(draw)") -> "draw"
            lower.contains("(w2)") -> "w2"
            selection.startsWith("${match.homeTeam} (") -> "home"
            selection.startsWith("${match.awayTeam} (") -> "away"
            lower.contains(match.homeTeam.lowercase()) && !lower.contains("vs ${match.homeTeam.lowercase()}") -> "home"
            lower.contains(match.awayTeam.lowercase()) && !lower.contains("vs ${match.awayTeam.lowercase()}") -> "away"
            else -> null
        }
    }

    internal fun evaluateHandicapLeg(match: SportMatch, pickSide: String): Double? {
        val raw = KyayFormatter.normalizeLegacyHandicap(match)
        val lower = raw.lowercase()
        val hasLeadingPlus = Regex("""(?:^|\s)\+\s*\d""").containsMatchIn(lower)
        val favIsHome = when {
            lower.startsWith("away") -> hasLeadingPlus
            lower.startsWith("home") -> !hasLeadingPlus
            else -> !raw.startsWith("+")
        }

        val cleanedNumber = raw
            .replace("home", "", ignoreCase = true)
            .replace("away", "", ignoreCase = true)
            .replace("draw", "", ignoreCase = true)
            .replace("even", "0", ignoreCase = true)
            .replace("=", "")
            .replace("(", "")
            .replace(")", "")
            .trim()
            .removePrefix("-")
            .removePrefix("+")
            .trim()

        val spec = parseLineSpec(cleanedNumber) ?: return null
        val favDiff = if (favIsHome) {
            (match.homeScore - match.awayScore).toDouble()
        } else {
            (match.awayScore - match.homeScore).toDouble()
        }

        val favOutcome = evaluateGoalDifference(favDiff, spec)
        val pickedFav = (pickSide == "home" && favIsHome) || (pickSide == "away" && !favIsHome)
        return if (pickedFav) favOutcome else -favOutcome
    }

    internal fun evaluateGoalLineLeg(match: SportMatch, pickSide: String): Double? {
        val spec = parseLineSpec(match.goalLine.trim()) ?: return null
        val totalGoals = (match.homeScore + match.awayScore).toDouble()
        val overOutcome = evaluateGoalDifference(totalGoals, spec)
        return if (pickSide == "over") overOutcome else -overOutcome
    }

    private fun evaluateGoalDifference(actual: Double, spec: LineSpec): Double {
        if (spec.kyaySignedPct != null) {
            return when {
                actual > spec.baseGoal + 0.001 -> 1.0
                actual < spec.baseGoal - 0.001 -> -1.0
                else -> spec.kyaySignedPct
            }
        }
        val margin = actual - spec.baseGoal
        return when {
            margin >= 0.49 -> 1.0
            margin >= 0.20 -> 0.5
            margin > -0.20 -> 0.0
            margin > -0.49 -> -0.5
            else -> -1.0
        }
    }

    private fun parseLineSpec(raw: String): LineSpec? {
        val text = raw.trim()
        if (text.isBlank()) return null

        // Split ball format e.g. "2.5/3" or "0.5/1"
        if (text.contains("/")) {
            val parts = text.split("/").mapNotNull { it.trim().toDoubleOrNull() }
            if (parts.size == 2) {
                return LineSpec(baseGoal = (parts[0] + parts[1]) / 2.0, kyaySignedPct = null)
            }
        }

        // Myanmar Kyay format e.g. "0+50", "1-30", "3-30", "2+50"
        val kyayRegex = Regex("""^(\d+(?:\.\d+)?)\s*([+-])\s*(\d{1,2})$""")
        val kyayMatch = kyayRegex.find(text)
        if (kyayMatch != null) {
            val base = kyayMatch.groupValues[1].toDoubleOrNull() ?: return null
            val sign = if (kyayMatch.groupValues[2] == "-") -1.0 else 1.0
            val pct = (kyayMatch.groupValues[3].toDoubleOrNull() ?: return null) / 100.0
            return LineSpec(baseGoal = base, kyaySignedPct = (sign * pct).coerceIn(-0.95, 0.95))
        }

        val plain = text.toDoubleOrNull() ?: return null
        return LineSpec(baseGoal = plain, kyaySignedPct = null)
    }
}
