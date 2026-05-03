CREATE UNIQUE INDEX uix_attempt_one_in_progress
    ON public.quiz_attempts (quiz_id, creator_id)
    WHERE status = 'IN_PROGRESS';