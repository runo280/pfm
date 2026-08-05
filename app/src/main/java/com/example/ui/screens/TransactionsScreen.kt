package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.AccountHorizontalSelector
import com.example.ui.components.CategoryHorizontalSelector
import com.example.ui.components.CategoryTwoLevelSelector
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.FilterPreferences
import com.example.util.JalaliCalendarHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencyUnit: CurrencyUnit,
    onAddTransaction: (TransactionEntity) -> Unit,
    onUpdateTransaction: (TransactionEntity, TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    editingTx: TransactionEntity? = null,
    onDismissEdit: () -> Unit = {},
    onExportPdf: (List<TransactionEntity>) -> Unit = {},
    onExportCsv: (List<TransactionEntity>) -> Unit = {}
) {
    val context = LocalContext.current
    val filterPrefs = remember { FilterPreferences(context) }

    var searchQuery by remember { mutableStateOf(filterPrefs.txSearchQuery) }
    var selectedFilterType by remember { mutableStateOf(filterPrefs.txFilterType) }
    var dateFilterMode by remember { mutableStateOf(filterPrefs.txDateMode) }
    var selectedCategoryId by remember { mutableStateOf(filterPrefs.txCategoryId) }
    var selectedAccountId by remember { mutableStateOf(filterPrefs.txAccountId) }

    val todayJalali = remember { JalaliCalendarHelper.getCurrentJalaliDate() }
    var selectedDailyDate by remember { mutableStateOf(todayJalali) }
    var selectedWeeklyEndDate by remember { mutableStateOf(todayJalali) }

    val savedYear = filterPrefs.txYear
    val savedMonth = filterPrefs.txMonth
    var selectedYear by remember { mutableIntStateOf(if (savedYear != -1) savedYear else todayJalali.year) }
    var selectedMonth by remember { mutableIntStateOf(if (savedMonth != -1) savedMonth else todayJalali.month) }
    var startDateJalali by remember { mutableStateOf(filterPrefs.txStartDate) }
    var endDateJalali by remember { mutableStateOf(filterPrefs.txEndDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isFilterVisible by remember { mutableStateOf(filterPrefs.txIsFilterVisible) }

    LaunchedEffect(
        searchQuery, selectedFilterType, dateFilterMode,
        selectedCategoryId, selectedAccountId, selectedYear, selectedMonth,
        startDateJalali, endDateJalali, isFilterVisible
    ) {
        filterPrefs.txSearchQuery = searchQuery
        filterPrefs.txFilterType = selectedFilterType
        filterPrefs.txDateMode = dateFilterMode
        filterPrefs.txCategoryId = selectedCategoryId
        filterPrefs.txAccountId = selectedAccountId
        filterPrefs.txYear = selectedYear
        filterPrefs.txMonth = selectedMonth
        filterPrefs.txStartDate = startDateJalali
        filterPrefs.txEndDate = endDateJalali
        filterPrefs.txIsFilterVisible = isFilterVisible
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var txToEdit by remember { mutableStateOf<TransactionEntity?>(editingTx) }

    LaunchedEffect(editingTx) {
        txToEdit = editingTx
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val hasActiveFilter = searchQuery.isNotBlank() || selectedFilterType != "ALL" || dateFilterMode != "MONTHLY" || selectedCategoryId != null || selectedAccountId != null

    val filteredTxs = remember(
        transactions, searchQuery, selectedFilterType, dateFilterMode,
        selectedDailyDate, selectedWeeklyEndDate, selectedYear, selectedMonth,
        startDateJalali, endDateJalali, selectedCategoryId, selectedAccountId
    ) {
        transactions.filter { tx ->
            val matchesSearch = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true)

            val matchesType = when (selectedFilterType) {
                "EXPENSE" -> tx.type == "EXPENSE"
                "INCOME" -> tx.type == "INCOME"
                "TRANSFER" -> tx.type == "TRANSFER"
                else -> true
            }

            val matchesDate = when (dateFilterMode) {
                "DAILY" -> tx.jalaliDate == selectedDailyDate.toFormattedString()
                "WEEKLY" -> {
                    val parsed = JalaliCalendarHelper.parseJalaliDate(tx.jalaliDate)
                    if (parsed != null) {
                        val txJdn = JalaliCalendarHelper.jalaliToJdn(parsed.year, parsed.month, parsed.day)
                        val (weekStart, weekEnd) = JalaliCalendarHelper.getWeekRange(selectedWeeklyEndDate)
                        val startJdn = JalaliCalendarHelper.jalaliToJdn(weekStart.year, weekStart.month, weekStart.day)
                        val endJdn = JalaliCalendarHelper.jalaliToJdn(weekEnd.year, weekEnd.month, weekEnd.day)
                        txJdn in startJdn..endJdn
                    } else false
                }
                "MONTHLY" -> {
                    val mStr = String.format(java.util.Locale.US, "%04d/%02d", selectedYear, selectedMonth)
                    tx.jalaliDate.startsWith(mStr)
                }
                "YEARLY" -> tx.jalaliDate.startsWith(selectedYear.toString())
                "CUSTOM" -> {
                    val afterStart = if (startDateJalali.isNotBlank()) tx.jalaliDate >= startDateJalali else true
                    val beforeEnd = if (endDateJalali.isNotBlank()) tx.jalaliDate <= endDateJalali else true
                    afterStart && beforeEnd
                }
                else -> true
            }

            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId
            val matchesAccount = selectedAccountId == null || tx.accountId == selectedAccountId || tx.targetAccountId == selectedAccountId

            matchesSearch && matchesType && matchesDate && matchesCategory && matchesAccount
        }
    }

    var visibleTxCount by remember { mutableStateOf(50) }
    LaunchedEffect(filteredTxs) {
        visibleTxCount = 50
    }
    val displayedTxs = remember(filteredTxs, visibleTxCount) {
        filteredTxs.take(visibleTxCount)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Action Bar with Export Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تراکنش‌ها",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PDF Button
                    Surface(
                        onClick = { onExportPdf(filteredTxs) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "خروجی PDF",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Excel Button
                    Surface(
                        onClick = { onExportCsv(filteredTxs) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "خروجی اکسل",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "اکسل",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جستجو در تراکنش‌ها...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filters Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFilterVisible = !isFilterVisible },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فیلتر تراکنش‌ها",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasActiveFilter) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ) {
                                    Box(modifier = Modifier.size(8.dp))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasActiveFilter) {
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedFilterType = "ALL"
                                        dateFilterMode = "MONTHLY"
                                        selectedCategoryId = null
                                        selectedAccountId = null
                                        selectedYear = todayJalali.year
                                        selectedMonth = todayJalali.month
                                        startDateJalali = ""
                                        endDateJalali = ""
                                        filterPrefs.resetTxFilters()
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = ExpenseRed)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("بازنشانی فیلترها", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                                }
                            }
                            IconButton(
                                onClick = { isFilterVisible = !isFilterVisible },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFilterVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isFilterVisible) "مخفی کردن فیلترها" else "نمایش فیلترها"
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isFilterVisible) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Type Filter Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedFilterType == "ALL",
                                    onClick = { selectedFilterType = "ALL" },
                                    label = { Text("همه نوع", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = selectedFilterType == "EXPENSE",
                                    onClick = { selectedFilterType = "EXPENSE" },
                                    label = { Text("هزینه‌ها", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = selectedFilterType == "INCOME",
                                    onClick = { selectedFilterType = "INCOME" },
                                    label = { Text("درآمدها", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = selectedFilterType == "TRANSFER",
                                    onClick = { selectedFilterType = "TRANSFER" },
                                    label = { Text("انتقال‌ها", style = MaterialTheme.typography.labelSmall) }
                                )
                            }

                            // Row 2: Time Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = dateFilterMode == "ALL",
                                    onClick = { dateFilterMode = "ALL" },
                                    label = { Text("همه زمان‌ها", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = dateFilterMode == "DAILY",
                                    onClick = { dateFilterMode = "DAILY" },
                                    label = { Text("روزانه", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = dateFilterMode == "WEEKLY",
                                    onClick = { dateFilterMode = "WEEKLY" },
                                    label = { Text("هفتگی", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = dateFilterMode == "MONTHLY",
                                    onClick = { dateFilterMode = "MONTHLY" },
                                    label = { Text("ماهیانه", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = dateFilterMode == "YEARLY",
                                    onClick = { dateFilterMode = "YEARLY" },
                                    label = { Text("سالیانه", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = dateFilterMode == "CUSTOM",
                                    onClick = { dateFilterMode = "CUSTOM" },
                                    label = { Text("بازه زمانی", style = MaterialTheme.typography.labelSmall) }
                                )
                            }

                            // Date Navigation / Range Display
                            DateFilterNavigator(
                                dateFilterMode = dateFilterMode,
                                selectedDailyDate = selectedDailyDate,
                                onDailyDateChange = { selectedDailyDate = it },
                                selectedWeeklyEndDate = selectedWeeklyEndDate,
                                onWeeklyEndDateChange = { selectedWeeklyEndDate = it },
                                selectedYear = selectedYear,
                                onYearChange = { selectedYear = it },
                                selectedMonth = selectedMonth,
                                onMonthChange = { selectedMonth = it },
                                startDateJalali = startDateJalali,
                                onStartDateChange = { startDateJalali = it },
                                endDateJalali = endDateJalali,
                                onEndDateChange = { endDateJalali = it },
                                onShowStartDatePicker = { showStartDatePicker = true },
                                onShowEndDatePicker = { showEndDatePicker = true }
                            )

                            // Row 3: Account & Category Dropdowns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Account Filter
                                ExposedDropdownMenuBox(
                                    expanded = accountDropdownExpanded,
                                    onExpandedChange = { accountDropdownExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val selectedAccName = accounts.find { it.id == selectedAccountId }?.name ?: "همه حساب‌ها"
                                    OutlinedTextField(
                                        value = selectedAccName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("حساب", style = MaterialTheme.typography.labelSmall) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )

                                    ExposedDropdownMenu(
                                        expanded = accountDropdownExpanded,
                                        onDismissRequest = { accountDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("همه حساب‌ها", fontWeight = if (selectedAccountId == null) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                selectedAccountId = null
                                                accountDropdownExpanded = false
                                            }
                                        )
                                        accounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = { Text(acc.name) },
                                                onClick = {
                                                    selectedAccountId = acc.id
                                                    accountDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Category Filter
                                ExposedDropdownMenuBox(
                                    expanded = categoryDropdownExpanded,
                                    onExpandedChange = { categoryDropdownExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val selectedCat = categories.find { it.id == selectedCategoryId }
                                    val selectedCatName = selectedCat?.name ?: "همه دسته‌بندی‌ها"
                                    OutlinedTextField(
                                        value = selectedCatName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("دسته‌بندی", style = MaterialTheme.typography.labelSmall) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )

                                    ExposedDropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("همه دسته‌بندی‌ها", fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                selectedCategoryId = null
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                        val mainCatsFilter = categories.filter { it.parentId == null }
                                        mainCatsFilter.forEach { mainCat ->
                                            DropdownMenuItem(
                                                text = { Text(mainCat.name, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    selectedCategoryId = mainCat.id
                                                    categoryDropdownExpanded = false
                                                }
                                            )
                                            val subCats = categories.filter { it.parentId == mainCat.id }
                                            subCats.forEach { subCat ->
                                                DropdownMenuItem(
                                                    text = { Text("    ↳ ${subCat.name}", style = MaterialTheme.typography.bodySmall) },
                                                    onClick = {
                                                        selectedCategoryId = subCat.id
                                                        categoryDropdownExpanded = false
                                                    }
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

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ تراکنشی یافت نشد.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedTxs, key = { it.id }) { tx ->
                        val category = categoryMap[tx.categoryId]
                        val account = accountMap[tx.accountId]
                        val icon = CategoryIconHelper.getIcon(category?.iconName ?: "MoreHoriz")
                        val iconColor = CategoryIconHelper.parseColor(category?.colorHex ?: "#6B7280")

                        var showDeleteDialog by remember { mutableStateOf(false) }

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("حذف تراکنش") },
                                text = { Text("آیا از حذف تراکنش '${tx.title}' اطمینان دارید؟") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onDeleteTransaction(tx)
                                        showDeleteDialog = false
                                    }) {
                                        Text("حذف", color = ExpenseRed)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("انصراف")
                                    }
                                }
                            )
                        }

                        val todayStr = remember { JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString() }
                        val displayDate = remember(tx.jalaliDate, todayStr) {
                            if (tx.jalaliDate == todayStr) {
                                "امروز"
                            } else {
                                JalaliCalendarHelper.toPersianDigits(tx.jalaliDate)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { txToEdit = tx },
                                    onLongClick = { showDeleteDialog = true }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Row 1: Icon + Title | Amount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(iconColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = tx.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val isExpense = tx.type == "EXPENSE"
                                    val prefix = if (isExpense) "-" else if (tx.type == "INCOME") "+" else ""
                                    val textColor = if (isExpense) ExpenseRed else if (tx.type == "INCOME") IncomeGreen else BlueAccent

                                    Text(
                                        text = "${CurrencyHelper.formatAmount(tx.amount, currencyUnit)}$prefix",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }

                                // Row 2: Note / Description / Category
                                val subcategory = categoryMap[tx.subcategoryId]
                                val catName = if (subcategory != null) "${category?.name ?: ""} > ${subcategory.name}" else category?.name
                                val middleText = tx.note.ifBlank { catName ?: "" }

                                if (middleText.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(50.dp))
                                        Text(
                                            text = middleText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Row 3: Date | Account
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 50.dp)
                                    ) {
                                        Text(
                                            text = displayDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }

                                    if (account != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = account.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (displayedTxs.size < filteredTxs.size) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { visibleTxCount += 50 },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val remaining = filteredTxs.size - displayedTxs.size
                                    Text("بارگذاری ۵۰ تراکنش بعدی (${JalaliCalendarHelper.toPersianDigits(remaining.toString())} مورد باقی‌مانده)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTransactionDialog(
            transaction = null,
            accounts = accounts,
            categories = categories,
            currencyUnit = currencyUnit,
            onDismiss = { showAddDialog = false },
            onSave = { newTx ->
                onAddTransaction(newTx)
                showAddDialog = false
            }
        )
    }

    if (showStartDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = if (startDateJalali.isNotBlank()) startDateJalali else JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString(),
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { selectedDate ->
                startDateJalali = selectedDate
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = if (endDateJalali.isNotBlank()) endDateJalali else JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString(),
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { selectedDate ->
                endDateJalali = selectedDate
                showEndDatePicker = false
            }
        )
    }

    if (txToEdit != null) {
        AddEditTransactionDialog(
            transaction = txToEdit,
            accounts = accounts,
            categories = categories,
            currencyUnit = currencyUnit,
            onDismiss = {
                txToEdit = null
                onDismissEdit()
            },
            onSave = { updatedTx ->
                onUpdateTransaction(txToEdit!!, updatedTx)
                txToEdit = null
                onDismissEdit()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(transaction?.type ?: "EXPENSE") }
    var amountValue by remember {
        mutableStateOf(
            if (transaction != null) {
                val amt = if (currencyUnit == CurrencyUnit.RIAL) transaction.amount * 10 else transaction.amount
                val formatted = CurrencyHelper.formatLiveAmountInput(amt.toLong().toString())
                TextFieldValue(formatted)
            } else TextFieldValue("")
        )
    }

    val defaultAccount = remember(accounts) { accounts.find { it.isDefault } ?: accounts.firstOrNull() }
    var selectedAccountId by remember { mutableStateOf(transaction?.accountId ?: defaultAccount?.id ?: 1L) }
    var selectedTargetAccountId by remember { mutableStateOf(transaction?.targetAccountId ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1L) }

    val initialMainCatId = remember(transaction, categories, type) {
        if (transaction != null && transaction.type == type) {
            val subCat = transaction.subcategoryId?.let { id -> categories.find { it.id == id } }
            val mainCat = transaction.categoryId?.let { id -> categories.find { it.id == id } }
            if (subCat != null) {
                if (subCat.parentId != null) subCat.parentId else subCat.id
            } else if (mainCat != null) {
                if (mainCat.parentId != null) mainCat.parentId else mainCat.id
            } else {
                null
            }
        } else {
            null
        }
    }

    val initialSubCatId = remember(transaction, categories, type) {
        if (transaction != null && transaction.type == type) {
            val subCat = transaction.subcategoryId?.let { id -> categories.find { it.id == id } }
            val mainCat = transaction.categoryId?.let { id -> categories.find { it.id == id } }
            if (subCat != null) {
                subCat.id
            } else if (mainCat != null && mainCat.parentId != null) {
                mainCat.id
            } else {
                null
            }
        } else {
            null
        }
    }

    var selectedCategoryId by remember(initialMainCatId) { mutableStateOf(initialMainCatId) }
    var selectedSubcategoryId by remember(initialSubCatId) { mutableStateOf(initialSubCatId) }
    var jalaliDateStr by remember { mutableStateOf(transaction?.jalaliDate ?: JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()) }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var transferFeeValue by remember {
        mutableStateOf(
            if (transaction != null && transaction.transferFee > 0) {
                val fee = if (currencyUnit == CurrencyUnit.RIAL) transaction.transferFee * 10 else transaction.transferFee
                val formatted = CurrencyHelper.formatLiveAmountInput(fee.toLong().toString())
                TextFieldValue(formatted)
            } else TextFieldValue("")
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }

    val filteredCategories = remember(categories, type) {
        categories.filter { it.type == type }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = jalaliDateStr,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                jalaliDateStr = selectedDate
                showDatePicker = false
            }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (transaction == null) "ثبت تراکنش جدید" else "ویرایش تراکنش") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switcher
                Column(modifier = Modifier.fillMaxWidth()) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = type == "EXPENSE",
                            onClick = {
                                if (type != "EXPENSE") {
                                    type = "EXPENSE"
                                    selectedCategoryId = null
                                    selectedSubcategoryId = null
                                }
                            },
                            enabled = transaction == null || transaction.type == "EXPENSE",
                            shape = SegmentedButtonDefaults.itemShape(0, 3)
                        ) {
                            Text("هزینه", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SegmentedButton(
                            selected = type == "INCOME",
                            onClick = {
                                if (type != "INCOME") {
                                    type = "INCOME"
                                    selectedCategoryId = null
                                    selectedSubcategoryId = null
                                }
                            },
                            enabled = transaction == null || transaction.type == "INCOME",
                            shape = SegmentedButtonDefaults.itemShape(1, 3)
                        ) {
                            Text("درآمد", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        SegmentedButton(
                            selected = type == "TRANSFER",
                            onClick = { type = "TRANSFER" },
                            enabled = transaction == null || transaction.type == "TRANSFER",
                            shape = SegmentedButtonDefaults.itemShape(2, 3)
                        ) {
                            Text("انتقال", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (transaction != null) {
                        Text(
                            text = "نوع تراکنش در هنگام ویرایش قابل تغییر نیست.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountValue,
                    onValueChange = { amountValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ (${currencyUnit.titleFa})") },
                    supportingText = if (amountValue.text.isNotBlank()) {
                        {
                            val raw = CurrencyHelper.parseRawAmount(amountValue.text)
                            val formatted = CurrencyHelper.formatAmount(if (currencyUnit == CurrencyUnit.RIAL) raw / 10.0 else raw, currencyUnit)
                            Text(
                                text = "معادل: $formatted",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Account Selector
                AccountHorizontalSelector(
                    title = if (type == "TRANSFER") "از حساب مبدأ" else "حساب بانکی",
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    currencyUnit = currencyUnit,
                    onAccountSelected = { selectedAccountId = it }
                )

                if (type == "TRANSFER") {
                    // Target Account Selector
                    AccountHorizontalSelector(
                        title = "به حساب مقصد",
                        accounts = accounts.filter { it.id != selectedAccountId },
                        selectedAccountId = selectedTargetAccountId,
                        currencyUnit = currencyUnit,
                        onAccountSelected = { selectedTargetAccountId = it }
                    )

                    OutlinedTextField(
                        value = transferFeeValue,
                        onValueChange = { transferFeeValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                        label = { Text("کارمزد انتقال (${currencyUnit.titleFa})") },
                        supportingText = if (transferFeeValue.text.isNotBlank() && CurrencyHelper.parseRawAmount(transferFeeValue.text) > 0) {
                            {
                                val raw = CurrencyHelper.parseRawAmount(transferFeeValue.text)
                                val formatted = CurrencyHelper.formatAmount(if (currencyUnit == CurrencyUnit.RIAL) raw / 10.0 else raw, currencyUnit)
                                Text(
                                    text = "معادل: $formatted",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else {
                    // Two-Level Category Selector
                    CategoryTwoLevelSelector(
                        title = "دسته‌بندی",
                        allCategories = filteredCategories,
                        selectedCategoryId = selectedCategoryId,
                        selectedSubcategoryId = selectedSubcategoryId,
                        onCategorySelected = { mainCatId, subCatId ->
                            selectedCategoryId = mainCatId
                            selectedSubcategoryId = subCatId
                        }
                    )
                }

                // Date & Note Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = JalaliCalendarHelper.toPersianDigits(jalaliDateStr),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاریخ") },
                            leadingIcon = {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            enabled = false
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("توضیحات (اختیاری)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (type != "TRANSFER" && selectedCategoryId == null) {
                        Toast.makeText(context, "لطفاً دسته‌بندی را انتخاب کنید", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val rawAmount = CurrencyHelper.parseRawAmount(amountValue.text)
                    val amountInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmount / 10.0 else rawAmount
                    val feeRaw = CurrencyHelper.parseRawAmount(transferFeeValue.text)
                    val feeInToman = if (currencyUnit == CurrencyUnit.RIAL) feeRaw / 10.0 else feeRaw

                    val titleToSave = if (type == "TRANSFER") "انتقال وجه"
                    else {
                        val subName = categories.find { it.id == selectedSubcategoryId }?.name
                        val mainName = categories.find { it.id == selectedCategoryId }?.name ?: "تراکنش"
                        if (subName != null) "$mainName - $subName" else mainName
                    }

                    val newTx = TransactionEntity(
                        id = transaction?.id ?: 0,
                        type = type,
                        amount = amountInToman,
                        accountId = selectedAccountId,
                        targetAccountId = if (type == "TRANSFER") selectedTargetAccountId else null,
                        categoryId = if (type != "TRANSFER") selectedCategoryId else null,
                        subcategoryId = if (type != "TRANSFER") selectedSubcategoryId else null,
                        jalaliDate = jalaliDateStr,
                        title = titleToSave,
                        note = note,
                        transferFee = feeInToman
                    )
                    onSave(newTx)
                },
                enabled = amountValue.text.isNotBlank() &&
                        CurrencyHelper.parseRawAmount(amountValue.text) > 0 &&
                        (type == "TRANSFER" || selectedCategoryId != null)
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
    }
}

@Composable
fun DateFilterNavigator(
    dateFilterMode: String,
    selectedDailyDate: com.example.util.JalaliDate,
    onDailyDateChange: (com.example.util.JalaliDate) -> Unit,
    selectedWeeklyEndDate: com.example.util.JalaliDate,
    onWeeklyEndDateChange: (com.example.util.JalaliDate) -> Unit,
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    selectedMonth: Int,
    onMonthChange: (Int) -> Unit,
    startDateJalali: String,
    onStartDateChange: (String) -> Unit,
    endDateJalali: String,
    onEndDateChange: (String) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit
) {
    val today = remember { JalaliCalendarHelper.getCurrentJalaliDate() }

    when (dateFilterMode) {
        "DAILY" -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onDailyDateChange(JalaliCalendarHelper.addDays(selectedDailyDate, -1)) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "روز قبل")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val isToday = selectedDailyDate == today
                        Text(
                            text = if (isToday) "امروز (${selectedDailyDate.toReadablePersianString()})" else selectedDailyDate.toReadablePersianString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isToday) {
                            Text(
                                text = "بازگشت به امروز",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onDailyDateChange(today) }
                            )
                        }
                    }

                    IconButton(onClick = { onDailyDateChange(JalaliCalendarHelper.addDays(selectedDailyDate, 1)) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "روز بعد")
                    }
                }
            }
        }
        "WEEKLY" -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onWeeklyEndDateChange(JalaliCalendarHelper.addDays(selectedWeeklyEndDate, -7)) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "هفته قبل")
                    }

                    val (weekStart, weekEnd) = remember(selectedWeeklyEndDate) { JalaliCalendarHelper.getWeekRange(selectedWeeklyEndDate) }
                    val isCurrentWeek = remember(selectedWeeklyEndDate, today) {
                        val (curStart, curEnd) = JalaliCalendarHelper.getWeekRange(today)
                        weekStart == curStart && weekEnd == curEnd
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${JalaliCalendarHelper.toPersianDigits(weekStart.toFormattedString())} تا ${JalaliCalendarHelper.toPersianDigits(weekEnd.toFormattedString())}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCurrentWeek) "هفته جاری (شنبه تا جمعه)" else "شنبه تا جمعه",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        if (!isCurrentWeek) {
                            Text(
                                text = "بازگشت به هفته جاری",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onWeeklyEndDateChange(today) }
                            )
                        }
                    }

                    IconButton(onClick = { onWeeklyEndDateChange(JalaliCalendarHelper.addDays(selectedWeeklyEndDate, 7)) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "هفته بعد")
                    }
                }
            }
        }
        "MONTHLY", "MONTH" -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val (ny, nm) = JalaliCalendarHelper.addMonths(selectedYear, selectedMonth, -1)
                        onYearChange(ny)
                        onMonthChange(nm)
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "ماه قبل")
                    }

                    Text(
                        text = "${JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(selectedMonth - 1) { "" }} ${JalaliCalendarHelper.toPersianDigits(selectedYear)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = {
                        val (ny, nm) = JalaliCalendarHelper.addMonths(selectedYear, selectedMonth, 1)
                        onYearChange(ny)
                        onMonthChange(nm)
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "ماه بعد")
                    }
                }
            }
        }
        "YEARLY", "YEAR" -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onYearChange(selectedYear - 1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "سال قبل")
                    }

                    Text(
                        text = "سال ${JalaliCalendarHelper.toPersianDigits(selectedYear)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { onYearChange(selectedYear + 1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "سال بعد")
                    }
                }
            }
        }
        "CUSTOM" -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShowStartDatePicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (startDateJalali.isNotBlank()) "از: ${JalaliCalendarHelper.toPersianDigits(startDateJalali)}" else "از تاریخ...",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = onShowEndDatePicker,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (endDateJalali.isNotBlank()) "تا: ${JalaliCalendarHelper.toPersianDigits(endDateJalali)}" else "تا تاریخ...",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
