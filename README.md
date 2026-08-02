# 척척 백엔드

Java 21, Spring Boot 3.5, Redis 기반 백엔드입니다.

## 로컬 실행

필수 도구: JDK 21, Docker

```bash
docker compose up -d
./gradlew bootRun
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`을 사용할 수 있습니다.

테스트는 다음 명령으로 실행합니다.

```bash
./gradlew test
```

## 설정

- 공통 설정은 `src/main/resources/application.yml`에 둡니다.
- 로컬 API 키와 비밀값은 Git에서 제외된 `application-local.yml`에 둡니다.
- 새 팀원은 `application-local.example.yml`을 기준으로 로컬 설정을 만듭니다.
- 배포 환경의 비밀값은 환경 변수로 주입합니다.

## 패키지 규칙

패키지 루트는 `com.chuckchuck`입니다. 기능을 구현할 때 `auth`, `voice`,
`session`, `train`, `youtube`, `map`, `kiosk` 하위에 도메인별 코드를 둡니다.
사용하지 않는 빈 패키지는 미리 만들지 않습니다.

모든 음성 기능의 진입점은 `POST /api/voice/process`이며, 기존 공통 응답의
`intent`, `step`, `slots`, `ttsText`, `screen`, `data` 필드를 유지해야 합니다.
