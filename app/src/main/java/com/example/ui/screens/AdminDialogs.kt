package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UserAccount

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = league, onValueChange = { league = it }, label = { Text("လိဂ်") }, singleLine = true)
                OutlinedTextField(value = home, onValueChange = { home = it }, label = { Text("အိမ်ရှင် အသင်း") }, singleLine = true)
                OutlinedTextField(value = away, onValueChange = { away = it }, label = { Text("ဧည့်သည် အသင်း") }, singleLine = true)
                OutlinedTextField(value = kickoff, onValueChange = { kickoff = it }, label = { Text("ကန်ချိန်") }, singleLine = true)
                OutlinedTextField(value = handicap, onValueChange = { handicap = it }, label = { Text("ဘော်ဒီ ကြေး") }, singleLine = true)
                OutlinedTextField(value = goalLine, onValueChange = { goalLine = it }, label = { Text("ဂိုးပေါင်း") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bodyOdds,
                        onValueChange = { bodyOdds = it },
                        label = { Text("Body Odds") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = goalOdds,
                        onValueChange = { goalOdds = it },
                        label = { Text("Goal Odds") },
                        singleLine = true,
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
                enabled = home.isNotBlank() && away.isNotBlank()
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
    onUpdate: (status: String, homeScore: Long, awayScore: Long) -> Unit
) {
    var status by remember { mutableStateOf(match.status) }
    var homeScore by remember { mutableStateOf(match.homeScore.toString()) }
    var awayScore by remember { mutableStateOf(match.awayScore.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${match.homeTeam} vs ${match.awayTeam}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        label = { Text(match.homeTeam) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = awayScore,
                        onValueChange = { awayScore = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text(match.awayTeam) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdate(status, homeScore.toLongOrNull() ?: 0L, awayScore.toLongOrNull() ?: 0L)
            }) {
                Text("အတည်ပြုမည်")
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
    onPublish: (winningNumber: String, status: String) -> Unit
) {
    var winNum by remember { mutableStateOf(draw.winningNumber) }
    var status by remember { mutableStateOf(draw.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(draw.roundTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("open" to "ဖွင့်", "closed" to "ပိတ်", "finished" to "ဂဏန်းထွက်").forEach { (st, lbl) ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(lbl) })
                    }
                }
                OutlinedTextField(
                    value = winNum,
                    onValueChange = { winNum = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("ထွက်ဂဏန်း (၃ လုံး)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onPublish(winNum, status) }) {
                Text("သိမ်းဆည်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
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
