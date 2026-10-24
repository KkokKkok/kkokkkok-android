# 디자인 자산

[Figma 원본](https://www.figma.com/design/pz3FjXweJTJTlk8rQipHSr/?node-id=26-141)은 읽기 전용입니다. 홈 `26:141`의 디자인 컨텍스트와 스크린샷을 기준으로 구현합니다.

## 벡터

Figma에서 제공한 SVG를 Android SDK `Svg2Vector`로 변환한 VectorDrawable입니다. 원본의 색상과 크기를 유지하며 아이콘 대체나 틴트를 적용하지 않습니다. 임시 다운로드 URL은 앱에 포함하지 않습니다.

| 리소스 | Figma 노드 | 원본 크기(dp) | 적용 위치 |
|---|---|---|---|
| `figma_mascot` | `26:155` | 36×36 | 헤더 로고, 앱 아이콘 |
| `figma_bell` | `26:164` | 22×22 | 44dp 알림 배지 |
| `figma_shield` | `59:507` | 32×32 | 74dp 주황 원 내부 |
| `figma_layers` | `26:191` | 18×18 | 36dp SNS 배지 |
| `figma_scan` | `70:809` | 18×18 | 36dp 위치 검사 배지 |
| `figma_home` | `I26:204;15:17` | 24×24 | 홈 탭 |
| `figma_clock` | `I26:204;15:23` | 24×24 | 검사 기록 탭 |
| `figma_pencil` | `I26:204;15:29` | 24×24 | 보호 기록 탭 |
| `figma_user` | `I26:204;15:34` | 24×24 | 마이 탭 |

앱 내 벡터 호출은 `FigmaIcon`을 사용하며, 원본의 자연 크기를 사용합니다. 개인 사진이나 실행 스크린샷은 UI 자산으로 사용하지 않습니다.

## SNS 게시물 선택 예시

Figma `3-3 게시물 선택`에 보이는 예시 썸네일만 `drawable-nodpi/sns_post_*.png`에 로컬로 포함합니다. 이 썸네일은 SNS 연동 대신 화면과 선택 동작을 확인하는 모의 자료이며, 사용자가 선택한 사진이나 실제 SNS 게시물 데이터가 아닙니다. 실행 화면 캡처는 저장소에 포함하지 않습니다.

## 글꼴

Google Fonts 공식 저장소의 원본 TTF를 로컬에 포함합니다. 네트워크 글꼴 로딩은 사용하지 않습니다.

- [Jua](https://github.com/google/fonts/tree/main/ofl/jua): 제목·브랜드. 라이선스: [Jua-OFL.txt](../licenses/fonts/Jua-OFL.txt)
- [Gowun Dodum](https://github.com/google/fonts/tree/main/ofl/gowundodum): 본문·레이블. 라이선스: [GowunDodum-OFL.txt](../licenses/fonts/GowunDodum-OFL.txt)

## 레이아웃과 범위

`KkokColors`, `KkokTypography`, `KkokSpacing`에서 공통 스타일을 관리합니다. 작은 화면·큰 글자에서는 내용이 줄바꿈되고 스크롤되며 하단 탭은 고정됩니다. Android의 상태바·화면 잘림 영역·제스처 바 안전 여백으로 인해 Figma의 기기 프레임과 상하 배치가 달라질 수 있습니다. 프레임 모서리나 상태바 이미지는 앱 콘텐츠로 복제하지 않습니다.

홈의 위치 노출 검사와 SNS 누적 검사 진입점을 활성화합니다. 보호 편집·알림·기록·마이는 각각 구현 범위에 따라 연결합니다. 사용자가 승인한 Android 시스템 Photo Picker를 사용하는 단일 사진 검사 화면은 Figma `29:52`의 내부 갤러리와 외형이 다릅니다. 준비 중 안내나 임의의 미리보기 화면은 추가하지 않으며, 선택 결과는 ViewModel에 보관하고 모의 검사 화면으로 이어집니다. 검사 중·결과·실패 화면은 각각 `30:120`, `30:191`, `63:954`를 기준으로 구현합니다. 모의 안내 문구를 표시하고 과거 게시물 비교·반복 노출 경고·`3 / 3` 표시는 제외합니다. 저장·수정 버튼은 비활성화하며 실패 안내는 사진 읽기/모의 검사 실패에 맞게 적용합니다. 선택 사진은 동적 콘텐츠이며 단서 영역은 고정 모의 예시입니다.

## 검사 흐름 벡터

같은 SDK 변환 방식으로 가져온 원본 SVG입니다. 원본의 자연 크기로 표시합니다.

| 리소스 | 기준 프레임 | 크기(dp) | 적용 위치 |
|---|---|---|---|
| `figma_back` | `30:120`, `30:191` | 22×22 | 40dp 뒤로 가기 배지 |
| `figma_analysis_mascot` | `30:120` | 48×48 | 검사 안내 왼쪽 |
| `figma_analysis_check` | `30:120` | 14×14 | 완료 단계의 24dp 배지 |
| `figma_analysis_dot` | `30:120` | 24×24 | 현재 진행 단계 |
| `figma_info` | `30:191` | 14×14 | 예시 결과 안내 |
| `figma_sad_mascot` | `63:954` | 112×112 | 실패 안내 |

사진 방향 표시는 [AndroidX ExifInterface](https://developer.android.com/jetpack/androidx/releases/exifinterface)를 사용하며 원본 사진은 수정하지 않습니다.
