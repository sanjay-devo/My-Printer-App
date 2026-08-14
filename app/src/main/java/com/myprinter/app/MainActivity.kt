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
            Toast.makeText(this, R.string.save_as_pdf, Toast.LENGTH_SHORT).show()
        }

        btnPhotos.setOnClickListener {
            Toast.makeText(this, R.string.photos_and_images, Toast.LENGTH_SHORT).show()
        }

        btnDocuments.setOnClickListener {
            Toast.makeText(this, R.string.documents, Toast.LENGTH_SHORT).show()
        }
    }
}