-- 개발·시연용 가상 데이터: TERM 50행, 나머지 8개 테이블 각 10행 = 총 130행 (빈 DB 기준).
-- 로컬 앱 시작 시 실행합니다. DB_SEED_MODE=never로 끌 수 있으며, 기존 행은 용어 기본 문구 정제 외에는 수정·삭제하지 않습니다.
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
-- apply_period_code: SPECIFIC_PERIOD(특정기간)
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
SELECT seed.policy_id, seed.policy_name, seed.category, seed.description, seed.support_content, seed.housing_type, '청년,주거,시연', 19, 39, TRUE, 'UNKNOWN', 'SPECIFIC_PERIOD', '실제 신청할 수 없는 개발·시연용 가상 정책입니다.', CAST('2026-01-01' AS DATE), CAST('2027-12-31' AS DATE), '시연용 링크이며 신청 기능은 제공하지 않습니다.', 'https://example.com/hyeja-demo/apply', 'https://example.com/hyeja-demo', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
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

-- 기존 기본 문구가 그대로인 용어만 정제하여, 개발자가 직접 수정한 데이터는 보존합니다.
UPDATE term
SET easy_description = CASE term
        WHEN '무주택자' THEN '정책에서 정한 주택 소유 범위에 따라 주택을 소유하지 않은 사람입니다. 부모의 주택 포함 여부 등은 공고마다 확인해야 합니다.'
        WHEN '보증금' THEN '계약을 지키기 위해 맡겼다가 계약 종료 시 돌려받는 돈입니다.'
        WHEN '월세' THEN '집을 빌린 대가로 매달 내는 돈입니다. 관리비는 일반적으로 별도입니다.'
        WHEN '전세' THEN '큰 보증금을 맡기고 계약기간 동안 거주하며 일반적으로 매달 월세를 내지 않는 임대차 방식입니다.'
        WHEN '공공임대' THEN '국가·지자체·공공기관 등이 공급하거나 지원하는 임대주택입니다. 종류마다 자격과 임대기간이 다릅니다.'
        WHEN '청약' THEN '공급되는 주택을 분양받거나 임대받기 위해 정해진 절차에 따라 신청하는 것입니다.'
        WHEN '임차인' THEN '돈을 내고 주택을 빌려 사용하는 사람, 즉 세입자입니다.'
        WHEN '임대인' THEN '주택을 다른 사람에게 빌려주는 사람, 즉 집주인입니다.'
        WHEN '전입신고' THEN '이사한 주소를 주민등록상 새 거주지로 신고하는 절차입니다.'
        WHEN '중위소득' THEN '정부가 가구 소득을 비교하기 위해 정하는 가운데 기준값입니다. 정책에서는 가구원 수별 금액을 사용합니다.'
        ELSE easy_description
    END,
    example = CASE term
        WHEN '무주택자' THEN '신청일 현재 무주택자'
        WHEN '보증금' THEN '보증금 1,000만원'
        WHEN '월세' THEN '월세 60만원 이하'
        WHEN '전세' THEN '전세보증금 1억원'
        WHEN '공공임대' THEN '공공임대 입주자 모집'
        WHEN '청약' THEN '모집공고를 확인하고 청약 신청'
        WHEN '임차인' THEN '임차인이 보증료를 납부'
        WHEN '임대인' THEN '임대인이 보증금을 반환'
        WHEN '전입신고' THEN '입주 후 전입신고 완료'
        WHEN '중위소득' THEN '기준 중위소득 60% 이하'
        ELSE example
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE (term = '무주택자' AND easy_description = '본인 명의 주택을 소유하지 않은 사람을 뜻합니다. 실제 판단 기준은 정책마다 확인해야 합니다.' AND example = '시연 예시: 본인 명의 주택이 없는 청년')
   OR (term = '보증금' AND easy_description = '임대차 계약에서 임차인이 임대인에게 맡기는 금액입니다.' AND example = '시연 예시: 보증금 500만원')
   OR (term = '월세' AND easy_description = '집을 빌리는 대가로 매달 내는 금액입니다.' AND example = '시연 예시: 매달 월세 40만원')
   OR (term = '전세' AND easy_description = '보증금을 맡기고 약정한 기간 동안 집을 사용하는 임대차 방식입니다.' AND example = '시연 예시: 전세 보증금 1억원')
   OR (term = '공공임대' AND easy_description = '공공기관 등이 공급하는 임대주택입니다. 입주 조건은 공고마다 다릅니다.' AND example = '시연 예시: 공공임대 입주 공고 확인')
   OR (term = '청약' AND easy_description = '공급하는 주택에 입주하거나 분양받기 위해 신청하는 절차입니다.' AND example = '시연 예시: 모집 공고에 따라 청약 신청')
   OR (term = '임차인' AND easy_description = '집이나 건물을 빌려 사용하는 사람입니다.' AND example = '시연 예시: 월세 집에 거주하는 계약자')
   OR (term = '임대인' AND easy_description = '집이나 건물을 다른 사람에게 빌려주는 사람입니다.' AND example = '시연 예시: 임대차 계약의 집주인')
   OR (term = '전입신고' AND easy_description = '새로운 거주지로 이사한 사실을 관할 기관에 신고하는 절차입니다.' AND example = '시연 예시: 이사 후 거주지 변경 신고')
   OR (term = '중위소득' AND easy_description = '우리나라 모든 가구를 소득 순서로 세웠을 때 가운데 가구의 소득입니다.' AND example = '시연 예시: 중위소득 60% 이하');

INSERT INTO term (term, easy_description, example, created_at, updated_at, deleted_at)
SELECT seed.term, seed.easy_description, seed.example, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
FROM (
    SELECT '중위소득' AS term, '정부가 가구 소득을 비교하기 위해 정하는 가운데 기준값입니다. 정책에서는 가구원 수별 금액을 사용합니다.' AS easy_description, '기준 중위소득 60% 이하' AS example
    UNION ALL SELECT '도시근로자 가구당 월평균소득', '전년도 도시 근로자 가구의 월소득을 가구원 수별로 평균 낸 금액입니다. 공공주택 자격을 판단할 때 자주 사용합니다.', '1인 가구 월평균소득 100% 이하'
    UNION ALL SELECT '소득인정액', '실제 소득에 재산을 소득으로 바꿔 계산한 금액을 더하고, 인정되는 공제를 반영한 값입니다.', '소득인정액이 선정 기준 이하인 가구'
    UNION ALL SELECT '소득평가액', '근로·사업소득 등 실제 소득에서 정책상 인정되는 공제를 뺀 금액입니다.', '청년가구 소득평가액 기준 충족'
    UNION ALL SELECT '총자산', '부동산·금융자산·자동차 등 정책이 정한 재산을 합한 금액입니다. 포함 범위는 정책마다 다릅니다.', '총자산 기준 이하'
    UNION ALL SELECT '순자산', '총자산에서 정책이 인정하는 부채를 뺀 금액입니다.', '부부합산 순자산 기준 충족'
    UNION ALL SELECT '무주택자', '정책에서 정한 주택 소유 범위에 따라 주택을 소유하지 않은 사람입니다. 부모의 주택 포함 여부 등은 공고마다 확인해야 합니다.', '신청일 현재 무주택자'
    UNION ALL SELECT '무주택세대구성원', '신청자뿐 아니라 정책이 정한 세대구성원 모두가 주택을 소유하지 않은 상태입니다.', '세대구성원 전원이 무주택'
    UNION ALL SELECT '세대주', '주민등록표에서 한 세대를 대표하는 사람입니다.', '신청일 현재 세대주'
    UNION ALL SELECT '예비세대주', '현재는 세대주가 아니지만 대출 실행일이나 입주 전까지 세대주가 될 예정인 사람입니다.', '대출 실행 후 세대주 전환 예정'
    UNION ALL SELECT '단독세대주', '주민등록상 본인만으로 세대를 구성한 세대주입니다. 상품마다 인정 범위가 다를 수 있습니다.', '만 34세 이하 단독세대주'
    UNION ALL SELECT '원가구', '청년가구에 부모를 포함해 판단하는 가구 범위입니다. 청년 월세지원에서 주로 사용합니다.', '원가구 중위소득 100% 이하'
    UNION ALL SELECT '청년가구', '청년 본인과 배우자·자녀, 같은 주소에 사는 민법상 가족 등을 포함하는 가구 범위입니다.', '청년가구 중위소득 60% 이하'
    UNION ALL SELECT '차상위계층', '기초생활수급자는 아니지만 소득·재산이 낮아 법이나 사업에서 지원 대상으로 인정된 계층입니다.', '기초·차상위 대학생'
    UNION ALL SELECT '기초생활수급자', '생계·의료·주거·교육급여 중 하나 이상을 받도록 결정된 사람이나 가구입니다.', '생계·의료급여 수급자 가구'
    UNION ALL SELECT '자립준비청년', '가정위탁이나 아동복지시설의 보호가 종료되어 홀로서기를 준비하는 청년입니다.', '보호 종료 후 5년 이내 자립준비청년'
    UNION ALL SELECT '보증금', '계약을 지키기 위해 맡겼다가 계약 종료 시 돌려받는 돈입니다.', '보증금 1,000만원'
    UNION ALL SELECT '임차보증금', '세입자가 임대차계약을 맺으며 집주인에게 맡기는 보증금입니다.', '임차보증금 3억원 이하'
    UNION ALL SELECT '임대보증금', '공공·민간 임대주택에 입주할 때 임대사업자에게 맡기는 보증금입니다.', '임대보증금의 5% 부담'
    UNION ALL SELECT '월세', '집을 빌린 대가로 매달 내는 돈입니다. 관리비는 일반적으로 별도입니다.', '월세 60만원 이하'
    UNION ALL SELECT '월차임', '법률·계약서에서 월세를 가리키는 표현입니다.', '월차임 납부확인서'
    UNION ALL SELECT '전세', '큰 보증금을 맡기고 계약기간 동안 거주하며 일반적으로 매달 월세를 내지 않는 임대차 방식입니다.', '전세보증금 1억원'
    UNION ALL SELECT '보증부월세', '보증금과 월세를 함께 내는 임대차 방식입니다.', '보증금 500만원, 월세 40만원'
    UNION ALL SELECT '임차인', '돈을 내고 주택을 빌려 사용하는 사람, 즉 세입자입니다.', '임차인이 보증료를 납부'
    UNION ALL SELECT '임대인', '주택을 다른 사람에게 빌려주는 사람, 즉 집주인입니다.', '임대인이 보증금을 반환'
    UNION ALL SELECT '임대차계약', '임대인이 주택을 빌려주고 임차인이 보증금이나 월세를 지급하기로 한 계약입니다.', '본인 명의 임대차계약'
    UNION ALL SELECT '전입신고', '이사한 주소를 주민등록상 새 거주지로 신고하는 절차입니다.', '입주 후 전입신고 완료'
    UNION ALL SELECT '확정일자', '임대차계약서가 특정 날짜에 존재했다는 사실을 공적으로 확인받는 절차입니다.', '계약서에 확정일자 받기'
    UNION ALL SELECT '대항력', '집주인이 바뀌어도 세입자가 임대차관계를 주장할 수 있는 권리입니다. 보통 주택 점유와 전입신고가 중요합니다.', '입주와 전입신고로 대항력 갖추기'
    UNION ALL SELECT '우선변제권', '주택이 경매·공매될 때 일정 요건을 갖춘 세입자가 보증금을 다른 채권자보다 먼저 돌려받을 수 있는 권리입니다.', '대항력과 확정일자 갖추기'
    UNION ALL SELECT '최우선변제금', '소액임차인이 일정 요건을 충족하면 다른 담보권보다 먼저 돌려받을 수 있는 보증금 일부입니다.', '지역별 최우선변제금 확인'
    UNION ALL SELECT '선순위채권', '세입자의 보증금보다 먼저 변제받을 권리가 있는 빚이나 보증금입니다.', '선순위채권과 전세보증금의 합'
    UNION ALL SELECT '근저당권', '집주인의 대출 등을 담보하기 위해 부동산에 설정되는 권리입니다. 등기부등본에서 확인할 수 있습니다.', '채권최고액이 설정된 근저당권'
    UNION ALL SELECT '전월세전환율', '전세보증금을 월세로, 또는 월세를 보증금으로 바꿔 계산할 때 사용하는 비율입니다.', '월세를 환산보증금으로 계산'
    UNION ALL SELECT '주택가액', '정책이나 보증기관이 정한 방식으로 평가한 주택의 가격입니다. 실제 거래가격과 다를 수 있습니다.', '선순위채권이 주택가액의 일정 비율 이하'
    UNION ALL SELECT '공공임대', '국가·지자체·공공기관 등이 공급하거나 지원하는 임대주택입니다. 종류마다 자격과 임대기간이 다릅니다.', '공공임대 입주자 모집'
    UNION ALL SELECT '매입임대', '공공기관이 기존 주택을 사들여 자격을 갖춘 사람에게 저렴하게 빌려주는 주택입니다.', '청년 매입임대'
    UNION ALL SELECT '전세임대', '입주자가 찾은 주택을 공공기관이 집주인과 전세계약한 뒤 입주자에게 다시 빌려주는 방식입니다.', '청년 전세임대'
    UNION ALL SELECT '행복주택', '청년·신혼부부 등에게 직장이나 학교와 가까운 곳의 주택을 공급하는 공공임대 유형입니다.', '행복주택 청년계층'
    UNION ALL SELECT '공공분양', '공공기관이 공급하는 분양주택입니다. 소득·자산·무주택 요건 등이 적용될 수 있습니다.', '뉴:홈 공공분양'
    UNION ALL SELECT '특별공급', '청년·신혼부부·생애최초 등 정책상 배려가 필요한 대상에게 주택 물량 일부를 별도로 공급하는 방식입니다.', '청년 특별공급'
    UNION ALL SELECT '청약', '공급되는 주택을 분양받거나 임대받기 위해 정해진 절차에 따라 신청하는 것입니다.', '모집공고를 확인하고 청약 신청'
    UNION ALL SELECT '청약통장', '주택 청약 자격과 순위 등을 인정받기 위해 가입하고 납입하는 통장입니다.', '청약통장 가입 6개월 이상'
    UNION ALL SELECT '전용면적', '공동주택에서 한 가구가 독립적으로 사용하는 내부 면적입니다. 공용 복도·계단 등은 제외됩니다.', '전용면적 60㎡ 이하'
    UNION ALL SELECT '모집공고일', '신청 자격의 나이·주소·무주택 여부 등을 판단하는 기준일로 주로 사용되는 공고 게시일입니다.', '모집공고일 현재 무주택'
    UNION ALL SELECT '예비입주자', '당장 계약할 입주자는 아니지만 빈집이 생기면 순서에 따라 입주할 수 있도록 선정된 사람입니다.', '청년 매입임대 예비입주자'
    UNION ALL SELECT '분양권', '아직 완공되지 않은 주택 등을 분양받아 입주할 수 있는 권리입니다. 정책에 따라 주택 보유로 판단될 수 있습니다.', '분양권 보유 여부 확인'
    UNION ALL SELECT '조합원 입주권', '재개발·재건축 조합원이 새로 지은 주택에 입주할 수 있는 권리입니다. 정책에 따라 주택 보유로 판단될 수 있습니다.', '조합원 입주권 보유자 제외'
    UNION ALL SELECT '전세보증금반환보증', '계약 종료 후 집주인이 보증금을 돌려주지 못할 때 보증기관이 정해진 범위에서 대신 지급하는 보증입니다.', 'HUG 전세보증금반환보증 가입'
    UNION ALL SELECT '보증료', '보증기관의 보증을 이용하기 위해 내는 비용입니다. 보증금액·기간·주택유형 등에 따라 달라집니다.', '납부한 반환보증 보증료 지원'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM term existing WHERE existing.term = seed.term
);
