package com.example.data.network

import android.util.Log
import com.example.data.model.LiveFootballFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class FootballFeedService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val espnLeagues = listOf(
        "eng.1" to "Premier League",
        "esp.1" to "La Liga",
        "ger.1" to "Bundesliga",
        "ita.1" to "Serie A",
        "fra.1" to "Ligue 1",
        "uefa.champions" to "Champions League",
        "uefa.europa" to "Europa League",
        "eng.fa" to "FA Cup",
        "eng.league_cup" to "Carabao Cup",
        "fifa.world" to "World Cup Qualifiers"
    )

    fun todayDateIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date())
    }

    suspend fun fetchFixturesForDays(dayOffsets: List<Int>): Result<List<LiveFootballFixture>> = withContext(Dispatchers.IO) {
        try {
            val allFixtures = linkedMapOf<String, LiveFootballFixture>()
            for (offset in dayOffsets) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                val espnDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)
                val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

                val espnResults = coroutineScope {
                    espnLeagues.map { (slug, leagueName) ->
                        async { fetchEspnLeague(slug, leagueName, espnDate, isoDate) }
                    }.awaitAll().flatten()
                }
                espnResults.forEach { fix ->
                    allFixtures[fix.externalId] = fix
                }

                // Fallback or supplement from TheSportsDB if ESPN had few matches on that date
                if (espnResults.isEmpty()) {
                    val tsdbResults = fetchTheSportsDbDay(isoDate)
                    tsdbResults.forEach { fix ->
                        allFixtures.putIfAbsent(fix.externalId, fix)
                    }
                }
            }
            Result.success(allFixtures.values.toList())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch live football feed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fetchEspnLeague(
        slug: String,
        leagueName: String,
        espnDate: String,
        isoDate: String
    ): List<LiveFootballFixture> {
        val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/$slug/scoreboard?dates=$espnDate&limit=30"
        val body = httpGet(url) ?: return emptyList()
        val root = JSONObject(body)
        val events = root.optJSONArray("events") ?: return emptyList()
        val list = mutableListOf<LiveFootballFixture>()

        for (i in 0 until events.length()) {
            val ev = events.optJSONObject(i) ?: continue
            val eventId = ev.optString("id").trim()
            if (eventId.isBlank()) continue
            val comps = ev.optJSONArray("competitions") ?: continue
            val comp = comps.optJSONObject(0) ?: continue
            val competitors = comp.optJSONArray("competitors") ?: continue

            var homeName = ""
            var awayName = ""
            var homeScore = 0L
            var awayScore = 0L
            for (j in 0 until competitors.length()) {
                val c = competitors.optJSONObject(j) ?: continue
                val ha = c.optString("homeAway")
                val teamObj = c.optJSONObject("team")
                val name = teamObj?.optString("shortDisplayName")
                    ?.ifBlank { teamObj.optString("displayName") }
                    ?.ifBlank { teamObj.optString("name") }
                    .orEmpty()
                val score = c.optString("score").toLongOrNull() ?: 0L
                if (ha == "home") {
                    homeName = name
                    homeScore = score
                } else if (ha == "away") {
                    awayName = name
                    awayScore = score
                }
            }
            if (homeName.isBlank() || awayName.isBlank()) continue

            val statusObj = comp.optJSONObject("status") ?: ev.optJSONObject("status")
            val typeObj = statusObj?.optJSONObject("type")
            val state = typeObj?.optString("state")?.lowercase(Locale.US).orEmpty()
            val statusName = typeObj?.optString("name").orEmpty()
            val completed = typeObj?.optBoolean("completed") == true
            val detail = typeObj?.optString("shortDetail")
                ?.ifBlank { statusObj.optString("displayClock") }
                .orEmpty()

            if (statusName.contains("POSTPONED", ignoreCase = true) ||
                statusName.contains("CANCELED", ignoreCase = true)
            ) {
                continue
            }

            val mappedStatus = when {
                completed || state == "post" || statusName == "STATUS_FINAL" || statusName == "STATUS_FULL_TIME" -> "finished"
                state == "in" || statusName == "STATUS_IN_PROGRESS" || statusName == "STATUS_HALFTIME" -> "closed"
                else -> "open"
            }

            val liveClock = when (mappedStatus) {
                "finished" -> "FT"
                "closed" -> if (detail.isNotBlank()) "LIVE $detail" else "LIVE"
                else -> ""
            }

            val isoUtc = ev.optString("date")
            val kickoffLabel = if (mappedStatus == "open") {
                formatUtcToLocalLabel(isoUtc)
            } else {
                liveClock.ifBlank { formatUtcToLocalLabel(isoUtc) }
            }

            // Extract optional odds if ESPN includes them
            var goalLine = "2.5"
            var handicapLine = "Home -0.5"
            val oddsArr = comp.optJSONArray("odds")
            if (oddsArr != null && oddsArr.length() > 0) {
                val firstOdds = oddsArr.optJSONObject(0)
                val overUnder = firstOdds?.optDouble("overUnder", Double.NaN) ?: Double.NaN
                if (!overUnder.isNaN() && overUnder > 0.0) {
                    goalLine = overUnder.toString().removeSuffix(".0")
                }
                val details = firstOdds?.optString("details").orEmpty()
                if (details.isNotBlank() && details.length <= 35) {
                    handicapLine = details
                }
            }

            list.add(
                LiveFootballFixture(
                    externalId = "espn_$eventId",
                    league = leagueName.take(60),
                    homeTeam = homeName.take(60),
                    awayTeam = awayName.take(60),
                    kickoffLabel = kickoffLabel.take(40),
                    matchDate = isoDate,
                    handicapLine = handicapLine.take(40),
                    goalLine = goalLine.take(20),
                    bodyOdds = 1.92,
                    goalOdds = 1.90,
                    status = mappedStatus,
                    liveClock = liveClock.take(24),
                    homeScore = homeScore.coerceIn(0L, 99L),
                    awayScore = awayScore.coerceIn(0L, 99L)
                )
            )
        }
        return list
    }

    private fun fetchTheSportsDbDay(isoDate: String): List<LiveFootballFixture> {
        val url = "https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=$isoDate&s=Soccer"
        val body = httpGet(url) ?: return emptyList()
        val root = JSONObject(body)
        val events = root.optJSONArray("events") ?: return emptyList()
        val list = mutableListOf<LiveFootballFixture>()

        for (i in 0 until events.length().coerceAtMost(35)) {
            val ev = events.optJSONObject(i) ?: continue
            val eventId = ev.optString("idEvent").trim()
            val home = ev.optString("strHomeTeam").trim()
            val away = ev.optString("strAwayTeam").trim()
            val league = ev.optString("strLeague").trim().ifBlank { "Football" }
            if (eventId.isBlank() || home.isBlank() || away.isBlank()) continue
            if (ev.optString("strPostponed").equals("yes", ignoreCase = true)) continue

            val rawStatus = ev.optString("strStatus").trim()
            val homeScore = ev.optString("intHomeScore").toLongOrNull() ?: 0L
            val awayScore = ev.optString("intAwayScore").toLongOrNull() ?: 0L

            val mappedStatus = when {
                rawStatus.contains("Finished", ignoreCase = true) || rawStatus.equals("FT", ignoreCase = true) -> "finished"
                rawStatus.contains("Half", ignoreCase = true) || rawStatus.contains("Live", ignoreCase = true) ||
                    rawStatus.endsWith("'") -> "closed"
                else -> "open"
            }

            val dateStr = ev.optString("dateEvent").ifBlank { isoDate }
            val timeStr = ev.optString("strTime").take(5)
            val kickoff = if (mappedStatus == "finished") "FT"
            else if (mappedStatus == "closed") "LIVE ${rawStatus.take(12)}".trim()
            else formatDateAndUtcTime(dateStr, timeStr)

            list.add(
                LiveFootballFixture(
                    externalId = "tsdb_$eventId",
                    league = league.take(60),
                    homeTeam = home.take(60),
                    awayTeam = away.take(60),
                    kickoffLabel = kickoff.take(40),
                    matchDate = dateStr.take(20),
                    handicapLine = "Home -0.5",
                    goalLine = "2.5",
                    bodyOdds = 1.92,
                    goalOdds = 1.90,
                    status = mappedStatus,
                    liveClock = if (mappedStatus == "finished") "FT" else if (mappedStatus == "closed") rawStatus.take(20) else "",
                    homeScore = homeScore.coerceIn(0L, 99L),
                    awayScore = awayScore.coerceIn(0L, 99L)
                )
            )
        }
        return list
    }

    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "HtetThuYa-Android/1.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            Log.d(TAG, "GET $url failed: ${e.message}")
            null
        }
    }

    private fun formatUtcToLocalLabel(isoUtc: String): String {
        if (isoUtc.isBlank()) return "Today"
        val patterns = listOf("yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = parser.parse(isoUtc) ?: continue
                val outFmt = SimpleDateFormat("dd MMM hh:mm a", Locale.US).apply {
                    timeZone = TimeZone.getDefault()
                }
                return outFmt.format(parsed)
            } catch (_: Exception) {
            }
        }
        return isoUtc.take(16)
    }

    private fun formatDateAndUtcTime(dateStr: String, timeStr: String): String {
        if (timeStr.isBlank()) return dateStr
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = parser.parse("$dateStr $timeStr") ?: return "$dateStr $timeStr"
            val outFmt = SimpleDateFormat("dd MMM hh:mm a", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            outFmt.format(parsed)
        } catch (_: Exception) {
            "$dateStr $timeStr"
        }
    }

    companion object {
        private const val TAG = "FootballFeed"

        fun normalizeTeam(name: String): String {
            return name.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]"), "")
                .removeSuffix("fc")
                .removePrefix("fc")
        }
    }
}
