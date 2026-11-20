package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AppLanguage
import com.example.data.model.SystemGameConfig
import com.example.data.model.UserAccount
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.formatKs
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmeraldDark

@Composable
fun SettingsAndRulesScreen(
    account: UserAccount?,
    displayName: String,
    currentLanguage: AppLanguage,
    systemGameConfig: SystemGameConfig,
    onSelectLanguage: (AppLanguage) -> Unit,
    onUpdateSecurityPin: (String, () -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var showRulesDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showTeamThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val isEnglish = currentLanguage == AppLanguage.EN

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            SubScreenTopBar(
                title = if (isEnglish) "Settings & Rules" else "ဆက်တင် / စည်းကမ်း",
                onBack = onBack
            )
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, PearlCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsActionRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = if (isEnglish) "Rules & Payout Terms" else "စည်းကမ်းသတ်မှတ်ချက်များ",
                            trailingBadge = null,
                            testTag = "settings_rules_row",
                            onClick = { showRulesDialog = true }
                        )
                        HorizontalDivider(color = PearlCardBorder)
                        SettingsActionRow(
                            icon = Icons.Default.Lock,
                            title = if (isEnglish) "Change Password / PIN" else "စကားဝှက် ပြောင်းရန်",
                            trailingBadge = if (account?.securityPinHash?.isNotBlank() == true) "PIN ON" else null,
                            testTag = "settings_pin_row",
                            onClick = { showPinDialog = true }
                        )
                        HorizontalDivider(color = PearlCardBorder)
                        SettingsActionRow(
                            icon = Icons.Default.SportsSoccer,
                            title = if (isEnglish) "Team Name & Version" else "အသင်းအမည်",
                            trailingBadge = "EN / MM",
                            badgeColor = Color(0xFF1D4ED8),
                            testTag = "settings_team_row",
                            onClick = { showTeamThemeDialog = true }
                        )
                        HorizontalDivider(color = PearlCardBorder)
                        SettingsActionRow(
                            icon = Icons.Default.Language,
                            title = if (isEnglish) "Choose Language" else "ဘာသာစကားရွေးရန်",
                            trailingBadge = if (isEnglish) "EN" else "MY",
                            badgeColor = Color(0xFF047857),
                            testTag = "settings_language_row",
                            onClick = { showLanguageDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = { Text(if (isEnglish) "Rules & Payout Limits" else "စည်းကမ်းသတ်မှတ်ချက်များ") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "• အနည်းဆုံး ထိုးကြေး: ${formatKs(systemGameConfig.minStakeKs)}\n" +
                            "• အများဆုံး ထိုးကြေး: ${formatKs(systemGameConfig.maxStakeKs)}",
                        fontWeight = FontWeight.SemiBold,
                        color = PitchEmeraldDark
                    )
                    Text("• ၂လုံးထီ (2D): ${systemGameConfig.twoDPayoutMultiplier.toInt()}x (Live SET Index 12:01 / 4:30)")
                    Text("• ၃လုံးထီ (3D): ${systemGameConfig.threeDPayoutMultiplier.toInt()}x")
                    Text("• အံစာတုံး (Dice): Big/Small/Odd/Even ${systemGameConfig.dicePayoutMultiplier}x • Exact 8x • Triple 24x")
                    Text("• ၆ကောင်ဂျင် (6 Animals): ၁ ကောင်တူ 2x • ၂ ကောင်တူ 3x • ၃ ကောင်တူ 4x")
                    Text("• ဘောလုံးပွဲစဉ် (မောင်း / ဘော်ဒီ/ဂိုးပေါင်း): 90 mins + injury time ရလဒ်အပေါ် တွက်ချက်သည်။")
                    Text("• ငွေထုတ် ${formatKs(systemGameConfig.dualApprovalThresholdKs)} နှင့်အထက်ကို Main Admin ထပ်ဆင့် အတည်ပြုပေးပါမည်။")
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) { Text("ပိတ်မည်") }
            }
        )
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        val valid = newPin.length in 4..8 && newPin == confirmPin
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("လုံခြုံရေး PIN သတ်မှတ် / ပြောင်းရန်") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ဂဏန်း 4-8 လုံးအထိ PIN အသစ် ထည့်သွင်းပါ (Admin / Wallet ဝင်ရောက်မှု လုံခြုံစေရန်)။")
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("PIN အသစ်") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_security_pin_input")
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("PIN ထပ်မံအတည်ပြုပါ") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_security_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSecurityPin(newPin) {
                            showPinDialog = false
                        }
                    },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                    modifier = Modifier.testTag("save_security_pin_button")
                ) {
                    Text("သိမ်းဆည်းမည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("ပိတ်မည်") }
            }
        )
    }

    if (showTeamThemeDialog) {
        AlertDialog(
            onDismissRequest = { showTeamThemeDialog = false },
            title = { Text("လိုတရ • Account & App Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("App: လိုတရ v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontWeight = FontWeight.Bold)
                    Text("Member: ${account?.displayName ?: displayName}")
                    if (account?.email?.isNotBlank() == true) {
                        Text("Email: ${account.email}", color = PitchEmeraldDark)
                    }
                    Text("Team Badges: ESPN / TheSportsDB Official Badges + Auto Crests")
                    Text("UID: ${account?.userId ?: "-"}", fontFamily = JetBrainsMonoFontFamily, color = InkMuted, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTeamThemeDialog = false }) { Text("ပိတ်မည်") }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("ဘာသာစကားရွေးရန် / Choose Language") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = currentLanguage == lang,
                            onClick = {
                                onSelectLanguage(lang)
                                showLanguageDialog = false
                            },
                            label = { Text("${lang.code.uppercase()} • ${lang.label}") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("ပိတ်မည်") }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    trailingBadge: String?,
    badgeColor: Color = PitchEmeraldDark,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                color = InkDark,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingBadge != null) {
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = trailingBadge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                tint = InkMuted
            )
        }
    }
}
