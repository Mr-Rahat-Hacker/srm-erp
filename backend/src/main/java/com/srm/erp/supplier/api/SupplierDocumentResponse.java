package com.srm.erp.supplier.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierDocumentResponse(
        UUID documentId,
        UUID supplierId,
        String documentType,
        String originalFilename,
        String contentType,
        int versionNumber,
        String reviewStatus,
        LocalDate expiryDate,
        String reviewRemarks,
        Instant uploadedAt,
        Instant reviewedAt
) {}
