package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Casino
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Live2DStatus
import com.example.data.model.LotteryDraw
import com.example.data.model.MiniGameControlConfig
import com.example.data.model.MiniGameRound
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
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
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TwoDLotteryScreen(
    drawsState: UiState<List<LotteryDraw>>,
    live2DStatus: Live2DStatus?,
    balanceKs: Long,
    systemGameConfig: SystemGameConfig,
    busy: Boolean,
    onRefreshLive2D: () -> Unit,
    onBack: () -> Unit,
    onSubmit2DOrder: (
        selections: List<String>,
        drawRound: String,
        stakeKs: Long,
        multiplier: Double
    ) -> Unit
) {
    val selectedNumbers = remember { mutableStateListOf<String>() }
    var manualInput by remember { mutableStateOf("") }
    var includeReverse by remember { mutableStateOf(false) }
    var stakePerNumberInput by remember { mutableStateOf("500") }

    val openDraw = when (drawsState) {
        is UiState.Success -> drawsState.data.firstOrNull { it.status == "open" && it.drawType == "2d" }
        else -> null
    }
    val drawMultiplier = openDraw?.payoutMultiplier ?: systemGameConfig.twoDPayoutMultiplier
    val stakePerNumber = stakePerNumberInput.toLongOrNull() ?: 0L
    val totalStake = stakePerNumber * selectedNumbers.size
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)

    val canSubmit = !busy &&
        openDraw != null &&
        selectedNumbers.isNotEmpty() &&
        stakePerNumber >= 100L &&
        totalStake in minStake..maxStake &&
        totalStake <= balanceKs

    val rows00To99 = remember {
        (0..99).map { it.toString().padStart(2, '0') }.chunked(10)
    }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "နှစ်လုံးထီ (2D)", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            // Open Round Info
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
                                text = openDraw?.roundTitle ?: "လက်ရှိ ၂လုံးထီ အကြိမ် မဖွင့်သေးပါ",
                                color = InkDark,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
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
                        Text(
                            text = if (openDraw != null) "ပိတ်ချိန်: ${openDraw.closeTimeLabel} • လက်ကျန်: ${formatKs(balanceKs)}"
                            else "Admin မှ ၂လုံးထီ (2D) အကြိမ်သစ် ဖွင့်လှစ်ပေးရန် စောင့်ဆိုင်းပါ။",
                            color = InkMuted,
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

            // Interactive 00-99 Grid
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
                                val round = openDraw?.roundTitle ?: "2D Round"
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
    var selectedOption by remember { mutableStateOf("BIG") }
    var exactNumber by remember { mutableIntStateOf(11) }
    var stakeInput by remember { mutableStateOf("1000") }
    var lastDice by remember { mutableStateOf(listOf(4, 5, 2)) }
    var lastRound by remember { mutableStateOf<MiniGameRound?>(null) }

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
    val canPlay = !busy && parsedStake in minStake..maxStake && parsedStake <= balanceKs

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
                            Text("စုစုပေါင်း: ${lastDice.sum()}", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            lastDice.forEach { dieValue ->
                                DiceFaceVisual(value = dieValue)
                            }
                        }

                        lastRound?.let { r ->
                            Surface(
                                color = if (r.payoutKs > 0L) PitchEmeraldDark else AdminCrimson.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (r.payoutKs > 0L) "အနိုင်ရ! ${r.outcomeLabel} → +${formatKs(r.payoutKs)}"
                                    else "ရှုံးသွားပါသည် (${r.outcomeLabel})",
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
                                onPlayDice(selectedOption, exactNumber, parsedStake) { dice, round ->
                                    lastDice = dice
                                    lastRound = round
                                }
                            },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("dice_play_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
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
                    Text(
                        "လတ်တလော မှတ်တမ်း (${diceHistory.size})",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                    )
                }
                items(diceHistory.take(15), key = { it.id }) { round ->
                    MiniGameRoundCard(round)
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
        selectedAnimalIndex: Int,
        animalLabels: List<String>,
        stakeKs: Long,
        onResult: (List<Int>, MiniGameRound) -> Unit
    ) -> Unit
) {
    var selectedAnimalIndex by remember { mutableIntStateOf(0) }
    var stakeInput by remember { mutableStateOf("1000") }
    var lastRolledIndices by remember { mutableStateOf(listOf(0, 2, 5)) }
    var lastRound by remember { mutableStateOf<MiniGameRound?>(null) }

    val parsedStake = stakeInput.toLongOrNull() ?: 0L
    val minStake = systemGameConfig.minStakeKs.coerceAtLeast(100L)
    val maxStake = systemGameConfig.maxStakeKs.coerceAtLeast(minStake)
    val canPlay = !busy && parsedStake in minStake..maxStake && parsedStake <= balanceKs

    val singleMult = miniGameControlConfig.sixAnimalsMult.coerceAtLeast(1.2)
    val bonus = (singleMult - 1.0).coerceAtLeast(0.2)
    val doubleMult = 1.0 + bonus * 2.0
    val tripleAnimalMult = 1.0 + bonus * 3.0

    val animalHistory = remember(roundsState) {
        ((roundsState as? UiState.Success)?.data ?: emptyList()).filter { it.gameType == "six_animals" }
    }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "၆ကောင်ဂျင် (6 Animals)", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            lastRolledIndices.forEach { idx ->
                                val token = SIX_ANIMALS_LIST.getOrElse(idx) { SIX_ANIMALS_LIST.first() }
                                Surface(
                                    color = token.badgeColor,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, ChampionshipGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = token.title.substringBefore(" "),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp)
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
                                    text = if (r.payoutKs > 0L) "အနိုင်ရ! ${r.outcomeLabel} → +${formatKs(r.payoutKs)}"
                                    else "မပေါက်ပါ (${r.outcomeLabel})",
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
                        Text("အကောင် ၆ မျိုးမှ ၁ မျိုး ရွေးပါ", color = InkDark, fontWeight = FontWeight.Bold)
                        SIX_ANIMALS_LIST.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { token ->
                                    val isSel = selectedAnimalIndex == token.index
                                    Surface(
                                        color = if (isSel) token.badgeColor else Color(0xFFEDF2EF),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) ChampionshipGold else PearlCardBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedAnimalIndex = token.index }
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
                                                    imageVector = Icons.Default.Pets,
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
                            label = { Text("ထိုးကြေး (Ks)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("six_animals_stake_input")
                        )

                        Button(
                            onClick = {
                                onPlaySixAnimals(
                                    selectedAnimalIndex,
                                    SIX_ANIMALS_LIST.map { it.title },
                                    parsedStake
                                ) { rolls, round ->
                                    lastRolledIndices = rolls
                                    lastRound = round
                                }
                            },
                            enabled = canPlay,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("six_animals_play_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("၆ကောင်ဂျင် လှိမ့်မည်", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (animalHistory.isNotEmpty()) {
                item {
                    Text(
                        "လတ်တလော မှတ်တမ်း (${animalHistory.size})",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                    )
                }
                items(animalHistory.take(15), key = { it.id }) { round ->
                    MiniGameRoundCard(round)
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
