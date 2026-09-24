# 로컬 시드 데이터

API 개발과 프론트 시연을 위한 가상 데이터입니다. 실제 지원 사업이나 신청 링크가 아닙니다.
빈 DB에서 9개 테이블에 각각 10개, 총 90개를 넣습니다.

## 실행

1. MariaDB에 `hyeja` 데이터베이스를 만듭니다. 이미 있으면 그대로 사용합니다.

   ```sql
   CREATE DATABASE IF NOT EXISTS hyeja CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 프로젝트 루트의 `.env`에 각자 접속 정보를 입력합니다. `.env`는 Git에 올리지 않습니다.

   ```properties
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=hyeja
   DB_USERNAME=각자의_DB_계정
   DB_PASSWORD=각자의_DB_비밀번호
   DB_SEED_MODE=always
   SERVER_PORT=8080
   ```

   `DB_USERNAME`과 `DB_PASSWORD`는 본인의 MariaDB 계정 정보로 바꿉니다.
   `KEY=value` 형식으로 작성하며, `SPRING_PROFILES_ACTIVE`는 설정하지 않습니다.
   기존 `.env`나 IDE 실행 설정에 프로필을 지정했다면 제거합니다.
   `DB_SEED_MODE=always`이면 앱 시작 시 없는 시드 행을 추가합니다.
   이 항목을 생략해도 기본값은 `always`입니다. `SERVER_PORT`의 기본값은 `8080`입니다.

3. 별도 프로필 지정 없이 실행합니다.

   ```bash
   ./gradlew bootRun
   ```

   IntelliJ에서는 실행 설정의 Active profiles를 비워 둡니다.
   작업 디렉터리는 `.env`가 있는 프로젝트 루트로 설정합니다.
   `.env`를 수정하기 전에 앱이 이미 실행 중이었다면 종료한 뒤 다시 실행합니다.

`application.yml`의 `ddl-auto: update`로 테이블을 생성·갱신한 뒤,
`defer-datasource-initialization: true`로 기존 enum 값 호환 스크립트와
`db/seed/dev-data.sql`을 순서대로 실행합니다. 호환 스크립트는 과거의 `category='주거'`
행과 임시 소득 구간 코드를 현재 enum 값으로 변환하며, 반복 실행해도 같은 결과를 유지합니다.
기존 데이터를 지우는 `create`나 `create-drop`을 개발 설정에 사용하지 않습니다.
로컬 개발 설정은 `application.yml` 하나로 관리하고, 각자의 DB 접속 정보는 `.env`에 둡니다.
`application-test.yml`은 H2와 `spring.sql.init.mode: never`를 사용해 일반 테스트에 시드를 넣지 않습니다.
시드 전용 테스트인 `SeedDataTest`에서만 `spring.sql.init.mode=always`로 실행을 켭니다.
테스트에서 사용하는 `create-drop`은 별도 H2 테스트 DB에만 적용됩니다.

시드 실행을 끄려면 `.env`에 아래 값을 넣고 재시작합니다.

```properties
DB_SEED_MODE=never
```

다시 실행하려면 `always`로 바꾸거나 해당 항목을 삭제합니다. 설정을 바꿔도 이미 넣은 데이터는 남습니다.

## 데이터 구성

| 테이블 | 수량 | 내용 |
| --- | ---: | --- |
| MEMBER | 10 | `seed01@hyeja.test` ~ `seed10@hyeja.test`, USER 역할 |
| PROFILE | 10 | 회원마다 프로필 1개, 서울 지역과 다양한 주거·취업 상태 |
| REGION | 10 | 서울 시군구 코드 10개 |
| POLICY | 10 | `DEMO-HOUSING-001` ~ `DEMO-HOUSING-010`, 분류별 2개 |
| POLICY_REGION | 10 | 정책마다 신청 지역 1개 |
| CARD_NEWS | 10 | 정책마다 첫 번째 카드 1개, 제목·문구 포함 |
| FAVORITE | 10 | 회원마다 관심 정책 1개 |
| NOTIFICATION | 10 | 회원마다 알림 1개, 읽음·안 읽음 각각 5개 |
| TERM | 10 | 주거 용어와 쉬운 설명, 예시 |

카드뉴스는 **총 10개**입니다. 각 정책의 4장 전체를 채운 데이터가 아닙니다.
정책 신청 기간은 시연을 위해 `2026-01-01` ~ `2027-12-31`로 고정했습니다.
SQL로 직접 저장하므로 생성·수정 시각을 명시하며, 신규 데이터의 `deleted_at`은 `NULL`입니다.
`housing_type`은 `MONTHLY_RENT`를 담을 수 있도록 PROFILE·POLICY 모두 길이 20으로 확장했습니다.
CARD_NEWS의 `title`은 ERD에 맞춰 길이 255의 선택 항목으로 추가했습니다.

## 테스트 계정

- 이메일: `seed01@hyeja.test` (나머지 `seed02` ~ `seed10`도 사용 가능)
- 공통 비밀번호: `Hyeja1234!`
- DB에는 평문 대신 BCrypt 해시를 저장합니다.

공개된 개발용 계정입니다. 로그인 API는 별도로 구현해야 하며, 시드 추가가 로그인 기능을 구현하는 것은 아닙니다.

## 코드값

프로필의 취업 상태·혼인 상태·소득 구간·학력·주거 형태와 정책 분류는 Java enum 이름을 사용합니다.
전체 코드와 화면 표시명은 [프로필·정책 enum 안내](profile-policy-enums.md)를 참고합니다.
정책의 외부 API 코드 필드는 문자열로 유지하며, 미확정 값은 NULL입니다.

| 필드 | 시드 값 |
| --- | --- |
| PROFILE.employment_code | EmploymentStatus 9개 선택지 전체 |
| PROFILE.marriage_code | SINGLE, MARRIED |
| PROFILE.income_range_code | UNDER_2000, R2000_3000, R3000_4000, R4000_5000, OVER_5000 |
| PROFILE.education_code | EducationLevel 9개 선택지 전체와 NULL |
| PROFILE.housing_type | PARENTS, MONTHLY_RENT, JEONSE, OWNED |
| POLICY.housing_type | 문자열 유지. 시드에는 MONTHLY_RENT, JEONSE 또는 NULL 사용 |
| POLICY.category | MONTHLY_RENT, JEONSE, PURCHASE, PUBLIC_RENT, OTHER |
| POLICY.apply_period_code | PERIOD |
| POLICY.employment_codes 등 미확정 외부 API 코드 | NULL |

소득 구간은 연소득 기준으로 각각 2천만원 미만, 2천~3천, 3천~4천, 4천~5천, 5천만원 이상을 뜻합니다.
신규 시드는 취업 상태 9개 선택지를 모두 포함하지만, 기존 시드 행의 취업 상태는 재실행으로 덮어쓰지 않습니다.
기존 시드의 EMPLOYED, UNEMPLOYED, FREELANCER 등은 새 enum과 호환되므로 그대로 사용할 수 있습니다.
수동으로 다른 문자열을 넣었다면 [기존 DB 확인 방법](profile-policy-enums.md#기존-db-확인)을 먼저 확인합니다.

## 재실행과 기존 데이터

- 숫자 PK를 고정하지 않고 DB에서 생성합니다. FK는 회원 이메일·정책 ID·지역 코드로 연결합니다.
- 각 INSERT는 `NOT EXISTS`로 이미 있는 행을 건너뜁니다. 기존 회원, 수정된 내용, 읽음 상태, 삭제 시각을 덮어쓰지 않습니다.
- 기존 데이터가 있는 DB에서는 전체 행 수가 10개보다 많을 수 있습니다. 이미 존재하는 지역 코드나 용어도 그대로 사용합니다.
- 알림은 시드 회원·정책 조합에 알림이 있으면 추가하지 않습니다. 이는 시드 전용 규칙이며 실제 알림의 복수 생성을 제한하지 않습니다.
- 용어는 같은 용어명이 있으면 추가하지 않습니다.
- 관심 정책처럼 **물리 삭제한 시드 행은 다음 실행에서 다시 생길 수 있습니다.** 삭제 상태를 유지하며 개발하려면 `DB_SEED_MODE=never`로 실행합니다.
- 한 DB에 연결하는 앱 한 개를 시작하는 로컬 개발 환경을 기준으로 합니다.
- SQL 오류는 무시하지 않고 시작 시 실패하도록 설정했습니다.

## 검증

```bash
./gradlew test
```

자동 테스트에서 최초 생성 수량, FK 연결, BCrypt 비밀번호, 재실행 중복 방지,
기존 데이터·수정 내용 보존, 자동 생성 ID 충돌 방지, 일반 테스트 프로필에서 시드 미실행을 확인합니다.
