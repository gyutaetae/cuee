# Phase 3: overlay-accessibility

## 사전 준비

먼저 아래 문서들을 읽어라:

- `docs/flow.md`
- `docs/adr.md`
- `docs/data-schema.md`
- `docs/code-architecture.md`
- `tasks/0-korail-mvp/docs-diff.md`

그리고 이전 phase 산출물을 확인하라:

- `app/src/main/java/com/cuee/domain/*`
- `app/src/test/java/com/cuee/domain/*`
- `app/src/main/java/com/cuee/service/CueAccessibilityService.kt`

## 작업 내용

접근성 tree 변환과 overlay 계산/표시 레이어를 구현한다.

필수 파일:

- `accessibility/AccessibilitySnapshotMapper.kt`
- `accessibility/KorailScreenAnalyzer.kt`
- `overlay/BubbleOverlayController.kt`
- `overlay/MaskOverlayController.kt`
- `overlay/CandidateHighlighter.kt`
- `overlay/OverlayLayoutCalculator.kt`

구현 규칙:

- `AccessibilitySnapshotMapper`는 `AccessibilityNodeInfo`를 `ScreenSnapshot`/`ScreenNode`로 변환한다.
- Android node 객체를 오래 보관하지 않는다.
- `BubbleOverlayController`는 cue bubble을 접근성 overlay로 표시하고 좌우 edge snap을 지원한다.
- `MaskOverlayController`는 후보 영역을 비운 흰색 overlay 조각을 표시한다.
- 후보 영역에는 overlay를 두지 않아 실제 코레일톡 터치가 전달되어야 한다.
- `CandidateHighlighter`는 얇은 초록 테두리만 표시한다.
- 마스킹 중 설명 텍스트는 표시하지 않는다.
- `OverlayLayoutCalculator`는 후보 padding, 겹침, mask rectangle 계산을 담당하며 unit test 가능해야 한다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## AC 검증 방법

위 AC 커맨드를 실행하라. 모두 통과하면 `/tasks/0-korail-mvp/index.json`의 phase 3 status를 `"completed"`로 변경하라.
수정 3회 이상 시도해도 실패하면 status를 `"error"`로 변경하고, 에러 내용을 index.json의 해당 phase에 `"error_message"` 필드로 기록하라.

## 주의사항

- `TYPE_APPLICATION_OVERLAY`가 아니라 접근성 서비스 overlay를 사용하라.
- 후보 영역을 덮는 overlay를 만들지 마라.
- 텍스트가 포함된 안내 패널을 마스킹 위에 추가하지 마라.
