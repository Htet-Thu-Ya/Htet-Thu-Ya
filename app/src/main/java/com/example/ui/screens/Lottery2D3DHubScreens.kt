package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Live2DStatus
import com.example.data.model.Lottery2D3DControlConfig
import com.example.data.model.Lottery2DDayRecord
import com.example.data.model.Lottery3DMonthEntry
import com.example.data.model.LotteryCalendarDoc
import com.example.data.model.LotteryDraw
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LotteryHubAmber
import com.example.ui.theme.LotteryHubBg
import com.example.ui.theme.LotteryHubInk
import com.example.ui.theme.LotteryHubIvory
import com.example.ui.theme.LotteryHubIvoryBorder
import com.example.ui.theme.LotteryHubMuted
import com.example.ui.theme.LotteryHubSurface
import com.example.ui.theme.LotteryHubSurfaceAlt
import com.example.ui.theme.LotteryHubTeal
import com.example.ui.theme.LotteryHubTealSoft
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.SyneFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class LotteryHubTab {
    LIVE,
    CALENDAR,
    PRESENT,
    BET
}

@Composable
fun CustomerLotteryHubWrapper(
    initialTab: LotteryHubTab = LotteryHubTab.BET,
    initialBetType: String = "2d",
    live2DStatus: Live2DStatus?,
    lottery2D3DConfig: Lottery2D3DControlConfig,
    lotteryCalendars: List<LotteryCalendarDoc>,
    api2DHistory: Map<String, Lottery2DDayRecord>,
    draws: List<LotteryDraw>,
    hideTwoD: Boolean = false,
    hideThreeD: Boolean = false,
    onRefreshLive2D: () -> Unit,
    onOpenQualityRating: () -> Unit,
    onBack: () -> Unit,
    twoDBetContent: @Composable () -> Unit,
    threeDBetContent: @Composable () -> Unit
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var activeBetType by remember(initialBetType) {
        mutableStateOf(
            when {
                initialBetType == "3d" && !hideThreeD -> "3d"
                !hideTwoD -> "2d"
                else -> "3d"
            }
        )
    }

    BackHandler(enabled = activeTab != initialTab) {
        activeTab = initialTab
    }

    Scaffold(
        containerColor = LotteryHubBg,
        topBar = {
            Surface(
                color = LotteryHubSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("lottery_hub_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = LotteryHubIvory
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    Brush.linearGradient(listOf(LotteryHubTeal, LotteryHubAmber)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L",
                                color = Color.White,
                                fontFamily = SyneFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LoTaYa 2D • 3D",
                                color = LotteryHubIvory,
                                fontFamily = SyneFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (activeTab) {
                                    LotteryHubTab.LIVE -> "တိုက်ရိုက် Thai SET 2D ရလဒ်"
                                    LotteryHubTab.CALENDAR -> "2D & 3D ရလဒ် ပြက္ခဒိန်"
                                    LotteryHubTab.PRESENT -> "လက်ဆောင်ကဒ် နှင့် အတိတ်စာရွက်"
                                    LotteryHubTab.BET -> if (activeBetType == "2d") "၂လုံးထီ (2D) ထိုးရန်" else "၃လုံးထီ (3D) ထိုးရန်"
                                },
                                color = Color(0xFF94A3B8),
                                fontFamily = ManropeFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (activeTab == LotteryHubTab.BET && !hideTwoD && !hideThreeD) {
                            Surface(
                                color = LotteryHubSurfaceAlt,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LotteryHubTeal.copy(alpha = 0.6f))
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    listOf("2d" to "2D", "3d" to "3D").forEach { (key, label) ->
                                        val selected = activeBetType == key
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) LotteryHubTeal else Color.Transparent)
                                                .clickable { activeBetType = key }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("lottery_hub_bet_switch_$key")
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) Color.White else Color(0xFFCBD5E1),
                                                fontFamily = SyneFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = onRefreshLive2D,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("lottery_hub_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh 2D/3D", tint = LotteryHubAmber)
                        }
                    }
                }
            }
        },
        bottomBar = {
            LotteryHubBottomBar(
                activeTab = activeTab,
                onSelectTab = { activeTab = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (activeTab) {
                LotteryHubTab.LIVE -> LotteryLiveSubview(
                    live2DStatus = live2DStatus,
                    autoMode = lottery2D3DConfig.autoMode,
                    onRefresh = onRefreshLive2D,
                    onOpenCalendar = { activeTab = LotteryHubTab.CALENDAR },
                    onOpenBet = { activeTab = LotteryHubTab.BET }
                )
                LotteryHubTab.CALENDAR -> LotteryCalendarSubview(
                    lotteryCalendars = lotteryCalendars,
                    api2DHistory = api2DHistory,
                    draws = draws,
                    live2DStatus = live2DStatus
                )
                LotteryHubTab.PRESENT -> LotteryPresentSubview(
                    config = lottery2D3DConfig,
                    onOpenQualityRating = onOpenQualityRating
                )
                LotteryHubTab.BET -> {
                    if (activeBetType == "3d" && !hideThreeD) {
                        threeDBetContent()
                    } else {
                        twoDBetContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun LotteryHubBottomBar(
    activeTab: LotteryHubTab,
    onSelectTab: (LotteryHubTab) -> Unit
) {
    val navItems = listOf(
        Triple(LotteryHubTab.LIVE, "Live", Icons.Default.LiveTv),
        Triple(LotteryHubTab.CALENDAR, "Calendar", Icons.Default.CalendarMonth),
        Triple(LotteryHubTab.PRESENT, "Present", Icons.Default.CardGiftcard),
        Triple(LotteryHubTab.BET, "ထီထိုးရန်", Icons.Default.ConfirmationNumber)
    )
    Surface(
        color = LotteryHubIvory,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, LotteryHubIvoryBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { (tab, label, icon) ->
                val selected = activeTab == tab
                val tint = if (selected) LotteryHubTeal else LotteryHubMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) LotteryHubTealSoft.copy(alpha = 0.6f) else Color.Transparent)
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 6.dp)
                        .testTag("lottery_hub_nav_${tab.name.lowercase(Locale.US)}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = tint,
                        fontFamily = ManropeFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LotteryLiveSubview(
    live2DStatus: Live2DStatus?,
    autoMode: Boolean,
    onRefresh: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBet: () -> Unit
) {
    val yangonTz = remember { TimeZone.getTimeZone("Asia/Yangon") }
    val dayLabel = remember(live2DStatus?.dayLabel) {
        live2DStatus?.dayLabel?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("EEEE", Locale.ENGLISH).apply { timeZone = yangonTz }.format(Date())
    }
    val dateLabel = remember(live2DStatus?.dateLabel) {
        live2DStatus?.dateLabel?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH).apply { timeZone = yangonTz }.format(Date())
    }
    val updatedText = remember(live2DStatus?.updatedAtText, live2DStatus?.liveTime) {
        live2DStatus?.updatedAtText?.takeIf { it.isNotBlank() }
            ?: live2DStatus?.liveTime?.takeIf { it.isNotBlank() }
            ?: SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH).apply { timeZone = yangonTz }.format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Indigo Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(LotteryHubSurface, LotteryHubSurfaceAlt)
                        )
                    )
                    .border(1.dp, LotteryHubTeal.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dayLabel,
                                color = LotteryHubIvory,
                                fontFamily = ManropeFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateLabel,
                                color = Color(0xFFCBD5E1),
                                fontFamily = ManropeFontFamily,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = if (autoMode) LotteryHubTeal.copy(alpha = 0.25f) else LotteryHubAmber.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (autoMode) "AUTO LIVE SET" else "ADMIN LIVE",
                                    color = if (autoMode) Color(0xFF5EEAD4) else Color(0xFFFCD34D),
                                    fontFamily = ManropeFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = live2DStatus?.twod?.ifBlank { "--" } ?: "--",
                            color = LotteryHubIvory,
                            fontFamily = SyneFontFamily,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("lottery_live_hero_twod")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Updated",
                                tint = LotteryHubTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Updated: $updatedText",
                                color = Color(0xFFCBD5E1),
                                fontFamily = ManropeFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = onOpenCalendar,
                            modifier = Modifier.testTag("lottery_live_open_calendar_button")
                        ) {
                            Text("ပြက္ခဒိန်", color = LotteryHubAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!live2DStatus?.setText.isNullOrBlank() && live2DStatus?.setText != "--") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = buildAnnotatedSetText(live2DStatus?.setText ?: "--"),
                                color = Color(0xFFE2E8F0),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = buildAnnotatedValText(live2DStatus?.valueText ?: "--"),
                                color = Color(0xFFE2E8F0),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // 12:01 PM Card
        item {
            SessionDrawIvoryCard(
                sessionLabel = "12:01 PM",
                setText = live2DStatus?.m1201Set ?: "--",
                valText = live2DStatus?.m1201Val ?: "--",
                twodResult = live2DStatus?.morning1201 ?: "--"
            )
        }

        // 4:30 PM Card
        item {
            SessionDrawIvoryCard(
                sessionLabel = "4:30 PM",
                setText = live2DStatus?.e430Set ?: "--",
                valText = live2DStatus?.e430Val ?: "--",
                twodResult = live2DStatus?.evening430 ?: "--"
            )
        }

        // 9:30 AM & 2:00 PM Modern / Internet Split Card
        item {
            ModernInternetSplitRow(
                am930Modern = live2DStatus?.am930Modern ?: "--",
                am930Internet = live2DStatus?.am930Internet ?: "--",
                pm200Modern = live2DStatus?.pm200Modern ?: "--",
                pm200Internet = live2DStatus?.pm200Internet ?: "--"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    border = BorderStroke(1.dp, LotteryHubTeal),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("lottery_live_sync_btn")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = LotteryHubTeal, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Live ဆွဲယူမည်", color = LotteryHubIvory, fontFamily = ManropeFontFamily)
                }
                Button(
                    onClick = onOpenBet,
                    colors = ButtonDefaults.buttonColors(containerColor = LotteryHubTeal, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("lottery_live_go_bet_btn")
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("2D / 3D ထိုးမည်", fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SessionDrawIvoryCard(
    sessionLabel: String,
    setText: String,
    valText: String,
    twodResult: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LotteryHubIvory),
        border = BorderStroke(1.dp, LotteryHubIvoryBorder),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = sessionLabel,
                    color = LotteryHubTeal,
                    fontFamily = SyneFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = buildAnnotatedSetText(setText, darkText = true),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = buildAnnotatedValText(valText, darkText = true),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(LotteryHubTealSoft)
                    .border(1.5.dp, LotteryHubTeal.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = twodResult.ifBlank { "--" },
                    color = LotteryHubTeal,
                    fontFamily = SyneFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ModernInternetSplitRow(
    am930Modern: String,
    am930Internet: String,
    pm200Modern: String,
    pm200Internet: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModernInternetBox(
            timeTitle = "9:30 AM",
            modern = am930Modern,
            internet = am930Internet,
            modifier = Modifier.weight(1f)
        )
        ModernInternetBox(
            timeTitle = "2:00 PM",
            modern = pm200Modern,
            internet = pm200Internet,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModernInternetBox(
    timeTitle: String,
    modern: String,
    internet: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LotteryHubIvory),
        border = BorderStroke(1.dp, LotteryHubIvoryBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = timeTitle,
                color = LotteryHubTeal,
                fontFamily = SyneFontFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MODERN",
                        color = LotteryHubMuted,
                        fontFamily = ManropeFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = highlightFirstDigit(modern),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(34.dp)
                        .background(LotteryHubIvoryBorder)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "INTERNET",
                        color = LotteryHubMuted,
                        fontFamily = ManropeFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = highlightFirstDigit(internet),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun buildAnnotatedSetText(rawSet: String, darkText: Boolean = false): AnnotatedString {
    val clean = rawSet.trim().ifBlank { "--" }
    val baseColor = if (darkText) LotteryHubInk else Color(0xFFE2E8F0)
    val labelColor = if (darkText) LotteryHubMuted else Color(0xFF94A3B8)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = labelColor, fontWeight = FontWeight.SemiBold)) {
            append("SET : ")
        }
        if (clean == "--" || clean.length < 2) {
            withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)) {
                append(clean)
            }
        } else {
            val idx = clean.indexOfLast { it.isDigit() }
            if (idx >= 0) {
                withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)) {
                    append(clean.substring(0, idx))
                }
                withStyle(SpanStyle(color = LotteryHubAmber, fontWeight = FontWeight.ExtraBold)) {
                    append(clean.substring(idx, idx + 1))
                }
                if (idx + 1 < clean.length) {
                    withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)) {
                        append(clean.substring(idx + 1))
                    }
                }
            } else {
                withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)) {
                    append(clean)
                }
            }
        }
    }
}

private fun buildAnnotatedValText(rawVal: String, darkText: Boolean = false): AnnotatedString {
    val clean = rawVal.trim().ifBlank { "--" }
    val baseColor = if (darkText) LotteryHubInk else Color(0xFFE2E8F0)
    val labelColor = if (darkText) LotteryHubMuted else Color(0xFF94A3B8)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = labelColor, fontWeight = FontWeight.SemiBold)) {
            append("VAL : ")
        }
        val dotIdx = clean.indexOf('.')
        val highlightIdx = if (dotIdx > 0 && clean[dotIdx - 1].isDigit()) dotIdx - 1 else clean.indexOfLast { it.isDigit() }
        if (clean == "--" || highlightIdx < 0) {
            withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)) {
                append(clean)
            }
        } else {
            withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)) {
                append(clean.substring(0, highlightIdx))
            }
            withStyle(SpanStyle(color = LotteryHubAmber, fontWeight = FontWeight.ExtraBold)) {
                append(clean.substring(highlightIdx, highlightIdx + 1))
            }
            if (highlightIdx + 1 < clean.length) {
                withStyle(SpanStyle(color = baseColor, fontWeight = FontWeight.Bold)) {
                    append(clean.substring(highlightIdx + 1))
                }
            }
        }
    }
}

private fun highlightFirstDigit(raw: String): AnnotatedString {
    val clean = raw.trim().ifBlank { "--" }
    return buildAnnotatedString {
        if (clean == "--" || !clean.first().isDigit()) {
            withStyle(SpanStyle(color = LotteryHubInk)) { append(clean) }
        } else {
            withStyle(SpanStyle(color = LotteryHubAmber, fontWeight = FontWeight.ExtraBold)) {
                append(clean.substring(0, 1))
            }
            withStyle(SpanStyle(color = LotteryHubTeal, fontWeight = FontWeight.Bold)) {
                append(clean.substring(1))
            }
        }
    }
}

@Composable
private fun LotteryCalendarSubview(
    lotteryCalendars: List<LotteryCalendarDoc>,
    api2DHistory: Map<String, Lottery2DDayRecord>,
    draws: List<LotteryDraw>,
    live2DStatus: Live2DStatus?
) {
    val yangonTz = remember { TimeZone.getTimeZone("Asia/Yangon") }
    val initCal = remember { Calendar.getInstance(yangonTz) }
    var is2DCalendar by remember { mutableStateOf(true) }
    var selectedYear by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initCal.get(Calendar.MONTH) + 1) } // 1..12
    var inspectingDayDetail by remember { mutableStateOf<Pair<String, Lottery2DDayRecord>?>(null) }

    val monthNames = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Segmented Toggle: 2D Calendar | 3D Calendar
        Surface(
            color = LotteryHubSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, LotteryHubTeal.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(true to "2D Calendar", false to "3D Calendar").forEach { (is2D, title) ->
                    val selected = is2DCalendar == is2D
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) LotteryHubTeal else Color.Transparent)
                            .clickable { is2DCalendar = is2D }
                            .testTag(if (is2D) "calendar_tab_2d" else "calendar_tab_3d"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (selected) Color.White else Color(0xFF94A3B8),
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (is2DCalendar) {
            // Month Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedMonth == 1) {
                            selectedMonth = 12
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(LotteryHubSurface, CircleShape)
                        .border(1.dp, LotteryHubTeal.copy(alpha = 0.5f), CircleShape)
                        .testTag("calendar_2d_prev_month")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = LotteryHubTeal)
                }
                Text(
                    text = "${monthNames[(selectedMonth - 1).coerceIn(0, 11)]} $selectedYear",
                    color = LotteryHubIvory,
                    fontFamily = SyneFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        if (selectedMonth == 12) {
                            selectedMonth = 1
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(LotteryHubSurface, CircleShape)
                        .border(1.dp, LotteryHubTeal.copy(alpha = 0.5f), CircleShape)
                        .testTag("calendar_2d_next_month")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = LotteryHubTeal)
                }
            }

            // Weekday Header
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                    Text(
                        text = d,
                        color = Color(0xFF94A3B8),
                        fontFamily = ManropeFontFamily,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val monthCalDoc = remember(lotteryCalendars, selectedYear, selectedMonth) {
                val id = "2d_${selectedYear}_$selectedMonth"
                lotteryCalendars.firstOrNull { it.id == id }
            }
            val todayIso = remember {
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = yangonTz }.format(Date())
            }
            val firstDayCal = remember(selectedYear, selectedMonth) {
                Calendar.getInstance(yangonTz).apply {
                    set(selectedYear, selectedMonth - 1, 1, 0, 0, 0)
                }
            }
            val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val startOffset = firstDayCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            val gridCells = remember(selectedYear, selectedMonth, daysInMonth, startOffset) {
                buildList {
                    repeat(startOffset) { add(0) }
                    for (day in 1..daysInMonth) add(day)
                    while (size % 7 != 0) add(0)
                }.chunked(7)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(gridCells) { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        week.forEach { dayNum ->
                            if (dayNum == 0) {
                                Spacer(Modifier.weight(1f).height(64.dp))
                            } else {
                                val dayKey = dayNum.toString().padStart(2, '0')
                                val monthKey = selectedMonth.toString().padStart(2, '0')
                                val isoKey = "$selectedYear-$monthKey-$dayKey"
                                val adminRec = monthCalDoc?.days?.get(dayKey)?.let { Lottery2DDayRecord.fromMap(it) }
                                val apiRec = api2DHistory[isoKey]
                                val todayFallback = if (isoKey == todayIso && live2DStatus != null) {
                                    Lottery2DDayRecord(
                                        m1201Set = live2DStatus.m1201Set,
                                        m1201Val = live2DStatus.m1201Val,
                                        m1201Twod = live2DStatus.morning1201,
                                        e430Set = live2DStatus.e430Set,
                                        e430Val = live2DStatus.e430Val,
                                        e430Twod = live2DStatus.evening430,
                                        am930Modern = live2DStatus.am930Modern,
                                        am930Internet = live2DStatus.am930Internet,
                                        pm200Modern = live2DStatus.pm200Modern,
                                        pm200Internet = live2DStatus.pm200Internet
                                    )
                                } else null
                                val rec = adminRec ?: apiRec ?: todayFallback
                                val hasValues = rec != null && (rec.m1201Twod != "--" || rec.e430Twod != "--")
                                val isToday = isoKey == todayIso
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isToday -> LotteryHubTeal.copy(alpha = 0.25f)
                                                hasValues -> LotteryHubSurfaceAlt
                                                else -> LotteryHubSurface
                                            }
                                        )
                                        .border(
                                            width = if (isToday) 1.5.dp else 1.dp,
                                            color = if (isToday) LotteryHubAmber else LotteryHubTeal.copy(alpha = if (hasValues) 0.55f else 0.18f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = rec != null) {
                                            if (rec != null) inspectingDayDetail = isoKey to rec
                                        }
                                        .padding(4.dp)
                                        .testTag("calendar_day_$dayKey"),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            color = if (hasValues || isToday) LotteryHubIvory else LotteryHubMuted,
                                            fontFamily = ManropeFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (rec != null && hasValues) {
                                            Text(
                                                text = rec.m1201Twod.takeIf { it != "--" } ?: "--",
                                                color = Color(0xFF5EEAD4),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = rec.e430Twod.takeIf { it != "--" } ?: "--",
                                                color = LotteryHubAmber,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 3D Calendar by Year
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedYear -= 1 },
                    modifier = Modifier
                        .size(48.dp)
                        .background(LotteryHubSurface, CircleShape)
                        .border(1.dp, LotteryHubTeal.copy(alpha = 0.5f), CircleShape)
                        .testTag("calendar_3d_prev_year")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Year", tint = LotteryHubTeal)
                }
                Text(
                    text = "$selectedYear (3D Calendar)",
                    color = LotteryHubIvory,
                    fontFamily = SyneFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { selectedYear += 1 },
                    modifier = Modifier
                        .size(48.dp)
                        .background(LotteryHubSurface, CircleShape)
                        .border(1.dp, LotteryHubTeal.copy(alpha = 0.5f), CircleShape)
                        .testTag("calendar_3d_next_year")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Year", tint = LotteryHubTeal)
                }
            }

            val yearDoc = remember(lotteryCalendars, selectedYear) {
                lotteryCalendars.firstOrNull { it.id == "3d_$selectedYear" }
            }
            val finished3DDraws = remember(draws) {
                draws.filter { it.drawType != "2d" && it.winningNumber.isNotBlank() }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items((1..12).toList()) { mIdx ->
                    val mKey = mIdx.toString().padStart(2, '0')
                    val mName = monthNames[mIdx - 1]
                    val adminEntry = yearDoc?.months?.get(mKey)?.let { Lottery3DMonthEntry.fromMap(it) }
                    val fallbackDraws = finished3DDraws.filter {
                        it.roundTitle.contains(mName.take(3), ignoreCase = true) &&
                            it.roundTitle.contains(selectedYear.toString())
                    }
                    val entry = adminEntry ?: Lottery3DMonthEntry(
                        firstDay = "01",
                        firstNum = fallbackDraws.getOrNull(0)?.winningNumber ?: "---",
                        secondDay = "16",
                        secondNum = fallbackDraws.getOrNull(1)?.winningNumber ?: "---"
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LotteryHubIvory),
                        border = BorderStroke(1.dp, LotteryHubIvoryBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "$mName $selectedYear",
                                color = LotteryHubTeal,
                                fontFamily = SyneFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = LotteryHubTealSoft,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${entry.firstDay} ရက်", color = LotteryHubMuted, style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            text = entry.firstNum,
                                            color = LotteryHubInk,
                                            fontFamily = SyneFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${entry.secondDay} ရက်", color = LotteryHubMuted, style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            text = entry.secondNum,
                                            color = LotteryHubAmber,
                                            fontFamily = SyneFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectingDayDetail?.let { (dateIso, rec) ->
        AlertDialog(
            onDismissRequest = { inspectingDayDetail = null },
            containerColor = LotteryHubSurface,
            title = {
                Text(
                    text = "2D ရလဒ် ($dateIso)",
                    color = LotteryHubIvory,
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SessionDrawIvoryCard(
                        sessionLabel = "12:01 PM",
                        setText = rec.m1201Set,
                        valText = rec.m1201Val,
                        twodResult = rec.m1201Twod
                    )
                    SessionDrawIvoryCard(
                        sessionLabel = "4:30 PM",
                        setText = rec.e430Set,
                        valText = rec.e430Val,
                        twodResult = rec.e430Twod
                    )
                    ModernInternetSplitRow(
                        am930Modern = rec.am930Modern,
                        am930Internet = rec.am930Internet,
                        pm200Modern = rec.pm200Modern,
                        pm200Internet = rec.pm200Internet
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { inspectingDayDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = LotteryHubTeal, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("calendar_day_detail_close")
                ) {
                    Text("Close", fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun LotteryPresentSubview(
    config: Lottery2D3DControlConfig,
    onOpenQualityRating: () -> Unit
) {
    var selectedCardDetail by remember { mutableStateOf<Pair<String, String>?>(null) }

    val presentCards = remember(config) {
        listOf(
            "ပခုက္ကူ ရွှေစွန်ညို" to config.presentPakokku,
            "နေ့တိုက်ကဒ်" to config.presentDailyCard,
            "တစ်ပတ်စာကဒ်" to config.presentWeeklyCard,
            "Thai Cards (နေ့တိုက်ထိုင်းကဒ်)" to config.presentThaiCard,
            "Atake Cards (အတိတ်စာရွက်)" to config.presentAtakeCard
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rate Us Ivory Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LotteryHubIvory),
                border = BorderStroke(1.dp, LotteryHubIvoryBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "သင် ကြိုက်နှစ်သက်ပါသလား?",
                        color = LotteryHubInk,
                        fontFamily = ManropeFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = LotteryHubAmber,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Button(
                        onClick = onOpenQualityRating,
                        colors = ButtonDefaults.buttonColors(containerColor = LotteryHubTeal, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("present_rate_us_button")
                    ) {
                        Text("Rate Us • အဆင့်သတ်မှတ်ရန်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(presentCards.withIndex().toList(), key = { it.index }) { (idx, itemPair) ->
            val (title, body) = itemPair
            Card(
                colors = CardDefaults.cardColors(containerColor = LotteryHubIvory),
                border = BorderStroke(1.dp, LotteryHubIvoryBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedCardDetail = title to body }
                    .testTag("present_card_$idx")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(LotteryHubTealSoft, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = title,
                                tint = LotteryHubTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                color = LotteryHubInk,
                                fontFamily = ManropeFontFamily,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = body,
                                color = LotteryHubMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open $title",
                        tint = LotteryHubTeal
                    )
                }
            }
        }
    }

    selectedCardDetail?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { selectedCardDetail = null },
            containerColor = LotteryHubIvory,
            title = {
                Text(
                    text = title,
                    color = LotteryHubTeal,
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = body,
                    color = LotteryHubInk,
                    fontFamily = ManropeFontFamily,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedCardDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = LotteryHubTeal, contentColor = Color.White),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Admin2D3DControlRoomTab(
    config: Lottery2D3DControlConfig,
    live2DStatus: Live2DStatus?,
    busy: Boolean,
    onSyncFromApiNow: () -> Unit,
    onSaveConfig: (Lottery2D3DControlConfig) -> Unit,
    onTriggerManualNoti: (title: String, body: String) -> Unit,
    onSave2DCalendarDay: (year: Int, month: Int, dayKey: String, record: Lottery2DDayRecord) -> Unit,
    onSave3DCalendarMonth: (year: Int, monthKey: String, entry: Lottery3DMonthEntry) -> Unit
) {
    var autoMode by remember(config) { mutableStateOf(config.autoMode) }
    var autoNotify by remember(config) { mutableStateOf(config.autoNotifyOnResult) }
    var liveTwod by remember(config) { mutableStateOf(config.liveTwod) }
    var liveSet by remember(config) { mutableStateOf(config.liveSet) }
    var liveVal by remember(config) { mutableStateOf(config.liveVal) }
    var liveUpdated by remember(config) { mutableStateOf(config.liveUpdatedText) }

    var m1201Set by remember(config) { mutableStateOf(config.m1201Set) }
    var m1201Val by remember(config) { mutableStateOf(config.m1201Val) }
    var m1201Twod by remember(config) { mutableStateOf(config.m1201Twod) }
    var e430Set by remember(config) { mutableStateOf(config.e430Set) }
    var e430Val by remember(config) { mutableStateOf(config.e430Val) }
    var e430Twod by remember(config) { mutableStateOf(config.e430Twod) }
    var am930Mod by remember(config) { mutableStateOf(config.am930Modern) }
    var am930Int by remember(config) { mutableStateOf(config.am930Internet) }
    var pm200Mod by remember(config) { mutableStateOf(config.pm200Modern) }
    var pm200Int by remember(config) { mutableStateOf(config.pm200Internet) }

    var pakokku by remember(config) { mutableStateOf(config.presentPakokku) }
    var dailyCard by remember(config) { mutableStateOf(config.presentDailyCard) }
    var weeklyCard by remember(config) { mutableStateOf(config.presentWeeklyCard) }
    var thaiCard by remember(config) { mutableStateOf(config.presentThaiCard) }
    var atakeCard by remember(config) { mutableStateOf(config.presentAtakeCard) }

    var notiTitle by remember { mutableStateOf("🎯 လိုတရ 2D / 3D ထီရလဒ် အသိပေးချက်") }
    var notiBody by remember { mutableStateOf("") }

    val yangonCal = remember { Calendar.getInstance(TimeZone.getTimeZone("Asia/Yangon")) }
    var cal2DYear by remember { mutableStateOf(yangonCal.get(Calendar.YEAR).toString()) }
    var cal2DMonth by remember { mutableStateOf((yangonCal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')) }
    var cal2DDay by remember { mutableStateOf(yangonCal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')) }

    var cal3DYear by remember { mutableStateOf(yangonCal.get(Calendar.YEAR).toString()) }
    var cal3DMonth by remember { mutableStateOf((yangonCal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')) }
    var cal3DFirstDay by remember { mutableStateOf("01") }
    var cal3DFirstNum by remember { mutableStateOf("") }
    var cal3DSecondDay by remember { mutableStateOf("16") }
    var cal3DSecondNum by remember { mutableStateOf("") }

    var showPresentEditor by remember { mutableStateOf(false) }

    val whiteFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Auto vs Manual Mode & Thai SET Sync
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "2D / 3D သီးသန့် ထိန်းချုပ်ခန်း (Auto & Manual)",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Thai SET Live Mode", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (autoMode) "Thai SET API မှ အလိုအလျောက် တင်ဆက်နေသည်" else "Admin ကိုယ်တိုင် Manual ရိုက်ထည့်ထားသော ဂဏန်းပြနေသည်",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = autoMode,
                            onCheckedChange = { autoMode = it },
                            modifier = Modifier.testTag("control_room_auto_mode_switch")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("2D / 3D Result Auto Noti", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "12:01 / 4:30 / 3D ရလဒ်ထွက်တိုင်း Noti သီးသန့်ထုတ်ပေးမည်",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = autoNotify,
                            onCheckedChange = { autoNotify = it },
                            modifier = Modifier.testTag("control_room_auto_noti_switch")
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSyncFromApiNow,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = LotteryHubTeal, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("control_room_sync_api_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Thai SET မှ ချက်ချင်းဆွဲမည်", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = {
                                if (live2DStatus != null) {
                                    liveTwod = live2DStatus.twod
                                    liveSet = live2DStatus.setText
                                    liveVal = live2DStatus.valueText
                                    liveUpdated = live2DStatus.updatedAtText
                                    m1201Set = live2DStatus.m1201Set
                                    m1201Val = live2DStatus.m1201Val
                                    m1201Twod = live2DStatus.morning1201
                                    e430Set = live2DStatus.e430Set
                                    e430Val = live2DStatus.e430Val
                                    e430Twod = live2DStatus.evening430
                                    am930Mod = live2DStatus.am930Modern
                                    am930Int = live2DStatus.am930Internet
                                    pm200Mod = live2DStatus.pm200Modern
                                    pm200Int = live2DStatus.pm200Internet
                                }
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("API Box ဖြည့်", color = ChampionshipGold)
                        }
                    }
                }
            }
        }

        // 2. Manual / Live 2D Editor
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Live 2D, 12:01, 4:30 & Modern/Internet ပြင်ဆင်ရန်",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = liveTwod, onValueChange = { liveTwod = it.take(4) }, label = { Text("Live 2D") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = liveSet, onValueChange = { liveSet = it }, label = { Text("Live SET") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = liveVal, onValueChange = { liveVal = it }, label = { Text("Live VAL") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = liveUpdated, onValueChange = { liveUpdated = it }, label = { Text("Updated Label") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = m1201Twod, onValueChange = { m1201Twod = it.take(4) }, label = { Text("12:01 2D") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = m1201Set, onValueChange = { m1201Set = it }, label = { Text("12:01 SET") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = m1201Val, onValueChange = { m1201Val = it }, label = { Text("12:01 VAL") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = e430Twod, onValueChange = { e430Twod = it.take(4) }, label = { Text("4:30 2D") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = e430Set, onValueChange = { e430Set = it }, label = { Text("4:30 SET") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = e430Val, onValueChange = { e430Val = it }, label = { Text("4:30 VAL") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = am930Mod, onValueChange = { am930Mod = it.take(6) }, label = { Text("9:30 Mod") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = am930Int, onValueChange = { am930Int = it.take(6) }, label = { Text("9:30 Int") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pm200Mod, onValueChange = { pm200Mod = it.take(6) }, label = { Text("2:00 Mod") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pm200Int, onValueChange = { pm200Int = it.take(6) }, label = { Text("2:00 Int") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            onSaveConfig(
                                config.copy(
                                    autoMode = autoMode,
                                    autoNotifyOnResult = autoNotify,
                                    liveTwod = liveTwod.ifBlank { "--" },
                                    liveSet = liveSet.ifBlank { "--" },
                                    liveVal = liveVal.ifBlank { "--" },
                                    liveUpdatedText = liveUpdated,
                                    m1201Set = m1201Set.ifBlank { "--" },
                                    m1201Val = m1201Val.ifBlank { "--" },
                                    m1201Twod = m1201Twod.ifBlank { "--" },
                                    e430Set = e430Set.ifBlank { "--" },
                                    e430Val = e430Val.ifBlank { "--" },
                                    e430Twod = e430Twod.ifBlank { "--" },
                                    am930Modern = am930Mod.ifBlank { "--" },
                                    am930Internet = am930Int.ifBlank { "--" },
                                    pm200Modern = pm200Mod.ifBlank { "--" },
                                    pm200Internet = pm200Int.ifBlank { "--" },
                                    presentPakokku = pakokku,
                                    presentDailyCard = dailyCard,
                                    presentWeeklyCard = weeklyCard,
                                    presentThaiCard = thaiCard,
                                    presentAtakeCard = atakeCard
                                )
                            )
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("control_room_save_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Live & 2D/3D ဆက်တင် သိမ်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Manual Push Notification Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, LotteryHubTeal.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = LotteryHubTeal)
                        Spacer(Modifier.width(8.dp))
                        Text("2D / 3D သီးသန့် Noti ပို့ရန်", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = notiTitle,
                        onValueChange = { notiTitle = it },
                        label = { Text("Noti ခေါင်းစဉ်") },
                        singleLine = true,
                        colors = whiteFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notiBody,
                        onValueChange = { notiBody = it },
                        label = { Text("Noti စာသား (Blank ထားပါက လက်ရှိ 2D ရလဒ် အလိုအလျောက်ထည့်မည်)") },
                        minLines = 2,
                        maxLines = 4,
                        colors = whiteFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onTriggerManualNoti(notiTitle, notiBody) },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = LotteryHubTeal, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("control_room_send_noti_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("2D/3D Noti ချက်ချင်းပို့မည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. 2D Calendar Date Editor
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2D ပြက္ခဒိန် ရက်စွဲ ထည့်/ပြင်ရန်", color = ChampionshipGold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cal2DYear,
                            onValueChange = { cal2DYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Year") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = whiteFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cal2DMonth,
                            onValueChange = { cal2DMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Month") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = whiteFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cal2DDay,
                            onValueChange = { cal2DDay = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Day (01-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = whiteFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val yr = cal2DYear.toIntOrNull() ?: 2026
                            val mo = (cal2DMonth.toIntOrNull() ?: 1).coerceIn(1, 12)
                            val dk = (cal2DDay.toIntOrNull() ?: 1).coerceIn(1, 31).toString().padStart(2, '0')
                            onSave2DCalendarDay(
                                yr,
                                mo,
                                dk,
                                Lottery2DDayRecord(
                                    m1201Set = m1201Set.ifBlank { "--" },
                                    m1201Val = m1201Val.ifBlank { "--" },
                                    m1201Twod = m1201Twod.ifBlank { "--" },
                                    e430Set = e430Set.ifBlank { "--" },
                                    e430Val = e430Val.ifBlank { "--" },
                                    e430Twod = e430Twod.ifBlank { "--" },
                                    am930Modern = am930Mod.ifBlank { "--" },
                                    am930Internet = am930Int.ifBlank { "--" },
                                    pm200Modern = pm200Mod.ifBlank { "--" },
                                    pm200Internet = pm200Int.ifBlank { "--" }
                                )
                            )
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("control_room_save_2d_calendar_button")
                    ) {
                        Text("အထက်ပါ 12:01 / 4:30 / Mod-Int ကို 2D Calendar ထဲသိမ်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. 3D Calendar Month Editor
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3D ပြက္ခဒိန် လစဉ်ဂဏန်း ထည့်/ပြင်ရန်", color = ChampionshipGold, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cal3DYear,
                            onValueChange = { cal3DYear = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Year") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = whiteFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cal3DMonth,
                            onValueChange = { cal3DMonth = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Month (01-12)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = whiteFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cal3DFirstDay, onValueChange = { cal3DFirstDay = it.take(4) }, label = { Text("1st Day") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(0.7f))
                        OutlinedTextField(value = cal3DFirstNum, onValueChange = { cal3DFirstNum = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("1st 3D") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = cal3DSecondDay, onValueChange = { cal3DSecondDay = it.take(4) }, label = { Text("2nd Day") }, singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(0.7f))
                        OutlinedTextField(value = cal3DSecondNum, onValueChange = { cal3DSecondNum = it.filter { c -> c.isDigit() }.take(3) }, label = { Text("2nd 3D") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = whiteFieldColors, modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            val yr = cal3DYear.toIntOrNull() ?: 2026
                            val mk = (cal3DMonth.toIntOrNull() ?: 1).coerceIn(1, 12).toString().padStart(2, '0')
                            onSave3DCalendarMonth(
                                yr,
                                mk,
                                Lottery3DMonthEntry(
                                    firstDay = cal3DFirstDay.ifBlank { "01" },
                                    firstNum = cal3DFirstNum.ifBlank { "---" },
                                    secondDay = cal3DSecondDay.ifBlank { "16" },
                                    secondNum = cal3DSecondNum.ifBlank { "---" }
                                )
                            )
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = LotteryHubTeal, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("control_room_save_3d_calendar_button")
                    ) {
                        Text("3D Calendar လစဉ် သိမ်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. Present Cards Editor (Collapsible)
        item {
            HideBarSectionHeader(
                title = "Present ကဒ် ၅ ခု စာသား/ဂဏန်း ပြင်ရန်",
                isVisible = showPresentEditor,
                onToggle = { showPresentEditor = !showPresentEditor },
                testTag = "hide_bar_control_room_present"
            )
        }
        if (showPresentEditor) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pakokku, onValueChange = { pakokku = it }, label = { Text("ပခုက္ကူ ရွှေစွန်ညို") }, minLines = 2, colors = whiteFieldColors, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dailyCard, onValueChange = { dailyCard = it }, label = { Text("နေ့တိုက်ကဒ်") }, minLines = 2, colors = whiteFieldColors, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = weeklyCard, onValueChange = { weeklyCard = it }, label = { Text("တစ်ပတ်စာကဒ်") }, minLines = 2, colors = whiteFieldColors, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = thaiCard, onValueChange = { thaiCard = it }, label = { Text("Thai Cards (နေ့တိုက်ထိုင်းကဒ်)") }, minLines = 2, colors = whiteFieldColors, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = atakeCard, onValueChange = { atakeCard = it }, label = { Text("Atake Cards (အတိတ်စာရွက်)") }, minLines = 2, colors = whiteFieldColors, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                onSaveConfig(
                                    config.copy(
                                        autoMode = autoMode,
                                        autoNotifyOnResult = autoNotify,
                                        presentPakokku = pakokku,
                                        presentDailyCard = dailyCard,
                                        presentWeeklyCard = weeklyCard,
                                        presentThaiCard = thaiCard,
                                        presentAtakeCard = atakeCard
                                    )
                                )
                            },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("control_room_save_present_button")
                        ) {
                            Text("Present ကဒ်စာသားများ သိမ်းမည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
