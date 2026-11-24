package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SportMatch
import com.example.ui.theme.AmberPending
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ChampionshipGoldBright
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PauseBannerBg
import com.example.ui.theme.PauseBannerPillBg
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reusable collapsible "Hide Bar" for large tables, lists, and cards.
 */
@Composable
fun HideBarSectionHeader(
    title: String,
    isVisible: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    countBadge: String? = null,
    containerColor: Color = Color(0xFF13261E),
    accentColor: Color = ChampionshipGold
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag(testTag),
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!countBadge.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = countBadge,
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isVisible) "ဝှက်မည်" else "ပြမည်",
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isVisible) "Hide section" else "Show section",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compresses and encodes a selected gallery/photo URI to a Firestore-safe JPEG Base64 string (<= 135,000 chars).
 */
fun encodeUriToReceiptBase64(context: Context, uri: Uri): String? {
    return runCatching {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        val maxDim = 720
        var sampleSize = 1
        while ((origW / sampleSize) > maxDim * 2 || (origH / sampleSize) > maxDim * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val longest = max(decoded.width, decoded.height)
        val scaledBitmap = if (longest > maxDim) {
            val scale = maxDim.toFloat() / longest.toFloat()
            val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        var resultBase64: String? = null
        val qualities = listOf(72, 58, 45, 34, 24)
        for (q in qualities) {
            val out = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, q, out)
            val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            resultBase64 = encoded
            if (encoded.length <= 135_000) {
                break
            }
        }
        scaledBitmap.recycle()
        resultBase64?.takeIf { it.length <= 175_000 }
    }.getOrNull()
}

fun decodeReceiptBitmap(base64: String): ImageBitmap? {
    val clean = base64.trim().substringAfter("base64,", base64.trim())
    if (clean.isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

@Composable
fun ReceiptImageThumbnail(
    base64: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    label: String = "ပြေစာ"
) {
    val bmp = remember(base64) { decodeReceiptBitmap(base64) }
    if (bmp == null) return
    Surface(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
        color = Color(0xFF0E1D17)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = bmp,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(topStart = 6.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom receipt",
                    tint = Color.White,
                    modifier = Modifier.padding(3.dp).size(12.dp)
                )
            }
        }
    }
}

@Composable
fun FullScreenReceiptDialog(
    base64: String,
    title: String = "ငွေလွှဲပြေစာ ဓာတ်ပုံ",
    onDismiss: () -> Unit
) {
    val bmp = remember(base64) { decodeReceiptBitmap(base64) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            color = DeepPitchBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("ဓာတ်ပုံ ဖွင့်၍မရပါ", color = Color(0xFFB4C8BE))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

fun formatCountdownPill(reopenAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (reopenAtMs <= nowMs) return ""
    val remSec = ((reopenAtMs - nowMs) / 1000L).coerceAtLeast(0L)
    val days = remSec / 86_400L
    val hrs = (remSec % 86_400L) / 3600L
    val mins = (remSec % 3600L) / 60L
    val secs = remSec % 60L
    val hms = String.format(Locale.ENGLISH, "%02d:%02d:%02d", hrs, mins, secs)
    return if (days > 0L) "${days}ရက် $hms" else hms
}

fun formatYangonReopenTime(reopenAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (reopenAtMs <= 0L) return ""
    val tz = TimeZone.getTimeZone("Asia/Yangon")
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = tz }
    val sameDay = dayFmt.format(Date(reopenAtMs)) == dayFmt.format(Date(nowMs))
    val pattern = if (sameDay) "hh:mm a" else "dd MMM hh:mm a"
    val fmt = SimpleDateFormat(pattern, Locale.ENGLISH).apply { timeZone = tz }
    return fmt.format(Date(reopenAtMs))
}

fun formatRemainingReopenCountdown(reopenAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (reopenAtMs <= 0L) return ""
    val openAtStr = formatYangonReopenTime(reopenAtMs, nowMs)
    val countdown = formatCountdownPill(reopenAtMs, nowMs).ifBlank { "00:00:00" }
    return "$openAtStr (မြန်မာစံတော်ချိန်) တွင် အလိုအလျောက် ပြန်ဖွင့်မည် • $countdown ကျန်"
}

@Composable
fun PauseStatusBannerCard(
    title: String,
    reopenAtMs: Long,
    nowMs: Long,
    modifier: Modifier = Modifier
) {
    val hasCountdown = reopenAtMs > nowMs
    val countdownText = remember(reopenAtMs, nowMs) {
        if (hasCountdown) formatCountdownPill(reopenAtMs, nowMs) else ""
    }
    val openAtText = remember(reopenAtMs, nowMs) {
        if (hasCountdown) formatYangonReopenTime(reopenAtMs, nowMs) else ""
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("pause_status_banner_card"),
        colors = CardDefaults.cardColors(containerColor = PauseBannerBg),
        border = BorderStroke(1.2.dp, AmberPending),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PauseCircle,
                    contentDescription = null,
                    tint = AmberPending,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!hasCountdown) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Admin မှ ယာယီရပ်နားထားပါသည်။ ပြန်ဖွင့်ချိန်ကို စောင့်ဆိုင်းပါ။",
                            color = ChampionshipGoldBright,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (hasCountdown) {
                    Surface(
                        color = PauseBannerPillBg,
                        border = BorderStroke(1.dp, ChampionshipGoldBright.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reopen_countdown_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Countdown",
                                tint = ChampionshipGoldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = countdownText,
                                color = ChampionshipGoldBright,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            if (hasCountdown) {
                Surface(
                    color = Color.Black.copy(alpha = 0.24f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ပြန်ဖွင့်မည့်အချိန်: $openAtText (မြန်မာစံတော်ချိန်) — အချိန်ပြည့်သည်နှင့် အလိုအလျောက် ပြန်ဖွင့်ပေးပါမည်",
                        color = ChampionshipGoldBright,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

fun parseMatchKickoffEpochMs(match: SportMatch): Long {
    val label = match.kickoffLabel.trim()
    val dateStr = match.matchDate.trim()
    val timeMatch = Regex("""(\d{1,2}):(\d{2})(?:\s*(AM|PM))?""", RegexOption.IGNORE_CASE).find(label)
        ?: return 0L
    var hour = timeMatch.groupValues[1].toIntOrNull() ?: return 0L
    val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
    val amPm = timeMatch.groupValues[3].uppercase()
    if (amPm == "PM" && hour < 12) hour += 12
    else if (amPm == "AM" && hour == 12) hour = 0

    val cal = java.util.Calendar.getInstance()
    val fullDateMatch = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(dateStr)
        ?: Regex("""(\d{4})-(\d{2})-(\d{2})""").find(label)
    if (fullDateMatch != null) {
        val y = fullDateMatch.groupValues[1].toInt()
        val m = fullDateMatch.groupValues[2].toInt() - 1
        val d = fullDateMatch.groupValues[3].toInt()
        cal.set(y, m, d, hour, minute, 0)
    } else {
        val dmyMatch = Regex("""(\d{1,2})-(\d{1,2})-(\d{4})""").find(label)
        if (dmyMatch != null) {
            val d = dmyMatch.groupValues[1].toInt()
            val m = dmyMatch.groupValues[2].toInt() - 1
            val y = dmyMatch.groupValues[3].toInt()
            cal.set(y, m, d, hour, minute, 0)
        } else {
            if (label.contains("tomorrow", ignoreCase = true) || label.contains("မနက်ဖြန်")) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
            cal.set(java.util.Calendar.MINUTE, minute)
            cal.set(java.util.Calendar.SECOND, 0)
        }
    }
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun buildOrderKickoffSummaryText(pickedMatches: List<SportMatch>): String {
    if (pickedMatches.isEmpty()) return ""
    return if (pickedMatches.size == 1) {
        val m = pickedMatches.first()
        "${m.homeTeam} vs ${m.awayTeam} • ${m.kickoffLabel}".take(160)
    } else {
        val earliest = pickedMatches.minByOrNull { m ->
            val epoch = parseMatchKickoffEpochMs(m)
            if (epoch > 0L) epoch else Long.MAX_VALUE
        } ?: pickedMatches.first()
        val extra = pickedMatches.drop(1).take(2).joinToString(", ") { "${it.homeTeam}-${it.awayTeam} (${it.kickoffLabel})" }
        val tail = if (extra.isNotBlank()) " | $extra" else ""
        "${earliest.homeTeam} vs ${earliest.awayTeam} (${earliest.kickoffLabel})$tail".take(160)
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFB4C8BE),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(18.dp)
        )
    }
}

