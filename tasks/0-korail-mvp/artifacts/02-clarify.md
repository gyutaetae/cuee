# Clarify Summary: korail-mvp

## 1. Feasibility

- 외국어 음성 명령 인식은 Android `SpeechRecognizer` locale 전환과 다국어 command parser로 구현 가능하다.
- 하지만 빠르고 정확한 MVP를 위해 첫 구현은 한국어 코레일톡 명령 2개로 제한한다.
- 외국인은 발표 스토리와 UT 타깃에 포함한다. 핵심 가설은 "버튼 이름을 몰라도 마스킹으로 남겨진 영역을 누르면 된다"이다.

## 2. Tech Stack

- Android Native Kotlin으로 구현한다.
- 핵심 기술은 `AccessibilityService`, `AccessibilityNodeInfo`, `WindowManager.TYPE_ACCESSIBILITY_OVERLAY`, `SpeechRecognizer`, `TextToSpeech`, DataStore Preferences다.
- MVP는 실제 코레일톡 위 접근성 오버레이에 100% 집중한다.
- 발표용 시뮬레이션 화면은 만들지 않는다. 실제 앱 위 동작이 매끄럽지 않을 때만 fallback으로 추가한다.
- 발표 리스크는 실제 기기 녹화 영상과 수동 시연 체크리스트로 줄인다.

## 3. User Flow

- 사용자가 코레일톡을 직접 연다.
- 화면 가장자리에 cue bubble이 표시된다.
- 사용자가 cue bubble을 누르면 큐가 "무엇을 도와드릴까요?"라고 말하고 한국어 음성 인식을 시작한다.
- 사용자는 "예매한 표 보여줘" 또는 "승차권 예매 찾기"라고 말한다.
- 큐는 현재 코레일톡 화면의 접근성 노드를 분석하고, 후보가 명확하면 눌러야 할 영역만 남기고 흰색 마스킹을 표시한다.
- 사용자는 노출된 영역을 직접 누른다.
- MVP에는 최대 3-step 연속 안내를 넣는다.
- 연속 안내는 매 step마다 화면 package, 민감 키워드, 후보 score를 다시 검사한다.
- 후보가 불명확하거나 score가 낮으면 안내를 멈추고 실패 상태를 보여준다.
- 로그인, 개인정보, 결제, 인증, 예매 확정, 구매 확정 화면이면 즉시 멈추고 "이 화면은 직접 확인해 주세요"라고 안내한다.
- 빠른 완료보다 정확도를 우선한다. 정확도가 낮다고 판단되는 흐름은 연속 안내 대상에서 제외한다.

## 4. UI Design

- AI API key는 MVP 필수 조건이 아니다. 코레일톡 단일 MVP는 접근성 노드의 text, contentDescription, className, bounds, clickability, packageName을 분석해 deterministic matching으로 구현한다.
- AI API는 범용 화면 이해와 자연어 확장에는 유리하지만, 첫 MVP에서는 네트워크 지연, 비용, 개인정보/화면 공유 우려, 시연 불안정성, 오답 가능성을 늘린다.
- 코레일톡 명령 2개에서는 API보다 고정된 app dictionary + safety policy + node scoring이 정확도와 통제 가능성에 더 유리하다.
- Gemini Live와의 차별점은 "화면을 보며 말로 조언하는 범용 AI"가 아니라 "사용자가 실제 거래를 완료하도록 앱 위에 바로 행동 가능한 영역만 남기는 completion layer"라는 점이다.
- cue bubble을 누르면 음성 듣는 중 상태를 표시한다.
- 듣는 중 사용자가 코레일톡 화면의 다른 곳을 클릭하거나 bubble 외부를 터치하면 듣기를 멈춘다.
- UI는 최소화한다. 마스킹 중에는 설명 텍스트를 표시하지 않는다.
- 안내 중 화면은 흰색 마스킹으로 덮고, 후보 영역만 보이게 둔다.
- 후보 영역에는 아주 얇은 초록 테두리를 표시한다.
- 후보가 여러 개일 때는 서로 겹치는 후보를 그대로 노출하지 않는다. 후보 rect를 score 기준으로 정렬하고, IoU/교차 면적이 큰 후보는 같은 cluster로 묶어 가장 높은 score 후보만 남긴다.
- 여러 후보가 서로 다른 행동으로 모두 유효하면 최대 3개까지만 남기고, 각 후보 주변 margin을 적용한 뒤 마스크 조각을 계산해 터치 가능 영역이 서로 겹치지 않게 한다.
- 후보 간 거리가 너무 가까워 사용자가 헷갈릴 수 있으면 더 큰 container 후보 대신 내부의 명확한 clickable child 후보를 우선한다.
- 그래도 겹침이 해소되지 않으면 "못 찾았어요. 다시 말해 주세요"로 실패 처리한다. 정확도가 우선이다.

## 5. API Design

- Android service는 orchestration만 담당하고, 정확도와 사업 핵심 로직은 testable domain module에 둔다.
- 내부 API는 "음성 요청 -> command -> screen snapshot -> safety check -> target scoring -> overlay instruction -> guide session transition" 파이프라인으로 설계한다.

### Domain Models

- `KorailCommand`: `SHOW_MY_TICKET`, `FIND_RESERVATION_START`
- `GuideState`: `Idle`, `Listening`, `Thinking`, `Guiding`, `SensitivePause`, `Failed`
- `ScreenSnapshot`: packageName, nodes, timestamp를 담는 접근성 화면 스냅샷
- `ScreenNode`: text, contentDescription, className, bounds, clickable, enabled, visible, depth, parentHint를 담는 pure data model
- `SafetyDecision`: allowed, reason, matchedKeywords
- `TargetCandidate`: nodeId, bounds, score, targetType, evidence
- `OverlayInstruction`: visibleHoles, highlightedBounds, spokenPrompt, timeoutMs
- `GuideStepResult`: nextState, overlayInstruction, stopReason

### Core Modules

- `SpeechController`: `startListening(locale)`, `stopListening()`, `onResult(text)`, `onError(error)`를 제공한다.
- `KorailCommandParser`: 한국어 음성 문장을 `KorailCommand`로 변환한다. MVP에서는 2개 command만 지원한다.
- `AccessibilitySnapshotMapper`: `AccessibilityNodeInfo` tree를 `ScreenSnapshot`/`ScreenNode`로 변환한다.
- `KorailScreenAnalyzer`: 현재 코레일톡 화면이 어떤 screen context인지 판별한다.
- `SafetyPolicy`: 로그인, 결제, 인증, 개인정보, 예매 확정, 구매 확정 화면을 중단시킨다.
- `TargetScorer`: command별 후보 node를 점수화한다.
- `CandidateResolver`: 중복/겹침 후보를 cluster 처리하고 최대 3개 후보만 남긴다.
- `OverlayController`: 흰색 mask 조각과 초록 테두리를 그린다.
- `GuideSession`: 최대 3-step 연속 안내 상태, timeout, stop condition을 관리한다.
- `CueAccessibilityService`: Android lifecycle, 접근성 이벤트 수신, overlay attach/detach, module orchestration만 담당한다.
- `SettingsRepository`: onboarding, bubble enabled, bubble position, voice enabled만 저장한다.

### Public Internal Contracts

```kotlin
interface KorailCommandParser {
    fun parse(utterance: String): KorailCommand?
}

interface SafetyPolicy {
    fun evaluate(snapshot: ScreenSnapshot): SafetyDecision
}

interface TargetScorer {
    fun score(snapshot: ScreenSnapshot, command: KorailCommand): List<TargetCandidate>
}

interface CandidateResolver {
    fun resolve(candidates: List<TargetCandidate>): List<TargetCandidate>
}

interface GuideSession {
    fun begin(command: KorailCommand)
    fun next(snapshot: ScreenSnapshot): GuideStepResult
    fun stop(reason: StopReason)
}
```

### Test Strategy

- `KorailCommandParser`, `SafetyPolicy`, `TargetScorer`, `CandidateResolver`, `GuideSession`은 JVM unit test로 검증한다.
- Android framework 의존성이 큰 `OverlayController`와 `CueAccessibilityService`는 thin adapter로 유지하고 build + 수동 테스트 중심으로 검증한다.

## 6. Data Design

- 데이터 저장은 제품 동작과 UT/B2B 설득에 꼭 필요한 최소값으로 제한한다.
- 화면 원문, 음성 원문, 승차권 정보, 개인정보, 결제/인증 정보, 코레일 계정 정보는 저장하지 않는다.
- 기본 설정은 DataStore Preferences에 저장한다.
- UT metric은 로컬 JSONL 또는 DataStore counter 중 구현이 쉬운 방식으로 저장한다. 단 원문 payload는 금지한다.

### 저장 허용

- `onboarding_completed`: Boolean
- `accessibility_guide_completed`: Boolean
- `bubble_enabled`: Boolean
- `bubble_edge`: `left` 또는 `right`
- `bubble_y_ratio`: Float
- `voice_enabled`: Boolean
- `consent_version`: String
- `consent_accepted_at`: Long timestamp
- `ut_session_id`: 익명 UUID
- `ut_task_type`: `show_my_ticket` 또는 `find_reservation_start`
- `ut_started_at`: Long timestamp
- `ut_finished_at`: Long timestamp
- `ut_elapsed_ms`: Long
- `ut_result`: `success`, `failed`, `sensitive_pause`, `cancelled`
- `ut_stop_reason`: enum 값만 저장
- `ut_step_count`: Int

### 저장 금지

- STT 결과 원문
- 화면 텍스트 원문
- 접근성 node dump
- 승차권 번호, 열차 번호, 좌석, 날짜, 이름, 전화번호
- 로그인 ID, 비밀번호, 인증번호
- 결제 수단, 카드, 계좌, 주문/예매 확정 정보

### Metric Goal

- 완료시간 50% 감소는 UT에서 cuee 사용 전/후 같은 task를 비교해 측정한다.
- 도움 요청 70% 감소는 observer가 세는 외부 기록으로 측정한다. 앱 내부 저장 필수값이 아니다.
- 실패율 30%p 감소는 `ut_result` 기준으로 집계한다.
- B2B 피칭에는 개인 데이터가 아니라 aggregate metric만 사용한다.

## 7. Architecture

- 패키지 구조는 구현이 쉽고, 정확도 로직을 빠르게 테스트/수정할 수 있게 나눈다.
- `domain`은 Android framework 의존성을 최소화한다.
- `service`는 lifecycle과 orchestration만 담당한다.
- 정확도는 `command`, `safety`, `scoring`, `session`에서 개선한다.
- 코레일톡 외 앱은 모두 ignore한다.

```text
app/src/main/java/com/cuee/
  data/
    SettingsRepository
    DataStoreSettingsRepository
    UtMetricRepository
  domain/
    command/
      KorailCommand
      KorailCommandParser
    safety/
      SafetyPolicy
      SafetyDecision
      StopReason
    scoring/
      ScreenNode
      ScreenSnapshot
      TargetCandidate
      TargetScorer
      CandidateResolver
    session/
      GuideState
      GuideSession
      GuideStepResult
  accessibility/
    AccessibilitySnapshotMapper
    KorailScreenAnalyzer
  overlay/
    BubbleOverlayController
    MaskOverlayController
    CandidateHighlighter
  speech/
    AndroidSpeechController
    AndroidTtsController
  service/
    CueAccessibilityService
  ui/
    MainActivity
```

- Unit test는 `domain` 중심으로 작성한다.
- Android 의존성이 큰 overlay/service는 얇은 adapter로 유지하고 debug build + 실제 기기 수동 테스트로 검증한다.

## 8. Technical Decisions

- 첫 MVP에서는 AI API key를 사용하지 않는다.
- 첫 MVP에서는 Gemini Live 같은 범용 화면 상담 AI와 경쟁하지 않고, 코레일톡 거래 실패율을 낮추는 app-specific completion layer로 포지셔닝한다.
- 한국어 명령 2개만 구현한다: "예매한 표 보여줘", "승차권 예매 찾기".
- 외국인은 구현 범위가 아니라 발표 스토리와 UT 확장 타깃으로 포함한다.
- 실제 코레일톡 위 접근성 오버레이만 구현한다.
- 시뮬레이션 화면은 만들지 않는다. 실제 동작이 충분히 매끄럽지 않을 때만 발표 fallback으로 추가한다.
- 자동 클릭은 하지 않는다.
- 자동 예매, 결제, 로그인, 인증, 예매 확정은 하지 않는다.
- 최대 3-step 연속 안내를 허용하되, 매 step마다 safety/score를 재검사한다.
- 정확도가 낮으면 멈춘다. 완료시간보다 오안내 방지가 우선이다.
- 마스킹 중 텍스트 설명은 표시하지 않는다.
- 후보 영역은 흰색 마스크 사이에 남기고, 아주 얇은 초록 테두리만 표시한다.
- 후보가 겹치면 cluster/score 기반으로 정리하고, 겹침을 안전하게 해소하지 못하면 실패 처리한다.
- UT metric은 로컬 익명 숫자 데이터만 저장한다.
- 발표용 수치는 목표 가설로 명확히 표시한다: 완료시간 50% 감소, 도움 요청 70% 감소, 실패율 30%p 감소.

## 9. Final Docs / Pitch Decisions

- 한문장 소개: "큐(cuee)는 모바일 거래에 실패하는 사람이 앱을 배우지 않아도 다음에 누를 곳만 보여줘 거래를 끝내게 하는 화면 길잡이입니다."
- 모두가 같은 장면을 떠올리게 하는 중심 이미지는 코레일톡 화면, 흰색 마스킹, 남겨진 버튼, 사용자의 직접 터치다.
- 타깃은 "노인"이 아니라 "모바일 거래 실패율이 높은 사용자"다. 고령자, 인지 부담이 큰 사용자, 방한 외국인이 자연스럽게 포함된다.
- 첫 wedge는 코레일톡이다. 이유는 모바일 예매 비중이 높고, 실패 시 창구/콜센터/가족 대리예매/민원으로 비용이 전가되기 때문이다.
- B2C 스토리는 보호자 가족이 월 7,900원을 내는 가설로 둔다. 구매 이유는 부모가 매번 앱 사용에 실패해 전화로 대리 도움을 요청하는 반복 비용이다.
- B2B 스토리는 코레일/교통/관광 사업자에게 거래 실패율, 민원, 창구 전환, 콜센터 부담을 줄이는 화면 길잡이 솔루션을 제공하는 것이다.
- 피치 순서는 B2C로 절박한 수요를 증명하고, B2B로 큰 시장과 반복 매출 가능성을 보여주는 구조로 간다.
- 때려잡을 대상은 "앱 실패 뒤에 생기는 대리 예매 구조"다: 가족 전화 도움, 역 창구, 콜센터, 여행 대행/우회 서비스, 디지털 교육만으로 해결하려는 방식.
- Gemini Live 대비 차별점은 범용 상담이 아니라 app-specific completion이다. 말로 조언하는 것이 아니라 실제 앱 위에서 눌러야 할 곳만 남겨 거래 완료를 돕는다.
- 올해가 적기인 이유는 모바일 예매 채널 집중, 디지털포용법 시행, 고령층/외국인 디지털 접근성 니즈, 기업의 접근성/민원 비용 압박으로 설명한다.
- 출시 즉시 살 고객은 부모의 교통 예매를 반복적으로 대신해주는 보호자다. 노브랜드여도 돈을 낼 만큼 문제가 반복적이고 감정 비용이 크다는 점을 검증한다.
- UT 계획은 고령자와 외국인/비숙련 사용자에게 코레일톡 "예매한 표 찾기"와 "승차권 예매 시작" task를 cuee 사용 전/후로 수행시키고 완료시간, 실패율, 도움 요청 횟수를 비교한다.
- 목표 수치는 완료시간 50% 감소, 도움 요청 70% 감소, 실패율 30%p 감소로 둔다.
