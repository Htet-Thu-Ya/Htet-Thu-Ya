package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.betting.SettlementCalculator
import com.example.data.model.BetOrder
import com.example.data.model.BettingConfig
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UserAccount
import com.example.data.model.WalletRequest
import com.example.ui.components.OrderSlipCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatKs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark

@Composable
fun AddMatchDialog(
    onDismiss: () -> Unit,
    onSave: (
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double,
        goalOdds: Double
    ) -> Unit
) {
    var league by remember { mutableStateOf("Premier League") }
    var home by remember { mutableStateOf("") }
    var away by remember { mutableStateOf("") }
    var kickoff by remember { mutableStateOf("Tonight 21:00") }
    var handicap by remember { mutableStateOf("Home -0.5") }
    var goalLine by remember { mutableStateOf("2.5") }
    var bodyOdds by remember { mutableStateOf("1.92") }
    var goalOdds by remember { mutableStateOf("1.90") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ပွဲစဉ်အသစ် ထည့်သွင်းရန်") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = league, onValueChange = { league = it }, label = { Text("လိဂ်") }, singleLine = true)
                OutlinedTextField(value = home, onValueChange = { home = it }, label = { Text("အိမ်ရှင် အသင်း") }, singleLine = true)
                OutlinedTextField(value = away, onValueChange = { away = it }, label = { Text("ဧည့်သည် အသင်း") }, singleLine = true)
                OutlinedTextField(value = kickoff, onValueChange = { kickoff = it }, label = { Text("ကန်ချိန်") }, singleLine = true)
                OutlinedTextField(value = handicap, onValueChange = { handicap = it }, label = { Text("ဘော်ဒီ ကြေး") }, singleLine = true)
                OutlinedTextField(value = goalLine, onValueChange = { goalLine = it }, label = { Text("ဂိုးပေါင်း") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bodyOdds,
                        onValueChange = { bodyOdds = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Body Odds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = goalOdds,
                        onValueChange = { goalOdds = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Goal Odds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        league,
                        home,
                        away,
                        kickoff,
                        handicap,
                        goalLine,
                        bodyOdds.toDoubleOrNull() ?: 1.90,
                        goalOdds.toDoubleOrNull() ?: 1.90
                    )
                },
                enabled = league.isNotBlank() && home.isNotBlank() && away.isNotBlank()
            ) {
                Text("သိမ်းဆည်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@Composable
fun EditMatchResultDialog(
    match: SportMatch,
    onDismiss: () -> Unit,
    onUpdate: (
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
        awayScore: Long
    ) -> Unit,
    onRequestDelete: () -> Unit
) {
    var league by remember(match.id) { mutableStateOf(match.league) }
    var homeTeam by remember(match.id) { mutableStateOf(match.homeTeam) }
    var awayTeam by remember(match.id) { mutableStateOf(match.awayTeam) }
    var kickoffLabel by remember(match.id) { mutableStateOf(match.kickoffLabel) }
    var handicapLine by remember(match.id) { mutableStateOf(match.handicapLine) }
    var goalLine by remember(match.id) { mutableStateOf(match.goalLine) }
    var bodyOdds by remember(match.id) { mutableStateOf(match.bodyOdds.toString()) }
    var goalOdds by remember(match.id) { mutableStateOf(match.goalOdds.toString()) }
    var status by remember(match.id) { mutableStateOf(match.status) }
    var homeScore by remember(match.id) { mutableStateOf(match.homeScore.toString()) }
    var awayScore by remember(match.id) { mutableStateOf(match.awayScore.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ပွဲစဉ် ပြင်ဆင်ရန်") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("open" to "ဖွင့်", "closed" to "ပိတ်", "finished" to "ပြီးဆုံး").forEach { (st, label) ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = homeScore,
                        onValueChange = { homeScore = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("အိမ်ရှင် ဂိုး") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = awayScore,
                        onValueChange = { awayScore = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("ဧည့်သည် ဂိုး") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = league, onValueChange = { league = it }, label = { Text("လိဂ်") }, singleLine = true)
                OutlinedTextField(value = homeTeam, onValueChange = { homeTeam = it }, label = { Text("အိမ်ရှင် အသင်း") }, singleLine = true)
                OutlinedTextField(value = awayTeam, onValueChange = { awayTeam = it }, label = { Text("ဧည့်သည် အသင်း") }, singleLine = true)
                OutlinedTextField(value = kickoffLabel, onValueChange = { kickoffLabel = it }, label = { Text("ကန်ချိန်") }, singleLine = true)
                OutlinedTextField(value = handicapLine, onValueChange = { handicapLine = it }, label = { Text("ဘော်ဒီ ကြေး") }, singleLine = true)
                OutlinedTextField(value = goalLine, onValueChange = { goalLine = it }, label = { Text("ဂိုးပေါင်း") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bodyOdds,
                        onValueChange = { bodyOdds = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Body Odds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = goalOdds,
                        onValueChange = { goalOdds = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Goal Odds") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                TextButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.testTag("dialog_delete_match_button")
                ) {
                    Text("ဤပွဲစဉ်ကို ဖျက်မည်", color = AdminCrimson)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(
                        league,
                        homeTeam,
                        awayTeam,
                        kickoffLabel,
                        handicapLine,
                        goalLine,
                        bodyOdds.toDoubleOrNull() ?: match.bodyOdds,
                        goalOdds.toDoubleOrNull() ?: match.goalOdds,
                        status,
                        homeScore.toLongOrNull() ?: 0L,
                        awayScore.toLongOrNull() ?: 0L
                    )
                },
                enabled = league.isNotBlank() && homeTeam.isNotBlank() && awayTeam.isNotBlank(),
                modifier = Modifier.testTag("dialog_save_match_button")
            ) {
                Text("သိမ်းဆည်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@Composable
fun AddLotteryDrawDialog(
    onDismiss: () -> Unit,
    onSave: (roundTitle: String, closeTimeLabel: String, multiplier: Double) -> Unit
) {
    var title by remember { mutableStateOf("3D Round") }
    var closeLabel by remember { mutableStateOf("3:30 PM Close") }
    var mult by remember { mutableStateOf("550") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("၃လုံးထီ အကြိမ်သစ် ဖွင့်ရန်") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("အကြိမ်အမည်") }, singleLine = true)
                OutlinedTextField(value = closeLabel, onValueChange = { closeLabel = it }, label = { Text("ပိတ်ချိန်") }, singleLine = true)
                OutlinedTextField(
                    value = mult,
                    onValueChange = { mult = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("အဆ (ဥပမာ 550)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, closeLabel, mult.toDoubleOrNull() ?: 550.0) },
                enabled = title.isNotBlank()
            ) {
                Text("ဖွင့်လှစ်မည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@Composable
fun PublishLotteryResultDialog(
    draw: LotteryDraw,
    onDismiss: () -> Unit,
    onPublish: (
        roundTitle: String,
        closeTimeLabel: String,
        payoutMultiplier: Double,
        winningNumber: String,
        status: String
    ) -> Unit,
    onRequestDelete: () -> Unit
) {
    var roundTitle by remember(draw.id) { mutableStateOf(draw.roundTitle) }
    var closeTimeLabel by remember(draw.id) { mutableStateOf(draw.closeTimeLabel) }
    var mult by remember(draw.id) { mutableStateOf(draw.payoutMultiplier.toInt().toString()) }
    var winNum by remember(draw.id) { mutableStateOf(draw.winningNumber) }
    var status by remember(draw.id) { mutableStateOf(draw.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("၃လုံးထီ ပြင်ဆင်ရန်") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("open" to "ဖွင့်", "closed" to "ပိတ်", "finished" to "ဂဏန်းထွက်").forEach { (st, lbl) ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(lbl) })
                    }
                }
                OutlinedTextField(value = roundTitle, onValueChange = { roundTitle = it }, label = { Text("အကြိမ်အမည်") }, singleLine = true)
                OutlinedTextField(value = closeTimeLabel, onValueChange = { closeTimeLabel = it }, label = { Text("ပိတ်ချိန်") }, singleLine = true)
                OutlinedTextField(
                    value = mult,
                    onValueChange = { mult = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("အဆ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = winNum,
                    onValueChange = { winNum = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("ထွက်ဂဏန်း (၃ လုံး)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.testTag("dialog_delete_draw_button")
                ) {
                    Text("ဤ ၃လုံးထီအကြိမ်ကို ဖျက်မည်", color = AdminCrimson)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPublish(
                        roundTitle,
                        closeTimeLabel,
                        mult.toDoubleOrNull() ?: draw.payoutMultiplier,
                        winNum,
                        status
                    )
                },
                enabled = roundTitle.isNotBlank()
            ) {
                Text("သိမ်းဆည်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("ဖျက်မည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မဖျက်ပါ") }
        }
    )
}

@Composable
fun AdjustUserBalanceDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onAdjust: (deltaKs: Long, note: String) -> Unit
) {
    var amountInput by remember { mutableStateOf("5000") }
    var isAdd by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("Admin ငွေစာရင်းပြင်ဆင်မှု") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${user.displayName} လက်ကျန်ငွေ ပြင်ရန်") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (user.email.isNotBlank()) {
                    Text(user.email, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isAdd, onClick = { isAdd = true }, label = { Text("+ ထည့်မည်") })
                    FilterChip(selected = !isAdd, onClick = { isAdd = false }, label = { Text("- နှုတ်မည်") })
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("ပမာဏ (Ks)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("မှတ်ချက်") },
                    singleLine = true
                )
                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toLongOrNull() ?: 0L
                    if (amt > 0L) onAdjust(if (isAdd) amt else -amt, note)
                }
            ) { Text("အတည်ပြုမည်") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GiftRewardPointsDialog(
    user: UserAccount,
    onDismiss: () -> Unit,
    onGift: (deltaPoints: Long, note: String) -> Unit
) {
    var pointsInput by remember { mutableStateOf("100") }
    var isAdd by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("Admin မှ လက်ဆောင်ပွိုင့်") }
    val parsedPts = pointsInput.toLongOrNull() ?: 0L
    val cannotDeductZero = !isAdd && user.rewardPoints <= 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isAdd) "${user.displayName} ထံ ပွိုင့်လက်ဆောင်ပေးရန်"
                else "${user.displayName} ထံမှ ပွိုင့်ပြန်နှုတ်ရန်"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (user.email.isNotBlank()) {
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                }
                Text("လက်ရှိပွိုင့်: ${user.rewardPoints} Pts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAdd,
                        onClick = {
                            isAdd = true
                            if (note == "Admin မှ ပွိုင့်ပြန်နှုတ်ခြင်း") note = "Admin မှ လက်ဆောင်ပွိုင့်"
                        },
                        label = { Text("+ လက်ဆောင်ပေးမည်") }
                    )
                    FilterChip(
                        selected = !isAdd,
                        onClick = {
                            isAdd = false
                            if (note == "Admin မှ လက်ဆောင်ပွိုင့်") note = "Admin မှ ပွိုင့်ပြန်နှုတ်ခြင်း"
                        },
                        label = { Text("- ပြန်နှုတ်မည်") }
                    )
                }
                if (cannotDeductZero) {
                    Text(
                        "လက်ရှိပွိုင့် 0 Pts ဖြစ်နေသောကြောင့် ပြန်နှုတ်၍ မရနိုင်ပါ",
                        color = AdminCrimson,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50L, 100L, 500L, 1000L).forEach { preset ->
                        FilterChip(
                            selected = parsedPts == preset,
                            onClick = { pointsInput = preset.toString() },
                            label = { Text("$preset Pts") }
                        )
                    }
                }
                OutlinedTextField(
                    value = pointsInput,
                    onValueChange = { pointsInput = it.filter { c -> c.isDigit() }.take(7) },
                    label = { Text("ပွိုင့်ပမာဏ (Pts)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("gift_points_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("မှတ်ချက်") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("gift_points_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedPts > 0L) {
                        val fallback = if (isAdd) "Admin လက်ဆောင်ပွိုင့်" else "Admin မှ ပွိုင့်ပြန်နှုတ်ခြင်း"
                        onGift(if (isAdd) parsedPts else -parsedPts, note.ifBlank { fallback })
                    }
                },
                enabled = parsedPts > 0L && !cannotDeductZero,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAdd) PitchEmeraldDark else AdminCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_gift_points_button")
            ) {
                Text(if (isAdd) "လက်ဆောင်ပို့မည်" else "ပွိုင့်နှုတ်မည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettleWinDialog(
    order: BetOrder,
    bettingConfig: BettingConfig,
    onDismiss: () -> Unit,
    onConfirm: (commissionPercent: Double, grossPayoutKs: Long, note: String) -> Unit
) {
    val initialPctStr = if (bettingConfig.winCommissionPercent % 1.0 == 0.0) {
        bettingConfig.winCommissionPercent.toInt().toString()
    } else {
        bettingConfig.winCommissionPercent.toString()
    }
    var grossInput by remember(order.id) { mutableStateOf(order.potentialPayoutKs.toString()) }
    var commissionPctInput by remember(order.id) { mutableStateOf(initialPctStr) }
    var note by remember(order.id) { mutableStateOf("အနိုင်ရ - ငွေရှင်းပြီး") }

    val grossKs = (grossInput.toLongOrNull() ?: order.potentialPayoutKs).coerceAtLeast(1L)
    val pct = (commissionPctInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 50.0)
    val (commissionKs, netPayoutKs) = bettingConfig.computeCommissionAndNet(order.stakeKs, grossKs, pct)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = PitchEmeraldDark) },
        title = { Text("အနိုင်ငွေရှင်း & အကောက်သတ်မှတ်ရန်") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${order.customerName} • ထိုးကြေး: ${formatKs(order.stakeKs)}",
                    fontWeight = FontWeight.Bold
                )
                order.selections.take(4).forEach { sel ->
                    Text("• $sel", style = MaterialTheme.typography.bodySmall)
                }
                if (order.selections.size > 4) {
                    Text("... +${order.selections.size - 4} ပွဲ", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = grossInput,
                    onValueChange = { grossInput = it.filter { c -> c.isDigit() }.take(9) },
                    label = { Text("မူလအနိုင်ရငွေ (Ks)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settle_win_gross_input")
                )
                OutlinedTextField(
                    value = commissionPctInput,
                    onValueChange = { commissionPctInput = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = {
                        val basisLabel = if (bettingConfig.winCommissionBasis == "profit") "အမြတ်ငွေပေါ်" else "စုစုပေါင်းအလျော်ငွေပေါ်"
                        Text("အနိုင်အကောက် % ($basisLabel)")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settle_win_commission_input")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.0 to "0%", 5.0 to "5%", 10.0 to "10%", 15.0 to "15%").forEach { (preset, lbl) ->
                        FilterChip(
                            selected = pct == preset,
                            onClick = { commissionPctInput = preset.toInt().toString() },
                            label = { Text(lbl) }
                        )
                    }
                }
                Surface(
                    color = DeepPitchSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("နုတ်ယူမည့် အကောက်: -${formatKs(commissionKs)}", color = Color(0xFFFCA5A5), fontFamily = JetBrainsMonoFontFamily)
                        Text("Customer ရရှိမည့် အသားတင်: ${formatKs(netPayoutKs)}", color = ChampionshipGold, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFontFamily)
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("မှတ်ချက်") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pct, grossKs, note) },
                enabled = grossKs > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_settle_win_button")
            ) {
                Text("အနိုင်ငွေပေးချေမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("မလုပ်ပါ") }
        }
    )
}

@Composable
fun AdminCustomerDetailDialog(
    user: UserAccount,
    customerOrders: List<BetOrder>,
    customerWalletReqs: List<WalletRequest>,
    busy: Boolean = false,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onAutoSettleCustomer: () -> Unit = {},
    onOpenAdjustBalance: () -> Unit,
    onOpenGiftPoints: () -> Unit,
    onAcceptOrder: (BetOrder) -> Unit,
    onRequestSettleWin: (BetOrder) -> Unit,
    onSettleLoss: (BetOrder) -> Unit,
    onRejectOrder: (BetOrder) -> Unit,
    onDeleteOrder: (String) -> Unit,
    onProcessWalletRequest: (WalletRequest, Boolean, String) -> Unit,
    onDeleteWalletRequest: (String) -> Unit
) {
    var detailTab by remember { mutableIntStateOf(0) }
    val activeOrders = remember(customerOrders) {
        customerOrders.filter { it.status in listOf("pending", "accepted") }
    }
    val settledOrders = remember(customerOrders) {
        customerOrders.filter { it.status in listOf("won", "lost", "rejected") }
    }
    val customerSummary = remember(user, customerOrders) {
        SettlementCalculator.summarizeCustomer(user.userId, user, customerOrders)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            shape = RoundedCornerShape(20.dp),
            color = DeepPitchBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(color = DeepPitchElevated, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = user.displayName.ifBlank { "Member" },
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = user.email.ifBlank { "Gmail မရှိသေးပါ" },
                                        color = Color(0xFFC5D7CE),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onRefresh, modifier = Modifier.testTag("customer_detail_refresh")) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ChampionshipGold)
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("customer_detail_close")) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }

                // Account Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("လက်ကျန်ငွေ", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                Text(formatKs(user.balanceKs), color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ပွိုင့်", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                Text("${user.rewardPoints} Pts", color = InkDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Active လောင်းငွေ", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                Text(formatKs(user.activeStakeKs), color = InkDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("နိုင်: ${customerSummary.wonCount} ခု (${formatKs(customerSummary.totalWonPayoutKs)})", color = PitchEmeraldDark, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("ရှုံး: ${customerSummary.lostCount} ခု (${formatKs(customerSummary.totalLostStakeKs)})", color = AdminCrimson, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        val netLabel = when {
                            customerSummary.customerNetKs > 0L -> "Customer အသားတင်: +${formatKs(customerSummary.customerNetKs)} (နိုင်)"
                            customerSummary.customerNetKs < 0L -> "Customer အသားတင်: -${formatKs(kotlin.math.abs(customerSummary.customerNetKs))} (ရှုံး)"
                            else -> "Customer အသားတင်: 0 Ks"
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = netLabel,
                                color = if (customerSummary.customerNetKs >= 0L) PitchEmeraldDark else AdminCrimson,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text("စာရင်းဝင်: ${formatTimestamp(user.createdAt)}", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("UID: ${user.userId}", color = InkMuted, style = MaterialTheme.typography.labelSmall, fontFamily = JetBrainsMonoFontFamily)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = onOpenAdjustBalance, modifier = Modifier.weight(1f)) {
                                Text("ငွေပြင် (+/-)", color = InkDark)
                            }
                            Button(
                                onClick = onOpenGiftPoints,
                                colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ပွိုင့် +/-", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (activeOrders.isNotEmpty()) {
                            Button(
                                onClick = onAutoSettleCustomer,
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_detail_auto_settle_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ဤ Customer အော်ဒါများ (${activeOrders.size}) Auto ရှင်းမည်", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = detailTab,
                    containerColor = DeepPitchSurface,
                    contentColor = ChampionshipGold
                ) {
                    Tab(
                        selected = detailTab == 0,
                        onClick = { detailTab = 0 },
                        text = { Text("လက်ရှိလောင်း (${activeOrders.size})") }
                    )
                    Tab(
                        selected = detailTab == 1,
                        onClick = { detailTab = 1 },
                        text = { Text("ပြီးဆုံး (${settledOrders.size})") }
                    )
                    Tab(
                        selected = detailTab == 2,
                        onClick = { detailTab = 2 },
                        text = { Text("ငွေစာရင်း (${customerWalletReqs.size})") }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (detailTab) {
                        0 -> {
                            if (activeOrders.isEmpty()) {
                                item {
                                    Text("လက်ရှိတင်ထားသော လောင်းကြေးမရှိသေးပါ", color = Color(0xFFB4C8BE), modifier = Modifier.padding(16.dp))
                                }
                            } else {
                                items(activeOrders, key = { it.id }) { order ->
                                    OrderSlipCard(
                                        order = order,
                                        isAdminView = true,
                                        onAccept = onAcceptOrder,
                                        onSettleWin = onRequestSettleWin,
                                        onSettleLoss = onSettleLoss,
                                        onReject = onRejectOrder
                                    )
                                }
                            }
                        }
                        1 -> {
                            if (settledOrders.isEmpty()) {
                                item {
                                    Text("ပြီးဆုံးပြီးသော လောင်းကြေးမှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE), modifier = Modifier.padding(16.dp))
                                }
                            } else {
                                items(settledOrders, key = { it.id }) { order ->
                                    OrderSlipCard(
                                        order = order,
                                        isAdminView = true,
                                        onDeleteOrder = { onDeleteOrder(it.id) }
                                    )
                                }
                            }
                        }
                        2 -> {
                            if (customerWalletReqs.isEmpty()) {
                                item {
                                    Text("ငွေသွင်း / ငွေထုတ် မှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE), modifier = Modifier.padding(16.dp))
                                }
                            } else {
                                items(customerWalletReqs, key = { it.id }) { req ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    if (req.requestType == "topup") "ငွေသွင်း (${req.channel})" else "ငွေထုတ် (${req.channel})",
                                                    color = InkDark,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    StatusBadge(req.status)
                                                    if (req.status != "pending") {
                                                        IconButton(onClick = { onDeleteWalletRequest(req.id) }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdminCrimson)
                                                        }
                                                    }
                                                }
                                            }
                                            Text(
                                                formatKs(req.amountKs),
                                                color = PitchEmeraldDark,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("Ref: ${req.referenceNote} • ${formatTimestamp(req.createdAt)}", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                            if (req.status == "pending") {
                                                Spacer(Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { onProcessWalletRequest(req, true, "အတည်ပြုပြီး") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("အတည်ပြု")
                                                    }
                                                    OutlinedButton(
                                                        onClick = { onProcessWalletRequest(req, false, "ပယ်ဖျက်ပြီး") },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("ပယ်ဖျက်", color = AdminCrimson)
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
        }
    }
}

