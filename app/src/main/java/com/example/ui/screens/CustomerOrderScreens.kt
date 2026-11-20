package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.betting.KyayFormatter
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.TeamLogoAvatar
import com.example.ui.components.formatKs
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmeraldDark
import java.util.Locale
import kotlin.math.roundToLong

private data class MatchPick(
    val matchId: String,
    val label: String,
    val odds: Double,
    val pickKey: String
)

private val BoardTopBarBg = Color(0xFF232B28)
private val BoardHeaderDark = Color(0xFF4F5451)
private val BoardGreenAccent = Color(0xFF1DB954)
private val BoardCellSelectedBg = Color(0xFF16A34A)
private val BoardCellDefaultBg = Color(0xFFFDFDFD)
private val BoardBorderGray = Color(0xFFD5DCD8)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SportsOrderScreen(
    isParlay: Boolean,
    matchesState: UiState<List<SportMatch>>,
    balanceKs: Long,
    systemGameConfig: SystemGameConfig = SystemGameConfig(),
    busy: Boolean,
    onBack: () -> Unit,
    onSubmitOrder: (
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        pickKeys: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double
    ) -> Unit
) {
    val selectedPicks = remember { mutableStateMapOf<String, MatchPick>() }
    var stakeInput by remember { mutableStateOf("1000") }
    var selectedLeagueFilter by remember { mutableStateOf<String?>(null) }
    var showLeagueFilterDialog by remember { mutableStateOf(false) }
    var infoMatch by remember { mutableStateOf<SportMatch?>(null) }

    val openMatches = when (matchesState) {
        is UiState.Success -> matchesState.data.filter { it.status == "open" }
        else -> emptyList()
    }

    val availableLeagues = remember(openMatches) {
        openMatches.map { it.league.ifBlank { "Other" } }.distinct()
    }

    val filteredMatches = remember(openMatches, selectedLeagueFilter) {
        if (selectedLeagueFilter == null) openMatches
        else openMatches.filter { it.league == selectedLeagueFilter }
    }

    val groupedByLeague = remember(filteredMatches) {
        filteredMatches.groupBy { it.league.ifBlank { "Football" } }
    }

    val combinedMultiplier = remember(selectedPicks.values.toList()) {
        if (selectedPicks.isEmpty()) 1.0
        else selectedPicks.values.fold(1.0) { acc, pick -> acc * pick.odds }
            .coerceAtMost(1000.0)
    }

    val parsedStake = stakeInput.toLongOrNull() ?: 0L
    val potentialGross = if (selectedPicks.isEmpty()) 0L else (parsedStake * combinedMultiplier).roundToLong()
    val potentialProfit = (potentialGross - parsedStake).coerceAtLeast(0L)
    val minSelections = if (isParlay) 2 else 1
    val minAllowedStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxAllowedStake = systemGameConfig.maxStakeKs.coerceAtLeast(minAllowedStake)
    val canSubmit = !busy &&
        selectedPicks.size >= minSelections &&
        parsedStake in minAllowedStake..maxAllowedStake &&
        parsedStake <= balanceKs

    Scaffold(
        containerColor = Color(0xFFEBEFED),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isParlay) "မောင်း" else "ဘော်ဒီ/ဂိုးပေါင်း",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (selectedLeagueFilter != null) {
                        Surface(
                            color = BoardGreenAccent.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { selectedLeagueFilter = null }
                        ) {
                            Text(
                                text = selectedLeagueFilter ?: "",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(
                        onClick = { showLeagueFilterDialog = true },
                        modifier = Modifier.testTag("league_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "League Filter",
                            tint = if (selectedLeagueFilter != null) ChampionshipGold else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BoardTopBarBg,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
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
                    // Compact outcome strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (isParlay) "မောင်း: ${selectedPicks.size} | " else "ရွေးချယ်: ${selectedPicks.size} | "}x${String.format(Locale.US, "%.2f", combinedMultiplier)}",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedPicks.isEmpty()) "လက်ကျန်: ${formatKs(balanceKs)}"
                            else "နိုင် +${formatKs(potentialProfit)} / ရှုံး -${formatKs(parsedStake)}",
                            color = if (selectedPicks.isEmpty()) Color(0xFFD1D5DB) else Color(0xFF6EE7B7),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Screenshot-faithful bottom bar: လောင်းငွေ | White Box | Bright Green Pill
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
                                onValueChange = { stakeInput = it.filter { c -> c.isDigit() }.take(8) },
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
                                    .testTag("sports_stake_input")
                            )
                        }

                        Button(
                            onClick = {
                                val ordered = selectedPicks.values.toList()
                                onSubmitOrder(
                                    if (isParlay) "parlay" else "body_goal",
                                    ordered.map { it.label },
                                    ordered.map { it.matchId },
                                    ordered.map { it.pickKey },
                                    if (isParlay) "မောင်း (${ordered.size} ပွဲ)" else "ဘော်ဒီ/ဂိုးပေါင်း",
                                    parsedStake,
                                    combinedMultiplier
                                )
                            },
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
                                .height(46.dp)
                                .testTag("submit_sports_order_button")
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

                    // Quick stake chips + Parlay helper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { chipAmt ->
                            Surface(
                                color = if (parsedStake == chipAmt) BoardGreenAccent else Color(0xFF374151),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { stakeInput = chipAmt.toString() }
                            ) {
                                Text(
                                    text = "$chipAmt",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (isParlay && selectedPicks.size == 1) {
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (matchesState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BoardGreenAccent)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(matchesState.message, color = Color(0xFFB91C1C))
                    }
                }
                is UiState.Success -> {
                    if (filteredMatches.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = PitchEmeraldDark, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "လက်ရှိ ဖွင့်ထားသော ပွဲစဉ် မရှိသေးပါ",
                                color = InkDark,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Admin မှ ပွဲစဉ်အသစ်များ ထည့်သွင်းပေးပါက ဤနေရာတွင် ချက်ချင်း ပေါ်လာပါမည်။",
                                color = InkMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 620.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            groupedByLeague.forEach { (leagueName, leagueMatches) ->
                                item(key = "league_hdr_$leagueName") {
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

                                items(leagueMatches, key = { it.id }) { match ->
                                    val currentPick = selectedPicks[match.id]
                                    ScreenshotMatchRowCard(
                                        match = match,
                                        selectedPickKey = currentPick?.pickKey,
                                        onShowInfo = { infoMatch = match },
                                        onSelect = { label, odds, pickKey ->
                                            if (currentPick?.pickKey == pickKey) {
                                                selectedPicks.remove(match.id)
                                            } else {
                                                if (!isParlay) selectedPicks.clear()
                                                selectedPicks[match.id] = MatchPick(match.id, label, odds, pickKey)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLeagueFilterDialog) {
        AlertDialog(
            onDismissRequest = { showLeagueFilterDialog = false },
            title = { Text("လိဂ် ရွေးချယ်ရန်") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedLeagueFilter == null,
                        onClick = {
                            selectedLeagueFilter = null
                            showLeagueFilterDialog = false
                        },
                        label = { Text("အားလုံး (${openMatches.size})") }
                    )
                    availableLeagues.forEach { lg ->
                        FilterChip(
                            selected = selectedLeagueFilter == lg,
                            onClick = {
                                selectedLeagueFilter = lg
                                showLeagueFilterDialog = false
                            },
                            label = { Text(lg) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeagueFilterDialog = false }) {
                    Text("ပိတ်မည်")
                }
            }
        )
    }

    infoMatch?.let { m ->
        val hInfo = KyayFormatter.formatHandicap(m)
        val gInfo = KyayFormatter.formatGoalLine(m.goalLine)
        AlertDialog(
            onDismissRequest = { infoMatch = null },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = PitchEmeraldDark) },
            title = { Text("${m.homeTeam} vs ${m.awayTeam}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("လိဂ်: ${m.league} • ${m.kickoffLabel}", fontWeight = FontWeight.SemiBold)
                    Text("ဘော်ဒီကြေး: ${hInfo.summaryText}", color = PitchEmeraldDark)
                    Text(gInfo, color = Color(0xFF92400E))
                    Text("Body Odds: x${m.bodyOdds} • Goal Odds: x${m.goalOdds}", fontFamily = JetBrainsMonoFontFamily)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoMatch = null }) { Text("နားလည်ပါပြီ") }
            }
        )
    }
}

@Composable
private fun ScreenshotMatchRowCard(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        // Dark gray kickoff strip matching screenshots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BoardHeaderDark)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ပွဲချိန် : ${match.kickoffLabel}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "ကြေးအသေးစိတ်",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onShowInfo)
            )
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
                .height(44.dp)
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
private fun KyayPillBadge(
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

private fun compactKyayText(normalizedLine: String): String {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreeDLotteryScreen(
    drawsState: UiState<List<LotteryDraw>>,
    balanceKs: Long,
    systemGameConfig: SystemGameConfig = SystemGameConfig(),
    busy: Boolean,
    onBack: () -> Unit,
    onSubmit3DOrder: (
        selections: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double
    ) -> Unit
) {
    var digitsInput by remember { mutableStateOf("") }
    var includeReverse by remember { mutableStateOf(false) }
    var stakePerNumberInput by remember { mutableStateOf("500") }

    val openDraw = when (drawsState) {
        is UiState.Success -> drawsState.data.firstOrNull { it.status == "open" && it.drawType != "2d" }
        else -> null
    }

    val numbersToBet = remember(digitsInput, includeReverse) {
        if (digitsInput.length != 3) emptyList()
        else if (!includeReverse) listOf(digitsInput)
        else generate3DPermutations(digitsInput)
    }

    val stakePerNumber = stakePerNumberInput.toLongOrNull() ?: 0L
    val totalStake = stakePerNumber * numbersToBet.size
    val drawMultiplier = openDraw?.payoutMultiplier ?: systemGameConfig.threeDPayoutMultiplier
    val minAllowedStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxAllowedStake = systemGameConfig.maxStakeKs.coerceAtLeast(minAllowedStake)
    val canSubmit = !busy &&
        openDraw != null &&
        numbersToBet.isNotEmpty() &&
        stakePerNumber >= 100L &&
        totalStake in minAllowedStake..maxAllowedStake &&
        totalStake <= balanceKs

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            SubScreenTopBar(title = "သုံးလုံးထီ (3D)", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Casino, contentDescription = null, tint = ChampionshipGold)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = openDraw?.roundTitle ?: "လက်ရှိ ၃လုံးထီ အကြိမ် မဖွင့်သေးပါ",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (openDraw != null) {
                                Surface(color = PitchEmeraldDark, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "${openDraw.payoutMultiplier.toInt()}x",
                                        color = Color.White,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (openDraw != null) "ပိတ်ချိန်: ${openDraw.closeTimeLabel} • လက်ကျန်: ${formatKs(balanceKs)}"
                            else "Admin မှ ၃လုံးထီ အကြိမ်သစ် ဖွင့်လှစ်ပေးရန် စောင့်ဆိုင်းပါ။",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ဂဏန်း ၃ လုံး ထည့်သွင်းပါ (000 - 999)",
                            color = InkDark,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = digitsInput,
                            onValueChange = { digitsInput = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("3D ဂဏန်း") },
                            placeholder = { Text("ဥပမာ 555") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark,
                                focusedBorderColor = PitchEmeraldDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("three_d_number_input")
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { includeReverse = !includeReverse }
                        ) {
                            Checkbox(
                                checked = includeReverse,
                                onCheckedChange = { includeReverse = it }
                            )
                            Text("R (အကွက်ပတ် အားလုံးတွဲထိုးမည်)", color = InkDark)
                        }

                        if (numbersToBet.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                numbersToBet.forEach { n ->
                                    Surface(color = PitchEmeraldDark, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = n,
                                            color = Color.White,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("တစ်ကွက် ထိုးကြေး (Ks)", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100L, 500L, 1000L, 3000L, 5000L).forEach { amt ->
                                FilterChip(
                                    selected = stakePerNumber == amt,
                                    onClick = { stakePerNumberInput = amt.toString() },
                                    label = { Text("$amt") }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = stakePerNumberInput,
                            onValueChange = { stakePerNumberInput = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("တစ်ကွက် ထိုးကြေး") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("three_d_stake_input")
                        )

                        Spacer(Modifier.height(14.dp))
                        val winGross = (stakePerNumber * drawMultiplier).roundToLong()
                        val winNet = (winGross - totalStake).coerceAtLeast(0L)
                        Surface(
                            color = Color(0xFFF3F7F4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PearlCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("လောင်းကြေးငွေစုစုပေါင်း:", color = InkMuted)
                                    Text(
                                        formatKs(totalStake),
                                        color = InkDark,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("နိုင်ရင် ရရှိမည် (၁ ကွက်ပေါက်):", color = PitchEmeraldDark)
                                    Text(
                                        "${formatKs(winGross)} (အမြတ် +${formatKs(winNet)})",
                                        color = PitchEmeraldDark,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ရှုံးရင် ရှုံးမည်:", color = Color(0xFFB91C1C))
                                    Text(
                                        "-${formatKs(totalStake)}",
                                        color = Color(0xFFB91C1C),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val round = openDraw?.roundTitle ?: "3D"
                                val selections = numbersToBet.map { num ->
                                    "3D: $num (${formatKs(stakePerNumber)})"
                                }
                                val effectiveMult = if (numbersToBet.isEmpty()) drawMultiplier
                                else (drawMultiplier / numbersToBet.size.toDouble()).coerceAtLeast(1.0)
                                onSubmit3DOrder(selections, round, totalStake, effectiveMult)
                            },
                            enabled = canSubmit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PitchEmeraldDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_three_d_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("၃လုံးထီ အော်ဒါတင်မည်", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generate3DPermutations(num: String): List<String> {
    if (num.length != 3) return emptyList()
    val chars = num.toCharArray()
    val set = linkedSetOf<String>()
    for (i in 0..2) {
        for (j in 0..2) {
            for (k in 0..2) {
                if (i != j && j != k && i != k) {
                    set.add("${chars[i]}${chars[j]}${chars[k]}")
                }
            }
        }
    }
    return set.toList()
}
