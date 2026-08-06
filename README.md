# AI Cognitive Operating System (AI-COS) - Backend

Enterprise-grade Spring Boot 3 & Java 21 REST API server providing Authentication, Identity Management, Session Tracking, and OAuth2 Integration.

---

## 🚀 Tech Stack

- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security 6, Argon2id Password Hashing, JJWT (v0.12.5)
- **Database**: PostgreSQL 15+ (H2 in-memory for testing)
- **ORM & Migrations**: Spring Data JPA / Hibernate, Flyway
- **Build System**: Gradle 8.x
- **API Documentation**: OpenAPI 3 / Swagger UI (`springdoc-openapi`)

---

## 🔑 Core Security & Authentication Scope

- **Password Cryptography**: OWASP-compliant Argon2id (`Argon2PasswordEncoder`)
- **Authentication**: Stateless Short-lived Access Tokens (15 min) + Refresh Token Rotation (7 days)
- **OAuth Providers**: Google, GitHub, and Microsoft OAuth identity linking (`oauth_accounts`)
- **Session Management**: Multi-device active session tracking (`user_sessions`) with IP, User-Agent, location, and explicit device revocation (`/v1/sessions`)
- **Brute Force Protection**: Account lockout after 5 consecutive failed login attempts (15 min lockout window)
- **Security Audit Logs**: Immutable `login_history` tracking success/failure events, IP addresses, and timestamps

---

## 🛠 Project Structure

```
backend/
├── src/main/java/com/app/
│   ├── config/          # Spring Security, CORS, JWT, OpenAPI configurations
│   ├── common/          # ApiConstants, BaseEntity, global wrappers
│   ├── controller/      # REST Controllers (AuthController, SessionController, UserController, HealthController)
│   ├── dto/             # Request & Response Data Transfer Objects
│   │   ├── request/     # Inbound JSON DTOs
│   │   └── response/    # Outbound JSON DTOs
│   ├── entity/          # JPA Entities (UserEntity, RefreshTokenEntity, OAuthAccountEntity, LoginHistoryEntity)
│   ├── enums/           # Enumerations (UserRole, UserStatus)
│   ├── exception/       # Centralized GlobalExceptionHandler & custom exceptions
│   ├── model/           # Internal Domain Models
│   ├── repository/      # Spring Data JPA Repositories
│   ├── service/         # Service interfaces & implementations (AuthService, JwtService, UserService)
│   └── util/            # Pure helper utilities & mappers
├── src/main/resources/
│   ├── application.yml  # Application configuration
│   └── db/migration/    # Flyway DDL migration scripts (V1, V2, V3)
└── build.gradle         # Gradle dependencies and task rules
```

---

## ⚙️ Environment Configuration

Create a `.env` file in the `backend/` directory:

```env
# Server Port
SERVER_PORT=8080

# PostgreSQL Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ai_cos
DB_USERNAME=postgres
DB_PASSWORD=postgrespassword

# JWT Security Configuration (HMAC SHA-512 Secret)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION=900000
```

---

## 🚦 Getting Started & Commands

### 1. Build Backend
```bash
./gradlew build -x test
```

### 2. Run Integration & Unit Test Suite
```bash
./gradlew test
```

### 3. Run Development Server
```bash
./gradlew bootRun
```

---

## 📚 API Endpoints Summary

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/v1/auth/register` | Public | Register new user account |
| `POST` | `/v1/auth/login` | Public | Authenticate email/password (Argon2id) |
| `POST` | `/v1/auth/oauth` | Public | Authenticate / register via Google, GitHub, Microsoft |
| `POST` | `/v1/auth/refresh-token` | Public | Obtain new access token via refresh token |
| `POST` | `/v1/auth/verify-email` | Public | Verify user email with token |
| `POST` | `/v1/auth/forgot-password` | Public | Request password reset token |
| `POST` | `/v1/auth/reset-password` | Public | Reset password with token |
| `POST` | `/v1/auth/change-password` | Bearer | Change current user password |
| `PUT` | `/v1/auth/profile` | Bearer | Update user profile details |
| `POST` | `/v1/auth/logout` | Bearer | Revoke active refresh token session |
| `POST` | `/v1/auth/logout-all` | Bearer | Revoke all active sessions for user |
| `GET` | `/v1/auth/me` | Bearer | Get current authenticated user profile |
| `GET` | `/v1/sessions` | Bearer | List active sessions & device details |
| `DELETE` | `/v1/sessions/{public_id}` | Bearer | Revoke a specific active device session |
| `GET` | `/v1/sessions/login-history` | Bearer | Retrieve login history audit log |
