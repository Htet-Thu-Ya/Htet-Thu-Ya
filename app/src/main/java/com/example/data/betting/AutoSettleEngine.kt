package com.example.data.betting

import com.example.data.model.BetOrder
import com.example.data.model.BettingConfig
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
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
        bettingConfig: BettingConfig = BettingConfig()
    ): AutoSettleDecision {
        if (order.status !in listOf("pending", "accepted")) return AutoSettleDecision.NotReady
        return when (order.orderType) {
            "three_d" -> evaluateThreeD(order, draws)
            "body_goal", "parlay" -> evaluateSports(order, matchesById)
            else -> AutoSettleDecision.NotReady
        }
    }

    private fun evaluateThreeD(order: BetOrder, draws: List<LotteryDraw>): AutoSettleDecision {
        val round = order.drawRound.trim()
        val draw = draws.firstOrNull {
            it.status == "finished" &&
                it.winningNumber.length == 3 &&
                (round.isBlank() || it.roundTitle.equals(round, ignoreCase = true))
        } ?: return AutoSettleDecision.NotReady

        val winNum = draw.winningNumber.trim()
        val regex = Regex("""(\d{3})""")
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
            if (pickedNum == winNum) {
                matchedCount++
                matchedGrossKs += pickStake * draw.payoutMultiplier
            }
        }

        return if (matchedCount > 0) {
            val gross = matchedGrossKs.roundToLong().coerceAtLeast(order.stakeKs)
            AutoSettleDecision.Win(
                grossPayoutKs = gross,
                note = "အလိုအလျောက် 3D ထွက်ဂဏန်း $winNum အနိုင်"
            )
        } else {
            AutoSettleDecision.Loss(
                note = "အလိုအလျောက် 3D ထွက်ဂဏန်း $winNum ရှုံး"
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
                "home", "away" -> evaluateHandicapLeg(match, pickSide)
                "over", "under" -> evaluateGoalLineLeg(match, pickSide)
                else -> null
            } ?: return AutoSettleDecision.NotReady

            val legOdds = when (pickSide) {
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
        if (!pickKey.isNullOrBlank() && pickKey in listOf("home", "away", "over", "under")) {
            return pickKey
        }
        val lower = selection.lowercase()
        return when {
            lower.contains("over (") || lower.contains("ဂိုးပေါင်း over") -> "over"
            lower.contains("under (") || lower.contains("ဂိုးပေါင်း under") -> "under"
            selection.startsWith("${match.homeTeam} (") -> "home"
            selection.startsWith("${match.awayTeam} (") -> "away"
            lower.contains(match.homeTeam.lowercase()) && !lower.contains("vs ${match.homeTeam.lowercase()}") -> "home"
            lower.contains(match.awayTeam.lowercase()) && !lower.contains("vs ${match.awayTeam.lowercase()}") -> "away"
            else -> null
        }
    }

    internal fun evaluateHandicapLeg(match: SportMatch, pickSide: String): Double? {
        val raw = match.handicapLine.trim()
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
