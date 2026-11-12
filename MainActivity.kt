package com.example.wocabeehelper

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        val statusText = TextView(this).apply {
            text = "Status: STOPPED"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val startButton = Button(this).apply {
            text = "START"
            textSize = 18f
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                WocaAccessibilityService.isAutoRunning = true
                statusText.text = "Status: RUNNING (Switch to WocaBee)"
            }
        }

        val stopButton = Button(this).apply {
            text = "STOP"
            textSize = 18f
            setBackgroundColor(Color.parseColor("#C62828"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                WocaAccessibilityService.isAutoRunning = false
                statusText.text = "Status: STOPPED"
            }
        }

        val permissionButton = Button(this).apply {
            text = "1. Grant Overlay & Accessibility"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                } else {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }

        layout.addView(statusText)
        layout.addView(startButton)
        layout.addView(stopButton)
        layout.addView(permissionButton)

        setContentView(layout)
    }
}
