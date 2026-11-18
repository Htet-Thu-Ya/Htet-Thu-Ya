package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BetOrder
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LedgerTransaction
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WalletRequest
import com.example.ui.components.OrderSlipCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.formatKs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchBg
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PearlCardBorder
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldDark

@Composable
fun OrdersHistoryScreen(
    activeOnly: Boolean,
    ordersState: UiState<List<BetOrder>>,
    onBack: () -> Unit
) {
    val title = if (activeOnly) "လောင်းထားသောပွဲစဉ်များ" else "ပွဲစဉ်ဟောင်းများ"
    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = title, onBack = onBack) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (ordersState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChampionshipGold)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(ordersState.message, color = Color(0xFFFCA5A5))
                }
                is UiState.Success -> {
                    val filtered = ordersState.data.filter { order ->
                        if (activeOnly) order.status in listOf("pending", "accepted")
                        else order.status in listOf("won", "lost", "rejected")
                    }
                    if (filtered.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (activeOnly) "လက်ရှိ အော်ဒါအသစ် မရှိသေးပါ" else "ပြီးဆုံးပြီးသော အော်ဒါမှတ်တမ်း မရှိသေးပါ",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { order ->
                                OrderSlipCard(order = order, isAdminView = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletLedgerScreen(
    accountState: UiState<UserAccount?>,
    requestsState: UiState<List<WalletRequest>>,
    transactionsState: UiState<List<LedgerTransaction>>,
    busy: Boolean,
    onBack: () -> Unit,
    onSubmitWalletRequest: (requestType: String, amountKs: Long, channel: String, referenceNote: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var reqType by remember { mutableStateOf("topup") }
    var channel by remember { mutableStateOf("KBZPay") }
    var amountInput by remember { mutableStateOf("5000") }
    var refNote by remember { mutableStateOf("") }

    val balanceKs = (accountState as? UiState.Success)?.data?.balanceKs ?: 0L
    val parsedAmt = amountInput.toLongOrNull() ?: 0L
    val canSubmit = !busy && parsedAmt >= 500L && refNote.trim().isNotEmpty() &&
        (reqType == "topup" || parsedAmt <= balanceKs)

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "ငွေစာရင်း (Wallet)", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepPitchElevated,
                contentColor = ChampionshipGold
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("ငွေသွင်း / ထုတ်") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("ငွေစာရင်းမှတ်တမ်း") })
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PearlCard),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PitchEmeraldDark)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "လက်ရှိ လက်ကျန်ငွေ: ${formatKs(balanceKs)}",
                                        color = InkDark,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = reqType == "topup",
                                        onClick = { reqType = "topup" },
                                        label = { Text("ငွေသွင်း (Top-up)") }
                                    )
                                    FilterChip(
                                        selected = reqType == "withdraw",
                                        onClick = { reqType = "withdraw" },
                                        label = { Text("ငွေထုတ် (Cash-out)") }
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("KBZPay", "WavePay", "CB Pay", "Cash").forEach { ch ->
                                        FilterChip(
                                            selected = channel == ch,
                                            onClick = { channel = ch },
                                            label = { Text(ch) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = amountInput,
                                    onValueChange = { amountInput = it.filter { c -> c.isDigit() }.take(8) },
                                    label = { Text("ပမာဏ (အနည်းဆုံး 500 Ks)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = InkDark,
                                        unfocusedTextColor = InkDark
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("wallet_amount_input")
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = refNote,
                                    onValueChange = { refNote = it },
                                    label = {
                                        Text(
                                            if (reqType == "topup") "ငွေလွှဲနံပါတ် နောက်ဆုံး ၆ လုံး / မှတ်ချက်"
                                            else "လက်ခံမည့် ဖုန်းနံပါတ် / အကောင့်အမည်"
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = InkDark,
                                        unfocusedTextColor = InkDark
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("wallet_ref_input")
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onSubmitWalletRequest(reqType, parsedAmt, channel, refNote)
                                        refNote = ""
                                    },
                                    enabled = canSubmit,
                                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_wallet_request_button")
                                ) {
                                    Text(if (reqType == "topup") "ငွေသွင်းတောင်းဆိုမည်" else "ငွေထုတ်တောင်းဆိုမည်")
                                }
                            }
                        }
                    }

                    val requests = (requestsState as? UiState.Success)?.data ?: emptyList()
                    if (requests.isNotEmpty()) {
                        item {
                            Text("တောင်းဆိုမှု အခြေအနေများ", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
                        }
                        items(requests, key = { it.id }) { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PearlCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${if (req.requestType == "topup") "ငွေသွင်း" else "ငွေထုတ်"} • ${req.channel}",
                                            color = InkDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${formatKs(req.amountKs)} • Ref: ${req.referenceNote}",
                                            color = InkMuted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (req.adminNote.isNotBlank()) {
                                            Text("Admin: ${req.adminNote}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    StatusBadge(req.status)
                                }
                            }
                        }
                    }
                }
            } else {
                val txs = (transactionsState as? UiState.Success)?.data ?: emptyList()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (txs.isEmpty()) {
                        item {
                            Text("ငွေစာရင်း မှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE))
                        }
                    } else {
                        items(txs, key = { it.id }) { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PearlCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.title, color = InkDark, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${formatTimestamp(tx.createdAt)} • လက်ကျန် ${formatKs(tx.balanceAfterKs)}",
                                            color = InkMuted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = "${if (tx.direction == "credit") "+" else "-"}${formatKs(tx.amountKs)}",
                                        color = if (tx.direction == "credit") PitchEmeraldDark else AdminCrimson,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
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

@Composable
fun ResultsScreen(
    matchesState: UiState<List<SportMatch>>,
    drawsState: UiState<List<LotteryDraw>>,
    onBack: () -> Unit
) {
    val finishedMatches = (matchesState as? UiState.Success)?.data?.filter { it.status == "finished" } ?: emptyList()
    val finishedDraws = (drawsState as? UiState.Success)?.data?.filter { it.status == "finished" } ?: emptyList()

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "ပွဲပြီး ရလဒ်များ", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "၃လုံးထီ ရလဒ်များ",
                    color = ChampionshipGold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                )
            }
            if (finishedDraws.isEmpty()) {
                item {
                    Text("၃လုံးထီ ရလဒ် မရှိသေးပါ", color = Color(0xFFB4C8BE))
                }
            } else {
                items(finishedDraws, key = { it.id }) { draw ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(draw.roundTitle, color = InkDark, fontWeight = FontWeight.Bold)
                                Text(draw.closeTimeLabel, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            Surface(color = PitchEmeraldDark, shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = draw.winningNumber.ifBlank { "---" },
                                    color = ChampionshipGold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "ဘောလုံး ပွဲပြီး ရလဒ်များ",
                    color = ChampionshipGold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                )
            }
            if (finishedMatches.isEmpty()) {
                item {
                    Text("ပြီးဆုံးပြီးသော ပွဲစဉ် မရှိသေးပါ", color = Color(0xFFB4C8BE))
                }
            } else {
                items(finishedMatches, key = { it.id }) { m ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PearlCard),
                        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(m.league, color = PitchEmeraldDark, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${m.homeTeam} vs ${m.awayTeam}", color = InkDark, fontWeight = FontWeight.Bold)
                                Text(
                                    "${m.homeScore} - ${m.awayScore}",
                                    color = InkDark,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
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
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "အဆင့်ဇယား (Leaderboard)", onBack = onBack) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (leaderboardState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChampionshipGold)
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(leaderboardState.message, color = Color(0xFFFCA5A5))
                }
                is UiState.Success -> {
                    val entries = leaderboardState.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(entries, key = { _, e -> e.userId }) { idx, entry ->
                            val isMe = entry.userId == currentUserId
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) Color(0xFFE6F4EE) else PearlCard
                                ),
                                border = if (isMe) BorderStroke(2.dp, ChampionshipGold) else BorderStroke(1.dp, PearlCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = when (idx) {
                                                0 -> ChampionshipGold
                                                1 -> Color(0xFF94A3B8)
                                                2 -> Color(0xFFB45309)
                                                else -> InkMuted
                                            }
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                "#${idx + 1} ${entry.displayName}${if (isMe) " (သင်)" else ""}",
                                                color = InkDark,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "ပွိုင့်: ${entry.rewardPoints} Pts",
                                                color = InkMuted,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    Text(
                                        formatKs(entry.totalWonKs),
                                        color = PitchEmeraldDark,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PointExchangeScreen(
    accountState: UiState<UserAccount?>,
    busy: Boolean,
    onBack: () -> Unit,
    onExchange: (Long) -> Unit
) {
    val account = (accountState as? UiState.Success)?.data
    val points = account?.rewardPoints ?: 0L
    var ptsInput by remember { mutableStateOf("10") }
    val parsedPts = ptsInput.toLongOrNull() ?: 0L

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "ပွိုင့်လဲလှယ်", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PearlCard),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = PitchEmeraldDark)
                        Spacer(Modifier.width(8.dp))
                        Text("လက်ဆောင်ပွိုင့် ငွေသားပြောင်းလဲရန်", color = InkDark, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("လက်ရှိပွိုင့်: $points Pts (10 Pts = 100 Ks)", color = PitchEmeraldDark, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10L, 50L, 100L, 500L).forEach { amt ->
                            FilterChip(
                                selected = parsedPts == amt,
                                onClick = { ptsInput = amt.toString() },
                                label = { Text("$amt Pts (${amt * 10} Ks)") }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ptsInput,
                        onValueChange = { ptsInput = it.filter { c -> c.isDigit() }.take(7) },
                        label = { Text("လဲလှယ်မည့် ပွိုင့် (အနည်းဆုံး 10)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = InkDark,
                            unfocusedTextColor = InkDark
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("points_exchange_input")
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { onExchange(parsedPts) },
                        enabled = !busy && parsedPts >= 10L && parsedPts <= points,
                        colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("exchange_points_button")
                    ) {
                        Text("ငွေသား ${formatKs(parsedPts * 10L)} သို့ ပြောင်းမည်")
                    }
                }
            }
        }
    }
}
