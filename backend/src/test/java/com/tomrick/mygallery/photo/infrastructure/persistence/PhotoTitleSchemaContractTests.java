package com.tomrick.mygallery.photo.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoTitleSchemaContractTests {

    @Test
    void entityKeepsVarcharLengthButAllowsNullTitles() throws NoSuchFieldException {
        var title = PhotoEntity.class.getDeclaredField("title");
        Column column = title.getAnnotation(Column.class);
        Size size = title.getAnnotation(Size.class);

        assertNotNull(column);
        assertTrue(column.nullable());
        assertEquals(200, column.length());
        assertNotNull(size);
        assertEquals(200, size.max());
        assertNull(title.getAnnotation(NotBlank.class));
    }

    @Test
    void v2RelaxesOnlyTheTitleNullabilityAndNonblankConstraint() throws IOException {
        String v1 = migration("V1__create_photos.sql");
        String v2 = migration("V2__allow_null_photo_titles.sql");

        assertTrue(v1.contains("title VARCHAR(200) NOT NULL"));
        assertTrue(v2.contains("ALTER COLUMN title DROP NOT NULL"));
        assertTrue(v2.contains("DROP CONSTRAINT ck_photos_title_nonblank"));
        assertTrue(v2.contains("CHECK (title IS NULL OR btrim(title) <> '')"));
        assertFalse(v2.toUpperCase().contains("UPDATE PHOTOS"));
    }

    private static String migration(String name) throws IOException {
        String path = "db/migration/" + name;
        try (InputStream input = PhotoTitleSchemaContractTests.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            assertNotNull(input, path + " must exist");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
