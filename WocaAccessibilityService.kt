package com.example.wocabeehelper

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button

class WocaAccessibilityService : AccessibilityService() {

    companion object {
        var isAutoRunning = false
        val dictionary = mutableMapOf(
            "pes" to "dog",
            "kočka" to "cat",
            "dům" to "house",
            "auto" to "car",
            "škola" to "school"
        )
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private var isCurrentlyTyping = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        overlayButton = Button(this).apply {
            text = "START"
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)

            var initialX = 0
            var initialY = 0
            var touchX = 0f
            var touchY = 0f

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(this, params)
                        true
                    }
                    else -> false
                }
            }

            setOnClickListener {
                isAutoRunning = !isAutoRunning
                updateButtonAppearance()
                if (isAutoRunning) runBotLoop()
            }
        }

        windowManager.addView(overlayButton, params)
        startStateSyncLoop()
    }

    private fun updateButtonAppearance() {
        if (isAutoRunning) {
            overlayButton.text = "STOP"
            overlayButton.setBackgroundColor(Color.parseColor("#C62828"))
        } else {
            overlayButton.text = "START"
            overlayButton.setBackgroundColor(Color.parseColor("#2E7D32"))
        }
    }

    private fun startStateSyncLoop() {
        updateButtonAppearance()
        if (isAutoRunning && !isCurrentlyTyping) {
            runBotLoop()
        }
        handler.postDelayed({ startStateSyncLoop() }, 300)
    }

    private fun runBotLoop() {
        if (!isAutoRunning || isCurrentlyTyping) return
        val rootNode = rootInActiveWindow ?: return
        solveScreen(rootNode)
    }

    private fun solveScreen(rootNode: AccessibilityNodeInfo) {
        val allTextNodes = mutableListOf<String>()
        var inputNode: AccessibilityNodeInfo? = null
        var submitButton: AccessibilityNodeInfo? = null

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val cls = node.className?.toString() ?: ""
            if (cls.contains("EditText")) {
                inputNode = node
            } else if (node.text != null) {
                val t = node.text.toString().trim()
                if (t.isNotEmpty()) allTextNodes.add(t)
                if (t.equals("Check", ignoreCase = true) ||
                    t.equals("Submit", ignoreCase = true) ||
                    t.equals("Ověřit", ignoreCase = true) ||
                    t.equals("Overiť", ignoreCase = true)
                ) {
                    submitButton = node
                }
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(rootNode)

        val targetInput = inputNode ?: return
        for (screenText in allTextNodes) {
            val cleanPrompt = screenText.lowercase()
            val answer = dictionary[cleanPrompt]
                ?: dictionary.entries.firstOrNull { it.value.lowercase() == cleanPrompt }?.key

            if (answer != null) {
                typeFast(targetInput, answer, submitButton)
                break
            }
        }
    }

    private fun typeFast(
        inputNode: AccessibilityNodeInfo,
        word: String,
        submitButton: AccessibilityNodeInfo?
    ) {
        isCurrentlyTyping = true
        inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        for (i in 1..word.length) {
            val partial = word.substring(0, i)
            handler.postDelayed({
                val args = Bundle()
                args.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    partial
                )
                inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

                if (i == word.length) {
                    submitButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    handler.postDelayed({ isCurrentlyTyping = false }, 250)
                }
            }, (i * 25).toLong())
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
