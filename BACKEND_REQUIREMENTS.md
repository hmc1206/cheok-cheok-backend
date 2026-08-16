# 척척 통합 API 명세 및 백엔드 요구사항

> 버전: v2.0 (통합본)
> 최종 수정: 2026-08-08

이 문서가 최신 API 계약이다. 기존 `API 상세 명세서`, `유튜브 외부 앱 실행 연동
API 명세서`, `네이버 지도 통합 경로 연동 API 명세서`는 하위 보관 문서로만
취급한다. 기존 문서나 구현이 이 문서와 충돌하면 v2.0을 우선하고, 계약 변경이
필요하면 프론트엔드와 먼저 합의한다.

## 0. 문서 구조

서비스 API는 두 층으로 구성한다.

| 층 | 역할 |
| --- | --- |
| 1. `POST /voice/process` | 프론트가 호출하는 유일한 대화 진입점. 음성 또는 버튼 입력을 받아 의도를 파악하고 다음 질문이나 실행 정보를 반환한다. |
| 2. 기능별 직접 API | 1번이 내부에서 사용하는 실무 로직. 유튜브 딥링크는 `GET /api/v1/youtube/link`, 네이버지도 경로는 `POST /api/v1/routes/naver-link`, 날씨 조회는 `GET /api/v1/weather`가 담당한다. QA와 디버깅을 위해 독립 엔드포인트로도 공개한다. |

프론트엔드는 평상시 1번만 호출한다. 기능별 링크는 `/voice/process` 응답의
`data`에 같은 필드명으로 포함되므로, 직접 테스트하거나 링크만 다시 만들 때만
2번 API를 호출한다.

## 1. 공통 사항

| 항목 | 내용 |
| --- | --- |
| Base URL | `https://api.chuckchuck.com` |
| 프로토콜 | HTTPS |
| 인증 방식 | Google 로그인 후 발급한 JWT Bearer Token |
| 공통 헤더 | `Authorization: Bearer {token}`, `Content-Type: application/json` |
| 인코딩 | UTF-8. 모든 URL 파라미터는 UTF-8로 URL 인코딩한다. |
| 날짜/시간 형식 | ISO 8601, 예: `2026-08-02T14:00:00+09:00` |

인증된 요청의 사용자 ID는 JWT에서 가져온다. 본문의 `userId`는 호환 목적으로
받을 수 있지만 JWT 사용자와 다르면 `401 UNAUTHORIZED`로 거부한다.

### 실행 방식 원칙

| 기능 | 실행 방식 |
| --- | --- |
| 유튜브 | 인앱 재생이 아니라 유튜브 앱 또는 웹으로 이동하는 외부 앱 딥링크를 실행한다. |
| 지도 | 네이버 지도 딥링크 또는 SDK 임베드를 사용한다. 경로 계산은 네이버지도가 담당하고 서버는 좌표와 링크를 생성한다. |
| 키오스크 | 사양 미정이다. 화면 흐름 확정 후 별도 API를 추가한다. |

## 2. 대화 공통 진입점

### `POST /voice/process`

모든 사용자 입력의 공통 진입점이다. 음성 녹음 또는 `quickReplies` 버튼 값을
받아 STT, 의도 분류, 세션 처리와 도메인 실행을 수행한다.

### 요청 본문

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `userId` | string | Y | JWT에서 추출되는 사용자 ID |
| `audio` | string(base64)/null | N | 음성 입력의 녹음 원본. 프론트는 STT를 하지 않고 그대로 전달한다. |
| `text` | string/null | N | `quickReplies` 버튼을 탭한 경우 해당 버튼의 `value` |

`audio`와 `text` 중 하나는 반드시 있어야 한다. 둘 다 없으면
`400 INVALID_REQUEST`를 반환한다. 문서 예시는 이해를 위해 인식된 문장을
`text`로 보여주지만, 실제 음성 입력은 `audio`를 사용한다.

```json
{
  "userId": "u123",
  "audio": null,
  "text": "내일 서울 날씨 알려줘"
}
```

### 성공 응답 공통 봉투

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `intent` | string | Y | `YOUTUBE_PLAY`, `WEATHER_INFO`, `MEDICAL_ROUTE`, `MAP_ROUTE`, `KIOSK_HELP`, `UNKNOWN` 중 하나 |
| `step` | string | Y | 대화 진행 단계. 단발성 기능은 `DONE` |
| `slots` | object | Y | 현재까지 수집한 누적 파라미터 |
| `ttsText` | string | Y | 프론트가 즉시 TTS로 읽을 문장 |
| `screen` | string | Y | 프론트 화면 라우팅 코드 |
| `quickReplies` | array/null | N | 큰 버튼 선택지. 각 항목은 `label`과 `value`를 가진다. |
| `data` | object/null | N | 도메인별 실행 정보 또는 결과 |

어르신 사용자는 음성 재입력보다 큰 버튼 선택을 우선한다. 프론트는
`quickReplies[].label`을 버튼에 표시하고, 버튼을 누르면 `value`를 다음 요청의
`text`에 담아 같은 `/voice/process`를 호출한다. 백엔드는 버튼 입력과 음성
응답을 같은 대화 입력으로 처리한다.

### 처리 순서

1. Redis에서 JWT 사용자 ID 기준 기존 세션을 조회한다.
2. `audio`가 있으면 STT를 수행하고, `text`가 있으면 그대로 사용한다.
3. 신규 대화면 LLM으로 intent를 분류하고, 기존 대화면 저장된 `step`과
   `slots`를 이어서 처리한다.
4. 도메인별 `IntentHandler`가 다음 질문 또는 실행 정보를 결정한다. 필요하면
   기능별 링크 생성 로직을 내부 호출한다.
5. 진행 중인 대화는 Redis 세션을 갱신하고, `DONE`이면 세션을 삭제한다.

### 공통 오류 응답

```json
{
  "success": false,
  "error": {
    "code": "SESSION_EXPIRED",
    "message": "세션이 만료되었습니다."
  },
  "ttsText": "죄송해요, 다시 한번 말씀해주시겠어요?"
}
```

전체 오류 코드는 [9. 오류 코드](#9-오류-코드)를 따른다.

## 3. 유튜브

### `POST /voice/process` (`YOUTUBE_PLAY`)

우리 앱에서 영상을 재생하지 않고 유튜브 앱으로 이동한다. 유튜브 앱이 없으면
웹 URL로 대체한다. 특정 영상이 검색되면 실행 전 미리보기와 확인 버튼을 먼저
보여준다.

### 1단계: 실행 확인 (`CONFIRM`)

```json
{
  "intent": "YOUTUBE_PLAY",
  "step": "CONFIRM",
  "slots": { "query": "미스트롯" },
  "ttsText": "미스트롯 영상을 열어드릴까요?",
  "screen": "APP_LAUNCH",
  "quickReplies": [
    { "label": "네, 열어줘", "value": "네" },
    { "label": "아니요", "value": "아니요" }
  ],
  "data": {
    "title": "미스트롯 시즌1 하이라이트",
    "thumbnailUrl": "https://i.ytimg.com/vi/abcd1234/hqdefault.jpg",
    "channelName": "TV조선",
    "app_url": "vnd.youtube://www.youtube.com/watch?v=abcd1234",
    "web_url": "https://www.youtube.com/watch?v=abcd1234"
  }
}
```

### 2단계: 사용자 동의 후 실행 (`DONE`)

```json
{
  "intent": "YOUTUBE_PLAY",
  "step": "DONE",
  "slots": { "query": "미스트롯" },
  "ttsText": "유튜브를 열어드릴게요.",
  "screen": "APP_LAUNCH",
  "quickReplies": null,
  "data": {
    "app_url": "vnd.youtube://www.youtube.com/watch?v=abcd1234",
    "web_url": "https://www.youtube.com/watch?v=abcd1234"
  }
}
```

| `data` 필드 | 타입 | 설명 |
| --- | --- | --- |
| `title` | string | 확인 카드의 영상 제목. `CONFIRM`에서만 사용한다. |
| `thumbnailUrl` | string | 확인 카드의 썸네일. `CONFIRM`에서만 사용한다. |
| `channelName` | string | 확인 카드의 채널명. `CONFIRM`에서만 사용한다. |
| `app_url` | string | 유튜브 앱 실행용 딥링크 |
| `web_url` | string | 앱 미설치 시 사용할 웹 URL |

검색 결과만 있고 특정 영상을 선택하지 않으면 미리보기 없이 `DONE`을 반환한다.
이때 `app_url`은 `vnd.youtube://www.youtube.com/results?search_query=...` 형태다.
앱 실행 이후 재생, 일시정지, 다음 영상 같은 인앱 제어는 서버가 담당하지 않는다.

## 4. 지도 및 길찾기

### `POST /voice/process` (`MAP_ROUTE`)

상태 흐름은 `ASK_ORIGIN -> DONE`이다. 서버는 출발지와 도착지를 Geocoding해
명칭과 좌표를 확정하고 네이버 지도 딥링크를 만든다. 실제 경로 계산과 안내는
네이버지도가 담당한다.

### 1턴: 목적지만 입력된 경우

```json
{
  "userId": "u123",
  "text": "아들 집 가는 길 알려줘"
}
```

```json
{
  "intent": "MAP_ROUTE",
  "step": "ASK_ORIGIN",
  "slots": { "destinationAlias": "아들집" },
  "ttsText": "지금 계신 곳에서 출발할까요?",
  "screen": "MAP_INPUT",
  "quickReplies": [
    { "label": "네, 여기서 출발", "value": "네" },
    { "label": "다른 곳에서 출발", "value": "다른 곳" }
  ],
  "data": null
}
```

`아들집` 같은 별칭은 사전에 등록한 주소로 매핑한다. 등록되지 않은 별칭이면
주소를 확인하는 추가 질문을 반환한다.

### 2턴: 경로 확정

```json
{
  "intent": "MAP_ROUTE",
  "step": "DONE",
  "slots": {
    "origin": "현재위치",
    "destination": "경기도 성남시 ..."
  },
  "ttsText": "아드님 댁까지 가는 길을 지도에서 보여드릴게요.",
  "screen": "NAVER_MAP_VIEW",
  "quickReplies": null,
  "data": {
    "resolvedStart": {
      "name": "현재위치",
      "lat": 37.5665,
      "lng": 126.9780
    },
    "resolvedGoal": {
      "name": "경기도 성남시 ...",
      "lat": 37.4201,
      "lng": 127.1262
    },
    "naverMapAppUrl": "nmap://route/public?slat=37.5665&slng=126.9780&sname=%ED%98%84%EC%9E%AC%EC%9C%84%EC%B9%98&dlat=37.4201&dlng=127.1262&dname=%EC%95%84%EB%93%A4%EC%A7%91&appname=com.chuckchuck.app",
    "naverMapWebUrl": "https://map.naver.com/p/directions/..."
  }
}
```

| `data` 필드 | 타입 | 설명 |
| --- | --- | --- |
| `resolvedStart` | object | Geocoding으로 확정한 출발지 명칭, 위도와 경도 |
| `resolvedGoal` | object | Geocoding으로 확정한 도착지 명칭, 위도와 경도 |
| `naverMapAppUrl` | string | 네이버 지도 앱 실행용 `nmap://` 딥링크 |
| `naverMapWebUrl` | string | 앱 미설치 시 사용할 네이버 지도 웹 URL |

좌표를 확정하지 못하면 `404 GEOCODE_NOT_FOUND`를 반환하거나
`screen: MAP_NOT_FOUND`로 안내한다.

## 5. 키오스크 사용 안내

키오스크 화면 흐름과 API는 아직 확정되지 않았다. 사양이 확정되면 별도 API를
추가한다.

추후 검토할 오류 상황:

- 카메라 인식 실패 시 재시도 안내
- 매장별 UI 차이 대응
- 결제 실패와 입력 시간 초과 처리

## 6. 인증

### `GET /oauth2/authorization/google`

프론트의 Google 로그인 버튼은 이 URL로 이동한다. Google 동의 화면과 백엔드
콜백 처리가 끝나면 JWT를 발급하고 프론트로 리다이렉트한다.

```text
https://chuckchuck.com/auth/callback?token=eyJhbGciOi...&isNewUser=true
```

| 파라미터 | 설명 |
| --- | --- |
| `token` | 이후 API의 `Authorization` 헤더에 사용할 Access Token |
| `isNewUser` | 온보딩 화면 분기에 사용할 신규 가입 여부 |

### `GET /api/users/me`

```json
{
  "userId": "u123",
  "name": "홍길동",
  "email": "hong@gmail.com",
  "profileImageUrl": "https://..."
}
```

### `POST /auth/refresh`

HttpOnly 쿠키의 Refresh Token을 검증하고 새 Access Token을 반환한다.

```json
{
  "token": "새로운 JWT"
}
```

## 7. 기능별 링크 생성 API

`/voice/process`가 내부에서 사용하는 로직이다. QA, 디버깅 또는 링크 단독
재생성이 필요할 때 직접 호출한다.

### 7.1 `GET /api/v1/youtube/link`

| 항목 | 내용 |
| --- | --- |
| Query Parameter | `keyword` (string, 선택). 없으면 유튜브 홈 링크를 반환한다. |

```http
GET /api/v1/youtube/link?keyword=%EC%95%84%EC%9D%B4%EC%9C%A0
```

```json
{
  "success": true,
  "data": {
    "web_url": "https://www.youtube.com/results?search_query=%EC%95%84%EC%9D%B4%EC%9C%A0",
    "app_url": "vnd.youtube://www.youtube.com/results?search_query=%EC%95%84%EC%9D%B4%EC%9C%A0"
  }
}
```

### 7.2 `POST /api/v1/routes/naver-link`

```json
{
  "startName": "서울역",
  "goalName": "부산역"
}
```

```json
{
  "success": true,
  "data": {
    "naverMapAppUrl": "nmap://route/public?slat=37.5547&slng=126.9707&sname=%EC%84%9C%EC%9A%B8%EC%97%AD&dlat=35.1152&dlng=129.0416&dname=%EB%B6%80%EC%82%B0%EC%97%AD&appname=com.chuckchuck.app",
    "naverMapWebUrl": "https://map.naver.com/p/directions/...",
    "resolvedStart": {
      "name": "서울역",
      "lat": 37.5547,
      "lng": 126.9707
    },
    "resolvedGoal": {
      "name": "부산역",
      "lat": 35.1152,
      "lng": 129.0416
    }
  }
}
```

처리 순서:

1. 출발지와 도착지를 Geocoding해 좌표를 얻는다.
2. `nmap://route/public?...` 딥링크를 조합한다.
3. 명칭과 URL 파라미터를 UTF-8로 URL 인코딩한다.
4. 앱 URL과 웹 Fallback URL을 함께 반환한다.

`appname`은 네이버 개발자센터에 등록한 호출 앱 식별자를 사용한다.

## 8. 오류 코드

| HTTP | 오류 코드 | 발생 시점 | 대응 |
| --- | --- | --- | --- |
| 400 | `INVALID_REQUEST` | 필수 파라미터 누락 또는 형식 오류 | 오류 메시지를 표시한다. |
| 401 | `UNAUTHORIZED` | 토큰 없음 또는 만료 | 재로그인을 유도한다. |
| 404 | `RESOURCE_NOT_FOUND` | 요청 리소스 없음 | 리소스가 없음을 안내한다. |
| 409 | `SESSION_EXPIRED` | 10분간 미활동으로 대화 세션 만료 | 다시 말하도록 안내한다. |
| 422 | `INTENT_NOT_FOUND` | 의도 파악 실패 | 재질문한다. |
| 422 | `STT_EMPTY_INPUT` | 음성 인식 결과가 공백 또는 무음 | 잘 듣지 못했다는 TTS를 재생한다. |
| 429 | `MAX_RETRY_EXCEEDED` | 같은 요청을 3회 이상 재질문 | 터치 입력 전환 또는 보호자 연락을 안내한다. |
| 404 | `APP_NOT_INSTALLED` | 대상 앱 미설치 | `web_url`로 전환한다. |
| 403 | `APP_PERMISSION_DENIED` | 앱 권한 거부 | 권한 허용 방법을 안내한다. |
| 403 | `UNSAFE_APP_BLOCKED` | 화이트리스트 밖 앱 또는 사이트 요청 | 연결을 차단한다. |
| 404 | `GEOCODE_NOT_FOUND` | 지역명 좌표를 찾지 못함 | 지역명 재입력을 요청한다. |
| 502 | `GEOCODE_API_FAIL` | Geocoding 외부 API 실패 | 외부 API 오류를 안내한다. |
| 502 | `EXTERNAL_API_FAIL` | 유튜브 또는 네이버지도 API 실패 | 외부 API 오류를 안내한다. |
| 409 | `SESSION_INTERRUPTED` | 단계 안내 중 다른 intent 수신 | 기존 작업 중단 여부를 확인한다. |
| 422 | `KIOSK_VISION_FAIL` | 키오스크 카메라 인식 실패 | 사양 확정 후 적용한다. |
| 408 | `KIOSK_ACTION_TIMEOUT` | 키오스크 입력 시간 초과 | 사양 확정 후 적용한다. |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 | 일반 오류 문구를 안내한다. |

## 9. 프론트엔드 처리 가이드

### 9.1 `quickReplies`

서버가 질문을 반환하면 프론트는 `ttsText`를 읽는 동시에 `quickReplies`를 큰
버튼으로 표시한다. 사용자가 버튼을 누르면 해당 버튼의 `value`를 `text`에 담아
`/voice/process`를 다시 호출한다.

### 9.2 딥링크와 웹 Fallback

1. `data.app_url` 또는 `data.naverMapAppUrl` 실행을 시도한다.
2. `canOpenURL` 또는 `resolveActivity`로 실행 가능 여부를 확인한다.
3. 앱을 열 수 없거나 약 1~1.5초 안에 전환되지 않으면 `web_url` 또는
   `naverMapWebUrl`을 연다.

#### iOS (Swift)

```swift
func openDeepLink(appUrl: String, webUrl: String) {
    if let url = URL(string: appUrl), UIApplication.shared.canOpenURL(url) {
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
    } else if let fallback = URL(string: webUrl) {
        UIApplication.shared.open(fallback, options: [:], completionHandler: nil)
    }
}
```

#### Android (Kotlin)

```kotlin
fun openDeepLink(context: Context, appUrl: String, webUrl: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appUrl))
    try {
        context.startActivity(appIntent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
    }
}
```

### 9.3 모바일 앱 스키마 화이트리스트

#### iOS `Info.plist`

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>youtube</string>
    <string>vnd.youtube</string>
    <string>nmap</string>
</array>
```

#### Android `AndroidManifest.xml`

```xml
<queries>
    <package android:name="com.google.android.youtube" />
    <package android:name="com.nhn.android.nmap" />
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="vnd.youtube" />
    </intent>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="nmap" />
    </intent>
</queries>
```

이 설정이 없으면 Android 11(API 30) 이상에서 앱이 설치되어 있어도
`resolveActivity`가 `null`을 반환해 웹 Fallback만 실행될 수 있다.

## 10. 용어

| 용어 | 설명 |
| --- | --- |
| Intent | 사용자의 발화가 무엇을 원하는지 분류한 값 |
| Slot | 대화를 통해 채우는 파라미터 |
| Step | 멀티턴 대화의 현재 진행 상태 |
| `quickReplies` | 음성 재입력 대신 누를 수 있는 큰 버튼 선택지 |
| 딥링크 | 앱의 특정 화면을 여는 URL 스킴. 예: `vnd.youtube://`, `nmap://` |
| Geocoding | 지명 또는 주소를 위도와 경도로 변환하는 과정 |
| Redis 세션 | 대화 중간 상태를 저장하는 임시 저장소. 10분 미활동 시 삭제한다. |
| STT / TTS | 음성을 텍스트로 변환 / 텍스트를 음성으로 변환 |

## 11. 다음 단계

- [ ] 유튜브 딥링크를 Android와 iOS 실기기에서 테스트
- [ ] 네이버 지도 `appname`을 개발자센터에 등록하고 실기기에서 테스트
- [ ] Geocoding 결과 캐싱 정책 확정. Redis TTL 1일 제안
- [ ] 키오스크 화면 흐름 확정 후 API 설계 추가
- [ ] Postman 컬렉션 작성 및 공유
- [ ] Swagger/OpenAPI 자동 문서화 연동

## 12. 완료 기준

- 요청과 응답이 v2.0 계약을 따른다.
- `/voice/process`가 음성 입력과 `quickReplies` 버튼 입력을 같은 흐름으로 처리한다.
- `quickReplies` 문구와 `ttsText`는 어르신이 이해하기 쉬운 짧은 존댓말을 사용한다.
- 유튜브와 네이버지도 응답에 앱 URL과 웹 Fallback URL을 함께 제공한다.
- Redis 세션은 10분 TTL을 적용하고 `DONE`에서 삭제한다.
- 정상 흐름과 주요 오류 흐름 테스트를 작성한다.
- `./gradlew test`가 통과한다.
- 실제 비밀값과 개인정보를 코드, Git 또는 로그에 남기지 않는다.

## 13. 날씨 API

> 버전: v1.1
> 추가일: 2026-08-16
> 최종 수정: 2026-08-17
> 외부 제공업체: 기상청 API허브 동네예보 격자자료 / Open-Meteo Geocoding API

사용자가 지역과 날짜를 말하면 날씨를 조회하고, 어르신이 이해하기 쉬운 문장으로 안내한다.

지원 발화 예시:

- "오늘 서울 날씨 알려줘"
- "내일 부산에 비 와?"
- "지금 있는 곳 날씨 알려줘"
- "오늘 우산 가져가야 해?"

### 13.1 음성 통합 API

#### `POST /voice/process` (`WEATHER_INFO`)

기존 음성 요청에 현재 위치 좌표를 선택 필드로 추가한다. `latitude`와 `longitude`는
둘 다 보내거나 둘 다 생략해야 한다.

```json
{
  "userId": "u123",
  "text": "내일 서울 날씨 알려줘",
  "audio": null,
  "latitude": null,
  "longitude": null
}
```

현재 위치 요청 예시:

```json
{
  "userId": "u123",
  "text": "지금 있는 곳 날씨 알려줘",
  "audio": null,
  "latitude": 37.5665,
  "longitude": 126.978
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `userId` | string | Y | JWT 사용자와 일치해야 하는 사용자 ID |
| `text` | string/null | 조건부 | 텍스트 또는 큰 버튼 입력 |
| `audio` | string/null | 조건부 | Base64 음성 원본 |
| `latitude` | number/null | N | 현재 위치 위도, -90~90 |
| `longitude` | number/null | N | 현재 위치 경도, -180~180 |

`text`와 `audio` 중 하나는 반드시 있어야 한다.

#### 지역 확인 (`ASK_LOCATION`)

발화에 지역이 없고 현재 위치 좌표도 없으면 지역을 다시 질문한다.

```json
{
  "intent": "WEATHER_INFO",
  "step": "ASK_LOCATION",
  "slots": {
    "forecastDate": "2026-08-17"
  },
  "ttsText": "어느 지역의 날씨를 알려드릴까요?",
  "screen": "WEATHER_INPUT",
  "quickReplies": [
    { "label": "현재 위치 날씨", "value": "현재 위치" },
    { "label": "지역 직접 말하기", "value": "지역 직접 입력" }
  ],
  "data": null
}
```

`현재 위치 날씨` 버튼을 누르면 프론트가 위치 권한을 요청하고 다음
`/voice/process` 요청에 위도와 경도를 포함한다.

#### 날씨 조회 완료 (`DONE`)

```json
{
  "intent": "WEATHER_INFO",
  "step": "DONE",
  "slots": {
    "location": "서울특별시",
    "forecastDate": "2026-08-17"
  },
  "ttsText": "내일 서울특별시 날씨는 비예요. 최고 29도, 최저 24도예요. 비나 눈이 올 확률은 70퍼센트예요. 외출하실 때 우산을 챙기세요.",
  "screen": "WEATHER_RESULT",
  "quickReplies": null,
  "data": {
    "location": {
      "name": "서울특별시",
      "latitude": 37.5665,
      "longitude": 126.978
    },
    "forecastDate": "2026-08-17",
    "updatedAt": "2026-08-16T15:00:00+09:00",
    "conditionCode": "RAIN",
    "conditionText": "비",
    "currentTemperature": null,
    "feelsLikeTemperature": null,
    "minimumTemperature": 24.0,
    "maximumTemperature": 29.0,
    "precipitationProbability": 70,
    "humidity": null,
    "windSpeed": 2.4,
    "umbrellaRecommended": true,
    "advice": "외출하실 때 우산을 챙기세요."
  }
}
```

예보 날짜에는 `currentTemperature`, `feelsLikeTemperature`, `humidity`가 `null`일 수 있다.

### 13.2 직접 조회 API

#### `GET /api/v1/weather`

음성 처리 없이 날씨 기능만 테스트할 때 사용한다. JWT 인증이 필요하다.

| Query Parameter | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `location` | string | 조건부 | 조회할 대한민국 지역명 |
| `latitude` | number | 조건부 | 현재 위치 위도 |
| `longitude` | number | 조건부 | 현재 위치 경도 |
| `date` | string | N | `YYYY-MM-DD`, 생략하면 오늘 |

지역명 또는 위도와 경도 중 하나를 전달한다. 기상청 단기예보 제공 범위에 맞춰 오늘부터 5일 뒤까지 조회할 수 있다.

```http
GET /api/v1/weather?location=서울&date=2026-08-17
```

```json
{
  "success": true,
  "data": {
    "location": {
      "name": "서울특별시",
      "latitude": 37.5665,
      "longitude": 126.978
    },
    "forecastDate": "2026-08-17",
    "conditionCode": "RAIN",
    "conditionText": "비",
    "minimumTemperature": 24.0,
    "maximumTemperature": 29.0,
    "precipitationProbability": 70,
    "umbrellaRecommended": true,
    "advice": "외출하실 때 우산을 챙기세요."
  }
}
```

### 13.3 날씨 상태 코드

| 코드 | 표시 문구 |
| --- | --- |
| `CLEAR` | 맑음 |
| `PARTLY_CLOUDY` | 구름 조금 |
| `CLOUDY` | 흐림 |
| `FOG` | 안개 |
| `RAIN` | 비 |
| `SHOWER` | 소나기 |
| `SNOW` | 눈 |
| `UNKNOWN` | 날씨 정보 확인 불가 |

기상청의 하늘상태(`SKY`)와 강수형태(`PTY`) 코드는 백엔드에서 위 공통 코드로 변환한다.

### 13.4 오류 코드

| HTTP | 오류 코드 | 발생 조건 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 좌표 쌍 누락, 좌표 범위 또는 날짜 형식 오류 |
| 400 | `WEATHER_DATE_NOT_SUPPORTED` | 조회 가능한 예보 기간을 벗어남 |
| 401 | `UNAUTHORIZED` | JWT가 없거나 유효하지 않음 |
| 404 | `WEATHER_LOCATION_NOT_FOUND` | 지역명 또는 좌표가 없거나 지역 검색 실패 |
| 404 | `WEATHER_DATA_NOT_FOUND` | 해당 날짜의 예보 자료 없음 |
| 502 | `WEATHER_API_FAIL` | 외부 날씨 또는 지역 검색 API 호출 실패 |

### 13.5 프론트엔드 처리

1. `WEATHER_INFO`를 `/weather` 화면으로 연결한다.
2. `ttsText`를 즉시 읽고 큰 글자로 함께 표시한다.
3. `conditionCode`에 맞는 날씨 아이콘을 표시한다.
4. `umbrellaRecommended`가 `true`면 우산 안내를 강조한다.
5. 위치 권한이 거부되면 지역 이름을 직접 말하도록 안내한다.

### 13.6 완료 기준

- [ ] `WEATHER_INFO` 의도 분류가 동작한다.
- [ ] 오늘, 내일, 모레, 지역명, 현재 위치 요청을 처리한다.
- [ ] 지역이 없으면 `ASK_LOCATION`을 반환한다.
- [ ] 날씨 조회 성공 시 `WEATHER_RESULT`를 반환한다.
- [ ] 외부 날씨 코드를 공통 날씨 코드로 변환한다.
- [ ] API 키와 위치정보를 코드 또는 로그에 남기지 않는다.
- [ ] 정상 흐름과 주요 실패 흐름 테스트가 통과한다.

## 14. 병원·약국 검색 API

### 14.1 음성 통합 API

#### `POST /voice/process` (`MEDICAL_ROUTE`)

현재 위치 주변의 병원 또는 약국을 검색하고 네이버 지도 앱·웹 URL과 TTS 문장을 반환한다.
요청의 `latitude`와 `longitude`는 반드시 함께 전달한다.

좌표가 없으면 다음과 같이 위치 권한을 안내한다.

```json
{
  "intent": "MEDICAL_ROUTE",
  "step": "ASK_LOCATION",
  "slots": { "type": "PHARMACY" },
  "ttsText": "가까운 약국을 찾으려면 현재 위치가 필요해요. 위치 권한을 허용해 주세요.",
  "screen": "MEDICAL_INPUT",
  "quickReplies": [
    { "label": "현재 위치 사용", "value": "현재 위치" }
  ],
  "data": null
}
```

검색이 완료되면 `step: DONE`, `screen: NAVER_MAP_VIEW`로 응답한다.

```json
{
  "intent": "MEDICAL_ROUTE",
  "step": "DONE",
  "slots": { "type": "PHARMACY" },
  "ttsText": "주변 약국 검색 결과를 찾았어요. 목적지는 새봄약국입니다. 네이버 지도를 열게요.",
  "screen": "NAVER_MAP_VIEW",
  "quickReplies": null,
  "data": {
    "naverMapAppUrl": "nmap://route/public?...",
    "naverMapWebUrl": "https://map.naver.com/p/directions/..."
  }
}
```

### 14.2 직접 조회 API

#### `POST /api/map/hospital`

```json
{
  "userId": "u123",
  "text": "가까운 약국 찾아줘",
  "type": "PHARMACY",
  "latitude": 37.5665,
  "longitude": 126.978
}
```

`type`은 `HOSPITAL` 또는 `PHARMACY` 중 하나다. 성공 응답은 음성 통합 API와 동일한
`intent`, `step`, `ttsText`, `screen`, `data` 필드를 반환한다.
