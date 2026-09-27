# API E2E 테스트

## 목적

E2E 테스트는 실제 HTTP 서버를 띄우고 Spring Security, Controller, Service, Repository와 데이터 저장소를 한 번에 통과하는 핵심 사용자 여정을 검증한다. 외부 온통청년 API와 OpenAI API는 호출하지 않는다.

## 실행

Docker Desktop 또는 Docker Engine을 실행한 뒤 저장소 루트에서 다음 명령을 사용한다.

```bash
./gradlew e2eTest
```

- MariaDB `11.4.8`과 Redis `7.2.10-alpine` 컨테이너는 Testcontainers가 시작하고 종료한다.
- 로컬 MariaDB·Redis와 운영 환경변수는 사용하지 않는다.
- 일반 `./gradlew test`와 E2E 테스트는 소스셋과 리포트가 분리되어 있다.
- HTML 리포트는 `build/reports/tests/e2eTest/index.html`에 생성된다.

## 검증 범위

| 여정 | 검증 내용 |
| --- | --- |
| E-1 회원 | 회원가입 → 로그인 → 내 계정·프로필 조회 → MariaDB 저장 확인 |
| E-2 인증 | 토큰 없음·위조 토큰 거부 → 정상 인증 → 로그아웃 → Redis 블랙리스트에 따른 재사용 거부 |
| E-3 정책 | 비회원 정책 목록의 활성·마감 필터, 카테고리, 정렬, 페이지네이션, 지역·D-day 응답 |
| E-4 관심 정책 | 등록 → 목록 조회 → 중복 방지 → 삭제 → MariaDB 삭제 확인 |
| E-5 알림 | D-7 알림 생성과 중복 방지 → 목록 → 읽음 → 삭제 |

## 재현성과 격리

- 각 테스트 전에 관련 MariaDB 테이블과 Redis 키를 비운다.
- 테스트는 실행 순서와 이전 실행 결과에 의존하지 않는다.
- Gradle E2E task는 JVM 시간대를 `Asia/Seoul`로 고정한다.
- 정책 마감일과 알림 생성 기준은 테스트용 고정 `Clock`의 `2026-09-27`을 사용한다.
- 테스트 fixture는 Repository를 통해 직접 만들며 네트워크의 외부 API 데이터에 의존하지 않는다.

## CI

PR에서는 기존 `Gradle Test` job과 별도로 `API E2E` job이 병렬 실행된다. 성공·실패와 관계없이 `e2e-test-reports` artifact에 HTML·XML 리포트를 7일간 보관한다.
