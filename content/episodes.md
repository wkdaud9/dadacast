# 다다캐스트 데모 에피소드 스크립트

> 이 문서는 `episodes.json`(schema 0.2)과 동일한 내용을 사람이 읽기 쉽게 정리한 것입니다. 모든 회사·인물·수치는 가상입니다.

## 공통 규칙

- **speakers**
  - `narrator_ko`: 한국어 내레이터 TTS
  - `en_voice`: 영어 원어민 TTS (대화 예문·따라 말하기 문장)
  - `ai_partner`: 정차 시 대화 파트너 (LLM + TTS). drive 청크의 영어 대화에서는 상대역(학습자 쪽이 아닌 인물)을 맡아 en_voice와 다른 목소리로 읽힘
- **modes**
  - `drive`: 주행 모드. 에피소드의 선형 재생 순서(chunks)는 모두 drive 청크이며 마지막은 정리(recap) 청크. 답변을 요구하지 않음. 주행 중에는 음성 입력(마이크)을 쓰지 않으며, 재생 제어는 스티어링 휠·미디어 버튼으로 함. 음성 입력은 차가 멈춰 있을 때(정차 연습)만 사용.
  - `stop`: 정차 상태에서만 열리는 말하기 연습. 아래 red_light_practice(신호대기 마이크로 연습)와 deferred_roleplays(주차·장시간 정차 롤플레이) 두 종류.
- **red_light_practice**: 신호대기 마이크로 연습. 각 drive 청크(표현이 없는 인트로 제외)의 선택 필드. 주행 중 차량이 정차하면 현재 청크가 끝나는 경계에 삽입되어, 방금 들은 target_expression을 쓰도록 AI 파트너가 짧은 프롬프트 하나를 던지고(한 문장으로 답할 수 있는 질문), 사용자 답변 뒤 ai_followup(격려 또는 부드러운 교정) 한 마디로 끝남. max_turns 2, max_seconds 30. 차량 출발 신호가 오면 즉시 중단(재생 중인 문장까지만 마침)하고, 다음 청크 처음부터 drive 재생을 이어감. 영어 에피소드는 영어로, 한국어 에피소드(면접, 내 자료)는 한국어로 연습.
- **deferred_roleplays**: 에피소드별 긴 롤플레이 목록. 선형 재생 위치가 없으며 trigger: parked_or_long_stop(기어 P 또는 60초 이상 정차)일 때만 제안·실행. 짧은 신호대기에는 열리지 않음. 출발 신호 시 즉시 종료하고 drive 재생으로 복귀. est_seconds_nominal은 60초 체크포인트 단위의 명목값, 최대 길이는 roleplay.max_session_minutes.
- **pause_after_sec**: 해당 줄 재생 뒤 무음 길이(초). 따라 말하기 줄(shadow: true)은 2~4초.
- **shadow**: 선택 필드. true이면 따라 말하기용 문장.
- **est_seconds**: drive 청크의 TTS 기준 추정 재생 시간(무음 포함).
- **est_total_seconds**: 선형 drive 재생 시간 합계. 신호대기 연습과 롤플레이 시간은 제외.
- **est_optional_seconds**: 선택적 정차 연습의 최대 시간 합계 (red_light_practice max_seconds 합 + deferred_roleplays 최대 세션 시간 합). 세부는 est_optional_breakdown.
- **sections**: 선택 필드(에피소드). 긴 통근 편의 구간(워밍업·본편·복습) 표시. 각 항목은 section, title, chunk_ids, est_seconds. 앱 파서는 읽지 않으며, 같은 구간이 청크 제목 머리말(예: '워밍업 · ')로도 드러남.
- **interruption**: 갑작스러운 운전 부하 신호(방향지시등)는 현재 문장이 끝나는 지점에서, 미리 알 수 있는 신호(회전 안내, 주의 지점)는 현재 청크 경계에서 멈춤. 내비 음성이 나오면 Audio Focus 정책에 따라 즉시 멈추고 양보. 재개 시 중단된 문장 처음부터 다시 재생(청크 처음으로 되돌아가지 않음). (신호대기 연습 후 복귀는 다음 청크부터)
- **tts**: 지문·괄호·기호 없이 읽을 텍스트만 기재. 숫자는 읽는 형태로 표기.
- 표기: 각 줄 끝 `⏸ n초`는 줄 뒤 무음, `[따라 말하기]`는 shadow 문장, `[신호대기 연습]`은 red_light_practice입니다.

## 에피소드 목록

| ID | 제목 | 레벨 | 주행 청크 | 신호대기 연습 | 주차 롤플레이 | 선형 재생(추정) | 선택 연습(최대) |
|---|---|---|---|---|---|---|---|
| `ep01_hotel_checkin` | 영어 회화 A2: 호텔 체크인 | A2 | 9 | 8 | 1 | 6분 32초 | 9분 0초 |
| `ep02_meeting_opinions` | 영어 회화 B1: 회의에서 의견 말하기 | B1 | 9 | 8 | 1 | 6분 3초 | 9분 0초 |
| `ep03_airport_delay` | 영어 회화 B1: 공항에서 비행 지연 대응 | B1 | 9 | 8 | 1 | 6분 14초 | 9분 0초 |
| `ep04_interview_rehearsal_ko` | 면접 리허설: 직무 면접 자주 나오는 질문 3개 | 취업·이직 준비 (한국어) | 9 | 8 | 1 | 6분 4초 | 9분 0초 |
| `ep05_my_material_demo` | 내 자료로 에피소드 (데모): 3분기 실적 발표 리허설 | 발표 리허설 (한국어) | 9 | 8 | 1 | 6분 7초 | 9분 0초 |
| `ep06_commute_running_late` | 영어 회화 B1: 출근길 전화로 회의 미루기 | B1 | 43 | 42 | 1 | 32분 33초 | 26분 0초 |

---

## 영어 회화 A2: 호텔 체크인

- **ID**: `ep01_hotel_checkin`  
- **레벨**: A2  
- **목표 길이**: 약 7분 (선형 재생 추정 6분 32초, 주행 청크 9개)  
- **선택 정차 연습**: 최대 9분 0초 (선형 재생 시간에 미포함)

### 학습 목표 (핵심 표현 3)

1. **I have a reservation under the name Kim.** — 김이라는 이름으로 예약했어요. (예약 확인하기)
2. **Could I get a late check-out?** — 체크아웃을 늦게 할 수 있을까요? (정중하게 요청하기)
3. **What time is breakfast served?** — 조식은 몇 시에 나오나요? (시간 묻기)

### 주행 청크 (선형 재생 순서)

#### `ep01_c01` 인트로 — 주행(drive), 약 36초

- **내레이터(KO)**: 안녕하세요, 다다캐스트의 다다예요. `⏸ 1초`
- **내레이터(KO)**: 오늘은 호텔 체크인 영어를 함께 연습합니다. `⏸ 1초`
- **내레이터(KO)**: 운전하시면서 편하게 들으시면 됩니다. `⏸ 1초`
- **내레이터(KO)**: 문장이 나온 뒤 잠깐 조용해지면, 원하실 때만 소리 내어 따라 하세요. `⏸ 1초`
- **내레이터(KO)**: 대답하실 필요는 없습니다. `⏸ 1초`
- **내레이터(KO)**: 오늘의 핵심 표현은 세 가지입니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 예약 확인하기. 둘째, 늦은 체크아웃 부탁하기. 셋째, 조식 시간 묻기. `⏸ 2초`
- **내레이터(KO)**: 그럼 시작하겠습니다. `⏸ 1초`

#### `ep01_c02` 전체 대화 듣기 — 주행(drive), 약 47초

- **내레이터(KO)**: 먼저 프런트에서 오가는 대화를 한 번 들어 보겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Good evening. Welcome to the hotel. `⏸ 1초`
- **영어 음성(EN)**: Hi. I have a reservation under the name Kim. `⏸ 1초`
- **영어 음성(EN)**: Let me check. Could I see your passport, please? `⏸ 1초`
- **영어 음성(EN)**: Sure. Here you go. `⏸ 1초`
- **영어 음성(EN)**: Thank you. You're in room five twelve, for two nights. `⏸ 1초`
- **영어 음성(EN)**: Great. Could I get a late check-out on Sunday? `⏸ 1초`
- **영어 음성(EN)**: Of course. Is one o'clock okay? `⏸ 1초`
- **영어 음성(EN)**: Perfect. And what time is breakfast served? `⏸ 1초`
- **영어 음성(EN)**: From seven to ten, on the second floor. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I have a reservation under the name Kim.*  
> AI 파트너: Good evening. Do you have a reservation?  
> 사용자(예시): Yes, I have a reservation under the name Kim.  
> AI 파트너: Perfect. Welcome to the hotel!

#### `ep01_c03` 표현 1: 예약 확인하기 — 주행(drive), 약 45초

- **내레이터(KO)**: 첫 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 아이 해버 레저베이션은, 예약을 했다는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: I have a reservation. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 언더 더 네임은, 어떤 이름으로 예약했는지 알려 줄 때 씁니다. `⏸ 1초`
- **영어 음성(EN)**: under the name Kim. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 이제 이어서 말해 보겠습니다. `⏸ 1초`
- **영어 음성(EN)**: I have a reservation under the name Kim. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 이름 대신 성만 말해도 충분합니다. `⏸ 1초`
- **영어 음성(EN)**: I have a reservation under the name Park. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I have a reservation under the name ~.*  
> AI 파트너: Hi there. What name is the reservation under?  
> 사용자(예시): I have a reservation under the name Park.  
> AI 파트너: Thank you. Let me check that for you.

#### `ep01_c04` 표현 1 응용: 체크인 기본 문장 — 주행(drive), 약 41초

- **내레이터(KO)**: 체크인할 때 함께 쓰는 문장도 익혀 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 이틀 밤 묵는다고 말할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I booked a room for two nights. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 직원이 신분증을 보여 달라고 할 때는 이렇게 말합니다. `⏸ 1초`
- **영어 음성(EN)**: Could I see your ID, please? `⏸ 1초`
- **내레이터(KO)**: 여기 있습니다, 라고 답할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Sure. Here you go. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 여권을 건넬 때는 이렇게도 말합니다. `⏸ 1초`
- **영어 음성(EN)**: Here's my passport. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I booked a room for two nights.*  
> AI 파트너: How many nights are you staying with us?  
> 사용자(예시): I booked a room for two nights.  
> AI 파트너: Two nights. Got it, thank you.

#### `ep01_c05` 표현 2: 늦은 체크아웃 부탁하기 — 주행(drive), 약 47초

- **내레이터(KO)**: 두 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 쿠다이 겟은, 무언가를 받을 수 있는지 정중하게 부탁하는 말입니다. `⏸ 1초`
- **영어 음성(EN)**: Could I get a late check-out? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 원하는 시간을 덧붙여 보겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Could I get a late check-out, around one o'clock? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 직원은 보통 이렇게 답합니다. `⏸ 1초`
- **영어 음성(EN)**: Let me see. One o'clock should be fine. `⏸ 1초`
- **내레이터(KO)**: 추가 요금이 있는지 확인할 때는 이렇게 말합니다. `⏸ 1초`
- **영어 음성(EN)**: Is there an extra charge for that? [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could I get a late check-out?*  
> AI 파트너: Check-out is at eleven. Do you need anything else?  
> 사용자(예시): Could I get a late check-out, around one o'clock?  
> AI 파트너: Sure, one o'clock is fine.

#### `ep01_c06` 표현 2 응용: 다른 부탁들 — 주행(drive), 약 41초

- **내레이터(KO)**: 같은 틀로 다른 부탁도 해 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 높은 층 방을 부탁할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could I get a room on a higher floor? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 수건을 하나 더 받고 싶을 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could I get an extra towel? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 아침 일곱 시에 모닝콜을 부탁할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could I get a wake-up call at seven? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 쿠다이 겟 하나로 호텔에서 대부분의 부탁을 할 수 있습니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could I get ~?*  
> AI 파트너: Is there anything you need for your room?  
> 사용자(예시): Could I get an extra towel?  
> AI 파트너: Of course. I'll send one up right away.

#### `ep01_c07` 표현 3: 조식 시간 묻기 — 주행(drive), 약 41초

- **내레이터(KO)**: 세 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 조식이 몇 시에 나오는지 물을 때 씁니다. `⏸ 1초`
- **영어 음성(EN)**: What time is breakfast served? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 어디에서 먹는지 물을 때는 왓 타임 대신 웨어를 씁니다. `⏸ 1초`
- **영어 음성(EN)**: Where is breakfast served? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 직원의 대답은 이렇게 들립니다. `⏸ 1초`
- **영어 음성(EN)**: Breakfast is served from seven to ten, on the second floor. `⏸ 1초`
- **내레이터(KO)**: 조식이 포함되어 있는지 확인할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Is breakfast included? [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *What time is breakfast served?*  
> AI 파트너: Breakfast is included with your room. Any questions?  
> 사용자(예시): What time is breakfast served?  
> AI 파트너: It's served from seven to ten. Enjoy!

#### `ep01_c08` 전체 대화 다시 듣기 — 주행(drive), 약 55초

- **내레이터(KO)**: 처음 대화를 다시 들어 보겠습니다. 이번에는 손님 문장 뒤에 따라 할 틈을 두겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Good evening. Welcome to the hotel. `⏸ 1초`
- **영어 음성(EN)**: Hi. I have a reservation under the name Kim. [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: Let me check. Could I see your passport, please? `⏸ 1초`
- **영어 음성(EN)**: Sure. Here you go. [따라 말하기] `⏸ 2초`
- **영어 음성(EN)**: Thank you. You're in room five twelve, for two nights. `⏸ 1초`
- **영어 음성(EN)**: Great. Could I get a late check-out on Sunday? [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: Of course. Is one o'clock okay? `⏸ 1초`
- **영어 음성(EN)**: Perfect. And what time is breakfast served? [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: From seven to ten, on the second floor. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could I get a late check-out?*  
> AI 파트너: You're all set. Anything else I can do for you?  
> 사용자(예시): Could I get a late check-out on Sunday?  
> AI 파트너: No problem. One o'clock on Sunday is fine.

#### `ep01_c09` 정리: 오늘의 세 표현 — 주행(drive), 약 39초

- **내레이터(KO)**: 오늘 배운 세 표현을 정리하겠습니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 예약 확인하기. `⏸ 1초`
- **영어 음성(EN)**: I have a reservation under the name Kim. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 늦은 체크아웃 부탁하기. `⏸ 1초`
- **영어 음성(EN)**: Could I get a late check-out? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 셋째, 조식 시간 묻기. `⏸ 1초`
- **영어 음성(EN)**: What time is breakfast served? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 수고하셨습니다. 다음 에피소드에서 또 만나요. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *What time is breakfast served?*  
> AI 파트너: One last thing. Ask me about breakfast in one sentence.  
> 사용자(예시): What time is breakfast served?  
> AI 파트너: Great question. From seven to ten, on the second floor.

### 주차·장시간 정차 롤플레이 (deferred_roleplays)

선형 재생 위치 없음. 기어 P 또는 60초 이상 정차 시에만 제안됩니다.

#### `ep01_rp01` 정차 롤플레이: 프런트 체크인 — trigger: `parked_or_long_stop`, 최대 5분

- **내레이터(KO)**: 차를 세워 두셨을 때 열리는 대화 연습입니다. `⏸ 1초`
- **내레이터(KO)**: 이번에는 제가 프런트 직원이 되겠습니다. 손님이 되어 체크인해 보세요. `⏸ 1초`
- **내레이터(KO)**: 막히면 힌트, 라고 말씀하시면 됩니다. `⏸ 1초`
- **AI 파트너**: Good evening. Welcome. How can I help you? `⏸ 0초`

**AI 파트너 지시문 (system)**

```text
You are a friendly front-desk clerk at a fictional mid-size city hotel. Speak CEFR A2 English: short sentences, common words, slow pace. Guide the guest through check-in: confirm the reservation name, ask for ID, give the room number and number of nights, then respond to any request (late check-out, breakfast time, towels). Encourage the learner to use: 'I have a reservation under the name ~', 'Could I get ~?', 'What time is breakfast served?'. If the learner makes an error, do not correct explicitly; recast the correct form naturally in your reply. If the learner says 'hint' or is silent for 8 seconds, give a one-sentence hint in Korean with the English phrase to try. Keep each turn under 20 words. Never mention real hotel brands. End politely after check-in is complete or after 5 minutes. If the app signals that the vehicle is moving, stop immediately without asking anything.
```

**샘플 대화**

- AI 파트너: Good evening. Welcome. How can I help you?
- 사용자(예시): Hi. I have a reservation. Name is Kim.
- AI 파트너: Great, you have a reservation under the name Kim. Could I see your ID, please?
- 사용자(예시): Here you go. Could I get a late check-out?
- AI 파트너: Sure. Is noon okay? Or would you like one o'clock?

---

## 영어 회화 B1: 회의에서 의견 말하기

- **ID**: `ep02_meeting_opinions`  
- **레벨**: B1  
- **목표 길이**: 약 6분 (선형 재생 추정 6분 3초, 주행 청크 9개)  
- **선택 정차 연습**: 최대 9분 0초 (선형 재생 시간에 미포함)

### 학습 목표 (핵심 표현 3)

1. **The way I see it, we should ~.** — 제가 보기에는 ~해야 할 것 같아요. (의견 말하기)
2. **That's a fair point.** — 일리 있는 말씀이에요. (동의·인정하기)
3. **I see where you're coming from, but ~.** — 어떤 뜻인지는 알겠지만, ~. (정중하게 반대하기)

### 주행 청크 (선형 재생 순서)

#### `ep02_c01` 인트로 — 주행(drive), 약 32초

- **내레이터(KO)**: 안녕하세요, 다다캐스트의 다다예요. `⏸ 1초`
- **내레이터(KO)**: 오늘은 영어 회의에서 의견을 말하는 법을 연습합니다. `⏸ 1초`
- **내레이터(KO)**: 특히 상대 의견에 동의하거나, 부드럽게 반대하는 표현에 집중합니다. `⏸ 1초`
- **내레이터(KO)**: 운전 중에는 듣기만 하셔도 충분합니다. 대답하실 필요는 없습니다. 원하실 때만 소리 내어 따라 하세요. `⏸ 1초`
- **내레이터(KO)**: 오늘의 세 표현은, 내 의견 말하기, 상대 의견 인정하기, 정중하게 반대하기입니다. `⏸ 2초`

#### `ep02_c02` 전체 대화 듣기 — 주행(drive), 약 48초

- **내레이터(KO)**: 신제품 출시 일정을 논의하는 팀 회의입니다. 한 번 들어 보겠습니다. `⏸ 1초`
- **영어 음성(EN)**: We're behind schedule. I think we should cut the testing phase to one week. `⏸ 1초`
- **영어 음성(EN)**: I see where you're coming from, but one week might not be enough. `⏸ 1초`
- **영어 음성(EN)**: Why not? The last test went smoothly. `⏸ 1초`
- **영어 음성(EN)**: That's a fair point. Still, this version has more new features. `⏸ 1초`
- **영어 음성(EN)**: So what do you suggest? `⏸ 1초`
- **영어 음성(EN)**: The way I see it, we should delay the launch by one week and keep the full test. `⏸ 1초`
- **영어 음성(EN)**: Hmm. Okay, let's take that to the manager. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I see where you're coming from, but ~.*  
> AI 파트너: Let's cut the testing phase to one week.  
> 사용자(예시): I see where you're coming from, but one week might not be enough.  
> AI 파트너: Fair enough. That's a polite way to push back.

#### `ep02_c03` 표현 1: 의견 말하기 — 주행(drive), 약 44초

- **내레이터(KO)**: 첫 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 더 웨이 아이 씨 잇은, 제가 보기에는, 이라는 뜻입니다. 아이 띵크보다 조금 더 생각이 정리된 느낌을 줍니다. `⏸ 1초`
- **영어 음성(EN)**: The way I see it, [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: The way I see it, we should delay the launch. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 뒤에 오는 내용만 바꿔 보겠습니다. `⏸ 1초`
- **영어 음성(EN)**: The way I see it, we need more data first. [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: The way I see it, this is a budget issue. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *The way I see it, we should ~.*  
> AI 파트너: What do you think we should do about the deadline?  
> 사용자(예시): The way I see it, we should delay the launch.  
> AI 파트너: Clear and confident. Nice.

#### `ep02_c04` 표현 1 응용: 의견을 부드럽게 — 주행(drive), 약 40초

- **내레이터(KO)**: 의견을 조금 더 부드럽게 내고 싶을 때 쓰는 말도 있습니다. `⏸ 1초`
- **내레이터(KO)**: 제 생각엔 ~하는 게 좋을 것 같아요, 라는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: I'd suggest we keep the full test. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 확실하지 않을 때는 이렇게 시작합니다. `⏸ 1초`
- **영어 음성(EN)**: I might be wrong, but I think the deadline is too tight. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 짧게 덧붙이는 이유도 함께 연습해 보겠습니다. `⏸ 1초`
- **영어 음성(EN)**: The main reason is quality. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'd suggest we ~.*  
> AI 파트너: We're stuck. Any suggestions?  
> 사용자(예시): I'd suggest we keep the full test.  
> AI 파트너: Good suggestion. Let's note that.

#### `ep02_c05` 표현 2: 동의·인정하기 — 주행(drive), 약 35초

- **내레이터(KO)**: 두 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 댓츠 어 페어 포인트는, 일리 있는 말씀이에요, 라는 뜻입니다. `⏸ 1초`
- **내레이터(KO)**: 완전히 동의하지 않아도 상대의 논리를 인정할 때 쓸 수 있어 회의에서 아주 유용합니다. `⏸ 1초`
- **영어 음성(EN)**: That's a fair point. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 완전히 동의할 때는 이렇게 말합니다. `⏸ 1초`
- **영어 음성(EN)**: I completely agree. [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: I'm with you on that. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *That's a fair point.*  
> AI 파트너: Users complained a lot about the last update, you know.  
> 사용자(예시): That's a fair point.  
> AI 파트너: Thanks. So let's be careful this time.

#### `ep02_c06` 표현 3: 정중하게 반대하기 — 주행(drive), 약 40초

- **내레이터(KO)**: 세 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 아이 씨 웨어 유어 커밍 프롬은, 그렇게 생각하시는 이유는 알겠어요, 라는 뜻입니다. `⏸ 1초`
- **내레이터(KO)**: 상대 입장을 먼저 인정하고, 벗 뒤에 내 의견을 붙입니다. `⏸ 1초`
- **영어 음성(EN)**: I see where you're coming from, [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: I see where you're coming from, but one week might not be enough. [따라 말하기] `⏸ 4초`
- **영어 음성(EN)**: I see where you're coming from, but the budget is limited. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I see where you're coming from, but ~.*  
> AI 파트너: I think we should hire two more people right now.  
> 사용자(예시): I see where you're coming from, but the budget is limited.  
> AI 파트너: Nicely put. Polite and clear.

#### `ep02_c07` 표현 3 응용: 반대를 더 부드럽게 — 주행(drive), 약 32초

- **내레이터(KO)**: 반대 의견을 더 부드럽게 꺼내는 표현들입니다. `⏸ 1초`
- **내레이터(KO)**: 확신이 서지 않는다고 말할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm not sure that's the best approach. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 다른 방법을 제안할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could we look at it another way? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 영어 회의에서는 노, 라고 바로 말하기보다 이렇게 쿠션을 두는 경우가 많습니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm not sure that's the best approach.*  
> AI 파트너: Let's just copy what we did last year.  
> 사용자(예시): I'm not sure that's the best approach.  
> AI 파트너: Fair. We can look for another way later.

#### `ep02_c08` 전체 대화 다시 듣기 — 주행(drive), 약 50초

- **내레이터(KO)**: 처음 대화를 다시 들어 보겠습니다. 핵심 문장 뒤에는 따라 할 틈을 두겠습니다. `⏸ 1초`
- **영어 음성(EN)**: We're behind schedule. I think we should cut the testing phase to one week. `⏸ 1초`
- **영어 음성(EN)**: I see where you're coming from, but one week might not be enough. [따라 말하기] `⏸ 4초`
- **영어 음성(EN)**: Why not? The last test went smoothly. `⏸ 1초`
- **영어 음성(EN)**: That's a fair point. Still, this version has more new features. [따라 말하기] `⏸ 4초`
- **영어 음성(EN)**: So what do you suggest? `⏸ 1초`
- **영어 음성(EN)**: The way I see it, we should delay the launch by one week. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *The way I see it, we should ~.*  
> AI 파트너: So what do you suggest?  
> 사용자(예시): The way I see it, we should delay the launch by one week.  
> AI 파트너: That sounds reasonable to me.

#### `ep02_c09` 정리: 오늘의 세 표현 — 주행(drive), 약 42초

- **내레이터(KO)**: 오늘 배운 세 표현을 정리하겠습니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 내 의견 말하기. `⏸ 1초`
- **영어 음성(EN)**: The way I see it, we should delay the launch. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 상대 의견 인정하기. `⏸ 1초`
- **영어 음성(EN)**: That's a fair point. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 셋째, 정중하게 반대하기. `⏸ 1초`
- **영어 음성(EN)**: I see where you're coming from, but one week might not be enough. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 수고하셨습니다. 다음 회의에서 꼭 한 번 써 보세요. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *That's a fair point.*  
> AI 파트너: I think quality matters more than speed.  
> 사용자(예시): That's a fair point. I'm with you on that.  
> AI 파트너: Great, we agree on that.

### 주차·장시간 정차 롤플레이 (deferred_roleplays)

선형 재생 위치 없음. 기어 P 또는 60초 이상 정차 시에만 제안됩니다.

#### `ep02_rp01` 정차 롤플레이: 일정 회의 — trigger: `parked_or_long_stop`, 최대 5분

- **내레이터(KO)**: 차를 세워 두셨을 때 열리는 대화 연습입니다. `⏸ 1초`
- **내레이터(KO)**: 저는 일정을 서두르고 싶은 동료 역할입니다. 제 의견을 인정하면서도, 정중하게 반대해 보세요. `⏸ 1초`
- **내레이터(KO)**: 막히면 힌트, 라고 말씀하시면 됩니다. `⏸ 1초`
- **AI 파트너**: Honestly, I think we should skip the user survey. We just don't have time. `⏸ 0초`

**AI 파트너 지시문 (system)**

```text
You are a coworker in a team meeting at a fictional company. Speak natural CEFR B1 English. Your position: the team should skip a step (the user survey, or cut testing) to meet a deadline. Give one clear reason per turn and push back once or twice so the learner must disagree politely, then be open to compromise. Encourage use of 'The way I see it, ~', 'That's a fair point', 'I see where you're coming from, but ~'. When the learner disagrees too directly (e.g., 'No, that's wrong'), respond naturally, then add a short Korean tip suggesting a softer phrase. If the learner says 'hint' or is silent for 8 seconds, offer one Korean hint with an English sentence starter. Keep turns under 30 words. Wrap up after reaching a compromise or after 5 minutes, and give a 2-sentence Korean feedback summary. No real company or product names. Stop immediately if the app signals the vehicle is moving.
```

**샘플 대화**

- AI 파트너: Honestly, I think we should skip the user survey. We just don't have time.
- 사용자(예시): I see where you're coming from, but the survey helps us find problems early.
- AI 파트너: That's true, but the last survey didn't change much. Why do you think this time is different?
- 사용자(예시): That's a fair point. The way I see it, we could do a shorter survey with ten users.
- AI 파트너: A shorter survey... okay, I could live with that. Let's propose it together.

---

## 영어 회화 B1: 공항에서 비행 지연 대응

- **ID**: `ep03_airport_delay`  
- **레벨**: B1  
- **목표 길이**: 약 6분 (선형 재생 추정 6분 14초, 주행 청크 9개)  
- **선택 정차 연습**: 최대 9분 0초 (선형 재생 시간에 미포함)

### 학습 목표 (핵심 표현 3)

1. **Could you tell me the new departure time?** — 변경된 출발 시간을 알려 주시겠어요? (정보 요청하기)
2. **Is it possible to rebook me on the next flight?** — 다음 비행기로 다시 예약해 주실 수 있나요? (대안 요청하기)
3. **Is there any compensation for the delay?** — 지연에 대한 보상이 있나요? (보상 문의하기)

### 주행 청크 (선형 재생 순서)

#### `ep03_c01` 인트로 — 주행(drive), 약 33초

- **내레이터(KO)**: 안녕하세요, 다다캐스트의 다다예요. `⏸ 1초`
- **내레이터(KO)**: 오늘은 공항에서 비행기가 늦어질 때 쓰는 영어를 연습합니다. `⏸ 1초`
- **내레이터(KO)**: 안내 방송을 알아듣는 것부터, 직원에게 대안을 요청하는 것까지 다룹니다. `⏸ 1초`
- **내레이터(KO)**: 운전 중에는 듣기만 하셔도 됩니다. 대답하실 필요는 없습니다. 원하실 때만 소리 내어 따라 하세요. `⏸ 1초`
- **내레이터(KO)**: 오늘의 세 표현은, 새 출발 시간 묻기, 다음 비행기로 바꿔 달라고 하기, 보상 문의하기입니다. `⏸ 2초`

#### `ep03_c02` 안내 방송 듣기 — 주행(drive), 약 39초

- **내레이터(KO)**: 먼저 공항 안내 방송을 들어 보겠습니다. 핵심 단어는 딜레이드, 지연되었다는 말입니다. `⏸ 1초`
- **영어 음성(EN)**: Attention, please. Flight two-oh-seven to Singapore has been delayed. `⏸ 1초`
- **영어 음성(EN)**: The new departure time is four forty-five p.m. `⏸ 1초`
- **영어 음성(EN)**: We apologize for the inconvenience. `⏸ 2초`
- **내레이터(KO)**: 방송의 핵심은 두 가지였습니다. 비행기가 지연되었다는 것, 그리고 새 출발 시간이 오후 네 시 사십오 분이라는 것입니다. `⏸ 1초`
- **영어 음성(EN)**: My flight has been delayed. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *My flight has been delayed.*  
> AI 파트너: You look worried. What happened?  
> 사용자(예시): My flight has been delayed.  
> AI 파트너: Oh no, I'm sorry to hear that.

#### `ep03_c03` 표현 1: 새 출발 시간 묻기 — 주행(drive), 약 41초

- **내레이터(KO)**: 첫 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 방송을 놓쳤다면 직원에게 이렇게 묻습니다. 쿠쥬 텔 미는, 알려 주시겠어요, 라는 정중한 부탁입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you tell me the new departure time? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 게이트가 바뀌었는지 확인할 때도 같은 틀을 씁니다. `⏸ 1초`
- **영어 음성(EN)**: Could you tell me if the gate has changed? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 얼마나 늦어지는지 물을 때입니다. `⏸ 1초`
- **영어 음성(EN)**: How long is the delay? [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you tell me the new departure time?*  
> AI 파트너: Hello, this is the information desk. How can I help?  
> 사용자(예시): Could you tell me the new departure time?  
> AI 파트너: Of course. It's now four forty-five.

#### `ep03_c04` 표현 2: 다음 비행기로 바꾸기 — 주행(drive), 약 38초

- **내레이터(KO)**: 두 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 리북은, 다시 예약한다는 뜻입니다. 이즈 잇 파서블 투는, ~하는 게 가능할까요, 라는 부드러운 요청입니다. `⏸ 1초`
- **영어 음성(EN)**: Is it possible to rebook me [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: Is it possible to rebook me on the next flight? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 더 이른 비행기를 원할 때는 이렇게 바꿉니다. `⏸ 1초`
- **영어 음성(EN)**: Is it possible to get on an earlier flight? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Is it possible to rebook me on the next flight?*  
> AI 파트너: Your flight is delayed by three hours. What would you like to do?  
> 사용자(예시): Is it possible to rebook me on the next flight?  
> AI 파트너: Let me check. Yes, there's a seat available.

#### `ep03_c05` 표현 2 응용: 환승 걱정 말하기 — 주행(drive), 약 42초

- **내레이터(KO)**: 환승 비행기를 놓칠까 걱정될 때 쓰는 문장입니다. `⏸ 1초`
- **내레이터(KO)**: 싱가포르에서 환승한다고 말할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I have a connecting flight in Singapore. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 환승편을 놓칠 것 같다고 말할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm worried I'll miss my connection. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 직원은 보통 이렇게 답합니다. `⏸ 1초`
- **영어 음성(EN)**: Let me check what's available. There's a seat on the eight p.m. flight. `⏸ 1초`
- **영어 음성(EN)**: That works for me. Thank you. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm worried I'll miss my connection.*  
> AI 파트너: Is the delay a problem for you?  
> 사용자(예시): Yes. I'm worried I'll miss my connection.  
> AI 파트너: Don't worry. We'll find you a new flight.

#### `ep03_c06` 표현 3: 보상 문의하기 — 주행(drive), 약 42초

- **내레이터(KO)**: 세 번째 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 컴펜세이션은 보상이라는 뜻입니다. 지연에 대한 보상이 있는지 정중하게 물어봅니다. `⏸ 1초`
- **영어 음성(EN)**: Is there any compensation for the delay? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 식사 쿠폰을 받을 수 있는지 물을 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could I get a meal voucher while I wait? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 직원의 대답은 이렇게 들릴 수 있습니다. `⏸ 1초`
- **영어 음성(EN)**: Yes, here's a meal voucher. You can use it at any restaurant in this terminal. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Is there any compensation for the delay?*  
> AI 파트너: Again, we're very sorry for the long delay.  
> 사용자(예시): Is there any compensation for the delay?  
> AI 파트너: Yes, you'll get a meal voucher.

#### `ep03_c07` 전체 대화 듣기 — 주행(drive), 약 52초

- **내레이터(KO)**: 이제 게이트 앞에서 직원과 나누는 전체 대화를 들어 보겠습니다. 손님 문장 뒤에는 따라 할 틈을 두겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Excuse me. Could you tell me the new departure time? [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: It's now four forty-five, about two hours late. `⏸ 1초`
- **영어 음성(EN)**: I have a connecting flight in Singapore. Is it possible to rebook me on the next flight? [따라 말하기] `⏸ 4초`
- **영어 음성(EN)**: Let me check. Yes, I can put you on the eight p.m. connection. `⏸ 1초`
- **영어 음성(EN)**: Thank you. Is there any compensation for the delay? [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: You'll get a meal voucher. Here you go. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you tell me the new departure time?*  
> AI 파트너: Hi. Do you have a question about your flight?  
> 사용자(예시): Yes. Could you tell me the new departure time?  
> AI 파트너: Sure. It leaves at four forty-five.

#### `ep03_c08` 안내 방송 하나 더: 게이트 변경 — 주행(drive), 약 44초

- **내레이터(KO)**: 공항에서 자주 듣는 방송을 하나 더 들어 보겠습니다. 이번에는 게이트 변경입니다. `⏸ 1초`
- **영어 음성(EN)**: Attention, please. Passengers on flight two-oh-seven, please note that the departure gate has changed to gate twenty-two. `⏸ 1초`
- **내레이터(KO)**: 핵심은 출발 게이트가 이십이 번으로 바뀌었다는 것입니다. `⏸ 1초`
- **내레이터(KO)**: 게이트가 바뀌었다고 말할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: The gate has changed. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 새 게이트가 어디인지 물을 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you tell me where gate twenty-two is? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you tell me where gate ~ is?*  
> AI 파트너: Hi there. You look a little lost. Can I help?  
> 사용자(예시): Yes. Could you tell me where gate twenty-two is?  
> AI 파트너: Sure. Go straight and turn left.

#### `ep03_c09` 정리: 오늘의 세 표현 — 주행(drive), 약 43초

- **내레이터(KO)**: 오늘 배운 세 표현을 정리하겠습니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 새 출발 시간 묻기. `⏸ 1초`
- **영어 음성(EN)**: Could you tell me the new departure time? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 다음 비행기로 바꿔 달라고 하기. `⏸ 1초`
- **영어 음성(EN)**: Is it possible to rebook me on the next flight? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 셋째, 보상 문의하기. `⏸ 1초`
- **영어 음성(EN)**: Is there any compensation for the delay? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 수고하셨습니다. 다음 여행에서는 지연도 덜 당황스러울 거예요. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Is it possible to rebook me on the next flight?*  
> AI 파트너: I'm sorry, your connecting flight has been cancelled.  
> 사용자(예시): Is it possible to rebook me on the next flight?  
> AI 파트너: Of course. You're on the nine o'clock flight.

### 주차·장시간 정차 롤플레이 (deferred_roleplays)

선형 재생 위치 없음. 기어 P 또는 60초 이상 정차 시에만 제안됩니다.

#### `ep03_rp01` 정차 롤플레이: 게이트 직원과 대화 — trigger: `parked_or_long_stop`, 최대 5분

- **내레이터(KO)**: 차를 세워 두셨을 때 열리는 대화 연습입니다. `⏸ 1초`
- **내레이터(KO)**: 저는 게이트 직원입니다. 비행기가 지연된 상황에서, 새 출발 시간을 묻고, 환승편을 바꾸고, 보상도 문의해 보세요. `⏸ 1초`
- **내레이터(KO)**: 막히면 힌트, 라고 말씀하시면 됩니다. `⏸ 1초`
- **AI 파트너**: Hello. I'm sorry, this flight is delayed. How can I help you? `⏸ 0초`

**AI 파트너 지시문 (system)**

```text
You are a polite but busy gate agent at an airport for a fictional airline (do not name any real airline). Speak natural CEFR B1 English. Situation: the learner's flight to Singapore is delayed by two hours and they have a connecting flight. Answer questions about the new time, offer rebooking options (one is fully booked, one has seats), and explain that a meal voucher is available but cash compensation depends on the airline policy. Encourage 'Could you tell me ~?', 'Is it possible to rebook me on ~?', 'Is there any compensation ~?'. Recast errors naturally instead of correcting. If the learner says 'hint' or is silent for 8 seconds, give one Korean hint with an English phrase. Keep turns under 30 words. End after the issue is resolved or after 5 minutes with a 2-sentence Korean feedback. Stop immediately if the app signals the vehicle is moving.
```

**샘플 대화**

- AI 파트너: Hello. I'm sorry, this flight is delayed. How can I help you?
- 사용자(예시): Could you tell me the new departure time? I have a connecting flight.
- AI 파트너: It's now four forty-five. Where is your connection to, and what time does it leave?
- 사용자(예시): To Sydney, at six. Is it possible to rebook me on the next flight?
- AI 파트너: The six-thirty is full, but I can rebook you on the eight p.m. flight. Would that work?

---

## 면접 리허설: 직무 면접 자주 나오는 질문 3개

- **ID**: `ep04_interview_rehearsal_ko`  
- **레벨**: 취업·이직 준비 (한국어)  
- **목표 길이**: 약 6분 (선형 재생 추정 6분 4초, 주행 청크 9개)  
- **선택 정차 연습**: 최대 9분 0초 (선형 재생 시간에 미포함)

- **비고**: 지원 직무는 '서비스 기획'을 예시로 하며, 회사명은 등장하지 않습니다. 모범 답변의 수치·경험은 모두 가상입니다. 주행 모드에서는 질문을 의문문으로 던지지 않고 '첫 번째 질문은 ~입니다'처럼 평서문으로 소개합니다.

### 학습 목표 (핵심 표현 3)

1. **저는 ~한 경험을 통해 ~ 역량을 쌓아 온 ~입니다.** — 자기소개 첫 문장: 정체성 + 근거 + 역량을 한 문장에 (자기소개 (결론 먼저))
2. **제가 이 직무에 지원한 이유는 두 가지입니다.** — 답의 개수를 먼저 밝혀 듣는 사람이 구조를 따라오게 하기 (지원 동기 (번호 매기기))
3. **그 경험을 통해 ~를 배웠고, 이후에는 ~하고 있습니다.** — 경험 질문을 배운 점과 변화로 마무리하기 (경험 질문 (STAR 마무리))

### 주행 청크 (선형 재생 순서)

#### `ep04_c01` 인트로 — 주행(drive), 약 34초

- **내레이터(KO)**: 안녕하세요, 다다캐스트의 다다예요. `⏸ 1초`
- **내레이터(KO)**: 오늘은 직무 면접에서 자주 나오는 질문 세 가지를 리허설합니다. `⏸ 1초`
- **내레이터(KO)**: 주행 중에는 질문과 모범 답변의 뼈대를 듣고, 원하실 때만 문장을 따라 하시면 됩니다. `⏸ 1초`
- **내레이터(KO)**: 신호를 기다릴 때는 방금 들은 문장으로 짧게 말해 보는 연습이, 차를 세워 두셨을 때는 실전 답변 연습이 열립니다. `⏸ 1초`
- **내레이터(KO)**: 오늘 다룰 질문은 자기소개, 지원 동기, 그리고 어려움을 극복한 경험입니다. `⏸ 2초`

#### `ep04_c02` 질문 1 구조: 자기소개 — 주행(drive), 약 35초

- **내레이터(KO)**: 첫 번째 질문은, 일 분 자기소개입니다. `⏸ 1초`
- **내레이터(KO)**: 핵심은 결론부터 말하는 것입니다. 구조는 세 단계입니다. `⏸ 1초`
- **내레이터(KO)**: 하나, 나는 어떤 사람인지 한 문장으로. `⏸ 1초`
- **내레이터(KO)**: 둘, 그것을 보여 주는 경험 하나. `⏸ 1초`
- **내레이터(KO)**: 셋, 이 회사에서 어떻게 기여할지 한 문장으로. `⏸ 2초`
- **내레이터(KO)**: 첫 문장의 틀을 따라 해 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 저는 고객 데이터를 바탕으로 서비스를 개선해 온 기획자입니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *저는 ~한 경험을 통해 ~ 역량을 쌓아 온 ~입니다.*  
> AI 파트너: 자기소개 첫 문장만 한 문장으로 말씀해 주세요.  
> 사용자(예시): 저는 고객 데이터를 바탕으로 서비스를 개선해 온 기획자입니다.  
> AI 파트너: 좋습니다. 결론이 먼저 나와서 선명합니다.

#### `ep04_c03` 질문 1 모범 답변 따라 하기 — 주행(drive), 약 40초

- **내레이터(KO)**: 모범 답변을 문장 단위로 들려 드립니다. 문장 뒤의 짧은 틈에 따라 하셔도 좋습니다. `⏸ 1초`
- **내레이터(KO)**: 저는 고객 데이터를 바탕으로 서비스를 개선해 온 기획자입니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 이전 회사에서 회원 가입 단계를 다섯 단계에서 세 단계로 줄여, 가입 완료율을 십오 퍼센트 높였습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 입사하게 된다면, 사용자 행동 데이터를 근거로 빠르게 가설을 세우고 검증하는 기획자가 되겠습니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *숫자가 들어간 경험 한 문장*  
> AI 파트너: 그 역량을 보여 주는 경험을, 숫자 하나를 넣어 한 문장으로 말씀해 주세요.  
> 사용자(예시): 가입 단계를 다섯 단계에서 세 단계로 줄여 가입 완료율을 십오 퍼센트 높였습니다.  
> AI 파트너: 숫자가 들어가니 설득력이 생깁니다.

#### `ep04_c04` 질문 2 구조: 지원 동기 — 주행(drive), 약 36초

- **내레이터(KO)**: 두 번째 질문은, 우리 회사와 이 직무에 지원한 이유입니다. `⏸ 1초`
- **내레이터(KO)**: 이 질문에서는 이유의 개수를 먼저 말하면 훨씬 정돈되어 들립니다. `⏸ 1초`
- **내레이터(KO)**: 제가 이 직무에 지원한 이유는 두 가지입니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 첫째 이유는 회사가 하는 일과 나의 관심이 맞닿는 지점, 둘째 이유는 내 경험이 바로 쓰일 수 있는 지점으로 구성합니다. `⏸ 1초`
- **내레이터(KO)**: 회사 칭찬만 늘어놓기보다, 나와 연결되는 근거를 말하는 것이 좋습니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *제가 이 직무에 지원한 이유는 두 가지입니다.*  
> AI 파트너: 지원 동기 답변의 첫 문장만 말씀해 주세요.  
> 사용자(예시): 제가 이 직무에 지원한 이유는 두 가지입니다.  
> AI 파트너: 좋습니다. 듣는 사람이 구조를 바로 알 수 있어요.

#### `ep04_c05` 질문 2 모범 답변 따라 하기 — 주행(drive), 약 43초

- **내레이터(KO)**: 모범 답변입니다. `⏸ 1초`
- **내레이터(KO)**: 제가 이 직무에 지원한 이유는 두 가지입니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 첫째, 이동하는 시간을 더 가치 있게 만드는 서비스에 오래 관심을 가져 왔습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 제가 해 온 데이터 기반 개선 경험이, 이 직무의 사용자 지표 개선 업무에 바로 쓰일 수 있다고 생각했습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 그래서 입사 후 첫 육 개월 안에, 핵심 지표 하나를 눈에 띄게 개선하는 것을 목표로 하겠습니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *첫째, ~.*  
> AI 파트너: 지원 이유 중 첫째 이유만 한 문장으로 말씀해 주세요.  
> 사용자(예시): 첫째, 이동 시간을 더 가치 있게 만드는 서비스에 오래 관심을 가져 왔습니다.  
> AI 파트너: 좋아요. 첫째로 시작해서 정돈되어 들립니다.

#### `ep04_c06` 질문 3 구조: 어려움을 극복한 경험 — 주행(drive), 약 35초

- **내레이터(KO)**: 세 번째 질문은, 어려움을 극복했던 경험입니다. `⏸ 1초`
- **내레이터(KO)**: 이때는 스타 구조를 씁니다. 상황, 과제, 행동, 결과의 순서입니다. `⏸ 1초`
- **내레이터(KO)**: 상황과 과제는 짧게, 내가 한 행동은 구체적으로, 결과는 숫자로 말합니다. `⏸ 1초`
- **내레이터(KO)**: 그리고 마지막에 배운 점으로 마무리합니다. 마무리 문장의 틀입니다. `⏸ 1초`
- **내레이터(KO)**: 그 경험을 통해 일정보다 합의가 먼저라는 것을 배웠고, 이후에는 회의 전에 기준부터 맞추고 있습니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *그 경험을 통해 ~를 배웠고, 이후에는 ~하고 있습니다.*  
> AI 파트너: 어려웠던 경험에서 배운 점을 한 문장으로 마무리해 보세요.  
> 사용자(예시): 그 경험을 통해 합의가 먼저라는 것을 배웠고, 이후에는 기준부터 맞추고 있습니다.  
> AI 파트너: 배운 점과 달라진 행동이 함께 있어 좋습니다.

#### `ep04_c07` 질문 3 모범 답변 따라 하기 — 주행(drive), 약 44초

- **내레이터(KO)**: 모범 답변입니다. `⏸ 1초`
- **내레이터(KO)**: 출시 이 주 전에, 개발팀과 디자인팀의 의견이 크게 엇갈린 적이 있습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 저는 두 팀의 요구를 표 하나로 정리하고, 사용자 테스트로 결정하자고 제안했습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 닷새 동안 스무 명을 테스트한 결과를 바탕으로 합의했고, 일정도 지킬 수 있었습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 그 경험을 통해 일정보다 합의가 먼저라는 것을 배웠고, 이후에는 회의 전에 기준부터 맞추고 있습니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *저는 ~하자고 제안했습니다.*  
> AI 파트너: 그 상황에서 본인이 한 행동만 한 문장으로 말씀해 주세요.  
> 사용자(예시): 저는 두 팀의 요구를 표로 정리하고 사용자 테스트로 결정하자고 제안했습니다.  
> AI 파트너: 저는으로 시작해서 본인의 역할이 분명합니다.

#### `ep04_c08` 꼬리 질문 대비: 숫자와 역할 — 주행(drive), 약 55초

- **내레이터(KO)**: 면접관은 답변에 나온 숫자를 자주 다시 묻습니다. `⏸ 1초`
- **내레이터(KO)**: 예를 들어, 가입 완료율 십오 퍼센트 상승이 정말 그 변화 덕분인지에 대한 꼬리 질문입니다. `⏸ 1초`
- **내레이터(KO)**: 이때는 비교 방법과 기간을 짧게 말하면 됩니다. `⏸ 1초`
- **내레이터(KO)**: 숫자는 기간과 비교 대상이 함께 있을 때 믿음을 줍니다. `⏸ 1초`
- **내레이터(KO)**: 변경 전후 각각 이 주 동안의 가입 완료율을 비교한 결과입니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 혼자 다 한 일처럼 들리지 않도록, 내 역할을 분명히 하는 문장도 준비합니다. `⏸ 1초`
- **내레이터(KO)**: 저는 문제 정의와 개선안 설계를 맡았고, 개발은 개발팀과 함께 진행했습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 숫자의 근거와 내 역할, 이 두 가지만 준비해 두면 꼬리 질문도 당황하지 않습니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *저는 ~을 맡았고, ~는 ~와 함께 진행했습니다.*  
> AI 파트너: 그 프로젝트에서 본인의 역할은 정확히 무엇이었나요? 한 문장으로 답해 주세요.  
> 사용자(예시): 저는 문제 정의와 개선안 설계를 맡았고, 개발은 개발팀과 함께 진행했습니다.  
> AI 파트너: 역할이 분명하게 구분되어 신뢰가 갑니다.

#### `ep04_c09` 정리: 오늘의 세 문장 틀 — 주행(drive), 약 42초

- **내레이터(KO)**: 오늘의 세 문장 틀을 정리하겠습니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 자기소개는 결론부터. `⏸ 1초`
- **내레이터(KO)**: 저는 고객 데이터를 바탕으로 서비스를 개선해 온 기획자입니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 지원 동기는 개수부터. `⏸ 1초`
- **내레이터(KO)**: 제가 이 직무에 지원한 이유는 두 가지입니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 셋째, 경험 질문은 배운 점으로 마무리. `⏸ 1초`
- **내레이터(KO)**: 그 경험을 통해 배웠고, 이후에는 이렇게 하고 있습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 수고하셨습니다. 면접 잘 보시길 응원합니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *제가 이 직무에 지원한 이유는 두 가지입니다.*  
> AI 파트너: 마지막으로, 지원 동기의 첫 문장을 다시 한번 말씀해 주세요.  
> 사용자(예시): 제가 이 직무에 지원한 이유는 두 가지입니다.  
> AI 파트너: 완벽합니다. 이 문장 하나로 답변이 정돈됩니다.

### 주차·장시간 정차 롤플레이 (deferred_roleplays)

선형 재생 위치 없음. 기어 P 또는 60초 이상 정차 시에만 제안됩니다.

#### `ep04_rp01` 정차 연습: 실제 답변 말해 보기 — trigger: `parked_or_long_stop`, 최대 5분

- **내레이터(KO)**: 차를 세워 두셨을 때 열리는 실전 연습입니다. `⏸ 1초`
- **내레이터(KO)**: 제가 면접관이 되어 오늘의 세 질문을 하나씩 드리겠습니다. 이번에는 직접 답변해 보세요. `⏸ 1초`
- **내레이터(KO)**: 답변이 끝나면 구조와 길이에 대해 짧게 피드백을 드리겠습니다. `⏸ 1초`
- **AI 파트너**: 안녕하세요. 편하게 시작하겠습니다. 먼저 1분 정도로 자기소개 부탁드립니다. `⏸ 0초`

**AI 파트너 지시문 (system)**

```text
당신은 차분하고 정중한 한국어 직무 면접관입니다(가상의 회사, 서비스 기획 직무). 질문은 자기소개, 지원 동기, 어려움을 극복한 경험 순서로 한 번에 하나씩 합니다. 사용자의 답변이 끝나면 2~3문장으로 피드백합니다: (1) 결론을 먼저 말했는지, (2) 근거 경험이 구체적이고 숫자가 있는지, (3) 길이가 60~90초 안인지. 칭찬 하나와 개선점 하나를 말하고, 개선된 첫 문장 예시를 한 줄 제시합니다. 필요하면 꼬리 질문을 한 번만 합니다. 사용자가 '힌트'라고 하거나 8초 이상 말이 없으면 답변 구조(예: '이유는 두 가지입니다')를 한 줄로 알려 줍니다. 실존 기업·인물을 언급하지 않습니다. 차량이 출발했다는 신호가 오면 즉시 질문 없이 종료합니다.
```

**샘플 대화**

- AI 파트너: 안녕하세요. 편하게 시작하겠습니다. 먼저 1분 정도로 자기소개 부탁드립니다.
- 사용자(예시): 안녕하세요. 저는 고객 의견을 듣고 서비스를 개선하는 일을 해 왔습니다. 이전 회사에서 앱 리뷰를 분석해서 불편 사항을 정리했고...
- AI 파트너: 경험을 구체적으로 말씀해 주셔서 좋았습니다. 다만 결과 수치가 빠졌습니다. 첫 문장을 '저는 고객 리뷰 분석으로 불편을 줄여 온 기획자입니다'처럼 결론부터 시작해 보시면 더 선명해집니다. 다음 질문입니다. 저희 직무에 지원하신 이유는 무엇인가요?

---

## 내 자료로 에피소드 (데모): 3분기 실적 발표 리허설

- **ID**: `ep05_my_material_demo`  
- **레벨**: 발표 리허설 (한국어)  
- **목표 길이**: 약 6분 (선형 재생 추정 6분 7초, 주행 청크 9개)  
- **선택 정차 연습**: 최대 9분 0초 (선형 재생 시간에 미포함)

### 학습 목표 (핵심 표현 3)

1. **이번 분기 매출은 사백십이억 원으로, 전년 같은 기간보다 십이 퍼센트 늘었습니다.** — 핵심 수치 문장: 숫자 + 비교 기준을 한 호흡에 (핵심 수치 전달)
2. **사 분기에는 세 가지에 집중하겠습니다.** — 계획을 개수로 먼저 예고하기 (계획 구조화)
3. **외형 성장은 확인했고, 이제는 수익성을 회복할 차례입니다.** — 발표 전체를 한 문장으로 요약하는 클로징 (한 줄 요약 클로징)

### 원본 자료 (사용자 업로드 가정)

> 가상 회사(새벽별식품)와 가상 수치로 작성한 데모용 발표 원고입니다. 실존 기업·인물과 무관합니다.

> 안녕하십니까, 영업기획팀입니다. 지금부터 새벽별식품의 2026년 3분기 실적을 보고드리겠습니다. 이번 분기 매출은 412억 원으로, 전년 같은 기간보다 12% 늘었습니다. 성장을 이끈 것은 간편식 정기배송 서비스입니다. 정기배송 가입자는 3분기에만 4만 2천 명이 새로 늘어 누적 18만 명을 넘어섰고, 특히 30대 맞벌이 가구가 신규 가입자의 절반을 차지했습니다. 반면 영업이익률은 6.1%로, 지난 분기보다 0.8%포인트 낮아졌습니다. 원재료 가격 상승과 물류센터 확장 비용이 반영된 결과입니다. 4분기에는 세 가지에 집중하겠습니다. 첫째, 포장재 공급처를 다변화해 원가를 낮추겠습니다. 둘째, 수도권 신규 물류센터를 11월부터 정상 가동해 배송 단가를 줄이겠습니다. 셋째, 정기배송 해지율을 현재 9%에서 7%까지 낮추겠습니다. 정리하면, 외형 성장은 확인했고 이제는 수익성을 회복할 차례입니다. 이상으로 보고를 마치겠습니다. 감사합니다.

### 변환 방식

- 1) 원고를 문장 단위로 나누고, 발표의 뼈대가 되는 문장(오프닝, 핵심 수치, 4분기 계획, 클로징)을 '따라 말하기 문장'으로 선정.
- 2) TTS 정규화: 숫자·기호를 한글 읽기로 변환 (412억 원 → 사백십이억 원, 12% → 십이 퍼센트, 0.8%포인트 → 영 점 팔 퍼센트포인트).
- 3) 긴 문장은 호흡 단위로 분할하고, 따라 말하기 문장 뒤에 3~4초 무음 삽입.
- 4) 원고 내용에서 예상 질문(수익성 하락 원인, 해지율 목표 근거, 물류센터 투자 회수)을 추출해 주행 모드에서는 '질문 소개 + 답변 뼈대' 평서문으로, 정차 모드에서는 AI 임원과의 실제 Q&A로 배치.
- 5) 모든 청크는 30~60초 단위로 끊어, 회전 안내·주의 지점은 청크 경계에서, 방향지시등은 문장이 끝나는 지점에서 멈추고 중단된 청크 처음부터 재개.

### 주행 청크 (선형 재생 순서)

#### `ep05_c01` 인트로: 내 자료로 만든 에피소드 — 주행(drive), 약 39초

- **내레이터(KO)**: 안녕하세요, 다다캐스트의 다다예요. `⏸ 1초`
- **내레이터(KO)**: 이 에피소드는 사용자가 올린 발표 원고로 자동 생성된 리허설 예시입니다. `⏸ 1초`
- **내레이터(KO)**: 원고는 가상 회사인 새벽별식품의 삼 분기 실적 보고입니다. `⏸ 1초`
- **내레이터(KO)**: 원고에서 따라 말할 핵심 문장과, 받을 만한 예상 질문을 뽑아 구성했습니다. `⏸ 1초`
- **내레이터(KO)**: 주행 중에는 핵심 문장을 듣고 원하실 때만 따라 말하시면 됩니다. `⏸ 1초`
- **내레이터(KO)**: 신호를 기다릴 때는 핵심 문장으로 짧게, 차를 세워 두셨을 때는 예상 질문에 길게 답해 보는 연습이 열립니다. `⏸ 2초`

#### `ep05_c02` 발표 전체 흐름 요약 — 주행(drive), 약 32초

- **내레이터(KO)**: 먼저 발표 전체의 흐름을 세 줄로 요약해 드립니다. `⏸ 1초`
- **내레이터(KO)**: 하나, 매출은 사백십이억 원으로 작년보다 십이 퍼센트 늘었습니다. 정기배송 서비스가 성장을 이끌었습니다. `⏸ 2초`
- **내레이터(KO)**: 둘, 영업이익률은 육 점 일 퍼센트로 조금 낮아졌습니다. 원재료와 물류센터 비용 때문입니다. `⏸ 2초`
- **내레이터(KO)**: 셋, 사 분기에는 원가 절감, 물류센터 가동, 해지율 개선, 이 세 가지에 집중합니다. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *성장을 이끈 것은 간편식 정기배송 서비스입니다.*  
> AI 파트너: 매출이 늘어난 가장 큰 이유를 한 문장으로 말씀해 주세요.  
> 사용자(예시): 성장을 이끈 것은 간편식 정기배송 서비스입니다.  
> AI 파트너: 핵심을 정확히 짚으셨습니다.

#### `ep05_c03` 핵심 문장 1: 오프닝과 매출 — 주행(drive), 약 45초

- **내레이터(KO)**: 발표의 첫인상을 정하는 오프닝 문장입니다. `⏸ 1초`
- **내레이터(KO)**: 지금부터 새벽별식품의 이천이십육 년 삼 분기 실적을 보고드리겠습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 다음은 핵심 수치 문장입니다. 숫자와 비교 기준을 한 호흡에 말하는 것이 포인트입니다. `⏸ 1초`
- **내레이터(KO)**: 이번 분기 매출은 사백십이억 원으로, [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 전년 같은 기간보다 십이 퍼센트 늘었습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 이번 분기 매출은 사백십이억 원으로, 전년 같은 기간보다 십이 퍼센트 늘었습니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *이번 분기 매출은 사백십이억 원으로, 전년 같은 기간보다 십이 퍼센트 늘었습니다.*  
> AI 파트너: 이번 분기 매출을 비교 기준과 함께 한 문장으로 말씀해 주세요.  
> 사용자(예시): 이번 분기 매출은 사백십이억 원으로, 전년 같은 기간보다 십이 퍼센트 늘었습니다.  
> AI 파트너: 숫자와 비교 기준이 한 호흡에 들어갔습니다. 좋습니다.

#### `ep05_c04` 핵심 문장 2: 성장 요인과 이익률 — 주행(drive), 약 36초

- **내레이터(KO)**: 성장 요인을 설명하는 문장입니다. `⏸ 1초`
- **내레이터(KO)**: 성장을 이끈 것은 간편식 정기배송 서비스입니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 좋지 않은 숫자는 원인과 함께, 담담하게 말하는 것이 좋습니다. `⏸ 1초`
- **내레이터(KO)**: 반면 영업이익률은 육 점 일 퍼센트로, 지난 분기보다 영 점 팔 퍼센트포인트 낮아졌습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 원재료 가격 상승과 물류센터 확장 비용이 반영된 결과입니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *원재료 가격 상승과 물류센터 확장 비용이 반영된 결과입니다.*  
> AI 파트너: 이익률이 떨어진 이유를 한 문장으로 설명해 주세요.  
> 사용자(예시): 원재료 가격 상승과 물류센터 확장 비용이 반영된 결과입니다.  
> AI 파트너: 원인을 담담하게 잘 말씀하셨습니다.

#### `ep05_c05` 핵심 문장 3: 사 분기 계획과 클로징 — 주행(drive), 약 48초

- **내레이터(KO)**: 계획을 말할 때는 개수부터 예고합니다. `⏸ 1초`
- **내레이터(KO)**: 사 분기에는 세 가지에 집중하겠습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 첫째, 포장재 공급처를 다변화해 원가를 낮추겠습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 둘째, 수도권 신규 물류센터를 십일 월부터 정상 가동하겠습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 셋째, 정기배송 해지율을 구 퍼센트에서 칠 퍼센트까지 낮추겠습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 마지막은 발표 전체를 한 문장으로 묶는 클로징입니다. `⏸ 1초`
- **내레이터(KO)**: 외형 성장은 확인했고, 이제는 수익성을 회복할 차례입니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *사 분기에는 세 가지에 집중하겠습니다.*  
> AI 파트너: 사 분기 계획을 소개하는 첫 문장만 말씀해 주세요.  
> 사용자(예시): 사 분기에는 세 가지에 집중하겠습니다.  
> AI 파트너: 개수를 먼저 말해서 청중이 따라오기 쉽습니다.

#### `ep05_c06` 예상 질문 1: 이익률 하락 — 주행(drive), 약 41초

- **내레이터(KO)**: 이제 원고에서 뽑은 예상 질문과 답변 뼈대를 들려 드립니다. 지금은 듣기만 하셔도 됩니다. `⏸ 1초`
- **내레이터(KO)**: 첫 번째 예상 질문은, 이익률이 떨어진 원인이 일시적인지에 대한 것입니다. `⏸ 1초`
- **내레이터(KO)**: 답변 뼈대는, 일회성 비용과 구조적 비용을 나눠서 말하는 것입니다. `⏸ 1초`
- **내레이터(KO)**: 물류센터 확장 비용은 일회성이고, 십일 월 가동 이후에는 배송 단가 절감으로 회수됩니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 원재료 부담은 공급처 다변화로 사 분기부터 줄여 나가겠습니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *물류센터 확장 비용은 일회성이고, ~ 회수됩니다.*  
> AI 파트너: 이익률 하락은 일시적인 겁니까? 한 문장으로 답해 주세요.  
> 사용자(예시): 물류센터 확장 비용은 일회성이고, 십일 월 가동 이후 배송 단가 절감으로 회수됩니다.  
> AI 파트너: 결론부터 말씀하셔서 좋습니다.

#### `ep05_c07` 예상 질문 2·3: 해지율 목표와 투자 회수 — 주행(drive), 약 47초

- **내레이터(KO)**: 두 번째 예상 질문은, 해지율 칠 퍼센트 목표의 근거입니다. `⏸ 1초`
- **내레이터(KO)**: 답변 뼈대는, 해지 이유 데이터와 구체적인 대책을 연결하는 것입니다. `⏸ 1초`
- **내레이터(KO)**: 해지 사유의 절반이 배송 시간 불만이어서, 신규 물류센터로 배송 시간을 줄이면 목표 달성이 가능하다고 봅니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 세 번째 예상 질문은, 물류센터 투자를 언제 회수하느냐입니다. `⏸ 1초`
- **내레이터(KO)**: 이 질문은 숫자가 원고에 없으므로, 확인 후 답변드리겠다는 정직한 답이 안전합니다. `⏸ 1초`
- **내레이터(KO)**: 정확한 회수 시점은 재무팀과 확인해 별도로 공유드리겠습니다. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *정확한 ~는 확인해 별도로 공유드리겠습니다.*  
> AI 파트너: 물류센터 투자는 언제 회수됩니까?  
> 사용자(예시): 정확한 회수 시점은 재무팀과 확인해 별도로 공유드리겠습니다.  
> AI 파트너: 원고에 없는 숫자를 지어내지 않은 좋은 답입니다.

#### `ep05_c08` 숫자 읽기 연습 — 주행(drive), 약 37초

- **내레이터(KO)**: 발표에서 가장 자주 꼬이는 것이 숫자입니다. 원고의 숫자만 모아 한 번씩 읽어 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 사백십이억 원. [따라 말하기] `⏸ 2초`
- **내레이터(KO)**: 사만 이천 명, 누적 십팔만 명. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 육 점 일 퍼센트. [따라 말하기] `⏸ 2초`
- **내레이터(KO)**: 영 점 팔 퍼센트포인트. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 구 퍼센트에서 칠 퍼센트로. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 숫자 뒤의 단위까지 또박또박 말하면 듣는 사람이 메모하기 쉽습니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *정기배송 가입자는 누적 십팔만 명을 넘어섰습니다.*  
> AI 파트너: 정기배송 가입자 현황을 숫자를 넣어 한 문장으로 말씀해 주세요.  
> 사용자(예시): 정기배송 가입자는 삼 분기에만 사만 이천 명이 늘어, 누적 십팔만 명을 넘어섰습니다.  
> AI 파트너: 숫자 두 개를 순서대로 정확하게 말씀하셨습니다.

#### `ep05_c09` 정리: 발표의 세 문장 — 주행(drive), 약 42초

- **내레이터(KO)**: 발표의 뼈대가 되는 세 문장을 정리하겠습니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 핵심 수치. `⏸ 1초`
- **내레이터(KO)**: 이번 분기 매출은 사백십이억 원으로, 전년 같은 기간보다 십이 퍼센트 늘었습니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 계획 예고. `⏸ 1초`
- **내레이터(KO)**: 사 분기에는 세 가지에 집중하겠습니다. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 셋째, 한 줄 클로징. `⏸ 1초`
- **내레이터(KO)**: 외형 성장은 확인했고, 이제는 수익성을 회복할 차례입니다. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 수고하셨습니다. 발표 잘 하시길 응원합니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *외형 성장은 확인했고, 이제는 수익성을 회복할 차례입니다.*  
> AI 파트너: 발표를 마무리하는 클로징 문장을 말씀해 주세요.  
> 사용자(예시): 외형 성장은 확인했고, 이제는 수익성을 회복할 차례입니다.  
> AI 파트너: 발표 전체가 한 문장에 담겼습니다. 훌륭합니다.

### 주차·장시간 정차 롤플레이 (deferred_roleplays)

선형 재생 위치 없음. 기어 P 또는 60초 이상 정차 시에만 제안됩니다.

#### `ep05_rp01` 정차 연습: 임원 Q&A 리허설 — trigger: `parked_or_long_stop`, 최대 5분

- **내레이터(KO)**: 차를 세워 두셨을 때 열리는 실전 질의응답 연습입니다. `⏸ 1초`
- **내레이터(KO)**: 저는 발표를 들은 임원 역할입니다. 원고 내용을 바탕으로 질문을 드릴 테니 직접 답변해 보세요. `⏸ 1초`
- **내레이터(KO)**: 답변이 끝날 때마다 짧게 피드백해 드리겠습니다. `⏸ 1초`
- **AI 파트너**: 발표 잘 들었습니다. 그런데 매출은 늘었는데 이익률은 왜 떨어진 겁니까? 일시적인 건가요? `⏸ 0초`

**AI 파트너 지시문 (system)**

```text
당신은 가상 회사 '새벽별식품'의 실적 보고를 들은 임원입니다. 아래 source_material 원고에 있는 사실만 근거로 질문합니다. 질문 순서: (1) 이익률 하락 원인과 일시성, (2) 해지율 7% 목표의 근거, (3) 물류센터 투자 회수 시점(원고에 없는 정보). 약간 날카롭지만 무례하지 않은 어조를 유지합니다. 사용자의 답변마다 2문장 이내로 피드백합니다: 결론을 먼저 말했는지, 원고 수치를 정확히 인용했는지, 원고에 없는 내용을 지어내지 않았는지. 사용자가 원고에 없는 수치를 말하면 그 점을 지적하고 '확인 후 공유드리겠습니다' 같은 안전한 답을 제안합니다. 사용자가 '힌트'라고 하거나 8초 이상 말이 없으면 답변 뼈대를 한 줄로 줍니다. 5분 또는 질문 3개 후 종료하고 총평을 3문장으로 말합니다. 차량 출발 신호가 오면 즉시 질문 없이 종료합니다.
```

**샘플 대화**

- AI 파트너: 발표 잘 들었습니다. 그런데 매출은 늘었는데 이익률은 왜 떨어진 겁니까? 일시적인 건가요?
- 사용자(예시): 네, 원재료 가격이 오르고 물류센터 확장 비용이 들어갔기 때문입니다. 물류센터 비용은 일회성이라 11월 가동 이후에는 개선될 것으로 봅니다.
- AI 파트너: 원인을 두 가지로 나눠 말씀하신 점이 명확했습니다. '일회성'이라는 결론을 첫 문장에 두면 더 좋겠습니다. 다음 질문입니다. 해지율을 7%까지 낮출 수 있다는 근거는 무엇입니까?

---

## 영어 회화 B1: 출근길 전화로 회의 미루기

- **ID**: `ep06_commute_running_late`  
- **레벨**: B1  
- **목표 길이**: 약 32분 (선형 재생 추정 32분 33초, 주행 청크 43개)  
- **선택 정차 연습**: 최대 26분 0초 (선형 재생 시간에 미포함)

- **구성**: 워밍업 약 3분 10초 (`ep06_c01`~`ep06_c04`, 4청크) → 본편 약 25분 29초 (`ep06_c05`~`ep06_c38`, 34청크) → 복습 약 3분 54초 (`ep06_c39`~`ep06_c43`, 5청크)

- **비고**: 기획서 ③ 통근 시간 맞춤 구성(워밍업 3분 + 본편 25분 + 복습 4분)을 한 에피소드로 담은 풀코스 편입니다. 구간은 sections와 청크 제목 머리말(워밍업·본편·복습)로 표시합니다. 등장인물(민호, 그레이스, 톰, 사라)과 회사는 모두 가상입니다. 주행 청크의 영어는 모두 en_voice 한 목소리로 읽으므로, 대화문은 이름과 내용으로 화자가 드러나게 썼습니다.

### 학습 목표 (핵심 표현 3)

1. **I'm running about fifteen minutes late.** — 십오 분쯤 늦을 것 같아요. (늦는다고 알리기)
2. **Could you cover for me until I get there?** — 제가 도착할 때까지 대신 좀 맡아 줄 수 있어요? (동료에게 부탁하기)
3. **Could we push the meeting back to eleven?** — 회의를 열한 시로 미룰 수 있을까요? (일정 미루기)

### 주행 청크 (선형 재생 순서)

#### `ep06_c01` 워밍업 · 오늘의 출근길 이야기 — 주행(drive), 약 51초

- **내레이터(KO)**: 안녕하세요, 다다캐스트의 다다예요. `⏸ 1초`
- **내레이터(KO)**: 오늘은 출근길을 통째로 채우는 긴 편입니다. 삼 분 워밍업, 이십오 분 본편, 사 분 복습 순서로 이어집니다. `⏸ 1초`
- **내레이터(KO)**: 주제는 출근길 영어 전화입니다. 차가 막혀 늦을 때 연락하고, 동료에게 부탁하고, 고객과 회의 시간을 다시 맞추는 상황이에요. `⏸ 1초`
- **내레이터(KO)**: 운전 중에는 듣기만 하셔도 됩니다. 대답하실 필요는 없습니다. 원하실 때만 소리 내어 따라 하세요. `⏸ 1초`
- **내레이터(KO)**: 오늘의 주인공은 디자인팀의 민호 씨입니다. 월요일 아침 열 시에 중요한 고객 미팅이 있는데, 고속도로에서 사고가 나 차가 꼼짝하지 않습니다. `⏸ 2초`

#### `ep06_c02` 워밍업 · 지난 표현으로 입 풀기 — 주행(drive), 약 48초

- **내레이터(KO)**: 본격적으로 시작하기 전에 입을 먼저 풀어 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 지난 공항 편에서 쿠쥬 텔 미라는 정중한 부탁을 연습했습니다. `⏸ 1초`
- **영어 음성(EN)**: Could you tell me the new departure time? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 쿠쥬 뒤에 동사만 바꾸면 어떤 부탁이든 만들 수 있습니다. 오늘도 이 틀이 여러 번 나옵니다. `⏸ 1초`
- **내레이터(KO)**: 다시 전화해 달라고 부탁할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you call me back? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 파일을 보내 달라고 부탁할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you send me the file? [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you ~?*  
> AI 파트너: I'm free now. Do you need anything from me?  
> 사용자(예시): Could you send me the file?  
> AI 파트너: Sure. I'll send it right away.

#### `ep06_c03` 워밍업 · 오늘의 세 표현 미리 듣기 — 주행(drive), 약 47초

- **내레이터(KO)**: 오늘의 핵심 표현 세 개를 먼저 들어 보겠습니다. 지금은 뜻만 가볍게 잡으시면 됩니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 늦는다고 알리는 표현입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm running about fifteen minutes late. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 내가 도착할 때까지 대신 맡아 달라고 부탁하는 표현입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you cover for me until I get there? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 셋째, 회의 시간을 뒤로 미루자고 제안하는 표현입니다. `⏸ 1초`
- **영어 음성(EN)**: Could we push the meeting back to eleven? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm running about ~ late.*  
> AI 파트너: Hi, where are you? The meeting starts soon.  
> 사용자(예시): I'm running about fifteen minutes late.  
> AI 파트너: No problem. Drive safely.

#### `ep06_c04` 워밍업 · 전화 첫마디 — 주행(drive), 약 44초

- **내레이터(KO)**: 전화 영어에서 꼭 필요한 첫마디 두 개를 챙기고 본편으로 넘어가겠습니다. `⏸ 1초`
- **내레이터(KO)**: 전화로 내가 누구인지 밝힐 때는 아이 엠 대신 잇츠를 씁니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, Grace. It's Minho. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 상대가 지금 통화할 수 있는지 물을 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Is now a good time? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 통화를 시작할 때 이 한마디를 붙이면 훨씬 배려 있게 들립니다. `⏸ 1초`
- **내레이터(KO)**: 워밍업은 여기까지입니다. 이제 민호 씨의 월요일 아침으로 들어가 보겠습니다. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *It's ~.*  
> AI 파트너: Hello? Who's calling, please?  
> 사용자(예시): Hi, it's Minho.  
> AI 파트너: Oh, hi Minho. What's up?

#### `ep06_c05` 본편 · 장면 1 전체 듣기: 팀장에게 전화 — 주행(drive), 약 41초

- **내레이터(KO)**: 장면 일입니다. 민호 씨가 팀장 그레이스에게 전화를 겁니다. 먼저 대화 전체를 편하게 들어 보세요. `⏸ 1초`
- **영어 음성(EN)**: Hi, Grace. It's Minho. Is now a good time? `⏸ 1초`
- **AI 파트너**: Sure, Minho. What's up? `⏸ 1초`
- **영어 음성(EN)**: I'm stuck in traffic. There's been an accident on the highway. `⏸ 1초`
- **AI 파트너**: Oh no. Are you okay? `⏸ 1초`
- **영어 음성(EN)**: I'm fine, but I'm running about fifteen minutes late. `⏸ 1초`
- **AI 파트너**: Thanks for letting me know. What time do you think you'll get here? `⏸ 1초`
- **영어 음성(EN)**: I should be there by nine forty-five. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm stuck in traffic.*  
> AI 파트너: You're not at your desk yet. Where are you?  
> 사용자(예시): I'm stuck in traffic.  
> AI 파트너: Oh, I see. Take your time.

#### `ep06_c06` 본편 · 장면 1 표현: 차가 막힐 때 — 주행(drive), 약 44초

- **내레이터(KO)**: 첫 문장부터 보겠습니다. 차가 막힌다고 할 때 영어로는 차들 사이에 갇혔다고 말합니다. `⏸ 1초`
- **영어 음성(EN)**: I'm stuck in traffic. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 스턱은 꽉 끼어서 움직이지 못한다는 뜻입니다. `⏸ 1초`
- **내레이터(KO)**: 이유를 덧붙일 때입니다. 고속도로에서 사고가 났다는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: There's been an accident on the highway. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 데어즈 빈은 방금 무슨 일이 생겼다고 알릴 때 자주 쓰는 틀입니다. `⏸ 1초`
- **영어 음성(EN)**: There's been a change of plans. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *There's been ~.*  
> AI 파트너: Why is the traffic so slow today?  
> 사용자(예시): There's been an accident on the highway.  
> AI 파트너: That explains it. I hope it clears up soon.

#### `ep06_c07` 본편 · 핵심 표현 1: 늦는다고 알리기 — 주행(drive), 약 45초

- **내레이터(KO)**: 첫 번째 핵심 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 늦는다고 할 때 아임 레이트도 맞지만, 원어민은 러닝 레이트를 더 자주 씁니다. 지금 늦어지고 있는 중이라는 느낌입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm running late. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 얼마나 늦는지 시간을 넣으면 상대가 계획을 세우기 쉬워집니다. 어바웃은 대략이라는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm running about fifteen minutes late. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 조금만 늦을 때는 어 리틀 비트를 넣습니다. `⏸ 1초`
- **영어 음성(EN)**: I'm running a little bit late. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm running about ~ late.*  
> AI 파트너: Hi, it's Grace. Are you on your way?  
> 사용자(예시): Yes, but I'm running about fifteen minutes late.  
> AI 파트너: Okay, thanks for letting me know.

#### `ep06_c08` 본편 · 핵심 표현 1 응용: 도착 시간 말하기 — 주행(drive), 약 48초

- **내레이터(KO)**: 늦는다고 말했다면 언제 도착할지도 알려 주는 게 예의입니다. `⏸ 1초`
- **내레이터(KO)**: 아이 슈드 비 데어 바이는, 그때까지는 도착할 것 같다는 뜻입니다. 바이는 늦어도 그 시각까지라는 말입니다. `⏸ 1초`
- **영어 음성(EN)**: I should be there by nine forty-five. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 시각 대신 몇 분 뒤라고 말할 수도 있습니다. `⏸ 1초`
- **영어 음성(EN)**: I'll be there in about twenty minutes. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 바이 뒤에는 몇 시, 인 뒤에는 몇 분이 옵니다. 이 차이만 기억하시면 됩니다. `⏸ 1초`
- **영어 음성(EN)**: I'll be there by ten. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I should be there by ~.*  
> AI 파트너: What time do you think you'll get here?  
> 사용자(예시): I should be there by nine forty-five.  
> AI 파트너: Great. See you then.

#### `ep06_c09` 본편 · 장면 1 응용: 상대의 반응 알아듣기 — 주행(drive), 약 49초

- **내레이터(KO)**: 이번에는 상대방의 대답을 알아듣는 연습입니다. 늦는다고 알리면 이런 대답이 돌아옵니다. `⏸ 1초`
- **AI 파트너**: Thanks for letting me know. `⏸ 1초`
- **내레이터(KO)**: 미리 알려 줘서 고맙다는 뜻입니다. 내가 소식을 전해 들었을 때도 그대로 쓸 수 있습니다. `⏸ 1초`
- **영어 음성(EN)**: Thanks for letting me know. [따라 말하기] `⏸ 3초`
- **AI 파트너**: No worries. Drive safely. `⏸ 1초`
- **내레이터(KO)**: 걱정 말고 운전 조심하라는 따뜻한 인사입니다. `⏸ 1초`
- **AI 파트너**: Take your time. We'll wait for you. `⏸ 1초`
- **내레이터(KO)**: 서두르지 말라는 뜻입니다. 이런 말을 들으면 이렇게 답하면 됩니다. `⏸ 1초`
- **영어 음성(EN)**: Thanks. I appreciate it. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Thanks for letting me know.*  
> AI 파트너: Just so you know, the meeting room has changed to room three.  
> 사용자(예시): Thanks for letting me know.  
> AI 파트너: You're welcome. See you there.

#### `ep06_c10` 본편 · 장면 1 다시 듣기: 따라 말하기 — 주행(drive), 약 48초

- **내레이터(KO)**: 장면 일을 다시 들어 보겠습니다. 이번에는 민호 씨의 문장 뒤에 따라 할 틈을 두겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, Grace. It's Minho. Is now a good time? [따라 말하기] `⏸ 3초`
- **AI 파트너**: Sure, Minho. What's up? `⏸ 1초`
- **영어 음성(EN)**: I'm stuck in traffic. There's been an accident on the highway. [따라 말하기] `⏸ 4초`
- **AI 파트너**: Oh no. Are you okay? `⏸ 1초`
- **영어 음성(EN)**: I'm fine, but I'm running about fifteen minutes late. [따라 말하기] `⏸ 4초`
- **AI 파트너**: What time do you think you'll get here? `⏸ 1초`
- **영어 음성(EN)**: I should be there by nine forty-five. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm running about ~ late.*  
> AI 파트너: Hi, Minho. Is everything okay?  
> 사용자(예시): I'm stuck in traffic, and I'm running about fifteen minutes late.  
> AI 파트너: Okay. Thanks for the heads-up.

#### `ep06_c11` 본편 · 발음 쉼표: 러닝 레이트와 피프틴 — 주행(drive), 약 43초

- **내레이터(KO)**: 잠깐 발음을 짚고 가겠습니다. `⏸ 1초`
- **내레이터(KO)**: 러닝 레이트는 두 단어를 끊지 않고 한 덩어리로 이어 말합니다. `⏸ 1초`
- **영어 음성(EN)**: running late, running late [따라 말하기] `⏸ 2초`
- **내레이터(KO)**: 피프틴과 피프티는 헷갈리기 쉽습니다. 십오는 뒤쪽 틴에 힘을 주고 길게, 오십은 앞쪽 피프에 힘을 줍니다. `⏸ 1초`
- **영어 음성(EN)**: fifteen, fifty [따라 말하기] `⏸ 2초`
- **내레이터(KO)**: 숫자를 잘못 들으면 십오 분 늦는 게 오십 분 늦는 걸로 바뀔 수 있습니다. 이제 문장으로 말해 보겠습니다. `⏸ 1초`
- **영어 음성(EN)**: I'm running about fifteen minutes late. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm running about ~ late.*  
> AI 파트너: Sorry, did you say fifteen or fifty minutes?  
> 사용자(예시): Fifteen. I'm running about fifteen minutes late.  
> AI 파트너: Got it, fifteen. That's not too bad.

#### `ep06_c12` 본편 · 장면 2 전체 듣기: 동료에게 부탁하기 — 주행(drive), 약 46초

- **내레이터(KO)**: 장면 이입니다. 열 시 고객 미팅을 준비해야 하는데, 민호 씨는 아직 길 위에 있습니다. 그래서 옆자리 동료 톰에게 전화를 겁니다. `⏸ 1초`
- **영어 음성(EN)**: Hey, Tom. It's Minho. Sorry to bother you. `⏸ 1초`
- **AI 파트너**: No problem. What's going on? `⏸ 1초`
- **영어 음성(EN)**: I'm running late, and the client is coming at ten. Could you cover for me until I get there? `⏸ 1초`
- **AI 파트너**: Sure. What do you need me to do? `⏸ 1초`
- **영어 음성(EN)**: Could you set up the meeting room and greet them for me? `⏸ 1초`
- **AI 파트너**: Of course. Don't worry about it. `⏸ 1초`
- **영어 음성(EN)**: Thanks, Tom. I owe you one. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you cover for me until ~?*  
> AI 파트너: Hey, it's Tom. Do you need any help?  
> 사용자(예시): Could you cover for me until I get there?  
> AI 파트너: Sure. I've got it covered.

#### `ep06_c13` 본편 · 장면 2 표현: 부탁 앞에 붙이는 한마디 — 주행(drive), 약 44초

- **내레이터(KO)**: 부탁하기 전에 먼저 양해를 구하는 말부터 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 쏘리 투 바더 유는, 귀찮게 해서 미안하다는 뜻입니다. 바쁜 사람에게 연락할 때 첫마디로 좋습니다. `⏸ 1초`
- **영어 음성(EN)**: Sorry to bother you. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 갑자기 부탁할 때는 이렇게 말합니다. 쇼트 노티스는 촉박한 연락이라는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Sorry for the short notice. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 상대가 무슨 일이냐고 물을 때는 이런 문장이 들립니다. `⏸ 1초`
- **AI 파트너**: What's going on? `⏸ 1초`
- **AI 파트너**: What's up? `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Sorry to bother you.*  
> AI 파트너: Hello? This is Tom speaking.  
> 사용자(예시): Hi, Tom. Sorry to bother you.  
> AI 파트너: No problem at all. What's up?

#### `ep06_c14` 본편 · 핵심 표현 2: 대신 맡아 달라고 부탁하기 — 주행(drive), 약 50초

- **내레이터(KO)**: 두 번째 핵심 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 커버 포 미는, 내 자리를 잠시 대신 맡아 달라는 뜻입니다. 경기에서 동료의 빈자리를 커버한다는 말과 같은 느낌입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you cover for me? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 언제까지인지 붙이면 부탁이 더 분명해집니다. 언틸 아이 겟 데어는, 내가 도착할 때까지라는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you cover for me until I get there? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 회의 앞부분만 부탁할 때는 이렇게 바꿉니다. `⏸ 1초`
- **영어 음성(EN)**: Could you cover for me for the first ten minutes? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you cover for me ~?*  
> AI 파트너: The client is here, but you're not. What should I do?  
> 사용자(예시): Could you cover for me until I get there?  
> AI 파트너: Sure. I'll take care of it.

#### `ep06_c15` 본편 · 핵심 표현 2 응용: 구체적으로 부탁하기 — 주행(drive), 약 47초

- **내레이터(KO)**: 커버해 달라고 한 뒤에는 무엇을 해 주면 좋을지 구체적으로 말해야 합니다. `⏸ 1초`
- **내레이터(KO)**: 회의실 준비를 부탁할 때입니다. 셋 업은 미리 준비해 둔다는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you set up the meeting room? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 고객을 나 대신 맞아 달라고 할 때입니다. 끝에 포 미를 붙이면 나 대신이라는 뜻이 됩니다. `⏸ 1초`
- **영어 음성(EN)**: Could you greet them for me? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 내가 오는 중이라고 전해 달라고 할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you let them know I'm on my way? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you let them know ~?*  
> AI 파트너: The client just arrived. Anything you want me to tell them?  
> 사용자(예시): Could you let them know I'm on my way?  
> AI 파트너: Sure. I'll tell them right now.

#### `ep06_c16` 본편 · 장면 2 응용: 고마움 표현하기 — 주행(drive), 약 42초

- **내레이터(KO)**: 부탁을 들어준 동료에게는 고마움을 꼭 전합니다. `⏸ 1초`
- **내레이터(KO)**: 아이 오 유 원은, 신세 한 번 졌다, 다음에 꼭 갚겠다는 뜻입니다. 동료 사이에서 아주 자주 씁니다. `⏸ 1초`
- **영어 음성(EN)**: Thanks. I owe you one. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 조금 더 정중하게 말하고 싶다면 이렇게 말합니다. `⏸ 1초`
- **영어 음성(EN)**: I really appreciate your help. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 동료는 보통 이렇게 답합니다. 괜찮으니 신경 쓰지 말라는 뜻입니다. `⏸ 1초`
- **AI 파트너**: Don't worry about it. `⏸ 1초`
- **AI 파트너**: Anytime. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I owe you one.*  
> AI 파트너: Don't worry. I already set up the meeting room for you.  
> 사용자(예시): Thanks. I owe you one.  
> AI 파트너: Anytime. Just get here safely.

#### `ep06_c17` 본편 · 장면 2 다시 듣기: 따라 말하기 — 주행(drive), 약 49초

- **내레이터(KO)**: 장면 이를 다시 들어 보겠습니다. 민호 씨의 문장 뒤에 따라 할 틈을 두겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Hey, Tom. It's Minho. Sorry to bother you. [따라 말하기] `⏸ 3초`
- **AI 파트너**: No problem. What's going on? `⏸ 1초`
- **영어 음성(EN)**: I'm running late. Could you cover for me until I get there? [따라 말하기] `⏸ 4초`
- **AI 파트너**: Sure. What do you need me to do? `⏸ 1초`
- **영어 음성(EN)**: Could you set up the meeting room and let the client know I'm on my way? [따라 말하기] `⏸ 4초`
- **AI 파트너**: Of course. Don't worry about it. `⏸ 1초`
- **영어 음성(EN)**: Thanks, Tom. I owe you one. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could you cover for me until ~?*  
> AI 파트너: Hi, Minho. You sound like you're driving. What's up?  
> 사용자(예시): I'm running late. Could you cover for me until I get there?  
> AI 파트너: No problem. I've got you covered.

#### `ep06_c18` 본편 · 중간 점검: 지금까지 나온 문장 — 주행(drive), 약 46초

- **내레이터(KO)**: 본편 앞부분을 마쳤습니다. 지금까지 나온 문장을 짧게 되짚어 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 차가 막힌다고 할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm stuck in traffic. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 늦는다고 알릴 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm running about fifteen minutes late. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 도착 시간을 말할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I should be there by nine forty-five. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 동료에게 대신 맡아 달라고 할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Could you cover for me until I get there? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I should be there by ~.*  
> AI 파트너: Are you almost here?  
> 사용자(예시): Almost. I should be there by nine forty-five.  
> AI 파트너: Perfect. See you soon.

#### `ep06_c19` 본편 · 장면 3 전체 듣기: 고객에게 시간 변경 요청 — 주행(drive), 약 45초

- **내레이터(KO)**: 장면 삼입니다. 사고 처리가 길어지면서 열 시까지 도착하기 어려워졌습니다. 민호 씨가 고객 사라에게 직접 전화를 겁니다. `⏸ 1초`
- **영어 음성(EN)**: Good morning, Sarah. This is Minho from the design team. `⏸ 1초`
- **AI 파트너**: Good morning, Minho. `⏸ 1초`
- **영어 음성(EN)**: I'm sorry for the short notice, but I'm stuck in traffic. Could we push the meeting back to eleven? `⏸ 1초`
- **AI 파트너**: Eleven? Let me check my schedule. `⏸ 1초`
- **영어 음성(EN)**: Of course. If eleven doesn't work, I'm also free after two. `⏸ 1초`
- **AI 파트너**: Eleven works for me. `⏸ 1초`
- **영어 음성(EN)**: Thank you so much. I'll see you at eleven. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could we push the meeting back to ~?*  
> AI 파트너: Hi, Minho. Are we still on for ten o'clock?  
> 사용자(예시): Could we push the meeting back to eleven?  
> AI 파트너: Sure. Eleven works for me.

#### `ep06_c20` 본편 · 장면 3 표현: 고객에게 전화할 때 첫마디 — 주행(drive), 약 47초

- **내레이터(KO)**: 고객에게 전화할 때는 첫마디가 조금 더 격식 있습니다. `⏸ 1초`
- **내레이터(KO)**: 동료에게는 잇츠 민호라고 했지만, 고객에게는 디스 이즈 민호라고 하고 소속을 붙입니다. `⏸ 1초`
- **영어 음성(EN)**: This is Minho from the design team. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 왜 전화했는지 바로 밝힐 때는 이렇게 말합니다. `⏸ 1초`
- **영어 음성(EN)**: I'm calling about our meeting this morning. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 아임 콜링 어바웃은, 그 건으로 전화드렸다는 뜻입니다. 뒤에 용건만 바꾸면 됩니다. `⏸ 1초`
- **영어 음성(EN)**: I'm calling about the new design. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm calling about ~.*  
> AI 파트너: Good morning. How can I help you?  
> 사용자(예시): Hi, this is Minho. I'm calling about our meeting this morning.  
> AI 파트너: Oh, yes. What about it?

#### `ep06_c21` 본편 · 핵심 표현 3: 회의 시간 미루기 — 주행(drive), 약 47초

- **내레이터(KO)**: 세 번째 핵심 표현입니다. `⏸ 1초`
- **내레이터(KO)**: 푸시 백은 뒤로 민다는 뜻으로, 일정을 늦출 때 씁니다. 쿠드 위로 시작하면, 우리 이렇게 해도 될까요, 라는 부드러운 제안이 됩니다. `⏸ 1초`
- **영어 음성(EN)**: Could we push the meeting back? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 몇 시로 미룰지 넣어 보겠습니다. 투 다음에 시각을 붙입니다. `⏸ 1초`
- **영어 음성(EN)**: Could we push the meeting back to eleven? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 시각 대신 얼마나 미룰지 말할 때는 바이를 씁니다. `⏸ 1초`
- **영어 음성(EN)**: Could we push it back by thirty minutes? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could we push the meeting back to ~?*  
> AI 파트너: So, are we still meeting at ten?  
> 사용자(예시): Could we push the meeting back to eleven?  
> AI 파트너: Let me check. Yes, that's fine.

#### `ep06_c22` 본편 · 핵심 표현 3 응용: 앞당기기와 다른 날로 옮기기 — 주행(drive), 약 44초

- **내레이터(KO)**: 반대로 일정을 앞당길 때는 무브 업을 씁니다. `⏸ 1초`
- **영어 음성(EN)**: Could we move the meeting up to nine? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 아예 다른 날로 옮길 때는 리스케줄이라고 합니다. `⏸ 1초`
- **영어 음성(EN)**: Could we reschedule for tomorrow? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 정리하면, 뒤로 미룰 때는 푸시 백, 앞당길 때는 무브 업, 다른 날로 옮길 때는 리스케줄입니다. `⏸ 1초`
- **내레이터(KO)**: 오늘 아침처럼 늦을 때 가장 많이 쓰는 건 역시 푸시 백입니다. `⏸ 1초`
- **영어 음성(EN)**: Could we push it back to eleven? [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could we reschedule for ~?*  
> AI 파트너: I'm afraid today is really busy for me.  
> 사용자(예시): Could we reschedule for tomorrow?  
> AI 파트너: Tomorrow works. Let's say ten.

#### `ep06_c23` 본편 · 장면 3 응용: 사과 한마디 붙이기 — 주행(drive), 약 44초

- **내레이터(KO)**: 일정을 바꿀 때는 사과를 먼저 하면 훨씬 부드럽게 들립니다. `⏸ 1초`
- **영어 음성(EN)**: I'm sorry for the short notice. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 불편을 드려 죄송하다고 말할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I'm sorry for the inconvenience. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 사과와 부탁을 한 문장으로 이을 때는 사이에 벗을 넣습니다. `⏸ 1초`
- **영어 음성(EN)**: I'm sorry for the short notice, but could we push the meeting back to eleven? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 사과로 시작하고 부탁으로 끝나는 이 순서를 기억해 두세요. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm sorry for the short notice, but ~*  
> AI 파트너: Hello, Minho. Is something wrong?  
> 사용자(예시): I'm sorry for the short notice, but could we push the meeting back to eleven?  
> AI 파트너: That's okay. Eleven is fine.

#### `ep06_c24` 본편 · 장면 3 응용: 대안 시간 제안하기 — 주행(drive), 약 44초

- **내레이터(KO)**: 상대가 그 시간에 안 될 수도 있습니다. 그럴 때를 대비해 대안을 하나 더 준비해 둡니다. `⏸ 1초`
- **영어 음성(EN)**: If eleven doesn't work, I'm also free after two. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 상대에게 시간을 고르게 할 때는 이렇게 묻습니다. `⏸ 1초`
- **영어 음성(EN)**: What time works best for you? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 내가 제안한 시간이 괜찮은지 확인할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: Does eleven work for you? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 여기서 워크는 일하다가 아니라, 괜찮다, 맞는다는 뜻입니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Does ~ work for you?*  
> AI 파트너: I can meet later today. What time were you thinking?  
> 사용자(예시): Does eleven work for you?  
> AI 파트너: Eleven works for me.

#### `ep06_c25` 본편 · 장면 3 응용: 상대의 대답 알아듣기 — 주행(drive), 약 45초

- **내레이터(KO)**: 시간 변경을 요청하면 이런 대답들이 돌아올 수 있습니다. 차례로 들어 보겠습니다. `⏸ 1초`
- **AI 파트너**: Eleven works for me. `⏸ 1초`
- **내레이터(KO)**: 열한 시 괜찮다는 뜻입니다. `⏸ 1초`
- **AI 파트너**: Let me check my schedule. `⏸ 1초`
- **내레이터(KO)**: 일정을 확인해 보겠다는 뜻입니다. 잠시 기다리면 됩니다. `⏸ 1초`
- **AI 파트너**: I'm afraid I have another meeting at eleven. `⏸ 1초`
- **내레이터(KO)**: 아임 어프레이드는 유감이지만이라는 뜻으로, 거절을 부드럽게 할 때 씁니다. 이럴 땐 준비해 둔 대안을 꺼내면 됩니다. `⏸ 1초`
- **영어 음성(EN)**: No problem. How about two o'clock? [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *How about ~?*  
> AI 파트너: I'm afraid I have another meeting at eleven.  
> 사용자(예시): No problem. How about two o'clock?  
> AI 파트너: Two o'clock is perfect.

#### `ep06_c26` 본편 · 장면 3 다시 듣기: 따라 말하기 — 주행(drive), 약 45초

- **내레이터(KO)**: 장면 삼을 다시 들어 보겠습니다. 민호 씨의 문장 뒤에 따라 할 틈을 두겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Good morning, Sarah. This is Minho from the design team. [따라 말하기] `⏸ 4초`
- **AI 파트너**: Good morning, Minho. `⏸ 1초`
- **영어 음성(EN)**: I'm sorry for the short notice, but I'm stuck in traffic. [따라 말하기] `⏸ 4초`
- **영어 음성(EN)**: Could we push the meeting back to eleven? [따라 말하기] `⏸ 4초`
- **AI 파트너**: Let me check my schedule. Eleven works for me. `⏸ 1초`
- **영어 음성(EN)**: Thank you so much. I'll see you at eleven. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could we push the meeting back to ~?*  
> AI 파트너: Good morning, Minho. What can I do for you?  
> 사용자(예시): I'm sorry for the short notice, but could we push the meeting back to eleven?  
> AI 파트너: No problem. See you at eleven.

#### `ep06_c27` 본편 · 장면 4 전체 듣기: 음성 메시지 남기기 — 주행(drive), 약 42초

- **내레이터(KO)**: 장면 사입니다. 회의 시간이 바뀌었다고 팀장에게 알려야 하는데, 이번에는 전화를 받지 않습니다. 민호 씨가 음성 메시지를 남깁니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, Grace. It's Minho again. `⏸ 1초`
- **영어 음성(EN)**: Just a quick update. The client meeting has been moved to eleven. `⏸ 1초`
- **영어 음성(EN)**: Tom is setting up the room, so everything is on track. `⏸ 1초`
- **영어 음성(EN)**: I'll keep you posted. Please call me back when you get a chance. Thanks. `⏸ 2초`
- **내레이터(KO)**: 짧지만 누가, 무슨 일로, 앞으로 어떻게 할지가 모두 들어 있는 메시지입니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Just a quick update.*  
> AI 파트너: Hi, Minho. I missed your call. What's going on?  
> 사용자(예시): Just a quick update. The meeting has been moved to eleven.  
> AI 파트너: Got it. Thanks for the update.

#### `ep06_c28` 본편 · 장면 4 표현: 메시지 첫머리와 용건 — 주행(drive), 약 42초

- **내레이터(KO)**: 음성 메시지는 누구인지, 무슨 일인지를 먼저 말합니다. `⏸ 1초`
- **내레이터(KO)**: 같은 사람에게 또 연락할 때는 끝에 어게인을 붙입니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, it's Minho again. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 저스트 어 퀵 업데이트는, 짧게 소식 하나 전한다는 뜻입니다. 메시지를 시작할 때 아주 자연스럽습니다. `⏸ 1초`
- **영어 음성(EN)**: Just a quick update. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 회의가 옮겨졌다고 알릴 때입니다. `⏸ 1초`
- **영어 음성(EN)**: The meeting has been moved to eleven. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *The meeting has been moved to ~.*  
> AI 파트너: Is the meeting still at ten?  
> 사용자(예시): No. The meeting has been moved to eleven.  
> AI 파트너: Okay. I'll update my calendar.

#### `ep06_c29` 본편 · 장면 4 표현: 진행 상황 알리기 — 주행(drive), 약 45초

- **내레이터(KO)**: 일이 잘 굴러가고 있다고 안심시키는 표현입니다. 온 트랙은 계획대로 진행 중이라는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Everything is on track. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 계속 소식을 전하겠다는 약속입니다. 킵 유 포스티드는 진행 상황을 계속 알려 주겠다는 말입니다. `⏸ 1초`
- **영어 음성(EN)**: I'll keep you posted. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 시간 날 때 다시 전화해 달라고 부탁할 때입니다. 웬 유 겟 어 챈스는 시간이 될 때라는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Please call me back when you get a chance. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'll keep you posted.*  
> AI 파트너: Let me know if anything changes, okay?  
> 사용자(예시): Sure. I'll keep you posted.  
> AI 파트너: Great, thanks.

#### `ep06_c30` 본편 · 장면 4 응용: 팀장의 답 전화 — 주행(drive), 약 51초

- **내레이터(KO)**: 잠시 뒤 팀장 그레이스가 다시 전화를 걸어옵니다. `⏸ 1초`
- **AI 파트너**: Hi, Minho. I got your message. Thanks for the update. `⏸ 1초`
- **영어 음성(EN)**: No problem. Sorry about all the confusion this morning. [따라 말하기] `⏸ 4초`
- **AI 파트너**: Don't worry. These things happen. `⏸ 1초`
- **내레이터(KO)**: 디즈 띵즈 해픈은, 그럴 수도 있지, 라는 위로의 말입니다. `⏸ 1초`
- **내레이터(KO)**: 민호 씨의 사과 문장을 한 번 더 들어 보겠습니다. 컨퓨전은 혼란이라는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Sorry about all the confusion. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 메시지를 받은 쪽에서는 업데이트 고맙다고 답하면 됩니다. `⏸ 1초`
- **영어 음성(EN)**: Thanks for the update. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Thanks for the update.*  
> AI 파트너: Hi, it's Minho. The meeting is now at eleven.  
> 사용자(예시): Got it. Thanks for the update.  
> AI 파트너: You're welcome. See you soon.

#### `ep06_c31` 본편 · 장면 4 다시 듣기: 메시지 통째로 따라 하기 — 주행(drive), 약 44초

- **내레이터(KO)**: 장면 사의 음성 메시지를 문장별로 따라 해 보겠습니다. 실제로 메시지를 남긴다고 생각하고 말해 보세요. `⏸ 1초`
- **영어 음성(EN)**: Hi, Grace. It's Minho again. [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: Just a quick update. [따라 말하기] `⏸ 2초`
- **영어 음성(EN)**: The client meeting has been moved to eleven. [따라 말하기] `⏸ 3초`
- **영어 음성(EN)**: Tom is setting up the room, so everything is on track. [따라 말하기] `⏸ 4초`
- **영어 음성(EN)**: I'll keep you posted. [따라 말하기] `⏸ 2초`
- **영어 음성(EN)**: Please call me back when you get a chance. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Please call me back when you get a chance.*  
> AI 파트너: Sorry, I can't talk right now. I'm in a meeting.  
> 사용자(예시): No problem. Please call me back when you get a chance.  
> AI 파트너: Will do. Talk to you later.

#### `ep06_c32` 본편 · 장면 5 전체 듣기: 도착해서 회의 시작 — 주행(drive), 약 41초

- **내레이터(KO)**: 장면 오입니다. 드디어 사무실에 도착했습니다. 열한 시, 민호 씨가 고객 사라를 맞이합니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, Sarah. Sorry to keep you waiting. `⏸ 1초`
- **AI 파트너**: That's all right. The traffic was terrible this morning. `⏸ 1초`
- **영어 음성(EN)**: It really was. Thank you for being so flexible. `⏸ 1초`
- **AI 파트너**: Not at all. So, shall we get started? `⏸ 1초`
- **영어 음성(EN)**: Yes, let's get started. First, I'd like to show you the new design. `⏸ 2초`
- **내레이터(KO)**: 사과와 감사로 시작해서 바로 본론으로 넘어가는 흐름이 깔끔합니다. `⏸ 1초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Sorry to keep you waiting.*  
> AI 파트너: Hi, Minho. Nice to see you.  
> 사용자(예시): Hi, Sarah. Sorry to keep you waiting.  
> AI 파트너: That's all right. Traffic happens.

#### `ep06_c33` 본편 · 장면 5 표현: 기다려 준 사람에게 — 주행(drive), 약 42초

- **내레이터(KO)**: 기다리게 해서 미안하다고 할 때입니다. 킵 유 웨이팅은, 상대를 계속 기다리게 했다는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Sorry to keep you waiting. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 사과 대신 감사로 시작할 수도 있습니다. 듣는 사람에게는 이쪽이 더 긍정적으로 들리기도 합니다. `⏸ 1초`
- **영어 음성(EN)**: Thank you for waiting. [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 일정을 바꿔 준 데 고마움을 전할 때입니다. 플렉서블은 융통성 있다는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Thank you for being so flexible. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Thank you for being so flexible.*  
> AI 파트너: It's okay. I was happy to move the meeting to eleven.  
> 사용자(예시): Thank you for being so flexible.  
> AI 파트너: My pleasure. Let's get started.

#### `ep06_c34` 본편 · 장면 5 표현: 본론으로 넘어가기 — 주행(drive), 약 50초

- **내레이터(KO)**: 인사가 끝나면 회의를 시작합니다. 렛츠 겟 스타티드는, 시작하자는 뜻입니다. `⏸ 1초`
- **영어 음성(EN)**: Let's get started. [따라 말하기] `⏸ 2초`
- **내레이터(KO)**: 셸 위로 물으면 조금 더 정중한 제안이 됩니다. `⏸ 1초`
- **영어 음성(EN)**: Shall we get started? [따라 말하기] `⏸ 3초`
- **내레이터(KO)**: 처음에 무엇을 할지 소개할 때는 아이드 라이크 투를 씁니다. `⏸ 1초`
- **영어 음성(EN)**: First, I'd like to show you the new design. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 아이드 라이크 투 다음에 동사만 바꾸면 회의 진행 문장을 얼마든지 만들 수 있습니다. `⏸ 1초`
- **영어 음성(EN)**: First, I'd like to go over today's schedule. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'd like to ~.*  
> AI 파트너: So, what's first on the agenda?  
> 사용자(예시): First, I'd like to show you the new design.  
> AI 파트너: Great. I'm looking forward to it.

#### `ep06_c35` 본편 · 장면 5 응용: 회의 마무리하기 — 주행(drive), 약 43초

- **내레이터(KO)**: 회의가 끝날 무렵 다음 단계를 약속하는 문장도 알아 두면 좋습니다. `⏸ 1초`
- **내레이터(KO)**: 오늘 오후까지 바뀐 일정을 보내겠다고 할 때입니다. `⏸ 1초`
- **영어 음성(EN)**: I'll send you the updated schedule by this afternoon. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 질문이 있으면 언제든 연락하라는 마무리입니다. `⏸ 1초`
- **영어 음성(EN)**: Please let me know if you have any questions. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 오늘 시간 내 줘서 고맙다는 인사입니다. `⏸ 1초`
- **영어 음성(EN)**: Thanks again for your time today. [따라 말하기] `⏸ 3초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'll send you ~ by ~.*  
> AI 파트너: When can I see the updated schedule?  
> 사용자(예시): I'll send you the updated schedule by this afternoon.  
> AI 파트너: Perfect. Thank you.

#### `ep06_c36` 본편 · 장면 5 다시 듣기: 따라 말하기 — 주행(drive), 약 39초

- **내레이터(KO)**: 장면 오를 다시 들어 보겠습니다. 민호 씨의 문장 뒤에 따라 할 틈을 두겠습니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, Sarah. Sorry to keep you waiting. [따라 말하기] `⏸ 3초`
- **AI 파트너**: That's all right. The traffic was terrible this morning. `⏸ 1초`
- **영어 음성(EN)**: It really was. Thank you for being so flexible. [따라 말하기] `⏸ 4초`
- **AI 파트너**: Not at all. So, shall we get started? `⏸ 1초`
- **영어 음성(EN)**: Yes, let's get started. First, I'd like to show you the new design. [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Sorry to keep you waiting.*  
> AI 파트너: Oh, there you are, Minho.  
> 사용자(예시): Hi, Sarah. Sorry to keep you waiting.  
> AI 파트너: No problem. It's good to see you.

#### `ep06_c37` 본편 · 이어 듣기 1: 아침 통화 — 주행(drive), 약 45초

- **내레이터(KO)**: 이제 민호 씨의 아침을 처음부터 이어서 들어 보겠습니다. 이번에는 따라 하지 않고 흐름만 들으셔도 됩니다. 먼저 아침 통화들입니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, Grace. It's Minho. I'm stuck in traffic, and I'm running about fifteen minutes late. `⏸ 1초`
- **AI 파트너**: Thanks for letting me know. Drive safely. `⏸ 1초`
- **영어 음성(EN)**: Hey, Tom. Sorry to bother you. Could you cover for me until I get there? `⏸ 1초`
- **AI 파트너**: Sure. Don't worry about it. `⏸ 1초`
- **영어 음성(EN)**: Good morning, Sarah. I'm sorry for the short notice, but could we push the meeting back to eleven? `⏸ 1초`
- **AI 파트너**: Eleven works for me. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *Could we push the meeting back to ~?*  
> AI 파트너: Good morning. Are you still coming at ten?  
> 사용자(예시): I'm sorry, but could we push the meeting back to eleven?  
> AI 파트너: Sure. See you at eleven.

#### `ep06_c38` 본편 · 이어 듣기 2: 메시지와 회의 — 주행(drive), 약 42초

- **내레이터(KO)**: 이어서 음성 메시지와 회의 장면입니다. `⏸ 1초`
- **영어 음성(EN)**: Hi, Grace. It's Minho again. Just a quick update. The meeting has been moved to eleven. I'll keep you posted. `⏸ 1초`
- **영어 음성(EN)**: Hi, Sarah. Sorry to keep you waiting. Thank you for being so flexible. `⏸ 1초`
- **AI 파트너**: Not at all. Shall we get started? `⏸ 1초`
- **영어 음성(EN)**: Yes. First, I'd like to show you the new design. `⏸ 2초`
- **내레이터(KO)**: 본편은 여기까지입니다. 민호 씨의 아침이 꽤 바빴습니다. 이제 복습으로 오늘의 표현을 단단히 묶어 보겠습니다. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'll keep you posted.*  
> AI 파트너: Please keep me updated on the meeting, okay?  
> 사용자(예시): Sure. I'll keep you posted.  
> AI 파트너: Thanks, Minho.

#### `ep06_c39` 복습 · 핵심 표현 세 개 다시 듣기 — 주행(drive), 약 40초

- **내레이터(KO)**: 복습 시간입니다. 오늘의 핵심 표현 세 개를 다시 들어 보겠습니다. `⏸ 1초`
- **내레이터(KO)**: 첫째, 늦는다고 알리기. `⏸ 1초`
- **영어 음성(EN)**: I'm running about fifteen minutes late. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 둘째, 도착할 때까지 대신 맡아 달라고 부탁하기. `⏸ 1초`
- **영어 음성(EN)**: Could you cover for me until I get there? [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 셋째, 회의 시간을 뒤로 미루기. `⏸ 1초`
- **영어 음성(EN)**: Could we push the meeting back to eleven? [따라 말하기] `⏸ 4초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm running about ~ late.*  
> AI 파트너: Where are you? Everyone is here.  
> 사용자(예시): Sorry, I'm running about ten minutes late.  
> AI 파트너: Okay. We'll get started without you.

#### `ep06_c40` 복습 · 퀴즈 1: 늦을 때 — 주행(drive), 약 54초

- **내레이터(KO)**: 이제 짧은 퀴즈입니다. 상황을 한국어로 들려 드리면, 잠깐 동안 영어 문장을 떠올려 보세요. 이어서 정답이 나옵니다. `⏸ 1초`
- **내레이터(KO)**: 첫 번째 상황입니다. 차가 막힌다고 말하는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: I'm stuck in traffic. `⏸ 1초`
- **내레이터(KO)**: 두 번째 상황입니다. 아홉 시 사십오 분까지 도착할 것 같다는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: I should be there by nine forty-five. `⏸ 1초`
- **내레이터(KO)**: 세 번째 상황입니다. 바쁜 동료에게 미안하다며 말을 꺼내는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: Sorry to bother you. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I should be there by ~.*  
> AI 파트너: When do you think you'll arrive?  
> 사용자(예시): I should be there by nine forty-five.  
> AI 파트너: Great. See you soon.

#### `ep06_c41` 복습 · 퀴즈 2: 부탁하고 일정 바꾸기 — 주행(drive), 약 52초

- **내레이터(KO)**: 네 번째 상황입니다. 동료에게, 내가 도착할 때까지 대신 맡아 달라고 부탁하는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: Could you cover for me until I get there? `⏸ 2초`
- **내레이터(KO)**: 다섯 번째 상황입니다. 고객에게, 회의를 열한 시로 미룰 수 있는지 묻는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: Could we push the meeting back to eleven? `⏸ 2초`
- **내레이터(KO)**: 여섯 번째 상황입니다. 촉박하게 연락드려 죄송하다고 말하는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: I'm sorry for the short notice. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm sorry for the short notice.*  
> AI 파트너: You want to change the meeting time again?  
> 사용자(예시): Yes, I'm sorry for the short notice.  
> AI 파트너: That's okay. Things happen.

#### `ep06_c42` 복습 · 퀴즈 3: 메시지와 회의 — 주행(drive), 약 46초

- **내레이터(KO)**: 일곱 번째 상황입니다. 진행 상황을 계속 알려 드리겠다고 말하는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: I'll keep you posted. `⏸ 2초`
- **내레이터(KO)**: 여덟 번째 상황입니다. 기다리게 해서 미안하다고 말하는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: Sorry to keep you waiting. `⏸ 2초`
- **내레이터(KO)**: 마지막 상황입니다. 도와준 동료에게, 신세 한 번 졌다고 말하는 문장을 떠올려 보세요. `⏸ 4초`
- **영어 음성(EN)**: Thanks. I owe you one. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I owe you one.*  
> AI 파트너: I finished the report for you.  
> 사용자(예시): Thanks. I owe you one.  
> AI 파트너: Anytime. Have a good day.

#### `ep06_c43` 복습 · 마무리 인사 — 주행(drive), 약 42초

- **내레이터(KO)**: 퀴즈까지 모두 마쳤습니다. 수고하셨습니다. `⏸ 1초`
- **내레이터(KO)**: 오늘은 늦는다고 알리고, 동료에게 부탁하고, 고객과 회의 시간을 다시 맞추는 민호 씨의 아침을 함께 보냈습니다. `⏸ 1초`
- **내레이터(KO)**: 오늘 표현 중 딱 하나만 가져가신다면 이 문장입니다. 출근길에 한 번 더 소리 내어 말해 보세요. `⏸ 1초`
- **영어 음성(EN)**: I'm running about fifteen minutes late. [따라 말하기] `⏸ 4초`
- **내레이터(KO)**: 물론 실제로는 늦지 않는 출근길이 제일 좋겠지요. 오늘도 안전 운전하세요. 다다였습니다. `⏸ 2초`

> **[신호대기 연습]** (최대 2턴 / 30초) — 목표 표현: *I'm running about ~ late.*  
> AI 파트너: Good morning. Are you on your way to the office?  
> 사용자(예시): Yes, but I'm running about fifteen minutes late.  
> AI 파트너: No problem. Drive safely.

### 주차·장시간 정차 롤플레이 (deferred_roleplays)

선형 재생 위치 없음. 기어 P 또는 60초 이상 정차 시에만 제안됩니다.

#### `ep06_rp01` 정차 롤플레이: 늦는다고 알리고 회의 미루기 — trigger: `parked_or_long_stop`, 최대 5분

- **내레이터(KO)**: 차를 세워 두셨을 때 열리는 대화 연습입니다. `⏸ 1초`
- **내레이터(KO)**: 저는 오늘 열 시에 만나기로 한 고객입니다. 늦는다고 알리고, 회의 시간을 뒤로 미뤄 보세요. `⏸ 1초`
- **내레이터(KO)**: 막히면 힌트, 라고 말씀하시면 됩니다. `⏸ 1초`
- **AI 파트너**: Hello, this is Sarah. Are we still on for ten o'clock? `⏸ 0초`

**AI 파트너 지시문 (system)**

```text
You are Sarah, a friendly but busy client at a fictional company (do not name any real company). Speak natural CEFR B1 English. Situation: you have a ten o'clock meeting with the learner, who is stuck in traffic on the way to the office. Ask where they are and when they will arrive. When they ask to move the meeting, say eleven is difficult at first and offer another time, so the learner has to suggest an alternative (for example two o'clock). Encourage 'I'm running about ~ late', 'Could we push the meeting back to ~?', 'I'm sorry for the short notice', 'Does ~ work for you?'. Recast errors naturally instead of correcting. If the learner says 'hint' or is silent for 8 seconds, give one Korean hint with an English phrase. Keep turns under 30 words. End once a new time is agreed or after 5 minutes, with a 2-sentence Korean feedback. Stop immediately if the app signals the vehicle is moving.
```

**샘플 대화**

- AI 파트너: Hello, this is Sarah. Are we still on for ten o'clock?
- 사용자(예시): I'm sorry, but I'm stuck in traffic. I'm running about thirty minutes late.
- AI 파트너: Oh, I see. That happens. What would you like to do?
- 사용자(예시): Could we push the meeting back to eleven?
- AI 파트너: I'm afraid I have another call at eleven. Is there another time that works for you?
- 사용자(예시): How about two o'clock? Does two work for you?
- AI 파트너: Two o'clock works for me. See you then, and drive safely.

---

총 6개 에피소드 · 주행 청크 88개 · 신호대기 연습 82개 · 주차 롤플레이 6개.
