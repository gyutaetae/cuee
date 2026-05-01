package com.cuee.engine

import android.graphics.Rect

enum class SupportedApp {
    COUPANG,
    KAKAO,
    KORAIL
}

enum class ActionId {
    COUPANG_SEARCH,
    COUPANG_CART,
    KAKAO_COMPOSE_MESSAGE,
    KAKAO_PHOTO_ENTRY,
    KORAIL_MY_TICKET,
    KORAIL_TICKET_ENTRY,
    OPEN_APP
}

enum class TargetType {
    SEARCH_FIELD,
    SEARCH_BUTTON,
    CART_BUTTON,
    MESSAGE_INPUT,
    PHOTO_BUTTON,
    MY_TICKET_BUTTON,
    TICKET_RESERVATION_BUTTON,
    APP_ICON_OR_LAUNCH
}

enum class AutoInputPolicy {
    NONE,
    SEARCH_QUERY_ONLY,
    KAKAO_MESSAGE_ONLY
}

enum class StopPolicy {
    STOP_AFTER_TARGET_CLICK,
    STOP_AFTER_AUTO_INPUT,
    STOP_AT_SEARCH_RESULTS,
    STOP_AT_SENSITIVE_SCREEN,
    MAX_5_STEPS
}

enum class ScreenContext {
    UNKNOWN,
    COUPANG_HOME,
    COUPANG_SEARCH_INPUT,
    KAKAO_CHAT_LIST,
    KAKAO_CHAT_ROOM,
    KORAIL_HOME,
    SENSITIVE
}

enum class NodeAction {
    CLICK,
    SET_TEXT,
    SCROLL_FORWARD,
    SCROLL_BACKWARD
}

enum class FailureReason {
    SPEECH_NOT_RECOGNIZED,
    UNSUPPORTED_APP,
    UNSUPPORTED_REQUEST,
    TARGET_NOT_FOUND,
    TOO_MANY_CANDIDATES,
    AUTO_INPUT_FAILED,
    APP_NOT_INSTALLED
}

data class ActionSpec(
    val id: ActionId,
    val app: SupportedApp,
    val triggerKeywords: List<String>,
    val targetSequence: List<TargetType>,
    val autoInputPolicy: AutoInputPolicy,
    val stopPolicy: StopPolicy,
    val maxSteps: Int = 5
)

data class AppCapabilitySpec(
    val app: SupportedApp,
    val packageNames: List<String>,
    val displayName: String,
    val supportedActions: List<ActionSpec>,
    val blockedKeywords: List<String>,
    val sensitiveKeywords: List<String>
)

data class ParsedCommand(
    val rawText: String,
    val actionId: ActionId?,
    val app: SupportedApp?,
    val extractedText: String?,
    val confidence: Float
)

data class ScreenNode(
    val id: String,
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val packageName: String?,
    val bounds: Rect,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
    val enabled: Boolean,
    val actions: Set<NodeAction>
) {
    val label: String
        get() = listOfNotNull(text, contentDescription).joinToString(" ").trim()
}

data class TargetCandidate(
    val nodeId: String,
    val targetType: TargetType,
    val bounds: Rect,
    val score: Float,
    val reason: String
)

data class RuntimeSession(
    val originalCommand: String,
    val app: SupportedApp,
    val actionId: ActionId,
    val extractedText: String?,
    var stepCount: Int = 0,
    var scrollAttempts: Int = 0,
    var autoInputAttempts: Int = 0,
    var pendingTarget: TargetType? = null,
    var waitingForChatRoom: Boolean = false,
    val startedAt: Long = System.currentTimeMillis()
)

sealed interface CueState {
    data object Idle : CueState
    data object Listening : CueState
    data object Thinking : CueState
    data class Guiding(val candidates: List<TargetCandidate>) : CueState
    data object ScrollHint : CueState
    data class Failed(val reason: FailureReason) : CueState
    data object SensitivePause : CueState
}
