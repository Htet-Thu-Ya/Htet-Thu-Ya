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
}
