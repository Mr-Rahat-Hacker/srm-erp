package com.srm.erp.supplier.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SupplierDocumentServiceTest {
    @Test
    void supplierCannotListAnotherOrganizationsDocuments() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SupplierDocumentStorageService storage = mock(SupplierDocumentStorageService.class);
        UUID supplierId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        doReturn(false).when(jdbc).queryForObject(
                contains("supplier_user_membership"), eq(Boolean.class), eq(supplierId), eq(actorId));

        SupplierDocumentService service = new SupplierDocumentService(jdbc, storage);
        assertThrows(ResponseStatusException.class, () -> service.findAllForSupplierUser(supplierId, actorId));

        verify(jdbc).queryForObject(
                contains("supplier_user_membership"), eq(Boolean.class), eq(supplierId), eq(actorId));
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void supplierCannotDownloadAnotherOrganizationsDocument() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SupplierDocumentStorageService storage = mock(SupplierDocumentStorageService.class);
        UUID supplierId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        doReturn(false).when(jdbc).queryForObject(
                contains("supplier_user_membership"), eq(Boolean.class), eq(supplierId), eq(actorId));

        SupplierDocumentService service = new SupplierDocumentService(jdbc, storage);
        assertThrows(ResponseStatusException.class,
                () -> service.findFileForSupplierUser(supplierId, documentId, actorId));

        verify(jdbc).queryForObject(
                contains("supplier_user_membership"), eq(Boolean.class), eq(supplierId), eq(actorId));
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void supplierCannotUploadToAnotherOrganizationsDocuments() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SupplierDocumentStorageService storage = mock(SupplierDocumentStorageService.class);
        UUID supplierId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String storageKey = "private-document.pdf";
        doReturn(false).when(jdbc).queryForObject(
                contains("supplier_user_membership"), eq(Boolean.class), eq(supplierId), eq(actorId));

        SupplierDocumentService service = new SupplierDocumentService(jdbc, storage);
        assertThrows(ResponseStatusException.class, () -> service.uploadForSupplierUser(
                supplierId, actorId, "GST_CERTIFICATE", "gst.pdf", "application/pdf",
                storageKey, null));

        verify(jdbc).queryForObject(
                contains("supplier_user_membership"), eq(Boolean.class), eq(supplierId), eq(actorId));
        verifyNoMoreInteractions(jdbc);
        verify(storage).deleteQuietly(storageKey);
        verifyNoMoreInteractions(storage);
    }
}
