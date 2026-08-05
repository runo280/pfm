package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.ui.components.AccountHorizontalSelector
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.ExpenseRed
import com.example.util.CurrencyHelper
import com.example.util.CurrencyUnit
import com.example.util.JalaliCalendarHelper

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    currencyUnit: CurrencyUnit,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onTransfer: (Long, Long, Double, Double, String) -> Unit,
    onSetDefaultAccount: (Long) -> Unit = {}
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showTransferDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "انتقال بین حساب‌ها")
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExtendedFloatingActionButton(
                    onClick = { showAddAccountDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("افزودن حساب جدید") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "مدیریت حساب‌ها و کارت‌های بانکی",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(accounts, key = { it.id }) { acc ->
                    val cardColor = CategoryIconHelper.parseColor(acc.colorHex)
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("حذف حساب") },
                            text = { Text("آیا از حذف حساب '${acc.name}' اطمینان دارید؟") },
                            confirmButton = {
                                TextButton(onClick = {
                                    onDeleteAccount(acc)
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
                            .clickable { accountToEdit = acc },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(acc.iconName),
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            if (acc.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color.White.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "پیش‌فرض",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (acc.accountNumber.isNotBlank()) {
                                            Text(
                                                text = JalaliCalendarHelper.toPersianDigits(acc.accountNumber),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(onClick = { onSetDefaultAccount(acc.id) }) {
                                        Icon(
                                            imageVector = if (acc.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "تنظیم به عنوان حساب پیش‌فرض",
                                            tint = if (acc.isDefault) Color(0xFFFFD700) else Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                    IconButton(onClick = { showDeleteDialog = true }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "موجودی حساب:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = CurrencyHelper.formatAmount(acc.balance, currencyUnit),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddEditAccountDialog(
            account = null,
            currencyUnit = currencyUnit,
            onDismiss = { showAddAccountDialog = false },
            onSave = { acc ->
                onAddAccount(acc)
                showAddAccountDialog = false
            }
        )
    }

    if (accountToEdit != null) {
        AddEditAccountDialog(
            account = accountToEdit,
            currencyUnit = currencyUnit,
            onDismiss = { accountToEdit = null },
            onSave = { updatedAcc ->
                onUpdateAccount(updatedAcc)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    account: AccountEntity?,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var balanceValue by remember {
        mutableStateOf(
            if (account != null) {
                val amt = if (currencyUnit == CurrencyUnit.RIAL) account.balance * 10 else account.balance
                val formatted = CurrencyHelper.formatLiveAmountInput(amt.toLong().toString())
                TextFieldValue(formatted)
            } else TextFieldValue("")
        )
    }
    var selectedColor by remember { mutableStateOf(account?.colorHex ?: "#1E40AF") }
    var selectedIcon by remember { mutableStateOf(account?.iconName ?: "CreditCard") }

    val colorOptions = listOf("#1E40AF", "#059669", "#7C3AED", "#D97706", "#DC2626", "#4F46E5", "#0284C7")
    val iconOptions = listOf("CreditCard", "AccountBalanceWallet", "Savings", "Payments")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "افزودن حساب جدید" else "ویرایش حساب") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام حساب یا بانک") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("شماره کارت یا حساب (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balanceValue,
                    onValueChange = { balanceValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("موجودی اولیه (${currencyUnit.titleFa})") },
                    supportingText = if (balanceValue.text.isNotBlank()) {
                        {
                            val raw = CurrencyHelper.parseRawAmount(balanceValue.text)
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

                Text("انتخاب رنگ کارت:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { hex ->
                        val color = CategoryIconHelper.parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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

                Text("انتخاب آیکون:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    iconOptions.forEach { iconName ->
                        val icon = CategoryIconHelper.getIcon(iconName)
                        FilterChip(
                            selected = selectedIcon == iconName,
                            onClick = { selectedIcon = iconName },
                            label = { Icon(icon, contentDescription = null) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawAmt = CurrencyHelper.parseRawAmount(balanceValue.text)
                    val balanceInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmt / 10.0 else rawAmt

                    val newAcc = AccountEntity(
                        id = account?.id ?: 0,
                        name = name.ifBlank { "حساب بانکی" },
                        accountNumber = accountNumber,
                        balance = balanceInToman,
                        colorHex = selectedColor,
                        iconName = selectedIcon,
                        isDefault = account?.isDefault ?: false
                    )
                    onSave(newAcc)
                },
                enabled = name.isNotBlank()
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
fun TransferDialog(
    accounts: List<AccountEntity>,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onTransfer: (Long, Long, Double, Double, String) -> Unit
) {
    var fromAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var toAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1L) }
    var amountValue by remember { mutableStateOf(TextFieldValue("")) }
    var feeValue by remember { mutableStateOf(TextFieldValue("")) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتقال وجه بین حساب‌ها") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // From Account
                AccountHorizontalSelector(
                    title = "از حساب مبدأ",
                    accounts = accounts,
                    selectedAccountId = fromAccountId,
                    currencyUnit = currencyUnit,
                    onAccountSelected = { fromAccountId = it }
                )

                // To Account
                AccountHorizontalSelector(
                    title = "به حساب مقصد",
                    accounts = accounts.filter { it.id != fromAccountId },
                    selectedAccountId = toAccountId,
                    currencyUnit = currencyUnit,
                    onAccountSelected = { toAccountId = it }
                )

                OutlinedTextField(
                    value = amountValue,
                    onValueChange = { amountValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("مبلغ انتقال (${currencyUnit.titleFa})") },
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
                    value = feeValue,
                    onValueChange = { feeValue = CurrencyHelper.formatAmountTextFieldValue(it) },
                    label = { Text("کارمزد انتقال (${currencyUnit.titleFa})") },
                    supportingText = if (feeValue.text.isNotBlank() && CurrencyHelper.parseRawAmount(feeValue.text) > 0) {
                        {
                            val raw = CurrencyHelper.parseRawAmount(feeValue.text)
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
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawAmt = CurrencyHelper.parseRawAmount(amountValue.text)
                    val amtInToman = if (currencyUnit == CurrencyUnit.RIAL) rawAmt / 10.0 else rawAmt
                    val rawFee = CurrencyHelper.parseRawAmount(feeValue.text)
                    val feeInToman = if (currencyUnit == CurrencyUnit.RIAL) rawFee / 10.0 else rawFee

                    onTransfer(fromAccountId, toAccountId, amtInToman, feeInToman, note)
                },
                enabled = amountValue.text.isNotBlank() && CurrencyHelper.parseRawAmount(amountValue.text) > 0 && fromAccountId != toAccountId
            ) {
                Text("انجام انتقال")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
