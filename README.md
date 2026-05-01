# 큐(cuee) MVP

Android Native Kotlin MVP for the cuee accessibility guide.

## 구현 범위
- 온보딩, 접근성 설정 안내, 튜토리얼, 홈, 약관 화면
- AccessibilityService 기반 화면 노드 분석
- Messenger-style 큐 동그라미, 드래그, 하단 X 끄기
- 음성 인식, TTS 안내
- 흰색 마스킹 오버레이
- 쿠팡/카카오톡/코레일톡 6개 이하 명령 사전
- 쿠팡 검색어와 카카오톡 메시지 자동 입력 시도
- 결제/송금/주문 확정/민감정보 안내 차단

## 빌드
Android Studio에서 프로젝트 루트(`udam`)를 열고 Gradle Sync 후 실행한다.

현재 Codex 환경에는 `java`, `gradle`, `ANDROID_HOME`이 PATH에 없어 로컬 CLI 빌드는 수행하지 못했다.

## 첫 수동 검증
1. 앱 실행
2. 시작하기
3. 접근성 설정에서 `큐` 켜기
4. 튜토리얼 완료
5. 쿠팡을 열고 큐 동그라미 누르기
6. "물티슈 찾아줘" 말하기
7. 검색창 마스킹, 자동 입력, 검색 버튼 안내 확인

