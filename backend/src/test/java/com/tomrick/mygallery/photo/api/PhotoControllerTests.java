package com.tomrick.mygallery.photo.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PhotoControllerTests {

    private static final String PUBLIC_PHOTO_ID = "10000000-0000-0000-0000-000000000101";
    private static final String NEWEST_TIE_BREAKER_ID = "10000000-0000-0000-0000-000000000102";
    private static final String PRIVATE_PHOTO_ID = "10000000-0000-0000-0000-000000000901";
    private static final String UNKNOWN_PHOTO_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void photosReturnsOnlyPublicPhotosNewestFirstWithThePublicContract() throws Exception {
        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[0].id").value(NEWEST_TIE_BREAKER_ID))
                .andExpect(jsonPath("$[1].id").value(PUBLIC_PHOTO_ID))
                .andExpect(jsonPath("$[7].takenAt").value("2024-12-15"))
                .andExpect(jsonPath("$[*].id", not(hasItem(PRIVATE_PHOTO_ID))))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].month").value(8))
                .andExpect(jsonPath("$[0].orientation").value("portrait"))
                .andExpect(jsonPath("$[0].aspectRatio").value(2.0 / 3.0))
                .andExpect(jsonPath("$[2].orientation").value("square"))
                .andExpect(jsonPath("$[2].aspectRatio").value(1.0))
                .andExpect(jsonPath("$[0].image.thumbnailUrl").isString())
                .andExpect(jsonPath("$[0].image.cardUrl").isString())
                .andExpect(jsonPath("$[0].image.displayUrl").isString())
                .andExpect(jsonPath("$[0].imageUrl").doesNotExist())
                .andExpect(jsonPath("$[0].visibility").doesNotExist())
                .andExpect(jsonPath("$[0].cloudinaryPublicId").doesNotExist())
                .andExpect(jsonPath("$[0].originalUrl").doesNotExist())
                .andExpect(jsonPath("$[0].gps").doesNotExist())
                .andExpect(jsonPath("$[0].rawExif").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$[0].width").doesNotExist())
                .andExpect(jsonPath("$[0].height").doesNotExist());
    }

    @Test
    void featuredReturnsTheDeterministicPublicFeaturedPool() throws Exception {
        mockMvc.perform(get("/api/v1/photos/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(NEWEST_TIE_BREAKER_ID))
                .andExpect(jsonPath("$[1].id").value(PUBLIC_PHOTO_ID))
                .andExpect(jsonPath("$[2].id").value("10000000-0000-0000-0000-000000000104"))
                .andExpect(jsonPath("$[*].featured", everyItem(is(true))))
                .andExpect(jsonPath("$[*].id", not(hasItem(PRIVATE_PHOTO_ID))));
    }

    @Test
    void publicPhotoDetailReturnsAllowedMetadataWithoutInternalFields() throws Exception {
        mockMvc.perform(get("/api/v1/photos/{id}", PUBLIC_PHOTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PUBLIC_PHOTO_ID))
                .andExpect(jsonPath("$.orientation").value("landscape"))
                .andExpect(jsonPath("$.aspectRatio").value(1.5))
                .andExpect(jsonPath("$.camera").value("Example Camera X"))
                .andExpect(jsonPath("$.lens").value("Example 35mm Lens"))
                .andExpect(jsonPath("$.focalLength").value("35 mm"))
                .andExpect(jsonPath("$.aperture").value("f/2.8"))
                .andExpect(jsonPath("$.shutterSpeed").value("1/250 s"))
                .andExpect(jsonPath("$.iso").value(400))
                .andExpect(jsonPath("$.description").value("Late light across the pier."))
                .andExpect(jsonPath("$.visibility").doesNotExist())
                .andExpect(jsonPath("$.cloudinaryPublicId").doesNotExist())
                .andExpect(jsonPath("$.originalUrl").doesNotExist())
                .andExpect(jsonPath("$.gps").doesNotExist())
                .andExpect(jsonPath("$.rawExif").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.width").doesNotExist())
                .andExpect(jsonPath("$.height").doesNotExist());
    }

    @Test
    void privatePhotoDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/photos/{id}", PRIVATE_PHOTO_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownPhotoDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/photos/{id}", UNKNOWN_PHOTO_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiveGroupsOnlyPublicPhotosInDescendingOrderWithAccurateCounts() throws Exception {
        mockMvc.perform(get("/api/v1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[1].year").value(2025))
                .andExpect(jsonPath("$[2].year").value(2024))
                .andExpect(jsonPath("$[0].photoCount").value(4))
                .andExpect(jsonPath("$[0].months", hasSize(2)))
                .andExpect(jsonPath("$[0].months[0].month").value(8))
                .andExpect(jsonPath("$[0].months[0].label").value("AUGUST"))
                .andExpect(jsonPath("$[0].months[0].photoCount").value(3))
                .andExpect(jsonPath("$[0].months[0].photos[0].id").value(NEWEST_TIE_BREAKER_ID))
                .andExpect(jsonPath("$[0].months[0].photos[1].id").value(PUBLIC_PHOTO_ID))
                .andExpect(jsonPath("$[0].months[0].photos[2].takenAt").value("2026-08-02"))
                .andExpect(jsonPath("$[0].months[1].month").value(7))
                .andExpect(jsonPath("$[0].months[1].photoCount").value(1))
                .andExpect(jsonPath("$[0].months[1].photos", hasSize(1)))
                .andExpect(jsonPath("$[1].photoCount").value(3))
                .andExpect(jsonPath("$[1].months[0].month").value(11))
                .andExpect(jsonPath("$[1].months[1].month").value(3))
                .andExpect(jsonPath("$[1].months[1].photoCount").value(1))
                .andExpect(jsonPath("$[1].months[1].photos", hasSize(1)))
                .andExpect(jsonPath("$[2].photoCount").value(1))
                .andExpect(jsonPath("$[2].months", hasSize(1)))
                .andExpect(jsonPath("$[2].months[0].photos", hasSize(1)))
                .andExpect(jsonPath("$..id", not(hasItem(PRIVATE_PHOTO_ID))));
    }
}
