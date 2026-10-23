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

앱 내 벡터 호출은 `FigmaIcon`을 사용하며, 원본의 자연 크기를 사용합니다. 사진이나 스크린샷을 UI 자산으로 사용하지 않습니다.

## 글꼴

Google Fonts 공식 저장소의 원본 TTF를 로컬에 포함합니다. 네트워크 글꼴 로딩은 사용하지 않습니다.

- [Jua](https://github.com/google/fonts/tree/main/ofl/jua): 제목·브랜드. 라이선스: [Jua-OFL.txt](../licenses/fonts/Jua-OFL.txt)
- [Gowun Dodum](https://github.com/google/fonts/tree/main/ofl/gowundodum): 본문·레이블. 라이선스: [GowunDodum-OFL.txt](../licenses/fonts/GowunDodum-OFL.txt)

## 레이아웃과 범위

`KkokColors`, `KkokTypography`, `KkokSpacing`에서 공통 스타일을 관리합니다. 작은 화면·큰 글자에서는 내용이 줄바꿈되고 스크롤되며 하단 탭은 고정됩니다. Android의 상태바·화면 잘림 영역·제스처 바 안전 여백으로 인해 Figma의 기기 프레임과 상하 배치가 달라질 수 있습니다. 프레임 모서리나 상태바 이미지는 앱 콘텐츠로 복제하지 않습니다.

사용자와 합의한 이번 범위는 홈 외형입니다. 보호 편집·SNS 검사·알림·기록·마이와 아직 연결되지 않은 사진 검사 진입점은 외형을 유지하고 접근성 의미에서도 비활성화합니다. 준비 중 안내 화면이나 임의의 이동 동작은 추가하지 않습니다. 사진 선택 연결과 모의 분석 표시는 다음 PR에서 진행합니다.
