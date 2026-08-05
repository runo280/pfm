package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        InstallmentEntity::class,
        InstallmentItemEntity::class,
        ChequeEntity::class,
        DebtEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun installmentItemDao(): InstallmentItemDao
    abstract fun chequeDao(): ChequeDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_tracker_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(getDatabase(context))
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                // Initial Accounts
                val accountDao = database.accountDao()
                accountDao.insertAccount(
                    AccountEntity(
                        name = "کارت بانکی",
                        accountNumber = "",
                        type = "BANK",
                        balance = 0.0,
                        colorHex = "#1E40AF",
                        iconName = "CreditCard",
                        isDefault = true
                    )
                )

                // Initial Categories & Subcategories
                val categoryDao = database.categoryDao()
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
    }
}
