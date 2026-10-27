# Account and Transaction API

A REST API for managing accounts, recording credits and debits, retrieving transaction history, and transferring funds. Built with Java 21, Spring Boot, and PostgreSQL.

## Requirements

- Docker Desktop with the Linux container engine enabled, or:
  - Java 21 or later
  - Maven 3.9 or later
  - PostgreSQL 16 or compatible

## Run with Docker Compose

From this directory, start the API and PostgreSQL:

```powershell
docker compose up --build
```

Compose waits for PostgreSQL to pass its health check before starting the API.

| Service | Address |
| --- | --- |
| REST API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI document | `http://localhost:8080/v3/api-docs` |
| PostgreSQL | `localhost:5432` |

The Compose database uses the development credentials `app` / `app` and database `banking`. Do not use these credentials outside local development.

Stop the services with `Ctrl+C`, or run:

```powershell
docker compose down
```

Compose stores database files in the `postgres_data` volume. To remove the containers and permanently delete that local database:

```powershell
docker compose down --volumes
```

## Run the API outside Docker

Start a PostgreSQL database and set its connection details, then run:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/banking"
$env:SPRING_DATASOURCE_USERNAME = "app"
$env:SPRING_DATASOURCE_PASSWORD = "app"
mvn spring-boot:run
```

The database URL, username, and password can be configured through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. Flyway applies the schema migrations at startup; Hibernate validates the schema without managing it.

## Tests

Run the integration tests with:

```powershell
mvn test
```

Tests use an isolated in-memory H2 database. They cover request validation, account and transaction operations, duplicate references, debit rejection, transaction filtering and pagination, transfer behavior, and the generated OpenAPI examples.

## API reference

See [API.md](API.md) for endpoint details, request and response examples, error responses, and implementation assumptions. Swagger UI provides interactive documentation when the application is running.
