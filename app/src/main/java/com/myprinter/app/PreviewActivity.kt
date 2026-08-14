package com.myprinter.app

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myprinter.app.adapters.PrintAdapter
import com.myprinter.app.models.PrintItem
import com.myprinter.app.models.PrintSettings
import com.myprinter.app.utils.PrintUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

class PreviewActivity : AppCompatActivity() {

    private lateinit var printAdapter: PrintAdapter
    private val printItems = mutableListOf<PrintItem>()
    private var printSettings = PrintSettings()

    private val pickMoreImages = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            PrintUtils.getPrintItemFromUri(this, uri)?.let { printItems.add(it) }
        }
        updateUI()
    }

    private val pickMoreDocs = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            PrintUtils.getPrintItemFromUri(this, it)?.let { printItems.add(it) }
            updateUI()
        }
    }

    private val savePdfLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { performSavePdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.parseColor("#5E0006"))
        )
        setContentView(R.layout.activity_preview)

        val main = findViewById<View>(R.id.main)
        val header = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.updatePadding(top = systemBars.top)
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            insets
        }

        val initialItem = intent.getParcelableExtra<PrintItem>("INITIAL_ITEM")
        initialItem?.let { printItems.add(it) }

        setupRecyclerView()
        setupClickListeners()
        updateUI()
    }

    private fun setupRecyclerView() {
        val rvPages = findViewById<RecyclerView>(R.id.rvPages)
        printAdapter = PrintAdapter(lifecycleScope)
        rvPages.layoutManager = LinearLayoutManager(this)
        rvPages.adapter = printAdapter
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.ivAdd).setOnClickListener {
            pickMoreImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<View>(R.id.ivSettings).setOnClickListener {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnPrint).setOnClickListener {
            doPrint()
        }
        findViewById<View>(R.id.tvSaveAsPdf).setOnClickListener {
            savePdfLauncher.launch("MyPrinter_Document.pdf")
        }
    }

    private fun updateUI() {
        printAdapter.setItems(printItems)
        val totalPages = printItems.sumOf { it.pageCount }
        findViewById<TextView>(R.id.tvCopiesInfo).text = "Copies: ${printSettings.copies} | All ($totalPages pages)"
    }

    private fun doPrint() {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "${getString(R.string.app_name)} Document"
        printManager.print(jobName, MyPrintAdapter(this, printItems), null)
    }

    private fun performSavePdf(uri: android.net.Uri) {
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("id", "res", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        
        val pdf = PrintedPdfDocument(this, printAttributes)
        var currentPageIndex = 0
        
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                printItems.forEach { item ->
                    for (i in 0 until item.pageCount) {
                        val page = pdf.startPage(currentPageIndex)
                        PrintUtils.drawItemToCanvas(this@PreviewActivity, page.canvas, item, i, page.canvas.width, page.canvas.height)
                        pdf.finishPage(page)
                        currentPageIndex++
                    }
                }
                try {
                    contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                        pdf.writeTo(FileOutputStream(pfd.fileDescriptor))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PreviewActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PreviewActivity, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    pdf.close()
                }
            }
        }
    }

    private class MyPrintAdapter(val context: Context, val items: List<PrintItem>) : PrintDocumentAdapter() {
        private var pdfDocument: PrintedPdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            pdfDocument = PrintedPdfDocument(context, newAttributes)
            
            val totalPages = items.sumOf { it.pageCount }
            if (totalPages > 0) {
                val info = PrintDocumentInfo.Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(totalPages)
                    .build()
                callback.onLayoutFinished(info, true)
            } else {
                callback.onLayoutFailed("No pages to print")
            }
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val pdf = pdfDocument ?: return
            var currentPageIndex = 0
            
            items.forEach { item ->
                for (i in 0 until item.pageCount) {
                    val page = pdf.startPage(currentPageIndex)
                    PrintUtils.drawItemToCanvas(context, page.canvas, item, i, page.canvas.width, page.canvas.height)
                    pdf.finishPage(page)
                    currentPageIndex++
                }
            }

            try {
                pdf.writeTo(FileOutputStream(destination.fileDescriptor))
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: IOException) {
                callback.onWriteFailed(e.toString())
            } finally {
                pdf.close()
                pdfDocument = null
            }
        }
    }
}
