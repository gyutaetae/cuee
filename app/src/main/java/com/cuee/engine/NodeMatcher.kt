package com.cuee.engine

import android.graphics.Rect
import kotlin.math.abs

class NodeMatcher(private val safetyGuard: SafetyGuard) {
    private val keywords = mapOf(
        TargetType.SEARCH_FIELD to listOf("검색", "상품명", "무엇을 찾고", "Search"),
        TargetType.SEARCH_BUTTON to listOf("검색", "돋보기", "Search", "완료"),
        TargetType.CART_BUTTON to listOf("장바구니", "카트", "Cart"),
        TargetType.MESSAGE_INPUT to listOf("메시지", "입력", "채팅", "Aa"),
        TargetType.PHOTO_BUTTON to listOf("사진", "앨범", "이미지", "카메라", "미디어"),
        TargetType.MY_TICKET_BUTTON to listOf("승차권 확인", "예매내역", "나의 승차권", "표 확인", "승차권"),
        TargetType.TICKET_RESERVATION_BUTTON to listOf("승차권 예매", "예매", "기차표 예매")
    )

    fun findCandidates(
        targetType: TargetType,
        context: ScreenContext,
        nodes: List<ScreenNode>,
        appSpec: AppCapabilitySpec?,
        screenBounds: Rect
    ): CandidateResult {
        val cleanNodes = nodes
            .asSequence()
            .filter { it.enabled }
            .filter { it.bounds.width() >= dpLike(24) && it.bounds.height() >= dpLike(24) }
            .filter { Rect.intersects(it.bounds, screenBounds) }
            .filterNot { safetyGuard.isBlockedNode(it, appSpec) }
            .toList()

        val scored = cleanNodes.mapNotNull { node ->
            val score = scoreNode(targetType, context, node, screenBounds)
            if (score >= 60f) {
                TargetCandidate(node.id, targetType, Rect(node.bounds), score, node.label)
            } else {
                null
            }
        }.sortedByDescending { it.score }

        val strong = scored.filter { it.score >= 75f }
        if (strong.isEmpty()) {
            return if (cleanNodes.any { it.scrollable || it.actions.contains(NodeAction.SCROLL_FORWARD) }) {
                CandidateResult.ScrollPossible
            } else {
                CandidateResult.NotFound
            }
        }

        if (strong.size <= 3) return CandidateResult.Found(mergeNearby(strong, screenBounds))

        val top = strong.take(4)
        if (top[2].score - top[3].score >= 10f) {
            return CandidateResult.Found(mergeNearby(top.take(3), screenBounds))
        }
        return CandidateResult.TooMany
    }

    private fun scoreNode(targetType: TargetType, context: ScreenContext, node: ScreenNode, screen: Rect): Float {
        var score = 0f
        val label = node.label
        val targetKeywords = keywords[targetType].orEmpty()
        if (targetKeywords.any { label.equals(it, ignoreCase = true) }) score += 60f
        if (targetKeywords.any { label.contains(it, ignoreCase = true) }) score += 35f
        if (targetKeywords.any { node.contentDescription?.contains(it, ignoreCase = true) == true }) score += 45f
        if (node.editable && (targetType == TargetType.SEARCH_FIELD || targetType == TargetType.MESSAGE_INPUT)) score += 50f
        if (node.clickable && targetType != TargetType.SEARCH_FIELD && targetType != TargetType.MESSAGE_INPUT) score += 25f
        if (node.className?.contains("EditText", ignoreCase = true) == true) score += 30f
        if (node.className?.let { it.contains("Button", true) || it.contains("TextView", true) } == true) score += 10f
        if (node.actions.contains(NodeAction.SET_TEXT) && (targetType == TargetType.SEARCH_FIELD || targetType == TargetType.MESSAGE_INPUT)) score += 20f
        score += positionHint(targetType, context, node.bounds, screen)
        if (label.length > 40) score -= 30f
        if (label in listOf("확인", "취소", "닫기", "더보기")) score -= 20f
        return score
    }

    private fun positionHint(targetType: TargetType, context: ScreenContext, bounds: Rect, screen: Rect): Float {
        val yRatio = bounds.centerY().toFloat() / screen.height().coerceAtLeast(1)
        val xRatio = bounds.centerX().toFloat() / screen.width().coerceAtLeast(1)
        return when (targetType) {
            TargetType.SEARCH_FIELD -> if (yRatio < 0.30f) 10f else 0f
            TargetType.SEARCH_BUTTON -> if (context == ScreenContext.COUPANG_SEARCH_INPUT || yRatio < 0.35f || xRatio > 0.70f) 10f else 0f
            TargetType.MESSAGE_INPUT -> if (yRatio > 0.65f) 10f else 0f
            TargetType.PHOTO_BUTTON -> if (yRatio > 0.55f) 10f else 0f
            TargetType.CART_BUTTON -> if (yRatio < 0.25f || yRatio > 0.75f) 8f else 0f
            else -> 0f
        }
    }

    private fun mergeNearby(candidates: List<TargetCandidate>, screen: Rect): List<TargetCandidate> {
        val result = mutableListOf<TargetCandidate>()
        candidates.forEach { candidate ->
            val existingIndex = result.indexOfFirst { nearby(it.bounds, candidate.bounds) }
            if (existingIndex >= 0) {
                val existing = result[existingIndex]
                val union = Rect(existing.bounds)
                union.union(candidate.bounds)
                val merged = existing.copy(bounds = union, score = maxOf(existing.score, candidate.score))
                result[existingIndex] = merged
            } else {
                result += candidate
            }
        }
        return result.filter { it.bounds.width() * it.bounds.height() < screen.width() * screen.height() * 0.35f }
    }

    private fun nearby(a: Rect, b: Rect): Boolean {
        val horizontalGap = maxOf(0, maxOf(a.left, b.left) - minOf(a.right, b.right))
        val verticalGap = maxOf(0, maxOf(a.top, b.top) - minOf(a.bottom, b.bottom))
        return abs(horizontalGap) <= dpLike(12) && abs(verticalGap) <= dpLike(12)
    }

    private fun dpLike(value: Int): Int = value
}

sealed interface CandidateResult {
    data class Found(val candidates: List<TargetCandidate>) : CandidateResult
    data object ScrollPossible : CandidateResult
    data object TooMany : CandidateResult
    data object NotFound : CandidateResult
}

