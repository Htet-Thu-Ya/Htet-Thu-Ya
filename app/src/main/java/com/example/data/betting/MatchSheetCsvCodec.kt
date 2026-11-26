package com.example.data.betting

import com.example.data.model.SportMatch
import com.example.data.util.MyanmarTimeUtil
import java.util.Locale

data class SheetMatchRow(
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffLabel: String = "",
    val handicapLine: String = "Home -0.5",
    val goalLine: String = "2.5",
    val bodyOdds: Double = KyayFormatter.LOTAYA_STANDARD_BODY_ODDS,
    val goalOdds: Double = KyayFormatter.LOTAYA_STANDARD_GOAL_ODDS,
    val win1Odds: Double? = null,
    val drawOdds: Double? = null,
    val win2Odds: Double? = null,
    val overOdds: Double? = null,
    val underOdds: Double? = null,
    val status: String = "open",
    val homeScore: Long = 0L,
    val awayScore: Long = 0L,
    val boardSource: String = "standard",
    val matchDate: String = "",
    val oneExpressKyay: String = "",
    val fiveFiveFiveKyay: String = ""
)

object MatchSheetCsvCodec {

    private val CSV_HEADERS = listOf(
        "league",
        "homeTeam",
        "awayTeam",
        "kickoffLabel",
        "handicapLine",
        "goalLine",
        "bodyOdds",
        "goalOdds",
        "win1Odds",
        "drawOdds",
        "win2Odds",
        "overOdds",
        "underOdds",
        "status",
        "homeScore",
        "awayScore",
        "boardSource",
        "matchDate",
        "oneExpressKyay",
        "fiveFiveFiveKyay"
    )

    fun exportToCsv(matches: List<SportMatch>): String {
        val sb = StringBuilder()
        sb.appendLine(CSV_HEADERS.joinToString(","))
        matches.forEach { m ->
            val cols = listOf(
                m.league,
                m.homeTeam,
                m.awayTeam,
                m.kickoffLabel,
                m.handicapLine,
                m.goalLine,
                fmtNum(m.bodyOdds),
                fmtNum(m.goalOdds),
                fmtNum(m.win1Odds),
                fmtNum(m.drawOdds),
                fmtNum(m.win2Odds),
                fmtNum(m.overOdds),
                fmtNum(m.underOdds),
                m.status,
                m.homeScore.toString(),
                m.awayScore.toString(),
                m.boardSource.ifBlank { "standard" },
                m.matchDate,
                m.oneExpressKyay,
                m.fiveFiveFiveKyay
            )
            sb.appendLine(cols.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    fun parseCsvOrTsv(rawText: String, defaultBoardSource: String = "standard"): List<SheetMatchRow> {
        val lines = rawText.replace("\r\n", "\n").replace("\r", "\n")
            .split('\n')
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val firstLine = lines.first().removePrefix("\uFEFF")
        val delimiter = if (firstLine.count { it == '\t' } > firstLine.count { it == ',' }) '\t' else ','
        val firstCells = parseDelimitedLine(firstLine, delimiter)
        val hasHeader = firstCells.any { cell ->
            val key = cell.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
            key in setOf("league", "hometeam", "home", "awayteam", "away", "handicapline", "goalline", "status")
        }

        val headerIndex = if (hasHeader) {
            firstCells.mapIndexed { idx, h -> normalizeHeaderKey(h) to idx }.toMap()
        } else {
            emptyMap()
        }

        val dataLines = if (hasHeader) lines.drop(1) else lines
        val rows = mutableListOf<SheetMatchRow>()
        val todayIso = MyanmarTimeUtil.todayIsoDate()

        for (line in dataLines) {
            val cleanLine = line.removePrefix("\uFEFF")
            val cells = parseDelimitedLine(cleanLine, delimiter)
            if (cells.all { it.isBlank() }) continue

            fun cell(vararg keys: String, fallbackIdx: Int): String {
                if (hasHeader) {
                    for (k in keys) {
                        val idx = headerIndex[k]
                        if (idx != null && idx in cells.indices) {
                            return cells[idx].trim()
                        }
                    }
                    return ""
                }
                return cells.getOrNull(fallbackIdx)?.trim().orEmpty()
            }

            val league = cell("league", "cup", "competition", fallbackIdx = 0).ifBlank { "Football" }
            val home = cell("hometeam", "home", "team1", fallbackIdx = 1)
            val away = cell("awayteam", "away", "team2", fallbackIdx = 2)
            if (home.isBlank() || away.isBlank()) continue

            val kickoff = cell("kickofflabel", "kickoff", "time", "start", fallbackIdx = 3)
            val handicap = cell("handicapline", "handicap", "body", "ah", fallbackIdx = 4).ifBlank { "Home -0.5" }
            val goal = cell("goalline", "goal", "ou", "total", fallbackIdx = 5).ifBlank { "2.5" }
            val bodyOdds = cell("bodyodds", "body_odds", fallbackIdx = 6).toDoubleOrNull()?.coerceIn(1.05, 50.0)
                ?: KyayFormatter.LOTAYA_STANDARD_BODY_ODDS
            val goalOdds = cell("goalodds", "goal_odds", fallbackIdx = 7).toDoubleOrNull()?.coerceIn(1.05, 50.0)
                ?: KyayFormatter.LOTAYA_STANDARD_GOAL_ODDS

            val win1 = cell("win1odds", "win1", "w1", fallbackIdx = 8).toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val draw = cell("drawodds", "draw", "x", fallbackIdx = 9).toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val win2 = cell("win2odds", "win2", "w2", fallbackIdx = 10).toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val over = cell("overodds", "over", fallbackIdx = 11).toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val under = cell("underodds", "under", fallbackIdx = 12).toDoubleOrNull()?.coerceIn(1.01, 99.0)

            val rawStatus = cell("status", "state", fallbackIdx = if (hasHeader) 13 else 8).lowercase(Locale.US)
            val mappedStatus = when {
                rawStatus in listOf("finished", "ft", "done", "ended", "completed", "ပြီးဆုံး") -> "finished"
                rawStatus in listOf("closed", "live", "started", "locked", "ပိတ်") -> "closed"
                else -> "open"
            }

            var homeScore = cell("homescore", "score1", fallbackIdx = if (hasHeader) 14 else 9).toLongOrNull()
            var awayScore = cell("awayscore", "score2", fallbackIdx = if (hasHeader) 15 else 10).toLongOrNull()
            if (homeScore == null || awayScore == null) {
                val combinedScore = cell("score", "result", "ft", fallbackIdx = -1)
                val scoreMatch = Regex("""(\d+)\s*[-:]\s*(\d+)""").find(combinedScore)
                if (scoreMatch != null) {
                    homeScore = scoreMatch.groupValues[1].toLongOrNull() ?: 0L
                    awayScore = scoreMatch.groupValues[2].toLongOrNull() ?: 0L
                }
            }

            val rawBoard = cell("boardsource", "board", "source", fallbackIdx = 16).lowercase(Locale.US)
            val boardSource = when {
                rawBoard in listOf("one_express", "1xbet", "oneexpress", "1x") -> "one_express"
                rawBoard in listOf("five_five_five", "555", "555mix", "mix") -> "five_five_five"
                rawBoard == "standard" -> "standard"
                else -> defaultBoardSource
            }

            val matchDate = cell("matchdate", "date", fallbackIdx = 17).ifBlank { todayIso }
            val oneExpKyay = cell("oneexpresskyay", "1xbetkyay", fallbackIdx = 18)
            val fffKyay = cell("fivefivefivekyay", "555kyay", fallbackIdx = 19)

            rows.add(
                SheetMatchRow(
                    league = league.take(60),
                    homeTeam = home.take(60),
                    awayTeam = away.take(60),
                    kickoffLabel = kickoff.take(40),
                    handicapLine = handicap.take(40),
                    goalLine = goal.take(20),
                    bodyOdds = bodyOdds,
                    goalOdds = goalOdds,
                    win1Odds = win1,
                    drawOdds = draw,
                    win2Odds = win2,
                    overOdds = over,
                    underOdds = under,
                    status = mappedStatus,
                    homeScore = (homeScore ?: 0L).coerceIn(0L, 99L),
                    awayScore = (awayScore ?: 0L).coerceIn(0L, 99L),
                    boardSource = boardSource,
                    matchDate = matchDate.take(20),
                    oneExpressKyay = oneExpKyay.take(60),
                    fiveFiveFiveKyay = fffKyay.take(60)
                )
            )
        }
        return rows
    }

    private fun normalizeHeaderKey(raw: String): String {
        return raw.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
    }

    private fun parseDelimitedLine(line: String, delimiter: Char): List<String> {
        if (delimiter == '\t') return line.split('\t')
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    cur.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == delimiter && !inQuotes) {
                out.add(cur.toString())
                cur.clear()
            } else {
                cur.append(ch)
            }
            i++
        }
        out.add(cur.toString())
        return out
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(',') || clean.contains('"')) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    private fun fmtNum(v: Double): String {
        return if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.2f", v)
    }
}
