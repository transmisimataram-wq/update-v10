package com.example.data.repository

import android.content.Context
import com.example.data.model.AccountWallet
import com.example.data.model.AssetItem
import com.example.data.model.AssetRecord
import com.example.data.model.BudgetCategory
import com.example.data.model.DebtItem
import com.example.data.model.DebtType
import com.example.data.model.MainCategory
import com.example.data.model.PlanSectionType
import com.example.data.model.PlannedItem
import com.example.data.model.RecurringTransaction
import com.example.data.model.SubcategoryItem
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UpcomingScheduleItem
import com.example.data.model.UserSettings
import com.example.util.TransactionMathHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(private val context: Context? = null) {

    companion object {
        private var sharedContext: Context? = null

        fun setApplicationContext(context: Context) {
            sharedContext = context.applicationContext
        }
    }

    private fun getActiveContext(): Context? = context ?: sharedContext

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _wallets = MutableStateFlow<List<AccountWallet>>(emptyList())
    val wallets: StateFlow<List<AccountWallet>> = _wallets.asStateFlow()

    private val _assets = MutableStateFlow<List<AssetItem>>(emptyList())
    val assets: StateFlow<List<AssetItem>> = _assets.asStateFlow()

    private val _debts = MutableStateFlow<List<DebtItem>>(emptyList())
    val debts: StateFlow<List<DebtItem>> = _debts.asStateFlow()

    private val _upcomingItems = MutableStateFlow<List<UpcomingScheduleItem>>(emptyList())
    val upcomingItems: StateFlow<List<UpcomingScheduleItem>> = _upcomingItems.asStateFlow()

    private val _recurringTransactions = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    val recurringTransactions: StateFlow<List<RecurringTransaction>> = _recurringTransactions.asStateFlow()

    private val _budgets = MutableStateFlow<List<BudgetCategory>>(emptyList())
    val budgets: StateFlow<List<BudgetCategory>> = _budgets.asStateFlow()

    private val _categories = MutableStateFlow<List<MainCategory>>(FinanceStorageHelper.getDefaultCategories())
    val categories: StateFlow<List<MainCategory>> = _categories.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _labels = MutableStateFlow<List<String>>(
        listOf("Investasi", "Susu Egra", "Tabungan", "Pribadi", "Keluarga")
    )
    val labels: StateFlow<List<String>> = _labels.asStateFlow()

    private val _planTitle = MutableStateFlow("Pembagian Gaji")
    val planTitle: StateFlow<String> = _planTitle.asStateFlow()

    private val _plannedItems = MutableStateFlow<List<PlannedItem>>(emptyList())
    val plannedItems: StateFlow<List<PlannedItem>> = _plannedItems.asStateFlow()

    private val _selectedWalletFilter = MutableStateFlow("Semua")
    val selectedWalletFilter: StateFlow<String> = _selectedWalletFilter.asStateFlow()

    fun selectWalletFilter(walletName: String) {
        _selectedWalletFilter.value = walletName
        saveToDisk()
    }

    init {
        val ctx = getActiveContext()
        if (ctx != null && FinanceStorageHelper.hasSavedData(ctx)) {
            val loaded = loadFromDisk(ctx)
            if (!loaded) {
                loadSampleData()
                saveToDisk(ctx)
            }
        } else {
            loadSampleData()
            if (ctx != null) {
                saveToDisk(ctx)
            }
        }
    }

    fun initContext(ctx: Context) {
        sharedContext = ctx.applicationContext
        if (FinanceStorageHelper.hasSavedData(ctx)) {
            loadFromDisk(ctx)
        } else {
            saveToDisk(ctx)
        }
    }

    private fun loadFromDisk(ctx: Context): Boolean {
        val data = FinanceStorageHelper.loadData(ctx) ?: return false
        _transactions.value = data.transactions
        _wallets.value = data.wallets
        _assets.value = data.assets
        _debts.value = data.debts
        _budgets.value = data.budgets
        _recurringTransactions.value = data.recurringTransactions
        _plannedItems.value = data.plannedItems
        _planTitle.value = data.planTitle
        _labels.value = data.labels
        _settings.value = data.settings
        _selectedWalletFilter.value = data.selectedWalletFilter
        if (data.categories.isNotEmpty()) {
            _categories.value = data.categories
        }
        return true
    }

    fun saveToDisk(ctx: Context? = getActiveContext()) {
        val c = ctx ?: return
        val data = FinanceStorageHelper.PersistentData(
            transactions = _transactions.value,
            wallets = _wallets.value,
            assets = _assets.value,
            debts = _debts.value,
            budgets = _budgets.value,
            recurringTransactions = _recurringTransactions.value,
            plannedItems = _plannedItems.value,
            planTitle = _planTitle.value,
            labels = _labels.value,
            settings = _settings.value,
            selectedWalletFilter = _selectedWalletFilter.value,
            categories = _categories.value
        )
        ioScope.launch {
            FinanceStorageHelper.saveData(c, data)
        }
    }

    fun loadSampleData() {
        val initialWallets = listOf(
            AccountWallet(
                name = "BRI Utama",
                bankType = "Bank · IDR",
                balance = 341472.0,
                lastCategory = "Other",
                lastAmount = 100000.0,
                lastActivityTime = "2 hari lalu"
            ),
            AccountWallet(
                name = "BRI Istri",
                bankType = "Bank · IDR",
                balance = 106857.0,
                lastCategory = "Market",
                lastAmount = 75000.0,
                lastActivityTime = "3 hari lalu"
            ),
            AccountWallet(
                name = "Bank Jago",
                bankType = "Bank · IDR",
                balance = 7575.0,
                lastCategory = "Transfer",
                lastAmount = 50000.0,
                lastActivityTime = "1 minggu lalu"
            ),
            AccountWallet(
                name = "RDN Bank Jago",
                bankType = "Bank · IDR",
                balance = 2323.56,
                lastCategory = "Investasi",
                lastAmount = 10000.0,
                lastActivityTime = "2 minggu lalu"
            )
        )

        val tabunganBibitId = "asset_tabungan_bibit_1"
        val initialAssets = listOf(
            AssetItem(
                name = "Saham BBRI",
                group = "Investasi",
                assetType = "Saham",
                value = 7309865.32,
                platform = "Stockbit",
                performancePercent = 14.8,
                createdAtDate = "15 Sep 2026",
                createdAtTime = "09.30",
                records = listOf(
                    AssetRecord(
                        assetId = "asset_saham_bbri",
                        tag = "Buy",
                        isIncrease = true,
                        amount = 6500000.0,
                        sourceWalletName = "BRI Utama",
                        date = "15 Sep 2026",
                        time = "09.30",
                        note = "Beli 10 lot BBRI"
                    )
                )
            ),
            AssetItem(
                name = "Tabungan Allegra bibit",
                group = "Investasi",
                assetType = "Reksadana",
                value = 4100000.0,
                platform = "Bibit",
                performancePercent = 8.5,
                createdAtDate = "1 Okt 2026",
                createdAtTime = "14.20"
            ),
            AssetItem(
                id = tabunganBibitId,
                name = "Tabungan Bibit",
                group = "Tunai & Bank",
                assetType = "Bank Account",
                value = 1200000.0,
                platform = "Bibit",
                performancePercent = 5.2,
                createdAtDate = "29 Okt 2026",
                createdAtTime = "00.15",
                records = listOf(
                    AssetRecord(
                        assetId = tabunganBibitId,
                        tag = "Buy",
                        isIncrease = true,
                        amount = 300000.0,
                        sourceWalletName = "BRI Utama",
                        date = "7 Okt 2026",
                        time = "10.09",
                        note = "tabungan ayah"
                    ),
                    AssetRecord(
                        assetId = tabunganBibitId,
                        tag = "Invest more",
                        isIncrease = true,
                        amount = 500000.0,
                        sourceWalletName = "BRI Utama",
                        date = "30 Sep 2026",
                        time = "08.45",
                        note = "Setoran awal Bibit"
                    )
                )
            ),
            AssetItem(
                name = "Emas Antam LM",
                group = "Investasi",
                assetType = "Emas",
                value = 14500000.0,
                platform = "Antam",
                performancePercent = 12.4,
                createdAtDate = "10 Okt 2026",
                createdAtTime = "11.00",
                records = listOf(
                    AssetRecord(
                        assetId = "asset_emas_antam",
                        tag = "Buy",
                        isIncrease = true,
                        amount = 13000000.0,
                        sourceWalletName = "BRI Utama",
                        date = "10 Okt 2026",
                        time = "11.00",
                        note = "Beli 10gr Emas Batangan Antam"
                    )
                )
            ),
            AssetItem(
                name = "Kripto Bitcoin",
                group = "Investasi",
                assetType = "Kripto",
                value = 8750000.0,
                platform = "Indodax",
                performancePercent = 18.2,
                createdAtDate = "5 Okt 2026",
                createdAtTime = "20.15",
                records = listOf(
                    AssetRecord(
                        assetId = "asset_kripto_btc",
                        tag = "Buy",
                        isIncrease = true,
                        amount = 7500000.0,
                        sourceWalletName = "BRI Utama",
                        date = "5 Okt 2026",
                        time = "20.15",
                        note = "Akumulasi DCA BTC"
                    )
                )
            ),
            AssetItem(
                name = "Dana Darurat",
                group = "Tunai & Bank",
                assetType = "Bank Account",
                value = 3164000.0,
                platform = "Bibit",
                performancePercent = 6.1,
                createdAtDate = "25 Sep 2026",
                createdAtTime = "11.10"
            )
        )

        val initialDebts = listOf(
            DebtItem(
                name = "Uang Allegra",
                type = DebtType.BORROWED,
                totalAmount = 7500000.0,
                paidAmount = 450000.0,
                dueDate = "1 Apr 2029"
            ),
            DebtItem(
                name = "beli BBRI",
                type = DebtType.BORROWED,
                totalAmount = 550000.0,
                paidAmount = 0.0,
                dueDate = "16 Nov 2026"
            )
        )

        val initialUpcoming = listOf(
            UpcomingScheduleItem(
                title = "Gaji Pokok",
                accountName = "BRI Utama",
                amount = 2700000.0,
                isIncome = true,
                dateLabel = "1 Nov",
                dayLabel = "Min"
            ),
            UpcomingScheduleItem(
                title = "Admin BRI Istri",
                accountName = "BRI Istri",
                amount = 8000.0,
                isIncome = false,
                dateLabel = "16 Nov",
                dayLabel = "Sen"
            ),
            UpcomingScheduleItem(
                title = "Cicilan BBRI",
                accountName = "BRI Utama",
                amount = 55000.0,
                isIncome = false,
                dateLabel = "25 Nov",
                dayLabel = "Rab"
            )
        )

        val initialTransactions = mutableListOf(
            Transaction(
                title = "Ambil Cash",
                category = "Other",
                amount = 100000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 26,
                monthIndex = 10,
                dayOfWeekName = "Sen",
                note = "Tarik tunai ATM"
            ),
            Transaction(
                title = "Admin BRI Utama",
                category = "Other",
                amount = 4000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 20,
                monthIndex = 10,
                dayOfWeekName = "Sel",
                note = "Biaya admin bulanan"
            ),
            Transaction(
                title = "rapel tambahan gaji",
                category = "Extra Income",
                amount = 289200.0,
                type = TransactionType.INCOME,
                accountName = "BRI Utama",
                dayOfMonth = 19,
                monthIndex = 10,
                dayOfWeekName = "Sen",
                note = "Bonus proyek"
            ),
            Transaction(
                title = "uang egra",
                category = "Extra Income",
                amount = 100000.0,
                type = TransactionType.INCOME,
                accountName = "BRI Utama",
                dayOfMonth = 18,
                monthIndex = 10,
                dayOfWeekName = "Min",
                note = "Pemberian teman"
            ),
            Transaction(
                title = "pulsa",
                category = "Bills & Fees",
                amount = 75890.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 18,
                monthIndex = 10,
                dayOfWeekName = "Min",
                note = "Paket internet telkomsel"
            ),
            Transaction(
                title = "Belanja Mingguan",
                category = "Market",
                amount = 300000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 17,
                monthIndex = 10,
                dayOfWeekName = "Sab",
                note = "Sayur dan buah"
            ),
            Transaction(
                title = "Biaya Admin Rekening",
                category = "Bills & Fees",
                amount = 8000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Istri",
                dayOfMonth = 16,
                monthIndex = 10,
                dayOfWeekName = "Jum"
            ),
            Transaction(
                title = "Bensin & Parkir",
                category = "Transport",
                amount = 170000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 13,
                monthIndex = 10,
                dayOfWeekName = "Sel"
            ),
            Transaction(
                title = "Makan Siang Tim",
                category = "Food & Dining",
                amount = 100000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 12,
                monthIndex = 10,
                dayOfWeekName = "Sen"
            ),
            Transaction(
                title = "Listrik PLN",
                category = "Bills & Fees",
                amount = 288000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 11,
                monthIndex = 10,
                dayOfWeekName = "Min"
            ),
            Transaction(
                title = "Kopi & Snack",
                category = "Food & Dining",
                amount = 90000.0,
                type = TransactionType.EXPENSE,
                accountName = "Bank Jago",
                dayOfMonth = 10,
                monthIndex = 10,
                dayOfWeekName = "Sab"
            ),
            Transaction(
                title = "Belanja Elektronik",
                category = "Shopping",
                amount = 629000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 8,
                monthIndex = 10,
                dayOfWeekName = "Kam"
            ),
            Transaction(
                title = "Service Motor & Oli",
                category = "Other",
                amount = 984000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 7,
                monthIndex = 10,
                dayOfWeekName = "Rab",
                note = "Hari terboros"
            ),
            Transaction(
                title = "Belanja Perlengkapan Rumah",
                category = "Shopping",
                amount = 433000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Istri",
                dayOfMonth = 6,
                monthIndex = 10,
                dayOfWeekName = "Sel"
            ),
            Transaction(
                title = "Langganan Netflix & Spotify",
                category = "Bills & Fees",
                amount = 493000.0,
                type = TransactionType.EXPENSE,
                accountName = "Bank Jago",
                dayOfMonth = 3,
                monthIndex = 10,
                dayOfWeekName = "Jum"
            ),
            Transaction(
                title = "Belanja Bulanan Supermarket",
                category = "Market",
                amount = 600000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 2,
                monthIndex = 10,
                dayOfWeekName = "Kam"
            ),
            Transaction(
                title = "Sarapan Pagi",
                category = "Food & Dining",
                amount = 100000.0,
                type = TransactionType.EXPENSE,
                accountName = "BRI Utama",
                dayOfMonth = 1,
                monthIndex = 10,
                dayOfWeekName = "Rab"
            ),
            Transaction(
                title = "Gaji Pokok Oktober",
                category = "Salary",
                amount = 5793000.0,
                type = TransactionType.INCOME,
                accountName = "BRI Utama",
                dayOfMonth = 1,
                monthIndex = 10,
                dayOfWeekName = "Rab",
                note = "Payroll perusahaan"
            ),
            Transaction(
                title = "Transfer ke Bank Jago",
                category = "Transfer",
                amount = 500000.0,
                type = TransactionType.TRANSFER,
                accountName = "BRI Utama",
                toAccountName = "Bank Jago",
                transferFee = 2500.0,
                dayOfMonth = 14,
                monthIndex = 10,
                dayOfWeekName = "Sel",
                note = "Top up saldo operasional Jago"
            ),
            Transaction(
                title = "Transfer ke Bibit Reksadana",
                category = "Asset",
                amount = 350000.0,
                type = TransactionType.TRANSFER,
                accountName = "BRI Utama",
                targetAssetId = "1",
                targetAssetName = "Bibit Reksadana",
                dayOfMonth = 9,
                monthIndex = 10,
                dayOfWeekName = "Jum",
                note = "Investasi rutin bulanan"
            )
        )

        val initialBudgets = listOf(
            BudgetCategory(category = "Makanan & Minuman", allocated = 2000000.0, spent = 1450000.0),
            BudgetCategory(category = "Bills & Fees", allocated = 1500000.0, spent = 1256890.0),
            BudgetCategory(category = "Market / Belanja", allocated = 1500000.0, spent = 1333000.0),
            BudgetCategory(category = "Transport", allocated = 800000.0, spent = 340000.0),
            BudgetCategory(category = "Other", allocated = 1200000.0, spent = 1088000.0)
        )

        val initialRecurring = listOf(
            RecurringTransaction(
                title = "Langganan Netflix & Spotify",
                amount = 186000.0,
                type = TransactionType.EXPENSE,
                category = "Bills & Fees",
                accountName = "Bank Jago",
                dayOfMonth = 3,
                note = "Autodebet langganan media",
                label = "Pribadi",
                isActive = true,
                autoDeduct = true,
                lastProcessedMonthYear = "10/2026"
            ),
            RecurringTransaction(
                title = "Listrik PLN & Token",
                amount = 288000.0,
                type = TransactionType.EXPENSE,
                category = "Bills & Fees",
                accountName = "BRI Utama",
                dayOfMonth = 11,
                note = "Listrik rumah bulanan",
                label = "Keluarga",
                isActive = true,
                autoDeduct = true,
                lastProcessedMonthYear = "10/2026"
            ),
            RecurringTransaction(
                title = "Tagihan WiFi Indihome",
                amount = 350000.0,
                type = TransactionType.EXPENSE,
                category = "Bills & Fees",
                accountName = "BRI Utama",
                dayOfMonth = 20,
                note = "Internet 50 Mbps",
                label = "Keluarga",
                isActive = true,
                autoDeduct = true,
                lastProcessedMonthYear = "10/2026"
            ),
            RecurringTransaction(
                title = "Cicilan Motor Honda",
                amount = 850000.0,
                type = TransactionType.EXPENSE,
                category = "Other",
                accountName = "BRI Utama",
                dayOfMonth = 28,
                note = "Potong otomatis setiap tgl 28",
                label = "Pribadi",
                isActive = true,
                autoDeduct = true,
                lastProcessedMonthYear = ""
            ),
            RecurringTransaction(
                title = "Gaji Pokok Karyawan",
                amount = 5793000.0,
                type = TransactionType.INCOME,
                category = "Salary",
                accountName = "BRI Utama",
                dayOfMonth = 1,
                note = "Payroll bulanan",
                label = "Pribadi",
                isActive = true,
                autoDeduct = true,
                lastProcessedMonthYear = "10/2026"
            )
        )

        val initialPlannedItems = listOf(
            // Pemasukan (Total: 5.758.000)
            PlannedItem(name = "Gaji Pokok", category = "Salary", amount = 2684000.0, section = PlanSectionType.INCOME),
            PlannedItem(name = "Tunjangan Kinerja", category = "Salary", amount = 2199000.0, section = PlanSectionType.INCOME),
            PlannedItem(name = "Uang Makan", category = "Extra Income", amount = 875000.0, section = PlanSectionType.INCOME),

            // Pengeluaran (Total: 4.150.000)
            PlannedItem(name = "Uang Belanja", category = "Food & Drink", amount = 2000000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Bensin", category = "Transport", amount = 200000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Susu", category = "Family & Personal", amount = 400000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Pampers", category = "Family & Personal", amount = 250000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Pulsa", category = "Bills & Fees", amount = 100000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Wifi", category = "Bills & Fees", amount = 100000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Cicilan Shopee", category = "Bills & Fees", amount = 300000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "PNPM", category = "Bills & Fees", amount = 200000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Jajan & Hiburan", category = "Entertainment", amount = 300000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Arisan DWP", category = "Other", amount = 50000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Kas Seganteng", category = "Other", amount = 30000.0, section = PlanSectionType.EXPENSE),
            PlannedItem(name = "Susu Dancow", category = "Food & Drink", amount = 220000.0, section = PlanSectionType.EXPENSE),

            // Rencana Tabungan (Total: 1.600.000)
            PlannedItem(name = "Tabungan", category = "", amount = 300000.0, section = PlanSectionType.SAVINGS),
            PlannedItem(name = "Dana Darurat", category = "", amount = 300000.0, section = PlanSectionType.SAVINGS),
            PlannedItem(name = "Investasi", category = "", amount = 700000.0, section = PlanSectionType.SAVINGS),
            PlannedItem(name = "Tabungan Allegra", category = "", amount = 200000.0, section = PlanSectionType.SAVINGS),
            PlannedItem(name = "BRI Egra", category = "", amount = 50000.0, section = PlanSectionType.SAVINGS),
            PlannedItem(name = "Pajak Sepeda", category = "", amount = 50000.0, section = PlanSectionType.SAVINGS)
        )

        _wallets.value = initialWallets
        _assets.value = initialAssets
        _debts.value = initialDebts
        _upcomingItems.value = initialUpcoming
        _transactions.value = initialTransactions
        _budgets.value = initialBudgets
        _recurringTransactions.value = initialRecurring
        _plannedItems.value = initialPlannedItems
    }

    fun addTransaction(tx: Transaction) {
        val cleanTx = TransactionMathHelper.sanitizeTransaction(tx)
        _transactions.update { listOf(cleanTx) + it }

        // Reactively adjust wallet balance
        _wallets.update { list ->
            list.map { wallet ->
                val isSource = wallet.name.equals(cleanTx.accountName, ignoreCase = true)
                val isDest = cleanTx.toAccountName.isNotBlank() && wallet.name.equals(cleanTx.toAccountName, ignoreCase = true)

                var newBalance = wallet.balance
                if (isSource) {
                    newBalance = TransactionMathHelper.applyTransactionToBalance(newBalance, cleanTx, isSourceAccount = true)
                }
                if (isDest && cleanTx.type == TransactionType.TRANSFER) {
                    newBalance = TransactionMathHelper.applyTransactionToBalance(newBalance, cleanTx, isSourceAccount = false)
                }

                if (isSource) {
                    wallet.copy(
                        balance = newBalance,
                        lastCategory = cleanTx.category,
                        lastAmount = cleanTx.amount,
                        lastActivityTime = "Baru saja"
                    )
                } else if (newBalance != wallet.balance) {
                    wallet.copy(
                        balance = newBalance,
                        lastCategory = "Transfer Masuk",
                        lastAmount = cleanTx.amount,
                        lastActivityTime = "Baru saja"
                    )
                } else {
                    wallet
                }
            }
        }

        // If transferred to asset, increase asset value AND record into asset's riwayat pencatatan
        if (cleanTx.type == TransactionType.TRANSFER && (cleanTx.targetAssetId.isNotBlank() || cleanTx.targetAssetName.isNotBlank())) {
            val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val monthStr = if (cleanTx.monthIndex in 1..12) monthNames[cleanTx.monthIndex] else "Okt"
            _assets.update { list ->
                list.map { asset ->
                    if (asset.id == cleanTx.targetAssetId || (cleanTx.targetAssetName.isNotBlank() && asset.name.equals(cleanTx.targetAssetName, ignoreCase = true))) {
                        val record = AssetRecord(
                            id = "tx_${cleanTx.id}",
                            assetId = asset.id,
                            tag = "Transfer",
                            isIncrease = true,
                            amount = cleanTx.amount,
                            sourceWalletName = cleanTx.accountName,
                            date = "${cleanTx.dayOfMonth} $monthStr ${cleanTx.year}",
                            time = cleanTx.time.ifBlank { "12:00" },
                            note = if (cleanTx.note.isNotBlank()) cleanTx.note else "Transfer dari ${cleanTx.accountName}"
                        )
                        asset.copy(
                            value = asset.value + cleanTx.amount,
                            records = listOf(record) + asset.records
                        )
                    } else asset
                }
            }
        }
        saveToDisk()
    }

    fun deleteTransaction(txId: String) {
        val target = _transactions.value.find { it.id == txId } ?: return
        val cleanTx = TransactionMathHelper.sanitizeTransaction(target)
        _transactions.update { it.filterNot { item -> item.id == txId } }

        // Reverse wallet balance strictly according to rules:
        // - Income deleted -> wallet balance decreases (new = old - amount)
        // - Expense deleted -> wallet balance increases back (new = old + amount)
        // - Transfer deleted -> source wallet increases back (new = old + amount + fee), destination wallet decreases (new = old - amount)
        _wallets.update { list ->
            list.map { wallet ->
                val isSource = wallet.name.equals(cleanTx.accountName, ignoreCase = true)
                val isDest = cleanTx.toAccountName.isNotBlank() && wallet.name.equals(cleanTx.toAccountName, ignoreCase = true)

                var newBal = wallet.balance
                if (isSource) {
                    newBal = TransactionMathHelper.revertTransactionFromBalance(newBal, cleanTx, isSourceAccount = true)
                }
                if (isDest && cleanTx.type == TransactionType.TRANSFER) {
                    newBal = TransactionMathHelper.revertTransactionFromBalance(newBal, cleanTx, isSourceAccount = false)
                }

                if (newBal != wallet.balance) {
                    wallet.copy(balance = newBal)
                } else {
                    wallet
                }
            }
        }

        // Reverse asset value and remove record if transferred to asset
        if (cleanTx.type == TransactionType.TRANSFER && (cleanTx.targetAssetId.isNotBlank() || cleanTx.targetAssetName.isNotBlank())) {
            val recId = "tx_${cleanTx.id}"
            val assetRecId = "asset_rec_${cleanTx.id}"
            _assets.update { list ->
                list.map { asset ->
                    if (asset.id == cleanTx.targetAssetId || (cleanTx.targetAssetName.isNotBlank() && asset.name.equals(cleanTx.targetAssetName, ignoreCase = true))) {
                        asset.copy(
                            value = (asset.value - cleanTx.amount).coerceAtLeast(0.0),
                            records = asset.records.filterNot { it.id == recId || it.id == assetRecId || it.id == cleanTx.id }
                        )
                    } else asset
                }
            }
        }
        saveToDisk()
    }

    fun updateTransaction(updatedTx: Transaction) {
        val oldTxRaw = _transactions.value.find { it.id == updatedTx.id } ?: return
        val oldTx = TransactionMathHelper.sanitizeTransaction(oldTxRaw)
        val newTx = TransactionMathHelper.sanitizeTransaction(updatedTx)

        _transactions.update { list ->
            list.map { if (it.id == updatedTx.id) newTx else it }
        }

        // Revert old effect and apply new effect on wallets
        _wallets.update { list ->
            list.map { wallet ->
                var bal = wallet.balance
                // 1. Revert old transaction effect
                if (wallet.name.equals(oldTx.accountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.revertTransactionFromBalance(bal, oldTx, isSourceAccount = true)
                }
                if (oldTx.type == TransactionType.TRANSFER && oldTx.toAccountName.isNotBlank() && wallet.name.equals(oldTx.toAccountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.revertTransactionFromBalance(bal, oldTx, isSourceAccount = false)
                }

                // 2. Apply new transaction effect
                if (wallet.name.equals(newTx.accountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.applyTransactionToBalance(bal, newTx, isSourceAccount = true)
                }
                if (newTx.type == TransactionType.TRANSFER && newTx.toAccountName.isNotBlank() && wallet.name.equals(newTx.toAccountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.applyTransactionToBalance(bal, newTx, isSourceAccount = false)
                }

                if (wallet.name.equals(newTx.accountName, ignoreCase = true)) {
                    wallet.copy(
                        balance = bal,
                        lastCategory = newTx.category,
                        lastAmount = newTx.amount,
                        lastActivityTime = "Baru saja"
                    )
                } else if (bal != wallet.balance) {
                    wallet.copy(balance = bal)
                } else {
                    wallet
                }
            }
        }

        // Revert old asset and apply new asset
        if (oldTx.type == TransactionType.TRANSFER || newTx.type == TransactionType.TRANSFER) {
            val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val monthStr = if (newTx.monthIndex in 1..12) monthNames[newTx.monthIndex] else "Okt"
            val oldRecId = "tx_${oldTx.id}"
            val newRecId = "tx_${newTx.id}"

            _assets.update { list ->
                list.map { asset ->
                    var v = asset.value
                    var recs = asset.records
                    if (oldTx.type == TransactionType.TRANSFER && (asset.id == oldTx.targetAssetId || asset.name.equals(oldTx.targetAssetName, ignoreCase = true))) {
                        v = (v - oldTx.amount).coerceAtLeast(0.0)
                        recs = recs.filterNot { it.id == oldRecId || it.id == newRecId }
                    }
                    if (newTx.type == TransactionType.TRANSFER && (asset.id == newTx.targetAssetId || asset.name.equals(newTx.targetAssetName, ignoreCase = true))) {
                        v += newTx.amount
                        val record = AssetRecord(
                            id = newRecId,
                            assetId = asset.id,
                            tag = "Transfer",
                            isIncrease = true,
                            amount = newTx.amount,
                            sourceWalletName = newTx.accountName,
                            date = "${newTx.dayOfMonth} $monthStr ${newTx.year}",
                            time = newTx.time.ifBlank { "12:00" },
                            note = if (newTx.note.isNotBlank()) newTx.note else "Transfer dari ${newTx.accountName}"
                        )
                        recs = listOf(record) + recs
                    }
                    asset.copy(value = v, records = recs)
                }
            }
        }
        saveToDisk()
    }

    fun batchDeleteTransactions(txIds: Set<String>) {
        if (txIds.isEmpty()) return
        val targets = _transactions.value.filter { it.id in txIds }.map { TransactionMathHelper.sanitizeTransaction(it) }
        _transactions.update { it.filterNot { item -> item.id in txIds } }

        // Reverse wallet balance for all deleted transactions
        _wallets.update { list ->
            list.map { wallet ->
                var bal = wallet.balance
                targets.forEach { target ->
                    val isSource = wallet.name.equals(target.accountName, ignoreCase = true)
                    val isDest = target.toAccountName.isNotBlank() && wallet.name.equals(target.toAccountName, ignoreCase = true)

                    if (isSource) {
                        bal = TransactionMathHelper.revertTransactionFromBalance(bal, target, isSourceAccount = true)
                    }
                    if (isDest && target.type == TransactionType.TRANSFER) {
                        bal = TransactionMathHelper.revertTransactionFromBalance(bal, target, isSourceAccount = false)
                    }
                }
                wallet.copy(balance = bal)
            }
        }

        // Reverse asset records for all deleted transfers to assets
        targets.filter { it.type == TransactionType.TRANSFER && (it.targetAssetId.isNotBlank() || it.targetAssetName.isNotBlank()) }.forEach { target ->
            val recId = "tx_${target.id}"
            val assetRecId = "asset_rec_${target.id}"
            _assets.update { list ->
                list.map { asset ->
                    if (asset.id == target.targetAssetId || (target.targetAssetName.isNotBlank() && asset.name.equals(target.targetAssetName, ignoreCase = true))) {
                        asset.copy(
                            value = (asset.value - target.amount).coerceAtLeast(0.0),
                            records = asset.records.filterNot { it.id == recId || it.id == assetRecId || it.id == target.id }
                        )
                    } else asset
                }
            }
        }

        saveToDisk()
    }

    fun batchUpdateCategory(txIds: Set<String>, newCategory: String, newSubcategory: String = "") {
        if (txIds.isEmpty()) return
        val matchingCat = _categories.value.find { it.name.equals(newCategory, ignoreCase = true) }
        val newType = matchingCat?.type ?: when (newCategory) {
            "Salary", "Extra Income", "Bonus", "Investasi", "Pemberian" -> TransactionType.INCOME
            "Transfer", "Transfer Antar Bank", "Asset", "Aset", "Top Up", "Top Up E-Wallet" -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        _transactions.update { list ->
            list.map { tx ->
                if (tx.id in txIds) {
                    tx.copy(category = newCategory, subcategory = newSubcategory, type = newType)
                } else tx
            }
        }
        saveToDisk()
    }

    fun batchUpdateLabel(txIds: Set<String>, newLabel: String) {
        if (txIds.isEmpty()) return
        _transactions.update { list ->
            list.map { tx ->
                if (tx.id in txIds) {
                    tx.copy(label = newLabel)
                } else tx
            }
        }
        saveToDisk()
    }

    fun addWallet(wallet: AccountWallet) {
        _wallets.update { it + wallet }
        saveToDisk()
    }

    fun updateWallet(wallet: AccountWallet) {
        _wallets.update { list -> list.map { if (it.id == wallet.id) wallet else it } }
        saveToDisk()
    }

    fun deleteWallet(walletId: String) {
        _wallets.update { list -> list.filterNot { it.id == walletId } }
        saveToDisk()
    }

    fun addAsset(asset: AssetItem) {
        _assets.update { it + asset }
        saveToDisk()
    }

    fun updateAsset(asset: AssetItem) {
        _assets.update { list -> list.map { if (it.id == asset.id) asset else it } }
        saveToDisk()
    }

    fun deleteAsset(assetId: String) {
        _assets.update { list -> list.filterNot { it.id == assetId } }
        saveToDisk()
    }

    data class ParsedDate(val day: Int, val month: Int, val year: Int, val dayOfWeek: String)

    private fun parseDateString(dateStr: String): ParsedDate {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

        val tokens = dateStr.trim().split(" ")
        val day = tokens.getOrNull(0)?.toIntOrNull() ?: currentDay
        val monthToken = tokens.getOrNull(1)?.lowercase() ?: ""
        val month = when {
            monthToken.startsWith("jan") -> 1
            monthToken.startsWith("feb") -> 2
            monthToken.startsWith("mar") -> 3
            monthToken.startsWith("apr") -> 4
            monthToken.startsWith("mei") || monthToken.startsWith("may") -> 5
            monthToken.startsWith("jun") -> 6
            monthToken.startsWith("jul") -> 7
            monthToken.startsWith("agu") || monthToken.startsWith("aug") -> 8
            monthToken.startsWith("sep") -> 9
            monthToken.startsWith("okt") || monthToken.startsWith("oct") -> 10
            monthToken.startsWith("nov") -> 11
            monthToken.startsWith("des") || monthToken.startsWith("dec") -> 12
            else -> currentMonth
        }
        val year = tokens.getOrNull(2)?.toIntOrNull() ?: currentYear
        val testCal = Calendar.getInstance().apply { set(year, month - 1, day) }
        val dayOfWeek = dayNames[(testCal.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7]
        return ParsedDate(day, month, year, dayOfWeek)
    }

    fun addAssetRecord(assetId: String, record: AssetRecord, updateWalletBalance: Boolean = true) {
        _assets.update { list ->
            list.map { asset ->
                if (asset.id == assetId) {
                    val newRecords = listOf(record) + asset.records
                    val newValue = if (record.isIncrease) asset.value + record.amount else (asset.value - record.amount).coerceAtLeast(0.0)
                    asset.copy(value = newValue, records = newRecords)
                } else asset
            }
        }
        if (updateWalletBalance && record.sourceWalletName.isNotBlank()) {
            _wallets.update { list ->
                list.map { wallet ->
                    if (wallet.name.equals(record.sourceWalletName, ignoreCase = true)) {
                        val newBal = if (record.isIncrease) wallet.balance - record.amount else wallet.balance + record.amount
                        wallet.copy(balance = newBal, lastActivityTime = "Baru saja")
                    } else wallet
                }
            }
            // Sinkronkan ke riwayat pencatatan transaksi utama (TransactionScreen)
            val targetAsset = _assets.value.find { it.id == assetId }
            val assetName = targetAsset?.name ?: "Aset"
            val parsed = parseDateString(record.date)
            val tx = Transaction(
                id = "asset_rec_${record.id}",
                title = if (record.note.isNotBlank()) record.note else "Transfer ke $assetName",
                category = "Asset",
                amount = record.amount,
                type = TransactionType.TRANSFER,
                accountName = record.sourceWalletName,
                dayOfMonth = parsed.day,
                monthIndex = parsed.month,
                year = parsed.year,
                dayOfWeekName = parsed.dayOfWeek,
                note = record.note,
                time = record.time.ifBlank { "12:00" },
                targetAssetId = assetId,
                targetAssetName = assetName
            )
            _transactions.update { list -> listOf(tx) + list.filterNot { it.id == tx.id } }
        }
        saveToDisk()
    }

    fun updateAssetRecord(assetId: String, oldRecord: AssetRecord, updatedRecord: AssetRecord, updateWalletBalance: Boolean = true) {
        _assets.update { list ->
            list.map { asset ->
                if (asset.id == assetId) {
                    val newRecords = asset.records.map { if (it.id == updatedRecord.id) updatedRecord else it }
                    val revertedValue = if (oldRecord.isIncrease) asset.value - oldRecord.amount else asset.value + oldRecord.amount
                    val finalValue = if (updatedRecord.isIncrease) revertedValue + updatedRecord.amount else (revertedValue - updatedRecord.amount).coerceAtLeast(0.0)
                    asset.copy(value = finalValue, records = newRecords)
                } else asset
            }
        }
        if (updateWalletBalance) {
            if (oldRecord.sourceWalletName.isNotBlank() || updatedRecord.sourceWalletName.isNotBlank()) {
                _wallets.update { list ->
                    list.map { wallet ->
                        var b = wallet.balance
                        if (oldRecord.sourceWalletName.isNotBlank() && wallet.name.equals(oldRecord.sourceWalletName, ignoreCase = true)) {
                            b = if (oldRecord.isIncrease) b + oldRecord.amount else b - oldRecord.amount
                        }
                        if (updatedRecord.sourceWalletName.isNotBlank() && wallet.name.equals(updatedRecord.sourceWalletName, ignoreCase = true)) {
                            b = if (updatedRecord.isIncrease) b - updatedRecord.amount else b + updatedRecord.amount
                        }
                        if (b != wallet.balance) wallet.copy(balance = b, lastActivityTime = "Baru saja") else wallet
                    }
                }
            }
            // Update juga di riwayat pencatatan transaksi utama
            val targetAsset = _assets.value.find { it.id == assetId }
            val assetName = targetAsset?.name ?: "Aset"
            if (updatedRecord.sourceWalletName.isNotBlank()) {
                val parsed = parseDateString(updatedRecord.date)
                val tx = Transaction(
                    id = "asset_rec_${updatedRecord.id}",
                    title = if (updatedRecord.note.isNotBlank()) updatedRecord.note else "Transfer ke $assetName",
                    category = "Asset",
                    amount = updatedRecord.amount,
                    type = TransactionType.TRANSFER,
                    accountName = updatedRecord.sourceWalletName,
                    dayOfMonth = parsed.day,
                    monthIndex = parsed.month,
                    year = parsed.year,
                    dayOfWeekName = parsed.dayOfWeek,
                    note = updatedRecord.note,
                    time = updatedRecord.time.ifBlank { "12:00" },
                    targetAssetId = assetId,
                    targetAssetName = assetName
                )
                _transactions.update { list ->
                    val exists = list.any { it.id == tx.id }
                    if (exists) list.map { if (it.id == tx.id) tx else it } else listOf(tx) + list
                }
            } else {
                _transactions.update { list -> list.filterNot { it.id == "asset_rec_${updatedRecord.id}" } }
            }
        }
        saveToDisk()
    }

    fun deleteAssetRecord(assetId: String, recordId: String, revertWalletBalance: Boolean = true) {
        var targetRecord: AssetRecord? = null
        _assets.update { list ->
            list.map { asset ->
                if (asset.id == assetId) {
                    targetRecord = asset.records.find { it.id == recordId }
                    val rec = targetRecord
                    val newRecords = asset.records.filterNot { it.id == recordId }
                    val finalValue = if (rec != null) {
                        if (rec.isIncrease) (asset.value - rec.amount).coerceAtLeast(0.0) else asset.value + rec.amount
                    } else asset.value
                    asset.copy(value = finalValue, records = newRecords)
                } else asset
            }
        }
        val rec = targetRecord
        if (revertWalletBalance && rec != null && rec.sourceWalletName.isNotBlank()) {
            _wallets.update { list ->
                list.map { wallet ->
                    if (wallet.name.equals(rec.sourceWalletName, ignoreCase = true)) {
                        val b = if (rec.isIncrease) wallet.balance + rec.amount else wallet.balance - rec.amount
                        wallet.copy(balance = b)
                    } else wallet
                }
            }
            // Hapus dari riwayat pencatatan transaksi utama
            _transactions.update { list ->
                list.filterNot { it.id == "asset_rec_$recordId" || it.id == "tx_$recordId" }
            }
        }
        saveToDisk()
    }

    fun addDebt(debt: DebtItem) {
        _debts.update { it + debt }
        saveToDisk()
    }

    fun updateDebt(debt: DebtItem) {
        _debts.update { list -> list.map { if (it.id == debt.id) debt else it } }
        saveToDisk()
    }

    fun payDebt(debtId: String, amount: Double) {
        _debts.update { list ->
            list.map { debt ->
                if (debt.id == debtId) {
                    val newPaid = (debt.paidAmount + amount).coerceAtMost(debt.totalAmount)
                    debt.copy(paidAmount = newPaid)
                } else debt
            }
        }
        saveToDisk()
    }

    fun deleteDebt(debtId: String) {
        _debts.update { list -> list.filterNot { it.id == debtId } }
        saveToDisk()
    }

    fun addBudget(budget: BudgetCategory) {
        _budgets.update { it + budget }
        saveToDisk()
    }

    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        saveToDisk()
    }

    fun addLabel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_labels.value.any { it.equals(trimmed, ignoreCase = true) }) {
            _labels.update { it + trimmed }
            saveToDisk()
        }
    }

    fun deleteLabel(name: String) {
        _labels.update { it.filterNot { label -> label.equals(name, ignoreCase = true) } }
        saveToDisk()
    }

    // Category and Subcategory methods
    fun addCategory(category: MainCategory) {
        _categories.update { current ->
            current + category
        }
        saveToDisk()
    }

    fun updateCategory(category: MainCategory) {
        _categories.update { current ->
            current.map { if (it.id == category.id) category else it }
        }
        saveToDisk()
    }

    fun deleteCategory(categoryId: String) {
        _categories.update { current ->
            current.filterNot { it.id == categoryId }
        }
        saveToDisk()
    }

    fun addSubcategory(categoryId: String, subcategory: SubcategoryItem) {
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(subcategories = cat.subcategories + subcategory)
                } else cat
            }
        }
        saveToDisk()
    }

    fun updateSubcategory(categoryId: String, subcategory: SubcategoryItem) {
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(subcategories = cat.subcategories.map { if (it.id == subcategory.id) subcategory else it })
                } else cat
            }
        }
        saveToDisk()
    }

    fun deleteSubcategory(categoryId: String, subcategoryId: String) {
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(subcategories = cat.subcategories.filterNot { it.id == subcategoryId })
                } else cat
            }
        }
        saveToDisk()
    }

    fun toggleCategoryVisibility(categoryId: String) {
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) cat.copy(isVisible = !cat.isVisible) else cat
            }
        }
        saveToDisk()
    }

    fun toggleSubcategoryVisibility(categoryId: String, subcategoryId: String) {
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(subcategories = cat.subcategories.map {
                        if (it.id == subcategoryId) it.copy(isVisible = !it.isVisible) else it
                    })
                } else cat
            }
        }
        saveToDisk()
    }

    fun addRecurringTransaction(
        recurring: RecurringTransaction,
        checkImmediateProcess: Boolean = true,
        currentDay: Int = 28,
        currentMonth: Int = 10,
        currentYear: Int = 2026
    ) {
        var itemToAdd = recurring
        if (checkImmediateProcess && recurring.isActive && recurring.autoDeduct && recurring.dayOfMonth <= currentDay) {
            val key = "$currentMonth/$currentYear"
            if (itemToAdd.lastProcessedMonthYear != key) {
                val dayOfWeek = getDayOfWeekName(itemToAdd.dayOfMonth, currentMonth, currentYear)
                val tx = Transaction(
                    title = itemToAdd.title,
                    category = itemToAdd.category,
                    subcategory = itemToAdd.subcategory,
                    amount = itemToAdd.amount,
                    type = itemToAdd.type,
                    accountName = itemToAdd.accountName,
                    dayOfMonth = itemToAdd.dayOfMonth,
                    monthIndex = currentMonth,
                    year = currentYear,
                    dayOfWeekName = dayOfWeek,
                    note = if (itemToAdd.note.isNotBlank()) "${itemToAdd.note} (Otomatis Rutin)" else "Otomatis dari Transaksi Berulang",
                    label = itemToAdd.label,
                    recurrence = "Bulanan (Tgl ${itemToAdd.dayOfMonth})"
                )
                addTransaction(tx)
                itemToAdd = itemToAdd.copy(lastProcessedMonthYear = key)
            }
        }
        _recurringTransactions.update { it + itemToAdd }
        saveToDisk()
    }

    fun updateRecurringTransaction(recurring: RecurringTransaction) {
        _recurringTransactions.update { list ->
            list.map { if (it.id == recurring.id) recurring else it }
        }
        saveToDisk()
    }

    fun deleteRecurringTransaction(id: String) {
        _recurringTransactions.update { it.filterNot { item -> item.id == id } }
        saveToDisk()
    }

    fun toggleRecurringActive(id: String) {
        _recurringTransactions.update { list ->
            list.map { if (it.id == id) it.copy(isActive = !it.isActive) else it }
        }
        saveToDisk()
    }

    fun processRecurringTransaction(id: String, month: Int, year: Int): Boolean {
        val recurring = _recurringTransactions.value.find { it.id == id } ?: return false
        val key = "$month/$year"
        val dayOfWeek = getDayOfWeekName(recurring.dayOfMonth, month, year)
        val tx = Transaction(
            title = recurring.title,
            category = recurring.category,
            subcategory = recurring.subcategory,
            amount = recurring.amount,
            type = recurring.type,
            accountName = recurring.accountName,
            dayOfMonth = recurring.dayOfMonth,
            monthIndex = month,
            year = year,
            dayOfWeekName = dayOfWeek,
            note = if (recurring.note.isNotBlank()) "${recurring.note} (Otomatis Rutin)" else "Otomatis dari Transaksi Berulang",
            label = recurring.label,
            recurrence = "Bulanan (Tgl ${recurring.dayOfMonth})"
        )
        addTransaction(tx)
        _recurringTransactions.update { list ->
            list.map { if (it.id == id) it.copy(lastProcessedMonthYear = key) else it }
        }
        saveToDisk()
        return true
    }

    fun checkAndExecuteDueRecurringTransactions(currentDay: Int, currentMonth: Int, currentYear: Int): Int {
        val key = "$currentMonth/$currentYear"
        var executedCount = 0
        _recurringTransactions.value.forEach { item ->
            if (item.isActive && item.autoDeduct && item.dayOfMonth <= currentDay && item.lastProcessedMonthYear != key) {
                processRecurringTransaction(item.id, currentMonth, currentYear)
                executedCount++
            }
        }
        return executedCount
    }

    private fun getDayOfWeekName(day: Int, month: Int, year: Int): String {
        val c = java.util.Calendar.getInstance()
        c.set(year, month - 1, day)
        return when (c.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "Min"
            java.util.Calendar.MONDAY -> "Sen"
            java.util.Calendar.TUESDAY -> "Sel"
            java.util.Calendar.WEDNESDAY -> "Rab"
            java.util.Calendar.THURSDAY -> "Kam"
            java.util.Calendar.FRIDAY -> "Jum"
            java.util.Calendar.SATURDAY -> "Sab"
            else -> "Sen"
        }
    }

    fun updatePlanTitle(newTitle: String) {
        _planTitle.value = newTitle
        saveToDisk()
    }

    fun addPlannedItem(item: PlannedItem) {
        _plannedItems.update { it + item }
        saveToDisk()
    }

    fun updatePlannedItem(item: PlannedItem) {
        _plannedItems.update { list -> list.map { if (it.id == item.id) item else it } }
        saveToDisk()
    }

    fun deletePlannedItem(id: String) {
        _plannedItems.update { list -> list.filterNot { it.id == id } }
        saveToDisk()
    }

    fun exportTransactionsJson(): String {
        val root = JSONObject()
        root.put("app", "Keuangan")
        root.put("version", "3.0")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        root.put("exportedAt", sdf.format(Date()))
        root.put("totalTransactions", _transactions.value.size)
        root.put("totalWallets", _wallets.value.size)
        root.put("totalAssets", _assets.value.size)
        root.put("totalDebts", _debts.value.size)
        root.put("totalCategories", _categories.value.size)
        root.put("totalPlans", _plannedItems.value.size)
        root.put("totalBudgets", _budgets.value.size)
        root.put("totalRecurring", _recurringTransactions.value.size)

        // 1. Transactions
        val txArr = JSONArray()
        _transactions.value.forEach { tx ->
            val o = JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("category", tx.category)
                put("subcategory", tx.subcategory)
                put("amount", tx.amount)
                put("type", tx.type.name)
                put("accountName", tx.accountName)
                put("dayOfMonth", tx.dayOfMonth)
                put("monthIndex", tx.monthIndex)
                put("year", tx.year)
                put("dayOfWeekName", tx.dayOfWeekName)
                put("note", tx.note)
                put("label", tx.label)
                put("recurrence", tx.recurrence)
                put("time", tx.time)
                put("toAccountName", tx.toAccountName)
                put("transferFee", tx.transferFee)
                put("targetAssetId", tx.targetAssetId)
                put("targetAssetName", tx.targetAssetName)
            }
            txArr.put(o)
        }
        root.put("transactions", txArr)

        // 2. Wallets / Akun
        val wArr = JSONArray()
        _wallets.value.forEach { w ->
            val o = JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("bankType", w.bankType)
                put("balance", w.balance)
                put("lastCategory", w.lastCategory)
                put("lastAmount", w.lastAmount)
                put("lastActivityTime", w.lastActivityTime)
                put("currency", w.currency)
                put("iconName", w.iconName)
                put("excludeFromNetWorth", w.excludeFromNetWorth)
                put("excludeFromBudget", w.excludeFromBudget)
            }
            wArr.put(o)
        }
        root.put("wallets", wArr)

        // 3. Assets
        val aArr = JSONArray()
        _assets.value.forEach { a ->
            val o = JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("group", a.group)
                put("assetType", a.assetType)
                put("value", a.value)
                put("platform", a.platform)
                put("performancePercent", a.performancePercent)
                put("currency", a.currency)
                put("createdAtDate", a.createdAtDate)
                put("createdAtTime", a.createdAtTime)
                put("valuationMethod", a.valuationMethod)

                val rArr = JSONArray()
                a.records.forEach { r ->
                    val ro = JSONObject().apply {
                        put("id", r.id)
                        put("assetId", r.assetId)
                        put("tag", r.tag)
                        put("isIncrease", r.isIncrease)
                        put("amount", r.amount)
                        put("sourceWalletName", r.sourceWalletName)
                        put("date", r.date)
                        put("time", r.time)
                        put("note", r.note)
                    }
                    rArr.put(ro)
                }
                put("records", rArr)
            }
            aArr.put(o)
        }
        root.put("assets", aArr)

        // 4. Debts
        val dArr = JSONArray()
        _debts.value.forEach { d ->
            val o = JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("type", d.type.name)
                put("totalAmount", d.totalAmount)
                put("paidAmount", d.paidAmount)
                put("dueDate", d.dueDate)
            }
            dArr.put(o)
        }
        root.put("debts", dArr)

        // 5. Budgets
        val bArr = JSONArray()
        _budgets.value.forEach { b ->
            val o = JSONObject().apply {
                put("id", b.id)
                put("category", b.category)
                put("allocated", b.allocated)
                put("spent", b.spent)
            }
            bArr.put(o)
        }
        root.put("budgets", bArr)

        // 6. Recurring Transactions
        val recArr = JSONArray()
        _recurringTransactions.value.forEach { r ->
            val o = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("amount", r.amount)
                put("type", r.type.name)
                put("category", r.category)
                put("subcategory", r.subcategory)
                put("accountName", r.accountName)
                put("dayOfMonth", r.dayOfMonth)
                put("frequency", r.frequency)
                put("startDate", r.startDate)
                put("endDate", r.endDate)
                put("note", r.note)
                put("label", r.label)
                put("isActive", r.isActive)
                put("autoDeduct", r.autoDeduct)
                put("lastProcessedMonthYear", r.lastProcessedMonthYear)
            }
            recArr.put(o)
        }
        root.put("recurringTransactions", recArr)

        // 7. Upcoming Schedules
        val upArr = JSONArray()
        _upcomingItems.value.forEach { up ->
            val o = JSONObject().apply {
                put("id", up.id)
                put("title", up.title)
                put("accountName", up.accountName)
                put("amount", up.amount)
                put("isIncome", up.isIncome)
                put("dateLabel", up.dateLabel)
                put("dayLabel", up.dayLabel)
            }
            upArr.put(o)
        }
        root.put("upcomingItems", upArr)

        // 8. Categories & Subcategories (including all custom categories added by user)
        val cArr = JSONArray()
        _categories.value.forEach { cat ->
            val co = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("type", cat.type.name)
                put("iconName", cat.iconName)
                put("isVisible", cat.isVisible)
                val subArr = JSONArray()
                cat.subcategories.forEach { sub ->
                    val so = JSONObject().apply {
                        put("id", sub.id)
                        put("name", sub.name)
                        put("iconName", sub.iconName)
                        put("isVisible", sub.isVisible)
                    }
                    subArr.put(so)
                }
                put("subcategories", subArr)
            }
            cArr.put(co)
        }
        root.put("categories", cArr)

        // 9. Planned Items (Perencanaan Keuangan)
        val pArr = JSONArray()
        _plannedItems.value.forEach { p ->
            val o = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category)
                put("section", p.section.name)
                put("amount", p.amount)
            }
            pArr.put(o)
        }
        root.put("plannedItems", pArr)
        root.put("planTitle", _planTitle.value)

        // 10. Labels
        val lArr = JSONArray()
        _labels.value.forEach { lArr.put(it) }
        root.put("labels", lArr)

        // 11. User Settings
        val s = _settings.value
        val sObj = JSONObject().apply {
            put("language", s.language)
            put("isDarkMode", s.isDarkMode)
            put("primaryCurrency", s.primaryCurrency)
            put("monthlyStartDay", s.monthlyStartDay)
            put("textSizePercent", s.textSizePercent)
            put("cashAccountsOnly", s.cashAccountsOnly)
            put("showPlannedInBalance", s.showPlannedInBalance)
            put("saveReminder", s.saveReminder)
            put("billReminder", s.billReminder)
            put("budgetAlert", s.budgetAlert)
            put("passcodeEnabled", s.passcodeEnabled)
            put("appPasscode", s.appPasscode)
            put("themeColorName", s.themeColorName)
        }
        root.put("settings", sObj)
        root.put("selectedWalletFilter", _selectedWalletFilter.value)

        return root.toString(2)
    }

    fun importTransactionsFromJson(jsonStr: String, replaceExisting: Boolean): Result<com.example.data.model.BackupRestoreResult> {
        return try {
            val trimmed = jsonStr.trim()
            val rootObj = if (trimmed.startsWith("{")) JSONObject(trimmed) else null

            // 1. Parse Transactions
            val txArr = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                rootObj?.optJSONArray("transactions") ?: JSONArray()
            }

            val importedTxs = mutableListOf<Transaction>()
            for (i in 0 until txArr.length()) {
                val o = txArr.getJSONObject(i)
                importedTxs.add(
                    Transaction(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title", "Transaksi"),
                        category = o.optString("category", "Other"),
                        subcategory = o.optString("subcategory", ""),
                        amount = o.optDouble("amount", 0.0),
                        type = try { TransactionType.valueOf(o.optString("type", "EXPENSE")) } catch (e: Exception) { TransactionType.EXPENSE },
                        accountName = o.optString("accountName", "BRI Utama"),
                        dayOfMonth = o.optInt("dayOfMonth", 1),
                        monthIndex = o.optInt("monthIndex", 1),
                        year = o.optInt("year", 2026),
                        dayOfWeekName = o.optString("dayOfWeekName", "Sen"),
                        note = o.optString("note", ""),
                        label = o.optString("label", ""),
                        recurrence = o.optString("recurrence", "Tidak pernah"),
                        time = o.optString("time", "12:00"),
                        toAccountName = o.optString("toAccountName", ""),
                        transferFee = o.optDouble("transferFee", 0.0),
                        targetAssetId = o.optString("targetAssetId", ""),
                        targetAssetName = o.optString("targetAssetName", "")
                    )
                )
            }

            // 2. Parse Wallets
            val importedWallets = mutableListOf<AccountWallet>()
            val wArr = rootObj?.optJSONArray("wallets")
            if (wArr != null) {
                for (i in 0 until wArr.length()) {
                    val o = wArr.getJSONObject(i)
                    importedWallets.add(
                        AccountWallet(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Dompet"),
                            bankType = o.optString("bankType", "Bank · IDR"),
                            balance = o.optDouble("balance", 0.0),
                            lastCategory = o.optString("lastCategory", ""),
                            lastAmount = o.optDouble("lastAmount", 0.0),
                            lastActivityTime = o.optString("lastActivityTime", ""),
                            currency = o.optString("currency", "IDR"),
                            iconName = o.optString("iconName", "Bank"),
                            excludeFromNetWorth = o.optBoolean("excludeFromNetWorth", false),
                            excludeFromBudget = o.optBoolean("excludeFromBudget", false)
                        )
                    )
                }
            }

            // 3. Parse Assets
            val importedAssets = mutableListOf<AssetItem>()
            val aArr = rootObj?.optJSONArray("assets")
            if (aArr != null) {
                for (i in 0 until aArr.length()) {
                    val o = aArr.getJSONObject(i)
                    val recList = mutableListOf<AssetRecord>()
                    val rArr = o.optJSONArray("records")
                    if (rArr != null) {
                        for (j in 0 until rArr.length()) {
                            val ro = rArr.getJSONObject(j)
                            recList.add(
                                AssetRecord(
                                    id = ro.optString("id", UUID.randomUUID().toString()),
                                    assetId = ro.optString("assetId", ""),
                                    tag = ro.optString("tag", "Buy"),
                                    isIncrease = ro.optBoolean("isIncrease", true),
                                    amount = ro.optDouble("amount", 0.0),
                                    sourceWalletName = ro.optString("sourceWalletName", ""),
                                    date = ro.optString("date", "29 Okt 2026"),
                                    time = ro.optString("time", "00.15"),
                                    note = ro.optString("note", "")
                                )
                            )
                        }
                    }
                    importedAssets.add(
                        AssetItem(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Aset"),
                            group = o.optString("group", "Investasi"),
                            assetType = o.optString("assetType", "Bank Account"),
                            value = o.optDouble("value", 0.0),
                            platform = o.optString("platform", "Bibit"),
                            performancePercent = o.optDouble("performancePercent", 10.2),
                            currency = o.optString("currency", "IDR"),
                            createdAtDate = o.optString("createdAtDate", "29 Okt 2026"),
                            createdAtTime = o.optString("createdAtTime", "00.15"),
                            valuationMethod = o.optString("valuationMethod", "Manual"),
                            records = recList
                        )
                    )
                }
            }

            // 4. Parse Debts
            val importedDebts = mutableListOf<DebtItem>()
            val dArr = rootObj?.optJSONArray("debts")
            if (dArr != null) {
                for (i in 0 until dArr.length()) {
                    val o = dArr.getJSONObject(i)
                    importedDebts.add(
                        DebtItem(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Pinjaman"),
                            type = try { DebtType.valueOf(o.optString("type", "BORROWED")) } catch (e: Exception) { DebtType.BORROWED },
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            dueDate = o.optString("dueDate", "1 Des 2027")
                        )
                    )
                }
            }

            // 5. Parse Categories & Subcategories
            val importedCategories = mutableListOf<MainCategory>()
            val cArr = rootObj?.optJSONArray("categories")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val co = cArr.getJSONObject(i)
                    val catType = try {
                        TransactionType.valueOf(co.optString("type", "EXPENSE"))
                    } catch (e: Exception) {
                        TransactionType.EXPENSE
                    }
                    val subList = mutableListOf<SubcategoryItem>()
                    val subArr = co.optJSONArray("subcategories")
                    if (subArr != null) {
                        for (j in 0 until subArr.length()) {
                            val so = subArr.getJSONObject(j)
                            subList.add(
                                SubcategoryItem(
                                    id = so.optString("id", UUID.randomUUID().toString()),
                                    name = so.optString("name", "Sub"),
                                    iconName = so.optString("iconName", ""),
                                    isVisible = so.optBoolean("isVisible", true)
                                )
                            )
                        }
                    }
                    importedCategories.add(
                        MainCategory(
                            id = co.optString("id", UUID.randomUUID().toString()),
                            name = co.optString("name", "Kategori"),
                            type = catType,
                            iconName = co.optString("iconName", ""),
                            isVisible = co.optBoolean("isVisible", true),
                            subcategories = subList
                        )
                    )
                }
            }

            // 6. Parse Planned Items (Perencanaan)
            val importedPlans = mutableListOf<PlannedItem>()
            val pArr = rootObj?.optJSONArray("plannedItems")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    val po = pArr.getJSONObject(i)
                    val section = try {
                        PlanSectionType.valueOf(po.optString("section", "EXPENSE"))
                    } catch (e: Exception) {
                        PlanSectionType.EXPENSE
                    }
                    importedPlans.add(
                        PlannedItem(
                            id = po.optString("id", UUID.randomUUID().toString()),
                            name = po.optString("name", "Rencana"),
                            category = po.optString("category", ""),
                            amount = po.optDouble("amount", 0.0),
                            section = section
                        )
                    )
                }
            }
            val importedPlanTitle = rootObj?.optString("planTitle", "") ?: ""

            // 7. Parse Budgets
            val importedBudgets = mutableListOf<BudgetCategory>()
            val bArr = rootObj?.optJSONArray("budgets")
            if (bArr != null) {
                for (i in 0 until bArr.length()) {
                    val bo = bArr.getJSONObject(i)
                    importedBudgets.add(
                        BudgetCategory(
                            id = bo.optString("id", UUID.randomUUID().toString()),
                            category = bo.optString("category", "Kategori"),
                            allocated = bo.optDouble("allocated", 0.0),
                            spent = bo.optDouble("spent", 0.0)
                        )
                    )
                }
            }

            // 8. Parse Recurring Transactions
            val importedRecurring = mutableListOf<RecurringTransaction>()
            val recArr = rootObj?.optJSONArray("recurringTransactions")
            if (recArr != null) {
                for (i in 0 until recArr.length()) {
                    val ro = recArr.getJSONObject(i)
                    importedRecurring.add(
                        RecurringTransaction(
                            id = ro.optString("id", UUID.randomUUID().toString()),
                            title = ro.optString("title", "Transaksi Berulang"),
                            amount = ro.optDouble("amount", 0.0),
                            type = try { TransactionType.valueOf(ro.optString("type", "EXPENSE")) } catch (e: Exception) { TransactionType.EXPENSE },
                            category = ro.optString("category", "Other"),
                            subcategory = ro.optString("subcategory", ""),
                            accountName = ro.optString("accountName", "BRI Utama"),
                            dayOfMonth = ro.optInt("dayOfMonth", 1),
                            frequency = ro.optString("frequency", "Setiap bulan"),
                            startDate = ro.optString("startDate", ""),
                            endDate = ro.optString("endDate", ""),
                            note = ro.optString("note", ""),
                            label = ro.optString("label", ""),
                            isActive = ro.optBoolean("isActive", true),
                            autoDeduct = ro.optBoolean("autoDeduct", true),
                            lastProcessedMonthYear = ro.optString("lastProcessedMonthYear", "")
                        )
                    )
                }
            }

            // 9. Parse Upcoming Schedules
            val importedUpcoming = mutableListOf<UpcomingScheduleItem>()
            val upArr = rootObj?.optJSONArray("upcomingItems")
            if (upArr != null) {
                for (i in 0 until upArr.length()) {
                    val uo = upArr.getJSONObject(i)
                    importedUpcoming.add(
                        UpcomingScheduleItem(
                            id = uo.optString("id", UUID.randomUUID().toString()),
                            title = uo.optString("title", "Jadwal"),
                            accountName = uo.optString("accountName", "BRI Utama"),
                            amount = uo.optDouble("amount", 0.0),
                            isIncome = uo.optBoolean("isIncome", false),
                            dateLabel = uo.optString("dateLabel", "1 Nov"),
                            dayLabel = uo.optString("dayLabel", "Min")
                        )
                    )
                }
            }

            // 10. Parse Labels
            val importedLabels = mutableListOf<String>()
            val lArr = rootObj?.optJSONArray("labels")
            if (lArr != null) {
                for (i in 0 until lArr.length()) {
                    importedLabels.add(lArr.getString(i))
                }
            }

            // 11. Parse Settings
            val sObj = rootObj?.optJSONObject("settings")
            val importedSettings: UserSettings? = if (sObj != null) {
                UserSettings(
                    language = sObj.optString("language", "Indonesia"),
                    isDarkMode = sObj.optBoolean("isDarkMode", true),
                    primaryCurrency = sObj.optString("primaryCurrency", "IDR"),
                    monthlyStartDay = sObj.optInt("monthlyStartDay", 1),
                    textSizePercent = sObj.optInt("textSizePercent", 100),
                    cashAccountsOnly = sObj.optBoolean("cashAccountsOnly", false),
                    showPlannedInBalance = sObj.optBoolean("showPlannedInBalance", true),
                    saveReminder = sObj.optBoolean("saveReminder", false),
                    billReminder = sObj.optBoolean("billReminder", false),
                    budgetAlert = sObj.optBoolean("budgetAlert", true),
                    passcodeEnabled = sObj.optBoolean("passcodeEnabled", false),
                    appPasscode = sObj.optString("appPasscode", "1234"),
                    themeColorName = sObj.optString("themeColorName", "FinTrack Blue")
                )
            } else null

            val importedFilter = rootObj?.optString("selectedWalletFilter", "") ?: ""

            val hasAnyData = importedTxs.isNotEmpty() ||
                    importedWallets.isNotEmpty() ||
                    importedAssets.isNotEmpty() ||
                    importedDebts.isNotEmpty() ||
                    importedCategories.isNotEmpty() ||
                    importedPlans.isNotEmpty() ||
                    importedBudgets.isNotEmpty() ||
                    importedRecurring.isNotEmpty() ||
                    importedSettings != null

            if (!hasAnyData) {
                return Result.failure(Exception("Tidak ditemukan data yang valid dalam file cadangan JSON"))
            }

            if (replaceExisting) {
                if (importedTxs.isNotEmpty()) _transactions.value = importedTxs
                if (importedWallets.isNotEmpty()) _wallets.value = importedWallets
                if (importedAssets.isNotEmpty()) _assets.value = importedAssets
                if (importedDebts.isNotEmpty()) _debts.value = importedDebts
                if (importedCategories.isNotEmpty()) _categories.value = importedCategories
                if (importedPlans.isNotEmpty()) _plannedItems.value = importedPlans
                if (importedPlanTitle.isNotBlank()) _planTitle.value = importedPlanTitle
                if (importedBudgets.isNotEmpty()) _budgets.value = importedBudgets
                if (importedRecurring.isNotEmpty()) _recurringTransactions.value = importedRecurring
                if (importedUpcoming.isNotEmpty()) _upcomingItems.value = importedUpcoming
                if (importedLabels.isNotEmpty()) _labels.value = importedLabels
                if (importedSettings != null) _settings.value = importedSettings
                if (importedFilter.isNotBlank()) _selectedWalletFilter.value = importedFilter
            } else {
                // Merge Transactions
                if (importedTxs.isNotEmpty()) {
                    val existingTxIds = _transactions.value.map { it.id }.toSet()
                    val newTxs = importedTxs.filterNot { it.id in existingTxIds }
                    _transactions.value = newTxs + _transactions.value
                }
                // Merge Wallets
                if (importedWallets.isNotEmpty()) {
                    val existingWalletNames = _wallets.value.map { it.name.trim().lowercase() }.toSet()
                    val existingWalletIds = _wallets.value.map { it.id }.toSet()
                    val newWallets = importedWallets.filterNot {
                        it.id in existingWalletIds || it.name.trim().lowercase() in existingWalletNames
                    }
                    _wallets.value = _wallets.value + newWallets
                }
                // Merge Assets
                if (importedAssets.isNotEmpty()) {
                    val existingAssetNames = _assets.value.map { it.name.trim().lowercase() }.toSet()
                    val existingAssetIds = _assets.value.map { it.id }.toSet()
                    val newAssets = importedAssets.filterNot {
                        it.id in existingAssetIds || it.name.trim().lowercase() in existingAssetNames
                    }
                    _assets.value = _assets.value + newAssets
                }
                // Merge Debts
                if (importedDebts.isNotEmpty()) {
                    val existingDebtNames = _debts.value.map { it.name.trim().lowercase() }.toSet()
                    val existingDebtIds = _debts.value.map { it.id }.toSet()
                    val newDebts = importedDebts.filterNot {
                        it.id in existingDebtIds || it.name.trim().lowercase() in existingDebtNames
                    }
                    _debts.value = _debts.value + newDebts
                }
                // Merge Categories & Subcategories
                if (importedCategories.isNotEmpty()) {
                    val currentCats = _categories.value.toMutableList()
                    importedCategories.forEach { impCat ->
                        val existingIndex = currentCats.indexOfFirst {
                            it.name.trim().equals(impCat.name.trim(), ignoreCase = true) && it.type == impCat.type
                        }
                        if (existingIndex >= 0) {
                            val existingCat = currentCats[existingIndex]
                            val existingSubNames = existingCat.subcategories.map { it.name.trim().lowercase() }.toSet()
                            val newSubs = impCat.subcategories.filterNot { it.name.trim().lowercase() in existingSubNames }
                            if (newSubs.isNotEmpty()) {
                                currentCats[existingIndex] = existingCat.copy(subcategories = existingCat.subcategories + newSubs)
                            }
                        } else {
                            currentCats.add(impCat)
                        }
                    }
                    _categories.value = currentCats
                }
                // Merge Plans
                if (importedPlans.isNotEmpty()) {
                    val existingPlanNames = _plannedItems.value.map { it.name.trim().lowercase() to it.section }.toSet()
                    val newPlans = importedPlans.filterNot { (it.name.trim().lowercase() to it.section) in existingPlanNames }
                    _plannedItems.value = _plannedItems.value + newPlans
                }
                if (importedPlanTitle.isNotBlank() && _planTitle.value.isBlank()) {
                    _planTitle.value = importedPlanTitle
                }
                // Merge Budgets
                if (importedBudgets.isNotEmpty()) {
                    val existingBudgetCats = _budgets.value.map { it.category.trim().lowercase() }.toSet()
                    val newBudgets = importedBudgets.filterNot { it.category.trim().lowercase() in existingBudgetCats }
                    _budgets.value = _budgets.value + newBudgets
                }
                // Merge Recurring
                if (importedRecurring.isNotEmpty()) {
                    val existingRecIds = _recurringTransactions.value.map { it.id }.toSet()
                    val newRec = importedRecurring.filterNot { it.id in existingRecIds }
                    _recurringTransactions.value = _recurringTransactions.value + newRec
                }
                // Merge Upcoming
                if (importedUpcoming.isNotEmpty()) {
                    val existingUpIds = _upcomingItems.value.map { it.id }.toSet()
                    val newUp = importedUpcoming.filterNot { it.id in existingUpIds }
                    _upcomingItems.value = _upcomingItems.value + newUp
                }
                // Merge Labels
                if (importedLabels.isNotEmpty()) {
                    _labels.value = (_labels.value + importedLabels).distinct()
                }
                // Update Settings if provided
                if (importedSettings != null) {
                    _settings.value = importedSettings
                }
            }

            saveToDisk()
            Result.success(
                com.example.data.model.BackupRestoreResult(
                    transactionCount = importedTxs.size,
                    walletCount = importedWallets.size,
                    assetCount = importedAssets.size,
                    debtCount = importedDebts.size,
                    categoryCount = importedCategories.size,
                    planCount = importedPlans.size,
                    budgetCount = importedBudgets.size,
                    recurringCount = importedRecurring.size,
                    settingsRestored = importedSettings != null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetAllData(createDefaultWallet: Boolean = true) {
        _transactions.value = emptyList()
        _wallets.value = if (createDefaultWallet) {
            listOf(
                AccountWallet(
                    name = "Dompet Utama",
                    balance = 0.0,
                    bankType = "Tunai · IDR",
                    currency = "IDR"
                )
            )
        } else {
            emptyList()
        }
        _assets.value = emptyList()
        _debts.value = emptyList()
        _budgets.value = emptyList()
        _upcomingItems.value = emptyList()
        _plannedItems.value = emptyList()
        _recurringTransactions.value = emptyList()
        _selectedWalletFilter.value = "Semua"
        _categories.value = FinanceStorageHelper.getDefaultCategories()
        _labels.value = listOf("Investasi", "Pribadi", "Keluarga", "Tabungan")
        saveToDisk()
    }
}
