package com.srm.erp.supplier.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.supplier.service.SupplierDocumentService;
import com.srm.erp.supplier.service.SupplierDocumentStorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/documents")
public class SupplierDocumentController {
    private final SupplierDocumentService documents;
    private final SupplierDocumentStorageService storage;

    public SupplierDocumentController(SupplierDocumentService documents, SupplierDocumentStorageService storage) {
        this.documents = documents;
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SRM_ADMIN','SUPER_ADMIN')")
    public ApiResponse<SupplierDocumentResponse> upload(
            @PathVariable UUID supplierId,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) LocalDate expiryDate) {
        String key = storage.store(file);
        try {
            return ApiResponse.success("Supplier document uploaded for verification",
                    documents.upload(supplierId, currentActor(), documentType, file.getOriginalFilename(),
                            file.getContentType(), key, expiryDate));
        } catch (RuntimeException exception) {
            storage.deleteQuietly(key);
            throw exception;
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ApiResponse<List<SupplierDocumentResponse>> list(@PathVariable UUID supplierId) {
        return ApiResponse.success("Supplier documents retrieved successfully", documents.findAll(supplierId));
    }

    @GetMapping("/{documentId}/content")
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
    public ResponseEntity<FileSystemResource> download(@PathVariable UUID supplierId,
                                                        @PathVariable UUID documentId) {
        var file = documents.findFile(supplierId, documentId);
        Path path = storage.resolve(file.storageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename().replace("\"", "") + "\"")
                .header(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff")
                .body(new FileSystemResource(path));
    }

    @PostMapping("/{documentId}/review")
    @PreAuthorize("hasAnyRole('SUPPLIER_VERIFIER','SRM_ADMIN')")
    public ApiResponse<SupplierDocumentResponse> review(@PathVariable UUID supplierId,
            @PathVariable UUID documentId, @Valid @RequestBody SupplierDocumentReviewRequest request) {
        return ApiResponse.success("Supplier document review recorded",
                documents.review(supplierId, documentId, currentActor(), request.decision(), request.remarks()));
    }

    private UUID currentActor() {
        return UUID.fromString(org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName());
    }
}
