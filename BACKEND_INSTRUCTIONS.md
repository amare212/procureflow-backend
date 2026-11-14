# ProcureFlow Backend: Build Instructions

Procurement & Inventory Management System, REST API.
Repository: `procureflow-backend` (replace with your actual repo name).

Follow this document in order. Build in phases, and commit at the end of each phase (see section 14).

---

## 1. Tech Stack (fixed)

| Layer | Technology |
|---|---|
| Language / Framework | Java 21 (17 minimum), Spring Boot 3.x |
| Web | Spring Web (REST controllers) |
| Persistence | Spring Data JPA (Hibernate) |
| Security | Spring Security + JWT (`jjwt` library) |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| API docs | Swagger / OpenAPI via `springdoc-openapi-starter-webmvc-ui` |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Mapping | MapStruct (or manual mappers) + Lombok |
| Build | Maven |
| Infra | Docker, Docker Compose |

**Intentionally excluded:** automated testing (no JUnit, no Mockito, no test classes). Do not add `spring-boot-starter-test` to the `pom.xml`. Verify behaviour manually through Swagger UI and seeded data instead.

---

## 2. Project Structure

```
procureflow-backend/
├── src/main/java/com/procureflow/
│   ├── ProcureFlowApplication.java
│   ├── config/            # OpenApiConfig, CorsConfig, JpaAuditingConfig
│   ├── security/          # SecurityConfig, JwtService, JwtAuthFilter, CurrentUser, UserDetailsServiceImpl
│   ├── common/
│   │   ├── exception/     # ApiException types, GlobalExceptionHandler
│   │   ├── dto/           # ApiError, PageResponse
│   │   └── entity/        # BaseEntity (id, createdAt, updatedAt, version)
│   ├── auth/              # controller, service, dto
│   ├── user/ department/ supplier/ item/ category/
│   ├── requisition/       # controller, service, repository, entity, dto, workflow/
│   ├── approval/
│   ├── quotation/
│   ├── purchaseorder/
│   ├── receiving/         # goods receipts
│   ├── inventory/         # inventory + stock movements
│   └── report/            # dashboard + reports
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/      # V1__..., V2__...
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

Layering rule: `Controller → Service → Repository → Entity`.

- Controllers only handle HTTP, validation (`@Valid`), and call a service.
- Services hold all business rules and `@Transactional` boundaries.
- Repositories are Spring Data interfaces only.
- Entities are never returned from a controller. Always use request/response DTOs (Java `record`s work well).

---

## 3. Roles

`ADMIN`, `EMPLOYEE`, `DEPARTMENT_MANAGER`, `PROCUREMENT_OFFICER`, `STORE_KEEPER`, `FINANCE_OFFICER`.

Store the role in `roles` and link it from `users`. Each user belongs to exactly one department. Use `@EnableMethodSecurity` and `@PreAuthorize` on service methods, with record-level checks inside services (see section 7).

---

## 4. Database Schema

Create every table through Flyway migrations, never through `ddl-auto`. Set `spring.jpa.hibernate.ddl-auto=validate`.

**Suggested migration order**

1. `V1__roles_departments_users.sql`
2. `V2__suppliers_categories_items.sql`
3. `V3__requisitions_and_items.sql`
4. `V4__approvals.sql`
5. `V5__quotations.sql`
6. `V6__purchase_orders.sql`
7. `V7__goods_receipts.sql`
8. `V8__inventory_and_stock_movements.sql`
9. `V9__indexes.sql`
10. `V10__seed_data.sql` (roles, a department, one user per role, categories, items, a supplier)

**Tables** (all with `id` PK, `created_at`, `updated_at`; add `version` for optimistic locking where noted):

| Table | Key columns and constraints |
|---|---|
| `roles` | `name` UNIQUE |
| `departments` | `name` UNIQUE, `code` UNIQUE, `budget_total`, `budget_spent` |
| `users` | `email` UNIQUE, `password_hash`, `full_name`, `role_id` FK, `department_id` FK, `active` |
| `suppliers` | `name` UNIQUE, contact fields, `status` (ACTIVE/INACTIVE/BLACKLISTED), `notes` |
| `item_categories` | `name` UNIQUE |
| `items` | `sku` UNIQUE, `name`, `category_id` FK, `unit`, `reorder_level`, `unit_cost` |
| `supplier_categories` | (`supplier_id`, `category_id`) composite PK |
| `requisitions` | `requisition_number` UNIQUE, `requester_id` FK, `department_id` FK, `justification`, `priority`, `required_date`, `status`, `estimated_total`, `version` |
| `requisition_items` | `requisition_id` FK, `item_id` FK, `quantity` CHECK > 0, `estimated_unit_price` CHECK >= 0 |
| `approvals` | `requisition_id` FK, `stage`, `actor_id` FK, `decision` (APPROVED/REJECTED), `comment`, `decided_at` |
| `quotations` | `requisition_id` FK, `supplier_id` FK, `total_amount`, `valid_until`, `selected` boolean, plus a `quotation_items` table for line prices |
| `purchase_orders` | `po_number` UNIQUE, `requisition_id` FK, `supplier_id` FK, `quotation_id` FK, `status`, `expected_delivery_date`, `total_amount`, `version` |
| `purchase_order_items` | `purchase_order_id` FK, `item_id` FK, `ordered_quantity`, `received_quantity` default 0, `unit_price`, CHECK `received_quantity <= ordered_quantity` |
| `goods_receipts` | `receipt_number` UNIQUE, `purchase_order_id` FK, `received_by` FK, `delivery_note_number`, `idempotency_key` UNIQUE, `received_at`, UNIQUE (`purchase_order_id`, `delivery_note_number`) |
| `goods_receipt_items` | `goods_receipt_id` FK, `purchase_order_item_id` FK, `quantity_received`, `quantity_accepted`, `notes` |
| `inventory` | `item_id` FK UNIQUE (or UNIQUE with `location`), `quantity_on_hand` CHECK >= 0, `version` |
| `stock_movements` | `item_id` FK, `type` (RECEIPT/ISSUE/ADJUSTMENT), `quantity` (signed), `balance_after`, `reference_type`, `reference_id`, `department_id` (for issues), `recipient_name`, `performed_by` FK, `reason`, `created_at` |

Add indexes on every FK, on `requisitions(status)`, `requisitions(department_id)`, `purchase_orders(status)`, `stock_movements(item_id, created_at)`.

**Deliverable before coding the DB:** draw an ERD (dbdiagram.io or draw.io) and save it as `docs/erd.png` in the repo.

---

## 5. Requisition Workflow (state machine)

Create a dedicated `RequisitionWorkflow` class that is the **only** place status changes happen. It holds the transition table and rejects anything not listed.

| From | Action | To | Who may do it |
|---|---|---|---|
| DRAFT | submit | SUBMITTED | the requester only |
| SUBMITTED | auto | MANAGER_APPROVAL | system, immediately after submit |
| MANAGER_APPROVAL | approve | FINANCE_APPROVAL | `DEPARTMENT_MANAGER` of the **same department**, never the requester |
| MANAGER_APPROVAL | reject | REJECTED | same as above, reason required |
| FINANCE_APPROVAL | approve | PROCUREMENT | `FINANCE_OFFICER`, budget check passes |
| FINANCE_APPROVAL | reject | REJECTED | `FINANCE_OFFICER`, reason required |
| PROCUREMENT | issue PO | ORDERED | `PROCUREMENT_OFFICER` (a selected quotation must exist) |
| ORDERED | receipt completes | RECEIVED | system, when all PO lines are fully received |
| RECEIVED | close | COMPLETED | `PROCUREMENT_OFFICER` or `STORE_KEEPER` |

Add `REJECTED` (and optionally `CANCELLED`) as terminal states. Rules:

- No stage can be skipped. Requests that do not match the transition table return `409 Conflict`.
- Rejection requires a non-blank `comment`.
- Every decision writes an `approvals` row (actor, timestamp, decision, comment) in the same transaction as the status change.
- Only the requester can edit a requisition, and only while it is in DRAFT.
- Finance approval checks `department.budget_total - department.budget_spent >= estimated_total`. If not, return a clear business error. Reserve or commit the amount when the PO is issued.
- Use `@Version` on `requisitions` so two approvers cannot act on the same version at once.

---

## 6. Authentication

- `POST /api/auth/login` returns an access token (JWT, 30 to 60 min) plus user info. A refresh-token endpoint is optional.
- `GET /api/auth/me` returns the current user, role, and department.
- Passwords are hashed with BCrypt. Never return `password_hash`.
- JWT claims: `sub` (user id), `role`, `departmentId`. Sign with HS256 using a secret from env (at least 32 bytes).
- Stateless sessions (`SessionCreationPolicy.STATELESS`), a `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`.
- Public routes: `/api/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**`. Everything else requires authentication.
- Provide a `CurrentUser` helper that reads identity from `SecurityContextHolder`. **Never accept a user id from the request body or query string to decide who is acting.**

---

## 7. Authorization (role-based and record-level)

Role-based checks go on endpoints or service methods with `@PreAuthorize`. Record-level checks go in services.

| Resource | Rule |
|---|---|
| Requisition read | EMPLOYEE sees only their own. DEPARTMENT_MANAGER sees their department's. FINANCE_OFFICER and PROCUREMENT_OFFICER see requisitions at or past their stages. ADMIN sees all. |
| Requisition edit/submit | Owner only, DRAFT only |
| Manager approval | Manager of the requisition's department, not the requester |
| Finance approval | FINANCE_OFFICER only |
| Quotations / POs | PROCUREMENT_OFFICER only, only for requisitions in PROCUREMENT or later |
| Goods receipt / stock issue | STORE_KEEPER only |
| Reports | ADMIN, FINANCE_OFFICER, PROCUREMENT_OFFICER get all. DEPARTMENT_MANAGER gets their department only. |
| Users, departments, suppliers, categories | ADMIN full. PROCUREMENT_OFFICER manages suppliers. Others read-only where needed. |

When a user requests a record they may not see, return `404` (hides existence) or `403`. Pick one and apply it consistently.

---

## 8. Goods Receiving: Critical Acceptance Scenario

Receiving the same PO or delivery twice must **never** add stock twice. Implement all four layers:

1. **Single transaction.** `ReceivingService.receive()` is `@Transactional` and does the following atomically: validate, insert `goods_receipts` and `goods_receipt_items`, update `purchase_order_items.received_quantity`, update `inventory`, insert `stock_movements`, update PO and requisition status.
2. **Row locking.** Load the PO with a pessimistic write lock (`@Lock(PESSIMISTIC_WRITE)` on a repository method) so concurrent receipts for the same PO are serialized.
3. **Idempotency protection:**
   - Client sends an `Idempotency-Key` header (UUID, generated when the receiving form opens). It is stored in `goods_receipts.idempotency_key` with a UNIQUE constraint. On a duplicate key, return the **original** receipt with `200` and do not apply stock again.
   - UNIQUE (`purchase_order_id`, `delivery_note_number`) blocks re-recording the same physical delivery.
4. **Quantity guards:**
   - Reject if the PO is not in an open status (ORDERED or PARTIALLY_RECEIVED).
   - Reject if `received_quantity + new quantity > ordered_quantity`.
   - Database CHECK constraints (`received_quantity <= ordered_quantity`, `quantity_on_hand >= 0`) are the last line of defence.

**Inventory updates** must be race-safe. Either lock the inventory row (`PESSIMISTIC_WRITE`) or use an atomic statement such as `UPDATE inventory SET quantity_on_hand = quantity_on_hand + :qty WHERE item_id = :id`. Never read, add in Java, then save without a lock or `@Version`.

**Issuing stock** to a department (`POST /api/inventory/issues`) uses the same locking, rejects if `quantity_on_hand < requested`, and records recipient and department in `stock_movements`.

Stock movements are append-only. Corrections are new ADJUSTMENT rows, never edits or deletes.

---

## 9. API Conventions

**Base path:** `/api`. JSON only.

**Pagination, sorting, searching, filtering** on every list endpoint: `?page=0&size=20&sort=createdAt,desc&search=...&status=...`. Return a `PageResponse<T>` (content, page, size, totalElements, totalPages). Use `Pageable` and JPA `Specification`s for filters.

**Consistent error body** (via `@RestControllerAdvice`):

```json
{
  "timestamp": "2026-11-14T10:15:30Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/requisitions",
  "fieldErrors": [{ "field": "quantity", "message": "must be greater than 0" }]
}
```

Map: validation to 400, unauthenticated to 401, forbidden to 403, not found to 404, invalid state transition or duplicate to 409, other business rule violations to 422, unexpected errors to 500 (no stack traces in the response).

**Endpoints (minimum)**

```
POST   /api/auth/login            GET /api/auth/me
CRUD   /api/users                 (ADMIN)
CRUD   /api/departments           CRUD /api/item-categories     CRUD /api/items
CRUD   /api/suppliers             GET  /api/suppliers/{id}/purchase-history

POST   /api/requisitions          GET  /api/requisitions        GET /api/requisitions/{id}
PUT    /api/requisitions/{id}     (DRAFT only)
POST   /api/requisitions/{id}/submit
POST   /api/requisitions/{id}/approve     { comment }
POST   /api/requisitions/{id}/reject      { comment }   // required
GET    /api/requisitions/{id}/approvals   // audit trail

POST   /api/requisitions/{id}/quotations  GET /api/requisitions/{id}/quotations
POST   /api/quotations/{id}/select
POST   /api/purchase-orders               GET /api/purchase-orders (filter outstanding/completed)
GET    /api/purchase-orders/{id}

POST   /api/purchase-orders/{id}/receipts (Idempotency-Key header)
GET    /api/goods-receipts

GET    /api/inventory                     GET /api/inventory/low-stock
POST   /api/inventory/issues              POST /api/inventory/adjustments
GET    /api/stock-movements

GET    /api/dashboard/summary
GET    /api/reports/expenditure  /requisitions  /inventory-valuation  /suppliers  /departments
```

Report endpoints accept `from`, `to`, `departmentId`, `supplierId`, `status`, `categoryId`.

Requisition and PO numbers are generated server-side (for example `REQ-2026-000123`, `PO-2026-000045`) using a database sequence, never client-supplied.

---

## 10. Swagger / OpenAPI

- Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui`.
- UI at `/swagger-ui.html`, spec at `/v3/api-docs`.
- Define a `bearerAuth` JWT security scheme in `OpenApiConfig` so the **Authorize** button works.
- Annotate controllers with `@Tag`, and key endpoints with `@Operation(summary=...)` and `@ApiResponse` for 400/403/404/409.
- Document the `Idempotency-Key` header on the receipts endpoint.

---

## 11. Configuration and Secrets

`application.yml` reads everything sensitive from environment variables. Commit only `.env.example`.

```env
# .env.example
POSTGRES_DB=procureflow
POSTGRES_USER=procureflow
POSTGRES_PASSWORD=change_me
DB_URL=jdbc:postgresql://db:5432/procureflow
JWT_SECRET=replace_with_at_least_32_random_bytes
JWT_EXPIRATION_MINUTES=60
CORS_ALLOWED_ORIGINS=http://localhost:3000
SERVER_PORT=8080
```

`.gitignore` must include `.env`, `target/`, `.idea/`, `*.iml`, `.vscode/`, `*.log`.

---

## 12. Docker

- `Dockerfile`: multi-stage (Maven build stage, then a slim JRE runtime stage), runs as non-root.
- `docker-compose.yml` with services `db` (postgres with a named volume and healthcheck) and `api` (depends on db being healthy, loads `.env`, exposes 8080).
- The whole backend must start with `docker compose up --build`. Flyway runs automatically on startup.
- CORS allows the frontend origin from `CORS_ALLOWED_ORIGINS`.

---

## 13. Build Order (phases)

1. **Foundation:** Spring Boot project, Docker Compose with Postgres, Flyway V1, `/actuator/health`, Swagger working.
2. **Security:** users/roles, login, JWT filter, `CurrentUser`, global exception handler.
3. **Master data:** departments, categories, items, suppliers CRUD with pagination and search.
4. **Requisitions:** CRUD, line items, submit, workflow class, approvals table, manager and finance decisions.
5. **Procurement:** quotations, supplier selection, PO generation with traceability.
6. **Receiving and inventory:** transactional receiving, idempotency, stock movements, issues, low-stock.
7. **Reports and dashboard.**
8. **Hardening:** review every endpoint against the authorization table, finalize README and ERD.

## Definition of Done (maps to PRD acceptance criteria)

- [ ] Employee creates a multi-line requisition
- [ ] Correct manager approves or rejects (reason required)
- [ ] Finance records a budget decision
- [ ] Procurement records quotations and issues a PO
- [ ] Storekeeper records a verified goods receipt
- [ ] Receipt updates stock exactly once (duplicate submissions are safe)
- [ ] Reports are viewable by authorized roles
- [ ] Unauthorized actions return 403/404/409 as appropriate
- [ ] `docker compose up --build` starts everything from a clean clone

---

## 14. Git and GitHub

**Setup (once)**

```bash
cd procureflow-backend
git init -b main
git remote add origin https://github.com/<your-username>/<backend-repo>.git
# add .gitignore, .env.example, README.md first
git add .gitignore .env.example README.md
git commit -m "chore: initialize repository with gitignore, env example and readme"
git push -u origin main
```

**Branching**

- `main` is always working and protected from direct pushes (enable branch protection on GitHub if you can).
- Create one branch per feature from `main`: `feat/auth-jwt`, `feat/requisition-workflow`, `fix/receipt-duplicate-check`, `chore/docker-compose`.
- Merge through a Pull Request, even if you are working alone. Write a short PR description and use "Squash and merge" or "Merge" consistently.

**Commit messages**: Conventional Commits, imperative mood, small and focused.

```
feat(auth): add JWT login and authentication filter
feat(requisition): implement workflow state machine with transition validation
feat(receiving): add transactional goods receipt with idempotency key
fix(inventory): prevent negative stock on concurrent issue
docs: add ERD and API usage notes to README
chore(docker): add multi-stage Dockerfile and compose file
refactor(common): centralize error responses in GlobalExceptionHandler
```

Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `perf`, `build`.

**Suggested commit milestones** (one or several commits each, and push at least daily)

1. Repository setup, Docker Compose, Flyway V1
2. Security and JWT
3. Master data modules
4. Requisitions and workflow
5. Quotations and purchase orders
6. Receiving and inventory
7. Reports and dashboard
8. Docs, ERD, final cleanup

**Rules**

- Never commit `.env`, secrets, `target/`, or IDE files. If a secret is committed by accident, rotate it immediately (removing it in a later commit does not remove it from history).
- Each Flyway migration, once pushed, is never edited. Add a new `V#` file instead.
- Tag the finished version: `git tag -a v1.0.0 -m "ProcureFlow backend v1.0.0" && git push origin v1.0.0`.

**README must contain:** project overview, stack, how to run (`cp .env.example .env`, then `docker compose up --build`), Swagger URL, seeded demo accounts, and a link to `docs/erd.png`.
