-- 개발·시연용 가상 데이터: 9개 테이블 × 10행 = 총 90행 (빈 DB 기준).
-- 로컬 앱 시작 시 실행합니다. DB_SEED_MODE=never로 끌 수 있으며, 기존 행은 수정·삭제하지 않습니다.
-- IDENTITY PK는 DB가 생성하고, FK는 이메일/정책 ID/지역 코드로 연결합니다.
-- 재실행 시 같은 키의 행이 있으면 건너뜁니다. deleted_at이 있는 행도 복구하지 않습니다.
-- 프로필·정책 분류는 Java enum 이름과 동일하게 저장합니다. 전체 목록: docs/profile-policy-enums.md
-- employment_code: EMPLOYED / SELF_EMPLOYED / UNEMPLOYED / FREELANCER / DAILY_WORKER
--                  ENTREPRENEUR / SHORT_TERM_WORKER / FARMER / OTHER
-- marriage_code: SINGLE(미혼), MARRIED(기혼)
-- income_range_code: UNDER_2000 / R2000_3000 / R3000_4000 / R4000_5000 / OVER_5000
-- 연소득 구간: 2천만원 미만 / 2천~3천 / 3천~4천 / 4천~5천 / 5천만원 이상
-- education_code: BELOW_HIGH_SCHOOL / HIGH_SCHOOL_STUDENT / HIGH_SCHOOL_EXPECTED_GRADUATE
--                 HIGH_SCHOOL_GRADUATE / COLLEGE_GRADUATE / COLLEGE_EXPECTED_GRADUATE
--                 COLLEGE_STUDENT / MASTER_OR_DOCTOR / OTHER / NULL(미선택)
-- housing_type: PARENTS / MONTHLY_RENT / JEONSE / OWNED
-- category: MONTHLY_RENT / JEONSE / PURCHASE / PUBLIC_RENT / OTHER
-- apply_period_code: PERIOD(기간 지정). 외부 API 공식 코드가 아닙니다.
-- 외부 API 코드인 employment_codes 등은 확인 전까지 NULL로 둡니다.
-- 공통 테스트 비밀번호 Hyeja1234!의 BCrypt 해시를 저장합니다.

INSERT INTO region (region_code, sigungu_name, created_at, updated_at, deleted_at)
SELECT seed.region_code, seed.sigungu_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT '11440' AS region_code, '서울특별시 마포구' AS sigungu_name
    UNION ALL SELECT '11680', '서울특별시 강남구'
    UNION ALL SELECT '11200', '서울특별시 성동구'
    UNION ALL SELECT '11620', '서울특별시 관악구'
    UNION ALL SELECT '11710', '서울특별시 송파구'
    UNION ALL SELECT '11350', '서울특별시 노원구'
    UNION ALL SELECT '11500', '서울특별시 강서구'
    UNION ALL SELECT '11590', '서울특별시 동작구'
    UNION ALL SELECT '11410', '서울특별시 서대문구'
    UNION ALL SELECT '11740', '서울특별시 강동구'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM region existing WHERE existing.region_code = seed.region_code
);

INSERT INTO member (email, password, nickname, role, created_at, updated_at, deleted_at)
SELECT seed.email, '$2a$10$ihBqhvo7s6dkXcMQiulmvOpcFK3aV4XQ1mXFncW7vdxahgER3l6jG', seed.nickname, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT 'seed01@hyeja.test' AS email, '[시드]민지' AS nickname
    UNION ALL SELECT 'seed02@hyeja.test', '[시드]준호'
    UNION ALL SELECT 'seed03@hyeja.test', '[시드]서연'
    UNION ALL SELECT 'seed04@hyeja.test', '[시드]도윤'
    UNION ALL SELECT 'seed05@hyeja.test', '[시드]지우'
    UNION ALL SELECT 'seed06@hyeja.test', '[시드]수빈'
    UNION ALL SELECT 'seed07@hyeja.test', '[시드]현우'
    UNION ALL SELECT 'seed08@hyeja.test', '[시드]예린'
    UNION ALL SELECT 'seed09@hyeja.test', '[시드]건우'
    UNION ALL SELECT 'seed10@hyeja.test', '[시드]하은'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM member existing WHERE existing.email = seed.email
);

INSERT INTO profile (email, region_code, birth, employment_code, houseless_yn, marriage_code, income_range_code, housing_type, education_code, created_at, updated_at, deleted_at)
SELECT seed.email, seed.region_code, CAST(seed.birth AS DATE), seed.employment_code, seed.houseless_yn, seed.marriage_code, seed.income_range_code, seed.housing_type, seed.education_code, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT 'seed01@hyeja.test' AS email, '11440' AS region_code, '1999-03-12' AS birth, 'UNEMPLOYED' AS employment_code, TRUE AS houseless_yn, 'SINGLE' AS marriage_code, 'R2000_3000' AS income_range_code, 'MONTHLY_RENT' AS housing_type, 'COLLEGE_STUDENT' AS education_code
    UNION ALL SELECT 'seed02@hyeja.test', '11680', '1996-07-21', 'EMPLOYED', TRUE, 'SINGLE', 'R3000_4000', 'JEONSE', 'COLLEGE_GRADUATE'
    UNION ALL SELECT 'seed03@hyeja.test', '11200', '2001-11-05', 'FREELANCER', TRUE, 'SINGLE', 'UNDER_2000', 'PARENTS', 'COLLEGE_EXPECTED_GRADUATE'
    UNION ALL SELECT 'seed04@hyeja.test', '11620', '1994-02-18', 'SELF_EMPLOYED', FALSE, 'MARRIED', 'OVER_5000', 'OWNED', 'MASTER_OR_DOCTOR'
    UNION ALL SELECT 'seed05@hyeja.test', '11710', '2000-08-30', 'DAILY_WORKER', TRUE, 'SINGLE', 'UNDER_2000', 'PARENTS', 'BELOW_HIGH_SCHOOL'
    UNION ALL SELECT 'seed06@hyeja.test', '11350', '1998-05-09', 'ENTREPRENEUR', TRUE, 'SINGLE', 'R2000_3000', 'MONTHLY_RENT', 'HIGH_SCHOOL_GRADUATE'
    UNION ALL SELECT 'seed07@hyeja.test', '11500', '1995-12-14', 'SHORT_TERM_WORKER', TRUE, 'MARRIED', 'R4000_5000', 'JEONSE', 'HIGH_SCHOOL_EXPECTED_GRADUATE'
    UNION ALL SELECT 'seed08@hyeja.test', '11590', '2002-01-27', 'FARMER', TRUE, 'SINGLE', 'UNDER_2000', 'PARENTS', 'HIGH_SCHOOL_STUDENT'
    UNION ALL SELECT 'seed09@hyeja.test', '11410', '1997-09-03', 'OTHER', TRUE, 'SINGLE', 'R3000_4000', 'MONTHLY_RENT', 'OTHER'
    UNION ALL SELECT 'seed10@hyeja.test', '11740', '1993-06-16', 'FREELANCER', FALSE, 'MARRIED', 'OVER_5000', 'OWNED', NULL
) seed
JOIN member m ON m.email = seed.email
JOIN region r ON r.region_code = seed.region_code
WHERE NOT EXISTS (
    SELECT 1 FROM profile existing WHERE existing.email = seed.email
);

INSERT INTO policy (policy_id, policy_name, category, description, support_content, housing_type, keywords, min_age, max_age, age_limit_yn, houseless_yn, apply_period_code, extra_qualification, apply_start_date, apply_end_date, apply_method, apply_url, ref_url, view_count, active_yn, created_at, updated_at, deleted_at)
SELECT seed.policy_id, seed.policy_name, seed.category, seed.description, seed.support_content, seed.housing_type, '청년,주거,시연', 19, 39, TRUE, NULL, 'PERIOD', '실제 신청할 수 없는 개발·시연용 가상 정책입니다.', CAST('2026-01-01' AS DATE), CAST('2027-12-31' AS DATE), '시연용 링크이며 신청 기능은 제공하지 않습니다.', 'https://example.com/hyeja-demo/apply', 'https://example.com/hyeja-demo', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT 'DEMO-HOUSING-001' AS policy_id, '[시연] 월세 부담 완화' AS policy_name, 'MONTHLY_RENT' AS category, '월 임대료 일부를 지원하는 가상 정책입니다.' AS description, '월 10만원씩 6개월 지원' AS support_content, 'MONTHLY_RENT' AS housing_type
    UNION ALL SELECT 'DEMO-HOUSING-002', '[시연] 전세 보증금 이자 지원', 'JEONSE', '전세 보증금 대출 이자를 지원하는 가상 정책입니다.', '연 이자 최대 30만원 지원', 'JEONSE'
    UNION ALL SELECT 'DEMO-HOUSING-003', '[시연] 내 집 준비 청약 교육', 'PURCHASE', '청약 절차를 안내하는 가상 교육 정책입니다.', '온라인 청약 교육 3회 제공', NULL
    UNION ALL SELECT 'DEMO-HOUSING-004', '[시연] 청년 공공임대 입주 안내', 'PUBLIC_RENT', '청년 공공임대 입주 절차를 소개하는 가상 정책입니다.', '시연용 임대주택 10호 공급 안내', NULL
    UNION ALL SELECT 'DEMO-HOUSING-005', '[시연] 첫 독립 이사 지원', 'OTHER', '첫 독립을 위한 이사비를 지원하는 가상 정책입니다.', '이사비 최대 15만원 지원', NULL
    UNION ALL SELECT 'DEMO-HOUSING-006', '[시연] 사회초년생 월세 지원', 'MONTHLY_RENT', '사회초년생의 월세를 지원하는 가상 정책입니다.', '월 8만원씩 6개월 지원', 'MONTHLY_RENT'
    UNION ALL SELECT 'DEMO-HOUSING-007', '[시연] 전세 계약 안심 상담', 'JEONSE', '전세 계약 확인 사항을 안내하는 가상 정책입니다.', '온라인 계약 상담 1회 제공', 'JEONSE'
    UNION ALL SELECT 'DEMO-HOUSING-008', '[시연] 청약 저축 준비 지원', 'PURCHASE', '청약 저축 준비를 돕는 가상 정책입니다.', '청약 준비 교육 및 상담 제공', NULL
    UNION ALL SELECT 'DEMO-HOUSING-009', '[시연] 역세권 임대주택 안내', 'PUBLIC_RENT', '교통이 편리한 임대주택을 소개하는 가상 정책입니다.', '시연용 임대주택 5호 공급 안내', NULL
    UNION ALL SELECT 'DEMO-HOUSING-010', '[시연] 주거 생활 정착 지원', 'OTHER', '독립 후 생활 정착을 돕는 가상 정책입니다.', '주거 생활 교육 2회 제공', NULL
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM policy existing WHERE existing.policy_id = seed.policy_id
);

INSERT INTO policy_region (policy_id, region_code, created_at, updated_at, deleted_at)
SELECT seed.policy_id, seed.region_code, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT 'DEMO-HOUSING-001' AS policy_id, '11440' AS region_code
    UNION ALL SELECT 'DEMO-HOUSING-002', '11680'
    UNION ALL SELECT 'DEMO-HOUSING-003', '11200'
    UNION ALL SELECT 'DEMO-HOUSING-004', '11620'
    UNION ALL SELECT 'DEMO-HOUSING-005', '11710'
    UNION ALL SELECT 'DEMO-HOUSING-006', '11350'
    UNION ALL SELECT 'DEMO-HOUSING-007', '11500'
    UNION ALL SELECT 'DEMO-HOUSING-008', '11590'
    UNION ALL SELECT 'DEMO-HOUSING-009', '11410'
    UNION ALL SELECT 'DEMO-HOUSING-010', '11740'
) seed
JOIN policy p ON p.policy_id = seed.policy_id
JOIN region r ON r.region_code = seed.region_code
WHERE NOT EXISTS (
    SELECT 1 FROM policy_region existing WHERE existing.policy_id = seed.policy_id AND existing.region_code = seed.region_code
);

INSERT INTO card_news (policy_id, title, body, card_no, created_at, updated_at, deleted_at)
SELECT seed.policy_id, seed.title, seed.body, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT 'DEMO-HOUSING-001' AS policy_id, '[시연] 월세 부담 완화' AS title, '월 10만원씩 6개월 지원. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.' AS body
    UNION ALL SELECT 'DEMO-HOUSING-002', '[시연] 전세 보증금 이자 지원', '연 이자 최대 30만원 지원. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-003', '[시연] 내 집 준비 청약 교육', '온라인 청약 교육 3회 제공. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-004', '[시연] 청년 공공임대 입주 안내', '시연용 임대주택 10호 공급 안내. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-005', '[시연] 첫 독립 이사 지원', '이사비 최대 15만원 지원. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-006', '[시연] 사회초년생 월세 지원', '월 8만원씩 6개월 지원. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-007', '[시연] 전세 계약 안심 상담', '온라인 계약 상담 1회 제공. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-008', '[시연] 청약 저축 준비 지원', '청약 준비 교육 및 상담 제공. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-009', '[시연] 역세권 임대주택 안내', '시연용 임대주택 5호 공급 안내. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
    UNION ALL SELECT 'DEMO-HOUSING-010', '[시연] 주거 생활 정착 지원', '주거 생활 교육 2회 제공. 자세한 내용은 정책 상세 화면에서 확인하세요. 실제 지원 사업이 아닌 가상 데이터입니다.'
) seed
JOIN policy p ON p.policy_id = seed.policy_id
WHERE NOT EXISTS (
    SELECT 1 FROM card_news existing WHERE existing.policy_id = seed.policy_id AND existing.card_no = 1
);

INSERT INTO favorite (member_id, policy_id, created_at, updated_at, deleted_at)
SELECT m.member_id, seed.policy_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT 'seed01@hyeja.test' AS email, 'DEMO-HOUSING-001' AS policy_id
    UNION ALL SELECT 'seed02@hyeja.test', 'DEMO-HOUSING-002'
    UNION ALL SELECT 'seed03@hyeja.test', 'DEMO-HOUSING-003'
    UNION ALL SELECT 'seed04@hyeja.test', 'DEMO-HOUSING-004'
    UNION ALL SELECT 'seed05@hyeja.test', 'DEMO-HOUSING-005'
    UNION ALL SELECT 'seed06@hyeja.test', 'DEMO-HOUSING-006'
    UNION ALL SELECT 'seed07@hyeja.test', 'DEMO-HOUSING-007'
    UNION ALL SELECT 'seed08@hyeja.test', 'DEMO-HOUSING-008'
    UNION ALL SELECT 'seed09@hyeja.test', 'DEMO-HOUSING-009'
    UNION ALL SELECT 'seed10@hyeja.test', 'DEMO-HOUSING-010'
) seed
JOIN member m ON m.email = seed.email
JOIN policy p ON p.policy_id = seed.policy_id
WHERE NOT EXISTS (
    SELECT 1 FROM favorite existing WHERE existing.member_id = m.member_id AND existing.policy_id = seed.policy_id
);

INSERT INTO notification (
    member_id, policy_id, deadline_date, read_yn, created_at, updated_at, deleted_at
)
SELECT m.member_id, seed.policy_id, p.apply_end_date, seed.read_yn,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT 'seed01@hyeja.test' AS email, 'DEMO-HOUSING-001' AS policy_id, FALSE AS read_yn
    UNION ALL SELECT 'seed02@hyeja.test', 'DEMO-HOUSING-002', TRUE
    UNION ALL SELECT 'seed03@hyeja.test', 'DEMO-HOUSING-003', FALSE
    UNION ALL SELECT 'seed04@hyeja.test', 'DEMO-HOUSING-004', TRUE
    UNION ALL SELECT 'seed05@hyeja.test', 'DEMO-HOUSING-005', FALSE
    UNION ALL SELECT 'seed06@hyeja.test', 'DEMO-HOUSING-006', TRUE
    UNION ALL SELECT 'seed07@hyeja.test', 'DEMO-HOUSING-007', FALSE
    UNION ALL SELECT 'seed08@hyeja.test', 'DEMO-HOUSING-008', TRUE
    UNION ALL SELECT 'seed09@hyeja.test', 'DEMO-HOUSING-009', FALSE
    UNION ALL SELECT 'seed10@hyeja.test', 'DEMO-HOUSING-010', TRUE
) seed
JOIN member m ON m.email = seed.email
JOIN policy p ON p.policy_id = seed.policy_id
WHERE NOT EXISTS (
    SELECT 1
    FROM notification existing
    WHERE existing.member_id = m.member_id
      AND existing.policy_id = seed.policy_id
      AND existing.deadline_date = p.apply_end_date
);

INSERT INTO term (term, easy_description, example, created_at, updated_at, deleted_at)
SELECT seed.term, seed.easy_description, seed.example, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT '무주택자' AS term, '본인 명의 주택을 소유하지 않은 사람을 뜻합니다. 실제 판단 기준은 정책마다 확인해야 합니다.' AS easy_description, '시연 예시: 본인 명의 주택이 없는 청년' AS example
    UNION ALL SELECT '보증금', '임대차 계약에서 임차인이 임대인에게 맡기는 금액입니다.', '시연 예시: 보증금 500만원'
    UNION ALL SELECT '월세', '집을 빌리는 대가로 매달 내는 금액입니다.', '시연 예시: 매달 월세 40만원'
    UNION ALL SELECT '전세', '보증금을 맡기고 약정한 기간 동안 집을 사용하는 임대차 방식입니다.', '시연 예시: 전세 보증금 1억원'
    UNION ALL SELECT '공공임대', '공공기관 등이 공급하는 임대주택입니다. 입주 조건은 공고마다 다릅니다.', '시연 예시: 공공임대 입주 공고 확인'
    UNION ALL SELECT '청약', '공급하는 주택에 입주하거나 분양받기 위해 신청하는 절차입니다.', '시연 예시: 모집 공고에 따라 청약 신청'
    UNION ALL SELECT '임차인', '집이나 건물을 빌려 사용하는 사람입니다.', '시연 예시: 월세 집에 거주하는 계약자'
    UNION ALL SELECT '임대인', '집이나 건물을 다른 사람에게 빌려주는 사람입니다.', '시연 예시: 임대차 계약의 집주인'
    UNION ALL SELECT '전입신고', '새로운 거주지로 이사한 사실을 관할 기관에 신고하는 절차입니다.', '시연 예시: 이사 후 거주지 변경 신고'
    UNION ALL SELECT '관리비', '건물과 공용 시설의 관리 등에 드는 비용입니다. 항목은 계약과 고지서를 확인합니다.', '시연 예시: 공용 전기료와 청소비'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM term existing WHERE existing.term = seed.term
);
