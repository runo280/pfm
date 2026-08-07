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
import androidx.compose.ui.unit.sp
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
import com.example.util.JalaliDate

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

    val prevPeriodLabel = remember(dateFilterMode) {
        when (dateFilterMode) {
            "DAILY" -> "روز قبل"
            "WEEKLY" -> "هفته قبل"
            "MONTH" -> "ماه قبل"
            "YEAR" -> "سال قبل"
            "CUSTOM" -> "دوره قبل"
            else -> null
        }
    }

    val prevFilteredTransactions = remember(
        transactions, dateFilterMode, selectedDailyDate, selectedWeeklyEndDate,
        selectedYearInt, selectedMonthInt, startDateJalali, endDateJalali,
        selectedAccountId, selectedCategoryId
    ) {
        if (dateFilterMode == "ALL") emptyList()
        else {
            transactions.filter { tx ->
                val matchesDate = when (dateFilterMode) {
                    "DAILY" -> {
                        val prevDay = JalaliCalendarHelper.addDays(selectedDailyDate, -1)
                        tx.jalaliDate == prevDay.toFormattedString()
                    }
                    "WEEKLY" -> {
                        val parsed = JalaliCalendarHelper.parseJalaliDate(tx.jalaliDate)
                        if (parsed != null) {
                            val txJdn = JalaliCalendarHelper.jalaliToJdn(parsed.year, parsed.month, parsed.day)
                            val prevWeeklyEnd = JalaliCalendarHelper.addDays(selectedWeeklyEndDate, -7)
                            val (prevStart, prevEnd) = JalaliCalendarHelper.getWeekRange(prevWeeklyEnd)
                            val startJdn = JalaliCalendarHelper.jalaliToJdn(prevStart.year, prevStart.month, prevStart.day)
                            val endJdn = JalaliCalendarHelper.jalaliToJdn(prevEnd.year, prevEnd.month, prevEnd.day)
                            txJdn in startJdn..endJdn
                        } else false
                    }
                    "MONTH" -> {
                        val (prevY, prevM) = if (selectedMonthInt == 1) {
                            Pair(selectedYearInt - 1, 12)
                        } else {
                            Pair(selectedYearInt, selectedMonthInt - 1)
                        }
                        val mStr = String.format(java.util.Locale.US, "%04d/%02d", prevY, prevM)
                        tx.jalaliDate.startsWith(mStr)
                    }
                    "YEAR" -> {
                        val prevY = selectedYearInt - 1
                        tx.jalaliDate.startsWith(prevY.toString())
                    }
                    "CUSTOM" -> {
                        val startP = JalaliCalendarHelper.parseJalaliDate(startDateJalali)
                        val endP = JalaliCalendarHelper.parseJalaliDate(endDateJalali)
                        if (startP != null && endP != null) {
                            val sJdn = JalaliCalendarHelper.jalaliToJdn(startP.year, startP.month, startP.day)
                            val eJdn = JalaliCalendarHelper.jalaliToJdn(endP.year, endP.month, endP.day)
                            val duration = eJdn - sJdn + 1
                            if (duration > 0) {
                                val prevSJdn = sJdn - duration
                                val prevEJdn = sJdn - 1
                                val parsed = JalaliCalendarHelper.parseJalaliDate(tx.jalaliDate)
                                if (parsed != null) {
                                    val txJdn = JalaliCalendarHelper.jalaliToJdn(parsed.year, parsed.month, parsed.day)
                                    txJdn in prevSJdn..prevEJdn
                                } else false
                            } else false
                        } else false
                    }
                    else -> false
                }

                val matchesAccount = (selectedAccountId == null || tx.accountId == selectedAccountId)
                val matchesCategory = (selectedCategoryId == null || tx.categoryId == selectedCategoryId || tx.subcategoryId == selectedCategoryId)

                matchesDate && matchesAccount && matchesCategory
            }
        }
    }

    val prevTotalIncome = remember(prevFilteredTransactions) {
        prevFilteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val prevTotalExpense = remember(prevFilteredTransactions) {
        prevFilteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("مجموع درآمد", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(
                                    text = CurrencyHelper.formatAmount(totalIncome, currencyUnit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                                if (prevPeriodLabel != null) {
                                    ChangePercentBadge(
                                        current = totalIncome,
                                        previous = prevTotalIncome,
                                        isIncome = true,
                                        periodName = prevPeriodLabel
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("مجموع هزینه", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(
                                    text = CurrencyHelper.formatAmount(totalExpense, currencyUnit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                                if (prevPeriodLabel != null) {
                                    ChangePercentBadge(
                                        current = totalExpense,
                                        previous = prevTotalExpense,
                                        isIncome = false,
                                        periodName = prevPeriodLabel
                                    )
                                }
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

            // Periodic Comparison Bar Chart Card
            if (dateFilterMode in listOf("DAILY", "WEEKLY", "MONTH", "YEAR")) {
                item {
                    val comparisonTitle = when (dateFilterMode) {
                        "DAILY" -> {
                            val monthName = JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrNull(selectedDailyDate.month - 1) ?: ""
                            "مقایسه روزهای ماه ($monthName ${JalaliCalendarHelper.toPersianDigits(selectedDailyDate.year)})"
                        }
                        "WEEKLY" -> "مقایسه ۷ هفته اخیر"
                        "MONTH" -> "مقایسه ماه‌های سال (${JalaliCalendarHelper.toPersianDigits(selectedYearInt)})"
                        "YEAR" -> "مقایسه سال‌های موجود"
                        else -> "نمودار مقایسه دوره"
                    }

                    val comparisonBars = remember(
                        transactions, dateFilterMode, selectedDailyDate, selectedWeeklyEndDate,
                        selectedYearInt, selectedMonthInt, selectedAccountId, selectedCategoryId
                    ) {
                        when (dateFilterMode) {
                            "DAILY" -> {
                                val daysInMonth = JalaliCalendarHelper.getDaysInJalaliMonth(selectedDailyDate.year, selectedDailyDate.month)
                                val monthName = JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrNull(selectedDailyDate.month - 1) ?: ""
                                (1..daysInMonth).map { day ->
                                    val dateStr = String.format(java.util.Locale.US, "%04d/%02d/%02d", selectedDailyDate.year, selectedDailyDate.month, day)
                                    val dayTxs = transactions.filter { tx ->
                                        tx.jalaliDate == dateStr &&
                                        (selectedAccountId == null || tx.accountId == selectedAccountId) &&
                                        (selectedCategoryId == null || tx.categoryId == selectedCategoryId || tx.subcategoryId == selectedCategoryId)
                                    }
                                    val inc = dayTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                                    val exp = dayTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                    BarChartItem(
                                        id = "day_$day",
                                        label = JalaliCalendarHelper.toPersianDigits(day),
                                        fullTitle = "${JalaliCalendarHelper.toPersianDigits(day)} $monthName ${JalaliCalendarHelper.toPersianDigits(selectedDailyDate.year)}",
                                        income = inc,
                                        expense = exp,
                                        isSelected = (day == selectedDailyDate.day),
                                        onClick = { selectedDailyDate = JalaliDate(selectedDailyDate.year, selectedDailyDate.month, day) }
                                    )
                                }
                            }
                            "WEEKLY" -> {
                                (6 downTo 0).map { weekIndex ->
                                    val weekRefDate = JalaliCalendarHelper.addDays(selectedWeeklyEndDate, -weekIndex * 7)
                                    val (wStart, wEnd) = JalaliCalendarHelper.getWeekRange(weekRefDate)
                                    val startJdn = JalaliCalendarHelper.jalaliToJdn(wStart.year, wStart.month, wStart.day)
                                    val endJdn = JalaliCalendarHelper.jalaliToJdn(wEnd.year, wEnd.month, wEnd.day)

                                    val weekTxs = transactions.filter { tx ->
                                        val parsed = JalaliCalendarHelper.parseJalaliDate(tx.jalaliDate)
                                        if (parsed != null) {
                                            val jdn = JalaliCalendarHelper.jalaliToJdn(parsed.year, parsed.month, parsed.day)
                                            jdn in startJdn..endJdn &&
                                            (selectedAccountId == null || tx.accountId == selectedAccountId) &&
                                            (selectedCategoryId == null || tx.categoryId == selectedCategoryId || tx.subcategoryId == selectedCategoryId)
                                        } else false
                                    }
                                    val inc = weekTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                                    val exp = weekTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                    val fullRangeTitle = "هفته از ${wStart.toReadablePersianString()} تا ${wEnd.toReadablePersianString()}"

                                    BarChartItem(
                                        id = "week_$weekIndex",
                                        label = if (weekIndex == 0) "جاری" else JalaliCalendarHelper.toPersianDigits(7 - weekIndex),
                                        fullTitle = fullRangeTitle,
                                        income = inc,
                                        expense = exp,
                                        isSelected = (weekIndex == 0),
                                        onClick = { selectedWeeklyEndDate = wEnd }
                                    )
                                }
                            }
                            "MONTH" -> {
                                (1..12).map { month ->
                                    val mPrefix = String.format(java.util.Locale.US, "%04d/%02d", selectedYearInt, month)
                                    val monthTxs = transactions.filter { tx ->
                                        tx.jalaliDate.startsWith(mPrefix) &&
                                        (selectedAccountId == null || tx.accountId == selectedAccountId) &&
                                        (selectedCategoryId == null || tx.categoryId == selectedCategoryId || tx.subcategoryId == selectedCategoryId)
                                    }
                                    val inc = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                                    val exp = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                    val mName = JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrNull(month - 1) ?: ""

                                    BarChartItem(
                                        id = "month_$month",
                                        label = mName.take(4),
                                        fullTitle = "$mName ${JalaliCalendarHelper.toPersianDigits(selectedYearInt)}",
                                        income = inc,
                                        expense = exp,
                                        isSelected = (month == selectedMonthInt),
                                        onClick = { selectedMonthInt = month }
                                    )
                                }
                            }
                            "YEAR" -> {
                                val txYears = transactions.mapNotNull { JalaliCalendarHelper.parseJalaliDate(it.jalaliDate)?.year }
                                val minYr = (txYears.minOrNull() ?: selectedYearInt).coerceAtMost(selectedYearInt - 3)
                                val maxYr = (txYears.maxOrNull() ?: selectedYearInt).coerceAtLeast(selectedYearInt + 1)

                                (minYr..maxYr).map { yr ->
                                    val yPrefix = String.format(java.util.Locale.US, "%04d/", yr)
                                    val yearTxs = transactions.filter { tx ->
                                        tx.jalaliDate.startsWith(yPrefix) &&
                                        (selectedAccountId == null || tx.accountId == selectedAccountId) &&
                                        (selectedCategoryId == null || tx.categoryId == selectedCategoryId || tx.subcategoryId == selectedCategoryId)
                                    }
                                    val inc = yearTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                                    val exp = yearTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                                    BarChartItem(
                                        id = "year_$yr",
                                        label = JalaliCalendarHelper.toPersianDigits(yr),
                                        fullTitle = "سال ${JalaliCalendarHelper.toPersianDigits(yr)}",
                                        income = inc,
                                        expense = exp,
                                        isSelected = (yr == selectedYearInt),
                                        onClick = { selectedYearInt = yr }
                                    )
                                }
                            }
                            else -> emptyList()
                        }
                    }

                    PeriodComparisonBarChart(
                        title = comparisonTitle,
                        items = comparisonBars,
                        currencyUnit = currencyUnit
                    )
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

data class BarChartItem(
    val id: String,
    val label: String,
    val fullTitle: String,
    val income: Double,
    val expense: Double,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun PeriodComparisonBarChart(
    title: String,
    items: List<BarChartItem>,
    currencyUnit: CurrencyUnit
) {
    var activeItem by remember(items) { mutableStateOf(items.firstOrNull { it.isSelected } ?: items.firstOrNull()) }

    LaunchedEffect(items) {
        val selected = items.firstOrNull { it.isSelected }
        if (selected != null) {
            activeItem = selected
        }
    }

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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "درآمد",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "هزینه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Item Details Summary Box
            activeItem?.let { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.fullTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "درآمد: ${CurrencyHelper.formatAmount(item.income, currencyUnit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = IncomeGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "هزینه: ${CurrencyHelper.formatAmount(item.expense, currencyUnit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ExpenseRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (items.isEmpty()) {
                Text(
                    text = "اطلاعاتی برای نمایش وجود ندارد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val maxVal = items.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    items.forEach { item ->
                        val isItemActive = (activeItem?.id == item.id) || item.isSelected

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isItemActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    activeItem = item
                                    item.onClick()
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(120.dp)
                                    .width(if (items.size > 15) 22.dp else 36.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxHeight(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val incFraction = (item.income / maxVal).toFloat().coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(incFraction.coerceAtLeast(if (item.income > 0) 0.05f else 0.01f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (item.income > 0) IncomeGreen else IncomeGreen.copy(alpha = 0.15f))
                                    )

                                    val expFraction = (item.expense / maxVal).toFloat().coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(expFraction.coerceAtLeast(if (item.expense > 0) 0.05f else 0.01f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (item.expense > 0) ExpenseRed else ExpenseRed.copy(alpha = 0.15f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = if (items.size > 20) 9.sp else 11.sp,
                                fontWeight = if (isItemActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isItemActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePercentBadge(
    current: Double,
    previous: Double,
    isIncome: Boolean,
    periodName: String
) {
    if (current == 0.0 && previous == 0.0) {
        Text(
            text = "بدون تغییر نسبت به $periodName",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        return
    }

    val pct = if (previous > 0) {
        ((current - previous) / previous) * 100.0
    } else if (current > 0) {
        100.0
    } else {
        -100.0
    }

    val isIncrease = pct > 0
    val isZero = Math.abs(pct) < 0.05

    val icon = if (isIncrease) Icons.Default.ArrowUpward else if (isZero) Icons.Default.Remove else Icons.Default.ArrowDownward

    // Color logic:
    // Income: increase is green, decrease is red
    // Expense: increase is red (more spending), decrease is green (less spending)
    val tintColor = if (isZero) Color.Gray else if (isIncome) {
        if (isIncrease) IncomeGreen else ExpenseRed
    } else {
        if (isIncrease) ExpenseRed else IncomeGreen
    }

    val absPctStr = formatPercentValue(Math.abs(pct))
    val textLabel = if (isZero) {
        "بدون تغییر نسبت به $periodName"
    } else {
        val direction = if (isIncrease) "افزایش" else "کاهش"
        "٪$absPctStr $direction نسبت به $periodName"
    }

    Surface(
        color = tintColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = textLabel,
                style = MaterialTheme.typography.labelSmall,
                color = tintColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatPercentValue(absVal: Double): String {
    val formatted = if (absVal % 1.0 == 0.0) {
        String.format(java.util.Locale.US, "%.0f", absVal)
    } else {
        String.format(java.util.Locale.US, "%.1f", absVal)
    }
    return JalaliCalendarHelper.toPersianDigits(formatted)
}
