package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.betting.KyayFormatter
import com.example.data.betting.MatchSheetCsvCodec
import com.example.data.model.BoardConfig
import com.example.data.model.OneXBetOddsSpec
import com.example.data.model.SportMatch
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DeepPitchElevated
import com.example.ui.theme.InkDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PitchEmeraldDark

private data class SheetColumnDef(
    val letter: String,
    val header: String,
    val width: Dp
)

private val SHEET_COLUMNS = listOf(
    SheetColumnDef("A", "", 88.dp),
    SheetColumnDef("B", "အိမ်ရှင်ဂိုး", 78.dp),
    SheetColumnDef("C", "ဧည့်သည်ဂိုး", 82.dp),
    SheetColumnDef("D", "လိဂ်", 136.dp),
    SheetColumnDef("E", "အိမ်ရှင်အသင်း", 140.dp),
    SheetColumnDef("F", "ဧည့်သည်အသင်း", 140.dp),
    SheetColumnDef("G", "ကန်ချိန်", 118.dp),
    SheetColumnDef("H", "ဘော်ဒီကြေး", 104.dp),
    SheetColumnDef("I", "ဂိုးပေါင်း", 82.dp),
    SheetColumnDef("J", "W1", 68.dp),
    SheetColumnDef("K", "သရေ", 68.dp),
    SheetColumnDef("L", "W2", 68.dp),
    SheetColumnDef("M", "ဂိုးပေါ်", 72.dp),
    SheetColumnDef("N", "ဂိုးအောက်", 76.dp),
    SheetColumnDef("O", "Body Odds", 82.dp),
    SheetColumnDef("P", "Goal Odds", 82.dp)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminMatchSheetTableTab(
    matches: List<SportMatch>,
    boardConfig: BoardConfig,
    busy: Boolean,
    initialSourceFilter: String = "all",
    onSaveSheetUrl: (String) -> Unit,
    onSyncMatchesFromSheet: (String, String) -> Unit,
    onImportCsvText: (String, String) -> Unit,
    onUpdateMatchFromSheetRow: (
        match: SportMatch,
        boardSource: String,
        homeScore: Long,
        awayScore: Long,
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        win1Odds: Double,
        drawOdds: Double,
        win2Odds: Double,
        overOdds: Double,
        underOdds: Double,
        bodyOdds: Double,
        goalOdds: Double
    ) -> Unit,
    onSaveNewMatch: (
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        bodyOdds: Double,
        goalOdds: Double,
        boardSource: String,
        oneExpressKyay: String,
        fiveFiveFiveKyay: String,
        oneXBetOdds: OneXBetOddsSpec?
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val hScroll = rememberScrollState()

    var sourceFilter by remember(initialSourceFilter) { mutableStateOf(initialSourceFilter) }
    var searchQuery by remember { mutableStateOf("") }
    var sheetUrlInput by remember(boardConfig.googleSheetCsvUrl) { mutableStateOf(boardConfig.googleSheetCsvUrl) }
    var showSyncBar by remember { mutableStateOf(false) }
    var showPasteCsvDialog by remember { mutableStateOf(false) }
    var editingRowMatch by remember { mutableStateOf<SportMatch?>(null) }
    var showAddRowDialog by remember { mutableStateOf(false) }

    val filteredMatches = remember(matches, sourceFilter, searchQuery) {
        matches.filter { m ->
            val srcOk = sourceFilter == "all" || m.boardSource == sourceFilter
            val q = searchQuery.trim()
            val qOk = q.isBlank() ||
                m.homeTeam.contains(q, ignoreCase = true) ||
                m.awayTeam.contains(q, ignoreCase = true) ||
                m.league.contains(q, ignoreCase = true)
            srcOk && qOk
        }
    }

    val exported16ColCsv = remember(filteredMatches) {
        MatchSheetCsvCodec.exportToMyanmarSheetCsv(filteredMatches)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Toolbar
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepPitchElevated),
            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(
                                "Google Sheet ဇယား (A–P • 16 Columns)",
                                color = ChampionshipGold,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Row တစ်ခုချင်းနှိပ်၍ တိုက်ရိုက်ပြင်နိုင်ပြီး Auto-Sync ပွဲများ အလိုအလျောက်ပေါ်ပါမည်",
                                color = Color(0xFFB4C8BE),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showSyncBar = !showSyncBar },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sheet_table_toggle_sync_bar")
                    ) {
                        Text(if (showSyncBar) "Link ဝှက်" else "Sheet Sync", color = ChampionshipGold, style = MaterialTheme.typography.labelSmall)
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "all" to "အားလုံး (${matches.size})",
                        "one_express" to "1xBET",
                        "five_five_five" to "555 Mix",
                        "standard" to "Standard"
                    ).forEach { (key, lbl) ->
                        FilterChip(
                            selected = sourceFilter == key,
                            onClick = { sourceFilter = key },
                            label = { Text(lbl) },
                            modifier = Modifier.testTag("sheet_filter_$key")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("အသင်း / လိဂ် ရှာရန်...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampionshipGold) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("sheet_table_search_input")
                    )
                    Button(
                        onClick = { showAddRowDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark, contentColor = Color.White),
                        modifier = Modifier.height(50.dp).testTag("sheet_table_add_row_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Row သစ်", maxLines = 1)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(exported16ColCsv))
                            Toast.makeText(context, "A–P 16-Col CSV (${filteredMatches.size} Rows) Copy ကူးပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("sheet_table_copy_16col_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("A–P CSV Copy", color = ChampionshipGold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showPasteCsvDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("sheet_table_paste_csv_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CSV Paste Import", color = Color(0xFF34D399), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                if (showSyncBar) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                    OutlinedTextField(
                        value = sheetUrlInput,
                        onValueChange = { sheetUrlInput = it },
                        label = { Text("Published Google Sheet CSV URL") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sheet_table_url_input")
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSaveSheetUrl(sheetUrlInput) },
                            enabled = !busy && sheetUrlInput.isNotBlank(),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Link သိမ်း", color = ChampionshipGold)
                        }
                        Button(
                            onClick = {
                                val targetSrc = if (sourceFilter == "all") "standard" else sourceFilter
                                onSyncMatchesFromSheet(targetSrc, sheetUrlInput)
                            },
                            enabled = !busy && sheetUrlInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("sheet_table_pull_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sheet မှ ဆွဲယူ", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Spreadsheet Grid (A-P)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F1A)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sticky A-P Column Letter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScroll)
                        .background(Color(0xFF1E293B))
                ) {
                    SheetCell("#", 42.dp, Color(0xFF94A3B8), bold = true, centered = true)
                    SHEET_COLUMNS.forEach { col ->
                        SheetCell(
                            text = col.letter,
                            width = col.width,
                            textColor = if (col.letter == "P") Color(0xFFD8B4FE) else Color(0xFFE2E8F0),
                            bg = if (col.letter == "P") Color(0xFF4C1D95) else Color(0xFF1E293B),
                            bold = true,
                            centered = true
                        )
                    }
                }
                // Sticky Row 1 Myanmar/Odds Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(hScroll)
                        .background(Color(0xFF0F172A))
                ) {
                    SheetCell("1", 42.dp, Color(0xFF94A3B8), bold = true, centered = true)
                    SHEET_COLUMNS.forEach { col ->
                        SheetCell(
                            text = col.header,
                            width = col.width,
                            textColor = Color(0xFFF8FAFC),
                            bg = Color(0xFF0F172A),
                            bold = true,
                            centered = false
                        )
                    }
                }

                // Data Rows (2, 3, ...)
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredMatches, key = { _, m -> m.id }) { idx, m ->
                        val rowNum = idx + 2
                        val rowBg = if (idx % 2 == 0) Color(0xFF0B1220) else Color(0xFF111B2E)
                        val vals = listOf(
                            MatchSheetCsvCodec.formatBoardSourceDisplay(m.boardSource),
                            m.homeScore.toString(),
                            m.awayScore.toString(),
                            m.league,
                            m.homeTeam,
                            m.awayTeam,
                            m.kickoffLabel,
                            m.handicapLine,
                            m.goalLine,
                            String.format(java.util.Locale.US, "%.2f", m.win1Odds),
                            String.format(java.util.Locale.US, "%.2f", m.drawOdds),
                            String.format(java.util.Locale.US, "%.2f", m.win2Odds),
                            String.format(java.util.Locale.US, "%.2f", m.overOdds),
                            String.format(java.util.Locale.US, "%.2f", m.underOdds),
                            String.format(java.util.Locale.US, "%.2f", m.bodyOdds),
                            String.format(java.util.Locale.US, "%.2f", m.goalOdds)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(hScroll)
                                .background(rowBg)
                                .clickable { editingRowMatch = m }
                                .testTag("sheet_row_${m.id}")
                        ) {
                            SheetCell(rowNum.toString(), 42.dp, Color(0xFF94A3B8), bg = Color(0xFF1E293B), bold = true, centered = true)
                            vals.forEachIndexed { colIdx, cellText ->
                                val colDef = SHEET_COLUMNS[colIdx]
                                val cellColor = when (colIdx) {
                                    0 -> when (m.boardSource) {
                                        "one_express" -> Color(0xFF38BDF8)
                                        "five_five_five" -> Color(0xFFFBBF24)
                                        else -> Color(0xFF6EE7B7)
                                    }
                                    1, 2 -> Color(0xFFFDE68A)
                                    else -> Color(0xFFE2E8F0)
                                }
                                SheetCell(
                                    text = cellText,
                                    width = colDef.width,
                                    textColor = cellColor,
                                    bg = rowBg,
                                    bold = colIdx == 0,
                                    centered = colIdx in listOf(1, 2, 8, 9, 10, 11, 12, 13, 14, 15)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingRowMatch?.let { m ->
        SheetRowEditorDialog(
            match = m,
            onDismiss = { editingRowMatch = null },
            onSave = { boardSrc, hs, ascore, lg, ht, at, ko, hcp, gl, w1, dr, w2, ov, un, bo, go ->
                editingRowMatch = null
                onUpdateMatchFromSheetRow(m, boardSrc, hs, ascore, lg, ht, at, ko, hcp, gl, w1, dr, w2, ov, un, bo, go)
            }
        )
    }

    if (showAddRowDialog) {
        var newRowSource by remember(sourceFilter) {
            mutableStateOf(if (sourceFilter == "all") "one_express" else sourceFilter)
        }
        Column {
            AddMatchDialog(
                boardMode = newRowSource,
                onDismiss = { showAddRowDialog = false },
                onSave = { lg, ht, at, ko, hcp, gl, bo, go, oneExp, fff, xSpec ->
                    showAddRowDialog = false
                    onSaveNewMatch(lg, ht, at, ko, hcp, gl, bo, go, newRowSource, oneExp, fff, xSpec)
                }
            )
        }
    }

    if (showPasteCsvDialog) {
        var csvText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteCsvDialog = false },
            title = { Text("A–P CSV / Excel Data ထည့်သွင်းရန်") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Google Sheet သို့မဟုတ် Excel မှ Copy ကူးထားသော 16-Column (A–P) CSV/TSV ကို Paste ထည့်ပါ:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        label = { Text("CSV / TSV Paste") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("sheet_table_paste_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fallbackSrc = if (sourceFilter == "all") "standard" else sourceFilter
                        showPasteCsvDialog = false
                        onImportCsvText(fallbackSrc, csvText)
                    },
                    enabled = csvText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                    modifier = Modifier.testTag("sheet_table_paste_confirm")
                ) {
                    Text("ထည့်သွင်းမည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCsvDialog = false }) { Text("ပိတ်မည်") }
            }
        )
    }
}

@Composable
private fun SheetCell(
    text: String,
    width: Dp,
    textColor: Color,
    bg: Color = Color.Transparent,
    bold: Boolean = false,
    centered: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(36.dp)
            .background(bg)
            .border(0.5.dp, Color(0xFF334155))
            .padding(horizontal = 6.dp),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = if (centered) JetBrainsMonoFontFamily else MaterialTheme.typography.bodySmall.fontFamily,
            fontSize = 11.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
    }
}

@Composable
private fun SheetRowEditorDialog(
    match: SportMatch,
    onDismiss: () -> Unit,
    onSave: (
        boardSource: String,
        homeScore: Long,
        awayScore: Long,
        league: String,
        homeTeam: String,
        awayTeam: String,
        kickoffLabel: String,
        handicapLine: String,
        goalLine: String,
        win1Odds: Double,
        drawOdds: Double,
        win2Odds: Double,
        overOdds: Double,
        underOdds: Double,
        bodyOdds: Double,
        goalOdds: Double
    ) -> Unit
) {
    var boardSource by remember(match.id) { mutableStateOf(match.boardSource) }
    var homeScore by remember(match.id) { mutableStateOf(match.homeScore.toString()) }
    var awayScore by remember(match.id) { mutableStateOf(match.awayScore.toString()) }
    var league by remember(match.id) { mutableStateOf(match.league) }
    var homeTeam by remember(match.id) { mutableStateOf(match.homeTeam) }
    var awayTeam by remember(match.id) { mutableStateOf(match.awayTeam) }
    var kickoff by remember(match.id) { mutableStateOf(match.kickoffLabel) }
    var handicap by remember(match.id) { mutableStateOf(match.handicapLine) }
    var goalLine by remember(match.id) { mutableStateOf(match.goalLine) }
    var w1 by remember(match.id) { mutableStateOf(match.win1Odds.toString()) }
    var dr by remember(match.id) { mutableStateOf(match.drawOdds.toString()) }
    var w2 by remember(match.id) { mutableStateOf(match.win2Odds.toString()) }
    var ov by remember(match.id) { mutableStateOf(match.overOdds.toString()) }
    var un by remember(match.id) { mutableStateOf(match.underOdds.toString()) }
    var bodyOdds by remember(match.id) { mutableStateOf(match.bodyOdds.toString()) }
    var goalOdds by remember(match.id) { mutableStateOf(match.goalOdds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = PitchEmeraldDark)
                Spacer(Modifier.width(6.dp))
                Text("Sheet Row ပြင်ဆင်ရန် (A–P)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Col A: ကြေးအမျိုးအစား", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("one_express" to "1xBET", "five_five_five" to "555 Mix", "standard" to "Standard").forEach { (src, lbl) ->
                        FilterChip(
                            selected = boardSource == src,
                            onClick = { boardSource = src },
                            label = { Text(lbl) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = homeScore, onValueChange = { homeScore = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("B: အိမ်ရှင်ဂိုး") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = awayScore, onValueChange = { awayScore = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("C: ဧည့်သည်ဂိုး") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = league, onValueChange = { league = it }, label = { Text("D: လိဂ်") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = homeTeam, onValueChange = { homeTeam = it }, label = { Text("E: အိမ်ရှင်အသင်း") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = awayTeam, onValueChange = { awayTeam = it }, label = { Text("F: ဧည့်သည်အသင်း") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = kickoff, onValueChange = { kickoff = it }, label = { Text("G: ကန်ချိန်") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = handicap, onValueChange = { handicap = it }, label = { Text("H: ဘော်ဒီကြေး") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = goalLine, onValueChange = { goalLine = it }, label = { Text("I: ဂိုးပေါင်း") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = w1, onValueChange = { w1 = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("J: W1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dr, onValueChange = { dr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("K: သရေ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = w2, onValueChange = { w2 = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("L: W2") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ov, onValueChange = { ov = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("M: ဂိုးပေါ်") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = un, onValueChange = { un = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("N: ဂိုးအောက်") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bodyOdds, onValueChange = { bodyOdds = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("O: Body Odds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = goalOdds, onValueChange = { goalOdds = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("P: Goal Odds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        boardSource,
                        homeScore.toLongOrNull() ?: match.homeScore,
                        awayScore.toLongOrNull() ?: match.awayScore,
                        league.trim().ifBlank { match.league },
                        homeTeam.trim().ifBlank { match.homeTeam },
                        awayTeam.trim().ifBlank { match.awayTeam },
                        kickoff.trim(),
                        handicap.trim(),
                        goalLine.trim(),
                        w1.toDoubleOrNull() ?: match.win1Odds,
                        dr.toDoubleOrNull() ?: match.drawOdds,
                        w2.toDoubleOrNull() ?: match.win2Odds,
                        ov.toDoubleOrNull() ?: match.overOdds,
                        un.toDoubleOrNull() ?: match.underOdds,
                        bodyOdds.toDoubleOrNull() ?: KyayFormatter.LOTAYA_STANDARD_BODY_ODDS,
                        goalOdds.toDoubleOrNull() ?: KyayFormatter.LOTAYA_STANDARD_GOAL_ODDS
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PitchEmeraldDark),
                modifier = Modifier.testTag("sheet_row_save_button")
            ) {
                Text("သိမ်းဆည်းမည်")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ပိတ်မည်") }
        }
    )
}
