-- ============================================================
-- quiz_question_configs: cấu hình random câu hỏi cho quiz
-- ============================================================
CREATE TABLE public.quiz_question_configs (
    id                   BIGSERIAL PRIMARY KEY,
    quiz_id              BIGINT NOT NULL,
    category_question_id BIGINT,
    group_question_id    BIGINT,
    quantity             INTEGER NOT NULL CHECK (quantity > 0),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE public.quiz_question_configs
    ADD CONSTRAINT fk_qqc_quiz
        FOREIGN KEY (quiz_id) REFERENCES public.quizzes(id) ON DELETE CASCADE;

ALTER TABLE public.quiz_question_configs
    ADD CONSTRAINT fk_qqc_category
        FOREIGN KEY (category_question_id) REFERENCES public.category_questions(id) ON DELETE SET NULL;

ALTER TABLE public.quiz_question_configs
    ADD CONSTRAINT fk_qqc_group
        FOREIGN KEY (group_question_id) REFERENCES public.group_questions(id) ON DELETE SET NULL;

-- ============================================================
-- attempt_questions: câu hỏi đã random lưu lại cho từng attempt
-- ============================================================
CREATE TABLE public.attempt_questions (
    id                   BIGSERIAL PRIMARY KEY,
    attempt_id           BIGINT NOT NULL,
    question_id          BIGINT NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id)
);

ALTER TABLE public.attempt_questions
    ADD CONSTRAINT fk_aq_attempt
        FOREIGN KEY (attempt_id) REFERENCES public.quiz_attempts(id) ON DELETE CASCADE;

ALTER TABLE public.attempt_questions
    ADD CONSTRAINT fk_aq_question
        FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;

ALTER TABLE public.quizzes
    ADD COLUMN is_random BOOLEAN NOT NULL DEFAULT FALSE;