package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountWallet
import com.example.data.model.AssetItem
import com.example.data.model.DebtItem
import com.example.util.ChartDataHelper
import com.example.util.ChartPeriod
import com.example.ui.dialogs.EditDebtDialog
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.SegmentAccountBlue
import com.example.ui.theme.SegmentAssetGreen
import com.example.ui.theme.SegmentDebtRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TrophyYellow
import com.example.ui.theme.TrophyYellowLight
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyHelper
import com.example.util.IndonesianInstitutionHelper
import com.example.util.InstitutionLogoBadge
import com.example.util.AssetLogoBadge
import com.example.util.ThousandsSeparatorVisualTransformation
import com.example.util.AssetIconHelper
import com.example.util.availableAssetCategories

@Composable
fun AccountScreen(
    viewModel: FinanceViewModel,
    onAddWalletClick: () -> Unit,
    onAddAssetClick: () -> Unit,
    onAddDebtClick: () -> Unit,
    onPayDebtClick: (DebtItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedWalletIdForDetail by viewModel.selectedWalletIdForDetail.collectAsState()
    val selectedAssetIdForDetail by viewModel.selectedAssetIdForDetail.collectAsState()
    var debtToEdit by remember { mutableStateOf<DebtItem?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtItem?>(null) }

    if (selectedWalletIdForDetail != null) {
        AccountDetailScreen(
            walletId = selectedWalletIdForDetail!!,
            viewModel = viewModel,
            onBack = { viewModel.selectWalletForDetail(null) }
        )
        return
    }

    if (selectedAssetIdForDetail != null) {
        AssetDetailScreen(
            assetId = selectedAssetIdForDetail!!,
            viewModel = viewModel,
            onBack = { viewModel.selectAssetForDetail(null) }
        )
        return
    }

    val subTab by viewModel.accountSubTab.collectAsState()
    val totalWalletBalance by viewModel.totalAllWalletsBalance.collectAsState()
    val totalAssets by viewModel.totalAssetsValue.collectAsState()
    val totalDebts by viewModel.totalDebtsValue.collectAsState()
    val netWorth by viewModel.netWorthValue.collectAsState()

    val wallets by viewModel.wallets.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var selectedTimePeriod by remember { mutableStateOf("1M") }
    var selectedAssetCategory by remember { mutableStateOf("all") }

    var showAccountOptionsDialog by remember { mutableStateOf(false) }
    var showRetirementPlannerDialog by remember { mutableStateOf(false) }
    var accountSortOrder by remember { mutableStateOf("DEFAULT") } // "DEFAULT", "HIGH", "LOW", "NAME"
    var hideZeroBalanceWallets by remember { mutableStateOf(false) }
    var walletViewMode by remember { mutableStateOf("grid") } // "grid" (kotak-kotak) or "list" (daftar)
    var assetViewMode by remember { mutableStateOf("grid") }  // "grid" (kotak-kotak) or "list" (daftar)

    val displayedWallets = remember(wallets, accountSortOrder, hideZeroBalanceWallets) {
        var list = if (hideZeroBalanceWallets) wallets.filter { it.balance > 0 } else wallets
        when (accountSortOrder) {
            "HIGH" -> list.sortedByDescending { it.balance }
            "LOW" -> list.sortedBy { it.balance }
            "NAME" -> list.sortedBy { it.name }
            else -> list
        }
    }

    val displayedAssets = remember(assets, selectedAssetCategory) {
        if (selectedAssetCategory == "all") assets
        else assets.filter {
            val cat = selectedAssetCategory.lowercase()
            val t = it.assetType.lowercase()
            val n = it.name.lowercase()
            val p = it.platform.lowercase()
            when (cat) {
                "saham" -> t.contains("saham") || n.contains("saham") || p.contains("stock") || n.contains("bbca") || n.contains("bbri")
                "emas" -> t.contains("emas") || n.contains("emas") || t.contains("gold") || p.contains("antam") || p.contains("pegadaian")
                "reksadana" -> t.contains("reksa") || n.contains("reksa") || p.contains("bibit") || p.contains("bareksa")
                "kripto" -> t.contains("kripto") || t.contains("crypto") || n.contains("btc") || n.contains("bitcoin") || p.contains("indodax") || p.contains("tokocrypto")
                "obligasi" -> t.contains("obligasi") || n.contains("sbn") || t.contains("bond")
                "deposito" -> t.contains("deposito") || t.contains("deposit")
                "properti" -> t.contains("properti") || n.contains("rumah") || n.contains("tanah")
                "bank" -> it.group == "Tunai & Bank" || t.contains("bank")
                else -> true
            }
        }
    }

    val currentChartPeriod = when (selectedTimePeriod) {
        "7D" -> ChartPeriod.SEVEN_DAYS
        "3M" -> ChartPeriod.THREE_MONTHS
        "YTD" -> ChartPeriod.ONE_YEAR
        "ALL" -> ChartPeriod.ALL
        else -> ChartPeriod.ONE_MONTH
    }

    val chartData = remember(wallets, assets, debts, transactions, subTab, currentChartPeriod) {
        ChartDataHelper.getNetWorthTrend(wallets, assets, debts, transactions, subTab, currentChartPeriod)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Akun",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            IconButton(
                onClick = { showAccountOptionsDialog = true },
                modifier = Modifier.testTag("btn_account_tune")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Pengaturan & Urutan Akun",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Top Metric & Chart based on Sub-Tab
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val delta = chartData.deltaAmount
                    val isPositive = chartData.isPositive
                    val sign = if (delta >= 0) "+" else "-"
                    val absDelta = Math.abs(delta)
                    val formattedDelta = "${sign}${CurrencyHelper.formatRupiah(absDelta)}"
                    val formattedPercent = "${sign}${String.format(java.util.Locale.US, "%.1f", Math.abs(chartData.percentChange))}%"
                    val trendColor = if (isPositive) IncomeGreen else ExpenseRed
                    val trendIcon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

                    when (subTab) {
                        0 -> {
                            // Nilai Bersih
                            Text(
                                text = CurrencyHelper.formatRupiah(netWorth),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = trendIcon,
                                    contentDescription = null,
                                    tint = trendColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$formattedDelta  $formattedPercent  $selectedTimePeriod",
                                    fontSize = 12.sp,
                                    color = trendColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Nilai Bersih", fontSize = 13.sp, color = TextMuted)
                            Text(
                                text = "📈 Proyeksi saldo sesuai transaksi: ${CurrencyHelper.formatRupiah(netWorth + delta)}",
                                fontSize = 11.sp,
                                color = TextSubtle
                            )
                        }
                        1 -> {
                            // Akun / Dompet
                            Text(
                                text = CurrencyHelper.formatRupiah(totalWalletBalance),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = trendIcon,
                                    contentDescription = null,
                                    tint = trendColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$formattedDelta  $formattedPercent  $selectedTimePeriod",
                                    fontSize = 12.sp,
                                    color = trendColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total Saldo Tersedia · ${wallets.size} Dompet",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                        2 -> {
                            // Aset
                            Text(
                                text = CurrencyHelper.formatRupiah(totalAssets),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = trendIcon,
                                    contentDescription = null,
                                    tint = trendColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$formattedDelta  $formattedPercent  $selectedTimePeriod",
                                    fontSize = 12.sp,
                                    color = trendColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Total Nilai Aset", fontSize = 13.sp, color = TextMuted)
                        }
                        3 -> {
                            // Utang
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ExpenseRed))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("DIPINJAM", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = CurrencyHelper.formatRupiah(totalDebts),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    val borrowedSign = if (totalDebts > 0) "▲" else "▼"
                                    val borrowedColor = if (totalDebts > 0) ExpenseRed else IncomeGreen
                                    Text("$borrowedSign ${CurrencyHelper.formatRupiah(totalDebts)}  $selectedTimePeriod", fontSize = 11.sp, color = borrowedColor)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(IncomeGreen))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("DIPINJAMKAN", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Rp0",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text("▼ 0.0%  $selectedTimePeriod", fontSize = 11.sp, color = IncomeGreen)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Time Period Selector
                    Row(
                        modifier = Modifier
                            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                            .padding(2.dp)
                    ) {
                        listOf("7D", "1M", "3M", "YTD", "ALL").forEach { period ->
                            val isSelected = selectedTimePeriod == period
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AccentBlue else Color.Transparent,
                                modifier = Modifier.clickable { selectedTimePeriod = period }
                            ) {
                                Text(
                                    text = period,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextMuted,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Trend Line Canvas with dynamic trend color and dashed grid
                    NetWorthTrendCanvas(
                        points = chartData.points,
                        subTab = subTab,
                        trendDirection = chartData.trendDirection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(chartData.startLabel, fontSize = 10.sp, color = TextSubtle)
                        Text(chartData.midLabel, fontSize = 10.sp, color = TextSubtle)
                        Text(chartData.endLabel, fontSize = 10.sp, color = TextSubtle)
                    }
                }
            }


            // Content based on selected Sub-Tab
            when (subTab) {
                0 -> {
                    // Nilai Bersih View
                    item {
                        // Proportional Balance, Asset, and Debt Chart Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Three Summary Columns with Proportional Mini Bars
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Total Saldo", fontSize = 11.sp, color = TextSubtle)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            CurrencyHelper.formatRupiah(totalWalletBalance),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SegmentAccountBlue
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Total Aset", fontSize = 11.sp, color = TextSubtle)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            CurrencyHelper.formatRupiah(totalAssets),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SegmentAssetGreen
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Total Utang", fontSize = 11.sp, color = TextSubtle)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "-${CurrencyHelper.formatRupiah(totalDebts)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SegmentDebtRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Calculate true mathematical proportions
                                val safeWallet = totalWalletBalance.coerceAtLeast(0.0)
                                val safeAssets = totalAssets.coerceAtLeast(0.0)
                                val safeDebts = totalDebts.coerceAtLeast(0.0)
                                val grossTotal = safeWallet + safeAssets + safeDebts

                                val wRatio = if (grossTotal > 0 && safeWallet > 0) (safeWallet / grossTotal).toFloat() else 0f
                                val aRatio = if (grossTotal > 0 && safeAssets > 0) (safeAssets / grossTotal).toFloat() else 0f
                                val dRatio = if (grossTotal > 0 && safeDebts > 0) (safeDebts / grossTotal).toFloat() else 0f

                                val wPct = if (grossTotal > 0) ((safeWallet / grossTotal) * 100).toInt() else 0
                                val aPct = if (grossTotal > 0) ((safeAssets / grossTotal) * 100).toInt() else 0
                                val dPct = if (grossTotal > 0 && safeDebts > 0) (100 - wPct - aPct).coerceAtLeast(0) else 0

                                // Consolidated Proportional Multi-Segment Bar (Full Width & Proportional)
                                if (grossTotal <= 0.0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DarkSurfaceElevated)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        if (wRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(wRatio)
                                                    .fillMaxHeight()
                                                    .background(SegmentAccountBlue)
                                            )
                                        }
                                        if (wRatio > 0f && (aRatio > 0f || dRatio > 0f)) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }
                                        if (aRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(aRatio)
                                                    .fillMaxHeight()
                                                    .background(SegmentAssetGreen)
                                            )
                                        }
                                        if (aRatio > 0f && dRatio > 0f) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }
                                        if (dRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(dRatio)
                                                    .fillMaxHeight()
                                                    .background(SegmentDebtRed)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Proportional Legend with indicators spanning full width
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SegmentAccountBlue))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Saldo $wPct%", fontSize = 11.sp, color = TextSubtle, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SegmentAssetGreen))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Aset $aPct%", fontSize = 11.sp, color = TextSubtle, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(SegmentDebtRed))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Utang $dPct%", fontSize = 11.sp, color = TextSubtle, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    // Monthly Net Worth Bar Chart Card - Dynamic and changes according to actual data
                    item {
                        val monthlyBars = remember(wallets, assets, debts, transactions) {
                            ChartDataHelper.getMonthlyNetWorthBars(wallets, assets, debts, transactions)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Riwayat Nilai Bersih",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = "Perkembangan saldo 6 bulan terakhir",
                                            fontSize = 11.sp,
                                            color = TextSubtle
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceElevated
                                    ) {
                                        Text(
                                            text = "Bulanan",
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Monthly bar chart with dashed grid lines
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                ) {
                                    // Dashed grid background
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        with(ChartDataHelper) {
                                            drawDashedChartGrid(
                                                horizontalLines = 3,
                                                verticalLines = 5,
                                                gridColor = Color(0xFF334155).copy(alpha = 0.4f),
                                                drawFrame = true
                                            )
                                        }
                                    }

                                    // Dynamic Bars
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        monthlyBars.forEach { bar ->
                                            val barColor = ChartDataHelper.getTrendColor(bar.trendDirection)
                                            val barHeight = (bar.heightRatio * 58).dp.coerceAtLeast(6.dp)

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = bar.formattedValue,
                                                    fontSize = 8.sp,
                                                    color = barColor,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(28.dp)
                                                        .height(barHeight)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(barColor)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = bar.monthName,
                                                    fontSize = 10.sp,
                                                    color = TextMuted,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val lastBar = monthlyBars.lastOrNull()
                                val prevBar = if (monthlyBars.size >= 2) monthlyBars[monthlyBars.size - 2] else null
                                val recentDelta = if (lastBar != null && prevBar != null) lastBar.value - prevBar.value else 0.0
                                val isGrowing = recentDelta >= 0
                                val trendSummaryColor = if (isGrowing) IncomeGreen else ExpenseRed

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isGrowing) IncomeGreenSoft else ExpenseRedSoft,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isGrowing) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                    contentDescription = null,
                                                    tint = trendSummaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isGrowing) "Nilai bersih sedang meningkat" else "Nilai bersih mengalami penyesuaian",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite
                                            )
                                            val deltaStr = CurrencyHelper.formatRupiah(Math.abs(recentDelta))
                                            Text(
                                                text = if (isGrowing) "+$deltaStr dibanding bulan sebelumnya" else "-$deltaStr dibanding bulan sebelumnya",
                                                fontSize = 11.sp,
                                                color = trendSummaryColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Total Utang Ratio Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = TrophyYellow,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Total Utang",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                    }
                                    Text(
                                        text = "49.6%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TrophyYellowLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { 0.496f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = TrophyYellow,
                                    trackColor = DarkSurfaceElevated
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Moderate debt ratio — manageable but watch it",
                                    fontSize = 11.sp,
                                    color = TextSubtle
                                )
                            }
                        }
                    }

                    // Combined Breakdown List
                    item {
                        Text(
                            text = "Akun  ${CurrencyHelper.formatRupiah(totalWalletBalance)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(displayedWallets) { wallet ->
                        WalletMiniRow(
                            wallet = wallet,
                            onClick = { viewModel.selectWalletForDetail(wallet.id) }
                        )
                    }

                    item {
                        Text(
                            text = "Aset  ${CurrencyHelper.formatRupiah(totalAssets)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(assets) { asset ->
                        AssetMiniRow(asset, onClick = { viewModel.selectAssetForDetail(asset.id) })
                    }

                    item {
                        Text(
                            text = "Utang  -${CurrencyHelper.formatRupiah(totalDebts)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(debts) { debt ->
                        DebtMiniRow(debt)
                    }

                    // Retirement Planner Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRetirementPlannerDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x332563EB),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Umbrella,
                                            contentDescription = null,
                                            tint = AccentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Perencana pensiun",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "Proyeksikan tabunganmu hingga usia pensiun",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Akun (Wallets) View (Kotak-kotak / Grid vs Daftar / List)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dompet (${displayedWallets.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            // View Mode Toggle (Grid / List)
                            Row(
                                modifier = Modifier
                                    .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (walletViewMode == "grid") AccentBlue else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { walletViewMode = "grid" }
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "Tampilan Kotak",
                                            tint = if (walletViewMode == "grid") Color.White else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (walletViewMode == "list") AccentBlue else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { walletViewMode = "list" }
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                                            contentDescription = "Tampilan Daftar",
                                            tint = if (walletViewMode == "list") Color.White else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (walletViewMode == "grid") {
                        // 2x2 Grid of Wallets with matching compact sizing as Asset grid
                        items(displayedWallets.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { wallet ->
                                    val percent = if (totalWalletBalance > 0) ((wallet.balance / totalWalletBalance) * 100).toInt() else 0
                                    val inst = IndonesianInstitutionHelper.resolveInstitution(wallet.name, wallet.iconName)
                                    val typeLabel = wallet.bankType.split("·").firstOrNull()?.trim() ?: wallet.bankType
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectWalletForDetail(wallet.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                        border = BorderStroke(1.dp, inst.brandColor.copy(alpha = 0.35f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                InstitutionLogoBadge(
                                                    walletName = wallet.name,
                                                    iconName = wallet.iconName,
                                                    size = 34.dp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = DarkSurfaceElevated,
                                                    border = BorderStroke(0.8.dp, DarkCardBorder)
                                                ) {
                                                    Text(
                                                        text = typeLabel,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextMuted,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = wallet.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "$percent% • $typeLabel",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AccentBlue,
                                                maxLines = 1
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = CurrencyHelper.formatRupiah(wallet.balance),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        // Daftar (List View) of Wallets
                        items(displayedWallets) { wallet ->
                            val percent = if (totalWalletBalance > 0) ((wallet.balance / totalWalletBalance) * 100).toInt() else 0
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectWalletForDetail(wallet.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        InstitutionLogoBadge(
                                            walletName = wallet.name,
                                            iconName = wallet.iconName,
                                            size = 36.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = wallet.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                            Text(
                                                text = if (wallet.lastAmount > 0) "${wallet.bankType} · ${wallet.lastCategory}" else wallet.bankType,
                                                fontSize = 11.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = CurrencyHelper.formatRupiah(wallet.balance),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                            Text(
                                                text = "$percent% dari total",
                                                fontSize = 10.sp,
                                                color = AccentBlue,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Aset View (Kotak-kotak / Grid vs Daftar / List)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daftar Aset & Investasi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            // View Mode Toggle (Grid / List)
                            Row(
                                modifier = Modifier
                                    .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (assetViewMode == "grid") AccentBlue else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { assetViewMode = "grid" }
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "Tampilan Kotak",
                                            tint = if (assetViewMode == "grid") Color.White else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (assetViewMode == "list") AccentBlue else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { assetViewMode = "list" }
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                                            contentDescription = "Tampilan Daftar",
                                            tint = if (assetViewMode == "list") Color.White else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Horizontal Category Selector (Semua, Saham, Emas, Reksadana, Kripto, Obligasi, dll.)
                    item {
                        val assetScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(assetScrollState)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableAssetCategories.forEach { cat ->
                                val isSelected = selectedAssetCategory == cat.id
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) cat.color else DarkSurfaceElevated,
                                    border = BorderStroke(1.dp, if (isSelected) cat.color else DarkCardBorder),
                                    modifier = Modifier.clickable { selectedAssetCategory = cat.id }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = cat.name,
                                            tint = if (isSelected) Color.White else cat.color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextWhite
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedAssetCategory == "all") {
                        val cashAssets = displayedAssets.filter { it.group == "Tunai & Bank" }
                        val investAssets = displayedAssets.filter { it.group == "Investasi" }

                        if (cashAssets.isNotEmpty()) {
                            val sumCash = cashAssets.sumOf { it.value }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SegmentAccountBlue))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tunai & Bank (${cashAssets.size})  ${CurrencyHelper.formatRupiah(sumCash)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                }
                            }
                            if (assetViewMode == "grid") {
                                items(cashAssets.chunked(2)) { pair ->
                                    AssetGridRow(pair, totalAssets, onAssetClick = { viewModel.selectAssetForDetail(it.id) })
                                }
                            } else {
                                items(cashAssets) { asset ->
                                    AssetListCard(asset, totalAssets, onAssetClick = { viewModel.selectAssetForDetail(it.id) })
                                }
                            }
                        }

                        if (investAssets.isNotEmpty()) {
                            val sumInvest = investAssets.sumOf { it.value }
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SegmentAssetGreen))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Investasi (${investAssets.size})  ${CurrencyHelper.formatRupiah(sumInvest)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                }
                            }
                            if (assetViewMode == "grid") {
                                items(investAssets.chunked(2)) { pair ->
                                    AssetGridRow(pair, totalAssets, onAssetClick = { viewModel.selectAssetForDetail(it.id) })
                                }
                            } else {
                                items(investAssets) { asset ->
                                    AssetListCard(asset, totalAssets, onAssetClick = { viewModel.selectAssetForDetail(it.id) })
                                }
                            }
                        }
                    } else {
                        val sumFiltered = displayedAssets.sumOf { it.value }
                        val activeCatObj = availableAssetCategories.find { it.id == selectedAssetCategory }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(activeCatObj?.color ?: SegmentAssetGreen))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeCatObj?.name ?: "Aset"} (${displayedAssets.size})  ${CurrencyHelper.formatRupiah(sumFiltered)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                            }
                        }
                        if (assetViewMode == "grid") {
                            items(displayedAssets.chunked(2)) { pair ->
                                AssetGridRow(pair, totalAssets, onAssetClick = { viewModel.selectAssetForDetail(it.id) })
                            }
                        } else {
                            items(displayedAssets) { asset ->
                                AssetListCard(asset, totalAssets, onAssetClick = { viewModel.selectAssetForDetail(it.id) })
                            }
                        }
                    }
                }
                3 -> {
                    // Utang List View
                    item {
                        Text(
                            text = "Daftar Pinjaman / Utang (${debts.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(debts) { debt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { debtToEdit = debt },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = ExpenseRedSoft,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.CreditCard,
                                                    contentDescription = null,
                                                    tint = ExpenseRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = debt.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                            Text(
                                                text = "Tempo: ${debt.dueDate}",
                                                fontSize = 11.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyHelper.formatRupiah(debt.totalAmount - debt.paidAmount),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = "${debt.progressPercent}% lunas",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (debt.progressPercent > 50) IncomeGreen else ExpenseRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { (debt.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = IncomeGreen,
                                    trackColor = DarkSurfaceElevated
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { debtToEdit = debt },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Utang",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { debtToDelete = debt },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Utang",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DarkSurfaceElevated,
                                        modifier = Modifier.clickable { onPayDebtClick(debt) }
                                    ) {
                                        Text(
                                            text = "Bayar Cicilan >",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Fixed Sub-Tabs Toolbar: Nilai Bersih, Akun, Aset, Utang (Lebih pendek & teks di bawah icon ukuran kecil)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth(0.60f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subTabs = listOf(
                        Triple(0, "Nilai Bersih", Icons.Default.ShowChart to Color(0xFF3B82F6)),
                        Triple(1, "Akun", Icons.Default.Wallet to Color(0xFF06B6D4)),
                        Triple(2, "Aset", Icons.Default.Diamond to Color(0xFFF59E0B)),
                        Triple(3, "Utang", Icons.Default.Handshake to Color(0xFFEF4444))
                    )

                    subTabs.forEach { (index, title, iconAndColor) ->
                        val (icon, tabColor) = iconAndColor
                        val isSelected = subTab == index
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) tabColor.copy(alpha = 0.22f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, tabColor.copy(alpha = 0.65f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setAccountSubTab(index) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 3.dp, horizontal = 1.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) tabColor else TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = title,
                                    fontSize = 8.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Debt Dialog
        debtToEdit?.let { debt ->
            EditDebtDialog(
                debt = debt,
                viewModel = viewModel,
                onDismiss = { debtToEdit = null },
                onDelete = {
                    debtToDelete = debt
                    debtToEdit = null
                }
            )
        }

        // Delete Debt Confirmation Dialog
        debtToDelete?.let { debt ->
            AlertDialog(
                onDismissRequest = { debtToDelete = null },
                containerColor = DarkSurface,
                title = { Text("Hapus Utang?", color = TextWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Apakah Anda yakin ingin menghapus data utang \"${debt.name}\"?",
                        color = TextMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteDebt(debt.id)
                            debtToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { debtToDelete = null }) {
                        Text("Batal", color = TextSubtle)
                    }
                }
            )
        }

        // Account Options Dialog (Sort & Filter)
        if (showAccountOptionsDialog) {
            AccountOptionsDialog(
                currentSort = accountSortOrder,
                hideZero = hideZeroBalanceWallets,
                onApply = { sort, hideZero ->
                    accountSortOrder = sort
                    hideZeroBalanceWallets = hideZero
                    showAccountOptionsDialog = false
                },
                onDismiss = { showAccountOptionsDialog = false }
            )
        }

        // Retirement Planner Dialog
        if (showRetirementPlannerDialog) {
            RetirementPlannerDialog(
                currentNetWorth = netWorth,
                totalAssets = totalAssets,
                onDismiss = { showRetirementPlannerDialog = false }
            )
        }
    }
}

@Composable
fun AccountOptionsDialog(
    currentSort: String,
    hideZero: Boolean,
    onApply: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }
    var hideZeroBalance by remember { mutableStateOf(hideZero) }

    val sortOptions = listOf(
        Pair("DEFAULT", "Default (Bawaan)"),
        Pair("HIGH", "Saldo Tertinggi ke Terendah"),
        Pair("LOW", "Saldo Terendah ke Tertinggi"),
        Pair("NAME", "Nama Akun (A - Z)")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengaturan Akun & Dompet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Urutkan Dompet Berdasarkan:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSubtle
                )

                sortOptions.forEach { (key, label) ->
                    val isSelected = selectedSort == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x332563EB) else DarkSurfaceElevated,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AccentBlue) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSort = key }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sembunyikan Saldo Rp0",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextWhite
                            )
                            Text(
                                text = "Hanya tampilkan dompet yang memiliki saldo",
                                fontSize = 10.sp,
                                color = TextSubtle
                            )
                        }
                        Switch(
                            checked = hideZeroBalance,
                            onCheckedChange = { hideZeroBalance = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedSort, hideZeroBalance) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Terapkan", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSubtle)
            }
        }
    )
}

@Composable
fun RetirementPlannerDialog(
    currentNetWorth: Double,
    totalAssets: Double,
    onDismiss: () -> Unit
) {
    var currentAgeText by remember { mutableStateOf("26") }
    var retirementAgeText by remember { mutableStateOf("55") }
    var monthlyExpenseText by remember { mutableStateOf("10000000") }

    val currentAge = currentAgeText.toIntOrNull() ?: 26
    val retirementAge = retirementAgeText.toIntOrNull() ?: 55
    val monthlyExpense = monthlyExpenseText.toDoubleOrNull() ?: 10_000_000.0

    val yearsLeft = (retirementAge - currentAge).coerceAtLeast(1)
    val monthsLeft = yearsLeft * 12
    val targetRetirementFund = monthlyExpense * 12 * 25 // 4% rule (25x annual expenses)
    val totalAvailable = (currentNetWorth + totalAssets).coerceAtLeast(0.0)
    val shortfall = (targetRetirementFund - totalAvailable).coerceAtLeast(0.0)
    val monthlySavingsNeeded = if (monthsLeft > 0) shortfall / monthsLeft else 0.0
    val progressPercent = if (targetRetirementFund > 0) ((totalAvailable / targetRetirementFund) * 100).toInt().coerceIn(0, 100) else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Umbrella,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Perencana Pensiun & FIRE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentAgeText,
                        onValueChange = { if (it.length <= 2) currentAgeText = it.filter { c -> c.isDigit() } },
                        label = { Text("Usia Sekarang", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkCardBorder
                        )
                    )
                    OutlinedTextField(
                        value = retirementAgeText,
                        onValueChange = { if (it.length <= 2) retirementAgeText = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Pensiun", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkCardBorder
                        )
                    )
                }

                OutlinedTextField(
                    value = monthlyExpenseText,
                    onValueChange = { if (it.length <= 11) monthlyExpenseText = it.filter { c -> c.isDigit() } },
                    label = { Text("Estimasi Pengeluaran / Bulan", fontSize = 11.sp) },
                    visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                // Results Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Dana Pensiun (Aturan 4%)", fontSize = 11.sp, color = TextSubtle)
                            Text(
                                text = CurrencyHelper.formatRupiah(targetRetirementFund),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrophyYellowLight
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aset & Tabungan Saat Ini", fontSize = 11.sp, color = TextSubtle)
                            Text(
                                text = CurrencyHelper.formatRupiah(totalAvailable),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentBlue,
                            trackColor = DarkBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kesiapan: $progressPercent%", fontSize = 11.sp, color = TextMuted)
                            Text("Waktu: $yearsLeft tahun lagi", fontSize = 11.sp, color = TextMuted)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x332563EB),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🎯 Rekomendasi Menabung:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD)
                                )
                                Text(
                                    text = "Sisihkan ${CurrencyHelper.formatRupiah(monthlySavingsNeeded)} per bulan untuk mencapai kebebasan finansial tepat waktu.",
                                    fontSize = 11.sp,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Selesai", color = Color.White)
            }
        }
    )
}

@Composable
fun WalletMiniRow(wallet: AccountWallet, onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCardSubtle,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InstitutionLogoBadge(
                    walletName = wallet.name,
                    iconName = wallet.iconName,
                    size = 24.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(wallet.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            }
            Text(
                text = CurrencyHelper.formatRupiah(wallet.balance),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
    }
}

@Composable
fun AssetMiniRow(
    asset: AssetItem,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCardSubtle,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetLogoBadge(
                    assetType = asset.assetType,
                    assetName = asset.name,
                    platform = asset.platform,
                    size = 28.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(asset.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Text(
                        text = "${asset.assetType} • ${asset.platform}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
            Text(
                text = CurrencyHelper.formatRupiah(asset.value),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
    }
}

@Composable
fun DebtMiniRow(debt: DebtItem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCardSubtle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ExpenseRedSoft,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(debt.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            }
            Text(
                text = "-${CurrencyHelper.formatRupiah(debt.totalAmount - debt.paidAmount)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed
            )
        }
    }
}

@Composable
fun AssetGridRow(
    pair: List<AssetItem>,
    totalAssets: Double,
    onAssetClick: (AssetItem) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pair.forEach { asset ->
            val percent = if (totalAssets > 0) String.format(java.util.Locale.US, "%.1f", (asset.value / totalAssets) * 100) else "0"
            val profile = AssetIconHelper.getAssetProfile(asset.assetType, asset.name, asset.platform)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAssetClick(asset) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, profile.primaryColor.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssetLogoBadge(
                            assetType = asset.assetType,
                            assetName = asset.name,
                            platform = asset.platform,
                            iconUri = asset.iconUri,
                            size = 34.dp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = asset.platform,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = asset.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "$percent% • ${asset.assetType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = profile.primaryColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        CurrencyHelper.formatRupiah(asset.value),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }
        }
        if (pair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AssetListCard(
    asset: AssetItem,
    totalAssets: Double,
    onAssetClick: (AssetItem) -> Unit = {}
) {
    val percent = if (totalAssets > 0) String.format(java.util.Locale.US, "%.1f", (asset.value / totalAssets) * 100) else "0"
    val profile = AssetIconHelper.getAssetProfile(asset.assetType, asset.name, asset.platform)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAssetClick(asset) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, profile.primaryColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AssetLogoBadge(
                    assetType = asset.assetType,
                    assetName = asset.name,
                    platform = asset.platform,
                    iconUri = asset.iconUri,
                    size = 38.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = asset.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(0.8.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = asset.platform,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = asset.assetType,
                            fontSize = 11.sp,
                            color = profile.primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyHelper.formatRupiah(asset.value),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "$percent% dari aset",
                        fontSize = 10.sp,
                        color = profile.primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun NetWorthTrendCanvas(
    points: List<Float> = emptyList(),
    subTab: Int,
    trendDirection: Int = 0,
    modifier: Modifier = Modifier
) {
    val effectivePoints = if (points.size >= 2) points else listOf(0.4f, 0.45f, 0.5f, 0.55f, 0.65f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 1. Warna: netral abu-abu, naik hijau, turun merah
        val color = ChartDataHelper.getTrendColor(trendDirection)

        // Grid garis putus-putus dalam kotak grafik
        with(ChartDataHelper) {
            drawDashedChartGrid(
                horizontalLines = 3,
                verticalLines = 4,
                gridColor = Color(0xFF334155).copy(alpha = 0.55f),
                drawFrame = true
            )
        }

        val stepX = width / (effectivePoints.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()
        fillPath.moveTo(0f, height)

        var prevX = 0f
        var prevY = 0f

        effectivePoints.forEachIndexed { i, value ->
            val x = i * stepX
            val y = height - (value * height * 0.78f).coerceAtMost(height * 0.92f).coerceAtLeast(height * 0.08f)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                val cx1 = prevX + (x - prevX) / 2f
                path.cubicTo(cx1, prevY, cx1, y, x, y)
                fillPath.cubicTo(cx1, prevY, cx1, y, x, y)
            }
            prevX = x
            prevY = y
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.05f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw endpoint glowing dot
        if (effectivePoints.isNotEmpty()) {
            drawCircle(
                color = color.copy(alpha = 0.35f),
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(prevX, prevY)
            )
            drawCircle(
                color = color,
                radius = 3.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(prevX, prevY)
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(prevX, prevY)
            )
        }
    }
}
