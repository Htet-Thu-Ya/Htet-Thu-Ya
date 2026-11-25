package com.example.data.util

import com.example.data.model.SportMatch
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MyanmarTimeUtil {
    val YANGON_TZ: TimeZone = TimeZone.getTimeZone("Asia/Yangon")

    private val MONTH_MAP = mapOf(
        "jan" to Calendar.JANUARY,
        "feb" to Calendar.FEBRUARY,
        "mar" to Calendar.MARCH,
        "apr" to Calendar.APRIL,
        "may" to Calendar.MAY,
        "jun" to Calendar.JUNE,
        "jul" to Calendar.JULY,
        "aug" to Calendar.AUGUST,
        "sep" to Calendar.SEPTEMBER,
        "oct" to Calendar.OCTOBER,
        "nov" to Calendar.NOVEMBER,
        "dec" to Calendar.DECEMBER
    )

    fun nowMs(): Long = System.currentTimeMillis()

    fun todayIsoDate(nowMs: Long = System.currentTimeMillis()): String {
        return isoDateForOffset(0, nowMs)
    }

    fun isoDateForOffset(dayOffset: Int, nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(YANGON_TZ).apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = YANGON_TZ
        }.format(cal.time)
    }

    fun espnDateForOffset(dayOffset: Int, nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(YANGON_TZ).apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        return SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = YANGON_TZ
        }.format(cal.time)
    }

    fun utcIsoDateForOffset(dayOffset: Int, nowMs: Long = System.currentTimeMillis()): String {
        val utc = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(utc).apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = utc
        }.format(cal.time)
    }

    fun formatKickoffLabel(epochMs: Long): String {
        if (epochMs <= 0L) return "Today"
        return SimpleDateFormat("dd MMM hh:mm a", Locale.US).apply {
            timeZone = YANGON_TZ
        }.format(Date(epochMs))
    }

    fun formatEpochDateTime(epochMs: Long): String {
        if (epochMs <= 0L) return "-"
        return SimpleDateFormat("dd MMM HH:mm", Locale.US).apply {
            timeZone = YANGON_TZ
        }.format(Date(epochMs))
    }

    fun formatTimestamp(ts: Timestamp?): String {
        if (ts == null) return "Just now"
        return SimpleDateFormat("dd MMM HH:mm", Locale.US).apply {
            timeZone = YANGON_TZ
        }.format(ts.toDate())
    }

    fun formatMatchDateIso(epochMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = YANGON_TZ
        }.format(Date(epochMs))
    }

    fun parseIsoUtcEpochMs(isoUtc: String): Long {
        val raw = isoUtc.trim()
        if (raw.isBlank()) return 0L
        val utc = TimeZone.getTimeZone("UTC")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mmXXX"
        )
        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = utc
                }
                val parsed = fmt.parse(raw)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    fun parseDateAndUtcTimeEpochMs(dateStr: String, timeStr: String): Long {
        val d = dateStr.trim()
        val t = timeStr.trim()
        if (d.isBlank() || t.isBlank()) return 0L
        val utc = TimeZone.getTimeZone("UTC")
        val hhmm = if (t.length >= 8) t.take(8) else t.take(5)
        val patterns = if (hhmm.length == 8) listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm") else listOf("yyyy-MM-dd HH:mm")
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US).apply {
                    timeZone = utc
                }
                val parsed = fmt.parse("$d $hhmm")
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return 0L
    }

    fun resolveMatchKickoffEpochMs(
        match: SportMatch,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        if (match.kickoffEpochMs > 0L) return match.kickoffEpochMs

        val label = match.kickoffLabel.trim()
        if (label.isBlank()) return 0L
        val low = label.lowercase(Locale.US)
        if (low == "finished" || low == "ft" || low == "closed" || low == "tbd" || low.startsWith("live")) {
            return 0L
        }

        val timeMatch = Regex("""(\d{1,2}):(\d{2})(?:\s*(AM|PM))?""", RegexOption.IGNORE_CASE).find(label)
            ?: return 0L
        var hour = timeMatch.groupValues[1].toIntOrNull() ?: return 0L
        val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
        val amPm = timeMatch.groupValues[3].uppercase(Locale.US)
        if (amPm == "PM" && hour < 12) hour += 12
        else if (amPm == "AM" && hour == 12) hour = 0
        if (hour !in 0..23 || minute !in 0..59) return 0L

        val dateStr = match.matchDate.trim()
        val matchDateParsed = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(dateStr)

        // Priority 1: Explicit "dd MMM" inside kickoffLabel (e.g. "25 Nov 02:30 AM")
        val dayMonthMatch = Regex("""\b(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b""", RegexOption.IGNORE_CASE)
            .find(label)
        if (dayMonthMatch != null) {
            val day = dayMonthMatch.groupValues[1].toIntOrNull() ?: 1
            val monKey = dayMonthMatch.groupValues[2].lowercase(Locale.US)
            val targetMonth = MONTH_MAP[monKey]
            if (targetMonth != null) {
                val baseCal = Calendar.getInstance(YANGON_TZ).apply { timeInMillis = nowMs }
                var year = matchDateParsed?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: baseCal.get(Calendar.YEAR)
                val refMonth = matchDateParsed?.groupValues?.getOrNull(2)?.toIntOrNull()?.minus(1)
                    ?: baseCal.get(Calendar.MONTH)
                // Handle Dec <-> Jan year rollover cleanly
                if (refMonth == Calendar.DECEMBER && targetMonth == Calendar.JANUARY) {
                    year += 1
                } else if (refMonth == Calendar.JANUARY && targetMonth == Calendar.DECEMBER) {
                    year -= 1
                }
                val cal = Calendar.getInstance(YANGON_TZ).apply {
                    set(year, targetMonth, day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        }

        // Priority 2: Explicit yyyy-MM-dd in label or matchDate
        val fullDateMatch = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(label) ?: matchDateParsed
        if (fullDateMatch != null) {
            val y = fullDateMatch.groupValues[1].toInt()
            val m = fullDateMatch.groupValues[2].toInt() - 1
            val d = fullDateMatch.groupValues[3].toInt()
            val cal = Calendar.getInstance(YANGON_TZ).apply {
                set(y, m, d, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        // Priority 3: dd-MM-yyyy in label
        val dmyMatch = Regex("""(\d{1,2})-(\d{1,2})-(\d{4})""").find(label)
        if (dmyMatch != null) {
            val d = dmyMatch.groupValues[1].toInt()
            val m = dmyMatch.groupValues[2].toInt() - 1
            val y = dmyMatch.groupValues[3].toInt()
            val cal = Calendar.getInstance(YANGON_TZ).apply {
                set(y, m, d, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        // Priority 4: Relative label ("Tonight 21:00", "Tomorrow 01:30", "Today 22:00") in Yangon TZ
        val cal = Calendar.getInstance(YANGON_TZ).apply {
            timeInMillis = nowMs
            if (low.contains("tomorrow") || label.contains("မနက်ဖြန်")) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isMatchBettable(match: SportMatch, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (match.status != "open") return false
        if (match.liveClock.isNotBlank()) return false
        val lblLow = match.kickoffLabel.trim().lowercase(Locale.US)
        if (lblLow == "finished" || lblLow == "ft" || lblLow == "closed" || lblLow.startsWith("live")) {
            return false
        }
        val kickoffMs = resolveMatchKickoffEpochMs(match, nowMs)
        if (kickoffMs > 0L && nowMs >= kickoffMs) {
            return false
        }
        return true
    }

    fun isMatchStartedOrExpired(match: SportMatch, nowMs: Long = System.currentTimeMillis()): Boolean {
        return !isMatchBettable(match, nowMs)
    }
}
