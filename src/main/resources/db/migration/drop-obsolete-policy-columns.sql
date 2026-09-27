-- 정책 동기화에서 더 이상 사용하지 않는 파생·미사용 컬럼을 제거합니다.
ALTER TABLE policy DROP COLUMN IF EXISTS subtype_code;
ALTER TABLE policy DROP COLUMN IF EXISTS region_condition;
