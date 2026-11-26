package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.example.data.model.BetOrder
import com.example.data.model.BrandingConfig
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LedgerTransaction
import com.example.data.model.LiveFootballFixture
import com.example.data.model.LotteryDraw
import com.example.data.model.PaymentAccount
import com.example.data.model.SportMatch
import com.example.data.model.SupportAgentProfile
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.WalletConfig
import com.example.data.model.WalletRequest
import com.example.ui.components.FullScreenReceiptDialog
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.OrderSlipCard
import com.example.ui.components.ReceiptImageThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.TeamLogoAvatar
import com.example.ui.components.encodeUriToReceiptBase64
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
import kotlinx.coroutines.delay

@Composable
fun OrdersHistoryScreen(
    activeOnly: Boolean,
    ordersState: UiState<List<BetOrder>>,
    matchesState: UiState<List<SportMatch>> = UiState.Success(emptyList()),
    onRefreshLiveScores: () -> Unit = {},
    onBack: () -> Unit,
    onDeleteOrder: (String) -> Unit = {},
    onClearCompletedOrders: (List<String>) -> Unit = {}
) {
    val title = if (activeOnly) "လောင်းထားသောပွဲစဉ်များ" else "ပွဲစဉ်ဟောင်းများ (ပြီးဆုံး)"
    var deletingOrder by remember { mutableStateOf<BetOrder?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showOrdersTable by remember { mutableStateOf(true) }
    var tickerNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val matchesById = remember(matchesState) {
        ((matchesState as? UiState.Success)?.data ?: emptyList()).associateBy { it.id }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            tickerNowMs = System.currentTimeMillis()
        }
    }

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
                            item {
                                HideBarSectionHeader(
                                    title = title,
                                    countBadge = "${filtered.size} စလစ်",
                                    isVisible = showOrdersTable,
                                    onToggle = { showOrdersTable = !showOrdersTable },
                                    testTag = if (activeOnly) "hide_bar_active_orders" else "hide_bar_past_orders"
                                )
                            }
                            if (showOrdersTable) {
                                item {
                                    OutlinedButton(
                                        onClick = onRefreshLiveScores,
                                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.7f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("orders_refresh_live_goals_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("တိုက်ရိုက်ဂိုးရလဒ် Refresh", color = ChampionshipGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (!activeOnly && filtered.isNotEmpty()) {
                                    item {
                                        OutlinedButton(
                                            onClick = { showClearAllConfirm = true },
                                            border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.7f)),
                                            modifier = Modifier.fillMaxWidth().testTag("customer_clear_completed_orders_button")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("ပြီးဆုံးပြီးသား အော်ဒါအားလုံး ဖျက်မည် (${filtered.size})", color = AdminCrimson)
                                        }
                                    }
                                }
                                items(filtered, key = { "${it.id}_${tickerNowMs / 15000L}" }) { order ->
                                    OrderSlipCard(
                                        order = order,
                                        isAdminView = false,
                                        matchesById = matchesById,
                                        onDeleteOrder = if (!activeOnly) { target -> deletingOrder = target } else null
                                    )
                                }
                            }
                        }

                        deletingOrder?.let { target ->
                            ConfirmDeleteDialog(
                                title = "ပြီးဆုံးအော်ဒါ ဖျက်မည်လား?",
                                message = "${target.drawRound.ifBlank { "အော်ဒါ" }} (${formatKs(target.stakeKs)}) မှတ်တမ်းကို ဖျက်ပစ်ပါမည်။",
                                onDismiss = { deletingOrder = null },
                                onConfirm = {
                                    val id = target.id
                                    deletingOrder = null
                                    onDeleteOrder(id)
                                }
                            )
                        }

                        if (showClearAllConfirm) {
                            val ids = filtered.map { it.id }
                            ConfirmDeleteDialog(
                                title = "ပြီးဆုံးပြီးသား အားလုံးဖျက်မည်လား?",
                                message = "ပြီးဆုံးပြီးသော အော်ဒါမှတ်တမ်း ${ids.size} ခုလုံးကို ဖျက်ပစ်ပါမည်။",
                                onDismiss = { showClearAllConfirm = false },
                                onConfirm = {
                                    showClearAllConfirm = false
                                    onClearCompletedOrders(ids)
                                }
                            )
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
    walletConfig: WalletConfig,
    brandingConfig: BrandingConfig = BrandingConfig(),
    supportAgents: List<SupportAgentProfile> = emptyList(),
    paymentAccounts: List<PaymentAccount> = emptyList(),
    busy: Boolean,
    onBack: () -> Unit,
    onSubmitWalletRequest: (requestType: String, amountKs: Long, channel: String, referenceNote: String, receiptImageBase64: String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var reqType by remember { mutableStateOf("topup") }
    var channel by remember { mutableStateOf("KBZPay") }
    var amountInput by remember { mutableStateOf("5000") }
    var refNote by remember { mutableStateOf("") }
    var receiptImageBase64 by remember { mutableStateOf("") }
    var previewReceiptBase64 by remember { mutableStateOf<String?>(null) }
    var copiedAccountId by remember { mutableStateOf<String?>(null) }
    var showAgentsCard by remember { mutableStateOf(true) }
    var showAccountsCard by remember { mutableStateOf(true) }
    var showRequestHistory by remember { mutableStateOf(true) }
    var showTransactionsList by remember { mutableStateOf(true) }

    val pickReceiptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = encodeUriToReceiptBase64(context, uri)
            if (!encoded.isNullOrBlank()) {
                receiptImageBase64 = encoded
            } else {
                Toast.makeText(context, "ပုံဖိုင် အရွယ်အစားကြီးလွန်းနေပါသည် (အခြားပုံ ရွေးပါ)", Toast.LENGTH_LONG).show()
            }
        }
    }

    val activePaymentAccounts = remember(paymentAccounts) {
        paymentAccounts.filter { it.active }
    }
    val activeAgents = remember(supportAgents) {
        supportAgents.filter { it.active }
    }
    val channelOptions = remember(activePaymentAccounts) {
        (activePaymentAccounts.map { it.providerName } + listOf("KBZPay", "WavePay", "CB Pay", "Cash"))
            .filter { it.isNotBlank() }
            .distinct()
    }

    val balanceKs = (accountState as? UiState.Success)?.data?.balanceKs ?: 0L
    val parsedAmt = amountInput.toLongOrNull() ?: 0L
    val canSubmit = !busy && parsedAmt >= 500L &&
        (refNote.trim().isNotEmpty() || receiptImageBase64.isNotBlank()) &&
        (reqType == "topup" || parsedAmt <= balanceKs)

    val activePercent = if (reqType == "topup") walletConfig.depositPercent else walletConfig.withdrawPercent
    val activeMode = if (reqType == "topup") walletConfig.depositMode else walletConfig.withdrawMode
    val netPreviewKs = if (reqType == "topup") walletConfig.computeNetTopup(parsedAmt) else walletConfig.computeNetWithdraw(parsedAmt)

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "ငွေစာရင်း (Wallet)", onBack = onBack, brandingConfig = brandingConfig) }
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
                    if (brandingConfig.maxContactMainAdminKs > 0L) {
                        item {
                            Surface(
                                color = Color(0xFF422006),
                                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Main Admin တိုက်ရိုက် ဆက်သွယ်ရန် ကန့်သတ်ချက်: ${formatKs(brandingConfig.maxContactMainAdminKs)} နှင့်အထက်",
                                        color = ChampionshipGold,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (brandingConfig.mainAdminContactNote.isNotBlank()) {
                                        Text(
                                            brandingConfig.mainAdminContactNote,
                                            color = Color(0xFFFDE68A),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (activeAgents.isNotEmpty()) {
                        item {
                            HideBarSectionHeader(
                                title = "Admin နှင့် Second Admin ဆက်သွယ်ရန်",
                                countBadge = "${activeAgents.size}",
                                isVisible = showAgentsCard,
                                onToggle = { showAgentsCard = !showAgentsCard },
                                testTag = "hide_bar_wallet_agents"
                            )
                        }
                        if (showAgentsCard) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                                    border = BorderStroke(1.dp, PitchEmerald.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Admin နှင့် Second Admin ဆက်သွယ်ရန် လိပ်စာများ",
                                            color = ChampionshipGold,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        activeAgents.forEach { ag ->
                                            Surface(
                                                color = Color(0xFF1A3329),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(ag.displayName.ifBlank { "Admin" }, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Surface(
                                                            color = if (ag.role == "owner") ChampionshipGold else Color(0xFF0284C7),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                if (ag.role == "owner") "Main Admin" else "Second Admin",
                                                                color = if (ag.role == "owner") InkDark else Color.White,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    if (ag.phone.isNotBlank()) {
                                                        Text("ဖုန်း: ${ag.phone}", color = ChampionshipGold, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    if (ag.messagingHandle.isNotBlank()) {
                                                        Text("Viber/Telegram: ${ag.messagingHandle}", color = Color(0xFFD1FAE5), style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    if (ag.paymentSummary.isNotBlank()) {
                                                        Text("ငွေကြေးစနစ်: ${ag.paymentSummary}", color = Color(0xFF93C5FD), style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    if (ag.note.isNotBlank()) {
                                                        Text(ag.note, color = Color(0xFFB4C8BE), style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (reqType == "topup" && activePaymentAccounts.isNotEmpty()) {
                        item {
                            HideBarSectionHeader(
                                title = "ငွေလွှဲပို့ရန် Admin အကောင့်များ",
                                countBadge = "${activePaymentAccounts.size}",
                                isVisible = showAccountsCard,
                                onToggle = { showAccountsCard = !showAccountsCard },
                                testTag = "hide_bar_wallet_payment_accounts"
                            )
                        }
                        if (showAccountsCard) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "ငွေလွှဲပို့ရန် Admin အကောင့်များ",
                                            color = ChampionshipGold,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        activePaymentAccounts.forEach { acc ->
                                            Surface(
                                                color = Color(0xFF1E372D),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            "${acc.providerName} • ${acc.accountName}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (acc.ownerAdminName.isNotBlank()) {
                                                            Text(
                                                                "တာဝန်ခံ: ${acc.ownerAdminName}",
                                                                color = Color(0xFF93C5FD),
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                        Text(
                                                            acc.accountNumber,
                                                            color = ChampionshipGold,
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (acc.note.isNotBlank()) {
                                                            Text(acc.note, color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            clipboard.setText(AnnotatedString(acc.accountNumber))
                                                            copiedAccountId = acc.id
                                                            channel = acc.providerName
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            if (copiedAccountId == acc.id) "ကူးပြီး" else "Copy",
                                                            color = ChampionshipGold
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
                                    channelOptions.forEach { ch ->
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
                                if (activePercent > 0.0 && activeMode != "none" && parsedAmt > 0L) {
                                    Spacer(Modifier.height(8.dp))
                                    val modeText = if (activeMode == "bonus") "အပိုဆု +${activePercent}%" else "ဝန်ဆောင်ခ -${activePercent}%"
                                    Surface(
                                        color = Color(0xFFE6F4EE),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "$modeText → အမှန်တကယ် ရရှိမည့်ငွေ: ${formatKs(netPreviewKs)}",
                                            color = PitchEmeraldDark,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
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
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            pickReceiptLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.weight(1f).testTag("wallet_pick_receipt_button")
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PitchEmeraldDark, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (receiptImageBase64.isBlank()) "ပြေစာ ဓာတ်ပုံ ပူးတွဲရန် (Device မှ ရွေးပါ)" else "ပုံပြောင်းရွေးမည်",
                                            color = PitchEmeraldDark,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    if (receiptImageBase64.isNotBlank()) {
                                        ReceiptImageThumbnail(
                                            base64 = receiptImageBase64,
                                            onClick = { previewReceiptBase64 = receiptImageBase64 },
                                            size = 48.dp
                                        )
                                        IconButton(
                                            onClick = { receiptImageBase64 = "" },
                                            modifier = Modifier.testTag("wallet_clear_picked_receipt")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear receipt photo", tint = AdminCrimson)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val noteToSend = refNote.trim().ifBlank { "ပြေစာဓာတ်ပုံ ပူးတွဲထားသည်" }
                                        onSubmitWalletRequest(reqType, parsedAmt, channel, noteToSend, receiptImageBase64)
                                        refNote = ""
                                        receiptImageBase64 = ""
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
                            HideBarSectionHeader(
                                title = "တောင်းဆိုမှု အခြေအနေများ",
                                countBadge = "${requests.size}",
                                isVisible = showRequestHistory,
                                onToggle = { showRequestHistory = !showRequestHistory },
                                testTag = "hide_bar_wallet_request_history"
                            )
                        }
                        if (showRequestHistory) {
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
                                            if (req.netAmountKs > 0L && req.netAmountKs != req.amountKs) {
                                                Text(
                                                    text = "နှုန်း (${if (req.rateMode == "bonus") "+" else "-"}${req.ratePercent}%) → အသားတင်: ${formatKs(req.netAmountKs)}",
                                                    color = PitchEmeraldDark,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (req.adminNote.isNotBlank()) {
                                                Text("Admin: ${req.adminNote}", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (req.receiptImageBase64.isNotBlank()) {
                                                ReceiptImageThumbnail(
                                                    base64 = req.receiptImageBase64,
                                                    onClick = { previewReceiptBase64 = req.receiptImageBase64 },
                                                    size = 46.dp
                                                )
                                            }
                                            StatusBadge(req.status)
                                        }
                                    }
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
                    item {
                        HideBarSectionHeader(
                            title = "ငွေစာရင်း အသေးစိတ် ဇယား",
                            countBadge = "${txs.size}",
                            isVisible = showTransactionsList,
                            onToggle = { showTransactionsList = !showTransactionsList },
                            testTag = "hide_bar_wallet_ledger_transactions"
                        )
                    }
                    if (showTransactionsList) {
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
                                        if (tx.txType == "point_gift") {
                                            Surface(color = ChampionshipGold.copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp)) {
                                                Text(
                                                    text = "Gift Pts",
                                                    color = InkDark,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
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
    }

    previewReceiptBase64?.let { b64 ->
        FullScreenReceiptDialog(
            base64 = b64,
            onDismiss = { previewReceiptBase64 = null }
        )
    }
}

@Composable
fun ResultsScreen(
    matchesState: UiState<List<SportMatch>>,
    drawsState: UiState<List<LotteryDraw>>,
    liveFeedState: UiState<List<LiveFootballFixture>>,
    onRefreshLiveFeed: () -> Unit,
    onBack: () -> Unit
) {
    val liveOrFinishedMatches = (matchesState as? UiState.Success)?.data?.filter {
        it.status == "finished" || (it.status == "closed" && it.liveClock.isNotBlank())
    } ?: emptyList()
    val finishedDraws = (drawsState as? UiState.Success)?.data?.filter { it.status == "finished" } ?: emptyList()
    val liveFeed = (liveFeedState as? UiState.Success)?.data?.filter {
        it.status == "closed" || it.status == "finished"
    } ?: emptyList()

    var showDrawsSection by remember { mutableStateOf(true) }
    var showAppMatchesSection by remember { mutableStateOf(true) }
    var showLiveFeedSection by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = DeepPitchBg,
        topBar = { SubScreenTopBar(title = "တိုက်ရိုက် & ပွဲပြီး ရလဒ်များ", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRefreshLiveFeed,
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("refresh_live_results_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Live Scores", tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("တိုက်ရိုက်ပြန်စစ်", color = ChampionshipGold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item {
                HideBarSectionHeader(
                    title = "၂လုံး / ၃လုံးထီ ရလဒ်များ",
                    countBadge = "${finishedDraws.size}",
                    isVisible = showDrawsSection,
                    onToggle = { showDrawsSection = !showDrawsSection },
                    testTag = "hide_bar_results_draws",
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
            if (showDrawsSection) {
                if (finishedDraws.isEmpty()) {
                    item {
                        Text("၂လုံး / ၃လုံးထီ ရလဒ် မရှိသေးပါ", color = Color(0xFFB4C8BE))
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(color = ChampionshipGold, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = draw.drawType.uppercase(),
                                            color = InkDark,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(draw.roundTitle, color = InkDark, fontWeight = FontWeight.Bold)
                                }
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
            }

            item {
                Spacer(Modifier.height(6.dp))
                HideBarSectionHeader(
                    title = "App ပွဲစဉ် ရလဒ်များ",
                    countBadge = "${liveOrFinishedMatches.size}",
                    isVisible = showAppMatchesSection,
                    onToggle = { showAppMatchesSection = !showAppMatchesSection },
                    testTag = "hide_bar_results_app_matches",
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
            if (showAppMatchesSection) {
                if (liveOrFinishedMatches.isEmpty()) {
                    item {
                        Text("ပြီးဆုံး/တိုက်ရိုက် App ပွဲစဉ် မရှိသေးပါ", color = Color(0xFFB4C8BE))
                    }
                } else {
                    items(liveOrFinishedMatches, key = { it.id }) { m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PearlCard),
                            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m.league, color = PitchEmeraldDark, style = MaterialTheme.typography.labelLarge)
                                    val badgeLabel = if (m.status == "closed" && m.liveClock.isNotBlank()) m.liveClock else "FT"
                                    Surface(
                                        color = if (m.status == "closed") AdminCrimson else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            color = if (m.status == "closed") Color.White else InkDark,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        TeamLogoAvatar(logoUrl = m.homeLogoUrl, teamName = m.homeTeam, size = 24.dp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("${m.homeTeam} vs ${m.awayTeam}", color = InkDark, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                                        Spacer(Modifier.width(6.dp))
                                        TeamLogoAvatar(logoUrl = m.awayLogoUrl, teamName = m.awayTeam, size = 24.dp)
                                    }
                                    Spacer(Modifier.width(8.dp))
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

            item {
                Spacer(Modifier.height(6.dp))
                HideBarSectionHeader(
                    title = "တိုက်ရိုက် ဘောလုံးရလဒ်များ (Live Feed)",
                    countBadge = "${liveFeed.size}",
                    isVisible = showLiveFeedSection,
                    onToggle = { showLiveFeedSection = !showLiveFeedSection },
                    testTag = "hide_bar_results_live_feed",
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }
            if (showLiveFeedSection) {
                when (liveFeedState) {
                    is UiState.Loading -> item {
                        CircularProgressIndicator(color = ChampionshipGold, modifier = Modifier.padding(12.dp))
                    }
                    is UiState.Error -> item {
                        Text(liveFeedState.message, color = Color(0xFFFCA5A5))
                    }
                    is UiState.Success -> {
                        if (liveFeed.isEmpty()) {
                            item {
                                Text("လက်ရှိ ကစားနေဆဲ / ပြီးဆုံး နိုင်ငံတကာပွဲစဉ် မရှိသေးပါ", color = Color(0xFFB4C8BE))
                            }
                        } else {
                            items(liveFeed, key = { it.externalId }) { fx ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                                    modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${fx.league} • ${fx.matchDate}", color = PitchEmeraldDark, style = MaterialTheme.typography.labelLarge)
                                            Surface(
                                                color = if (fx.status == "closed") AdminCrimson else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = fx.liveClock.ifBlank { if (fx.status == "finished") "FT" else "LIVE" },
                                                    color = if (fx.status == "closed") Color.White else InkDark,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${fx.homeTeam} vs ${fx.awayTeam}", color = InkDark, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            Text(
                                                "${fx.homeScore} - ${fx.awayScore}",
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
                    var showLeaderboardTable by remember { mutableStateOf(true) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            HideBarSectionHeader(
                                title = "ထိပ်တန်း ကစားသမား ဇယား",
                                countBadge = "${entries.size}",
                                isVisible = showLeaderboardTable,
                                onToggle = { showLeaderboardTable = !showLeaderboardTable },
                                testTag = "hide_bar_leaderboard_table"
                            )
                        }
                        if (showLeaderboardTable) {
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
