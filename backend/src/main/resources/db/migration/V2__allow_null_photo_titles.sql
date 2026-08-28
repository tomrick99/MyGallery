ALTER TABLE photos
    ALTER COLUMN title DROP NOT NULL;

ALTER TABLE photos
    DROP CONSTRAINT ck_photos_title_nonblank;

ALTER TABLE photos
    ADD CONSTRAINT ck_photos_title_nonblank
        CHECK (title IS NULL OR btrim(title) <> '');
