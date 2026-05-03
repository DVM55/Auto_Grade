ALTER TABLE public.quizzes
DROP COLUMN class_id;

ALTER TABLE public.quizzes
    ADD COLUMN access_type VARCHAR(10) NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE public.quizzes
    ADD CONSTRAINT chk_access_type
        CHECK (access_type IN ('PUBLIC', 'PRIVATE'));