package com.example.util

import com.example.data.local.*
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreHelper {

    fun exportToJson(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        installments: List<InstallmentEntity>,
        cheques: List<ChequeEntity>,
        debts: List<DebtEntity> = emptyList(),
        currencyUnit: CurrencyUnit
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "ExpenseTracker")
        root.put("currencyUnit", currencyUnit.name)
        root.put("exportDate", JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString())

        // Accounts
        val accArray = JSONArray()
        accounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("accountNumber", acc.accountNumber)
                put("type", acc.type)
                put("balance", acc.balance)
                put("colorHex", acc.colorHex)
                put("iconName", acc.iconName)
                put("isDefault", acc.isDefault)
            }
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        // Categories
        val catArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("type", cat.type)
                put("iconName", cat.iconName)
                put("colorHex", cat.colorHex)
                put("isSystem", cat.isSystem)
                if (cat.parentId != null) put("parentId", cat.parentId)
            }
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Transactions
        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("amount", tx.amount)
                put("accountId", tx.accountId)
                if (tx.targetAccountId != null) put("targetAccountId", tx.targetAccountId)
                if (tx.categoryId != null) put("categoryId", tx.categoryId)
                if (tx.subcategoryId != null) put("subcategoryId", tx.subcategoryId)
                put("timestamp", tx.timestamp)
                put("jalaliDate", tx.jalaliDate)
                put("title", tx.title)
                put("note", tx.note)
                put("transferFee", tx.transferFee)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Budgets
        val bgArray = JSONArray()
        budgets.forEach { bg ->
            val obj = JSONObject().apply {
                put("id", bg.id)
                if (bg.categoryId != null) put("categoryId", bg.categoryId)
                put("monthlyLimit", bg.monthlyLimit)
                put("jalaliYearMonth", bg.jalaliYearMonth)
            }
            bgArray.put(obj)
        }
        root.put("budgets", bgArray)

        // Installments
        val instArray = JSONArray()
        installments.forEach { inst ->
            val obj = JSONObject().apply {
                put("id", inst.id)
                put("title", inst.title)
                put("totalAmount", inst.totalAmount)
                put("monthlyPayment", inst.monthlyPayment)
                put("totalInstallments", inst.totalInstallments)
                put("paidInstallments", inst.paidInstallments)
                put("dueDay", inst.dueDay)
                if (inst.accountId != null) put("accountId", inst.accountId)
                put("note", inst.note)
                put("status", inst.status)
            }
            instArray.put(obj)
        }
        root.put("installments", instArray)

        // Cheques
        val chkArray = JSONArray()
        cheques.forEach { chk ->
            val obj = JSONObject().apply {
                put("id", chk.id)
                put("chequeNumber", chk.chequeNumber)
                put("bankName", chk.bankName)
                put("amount", chk.amount)
                put("type", chk.type)
                put("dueDateJalali", chk.dueDateJalali)
                put("payeeOrDrawer", chk.payeeOrDrawer)
                put("status", chk.status)
                if (chk.accountId != null) put("accountId", chk.accountId)
                put("note", chk.note)
            }
            chkArray.put(obj)
        }
        root.put("cheques", chkArray)

        // Debts
        val debtArray = JSONArray()
        debts.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("personName", d.personName)
                put("type", d.type)
                put("amount", d.amount)
                put("paidAmount", d.paidAmount)
                put("dueDateJalali", d.dueDateJalali)
                put("status", d.status)
                put("note", d.note)
                put("createdDateJalali", d.createdDateJalali)
            }
            debtArray.put(obj)
        }
        root.put("debts", debtArray)

        return root.toString(2)
    }

    data class BackupData(
        val currencyUnit: CurrencyUnit,
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>,
        val installments: List<InstallmentEntity>,
        val cheques: List<ChequeEntity>,
        val debts: List<DebtEntity> = emptyList()
    )

    fun importFromJson(jsonStr: String): BackupData {
        val root = JSONObject(jsonStr)
        val currUnitStr = root.optString("currencyUnit", "TOMAN")
        val currencyUnit = try { CurrencyUnit.valueOf(currUnitStr) } catch (e: Exception) { CurrencyUnit.TOMAN }

        // Accounts
        val accounts = mutableListOf<AccountEntity>()
        val accArray = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accArray.length()) {
            val obj = accArray.getJSONObject(i)
            accounts.add(
                AccountEntity(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", "حساب"),
                    accountNumber = obj.optString("accountNumber", ""),
                    type = obj.optString("type", "BANK"),
                    balance = obj.optDouble("balance", 0.0),
                    colorHex = obj.optString("colorHex", "#3B82F6"),
                    iconName = obj.optString("iconName", "CreditCard"),
                    isDefault = obj.optBoolean("isDefault", false)
                )
            )
        }

        // Categories
        val categories = mutableListOf<CategoryEntity>()
        val catArray = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArray.length()) {
            val obj = catArray.getJSONObject(i)
            categories.add(
                CategoryEntity(
                    id = obj.optLong("id", 0),
                    name = obj.optString("name", "دسته"),
                    type = obj.optString("type", "EXPENSE"),
                    iconName = obj.optString("iconName", "Category"),
                    colorHex = obj.optString("colorHex", "#6B7280"),
                    isSystem = obj.optBoolean("isSystem", false),
                    parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.optLong("parentId") else null
                )
            )
        }

        // Transactions
        val transactions = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            transactions.add(
                TransactionEntity(
                    id = obj.optLong("id", 0),
                    type = obj.optString("type", "EXPENSE"),
                    amount = obj.optDouble("amount", 0.0),
                    accountId = obj.optLong("accountId", 1),
                    targetAccountId = if (obj.has("targetAccountId") && !obj.isNull("targetAccountId")) obj.optLong("targetAccountId") else null,
                    categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.optLong("categoryId") else null,
                    subcategoryId = if (obj.has("subcategoryId") && !obj.isNull("subcategoryId")) obj.optLong("subcategoryId") else null,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    jalaliDate = obj.optString("jalaliDate", JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()),
                    title = obj.optString("title", "تراکنش"),
                    note = obj.optString("note", ""),
                    transferFee = obj.optDouble("transferFee", 0.0)
                )
            )
        }

        // Budgets
        val budgets = mutableListOf<BudgetEntity>()
        val bgArray = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until bgArray.length()) {
            val obj = bgArray.getJSONObject(i)
            budgets.add(
                BudgetEntity(
                    id = obj.optLong("id", 0),
                    categoryId = if (obj.has("categoryId") && !obj.isNull("categoryId")) obj.optLong("categoryId") else null,
                    monthlyLimit = obj.optDouble("monthlyLimit", 0.0),
                    jalaliYearMonth = obj.optString("jalaliYearMonth", JalaliCalendarHelper.getCurrentJalaliYearMonth())
                )
            )
        }

        // Installments
        val installments = mutableListOf<InstallmentEntity>()
        val instArray = root.optJSONArray("installments") ?: JSONArray()
        for (i in 0 until instArray.length()) {
            val obj = instArray.getJSONObject(i)
            installments.add(
                InstallmentEntity(
                    id = obj.optLong("id", 0),
                    title = obj.optString("title", "وام"),
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    monthlyPayment = obj.optDouble("monthlyPayment", 0.0),
                    totalInstallments = obj.optInt("totalInstallments", 12),
                    paidInstallments = obj.optInt("paidInstallments", 0),
                    dueDay = obj.optInt("dueDay", 1),
                    accountId = if (obj.has("accountId") && !obj.isNull("accountId")) obj.optLong("accountId") else null,
                    note = obj.optString("note", ""),
                    status = obj.optString("status", "ACTIVE")
                )
            )
        }

        // Cheques
        val cheques = mutableListOf<ChequeEntity>()
        val chkArray = root.optJSONArray("cheques") ?: JSONArray()
        for (i in 0 until chkArray.length()) {
            val obj = chkArray.getJSONObject(i)
            cheques.add(
                ChequeEntity(
                    id = obj.optLong("id", 0),
                    chequeNumber = obj.optString("chequeNumber", ""),
                    bankName = obj.optString("bankName", "بانک"),
                    amount = obj.optDouble("amount", 0.0),
                    type = obj.optString("type", "PAYABLE"),
                    dueDateJalali = obj.optString("dueDateJalali", JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()),
                    payeeOrDrawer = obj.optString("payeeOrDrawer", ""),
                    status = obj.optString("status", "PENDING"),
                    accountId = if (obj.has("accountId") && !obj.isNull("accountId")) obj.optLong("accountId") else null,
                    note = obj.optString("note", "")
                )
            )
        }

        // Debts
        val debts = mutableListOf<DebtEntity>()
        val dbtArray = root.optJSONArray("debts") ?: JSONArray()
        for (i in 0 until dbtArray.length()) {
            val obj = dbtArray.getJSONObject(i)
            debts.add(
                DebtEntity(
                    id = obj.optLong("id", 0),
                    personName = obj.optString("personName", ""),
                    type = obj.optString("type", "PAYABLE"),
                    amount = obj.optDouble("amount", 0.0),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    dueDateJalali = obj.optString("dueDateJalali", ""),
                    status = obj.optString("status", "PENDING"),
                    note = obj.optString("note", ""),
                    createdDateJalali = obj.optString("createdDateJalali", "")
                )
            )
        }

        return BackupData(currencyUnit, accounts, categories, transactions, budgets, installments, cheques, debts)
    }
}
