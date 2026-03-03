-- 1️⃣ Drop foreign key cũ
ALTER TABLE public.answer_keys
DROP CONSTRAINT fk_answer_keys_exam;

-- 2️⃣ Drop unique constraint cũ
ALTER TABLE public.answer_keys
DROP CONSTRAINT uk_answer_keys_exam_paper;

-- 3️⃣ Rename column
ALTER TABLE public.answer_keys
    RENAME COLUMN exam_id TO exam_session_id;

-- 4️⃣ Tạo foreign key mới
ALTER TABLE public.answer_keys
    ADD CONSTRAINT fk_answer_keys_exam_session
        FOREIGN KEY (exam_session_id)
            REFERENCES public.exam_sessions(id)
            ON DELETE CASCADE;

-- 5️⃣ Tạo lại unique constraint mới
ALTER TABLE public.answer_keys
    ADD CONSTRAINT uk_answer_keys_session_paper
        UNIQUE (exam_session_id, paper_code);