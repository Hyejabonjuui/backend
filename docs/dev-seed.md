# 로컬 기준 데이터

로컬 실행 시 SQL 시드로 관리하는 데이터는 주거 용어 50개뿐입니다.
지역과 관리자는 각각 별도의 초기화 로직으로 생성하고,
정책·회원·관심 정책·알림은 실제 기능 흐름에서 생성합니다.

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
   JWT_SECRET=32자_이상의_아무_문자열
   # 선택: 비워 두면 회원가입 인증 코드가 메일 대신 서버 로그에 출력됩니다.
   MAIL_USERNAME=
   MAIL_PASSWORD=
   # 선택: 정책 동기화·알림 생성 관리자 API를 쓸 때만 입력합니다.
   ADMIN_EMAIL=
   ADMIN_PASSWORD=
   ```

   `DB_SEED_MODE=always`이면 앱 시작 시 없는 용어를 추가합니다.
   이 항목을 생략해도 기본값은 `always`입니다.

3. 별도 프로필 지정 없이 실행합니다.

   ```bash
   ./gradlew bootRun
   ```

   IntelliJ에서는 실행 설정의 Active profiles를 비워 두고,
   작업 디렉터리를 `.env`가 있는 프로젝트 루트로 설정합니다.

`application.yml`의 `ddl-auto: update`로 테이블을 생성·갱신한 뒤,
`defer-datasource-initialization: true`로 기존 enum 값 호환 스크립트와
`db/seed/dev-data.sql`의 용어 데이터를 순서대로 실행합니다.
호환 스크립트와 용어 시드는 반복 실행해도 결과가 중복되지 않습니다.

## 데이터별 생성 주체

| 데이터 | 생성 주체 | 비고 |
| --- | --- | --- |
| TERM | `dev-data.sql` | 주거 자격·임대차·청약 용어 50개 |
| REGION | `RegionDataInitializer` | `csv/region_sigungu.csv`의 시군구 269개 |
| 관리자 MEMBER | `AdminInitializer` | `ADMIN_EMAIL`·`ADMIN_PASSWORD`가 모두 있을 때만 생성 |
| 일반 MEMBER·PROFILE | 회원가입·프로필 입력 | SQL 시드 없음 |
| POLICY·POLICY_REGION·CARD_NEWS | `POST /api/policies/sync` | 외부 정책 데이터 기준 |
| FAVORITE | 관심 정책 등록 | SQL 시드 없음 |
| NOTIFICATION | 알림 생성 로직 | SQL 시드 없음 |

`RegionDataInitializer`와 `AdminInitializer`는 `dev-data.sql`과 별개로 실행됩니다.
관리자는 정책 동기화 등 관리자 API 호출에 프로필이 필요하지 않으므로
`MEMBER`만 생성하고 `PROFILE`은 생성하지 않습니다.

## 시드 실행 설정

용어 시드 및 같은 초기화 설정에 등록된 호환 스크립트 실행을 끄려면
`.env`에 아래 값을 넣고 재시작합니다.

```properties
DB_SEED_MODE=never
```

`never`로 설정해도 일반 실행 환경의 지역 CSV 적재와 관리자 생성은 동작합니다.
다시 실행하려면 `always`로 바꾸거나 해당 항목을 삭제합니다.
설정을 바꿔도 이미 생성된 데이터는 삭제되지 않습니다.

## 재실행과 기존 데이터

- 용어는 같은 용어명이 있으면 추가하지 않습니다.
- 기존 10개 용어는 이전 기본 문구가 그대로인 경우에만 정제된 설명과 예시로 갱신합니다.
- 개발자가 수정한 용어 설명·예시는 덮어쓰지 않습니다.
- 이 변경은 기존 로컬 DB의 가상 회원·정책·관심 정책·알림을 자동으로 삭제하지 않습니다.
- SQL 오류는 무시하지 않고 앱 시작 실패로 처리합니다.

## 검증

```bash
./gradlew test
```

`SeedDataTest`는 용어 50개만 생성되는지, 재실행 시 중복·덮어쓰기가 없는지,
관련 없는 테이블의 기존 데이터를 건드리지 않는지 확인합니다.
`RegionDataInitializerTest`와 `AdminInitializerTest`는 각 초기화 로직을 별도로 검증합니다.
