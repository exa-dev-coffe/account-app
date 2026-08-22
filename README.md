# ☕ Account Service (`be-coffe-java`)

Account Service is a core backend microservice built with **Java 17** and **Spring Boot 3**. It handles user authentication, authorization, account management, and role-based access control for the application.

## 🚀 Technologies

*   **Language**: Java 17
*   **Framework**: Spring Boot 3.4
*   **Database**: PostgreSQL
*   **Migration**: Liquibase
*   **ORM**: Spring Data JPA / Hibernate
*   **Security**: Spring Security + JWT
*   **Storage**: MinIO (Object Storage)
*   **Message Broker**: RabbitMQ
*   **Observability**: OpenTelemetry, Grafana Alloy (OTLP)
*   **Logging**: Logback (JSON Format with trace correlation)
*   **Build Tool**: Maven

## 📦 Features

*   **JWT Authentication**: Secure user login and token generation.
*   **Google OAuth2**: Integration for Google single sign-on.
*   **User Management**: Registration, profile updates, and role assignments.
*   **HMAC Validation**: Secure internal inter-service communication validation.
*   **Distributed Tracing**: Fully instrumented with OpenTelemetry sending spans to Tempo.

## 🛠️ Prerequisites

*   JDK 17
*   Maven 3.8+
*   PostgreSQL
*   RabbitMQ
*   MinIO

## ⚙️ Environment Variables

Copy the `.env.example` file to `.env` and fill in the necessary values:

```bash
cp .env.example .env
```

## 🚀 How to Run

1.  **Local Development:**
    ```bash
    ./mvnw spring-boot:run
    ```

2.  **Build Docker Image:**
    ```bash
    docker build -t eka-dev/account-service .
    ```

## 🗄️ Database Migration (Liquibase)

Service ini menggunakan **Liquibase** untuk database migration. Dokumentasi lengkap cara menjalankan dan membuat file migration tersedia di **[DOCS_MIGRATION.md](file:///d:/Project/coffe/be-coffe-java/DOCS_MIGRATION.md)**.

- **Otomatis**: Dijalankan otomatis oleh Spring Boot saat aplikasi startup (`spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml`).
- **Manual (CLI)**: `./mvnw liquibase:update`

## 🧪 Integration Testing

Jalankan perintah berikut untuk menguji seluruh endpoint domain secara modular dengan Testcontainers:

```bash
# Windows
.\mvnw.cmd test

# Linux/macOS
./mvnw test
```

*Requirement:* Docker Desktop/Daemon must be running.

## 📡 Endpoints Overview
*   `POST /api/1.0/auth/login` - User login
*   `POST /api/1.0/auth/register` - User registration
*   `GET /api/1.0/me` - Get current user profile
