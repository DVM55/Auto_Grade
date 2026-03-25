ALTER TABLE public.quizzes
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE public.quizzes
    ADD CONSTRAINT chk_quizzes_status
        CHECK (status IN ('DRAFT','PUBLISHED'));