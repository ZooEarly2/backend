# 쥬얼리 (ZooEarly) — AI API 명세서

> **v2.2.0 · 2026-08-27**
> React Native 앱 ↔ API Gateway ↔ FastAPI Inference Server(STT / LLM / TTS → OpenAI API)
> **이 문서가 기존 `zooearly-api-spec.md`(13개 엔드포인트)를 대체한다.** 시나리오·스토리·진행 상태는 전부 앱 로컬로 이동했고, 서버에 남는 것은 AI 추론뿐이다.

| 항목 | 값 |
|---|---|
| Base URL | `https://zooearly-gw.jollyhill-992be81c.koreacentral.azurecontainerapps.io/api/v1` · 로컬 `http://localhost:8080/api/v1` |
| 프로토콜 | HTTPS only |
| 인코딩 | UTF-8 |
| 요청/응답 | `application/json` (음성 업로드만 `multipart/form-data`) |
| 인증 | 없음 (프로토타입) |
| 엔드포인트 | **10개** — `/api/v1/ai` 7개(`chat`·`stt`·`tts`·`feedback`·`pronunciation`·`pronunciation/sentences`·`story`) + `/api/v1/albums` 3개(§9) |

---

## 변경 이력

| 버전 | 날짜 | 변경 | 앱 영향 |
|---|---|---|---|
| **2.0.0** | 2026-08-27 | **동화 앨범 신설**(`/api/v1/albums` 3개) — 게이트웨이가 MySQL 에 동화를 남긴다. 엔드포인트별 타임아웃 분리. `@Valid` 검증 실패도 `400 INVALID_PARAMETER` 로 통일 | ⚠️ **있음** — 앱이 기기마다 `childId`(UUIDv4)를 만들어 두고, 동화를 **화면에 띄운 뒤** 앨범에 올린다. 조회할 때도 `childId` 를 함께 보낸다 |
| **1.7.0** | 2026-08-24 | `story`(동화 생성) 추가 — 하루치 4장면을 동화로 엮는다 | ⚠️ **있음** — 앱이 등교·수업·점심·하교 기록을 **모아뒀다가** 한 번에 보내야 한다. 서버는 저장하지 않는다(무상태). 응답이 오래 걸려(최대 60초) 로딩 화면이 필요하다 |
| **2.2.0** | 2026-08-27 | `pronunciation/sentences` 에 `translationParts`(빈칸↔번역 조각 대응) 추가. 문장이 **10개 → 21개**(등교 9 · 수업 3 · 급식 3 · 하교 6). `departure_1` 문구를 "안녕히 가세요" → **"안녕히 계세요"** 로 바로잡음 | ⚠️ **있음** — 앱은 카테고리별로 걸러 **무작위 3개만** 띄운다(전부 띄우면 아홉 개 중에 고르라는 화면이 된다). 개수를 숫자로 못박은 곳이 있으면 지운다 |
| **2.1.0** | 2026-08-27 | `pronunciation/sentences` 응답에 `translations`(모국어 뜻) 추가 · `422 OFF_SCRIPT` 에러 코드 신설(§1.3) | 🟢 **없음(하위호환)** — 필드가 하나 붙기만 한다. 안 읽으면 예전과 똑같다. 새로 생기거나 사라진 엔드포인트는 **없고, 게이트웨이는 한 줄도 바뀌지 않는다**(§6-1 계약 7) |
| **1.6.0** | 2026-08-24 | `pronunciation/sentences`에 `study`(수업시간 시) 카테고리 추가. 9개 → **10개** | ⚠️ **있음** — 수업시간 "같이 읽어볼까요?"도 이제 이 목록의 `sentenceId`로 `/pronunciation`을 부를 수 있다. `study`는 3개가 아니라 1개다 |
| **1.5.0** | 2026-08-24 | `pronunciation/sentences` 신설 · `pronunciation`의 `targetSentence`→`sentenceId` 전환 · `quizSentence` 제거 · 잘함/못함 판정이 앱→FastAPI로 이동 | ⚠️ **있음** — "표현 고르기" 선택지가 앱 번들이 아니라 서버 목록이 된다. `/pronunciation` 요청 필드명이 바뀐다. 빈칸 문장은 앱이 직접 만들어야 한다 |
| **1.4.0** | 2026-08-22 | `pronunciation`(발음 채점) 추가 · 오디오 **최대 길이 60초 → 30초** | ⚠️ **있음** — 녹음을 30초에서 끊어야 한다. 발음 피드백 화면은 신규 구현 |
| **1.3.0** | 2026-08-21 | `tts`의 `language`를 **필수**로 전환 | ⚠️ **있음** — `/tts`를 부르는 **모든 곳**에 넣어야 한다. 피드백 화면뿐 아니라 `DIALOGUE` 🔊·`LISTEN` 스텝의 한국어 재생도 `"KOREAN"`을 명시한다. 누락 시 `400 INVALID_PARAMETER` |
| 1.2.0 | 2026-08-21 | `tts`에 `language` 선택 필드 추가 | — (1.3.0에서 필수로 바뀜) |
| **1.1.0** | 2026-08-21 | `chat` / `feedback`에 `nickname` **필수** 필드 추가 | ⚠️ **있음** — 앱이 온보딩에서 받은 닉네임을 매 요청에 보내야 한다. 누락 시 `400 INVALID_PARAMETER` |
| 1.0.0 | 2026-08-21 | 최초 작성. 엔드포인트 4개 | — |

> `nativeLanguage`는 1.0.0과 동일하게 **선택**이다 (생략 시 `KOREAN`). 바뀌지 않았다.

---

## 0. 아키텍처 계약

```
React Native App ──HTTPS/REST──▶ API Gateway ──HTTP──▶ FastAPI ──▶ OpenAI API
 (UI/시나리오/게임/로컬 상태)      (릴레이 전용)         (STT/LLM/TTS)
```

### 0.1 게이트웨이는 릴레이다

게이트웨이(Spring)가 하는 일은 딱 세 가지다.

1. **요청 검증** — 필수 파라미터, 오디오 포맷·용량. 잘못된 요청은 FastAPI까지 가지 않고 게이트웨이에서 `400`으로 끊는다.
2. **전달** — 검증을 통과한 요청을 FastAPI의 동일 엔드포인트로 그대로 넘기고, 응답을 그대로 되돌려준다. **body를 가공하지 않는다.**
3. **에러 통일** — FastAPI가 죽었거나 늦거나 5xx를 내면, 앱에는 항상 §1.3의 공통 에러 포맷으로 변환해서 내려준다. 앱은 FastAPI의 생(raw) 에러를 볼 일이 없다.

**하지 않는 일**: 중계 경로(`/api/v1/ai/*`)는 DB 저장·이력 관리·사용자 조회·비즈니스 로직을 하지 않는다.
게이트웨이가 스스로 데이터를 갖는 곳은 **동화 앨범(§9) 하나뿐**이다.

### 0.2 상태는 전부 앱에 있다

서버가 아무것도 기억하지 않으므로, **문맥이 필요한 요청은 앱이 문맥을 함께 보낸다.**

- `chat`의 대화 이력(`history`) — 앱이 로컬에 쌓아서 매 요청에 실어 보낸다
- `feedback`의 목표 문장(`targetSentence`) — 스텝 데이터가 앱 번들에 있으므로 앱이 보낸다
- 시나리오 컨텍스트(`scenario`) — LLM 프롬프트 구성용 힌트로 앱이 보낸다
- 아이 호칭(`nickname`) — 앱 온보딩에서 필수로 받는 값이므로 앱이 매 요청에 보낸다. 서버는 저장하지 않는다

**예외 하나 — 동화 앨범(§9).** 2026-08-27 부터 아이가 만든 동화는 게이트웨이가
MySQL 에 남긴다. 다음 날에도 다시 읽을 수 있어야 하기 때문이다. 그 밖의 것(진행도 ·
모국어 · 녹음)은 여전히 서버에 남지 않는다. 앨범은 기기가 만든 `childId`(UUIDv4)로
묶고 닉네임은 표시용으로만 함께 저장한다 — 자세한 것은 §9 와 `zooearly-erd.md`.

**예외 — `pronunciation`의 `sentenceId`(§6-1)는 앱이 만들지 않는다.** 발음 연습 문장
21개는 고정 목록이라 서버(`GET /pronunciation/sentences`)가 준다. 사용자별로 다른 걸
기억하는 게 아니라 누가 불러도 같은 값이 오므로 게이트웨이는 여전히 무상태다 — 아이
개인의 상태를 저장하는 것과는 다르다.

### 0.3 경로 매핑

앱은 이 문서의 `/api/v1/ai/*` 경로만 안다. **게이트웨이가 FastAPI로 부르는 실제 경로는
이것과 다르다** (v1.4.0부터) — FastAPI가 이미 정해둔 이름(`/internal/v1/speech/transcribe` 등)을
그대로 쓰기로 했고, 게이트웨이의 `application.yml`(`inference.path.*`)에서 관리한다.
FastAPI가 경로를 또 바꿔도 앱과 이 명세는 그대로다 — 게이트웨이 설정만 고치면 된다.

실제 대응은 FastAPI 담당자용 문서 [`zooearly-gateway-to-fastapi.md`](zooearly-gateway-to-fastapi.md) §1 참고.

### 0.4 타임아웃 정책

| 구간 | 값 | 초과 시 |
|---|---|---|
| Gateway → FastAPI 연결 | 3s | `504 AI_TIMEOUT` |
| Gateway → FastAPI 응답 (`tts` / 문장 목록) | 15s | `504 AI_TIMEOUT` |
| Gateway → FastAPI 응답 (`feedback` — OpenAI 를 두 번 직렬로) | 30s | `504 AI_TIMEOUT` |
| Gateway → FastAPI 응답 (`chat` — STT+LLM+TTS 3단) | 30s | `504 AI_TIMEOUT` |
| Gateway → FastAPI 응답 (`stt`) | 45s | `504 AI_TIMEOUT` |
| Gateway → FastAPI 응답 (`pronunciation`) | 65s | `504 AI_TIMEOUT` |
| Gateway → FastAPI 응답 (`story`) | 60s | `504 AI_TIMEOUT` |
| 앱 → Gateway | 위 값 + 5s 여유를 앱 쪽 클라이언트에 설정 | 앱 로컬 폴백 |

`feedback` 이 30초인 이유는 다르다 — 추론 서버가 그 안에서 OpenAI 를 **두 번 직렬로**
부른다(교정 문장을 만들고, 그 결과를 모국어로 번역). 15초로 뒀다가 배포 직후 실측에서
504 가 났다.

`stt` 와 `pronunciation` 이 유독 긴 이유는 **모델이 느려서가 아니라 컨테이너가 자고 있어서다.**
둘 다 min-replicas 가 0인 Azure Container App 을 부르고, 유휴 뒤 첫 요청이 컨테이너를
깨우며 30~43초가 걸린다(2026-08-25 실측). 평소에는 1초 안에 끝난다.

> 앱은 어떤 타임아웃·에러에서도 아이에게 "오류"를 보여주지 않는다. 전부 "괜찮아, 다시 해볼까?" 화면으로 폴백한다.

---

## 1. 공통 규약

### 1.1 타입 표기법

| 표기 | TypeScript 대응 | 설명 | 예시 |
|---|---|---|---|
| `string` | `string` | 문자열 | `"많이 주세요."` |
| `string(base64)` | `string` | base64 인코딩된 바이너리 | `"UklGRi4A..."` |
| `string(enum)` | union type | 허용값은 §1.4 | `"LUNCH"` |
| `string(JSON)` | `string` | multipart 필드에 실린 JSON 문자열 | `"[{\"role\":\"user\",...}]"` |
| `integer` | `number` | 정수 | `3` |
| `number` | `number` | 소수 | `0.97` |
| `boolean` | `boolean` | 참/거짓 | `true` |
| `object` | interface | 하위 필드는 별도 표 | `{ ... }` |
| `object[]` | `T[]` | 객체 배열 | `[{ ... }]` |
| **`?` 접미사** | nullable | **`null`이 올 수 있다** | `string?` |

> `?`가 없으면 `null`이 오지 않는다. 빈 배열과 `null`은 다르다 — 값이 없을 때 `[]`를 보내지 `null`을 보내지 않는다.

### 1.2 응답 포맷

**성공**

```json
{ "success": true, "data": { } }
```

**실패**

```json
{
  "success": false,
  "error": {
    "code": "AI_SERVER_ERROR",
    "message": "추론 서버가 응답하지 않습니다.",
    "field": null
  }
}
```

| 이름 | 타입 | 설명 |
|---|---|---|
| `error.code` | `string(enum)` | §1.3 에러 코드 |
| `error.message` | `string` | 개발자용. 아이 화면에 띄우지 않는다 |
| `error.field` | `string?` | 문제가 된 파라미터명. 검증 에러에만 채워진다 |

### 1.3 에러 코드

| HTTP | code | 발생 위치 | 상황 |
|---|---|---|---|
| 400 | `INVALID_PARAMETER` | Gateway | 필수 파라미터 누락·형식 오류 |
| 400 | `UNSUPPORTED_AUDIO_FORMAT` | Gateway | 허용 외 오디오 포맷 |
| 400 | `AUDIO_TOO_LARGE` | Gateway | 음성 파일 10MB 초과 |
| 413 | `PAYLOAD_TOO_LARGE` | Gateway | 요청 전체 용량 초과 |
| 422 | `STT_FAILED` | FastAPI | STT 엔진 자체 실패 (인식 실패와 다름 — §2 계약 참고) |
| 422 | `OFF_SCRIPT` | FastAPI | **(v2.1.0 신설)** 아이가 고른 문장이 아니라 아주 다른 말을 했다 (§6 계약 6) |
| 429 | `RATE_LIMITED` | FastAPI | OpenAI API 쿼터 초과 |
| 502 | `AI_SERVER_ERROR` | Gateway | FastAPI가 5xx를 반환하거나 연결 불가 |
| 404 | `NOT_FOUND` | Gateway | 없는 경로 · 앨범이 없거나 `childId` 가 그 동화의 주인이 아님(§9.3) |
| 504 | `AI_TIMEOUT` | Gateway | FastAPI 응답이 §0.4 타임아웃 초과 |
| 500 | `INTERNAL_ERROR` | Gateway | 게이트웨이 자체 오류 |

> **FastAPI의 어떤 에러도 앱에 그대로 새지 않는다.** FastAPI가 `{"detail": "..."}`를 내더라도 게이트웨이가 `AI_SERVER_ERROR`로 감싼다. 단 `STT_FAILED` / `OFF_SCRIPT` / `RATE_LIMITED`는 FastAPI가 §1.2 포맷으로 직접 만들어 보내고 게이트웨이는 그대로 통과시킨다.

> **`OFF_SCRIPT` 를 `INVALID_PARAMETER` 와 갈라 놓은 이유.** 나머지 4xx 는 아이가 할 수 있는 일이 없지만, 이건 **다시 말하면 되는 일**이다. 앱이 두 경우를 구분하지 못하면 둘 다 같은 처리를 하게 되는데 실제로 그랬다 — 앱이 채점 실패를 전부 칭찬 화면으로 흘려보내서, 아이가 전혀 다른 말을 해도 "잘했어!" 가 떴다. 서버는 알고 있었고 앱이 그 신호를 버렸다. 상태 코드를 422 로 둔 것도 필요해서다: 게이트웨이는 422/429 만 본문째 통과시키므로(§1.3) 다른 코드로 바꾸면 이 구분이 앱까지 닿지 못한다.

### 1.4 오디오 규격

**업로드 (앱 → 서버, multipart)**

| 항목 | 값 |
|---|---|
| 포맷 | `m4a` / `wav` / `webm` |
| 최대 용량 | 10MB |
| 최대 길이 | **30초** |
| multipart 필드명 | `audio` |

**다운로드 (서버 → 앱, JSON 내 base64)**

| 항목 | 값 |
|---|---|
| 포맷 | `mp3` (OpenAI TTS 기본) |
| 인코딩 | base64 문자열 |
| 필드 구조 | `{ "data": "<base64>", "format": "mp3" }` |

> **응답 오디오는 항상 같은 `audio` 객체 구조다.** `chat`과 `tts`가 동일한 모양을 쓴다 — 앱의 재생 코드가 하나면 된다.
>
> base64는 원본보다 약 33% 크다. TTS 문장은 짧아(수 초) 실사용 페이로드는 수백 KB 수준이므로 허용한다. 문장이 길어져 문제가 되면 그때 바이너리 응답으로 바꾼다.

### 1.5 Enum

**`scenario`** — LLM 프롬프트 컨텍스트용 힌트. 앱 로컬의 시나리오 코드와 동일하다.

| 값 | 상황 |
|---|---|
| `ARRIVAL` | 등교하기 |
| `CLASS` | 수업시간 |
| `LUNCH` | 급식시간 |
| `DISMISSAL` | 하교시간 |

**`nativeLanguage`** — 피드백·번역 생성 언어

| 값 | 언어 |
|---|---|
| `KOREAN` | 한국어 (번역 생략) |
| `CHINESE` | 중국어 |
| `VIETNAMESE` | 베트남어 |

**`role`** — `chat` 대화 이력의 화자

| 값 | 의미 |
|---|---|
| `user` | 아이 |
| `assistant` | AI 캐릭터(선생님/친구) |

---

## 2. POST /api/v1/ai/chat — 음성 대화 (통합 파이프라인) ★

아이의 음성을 올리면 서버가 **STT → LLM → TTS를 한 번에** 처리하고, 텍스트와 음성을 함께 돌려준다. 왕복 1회로 지연을 최소화한 핵심 엔드포인트다.

```http
POST /api/v1/ai/chat
Content-Type: multipart/form-data
```

**Request (multipart)**

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `audio` | `file` | ✅ | 아이의 발화. §1.4 업로드 규격 | `speech.m4a` |
| `scenario` | `string(enum)` | ✅ | LLM 시스템 프롬프트 구성용 | `"LUNCH"` |
| `history` | `string(JSON)` | ✅ | 지금까지의 대화. 없으면 `"[]"` | 아래 참고 |
| `nativeLanguage` | `string(enum)` | — | 생략 시 `KOREAN` | `"VIETNAMESE"` |
| `nickname` | `string` | ✅ | 아이 호칭. LLM이 말을 걸 때 쓴다. 최대 20자 | `"민수"` |

**`history` JSON 구조** — 앱이 로컬에 쌓아 매 요청에 실어 보낸다 (서버는 무상태)

```json
[
  { "role": "assistant", "content": "불고기 많이 줄까?" },
  { "role": "user", "content": "네, 많이 주세요." }
]
```

| 이름 | 타입 | 설명 |
|---|---|---|
| `[].role` | `string(enum)` | `user` / `assistant` |
| `[].content` | `string` | 발화 텍스트 |

> **`history`는 최근 10턴까지만 보낸다.** 그 이상은 앱이 잘라서 보낸다 — 프롬프트 길이와 비용이 대화 길이에 비례해 늘어나는 것을 앱 쪽에서 차단한다.

```bash
curl -X POST https://zooearly.app/api/v1/ai/chat \
  -F "audio=@speech.m4a;type=audio/m4a" \
  -F "scenario=LUNCH" \
  -F 'history=[{"role":"assistant","content":"불고기 많이 줄까?"}]' \
  -F "nativeLanguage=VIETNAMESE" \
  -F "nickname=민수"
```

**Response `200 OK`**

| 이름 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `userText` | `string?` | STT 결과. **못 알아들으면 `null`** | `"네, 많이 주세요."` |
| `aiText` | `string` | LLM이 생성한 응답 문장 | `"그래, 많이 줄게! 맛있게 먹어."` |
| `audio` | `object` | AI 응답의 TTS. §1.4 다운로드 규격 | `{ ... }` |
| `audio.data` | `string(base64)` | mp3 바이너리 | `"SUQzBAAA..."` |
| `audio.format` | `string` | 항상 `"mp3"` | `"mp3"` |

```json
{
  "success": true,
  "data": {
    "userText": "네, 많이 주세요.",
    "aiText": "그래, 많이 줄게! 맛있게 먹어.",
    "audio": { "data": "SUQzBAAA...", "format": "mp3" }
  }
}
```

**설계 계약**

1. **STT가 아이 말을 못 알아들어도 `200`이다.** `userText: null`로 내려가고, `aiText`는 "잘 안 들렸어. 다시 말해 줄래?" 류의 되묻기 문장이 온다. `422 STT_FAILED`는 STT 엔진 자체가 죽었을 때만 쓴다.
2. **응답을 받은 앱은 `history`에 두 턴을 추가한다** — `{role:"user", content:userText}` + `{role:"assistant", content:aiText}`. `userText`가 `null`이면 user 턴은 추가하지 않는다.
3. **오디오 원본은 서버에 저장하지 않는다.** 추론 직후 폐기한다. 응답 헤더 `X-Audio-Retention: none`.
4. **`aiText`는 국립국어원 표준 한국어교육과정 1~2급 어휘 범위로 생성한다.** 이 제약은 FastAPI의 시스템 프롬프트가 담당한다.

**에러** — `400 INVALID_PARAMETER` / `UNSUPPORTED_AUDIO_FORMAT` / `AUDIO_TOO_LARGE`, `422 STT_FAILED`, `429 RATE_LIMITED`, `502 AI_SERVER_ERROR`, `504 AI_TIMEOUT`

---

## 3. POST /api/v1/ai/stt — 음성 → 텍스트

음성만 텍스트로 바꾼다. **`SPEAK` 스텝(목표 문장 따라 말하기)** 처럼 LLM 응답이 필요 없는 화면에서 쓴다 — 인식 결과와 목표 문장의 매칭 판정은 앱 로컬에서 한다.

```http
POST /api/v1/ai/stt
Content-Type: multipart/form-data
```

**Request (multipart)**

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `audio` | `file` | ✅ | §1.4 업로드 규격 | `attempt.m4a` |
| `language` | `string` | — | BCP-47. 생략 시 `ko-KR` | `"ko-KR"` |

**Response `200 OK`**

| 이름 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `text` | `string?` | 인식 결과. **못 알아들으면 `null`** | `"많이 주세요"` |
| `confidence` | `number?` | 0~1. 엔진이 안 주면 `null`. **화면 표시 금지** | `0.94` |

```json
{
  "success": true,
  "data": { "text": "많이 주세요", "confidence": 0.94 }
}
```

> **인식 실패는 에러가 아니다.** `text: null`로 `200`이 내려간다. 앱은 이를 "다시 해볼까?" 화면으로 처리한다.

**에러** — `400 INVALID_PARAMETER` / `UNSUPPORTED_AUDIO_FORMAT` / `AUDIO_TOO_LARGE`, `422 STT_FAILED`, `429 RATE_LIMITED`, `502 AI_SERVER_ERROR`, `504 AI_TIMEOUT`

---

## 4. POST /api/v1/ai/tts — 텍스트 → 음성

텍스트를 음성으로 바꾼다. `DIALOGUE` 말풍선의 🔊 버튼, `LISTEN` 스텝의 다시 듣기, 피드백 화면의 자연스러운 표현·모국어 번역 재생에 쓴다.

**한국어 전용이 아니다.** 피드백 화면은 한국어 문장과 모국어 번역을 각각 재생하므로 `language`로 어느 쪽인지 알려준다.

```http
POST /api/v1/ai/tts
Content-Type: application/json
```

**Request Body**

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `text` | `string` | ✅ | 읽을 문장. 최대 200자 | `"불고기 많이 줄까?"` |
| `voice` | `string(enum)` | — | `TEACHER` / `FRIEND`. 생략 시 `TEACHER` | `"TEACHER"` |
| `speed` | `number` | — | 0.5~1.5. 생략 시 `0.9` (아동용 기본 느리게) | `0.9` |
| `language` | `string(enum)` | ✅ | 읽을 문장의 언어. §1.5 enum | `"VIETNAMESE"` |

```json
{ "text": "불고기 많이 줄까?", "voice": "TEACHER", "speed": 0.9, "language": "KOREAN" }
```

모국어 번역을 읽어줄 때 — 피드백 화면 아래쪽 상자 (`voice`·`speed`는 생략 가능)

```json
{ "text": "Cho mình nhiều nhé.", "language": "VIETNAMESE" }
```

**Response `200 OK`**

| 이름 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `audio` | `object` | §1.4 다운로드 규격. `chat`과 동일 구조 | `{ ... }` |
| `audio.data` | `string(base64)` | mp3 바이너리 | `"SUQzBAAA..."` |
| `audio.format` | `string` | 항상 `"mp3"` | `"mp3"` |

> **`language`는 필수다.** 한국어 문장을 읽을 때도 `"KOREAN"`을 명시한다. 같은 엔드포인트로 여러 언어가 나가므로 추측의 여지를 두지 않는다 — 성조 부호 없는 로마자 표기(`chao! Minh cung rat vui`)는 다른 언어로 오판되기 쉽고, 그러면 아이가 엉뚱한 발음을 듣는다.
>
> **`/stt`의 `language`와 형식이 다르다.** `/stt`는 BCP-47 자유 문자열(`ko-KR`), `/tts`는 §1.5 enum이다. `/tts`는 앱이 이미 가진 `nativeLanguage` 값을 그대로 쓰면 되고, 닫힌 집합이라 게이트웨이가 검증할 수 있다.
>
> **`voice`는 OpenAI 보이스 ID가 아니라 역할 enum이다.** 역할 → 실제 보이스 매핑(`TEACHER` → `nova` 등)은 FastAPI 설정에 둔다. 보이스를 교체해도 앱과 게이트웨이는 안 바뀐다.
>
> **같은 문장의 TTS 결과는 앱이 로컬 캐시한다.** 스텝 문장은 고정 텍스트라 캐시 적중률이 높다 — 같은 문장을 매번 서버에 묻지 않는다.

**에러** — `400 INVALID_PARAMETER`, `429 RATE_LIMITED`, `502 AI_SERVER_ERROR`, `504 AI_TIMEOUT`

---

## 5. POST /api/v1/ai/feedback — 발화 피드백 생성

> ⚠️ **현재 이 엔드포인트를 부르는 화면이 없다.** 표현 교정 화면("이렇게 말하면 더
> 자연스러워요")을 구현하지 않기로 했고, FastAPI 에는 `/internal/v1/feedback/expression` 이 구현돼 있고 게이트웨이도 연결돼 있다 — **부르는 앱 화면만 없다.**
> 게이트웨이에는 구현·테스트가 끝난 상태로 남겨둔다 — 나중에 화면이 생기면
> 앱에서 부르기만 하면 된다. 아래 명세는 그때를 위한 것이다.

아이의 발화(STT 결과 텍스트)와 목표 문장을 주면, `FEEDBACK` 스텝에 그릴 피드백 객체를 생성한다. **음성이 아니라 텍스트를 받는다** — STT는 §3에서 이미 끝났다.

```http
POST /api/v1/ai/feedback
Content-Type: application/json
```

**Request Body**

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `targetSentence` | `string` | ✅ | 목표 문장 (앱 번들의 스텝 데이터) | `"많이 주세요."` |
| `recognizedText` | `string?` | ✅ | STT 결과. 인식 실패면 `null` | `"많이 주세여"` |
| `scenario` | `string(enum)` | — | 상황 힌트 | `"LUNCH"` |
| `nativeLanguage` | `string(enum)` | — | 번역 생성 언어. 생략 시 `KOREAN`(번역 없음) | `"VIETNAMESE"` |
| `nickname` | `string` | ✅ | 아이 호칭. 피드백 문구에 쓴다. 최대 20자 | `"민수"` |

```json
{
  "targetSentence": "많이 주세요.",
  "recognizedText": "많이 주세여",
  "scenario": "LUNCH",
  "nativeLanguage": "VIETNAMESE",
  "nickname": "민수"
}
```

**Response `200 OK`**

| 이름 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `understood` | `boolean` | 의미가 통했는가 (아이콘 결정) | `true` |
| `matched` | `boolean` | 목표 문장과 통했는가 (별점 판정용) | `true` |
| `similarity` | `number` | 0~1. **화면 표시 금지** | `0.92` |
| `title` | `string` | 배너 제목 | `"잘했어요!"` |
| `body` | `string?` | 배너 본문 | `"무슨 뜻인지 잘 이해했어요."` |
| `naturalSentence` | `string?` | 더 자연스러운 표현 | `"많이 주세요."` |
| `naturalHint` | `string?` | 설명문. 불필요하면 `null` | `"'주세여'보다 '주세요'가 좋아요."` |
| `highlightWords` | `string[]` | 밑줄 칠 어절. 없으면 `[]` | `["주세요"]` |
| `translation` | `string?` | `naturalSentence`의 모국어 번역. `KOREAN`이면 `null` | `"Cho mình nhiều nhé."` |

```json
{
  "success": true,
  "data": {
    "understood": true,
    "matched": true,
    "similarity": 0.92,
    "title": "잘했어요!",
    "body": "무슨 뜻인지 잘 이해했어요.",
    "naturalSentence": "많이 주세요.",
    "naturalHint": "'주세여'보다 '주세요'가 좋아요.",
    "highlightWords": ["주세요"],
    "translation": "Cho mình nhiều nhé."
  }
}
```

> `naturalSentence`와 `translation`은 피드백 화면에서 각각 🔊 버튼이 달린 상자로 표시된다. 탭하면 앱이 `/tts`를 호출한다 — 흐름은 §7 참고.

**설계 계약**

1. **`recognizedText: null`도 유효한 요청이다.** "괜찮아, 다시 해볼까?" 류의 격려 피드백이 생성된다 (`understood: false`, `matched: false`).
2. **`title`에 "틀렸어요"류 문구를 넣지 않는다.** 이 제약은 FastAPI 프롬프트가 담당하되, 게이트웨이 테스트에서도 검증한다.
3. **`matched` 판정과 별점 계산은 이 응답을 받은 앱이 로컬에서 한다.** 서버는 판정 재료만 준다.
4. **발화 기록은 서버에 남지 않는다.** 성공 문장 보관함(마이페이지)도 앱 로컬 저장소가 담당한다.

**에러** — `400 INVALID_PARAMETER`, `429 RATE_LIMITED`, `502 AI_SERVER_ERROR`, `504 AI_TIMEOUT`

---

## 6. POST /api/v1/ai/pronunciation — 발음 채점

아이가 따라 말한 녹음의 **발음**을 채점한다. `발음 피드백` 화면에서 쓴다.

> **`/feedback`과 다르다.** 저쪽은 "어떤 **단어**를 골랐나"를 텍스트로 보고,
> 이쪽은 "어떻게 **소리** 냈나"를 오디오로 본다.
> 단어를 맞게 골랐어도 발음이 어눌할 수 있고, 그 반대도 있다.
> 그래서 STT를 거치지 않고 **녹음을 그대로 보낸다** — 텍스트로는 발음을 알 수 없다.

```http
POST /api/v1/ai/pronunciation
Content-Type: multipart/form-data
```

**Request**

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `audio` | `file` | ✅ | 따라 말한 녹음. §1.4 업로드 규격 | `speech.m4a` |
| `sentenceId` | `string` | ✅ | `GET /api/v1/ai/pronunciation/sentences`(§6-1)가 준 값 중 하나 | `"arrival_2"` |

> **자유 텍스트가 아니다.** (v1.5.0부터) FastAPI가 자기 문장 목록에서 채점 기준을
> 직접 찾기 때문에, 목록에 없는 문장으로는 채점할 수 없다. 앱이 `sentenceId`를
> 만들어내면 안 되고, 반드시 §6-1이 내려준 값을 그대로 써야 한다.

```bash
curl -X POST https://zooearly.app/api/v1/ai/pronunciation \
  -F "audio=@speech.m4a;type=audio/m4a" \
  -F "sentenceId=arrival_2"
```

**Response `200 OK`**

| 이름 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `sentenceId` | `string` | 요청에 실은 값을 그대로 돌려준다 | `"arrival_2"` |
| `sentence` | `string` | 채점 대상 문장 | `"안녕! 우리 같이 놀자!"` |
| `targetWord` | `string?` | **가장 약하게 발음한 어절.** 빈칸으로 만들 대상. **`null`이면 전부 기준 이상** — 아래 계약 3 참고 | `"지내자"` |
| `targetIndex` | `integer?` | 그 어절이 몇 번째인가 (0부터) | `2` |
| `targetZ` | `number?` | 그 어절의 z점수. **낮을수록 약함** | `-1.82` |
| `words` | `object[]` | 어절별 채점 결과 | 아래 |
| `words[].word` | `string` | 어절 | `"지내자"` |
| `words[].z` | `number?` | z점수 | `-1.82` |
| `words[].warn` | `boolean` | 주의 임계값 미만인가 | `true` |
| `words[].worstPhone` | `string?` | 그 어절에서 가장 약한 음소 | `"ㄴ"` |

```json
{
  "success": true,
  "data": {
    "sentenceId": "arrival_2",
    "sentence": "안녕! 우리 같이 놀자!",
    "targetWord": "지내자",
    "targetIndex": 2,
    "targetZ": -1.82,
    "words": [
      { "word": "안녕!",  "z": 0.31,  "warn": false, "worstPhone": null },
      { "word": "우리",   "z": -0.42, "warn": false, "worstPhone": null },
      { "word": "친하게", "z": -1.12, "warn": false, "worstPhone": null },
      { "word": "지내자", "z": -1.82, "warn": true,  "worstPhone": "ㄴ" }
    ]
  }
}
```

**설계 계약**

1. **점수는 0~1이 아니라 z점수다.** 또래 규준 대비 상대값이라 **음수가 정상**이다. 0에 가까울수록 또래 평균, 낮을수록 약하다. **화면에 숫자를 표시하지 않는다** — 아이에게 점수를 보여주지 않는다.
2. **빈칸은 `targetWord` 하나뿐이다.** `warn`이 여러 개 켜져도 빈칸은 하나만 만든다. 여러 곳을 동시에 지적하면 아이가 좌절한다.
3. **잘함/못함 판정은 FastAPI가 한다** (v1.5.0부터 바뀐 부분). `targetWord`가 `null`이면 **모든 어절이 기준(z ≥ -1.5) 이상**이라는 뜻이다 — 이때는 **퀴즈 화면 없이 바로 칭찬 화면**으로 간다. `null`이 아니면 퀴즈 화면으로 간다. 앱은 이 값을 보고 분기만 하면 되고, 직접 임계값을 계산할 필요가 없다.
4. **빈칸 문장은 앱이 만든다.** 서버는 `quizSentence`를 주지 않는다 — `sentence`를 공백 기준으로 나눠 `targetIndex`번째를 빈칸으로 바꾸면 된다. (v1.5.0에서 `quizSentence` 필드가 빠졌다.)
5. **규준 집단이 우리 사용자와 다르다.** ⚠️ 아래 주의 참고 — 다만 임계값(z ≥ -1.5) 자체는 FastAPI가 정해서 이미 반영했다.

> ### ⚠️ 규준 한계 — 참고
>
> 채점 모델의 규준은 **만 8~13세 네이티브 아동** 262명에서 산출됐다.
> 이 앱의 사용자는 **중도입국 초등 1~2학년(만 7~8세)** 이라 연령·모어가 모두 다르다.
>
> 모델 제작자의 실험(`demo_l2.py`)에서, 외국어 억양이 섞이면
> **주의 어절 비율이 7% → 59%** 로 뛰었다. 임계값을 너무 엄격하게 잡으면
> 아이가 계속 틀렸다는 화면만 보게 되어 §0.4의 톤 원칙과 어긋난다.
>
> **잘함/못함 임계값(z ≥ -1.5)은 FastAPI가 정해서 이미 적용했다** (계약 3).
> 그래도 실제 중도입국 아동 녹음으로 검증된 값은 아니므로, 시연·초기 운영 중
> "다 틀렸다고 나온다"는 피드백이 들어오면 FastAPI 쪽에 임계값 재조정을 요청한다.

**에러** — `400 INVALID_PARAMETER` / `UNSUPPORTED_AUDIO_FORMAT` / `AUDIO_TOO_LARGE`, **`422 OFF_SCRIPT`**, **`422 INVALID_PARAMETER`**, `429 RATE_LIMITED`, `502 AI_SERVER_ERROR`, `504 AI_TIMEOUT`

> **이 경로에 `STT_FAILED` 는 오지 않는다.** 예전 명세가 그렇게 적어 뒀지만 구현을 훑어보면
> 발음 채점은 STT 를 거치지 않는다 — 오디오를 그대로 채점 서비스에 넘긴다. 이 자리의 422 는
> 아래 둘뿐이다.
>
> | code | `field` | 언제 |
> |---|---|---|
> | `OFF_SCRIPT` | `audio` | 고른 문장이 아니라 아주 다른 말을 했다 |
> | `INVALID_PARAMETER` | `audio` | 녹음이 비었거나 · 20MB 를 넘거나 · 채점할 수 있는 어절이 하나도 없다(아이가 아무 말도 안 했거나 웅얼거렸다) |
> | `INVALID_PARAMETER` | `sentenceId` | 문장을 안 골랐거나 · 목록에 없는 id 다 — **앱의 버그**다. 아이가 다시 말해도 안 풀린다 |
>
> **앱은 `code` 가 아니라 422 인지를 본다.** 앞의 두 줄은 아이 입장에서 전부 "네 말이 닿지
> 않았으니 다시 해보자" 하나이고, 세 줄로 다른 말을 해줄 것도 아니면서 코드를 갈라 보면
> 서버가 코드를 하나 더 늘리는 날 조용히 칭찬 화면으로 새는 길만 생긴다. 마지막 줄(앱 버그)은
> 앱이 `sentenceId` 를 서버 목록에서만 가져오고 비어 있으면 채점을 부르지 않으므로 실제로는
> 오지 않는다 — 오면 되묻기 3회를 소진하고 다음으로 넘어간다(거짓 칭찬은 없다).

6. **`422 OFF_SCRIPT` 는 실패가 아니라 되묻기다.** 아이가 고른 문장이 아니라 아주 다른 말을 했을 때 온다. 앱은 이 코드만 따로 받아 **녹음 화면에 그대로 머물면서** "잘 못 들었어. 다시 말해줄래?" 로 되묻고, 다시 녹음할 수 있게 한다. 다른 4xx 와 같이 처리하면 안 된다 — 그러면 아이가 배우는 것이 "아무 말이나 하면 통과한다" 가 된다. 다만 세 번까지만 되묻고, 그 뒤에는 넘어갈 길(고스트 버튼)을 연다. 마이크가 멀거나 주변이 시끄러운 날에 아이를 한 화면에 가둘 수는 없다. **그 길도 칭찬 화면으로는 가지 않는다.**

---

## 6-1. GET /api/v1/ai/pronunciation/sentences — 발음 연습 문장 목록

발음 연습용 문장 **21개**를 받는다 — 등교 9 · 수업시간 동시 3 · 급식 3 · 하교 6.
개수는 카테고리마다 다르고 앞으로도 늘어난다. **앱은 카테고리로 거른 뒤 무작위 3개만
화면에 띄운다** — 아홉 개를 한 번에 보여주면 아이가 고르지 못하고 헤맨다.
수업시간 동시는 고르는 것이 아니라 앱이 회차마다 한 편을 뽑는다.
**"어떤 표현을 사용해볼까요?" 화면의 선택지 3개, 수업시간 "같이 읽어볼까요?"의
시 구절이 모두 여기서 온다** (2026-08-24부터) — 그 전에는 앱 번들에 하드코딩돼 있었다.

```http
GET /api/v1/ai/pronunciation/sentences
```

요청 파라미터 없음.

**Response `200 OK`**

| 이름 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `sentenceId` | `string` | `POST /api/v1/ai/pronunciation`(§6)에 그대로 실어 보내는 값 | `"arrival_1"` |
| `category` | `string(enum)` | `arrival` / `study` / `lunch` / `departure`. 화면 그룹핑용 | `"arrival"` |
| `text` | `string` | 화면에 보여줄 문장 원문. `study`만 여러 문장이 한 항목에 이어져 있다 (시 전체) | `"안녕 나도 만나서 반가워 !"` |
| `translations` | `object` | **(v2.1.0 신설)** 이 문장의 모국어 뜻. 키는 언어 코드(`vi`·`zh`), 값은 번역문. 한국어는 키가 없다 — 원문이 곧 그것이다 | `{ "vi": "Chào cậu!…", "zh": "你好！…" }` |
| `translationParts` | `object` | **(v2.2.0 신설)** 번역문의 어느 조각이 한국어 어느 어절인지. 빈칸 자리를 모국어 문장에서 짚어주는 데 쓴다. 동시(`study`)에는 없다(`{}`) | 아래 |

```json
{
  "success": true,
  "data": [
    {
      "sentenceId": "arrival_1",
      "category": "arrival",
      "text": "안녕 나도 만나서 반가워 !",
      "translations": {
        "vi": "Chào cậu! Mình cũng rất vui được gặp cậu!",
        "zh": "你好！我也很高兴见到你！"
      }
    },
    { "sentenceId": "arrival_2",   "category": "arrival",   "text": "안녕! 우리 같이 놀자!" },
    { "sentenceId": "arrival_3",   "category": "arrival",   "text": "안녕! 같이 들어가자!" },
    { "sentenceId": "study_1",     "category": "study",     "text": "노란 꽃이 피었어요. 예쁜 꽃이 피었어요. 바람이 살랑살랑 꽃이 웃어요." },
    { "sentenceId": "lunch_1",     "category": "lunch",     "text": "조금만 주세요." },
    { "sentenceId": "lunch_2",     "category": "lunch",     "text": "적당히 주세요." },
    { "sentenceId": "lunch_3",     "category": "lunch",     "text": "많이 주세요." },
    { "sentenceId": "departure_1", "category": "departure", "text": "선생님, 안녕히 가세요!" },
    { "sentenceId": "departure_2", "category": "departure", "text": "선생님, 감사합니다!" },
    { "sentenceId": "departure_3", "category": "departure", "text": "내일 또 뵙겠습니다!" }
  ]
}
```

### `translationParts` 의 모양

```jsonc
{
  "vi": [
    { "t": "Cho con",          "k": [1] },   // "주세요" 가 앞에 온다
    { "t": "một chút thôi ạ.", "k": [0] }    // "조금만" 이 뒤에 온다
  ],
  "zh": [
    { "t": "请给我",   "k": [1] },
    { "t": "一点点。", "k": [0] }
  ]
}
```

- **조각은 번역문을 읽는 순서대로다.** 한국어 순서가 아니다 — 어순이 다르면
  `k` 가 뒤죽박죽인 것이 정상이고, 그게 맞는 것이다.
- **`k` 는 배열이다.** 한 조각이 어절 여럿을 덮을 수 있다 — "선생님, **안녕히 계세요!**"
  는 베트남어로 `em về ạ!` 한 덩어리라 억지로 쪼개면 뜻이 어긋난다.
  대응하는 어절이 없는 조각(순수 문법 요소)은 `[]` 다.
- **조각을 이으면 `translations` 와 글자 하나까지 같다.** 베트남어는 공백 한 칸,
  중국어는 아무것도 넣지 않고 잇는다. 서버가 기동할 때 이걸 검산해서, 두 표 중
  한쪽만 고치면 배포 전에 터진다.
- **`k` 는 `text.split(' ')` 의 인덱스다.** `/pronunciation` 이 주는 `targetIndex`
  와 같은 눈금이라 그대로 맞춰 보면 된다.
- **비어 있을 수 있다.** 동시(`study`)는 빈칸 퀴즈를 내지 않아 `{}` 다. 앱은 그때
  뜻만 통째로 보여준다 — 짚어줄 자리를 모르는 채 아무 데나 밑줄을 그으면
  틀린 것을 가르치게 된다.

**설계 계약**

1. **`category`는 §1.5의 `scenario` enum과 다르다.** `arrival`/`study`/`lunch`/`departure`는
   소문자이고 이 API 전용 값이다. `scenario`(`ARRIVAL`/`CLASS`/`LUNCH`/`DISMISSAL`, 대문자)와
   섞어 쓰지 않는다. 특히 `departure` ↔ `DISMISSAL`, `study` ↔ `CLASS` 이름이 다르다는 점을 주의한다.
2. **`study`만 3개가 아니라 1개다.** 시가 하나뿐이라 고를 필요가 없어서인 것으로 보인다.
   화면에 "고르는 UI"가 필요 없다 — `category === "study"`인 항목을 그대로 쓰면 된다.
3. **전부를 한 번에 받는다.** 시나리오마다 따로 부르지 않는다 — 앱이 `category`로
   화면에 맞는 항목만 걸러서, 그중 **무작위 3개**를 보여준다. 뽑기는 앱이 한다.
   서버는 누가 불러도 같은 목록을 준다(§0.2 무상태 원칙).
4. **개수를 숫자로 못박지 마라.** 등교 9 · 수업 3 · 급식 3 · 하교 6 이지만 이건 콘텐츠라
   늘어난다. `study` 가 1개라고 적어 뒀던 옛 명세가 3편으로 늘면서 한 번 틀렸다.
5. **앱이 캐시해도 된다.** 이 목록은 고정값이다. 앱 실행마다 새로 받을 필요는 없지만,
   서버가 바뀔 가능성을 생각하면 세션마다 한 번은 새로 받는 편이 안전하다.
5. **`말해보기`(자유 발화) 경로에는 `sentenceId`가 없다.** 이 목록에서 문장을 **고른**
   경우에만 `sentenceId`가 생기고, 그 값으로만 §6 발음 채점을 부를 수 있다.
7. **번역을 여기에 실어 보내는 것은 의도한 설계다.** 앱의 힌트 전구(💡)를 누르면
   퀴즈 문장의 모국어 뜻이 뜨는데, 그때 번역을 따로 부르지 않는다. 목록이 고정이라
   함께 담는 편이 맞다 — 누를 때마다 부르면 아이가 전구를 누르고
   몇 초를 기다려야 하고, 그 사이 자기가 무엇을 물었는지를 잊는다. 게이트웨이에
   번역 엔드포인트가 없다는 사정도 같은 방향을 가리켰다(FastAPI의
   `/internal/v1/text/translate`는 게이트웨이가 노출하지 않는다).
8. **v2.1.0 은 하위호환이다.** `translations` 는 **추가만** 됐다. 기존 필드는 이름도
   타입도 그대로고, 이 필드를 안 읽는 클라이언트는 예전과 완전히 같이 동작한다.
   게이트웨이는 이 응답의 본문을 파싱하지 않고 문자열째 흘려보내므로
   (`AiRelayService.sentences()` → `ResponseEntity<String>`) **게이트웨이 코드는
   한 줄도 바뀌지 않았다.** 새 엔드포인트도, 없어진 엔드포인트도 없다.
9. **번역이 비어 있을 수 있다고 보고 짜야 한다.** 문장을 새로 넣으면서 번역을
   빠뜨리면 `{}` 가 온다. 앱은 그때 전구를 아예 보여주지 않는다 — 눌러도 아무 일이
   없는 버튼이 화면에 있는 것이 제일 나쁘다. (서버 쪽 회귀 테스트:
   `test_every_sentence_carries_mother_tongue_translations`)

**에러** — `502 AI_SERVER_ERROR`, `504 AI_TIMEOUT`

---

## 엔드포인트 요약 (10개)

| 절 | Method | Path | 입력 | 출력 | 쓰는 화면 |
|---|---|---|---|---|---|
| §2 | POST | `/api/v1/ai/chat` | 음성 + history (multipart) | 텍스트 + 음성(base64) | 자유 대화 — **현재 쓰는 화면 없음** |
| §3 | POST | `/api/v1/ai/stt` | 음성 (multipart) | 텍스트 | 말해보기, 같이 읽어볼까요 |
| §4 | POST | `/api/v1/ai/tts` | 텍스트 (JSON) | 음성(base64) | 🔊 버튼, 시 터치 |
| §5 | POST | `/api/v1/ai/feedback` | 텍스트 2개 (JSON) | **표현 교정** 객체 | 이렇게 말하면 더 자연스러워요 — **현재 쓰는 화면 없음** |
| §6 | POST | `/api/v1/ai/pronunciation` | 음성 + `sentenceId` (multipart) | **발음 채점** 객체 | 발음 피드백 (빈칸 퀴즈) |
| §6-1 | GET | `/api/v1/ai/pronunciation/sentences` | 없음 | 문장 21개 배열 | 어떤 표현을 사용해볼까요?, 같이 읽어볼까요? |
| §6-2 | POST | `/api/v1/ai/story` | 하루치 4장면 (JSON) | **동화** 객체 | 동화 생성 |
| §9 | POST | `/api/v1/albums` | `childId` + 동화 (JSON) | `{ id }` | 동화가 만들어진 직후 (앱이 자동으로) |
| §9 | GET | `/api/v1/albums` | `childId` (쿼리) | 동화 목록 | 내 동화 앨범 |
| §9 | GET | `/api/v1/albums/{id}` | `childId` (쿼리) | **동화** 한 편 | 앨범에서 동화 읽기 |

> **`/albums` 만 `/ai` 아래가 아니다.** `/api/v1/ai/*` 는 "추론 서버로 중계한다"는
> 뜻이고, 앨범은 게이트웨이가 직접 가진 데이터라 이름이 뜻과 맞아야 한다.
> 여기서는 게이트웨이가 body 를 열어 보고 응답도 직접 만든다.

> `feedback`과 `pronunciation`은 성격이 다르다. 전자는 **어떤 단어를 골랐나**(텍스트),
> 후자는 **어떻게 소리 냈나**(오디오)를 본다. 화면도 다르다.

---

## 6-2. POST /api/v1/ai/story — 동화 생성

하루치 플레이 기록 4장면을 받아 LLM이 동화로 엮어 돌려준다.

**다른 엔드포인트와 다른 점**: "방금 한 행동"이 아니라 **"오늘 한 일 전부"** 를 한 번에 보낸다.
그 기록을 모아두는 것은 **앱의 몫**이다 — 생성 요청 자체는 무상태다.
만들어진 동화를 남기는 것은 별개의 **§9 앨범 API** 다.

### 요청 (application/json)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `childName` | string | ✅ | 아이 이름. 내레이션에 그대로 쓰인다. 1~20자 |
| `scenes` | array | ✅ | **정확히 4개.** 순서 고정: `school_arrival` → `class` → `lunch` → `school_departure` |
| `scenes[].category` | string | ✅ | 위 4개 값 중 하나. 위치와 값이 일치해야 한다 |
| `scenes[].partnerLine` | string | 조건부 | **대화 장면(등교·점심·하교) 필수.** 상대방이 아이에게 한 말 |
| `scenes[].childSaid` | string \| null | ❌ | 아이가 고른 문장. 안 골랐으면 `null` |
| `scenes[].poemText` | string | 조건부 | **수업(`class`) 필수.** 아이가 읽은 동시 전문 |
| `scenes[].practicedWord` | string \| null | ❌ | 발음이 약해 연습한 낱말(`/pronunciation`의 `targetWord`) |

```json
{
  "childName": "지우",
  "scenes": [
    { "category": "school_arrival", "partnerLine": "안녕! 오늘도 만나서 반가워", "childSaid": "안녕! 나도 만나서 반가워 !" },
    { "category": "class", "poemText": "노란 꽃이 피었어요. 바람이 살랑살랑 꽃이 웃어요.", "practicedWord": "살랑살랑" },
    { "category": "lunch", "partnerLine": "오늘 반찬 맛있게 먹어요", "childSaid": null },
    { "category": "school_departure", "partnerLine": "오늘도 수고했어, 내일 봐", "childSaid": "선생님, 안녕히 가세요!" }
  ]
}
```

> **`category`는 다른 곳의 값과 다르다.** `/pronunciation/sentences`는 `arrival`·`study`·`lunch`·`departure`를 쓰고,
> 여기는 `school_arrival`·`class`·`lunch`·`school_departure`다. 섞어 쓰면 `400`이다.

### 응답 (200)

| 필드 | 타입 | 설명 |
|---|---|---|
| `title` | string | 동화 제목 |
| `scenes` | array | 요청과 **같은 개수·순서·카테고리** |
| `scenes[].subtitle` | string | 장면 소제목 |
| `scenes[].opening` | string | 장면을 여는 전환구 |
| `scenes[].quote` | string \| null | 아이가 한 말 인용. 없으면 `null` |
| `scenes[].narration` | string | 동화 문장 2~3문장 |

### 에러

| 상태 | `field` | 상황 |
|---|---|---|
| 400 | `childName` | 없거나 비었거나 20자 초과 |
| 400 | `scenes` | 배열이 아니거나 4개가 아님 |
| 400 | `scenes[i].category` | 값이 없거나 그 자리에 올 값이 아님(순서 위반) |
| 400 | `scenes[i].partnerLine` | 대화 장면인데 비어 있음 |
| 400 | `scenes[i].poemText` | 수업 장면인데 비어 있음 |
| 502 | — | LLM 호출 실패 |
| 504 | — | **60초** 안에 안 끝남 |

### ⚠️ 앱이 알아둘 것

- **타임아웃이 60초다.** 15초로 끝나는 `tts`·문장 목록보다 훨씬 길다(엔드포인트별 값은 §0.4 표). 4장면을 한 번에 생성하기 때문이다.
  **반드시 로딩 화면을 띄운다.** 실패해도 §0.4대로 `"괜찮아, 다시 해볼까?"` 로 폴백한다
- **기록은 앱이 모은다.** 서버는 저장하지 않으므로, 하루를 진행하며 4장면을 앱이 쌓아뒀다가 한 번에 보낸다
- **`childSaid`는 `null`이어도 된다.** 아이가 말을 고르지 않은 장면이 있을 수 있다

---

## 7. 대표 호출 흐름

```
[표현 고르기 → 발음 피드백]
  ① GET /pronunciation/sentences        → 문장 21개(앱이 3개만 띄운다)
       └ category로 현재 시나리오(등교/급식/하교)에 맞는 3개만 골라 보여준다.
         (수업시간의 시 읽기는 category="study" 1개를 고를 것 없이 바로 쓴다)
  ② 아이가 3개 중 하나를 고른다          → 그 sentenceId를 들고 있는다
  ③ 🔊 문장 상자 탭 → POST /tts          → 미리 들어본다 (선택)
  ④ 🎤 따라 말하기 녹음 종료
  ⑤ POST /pronunciation { audio, sentenceId }
       └ targetWord가 null이면 → 칭찬 화면 (퀴즈 없이 바로)
       └ null이 아니면        → 퀴즈 화면. sentence + targetIndex로 빈칸을 앱이 만든다
  ⑥ 앱 로컬: 진행 상태 저장

[SPEAK 스텝 — 자유 발화, 채점 없이 인식만]
  ① 녹음 종료
  ② POST /stt                          → "많이 주세여"
  ③ 앱 로컬: 화면에 반영 (표현 교정 §5는 현재 미사용)

[자유 대화 화면]
  ① 녹음 종료
  ② POST /chat (audio + scenario + history)
       서버 내부: STT → LLM → TTS
  ③ 응답: userText + aiText + mp3
  ④ 앱 로컬: history에 2턴 추가, mp3 재생

[DIALOGUE 스텝 — 🔊 버튼]  /  [LISTEN 스텝 — 다시 듣기]
  ① 로컬 캐시 확인 → 있으면 즉시 재생
  ② 없으면 POST /tts { text: <앱 번들 문장>, language: "KOREAN" }
       └ 한국어 문장이어도 language를 넣는다. 필수다
  ③ mp3 캐시 + 재생

[피드백 화면 — 🔊 상자 두 개]  ※ §5가 구현되면 쓸 흐름. 현재는 미사용
  ① POST /feedback 응답에서 두 문장을 받아둔다
       naturalSentence  "많이 주세요."          (한국어)
       translation      "Cho mình nhiều nhé."   (모국어, KOREAN이면 null)
  ② 윗상자 탭  → POST /tts { text: naturalSentence, language: "KOREAN" }
  ③ 아랫상자 탭 → POST /tts { text: translation,     language: <아이의 nativeLanguage> }
  ④ 앱 로컬: 문장별로 캐시. 같은 문장은 두 번 묻지 않는다

[네트워크 실패 시 (어디서든)]
  → "괜찮아, 다시 해볼까?" 폴백. 스텝 진행은 막지 않는다.
```

> **스텝 플레이(목표 문장 있음) vs `chat`(자유 대화)의 구분 기준**: 목표 문장이 정해져 있으면 `stt`/`pronunciation` 조합을 쓰고, 자유 대화면 `chat`을 쓴다. 스텝 플레이에서 `chat`을 쓰지 않는 이유는 LLM 응답 생성·TTS가 불필요해 지연과 비용만 늘기 때문이다.
>
> **피드백 화면의 아랫상자에는 `language`를 반드시 넣는다.** 같은 `/tts`로 한국어와 모국어가 둘 다 나가므로, 언어를 안 알려주면 FastAPI가 텍스트로 추측해야 한다. 성조 부호 없는 로마자 표기(`chao! Minh cung rat vui`)는 오판되기 쉽고, 그러면 아이가 엉뚱한 발음을 듣는다.
>
> **"어떤 표현을 사용해볼까요?"의 선택지는 (v1.5.0부터) 서버가 준다.** `GET /pronunciation/sentences`가 내려주는 21개 중 시나리오에 맞는 것을 앱이 걸러, 그중 **무작위 3개**를 띄운다(등교 9 · 급식 3 · 하교 6 중에서). 수업시간 동시 3편도 같은 목록에 있고, 고르는 화면 없이 앱이 한 편을 뽑는다. §0.2의 무상태 원칙과는 어긋나지 않는다 — 서버가 사용자별로 다른 걸 기억하는 게 아니라, 누가 불러도 같은 고정 목록을 주는 것뿐이다. (다른 화면의 스텝 진행 상태 같은 건 여전히 앱 로컬이다.)

---

## 8. 게이트웨이 구현 노트 (Spring)

기존 도메인 설계(`zooearly-domain-design.md`)의 4개 도메인(user/story/play/speech)은 이 아키텍처에서 **전부 사라진다.** 남는 구조는 이것뿐이다.

```
src/main/java/com/zooearly/
├── ZooEarlyApplication.java
├── common/
│   ├── response/ApiResponse.java        success/data/error 래퍼
│   ├── response/ErrorCode.java          §1.3
│   └── exception/GlobalExceptionHandler.java
└── ai/
    ├── AiController.java                4개 엔드포인트
    ├── AiRelayService.java              검증 + FastAPI 호출 + 에러 변환
    └── client/
        └── InferenceClient.java         WebClient/RestClient. 타임아웃 §0.4
```

- **중계 경로(`/api/v1/ai/*`)는 DB 를 쓰지 않는다.** JPA·데이터소스는 앨범(§9)만 쓰고,
  중계와 섞지 않는다.
- `AiRelayService`는 응답 body를 파싱하지 않고 통과시키는 것이 기본이다. 파싱하는 순간 FastAPI 응답 스키마가 바뀔 때마다 게이트웨이도 배포해야 한다.
- multipart는 스트리밍으로 릴레이한다(메모리에 전부 올리지 않는다) — 10MB 오디오 동시 요청을 견디기 위함이다.

---

## 9. 동화 앨범 — `/api/v1/albums`

> **여기만 `/ai` 아래가 아니다.** `/api/v1/ai/*` 는 추론 서버로 중계한다는 뜻이고,
> 앨범은 게이트웨이가 직접 가진 데이터다. 그래서 여기서는 게이트웨이가 body 를
> 열어 보고 응답도 직접 만든다.

하루를 마치면 동화가 한 편 만들어진다. 그것을 남겼다가 **다음 날에도 다시 읽게** 한다.

### 9.0 누구의 것인가 — `childId`

로그인이 없다. 기기가 처음 켜질 때 만든 **UUIDv4** 하나로 묶는다.

| | 닉네임 | `childId` |
|---|---|---|
| 겹치나 | **겹친다** — 같은 반에 "지우"가 둘이면 앨범이 섞인다 | 겹치지 않는다 |
| 바뀌나 | **바뀐다** — 이름을 고치면 과거 앨범을 잃는다 | 바뀌지 않는다 |

닉네임은 **그때 그 이름**을 표지에 띄우기 위한 표시용으로만 함께 저장한다.

**인증이 없으므로 `childId` 가 곧 열쇠다.** 조회할 때도 반드시 함께 보낸다 —
`id` 만으로 꺼낼 수 있게 두면 번호를 하나씩 올려보는 것만으로 남의 동화가 열린다.

### 9.1 POST /api/v1/albums — 동화 남기기

**앱은 동화를 화면에 띄운 뒤에 부른다.** 저장을 기다렸다 보여주면, 저장이 늦거나
실패하는 날 아이가 동화를 아예 못 본다. 실패해도 화면을 막지 않는다.

```jsonc
{
  "childId": "a355d4f5-f7ac-4b74-8733-b28236b270ac",  // 필수 · UUIDv4
  "nickname": "지우",                                   // 필수 · 40자 이하
  "title": "지우가 들려준 오늘",                          // 필수 · 120자 이하
  "scenes": [                                          // 필수 · 1개 이상
    {
      "category": "school_arrival",   // 필수 · school_arrival|class|lunch|school_departure
      "subtitle": "학교 오는 길",       // 필수 · 120자 이하
      "opening": "아침에",             // 2000자 이하
      "quote": "안녕! 우리 같이 놀자!",  // 500자 이하 · null 가능
      "narration": "지우가 학교에 왔어요." // 필수 · 4000자 이하
    }
  ]
}
```

**삽화는 보내지 않는다.** 앱 번들의 정적 그림이고 `category` 하나로 결정된다.

```jsonc
// 200
{ "success": true, "data": { "id": 12 } }
```

| 에러 | 언제 |
|---|---|
| `400 INVALID_PARAMETER` · `field: childId` | UUID 형식이 아니거나, 한 아이가 500편을 넘겼다 |
| `400 INVALID_PARAMETER` · `field: scenes[0].category` | 알 수 없는 장면 종류 |

### 9.2 GET /api/v1/albums?childId={uuid} — 목록

최신순. 표지를 그리는 데 필요한 것만 준다.

```jsonc
{
  "success": true,
  "data": [
    {
      "id": 12,
      "title": "지우가 들려준 오늘",
      "nickname": "지우",
      "createdAt": "2026-08-27T01:10:51.313624Z",
      "categories": ["school_arrival", "class", "lunch", "school_departure"]
    }
  ]
}
```

`categories` 는 목록에서 **보석 줄**을 그리는 데 쓴다 — 글자를 아직 못 읽는 아이도
색이 다른 보석 네 개를 보고 그날을 알아본다.

### 9.3 GET /api/v1/albums/{id}?childId={uuid} — 한 편

```jsonc
{
  "success": true,
  "data": {
    "id": 12,
    "title": "지우가 들려준 오늘",
    "nickname": "지우",            // 그때 그 이름. 지금 프로필과 다를 수 있다
    "createdAt": "2026-08-27T01:10:51.313624Z",
    "scenes": [ /* 9.1 과 같은 모양 */ ]
  }
}
```

| 에러 | 언제 |
|---|---|
| `404 NOT_FOUND` · `field: id` | 없거나, **`childId` 가 그 동화의 주인이 아니다** |
| `400 INVALID_PARAMETER` · `field: childId` | UUID 형식이 아니다 |

> 주인이 아닐 때 `403` 이 아니라 `404` 를 준다. `403` 은 "있긴 있다"를 알려주는 셈이라,
> 번호를 훑어 남의 동화가 몇 편인지 셀 수 있다.

### 9.4 저장 구조

표는 하나다(`story_album`). 자세한 것은 [`zooearly-erd.md`](./zooearly-erd.md).
