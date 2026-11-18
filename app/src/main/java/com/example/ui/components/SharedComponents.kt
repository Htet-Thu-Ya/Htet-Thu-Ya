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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
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
import com.example.R
import com.example.data.model.BetOrder
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

fun orderTypeBurmese(type: String): String = when (type) {
    "parlay" -> "မောင်း"
    "body_goal" -> "ဘော်ဒီ/ဂိုးပေါင်း"
    "three_d" -> "သုံးလုံးထီ"
    else -> type
}

@Composable
fun HtetThuYaBrandLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.htet_thu_ya_logo),
        contentDescription = "Htet Thu Ya Logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .border(1.5.dp, ChampionshipGold, RoundedCornerShape(size * 0.24f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    containerColor: Color = DeepPitchBg
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HtetThuYaBrandLogo(size = 30.dp)
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
        "accepted" -> Triple("အတည်ပြုပြီး", SkyInfoSoft, SkyInfo)
        "approved" -> Triple("အတည်ပြုပြီး", EmeraldSoft, PitchEmeraldDark)
        "won" -> Triple("အနိုင်ရ", EmeraldSoft, PitchEmeraldDark)
        "lost" -> Triple("ရှုံး", Color(0xFFE5E7EB), InkMuted)
        "rejected" -> Triple("ပယ်ဖျက်", AdminCrimsonSoft, AdminCrimson)
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
    onAccept: ((BetOrder) -> Unit)? = null,
    onSettleWin: ((BetOrder) -> Unit)? = null,
    onSettleLoss: ((BetOrder) -> Unit)? = null,
    onReject: ((BetOrder) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = PearlCard),
        border = BorderStroke(1.dp, PearlCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                StatusBadge(order.status)
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

            if (order.adminNote.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "မှတ်ချက်: ${order.adminNote}",
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = formatTimestamp(order.createdAt),
                color = InkMuted,
                style = MaterialTheme.typography.labelMedium
            )

            if (isAdminView && (order.status == "pending" || order.status == "accepted")) {
                Spacer(Modifier.height(10.dp))
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
        title = { Text("သူငယ်ချင်း Admin ဝင်ရောက်ရန်") },
        text = {
            Column {
                Text(
                    "Admin ထံမှ ရရှိထားသော Invite Code (ဥပမာ- HTY-XXXXXX) ကို ထည့်သွင်းပြီး Admin စီမံခန့်ခွဲမှု ဖွင့်လှစ်ပါ။",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Invite Code") },
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
