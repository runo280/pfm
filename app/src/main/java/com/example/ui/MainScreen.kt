package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.local.TransactionEntity
import com.example.ui.screens.*

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.util.CurrencyHelper
import com.example.util.JalaliCalendarHelper
import androidx.compose.ui.graphics.Color

sealed class BottomNavTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavTab("dashboard", "خانه", Icons.Default.Home)
    object CalendarTab : BottomNavTab("calendar", "تقویم", Icons.Default.CalendarMonth)
    object Transactions : BottomNavTab("transactions", "تراکنش‌ها", Icons.Default.Receipt)
    object Accounts : BottomNavTab("accounts", "حساب‌ها", Icons.Default.CreditCard)
    object Installments : BottomNavTab("installments", "اقساط و چک", Icons.Default.Payments)
    object Analytics : BottomNavTab("analytics", "آمار", Icons.Default.PieChart)
    object Settings : BottomNavTab("settings", "تنظیمات", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val installments by viewModel.installments.collectAsState()
    val cheques by viewModel.cheques.collectAsState()
    val debts by viewModel.debts.collectAsState()

    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    val themePrimaryColor by viewModel.themePrimaryColor.collectAsState()
    val dueReminderItems by viewModel.dueReminderItems.collectAsState()
    var hasDismissedDueReminder by rememberSaveable { mutableStateOf(false) }

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val appPin by viewModel.appPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()

    var currentTab by remember { mutableStateOf<BottomNavTab>(BottomNavTab.Dashboard) }

    val currentJalali = remember { JalaliCalendarHelper.getCurrentJalaliDate() }
    val installmentsAndChequesBadgeCount = remember(installments, cheques, currentJalali) {
        val activeInstallmentsCount = installments.count { inst ->
            if (inst.status == "ACTIVE" && inst.paidInstallments < inst.totalInstallments) {
                val nextDueDate = JalaliCalendarHelper.getInstallmentNextDueDate(inst, currentJalali)
                nextDueDate.year < currentJalali.year || (nextDueDate.year == currentJalali.year && nextDueDate.month <= currentJalali.month)
            } else false
        }
        val currentMonthChequesCount = cheques.count { chk ->
            chk.status == "PENDING" && run {
                val p = JalaliCalendarHelper.parseJalaliDate(chk.dueDateJalali)
                p != null && (p.year < currentJalali.year || (p.year == currentJalali.year && p.month <= currentJalali.month))
            }
        }
        activeInstallmentsCount + currentMonthChequesCount
    }

    // Dialog trigger states from Dashboard quick actions
    var quickTransactionType by remember { mutableStateOf<String?>(null) } // "EXPENSE" or "INCOME"
    var showQuickTransferDialog by remember { mutableStateOf(false) }
    var editingTransactionFromDashboard by remember { mutableStateOf<TransactionEntity?>(null) }

    // Listen to user feedback messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Request notification permission if needed on Android 13+ (API 33+)
    NotificationPermissionHandler()

    // Force RTL layout for Persian language UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    val tabs = listOf(
                        BottomNavTab.Dashboard,
                        BottomNavTab.CalendarTab,
                        BottomNavTab.Transactions,
                        BottomNavTab.Installments,
                        BottomNavTab.Analytics,
                        BottomNavTab.Settings
                    )

                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                if (tab == BottomNavTab.Installments && installmentsAndChequesBadgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) {
                                                Text(
                                                    text = JalaliCalendarHelper.toPersianDigits(installmentsAndChequesBadgeCount),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.title)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            },
                            label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    BottomNavTab.Dashboard -> {
                        DashboardScreen(
                            accounts = accounts,
                            categories = categories,
                            transactions = transactions,
                            currencyUnit = currencyUnit,
                            onAddTransaction = { viewModel.addTransaction(it) },
                            onTransfer = { fromId, toId, amt, fee, note ->
                                viewModel.transferBetweenAccounts(fromId, toId, amt, fee, note)
                            }
                        )
                    }
                    BottomNavTab.CalendarTab -> {
                        CalendarScreen(
                            transactions = transactions,
                            installments = installments,
                            cheques = cheques,
                            debts = debts,
                            categories = categories,
                            accounts = accounts,
                            currencyUnit = currencyUnit
                        )
                    }
                    BottomNavTab.Transactions -> {
                        TransactionsScreen(
                            transactions = transactions,
                            accounts = accounts,
                            categories = categories,
                            currencyUnit = currencyUnit,
                            onAddTransaction = { viewModel.addTransaction(it) },
                            onUpdateTransaction = { oldTx, newTx -> viewModel.updateTransaction(oldTx, newTx) },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            editingTx = editingTransactionFromDashboard,
                            onDismissEdit = { editingTransactionFromDashboard = null },
                            onExportPdf = { filteredList -> viewModel.exportFilteredPdf(context, filteredList) },
                            onExportCsv = { filteredList -> viewModel.exportFilteredCsv(context, filteredList) }
                        )
                    }
                    BottomNavTab.Accounts -> {
                        AccountsScreen(
                            accounts = accounts,
                            currencyUnit = currencyUnit,
                            onAddAccount = { viewModel.addAccount(it) },
                            onUpdateAccount = { viewModel.updateAccount(it) },
                            onDeleteAccount = { viewModel.deleteAccount(it) },
                            onTransfer = { fromId, toId, amt, fee, note ->
                                viewModel.transferBetweenAccounts(fromId, toId, amt, fee, note)
                            },
                            onSetDefaultAccount = { viewModel.setDefaultAccount(it) }
                        )
                    }
                    BottomNavTab.Installments -> {
                        InstallmentsChequesScreen(
                            installments = installments,
                            cheques = cheques,
                            debts = debts,
                            accounts = accounts,
                            currencyUnit = currencyUnit,
                            onAddInstallment = { inst, customItems -> viewModel.addInstallment(inst, customItems) },
                            onUpdateInstallment = { viewModel.updateInstallment(it) },
                            onDeleteInstallment = { viewModel.deleteInstallment(it) },
                            onPayInstallment = { inst, accountId, createTx -> viewModel.payInstallment(inst, accountId, createTx) },
                            onUnpayInstallment = { inst -> viewModel.unpayInstallment(inst) },
                            onGetInstallmentItems = { instId -> viewModel.getInstallmentItems(instId) },
                            onUpdateInstallmentItem = { item -> viewModel.updateInstallmentItem(item) },
                            onPaySpecificInstallmentItem = { inst, item, accId, createTx -> viewModel.paySpecificInstallmentItem(inst, item, accId, createTx) },
                            onUnpaySpecificInstallmentItem = { inst, item -> viewModel.unpaySpecificInstallmentItem(inst, item) },
                            onAddCheque = { viewModel.addCheque(it) },
                            onUpdateCheque = { viewModel.updateCheque(it) },
                            onDeleteCheque = { viewModel.deleteCheque(it) },
                            onMarkChequePassed = { cheque, accountId, createTx -> viewModel.markChequePassed(cheque, accountId, createTx) },
                            onUnpassCheque = { viewModel.unpassCheque(it) },
                            onAddDebt = { viewModel.addDebt(it) },
                            onUpdateDebt = { viewModel.updateDebt(it) },
                            onDeleteDebt = { viewModel.deleteDebt(it) },
                            onSettleDebt = { debt, amount, accId -> viewModel.settleDebt(debt, amount, accId) }
                        )
                    }
                    BottomNavTab.Analytics -> {
                        AnalyticsScreen(
                            transactions = transactions,
                            categories = categories,
                            accounts = accounts,
                            selectedMonth = selectedMonth,
                            currencyUnit = currencyUnit,
                            onMonthChanged = { viewModel.setSelectedMonth(it) }
                        )
                    }
                    BottomNavTab.Settings -> {
                        SettingsScreen(
                            currencyUnit = currencyUnit,
                            accounts = accounts,
                            categories = categories,
                            transactions = transactions,
                            reminderHour = reminderHour,
                            themeMode = themeMode,
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            themePrimaryColor = themePrimaryColor,
                            onThemePrimaryColorChange = { viewModel.setThemePrimaryColor(it) },
                            isAppLockEnabled = isAppLockEnabled,
                            onAppLockToggle = { viewModel.setAppLockEnabled(it) },
                            appPin = appPin,
                            onAppPinChange = { viewModel.setAppPin(it) },
                            isBiometricEnabled = isBiometricEnabled,
                            onBiometricToggle = { viewModel.setBiometricEnabled(it) },
                            onCurrencyUnitChange = { viewModel.setCurrencyUnit(it) },
                            onSetDefaultAccount = { viewModel.setDefaultAccount(it) },
                            onSetReminderHour = { viewModel.setReminderHour(it) },
                            onAddCategory = { viewModel.addCategory(it) },
                            onUpdateCategory = { viewModel.updateCategory(it) },
                            onDeleteCategory = { viewModel.deleteCategory(it) },
                            onDeleteCategoryWithOption = { cat, delTxs, targetMainId, targetSubId ->
                                viewModel.deleteCategoryWithHandling(cat, delTxs, targetMainId, targetSubId)
                            },
                            onAddAccount = { viewModel.addAccount(it) },
                            onUpdateAccount = { viewModel.updateAccount(it) },
                            onDeleteAccount = { viewModel.deleteAccount(it) },
                            onTransfer = { fromId, toId, amt, fee, note ->
                                viewModel.transferBetweenAccounts(fromId, toId, amt, fee, note)
                            },
                            onBackupData = { uri -> viewModel.exportBackupJson(context, uri) },
                            onShareBackupData = { viewModel.shareBackupJson(context) },
                            onRestoreData = { uri -> viewModel.importBackupJson(context, uri) },
                            onResetAndSeedMockData = { viewModel.resetAndSeedMockData() },
                            onClearAllData = { viewModel.clearAllAppData() }
                        )
                    }
                }
            }
        }

        // App Security Lock Overlay
        if (isAppLockEnabled && !isAppUnlocked) {
            com.example.ui.components.SecurityLockOverlay(
                correctPin = appPin,
                isBiometricEnabled = isBiometricEnabled,
                onUnlock = { viewModel.unlockApp() }
            )
        }

        // Quick add transaction dialog from Dashboard
        if (quickTransactionType != null) {
            AddEditTransactionDialog(
                transaction = null,
                accounts = accounts,
                categories = categories,
                currencyUnit = currencyUnit,
                onDismiss = { quickTransactionType = null },
                onSave = { newTx ->
                    viewModel.addTransaction(newTx.copy(type = quickTransactionType!!))
                    quickTransactionType = null
                }
            )
        }

        // Quick transfer dialog from Dashboard
        if (showQuickTransferDialog) {
            TransferDialog(
                accounts = accounts,
                currencyUnit = currencyUnit,
                onDismiss = { showQuickTransferDialog = false },
                onTransfer = { fromId, toId, amt, fee, note ->
                    viewModel.transferBetweenAccounts(fromId, toId, amt, fee, note)
                    showQuickTransferDialog = false
                }
            )
        }

        // Due Items Reminder Dialog on App Launch
        if (dueReminderItems.isNotEmpty() && !hasDismissedDueReminder) {
            AlertDialog(
                onDismissRequest = { hasDismissedDueReminder = true },
                icon = {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "یادآوری سررسید اقساط و چک‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "موعد پرداخت یا دریافت سررسیدهای زیر امروز یا فردا می‌باشد:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dueReminderItems) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item.isToday)
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                    else
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "[${item.typeName}] ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "مبلغ: ${CurrencyHelper.formatAmount(item.amount, currencyUnit)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (item.isToday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = item.dueText,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { hasDismissedDueReminder = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("متوجه شدم")
                    }
                }
            )
        }
    }
}

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionHandler() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showRationaleDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                Toast.makeText(context, "دسترسی اعلان‌ها با موفقیت فعال شد.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "دسترسی ارسال اعلان اعطا نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val activity = context.findActivity()
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                showRationaleDialog = true
            } else {
                try {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "درخواست دسترسی به اعلان‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "برای دریافت به موقع یادآوری‌های سررسید چک‌ها، اقساط، و گزارش‌های روزانه مالی، لطفاً دسترسی ارسال اعلان را فعال کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        try {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("فعالسازی دسترسی")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationaleDialog = false }
                ) {
                    Text("بعداً")
                }
            }
        )
    }
}
