-- 임시 소득 구간 코드를 확정된 IncomeRange enum 이름으로 변환합니다.
-- 기존 INC_40_UP은 세부 금액을 알 수 없어 새 구간 중 하한이 같은 R4000_5000으로 변환합니다.
UPDATE profile
SET income_range_code = CASE income_range_code
    WHEN 'INC_0_20' THEN 'UNDER_2000'
    WHEN 'INC_20_30' THEN 'R2000_3000'
    WHEN 'INC_30_40' THEN 'R3000_4000'
    WHEN 'INC_40_UP' THEN 'R4000_5000'
    ELSE income_range_code
END
WHERE income_range_code IN ('INC_0_20', 'INC_20_30', 'INC_30_40', 'INC_40_UP');
