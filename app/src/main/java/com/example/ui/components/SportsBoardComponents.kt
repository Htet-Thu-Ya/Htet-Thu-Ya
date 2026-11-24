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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PitchEmeraldDark
import java.util.Locale

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

@Composable
fun SportsModeSwitchStrip(
    isParlay: Boolean,
    accentColor: Color = BoardGreenAccent,
    onSelectMode: (Boolean) -> Unit,
    testTagPrefix: String
) {
    Surface(
        color = BoardTopBarBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val singleSelected = !isParlay
            Surface(
                color = if (singleSelected) accentColor else Color(0xFF323D39),
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
                color = if (parlaySelected) accentColor else Color(0xFF323D39),
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
fun ScreenshotMatchRowCard(
    match: SportMatch,
    selectedPickKey: String?,
    onShowInfo: () -> Unit,
    onSelect: (String, Double, String) -> Unit
) {
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

    val kickoffDisplay = remember(match.kickoffLabel, match.liveClock, match.status, match.homeScore, match.awayScore) {
        val base = "ပွဲချိန် : ${match.kickoffLabel}"
        when {
            match.liveClock.isNotBlank() -> "$base • ${match.homeScore}-${match.awayScore} (${match.liveClock})"
            match.status == "finished" -> "$base •FT ${match.homeScore}-${match.awayScore}"
            else -> base
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
                .background(BoardHeaderDark)
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
                    .background(if (homeSelected) BoardCellSelectedBg else BoardCellDefaultBg)
                    .clickable { onSelect(homePick, match.bodyOdds, "home") }
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
                    .background(if (awaySelected) BoardCellSelectedBg else BoardCellDefaultBg)
                    .clickable { onSelect(awayPick, match.bodyOdds, "away") }
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
                    .background(if (overSelected) BoardCellSelectedBg else BoardCellDefaultBg)
                    .clickable { onSelect(overPick, match.goalOdds, "over") }
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
                    .background(if (underSelected) BoardCellSelectedBg else BoardCellDefaultBg)
                    .clickable { onSelect(underPick, match.goalOdds, "under") }
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
    submitTestTag: String = "submit_sports_order_button"
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
                Text(
                    text = "${formatKickoffCountdown(earliestKickoffMs)} • စကန်ချိန်: ${formatEpochDateTime(scheduledScanAtMs)}",
                    color = Color(0xFF93C5FD),
                    style = MaterialTheme.typography.labelSmall,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SportsLeagueFilterDialog(
    totalCount: Int,
    availableLeagues: List<String>,
    selectedLeague: String?,
    onSelectLeague: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("လိဂ် ရွေးချယ်ရန်") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedLeague == null,
                    onClick = {
                        onSelectLeague(null)
                        onDismiss()
                    },
                    label = { Text("အားလုံး ($totalCount)") }
                )
                availableLeagues.forEach { lg ->
                    FilterChip(
                        selected = selectedLeague == lg,
                        onClick = {
                            onSelectLeague(lg)
                            onDismiss()
                        },
                        label = { Text(lg) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်မည်")
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
                Text("လိဂ်: ${match.league} • ${match.kickoffLabel}", fontWeight = FontWeight.SemiBold)
                if (!boardSourceLabel.isNullOrBlank()) {
                    Text("စနစ်: $boardSourceLabel • လိုတရ APK ကြေး", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text("ဘော်ဒီကြေး: ${hInfo.summaryText}", color = PitchEmeraldDark)
                Text(gInfo, color = Color(0xFF92400E))
                if (sourceKyay.isNotBlank()) {
                    Text(sourceKyay, fontFamily = JetBrainsMonoFontFamily, color = Color(0xFF0369A1))
                }
                Text("Body Odds: x${match.bodyOdds} • Goal Odds: x${match.goalOdds}", fontFamily = JetBrainsMonoFontFamily)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("နားလည်ပါပြီ") }
        }
    )
}
