package com.example.util

import com.example.data.local.InstallmentEntity
import java.util.Calendar
import java.util.Locale

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun toFormattedString(): String {
        return String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
    }

    fun toReadablePersianString(): String {
        val monthName = JalaliCalendarHelper.PERSIAN_MONTH_NAMES.getOrNull(month - 1) ?: ""
        return "${JalaliCalendarHelper.toPersianDigits(day)} $monthName ${JalaliCalendarHelper.toPersianDigits(year)}"
    }
}

object JalaliCalendarHelper {
    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun getCurrentJalaliDate(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun getCurrentTimeString(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%02d-%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    fun getCurrentJalaliDateTimeString(): String {
        val dateStr = getCurrentJalaliDate().toFormattedString().replace('/', '_')
        val timeStr = getCurrentTimeString()
        return "${dateStr}_$timeStr"
    }

    fun getTomorrowJalaliDate(): JalaliDate {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return gregorianToJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun getCurrentJalaliYearMonth(): String {
        val jd = getCurrentJalaliDate()
        return String.format(Locale.US, "%04d/%02d", jd.year, jd.month)
    }

    fun getCurrentDayOfWeekName(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun getCurrentFullJalaliDateWithDay(): String {
        val today = getCurrentJalaliDate()
        val dayName = getCurrentDayOfWeekName()
        return "$dayName، ${today.toReadablePersianString()}"
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
        val jd = 1 + (if (days < 186) days % 31 else (days - 186) % 30)
        return JalaliDate(jy, jm, jd)
    }

    fun parseJalaliDate(dateStr: String): JalaliDate? {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                JalaliDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun jalaliToJdn(year: Int, month: Int, day: Int): Long {
        val jy = year - 979
        val jm = month - 1
        val jd = day - 1
        var jDayNo = 365L * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4
        for (i in 0 until jm) {
            jDayNo += if (i < 6) 31 else 30
        }
        return jDayNo + jd
    }

    fun jdnToJalali(jdn: Long): JalaliDate {
        var jDayNo = jdn
        var jy = (33 * jDayNo + 3) / 12053
        var jDayNoTemp = 365L * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4
        while (jDayNo < jDayNoTemp) {
            jy--
            jDayNoTemp = 365L * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4
        }
        var dayDiff = (jDayNo - jDayNoTemp).toInt()
        var jm = 0
        while (jm < 12) {
            val daysInM = if (jm < 6) 31 else 30
            if (dayDiff < daysInM) break
            dayDiff -= daysInM
            jm++
        }
        val year = (jy + 979).toInt()
        val month = jm + 1
        val day = dayDiff + 1
        return JalaliDate(year, month, day)
    }

    fun addDays(date: JalaliDate, days: Int): JalaliDate {
        val jdn = jalaliToJdn(date.year, date.month, date.day) + days
        return jdnToJalali(jdn)
    }

    fun addMonths(year: Int, month: Int, monthsToAdd: Int): Pair<Int, Int> {
        val totalMonths = (year * 12 + (month - 1)) + monthsToAdd
        var newYear = totalMonths / 12
        var newMonth = (totalMonths % 12) + 1
        if (newMonth <= 0) {
            newMonth += 12
            newYear -= 1
        }
        return Pair(newYear, newMonth)
    }

    fun daysBetween(fromDate: JalaliDate, toDate: JalaliDate): Long {
        val jdnFrom = jalaliToJdn(fromDate.year, fromDate.month, fromDate.day)
        val jdnTo = jalaliToJdn(toDate.year, toDate.month, toDate.day)
        return jdnTo - jdnFrom
    }

    fun getDaysRemainingMessage(targetDate: JalaliDate, today: JalaliDate = getCurrentJalaliDate()): String {
        val diffDays = daysBetween(today, targetDate)
        return when {
            diffDays == 0L -> "امروز سررسید است!"
            diffDays > 0 -> "${toPersianDigits(diffDays)} روز مانده تا سررسید"
            else -> "${toPersianDigits(-diffDays)} روز سررسید گذشته (تاخیر)"
        }
    }

    fun getDaysRemainingMessage(targetJalaliDateStr: String, today: JalaliDate = getCurrentJalaliDate()): String {
        val targetDate = parseJalaliDate(targetJalaliDateStr) ?: return ""
        return getDaysRemainingMessage(targetDate, today)
    }

    fun isJalaliLeapYear(year: Int): Boolean {
        val jy = year - 474
        val rem = (jy % 2820 + 474 + 38) * 682
        return (rem % 2816) < 682
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    fun calculateInstallmentDueDate(baseJalaliDate: JalaliDate, dueDay: Int, installmentIndex: Int): JalaliDate {
        var totalMonths = (baseJalaliDate.year * 12 + (baseJalaliDate.month - 1)) + installmentIndex
        val year = totalMonths / 12
        val month = (totalMonths % 12) + 1
        val maxDays = getDaysInJalaliMonth(year, month)
        val day = dueDay.coerceAtMost(maxDays).coerceAtLeast(1)
        return JalaliDate(year, month, day)
    }

    fun getInstallmentNextDueDate(
        installment: InstallmentEntity,
        today: JalaliDate = getCurrentJalaliDate()
    ): JalaliDate {
        val startJDate = parseJalaliDate(installment.startJalaliDate)
        return if (startJDate != null) {
            calculateInstallmentDueDate(startJDate, installment.dueDay, installment.paidInstallments)
        } else {
            calculateInstallmentDueDate(today, installment.dueDay, 0)
        }
    }

    fun getInstallmentItemDueDate(
        installment: InstallmentEntity,
        itemNumber: Int,
        today: JalaliDate = getCurrentJalaliDate()
    ): JalaliDate {
        val startJDate = parseJalaliDate(installment.startJalaliDate)
        val base = startJDate ?: today
        return calculateInstallmentDueDate(base, installment.dueDay, itemNumber - 1)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
        val jy2 = jy + 1595
        var days = -355668 + (365 * jy2) + (jy2 / 33) * 8 + (((jy2 % 33) + 3) / 4) + jd + (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val sal_a = intArrayOf(0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > sal_a[gm]) {
            gd -= sal_a[gm]
            gm++
        }
        return intArrayOf(gy, gm, gd)
    }

    fun getDayOfWeekIndex(date: JalaliDate): Int { // 0: Saturday (شنبه), 1: Sunday, ..., 6: Friday
        val g = jalaliToGregorian(date.year, date.month, date.day)
        val cal = Calendar.getInstance().apply {
            set(g[0], g[1] - 1, g[2])
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun getWeekRange(refDate: JalaliDate): Pair<JalaliDate, JalaliDate> {
        val dayIdx = getDayOfWeekIndex(refDate) // 0..6
        val weekStart = addDays(refDate, -dayIdx) // Saturday
        val weekEnd = addDays(weekStart, 6) // Friday
        return Pair(weekStart, weekEnd)
    }

    fun toPersianDigits(number: Any): String {
        val str = number.toString()
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                builder.append(persianDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }
}
