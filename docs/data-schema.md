# 데이터 스키마: KorailTalk MVP

## 원칙

- 서버, 로그인, Room DB 없음.
- 저장은 설정값과 익명 UT metric만 허용.
- 화면 텍스트, 음성 원문, 접근성 node dump, 승차권/개인/결제/인증 정보 저장 금지.
- 접근성 화면 스냅샷과 세션 상태는 메모리 전용이며 종료 시 폐기.

## DataStore: UserSettings

```kotlin
data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val accessibilityGuideCompleted: Boolean = false,
    val consentVersion: String = "2026-05-20",
    val consentAcceptedAt: Long = 0L,
    val bubbleEnabled: Boolean = true,
    val bubbleEdge: BubbleEdge = BubbleEdge.RIGHT,
    val bubbleYRatio: Float = 0.55f,
    val voiceEnabled: Boolean = true
)

enum class BubbleEdge { LEFT, RIGHT }
```

DataStore keys:

```text
onboarding_completed
accessibility_guide_completed
consent_version
consent_accepted_at
bubble_enabled
bubble_edge
bubble_y_ratio
voice_enabled
```

## Commands

```kotlin
enum class KorailCommand {
    SHOW_MY_TICKET,
    FIND_RESERVATION_START
}
```

- `SHOW_MY_TICKET`: "예매한 표 보여줘", "내 표 보여줘", "표 확인"
- `FIND_RESERVATION_START`: "승차권 예매 찾기", "기차표 예매", "예매 시작"

외국어 명령은 MVP 구현 범위가 아니다.

## Runtime Models

메모리 전용. 저장 금지.

```kotlin
data class ScreenSnapshot(
    val packageName: String,
    val nodes: List<ScreenNode>,
    val capturedAt: Long
)

data class ScreenNode(
    val id: String,
    val text: String?,
    val contentDescription: String?,
    val className: String?,
    val packageName: String?,
    val bounds: Bounds,
    val clickable: Boolean,
    val enabled: Boolean,
    val visible: Boolean,
    val scrollable: Boolean,
    val editable: Boolean,
    val depth: Int,
    val parentHint: String?
)

data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)
```

`text`, `contentDescription`, `parentHint`는 scoring 중에만 사용하고 로그/파일/DataStore에 남기지 않는다.

## Safety

```kotlin
data class SafetyDecision(
    val allowed: Boolean,
    val reason: StopReason?,
    val matchedPolicy: SafetyPolicyId?
)

enum class StopReason {
    NOT_KORAIL_APP,
    SENSITIVE_LOGIN,
    SENSITIVE_PERSONAL_INFO,
    SENSITIVE_PAYMENT,
    SENSITIVE_AUTH_CODE,
    SENSITIVE_CONFIRMATION,
    LOW_CONFIDENCE,
    OVERLAPPING_CANDIDATES,
    NO_TARGET,
    USER_CANCELLED,
    MAX_STEPS_REACHED
}

enum class SafetyPolicyId {
    LOGIN,
    PERSONAL_INFO,
    PAYMENT,
    AUTH_CODE,
    RESERVATION_CONFIRMATION
}
```

민감 화면에서는 안내하지 않고 "이 화면은 직접 확인해 주세요"로 멈춘다.

## Candidate / Overlay

```kotlin
data class TargetCandidate(
    val nodeId: String,
    val command: KorailCommand,
    val targetType: TargetType,
    val bounds: Bounds,
    val score: Int,
    val evidence: CandidateEvidence
)

enum class TargetType {
    MY_TICKET_ENTRY,
    RESERVATION_ENTRY,
    NEXT_STEP_ENTRY
}

data class CandidateEvidence(
    val textMatched: Boolean,
    val contentDescriptionMatched: Boolean,
    val clickable: Boolean,
    val classHintMatched: Boolean,
    val positionHintMatched: Boolean
)

data class OverlayInstruction(
    val visibleHoles: List<Bounds>,
    val highlightedBounds: List<Bounds>,
    val timeoutMs: Long = 12_000L,
    val spokenPrompt: SpokenPrompt
)

enum class SpokenPrompt { PLEASE_TAP, TRY_AGAIN, CHECK_DIRECTLY }
```

Candidate policy:

- 80점 이상 strong, 65-79점 weak, 65점 미만 제외.
- 겹치는 후보는 cluster 후 최고 score만 유지.
- 최대 3개 후보만 표시.
- 겹침을 안전하게 해소하지 못하면 실패 처리.

## Guide Session

메모리 전용.

```kotlin
data class GuideSessionState(
    val command: KorailCommand,
    val state: GuideState,
    val stepCount: Int,
    val startedAt: Long,
    val lastStepAt: Long
)

enum class GuideState {
    IDLE,
    LISTENING,
    THINKING,
    GUIDING,
    SENSITIVE_PAUSE,
    FAILED
}
```

- 최대 3-step.
- 매 step마다 package, safety, target score 재검사.
- 정확도가 낮으면 완료시간보다 안전을 우선해 중단.

## UT Metric

원문 없는 로컬 익명 metric.

```kotlin
data class UtMetric(
    val sessionId: String,
    val taskType: UtTaskType,
    val startedAt: Long,
    val finishedAt: Long?,
    val elapsedMs: Long?,
    val result: UtResult,
    val stopReason: StopReason?,
    val stepCount: Int
)

enum class UtTaskType { SHOW_MY_TICKET, FIND_RESERVATION_START }
enum class UtResult { SUCCESS, FAILED, SENSITIVE_PAUSE, CANCELLED }
```

측정 목표:

- 완료시간 50% 감소: `elapsedMs` 전/후 비교
- 실패율 30%p 감소: `SUCCESS` 비율 비교
- 도움 요청 70% 감소: 앱 저장값이 아니라 UT 관찰자가 기록
