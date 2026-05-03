CREATE TABLE public.quiz_classes (
    quiz_id    BIGINT NOT NULL,
    class_id   BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- primary key (composite)
ALTER TABLE public.quiz_classes
    ADD CONSTRAINT pk_quiz_classes
        PRIMARY KEY (quiz_id, class_id);

-- foreign key -> quizzes
ALTER TABLE public.quiz_classes
    ADD CONSTRAINT fk_quiz_classes_quiz
        FOREIGN KEY (quiz_id)
            REFERENCES public.quizzes(id)
            ON DELETE CASCADE;

-- foreign key -> classes
ALTER TABLE public.quiz_classes
    ADD CONSTRAINT fk_quiz_classes_class
        FOREIGN KEY (class_id)
            REFERENCES public.classes(id)
            ON DELETE CASCADE;

ALTER TABLE public.quizzes
    ADD COLUMN auto_score BOOLEAN NOT NULL DEFAULT TRUE;