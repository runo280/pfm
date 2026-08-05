package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.FinanceRepository
import com.example.util.BackupRestoreHelper
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.ExportHelper
import com.example.util.JalaliCalendarHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.util.UserPreferencesRepository

data class DueReminderItem(
    val id: String,
    val title: String,
    val amount: Double,
    val typeName: String, // "قسط" or "چک"
    val dueText: String,
    val isToday: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val userPrefsRepo = UserPreferencesRepository(application)

    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    private val _currencyUnit = MutableStateFlow(
        try {
            CurrencyUnit.valueOf(prefs.getString("currency_unit", "TOMAN") ?: "TOMAN")
        } catch (e: Exception) {
            CurrencyUnit.TOMAN
        }
    )
    val currencyUnit: StateFlow<CurrencyUnit> = _currencyUnit.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt("reminder_hour", 22))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themePrimaryColor = MutableStateFlow(prefs.getString("primary_color", "BLUE") ?: "BLUE")
    val themePrimaryColor: StateFlow<String> = _themePrimaryColor.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean("is_app_lock_enabled", false))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _appPin = MutableStateFlow(prefs.getString("app_pin", "") ?: "")
    val appPin: StateFlow<String> = _appPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("is_biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(!_isAppLockEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _selectedMonth = MutableStateFlow(JalaliCalendarHelper.getCurrentJalaliYearMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val accounts: StateFlow<List<AccountEntity>>
    val categories: StateFlow<List<CategoryEntity>>
    val transactions: StateFlow<List<TransactionEntity>>
    val installments: StateFlow<List<InstallmentEntity>>
    val cheques: StateFlow<List<ChequeEntity>>
    val debts: StateFlow<List<DebtEntity>>

    val dueReminderItems: StateFlow<List<DueReminderItem>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db)

        viewModelScope.launch {
            repository.ensureDefaultCategoriesExist()
            repository.ensureInitialDataSeededIfEmpty()
        }

        viewModelScope.launch {
            userPrefsRepo.themeMode.collect { mode ->
                _themeMode.value = mode
            }
        }

        viewModelScope.launch {
            userPrefsRepo.themePrimaryColor.collect { colorKey ->
                _themePrimaryColor.value = colorKey
            }
        }

        accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        transactions = repository.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        installments = repository.installments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        cheques = repository.cheques.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        debts = repository.debts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dueReminderItems = combine(installments, cheques) { instList, chqList ->
            val items = mutableListOf<DueReminderItem>()
            val today = JalaliCalendarHelper.getCurrentJalaliDate()
            val tomorrow = JalaliCalendarHelper.getTomorrowJalaliDate()
            val todayStr = today.toFormattedString()
            val tomorrowStr = tomorrow.toFormattedString()

            // Active Installments
            instList.filter { it.status == "ACTIVE" && it.paidInstallments < it.totalInstallments }.forEach { inst ->
                val nextDueDate = JalaliCalendarHelper.getInstallmentNextDueDate(inst, today)
                if (nextDueDate.year == today.year && nextDueDate.month == today.month && nextDueDate.day == today.day) {
                    items.add(
                        DueReminderItem(
                            id = "inst_${inst.id}",
                            title = inst.title,
                            amount = inst.monthlyPayment,
                            typeName = "قسط",
                            dueText = "امروز (${today.day}ام ماه)",
                            isToday = true
                        )
                    )
                } else if (nextDueDate.year == tomorrow.year && nextDueDate.month == tomorrow.month && nextDueDate.day == tomorrow.day) {
                    items.add(
                        DueReminderItem(
                            id = "inst_${inst.id}",
                            title = inst.title,
                            amount = inst.monthlyPayment,
                            typeName = "قسط",
                            dueText = "فردا (${tomorrow.day}ام ماه)",
                            isToday = false
                        )
                    )
                }
            }

            // Pending Cheques
            chqList.filter { it.status == "PENDING" }.forEach { chq ->
                val chqType = if (chq.type == "PAYABLE") "چک پرداختی" else "چک دریافتی"
                if (chq.dueDateJalali == todayStr) {
                    items.add(
                        DueReminderItem(
                            id = "chq_${chq.id}",
                            title = "$chqType - ${chq.bankName} (${chq.chequeNumber})",
                            amount = chq.amount,
                            typeName = "چک",
                            dueText = "امروز",
                            isToday = true
                        )
                    )
                } else if (chq.dueDateJalali == tomorrowStr) {
                    items.add(
                        DueReminderItem(
                            id = "chq_${chq.id}",
                            title = "$chqType - ${chq.bankName} (${chq.chequeNumber})",
                            amount = chq.amount,
                            typeName = "چک",
                            dueText = "فردا",
                            isToday = false
                        )
                    )
                }
            }

            items
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setCurrencyUnit(unit: CurrencyUnit) {
        _currencyUnit.value = unit
        prefs.edit().putString("currency_unit", unit.name).apply()
        viewModelScope.launch { _userMessage.emit("واحد پول به ${unit.titleFa} تغییر یافت.") }
    }

    fun setReminderHour(hour: Int) {
        _reminderHour.value = hour
        prefs.edit().putInt("reminder_hour", hour).apply()
        viewModelScope.launch { _userMessage.emit("ساعت یادآوری روزانه به $hour:00 تنظیم گردید.") }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            userPrefsRepo.setThemeMode(mode)
            val title = when (mode) {
                "LIGHT" -> "حالت روشن"
                "DARK" -> "حالت تاریک"
                else -> "پیروی از سیستم"
            }
            _userMessage.emit("پوسته برنامه به $title تغییر یافت.")
        }
    }

    fun setThemePrimaryColor(colorKey: String) {
        _themePrimaryColor.value = colorKey
        viewModelScope.launch {
            userPrefsRepo.setThemePrimaryColor(colorKey)
            val colorObj = com.example.ui.theme.PrimaryThemeColor.fromKey(colorKey)
            _userMessage.emit("رنگ اصلی پوسته به «${colorObj.titleFa}» تغییر یافت.")
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        _isAppLockEnabled.value = enabled
        prefs.edit().putBoolean("is_app_lock_enabled", enabled).apply()
        if (!enabled) {
            _isAppUnlocked.value = true
        } else if (_appPin.value.isEmpty()) {
            _isAppUnlocked.value = true
        }
        val msg = if (enabled) "قفل امنیت برنامه فعال شد." else "قفل امنیت برنامه غیرفعال شد."
        viewModelScope.launch { _userMessage.emit(msg) }
    }

    fun setAppPin(pin: String) {
        _appPin.value = pin
        prefs.edit().putString("app_pin", pin).apply()
        viewModelScope.launch { _userMessage.emit("رمز عبور برنامه به‌روزرسانی شد.") }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        prefs.edit().putBoolean("is_biometric_enabled", enabled).apply()
        val msg = if (enabled) "ورود با اثر انگشت فعال شد." else "ورود با اثر انگشت غیرفعال شد."
        viewModelScope.launch { _userMessage.emit(msg) }
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun lockApp() {
        if (_isAppLockEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun setSelectedMonth(yearMonth: String) {
        _selectedMonth.value = yearMonth
    }

    // --- Accounts ---
    fun setDefaultAccount(accountId: Long) {
        viewModelScope.launch {
            repository.setDefaultAccount(accountId)
            _userMessage.emit("حساب پیش‌فرض ثبت هزینه تغییر کرد.")
        }
    }

    // --- Accounts ---
    fun addAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.addAccount(account)
            _userMessage.emit("حساب '${account.name}' با موفقیت ایجاد شد.")
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
            _userMessage.emit("اطلاعات حساب به روزرسانی شد.")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _userMessage.emit("حساب حذف شد.")
        }
    }

    fun transferBetweenAccounts(fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double, note: String) {
        viewModelScope.launch {
            repository.transferBetweenAccounts(fromAccountId, toAccountId, amount, fee, note)
            _userMessage.emit("انتقال وجه با موفقیت انجام شد.")
        }
    }

    // --- Transactions ---
    fun addTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(tx)
            _userMessage.emit("تراکنش ثبت شد.")
        }
    }

    fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
            _userMessage.emit("تراکنش بروزرسانی شد.")
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _userMessage.emit("تراکنش حذف شد.")
        }
    }

    // --- Categories ---
    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.addCategory(category)
            _userMessage.emit("دسته‌بندی '${category.name}' اضافه شد.")
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            _userMessage.emit("دسته‌بندی ویرایش شد.")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _userMessage.emit("دسته‌بندی حذف شد.")
        }
    }

    fun deleteCategoryWithHandling(
        category: CategoryEntity,
        deleteTransactions: Boolean,
        targetMainCategoryId: Long? = null,
        targetSubcategoryId: Long? = null
    ) {
        viewModelScope.launch {
            try {
                repository.deleteCategoryWithHandling(
                    category = category,
                    deleteTransactions = deleteTransactions,
                    targetMainCategoryId = targetMainCategoryId,
                    targetSubcategoryId = targetSubcategoryId
                )
                _userMessage.emit("دسته‌بندی با موفقیت حذف شد.")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در حذف دسته‌بندی.")
            }
        }
    }

    // --- Installments ---
    fun addInstallment(installment: InstallmentEntity, customItemAmounts: List<Double>? = null) {
        viewModelScope.launch {
            repository.addInstallment(installment, customItemAmounts)
            _userMessage.emit("اطلاعات وام و قسط ثبت شد.")
        }
    }

    fun updateInstallment(installment: InstallmentEntity) {
        viewModelScope.launch {
            repository.updateInstallment(installment)
            _userMessage.emit("اطلاعات قسط ویرایش شد.")
        }
    }

    fun deleteInstallment(installment: InstallmentEntity) {
        viewModelScope.launch {
            repository.deleteInstallment(installment)
            _userMessage.emit("قسط حذف شد.")
        }
    }

    fun payInstallment(installment: InstallmentEntity, accountId: Long) {
        viewModelScope.launch {
            repository.payInstallment(installment, accountId)
            _userMessage.emit("قسط با موفقیت پرداخت و هزینه آن ثبت شد.")
        }
    }

    fun unpayInstallment(installment: InstallmentEntity) {
        viewModelScope.launch {
            try {
                repository.unpayInstallment(installment)
                _userMessage.emit("وضعیت قسط به عدم پرداخت بازگردانده شد.")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در تغییر وضعیت قسط.")
            }
        }
    }

    fun getInstallmentItems(installmentId: Long): Flow<List<InstallmentItemEntity>> {
        return repository.getInstallmentItems(installmentId)
    }

    fun updateInstallmentItem(item: InstallmentItemEntity) {
        viewModelScope.launch {
            repository.updateInstallmentItem(item)
            _userMessage.emit("مبلغ/اطلاعات قسط ویرایش شد.")
        }
    }

    fun unpaySpecificInstallmentItem(installment: InstallmentEntity, item: InstallmentItemEntity) {
        viewModelScope.launch {
            try {
                repository.unpaySpecificInstallmentItem(installment, item)
                _userMessage.emit("قسط شماره ${item.installmentNumber} به وضعیت عدم پرداخت بازگردانده شد.")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در تغییر وضعیت قسط.")
            }
        }
    }

    fun paySpecificInstallmentItem(installment: InstallmentEntity, item: InstallmentItemEntity, accountId: Long) {
        viewModelScope.launch {
            repository.paySpecificInstallmentItem(installment, item, accountId)
            _userMessage.emit("قسط شماره ${item.installmentNumber} با موفقیت پرداخت شد.")
        }
    }

    // --- Cheques ---
    fun addCheque(cheque: ChequeEntity) {
        viewModelScope.launch {
            repository.addCheque(cheque)
            _userMessage.emit("اطلاعات چک ثبت شد.")
        }
    }

    fun updateCheque(cheque: ChequeEntity) {
        viewModelScope.launch {
            repository.updateCheque(cheque)
            _userMessage.emit("اطلاعات چک ویرایش شد.")
        }
    }

    fun deleteCheque(cheque: ChequeEntity) {
        viewModelScope.launch {
            repository.deleteCheque(cheque)
            _userMessage.emit("چک حذف شد.")
        }
    }

    fun markChequePassed(cheque: ChequeEntity, accountId: Long) {
        viewModelScope.launch {
            repository.markChequePassed(cheque, accountId)
            _userMessage.emit("چک با موفقیت پاس شد و تراکنش آن ثبت گردید.")
        }
    }

    fun unpassCheque(cheque: ChequeEntity) {
        viewModelScope.launch {
            repository.unpassCheque(cheque)
            _userMessage.emit("چک به وضعیت پاس نشده بازگردانده شد.")
        }
    }

    // --- Export & Backup ---
    fun exportBackupJson(context: Context, uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dbAccounts = repository.getAllAccountsSync()
                val dbCategories = repository.getAllCategoriesSync()
                val dbTransactions = repository.getAllTransactionsSync()
                val dbBudgets = repository.getAllBudgetsSync()
                val dbInstallments = repository.getAllInstallmentsSync()
                val dbCheques = repository.getAllChequesSync()
                val dbDebts = repository.getAllDebtsSync()

                val jsonStr = BackupRestoreHelper.exportToJson(
                    accounts = dbAccounts,
                    categories = dbCategories,
                    transactions = dbTransactions,
                    budgets = dbBudgets,
                    installments = dbInstallments,
                    cheques = dbCheques,
                    debts = dbDebts,
                    currencyUnit = currencyUnit.value
                )

                var written = false
                try {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                        out.flush()
                        written = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (!written) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }

                _userMessage.emit("پشتیبان‌گیری کامل با موفقیت انجام شد.")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در ایجاد پشتیبان. لطفاً مجدداً تلاش کنید.")
            }
        }
    }

    fun shareBackupJson(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbAccounts = repository.getAllAccountsSync()
                val dbCategories = repository.getAllCategoriesSync()
                val dbTransactions = repository.getAllTransactionsSync()
                val dbBudgets = repository.getAllBudgetsSync()
                val dbInstallments = repository.getAllInstallmentsSync()
                val dbCheques = repository.getAllChequesSync()
                val dbDebts = repository.getAllDebtsSync()

                val jsonStr = BackupRestoreHelper.exportToJson(
                    accounts = dbAccounts,
                    categories = dbCategories,
                    transactions = dbTransactions,
                    budgets = dbBudgets,
                    installments = dbInstallments,
                    cheques = dbCheques,
                    debts = dbDebts,
                    currencyUnit = currencyUnit.value
                )

                val fileName = "ExpenseTracker_Backup_${JalaliCalendarHelper.getCurrentJalaliDateTimeString()}.json"
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { out ->
                    out.write(jsonStr.toByteArray(Charsets.UTF_8))
                }

                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                withContext(Dispatchers.Main) {
                    ExportHelper.shareFile(context, fileUri, "application/json", "اشتراک‌گذاری فایل پشتیبان")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در ایجاد فایل پشتیبان.")
            }
        }
    }

    fun importBackupJson(context: Context, uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                if (jsonStr.isBlank()) {
                    _userMessage.emit("فایل انتخاب‌شده خالی است.")
                    return@launch
                }
                val data = BackupRestoreHelper.importFromJson(jsonStr)
                repository.restoreDatabase(
                    data.accounts,
                    data.categories,
                    data.transactions,
                    data.budgets,
                    data.installments,
                    data.cheques,
                    data.debts
                )
                setCurrencyUnit(data.currencyUnit)
                _userMessage.emit("بازیابی اطلاعات با موفقیت انجام شد.")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در بازگردانی فایل پشتیبان. ساختار فایل معتبر نیست.")
            }
        }
    }

    fun resetAndSeedMockData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.resetAndSeedMockData()
                _userMessage.emit("داده‌های نمونه با موفقیت جایگزین شدند.")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در ایجاد داده‌های نمونه.")
            }
        }
    }

    fun clearAllAppData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.clearAllData()
                _userMessage.emit("تمام اطلاعات برنامه با موفقیت حذف شد.")
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.emit("خطا در حذف اطلاعات برنامه.")
            }
        }
    }

    fun exportFilteredPdf(
        context: Context,
        filteredTxs: List<TransactionEntity>
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val dbAccounts = repository.getAllAccountsSync()
            val dbCategories = repository.getAllCategoriesSync()

            val accMap = dbAccounts.associateBy { it.id }
            val catMap = dbCategories.associateBy { it.id }

            val totalInc = filteredTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExp = filteredTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val uri = ExportHelper.exportToPdf(
                context,
                filteredTxs,
                accMap,
                catMap,
                totalInc,
                totalExp,
                currencyUnit.value
            )
            if (uri != null) {
                ExportHelper.shareFile(context, uri, "application/pdf", "اشتراک‌گذاری گزارش پی‌دی‌اف")
            } else {
                _userMessage.emit("خطا در ساخت فایل PDF")
            }
        }
    }

    fun exportFilteredCsv(
        context: Context,
        filteredTxs: List<TransactionEntity>
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val dbAccounts = repository.getAllAccountsSync()
            val dbCategories = repository.getAllCategoriesSync()

            val accMap = dbAccounts.associateBy { it.id }
            val catMap = dbCategories.associateBy { it.id }

            val uri = ExportHelper.exportToCsv(
                context,
                filteredTxs,
                accMap,
                catMap,
                currencyUnit.value
            )
            if (uri != null) {
                ExportHelper.shareFile(context, uri, "text/csv", "اشتراک‌گذاری گزارش اکسل / CSV")
            } else {
                _userMessage.emit("خطا در ساخت فایل اکسل")
            }
        }
    }

    // --- Debts & Receivables ---
    fun addDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.addDebt(debt)
            _userMessage.emit("طلب / بدهی جدید ثبت شد")
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.updateDebt(debt)
            _userMessage.emit("اطلاعات طلب / بدهی بروزرسانی شد")
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            _userMessage.emit("طلب / بدهی با موفقیت حذف شد")
        }
    }

    fun settleDebt(debt: DebtEntity, settleAmount: Double, accountId: Long?) {
        viewModelScope.launch {
            repository.settleDebt(debt, settleAmount, accountId)
            _userMessage.emit("تسویه حساب با موفقیت ثبت شد")
        }
    }
}
