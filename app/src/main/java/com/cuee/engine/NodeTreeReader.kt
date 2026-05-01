package com.cuee.engine

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

class NodeTreeReader {
    fun read(root: AccessibilityNodeInfo?): List<ScreenNode> {
        if (root == null) return emptyList()
        val nodes = mutableListOf<ScreenNode>()
        traverse(root, "0", nodes)
        return nodes
    }

    fun findFocusedEditable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused?.isEditable == true) return focused
        return findFirstEditable(root)
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditable(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun traverse(node: AccessibilityNodeInfo, id: String, out: MutableList<ScreenNode>) {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        out += ScreenNode(
            id = node.viewIdResourceName ?: id,
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            className = node.className?.toString(),
            packageName = node.packageName?.toString(),
            bounds = rect,
            clickable = node.isClickable,
            editable = node.isEditable,
            scrollable = node.isScrollable,
            enabled = node.isEnabled,
            actions = node.actionList.mapNotNull { action ->
                when (action.id) {
                    AccessibilityNodeInfo.ACTION_CLICK -> NodeAction.CLICK
                    AccessibilityNodeInfo.ACTION_SET_TEXT -> NodeAction.SET_TEXT
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> NodeAction.SCROLL_FORWARD
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> NodeAction.SCROLL_BACKWARD
                    else -> null
                }
            }.toSet()
        )
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverse(child, "$id.$i", out)
            child.recycle()
        }
    }
}

