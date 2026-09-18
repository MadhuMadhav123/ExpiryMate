# ExpiryMate Backend

Spring Boot microservices backend for ExpiryMate.

## Services

| Service | Port | Purpose |
|---|---:|---|
| Eureka Server | 8761 | Service registry |
| API Gateway | 8080 | Single API entry point + centralized Swagger UI |
| Auth Service | 8081 | Registration, login, JWT, profile |
| Document Service | 8082 | Document CRUD, search/filter, upload/download, dashboard |
| Notification Service | 8083 | Welcome/expiry emails, scheduler, notification logs |
| MySQL (Docker host port) | 3307 | `auth_db`, `document_db`, `notification_db` |
| Mailpit UI | 8025 | Local email inbox |

## Validation included

- Registration: required name, valid email, strong password, duplicate email prevention.
- Login: valid email shape + required password; invalid credentials return HTTP 401.
- Profile: name length validation; current-password verification; strong/new/different password validation.
- Documents: required name/category/expiry date, issue date <= expiry date, notes/number length checks.
- Duplicate document names are rejected case-insensitively.
- Duplicate document numbers are rejected while the existing document is Active/Expiring.
- Uploads allow PDF/JPG/PNG up to 10 MB.

## Email behavior

- Successful registration requests a welcome email.
- Adding a document that expires today requests an immediate expiry email.
- Notification Service automatically scans for documents expiring within 30 days.
- A successful expiry reminder is logged and is not repeatedly sent on every scan.
- Failures are stored in `notification_logs.error_message`.

Local Docker defaults to Mailpit. To use Gmail SMTP, copy `.env.example` to `.env` and configure the Gmail values with a Google App Password.

## Run with Docker

```bash
cp .env.example .env
docker compose up --build
```

Open:

- Eureka: http://localhost:8761
- API Gateway: http://localhost:8080
- Central Swagger UI: http://localhost:8080/swagger-ui.html
- Mailpit: http://localhost:8025

Swagger contains Authentication, Document, and Notification definitions. The service OpenAPI documents advertise `/` as their server, so "Try it out" sends requests through the API Gateway rather than directly to ports 8081/8082/8083.

## Run locally from STS

Create the three schemas in your local MySQL on port 3306:

```sql
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS document_db;
CREATE DATABASE IF NOT EXISTS notification_db;
```

The default local MySQL credentials in the YAML are `root / root@9966`. Override with `DB_USER` and `DB_PASSWORD` if needed.

Start in this order:

1. Eureka Server
2. Notification Service
3. Auth Service
4. Document Service
5. API Gateway

For local email testing, run Mailpit separately on SMTP port 1025.

## Build all Java modules

If Maven is installed:

```bash
mvn clean package
```

The root `pom.xml` is an aggregator for all five Spring Boot modules.

## Persistence

Docker volumes preserve:

- MySQL data: `mysql_data`
- Uploaded document files: `document_files` mounted at `/data/documents` in Document Service
