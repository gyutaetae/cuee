# Context: korail-mvp

## 디렉토리 구조

- `app/` — Android application module.
- `app/src/main/` — manifest, resources, assets. Kotlin source directory is not present yet.
- `docs/` — agent-facing product/flow/ADR/data/architecture docs. Only five docs are intended to remain.
- `scripts/` — plan-and-build runner utilities.
- `tasks/` — plan-and-build artifacts and phase prompts.

## 진입점

- Gradle project: `settings.gradle.kts`, root `build.gradle.kts`, `app/build.gradle.kts`.
- Android manifest references:
  - `.ui.MainActivity`
  - `.service.CueAccessibilityService`
  - `@xml/cue_accessibility_service`
- Build commands:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:testDebugUnitTest`

## 현재 코드 상태

- Kotlin source files under `app/src/main/java` do not exist yet.
- Existing app resources:
  - `app/src/main/res/drawable/ic_launcher.xml`
  - `app/src/main/res/values/styles.xml`
  - `app/src/main/res/xml/cue_accessibility_service.xml`
- Existing asset `app/src/main/assets/capabilities/apps.json` still contains old 3-app capability data. It conflicts with the new KorailTalk-only MVP and should be removed or replaced in phase 0/1.
- `AndroidManifest.xml` still queries Coupang and KakaoTalk packages. It should be narrowed to KorailTalk only.

## 관련 기존 모듈

None. This implementation should create fresh Kotlin source using the new domain-first architecture in `docs/code-architecture.md`.

## 코드 관습

- Kotlin Android Native.
- Android Gradle Plugin `8.7.3`, Kotlin `2.0.21`.
- JVM target/source compatibility: Java 17.
- Current dependencies:
  - `androidx.datastore:datastore-preferences:1.1.1`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0`
- No test dependencies are currently declared. Unit test dependencies should be added when domain tests are introduced.

## 충돌 / 주의사항

- New docs require KorailTalk-only MVP, but `apps.json` and manifest still include Coupang/KakaoTalk.
- The plan should not revive the old 3-app `engine` design from prior history.
- The five required docs are:
  - `docs/prd.md`
  - `docs/flow.md`
  - `docs/adr.md`
  - `docs/data-schema.md`
  - `docs/code-architecture.md`
- Removed docs should not be recreated unless the user explicitly asks.

## plan stage에 전달할 권고

- Phase 0 should verify only the five docs remain, narrow manifest/assets to KorailTalk-only, and establish docs-diff.
- Phase 1 should create buildable Android skeleton: `MainActivity`, `CueAccessibilityService`, settings store, resources, and Gradle test deps.
- Phase 2 should implement testable domain models/parser/safety/scoring/resolver/session with unit tests.
- Phase 3 should implement accessibility snapshot mapping and overlay layout/masking controllers.
- Phase 4 should integrate speech/TTS, service orchestration, KorailTalk package filtering, and guide session.
- Phase 5 should add minimal UI, UT metrics, hardening, and final build/test verification.
