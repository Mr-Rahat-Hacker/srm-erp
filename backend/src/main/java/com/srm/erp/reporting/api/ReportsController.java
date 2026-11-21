package com.srm.erp.reporting.api;

import com.srm.erp.platform.response.ApiResponse;
import com.srm.erp.reporting.service.ReportsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('SUPPLIER_USER','SUPPLIER_VERIFIER','PROCUREMENT_BUYER','PROCUREMENT_APPROVER','SRM_ADMIN','SUPER_ADMIN','AUDITOR')")
public class ReportsController {
    private final ReportsService reports;

    public ReportsController(ReportsService reports) {
        this.reports = reports;
    }

    @GetMapping("/dashboard")
    public ApiResponse<ReportsDashboard> dashboard() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID actorId = UUID.fromString(authentication.getName());
        Set<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());
        return ApiResponse.success("Dashboard report retrieved", reports.dashboard(actorId, roles));
    }
}
