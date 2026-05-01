# 코드 아키텍처

## 아키텍처 원칙
- Kotlin Android Native.
- MVP는 단일 앱 모듈로 시작한다. 멀티모듈은 아직 금지.
- Compose는 앱 내부 화면에 사용 가능. 오버레이는 Android View 기반이 더 안정적이다.
- 복잡한 Clean Architecture보다 명확한 패키지 분리와 작은 클래스가 우선이다.
- 핵심 경로는 STT 결과 이후 1초 안에 마스킹해야 한다.

## 패키지 구조
```text
app/
  ui/
    onboarding/
    tutorial/
    home/
    terms/
  service/
    CueAccessibilityService.kt
  overlay/
    BubbleOverlayController.kt
    SpeechPanelController.kt
    MaskOverlayController.kt
    DragDismissController.kt
  engine/
    CueEngine.kt
    IntentParser.kt
    FlowController.kt
    ScreenContextDetector.kt
    NodeTreeReader.kt
    NodeMatcher.kt
    SafetyGuard.kt
    TextExtractor.kt
  data/
    UserSettingsStore.kt
    AppCapabilityRepository.kt
  voice/
    SpeechController.kt
    TtsController.kt
  launcher/
    AppLauncher.kt
```

## 주요 컴포넌트
### CueAccessibilityService
- 접근성 이벤트 수신.
- 현재 루트 노드 제공.
- 화면 변경, 클릭, 콘텐츠 변경 이벤트를 `CueEngine`에 전달.
- 큐 자체 오버레이 이벤트와 외부 앱 이벤트를 구분한다.

### OverlayController 계열
- `BubbleOverlayController`: 대기 동그라미, 드래그, 가장자리 스냅.
- `SpeechPanelController`: 듣는 중/찾는 중 표시.
- `MaskOverlayController`: 후보 영역을 제외하고 흰색 마스킹.
- `DragDismissController`: 드래그 중 하단 X 표시와 끄기 처리.

### CueEngine
요청 처리의 진입점.

```text
STT result
 -> IntentParser
 -> SafetyGuard pre-check
 -> NodeTreeReader
 -> ScreenContextDetector
 -> NodeMatcher
 -> FlowController
 -> OverlayController
```

### IntentParser
- 규칙 기반.
- AI API 사용 금지.
- 지원 범위 밖 요청은 빠르게 실패한다.
- 쿠팡 검색어와 카톡 메시지 본문만 추출한다.

### ScreenContextDetector
- 점수 기반 단독 탐색의 한계를 줄인다.
- 앱별 화면 상태를 2-3개만 판단한다.
- 예: `COUPANG_HOME`, `COUPANG_SEARCH_INPUT`, `KAKAO_CHAT_ROOM`, `KORAIL_HOME`, `SENSITIVE`.

### NodeMatcher
- 현재 `ScreenContext`에서 가능한 `TargetType`만 찾는다.
- 좌표 레시피를 쓰지 않는다.
- 텍스트, contentDescription, className, editable, clickable, actions, bounds를 점수화한다.

### SafetyGuard
- 금지 버튼 후보 제거.
- 민감 화면 감지.
- 자동 입력 금지 여부 판단.
- `확인`, `동의`는 민감 화면에서만 차단한다.

### FlowController
- 한 요청당 최대 5단계.
- 스크롤 재탐색 최대 3회.
- 마스킹 12초 무동작 시 종료.
- 민감 화면, 실패, 사용자 화살표 종료 시 세션 폐기.

### SpeechController
- Android `SpeechRecognizer` 사용.
- 가능하면 on-device recognizer 우선.
- 실패 시 "다시 말하기"로 회복.

### TtsController
- 짧은 존댓말.
- 기본 속도 1.0.
- 마스킹 중 화면 텍스트 대신 음성으로 안내.

## NodeMatcher 정책
### 필터
제외:
- disabled 노드
- bounds 없음
- 화면 밖 bounds
- 24dp보다 작은 영역
- 금지 키워드 포함
- 현재 앱/시스템 키보드/큐 외 이상한 패키지

### 점수 기준
```text
정확 키워드 일치 +60
부분 키워드 일치 +35
contentDescription 일치 +45
editable + target이 입력칸 +50
clickable + target이 버튼 +25
EditText class +30
Button/ImageButton/TextView class +10
상태별 위치 힌트 +10
SET_TEXT action + 자동 입력 필요 +20
너무 일반적인 텍스트 -20
긴 문단 -30
형제 후보 과다 -10
```

### 후보 선택
- 75점 이상: 강한 후보
- 60-74점: 약한 후보
- 60점 미만: 제외
- 강한 후보 1개: 바로 안내
- 강한 후보 2-3개: 모두 노출
- 강한 후보 4개 이상: 상위 3개만, 단 3위와 4위 점수 차 10점 이상일 때만
- 약한 후보만 있음 + 스크롤 가능: 스크롤 안내
- 그 외 실패

## 마스킹 구현
- 전체 흰색 단일 오버레이에 구멍을 뚫는 방식은 피한다.
- 후보 영역을 제외한 주변을 여러 흰색 오버레이 사각형으로 덮는다.
- 후보 영역은 오버레이가 없어 실제 앱 터치가 전달된다.
- 후보 bounds padding은 4-8dp, 기본 6dp.
- 후보가 12dp 이내로 가까우면 노출 영역 병합.
- 병합 영역이 화면의 35% 이상이면 실패 처리.
- 좌측 상단 화살표는 마스킹 중에만 표시한다.

## 자동 입력
- `ACTION_SET_TEXT` 우선.
- 성공 판단은 `performAction` 반환값 우선.
- 쿠팡 검색어 입력 후 키보드 검색 버튼을 먼저 찾는다.
- 키보드 검색 버튼이 없으면 앱 검색 버튼을 찾는다.
- 카톡 메시지 입력 후 바로 종료하고 "보내기 전 내용을 확인해 주세요." 안내.

## 앱 실행
- "쿠팡 열어줘", "카카오톡 열어줘", "코레일 열어줘"만 지원.
- Android intent로 앱만 연다.
- 앱 실행 후 자동 안내는 시작하지 않는다.

## 성능 목표
- STT 결과 이후 1초 이내 오버레이 변화.
- 노드 트리 변환 100ms 이하 목표.
- 후보 계산 50ms 이하 목표.
- 오버레이 표시 100ms 이하 목표.

## 테스트 기준
- 각 지원 명령은 30회 중 27회 이상 성공해야 통과.
- 최소 조합:
  - 삼성 실기기 1대
  - 에뮬레이터 또는 Pixel 계열 1대
  - 글자 크기 기본/크게
  - 삼성 키보드/Gboard

