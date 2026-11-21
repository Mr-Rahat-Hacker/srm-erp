# IAM setup (development)

## What is included
- Stateless JWT bearer authentication.
- BCrypt password verification (work factor 12).
- Seven seeded SRM roles are used as JWT authorities.
- Login is allowed only for accounts with `account_status = 'ACTIVE'` and at least one assigned role.
- Supplier list access is role-restricted. Public supplier self-registration remains available.
- All other API routes require authentication by default.

## Required configuration
Set `SRM_JWT_SECRET` to a random secret containing at least 32 UTF-8 bytes. Never commit production secrets.
The access token currently expires after 3600 seconds. The client must send:
`Authorization: Bearer <accessToken>`

## Trusted user provisioning
There is deliberately no public user/admin creation endpoint and no default administrator password.
Create users through a trusted provisioning process. Store only a BCrypt hash in `srm.app_user.password_hash`,
set the account to `ACTIVE` only after identity verification, then assign the minimum required role(s)
using `srm.user_role`. The seven role codes are seeded by migration V3.

Example BCrypt hash generation can be done in a trusted Spring application context using
`new BCryptPasswordEncoder(12).encode(rawPassword)`. Do not store or log the raw password.

## API
- `POST /api/v1/auth/login` with JSON `{"email":"user@example.com","password":"..." }`
- Successful response contains `data.accessToken`, `data.tokenType`, `data.expiresIn`, user details and roles.
- Invalid credentials and inactive/unassigned accounts receive a generic 401 response.

## Security follow-ups before production
Implement refresh-token rotation/revocation, account lockout and rate limiting, email verification,
password reset, token revocation strategy, resource-level supplier membership checks, and security tests.
This milestone is an initial IAM foundation, not a production security certification.
