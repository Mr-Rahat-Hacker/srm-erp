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

## Create the first Super Admin (development bootstrap)

A one-time bootstrap runner is available. It is disabled by default, has no HTTP endpoint, and creates an account only when `srm.app_user` is completely empty. It assigns only the `SUPER_ADMIN` role and writes an audit event. It never prints or stores the raw password.

In the Codespaces backend terminal, run the following commands. Use your own email, full name, and a unique password (at least 12 characters). The password is read silently rather than typed into shell history:

```bash
cd /workspaces/srm-erp/backend
read -rp "Admin email: " SRM_BOOTSTRAP_ADMIN_EMAIL
read -rp "Admin full name: " SRM_BOOTSTRAP_ADMIN_FULL_NAME
read -rsp "Create a strong password (12+ characters): " SRM_BOOTSTRAP_ADMIN_PASSWORD
echo
export SRM_BOOTSTRAP_ENABLED=true
export SRM_BOOTSTRAP_ADMIN_EMAIL SRM_BOOTSTRAP_ADMIN_FULL_NAME SRM_BOOTSTRAP_ADMIN_PASSWORD
mvn spring-boot:run
```

Wait for the log message confirming that the first administrator was provisioned. Stop the backend with Ctrl+C, then remove the bootstrap variables:

```bash
unset SRM_BOOTSTRAP_ENABLED SRM_BOOTSTRAP_ADMIN_EMAIL SRM_BOOTSTRAP_ADMIN_FULL_NAME SRM_BOOTSTRAP_ADMIN_PASSWORD
mvn spring-boot:run
```

Keep the second backend process running. Open the Angular app and sign in at `/login` using the email and password you just created. Do not share the password in chat, commit it, or put it in a screenshot.

**Important safeguards:** if any application user already exists, bootstrap skips without changing accounts. It does not reset passwords, add a role to an existing user, or create a second administrator. For an existing database, use a trusted DBA/admin provisioning procedure instead. Never enable this bootstrap in production; use a managed secret store and a controlled identity-provisioning process.

## Trusted user provisioning
There is deliberately no public user/admin creation endpoint and no default administrator password.
For subsequent users, create accounts through a trusted provisioning process. Store only a BCrypt hash in `srm.app_user.password_hash`,
set the account to `ACTIVE` only after identity verification, then assign the minimum required role(s)
using `srm.user_role`. The seven role codes are seeded by migration V3.

## API
- `POST /api/v1/auth/login` with JSON `{"email":"user@example.com","password":"..." }`
- Successful response contains `data.accessToken`, `data.tokenType`, `data.expiresIn`, user details and roles.
- Invalid credentials and inactive/unassigned accounts receive a generic 401 response.

## Security follow-ups before production
Implement refresh-token rotation/revocation, account lockout and rate limiting, email verification,
password reset, token revocation strategy, resource-level supplier membership checks, and security tests.
This milestone is an initial IAM foundation, not a production security certification.
