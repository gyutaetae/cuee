package com.cuee.overlay

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class ScrollHintController(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var view: LinearLayout? = null

    fun show() {
        hide()
        val screen = windowManager.screenBounds()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = 0.92f
            setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(12))
            background = rounded(0xDDFFFFFF.toInt(), context.dp(16))
        }
        layout.addView(TextView(context).apply {
            text = "↓"
            textSize = 56f
            setTextColor(CUE_GREEN)
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(context).apply {
            text = "아래로 밀어주세요."
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        })
        val lp = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            x = context.dp(36),
            y = (screen.height() * 0.40f).toInt(),
            touchable = false
        )
        view = layout
        windowManager.addView(layout, lp)
    }

    fun hide() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
    }
}

