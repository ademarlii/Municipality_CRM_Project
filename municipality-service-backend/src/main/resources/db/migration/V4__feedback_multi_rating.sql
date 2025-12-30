-- 1) complaint_id tekil constraint kaldır
ALTER TABLE feedback DROP CONSTRAINT IF EXISTS feedback_complaint_id_key;

-- 2) NOT NULL yap
ALTER TABLE feedback ALTER COLUMN complaint_id SET NOT NULL;
ALTER TABLE feedback ALTER COLUMN citizen_id SET NOT NULL;
ALTER TABLE feedback ALTER COLUMN rating SET NOT NULL;

-- 3) yorum istemiyorsun: kolonu kaldır
ALTER TABLE feedback DROP COLUMN IF EXISTS comment;

-- 4) yeni unique
ALTER TABLE feedback
    ADD CONSTRAINT uq_feedback_complaint_citizen UNIQUE (complaint_id, citizen_id);

-- 5) check
ALTER TABLE feedback
    ADD CONSTRAINT ck_feedback_rating_range CHECK (rating BETWEEN 1 AND 5);

-- 6) index
CREATE INDEX IF NOT EXISTS idx_feedback_complaint ON feedback(complaint_id);
CREATE INDEX IF NOT EXISTS idx_feedback_citizen   ON feedback(citizen_id);
