ALTER TABLE policy MODIFY COLUMN houseless_yn VARCHAR(20) NULL;

UPDATE policy
SET houseless_yn = CASE
    WHEN houseless_yn IN ('1', 'true', 'TRUE', 'REQUIRED') THEN 'REQUIRED'
    WHEN houseless_yn IN ('0', 'false', 'FALSE', 'NOT_REQUIRED') THEN 'NOT_REQUIRED'
    ELSE 'UNKNOWN'
END;

ALTER TABLE policy MODIFY COLUMN houseless_yn VARCHAR(20) NOT NULL;
ALTER TABLE policy MODIFY COLUMN apply_period_code VARCHAR(20) NOT NULL;

UPDATE policy
SET apply_period_code = CASE
    WHEN TRIM(LEADING '0' FROM apply_period_code) = '57001' THEN 'SPECIFIC_PERIOD'
    WHEN TRIM(LEADING '0' FROM apply_period_code) = '57002' THEN 'ALWAYS'
    WHEN TRIM(LEADING '0' FROM apply_period_code) = '57003' THEN 'CLOSED'
    ELSE apply_period_code
END;
