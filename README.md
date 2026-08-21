# 척척 (Cheok-Cheok) — 시니어 음성 생활 도우미 백엔드

어르신의 음성/텍스트 요청을 의도(Intent)로 분류하고, 기능별 핸들러로 라우팅해 앱 실행 링크 또는 단계별 안내 응답을 반환하는 Spring Boot 백엔드입니다. 프론트엔드는 평상시 `POST /voice/process` 한 곳만 호출하는 구조를 전제로 합니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어/프레임워크 | Java 21, Spring Boot 3.5 |
| 보안/인증 | Spring Security, OAuth2 Client(Google), OAuth2 Resource Server(JWT) |
| 데이터 | Spring Data JPA, MySQL(운영) / H2(개발), Spring Data Redis |
| 외부 연동 | OpenAI(STT·의도분류), 기상청 API허브(날씨), 네이버 검색/지도 API, YouTube Data API |
| 빌드 | Gradle |

## 아키텍처

```
클라이언트 → POST /voice/process
           → VoiceService.process()
             1) 요청 검증 (userId, text/audio 중 하나, 좌표 범위)
             2) 음성이면 SpeechTranscriber(OpenAI STT)로 텍스트 변환
             3) IntentClassifier(OpenAI)로 발화를 Intent로 분류
             4) SessionService(Redis)에서 기존 멀티턴 세션 조회/이어가기 판단
             5) IntentRouter가 Intent별 IntentHandler로 라우팅
             6) 응답이 DONE이면 세션 삭제, 아니면 TTL 10분으로 세션 갱신
```

- **의도 기반 라우팅**: `Intent` enum(`YOUTUBE_SEARCH`, `YOUTUBE_PLAY`, `WEATHER_INFO`, `MEDICAL_ROUTE`, `MAP_ROUTE`, `KIOSK_HELP`, `UNKNOWN`)마다 `IntentHandler` 구현체가 매핑되며, `IntentRouter`가 Spring이 주입한 핸들러 목록으로 `EnumMap`을 구성합니다(중복 매핑 시 기동 실패).
- **멀티턴 세션**: Redis에 `voice:session:{userId}` 키로 현재 진행 중인 `Intent`/`step`/`slots`를 저장(TTL 10분). 새 발화가 `UNKNOWN`으로 분류되면 후속 답변으로 간주해 기존 세션을 이어가고, 명확한 새 의도면 기존 세션을 버리고 새로 시작합니다.
- **인증**: Google OAuth2 로그인(`CustomOAuth2UserService` → `OAuth2LoginSuccessHandler`) 성공 시 자체 Access/Refresh JWT(HS256)를 발급합니다. Access Token은 Bearer 헤더로, Resource Server(`JwtTokenService.accessTokenDecoder()`)가 요청마다 검증합니다. `POST /api/auth/refresh`는 인증 없이 허용됩니다.
- **보안 정책**: STATELESS 세션, CSRF 비활성화, CORS는 `http://localhost:5173`만 허용(운영 도메인 반영 필요). `/`, `/error`, `/oauth2/**`, `/login/**`, `/api/auth/refresh`, `/api/v1/routes/**`를 제외한 모든 요청은 인증이 필요합니다.
- **공통 에러 처리**: `GlobalExceptionHandler`가 `ApiException`/`ErrorCode`를 `{ success, error: { code, message }, ttsText }` 형태의 공통 에러 응답으로 변환합니다.

## 패키지 구조

```
com.chuckchuck/
├── auth/           # OAuth2 로그인, JWT 발급/검증, 사용자 조회
│   ├── jwt/
│   ├── oauth/
│   └── user/
├── common/exception/  # ApiException, ErrorCode, 전역 예외 처리
├── hospital/       # 병원·약국 검색(네이버 Search API)
├── kiosk/          # 키오스크 시나리오 진행(세션 기반 단계 안내)
├── map/            # 길찾기 링크 생성(네이버 지도)
├── session/         # Redis 기반 대화 세션 상태
├── voice/          # 음성 처리 진입점: 분류·라우팅·핸들러 계약
├── weather/        # 기상청 단기예보 연동
└── youtube/        # 유튜브 재생/검색/딥링크 생성
```

## API 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/voice/process` | 통합 음성/버튼 대화 진입점 | 필요 |
| POST | `/api/auth/refresh` | Refresh Token으로 Access Token 재발급 | 불필요 |
| GET | `/api/users/me` | 로그인 사용자 정보 조회 | 필요 |
| POST | `/api/v1/routes/naver-link` | 길찾기 링크 단독 생성(디버깅용) | 불필요 |
| POST | `/api/map/hospital` | 병원·약국 검색 | 필요 |
| GET | `/api/v1/weather` | 날씨 조회 | 필요 |
| POST | `/api/youtube/play` | 유튜브 재생 링크 생성 | 필요 |
| GET | `/api/youtube/link` | 유튜브 링크 단독 조회 | 필요 |
| POST | `/api/youtube/search` | 유튜브 검색 | 필요 |
| GET | `/api/kiosk/scenarios` | 키오스크 시나리오 목록 | 필요 |
| POST | `/api/kiosk/scenarios/{scenarioId}/start` | 키오스크 안내 시작 | 필요 |
| POST | `/api/kiosk/scenarios/{scenarioId}/action` | 키오스크 단계 진행 | 필요 |

## 시작하기

### 사전 준비

- Java 21, Docker(Redis용)
- MySQL(운영) 또는 기본 H2(개발)

### 1. Redis 실행

```bash
docker compose up -d
```

### 2. 환경 변수 설정

`.env.example`을 복사해 `.env`를 만들고 값을 채웁니다.

```bash
cp .env.example .env
```

| 변수 | 설명 |
| --- | --- |
| `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_TRANSCRIPTION_MODEL`, `OPENAI_INTENT_MODEL` | STT·의도분류용 OpenAI 설정 |
| `KMA_API_AUTH_KEY` | 기상청 API허브 단기예보 인증키 |
| `NAVER_SEARCH_CLIENT_ID`, `NAVER_SEARCH_CLIENT_SECRET` | 네이버 Search API 인증 정보 |
| `REDIS_HOST`, `REDIS_PORT` | Redis 접속 정보 |

`application.yml`에서 추가로 참조하는 값(`.env`에 없다면 `application-local.example.yml` 참고): `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`(32바이트 이상), `YOUTUBE_API_KEY`, `SERVER_PORT`(기본 8080), `FRONTEND_CALLBACK_URL`(기본 `http://localhost:5173/auth/callback`).

### 3. 실행

```bash
./gradlew bootRun
```

### 4. 테스트

```bash
./gradlew test
```

## 참고 문서

- `BACKEND_REQUIREMENTS.md`: 백엔드 요구사항 정의
- `cheok-cheok-weather.postman_collection.json`: 날씨 API Postman 컬렉션

## 알려진 주의사항

- `SecurityConfig`의 CORS 허용 origin이 `http://localhost:5173`로 고정되어 있어 운영 배포 시 프론트 도메인 반영이 필요합니다.
- `JwtTokenService.requireRefreshSubject`에 디버깅용 `System.out.println` 로그가 남아 있어 운영 배포 전 정리가 필요합니다.
- Refresh Token 쿠키의 `secure-cookie` 설정이 `false`로 고정되어 있어, HTTPS 운영 환경에서는 `true`로 전환이 필요합니다.
