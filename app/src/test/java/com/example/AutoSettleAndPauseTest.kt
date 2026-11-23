package com.example

import com.example.data.betting.AutoSettleDecision
import com.example.data.betting.AutoSettleEngine
import com.example.data.betting.KyayFormatter
import com.example.data.model.BetOrder
import com.example.data.model.SportMatch
import com.example.data.model.SystemGameConfig
import com.example.data.model.WalletRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSettleAndPauseTest {

    @Test
    fun formatOneExpressKyay_formatsMyanmarHandicapAndGoalLines() {
        val homeHalf = KyayFormatter.formatOneExpressKyay(
            handicapLine = "Home -0.5",
            goalLine = "2.5",
            bodyOdds = 1.92,
            goalOdds = 1.90
        )
        assertTrue(homeHalf.contains("1X: H-0.5 (1.92)"))
        assertTrue(homeHalf.contains("G 2.5 (1.9)"))

        val awayQuarter = KyayFormatter.formatOneExpressKyay(
            handicapLine = "Away -0.25",
            goalLine = "3",
            bodyOdds = 1.95,
            goalOdds = 1.88
        )
        assertTrue(awayQuarter.contains("1X: A 0+50 (1.95)"))
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
        assertTrue(evenLine.startsWith("555: H="))
        assertTrue(evenLine.contains("ဂိုး 2.5/3 (1.92)"))
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
}
