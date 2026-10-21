# Spring Boot RBAC API

A RESTful user-management API with **JWT authentication** and **role-based access control (RBAC)**, built with Java 21, Spring Boot 3, Spring Security, and PostgreSQL. It was extracted from a larger pharmaceutical supply chain platform, where it handles who can do what.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## Features

- **Stateless JWT authentication** with Spring Security (HMAC-SHA256 signed tokens with expiration)
- **BCrypt password hashing**: passwords are never stored or compared in plain text
- **Role-based access control**: 5 hierarchical roles and 15 granular permissions, enforced per endpoint with `@PreAuthorize`
- **Consistent JSON errors**: `401` for missing or invalid tokens, `403` for missing permissions, `404`/`409`/`400` for business errors
- **Business rules**: the last administrator can't be deleted or demoted; public sign-up always gets the lowest role
- **Layered architecture**: Controller → Service → Repository, with DTOs and centralized exception handling
- **Tests**: unit tests for the JWT logic and integration tests for every access rule, running on in-memory H2
- **Docker**: multi-stage Dockerfile and a Compose file that starts the API with PostgreSQL

---

## How a request is authorized

```
Client ──► POST /api/auth/login {username, password}
             │  UserService checks the password against its BCrypt hash
             ▼
           JWT returned  ───────────────────────────────┐
                                                        │
Client ──► GET /api/users                               │
           Authorization: Bearer <JWT> ◄────────────────┘
             │
             ▼
   JwtAuthenticationFilter   verifies signature + expiration,
             │               loads the user from the DB (must be active),
             │               turns the role's permissions into authorities
             ▼
   SecurityConfig            no valid token on a protected route  ──► 401
             │
             ▼
   @PreAuthorize("hasAuthority('users.read')")
             │               permission missing  ──► 403
             ▼
   UserController → UserService → UserRepository → PostgreSQL
```

The user is reloaded from the database on every request, so a role change or deactivation takes effect immediately, even for tokens issued before the change.

---

## Roles and permissions

| Role | Level | Summary |
|------|:-----:|---------|
| `ADMINISTRATOR` | 5 | Full access, including user management |
| `MANAGER` | 4 | Approves transfers, manages inventory, reads users |
| `WAREHOUSE_CLERK` | 3 | Creates and views transfers |
| `AUDITOR` | 2 | Read-only access, including users and audit logs |
| `VIEWER` | 1 | Can only view inventory |

| Permission | Admin | Manager | Clerk | Auditor | Viewer |
|------------|:-----:|:-------:|:-----:|:-------:|:------:|
| `users.create` | ✅ | | | | |
| `users.read` | ✅ | ✅ | | ✅ | |
| `users.update` | ✅ | | | | |
| `users.delete` | ✅ | | | | |
| `transfers.create` | ✅ | ✅ | ✅ | | |
| `transfers.read` | ✅ | ✅ | ✅ | ✅ | |
| `transfers.update` | ✅ | ✅ | | | |
| `transfers.delete` | ✅ | | | | |
| `transfers.approve` | ✅ | ✅ | | | |
| `inventory.read` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `inventory.update` | ✅ | ✅ | | | |
| `reports.view` | ✅ | ✅ | ✅ | ✅ | |
| `reports.export` | ✅ | ✅ | | | |
| `audit.view` | ✅ | ✅ | | ✅ | |
| `settings.manage` | ✅ | | | | |

Roles and permissions are defined in [`Role.java`](src/main/java/com/rbac/api/entity/Role.java). This service enforces the `users.*` permissions on its own endpoints; the rest are returned at login so the wider platform (transfers, inventory, reports) can apply them.

---

## API endpoints

### Authentication

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| `POST` | `/api/auth/login` | Returns a JWT plus the user's role and permissions | Public |
| `POST` | `/api/auth/register` | Creates an account with the `VIEWER` role | Public |
| `POST` | `/api/auth/logout` | Confirms logout (the client discards the token) | Authenticated |
| `GET` | `/api/auth/status` | Service status | Public |

### Users

| Method | Endpoint | Description | Required permission |
|--------|----------|-------------|---------------------|
| `GET` | `/api/users` | List users (newest first) | `users.read` |
| `GET` | `/api/users/{id}` | Get a user by id | `users.read` |
| `GET` | `/api/users/username/{username}` | Get a user by username | `users.read` |
| `GET` | `/api/users/search?q=` | Search by name or username | `users.read` |
| `GET` | `/api/users/role/{role}` | List users with a role | `users.read` |
| `GET` | `/api/users/roles` | List all roles | Any authenticated user |
| `POST` | `/api/users` | Create a user with any role | `users.create` |
| `PUT` | `/api/users/{id}` | Update name, email, password, or active status | `users.update` |
| `PUT` | `/api/users/{id}/role` | Change a user's role | `users.update` |
| `DELETE` | `/api/users/{id}` | Delete a user | `users.delete` |

### Health

| Method | Endpoint | Access |
|--------|----------|--------|
| `GET` | `/actuator/health` | Public |

---

## Quick start

### Option A: Docker (recommended)

Requires [Docker](https://www.docker.com/products/docker-desktop/). Starts PostgreSQL and the API together.

```bash
git clone https://github.com/EstebanMezaCS/spring-boot-rbac-api.git
cd spring-boot-rbac-api
docker compose up --build
```

The API runs on `http://localhost:8080`.

### Option B: Run locally

Requires Java 21+, Maven 3.9+, and a running PostgreSQL with a database named `rbac_db`.

```bash
# Linux / macOS
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=any-random-string-of-at-least-32-characters
mvn spring-boot:run
```

```powershell
# Windows PowerShell
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "your_password"
$env:JWT_SECRET = "any-random-string-of-at-least-32-characters"
mvn spring-boot:run
```

### Configuration

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `SPRING_DATASOURCE_URL` | | `jdbc:postgresql://localhost:5432/rbac_db` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | | Database password |
| `JWT_SECRET` | ✅ | | Token signing secret, at least 32 characters. The app refuses to start with a shorter one |
| `JWT_EXPIRATION_MS` | | `3600000` (1 hour) | Token lifetime |

---

## Try it

On first start the app creates these demo accounts (only if the `users` table is empty):

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | Administrator |
| `manager` | `manager123` | Manager |
| `clerk` | `clerk123` | Warehouse Clerk |
| `auditor` | `auditor123` | Auditor |
| `viewer` | `viewer123` | Viewer |

```bash
# 1. Log in and copy the "token" field from the response
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# 2. Call a protected endpoint with the token
curl http://localhost:8080/api/users -H "Authorization: Bearer <token>"

# 3. Without a token you get 401; logged in as "viewer" you get 403
curl http://localhost:8080/api/users
```

**Login response:**

```json
{
  "id": "7ccfbaa3-68cf-44f3-820d-ff052d28d342",
  "username": "admin",
  "name": "System Administrator",
  "email": "admin@company.com",
  "role": "ADMINISTRATOR",
  "permissions": ["users.create", "users.read", "users.update", "users.delete", "..."],
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error response (403):**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to perform this action",
  "timestamp": "2026-10-21T13:48:59.21"
}
```

---

## Running the tests

```bash
mvn test
```

The tests use an in-memory H2 database (`test` profile), so they don't need PostgreSQL or any environment variables.

- **`JwtServiceTest`**: valid tokens, tampered tokens, tokens signed with another secret, expired tokens, and secrets that are too short
- **`SecurityIntegrationTest`**: starts the full application and checks every access rule through the real filter chain: 401 without or with an invalid token, 403 for missing permissions, what each role can and can't do, and that public sign-up always gets `VIEWER`

---

## Project structure

```
src/main/java/com/rbac/api/
├── RbacApplication.java
├── config/
│   ├── SecurityConfig.java          # Filter chain, public routes, 401/403 JSON responses
│   ├── PasswordConfig.java          # BCrypt PasswordEncoder bean
│   ├── CorsConfig.java              # CORS for browser clients
│   └── DataInitializer.java         # Creates demo accounts on an empty database
├── security/
│   ├── JwtService.java              # Creates and verifies JWTs
│   └── JwtAuthenticationFilter.java # Reads the Bearer token and authenticates the request
├── controller/
│   ├── AuthController.java          # Login, register, logout
│   └── UserController.java          # User management, protected with @PreAuthorize
├── service/
│   └── UserService.java             # Business logic and validation rules
├── repository/
│   └── UserRepository.java          # Spring Data JPA queries
├── entity/
│   ├── User.java                    # JPA entity
│   └── Role.java                    # Roles, hierarchy levels, and permissions
├── dto/
│   └── Dtos.java                    # Request and response objects
└── exception/
    ├── Exceptions.java              # Custom exceptions
    └── GlobalExceptionHandler.java  # Maps exceptions to HTTP status codes

src/test/java/com/rbac/api/
├── security/JwtServiceTest.java
└── controller/SecurityIntegrationTest.java
```

---

## Tech stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Language |
| Spring Boot 3.3 | Application framework |
| Spring Security 6 | Authentication filter chain and method-level authorization |
| JJWT 0.12 | JWT creation and verification |
| Spring Data JPA / Hibernate | Persistence |
| PostgreSQL | Database |
| JUnit 5, MockMvc, H2 | Testing |
| Docker, Docker Compose | Containerization |
| Maven | Build |

---

## License

MIT. See [LICENSE](LICENSE).

## Author

**Esteban Meza** · [GitHub](https://github.com/EstebanMezaCS) · [LinkedIn](https://linkedin.com/in/esteban-meza-87731a22b)
