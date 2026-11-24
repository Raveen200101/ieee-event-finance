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

## Expense tracking

Open an event to add expenses and view total spending and the remaining
budget in LKR. Spending above the planned budget is allowed and displayed
as an over-budget amount.

- POST /api/events/{eventId}/expenses records an expense and returns HTTP 201.
- GET /api/events/{eventId}/expenses returns the expense list, plannedBudget,
  totalSpent, and remainingBudget. Missing events return HTTP 404.

Each expense requires description (up to 500 characters), category (up to
50 characters), amount (at least 0.01, up to 13 integer digits and 2 decimal
places), and expenseDate (ISO date). Category is free text with common
suggestions in the UI. Dates can be past or future. Lists sort by expense
date descending, then ID descending.

V2__create_expenses.sql adds the expenses table with an event foreign key
and positive-amount constraint. Amounts use MySQL DECIMAL and Java BigDecimal.
Totals are calculated from the stored expenses. Existing events initially
have zero expenses. The event API's existing development access rules also
cover these nested expense endpoints.

ExpenseApiTests verifies persistence, event isolation, exact totals,
overspending, validation, and missing events. It removes its own test data.

## Income tracking

Select an event to record sponsorships, registration fees, donations, or
other income. Each record requires description (up to 500 characters),
source (up to 150 characters), amount (at least 0.01 with up to 13 integer
digits and 2 decimal places), and incomeDate (ISO date). Source is free
text with common suggestions. Dates can be past or future.

- POST /api/events/{eventId}/incomes records income and returns HTTP 201.
- GET /api/events/{eventId}/incomes returns incomes, totalIncome,
  totalExpenses, and balance. Missing events return HTTP 404.

Balance equals recorded income minus recorded expenses and may be negative.
Planned budget remains the spending target used by expense tracking.
Amounts use MySQL DECIMAL and Java BigDecimal. Records sort by income date
descending, then ID descending. Adding expenses refreshes the income
panel's financial totals.

V3__create_incomes.sql creates the incomes table with an event foreign key
and positive-amount constraint. Existing records and applied migrations
remain intact. Income endpoints use the existing event API development
access rules. IncomeApiTests verifies persistence, validation, event
isolation, decimal totals, negative balances, and missing events.

## Figma event screens

The Events, Create Event, and Event Overview pages follow the supplied
screen exports. Start at /#/events. Search and status/term filters operate
on saved events; selecting a row opens its overview. Browser back/forward
navigation is supported. Expense and income entry remain available from
the sidebar after selecting an event.

V4__add_event_setup_details.sql adds optional ExCom term, finance lead,
and expected participants, plus an event status defaulting to Planning.
Older requests remain valid, and existing records retain their data.
Supported statuses are Planning, In progress, Completed, and Archived.

The interface uses recorded expenses for actual spending. Bank transfers,
verified amounts, documents, Excel
reports, and role-based identity are still pending, and their unavailable
actions or values are marked accordingly. Local development identity is
shown in place of the mock Chair account from the design.

## Planned budget

Select an event, then **Planned budget** in the sidebar (/#/events/{id}/budget).
Add categories with a responsible person's name and a planned amount in LKR.
Edit owners/amounts or remove allocations. Names remain fixed while editing;
removing an allocation never deletes expenses. Owners are free text until team
membership is implemented. Category names match expense categories ignoring
case and surrounding spaces; use the same name when entering expenses.

The page and event overview compare category plans with recorded expenses,
including categories without allocations. Amounts use DECIMAL/BigDecimal.
Zero allocations are allowed; negative amounts or more than two decimal places
are rejected. Duplicate categories within an event return HTTP 409. Category
allocations may exceed the event spending target, with a visible warning.

- GET /api/events/{eventId}/budget returns allocations, actual spending,
  remaining amounts, totalAllocated and unallocatedBudget.
- POST /api/events/{eventId}/budget accepts category (1–50 characters),
  owner (1–150 characters), plannedAmount (0–9999999999999.99), returns 201.
- PUT /api/events/{eventId}/budget/{id} updates owner and plannedAmount.
- DELETE /api/events/{eventId}/budget/{id} removes an allocation.

Missing events or allocations belonging to another event return 404.
V5__create_budget_categories.sql creates the new table on backend startup;
earlier migrations and existing financial records are preserved. Development
access rules continue to apply; sign-in and role enforcement remain pending.

Run the backend with the existing Java 21 and MySQL environment variables,
then start Vite as described above. BudgetApiTests uses that MySQL database
and removes its own records. Run `.\mvnw.cmd test` in backend, and
`npm.cmd run build` plus `npm.cmd run lint` in frontend.
