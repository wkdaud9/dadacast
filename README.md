# 다다캐스트 (DadaCast)

> 현대자동차그룹 차량용 앱 개발 공모전 출품작

출퇴근길에 듣는 대화형 오디오 러닝 앱입니다. AI 진행자 '다다'가 영어 회화·면접 연습 에피소드를 들려줍니다.

- **주행 중:** 방향지시등, 회전 안내, 내비 안내 음성처럼 운전에 집중해야 하는 순간에는 스스로 잠시 멈추고, 상황이 끝나면 끊긴 문장부터 이어서 들려줍니다.
- **차가 섰을 때:** 신호 대기나 주차 중에는 방금 배운 표현을 소리 내어 말해 보는 짧은 연습을 권합니다. 대답은 선택입니다.
- **데이터 처리:** 음성 인식과 합성은 모두 차 안에서 이루어지고, 차량 정보는 차 밖으로 나가지 않습니다.

## 들어 있는 것 (v1.1)
- **에피소드 6편:** 영어 회화, 면접 리허설, 내 자료 발표 리허설(데모) 6~7분짜리 5편과, 출근길을 통째로 채우는 풀코스 1편(워밍업 약 3분 → 본편 약 25분 → 복습 약 4분).
- **이전 버튼:** 지금 문장을 처음부터 다시 들려줍니다.
- **따라 말하기 간격:** 문장이 길수록 따라 말할 시간을 더 줍니다.
- **신호 대기 연습:** 시속 0km로 4초 이상 서 있으면, 지금 묶음이 끝난 뒤 시작합니다.
- **연습 화면:** 말해 볼 문장은 차가 서 있을 때만 보입니다. 지금 말할 줄과 다음 줄이 함께 보이고, 말하는 것을 따라 한 줄씩 올라갑니다.
- **너그러운 인식:** 음성 인식이 단어를 조금 틀리게 알아들어도 같은 단어로 쳐서, 화면 넘김과 판정에 함께 적용합니다.
- **대답 듣기:** 기본 12초를 듣고, 긴 문장은 줄마다 3초씩 더 줍니다(최대 24초). 마지막 줄에 닿기 전에는 잠깐 쉬어도 끝나지 않고 이어서 듣습니다.

설치, 처음 실행, 기능 체험 방법은 [app/README.md](app/README.md)에, 자세한 점검 절차는 [app/TESTING.md](app/TESTING.md)에 있습니다.

## 바로 설치하기 (APK)
1. [최신 릴리스](https://github.com/wkdaud9/dadacast/releases/latest)에서 `dadacast-v1.1-debug.apk`를 받습니다.
2. Pleos Connect Emulator v2.1.0(AVD `Pleos_Connect_v2.1`)을 켠 뒤 설치합니다.
   ```sh
   adb install -r dadacast-v1.1-debug.apk
   ```
3. 이후 과정은 [app/README.md](app/README.md)의 "처음 실행"을 따릅니다.

## 소스에서 빌드하기
- JDK 17이 필요합니다.
- Pleos SDK maven 저장소는 `app/settings.gradle.kts`에 이미 등록되어 있습니다.

```sh
cd app
./gradlew assembleDebug
# 결과물: app/app/build/outputs/apk/debug/app-debug.apk
```

## 저장소 구성
```
app/        Android 프로젝트 (Gradle)
  app/src/main/   앱 소스, 리소스, 에피소드 데이터(assets/episodes.json)
  app/src/debug/  시연용 테스트 보조 도구 (debug 빌드 전용)
  app/src/test/   단위 테스트
  README.md       설치·사용 안내
  TESTING.md      점검 절차
  DESIGN_GUIDE_NOTES.md  Pleos Connect 디자인 가이드 요약과 화면 점검
content/    에피소드 원고 (episodes.json, 읽기용 episodes.md)
```

글꼴 Asta Sans는 SIL Open Font License 1.1을 따르며, 라이선스 전문은 `app/app/src/main/assets/licenses/AstaSans-OFL.txt`에 있습니다.
