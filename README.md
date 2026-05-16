# User Service

Spring Boot microservice for user registration, authentication, JWT issuance, refresh-token backed sessions, profile management, and admin user operations in the Ticketmaster-style backend platform.

This service is the identity boundary for the platform. Other services do not own user records; they trust JWT claims issued by user-service for user id, email, and role-based access decisions.

## Where It Fits

```text
customer / organiser / admin
      |
      | register, login, refresh, logout
      v
user-service
      |
      | JWT access token: sub, email, role, issuer, expiry
      v
event-management-service
booking-service
seats-allocation-service
payment-service
```

The service does not own events, seats, payments, bookings, or tickets. It owns user identity, account status, password verification, token issuance, and admin-level user control.

## What It Does Today

- Registers users with normalized email and BCrypt password hashing.
- Issues HMAC SHA-256 JWT access tokens.
- Includes `sub`, `email`, `role`, `iss`, `iat`, and `exp` claims in access tokens.
- Creates opaque refresh tokens and stores only SHA-256 hashes.
- Revokes older active refresh tokens when a new login session is created.
- Refreshes access tokens using a valid refresh token.
- Revokes refresh tokens on logout.
- Blocks login and refresh for disabled users.
- Exposes authenticated profile read/update APIs.
- Exposes admin-only user listing, status changes, and role changes.

## Core Components

| Component | Responsibility |
| --- | --- |
| `AuthController` | Register, login, refresh, logout APIs |
| `AuthService` | Registration, credential verification, session creation, refresh, logout |
| `JWTService` | JWT generation and validation |
| `RefreshTokenService` | Opaque refresh token creation, hashing, and revocation |
| `JwtAuthenticationFilter` | Extracts bearer JWT and populates Spring Security context |
| `SecurityConfig` | Stateless security rules for `/users/**` and `/admin/**` |
| `UserController` | Authenticated user profile APIs |
| `AdminUserController` | Admin user search, status, and role APIs |
| `AdminUserService` | User filtering and administrative mutations |

## Authentication Flow

```text
register
  |
login
  |
  | returns access JWT + opaque refresh token
  v
call protected APIs with Authorization: Bearer <access-token>
  |
refresh access token with Authorization: Bearer <refresh-token>
  |
logout revokes refresh token
```

Access tokens are stateless JWTs. Refresh tokens are opaque values; only their SHA-256 hashes are stored in PostgreSQL.

## JWT Contract

Access tokens are signed with HMAC SHA-256.

Claims issued today:

| Claim | Meaning |
| --- | --- |
| `iss` | Token issuer, default `user-service` |
| `sub` | User id |
| `email` | User email |
| `role` | Platform role |
| `iat` | Issued-at epoch seconds |
| `exp` | Expiry epoch seconds |

Default access-token TTL:

```text
900 seconds
```

Roles supported by the database:

- `CUSTOMER`
- `ORGANIZER`
- `ADMIN`
- `GATE_AGENT`

Other services use these claims for their own boundary checks. For example, event-management checks organiser roles, booking-service uses `userId`/identity context, and admin-only paths require elevated roles.

## API Surface

Configured base path:

```text
/user-service/v1
```

Primary endpoints:

| Endpoint | Purpose |
| --- | --- |
| `POST /auth/register` | Create customer account |
| `POST /auth/login` | Verify credentials and issue session tokens |
| `POST /auth/refresh` | Issue new access token from refresh token |
| `POST /auth/logout` | Revoke refresh token |
| `GET /users/me` | Fetch authenticated user profile |
| `PATCH /users/me` | Update authenticated user profile |
| `GET /admin/users` | Search/list users |
| `PATCH /admin/users/{id}/status` | Enable/disable user |
| `PATCH /admin/users/{id}/role` | Change user role |

Detailed examples are in [api.md](api.md).

## Security And Consistency Choices

- **BCrypt password hashing:** raw passwords are not stored.
- **Opaque refresh tokens:** refresh token values are not JWTs and are stored only as hashes.
- **Single active login session behavior:** login revokes previous active refresh tokens for the user.
- **Short-lived access tokens:** default access-token TTL is 15 minutes.
- **Stateless protected APIs:** `/users/**` and `/admin/**` are protected by JWT validation.
- **Role-based admin access:** `/admin/**` requires `ROLE_ADMIN`.
- **Disabled-user enforcement:** disabled accounts cannot log in or refresh.
- **Email normalization:** emails are trimmed and lowercased before registration/login lookup.

## Data Model

Core tables:

- `users`
- `refresh_tokens`

Important constraints:

- unique user email
- user status restricted to `ACTIVE`, `DISABLED`
- user role restricted to `CUSTOMER`, `ORGANIZER`, `ADMIN`, `GATE_AGENT`
- unique refresh token hash
- refresh tokens cascade when a user is deleted

Schema: [scripts/db.sql](scripts/db.sql)

The script also inserts a default admin account:

```text
admin@capstone.com
```

## Configuration

Primary settings:

| Property | Purpose |
| --- | --- |
| `api.prefix` | Servlet context path |
| `spring.datasource.*` | PostgreSQL connection |
| `security.jwt.secret` | HMAC signing/verification secret |
| `security.jwt.issuer` | JWT issuer claim |
| `security.jwt.access-ttl-seconds` | Access-token TTL |
| `security.jwt.refresh-ttl-days` | Refresh-token TTL |

## Running Locally

Prerequisites:

- Java 21
- PostgreSQL
- user schema from [scripts/db.sql](scripts/db.sql)

Run:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Run tests:

```powershell
.\mvnw.cmd test
```

## Current Limitations

- Refresh tokens are reused during refresh; they are not rotated on every refresh call.
- Logout returns plain text instead of the common JSON envelope.
- Role update relies on database constraints for allowed role values.
- Password reset, email verification, MFA, and account recovery flows are not implemented yet.
- No audit log exists yet for admin status/role changes.
- No rate limiting or brute-force protection is implemented in this repository.
- No OpenAPI contract is generated yet.

## Production Hardening Roadmap

- Rotate refresh tokens on every refresh and detect refresh-token replay.
- Add password reset, email verification, and optional MFA.
- Add login throttling and suspicious-login detection.
- Add audit logging for login, logout, refresh, admin status changes, and role changes.
- Move JWT secret and default admin bootstrap into secure environment management.
- Standardize logout and error responses through one JSON envelope.
- Add OpenAPI generation and contract tests for JWT claim expectations used by downstream services.
