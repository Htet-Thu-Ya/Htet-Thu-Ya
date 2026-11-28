package com.example

import com.example.data.betting.AutoSettleDecision
import com.example.data.betting.AutoSettleEngine
import com.example.data.betting.KyayFormatter
import com.example.data.betting.MatchSheetCsvCodec
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

    @Test
    fun oneXBetUrlHelpers_andMarketOutcomeParser_workEndToEnd() {
        val defaultUrl = "https://1xlite-19808.pro/en/live"
        assertEquals(defaultUrl, com.example.data.network.FootballFeedService.normalizeOneXBetLiveUrl(defaultUrl))
        assertEquals("https://1xlite-19808.pro", com.example.data.network.FootballFeedService.extractOrigin(defaultUrl))
        assertEquals("en", com.example.data.network.FootballFeedService.extractLanguageCode(defaultUrl))
        assertEquals(
            "https://1xlite-99999.pro/my/live",
            com.example.data.network.FootballFeedService.normalizeOneXBetLiveUrl("http://1xlite-99999.pro/my/live")
        )
        assertEquals(
            "https://1xlite-99999.pro",
            com.example.data.network.FootballFeedService.extractOrigin("1xlite-99999.pro/my/live")
        )
        assertEquals(
            "my",
            com.example.data.network.FootballFeedService.extractLanguageCode("1xlite-99999.pro/my/live")
        )

        val asianParsed = KyayFormatter.parseOneXBetOutcomesToLines(
            listOf(
                KyayFormatter.OneXBetMarketOutcome(type = 7, param = -0.5, coef = 1.95),
                KyayFormatter.OneXBetMarketOutcome(type = 8, param = 0.5, coef = 1.91),
                KyayFormatter.OneXBetMarketOutcome(type = 9, param = 2.75, coef = 1.88),
                KyayFormatter.OneXBetMarketOutcome(type = 10, param = 2.75, coef = 1.96)
            )
        )
        assertEquals("Home -0.5", asianParsed.handicapLine)
        assertEquals("2.5/3", asianParsed.goalLine)
        assertEquals(1.90, asianParsed.bodyOdds, 0.001)
        assertEquals(1.90, asianParsed.goalOdds, 0.001)

        val awayFavParsed = KyayFormatter.parseOneXBetOutcomesToLines(
            listOf(
                KyayFormatter.OneXBetMarketOutcome(type = 7, param = 0.25, coef = 1.90),
                KyayFormatter.OneXBetMarketOutcome(type = 9, param = 2.5, coef = 1.91)
            )
        )
        assertEquals("Away -0.25", awayFavParsed.handicapLine)
        assertEquals("2.5", awayFavParsed.goalLine)
        assertEquals(1.90, awayFavParsed.bodyOdds, 0.001)
        assertEquals(1.90, awayFavParsed.goalOdds, 0.001)

        val moneylineFallback = KyayFormatter.parseOneXBetOutcomesToLines(
            listOf(
                KyayFormatter.OneXBetMarketOutcome(type = 1, param = null, coef = 1.48),
                KyayFormatter.OneXBetMarketOutcome(type = 3, param = null, coef = 6.20)
            )
        )
        assertEquals("Home -1", moneylineFallback.handicapLine)
        assertEquals("2.5", moneylineFallback.goalLine)
        assertEquals(1.90, moneylineFallback.bodyOdds, 0.001)
        assertEquals(1.90, moneylineFallback.goalOdds, 0.001)

        val nowMs = 1_700_000_000_000L
        val openFix = com.example.data.network.FootballFeedService.buildOneXBetFixture(
            gameId = "55123",
            league = "UEFA Champions League",
            homeTeam = "Arsenal",
            awayTeam = "Inter",
            homeImgFile = "arsenal.png",
            awayImgFile = "inter.png",
            startEpochSec = (nowMs / 1000L) + 7200L,
            homeScore = 0L,
            awayScore = 0L,
            livePeriodClock = "",
            isFinished = false,
            isLiveFeed = false,
            outcomes = listOf(
                KyayFormatter.OneXBetMarketOutcome(type = 7, param = -0.5, coef = 1.94),
                KyayFormatter.OneXBetMarketOutcome(type = 9, param = 2.5, coef = 1.90)
            ),
            origin = "https://1xlite-19808.pro",
            nowMs = nowMs
        )!!
        assertEquals("1xbet_55123", openFix.externalId)
        assertEquals("open", openFix.status)
        assertEquals("https://1xlite-19808.pro/sfiles/logo_teams/arsenal.png", openFix.homeLogoUrl)
        assertEquals("Home -0.5", openFix.handicapLine)
        assertEquals("2.5", openFix.goalLine)
        assertEquals(1.90, openFix.bodyOdds, 0.001)
        assertEquals(1.90, openFix.goalOdds, 0.001)

        val liveFix = com.example.data.network.FootballFeedService.buildOneXBetFixture(
            gameId = "55123",
            league = "UEFA Champions League",
            homeTeam = "Arsenal",
            awayTeam = "Inter",
            homeImgFile = "arsenal.png",
            awayImgFile = "inter.png",
            startEpochSec = (nowMs / 1000L) - 1800L,
            homeScore = 2L,
            awayScore = 1L,
            livePeriodClock = "64'",
            isFinished = false,
            isLiveFeed = true,
            outcomes = listOf(
                KyayFormatter.OneXBetMarketOutcome(type = 7, param = -0.5, coef = 1.94),
                KyayFormatter.OneXBetMarketOutcome(type = 9, param = 2.5, coef = 1.90)
            ),
            origin = "https://1xlite-19808.pro",
            nowMs = nowMs
        )!!
        assertEquals("closed", liveFix.status)
        assertEquals("LIVE 64'", liveFix.liveClock)
        assertEquals(2L, liveFix.homeScore)
        assertEquals(1L, liveFix.awayScore)

        val settledMatch = SportMatch(
            id = "ext_oneexp_1xbet_55123",
            homeTeam = openFix.homeTeam,
            awayTeam = openFix.awayTeam,
            handicapLine = openFix.handicapLine,
            goalLine = openFix.goalLine,
            bodyOdds = openFix.bodyOdds,
            goalOdds = openFix.goalOdds,
            status = "finished",
            homeScore = 2L,
            awayScore = 1L,
            boardSource = "one_express"
        )
        val decision = AutoSettleEngine.evaluate(
            BetOrder(
                id = "ord_1x",
                orderType = "one_express",
                selections = listOf("Arsenal (-0.5)"),
                matchIds = listOf("ext_oneexp_1xbet_55123"),
                pickKeys = listOf("home"),
                stakeKs = 10_000L,
                multiplier = 1.90,
                potentialPayoutKs = 19_000L,
                status = "accepted"
            ),
            mapOf("ext_oneexp_1xbet_55123" to settledMatch),
            emptyList()
        )
        assertTrue(decision is AutoSettleDecision.Win)
        assertEquals(19_000L, (decision as AutoSettleDecision.Win).grossPayoutKs)
    }

    @Test
    fun myanmarTimeUtil_resolvesKickoffEpochAndBettabilityInYangonTimezone() {
        val cal = java.util.Calendar.getInstance(com.example.data.util.MyanmarTimeUtil.YANGON_TZ).apply {
            set(2026, java.util.Calendar.NOVEMBER, 25, 2, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val expectedNov25 = cal.timeInMillis

        val crossMidnightMatch = SportMatch(
            id = "ucl_1",
            kickoffLabel = "25 Nov 02:30 AM",
            matchDate = "2026-11-24",
            status = "open"
        )
        val resolvedMs = com.example.data.util.MyanmarTimeUtil.resolveMatchKickoffEpochMs(
            crossMidnightMatch,
            expectedNov25 - 7_200_000L
        )
        assertEquals(expectedNov25, resolvedMs)
        assertEquals(expectedNov25 + 135L * 60_000L, resolvedMs + 135L * 60_000L)

        // Open before kickoff -> bettable; at or after kickoff -> not bettable
        assertTrue(com.example.data.util.MyanmarTimeUtil.isMatchBettable(crossMidnightMatch, expectedNov25 - 60_000L))
        assertFalse(com.example.data.util.MyanmarTimeUtil.isMatchBettable(crossMidnightMatch, expectedNov25))
        assertFalse(com.example.data.util.MyanmarTimeUtil.isMatchBettable(crossMidnightMatch, expectedNov25 + 60_000L))
        assertFalse(
            com.example.data.util.MyanmarTimeUtil.isMatchBettable(
                crossMidnightMatch.copy(status = "closed"),
                expectedNov25 - 60_000L
            )
        )
        assertFalse(
            com.example.data.util.MyanmarTimeUtil.isMatchBettable(
                crossMidnightMatch.copy(liveClock = "12'"),
                expectedNov25 - 60_000L
            )
        )

        // Dec -> Jan year rollover
        val janCal = java.util.Calendar.getInstance(com.example.data.util.MyanmarTimeUtil.YANGON_TZ).apply {
            set(2027, java.util.Calendar.JANUARY, 1, 2, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val janMatch = SportMatch(
            id = "ny_1",
            kickoffLabel = "01 Jan 02:00 AM",
            matchDate = "2026-12-31",
            status = "open"
        )
        assertEquals(
            janCal.timeInMillis,
            com.example.data.util.MyanmarTimeUtil.resolveMatchKickoffEpochMs(janMatch, janCal.timeInMillis - 7_200_000L)
        )
    }

    @Test
    fun oneXBetFixture_setsKickoffEpochMsAndClosesStartedMatch() {
        val nowMs = 1_700_000_000_000L
        val pastStartSec = (nowMs / 1000L) - 300L
        val pastNonLiveFeedFix = com.example.data.network.FootballFeedService.buildOneXBetFixture(
            gameId = "99100",
            league = "AFC Champions League Elite",
            homeTeam = "Vissel Kobe",
            awayTeam = "Buriram United",
            homeImgFile = "",
            awayImgFile = "",
            startEpochSec = pastStartSec,
            homeScore = 0L,
            awayScore = 0L,
            livePeriodClock = "",
            isFinished = false,
            isLiveFeed = false,
            outcomes = emptyList(),
            origin = "https://1xlite-19808.pro",
            nowMs = nowMs
        )!!
        assertEquals(pastStartSec * 1000L, pastNonLiveFeedFix.kickoffEpochMs)
        assertEquals("closed", pastNonLiveFeedFix.status)
        assertEquals("LIVE", pastNonLiveFeedFix.liveClock)

        val futureStartSec = (nowMs / 1000L) + 7200L
        val futureFix = com.example.data.network.FootballFeedService.buildOneXBetFixture(
            gameId = "99101",
            league = "Thai League 1",
            homeTeam = "BG Pathum",
            awayTeam = "Muangthong",
            homeImgFile = "",
            awayImgFile = "",
            startEpochSec = futureStartSec,
            homeScore = 0L,
            awayScore = 0L,
            livePeriodClock = "",
            isFinished = false,
            isLiveFeed = false,
            outcomes = emptyList(),
            origin = "https://1xlite-19808.pro",
            nowMs = nowMs
        )!!
        assertEquals(futureStartSec * 1000L, futureFix.kickoffEpochMs)
        assertEquals("open", futureFix.status)
        assertEquals(
            com.example.data.util.MyanmarTimeUtil.formatKickoffLabel(futureStartSec * 1000L),
            futureFix.kickoffLabel
        )
        assertEquals(
            com.example.data.util.MyanmarTimeUtil.formatMatchDateIso(futureStartSec * 1000L),
            futureFix.matchDate
        )
    }

    @Test
    fun formatAsianMarketTag_formatsHandicapAndGoalLine() {
        assertEquals("AH H-0.5 • O/U 2.5", KyayFormatter.formatAsianMarketTag("Home -0.5", "2.5"))
        assertEquals("AH A-0.25 • O/U 2.5/3", KyayFormatter.formatAsianMarketTag("Away -0.25", "2.5/3"))
        assertEquals("AH 0 • O/U 2.5", KyayFormatter.formatAsianMarketTag("Home -0", "2.5"))
    }

    @Test
    fun oneXBet1x2AndOverUnder_formatsParsesAndAutoSettlesCorrectly() {
        assertEquals("3.38", KyayFormatter.formatOneXBetOdds(3.38))
        assertEquals("3.74", KyayFormatter.formatOneXBetOdds(3.74))
        assertEquals("1.975", KyayFormatter.formatOneXBetOdds(1.975))
        assertEquals("1.90", KyayFormatter.formatOneXBetOdds(1.9))

        val parsed = KyayFormatter.parseOneXBetOutcomesToLines(
            listOf(
                KyayFormatter.OneXBetMarketOutcome(type = 1, param = null, coef = 3.38),
                KyayFormatter.OneXBetMarketOutcome(type = 2, param = null, coef = 3.74),
                KyayFormatter.OneXBetMarketOutcome(type = 3, param = null, coef = 1.975),
                KyayFormatter.OneXBetMarketOutcome(type = 9, param = 2.5, coef = 1.85),
                KyayFormatter.OneXBetMarketOutcome(type = 10, param = 2.5, coef = 1.96)
            )
        )
        assertEquals(3.38, parsed.win1Odds, 0.001)
        assertEquals(3.74, parsed.drawOdds, 0.001)
        assertEquals(1.975, parsed.win2Odds, 0.001)
        assertEquals(1.85, parsed.overOdds, 0.001)
        assertEquals(1.96, parsed.underOdds, 0.001)

        val homeWinMatch = SportMatch(
            id = "1x_m1",
            homeTeam = "AC Taipei",
            awayTeam = "Hang Yuen",
            goalLine = "2.5",
            win1Odds = 3.38,
            drawOdds = 3.74,
            win2Odds = 1.975,
            overOdds = 1.85,
            underOdds = 1.96,
            status = "finished",
            homeScore = 2L,
            awayScore = 1L,
            boardSource = "one_express"
        )
        val drawMatch = homeWinMatch.copy(id = "1x_m2", homeScore = 1L, awayScore = 1L)
        val awayWinMatch = homeWinMatch.copy(id = "1x_m3", homeScore = 0L, awayScore = 2L)
        val matchMap = mapOf(
            "1x_m1" to homeWinMatch,
            "1x_m2" to drawMatch,
            "1x_m3" to awayWinMatch
        )

        val w1Win = AutoSettleEngine.evaluate(
            BetOrder(
                id = "1x_o1",
                orderType = "one_express",
                selections = listOf("AC Taipei • W1"),
                matchIds = listOf("1x_m1"),
                pickKeys = listOf("w1"),
                stakeKs = 10_000L,
                multiplier = 3.38,
                potentialPayoutKs = 33_800L,
                status = "accepted"
            ),
            matchMap,
            emptyList()
        )
        assertTrue(w1Win is AutoSettleDecision.Win)
        assertEquals(33_800L, (w1Win as AutoSettleDecision.Win).grossPayoutKs)

        val drawWin = AutoSettleEngine.evaluate(
            BetOrder(
                id = "1x_o2",
                orderType = "one_express",
                selections = listOf("AC Taipei vs Hang Yuen • သရေ"),
                matchIds = listOf("1x_m2"),
                pickKeys = listOf("draw"),
                stakeKs = 10_000L,
                multiplier = 3.74,
                potentialPayoutKs = 37_400L,
                status = "accepted"
            ),
            matchMap,
            emptyList()
        )
        assertTrue(drawWin is AutoSettleDecision.Win)
        assertEquals(37_400L, (drawWin as AutoSettleDecision.Win).grossPayoutKs)

        val w2Win = AutoSettleEngine.evaluate(
            BetOrder(
                id = "1x_o3",
                orderType = "one_express",
                selections = listOf("Hang Yuen • W2"),
                matchIds = listOf("1x_m3"),
                pickKeys = listOf("w2"),
                stakeKs = 10_000L,
                multiplier = 1.975,
                potentialPayoutKs = 19_750L,
                status = "accepted"
            ),
            matchMap,
            emptyList()
        )
        assertTrue(w2Win is AutoSettleDecision.Win)
        assertEquals(19_750L, (w2Win as AutoSettleDecision.Win).grossPayoutKs)

        val w1OnDrawLoss = AutoSettleEngine.evaluate(
            BetOrder(
                id = "1x_o4",
                orderType = "one_express",
                selections = listOf("AC Taipei • W1"),
                matchIds = listOf("1x_m2"),
                pickKeys = listOf("w1"),
                stakeKs = 10_000L,
                multiplier = 3.38,
                potentialPayoutKs = 33_800L,
                status = "accepted"
            ),
            matchMap,
            emptyList()
        )
        assertTrue(w1OnDrawLoss is AutoSettleDecision.Loss)

        val mixedParlayWin = AutoSettleEngine.evaluate(
            BetOrder(
                id = "1x_o5",
                orderType = "one_express",
                selections = listOf("AC Taipei • W1", "ဂိုးအောက် 2.5"),
                matchIds = listOf("1x_m1", "1x_m3"),
                pickKeys = listOf("w1", "under"),
                stakeKs = 1_000L,
                multiplier = 6.625,
                potentialPayoutKs = 6_625L,
                status = "accepted"
            ),
            matchMap,
            emptyList()
        )
        assertTrue(mixedParlayWin is AutoSettleDecision.Win)
        assertEquals(6_625L, (mixedParlayWin as AutoSettleDecision.Win).grossPayoutKs)
    }

    @Test
    fun matchSheetCsvCodec_roundTripsCsvAndExcelTsv() {
        val sample = listOf(
            SportMatch(
                id = "m_csv_1",
                league = "French Ligue 1",
                homeTeam = "Troyes",
                awayTeam = "Le Havre",
                kickoffLabel = "24 Nov 02:15",
                handicapLine = "Home -0.5",
                goalLine = "2.5",
                bodyOdds = 1.95,
                goalOdds = 1.90,
                win1Odds = 2.15,
                drawOdds = 3.30,
                win2Odds = 3.45,
                overOdds = 1.92,
                underOdds = 1.88,
                status = "finished",
                homeScore = 3L,
                awayScore = 1L,
                boardSource = "one_express",
                matchDate = "2026-11-24",
                oneExpressKyay = "Home (-0.5) • 2.5"
            )
        )
        val csv = MatchSheetCsvCodec.exportToCsv(sample)
        val parsedCsv = MatchSheetCsvCodec.parseCsvOrTsv(csv, defaultBoardSource = "standard")
        assertEquals(1, parsedCsv.size)
        assertEquals("French Ligue 1", parsedCsv[0].league)
        assertEquals("Troyes", parsedCsv[0].homeTeam)
        assertEquals("Le Havre", parsedCsv[0].awayTeam)
        assertEquals("finished", parsedCsv[0].status)
        assertEquals(3L, parsedCsv[0].homeScore)
        assertEquals(1L, parsedCsv[0].awayScore)
        assertEquals("one_express", parsedCsv[0].boardSource)
        assertEquals(2.15, parsedCsv[0].win1Odds ?: 0.0, 0.001)

        val tsv = "league\thomeTeam\tawayTeam\tscore\tstatus\nPremier League\tArsenal\tChelsea\t2-1\tft"
        val parsedTsv = MatchSheetCsvCodec.parseCsvOrTsv(tsv, defaultBoardSource = "five_five_five")
        assertEquals(1, parsedTsv.size)
        assertEquals("Arsenal", parsedTsv[0].homeTeam)
        assertEquals(2L, parsedTsv[0].homeScore)
        assertEquals(1L, parsedTsv[0].awayScore)
        assertEquals("finished", parsedTsv[0].status)
        assertEquals("five_five_five", parsedTsv[0].boardSource)
    }

    @Test
    fun miniGameEngine_multiPickSixAnimals_dragonTiger_andLuckyWheel() {
        // Multi-pick 6 animals payout: stake 1,000 x {0, 2}, roll has 0 twice and 2 once
        val payout = MiniGameEngine.computeSixAnimalsMultiPickPayout(
            roll = Triple(0, 0, 2),
            selectedAnimalIndices = setOf(0, 2),
            stakePerAnimalKs = 1000L,
            baseBonus = 1.0
        )
        // idx 0 matches twice -> 1000 + 1000*2 = 3000; idx 2 matches once -> 1000 + 1000*1 = 2000
        assertEquals(5000L, payout)

        val allCombos = buildList {
            for (a in 0..5) for (b in 0..5) for (c in 0..5) add(Triple(a, b, c))
        }
        val winRoll = MiniGameEngine.selectSixAnimalsRollMultiPick(
            allCombos = allCombos,
            quotaCombos = allCombos,
            selectedAnimalIndices = setOf(1, 3, 5),
            stakePerAnimalKs = 1000L,
            baseBonus = 1.0,
            directive = "force_win",
            stealthHouseWinPct = 75,
            recentCustomerRounds = emptyList()
        )
        assertTrue(
            MiniGameEngine.computeSixAnimalsMultiPickPayout(winRoll, setOf(1, 3, 5), 1000L, 1.0) > 3000L
        )

        val dtWin = MiniGameEngine.selectDragonTigerOutcome(
            pick = "DRAGON",
            directive = "force_win",
            stealthHouseWinPct = 75,
            stakeKs = 1000L,
            recentCustomerRounds = emptyList()
        )
        assertTrue(dtWin.dragonRank > dtWin.tigerRank)

        val segments = MiniGameEngine.buildLuckyWheelSegments(2.0)
        assertEquals(8, segments.size)
        val wheelWinIdx = MiniGameEngine.selectLuckyWheelSegmentIndex(
            segments = segments,
            directive = "force_win",
            stealthHouseWinPct = 75,
            stakeKs = 1000L,
            recentCustomerRounds = emptyList()
        )
        assertTrue(segments[wheelWinIdx].multiplier > 1.0)
    }

    @Test
    fun fuzzyTeamMatches_handlesCommonPrefixesAndSuffixes() {
        assertTrue(com.example.data.network.FootballFeedService.fuzzyTeamMatches("ESTAC Troyes", "Troyes"))
        assertTrue(com.example.data.network.FootballFeedService.fuzzyTeamMatches("Le Havre AC", "Le Havre"))
        assertFalse(com.example.data.network.FootballFeedService.fuzzyTeamMatches("Arsenal", "Chelsea"))
    }

    @Test
    fun matchSheetCsvCodec_myanmar16ColAtoPRoundTrip_supports1xBet555MixAndTypoHeaders() {
        val tsvAtoP = listOf(
            "\tအိမ်ရှင်ဂိုး\tဧည့်သည်ဂိုး\tလိဂ်\tအိမ်ရှင်အသင်း\tဧည့်သည်အသင်း\tကန်ချိန်\tဘော်ဒီကြေး\tဂိုးပေါင်း\tW1\tသရေ\tW2\tဂိုးပါ်\tဂိုးအောက်\tBody Odds\tGoal Odds",
            "1xBET\t2\t1\tPremier League\tArsenal\tChelsea\t21:30\tHome -0.5\t2.5\t1.92\t3.40\t4.10\t1.88\t1.95\t1.95\t1.90",
            "555 Mix\t0\t0\tLa Liga\tReal Madrid\tBarcelona\t02:30\tHome -1\t3.0\t2.05\t3.50\t3.60\t1.90\t1.90\t1.92\t1.92"
        ).joinToString("\n")

        val parsed = MatchSheetCsvCodec.parseCsvOrTsv(tsvAtoP, defaultBoardSource = "standard")
        assertEquals(2, parsed.size)

        assertEquals("one_express", parsed[0].boardSource)
        assertEquals(2L, parsed[0].homeScore)
        assertEquals(1L, parsed[0].awayScore)
        assertEquals("Premier League", parsed[0].league)
        assertEquals("Arsenal", parsed[0].homeTeam)
        assertEquals("Chelsea", parsed[0].awayTeam)
        assertEquals("21:30", parsed[0].kickoffLabel)
        assertEquals("Home -0.5", parsed[0].handicapLine)
        assertEquals("2.5", parsed[0].goalLine)
        assertEquals(1.92, parsed[0].win1Odds ?: 0.0, 0.001)
        assertEquals(3.40, parsed[0].drawOdds ?: 0.0, 0.001)
        assertEquals(4.10, parsed[0].win2Odds ?: 0.0, 0.001)
        assertEquals(1.88, parsed[0].overOdds ?: 0.0, 0.001)
        assertEquals(1.95, parsed[0].underOdds ?: 0.0, 0.001)
        assertEquals(1.95, parsed[0].bodyOdds, 0.001)
        assertEquals(1.90, parsed[0].goalOdds, 0.001)

        assertEquals("five_five_five", parsed[1].boardSource)
        assertEquals("Real Madrid", parsed[1].homeTeam)
        assertEquals("Barcelona", parsed[1].awayTeam)

        val exportedCsv = MatchSheetCsvCodec.exportToMyanmarSheetCsv(
            listOf(
                SportMatch(
                    id = "s1",
                    league = "Premier League",
                    homeTeam = "Arsenal",
                    awayTeam = "Chelsea",
                    kickoffLabel = "21:30",
                    handicapLine = "Home -0.5",
                    goalLine = "2.5",
                    bodyOdds = 1.95,
                    goalOdds = 1.90,
                    win1Odds = 1.92,
                    drawOdds = 3.40,
                    win2Odds = 4.10,
                    overOdds = 1.88,
                    underOdds = 1.95,
                    homeScore = 2L,
                    awayScore = 1L,
                    boardSource = "one_express"
                )
            )
        )
        assertTrue(exportedCsv.contains("1xBET"))
        assertTrue(exportedCsv.contains("ဂိုးပေါ်"))
        val reParsed = MatchSheetCsvCodec.parseCsvOrTsv(exportedCsv)
        assertEquals(1, reParsed.size)
        assertEquals("one_express", reParsed[0].boardSource)
        assertEquals(1.88, reParsed[0].overOdds ?: 0.0, 0.001)
        assertEquals(1.95, reParsed[0].bodyOdds, 0.001)
    }

    @Test
    fun adminPermissions_andLotteryAndUpdateDefaults_roundTripCleanly() {
        val perms = com.example.data.model.AdminPermissions(
            canApproveWallet = true,
            canManage2D3DRoom = true,
            canManageBoardSheet = true
        )
        val restored = com.example.data.model.AdminPermissions.fromMap(perms.toMap())
        assertTrue(restored.canApproveWallet)
        assertTrue(restored.canManage2D3DRoom)
        assertTrue(restored.canManageBoardSheet)
        assertFalse(restored.canManageOrders)

        val lotteryCfg = com.example.data.model.Lottery2D3DControlConfig()
        assertTrue(lotteryCfg.autoMode)
        assertTrue(lotteryCfg.presentPakokku.isNotBlank())
        assertTrue(lotteryCfg.presentAtakeCard.isNotBlank())

        val updateCfg = com.example.data.model.AppUpdateConfig()
        assertFalse(updateCfg.bannerTitle.contains("Htet Thu Ya", ignoreCase = true))
        assertFalse(updateCfg.bannerSubtitle.contains("Htet Thu Ya", ignoreCase = true))
        assertFalse(updateCfg.dialogTitle.contains("Htet Thu Ya", ignoreCase = true))
        assertTrue(updateCfg.installButtonLabel.isNotBlank())
        assertTrue(updateCfg.laterButtonLabel.isNotBlank())
    }

    @Test
    fun canonical42Leagues_andGoogleSheetEditUrlNormalization_workCorrectly() {
        val leagues = com.example.data.network.FootballFeedService.DEFAULT_LEAGUE_OPTIONS
        assertEquals(42, leagues.size)
        assertEquals("Turkey 1. Lig", leagues.first())
        assertEquals("Argentine Division 1", leagues.last())

        assertEquals(
            "English Premier League",
            com.example.data.network.FootballFeedService.canonicalLeagueName("Premier League")
        )
        assertEquals(
            "Italy Serie A",
            com.example.data.network.FootballFeedService.canonicalLeagueName("Italian Serie A")
        )
        assertEquals(
            "England Championship",
            com.example.data.network.FootballFeedService.canonicalLeagueName("EFL Championship")
        )
        assertTrue(
            com.example.data.network.FootballFeedService.leagueSortOrderIndex("Turkey 1. Lig") <
                com.example.data.network.FootballFeedService.leagueSortOrderIndex("English Premier League")
        )
        assertTrue(
            com.example.data.network.FootballFeedService.leagueSortOrderIndex("English Premier League") <
                com.example.data.network.FootballFeedService.leagueSortOrderIndex("Unknown Friendly Cup")
        )
        assertTrue(
            com.example.data.network.FootballFeedService.matchesAllowedLeague("Spanish La Liga", setOf("spanish la liga"))
        )
        assertFalse(
            com.example.data.network.FootballFeedService.matchesAllowedLeague("Spanish La Liga 2", setOf("spanish la liga"))
        )
        assertFalse(
            com.example.data.network.FootballFeedService.matchesAllowedLeague("German Bundesliga 2", setOf("german bundesliga"))
        )

        val csvUrl = "https://docs.google.com/spreadsheets/d/abc123XYZ/export?format=csv&gid=42"
        val editUrl = com.example.data.network.FootballFeedService.normalizeGoogleSheetEditUrl(csvUrl)
        assertEquals("https://docs.google.com/spreadsheets/d/abc123XYZ/edit#gid=42", editUrl)
        assertEquals(
            "https://docs.google.com/spreadsheets/d/abc123XYZ/export?format=csv&gid=42",
            com.example.data.network.FootballFeedService.normalizeGoogleSheetCsvUrl(editUrl)
        )
    }

    @Test
    fun autoSettleEngine_twoDSessionSplitAndDateIsolation() {
        val cal = java.util.Calendar.getInstance(com.example.data.util.MyanmarTimeUtil.YANGON_TZ).apply {
            set(2026, java.util.Calendar.NOVEMBER, 27, 12, 10, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val noonTodayMs = cal.timeInMillis
        val todayIso = com.example.data.util.MyanmarTimeUtil.todayIsoDate(noonTodayMs)
        val tomorrowIso = com.example.data.util.MyanmarTimeUtil.formatMatchDateIso(noonTodayMs + 86_400_000L)

        val liveStatus = com.example.data.model.Live2DStatus(
            twod = "42",
            morning1201 = "42",
            evening430 = "88"
        )
        val config = com.example.data.model.Lottery2D3DControlConfig(autoMode = true)

        val morningOrderToday = BetOrder(
            id = "2d_m_today",
            orderType = "two_d",
            drawRound = "2D နေ့လည် (12:01 PM) • $todayIso",
            selections = listOf("42 (1,000 Ks)"),
            stakeKs = 1_000L,
            multiplier = 80.0,
            potentialPayoutKs = 80_000L,
            status = "accepted"
        )
        val eveningOrderToday = BetOrder(
            id = "2d_e_today",
            orderType = "two_d",
            drawRound = "2D ညနေ (4:30 PM) • $todayIso",
            selections = listOf("88 (1,000 Ks)"),
            stakeKs = 1_000L,
            multiplier = 80.0,
            potentialPayoutKs = 80_000L,
            status = "accepted"
        )
        val morningOrderTomorrow = morningOrderToday.copy(
            id = "2d_m_tomorrow",
            drawRound = "2D နေ့လည် (12:01 PM) • $tomorrowIso"
        )

        // At 12:10 PM Yangon time: 12:01 PM settles, 4:30 PM does NOT settle yet, tomorrow does NOT settle
        val mDecision = AutoSettleEngine.evaluate(
            order = morningOrderToday,
            matchesById = emptyMap(),
            draws = emptyList(),
            live2DConfig = config,
            live2DStatus = liveStatus,
            nowMs = noonTodayMs
        )
        assertTrue(mDecision is AutoSettleDecision.Win)
        assertEquals(80_000L, (mDecision as AutoSettleDecision.Win).grossPayoutKs)

        val eDecisionBefore430 = AutoSettleEngine.evaluate(
            order = eveningOrderToday,
            matchesById = emptyMap(),
            draws = emptyList(),
            live2DConfig = config,
            live2DStatus = liveStatus,
            nowMs = noonTodayMs
        )
        assertEquals(AutoSettleDecision.NotReady, eDecisionBefore430)

        val tomorrowDecision = AutoSettleEngine.evaluate(
            order = morningOrderTomorrow,
            matchesById = emptyMap(),
            draws = emptyList(),
            live2DConfig = config,
            live2DStatus = liveStatus,
            nowMs = noonTodayMs
        )
        assertEquals(AutoSettleDecision.NotReady, tomorrowDecision)

        // At 16:35 PM Yangon time: 4:30 PM settles cleanly
        val after430Ms = noonTodayMs + (4 * 60 + 25) * 60_000L
        val eDecisionAfter430 = AutoSettleEngine.evaluate(
            order = eveningOrderToday,
            matchesById = emptyMap(),
            draws = emptyList(),
            live2DConfig = config,
            live2DStatus = liveStatus,
            nowMs = after430Ms
        )
        assertTrue(eDecisionAfter430 is AutoSettleDecision.Win)
        assertEquals(80_000L, (eDecisionAfter430 as AutoSettleDecision.Win).grossPayoutKs)
    }

    @Test
    fun systemGameConfig_perGameDisableAndTimerAutoReopen_andExactSearchIsolation() {
        val now = 1_700_000_000_000L
        assertEquals(10, SystemGameConfig.ALL_GAME_KEYS.size)
        assertTrue("one_express" in SystemGameConfig.ALL_GAME_KEYS)
        assertTrue("five_five_five" in SystemGameConfig.ALL_GAME_KEYS)

        val defaultCfg = SystemGameConfig()
        SystemGameConfig.ALL_GAME_KEYS.forEach { key ->
            assertTrue("Expected $key enabled by default", defaultCfg.isGameEnabled(key, now))
        }

        // Disabling ALL 10 games keeps every game disabled (fixes empty enabledGames bug)
        val allDisabledCfg = SystemGameConfig(
            enabledGames = emptyList(),
            disabledGames = SystemGameConfig.ALL_GAME_KEYS
        )
        SystemGameConfig.ALL_GAME_KEYS.forEach { key ->
            assertFalse("Expected $key disabled when all 10 disabled", allDisabledCfg.isGameEnabled(key, now))
        }

        // Timed disable on one_express and dice -> disabled before expiry, auto-reopens at expiry
        val timedCfg = SystemGameConfig(
            disabledGames = listOf("one_express", "dice"),
            perGameReopenAtMs = mapOf(
                "one_express" to now + 60_000L,
                "dice" to now + 120_000L
            )
        )
        assertFalse(timedCfg.isGameEnabled("one_express", now))
        assertFalse(timedCfg.isGameEnabled("dice", now))
        assertTrue(timedCfg.isGameEnabled("five_five_five", now))

        assertTrue(timedCfg.isGameEnabled("one_express", now + 60_000L))
        assertFalse(timedCfg.isGameEnabled("dice", now + 60_000L))
        assertTrue(timedCfg.isGameEnabled("dice", now + 120_000L))

        // Exact canonical league search must NOT bleed into Division 2 leagues
        assertTrue(
            com.example.data.network.FootballFeedService.matchesSearchQuery(
                rawLeague = "Spanish La Liga",
                homeTeam = "Real Madrid",
                awayTeam = "Barcelona",
                rawQuery = "Spanish La Liga"
            )
        )
        assertFalse(
            com.example.data.network.FootballFeedService.matchesSearchQuery(
                rawLeague = "Spanish La Liga 2",
                homeTeam = "Eibar",
                awayTeam = "Levante",
                rawQuery = "Spanish La Liga"
            )
        )
        assertFalse(
            com.example.data.network.FootballFeedService.matchesSearchQuery(
                rawLeague = "German Bundesliga 2",
                homeTeam = "HSV",
                awayTeam = "Schalke",
                rawQuery = "German Bundesliga"
            )
        )
        // Partial query still works for quick search
        assertTrue(
            com.example.data.network.FootballFeedService.matchesSearchQuery(
                rawLeague = "Turkey 1. Lig",
                homeTeam = "Eyupspor",
                awayTeam = "Kocaelispor",
                rawQuery = "Turkey"
            )
        )
    }
}
