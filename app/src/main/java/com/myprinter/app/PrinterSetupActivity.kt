package com.myprinter.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.myprinter.app.models.PrinterDestination

class PrinterSetupActivity : AppCompatActivity() {

    private var currentDestination: PrinterDestination? = null

    private val printerPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val destination = result.data?.getParcelableExtra<PrinterDestination>("SELECTED_PRINTER")
            if (destination != null) {
                returnResult(destination)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.parseColor("#5E0006"))
        )
        setContentView(R.layout.activity_printer_setup)

        currentDestination = intent.getParcelableExtra("CURRENT_DESTINATION")

        val main = findViewById<View>(R.id.main)
        val header = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.updatePadding(top = systemBars.top)
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            insets
        }

        setupClickListeners()
        updateSelectionUI()
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.ivBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnSaveAsPdf).setOnClickListener {
            returnResult(PrinterDestination.Pdf)
        }

        findViewById<View>(R.id.btnWifiPrinters).setOnClickListener {
            printerPickerLauncher.launch(Intent(this, WifiPrintersActivity::class.java))
        }

        findViewById<View>(R.id.btnBluetoothPrinters).setOnClickListener {
            printerPickerLauncher.launch(Intent(this, BluetoothPrintersActivity::class.java))
        }

        findViewById<View>(R.id.btnUsbPrinters).setOnClickListener {
            printerPickerLauncher.launch(Intent(this, UsbPrintersActivity::class.java))
        }
    }

    private fun updateSelectionUI() {
        findViewById<ImageView>(R.id.ivCheckPdf).visibility = 
            if (currentDestination is PrinterDestination.Pdf) View.VISIBLE else View.GONE
    }

    private fun returnResult(destination: PrinterDestination) {
        val resultIntent = Intent().apply {
            putExtra("SELECTED_PRINTER", destination)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
