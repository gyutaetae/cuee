# Phase 5: ui-metrics-hardening

## 사전 준비

먼저 아래 문서들을 읽어라:

- `docs/prd.md`
- `docs/flow.md`
- `docs/adr.md`
- `docs/data-schema.md`
- `docs/code-architecture.md`
- `tasks/0-korail-mvp/docs-diff.md`

그리고 이전 phase 산출물을 확인하라:

- `app/src/main/java/com/cuee/data/*`
- `app/src/main/java/com/cuee/domain/*`
- `app/src/main/java/com/cuee/accessibility/*`
- `app/src/main/java/com/cuee/overlay/*`
- `app/src/main/java/com/cuee/speech/*`
- `app/src/main/java/com/cuee/service/CueAccessibilityService.kt`
- `app/src/main/java/com/cuee/ui/MainActivity.kt`

## 작업 내용

MVP를 발표/UT 가능한 상태로 마무리한다.

- `MainActivity`에 최소 UI를 구현한다.
  - 접근성 설정 열기
  - cuee가 대신 누르지 않음
  - 결제/로그인/개인정보 화면에서 멈춤
  - 코레일톡 단일 MVP 안내
- `data/UtMetricRepository.kt`와 필요 시 `LocalUtMetricRepository.kt`를 구현한다.
- UT metric은 원문 없는 숫자/enum만 저장한다.
- domain/overlay/session test를 보강한다.
- 빌드 경고 중 crash 가능성이 있는 문제를 정리한다.
- docs 5개와 구현이 충돌하지 않는지 확인한다.

## Acceptance Criteria

```bash
test "$(find docs -maxdepth 1 -type f | wc -l | tr -d ' ')" = "5"
! rg -n "com.coupang.mobile|com.kakao.talk|COUPANG|KAKAO|자동 클릭|AI API key" app/src/main docs
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## AC 검증 방법

위 AC 커맨드를 실행하라. 모두 통과하면 `/tasks/0-korail-mvp/index.json`의 phase 5 status를 `"completed"`로 변경하라.
수정 3회 이상 시도해도 실패하면 status를 `"error"`로 변경하고, 에러 내용을 index.json의 해당 phase에 `"error_message"` 필드로 기록하라.

## 주의사항

- 보호자 계정, 서버, 로그인, 네트워크 기능을 추가하지 마라.
- UT metric에 화면/음성/승차권/개인정보 원문을 넣지 마라.
- 시뮬레이션 화면을 만들지 마라.
