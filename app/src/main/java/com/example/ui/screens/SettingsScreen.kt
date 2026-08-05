package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.CategoryTwoLevelSelector
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryThemeColor
import androidx.compose.foundation.border
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.JalaliCalendarHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
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
    onClearAllData: () -> Unit = {}
) {
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showResetMockDataDialog by remember { mutableStateOf(false) }
    var showClearAllDataDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Account Dialog states
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // PIN Setup Dialog state
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "دسترسی اعلان‌ها با موفقیت فعال شد", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "دسترسی ارسال اعلان اعطا نشد", Toast.LENGTH_SHORT).show()
        }
    }

    // Launchers for Backup file saving and restoring
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 0: Accounts & Cards Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مدیریت حساب‌ها و کارت‌های بانکی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showAddAccountDialog = true }) {
                            Icon(Icons.Default.AddCard, contentDescription = "افزودن حساب جدید", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddAccountDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حساب جدید", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { showTransferDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = accounts.size >= 2
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("انتقال وجه", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    if (accounts.isEmpty()) {
                        Text("هیچ حسابی ثبت نشده است.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            accounts.forEach { acc ->
                                var showDeleteAccountDialog by remember { mutableStateOf(false) }

                                if (showDeleteAccountDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteAccountDialog = false },
                                        title = { Text("حذف حساب") },
                                        text = { Text("آیا از حذف حساب '${acc.name}' اطمینان دارید؟") },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                onDeleteAccount(acc)
                                                showDeleteAccountDialog = false
                                            }) {
                                                Text("حذف", color = ExpenseRed)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteAccountDialog = false }) {
                                                Text("انصراف")
                                            }
                                        }
                                    )
                                }

                                val cardColor = CategoryIconHelper.parseColor(acc.colorHex)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = cardColor.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { accountToEdit = acc }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(cardColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = CategoryIconHelper.getIcon(acc.iconName),
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = acc.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (acc.isDefault) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "پیش‌فرض",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                if (acc.accountNumber.isNotBlank()) {
                                                    Text(
                                                        text = JalaliCalendarHelper.toPersianDigits(acc.accountNumber),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = CurrencyHelper.formatAmount(acc.balance, currencyUnit),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            IconButton(onClick = { onSetDefaultAccount(acc.id) }) {
                                                Icon(
                                                    imageVector = if (acc.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "پیش‌فرض",
                                                    tint = if (acc.isDefault) Color(0xFFFFB703) else Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(onClick = { showDeleteAccountDialog = true }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = ExpenseRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Currency Unit Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنظیمات واحد پول",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("واحد پولی مورد نظر خود برای نمایش مبالغ را انتخاب کنید:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))

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
            }
        }

        // Theme / Appearance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "پوسته و ظاهر برنامه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("پوسته برنامه (تاریک / روشن) را بر اساس سلیقه خود تغییر دهید:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (themeMode == "DARK") Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حالت شب / تاریک (DataStore)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = themeMode == "DARK",
                            onCheckedChange = { isDark ->
                                onThemeModeChange(if (isDark) "DARK" else "LIGHT")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = themeMode == "SYSTEM",
                            onClick = { onThemeModeChange("SYSTEM") },
                            shape = SegmentedButtonDefaults.itemShape(0, 3)
                        ) {
                            Text("سیستم", style = MaterialTheme.typography.labelSmall)
                        }
                        SegmentedButton(
                            selected = themeMode == "LIGHT",
                            onClick = { onThemeModeChange("LIGHT") },
                            shape = SegmentedButtonDefaults.itemShape(1, 3)
                        ) {
                            Text("روشن", style = MaterialTheme.typography.labelSmall)
                        }
                        SegmentedButton(
                            selected = themeMode == "DARK",
                            onClick = { onThemeModeChange("DARK") },
                            shape = SegmentedButtonDefaults.itemShape(2, 3)
                        ) {
                            Text("تاریک", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "رنگ اصلی برنامه (Accent Color)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "رنگ مشخصه اصلی برنامه جهت شخصی‌سازی کل محیط کاربری:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(PrimaryThemeColor.entries.toTypedArray()) { colorObj ->
                            val isSelected = colorObj.key == themePrimaryColor
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onThemePrimaryColorChange(colorObj.key) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(colorObj.lightPrimary)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    3.dp,
                                                    MaterialTheme.colorScheme.onSurface,
                                                    CircleShape
                                                )
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "انتخاب شده",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = colorObj.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Security & App Lock Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "قفل و امنیت اطلاعات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { enabled ->
                                onAppLockToggle(enabled)
                                if (enabled && appPin.isEmpty()) {
                                    showPinSetupDialog = true
                                }
                            }
                        )
                    }

                    Text(
                        text = "محافظت از اطلاعات مالی شما با رمز ۴ رقمی و اثر انگشت",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    if (isAppLockEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("رمز عبور ورود (PIN ۴ رقمی)", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(
                                onClick = { showPinSetupDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (appPin.isEmpty()) "تعیین رمز" else "تغییر رمز", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ورود با اثر انگشت / بیومتریک", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { onBiometricToggle(it) }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Daily Reminder Time Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ساعت یادآوری روزانه اقساط و چک‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("یادآور سررسید چک‌ها و اقساط هر شب سر این ساعت نمایش داده می‌شود:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))

                    val hourOptions = listOf(8, 9, 10, 12, 14, 16, 18, 20, 21, 22, 23)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hourOptions) { hour ->
                            FilterChip(
                                selected = reminderHour == hour,
                                onClick = { onSetReminderHour(hour) },
                                label = {
                                    Text(
                                        text = "ساعت ${JalaliCalendarHelper.toPersianDigits(hour)}:۰۰",
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val isGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (!isGranted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Toast.makeText(context, "دسترسی ارسال اعلان‌ها فعال است", Toast.LENGTH_SHORT).show()
                                    try {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("درخواست دسترسی ارسال اعلان‌های یادآوری", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Section 4: Category Management
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryManagerDialog = true }
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "مدیریت دسته‌بندی‌ها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "افزودن، ویرایش و حذف دسته‌های درآمد و هزینه",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
            }
        }

        // Section 5: Data Backup & Restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "پشتیبان‌گیری و بازگردانی فایل",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "می‌توانید تمام اطلاعات تراکنش‌ها، حساب‌ها و اقساط را بصورت یک فایل JSON پشتیبان‌گیری و یا بازگردانی کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            try {
                                backupLauncher.launch("ExpenseTracker_Backup_${JalaliCalendarHelper.getCurrentJalaliDateTimeString()}.json")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onShareBackupData()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ذخیره فایل پشتیبان JSON")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onShareBackupData() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اشتراک‌گذاری / ارسال فایل پشتیبان")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                restoreLauncher.launch("*/*")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "دستگاه امکان انتخاب فایل پشتیبان را ندارد.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بازگردانی اطلاعات از فایل پشتیبان")
                    }
                }
            }
        }

        // Section 6: Debug Tools (Only in Debug build)
        if (com.example.BuildConfig.DEBUG) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ابزارهای دیباگ و توسعه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "پاک‌سازی تمام اطلاعات برنامه (به جز دسته‌بندی‌ها) و تزریق داده‌های نمونه شامل ۲ حساب، ۵ چک، ۵ قسط، ۵ طلب/بدهی و ۳۰ تراکنش مربوط به ماه جاری و قبلی.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showResetMockDataDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("پاک‌سازی و تزریق داده‌های فیک نمونه")
                        }
                    }
                }
            }
        }

        // Section 7: Danger Zone - Clear All App Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ExpenseRed.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = ExpenseRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حذف تمامی داده‌های برنامه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "با استفاده از این گزینه، تمامی تراکنش‌ها، حساب‌ها، اقساط، چک‌ها و بدهی‌های شما به‌صورت کامل و غیرقابل بازگشت پاک‌سازی می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showClearAllDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف تمامی داده‌های برنامه")
                    }
                }
            }
        }

        // Section 8: About App
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true }
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "درباره برنامه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اطلاعات نسخه و مشخصات برنامه",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
            }
        }
    }

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
                Button(onClick = { showAboutDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    if (showClearAllDataDialog) {
        val requiredPhrase = "از حذف تمام اطلاعات برنامه اطمینان دارم"
        var typedConfirmation by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showClearAllDataDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف تمامی اطلاعات برنامه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "این عملیات غیرقابل بازگشت است و تمام تراکنش‌ها، حساب‌ها، اقساط، چک‌ها و طلب/بدهی‌ها را پاک می‌کند.\nجهت تایید، عبارت زیر را دقیقاً وارد نمایید:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = requiredPhrase,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = typedConfirmation,
                        onValueChange = { typedConfirmation = it },
                        placeholder = { Text("عبارت فوق را تایپ کنید...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (typedConfirmation.trim() == requiredPhrase) IncomeGreen else ExpenseRed
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDataDialog = false
                        onClearAllData()
                    },
                    enabled = typedConfirmation.trim() == requiredPhrase,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
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

    if (showResetMockDataDialog) {
        AlertDialog(
            onDismissRequest = { showResetMockDataDialog = false },
            title = { Text("بازنشانی و تزریق داده نمونه") },
            text = { Text("آیا از پاک‌سازی اطلاعات برنامه و ورود داده‌های نمونه فیک (۲ حساب، ۵ چک، ۵ قسط، ۵ طلب/بدهی و ۳۰ تراکنش) اطمینان دارید؟ دسته‌بندی‌ها دست‌نخورده باقی خواهند ماند.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetMockDataDialog = false
                        onResetAndSeedMockData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تایید و بازنشانی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetMockDataDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

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

    if (showAddAccountDialog || accountToEdit != null) {
        AddEditAccountDialog(
            account = accountToEdit,
            currencyUnit = currencyUnit,
            onDismiss = {
                showAddAccountDialog = false
                accountToEdit = null
            },
            onSave = { acc ->
                if (accountToEdit == null) {
                    onAddAccount(acc)
                } else {
                    onUpdateAccount(acc)
                }
                showAddAccountDialog = false
                accountToEdit = null
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = accounts,
            currencyUnit = currencyUnit,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromId, toId, amt, fee, note ->
                onTransfer(fromId, toId, amt, fee, note)
                showTransferDialog = false
            }
        )
    }

    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinSetupDialog = false
                newPinInput = ""
            },
            title = { Text("تنظیم رمز عبور (PIN ۴ رقمی)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("یک رمز عبور ۴ رقمی عددی جهت ورود به برنامه وارد کنید:")
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPinInput = it },
                        label = { Text("رمز عبور ۴ رقمی") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
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
                        }
                    },
                    enabled = newPinInput.length == 4
                ) {
                    Text("ثبت و ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinSetupDialog = false
                    newPinInput = ""
                }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAddCategory: (CategoryEntity) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategoryWithOption: (CategoryEntity, Boolean, Long?, Long?) -> Unit
) {
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var showAddCategoryModal by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var addSubcategoryParentId by remember { mutableStateOf<Long?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val filteredCats = categories.filter { it.type == selectedType }
    val mainCats = filteredCats.filter { it.parentId == null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مدیریت دسته‌بندی‌ها") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == "EXPENSE",
                        onClick = { selectedType = "EXPENSE" },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) {
                        Text("هزینه‌ها")
                    }
                    SegmentedButton(
                        selected = selectedType == "INCOME",
                        onClick = { selectedType = "INCOME" },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) {
                        Text("درآمدها")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mainCats, key = { it.id }) { mainCat ->
                        val color = CategoryIconHelper.parseColor(mainCat.colorHex)
                        val icon = CategoryIconHelper.getIcon(mainCat.iconName)
                        val subCats = filteredCats.filter { it.parentId == mainCat.id }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = color)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(mainCat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            addSubcategoryParentId = mainCat.id
                                            showAddCategoryModal = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "افزودن زیردسته", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { editingCategory = mainCat }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { categoryToDelete = mainCat }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ExpenseRed)
                                    }
                                }
                            }

                            if (subCats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    subCats.forEach { subCat ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "↳ ${subCat.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row {
                                                IconButton(onClick = { editingCategory = subCat }, modifier = Modifier.size(28.dp)) {
                                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { categoryToDelete = subCat }, modifier = Modifier.size(28.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        addSubcategoryParentId = null
                        showAddCategoryModal = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن دسته‌بندی جدید")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )

    if (categoryToDelete != null) {
        DeleteCategoryConfirmationDialog(
            categoryToDelete = categoryToDelete!!,
            categories = categories,
            transactions = transactions,
            onDismiss = { categoryToDelete = null },
            onConfirmDelete = { deleteTxs, targetMainId, targetSubId ->
                onDeleteCategoryWithOption(categoryToDelete!!, deleteTxs, targetMainId, targetSubId)
                categoryToDelete = null
            }
        )
    }

    if (showAddCategoryModal) {
        AddCategoryModal(
            categoryToEdit = null,
            defaultParentId = addSubcategoryParentId,
            type = selectedType,
            categories = categories,
            onDismiss = {
                showAddCategoryModal = false
                addSubcategoryParentId = null
            },
            onSave = { newCat ->
                onAddCategory(newCat)
                showAddCategoryModal = false
                addSubcategoryParentId = null
            }
        )
    }

    if (editingCategory != null) {
        AddCategoryModal(
            categoryToEdit = editingCategory,
            type = editingCategory?.type ?: selectedType,
            categories = categories,
            onDismiss = { editingCategory = null },
            onSave = { updatedCat ->
                onUpdateCategory(updatedCat)
                editingCategory = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportFilterDialog(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onExportPdf: (Long?, Long?, String?, String?) -> Unit,
    onExportCsv: (Long?, Long?, String?, String?) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var startDateJalali by remember { mutableStateOf("") }
    var endDateJalali by remember { mutableStateOf("") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = if (startDateJalali.isNotBlank()) startDateJalali else JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString(),
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { date ->
                startDateJalali = date
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = if (endDateJalali.isNotBlank()) endDateJalali else JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString(),
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { date ->
                endDateJalali = date
                showEndDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فیلترهای گزارش خروجی") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Account Filter
                Text("حساب مورد نظر:", style = MaterialTheme.typography.labelMedium)
                var accExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accExpanded,
                    onExpandedChange = { accExpanded = it }
                ) {
                    val accText = accounts.find { it.id == selectedAccountId }?.name ?: "همه حساب‌ها"
                    OutlinedTextField(
                        value = accText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = accExpanded,
                        onDismissRequest = { accExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("همه حساب‌ها") },
                            onClick = { selectedAccountId = null; accExpanded = false }
                        )
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = { selectedAccountId = acc.id; accExpanded = false }
                            )
                        }
                    }
                }

                // Category Filter
                Text("دسته‌بندی:", style = MaterialTheme.typography.labelMedium)
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    val catText = categories.find { it.id == selectedCategoryId }?.name ?: "همه دسته‌بندی‌ها"
                    OutlinedTextField(
                        value = catText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("همه دسته‌بندی‌ها") },
                            onClick = { selectedCategoryId = null; catExpanded = false }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCategoryId = cat.id; catExpanded = false }
                            )
                        }
                    }
                }

                // Date Range
                Text("بازه زمانی (اختیاری):", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (startDateJalali.isNotBlank()) "از: ${JalaliCalendarHelper.toPersianDigits(startDateJalali)}" else "از تاریخ",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (endDateJalali.isNotBlank()) "تا: ${JalaliCalendarHelper.toPersianDigits(endDateJalali)}" else "تا تاریخ",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (startDateJalali.isNotBlank() || endDateJalali.isNotBlank()) {
                    TextButton(
                        onClick = {
                            startDateJalali = ""
                            endDateJalali = ""
                        }
                    ) {
                        Text("پاکسازی فیلتر تاریخ")
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onExportPdf(selectedAccountId, selectedCategoryId, startDateJalali.ifBlank { null }, endDateJalali.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("دریافت فایل PDF")
                }

                OutlinedButton(
                    onClick = { onExportCsv(selectedAccountId, selectedCategoryId, startDateJalali.ifBlank { null }, endDateJalali.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("دریافت فایل اکسل")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryModal(
    categoryToEdit: CategoryEntity? = null,
    defaultParentId: Long? = null,
    type: String,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var selectedColor by remember { mutableStateOf(categoryToEdit?.colorHex ?: "#F59E0B") }
    var selectedIcon by remember { mutableStateOf(categoryToEdit?.iconName ?: "ShoppingCart") }
    var selectedParentId by remember { mutableStateOf<Long?>(categoryToEdit?.parentId ?: defaultParentId) }

    var parentDropdownExpanded by remember { mutableStateOf(false) }

    val mainCategories = remember(categories, type) {
        categories.filter { it.type == type && it.parentId == null && it.id != categoryToEdit?.id }
    }

    val colorOptions = listOf("#F59E0B", "#EF4444", "#3B82F6", "#10B981", "#8B5CF6", "#EC4899", "#64748B")
    val iconOptions = listOf("ShoppingCart", "DirectionsCar", "Home", "Receipt", "Restaurant", "MedicalServices", "Payments", "Work", "AttachMoney", "School", "Flight", "MoreHoriz")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoryToEdit == null) "افزودن دسته‌بندی جدید" else "ویرایش دسته‌بندی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام دسته‌بندی") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = parentDropdownExpanded,
                    onExpandedChange = { parentDropdownExpanded = !parentDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val parentName = mainCategories.find { it.id == selectedParentId }?.name ?: "دسته‌بندی اصلی (بدون والد)"
                    OutlinedTextField(
                        value = parentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع دسته (اصلی یا زیردسته)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = parentDropdownExpanded,
                        onDismissRequest = { parentDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("دسته‌بندی اصلی (بدون والد)", fontWeight = if (selectedParentId == null) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedParentId = null
                                parentDropdownExpanded = false
                            }
                        )
                        mainCategories.forEach { mainCat ->
                            DropdownMenuItem(
                                text = { Text("زیردسته: ${mainCat.name}", fontWeight = if (selectedParentId == mainCat.id) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedParentId = mainCat.id
                                    selectedColor = mainCat.colorHex
                                    selectedIcon = mainCat.iconName
                                    parentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("رنگ دسته‌بندی:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { hex ->
                        val color = CategoryIconHelper.parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                Text("آیکون دسته‌بندی:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconOptions.take(6).forEach { iconName ->
                        val icon = CategoryIconHelper.getIcon(iconName)
                        FilterChip(
                            selected = selectedIcon == iconName,
                            onClick = { selectedIcon = iconName },
                            label = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = categoryToEdit?.copy(
                        name = name,
                        colorHex = selectedColor,
                        iconName = selectedIcon,
                        parentId = selectedParentId
                    ) ?: CategoryEntity(
                        name = name,
                        type = type,
                        iconName = selectedIcon,
                        colorHex = selectedColor,
                        isSystem = false,
                        parentId = selectedParentId
                    )
                    onSave(result)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (categoryToEdit == null) "ذخیره" else "بروزرسانی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteCategoryConfirmationDialog(
    categoryToDelete: CategoryEntity,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onConfirmDelete: (deleteTransactions: Boolean, targetMainCategoryId: Long?, targetSubcategoryId: Long?) -> Unit
) {
    val subCatIds = remember(categoryToDelete, categories) {
        if (categoryToDelete.parentId == null) {
            categories.filter { it.parentId == categoryToDelete.id }.map { it.id }.toSet()
        } else emptySet()
    }
    val allCatIds = remember(categoryToDelete, subCatIds) {
        setOf(categoryToDelete.id) + subCatIds
    }

    val affectedTxs = remember(allCatIds, transactions) {
        transactions.filter { tx ->
            (tx.categoryId != null && allCatIds.contains(tx.categoryId)) ||
            (tx.subcategoryId != null && allCatIds.contains(tx.subcategoryId))
        }
    }

    var actionOption by remember { mutableStateOf("TRANSFER") }
    var selectedTargetMainCatId by remember { mutableStateOf<Long?>(null) }
    var selectedTargetSubCatId by remember { mutableStateOf<Long?>(null) }

    val availableTargetCategories = remember(categories, categoryToDelete, allCatIds) {
        categories.filter { it.type == categoryToDelete.type && !allCatIds.contains(it.id) }
    }

    LaunchedEffect(availableTargetCategories) {
        if (selectedTargetMainCatId == null) {
            val firstMain = availableTargetCategories.firstOrNull { it.parentId == null }
            if (firstMain != null) {
                selectedTargetMainCatId = firstMain.id
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حذف دسته‌بندی «${categoryToDelete.name}»",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (affectedTxs.isEmpty()) {
                    Text(
                        text = "آیا از حذف دسته‌بندی «${categoryToDelete.name}» اطمینان دارید؟ هیچ تراکنشی با این دسته‌بندی ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Surface(
                        color = ExpenseRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "تعداد ${JalaliCalendarHelper.toPersianDigits(affectedTxs.size.toString())} تراکنش با این دسته‌بندی ثبت شده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }

                    Text(
                        text = "لطفاً نحوه مدیریت این تراکنش‌ها را انتخاب کنید:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { actionOption = "TRANSFER" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = actionOption == "TRANSFER",
                            onClick = { actionOption = "TRANSFER" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "انتقال تراکنش‌ها به دسته‌بندی دیگر",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (actionOption == "TRANSFER") {
                        if (availableTargetCategories.none { it.parentId == null }) {
                            Text(
                                text = "دسته‌بندی دیگری برای انتقال وجود ندارد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                        } else {
                            Box(modifier = Modifier.padding(start = 12.dp)) {
                                CategoryTwoLevelSelector(
                                    title = "مقصد جدید تراکنش‌ها",
                                    allCategories = availableTargetCategories,
                                    selectedCategoryId = selectedTargetMainCatId,
                                    selectedSubcategoryId = selectedTargetSubCatId,
                                    onCategorySelected = { mainCatId, subCatId ->
                                        selectedTargetMainCatId = mainCatId
                                        selectedTargetSubCatId = subCatId
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { actionOption = "DELETE" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = actionOption == "DELETE",
                            onClick = { actionOption = "DELETE" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حذف تمامی تراکنش‌های مرتبط",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExpenseRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isTransferValid = actionOption == "TRANSFER" && selectedTargetMainCatId != null
            val isDeleteValid = actionOption == "DELETE"
            val canConfirm = affectedTxs.isEmpty() || isTransferValid || isDeleteValid

            Button(
                onClick = {
                    if (affectedTxs.isEmpty()) {
                        onConfirmDelete(false, null, null)
                    } else if (actionOption == "DELETE") {
                        onConfirmDelete(true, null, null)
                    } else {
                        onConfirmDelete(false, selectedTargetMainCatId, selectedTargetSubCatId)
                    }
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpenseRed
                )
            ) {
                Text(if (affectedTxs.isEmpty()) "حذف دسته‌بندی" else "تأیید و حذف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
