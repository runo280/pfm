package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.ui.BottomNavTab
import com.example.ui.theme.ExpenseRed
import com.example.util.CurrencyUnit
import com.example.util.JalaliCalendarHelper

data class ToolGridItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    currencyUnit: CurrencyUnit,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    reminderHour: Int,
    themeMode: String = "SYSTEM",
    onThemeModeChange: (String) -> Unit = {},
    themePrimaryColor: String = "BLUE",
    onThemePrimaryColorChange: (String) -> Unit = {},
    isAppLockEnabled: Boolean = false,
    onAppLockToggle: (Boolean) -> Unit = {},
    appPin: String = "",
    onAppPinChange: (String) -> Unit = {},
    isBiometricEnabled: Boolean = false,
    onBiometricToggle: (Boolean) -> Unit = {},
    onCurrencyUnitChange: (CurrencyUnit) -> Unit,
    onSetDefaultAccount: (Long) -> Unit,
    onSetReminderHour: (Int) -> Unit,
    onAddCategory: (CategoryEntity) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDeleteCategoryWithOption: (CategoryEntity, Boolean, Long?, Long?) -> Unit = { _, _, _, _ -> },
    onAddAccount: (AccountEntity) -> Unit = {},
    onUpdateAccount: (AccountEntity) -> Unit = {},
    onDeleteAccount: (AccountEntity) -> Unit = {},
    onTransfer: (Long, Long, Double, Double, String) -> Unit = { _, _, _, _, _ -> },
    onBackupData: (Uri) -> Unit,
    onShareBackupData: () -> Unit = {},
    onRestoreData: (Uri) -> Unit,
    onResetAndSeedMockData: () -> Unit = {},
    onClearAllData: () -> Unit = {},
    onNavigateToTab: (BottomNavTab, Int?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    var showGeneralSettingsDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showResetMockDataDialog by remember { mutableStateOf(false) }
    var showClearAllDataDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // PIN Setup Dialog state
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    // Backup & Restore activity launchers
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onBackupData(uri)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onRestoreData(uri)
        }
    }

    val toolsList = remember {
        listOf(
            ToolGridItem(
                id = "settings",
                title = "تنظیمات",
                icon = Icons.Default.Settings,
                iconColor = Color(0xFF3B82F6),
                onClick = { showGeneralSettingsDialog = true }
            ),
            ToolGridItem(
                id = "installments",
                title = "اقساط و وام",
                icon = Icons.Default.Payments,
                iconColor = Color(0xFF0284C7),
                onClick = { onNavigateToTab(BottomNavTab.Installments, 0) }
            ),
            ToolGridItem(
                id = "cheques",
                title = "چک‌ها",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconColor = Color(0xFF8B5CF6),
                onClick = { onNavigateToTab(BottomNavTab.Installments, 1) }
            ),
            ToolGridItem(
                id = "debts",
                title = "طلب و بدهی",
                icon = Icons.Default.FolderShared,
                iconColor = Color(0xFFD97706),
                onClick = { onNavigateToTab(BottomNavTab.Installments, 2) }
            ),
            ToolGridItem(
                id = "accounts",
                title = "مدیریت حساب‌ها",
                icon = Icons.Default.CreditCard,
                iconColor = Color(0xFF10B981),
                onClick = { onNavigateToTab(BottomNavTab.Accounts, null) }
            ),
            ToolGridItem(
                id = "backup",
                title = "پشتیبان‌گیری و بازگردانی",
                icon = Icons.Default.Backup,
                iconColor = Color(0xFF06B6D4),
                onClick = { showBackupRestoreDialog = true }
            ),
            ToolGridItem(
                id = "categories",
                title = "مدیریت دسته‌بندی‌ها",
                icon = Icons.Default.Category,
                iconColor = Color(0xFFEC4899),
                onClick = { showCategoryManagerDialog = true }
            ),
            ToolGridItem(
                id = "about",
                title = "درباره برنامه",
                icon = Icons.Default.Info,
                iconColor = Color(0xFF64748B),
                onClick = { showAboutDialog = true }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "ابزارها و امکانات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "دسترسی سریع به ابزارهای مالی و تنظیمات برنامه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3-Column Grid Layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(toolsList, key = { it.id }) { item ->
                Card(
                    onClick = item.onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = item.iconColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }

    // General Settings Dialog
    if (showGeneralSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showGeneralSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنظیمات عمومی", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Currency Unit
                    Column {
                        Text("واحد پول نمایش مبالغ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = currencyUnit == CurrencyUnit.TOMAN,
                                onClick = { onCurrencyUnitChange(CurrencyUnit.TOMAN) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2)
                            ) {
                                Text("تومان")
                            }
                            SegmentedButton(
                                selected = currencyUnit == CurrencyUnit.RIAL,
                                onClick = { onCurrencyUnitChange(CurrencyUnit.RIAL) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2)
                            ) {
                                Text("ریال")
                            }
                        }
                    }

                    HorizontalDivider()

                    // Theme Mode
                    Column {
                        Text("پوسته و حالت روز/شب", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = themeMode == "SYSTEM",
                                onClick = { onThemeModeChange("SYSTEM") },
                                label = { Text("پیش‌فرض", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = themeMode == "LIGHT",
                                onClick = { onThemeModeChange("LIGHT") },
                                label = { Text("روز", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = themeMode == "DARK",
                                onClick = { onThemeModeChange("DARK") },
                                label = { Text("شب", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Security Lock
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("قفل ورود به برنامه (PIN)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (isAppLockEnabled) "رمز عبور فعال است" else "رمز عبور غیرفعال است",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = isAppLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && appPin.isEmpty()) {
                                        showPinSetupDialog = true
                                    } else {
                                        onAppLockToggle(enabled)
                                    }
                                }
                            )
                        }

                        if (isAppLockEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showPinSetupDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تغییر رمز PIN")
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ورود با اثر انگشت", style = MaterialTheme.typography.bodySmall)
                                    Checkbox(
                                        checked = isBiometricEnabled,
                                        onCheckedChange = { onBiometricToggle(it) }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Reminder Hour
                    Column {
                        Text("ساعت یادآوری روزانه ثبت تراکنش", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        var reminderExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = reminderExpanded,
                            onExpandedChange = { reminderExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "ساعت ${JalaliCalendarHelper.toPersianDigits(reminderHour)}:00",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = reminderExpanded,
                                onDismissRequest = { reminderExpanded = false }
                            ) {
                                (8..22).forEach { hour ->
                                    DropdownMenuItem(
                                        text = { Text("ساعت ${JalaliCalendarHelper.toPersianDigits(hour)}:00") },
                                        onClick = {
                                            onSetReminderHour(hour)
                                            reminderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Reset & Clear Data Actions
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("مدیریت داده‌ها", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        if (com.example.BuildConfig.DEBUG) {
                            OutlinedButton(
                                onClick = {
                                    showGeneralSettingsDialog = false
                                    showResetMockDataDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بازنشانی و تزریق داده‌های نمونه (دیباگ)")
                            }
                        }
                        Button(
                            onClick = {
                                showGeneralSettingsDialog = false
                                showClearAllDataDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف تمامی داده‌های برنامه")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGeneralSettingsDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تأیید و بستن")
                }
            }
        )
    }

    // Backup & Restore Dialog
    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            icon = {
                Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("پشتیبان‌گیری و بازگردانی اطلاعات", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "می‌توانید تمام اطلاعات تراکنش‌ها، حساب‌ها، اقساط و چک‌های خود را به‌صورت یک فایل استاندارد JSON ذخیره کرده و در صورت نیاز بازگردانی نمایید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Button(
                        onClick = {
                            try {
                                backupLauncher.launch("ExpenseTracker_Backup_${JalaliCalendarHelper.getCurrentJalaliDateTimeString()}.json")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onShareBackupData()
                            }
                            showBackupRestoreDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ذخیره فایل پشتیبان JSON")
                    }

                    OutlinedButton(
                        onClick = {
                            onShareBackupData()
                            showBackupRestoreDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اشتراک‌گذاری فایل پشتیبان")
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                restoreLauncher.launch("*/*")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "دستگاه امکان انتخاب فایل پشتیبان را ندارد.", Toast.LENGTH_LONG).show()
                            }
                            showBackupRestoreDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بازگردانی اطلاعات از فایل پشتیبان")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupRestoreDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    // Category Manager Modal
    if (showCategoryManagerDialog) {
        CategoryManagerDialog(
            categories = categories,
            transactions = transactions,
            onDismiss = { showCategoryManagerDialog = false },
            onAddCategory = onAddCategory,
            onUpdateCategory = onUpdateCategory,
            onDeleteCategoryWithOption = onDeleteCategoryWithOption
        )
    }

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("تنظیم رمز عبور ۴ رقمی (PIN)") },
            text = {
                Column {
                    Text("یک رمز عبور ۴ رقمی جهت ورود به برنامه وارد کنید:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinInput = it },
                        label = { Text("رمز PIN") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length == 4) {
                            onAppPinChange(newPinInput)
                            onAppLockToggle(true)
                            showPinSetupDialog = false
                            newPinInput = ""
                            Toast.makeText(context, "رمز عبور با موفقیت ثبت و فعال شد", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "رمز عبور باید دقیقاً ۴ رقم باشد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ثبت و فعال‌سازی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Debug Mock Data Dialog
    if (showResetMockDataDialog) {
        AlertDialog(
            onDismissRequest = { showResetMockDataDialog = false },
            title = { Text("پاک‌سازی و تزریق داده‌های نمونه") },
            text = { Text("آیا اطمینان دارید که می‌خواهید تمام داده‌های فعلی را پاک کرده و داده‌های نمونه فیک تزریق کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAndSeedMockData()
                        showResetMockDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تأیید و تزریق داده‌ها")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetMockDataDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Clear All App Data Dialog
    if (showClearAllDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDataDialog = false },
            title = { Text("حذف تمامی داده‌ها", color = ExpenseRed) },
            text = { Text("آیا از حذف تمامی اطلاعات برنامه (تراکنش‌ها، حساب‌ها، اقساط، چک‌ها و بدهی‌ها) اطمینان کامل دارید؟ این عمل غیرقابل بازگشت است.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearAllDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حذف کامل اطلاعات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDataDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // About App Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "درباره برنامه مدیریت مالی شخصی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "نسخه ۱.۵.۰",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "برنامه هوشمند مدیریت مالی شخصی جهت مدیریت آسان حساب‌ها، تراکنش‌ها، وام‌ها، اقساط، چک‌ها و بدهی‌ها با تقویم خورشیدی و پشتیبان‌گیری آفلاین.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "طراحی‌شده با متریال دیزاین ۳ و Jetpack Compose",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("متوجه شدم")
                }
            }
        )
    }
}
