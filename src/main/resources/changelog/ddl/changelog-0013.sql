ALTER TABLE public.medias
    ADD COLUMN media_type VARCHAR(20) NOT NULL;

ALTER TABLE public.medias
    ADD CONSTRAINT chk_medias_type
        CHECK (media_type IN ('IMAGE','VIDEO','AUDIO'));

