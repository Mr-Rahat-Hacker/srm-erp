package com.srm.erp.rfq.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RfqCreateRequestTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void rejectsAnRFQWithoutLines() {
        RfqCreateRequest request = request("INR", List.of());

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> "lines".equals(error.getPropertyPath().toString())));
    }

    @Test
    void validatesCurrencyAndNestedLineFields() {
        RfqLineRequest invalidLine = new RfqLineRequest(
                " ", " ", null, BigDecimal.ZERO, "", null, null);
        RfqCreateRequest request = request("RUPEES", List.of(invalidLine));

        Set<?> violations = validator.validate(request);

        assertTrue(violations.size() >= 4);
    }

    @Test
    void acceptsACompleteRFQWithAFutureDeadline() {
        RfqLineRequest line = new RfqLineRequest(
                "SAP-MAT-100", "Steel mounting bracket", "REV-A",
                new BigDecimal("25.0000"), "EA", "PLANT-01", "Zinc coated");
        RfqCreateRequest request = request("INR", List.of(line));

        assertFalse(validator.validate(request).iterator().hasNext());
    }

    private RfqCreateRequest request(String currencyCode, List<RfqLineRequest> lines) {
        return new RfqCreateRequest("Bracket sourcing", "Prototype order", "PRJ-42", "Engineering",
                "PLANT-01", "PG-01", currencyCode, Instant.now().plusSeconds(3600),
                LocalDate.now().plusDays(30), "Net 30", "DAP", lines);
    }
}
