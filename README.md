# 꼭꼭 안드로이드

Kotlin / Jetpack Compose 기반 안드로이드 앱입니다. 현재는 Figma 기준의 홈 화면과 공통 디자인 요소를 구현한 단계입니다. 사진 선택·분석 흐름은 후속 PR에서 구현합니다. 미구현 진입점은 디자인 외형을 유지한 채 비활성화되어 있습니다. 실제 AI·로그인·SNS·지도·백엔드 연결은 없습니다.

## 협업 방식

두 레포는 각각 GitHub Flow를 사용합니다.

1. 최신 `main`에서 작업 브랜치를 만듭니다.
2. 하나의 작업 목적에 맞춰 구현하고 검증합니다.
3. PR에 목적·변경 내용·검증 결과를 작성하고 다른 팀원에게 리뷰를 요청합니다.
4. 필요한 검증을 마치면 작성자가 `main`에 병합할 수 있습니다. 승인을 기다릴 의무는 없습니다.
5. 병합 후 리뷰도 확인하고 필요한 수정은 후속 PR로 반영합니다.

`develop` 브랜치는 사용하지 않습니다. 자동 검사와 승인 강제 설정은 이번 문서 세팅에 포함하지 않습니다.

### 브랜치와 커밋

- 브랜치: `<type>/<영문 작업명>` — 예: `feat/photo-picker`, `fix/upload-error`, `docs/api-guide`
- 커밋: `<type>: <한글 중심 설명>` — 예: `feat: 사진 선택 화면 추가`
- type: `feat`, `fix`, `refactor`, `docs`, `style`, `test`, `chore`
- 하나의 PR은 가능한 한 하나의 작업 목적을 담습니다.

### PR 전 확인

- 코드 변경: 빌드와 해당 기능의 기본 동작을 확인하고, 관련 테스트가 있으면 실행합니다.
- 문서 변경: 내용과 링크를 확인합니다.
- 검증하지 못한 항목은 사유를 PR에 기록합니다.
- 실제 API와 모의 데이터·모의 응답 검증을 구분해서 기록합니다.

Codex 작업 원칙은 [AGENTS.md](./AGENTS.md)를 따릅니다.

## 실행 및 개발 환경

### 준비

- Android Studio와 JDK 21을 사용합니다. Android Studio의 Gradle JDK를 내장 `jbr-21`로 설정합니다.
- Android SDK Platform 36, Build Tools 36.0.0, Platform Tools를 설치합니다.
- 최소 지원 버전은 Android 8.0(API 26), target/compile SDK는 36입니다.
- AGP 9.1.1의 내장 Kotlin 2.2.10과 Compose compiler plugin 2.2.10을 사용합니다. 의존성 버전은 `gradle/libs.versions.toml`에서 관리합니다.
- Gradle 9.3.1은 저장소의 Wrapper가 내려받습니다. 배포본 SHA-256을 검증하며, 첫 빌드에는 인터넷 연결이 필요합니다.
- 실제 비밀값이나 서버 환경변수는 필요하지 않습니다.

Android Studio에서 저장소 루트를 열고 Gradle Sync를 실행합니다. `local.properties`의 `sdk.dir`에는 각자 설치한 SDK 경로를 설정합니다. 이 파일과 IDE 설정·빌드 결과는 커밋하지 않습니다.

macOS 터미널에서는 Android Studio 기본 설치 기준으로 다음과 같이 JDK를 지정할 수 있습니다.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

### 빌드·설치·검증

JDK와 SDK를 준비한 뒤 저장소 루트에서 실행합니다.

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

에뮬레이터나 USB 디버깅이 켜진 실제 기기를 연결한 뒤 실행합니다.

```sh
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
adb shell am start -W -n com.kkokkkok.android/.MainActivity
```

Android Studio에서는 Device Manager에서 Pixel 7 / API 36 ARM64 가상 기기를 생성하고 실행한 뒤 `app`의 Run 버튼을 눌러도 됩니다. `adb`를 찾지 못하면 SDK의 `platform-tools`를 PATH에 추가합니다.

홈 검증은 JDK 21과 Pixel 7 / API 36 ARM64 에뮬레이터에서 수행했습니다. Debug APK 빌드와 Lint, 앱 실행·홈 문구·홈 탭 선택 상태·미구현 기능 비활성화·스크롤 접근을 확인하는 기기 테스트 4개를 확인했습니다. 390×844dp / 기본 글자 크기와 320×640dp / 글자 200% 설정에서 테스트를 실행하고 실행 화면을 확인했습니다. 단위 테스트 소스는 아직 없어 `testDebugUnitTest`는 `NO-SOURCE`입니다. 실제 기기·API 26 실행·사진 선택 및 실제 API 연결은 미검증입니다.

Lint에는 고정한 도구·라이브러리의 새 버전 알림이 남아 있습니다. 런처 아이콘에는 Figma의 마스코트 벡터를 사용합니다. `app/build/reports/`에서 Lint와 기기 테스트 보고서를 확인할 수 있으며, 보고서·스크린샷은 커밋하지 않습니다.

### 구조와 다음 단계

- `app`: 단일 앱 모듈, `MainActivity`가 `HomeScreen`을 실행합니다.
- `ui/design`: 공통 색상·글꼴·간격, 텍스트·아이콘·아이콘 배지입니다.
- `ui/home`: 홈 카드·단축 메뉴·하단 탭입니다. 현재는 정적 화면으로, 상태나 데이터가 필요한 다음 단계에서 ViewModel·Repository를 추가합니다.
- `app/src/androidTest`: 홈 표시·미구현 기능 비활성화·스크롤 접근을 확인하는 기기 테스트입니다.
- 홈의 색상·Jua/Gowun Dodum 글꼴·아이콘은 Figma 기준입니다. [디자인 자산 출처와 적용 기준](docs/design-assets.md)을 참고하세요.
- 시스템 상태바와 제스처 바는 Android 기본 UI입니다. Figma에 그려진 상태바를 이미지로 중복 구현하지 않으며 실제 기기의 안전 여백을 반영합니다.
- 이후 화면 → ViewModel → Repository 인터페이스 → 모의 구현으로 확장합니다. 모의 모델은 임시 앱 내부 모델이며 확정된 API 명세가 아닙니다.
- 사진·키·개인정보·실험 결과는 커밋하지 않습니다. 로컬 검증 자료가 필요하면 Git에서 제외된 `private-data/`를 사용합니다.

[Figma 원본](https://www.figma.com/design/pz3FjXweJTJTlk8rQipHSr/?node-id=10-2)은 읽기 전용입니다. 홈 프레임은 `26:141`이며, 화면 구현 단계에서 실행 화면을 직접 비교하고 확인 가능한 시각적 차이와 동작 오류를 수정합니다.
