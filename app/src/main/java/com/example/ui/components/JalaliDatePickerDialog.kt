package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.util.JalaliCalendarHelper
import com.example.util.JalaliDate

@Composable
fun JalaliDatePickerDialog(
    initialDateStr: String,
    onDismissRequest: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialDate = remember(initialDateStr) {
        JalaliCalendarHelper.parseJalaliDate(initialDateStr) ?: JalaliCalendarHelper.getCurrentJalaliDate()
    }

    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.month) }
    var selectedDay by remember { mutableIntStateOf(initialDate.day) }

    val years = (1398..1415).toList()
    val months = JalaliCalendarHelper.PERSIAN_MONTH_NAMES
    val maxDays = when (selectedMonth) {
        in 1..6 -> 31
        in 7..11 -> 30
        else -> 29
    }

    if (selectedDay > maxDays) {
        selectedDay = maxDays
    }

    Dialog(onDismissRequest = onDismissRequest) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "انتخاب تاریخ شمسی",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Display selected date preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val previewDate = JalaliDate(selectedYear, selectedMonth, selectedDay)
                    Text(
                        text = previewDate.toReadablePersianString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Selector
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("روز", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPickerRow(
                            value = selectedDay,
                            min = 1,
                            max = maxDays,
                            onValueChange = { selectedDay = it }
                        )
                    }

                    // Month Selector
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("ماه", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        DropdownMonthPicker(
                            selectedMonth = selectedMonth,
                            months = months,
                            onMonthSelected = { selectedMonth = it }
                        )
                    }

                    // Year Selector
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("سال", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPickerRow(
                            value = selectedYear,
                            min = 1398,
                            max = 1415,
                            onValueChange = { selectedYear = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val result = JalaliDate(selectedYear, selectedMonth, selectedDay).toFormattedString()
                        onDateSelected(result)
                    }) {
                        Text("تأیید")
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun NumberPickerRow(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { if (value > min) onValueChange(value - 1) },
            enabled = value > min
        ) {
            Text("-", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = JalaliCalendarHelper.toPersianDigits(value),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        IconButton(
            onClick = { if (value < max) onValueChange(value + 1) },
            enabled = value < max
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun DropdownMonthPicker(
    selectedMonth: Int,
    months: List<String>,
    onMonthSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { expanded = true }
        ) {
            Text(
                text = months.getOrNull(selectedMonth - 1) ?: "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onMonthSelected(index + 1)
                        expanded = false
                    }
                )
            }
        }
    }
}
