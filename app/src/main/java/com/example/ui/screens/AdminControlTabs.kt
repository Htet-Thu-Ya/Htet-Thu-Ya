package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminMember
import com.example.data.model.BrandingConfig
import com.example.data.model.MiniGameControlConfig
import com.example.data.model.MiniGameRound
import com.example.data.model.PaymentAccount
import com.example.data.model.SupportAgentProfile
import com.example.data.model.UserAccount
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatKs
import com.example.ui.components.formatTimestamp
import java.io.ByteArrayOutputStream
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark

private val ANIMAL_QUOTA_ENTRIES = listOf(
    "deer" to "သမင် (Deer)",
    "tiger" to "ကျား (Tiger)",
    "elephant" to "ဆင် (Elephant)",
    "rooster" to "ကြက် (Rooster)",
    "prawn" to "ပုစွန် (Prawn)",
    "fish" to "ငါး (Fish)"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMiniGameControlTab(
    config: MiniGameControlConfig,
    rounds: List<MiniGameRound>,
    users: List<UserAccount>,
    busy: Boolean,
    onSaveConfig: (MiniGameControlConfig) -> Unit,
    onSetCustomerDirective: (userId: String, customerName: String, directive: String) -> Unit
) {
    var globalDirective by remember { mutableStateOf(config.globalDirective) }
    var stealthHouseWinPct by remember { mutableIntStateOf(config.stealthHouseWinPct.coerceIn(50, 95)) }
    var diceStdMult by remember { mutableStateOf(config.diceStandardMult.toString()) }
    var diceTripleMult by remember { mutableStateOf(config.diceTripleMult.toString()) }
    var diceExactMult by remember { mutableStateOf(config.diceExactMult.toString()) }
    var sixAnimalsMult by remember { mutableStateOf(config.sixAnimalsMult.toString()) }
    var dragonTigerMult by remember { mutableStateOf(config.dragonTigerMult.toString()) }
    var luckyWheelMult by remember { mutableStateOf(config.luckyWheelMult.toString()) }

    var allowedTotals by remember { mutableStateOf(config.diceAllowedTotals.toSet()) }
    var maxBig by remember { mutableStateOf(config.diceMaxBigPerDay.toString()) }
    var maxSmall by remember { mutableStateOf(config.diceMaxSmallPerDay.toString()) }
    var maxOdd by remember { mutableStateOf(config.diceMaxOddPerDay.toString()) }
    var maxEven by remember { mutableStateOf(config.diceMaxEvenPerDay.toString()) }

    val totalQuotas = remember { mutableStateMapOf<String, Long>() }
    var selectedQuotaSum by remember { mutableIntStateOf(11) }
    var selectedQuotaCap by remember { mutableStateOf("5") }

    val animalQuotaInputs = remember { mutableStateMapOf<String, String>() }

    var showDiceQuotaSection by remember { mutableStateOf(true) }
    var showAnimalQuotaSection by remember { mutableStateOf(true) }
    var showUserDirectivesSection by remember { mutableStateOf(true) }
    var showRecentRoundsSection by remember { mutableStateOf(true) }
    var customerSearch by remember { mutableStateOf("") }

    LaunchedEffect(config) {
        globalDirective = config.globalDirective
        stealthHouseWinPct = config.stealthHouseWinPct.coerceIn(50, 95)
        diceStdMult = config.diceStandardMult.toString()
        diceTripleMult = config.diceTripleMult.toString()
        diceExactMult = config.diceExactMult.toString()
        sixAnimalsMult = config.sixAnimalsMult.toString()
        dragonTigerMult = config.dragonTigerMult.toString()
        luckyWheelMult = config.luckyWheelMult.toString()
        allowedTotals = config.diceAllowedTotals.toSet()
        maxBig = config.diceMaxBigPerDay.toString()
        maxSmall = config.diceMaxSmallPerDay.toString()
        maxOdd = config.diceMaxOddPerDay.toString()
        maxEven = config.diceMaxEvenPerDay.toString()
        totalQuotas.clear()
        totalQuotas.putAll(config.diceTotalDailyQuotas)
        animalQuotaInputs.clear()
        ANIMAL_QUOTA_ENTRIES.forEach { (key, _) ->
            animalQuotaInputs[key] = (config.animalDailyQuotas[key] ?: 0L).toString()
        }
    }

    val filteredUsers = remember(users, customerSearch) {
        val q = customerSearch.trim().lowercase()
        if (q.isBlank()) users else users.filter {
            it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    fun buildAndSaveConfig() {
        val parsedAnimalQuotas = ANIMAL_QUOTA_ENTRIES.mapNotNull { (key, _) ->
            val v = (animalQuotaInputs[key]?.toLongOrNull() ?: 0L).coerceAtLeast(0L)
            if (v > 0L) key to v else null
        }.toMap()
        onSaveConfig(
            config.copy(
                diceStandardMult = (diceStdMult.toDoubleOrNull() ?: config.diceStandardMult).coerceIn(1.1, 50.0),
                diceTripleMult = (diceTripleMult.toDoubleOrNull() ?: config.diceTripleMult).coerceIn(1.1, 100.0),
                diceExactMult = (diceExactMult.toDoubleOrNull() ?: config.diceExactMult).coerceIn(1.1, 100.0),
                sixAnimalsMult = (sixAnimalsMult.toDoubleOrNull() ?: config.sixAnimalsMult).coerceIn(1.1, 30.0),
                dragonTigerMult = (dragonTigerMult.toDoubleOrNull() ?: config.dragonTigerMult).coerceIn(1.1, 50.0),
                luckyWheelMult = (luckyWheelMult.toDoubleOrNull() ?: config.luckyWheelMult).coerceIn(1.1, 50.0),
                diceAllowedTotals = allowedTotals.sorted(),
                diceMaxBigPerDay = (maxBig.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                diceMaxSmallPerDay = (maxSmall.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                diceMaxOddPerDay = (maxOdd.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                diceMaxEvenPerDay = (maxEven.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                diceTotalDailyQuotas = totalQuotas.filterValues { it > 0L }.toMap(),
                animalDailyQuotas = parsedAnimalQuotas,
                globalDirective = globalDirective,
                stealthHouseWinPct = stealthHouseWinPct.coerceIn(50, 95)
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Multipliers & Global Directive Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ဂိမ်းခန်း အဆနှင့် ထိန်းချုပ်မှု (Dice, 6 Animals, Dragon Tiger & Wheel)",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("ပင်မရလဒ် ထိန်းချုပ်မှု (Global Directive):", color = Color.White, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "auto" to "ပုံမှန် (Auto)",
                            "stealth_house" to "ဒိုင်အနိုင် (Stealth Auto)",
                            "force_win" to "အနိုင်ပေး (Win)",
                            "force_loss" to "အရှုံးပေး (Loss)"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = globalDirective == key,
                                onClick = { globalDirective = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("mini_game_global_dir_$key")
                            )
                        }
                    }

                    Surface(
                        color = DeepPitchSurface,
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ဒိုင်အနိုင်နှုန်း (Stealth House Edge): $stealthHouseWinPct%",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ကစားသမား သတိမထားမိစေဘဲ ကပ်ရှုံး/သဘာဝကျသော ရလဒ်များဖြင့် ဒိုင်အသာစီးရစေသည် (လောင်းကြေးကြီး/နိုင်ကြိမ်များလျှင် အလိုအလျောက် ထိန်းပေးသည်)",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(65, 75, 78, 85, 90).forEach { pct ->
                                    FilterChip(
                                        selected = stealthHouseWinPct == pct,
                                        onClick = { stealthHouseWinPct = pct },
                                        label = { Text("$pct%") },
                                        modifier = Modifier.testTag("stealth_house_pct_$pct")
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = diceStdMult,
                            onValueChange = { diceStdMult = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("ကြီး/သေး/စုံ/မ အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("dice_std_mult_input")
                        )
                        OutlinedTextField(
                            value = diceTripleMult,
                            onValueChange = { diceTripleMult = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("၃လုံးတူ (Triple) အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("dice_triple_mult_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = diceExactMult,
                            onValueChange = { diceExactMult = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("ပေါင်းလဒ်တိကျ (Exact)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("dice_exact_mult_input")
                        )
                        OutlinedTextField(
                            value = sixAnimalsMult,
                            onValueChange = { sixAnimalsMult = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("၆ကောင်ဂျင် အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("six_animals_mult_input")
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dragonTigerMult,
                            onValueChange = { dragonTigerMult = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("နဂါး vs ကျား အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("dragon_tiger_mult_input")
                        )
                        OutlinedTextField(
                            value = luckyWheelMult,
                            onValueChange = { luckyWheelMult = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                            label = { Text("ကံစမ်းဘီး အခြေခံအဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("lucky_wheel_mult_input")
                        )
                    }

                    Button(
                        onClick = { buildAndSaveConfig() },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier.fillMaxWidth().testTag("save_mini_game_control_top_button")
                    ) {
                        Text("အဆနှင့် ထိန်းချုပ်မှု သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dice Quotas & Allowed Totals Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDiceQuotaSection = !showDiceQuotaSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "အံစာတုံး နေ့စဉ် နံပါတ်/စုံမ/ကြီးသေး ကန့်သတ်ချက်",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showDiceQuotaSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle dice quotas",
                            tint = ChampionshipGold
                        )
                    }

                    if (showDiceQuotaSection) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (allowedTotals.isEmpty()) "ခွင့်ပြုပေါင်းလဒ်: အားလုံး (3-18)"
                                else "ခွင့်ပြုပေါင်းလဒ်: ${allowedTotals.sorted().joinToString(", ")}",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            if (allowedTotals.isNotEmpty()) {
                                TextButton(onClick = { allowedTotals = emptySet() }) {
                                    Text("အားလုံးဖွင့်", color = ChampionshipGold)
                                }
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            (3..18).forEach { sumVal ->
                                val selected = allowedTotals.contains(sumVal)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        allowedTotals = if (selected) allowedTotals - sumVal else allowedTotals + sumVal
                                    },
                                    label = { Text("$sumVal") }
                                )
                            }
                        }

                        Text("တစ်နေ့ အများဆုံးကျနိုင်ကြိမ် (0 = အကန့်အသတ်မရှိ):", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxBig,
                                onValueChange = { maxBig = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("အကြီး (11-17)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxSmall,
                                onValueChange = { maxSmall = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("အသေး (4-10)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxOdd,
                                onValueChange = { maxOdd = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("မ (Odd)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = maxEven,
                                onValueChange = { maxEven = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("စုံ (Even)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Text("နံပါတ်အလိုက် (Sum 3..18) နေ့စဉ် ကျနိုင်ကြိမ် သတ်မှတ်ရန်:", color = ChampionshipGold, style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            (3..18).forEach { s ->
                                FilterChip(
                                    selected = selectedQuotaSum == s,
                                    onClick = { selectedQuotaSum = s },
                                    label = { Text("$s") }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = selectedQuotaCap,
                                onValueChange = { selectedQuotaCap = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("Sum $selectedQuotaSum အကြိမ်") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val cap = selectedQuotaCap.toLongOrNull() ?: 0L
                                    if (cap > 0L) totalQuotas["$selectedQuotaSum"] = cap
                                    else totalQuotas.remove("$selectedQuotaSum")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark)
                            ) {
                                Text("ထည့်မည်")
                            }
                        }

                        if (totalQuotas.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                totalQuotas.entries.sortedBy { it.key.toIntOrNull() ?: 0 }.forEach { (sumKey, cap) ->
                                    Surface(
                                        color = DeepPitchSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable { totalQuotas.remove(sumKey) }
                                    ) {
                                        Text(
                                            text = "Sum $sumKey: ${cap}x ×",
                                            color = ChampionshipGold,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Six Animals Daily Quotas Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showAnimalQuotaSection = !showAnimalQuotaSection },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "၆ကောင်ဂျင် တစ်ကောင်ချင်း နေ့စဉ် ကျနိုင်ကြိမ် (0 = Unlimited)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showAnimalQuotaSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle 6 animals quotas",
                            tint = ChampionshipGold
                        )
                    }

                    if (showAnimalQuotaSection) {
                        ANIMAL_QUOTA_ENTRIES.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { (key, label) ->
                                    OutlinedTextField(
                                        value = animalQuotaInputs[key] ?: "0",
                                        onValueChange = { v ->
                                            animalQuotaInputs[key] = v.filter { c -> c.isDigit() }.take(4)
                                        },
                                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { buildAndSaveConfig() },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier.fillMaxWidth().testTag("save_mini_game_control_all_button")
                        ) {
                            Text("ဂိမ်းခန်း ကန့်သတ်ချက်အားလုံး သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Per-User Directive Section
        item {
            HideBarSectionHeader(
                title = "Customer တစ်ဦးချင်း ဂိမ်းထိန်းချုပ်မှု ဇယား",
                countBadge = "${users.size} ဦး",
                isVisible = showUserDirectivesSection,
                onToggle = { showUserDirectivesSection = !showUserDirectivesSection },
                testTag = "hide_bar_minigame_user_directives"
            )
            if (showUserDirectivesSection) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = customerSearch,
                    onValueChange = { customerSearch = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampionshipGold) },
                    label = { Text("Customer ရှာရန်") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showUserDirectivesSection) {

        items(filteredUsers.take(30), key = { it.userId }) { user ->
            val currentDir = config.userDirectives[user.userId] ?: "auto"
            val custName = user.displayName.ifBlank { "Member" }
            Card(colors = CardDefaults.cardColors(containerColor = PearlCard), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PitchEmeraldDark)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(custName, color = InkDark, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (user.email.isNotBlank()) {
                                    Text(user.email, color = InkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Text(formatKs(user.balanceKs), color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            "auto" to "Auto",
                            "stealth_house" to "ဒိုင်သာ",
                            "force_win" to "Win",
                            "force_loss" to "Loss"
                        ).forEach { (dirKey, lbl) ->
                            FilterChip(
                                selected = currentDir == dirKey,
                                onClick = { onSetCustomerDirective(user.userId, custName, dirKey) },
                                label = { Text(lbl, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("user_dir_${user.userId}_$dirKey")
                            )
                        }
                    }
                }
            }
        }
        }

        // Recent Live Rounds Monitor
        item {
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Spacer(Modifier.height(4.dp))
            HideBarSectionHeader(
                title = "လတ်တလော ဂိမ်းကစားမှု မှတ်တမ်းဇယား",
                countBadge = "${rounds.size} ကြိမ်",
                isVisible = showRecentRoundsSection,
                onToggle = { showRecentRoundsSection = !showRecentRoundsSection },
                testTag = "hide_bar_minigame_recent_rounds"
            )
        }

        if (showRecentRoundsSection) {
        if (rounds.isEmpty()) {
            item {
                Text("ဂိမ်းကစားမှု မှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(rounds.take(25), key = { it.id }) { round ->
                val isWin = round.payoutKs > 0L
                val custName = round.customerName.ifBlank { "Member" }
                Card(colors = CardDefaults.cardColors(containerColor = PearlCard), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$custName • ${if (round.gameType == "dice") "အံစာတုံး" else "၆ကောင်ဂျင်"}",
                                color = InkDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isWin) "+${formatKs(round.payoutKs)}" else "-${formatKs(round.stakeKs)}",
                                color = if (isWin) PitchEmeraldDark else AdminCrimson,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("ရွေးချယ်: ${round.betLabel} -> ရလဒ်: ${round.outcomeLabel}", color = InkDark, style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ထိုး: ${formatKs(round.stakeKs)} • ${formatTimestamp(round.createdAt)}", color = InkMuted, style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(onClick = { onSetCustomerDirective(round.userId, custName, "stealth_house") }) {
                                    Text("ဒိုင်သာ", color = Color(0xFFB45309), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { onSetCustomerDirective(round.userId, custName, "force_win") }) {
                                    Text("Win ပေး", color = PitchEmeraldDark, style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { onSetCustomerDirective(round.userId, custName, "force_loss") }) {
                                    Text("Loss ပေး", color = AdminCrimson, style = MaterialTheme.typography.labelSmall)
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

private fun encodeLogoUriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null
        val maxDim = 256f
        val scale = minOf(maxDim / raw.width.toFloat(), maxDim / raw.height.toFloat(), 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else raw
        var quality = 85
        var b64 = ""
        while (quality >= 30) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (b64.length <= 180_000) break
            quality -= 15
        }
        if (scaled != raw) scaled.recycle()
        raw.recycle()
        b64.takeIf { it.isNotBlank() && it.length <= 180_000 }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminBrandingTab(
    brandingConfig: BrandingConfig,
    busy: Boolean,
    onSaveBrandingConfig: (BrandingConfig) -> Unit
) {
    val context = LocalContext.current
    var appName by remember { mutableStateOf(brandingConfig.appName) }
    var logoVariant by remember { mutableStateOf(brandingConfig.logoVariant) }
    var customLogoUrl by remember { mutableStateOf(brandingConfig.customLogoUrl) }
    var customLogoBase64 by remember { mutableStateOf(brandingConfig.customLogoBase64) }
    var themePreset by remember { mutableStateOf(brandingConfig.themePreset) }
    var showRulesOnStartup by remember { mutableStateOf(brandingConfig.showRulesOnStartup) }
    var maxContactKs by remember { mutableStateOf(brandingConfig.maxContactMainAdminKs.toString()) }
    var mainAdminContactNote by remember { mutableStateOf(brandingConfig.mainAdminContactNote) }
    var rulesText by remember { mutableStateOf(brandingConfig.rulesText) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = encodeLogoUriToBase64(context, uri)
            if (encoded != null) {
                customLogoBase64 = encoded
                logoVariant = "custom_upload"
                Toast.makeText(context, "Logo ပုံ ရွေးချယ်ပြီးပါပြီ (သိမ်းဆည်းမည် နှိပ်ပါ)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "ပုံဖိုင် ဖတ်မရပါ သို့မဟုတ် အရွယ်အစားကြီးနေပါသည်", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(brandingConfig) {
        appName = brandingConfig.appName
        logoVariant = brandingConfig.logoVariant
        customLogoUrl = brandingConfig.customLogoUrl
        customLogoBase64 = brandingConfig.customLogoBase64
        themePreset = brandingConfig.themePreset
        showRulesOnStartup = brandingConfig.showRulesOnStartup
        maxContactKs = brandingConfig.maxContactMainAdminKs.toString()
        mainAdminContactNote = brandingConfig.mainAdminContactNote
        rulesText = brandingConfig.rulesText
    }

    val previewConfig = brandingConfig.copy(
        appName = appName.ifBlank { "လိုတရ" },
        logoVariant = logoVariant,
        customLogoUrl = customLogoUrl.trim(),
        customLogoBase64 = customLogoBase64,
        themePreset = themePreset
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BrandLogoAvatar(brandingConfig = previewConfig, size = 56.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Brush, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "App Brand, Logo, Theme & စည်းကမ်းချက်",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "အမည်: ${previewConfig.appName} • Theme: $themePreset",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("App နာမည် (Title)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("branding_app_name_input")
                    )

                    Text("Logo ပုံစံ (Logo Variant):", color = ChampionshipGold, style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "crest" to "Crest",
                            "gold_badge" to "Gold Badge",
                            "emerald_shield" to "Emerald Shield",
                            "custom_upload" to "Gallery မှ Upload",
                            "custom_url" to "Custom URL"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = logoVariant == key,
                                onClick = { logoVariant = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("branding_pick_logo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Gallery မှ Logo ရွေး", maxLines = 1)
                        }
                        if (customLogoBase64.isNotBlank() || logoVariant == "custom_upload") {
                            OutlinedButton(
                                onClick = {
                                    customLogoBase64 = ""
                                    logoVariant = "crest"
                                },
                                modifier = Modifier.testTag("branding_reset_logo_button")
                            ) {
                                Text("မူလ Logo ပြန်ထား", color = ChampionshipGold, maxLines = 1)
                            }
                        }
                    }

                    if (logoVariant == "custom_url") {
                        OutlinedTextField(
                            value = customLogoUrl,
                            onValueChange = { customLogoUrl = it },
                            label = { Text("Custom Logo URL (https://...)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().testTag("branding_logo_url_input")
                        )
                    }

                    Text("အရောင် Theme (Color Scheme):", color = ChampionshipGold, style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "emerald_gold" to "Emerald Gold",
                            "royal_blue" to "Royal Blue",
                            "crimson_gold" to "Crimson Gold",
                            "obsidian_amber" to "Obsidian Amber"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = themePreset == key,
                                onClick = { themePreset = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("App ဖွင့်တိုင်း စည်းကမ်းချက် Popup ပြမည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Customer များ Login ဝင်ပြီးစဖွင့်ချိန်တွင် စည်းကမ်းချက် ပြသပါမည်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = showRulesOnStartup, onCheckedChange = { showRulesOnStartup = it })
                    }

                    OutlinedTextField(
                        value = maxContactKs,
                        onValueChange = { maxContactKs = it.filter { c -> c.isDigit() }.take(10) },
                        label = { Text("Main Admin ဆက်သွယ်ရမည့် အများဆုံး ငွေပမာဏ (Ks)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("branding_max_contact_ks_input")
                    )

                    OutlinedTextField(
                        value = mainAdminContactNote,
                        onValueChange = { mainAdminContactNote = it },
                        label = { Text("Main Admin ဆက်သွယ်ရန် သတိပေးစာ") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rulesText,
                        onValueChange = { rulesText = it },
                        label = { Text("App စည်းကမ်းချက်များ (Startup & Settings Rules)") },
                        minLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("branding_rules_text_input")
                    )

                    Button(
                        onClick = {
                            onSaveBrandingConfig(
                                brandingConfig.copy(
                                    appName = appName.trim().ifBlank { "လိုတရ" },
                                    logoVariant = logoVariant,
                                    customLogoUrl = customLogoUrl.trim(),
                                    customLogoBase64 = customLogoBase64,
                                    themePreset = themePreset,
                                    showRulesOnStartup = showRulesOnStartup,
                                    rulesText = rulesText.trim().ifBlank { BrandingConfig.DEFAULT_RULES_TEXT },
                                    maxContactMainAdminKs = (maxContactKs.toLongOrNull() ?: 500_000L).coerceAtLeast(0L),
                                    mainAdminContactNote = mainAdminContactNote.trim()
                                )
                            )
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier.fillMaxWidth().testTag("save_branding_config_button")
                    ) {
                        Text("Branding နှင့် စည်းကမ်းချက် သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAgentSelfProfileTab(
    currentUserId: String,
    isOwnerAdmin: Boolean,
    defaultDisplayName: String,
    supportAgents: List<SupportAgentProfile>,
    paymentAccounts: List<PaymentAccount>,
    adminMember: AdminMember? = null,
    busy: Boolean,
    onSaveSupportAgentProfile: (SupportAgentProfile) -> Unit,
    onSavePaymentAccount: (id: String?, providerName: String, accountName: String, accountNumber: String, note: String, active: Boolean) -> Unit,
    onDeletePaymentAccount: (String) -> Unit
) {
    val existingProfile = remember(supportAgents, currentUserId) {
        supportAgents.firstOrNull { it.adminUid == currentUserId }
    }
    var displayName by remember { mutableStateOf(existingProfile?.displayName ?: defaultDisplayName) }
    var phone by remember { mutableStateOf(existingProfile?.phone ?: "") }
    var messagingHandle by remember { mutableStateOf(existingProfile?.messagingHandle ?: "") }
    var paymentSummary by remember { mutableStateOf(existingProfile?.paymentSummary ?: "") }
    var note by remember { mutableStateOf(existingProfile?.note ?: "") }
    var active by remember { mutableStateOf(existingProfile?.active ?: true) }

    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<PaymentAccount?>(null) }

    LaunchedEffect(existingProfile) {
        if (existingProfile != null) {
            displayName = existingProfile.displayName.ifBlank { defaultDisplayName }
            phone = existingProfile.phone
            messagingHandle = existingProfile.messagingHandle
            paymentSummary = existingProfile.paymentSummary
            note = existingProfile.note
            active = existingProfile.active
        }
    }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val myAccounts = remember(paymentAccounts, currentUserId, isOwnerAdmin) {
        paymentAccounts.filter { acc ->
            acc.ownerAdminId == currentUserId || (isOwnerAdmin && acc.ownerAdminId.isBlank())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isOwnerAdmin && adminMember != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2A22)),
                    border = BorderStroke(1.5.dp, ChampionshipGold.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("self_admin_float_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ChampionshipGold)
                                Spacer(Modifier.width(8.dp))
                                Text("မိမိ၏ Float ငွေလက်ကျန်", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = formatKs(adminMember.walletBalanceKs),
                                color = if (adminMember.walletBalanceKs > 0L) Color(0xFF34D399) else AdminCrimson,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Main Admin ခွဲဝေပေးငွေ: ${formatKs(adminMember.totalAllocatedKs)} • ထည့်ပေးပြီး: ${formatKs(adminMember.totalTopupsGivenKs)} • ငွေထုတ်လက်ခံ: ${formatKs(adminMember.totalWithdrawalsTakenKs)}",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.ContactPhone, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "မိမိ၏ ဆက်သွယ်ရန် နှင့် Online ငွေကြေးအချက်အလက်",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Customer များ Chat နှင့် Wallet တွင် တိုက်ရိုက်ကြည့်ရှု ဆက်သွယ်နိုင်ပါမည်",
                                    color = Color(0xFFB4C8BE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        StatusBadge(if (active) "active" else "suspended")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val summary = buildString {
                                    appendLine("$displayName (${if (isOwnerAdmin) "Main Admin" else "Second Admin"})")
                                    if (phone.isNotBlank()) appendLine("Phone: $phone")
                                    if (messagingHandle.isNotBlank()) appendLine("Contact: $messagingHandle")
                                    if (paymentSummary.isNotBlank()) appendLine("Pay: $paymentSummary")
                                    myAccounts.filter { it.active }.forEach { acc ->
                                        appendLine("${acc.providerName}: ${acc.accountName} (${acc.accountNumber})")
                                    }
                                }.trim()
                                clipboard.setText(AnnotatedString(summary))
                                Toast.makeText(context, "မိမိဆက်သွယ်ရန် & ငွေလွှဲစာရင်း Copy ကူးပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("copy_self_agent_summary_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("အချက်အလက် ကူး", color = ChampionshipGold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                val summary = buildString {
                                    appendLine("$displayName (${if (isOwnerAdmin) "Main Admin" else "Second Admin"})")
                                    if (phone.isNotBlank()) appendLine("Phone: $phone")
                                    if (messagingHandle.isNotBlank()) appendLine("Contact: $messagingHandle")
                                    if (paymentSummary.isNotBlank()) appendLine("Pay: $paymentSummary")
                                    myAccounts.filter { it.active }.forEach { acc ->
                                        appendLine("${acc.providerName}: ${acc.accountName} (${acc.accountNumber})")
                                    }
                                }.trim()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Admin Contact"))
                            },
                            modifier = Modifier.weight(1f).testTag("share_self_agent_summary_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share လုပ်", color = Color(0xFF34D399), maxLines = 1)
                        }
                    }

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Admin ပြသမည့် အမည်") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("agent_profile_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("ဖုန်းနံပါတ် (KPay / Wave / Viber / Phone)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("agent_profile_phone_input")
                    )
                    OutlinedTextField(
                        value = messagingHandle,
                        onValueChange = { messagingHandle = it },
                        label = { Text("Telegram / Viber Username or Link") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("agent_profile_messaging_input")
                    )
                    OutlinedTextField(
                        value = paymentSummary,
                        onValueChange = { paymentSummary = it },
                        label = { Text("Online ငွေလက်ခံစနစ် အကျဉ်း (ဥပမာ KPay / Wave / CB)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("agent_profile_payment_input")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Customer များထံ မှတ်ချက်") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Customer များထံ ဖော်ပြမည် (Active)", color = Color.White)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }

                    Button(
                        onClick = {
                            onSaveSupportAgentProfile(
                                SupportAgentProfile(
                                    adminUid = currentUserId,
                                    displayName = displayName.trim().ifBlank { defaultDisplayName },
                                    role = if (isOwnerAdmin) "owner" else "admin",
                                    phone = phone.trim(),
                                    messagingHandle = messagingHandle.trim(),
                                    paymentSummary = paymentSummary.trim(),
                                    note = note.trim(),
                                    active = active
                                )
                            )
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier.fillMaxWidth().testTag("save_agent_profile_button")
                    ) {
                        Text("ဆက်သွယ်ရန် Profile သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ChampionshipGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "မိမိ၏ Online ငွေလက်ခံအကောင့်များ (${myAccounts.size})",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = { showCreateAccountDialog = true },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                    modifier = Modifier.testTag("agent_add_payment_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("အကောင့်သစ်", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (myAccounts.isEmpty()) {
            item {
                Text("မိမိအတွက် ငွေလက်ခံအကောင့် မထည့်ရသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(myAccounts, key = { it.id }) { acc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${acc.providerName} • ${acc.accountName}", color = InkDark, fontWeight = FontWeight.Bold)
                                StatusBadge(if (acc.active) "active" else "suspended")
                            }
                            Text(acc.accountNumber, color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            if (acc.ownerAdminName.isNotBlank()) {
                                Text("Admin: ${acc.ownerAdminName}", color = InkMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            if (acc.note.isNotBlank()) {
                                Text(acc.note, color = InkMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row {
                            IconButton(onClick = { editingAccount = acc }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Payment Account", tint = PitchEmeraldDark)
                            }
                            IconButton(onClick = { onDeletePaymentAccount(acc.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Payment Account", tint = AdminCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateAccountDialog) {
        SelfPaymentAccountDialog(
            initial = null,
            onDismiss = { showCreateAccountDialog = false },
            onSave = { provider, name, number, accNote, isAct ->
                showCreateAccountDialog = false
                onSavePaymentAccount(null, provider, name, number, accNote, isAct)
            }
        )
    }

    editingAccount?.let { acc ->
        SelfPaymentAccountDialog(
            initial = acc,
            onDismiss = { editingAccount = null },
            onSave = { provider, name, number, accNote, isAct ->
                editingAccount = null
                onSavePaymentAccount(acc.id, provider, name, number, accNote, isAct)
            }
        )
    }
}

@Composable
private fun SelfPaymentAccountDialog(
    initial: PaymentAccount?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    var provider by remember { mutableStateOf(initial?.providerName ?: "KBZPay") }
    var accountName by remember { mutableStateOf(initial?.accountName ?: "") }
    var accountNumber by remember { mutableStateOf(initial?.accountNumber ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "ငွေလက်ခံအကောင့် ထည့်ရန်" else "ငွေလက်ခံအကောင့် ပြင်ရန်") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("ဝန်ဆောင်မှု (KBZPay / WavePay / CB)") }, singleLine = true)
                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("အကောင့်အမည်") }, singleLine = true)
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("ဖုန်း / ဘဏ်အကောင့်နံပါတ်") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("မှတ်ချက်") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("အသုံးပြုခွင့် ဖွင့်မည်")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(provider, accountName, accountNumber, note, active) },
                enabled = provider.isNotBlank() && accountName.isNotBlank() && accountNumber.isNotBlank()
            ) {
                Text("သိမ်းဆည်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}
