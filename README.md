# 다다캐스트 (DadaCast)

출퇴근 운전 중에 듣는 학습 오디오 앱입니다. 주행 중엔 운전 상황에 맞춰 스스로 멈추고 이어지는 오디오 학습을 제공하고, 정차 시에만 AI와 대화로 연습하는 차량 전용 오디오 학습 앱

## 실행 방법
1. Pleos Connect 에뮬레이터를 켠 뒤 APK를 설치합니다.
   ```sh
   adb install -r dadacast-v1.1-debug.apk
   ```
2. 차를 **P**(주차)로 두고 앱 목록에서 **다다캐스트**를 누릅니다. 미디어 센터에서 열립니다.
3. 처음 실행하면 **시작하기** → "다다캐스트가 쓰는 정보" 화면에서 **동의하기** → 권한 창에서 **허용**을 누릅니다. (차량 속도 권한이 "위치 정보"라는 이름으로 표시됩니다.)
4. 미디어 센터 목록에서 에피소드를 골라 재생합니다.

소스에서 빌드하려면 JDK 17이 필요합니다.
```sh
cd app && ./gradlew assembleDebug   # 결과물: app/app/build/outputs/apk/debug/app-debug.apk
```

## 테스트 환경 / 필요한 설정
- Pleos Connect Emulator v2.1.0 (Android Automotive, API 34, AVD `Pleos_Connect_v2.1`), cold boot로 실행
- Virtual Console 연결: `adb forward tcp:8088 tcp:8088` 후 브라우저에서 http://127.0.0.1:8088 을 엽니다. 기어·속도·방향지시등(Turn Signal State)을 여기서 바꿉니다.
- 말하기 연습을 하려면 에뮬레이터 마이크를 켭니다.
  - `-allow-host-audio`를 붙여 에뮬레이터 실행
  - **⋯ (Extended controls) → Microphone**에서 "Virtual microphone uses host audio input" 켜기
  - macOS가 마이크 권한을 물으면 허용
- 처음 실행 때 차량 정보 사용에 동의해야 방향지시등·신호 대기 연습이 동작합니다. 동의하지 않으면 내비 안내 음성에만 멈춥니다.
- 자세한 점검 절차: [app/TESTING.md](app/TESTING.md)

## APK
[최신 Release](https://github.com/wkdaud9/dadacast/releases/latest)에서 `dadacast-v1.1-debug.apk`를 받습니다.

글꼴 Asta Sans는 SIL Open Font License 1.1을 따릅니다(`app/app/src/main/assets/licenses/AstaSans-OFL.txt`).
