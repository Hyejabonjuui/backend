# 프로필·정책 enum

이슈 #27에서는 아래 여섯 필드를 enum으로 관리합니다. 기존 컬럼 이름과 Java 필드 이름은 유지합니다.
API와 DB에 사용할 값은 대문자 enum 이름이며, `getLabel()`은 화면 표시용 한글 이름입니다.

| 필드 | Java 타입 | DB 컬럼 | NULL |
| --- | --- | --- | --- |
| PROFILE.employment_code | EmploymentStatus | VARCHAR(20) | 불가 |
| PROFILE.marriage_code | MaritalStatus | VARCHAR(10) | 허용 |
| PROFILE.income_range_code | IncomeRange | VARCHAR(10) | 허용 |
| PROFILE.education_code | EducationLevel | VARCHAR(30) | 허용 |
| PROFILE.housing_type | HousingType | VARCHAR(20) | 허용 |
| POLICY.category | PolicyCategory | VARCHAR(20) | 불가 |

프로필 enum은 `com.hyeja.domain.profile.enums`, 정책 enum은 `com.hyeja.domain.policy.enums`에 둡니다.
기본적으로 `@Enumerated(EnumType.STRING)`으로 이름을 저장하고 `@JdbcTypeCode(SqlTypes.VARCHAR)`로
기존 VARCHAR 타입을 유지합니다. 소득 구간과 정책 분류는 이전 DB 값도 읽을 수 있는 JPA converter를 사용하며,
새로 저장할 때는 동일하게 현재 enum 이름을 저장합니다.
취업 상태에는 `SHORT_TERM_WORKER`처럼 10자를 넘는 값이 있어 컬럼 길이를 10에서 20으로 늘렸습니다.
학력에는 `HIGH_SCHOOL_EXPECTED_GRADUATE`처럼 10자를 넘는 값이 있어 컬럼 길이를 10에서 30으로 늘렸습니다.
미선택인 혼인 상태·소득 구간·학력·주거 형태는 `null`을 사용하며, 빈 문자열이나 문자열 `"null"`을 보내지 않습니다.

## 선택지

[피그마 내 조건 수정 화면](https://www.figma.com/design/19OJDjkZqMnGthjP4aCi0N?node-id=52-297)과
[홈 정책 분류](https://www.figma.com/design/19OJDjkZqMnGthjP4aCi0N?node-id=37-536)를 기준으로 확인했습니다.

### EmploymentStatus

| 코드 | 표시명 |
| --- | --- |
| EMPLOYED | 재직자 |
| SELF_EMPLOYED | 자영업자 |
| UNEMPLOYED | 미취업자 |
| FREELANCER | 프리랜서 |
| DAILY_WORKER | 일용근로자 |
| ENTREPRENEUR | (예비)창업자 |
| SHORT_TERM_WORKER | 단기근로자 |
| FARMER | 영농종사자 |
| OTHER | 기타 |

### MaritalStatus

| 코드 | 표시명 |
| --- | --- |
| SINGLE | 미혼 |
| MARRIED | 기혼 |

### IncomeRange

| 코드 | 표시명 |
| --- | --- |
| UNDER_2000 | 2천만원 미만 |
| R2000_3000 | 2천만원 이상 3천만원 미만 |
| R3000_4000 | 3천만원 이상 4천만원 미만 |
| R4000_5000 | 4천만원 이상 5천만원 미만 |
| OVER_5000 | 5천만원 이상 |

### HousingType

| 코드 | 표시명 |
| --- | --- |
| PARENTS | 부모님 집 |
| MONTHLY_RENT | 월세 |
| JEONSE | 전세 |
| OWNED | 자가 |

### PolicyCategory

| 코드 | 표시명 |
| --- | --- |
| MONTHLY_RENT | 월세 |
| JEONSE | 전세 |
| PURCHASE | 청약·구입 |
| PUBLIC_RENT | 공공임대 |
| OTHER | 기타 주거 |

### EducationLevel

| 코드 | 표시명 |
| --- | --- |
| BELOW_HIGH_SCHOOL | 고졸 미만 |
| HIGH_SCHOOL_STUDENT | 고교 재학 |
| HIGH_SCHOOL_EXPECTED_GRADUATE | 고졸 예정 |
| HIGH_SCHOOL_GRADUATE | 고교 졸업 |
| COLLEGE_GRADUATE | 대학 졸업 |
| COLLEGE_EXPECTED_GRADUATE | 대졸 예정 |
| COLLEGE_STUDENT | 대학 재학 |
| MASTER_OR_DOCTOR | 석박사 |
| OTHER | 기타 |

화면의 '전체'는 필터를 적용하지 않는 선택지이므로 DB에 저장하는 enum에는 포함하지 않습니다.

## 기존 DB 확인

앱 시작 시 `ddl-auto: update`가 취업 상태와 학력 컬럼 길이를 확장합니다.
그다음 호환 스크립트가 이전 소득 코드 `INC_0_20`~`INC_40_UP`을 새 enum 이름으로 변환합니다.
기존 `INC_40_UP`은 세부 금액을 알 수 없으므로 하한이 같은 `R4000_5000`으로 변환합니다.

직접 넣거나 수정한 데이터가 있다면 앱 실행 전에 DB IDE에서 다음 쿼리를 실행합니다.
각 쿼리의 결과가 없으면 해당 필드의 기존 값이 enum과 호환됩니다.
`BINARY` 비교를 사용해 대소문자까지 확인합니다.

```sql
SELECT employment_code, COUNT(*) AS row_count
FROM profile
WHERE employment_code IS NULL OR BINARY employment_code NOT IN (
    'EMPLOYED', 'SELF_EMPLOYED', 'UNEMPLOYED', 'FREELANCER', 'DAILY_WORKER',
    'ENTREPRENEUR', 'SHORT_TERM_WORKER', 'FARMER', 'OTHER'
)
GROUP BY employment_code;

SELECT marriage_code, COUNT(*) AS row_count
FROM profile
WHERE marriage_code IS NOT NULL
  AND BINARY marriage_code NOT IN ('SINGLE', 'MARRIED')
GROUP BY marriage_code;

SELECT income_range_code, COUNT(*) AS row_count
FROM profile
WHERE income_range_code IS NOT NULL
  AND BINARY income_range_code NOT IN (
    'UNDER_2000', 'R2000_3000', 'R3000_4000', 'R4000_5000', 'OVER_5000'
)
GROUP BY income_range_code;

SELECT housing_type, COUNT(*) AS row_count
FROM profile
WHERE housing_type IS NOT NULL
  AND BINARY housing_type NOT IN ('PARENTS', 'MONTHLY_RENT', 'JEONSE', 'OWNED')
GROUP BY housing_type;

SELECT education_code, COUNT(*) AS row_count
FROM profile
WHERE education_code IS NOT NULL
  AND BINARY education_code NOT IN (
    'BELOW_HIGH_SCHOOL', 'HIGH_SCHOOL_STUDENT', 'HIGH_SCHOOL_EXPECTED_GRADUATE',
    'HIGH_SCHOOL_GRADUATE', 'COLLEGE_GRADUATE', 'COLLEGE_EXPECTED_GRADUATE',
    'COLLEGE_STUDENT', 'MASTER_OR_DOCTOR', 'OTHER'
)
GROUP BY education_code;

SELECT category, COUNT(*) AS row_count
FROM policy
WHERE category IS NULL
   OR BINARY category NOT IN ('MONTHLY_RENT', 'JEONSE', 'PURCHASE', 'PUBLIC_RENT', 'OTHER')
GROUP BY category;
```

결과가 있다면 해당 행의 의미를 확인해서 위 표의 값으로 수정합니다.
예를 들어 미혼을 `UNMARRIED`로 넣었던 데이터는 다음과 같이 바꿀 수 있습니다.
이 SQL은 자동 실행되지 않으며, 해당 값이 있는 경우에만 DB IDE에서 직접 실행합니다.

```sql
UPDATE profile
SET marriage_code = 'SINGLE'
WHERE BINARY marriage_code = 'UNMARRIED';
```

정책 대분류 `주거`와 이전 소득 구간 코드는 호환 스크립트가 자동 변환합니다.
그 밖의 `TEST`처럼 의미를 알 수 없는 값은 해당 회원의 선택이나 정책 내용을 확인하여
정확한 코드로 수정한 뒤 위 확인 쿼리를 다시 실행합니다.
알 수 없는 enum 이름이 남으면 JPA 조회 시 변환 오류가 발생할 수 있습니다.

## 문자열로 유지하는 외부 API 필드

- POLICY.employment_codes, marriage_code, housing_type 등: 기존 외부 API 코드 문자열.
- MEMBER.role: 기존 Role enum 유지.

프로필의 enum 이름은 외부 API 원본 코드와 구분하며, 외부 API와의 대응 관계는 연동 작업에서 정의합니다.

## 외부 정책 API 분류

온통청년 API의 `lclsfNm` 값 `주거`는 혜자의 월세·전세·청약·구입·공공임대보다 큰 분류입니다.
현재 응답 DTO에는 세부 분류 정보가 없으므로 동기화한 정책은 `PolicyCategory.OTHER`로 저장합니다.
세부 분류 필드를 추가로 연동할 때 해당 값을 기준으로 `PolicyCategory` 변환 규칙을 추가합니다.
외부 API DTO의 원본 분류 필드는 문자열로 유지합니다.

## 검증

```bash
./gradlew test
```

모든 enum 값의 저장·조회 및 DB 문자열을 확인하고, 선택 입력의 null 허용과 필수값 제약을 검증합니다.
시드 테스트는 9개 테이블의 각 10개 입력, enum 전체 선택지 포함, 기존 데이터 보존을 확인합니다.
테스트용 H2 2.4.240의 [CHECK 제약 처리 문제](https://github.com/h2database/h2database/issues/4308)를 피하기 위해
`application-test.yml`에서 `spring.test.database.replace: none`으로 설정된 H2/Hikari 연결 풀을 사용합니다.
