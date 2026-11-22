package com.srm.erp.audit.api;

import com.srm.erp.audit.service.AuditSearchService;
import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.platform.response.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasAnyRole('AUDITOR','SRM_ADMIN','SUPER_ADMIN','PROCUREMENT_BUYER','PROCUREMENT_APPROVER')")
public class AuditSearchController {
    private final AuditSearchService audit;

    public AuditSearchController(AuditSearchService audit) {
        this.audit = audit;
    }

    @GetMapping
    public ApiResponse<List<AuditEventRecord>> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID rfqId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int limit) {
        SearchActor actor = currentActor();
        return ApiResponse.success("Audit events retrieved",
                audit.search(actor.userId(), actor.roles(), action, entityType, rfqId, actorUserId,
                        from, to, limit));
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<AuditEventRecord>> searchPage(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID rfqId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
        SearchActor actor = currentActor();
        return ApiResponse.success("Audit events retrieved",
                audit.searchPage(actor.userId(), actor.roles(), action, entityType, rfqId, actorUserId,
                        from, to, page, size));
    }

    private SearchActor currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID actorId = UUID.fromString(auth.getName());
        Set<String> roles = auth.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(java.util.stream.Collectors.toSet());
        return new SearchActor(actorId, roles);
    }

    private record SearchActor(UUID userId, Set<String> roles) {}
}
