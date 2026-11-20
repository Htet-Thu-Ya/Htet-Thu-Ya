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
import com.example.data.model.AdminInvite
import com.example.data.model.AdminMember
import com.example.data.model.AdminPermissions
import com.example.data.model.AuditLogEntry
import com.example.data.model.BetOrder
import com.example.data.model.PaymentAccount
import com.example.data.model.SecurityConfig
import com.example.data.model.SystemGameConfig
import com.example.data.model.UserAccount
import com.example.data.model.WalletRequest
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatKs
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AdminCrimson
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkMuted
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PearlCard
import com.example.ui.theme.PitchEmeraldDark

@Composable
fun AdminFinancialOverviewTab(
    users: List<UserAccount>,
    orders: List<BetOrder>,
    walletRequests: List<WalletRequest>,
    paymentAccounts: List<PaymentAccount>,
    busy: Boolean,
    onSavePaymentAccount: (id: String?, providerName: String, accountName: String, accountNumber: String, note: String, active: Boolean) -> Unit,
    onDeletePaymentAccount: (String) -> Unit
) {
    var editingAccount by remember { mutableStateOf<PaymentAccount?>(null) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }

    val totalTopups = remember(walletRequests) {
        walletRequests.filter { it.requestType == "topup" && it.status == "approved" }.sumOf { it.amountKs }
    }
    val totalWithdrawals = remember(walletRequests) {
        walletRequests.filter { it.requestType == "withdraw" && it.status == "approved" }.sumOf { it.amountKs }
    }
    val awaitingDualCount = remember(walletRequests) {
        walletRequests.count { it.status == "awaiting_main_approval" }
    }
    val totalStake = remember(orders) { orders.filter { it.status != "rejected" }.sumOf { it.stakeKs } }
    val totalWonPayout = remember(orders) { orders.filter { it.status == "won" }.sumOf { it.actualPayoutKs } }
    val totalLostStake = remember(orders) { orders.filter { it.status == "lost" }.sumOf { it.stakeKs } }
    val totalCommission = remember(orders) { orders.filter { it.status == "won" }.sumOf { it.commissionKs } }
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
                    Text(
                        "ဘဏ္ဍာရေးနှင့် ငွေစာရင်း အပြည့်အစုံ",
                        color = ChampionshipGold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
    onCreateInvite: (label: String, assignedEmail: String, initialPin: String, permissions: AdminPermissions) -> Unit,
    onToggleInvite: (String, Boolean) -> Unit,
    onUpdateAdminPermissions: (String, String, AdminPermissions) -> Unit,
    onSetAdminStatus: (String, String, Boolean) -> Unit,
    onResetAdminPin: (String, String, String) -> Unit,
    onForceLogoutAdmin: (String, String) -> Unit,
    onRemoveAdmin: (String) -> Unit
) {
    val context = LocalContext.current
    var showCreateCard by remember { mutableStateOf(true) }
    var inviteLabel by remember { mutableStateOf("") }
    var assignedEmail by remember { mutableStateOf("") }
    var initialPin by remember { mutableStateOf("") }
    var selectedPermissions by remember { mutableStateOf(AdminPermissions.PRESET_WALLET_CASHIER) }

    var editingPermissionsMember by remember { mutableStateOf<AdminMember?>(null) }
    var resettingPinMember by remember { mutableStateOf<AdminMember?>(null) }

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
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "လိုတရ Second Admin Invite Code: ${inv.code}\nApp ထဲတွင် Login ဝင်ပြီး 'Admin Key' ခလုတ်ကိုနှိပ်၍ ဤ Code ကို ထည့်သွင်းပါ။"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Second Admin Invite"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Invite Code", tint = PitchEmeraldDark)
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
                        }
                        StatusBadge(if (member.role == "owner") "active" else member.status)
                    }

                    val activeTags = buildList {
                        if (perms.canApproveWallet) add("Wallet")
                        if (perms.canManageOrders) add("Orders")
                        if (perms.canViewUsers) add("Users")
                        if (perms.canLockUsers) add("Lock Users")
                        if (perms.canManageBoard) add("Board")
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

                    if (member.role != "owner" && member.userId != currentUserId) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        "six_animals" to "၆ကောင်ဂျင် (6 Animals)"
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
                            Text("Maintenance Mode (ဂိမ်းများ ခေတ္တပိတ်မည်)", color = Color.White, fontWeight = FontWeight.Bold)
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

                    Button(
                        onClick = {
                            onSaveSystemGameConfig(
                                config.copy(
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
                                    maintenanceMessage = maintenanceMsg.trim()
                                )
                            )
                        },
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
    var showForceLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(securityConfig) {
        requirePin = securityConfig.requireAdminPin
        ipWhitelistEnabled = securityConfig.ipWhitelistEnabled
        ipsText = securityConfig.allowedIps.joinToString("\n")
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
                        Text("လုံခြုံရေး ထိန်းချုပ်မှု (Security Controls)", color = ChampionshipGold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            Text("IP Whitelist ကန့်သတ်မည်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("သတ်မှတ်ထားသော IP မှသာ Second Admin ဝင်ခွင့်ပြုမည်", color = Color(0xFFB4C8BE), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = ipWhitelistEnabled, onCheckedChange = { ipWhitelistEnabled = it })
                    }

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
                                    allowedIps = list
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
    isOwnerOrCanViewAll: Boolean
) {
    var categoryFilter by remember { mutableStateOf("all") }

    val filteredLogs = remember(logs, categoryFilter) {
        if (categoryFilter == "all") logs
        else logs.filter { it.category == categoryFilter }
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
                            text = if (isOwnerOrCanViewAll) "Audit Log (Admin အားလုံး မှတ်တမ်း - Immutable)"
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
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Text("လုပ်ဆောင်ချက် မှတ်တမ်း မရှိသေးပါ", color = Color(0xFFB4C8BE))
            }
        } else {
            items(filteredLogs, key = { it.id }) { entry ->
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
                            Surface(color = Color(0xFFE2E8F0), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = entry.category,
                                    color = InkDark,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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
}
