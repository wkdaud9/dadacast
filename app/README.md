# 다다캐스트 (DadaCast)

출퇴근길에 듣는 대화형 오디오 러닝 앱입니다. AI 진행자 '다다'가 영어 회화·면접 연습 에피소드를 들려줍니다.

- **주행 중:** 방향지시등, 회전 안내, 내비 안내 음성처럼 운전에 집중해야 하는 순간에는 스스로 잠시 멈춥니다. 상황이 끝나면 끊긴 문장부터 이어서 들려줍니다.
- **차가 섰을 때:** 신호 대기나 주차 중에는 방금 배운 표현을 소리 내어 말해 보는 짧은 연습을 권합니다. 대답은 선택입니다.
- **데이터 처리:** 음성 인식과 합성은 모두 차 안에서 이루어지고, 차량 정보는 차 밖으로 나가지 않습니다.

## 필요한 것
- Pleos Connect Emulator v2.1.0 (AVD `Pleos_Connect_v2.1`, cold boot)
- Android SDK platform-tools(`adb`)
- 차량 상태를 바꿀 Pleos Connect Virtual Console

## 설치
```sh
adb install -r app-debug.apk
adb forward tcp:8088 tcp:8088      # Virtual Console 연결
```
브라우저에서 http://127.0.0.1:8088 을 열면 Virtual Console을 쓸 수 있습니다.

## 에뮬레이터 마이크 설정 (말하기 연습용)
1. 호스트 오디오를 켠 채로 에뮬레이터를 실행합니다.
   ```sh
   ~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2.1 -no-snapshot-load -allow-host-audio
   ```
2. 에뮬레이터 옆 **⋯ (Extended controls) → Microphone**에서 **Virtual microphone uses host audio input**을 켭니다.
3. macOS가 마이크 권한을 물으면 허용합니다. 시스템 설정 → 개인정보 보호 및 보안 → 마이크에서 에뮬레이터를 실행한 터미널(또는 Android Studio)을 켜 두어도 됩니다.

## 처음 실행
1. 차를 **P**(주차)로 둡니다.
2. 앱 목록에서 **다다캐스트**를 누릅니다.
3. 미디어 센터에 **시작하기** 버튼이 보이면 누릅니다.
4. "다다캐스트가 쓰는 정보"를 읽고 **동의하기**를 누릅니다.
5. 차량 속도 권한 창이 뜨면 **허용**을 누릅니다. 시스템은 이 권한을 "위치 정보"라는 이름으로 표시합니다.

동의하지 않아도 에피소드는 들을 수 있습니다. 이때는 내비 안내 음성이 나올 때만 멈춥니다. 선택은 다다캐스트 화면의 **설정**에서 바꿀 수 있습니다.

## 기능 체험 (Virtual Console)
미디어 센터에서 에피소드를 고르고 재생한 상태에서 진행합니다.

| 해 볼 것 | Virtual Console 조작 | 결과 |
|---|---|---|
| 방향지시등 | 방향지시등 켜기 → 끄기 | 지금 문장까지만 듣고 멈춤 ("방향지시등이 켜져 잠시 멈췄어요"), 끄면 다음 문장부터 이어짐 |
| 내비 안내 음성 | 내비게이션 → 검색 → 최근 목적지 → 안내 시작 | 안내 음성이 나오는 동안 멈춤, 끝나면 끊긴 문장부터 다시 |
| 신호 대기 연습 | 기어 **D**, 속도 **40** → 속도 **0** | 지금 묶음이 끝나면 다다가 "신호 대기 중이에요…"라고 권하고, 영어로 짧게 묻습니다. 대답하면 결과를 알려 주고, 대답하지 않으면 그대로 이어서 재생 |
| 출발하면 연습 중단 | 연습 중 속도 **40** | 듣기와 연습을 바로 멈추고 다음 묶음 재생 |
| 주차 중 대화 연습 | 기어 **P** | 묶음이 끝나면 프런트 직원 역할로 짧은 대화 연습 (에피소드마다 한 번) |
| 비 오는 날 | 와이퍼(Windshield Wiper Switch) 켜기 | 와이퍼가 도는 동안은 신호 대기 연습을 쉬고 계속 재생 |

연습 중에는 미디어 센터와 왼쪽 미니 플레이어에 "말하기 연습 중", "듣고 있어요", "확인 중"이 표시됩니다.

## 문제 해결
- **Virtual Console이 `emulator_inactive`를 돌려줌:** 브라우저에서 Virtual Console 페이지를 열어 두세요. `adb forward tcp:8088 tcp:8088`도 다시 실행합니다.
- **미니 플레이어 버튼이 반응하지 않음:** 미디어 센터를 다시 띄웁니다.
  ```sh
  adb shell am force-stop ai.umos.mediaapp
  adb shell am start -a android.car.intent.action.MEDIA_TEMPLATE --es android.car.intent.extra.MEDIA_COMPONENT ai.dadacast.app/ai.dadacast.app.playback.DadaCastService
  ```
- **말하기 연습에서 "마이크 입력이 없어요"가 보임:** 위의 마이크 설정 세 단계를 확인합니다.
- **연습이 나오지 않음:** 처음 실행에서 동의했는지 확인합니다. 첫 묶음(인트로) 뒤와 와이퍼가 도는 동안에는 신호 대기 연습이 없습니다.

## 테스트 보조 도구 (debug 빌드 전용)
실제 주행이나 목소리 없이 시연하기 위한 도구입니다. 실제 기능이 아닌 **시뮬레이션**입니다.
```sh
# 150m 앞 우회전 안내를 흉내 냄 → 지금 묶음이 끝나면 "회전 구간이 가까워 잠시 멈췄어요"
adb shell am broadcast -n ai.dadacast.app/.debug.TurnGuidanceReceiver --es type TURN_RIGHT --ei distance 150
adb shell am broadcast -n ai.dadacast.app/.debug.TurnGuidanceReceiver --es type none

# 다른 앱이 음성을 낸 것처럼 오디오 포커스를 잠시 가져감
adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action take
adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action release

# 다음 연습 대답을 녹음 파일(16kHz, 16-bit, mono PCM)로 대신함 (자세한 방법은 TESTING.md)
adb shell am broadcast -n ai.dadacast.app/.debug.SpeechFeedReceiver --es file good.pcm
```

자세한 점검 절차는 [TESTING.md](TESTING.md)에 있습니다.
