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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BoardConfig
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
import com.example.data.util.MyanmarTimeUtil
import com.example.ui.components.BoardGreenAccent
import com.example.ui.components.BoardMatchInfoDialog
import com.example.ui.components.BoardTopBarBg
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.LeagueHideBarHeader
import com.example.ui.components.MatchPick
import com.example.ui.components.PauseStatusBannerCard
import com.example.ui.components.ScreenshotMatchRowCard
import com.example.ui.components.SportsBetBottomBar
import com.example.ui.components.SportsLeagueFilterDialog
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.buildOrderKickoffSummaryText
import com.example.ui.components.formatKs
import com.example.ui.components.parseMatchKickoffEpochMs
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmeraldDark
import kotlinx.coroutines.delay
import kotlin.math.roundToLong



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SportsOrderScreen(
    isParlay: Boolean,
    matchesState: UiState<List<SportMatch>>,
    balanceKs: Long,
    systemGameConfig: SystemGameConfig = SystemGameConfig(),
    boardConfig: BoardConfig = BoardConfig(),
    busy: Boolean,
    onBack: () -> Unit,
    onSubmitOrder: (
        orderType: String,
        selections: List<String>,
        matchIds: List<String>,
        pickKeys: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        matchKickoffMs: Long,
        scanAtMs: Long,
        matchKickoffText: String
    ) -> Unit
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }
    val isFootballPaused = systemGameConfig.maintenanceMode || systemGameConfig.isFootballPausedNow(nowMs)

    val selectedPicks = remember { mutableStateMapOf<String, MatchPick>() }
    var stakeInput by remember { mutableStateOf("1000") }
    var selectedLeagueFilter by remember { mutableStateOf<String?>(null) }
    var showLeagueFilterDialog by remember { mutableStateOf(false) }
    var infoMatch by remember { mutableStateOf<SportMatch?>(null) }
    var showSportsTable by remember { mutableStateOf(true) }
    val expandedLeagues = remember { mutableStateMapOf<String, Boolean>() }
    val timeBucket = nowMs / 10_000L

    val openMatches = remember(matchesState, boardConfig.allowedLeagues, timeBucket) {
        val allOpen = when (matchesState) {
            is UiState.Success -> matchesState.data.filter {
                (it.boardSource.isBlank() || it.boardSource == "standard") &&
                    MyanmarTimeUtil.isMatchBettable(it, nowMs)
            }
            else -> emptyList()
        }
        val allowed = boardConfig.allowedLeagues.map { it.trim() }.filter { it.isNotBlank() }
        if (allowed.isEmpty()) allOpen
        else allOpen.filter { m ->
            allowed.any { lg -> m.league.equals(lg, ignoreCase = true) || m.league.contains(lg, ignoreCase = true) }
        }
    }

    LaunchedEffect(openMatches) {
        val validIds = openMatches.map { it.id }.toSet()
        val staleKeys = selectedPicks.keys.filter { it !in validIds }
        staleKeys.forEach { selectedPicks.remove(it) }
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
    val earliestKickoffMs = remember(selectedPicks.keys.toList(), openMatches) {
        val pickedIds = selectedPicks.keys
        openMatches.filter { it.id in pickedIds }
            .map { parseMatchKickoffEpochMs(it) }
            .filter { it > 0L }
            .minOrNull() ?: 0L
    }
    val scheduledScanAtMs = remember(earliestKickoffMs) {
        if (earliestKickoffMs > 0L) earliestKickoffMs + (135L * 60_000L) else 0L
    }
    val hasExpiredPick = earliestKickoffMs in 1L..nowMs
    val minSelections = if (isParlay) 2 else 1
    val minAllowedStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxAllowedStake = systemGameConfig.maxStakeKs.coerceAtLeast(minAllowedStake)
    val canSubmit = !busy &&
        !isFootballPaused &&
        !hasExpiredPick &&
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
            SportsBetBottomBar(
                isParlay = isParlay,
                selectedCount = selectedPicks.size,
                combinedMultiplier = combinedMultiplier,
                balanceKs = balanceKs,
                potentialProfit = potentialProfit,
                parsedStake = parsedStake,
                earliestKickoffMs = earliestKickoffMs,
                scheduledScanAtMs = scheduledScanAtMs,
                stakeInput = stakeInput,
                onStakeChange = { stakeInput = it },
                canSubmit = canSubmit,
                busy = busy,
                nowMs = nowMs,
                onSubmit = {
                    val ordered = selectedPicks.values.toList()
                    val pickedMatches = openMatches.filter { it.id in selectedPicks.keys }
                    val kickoffSummary = buildOrderKickoffSummaryText(pickedMatches)
                    onSubmitOrder(
                        if (isParlay) "parlay" else "body_goal",
                        ordered.map { it.label },
                        ordered.map { it.matchId },
                        ordered.map { it.pickKey },
                        if (isParlay) "မောင်း (${ordered.size} ပွဲ)" else "ဘော်ဒီ/ဂိုးပေါင်း",
                        parsedStake,
                        combinedMultiplier,
                        earliestKickoffMs,
                        scheduledScanAtMs,
                        kickoffSummary
                    )
                }
            )
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isFootballPaused) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 620.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                PauseStatusBannerCard(
                                    title = if (systemGameConfig.maintenanceMode) {
                                        systemGameConfig.maintenanceMessage.ifBlank { "စနစ်ပြုပြင်နေပါသည်" }
                                    } else {
                                        "ဘောလုံးပွဲ လောင်းကြေးတင်ခွင့် ခေတ္တပိတ်ထားပါသည်"
                                    },
                                    reopenAtMs = systemGameConfig.footballReopenAtMs,
                                    nowMs = nowMs
                                )
                            }
                        }
                        if (filteredMatches.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
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
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .widthIn(max = 620.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                            item(key = "hide_bar_sports_table") {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HideBarSectionHeader(
                                        title = if (isParlay) "မောင်း ပွဲစဉ်ဇယား" else "ဘော်ဒီ/ဂိုးပေါင်း ပွဲစဉ်ဇယား",
                                        countBadge = "${filteredMatches.size} ပွဲ",
                                        isVisible = showSportsTable,
                                        onToggle = { showSportsTable = !showSportsTable },
                                        testTag = "hide_bar_sports_matches_table"
                                    )
                                    if (showSportsTable) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "မြန်မာစံတော်ချိန် (MMT) • အာရှကြေး",
                                                color = InkMuted,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                androidx.compose.material3.TextButton(
                                                    onClick = {
                                                        groupedByLeague.keys.forEach { expandedLeagues[it] = true }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("အားလုံးဖွင့်", color = PitchEmeraldDark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                androidx.compose.material3.TextButton(
                                                    onClick = {
                                                        groupedByLeague.keys.forEach { expandedLeagues[it] = false }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("အားလုံးဝှက်", color = InkMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (showSportsTable) {
                                val singleLeagueMode = groupedByLeague.size == 1 || selectedLeagueFilter != null
                                groupedByLeague.forEach { (leagueName, leagueMatches) ->
                                    val isExpanded = expandedLeagues[leagueName] ?: singleLeagueMode
                                    item(key = "league_hdr_$leagueName") {
                                        LeagueHideBarHeader(
                                            leagueName = leagueName,
                                            matchCount = leagueMatches.size,
                                            isExpanded = isExpanded,
                                            onToggle = { expandedLeagues[leagueName] = !isExpanded }
                                        )
                                    }

                                    if (isExpanded) {
                                        items(leagueMatches, key = { it.id }) { match ->
                                            val currentPick = selectedPicks[match.id]
                                            ScreenshotMatchRowCard(
                                                match = match,
                                                selectedPickKey = currentPick?.pickKey,
                                                onShowInfo = { infoMatch = match },
                                                nowMs = nowMs,
                                                onSelect = { label, odds, pickKey ->
                                                    if (!isFootballPaused && MyanmarTimeUtil.isMatchBettable(match, nowMs)) {
                                                        if (currentPick?.pickKey == pickKey) {
                                                            selectedPicks.remove(match.id)
                                                        } else {
                                                            if (!isParlay) selectedPicks.clear()
                                                            selectedPicks[match.id] = MatchPick(match.id, label, odds, pickKey)
                                                        }
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
            }
        }
    }

    if (showLeagueFilterDialog) {
        SportsLeagueFilterDialog(
            totalCount = openMatches.size,
            availableLeagues = availableLeagues,
            selectedLeague = selectedLeagueFilter,
            onSelectLeague = { selectedLeagueFilter = it },
            onDismiss = { showLeagueFilterDialog = false }
        )
    }

    infoMatch?.let { m ->
        BoardMatchInfoDialog(
            match = m,
            onDismiss = { infoMatch = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThreeDLotteryScreen(
    drawsState: UiState<List<LotteryDraw>>,
    balanceKs: Long,
    systemGameConfig: SystemGameConfig = SystemGameConfig(),
    busy: Boolean,
    embeddedInHub: Boolean = false,
    onBack: () -> Unit,
    onSubmit3DOrder: (
        selections: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double
    ) -> Unit
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }
    val isGamesPaused = systemGameConfig.maintenanceMode || systemGameConfig.isGamesPausedNow(nowMs)

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
        !isGamesPaused &&
        openDraw != null &&
        numbersToBet.isNotEmpty() &&
        stakePerNumber >= 100L &&
        totalStake in minAllowedStake..maxAllowedStake &&
        totalStake <= balanceKs

    val contentBody: @Composable (Modifier) -> Unit = { containerMod ->
        LazyColumn(
            modifier = containerMod.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isGamesPaused) {
                item {
                    PauseStatusBannerCard(
                        title = if (systemGameConfig.maintenanceMode) {
                            systemGameConfig.maintenanceMessage.ifBlank { "စနစ်ပြုပြင်နေပါသည်" }
                        } else {
                            "ဂိမ်းနှင့် ထီကစားခြင်း ခေတ္တရပ်နားထားပါသည်"
                        },
                        reopenAtMs = systemGameConfig.gamesReopenAtMs,
                        nowMs = nowMs
                    )
                }
            }
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

    if (embeddedInHub) {
        contentBody(Modifier)
    } else {
        Scaffold(
            containerColor = DeepPitchBg,
            topBar = {
                SubScreenTopBar(title = "သုံးလုံးထီ (3D)", onBack = onBack)
            }
        ) { padding ->
            contentBody(Modifier.padding(padding))
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
