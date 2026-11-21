# API Documentation & Testing

The backend exposes an OpenAPI 3 specification and Swagger UI for interactive API testing.

## Run the backend

From the repository root:

```bash
cd backend
mvn spring-boot:run
```

The first run downloads the Springdoc OpenAPI dependency. Keep the backend terminal running.

## Open Swagger UI in GitHub Codespaces

Open the forwarded port **8080** and append:

```text
/swagger-ui/index.html
```

For example:

```text
https://YOUR-CODESPACE-8080.app.github.dev/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
https://YOUR-CODESPACE-8080.app.github.dev/v3/api-docs
```

## Initial test sequence

1. Run `GET /api/v1/health` and expect HTTP 200.
2. Run `GET /api/v1/suppliers` and expect HTTP 200 with an empty data array when no supplier exists.
3. Run `POST /api/v1/suppliers` with a valid registration body and expect HTTP 201.
4. Run `GET /api/v1/suppliers` again and verify the created record.
5. Test invalid request values and verify HTTP 400 validation responses.

## Sample supplier registration body

```json
{
  "legalName": "Apex Precision Components Private Limited",
  "tradeName": "Apex Precision",
  "gstin": "27ABCDE1234F1Z5",
  "pan": "ABCDE1234F",
  "contactName": "Ravi Sharma",
  "email": "ravi.sharma@example.com",
  "phone": "+91-9876543210",
  "addressLine1": "Plot 21, MIDC Industrial Area",
  "city": "Pune",
  "stateRegion": "Maharashtra",
  "postalCode": "411019",
  "countryCode": "IN"
}
```

Use test data only. Do not enter real supplier bank details, personal data, production credentials, or confidential quotations into a development environment.

Swagger UI is an API testing interface, not an authentication or authorization mechanism. Keep the development API private and do not expose it publicly for production use.
