-- enum 적용 전에 저장된 외부 API 대분류를 현재 정책 분류인 기타 주거로 변환합니다.
-- 이미 변환된 DB에서 다시 실행해도 변경되는 행이 없는 멱등 스크립트입니다.
UPDATE policy
SET category = 'OTHER'
WHERE category = '주거';
