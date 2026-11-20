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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.ui.components.AdminInviteRedeemDialog
import com.example.ui.components.HtetThuYaBrandLogo
import com.example.ui.components.formatKs
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
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldDark

enum class CustomerRoute {
    PARLAY,
    BODY_GOAL,
    ACTIVE_ORDERS,
    PAST_ORDERS,
    LEDGER,
    RESULTS,
    THREE_D,
    LEADERBOARD,
    POINT_EXCHANGE
}

private data class MenuTile(
    val route: CustomerRoute,
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeColor: Color,
    val testTag: String
)

@Composable
fun CustomerHomeScreen(
    accountState: UiState<UserAccount?>,
    displayName: String,
    isAdmin: Boolean,
    pendingAdminCount: Int,
    onNavigate: (CustomerRoute) -> Unit,
    onSwitchToAdmin: () -> Unit,
    onRedeemInviteCode: (String) -> Unit,
    onRefresh: () -> Unit = {},
    onSignOut: () -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }

    val menuTiles = remember {
        listOf(
            MenuTile(CustomerRoute.PARLAY, "မောင်း", Icons.Default.Stadium, Color(0xFF16A34A), Color(0xFFDCFCE7), "menu_parlay"),
            MenuTile(CustomerRoute.BODY_GOAL, "ဘော်ဒီ/ဂိုးပေါင်း", Icons.Default.SportsSoccer, Color(0xFF1E293B), Color(0xFFE2E8F0), "menu_body_goal"),
            MenuTile(CustomerRoute.ACTIVE_ORDERS, "လောင်းထားသောပွဲစဉ်များ", Icons.Default.History, Color(0xFF334155), Color(0xFFE2E8F0), "menu_active_orders"),
            MenuTile(CustomerRoute.PAST_ORDERS, "ပွဲစဉ်ဟောင်းများ", Icons.Default.CalendarMonth, Color(0xFFEA580C), Color(0xFFFFEDD5), "menu_past_orders"),
            MenuTile(CustomerRoute.LEDGER, "ငွေစာရင်း", Icons.Default.MonetizationOn, Color(0xFFD97706), Color(0xFFFEF3C7), "menu_ledger"),
            MenuTile(CustomerRoute.RESULTS, "ပွဲပြီး ရလဒ်များ", Icons.Default.LiveTv, Color(0xFF0284C7), Color(0xFFE0F2FE), "menu_results"),
            MenuTile(CustomerRoute.THREE_D, "သုံးလုံးထီ", Icons.Default.Casino, Color(0xFF059669), Color(0xFFD1FAE5), "menu_three_d"),
            MenuTile(CustomerRoute.LEADERBOARD, "အဆင့်ဇယား", Icons.Default.EmojiEvents, Color(0xFFEA580C), Color(0xFFFFEDD5), "menu_leaderboard"),
            MenuTile(CustomerRoute.POINT_EXCHANGE, "ပွိုင့်လဲလှယ်", Icons.Default.CurrencyExchange, Color(0xFF16A34A), Color(0xFFDCFCE7), "menu_point_exchange")
        )
    }

    val account = (accountState as? UiState.Success)?.data

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
                        HtetThuYaBrandLogo(size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Htet Thu Ya",
                                style = MaterialTheme.typography.titleLarge,
                                color = ChampionshipGold,
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
                                    .clickable { showInviteDialog = true }
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

            // 555-style Summary Wallet Card
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
                                    text = "လက်ကျန်ငွေ",
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
                                        text = "လက်ဆောင် ပွိုင့်များ",
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
                                    text = "လောင်းထားသောငွေ",
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
                                        text = "ငွေသွင်း / ထုတ်",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 9-Tile 555 Grid Menu
            items(menuTiles, key = { it.route.name }) { tile ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(114.dp)
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
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = tile.title,
                            color = InkDark,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
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
            onDismiss = { showInviteDialog = false },
            onRedeem = { code ->
                showInviteDialog = false
                onRedeemInviteCode(code)
            }
        )
    }
}
