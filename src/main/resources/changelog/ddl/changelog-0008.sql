ALTER TABLE user_details
DROP CONSTRAINT user_details_gender_check;

ALTER TABLE user_details
    ADD CONSTRAINT user_details_gender_check
        CHECK (
            gender IS NULL
                OR gender IN ('MALE', 'FEMALE', 'OTHER')
            );

ALTER TABLE public.accounts
    DROP CONSTRAINT accounts_username_key;