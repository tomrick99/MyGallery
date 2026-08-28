package com.tomrick.mygallery.common.web;

import com.jayway.jsonpath.JsonPath;
import com.tomrick.mygallery.photo.application.PhotoNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTests {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .addFilters(new RequestCorrelationFilter())
            .build();

    @Test
    void unexpectedApiErrorsAreCorrelatedAndSanitized() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists(RequestCorrelationFilter.HEADER_NAME))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(content().string(not(containsString("SQLException"))))
                .andExpect(content().string(not(containsString("photos_private_idx"))))
                .andExpect(content().string(not(containsString("/srv/mygallery"))))
                .andExpect(content().string(not(containsString("provider-secret"))))
                .andReturn();

        String headerRequestId = result.getResponse().getHeader(
                RequestCorrelationFilter.HEADER_NAME
        );
        String bodyRequestId = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.requestId"
        );
        assertEquals(headerRequestId, bodyRequestId);
    }

    @Test
    void knownPublicNotFoundIsNotSwallowedByTheGenericHandler() throws Exception {
        mockMvc.perform(get("/api/v1/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(RequestCorrelationFilter.HEADER_NAME))
                .andExpect(content().string(""));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/api/v1/test/unexpected")
        String unexpected() {
            throw new IllegalStateException(
                    "SQLException photos_private_idx /srv/mygallery provider-secret"
            );
        }

        @GetMapping("/api/v1/test/not-found")
        String notFound() {
            throw new PhotoNotFoundException();
        }
    }
}
