package com.myprinter.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build

class UsbPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (UsbPrintersActivity.ACTION_USB_PERMISSION == action) {
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }

            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            
            // Forward the result to the active UsbPrintersActivity
            val resultIntent = Intent(UsbPrintersActivity.ACTION_USB_PERMISSION_RESULT).apply {
                setPackage(context.packageName)
                putExtra(UsbManager.EXTRA_DEVICE, device)
                putExtra(UsbManager.EXTRA_PERMISSION_GRANTED, granted)
            }
            context.sendBroadcast(resultIntent)
        }
    }
}
