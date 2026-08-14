package com.myprinter.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure edge-to-edge with light status bar icons (dark style)
        // and #5E0006 as the background color for older Android versions.
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

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to the header so its background shows behind the status bar area.
            header.updatePadding(top = systemBars.top)
            
            // Apply side and bottom insets to the root layout.
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
            Toast.makeText(this, R.string.save_as_pdf, Toast.LENGTH_SHORT).show()
        }
    }
}