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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.betting.KyayFormatter
import com.example.data.model.BoardConfig
import com.example.data.model.SportMatch
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
import com.example.data.network.FootballFeedService
import com.example.ui.components.BoardGreenAccent
import com.example.ui.components.BoardMatchInfoDialog
import com.example.ui.components.BoardTopBarBg
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.MatchPick
import com.example.ui.components.PauseStatusBannerCard
import com.example.ui.components.ScreenshotMatchRowCard
import com.example.ui.components.SportsBetBottomBar
import com.example.ui.components.SportsLeagueFilterDialog
import com.example.ui.components.SportsLeagueHeaderRow
import com.example.ui.components.SportsModeSwitchStrip
import com.example.ui.components.StatusBadge
import com.example.ui.components.TeamLogoAvatar
import com.example.ui.components.buildOrderKickoffSummaryText
import com.example.ui.components.formatEpochDateTime
import com.example.ui.components.parseMatchKickoffEpochMs
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressOddsOrderScreen(
    boardMode: String, // "one_express" or "five_five_five"
    initialIsParlay: Boolean = false,
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
        pickKeys: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double,
        matchKickoffMs: Long,
        scanAtMs: Long,
        matchKickoffText: String
    ) -> Unit
) {
    val isOneExpress = boardMode == "one_express"
    val boardName = if (isOneExpress) "One Express" else "555"
    val accentColor = if (isOneExpress) BoardGreenAccent else ChampionshipGold

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    var isParlay by remember(boardMode, initialIsParlay) { mutableStateOf(initialIsParlay) }
    val selectedPicks = remember(boardMode) { mutableStateMapOf<String, MatchPick>() }
    var stakeInput by remember(boardMode) { mutableStateOf("1000") }
    var selectedLeagueFilter by remember(boardMode) { mutableStateOf<String?>(null) }
    var showLeagueFilterDialog by remember { mutableStateOf(false) }
    var infoMatch by remember { mutableStateOf<SportMatch?>(null) }
    var showBoardTable by remember { mutableStateOf(true) }

    val isFootballPaused = systemGameConfig.maintenanceMode || systemGameConfig.isFootballPausedNow(nowMs)

    val openMatches = remember(matchesState, boardMode, boardConfig.allowedLeagues) {
        val allOpen = when (matchesState) {
            is UiState.Success -> matchesState.data.filter {
                it.status == "open" && it.visibility == "public" && it.boardSource == boardMode
            }
            else -> emptyList()
        }
        val allowed = boardConfig.allowedLeagues.map { it.trim() }.filter { it.isNotBlank() }
        if (allowed.isEmpty()) allOpen
        else allOpen.filter { m ->
            allowed.any { lg -> m.league.equals(lg, ignoreCase = true) || m.league.contains(lg, ignoreCase = true) }
        }
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
    val minSelections = if (isParlay) 2 else 1
    val minAllowedStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxAllowedStake = systemGameConfig.maxStakeKs.coerceAtLeast(minAllowedStake)
    val canSubmit = !busy &&
        !isFootballPaused &&
        selectedPicks.size >= minSelections &&
        parsedStake in minAllowedStake..maxAllowedStake &&
        parsedStake <= balanceKs

    Scaffold(
        containerColor = Color(0xFFEBEFED),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isParlay) "$boardName • မောင်း" else "$boardName • ဘော်ဒီ/ဂိုးပေါင်း",
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
                            modifier = Modifier.testTag("express_${boardMode}_filter_button")
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
                SportsModeSwitchStrip(
                    isParlay = isParlay,
                    accentColor = accentColor,
                    onSelectMode = { targetParlay ->
                        isParlay = targetParlay
                        if (!targetParlay && selectedPicks.size > 1) {
                            val keepEntry = selectedPicks.entries.lastOrNull()
                            selectedPicks.clear()
                            if (keepEntry != null) {
                                selectedPicks[keepEntry.key] = keepEntry.value
                            }
                        }
                    },
                    testTagPrefix = "express_${boardMode}_mode"
                )
            }
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
                stakeTestTag = "express_${boardMode}_stake_input",
                submitTestTag = "submit_${boardMode}_order_button",
                onSubmit = {
                    val ordered = selectedPicks.values.toList()
                    val pickedMatches = openMatches.filter { it.id in selectedPicks.keys }
                    val kickoffSummary = buildOrderKickoffSummaryText(pickedMatches)
                    val roundTitle = if (isParlay) {
                        "$boardName မောင်း (${ordered.size} ပွဲ)"
                    } else {
                        "$boardName ဘော်ဒီ/ဂိုးပေါင်း"
                    }
                    onSubmitOrder(
                        boardMode,
                        ordered.map { it.label },
                        ordered.map { it.matchId },
                        ordered.map { it.pickKey },
                        roundTitle.take(60),
                        parsedStake,
                        combinedMultiplier,
                        earliestKickoffMs,
                        scheduledScanAtMs,
                        kickoffSummary
                    )
                    selectedPicks.clear()
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
                                text = "လက်ရှိ $boardName ပွဲစဉ် မရှိသေးပါ",
                                color = InkDark,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Admin မှ $boardName ပွဲစဉ်များ ထည့်သွင်း/Auto-Sync လုပ်ပေးပါက ဤနေရာတွင် ချက်ချင်း ပေါ်လာပါမည်။",
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
                            if (isFootballPaused) {
                                item(key = "express_pause_banner") {
                                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                            }
                            item(key = "hide_bar_express_table") {
                                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    HideBarSectionHeader(
                                        title = if (isParlay) "$boardName မောင်း ပွဲစဉ်ဇယား" else "$boardName ပွဲစဉ်ဇယား",
                                        countBadge = "${filteredMatches.size} ပွဲ",
                                        isVisible = showBoardTable,
                                        onToggle = { showBoardTable = !showBoardTable },
                                        testTag = "hide_bar_express_${boardMode}_list"
                                    )
                                }
                            }
                            if (showBoardTable) {
                                groupedByLeague.forEach { (leagueName, leagueMatches) ->
                                    item(key = "express_league_hdr_$leagueName") {
                                        SportsLeagueHeaderRow(leagueName = leagueName)
                                    }

                                    items(leagueMatches, key = { it.id }) { match ->
                                        val currentPick = selectedPicks[match.id]
                                        ScreenshotMatchRowCard(
                                            match = match,
                                            selectedPickKey = currentPick?.pickKey,
                                            onShowInfo = { infoMatch = match },
                                            onSelect = { label, odds, pickKey ->
                                                if (!isFootballPaused) {
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
            boardSourceLabel = if (isOneExpress) "One Express" else "555 Odds",
            onDismiss = { infoMatch = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminExpressBoardTab(
    boardMode: String, // "one_express" or "five_five_five"
    matches: List<SportMatch>,
    boardConfig: BoardConfig,
    busy: Boolean,
    onSyncNow: () -> Unit,
    onSyncLiveResultsNow: () -> Unit,
    onAutoSettleNow: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onToggleAutoLiveResults: (Boolean) -> Unit,
    onSaveAllowedLeagues: (List<String>) -> Unit,
    onSeedSampleBoard: () -> Unit,
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
    val boardTitle = if (isOneExpress) "One Express ဘောလုံးပွဲကြေး" else "555 ဘောလုံးပွဲကြေး"
    val accent = if (isOneExpress) Color(0xFF38BDF8) else ChampionshipGold
    val autoEnabled = if (isOneExpress) boardConfig.autoSyncOneExpress else boardConfig.autoSyncFiveFiveFive
    val filteredMatches = remember(matches, boardMode) {
        matches.filter { it.boardSource == boardMode }
    }

    var showAutoSyncCard by remember(boardMode) { mutableStateOf(true) }
    var showMatchesTable by remember(boardMode) { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMatch by remember { mutableStateOf<SportMatch?>(null) }
    var deleteConfirmMatch by remember { mutableStateOf<SportMatch?>(null) }
    var selectedLeagues by remember(boardConfig.allowedLeagues) {
        mutableStateOf(boardConfig.allowedLeagues.toSet())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dedicated Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (isOneExpress) Icons.Default.Bolt else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = boardTitle,
                                color = accent,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "သီးသန့်ပွဲကြေး & ပထမစနစ် အလိုအလျောက်စီမံခန့်ခွဲမှု",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Surface(
                        color = accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "${filteredMatches.count { it.status == "open" }} ဖွင့်",
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Collapsible Automation Card mirroring primary AdminBoardTab
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAutoSyncCard = !showAutoSyncCard }
                            .testTag("toggle_auto_sync_${boardMode}_card"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = accent)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "နေ့စဉ်ပွဲစဉ် & တိုက်ရိုက်ရလဒ် အလိုအလျောက်စနစ်",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto ပွဲ: ${if (autoEnabled) "ON" else "OFF"} • Live ရလဒ်: ${if (boardConfig.autoSyncLiveResults) "ON" else "OFF"}",
                                    color = Color(0xFFB4C8BE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showAutoSyncCard) "ဝှက်မည်" else "ပြမည်",
                                color = accent,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = if (showAutoSyncCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle auto sync card",
                                tint = accent
                            )
                        }
                    }

                    if (showAutoSyncCard) {
                        Button(
                            onClick = onSyncNow,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sync_now_$boardMode")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOneExpress) "ယနေ့ One Express ပွဲစဉ်များ အလိုအလျောက်ထည့်မည်" else "ယနေ့ 555 ပွဲစဉ်များ အလိုအလျောက်ထည့်မည်",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onSyncLiveResultsNow,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sync_live_results_$boardMode")
                        ) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("တိုက်ရိုက်ရလဒ်များ ချိတ်ဆက်မည်", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onAutoSettleNow,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E), contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auto_settle_$boardMode")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ပွဲပြီး အော်ဒါများ Auto ရှင်းမည်", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("ပွဲရှိသည့်နေ့တိုင်း အလိုအလျောက်ထည့်မည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                                if (boardConfig.lastSyncedDate.isNotBlank()) {
                                    Text(
                                        text = "နောက်ဆုံးထည့်ပြီးရက်: ${boardConfig.lastSyncedDate}",
                                        color = Color(0xFFB4C8BE),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Switch(
                                checked = autoEnabled,
                                onCheckedChange = onToggleAutoSync,
                                modifier = Modifier.testTag("toggle_auto_sync_$boardMode")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "တိုက်ရိုက်ရလဒ် အလိုအလျောက်ချိတ်ဆက်မည်",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Switch(
                                checked = boardConfig.autoSyncLiveResults,
                                onCheckedChange = onToggleAutoLiveResults,
                                modifier = Modifier.testTag("toggle_auto_live_$boardMode")
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        Text(
                            text = if (selectedLeagues.isEmpty()) "ဖွင့်လှစ်မည့် ဖလား/လိဂ်: အားလုံး (All)"
                            else "ရွေးချယ်ထားသော လိဂ် (${selectedLeagues.size})",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedLeagues.isEmpty(),
                                onClick = { selectedLeagues = emptySet() },
                                label = { Text("အားလုံး (All)") }
                            )
                            FootballFeedService.DEFAULT_LEAGUE_OPTIONS.forEach { leagueName ->
                                val picked = selectedLeagues.contains(leagueName)
                                FilterChip(
                                    selected = picked,
                                    onClick = {
                                        selectedLeagues = if (picked) selectedLeagues - leagueName else selectedLeagues + leagueName
                                    },
                                    label = { Text(leagueName) }
                                )
                            }
                        }
                        Button(
                            onClick = { onSaveAllowedLeagues(selectedLeagues.toList()) },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_allowed_leagues_$boardMode")
                        ) {
                            Text("ဖလား/လိဂ် ရွေးချယ်မှု သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onSeedSampleBoard,
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("seed_sample_$boardMode")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOneExpress) "နမူနာ One Express ပွဲစဉ်များ အလိုအလျောက်ထည့်မည်" else "နမူနာ 555 ပွဲစဉ်များ အလိုအလျောက်ထည့်မည်",
                                color = ChampionshipGold
                            )
                        }
                    }
                }
            }
        }

        // Manual Add Match Button
        item {
            Button(
                onClick = { showAddDialog = true },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = InkDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_match_$boardMode")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isOneExpress) "One Express ပွဲစဉ်သစ် ထည့်မည်" else "555 ပွဲစဉ်သစ် ထည့်မည်",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Matches List Hide Bar
        item {
            HideBarSectionHeader(
                title = if (isOneExpress) "One Express ပွဲစဉ်များ ဇယား" else "555 ပွဲစဉ်များ ဇယား",
                countBadge = "${filteredMatches.size} ပွဲ",
                isVisible = showMatchesTable,
                onToggle = { showMatchesTable = !showMatchesTable },
                testTag = "hide_bar_admin_${boardMode}_table",
                accentColor = accent
            )
        }

        if (showMatchesTable) {
            if (filteredMatches.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "$boardTitle တွင် ပွဲစဉ်မရှိသေးပါ — 'အလိုအလျောက်ထည့်မည်' သို့မဟုတ် 'ပွဲစဉ်သစ် ထည့်မည်' ကို နှိပ်ပါ။"
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingMatch = m },
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
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                StatusBadge(status = m.status)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TeamLogoAvatar(logoUrl = m.homeLogoUrl, teamName = m.homeTeam, size = 22.dp)
                                Text(
                                    text = "${m.homeTeam} vs ${m.awayTeam} (${m.homeScore}-${m.awayScore}${if (m.liveClock.isNotBlank()) " ${m.liveClock}" else ""})",
                                    color = InkDark,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TeamLogoAvatar(logoUrl = m.awayLogoUrl, teamName = m.awayTeam, size = 22.dp)
                            }
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
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { editingMatch = m },
                                    modifier = Modifier.testTag("edit_express_match_${m.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ပြုပြင် / ရလဒ်ထည့်")
                                }
                                Spacer(Modifier.width(6.dp))
                                IconButton(
                                    onClick = { deleteConfirmMatch = m },
                                    modifier = Modifier.testTag("delete_express_match_${m.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Match", tint = Color(0xFFB91C1C))
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
