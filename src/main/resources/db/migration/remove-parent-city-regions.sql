-- 구가 있는 시의 상위 시 코드(예: 41110 경기도 수원시)를 지역에서 뺍니다.
-- 온통청년 정책 지역(zipCd)은 구 코드(41111 수원시 장안구 등)로만 오므로, 회원이 상위 시를 고르면 어떤 정책과도 맞지 않습니다.
-- CSV(region_sigungu.csv)에서도 뺐고, 이미 적재된 DB에서는 여기서 지웁니다.
-- 회원 조건·정책이 참조 중인 행은 지우지 않습니다(외래 키). 반복 실행해도 결과가 같습니다.
DELETE FROM region
WHERE region_code IN ('41110', '41130', '41170', '41190', '41270', '41280', '41460', '41590',
                      '43110', '44130', '47110', '48120', '52110')
  AND NOT EXISTS (SELECT 1 FROM profile p WHERE p.region_code = region.region_code)
  AND NOT EXISTS (SELECT 1 FROM policy_region pr WHERE pr.region_code = region.region_code);
