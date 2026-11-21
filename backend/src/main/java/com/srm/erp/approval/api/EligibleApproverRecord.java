package com.srm.erp.approval.api;

import java.util.UUID;

public record EligibleApproverRecord(UUID userId, String fullName, String email) {}
