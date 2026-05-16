# User Service API

Current HTTP contract for `user-service`.

Base path:

```text
/user-service/v1
```

## Conventions

Public endpoints:

```text
/auth/**
```

Protected user endpoints:

```http
Authorization: Bearer <access-token>
```

Admin endpoints require an access token with role `ADMIN`.

Most responses use:

- `status: SUCCESS`
- `status: FAILURE`

## JWT Access Token

Access tokens are signed with HMAC SHA-256 and include:

| Claim | Meaning |
| --- | --- |
| `iss` | Issuer, default `user-service` |
| `sub` | User id |
| `email` | User email |
| `role` | User role |
| `iat` | Issued-at epoch seconds |
| `exp` | Expiry epoch seconds |

Default TTL:

```text
900 seconds
```

## POST `/auth/register`

Creates a new customer account.

### Request

```json
{
  "email": "alice@example.com",
  "password": "Password123",
  "fullName": "Alice Doe",
  "phone": "+919999999999"
}
```

### Validation

- `email` is required and must be valid.
- `password` is required and must be 8 to 72 characters.
- `fullName` is optional, max 255 chars, and allows letters, digits, space, `.`, `'`, `-`.
- `phone` is optional, max 20 chars, optional `+` followed by 7 to 15 digits.

### Success Response

`201 Created`

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

### Behavior Notes

- Email is normalized to lowercase and trimmed.
- Password is stored as BCrypt hash.
- Role is set to `CUSTOMER`.
- Status is set to `ACTIVE`.

### Errors

- `409 Conflict` if the user already exists.
- `400 Bad Request` for illegal service-level input.
- `500 Internal Server Error` for unexpected failures.

## POST `/auth/login`

Authenticates a user and returns access + refresh tokens.

### Request

```json
{
  "email": "alice@example.com",
  "password": "Password123"
}
```

### Success Response

`200 OK`

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

### Behavior Notes

- Disabled users cannot log in.
- Existing active refresh tokens for the user are revoked when a new login session is created.
- New refresh token is stored only as SHA-256 hash.

### Errors

- `403 Forbidden` if the account is disabled.
- `401 Unauthorized` for invalid credentials.

## POST `/auth/refresh`

Issues a fresh access token from a valid refresh token.

### Headers

```http
Authorization: Bearer <refresh-token>
```

### Success Response

`200 OK`

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

### Behavior Notes

- Refresh token is looked up by SHA-256 hash.
- Expired refresh tokens are revoked immediately.
- Disabled users cannot refresh.
- Current implementation reuses the same refresh token; it does not rotate refresh tokens on every refresh.

### Errors

- `403 Forbidden` if the user is disabled.
- `401 Unauthorized` if the refresh token is invalid or expired.

## POST `/auth/logout`

Revokes the refresh token associated with the current session.

### Headers

```http
Authorization: Bearer <refresh-token>
```

### Success Response

`200 OK`

```text
Logged out successfully
```

### Behavior Notes

- The service expects a refresh token, even though the controller parameter is named `accessToken`.
- Logout currently returns plain text rather than the common JSON envelope.

### Errors

- `401 Unauthorized` if the supplied refresh token is invalid or already revoked.

## GET `/users/me`

Returns the authenticated user's profile.

### Headers

```http
Authorization: Bearer <access-token>
```

### Success Response

`200 OK`

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

### Behavior Notes

- Authenticated principal name is the email claim from the JWT.
- User lookup is performed by email.

### Errors

- `401 Unauthorized` for missing/invalid access token.
- `404 Not Found` if the token email no longer maps to a user.

## PATCH `/users/me`

Updates the authenticated user's profile.

### Headers

```http
Authorization: Bearer <access-token>
```

### Request

```json
{
  "name": "Alice Updated",
  "phone": "+918888888888"
}
```

### Validation

- At least one of `name` or `phone` must be provided.
- `name` cannot be blank and max length is 255.
- `phone` cannot be blank and max length is 20.

### Success Response

`200 OK`

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

### Behavior Notes

- Only provided fields are updated.
- Request field `name` maps to persisted `fullName`.

## GET `/admin/users`

Returns a paginated list of users.

### Headers

```http
Authorization: Bearer <admin-access-token>
```

### Query Params

| Param | Default | Purpose |
| --- | --- | --- |
| `page` | `0` | Page number |
| `size` | `20` | Page size |
| `sortBy` | `createdAt` | Sort field |
| `sortDir` | `desc` | `asc` or `desc` |
| `status` | none | Optional `ACTIVE` or `DISABLED` |
| `role` | none | Optional role filter |
| `q` | none | Search across email, full name, and phone |

### Success Response

`200 OK`

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

## PATCH `/admin/users/{id}/status`

Updates a user's account status.

### Headers

```http
Authorization: Bearer <admin-access-token>
```

### Request

```json
{
  "status": "DISABLED"
}
```

### Success Response

`200 OK`

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
    "status": "DISABLED"
  }
}
```

### Behavior Notes

- Disabled users cannot log in or refresh access tokens.

## PATCH `/admin/users/{id}/role`

Updates a user's platform role.

### Headers

```http
Authorization: Bearer <admin-access-token>
```

### Request

```json
{
  "role": "ORGANIZER"
}
```

### Success Response

`200 OK`

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
    "status": "ACTIVE"
  }
}
```

### Behavior Notes

- Role is trimmed and uppercased before persistence.
- Service-level validation does not restrict role values yet.
- Database constraint allows only `CUSTOMER`, `ORGANIZER`, `ADMIN`, and `GATE_AGENT`.

## Persistence Notes

Tables:

- `users`
- `refresh_tokens`

Refresh tokens are stored as hashes, not raw token values.

## Common Errors

- `400 Bad Request` for invalid request values.
- `401 Unauthorized` for missing/invalid tokens.
- `403 Forbidden` for disabled account login/refresh or non-admin access to admin APIs.
- `404 Not Found` for missing authenticated user profile.
- `409 Conflict` for duplicate registration.
