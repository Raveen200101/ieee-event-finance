# ieee-event-finance
Event finance and procurement system for the IEEE Computer Society Chapter at SLIIT

## Local setup

Use JDK 21, Node.js 22.12 or newer, npm, and MySQL. The backend
includes the Maven Wrapper. Run the following commands from the repository
root in separate PowerShell windows.

### Prepare MySQL

Install MySQL for Windows with the command-line tools and start its
service on port 3306. With MySQL's `bin` folder on PATH, open a session
as the MySQL administrator and create the application user and database once.

```powershell
mysql -h localhost -P 3306 -u root -p
```

```sql
CREATE DATABASE ieee_event_finance_dev CHARACTER SET utf8mb4;
CREATE USER 'ieee_event_finance'@'localhost' IDENTIFIED BY 'REPLACE_WITH_YOUR_LOCAL_PASSWORD';
GRANT ALL PRIVILEGES ON ieee_event_finance_dev.* TO 'ieee_event_finance'@'localhost';
exit
```

Replace the password placeholder with your chosen local password before running the SQL. Use MySQL 8.4 LTS or a compatible supported MySQL release. Flyway will run future migrations
from `backend/src/main/resources/db/migration/`. Hibernate validates the
schema at startup.

### Window 1 — Spring Boot

Adjust the example `JAVA_HOME` path to match your JDK 21 installation. The datasource URL below matches the default. Supply the username and password through environment variables. Set
`SPRING_DATASOURCE_PASSWORD` in the current shell using the development
role's password.

```powershell
Set-Location .\backend
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/ieee_event_finance_dev'
$env:SPRING_DATASOURCE_USERNAME = 'ieee_event_finance'
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'MySQL password' -AsSecureString)).Password
.\mvnw.cmd -version
.\mvnw.cmd spring-boot:run
```

Use `.\mvnw.cmd compile` to
check compilation and `.\mvnw.cmd test` to run the generated context test
with the same database settings.

### Window 2 — React

```powershell
Set-Location .\frontend
npm.cmd ci
npm.cmd run dev
```

Open <http://localhost:5173>. Vite proxies `/api` and `/actuator/health`
to Spring Boot on port 8080. With both apps running, run these commands
from another PowerShell prompt. Both health checks should report `UP`.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://localhost:5173/actuator/health
```

Run `npm.cmd run build` and `npm.cmd run lint` from `frontend` to verify
the frontend.

The generated Spring Boot context test loads the real application configuration
and requires the same MySQL database and environment variables. There is no
separate test datasource or in-memory database. To inspect database dependencies,
run the following from backend.

```powershell
.\mvnw.cmd dependency:tree "-Dincludes=com.mysql:mysql-connector-j,org.flywaydb:*,org.postgresql:*"
```

V1__create_events.sql creates the events table. Future migrations belong in backend/src/main/resources/db/migration. Keep applied migration files unchanged.

### Run from your IDEs

In IntelliJ IDEA, open the backend folder as a Maven project. Use JDK 21
and main class lk.sliit.ieee.eventfinance.EventFinanceApplication.
In Run > Edit Configurations, set SPRING_DATASOURCE_URL,
SPRING_DATASOURCE_USERNAME, and SPRING_DATASOURCE_PASSWORD under
Environment variables using the values above. Keep the configuration local
with Store as project file unchecked, then click Run.

In VS Code, open the frontend folder and use its PowerShell terminal to run
npm.cmd ci followed by npm.cmd run dev.

### Bootstrap verification

Verified on 2026-11-23 with Java 21 and local MySQL. Backend compilation and the generated context test passed. Frontend build and lint passed. Spring Boot started successfully, and both the direct and Vite-proxied health endpoints returned UP. Credentials must be supplied locally when starting the application.

## Event management

The backend follows Spring MVC with controller, dto, service, repository,
entity, exception, and config packages under lk.sliit.ieee.eventfinance.
Controllers handle HTTP and validation, services handle transactions and
mapping, repositories access MySQL, and DTOs define request/response data.

- POST /api/events creates an event and returns HTTP 201 with its Location.
- GET /api/events lists events by event date descending, then ID descending.
- GET /api/events/{id} returns one event, or HTTP 404 if it does not exist.

Event fields are name (required, up to 150 characters), description (up to
2000 characters, an empty string is allowed), eventDate (required ISO date),
venue (required, up to 255 characters), and plannedBudget (required,
nonnegative, up to 13 integer digits and 2 decimal places, in LKR).
MySQL uses an auto-increment BIGINT ID and DECIMAL(15,2) for the budget.
Dates can be past or future.

During this local milestone, event endpoints are publicly accessible and
excluded from CSRF checks. Other endpoints retain Spring Security protection,
with health checks public. Add authentication and authorization before
deploying event management for shared use.

The React page supports creating events, listing them, and opening event
details. Reloading retrieves saved records from MySQL.

Run the Maven test command above with MySQL configured. EventApiTests checks
the real HTTP endpoints, persistence, validation, malformed requests, and
missing records, and removes its own created record afterward.
