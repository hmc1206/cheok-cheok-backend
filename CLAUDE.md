# Claude Code 작업 인수인계

이 문서는 다른 컴퓨터의 Claude Code에서 현재 작업을 이어가기 위한 상태 기록이다.
작업을 시작하기 전에 루트의 `AGENTS.md`와 `BACKEND_REQUIREMENTS.md`를 먼저 읽고,
두 문서의 규칙과 통합 API 명세 v2.0을 우선한다.

## Git 상태

- 저장소: `https://github.com/hmc1206/cheok-cheok-backend.git`
- 이어서 작업할 브랜치: `feature/be-youtube-search-play`
- 기준 develop 커밋: `579b140` (`feature/be-hospital-pharmacy-tts` 병합본)
- 이 브랜치에서 수정한 코드를 바로 `develop`에 추가 커밋하지 말고, 테스트와 검토 후
  PR 대상으로 `develop`을 사용한다.

## 이번 작업의 목표와 확정한 계약

사용자 발화 `검색해줘`와 `틀어줘`가 같은 결과를 내던 문제를 구분했다.
통합 API v2.0과 기존 프론트 계약을 깨지 않기 위해 intent는 둘 다
`YOUTUBE_PLAY`를 유지하고 `slots.action`으로 나눈다.

### 검색 요청

- 예: `아이유 검색해줘`, `아이유 찾아줘`
- `intent`: `YOUTUBE_PLAY`
- `slots.action`: `SEARCH`
- `step`: `DONE`
- `screen`: `APP_LAUNCH`
- `ttsText`: `아이유 검색 결과를 보여드릴게요.`
- `data`: YouTube 검색 결과용 `app_url`, `web_url`
- 특정 영상을 고르거나 확인 질문을 하지 않고 검색 결과 화면을 바로 연다.

### 재생 요청

- 예: `아이유 틀어줘`, `아이유 재생해줘`
- `intent`: `YOUTUBE_PLAY`
- `slots.action`: `PLAY`
- 첫 응답은 `CONFIRM`, `APP_LAUNCH`
- TTS: `아이유 영상을 열어드릴까요?`
- 첫 실제 YouTube 영상의 미리보기, `app_url`, `web_url`, `네/아니요`를 반환한다.
- 같은 사용자가 `네`라고 답하면 `DONE`과 특정 영상 링크를 반환한다.
- 결과가 없으면 `SEARCH`로 전환해 검색 결과 링크를 반환한다.

## 구현 내용

- `YoutubeIntentHandler`
  - 검색/재생 표현을 구분한다.
  - 명령어를 제거해 정확한 검색어만 슬롯에 저장한다.
  - 검색은 즉시 `DONE`, 재생은 `CONFIRM -> DONE`으로 처리한다.
- `YoutubeApiClient`
  - UUID 기반 Mock 영상 생성을 제거했다.
  - 기존 `YoutubeClient`를 사용해 실제 YouTube Data API 결과 중 첫 영상 ID를 고른다.
- `SecurityConfig`
  - 디버그용 빈 401 응답을 제거했다.
  - 기존 `RestAuthenticationEntryPoint`를 사용해 `error.code`와 `ttsText`가 있는
    공통 JSON 401 응답을 반환한다.
- 오래된 auth/map/youtube 테스트를 현재 OIDC, 지도 링크, 유튜브 계약에 맞췄다.
- 테스트용 설정에 실제 비밀값이 아닌 test 전용 YouTube/Naver 값을 추가했다.
- `BACKEND_REQUIREMENTS.md`에 `SEARCH`와 `PLAY` 응답 차이를 기록했다.

## 검증 결과

- `./gradlew test`: 59개 전체 통과, 실패 0개
- `compileJava`, `compileTestJava`: 통과
- 실제 YouTube API 연결: 성공
- 실제 `아이유` 검색 10개 결과 중 `videoId`가 있는 영상 9개 확인
- `.env`, `application-local.yml` 및 실제 API 키는 Git에 포함되지 않음

## 다음 수동 테스트

백엔드 공통 음성 주소는 README의 오래된 표기와 달리 다음이 정확하다.

```http
POST http://localhost:8080/voice/process
Authorization: Bearer {Access Token}
Content-Type: application/json
```

다음 네 문장을 프론트 마이크와 텍스트 요청으로 각각 확인한다.

1. `아이유 검색해줘`
2. `아이유 찾아줘`
3. `아이유 틀어줘`
4. `아이유 재생해줘`

확인 항목:

- 검색은 `action: SEARCH`, `DONE`, 검색 결과 URL을 반환하는가?
- 재생은 `action: PLAY`, `CONFIRM`, 실제 영상 미리보기를 반환하는가?
- 재생 확인 후 같은 사용자로 `네`를 보내면 특정 영상 URL이 열리는가?
- 검색 TTS와 재생 TTS가 정확히 한 번씩 겹치지 않고 읽히는가?
- `아니요`를 보내면 앱을 열지 않고 `DONE`으로 끝나는가?
- Access Token이 없으면 공통 JSON 401과 TTS 문구가 반환되는가?

## 새 컴퓨터 설정

```powershell
git clone https://github.com/hmc1206/cheok-cheok-backend.git
cd cheok-cheok-backend
git switch feature/be-youtube-search-play
```

현재 컴퓨터의 `.env`와 `src/main/resources/application-local.yml`은 Git에 올리지 말고
암호화된 USB, 비밀번호 관리 도구 또는 안전한 개인 전송 수단으로 따로 복사한다.
필요한 환경 변수 이름은 다음과 같다.

- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`
- `YOUTUBE_API_KEY`
- `OPENAI_API_KEY`
- `KMA_API_AUTH_KEY`
- `NAVER_SEARCH_CLIENT_ID`, `NAVER_SEARCH_CLIENT_SECRET`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`

실행:

```powershell
docker compose up -d
.\gradlew.bat test
.\gradlew.bat bootRun
```

프론트엔드는 별도 저장소에서 실행해야 실제 TTS 소리를 확인할 수 있다. 백엔드는
음성 파일을 만들지 않고 `ttsText`만 반환한다.

## 아직 남은 별도 이슈

- 검색은 현재 앱 내부 영상 목록이 아니라 YouTube 외부 검색 결과를 연다. 앱 내부
  목록이 필요해질 때만 `YOUTUBE_SEARCH` intent와 전용 화면 계약을 추가한다.
- README의 `/api/voice/process` 표기는 실제 `/voice/process`와 다르다.
- 문서의 `/api/v1/youtube/link`와 구현의 `/api/youtube/link` 경로가 다르다.
- 일반 지도는 현재 위치 문자열 처리와 `MAP_RESULT`/`NAVER_MAP_VIEW` 계약 차이를
  별도 작업으로 해결해야 한다.
- Docker Desktop과 Redis, 백엔드, 프론트 서버는 마지막 점검 시 실행 중이 아니었다.

## Claude Code에 보낼 첫 요청

```text
루트의 AGENTS.md, BACKEND_REQUIREMENTS.md, CLAUDE.md를 모두 읽고 현재 브랜치와
git status를 확인해줘. 기존 구현을 다시 만들지 말고, 먼저 ./gradlew test를 실행한
뒤 CLAUDE.md의 수동 유튜브 SEARCH/PLAY 테스트부터 이어서 진행해줘. 비밀값은
출력하거나 커밋하지 마.
```
