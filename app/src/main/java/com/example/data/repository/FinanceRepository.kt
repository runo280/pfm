package com.example.data.repository

import com.example.data.local.*
import com.example.util.JalaliCalendarHelper
import com.example.util.JalaliDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class FinanceRepository(private val database: AppDatabase) {

    val accountDao = database.accountDao()
    val categoryDao = database.categoryDao()
    val transactionDao = database.transactionDao()
    val budgetDao = database.budgetDao()
    val installmentDao = database.installmentDao()
    val installmentItemDao = database.installmentItemDao()
    val chequeDao = database.chequeDao()
    val debtDao = database.debtDao()

    val accounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val installments: Flow<List<InstallmentEntity>> = installmentDao.getAllInstallments()
    val cheques: Flow<List<ChequeEntity>> = chequeDao.getAllCheques()
    val debts: Flow<List<DebtEntity>> = debtDao.getAllDebts()

    fun getInstallmentItems(installmentId: Long): Flow<List<InstallmentItemEntity>> = flow {
        var items = installmentItemDao.getItemsForInstallmentSync(installmentId)
        if (items.isEmpty()) {
            val inst = installmentDao.getInstallmentById(installmentId)
            if (inst != null) {
                val generated = (1..inst.totalInstallments).map { num ->
                    InstallmentItemEntity(
                        installmentId = inst.id,
                        installmentNumber = num,
                        amount = inst.monthlyPayment,
                        isPaid = num <= inst.paidInstallments
                    )
                }
                installmentItemDao.insertItems(generated)
            }
        }
        emitAll(installmentItemDao.getItemsForInstallment(installmentId))
    }

    suspend fun setDefaultAccount(accountId: Long) {
        accountDao.updateDefaultAccount(accountId)
    }

    fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMonth(yearMonth)

    fun getBudgetsForMonth(yearMonth: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(yearMonth)

    // --- Accounts & Transfers ---
    suspend fun addAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        fee: Double,
        note: String,
        jalaliDate: String = JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()
    ) {
        val totalDeduction = amount + fee
        accountDao.updateBalance(fromAccountId, -totalDeduction)
        accountDao.updateBalance(toAccountId, amount)

        val tx = TransactionEntity(
            type = "TRANSFER",
            amount = amount,
            accountId = fromAccountId,
            targetAccountId = toAccountId,
            jalaliDate = jalaliDate,
            title = "انتقال بین حساب‌ها",
            note = note,
            transferFee = fee
        )
        transactionDao.insertTransaction(tx)
    }

    // --- Transactions ---
    suspend fun addTransaction(tx: TransactionEntity) {
        transactionDao.insertTransaction(tx)
        when (tx.type) {
            "EXPENSE" -> {
                accountDao.updateBalance(tx.accountId, -tx.amount)
            }
            "INCOME" -> {
                accountDao.updateBalance(tx.accountId, tx.amount)
            }
            "TRANSFER" -> {
                if (tx.targetAccountId != null) {
                    val totalDeduct = tx.amount + tx.transferFee
                    accountDao.updateBalance(tx.accountId, -totalDeduct)
                    accountDao.updateBalance(tx.targetAccountId, tx.amount)
                }
            }
        }
    }

    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        // Reverse old transaction impact
        when (oldTx.type) {
            "EXPENSE" -> accountDao.updateBalance(oldTx.accountId, oldTx.amount)
            "INCOME" -> accountDao.updateBalance(oldTx.accountId, -oldTx.amount)
            "TRANSFER" -> {
                if (oldTx.targetAccountId != null) {
                    accountDao.updateBalance(oldTx.accountId, oldTx.amount + oldTx.transferFee)
                    accountDao.updateBalance(oldTx.targetAccountId, -oldTx.amount)
                }
            }
        }

        // Apply new transaction
        transactionDao.updateTransaction(newTx)
        when (newTx.type) {
            "EXPENSE" -> accountDao.updateBalance(newTx.accountId, -newTx.amount)
            "INCOME" -> accountDao.updateBalance(newTx.accountId, newTx.amount)
            "TRANSFER" -> {
                if (newTx.targetAccountId != null) {
                    accountDao.updateBalance(newTx.accountId, -(newTx.amount + newTx.transferFee))
                    accountDao.updateBalance(newTx.targetAccountId, newTx.amount)
                }
            }
        }
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        transactionDao.deleteTransaction(tx)
        when (tx.type) {
            "EXPENSE" -> accountDao.updateBalance(tx.accountId, tx.amount)
            "INCOME" -> accountDao.updateBalance(tx.accountId, -tx.amount)
            "TRANSFER" -> {
                if (tx.targetAccountId != null) {
                    accountDao.updateBalance(tx.accountId, tx.amount + tx.transferFee)
                    accountDao.updateBalance(tx.targetAccountId, -tx.amount)
                }
            }
        }
    }

    // --- Categories ---
    suspend fun ensureDefaultCategoriesExist() = withContext(Dispatchers.IO) {
        val existing = categoryDao.getAllCategoriesSync()
        val isUpToDate = existing.any { it.name == "مسکن و خانه" || it.name == "درآمدهای اصلی و فرعی" }
        if (existing.isEmpty() || !isUpToDate) {
            if (!isUpToDate && existing.isNotEmpty()) {
                // If old category structure exists, clean category table so new 15-category tree is created
                existing.forEach { categoryDao.deleteCategory(it) }
            }
            val defaultStructure = mapOf(
                CategoryEntity(name = "درآمدهای اصلی و فرعی", type = "INCOME", iconName = "Work", colorHex = "#10B981", isSystem = false) to listOf(
                    "حقوق و دستمزد", "کسب‌وکار / آزاد", "سرمایه‌گذاری", "سایر درآمدها"
                ),
                CategoryEntity(name = "مسکن و خانه", type = "EXPENSE", iconName = "Home", colorHex = "#3B82F6", isSystem = false) to listOf(
                    "اجاره / رهن", "شارژ و قبوض", "تعمیرات و نگهداری", "اثاثیه و تجهیزات"
                ),
                CategoryEntity(name = "خوراک و سوپرمارکت", type = "EXPENSE", iconName = "ShoppingCart", colorHex = "#F59E0B", isSystem = false) to listOf(
                    "خرید روزمره", "میوه و تره‌بار", "پروتئین", "تنقلات و نوشیدنی"
                ),
                CategoryEntity(name = "حمل‌ونقل و خودرو", type = "EXPENSE", iconName = "DirectionsCar", colorHex = "#EF4444", isSystem = false) to listOf(
                    "سوخت", "حمل‌ونقل عمومی", "استهلاک و سرویس", "بیمه و عوارض"
                ),
                CategoryEntity(name = "پوشاک و زیبایی", type = "EXPENSE", iconName = "Face", colorHex = "#EC4899", isSystem = false) to listOf(
                    "لباس و کفش", "آرایشی و بهداشتی", "خدمات آرایشگاه", "اکسسوری"
                ),
                CategoryEntity(name = "سلامت و درمان", type = "EXPENSE", iconName = "MedicalServices", colorHex = "#10B981", isSystem = false) to listOf(
                    "دارو و داروخانه", "پزشک و درمان", "دندانپزشکی", "بیمه درمان"
                ),
                CategoryEntity(name = "قبض، ارتباطات و فناوری", type = "EXPENSE", iconName = "Receipt", colorHex = "#06B6D4", isSystem = false) to listOf(
                    "اینترنت", "شارژ سیم‌کارت", "اشتراک‌ها", "نرم‌افزار و ابزارها"
                ),
                CategoryEntity(name = "آموزش و ارتقای شخصی", type = "EXPENSE", iconName = "School", colorHex = "#6366F1", isSystem = false) to listOf(
                    "شهریه", "دوره و آموزش", "کتاب و نوشت‌افزار"
                ),
                CategoryEntity(name = "تفریح، سرگرمی و سفر", type = "EXPENSE", iconName = "Restaurant", colorHex = "#F43F5E", isSystem = false) to listOf(
                    "رستوران و کافه", "سفر و گردشگری", "سرگرمی", "ورزش"
                ),
                CategoryEntity(name = "فرزندان / خانواده", type = "EXPENSE", iconName = "ChildCare", colorHex = "#8B5CF6", isSystem = false) to listOf(
                    "مراقبت و نگهداری", "اسباب‌بازی و سرگرمی", "پول توجیبی", "حمایت از والدین/فامیل"
                ),
                CategoryEntity(name = "پت / حیوانات خانگی", type = "EXPENSE", iconName = "Pets", colorHex = "#D97706", isSystem = false) to listOf(
                    "غذای پت", "دامپزشکی", "وسایل و بهداشت"
                ),
                CategoryEntity(name = "هدیه، مناسبت‌ها و کادو", type = "EXPENSE", iconName = "CardGiftcard", colorHex = "#E11D48", isSystem = false) to listOf(
                    "کادو و مناسبت", "عیدی و چشم‌روشنی", "مراسم‌ها"
                ),
                CategoryEntity(name = "اقساط، وام و بدهی", type = "EXPENSE", iconName = "Payments", colorHex = "#7C3AED", isSystem = false) to listOf(
                    "قسط وام", "اقساط خرید", "تسویه بدهی"
                ),
                CategoryEntity(name = "پس‌انداز و سرمایه‌گذاری", type = "EXPENSE", iconName = "Savings", colorHex = "#059669", isSystem = false) to listOf(
                    "خرید طلا / سکه", "ارز و رمزارز", "صندوق‌ها و بورس", "پس‌انداز اضطراری"
                ),
                CategoryEntity(name = "متفرقه و پیش‌بینی‌نشده", type = "EXPENSE", iconName = "MoreHoriz", colorHex = "#64748B", isSystem = false) to listOf(
                    "صدقه و امور خیریه", "جرایم و خسارت‌ها", "سایر هزینه‌های خرد"
                )
            )

            defaultStructure.forEach { (mainCat, subs) ->
                val mainId = categoryDao.insertCategory(mainCat)
                subs.forEach { subName ->
                    categoryDao.insertCategory(
                        CategoryEntity(
                            name = subName,
                            type = mainCat.type,
                            iconName = mainCat.iconName,
                            colorHex = mainCat.colorHex,
                            isSystem = false,
                            parentId = mainId
                        )
                    )
                }
            }
        }
    }

    suspend fun addCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteCategoryWithHandling(
        category: CategoryEntity,
        deleteTransactions: Boolean,
        targetMainCategoryId: Long? = null,
        targetSubcategoryId: Long? = null
    ) = withContext(Dispatchers.IO) {
        val allCategories = categoryDao.getAllCategoriesSync()
        val allSubCategoryIds = if (category.parentId == null) {
            allCategories.filter { it.parentId == category.id }.map { it.id }.toSet()
        } else emptySet()

        val affectedCatIds = setOf(category.id) + allSubCategoryIds

        val allTxs = transactionDao.getAllTransactionsSync()
        val affectedTxs = allTxs.filter { tx ->
            (tx.categoryId != null && affectedCatIds.contains(tx.categoryId)) ||
            (tx.subcategoryId != null && affectedCatIds.contains(tx.subcategoryId))
        }

        if (deleteTransactions) {
            affectedTxs.forEach { tx ->
                transactionDao.deleteTransaction(tx)
            }
        } else if (targetMainCategoryId != null) {
            val targetSubName = if (targetSubcategoryId != null) allCategories.find { it.id == targetSubcategoryId }?.name else null
            val targetMainName = allCategories.find { it.id == targetMainCategoryId }?.name ?: "تراکنش"
            val newTitle = if (targetSubName != null) "$targetMainName - $targetSubName" else targetMainName

            affectedTxs.forEach { tx ->
                val updatedTx = tx.copy(
                    categoryId = targetMainCategoryId,
                    subcategoryId = targetSubcategoryId,
                    title = if (tx.title.contains(" - ")) newTitle else tx.title
                )
                transactionDao.updateTransaction(updatedTx)
            }
        }

        // Delete subcategories if main category
        if (category.parentId == null) {
            val subCats = allCategories.filter { it.parentId == category.id }
            subCats.forEach { categoryDao.deleteCategory(it) }
        }

        // Delete category itself
        categoryDao.deleteCategory(category)
    }

    // --- Budgets ---
    suspend fun setBudget(budget: BudgetEntity) {
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.deleteBudget(budget)
    }

    // --- Installments ---
    suspend fun addInstallment(installment: InstallmentEntity, customItemAmounts: List<Double>? = null) {
        val newId = installmentDao.insertInstallment(installment)
        val startJDate = JalaliCalendarHelper.parseJalaliDate(installment.startJalaliDate)
        val items = (1..installment.totalInstallments).map { num ->
            val amt = customItemAmounts?.getOrNull(num - 1) ?: installment.monthlyPayment
            val isPaid = num <= installment.paidInstallments
            val paidDate = if (isPaid) {
                if (startJDate != null) {
                    JalaliCalendarHelper.calculateInstallmentDueDate(startJDate, installment.dueDay, num - 1).toFormattedString()
                } else JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()
            } else null

            InstallmentItemEntity(
                installmentId = newId,
                installmentNumber = num,
                amount = amt,
                isPaid = isPaid,
                paidDateJalali = paidDate
            )
        }
        installmentItemDao.insertItems(items)
    }

    suspend fun updateInstallment(installment: InstallmentEntity) {
        installmentDao.updateInstallment(installment)
    }

    suspend fun deleteInstallment(installment: InstallmentEntity) {
        installmentItemDao.deleteItemsForInstallment(installment.id)
        installmentDao.deleteInstallment(installment)
    }

    suspend fun updateInstallmentItem(item: InstallmentItemEntity) {
        installmentItemDao.updateItem(item)
    }

    suspend fun paySpecificInstallmentItem(installment: InstallmentEntity, item: InstallmentItemEntity, accountId: Long, createTransaction: Boolean = true) {
        val updatedItem = item.copy(
            isPaid = true,
            paidDateJalali = JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()
        )
        installmentItemDao.updateItem(updatedItem)

        val items = installmentItemDao.getItemsForInstallmentSync(installment.id)
        val paidCount = items.count { it.isPaid || it.id == item.id }
        val newStatus = if (paidCount >= installment.totalInstallments) "COMPLETED" else "ACTIVE"
        val updatedInstallment = installment.copy(paidInstallments = paidCount, status = newStatus)
        installmentDao.updateInstallment(updatedInstallment)

        if (createTransaction) {
            val categories = categoryDao.getAllCategoriesSync()
            val installmentCatId = categories.firstOrNull { it.name.contains("اقساط") }?.id

            val tx = TransactionEntity(
                type = "EXPENSE",
                amount = item.amount,
                accountId = accountId,
                categoryId = installmentCatId,
                jalaliDate = JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString(),
                title = "پرداخت قسط شماره ${item.installmentNumber}: ${installment.title}",
                note = if (item.note.isNotBlank()) item.note else installment.note
            )
            addTransaction(tx)
        }
    }

    suspend fun payInstallment(installment: InstallmentEntity, accountId: Long, createTransaction: Boolean = true) {
        var items = installmentItemDao.getItemsForInstallmentSync(installment.id)
        if (items.isEmpty()) {
            // Auto-generate items if legacy
            val generated = (1..installment.totalInstallments).map { num ->
                InstallmentItemEntity(
                    installmentId = installment.id,
                    installmentNumber = num,
                    amount = installment.monthlyPayment,
                    isPaid = num <= installment.paidInstallments
                )
            }
            installmentItemDao.insertItems(generated)
            items = installmentItemDao.getItemsForInstallmentSync(installment.id)
        }

        val firstUnpaid = items.firstOrNull { !it.isPaid }
        if (firstUnpaid != null) {
            paySpecificInstallmentItem(installment, firstUnpaid, accountId, createTransaction)
        } else {
            val newPaidCount = installment.paidInstallments + 1
            val newStatus = if (newPaidCount >= installment.totalInstallments) "COMPLETED" else "ACTIVE"
            val updated = installment.copy(paidInstallments = newPaidCount, status = newStatus)
            installmentDao.updateInstallment(updated)

            if (createTransaction) {
                val categories = categoryDao.getAllCategoriesSync()
                val installmentCatId = categories.firstOrNull { it.name.contains("اقساط") }?.id

                val tx = TransactionEntity(
                    type = "EXPENSE",
                    amount = installment.monthlyPayment,
                    accountId = accountId,
                    categoryId = installmentCatId,
                    jalaliDate = JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString(),
                    title = "پرداخت قسط: ${installment.title} (${newPaidCount} از ${installment.totalInstallments})",
                    note = installment.note
                )
                addTransaction(tx)
            }
        }
    }

    suspend fun unpayInstallment(installment: InstallmentEntity) {
        val items = installmentItemDao.getItemsForInstallmentSync(installment.id)
        val lastPaid = items.lastOrNull { it.isPaid }
        if (lastPaid != null) {
            unpaySpecificInstallmentItem(installment, lastPaid)
        } else {
            if (installment.paidInstallments > 0) {
                val newPaidCount = installment.paidInstallments - 1
                val updated = installment.copy(
                    paidInstallments = newPaidCount,
                    status = "ACTIVE"
                )
                installmentDao.updateInstallment(updated)
            }
        }
    }

    suspend fun unpaySpecificInstallmentItem(installment: InstallmentEntity, item: InstallmentItemEntity) {
        val updatedItem = item.copy(
            isPaid = false,
            paidDateJalali = null
        )
        installmentItemDao.updateItem(updatedItem)

        val items = installmentItemDao.getItemsForInstallmentSync(installment.id)
        val paidCount = items.count { it.isPaid }
        val updatedInstallment = installment.copy(
            paidInstallments = paidCount,
            status = "ACTIVE"
        )
        installmentDao.updateInstallment(updatedInstallment)
    }

    // --- Cheques ---
    suspend fun addCheque(cheque: ChequeEntity) {
        chequeDao.insertCheque(cheque)
    }

    suspend fun updateCheque(cheque: ChequeEntity) {
        chequeDao.updateCheque(cheque)
    }

    suspend fun deleteCheque(cheque: ChequeEntity) {
        chequeDao.deleteCheque(cheque)
    }

    suspend fun unpassCheque(cheque: ChequeEntity) {
        val updated = cheque.copy(status = "PENDING", accountId = null)
        chequeDao.updateCheque(updated)
    }

    suspend fun markChequePassed(cheque: ChequeEntity, accountId: Long, createTransaction: Boolean = true) {
        val updated = cheque.copy(status = "PASSED", accountId = accountId)
        chequeDao.updateCheque(updated)

        if (createTransaction) {
            val txType = if (cheque.type == "RECEIVABLE") "INCOME" else "EXPENSE"
            val txTitle = if (cheque.type == "RECEIVABLE") {
                "وصول چک دریافتی: ${cheque.bankName} (شماره ${cheque.chequeNumber})"
            } else {
                "پاس شدن چک پرداختی: ${cheque.bankName} (شماره ${cheque.chequeNumber})"
            }

            val categories = categoryDao.getAllCategoriesSync()
            val categoryId = if (cheque.type == "RECEIVABLE") {
                categories.firstOrNull { it.type == "INCOME" && it.name.contains("سایر") }?.id
            } else {
                categories.firstOrNull { it.name.contains("چک") || it.name.contains("اقساط") }?.id
            }

            val tx = TransactionEntity(
                type = txType,
                amount = cheque.amount,
                accountId = accountId,
                categoryId = categoryId,
                jalaliDate = cheque.dueDateJalali,
                title = txTitle,
                note = "طرف حساب: ${cheque.payeeOrDrawer}"
            )
            addTransaction(tx)
        }
    }

    // --- Debts ---
    suspend fun addDebt(debt: DebtEntity): Long = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: DebtEntity) = debtDao.updateDebt(debt)
    suspend fun deleteDebt(debt: DebtEntity) = debtDao.deleteDebt(debt)

    suspend fun settleDebt(debt: DebtEntity, settleAmount: Double, accountId: Long?) {
        val newPaid = debt.paidAmount + settleAmount
        val newStatus = if (newPaid >= debt.amount) "SETTLED" else "PENDING"
        val updated = debt.copy(paidAmount = newPaid, status = newStatus)
        debtDao.updateDebt(updated)

        if (accountId != null && settleAmount > 0) {
            val categories = categoryDao.getAllCategoriesSync()
            val categoryId = if (debt.type == "RECEIVABLE") {
                categories.firstOrNull { it.name.contains("مطالبات") || it.type == "INCOME" }?.id
            } else {
                categories.firstOrNull { it.name.contains("دیون") || it.type == "EXPENSE" }?.id
            }

            val txType = if (debt.type == "RECEIVABLE") "INCOME" else "EXPENSE"
            val txTitle = if (debt.type == "RECEIVABLE") "دریافت طلب از ${debt.personName}" else "پرداخت بدهی به ${debt.personName}"

            val tx = TransactionEntity(
                type = txType,
                amount = settleAmount,
                accountId = accountId,
                categoryId = categoryId,
                jalaliDate = JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString(),
                title = txTitle,
                note = "تسویه حساب بابت: ${debt.note}"
            )
            addTransaction(tx)
        }
    }

    // Sync data getters for backup export
    suspend fun getAllAccountsSync(): List<AccountEntity> = withContext(Dispatchers.IO) { accountDao.getAllAccountsSync() }
    suspend fun getAllCategoriesSync(): List<CategoryEntity> = withContext(Dispatchers.IO) { categoryDao.getAllCategoriesSync() }
    suspend fun getAllTransactionsSync(): List<TransactionEntity> = withContext(Dispatchers.IO) { transactionDao.getAllTransactionsSync() }
    suspend fun getAllBudgetsSync(): List<BudgetEntity> = withContext(Dispatchers.IO) { budgetDao.getAllBudgetsSync() }
    suspend fun getAllInstallmentsSync(): List<InstallmentEntity> = withContext(Dispatchers.IO) { installmentDao.getAllInstallmentsSync() }
    suspend fun getAllChequesSync(): List<ChequeEntity> = withContext(Dispatchers.IO) { chequeDao.getAllChequesSync() }
    suspend fun getAllDebtsSync(): List<DebtEntity> = withContext(Dispatchers.IO) { debtDao.getAllDebtsSync() }

    // Clear and restore database during restore
    suspend fun restoreDatabase(
        accountsList: List<AccountEntity>,
        categoriesList: List<CategoryEntity>,
        transactionsList: List<TransactionEntity>,
        budgetsList: List<BudgetEntity>,
        installmentsList: List<InstallmentEntity>,
        chequesList: List<ChequeEntity>,
        debtsList: List<DebtEntity> = emptyList()
    ) = withContext(Dispatchers.IO) {
        database.clearAllTables()

        accountsList.forEach { accountDao.insertAccount(it) }
        categoriesList.forEach { categoryDao.insertCategory(it) }
        transactionsList.forEach { transactionDao.insertTransaction(it) }
        budgetsList.forEach { budgetDao.insertOrUpdateBudget(it) }
        installmentsList.forEach { installmentDao.insertInstallment(it) }
        chequesList.forEach { chequeDao.insertCheque(it) }
        debtsList.forEach { debtDao.insertDebt(it) }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        ensureDefaultCategoriesExist()
        accountDao.insertAccount(
            AccountEntity(
                name = "حساب اصلی",
                type = "BANK",
                balance = 0.0,
                accountNumber = "",
                iconName = "CreditCard",
                colorHex = "#1E40AF",
                isDefault = true
            )
        )
    }

    suspend fun ensureInitialDataSeededIfEmpty() = withContext(Dispatchers.IO) {
        val txCount = transactionDao.getAllTransactionsSync().size
        val instCount = installmentDao.getAllInstallmentsSync().size
        val chqCount = chequeDao.getAllChequesSync().size
        if (txCount == 0 && instCount == 0 && chqCount == 0) {
            resetAndSeedMockData()
        }
    }

    // Reset all app data and seed mock debug data while preserving categories
    suspend fun resetAndSeedMockData() = withContext(Dispatchers.IO) {
        // 1. Preserve existing categories
        var existingCategories = categoryDao.getAllCategoriesSync()
        if (existingCategories.isEmpty()) {
            ensureDefaultCategoriesExist()
            existingCategories = categoryDao.getAllCategoriesSync()
        }

        // 2. Clear all tables
        database.clearAllTables()

        // 3. Re-insert categories
        existingCategories.forEach { categoryDao.insertCategory(it) }
        val categories = categoryDao.getAllCategoriesSync()

        fun findCat(name: String) = categories.find { it.name == name }

        val incMain = findCat("درآمدهای اصلی و فرعی")?.id
        val salarySub = findCat("حقوق و دستمزد")?.id
        val freelanceSub = findCat("کسب‌وکار / آزاد")?.id
        val investSub = findCat("سرمایه‌گذاری")?.id
        val otherIncSub = findCat("سایر درآمدها")?.id

        val housingMain = findCat("مسکن و خانه")?.id
        val rentSub = findCat("اجاره / رهن")?.id
        val billsSub = findCat("شارژ و قبوض")?.id

        val foodMain = findCat("خوراک و سوپرمارکت")?.id
        val grocerySub = findCat("خرید روزمره")?.id
        val fruitSub = findCat("میوه و تره‌بار")?.id
        val proteinSub = findCat("پروتئین")?.id
        val snackSub = findCat("تنقلات و نوشیدنی")?.id

        val carMain = findCat("حمل‌ونقل و خودرو")?.id
        val fuelSub = findCat("سوخت")?.id
        val pubTransportSub = findCat("حمل‌ونقل عمومی")?.id
        val carServiceSub = findCat("استهلاک و سرویس")?.id

        val clothesMain = findCat("پوشاک و زیبایی")?.id
        val clothesSub = findCat("لباس و کفش")?.id

        val healthMain = findCat("سلامت و درمان")?.id
        val pharmaSub = findCat("دارو و داروخانه")?.id
        val doctorSub = findCat("پزشک و درمان")?.id

        val techMain = findCat("قبض، ارتباطات و فناوری")?.id
        val internetSub = findCat("اینترنت")?.id
        val simSub = findCat("شارژ سیم‌کارت")?.id

        val eduMain = findCat("آموزش و ارتقای شخصی")?.id
        val eduSub = findCat("دوره و آموزش")?.id

        val funMain = findCat("تفریح، سرگرمی و سفر")?.id
        val restaurantSub = findCat("رستوران و کافه")?.id

        val debtMain = findCat("اقساط، وام و بدهی")?.id
        val loanInstSub = findCat("قسط وام")?.id

        val savingsMain = findCat("پس‌انداز و سرمایه‌گذاری")?.id
        val goldSub = findCat("خرید طلا / سکه")?.id

        val otherMain = findCat("متفرقه و پیش‌بینی‌نشده")?.id
        val otherExpSub = findCat("سایر هزینه‌های خرد")?.id

        // 4. Create 2 Accounts
        val acc1Id = accountDao.insertAccount(
            AccountEntity(
                name = "کارت اصلی بانک ملی",
                accountNumber = "6037991234567890",
                type = "BANK",
                balance = 45000000.0,
                colorHex = "#0066FF",
                iconName = "CreditCard",
                isDefault = true
            )
        )

        val acc2Id = accountDao.insertAccount(
            AccountEntity(
                name = "حساب پس‌انداز سامان",
                accountNumber = "6219861098765432",
                type = "SAVINGS",
                balance = 68000000.0,
                colorHex = "#10B981",
                iconName = "AccountBalance",
                isDefault = false
            )
        )

        // Current Jalali date & Previous Jalali month
        val today = JalaliCalendarHelper.getCurrentJalaliDate()
        val cYear = today.year
        val cMonth = today.month

        val (pYear, pMonth) = if (cMonth == 1) {
            Pair(cYear - 1, 12)
        } else {
            Pair(cYear, cMonth - 1)
        }

        // 5. Create Loans requested:
        val loan1 = InstallmentEntity(
            title = "وام ۱۸۰ میلیونی",
            totalAmount = 180000000.0,
            monthlyPayment = 1636400.0,
            totalInstallments = 120,
            paidInstallments = 34,
            dueDay = 15,
            accountId = acc1Id,
            note = "وام ۱۸۰ میلیونی - تاریخ شروع ۱۵ شهریور ۱۴۰۲",
            status = "ACTIVE",
            startJalaliDate = "1402/06/15"
        )

        val loan2 = InstallmentEntity(
            title = "وام ۵۰ میلیونی",
            totalAmount = 50000000.0,
            monthlyPayment = 2000000.0,
            totalInstallments = 25,
            paidInstallments = 2,
            dueDay = 7,
            accountId = acc1Id,
            note = "وام ۵۰ میلیونی - شروع از خرداد ۱۴۰۵",
            status = "ACTIVE",
            startJalaliDate = "1405/03/07"
        )

        addInstallment(loan1)
        addInstallment(loan2)

        // 6. Create 7 Cheques
        val chequeList = (4..10).mapIndexed { idx, monthIndex ->
            ChequeEntity(
                chequeNumber = "7720${idx + 1}",
                bankName = if (idx % 2 == 0) "بانک ملی" else "بانک ملت",
                amount = 21000000.0,
                type = "PAYABLE",
                dueDateJalali = JalaliDate(cYear, monthIndex, 1).toFormattedString(),
                payeeOrDrawer = "شرکت بازرگانی پارس (چک شماره ${idx + 1})",
                status = if (idx == 0) "PASSED" else "PENDING",
                accountId = acc1Id,
                note = "چک ماهانه اقساطی ۲۱ میلیونی"
            )
        }
        chequeList.forEach { chequeDao.insertCheque(it) }

        // 7. Create Debts
        val debt1 = DebtEntity(personName = "علیرضا حسینی", type = "RECEIVABLE", amount = 8500000.0, paidAmount = 2500000.0, dueDateJalali = JalaliDate(cYear, cMonth, 20).toFormattedString(), status = "PENDING", note = "قرض‌الحسنه شخصی", createdDateJalali = JalaliDate(pYear, pMonth, 10).toFormattedString())
        val debt2 = DebtEntity(personName = "محمد محمدی (همکار)", type = "PAYABLE", amount = 4000000.0, paidAmount = 1000000.0, dueDateJalali = JalaliDate(cYear, cMonth, 26).toFormattedString(), status = "PENDING", note = "دنگ سفر کاری", createdDateJalali = JalaliDate(cYear, cMonth, 5).toFormattedString())
        listOf(debt1, debt2).forEach { debtDao.insertDebt(it) }

        // 8. Create 50 Transactions across current and previous Jalali month
        data class RawTx(val type: String, val amount: Double, val catId: Long?, val subCatId: Long?, val title: String, val note: String)

        val prevMonthTxList = listOf(
            RawTx("INCOME", 52000000.0, incMain, salarySub, "حقوق ماه قبل", "واریزی حقوق و مزایا"),
            RawTx("INCOME", 4500000.0, incMain, investSub, "سود سپرده بانکی", "واریزی بانک سامان"),
            RawTx("INCOME", 1200000.0, incMain, otherIncSub, "یارانه معیشتی", "واریزی دولت"),
            RawTx("EXPENSE", 15000000.0, housingMain, rentSub, "اجاره مسکن ماه قبل", "پرداخت اجاره‌بها"),
            RawTx("EXPENSE", 3200000.0, foodMain, grocerySub, "خرید مواد غذایی افق کوروش", "فروشگاه زنجیره‌ای"),
            RawTx("EXPENSE", 1800000.0, foodMain, fruitSub, "خرید میوه و سبزیجات", "بازار روز"),
            RawTx("EXPENSE", 2400000.0, foodMain, proteinSub, "خرید گوشت و مرغ", "قصابی مرکزی"),
            RawTx("EXPENSE", 850000.0, housingMain, billsSub, "قبض برق و گاز", "پرداخت قبض آنلاین"),
            RawTx("EXPENSE", 650000.0, techMain, internetSub, "بسته اینترنت ۳ ماهه", "همراه اول"),
            RawTx("EXPENSE", 1636400.0, debtMain, loanInstSub, "قسط وام ۱۸۰ میلیونی", "قسط ماه قبل"),
            RawTx("EXPENSE", 2000000.0, debtMain, loanInstSub, "قسط وام ۵۰ میلیونی", "قسط ماه قبل"),
            RawTx("EXPENSE", 21000000.0, debtMain, loanInstSub, "پاس شدن چک اول (تیر)", "چک ۲۱ میلیونی"),
            RawTx("EXPENSE", 1400000.0, carMain, carServiceSub, "تعویض روغن و سرویس خودرو", "تعویض روغنی صدف"),
            RawTx("EXPENSE", 920000.0, carMain, fuelSub, "بنزین و کارواش", "پمپ بنزین"),
            RawTx("EXPENSE", 3100000.0, clothesMain, clothesSub, "خرید پوشاک تابستانی", "فروشگاه پاساژ"),
            RawTx("EXPENSE", 1250000.0, healthMain, pharmaSub, "داروخانه و آزمایشگاه", "کلینیک درمان"),
            RawTx("EXPENSE", 2200000.0, funMain, restaurantSub, "شام رستوران با خانواده", "رستوران شندیز"),
            RawTx("EXPENSE", 780000.0, eduMain, eduSub, "خرید کتاب و لوازم‌التحریر", "انقلاب"),
            RawTx("EXPENSE", 450000.0, housingMain, billsSub, "شارژ ساختمان", "مدیریت مجتمع"),
            RawTx("EXPENSE", 1100000.0, foodMain, snackSub, "خرید نان و تنقلات", "خرید خرد ماهانه"),
            RawTx("EXPENSE", 850000.0, healthMain, doctorSub, "ویزیت دندانپزشکی", "مطب دکتر"),
            RawTx("EXPENSE", 1300000.0, funMain, restaurantSub, "کافه و شهربازی", "تفریح خانوادگی"),
            RawTx("TRANSFER", 8000000.0, null, null, "پس‌انداز ماه قبل", "انتقال به پس‌انداز سامان"),
            RawTx("EXPENSE", 950000.0, carMain, carServiceSub, "قطعات یدکی خودرو", "لوازم یدکی"),
            RawTx("EXPENSE", 500000.0, techMain, simSub, "شارژ اعتباری همراه", "شارژ مستقیم")
        )

        val currMonthTxList = listOf(
            RawTx("INCOME", 55000000.0, incMain, salarySub, "حقوق ماه جاری", "واریزی شرکت"),
            RawTx("INCOME", 8500000.0, incMain, freelanceSub, "پاداش و پروژه آزاد", "واریزی کارفرما"),
            RawTx("INCOME", 1200000.0, incMain, otherIncSub, "یارانه معیشتی", "واریزی دولت"),
            RawTx("INCOME", 3800000.0, incMain, investSub, "سود سپرده بانکی", "سامان"),
            RawTx("EXPENSE", 15000000.0, housingMain, rentSub, "اجاره مسکن ماه جاری", "پرداخت به صاحبخانه"),
            RawTx("EXPENSE", 3800000.0, foodMain, grocerySub, "خرید سوپرمارکت آنلاین", "اسنپ‌مارکت"),
            RawTx("EXPENSE", 2100000.0, foodMain, proteinSub, "خرید پروتئین و گوشت", "قصابی محل"),
            RawTx("EXPENSE", 1650000.0, foodMain, fruitSub, "خرید میوه و تره‌بار", "میوه فروشی"),
            RawTx("EXPENSE", 920000.0, housingMain, billsSub, "قبض آب و برق و تلفن", "سامانه قبض"),
            RawTx("EXPENSE", 480000.0, techMain, simSub, "شارژ و بسته اینترنت", "ایرانسل"),
            RawTx("EXPENSE", 1636400.0, debtMain, loanInstSub, "قسط وام ۱۸۰ میلیونی ماه جاری", "کاهش از حساب"),
            RawTx("EXPENSE", 2000000.0, debtMain, loanInstSub, "قسط وام ۵۰ میلیونی ماه جاری", "کاهش از حساب"),
            RawTx("EXPENSE", 1800000.0, carMain, fuelSub, "بنزین و عوارضی جاده", "سفر آخر هفته"),
            RawTx("EXPENSE", 2500000.0, clothesMain, clothesSub, "خرید کفش و کیف", "فروشگاه چرم"),
            RawTx("EXPENSE", 1400000.0, healthMain, pharmaSub, "داروخانه و مکمل", "داروخانه دکتر سمیعی"),
            RawTx("EXPENSE", 2800000.0, funMain, restaurantSub, "رستوران و کافه", "دورهمی دوستانه"),
            RawTx("EXPENSE", 950000.0, eduMain, eduSub, "دوره آموزشی آنلاین", "آموزش تخصصی"),
            RawTx("EXPENSE", 600000.0, foodMain, snackSub, "تنقلات و قهوه", "فروشگاه هایپرمی"),
            RawTx("EXPENSE", 1200000.0, foodMain, grocerySub, "خرید از فروشگاه جانبو", "اقلام ضروری"),
            RawTx("EXPENSE", 1750000.0, carMain, carServiceSub, "تعویض فیلتر و بالانس چرخ", "مکانیکی"),
            RawTx("EXPENSE", 890000.0, healthMain, doctorSub, "ویزیت پزشک عمومی", "درمانگاه"),
            RawTx("EXPENSE", 1500000.0, funMain, restaurantSub, "خرید تجهیزات ورزشی", "کالا ورزشی"),
            RawTx("EXPENSE", 700000.0, housingMain, billsSub, "شارژ ساختمان ماه جاری", "مدیریت مجتمع"),
            RawTx("TRANSFER", 10000000.0, null, null, "انتقال به پس‌انداز ماه جاری", "انتقال بین بانکی"),
            RawTx("EXPENSE", 550000.0, techMain, simSub, "خرید شارژ مستقیم", "پرداخت همراه")
        )

        val baseTime = System.currentTimeMillis() - (60L * 24 * 3600 * 1000)

        prevMonthTxList.forEachIndexed { i, tx ->
            val day = (i + 1).coerceAtMost(28)
            val jDate = JalaliDate(pYear, pMonth, day).toFormattedString()
            val ts = baseTime + (i * 24 * 3600 * 1000L)
            val accId = if (tx.type == "TRANSFER") acc1Id else (if (i % 2 == 0) acc1Id else acc2Id)
            val targetAccId = if (tx.type == "TRANSFER") acc2Id else null

            transactionDao.insertTransaction(
                TransactionEntity(
                    type = tx.type,
                    amount = tx.amount,
                    accountId = accId,
                    targetAccountId = targetAccId,
                    categoryId = tx.catId,
                    subcategoryId = tx.subCatId,
                    timestamp = ts,
                    jalaliDate = jDate,
                    title = tx.title,
                    note = tx.note
                )
            )
        }

        currMonthTxList.forEachIndexed { i, tx ->
            val day = (i + 1).coerceAtMost(28)
            val jDate = JalaliDate(cYear, cMonth, day).toFormattedString()
            val ts = baseTime + ((25 + i) * 24 * 3600 * 1000L)
            val accId = if (tx.type == "TRANSFER") acc1Id else (if (i % 3 == 0) acc2Id else acc1Id)
            val targetAccId = if (tx.type == "TRANSFER") acc2Id else null

            transactionDao.insertTransaction(
                TransactionEntity(
                    type = tx.type,
                    amount = tx.amount,
                    accountId = accId,
                    targetAccountId = targetAccId,
                    categoryId = tx.catId,
                    subcategoryId = tx.subCatId,
                    timestamp = ts,
                    jalaliDate = jDate,
                    title = tx.title,
                    note = tx.note
                )
            )
        }
    }
}
