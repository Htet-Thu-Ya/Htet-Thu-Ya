package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.betting.KyayFormatter
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UiState
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.formatKs
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldDark
import java.util.Locale
import kotlin.math.roundToLong

private data class MatchPick(
    val matchId: String,
    val label: String,
    val odds: Double,
    val pickKey: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SportsOrderScreen(
    isParlay: Boolean,
    matchesState: UiState<List<SportMatch>>,
    balanceKs: Long,
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

    val openMatches = when (matchesState) {
        is UiState.Success -> matchesState.data.filter { it.status == "open" }
        else -> emptyList()
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
    val canSubmit = !busy &&
        selectedPicks.size >= minSelections &&
        parsedStake in 100L..balanceKs &&
        parsedStake <= 10_000_000L

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            SubScreenTopBar(
                title = if (isParlay) "မောင်း (Parlay)" else "ဘော်ဒီ / ဂိုးပေါင်း",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                color = DeepPitchSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ရွေးချယ်မှု (${selectedPicks.size}) • x${String.format(Locale.US, "%.2f", combinedMultiplier)}",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "လက်ကျန်: ${formatKs(balanceKs)}",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // 3D-style Outcome Preview (Total Stake, If Win, If Lose)
                    Surface(
                        color = DeepPitchElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sports_outcome_preview_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ထိုးငွေစုစုပေါင်း", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = formatKs(parsedStake),
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("နိုင်ရင် ရရှိမည်", color = Color(0xFF6EE7B7), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = if (selectedPicks.isEmpty()) "--" else "${formatKs(potentialGross)} (+${formatKs(potentialProfit)})",
                                    color = ChampionshipGold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("ရှုံးရင် ရှုံးမည်", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "-${formatKs(parsedStake)}",
                                    color = Color(0xFFFCA5A5),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { chipAmt ->
                            FilterChip(
                                selected = parsedStake == chipAmt,
                                onClick = { stakeInput = chipAmt.toString() },
                                label = { Text("${chipAmt} Ks") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PitchEmeraldDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stakeInput,
                            onValueChange = { stakeInput = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("ထိုးကြေး (Ks)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChampionshipGold,
                                unfocusedBorderColor = Color(0xFF395649),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sports_stake_input")
                        )

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
                                containerColor = ChampionshipGold,
                                contentColor = InkDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("submit_sports_order_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = InkDark)
                            } else {
                                val pot = (parsedStake * combinedMultiplier).roundToLong()
                                Text(
                                    text = "တင်မည်\n(${formatKs(pot)})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (isParlay && selectedPicks.size == 1) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "* မောင်းအတွက် အနည်းဆုံး ၂ ပွဲ ရွေးချယ်ပါ",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        CircularProgressIndicator(color = ChampionshipGold)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(matchesState.message, color = Color(0xFFFCA5A5))
                    }
                }
                is UiState.Success -> {
                    if (openMatches.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "လက်ရှိ ဖွင့်ထားသော ပွဲစဉ် မရှိသေးပါ",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Admin မှ ပွဲစဉ်အသစ်များ ထည့်သွင်းပေးပါက ဤနေရာတွင် ချက်ချင်း ပေါ်လာပါမည်။",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 600.dp),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(openMatches, key = { it.id }) { match ->
                                val currentPick = selectedPicks[match.id]
                                MatchBoardCard(
                                    match = match,
                                    selectedLabel = currentPick?.label,
                                    onSelect = { label, odds, pickKey ->
                                        if (currentPick?.label == label) {
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

@Composable
private fun MatchBoardCard(
    match: SportMatch,
    selectedLabel: String?,
    onSelect: (String, Double, String) -> Unit
) {
    val handicapInfo = remember(match.id, match.handicapLine, match.homeTeam, match.awayTeam) {
        KyayFormatter.formatHandicap(match)
    }
    val goalExplanation = remember(match.id, match.goalLine) {
        KyayFormatter.formatGoalLine(match.goalLine)
    }

    val homePick = "${match.homeTeam} (${handicapInfo.homeBadge}) vs ${match.awayTeam}"
    val awayPick = "${match.awayTeam} (${handicapInfo.awayBadge}) vs ${match.homeTeam}"
    val overPick = "${match.homeTeam}-${match.awayTeam} ဂိုးပေါင်း Over (${match.goalLine})"
    val underPick = "${match.homeTeam}-${match.awayTeam} ဂိုးပေါင်း Under (${match.goalLine})"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PearlCard),
        border = BorderStroke(1.dp, PearlCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(match.league, color = PitchEmeraldDark, style = MaterialTheme.typography.labelLarge)
                Text(match.kickoffLabel, color = InkMuted, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${match.homeTeam}  vs  ${match.awayTeam}",
                color = InkDark,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Surface(
                color = Color(0xFFE8F5EE),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ဘော်ဒီ: ${handicapInfo.summaryText}",
                    color = PitchEmeraldDark,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickOptionChip(
                    title = "${match.homeTeam} • @ ${match.bodyOdds}",
                    subtitle = handicapInfo.homeBadge,
                    selected = selectedLabel == homePick,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(homePick, match.bodyOdds, "home") }
                )
                PickOptionChip(
                    title = "${match.awayTeam} • @ ${match.bodyOdds}",
                    subtitle = handicapInfo.awayBadge,
                    selected = selectedLabel == awayPick,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(awayPick, match.bodyOdds, "away") }
                )
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color(0xFFFEF6E7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = goalExplanation,
                    color = Color(0xFF92400E),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickOptionChip(
                    title = "အပေါ် Over @ ${match.goalOdds}",
                    subtitle = "> ${match.goalLine} ဂိုး",
                    selected = selectedLabel == overPick,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(overPick, match.goalOdds, "over") }
                )
                PickOptionChip(
                    title = "အောက် Under @ ${match.goalOdds}",
                    subtitle = "< ${match.goalLine} ဂိုး",
                    selected = selectedLabel == underPick,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(underPick, match.goalOdds, "under") }
                )
            }
        }
    }
}

@Composable
private fun PickOptionChip(
    title: String,
    subtitle: String = "",
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) PitchEmeraldDark else Color(0xFFEAF0EC),
        shape = RoundedCornerShape(10.dp),
        border = if (selected) BorderStroke(1.5.dp, ChampionshipGold) else null,
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = if (selected) Color.White else InkDark,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = if (selected) ChampionshipGold else InkMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreeDLotteryScreen(
    drawsState: UiState<List<LotteryDraw>>,
    balanceKs: Long,
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
        is UiState.Success -> drawsState.data.firstOrNull { it.status == "open" }
        else -> null
    }

    val numbersToBet = remember(digitsInput, includeReverse) {
        if (digitsInput.length != 3) emptyList()
        else if (!includeReverse) listOf(digitsInput)
        else generate3DPermutations(digitsInput)
    }

    val stakePerNumber = stakePerNumberInput.toLongOrNull() ?: 0L
    val totalStake = stakePerNumber * numbersToBet.size
    val drawMultiplier = openDraw?.payoutMultiplier ?: 550.0
    val canSubmit = !busy &&
        openDraw != null &&
        numbersToBet.isNotEmpty() &&
        stakePerNumber >= 100L &&
        totalStake in 100L..balanceKs &&
        totalStake <= 10_000_000L

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
                                // Effective multiplier across slip = (stakePerNumber * drawMultiplier) / totalStake
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
