package com.example.data.network

import android.util.Log
import com.example.data.betting.KyayFormatter
import com.example.data.model.GitHubReleaseInfo
import com.example.data.model.Live2DStatus
import com.example.data.model.LiveFootballFixture
import com.example.data.model.Lottery2DDayRecord
import com.example.data.util.MyanmarTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
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
        "uefa.champions" to "UEFA Champions League",
        "uefa.europa" to "Europa League",
        "uefa.europa.conf" to "UEFA Conference League",
        "uefa.youth" to "UEFA Youth League",
        "eng.1" to "Premier League",
        "eng.2" to "Championship",
        "eng.fa" to "FA Cup",
        "eng.league_cup" to "Carabao Cup",
        "esp.1" to "La Liga",
        "esp.2" to "La Liga 2",
        "esp.copa_del_rey" to "Copa del Rey",
        "ger.1" to "Bundesliga",
        "ger.2" to "2. Bundesliga",
        "ita.1" to "Serie A",
        "ita.2" to "Serie B",
        "ita.coppa_italia" to "Coppa Italia",
        "fra.1" to "Ligue 1",
        "fra.2" to "Ligue 2",
        "afc.champions" to "AFC Champions League Elite",
        "afc.cup" to "AFC Champions League 2",
        "tha.1" to "Thai League 1",
        "jpn.1" to "J1 League (Japan)",
        "kor.1" to "K League 1 (Korea)",
        "sau.1" to "Saudi Pro League",
        "chn.1" to "Chinese Super League",
        "aus.1" to "A-League",
        "rus.1" to "Russian Premier League",
        "rus.cup" to "Russian Cup",
        "egy.1" to "Egypt. Premier League",
        "isr.1" to "Israel",
        "bra.1" to "Brasileirao Serie A",
        "fifa.world" to "World Cup Qualifiers"
    )

    fun todayDateIso(): String = MyanmarTimeUtil.todayIsoDate()

    private fun statusRank(status: String): Int = when (status) {
        "finished" -> 3
        "closed" -> 2
        else -> 1
    }

    private fun mergeFixture(
        map: MutableMap<String, LiveFootballFixture>,
        fix: LiveFootballFixture
    ) {
        val existing = map[fix.externalId]
        if (existing == null || statusRank(fix.status) >= statusRank(existing.status)) {
            map[fix.externalId] = fix
        }
    }

    suspend fun fetchFixturesForDays(
        dayOffsets: List<Int>,
        allowedLeagues: List<String> = emptyList()
    ): Result<List<LiveFootballFixture>> = withContext(Dispatchers.IO) {
        try {
            val nowMs = MyanmarTimeUtil.nowMs()
            val allowedSet = allowedLeagues.map { it.trim().lowercase(Locale.US) }.filter { it.isNotBlank() }.toSet()
            val targetLeagues = if (allowedSet.isEmpty()) {
                espnLeagues
            } else {
                espnLeagues.filter { (_, name) ->
                    val low = name.lowercase(Locale.US)
                    allowedSet.any { a -> low == a || low.contains(a) || a.contains(low) }
                }.ifEmpty { espnLeagues }
            }
            val allFixtures = linkedMapOf<String, LiveFootballFixture>()

            // Include previous UTC day when fetching today (offset 0) so early Yangon morning kickoffs (00:00-06:29 MMT) are included
            val espnDateSet = linkedSetOf<String>()
            val tsdbDateSet = linkedSetOf<String>()
            dayOffsets.forEach { offset ->
                espnDateSet.add(MyanmarTimeUtil.espnDateForOffset(offset, nowMs))
                tsdbDateSet.add(MyanmarTimeUtil.isoDateForOffset(offset, nowMs))
                tsdbDateSet.add(MyanmarTimeUtil.utcIsoDateForOffset(offset, nowMs))
                if (offset == 0) {
                    espnDateSet.add(
                        MyanmarTimeUtil.utcIsoDateForOffset(-1, nowMs).replace("-", "")
                    )
                    tsdbDateSet.add(MyanmarTimeUtil.utcIsoDateForOffset(-1, nowMs))
                }
            }

            val espnJobs = coroutineScope {
                espnDateSet.flatMap { espnDate ->
                    targetLeagues.map { (slug, leagueName) ->
                        async { fetchEspnLeague(slug, leagueName, espnDate, nowMs) }
                    }
                }.awaitAll().flatten()
            }
            espnJobs.forEach { fix ->
                val lLow = fix.league.lowercase(Locale.US)
                if (allowedSet.isEmpty() || allowedSet.any { lLow.contains(it) || it.contains(lLow) }) {
                    mergeFixture(allFixtures, fix)
                }
            }

            // Always pull TheSportsDB for broader cup/regional/Asian coverage & finished match completion
            val tsdbJobs = coroutineScope {
                tsdbDateSet.map { isoDate ->
                    async { fetchTheSportsDbDay(isoDate, nowMs) }
                }.awaitAll().flatten()
            }
            tsdbJobs.forEach { fix ->
                val lLow = fix.league.lowercase(Locale.US)
                if (allowedSet.isEmpty() || allowedSet.any { lLow.contains(it) || it.contains(lLow) }) {
                    mergeFixture(allFixtures, fix)
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
                            "$origin/service-api/LineFeed/Get1x2_VZip?sports=1&count=250&lng=$lng&tf=2200000&tz=6&mode=4&country=190&partner=1&getEmpty=true",
                            "$origin/LineFeed/Get1x2_VZip?sports=1&count=250&lng=$lng&tf=2200000&tz=6&mode=4&country=190&partner=1&getEmpty=true"
                        ),
                        refererUrl = normalizedUrl,
                        origin = origin,
                        isLiveFeed = false
                    )
                }
                val liveDeferred = async {
                    fetchOneXBetEndpoint(
                        urls = listOf(
                            "$origin/service-api/LiveFeed/Get1x2_VZip?sports=1&count=120&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true",
                            "$origin/LiveFeed/Get1x2_VZip?sports=1&count=120&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true"
                        ),
                        refererUrl = normalizedUrl,
                        origin = origin,
                        isLiveFeed = true
                    )
                }
                lineDeferred.await() to liveDeferred.await()
            }

            val merged = linkedMapOf<String, LiveFootballFixture>()
            lineFixtures.forEach { mergeFixture(merged, it) }
            liveFixtures.forEach { mergeFixture(merged, it) }

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
                    "$origin/service-api/LiveFeed/Get1x2_VZip?sports=1&count=120&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true",
                    "$origin/LiveFeed/Get1x2_VZip?sports=1&count=120&lng=$lng&gr=44&mode=4&country=190&partner=1&getEmpty=true"
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
            for (i in 0 until valueArr.length().coerceAtMost(250)) {
                val obj = valueArr.optJSONObject(i) ?: continue
                val gameId = obj.optLong("I", 0L).takeIf { it > 0L }?.toString()
                    ?: obj.optString("CI").trim()
                if (gameId.isBlank()) continue

                val league = obj.optString("LE")
                    .ifBlank { obj.optString("L") }
                    .ifBlank { "Football" }
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
        nowMs: Long
    ): List<LiveFootballFixture> {
        val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/$slug/scoreboard?dates=$espnDate&limit=50"
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
                statusName.contains("CANCELED", ignoreCase = true) ||
                statusName.contains("ABANDONED", ignoreCase = true)
            ) {
                continue
            }

            val isoUtc = ev.optString("date")
            val kickoffEpochMs = MyanmarTimeUtil.parseIsoUtcEpochMs(isoUtc)
            val yangonDate = if (kickoffEpochMs > 0L) {
                MyanmarTimeUtil.formatMatchDateIso(kickoffEpochMs)
            } else {
                MyanmarTimeUtil.todayIsoDate(nowMs)
            }

            val isFinished = completed ||
                state == "post" ||
                statusName == "STATUS_FINAL" ||
                statusName == "STATUS_FULL_TIME" ||
                statusName == "STATUS_FINAL_AET" ||
                statusName == "STATUS_FINAL_PEN"
            val isLiveOrStarted = state == "in" ||
                statusName == "STATUS_IN_PROGRESS" ||
                statusName == "STATUS_HALFTIME" ||
                (!isFinished && kickoffEpochMs in 1L..nowMs)

            val mappedStatus = when {
                isFinished -> "finished"
                isLiveOrStarted -> "closed"
                else -> "open"
            }

            val liveClock = when (mappedStatus) {
                "finished" -> "FT"
                "closed" -> if (detail.isNotBlank()) "LIVE $detail" else "LIVE"
                else -> ""
            }

            val kickoffLabel = if (mappedStatus == "open") {
                if (kickoffEpochMs > 0L) MyanmarTimeUtil.formatKickoffLabel(kickoffEpochMs)
                else formatUtcToLocalLabel(isoUtc)
            } else {
                liveClock.ifBlank {
                    if (kickoffEpochMs > 0L) MyanmarTimeUtil.formatKickoffLabel(kickoffEpochMs) else "LIVE"
                }
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
            var derivedWin1Odds = 1.95
            var derivedDrawOdds = 3.45
            var derivedWin2Odds = 3.70
            var derivedOverOdds = 1.90
            var derivedUnderOdds = 1.90
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
                val drawOddsObj = firstOdds?.optJSONObject("drawOdds")
                val hMl = homeOddsObj?.optDouble("moneyLine", Double.NaN) ?: Double.NaN
                val dMl = drawOddsObj?.optDouble("moneyLine", Double.NaN) ?: Double.NaN
                val aMl = awayOddsObj?.optDouble("moneyLine", Double.NaN) ?: Double.NaN
                val oVal = firstOdds?.optDouble("overOdds", Double.NaN) ?: Double.NaN
                val uVal = firstOdds?.optDouble("underOdds", Double.NaN) ?: Double.NaN
                val (bOdds, gOdds) = KyayFormatter.deriveRealOddsFromEspn(
                    homeMoneyLine = hMl,
                    awayMoneyLine = aMl,
                    homeSpreadOdds = homeOddsObj?.optDouble("spreadOdds", Double.NaN) ?: Double.NaN,
                    awaySpreadOdds = awayOddsObj?.optDouble("spreadOdds", Double.NaN) ?: Double.NaN,
                    overOdds = oVal,
                    underOdds = uVal
                )
                derivedBodyOdds = bOdds
                derivedGoalOdds = gOdds
                val oneX2List = KyayFormatter.deriveOneX2AndGoalFromEspn(
                    handicapLine = handicapLine,
                    homeMoneyLine = hMl,
                    drawMoneyLine = dMl,
                    awayMoneyLine = aMl,
                    overOdds = oVal,
                    underOdds = uVal
                )
                derivedWin1Odds = oneX2List[0]
                derivedDrawOdds = oneX2List[1]
                derivedWin2Odds = oneX2List[2]
                derivedOverOdds = oneX2List[3]
                derivedUnderOdds = oneX2List[4]
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
                    matchDate = yangonDate.take(20),
                    kickoffEpochMs = kickoffEpochMs.coerceAtLeast(0L),
                    handicapLine = handicapLine.take(40),
                    goalLine = goalLine.take(20),
                    bodyOdds = derivedBodyOdds,
                    goalOdds = derivedGoalOdds,
                    win1Odds = derivedWin1Odds,
                    drawOdds = derivedDrawOdds,
                    win2Odds = derivedWin2Odds,
                    overOdds = derivedOverOdds,
                    underOdds = derivedUnderOdds,
                    status = mappedStatus,
                    liveClock = liveClock.take(24),
                    homeScore = homeScore.coerceIn(0L, 99L),
                    awayScore = awayScore.coerceIn(0L, 99L)
                )
            )
        }
        return list
    }

    private fun fetchTheSportsDbDay(isoDate: String, nowMs: Long): List<LiveFootballFixture> {
        val url = "https://www.thesportsdb.com/api/v1/json/3/eventsday.php?d=$isoDate&s=Soccer"
        val body = httpGet(url) ?: return emptyList()
        return parseTheSportsDbEvents(body = body, fallbackIsoDate = isoDate, nowMs = nowMs)
    }

    suspend fun fetchTheSportsDbDates(isoDates: List<String>): List<LiveFootballFixture> = withContext(Dispatchers.IO) {
        val cleanDates = isoDates.map { it.trim() }.filter { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) }.distinct().take(12)
        if (cleanDates.isEmpty()) return@withContext emptyList()
        val nowMs = MyanmarTimeUtil.nowMs()
        val results = coroutineScope {
            cleanDates.map { iso -> async { fetchTheSportsDbDay(iso, nowMs) } }.awaitAll().flatten()
        }
        val merged = linkedMapOf<String, LiveFootballFixture>()
        results.forEach { mergeFixture(merged, it) }
        merged.values.toList()
    }

    suspend fun searchTheSportsDbMatch(
        homeTeam: String,
        awayTeam: String,
        nowMs: Long = MyanmarTimeUtil.nowMs()
    ): List<LiveFootballFixture> = withContext(Dispatchers.IO) {
        val cleanHome = homeTeam.trim()
        val cleanAway = awayTeam.trim()
        if (cleanHome.isBlank() || cleanAway.isBlank()) return@withContext emptyList()
        val merged = linkedMapOf<String, LiveFootballFixture>()
        val queries = listOf(
            "${cleanHome}_vs_${cleanAway}",
            "$cleanHome vs $cleanAway"
        ).distinct()
        for (q in queries) {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val url = "https://www.thesportsdb.com/api/v1/json/3/searchevents.php?e=$encoded"
            val body = httpGet(url) ?: continue
            parseTheSportsDbEvents(
                body = body,
                fallbackIsoDate = MyanmarTimeUtil.todayIsoDate(nowMs),
                nowMs = nowMs
            ).filter { fix ->
                fuzzyTeamMatches(fix.homeTeam, cleanHome) && fuzzyTeamMatches(fix.awayTeam, cleanAway)
            }.forEach { mergeFixture(merged, it) }
            if (merged.values.any { it.status == "finished" }) break
        }
        merged.values.toList()
    }

    suspend fun fetchGoogleSheetCsvMatches(csvUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val exportUrl = normalizeGoogleSheetCsvUrl(csvUrl)
            require(exportUrl.isNotBlank()) { "Google Sheet / CSV Link ထည့်သွင်းပါ" }
            val req = Request.Builder()
                .url(exportUrl)
                .header("User-Agent", "LoTaYa-Android/5.0")
                .header("Accept", "text/csv, text/plain, */*")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Google Sheet ဖတ်မရပါ (HTTP ${resp.code}) — 'Anyone with the link can view' ဖွင့်ထားပါ"))
                }
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("Google Sheet ထဲတွင် ဒေတာ မရှိသေးပါ"))
                }
                if (text.trimStart().startsWith("<!DOCTYPE html", ignoreCase = true) ||
                    text.trimStart().startsWith("<html", ignoreCase = true)
                ) {
                    return@withContext Result.failure(IllegalStateException("Google Sheet ကို Public / Published to web CSV အဖြစ် ဖွင့်ပေးရန် လိုအပ်ပါသည်"))
                }
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTheSportsDbEvents(
        body: String,
        fallbackIsoDate: String,
        nowMs: Long
    ): List<LiveFootballFixture> {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val events = root.optJSONArray("events") ?: return emptyList()
        val list = mutableListOf<LiveFootballFixture>()

        for (i in 0 until events.length()) {
            val ev = events.optJSONObject(i) ?: continue
            val sport = ev.optString("strSport").trim()
            if (sport.isNotBlank() && !sport.equals("Soccer", ignoreCase = true) && !sport.equals("Football", ignoreCase = true)) {
                continue
            }
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
            val hasExplicitScores = !ev.isNull("intHomeScore") &&
                !ev.isNull("intAwayScore") &&
                ev.optString("intHomeScore").trim().isNotBlank() &&
                ev.optString("intAwayScore").trim().isNotBlank()

            val dateStr = ev.optString("dateEvent").ifBlank { fallbackIsoDate }
            val timeStr = ev.optString("strTime").trim().take(8)
            val timestampStr = ev.optString("strTimestamp").trim()
            val kickoffEpochMs = MyanmarTimeUtil.parseIsoUtcEpochMs(timestampStr)
                .takeIf { it > 0L }
                ?: MyanmarTimeUtil.parseDateAndUtcTimeEpochMs(dateStr, timeStr)

            val isFinished = rawStatus.contains("Finished", ignoreCase = true) ||
                rawStatus.equals("FT", ignoreCase = true) ||
                rawStatus.equals("AET", ignoreCase = true) ||
                rawStatus.equals("AOT", ignoreCase = true) ||
                rawStatus.equals("AP", ignoreCase = true) ||
                rawStatus.equals("Match Finished", ignoreCase = true) ||
                (hasExplicitScores && kickoffEpochMs in 1L..(nowMs - 3L * 3_600_000L))
            val isLiveOrStarted = rawStatus.contains("Half", ignoreCase = true) ||
                rawStatus.contains("Live", ignoreCase = true) ||
                rawStatus.endsWith("'") ||
                (!isFinished && kickoffEpochMs in 1L..nowMs)

            val mappedStatus = when {
                isFinished -> "finished"
                isLiveOrStarted -> "closed"
                else -> "open"
            }

            val yangonDate = if (kickoffEpochMs > 0L) {
                MyanmarTimeUtil.formatMatchDateIso(kickoffEpochMs)
            } else {
                dateStr.take(20)
            }
            val kickoff = when (mappedStatus) {
                "finished" -> "FT"
                "closed" -> "LIVE ${rawStatus.take(12)}".trim()
                else -> if (kickoffEpochMs > 0L) MyanmarTimeUtil.formatKickoffLabel(kickoffEpochMs)
                else formatDateAndUtcTime(dateStr, timeStr.take(5))
            }

            list.add(
                LiveFootballFixture(
                    externalId = "tsdb_$eventId",
                    league = league.take(60),
                    homeTeam = home.take(60),
                    awayTeam = away.take(60),
                    homeLogoUrl = homeBadge.take(300),
                    awayLogoUrl = awayBadge.take(300),
                    kickoffLabel = kickoff.take(40),
                    matchDate = yangonDate.take(20),
                    kickoffEpochMs = kickoffEpochMs.coerceAtLeast(0L),
                    handicapLine = "Home -0.5",
                    goalLine = "2.5",
                    bodyOdds = 1.92,
                    goalOdds = 1.90,
                    win1Odds = 1.95,
                    drawOdds = 3.45,
                    win2Odds = 3.70,
                    overOdds = 1.90,
                    underOdds = 1.90,
                    status = mappedStatus,
                    liveClock = if (mappedStatus == "finished") "FT" else if (mappedStatus == "closed") rawStatus.take(20).ifBlank { "LIVE" } else "",
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
            var m1201Set = "--"
            var m1201Val = "--"
            var e1630 = "--"
            var e430Set = "--"
            var e430Val = "--"
            var am930Mod = "--"
            var am930Int = "--"
            var pm200Mod = "--"
            var pm200Int = "--"

            val resultArr = root.optJSONArray("result")
            if (resultArr != null) {
                for (i in 0 until resultArr.length()) {
                    val item = resultArr.optJSONObject(i) ?: continue
                    val openTime = item.optString("open_time")
                    val r2d = item.optString("twod").ifBlank { "--" }
                    val rSet = item.optString("set").ifBlank { "--" }
                    val rVal = item.optString("value").ifBlank { "--" }
                    val mod = item.optString("modern", item.optString("modern_digit", "")).ifBlank { "--" }
                    val intl = item.optString("internet", item.optString("internet_digit", "")).ifBlank { "--" }
                    when {
                        openTime.contains("12:01") -> {
                            if (r2d != "--") m1201 = r2d
                            if (rSet != "--") m1201Set = rSet
                            if (rVal != "--") m1201Val = rVal
                        }
                        openTime.contains("16:30") || openTime.contains("4:30") -> {
                            if (r2d != "--") e1630 = r2d
                            if (rSet != "--") e430Set = rSet
                            if (rVal != "--") e430Val = rVal
                        }
                        openTime.contains("9:30") -> {
                            if (mod != "--") am930Mod = mod
                            if (intl != "--") am930Int = intl
                        }
                        openTime.contains("14:00") || openTime.contains("2:00") -> {
                            if (mod != "--") pm200Mod = mod
                            if (intl != "--") pm200Int = intl
                        }
                    }
                }
            }

            val holidayObj = root.optJSONObject("holiday")
            val serverDate = root.optString("server_time", timeStr)
            val nowMs = MyanmarTimeUtil.nowMs()
            val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Yangon")).apply {
                timeInMillis = nowMs
            }
            val dayName = SimpleDateFormat("EEEE", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Yangon")
            }.format(Date(nowMs))
            val dateLabel = SimpleDateFormat("MMM dd yyyy", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Yangon")
            }.format(Date(nowMs))
            val updatedText = if (timeStr.isNotBlank()) timeStr else serverDate.ifBlank {
                if (holidayObj != null) holidayObj.optString("name", "") else ""
            }

            Live2DStatus(
                twod = twod,
                setText = setStr,
                valueText = valStr,
                liveTime = timeStr,
                morning1201 = m1201,
                evening430 = e1630,
                m1201Set = m1201Set,
                m1201Val = m1201Val,
                e430Set = e430Set,
                e430Val = e430Val,
                am930Modern = am930Mod,
                am930Internet = am930Int,
                pm200Modern = pm200Mod,
                pm200Internet = pm200Int,
                updatedAtText = updatedText,
                dayLabel = dayName,
                dateLabel = dateLabel.ifBlank {
                    String.format(Locale.US, "%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
                }
            )
        } catch (e: Exception) {
            Log.d(TAG, "2D live fetch skipped: ${e.message}")
            null
        }
    }

    suspend fun fetch2DCalendarHistory(): Map<String, Lottery2DDayRecord> = withContext(Dispatchers.IO) {
        try {
            val body = httpGet("https://api.thaistock2d.com/2d_result") ?: return@withContext emptyMap()
            val arr = org.json.JSONArray(body)
            val out = LinkedHashMap<String, Lottery2DDayRecord>()
            for (i in 0 until arr.length()) {
                val dayObj = arr.optJSONObject(i) ?: continue
                val dateKey = dayObj.optString("date").trim().take(10)
                if (dateKey.length != 10) continue
                val childArr = dayObj.optJSONArray("child") ?: continue
                var m1201Set = "--"
                var m1201Val = "--"
                var m1201Twod = "--"
                var e430Set = "--"
                var e430Val = "--"
                var e430Twod = "--"
                var am930Mod = "--"
                var am930Int = "--"
                var pm200Mod = "--"
                var pm200Int = "--"
                for (j in 0 until childArr.length()) {
                    val item = childArr.optJSONObject(j) ?: continue
                    val t = item.optString("time")
                    val set = item.optString("set").ifBlank { "--" }
                    val value = item.optString("value").ifBlank { "--" }
                    val twod = item.optString("twod").ifBlank { "--" }
                    val mod = item.optString("modern", item.optString("modern_digit", "")).ifBlank { "--" }
                    val intl = item.optString("internet", item.optString("internet_digit", "")).ifBlank { "--" }
                    when {
                        t.contains("12:01") -> {
                            m1201Set = set
                            m1201Val = value
                            m1201Twod = twod
                        }
                        t.contains("16:30") || t.contains("4:30") -> {
                            e430Set = set
                            e430Val = value
                            e430Twod = twod
                        }
                        t.contains("9:30") -> {
                            if (mod != "--") am930Mod = mod
                            if (intl != "--") am930Int = intl
                        }
                        t.contains("14:00") || t.contains("2:00") -> {
                            if (mod != "--") pm200Mod = mod
                            if (intl != "--") pm200Int = intl
                        }
                    }
                }
                out[dateKey] = Lottery2DDayRecord(
                    m1201Set = m1201Set,
                    m1201Val = m1201Val,
                    m1201Twod = m1201Twod,
                    e430Set = e430Set,
                    e430Val = e430Val,
                    e430Twod = e430Twod,
                    am930Modern = am930Mod,
                    am930Internet = am930Int,
                    pm200Modern = pm200Mod,
                    pm200Internet = pm200Int
                )
            }
            out
        } catch (e: Exception) {
            Log.d(TAG, "2D history fetch skipped: ${e.message}")
            emptyMap()
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
                .header("User-Agent", "LoTaYa-Android/5.0")
                .build()
            val body = client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            } ?: return@withContext null

            val root = JSONObject(body)
            val tagName = root.optString("tag_name", "latest")
            val rawNotes = root.optString("body", "")
            val notes = rawNotes.lines()
                .filterNot { line ->
                    val low = line.lowercase(Locale.US)
                    low.contains("htet thu ya") ||
                        low.contains("htet-thu-ya") ||
                        low.contains("github.com")
                }
                .joinToString("\n")
                .trim()
                .take(350)
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
                .header("User-Agent", "LoTaYa-Android/5.0")
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
        val epochMs = MyanmarTimeUtil.parseIsoUtcEpochMs(isoUtc)
        return if (epochMs > 0L) MyanmarTimeUtil.formatKickoffLabel(epochMs) else isoUtc.take(16)
    }

    private fun formatDateAndUtcTime(dateStr: String, timeStr: String): String {
        if (timeStr.isBlank()) return dateStr
        val epochMs = MyanmarTimeUtil.parseDateAndUtcTimeEpochMs(dateStr, timeStr)
        return if (epochMs > 0L) MyanmarTimeUtil.formatKickoffLabel(epochMs) else "$dateStr $timeStr"
    }

    companion object {
        private const val TAG = "FootballFeed"
        const val DEFAULT_ONE_X_BET_LIVE_URL = "https://1xlite-19808.pro/en/live"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        val DEFAULT_LEAGUE_OPTIONS = listOf(
            "UEFA Champions League",
            "UEFA Champions League. Duel of the players. Goals",
            "UEFA Champions League. Winner",
            "UEFA Youth League",
            "Europa League",
            "UEFA Conference League",
            "Premier League",
            "FA Cup",
            "Carabao Cup",
            "Championship",
            "La Liga",
            "La Liga 2",
            "Copa del Rey",
            "Spain",
            "Bundesliga",
            "2. Bundesliga",
            "Serie A",
            "Serie B",
            "Coppa Italia",
            "Italy",
            "Ligue 1",
            "Ligue 2",
            "AFC Champions League Elite",
            "AFC Champions League 2",
            "Gulf Club Championship",
            "Thai League 1",
            "J1 League (Japan)",
            "K League 1 (Korea)",
            "Saudi Pro League",
            "Chinese Super League",
            "A-League",
            "Russian Premier League",
            "Russian Cup",
            "Egypt. Premier League",
            "Peru Cup",
            "Botswana Championship",
            "Austria. Amateur Cup",
            "El Salvador. Reserve League",
            "Greece. Amateur Cup",
            "Israel",
            "Brazil",
            "Brasileirao Serie A",
            "Senegal. League des Pepites U20",
            "Short Football 3x3",
            "Short Football 4x4",
            "Short Football 5x5",
            "Short Football 3x3 L1",
            "Short Football 4x4 L2",
            "Student League",
            "Student League 2",
            "BudnesLiga LFL 5x5",
            "Division 4x4",
            "FIFA 27. Amateur daily league",
            "FIFA 23. Volta daily league",
            "World Cup Qualifiers"
        )

        fun normalizeGoogleSheetCsvUrl(raw: String): String {
            val cleaned = raw.trim()
            if (cleaned.isBlank()) return ""
            val withHttps = when {
                cleaned.startsWith("https://", ignoreCase = true) -> cleaned
                cleaned.startsWith("http://", ignoreCase = true) -> "https://" + cleaned.removePrefix("http://").removePrefix("HTTP://")
                else -> "https://$cleaned"
            }
            val sheetMatch = Regex("""docs\.google\.com/spreadsheets/d/([a-zA-Z0-9_-]+)""").find(withHttps)
            if (sheetMatch != null && !withHttps.contains("/export", ignoreCase = true) && !withHttps.contains("output=csv", ignoreCase = true)) {
                val docId = sheetMatch.groupValues[1]
                val gidMatch = Regex("""gid=(\d+)""").find(withHttps)
                val gidPart = if (gidMatch != null) "&gid=${gidMatch.groupValues[1]}" else ""
                return "https://docs.google.com/spreadsheets/d/$docId/export?format=csv$gidPart"
            }
            return withHttps.take(500)
        }

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
                isLiveFeed || (startMs in 1L..nowMs) -> "closed"
                else -> "open"
            }
            val liveClock = when (status) {
                "finished" -> "FT"
                "closed" -> if (livePeriodClock.isNotBlank()) "LIVE $livePeriodClock" else "LIVE"
                else -> ""
            }

            val kickoffLabel = if (status == "open" && startMs > 0L) {
                MyanmarTimeUtil.formatKickoffLabel(startMs)
            } else {
                liveClock.ifBlank { "LIVE" }
            }

            val matchDate = if (startMs > 0L) {
                MyanmarTimeUtil.formatMatchDateIso(startMs)
            } else {
                MyanmarTimeUtil.formatMatchDateIso(nowMs)
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
                kickoffEpochMs = startMs.coerceAtLeast(0L),
                handicapLine = parsedLines.handicapLine.take(40),
                goalLine = parsedLines.goalLine.take(20),
                bodyOdds = parsedLines.bodyOdds,
                goalOdds = parsedLines.goalOdds,
                win1Odds = parsedLines.win1Odds,
                drawOdds = parsedLines.drawOdds,
                win2Odds = parsedLines.win2Odds,
                overOdds = parsedLines.overOdds,
                underOdds = parsedLines.underOdds,
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
                .removeSuffix("ac")
                .removeSuffix("sc")
                .removeSuffix("cf")
                .removePrefix("fc")
                .removePrefix("ac")
                .removePrefix("estac")
                .removePrefix("as")
        }

        fun fuzzyTeamMatches(a: String, b: String): Boolean {
            val normA = normalizeTeam(a)
            val normB = normalizeTeam(b)
            if (normA.isBlank() || normB.isBlank()) return false
            if (normA == normB) return true
            if (normA.length >= 4 && normB.length >= 4 && (normA.contains(normB) || normB.contains(normA))) {
                return true
            }
            val tokensA = a.lowercase(Locale.US).split(Regex("[^a-z0-9]+")).filter { it.length >= 4 }.toSet()
            val tokensB = b.lowercase(Locale.US).split(Regex("[^a-z0-9]+")).filter { it.length >= 4 }.toSet()
            return tokensA.isNotEmpty() && tokensA.intersect(tokensB).isNotEmpty()
        }
    }
}
