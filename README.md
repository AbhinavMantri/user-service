# user-service

`user-service` is a Spring Boot microservice responsible for user registration, authentication, profile management, and basic admin-level user operations.

It exposes JWT-based auth flows for end users and protected management APIs for authenticated users and admins. The current implementation uses PostgreSQL for user and refresh-token persistence and stores refresh tokens as hashes instead of storing raw token values.

## Purpose

This service is the identity and user-account boundary for a larger system. It currently handles:

- user registration
- login and refresh-token based session renewal
- logout by revoking refresh tokens
- authenticated self-service profile reads and updates
- admin-only user listing, status changes, and role changes

## Tech Summary

- Java 21
- Spring Boot 4
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven Wrapper

## API Summary

Base URL:

```text
/user-service/v1
```

Public auth APIs:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

Authenticated user APIs:

- `GET /users/me`
- `PATCH /users/me`

Admin APIs:

- `GET /admin/users`
- `PATCH /admin/users/{id}/status`
- `PATCH /admin/users/{id}/role`

Broad API details are documented in [api.md](/d:/Scaler-Projects/user-service/api.md).

## Security Model

- Access to `/users/**` requires a valid JWT access token in `Authorization: Bearer <token>`.
- Access to `/admin/**` requires a valid JWT access token with role `ADMIN`.
- Login returns:
  - `sessionToken`: access JWT
  - `refreshToken`: opaque refresh token
- Refresh and logout operate on the refresh token supplied through the `Authorization` header.
- Only one active refresh token is maintained per user login session because older active refresh tokens are revoked when a new one is issued.

## Data Model

Primary tables defined in [scripts/db.sql](/d:/Scaler-Projects/user-service/scripts/db.sql):

- `users`
- `refresh_tokens`

Important defaults and constraints from the current implementation:

- default user role on registration: `CUSTOMER`
- supported statuses: `ACTIVE`, `DISABLED`
- role constraint in SQL: `CUSTOMER`, `ORGANIZER`, `ADMIN`, `GATE_AGENT`
- refresh tokens are stored as hashes

The DB script also inserts a default admin user:

- email: `admin@capstone.com`

## Local Usage

Prerequisites:

- Java 21
- PostgreSQL

Default dev configuration in [application.properties](/d:/Scaler-Projects/user-service/src/main/resources/application.properties) and [application-dev.properties](/d:/Scaler-Projects/user-service/src/main/resources/application-dev.properties):

- active profile: `dev`
- database URL: `jdbc:postgresql://localhost:5432/user_service?currentSchema=auth_db`
- database username: `postgres`
- database password: `postgres`
- JWT issuer: `user-service`
- access token TTL: `900` seconds
- refresh token TTL: `7` days

Run the schema setup first, then start the app:

```powershell
psql -U postgres -d user_service -f scripts/db.sql
./mvnw spring-boot:run
```

Or on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Response Shape

Most JSON APIs return a common envelope:

```json
{
  "status": "SUCCESS",
  "message": "Human readable message"
}
```

Depending on the endpoint, the response may additionally include:

- registration fields such as `userId`, `email`, `fullName`, `phone`
- `session.sessionToken` and `session.refreshToken`
- `userProfile`
- `user`
- paginated `users` plus pagination metadata

## Notes From Current Implementation

- `POST /auth/logout` currently expects a refresh token in the `Authorization` header, even though the controller variable is named `accessToken`.
- `POST /auth/refresh` reuses the existing refresh token if it is still valid; it does not rotate it.
- The project already contains controller and service tests under `src/test/java`.
