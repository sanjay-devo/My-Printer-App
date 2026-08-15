package com.myprinter.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.myprinter.app.models.PrinterDestination
import com.myprinter.app.models.PrinterManager
import com.myprinter.app.utils.PrintUtils

class MainActivity : AppCompatActivity() {

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { handleSelectedUri(it) }
    }

    private val pickPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleSelectedUri(it) }
    }

    private val printerSetupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val destination = result.data?.getParcelableExtra<PrinterDestination>("SELECTED_PRINTER")
            if (destination != null) {
                PrinterManager.selectedPrinter = destination
                updatePrinterUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                Color.parseColor("#5E0006")
            )
        )
        
        setContentView(R.layout.activity_main)

        val main = findViewById<View>(R.id.main)
        val header = findViewById<View>(R.id.header)
        val ivInfo = findViewById<View>(R.id.ivInfo)
        val btnSaveAsPdf = findViewById<View>(R.id.btnSaveAsPdf)
        val btnPhotos = findViewById<View>(R.id.btnPhotos)
        val btnDocuments = findViewById<View>(R.id.btnDocuments)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.updatePadding(top = systemBars.top)
            v.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        ivInfo.setOnClickListener {
            Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show()
        }

        btnSaveAsPdf.setOnClickListener {
            val intent = Intent(this, PrinterSetupActivity::class.java).apply {
                putExtra("CURRENT_DESTINATION", PrinterManager.selectedPrinter)
            }
            printerSetupLauncher.launch(intent)
        }

        btnPhotos.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnDocuments.setOnClickListener {
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }

        updatePrinterUI()
    }

    private fun updatePrinterUI() {
        val tvPrinterName = findViewById<TextView>(R.id.tvSaveAsPdf)
        val ivPrinterIcon = findViewById<ImageView>(R.id.ivSaveAsPdf)
        
        when (val destination = PrinterManager.selectedPrinter) {
            is PrinterDestination.Pdf -> {
                tvPrinterName.text = getString(R.string.save_as_pdf)
                ivPrinterIcon.setImageResource(R.drawable.ic_document)
            }
            is PrinterDestination.Usb -> {
                tvPrinterName.text = destination.productName ?: getString(R.string.usb_printers)
                ivPrinterIcon.setImageResource(R.drawable.ic_usb)
            }
            is PrinterDestination.WifiPlaceholder -> {
                tvPrinterName.text = getString(R.string.wifi_printers)
                ivPrinterIcon.setImageResource(R.drawable.ic_wifi)
            }
            is PrinterDestination.BluetoothPlaceholder -> {
                tvPrinterName.text = getString(R.string.bluetooth_printers)
                ivPrinterIcon.setImageResource(R.drawable.ic_bluetooth)
            }
        }
    }

    private fun handleSelectedUri(uri: android.net.Uri) {
        val printItem = PrintUtils.getPrintItemFromUri(this, uri)
        if (printItem != null) {
            val intent = Intent(this, PreviewActivity::class.java).apply {
                putExtra("INITIAL_ITEM", printItem)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Failed to analyze file", Toast.LENGTH_SHORT).show()
        }
    }
}
