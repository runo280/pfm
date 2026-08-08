package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.*
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.JalaliCalendarHelper
import com.example.util.JalaliDate

private val DotIncomeColor = IncomeGreen
private val DotExpenseColor = ExpenseRed
private val DotInstallmentColor = FineteekBlue
private val DotChequeColor = Color(0xFF8B5CF6) // Purple
private val DotDebtColor = GoldWarning

data class DayIndicatorStatus(
    val hasIncome: Boolean = false,
    val hasExpense: Boolean = false,
    val hasInstallment: Boolean = false,
    val hasCheque: Boolean = false,
    val hasDebt: Boolean = false
) {
    val hasAny: Boolean get() = hasIncome || hasExpense || hasInstallment || hasCheque || hasDebt
}

data class CalendarInstallmentMatch(
    val installment: InstallmentEntity,
    val itemNumber: Int,
    val isPaid: Boolean,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    transactions: List<TransactionEntity>,
    installments: List<InstallmentEntity>,
    cheques: List<ChequeEntity>,
    debts: List<DebtEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    currencyUnit: CurrencyUnit
) {
    val today = remember { JalaliCalendarHelper.getCurrentJalaliDate() }
    
    // View state
    var currentYearMonth by remember { mutableStateOf(Pair(today.year, today.month)) }
    var selectedDate by remember { mutableStateOf(today) }

    // Visibility Settings Toggles
    var showTransactions by remember { mutableStateOf(true) }
    var showInstallments by remember { mutableStateOf(true) }
    var showCheques by remember { mutableStateOf(true) }
    var showDebts by remember { mutableStateOf(true) }

    val year = currentYearMonth.first
    val month = currentYearMonth.second

    val daysInMonth = remember(year, month) {
        JalaliCalendarHelper.getDaysInJalaliMonth(year, month)
    }

    val firstDayOfWeekIndex = remember(year, month) {
        JalaliCalendarHelper.getDayOfWeekIndex(JalaliDate(year, month, 1))
    }

    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    // Pre-calculate installment due dates mapping for performance
    val monthInstallmentItems = remember(installments, year, month) {
        val matchesMap = mutableMapOf<String, MutableList<CalendarInstallmentMatch>>()
        installments.filter { it.status == "ACTIVE" }.forEach { inst ->
            val total = inst.totalInstallments
            for (k in 1..total) {
                val due = JalaliCalendarHelper.getInstallmentItemDueDate(inst, k)
                if (due.year == year && due.month == month) {
                    val key = due.toFormattedString()
                    val list = matchesMap.getOrPut(key) { mutableListOf() }
                    list.add(
                        CalendarInstallmentMatch(
                            installment = inst,
                            itemNumber = k,
                            isPaid = k <= inst.paidInstallments,
                            amount = inst.monthlyPayment
                        )
                    )
                }
            }
        }
        matchesMap
    }

    // Map day dates in current month to their indicator statuses
    val monthIndicators = remember(
        year, month, daysInMonth, transactions, cheques, debts, monthInstallmentItems,
        showTransactions, showInstallments, showCheques, showDebts
    ) {
        val map = mutableMapOf<Int, DayIndicatorStatus>()
        for (day in 1..daysInMonth) {
            val date = JalaliDate(year, month, day)
            val dateStr = date.toFormattedString()

            val txsForDay = if (showTransactions) transactions.filter { it.jalaliDate == dateStr } else emptyList()
            val hasIncome = txsForDay.any { it.type == "INCOME" }
            val hasExpense = txsForDay.any { it.type == "EXPENSE" }

            val hasInstallment = showInstallments && (monthInstallmentItems[dateStr]?.isNotEmpty() == true)
            val hasCheque = showCheques && cheques.any { it.dueDateJalali == dateStr }
            val hasDebt = showDebts && debts.any { it.dueDateJalali == dateStr || it.createdDateJalali == dateStr }

            map[day] = DayIndicatorStatus(
                hasIncome = hasIncome,
                hasExpense = hasExpense,
                hasInstallment = hasInstallment,
                hasCheque = hasCheque,
                hasDebt = hasDebt
            )
        }
        map
    }

    // Items for currently selected date
    val selectedDateStr = selectedDate.toFormattedString()

    val selectedDayTransactions = remember(selectedDateStr, transactions, showTransactions) {
        if (!showTransactions) emptyList()
        else transactions.filter { it.jalaliDate == selectedDateStr }
    }

    val selectedDayInstallmentMatches = remember(selectedDateStr, monthInstallmentItems, showInstallments) {
        if (!showInstallments) emptyList()
        else monthInstallmentItems[selectedDateStr] ?: emptyList()
    }

    val selectedDayCheques = remember(selectedDateStr, cheques, showCheques) {
        if (!showCheques) emptyList()
        else cheques.filter { it.dueDateJalali == selectedDateStr }
    }

    val selectedDayDebts = remember(selectedDateStr, debts, showDebts) {
        if (!showDebts) emptyList()
        else debts.filter { it.dueDateJalali == selectedDateStr || it.createdDateJalali == selectedDateStr }
    }

    val totalSelectedItemsCount = selectedDayTransactions.size +
            selectedDayInstallmentMatches.size +
            selectedDayCheques.size +
            selectedDayDebts.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Top Month Navigation Header ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Month Button (Left in RTL = forward in time)
                IconButton(onClick = {
                    val next = JalaliCalendarHelper.addMonths(year, month, 1)
                    currentYearMonth = next
                    selectedDate = JalaliDate(next.first, next.second, 1)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ماه بعد"
                    )
                }

                // Title and Jump to Today
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrNull(month - 1)} ${JalaliCalendarHelper.toPersianDigits(year)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                currentYearMonth = Pair(today.year, today.month)
                                selectedDate = today
                            },
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "امروز",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Previous Month Button (Right in RTL = backward in time)
                IconButton(onClick = {
                    val prev = JalaliCalendarHelper.addMonths(year, month, -1)
                    currentYearMonth = prev
                    selectedDate = JalaliDate(prev.first, prev.second, 1)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "ماه قبل"
                    )
                }
            }
        }

        // --- Filter Toggles Bar ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = showTransactions,
                    onClick = { showTransactions = !showTransactions },
                    label = { Text("درآمد و هزینه", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DotIncomeColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreenContainer,
                        selectedLabelColor = IncomeGreen
                    )
                )
            }
            item {
                FilterChip(
                    selected = showInstallments,
                    onClick = { showInstallments = !showInstallments },
                    label = { Text("اقساط", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DotInstallmentColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            item {
                FilterChip(
                    selected = showCheques,
                    onClick = { showCheques = !showCheques },
                    label = { Text("چک‌ها", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DotChequeColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DotChequeColor.copy(alpha = 0.15f),
                        selectedLabelColor = DotChequeColor
                    )
                )
            }
            item {
                FilterChip(
                    selected = showDebts,
                    onClick = { showDebts = !showDebts },
                    label = { Text("طلب و بدهی", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DotDebtColor)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldContainer,
                        selectedLabelColor = GoldWarning
                    )
                )
            }
        }

        // --- Calendar Month Grid Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Weekday Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val weekDays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                    weekDays.forEachIndexed { index, dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (index == 6) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid Layout
                val totalCells = firstDayOfWeekIndex + daysInMonth
                val rowsCount = (totalCells + 6) / 7

                for (r in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - firstDayOfWeekIndex + 1

                            if (dayNumber in 1..daysInMonth) {
                                val date = JalaliDate(year, month, dayNumber)
                                val isToday = (date == today)
                                val isSelected = (date == selectedDate)
                                val isFriday = (c == 6)
                                val status = monthIndicators[dayNumber] ?: DayIndicatorStatus()

                                DayCell(
                                    dayNumber = dayNumber,
                                    isToday = isToday,
                                    isSelected = isSelected,
                                    isFriday = isFriday,
                                    status = status,
                                    onClick = { selectedDate = date },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                )
                            }
                        }
                    }
                    if (r < rowsCount - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // --- Selected Day Details Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDate.toReadablePersianString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${JalaliCalendarHelper.toPersianDigits(totalSelectedItemsCount)} مورد",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (totalSelectedItemsCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "هیچ تراکنش یا سررسیدی برای این روز ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 1. Transactions
                items(selectedDayTransactions, key = { "tx_${it.id}" }) { tx ->
                    val category = categoryMap[tx.categoryId]
                    val account = accountMap[tx.accountId]
                    val isIncome = tx.type == "INCOME"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isIncome) IncomeGreenContainer else ExpenseRedContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(category?.iconName ?: "Category"),
                                            contentDescription = null,
                                            tint = if (isIncome) IncomeGreen else ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tx.title.ifBlank { category?.name ?: "تراکنش" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${if (isIncome) "درآمد" else "هزینه"} • ${account?.name ?: "حساب"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Text(
                                text = "${if (isIncome) "+" else "-"}${CurrencyHelper.formatAmount(tx.amount, currencyUnit)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }

                // 2. Installments
                items(selectedDayInstallmentMatches, key = { "inst_${it.installment.id}_${it.itemNumber}" }) { match ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = FineteekBlueContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = FineteekBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${match.installment.title} (قسط ${JalaliCalendarHelper.toPersianDigits(match.itemNumber)} از ${JalaliCalendarHelper.toPersianDigits(match.installment.totalInstallments)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (match.isPaid) "پرداخت شده" else "موعد پرداخت قسط",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (match.isPaid) IncomeGreen else FineteekBlue
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyHelper.formatAmount(match.amount, currencyUnit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (match.isPaid) IncomeGreenContainer else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (match.isPaid) "پرداخت شده" else "در انتظار پرداخت",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (match.isPaid) IncomeGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Cheques
                items(selectedDayCheques, key = { "chq_${it.id}" }) { chq ->
                    val isReceivable = chq.type == "RECEIVABLE"
                    val isPassed = chq.status == "PASSED"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DotChequeColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = DotChequeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "چک ${if (isReceivable) "دریافتی" else "پرداختی"} ${chq.bankName} (${JalaliCalendarHelper.toPersianDigits(chq.chequeNumber)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "طرف حساب: ${chq.payeeOrDrawer}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyHelper.formatAmount(chq.amount, currencyUnit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReceivable) IncomeGreen else ExpenseRed
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPassed) IncomeGreenContainer else DotChequeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isPassed) "پاس شده" else "در انتظار سررسید",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPassed) IncomeGreen else DotChequeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Debts & Receivables
                items(selectedDayDebts, key = { "debt_${it.id}" }) { debt ->
                    val isReceivable = debt.type == "RECEIVABLE"
                    val isSettled = debt.status == "SETTLED"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = GoldContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FolderShared,
                                            contentDescription = null,
                                            tint = GoldWarning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${if (isReceivable) "طلب از" else "بدهی به"} ${debt.personName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (debt.note.isNotBlank()) debt.note else if (isReceivable) "طلبکاری" else "بدهکاری",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyHelper.formatAmount(debt.amount, currencyUnit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReceivable) IncomeGreen else ExpenseRed
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSettled) IncomeGreenContainer else GoldContainer
                                ) {
                                    Text(
                                        text = if (isSettled) "تسویه شده" else "جاری",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSettled) IncomeGreen else GoldWarning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
fun DayCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isFriday: Boolean,
    status: DayIndicatorStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = JalaliCalendarHelper.toPersianDigits(dayNumber),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFriday -> ExpenseRed
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Indicator Dots Row
            if (status.hasAny) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeDotColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else null

                    if (status.hasIncome) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(activeDotColor ?: DotIncomeColor)
                        )
                    }
                    if (status.hasExpense) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(activeDotColor ?: DotExpenseColor)
                        )
                    }
                    if (status.hasInstallment) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(activeDotColor ?: DotInstallmentColor)
                        )
                    }
                    if (status.hasCheque) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(activeDotColor ?: DotChequeColor)
                        )
                    }
                    if (status.hasDebt) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(activeDotColor ?: DotDebtColor)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
