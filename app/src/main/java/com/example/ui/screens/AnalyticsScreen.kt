package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.FilterPreferences
import com.example.util.JalaliCalendarHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    selectedMonth: String,
    currencyUnit: CurrencyUnit,
    onMonthChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val filterPrefs = remember { FilterPreferences(context) }

    // Filter States
    var selectedAccountId by remember { mutableStateOf(filterPrefs.analyticsAccountId) }
    var selectedCategoryId by remember { mutableStateOf(filterPrefs.analyticsCategoryId) }
    var dateFilterMode by remember { mutableStateOf(filterPrefs.analyticsDateMode) }

    val todayJalali = remember { JalaliCalendarHelper.getCurrentJalaliDate() }
    var selectedDailyDate by remember { mutableStateOf(todayJalali) }
    var selectedWeeklyEndDate by remember { mutableStateOf(todayJalali) }

    val savedYear = filterPrefs.analyticsYear
    val savedMonth = filterPrefs.analyticsMonth
    var selectedYearInt by remember { mutableIntStateOf(if (savedYear != -1) savedYear else todayJalali.year) }
    var selectedMonthInt by remember { mutableIntStateOf(if (savedMonth != -1) savedMonth else todayJalali.month) }
    var startDateJalali by remember { mutableStateOf(filterPrefs.analyticsStartDate) }
    var endDateJalali by remember { mutableStateOf(filterPrefs.analyticsEndDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Dropdown expanded states
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isFilterVisible by remember { mutableStateOf(filterPrefs.analyticsIsFilterVisible) }

    LaunchedEffect(
        selectedAccountId, selectedCategoryId, dateFilterMode,
        selectedYearInt, selectedMonthInt, startDateJalali, endDateJalali, isFilterVisible
    ) {
        filterPrefs.analyticsAccountId = selectedAccountId
        filterPrefs.analyticsCategoryId = selectedCategoryId
        filterPrefs.analyticsDateMode = dateFilterMode
        filterPrefs.analyticsYear = selectedYearInt
        filterPrefs.analyticsMonth = selectedMonthInt
        filterPrefs.analyticsStartDate = startDateJalali
        filterPrefs.analyticsEndDate = endDateJalali
        filterPrefs.analyticsIsFilterVisible = isFilterVisible
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    val filteredTransactions = remember(
        transactions, dateFilterMode, selectedDailyDate, selectedWeeklyEndDate,
        selectedYearInt, selectedMonthInt, startDateJalali, endDateJalali,
        selectedAccountId, selectedCategoryId
    ) {
        transactions.filter { tx ->
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
                "MONTH" -> {
                    val mStr = String.format(java.util.Locale.US, "%04d/%02d", selectedYearInt, selectedMonthInt)
                    tx.jalaliDate.startsWith(mStr)
                }
                "YEAR" -> tx.jalaliDate.startsWith(selectedYearInt.toString())
                "ALL" -> true
                "CUSTOM" -> {
                    val afterStart = if (startDateJalali.isNotBlank()) tx.jalaliDate >= startDateJalali else true
                    val beforeEnd = if (endDateJalali.isNotBlank()) tx.jalaliDate <= endDateJalali else true
                    afterStart && beforeEnd
                }
                else -> true
            }

            val matchesAccount = (selectedAccountId == null || tx.accountId == selectedAccountId)
            val matchesCategory = (selectedCategoryId == null || tx.categoryId == selectedCategoryId || tx.subcategoryId == selectedCategoryId)

            matchesDate && matchesAccount && matchesCategory
        }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    val netBalance = totalIncome - totalExpense

    data class CategoryStat(
        val name: String,
        val colorHex: String,
        val amount: Double
    )

    val categoryStats = remember(filteredTransactions, categories) {
        val catMap = categories.associateBy { it.id }
        val installmentCat = categories.firstOrNull { it.name.contains("اقساط") }
        val chequeCat = categories.firstOrNull { it.name.contains("چک") }
        val otherCat = categories.firstOrNull { it.name.contains("سایر") }

        filteredTransactions.filter { it.type == "EXPENSE" }
            .groupBy { tx ->
                if (tx.categoryId != null && catMap.containsKey(tx.categoryId)) {
                    catMap[tx.categoryId]!!
                } else if (tx.title.contains("قسط") || tx.title.contains("وام")) {
                    installmentCat ?: CategoryEntity(name = "اقساط، وام و بدهی", type = "EXPENSE", colorHex = "#8B5CF6")
                } else if (tx.title.contains("چک")) {
                    chequeCat ?: CategoryEntity(name = "چک و تعهدات مالی", type = "EXPENSE", colorHex = "#06B6D4")
                } else {
                    otherCat ?: CategoryEntity(name = "سایر هزینه‌ها", type = "EXPENSE", colorHex = "#64748B")
                }
            }
            .map { (cat, txList) ->
                CategoryStat(
                    name = cat.name,
                    colorHex = cat.colorHex,
                    amount = txList.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.amount }
    }

    val hasActiveFilter = selectedAccountId != null || selectedCategoryId != null || dateFilterMode != "MONTH"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Control Panel Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isFilterVisible = !isFilterVisible },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "فیلتر پیشرفته آمار",
                                    style = MaterialTheme.typography.titleMedium,
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
                                    TextButton(onClick = {
                                        selectedAccountId = null
                                        selectedCategoryId = null
                                        dateFilterMode = "MONTH"
                                        selectedYearInt = todayJalali.year
                                        selectedMonthInt = todayJalali.month
                                        startDateJalali = ""
                                        endDateJalali = ""
                                        filterPrefs.resetAnalyticsFilters()
                                    }) {
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
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Date Filter Mode Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
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
                                        selected = dateFilterMode == "MONTH",
                                        onClick = { dateFilterMode = "MONTH" },
                                        label = { Text("ماهانه", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = dateFilterMode == "YEAR",
                                        onClick = { dateFilterMode = "YEAR" },
                                        label = { Text("سالانه", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = dateFilterMode == "ALL",
                                        onClick = { dateFilterMode = "ALL" },
                                        label = { Text("همه", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = dateFilterMode == "CUSTOM",
                                        onClick = { dateFilterMode = "CUSTOM" },
                                        label = { Text("بازه زمانی", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }

                                // Date Selector Display
                                DateFilterNavigator(
                                    dateFilterMode = dateFilterMode,
                                    selectedDailyDate = selectedDailyDate,
                                    onDailyDateChange = { selectedDailyDate = it },
                                    selectedWeeklyEndDate = selectedWeeklyEndDate,
                                    onWeeklyEndDateChange = { selectedWeeklyEndDate = it },
                                    selectedYear = selectedYearInt,
                                    onYearChange = { selectedYearInt = it },
                                    selectedMonth = selectedMonthInt,
                                    onMonthChange = { selectedMonthInt = it },
                                    startDateJalali = startDateJalali,
                                    onStartDateChange = { startDateJalali = it },
                                    endDateJalali = endDateJalali,
                                    onEndDateChange = { endDateJalali = it },
                                    onShowStartDatePicker = { showStartDatePicker = true },
                                    onShowEndDatePicker = { showEndDatePicker = true }
                                )

                                // Account and Category Dropdowns Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Account Filter Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = accountDropdownExpanded,
                                        onExpandedChange = { accountDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "همه حساب‌ها"
                                        OutlinedTextField(
                                            value = selectedAccountName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("حساب", style = MaterialTheme.typography.labelSmall) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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

                                    // Category Filter Dropdown
                                    ExposedDropdownMenuBox(
                                        expanded = categoryDropdownExpanded,
                                        onExpandedChange = { categoryDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val selectedCat = categories.find { it.id == selectedCategoryId }
                                        val selectedCategoryName = selectedCat?.name ?: "همه دسته‌بندی‌ها"
                                        OutlinedTextField(
                                            value = selectedCategoryName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("دسته‌بندی", style = MaterialTheme.typography.labelSmall) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
        }

            // Summary Totals Card
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
                        val periodLabel = when (dateFilterMode) {
                            "DAILY" -> "امروز"
                            "WEEKLY" -> "هفته جاری (شنبه تا جمعه)"
                            "MONTH" -> "${JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(selectedMonthInt - 1) { "" }} ${JalaliCalendarHelper.toPersianDigits(selectedYearInt)}"
                            "YEAR" -> "سال ${JalaliCalendarHelper.toPersianDigits(selectedYearInt)}"
                            "ALL" -> "همه زمان‌ها"
                            else -> "بازه زمانی انتخاب‌شده"
                        }
                        Text(
                            text = "خلاصه تراکنش‌های $periodLabel (${JalaliCalendarHelper.toPersianDigits(filteredTransactions.size)} تراکنش)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("مجموع درآمد", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(
                                    text = CurrencyHelper.formatAmount(totalIncome, currencyUnit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("مجموع هزینه", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(
                                    text = CurrencyHelper.formatAmount(totalExpense, currencyUnit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تفاضل کل (مانده):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = CurrencyHelper.formatAmount(netBalance, currencyUnit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // Donut Chart Card
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تحلیل سهم هزینه‌ها به تفکیک دسته‌بندی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (categoryStats.isEmpty() || totalExpense <= 0) {
                            Text("هیچ هزینه‌ای با فیلترهای انتخابی یافت نشد.", color = Color.Gray)
                        } else {
                            // Donut Chart Canvas
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = -90f
                                    val strokeWidth = 36.dp.toPx()

                                    categoryStats.forEach { stat ->
                                        val color = CategoryIconHelper.parseColor(stat.colorHex)
                                        val sweepAngle = ((stat.amount / totalExpense) * 360f).toFloat()

                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth)
                                        )
                                        startAngle += sweepAngle
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("کل هزینه", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(
                                        text = CurrencyHelper.formatAmount(totalExpense, currencyUnit, includeUnit = false),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Legend List
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryStats.forEach { stat ->
                                    val color = CategoryIconHelper.parseColor(stat.colorHex)
                                    val percent = ((stat.amount / totalExpense) * 100).toInt()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stat.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${JalaliCalendarHelper.toPersianDigits(percent)}٪",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = CurrencyHelper.formatAmount(stat.amount, currencyUnit),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
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

    // Date Picker Dialogs
    if (showStartDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = startDateJalali.ifBlank { JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString() },
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { dateStr ->
                startDateJalali = dateStr
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = endDateJalali.ifBlank { JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString() },
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { dateStr ->
                endDateJalali = dateStr
                showEndDatePicker = false
            }
        )
    }
}
