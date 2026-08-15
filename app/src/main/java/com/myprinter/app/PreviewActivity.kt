package com.myprinter.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.view.View
import android.widget.ImageView
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
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.myprinter.app.adapters.PrintAdapter
import com.myprinter.app.models.PrintItem
import com.myprinter.app.models.PrintSettings
import com.myprinter.app.models.PrinterDestination
import com.myprinter.app.models.PrinterManager
import com.myprinter.app.utils.PrintUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val setupPrinterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val destination = result.data?.getParcelableExtra<PrinterDestination>("SELECTED_PRINTER")
            if (destination != null) {
                PrinterManager.selectedPrinter = destination
                updatePrinterUI()
            }
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
        updatePrinterUI()
    }

    private fun setupRecyclerView() {
        val rvPages = findViewById<RecyclerView>(R.id.rvPages)
        val tvPageNumber = findViewById<TextView>(R.id.tvGlobalPageNumber)
        
        printAdapter = PrintAdapter(lifecycleScope)
        rvPages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPages.adapter = printAdapter
        
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rvPages)
        
        rvPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstVisibleItemPosition()
                if (position != RecyclerView.NO_POSITION) {
                    val total = printAdapter.itemCount
                    tvPageNumber.text = "${position + 1} / $total"
                }
            }
        })
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.ivAdd).setOnClickListener {
            pickMoreImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<View>(R.id.ivSettings).setOnClickListener {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.tvSaveAsPdf).setOnClickListener {
            val intent = Intent(this, PrinterSetupActivity::class.java).apply {
                putExtra("CURRENT_DESTINATION", PrinterManager.selectedPrinter)
            }
            setupPrinterLauncher.launch(intent)
        }
        
        findViewById<View>(R.id.btnPrint).setOnClickListener {
            when (val destination = PrinterManager.selectedPrinter) {
                is PrinterDestination.Pdf -> {
                    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                    savePdfLauncher.launch("My_Printer_$date.pdf")
                }
                is PrinterDestination.Usb -> {
                    Toast.makeText(this, "Printing to ${destination.productName ?: "USB Printer"}...", Toast.LENGTH_SHORT).show()
                    // Real USB printing would be implemented here or in a separate manager
                }
                else -> {
                    Toast.makeText(this, "Selected printer is not available for printing yet.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePrinterUI() {
        val tvPrinter = findViewById<TextView>(R.id.tvSaveAsPdf)
        val ivPrinter = findViewById<ImageView>(R.id.ivPrinter)
        
        when (val destination = PrinterManager.selectedPrinter) {
            is PrinterDestination.Pdf -> {
                tvPrinter.text = getString(R.string.save_as_pdf)
                ivPrinter.setImageResource(R.drawable.ic_document)
            }
            is PrinterDestination.Usb -> {
                tvPrinter.text = destination.productName ?: getString(R.string.usb_printers)
                ivPrinter.setImageResource(R.drawable.ic_usb)
            }
            is PrinterDestination.WifiPlaceholder -> {
                tvPrinter.text = getString(R.string.wifi_printers)
                ivPrinter.setImageResource(R.drawable.ic_wifi)
            }
            is PrinterDestination.BluetoothPlaceholder -> {
                tvPrinter.text = getString(R.string.bluetooth_printers)
                ivPrinter.setImageResource(R.drawable.ic_bluetooth)
            }
        }
    }

    private fun updateUI() {
        printAdapter.setItems(printItems)
        val totalPages = printItems.sumOf { it.pageCount }
        findViewById<TextView>(R.id.tvCopiesInfo).text = "Copies: ${printSettings.copies} | All ($totalPages pages)"
        findViewById<TextView>(R.id.tvGlobalPageNumber).text = "1 / $totalPages"
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
                        Toast.makeText(this@PreviewActivity, "PDF saved successfully", Toast.LENGTH_SHORT).show()
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
}
