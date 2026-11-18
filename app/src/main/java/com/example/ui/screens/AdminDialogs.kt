package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UserAccount
import com.example.ui.theme.AdminCrimson

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
