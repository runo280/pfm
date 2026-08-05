package com.example.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class CurrencyUnit(val titleFa: String) {
    TOMAN("تومان"),
    RIAL("ریال")
}

object CurrencyHelper {

    fun parseRawDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(ch)
            } else {
                val idx = persianDigits.indexOf(ch)
                if (idx != -1) {
                    sb.append(englishDigits[idx])
                }
            }
        }
        return sb.toString()
    }

    fun parseRawAmount(input: String): Double {
        val clean = parseRawDigits(input)
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun formatLiveAmountInput(inputStr: String): String {
        val cleanDigits = parseRawDigits(inputStr)
        if (cleanDigits.isBlank()) return ""
        val rawLong = cleanDigits.toLongOrNull() ?: return ""
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedStr = formatter.format(rawLong)
        return JalaliCalendarHelper.toPersianDigits(formattedStr)
    }

    fun formatAmountTextFieldValue(newValue: TextFieldValue): TextFieldValue {
        val newText = newValue.text
        val rawDigits = parseRawDigits(newText)
        if (rawDigits.isBlank()) {
            return TextFieldValue(text = "", selection = TextRange(0))
        }

        val safeDigits = if (rawDigits.length > 15) rawDigits.substring(0, 15) else rawDigits
        val formattedText = formatLiveAmountInput(safeDigits)

        val rawDigitsBeforeCursor = parseRawDigits(newText.take(newValue.selection.end.coerceIn(0, newText.length))).length

        var cursorInFormatted = 0
        var digitCount = 0
        for (i in formattedText.indices) {
            val ch = formattedText[i]
            if (ch in '0'..'9' || ch in '۰'..'۹') {
                digitCount++
            }
            if (digitCount == rawDigitsBeforeCursor) {
                cursorInFormatted = i + 1
                break
            }
        }

        if (rawDigitsBeforeCursor == 0) cursorInFormatted = 0
        if (rawDigitsBeforeCursor >= safeDigits.length) cursorInFormatted = formattedText.length

        return TextFieldValue(
            text = formattedText,
            selection = TextRange(cursorInFormatted.coerceIn(0, formattedText.length))
        )
    }

    fun formatInputValue(inputStr: String, unit: CurrencyUnit? = null): String {
        val formatted = formatLiveAmountInput(inputStr)
        if (formatted.isBlank()) return ""
        return if (unit != null) "$formatted ${unit.titleFa}" else formatted
    }

    fun formatAmount(
        amountInToman: Double,
        unit: CurrencyUnit,
        includeUnit: Boolean = true,
        usePersianDigits: Boolean = true
    ): String {
        val convertedAmount = if (unit == CurrencyUnit.RIAL) {
            amountInToman * 10
        } else {
            amountInToman
        }

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedStr = formatter.format(convertedAmount.toLong())

        val resultStr = if (usePersianDigits) {
            JalaliCalendarHelper.toPersianDigits(formattedStr)
        } else {
            formattedStr
        }

        return if (includeUnit) {
            "$resultStr ${unit.titleFa}"
        } else {
            resultStr
        }
    }
}

