# 백엔드 아키텍처

## 기술 스택

- Java 17
- Spring Boot 4.1.1
- Spring MVC, Bean Validation
- Spring Data JPA
- MariaDB(로컬 실행), H2(테스트)
- Springdoc OpenAPI 3.1.1
- Gradle Wrapper 9.7.1
- Lombok

## 패키지 구조

애플리케이션 루트 패키지는 `com.hyeja`다. 도메인별 패키지 안에 계층을 두는 구조다.

```text
com.hyeja
├── domain
│   ├── cardnews       # ctrl, dto, entity, repository, service
│   ├── favorite       # entity
│   ├── member         # ctrl, dto, entity, repository, service, converter, enums
│   ├── notification   # controller, dto, entity, repository, service, scheduler, converter
│   ├── policy         # ctrl, dto, entity, repository, service, converter, enums
│   ├── profile        # entity, converter, enums
│   ├── region         # entity
│   └── term           # entity
└── global
    ├── apiPayload     # 공통 응답과 상태 코드
    ├── baseEntity     # 감사 시각과 deletedAt
    ├── config         # JPA Auditing, Scheduling, Swagger, RestTemplate
    ├── exception      # 전역 예외 처리
    └── health         # 헬스체크
```

기존에 `ctrl`과 `controller` 같은 명칭이 혼재한다. 새 패키지명을 임의로 일괄 변경하지 말고, 기능을 추가할 때 해당 도메인의 기존 구조와 이슈 범위를 따른다.

## 일반 요청 흐름

```text
HTTP 요청
  → Controller: 입력 검증과 공통 응답 생성
  → Service: 유스케이스와 트랜잭션
  → Repository: JPA 조회·저장
  → Entity / Converter
  → DTO
  → ApiResponse
```

헬스체크는 단순 Map 응답이며 공통 `ApiResponse`를 사용하지 않는다. 그 밖의 현재 API는 공통 래퍼를 사용하지만, 일부 `result`에는 별도 응답 DTO 대신 JPA 엔티티 목록이 직접 들어간다. 새 API에서도 이를 그대로 답습할지 먼저 계약을 확인한다.

## 데이터베이스와 초기화

- `spring.jpa.hibernate.ddl-auto=update`
- `open-in-view=false`
- Hibernate DDL 이후 Spring SQL initializer가 일반 SQL을 실행한다.
- Flyway나 Liquibase는 현재 사용하지 않는다.
- 초기화 순서는 Policy category 정규화, IncomeRange 정규화, 개발 시드다.
- 개발 시드는 9개 테이블에 각 10행, 총 90행을 중복 없이 넣도록 구성되어 있다.
- 자세한 실행·검증 방법은 `docs/dev-seed.md`를 참고한다.

`BaseEntity`는 `@MappedSuperclass`이며 `BASE_ENTITY`라는 물리 테이블이 아니다. `softDelete()`는 `deletedAt`만 기록하고, 모든 조회에서 삭제 데이터를 자동 제외하지 않는다.

## 외부 정책 연동

`PolicyService`는 온통청년 API를 `RestTemplate`로 호출한다.

- 연결 및 읽기 timeout은 각각 10초다.
- 현재 최대 20페이지, 페이지당 100건을 가져온다.
- 외부 분류가 `주거`인 데이터만 저장한다.
- 동기화 스케줄러는 없고 `/api/policies/sync`로 수동 실행한다.
- 외부 정책 ID를 서비스의 문자열 PK로 그대로 저장한다.
- OpenAI 한 번의 요청으로 정책 요약, 복수 주거 카테고리, 무주택·소득 조건을 분석한다.
- API 지역 코드는 `PolicyRegion`에 그대로 저장하며 `xx000`은 조회 시 앞 두 자리 범위로 판정한다.

## 관심 정책 마감 알림

- 서버 스케줄러는 `Asia/Seoul` 기준 매일 00시 05분에 실행된다.
- 실행일로부터 7일 뒤 마감되는 활성 관심 정책을 회원별로 조회한다.
- 삭제된 관심 정책·회원·정책과 비활성 정책은 알림 생성 대상에서 제외한다.
- 동일 회원·정책·마감일 알림은 중복 생성하지 않는다.
- 스케줄러는 기준일을 전달하고, 조회와 생성 트랜잭션은 알림 생성 서비스가 담당한다.
- 개발·테스트용 `POST /api/notification/admin/generate?memberId={id}`는 서울 기준 오늘의 D-7 알림을 지정 회원에게만 생성한다.

## 환경변수

애플리케이션은 저장소 루트의 `.env`를 선택적으로 읽는다. 실제 값은 절대 커밋하거나 출력하지 않는다.

| 변수 | 용도 | 기본값 |
| --- | --- | --- |
| `DB_HOST` | MariaDB 호스트 | `localhost` |
| `DB_PORT` | MariaDB 포트 | `3306` |
| `DB_NAME` | DB 이름 | 없음 |
| `DB_USERNAME` | DB 사용자 | 없음 |
| `DB_PASSWORD` | DB 비밀번호 | 없음 |
| `DB_SEED_MODE` | SQL 초기화 모드 | `always` |
| `SERVER_PORT` | 서버 포트 | `8080` |
| `YOUTH_API_KEY` | 온통청년 API 키 | 없음 |

## 실행과 문서 경로

```bash
./gradlew bootRun
./gradlew test
```

- Swagger UI: `http://localhost:${SERVER_PORT:-8080}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:${SERVER_PORT:-8080}/v3/api-docs`
- 비즈니스 API 기본 prefix: `/api`

운영 서버 기본 URL은 없다. 문서와 테스트는 로컬 실행을 기준으로 한다.

## 현재 구조상 주의점

- 인증·인가는 아직 없다.
- `/api/policies/sync`도 현재 인증 없이 노출된다.
- `/api/policies/housing`은 이름과 달리 저장된 Policy 전체를 반환하며 별도 category, active, deleted 필터가 없다.
- 카드뉴스 조회도 active와 soft-delete를 자동 필터링하지 않는다.
- MariaDB, 실제 외부 API, 인증 흐름을 검증하는 통합 테스트는 아직 없다.
