\# Backend Engineering Instructions



\## Architecture



\- Organize packages by business feature.

\- Keep the main Spring Boot application class in the root package.

\- Controllers handle HTTP concerns only.

\- Application services coordinate use cases and transactions.

\- Domain objects contain business invariants when appropriate.

\- Infrastructure packages contain persistence and external integrations.

\- Domain packages must not depend on web or persistence implementations.

\- Do not expose JPA entities from controllers.



\## Abstraction rules



\- Do not create generic BaseController, BaseService or BaseRepository classes.

\- Do not create an interface for every service implementation.

\- Use an interface when:

&#x20; - an architectural boundary must be protected;

&#x20; - multiple implementations are realistic;

&#x20; - an external integration needs an adapter;

&#x20; - the domain must not depend on infrastructure;

&#x20; - a test seam provides meaningful value.

\- Prefer composition over inheritance.

\- Introduce generic types only when semantics are truly shared.

\- Do not use generics merely to reduce repeated lines.



\## Spring rules



\- Prefer constructor injection.

\- Do not use field injection.

\- Define transaction boundaries in application services.

\- Mark read-only query transactions when appropriate.

\- Keep transactions short.

\- Do not make remote calls inside database transactions.

\- Use configuration properties for grouped application configuration.

\- Do not read environment variables directly throughout business code.



\## Persistence



\- Review generated SQL for complex repository operations.

\- Prevent N+1 queries using query-specific fetch strategies.

\- Do not globally change relationships to eager loading.

\- Use projections for read-heavy endpoints when full entities are unnecessary.

\- Use database pagination for large result sets.

\- Define deterministic sorting for pagination.

\- Avoid offset pagination for very large or frequently changing datasets

&#x20; when cursor pagination is more suitable.

\- Review indexes whenever query patterns or relations change.

\- Do not cascade remove large object graphs without reviewing execution cost.

\- Use batch operations intentionally and verify generated SQL.



\## API



\- Request and response models must be separate from persistence entities.

\- Validate request models.

\- Monetary request fields must remain numeric JSON values. Do not accept
  locale-formatted strings with thousands separators at the API boundary.

\- Use a consistent error model.

\- Handle exceptions centrally.

\- Do not leak exception messages or stack traces to clients.

\- Return appropriate status codes.

\- Document pagination, sorting and filtering rules.

\- Keep API changes synchronized with the OpenAPI contract.



\## Security



\- Deny access by default.

\- Validate authorization at the use-case or method boundary.

\- Never trust client-provided ownership or role information.

\- Do not log passwords, tokens, credentials or sensitive payloads.

\- Apply size and format constraints to user-controlled input.

\- Review mass-assignment risks when mapping request models.



\## Testing



\- Unit-test domain rules and application services.

\- Integration-test persistence queries against a real database-compatible environment.

\- Test authorization failures.

\- Test transaction and concurrency behavior when relevant.

\- Use test fixtures or builders instead of repetitive object construction.

\- Avoid mocking JPA repositories in persistence tests.



\## Verification



Run:



1\. `./mvnw spotless:check`

2\. `./mvnw compile`

3\. `./mvnw test`

4\. `./mvnw verify`

## Security requirements

Use `$spring-security-jwt` before creating or modifying:

- Authentication
- Login or logout
- JWT creation or validation
- Refresh-token behavior
- Spring Security configuration
- Roles or permissions
- Protected controllers
- Method security
- Password handling
- CORS
- CSRF
- Authentication cookies
- Account session management

Authentication and authorization changes are security-sensitive.

Do not implement JWT validation using a custom request filter when
Spring Security OAuth2 Resource Server can perform the validation.

Access tokens must not be stored in frontend localStorage,
sessionStorage or IndexedDB.

Use a short-lived JWT access token and a rotating opaque refresh token
unless an approved architecture decision specifies another model.

All protected operations require backend authorization.

Frontend permission checks are user-experience controls only.

Do not declare security work complete until the security test matrix
from `$spring-security-jwt` passes.
