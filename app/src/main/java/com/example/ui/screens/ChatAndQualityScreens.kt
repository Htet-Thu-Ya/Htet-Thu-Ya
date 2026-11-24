package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.network.ApkDownloadHelper
import kotlinx.coroutines.launch
import java.io.File
import com.example.data.model.AppQualityReview
import com.example.data.model.AvailableApkUpdate
import com.example.data.model.BrandingConfig
import com.example.data.model.DirectChatMessage
import com.example.data.model.GroupChatMessage
import com.example.data.model.SupportAgentProfile
import com.example.data.model.UiState
import com.example.ui.components.SubScreenTopBar
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
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmeraldDark
import java.util.Locale

@Composable
fun ApkUpdateBanner(
    update: AvailableApkUpdate,
    onOpenDialog: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF163A2A)),
        border = BorderStroke(1.dp, ChampionshipGold),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDialog)
            .testTag("apk_update_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ChampionshipGold.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = ChampionshipGold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "✨ စနစ်သစ် ${update.versionLabel} အဆင်သင့်ဖြစ်ပါပြီ!",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1xBET နှင့် 555 Mix တိုက်ရိုက်ကြေး + Live ရလဒ် မြန်နှုန်းမြှင့်တင်ပြီး • နှိပ်၍ ချက်ချင်း Update ရယူပါ",
                        color = Color(0xFFD7EAE1),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = ChampionshipGold,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "ယခု Update",
                    color = InkDark,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ApkUpdatePromptDialog(
    update: AvailableApkUpdate,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val directUrl = remember(update.downloadUrl) {
        ApkDownloadHelper.toDirectDownloadUrl(update.downloadUrl)
    }
    var downloading by remember { mutableStateOf(false) }
    var progressPct by remember { mutableIntStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }

    fun triggerInstall(file: File) {
        ApkDownloadHelper.installApkFile(context, file)
            .onSuccess { launched ->
                statusMsg = if (launched) {
                    "APK ဖိုင်ကို Install စတင်ဖွင့်လိုက်ပါပြီ။"
                } else {
                    "Unknown Apps ခွင့်ပြုချက်ဖွင့်ပြီးပါက 'APK ဖွင့်၍ Install မည်' ကို ထပ်နှိပ်ပါ။"
                }
            }
            .onFailure { err ->
                errorMsg = err.localizedMessage ?: "APK Install ဖွင့်၍မရပါ"
            }
    }

    AlertDialog(
        onDismissRequest = { if (!update.forceUpdate && !downloading) onDismiss() },
        icon = {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = PitchEmeraldDark, modifier = Modifier.size(32.dp))
        },
        title = {
            Text(
                text = "🔥 လိုတရ ဗားရှင်းအသစ် (${update.versionLabel})",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = update.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkDark
                )
                Surface(
                    color = Color(0xFFEFF6F2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "အရင်းအမြစ်: ${update.source}\nတိုက်ရိုက်ဒေါင်းလုဒ်လင့်: $directUrl",
                            color = PitchEmeraldDark,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (downloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (progressPct > 0) {
                            LinearProgressIndicator(
                                progress = { progressPct / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "APK တိုက်ရိုက်ဒေါင်းလုဒ်ဆွဲနေပါသည်... $progressPct%",
                                style = MaterialTheme.typography.labelMedium,
                                color = PitchEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = "APK ဖိုင်ကို စတင်ဆွဲယူနေပါသည်...",
                                style = MaterialTheme.typography.labelMedium,
                                color = PitchEmeraldDark
                            )
                        }
                    }
                }
                if (statusMsg != null) {
                    Text(
                        text = statusMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = PitchEmeraldDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ApkDownloadHelper.enqueueSystemDownload(
                                context = context,
                                rawUrl = directUrl,
                                title = "လိုတရ ${update.versionLabel}"
                            ).onSuccess {
                                statusMsg = "Download Manager ဖြင့် Downloads ဖိုင်တွဲသို့ တိုက်ရိုက်ဆွဲနေပါပြီ။"
                                errorMsg = null
                            }.onFailure { err ->
                                errorMsg = err.localizedMessage ?: "DownloadManager မအောင်မြင်ပါ"
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("system_apk_download_button")
                    ) {
                        Text("Downloads ဖိုင်တွဲဆွဲ", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            val shareText = ApkDownloadHelper.buildApkShareText(
                                appName = "လိုတရ",
                                rawUrl = directUrl,
                                versionLabel = update.versionLabel
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "လိုတရ APK Direct Download")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Direct APK Link မျှဝေမည်"))
                        },
                        modifier = Modifier.weight(1f).testTag("share_apk_direct_link_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Link ရှယ်မည်", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            val readyFile = downloadedApk
            if (readyFile != null && readyFile.exists()) {
                Button(
                    onClick = { triggerInstall(readyFile) },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                    modifier = Modifier.testTag("install_downloaded_apk_button")
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("APK ဖွင့်၍ Install မည်")
                }
            } else {
                Button(
                    enabled = !downloading,
                    onClick = {
                        downloading = true
                        progressPct = 0
                        errorMsg = null
                        statusMsg = null
                        scope.launch {
                            ApkDownloadHelper.downloadApkToCache(
                                context = context,
                                rawUrl = directUrl,
                                onProgress = { pct -> progressPct = pct }
                            ).onSuccess { file ->
                                downloading = false
                                downloadedApk = file
                                triggerInstall(file)
                            }.onFailure { err ->
                                downloading = false
                                errorMsg = err.localizedMessage ?: "APK တိုက်ရိုက်ဒေါင်း၍မရပါ"
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_apk_update_download_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (downloading) "ဒေါင်းနေသည်..." else "တိုက်ရိုက် Download & Install")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(directUrl)))
                        }
                    }
                ) {
                    Text("Browser")
                }
                if (!update.forceUpdate && !downloading) {
                    TextButton(onClick = onDismiss) {
                        Text("နောက်မှ")
                    }
                }
            }
        }
    )
}

@Composable
fun ChatHubScreen(
    currentUserId: String,
    isAdmin: Boolean,
    directMessagesState: UiState<List<DirectChatMessage>>,
    groupMessagesState: UiState<List<GroupChatMessage>>,
    brandingConfig: BrandingConfig = BrandingConfig(),
    supportAgents: List<SupportAgentProfile> = emptyList(),
    onSendDirectMessage: (String) -> Unit,
    onSendGroupMessage: (String) -> Unit,
    onDeleteDirectMessage: (String) -> Unit,
    onDeleteGroupMessage: (String) -> Unit,
    onStartVoiceCall: () -> Unit = {},
    onStartVideoCall: () -> Unit = {},
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAgentDirectory by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            SubScreenTopBar(title = "ဆက်သွယ်ရေး Chatbox", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepPitchSurface,
                contentColor = ChampionshipGold
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Admin နှင့် 1:1")
                        }
                    },
                    modifier = Modifier.testTag("chat_tab_direct")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("အများသုံး Group")
                        }
                    },
                    modifier = Modifier.testTag("chat_tab_group")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = DeepPitchElevated,
                                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Admin တိုက်ရိုက်ခေါ်ရန်",
                                            color = ChampionshipGold,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = onStartVoiceCall,
                                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("chat_voice_call_button")
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Voice", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = onStartVideoCall,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("chat_video_call_button")
                                        ) {
                                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Video", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    val activeAgents = remember(supportAgents) { supportAgents.filter { it.active } }
                                    if (activeAgents.isNotEmpty() || brandingConfig.maxContactMainAdminKs > 0L) {
                                        OutlinedButton(
                                            onClick = { showAgentDirectory = !showAgentDirectory },
                                            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.4f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (showAgentDirectory) "Second Admin / Main Admin ဆက်သွယ်ရန် ဝှက်မည်"
                                                else "Second Admin / Main Admin ဖုန်း & ငွေလက်ခံအချက်အလက် (${activeAgents.size})",
                                                color = ChampionshipGold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }

                                    if (showAgentDirectory) {
                                        if (brandingConfig.maxContactMainAdminKs > 0L) {
                                            Text(
                                                text = "* ${formatKs(brandingConfig.maxContactMainAdminKs)} နှင့်အထက်အတွက် Main Admin တိုက်ရိုက်ဆက်သွယ်ပါ။",
                                                color = Color(0xFFFDE68A),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        activeAgents.forEach { ag ->
                                            Surface(
                                                color = Color(0xFF1A3329),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        "${ag.displayName} (${if (ag.role == "owner") "Main Admin" else "Second Admin"})",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (ag.phone.isNotBlank()) Text("Phone: ${ag.phone}", color = ChampionshipGold, style = MaterialTheme.typography.labelSmall)
                                                    if (ag.messagingHandle.isNotBlank()) Text("Msg: ${ag.messagingHandle}", color = Color(0xFFD1FAE5), style = MaterialTheme.typography.labelSmall)
                                                    if (ag.paymentSummary.isNotBlank()) Text("Pay: ${ag.paymentSummary}", color = Color(0xFF93C5FD), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (directMessagesState) {
                                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = ChampionshipGold)
                                    }
                                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Text(directMessagesState.message, color = Color(0xFFFCA5A5))
                                    }
                                    is UiState.Success -> {
                                        DirectChatThreadContent(
                                            messages = directMessagesState.data,
                                            currentUserId = currentUserId,
                                            isAdminViewer = isAdmin,
                                            emptyHint = "Admin ထံ မေးမြန်းလိုသည်များ၊ ငွေသွင်း/ငွေထုတ် သို့မဟုတ် အကူအညီများကို ဤနေရာတွင် တိုက်ရိုက် စာပို့/ဖုန်းခေါ်နိုင်ပါသည်။",
                                            onSend = onSendDirectMessage,
                                            onDeleteMessage = onDeleteDirectMessage
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        when (groupMessagesState) {
                            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ChampionshipGold)
                            }
                            is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(groupMessagesState.message, color = Color(0xFFFCA5A5))
                            }
                            is UiState.Success -> {
                                GroupChatThreadContent(
                                    messages = groupMessagesState.data,
                                    currentUserId = currentUserId,
                                    isAdminViewer = isAdmin,
                                    onSend = onSendGroupMessage,
                                    onDeleteMessage = onDeleteGroupMessage
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectChatThreadContent(
    messages: List<DirectChatMessage>,
    currentUserId: String,
    isAdminViewer: Boolean,
    emptyHint: String,
    onSend: (String) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Forum, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("စကားပြောခန်း အသစ်", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(emptyHint, color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        senderName = msg.senderName,
                        senderRole = msg.senderRole,
                        text = msg.text,
                        timestampLabel = formatTimestamp(msg.createdAt),
                        isMine = msg.senderId == currentUserId,
                        canDelete = isAdminViewer,
                        onDelete = { onDeleteMessage(msg.id) }
                    )
                }
            }
        }

        ChatComposerBar(
            draft = draft,
            onDraftChange = { draft = it },
            placeholder = "မက်ဆေ့ချ် ရိုက်ထည့်ပါ...",
            testTagPrefix = "direct_chat",
            onSend = {
                val clean = draft.trim()
                if (clean.isNotEmpty()) {
                    onSend(clean)
                    draft = ""
                }
            }
        )
    }
}

@Composable
fun GroupChatThreadContent(
    messages: List<GroupChatMessage>,
    currentUserId: String,
    isAdminViewer: Boolean,
    onSend: (String) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Customer & Admin Group Chat", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Customer များနှင့် Admin များ စုပေါင်းစကားပြောဆိုနိုင်သော Group Chatbox ဖြစ်ပါသည်။",
                        color = Color(0xFFB4C8BE),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        senderName = msg.senderName,
                        senderRole = msg.senderRole,
                        text = msg.text,
                        timestampLabel = formatTimestamp(msg.createdAt),
                        isMine = msg.senderId == currentUserId,
                        canDelete = isAdminViewer || msg.senderId == currentUserId,
                        onDelete = { onDeleteMessage(msg.id) }
                    )
                }
            }
        }

        ChatComposerBar(
            draft = draft,
            onDraftChange = { draft = it },
            placeholder = "Group ထဲသို့ စာပို့ပါ...",
            testTagPrefix = "group_chat",
            onSend = {
                val clean = draft.trim()
                if (clean.isNotEmpty()) {
                    onSend(clean)
                    draft = ""
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    senderName: String,
    senderRole: String,
    text: String,
    timestampLabel: String,
    isMine: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val isFromAdmin = senderRole == "admin"
    val containerColor = when {
        isMine && isFromAdmin -> Color(0xFF4A1820)
        isMine -> Color(0xFF154734)
        isFromAdmin -> Color(0xFF2B2314)
        else -> DeepPitchElevated
    }
    val borderColor = when {
        isFromAdmin -> ChampionshipGold.copy(alpha = 0.6f)
        isMine -> Color(0xFF34D399).copy(alpha = 0.5f)
        else -> Color(0xFF2E473B)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (isFromAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isFromAdmin) ChampionshipGold else Color(0xFF9ED5B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isFromAdmin) "$senderName • Admin" else senderName,
                            color = if (isFromAdmin) ChampionshipGold else Color(0xFF9ED5B8),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete message",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timestampLabel,
                    color = Color(0xFF9FB4A9),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ChatComposerBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    placeholder: String,
    testTagPrefix: String,
    onSend: () -> Unit
) {
    Surface(
        color = DeepPitchSurface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) onDraftChange(it) },
                placeholder = { Text(placeholder) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChampionshipGold,
                    unfocusedBorderColor = Color(0xFF395649),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_input")
            )
            Button(
                onClick = onSend,
                enabled = draft.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampionshipGold,
                    contentColor = InkDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("${testTagPrefix}_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun QualityRatingScreen(
    currentUserId: String,
    isAdmin: Boolean,
    reviewsState: UiState<List<AppQualityReview>>,
    busy: Boolean,
    onSubmitReview: (stars: Int, qualityTier: String, comment: String) -> Unit,
    onDeleteReview: (targetUserId: String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            SubScreenTopBar(title = "အရည်အသွေး အဆင့်သတ်မှတ်ချက်", onBack = onBack)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (reviewsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChampionshipGold)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(reviewsState.message, color = Color(0xFFFCA5A5))
                }
                is UiState.Success -> {
                    QualityReviewsContent(
                        reviews = reviewsState.data,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        allowSubmit = true,
                        busy = busy,
                        onSubmitReview = onSubmitReview,
                        onDeleteReview = onDeleteReview
                    )
                }
            }
        }
    }
}

@Composable
fun QualityReviewsContent(
    reviews: List<AppQualityReview>,
    currentUserId: String,
    isAdmin: Boolean,
    allowSubmit: Boolean,
    busy: Boolean,
    onSubmitReview: (stars: Int, qualityTier: String, comment: String) -> Unit,
    onDeleteReview: (targetUserId: String) -> Unit
) {
    val myReview = remember(reviews, currentUserId) {
        reviews.firstOrNull { it.userId == currentUserId }
    }
    var selectedStars by remember(myReview?.stars) { mutableIntStateOf(myReview?.stars ?: 5) }
    var commentInput by remember(myReview?.comment) { mutableStateOf(myReview?.comment ?: "") }

    val tierLabel = remember(selectedStars) { qualityTierForStars(selectedStars) }
    val avgScore = remember(reviews) {
        if (reviews.isEmpty()) 0.0 else reviews.map { it.stars }.average()
    }
    val avgTier = remember(avgScore, reviews.size) {
        if (reviews.isEmpty()) "သတ်မှတ်မှု မရှိသေးပါ"
        else when {
            avgScore >= 4.5 -> "အထူးကောင်းမွန် (Grade A+)"
            avgScore >= 3.8 -> "ကောင်းမွန် (Grade A)"
            avgScore >= 3.0 -> "သင့်တင့် (Grade B)"
            avgScore >= 2.0 -> "တိုးတက်ရန်လို (Grade C)"
            else -> "အားနည်း (Grade D)"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 620.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("App ဝန်ဆောင်မှု အရည်အသွေး", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(avgTier, color = Color(0xFFD7EAE1), style = MaterialTheme.typography.bodyMedium)
                        }
                        Surface(
                            color = PitchEmeraldDark,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (reviews.isEmpty()) "--" else String.format(Locale.US, "%.1f", avgScore),
                                    color = Color.White,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text("စုစုပေါင်း သတ်မှတ်သူ: ${reviews.size} ဦး", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)

                    (5 downTo 1).forEach { star ->
                        val count = reviews.count { it.stars == star }
                        val ratio = if (reviews.isEmpty()) 0f else count.toFloat() / reviews.size.toFloat()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("$star ★", color = ChampionshipGold, fontFamily = JetBrainsMonoFontFamily, modifier = Modifier.width(34.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = ChampionshipGold,
                                trackColor = Color(0xFF284236),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text("$count", color = Color.White, fontFamily = JetBrainsMonoFontFamily, modifier = Modifier.width(28.dp))
                        }
                    }
                }
            }
        }

        // Submit Card
        if (allowSubmit) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    border = BorderStroke(1.dp, PearlCardBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (myReview != null) "သင်၏ အဆင့်သတ်မှတ်ချက်ကို ပြင်ဆင်ရန်" else "အရည်အသွေး အဆင့်သတ်မှတ်ပေးပါ",
                            color = InkDark,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { selectedStars = star },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("quality_star_$star")
                                ) {
                                    Icon(
                                        imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "$star Stars",
                                        tint = if (star <= selectedStars) Color(0xFFD97706) else InkMuted,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            color = Color(0xFFE8F5EE),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ရွေးချယ်ထားသော အဆင့်: $tierLabel",
                                color = PitchEmeraldDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { if (it.length <= 300) commentInput = it },
                            label = { Text("အကြံပြုချက် / မှတ်ချက် (Optional)") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quality_comment_input")
                        )
                        Button(
                            onClick = { onSubmitReview(selectedStars, tierLabel, commentInput) },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_quality_review_button")
                        ) {
                            Text(if (myReview != null) "အဆင့်သတ်မှတ်ချက် ပြင်မည်" else "အဆင့်သတ်မှတ်မည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Customer များ၏ အရည်အသွေး သုံးသပ်ချက်များ (${reviews.size})",
                color = ChampionshipGold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (reviews.isEmpty()) {
            item {
                Text("သုံးသပ်ချက် မရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(reviews, key = { it.userId }) { rev ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rev.displayName, color = InkDark, fontWeight = FontWeight.Bold)
                                Text(rev.qualityTier, color = PitchEmeraldDark, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "${rev.stars} ★",
                                        color = Color(0xFFB45309),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (isAdmin || rev.userId == currentUserId) {
                                    IconButton(onClick = { onDeleteReview(rev.userId) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete review", tint = AdminCrimson)
                                    }
                                }
                            }
                        }
                        if (rev.comment.isNotBlank()) {
                            Text(rev.comment, color = InkDark, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(formatTimestamp(rev.updatedAt), color = InkMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun qualityTierForStars(stars: Int): String = when (stars) {
    5 -> "အထူးကောင်းမွန် (5★)"
    4 -> "ကောင်းမွန် (4★)"
    3 -> "သင့်တင့် (3★)"
    2 -> "တိုးတက်ရန်လို (2★)"
    else -> "အားနည်း (1★)"
}
