# Phase 4: speech-service

## 사전 준비

먼저 아래 문서들을 읽어라:

- `docs/prd.md`
- `docs/flow.md`
- `docs/adr.md`
- `docs/code-architecture.md`
- `tasks/0-korail-mvp/docs-diff.md`

그리고 이전 phase 산출물을 확인하라:

- `app/src/main/java/com/cuee/domain/*`
- `app/src/main/java/com/cuee/accessibility/*`
- `app/src/main/java/com/cuee/overlay/*`
- `app/src/main/java/com/cuee/service/CueAccessibilityService.kt`

## 작업 내용

음성, TTS, 접근성 서비스 orchestration을 연결한다.

필수 파일:

- `speech/AndroidSpeechController.kt`
- `speech/AndroidTtsController.kt`
- `service/CueAccessibilityService.kt` 업데이트

구현 규칙:

- 한국어 locale `ko-KR`만 사용한다.
- bubble tap 시 `LISTENING` 상태로 들어가고 "무엇을 도와드릴까요?"를 안내한다.
- 외부 터치/코레일톡 화면 클릭이 감지되면 listening을 중단한다.
- STT 결과 원문은 parser 입력으로만 사용하고 저장하지 않는다.
- `CueAccessibilityService`는 command parser, mapper, safety, scorer, resolver, guide session, overlay controller를 orchestration한다.
- 현재 package가 코레일톡이 아니면 안내하지 않는다.
- 민감 화면이면 "이 화면은 직접 확인해 주세요"로 중단한다.
- 사용자가 노출 영역을 누른 뒤 접근성 이벤트가 오면 최대 3-step까지 다음 안내를 시도한다.

## Acceptance Criteria

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## AC 검증 방법

위 AC 커맨드를 실행하라. 모두 통과하면 `/tasks/0-korail-mvp/index.json`의 phase 4 status를 `"completed"`로 변경하라.
수정 3회 이상 시도해도 실패하면 status를 `"error"`로 변경하고, 에러 내용을 index.json의 해당 phase에 `"error_message"` 필드로 기록하라.

## 주의사항

- 음성 원문을 로그/파일/DataStore에 저장하지 마라.
- AI API나 네트워크 호출을 추가하지 마라.
- 자동 클릭을 구현하지 마라.
