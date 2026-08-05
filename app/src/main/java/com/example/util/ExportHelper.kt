package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    fun exportToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        accountsMap: Map<Long, AccountEntity>,
        categoriesMap: Map<Long, CategoryEntity>,
        currencyUnit: CurrencyUnit
    ): Uri? {
        return try {
            val sb = StringBuilder()
            // UTF-8 BOM for Excel Persian support
            sb.append('\uFEFF')
            sb.append("ردیف,تاریخ شمسی,نوع,عنوان,مبلغ (${currencyUnit.titleFa}),حساب,دسته‌بندی,توضیحات\n")

            transactions.forEachIndexed { index, tx ->
                val typeName = when (tx.type) {
                    "EXPENSE" -> "هزینه"
                    "INCOME" -> "درآمد"
                    "TRANSFER" -> "انتقال"
                    else -> tx.type
                }
                val accName = accountsMap[tx.accountId]?.name ?: "نامشخص"
                val catName = categoriesMap[tx.categoryId]?.name ?: "بدون دسته‌بندی"
                val formattedAmount = CurrencyHelper.formatAmount(tx.amount, currencyUnit, includeUnit = false, usePersianDigits = false)

                val titleClean = tx.title.replace(",", " ")
                val noteClean = tx.note.replace(",", " ")

                sb.append("${index + 1},${tx.jalaliDate},$typeName,$titleClean,$formattedAmount,$accName,$catName,$noteClean\n")
            }

            val fileName = "گزارش_مالی_${JalaliCalendarHelper.getCurrentJalaliDateTimeString()}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.trim().split("\\s+".toRegex())
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                if (paint.measureText(word) > maxWidth) {
                    var subWord = word
                    while (subWord.isNotEmpty() && paint.measureText(subWord) > maxWidth) {
                        var chunkLen = subWord.length - 1
                        while (chunkLen > 0 && paint.measureText(subWord.substring(0, chunkLen)) > maxWidth) {
                            chunkLen--
                        }
                        if (chunkLen == 0) chunkLen = 1
                        lines.add(subWord.substring(0, chunkLen))
                        subWord = subWord.substring(chunkLen)
                    }
                    currentLine = subWord
                } else {
                    currentLine = word
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    fun exportToPdf(
        context: Context,
        transactions: List<TransactionEntity>,
        accountsMap: Map<Long, AccountEntity>,
        categoriesMap: Map<Long, CategoryEntity>,
        totalIncome: Double,
        totalExpense: Double,
        currencyUnit: CurrencyUnit
    ): Uri? {
        return try {
            val pdfDocument = PdfDocument()
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = currentPage.canvas

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(13, 27, 42) // Dark Navy
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
            }

            val datePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(71, 85, 105)
                textSize = 11f
            }

            val sectionTitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }

            val tableHeaderPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(30, 41, 59)
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }

            val tableHeaderLeftPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(30, 41, 59)
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }

            val bodyPaintRight = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 10f
                textAlign = Paint.Align.RIGHT
            }

            val titleBoldPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }

            val notePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(71, 85, 105)
                textSize = 9f
                textAlign = Paint.Align.RIGHT
            }

            val subTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(51, 65, 85)
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
            }

            val incomeAmountPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(16, 185, 129)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }

            val expenseAmountPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(239, 68, 68)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }

            val dividerPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 0.8f
            }

            val linePaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                strokeWidth = 1f
            }

            var y = 45f

            fun checkPageBreak(requiredHeight: Float, activeSectionName: String? = null, activeSectionColor: Int? = null) {
                if (y + requiredHeight > 780f) {
                    pdfDocument.finishPage(currentPage)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    y = 45f

                    if (activeSectionName != null && activeSectionColor != null) {
                        sectionTitlePaint.color = activeSectionColor
                        canvas.drawText("$activeSectionName (ادامه)", 550f, y, sectionTitlePaint.apply { textAlign = Paint.Align.RIGHT })
                        y += 20f

                        canvas.drawText("ردیف", 550f, y, tableHeaderPaint)
                        canvas.drawText("تاریخ", 515f, y, tableHeaderPaint)
                        canvas.drawText("عنوان و توضیحات", 445f, y, tableHeaderPaint)
                        canvas.drawText("دسته / حساب", 225f, y, tableHeaderPaint)
                        canvas.drawText("مبلغ (${currencyUnit.titleFa})", 40f, y, tableHeaderLeftPaint)
                        y += 8f
                        canvas.drawLine(40f, y, 550f, y, linePaint)
                        y += 18f
                    }
                }
            }

            // Document Header
            canvas.drawText("گزارش جامع تراکنش‌های مالی - مدیریت مالی", 550f, y, titlePaint.apply { textAlign = Paint.Align.RIGHT })
            y += 20f
            val dateStr = "تاریخ تنظیم گزارش: ${JalaliCalendarHelper.getCurrentJalaliDate().toReadablePersianString()}"
            canvas.drawText(dateStr, 550f, y, datePaint.apply { textAlign = Paint.Align.RIGHT })
            y += 16f

            val dateRangeStr = if (transactions.isNotEmpty()) {
                val sortedDates = transactions.mapNotNull { JalaliCalendarHelper.parseJalaliDate(it.jalaliDate) }
                    .sortedWith(compareBy({ it.year }, { it.month }, { it.day }))
                if (sortedDates.isNotEmpty()) {
                    val minD = sortedDates.first().toReadablePersianString()
                    val maxD = sortedDates.last().toReadablePersianString()
                    if (minD == maxD) {
                        "بازه زمانی گزارش: $minD"
                    } else {
                        "بازه زمانی گزارش: از $minD تا $maxD"
                    }
                } else {
                    "بازه زمانی گزارش: کلیه تراکنش‌ها"
                }
            } else {
                "بازه زمانی گزارش: بدون تراکنش"
            }
            canvas.drawText(dateRangeStr, 550f, y, datePaint.apply { textAlign = Paint.Align.RIGHT })
            y += 28f

            // Summary Box
            val netBalance = totalIncome - totalExpense
            val incStr = "جمع درآمدها: ${CurrencyHelper.formatAmount(totalIncome, currencyUnit)}"
            val expStr = "جمع هزینه‌ها: ${CurrencyHelper.formatAmount(totalExpense, currencyUnit)}"
            val diffStr = "تفاضل (مانده): ${CurrencyHelper.formatAmount(netBalance, currencyUnit)}"

            val summaryBgPaint = Paint().apply {
                color = Color.rgb(243, 244, 246)
                style = Paint.Style.FILL
            }
            val summaryBorderPaint = Paint().apply {
                color = Color.rgb(229, 231, 235)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            canvas.drawRect(40f, y - 16f, 550f, y + 24f, summaryBgPaint)
            canvas.drawRect(40f, y - 16f, 550f, y + 22f, summaryBorderPaint)

            val summaryIncomePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(16, 185, 129)
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val summaryExpensePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(239, 68, 68)
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val summaryDiffPaint = Paint().apply {
                isAntiAlias = true
                color = if (netBalance >= 0) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawText(incStr, 540f, y, summaryIncomePaint)
            canvas.drawText(expStr, 360f, y, summaryExpensePaint)
            canvas.drawText(diffStr, 180f, y, summaryDiffPaint)
            y += 45f

            // Split transactions into lists
            val incomeList = transactions.filter { it.type == "INCOME" }
            val expenseList = transactions.filter { it.type == "EXPENSE" }
            val transferList = transactions.filter { it.type == "TRANSFER" }

            fun renderTransactionSection(
                sectionName: String,
                sectionColor: Int,
                txList: List<TransactionEntity>,
                isIncome: Boolean
            ) {
                checkPageBreak(60f)
                sectionTitlePaint.color = sectionColor
                canvas.drawText(sectionName, 550f, y, sectionTitlePaint.apply { textAlign = Paint.Align.RIGHT })
                y += 20f

                if (txList.isEmpty()) {
                    val emptyPaint = Paint().apply {
                        isAntiAlias = true
                        color = Color.GRAY
                        textSize = 10f
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText("هیچ تراکنشی در این بخش ثبت نشده است.", 550f, y, emptyPaint)
                    y += 25f
                    return
                }

                // Table Header RTL
                canvas.drawText("ردیف", 550f, y, tableHeaderPaint)
                canvas.drawText("تاریخ", 515f, y, tableHeaderPaint)
                canvas.drawText("عنوان و توضیحات", 445f, y, tableHeaderPaint)
                canvas.drawText("دسته / حساب", 225f, y, tableHeaderPaint)
                canvas.drawText("مبلغ (${currencyUnit.titleFa})", 40f, y, tableHeaderLeftPaint)
                y += 8f
                canvas.drawLine(40f, y, 550f, y, linePaint)
                y += 18f

                txList.forEachIndexed { index, tx ->
                    val titleLines = wrapText(tx.title, titleBoldPaint, 210f)
                    val noteText = if (tx.note.isNotBlank()) "توضیحات: ${tx.note}" else ""
                    val noteLines = if (noteText.isNotBlank()) wrapText(noteText, notePaint, 210f) else emptyList()

                    val catName = categoriesMap[tx.categoryId]?.name ?: "بدون دسته‌بندی"
                    val accName = accountsMap[tx.accountId]?.name ?: "-"
                    val catAccStr = "$catName ($accName)"
                    val catAccLines = wrapText(catAccStr, subTextPaint, 175f)

                    val textBlockLines = titleLines.size + noteLines.size
                    val maxLines = maxOf(textBlockLines, catAccLines.size, 1)
                    val rowHeight = maxLines * 13f + 10f

                    checkPageBreak(rowHeight, sectionName, sectionColor)

                    // Draw Row Index (RTL at x=550)
                    canvas.drawText(JalaliCalendarHelper.toPersianDigits(index + 1), 550f, y + 10f, bodyPaintRight)

                    // Draw Date with Persian letters (RTL at x=515)
                    val persianDateStr = JalaliCalendarHelper.parseJalaliDate(tx.jalaliDate)?.toReadablePersianString()
                        ?: JalaliCalendarHelper.toPersianDigits(tx.jalaliDate)
                    canvas.drawText(persianDateStr, 515f, y + 10f, bodyPaintRight)

                    // Draw Title & Full Description (RTL at x=445)
                    var lineY = y + 10f
                    titleLines.forEach { line ->
                        canvas.drawText(line, 445f, lineY, titleBoldPaint)
                        lineY += 13f
                    }
                    noteLines.forEach { line ->
                        canvas.drawText(line, 445f, lineY, notePaint)
                        lineY += 13f
                    }

                    // Draw Category & Account (RTL at x=225)
                    var catY = y + 10f
                    catAccLines.forEach { line ->
                        canvas.drawText(line, 225f, catY, subTextPaint)
                        catY += 13f
                    }

                    // Draw Amount (LEFT aligned at x=40)
                    val amountStr = CurrencyHelper.formatAmount(tx.amount, currencyUnit, includeUnit = false)
                    val amtPaint = if (isIncome) incomeAmountPaint else expenseAmountPaint
                    canvas.drawText(amountStr, 40f, y + 10f, amtPaint)

                    y += rowHeight
                    canvas.drawLine(40f, y - 4f, 550f, y - 4f, dividerPaint)
                }

                y += 20f
            }

            // Render Income Section
            renderTransactionSection(
                sectionName = "۱. لیست درآمدها",
                sectionColor = Color.rgb(16, 185, 129), // Green
                txList = incomeList,
                isIncome = true
            )

            // Render Expense Section
            renderTransactionSection(
                sectionName = "۲. لیست هزینه‌ها",
                sectionColor = Color.rgb(239, 68, 68), // Red
                txList = expenseList,
                isIncome = false
            )

            // Render Transfer Section (if any transfers exist)
            if (transferList.isNotEmpty()) {
                renderTransactionSection(
                    sectionName = "۳. لیست انتقال‌ها",
                    sectionColor = Color.rgb(59, 130, 246), // Blue
                    txList = transferList,
                    isIncome = false
                )
            }

            pdfDocument.finishPage(currentPage)

            val fileName = "گزارش_مالی_${JalaliCalendarHelper.getCurrentJalaliDateTimeString()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
