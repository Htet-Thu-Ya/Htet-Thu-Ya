package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.BuildConfig
import com.example.data.model.AppQualityReview
import com.example.data.model.AppUpdateConfig
import com.example.data.model.DirectChatMessage
import com.example.data.model.GroupChatMessage
import com.example.data.model.UserAccount
import com.example.data.network.ApkDownloadHelper
import kotlinx.coroutines.launch
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark

@Composable
fun AdminChatTab(
    currentUserId: String,
    users: List<UserAccount>,
    allDirectMessages: List<DirectChatMessage>,
    groupMessages: List<GroupChatMessage>,
    initialCustomerUid: String?,
    onClearInitialCustomer: () -> Unit,
    onSendAdminDirectMessage: (threadUserId: String, text: String) -> Unit,
    onDeleteDirectMessage: (String) -> Unit,
    onSendGroupMessage: (String) -> Unit,
    onDeleteGroupMessage: (String) -> Unit,
    onCallCustomer: (customerUid: String, customerName: String, callType: String) -> Unit = { _, _, _ -> }
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = 1:1, 1 = Group
    var activeThreadUid by remember { mutableStateOf<String?>(initialCustomerUid) }

    LaunchedEffect(initialCustomerUid) {
        if (initialCustomerUid != null) {
            mode = 0
            activeThreadUid = initialCustomerUid
            onClearInitialCustomer()
        }
    }

    val usersById = remember(users) { users.associateBy { it.userId } }
    val msgsByThread = remember(allDirectMessages) { allDirectMessages.groupBy { it.threadUserId } }

    // Combine users who already messaged + all registered users
    val orderedThreads = remember(users, msgsByThread) {
        val uids = (msgsByThread.keys + users.map { it.userId }).distinct()
        uids.sortedByDescending { uid ->
            msgsByThread[uid]?.lastOrNull()?.createdAt?.seconds ?: 0L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0 },
                leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Customer 1:1 (${msgsByThread.size})") },
                modifier = Modifier.weight(1f).testTag("admin_chat_mode_direct")
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1 },
                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Group Chat (${groupMessages.size})") },
                modifier = Modifier.weight(1f).testTag("admin_chat_mode_group")
            )
        }

        if (mode == 1) {
            GroupChatThreadContent(
                messages = groupMessages,
                currentUserId = currentUserId,
                isAdminViewer = true,
                onSend = onSendGroupMessage,
                onDeleteMessage = onDeleteGroupMessage
            )
        } else {
            val selectedUid = activeThreadUid
            if (selectedUid == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Customer တစ်ဦးချင်းစီနှင့် တိုက်ရိုက်စကားပြောရန် ရွေးချယ်ပါ",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (orderedThreads.isEmpty()) {
                        item {
                            Text("Customer မရှိသေးပါ", color = Color(0xFFB4C8BE))
                        }
                    } else {
                        items(orderedThreads, key = { it }) { uid ->
                            val user = usersById[uid]
                            val threadMsgs = msgsByThread[uid].orEmpty()
                            val lastMsg = threadMsgs.lastOrNull()
                            val name = user?.displayName?.ifBlank { null }
                                ?: threadMsgs.firstOrNull { it.senderRole == "customer" }?.senderName
                                ?: "Member"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PearlCard),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeThreadUid = uid }
                                    .testTag("admin_chat_thread_$uid")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = PitchEmeraldDark)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(name, color = InkDark, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                                if (user != null) {
                                                    com.example.ui.components.OnlineStatusDot(
                                                        isOnline = user.isOnline,
                                                        lastSeenTs = user.lastSeenAt
                                                    )
                                                }
                                            }
                                            if (user?.email?.isNotBlank() == true) {
                                                Text(user.email, color = PitchEmeraldDark, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(
                                                text = if (lastMsg != null) "${lastMsg.senderName}: ${lastMsg.text}" else "စကားပြောရန် နှိပ်ပါ",
                                                color = InkMuted,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onCallCustomer(uid, name, "voice") },
                                            modifier = Modifier.testTag("admin_chat_voice_call_$uid")
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = PitchEmeraldDark)
                                        }
                                        IconButton(
                                            onClick = { onCallCustomer(uid, name, "video") },
                                            modifier = Modifier.testTag("admin_chat_video_call_$uid")
                                        ) {
                                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color(0xFF0284C7))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            if (threadMsgs.isNotEmpty()) {
                                                Surface(color = PitchEmeraldDark, shape = RoundedCornerShape(8.dp)) {
                                                    Text(
                                                        text = "${threadMsgs.size}",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Text(formatTimestamp(lastMsg?.createdAt), color = InkMuted, style = MaterialTheme.typography.labelSmall)
                                            } else {
                                                Icon(Icons.Default.Forum, contentDescription = null, tint = PitchEmeraldDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val targetUser = usersById[selectedUid]
                val threadMsgs = msgsByThread[selectedUid].orEmpty()
                val targetName = targetUser?.displayName?.ifBlank { null }
                    ?: threadMsgs.firstOrNull { it.senderRole == "customer" }?.senderName
                    ?: "Member"
                Surface(color = DeepPitchSurface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(onClick = { activeThreadUid = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampionshipGold)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(targetName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (targetUser != null) {
                                        com.example.ui.components.OnlineStatusDot(
                                            isOnline = targetUser.isOnline,
                                            lastSeenTs = targetUser.lastSeenAt
                                        )
                                    }
                                }
                                if (targetUser?.email?.isNotBlank() == true) {
                                    Text(targetUser.email, color = ChampionshipGold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onCallCustomer(selectedUid, targetName, "voice") },
                                modifier = Modifier.testTag("admin_thread_voice_call")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color(0xFF34D399))
                            }
                            IconButton(
                                onClick = { onCallCustomer(selectedUid, targetName, "video") },
                                modifier = Modifier.testTag("admin_thread_video_call")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color(0xFF38BDF8))
                            }
                            OutlinedButton(onClick = { activeThreadUid = null }) {
                                Text("စာရင်း", color = ChampionshipGold)
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF284236))
                DirectChatThreadContent(
                    messages = threadMsgs,
                    currentUserId = currentUserId,
                    isAdminViewer = true,
                    emptyHint = "$targetName နှင့် တိုက်ရိုက်စကားပြောခန်း စတင်ပါ။",
                    onSend = { text -> onSendAdminDirectMessage(selectedUid, text) },
                    onDeleteMessage = onDeleteDirectMessage
                )
            }
        }
    }
}

@Composable
fun AdminUpdateAndQualityTab(
    currentUserId: String,
    appUpdateConfig: AppUpdateConfig?,
    reviews: List<AppQualityReview>,
    busy: Boolean,
    onSaveAppUpdateConfig: (Int, String, String, String, Boolean) -> Unit,
    onDeleteReview: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showUpdatePanel by remember { mutableStateOf(true) }
    var copiedLinkHint by remember { mutableStateOf(false) }
    var versionCodeText by remember { mutableStateOf((appUpdateConfig?.latestVersionCode ?: BuildConfig.VERSION_CODE).toString()) }
    var versionNameText by remember { mutableStateOf(appUpdateConfig?.latestVersionName ?: BuildConfig.VERSION_NAME) }
    var downloadUrlText by remember { mutableStateOf(appUpdateConfig?.downloadUrl ?: AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL) }
    var releaseNotesText by remember { mutableStateOf(appUpdateConfig?.releaseNotes ?: "ဗားရှင်းအသစ် ထွက်ရှိပါပြီ။") }
    var forceUpdate by remember { mutableStateOf(appUpdateConfig?.forceUpdate ?: false) }

    val resolvedDirectUrl = remember(downloadUrlText) {
        ApkDownloadHelper.toDirectDownloadUrl(
            downloadUrlText.trim().ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL }
        ).ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL }
    }
    val canNormalizeUrl = downloadUrlText.trim().isNotBlank() && resolvedDirectUrl != downloadUrlText.trim()

    LaunchedEffect(appUpdateConfig) {
        if (appUpdateConfig != null) {
            versionCodeText = appUpdateConfig.latestVersionCode.toString()
            versionNameText = appUpdateConfig.latestVersionName
            downloadUrlText = appUpdateConfig.downloadUrl
            releaseNotesText = appUpdateConfig.releaseNotes
            forceUpdate = appUpdateConfig.forceUpdate
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUpdatePanel = !showUpdatePanel },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "APK Update ကြေညာချက် (Direct Download & Share)",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "လက်ရှိ Build: v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showUpdatePanel) "ဝှက်မည်" else "ပြမည်",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (showUpdatePanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle update settings",
                            tint = ChampionshipGold
                        )
                    }
                }

                if (showUpdatePanel) {
                    Text(
                        text = "Google Drive / Dropbox / GitHub Release လင့်ထည့်လိုက်ရုံဖြင့် တိုက်ရိုက် APK Download Link အဖြစ် အလိုအလျောက် ပြောင်းလဲပေးပြီး ဖိုင်တိုက်ရိုက် ရှယ်နိုင်ပါသည်။",
                        color = Color(0xFFB4C8BE),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = versionCodeText,
                            onValueChange = { versionCodeText = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Version Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).testTag("admin_update_version_code")
                        )
                        OutlinedTextField(
                            value = versionNameText,
                            onValueChange = { versionNameText = it },
                            label = { Text("Version Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).testTag("admin_update_version_name")
                        )
                    }
                    OutlinedTextField(
                        value = downloadUrlText,
                        onValueChange = {
                            downloadUrlText = it
                            copiedLinkHint = false
                        },
                        label = { Text("APK Download Link (Drive / GitHub / Direct https://...)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_update_url")
                    )
                    Surface(
                        color = Color(0xFF0E2920),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "တိုက်ရိုက် Download Link (Auto-Direct):",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = resolvedDirectUrl,
                                color = Color(0xFFD7EAE1),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (canNormalizeUrl) {
                                TextButton(
                                    onClick = { downloadUrlText = resolvedDirectUrl },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Direct Link အဖြစ် Box ထဲပြောင်းထည့်မည်", color = Color(0xFF34D399))
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(resolvedDirectUrl))
                                copiedLinkHint = true
                            },
                            modifier = Modifier.weight(1f).testTag("copy_apk_update_link_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (copiedLinkHint) "ကူးယူပြီးပါပြီ" else "Direct Link ကူးမည်", color = ChampionshipGold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                val msg = ApkDownloadHelper.buildApkShareText(
                                    appName = "လိုတရ",
                                    rawUrl = resolvedDirectUrl,
                                    versionLabel = versionNameText
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "လိုတရ APK Direct Download")
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Direct APK Link မျှဝေမည်"))
                            },
                            modifier = Modifier.weight(1f).testTag("share_apk_update_link_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Direct Link ရှယ်မည်", color = Color(0xFF34D399), maxLines = 1)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ApkDownloadHelper.enqueueSystemDownload(
                                    context = context,
                                    rawUrl = resolvedDirectUrl,
                                    title = "လိုတရ $versionNameText APK"
                                ).onSuccess {
                                    Toast.makeText(context, "Downloads ဖိုင်တွဲသို့ APK တိုက်ရိုက်ဆွဲနေပါပြီ", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.localizedMessage ?: "Download စမ်းသပ်၍မရပါ", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("admin_test_direct_download_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("တိုက်ရိုက်ဒေါင်းစမ်းမည်", color = Color.White, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    ApkDownloadHelper.shareInstalledApkFile(context, "လိုတရ")
                                        .onFailure { err ->
                                            Toast.makeText(context, err.localizedMessage ?: "APK ဖိုင်ရှယ်၍မရပါ", Toast.LENGTH_LONG).show()
                                        }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("admin_share_apk_file_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("APK ဖိုင်တိုက်ရိုက်ပို့မည်", color = ChampionshipGold, maxLines = 1)
                        }
                    }
                    OutlinedTextField(
                        value = releaseNotesText,
                        onValueChange = { releaseNotesText = it },
                        label = { Text("Update မှတ်စု") },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("admin_update_notes")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Customer များ မဖြစ်မနေ Update လုပ်ခိုင်းမည်", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Switch(checked = forceUpdate, onCheckedChange = { forceUpdate = it })
                    }
                    Button(
                        onClick = {
                            val code = (versionCodeText.toIntOrNull() ?: (BuildConfig.VERSION_CODE + 1)).coerceAtLeast(1)
                            onSaveAppUpdateConfig(code, versionNameText, resolvedDirectUrl, releaseNotesText, forceUpdate)
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier.fillMaxWidth().testTag("publish_apk_update_button")
                    ) {
                        Text("Customer များထံ APK Update ထုတ်ပြန်မည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quality ratings viewer & moderator below
        QualityReviewsContent(
            reviews = reviews,
            currentUserId = currentUserId,
            isAdmin = true,
            allowSubmit = false,
            busy = busy,
            onSubmitReview = { _, _, _ -> },
            onDeleteReview = onDeleteReview
        )
    }
}
