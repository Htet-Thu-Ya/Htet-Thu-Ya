package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.BetOrder
import com.example.data.model.BrandingConfig
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.PadaukFontFamily
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.AdminCrimsonSoft
import com.example.ui.theme.AmberPending
import com.example.ui.theme.AmberPendingSoft
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldDark
import com.example.ui.theme.SkyInfo
import com.example.ui.theme.SkyInfoSoft
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatKs(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "${formatter.format(amount)} Ks"
}

fun formatTimestamp(ts: Timestamp?): String {
    if (ts == null) return "Just now"
    val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return fmt.format(ts.toDate())
}

fun formatEpochDateTime(epochMs: Long): String {
    if (epochMs <= 0L) return "-"
    val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return fmt.format(Date(epochMs))
}

fun formatKickoffCountdown(matchKickoffMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (matchKickoffMs <= 0L) return ""
    val diffMs = matchKickoffMs - nowMs
    if (diffMs <= 0L) return "ပွဲစတင်နေပြီ / ပြီးစီး"
    val totalMinutes = diffMs / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val mins = totalMinutes % 60L
    return when {
        days > 0L -> "ပွဲစဖို့ $days ရက် $hours နာရီ လို"
        hours > 0L -> "ပွဲစဖို့ $hours နာရီ $mins မိနစ် လို"
        else -> "ပွဲစဖို့ ${mins.coerceAtLeast(1L)} မိနစ် လို"
    }
}

fun orderTypeBurmese(type: String): String = when (type) {
    "parlay" -> "မောင်း"
    "body_goal" -> "ဘော်ဒီ/ဂိုးပေါင်း"
    "one_express" -> "One Express ကြေး"
    "five_five_five" -> "555 ကြေး"
    "three_d" -> "သုံးလုံးထီ"
    "two_d" -> "နှစ်လုံးထီ (2D)"
    "dice" -> "အံစာတုံး"
    "six_animals" -> "၆ကောင်ဂျင်"
    else -> type
}

@Composable
fun BrandLogoAvatar(
    brandingConfig: BrandingConfig = BrandingConfig(),
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val shape = when (brandingConfig.logoVariant) {
        "gold_badge" -> CircleShape
        "emerald_shield" -> RoundedCornerShape(size * 0.36f)
        else -> RoundedCornerShape(size * 0.24f)
    }
    val borderColor = when (brandingConfig.logoVariant) {
        "emerald_shield" -> PitchEmerald
        "gold_badge" -> ChampionshipGold
        else -> ChampionshipGold
    }
    val borderWidth = if (brandingConfig.logoVariant == "gold_badge") 2.5.dp else 1.5.dp

    if (brandingConfig.logoVariant == "custom_url" && brandingConfig.customLogoUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = brandingConfig.customLogoUrl,
            contentDescription = "${brandingConfig.appName} Logo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(borderWidth, borderColor, shape),
            loading = {
                LoTaYaBrandLogo(size = size, modifier = modifier)
            },
            error = {
                LoTaYaBrandLogo(size = size, modifier = modifier)
            }
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.lotaya_logo),
            contentDescription = "${brandingConfig.appName} Logo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .border(borderWidth, borderColor, shape)
        )
    }
}

@Composable
fun LoTaYaBrandLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.lotaya_logo),
        contentDescription = "လိုတရ Logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .border(1.5.dp, ChampionshipGold, RoundedCornerShape(size * 0.24f))
    )
}

@Composable
fun HtetThuYaBrandLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    LoTaYaBrandLogo(size = size, modifier = modifier)
}

@Composable
fun TeamLogoAvatar(
    logoUrl: String,
    teamName: String,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(teamName) {
        val parts = teamName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> "FC"
        }
    }
    val fallbackBg = remember(teamName) {
        val palette = listOf(
            Color(0xFF1E3A8A),
            Color(0xFF065F46),
            Color(0xFF991B1B),
            Color(0xFF92400E),
            Color(0xFF4C1D95),
            Color(0xFF0F766E)
        )
        val idx = kotlin.math.abs(teamName.hashCode()) % palette.size
        palette[idx]
    }

    if (logoUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = logoUrl,
            contentDescription = "$teamName Logo",
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f)),
            loading = {
                TeamMonogramBadge(initials = initials, bg = fallbackBg, size = size)
            },
            error = {
                TeamMonogramBadge(initials = initials, bg = fallbackBg, size = size)
            }
        )
    } else {
        TeamMonogramBadge(initials = initials, bg = fallbackBg, size = size, modifier = modifier)
    }
}

@Composable
private fun TeamMonogramBadge(
    initials: String,
    bg: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, ChampionshipGold.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun OnlineStatusDot(
    isOnline: Boolean,
    lastSeenTs: Timestamp? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF22C55E) else Color(0xFF9CA3AF))
                .border(1.dp, Color.White, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (isOnline) "Online" else if (lastSeenTs != null) formatTimestamp(lastSeenTs) else "Offline",
            color = if (isOnline) PitchEmeraldDark else InkMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isOnline) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    containerColor: Color = DeepPitchBg,
    brandingConfig: BrandingConfig = BrandingConfig()
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogoAvatar(brandingConfig = brandingConfig, size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = Color.White,
            navigationIconContentColor = ChampionshipGold
        )
    )
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, fg) = when (status) {
        "pending" -> Triple("စောင့်ဆိုင်းဆဲ", AmberPendingSoft, AmberPending)
        "awaiting_main_approval" -> Triple("Main Admin စောင့်ဆိုင်း", AmberPendingSoft, AmberPending)
        "accepted" -> Triple("အတည်ပြုပြီး", SkyInfoSoft, SkyInfo)
        "approved" -> Triple("အတည်ပြုပြီး", EmeraldSoft, PitchEmeraldDark)
        "won" -> Triple("အနိုင်ရ", EmeraldSoft, PitchEmeraldDark)
        "lost" -> Triple("ရှုံး", Color(0xFFE5E7EB), InkMuted)
        "rejected" -> Triple("ပယ်ဖျက်", AdminCrimsonSoft, AdminCrimson)
        "suspended" -> Triple("ယာယီပိတ်", AdminCrimsonSoft, AdminCrimson)
        "active" -> Triple("အသုံးပြုနေ", EmeraldSoft, PitchEmeraldDark)
        "open" -> Triple("ဖွင့်ထားသည်", EmeraldSoft, PitchEmeraldDark)
        "closed" -> Triple("ပိတ်ထားသည်", AmberPendingSoft, AmberPending)
        "finished" -> Triple("ပြီးဆုံး", SkyInfoSoft, SkyInfo)
        else -> Triple(status, Color(0xFFE5E7EB), InkDark)
    }
    Surface(
        color = bg,
        shape = CircleShape
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderSlipCard(
    order: BetOrder,
    isAdminView: Boolean = false,
    busy: Boolean = false,
    canDeleteAnyOrder: Boolean = false,
    onAccept: ((BetOrder) -> Unit)? = null,
    onAutoSettleOrder: ((BetOrder) -> Unit)? = null,
    onSettleWin: ((BetOrder) -> Unit)? = null,
    onSettleLoss: ((BetOrder) -> Unit)? = null,
    onReject: ((BetOrder) -> Unit)? = null,
    onDeleteOrder: ((BetOrder) -> Unit)? = null,
    onToggleHideOrder: ((BetOrder) -> Unit)? = null,
    onEditOrder: ((BetOrder) -> Unit)? = null
) {
    val isCompleted = order.status in listOf("won", "lost", "rejected")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = if (order.isHidden && isAdminView) Color(0xFFF3F4F6) else PearlCard),
        border = BorderStroke(1.dp, if (order.isHidden && isAdminView) AmberPending else PearlCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = DeepPitchSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = orderTypeBurmese(order.orderType),
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (isAdminView) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = order.customerName,
                            color = InkDark,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (isAdminView && order.isHidden) {
                        Surface(color = AmberPendingSoft, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "HIDDEN",
                                color = AmberPending,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    StatusBadge(order.status)
                    if (isAdminView && onToggleHideOrder != null) {
                        IconButton(
                            onClick = { onToggleHideOrder(order) },
                            modifier = Modifier.testTag("toggle_hide_order_${order.id}")
                        ) {
                            Icon(
                                imageVector = if (order.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (order.isHidden) "Show Order" else "Hide Order",
                                tint = InkMuted
                            )
                        }
                    }
                    if (isAdminView && onEditOrder != null) {
                        IconButton(
                            onClick = { onEditOrder(order) },
                            modifier = Modifier.testTag("edit_order_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Order",
                                tint = PitchEmeraldDark
                            )
                        }
                    }
                    if ((isCompleted || canDeleteAnyOrder) && onDeleteOrder != null) {
                        IconButton(
                            onClick = { onDeleteOrder(order) },
                            modifier = Modifier.testTag("delete_order_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Order",
                                tint = AdminCrimson
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (order.drawRound.isNotBlank()) {
                Text(
                    text = order.drawRound,
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
            }

            order.selections.forEach { sel ->
                Text(
                    text = "• $sel",
                    color = InkDark,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ထိုးကြေး", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatKs(order.stakeKs),
                        color = InkDark,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("နှုန်း", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "x${String.format(Locale.US, "%.2f", order.multiplier)}",
                        color = PitchEmeraldDark,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (order.status == "won") "ရရှိငွေ" else "ရနိုင်ချေ",
                        color = InkMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        formatKs(if (order.status == "won") order.actualPayoutKs else order.potentialPayoutKs),
                        color = if (order.status == "won") PitchEmeraldDark else InkDark,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (order.status == "won" && order.commissionKs > 0L) {
                Spacer(Modifier.height(6.dp))
                val pctStr = if (order.commissionPercent % 1.0 == 0.0) {
                    order.commissionPercent.toInt().toString()
                } else {
                    order.commissionPercent.toString()
                }
                Text(
                    text = "အနိုင်အကောက် (-$pctStr%): -${formatKs(order.commissionKs)}",
                    color = AdminCrimson,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (order.adminNote.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "မှတ်ချက်: ${order.adminNote}",
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = InkMuted, modifier = Modifier.size(14.dp))
                Text(
                    text = "လောင်းချိန်: ${formatTimestamp(order.createdAt)}",
                    color = InkMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (order.matchKickoffMs > 0L || order.matchKickoffText.isNotBlank() || order.scanAtMs > 0L) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = SkyInfoSoft,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (order.matchKickoffText.isNotBlank()) {
                            Text(
                                text = "ပွဲကန်မည့်အချိန်: ${order.matchKickoffText}",
                                color = SkyInfo,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (order.matchKickoffMs > 0L) {
                            val countdown = formatKickoffCountdown(order.matchKickoffMs)
                            Text(
                                text = "ပွဲစချိန်: ${formatEpochDateTime(order.matchKickoffMs)} • $countdown",
                                color = SkyInfo,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (order.scanAtMs > 0L) {
                            Text(
                                text = "စကန်မည့်အချိန်: ${formatEpochDateTime(order.scanAtMs)}",
                                color = PitchEmeraldDark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (isAdminView && (order.status == "pending" || order.status == "accepted")) {
                Spacer(Modifier.height(10.dp))
                if (onAutoSettleOrder != null) {
                    Button(
                        onClick = { onAutoSettleOrder(order) },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_settle_order_${order.id}")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ဤပွဲ/အော်ဒါ Auto ရှင်းမည်", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (order.status == "pending" && onAccept != null) {
                        Button(
                            onClick = { onAccept(order) },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyInfo),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("accept_order_${order.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("လက်ခံ", maxLines = 1)
                        }
                    }
                    if (onSettleWin != null) {
                        Button(
                            onClick = { onSettleWin(order) },
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("win_order_${order.id}")
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("အနိုင်", maxLines = 1)
                        }
                    }
                    if (onSettleLoss != null) {
                        OutlinedButton(
                            onClick = { onSettleLoss(order) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lose_order_${order.id}")
                        ) {
                            Text("ရှုံး", color = InkDark, maxLines = 1)
                        }
                    }
                    if (onReject != null) {
                        IconButton(
                            onClick = { onReject(order) },
                            modifier = Modifier.testTag("reject_order_${order.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Reject & Refund", tint = AdminCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminInviteRedeemDialog(
    onDismiss: () -> Unit,
    onRedeem: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null, tint = ChampionshipGold) },
        title = { Text("Second Admin ဝင်ရောက်ရန်") },
        text = {
            Column {
                Text(
                    "Main Admin ထံမှ ရရှိထားသော Invite Code (ဥပမာ- HTY-XXXXXX) ကို ထည့်သွင်းပြီး Admin စီမံခန့်ခွဲမှု ဖွင့်လှစ်ပါ။",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Admin Invite Code") },
                    placeholder = { Text("HTY-XXXXXX") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (code.isNotBlank()) onRedeem(code) },
                enabled = code.trim().length >= 4,
                modifier = Modifier.testTag("redeem_invite_button")
            ) {
                Text("အတည်ပြုဝင်မည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်မည်")
            }
        }
    )
}
