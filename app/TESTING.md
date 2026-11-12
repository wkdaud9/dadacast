# 다다캐스트 에뮬레이터 테스트 안내

이 문서는 Pleos Connect 에뮬레이터에서 다다캐스트를 직접 확인하는 순서입니다.
각 단계 끝의 **확인할 것**이 보이면 성공입니다.

표시 규칙
- ✅ 개발 중 에뮬레이터에서 실제로 해 본 단계
- ⚠️ 해 보지 못했거나 일부만 확인한 단계 (이유를 함께 적었습니다)

모든 명령은 macOS 터미널에서 실행합니다. 처음에 한 번 아래를 입력해 두면 `adb`를 바로 쓸 수 있습니다.

```sh
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
```

---

## 1. 준비

### 1-1. 에뮬레이터 켜기 ✅
```sh
~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2.1 -no-snapshot-load -no-boot-anim
```
- 처음 켤 때는 몇 분 걸립니다. 홈 화면이 나올 때까지 기다리세요.
- 실제 마이크로 말하기 연습을 해 보려면 `-allow-host-audio`를 붙여서 켭니다(4-6 참고).

**확인할 것:** `adb devices` 결과에 `emulator-5554  device`가 보임

### 1-2. 앱 설치 ✅
```sh
cd app   # 저장소 루트 기준
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
**확인할 것:** `Success`

### 1-3. 처음 실행: 정보 사용 동의 ✅
adb로 권한을 따로 켤 필요가 없습니다. 차가 **P**일 때 진행하세요. 동의 화면은 주차 중에만 열립니다.

1. 아래 앱 목록에서 **다다캐스트**를 누릅니다.
2. 미디어 센터에 "시작하기 전에 확인해 주세요"와 **시작하기** 버튼이 보이면 누릅니다.
3. "다다캐스트가 쓰는 정보" 화면에서 **동의하기**를 누릅니다.
4. Android 권한 창이 뜨면 **허용**을 누릅니다.
   - 시스템은 차량 속도 권한을 "위치 정보"라는 이름으로 묻습니다.

**확인할 것:** 미디어 센터에 에피소드 목록이 나타남

선택에 따라 달라지는 동작

| 선택 | 동작 |
|---|---|
| 동의 + 허용 | 모든 기능. 차량 속도가 4초 이상 계속 0이면 신호 대기로 판단 |
| 동의 + 허용 안함 | 브레이크를 2초 이상 밟고 있으면 신호 대기로 판단 |
| 동의하지 않기 | 에피소드만 재생. 내비 안내 음성이 나올 때만 멈추고, 방향지시등·회전 안내·말하기 연습은 쓰지 않음 |

나중에 바꾸려면
- 다다캐스트 화면 오른쪽 위 **설정**을 누릅니다(주차 중에만 보임).
- 또는 아래 명령으로 바로 엽니다.
  ```sh
  adb shell am start -n ai.dadacast.app/.ui.ConsentActivity
  ```
- 처음 상태로 되돌리려면 앱을 지우고 다시 설치합니다.
  ```sh
  adb uninstall ai.dadacast.app
  adb install app/build/outputs/apk/debug/app-debug.apk
  ```

### 1-4. 가상 콘솔 연결 ✅
```sh
adb forward tcp:8088 tcp:8088
```
- 브라우저에서 http://127.0.0.1:8088 을 열면 기어·속도·방향지시등을 바꿀 수 있습니다.
- 브라우저 화면을 열어 둔 동안에는 연결이 유지됩니다.
- ⚠️ 개발 중에는 브라우저 대신 같은 기능을 터미널 명령으로 썼습니다. 아래 단계마다 터미널 명령도 함께 적었습니다.
- 터미널 명령만 쓸 때는 연결이 끊기지 않게, 별도 터미널 창에서 아래를 켜 두세요.
  ```sh
  while true; do curl -s -X POST 'http://127.0.0.1:8088/browser/session?client=me' >/dev/null; sleep 1; done
  ```

자주 쓰는 터미널 명령
```sh
VC=http://127.0.0.1:8088
curl -s -X POST "$VC/vehicle/gear?gear=Drive"                         # D
curl -s -X POST "$VC/vehicle/gear?gear=Park"                          # P
curl -s -X POST "$VC/vehicle/controls?speed_kph=40&accelerator=30&brake=0"   # 시속 40km로 주행
curl -s -X POST "$VC/vehicle/controls?speed_kph=0&accelerator=0&brake=30"    # 신호 대기처럼 정차
curl -s -X POST "$VC/vehicle/turn-signal?mode=BlinkLeft"              # 왼쪽 방향지시등
curl -s -X POST "$VC/vehicle/turn-signal?mode=Off"                    # 방향지시등 끄기
```

---

## 2. 기본 기능

2-5, 2-7, 2-8은 1-3에서 **동의**했을 때만 동작합니다. 동의하지 않았다면 2-6(내비 안내 음성)만 동작합니다.

### 2-1. 미디어 센터에서 재생 ✅
1. 아래 앱 목록(네모 네 개 아이콘)에서 **다다캐스트**를 누릅니다.
2. 미디어 센터가 열리고 위쪽에 **영어 회화 / 면접 연습 / 내 자료** 탭이 보입니다.
3. 에피소드 하나를 누릅니다.

**확인할 것:**
- 목록과 '현재 재생 중' 화면에 다다 얼굴 그림(영어 회화는 파랑, 면접 연습은 보라, 내 자료는 초록)이 보임
- '현재 재생 중' 화면에 에피소드 이름과 "재생 중"이 보임
- 진행 바가 움직이고 소리가 나옴

같은 화면을 터미널 명령으로 여는 방법:
```sh
adb shell am start -a android.car.intent.action.MEDIA_TEMPLATE --es android.car.intent.extra.MEDIA_COMPONENT ai.dadacast.app/ai.dadacast.app.playback.DadaCastService
```

### 2-2. 왼쪽 미니 플레이어
- ✅ **화면:** 왼쪽 패널 아래에 에피소드 이름과 상태가 보입니다. 패널이 다른 페이지면 옆으로 넘겨서 확인하세요.
- ⚠️ **버튼을 손으로 누르기:** 에뮬레이터에서는 버튼 터치가 위젯까지 가지 않을 수 있습니다(아래 "미니 플레이어 버튼이 안 먹음" 참고). 이때는 앱 문제가 아니므로 미디어 센터 버튼이나 2-3 미디어 키로 확인합니다. 버튼이 먹는 상태라면:
  - 일시정지를 누르면 상태가 "일시정지"로 바뀌는지
  - 다음을 누르면 에피소드가 바뀌는지
  - 이전을 누르면 지금 문장을 처음부터 다시 들려주는지(2-13)

### 2-3. 미디어 키 ✅
```sh
adb shell input keyevent 127   # 일시정지
adb shell input keyevent 126   # 재생
adb shell input keyevent 87    # 다음 에피소드
adb shell input keyevent 88    # 문장 다시 듣기(2-13)
```
**확인할 것:** 상태 글자와 소리가 키에 맞게 바뀜

### 2-4. 다른 화면으로 가도 계속 재생 ✅
재생 중에 홈 버튼을 누르거나 다른 앱을 엽니다.

**확인할 것:** 소리가 계속 나오고 미니 플레이어도 "재생 중"

### 2-5. 방향지시등을 켜면 잠시 멈춤 ✅
1. 재생 중에 가상 콘솔에서 방향지시등을 켭니다(`BlinkLeft`).
2. 몇 초 뒤 끕니다(`Off`).

**확인할 것:**
- 켜면 지금 읽던 문장까지만 듣고 멈추며, 상태가 "방향지시등이 켜져 잠시 멈췄어요"로 바뀜
- 끄면 "곧 이어서 들려드릴게요"가 보이고 약 1.5초 뒤 **다음 문장부터** 이어짐

### 2-6. 내비 안내 음성이 나오면 잠시 멈춤 ✅ (다른 검증에서 확인)

Gleo 음성 비서가 말할 때도 같은 방식으로 멈춥니다. 다만 에뮬레이터에서는 Gleo 음성 대화를 띄우지 못해, 아래 개발용 명령으로 같은 상황을 흉내 내 확인했습니다.
```sh
adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action take     # 다른 음성 시작
adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action release  # 끝
```
1. **내비게이션** 앱 → 검색 → 최근 목적지 **42dot** → **안내 시작**을 누릅니다.
2. 안내 음성이 나오는 동안 다다캐스트를 지켜봅니다.

**확인할 것:**
- 안내 음성이 나오는 동안 "다른 음성이 나와서 잠시 멈췄어요"
- 음성이 끝나면 **끊긴 그 문장부터** 다시 들림

### 2-7. 회전 구간 앞에서 멈춤
에뮬레이터는 차를 경로를 따라 움직일 수 없어서, 회전 안내는 처음 한 번만 옵니다. 그래서 두 가지 방법을 적었습니다.

**(가) 실제 내비게이션 ⚠️ 일부 확인**
- 2-6처럼 안내를 시작하면 로그에 회전 안내가 한 줄 찍힙니다.
- 첫 회전이 300m보다 멀면 멈추지 않는 것이 정상입니다. 예를 들어 42dot 경로는 700m입니다.
- 내비 샘플 앱(NaviDevApp)에서 `requestRoute`의 **Execute**를 눌러도 회전 안내가 한 번 들어옵니다.

**(나) 터미널로 흉내 내기 ✅** (실제 내비가 아닌 시뮬레이션입니다)
```sh
# 150m 앞 우회전이 있다고 알림
adb shell am broadcast -n ai.dadacast.app/.debug.TurnGuidanceReceiver --es type TURN_RIGHT --ei distance 150
# 회전이 끝났다고 알림
adb shell am broadcast -n ai.dadacast.app/.debug.TurnGuidanceReceiver --es type none
```

**확인할 것:**
- 알림 직후에는 계속 재생되다가, 지금 묶음(청크)이 끝나는 곳에서 "회전 구간이 가까워 잠시 멈췄어요"가 뜸
- `none`을 보내면 다음 묶음 처음부터 이어짐

참고
- 15초 동안 새 회전 안내가 없으면 앱이 스스로 해제합니다. 묶음 끝이 15초보다 멀면 멈춤이 보이지 않을 수 있으니, 묶음이 거의 끝날 때 보내세요.
- "도착" 안내(`TURN_ARRIVE`)에는 멈추지 않습니다.

### 2-8. 주행/정차/주차 전환 ✅
| 가상 콘솔 설정 | 앱이 보는 상태 | 로그 |
|---|---|---|
| D, 시속 40km | 주행 중 | `car is NONE`, `requiresDO=true active=80` |
| D, 시속 0km (브레이크) | 신호 대기 (4초 이상 서 있을 때) | `car is RED_LIGHT`, `requiresDO=true active=80` |
| P | 주차 | `car is PARKED`, `requiresDO=false active=0` |

**확인할 것:** 어느 상태에서도 재생은 끊기지 않음

⚠️ 단위 테스트만: 속도로 판단할 때는 시속 0km가 **4초 이상 이어져야** `car is RED_LIGHT`가 찍힙니다. 4초가 되기 전에 다시 움직이면(가다 서다 하는 정체) 신호 대기로 보지 않고, 움직이는 순간 바로 `car is NONE`으로 돌아갑니다. 시속 0km로 바꾼 뒤 4초쯤 지나 로그가 찍히는지 확인하세요.

### 2-9. 끊긴 뒤 어디서부터 다시 듣는지 ✅
- **문장 중간에 끊기면** (안내 음성, 일시정지): 그 문장 처음부터 다시 듣습니다.
- **문장이 끝난 뒤나 문장 사이 쉬는 시간에 끊기면** (방향지시등, 회전 구간): 다음 문장부터 듣습니다.
- 묶음의 앞 문장이나 첫 소개 문장으로 되돌아가지 않습니다.

확인 방법
1. 재생이 세 번째 묶음 이후로 갔을 때(로그에 `chunk=3`) 아래를 실행합니다.
   ```sh
   adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action take     # 안내 음성 흉내
   adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action release  # 끝
   ```
2. 로그에서 멈췄던 `chunk=… sentence=…`와 다시 말한 문장이 같은지 봅니다.

---

### 2-10. 앱을 다시 열거나 다시 켰을 때 이어 듣기 ✅
1. 에피소드를 듣다가 일시정지합니다.
2. 앱 프로세스를 끝냅니다(시동을 껐다 켠 것과 비슷한 상황).
   ```sh
   adb shell am force-stop ai.dadacast.app
   ```
3. 미디어 센터를 다시 열고 재생(▶)을 누릅니다.

**확인할 것:**
- 로그에 `restored …, paused`가 찍힘
- 저절로 재생되지는 않음
- ▶를 누르면 멈췄던 문장부터 다시 들림

### 2-11. 시동을 끌 때(절전) ✅
```sh
adb shell cmd car_service suspend --simulate --skip-garagemode --wakeup-after 8
```
**확인할 것:**
- 소리가 바로 멈추고 "일시정지"가 됨(로그 `focus change -1`, `focus abandoned`)
- 다시 켜진 뒤에도 저절로 재생되지 않음

### 2-12. 밝은 화면 / 어두운 화면 ✅
```sh
adb shell cmd car_service day-night-mode night   # 어두운 화면
adb shell cmd car_service day-night-mode day     # 밝은 화면
```
**확인할 것:** 다다캐스트 화면도 시스템을 따라 밝게 또는 어둡게 바뀜

### 2-13. 이전 버튼 = 문장 다시 듣기 ⚠️ 단위 테스트만
미니 플레이어·미디어 센터의 이전(⏮) 버튼과 미디어 키 88은 에피소드를 바꾸지 않고 **문장을 다시 들려줍니다.** 에피소드는 미디어 센터 목록이나 다음(⏭)으로 바꿉니다.
- 문장이 나온 지 2초가 지났으면: 그 문장을 처음부터 다시
- 문장이 막 시작했거나(2초 안), 문장 뒤 쉬는 시간이 아니라 아직 시작 전이면: 바로 앞 문장으로. 묶음 첫 문장이면 앞 묶음의 마지막 문장으로, 에피소드 첫 문장이면 그 문장을 다시
- 문장이 끝난 뒤 쉬는 시간에 누르면: 방금 끝난 문장을 다시
- 일시정지 중에 누르면 위치만 한 문장 앞으로 옮기고 재생하지 않음. 방향지시등·안내 음성으로 멈춘 동안에도 위치만 옮기고, 멈춤이 풀리면 그 문장부터

**확인할 것:** 로그의 `sentence=`가 위 규칙대로 바뀌고 같은 문장이 다시 들림

### 2-14. 음성 합성이 실패할 때 ⚠️ 단위 테스트만
- 한 문장에서 음성 합성이 실패하면 0.5초 뒤 같은 문장을 한 번 더 말합니다.
- 또 실패하면 그 문장을 건너뛰고 다음 문장으로 갑니다.
- 연달아 세 문장을 건너뛰어야 할 때만 "오류" 상태로 멈춥니다. ▶를 누르면 멈춘 문장부터 다시 시도합니다.
- 다시 시도하기 전 0.5초 사이에 일시정지하면 그대로 멈춰 있고, 방향지시등 등으로 멈춰야 할 때는 멈춤이 풀린 뒤에 다시 시도합니다.

**확인할 것:** 로그 `speech failed: …` 뒤에 같은 문장의 `speak […]`가 한 번 더 찍힘

## 3. 말하기 연습 (정차 중에만)

### 원리
- 1-3에서 **동의**했을 때만 동작합니다.
- 연습은 모두 **목소리로** 진행됩니다. 화면을 볼 필요가 없습니다.
- 연습은 **차가 서 있을 때만**, 지금 묶음이 끝나는 곳에서 시작합니다.
  - 신호 대기: D, 시속 0km로 4초 이상
  - 주차: P
- 대답하지 않아도 됩니다. 말이 전혀 없으면 "괜찮아요. 이어서 들을게요."라고 하고 에피소드를 이어 갑니다.
- 대답 한 번 = 12초, 문장이 화면 줄로 3줄 이상이면 줄마다 3초 더(최대 24초). 예: 3줄 15초, 4줄 18초. 음성 인식은 말 사이 1초 남짓 쉬면 끝나 버리므로, 앱이 대답을 이어서 듣습니다.
  - 인식이 끝났을 때 예시 문장의 70% 미만만 들렸고, 한 번이라도 목소리가 있었고, 1초 이상 남았으면 → 인식을 다시 열고 들은 말을 이어 붙임
  - **아직 마지막 줄에 닿지 않았으면 쉬어도 끝나지 않습니다.** 화면도 그 줄에 그대로 있고, 시간이 다 될 때까지 계속 듣습니다(인식 오류가 나도 3번까지 다시 엶)
  - 대답이 끝나는 때: 예시 문장의 70% 이상을 들었을 때, 마지막 줄에 닿은 뒤 2초 동안 조용할 때, 시간이 다 됐을 때. 이어 붙인 말 전체로 판정합니다
  - 시간이 다 됐을 때 말하던 중이면, 그때까지 들은 말로 판정합니다
  - 단어는 느슨하게 맞춥니다: 철자가 60% 이상 같거나, 4글자 이상 단어가 앞 3글자가 같으면 같은 단어로 침(예: "pussie" = "push"). 판정("좋아요/거의/엉뚱")과 70% 판단 모두 이 기준. 한국어는 두 글자 조각이 정확히 같아야 함
  - Pleos STT 문서에는 무음 시간(EPD)을 바꾸는 설정이 없습니다(docs/pleos_api/tts_stt.md)
- 연습 중 차가 움직이면(시속 40km 등) 듣기와 말하기를 바로 멈추고 다음 묶음으로 넘어갑니다.
- 미디어 센터 재생 화면에 말해 볼 문장이 보입니다. **시간이 지나서 넘어가는 일은 없습니다.** 안내 → 상대 질문 → 듣기 → 확인 중 → 피드백 내내 같은 문장입니다.
  - 문장을 첫 줄(큰 글씨, 한글 약 18자·영문 약 28자)에 들어가는 줄들로 나눕니다(단어 사이에서만, 마지막 줄이 한 단어만 남지 않게 고르게). 내장 연습 문장 92개: 1줄 17, 2줄 55, 3줄 18, 4줄 2
  - **올라가는 줄**: 첫 줄 = 지금 말할 줄, 둘째 줄 = 다음 줄(미리 보기). 마지막 줄에서는 둘째 줄 = 상태("따라 말해 보세요" → "듣고 있어요" → "확인 중", 마이크 입력이 없으면 "마이크 입력이 없어요")
  - 듣는 동안 실시간으로 알아들은 말을 따라 한 줄씩 위로 올라감(노래방처럼). 다음 중 하나면 다음 줄로: 지금 줄 단어의 50% 이상을 말함 / 지금 줄의 마지막 두 단어 중 하나를 말함 / 다음 줄 단어를 하나라도 말함. 순서대로 맞추고, 건너뛴 단어는 괜찮고, 뒤로는 돌아가지 않음
  - 마이크가 열리기 전과 듣기가 끝난 뒤(확인 후 피드백·예시 문장)에는 첫 줄부터 다시 보임
  - 연습이 끝나거나 차가 움직이면 곧바로 에피소드 제목과 "재생 중"으로 돌아옴. 에피소드 목록 강조와 미니 플레이어도 같은 에피소드를 가리킴
- 다다캐스트 화면을 직접 열어 두었다면 그 화면에도 연습이 보입니다. 연습 때문에 화면이 저절로 열리지는 않습니다.
  - "다다가 말하고 있어요": 스피커 그림
  - "말씀해 보세요": 파란 마이크
  - "확인 중": 점 세 개. 말을 마친 뒤 판정이 나오기 전 잠깐
  - 그 아래 말해 볼 문장, 들은 말, 결과("좋아요!", "거의 맞았어요", "다시 들어 보세요")

### 3-1. 신호 대기 연습 ✅ (녹음한 목소리로 확인)
1. 에피소드 1(호텔 체크인)을 재생하고 가상 콘솔을 **D, 시속 40km**로 둡니다.
2. 두 번째 묶음(전체 대화 듣기)이 나오면 **시속 0km**로 바꿉니다.
3. 묶음이 끝나면 다다가 이렇게 말합니다.
   - "신호 대기 중이에요. 방금 표현을 한 번 말해 볼까요?"
   - 이어서 영어 질문: "Good evening. Do you have a reservation?"
4. 미디어 센터에 "듣고 있어요"가 보이면 대답합니다. 예: **"Yes, I have a reservation under the name Kim."**

**확인할 것:**
| 대답 | 다다의 반응 |
|---|---|
| 맞는 대답 | "좋아요!" → 영어 한마디 → "그럼 이어서 들을게요." |
| 일부만 맞음 | "거의 맞았어요. 이렇게 말해 보세요." → 예시 문장 한 번 → "그럼 이어서 들을게요." |
| 엉뚱한 대답 | "이렇게 말해 볼 수 있어요." → 예시 문장 한 번 → "그럼 이어서 들을게요." |
| 대답 없음 | 12초 뒤 "괜찮아요. 이어서 들을게요." |

### 3-2. 주차 중 대화 연습 ✅ (녹음한 목소리로 확인)
1. 재생 중에 가상 콘솔을 **P**로 바꿉니다.
2. 묶음이 끝나면 다다가 이렇게 시작합니다.
   - "차를 세워 두셨네요. 짧은 대화 연습을 해 볼게요…"
   - 프런트 직원 역할로 "Good evening. Welcome. How can I help you?"
3. 손님이 되어 대답합니다. 예: "Hi. I have a reservation. Name is Kim."
4. 다음 질문에도 대답합니다. 예: "Here you go. Could I get a late check-out?"

**확인할 것:**
- 대답을 두 번까지 듣고 끝냄
- 엉뚱한 대답이면 "이렇게 말해 볼 수 있어요." → 예시 문장 → "그럼 이어서 들을게요."로 바로 끝냄
- 대답이 없으면 "괜찮아요. 이어서 들을게요."로 바로 끝냄("괜찮아요"는 대답이 없을 때만)
- 주차 대화 연습은 에피소드마다 한 번만 열리고, 그 뒤 정차 때는 3-1의 짧은 연습이 나옴

### 3-3. 연습 중 출발 ✅
"듣고 있어요"가 보일 때 가상 콘솔을 **시속 40km**로 바꿉니다.

**확인할 것:** 듣기가 바로 멈추고(`listening stopped`), 곧바로 다음 묶음이 재생됨

### 3-3a. 비 오는 날(와이퍼) ✅
Virtual Console의 **Windshield Wiper Switch**(앞)를 Low 등으로 켭니다. 터미널: `curl -s -X POST "$VC/vehicle/body?front_wiper=Low"` / `front_wiper=Off`

**확인할 것:**
- 로그에 `DadaCast.Vehicle: wiper ON`과 `wipers running`이 찍힘
- 와이퍼가 도는 동안 D·속도 0으로 서도 신호 대기 연습 없이 계속 재생
- P(주차)의 대화 연습은 그대로 열림

### 3-3b. 마이크 입력이 없을 때 ⚠️ 단위 테스트만
마이크가 꺼진 상태로 두 번 연속 아무 소리도 들어오지 않으면, 다음 듣기 때 미디어 센터·미니 플레이어·우리 화면에 "마이크 입력이 없어요"가 보입니다. 소리가 들어오면 바로 사라집니다.
- 소리로 치는 기준: 입력 크기(rms) 8 초과. 마이크가 꺼져 있으면 약 4.7로 고정되고, 켜져 있으면 조용한 방에서도 10~50, 말하면 수백~1000 이상입니다.
- 흉내 내기: `adb emu avd hostmicoff`로 마이크를 끄고 연습을 두 번 넘기면, 세 번째 듣기에서 문구가 보입니다. `adb emu avd hostmicon`으로 다시 켭니다.

### 3-3c. 목소리 ✅ (영어 상대역 finley는 로그로 아직 확인 못 함)
- 다다(한국어 진행): `sanghyeok`
- 영어 예문: `lyra`
- 연습 상대(영어): `finley`
- 연습 상대(한국어): `haeun`
- 로그 `DadaCast.Service: speak [en-US/lyra] …`에서 문장마다 쓰인 목소리를 볼 수 있습니다.
- 목소리는 `speech/Voices.kt` 한 곳에서 바꿉니다.

### 3-4. 사람 목소리 대신 녹음 파일로 대답하기 (개발용) ✅
에뮬레이터에 말을 걸 수 없을 때 씁니다. 다음 듣기 한 번만 마이크 대신 파일을 씁니다.
```sh
D=$(mktemp -d)
mkdir -p "$D" && cd "$D"
say -v Samantha "Yes, I have a reservation under the name Kim." -o good.aiff
afconvert -f WAVE -d LEI16@16000 -c 1 good.aiff good.wav
python3 -c "import wave;w=wave.open('good.wav');open('good.pcm','wb').write(w.readframes(w.getnframes()))"
adb push good.pcm /data/local/tmp/good.pcm
adb shell "cat /data/local/tmp/good.pcm | run-as ai.dadacast.app --user 10 sh -c 'cat > files/good.pcm'"
adb shell am broadcast -n ai.dadacast.app/.debug.SpeechFeedReceiver --es file good.pcm   # 다음 대답으로 사용
adb shell am broadcast -n ai.dadacast.app/.debug.SpeechFeedReceiver --es file none       # 다시 마이크로
```
- 파일 형식: 16kHz, 16비트, 모노 PCM
- `run-as` 줄에서 `files` 폴더가 없다는 오류가 나면 `sh -c 'mkdir -p files; cat > files/good.pcm'`로 바꿔 실행합니다(새로 설치한 직후).

### 3-5. 실제 마이크로 말하기 ⚠️ 확인하지 못함

마이크 소리가 음성 인식까지 가는지 보려면, 말하는 동안 아래 로그를 켜 두세요.
```sh
adb logcat -s DadaCast.Stt:D
```
- `listen window start: locale=en_US`: 듣기 시작과 언어
- `rms 3.2`: 들어오는 소리 크기(1초에 최대 4번)
- `heard (partial): …` / `heard (final): …`: 알아들은 말
- `short pause: listening on (recognition 2, line 1, so far: …)`: 짧게 쉬어서 인식이 끝났지만 대답을 이어서 들음(line = 닿은 화면 줄)
- `stt error mid-answer: listening on …`: 인식 오류였지만 마지막 줄 전이라 다시 들음
- `quiet after speech: turn over`: 마지막 줄에 닿은 뒤 2초 동안 조용해서 대답을 마침
- `stt error …`: 오류
- `listen window end: rms updates=…, max=…`: 듣기가 끝날 때 요약

읽는 법
- rms 줄이 없거나 max가 0 근처: 소리가 음성 인식까지 오지 않음(에뮬레이터 마이크 설정이나 macOS 권한)
- rms는 움직이는데 heard가 없음: 소리는 오지만 알아듣지 못함(언어나 인식 문제)
개발 중 다음을 시도했지만 인식 결과가 전혀 오지 않았습니다.
- `adb emu avd hostmicon`으로 맥 마이크를 켬
- 맥 스피커로 대답을 재생함

에뮬레이터를 `-allow-host-audio` 없이 켰고, 맥의 마이크 권한도 확인하지 못했습니다. 직접 해 보려면 다음을 순서대로 하세요.
1. 에뮬레이터를 끄고 `-allow-host-audio`를 붙여 다시 켭니다.
   ```sh
   ~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2.1 -no-snapshot-load -no-boot-anim -allow-host-audio
   ```
2. 맥에서 마이크 권한을 묻는 창이 뜨면 **허용**합니다.
   - 이미 거절했다면: 시스템 설정 → 개인정보 보호 및 보안 → 마이크에서 터미널(또는 Android Studio)을 켭니다.
3. 에뮬레이터 옆 **⋯ (Extended controls) → Microphone**에서 "Virtual microphone uses host audio input"을 켭니다.
4. 3-1 순서대로 하고 마이크가 초록색일 때 말합니다.

---

## 4. 문제 해결

- **`emulator_inactive`가 나옴:** 가상 콘솔 연결이 끊긴 것입니다.
  - 브라우저에서 http://127.0.0.1:8088 을 열어 두거나, 1-4의 반복 명령을 켜 두세요.
  - `adb forward tcp:8088 tcp:8088`도 다시 실행합니다.
- **소리가 안 남:**
  - 에뮬레이터 볼륨을 확인합니다.
  - 미디어 센터에서 상태가 "재생 중"인지 봅니다.
  - "다른 음성이 나와서 잠시 멈췄어요"라면 내비 안내가 끝나기를 기다립니다.
- **미디어 키가 안 먹음:** 한 번 재생을 시작한 뒤에 눌러 보세요. 재생해야 미디어 키가 다다캐스트로 옵니다.
- **연습이 시작되지 않음:**
  - 로그에 `car is RED_LIGHT` 또는 `car is PARKED`가 있는지 확인합니다.
  - 연습은 묶음이 끝날 때만 시작합니다. 첫 묶음(인트로) 뒤에는 연습이 없습니다.
- **미니 플레이어(왼쪽 아래) 버튼이 안 먹음:** 먼저 터치가 위젯까지 가는지 봅니다. 홈 화면이 받는 터치 영역을 확인합니다.
  ```sh
  adb shell dumpsys input | grep "ai.umos.homescreen/ai.umos.homescreen.HomeActivity', id" | grep -o "touchableRegion=[^ ]*"
  ```
  - 왼쪽 패널 부분이 `[0,1260][853,1300]`처럼 맨 아래 40px뿐이면, 미니 플레이어 버튼(y≈1160~1255)을 누른 터치는 그 위에 겹친 주행 화면(`ai.umos.drivingview`)이 받습니다. 로그에는 `SingleFingerEventView clicked: MiniAppContainerView`와 `onTaskMovedToFront ...DrivingViewActivity`만 찍히고, 버튼의 PendingIntent는 한 번도 보내지지 않습니다(`adb shell dumpsys activity intents`에서 `action_playback_control`에 `sent=true`가 없음). 다다캐스트까지 명령이 오지 않으므로 **앱이나 세션을 고쳐서 해결되지 않습니다**(2026-11-13 확인. 다른 미니 앱 페이지도 같음). 미디어 센터를 다시 띄워도 바뀌지 않습니다.
  - 이때 시연은 미디어 센터의 ⏮ ⏯ ⏭, 2-3 미디어 키, 음성으로 합니다. 미니 플레이어 맨 아래 띠(y 1260~1300)를 누르면 미디어 센터가 열립니다.
  - 터치가 위젯까지 가는데도 안 먹으면(로그에 `onInterceptTouchEvent`가 찍히는데 다다캐스트 로그에 `command`가 없음) 미디어 센터를 다시 띄웁니다. 앱은 미디어 센터가 연결될 때마다 세션을 다시 알립니다(로그 `re-announcing media session`).
  ```sh
  adb shell am force-stop ai.umos.mediaapp
  adb shell am start -a android.car.intent.action.MEDIA_TEMPLATE --es android.car.intent.extra.MEDIA_COMPONENT ai.dadacast.app/ai.dadacast.app.playback.DadaCastService
  ```
  확인: 미니 플레이어 버튼을 누르면 로그에 `command play from ai.umos.mediaapp`(또는 pause)가 찍힘
- **"시작하기 전에 확인해 주세요"만 보임:** 1-3의 동의를 아직 하지 않은 상태입니다. **시작하기**를 누르세요.
- **말하기 연습이 전혀 안 나옴:** 동의하지 않기를 골랐을 수 있습니다. **설정**에서 다시 고르세요.
- **마이크:** 3-5를 보세요.
- **미디어 센터 목록이 옛날 모양:** `adb shell am force-stop ai.umos.mediaapp` 후 다시 엽니다.

---

## 5. 로그 보는 법

```sh
adb logcat -c                                  # 이전 로그 지우기
adb logcat | grep DadaCast                     # 다다캐스트 전체
```

| 보고 싶은 것 | 명령 |
|---|---|
| 재생 상태, 말한 문장 | `adb logcat \| grep -E "DadaCast.Service: (state\|speak)"` |
| 정차 판단 | `adb logcat \| grep -E "car is\|DadaCast.Ux"` |
| 동의 상태와 켜진 기능 | `adb logcat \| grep "DadaCast.Service: features"` |
| 방향지시등, 브레이크 | `adb logcat \| grep DadaCast.Vehicle` |
| 내비 회전 안내 | `adb logcat \| grep -E "DadaCast.Navi\|TURN_GUIDANCE"` |
| 안내 음성으로 인한 멈춤 | `adb logcat \| grep DadaCast.Focus` |
| 음성 인식 | `adb logcat \| grep -E "DadaCast.Stt\|heard"` |
| 연습 진행(판정 포함) | `adb logcat \| grep "practice="` |
| 개발용 흉내 명령 | `adb logcat \| grep DadaCast.Probe` |
| 우리 화면과 재생 서비스 연결 | `adb logcat \| grep DadaCast.Client` |
