package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountNumber: String = "",
    val type: String = "BANK", // BANK, CASH, SAVINGS, CREDIT
    val balance: Double = 0.0, // stored in Toman
    val colorHex: String = "#3B82F6",
    val iconName: String = "CreditCard",
    val isDefault: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // EXPENSE, INCOME
    val iconName: String = "Category",
    val colorHex: String = "#6B7280",
    val isSystem: Boolean = false,
    val parentId: Long? = null
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val amount: Double, // in Toman
    val accountId: Long,
    val targetAccountId: Long? = null, // for TRANSFER
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val jalaliDate: String, // format YYYY/MM/DD
    val title: String,
    val note: String = "",
    val transferFee: Double = 0.0
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null, // null means overall monthly budget
    val monthlyLimit: Double,
    val jalaliYearMonth: String // format YYYY/MM
)

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val monthlyPayment: Double,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val dueDay: Int = 1, // day of Jalali month
    val accountId: Long? = null,
    val note: String = "",
    val reminderDaysBefore: Int = 3,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    val startJalaliDate: String = "" // format YYYY/MM/DD
)

@Entity(tableName = "installment_items")
data class InstallmentItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val installmentId: Long,
    val installmentNumber: Int,
    val amount: Double,
    val isPaid: Boolean = false,
    val paidDateJalali: String? = null,
    val note: String = ""
)

@Entity(tableName = "cheques")
data class ChequeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chequeNumber: String,
    val bankName: String,
    val amount: Double,
    val type: String, // RECEIVABLE (دریافتی), PAYABLE (پرداختی)
    val dueDateJalali: String, // YYYY/MM/DD
    val payeeOrDrawer: String,
    val status: String = "PENDING", // PENDING (پاس نشده), PASSED (پاس شده), BOUNCED (برگشتی)
    val accountId: Long? = null,
    val note: String = "",
    val reminderDaysBefore: Int = 3
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val type: String, // RECEIVABLE (طلب/طلبکاری - من طلبکارم), PAYABLE (بدهی/بدهکاری - من بدهکارم)
    val amount: Double,
    val paidAmount: Double = 0.0,
    val dueDateJalali: String = "",
    val status: String = "PENDING", // PENDING (جاری), SETTLED (تسویه شده)
    val note: String = "",
    val createdDateJalali: String = ""
)
