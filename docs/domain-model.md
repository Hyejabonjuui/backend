# 도메인 모델과 ERD 정합성

이 문서는 **현재 Java 코드**를 기준으로 모델을 설명하고, 팀에서 확정했지만 아직 코드나 SQL에 반영되지 않은 결정을 분리해 기록한다.

## 현재 코드의 엔티티

| 엔티티 | 식별자 | 주요 관계와 제약 |
| --- | --- | --- |
| `Member` | `member_id BIGINT` | email은 `VARCHAR(100) NOT NULL UNIQUE`, nickname은 코드상 UNIQUE 아님 |
| `Profile` | `email VARCHAR(100)` | 동일한 email로 Member 참조, Region 필수 N:1, 현재 Java 매핑은 `@ManyToOne` |
| `Region` | `region_code CHAR(5)` | Profile 및 PolicyRegion에서 참조 |
| `Policy` | 외부 `policy_id` 문자열 | 현재 Java 컬럼 길이 30, 외부 ID를 가공하지 않고 사용. `category`에는 하나 이상의 `PolicyCategory`를 쉼표로 저장 |
| `PolicyRegion` | `(policy_id, region_code)` | `@EmbeddedId`와 `@MapsId`를 사용하는 식별 연결 엔티티 |
| `Favorite` | `favorite_id BIGINT` | Member·Policy N:1, `(member_id, policy_id)` UNIQUE, 관심 해제는 물리 삭제 의도 |
| `Notification` | `notification_id BIGINT` | Member·Policy N:1, 마감일 스냅샷 저장, `(member_id, policy_id, deadline_date)` UNIQUE, 생성 시 `readYn=false` |
| `CardNews` | `card_news_id BIGINT` | Policy N:1 비식별 관계, `(policy_id, card_no)` UNIQUE, `card_no` 1~4 |
| `Term` | `term_id INT` | 독립 용어 풀이 테이블, term은 현재 코드상 UNIQUE 아님 |

모든 엔티티는 `BaseEntity`의 `createdAt`, `updatedAt`, `deletedAt`을 상속한다. Cascade 설정은 현재 없다.

마감 알림은 정책의 마감일이 이후 변경되어도 생성 당시 정보를 유지하도록 `deadlineDate`를 저장한다.
동일 회원·정책·마감일 조합은 한 번만 생성할 수 있다.

정책의 `description`은 동기화 시 OpenAI가 정책 원문을 1~2문장으로 요약한 값이다.
정책의 무주택 조건은 `REQUIRED`, `NOT_REQUIRED`, `UNKNOWN` enum으로 저장하며 회원
Profile의 `houselessYn`과 비교한다. 신청기간은 API의 `57001`, `57002`, `57003`을 각각
`SPECIFIC_PERIOD`, `ALWAYS`, `CLOSED`로 변환한다. 특정기간일 때만 시작일과 종료일을
저장하고 상시·마감은 두 날짜를 null로 유지한다.
정책 지원 지역은 `PolicyRegion`에 온통청년 API의 5자리 코드를 그대로 연결한다. `xx000` 광역 코드는
상세 시군구로 확장 저장하지 않고, 적합성 판정 시 회원 지역 코드의 앞 두 자리와 비교한다.
사용하지 않는 `subtype_code`와 파생 표시값이었던 `region_condition` 컬럼은 초기화 SQL로 제거한다.

## Profile 필드

Profile은 다음 조건을 저장한다.

- 필수: email, region, birth, employmentCode, houselessYn
- 선택: marriageCode, incomeRangeCode, educationCode, housingType
- 나이는 생년월일로 계산하며 별도 컬럼으로 저장하지 않는다.

enum 값과 DB 저장 코드는 `docs/profile-policy-enums.md`를 기준으로 확인한다.

## CardNews 계약

- `title`: `String`, nullable, 최대 255자
- `body`: `String`, not null, 최대 500자
- 한 정책 안에서 `cardNo`는 1~4이며 중복할 수 없다.
- 현재 비회원 카드뉴스 응답의 `description`에는 title이 아니라 body가 들어간다.

## 확정됐지만 별도 구현이 필요한 정합성 작업

아래 결정은 확정됐지만 현재 코드·테스트·ERD·SQL이 모두 정렬된 상태는 아니다. 관련 이슈에서 함께 수정하고, 무관한 작업에 섞지 않는다.

| 항목 | 확정 방향 | 현재 상태 |
| --- | --- | --- |
| Member–Profile | Member 1 : Profile 0..1, `Profile.member @OneToOne` | DB join은 unique이나 Java는 `@ManyToOne` |
| Profile 식별자 | `Profile.email`을 PK이자 `Member.email` FK로 사용 | 코드 방향은 일치 |
| 이메일 길이 | Member/Profile 모두 100 | Java는 이미 100, 최신 ERD·SQL 정렬 필요 |
| 외부 정책 ID | Policy PK와 모든 관련 FK를 `VARCHAR(100)` | 최신 ERD는 100, Java 일부는 30 |
| Boolean | DB BOOLEAN, 의미는 true/false로 통일 | 주요 코드와 최신 ERD에 반영 |

팀 ERD에서는 `Profile.email`이 자식 PK이자 `Member.email` FK이므로 이 관계를 **식별 관계**로 표현한다. 다만 `Member.email`은 부모 PK가 아니라 UNIQUE 후보키이므로 엄격한 공유-PK 매핑과는 다르다. JPA의 일대일 매핑 방식은 변경 작업에서 테스트로 검증한다.

## 최신 ERD와 코드의 추가 차이

- 최신 ERD가 `Member.nickname`을 UNIQUE로 본다면 현재 코드에는 해당 제약이 없다.
- `BASE_ENTITY`는 ERD의 논리적 공통 영역일 뿐 실제 테이블로 생성하지 않는다.
- CardNews는 자체 `card_news_id`만 PK로 사용하고 Policy와 비식별 N:1 관계를 유지한다.
- soft delete는 공통 필드만 제공한다. Repository 조회 조건이나 삭제 서비스가 따로 구현되지 않으면 자동 적용되지 않는다.

## 오래된 SQL 내보내기 주의

기존 `혜자 (1).sql`은 최종 스키마로 바로 실행할 수 있는 기준 파일이 아니다. 특히 다음 항목이 오래되었거나 잘못되어 있다.

- Profile email이 Member member_id를 참조하는 잘못된 FK
- email 길이 20
- 일부 enum 컬럼 길이 부족
- CardNews의 잘못된 복합 PK
- 여러 FK와 Member UNIQUE 제약 누락
- `BASE_ENTITY` 물리 테이블 생성

DDL을 갱신할 때는 엔티티와 테스트를 먼저 확인하고, 최신 합의 사항을 반영한 뒤 MariaDB에서 검증한다.
