# SAP PIR Integration Gateway Contract

The SRM application does not connect to the SAP database. After a buyer records an award, the buyer can queue a Purchase Info Record (PIR) upsert for the configured, authorized integration gateway.

## Configuration

Set these backend environment variables only after the gateway endpoint and credentials are approved:

- `SAP_PIR_ENABLED=true` enables the background outbox sender.
- `SAP_PIR_GATEWAY_URL` is the gateway's HTTPS endpoint.
- `SAP_PIR_GATEWAY_TOKEN` is a bearer token read at runtime and never stored in an integration event.
- `SAP_PIR_POLL_INTERVAL_MS` controls queue polling.

Email or approval links are not used to authorize SAP writes. A buyer must be authenticated, own the RFQ, have a recorded award, and the selected supplier must have an active SAP Vendor Code mapping for the chosen SAP system.

## Request contract

The gateway receives JSON with an idempotency key and approved award references. The adapter includes RFQ material codes and awarded quotation line prices; the gateway is responsible for validating and mapping these fields to the target SAP API or approved middleware operation.

```json
{
  "requestKey": "UUID",
  "operation": "UPSERT_PURCHASE_INFO_RECORD",
  "sapSystemCode": "configured-system",
  "awardId": "UUID",
  "rfqNumber": "RFQ-000000001",
  "supplierId": "UUID",
  "vendorCode": "existing-SAP-vendor-code",
  "purchasingGroup": "group-code",
  "plantCode": "plant-code",
  "quotationVersion": 1,
  "currencyCode": "INR",
  "totalLandedCost": 12500.00,
  "paymentTerms": "configured terms",
  "incoterms": "EXW",
  "items": [
    {
      "lineNumber": 1,
      "materialCode": "existing-SAP-material-code",
      "description": "RFQ line description",
      "quantity": 10.0000,
      "unit": "EA",
      "netUnitPrice": 1250.0000,
      "landedLineTotal": 12500.00
    }
  ]
}
```

The gateway must treat the `Idempotency-Key` HTTP header and payload `requestKey` as the same key. A successful 2xx response means the gateway accepted the request; it may return an `X-Integration-Request-Id` response header for reconciliation. SRM stores the request key, sanitized status, gateway reference and attempt history for audit.

## Delivery and retry

The outbox marks a claimed request as sending, then records success or a sanitized failure. Stale sending requests can be reclaimed using the same idempotency key. A failed request is visible to the RFQ buyer and can be explicitly retried after correcting configuration or source data. The gateway token and full exception messages are not written to the audit trail.

This contract is the SRM-to-gateway boundary. The final SAP API, field mapping, business validation, certificate policy and production credentials must be supplied by the authorized SAP integration owner.
