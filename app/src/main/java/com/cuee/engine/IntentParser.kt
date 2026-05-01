package com.cuee.engine

class IntentParser {
    fun parse(raw: String, currentApp: SupportedApp?): ParsedCommand {
        val text = raw.trim()
        if (text.isBlank()) {
            return ParsedCommand(raw, null, currentApp, null, 0f)
        }

        parseOpenApp(text)?.let { return it }

        if (currentApp == SupportedApp.COUPANG || text.contains("쿠팡")) {
            if (containsAny(text, "장바구니", "카트", "담은 거")) {
                return ParsedCommand(raw, ActionId.COUPANG_CART, SupportedApp.COUPANG, null, 0.95f)
            }
            if (containsAny(text, "찾아줘", "검색", "사고 싶어", "사려고", "필요해", "주문하고 싶어")) {
                val query = TextExtractor.extractSearchQuery(text)
                return ParsedCommand(raw, ActionId.COUPANG_SEARCH, SupportedApp.COUPANG, query.ifBlank { null }, 0.9f)
            }
        }

        if (currentApp == SupportedApp.KAKAO || containsAny(text, "카톡", "카카오톡")) {
            if (containsAny(text, "사진", "앨범", "이미지", "카메라")) {
                return ParsedCommand(raw, ActionId.KAKAO_PHOTO_ENTRY, SupportedApp.KAKAO, null, 0.86f)
            }
            if (containsAny(text, "한테", "에게", "보내", "말해", "메시지", "문자")) {
                val message = TextExtractor.extractKakaoMessage(text)
                return ParsedCommand(raw, ActionId.KAKAO_COMPOSE_MESSAGE, SupportedApp.KAKAO, message.ifBlank { null }, 0.86f)
            }
        }

        if (currentApp == SupportedApp.KORAIL || containsAny(text, "코레일", "기차", "승차권", "표")) {
            if (containsAny(text, "예매한 표", "표 확인", "승차권 확인", "기차표 확인", "예매내역")) {
                return ParsedCommand(raw, ActionId.KORAIL_MY_TICKET, SupportedApp.KORAIL, null, 0.92f)
            }
            if (containsAny(text, "승차권 예매", "기차 예매", "표 예매", "예매하고")) {
                return ParsedCommand(raw, ActionId.KORAIL_TICKET_ENTRY, SupportedApp.KORAIL, null, 0.9f)
            }
        }

        return ParsedCommand(raw, null, currentApp, null, 0f)
    }

    private fun parseOpenApp(text: String): ParsedCommand? {
        if (!containsAny(text, "열어줘", "열어", "켜줘", "켜")) return null
        val app = when {
            text.contains("쿠팡") -> SupportedApp.COUPANG
            text.contains("카카오톡") || text.contains("카톡") -> SupportedApp.KAKAO
            text.contains("코레일") || text.contains("기차") -> SupportedApp.KORAIL
            else -> null
        } ?: return null
        return ParsedCommand(text, ActionId.OPEN_APP, app, null, 0.95f)
    }

    private fun containsAny(text: String, vararg needles: String): Boolean = needles.any { text.contains(it) }
}

