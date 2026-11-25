# JWT backend implementation report

Implemented on branch `codex/authentication-roles`, left uncommitted for review.
No commit, push, merge or deployment was performed for this JWT task.

## Result and decisions

Spring Boot 4.1.2 / Spring Security 7.1.2 / Java 21. The existing technical
controller/service/repository/entity/dto/config/exception package layout is retained.
Spring Security Resource Server provides BearerTokenAuthenticationFilter,
JwtAuthenticationProvider and RSA JWT verification. No custom JWT filters or
third-party JWT API were added. Nimbus JOSE is a managed transitive implementation.

Implemented JSON email/password login, BCrypt account persistence, RS256 access
JWTs, signed-claim validation, current database authorization, opaque hashed refresh
tokens with transactional rotation/replay-family revocation, logout, own profile,
password changes, and privileged account provisioning. Role/admin changes and
password changes revoke refresh sessions and invalidate old access tokens by version.
Cookie endpoints retain CSRF protection; bearer-only business writes are stateless.
CORS uses an explicit allowlist and credential-compatible headers/methods.

The latest user clarification is implemented: Finance Team Leads can add/edit only
their own expense/income rows in assigned events, and cannot configure events,
budgets or income categories. Logistics Team Leads can submit their own quotation
records only in their owned purchase groups in assigned events, cannot select quotes
or access finance ledgers, and cannot edit/remove preferred quotations after committee
selection. Generic volunteer access remains minimal. Account administration is a
separate ACCOUNT_ADMIN privilege. Event assignments and configured income sources
are committee-only controls. See AUTHENTICATION.md for the complete matrix, flows,
ER/sequence/filter diagrams, API contracts, configuration and deployment instructions.

## Data and migrations

Existing V1–V7 were not changed during the JWT conversion. V8 migrates centralized
chapter_users to a role FK and adds roles, refresh_tokens, event_memberships,
income_sources, user timestamps/admin flag and nullable creator/purchase owner links.
Existing financial values, user IDs and password hashes are preserved. Existing
unowned rows remain committee-only. Nothing is assigned to a volunteer automatically.
Hibernate remains ddl-auto=validate; open-in-view is disabled.

V8 is not automatically applied to the chapter development database by these tests.
At the original test milestone, MySQL-specific V8 execution was pending. The user chose fake repositories rather
than preparing an additional MySQL database. Unit tests use Mockito fakes; integration
tests use isolated H2 with a schema mirror, real JWT signatures and the real filter
chain. H2 validation is not presented as proof of MySQL DDL compatibility.

## Dependencies

Added only spring-boot-starter-oauth2-resource-server (managed 4.1.2) and test-scoped
H2 (managed 2.4.240). Dependency tree verifies security OAuth2/core/config/web/Jose
7.1.2, Nimbus JOSE 10.9.1 and existing Spring Security Test. No manual Hibernate,
JWT version pinning or duplicate JDBC/Flyway dependencies were introduced.

## Actual verification

- Java 21 Maven Wrapper `clean test`: **23 tests, 0 failures, 0 errors, 0 skipped**.
- Verified JWT signatures and invalid signature/payload/expiry/issuer/audience/roles,
  required identity/version claims and explicit algorithm restrictions.
- Verified refresh rotation, stored hashes, reuse revocation, concurrent replay,
  expiry, disabled accounts, logout and credential/access-change revocation.
- Verified profile ownership, account administration, assigned-event filtering,
  Finance row ownership, income-source restrictions and Logistics purchase ownership.
- Existing event, budget, expense, income and quotation API regressions pass with
  authenticated bearer requests; no security bypass was used.
- Filter-chain test verifies Spring's built-in bearer filter and actual order.
- Explicit Maven Wrapper `compile`: passed on Java 21.
- Java source-file RSA key-generation helper: executed successfully (3072-bit keys, ignored target directory).
- Dependency-tree inspection: passed; versions above are from the actual Maven output.
- `git diff --check`: passed.
- Source secret scan: no actual private-key blocks or local database password literals.
- No authentication tests connected to or deleted data in the chapter MySQL database.

## Boundaries and next steps

The React follow-up now uses JSON login, in-memory bearer tokens, single-flight
refresh (with Web Locks coordination where supported), four-role controls and
assignment-aware requests. It retries a protected request once after a 401;
403 responses do not trigger refresh.

PDF quotation/bill uploads and transaction-receipt storage/delivery are not implemented
by this authentication task. Their future access must follow assigned events and
purchase/expense ownership; Logistics must never gain a general finance endpoint.
The protected PDF upload feature remains a future milestone once storage is configured.

### Local integration follow-up, 2026-11-25

- Confirmed V8 is applied successfully in the local development MySQL database;
  Spring Boot started with Hibernate schema validation. Disposable MySQL migration
  fixture verification remains separate from this live startup check.
- Browser sign-in, reload restoration, logout, signed-out reload, and an existing
  event overview were verified through `http://localhost:5173`.
- Five mocked HTTP authentication client tests, frontend build and lint passed.
- Existing financial records were viewed without modification. Changes remain
  uncommitted for review.

Public deployment still requires HTTPS, secure cookies, private-key management,
login rate limiting at the gateway, MySQL migration verification, and operational
backup/retention. Email recovery/invitations, MFA, a staged key ring and refresh-row
retention are documented future improvements. Logout revokes refresh families but
an already issued access JWT can remain valid until expiry (normally 15 minutes).

## File inventory

The inventory below includes reused/uncommitted backend authentication files from
the prior session implementation, now adapted to JWT. It excludes the preserved
frontend work. Responsibilities and exact package paths are in AUTHENTICATION.md.

### Added backend and documentation files

- `backend/.env.example`
- `backend/scripts/GenerateJwtKeys.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/config/BootstrapChair.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/config/ChapterPrincipal.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/config/JwtAccountConverter.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/config/JwtConfig.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/config/JwtProperties.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/config/SecurityErrors.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/AuthController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/ChapterUserController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/EventMembershipController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/IncomeSourceController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/AssignPurchaseOwnerRequest.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/ChangePasswordRequest.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/CreateUserRequest.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/IncomeSourceRequest.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/LoginRequest.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/LoginResponse.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/PublicEventResponse.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/UpdateUserAccessRequest.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/dto/UserResponse.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/ChapterRole.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/ChapterUser.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/EventMembership.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/IncomeSource.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/RefreshToken.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/Role.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/exception/RefreshTokenRejectedException.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/ChapterUserRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/EventMembershipRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/IncomeSourceRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/RefreshTokenRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/RoleRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/AccessPolicy.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/AuthService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/ChapterUserService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/CustomUserDetailsService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/EventMembershipService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/IncomeSourceService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/JwtService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/RefreshTokenService.java`
- `backend/src/main/resources/db/migration/V7__create_chapter_users.sql`
- `backend/src/main/resources/db/migration/V8__jwt_roles_and_refresh_tokens.sql`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/AuthApiTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/AuthenticatedApiTest.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/ChapterUserServiceTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/JwtTestInitializer.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/JwtValidationTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/RefreshTokenTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/SecurityArchitectureTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/TeamLeadAuthorizationTests.java`
- `backend/src/test/resources/db/testmigration/V1__isolated_schema.sql`
- `docs/AUTHENTICATION.md`

### Modified existing backend/configuration/documentation files

- `.gitignore`
- `README.md`
- `backend/pom.xml`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/config/SecurityConfig.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/EventController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/ExpenseController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/IncomeController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/controller/PurchaseController.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/Expense.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/Income.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/PurchaseGroup.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/entity/Quotation.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/exception/ApiExceptionHandler.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/EventRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/ExpenseRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/IncomeRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/repository/PurchaseGroupRepository.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/BudgetService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/EventService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/ExpenseService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/IncomeService.java`
- `backend/src/main/java/lk/sliit/ieee/eventfinance/service/PurchaseService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/BudgetApiTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/EventApiTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/EventFinanceApplicationTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/ExpenseApiTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/IncomeApiTests.java`
- `backend/src/test/java/lk/sliit/ieee/eventfinance/PurchaseApiTests.java`
