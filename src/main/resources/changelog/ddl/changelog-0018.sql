-- ============================================================
-- quiz_attempts
-- ============================================================
CREATE TABLE public.quiz_attempts (
    id           BIGSERIAL PRIMARY KEY,
    quiz_id      BIGINT    NOT NULL,
    creator_id   BIGINT    NOT NULL,
    started_at   TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,
    expired_at   TIMESTAMP,
    status       VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
        CONSTRAINT chk_attempt_status
            CHECK (status IN ('IN_PROGRESS', 'SUBMITTED')),
    total_score  DOUBLE PRECISION,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP
);

ALTER TABLE public.quiz_attempts
    ADD CONSTRAINT fk_attempt_quiz
        FOREIGN KEY (quiz_id)
            REFERENCES public.quizzes(id)
            ON DELETE CASCADE;

ALTER TABLE public.quiz_attempts
    ADD CONSTRAINT fk_attempt_creator
        FOREIGN KEY (creator_id)
            REFERENCES public.accounts(id)
            ON DELETE CASCADE;

-- ============================================================
-- quiz_answers
-- ============================================================
CREATE TABLE public.quiz_answers (
    id          BIGSERIAL PRIMARY KEY,
    attempt_id  BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_text TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uq_answer_attempt_question UNIQUE (attempt_id, question_id)
);

ALTER TABLE public.quiz_answers
    ADD CONSTRAINT fk_answer_attempt
        FOREIGN KEY (attempt_id)
            REFERENCES public.quiz_attempts(id)
            ON DELETE CASCADE;

ALTER TABLE public.quiz_answers
    ADD CONSTRAINT fk_answer_question
        FOREIGN KEY (question_id)
            REFERENCES public.questions(id)
            ON DELETE CASCADE;

-- ============================================================
-- quiz_answer_options (bảng join ManyToMany)
-- ============================================================
CREATE TABLE public.quiz_answer_options (
    answer_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    CONSTRAINT pk_quiz_answer_options PRIMARY KEY (answer_id, option_id)
);

ALTER TABLE public.quiz_answer_options
    ADD CONSTRAINT fk_answer_option_answer
        FOREIGN KEY (answer_id)
            REFERENCES public.quiz_answers(id)
            ON DELETE CASCADE;

ALTER TABLE public.quiz_answer_options
    ADD CONSTRAINT fk_answer_option_option
        FOREIGN KEY (option_id)
            REFERENCES public.question_options(id)
            ON DELETE CASCADE;