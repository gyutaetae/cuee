package com.cuee.engine

class ScreenContextDetector {
    fun detect(app: SupportedApp?, nodes: List<ScreenNode>, sensitive: Boolean): ScreenContext {
        if (sensitive) return ScreenContext.SENSITIVE
        return when (app) {
            SupportedApp.COUPANG -> detectCoupang(nodes)
            SupportedApp.KAKAO -> detectKakao(nodes)
            SupportedApp.KORAIL -> detectKorail(nodes)
            null -> ScreenContext.UNKNOWN
        }
    }

    private fun detectCoupang(nodes: List<ScreenNode>): ScreenContext {
        val hasEditable = nodes.any { it.editable }
        val hasKeyboard = nodes.any { it.packageName?.contains("inputmethod", true) == true || it.packageName?.contains("honeyboard", true) == true }
        if (hasEditable && hasKeyboard) return ScreenContext.COUPANG_SEARCH_INPUT
        return ScreenContext.COUPANG_HOME
    }

    private fun detectKakao(nodes: List<ScreenNode>): ScreenContext {
        val screenBottom = nodes.maxOfOrNull { it.bounds.bottom }?.coerceAtLeast(1) ?: 1
        val hasBottomEditable = nodes.any { it.editable && it.bounds.centerY() > screenBottom * 0.60f }
        val hasMessageWords = nodes.any { node ->
            val label = node.label
            label.contains("메시지") || label.contains("입력") || label.contains("채팅") || label.contains("Aa")
        }
        return if (hasBottomEditable || hasMessageWords) ScreenContext.KAKAO_CHAT_ROOM else ScreenContext.KAKAO_CHAT_LIST
    }

    private fun detectKorail(nodes: List<ScreenNode>): ScreenContext {
        return if (nodes.any { it.label.contains("승차권") || it.label.contains("예매") }) {
            ScreenContext.KORAIL_HOME
        } else {
            ScreenContext.UNKNOWN
        }
    }
}
