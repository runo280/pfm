package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.ChequeEntity
import com.example.data.local.DebtEntity
import com.example.data.local.InstallmentEntity
import com.example.ui.components.AccountHorizontalSelector
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.FilterPreferences
import com.example.util.JalaliCalendarHelper

import com.example.data.local.InstallmentItemEntity
import kotlinx.coroutines.flow.Flow

@Composable
fun InstallmentsChequesScreen(
    installments: List<InstallmentEntity>,
    cheques: List<ChequeEntity>,
    debts: List<DebtEntity> = emptyList(),
    accounts: List<AccountEntity>,
    currencyUnit: CurrencyUnit,
    onAddInstallment: (InstallmentEntity, List<Double>?) -> Unit,
    onUpdateInstallment: (InstallmentEntity) -> Unit,
    onDeleteInstallment: (InstallmentEntity) -> Unit,
    onPayInstallment: (InstallmentEntity, Long) -> Unit,
    onUnpayInstallment: ((InstallmentEntity) -> Unit)? = null,
    onGetInstallmentItems: ((Long) -> Flow<List<InstallmentItemEntity>>)? = null,
    onUpdateInstallmentItem: ((InstallmentItemEntity) -> Unit)? = null,
    onPaySpecificInstallmentItem: ((InstallmentEntity, InstallmentItemEntity, Long) -> Unit)? = null,
    onUnpaySpecificInstallmentItem: ((InstallmentEntity, InstallmentItemEntity) -> Unit)? = null,
    onAddCheque: (ChequeEntity) -> Unit,
    onUpdateCheque: (ChequeEntity) -> Unit,
    onDeleteCheque: (ChequeEntity) -> Unit,
    onMarkChequePassed: (ChequeEntity, Long) -> Unit,
    onUnpassCheque: ((ChequeEntity) -> Unit)? = null,
    onAddDebt: ((DebtEntity) -> Unit)? = null,
    onUpdateDebt: ((DebtEntity) -> Unit)? = null,
    onDeleteDebt: ((DebtEntity) -> Unit)? = null,
    onSettleDebt: ((DebtEntity, Double, Long?) -> Unit)? = null
) {
    val context = LocalContext.current
    val filterPrefs = remember { FilterPreferences(context) }

    var selectedTab by remember { mutableIntStateOf(filterPrefs.instSelectedTab) } // 0 = Installments, 1 = Cheques

    var showAddInstallmentDialog by remember { mutableStateOf(false) }
    var installmentToEdit by remember { mutableStateOf<InstallmentEntity?>(null) }
    var installmentToPay by remember { mutableStateOf<InstallmentEntity?>(null) }
    var installmentForDetails by remember { mutableStateOf<InstallmentEntity?>(null) }

    var showAddChequeDialog by remember { mutableStateOf(false) }
    var chequeToEdit by remember { mutableStateOf<ChequeEntity?>(null) }
    var chequeToPass by remember { mutableStateOf<ChequeEntity?>(null) }

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }
    var debtToSettle by remember { mutableStateOf<DebtEntity?>(null) }
    var debtFilterTab by remember { mutableIntStateOf(filterPrefs.instDebtFilterTab) } // 0 = All, 1 = Receivable, 2 = Payable, 3 = Settled

    LaunchedEffect(selectedTab, debtFilterTab) {
        filterPrefs.instSelectedTab = selectedTab
        filterPrefs.instDebtFilterTab = debtFilterTab
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddInstallmentDialog = true
                        1 -> showAddChequeDialog = true
                        else -> showAddDebtDialog = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        when (selectedTab) {
                            0 -> "ثبت وام و قسط"
                            1 -> "ثبت چک جدید"
                            else -> "ثبت طلب یا بدهی"
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            DebtSummaryCard(
                installments = installments,
                cheques = cheques,
                debts = debts,
                currencyUnit = currencyUnit
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("اقساط (${JalaliCalendarHelper.toPersianDigits(installments.size)})") },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("چک‌ها (${JalaliCalendarHelper.toPersianDigits(cheques.size)})") },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("طلب/بدهی (${JalaliCalendarHelper.toPersianDigits(debts.size)})") },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Installments Tab
                if (installments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("هیچ وام یا قسطی ثبت نشده است.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(installments, key = { it.id }) { inst ->
                            val progress = if (inst.totalInstallments > 0) {
                                inst.paidInstallments.toFloat() / inst.totalInstallments.toFloat()
                            } else 0f

                            var showDeleteDialog by remember { mutableStateOf(false) }

                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("حذف قسط") },
                                    text = { Text("آیا از حذف وام '${inst.title}' اطمینان دارید؟") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            onDeleteInstallment(inst)
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

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { installmentToEdit = inst },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Payments,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = inst.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = { installmentToEdit = inst }) {
                                                Icon(Icons.Default.Edit, contentDescription = "ویرایش مشخصات اصلی وام", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { showDeleteDialog = true }) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("مبلغ هر قسط:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(
                                            text = CurrencyHelper.formatAmount(inst.monthlyPayment, currencyUnit),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("سررسید ماهانه:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(
                                            text = "روز ${JalaliCalendarHelper.toPersianDigits(inst.dueDay)} هر ماه",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (inst.status != "COMPLETED") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val today = JalaliCalendarHelper.getCurrentJalaliDate()
                                        val nextDueDate = JalaliCalendarHelper.getInstallmentNextDueDate(inst, today)
                                        val diffDays = JalaliCalendarHelper.daysBetween(today, nextDueDate)
                                        val remMsg = JalaliCalendarHelper.getDaysRemainingMessage(nextDueDate, today)
                                        val bannerColor = when {
                                            diffDays < 0 -> ExpenseRedContainer
                                            diffDays == 0L -> ExpenseRedContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        }
                                        val textColor = when {
                                            diffDays < 0 -> ExpenseRed
                                            diffDays == 0L -> ExpenseRed
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bannerColor)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = textColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("یادآوری قسط بعدی:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = remMsg,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Progress Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "پرداخت شده: ${JalaliCalendarHelper.toPersianDigits(inst.paidInstallments)} از ${JalaliCalendarHelper.toPersianDigits(inst.totalInstallments)} قسط",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${JalaliCalendarHelper.toPersianDigits((progress * 100).toInt())}٪",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (inst.status == "COMPLETED") IncomeGreen else MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { installmentForDetails = inst },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ریز اقساط", style = MaterialTheme.typography.labelMedium)
                                        }

                                        if (inst.paidInstallments > 0) {
                                            OutlinedButton(
                                                onClick = { onUnpayInstallment?.invoke(inst) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                                            ) {
                                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("لغو پرداخت", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        if (inst.status == "ACTIVE" && inst.paidInstallments < inst.totalInstallments) {
                                            Button(
                                                onClick = { installmentToPay = inst },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("پرداخت بعدی", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // Cheques Tab
                if (cheques.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("هیچ چکی ثبت نشده است.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cheques, key = { it.id }) { chk ->
                            val isReceivable = chk.type == "RECEIVABLE"
                            val statusBg = when (chk.status) {
                                "PASSED" -> IncomeGreenContainer
                                "BOUNCED" -> ExpenseRedContainer
                                else -> GoldContainer
                            }
                            val statusColor = when (chk.status) {
                                "PASSED" -> IncomeGreen
                                "BOUNCED" -> ExpenseRed
                                else -> GoldWarning
                            }
                            val statusTitle = when (chk.status) {
                                "PASSED" -> "پاس شده"
                                "BOUNCED" -> "برگشتی"
                                else -> "پاس نشده"
                            }

                            var showDeleteDialog by remember { mutableStateOf(false) }
                            var showUnpassDialog by remember { mutableStateOf(false) }

                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("حذف چک") },
                                    text = { Text("آیا از حذف چک شماره '${chk.chequeNumber}' اطمینان دارید؟") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            onDeleteCheque(chk)
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

                            if (showUnpassDialog) {
                                AlertDialog(
                                    onDismissRequest = { showUnpassDialog = false },
                                    title = { Text("بازگرداندن چک به وضعیت پاس نشده") },
                                    text = { Text("آیا از تغییر وضعیت چک شماره '${chk.chequeNumber}' به حالت پاس نشده اطمینان دارید؟") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (onUnpassCheque != null) {
                                                    onUnpassCheque(chk)
                                                } else {
                                                    onUpdateCheque(chk.copy(status = "PENDING", accountId = null))
                                                }
                                                showUnpassDialog = false
                                            }
                                        ) {
                                            Text("تأیید و بازگرداندن")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showUnpassDialog = false }) {
                                            Text("انصراف")
                                        }
                                    }
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { chequeToEdit = chk },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isReceivable) IncomeGreenContainer else ExpenseRedContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isReceivable) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = if (isReceivable) IncomeGreen else ExpenseRed
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "چک ${if (isReceivable) "دریافتی" else "پرداختی"} - ${chk.bankName}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "شماره: ${JalaliCalendarHelper.toPersianDigits(chk.chequeNumber)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(statusTitle, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("طرف حساب:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(chk.payeeOrDrawer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("تاریخ سررسید:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(JalaliCalendarHelper.toPersianDigits(chk.dueDateJalali), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("مبلغ چک:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        Text(
                                            text = CurrencyHelper.formatAmount(chk.amount, currencyUnit),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isReceivable) IncomeGreen else ExpenseRed
                                        )
                                    }

                                    if (chk.status == "PENDING") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val today = JalaliCalendarHelper.getCurrentJalaliDate()
                                        val targetDate = JalaliCalendarHelper.parseJalaliDate(chk.dueDateJalali)
                                        val diffDays = if (targetDate != null) JalaliCalendarHelper.daysBetween(today, targetDate) else 0L
                                        val remMsg = JalaliCalendarHelper.getDaysRemainingMessage(chk.dueDateJalali, today)
                                        val bannerColor = when {
                                            diffDays < 0 -> ExpenseRedContainer
                                            diffDays == 0L -> ExpenseRedContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        }
                                        val textColor = when {
                                            diffDays < 0 -> ExpenseRed
                                            diffDays == 0L -> ExpenseRed
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bannerColor)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = textColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("یادآوری سررسید:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = remMsg,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showDeleteDialog = true }) {
                                            Text("حذف", color = ExpenseRed)
                                        }
                                        if (chk.status == "PASSED" || chk.status == "BOUNCED") {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            OutlinedButton(
                                                onClick = { showUnpassDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("بازگرداندن به پاس نشده", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        if (chk.status == "PENDING") {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = { chequeToPass = chk }) {
                                                Text("ثبت پاس شدن چک")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Debts & Receivables Tab
                val totalReceivable = debts.filter { it.type == "RECEIVABLE" && it.status == "PENDING" }.sumOf { it.amount - it.paidAmount }
                val totalPayable = debts.filter { it.type == "PAYABLE" && it.status == "PENDING" }.sumOf { it.amount - it.paidAmount }
                val netDebt = totalReceivable - totalPayable

                val filteredDebts = debts.filter { debt ->
                    when (debtFilterTab) {
                        1 -> debt.type == "RECEIVABLE" && debt.status == "PENDING"
                        2 -> debt.type == "PAYABLE" && debt.status == "PENDING"
                        3 -> debt.status == "SETTLED"
                        else -> true
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Summary Cards Header
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("طلبکاری شما", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                                Text(
                                    CurrencyHelper.formatAmount(totalReceivable, currencyUnit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            Divider(modifier = Modifier.height(36.dp).width(1.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("بدهکاری شما", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                                Text(
                                    CurrencyHelper.formatAmount(totalPayable, currencyUnit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Divider(modifier = Modifier.height(36.dp).width(1.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("خالص مطالبات", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(
                                    CurrencyHelper.formatAmount(netDebt, currencyUnit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netDebt >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }

                    // Filter Chips Row
                    ScrollableTabRow(
                        selectedTabIndex = debtFilterTab,
                        edgePadding = 0.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Tab(selected = debtFilterTab == 0, onClick = { debtFilterTab = 0 }, text = { Text("همه (${JalaliCalendarHelper.toPersianDigits(debts.size)})") })
                        Tab(selected = debtFilterTab == 1, onClick = { debtFilterTab = 1 }, text = { Text("طلب‌ها") })
                        Tab(selected = debtFilterTab == 2, onClick = { debtFilterTab = 2 }, text = { Text("بدهی‌ها") })
                        Tab(selected = debtFilterTab == 3, onClick = { debtFilterTab = 3 }, text = { Text("تسویه‌شده") })
                    }

                    if (filteredDebts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هیچ مورد طلب یا بدهی وجود ندارد.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredDebts, key = { it.id }) { debt ->
                                var showDeleteConfirm by remember { mutableStateOf(false) }
                                var showResetConfirm by remember { mutableStateOf(false) }
                                val isReceivable = debt.type == "RECEIVABLE"
                                val remaining = (debt.amount - debt.paidAmount).coerceAtLeast(0.0)
                                val progress = if (debt.amount > 0) (debt.paidAmount / debt.amount).toFloat().coerceIn(0f, 1f) else 1f

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (debt.status == "SETTLED") MaterialTheme.colorScheme.surface
                                        else if (isReceivable) IncomeGreenContainer.copy(alpha = 0.3f)
                                        else ExpenseRedContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isReceivable) IncomeGreen else ExpenseRed,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isReceivable) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                            contentDescription = null,
                                                            tint = Color.White
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        debt.personName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        if (isReceivable) "طلبکار هستید (از ${debt.personName})" else "بدهکار هستید (به ${debt.personName})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isReceivable) IncomeGreen else ExpenseRed
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (debt.status == "SETTLED") {
                                                    Surface(
                                                        color = IncomeGreenContainer,
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    ) {
                                                        Text(
                                                            "تسویه کامل",
                                                            color = IncomeGreen,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                IconButton(onClick = { debtToEdit = debt }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("مبلغ کل:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            Text(
                                                CurrencyHelper.formatAmount(debt.amount, currencyUnit),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (debt.paidAmount > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("پرداخت/تسویه شده:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                Text(
                                                    CurrencyHelper.formatAmount(debt.paidAmount, currencyUnit),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = IncomeGreen
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("باقیمانده:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                Text(
                                                    CurrencyHelper.formatAmount(remaining, currencyUnit),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isReceivable) IncomeGreen else ExpenseRed
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                                color = if (isReceivable) IncomeGreen else ExpenseRed
                                            )
                                        }

                                        if (debt.dueDateJalali.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "تاریخ قرار/سررسید: ${JalaliCalendarHelper.toPersianDigits(debt.dueDateJalali)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }

                                        if (debt.note.isNotBlank()) {
                                            Text(
                                                "توضیحات: ${debt.note}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { showDeleteConfirm = true }) {
                                                Text("حذف", color = ExpenseRed)
                                            }

                                            if (debt.paidAmount > 0 || debt.status == "SETTLED") {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                OutlinedButton(
                                                    onClick = { showResetConfirm = true },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text("بازگرداندن به عدم پرداخت", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }

                                            if (debt.status == "PENDING") {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { debtToSettle = debt },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isReceivable) IncomeGreen else MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Text(if (isReceivable) "ثبت دریافت طلب" else "ثبت پرداخت بدهی")
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showDeleteConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirm = false },
                                        title = { Text("حذف طلب/بدهی") },
                                        text = { Text("آیا از حذف حساب با ${debt.personName} اطمینان دارید؟") },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    onDeleteDebt?.invoke(debt)
                                                    showDeleteConfirm = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                                            ) {
                                                Text("حذف")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteConfirm = false }) {
                                                Text("انصراف")
                                            }
                                        }
                                    )
                                }

                                if (showResetConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showResetConfirm = false },
                                        title = { Text("بازگرداندن به حالت عدم پرداخت") },
                                        text = { Text("آیا از لغو پرداخت‌ها و بازگرداندن وضعیت بدهی/طلب با ${debt.personName} به حالت عدم پرداخت اطمینان دارید؟ تمامی مبالغ پرداختی صفر خواهند شد.") },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    onUpdateDebt?.invoke(debt.copy(paidAmount = 0.0, status = "PENDING"))
                                                    showResetConfirm = false
                                                }
                                            ) {
                                                Text("تأیید و بازگرداندن")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showResetConfirm = false }) {
                                                Text("انصراف")
                                            }
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

    // Pay Installment Dialog (Account selector)
    if (installmentForDetails != null && onGetInstallmentItems != null) {
        InstallmentItemsDialog(
            installment = installmentForDetails!!,
            accounts = accounts,
            currencyUnit = currencyUnit,
            onDismiss = { installmentForDetails = null },
            onGetInstallmentItems = onGetInstallmentItems,
            onUpdateInstallmentItem = { item -> onUpdateInstallmentItem?.invoke(item) },
            onPaySpecificInstallmentItem = { inst, item, accountId ->
                onPaySpecificInstallmentItem?.invoke(inst, item, accountId)
            },
            onUnpaySpecificInstallmentItem = { inst, item ->
                onUnpaySpecificInstallmentItem?.invoke(inst, item)
            }
        )
    }

    if (installmentToPay != null) {
        SelectAccountActionDialog(
            title = "پرداخت قسط: ${installmentToPay!!.title}",
            amountInToman = installmentToPay!!.monthlyPayment,
            accounts = accounts,
            currencyUnit = currencyUnit,
            onDismiss = { installmentToPay = null },
            onConfirm = { accountId ->
                onPayInstallment(installmentToPay!!, accountId)
                installmentToPay = null
            }
        )
    }

    // Pass Cheque Dialog (Account selector)
    if (chequeToPass != null) {
        SelectAccountActionDialog(
            title = "وصول/پاس شدن چک شماره ${chequeToPass!!.chequeNumber}",
            amountInToman = chequeToPass!!.amount,
            accounts = accounts,
            currencyUnit = currencyUnit,
            onDismiss = { chequeToPass = null },
            onConfirm = { accountId ->
                onMarkChequePassed(chequeToPass!!, accountId)
                chequeToPass = null
            }
        )
    }

    if (showAddInstallmentDialog) {
        AddEditInstallmentDialog(
            installment = null,
            currencyUnit = currencyUnit,
            onDismiss = { showAddInstallmentDialog = false },
            onSave = { inst, customItems ->
                onAddInstallment(inst, customItems)
                showAddInstallmentDialog = false
            }
        )
    }

    if (installmentToEdit != null) {
        AddEditInstallmentDialog(
            installment = installmentToEdit,
            currencyUnit = currencyUnit,
            onDismiss = { installmentToEdit = null },
            onSave = { updatedInst, _ ->
                onUpdateInstallment(updatedInst)
                installmentToEdit = null
            }
        )
    }

    if (showAddChequeDialog) {
        AddEditChequeDialog(
            cheque = null,
            currencyUnit = currencyUnit,
            onDismiss = { showAddChequeDialog = false },
            onSave = { chk ->
                onAddCheque(chk)
                showAddChequeDialog = false
            }
        )
    }

    if (chequeToEdit != null) {
        AddEditChequeDialog(
            cheque = chequeToEdit,
            currencyUnit = currencyUnit,
            onDismiss = { chequeToEdit = null },
            onSave = { updatedChk ->
                onUpdateCheque(updatedChk)
                chequeToEdit = null
            }
        )
    }

    if (showAddDebtDialog) {
        AddEditDebtDialog(
            debt = null,
            currencyUnit = currencyUnit,
            onDismiss = { showAddDebtDialog = false },
            onSave = { newDebt ->
                onAddDebt?.invoke(newDebt)
                showAddDebtDialog = false
            }
        )
    }

    if (debtToEdit != null) {
        AddEditDebtDialog(
            debt = debtToEdit,
            currencyUnit = currencyUnit,
            onDismiss = { debtToEdit = null },
            onSave = { updatedDebt ->
                onUpdateDebt?.invoke(updatedDebt)
                debtToEdit = null
            }
        )
    }

    if (debtToSettle != null) {
        SettleDebtDialog(
            debt = debtToSettle!!,
            accounts = accounts,
            currencyUnit = currencyUnit,
            onDismiss = { debtToSettle = null },
            onConfirm = { settleAmt, accId ->
                onSettleDebt?.invoke(debtToSettle!!, settleAmt, accId)
                debtToSettle = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAccountActionDialog(
    title: String,
    amountInToman: Double,
    accounts: List<AccountEntity>,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("مبلغ: ${CurrencyHelper.formatAmount(amountInToman, currencyUnit)}", fontWeight = FontWeight.Bold)

                AccountHorizontalSelector(
                    title = "لطفاً حساب بانکی مورد نظر جهت کسر/واریز وجه را انتخاب کنید:",
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    currencyUnit = currencyUnit,
                    onAccountSelected = { selectedAccountId = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedAccountId) }) {
                Text("تأیید و انجام")
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
fun AddEditInstallmentDialog(
    installment: InstallmentEntity?,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (InstallmentEntity, List<Double>?) -> Unit
) {
    var title by remember { mutableStateOf(installment?.title ?: "") }

    // Calculation Mode: 1 = Manual/Equal, 2 = Interest %, 3 = Annual Fee / Commission %
    var calcMode by remember { mutableIntStateOf(1) }

    // Principal loan input
    var principalValue by remember {
        mutableStateOf(
            if (installment != null) {
                val amt = if (currencyUnit == CurrencyUnit.RIAL) installment.totalAmount * 10 else installment.totalAmount
                TextFieldValue(CurrencyHelper.formatLiveAmountInput(amt.toLong().toString()))
            } else TextFieldValue("")
        )
    }

    var totalInstallmentsStr by remember { mutableStateOf(installment?.totalInstallments?.toString() ?: "12") }
    var interestRateStr by remember { mutableStateOf("18") } // Mode 2
    var commissionRateStr by remember { mutableStateOf("4") } // Mode 3

    var monthlyPaymentValue by remember {
        mutableStateOf(
            if (installment != null) {
                val amt = if (currencyUnit == CurrencyUnit.RIAL) installment.monthlyPayment * 10 else installment.monthlyPayment
                TextFieldValue(CurrencyHelper.formatLiveAmountInput(amt.toLong().toString()))
            } else TextFieldValue("")
        )
    }

    // First installment start date
    var startJalaliDate by remember {
        mutableStateOf(
            if (!installment?.startJalaliDate.isNullOrBlank()) installment!!.startJalaliDate
            else JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()
        )
    }

    var dueDayStr by remember {
        mutableStateOf(
            installment?.dueDay?.toString()
                ?: (JalaliCalendarHelper.parseJalaliDate(startJalaliDate)?.day?.toString() ?: "1")
        )
    }

    // Paid option mode: "COUNT" or "DATE"
    var paidOptionMode by remember { mutableStateOf("COUNT") }
    var paidInstallmentsStr by remember { mutableStateOf(installment?.paidInstallments?.toString() ?: "0") }
    var paidUpToDateJalali by remember { mutableStateOf(JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()) }

    var note by remember { mutableStateOf(installment?.note ?: "") }

    // Date Pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showPaidUpToDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = startJalaliDate,
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { date ->
                startJalaliDate = date
                JalaliCalendarHelper.parseJalaliDate(date)?.let { parsed ->
                    dueDayStr = parsed.day.toString()
                }
                showStartDatePicker = false
            }
        )
    }

    if (showPaidUpToDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = paidUpToDateJalali,
            onDismissRequest = { showPaidUpToDatePicker = false },
            onDateSelected = { date ->
                paidUpToDateJalali = date
                showPaidUpToDatePicker = false
            }
        )
    }

    // Dynamic Calculations
    val rawPrincipal = CurrencyHelper.parseRawAmount(principalValue.text)
    val principalInToman = if (currencyUnit == CurrencyUnit.RIAL) rawPrincipal / 10.0 else rawPrincipal
    val totalCount = (totalInstallmentsStr.toIntOrNull() ?: 12).coerceAtLeast(1)

    // Calculate paid installments automatically if in "DATE" mode
    LaunchedEffect(paidOptionMode, startJalaliDate, paidUpToDateJalali, totalCount, dueDayStr) {
        if (paidOptionMode == "DATE") {
            val startJ = JalaliCalendarHelper.parseJalaliDate(startJalaliDate)
            val upToJ = JalaliCalendarHelper.parseJalaliDate(paidUpToDateJalali)
            if (startJ != null && upToJ != null) {
                val dueDay = (dueDayStr.toIntOrNull() ?: startJ.day).coerceIn(1, 31)
                val upToJdn = JalaliCalendarHelper.jalaliToJdn(upToJ.year, upToJ.month, upToJ.day)
                var count = 0
                for (i in 0 until totalCount) {
                    val dueDate = JalaliCalendarHelper.calculateInstallmentDueDate(startJ, dueDay, i)
                    val dueJdn = JalaliCalendarHelper.jalaliToJdn(dueDate.year, dueDate.month, dueDate.day)
                    if (dueJdn <= upToJdn) {
                        count++
                    } else {
                        break
                    }
                }
                paidInstallmentsStr = count.toString()
            }
        }
    }

    val computed = remember(calcMode, principalInToman, totalCount, interestRateStr, commissionRateStr) {
        when (calcMode) {
            1 -> {
                val monthlyInToman = if (totalCount > 0) principalInToman / totalCount else 0.0
                Triple(principalInToman, monthlyInToman, null as List<Double>?)
            }
            2 -> {
                val rate = interestRateStr.toDoubleOrNull() ?: 0.0
                val totalInterestInToman = principalInToman * (rate / 100.0) * (totalCount / 12.0)
                val totalRepaymentInToman = principalInToman + totalInterestInToman
                val monthlyInToman = if (totalCount > 0) totalRepaymentInToman / totalCount else 0.0
                Triple(totalRepaymentInToman, monthlyInToman, null as List<Double>?)
            }
            3, 4 -> {
                val rate = commissionRateStr.toDoubleOrNull() ?: 4.0
                val baseMonthlyPrincipal = principalInToman / totalCount
                val items = MutableList(totalCount) { 0.0 }
                var totalFeeInToman = 0.0

                for (i in 0 until totalCount) {
                    val yearIndex = i / 12
                    if (i % 12 == 0) {
                        val remainingPrincipal = (principalInToman - (yearIndex * 12 * baseMonthlyPrincipal)).coerceAtLeast(0.0)
                        val yearlyFee = remainingPrincipal * (rate / 100.0)
                        items[i] = baseMonthlyPrincipal + yearlyFee
                        totalFeeInToman += yearlyFee
                    } else {
                        items[i] = baseMonthlyPrincipal
                    }
                }
                val totalRepaymentInToman = principalInToman + totalFeeInToman
                val avgMonthlyInToman = if (totalCount > 0) totalRepaymentInToman / totalCount else 0.0
                Triple(totalRepaymentInToman, avgMonthlyInToman, items.toList())
            }
            else -> Triple(principalInToman, 0.0, null)
        }
    }

    val finalTotalAmountInToman = computed.first
    val finalMonthlyPaymentInToman = computed.second
    val customItemAmounts = computed.third

    // Keep monthly payment string updated for non-manual modes
    LaunchedEffect(calcMode, principalInToman, totalCount, interestRateStr, commissionRateStr) {
        if (calcMode == 2 || calcMode == 3 || calcMode == 4) {
            val displayMonthly = if (currencyUnit == CurrencyUnit.RIAL) finalMonthlyPaymentInToman * 10 else finalMonthlyPaymentInToman
            monthlyPaymentValue = TextFieldValue(CurrencyHelper.formatLiveAmountInput(displayMonthly.toLong().toString()))
        } else if (calcMode == 1 && (monthlyPaymentValue.text.isBlank() || installment == null)) {
            val monthlyInToman = if (totalCount > 0) principalInToman / totalCount else 0.0
            val displayMonthly = if (currencyUnit == CurrencyUnit.RIAL) monthlyInToman * 10 else monthlyInToman
            monthlyPaymentValue = TextFieldValue(CurrencyHelper.formatLiveAmountInput(displayMonthly.toLong().toString()))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (installment == null) "ثبت وام و اقساط جدید" else "ویرایش اطلاعات وام") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Calculation Mode Tabs
                Text("روش محاسبه اقساط:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = calcMode == 1,
                        onClick = { calcMode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(0, 4)
                    ) {
                        Text("مساوی", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SegmentedButton(
                        selected = calcMode == 2,
                        onClick = { calcMode = 2 },
                        shape = SegmentedButtonDefaults.itemShape(1, 4)
                    ) {
                        Text("درصد سود", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SegmentedButton(
                        selected = calcMode == 3,
                        onClick = { calcMode = 3 },
                        shape = SegmentedButtonDefaults.itemShape(2, 4)
                    ) {
                        Text("کارمزد سالانه", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SegmentedButton(
                        selected = calcMode == 4,
                        onClick = {
                            calcMode = 4
                            if (title.isBlank() || title == "وام") {
                                title = "وام ازدواج"
                            }
                            commissionRateStr = "4"
                            totalInstallmentsStr = "120"
                            if (principalValue.text.isBlank()) {
                                val defaultAmt = if (currencyUnit == CurrencyUnit.RIAL) 3000000000L else 300000000L
                                principalValue = TextFieldValue(CurrencyHelper.formatLiveAmountInput(defaultAmt.toString()))
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(3, 4)
                    ) {
                        Text("وام ازدواج", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (calcMode == 4) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                "تنظیم سقف قانونی وام ازدواج",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val amt300Toman = if (currencyUnit == CurrencyUnit.RIAL) 3000000000L else 300000000L
                            val amt350Toman = if (currencyUnit == CurrencyUnit.RIAL) 3500000000L else 350000000L

                            FilterChip(
                                selected = principalInToman == 300000000.0,
                                onClick = {
                                    principalValue = TextFieldValue(CurrencyHelper.formatLiveAmountInput(amt300Toman.toString()))
                                },
                                label = { Text("۳۰۰ میلیون تومان", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = principalInToman == 350000000.0,
                                onClick = {
                                    principalValue = TextFieldValue(CurrencyHelper.formatLiveAmountInput(amt350Toman.toString()))
                                },
                                label = { Text("۳۵۰ میلیون (ایثارگران/سن)", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان وام / قسط") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = principalValue,
                    onValueChange = { principalValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ اصل وام (${currencyUnit.titleFa})") },
                    supportingText = if (principalValue.text.isNotBlank()) {
                        {
                            val formatted = CurrencyHelper.formatAmount(principalInToman, currencyUnit)
                            Text(text = "معادل: $formatted", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (calcMode == 2) {
                    OutlinedTextField(
                        value = interestRateStr,
                        onValueChange = { interestRateStr = it },
                        label = { Text("درصد سود سالانه (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else if (calcMode == 3) {
                    OutlinedTextField(
                        value = commissionRateStr,
                        onValueChange = { commissionRateStr = it },
                        label = { Text("درصد کارمزد سالانه (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = totalInstallmentsStr,
                    onValueChange = { totalInstallmentsStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("تعداد کل اقساط (ماه)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (calcMode == 1) {
                    OutlinedTextField(
                        value = monthlyPaymentValue,
                        onValueChange = { monthlyPaymentValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                        label = { Text("مبلغ هر قسط ماهانه (${currencyUnit.titleFa})") },
                        supportingText = if (monthlyPaymentValue.text.isNotBlank()) {
                            {
                                val raw = CurrencyHelper.parseRawAmount(monthlyPaymentValue.text)
                                val formatted = CurrencyHelper.formatAmount(if (currencyUnit == CurrencyUnit.RIAL) raw / 10.0 else raw, currencyUnit)
                                Text(text = "معادل: $formatted", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else if (calcMode == 4) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("خلاصه محاسبات وام ازدواج (۱۰ ساله - ۴٪ کارمزد):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("مجموع کل بازپرداخت: ${CurrencyHelper.formatAmount(finalTotalAmountInToman, currencyUnit)}", style = MaterialTheme.typography.bodySmall)
                            Text("مجموع کل کارمزد ۱۰ ساله: ${CurrencyHelper.formatAmount(finalTotalAmountInToman - principalInToman, currencyUnit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)

                            val firstMonthPayment = customItemAmounts?.getOrNull(0) ?: 0.0
                            val baseMonthly = if (totalCount > 0) principalInToman / totalCount else 0.0
                            Text("قسط ماه اول (شامل کارمزد سال اول): ${CurrencyHelper.formatAmount(firstMonthPayment, currencyUnit)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("قسط سایر ماه‌های عادی: ${CurrencyHelper.formatAmount(baseMonthly, currencyUnit)}", style = MaterialTheme.typography.bodySmall)

                            Text("💡 طبق قانون وام ازدواج در ایران، کارمزد ۴٪ سالانه در اولین ماه هر سال (ماه‌های ۱، ۱۳، ۲۵، ۳۷، ۴۹، ۶۱، ۷۳، ۸۵، ۹۷ و ۱۰۹) بر اساس مانده اصل وام کسر می‌گردد.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("خلاصه محاسبات وام:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("مجموع کل بازپرداخت: ${CurrencyHelper.formatAmount(finalTotalAmountInToman, currencyUnit)}", style = MaterialTheme.typography.bodySmall)
                            Text("مبلغ قسط ماهانه (میانگین): ${CurrencyHelper.formatAmount(finalMonthlyPaymentInToman, currencyUnit)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            if (calcMode == 3 && !customItemAmounts.isNullOrEmpty()) {
                                Text("نکته: کارمزد سالانه در اول هر سال کسر می‌گردد.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Date Selection Section
                Text("تاریخ و جدول زمانی اقساط:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = startJalaliDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تاریخ موعد اولین قسط") },
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "انتخاب تاریخ")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true }
                )

                OutlinedTextField(
                    value = dueDayStr,
                    onValueChange = { dueDayStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("روز سررسید در ماه (۱ تا ۳۱)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Options for Past Installments
                Text("ثبت اقساط قبلی پرداخت‌شده:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paidOptionMode == "COUNT",
                        onClick = { paidOptionMode = "COUNT" },
                        label = { Text("بر اساس تعداد") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paidOptionMode == "DATE",
                        onClick = { paidOptionMode = "DATE" },
                        label = { Text("پرداخت‌شده تا تاریخ") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (paidOptionMode == "COUNT") {
                    OutlinedTextField(
                        value = paidInstallmentsStr,
                        onValueChange = { paidInstallmentsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("تعداد اقساط پرداخت‌شده") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = paidUpToDateJalali,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("پرداخت‌شده تا این تاریخ") },
                            trailingIcon = {
                                IconButton(onClick = { showPaidUpToDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "انتخاب تاریخ")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaidUpToDatePicker = true }
                        )
                        Text(
                            text = "تعداد اقساط محاسبه‌شده: ${JalaliCalendarHelper.toPersianDigits(paidInstallmentsStr)} قسط",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paidCount = (paidInstallmentsStr.toIntOrNull() ?: 0).coerceIn(0, totalCount)
                    val dueDay = (dueDayStr.toIntOrNull() ?: 1).coerceIn(1, 31)

                    val rawMonthly = CurrencyHelper.parseRawAmount(monthlyPaymentValue.text)
                    val monthlyInToman = if (calcMode == 1) {
                        if (currencyUnit == CurrencyUnit.RIAL) rawMonthly / 10.0 else rawMonthly
                    } else finalMonthlyPaymentInToman

                    val newInst = InstallmentEntity(
                        id = installment?.id ?: 0,
                        title = title,
                        totalAmount = finalTotalAmountInToman,
                        monthlyPayment = monthlyInToman,
                        totalInstallments = totalCount,
                        paidInstallments = paidCount,
                        dueDay = dueDay,
                        note = note,
                        status = if (paidCount >= totalCount) "COMPLETED" else "ACTIVE",
                        startJalaliDate = startJalaliDate
                    )
                    onSave(newInst, customItemAmounts)
                },
                enabled = title.isNotBlank() && (principalValue.text.isNotBlank() || monthlyPaymentValue.text.isNotBlank())
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

@Composable
fun AddEditChequeDialog(
    cheque: ChequeEntity?,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (ChequeEntity) -> Unit
) {
    var chequeNumber by remember { mutableStateOf(cheque?.chequeNumber ?: "") }
    var bankName by remember { mutableStateOf(cheque?.bankName ?: "") }
    var type by remember { mutableStateOf(cheque?.type ?: "PAYABLE") } // PAYABLE / RECEIVABLE
    var amountValue by remember {
        mutableStateOf(
            if (cheque != null) {
                val amt = if (currencyUnit == CurrencyUnit.RIAL) cheque.amount * 10 else cheque.amount
                TextFieldValue(CurrencyHelper.formatLiveAmountInput(amt.toLong().toString()))
            } else TextFieldValue("")
        )
    }
    var dueDateJalali by remember { mutableStateOf(cheque?.dueDateJalali ?: JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()) }
    var payeeOrDrawer by remember { mutableStateOf(cheque?.payeeOrDrawer ?: "") }
    var note by remember { mutableStateOf(cheque?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = dueDateJalali,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                dueDateJalali = date
                showDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cheque == null) "ثبت چک جدید" else "ویرایش چک") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == "PAYABLE",
                        onClick = { type = "PAYABLE" },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) {
                        Text("چک پرداختی (صادره)")
                    }
                    SegmentedButton(
                        selected = type == "RECEIVABLE",
                        onClick = { type = "RECEIVABLE" },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) {
                        Text("چک دریافتی")
                    }
                }

                OutlinedTextField(
                    value = chequeNumber,
                    onValueChange = { chequeNumber = it },
                    label = { Text("شماره چک") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("نام بانک") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountValue,
                    onValueChange = { amountValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ چک (${currencyUnit.titleFa})") },
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

                OutlinedTextField(
                    value = payeeOrDrawer,
                    onValueChange = { payeeOrDrawer = it },
                    label = { Text(if (type == "PAYABLE") "نام دریافت‌کننده (در وجه)" else "نام صادرکننده چک") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تاریخ سررسید: ${JalaliCalendarHelper.toPersianDigits(dueDateJalali)}")
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawAmt = CurrencyHelper.parseRawAmount(amountValue.text)
                    val amountInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmt / 10.0 else rawAmt

                    val newChk = ChequeEntity(
                        id = cheque?.id ?: 0,
                        chequeNumber = chequeNumber,
                        bankName = bankName,
                        amount = amountInToman,
                        type = type,
                        dueDateJalali = dueDateJalali,
                        payeeOrDrawer = payeeOrDrawer,
                        status = cheque?.status ?: "PENDING",
                        note = note
                    )
                    onSave(newChk)
                },
                enabled = chequeNumber.isNotBlank() && amountValue.text.isNotBlank()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentItemsDialog(
    installment: InstallmentEntity,
    accounts: List<AccountEntity>,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onGetInstallmentItems: (Long) -> Flow<List<InstallmentItemEntity>>,
    onUpdateInstallmentItem: (InstallmentItemEntity) -> Unit,
    onPaySpecificInstallmentItem: (InstallmentEntity, InstallmentItemEntity, Long) -> Unit,
    onUnpaySpecificInstallmentItem: ((InstallmentEntity, InstallmentItemEntity) -> Unit)? = null
) {
    val itemsFlow = remember(installment.id) { onGetInstallmentItems(installment.id) }
    val items by itemsFlow.collectAsState(initial = emptyList())

    var itemToEdit by remember { mutableStateOf<InstallmentItemEntity?>(null) }
    var itemToPay by remember { mutableStateOf<InstallmentItemEntity?>(null) }

    if (itemToEdit != null) {
        EditInstallmentItemDialog(
            item = itemToEdit!!,
            currencyUnit = currencyUnit,
            onDismiss = { itemToEdit = null },
            onSave = { updated ->
                onUpdateInstallmentItem(updated)
                itemToEdit = null
            }
        )
    }

    if (itemToPay != null) {
        SelectAccountActionDialog(
            title = "پرداخت قسط شماره ${itemToPay!!.installmentNumber}",
            amountInToman = itemToPay!!.amount,
            accounts = accounts,
            currencyUnit = currencyUnit,
            onDismiss = { itemToPay = null },
            onConfirm = { accountId ->
                onPaySpecificInstallmentItem(installment, itemToPay!!, accountId)
                itemToPay = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "ریز اقساط: ${installment.title}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "شماره وام: #${JalaliCalendarHelper.toPersianDigits(installment.id)} | مجموع: ${JalaliCalendarHelper.toPersianDigits(installment.totalInstallments)} قسط | ${CurrencyHelper.formatAmount(installment.monthlyPayment, currencyUnit)}/ماه",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        text = {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val dueDate = JalaliCalendarHelper.getInstallmentItemDueDate(installment, item.installmentNumber)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isPaid) IncomeGreenContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "قسط شماره ${JalaliCalendarHelper.toPersianDigits(item.installmentNumber)} (وام #${JalaliCalendarHelper.toPersianDigits(installment.id)})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (item.isPaid) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = IncomeGreen.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "پرداخت شده",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = IncomeGreen,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "تاریخ سررسید: ${dueDate.toReadablePersianString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (item.isPaid && !item.paidDateJalali.isNullOrBlank()) {
                                        Text(
                                            text = "تاریخ پرداخت: ${JalaliCalendarHelper.toPersianDigits(item.paidDateJalali)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IncomeGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = CurrencyHelper.formatAmount(item.amount, currencyUnit),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (item.note.isNotBlank()) {
                                        Text(
                                            text = item.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { itemToEdit = item },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ویرایش", style = MaterialTheme.typography.labelMedium)
                                    }

                                    if (item.isPaid) {
                                        OutlinedButton(
                                            onClick = { onUnpaySpecificInstallmentItem?.invoke(installment, item) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                                        ) {
                                            Icon(
                                                Icons.Default.Undo,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("لغو پرداخت", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        Button(
                                            onClick = { itemToPay = item },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("پرداخت", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

@Composable
fun EditInstallmentItemDialog(
    item: InstallmentItemEntity,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (InstallmentItemEntity) -> Unit
) {
    val initialDisplayAmt = if (currencyUnit == CurrencyUnit.RIAL) (item.amount * 10).toLong().toString() else item.amount.toLong().toString()
    var amountValue by remember { mutableStateOf(TextFieldValue(CurrencyHelper.formatLiveAmountInput(initialDisplayAmt))) }
    var note by remember { mutableStateOf(item.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش قسط شماره ${JalaliCalendarHelper.toPersianDigits(item.installmentNumber)}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "می‌توانید مبلغ این قسط خاص (مثلاً کارمزد قسط اول) یا یادداشت آن را تغییر دهید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = amountValue,
                    onValueChange = { amountValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ قسط (${currencyUnit.titleFa})") },
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

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت / بابت کارمزد (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawAmt = CurrencyHelper.parseRawAmount(amountValue.text)
                    val amountInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmt / 10.0 else rawAmt
                    onSave(item.copy(amount = amountInToman, note = note))
                },
                enabled = amountValue.text.isNotBlank() && CurrencyHelper.parseRawAmount(amountValue.text) > 0
            ) {
                Text("ثبت تغییرات")
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
fun AddEditDebtDialog(
    debt: DebtEntity?,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (DebtEntity) -> Unit
) {
    var personName by remember { mutableStateOf(debt?.personName ?: "") }
    var type by remember { mutableStateOf(debt?.type ?: "RECEIVABLE") }
    var amountValue by remember {
        mutableStateOf(
            if (debt != null) {
                val amt = if (currencyUnit == CurrencyUnit.RIAL) debt.amount * 10 else debt.amount
                TextFieldValue(CurrencyHelper.formatLiveAmountInput(amt.toLong().toString()))
            } else TextFieldValue("")
        )
    }
    var dueDateJalali by remember { mutableStateOf(debt?.dueDateJalali ?: "") }
    var note by remember { mutableStateOf(debt?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt == null) "ثبت طلب یا بدهی جدید" else "ویرایش طلب / بدهی") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("نام طرف حساب (شخص/شرکت)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("نوع حساب:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "RECEIVABLE",
                        onClick = { type = "RECEIVABLE" },
                        label = { Text("طلبکارم (طلب از او)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "PAYABLE",
                        onClick = { type = "PAYABLE" },
                        label = { Text("بدهکارم (بدهی به او)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountValue,
                    onValueChange = { amountValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ کل (${currencyUnit.titleFa})") },
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

                OutlinedTextField(
                    value = if (dueDateJalali.isNotBlank()) JalaliCalendarHelper.toPersianDigits(dueDateJalali) else "انتخاب تاریخ (اختیاری)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تاریخ موعد / قرار تسویه") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات / یادداشت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawAmt = CurrencyHelper.parseRawAmount(amountValue.text)
                    val amountInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmt / 10.0 else rawAmt
                    val entity = (debt ?: DebtEntity(
                        personName = personName,
                        type = type,
                        amount = amountInToman,
                        createdDateJalali = JalaliCalendarHelper.getCurrentJalaliDate().toFormattedString()
                    )).copy(
                        personName = personName,
                        type = type,
                        amount = amountInToman,
                        dueDateJalali = dueDateJalali,
                        note = note
                    )
                    onSave(entity)
                },
                enabled = personName.isNotBlank() && amountValue.text.isNotBlank() && CurrencyHelper.parseRawAmount(amountValue.text) > 0
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDateStr = dueDateJalali,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date ->
                dueDateJalali = date
                showDatePicker = false
            }
        )
    }
}

@Composable
fun SettleDebtDialog(
    debt: DebtEntity,
    accounts: List<AccountEntity>,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onConfirm: (settleAmount: Double, accountId: Long?) -> Unit
) {
    val remainingToman = (debt.amount - debt.paidAmount).coerceAtLeast(0.0)
    var settleAmtValue by remember {
        val displayAmt = if (currencyUnit == CurrencyUnit.RIAL) remainingToman * 10 else remainingToman
        mutableStateOf(TextFieldValue(CurrencyHelper.formatLiveAmountInput(displayAmt.toLong().toString())))
    }
    var affectAccountBalance by remember { mutableStateOf(true) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: -1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (debt.type == "RECEIVABLE") "ثبت دریافت طلب از ${debt.personName}" else "ثبت پرداخت بدهی به ${debt.personName}")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "مبلغ باقیمانده: ${CurrencyHelper.formatAmount(remainingToman, currencyUnit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = settleAmtValue,
                    onValueChange = { settleAmtValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ تسویه (${currencyUnit.titleFa})") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { affectAccountBalance = !affectAccountBalance }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = affectAccountBalance,
                        onCheckedChange = { affectAccountBalance = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (debt.type == "RECEIVABLE") "واریز به حساب و ثبت تراکنش درآمد" else "کسر از حساب و ثبت تراکنش هزینه",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (affectAccountBalance && accounts.isNotEmpty()) {
                    AccountHorizontalSelector(
                        title = if (debt.type == "RECEIVABLE") "واریز به حساب:" else "پرداخت از حساب:",
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        currencyUnit = currencyUnit,
                        onAccountSelected = { selectedAccountId = it }
                    )
                } else if (!affectAccountBalance) {
                    Text(
                        text = "💡 در صورت غیرفعال بودن این گزینه، تسویه فقط در بخش طلب/بدهی ثبت می‌شود و تغییری در موجودی حساب‌ها و تراکنش‌ها داده نخواهد شد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val raw = CurrencyHelper.parseRawAmount(settleAmtValue.text)
                    val settleToman = if (currencyUnit == CurrencyUnit.RIAL) raw / 10.0 else raw
                    val targetAccId = if (affectAccountBalance && selectedAccountId > 0) selectedAccountId else null
                    onConfirm(settleToman, targetAccId)
                },
                enabled = settleAmtValue.text.isNotBlank() && CurrencyHelper.parseRawAmount(settleAmtValue.text) > 0
            ) {
                Text("تأیید و ثبت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun DebtSummaryCard(
    installments: List<InstallmentEntity>,
    cheques: List<ChequeEntity>,
    debts: List<DebtEntity>,
    currencyUnit: CurrencyUnit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filterPrefs = remember { FilterPreferences(context) }

    var filterMode by remember { mutableStateOf(filterPrefs.instFilterMode) } // "MONTHLY", "YEARLY", "ALL"
    val today = remember { JalaliCalendarHelper.getCurrentJalaliDate() }

    val savedYear = filterPrefs.instYear
    val savedMonth = filterPrefs.instMonth
    var selectedYear by remember { mutableIntStateOf(if (savedYear != -1) savedYear else today.year) }
    var selectedMonth by remember { mutableIntStateOf(if (savedMonth != -1) savedMonth else today.month) }
    var isExpanded by remember { mutableStateOf(filterPrefs.instSummaryExpanded) }

    LaunchedEffect(filterMode, selectedYear, selectedMonth, isExpanded) {
        filterPrefs.instFilterMode = filterMode
        filterPrefs.instYear = selectedYear
        filterPrefs.instMonth = selectedMonth
        filterPrefs.instSummaryExpanded = isExpanded
    }

    val hasActiveSummaryFilter = filterMode != "MONTHLY" || selectedYear != today.year || selectedMonth != today.month

    val todayJdn = remember(today) { JalaliCalendarHelper.jalaliToJdn(today.year, today.month, today.day) }

    val remainingColor = Color(0xFF1976D2) // Blue - بدهی بازه انتخابی
    val paidColor = IncomeGreen           // Green - بدهی پرداخت شده
    val overdueColor = ExpenseRed         // Red - بدهی معوق

    val (remainingDebt, unpaidDebt, overdueDebt) = remember(
        installments, cheques, debts, filterMode, selectedYear, selectedMonth, todayJdn
    ) {
        val maxDaysInSelectedMonth = if (selectedMonth <= 6) 31 else if (selectedMonth <= 11) 30 else if (JalaliCalendarHelper.isJalaliLeapYear(selectedYear)) 30 else 29
        val periodEndJdn = when (filterMode) {
            "MONTHLY" -> JalaliCalendarHelper.jalaliToJdn(selectedYear, selectedMonth, maxDaysInSelectedMonth)
            "YEARLY" -> JalaliCalendarHelper.jalaliToJdn(selectedYear, 12, if (JalaliCalendarHelper.isJalaliLeapYear(selectedYear)) 30 else 29)
            else -> JalaliCalendarHelper.jalaliToJdn(today.year + 50, 12, 29)
        }

        // 1. بدهی بازه انتخابی (پس از کسر میزان پرداخت شده)
        val periodUnpaid = when (filterMode) {
            "MONTHLY" -> {
                val instUnpaid = installments.filter { it.status == "ACTIVE" && it.paidInstallments < it.totalInstallments }.sumOf { inst ->
                    val nextDueDate = JalaliCalendarHelper.getInstallmentNextDueDate(inst, today)
                    if (nextDueDate.year == selectedYear && nextDueDate.month == selectedMonth) inst.monthlyPayment else 0.0
                }
                val chkUnpaid = cheques.filter { chk ->
                    chk.type == "PAYABLE" && chk.status == "PENDING" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(chk.dueDateJalali)
                        p != null && p.year == selectedYear && p.month == selectedMonth
                    }
                }.sumOf { it.amount }
                val dbtUnpaid = debts.filter { d ->
                    d.type == "PAYABLE" && d.status == "PENDING" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(d.dueDateJalali)
                        p != null && p.year == selectedYear && p.month == selectedMonth
                    }
                }.sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }
                instUnpaid + chkUnpaid + dbtUnpaid
            }
            "YEARLY" -> {
                val instUnpaid = installments.filter { it.status == "ACTIVE" && it.paidInstallments < it.totalInstallments }.sumOf { inst ->
                    val remCount = (inst.totalInstallments - inst.paidInstallments).coerceAtMost(12)
                    remCount * inst.monthlyPayment
                }
                val chkUnpaid = cheques.filter { chk ->
                    chk.type == "PAYABLE" && chk.status == "PENDING" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(chk.dueDateJalali)
                        p != null && p.year == selectedYear
                    }
                }.sumOf { it.amount }
                val dbtUnpaid = debts.filter { d ->
                    d.type == "PAYABLE" && d.status == "PENDING" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(d.dueDateJalali)
                        p != null && p.year == selectedYear
                    }
                }.sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }
                instUnpaid + chkUnpaid + dbtUnpaid
            }
            else -> {
                val instUnpaid = installments.filter { it.status == "ACTIVE" }.sumOf {
                    (it.totalInstallments - it.paidInstallments).coerceAtLeast(0) * it.monthlyPayment
                }
                val chkUnpaid = cheques.filter { it.type == "PAYABLE" && it.status == "PENDING" }.sumOf { it.amount }
                val dbtUnpaid = debts.filter { it.type == "PAYABLE" && it.status == "PENDING" }.sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }
                instUnpaid + chkUnpaid + dbtUnpaid
            }
        }

        // 2. بدهی پرداخت شده بازه انتخابی
        val periodPaid = when (filterMode) {
            "MONTHLY" -> {
                val instPaid = installments.sumOf { inst ->
                    val nextDueDate = JalaliCalendarHelper.getInstallmentNextDueDate(inst, today)
                    if (inst.paidInstallments > 0 && (nextDueDate.year > selectedYear || (nextDueDate.year == selectedYear && nextDueDate.month > selectedMonth))) {
                        inst.monthlyPayment
                    } else 0.0
                }
                val chkPaid = cheques.filter { chk ->
                    chk.type == "PAYABLE" && chk.status == "PASSED" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(chk.dueDateJalali)
                        p != null && p.year == selectedYear && p.month == selectedMonth
                    }
                }.sumOf { it.amount }
                val dbtPaid = debts.filter { d ->
                    d.type == "PAYABLE" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(d.dueDateJalali)
                        p != null && p.year == selectedYear && p.month == selectedMonth
                    }
                }.sumOf { if (it.status == "PAID") it.amount else it.paidAmount }
                instPaid + chkPaid + dbtPaid
            }
            "YEARLY" -> {
                val instPaid = installments.sumOf { inst ->
                    inst.paidInstallments.coerceAtMost(12) * inst.monthlyPayment
                }
                val chkPaid = cheques.filter { chk ->
                    chk.type == "PAYABLE" && chk.status == "PASSED" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(chk.dueDateJalali)
                        p != null && p.year == selectedYear
                    }
                }.sumOf { it.amount }
                val dbtPaid = debts.filter { d ->
                    d.type == "PAYABLE" && run {
                        val p = JalaliCalendarHelper.parseJalaliDate(d.dueDateJalali)
                        p != null && p.year == selectedYear
                    }
                }.sumOf { if (it.status == "PAID") it.amount else it.paidAmount }
                instPaid + chkPaid + dbtPaid
            }
            else -> {
                val instPaid = installments.sumOf { it.paidInstallments * it.monthlyPayment }
                val chkPaid = cheques.filter { it.type == "PAYABLE" && it.status == "PASSED" }.sumOf { it.amount }
                val dbtPaid = debts.filter { it.type == "PAYABLE" }.sumOf { if (it.status == "PAID") it.amount else it.paidAmount }
                instPaid + chkPaid + dbtPaid
            }
        }

        // 3. بدهی معوق از اول تا انتهای بازه انتخابی
        val overdueCutoffJdn = minOf(todayJdn - 1, periodEndJdn)

        val chkOverdue = cheques.filter { chk ->
            chk.type == "PAYABLE" && chk.status == "PENDING" && run {
                val p = JalaliCalendarHelper.parseJalaliDate(chk.dueDateJalali)
                p != null && run {
                    val jdn = JalaliCalendarHelper.jalaliToJdn(p.year, p.month, p.day)
                    jdn <= overdueCutoffJdn
                }
            }
        }.sumOf { it.amount }

        val dbtOverdue = debts.filter { d ->
            d.type == "PAYABLE" && d.status == "PENDING" && run {
                val p = JalaliCalendarHelper.parseJalaliDate(d.dueDateJalali)
                p != null && run {
                    val jdn = JalaliCalendarHelper.jalaliToJdn(p.year, p.month, p.day)
                    jdn <= overdueCutoffJdn
                }
            }
        }.sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }

        val instOverdue = installments.filter { it.status == "ACTIVE" && it.paidInstallments < it.totalInstallments }.sumOf { inst ->
            val nextDueDate = JalaliCalendarHelper.getInstallmentNextDueDate(inst, today)
            val nextDueJdn = JalaliCalendarHelper.jalaliToJdn(nextDueDate.year, nextDueDate.month, nextDueDate.day)
            if (nextDueJdn <= overdueCutoffJdn) {
                inst.monthlyPayment
            } else 0.0
        }

        val periodOverdue = chkOverdue + dbtOverdue + instOverdue

        Triple(periodUnpaid, periodPaid, periodOverdue)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "خلاصه وضعیت تعهدات و بدهی‌ها",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasActiveSummaryFilter) {
                        TextButton(
                            onClick = {
                                filterMode = "MONTHLY"
                                selectedYear = today.year
                                selectedMonth = today.month
                                filterPrefs.resetInstFilters()
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = ExpenseRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بازنشانی", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                        }
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "پنهان‌سازی" else "نمایش"
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == "MONTHLY",
                    onClick = { filterMode = "MONTHLY" },
                    label = { Text("ماهیانه") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = filterMode == "YEARLY",
                    onClick = { filterMode = "YEARLY" },
                    label = { Text("سالیانه") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = filterMode == "ALL",
                    onClick = { filterMode = "ALL" },
                    label = { Text("کلی") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month / Year Navigator Row
            if (filterMode == "MONTHLY") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (selectedMonth == 1) {
                                selectedMonth = 12
                                selectedYear--
                            } else {
                                selectedMonth--
                            }
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "ماه قبل")
                        }

                        Text(
                            text = "${JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(selectedMonth - 1) { "" }} ${JalaliCalendarHelper.toPersianDigits(selectedYear)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear++
                            } else {
                                selectedMonth++
                            }
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "ماه بعد")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else if (filterMode == "YEARLY") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "سال قبل")
                        }

                        Text(
                            text = "سال ${JalaliCalendarHelper.toPersianDigits(selectedYear)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { selectedYear++ }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "سال بعد")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Donut Chart + Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DebtDonutChart(
                        remainingDebt = remainingDebt,
                        unpaidDebt = unpaidDebt,
                        overdueDebt = overdueDebt,
                        modifier = Modifier.fillMaxSize(),
                        remainingColor = remainingColor,
                        unpaidColor = paidColor,
                        overdueColor = overdueColor
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Item 1: بدهی بازه انتخابی
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(remainingColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدهی بازه (مانده)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text(
                            text = CurrencyHelper.formatAmount(remainingDebt, currencyUnit),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = remainingColor
                        )
                    }

                    // Item 2: بدهی پرداخت شده بازه
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(paidColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدهی پرداخت شده", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text(
                            text = CurrencyHelper.formatAmount(unpaidDebt, currencyUnit),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = paidColor
                        )
                    }

                    // Item 3: بدهی معوق (تا پایان بازه)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(overdueColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("معوق (تا پایان بازه)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text(
                            text = CurrencyHelper.formatAmount(overdueDebt, currencyUnit),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = overdueColor
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun DebtDonutChart(
    remainingDebt: Double,
    unpaidDebt: Double,
    overdueDebt: Double,
    modifier: Modifier = Modifier,
    remainingColor: Color = Color(0xFF1976D2),
    unpaidColor: Color = Color(0xFFF57C00),
    overdueColor: Color = Color(0xFFD32F2F)
) {
    val total = remainingDebt + unpaidDebt + overdueDebt

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val diameter = minOf(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        if (total <= 0.0) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        } else {
            val remAngle = ((remainingDebt / total) * 360f).toFloat()
            val unpAngle = ((unpaidDebt / total) * 360f).toFloat()
            val ovrAngle = ((overdueDebt / total) * 360f).toFloat()

            var startAngle = -90f

            if (remAngle > 0f) {
                drawArc(
                    color = remainingColor,
                    startAngle = startAngle,
                    sweepAngle = remAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                startAngle += remAngle
            }

            if (unpAngle > 0f) {
                drawArc(
                    color = unpaidColor,
                    startAngle = startAngle,
                    sweepAngle = unpAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                startAngle += unpAngle
            }

            if (ovrAngle > 0f) {
                drawArc(
                    color = overdueColor,
                    startAngle = startAngle,
                    sweepAngle = ovrAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
