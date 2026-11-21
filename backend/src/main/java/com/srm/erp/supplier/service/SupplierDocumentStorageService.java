package com.srm.erp.supplier.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class SupplierDocumentStorageService {
    private final Path root;

    public SupplierDocumentStorageService(
            @Value("${srm.storage.documents-dir:./var/srm-documents}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to initialize private document storage", exception);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "A document file is required");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(BAD_REQUEST, "Document size must not exceed 10 MB");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        byte[] signature;
        try {
            signature = file.getInputStream().readNBytes(8);
        } catch (IOException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded document");
        }

        String extension;
        if ("application/pdf".equals(contentType) && startsWith(signature, new byte[]{0x25,0x50,0x44,0x46,0x2d})) {
            extension = ".pdf";
        } else if ("image/png".equals(contentType) && startsWith(signature,
                new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a})) {
            extension = ".png";
        } else if ("image/jpeg".equals(contentType) && startsWith(signature,
                new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff})) {
            extension = ".jpg";
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "Only valid PDF, PNG or JPEG documents are accepted");
        }

        String key = UUID.randomUUID() + extension;
        Path target = root.resolve(key).normalize();
        if (!target.getParent().equals(root)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid document storage key");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unable to store uploaded document");
        }
    }

    public Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.getParent().equals(root) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Document file not found");
        }
        return target;
    }

    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (Exception ignored) {
            // A failed cleanup is handled by operational storage reconciliation.
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) return false;
        }
        return true;
    }
}
