package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.betting.MiniGameEngine
import com.example.data.model.Live2DStatus
import com.example.data.model.LotteryDraw
import com.example.data.model.MiniGameControlConfig
import com.example.data.model.MiniGameRound
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
import com.example.data.util.MyanmarTimeUtil
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.PauseStatusBannerCard
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.formatKs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ChampionshipGoldBright
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TwoDLotteryScreen(
    drawsState: UiState<List<LotteryDraw>>,
    live2DStatus: Live2DStatus?,
    balanceKs: Long,
    systemGameConfig: SystemGameConfig,
    busy: Boolean,
    embeddedInHub: Boolean = false,
    onRefreshLive2D: () -> Unit,
    onBack: () -> Unit,
    onSubmit2DOrder: (
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

    val selectedNumbers = remember { mutableStateListOf<String>() }
    var manualInput by remember { mutableStateOf("") }
    var includeReverse by remember { mutableStateOf(false) }
    var stakePerNumberInput by remember { mutableStateOf("500") }
    var showTwoDGrid by remember { mutableStateOf(true) }

    val todayIso = remember { MyanmarTimeUtil.todayIsoDate() }
    val initialSession = remember {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yangon"))
        val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (mins < (12 * 60 + 5)) "1201" else "430"
    }
    var selectedSession by remember { mutableStateOf(initialSession) }

    val twoDDraws = when (drawsState) {
        is UiState.Success -> drawsState.data.filter { it.drawType == "2d" }
        else -> emptyList()
    }
    val morningOpenDraw = twoDDraws.firstOrNull {
        it.status == "open" && (it.roundTitle.contains("12:01") || it.roundTitle.contains("နေ့"))
    }
    val eveningOpenDraw = twoDDraws.firstOrNull {
        it.status == "open" && (it.roundTitle.contains("4:30") || it.roundTitle.contains("16:30") || it.roundTitle.contains("ည"))
    }
    val genericOpenDraw = twoDDraws.firstOrNull { it.status == "open" }
    val selectedOpenDraw = if (selectedSession == "1201") {
        morningOpenDraw ?: genericOpenDraw
    } else {
        eveningOpenDraw ?: genericOpenDraw
    }
    val sessionExplicitlyClosed = remember(twoDDraws, selectedSession, todayIso) {
        val matchingForSession = twoDDraws.filter { d ->
            if (selectedSession == "1201") d.roundTitle.contains("12:01") || d.roundTitle.contains("နေ့")
            else d.roundTitle.contains("4:30") || d.roundTitle.contains("16:30") || d.roundTitle.contains("ည")
        }
        matchingForSession.isNotEmpty() && matchingForSession.none { it.status == "open" }
    }
    val sessionRoundTitle = if (selectedSession == "1201") {
        "2D နေ့လည် (12:01 PM) • $todayIso"
    } else {
        "2D ညနေ (4:30 PM) • $todayIso"
    }
    val sessionCloseLabel = if (selectedSession == "1201") {
        morningOpenDraw?.closeTimeLabel ?: "11:55 AM Close"
    } else {
        eveningOpenDraw?.closeTimeLabel ?: "3:55 PM Close"
    }

    val drawMultiplier = selectedOpenDraw?.payoutMultiplier ?: systemGameConfig.twoDPayoutMultiplier
    val stakePerNumber = stakePerNumberInput.toLongOrNull() ?: 0L
    val totalStake = stakePerNumber * selectedNumbers.size
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)

    val canSubmit = !busy &&
        !isGamesPaused &&
        !sessionExplicitlyClosed &&
        selectedNumbers.isNotEmpty() &&
        stakePerNumber >= 100L &&
        totalStake in minStake..maxStake &&
        totalStake <= balanceKs

    val rows00To99 = remember {
        (0..99).map { it.toString().padStart(2, '0') }.chunked(10)
    }

    val contentBody: @Composable (Modifier) -> Unit = { containerMod ->
        LazyColumn(
            modifier = containerMod.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
            // Live Thai Stock 2D Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.5.dp, ChampionshipGold.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PitchEmerald)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Live Myanmar 2D (SET Feed)",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = onRefreshLive2D,
                                modifier = Modifier.testTag("refresh_live_2d_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh 2D", tint = ChampionshipGold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PitchEmeraldDark,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ChampionshipGold)
                            ) {
                                Text(
                                    text = live2DStatus?.twod ?: "--",
                                    color = ChampionshipGoldBright,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "SET: ${live2DStatus?.setText ?: "--"}",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Value: ${live2DStatus?.valueText ?: "--"}",
                                    color = Color(0xFFB4C8BE),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "12:01: ${live2DStatus?.morning1201 ?: "--"}  •  4:30: ${live2DStatus?.evening430 ?: "--"}",
                                    color = ChampionshipGold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // 12:01 PM vs 4:30 PM Session Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sessionRoundTitle,
                                color = InkDark,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(color = PitchEmeraldDark, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${drawMultiplier.toInt()}x",
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isMorningSelected = selectedSession == "1201"
                            Surface(
                                color = if (isMorningSelected) PitchEmeraldDark else Color(0xFFE7ECE9),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (isMorningSelected) 1.5.dp else 1.dp,
                                    color = if (isMorningSelected) ChampionshipGold else PearlCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSession = "1201" }
                                    .testTag("two_d_session_1201")
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "နေ့လည်ပိုင်း (12:01 PM)",
                                        color = if (isMorningSelected) Color.White else InkDark,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ရလဒ်: ${live2DStatus?.morning1201 ?: "--"} • 11:55 AM ပိတ်",
                                        color = if (isMorningSelected) ChampionshipGoldBright else InkMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }
                            }
                            Surface(
                                color = if (!isMorningSelected) PitchEmeraldDark else Color(0xFFE7ECE9),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (!isMorningSelected) 1.5.dp else 1.dp,
                                    color = if (!isMorningSelected) ChampionshipGold else PearlCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSession = "430" }
                                    .testTag("two_d_session_430")
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "ညနေပိုင်း (4:30 PM)",
                                        color = if (!isMorningSelected) Color.White else InkDark,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ရလဒ်: ${live2DStatus?.evening430 ?: "--"} • 3:55 PM ပိတ်",
                                        color = if (!isMorningSelected) ChampionshipGoldBright else InkMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (sessionExplicitlyClosed) "ဤအကြိမ်အတွက် ထီပိတ်ထားပါသည် • လက်ကျန်: ${formatKs(balanceKs)}"
                            else "ပိတ်ချိန်: $sessionCloseLabel • လက်ကျန်: ${formatKs(balanceKs)}",
                            color = if (sessionExplicitlyClosed) AdminCrimson else InkMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Quick numeric input + R toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualInput,
                                onValueChange = { manualInput = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text("2D ဂဏန်း (00-99)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = InkDark,
                                    unfocusedTextColor = InkDark
                                ),
                                modifier = Modifier.weight(1f).testTag("two_d_manual_input")
                            )
                            Button(
                                onClick = {
                                    if (manualInput.length == 2) {
                                        if (!selectedNumbers.contains(manualInput)) selectedNumbers.add(manualInput)
                                        if (includeReverse) {
                                            val rev = manualInput.reversed()
                                            if (!selectedNumbers.contains(rev)) selectedNumbers.add(rev)
                                        }
                                        manualInput = ""
                                    }
                                },
                                enabled = manualInput.length == 2,
                                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                                modifier = Modifier.height(52.dp).testTag("two_d_add_number_button")
                            ) {
                                Text("ထည့်မည်")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { includeReverse = !includeReverse }
                        ) {
                            Checkbox(checked = includeReverse, onCheckedChange = { includeReverse = it })
                            Text("R (အပြန်ပါ ထည့်မည်)", color = InkDark)
                            Spacer(Modifier.weight(1f))
                            if (selectedNumbers.isNotEmpty()) {
                                OutlinedButton(onClick = { selectedNumbers.clear() }) {
                                    Text("အားလုံးဖျက် (${selectedNumbers.size})", color = AdminCrimson)
                                }
                            }
                        }

                        if (selectedNumbers.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedNumbers.forEach { num ->
                                    Surface(
                                        color = PitchEmeraldDark,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { selectedNumbers.remove(num) }
                                    ) {
                                        Text(
                                            text = "$num ×",
                                            color = Color.White,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hide Bar + Interactive 00-99 Grid
            item {
                HideBarSectionHeader(
                    title = "00 - 99 ဂဏန်းဇယားကွက်",
                    countBadge = "${selectedNumbers.size} ကွက်ရွေး",
                    isVisible = showTwoDGrid,
                    onToggle = { showTwoDGrid = !showTwoDGrid },
                    testTag = "hide_bar_two_d_grid",
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
            if (showTwoDGrid) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "00 - 99 ဇယားကွက် (နှိပ်၍ ရွေးချယ်ပါ)",
                                color = InkDark,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            rows00To99.forEach { rowNums ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowNums.forEach { num ->
                                        val isSel = selectedNumbers.contains(num)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) PitchEmeraldDark else Color(0xFFE7ECE9))
                                                .border(
                                                    width = if (isSel) 1.5.dp else 0.5.dp,
                                                    color = if (isSel) ChampionshipGold else PearlCardBorder,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable {
                                                    if (isSel) selectedNumbers.remove(num)
                                                    else {
                                                        selectedNumbers.add(num)
                                                        if (includeReverse) {
                                                            val rev = num.reversed()
                                                            if (!selectedNumbers.contains(rev)) selectedNumbers.add(rev)
                                                        }
                                                    }
                                                }
                                                .testTag("two_d_cell_$num"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = num,
                                                color = if (isSel) Color.White else InkDark,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stake & Outcome Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("တစ်ကွက် ထိုးကြေး (Ks)", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100L, 500L, 1000L, 3000L, 5000L).forEach { amt ->
                                FilterChip(
                                    selected = stakePerNumber == amt,
                                    onClick = { stakePerNumberInput = amt.toString() },
                                    label = { Text("$amt") }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = stakePerNumberInput,
                            onValueChange = { stakePerNumberInput = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("တစ်ကွက် ထိုးကြေး (Ks)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("two_d_stake_input")
                        )

                        val winGross = (stakePerNumber * drawMultiplier).roundToLong()
                        val winNet = (winGross - totalStake).coerceAtLeast(0L)
                        Surface(
                            color = Color(0xFFF3F7F4),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PearlCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("လောင်းကြေးငွေစုစုပေါင်း:", color = InkMuted)
                                    Text(formatKs(totalStake), color = InkDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("နိုင်ရင် ရရှိမည် (၁ ကွက်ပေါက်):", color = PitchEmeraldDark)
                                    Text("${formatKs(winGross)} (+${formatKs(winNet)})", color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ရှုံးရင် ရှုံးမည်:", color = AdminCrimson)
                                    Text("-${formatKs(totalStake)}", color = AdminCrimson, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val round = sessionRoundTitle
                                val selections = selectedNumbers.map { num ->
                                    "2D: $num (${formatKs(stakePerNumber)})"
                                }
                                val effectiveMult = if (selectedNumbers.isEmpty()) drawMultiplier
                                else (drawMultiplier / selectedNumbers.size.toDouble()).coerceAtLeast(1.0)
                                onSubmit2DOrder(selections, round, totalStake, effectiveMult)
                            },
                            enabled = canSubmit,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_two_d_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("၂လုံးထီ အော်ဒါတင်မည် (${selectedNumbers.size} ကွက်)", fontWeight = FontWeight.Bold)
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
                SubScreenTopBar(title = "၂လုံးထီ (2D)", onBack = onBack)
            }
        ) { padding ->
            contentBody(Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiceGameScreen(
    balanceKs: Long,
    systemGameConfig: SystemGameConfig,
    miniGameControlConfig: MiniGameControlConfig = MiniGameControlConfig(),
    roundsState: UiState<List<MiniGameRound>>,
    busy: Boolean,
    onBack: () -> Unit,
    onPlayDice: (betOption: String, exactNumber: Int, stakeKs: Long, onResult: (List<Int>, MiniGameRound) -> Unit) -> Unit
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }
    val isGamesPaused = systemGameConfig.maintenanceMode || systemGameConfig.isGamesPausedNow(nowMs)

    var selectedOption by remember { mutableStateOf("BIG") }
    var exactNumber by remember { mutableIntStateOf(11) }
    var stakeInput by remember { mutableStateOf("1000") }
    var lastDice by remember { mutableStateOf(listOf(4, 5, 2)) }
    var displayDice by remember { mutableStateOf(listOf(4, 5, 2)) }
    var isRollingVisual by remember { mutableStateOf(false) }
    var lastRound by remember { mutableStateOf<MiniGameRound?>(null) }
    var showDiceHistory by remember { mutableStateOf(true) }
    val animScope = rememberCoroutineScope()

    val diceRotation by animateFloatAsState(
        targetValue = if (isRollingVisual) 360f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "dice_spin"
    )
    val diceScale by animateFloatAsState(
        targetValue = if (isRollingVisual) 1.09f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "dice_pop"
    )

    val allowedTotals = remember(miniGameControlConfig.diceAllowedTotals) {
        miniGameControlConfig.diceAllowedTotals.filter { it in 3..18 }.distinct().sorted()
    }
    LaunchedEffect(allowedTotals) {
        if (allowedTotals.isNotEmpty() && !allowedTotals.contains(exactNumber)) {
            exactNumber = allowedTotals.first()
        }
    }

    val parsedStake = stakeInput.toLongOrNull() ?: 0L
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)
    val canPlay = !busy && !isRollingVisual && !isGamesPaused && parsedStake in minStake..maxStake && parsedStake <= balanceKs

    val stdMult = miniGameControlConfig.diceStandardMult
    val tripleMult = miniGameControlConfig.diceTripleMult
    val exactMult = miniGameControlConfig.diceExactMult

    val multiplierPreview = when (selectedOption) {
        "TRIPLE" -> tripleMult
        "EXACT" -> exactMult
        else -> stdMult
    }

    val diceHistory = remember(roundsState) {
        ((roundsState as? UiState.Success)?.data ?: emptyList()).filter { it.gameType == "dice" }
    }
    val winStreak = remember(diceHistory) {
        diceHistory.takeWhile { it.payoutKs > 0L }.size
    }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "အံစာတုံး (Dice)", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
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
                            "ဂိမ်းကစားခြင်း ခေတ္တရပ်နားထားပါသည်"
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
                    border = BorderStroke(1.5.dp, ChampionshipGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("လက်ကျန်: ${formatKs(balanceKs)}", color = ChampionshipGold, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (winStreak >= 2) {
                                    Surface(color = Color(0xFFDC2626), shape = CircleShape) {
                                        Text(
                                            "🔥 $winStreak ဆက်တိုက်",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    "စုစုပေါင်း: ${displayDice.sum()}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            displayDice.forEachIndexed { idx, dieValue ->
                                Box(
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = if (isRollingVisual) (if (idx % 2 == 0) diceRotation else -diceRotation) else 0f
                                        scaleX = diceScale
                                        scaleY = diceScale
                                    }
                                ) {
                                    DiceFaceVisual(value = dieValue)
                                }
                            }
                        }

                        if (diceHistory.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                diceHistory.take(6).forEach { hr ->
                                    val won = hr.payoutKs > 0L
                                    Surface(
                                        color = if (won) Color(0xFF065F46) else Color(0xFF334155),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = if (won) "✔ ${hr.outcomeLabel.take(10)}" else "✖ ${hr.outcomeLabel.take(10)}",
                                            color = if (won) ChampionshipGoldBright else Color(0xFFCBD5E1),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        lastRound?.let { r ->
                            Surface(
                                color = if (r.payoutKs > 0L) PitchEmeraldDark else AdminCrimson.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (r.payoutKs > 0L) "🎉 အနိုင်ရ! ${r.outcomeLabel} → +${formatKs(r.payoutKs)}"
                                    else "မပေါက်သေးပါ (${r.outcomeLabel}) — ထပ်စမ်းကြည့်ပါ!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ကစားမည့် အမျိုးအစား ရွေးပါ", color = InkDark, fontWeight = FontWeight.Bold)
                        val options = listOf(
                            Triple("BIG", "အကြီး (11-17)", "${stdMult}x"),
                            Triple("SMALL", "အသေး (4-10)", "${stdMult}x"),
                            Triple("ODD", "မ (Odd)", "${stdMult}x"),
                            Triple("EVEN", "စုံ (Even)", "${stdMult}x"),
                            Triple("TRIPLE", "၃ လုံးတူ (Triple)", "${tripleMult}x"),
                            Triple("EXACT", "အတိအကျ (Exact)", "${exactMult}x")
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEach { (key, label, rate) ->
                                FilterChip(
                                    selected = selectedOption == key,
                                    onClick = { selectedOption = key },
                                    label = { Text("$label • $rate") }
                                )
                            }
                        }

                        if (selectedOption == "EXACT") {
                            if (allowedTotals.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "Admin ခွင့်ပြုထားသော ဂဏန်းများ:",
                                        color = PitchEmeraldDark,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        allowedTotals.forEach { num ->
                                            FilterChip(
                                                selected = exactNumber == num,
                                                onClick = { exactNumber = num },
                                                label = { Text("$num") }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("စုစုပေါင်း ဂဏန်း:", color = InkDark, fontWeight = FontWeight.SemiBold)
                                    IconButton(onClick = { if (exactNumber > 3) exactNumber-- }) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "$exactNumber",
                                        color = PitchEmeraldDark,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { if (exactNumber < 18) exactNumber++ }) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase")
                                    }
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { amt ->
                                FilterChip(
                                    selected = parsedStake == amt,
                                    onClick = { stakeInput = amt.toString() },
                                    label = { Text("$amt") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = stakeInput,
                            onValueChange = { stakeInput = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("ထိုးကြေး (Ks)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("dice_stake_input")
                        )

                        val potentialWin = (parsedStake * multiplierPreview).roundToLong()
                        Text(
                            "နိုင်ရင် ရရှိမည်: ${formatKs(potentialWin)} • ရှုံးရင်: -${formatKs(parsedStake)}",
                            color = PitchEmeraldDark,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = {
                                isRollingVisual = true
                                animScope.launch {
                                    repeat(6) {
                                        displayDice = listOf((1..6).random(), (1..6).random(), (1..6).random())
                                        delay(70L)
                                    }
                                }
                                onPlayDice(selectedOption, exactNumber, parsedStake) { dice, round ->
                                    animScope.launch {
                                        delay(380L)
                                        lastDice = dice
                                        displayDice = dice
                                        lastRound = round
                                        isRollingVisual = false
                                    }
                                }
                            },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("dice_play_button")
                        ) {
                            if (busy || isRollingVisual) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("အံစာတုံး လှိမ့်နေသည်...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Casino, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("အံစာတုံး လှိမ့်မည်", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (diceHistory.isNotEmpty()) {
                item {
                    HideBarSectionHeader(
                        title = "အံစာတုံး လတ်တလော မှတ်တမ်း",
                        countBadge = "${diceHistory.size}",
                        isVisible = showDiceHistory,
                        onToggle = { showDiceHistory = !showDiceHistory },
                        testTag = "hide_bar_dice_history",
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
                if (showDiceHistory) {
                    items(diceHistory.take(15), key = { it.id }) { round ->
                        MiniGameRoundCard(round)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiceFaceVisual(value: Int) {
    Surface(
        color = PearlCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, ChampionshipGold),
        shadowElevation = 6.dp,
        modifier = Modifier.size(64.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height
            val r = size.minDimension * 0.11f
            val dotColor = if (value == 1 || value == 4) Color(0xFFB91C1C) else Color(0xFF111C17)
            val points = when (value) {
                1 -> listOf(Offset(w * 0.5f, h * 0.5f))
                2 -> listOf(Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f))
                3 -> listOf(Offset(w * 0.25f, h * 0.25f), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.75f, h * 0.75f))
                4 -> listOf(
                    Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.25f),
                    Offset(w * 0.25f, h * 0.75f), Offset(w * 0.75f, h * 0.75f)
                )
                5 -> listOf(
                    Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.25f),
                    Offset(w * 0.5f, h * 0.5f),
                    Offset(w * 0.25f, h * 0.75f), Offset(w * 0.75f, h * 0.75f)
                )
                else -> listOf(
                    Offset(w * 0.25f, h * 0.2f), Offset(w * 0.75f, h * 0.2f),
                    Offset(w * 0.25f, h * 0.5f), Offset(w * 0.75f, h * 0.5f),
                    Offset(w * 0.25f, h * 0.8f), Offset(w * 0.75f, h * 0.8f)
                )
            }
            points.forEach { pt ->
                drawCircle(color = dotColor, radius = r, center = pt)
            }
        }
    }
}

private data class AnimalToken(
    val index: Int,
    val title: String,
    val badgeColor: Color
)

private val SIX_ANIMALS_LIST = listOf(
    AnimalToken(0, "သမင် (Deer)", Color(0xFF047857)),
    AnimalToken(1, "ကျား (Tiger)", Color(0xFFD97706)),
    AnimalToken(2, "ဆင် (Elephant)", Color(0xFF1D4ED8)),
    AnimalToken(3, "ကြက် (Rooster)", Color(0xFFB91C1C)),
    AnimalToken(4, "ပုစွန် (Prawn)", Color(0xFFDB2777)),
    AnimalToken(5, "ငါး (Fish)", Color(0xFF0891B2))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SixAnimalsGameScreen(
    balanceKs: Long,
    systemGameConfig: SystemGameConfig,
    miniGameControlConfig: MiniGameControlConfig = MiniGameControlConfig(),
    roundsState: UiState<List<MiniGameRound>>,
    busy: Boolean,
    onBack: () -> Unit,
    onPlaySixAnimals: (
        selectedAnimalIndices: Set<Int>,
        animalLabels: List<String>,
        stakePerAnimalKs: Long,
        onResult: (List<Int>, MiniGameRound) -> Unit
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

    var selectedAnimals by remember { mutableStateOf(setOf(0)) }
    var stakeInput by remember { mutableStateOf("1000") }
    var lastRolledIndices by remember { mutableStateOf(listOf(0, 2, 5)) }
    var displayRolledIndices by remember { mutableStateOf(listOf(0, 2, 5)) }
    var isRollingVisual by remember { mutableStateOf(false) }
    var lastRound by remember { mutableStateOf<MiniGameRound?>(null) }
    var showAnimalHistory by remember { mutableStateOf(true) }
    val animScope = rememberCoroutineScope()

    val slotScale by animateFloatAsState(
        targetValue = if (isRollingVisual) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "animal_slot_pop"
    )

    val parsedStake = stakeInput.toLongOrNull() ?: 0L
    val totalStake = parsedStake * selectedAnimals.size.coerceAtLeast(1)
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)
    val canPlay = !busy && !isRollingVisual && !isGamesPaused &&
        selectedAnimals.isNotEmpty() &&
        parsedStake >= 100L &&
        totalStake in minStake..maxStake.coerceAtLeast(1_000_000L) &&
        totalStake <= balanceKs

    val singleMult = miniGameControlConfig.sixAnimalsMult.coerceAtLeast(1.2)
    val bonus = (singleMult - 1.0).coerceAtLeast(0.2)
    val doubleMult = 1.0 + bonus * 2.0
    val tripleAnimalMult = 1.0 + bonus * 3.0
    val maxOneHitPayout = MiniGameEngine.computeSixAnimalsMultiPickPayout(
        roll = Triple(selectedAnimals.first(), selectedAnimals.first(), selectedAnimals.first()),
        selectedAnimalIndices = selectedAnimals,
        stakePerAnimalKs = parsedStake,
        baseBonus = bonus
    )

    val animalHistory = remember(roundsState) {
        ((roundsState as? UiState.Success)?.data ?: emptyList()).filter { it.gameType == "six_animals" }
    }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "၆ကောင်ဂျင် (Multi-Pick)", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
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
                            "ဂိမ်းကစားခြင်း ခေတ္တရပ်နားထားပါသည်"
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
                    border = BorderStroke(1.5.dp, ChampionshipGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("လက်ကျန်: ${formatKs(balanceKs)}", color = ChampionshipGold, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            Text(
                                "1တူ=${String.format(Locale.US, "%.1f", singleMult)}x • 2တူ=${String.format(Locale.US, "%.1f", doubleMult)}x • 3တူ=${String.format(Locale.US, "%.1f", tripleAnimalMult)}x",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            displayRolledIndices.forEach { idx ->
                                val token = SIX_ANIMALS_LIST.getOrElse(idx) { SIX_ANIMALS_LIST.first() }
                                val matched = selectedAnimals.contains(token.index)
                                Surface(
                                    color = token.badgeColor,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(if (matched) 2.5.dp else 1.5.dp, if (matched) ChampionshipGoldBright else ChampionshipGold.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = slotScale
                                            scaleY = slotScale
                                        }
                                ) {
                                    Text(
                                        text = token.title.substringBefore(" "),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                        }

                        lastRound?.let { r ->
                            Surface(
                                color = if (r.payoutKs > 0L) PitchEmeraldDark else AdminCrimson.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (r.payoutKs > 0L) "🎉 အနိုင်ရ! ${r.outcomeLabel} → +${formatKs(r.payoutKs)}"
                                    else "မပေါက်သေးပါ (${r.outcomeLabel}) — ထပ်စမ်းကြည့်ပါ!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("စိတ်ကြိုက်အကောင် ၁ ကောင်မက ရွေးပါ (${selectedAnimals.size}/6)", color = InkDark, fontWeight = FontWeight.Bold)
                                Text("တစ်ကောင်ချင်းစီ ထပ်တိုးရွေးချယ် ထိုးနိုင်ပါသည်", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            FilterChip(
                                selected = selectedAnimals.size == 6,
                                onClick = {
                                    selectedAnimals = if (selectedAnimals.size == 6) setOf(0) else setOf(0, 1, 2, 3, 4, 5)
                                },
                                label = { Text(if (selectedAnimals.size == 6) "၁ ကောင်ထဲ" else "အားလုံး") },
                                modifier = Modifier.testTag("six_animals_select_all_chip")
                            )
                        }
                        SIX_ANIMALS_LIST.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { token ->
                                    val isSel = selectedAnimals.contains(token.index)
                                    Surface(
                                        color = if (isSel) token.badgeColor else Color(0xFFEDF2EF),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) ChampionshipGold else PearlCardBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedAnimals = if (isSel) {
                                                    if (selectedAnimals.size > 1) selectedAnimals - token.index else selectedAnimals
                                                } else {
                                                    selectedAnimals + token.index
                                                }
                                            }
                                            .testTag("six_animal_choice_${token.index}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSel) Color.White else token.badgeColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isSel) Icons.Default.CheckCircle else Icons.Default.Pets,
                                                    contentDescription = null,
                                                    tint = if (isSel) token.badgeColor else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = token.title,
                                                color = if (isSel) Color.White else InkDark,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { amt ->
                                FilterChip(
                                    selected = parsedStake == amt,
                                    onClick = { stakeInput = amt.toString() },
                                    label = { Text("$amt") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = stakeInput,
                            onValueChange = { stakeInput = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("၁ ကောင်ချင်း ထိုးကြေး (Ks)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("six_animals_stake_input")
                        )

                        Surface(
                            color = Color(0xFFEDF2EF),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ရွေးထားသည့်အကောင်:", color = InkMuted)
                                    Text("${selectedAnimals.size} ကောင်", color = InkDark, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("စုစုပေါင်း ထိုးကြေး:", color = InkMuted)
                                    Text(formatKs(totalStake), color = InkDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("အမြင့်ဆုံး ပြန်ရနိုင်ချေ (3တူ):", color = PitchEmeraldDark)
                                    Text(formatKs(maxOneHitPayout), color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                isRollingVisual = true
                                animScope.launch {
                                    repeat(7) {
                                        displayRolledIndices = listOf((0..5).random(), (0..5).random(), (0..5).random())
                                        delay(65L)
                                    }
                                }
                                onPlaySixAnimals(
                                    selectedAnimals,
                                    SIX_ANIMALS_LIST.map { it.title },
                                    parsedStake
                                ) { rolls, round ->
                                    animScope.launch {
                                        delay(420L)
                                        lastRolledIndices = rolls
                                        displayRolledIndices = rolls
                                        lastRound = round
                                        isRollingVisual = false
                                    }
                                }
                            },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("six_animals_play_button")
                        ) {
                            if (busy || isRollingVisual) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("၆ကောင်ဂျင် လှိမ့်နေသည်...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("၆ကောင်ဂျင် လှိမ့်မည် (${selectedAnimals.size} ကောင် • ${formatKs(totalStake)})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (animalHistory.isNotEmpty()) {
                item {
                    HideBarSectionHeader(
                        title = "၆ကောင်ဂျင် လတ်တလော မှတ်တမ်း",
                        countBadge = "${animalHistory.size}",
                        isVisible = showAnimalHistory,
                        onToggle = { showAnimalHistory = !showAnimalHistory },
                        testTag = "hide_bar_six_animals_history",
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
                if (showAnimalHistory) {
                    items(animalHistory.take(15), key = { it.id }) { round ->
                        MiniGameRoundCard(round)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DragonTigerGameScreen(
    balanceKs: Long,
    systemGameConfig: SystemGameConfig,
    miniGameControlConfig: MiniGameControlConfig = MiniGameControlConfig(),
    roundsState: UiState<List<MiniGameRound>>,
    busy: Boolean,
    onBack: () -> Unit,
    onPlayDragonTiger: (
        pick: String,
        stakeKs: Long,
        onResult: (MiniGameEngine.DragonTigerRoll, MiniGameRound) -> Unit
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

    var selectedPick by remember { mutableStateOf("DRAGON") }
    var stakeInput by remember { mutableStateOf("1000") }
    var lastRoll by remember { mutableStateOf(MiniGameEngine.DragonTigerRoll(12, "♠", 9, "♥")) }
    var isFlipping by remember { mutableStateOf(false) }
    var lastRound by remember { mutableStateOf<MiniGameRound?>(null) }
    var showHistory by remember { mutableStateOf(true) }
    val animScope = rememberCoroutineScope()

    val cardScale by animateFloatAsState(
        targetValue = if (isFlipping) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "dt_card_pop"
    )

    val parsedStake = stakeInput.toLongOrNull() ?: 0L
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)
    val canPlay = !busy && !isFlipping && !isGamesPaused && parsedStake in minStake..maxStake && parsedStake <= balanceKs

    val dtMult = miniGameControlConfig.dragonTigerMult.coerceIn(1.1, 50.0)
    val previewMult = if (selectedPick == "TIE") 8.0 else dtMult
    val dtHistory = remember(roundsState) {
        ((roundsState as? UiState.Success)?.data ?: emptyList()).filter { it.gameType == "dragon_tiger" }
    }

    fun rankStr(rank: Int): String = when (rank) {
        1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> rank.toString()
    }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "နဂါး vs ကျား (Dragon Tiger)", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isGamesPaused) {
                item {
                    PauseStatusBannerCard(
                        title = if (systemGameConfig.maintenanceMode) systemGameConfig.maintenanceMessage.ifBlank { "စနစ်ပြုပြင်နေပါသည်" }
                        else "ဂိမ်းကစားခြင်း ခေတ္တရပ်နားထားပါသည်",
                        reopenAtMs = systemGameConfig.gamesReopenAtMs,
                        nowMs = nowMs
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.5.dp, ChampionshipGold.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("လက်ကျန်: ${formatKs(balanceKs)}", color = ChampionshipGold, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            Text("အမြန်ဖဲဖွင့်ဂိမ်း • Tie x8", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.labelMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF7F1D1D),
                                border = BorderStroke(2.dp, ChampionshipGold),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🐉 နဂါး", color = ChampionshipGoldBright, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (isFlipping) "?" else "${rankStr(lastRoll.dragonRank)}${lastRoll.dragonSuit}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text("VS", color = ChampionshipGoldBright, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Surface(
                                color = Color(0xFF92400E),
                                border = BorderStroke(2.dp, ChampionshipGold),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🐯 ကျား", color = ChampionshipGoldBright, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (isFlipping) "?" else "${rankStr(lastRoll.tigerRank)}${lastRoll.tigerSuit}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        lastRound?.let { r ->
                            Surface(
                                color = if (r.payoutKs > 0L) PitchEmeraldDark else AdminCrimson.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (r.payoutKs > 0L) "🎉 အနိုင်ရ! ${r.outcomeLabel} → +${formatKs(r.payoutKs)}"
                                    else "မပေါက်သေးပါ (${r.outcomeLabel})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ထိုးမည့်ဘက် ရွေးချယ်ပါ", color = InkDark, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("DRAGON", "🐉 နဂါး x${dtMult}", Color(0xFFB91C1C)),
                                Triple("TIE", "⚖️ သရေ x8", Color(0xFF047857)),
                                Triple("TIGER", "🐯 ကျား x${dtMult}", Color(0xFFD97706))
                            ).forEach { (key, label, tint) ->
                                val isSel = selectedPick == key
                                Surface(
                                    color = if (isSel) tint else Color(0xFFEDF2EF),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) ChampionshipGold else PearlCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPick = key }
                                        .testTag("dragon_tiger_pick_${key.lowercase()}")
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.White else InkDark,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { amt ->
                                FilterChip(
                                    selected = parsedStake == amt,
                                    onClick = { stakeInput = amt.toString() },
                                    label = { Text("$amt") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = stakeInput,
                            onValueChange = { stakeInput = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("ထိုးကြေး (Ks)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = InkDark, unfocusedTextColor = InkDark),
                            modifier = Modifier.fillMaxWidth().testTag("dragon_tiger_stake_input")
                        )

                        Text(
                            "နိုင်ရင် ရရှိမည်: ${formatKs((parsedStake * previewMult).roundToLong())}",
                            color = PitchEmeraldDark,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                isFlipping = true
                                onPlayDragonTiger(selectedPick, parsedStake) { roll, round ->
                                    animScope.launch {
                                        delay(380L)
                                        lastRoll = roll
                                        lastRound = round
                                        isFlipping = false
                                    }
                                }
                            },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("dragon_tiger_play_button")
                        ) {
                            if (busy || isFlipping) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("ဖဲဖွင့်နေသည်...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("ဖဲဖွင့် ကစားမည်", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (dtHistory.isNotEmpty()) {
                item {
                    HideBarSectionHeader(
                        title = "နဂါး vs ကျား လတ်တလော မှတ်တမ်း",
                        countBadge = "${dtHistory.size}",
                        isVisible = showHistory,
                        onToggle = { showHistory = !showHistory },
                        testTag = "hide_bar_dragon_tiger_history",
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
                if (showHistory) {
                    items(dtHistory.take(15), key = { it.id }) { round ->
                        MiniGameRoundCard(round)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LuckyWheelGameScreen(
    balanceKs: Long,
    systemGameConfig: SystemGameConfig,
    miniGameControlConfig: MiniGameControlConfig = MiniGameControlConfig(),
    roundsState: UiState<List<MiniGameRound>>,
    busy: Boolean,
    onBack: () -> Unit,
    onPlayLuckyWheel: (
        stakeKs: Long,
        onResult: (segmentIndex: Int, MiniGameRound) -> Unit
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

    val segments = remember(miniGameControlConfig.luckyWheelMult) {
        MiniGameEngine.buildLuckyWheelSegments(miniGameControlConfig.luckyWheelMult)
    }
    var stakeInput by remember { mutableStateOf("1000") }
    var isSpinning by remember { mutableStateOf(false) }
    var lastRound by remember { mutableStateOf<MiniGameRound?>(null) }
    var showHistory by remember { mutableStateOf(true) }
    val wheelRotation = remember { Animatable(0f) }
    val animScope = rememberCoroutineScope()

    val parsedStake = stakeInput.toLongOrNull() ?: 0L
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)
    val canPlay = !busy && !isSpinning && !isGamesPaused && parsedStake in minStake..maxStake && parsedStake <= balanceKs

    val wheelHistory = remember(roundsState) {
        ((roundsState as? UiState.Success)?.data ?: emptyList()).filter { it.gameType == "lucky_wheel" }
    }

    val sliceColors = remember {
        listOf(
            Color(0xFF334155),
            Color(0xFF0369A1),
            Color(0xFF047857),
            Color(0xFFD97706),
            Color(0xFF475569),
            Color(0xFF7C3AED),
            Color(0xFF0F766E),
            Color(0xFFB91C1C)
        )
    }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "ကံစမ်းဘီး (Lucky Wheel)", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isGamesPaused) {
                item {
                    PauseStatusBannerCard(
                        title = if (systemGameConfig.maintenanceMode) systemGameConfig.maintenanceMessage.ifBlank { "စနစ်ပြုပြင်နေပါသည်" }
                        else "ဂိမ်းကစားခြင်း ခေတ္တရပ်နားထားပါသည်",
                        reopenAtMs = systemGameConfig.gamesReopenAtMs,
                        nowMs = nowMs
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.5.dp, ChampionshipGold.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("လက်ကျန်: ${formatKs(balanceKs)}", color = ChampionshipGold, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            Text("အမြင့်ဆုံး ဂျက်ပေါ့", color = ChampionshipGoldBright, style = MaterialTheme.typography.labelMedium)
                        }

                        // Top pointer + rotating 8-slice Canvas wheel
                        Box(
                            modifier = Modifier.size(216.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(196.dp)
                                    .graphicsLayer { rotationZ = wheelRotation.value }
                            ) {
                                val sweep = 360f / segments.size.coerceAtLeast(1)
                                segments.forEachIndexed { idx, _ ->
                                    drawArc(
                                        color = sliceColors[idx % sliceColors.size],
                                        startAngle = -90f - sweep / 2f + idx * sweep,
                                        sweepAngle = sweep,
                                        useCenter = true,
                                        size = Size(size.width, size.height)
                                    )
                                }
                                drawCircle(
                                    color = ChampionshipGold,
                                    style = Stroke(width = 6f)
                                )
                                drawCircle(
                                    color = DeepPitchBg,
                                    radius = size.minDimension * 0.14f
                                )
                                drawCircle(
                                    color = ChampionshipGoldBright,
                                    radius = size.minDimension * 0.14f,
                                    style = Stroke(width = 4f)
                                )
                            }
                            // Top stationary pointer needle
                            Canvas(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .size(22.dp)
                            ) {
                                val path = Path().apply {
                                    moveTo(size.width * 0.5f, size.height)
                                    lineTo(0f, 0f)
                                    lineTo(size.width, 0f)
                                    close()
                                }
                                drawPath(path, color = ChampionshipGoldBright)
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            segments.forEachIndexed { idx, seg ->
                                Surface(
                                    color = sliceColors[idx % sliceColors.size],
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = seg.label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        lastRound?.let { r ->
                            Surface(
                                color = if (r.payoutKs > 0L) PitchEmeraldDark else AdminCrimson.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (r.payoutKs > 0L) "🎡 အနိုင်ရ! ${r.outcomeLabel} → +${formatKs(r.payoutKs)}"
                                    else "ဗလာ (${r.outcomeLabel}) — ထပ်လှည့်ကြည့်ပါ!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(500L, 1000L, 3000L, 5000L, 10000L).forEach { amt ->
                                FilterChip(
                                    selected = parsedStake == amt,
                                    onClick = { stakeInput = amt.toString() },
                                    label = { Text("$amt") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = stakeInput,
                            onValueChange = { stakeInput = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("လှည့်ကြေး (Ks)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = InkDark, unfocusedTextColor = InkDark),
                            modifier = Modifier.fillMaxWidth().testTag("lucky_wheel_stake_input")
                        )

                        Button(
                            onClick = {
                                isSpinning = true
                                onPlayLuckyWheel(parsedStake) { segIdx, round ->
                                    animScope.launch {
                                        val sweep = 360f / segments.size.coerceAtLeast(1)
                                        val currentMod = wheelRotation.value % 360f
                                        val desiredMod = (360f - (segIdx * sweep)) % 360f
                                        val deltaMod = ((desiredMod - currentMod) + 360f) % 360f
                                        val target = wheelRotation.value + 1080f + deltaMod
                                        wheelRotation.animateTo(
                                            targetValue = target,
                                            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                                        )
                                        lastRound = round
                                        isSpinning = false
                                    }
                                }
                            },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("lucky_wheel_play_button")
                        ) {
                            if (busy || isSpinning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("ဘီးလှည့်နေသည်...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("ကံစမ်းဘီး လှည့်မည်", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (wheelHistory.isNotEmpty()) {
                item {
                    HideBarSectionHeader(
                        title = "ကံစမ်းဘီး လတ်တလော မှတ်တမ်း",
                        countBadge = "${wheelHistory.size}",
                        isVisible = showHistory,
                        onToggle = { showHistory = !showHistory },
                        testTag = "hide_bar_lucky_wheel_history",
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }
                if (showHistory) {
                    items(wheelHistory.take(15), key = { it.id }) { round ->
                        MiniGameRoundCard(round)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniGameRoundCard(round: MiniGameRound) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PearlCard),
        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(round.betLabel, color = InkDark, fontWeight = FontWeight.Bold)
                Text("${round.outcomeLabel} • ${formatTimestamp(round.createdAt)}", color = InkMuted, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (round.payoutKs > 0L) "+${formatKs(round.payoutKs)}" else "-${formatKs(round.stakeKs)}",
                    color = if (round.payoutKs > 0L) PitchEmeraldDark else AdminCrimson,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text("Stake ${formatKs(round.stakeKs)}", color = InkMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
