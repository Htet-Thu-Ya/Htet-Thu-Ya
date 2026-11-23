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
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.example.BuildConfig
import com.example.data.model.AppLanguage
import com.example.data.model.AppUpdateConfig
import com.example.data.model.BrandingConfig
import com.example.data.model.MiniGameControlConfig
import com.example.data.model.SystemGameConfig
import com.example.data.model.UserAccount
import com.example.data.network.ApkDownloadHelper
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
    brandingConfig: BrandingConfig = BrandingConfig(),
    miniGameControlConfig: MiniGameControlConfig = MiniGameControlConfig(),
    apkDownloadUrl: String = AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL,
    isAdmin: Boolean = false,
    onSelectLanguage: (AppLanguage) -> Unit,
    onUpdateSecurityPin: (String, () -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showRulesDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showTeamThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showShareLinksDialog by remember { mutableStateOf(false) }

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
                        HorizontalDivider(color = PearlCardBorder)
                        SettingsActionRow(
                            icon = Icons.Default.Share,
                            title = if (isEnglish) "Share Link & Admin Link" else "Share လင့် နှင့် Admin Link များ",
                            trailingBadge = if (isAdmin) "ADMIN" else "SHARE",
                            badgeColor = if (isAdmin) Color(0xFFB91C1C) else Color(0xFF0284C7),
                            testTag = "settings_share_links_row",
                            onClick = { showShareLinksDialog = true }
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
                            "• အများဆုံး ထိုးကြေး: ${formatKs(systemGameConfig.maxStakeKs)}\n" +
                            "• Main Admin တိုက်ရိုက် ဆက်သွယ်ရန် ကန့်သတ်: ${formatKs(brandingConfig.maxContactMainAdminKs)}",
                        fontWeight = FontWeight.SemiBold,
                        color = PitchEmeraldDark
                    )
                    if (brandingConfig.rulesText.isNotBlank()) {
                        Text(brandingConfig.rulesText)
                    }
                    Text("• ၂လုံးထီ (2D): ${systemGameConfig.twoDPayoutMultiplier.toInt()}x (Live SET Index 12:01 / 4:30)")
                    Text("• ၃လုံးထီ (3D): ${systemGameConfig.threeDPayoutMultiplier.toInt()}x")
                    Text("• အံစာတုံး (Dice): Big/Small/Odd/Even ${miniGameControlConfig.diceStandardMult}x • Exact ${miniGameControlConfig.diceExactMult}x • Triple ${miniGameControlConfig.diceTripleMult}x")
                    val bonus = (miniGameControlConfig.sixAnimalsMult - 1.0).coerceAtLeast(0.2)
                    Text("• ၆ကောင်ဂျင် (6 Animals): 1တူ ${miniGameControlConfig.sixAnimalsMult}x • 2တူ ${1.0 + bonus * 2}x • 3တူ ${1.0 + bonus * 3}x")
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
            title = { Text("${brandingConfig.appName.ifBlank { "လိုတရ" }} • Account & App Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("App: ${brandingConfig.appName.ifBlank { "လိုတရ" }} v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontWeight = FontWeight.Bold)
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

    if (showShareLinksDialog) {
        val appName = brandingConfig.appName.ifBlank { "လိုတရ" }
        val directApkUrl = remember(apkDownloadUrl) { ApkDownloadHelper.buildCustomerShareLink(apkDownloadUrl) }
        val shareText = remember(appName, directApkUrl) {
            ApkDownloadHelper.buildApkShareText(appName = appName, rawUrl = directApkUrl)
        }
        val adminPortalLink = remember { ApkDownloadHelper.buildAdminPortalLink() }
        AlertDialog(
            onDismissRequest = { showShareLinksDialog = false },
            title = { Text("Share Link နှင့် Admin Link များ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Customer APK Share Link (${ApkDownloadHelper.CUSTOMER_SHARE_DEEP_LINK})", fontWeight = FontWeight.SemiBold)
                    Text(directApkUrl, style = MaterialTheme.typography.bodySmall, color = PitchEmeraldDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(shareText))
                                Toast.makeText(context, "Customer Share Link ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ကူးမည်")
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "$appName APK Download")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Customer Link"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ရှယ်မည်")
                        }
                    }
                    if (isAdmin) {
                        HorizontalDivider()
                        Text("Admin Portal Link", fontWeight = FontWeight.SemiBold)
                        Text(adminPortalLink, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB91C1C))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(adminPortalLink))
                                    Toast.makeText(context, "Admin Link ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Admin ကူးမည်")
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "$appName Admin Link")
                                        putExtra(Intent.EXTRA_TEXT, "$appName Admin Link: $adminPortalLink\nAPK: $directApkUrl")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Admin Link"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Admin ရှယ်မည်")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareLinksDialog = false }) { Text("ပိတ်မည်") }
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
