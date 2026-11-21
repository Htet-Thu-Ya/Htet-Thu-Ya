package com.example.data.network

import android.util.Log
import com.example.data.betting.KyayFormatter
import com.example.data.model.GitHubReleaseInfo
import com.example.data.model.Live2DStatus
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

    suspend fun fetchFixturesForDays(
        dayOffsets: List<Int>,
        allowedLeagues: List<String> = emptyList()
    ): Result<List<LiveFootballFixture>> = withContext(Dispatchers.IO) {
        try {
            val allowedSet = allowedLeagues.map { it.trim().lowercase(Locale.US) }.filter { it.isNotBlank() }.toSet()
            val targetLeagues = if (allowedSet.isEmpty()) {
                espnLeagues
            } else {
                espnLeagues.filter { (_, name) -> name.lowercase(Locale.US) in allowedSet }
                    .ifEmpty { espnLeagues }
            }
            val allFixtures = linkedMapOf<String, LiveFootballFixture>()
            for (offset in dayOffsets) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                val espnDate = SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)
                val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

                val espnResults = coroutineScope {
                    targetLeagues.map { (slug, leagueName) ->
                        async { fetchEspnLeague(slug, leagueName, espnDate, isoDate) }
                    }.awaitAll().flatten()
                }
                espnResults.forEach { fix ->
                    if (allowedSet.isEmpty() || fix.league.lowercase(Locale.US) in allowedSet) {
                        allFixtures[fix.externalId] = fix
                    }
                }

                if (espnResults.isEmpty()) {
                    val tsdbResults = fetchTheSportsDbDay(isoDate)
                    tsdbResults.forEach { fix ->
                        if (allowedSet.isEmpty() || fix.league.lowercase(Locale.US) in allowedSet) {
                            allFixtures.putIfAbsent(fix.externalId, fix)
                        }
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
            var homeLogoUrl = ""
            var awayLogoUrl = ""
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
                val logo = teamObj?.optString("logo").orEmpty().trim()
                val score = c.optString("score").toLongOrNull() ?: 0L
                if (ha == "home") {
                    homeName = name
                    homeLogoUrl = logo
                    homeScore = score
                } else if (ha == "away") {
                    awayName = name
                    awayLogoUrl = logo
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

            var homeAbbr = ""
            var awayAbbr = ""
            for (j in 0 until competitors.length()) {
                val c = competitors.optJSONObject(j) ?: continue
                val ha = c.optString("homeAway")
                val abbr = c.optJSONObject("team")?.optString("abbreviation").orEmpty()
                if (ha == "home") homeAbbr = abbr else if (ha == "away") awayAbbr = abbr
            }

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
                val spreadVal = firstOdds?.optDouble("spread", Double.NaN) ?: Double.NaN
                handicapLine = KyayFormatter.convertEspnOddsToHandicap(
                    details = details,
                    spreadVal = spreadVal,
                    homeTeam = homeName,
                    awayTeam = awayName,
                    homeAbbr = homeAbbr,
                    awayAbbr = awayAbbr
                )
            }

            list.add(
                LiveFootballFixture(
                    externalId = "espn_$eventId",
                    league = leagueName.take(60),
                    homeTeam = homeName.take(60),
                    awayTeam = awayName.take(60),
                    homeLogoUrl = homeLogoUrl.take(300),
                    awayLogoUrl = awayLogoUrl.take(300),
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
            val homeBadge = ev.optString("strHomeTeamBadge").trim()
            val awayBadge = ev.optString("strAwayTeamBadge").trim()
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
                    homeLogoUrl = homeBadge.take(300),
                    awayLogoUrl = awayBadge.take(300),
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

    suspend fun fetchLive2DResult(): Live2DStatus? = withContext(Dispatchers.IO) {
        try {
            val body = httpGet("https://api.thaistock2d.com/live") ?: return@withContext null
            val root = JSONObject(body)
            val live = root.optJSONObject("live") ?: return@withContext null
            val twod = live.optString("twod").ifBlank { "--" }
            val setStr = live.optString("set").ifBlank { "--" }
            val valStr = live.optString("value").ifBlank { "--" }
            val timeStr = live.optString("time").ifBlank { "" }

            var m1201 = "--"
            var e1630 = "--"
            val resultArr = root.optJSONArray("result")
            if (resultArr != null) {
                for (i in 0 until resultArr.length()) {
                    val item = resultArr.optJSONObject(i) ?: continue
                    val openTime = item.optString("open_time")
                    val r2d = item.optString("twod")
                    if (openTime.contains("12:01") && r2d.isNotBlank()) m1201 = r2d
                    if (openTime.contains("16:30") && r2d.isNotBlank()) e1630 = r2d
                }
            }

            Live2DStatus(
                twod = twod,
                setText = setStr,
                valueText = valStr,
                liveTime = timeStr,
                morning1201 = m1201,
                evening430 = e1630
            )
        } catch (e: Exception) {
            Log.d(TAG, "2D live fetch skipped: ${e.message}")
            null
        }
    }

    suspend fun fetchPublicIpAddress(): String? = withContext(Dispatchers.IO) {
        try {
            val body = httpGet("https://api.ipify.org?format=json") ?: return@withContext null
            JSONObject(body).optString("ip").trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.d(TAG, "Public IP fetch failed: ${e.message}")
            null
        }
    }

    suspend fun fetchLatestGitHubApkRelease(): GitHubReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/Htet-Thu-Ya/Htet-Thu-Ya/releases/tags/latest"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "LoTaYa-Android/4.0")
                .build()
            val body = client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            val root = JSONObject(body)
            val tagName = root.optString("tag_name", "latest")
            val notes = root.optString("body", "").take(350)
            val assets = root.optJSONArray("assets")
            var apkDownloadUrl = "https://github.com/Htet-Thu-Ya/Htet-Thu-Ya/releases/download/latest/LoTaYa.apk"
            var updatedIso = root.optString("published_at", "")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", apkDownloadUrl)
                        updatedIso = asset.optString("updated_at").ifBlank { updatedIso }
                        break
                    }
                }
            }
            val updatedMs = parseIsoTimestampMs(updatedIso) ?: return@withContext null
            GitHubReleaseInfo(
                tagName = tagName,
                updatedAtEpochMs = updatedMs,
                downloadUrl = apkDownloadUrl,
                releaseNotes = notes
            )
        } catch (e: Exception) {
            Log.d(TAG, "GitHub release check skipped: ${e.message}")
            null
        }
    }

    private fun parseIsoTimestampMs(iso: String): Long? {
        if (iso.isBlank()) return null
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm'Z'")
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return fmt.parse(iso)?.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "LoTaYa-Android/4.0")
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

        val DEFAULT_LEAGUE_OPTIONS = listOf(
            "Premier League",
            "La Liga",
            "Bundesliga",
            "Serie A",
            "Ligue 1",
            "Champions League",
            "Europa League",
            "FA Cup",
            "Carabao Cup",
            "World Cup Qualifiers"
        )

        fun normalizeTeam(name: String): String {
            return name.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]"), "")
                .removeSuffix("fc")
                .removePrefix("fc")
        }
    }
}
