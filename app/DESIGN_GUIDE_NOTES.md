# Pleos Connect 디자인 가이드 요약 + 다다캐스트 화면 점검

- 작성일: 2026-11-10
- 범위: `docs/connect/guide/docs-design/` 아래 42개 페이지 전부(sitemap.xml 기준, 페이지 안 내비게이션 링크와 대조했고 누락 없음) + 본문이 링크하는 "주행 안전을 고려한 설계" 1페이지
- 약어: 아래 URL 앞부분 `D/` = `https://document.pleos.ai/docs/connect/guide/docs-design/`
- 주의: 이 가이드는 **수치 대부분을 이미지(SVG/PNG)로만 제공**합니다. 텍스트로 없는 값은 이미지를 렌더링해서 읽었고, 그런 값에는 "(도표)"라고 표시했습니다. 이미지에도 수치가 없으면 "문서에 없음"이라고 적었습니다.
- 원본 HTML, 이미지, 렌더링 결과는 작성자 로컬 작업 폴더에만 있고 이 저장소에는 포함하지 않았습니다.

---

## 0. 기본 원칙 (Intro)

출처: `D/Introduction/intro`, `D/Introduction/designing-apps-for-driving`

- Easy & Safe: "운전 중 동영상, 애니메이션 이미지, 텍스트 자동 스크롤과 같이 운전자의 시선을 유도하면 안 됩니다."
- 한 손 조작: "앱 조작은 한 손만으로도 가능해야 하며, 양손을 모두 사용하게 해서는 안 됩니다."
- Glanceability: "운전자는 짧은 시간(1~2초) 내에 정보를 파악해야 하므로, 핵심 정보만 노출"
- 응답 시간: "사용자 입력 후 시스템 응답 시간이 0.25초를 초과하지 않아야 합니다. 응답이 지연되거나 시간이 필요한 경우에는 시각적으로 응답 중인 상태를 알리세요."
- 일관성: 기본 탑재 UI와 아이콘, 용어, 상호작용 패턴을 맞춥니다.
- 다국어와 여러 해상도에 대응해야 합니다.

## 1. 레이아웃

| 항목 | 값 | 출처 |
|---|---|---|
| 화면 구성 | Status bar / Driving view / GNB / Application window(앱 1~2개) | `D/getting-started/layout` |
| 방향 | "가로 전용 게임 등 … 을 제외한 모든 앱은 Landscape, Portrait 두 가지 레이아웃에 적절하게 조절되도록 설계해야 합니다." | `D/getting-started/layout` |
| 레이어 | Status bar, App library, 공조 패널, 키보드 같은 System UI는 앱보다 위에 그려져 앱을 가릴 수 있음 | `D/getting-started/layout` |
| 디스플레이 사양 (도표) | 아래 표 참고 | `D/getting-started/layout` (displaylayout_02.svg) |

| 타입 | 비율 | 인치 | DPI | 해상도(dp) | Landscape 앱 영역(dp) | Portrait 앱 영역(dp) |
|---|---|---|---|---|---|---|
| Connect-S-17 (Default) | 16:9 | 17 | 160 | 2560x1440 | **1700x1184** | 840x1184 |
| Connect-S-14.6 | 16:9 | 14.6 | 200 | 2048x1152 | 1360x948 | 672x948 |
| Connect-L-12.9 | 16:9 | 12.9 | 160 | 1920x1080 | 1274x855 | 628x855 |
| Connect-W-Standard | 9:2 | 24.6 | 160 | 3840x850 | 1640x810 | 800x810 |
| Connect-W-Extended | 24:9 | 24.6 | 160 | 3840x1440 | 1640x1400 | 800x1400 |
| Connect-WF | 32:9 | 18.5 | 200 | 3072x816 | 1256x800 | 648x800 |

- 현재 에뮬레이터: 2560x1440, density 160이므로 Connect-S-17에 해당하고, 앱 영역은 1700x1184dp(가로) 또는 840x1184dp(세로)입니다.
- 창 정책 (`D/getting-started/window`)
  - Connect-S는 앱 2개일 때 1:1, 1개일 때 2:0
  - Connect-W는 2:1 또는 1:2
  - 3손가락 스와이프나 윈도 바로 위치 변경, 크기 변경, 종료를 할 수 있음
  - 새로 연 앱은 이전 앱과 같은 orientation으로 뜸
- Safe area: 앱용으로는 **문서에 없음**. Status bar 쪽만 "40dp의 safe bounds를 right padding으로 포함"한다고 되어 있습니다 (`D/components/status-bar`).
- 그리드, 컬럼, 앱 내부 마진: **문서에 없음**
- 간격 관련 수치
  - Toast: 앱 패널 안에서 side padding 24, bottom padding 24, Toast max width 792, 메시지 max width 712 (`D/components/toast`)
  - Toast Popup: "24dp 공간을 두고 Max로 제공" (`D/components/popups/toast-popup`)

## 2. 타이포그래피

출처: `D/foundations/typography`

- 폰트: **Asta Sans** (Google Fonts, 한/영). "System UI에 적용된 폰트"입니다. "캐릭터가 강조된 폰트를 적용하는 것을 지양"합니다.
- 최소 크기: "최소 폰트 크기는 12sp 이상을 권장", "실제 픽셀 기준 최소 높이: 18px 이상", 행간은 "1.5배 이상"을 권장합니다.
- 주행 중 크기 (`D/Introduction/intro`): "중요 정보 … 기본 텍스트는 높이 5 mm 이상, 보조 텍스트는 최소 3 mm 이상"
- 스케일 (도표, 단위 표기는 없음. 같은 문서의 Toast 도해가 dp 기준이라 dp로 추정합니다):

| 카테고리 | Large | Medium | Small | Weight |
|---|---|---|---|---|
| Headline | 60/80 | 56/72 | 48/64 | Extra Bold, Bold |
| Title | 40/52 | 36/44 | 32/40 | Extra Bold, Bold, Regular |
| Body | 30/38 | 28/36 | 26/34 | Strong, Normal |
| Label | – | 24/28 | 20/24 | Normal |

(값은 size/line-height)

- 주행 중 텍스트 양 (`D/Introduction/intro`)
  - "영어로 된 단일 메시지 … 120자 또는 24 단어 이하"
  - "한 줄의 텍스트는 80자(영문 기준) 이하"
  - "문장 또는 단락은 텍스트 3줄을 넘지 않아야 합니다."
  - 일본어는 30자 이하
  - 한국어 기준: **문서에 없음**

## 3. 컬러

출처: `D/foundations/colors` (값은 모두 PNG 도표)

- 대비: "아이콘, 텍스트 및 기타 이미지의 대비율이 4.5:1 이상" (`D/Introduction/intro`)
- 주간/야간: "야간/주간에 따른 조명 환경에 따라 UI 밝기와 대비를 자동 조정" (`D/Introduction/intro`). 모든 System Color는 Light/Dark 값을 짝으로 가집니다.
- 토큰 이름 규칙: prefix `ivi`, snake_case, 축약하지 않음 (`D/Introduction/design-token`)
- Grayscale (Light → Dark 반전)
  - basic_00 #FFFFFF / #131417
  - basic_50 #F7F8FA / #1D1E21
  - basic_100 #EDEEF2 / #313236
  - basic_200 #E0E1E6 / #44464E
  - basic_300 #C7C9CE / #686A72
  - basic_400 #A4A8AE / #A4A8AE
  - basic_500 #686A72 / #C7C9CE
  - basic_600 #44464E / #E0E1E6
  - basic_700 #313236 / #EDEEF2
  - basic_800 #1D1E21 / #F7F8FA
  - basic_900 #131417 / #FFFFFF
- Static
  - basic_static_light_100~400 = #FFFFFF 100/84/64/32%
  - basic_static_dark_100~400 = #131417 100/84/64/32%
- Background
  - background_primary: Light basic_50 / Dark basic_50(#1D1E21)
  - background_secondary: basic_00 / basic_00(#131417)
  - background_popup: basic_00 / basic_100
- Surface
  - surface_basic: Light basic_00 / Dark basic_50
  - surface_low: basic_50 / basic_00
  - surface_high: basic_100 / basic_100
  - surface_accent: basic_200 / basic_200
- Text/Icon
  - text_dark_primary~quaternary = static_dark_100~400 (Light) / static_light_100~400 (Dark)
  - icon_* 토큰도 같은 구조
- Button
  - button_basic_enabled alpha_dark_50
  - button_filled_enabled basic_600, pressed basic_700(Light)/basic_500(Dark)
- Informative (Light / Dark)
  - active #02C265 / #00E074
  - positive #0064FF / #0082FF
  - negative #FE3D16 / #FF4C28
  - autonomous_driving #5A46FA / #6A57FF
- Media: radio_primary #C911E7, **music_primary #4781FF**, radio_gradient_start/end
- Gleo AI: gleo_bubble_primary #9BA8E2 / #545F93, gleo_bubble_speaker #3F4B81 / #B6C1F3 등
- 앱 브랜드(악센트) 색 사용 규칙: **문서에 없음**. 시스템 팔레트는 무채색 위주이고, 강조색은 Informative와 Media 토큰만 있습니다.
- Shadow (`D/foundations/shadow`)
  - L1: 0/6/24 8%
  - L2: 0/8/32 16% + 0/2/2 8%
  - L3: 0/20/40 24% + 0/2/4 20%

## 4. 터치

- 최소 터치 타겟 크기와 간격: **문서에 없음**
  - "주행 안전을 고려한 설계" 페이지는 "버튼 크기와 터치 영역 간 간격은 Pleos Connect UI Design Guide의 기준을 따릅니다"라고 하지만, 정작 디자인 가이드(Basic button 페이지 포함)에는 수치가 없습니다.
  - 출처: `https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline`
  - 같은 페이지에 "큰 터치 영역(large touch target), 높은 대비(high contrast), 텍스트 최소화 패턴을 적용"하라는 문장이 있습니다.
- Press 피드백 (`D/foundations/interaction`): "버튼 및 터치 가능한 요소는 모두 shrink effect가 적용", "Pressed : Change to scale down(95%) + background color by Mode". 터치 영역 밖으로 나가면 cancel.
- 제스처: Long press 2~3초는 편집 모드. 스크롤 끝에서는 bounce를 주고, overscroll은 전체의 약 2/5까지.
- 햅틱/음성 피드백: "터치 시 진동 피드백(Haptic Feedback)이나 음성 피드백을 제공" (`D/Introduction/intro`)

## 5. 컴포넌트

크기 수치는 모든 컴포넌트 페이지에서 **문서에 없음**입니다. 도해만 있습니다.

- Basic button (`D/components/actions/basic-button`)
  - 구성: Root / Label / Prefix·Suffix 아이콘
  - 상태: Pressed / Disabled / Loading(로더를 보여주고 조작 불가)
  - 위계: Primary(밝은 회색 채움), Secondary(진회색 basic_600 채움)
  - 크기: L/M/S, 둥근 사각형(라운드 사각)
- Icon button (`D/components/actions/icon-button`): Style Basic/Plain/Circle, Size L/M/S
- Text button: Medium/Small, 링크 용도
- Toggle button: Toggled 상태, Floating(그림자) 옵션
- Tabs (`D/components/segmented-menu`): Box/Line, Fitted
- Page Navigation (`D/components/page-navigation`): **앱 상단 헤더 = App icon + Primary Menu(Dropdown) + Secondary + Suffix 아이콘**. Depth 타입이면 Back 버튼이 나옵니다.
- Dropdown, Text/Number field, Checkbox, Radio, Switch, Keypad: 상태와 속성 표만 있음
- System Popup (`D/components/popups/system-popup`): Title / Description / Confirm / Cancel, Size L/M, 버튼 정렬 Single/Vertical/Horizontal
- Toast / Toast Popup: 위 1장 참고 (24 패딩, max 792)
- Spinner: 날짜/시간 피커(로딩 스피너 아님)
- 로딩/진행 표시 (`D/foundations/animation`): Loading 애니메이션 Linear(정적 로딩) / Tension(긴 로딩). Lottie 다운로드 zip 있음. Progress bar 규격: **문서에 없음**
- Controllers (`D/components/controllers`): Continuous/Centered slider와 Stepper. Volume 구간은 Max 100 / High 76~100 / Mid 26~75 / Low 1~25 / Min 0
- Bar: Window bar / Scroll bar
- Indicator(페이지 점), Widgets, App library(Connect-S에서 1줄 최대 7개), GNB, Status bar, System/Vehicle Notification
- 리스트와 카드: 독립 컴포넌트 페이지 **없음** (Gleo 패널의 list/card 템플릿에서만 언급)
- 아이콘 (`D/foundations/contents-creation/iconography`): "기본 사이즈(48 x 48dp)를 기준으로 작성된 그리드", Filled/Outlined 2종, "텍스트 라벨과 함께 제공"(intro)

## 6. 미디어

- 미디어 전용 화면, Now playing, 미니플레이어 패턴: **디자인 가이드에 없음** (Media Center는 시스템 UI)
- 관련 근거
  - Media 컬러 토큰(music_primary #4781FF, radio_primary #C911E7)
  - GNB OSD: 스티어링 휠로 미디어나 사운드를 조작하면 OSD가 뜨고 2초 뒤 자동으로 사라짐 (`D/components/gnb`)
  - Connect-W에서는 secondary 창에 마지막으로 쓴 Media 앱이 기본으로 뜸 (`D/getting-started/window`)
- 앨범아트 규격: **문서에 없음**
- 설계 가이드 (driver-distraction-guideline): "음악 앱은 프리셋 플레이리스트, 최근 재생 및 즐겨찾기와 같이 자주 사용하는 기능을 첫 화면에 배치"

## 7. 주행 규칙 / 음성 / 모션

- 주행 중 표시 (`D/Introduction/intro`, driver-distraction-guideline)
  - 동영상, 애니메이션, 자동 스크롤 금지
  - 장식적이거나 반복해서 움직이는 요소는 지양
  - CarUxRestrictions에 따라 UI를 축소하거나 비활성화
  - 텍스트 양은 2장 참고
- 음성 UI, Gleo 상태 모델 (`D/Experiences/gleo-ai`)
  - Sleep → Listening_Passive("음성 미입력 2초 후 '운전석에서 듣는 중...' … 메시지 표시") → Listening_Active → Thinking → Asking → Speaking → Standby(응답 화면을 5초 유지한 뒤 Sleep)
  - "사용자의 발화를 들을 수 있는 상태와 Gleo가 응답하는 상태를 명확히 구분"
  - Chrome(빛 효과)은 Lottie로 만들고 상태별로 애니메이션
  - Gleo 패널은 GNB 바로 아래 레이어에 떠서 앱을 가릴 수 있음
- 핸즈프리: "음성 대화를 시작, 종료 또는 중단하는 작업은 수동으로 실행할 수 있습니다." (intro)
- 모션 (`D/foundations/interaction`, 도표)
  - Easing: cubic-bezier(0.16, 1, 0.3, 1)
  - Duration: short1 100ms, short2 150ms, medium1 250ms, medium2 300ms, long1 400ms, long2 450ms (`ivi_motion_duration_*`)
  - 주행 중 모션의 상한: **문서에 없음**. "최소화/제한"이라는 원칙만 있습니다.

## 8. 브랜딩 / 앱 아이콘

출처: `D/foundations/contents-creation/app-icon`

- 사용처: GNB, App library, App Market
- "Foreground(로고 및 주요 그래픽)와 Background(배경)를 분리하여 제작", "Android Adaptive Icon 형식"으로 만듭니다. 차량 브랜드별 마스킹을 적용합니다(도해상 둥근 사각형과 각진 사각형).
- 그리드와 마스크 템플릿은 "Pleos Connect Design Assets" Figma에 있습니다. 픽셀 수치: **문서에 없음** (Android Adaptive Icon 가이드를 참조하라고 함)
- 테마 (`D/Introduction/theme`): 아이콘, 폰트, 컬러로 브랜드 테마를 바꿉니다. 이미지만 있음.

## 9. 다운로드

| 자료 | 링크 | 비고 |
|---|---|---|
| Pleos Connect display guide (.fig) | https://document.pleos.ai/img/Pleos_Connect_display_guide_215.fig | 44MB, HEAD 200 확인 |
| Pleos Connect design assets (.fig) | https://document.pleos.ai/img/Pleos_Connect_design_assets_215.fig | 2.5MB, 앱 아이콘 그리드와 마스크 포함 |
| Loading 애니메이션 (zip) | https://document.pleos.ai/assets/files/Loading-a564bf9ddfbe2fa3c9640ec7708d9e1d.zip | 320KB |
| 3D 샘플 (Figma) | https://www.figma.com/design/FYc6zTha14GLPvqsb1zmkD/... (`D/foundations/contents-creation/3d-contents`) | "[Internal] … (WIP)"라서 외부 접근이 안 될 수 있음 |
| Asta Sans | https://fonts.google.com/specimen/Asta+Sans | |

## 10. 접근 실패 / 수동 확인 필요

- 42개 페이지 모두 HTTP 200이고 본문은 정적 HTML에 들어 있었습니다(JS 청크 추출은 필요 없었음). **로드 실패나 빈 페이지는 없습니다.**
- 대신 수치가 이미지에만 있는 페이지가 많습니다. 텍스트 경로(`<text>`)가 없는 SVG입니다. 렌더링해서 확인한 것은 layout, typography, colors(일부), interaction, button 도해입니다.
- 렌더링하지 않아 수치를 확인하지 못한 이미지도 있습니다. 직접 확인을 권장합니다.
  - colors의 Controller / Climate / Dropdown / Dim / Driving / Fields / Keyboard / Overlay / Phone / Regulation / Switch / White·Gray Alpha 표
  - 각 컴포넌트의 size/behavior 도해
  - gleo-ai 레이아웃
  - theme
- Figma 3D 샘플 링크는 "[Internal]" 파일이라 공개 접근이 불확실합니다(열어 보지 않았음).
- AAOS 외부 가이드(https://source.android.com/docs/automotive/driver_distraction/guidelines)는 curl로 받은 본문에서 수치를 추출하지 못했습니다.

---

# 다다캐스트 화면 점검 (읽기 전용)

## 점검 대상

- 코드
  - `app/src/main/java/ai/dadacast/app/ui/DadaCastApp.kt`
  - `MainActivity.kt`
  - `playback/DadaCastService.kt`(메타데이터, 아트)
  - `res/`
- 스크린샷: 에뮬레이터 2560x1440 @160dpi (Connect-S-17)
  - `current.png`(로컬 캡처, 저장소 미포함): 지금 화면. Media Center가 로딩 스피너 상태이고 시스템은 Light 테마
  - 기존 캡처 `final_custom_ui.png`, `final_miniplayer.png`(로컬, 저장소 미포함): 이전 세션 캡처라서 아트 변경(`svc.before_art.kt`) 전일 수 있음

## 요약 관찰

- 시스템 UI(Status bar, Driving view, Media Center)는 **Light 테마**인데, 우리 화면은 고정 다크(#10131A)입니다. 분할 화면에서 우리 창만 검은 판처럼 보입니다.
- 우리 화면에는 Pleos 표준 헤더(Page Navigation: 앱 아이콘 + 제목)가 없습니다. 반면 같은 창의 Media Center는 헤더가 있습니다.
- 글자 크기가 Pleos 스케일에 없는 값입니다(44/38/34sp).
- 대비는 대체로 충분합니다(흰 글자/Surface 15.9:1, Muted/Surface 8.3:1, Accent/Background 10:1).
- 앨범아트가 런처 foreground(투명 배경, 108 중 36dp 크기의 청록 글리프)를 256px로 래스터화한 것입니다. Light Media Center의 밝은 회색 박스 위에서는 대비가 약 1.6:1이고 작아서 거의 보이지 않습니다.

## 갭 리스트

| # | 화면/요소 | 현재 값 (file:line) | 가이드 규칙 (출처) | 수정 제안 | 우선순위 |
|---|---|---|---|---|---|
| 1 | Media Center 아트 / 미니플레이어 아트 | `DadaCastService.kt:79` `ic_launcher_foreground` → `toBitmap(256,256)` (투명 배경, 글리프 약 33%), `:361-362` ART_URI도 같은 foreground | 대비 4.5:1 이상 (`D/Introduction/intro`). 아트 규격은 문서에 없음 | 불투명 배경(앱 아이콘 배경색 또는 basic 토큰)에 글리프를 60% 이상 크기로 넣은 전용 정사각 아트(예: 512px PNG)를 만들고 ART와 DISPLAY_ICON에 사용. Light Media Center의 #F0F0F0 위 청록 글리프는 1.6:1 | **H** |
| 2 | Media Center 탐색 목록 항목 | `DadaCastService.kt:160-169` MediaDescription에 icon/iconUri 없음 | 아이콘은 텍스트와 함께, 일관성 (`D/Introduction/intro`) | 에피소드와 카테고리 노드에 `setIconUri`/`setIconBitmap`(카테고리별 아트) 추가. 시스템 목록에 썸네일이 생겨 데모 완성도가 올라감 | M |
| 3 | 앱 전체 테마 | `DadaCastApp.kt:49-52, 61` 고정 다크 팔레트, `themes.xml:3-9` | 주간/야간 자동 조정, Light/Dark 토큰 쌍 (`D/foundations/colors`, `D/Introduction/intro`) | `isSystemInDarkTheme()`(또는 UiModeManager)로 Light/Dark를 나누고 Pleos 토큰을 매핑. Light는 background basic_50 #F7F8FA, surface basic_00 #FFFFFF, text #131417. Dark는 background #131417/#1D1E21, surface_high #313236 | **H** |
| 4 | 배경과 카드 색 | `DadaCastApp.kt:49-50` #10131A / #1D222C | Grayscale 토큰 (`D/foundations/colors`) | Dark 기준 background_secondary basic_00 #131417, 카드는 surface_high #313236 또는 basic_50 #1D1E21 | M |
| 5 | 강조색 | `DadaCastApp.kt:51` Accent #4FD1C5 (청록, 팔레트 밖) | 앱 악센트 규칙은 문서에 없음. Media 토큰 music_primary #4781FF (`D/foundations/colors`) | 브랜드색 유지는 가능. 다만 Light 테마에서 청록 텍스트는 흰 배경 대비 1.87:1로 **4.5:1 미달**. Light에서는 진한 변형을 쓰거나 텍스트 대신 채움 배경에만 사용 | M (3번과 함께) |
| 6 | 상단 헤더 / 뒤로가기 | `DadaCastApp.kt:144` 목록 제목 Text만 있음, `:181` 112dp 원형 Back 버튼 단독 | Page Navigation = App icon + Primary menu + Suffix, Depth면 Back (`D/components/page-navigation`) | 두 화면에 공통 헤더(앱 아이콘 + "다다캐스트" + Depth일 때 Back) 추가. 옆의 Media Center 헤더와 시각적으로 맞추기 | M |
| 7 | 타이포 스케일 | `DadaCastApp.kt:107,114,121,122,126,144,160,168,196,203,209` = 40/38/26/34/34/44/34/30/44/36/28sp | Title 40/36/32, Body 30/28/26, Label 24/20 (`D/foundations/typography`) | 매핑: 화면 제목 44 → Title L 40, 재생 제목 44 → Title L 40, 목록 항목 34 → Title S 32 또는 Body L 30, 상태 36 → Title M 36, 부가 30/28/26 → Body L/M/S, 연습 문구 38 → Title M 36. 행간도 토큰 값으로 | M |
| 8 | 폰트 | 시스템 기본 (`DadaCastApp.kt` 전체, fontFamily 미지정) | Asta Sans (`D/foundations/typography`) | Asta Sans를 res/font에 넣고 Typography에 적용. 단, 에뮬레이터 시스템 UI가 이미 이 폰트라면 기본값을 유지해도 차이가 작음. 캡처로 비교 권장 | L |
| 9 | 버튼 press 피드백 | `DadaCastApp.kt:154, 254` `clickable` 기본 ripple | "scale down(95%) + background color by Mode" (`D/foundations/interaction`) | ripple 대신 press 시 scale 0.95 + 배경을 button_*_pressed 색으로 바꾸는 공통 Modifier 적용. 심사위원이 터치하면 바로 느끼는 부분 | M |
| 10 | 버튼 형태 / 위계 | `DadaCastApp.kt:254` 모든 컨트롤이 CircleShape, 재생 버튼은 Accent 채움 | Icon button Style Basic/Plain/Circle, Basic button은 라운드 사각, Primary/Secondary 위계 (`D/components/actions/*`) | 재생/이전/다음은 Circle 허용. Back은 Plain 아이콘 버튼(헤더 안)으로. 목록 항목은 현재 카드 형태 유지 가능 | L |
| 11 | Talk 화면 상태 표현 | `DadaCastApp.kt:84-110` listening/speaking 2가지 상태만, 판정 대기 표시 없음 | Gleo 상태 구분 Listening_Passive/Active/Thinking/Speaking, "듣는 상태와 응답 상태를 명확히 구분" (`D/Experiences/gleo-ai`). 응답이 0.25초를 넘으면 진행 상태 표시 (`D/Introduction/intro`) | 발화 종료 뒤 판정 전까지 "확인 중" 상태(마이크 원 색이나 아이콘 변경, 정적 표시)를 추가. 듣기 상태와 들려주기 상태의 색 대비를 키우기(현재 speaking은 Surface #1D222C에 Muted 아이콘이라 비활성처럼 보임). 원한다면 Gleo 색(gleo_bubble_*)을 차용 | M |
| 12 | Talk 화면 텍스트 양 | `DadaCastApp.kt:105-127` 제목 + 목표 문장(3줄) + "들은 말" + 전사(2줄) + 판정 = 최대 7줄 이상 | 단락 3줄 이하, 짧은 문장 (`D/Introduction/intro`) | 정차 중에만 노출되니 규칙 위반은 아님. 다만 한눈에 보도록 목표 문장 2줄, 전사 1줄로 줄이고, 판정은 아이콘 + 짧은 라벨로 | L |
| 13 | 진행 바 | `DadaCastApp.kt:215-220` LinearProgressIndicator, 12dp, Accent | 미디어 진행 바 규격은 문서에 없음 | 유지. Light 테마 대응 때 트랙 색만 토큰(basic_200)으로 | L |
| 14 | 화면 바깥 여백 | `DadaCastApp.kt:62` 전체 padding 32dp | 앱 마진은 문서에 없음. Toast 등은 24 사용 | 24dp로 맞추면 시스템 Toast와 정렬됨(선택) | L |
| 15 | 앱 아이콘 | `res/mipmap-anydpi-v26/ic_launcher.xml:3-4` 단색 배경 #10131A + foreground; `ic_launcher_foreground.xml:2` 글리프가 108dp 캔버스의 약 36dp | Foreground/Background 분리, Adaptive Icon, 브랜드 마스킹, 그리드는 Figma 템플릿 (`D/foundations/contents-creation/app-icon`) | 형식은 충족. 디자인 에셋 Figma 그리드로 글리프를 키우고(Adaptive 안전영역 66dp 안에서 약 48~56dp), Material 기본 헤드폰 대신 다다캐스트 고유 심볼로. GNB에서는 지금도 식별 가능함 | M |
| 16 | 아이콘 라벨 | `DadaCastApp.kt:191` 아트 영역 아이콘 contentDescription null. 컨트롤 버튼은 라벨이 시각적으로 없음 | "텍스트 라벨과 함께 제공" (`D/Introduction/intro`) | 재생 컨트롤은 국제 표준 아이콘이라 괜찮음. Back은 헤더에 "목록" 텍스트를 함께 | L |
| 17 | 목록 개수 / 길이 | `DadaCastApp.kt:142, 159` UxRestrictions 기반 제한 있음 | CarUxRestrictions 준수 (driver-distraction-guideline) | 충족(갭 아님) | – |
| 18 | 터치 타겟 | `DadaCastApp.kt:53` 112dp, 재생 136dp, 목록 최소 높이 112dp | 수치는 문서에 없음. "큰 터치 영역" 원칙 | 충족. 간격 64dp도 충분 | – |
| 19 | 움직임 | 무한 애니메이션 없음 (Compose 기본만) | 주행 중 애니메이션 최소화, easing cubic-bezier(0.16,1,0.3,1), 100~450ms (`D/foundations/interaction`) | 화면 전환 애니메이션을 추가한다면 이 토큰을 사용. 현재는 갭 없음 | – |

### 데모 관점 우선순위

1. **H**: 1번 앨범아트(Media Center와 미니플레이어에 가장 크게 노출), 3번 Light/Dark 테마 대응(분할 화면에서 우리 창만 튐)
2. **M**: 6번 헤더, 7번 타이포 스케일, 9번 press 효과, 11번 Talk 상태 표현, 15번 아이콘, 2번 탐색 썸네일, 4~5번 토큰 색
3. **L**: 나머지
