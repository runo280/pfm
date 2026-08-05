package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
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

    val years = (1395..1415).toList()
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
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "انتخاب تاریخ شمسی",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day Selector
                        FieldStepperWithDropdown(
                            label = "روز",
                            displayText = selectedDay.toString(),
                            options = (1..maxDays).map { it.toString() },
                            selectedIndex = selectedDay - 1,
                            onSelectIndex = { selectedDay = it + 1 },
                            onIncrement = { if (selectedDay < maxDays) selectedDay++ },
                            onDecrement = { if (selectedDay > 1) selectedDay-- },
                            canIncrement = selectedDay < maxDays,
                            canDecrement = selectedDay > 1,
                            compact = false,
                            modifier = Modifier.weight(1f)
                        )

                        // Month Selector
                        FieldStepperWithDropdown(
                            label = "ماه",
                            displayText = months.getOrNull(selectedMonth - 1) ?: "",
                            options = months,
                            selectedIndex = selectedMonth - 1,
                            onSelectIndex = { selectedMonth = it + 1 },
                            onIncrement = { if (selectedMonth < 12) selectedMonth++ },
                            onDecrement = { if (selectedMonth > 1) selectedMonth-- },
                            canIncrement = selectedMonth < 12,
                            canDecrement = selectedMonth > 1,
                            compact = false,
                            modifier = Modifier.weight(1.3f)
                        )

                        // Year Selector
                        FieldStepperWithDropdown(
                            label = "سال",
                            displayText = selectedYear.toString(),
                            options = years.map { it.toString() },
                            selectedIndex = (selectedYear - 1395).coerceIn(0, years.size - 1),
                            onSelectIndex = { selectedYear = years[it] },
                            onIncrement = { if (selectedYear < 1415) selectedYear++ },
                            onDecrement = { if (selectedYear > 1395) selectedYear-- },
                            canIncrement = selectedYear < 1415,
                            canDecrement = selectedYear > 1395,
                            compact = false,
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) {
                            Text("انصراف")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val result = JalaliDate(selectedYear, selectedMonth, selectedDay).toFormattedString()
                                onDateSelected(result)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تأیید")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InlineJalaliDatePicker(
    dateStr: String,
    onDateChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialDate = remember(dateStr) {
        JalaliCalendarHelper.parseJalaliDate(dateStr) ?: JalaliCalendarHelper.getCurrentJalaliDate()
    }

    var selectedYear by remember(dateStr) { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember(dateStr) { mutableIntStateOf(initialDate.month) }
    var selectedDay by remember(dateStr) { mutableIntStateOf(initialDate.day) }

    val months = JalaliCalendarHelper.PERSIAN_MONTH_NAMES
    val years = (1395..1415).toList()
    val maxDays = when (selectedMonth) {
        in 1..6 -> 31
        in 7..11 -> 30
        else -> 29
    }

    if (selectedDay > maxDays) {
        selectedDay = maxDays
    }

    fun notifyChanged(y: Int, m: Int, d: Int) {
        val validDay = d.coerceAtMost(
            when (m) {
                in 1..6 -> 31
                in 7..11 -> 30
                else -> 29
            }
        )
        onDateChanged(JalaliDate(y, m, validDay).toFormattedString())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تاریخ تراکنش:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val previewDate = JalaliDate(selectedYear, selectedMonth, selectedDay)
                Text(
                    text = previewDate.toReadablePersianString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Field
                FieldStepperWithDropdown(
                    label = "روز",
                    displayText = selectedDay.toString(),
                    options = (1..maxDays).map { it.toString() },
                    selectedIndex = selectedDay - 1,
                    onSelectIndex = { idx ->
                        selectedDay = idx + 1
                        notifyChanged(selectedYear, selectedMonth, selectedDay)
                    },
                    onIncrement = {
                        if (selectedDay < maxDays) {
                            selectedDay++
                            notifyChanged(selectedYear, selectedMonth, selectedDay)
                        }
                    },
                    onDecrement = {
                        if (selectedDay > 1) {
                            selectedDay--
                            notifyChanged(selectedYear, selectedMonth, selectedDay)
                        }
                    },
                    canIncrement = selectedDay < maxDays,
                    canDecrement = selectedDay > 1,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )

                // Month Field
                FieldStepperWithDropdown(
                    label = "ماه",
                    displayText = months.getOrNull(selectedMonth - 1) ?: "",
                    options = months,
                    selectedIndex = selectedMonth - 1,
                    onSelectIndex = { idx ->
                        selectedMonth = idx + 1
                        notifyChanged(selectedYear, selectedMonth, selectedDay)
                    },
                    onIncrement = {
                        if (selectedMonth < 12) {
                            selectedMonth++
                            notifyChanged(selectedYear, selectedMonth, selectedDay)
                        }
                    },
                    onDecrement = {
                        if (selectedMonth > 1) {
                            selectedMonth--
                            notifyChanged(selectedYear, selectedMonth, selectedDay)
                        }
                    },
                    canIncrement = selectedMonth < 12,
                    canDecrement = selectedMonth > 1,
                    compact = true,
                    modifier = Modifier.weight(1.3f)
                )

                // Year Field
                FieldStepperWithDropdown(
                    label = "سال",
                    displayText = selectedYear.toString(),
                    options = years.map { it.toString() },
                    selectedIndex = (selectedYear - 1395).coerceIn(0, years.size - 1),
                    onSelectIndex = { idx ->
                        selectedYear = years[idx]
                        notifyChanged(selectedYear, selectedMonth, selectedDay)
                    },
                    onIncrement = {
                        if (selectedYear < 1415) {
                            selectedYear++
                            notifyChanged(selectedYear, selectedMonth, selectedDay)
                        }
                    },
                    onDecrement = {
                        if (selectedYear > 1395) {
                            selectedYear--
                            notifyChanged(selectedYear, selectedMonth, selectedDay)
                        }
                    },
                    canIncrement = selectedYear < 1415,
                    canDecrement = selectedYear > 1395,
                    compact = true,
                    modifier = Modifier.weight(1.1f)
                )
            }
        }
    }
}

@Composable
fun FieldStepperWithDropdown(
    label: String,
    displayText: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canIncrement: Boolean,
    canDecrement: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (!compact) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Up Arrow (+)
        IconButton(
            onClick = onIncrement,
            enabled = canIncrement,
            modifier = Modifier.size(if (compact) 28.dp else 34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "افزایش $label",
                tint = if (canIncrement) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
            )
        }

        // Middle Value Box with Dropdown
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(
                        horizontal = if (compact) 6.dp else 10.dp,
                        vertical = if (compact) 4.dp else 6.dp
                    )
                ) {
                    Text(
                        text = JalaliCalendarHelper.toPersianDigits(displayText),
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = JalaliCalendarHelper.toPersianDigits(opt),
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                                color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelectIndex(index)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Down Arrow (-)
        IconButton(
            onClick = onDecrement,
            enabled = canDecrement,
            modifier = Modifier.size(if (compact) 28.dp else 34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "کاهش $label",
                tint = if (canDecrement) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
            )
        }
    }
}
