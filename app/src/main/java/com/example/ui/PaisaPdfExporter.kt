package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.TransactionItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PaisaPdfExporter {

    fun exportTransactionsToPdf(
        context: Context,
        currencySymbol: String,
        totalIncome: Double,
        totalExpense: Double,
        netBalance: Double,
        categoryBreakdown: Map<String, Double>,
        transactions: List<TransactionItem>
    ) {
        try {
            val pdfDocument = PdfDocument()
            
            // Standard A4 Dimensions in PostScript points: 595 x 842
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Paint references for drawing
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 22f
                isFakeBoldText = true
                color = Color.parseColor("#4F378B") // Deep Purple Primary Logo theme
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.parseColor("#757575")
            }

            val sectionHeaderPaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                color = Color.parseColor("#4F378B")
            }

            val linePaint = Paint().apply {
                strokeWidth = 1f
                color = Color.parseColor("#E0E0E0")
            }

            var currentY = 50f

            // 1. Draw PDF Header Title
            canvas.drawText("PAISA TRACKER", 40f, currentY, titlePaint)
            currentY += 15f
            canvas.drawText("Track Every Rupee, Build Every Dream", 40f, currentY, subtitlePaint)
            
            // Draw current date
            val dateString = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Report Generated: $dateString", 380f, currentY, subtitlePaint)
            
            currentY += 25f
            canvas.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
            
            // 2. Draw Financial Overview Section
            currentY += 30f
            canvas.drawText("FINANCIAL STATEMENT SUMMARY", 40f, currentY, sectionHeaderPaint)
            
            currentY += 25f
            // Income, Expense, Balance Metrics Card Box draws
            val cardPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = Color.parseColor("#BDBDBD")
            }
            canvas.drawRect(40f, currentY, (pageWidth - 40).toFloat(), currentY + 60f, cardPaint)
            
            // Draw column values
            val columnY = currentY + 35f
            canvas.drawText("Total Income: $currencySymbol${String.format("%.2f", totalIncome)}", 60f, columnY, textPaint)
            canvas.drawText("Total Expense: $currencySymbol${String.format("%.2f", totalExpense)}", 220f, columnY, textPaint)
            
            val balancePaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = if (netBalance >= 0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            }
            canvas.drawText("Net Balance: $currencySymbol${String.format("%.2f", netBalance)}", 400f, columnY, balancePaint)
            
            // 3. Draw Category Wise Outlay Summary Table
            currentY += 100f
            canvas.drawText("CATEGORY OUTLAYS ANALYSIS (THIS MONTH)", 40f, currentY, sectionHeaderPaint)
            currentY += 10f
            canvas.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
            
            currentY += 20f
            if (categoryBreakdown.isEmpty()) {
                canvas.drawText("No categorized expenses recorded in this statement period.", 50f, currentY, subtitlePaint)
                currentY += 15f
            } else {
                // Table header
                canvas.drawText("Category", 50f, currentY, textPaint)
                canvas.drawText("Total Flow", 250f, currentY, textPaint)
                canvas.drawText("Consumption %", 420f, currentY, textPaint)
                currentY += 8f
                canvas.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
                
                categoryBreakdown.forEach { (cat, amt) ->
                    currentY += 20f
                    canvas.drawText(cat, 50f, currentY, textPaint)
                    canvas.drawText("$currencySymbol${String.format("%.2f", amt)}", 250f, currentY, textPaint)
                    
                    val percent = if (totalExpense > 0) String.format("%.1f", (amt / totalExpense) * 100) else "0.0"
                    canvas.drawText("$percent%", 420f, currentY, textPaint)
                }
                currentY += 15f
            }
            
            // 4. Draw Recent Ledger Entries (Top 12 Transactions)
            currentY += 25f
            canvas.drawText("COMPLETE TRANSACTION LOG", 40f, currentY, sectionHeaderPaint)
            currentY += 10f
            canvas.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
            
            currentY += 20f
            if (transactions.isEmpty()) {
                canvas.drawText("No transaction ledger items exist in your statement log.", 50f, currentY, subtitlePaint)
            } else {
                // Register headers
                canvas.drawText("Date", 50f, currentY, textPaint)
                canvas.drawText("Description", 150f, currentY, textPaint)
                canvas.drawText("Type", 320f, currentY, textPaint)
                canvas.drawText("Flow", 440f, currentY, textPaint)
                currentY += 8f
                canvas.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
                
                val limit = transactions.take(12) // Fit onto the single page nicely
                val dateFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                
                limit.forEach { trans ->
                    currentY += 20f
                    canvas.drawText(dateFormat.format(Date(trans.date)), 50f, currentY, textPaint)
                    canvas.drawText(trans.title, 150f, currentY, textPaint)
                    
                    val typePaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 11f
                        color = if (trans.isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                    }
                    val actionType = if (trans.isIncome) "Income" else "Expense"
                    canvas.drawText(actionType, 320f, currentY, typePaint)
                    canvas.drawText("$currencySymbol${String.format("%.1f", trans.amount)}", 440f, currentY, typePaint)
                }
            }

            pdfDocument.finishPage(page)

            // Save Pdf Document to cache storage privately
            val fileName = "Paisa_Tracker_Statement.pdf"
            val file = File(context.cacheDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.close()
            
            // Dispatch dynamic Sharing/viewing intent to standard Android OS FileProvider
            val intentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, intentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Paisa Tracker - Statement Report")
                putExtra(Intent.EXTRA_TEXT, "Here is my financial summary generated using Paisa Tracker.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Save or Share Paisa Report"))
            Toast.makeText(context, "PDF Report Prepared Successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Preparation Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
