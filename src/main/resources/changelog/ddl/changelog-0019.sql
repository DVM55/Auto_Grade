ALTER TABLE public.quiz_attempts
    ADD COLUMN correct_count INTEGER;

ALTER TABLE public.quiz_answers
    ADD COLUMN is_correct BOOLEAN DEFAULT FALSE;