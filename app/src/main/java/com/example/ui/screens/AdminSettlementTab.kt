package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.betting.SettlementCalculator
import com.example.data.betting.SettlementRange
import com.example.data.model.BetOrder
import com.example.data.model.UserAccount
import com.example.ui.components.OrderSlipCard
import com.example.ui.components.formatKs
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.AdminCrimsonSoft
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.DeepPitchSurface
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSettlementTab(
    orders: List<BetOrder>,
    usersById: Map<String, UserAccount>,
    busy: Boolean,
    onAutoSettleAll: () -> Unit,
    onAutoSettleCustomer: (userId: String, customerName: String) -> Unit,
    onInspectCustomer: (String) -> Unit,
    onAcceptOrder: (BetOrder) -> Unit,
    onRequestSettleWin: (BetOrder) -> Unit,
    onSettleLoss: (BetOrder) -> Unit,
    onRejectOrder: (BetOrder) -> Unit,
    onDeleteOrder: (String) -> Unit
) {
    var range by remember { mutableStateOf(SettlementRange.TODAY) }
    var searchQuery by remember { mutableStateOf("") }
    val expandedCustomers = remember { mutableStateMapOf<String, Boolean>() }

    val report = remember(orders, usersById, range) {
        SettlementCalculator.buildReport(orders, usersById, range)
    }

    val filteredCustomers = remember(report.customerSummaries, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) report.customerSummaries
        else report.customerSummaries.filter {
            it.customerName.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SettlementRange.entries.forEach { option ->
                    FilterChip(
                        selected = range == option,
                        onClick = { range = option },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag("settlement_range_${option.name.lowercase()}")
                    )
                }
            }
        }

        // Admin Daily / Range Win-Loss Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_settlement_hero_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Admin ${range.label} ပွဲစဉ် စာရင်းချုပ် (အနိုင်/အရှုံး)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val netColor = when {
                        report.adminNetKs > 0L -> Color(0xFF4ADE80)
                        report.adminNetKs < 0L -> Color(0xFFF87171)
                        else -> ChampionshipGold
                    }
                    val netText = when {
                        report.adminNetKs > 0L -> "+${formatKs(report.adminNetKs)} (Admin နိုင်)"
                        report.adminNetKs < 0L -> "-${formatKs(abs(report.adminNetKs))} (Admin ရှုံး)"
                        else -> "0 Ks (အရှုံးအမြတ် မရှိ)"
                    }

                    Surface(
                        color = DeepPitchSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Admin အသားတင် ရလဒ် (${range.label})",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = netText,
                                color = netColor,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("စာရင်းပိတ် ထိုးကြေး", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatKs(report.totalSettledStakeKs),
                                color = Color.White,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("ပြန်လျော်ငွေ စုစုပေါင်း", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatKs(report.totalPayoutKs),
                                color = Color(0xFFFCA5A5),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Customer ရှုံး (Admin ရ)", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatKs(report.totalLostKeptKs),
                                color = Color(0xFF4ADE80),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("အနိုင်အကောက် ရငွေ", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatKs(report.totalCommissionKs),
                                color = ChampionshipGold,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Text(
                        text = "နိုင် ${report.wonCount} ခု • ရှုံး ${report.lostCount} ခု • သရေ/ပယ်ဖျက် ${report.pushRejectedCount} ခု • မရှင်းရသေး ${report.pendingCount} ခု (${formatKs(report.totalPendingStakeKs)})",
                        color = Color(0xFFD5E3DC),
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (report.pendingCount > 0) {
                        Button(
                            onClick = onAutoSettleAll,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChampionshipGold,
                                contentColor = InkDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settlement_auto_settle_all_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ပွဲပြီးအော်ဒါအားလုံး Auto ရှင်းလင်းမည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampionshipGold) },
                label = { Text("Customer အမည် / Gmail ဖြင့် ရှာရန်") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ChampionshipGold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settlement_customer_search")
            )
        }

        item {
            Text(
                text = "Customer တစ်ဦးချင်းစီ အနိုင်/အရှုံး နှင့် လောင်းကြေး (${filteredCustomers.size})",
                color = ChampionshipGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredCustomers.isEmpty()) {
            item {
                Text(
                    text = "${range.label} အတွက် လောင်းကြေးမှတ်တမ်း မရှိသေးပါ",
                    color = Color(0xFFB4C8BE),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        } else {
            items(filteredCustomers, key = { it.userId }) { cs ->
                val isExpanded = expandedCustomers[cs.userId] == true
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settlement_customer_card_${cs.userId}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PitchEmeraldDark)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = cs.customerName,
                                        color = InkDark,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = PitchEmeraldDark, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = cs.email.ifBlank { "Gmail မရှိသေးပါ" },
                                            color = InkMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "လက်ကျန်: ${formatKs(cs.balanceKs)} • ${cs.rewardPoints} Pts",
                                        color = PitchEmeraldDark,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val (badgeText, badgeBg, badgeFg) = when {
                                cs.customerNetKs > 0L -> Triple("+${formatKs(cs.customerNetKs)} နိုင်", EmeraldSoft, PitchEmeraldDark)
                                cs.customerNetKs < 0L -> Triple("-${formatKs(abs(cs.customerNetKs))} ရှုံး", AdminCrimsonSoft, AdminCrimson)
                                cs.wonCount + cs.lostCount > 0 -> Triple("0 Ks သရေ", Color(0xFFE5E7EB), InkDark)
                                else -> Triple("မရှင်းရသေး (${cs.pendingCount})", Color(0xFFFEF3C7), Color(0xFFB45309))
                            }
                            Surface(color = badgeBg, shape = CircleShape) {
                                Text(
                                    text = badgeText,
                                    color = badgeFg,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("နိုင် (${cs.wonCount} ခု)", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatKs(cs.totalWonPayoutKs),
                                    color = PitchEmeraldDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ရှုံး (${cs.lostCount} ခု)", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatKs(cs.totalLostStakeKs),
                                    color = AdminCrimson,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("အကောက်", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatKs(cs.totalCommissionKs),
                                    color = InkDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val adminOutcomeText = when {
                            cs.adminNetKs > 0L -> "Admin ရလဒ်: +${formatKs(cs.adminNetKs)} နိုင်"
                            cs.adminNetKs < 0L -> "Admin ရလဒ်: -${formatKs(abs(cs.adminNetKs))} ရှုံး"
                            else -> "Admin ရလဒ်: 0 Ks"
                        }
                        Text(
                            text = "ထိုးကြေးစုစုပေါင်း: ${formatKs(cs.settledStakeKs + cs.pendingStakeKs)} • $adminOutcomeText",
                            color = InkMuted,
                            style = MaterialTheme.typography.bodySmall
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { expandedCustomers[cs.userId] = !isExpanded }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = InkDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("လောင်းကြေး (${cs.orders.size})", color = InkDark)
                            }
                            OutlinedButton(
                                onClick = { onInspectCustomer(cs.userId) }
                            ) {
                                Text("အကောင့်အသေးစိတ်", color = PitchEmeraldDark)
                            }
                            if (cs.pendingCount > 0) {
                                Button(
                                    onClick = { onAutoSettleCustomer(cs.userId, cs.customerName) },
                                    enabled = !busy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PitchEmeraldDark,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("auto_settle_customer_${cs.userId}")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Auto ရှင်း (${cs.pendingCount})")
                                }
                            }
                        }

                        if (isExpanded) {
                            Spacer(Modifier.height(4.dp))
                            cs.orders.forEach { order ->
                                OrderSlipCard(
                                    order = order,
                                    isAdminView = true,
                                    onAccept = onAcceptOrder,
                                    onSettleWin = onRequestSettleWin,
                                    onSettleLoss = onSettleLoss,
                                    onReject = onRejectOrder,
                                    onDeleteOrder = { onDeleteOrder(it.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
