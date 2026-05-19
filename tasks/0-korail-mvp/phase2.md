# Phase 2: domain-core

## 사전 준비

먼저 아래 문서들을 읽어라:

- `docs/prd.md`
- `docs/adr.md`
- `docs/data-schema.md`
- `docs/code-architecture.md`
- `tasks/0-korail-mvp/docs-diff.md`

그리고 이전 phase 산출물을 확인하라:

- `app/src/main/java/com/cuee/data/*`
- `app/src/main/java/com/cuee/ui/MainActivity.kt`
- `app/src/main/java/com/cuee/service/CueAccessibilityService.kt`

## 작업 내용

Android framework에 거의 의존하지 않는 domain core를 구현한다.

패키지:

- `domain/command`
- `domain/safety`
- `domain/scoring`
- `domain/session`

필수 구현:

- `KorailCommand`, `KorailCommandParser`, `DefaultKorailCommandParser`
- `Bounds`, `ScreenNode`, `ScreenSnapshot`
- `SafetyDecision`, `StopReason`, `SafetyPolicy`, `DefaultSafetyPolicy`
- `TargetCandidate`, `TargetType`, `CandidateEvidence`, `TargetScorer`, `KorailTargetScorer`
- `CandidateResolver`, `ClusterCandidateResolver`
- `GuideState`, `GuideSession`, `DefaultGuideSession`, `GuideStepResult`, `OverlayInstruction`, `SpokenPrompt`

핵심 규칙:

- 지원 명령은 `SHOW_MY_TICKET`, `FIND_RESERVATION_START`만 허용한다.
- 코레일톡 package가 아니면 `NOT_KORAIL_APP`으로 중단한다.
- 로그인/개인정보/인증번호/결제/예매 확정/구매 확정 키워드는 민감 화면으로 중단한다.
- 후보 score는 문서 기준을 따른다.
- 후보는 최대 3개까지 resolve한다.
- 겹치는 후보는 cluster 처리하고 최고 score만 유지한다.
- 최대 3-step을 넘기지 않는다.

JVM unit test를 추가한다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## AC 검증 방법

위 AC 커맨드를 실행하라. 모두 통과하면 `/tasks/0-korail-mvp/index.json`의 phase 2 status를 `"completed"`로 변경하라.
수정 3회 이상 시도해도 실패하면 status를 `"error"`로 변경하고, 에러 내용을 index.json의 해당 phase에 `"error_message"` 필드로 기록하라.

## 주의사항

- Android `AccessibilityNodeInfo`를 domain layer에 직접 노출하지 마라.
- 자동 클릭, 자동 예매, 자동 결제 관련 타입을 만들지 마라.
- 테스트 통과를 위해 safety policy를 약화하지 마라.
