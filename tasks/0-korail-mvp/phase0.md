# Phase 0: docs-baseline

## 사전 준비

먼저 아래 문서들을 읽고 이번 MVP의 범위를 확인하라:

- `docs/prd.md`
- `docs/flow.md`
- `docs/adr.md`
- `docs/data-schema.md`
- `docs/code-architecture.md`
- `tasks/0-korail-mvp/artifacts/01-initial-plan.md`
- `tasks/0-korail-mvp/artifacts/02-clarify.md`
- `tasks/0-korail-mvp/artifacts/03-context.md`

## 작업 내용

문서 baseline을 점검한다.

- `docs/`에는 아래 5개 문서만 남아야 한다.
  - `docs/prd.md`
  - `docs/flow.md`
  - `docs/adr.md`
  - `docs/data-schema.md`
  - `docs/code-architecture.md`
- 모든 문서는 코레일톡 단일 앱 MVP를 기준으로 해야 한다.
- 구현 범위는 한국어 명령 2개, 실제 접근성 오버레이, 최대 3-step, 자동 클릭 금지, AI API 미사용이다.
- 문서가 이미 조건을 만족하면 불필요하게 수정하지 말고 phase만 완료 처리한다.

`tasks/0-korail-mvp/docs-diff.md`는 직접 작성하지 마라. runner가 phase 0 완료 후 자동 생성한다.

## Acceptance Criteria

```bash
test "$(find docs -maxdepth 1 -type f | wc -l | tr -d ' ')" = "5"
test -f docs/prd.md
test -f docs/flow.md
test -f docs/adr.md
test -f docs/data-schema.md
test -f docs/code-architecture.md
! rg -n "COUPANG|KAKAO|5단계|자동 입력" docs
```

## AC 검증 방법

위 AC 커맨드를 실행하라. 모두 통과하면 `/tasks/0-korail-mvp/index.json`의 phase 0 status를 `"completed"`로 변경하라.
수정 3회 이상 시도해도 실패하면 status를 `"error"`로 변경하고, 에러 내용을 index.json의 해당 phase에 `"error_message"` 필드로 기록하라.

## 주의사항

- 삭제된 `docs/mission.md`, `docs/spec.md`, `docs/testing.md`, `docs/user-intervention.md`를 되살리지 마라.
- 문서를 길게 늘리지 마라. 이 문서는 AI agent의 context 절약을 위해 간결해야 한다.
