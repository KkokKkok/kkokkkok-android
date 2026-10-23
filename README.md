# 꼭꼭 안드로이드

Kotlin / Jetpack Compose 기반 안드로이드 앱입니다. 현재는 Figma 기준의 홈 화면과 공통 디자인 요소를 구현한 단계입니다. 홈의 위치 노출 검사에서 Android 시스템 Photo Picker로 사진 한 장을 선택할 수 있습니다. 선택 후 홈으로 돌아오며 미리보기·분석 흐름은 후속 PR에서 연결합니다. 그 외 미구현 진입점은 디자인 외형을 유지한 채 비활성화되어 있습니다. 실제 AI·로그인·SNS·지도·백엔드 연결은 없습니다.

## 협업 방식

두 레포는 각각 GitHub Flow를 사용합니다.

1. 작업 전에 이슈를 생성하고 목적·범위·완료 기준·검증 계획을 기록합니다. 기존 이슈가 있으면 해당 이슈를 사용합니다.
2. 최신 `main`에서 이슈의 작업 목적에 맞는 브랜치를 만듭니다.
3. 작은 단위로 구현·검증하고, PR에 관련 이슈·목적·변경 내용·검증 결과를 작성합니다. UI 변경은 실제 실행 이미지도 첨부합니다.
4. 다른 팀원에게 리뷰를 요청합니다. 사용자와 단계별 확인을 합의한 작업은 확인·병합 후 다음 작업으로 진행합니다. 일반적인 승인 대기는 필수가 아닙니다.
5. 필요한 검증과 합의된 확인을 마친 뒤 `main`에 병합합니다.
6. 병합 후 리뷰도 확인하고 필요한 수정은 후속 이슈·PR로 반영합니다.

`develop` 브랜치는 사용하지 않습니다. 자동 검사와 승인 강제 설정은 이번 문서 세팅에 포함하지 않습니다.

### 이슈와 PR 작성

- [작업 요청 템플릿](.github/ISSUE_TEMPLATE/task.yml): 기능·개선·리팩터링·문서 등 하나의 작업 목적과 완료 기준을 기록합니다.
- [버그 신고 템플릿](.github/ISSUE_TEMPLATE/bug.yml): 실제/기대 동작·재현 순서·실행 환경을 기록합니다.
- [PR 템플릿](.github/pull_request_template.md): 완료하는 이슈는 `Closes #번호`, 일부만 진행하면 `Refs #번호`로 연결합니다.
- UI 변경 PR에는 GitHub 첨부로 실제 실행 이미지를 넣고 기기·Android 버전·화면 크기·글자 배율을 적습니다. Figma 비교와 기본 동작 검증은 작성자가 먼저 수행합니다.
- UI 변경이 없으면 실행 화면은 해당 없음으로 적습니다. 실행하지 못한 경우 사유와 미검증 항목을 기록합니다.
- 실제 사진·개인정보·비밀값은 이슈·PR에 포함하지 않습니다. 검증 이미지는 합성 데이터로 만들고 저장소에 커밋하지 않습니다.

### 브랜치와 커밋

- 브랜치: `<type>/<영문 작업명>` — 예: `feat/photo-picker`, `fix/upload-error`, `docs/api-guide`
- 이슈·PR 제목: `<type>: <한글 중심 설명>` — 예: `feat: Android 시스템 Photo Picker 단일 사진 선택 연결`, `fix: 사진 선택 취소 오류 수정`
- 이슈는 작업 요청·버그 신고 두 종류를 사용합니다. 기능·개선·리팩터링·문서·테스트·설정은 작업 요청으로 묶고 제목의 type으로 구분합니다. 작업 유형을 별도 필드에 중복 입력하지 않습니다.
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

검증 환경은 JDK 21과 Pixel 7 / API 36 ARM64 에뮬레이터(390×844dp, 기본 글자 크기)입니다. Debug APK 빌드·Lint·기기 테스트 8개를 확인했습니다. 단위 테스트 소스는 아직 없어 `testDebugUnitTest`는 `NO-SOURCE`입니다.

- `MockAnalysisTest` 1개: 모의 분석 완료와 취소 시 Repository 작업 중단 확인
- 홈 테스트 4개: 홈 문구·선택된 탭·미구현 기능 비활성화·스크롤 접근 확인
- `PhotoPickerResultTest` 2개: 모의 Activity 결과로 단일 이미지 요청·상태 전달·재선택·취소 확인
- `SystemPhotoPickerTest` 1개: 실제 시스템 선택기에 임시 합성 이미지를 넣어 선택·내용 읽기·취소·Activity 재생성 시 상태 유지 확인. API 33 이상·영어 설정의 에뮬레이터에서만 실행하며 그 외 환경에서는 건너뜁니다.

기존 홈 PR에서는 320×640dp / 글자 200% 화면도 확인했습니다. 이번 Photo Picker의 실제 시스템 UI는 API 36에서 검증했으며 실제 기기·API 26 문서 선택기 fallback·실제 API 연결은 미검증입니다.

Lint에는 고정한 도구·라이브러리의 새 버전 알림이 남아 있습니다. 런처 아이콘에는 Figma의 마스코트 벡터를 사용합니다. `app/build/reports/`에서 Lint와 기기 테스트 보고서를 확인할 수 있으며, 보고서·스크린샷은 커밋하지 않습니다.

### 사진 선택 범위

- 홈의 `위치 노출 검사`를 누르면 시스템 Photo Picker가 이미지 한 장 선택 모드로 열립니다. 미지원 환경에서는 AndroidX가 문서 선택기로 전환합니다.
- 사진·저장소 전체 접근 권한을 추가하지 않습니다. 선택 취소·뒤로 가기는 기존 선택을 유지하며, 선택한 URI나 사진 내용을 로그에 출력하지 않습니다.
- 선택 상태는 Activity 재생성을 견디지만 프로세스 종료·앱 재시작 후에는 복원하지 않습니다. 장기 접근 권한·사진 복사·디코딩·업로드는 아직 수행하지 않습니다.
- 사용자가 승인한 시스템 UI를 사용하므로 Figma의 내부 갤러리·카테고리·위험도 배지는 구현하지 않습니다. 선택 후 별도 미리보기나 안내 없이 홈으로 돌아옵니다. 다음 단계는 별도 이슈·PR에서 연결합니다.

### 모의 분석 계층

`PhotoSelectionViewModel.startAnalysis()`가 `AnalysisRepository`를 통해 진행 단계와 완료 결과를 받으며 `analysisState`로 제공합니다. `cancelAnalysis()`와 새 사진 선택은 진행 작업을 취소하고 상태를 초기화합니다. 현재 앱 화면에서는 분석 시작을 호출하지 않으며, 분석 중·결과 UI는 후속 PR에서 연결합니다.

모의 구현은 사진 내용을 읽지 않고 고정 점수·단서와 정규화된 예시 영역을 반환합니다. 결과의 `isMock`은 `true`이며 실제 GPS·OCR·위치 추정 결과가 아닙니다. 이 모델은 임시 앱 내부 구조로 확정 API 명세가 아닙니다.

후속 화면은 모의 분석 안내를 표시하고 과거 게시물 비교와 `3 / 3` 표시를 제외하며, 단서 영역은 모의 예시로 구분하기로 합의했습니다. Figma 원본은 변경하지 않습니다.

### 구조와 다음 단계

- `app`: 단일 앱 모듈, `MainActivity`가 `HomeScreen`을 실행합니다.
- `ui/design`: 공통 색상·글꼴·간격, 텍스트·아이콘·아이콘 배지입니다.
- `ui/home`: 홈 카드·단축 메뉴·하단 탭입니다. 선택한 로컬 사진 URI는 `ui/photo/PhotoSelectionViewModel`의 읽기 전용 StateFlow로 관리합니다. 분석은 `analysis/AnalysisRepository` 인터페이스와 네트워크 없는 `MockAnalysisRepository`로 분리합니다.
- `app/src/androidTest`: 홈 표시·미구현 기능 비활성화·스크롤 접근을 확인하는 기기 테스트입니다.
- 홈의 색상·Jua/Gowun Dodum 글꼴·아이콘은 Figma 기준입니다. [디자인 자산 출처와 적용 기준](docs/design-assets.md)을 참고하세요.
- 시스템 상태바와 제스처 바는 Android 기본 UI입니다. Figma에 그려진 상태바를 이미지로 중복 구현하지 않으며 실제 기기의 안전 여백을 반영합니다.
- 이후 화면 → ViewModel → Repository 인터페이스 → 모의 구현으로 확장합니다. 모의 모델은 임시 앱 내부 모델이며 확정된 API 명세가 아닙니다.
- 사진·키·개인정보·실험 결과는 커밋하지 않습니다. 로컬 검증 자료가 필요하면 Git에서 제외된 `private-data/`를 사용합니다.

[Figma 원본](https://www.figma.com/design/pz3FjXweJTJTlk8rQipHSr/?node-id=10-2)은 읽기 전용입니다. 홈 프레임은 `26:141`이며, 화면 구현 단계에서 실행 화면을 직접 비교하고 확인 가능한 시각적 차이와 동작 오류를 수정합니다.
