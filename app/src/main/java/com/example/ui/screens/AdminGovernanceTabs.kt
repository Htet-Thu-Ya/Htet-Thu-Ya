package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.model.AppUpdateConfig
import com.example.data.network.ApkDownloadHelper
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.AdminPermissions
import com.example.data.model.AuditLogEntry
import com.example.data.model.BetOrder
import com.example.data.model.LedgerTransaction
import com.example.data.model.PaymentAccount
import com.example.data.model.SecurityConfig
import com.example.data.model.SupportAgentProfile
import com.example.data.model.SystemGameConfig
import com.example.data.model.UserAccount
import com.example.data.model.WalletRequest
import com.example.ui.components.FullScreenReceiptDialog
import com.example.ui.components.HideBarSectionHeader
import com.example.ui.components.ReceiptImageThumbnail
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatKs
import com.example.ui.components.formatRemainingReopenCountdown
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.AmberPending
import com.example.ui.theme.AmberPendingSoft
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminFinancialOverviewTab(
    users: List<UserAccount>,
    orders: List<BetOrder>,
    walletRequests: List<WalletRequest>,
    transactions: List<LedgerTransaction> = emptyList(),
    paymentAccounts: List<PaymentAccount>,
    isOwnerAdmin: Boolean = true,
    busy: Boolean,
    onSavePaymentAccount: (id: String?, providerName: String, accountName: String, accountNumber: String, note: String, active: Boolean) -> Unit,
    onDeletePaymentAccount: (String) -> Unit,
    onEditWalletRequest: (
        reqId: String,
        amountKs: Long,
        netAmountKs: Long,
        channel: String,
        referenceNote: String,
        status: String,
        adminNote: String,
        isHidden: Boolean,
        receiptImageBase64: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteWalletRequest: (String) -> Unit = {},
    onEditTransaction: (
        txId: String,
        title: String,
        amountKs: Long,
        direction: String,
        isHidden: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    onDeleteTransaction: (String) -> Unit = {},
    onClearAllTransactions: (List<String>) -> Unit = {}
) {
    var editingAccount by remember { mutableStateOf<PaymentAccount?>(null) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var showSummaryCard by remember { mutableStateOf(true) }
    var showLedgerManager by remember { mutableStateOf(true) }
    var showHiddenItems by remember { mutableStateOf(false) }
    var ledgerSubTab by remember { mutableStateOf("requests") } // "requests" or "ledger"

    var editingWalletReq by remember { mutableStateOf<WalletRequest?>(null) }
    var deletingWalletReq by remember { mutableStateOf<WalletRequest?>(null) }
    var zoomingReceiptBase64 by remember { mutableStateOf<String?>(null) }
    var editingTx by remember { mutableStateOf<LedgerTransaction?>(null) }
    var deletingTx by remember { mutableStateOf<LedgerTransaction?>(null) }
    var showClearAllTxConfirm by remember { mutableStateOf(false) }

    val usersById = remember(users) { users.associateBy { it.userId } }

    val visibleRequests = remember(walletRequests, showHiddenItems) {
        if (showHiddenItems) walletRequests else walletRequests.filter { !it.isHidden }
    }
    val visibleTransactions = remember(transactions, showHiddenItems) {
        if (showHiddenItems) transactions else transactions.filter { !it.isHidden }
    }
    val hiddenFinanceCount = remember(walletRequests, transactions) {
        walletRequests.count { it.isHidden } + transactions.count { it.isHidden }
    }

    val nonHiddenOrders = remember(orders) { orders.filter { !it.isHidden } }
    val nonHiddenReqs = remember(walletRequests) { walletRequests.filter { !it.isHidden } }

    val totalTopups = remember(nonHiddenReqs) {
        nonHiddenReqs.filter { it.requestType == "topup" && it.status == "approved" }.sumOf { it.amountKs }
    }
    val totalWithdrawals = remember(nonHiddenReqs) {
        nonHiddenReqs.filter { it.requestType == "withdraw" && it.status == "approved" }.sumOf { it.amountKs }
    }
    val awaitingDualCount = remember(walletRequests) {
        walletRequests.count { it.status == "awaiting_main_approval" }
    }
    val totalStake = remember(nonHiddenOrders) { nonHiddenOrders.filter { it.status != "rejected" }.sumOf { it.stakeKs } }
    val totalWonPayout = remember(nonHiddenOrders) { nonHiddenOrders.filter { it.status == "won" }.sumOf { it.actualPayoutKs } }
    val totalLostStake = remember(nonHiddenOrders) { nonHiddenOrders.filter { it.status == "lost" }.sumOf { it.stakeKs } }
    val totalCommission = remember(nonHiddenOrders) { nonHiddenOrders.filter { it.status == "won" }.sumOf { it.commissionKs } }
    val houseNetProfit = totalLostStake - totalWonPayout + totalCommission
    val totalUserBalances = remember(users) { users.sumOf { it.balanceKs } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSummaryCard = !showSummaryCard }
                            .testTag("toggle_finance_overview_card"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ဘဏ္ဍာရေးနှင့် ငွေစာရင်း အပြည့်အစုံ",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!showSummaryCard) {
                                Text(
                                    text = "အမြတ်: ${(if (houseNetProfit >= 0) "+" else "-") + formatKs(kotlin.math.abs(houseNetProfit))} • လက်ကျန်စုစုပေါင်း: ${formatKs(totalUserBalances)}",
                                    color = Color(0xFFB4C8BE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showSummaryCard) "ဝှက်မည်" else "ပြမည်",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showSummaryCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle financial overview",
                                tint = ChampionshipGold
                            )
                        }
                    }

                    if (showSummaryCard) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("စုစုပေါင်း ငွေသွင်း", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                                Text(formatKs(totalTopups), color = Color(0xFF6EE7B7), fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("စုစုပေါင်း ငွေထုတ်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                                Text(formatKs(totalWithdrawals), color = Color(0xFFFCA5A5), fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("စုစုပေါင်း ကော်မရှင်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                                Text(formatKs(totalCommission), color = ChampionshipGold, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("Admin အမြတ်/အရှုံး", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = (if (houseNetProfit >= 0) "+" else "-") + formatKs(kotlin.math.abs(houseNetProfit)),
                                    color = if (houseNetProfit >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Customer လက်ကျန်စုစုပေါင်း: ${formatKs(totalUserBalances)}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Text("Dual Pending: $awaitingDualCount", color = ChampionshipGold, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text("Total Valid Betting Volume: ${formatKs(totalStake)}", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ChampionshipGold)
                    Spacer(Modifier.width(8.dp))
                    Text("ငွေပေးချေမှု အကောင့်များ (${paymentAccounts.size})", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showCreateAccountDialog = true },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                    modifier = Modifier.testTag("add_payment_account_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("အကောင့်သစ်", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (paymentAccounts.isEmpty()) {
            item {
                Text("ငွေလက်ခံအကောင့် မထည့်ရသေးပါ (Customer Wallet တွင် ဖော်ပြရန် ထည့်သွင်းပါ)။", color = Color(0xFFB4C8BE))
            }
        } else {
            items(paymentAccounts, key = { it.id }) { acc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PearlCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${acc.providerName} • ${acc.accountName}", color = InkDark, fontWeight = FontWeight.Bold)
                                StatusBadge(if (acc.active) "active" else "suspended")
                            }
                            Text(acc.accountNumber, color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            Surface(color = Color(0xFFE6F4EE), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = if (acc.ownerAdminName.isNotBlank()) "Admin: ${acc.ownerAdminName}" else "Main Admin",
                                    color = PitchEmeraldDark,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (acc.note.isNotBlank()) {
                                Text(acc.note, color = InkMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row {
                            IconButton(onClick = { editingAccount = acc }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Account", tint = PitchEmeraldDark)
                            }
                            IconButton(onClick = { onDeletePaymentAccount(acc.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = AdminCrimson)
                            }
                        }
                    }
                }
            }
        }

        // Wallet Requests & Ledger Transactions Manager (Hide / Edit / Delete)
        item {
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLedgerManager = !showLedgerManager }
                    .testTag("toggle_finance_ledger_manager"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ငွေစာရင်း မှတ်တမ်းများ (Edit / Hide / Delete)",
                    color = ChampionshipGold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showLedgerManager) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle ledger manager",
                    tint = ChampionshipGold
                )
            }
        }

        if (showLedgerManager) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = ledgerSubTab == "requests",
                        onClick = { ledgerSubTab = "requests" },
                        label = { Text("ငွေသွင်း/ထုတ် (${visibleRequests.size})") }
                    )
                    if (isOwnerAdmin) {
                        FilterChip(
                            selected = ledgerSubTab == "ledger",
                            onClick = { ledgerSubTab = "ledger" },
                            label = { Text("Ledger မှတ်တမ်း (${visibleTransactions.size})") }
                        )
                    }
                    if (hiddenFinanceCount > 0 || showHiddenItems) {
                        FilterChip(
                            selected = showHiddenItems,
                            onClick = { showHiddenItems = !showHiddenItems },
                            label = { Text(if (showHiddenItems) "Hidden ပြထား ($hiddenFinanceCount)" else "Hidden ကြည့် ($hiddenFinanceCount)") },
                            modifier = Modifier.testTag("finance_show_hidden_chip")
                        )
                    }
                }
            }

            if (ledgerSubTab == "requests") {
                if (visibleRequests.isEmpty()) {
                    item {
                        Text("ငွေသွင်း/ထုတ် မှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE))
                    }
                } else {
                    items(visibleRequests, key = { "wr_${it.id}" }) { req ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (req.isHidden) Color(0xFFE5E7EB) else PearlCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (req.isHidden) {
                                            Surface(color = AmberPendingSoft, shape = RoundedCornerShape(6.dp)) {
                                                Text("HIDDEN", color = AmberPending, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        StatusBadge(req.status)
                                        IconButton(onClick = {
                                            onEditWalletRequest(req.id, req.amountKs, req.netAmountKs, req.channel, req.referenceNote, req.status, req.adminNote, !req.isHidden, null)
                                        }) {
                                            Icon(if (req.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Toggle Hide Wallet Request", tint = InkMuted)
                                        }
                                        if (isOwnerAdmin) {
                                            IconButton(onClick = { editingWalletReq = req }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Wallet Request", tint = PitchEmeraldDark)
                                            }
                                            IconButton(onClick = { deletingWalletReq = req }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Wallet Request", tint = AdminCrimson)
                                            }
                                        }
                                    }
                                }
                                Text("${formatKs(req.amountKs)} (Net: ${formatKs(req.netAmountKs)}) • ${req.channel}", color = PitchEmeraldDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                                Text("Ref: ${req.referenceNote} • ${formatTimestamp(req.createdAt)}", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                if (req.receiptImageBase64.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ReceiptImageThumbnail(
                                            base64 = req.receiptImageBase64,
                                            onClick = { zoomingReceiptBase64 = req.receiptImageBase64 },
                                            size = 48.dp
                                        )
                                        Text(
                                            text = "ပြေစာဓာတ်ပုံ ကြည့်ရန်",
                                            color = PitchEmeraldDark,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (isOwnerAdmin && transactions.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = { showClearAllTxConfirm = true },
                            enabled = !busy,
                            border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_all_ledger_transactions_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Ledger အားလုံး ဖျက်မည် (Clear All • ${transactions.size})",
                                color = AdminCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (visibleTransactions.isEmpty()) {
                    item {
                        Text("Ledger ငွေစာရင်း မှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE))
                    }
                } else {
                    items(visibleTransactions.take(60), key = { "tx_${it.id}" }) { tx ->
                        val custName = usersById[tx.userId]?.displayName?.ifBlank { null } ?: tx.userId.take(8)
                        val isCredit = tx.direction == "credit"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (tx.isHidden) Color(0xFFE5E7EB) else PearlCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(tx.title, color = InkDark, fontWeight = FontWeight.Bold)
                                        if (tx.isHidden) {
                                            Surface(color = AmberPendingSoft, shape = RoundedCornerShape(6.dp)) {
                                                Text("HIDDEN", color = AmberPending, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Text("$custName • ${formatTimestamp(tx.createdAt)}", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "${if (isCredit) "+" else "-"}${formatKs(tx.amountKs)} (Bal: ${formatKs(tx.balanceAfterKs)})",
                                        color = if (isCredit) PitchEmeraldDark else AdminCrimson,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        onEditTransaction(tx.id, tx.title, tx.amountKs, tx.direction, !tx.isHidden)
                                    }) {
                                        Icon(if (tx.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Toggle Hide Transaction", tint = InkMuted)
                                    }
                                    if (isOwnerAdmin) {
                                        IconButton(onClick = { editingTx = tx }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Transaction", tint = PitchEmeraldDark)
                                        }
                                        IconButton(onClick = { deletingTx = tx }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Transaction", tint = AdminCrimson)
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

    if (showCreateAccountDialog) {
        PaymentAccountDialog(
            initial = null,
            onDismiss = { showCreateAccountDialog = false },
            onSave = { provider, name, number, note, active ->
                showCreateAccountDialog = false
                onSavePaymentAccount(null, provider, name, number, note, active)
            }
        )
    }

    editingAccount?.let { acc ->
        PaymentAccountDialog(
            initial = acc,
            onDismiss = { editingAccount = null },
            onSave = { provider, name, number, note, active ->
                editingAccount = null
                onSavePaymentAccount(acc.id, provider, name, number, note, active)
            }
        )
    }

    editingWalletReq?.let { req ->
        EditWalletRequestDialog(
            req = req,
            onDismiss = { editingWalletReq = null },
            onSave = { amt, net, ch, ref, st, note, hid, receiptB64 ->
                val id = req.id
                editingWalletReq = null
                onEditWalletRequest(id, amt, net, ch, ref, st, note, hid, receiptB64)
            }
        )
    }

    zoomingReceiptBase64?.let { b64 ->
        FullScreenReceiptDialog(
            base64 = b64,
            onDismiss = { zoomingReceiptBase64 = null }
        )
    }

    deletingWalletReq?.let { req ->
        ConfirmDeleteDialog(
            title = "ငွေသွင်း/ထုတ် မှတ်တမ်း ဖျက်မည်လား?",
            message = "${req.customerName} (${formatKs(req.amountKs)}) မှတ်တမ်းကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingWalletReq = null },
            onConfirm = {
                val id = req.id
                deletingWalletReq = null
                onDeleteWalletRequest(id)
            }
        )
    }

    editingTx?.let { tx ->
        EditLedgerTransactionDialog(
            tx = tx,
            onDismiss = { editingTx = null },
            onSave = { title, amt, dir, hid ->
                val id = tx.id
                editingTx = null
                onEditTransaction(id, title, amt, dir, hid)
            }
        )
    }

    deletingTx?.let { tx ->
        ConfirmDeleteDialog(
            title = "ငွေစာရင်း မှတ်တမ်း ဖျက်မည်လား?",
            message = "${tx.title} (${formatKs(tx.amountKs)}) မှတ်တမ်းကို ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingTx = null },
            onConfirm = {
                val id = tx.id
                deletingTx = null
                onDeleteTransaction(id)
            }
        )
    }

    if (showClearAllTxConfirm) {
        ConfirmDeleteDialog(
            title = "Ledger မှတ်တမ်းအားလုံး ဖျက်မည်လား?",
            message = "Ledger ငွေစာရင်း မှတ်တမ်း ${transactions.size} ခုလုံးကို အပြီးတိုင် ဖျက်ပစ်ပါမည်။",
            onDismiss = { showClearAllTxConfirm = false },
            onConfirm = {
                showClearAllTxConfirm = false
                onClearAllTransactions(transactions.map { it.id })
            }
        )
    }
}

@Composable
private fun PaymentAccountDialog(
    initial: PaymentAccount?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    var provider by remember { mutableStateOf(initial?.providerName ?: "KBZPay") }
    var accountName by remember { mutableStateOf(initial?.accountName ?: "") }
    var accountNumber by remember { mutableStateOf(initial?.accountNumber ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "ငွေလက်ခံအကောင့် ထည့်ရန်" else "ငွေလက်ခံအကောင့် ပြင်ရန်") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("ဝန်ဆောင်မှု (KBZPay / WavePay / CB)") }, singleLine = true)
                OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("အကောင့်အမည်") }, singleLine = true)
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("ဖုန်း / ဘဏ်အကောင့်နံပါတ်") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("မှတ်ချက်") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("အသုံးပြုခွင့် ဖွင့်မည်")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(provider, accountName, accountNumber, note, active) },
                enabled = provider.isNotBlank() && accountName.isNotBlank() && accountNumber.isNotBlank()
            ) {
                Text("သိမ်းဆည်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSubAdminsTab(
    currentUserId: String,
    admins: List<AdminMember>,
    invites: List<AdminInvite>,
    supportAgents: List<SupportAgentProfile> = emptyList(),
    paymentAccounts: List<PaymentAccount> = emptyList(),
    appDisplayName: String = "လိုတရ",
    apkDownloadUrl: String = AppUpdateConfig.DEFAULT_APK_DOWNLOAD_URL,
    onCreateInvite: (label: String, assignedEmail: String, initialPin: String, permissions: AdminPermissions) -> Unit,
    onToggleInvite: (String, Boolean) -> Unit,
    onEditInvite: (
        oldCode: String,
        newCode: String,
        label: String,
        active: Boolean,
        assignedEmail: String,
        newPin: String?,
        permissions: AdminPermissions
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteInvite: (String) -> Unit = {},
    onUpdateAdminPermissions: (String, String, AdminPermissions) -> Unit,
    onSetAdminStatus: (String, String, Boolean) -> Unit,
    onResetAdminPin: (String, String, String) -> Unit,
    onForceLogoutAdmin: (String, String) -> Unit,
    onAllocateAdminFloat: (targetAdminUid: String, adminName: String, deltaKs: Long) -> Unit = { _, _, _ -> },
    onRemoveAdmin: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val adminPortalLink = remember { ApkDownloadHelper.buildAdminPortalLink() }
    val customerShareLink = remember(apkDownloadUrl) { ApkDownloadHelper.buildCustomerShareLink(apkDownloadUrl) }
    var showCreateCard by remember { mutableStateOf(true) }
    var inviteLabel by remember { mutableStateOf("") }
    var assignedEmail by remember { mutableStateOf("") }
    var initialPin by remember { mutableStateOf("") }
    var selectedPermissions by remember { mutableStateOf(AdminPermissions.PRESET_WALLET_CASHIER) }

    var editingPermissionsMember by remember { mutableStateOf<AdminMember?>(null) }
    var resettingPinMember by remember { mutableStateOf<AdminMember?>(null) }
    var floatAdjustMember by remember { mutableStateOf<AdminMember?>(null) }
    var editingInvite by remember { mutableStateOf<AdminInvite?>(null) }
    var deletingInvite by remember { mutableStateOf<AdminInvite?>(null) }

    val agentsByUid = remember(supportAgents) { supportAgents.associateBy { it.adminUid } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2920)),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Admin Link နှင့် Share Link စီမံခန့်ခွဲမှု",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Admin Portal Link: $adminPortalLink",
                        color = Color.White,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(adminPortalLink))
                                Toast.makeText(context, "Admin Link ကူးယူပြီးပါပြီ ($adminPortalLink)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("copy_admin_portal_link_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Admin Link ကူး", color = ChampionshipGold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                val msg = "$appDisplayName Admin Portal Link\n$adminPortalLink\n(Admin အကောင့်ဖြင့် ဝင်ထားပြီးဖြစ်ပါက နှိပ်ရုံဖြင့် Admin Dashboard သို့ တန်းဝင်နိုင်ပါသည်။)"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "$appDisplayName Admin Link")
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Admin Link"))
                            },
                            modifier = Modifier.weight(1f).testTag("share_admin_portal_link_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Admin Link ရှယ်", color = Color(0xFF34D399), maxLines = 1)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF1F473A))
                    Text(
                        "Customer Share Link (${ApkDownloadHelper.CUSTOMER_SHARE_DEEP_LINK}): $customerShareLink",
                        color = Color(0xFFD7EAE1),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val text = ApkDownloadHelper.buildApkShareText(appDisplayName, customerShareLink)
                                clipboard.setText(AnnotatedString(text))
                                Toast.makeText(context, "Customer Share Link ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("copy_customer_share_link_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share Link ကူး", color = Color.White, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                val text = ApkDownloadHelper.buildApkShareText(appDisplayName, customerShareLink)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "$appDisplayName Share Link")
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Customer App Link"))
                            },
                            modifier = Modifier.weight(1f).testTag("share_customer_share_link_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share Link ရှယ်", color = ChampionshipGold, maxLines = 1)
                        }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showCreateCard = !showCreateCard },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Second Admin အသစ် ဖိတ်ခေါ် & ခွင့်ပြုချက် သတ်မှတ်ရန်",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (showCreateCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle create invite",
                            tint = ChampionshipGold
                        )
                    }

                    if (showCreateCard) {
                        OutlinedTextField(
                            value = inviteLabel,
                            onValueChange = { inviteLabel = it },
                            label = { Text("Second Admin အမည် / ရာထူး") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().testTag("invite_label_input")
                        )
                        OutlinedTextField(
                            value = assignedEmail,
                            onValueChange = { assignedEmail = it },
                            label = { Text("ကန့်သတ် Gmail (Optional - မထည့်လည်းရ)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().testTag("invite_email_input")
                        )
                        OutlinedTextField(
                            value = initialPin,
                            onValueChange = { initialPin = it.filter { c -> c.isDigit() }.take(8) },
                            label = { Text("ကနဦး 2FA PIN (Optional, 4-8 digits)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth().testTag("invite_pin_input")
                        )

                        Text("အသင့်သုံး Preset များ:", color = ChampionshipGold, style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedPermissions == AdminPermissions.PRESET_WALLET_CASHIER,
                                onClick = { selectedPermissions = AdminPermissions.PRESET_WALLET_CASHIER },
                                label = { Text("Wallet Cashier") }
                            )
                            FilterChip(
                                selected = selectedPermissions == AdminPermissions.PRESET_ORDER_OPERATOR,
                                onClick = { selectedPermissions = AdminPermissions.PRESET_ORDER_OPERATOR },
                                label = { Text("Order Operator") }
                            )
                            FilterChip(
                                selected = selectedPermissions == AdminPermissions.PRESET_SUPPORT_ONLY,
                                onClick = { selectedPermissions = AdminPermissions.PRESET_SUPPORT_ONLY },
                                label = { Text("Support Staff") }
                            )
                            FilterChip(
                                selected = selectedPermissions == AdminPermissions.ALL_GRANTED,
                                onClick = { selectedPermissions = AdminPermissions.ALL_GRANTED },
                                label = { Text("Full Access") }
                            )
                        }

                        PermissionCheckboxesBlock(
                            permissions = selectedPermissions,
                            onChange = { selectedPermissions = it },
                            textColor = Color.White
                        )

                        Button(
                            onClick = {
                                onCreateInvite(
                                    inviteLabel.ifBlank { "Second Admin" },
                                    assignedEmail.trim(),
                                    initialPin.trim(),
                                    selectedPermissions
                                )
                                inviteLabel = ""
                                assignedEmail = ""
                                initialPin = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier.fillMaxWidth().testTag("create_invite_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Second Admin Invite Code ထုတ်မည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (invites.isNotEmpty()) {
            item {
                Text("Invite Codes (${invites.size})", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
            }
            items(invites, key = { it.code }) { inv ->
                Card(colors = CardDefaults.cardColors(containerColor = PearlCard), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(inv.code, color = InkDark, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                            Text(
                                text = inv.label + if (inv.assignedEmail.isNotBlank()) " • ${inv.assignedEmail}" else "",
                                color = InkMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = ApkDownloadHelper.buildAdminInviteLink(inv.code),
                                color = PitchEmeraldDark,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val inviteUrl = ApkDownloadHelper.buildAdminInviteLink(inv.code)
                                    clipboard.setText(AnnotatedString(inviteUrl))
                                    Toast.makeText(context, "1-Tap Admin Link ကူးပြီး: $inviteUrl", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_invite_link_${inv.code}")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Admin Invite Link", tint = PitchEmeraldDark)
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            ApkDownloadHelper.buildAdminInviteShareText(
                                                appName = appDisplayName,
                                                code = inv.code,
                                                label = inv.label,
                                                rawApkUrl = customerShareLink
                                            )
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Second Admin Invite"))
                                },
                                modifier = Modifier.testTag("share_invite_link_${inv.code}")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Invite Link", tint = PitchEmeraldDark)
                            }
                            IconButton(
                                onClick = { editingInvite = inv },
                                modifier = Modifier.testTag("edit_invite_${inv.code}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Invite Code", tint = PitchEmeraldDark)
                            }
                            IconButton(
                                onClick = { deletingInvite = inv },
                                modifier = Modifier.testTag("delete_invite_${inv.code}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Invite Code", tint = AdminCrimson)
                            }
                            Switch(checked = inv.active, onCheckedChange = { onToggleInvite(inv.code, it) })
                        }
                    }
                }
            }
        }

        item {
            Text("Admin အဖွဲ့ဝင်များ (${admins.size})", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium)
        }
        items(admins, key = { it.userId }) { member ->
            val perms = member.resolvedPermissions(member.role == "owner")
            val isSuspended = member.status == "suspended"
            val agent = agentsByUid[member.userId]
            val memberAccounts = remember(paymentAccounts, member.userId) {
                paymentAccounts.filter { it.ownerAdminId == member.userId }
            }
            Card(colors = CardDefaults.cardColors(containerColor = PearlCard), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.displayName.ifBlank { "Admin" }, color = InkDark, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${member.role.uppercase()} • ${member.email.ifBlank { member.inviteCodeUsed }}",
                                color = InkMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "ချိတ်ဆက်မှု: ${if (isSuspended) "Suspended" else "ချိတ်ဆက်ပြီး (Active)"} • ဘဏ်/Pay: ${memberAccounts.size} ခု • Support: ${if (agent?.active == true) "ON" else "OFF"}",
                                color = if (isSuspended) AdminCrimson else PitchEmeraldDark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (agent != null && (agent.phone.isNotBlank() || agent.messagingHandle.isNotBlank() || agent.paymentSummary.isNotBlank())) {
                                val contactLine = listOfNotNull(
                                    agent.phone.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
                                    agent.messagingHandle.takeIf { it.isNotBlank() },
                                    agent.paymentSummary.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                Text(contactLine, color = InkMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = {
                                    val summary = buildString {
                                        appendLine("${member.displayName.ifBlank { "Admin" }} (${member.role.uppercase()})")
                                        if (member.email.isNotBlank()) appendLine("Email: ${member.email}")
                                        if (agent?.phone?.isNotBlank() == true) appendLine("Phone: ${agent.phone}")
                                        if (agent?.messagingHandle?.isNotBlank() == true) appendLine("Contact: ${agent.messagingHandle}")
                                        appendLine("Portal: $adminPortalLink")
                                    }.trim()
                                    clipboard.setText(AnnotatedString(summary))
                                    Toast.makeText(context, "${member.displayName} ချိတ်ဆက်အချက်အလက် Copy ကူးပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_admin_member_contact_${member.userId}")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Admin Contact", tint = PitchEmeraldDark)
                            }
                            StatusBadge(if (member.role == "owner") "active" else member.status)
                        }
                    }

                    val activeTags = buildList {
                        if (perms.canApproveWallet) add("Wallet")
                        if (perms.canManageOrders) add("Orders")
                        if (perms.canViewUsers) add("Users")
                        if (perms.canLockUsers) add("Lock Users")
                        if (perms.canManageBoard) add("Board")
                        if (perms.canManage2D3DRoom) add("2D/3D Room")
                        if (perms.canManageBoardSheet) add("Sheet Table")
                        if (perms.canChat) add("Chat")
                        if (perms.canEditWalletRates) add("Wallet Rates")
                        if (perms.canEditGameConfig) add("Game Settings")
                        if (perms.canViewAllLogs) add("All Logs")
                        if (perms.canAdjustBalance) add("Balance +/-")
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        activeTags.forEach { tag ->
                            Surface(color = Color(0xFFE6F4EE), shape = RoundedCornerShape(6.dp)) {
                                Text(tag, color = PitchEmeraldDark, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    if (member.role != "owner") {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Float ငွေလက်ကျန်", color = InkDark, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = formatKs(member.walletBalanceKs),
                                        color = if (member.walletBalanceKs > 0L) PitchEmeraldDark else AdminCrimson,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "ခွဲဝေငွေ: ${formatKs(member.totalAllocatedKs)} • ငွေသွင်းပေး: ${formatKs(member.totalTopupsGivenKs)} • ငွေထုတ်လက်ခံ: ${formatKs(member.totalWithdrawalsTakenKs)}",
                                    color = InkMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                        }
                    }

                    if (member.role != "owner" && member.userId != currentUserId) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { floatAdjustMember = member },
                                modifier = Modifier.testTag("allocate_float_${member.userId}")
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PitchEmeraldDark, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Float ငွေထည့်/နှုတ်", color = PitchEmeraldDark)
                            }
                            OutlinedButton(onClick = { editingPermissionsMember = member }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ခွင့်ပြုချက်")
                            }
                            OutlinedButton(onClick = { onSetAdminStatus(member.userId, member.displayName, !isSuspended) }) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isSuspended) "ပြန်ဖွင့်" else "Suspend", color = AdminCrimson)
                            }
                            OutlinedButton(onClick = { resettingPinMember = member }) {
                                Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PIN")
                            }
                            OutlinedButton(onClick = { onForceLogoutAdmin(member.userId, member.displayName) }) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Force Out", color = AdminCrimson)
                            }
                            IconButton(onClick = { onRemoveAdmin(member.userId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Admin", tint = AdminCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    editingPermissionsMember?.let { member ->
        var draft by remember(member.userId) { mutableStateOf(member.resolvedPermissions(false)) }
        AlertDialog(
            onDismissRequest = { editingPermissionsMember = null },
            title = { Text("${member.displayName} ခွင့်ပြုချက်များ ပြင်ရန်") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PermissionCheckboxesBlock(
                        permissions = draft,
                        onChange = { draft = it },
                        textColor = InkDark
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val targetUid = member.userId
                    val targetName = member.displayName
                    editingPermissionsMember = null
                    onUpdateAdminPermissions(targetUid, targetName, draft)
                }) {
                    Text("သိမ်းဆည်းမည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPermissionsMember = null }) { Text("ပိတ်မည်") }
            }
        )
    }

    resettingPinMember?.let { member ->
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resettingPinMember = null },
            title = { Text("${member.displayName} PIN ပြန်သတ်မှတ်ရန်") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("PIN အသစ် (4-8 digits, blank = clear)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val uid = member.userId
                    val name = member.displayName
                    resettingPinMember = null
                    onResetAdminPin(uid, name, newPin)
                }) {
                    Text("သိမ်းမည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { resettingPinMember = null }) { Text("ပိတ်မည်") }
            }
        )
    }

    editingInvite?.let { inv ->
        EditAdminInviteDialog(
            invite = inv,
            onDismiss = { editingInvite = null },
            onSave = { oldCode, newCode, label, active, email, newPin, perms ->
                editingInvite = null
                onEditInvite(oldCode, newCode, label, active, email, newPin, perms)
            }
        )
    }

    deletingInvite?.let { inv ->
        ConfirmDeleteDialog(
            title = "Second Admin Invite Code ဖျက်မည်လား?",
            message = "${inv.code} (${inv.label}) ကို အပြီးတိုင် ဖျက်ပစ်ပါမည်။",
            onDismiss = { deletingInvite = null },
            onConfirm = {
                val c = inv.code
                deletingInvite = null
                onDeleteInvite(c)
            }
        )
    }

    floatAdjustMember?.let { member ->
        var amountInput by remember { mutableStateOf("100000") }
        var isAdd by remember { mutableStateOf(true) }
        val amtVal = amountInput.toLongOrNull() ?: 0L
        AlertDialog(
            onDismissRequest = { floatAdjustMember = null },
            title = { Text("${member.displayName} Float ငွေထည့်/နှုတ်") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("လက်ရှိ Float လက်ကျန်: ${formatKs(member.walletBalanceKs)}", fontWeight = FontWeight.Bold, color = PitchEmeraldDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isAdd, onClick = { isAdd = true }, label = { Text("+ Float ထည့်မည်") })
                        FilterChip(selected = !isAdd, onClick = { isAdd = false }, label = { Text("- ပြန်နှုတ်မည်") })
                    }
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() }.take(10) },
                        label = { Text("ပမာဏ (Ks)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("allocate_float_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = member.userId
                        val name = member.displayName.ifBlank { "Second Admin" }
                        floatAdjustMember = null
                        onAllocateAdminFloat(uid, name, if (isAdd) amtVal else -amtVal)
                    },
                    enabled = amtVal > 0L,
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                    modifier = Modifier.testTag("confirm_allocate_float_button")
                ) {
                    Text("အတည်ပြုမည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { floatAdjustMember = null }) { Text("ပိတ်မည်") }
            }
        )
    }
}

@Composable
private fun PermissionCheckboxesBlock(
    permissions: AdminPermissions,
    onChange: (AdminPermissions) -> Unit,
    textColor: Color
) {
    val rows = listOf(
        Triple("ငွေသွင်း / ငွေထုတ် အတည်ပြုခွင့်", permissions.canApproveWallet) { v: Boolean -> permissions.copy(canApproveWallet = v) },
        Triple("အော်ဒါ လက်ခံ / အလျော်အစား စီမံခွင့်", permissions.canManageOrders) { v: Boolean -> permissions.copy(canManageOrders = v) },
        Triple("Customer စာရင်း ကြည့်ရှုခွင့်", permissions.canViewUsers) { v: Boolean -> permissions.copy(canViewUsers = v) },
        Triple("Customer အကောင့် ယာယီပိတ်/ဖွင့် (Lock) ခွင့်", permissions.canLockUsers) { v: Boolean -> permissions.copy(canLockUsers = v) },
        Triple("ပွဲစဉ် / ထီ Board စီမံခွင့်", permissions.canManageBoard) { v: Boolean -> permissions.copy(canManageBoard = v) },
        Triple("2D/3D ထိန်းချုပ်ခန်း ဝင်ခွင့်", permissions.canManage2D3DRoom) { v: Boolean -> permissions.copy(canManage2D3DRoom = v) },
        Triple("Google Sheet ဇယား စီမံခွင့်", permissions.canManageBoardSheet) { v: Boolean -> permissions.copy(canManageBoardSheet = v) },
        Triple("Customer Chat / Call ဆက်သွယ်ခွင့်", permissions.canChat) { v: Boolean -> permissions.copy(canChat = v) },
        Triple("ငွေသွင်း/ထုတ် % နှုန်း ပြင်ခွင့်", permissions.canEditWalletRates) { v: Boolean -> permissions.copy(canEditWalletRates = v) },
        Triple("အနိုင်အကောက် % & Auto Settle ပြင်ခွင့်", permissions.canEditGameConfig) { v: Boolean -> permissions.copy(canEditGameConfig = v) },
        Triple("Admin အားလုံး Audit Log ကြည့်ခွင့်", permissions.canViewAllLogs) { v: Boolean -> permissions.copy(canViewAllLogs = v) },
        Triple("Customer လက်ကျန်ငွေ (+/-) ပြင်ခွင့်", permissions.canAdjustBalance) { v: Boolean -> permissions.copy(canAdjustBalance = v) }
    )
    Column {
        rows.forEach { (label, checked, updater) ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onChange(updater(!checked)) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = checked, onCheckedChange = { onChange(updater(it)) })
                Text(label, color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSystemSettingsTab(
    config: SystemGameConfig,
    busy: Boolean,
    onSaveSystemGameConfig: (SystemGameConfig) -> Unit
) {
    var enabledGames by remember { mutableStateOf(config.enabledGames) }
    var dualThreshold by remember { mutableStateOf(config.dualApprovalThresholdKs.toString()) }
    var minStake by remember { mutableStateOf(config.minStakeKs.toString()) }
    var maxStake by remember { mutableStateOf(config.maxStakeKs.toString()) }
    var twoDMult by remember { mutableStateOf(config.twoDPayoutMultiplier.toInt().toString()) }
    var threeDMult by remember { mutableStateOf(config.threeDPayoutMultiplier.toInt().toString()) }
    var diceMult by remember { mutableStateOf(config.dicePayoutMultiplier.toString()) }
    var animalsMult by remember { mutableStateOf(config.sixAnimalsPayoutMultiplier.toString()) }
    var promoText by remember { mutableStateOf(config.bonusPromoText) }
    var maintenanceMode by remember { mutableStateOf(config.maintenanceMode) }
    var maintenanceMsg by remember { mutableStateOf(config.maintenanceMessage) }
    var hideTwoD by remember { mutableStateOf(config.hideTwoD) }
    var hideThreeD by remember { mutableStateOf(config.hideThreeD) }

    var footballPaused by remember { mutableStateOf(config.footballPaused) }
    var footballReopenAtMs by remember { mutableLongStateOf(config.footballReopenAtMs) }
    var footballMinutesInput by remember { mutableStateOf("60") }

    var gamesPaused by remember { mutableStateOf(config.gamesPaused) }
    var gamesReopenAtMs by remember { mutableLongStateOf(config.gamesReopenAtMs) }
    var gamesMinutesInput by remember { mutableStateOf("60") }

    var showPauseControls by remember { mutableStateOf(true) }
    var showBasicSettings by remember { mutableStateOf(true) }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(config) {
        enabledGames = config.enabledGames
        dualThreshold = config.dualApprovalThresholdKs.toString()
        minStake = config.minStakeKs.toString()
        maxStake = config.maxStakeKs.toString()
        twoDMult = config.twoDPayoutMultiplier.toInt().toString()
        threeDMult = config.threeDPayoutMultiplier.toInt().toString()
        diceMult = config.dicePayoutMultiplier.toString()
        animalsMult = config.sixAnimalsPayoutMultiplier.toString()
        promoText = config.bonusPromoText
        maintenanceMode = config.maintenanceMode
        maintenanceMsg = config.maintenanceMessage
        hideTwoD = config.hideTwoD
        hideThreeD = config.hideThreeD
        footballPaused = config.footballPaused
        footballReopenAtMs = config.footballReopenAtMs
        gamesPaused = config.gamesPaused
        gamesReopenAtMs = config.gamesReopenAtMs
    }

    fun buildCurrentConfig(): SystemGameConfig {
        val fbActive = footballPaused && (footballReopenAtMs <= 0L || footballReopenAtMs > System.currentTimeMillis())
        val gmActive = gamesPaused && (gamesReopenAtMs <= 0L || gamesReopenAtMs > System.currentTimeMillis())
        return config.copy(
            enabledGames = enabledGames,
            dualApprovalThresholdKs = (dualThreshold.toLongOrNull() ?: 300_000L).coerceAtLeast(10_000L),
            minStakeKs = (minStake.toLongOrNull() ?: 500L).coerceAtLeast(100L),
            maxStakeKs = (maxStake.toLongOrNull() ?: 5_000_000L).coerceAtLeast(1_000L),
            twoDPayoutMultiplier = twoDMult.toDoubleOrNull() ?: 80.0,
            threeDPayoutMultiplier = threeDMult.toDoubleOrNull() ?: 550.0,
            dicePayoutMultiplier = diceMult.toDoubleOrNull() ?: 1.95,
            sixAnimalsPayoutMultiplier = animalsMult.toDoubleOrNull() ?: 2.0,
            bonusPromoText = promoText.trim(),
            maintenanceMode = maintenanceMode,
            maintenanceMessage = maintenanceMsg.trim(),
            hideTwoD = hideTwoD,
            hideThreeD = hideThreeD,
            footballPaused = fbActive,
            footballReopenAtMs = if (fbActive) footballReopenAtMs else 0L,
            gamesPaused = gmActive,
            gamesReopenAtMs = if (gmActive) gamesReopenAtMs else 0L
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HideBarSectionHeader(
                title = "ဘောလုံးပွဲ & ဂိမ်း ခေတ္တပိတ် / Auto-Open Timer",
                countBadge = when {
                    footballPaused && gamesPaused -> "၂ ခုလုံး ပိတ်"
                    footballPaused -> "ဘောလုံး ပိတ်"
                    gamesPaused -> "ဂိမ်း ပိတ်"
                    else -> "ဖွင့်ထား"
                },
                isVisible = showPauseControls,
                onToggle = { showPauseControls = !showPauseControls },
                testTag = "hide_bar_admin_pause_controls"
            )
        }

        if (showPauseControls) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                    border = BorderStroke(1.dp, AmberPending.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PauseCircle, contentDescription = null, tint = AmberPending)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "ဘောလုံးပွဲ နှင့် ဂိမ်းများ ခေတ္တပိတ် & အချိန်ပြည့် Auto-Open စနစ်",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "သတ်မှတ်အချိန်ပြည့်သည်နှင့် Customer ထံ အလိုအလျောက် လောင်းကြေးပြန်ပွင့်ပါမည်",
                                    color = Color(0xFFB4C8BE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // 1. Football Pause + Timer
                        val footballStillPaused = footballPaused && (footballReopenAtMs <= 0L || footballReopenAtMs > nowMs)
                        Surface(
                            color = Color(0xFF152921),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            "ဘောလုံးပွဲ (မောင်း/ဘော်ဒီ/1xBET/555 Mix) ခေတ္တပိတ်မည်",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (footballStillPaused) {
                                                if (footballReopenAtMs > nowMs) {
                                                    formatRemainingReopenCountdown(footballReopenAtMs, nowMs)
                                                } else {
                                                    "ခေတ္တပိတ်ထားသည် (Manual ပြန်ဖွင့်ရန်)"
                                                }
                                            } else {
                                                "လက်ရှိ ဘောလုံးပွဲများ ဖွင့်ထားပါသည်"
                                            },
                                            color = if (footballStillPaused) AmberPending else Color(0xFF6EE7B7),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = footballStillPaused,
                                        onCheckedChange = { checked ->
                                            footballPaused = checked
                                            if (!checked) footballReopenAtMs = 0L
                                        },
                                        modifier = Modifier.testTag("pause_football_switch")
                                    )
                                }

                                Text(
                                    "အလိုအလျောက် ပြန်ဖွင့်မည့်အချိန် (Auto-Open Timer) ရွေးချယ်ရန်:",
                                    color = Color(0xFFCDE2D6),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        0L to "အချိန်မရှိ (Manual)",
                                        15L to "15 မိနစ်",
                                        30L to "30 မိနစ်",
                                        60L to "1 နာရီ",
                                        120L to "2 နာရီ",
                                        360L to "6 နာရီ",
                                        720L to "12 နာရီ"
                                    ).forEach { (mins, lbl) ->
                                        FilterChip(
                                            selected = if (mins == 0L) (footballStillPaused && footballReopenAtMs == 0L)
                                            else false,
                                            onClick = {
                                                footballPaused = true
                                                footballReopenAtMs = if (mins == 0L) 0L else System.currentTimeMillis() + mins * 60_000L
                                                if (mins > 0L) footballMinutesInput = mins.toString()
                                            },
                                            label = { Text(lbl) }
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = footballMinutesInput,
                                        onValueChange = { footballMinutesInput = it.filter { c -> c.isDigit() }.take(4) },
                                        label = { Text("စိတ်ကြိုက် (မိနစ်)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val mins = (footballMinutesInput.toLongOrNull() ?: 0L).coerceAtLeast(1L)
                                            footballPaused = true
                                            footballReopenAtMs = System.currentTimeMillis() + mins * 60_000L
                                        },
                                        modifier = Modifier.testTag("apply_football_reopen_minutes")
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Timer ထားမည်", color = ChampionshipGold)
                                    }
                                }
                            }
                        }

                        // 2. Games & Lottery Pause + Timer
                        val gamesStillPaused = gamesPaused && (gamesReopenAtMs <= 0L || gamesReopenAtMs > nowMs)
                        Surface(
                            color = Color(0xFF152921),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            "ဂိမ်းနှင့် ထီ (2D/3D/Dice/6 Animals) ရပ်နားမည်",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (gamesStillPaused) {
                                                if (gamesReopenAtMs > nowMs) {
                                                    formatRemainingReopenCountdown(gamesReopenAtMs, nowMs)
                                                } else {
                                                    "ခေတ္တရပ်နားထားသည် (Manual ပြန်ဖွင့်ရန်)"
                                                }
                                            } else {
                                                "လက်ရှိ ဂိမ်းနှင့် ထီများ ဖွင့်ထားပါသည်"
                                            },
                                            color = if (gamesStillPaused) AmberPending else Color(0xFF6EE7B7),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = gamesStillPaused,
                                        onCheckedChange = { checked ->
                                            gamesPaused = checked
                                            if (!checked) gamesReopenAtMs = 0L
                                        },
                                        modifier = Modifier.testTag("pause_games_switch")
                                    )
                                }

                                Text(
                                    "အလိုအလျောက် ပြန်ဖွင့်မည့်အချိန် (Auto-Open Timer) ရွေးချယ်ရန်:",
                                    color = Color(0xFFCDE2D6),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        0L to "အချိန်မရှိ (Manual)",
                                        15L to "15 မိနစ်",
                                        30L to "30 မိနစ်",
                                        60L to "1 နာရီ",
                                        120L to "2 နာရီ",
                                        360L to "6 နာရီ",
                                        720L to "12 နာရီ"
                                    ).forEach { (mins, lbl) ->
                                        FilterChip(
                                            selected = if (mins == 0L) (gamesStillPaused && gamesReopenAtMs == 0L)
                                            else false,
                                            onClick = {
                                                gamesPaused = true
                                                gamesReopenAtMs = if (mins == 0L) 0L else System.currentTimeMillis() + mins * 60_000L
                                                if (mins > 0L) gamesMinutesInput = mins.toString()
                                            },
                                            label = { Text(lbl) }
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = gamesMinutesInput,
                                        onValueChange = { gamesMinutesInput = it.filter { c -> c.isDigit() }.take(4) },
                                        label = { Text("စိတ်ကြိုက် (မိနစ်)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val mins = (gamesMinutesInput.toLongOrNull() ?: 0L).coerceAtLeast(1L)
                                            gamesPaused = true
                                            gamesReopenAtMs = System.currentTimeMillis() + mins * 60_000L
                                        },
                                        modifier = Modifier.testTag("apply_games_reopen_minutes")
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Timer ထားမည်", color = ChampionshipGold)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onSaveSystemGameConfig(buildCurrentConfig()) },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                            modifier = Modifier.fillMaxWidth().testTag("save_pause_timer_config_button")
                        ) {
                            Text("ပိတ်/ဖွင့် Timer ဆက်တင် သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            HideBarSectionHeader(
                title = "ဂိမ်းအမျိုးအစား & စနစ်ဆိုင်ရာ ဆက်တင်များ",
                isVisible = showBasicSettings,
                onToggle = { showBasicSettings = !showBasicSettings },
                testTag = "hide_bar_admin_system_basic"
            )
        }

        if (showBasicSettings) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text("ဂိမ်းအမျိုးအစား & စနစ်ဆိုင်ရာ ဆက်တင်များ", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    val games = listOf(
                        "parlay" to "မောင်း (Parlay)",
                        "body_goal" to "ဘော်ဒီ / ဂိုးပေါင်း",
                        "two_d" to "၂လုံးထီ (2D)",
                        "three_d" to "၃လုံးထီ (3D)",
                        "dice" to "အံစာတုံး (Dice)",
                        "six_animals" to "၆ကောင်ဂျင် (6 Animals)",
                        "dragon_tiger" to "နဂါး vs ကျား (Dragon Tiger)",
                        "lucky_wheel" to "ကံစမ်းဘီး (Lucky Wheel)"
                    )
                    games.forEach { (key, label) ->
                        val active = enabledGames.contains(key)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = active,
                                onCheckedChange = { checked ->
                                    enabledGames = if (checked) (enabledGames + key).distinct()
                                    else enabledGames.filterNot { it == key }
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    OutlinedTextField(
                        value = dualThreshold,
                        onValueChange = { dualThreshold = it.filter { c -> c.isDigit() } },
                        label = { Text("Dual Approval ငွေထုတ်ပမာဏ သတ်မှတ်ချက် (Ks)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minStake,
                            onValueChange = { minStake = it.filter { c -> c.isDigit() } },
                            label = { Text("အနည်းဆုံး ထိုးကြေး") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxStake,
                            onValueChange = { maxStake = it.filter { c -> c.isDigit() } },
                            label = { Text("အများဆုံး ထိုးကြေး") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = twoDMult,
                            onValueChange = { twoDMult = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("2D အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = threeDMult,
                            onValueChange = { threeDMult = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("3D အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = diceMult,
                            onValueChange = { diceMult = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Dice အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = animalsMult,
                            onValueChange = { animalsMult = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("၆ကောင်ဂျင် အဆ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = promoText,
                        onValueChange = { promoText = it },
                        label = { Text("ဘောနပ်စ် / ပရိုမိုးရှင်း ကြေညာစာ (Optional)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Maintenance Mode (စနစ်တစ်ခုလုံး ခေတ္တပိတ်မည်)", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("ဖွင့်ထားပါက Customer များ လောင်းကြေးတင်၍ မရပါ", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = maintenanceMode, onCheckedChange = { maintenanceMode = it })
                    }

                    OutlinedTextField(
                        value = maintenanceMsg,
                        onValueChange = { maintenanceMsg = it },
                        label = { Text("Maintenance အသိပေးစာ") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("၂လုံးထီ (2D) ကို Customer Home မှ ဝှက်ထားမည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("ဖွင့်ထားပါက Customer မျက်နှာပြင်တွင် 2D ခလုတ် မမြင်ရပါ", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = hideTwoD, onCheckedChange = { hideTwoD = it }, modifier = Modifier.testTag("hide_twod_switch"))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("၃လုံးထီ (3D) ကို Customer Home မှ ဝှက်ထားမည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("ဖွင့်ထားပါက Customer မျက်နှာပြင်တွင် 3D ခလုတ် မမြင်ရပါ", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = hideThreeD, onCheckedChange = { hideThreeD = it }, modifier = Modifier.testTag("hide_threed_switch"))
                    }

                    Button(
                        onClick = { onSaveSystemGameConfig(buildCurrentConfig()) },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier.fillMaxWidth().testTag("save_system_game_config_button")
                    ) {
                        Text("စနစ်ဆက်တင်များ သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

@Composable
fun AdminSecurityControlsTab(
    securityConfig: SecurityConfig,
    currentPublicIp: String?,
    busy: Boolean,
    onRefreshIp: () -> Unit,
    onSaveSecurityConfig: (SecurityConfig) -> Unit,
    onTriggerGlobalForceLogout: () -> Unit
) {
    var requirePin by remember { mutableStateOf(securityConfig.requireAdminPin) }
    var ipWhitelistEnabled by remember { mutableStateOf(securityConfig.ipWhitelistEnabled) }
    var ipsText by remember { mutableStateOf(securityConfig.allowedIps.joinToString("\n")) }
    var hidePublicIpDisplay by remember { mutableStateOf(securityConfig.hidePublicIpDisplay) }
    var maskCustomerPiiForSubAdmins by remember { mutableStateOf(securityConfig.maskCustomerPiiForSubAdmins) }
    var screenshotProtectionEnabled by remember { mutableStateOf(securityConfig.screenshotProtectionEnabled) }
    var showForceLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(securityConfig) {
        requirePin = securityConfig.requireAdminPin
        ipWhitelistEnabled = securityConfig.ipWhitelistEnabled
        ipsText = securityConfig.allowedIps.joinToString("\n")
        hidePublicIpDisplay = securityConfig.hidePublicIpDisplay
        maskCustomerPiiForSubAdmins = securityConfig.maskCustomerPiiForSubAdmins
        screenshotProtectionEnabled = securityConfig.screenshotProtectionEnabled
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text("လုံခြုံရေး ထိန်းချုပ်မှု (Security & Privacy)", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Admin 2FA PIN စစ်ဆေးမှု ဖွင့်မည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Second Admin များ Admin Panel ဝင်တိုင်း PIN ထည့်ရပါမည်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = requirePin, onCheckedChange = { requirePin = it })
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Screenshot & Screen Record ကာကွယ်မှု (FLAG_SECURE)", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("App မျက်နှာပြင်အား Screenshot/Record ရိုက်ယူခြင်းမှ ကာကွယ်ပေးမည်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = screenshotProtectionEnabled,
                            onCheckedChange = { screenshotProtectionEnabled = it },
                            modifier = Modifier.testTag("security_screenshot_protection_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Second Admin များထံ Customer Email/UID ဖုံးကွယ်မည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Second Admin Dashboard တွင် Customer Email/UID ကို Mask လုပ်ထားပါမည်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = maskCustomerPiiForSubAdmins,
                            onCheckedChange = { maskCustomerPiiForSubAdmins = it },
                            modifier = Modifier.testTag("security_mask_pii_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Public IP ဖော်ပြမှု ဖုံးကွယ်မည် (Privacy Mode)", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("IP Whitelist မသုံးချိန်တွင် စက်၏ Public IP မဖော်ပြဘဲ ဖုံးကွယ်ထားမည်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = hidePublicIpDisplay,
                            onCheckedChange = { hidePublicIpDisplay = it },
                            modifier = Modifier.testTag("security_hide_ip_switch")
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("IP Whitelist ကန့်သတ်မည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("သတ်မှတ်ထားသော IP မှသာ Second Admin ဝင်ခွင့်ပြုမည်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = ipWhitelistEnabled, onCheckedChange = { ipWhitelistEnabled = it })
                    }

                    if (hidePublicIpDisplay && !ipWhitelistEnabled) {
                        Text(
                            "IP ဖုံးကွယ်ထားသည် (Privacy Mode ON)",
                            color = Color(0xFF34D399),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "သင့်လက်ရှိ Public IP: ${currentPublicIp ?: "Checking..."}",
                                color = ChampionshipGold,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = onRefreshIp) { Text("Refresh", color = ChampionshipGold) }
                                if (!currentPublicIp.isNullOrBlank()) {
                                    OutlinedButton(onClick = {
                                        val existing = ipsText.lines().map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                        if (!existing.contains(currentPublicIp)) existing.add(currentPublicIp)
                                        ipsText = existing.joinToString("\n")
                                    }) {
                                        Text("+ My IP", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ipsText,
                        onValueChange = { ipsText = it },
                        label = { Text("ခွင့်ပြုထားသော IP များ (တစ်ကြောင်းလျှင် ၁ ခု)") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val list = ipsText.lines().map { it.trim() }.filter { it.isNotBlank() }.distinct()
                            onSaveSecurityConfig(
                                securityConfig.copy(
                                    requireAdminPin = requirePin,
                                    ipWhitelistEnabled = ipWhitelistEnabled,
                                    allowedIps = list,
                                    hidePublicIpDisplay = hidePublicIpDisplay,
                                    maskCustomerPiiForSubAdmins = maskCustomerPiiForSubAdmins,
                                    screenshotProtectionEnabled = screenshotProtectionEnabled
                                )
                            )
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold, contentColor = InkDark),
                        modifier = Modifier.fillMaxWidth().testTag("save_security_config_button")
                    ) {
                        Text("လုံခြုံရေး ဆက်တင် သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3E1216)),
                border = BorderStroke(1.dp, AdminCrimson),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFCA5A5))
                        Spacer(Modifier.width(8.dp))
                        Text("Emergency Session Control", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Second Admin နှင့် Customer အားလုံး၏ လက်ရှိ Session များကို ချက်ချင်း Force Logout လုပ်နိုင်ပါသည်။",
                        color = Color(0xFFFECACA),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showForceLogoutConfirm = true },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().testTag("global_force_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Session အားလုံး Force Logout လုပ်မည်", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showForceLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showForceLogoutConfirm = false },
            title = { Text("Session အားလုံး Force Logout လုပ်မည်လား?") },
            text = { Text("Main Admin မှလွဲ၍ လက်ရှိ Login ဝင်ထားသော Second Admin နှင့် Customer အားလုံး အလိုအလျောက် Log out ထွက်သွားပါမည်။") },
            confirmButton = {
                Button(
                    onClick = {
                        showForceLogoutConfirm = false
                        onTriggerGlobalForceLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson, contentColor = Color.White)
                ) {
                    Text("Force Logout လုပ်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForceLogoutConfirm = false }) { Text("ပိတ်မည်") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAuditLogsTab(
    logs: List<AuditLogEntry>,
    isOwnerOrCanViewAll: Boolean,
    isOwnerAdmin: Boolean = false,
    currentUserId: String = "",
    onDeleteAuditLog: (String) -> Unit = {},
    onClearAllAuditLogs: (List<String>) -> Unit = {}
) {
    var categoryFilter by remember { mutableStateOf("all") }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, categoryFilter) {
        if (categoryFilter == "all") logs
        else logs.filter { it.category == categoryFilter }
    }
    val deletableIds = remember(logs, isOwnerAdmin, currentUserId) {
        logs.filter { isOwnerAdmin || (currentUserId.isNotBlank() && it.actorId == currentUserId) }
            .map { it.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 640.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isOwnerOrCanViewAll) "Audit Log (Admin အားလုံး မှတ်တမ်း)"
                            else "မိမိ၏ လုပ်ဆောင်ချက် မှတ်တမ်း (Personal Log)",
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("all", "order", "wallet", "board", "user", "admin", "security", "settings").forEach { cat ->
                            FilterChip(
                                selected = categoryFilter == cat,
                                onClick = { categoryFilter = cat },
                                label = { Text(if (cat == "all") "All (${logs.size})" else cat) }
                            )
                        }
                    }
                    if (deletableIds.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearAllConfirm = true },
                            border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth().testTag("clear_all_audit_logs_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = AdminCrimson, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOwnerAdmin) "Audit မှတ်တမ်းအားလုံး ရှင်းလင်းမည် (${deletableIds.size})"
                                else "မိမိမှတ်တမ်းအားလုံး ရှင်းလင်းမည် (${deletableIds.size})",
                                color = AdminCrimson
                            )
                        }
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Text("လုပ်ဆောင်ချက် မှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(filteredLogs, key = { it.id }) { entry ->
                val canDeleteEntry = isOwnerAdmin || (currentUserId.isNotBlank() && entry.actorId == currentUserId)
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
                            Text(entry.action, color = InkDark, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Color(0xFFE2E8F0), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = entry.category,
                                        color = InkDark,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (canDeleteEntry) {
                                    IconButton(
                                        onClick = { onDeleteAuditLog(entry.id) },
                                        modifier = Modifier.testTag("delete_audit_log_${entry.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Audit Log", tint = AdminCrimson)
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${entry.actorName} (${entry.actorRole}) • ${formatTimestamp(entry.createdAt)}",
                            color = PitchEmeraldDark,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (entry.targetName.isNotBlank() || entry.details.isNotBlank()) {
                            Text(
                                text = listOf(entry.targetName, entry.details).filter { it.isNotBlank() }.joinToString(" • "),
                                color = InkMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearAllConfirm) {
        ConfirmDeleteDialog(
            title = if (isOwnerAdmin) "Audit မှတ်တမ်းအားလုံး ဖျက်မည်လား?" else "မိမိ Audit မှတ်တမ်းများ ဖျက်မည်လား?",
            message = "မှတ်တမ်း ${deletableIds.size} ခုလုံးကို အပြီးတိုင် ဖျက်ပစ်ပါမည်။",
            onDismiss = { showClearAllConfirm = false },
            onConfirm = {
                showClearAllConfirm = false
                onClearAllAuditLogs(deletableIds)
            }
        )
    }
}
