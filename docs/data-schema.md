# 데이터 스키마

## 저장 원칙
- 서버 없음.
- 로그인 없음.
- Room DB 없음.
- 릴리즈 빌드에서 화면/음성/명령 로그 저장 없음.
- 사용자 설정만 `Preferences DataStore`에 저장한다.
- 앱별 지원 사전은 `assets/*.json`으로 번들한다.
- 요청 세션은 메모리에만 유지하고 종료 시 폐기한다.

## UserSettings
`DataStore` 저장 대상.

```kotlin
data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val tutorialCompleted: Boolean = false,
    val consentVersion: Int = 1,
    val consentAcceptedAt: Long = 0L,
    val bubbleEnabled: Boolean = true,
    val bubbleSide: BubbleSide = BubbleSide.RIGHT,
    val bubbleYRatio: Float = 0.55f,
    val voiceGuideEnabled: Boolean = true,
    val ttsRate: Float = 1.0f
)

enum class BubbleSide { LEFT, RIGHT }
```

## AppCapabilitySpec
앱별 얇은 사전. 좌표를 저장하지 않는다.

```kotlin
data class AppCapabilitySpec(
    val app: SupportedApp,
    val packageNames: List<String>,
    val displayName: String,
    val supportedActions: List<ActionSpec>,
    val blockedKeywords: List<String>,
    val sensitiveKeywords: List<String>,
    val contextHints: List<ScreenContextHint>
)
```

## ActionSpec
```kotlin
data class ActionSpec(
    val id: ActionId,
    val app: SupportedApp,
    val triggerKeywords: List<String>,
    val targetSequence: List<TargetType>,
    val autoInputPolicy: AutoInputPolicy,
    val stopPolicy: StopPolicy,
    val maxSteps: Int = 5
)
```

## Enums
```kotlin
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
```

## RuntimeSession
메모리 전용. 저장 금지.

```kotlin
data class RuntimeSession(
    val originalCommand: String,
    val app: SupportedApp,
    val actionId: ActionId,
    val extractedText: String?,
    val stepCount: Int = 0,
    val scrollAttempts: Int = 0,
    val startedAt: Long = System.currentTimeMillis()
)
```

## ParsedCommand
```kotlin
data class ParsedCommand(
    val rawText: String,
    val actionId: ActionId?,
    val app: SupportedApp?,
    val extractedText: String?,
    val confidence: Float
)
```

## ScreenNode
Android `AccessibilityNodeInfo`를 앱 내부 경량 모델로 변환한 값. Android 객체를 장시간 보관하지 않는다.

```kotlin
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
)

enum class NodeAction {
    CLICK,
    SET_TEXT,
    SCROLL_FORWARD,
    SCROLL_BACKWARD
}
```

## TargetCandidate
```kotlin
data class TargetCandidate(
    val nodeId: String,
    val targetType: TargetType,
    val bounds: Rect,
    val score: Float,
    val reason: String
)
```

## ScreenContext
상태 기반 탐색을 위해 사용한다. 너무 세분화하지 않는다.

```kotlin
enum class ScreenContext {
    UNKNOWN,
    COUPANG_HOME,
    COUPANG_SEARCH_INPUT,
    KAKAO_CHAT_LIST,
    KAKAO_CHAT_ROOM,
    KORAIL_HOME,
    SENSITIVE
}
```

## 앱별 JSON 예시
```json
{
  "app": "COUPANG",
  "packageNames": ["com.coupang.mobile"],
  "displayName": "쿠팡",
  "supportedActions": [
    {
      "id": "COUPANG_SEARCH",
      "triggerKeywords": ["찾아줘", "검색", "사고 싶어", "주문하고 싶어"],
      "targetSequence": ["SEARCH_FIELD", "SEARCH_BUTTON"],
      "autoInputPolicy": "SEARCH_QUERY_ONLY",
      "stopPolicy": "STOP_AFTER_TARGET_CLICK",
      "maxSteps": 5
    },
    {
      "id": "COUPANG_CART",
      "triggerKeywords": ["장바구니", "카트", "담은 거"],
      "targetSequence": ["CART_BUTTON"],
      "autoInputPolicy": "NONE",
      "stopPolicy": "STOP_AFTER_TARGET_CLICK",
      "maxSteps": 1
    }
  ],
  "blockedKeywords": ["결제", "주문하기", "구매확정", "바로구매"],
  "sensitiveKeywords": ["카드", "계좌", "비밀번호", "인증번호", "주소", "전화번호"]
}
```

