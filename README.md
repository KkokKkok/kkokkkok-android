# 꼭꼭 안드로이드

Kotlin / Jetpack Compose 기반 안드로이드 앱입니다. 사진 한 장 또는 여러 장을 선택해 단독 분석하고, 저장된 검사 기록에서 보호 편집을 시작할 수 있습니다. 보호 기록은 서버의 원본·수정 사진을 표시하고 누적 분석 기록은 당시 결과를 다시 엽니다. 실제 앱은 백엔드 API에 연결하며 실제 분석·편집 여부는 서버 Provider 설정에 따릅니다. 모의 응답은 별도로 표시하고, 점수가 없으면 미산정으로 표시합니다. 카카오 로그인은 별도 빌드 설정으로 사용할 수 있으며, 네이버 로그인·SNS 계정·지도 SDK 연동은 포함하지 않습니다.

## 협업 방식

두 레포는 각각 GitHub Flow를 사용합니다.

1. 작업 전에 이슈를 생성하고 목적·범위·완료 기준·검증 계획을 기록합니다. 기존 이슈가 있으면 해당 이슈를 사용합니다.
2. 최신 `main`에서 이슈의 작업 목적에 맞는 브랜치를 만듭니다.
3. 작은 단위로 구현·검증하고, PR에 관련 이슈·목적·변경 내용·검증 결과를 작성합니다. UI 변경은 실행 환경과 Figma 직접 비교 결과를 기록합니다. 실행 이미지는 별도 요청이 있을 때만 첨부합니다.
4. 다른 팀원에게 리뷰를 요청합니다. 사용자와 단계별 확인을 합의한 작업은 확인·병합 후 다음 작업으로 진행합니다. 일반적인 승인 대기는 필수가 아닙니다.
5. 필요한 검증과 합의된 확인을 마친 뒤 `main`에 병합합니다.
6. 병합 후 리뷰도 확인하고 필요한 수정은 후속 이슈·PR로 반영합니다.

`develop` 브랜치는 사용하지 않습니다. 자동 검사와 승인 강제 설정은 이번 문서 세팅에 포함하지 않습니다.

### 이슈와 PR 작성

- [작업 요청 템플릿](.github/ISSUE_TEMPLATE/task.yml): 기능·개선·리팩터링·문서 등 하나의 작업 목적과 완료 기준을 기록합니다.
- [버그 신고 템플릿](.github/ISSUE_TEMPLATE/bug.yml): 실제/기대 동작·재현 순서·실행 환경을 기록합니다.
- [PR 템플릿](.github/pull_request_template.md): 완료하는 이슈는 `Closes #번호`, 일부만 진행하면 `Refs #번호`로 연결합니다.
- UI 변경 PR에는 기기·Android 버전·화면 크기·글자 배율과 Figma 비교 결과·남은 차이를 적습니다. 실행 이미지는 별도 요청이 있을 때만 첨부하며, Figma 비교와 기본 동작 검증은 작성자가 먼저 수행합니다.
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

Android Studio에서 저장소 루트를 열고 Gradle Sync를 실행합니다. `local.properties`의 `sdk.dir`에는 각자 설치한 SDK 경로를 설정합니다. 필요하면 이 파일에 `kkokkkok.backend.baseUrl=http://10.0.2.2:8080`을 추가해 로컬 에뮬레이터가 연결할 백엔드 주소를 바꿀 수 있습니다. 이 값은 비밀값이 아니지만 `local.properties`와 IDE 설정·빌드 결과는 커밋하지 않습니다.

에뮬레이터에서 사진 분석을 확인할 때는 백엔드 저장소를 `SPRING_PROFILES_ACTIVE=local`로 실행합니다. 이 프로필은 로컬 개발 환경에서만 앱 API 키 검사를 끄며, 분석은 `stub` 모드로 실행되어 사진을 실제로 분석하지 않습니다. 기본 에뮬레이터 주소는 `http://10.0.2.2:8080`입니다.

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

검증 기준 환경은 JDK 21과 Pixel 7 / API 36 ARM64 에뮬레이터(390×844dp, 기본 글자 크기)입니다. 기기 테스트의 실행 대상과 결과는 각 PR의 검증란에 기록합니다. 단위 테스트 소스는 아직 없어 `testDebugUnitTest`는 `NO-SOURCE`입니다.

- `AnalysisFlowTest` 3개: 검사 취소·완료·홈 복귀, 사진 읽기 실패와 재시도, 점수 미산정 결과에서 단서 영역과 안내 표시 확인
- `AnalysisResultParserTest` 2개: 미산정 점수·단서 위험도 및 기존 점수·검토 등급 파싱 확인
- `MockAnalysisTest` 1개: 모의 분석 완료와 취소 시 Repository 작업 중단 확인
- 홈 테스트 4개: 홈 문구·선택된 탭·미구현 기능 비활성화·스크롤 접근 확인
- `PhotoPickerResultTest` 2개: 모의 Activity 결과로 단일 이미지 요청·상태 전달·재선택·취소 확인
- `SystemPhotoPickerTest` 1개: 실제 시스템 선택기에 임시 합성 이미지를 넣어 선택·내용 읽기·Activity 재생성·홈 복귀 확인. API 33 이상·영어 설정의 에뮬레이터에서만 실행하며 그 외 환경에서는 건너뜁니다.

기존 홈 PR에서는 320×640dp / 글자 200% 화면도 확인했습니다. 이번 Photo Picker의 실제 시스템 UI는 API 36에서 검증했으며 실제 기기·API 26 문서 선택기 fallback·실제 API 연결은 미검증입니다.

Lint에는 고정한 도구·라이브러리의 새 버전 알림이 남아 있습니다. 런처 아이콘에는 Figma의 마스코트 벡터를 사용합니다. `app/build/reports/`에서 Lint와 기기 테스트 보고서를 확인할 수 있으며, 보고서·스크린샷은 커밋하지 않습니다.

### 사진 선택 범위

- 홈의 `위치 노출 검사`를 누르면 시스템 Photo Picker가 이미지 한 장 선택 모드로 열립니다. 미지원 환경에서는 AndroidX가 문서 선택기로 전환합니다.
- 사진·저장소 전체 접근 권한을 추가하지 않습니다. Photo Picker에서 새 사진 선택을 취소하면 기존 선택을 유지합니다. 위치 화면에서 뒤로 가면 선택을 취소해 홈으로 돌아가며, 선택 URI나 사진 내용을 로그에 출력하지 않습니다.
- 선택 상태는 Activity 재생성을 견디지만 프로세스 종료·앱 재시작 후에는 복원하지 않습니다. 장기 접근 권한·사진 복사·업로드는 수행하지 않습니다. 화면 표시용 이미지는 백그라운드에서 축소 디코딩하고 EXIF 방향을 반영합니다. GPS 분석이나 이미지 수정은 하지 않습니다.
- 사용자가 승인한 시스템 UI를 사용하므로 Figma의 내부 갤러리·위험도 배지는 구현하지 않습니다. 사진을 업로드한 뒤 백엔드가 돌려준 `locationStatus`가 `available`이면 촬영 위치 입력을 건너뛰고 분석합니다. `unavailable`이면 사용자가 입력한 위치를 `locationFallback`으로 분석 요청에 전달합니다. EXIF GPS가 있는 경우 백엔드는 수동 입력보다 EXIF를 우선합니다. 위치 분류 칩은 화면 선택 상태이며 사진 태그와는 별개입니다. 모의 지도는 실제 지도 검색이나 좌표와 연결되지 않습니다. Photo Picker를 취소하면 홈에 남고, 위치 화면의 뒤로 가기는 사진 선택을 취소해 홈으로 돌아갑니다. 분석 화면에서 뒤로 가면 입력한 위치를 유지한 채 위치 화면으로 돌아갑니다. 업로드/검사 실패에는 다시 시도·홈 복귀를 제공합니다.

### 모의 분석 계층

`PhotoSelectionViewModel`이 `AnalysisRepository`를 통해 사진 업로드·EXIF 위치 여부 확인·분석 진행 상태를 처리합니다. 실제 앱은 `BackendAnalysisRepository`로 사진을 업로드하고 `assetId`와 필요한 경우에만 `locationFallback`을 보내 단독 분석을 조회합니다. 백엔드 미연결 시 오류·재시도 화면을 표시합니다. `MockAnalysisRepository`는 테스트용 모의 구현으로 유지합니다. 사진과 단서 영역은 같은 중심 잘림 좌표 변환을 사용합니다.

백엔드 stub은 사진 내용을 읽지 않고 고정 점수·단서와 정규화된 예시 영역을 반환합니다. 결과의 `isMock`은 `true`이며 실제 GPS·OCR·위치 추정 결과가 아닙니다. 앱 내부 모델은 API 응답으로부터 변환되며 확정된 파이프라인 명세가 아닙니다.

기초 파이프라인 응답에서 위험 점수나 단서별 위험도가 아직 정해지지 않은 경우에는 `미산정`으로 표시합니다. 이를 0점·낮은 위험이나 오류로 바꾸지 않으며, bbox가 있으면 위험 등급이 없어도 사진 위에 영역을 표시합니다. 이 표시는 현재 파이프라인 연결을 위한 임시 대응이고 API 계약을 확정하지 않습니다.

모의 구현에서는 검사 중·결과 화면에 모의 안내를 표시합니다. 실제 백엔드 결과는 모의 표시를 숨기고 응답의 단서 영역을 그립니다. 점수·등급이 미정인 응답은 `미산정`으로 구분합니다. 과거 게시물 비교와 `3 / 3` 표시는 제외하고, 저장 기능은 준비 중 안내를 표시합니다. Figma 원본은 변경하지 않습니다.

### 구조와 다음 단계

- `app`: 단일 앱 모듈, `MainActivity`가 홈과 검사 흐름을 연결합니다.
- `ui/design`: 공통 색상·글꼴·간격, 텍스트·아이콘·아이콘 배지입니다.
- `ui/analysis`: 검사 중·결과·실패 화면, 선택 사진 디코딩과 예시 단서 영역 표시입니다.
- `ui/home`: 홈 카드·단축 메뉴·하단 탭입니다. 선택한 사진 URI는 `ui/photo/PhotoSelectionViewModel`의 읽기 전용 StateFlow로 관리합니다. `analysis/AnalysisRepository` 경계 아래 Android 백엔드 구현과 테스트용 mock 구현을 둡니다.
- `app/src/androidTest`: 홈 표시·미구현 기능 비활성화·스크롤 접근을 확인하는 기기 테스트입니다.
- 홈의 색상·Jua/Gowun Dodum 글꼴·아이콘은 Figma 기준입니다. [디자인 자산 출처와 적용 기준](docs/design-assets.md)을 참고하세요.
- 시스템 상태바와 제스처 바는 Android 기본 UI입니다. Figma에 그려진 상태바를 이미지로 중복 구현하지 않으며 실제 기기의 안전 여백을 반영합니다.
- 화면 → ViewModel → Repository 인터페이스 → 백엔드/모의 구현으로 구성합니다. 모의 모델은 임시 앱 내부 모델이며 확정된 API 명세가 아닙니다.
- 사진·키·개인정보·실험 결과는 커밋하지 않습니다. 로컬 검증 자료가 필요하면 Git에서 제외된 `private-data/`를 사용합니다.

[Figma 원본](https://www.figma.com/design/pz3FjXweJTJTlk8rQipHSr/?node-id=10-2)은 읽기 전용입니다. 홈 프레임은 `26:141`이며, 화면 구현 단계에서 실행 화면을 직접 비교하고 확인 가능한 시각적 차이와 동작 오류를 수정합니다.

### 저장 기록과 보호 편집

- 사진 기록의 결과 상세에서 보호 편집을 시작하면 완료된 검사 ID와 선택 가능한 단서 좌표를 전달합니다. 이미 검사한 사진은 수정 대상 선택부터 진행합니다.
- 검사 결과의 ‘수정 없이 마치기’는 추가 저장/삭제 요청 없이 홈으로 이동합니다. 이미 서버에 보관된 원본·검사 기록은 유지합니다. 단독·다량 검사와 기존 기록에서 연 결과에 동일하게 적용합니다.
- 보호 기록은 저장된 수정 자산과 원본 자산의 실제 썸네일을 표시합니다. 기록을 누르면 기존 수정 결과 화면에서 비교·갤러리 저장하고, 원본의 편집 가능한 실제 검사 단서가 있으면 다시 수정할 수 있습니다. 평가된 점수가 없는 경우에는 숫자를 생성하지 않습니다.
- SNS 분석 탭은 외부 SNS 연결 대신 현재 설치의 장소별 누적 분석 실행 기록을 조회합니다. 기록을 열면 당시 점수·그룹·제외 근거를 재현하며 새 분석을 요청하지 않습니다.
- 누적 결과와 저장된 누적 기록의 ‘이 게시물들 보호 편집하기’는 결과에 포함된 사진의 완료된 실제 단독 검사를 조회합니다. 주의/높음이면서 좌표가 있는 단서가 있는 사진만 결과 순서대로 편집합니다. 사진 확인이나 다음 사진 이동은 AI를 호출하지 않고, 각 사진에서 ‘보호 이미지 만들기’를 눌러야 새 편집을 실행합니다. 준비 실패는 재시도하며 편집할 단서가 없는 사진은 안내합니다. 마지막에는 원래 누적 결과로 돌아옵니다.
- 누적 결과의 반복 사진 수는 단서 항목 수와 구분합니다. 서로 다른 사진 두 장 이상에서 확인된 그룹만 반복 항목으로 표시합니다.
- 서버가 실행 중이고 기기에서 접근 가능한 주소로 빌드해야 합니다. USB 테스트는 APK에 설정한 포트에 대해 `adb -s <기기ID> reverse tcp:<포트> tcp:<포트>`가 필요합니다. API 키는 APK에 넣지 않습니다.

### 실제 서버로 휴대폰에서 확인하기

1. 분석 서버에서 모델 키와 서버 간 인증 키를 설정하고 실행합니다. OCR·객체 탐지를 사용할 때는 보조 서버도 실행하고 URL과 인증 키를 함께 설정합니다. [분석 API 설정](https://github.com/jack0928/KkokKkok-PoC/blob/main/docs/analysis-api.md)을 참고하세요.
2. Spring 서버에 `ANALYSIS_PROVIDER=node`, `NODE_ANALYSIS_BASE_URL`, `NODE_ANALYSIS_API_KEY`, `NODE_ANALYSIS_EXTERNAL_IMAGE_CONSENT=true`를 설정합니다. 실제 편집은 `EDITING_PROVIDER=openai`와 서버의 `OPENAI_API_KEY`가 필요합니다. [백엔드 실행 안내](https://github.com/KkokKkok/kkokkkok-backend/blob/main/README.md)를 참고하세요.
3. 로컬 USB 시험에서는 서버를 loopback에 실행하고 백엔드의 `local` 프로필을 사용합니다. 앱 키를 APK에 넣지 않습니다. 이 설정은 공개 서버 배포 설정이 아닙니다.
4. `local.properties`의 `kkokkkok.backend.baseUrl`을 `http://127.0.0.1:<백엔드 포트>`로 맞추고 APK를 빌드합니다. `adb devices -l`로 기기를 확인한 뒤 `adb -s <기기ID> reverse tcp:<포트> tcp:<포트>`와 `adb -s <기기ID> install -r app/build/outputs/apk/debug/app-debug.apk`를 실행합니다.
5. 단독·여러 장 순차 결과·GPS 누락 입력·기록 재조회·여러 영역 편집·수정본 재검사·누적 분석을 확인합니다. 실제 연결 여부는 응답의 `mock`과 단계 상태로 판단합니다. 점수 미산정과 부분 실패도 성공 점수로 대체하지 않습니다.

서버 기본 Provider가 `stub`이면 실제 분석이나 편집이 수행되지 않습니다. PC와 USB 연결을 끊은 뒤에도 사용하려면 기기에서 접근 가능한 서버 주소와 별도 배포 구성이 필요합니다.

### 카카오 로그인 빌드

실제 로그인을 시험할 때만 `local.properties`에 다음 값을 설정합니다. 이 파일은 Git에 올리지 않습니다.

```properties
kakao.nativeAppKey=카카오 콘솔의 네이티브 앱 키
kkokkkok.login.required=true
```

네이티브 앱 키는 앱에 포함되는 공개 식별자이며, 카카오 콘솔의 Android 플랫폼에 등록한 패키지명(`com.kkokkkok.android`)과 서명 키 해시로 사용이 제한됩니다. 서버는 `AUTH_ENABLED=true`와 `KAKAO_APP_ID`로 실행해야 합니다([백엔드 로그인 API](https://github.com/KkokKkok/kkokkkok-backend/blob/main/docs/inspection-api.md)). `kkokkkok.pilot.required`와 함께 켤 수 없습니다.

- 카카오톡이 있으면 카카오톡으로, 없거나 실패하면 카카오 계정 화면으로 로그인합니다. 사용자가 취소하면 로그인 화면에 머뭅니다.
- 서버가 발급한 서비스 토큰만 Android Keystore AES-GCM으로 암호화해 서버 주소별로 보관합니다. 카카오 토큰은 저장하지 않습니다. access token 만료 1분 전에 자동 갱신하고, 갱신이 거부되면 로그인 화면으로 돌아갑니다.
- 처음 가입한 사용자는 약관 동의(필수 항목 기본 미체크)와 가입 완료 화면을 거칩니다. 약관 화면에서 뒤로 가면 가입을 취소하고 로그아웃합니다. 동의 내용은 서버에 이력으로 저장되며, 서버가 현재 약관 미동의 사용자의 API 이용을 막으면(403) 앱이 약관 화면을 다시 엽니다.
- 마이 화면의 설정 버튼에서 로그아웃합니다. Figma에 설정 화면이 없어 확인 창으로 제공합니다. 사용자가 바뀌면 이전 화면 상태를 모두 폐기합니다.
- 네이버 로그인은 준비 중 안내를 표시합니다.

### 비공개 파일럿 최초 등록

공개 HTTPS 서버 주소로 빌드하면 초대 코드 등록 화면이 기본 적용됩니다. 로컬에서 파일럿 등록을 시험할 때만 `local.properties`에 다음 값을 설정합니다. 이 파일과 초대 코드·토큰은 Git에 올리지 않습니다.

```properties
kkokkkok.backend.baseUrl=http://127.0.0.1:18083
kkokkkok.pilot.required=true
```

서버는 `postgres,pilot` profile과 HTTPS 배포 구성을 사용합니다. 등록은 처음 한 번이며 서버 재배포·동일 서명 APK 업데이트로 앱 데이터가 유지되면 반복하지 않습니다. 자격은 Android Keystore의 AES-GCM으로 암호화해 서버 주소별로 보관하며 백업·기기 이전에서 제외합니다. API 키를 APK에 넣지 않습니다.

모든 실제 API 요청에 설치 자격을 전달하고, 자격 만료·취소로 401이 오면 재등록 화면으로 돌아갑니다. 재등록 시 이전 화면의 ViewModel 상태를 폐기합니다. 새 기기·재설치·앱 데이터 삭제 시에는 초대 코드를 다시 입력합니다. 계정 로그인·다른 기기로 데이터 자동 복구는 제공하지 않습니다. 기존 `local` USB 모드는 등록 없이 유지하지만 다인 공개 배포에 쓰지 않습니다.

[백엔드 등록·운영 안내](https://github.com/KkokKkok/kkokkkok-backend/blob/main/docs/pilot-access.md). [암호화 구현 기준](https://developer.android.com/privacy-and-security/keystore).
