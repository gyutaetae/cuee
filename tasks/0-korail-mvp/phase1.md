# Phase 1: android-skeleton

## 사전 준비

먼저 아래 문서들을 읽어라:

- `docs/prd.md`
- `docs/flow.md`
- `docs/adr.md`
- `docs/data-schema.md`
- `docs/code-architecture.md`
- `tasks/0-korail-mvp/docs-diff.md`

그리고 이전 phase 산출물을 확인하라:

- `tasks/0-korail-mvp/index.json`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/cue_accessibility_service.xml`

## 작업 내용

빌드 가능한 Android skeleton을 만든다.

- `app/src/main/java/com/cuee/ui/MainActivity.kt`를 생성한다.
- `app/src/main/java/com/cuee/service/CueAccessibilityService.kt`를 생성한다.
- `data/SettingsRepository.kt`, `data/DataStoreSettingsRepository.kt`를 생성한다.
- manifest `queries`는 코레일톡 package만 남긴다.
- `app/src/main/assets/capabilities/apps.json`는 제거하거나 코레일톡 단일 JSON으로 바꾼다. 쿠팡/카카오톡 데이터는 남기지 않는다.
- Gradle에 JVM unit test를 위한 필요한 최소 의존성을 추가한다.
- 앱은 서버, 로그인, AI API 없이 동작해야 한다.

필수 타입:

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

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setOnboardingCompleted(value: Boolean)
    suspend fun setAccessibilityGuideCompleted(value: Boolean)
    suspend fun setBubbleEnabled(value: Boolean)
    suspend fun setBubblePosition(edge: BubbleEdge, yRatio: Float)
    suspend fun setVoiceEnabled(value: Boolean)
}
```

## Acceptance Criteria

```bash
! rg -n "com.coupang.mobile|com.kakao.talk|COUPANG|KAKAO" app/src/main
./gradlew :app:assembleDebug
```

## AC 검증 방법

위 AC 커맨드를 실행하라. 모두 통과하면 `/tasks/0-korail-mvp/index.json`의 phase 1 status를 `"completed"`로 변경하라.
수정 3회 이상 시도해도 실패하면 status를 `"error"`로 변경하고, 에러 내용을 index.json의 해당 phase에 `"error_message"` 필드로 기록하라.

## 주의사항

- 아직 scoring/overlay/speech를 깊게 구현하지 마라. phase 1은 buildable skeleton이 목표다.
- 화면 원문이나 음성 원문을 저장하는 코드/로그를 추가하지 마라.
