# API Documentation

This document describes the currently implemented API surface of `user-service` based on the source code under `src/main/java`.

## Base URL

```text
/user-service/v1
```

The prefix comes from:

- `api.prefix=/user-service/v1`
- `server.servlet.context-path=${api.prefix}`

## Authentication Rules

- `/auth/**` endpoints are public.
- `/users/**` requires a valid JWT access token.
- `/admin/**` requires a valid JWT access token with `ROLE_ADMIN`.

Protected endpoints expect:

```http
Authorization: Bearer <token>
```

Access tokens are JWTs signed with HMAC-SHA256 and include:

- `iss`
- `sub`
- `email`
- `role`
- `iat`
- `exp`

Default token settings:

- access token TTL: `900` seconds
- refresh token TTL: `7` days

## Common Response Envelope

Most endpoints return:

```json
{
  "status": "SUCCESS | FAILURE",
  "message": "..."
}
```

## Auth APIs

### POST `/auth/register`

Creates a new user account.

Request body:

```json
{
  "email": "alice@example.com",
  "password": "Password123",
  "fullName": "Alice Doe",
  "phone": "+919999999999"
}
```

Validation rules:

- `email` is required and must be a valid email
- `password` is required and must be 8 to 72 characters
- `fullName` is optional, max 255 chars, allowed chars: letters, digits, space, `.`, `'`, `-`
- `phone` is optional, max 20 chars, format: optional `+` followed by 7 to 15 digits

Behavior:

- email is normalized to lowercase and trimmed
- role is set to `CUSTOMER`
- status is set to `ACTIVE`
- password is stored as a BCrypt hash

Success response: `201 Created`

```json
{
  "status": "SUCCESS",
  "message": "User registered successfully",
  "userId": "fd5d6b58-c495-48fe-a8f8-0e9d84c3c040",
  "email": "alice@example.com",
  "fullName": "Alice Doe",
  "phone": "+919999999999"
}
```

Error cases:

- `409 Conflict` if user already exists
- `400 Bad Request` for illegal input handled by service
- `500 Internal Server Error` for unexpected failures

### POST `/auth/login`

Authenticates a user and returns a session token pair.

Request body:

```json
{
  "email": "alice@example.com",
  "password": "Password123"
}
```

Validation rules:

- `email` is required and must be valid
- `password` is required and must be 8 to 72 characters

Success response: `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "Login successful",
  "session": {
    "sessionToken": "<jwt access token>",
    "refreshToken": "<opaque refresh token>"
  }
}
```

Behavior:

- disabled users cannot log in
- login revokes existing active refresh tokens for that user
- a new refresh token is created and stored as a hash

Error cases:

- `403 Forbidden` if the account is disabled
- `401 Unauthorized` for invalid credentials and other auth failures

### POST `/auth/refresh`

Returns a fresh access token for a valid refresh token.

Headers:

```http
Authorization: Bearer <refresh token>
```

Success response: `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "Token refreshed",
  "session": {
    "sessionToken": "<new jwt access token>",
    "refreshToken": "<same refresh token>"
  }
}
```

Behavior:

- the refresh token is looked up by SHA-256 hash
- expired refresh tokens are revoked immediately
- disabled users cannot refresh
- the existing refresh token is reused if still valid

Error cases:

- `403 Forbidden` if the user is disabled
- `401 Unauthorized` if the refresh token is invalid or expired

### POST `/auth/logout`

Revokes the refresh token associated with the current session.

Headers:

```http
Authorization: Bearer <refresh token>
```

Success response: `200 OK`

```text
Logged out successfully
```

Important implementation note:

- despite the controller parameter being named `accessToken`, the service actually expects a refresh token here

Error cases:

- `401 Unauthorized` if the supplied token is invalid or already revoked

## User APIs

These endpoints require a valid JWT access token.

### GET `/users/me`

Returns the authenticated user's profile.

Headers:

```http
Authorization: Bearer <access token>
```

Success response: `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "User profile retrieved successfully",
  "userProfile": {
    "id": "fd5d6b58-c495-48fe-a8f8-0e9d84c3c040",
    "email": "alice@example.com",
    "fullName": "Alice Doe",
    "phone": "+919999999999"
  }
}
```

Error cases:

- `404 Not Found` if the user referenced by the token email does not exist

### PATCH `/users/me`

Updates the authenticated user's profile.

Headers:

```http
Authorization: Bearer <access token>
```

Request body:

```json
{
  "name": "Alice Updated",
  "phone": "+918888888888"
}
```

Validation rules:

- at least one of `name` or `phone` must be provided
- `name` cannot be blank and max length is 255
- `phone` cannot be blank and max length is 20

Behavior:

- only provided fields are updated
- `name` maps to the persisted `fullName` field

Success response: `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "User profile updated successfully",
  "userProfile": {
    "id": "fd5d6b58-c495-48fe-a8f8-0e9d84c3c040",
    "email": "alice@example.com",
    "fullName": "Alice Updated",
    "phone": "+918888888888"
  }
}
```

Error cases:

- `404 Not Found` if the user does not exist

## Admin APIs

These endpoints require an access token for a user with role `ADMIN`.

### GET `/admin/users`

Returns a paginated list of users.

Headers:

```http
Authorization: Bearer <admin access token>
```

Query parameters:

- `page` default `0`
- `size` default `20`
- `sortBy` default `createdAt`
- `sortDir` default `desc`
- `status` optional, one of `ACTIVE`, `DISABLED`
- `role` optional, case-insensitive
- `q` optional search string applied to `email`, `fullName`, and `phone`

Success response: `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "Users retrieved successfully",
  "users": [
    {
      "id": "fd5d6b58-c495-48fe-a8f8-0e9d84c3c040",
      "email": "alice@example.com",
      "fullName": "Alice Doe",
      "phone": "+919999999999",
      "role": "CUSTOMER",
      "status": "ACTIVE",
      "createdAt": "2026-04-12T08:00:00",
      "updatedAt": "2026-04-12T08:05:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

Error cases:

- `400 Bad Request` for invalid pageable or filter inputs

### PATCH `/admin/users/{id}/status`

Updates a user's status.

Headers:

```http
Authorization: Bearer <admin access token>
```

Path parameter:

- `id`: user UUID

Request body:

```json
{
  "status": "DISABLED"
}
```

Success response: `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "User status updated successfully",
  "user": {
    "id": "fd5d6b58-c495-48fe-a8f8-0e9d84c3c040",
    "email": "alice@example.com",
    "fullName": "Alice Doe",
    "phone": "+919999999999",
    "role": "CUSTOMER",
    "status": "DISABLED",
    "createdAt": "2026-04-12T08:00:00",
    "updatedAt": "2026-04-12T08:05:00"
  }
}
```

Error cases:

- `400 Bad Request` for invalid UUIDs or invalid request values
- `500 Internal Server Error` for other failures

### PATCH `/admin/users/{id}/role`

Updates a user's role.

Headers:

```http
Authorization: Bearer <admin access token>
```

Path parameter:

- `id`: user UUID

Request body:

```json
{
  "role": "ORGANIZER"
}
```

Behavior:

- role is trimmed and uppercased before persistence
- the service does not validate the role itself, but the database check constraint allows only:
  - `CUSTOMER`
  - `ORGANIZER`
  - `ADMIN`
  - `GATE_AGENT`

Success response: `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "User role updated successfully",
  "user": {
    "id": "fd5d6b58-c495-48fe-a8f8-0e9d84c3c040",
    "email": "alice@example.com",
    "fullName": "Alice Doe",
    "phone": "+919999999999",
    "role": "ORGANIZER",
    "status": "ACTIVE",
    "createdAt": "2026-04-12T08:00:00",
    "updatedAt": "2026-04-12T08:05:00"
  }
}
```

Error cases:

- `400 Bad Request` for invalid UUIDs or blank role
- `500 Internal Server Error` for downstream persistence failures such as DB role constraint violations

## Persistence Notes

Tables created by [scripts/db.sql](/d:/Scaler-Projects/user-service/scripts/db.sql):

- `users`
- `refresh_tokens`

The SQL script:

- enables `pgcrypto`
- creates indexes for user lookup and refresh-token lookup
- inserts a default admin user: `admin@capstone.com`

## Known Implementation Details

- `UserProfile` responses expose `fullName`, while the update request field is named `name`.
- `GET /users/me` calls `getUserProfile(email, null, null)`; the extra unused parameters are currently harmless but not used.
- logout returns plain text instead of the common JSON envelope.
