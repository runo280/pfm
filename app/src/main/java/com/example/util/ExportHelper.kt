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
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 12f
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(13, 27, 42) // Dark Navy
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }

            val headerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(16, 185, 129) // Emerald
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }

            // Document Header
            var y = 50f
            canvas.drawText("گزارش تراکنش‌های مالی - مدیریت مالی", 50f, y, titlePaint)
            y += 25f
            val dateStr = "تاریخ تنظیم: ${JalaliCalendarHelper.getCurrentJalaliDate().toReadablePersianString()}"
            canvas.drawText(dateStr, 50f, y, paint)
            y += 35f

            // Summary Box Background
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

            canvas.drawRect(40f, y - 18f, 550f, y + 22f, summaryBgPaint)
            canvas.drawRect(40f, y - 18f, 550f, y + 22f, summaryBorderPaint)

            val incomePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(16, 185, 129) // Emerald Green
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            }

            val expensePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(239, 68, 68) // Red
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            }

            val diffPaint = Paint().apply {
                isAntiAlias = true
                color = if (netBalance >= 0) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            }

            canvas.drawText(incStr, 50f, y, incomePaint)
            canvas.drawText(expStr, 220f, y, expensePaint)
            canvas.drawText(diffStr, 385f, y, diffPaint)
            y += 45f

            // Table Header
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 11f
            canvas.drawText("ردیف", 40f, y, paint)
            canvas.drawText("تاریخ", 80f, y, paint)
            canvas.drawText("عنوان", 160f, y, paint)
            canvas.drawText("نوع", 320f, y, paint)
            canvas.drawText("مبلغ", 390f, y, paint)
            canvas.drawText("حساب", 480f, y, paint)
            y += 10f

            // Table Line
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }
            canvas.drawLine(40f, y, 550f, y, linePaint)
            y += 20f

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f

            val limit = minOf(transactions.size, 35) // Fit first page
            for (i in 0 until limit) {
                val tx = transactions[i]
                val typeFa = when (tx.type) {
                    "EXPENSE" -> "هزینه"
                    "INCOME" -> "درآمد"
                    else -> "انتقال"
                }
                val acc = accountsMap[tx.accountId]?.name ?: "-"
                val amountStr = CurrencyHelper.formatAmount(tx.amount, currencyUnit, includeUnit = false)

                val truncatedTitle = if (tx.title.length > 22) tx.title.take(20) + ".." else tx.title

                canvas.drawText("${i + 1}", 40f, y, paint)
                canvas.drawText(tx.jalaliDate, 80f, y, paint)
                canvas.drawText(truncatedTitle, 160f, y, paint)
                canvas.drawText(typeFa, 320f, y, paint)
                canvas.drawText(amountStr, 390f, y, paint)
                canvas.drawText(acc, 480f, y, paint)

                y += 18f
            }

            pdfDocument.finishPage(page)

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
