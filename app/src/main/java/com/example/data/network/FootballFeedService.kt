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
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class OneXBetSyncResult(
    val fixtures: List<LiveFootballFixture>,
    val usedDirect1xBet: Boolean,
    val sourceOrigin: String
)

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

    suspend fun fetchOneXBetFixtures(
        liveUrl: String = DEFAULT_ONE_X_BET_LIVE_URL,
        allowedLeagues: List<String> = emptyList(),
        dayOffsets: List<Int> = listOf(0, 1)
    ): Result<OneXBetSyncResult> = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeOneXBetLiveUrl(liveUrl)
        val origin = extractOrigin(normalizedUrl)
        val lng = extractLanguageCode(normalizedUrl)
        try {
            val (lineFixtures, liveFixtures) = coroutineScope {
                val lineDeferred = async {
                    fetchOneXBetEndpoint(
                        urls = listOf(
                            "$origin/service-api/LineFeed/Get1x2_VZip?sports=1&count=80&lng=$lng&tf=2200000&tz=6&mode=4&country=190&partner=1&getEmpty=true",
                            "$origin/LineFeed/Get1x2_VZip?sports=1&count=80&lng=$lng&tf=2200000&tz=6&mode=4&country=190&partner=1&getEmpty=true"
                        ),
                        refererUrl = normalizedUrl,
                        origin = origin,
                        isLiveFeed = false
                    )
                }
                val liveDeferred = async {
                    fetchOneXBetEndpoint(
                        urls = listOf(
                            "$origin/service-api/LiveFeed/Get1x2_VZip?sports=1&count=50&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true",
                            "$origin/LiveFeed/Get1x2_VZip?sports=1&count=50&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true"
                        ),
                        refererUrl = normalizedUrl,
                        origin = origin,
                        isLiveFeed = true
                    )
                }
                lineDeferred.await() to liveDeferred.await()
            }

            val merged = linkedMapOf<String, LiveFootballFixture>()
            lineFixtures.forEach { merged[it.externalId] = it }
            liveFixtures.forEach { merged[it.externalId] = it }

            if (merged.isNotEmpty()) {
                val allowedSet = allowedLeagues
                    .map { it.trim().lowercase(Locale.US) }
                    .filter { it.isNotBlank() }
                    .toSet()
                val rawList = merged.values.toList()
                val filtered = if (allowedSet.isEmpty()) {
                    rawList
                } else {
                    rawList.filter { fix ->
                        val lLow = fix.league.lowercase(Locale.US)
                        allowedSet.any { allowed -> lLow.contains(allowed) || allowed.contains(lLow) }
                    }.ifEmpty { rawList }
                }
                return@withContext Result.success(
                    OneXBetSyncResult(
                        fixtures = filtered,
                        usedDirect1xBet = true,
                        sourceOrigin = origin
                    )
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direct 1xBET feed skipped ($origin): ${e.message}")
        }

        // Graceful fallback to live ESPN/SportsDB odds feed if mirror is geo-blocked or unreachable
        val fallback = fetchFixturesForDays(dayOffsets = dayOffsets, allowedLeagues = allowedLeagues)
        fallback.map { fixtures ->
            OneXBetSyncResult(
                fixtures = fixtures,
                usedDirect1xBet = false,
                sourceOrigin = origin
            )
        }
    }

    suspend fun fetchOneXBetLiveUpdates(
        liveUrl: String = DEFAULT_ONE_X_BET_LIVE_URL
    ): List<LiveFootballFixture> = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeOneXBetLiveUrl(liveUrl)
        val origin = extractOrigin(normalizedUrl)
        val lng = extractLanguageCode(normalizedUrl)
        try {
            fetchOneXBetEndpoint(
                urls = listOf(
                    "$origin/service-api/LiveFeed/Get1x2_VZip?sports=1&count=50&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true",
                    "$origin/LiveFeed/Get1x2_VZip?sports=1&count=50&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true"
                ),
                refererUrl = normalizedUrl,
                origin = origin,
                isLiveFeed = true
            )
        } catch (e: Exception) {
            Log.d(TAG, "1xBET live score poll skipped: ${e.message}")
            emptyList()
        }
    }

    private fun fetchOneXBetEndpoint(
        urls: List<String>,
        refererUrl: String,
        origin: String,
        isLiveFeed: Boolean
    ): List<LiveFootballFixture> {
        for (url in urls) {
            val body = httpGetBrowserJson(url = url, refererUrl = refererUrl, origin = origin)
            if (!body.isNullOrBlank()) {
                val parsed = parseOneXBetFeedJson(
                    jsonStr = body,
                    origin = origin,
                    isLiveFeed = isLiveFeed
                )
                if (parsed.isNotEmpty()) {
                    return parsed
                }
            }
        }
        return emptyList()
    }

    internal fun parseOneXBetFeedJson(
        jsonStr: String,
        origin: String,
        isLiveFeed: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): List<LiveFootballFixture> {
        val trimmed = jsonStr.trim()
        if (!trimmed.startsWith("{")) return emptyList()
        return try {
            val root = JSONObject(trimmed)
            val valueArr = root.optJSONArray("Value") ?: return emptyList()
            val list = mutableListOf<LiveFootballFixture>()
            for (i in 0 until valueArr.length().coerceAtMost(80)) {
                val obj = valueArr.optJSONObject(i) ?: continue
                val gameId = obj.optLong("I", 0L).takeIf { it > 0L }?.toString()
                    ?: obj.optString("CI").trim()
                if (gameId.isBlank()) continue

                val league = obj.optString("LE")
                    .ifBlank { obj.optString("L") }
                    .ifBlank { "1xBET Football" }
                    .trim()
                val homeTeam = obj.optString("O1E")
                    .ifBlank { obj.optString("O1") }
                    .trim()
                val awayTeam = obj.optString("O2E")
                    .ifBlank { obj.optString("O2") }
                    .trim()
                if (homeTeam.isBlank() || awayTeam.isBlank()) continue

                val o1Img = obj.optJSONArray("O1IMG")?.optString(0)?.trim().orEmpty()
                val o2Img = obj.optJSONArray("O2IMG")?.optString(0)?.trim().orEmpty()
                val startSec = obj.optLong("S", 0L)

                val scObj = obj.optJSONObject("SC")
                val fsObj = scObj?.optJSONObject("FS")
                val homeScore = fsObj?.optLong("S1", 0L) ?: 0L
                val awayScore = fsObj?.optLong("S2", 0L) ?: 0L
                val cps = scObj?.optString("CPS").orEmpty().trim()
                val isFinished = obj.optBoolean("F", false) ||
                    cps.equals("Match finished", ignoreCase = true) ||
                    cps.equals("FT", ignoreCase = true)

                val outcomes = mutableListOf<KyayFormatter.OneXBetMarketOutcome>()
                val eArr = obj.optJSONArray("E")
                if (eArr != null) {
                    for (j in 0 until eArr.length()) {
                        val eObj = eArr.optJSONObject(j) ?: continue
                        val t = eObj.optInt("T", -1)
                        val c = eObj.optDouble("C", Double.NaN)
                        val p = if (eObj.has("P") && !eObj.isNull("P")) {
                            eObj.optDouble("P", Double.NaN).takeUnless { it.isNaN() }
                        } else null
                        if (t > 0 && !c.isNaN() && c > 1.0) {
                            outcomes.add(KyayFormatter.OneXBetMarketOutcome(type = t, param = p, coef = c))
                        }
                    }
                }

                buildOneXBetFixture(
                    gameId = gameId,
                    league = league,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    homeImgFile = o1Img,
                    awayImgFile = o2Img,
                    startEpochSec = startSec,
                    homeScore = homeScore,
                    awayScore = awayScore,
                    livePeriodClock = cps,
                    isFinished = isFinished,
                    isLiveFeed = isLiveFeed,
                    outcomes = outcomes,
                    origin = origin,
                    nowMs = nowMs
                )?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.d(TAG, "1xBET JSON parse error: ${e.message}")
            emptyList()
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
            var derivedBodyOdds = 1.93
            var derivedGoalOdds = 1.90
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
                val homeOddsObj = firstOdds?.optJSONObject("homeTeamOdds")
                val awayOddsObj = firstOdds?.optJSONObject("awayTeamOdds")
                val (bOdds, gOdds) = KyayFormatter.deriveRealOddsFromEspn(
                    homeMoneyLine = homeOddsObj?.optDouble("moneyLine", Double.NaN) ?: Double.NaN,
                    awayMoneyLine = awayOddsObj?.optDouble("moneyLine", Double.NaN) ?: Double.NaN,
                    homeSpreadOdds = homeOddsObj?.optDouble("spreadOdds", Double.NaN) ?: Double.NaN,
                    awaySpreadOdds = awayOddsObj?.optDouble("spreadOdds", Double.NaN) ?: Double.NaN,
                    overOdds = firstOdds?.optDouble("overOdds", Double.NaN) ?: Double.NaN,
                    underOdds = firstOdds?.optDouble("underOdds", Double.NaN) ?: Double.NaN
                )
                derivedBodyOdds = bOdds
                derivedGoalOdds = gOdds
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
                    bodyOdds = derivedBodyOdds,
                    goalOdds = derivedGoalOdds,
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

    private fun httpGetBrowserJson(url: String, refererUrl: String, origin: String): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", refererUrl)
                .header("Origin", origin)
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            Log.d(TAG, "1xBET GET $url failed: ${e.message}")
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
        const val DEFAULT_ONE_X_BET_LIVE_URL = "https://1xlite-19808.pro/en/live"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

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

        fun normalizeOneXBetLiveUrl(raw: String): String {
            val cleaned = raw.trim()
            if (cleaned.isBlank()) return DEFAULT_ONE_X_BET_LIVE_URL
            val withHttps = when {
                cleaned.startsWith("https://", ignoreCase = true) -> cleaned
                cleaned.startsWith("http://", ignoreCase = true) -> "https://" + cleaned.removePrefix("http://").removePrefix("HTTP://")
                else -> "https://$cleaned"
            }
            return try {
                val uri = URI(withHttps)
                if (uri.host.isNullOrBlank()) DEFAULT_ONE_X_BET_LIVE_URL else withHttps.take(300)
            } catch (_: Exception) {
                DEFAULT_ONE_X_BET_LIVE_URL
            }
        }

        fun extractOrigin(liveUrl: String): String {
            val normalized = normalizeOneXBetLiveUrl(liveUrl)
            return try {
                val uri = URI(normalized)
                val host = uri.host?.takeIf { it.isNotBlank() } ?: return "https://1xlite-19808.pro"
                val portPart = if (uri.port > 0 && uri.port != 443) ":${uri.port}" else ""
                "https://$host$portPart"
            } catch (_: Exception) {
                "https://1xlite-19808.pro"
            }
        }

        fun extractLanguageCode(liveUrl: String): String {
            return try {
                val seg = URI(normalizeOneXBetLiveUrl(liveUrl)).path
                    ?.split('/')
                    ?.firstOrNull { it.length == 2 && it.all { ch -> ch.isLetter() } }
                    ?.lowercase(Locale.US)
                seg ?: "en"
            } catch (_: Exception) {
                "en"
            }
        }

        internal fun buildOneXBetFixture(
            gameId: String,
            league: String,
            homeTeam: String,
            awayTeam: String,
            homeImgFile: String,
            awayImgFile: String,
            startEpochSec: Long,
            homeScore: Long,
            awayScore: Long,
            livePeriodClock: String,
            isFinished: Boolean,
            isLiveFeed: Boolean,
            outcomes: List<KyayFormatter.OneXBetMarketOutcome>,
            origin: String,
            nowMs: Long = System.currentTimeMillis()
        ): LiveFootballFixture? {
            if (gameId.isBlank() || homeTeam.isBlank() || awayTeam.isBlank()) return null

            val startMs = if (startEpochSec > 0L) startEpochSec * 1000L else 0L
            val status = when {
                isFinished -> "finished"
                isLiveFeed || (startMs in 1 until nowMs) -> "closed"
                else -> "open"
            }
            val liveClock = when (status) {
                "finished" -> "FT"
                "closed" -> if (livePeriodClock.isNotBlank()) "LIVE $livePeriodClock" else "LIVE"
                else -> ""
            }

            val kickoffLabel = if (status == "open" && startMs > 0L) {
                val outFmt = SimpleDateFormat("dd MMM hh:mm a", Locale.US).apply {
                    timeZone = TimeZone.getDefault()
                }
                outFmt.format(Date(startMs))
            } else {
                liveClock.ifBlank { "1xBET Live" }
            }

            val matchDate = if (startMs > 0L) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startMs))
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
            }

            val parsedLines = KyayFormatter.parseOneXBetOutcomesToLines(outcomes)
            val homeLogoUrl = homeImgFile.trim().takeIf { it.isNotBlank() }?.let { file ->
                if (file.startsWith("http")) file else "$origin/sfiles/logo_teams/$file"
            }.orEmpty()
            val awayLogoUrl = awayImgFile.trim().takeIf { it.isNotBlank() }?.let { file ->
                if (file.startsWith("http")) file else "$origin/sfiles/logo_teams/$file"
            }.orEmpty()

            return LiveFootballFixture(
                externalId = "1xbet_$gameId",
                league = league.take(60),
                homeTeam = homeTeam.take(60),
                awayTeam = awayTeam.take(60),
                homeLogoUrl = homeLogoUrl.take(300),
                awayLogoUrl = awayLogoUrl.take(300),
                kickoffLabel = kickoffLabel.take(40),
                matchDate = matchDate.take(20),
                handicapLine = parsedLines.handicapLine.take(40),
                goalLine = parsedLines.goalLine.take(20),
                bodyOdds = parsedLines.bodyOdds,
                goalOdds = parsedLines.goalOdds,
                status = status,
                liveClock = liveClock.take(24),
                homeScore = homeScore.coerceIn(0L, 99L),
                awayScore = awayScore.coerceIn(0L, 99L)
            )
        }

        fun normalizeTeam(name: String): String {
            return name.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]"), "")
                .removeSuffix("fc")
                .removePrefix("fc")
        }
    }
}
