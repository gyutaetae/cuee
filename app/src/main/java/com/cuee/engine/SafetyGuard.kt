package com.cuee.engine

class SafetyGuard {
    private val commonBlocked = listOf(
        "결제", "결제하기", "주문하기", "구매확정", "바로구매", "송금", "이체",
        "출금", "카드 등록", "계좌 등록", "주문 확정", "보내기"
    )

    private val commonSensitive = listOf(
        "개인정보", "주소", "연락처", "전화번호", "배송지", "카드", "계좌",
        "비밀번호", "인증번호", "주민등록번호", "카드번호", "계좌번호", "보안카드"
    )

    fun isBlockedNode(node: ScreenNode, appSpec: AppCapabilitySpec?): Boolean {
        val label = node.label
        val blocked = commonBlocked + (appSpec?.blockedKeywords ?: emptyList())
        return blocked.any { label.contains(it, ignoreCase = true) }
    }

    fun isSensitiveScreen(nodes: List<ScreenNode>, appSpec: AppCapabilitySpec?): Boolean {
        val sensitive = commonSensitive + (appSpec?.sensitiveKeywords ?: emptyList())
        val joined = nodes.asSequence().map { it.label }.filter { it.isNotBlank() }.take(80).joinToString(" ")
        return sensitive.any { joined.contains(it, ignoreCase = true) }
    }

    fun canAutoInput(policy: AutoInputPolicy, text: String?): Boolean {
        if (policy == AutoInputPolicy.NONE || text.isNullOrBlank()) return false
        val risky = commonSensitive + listOf("결제", "송금", "이체", "주문", "인증")
        return risky.none { text.contains(it, ignoreCase = true) }
    }
}

