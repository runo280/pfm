package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAllAccountsSync(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, delta: Double)

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :accountId")
    suspend fun setDefault(accountId: Long)

    @Transaction
    suspend fun updateDefaultAccount(accountId: Long) {
        clearAllDefaults()
        setDefault(accountId)
    }
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE jalaliDate LIKE :yearMonth || '%' ORDER BY timestamp DESC")
    fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE jalaliYearMonth = :yearMonth")
    fun getBudgetsForMonth(yearMonth: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSync(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY id DESC")
    fun getAllInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments ORDER BY id DESC")
    suspend fun getAllInstallmentsSync(): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallmentById(id: Long): InstallmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Delete
    suspend fun deleteInstallment(installment: InstallmentEntity)
}

@Dao
interface InstallmentItemDao {
    @Query("SELECT * FROM installment_items WHERE installmentId = :installmentId ORDER BY installmentNumber ASC")
    fun getItemsForInstallment(installmentId: Long): Flow<List<InstallmentItemEntity>>

    @Query("SELECT * FROM installment_items WHERE installmentId = :installmentId ORDER BY installmentNumber ASC")
    suspend fun getItemsForInstallmentSync(installmentId: Long): List<InstallmentItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InstallmentItemEntity>)

    @Update
    suspend fun updateItem(item: InstallmentItemEntity)

    @Query("DELETE FROM installment_items WHERE installmentId = :installmentId")
    suspend fun deleteItemsForInstallment(installmentId: Long)
}

@Dao
interface ChequeDao {
    @Query("SELECT * FROM cheques ORDER BY dueDateJalali ASC")
    fun getAllCheques(): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques ORDER BY dueDateJalali ASC")
    suspend fun getAllChequesSync(): List<ChequeEntity>

    @Query("SELECT * FROM cheques WHERE id = :id")
    suspend fun getChequeById(id: Long): ChequeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheque(cheque: ChequeEntity): Long

    @Update
    suspend fun updateCheque(cheque: ChequeEntity)

    @Delete
    suspend fun deleteCheque(cheque: ChequeEntity)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY status ASC, id DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY status ASC, id DESC")
    suspend fun getAllDebtsSync(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)
}
