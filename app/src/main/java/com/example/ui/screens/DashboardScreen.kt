package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.ui.components.AccountHorizontalSelector
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.CategoryTwoLevelSelector
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.*
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.JalaliCalendarHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    currencyUnit: CurrencyUnit,
    onAddTransaction: (TransactionEntity) -> Unit,
    onTransfer: (fromAccountId: Long, toAccountId: Long, amount: Double, fee: Double, note: String) -> Unit
) {
    val context = LocalContext.current
    val totalBalance = remember(accounts) { accounts.sumOf { it.balance } }

    val currentJalaliMonth = remember { JalaliCalendarHelper.getCurrentJalaliYearMonth() }
    val monthTransactions = remember(transactions, currentJalaliMonth) {
        transactions.filter { it.jalaliDate.startsWith(currentJalaliMonth) }
    }

    val currentMonthIncome = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val currentMonthExpense = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    // Form states for embedded transaction / transfer entry
    var selectedFormType by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME", "TRANSFER"

    var amountValue by remember { mutableStateOf(TextFieldValue("")) }
    var transferFeeValue by remember { mutableStateOf(TextFieldValue("")) }
    var note by remember { mutableStateOf("") }

    val defaultAccount = remember(accounts) { accounts.find { it.isDefault } ?: accounts.firstOrNull() }
    var selectedAccountId by remember { mutableStateOf(defaultAccount?.id ?: 1L) }
    var selectedTargetAccountId by remember {
        mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1L)
    }

    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedSubcategoryId by remember { mutableStateOf<Long?>(null) }
    var jalaliDateStr by remember { mutableStateOf(JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()) }

    var showDatePicker by remember { mutableStateOf(false) }

    // Reset category selection when form type changes
    LaunchedEffect(selectedFormType) {
        if (selectedFormType != "TRANSFER") {
            selectedCategoryId = null
            selectedSubcategoryId = null
        }
    }

    // Keep account selections valid if accounts update
    LaunchedEffect(accounts) {
        if (accounts.none { it.id == selectedAccountId }) {
            selectedAccountId = defaultAccount?.id ?: accounts.firstOrNull()?.id ?: 1L
        }
        if (accounts.none { it.id == selectedTargetAccountId }) {
            selectedTargetAccountId = accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1L
        }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today Date Header
        item {
            val today = remember { JalaliCalendarHelper.getCurrentJalaliDate() }
            val totalDaysInMonth = remember(today) { JalaliCalendarHelper.getDaysInJalaliMonth(today.year, today.month) }
            val elapsedDays = today.day.coerceAtMost(totalDaysInMonth)
            val remainingDays = (totalDaysInMonth - elapsedDays).coerceAtLeast(0)
            val progress = (elapsedDays.toFloat() / totalDaysInMonth.toFloat()).coerceIn(0f, 1f)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "امروز: ${JalaliCalendarHelper.getCurrentFullJalaliDateWithDay()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "${JalaliCalendarHelper.toPersianDigits(remainingDays)} روز باقی‌مانده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "روز ${JalaliCalendarHelper.toPersianDigits(elapsedDays)} از ${JalaliCalendarHelper.toPersianDigits(totalDaysInMonth)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "ماه ${JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrNull(today.month - 1) ?: ""} (${JalaliCalendarHelper.toPersianDigits(totalDaysInMonth)} روزه)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // Merged Total Balance & Bank Accounts Full-Width Auto-Sliding Pager
        item {
            val totalPages = remember(accounts) { 1 + accounts.size }
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
            val coroutineScope = rememberCoroutineScope()
            var isUserInteracting by remember { mutableStateOf(false) }

            LaunchedEffect(pagerState, totalPages, isUserInteracting) {
                if (totalPages > 1 && !isUserInteracting) {
                    while (true) {
                        delay(3000L)
                        if (!pagerState.isScrollInProgress && !isUserInteracting) {
                            val nextPage = (pagerState.currentPage + 1) % totalPages
                            pagerState.animateScrollToPage(nextPage)
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    isUserInteracting = event.changes.any { it.pressed }
                                }
                            }
                        },
                    pageSpacing = 12.dp
                ) { pageIndex ->
                    if (pageIndex == 0) {
                        // Total Assets & Monthly Summary Card (Full Width)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "موجودی کل دارایی‌ها",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = JalaliCalendarHelper.toPersianDigits("${accounts.size} حساب"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = CurrencyHelper.formatAmount(totalBalance, currencyUnit),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (totalBalance >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "درآمد",
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "درآمد ماه: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = CurrencyHelper.formatAmount(currentMonthIncome, currencyUnit),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "هزینه",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "هزینه ماه: ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = CurrencyHelper.formatAmount(currentMonthExpense, currencyUnit),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Individual Account Card (Full Width)
                        val acc = accounts[pageIndex - 1]
                        val accColor = CategoryIconHelper.parseColor(acc.colorHex)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = accColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(acc.iconName),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "حساب بانکی",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "موجودی حساب",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyHelper.formatAmount(acc.balance, currencyUnit),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (acc.accountNumber.isNotBlank()) {
                                        Text(
                                            text = "شماره حساب: ${JalaliCalendarHelper.toPersianDigits(acc.accountNumber)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = "حساب فعال",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }

                                    Text(
                                        text = "ورق بزنید ◄",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (totalPages > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalPages) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .height(6.dp)
                                    .width(if (isSelected) 20.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Integrated Transaction Registration Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Text(
                    //     text = "ثبت سریع تراکنش",
                    //     style = MaterialTheme.typography.titleMedium,
                    //     fontWeight = FontWeight.Bold,
                    //     color = MaterialTheme.colorScheme.onSurface
                    // )

                    // 3 Chip Buttons for Expense, Income, Transfer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFormType == "EXPENSE",
                            onClick = { selectedFormType = "EXPENSE" },
                            label = {
                                Text(
                                    "ثبت هزینه",
                                    fontWeight = if (selectedFormType == "EXPENSE") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseRedContainer,
                                selectedLabelColor = ExpenseRed,
                                selectedLeadingIconColor = ExpenseRed
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedFormType == "INCOME",
                            onClick = { selectedFormType = "INCOME" },
                            label = {
                                Text(
                                    "ثبت درآمد",
                                    fontWeight = if (selectedFormType == "INCOME") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeGreenContainer,
                                selectedLabelColor = IncomeGreen,
                                selectedLeadingIconColor = IncomeGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedFormType == "TRANSFER",
                            onClick = { selectedFormType = "TRANSFER" },
                            label = {
                                Text(
                                    "انتقال",
                                    fontWeight = if (selectedFormType == "TRANSFER") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Form content based on chip selection
                    if (selectedFormType == "EXPENSE" || selectedFormType == "INCOME") {
                        val isExpense = selectedFormType == "EXPENSE"
                        val filteredCategories = categories.filter { it.type == selectedFormType }

                        // Amount Input
                        OutlinedTextField(
                            value = amountValue,
                            onValueChange = { amountValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                            label = { Text("مبلغ (${currencyUnit.titleFa})") },
                            supportingText = if (amountValue.text.isNotBlank()) {
                                {
                                    val raw = CurrencyHelper.parseRawAmount(amountValue.text)
                                    val formatted = CurrencyHelper.formatAmount(
                                        if (currencyUnit == CurrencyUnit.RIAL) raw / 10.0 else raw.toDouble(),
                                        currencyUnit
                                    )
                                    Text(
                                        text = "معادل: $formatted",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Category Selector
                        CategoryTwoLevelSelector(
                            //title = "دسته‌بندی",
                            allCategories = filteredCategories,
                            selectedCategoryId = selectedCategoryId,
                            selectedSubcategoryId = selectedSubcategoryId,
                            onCategorySelected = { mainCatId, subCatId ->
                                selectedCategoryId = mainCatId
                                selectedSubcategoryId = subCatId
                            }
                        )

                        // Account Selector
                        AccountHorizontalSelector(
                            // title = "حساب بانکی / کارت",
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            currencyUnit = currencyUnit,
                            onAccountSelected = { selectedAccountId = it }
                        )

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
                                    label = { Text("تاریخ", maxLines = 1) },
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
                                label = { Text("توضیحات (اختیاری)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                val rawAmount = CurrencyHelper.parseRawAmount(amountValue.text)
                                if (rawAmount <= 0) {
                                    Toast.makeText(context, "لطفاً مبلغ معتبری وارد کنید", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedFormType != "TRANSFER" && selectedCategoryId == null) {
                                    Toast.makeText(context, "لطفاً دسته‌بندی را انتخاب کنید", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val amountInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmount / 10.0 else rawAmount.toDouble()
                                val subCatName = categories.find { it.id == selectedSubcategoryId }?.name
                                val mainCatName = categories.find { it.id == selectedCategoryId }?.name
                                val finalTitle = if (subCatName != null && mainCatName != null) "$mainCatName - $subCatName"
                                    else mainCatName ?: if (isExpense) "هزینه" else "درآمد"

                                val tx = TransactionEntity(
                                    type = selectedFormType,
                                    amount = amountInToman,
                                    accountId = selectedAccountId,
                                    categoryId = selectedCategoryId,
                                    subcategoryId = selectedSubcategoryId,
                                    jalaliDate = jalaliDateStr,
                                    title = finalTitle,
                                    note = note
                                )
                                onAddTransaction(tx)
                                Toast.makeText(
                                    context,
                                    if (isExpense) "هزینه با موفقیت ثبت شد" else "درآمد با موفقیت ثبت شد",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Reset form inputs
                                amountValue = TextFieldValue("")
                                note = ""
                                selectedCategoryId = null
                                selectedSubcategoryId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isExpense) ExpenseRed else IncomeGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpense) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isExpense) "ثبت هزینه" else "ثبت درآمد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (selectedFormType == "TRANSFER") {
                        // Amount Input
                        OutlinedTextField(
                            value = amountValue,
                            onValueChange = { amountValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                            label = { Text("مبلغ انتقال (${currencyUnit.titleFa})") },
                            supportingText = if (amountValue.text.isNotBlank()) {
                                {
                                    val raw = CurrencyHelper.parseRawAmount(amountValue.text)
                                    val formatted = CurrencyHelper.formatAmount(
                                        if (currencyUnit == CurrencyUnit.RIAL) raw / 10.0 else raw.toDouble(),
                                        currencyUnit
                                    )
                                    Text(
                                        text = "معادل: $formatted",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // From Account
                        AccountHorizontalSelector(
                            title = "از حساب مبدأ",
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            currencyUnit = currencyUnit,
                            onAccountSelected = { selectedAccountId = it }
                        )

                        // To Account
                        AccountHorizontalSelector(
                            title = "به حساب مقصد",
                            accounts = accounts.filter { it.id != selectedAccountId },
                            selectedAccountId = selectedTargetAccountId,
                            currencyUnit = currencyUnit,
                            onAccountSelected = { selectedTargetAccountId = it }
                        )

                        // Transfer Fee
                        OutlinedTextField(
                            value = transferFeeValue,
                            onValueChange = { transferFeeValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                            label = { Text("کارمزد انتقال (${currencyUnit.titleFa} - اختیاری)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

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
                                    label = { Text("تاریخ", maxLines = 1) },
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
                                label = { Text("توضیحات (اختیاری)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Submit Transfer Button
                        Button(
                            onClick = {
                                val rawAmt = CurrencyHelper.parseRawAmount(amountValue.text)
                                if (rawAmt <= 0) {
                                    Toast.makeText(context, "لطفاً مبلغ معتبری وارد کنید", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedAccountId == selectedTargetAccountId) {
                                    Toast.makeText(context, "حساب مبدأ و مقصد نباید یکسان باشند", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val amtInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmt / 10.0 else rawAmt.toDouble()
                                val rawFee = CurrencyHelper.parseRawAmount(transferFeeValue.text)
                                val feeInToman = if (currencyUnit == CurrencyUnit.RIAL) rawFee / 10.0 else rawFee.toDouble()

                                onTransfer(selectedAccountId, selectedTargetAccountId, amtInToman, feeInToman, note)
                                Toast.makeText(context, "انتقال بین حساب‌ها با موفقیت انجام شد", Toast.LENGTH_SHORT).show()

                                // Reset form inputs
                                amountValue = TextFieldValue("")
                                transferFeeValue = TextFieldValue("")
                                note = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ثبت انتقال بین حساب‌ها",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
