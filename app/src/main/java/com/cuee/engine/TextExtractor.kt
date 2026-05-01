package com.cuee.engine

object TextExtractor {
    private val searchFillers = listOf(
        "쿠팡에서", "쿠팡", "찾아줘", "검색해줘", "검색", "사고 싶어", "사려고",
        "필요해", "주문하고 싶어", "좀", "하나", "주세요", "줘"
    )

    private val messageEndings = listOf(
        "보내고 싶어", "보내줘", "보내", "말해줘", "말해", "라고", "이라고",
        "카톡해줘", "카톡", "메시지", "문자"
    )

    fun extractSearchQuery(raw: String): String {
        var result = raw.clean()
        searchFillers.forEach { result = result.replace(it, " ") }
        return result.clean()
    }

    fun extractKakaoMessage(raw: String): String {
        var result = raw.clean()
        val receiverMarkers = listOf("한테", "에게")
        val marker = receiverMarkers.firstOrNull { result.contains(it) }
        if (marker != null) {
            result = result.substringAfter(marker)
        }
        messageEndings.forEach { result = result.replace(it, " ") }
        return result.clean()
    }

    fun String.clean(): String = replace(Regex("\\s+"), " ").trim()
}

