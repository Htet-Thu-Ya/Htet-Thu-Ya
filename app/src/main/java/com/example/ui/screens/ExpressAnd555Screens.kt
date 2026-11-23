package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.model.BoardConfig
import com.example.data.model.SportMatch
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.PauseStatusBannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.TeamLogoAvatar
import com.example.ui.components.buildOrderKickoffSummaryText
import com.example.ui.components.formatEpochDateTime
import com.example.ui.components.formatKickoffCountdown
import com.example.ui.components.formatKs
import com.example.ui.components.parseMatchKickoffEpochMs
import com.example.ui.theme.AmberPending
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
import com.example.ui.theme.SkyInfo
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpressOddsOrderScreen(
    boardMode: String, // "one_express" or "five_five_five"
    matchesState: UiState<List<SportMatch>>,
    balanceKs: Long,
    systemGameConfig: SystemGameConfig,
    boardConfig: BoardConfig,
    busy: Boolean,
    onBack: () -> Unit,
    onSubmitOrder: (
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        stakeKs: Long,
        multiplier: Double,
        pickKeys: List<String>,
        matchKickoffMs: Long,
        matchKickoffText: String
    ) -> Unit
) {
    val isOneExpress = boardMode == "one_express"
    val screenTitle = if (isOneExpress) "One Express ဘောလုံးပွဲကြေး" else "555 ဘောလုံးပွဲကြေး"
    val accentColor = if (isOneExpress) Color(0xFF38BDF8) else ChampionshipGold
    val boardTag = if (isOneExpress) "One Express Auto-Kyay" else "555 Auto-Kyay"

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val isFootballPaused = systemGameConfig.maintenanceMode || systemGameConfig.isFootballPausedNow(nowMs)
    val selectedPicks = remember(boardMode) { mutableStateMapOf<String, String>() }
    var stakeInput by remember(boardMode) { mutableStateOf("1000") }
    var showBoardList by remember { mutableStateOf(true) }
    var showSlipSummary by remember { mutableStateOf(true) }

    val boardMatches = when (matchesState) {
        is UiState.Success -> {
            matchesState.data.filter { m ->
                m.visibility == "public" && m.boardSource == boardMode
            }
        }
        else -> emptyList()
    }
    val openMatches = boardMatches.filter { it.status == "open" }

    val pickedPairs = openMatches.mapNotNull { m ->
        val side = selectedPicks[m.id] ?: return@mapNotNull null
        m to side
    }

    val combinedMult = if (pickedPairs.isEmpty()) 1.0 else {
        if (pickedPairs.size == 1) {
            val (m, side) = pickedPairs.first()
            if (side == "home" || side == "away") m.bodyOdds else m.goalOdds
        } else {
            pickedPairs.fold(1.0) { acc, (m, side) ->
                val legOdds = if (side == "home" || side == "away") m.bodyOdds else m.goalOdds
                acc * legOdds.coerceIn(1.05, 50.0)
            }.coerceIn(1.0, 1000.0)
        }
    }

    val stakeKs = stakeInput.toLongOrNull() ?: 0L
    val potentialPayout = (stakeKs * combinedMult).roundToLong()
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)
    val canSubmit = !busy &&
        !isFootballPaused &&
        pickedPairs.isNotEmpty() &&
        stakeKs in minStake..maxStake &&
        stakeKs <= balanceKs

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            SubScreenTopBar(title = screenTitle, onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isFootballPaused) {
                item {
                    PauseStatusBannerCard(
                        title = if (systemGameConfig.maintenanceMode) {
                            systemGameConfig.maintenanceMessage.ifBlank { "စနစ်ပြုပြင်နေပါသည်" }
                        } else {
                            "ဘောလုံးပွဲ လောင်းကြေးတင်ခွင့် ခေတ္တပိတ်ထားပါသည်"
                        },
                        reopenAtMs = if (systemGameConfig.maintenanceMode) 0L else systemGameConfig.footballReopenAtMs,
                        nowMs = nowMs
                    )
                }
            }

            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOneExpress) Icons.Default.Bolt else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = boardTag,
                                    color = accentColor,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = accentColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                val syncLabel = if (
                                    (isOneExpress && boardConfig.autoSyncOneExpress) ||
                                    (!isOneExpress && boardConfig.autoSyncFiveFiveFive)
                                ) "Auto ON" else "Admin Board"
                                Text(
                                    text = syncLabel,
                                    color = accentColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "လက်ကျန်ငွေ: ${formatKs(balanceKs)} • ရွေးချယ်ထားသောပွဲ: ${pickedPairs.size} ပွဲ",
                            color = Color(0xFFCDE2D6),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isOneExpress) {
                                "One Express ဘောလုံးပွဲထွက်ကြေးအတိုင်း တစ်ပွဲချင်း သို့မဟုတ် မောင်းတွဲလောင်းနိုင်ပါသည်။"
                            } else {
                                "555 ဘောလုံးပွဲထွက်ကြေးအတိုင်း တစ်ပွဲချင်း သို့မဟုတ် မောင်းတွဲလောင်းနိုင်ပါသည်။"
                            },
                            color = Color(0xFF9AB4A6),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Hide Bar for Express/555 Match List
            item {
                HideBarSectionHeader(
                    title = if (isOneExpress) "One Express ပွဲစဉ်ဇယား" else "555 ပွဲစဉ်ဇယား",
                    countBadge = "${openMatches.size} ပွဲဖွင့်",
                    isVisible = showBoardList,
                    onToggle = { showBoardList = !showBoardList },
                    testTag = "hide_bar_express_${boardMode}_list",
                    accentColor = accentColor,
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            if (showBoardList) {
                when (matchesState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = accentColor)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item { EmptyStateCard(message = matchesState.message) }
                    }
                    is UiState.Success -> {
                        if (boardMatches.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    message = "Admin မှ $screenTitle ပွဲစဉ်များ ထည့်သွင်း/Auto-Sync လုပ်ပေးရန် စောင့်ဆိုင်းပါ။"
                                )
                            }
                        } else {
                            items(boardMatches, key = { it.id }) { match ->
                                val kyayLabel = if (isOneExpress) {
                                    match.oneExpressKyay.ifBlank {
                                        KyayFormatter.formatOneExpressKyay(match.handicapLine, match.goalLine)
                                    }
                                } else {
                                    match.fiveFiveFiveKyay.ifBlank {
                                        KyayFormatter.formatFiveFiveFiveKyay(match.handicapLine, match.goalLine)
                                    }
                                }
                                val currentSide = selectedPicks[match.id]
                                val kickoffEpoch = parseMatchKickoffEpochMs(match)
                                val kickoffCountdown = formatKickoffCountdown(kickoffEpoch, nowMs)
                                val isSelectable = match.status == "open" && !isFootballPaused

                                Card(
                                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, PearlCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = match.league + if (match.liveClock.isNotBlank()) " • ${match.liveClock}" else "",
                                                color = InkMuted,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            StatusBadge(status = match.status)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                TeamLogoAvatar(match.homeLogoUrl, match.homeTeam, size = 28.dp)
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = match.homeTeam,
                                                    color = InkDark,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = if (match.status == "finished" || match.liveClock.isNotBlank()) {
                                                    "${match.homeScore} - ${match.awayScore}"
                                                } else "VS",
                                                color = PitchEmeraldDark,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = match.awayTeam,
                                                    color = InkDark,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                TeamLogoAvatar(match.awayLogoUrl, match.awayTeam, size = 28.dp)
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = PitchEmeraldDark,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = match.kickoffLabel + if (kickoffCountdown.isNotBlank()) " • $kickoffCountdown" else "",
                                                    color = InkMuted,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            color = if (isOneExpress) Color(0xFFE0F2FE) else Color(0xFFFEF3C7),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = kyayLabel,
                                                color = if (isOneExpress) Color(0xFF0369A1) else Color(0xFF92400E),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        if (match.status == "open") {
                                            Spacer(Modifier.height(8.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val sideOptions = listOf(
                                                    "home" to "Home • ${match.bodyOdds}",
                                                    "away" to "Away • ${match.bodyOdds}",
                                                    "over" to "Over (${match.goalLine}) • ${match.goalOdds}",
                                                    "under" to "Under (${match.goalLine}) • ${match.goalOdds}"
                                                )
                                                sideOptions.forEach { (sideKey, sideLabel) ->
                                                    FilterChip(
                                                        selected = currentSide == sideKey,
                                                        onClick = {
                                                            if (isSelectable) {
                                                                if (currentSide == sideKey) selectedPicks.remove(match.id)
                                                                else selectedPicks[match.id] = sideKey
                                                            }
                                                        },
                                                        enabled = isSelectable,
                                                        label = { Text(sideLabel) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Slip Summary + Hide Bar
            item {
                HideBarSectionHeader(
                    title = "လောင်းကြေးတင်ရန် စာရင်းချုပ်",
                    countBadge = if (pickedPairs.isEmpty()) "ပွဲမရွေးရသေး" else "${pickedPairs.size} ပွဲ",
                    isVisible = showSlipSummary,
                    onToggle = { showSlipSummary = !showSlipSummary },
                    testTag = "hide_bar_express_${boardMode}_slip",
                    accentColor = accentColor,
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            if (showSlipSummary) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ထိုးကြေး (Ks) ထည့်သွင်းပါ (${formatKs(minStake)} - ${formatKs(maxStake)})",
                                color = InkDark,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1000L, 3000L, 5000L, 10000L, 30000L).forEach { amt ->
                                    FilterChip(
                                        selected = stakeKs == amt,
                                        onClick = { stakeInput = amt.toString() },
                                        label = { Text("$amt") }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = stakeInput,
                                onValueChange = { stakeInput = it.filter { c -> c.isDigit() }.take(8) },
                                label = { Text("ထိုးကြေးပမာဏ (Ks)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = InkDark,
                                    unfocusedTextColor = InkDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("express_${boardMode}_stake_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("စုစုပေါင်းအဆ:", color = InkMuted)
                                Text(
                                    text = "x${String.format("%.2f", combinedMult)}",
                                    color = PitchEmeraldDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("နိုင်ရင် ရရှိမည့်ငွေ:", color = PitchEmeraldDark)
                                Text(
                                    text = formatKs(potentialPayout),
                                    color = PitchEmeraldDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val pickedMatchObjs = pickedPairs.map { it.first }
                                    val selections = pickedPairs.map { (m, side) ->
                                        val kyayTag = if (isOneExpress) {
                                            m.oneExpressKyay.ifBlank { KyayFormatter.formatOneExpressKyay(m.handicapLine, m.goalLine) }
                                        } else {
                                            m.fiveFiveFiveKyay.ifBlank { KyayFormatter.formatFiveFiveFiveKyay(m.handicapLine, m.goalLine) }
                                        }
                                        val pickText = when (side) {
                                            "home" -> "${m.homeTeam} (${m.handicapLine}) • $kyayTag"
                                            "away" -> "${m.awayTeam} vs ${m.homeTeam} (${m.handicapLine}) • $kyayTag"
                                            "over" -> "${m.homeTeam}-${m.awayTeam} Over ${m.goalLine} • $kyayTag"
                                            else -> "${m.homeTeam}-${m.awayTeam} Under ${m.goalLine} • $kyayTag"
                                        }
                                        pickText.take(120)
                                    }
                                    val matchIds = pickedMatchObjs.map { it.id }
                                    val pickKeys = pickedPairs.map { (m, side) -> "${m.id}:$side" }
                                    val kickoffMs = pickedMatchObjs.mapNotNull { m ->
                                        parseMatchKickoffEpochMs(m).takeIf { it > 0L }
                                    }.minOrNull() ?: 0L
                                    val kickoffText = buildOrderKickoffSummaryText(pickedMatchObjs)
                                    onSubmitOrder(
                                        boardMode,
                                        selections,
                                        matchIds,
                                        stakeKs,
                                        combinedMult,
                                        pickKeys,
                                        kickoffMs,
                                        kickoffText
                                    )
                                    selectedPicks.clear()
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
                                    .testTag("submit_${boardMode}_order_button")
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text(
                                        text = if (isOneExpress) "One Express ကြေး အော်ဒါတင်မည်" else "555 ကြေး အော်ဒါတင်မည်",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminExpressBoardTab(
    boardMode: String, // "one_express" or "five_five_five"
    matches: List<SportMatch>,
    boardConfig: BoardConfig,
    busy: Boolean,
    onSyncNow: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSaveNewMatch: (
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double,
        goalOdds: Double,
        boardSource: String,
        oneExpressKyay: String,
        fiveFiveFiveKyay: String
    ) -> Unit,
    onUpdateMatchDetails: (
        matchId: String,
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double,
        goalOdds: Double,
        status: String,
        homeScore: Long,
        awayScore: Long,
        oneExpressKyay: String?,
        fiveFiveFiveKyay: String?
    ) -> Unit,
    onDeleteMatch: (String) -> Unit
) {
    val isOneExpress = boardMode == "one_express"
    val title = if (isOneExpress) "One Express ဘောလုံးပွဲကြေး စီမံခန့်ခွဲမှု" else "555 ဘောလုံးပွဲကြေး စီမံခန့်ခွဲမှု"
    val accent = if (isOneExpress) Color(0xFF38BDF8) else ChampionshipGold
    val autoEnabled = if (isOneExpress) boardConfig.autoSyncOneExpress else boardConfig.autoSyncFiveFiveFive
    val filteredMatches = remember(matches, boardMode) {
        matches.filter { it.boardSource == boardMode }
    }

    var showSyncCard by remember { mutableStateOf(true) }
    var showMatchesTable by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMatch by remember { mutableStateOf<SportMatch?>(null) }
    var deleteConfirmMatch by remember { mutableStateOf<SportMatch?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HideBarSectionHeader(
                title = title,
                countBadge = if (autoEnabled) "Auto ON" else "Manual",
                isVisible = showSyncCard,
                onToggle = { showSyncCard = !showSyncCard },
                testTag = "hide_bar_admin_${boardMode}_controls",
                accentColor = accent,
                modifier = Modifier.widthIn(max = 720.dp)
            )
        }

        if (showSyncCard) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isOneExpress) "One Express အလိုအလျောက် ပွဲကြေးထည့်စနစ်" else "555 အလိုအလျောက် ပွဲကြေးထည့်စနစ်",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "နေ့စဉ်ပွဲစဉ်များကို အလိုအလျောက်ဆွဲယူပြီး ကြေးသတ်မှတ်ပေးပါမည်",
                                    color = Color(0xFFB4C8BE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = autoEnabled,
                                onCheckedChange = onToggleAutoSync,
                                modifier = Modifier.testTag("toggle_auto_sync_$boardMode")
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSyncNow,
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                                modifier = Modifier.weight(1f).testTag("sync_now_$boardMode")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Auto-Sync ဆွဲမည်", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).testTag("add_match_$boardMode")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = ChampionshipGold)
                                Spacer(Modifier.width(6.dp))
                                Text("ပွဲသစ်ထည့်မည်", color = ChampionshipGold)
                            }
                        }
                    }
                }
            }
        }

        item {
            HideBarSectionHeader(
                title = if (isOneExpress) "One Express ပွဲစဉ်များ ဇယား" else "555 ပွဲစဉ်များ ဇယား",
                countBadge = "${filteredMatches.size} ပွဲ",
                isVisible = showMatchesTable,
                onToggle = { showMatchesTable = !showMatchesTable },
                testTag = "hide_bar_admin_${boardMode}_table",
                accentColor = accent,
                modifier = Modifier.widthIn(max = 720.dp)
            )
        }

        if (showMatchesTable) {
            if (filteredMatches.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "$title တွင် ပွဲစဉ်မရှိသေးပါ — 'Auto-Sync ဆွဲမည်' သို့မဟုတ် 'ပွဲသစ်ထည့်မည်' ကို နှိပ်ပါ။"
                    )
                }
            } else {
                items(filteredMatches, key = { it.id }) { m ->
                    val kyay = if (isOneExpress) {
                        m.oneExpressKyay.ifBlank { KyayFormatter.formatOneExpressKyay(m.handicapLine, m.goalLine) }
                    } else {
                        m.fiveFiveFiveKyay.ifBlank { KyayFormatter.formatFiveFiveFiveKyay(m.handicapLine, m.goalLine) }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${m.league} • ${m.kickoffLabel}",
                                    color = InkMuted,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(status = m.status)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${m.homeTeam} vs ${m.awayTeam} (${m.homeScore}-${m.awayScore})",
                                color = InkDark,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = kyay,
                                color = PitchEmeraldDark,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Body: ${m.handicapLine} (${m.bodyOdds}x) • Goal: ${m.goalLine} (${m.goalOdds}x)" +
                                    if (m.autoUpdatedAtMs > 0L) " • Update: ${formatEpochDateTime(m.autoUpdatedAtMs)}" else "",
                                color = InkMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { editingMatch = m },
                                    modifier = Modifier.testTag("edit_express_match_${m.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ပြုပြင် / ရလဒ်ထည့်")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMatchDialog(
            boardMode = boardMode,
            onDismiss = { showAddDialog = false },
            onSave = { league, home, away, kickoff, handicap, goalLine, bodyOdds, goalOdds, oneExpKyay, fffKyay ->
                onSaveNewMatch(
                    league, home, away, kickoff, handicap, goalLine, bodyOdds, goalOdds,
                    boardMode, oneExpKyay, fffKyay
                )
                showAddDialog = false
            }
        )
    }

    editingMatch?.let { match ->
        EditMatchResultDialog(
            match = match,
            boardMode = boardMode,
            onDismiss = { editingMatch = null },
            onUpdate = { league, home, away, kickoff, handicap, goalLine, bodyOdds, goalOdds, status, hScore, aScore, oneExpKyay, fffKyay ->
                onUpdateMatchDetails(
                    match.id, league, home, away, kickoff, handicap, goalLine, bodyOdds, goalOdds,
                    status, hScore, aScore, oneExpKyay, fffKyay
                )
                editingMatch = null
            },
            onRequestDelete = {
                deleteConfirmMatch = match
                editingMatch = null
            }
        )
    }

    deleteConfirmMatch?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmMatch = null },
            title = { Text("ပွဲစဉ် ဖျက်သိမ်းမည်လား?") },
            text = { Text("${target.homeTeam} vs ${target.awayTeam} ကို ဖျက်သိမ်းမည်ဖြစ်ပါသည်။") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMatch(target.id)
                        deleteConfirmMatch = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("ဖျက်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmMatch = null }) { Text("မဖျက်တော့ပါ") }
            }
        )
    }
}
