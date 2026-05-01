package com.cuee.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class SpeechPanelController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onRetry: () -> Unit
) {
    private var panel: LinearLayout? = null

    fun showListening() {
        showPanel("무슨 도움이 필요한지 말해보세요.", retry = false)
    }

    fun showThinking() {
        showPanel("찾고 있어요.", retry = false)
    }

    fun showFailure(message: String) {
        showPanel(message, retry = true)
    }

    fun hide() {
        panel?.let { runCatching { windowManager.removeView(it) } }
        panel = null
    }

    private fun showPanel(message: String, retry: Boolean) {
        hide()
        val screen = windowManager.screenBounds()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dp(18), context.dp(14), context.dp(18), context.dp(14))
            background = rounded(Color.WHITE, context.dp(14))
            elevation = context.dp(10).toFloat()
        }
        layout.addView(TextView(context).apply {
            text = message
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        })
        if (retry) {
            layout.addView(TextView(context).apply {
                text = "다시 말하기"
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(context.dp(20), context.dp(10), context.dp(20), context.dp(10))
                background = rounded(CUE_GREEN, context.dp(8))
                setOnClickListener { onRetry() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(12)
            })
        }
        val lp = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            x = context.dp(20),
            y = (screen.height() * 0.68f).toInt()
        )
        panel = layout
        windowManager.addView(layout, lp)
    }
}

