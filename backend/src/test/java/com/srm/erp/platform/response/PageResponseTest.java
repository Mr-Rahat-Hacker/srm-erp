package com.srm.erp.platform.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageResponseTest {
    @Test
    void calculatesTotalPagesAndKeepsRequestedPageMetadata() {
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 1, 2, 3);

        assertEquals(List.of("a", "b"), response.items());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(3, response.totalElements());
        assertEquals(2, response.totalPages());
    }

    @Test
    void reportsZeroPagesForEmptyResults() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 25, 0);

        assertEquals(0, response.totalPages());
        assertEquals(0, response.totalElements());
    }
}
