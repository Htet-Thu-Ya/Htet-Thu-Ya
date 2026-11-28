package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.betting.KyayFormatter
import com.example.data.model.SportMatch
import com.example.data.network.FootballFeedService
import com.example.data.util.MyanmarTimeUtil
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PitchEmeraldDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MatchPick(
    val matchId: String,
    val label: String,
    val odds: Double,
    val pickKey: String
)

val BoardTopBarBg = Color(0xFF232B28)
val BoardHeaderDark = Color(0xFF4F5451)
val BoardGreenAccent = Color(0xFF1DB954)
val BoardCellSelectedBg = Color(0xFF16A34A)
val BoardCellDefaultBg = Color(0xFFFDFDFD)
val BoardBorderGray = Color(0xFFD5DCD8)

val OneXBetCanvasBg = Color(0xFFEEF3F8)
val OneXBetHeaderBlue = Color(0xFF123B6D)
val OneXBetStripBlue = Color(0xFF184D8C)
val OneXBetRoyalBlue = Color(0xFF1967D2)
val OneXBetNavyInk = Color(0xFF162C46)
val OneXBetSlateInk = Color(0xFF5A7088)
val OneXBetCardBorder = Color(0xFFDDE6F0)
val OneXBetTileBg = Color(0xFFEEF3F8)

fun compactKyayText(normalizedLine: String): String {
    val cleanSpec = normalizedLine
        .replace("home", "", ignoreCase = true)
        .replace("away", "", ignoreCase = true)
        .replace("draw", "", ignoreCase = true)
        .replace("even", "0", ignoreCase = true)
        .trim()
        .removePrefix("-")
        .removePrefix("+")
        .trim()
        .ifBlank { "0.5" }
    return when {
        cleanSpec == "0" || cleanSpec == "0.0" -> "="
        cleanSpec.startsWith("0+") -> "= +${cleanSpec.removePrefix("0+")}"
        cleanSpec.startsWith("0-") -> "= -${cleanSpec.removePrefix("0-")}"
        cleanSpec.startsWith("0 ") -> "= ${cleanSpec.removePrefix("0 ").trim()}"
        else -> cleanSpec
    }
}

fun formatFiveFiveFiveYellowKyay(rawLine: String): String {
    val clean = rawLine
        .replace("home", "", ignoreCase = true)
        .replace("away", "", ignoreCase = true)
        .replace("draw", "", ignoreCase = true)
        .replace("even", "0", ignoreCase = true)
        .trim()
        .removePrefix("-")
        .removePrefix("+")
        .trim()
        .ifBlank { "0.5" }

    val kyayRegex = Regex("""^(\d+(?:\.\d+)?)\s*([+-])\s*(\d{1,3})$""")
    val kyayMatch = kyayRegex.find(clean)
    if (kyayMatch != null) {
        val base = kyayMatch.groupValues[1]
        val sign = kyayMatch.groupValues[2]
        val pct = kyayMatch.groupValues[3]
        val baseDisplay = if (base == "0" || base == "0.0") "=" else base
        return "$baseDisplay $sign$pct"
    }

    return when (clean) {
        "0", "0.0" -> "="
        "0.25", "0/0.5" -> "= +50"
        "0.75", "0.5/1" -> "1 -50"
        "1.25", "1/1.5" -> "1 +50"
        "1.75", "1.5/2" -> "2 -50"
        "2.25", "2/2.5" -> "2 +50"
        "2.75", "2.5/3" -> "3 -50"
        "3.25", "3/3.5" -> "3 +50"
        "3.75", "3.5/4" -> "4 -50"
        else -> clean
    }
}

@Composable
fun SportsModeSwitchStrip(
    isParlay: Boolean,
    accentColor: Color = BoardGreenAccent,
    containerColor: Color = BoardTopBarBg,
    onSelectMode: (Boolean) -> Unit,
    testTagPrefix: String
) {
    Surface(
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val singleSelected = !isParlay
            val inactiveTileBg = if (containerColor == BoardTopBarBg) Color(0xFF323D39) else Color.White.copy(alpha = 0.12f)
            Surface(
                color = if (singleSelected) accentColor else inactiveTileBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (singleSelected) accentColor else Color.White.copy(alpha = 0.14f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(false) }
                    .testTag("${testTagPrefix}_single")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "ဘော်ဒီ/ဂိုးပေါင်း",
                        color = if (singleSelected && accentColor == ChampionshipGold) InkDark else Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val parlaySelected = isParlay
            Surface(
                color = if (parlaySelected) accentColor else inactiveTileBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (parlaySelected) accentColor else Color.White.copy(alpha = 0.14f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(true) }
                    .testTag("${testTagPrefix}_parlay")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "မောင်း (၂ ပွဲ+)",
                        color = if (parlaySelected && accentColor == ChampionshipGold) InkDark else Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SportsLeagueHeaderRow(leagueName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE2E8E5))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = BoardGreenAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = leagueName,
            color = InkDark,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LeagueHideBarHeader(
    leagueName: String,
    matchCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val low = leagueName.lowercase(Locale.US)
    val isCup = low.contains("champions") || low.contains("cup") || low.contains("uefa") || low.contains("afc")
    val isShort = low.contains("short") || low.contains("student") || low.contains("fifa") || low.contains("4x4") || low.contains("3x3") || low.contains("5x5")
    val badgeBg = when {
        isCup -> Color(0xFFFEF3C7)
        isShort -> Color(0xFFFEE2E2)
        else -> Color(0xFFDCFCE7)
    }
    val badgeTint = when {
        isCup -> Color(0xFFB45309)
        isShort -> Color(0xFFB91C1C)
        else -> PitchEmeraldDark
    }
    val safeTag = remember(leagueName) {
        leagueName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').take(32)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("league_hide_bar_$safeTag"),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isExpanded) BoardGreenAccent.copy(alpha = 0.65f) else BoardBorderGray
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isCup -> Icons.Default.EmojiEvents
                            isShort -> Icons.Default.SportsSoccer
                            else -> Icons.Default.Public
                        },
                        contentDescription = null,
                        tint = badgeTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$leagueName ($matchCount)",
                    color = InkDark,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = if (isExpanded) BoardGreenAccent.copy(alpha = 0.14f) else Color(0xFFF3F4F6),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "ဝှက်မည်" else "ပြမည်",
                        color = if (isExpanded) PitchEmeraldDark else InkMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Hide league matches" else "Show league matches",
                        tint = if (isExpanded) PitchEmeraldDark else InkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenshotMatchRowCard(
    match: SportMatch,
    selectedPickKey: String?,
    onShowInfo: () -> Unit,
    onSelect: (String, Double, String) -> Unit,
    nowMs: Long = System.currentTimeMillis()
) {
    val bettable = remember(match.id, match.status, match.liveClock, match.kickoffLabel, match.kickoffEpochMs, nowMs / 10_000L) {
        MyanmarTimeUtil.isMatchBettable(match, nowMs)
    }
    val handicapInfo = remember(match.id, match.handicapLine, match.homeTeam, match.awayTeam) {
        KyayFormatter.formatHandicap(match)
    }
    val compactHandicap = remember(handicapInfo.normalizedLine) {
        compactKyayText(handicapInfo.normalizedLine)
    }
    val compactGoal = remember(match.goalLine) {
        match.goalLine.trim().ifBlank { "2.5" }
    }

    val homePick = "${match.homeTeam} (${handicapInfo.homeBadge}) vs ${match.awayTeam}"
    val awayPick = "${match.awayTeam} (${handicapInfo.awayBadge}) vs ${match.homeTeam}"
    val overPick = "${match.homeTeam}-${match.awayTeam} ဂိုးပေါင်း Over (${match.goalLine})"
    val underPick = "${match.homeTeam}-${match.awayTeam} ဂိုးပေါင်း Under (${match.goalLine})"

    val homeSelected = selectedPickKey == "home"
    val awaySelected = selectedPickKey == "away"
    val overSelected = selectedPickKey == "over"
    val underSelected = selectedPickKey == "under"

    val kickoffDisplay = remember(match.kickoffLabel, match.liveClock, match.status, match.homeScore, match.awayScore, bettable) {
        val base = "ပွဲချိန် : ${match.kickoffLabel}"
        when {
            match.liveClock.isNotBlank() -> "$base • ${match.homeScore}-${match.awayScore} (${match.liveClock})"
            match.status == "finished" -> "$base • FT ${match.homeScore}-${match.awayScore}"
            !bettable -> "$base (ပိတ်ပြီး)"
            else -> "$base (MMT)"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (bettable) BoardHeaderDark else Color(0xFF6B3A3A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = kickoffDisplay,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onShowInfo)
                    .testTag("match_info_${match.id}"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "ကြေးအသေးစိတ်",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Row 1: Home Team vs Away Team with TeamLogoAvatar & compact Kyay Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .border(0.5.dp, BoardBorderGray)
        ) {
            // Home Cell
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (homeSelected) BoardCellSelectedBg else if (bettable) BoardCellDefaultBg else Color(0xFFF3F4F6))
                    .clickable(enabled = bettable) { onSelect(homePick, match.bodyOdds, "home") }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .testTag("match_home_${match.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoAvatar(
                        logoUrl = match.homeLogoUrl,
                        teamName = match.homeTeam,
                        size = 24.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = match.homeTeam,
                        color = if (homeSelected) Color.White else InkDark,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (handicapInfo.favIsHome) {
                    Spacer(Modifier.width(4.dp))
                    KyayPillBadge(text = compactHandicap, inverted = homeSelected)
                }
            }

            VerticalDivider(thickness = 1.dp, color = BoardBorderGray)

            // Away Cell
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (awaySelected) BoardCellSelectedBg else if (bettable) BoardCellDefaultBg else Color(0xFFF3F4F6))
                    .clickable(enabled = bettable) { onSelect(awayPick, match.bodyOdds, "away") }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .testTag("match_away_${match.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamLogoAvatar(
                        logoUrl = match.awayLogoUrl,
                        teamName = match.awayTeam,
                        size = 24.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = match.awayTeam,
                        color = if (awaySelected) Color.White else InkDark,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!handicapInfo.favIsHome) {
                    Spacer(Modifier.width(4.dp))
                    KyayPillBadge(text = compactHandicap, inverted = awaySelected)
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BoardBorderGray)

        // Row 2: Over | GoalLine | Under
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(0.5.dp, BoardBorderGray),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (overSelected) BoardCellSelectedBg else if (bettable) BoardCellDefaultBg else Color(0xFFF3F4F6))
                    .clickable(enabled = bettable) { onSelect(overPick, match.goalOdds, "over") }
                    .testTag("match_over_${match.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Over",
                    color = if (overSelected) Color.White else InkDark,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            VerticalDivider(thickness = 1.dp, color = BoardBorderGray)

            Box(
                modifier = Modifier
                    .widthIn(min = 68.dp)
                    .fillMaxHeight()
                    .background(BoardGreenAccent.copy(alpha = 0.14f))
                    .clickable(onClick = onShowInfo)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = compactGoal,
                    color = PitchEmeraldDark,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            VerticalDivider(thickness = 1.dp, color = BoardBorderGray)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (underSelected) BoardCellSelectedBg else if (bettable) BoardCellDefaultBg else Color(0xFFF3F4F6))
                    .clickable(enabled = bettable) { onSelect(underPick, match.goalOdds, "under") }
                    .testTag("match_under_${match.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Under",
                    color = if (underSelected) Color.White else InkDark,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private data class OneXBetTileSpec(
    val pickKey: String,
    val title: String,
    val oddsText: String,
    val oddsValue: Double,
    val pickLabel: String
)

@Composable
fun OneXBetSectionHeader(
    isVisible: Boolean,
    matchCount: Int,
    onToggle: () -> Unit,
    testTag: String = "one_xbet_section_header"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = OneXBetNavyInk,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ဘောလုံး ($matchCount)",
                color = OneXBetNavyInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Icon(
            imageVector = if (isVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isVisible) "Collapse football" else "Expand football",
            tint = OneXBetSlateInk,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun OneXBetLeagueHideBarHeader(
    leagueName: String,
    matchCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val low = leagueName.lowercase(Locale.US)
    val isCup = low.contains("champions") || low.contains("cup") || low.contains("uefa") || low.contains("afc")
    val isShort = low.contains("short") || low.contains("student") || low.contains("fifa") || low.contains("4x4") || low.contains("3x3") || low.contains("5x5")
    val badgeBg = when {
        isCup -> Color(0xFFFEF3C7)
        isShort -> Color(0xFFFEE2E2)
        else -> Color(0xFFE0ECFF)
    }
    val badgeTint = when {
        isCup -> Color(0xFFB45309)
        isShort -> Color(0xFFB91C1C)
        else -> OneXBetRoyalBlue
    }
    val safeTag = remember(leagueName) {
        leagueName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').take(32)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("one_xbet_league_hide_bar_$safeTag"),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OneXBetCardBorder),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isCup -> Icons.Default.EmojiEvents
                            isShort -> Icons.Default.SportsSoccer
                            else -> Icons.Default.Public
                        },
                        contentDescription = null,
                        tint = badgeTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$leagueName ($matchCount)",
                    color = OneXBetNavyInk,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Hide league matches" else "Show league matches",
                tint = OneXBetSlateInk,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun OneXBetScreenshotMatchCard(
    match: SportMatch,
    selectedPickKey: String?,
    onShowInfo: () -> Unit,
    onSelect: (String, Double, String) -> Unit,
    nowMs: Long = System.currentTimeMillis()
) {
    val bettable = remember(match.id, match.status, match.liveClock, match.kickoffLabel, match.kickoffEpochMs, nowMs / 10_000L) {
        MyanmarTimeUtil.isMatchBettable(match, nowMs)
    }
    val resolvedKickoffMs = remember(match.id, match.kickoffEpochMs, match.kickoffLabel, match.matchDate) {
        MyanmarTimeUtil.resolveMatchKickoffEpochMs(match, nowMs)
    }
    val headerTimeText = remember(match.kickoffLabel, match.liveClock, match.status, bettable, resolvedKickoffMs, nowMs / 30_000L) {
        when {
            match.liveClock.isNotBlank() -> match.liveClock
            match.status == "finished" -> "ပြီးဆုံး (FT)"
            !bettable -> "${match.kickoffLabel} • ပိတ်ပြီး"
            resolvedKickoffMs > nowMs -> {
                val mins = ((resolvedKickoffMs - nowMs) / 60_000L).coerceAtLeast(1L)
                if (mins < 60L) "$mins မိနစ်တွင် စတင်မည်"
                else "${match.kickoffLabel} (MMT)"
            }
            else -> "${match.kickoffLabel} (MMT)"
        }
    }
    val goalText = remember(match.goalLine) {
        match.goalLine.trim().ifBlank { "2.5" }
    }
    val tiles = remember(
        match.id,
        match.homeTeam,
        match.awayTeam,
        goalText,
        match.win1Odds,
        match.drawOdds,
        match.win2Odds,
        match.overOdds,
        match.underOdds
    ) {
        listOf(
            OneXBetTileSpec(
                pickKey = "w1",
                title = "W1",
                oddsText = KyayFormatter.formatOneXBetOdds(match.win1Odds),
                oddsValue = match.win1Odds,
                pickLabel = "${match.homeTeam} (W1) vs ${match.awayTeam}"
            ),
            OneXBetTileSpec(
                pickKey = "draw",
                title = "သရေ",
                oddsText = KyayFormatter.formatOneXBetOdds(match.drawOdds),
                oddsValue = match.drawOdds,
                pickLabel = "${match.homeTeam} vs ${match.awayTeam} (သရေ)"
            ),
            OneXBetTileSpec(
                pickKey = "w2",
                title = "W2",
                oddsText = KyayFormatter.formatOneXBetOdds(match.win2Odds),
                oddsValue = match.win2Odds,
                pickLabel = "${match.awayTeam} (W2) vs ${match.homeTeam}"
            ),
            OneXBetTileSpec(
                pickKey = "over",
                title = "ဂိုးပေါ် $goalText",
                oddsText = KyayFormatter.formatOneXBetOdds(match.overOdds),
                oddsValue = match.overOdds,
                pickLabel = "${match.homeTeam}-${match.awayTeam} ဂိုးပေါ် ($goalText)"
            ),
            OneXBetTileSpec(
                pickKey = "under",
                title = "ဂိုးအောက် $goalText",
                oddsText = KyayFormatter.formatOneXBetOdds(match.underOdds),
                oddsValue = match.underOdds,
                pickLabel = "${match.homeTeam}-${match.awayTeam} ဂိုးအောက် ($goalText)"
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OneXBetCardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card top row: soccer ball + kickoff / countdown + icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = null,
                        tint = OneXBetNavyInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = headerTimeText,
                        color = if (bettable) OneXBetSlateInk else Color(0xFFB91C1C),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (match.liveClock.isNotBlank()) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = "Live",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp).size(14.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onShowInfo)
                            .testTag("match_info_${match.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "ကြေးအသေးစိတ်",
                            tint = OneXBetSlateInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = OneXBetCanvasBg)

            // Teams + Scores + Subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogoAvatar(
                            logoUrl = match.homeLogoUrl,
                            teamName = match.homeTeam,
                            size = 22.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = match.homeTeam,
                            color = OneXBetNavyInk,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = match.homeScore.toString(),
                        color = OneXBetNavyInk,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogoAvatar(
                            logoUrl = match.awayLogoUrl,
                            teamName = match.awayTeam,
                            size = 22.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = match.awayTeam,
                            color = OneXBetNavyInk,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = match.awayScore.toString(),
                        color = OneXBetNavyInk,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${match.kickoffLabel} (MMT) • ဂိုးပေါင်း $goalText",
                    color = OneXBetRoyalBlue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Horizontally scrollable betting tiles: W1, သရေ, W2, ဂိုးပေါ်, ဂိုးအောက်
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("one_xbet_odds_row_${match.id}"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tiles, key = { it.pickKey }) { tile ->
                        val isSelected = selectedPickKey == tile.pickKey
                        Surface(
                            color = when {
                                isSelected -> OneXBetRoyalBlue
                                !bettable -> Color(0xFFF3F4F6)
                                else -> OneXBetTileBg
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) OneXBetRoyalBlue else Color(0xFFE0E8F0)
                            ),
                            modifier = Modifier
                                .widthIn(min = 96.dp)
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = bettable) {
                                    onSelect(tile.pickLabel, tile.oddsValue, tile.pickKey)
                                }
                                .testTag("match_${tile.pickKey}_${match.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tile.title,
                                    color = when {
                                        isSelected -> Color.White.copy(alpha = 0.92f)
                                        !bettable -> Color(0xFF9CA3AF)
                                        else -> OneXBetSlateInk
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = tile.oddsText,
                                    color = when {
                                        isSelected -> Color.White
                                        !bettable -> Color(0xFF9CA3AF)
                                        else -> OneXBetNavyInk
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val FiveFiveFiveDarkCanvas = Color(0xFF262626)
val FiveFiveFiveTopBarBg = Color(0xFF2B2B2B)
val FiveFiveFiveLeagueStripBg = Color(0xFF3A3A3A)
val FiveFiveFiveCellBg = Color(0xFF666666)
val FiveFiveFiveDividerBg = Color(0xFF2E2E2E)
val FiveFiveFiveYellowKyay = Color(0xFFFACC15)

@Composable
fun FiveFiveFiveLeagueHeaderStrip(leagueName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FiveFiveFiveLeagueStripBg)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = BoardGreenAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = leagueName,
            color = Color(0xFFE5E7EB),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FiveFiveFiveScreenshotCard(
    match: SportMatch,
    selectedPickKey: String?,
    onShowInfo: () -> Unit,
    onSelect: (String, Double, String) -> Unit,
    nowMs: Long = System.currentTimeMillis()
) {
    val bettable = remember(match.id, match.status, match.liveClock, match.kickoffLabel, match.kickoffEpochMs, nowMs / 10_000L) {
        MyanmarTimeUtil.isMatchBettable(match, nowMs)
    }
    val handicapInfo = remember(match.id, match.handicapLine, match.homeTeam, match.awayTeam) {
        KyayFormatter.formatHandicap(match)
    }
    val yellowHandicap = remember(handicapInfo.normalizedLine) {
        formatFiveFiveFiveYellowKyay(handicapInfo.normalizedLine)
    }
    val yellowGoal = remember(match.goalLine) {
        formatFiveFiveFiveYellowKyay(match.goalLine.ifBlank { "2.5" })
    }

    val homePick = "${match.homeTeam} (${handicapInfo.homeBadge}) vs ${match.awayTeam}"
    val awayPick = "${match.awayTeam} (${handicapInfo.awayBadge}) vs ${match.homeTeam}"
    val overPick = "${match.homeTeam}-${match.awayTeam} ဂိုးပေါ် (${match.goalLine})"
    val underPick = "${match.homeTeam}-${match.awayTeam} ဂိုးအောက် (${match.goalLine})"

    val homeSelected = selectedPickKey == "home"
    val awaySelected = selectedPickKey == "away"
    val overSelected = selectedPickKey == "over"
    val underSelected = selectedPickKey == "under"

    val formattedTime = remember(match.matchDate, match.kickoffLabel, match.liveClock, match.status, match.homeScore, match.awayScore, bettable) {
        val datePrefix = if (match.matchDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            val parts = match.matchDate.split("-")
            "${parts[2]}-${parts[1]}-${parts[0]} "
        } else ""
        val baseTime = match.kickoffLabel.trim()
        val timeText = if (datePrefix.isNotBlank() && !baseTime.contains(datePrefix.trim())) {
            "$datePrefix$baseTime"
        } else {
            baseTime
        }
        when {
            match.liveClock.isNotBlank() -> "ပွဲချိန် : $timeText • LIVE ${match.homeScore}-${match.awayScore} (${match.liveClock})"
            match.status == "finished" -> "ပွဲချိန် : $timeText • ပြီးဆုံး FT ${match.homeScore}-${match.awayScore}"
            !bettable -> "ပွဲချိန် : $timeText • ပိတ်ပြီး"
            else -> "ပွဲချိန် : $timeText"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FiveFiveFiveDarkCanvas)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShowInfo)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedTime,
                color = if (bettable) Color(0xFFD1D5DB) else Color(0xFFFCA5A5),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (match.liveClock.isNotBlank() || match.status == "finished") {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (match.status == "finished") "FT" else "LIVE",
                    color = if (match.status == "finished") Color(0xFF93C5FD) else BoardGreenAccent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Row 1: [ Home Team | Yellow Handicap | Away Team ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 50.dp)
                    .background(
                        when {
                            homeSelected -> BoardCellSelectedBg
                            !bettable -> Color(0xFF474747)
                            else -> FiveFiveFiveCellBg
                        }
                    )
                    .clickable(enabled = bettable) { onSelect(homePick, match.bodyOdds, "home") }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("match_home_${match.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.homeTeam,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (handicapInfo.favIsHome) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = yellowHandicap,
                        color = FiveFiveFiveYellowKyay,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            VerticalDivider(thickness = 1.dp, color = FiveFiveFiveDividerBg)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 50.dp)
                    .background(
                        when {
                            awaySelected -> BoardCellSelectedBg
                            !bettable -> Color(0xFF474747)
                            else -> FiveFiveFiveCellBg
                        }
                    )
                    .clickable(enabled = bettable) { onSelect(awayPick, match.bodyOdds, "away") }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("match_away_${match.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.awayTeam,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!handicapInfo.favIsHome) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = yellowHandicap,
                        color = FiveFiveFiveYellowKyay,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = FiveFiveFiveDividerBg)

        // Row 2: [ ဂိုးပေါ် | Yellow Goal | ဂိုးအောက် ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        when {
                            overSelected -> BoardCellSelectedBg
                            !bettable -> Color(0xFF474747)
                            else -> FiveFiveFiveCellBg
                        }
                    )
                    .clickable(enabled = bettable) { onSelect(overPick, match.goalOdds, "over") }
                    .testTag("match_over_${match.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ဂိုးပေါ်",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )
            }

            VerticalDivider(thickness = 1.dp, color = FiveFiveFiveDividerBg)

            Box(
                modifier = Modifier
                    .widthIn(min = 66.dp)
                    .fillMaxHeight()
                    .background(if (!bettable) Color(0xFF474747) else FiveFiveFiveCellBg)
                    .clickable(onClick = onShowInfo)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = yellowGoal,
                    color = FiveFiveFiveYellowKyay,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            VerticalDivider(thickness = 1.dp, color = FiveFiveFiveDividerBg)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        when {
                            underSelected -> BoardCellSelectedBg
                            !bettable -> Color(0xFF474747)
                            else -> FiveFiveFiveCellBg
                        }
                    )
                    .clickable(enabled = bettable) { onSelect(underPick, match.goalOdds, "under") }
                    .testTag("match_under_${match.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ဂိုးအောက်",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun KyayPillBadge(
    text: String,
    inverted: Boolean
) {
    Surface(
        color = if (inverted) Color.White else BoardGreenAccent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = if (inverted) PitchEmeraldDark else Color.White,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SportsBetBottomBar(
    isParlay: Boolean,
    selectedCount: Int,
    combinedMultiplier: Double,
    balanceKs: Long,
    potentialProfit: Long,
    parsedStake: Long,
    earliestKickoffMs: Long,
    scheduledScanAtMs: Long,
    stakeInput: String,
    onStakeChange: (String) -> Unit,
    canSubmit: Boolean,
    busy: Boolean,
    onSubmit: () -> Unit,
    stakeTestTag: String = "sports_stake_input",
    submitTestTag: String = "submit_sports_order_button",
    nowMs: Long = System.currentTimeMillis()
) {
    Surface(
        color = BoardTopBarBg,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (isParlay) "မောင်း: $selectedCount | " else "ရွေးချယ်: $selectedCount | "}x${String.format(Locale.US, "%.2f", combinedMultiplier)}",
                    color = ChampionshipGold,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedCount == 0) "လက်ကျန်: ${formatKs(balanceKs)}"
                    else "နိုင် +${formatKs(potentialProfit)} / ရှုံး -${formatKs(parsedStake)}",
                    color = if (selectedCount == 0) Color(0xFFD1D5DB) else Color(0xFF6EE7B7),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (earliestKickoffMs > 0L) {
                val expired = nowMs >= earliestKickoffMs
                Text(
                    text = if (expired) {
                        "ပွဲစတင်ပြီးဖြစ်၍ လောင်းကြေးတင်၍မရပါ"
                    } else {
                        "${formatKickoffCountdown(earliestKickoffMs, nowMs)} • စကန်ချိန် (MMT): ${formatEpochDateTime(scheduledScanAtMs)}"
                    },
                    color = if (expired) Color(0xFFFCA5A5) else Color(0xFF93C5FD),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (expired) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "လောင်းငွေ",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.5.dp, BoardGreenAccent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (stakeInput.isEmpty()) {
                        Text("500+", color = InkMuted, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = stakeInput,
                        onValueChange = { onStakeChange(it.filter { c -> c.isDigit() }.take(8)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = InkDark,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(PitchEmeraldDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(stakeTestTag)
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BoardGreenAccent,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF4B5563),
                        disabledContentColor = Color(0xFFD1D5DB)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag(submitTestTag)
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "လောင်းမည်",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { chipAmt ->
                    Surface(
                        color = if (parsedStake == chipAmt) BoardGreenAccent else Color(0xFF374151),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { onStakeChange(chipAmt.toString()) }
                            .testTag("stake_chip_$chipAmt")
                    ) {
                        Text(
                            text = "$chipAmt",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
                if (isParlay && selectedCount == 1) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "* ၂ ပွဲ+ ရွေးပါ",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun LeagueQuickSearchSuggestionsRow(
    query: String,
    leagueMatchCounts: Map<String, Int>,
    onPickLeague: (String) -> Unit,
    accentColor: Color = BoardGreenAccent
) {
    val cleanQuery = query.trim().lowercase(Locale.US)
    if (cleanQuery.isBlank()) return
    val matchedLeagues = remember(cleanQuery, leagueMatchCounts) {
        val base = FootballFeedService.DEFAULT_LEAGUE_OPTIONS
        val extras = leagueMatchCounts.keys.filter { lg -> base.none { it.equals(lg, ignoreCase = true) } }
        (base + extras)
            .filter { it.lowercase(Locale.US).contains(cleanQuery) }
            .sortedWith(
                compareByDescending<String> { lg ->
                    (leagueMatchCounts.entries.firstOrNull { it.key.equals(lg, ignoreCase = true) }?.value ?: 0) > 0
                }.thenBy { lg ->
                    FootballFeedService.leagueSortOrderIndex(lg)
                }.thenBy { it }
            )
    }
    if (matchedLeagues.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(matchedLeagues, key = { it }) { lg ->
            val cnt = leagueMatchCounts.entries.firstOrNull { it.key.equals(lg, ignoreCase = true) }?.value ?: 0
            Surface(
                color = if (cnt > 0) accentColor.copy(alpha = 0.16f) else Color(0xFFF3F4F6),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (cnt > 0) accentColor else Color(0xFFD1D5DB)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onPickLeague(lg) }
                    .testTag("search_suggest_league_${lg.take(20)}")
            ) {
                Text(
                    text = "$lg ($cnt)",
                    color = if (cnt > 0) PitchEmeraldDark else InkMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (cnt > 0) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun SportsLeagueFilterDialog(
    totalCount: Int,
    availableLeagues: List<String>,
    selectedLeague: String?,
    onSelectLeague: (String?) -> Unit,
    onDismiss: () -> Unit,
    selectedLeagues: Set<String>? = null,
    onSelectLeagues: ((Set<String>) -> Unit)? = null,
    confirmSaveLabel: String? = null,
    onConfirmSave: (() -> Unit)? = null
) {
    val orderedLeagues = remember { FootballFeedService.DEFAULT_LEAGUE_OPTIONS }
    val isMultiMode = onSelectLeagues != null
    val activeSelected = remember(selectedLeagues, selectedLeague, isMultiMode, orderedLeagues) {
        if (isMultiMode) {
            selectedLeagues ?: orderedLeagues.toSet()
        } else {
            if (selectedLeague == null) orderedLeagues.toSet() else setOf(selectedLeague)
        }
    }
    val allChecked = orderedLeagues.isNotEmpty() &&
        orderedLeagues.all { lg -> activeSelected.any { it.equals(lg, ignoreCase = true) } }

    var modalSearch by remember { mutableStateOf("") }
    val displayedLeagues = remember(orderedLeagues, modalSearch) {
        val q = modalSearch.trim().lowercase(Locale.US)
        if (q.isBlank()) orderedLeagues
        else orderedLeagues.filter { it.lowercase(Locale.US).contains(q) }
    }
    val modalBlue = Color(0xFF1E88E5)
    val isCustomerMode = confirmSaveLabel == null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${orderedLeagues.size} လိဂ်",
                    color = BoardGreenAccent,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (isMultiMode) {
                                if (allChecked) onSelectLeagues?.invoke(emptySet())
                                else onSelectLeagues?.invoke(orderedLeagues.toSet())
                            } else {
                                onSelectLeague(null)
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("league_filter_select_all")
                ) {
                    Text(
                        text = if (allChecked) "အကုန်ဖျက်" else "အကုန်ရွေး",
                        color = InkDark,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Checkbox(
                        checked = if (isMultiMode) allChecked else selectedLeague == null,
                        onCheckedChange = { checked ->
                            if (isMultiMode) {
                                if (checked) onSelectLeagues?.invoke(orderedLeagues.toSet())
                                else onSelectLeagues?.invoke(emptySet())
                            } else {
                                onSelectLeague(null)
                                onDismiss()
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = modalBlue,
                            uncheckedColor = InkMuted,
                            checkmarkColor = Color.White
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = modalSearch,
                    onValueChange = { modalSearch = it },
                    placeholder = {
                        Text(
                            text = "လိဂ်နာမည် ရှာရန်...",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = modalBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (modalSearch.isNotBlank()) {
                            IconButton(onClick = { modalSearch = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = InkMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedTextColor = InkDark,
                        unfocusedTextColor = InkDark,
                        focusedBorderColor = modalBlue,
                        unfocusedBorderColor = Color(0xFFD5DCD8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("league_modal_search_input")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(displayedLeagues, key = { it }) { lg ->
                        val checked = activeSelected.any { it.equals(lg, ignoreCase = true) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        if (isMultiMode) {
                                            if (isCustomerMode) {
                                                if (activeSelected.isEmpty()) {
                                                    // After "အကုန်ဖျက်", tapping checks this league so multi-check + "ပိတ်မည်" works like the video
                                                    onSelectLeagues?.invoke(setOf(lg))
                                                } else {
                                                    // Tapping a league name directly isolates that league and opens its matches immediately
                                                    onSelectLeagues?.invoke(setOf(lg))
                                                    onDismiss()
                                                }
                                            } else {
                                                val next = activeSelected.toMutableSet()
                                                val existing = next.firstOrNull { it.equals(lg, ignoreCase = true) }
                                                if (existing != null) next.remove(existing) else next.add(lg)
                                                onSelectLeagues?.invoke(next)
                                            }
                                        } else {
                                            onSelectLeague(if (checked && activeSelected.size == 1) null else lg)
                                            onDismiss()
                                        }
                                    }
                                    .padding(end = 8.dp)
                                    .testTag("league_row_${lg.take(24)}"),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = lg,
                                    color = InkDark,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isMultiMode) {
                                        val next = activeSelected.toMutableSet()
                                        val existing = next.firstOrNull { it.equals(lg, ignoreCase = true) }
                                        if (isChecked) {
                                            if (existing == null) next.add(lg)
                                        } else {
                                            if (existing != null) next.remove(existing)
                                        }
                                        onSelectLeagues?.invoke(next)
                                    } else {
                                        onSelectLeague(if (isChecked) lg else null)
                                        onDismiss()
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = modalBlue,
                                    uncheckedColor = InkMuted,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.testTag("league_checkbox_${lg.take(24)}")
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("league_filter_close")
            ) {
                Text(
                    text = "ပိတ်မည်",
                    color = Color(0xFF3B82F6),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            if (confirmSaveLabel != null && onConfirmSave != null) {
                Button(
                    onClick = onConfirmSave,
                    colors = ButtonDefaults.buttonColors(containerColor = BoardGreenAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("league_filter_save")
                ) {
                    Text(confirmSaveLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun AdminLeagueHideBarSection(
    selectedLeagues: List<String>,
    busy: Boolean,
    accentColor: Color = ChampionshipGold,
    testTagPrefix: String = "admin_leagues",
    onSaveAllowedLeagues: (List<String>) -> Unit
) {
    val allOptions = FootballFeedService.DEFAULT_LEAGUE_OPTIONS
    val totalLeagues = allOptions.size
    val validSaved = remember(selectedLeagues) {
        selectedLeagues.filter { lg -> allOptions.any { it.equals(lg, ignoreCase = true) } }
    }
    var expanded by remember { mutableStateOf(false) }
    var showPickerDialog by remember { mutableStateOf(false) }
    var workingSet by remember(validSaved) {
        mutableStateOf(
            if (validSaved.isEmpty()) allOptions.toSet()
            else validSaved.toSet()
        )
    }

    Surface(
        color = Color(0xFF14261E),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("${testTagPrefix}_hide_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ဖလား / လိဂ် ရွေးချယ်မှု ($totalLeagues လိဂ်)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (validSaved.isEmpty()) "လက်ရှိ: အားလုံး ($totalLeagues လိဂ်)"
                            else "လက်ရှိရွေးထား: ${validSaved.size} လိဂ်",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(enabled = !busy) {
                                workingSet = if (validSaved.isEmpty()) {
                                    allOptions.toSet()
                                } else {
                                    validSaved.toSet()
                                }
                                showPickerDialog = true
                            }
                            .testTag("${testTagPrefix}_open_modal")
                    ) {
                        Text(
                            text = "ရွေးမည်",
                            color = accentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Hide league summary" else "Show league summary",
                        tint = accentColor
                    )
                }
            }

            if (expanded) {
                Text(
                    text = if (validSaved.isEmpty()) {
                        "လိဂ်အားလုံး ($totalLeagues လိဂ်) ဖွင့်ထားပါသည်"
                    } else {
                        validSaved.joinToString(" • ")
                    },
                    color = Color(0xFFD1FAE5),
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        workingSet = if (validSaved.isEmpty()) {
                            allOptions.toSet()
                        } else {
                            validSaved.toSet()
                        }
                        showPickerDialog = true
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = InkDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("${testTagPrefix}_pick_button")
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("$totalLeagues လိဂ် စာရင်းဖွင့်၍ ရွေးချယ်မည်", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPickerDialog) {
        SportsLeagueFilterDialog(
            totalCount = totalLeagues,
            availableLeagues = allOptions,
            selectedLeague = null,
            onSelectLeague = {},
            onDismiss = { showPickerDialog = false },
            selectedLeagues = workingSet,
            onSelectLeagues = { workingSet = it },
            confirmSaveLabel = "သိမ်းဆည်းမည်",
            onConfirmSave = {
                val allCount = allOptions.size
                val toSave = if (workingSet.isEmpty() || workingSet.size >= allCount) {
                    emptyList()
                } else {
                    workingSet.toList()
                }
                showPickerDialog = false
                onSaveAllowedLeagues(toSave)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalendarDateBar(
    selectedDateIso: String,
    pinnedDateIso: String,
    busy: Boolean,
    accentColor: Color = ChampionshipGold,
    testTagPrefix: String = "admin_date",
    onSelectDate: (String) -> Unit,
    onPinDate: (String) -> Unit
) {
    var showCalendarDialog by remember { mutableStateOf(false) }
    val todayIso = remember { MyanmarTimeUtil.todayIsoDate() }
    val tomorrowIso = remember { MyanmarTimeUtil.formatMatchDateIso(MyanmarTimeUtil.nowMs() + 86_400_000L) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showCalendarDialog = true },
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.85f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("${testTagPrefix}_calendar_button")
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (selectedDateIso.isBlank()) "ပြက္ခဒိန် ရက်ရွေး (အားလုံး)"
                    else "ရက်စွဲ: $selectedDateIso",
                    color = accentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = { onPinDate(selectedDateIso) },
                enabled = !busy,
                border = BorderStroke(1.dp, if (pinnedDateIso == selectedDateIso) BoardGreenAccent else accentColor.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("${testTagPrefix}_pin_button")
            ) {
                Text(
                    text = if (selectedDateIso.isBlank()) "All Pin" else "Pin ရက်",
                    color = if (pinnedDateIso == selectedDateIso) BoardGreenAccent else accentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = selectedDateIso.isBlank(),
                    onClick = { onSelectDate("") },
                    label = { Text("အားလုံး") }
                )
            }
            item {
                FilterChip(
                    selected = selectedDateIso == todayIso,
                    onClick = { onSelectDate(todayIso) },
                    label = { Text("ယနေ့ ($todayIso)") }
                )
            }
            item {
                FilterChip(
                    selected = selectedDateIso == tomorrowIso,
                    onClick = { onSelectDate(tomorrowIso) },
                    label = { Text("မနက်ဖြန် ($tomorrowIso)") }
                )
            }
            if (selectedDateIso.isNotBlank() && selectedDateIso != todayIso && selectedDateIso != tomorrowIso) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = { showCalendarDialog = true },
                        label = { Text(selectedDateIso) }
                    )
                }
            }
        }
    }

    if (showCalendarDialog) {
        val initialUtcMs = remember(selectedDateIso) {
            val baseIso = selectedDateIso.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } ?: todayIso
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            runCatching { fmt.parse(baseIso)?.time }.getOrNull() ?: System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMs)
        DatePickerDialog(
            onDismissRequest = { showCalendarDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMs = datePickerState.selectedDateMillis
                        if (pickedMs != null) {
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onSelectDate(fmt.format(Date(pickedMs)))
                        }
                        showCalendarDialog = false
                    }
                ) {
                    Text("ရွေးချယ်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("ပိတ်မည်")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun ConfirmAutoSyncDialog(
    boardTitle: String,
    targetDateIso: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val dateLabel = targetDateIso?.takeIf { it.isNotBlank() } ?: "ယနေ့ & မနက်ဖြန်"
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Sync, contentDescription = null, tint = PitchEmeraldDark) },
        title = { Text("ပွဲစဉ်များ အလိုအလျောက်ထည့်မည်လား?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ဇယား: $boardTitle")
                Text("ဆွဲယူမည့်ရက်စွဲ: $dateLabel", fontWeight = FontWeight.SemiBold, color = PitchEmeraldDark)
                Text(
                    "ရှိပြီးသား ပွဲစဉ်များတွင် Admin ပြင်ဆင်ထားသော ကြေး/ဂိုးပေါင်း/Odds များကို မဖျက်ဘဲ ပွဲအသစ်နှင့် တိုက်ရိုက်ရလဒ်များကိုသာ ထည့်သွင်းအပ်ဒိတ်ပေးပါမည်။",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_auto_sync_button")
            ) {
                Text("အတည်ပြု ထည့်မည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("မထည့်ပါ")
            }
        }
    )
}

@Composable
fun BoardMatchInfoDialog(
    match: SportMatch,
    boardSourceLabel: String? = null,
    onDismiss: () -> Unit
) {
    val hInfo = KyayFormatter.formatHandicap(match)
    val gInfo = KyayFormatter.formatGoalLine(match.goalLine)
    val sourceKyay = when (match.boardSource) {
        "one_express" -> match.oneExpressKyay.ifBlank {
            KyayFormatter.formatOneExpressKyay(match.handicapLine, match.goalLine, match.bodyOdds, match.goalOdds)
        }
        "five_five_five" -> match.fiveFiveFiveKyay.ifBlank {
            KyayFormatter.formatFiveFiveFiveKyay(match.handicapLine, match.goalLine, match.bodyOdds, match.goalOdds)
        }
        else -> ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = PitchEmeraldDark) },
        title = { Text("${match.homeTeam} vs ${match.awayTeam}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("လိဂ်: ${match.league} • ${match.kickoffLabel} (MMT)", fontWeight = FontWeight.SemiBold)
                if (!boardSourceLabel.isNullOrBlank()) {
                    Text("စနစ်: $boardSourceLabel • လိုတရ APK ကြေး", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (match.boardSource == "one_express") {
                    Text(
                        text = "W1: ${KyayFormatter.formatOneXBetOdds(match.win1Odds)} • သရေ: ${KyayFormatter.formatOneXBetOdds(match.drawOdds)} • W2: ${KyayFormatter.formatOneXBetOdds(match.win2Odds)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        color = OneXBetRoyalBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ဂိုးပေါ် (${match.goalLine}): ${KyayFormatter.formatOneXBetOdds(match.overOdds)} • ဂိုးအောက် (${match.goalLine}): ${KyayFormatter.formatOneXBetOdds(match.underOdds)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color(0xFF047857)
                    )
                } else {
                    Text(
                        "အာရှကြေး: ${KyayFormatter.formatAsianMarketTag(match.handicapLine, match.goalLine)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color(0xFF047857),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("ဘော်ဒီကြေး: ${hInfo.summaryText}", color = PitchEmeraldDark)
                    Text(gInfo, color = Color(0xFF92400E))
                    if (sourceKyay.isNotBlank()) {
                        Text(sourceKyay, fontFamily = JetBrainsMonoFontFamily, color = Color(0xFF0369A1))
                    }
                    Text("Body Odds: x${match.bodyOdds} • Goal Odds: x${match.goalOdds}", fontFamily = JetBrainsMonoFontFamily)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("နားလည်ပါပြီ") }
        }
    )
}
