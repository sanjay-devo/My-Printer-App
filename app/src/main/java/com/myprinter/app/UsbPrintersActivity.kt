package com.myprinter.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myprinter.app.models.PrinterDestination

class UsbPrintersActivity : AppCompatActivity() {

    companion object {
        const val ACTION_USB_PERMISSION = "com.myprinter.app.USB_PERMISSION"
        const val ACTION_USB_PERMISSION_RESULT = "com.myprinter.app.USB_PERMISSION_RESULT"
    }

    private lateinit var usbManager: UsbManager
    private lateinit var adapter: UsbDeviceAdapter
    private val usbDevices = mutableListOf<UsbDevice>()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION_RESULT == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { selectDevice(it) }
                    } else {
                        Toast.makeText(context, R.string.usb_permission_denied, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.parseColor("#5E0006"))
        )
        setContentView(R.layout.activity_usb_printers)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val main = findViewById<View>(R.id.main)
        val header = findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.updatePadding(top = systemBars.top)
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupClickListeners()
        discoverDevices()

        val filter = IntentFilter(ACTION_USB_PERMISSION_RESULT)
        ContextCompat.registerReceiver(this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvUsbPrinters)
        adapter = UsbDeviceAdapter(usbDevices) { device ->
            requestPermission(device)
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<View>(R.id.tvRefresh).setOnClickListener { discoverDevices() }
        findViewById<View>(R.id.btnRefresh).setOnClickListener { discoverDevices() }
    }

    private fun discoverDevices() {
        usbDevices.clear()
        val deviceList = usbManager.deviceList
        for (device in deviceList.values) {
            usbDevices.add(device)
        }
        
        adapter.notifyDataSetChanged()
        
        val emptyView = findViewById<View>(R.id.emptyView)
        val rv = findViewById<View>(R.id.rvUsbPrinters)
        
        if (usbDevices.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
    }

    private fun requestPermission(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            selectDevice(device)
        } else {
            val intent = Intent(this, UsbPermissionReceiver::class.java).apply {
                action = ACTION_USB_PERMISSION
            }
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val permissionIntent = PendingIntent.getBroadcast(this, 0, intent, flags)
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    private fun selectDevice(device: UsbDevice) {
        val destination = PrinterDestination.Usb(
            deviceName = device.deviceName,
            manufacturerName = device.manufacturerName,
            productName = device.productName,
            vendorId = device.vendorId,
            productId = device.productId
        )
        val resultIntent = Intent().apply {
            putExtra("SELECTED_PRINTER", destination)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private inner class UsbDeviceAdapter(
        private val devices: List<UsbDevice>,
        private val onClick: (UsbDevice) -> Unit
    ) : RecyclerView.Adapter<UsbDeviceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvDetails: TextView = view.findViewById(R.id.tvDetails)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_usb_device, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            val isPrinter = isLikelyPrinter(device)
            
            holder.ivIcon.setImageResource(if (isPrinter) R.drawable.ic_printer else R.drawable.ic_usb)
            holder.tvName.text = device.productName ?: "Unknown USB Device"
            holder.tvDetails.text = "Vendor ID: ${device.vendorId} | Product ID: ${device.productId}"
            
            if (usbManager.hasPermission(device)) {
                holder.tvStatus.text = "Connected"
                holder.tvStatus.setTextColor(Color.parseColor("#D53E0F")) // Accent Orange
            } else {
                holder.tvStatus.text = "Ready to connect"
                holder.tvStatus.setTextColor(Color.parseColor("#6F6263")) // Secondary Text
            }

            holder.itemView.setOnClickListener { onClick(device) }
        }

        override fun getItemCount() = devices.size

        private fun isLikelyPrinter(device: UsbDevice): Boolean {
            if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
            for (i in 0 until device.interfaceCount) {
                if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
            }
            return false
        }
    }
}
