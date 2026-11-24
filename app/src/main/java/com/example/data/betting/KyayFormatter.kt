package com.example.data.betting

import com.example.data.model.SportMatch
import java.util.Locale
import kotlin.math.abs

data class HandicapDisplayInfo(
    val normalizedLine: String,
    val favIsHome: Boolean,
    val favTeam: String,
    val dogTeam: String,
    val summaryText: String,
    val homeBadge: String,
    val awayBadge: String
)

object KyayFormatter {

    fun normalizeLegacyHandicap(match: SportMatch): String {
        val raw = match.handicapLine.trim()
        if (raw.isBlank()) return "Home -0.5"
        val lower = raw.lowercase(Locale.US)
        if (lower.startsWith("home") || lower.startsWith("away")) {
            return raw
        }

        // Check for American moneyline pattern like "ROMA -350", "LAZ -280", "PAR +150"
        val mlRegex = Regex("""^([A-Za-z0-9.\-&\s]{1,18}?)\s+([+-]\d{3,4})$""")
        val mlMatch = mlRegex.find(raw)
        if (mlMatch != null) {
            val token = mlMatch.groupValues[1].trim()
            val price = mlMatch.groupValues[2].toIntOrNull() ?: -150
            val favSide = detectSideFromToken(token, match.homeTeam, match.awayTeam)
            val spread = moneylineToHandicap(abs(price))
            return if (price < 0) {
                "$favSide -$spread"
            } else {
                val otherSide = if (favSide == "Home") "Away" else "Home"
                "$otherSide -$spread"
            }
        }
        return raw
    }

    fun convertEspnOddsToHandicap(
        details: String,
        spreadVal: Double,
        homeTeam: String,
        awayTeam: String,
        homeAbbr: String,
        awayAbbr: String
    ): String {
        if (!spreadVal.isNaN() && abs(spreadVal) in 0.25..4.5) {
            val clean = formatCleanDecimal(abs(spreadVal))
            return if (spreadVal < 0) "Home -$clean" else "Away -$clean"
        }
        val trimmed = details.trim()
        val mlRegex = Regex("""^([A-Za-z0-9.\-&\s]{1,18}?)\s+([+-]\d{2,4})$""")
        val match = mlRegex.find(trimmed)
        if (match != null) {
            val token = match.groupValues[1].trim()
            val num = match.groupValues[2].toIntOrNull()
            if (num != null && abs(num) >= 100) {
                val side = detectSideFromToken(token, homeTeam, awayTeam, homeAbbr, awayAbbr)
                val spread = moneylineToHandicap(abs(num))
                return if (num < 0) {
                    "$side -$spread"
                } else {
                    val other = if (side == "Home") "Away" else "Home"
                    "$other -$spread"
                }
            } else if (num != null) {
                // small integer spread
                val side = detectSideFromToken(token, homeTeam, awayTeam, homeAbbr, awayAbbr)
                return "$side -${abs(num)}"
            }
        }
        val decSpreadRegex = Regex("""^([A-Za-z0-9.\-&\s]{1,18}?)\s+([+-]\d+(?:\.\d+)?)$""")
        val decMatch = decSpreadRegex.find(trimmed)
        if (decMatch != null) {
            val token = decMatch.groupValues[1].trim()
            val num = decMatch.groupValues[2].toDoubleOrNull()
            if (num != null && abs(num) <= 6.0) {
                val side = detectSideFromToken(token, homeTeam, awayTeam, homeAbbr, awayAbbr)
                val clean = formatCleanDecimal(abs(num))
                return if (num <= 0) "$side -$clean" else {
                    val other = if (side == "Home") "Away" else "Home"
                    "$other -$clean"
                }
            }
        }
        return "Home -0.5"
    }

    fun formatHandicap(match: SportMatch): HandicapDisplayInfo {
        val norm = normalizeLegacyHandicap(match)
        val lower = norm.lowercase(Locale.US)
        val hasLeadingPlus = Regex("""(?:^|\s)\+\s*\d""").containsMatchIn(lower)
        val favIsHome = when {
            lower.startsWith("away") -> hasLeadingPlus
            lower.startsWith("home") -> !hasLeadingPlus
            else -> !norm.startsWith("+")
        }

        val cleanSpec = norm
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
            .ifBlank { "0.5" }

        val favTeam = if (favIsHome) match.homeTeam else match.awayTeam
        val dogTeam = if (favIsHome) match.awayTeam else match.homeTeam
        val explanation = describeHandicapSpec(cleanSpec)

        val summary = if (cleanSpec == "0" || cleanSpec == "0.0") {
            "သရေ (0) • အနိုင်ရသည့်အသင်း အပြည့်နိုင်"
        } else {
            "$favTeam က ပေး ($cleanSpec) • $explanation"
        }

        val favBadge = if (cleanSpec == "0" || cleanSpec == "0.0") "0 (သရေ)" else "ပေး (-$cleanSpec)"
        val dogBadge = if (cleanSpec == "0" || cleanSpec == "0.0") "0 (သရေ)" else "စား (+$cleanSpec)"

        return HandicapDisplayInfo(
            normalizedLine = norm,
            favIsHome = favIsHome,
            favTeam = favTeam,
            dogTeam = dogTeam,
            summaryText = summary,
            homeBadge = if (favIsHome) favBadge else dogBadge,
            awayBadge = if (favIsHome) dogBadge else favBadge
        )
    }

    fun formatGoalLine(goalLine: String): String {
        val clean = goalLine.trim().ifBlank { "2.5" }
        val kyayMatch = Regex("""^(\d+(?:\.\d+)?)\s*([+-])\s*(\d{1,2})$""").find(clean)
        if (kyayMatch != null) {
            val base = kyayMatch.groupValues[1]
            val sign = kyayMatch.groupValues[2]
            val pct = kyayMatch.groupValues[3]
            return if (sign == "+") {
                "ဂိုးပေါင်း ($clean) • $base ဂိုးထက်များမှ Over နိုင် ($base ဂိုးတိတိ Over ${pct}% နိုင်)"
            } else {
                "ဂိုးပေါင်း ($clean) • $base ဂိုးထက်များမှ Over အပြည့်နိုင် ($base ဂိုးတိတိ Over ${pct}% ရှုံး)"
            }
        }
        return when (clean) {
            "2", "2.0" -> "ဂိုးပေါင်း (2.0) • ၃ ဂိုး+ Over နိုင် / ၂ ဂိုးတိတိ ငွေပြန်"
            "2.5" -> "ဂိုးပေါင်း (2.5) • ၃ ဂိုးနှင့်အထက် Over နိုင် / ၂ ဂိုးအောက် Under နိုင်"
            "2.5/3" -> "ဂိုးပေါင်း (2.5/3) • ၃ ဂိုးဆို Over တစ်ဝက်နိုင် / ၄ ဂိုး+ အပြည့်နိုင်"
            "3", "3.0" -> "ဂိုးပေါင်း (3.0) • ၄ ဂိုး+ Over နိုင် / ၃ ဂိုးတိတိ ငွေပြန်"
            "3.5" -> "ဂိုးပေါင်း (3.5) • ၄ ဂိုးနှင့်အထက် Over နိုင် / ၃ ဂိုးအောက် Under နိုင်"
            else -> "ဂိုးပေါင်း ($clean) • $clean ဂိုးအထက် Over / အောက် Under"
        }
    }

    private fun describeHandicapSpec(spec: String): String {
        val kyayMatch = Regex("""^(\d+(?:\.\d+)?)\s*([+-])\s*(\d{1,2})$""").find(spec)
        if (kyayMatch != null) {
            val base = kyayMatch.groupValues[1]
            val sign = kyayMatch.groupValues[2]
            val pct = kyayMatch.groupValues[3]
            return if (base == "0") {
                if (sign == "+") "နိုင်ရင် အပြည့် / သရေကျရင် ပေးအသင်း ${pct}% နိုင်"
                else "နိုင်ရင် အပြည့် / သရေကျရင် ပေးအသင်း ${pct}% ရှုံး"
            } else {
                if (sign == "+") "$base ဂိုးအပြတ် နိုင်ရင် အပြည့် / $base ဂိုးတိတိ ပေးအသင်း ${pct}% နိုင်"
                else "$base ဂိုးထက်ပြတ်မှ အပြည့် / $base ဂိုးတိတိ ပေးအသင်း ${pct}% ရှုံး"
            }
        }
        return when (spec) {
            "0.25", "0/0.5" -> "နိုင်ရင် အပြည့် / သရေဆို စားအသင်း တစ်ဝက်နိုင်"
            "0.5" -> "၁ ဂိုးနှင့်အထက် နိုင်မှ အပြည့်နိုင် (သရေဆို စားနိုင်)"
            "0.75", "0.5/1" -> "၂ ဂိုး+ အပြည့်နိုင် / ၁ ဂိုးနိုင် တစ်ဝက်နိုင်"
            "1", "1.0" -> "၂ ဂိုး+ အပြည့်နိုင် / ၁ ဂိုးတိတိ ငွေပြန်"
            "1.5" -> "၂ ဂိုးနှင့်အထက် နိုင်မှ အပြည့်နိုင်"
            "2", "2.0" -> "၃ ဂိုး+ အပြည့်နိုင် / ၂ ဂိုးတိတိ ငွေပြန်"
            else -> "$spec ဂိုးကွာဟချက်ဖြင့် ဆုံးဖြတ်မည်"
        }
    }

    private fun moneylineToHandicap(absPrice: Int): String = when {
        absPrice >= 450 -> "1.5"
        absPrice >= 260 -> "1"
        absPrice >= 155 -> "0.5"
        absPrice >= 120 -> "0+50"
        else -> "0"
    }

    private fun formatCleanDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }

    private fun detectSideFromToken(
        token: String,
        homeTeam: String,
        awayTeam: String,
        homeAbbr: String = "",
        awayAbbr: String = ""
    ): String {
        val t = token.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        val h = homeTeam.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        val a = awayTeam.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        val ha = homeAbbr.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        val aa = awayAbbr.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

        return when {
            ha.isNotBlank() && t == ha -> "Home"
            aa.isNotBlank() && t == aa -> "Away"
            t.length >= 2 && a.contains(t) && !h.contains(t) -> "Away"
            t.length >= 2 && h.contains(t) && !a.contains(t) -> "Home"
            t.length >= 3 && a.contains(t.takeLast(3)) && !h.contains(t.takeLast(3)) -> "Away"
            t.length >= 3 && h.contains(t.takeLast(3)) && !a.contains(t.takeLast(3)) -> "Home"
            else -> "Home"
        }
    }

    fun americanOddsToDecimal(rawOdds: Double): Double? {
        if (rawOdds.isNaN() || rawOdds == 0.0) return null
        val dec = when {
            rawOdds >= 100.0 -> 1.0 + (rawOdds / 100.0)
            rawOdds <= -100.0 -> 1.0 + (100.0 / abs(rawOdds))
            rawOdds in 1.05..25.0 -> rawOdds
            else -> return null
        }
        return kotlin.math.round(dec.coerceIn(1.50, 4.20) * 100.0) / 100.0
    }

    fun deriveRealOddsFromEspn(
        homeMoneyLine: Double = Double.NaN,
        awayMoneyLine: Double = Double.NaN,
        homeSpreadOdds: Double = Double.NaN,
        awaySpreadOdds: Double = Double.NaN,
        overOdds: Double = Double.NaN,
        underOdds: Double = Double.NaN
    ): Pair<Double, Double> {
        val hSpread = americanOddsToDecimal(homeSpreadOdds)
        val aSpread = americanOddsToDecimal(awaySpreadOdds)
        val hMl = americanOddsToDecimal(homeMoneyLine)
        val aMl = americanOddsToDecimal(awayMoneyLine)
        val bodyOdds = when {
            hSpread != null && aSpread != null -> kotlin.math.round(((hSpread + aSpread) / 2.0) * 100.0) / 100.0
            hSpread != null -> hSpread
            aSpread != null -> aSpread
            hMl != null && aMl != null -> {
                val fav = minOf(hMl, aMl)
                // Map favorite moneyline into realistic Asian handicap payout around 1.82..2.05
                (kotlin.math.round((1.78 + (fav - 1.0).coerceIn(0.05, 1.5) * 0.16) * 100.0) / 100.0).coerceIn(1.80, 2.08)
            }
            else -> 1.93
        }

        val oDec = americanOddsToDecimal(overOdds)
        val uDec = americanOddsToDecimal(underOdds)
        val goalOdds = when {
            oDec != null && uDec != null -> kotlin.math.round(((oDec + uDec) / 2.0) * 100.0) / 100.0
            oDec != null -> oDec
            uDec != null -> uDec
            else -> 1.90
        }

        return bodyOdds.coerceIn(1.50, 3.80) to goalOdds.coerceIn(1.50, 3.80)
    }

    fun formatOneExpressKyay(
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double = 1.93,
        goalOdds: Double = 1.90
    ): String {
        val cleanH = toMyanmarKyaySpec(handicapLine)
        val cleanG = goalLine.trim().ifBlank { "2.5" }
        val oddsLbl = String.format(Locale.US, "%.2f", bodyOdds)
        val gOddsLbl = String.format(Locale.US, "%.2f", goalOdds)
        return "1xBET: $cleanH ($oddsLbl) | G $cleanG ($gOddsLbl)".take(60)
    }

    fun formatFiveFiveFiveKyay(
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double = 1.93,
        goalOdds: Double = 1.90
    ): String {
        val cleanH = toMyanmarKyaySpec(handicapLine)
        val cleanG = goalLine.trim().ifBlank { "2.5" }
        val oddsLbl = String.format(Locale.US, "%.2f", bodyOdds)
        val gOddsLbl = String.format(Locale.US, "%.2f", goalOdds)
        return "555 Mix: $cleanH ($oddsLbl) | ဂိုး $cleanG ($gOddsLbl)".take(60)
    }

    private fun toMyanmarKyaySpec(handicapLine: String): String {
        val raw = handicapLine.trim().ifBlank { "Home -0.5" }
        val isAway = raw.startsWith("Away", ignoreCase = true)
        val prefix = if (isAway) "A" else "H"
        val body = raw
            .replace("Home", "", ignoreCase = true)
            .replace("Away", "", ignoreCase = true)
            .trim()
            .removePrefix("-")
            .removePrefix("+")
            .trim()
            .ifBlank { "0.5" }
        return when (body) {
            "0", "0.0" -> "$prefix="
            "0.25", "0/0.5" -> "$prefix 0+50"
            "0.75", "0.5/1" -> "$prefix 1-50"
            else -> "$prefix-$body"
        }
    }

    data class OneXBetMarketOutcome(
        val type: Int,
        val param: Double?,
        val coef: Double
    )

    data class ParsedOneXBetLines(
        val handicapLine: String,
        val goalLine: String,
        val bodyOdds: Double,
        val goalOdds: Double
    )

    fun parseOneXBetOutcomesToLines(outcomes: List<OneXBetMarketOutcome>): ParsedOneXBetLines {
        fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

        val handicapCandidates = outcomes.filter {
            (it.type == 7 || it.type == 8) &&
                it.param != null &&
                !it.param.isNaN() &&
                abs(it.param) <= 4.5 &&
                it.coef in 1.25..4.20
        }

        val chosenHandicap = handicapCandidates.minByOrNull { abs(it.coef - 1.92) }
        val (handicapLine, bodyOdds) = if (chosenHandicap != null && chosenHandicap.param != null) {
            val snappedP = kotlin.math.round(chosenHandicap.param * 4.0) / 4.0
            val absStr = formatCleanDecimal(abs(snappedP))
            val hLine = when (chosenHandicap.type) {
                7 -> when {
                    abs(snappedP) < 0.01 -> "Home -0"
                    snappedP < 0.0 -> "Home -$absStr"
                    else -> "Away -$absStr"
                }
                else -> when {
                    abs(snappedP) < 0.01 -> "Away -0"
                    snappedP < 0.0 -> "Away -$absStr"
                    else -> "Home -$absStr"
                }
            }
            val oppositeType = if (chosenHandicap.type == 7) 8 else 7
            val opposite = handicapCandidates.firstOrNull {
                it.type == oppositeType && it.param != null && abs(it.param + snappedP) < 0.05
            }
            val avgBodyOdds = if (opposite != null) {
                (chosenHandicap.coef + opposite.coef) / 2.0
            } else {
                chosenHandicap.coef
            }
            hLine to round2(avgBodyOdds.coerceIn(1.50, 3.80))
        } else {
            val homeWin = outcomes.firstOrNull { it.type == 1 && it.coef > 1.01 }?.coef
            val awayWin = outcomes.firstOrNull { it.type == 3 && it.coef > 1.01 }?.coef
            val derivedLine = if (homeWin != null && awayWin != null) {
                when {
                    abs(homeWin - awayWin) <= 0.25 -> "Home -0"
                    homeWin < awayWin -> when {
                        homeWin <= 1.28 -> "Home -1.5"
                        homeWin <= 1.52 -> "Home -1"
                        homeWin <= 1.78 -> "Home -0.75"
                        homeWin <= 2.08 -> "Home -0.5"
                        else -> "Home -0.25"
                    }
                    else -> when {
                        awayWin <= 1.28 -> "Away -1.5"
                        awayWin <= 1.52 -> "Away -1"
                        awayWin <= 1.78 -> "Away -0.75"
                        awayWin <= 2.08 -> "Away -0.5"
                        else -> "Away -0.25"
                    }
                }
            } else {
                "Home -0.5"
            }
            derivedLine to 1.92
        }

        val totalCandidates = outcomes.filter {
            (it.type == 9 || it.type == 10) &&
                it.param != null &&
                !it.param.isNaN() &&
                it.param in 0.5..7.5 &&
                it.coef in 1.25..4.20
        }

        val chosenTotal = totalCandidates.minByOrNull { abs(it.coef - 1.90) }
        val (goalLine, goalOdds) = if (chosenTotal != null && chosenTotal.param != null) {
            val snappedG = kotlin.math.round(chosenTotal.param * 4.0) / 4.0
            val formattedG = formatGoalLineParam(snappedG)
            val oppositeType = if (chosenTotal.type == 9) 10 else 9
            val opposite = totalCandidates.firstOrNull {
                it.type == oppositeType && it.param != null && abs(it.param - snappedG) < 0.05
            }
            val avgGoalOdds = if (opposite != null) {
                (chosenTotal.coef + opposite.coef) / 2.0
            } else {
                chosenTotal.coef
            }
            formattedG to round2(avgGoalOdds.coerceIn(1.50, 3.80))
        } else {
            "2.5" to 1.90
        }

        return ParsedOneXBetLines(
            handicapLine = handicapLine,
            goalLine = goalLine,
            bodyOdds = bodyOdds,
            goalOdds = goalOdds
        )
    }

    private fun formatGoalLineParam(snappedValue: Double): String {
        val floorInt = kotlin.math.floor(snappedValue).toInt()
        val frac = snappedValue - floorInt
        return when {
            abs(frac - 0.25) < 0.02 -> "$floorInt/${floorInt}.5"
            abs(frac - 0.75) < 0.02 -> "${floorInt}.5/${floorInt + 1}"
            else -> formatCleanDecimal(snappedValue)
        }
    }
}
