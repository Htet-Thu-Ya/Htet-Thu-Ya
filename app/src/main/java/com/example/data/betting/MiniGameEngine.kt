package com.example.data.betting

import com.example.data.model.MiniGameRound
import kotlin.math.abs
import kotlin.random.Random

object MiniGameEngine {

    fun computeAdaptiveHouseWinProbability(
        baseWinPct: Int,
        stakeKs: Long,
        recentCustomerRounds: List<MiniGameRound>
    ): Double {
        var prob = baseWinPct.coerceIn(50, 95) / 100.0
        val recent = recentCustomerRounds.take(6)
        val prevWon = recent.firstOrNull()?.let { it.payoutKs > 0L } == true
        val recentWins = recent.take(4).count { it.payoutKs > 0L }
        val consecutiveLosses = recent.takeWhile { it.payoutKs == 0L }.size

        if (prevWon) {
            prob += 0.08
        } else if (recentWins >= 2) {
            prob += 0.05
        } else if (consecutiveLosses >= 4 && stakeKs < 20_000L && baseWinPct < 90) {
            // Give an occasional believable small breather win on normal stakes after 4+ losses
            prob -= 0.18
        }

        if (stakeKs >= 50_000L) {
            prob += 0.10
        } else if (stakeKs >= 10_000L) {
            prob += 0.05
        }

        return prob.coerceIn(0.50, 0.96)
    }

    fun selectDiceRoll(
        allCombos: List<Triple<Int, Int, Int>>,
        quotaCombos: List<Triple<Int, Int, Int>>,
        directive: String,
        stealthHouseWinPct: Int,
        betOption: String,
        exactNumber: Int,
        stakeKs: Long,
        recentCustomerRounds: List<MiniGameRound>,
        computePayout: (Int, Int, Int) -> Long,
        random: Random = Random.Default
    ): Triple<Int, Int, Int> {
        val basePool = quotaCombos.ifEmpty { allCombos }
        val normOpt = betOption.trim().lowercase()

        when (directive) {
            "force_win" -> {
                val pool = quotaCombos.filter { computePayout(it.first, it.second, it.third) > 0L }
                    .ifEmpty { allCombos.filter { computePayout(it.first, it.second, it.third) > 0L } }
                    .ifEmpty { basePool }
                return pool[random.nextInt(pool.size)]
            }
            "force_loss" -> {
                val pool = quotaCombos.filter { computePayout(it.first, it.second, it.third) == 0L }
                    .ifEmpty { allCombos.filter { computePayout(it.first, it.second, it.third) == 0L } }
                    .ifEmpty { basePool }
                return pool[random.nextInt(pool.size)]
            }
            "stealth_house" -> {
                val houseWinProb = computeAdaptiveHouseWinProbability(
                    baseWinPct = stealthHouseWinPct,
                    stakeKs = stakeKs,
                    recentCustomerRounds = recentCustomerRounds
                )
                val wantHouseWin = random.nextDouble() < houseWinProb
                val losing = basePool.filter { computePayout(it.first, it.second, it.third) == 0L }
                val winning = basePool.filter { computePayout(it.first, it.second, it.third) > 0L }

                if (wantHouseWin && losing.isNotEmpty()) {
                    val nonTripleLoss = losing.filterNot { it.first == it.second && it.second == it.third }
                        .ifEmpty { losing }
                    val nearMissPool = when (normOpt) {
                        "big" -> nonTripleLoss.filter { (it.first + it.second + it.third) in 9..10 }
                        "small" -> nonTripleLoss.filter { (it.first + it.second + it.third) in 11..12 }
                        "exact" -> nonTripleLoss.filter { abs((it.first + it.second + it.third) - exactNumber) in 1..2 }
                        else -> nonTripleLoss.filter { (it.first + it.second + it.third) in 8..13 }
                    }
                    val chosenPool = if (nearMissPool.isNotEmpty() && random.nextDouble() < 0.70) {
                        nearMissPool
                    } else {
                        nonTripleLoss
                    }
                    return chosenPool[random.nextInt(chosenPool.size)]
                }

                if (!wantHouseWin && winning.isNotEmpty()) {
                    val naturalWins = if (normOpt != "triple") {
                        winning.filterNot { it.first == it.second && it.second == it.third }
                            .ifEmpty { winning }
                    } else {
                        winning
                    }
                    return naturalWins[random.nextInt(naturalWins.size)]
                }
            }
        }
        return basePool[random.nextInt(basePool.size)]
    }

    fun selectSixAnimalsRoll(
        allCombos: List<Triple<Int, Int, Int>>,
        quotaCombos: List<Triple<Int, Int, Int>>,
        selectedAnimalIndex: Int,
        directive: String,
        stealthHouseWinPct: Int,
        stakeKs: Long,
        recentCustomerRounds: List<MiniGameRound>,
        random: Random = Random.Default
    ): Triple<Int, Int, Int> {
        val basePool = quotaCombos.ifEmpty { allCombos }
        fun matchCount(c: Triple<Int, Int, Int>): Int {
            var m = 0
            if (c.first == selectedAnimalIndex) m++
            if (c.second == selectedAnimalIndex) m++
            if (c.third == selectedAnimalIndex) m++
            return m
        }

        when (directive) {
            "force_win" -> {
                val pool = quotaCombos.filter { matchCount(it) > 0 }
                    .ifEmpty { allCombos.filter { matchCount(it) > 0 } }
                    .ifEmpty { basePool }
                return pool[random.nextInt(pool.size)]
            }
            "force_loss" -> {
                val pool = quotaCombos.filter { matchCount(it) == 0 }
                    .ifEmpty { allCombos.filter { matchCount(it) == 0 } }
                    .ifEmpty { basePool }
                return pool[random.nextInt(pool.size)]
            }
            "stealth_house" -> {
                val houseWinProb = computeAdaptiveHouseWinProbability(
                    baseWinPct = stealthHouseWinPct,
                    stakeKs = stakeKs,
                    recentCustomerRounds = recentCustomerRounds
                )
                val wantHouseWin = random.nextDouble() < houseWinProb
                val losing = basePool.filter { matchCount(it) == 0 }
                val winning = basePool.filter { matchCount(it) > 0 }

                if (wantHouseWin && losing.isNotEmpty()) {
                    val distinctLosses = losing.filter { it.first != it.second && it.second != it.third && it.first != it.third }
                    val chosenPool = if (distinctLosses.isNotEmpty() && random.nextDouble() < 0.80) {
                        distinctLosses
                    } else {
                        losing
                    }
                    return chosenPool[random.nextInt(chosenPool.size)]
                }

                if (!wantHouseWin && winning.isNotEmpty()) {
                    // Prefer natural 1-match wins to avoid unnecessary 2x/3x payouts while feeling organic
                    val singleMatchWins = winning.filter { matchCount(it) == 1 }
                    val chosenPool = if (singleMatchWins.isNotEmpty() && random.nextDouble() < 0.90) {
                        singleMatchWins
                    } else {
                        winning
                    }
                    return chosenPool[random.nextInt(chosenPool.size)]
                }
            }
        }
        return basePool[random.nextInt(basePool.size)]
    }

    fun computeSixAnimalsMultiPickPayout(
        roll: Triple<Int, Int, Int>,
        selectedAnimalIndices: Collection<Int>,
        stakePerAnimalKs: Long,
        baseBonus: Double
    ): Long {
        if (selectedAnimalIndices.isEmpty() || stakePerAnimalKs <= 0L) return 0L
        var totalPayout = 0L
        val safeBonus = baseBonus.coerceIn(0.2, 29.0)
        for (idx in selectedAnimalIndices.distinct()) {
            var matches = 0
            if (roll.first == idx) matches++
            if (roll.second == idx) matches++
            if (roll.third == idx) matches++
            if (matches > 0) {
                totalPayout += stakePerAnimalKs + Math.round(stakePerAnimalKs * safeBonus * matches)
            }
        }
        return totalPayout
    }

    fun selectSixAnimalsRollMultiPick(
        allCombos: List<Triple<Int, Int, Int>>,
        quotaCombos: List<Triple<Int, Int, Int>>,
        selectedAnimalIndices: Set<Int>,
        stakePerAnimalKs: Long,
        baseBonus: Double,
        directive: String,
        stealthHouseWinPct: Int,
        recentCustomerRounds: List<MiniGameRound>,
        random: Random = Random.Default
    ): Triple<Int, Int, Int> {
        val cleanSelection = selectedAnimalIndices.filter { it in 0..5 }.toSet().ifEmpty { setOf(0) }
        if (cleanSelection.size == 1) {
            return selectSixAnimalsRoll(
                allCombos = allCombos,
                quotaCombos = quotaCombos,
                selectedAnimalIndex = cleanSelection.first(),
                directive = directive,
                stealthHouseWinPct = stealthHouseWinPct,
                stakeKs = stakePerAnimalKs,
                recentCustomerRounds = recentCustomerRounds,
                random = random
            )
        }

        val totalStake = stakePerAnimalKs * cleanSelection.size
        val basePool = quotaCombos.ifEmpty { allCombos }
        fun payoutOf(c: Triple<Int, Int, Int>): Long = computeSixAnimalsMultiPickPayout(
            roll = c,
            selectedAnimalIndices = cleanSelection,
            stakePerAnimalKs = stakePerAnimalKs,
            baseBonus = baseBonus
        )

        when (directive) {
            "force_win" -> {
                val pool = quotaCombos.filter { payoutOf(it) > totalStake }
                    .ifEmpty { allCombos.filter { payoutOf(it) > 0L } }
                    .ifEmpty { basePool }
                return pool[random.nextInt(pool.size)]
            }
            "force_loss" -> {
                val zeroPool = quotaCombos.filter { payoutOf(it) == 0L }
                    .ifEmpty { allCombos.filter { payoutOf(it) == 0L } }
                val lossPool = zeroPool.ifEmpty {
                    quotaCombos.filter { payoutOf(it) < totalStake }
                        .ifEmpty { allCombos.filter { payoutOf(it) < totalStake } }
                }.ifEmpty { basePool }
                return lossPool[random.nextInt(lossPool.size)]
            }
            "stealth_house" -> {
                val houseWinProb = computeAdaptiveHouseWinProbability(
                    baseWinPct = stealthHouseWinPct,
                    stakeKs = totalStake,
                    recentCustomerRounds = recentCustomerRounds
                )
                val wantHouseWin = random.nextDouble() < houseWinProb
                val zeroPayouts = basePool.filter { payoutOf(it) == 0L }
                val netLosses = basePool.filter { payoutOf(it) < totalStake }
                val netWins = basePool.filter { payoutOf(it) > totalStake }

                if (wantHouseWin && (zeroPayouts.isNotEmpty() || netLosses.isNotEmpty())) {
                    val chosenPool = when {
                        zeroPayouts.isNotEmpty() && (netLosses.size == zeroPayouts.size || random.nextDouble() < 0.72) -> zeroPayouts
                        netLosses.isNotEmpty() -> netLosses
                        else -> zeroPayouts
                    }
                    return chosenPool[random.nextInt(chosenPool.size)]
                }
                if (!wantHouseWin && netWins.isNotEmpty()) {
                    return netWins[random.nextInt(netWins.size)]
                }
            }
        }
        return basePool[random.nextInt(basePool.size)]
    }

    data class DragonTigerRoll(
        val dragonRank: Int,
        val dragonSuit: String,
        val tigerRank: Int,
        val tigerSuit: String
    )

    private val CARD_SUITS = listOf("♠", "♥", "♦", "♣")

    fun selectDragonTigerOutcome(
        pick: String, // "DRAGON", "TIGER", "TIE"
        directive: String,
        stealthHouseWinPct: Int,
        stakeKs: Long,
        recentCustomerRounds: List<MiniGameRound>,
        random: Random = Random.Default
    ): DragonTigerRoll {
        val normPick = pick.trim().uppercase()
        val wantCustomerWin: Boolean? = when (directive) {
            "force_win" -> true
            "force_loss" -> false
            "stealth_house" -> {
                val houseWinProb = computeAdaptiveHouseWinProbability(
                    baseWinPct = stealthHouseWinPct,
                    stakeKs = stakeKs,
                    recentCustomerRounds = recentCustomerRounds
                )
                random.nextDouble() >= houseWinProb
            }
            else -> null
        }

        val targetResult = when (wantCustomerWin) {
            true -> normPick
            false -> when (normPick) {
                "DRAGON" -> if (random.nextDouble() < 0.14) "TIE" else "TIGER"
                "TIGER" -> if (random.nextDouble() < 0.14) "TIE" else "DRAGON"
                else -> if (random.nextBoolean()) "DRAGON" else "TIGER"
            }
            null -> {
                val r = random.nextDouble()
                when {
                    r < 0.08 -> "TIE"
                    r < 0.54 -> "DRAGON"
                    else -> "TIGER"
                }
            }
        }

        val dSuit = CARD_SUITS[random.nextInt(CARD_SUITS.size)]
        val tSuit = CARD_SUITS[random.nextInt(CARD_SUITS.size)]
        return when (targetResult) {
            "TIE" -> {
                val rank = random.nextInt(2, 14)
                DragonTigerRoll(rank, dSuit, rank, if (tSuit == dSuit) "♣" else tSuit)
            }
            "DRAGON" -> {
                val nearMiss = wantCustomerWin == false && random.nextDouble() < 0.65
                val dRank = if (nearMiss) random.nextInt(7, 14) else random.nextInt(2, 14)
                val tRank = if (nearMiss) (dRank - 1).coerceAtLeast(1) else random.nextInt(1, dRank)
                DragonTigerRoll(dRank, dSuit, tRank, tSuit)
            }
            else -> {
                val nearMiss = wantCustomerWin == false && random.nextDouble() < 0.65
                val tRank = if (nearMiss) random.nextInt(7, 14) else random.nextInt(2, 14)
                val dRank = if (nearMiss) (tRank - 1).coerceAtLeast(1) else random.nextInt(1, tRank)
                DragonTigerRoll(dRank, dSuit, tRank, tSuit)
            }
        }
    }

    data class LuckyWheelSegment(
        val index: Int,
        val label: String,
        val multiplier: Double
    )

    fun buildLuckyWheelSegments(luckyWheelMult: Double): List<LuckyWheelSegment> {
        val base = luckyWheelMult.coerceIn(1.2, 15.0)
        val midHigh = (base * 1.5).coerceIn(2.2, 20.0)
        val jackpot = (base * 2.5).coerceIn(3.5, 40.0)
        fun fmt(d: Double): String = if (d % 1.0 == 0.0) "${d.toInt()}x" else String.format(java.util.Locale.US, "%.1fx", d)
        return listOf(
            LuckyWheelSegment(0, "0x (ဗလာ)", 0.0),
            LuckyWheelSegment(1, "0.5x", 0.5),
            LuckyWheelSegment(2, "1.5x", 1.5),
            LuckyWheelSegment(3, fmt(base), base),
            LuckyWheelSegment(4, "0x (ဗလာ)", 0.0),
            LuckyWheelSegment(5, fmt(midHigh), midHigh),
            LuckyWheelSegment(6, "1.0x (အရင်း)", 1.0),
            LuckyWheelSegment(7, "${fmt(jackpot)} ဂျက်ပေါ့", jackpot)
        )
    }

    fun selectLuckyWheelSegmentIndex(
        segments: List<LuckyWheelSegment>,
        directive: String,
        stealthHouseWinPct: Int,
        stakeKs: Long,
        recentCustomerRounds: List<MiniGameRound>,
        random: Random = Random.Default
    ): Int {
        if (segments.isEmpty()) return 0
        val losing = segments.filter { it.multiplier < 1.0 }
        val winning = segments.filter { it.multiplier > 1.0 }
        val pushOrLowWin = segments.filter { it.multiplier in 1.0..2.5 }

        when (directive) {
            "force_win" -> {
                val pool = winning.ifEmpty { segments }
                return pool[random.nextInt(pool.size)].index
            }
            "force_loss" -> {
                val zeroOnly = segments.filter { it.multiplier == 0.0 }.ifEmpty { losing }.ifEmpty { segments }
                return zeroOnly[random.nextInt(zeroOnly.size)].index
            }
            "stealth_house" -> {
                val houseWinProb = computeAdaptiveHouseWinProbability(
                    baseWinPct = stealthHouseWinPct,
                    stakeKs = stakeKs,
                    recentCustomerRounds = recentCustomerRounds
                )
                val wantHouseWin = random.nextDouble() < houseWinProb
                if (wantHouseWin && losing.isNotEmpty()) {
                    // 0x or 0.5x near jackpot/high slice keeps excitement high while protecting house margin
                    val zeroPool = losing.filter { it.multiplier == 0.0 }
                    return if (zeroPool.isNotEmpty() && random.nextDouble() < 0.65) {
                        zeroPool[random.nextInt(zeroPool.size)].index
                    } else {
                        losing[random.nextInt(losing.size)].index
                    }
                }
                if (!wantHouseWin && winning.isNotEmpty()) {
                    val pool = if (pushOrLowWin.isNotEmpty() && random.nextDouble() < 0.82) pushOrLowWin else winning
                    return pool[random.nextInt(pool.size)].index
                }
            }
        }
        return segments[random.nextInt(segments.size)].index
    }
}
