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

    val MYANMAR_SHEET_HEADERS = listOf(
        "",             // A: boardSource (1xBET / 555 Mix / Standard)
        "အိမ်ရှင်ဂိုး", // B: homeScore
        "ဧည့်သည်ဂိုး", // C: awayScore
        "လိဂ်",         // D: league
        "အိမ်ရှင်အသင်း",// E: homeTeam
        "ဧည့်သည်အသင်း",// F: awayTeam
        "ကန်ချိန်",      // G: kickoffLabel
        "ဘော်ဒီကြေး",   // H: handicapLine
        "ဂိုးပေါင်း",    // I: goalLine
        "W1",           // J: win1Odds
        "သရေ",         // K: drawOdds
        "W2",           // L: win2Odds
        "ဂိုးပေါ်",      // M: overOdds (also accepts ဂိုးပါ်)
        "ဂိုးအောက်",    // N: underOdds
        "Body Odds",    // O: bodyOdds
        "Goal Odds"     // P: goalOdds
    )

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

    fun formatBoardSourceDisplay(boardSource: String): String = when (boardSource.lowercase(Locale.US)) {
        "one_express", "1xbet", "oneexpress", "1x" -> "1xBET"
        "five_five_five", "555", "555mix", "mix", "555 mix" -> "555 Mix"
        else -> "Standard"
    }

    fun parseBoardSource(raw: String, fallback: String = "standard"): String {
        val low = raw.trim().lowercase(Locale.US)
        return when {
            low in listOf("one_express", "1xbet", "1x bet", "oneexpress", "1x") -> "one_express"
            low in listOf("five_five_five", "555", "555mix", "555 mix", "mix") -> "five_five_five"
            low == "standard" -> "standard"
            else -> fallback
        }
    }

    fun exportToMyanmarSheetCsv(matches: List<SportMatch>): String {
        val sb = StringBuilder()
        sb.appendLine(MYANMAR_SHEET_HEADERS.joinToString(",") { escapeCsv(it) })
        matches.forEach { m ->
            val cols = listOf(
                formatBoardSourceDisplay(m.boardSource),
                m.homeScore.toString(),
                m.awayScore.toString(),
                m.league,
                m.homeTeam,
                m.awayTeam,
                m.kickoffLabel,
                m.handicapLine,
                m.goalLine,
                fmtNum(m.win1Odds),
                fmtNum(m.drawOdds),
                fmtNum(m.win2Odds),
                fmtNum(m.overOdds),
                fmtNum(m.underOdds),
                fmtNum(m.bodyOdds),
                fmtNum(m.goalOdds)
            )
            sb.appendLine(cols.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

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
        val knownHeaderTokens = setOf(
            "league", "hometeam", "home", "awayteam", "away", "handicapline", "goalline", "status",
            "bodyodds", "goalodds", "w1", "w2",
            "အိမ်ရှင်ဂိုး", "ဧည့်သည်ဂိုး", "လိဂ်", "အိမ်ရှင်အသင်း", "ဧည့်သည်အသင်း",
            "ကန်ချိန်", "ဘော်ဒီကြေး", "ဂိုးပေါင်း", "သရေ", "ဂိုးပေါ်", "ဂိုးပါ်", "ဂိုးအောက်"
        )
        val normalizedFirst = firstCells.map { normalizeHeaderKey(it) }
        val hasHeader = normalizedFirst.any { it in knownHeaderTokens }
        val isMyanmarSheetLayout = hasHeader && (
            normalizedFirst.firstOrNull().isNullOrBlank() ||
                normalizedFirst.contains("အိမ်ရှင်အသင်း") ||
                normalizedFirst.contains("လိဂ်")
            )

        val headerIndex = if (hasHeader) {
            buildMap {
                firstCells.forEachIndexed { idx, h ->
                    val k = normalizeHeaderKey(h)
                    if (k.isBlank() && idx == 0) {
                        put("_col_a", idx)
                    } else if (k.isNotBlank()) {
                        put(k, idx)
                    }
                }
            }
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
                        val idx = headerIndex[normalizeHeaderKey(k)] ?: headerIndex[k]
                        if (idx != null && idx in cells.indices) {
                            return cells[idx].trim()
                        }
                    }
                    return ""
                }
                return cells.getOrNull(fallbackIdx)?.trim().orEmpty()
            }

            val league = cell("လိဂ်", "league", "cup", "competition", fallbackIdx = if (isMyanmarSheetLayout) 3 else 0)
                .ifBlank { "Football" }
            val home = cell("အိမ်ရှင်အသင်း", "hometeam", "home", "team1", fallbackIdx = if (isMyanmarSheetLayout) 4 else 1)
            val away = cell("ဧည့်သည်အသင်း", "awayteam", "away", "team2", fallbackIdx = if (isMyanmarSheetLayout) 5 else 2)
            if (home.isBlank() || away.isBlank()) continue

            val kickoff = cell("ကန်ချိန်", "kickofflabel", "kickoff", "time", "start", fallbackIdx = if (isMyanmarSheetLayout) 6 else 3)
            val handicap = cell("ဘော်ဒီကြေး", "handicapline", "handicap", "body", "ah", fallbackIdx = if (isMyanmarSheetLayout) 7 else 4)
                .ifBlank { "Home -0.5" }
            val goal = cell("ဂိုးပေါင်း", "goalline", "goal", "ou", "total", fallbackIdx = if (isMyanmarSheetLayout) 8 else 5)
                .ifBlank { "2.5" }
            val bodyOdds = cell("bodyodds", "body_odds", fallbackIdx = if (isMyanmarSheetLayout) 14 else 6)
                .toDoubleOrNull()?.coerceIn(1.05, 50.0)
                ?: KyayFormatter.LOTAYA_STANDARD_BODY_ODDS
            val goalOdds = cell("goalodds", "goal_odds", fallbackIdx = if (isMyanmarSheetLayout) 15 else 7)
                .toDoubleOrNull()?.coerceIn(1.05, 50.0)
                ?: KyayFormatter.LOTAYA_STANDARD_GOAL_ODDS

            val win1 = cell("w1", "win1odds", "win1", fallbackIdx = if (isMyanmarSheetLayout) 9 else 8)
                .toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val draw = cell("သရေ", "drawodds", "draw", "x", fallbackIdx = if (isMyanmarSheetLayout) 10 else 9)
                .toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val win2 = cell("w2", "win2odds", "win2", fallbackIdx = if (isMyanmarSheetLayout) 11 else 10)
                .toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val over = cell("ဂိုးပေါ်", "ဂိုးပါ်", "overodds", "over", fallbackIdx = if (isMyanmarSheetLayout) 12 else 11)
                .toDoubleOrNull()?.coerceIn(1.01, 99.0)
            val under = cell("ဂိုးအောက်", "underodds", "under", fallbackIdx = if (isMyanmarSheetLayout) 13 else 12)
                .toDoubleOrNull()?.coerceIn(1.01, 99.0)

            val rawStatus = cell("status", "state", fallbackIdx = if (hasHeader && !isMyanmarSheetLayout) 13 else -1)
                .lowercase(Locale.US)
            val mappedStatus = when {
                rawStatus in listOf("finished", "ft", "done", "ended", "completed", "ပြီးဆုံး") -> "finished"
                rawStatus in listOf("closed", "live", "started", "locked", "ပိတ်") -> "closed"
                kickoff.equals("FT", ignoreCase = true) || kickoff.equals("Finished", ignoreCase = true) -> "finished"
                kickoff.startsWith("LIVE", ignoreCase = true) -> "closed"
                else -> "open"
            }

            var homeScore = cell("အိမ်ရှင်ဂိုး", "homescore", "score1", fallbackIdx = if (isMyanmarSheetLayout) 1 else if (hasHeader) 14 else 9)
                .toLongOrNull()
            var awayScore = cell("ဧည့်သည်ဂိုး", "awayscore", "score2", fallbackIdx = if (isMyanmarSheetLayout) 2 else if (hasHeader) 15 else 10)
                .toLongOrNull()
            if (homeScore == null || awayScore == null) {
                val combinedScore = cell("score", "result", "ft", fallbackIdx = -1)
                val scoreMatch = Regex("""(\d+)\s*[-:]\s*(\d+)""").find(combinedScore)
                if (scoreMatch != null) {
                    homeScore = scoreMatch.groupValues[1].toLongOrNull() ?: 0L
                    awayScore = scoreMatch.groupValues[2].toLongOrNull() ?: 0L
                }
            }

            val rawBoard = cell("_col_a", "boardsource", "board", "source", fallbackIdx = if (isMyanmarSheetLayout) 0 else 16)
            val boardSource = parseBoardSource(rawBoard, defaultBoardSource)

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
        return raw.trim()
            .lowercase(Locale.US)
            .replace(Regex("""[\s_\-/().,:]+"""), "")
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
