package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.BetOrder
import com.example.data.model.LotteryDraw
import com.example.data.model.SportMatch
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WalletRequest
import com.example.ui.components.HtetThuYaBrandLogo
import com.example.ui.components.OrderSlipCard
import com.example.ui.components.StatusBadge
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
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldDark

@Composable
fun AdminDashboardScreen(
    currentUserId: String,
    allOrdersState: UiState<List<BetOrder>>,
    allWalletRequestsState: UiState<List<WalletRequest>>,
    matchesState: UiState<List<SportMatch>>,
    drawsState: UiState<List<LotteryDraw>>,
    allUsersState: UiState<List<UserAccount>>,
    adminMembersState: UiState<List<AdminMember>>,
    adminInvitesState: UiState<List<AdminInvite>>,
    onSwitchToCustomer: () -> Unit,
    onAcceptOrder: (String, String) -> Unit,
    onSettleWin: (BetOrder, String) -> Unit,
    onSettleLoss: (BetOrder, String) -> Unit,
    onRejectOrder: (BetOrder, String) -> Unit,
    onProcessWalletRequest: (WalletRequest, Boolean, String) -> Unit,
    onDeleteWalletRequest: (String) -> Unit,
    onClearProcessedWalletRequests: (List<String>) -> Unit,
    onSaveNewMatch: (String, String, String, String, String, String, Double, Double) -> Unit,
    onUpdateMatchDetails: (String, String, String, String, String, String, String, Double, Double, String, Long, Long) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onSaveNewDraw: (String, String, Double) -> Unit,
    onUpdateLotteryDraw: (String, String, String, Double, String, String) -> Unit,
    onDeleteLotteryDraw: (String) -> Unit,
    onSeedSampleBoard: () -> Unit,
    onCreateAdminInvite: (String) -> Unit,
    onToggleAdminInvite: (String, Boolean) -> Unit,
    onRemoveAdminMember: (String) -> Unit,
    onAdjustUserBalance: (String, Long, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val orders = (allOrdersState as? UiState.Success)?.data ?: emptyList()
    val walletReqs = (allWalletRequestsState as? UiState.Success)?.data ?: emptyList()
    val matches = (matchesState as? UiState.Success)?.data ?: emptyList()
    val draws = (drawsState as? UiState.Success)?.data ?: emptyList()
    val users = (allUsersState as? UiState.Success)?.data ?: emptyList()
    val admins = (adminMembersState as? UiState.Success)?.data ?: emptyList()
    val invites = (adminInvitesState as? UiState.Success)?.data ?: emptyList()

    val pendingOrderCount = orders.count { it.status == "pending" }
    val pendingWalletCount = walletReqs.count { it.status == "pending" }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF3E0E12), Color(0xFF1F1612))
                        )
                    )
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HtetThuYaBrandLogo(size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Htet Thu Ya Admin",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "အော်ဒါစောင့်ကြည့် & စီမံခန့်ခွဲမှု",
                                color = Color(0xFFE5D2D4),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Surface(
                        color = PitchEmeraldDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable(onClick = onSwitchToCustomer)
                            .testTag("switch_to_customer_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Customer", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepPitchSurface,
                contentColor = ChampionshipGold,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("အော်ဒါ ($pendingOrderCount)") },
                    modifier = Modifier.testTag("admin_tab_orders")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("ငွေသွင်း/ထုတ် ($pendingWalletCount)") },
                    modifier = Modifier.testTag("admin_tab_wallet")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ပွဲစဉ် / ၃လုံး") },
                    modifier = Modifier.testTag("admin_tab_board")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("သူငယ်ချင်း Admin & Users") },
                    modifier = Modifier.testTag("admin_tab_team")
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (selectedTab) {
                    0 -> AdminOrdersTab(
                        orders = orders,
                        onAccept = { onAcceptOrder(it.id, "Admin လက်ခံပြီး") },
                        onWin = { onSettleWin(it, "အနိုင်ရ - ငွေရှင်းပြီး") },
                        onLose = { onSettleLoss(it, "စာရင်းပိတ်ပြီး") },
                        onReject = { onRejectOrder(it, "ပယ်ဖျက်ပြီး - ငွေပြန်အမ်းပြီး") }
                    )
                    1 -> AdminWalletRequestsTab(
                        requests = walletReqs,
                        onProcess = onProcessWalletRequest,
                        onDeleteRequest = onDeleteWalletRequest,
                        onClearProcessed = onClearProcessedWalletRequests
                    )
                    2 -> AdminBoardTab(
                        matches = matches,
                        draws = draws,
                        onSaveNewMatch = onSaveNewMatch,
                        onUpdateMatchDetails = onUpdateMatchDetails,
                        onDeleteMatch = onDeleteMatch,
                        onSaveNewDraw = onSaveNewDraw,
                        onUpdateLotteryDraw = onUpdateLotteryDraw,
                        onDeleteLotteryDraw = onDeleteLotteryDraw,
                        onSeedSampleBoard = onSeedSampleBoard
                    )
                    3 -> AdminTeamAndUsersTab(
                        currentUserId = currentUserId,
                        users = users,
                        admins = admins,
                        invites = invites,
                        onCreateInvite = onCreateAdminInvite,
                        onToggleInvite = onToggleAdminInvite,
                        onRemoveAdmin = onRemoveAdminMember,
                        onAdjustUserBalance = onAdjustUserBalance
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminOrdersTab(
    orders: List<BetOrder>,
    onAccept: (BetOrder) -> Unit,
    onWin: (BetOrder) -> Unit,
    onLose: (BetOrder) -> Unit,
    onReject: (BetOrder) -> Unit
) {
    var filter by remember { mutableStateOf("pending") }
    val filtered = remember(orders, filter) {
        when (filter) {
            "pending" -> orders.filter { it.status == "pending" }
            "accepted" -> orders.filter { it.status == "accepted" }
            "settled" -> orders.filter { it.status in listOf("won", "lost", "rejected") }
            else -> orders
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "pending" to "အသစ် (${orders.count { it.status == "pending" }})",
                    "accepted" to "လက်ခံပြီး (${orders.count { it.status == "accepted" }})",
                    "settled" to "ပြီးဆုံး",
                    "all" to "အားလုံး"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) }
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    "ဤကဏ္ဍတွင် Customer အော်ဒါ မရှိသေးပါ",
                    color = Color(0xFFB4C8BE),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            items(filtered, key = { it.id }) { order ->
                OrderSlipCard(
                    order = order,
                    isAdminView = true,
                    onAccept = onAccept,
                    onSettleWin = onWin,
                    onSettleLoss = onLose,
                    onReject = onReject
                )
            }
        }
    }
}

@Composable
private fun AdminWalletRequestsTab(
    requests: List<WalletRequest>,
    onProcess: (WalletRequest, Boolean, String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onClearProcessed: (List<String>) -> Unit
) {
    var deletingRequest by remember { mutableStateOf<WalletRequest?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    val processedIds = remember(requests) {
        requests.filter { it.status in listOf("approved", "rejected") }.map { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (processedIds.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = { showClearAllConfirm = true },
                    border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("clear_old_wallet_requests_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ပြီးစီးပြီးသား အဟောင်းများ ဖျက်မည် (${processedIds.size})", color = AdminCrimson)
                }
            }
        }
        if (requests.isEmpty()) {
            item {
                Text("ငွေသွင်း / ငွေထုတ် တောင်းဆိုမှု မရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(requests, key = { it.id }) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${req.customerName} • ${if (req.requestType == "topup") "ငွေသွင်း" else "ငွေထုတ်"}",
                                color = InkDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusBadge(req.status)
                                if (req.status != "pending") {
                                    IconButton(
                                        onClick = { deletingRequest = req },
                                        modifier = Modifier.testTag("delete_wallet_request_${req.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Old Request", tint = AdminCrimson)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${formatKs(req.amountKs)} (${req.channel})",
                            color = PitchEmeraldDark,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Ref: ${req.referenceNote}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        Text(formatTimestamp(req.createdAt), color = InkMuted, style = MaterialTheme.typography.labelMedium)

                        if (req.status == "pending") {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onProcess(req, true, "အတည်ပြုပြီး") },
                                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("အတည်ပြု")
                                }
                                OutlinedButton(
                                    onClick = { onProcess(req, false, "ပယ်ဖျက်ပြီး") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ပယ်ဖျက်", color = AdminCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deletingRequest?.let { req ->
        ConfirmDeleteDialog(
            title = "ငွေစာရင်းအဟောင်း ဖျက်မည်လား?",
            message = "${req.customerName} (${formatKs(req.amountKs)}) မှတ်တမ်းကို ဖျက်ပစ်ပါမည်။ Customer ၏ လက်ရှိလက်ကျန်ငွေ မပြောင်းလဲပါ။",
            onDismiss = { deletingRequest = null },
            onConfirm = {
                val id = req.id
                deletingRequest = null
                onDeleteRequest(id)
            }
        )
    }

    if (showClearAllConfirm) {
        ConfirmDeleteDialog(
            title = "ပြီးစီးပြီးသား အဟောင်းများ အားလုံးဖျက်မည်လား?",
            message = "အတည်ပြုပြီး / ပယ်ဖျက်ပြီး ငွေသွင်း၊ ငွေထုတ် မှတ်တမ်း ${processedIds.size} ခုလုံးကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                showClearAllConfirm = false
                onClearProcessed(processedIds)
            }
        )
    }
}

@Composable
private fun AdminBoardTab(
    matches: List<SportMatch>,
    draws: List<LotteryDraw>,
    onSaveNewMatch: (String, String, String, String, String, String, Double, Double) -> Unit,
    onUpdateMatchDetails: (String, String, String, String, String, String, String, Double, Double, String, Long, Long) -> Unit,
    onDeleteMatch: (String) -> Unit,
    onSaveNewDraw: (String, String, Double) -> Unit,
    onUpdateLotteryDraw: (String, String, String, Double, String, String) -> Unit,
    onDeleteLotteryDraw: (String) -> Unit,
    onSeedSampleBoard: () -> Unit
) {
    var showAddMatch by remember { mutableStateOf(false) }
    var editingMatch by remember { mutableStateOf<SportMatch?>(null) }
    var deletingMatch by remember { mutableStateOf<SportMatch?>(null) }
    var showAddDraw by remember { mutableStateOf(false) }
    var editingDraw by remember { mutableStateOf<LotteryDraw?>(null) }
    var deletingDraw by remember { mutableStateOf<LotteryDraw?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddMatch = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                    modifier = Modifier.weight(1f).testTag("admin_add_match_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ပွဲစဉ်သစ်")
                }
                Button(
                    onClick = { showAddDraw = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                    modifier = Modifier.weight(1f).testTag("admin_add_draw_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("၃လုံးထီသစ်")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSeedSampleBoard,
                modifier = Modifier.fillMaxWidth().testTag("seed_sample_board_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChampionshipGold)
                Spacer(Modifier.width(6.dp))
                Text("နမူနာ ဘောလုံးပွဲစဉ်နှင့် ၃လုံးထီ အလိုအလျောက်ထည့်မည်", color = ChampionshipGold)
            }
        }

        item {
            Text("၃လုံးထီ အကြိမ်များ (နှိပ်ပြီး ပြင်ဆင် / ဖျက်ပါ)", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
        }
        items(draws, key = { it.id }) { draw ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PearlCard),
                modifier = Modifier.fillMaxWidth().clickable { editingDraw = draw }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(draw.roundTitle, color = InkDark, fontWeight = FontWeight.Bold)
                        Text("${draw.closeTimeLabel} • ${draw.payoutMultiplier.toInt()}x", color = InkMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (draw.winningNumber.isNotBlank()) {
                            Text(draw.winningNumber, color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                        }
                        StatusBadge(draw.status)
                        IconButton(
                            onClick = { deletingDraw = draw },
                            modifier = Modifier.testTag("delete_draw_${draw.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Draw", tint = AdminCrimson)
                        }
                    }
                }
            }
        }

        item {
            Text("ဘောလုံးပွဲစဉ်များ (နှိပ်ပြီး ပြင်ဆင် / ဖျက်ပါ)", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
        }
        items(matches, key = { it.id }) { match ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PearlCard),
                modifier = Modifier.fillMaxWidth().clickable { editingMatch = match }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${match.homeTeam} vs ${match.awayTeam}", color = InkDark, fontWeight = FontWeight.Bold)
                        Text("${match.league} • ${match.kickoffLabel}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        Text("${match.handicapLine} • ဂိုး ${match.goalLine}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(match.status)
                            if (match.status == "finished") {
                                Spacer(Modifier.height(4.dp))
                                Text("${match.homeScore} - ${match.awayScore}", color = InkDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(
                            onClick = { deletingMatch = match },
                            modifier = Modifier.testTag("delete_match_${match.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Match", tint = AdminCrimson)
                        }
                    }
                }
            }
        }
    }

    if (showAddMatch) {
        AddMatchDialog(
            onDismiss = { showAddMatch = false },
            onSave = { l, h, a, k, hand, goal, bOdds, gOdds ->
                showAddMatch = false
                onSaveNewMatch(l, h, a, k, hand, goal, bOdds, gOdds)
            }
        )
    }
    editingMatch?.let { m ->
        EditMatchResultDialog(
            match = m,
            onDismiss = { editingMatch = null },
            onUpdate = { l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore ->
                editingMatch = null
                onUpdateMatchDetails(m.id, l, h, a, k, hand, goal, bOdds, gOdds, st, hs, ascore)
            },
            onRequestDelete = {
                editingMatch = null
                deletingMatch = m
            }
        )
    }
    deletingMatch?.let { m ->
        ConfirmDeleteDialog(
            title = "ပွဲစဉ် ဖျက်မည်လား?",
            message = "${m.homeTeam} vs ${m.awayTeam} (${m.league}) ပွဲစဉ်ကို အပြီးတိုင် ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingMatch = null },
            onConfirm = {
                val id = m.id
                deletingMatch = null
                onDeleteMatch(id)
            }
        )
    }
    if (showAddDraw) {
        AddLotteryDrawDialog(
            onDismiss = { showAddDraw = false },
            onSave = { t, c, mult ->
                showAddDraw = false
                onSaveNewDraw(t, c, mult)
            }
        )
    }
    editingDraw?.let { d ->
        PublishLotteryResultDialog(
            draw = d,
            onDismiss = { editingDraw = null },
            onPublish = { title, closeLabel, mult, num, st ->
                editingDraw = null
                onUpdateLotteryDraw(d.id, title, closeLabel, mult, num, st)
            },
            onRequestDelete = {
                editingDraw = null
                deletingDraw = d
            }
        )
    }
    deletingDraw?.let { d ->
        ConfirmDeleteDialog(
            title = "၃လုံးထီ အကြိမ် ဖျက်မည်လား?",
            message = "${d.roundTitle} ကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingDraw = null },
            onConfirm = {
                val id = d.id
                deletingDraw = null
                onDeleteLotteryDraw(id)
            }
        )
    }
}

@Composable
private fun AdminTeamAndUsersTab(
    currentUserId: String,
    users: List<UserAccount>,
    admins: List<AdminMember>,
    invites: List<AdminInvite>,
    onCreateInvite: (String) -> Unit,
    onToggleInvite: (String, Boolean) -> Unit,
    onRemoveAdmin: (String) -> Unit,
    onAdjustUserBalance: (String, Long, String) -> Unit
) {
    val context = LocalContext.current
    var inviteLabel by remember { mutableStateOf("") }
    var adjustingUser by remember { mutableStateOf<UserAccount?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "သူငယ်ချင်းများကို Admin အဖြစ် မျှဝေရန်",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Invite Code တစ်ခု ဖန်တီးပြီး သူငယ်ချင်းထံ Share ပို့ပါ။ သူငယ်ချင်းသည် App ထဲရှိ 'Admin Key' မှတစ်ဆင့် Code ရိုက်ထည့်ပြီး အော်ဒါများကို အတူတကွ စီမံခန့်ခွဲနိုင်ပါမည်။",
                        color = Color(0xFFB4C8BE),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inviteLabel,
                            onValueChange = { inviteLabel = it },
                            label = { Text("သူငယ်ချင်းအမည် / မှတ်စု") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).testTag("invite_label_input")
                        )
                        Button(
                            onClick = {
                                onCreateInvite(inviteLabel.ifBlank { "Co-Admin Friend" })
                                inviteLabel = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier.testTag("create_invite_button")
                        ) {
                            Text("Code ထုတ်မည်")
                        }
                    }
                }
            }
        }

        if (invites.isNotEmpty()) {
            item {
                Text("မျှဝေထားသော Invite Codes", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
            }
            items(invites, key = { it.code }) { inv ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(inv.code, color = InkDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            Text(inv.label, color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Htet Thu Ya Admin Invite Code: ${inv.code}\nApp ထဲတွင် Google ဖြင့် ဝင်ပြီး 'Admin Key' ခလုတ်ကိုနှိပ်၍ ဤ Code ကို ထည့်သွင်းပါ။"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Admin Invite"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Invite Code", tint = PitchEmeraldDark)
                            }
                            Switch(
                                checked = inv.active,
                                onCheckedChange = { onToggleInvite(inv.code, it) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Admin အဖွဲ့ဝင်များ (${admins.size})", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
        }
        items(admins, key = { it.userId }) { member ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PearlCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(member.displayName, color = InkDark, fontWeight = FontWeight.Bold)
                        Text("Role: ${member.role} • Code: ${member.inviteCodeUsed}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (member.role != "owner" && member.userId != currentUserId) {
                        IconButton(onClick = { onRemoveAdmin(member.userId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Admin", tint = AdminCrimson)
                        }
                    }
                }
            }
        }

        item {
            Text("Customer စာရင်း (${users.size}) - နှိပ်ပြီး ငွေပြင်နိုင်သည်", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
        }
        items(users, key = { it.userId }) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PearlCard),
                modifier = Modifier.fillMaxWidth().clickable { adjustingUser = user }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PitchEmeraldDark)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(user.displayName, color = InkDark, fontWeight = FontWeight.Bold)
                            Text("Active: ${formatKs(user.activeStakeKs)} • Pts: ${user.rewardPoints}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        formatKs(user.balanceKs),
                        color = PitchEmeraldDark,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    adjustingUser?.let { target ->
        AdjustUserBalanceDialog(
            user = target,
            onDismiss = { adjustingUser = null },
            onAdjust = { delta, note ->
                adjustingUser = null
                onAdjustUserBalance(target.userId, delta, note)
            }
        )
    }
}
