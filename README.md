# ieee-event-finance
Event finance and procurement system for the IEEE Computer Society Chapter at SLIIT

## Local setup

The backend and React client now use JWT authentication. Read [the current authentication setup](docs/AUTHENTICATION.md) before restarting the backend. RSA keys are required. React stores access tokens in memory and restores sign-in through the refresh cookie.

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
# Generate the RSA pair once; keep it outside Git.
if (-not (Test-Path '.local/jwt-keys/jwt.private.pem')) {
    & "$env:JAVA_HOME\bin\java.exe" scripts/GenerateJwtKeys.java .local/jwt-keys
}
$env:JWT_PRIVATE_KEY_PATH = (Resolve-Path '.local/jwt-keys/jwt.private.pem').Path
$env:JWT_PUBLIC_KEY_PATH = (Resolve-Path '.local/jwt-keys/jwt.public.pem').Path
$env:AUTH_COOKIE_SECURE = 'false' # Local HTTP only; true in production

$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/ieee_event_finance_dev'
$env:SPRING_DATASOURCE_USERNAME = 'ieee_event_finance'
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'MySQL password' -AsSecureString)).Password
# First startup only, when chapter_users is empty:
$env:APP_BOOTSTRAP_CHAIR_EMAIL = 'admin@gmail.com'
$env:APP_BOOTSTRAP_CHAIR_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'New Chair password (at least 12 characters)' -AsSecureString)).Password
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

## Purchase groups and quotations

Open an event and choose **Purchase groups** (/#/events/{id}/purchases), or
**Compare quotations** on Event Overview. First create a saved budget category
in Planned budget, then create a purchase group such as Event T-shirts linked
to that category. Add supplier quotations with quantity, unit price in LKR,
and optional notes. Total equals quantity × unit price; enter prices including
applicable charges. Attachments and role-based approvals are future work.

Each group can have one preferred quotation. Selecting another replaces the
previous preference; selection can also be cleared. Editing a quotation clears
its preference so revised terms must be selected again. Removing a selected
quotation clears that selection. Group/category links remain fixed in this
milestone. A budget category linked to purchase groups cannot be removed;
its owner and allocation amount can still be edited.

Category comparisons sum preferred quotations from every group linked to that
category. Negative remaining amounts show an over-budget warning. These are
purchasing plans, and never create an expense or change income/balance totals.
Record actual spending separately using the matching expense category.
Actual expenses are not subtracted again from this quotation comparison.

API prefix: `/api/events/{eventId}/purchase-groups`:

- GET lists groups, quotations and category budget comparisons.
- POST creates a group using name, budgetCategoryId and optional notes (201).
- POST `/{groupId}/quotations` adds supplier, quantity, unitPrice, optional notes (201).
- PUT `/{groupId}/quotations/{id}` updates quotation terms.
- DELETE `/{groupId}/quotations/{id}` removes a quotation.
- PUT `/{groupId}/selection/{id}` selects a quotation.
- DELETE `/{groupId}/selection` clears the preference.

Names/suppliers accept up to 150 characters, notes up to 1000, quantity is an
integer from 1 to 1,000,000, unitPrice is 0.01–9999999999999.99 with two decimal
places. Missing or cross-event/category/group references return 404. Quotation
writes lock their purchase group to serialize concurrent preference changes.
Development access rules continue to apply until sign-in is implemented.

V6__create_purchase_groups_and_quotations.sql adds two tables and preserves
existing migrations and data. Restart the Java 21 backend with the documented
MySQL environment variables to apply it, then run Vite normally. No new
dependencies are needed. `PurchaseApiTests` checks decimal totals, selection,
concurrent selection, validation, event/group isolation, shared-category totals,
and expense preservation, and removes its own test records.

## Previous session-authentication setup (superseded)

The current backend JWT contract and team-lead permissions are documented in
[docs/AUTHENTICATION.md](docs/AUTHENTICATION.md). The following session instructions
are retained from the earlier in-progress implementation and are not the current
JWT API. The React client now uses the JWT API described in the linked document.

### Historical sign-in and chapter permissions

V7__create_chapter_users.sql adds local chapter accounts without changing event
or financial data. Sign in with email and password; accounts are independent
of Google Drive and Firebase. The first Chair email for this workspace is
`admin@gmail.com`. No account password is committed or provided by default.

For a new database, supply APP_BOOTSTRAP_CHAIR_EMAIL and
APP_BOOTSTRAP_CHAIR_PASSWORD before starting the backend, as shown above.
Bootstrap only runs while chapter_users is empty, and never resets an existing
account's password. Remove these two variables from your shell or IntelliJ
run configuration after initial creation. Existing accounts remain in MySQL.
IntelliJ uses the same variables in **Run → Edit Configurations → Environment
variables**. The Chair password must contain at least 12 characters and no
more than 72 UTF-8 bytes. Passwords are stored as BCrypt hashes.

| Role | Read chapter events and finances | Create events | Manage finances and quotations | Manage accounts |
| --- | --- | --- | --- | --- |
| Chair | Yes | Yes | Yes | Yes |
| Finance Lead / Treasurer | Yes | No | Yes | No |
| Member | Yes | No | No | No |

Permissions are chapter-wide; event-specific membership is a future feature.
The Chair can create accounts, change roles and disable/re-enable access under
**Chapter accounts**. The last active Chair cannot be disabled or demoted.
Changes apply to already signed-in users on their next API request. Everyone
can change their own password by clicking their name → **My account**.
Changing a password ends all sessions for that account. There is no public
registration, Google sign-in, email invitation or forgotten-password flow yet.
Share initial credentials with the intended member privately.

API authentication uses an HttpOnly JSESSIONID session cookie, SameSite=Lax,
30-minute inactivity timeout, and CSRF protection on all writes, including
login/logout. React fetches a fresh token before writes. Passwords and session
tokens are not stored in browser localStorage. API responses use 401 for missing
sign-in and 403 for insufficient access or invalid CSRF tokens. `/actuator/health`
remains public; protected financial APIs no longer allow anonymous access.
Earlier README references to public development access are superseded here.

- GET /api/auth/csrf returns token/headerName and initializes the session.
- POST /api/auth/login accepts form-urlencoded email/password plus CSRF header.
- GET /api/auth/me returns the signed-in account, without password information.
- POST /api/auth/logout ends the session (204).
- PUT /api/auth/password accepts currentPassword/newPassword and ends sessions (204).
- GET/POST /api/users and PUT /api/users/{id} are Chair-only account management.

For deployment, serve React and `/api` through the same HTTPS origin and set
APP_SESSION_COOKIE_SECURE=true. Use separate development/production credentials
and protect secrets in the hosting environment. Sessions currently live in
backend memory, so a restart requires signing in again; multiple instances need
shared sessions or sticky routing. Configure login rate limits at the reverse
proxy before public deployment. This milestone does not connect Google Drive.

Tests use real cookie sessions, CSRF tokens and isolated test accounts, with
the existing MySQL settings. They test role restrictions, disabled accounts,
password changes, logout, validation, and preservation of financial behavior.
Test accounts and financial test records are cleaned up afterward.

## JWT backend authentication

See [Authentication architecture, permissions and setup](docs/AUTHENTICATION.md).
The backend requires RSA key paths and bearer tokens. React uses JSON login,
in-memory access tokens, shared refresh requests, and role-aware navigation. No Google Drive
or Firebase storage is connected by this authentication milestone.

Verified locally on 2026-11-25: browser sign-in, restoration after reload, sign-out,
and loading an existing event overview. Frontend build, lint, and five authentication
client tests passed. Run `npm.cmd test` from `frontend` for these tests; they mock
HTTP responses and do not change the chapter database. Open `http://localhost:5173`
and press Ctrl+F5 if an older frontend is still loaded.

## Event team and purchase assignments

The **Chapter accounts** table includes **Current events** with linked event names
and statuses for each assigned member. Only Planning and In progress events appear;
Completed and Archived events are excluded. Use **Reload accounts** after changes.
Committee accounts show **All events (committee access)** because event access does
not imply an explicit work assignment. Disabled accounts keep their assignment list
visible for administration, but remain unable to sign in.

Sign in as a committee member, open an event, and select **Event team** in the
sidebar to see only members assigned to that event and remove access when needed.
The page does not list unassigned chapter accounts or offer add-assignment buttons.
Committee members already have chapter-wide event access. An account administrator
creates accounts under **Chapter accounts**; committee members do not need account
administrator privileges to view or remove event team assignments.

You can also assign events while creating an account: open **Chapter accounts →
Create chapter account**, choose the role, tick the optional **Assigned events**
checkboxes, and click **Create account**. The account and memberships save in one
transaction. Invalid event selections leave no partial account. Committee accounts
automatically access all events and do not need event selection. Existing clients
may omit `eventIds` in `POST /api/users` to create an account without assignments.

Under **Purchase groups**, choose an assigned, active Logistics Team Lead for each
group. Choose **Unassigned — committee only** to clear it. Removing a member from
the event also clears their purchase-group assignments in that event. Their
expenses, income and quotations remain stored with the original creator; rejoining
an event does not automatically restore cleared purchase assignments. Reassigning
a group does not transfer ownership of existing quotations to the new lead.

Finance leads can manage only their own financial records in assigned events.
Logistics leads can access assigned purchase groups and their own quotations;
they cannot access the expense/income ledgers or select preferred quotations.
Disabled accounts cannot be newly assigned. Existing disabled assignees remain
visible so the committee can remove them. No schema migration is needed here.

Assignment APIs (committee only):

- `GET /api/events/{eventId}/members/options`: limited directory with name, role,
  active status and event assignment; no email, credentials or administrator flag.
- `GET /api/events/{eventId}/members`: assigned user IDs.
- `PUT` / `DELETE /api/events/{eventId}/members/{userId}`: assign/remove event access.
- `PUT /api/events/{eventId}/purchase-groups/{groupId}/owner` with `{"userId":123}`:
  assign an active Logistics Team Lead who is already assigned to the event.
- `DELETE /api/events/{eventId}/purchase-groups/{groupId}/owner`: clear ownership.

Run `.\mvnw.cmd test` in `backend` with Java 21. Assignment tests use isolated H2,
including a non-admin committee member, invalid assignments, reassignment, access
revocation and record preservation; they do not change chapter data.
