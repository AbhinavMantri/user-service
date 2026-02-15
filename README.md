# user-service
User service to manage authentication and authorization

Entities
users
- id (UUID PK)
- email (unique, indexed)
- password_hash
- full_name
- phone (optional)
- role (varchar)
- status (ACTIVE, DISABLED)
- created_at
- updated_at

refresh_tokens
- id (UUID PK)
- user_id (FK)
- token_hash (store hash, not raw token)
- expires_at
- revoked (boolean)
- created_at

REST API contract
Auth

POST /user-service/v1/auth/register

POST /user-service/v1/auth/login

POST /user-service/v1/auth/refresh

POST /user-service/v1/auth/logout (optional: invalidate refresh token)

Users

GET /user-service/v1/users/me (current user profile)

PATCH /user-service/v1/users/me (optional: name, phone)

GET /user-service/v1/admin/users (ADMIN only)

PATCH /user-service/v1/admin/users/{id}/status (ADMIN only)

PATCH /user-service/v1/admin/users/{id}/role (ADMIN only)

User-service DB scripts
under /scripts/db.sql