package com.cuee.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.cuee.engine.TargetCandidate

class MaskOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onBack: () -> Unit
) {
    private val maskViews = mutableListOf<View>()
    private var backView: TextView? = null

    fun show(candidates: List<TargetCandidate>) {
        hide()
        val screen = windowManager.screenBounds()
        val holes = candidates
            .map { padded(it.bounds, context.dp(6), screen) }
            .let { mergeCloseRects(it) }
            .filter { it.width() * it.height() < screen.width() * screen.height() * 0.35f }

        val blocks = buildBlocks(screen, holes)
        blocks.forEach { rect ->
            if (rect.width() <= 0 || rect.height() <= 0) return@forEach
            val block = View(context).apply { setBackgroundColor(Color.WHITE) }
            val lp = overlayParams(rect.width(), rect.height(), rect.left, rect.top, touchable = true)
            maskViews += block
            windowManager.addView(block, lp)
        }
        showBack()
    }

    fun hide() {
        maskViews.forEach { runCatching { windowManager.removeView(it) } }
        maskViews.clear()
        backView?.let { runCatching { windowManager.removeView(it) } }
        backView = null
    }

    private fun showBack() {
        val size = context.dp(52)
        val view = TextView(context).apply {
            text = "<"
            textSize = 34f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = circle(Color.WHITE)
            elevation = context.dp(12).toFloat()
            setOnClickListener { onBack() }
        }
        val lp = overlayParams(size, size, context.dp(12), context.dp(20), touchable = true)
        backView = view
        windowManager.addView(view, lp)
    }

    private fun buildBlocks(screen: Rect, holes: List<Rect>): List<Rect> {
        if (holes.isEmpty()) return listOf(Rect(screen))
        val xs = mutableSetOf(screen.left, screen.right)
        val ys = mutableSetOf(screen.top, screen.bottom)
        holes.forEach { hole ->
            xs += hole.left.coerceIn(screen.left, screen.right)
            xs += hole.right.coerceIn(screen.left, screen.right)
            ys += hole.top.coerceIn(screen.top, screen.bottom)
            ys += hole.bottom.coerceIn(screen.top, screen.bottom)
        }
        val sortedX = xs.sorted()
        val sortedY = ys.sorted()
        val result = mutableListOf<Rect>()
        for (yi in 0 until sortedY.lastIndex) {
            for (xi in 0 until sortedX.lastIndex) {
                val rect = Rect(sortedX[xi], sortedY[yi], sortedX[xi + 1], sortedY[yi + 1])
                if (holes.none { Rect.intersects(rect, it) }) {
                    result += rect
                }
            }
        }
        return result
    }

    private fun padded(rect: Rect, padding: Int, screen: Rect): Rect = Rect(
        (rect.left - padding).coerceAtLeast(screen.left),
        (rect.top - padding).coerceAtLeast(screen.top),
        (rect.right + padding).coerceAtMost(screen.right),
        (rect.bottom + padding).coerceAtMost(screen.bottom)
    )

    private fun mergeCloseRects(rects: List<Rect>): List<Rect> {
        val merged = mutableListOf<Rect>()
        rects.forEach { rect ->
            val index = merged.indexOfFirst { close(it, rect, context.dp(12)) }
            if (index >= 0) {
                merged[index].union(rect)
            } else {
                merged += Rect(rect)
            }
        }
        return merged
    }

    private fun close(a: Rect, b: Rect, distance: Int): Boolean {
        val horizontalGap = maxOf(0, maxOf(a.left, b.left) - minOf(a.right, b.right))
        val verticalGap = maxOf(0, maxOf(a.top, b.top) - minOf(a.bottom, b.bottom))
        return horizontalGap <= distance && verticalGap <= distance
    }
}

