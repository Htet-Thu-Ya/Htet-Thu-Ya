package com.example

import com.example.data.betting.AutoSettleDecision
import com.example.data.betting.AutoSettleEngine
import com.example.data.betting.KyayFormatter
import com.example.data.betting.MiniGameEngine
import com.example.data.model.BetOrder
import com.example.data.model.MiniGameRound
import com.example.data.model.SportMatch
import com.example.data.model.SystemGameConfig
import com.example.data.model.WalletRequest
import com.example.ui.components.formatCountdownPill
import com.example.ui.components.formatRemainingReopenCountdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AutoSettleAndPauseTest {

    @Test
    fun formatOneExpressKyay_formatsMyanmarHandicapAndGoalLines() {
        val homeHalf = KyayFormatter.formatOneExpressKyay(
            handicapLine = "Home -0.5",
            goalLine = "2.5",
            bodyOdds = 1.92,
            goalOdds = 1.90
        )
        assertTrue(homeHalf.contains("1xBET: H-0.5 (1.92)"))
        assertTrue(homeHalf.contains("G 2.5 (1.90)"))

        val awayQuarter = KyayFormatter.formatOneExpressKyay(
            handicapLine = "Away -0.25",
            goalLine = "3",
            bodyOdds = 1.95,
            goalOdds = 1.88
        )
        assertTrue(awayQuarter.contains("1xBET: A 0+50 (1.95)"))
        assertTrue(awayQuarter.contains("G 3 (1.88)"))
    }

    @Test
    fun formatFiveFiveFiveKyay_formatsMyanmarKyayString() {
        val evenLine = KyayFormatter.formatFiveFiveFiveKyay(
            handicapLine = "Home -0",
            goalLine = "2.5/3",
            bodyOdds = 1.92,
            goalOdds = 1.90
        )
        assertTrue(evenLine.startsWith("555 Mix: H="))
        assertTrue(evenLine.contains("ဂိုး 2.5/3 (1.90)"))
    }

    @Test
    fun kyayFormatter_convertsAmericanAndEspnLiveOddsToDecimal() {
        assertEquals(1.91, KyayFormatter.americanOddsToDecimal(-110.0)!!, 0.01)
        assertEquals(2.50, KyayFormatter.americanOddsToDecimal(150.0)!!, 0.01)
        val (body, goal) = KyayFormatter.deriveRealOddsFromEspn(
            homeSpreadOdds = -115.0,
            awaySpreadOdds = -105.0,
            overOdds = -108.0,
            underOdds = -112.0
        )
        assertEquals(1.91, body, 0.03)
        assertEquals(1.91, goal, 0.03)
    }

    @Test
    fun autoSettleEngine_settlesOneExpressAndFiveFiveFiveOrders() {
        val match = SportMatch(
            id = "m1",
            homeTeam = "Arsenal",
            awayTeam = "Chelsea",
            handicapLine = "Home -0.5",
            goalLine = "2.5",
            bodyOdds = 1.92,
            goalOdds = 1.90,
            status = "finished",
            homeScore = 2L,
            awayScore = 0L,
            boardSource = "one_express"
        )
        val matchesById = mapOf("m1" to match)

        val oneExpressWinOrder = BetOrder(
            id = "o1",
            orderType = "one_express",
            selections = listOf("Arsenal (-0.5)"),
            matchIds = listOf("m1"),
            pickKeys = listOf("home"),
            stakeKs = 10_000L,
            multiplier = 1.92,
            potentialPayoutKs = 19_200L,
            status = "accepted"
        )
        val winDecision = AutoSettleEngine.evaluate(oneExpressWinOrder, matchesById, emptyList())
        assertTrue(winDecision is AutoSettleDecision.Win)
        assertEquals(19_200L, (winDecision as AutoSettleDecision.Win).grossPayoutKs)

        val fiveFiveFiveLossOrder = BetOrder(
            id = "o2",
            orderType = "five_five_five",
            selections = listOf("Over (2.5)"),
            matchIds = listOf("m1"),
            pickKeys = listOf("over"),
            stakeKs = 10_000L,
            multiplier = 1.90,
            potentialPayoutKs = 19_000L,
            status = "pending"
        )
        val lossDecision = AutoSettleEngine.evaluate(fiveFiveFiveLossOrder, matchesById, emptyList())
        assertTrue(lossDecision is AutoSettleDecision.Loss)

        val match2 = SportMatch(
            id = "m2",
            homeTeam = "Liverpool",
            awayTeam = "Everton",
            handicapLine = "Home -1",
            goalLine = "2.5",
            bodyOdds = 1.90,
            goalOdds = 1.90,
            status = "finished",
            homeScore = 3L,
            awayScore = 1L,
            boardSource = "five_five_five"
        )
        val parlayMatchesById = mapOf("m1" to match, "m2" to match2)

        val oneExpressParlayWin = BetOrder(
            id = "o3",
            orderType = "one_express",
            selections = listOf("Arsenal (-0.5)", "Over (2.5)"),
            matchIds = listOf("m1", "m2"),
            pickKeys = listOf("home", "over"),
            stakeKs = 5_000L,
            multiplier = 3.648,
            potentialPayoutKs = 18_240L,
            status = "accepted"
        )
        val parlayWinDecision = AutoSettleEngine.evaluate(oneExpressParlayWin, parlayMatchesById, emptyList())
        assertTrue(parlayWinDecision is AutoSettleDecision.Win)
        assertEquals(18_240L, (parlayWinDecision as AutoSettleDecision.Win).grossPayoutKs)

        val fiveFiveFiveParlayLoss = BetOrder(
            id = "o4",
            orderType = "five_five_five",
            selections = listOf("Arsenal (-0.5)", "Under (2.5)"),
            matchIds = listOf("m1", "m2"),
            pickKeys = listOf("home", "under"),
            stakeKs = 5_000L,
            multiplier = 3.61,
            potentialPayoutKs = 18_050L,
            status = "accepted"
        )
        val parlayLossDecision = AutoSettleEngine.evaluate(fiveFiveFiveParlayLoss, parlayMatchesById, emptyList())
        assertTrue(parlayLossDecision is AutoSettleDecision.Loss)
    }

    @Test
    fun systemGameConfig_pauseAndScheduledAutoOpenLogic() {
        val now = 1_700_000_000_000L

        // Unpaused by default
        val defaultCfg = SystemGameConfig()
        assertFalse(defaultCfg.isFootballPausedNow(now))
        assertFalse(defaultCfg.isGamesPausedNow(now))

        // Manual indefinite pause (reopenAtMs == 0)
        val indefiniteCfg = SystemGameConfig(
            footballPaused = true,
            footballReopenAtMs = 0L,
            gamesPaused = true,
            gamesReopenAtMs = 0L
        )
        assertTrue(indefiniteCfg.isFootballPausedNow(now))
        assertTrue(indefiniteCfg.isGamesPausedNow(now))

        // Scheduled reopen in future -> still paused
        val futureReopenCfg = SystemGameConfig(
            footballPaused = true,
            footballReopenAtMs = now + 60_000L,
            gamesPaused = true,
            gamesReopenAtMs = now + 120_000L
        )
        assertTrue(futureReopenCfg.isFootballPausedNow(now))
        assertTrue(futureReopenCfg.isGamesPausedNow(now))

        // Scheduled reopen reached or passed -> auto-opens
        assertFalse(futureReopenCfg.isFootballPausedNow(now + 60_000L))
        assertFalse(futureReopenCfg.isFootballPausedNow(now + 90_000L))
        assertTrue(futureReopenCfg.isGamesPausedNow(now + 90_000L))
        assertFalse(futureReopenCfg.isGamesPausedNow(now + 120_000L))
    }

    @Test
    fun betOrderKickoffAndWalletReceiptDefaults() {
        val defaultOrder = BetOrder()
        assertEquals("", defaultOrder.matchKickoffText)
        assertEquals(0L, defaultOrder.matchKickoffMs)

        val kickoffOrder = BetOrder(
            matchKickoffMs = 1_700_003_600_000L,
            matchKickoffText = "2026-11-23 21:30"
        )
        assertEquals("2026-11-23 21:30", kickoffOrder.matchKickoffText)
        assertEquals(1_700_003_600_000L, kickoffOrder.matchKickoffMs)

        val defaultWalletReq = WalletRequest()
        assertEquals("", defaultWalletReq.receiptImageBase64)

        val receiptReq = WalletRequest(receiptImageBase64 = "base64sampledata")
        assertEquals("base64sampledata", receiptReq.receiptImageBase64)
    }

    @Test
    fun miniGameEngine_stealthHouseAdvantageTargetsExpectedWinRateAndNearMisses() {
        val allDice = buildList {
            for (d1 in 1..6) for (d2 in 1..6) for (d3 in 1..6) add(Triple(d1, d2, d3))
        }
        fun bigPayout(d1: Int, d2: Int, d3: Int): Long {
            val isTriple = d1 == d2 && d2 == d3
            val sum = d1 + d2 + d3
            return if (!isTriple && sum in 11..17) 1950L else 0L
        }

        val highStakeProb = MiniGameEngine.computeAdaptiveHouseWinProbability(
            baseWinPct = 78,
            stakeKs = 50_000L,
            recentCustomerRounds = listOf(MiniGameRound(payoutKs = 2000L))
        )
        assertTrue(highStakeProb >= 0.90)

        val rng = Random(42)
        var houseWins = 0
        var nearMisses = 0
        var losses = 0
        val trials = 400
        repeat(trials) {
            val (d1, d2, d3) = MiniGameEngine.selectDiceRoll(
                allCombos = allDice,
                quotaCombos = allDice,
                directive = "stealth_house",
                stealthHouseWinPct = 78,
                betOption = "BIG",
                exactNumber = 11,
                stakeKs = 5_000L,
                recentCustomerRounds = emptyList(),
                computePayout = ::bigPayout,
                random = rng
            )
            val payout = bigPayout(d1, d2, d3)
            if (payout == 0L) {
                houseWins++
                losses++
                if ((d1 + d2 + d3) in 9..10) nearMisses++
            }
        }
        val houseWinRate = houseWins.toDouble() / trials.toDouble()
        assertTrue("Expected house win rate near 78%, got $houseWinRate", houseWinRate in 0.68..0.88)
        assertTrue("Expected noticeable near-miss losses for stealth feel", nearMisses > losses / 2)

        val allAnimals = buildList {
            for (a in 0..5) for (b in 0..5) for (c in 0..5) add(Triple(a, b, c))
        }
        var animalHouseWins = 0
        repeat(trials) {
            val r = MiniGameEngine.selectSixAnimalsRoll(
                allCombos = allAnimals,
                quotaCombos = allAnimals,
                selectedAnimalIndex = 2,
                directive = "stealth_house",
                stealthHouseWinPct = 78,
                stakeKs = 5_000L,
                recentCustomerRounds = emptyList(),
                random = rng
            )
            val matches = listOf(r.first, r.second, r.third).count { it == 2 }
            if (matches == 0) animalHouseWins++
        }
        val animalHouseRate = animalHouseWins.toDouble() / trials.toDouble()
        assertTrue("Expected animal house win rate near 78%, got $animalHouseRate", animalHouseRate in 0.68..0.88)
    }

    @Test
    fun reopenCountdownFormatter_displaysClearHmsAndDays() {
        val now = 1_700_000_000_000L
        assertEquals("01:05:09", formatCountdownPill(now + 3_909_000L, now))
        assertEquals("1ရက် 00:00:05", formatCountdownPill(now + 86_405_000L, now))
        val full = formatRemainingReopenCountdown(now + 3_909_000L, now)
        assertTrue(full.contains("မြန်မာစံတော်ချိန်"))
        assertTrue(full.contains("01:05:09 ကျန်"))
    }
}
