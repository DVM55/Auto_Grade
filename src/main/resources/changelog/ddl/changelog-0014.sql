ALTER TABLE public.questions
    ADD COLUMN media_type VARCHAR(20);

ALTER TABLE public.questions
    ADD CONSTRAINT chk_questions_media_type
        CHECK (
            media_type IN ('IMAGE','VIDEO','AUDIO')
                OR media_type IS NULL
            );