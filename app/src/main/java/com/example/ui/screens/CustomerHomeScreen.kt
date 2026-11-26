package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.AppUpdateConfig
import com.example.data.model.AvailableApkUpdate
import com.example.data.model.BrandingConfig
import com.example.data.model.SystemGameConfig
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.network.ApkDownloadHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.components.AdminInviteRedeemDialog
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.PauseStatusBannerCard
import com.example.ui.components.formatKs
import com.example.ui.components.formatRemainingReopenCountdown
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ChampionshipGoldBright
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MixAmber
import com.example.ui.theme.MixCharcoal
import com.example.ui.theme.OneXBetBlue
import com.example.ui.theme.OneXBetNavy
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldDark
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.WhiteCardBorder

enum class CustomerRoute {
    PARLAY,
    BODY_GOAL,
    ONE_EXPRESS,
    ONE_EXPRESS_PARLAY,
    FIVE_FIVE_FIVE,
    FIVE_FIVE_FIVE_PARLAY,
    LOTTERY_HUB,
    TWO_D,
    THREE_D,
    DICE_GAME,
    SIX_ANIMALS,
    DRAGON_TIGER,
    LUCKY_WHEEL,
    ACTIVE_ORDERS,
    PAST_ORDERS,
    LEDGER,
    RESULTS,
    LEADERBOARD,
    POINT_EXCHANGE,
    SETTINGS,
    CHAT,
    QUALITY
}

private data class MenuTile(
    val route: CustomerRoute,
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeColor: Color,
    val testTag: String,
    val gameKey: String? = null
)

@Composable
fun CustomerHomeScreen(
    accountState: UiState<UserAccount?>,
    displayName: String,
    isAdmin: Boolean,
    pendingAdminCount: Int,
    systemGameConfig: SystemGameConfig = SystemGameConfig(),
    brandingConfig: BrandingConfig = BrandingConfig(),
    appLanguage: AppLanguage = AppLanguage.MY,
    availableApkUpdate: AvailableApkUpdate? = null,
    apkDownloadUrl: String = AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL,
    pendingInviteCode: String? = null,
    onConsumePendingInviteCode: () -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onNavigate: (CustomerRoute) -> Unit,
    onSwitchToAdmin: () -> Unit,
    onRedeemInviteCode: (String) -> Unit,
    onStartVoiceCall: () -> Unit = {},
    onStartVideoCall: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showInviteDialog by remember { mutableStateOf(false) }
    var prefillInviteCode by remember { mutableStateOf("") }
    LaunchedEffect(pendingInviteCode) {
        val incoming = pendingInviteCode?.trim().orEmpty()
        if (incoming.isNotBlank()) {
            prefillInviteCode = incoming
            showInviteDialog = true
        }
    }
    var directDownloading by remember { mutableStateOf(false) }
    var directDownloadProgress by remember { mutableIntStateOf(0) }
    var directDownloadError by remember { mutableStateOf<String?>(null) }
    var showWalletCard by remember { mutableStateOf(true) }
    var showGamesGrid by remember { mutableStateOf(true) }
    var showApkCard by remember { mutableStateOf(true) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val account = (accountState as? UiState.Success)?.data
    val isLocked = account?.isLocked == true
    val isMaintenance = systemGameConfig.maintenanceMode
    val footballPausedNow = systemGameConfig.isFootballPausedNow(nowMs)
    val gamesPausedNow = systemGameConfig.isGamesPausedNow(nowMs)
    val isEnglish = appLanguage == AppLanguage.EN

    val menuTiles = remember(isEnglish, systemGameConfig.hideTwoD, systemGameConfig.hideThreeD) {
        buildList {
            add(MenuTile(CustomerRoute.PARLAY, if (isEnglish) "Parlay" else "မောင်း", Icons.Default.Stadium, Color(0xFF16A34A), Color(0xFFDCFCE7), "menu_parlay", "parlay"))
            add(MenuTile(CustomerRoute.BODY_GOAL, if (isEnglish) "Body / O-U" else "ဘော်ဒီ/ဂိုးပေါင်း", Icons.Default.SportsSoccer, Color(0xFF1E293B), Color(0xFFE2E8F0), "menu_body_goal", "body_goal"))
            if (!systemGameConfig.hideTwoD || !systemGameConfig.hideThreeD) {
                add(MenuTile(CustomerRoute.LOTTERY_HUB, if (isEnglish) "2D/3D Live & Calendar" else "2D/3D Live & ပြက္ခဒိန်", Icons.Default.CalendarMonth, Color(0xFF0D9488), Color(0xFFCCFBF1), "menu_lottery_hub", if (!systemGameConfig.hideTwoD) "two_d" else "three_d"))
            }
            if (!systemGameConfig.hideTwoD) {
                add(MenuTile(CustomerRoute.TWO_D, if (isEnglish) "2D Lottery" else "၂လုံးထီ (2D)", Icons.Default.TrendingUp, Color(0xFF0F766E), Color(0xFFCCFBF1), "menu_two_d", "two_d"))
            }
            if (!systemGameConfig.hideThreeD) {
                add(MenuTile(CustomerRoute.THREE_D, if (isEnglish) "3D Lottery" else "၃လုံးထီ (3D)", Icons.Default.Casino, Color(0xFF059669), Color(0xFFD1FAE5), "menu_three_d", "three_d"))
            }
            add(MenuTile(CustomerRoute.DICE_GAME, if (isEnglish) "Dice Roll" else "အံစာတုံး", Icons.Default.Casino, Color(0xFF7C3AED), Color(0xFFEDE9FE), "menu_dice", "dice"))
            add(MenuTile(CustomerRoute.SIX_ANIMALS, if (isEnglish) "6 Animals" else "၆ကောင်ဂျင်", Icons.Default.Pets, Color(0xFFB45309), Color(0xFFFEF3C7), "menu_six_animals", "six_animals"))
            add(MenuTile(CustomerRoute.DRAGON_TIGER, if (isEnglish) "Dragon Tiger" else "နဂါး vs ကျား", Icons.Default.Bolt, Color(0xFFDC2626), Color(0xFFFEE2E2), "menu_dragon_tiger", "dragon_tiger"))
            add(MenuTile(CustomerRoute.LUCKY_WHEEL, if (isEnglish) "Lucky Wheel" else "ကံစမ်းဘီး", Icons.Default.AutoAwesome, Color(0xFFD97706), Color(0xFFFEF3C7), "menu_lucky_wheel", "lucky_wheel"))
            add(MenuTile(CustomerRoute.ACTIVE_ORDERS, if (isEnglish) "Active Slips" else "လောင်းထားသောပွဲစဉ်များ", Icons.Default.History, Color(0xFF334155), Color(0xFFE2E8F0), "menu_active_orders"))
            add(MenuTile(CustomerRoute.PAST_ORDERS, if (isEnglish) "Past Slips" else "ပွဲစဉ်ဟောင်းများ", Icons.Default.CalendarMonth, Color(0xFFEA580C), Color(0xFFFFEDD5), "menu_past_orders"))
            add(MenuTile(CustomerRoute.LEDGER, if (isEnglish) "Wallet" else "ငွေစာရင်း", Icons.Default.MonetizationOn, Color(0xFFD97706), Color(0xFFFEF3C7), "menu_ledger"))
            add(MenuTile(CustomerRoute.RESULTS, if (isEnglish) "Results" else "ပွဲပြီး ရလဒ်များ", Icons.Default.LiveTv, Color(0xFF0284C7), Color(0xFFE0F2FE), "menu_results"))
            add(MenuTile(CustomerRoute.LEADERBOARD, if (isEnglish) "Leaderboard" else "အဆင့်ဇယား", Icons.Default.EmojiEvents, Color(0xFFEA580C), Color(0xFFFFEDD5), "menu_leaderboard"))
            add(MenuTile(CustomerRoute.POINT_EXCHANGE, if (isEnglish) "Point Shop" else "ပွိုင့်လဲလှယ်", Icons.Default.CurrencyExchange, Color(0xFF16A34A), Color(0xFFDCFCE7), "menu_point_exchange"))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF060E0B), DeepPitchBg, DeepPitchSurface)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        BrandLogoAvatar(brandingConfig = brandingConfig, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = brandingConfig.appName.ifBlank { "လိုတရ" },
                                style = MaterialTheme.typography.titleLarge,
                                color = ChampionshipGoldBright,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = account?.displayName ?: displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFC0D4CA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAdmin) {
                            Surface(
                                color = AdminCrimson,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .clickable(onClick = onSwitchToAdmin)
                                    .testTag("switch_to_admin_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (pendingAdminCount > 0) {
                                            Badge(containerColor = ChampionshipGold, contentColor = InkDark) {
                                                Text("$pendingAdminCount")
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = "Admin Mode",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Admin",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = DeepPitchElevated,
                                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .clickable {
                                        prefillInviteCode = ""
                                        showInviteDialog = true
                                    }
                                    .testTag("open_admin_invite_dialog")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Key,
                                        contentDescription = "Join Admin",
                                        tint = ChampionshipGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Admin Key",
                                        color = ChampionshipGold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("customer_refresh_button")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = ChampionshipGold
                            )
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = Color(0xFFB3C2BA)
                            )
                        }
                    }
                }
            }

            if (isLocked) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        color = AdminCrimson,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("account_locked_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "သင့်အကောင့်ကို Admin မှ ယာယီပိတ်ထားပါသည် (Locked) — Chat မှတစ်ဆင့် ဆက်သွယ်ပါ။",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isMaintenance) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        color = Color(0xFFB45309),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("maintenance_mode_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = systemGameConfig.maintenanceMessage.ifBlank { "စနစ်ပြုပြင်ထိန်းသိမ်းမှု ပြုလုပ်နေပါသည်။" },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (systemGameConfig.bonusPromoText.isNotBlank()) {
                item(span = { GridItemSpan(2) }) {
                    Surface(
                        color = DeepPitchElevated,
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = ChampionshipGoldBright, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = systemGameConfig.bonusPromoText,
                                color = ChampionshipGoldBright,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (footballPausedNow && (!isMaintenance || systemGameConfig.footballReopenAtMs > nowMs)) {
                item(span = { GridItemSpan(2) }) {
                    PauseStatusBannerCard(
                        title = "ဘောလုံးပွဲ လောင်းကြေးတင်ခွင့် ခေတ္တပိတ်ထားပါသည်",
                        reopenAtMs = systemGameConfig.footballReopenAtMs,
                        nowMs = nowMs
                    )
                }
            }

            if (gamesPausedNow && (!isMaintenance || systemGameConfig.gamesReopenAtMs > nowMs)) {
                item(span = { GridItemSpan(2) }) {
                    PauseStatusBannerCard(
                        title = "ဂိမ်းနှင့် ထီကစားခြင်း ခေတ္တရပ်နားထားပါသည်",
                        reopenAtMs = systemGameConfig.gamesReopenAtMs,
                        nowMs = nowMs
                    )
                }
            }

            if (availableApkUpdate != null) {
                item(span = { GridItemSpan(2) }) {
                    ApkUpdateBanner(
                        update = availableApkUpdate,
                        onOpenDialog = onOpenUpdateDialog
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                HideBarSectionHeader(
                    title = if (isEnglish) "Wallet Summary" else "လက်ကျန်ငွေ နှင့် ပွိုင့်အကျဉ်းချုပ်",
                    countBadge = formatKs(account?.balanceKs ?: 0L),
                    isVisible = showWalletCard,
                    onToggle = { showWalletCard = !showWalletCard },
                    testTag = "hide_bar_customer_wallet"
                )
            }

            // Summary Wallet Card
            if (showWalletCard) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                        border = BorderStroke(1.dp, PearlCardBorder),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEnglish) "Balance" else "လက်ကျန်ငွေ",
                                    color = InkMuted,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = formatKs(account?.balanceKs ?: 0L),
                                    color = InkDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isEnglish) "Reward Points" else "လက်ဆောင် ပွိုင့်များ",
                                        color = InkMuted,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = "Points History",
                                        tint = PitchEmerald,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { onNavigate(CustomerRoute.POINT_EXCHANGE) }
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${account?.rewardPoints ?: 0L}",
                                    color = InkDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = PearlCardBorder)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isEnglish) "Active Stake" else "လောင်းထားသောငွေ",
                                    color = InkMuted,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = formatKs(account?.activeStakeKs ?: 0L),
                                    color = InkDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                color = PitchEmeraldDark,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clickable { onNavigate(CustomerRoute.LEDGER) }
                                    .testTag("quick_topup_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AddCircle,
                                        contentDescription = "Top up",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isEnglish) "Deposit / Withdraw" else "ငွေသွင်း / ထုတ်",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            item(span = { GridItemSpan(2) }) {
                HideBarSectionHeader(
                    title = if (isEnglish) "Games & Services Grid" else "ပွဲစဉ်များ နှင့် ဝန်ဆောင်မှု ဇယား",
                    countBadge = "${menuTiles.size + 2}",
                    isVisible = showGamesGrid,
                    onToggle = { showGamesGrid = !showGamesGrid },
                    testTag = "hide_bar_customer_menu_grid"
                )
            }

            // White brand cards for 1xBET & 555 Mix positioned underneath the section bar
            if (showGamesGrid) {
                item(span = { GridItemSpan(2) }) {
                    val expressEnabled = !isLocked && !isMaintenance && !footballPausedNow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(86.dp)
                                .alpha(if (expressEnabled) 1f else 0.45f)
                                .clickable(enabled = expressEnabled) { onNavigate(CustomerRoute.ONE_EXPRESS) }
                                .testTag("menu_one_express"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, WhiteCardBorder),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = OneXBetNavy, fontWeight = FontWeight.Bold)) {
                                            append("1x")
                                        }
                                        withStyle(SpanStyle(color = OneXBetBlue, fontWeight = FontWeight.Bold)) {
                                            append("BET")
                                        }
                                    },
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 28.sp,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(86.dp)
                                .alpha(if (expressEnabled) 1f else 0.45f)
                                .clickable(enabled = expressEnabled) { onNavigate(CustomerRoute.FIVE_FIVE_FIVE) }
                                .testTag("menu_five_five_five"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, WhiteCardBorder),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = MixCharcoal, fontWeight = FontWeight.Bold)) {
                                            append("555 ")
                                        }
                                        withStyle(SpanStyle(color = MixAmber, fontWeight = FontWeight.Bold)) {
                                            append("Mix")
                                        }
                                    },
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 26.sp,
                                    letterSpacing = 0.4.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Menu Tiles (Sports, 2D, 3D, Mini Games, History, Wallet, Results, Settings)
            if (showGamesGrid) {
                items(menuTiles, key = { it.route.name }) { tile ->
                    val isGameTile = tile.gameKey != null
                    val isFootballTile = tile.gameKey == "parlay" || tile.gameKey == "body_goal"
                    val pausedByTimer = if (isFootballTile) footballPausedNow else if (isGameTile) gamesPausedNow else false
                    val gameEnabled = tile.gameKey == null || (systemGameConfig.isGameEnabled(tile.gameKey) && !pausedByTimer)
                    val tileEnabled = (!isGameTile || (!isLocked && !isMaintenance && gameEnabled))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(114.dp)
                            .alpha(if (tileEnabled) 1f else 0.45f)
                            .clickable(enabled = tileEnabled) { onNavigate(tile.route) }
                            .testTag(tile.testTag),
                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                        border = BorderStroke(1.dp, PearlCardBorder),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(tile.badgeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tile.icon,
                                    contentDescription = tile.title,
                                    tint = tile.iconTint,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = when {
                                    isGameTile && pausedByTimer -> "${tile.title} (ခေတ္တပိတ်)"
                                    isGameTile && !gameEnabled -> "${tile.title} (ပိတ်)"
                                    else -> tile.title
                                },
                                color = InkDark,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Support, Quality & Settings row
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val supportTiles = listOf(
                        MenuTile(CustomerRoute.CHAT, "Chatbox", Icons.Default.Forum, Color(0xFF0284C7), Color(0xFFE0F2FE), "menu_chat"),
                        MenuTile(CustomerRoute.QUALITY, "အရည်အသွေး", Icons.Default.StarRate, Color(0xFFD97706), Color(0xFFFEF3C7), "menu_quality"),
                        MenuTile(CustomerRoute.SETTINGS, if (isEnglish) "Settings" else "ဆက်တင် / စည်းကမ်း", Icons.Default.Settings, Color(0xFF047857), Color(0xFFD1FAE5), "menu_settings")
                    )
                    supportTiles.forEach { tile ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(106.dp)
                                .clickable { onNavigate(tile.route) }
                                .testTag(tile.testTag),
                            colors = CardDefaults.cardColors(containerColor = PearlCard),
                            border = BorderStroke(1.dp, PearlCardBorder),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(tile.badgeColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title,
                                        tint = tile.iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = tile.title,
                                    color = InkDark,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin တိုက်ရိုက် ခေါ်ဆိုရန်",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Voice Call / Video Call",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = onStartVoiceCall,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("home_voice_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Voice", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onStartVideoCall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("home_video_call_button")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Video", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                HideBarSectionHeader(
                    title = "APK တိုက်ရိုက်ဒေါင်းရန် & မျှဝေရန်",
                    isVisible = showApkCard,
                    onToggle = { showApkCard = !showApkCard },
                    testTag = "hide_bar_customer_apk_card"
                )
            }

            if (showApkCard) {
                item(span = { GridItemSpan(2) }) {
                    val resolvedApkUrl = remember(availableApkUpdate, apkDownloadUrl) {
                        ApkDownloadHelper.toDirectDownloadUrl(
                            availableApkUpdate?.downloadUrl?.ifBlank { apkDownloadUrl } ?: apkDownloadUrl
                        ).ifBlank { AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL }
                    }
                    val appDisplayName = brandingConfig.appName.ifBlank { "လိုတရ" }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                        border = BorderStroke(1.dp, PitchEmerald.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_direct_apk_card")
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "APK တိုက်ရိုက်ဒေါင်းရန် & မျှဝေရန်",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Link နှိပ်၍ တိုက်ရိုက်ဒေါင်းနိုင်သလို ဖုန်းထဲမှ APK ဖိုင်ကိုပါ တိုက်ရိုက် ရှယ်ပို့နိုင်ပါသည်။",
                            color = Color(0xFFB4C8BE),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (directDownloading) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (directDownloadProgress > 0) {
                                    LinearProgressIndicator(
                                        progress = { directDownloadProgress / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "APK ဒေါင်းလုဒ်ဆွဲနေသည်... $directDownloadProgress%",
                                        color = Color(0xFF34D399),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                        if (directDownloadError != null) {
                            Text(
                                text = directDownloadError!!,
                                color = Color(0xFFFCA5A5),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = !directDownloading,
                                onClick = {
                                    if (availableApkUpdate != null) {
                                        onOpenUpdateDialog()
                                    } else {
                                        directDownloading = true
                                        directDownloadProgress = 0
                                        directDownloadError = null
                                        scope.launch {
                                            ApkDownloadHelper.downloadApkToCache(
                                                context = context,
                                                rawUrl = resolvedApkUrl,
                                                onProgress = { pct -> directDownloadProgress = pct }
                                            ).onSuccess { file ->
                                                directDownloading = false
                                                ApkDownloadHelper.installApkFile(context, file)
                                                    .onFailure { err ->
                                                        directDownloadError = err.localizedMessage
                                                    }
                                            }.onFailure { err ->
                                                directDownloading = false
                                                directDownloadError = err.localizedMessage ?: "APK တိုက်ရိုက်ဒေါင်း၍မရပါ"
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).testTag("home_direct_apk_download_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("APK ဒေါင်းမည်", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    val shareText = ApkDownloadHelper.buildApkShareText(
                                        appName = appDisplayName,
                                        rawUrl = resolvedApkUrl,
                                        versionLabel = availableApkUpdate?.versionLabel.orEmpty()
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "$appDisplayName APK Direct Download")
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Direct APK Link မျှဝေမည်"))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).testTag("home_share_direct_link_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Link ရှယ်မည်", color = Color(0xFF34D399), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val shareText = ApkDownloadHelper.buildApkShareText(
                                    appName = appDisplayName,
                                    rawUrl = resolvedApkUrl,
                                    versionLabel = availableApkUpdate?.versionLabel.orEmpty()
                                )
                                clipboard.setText(AnnotatedString(shareText))
                                Toast.makeText(context, "Customer Share Link ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("home_copy_share_link_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFD7EAE1), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Link ကူးယူမည် (${ApkDownloadHelper.CUSTOMER_SHARE_DEEP_LINK})", color = Color(0xFFD7EAE1), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    ApkDownloadHelper.shareInstalledApkFile(context, appDisplayName)
                                        .onFailure { err ->
                                            Toast.makeText(
                                                context,
                                                err.localizedMessage ?: "APK ဖိုင်တိုက်ရိုက်ရှယ်၍မရပါ",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("home_share_apk_file_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("APK ဖိုင်တိုက်ရိုက်ပို့မည် (Install Ready .apk)", color = ChampionshipGold, maxLines = 1)
                        }
                        if (isAdmin) {
                            HorizontalDivider(color = Color(0xFF1F473A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val adminLink = ApkDownloadHelper.buildAdminPortalLink()
                                        clipboard.setText(AnnotatedString(adminLink))
                                        Toast.makeText(context, "Admin Link ကူးယူပြီး: $adminLink", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f).testTag("home_copy_admin_link_button")
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Admin Link ကူး", color = ChampionshipGold, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val adminLink = ApkDownloadHelper.buildAdminPortalLink()
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "$appDisplayName Admin Portal Link")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "$appDisplayName Admin Link: $adminLink\nApp Download: $resolvedApkUrl"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Admin Link မျှဝေမည်"))
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f).testTag("home_share_admin_link_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Admin Link ရှယ်", color = Color(0xFF34D399), maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showInviteDialog) {
        AdminInviteRedeemDialog(
            initialCode = prefillInviteCode,
            onDismiss = {
                showInviteDialog = false
                prefillInviteCode = ""
                onConsumePendingInviteCode()
            },
            onRedeem = { code ->
                showInviteDialog = false
                prefillInviteCode = ""
                onConsumePendingInviteCode()
                onRedeemInviteCode(code)
            }
        )
    }
}
