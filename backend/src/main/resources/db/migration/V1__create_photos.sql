CREATE TABLE photos (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    taken_at DATE NOT NULL,
    location VARCHAR(200),
    cloudinary_public_id VARCHAR(255) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    camera VARCHAR(150),
    lens VARCHAR(200),
    focal_length NUMERIC(8, 2),
    aperture NUMERIC(5, 2),
    shutter_speed NUMERIC(16, 9),
    iso INTEGER,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_photos_cloudinary_public_id UNIQUE (cloudinary_public_id),
    CONSTRAINT ck_photos_title_nonblank CHECK (btrim(title) <> ''),
    CONSTRAINT ck_photos_cloudinary_id_nonblank CHECK (btrim(cloudinary_public_id) <> ''),
    CONSTRAINT ck_photos_dimensions CHECK (width > 0 AND height > 0),
    CONSTRAINT ck_photos_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_photos_focal_length CHECK (focal_length IS NULL OR focal_length > 0),
    CONSTRAINT ck_photos_aperture CHECK (aperture IS NULL OR aperture > 0),
    CONSTRAINT ck_photos_shutter_speed CHECK (shutter_speed IS NULL OR shutter_speed > 0),
    CONSTRAINT ck_photos_iso CHECK (iso IS NULL OR iso > 0)
);

CREATE INDEX idx_photos_public_taken_at
    ON photos (taken_at DESC, created_at DESC, id DESC)
    WHERE visibility = 'PUBLIC';

CREATE INDEX idx_photos_public_featured_taken_at
    ON photos (taken_at DESC, created_at DESC, id DESC)
    WHERE visibility = 'PUBLIC' AND featured = TRUE;
