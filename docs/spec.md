# Spec

## 플랫폼
- Android Native Kotlin
- 최소 Android 10
- 앱 내부 UI는 단순 View 또는 Compose 가능
- 오버레이는 Android View 기반
- 핵심 API: `AccessibilityService`, `AccessibilityNodeInfo`, `SpeechRecognizer`, `TextToSpeech`, `WindowManager.TYPE_ACCESSIBILITY_OVERLAY`

## 사용자 흐름
1. 앱 첫 실행
2. 신뢰 문구와 동의
3. 접근성 설정으로 이동
4. 튜토리얼 필수 진행
5. 지원 앱 위에 큐 동그라미 상주
6. 사용자가 동그라미 탭
7. 음성 요청
8. 현재 화면 접근성 노드 분석
9. 후보 영역만 남기고 흰색 마스킹
10. 사용자가 직접 누름
11. 최대 5단계까지 연속 안내

## 상태
- Idle: 큐 동그라미
- Listening: “무슨 도움이 필요한지 말해보세요.”
- Thinking: “찾고 있어요.”
- Guiding: 흰색 마스킹, 좌측 상단 화살표
- ScrollHint: 마스킹 없이 아래 화살표
- Failed: 실패 문구와 “다시 말하기”
- SensitivePause: 개인정보 화면 직접 확인 안내

## 데이터
저장:
- 온보딩 완료 여부
- 튜토리얼 완료 여부
- 동의 버전/시각
- 동그라미 표시 여부
- 동그라미 좌우/세로 위치
- 음성 안내 여부

저장 금지:
- 화면 텍스트 원문
- 음성 원문
- 카톡 메시지
- 검색어
- 개인정보
- 릴리즈 빌드 진단 로그

## 앱별 사전
좌표를 저장하지 않는다. 앱별 사전은 package name, 지원 action, trigger keyword, target type, blocked keyword, sensitive keyword만 둔다.

TargetType:
- SEARCH_FIELD
- SEARCH_BUTTON
- CART_BUTTON
- MESSAGE_INPUT
- PHOTO_BUTTON
- MY_TICKET_BUTTON
- TICKET_RESERVATION_BUTTON
- APP_ICON_OR_LAUNCH

## 탐색 방식
1. 현재 앱 package 식별
2. 지원 action 파싱
3. 민감 화면 검사
4. ScreenContext 판단
5. TargetType별 노드 점수화
6. 후보 1~3개 노출
7. 후보 없음 + 스크롤 가능 시 최대 3회 스크롤 안내
8. 애매하면 실패

## 자동 입력
허용:
- 쿠팡 검색어
- 카카오톡 메시지 본문

금지:
- 주소, 전화번호, 계좌번호, 카드번호, 비밀번호, 인증번호, 주민등록번호
- 결제/송금/주문 확정/구매 확정 관련 입력

## 마스킹
- 전체 흰색 단일 오버레이가 아니라, 후보 영역을 비운 흰색 사각형 조합으로 구현한다.
- 후보 영역은 실제 앱이 터치를 받는다.
- 마스킹 중 텍스트는 표시하지 않는다.
- 음성으로만 “눌러주세요.”
- 좌측 상단 화살표는 큐 안내만 닫는다.

## 앱 실행
“쿠팡 열어줘”, “카카오톡 열어줘”, “코레일 열어줘”는 Android intent로 앱만 연다. 앱 실행 후 자동 조작은 하지 않는다.

