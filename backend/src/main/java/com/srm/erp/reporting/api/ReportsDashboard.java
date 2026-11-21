package com.srm.erp.reporting.api;

import java.util.Map;

public record ReportsDashboard(
        String audience,
        long supplierInvitations,
        long activeRfqs,
        long submittedQuotations,
        long openNegotiations,
        long pendingApprovals,
        long awards,
        Map<String, Long> supplierStatuses,
        Map<String, Long> rfqStatuses
) {}
