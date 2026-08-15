# Account Service (be-coffe-java)

Account Service is a core backend microservice built with **Java 17** and **Spring Boot 3**. It handles user authentication, authorization, account management, and role-based access control for the application.

## 🚀 Technologies

*   **Language**: Java 17
*   **Framework**: Spring Boot 3.4
*   **Database**: PostgreSQL
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

## 📡 Endpoints Overview
*   `POST /api/v1/auth/login` - User login
*   `POST /api/v1/auth/register` - User registration
*   `GET /api/v1/users/me` - Get current user profile
