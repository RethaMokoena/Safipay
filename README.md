# SafiPay — Full Stack Monorepo

> Modern digital wallet, instant payments & stokvel savings platform for South Africa.
> **Stack:** Spring Boot 3.2 microservices (Java 17) · Angular 17 (standalone) · JWT auth · H2 (dev)

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Architecture Overview](#architecture-overview)
3. [Backend Services](#backend-services)
4. [Frontend Application](#frontend-application)
5. [API Reference](#api-reference)
6. [Cross-Service Call Chain](#cross-service-call-chain)
7. [Event Types](#event-types)
8. [Quick Start](#quick-start)
9. [Environment Config](#environment-config)
10. [Roadmap](#roadmap)

---

## Project Structure

```
safipay/
├── backend/
│   ├── pom.xml                   ← Parent POM (multi-module Maven)
│   ├── gateway-service/          ← Port 8080
│   ├── user-service/             ← Port 8081
│   ├── wallet-service/           ← Port 8082
│   ├── payment-service/          ← Port 8083
│   ├── stokvel-service/          ← Port 8084
│   ├── ledger-service/           ← Port 8085
│   ├── merchant-service/         ← Port 8086
│   ├── fraud-service/            ← Port 8087
│   └── webhook-service/          ← Port 8088
└── frontend/
    └── safipay-app/              ← Angular 17 SPA (port 4200)
```

---

## Architecture Overview

```
Angular SPA (4200)
      │
      ▼  HTTP + Bearer JWT
┌─────────────────────────────────────────────┐
│         Gateway Service  (8080)              │
│  • JWT validation (AuthFilter)              │
│  • Rate limiting  (100 req/min/IP)          │
│  • Request logging (X-Request-Id tracing)   │
│  • Routes to all 8 downstream services      │
└──────┬──────┬──────┬──────┬──────┬──────┬──┘
       │      │      │      │      │      │
   user  wallet pay stokvl ledger merchant fraud
   8081  8082  8083  8084   8085   8086   8087
                                          │
                                     webhook
                                      8088

Internal (service-to-service, no gateway):
  payment-service  → fraud-service    /internal/fraud/evaluate
  payment-service  → wallet-service   /internal/wallets/{id}/debit|credit
  payment-service  → ledger-service   /internal/ledger/transactions
  payment-service  → webhook-service  /internal/webhooks/events
  stokvel-service  → wallet-service   /internal/wallets/{id}/debit|credit
  stokvel-service  → fraud-service    /internal/fraud/evaluate
  stokvel-service  → ledger-service   /internal/ledger/transactions
  stokvel-service  → webhook-service  /internal/webhooks/events
  merchant-service → wallet-service   /internal/wallets/{id}/debit|credit
  user-service     → (internal)       /internal/users/{id}
```

---

## Backend Services

| Service | Port | Java Files | Responsibility |
|---------|------|-----------|----------------|
| **gateway-service** | 8080 | 6 | JWT validation, rate limiting, request tracing, routing |
| **user-service** | 8081 | 23 | Registration, login, profile, password change, token refresh, admin user management |
| **wallet-service** | 8082 | 19 | Balances, top-up, P2P transfer, transaction history, admin freeze/unfreeze |
| **payment-service** | 8083 | 19 | Send money, request money, approve/decline, refund, fraud check, ledger posting, webhook events |
| **stokvel-service** | 8084 | 28 | ROSCA & pool savings groups, contributions, payouts, wallet integration, fraud check, ledger + webhooks |
| **ledger-service** | 8085 | 22 | Double-entry bookkeeping, account statements, reversal, idempotent posting |
| **merchant-service** | 8086 | 21 | Business accounts, HMAC API key lifecycle, charge customers, refunds, 1.5% fee calculation |
| **fraud-service** | 8087 | 22 | 11-rule risk scoring engine, fraud alerts, blacklisting, per-user risk profiles |
| **webhook-service** | 8088 | 21 | Endpoint registration, HMAC-SHA256 signed delivery, async retry scheduler, delivery audit trail |

**Total: 181 Java files**

### Send Money — full call chain
```
Client → Gateway (JWT + rate limit + log)
  → payment-service
      ├─ fraud-service     evaluate transaction        [APPROVED / REVIEW / BLOCKED]
      ├─ wallet-service    debit sender wallet
      ├─ wallet-service    credit recipient wallet
      ├─ ledger-service    post double-entry record
      └─ webhook-service   fire payment.completed event
```

### H2 Consoles (dev only)
| Service | Console URL |
|---------|-------------|
| user | http://localhost:8081/h2-console |
| wallet | http://localhost:8082/h2-console |
| payment | http://localhost:8083/h2-console |
| stokvel | http://localhost:8084/h2-console |
| ledger | http://localhost:8085/h2-console |
| merchant | http://localhost:8086/h2-console |
| fraud | http://localhost:8087/h2-console |
| webhook | http://localhost:8088/h2-console |

---

## Frontend Application

**Stack:** Angular 17 · Standalone components · TypeScript · SCSS · Signal-based state · Lazy-loaded routes

**Total: 64 source files**

### Feature Modules

| Route | Component | Description |
|-------|-----------|-------------|
| `/` | LandingComponent | Public marketing page — hero, features, comparison, testimonials, FAQ |
| `/auth/login` | LoginComponent | JWT login with form validation |
| `/auth/register` | RegisterComponent | 2-step registration (details → password) |
| `/dashboard` | DashboardComponent | Overview — wallet balance, recent payments, stokvels summary |
| `/dashboard/wallet` | WalletComponent | Balance card, top-up modal, transfer modal, paginated transaction history |
| `/dashboard/payments` | PaymentsComponent | Send money, request money, approve/decline requests, refund, history with filter tabs |
| `/dashboard/stokvel` | StokvelComponent | Create/join/browse stokvels, contribute, trigger payouts, detail view |
| `/dashboard/merchant` | MerchantComponent | Register business, generate API keys (shown once), payment history, refunds |
| `/dashboard/profile` | ProfileComponent | Edit personal details, change password, account info, danger zone |
| `/dashboard/security` | SecurityComponent | Risk tier, velocity stats, transaction evaluation history with score display |
| `/dashboard/webhooks` | WebhooksComponent | Register HTTPS endpoints, subscribe to events, view delivery history |
| `/dashboard/admin` | AdminComponent | User management (suspend/reactivate/promote), wallet management (freeze/unfreeze) |

### Core Services

| Service | Calls |
|---------|-------|
| `AuthService` | `/api/auth/**` — login, register, profile, change password, refresh token |
| `WalletService` | `/api/wallets/**` — balance, top-up, transfer, transaction history |
| `PaymentService` | `/api/payments/**` — send, request, approve, decline, refund, history |
| `StokvelService` | `/api/stokvels/**` — CRUD, join, contribute, payouts |
| `MerchantService` | `/api/merchants/**` — register, API keys, payments |
| `LedgerService` | `/api/ledger/**` — account, entries, statement |
| `FraudService` | `/api/fraud/**` — risk profile, evaluation history, alerts |
| `WebhookService` | `/api/webhooks/**` — endpoints, events, deliveries |
| `AdminService` | `/api/admin/**` — user management, wallet management |

### Shared Infrastructure

| Item | Description |
|------|-------------|
| `JwtInterceptor` | Attaches `Authorization: Bearer <token>` to every request, handles 401 → logout |
| `AuthGuard` | Blocks unauthenticated access to `/dashboard/**` |
| `GuestGuard` | Redirects logged-in users away from `/auth/**` |
| `ToastService` + `ToastComponent` | Signal-based toast notifications (success/error/info/warning) |
| `ConfirmDialogComponent` | Reusable confirmation modal with danger mode |
| `ZarPipe` | Formats amounts as `R 1,234.50` with optional sign |
| `RelativeDatePipe` | Human-readable dates: "2h ago", "3d ago" |

---

## API Reference

All requests through the **gateway on port 8080**.
Protected routes require: `Authorization: Bearer <token>`

### Auth
```
POST   /api/auth/register              { firstName, lastName, email, password, phoneNumber? }
POST   /api/auth/login                 { email, password }
GET    /api/auth/me                    ← current user
PUT    /api/auth/me                    { firstName?, lastName?, phoneNumber? }
PUT    /api/auth/change-password       { currentPassword, newPassword }
POST   /api/auth/refresh               { refreshToken }
```

### Admin — Users
```
GET    /api/admin/users                ?page=0&size=20
GET    /api/admin/users/{id}
POST   /api/admin/users/{id}/suspend
POST   /api/admin/users/{id}/reactivate
POST   /api/admin/users/{id}/promote
```

### Wallet
```
POST   /api/wallets                    ← create wallet
GET    /api/wallets/me
POST   /api/wallets/top-up             { amount, referenceId? }
POST   /api/wallets/transfer           { recipientUserId, amount, description? }
GET    /api/wallets/transactions       ?page=0&size=20
```

### Admin — Wallets
```
GET    /api/admin/wallets              ?page=0&size=20
POST   /api/admin/wallets/{userId}/freeze
POST   /api/admin/wallets/{userId}/unfreeze
```

### Payments
```
POST   /api/payments/send              { recipientUserId, amount, description?, referenceNote? }
POST   /api/payments/request           { fromUserId, amount, description? }
POST   /api/payments/requests/{id}/approve
POST   /api/payments/requests/{id}/decline
POST   /api/payments/{id}/refund
GET    /api/payments/history           ?page=0&size=20
GET    /api/payments/pending-requests
GET    /api/payments/{id}
```

### Stokvels
```
POST   /api/stokvels                   { name, type (ROSCA|POOL), contributionAmount, contributionFrequency, maxMembers }
GET    /api/stokvels                   ← all stokvels
GET    /api/stokvels/my                ← my stokvels
GET    /api/stokvels/{id}
POST   /api/stokvels/{id}/join
POST   /api/stokvels/{id}/activate     ← admin only
POST   /api/stokvels/{id}/contribute   { amount, transactionId }
POST   /api/stokvels/{id}/payouts/rosca               ← admin, ROSCA only
POST   /api/stokvels/{id}/payouts/pool/{recipientId}  ← admin, POOL only
GET    /api/stokvels/{id}/contributions
GET    /api/stokvels/{id}/payouts
```

### Ledger
```
POST   /api/ledger/accounts            { ownerId, type }
GET    /api/ledger/accounts/me
GET    /api/ledger/accounts/{ownerId}
GET    /api/ledger/entries             ?page=0&size=20
GET    /api/ledger/statement           ?from=ISO_DATE&to=ISO_DATE
POST   /api/ledger/transactions/{id}/reverse
```

### Merchant
```
POST   /api/merchants                  { businessName, category, ... }
GET    /api/merchants/my
GET    /api/merchants/{id}
POST   /api/merchants/{id}/approve     ← admin only
POST   /api/merchants/{id}/suspend     ← admin only
POST   /api/merchants/{id}/api-keys    { label, environment (TEST|LIVE) }
GET    /api/merchants/{id}/api-keys
DELETE /api/merchants/{id}/api-keys/{keyId}
POST   /api/merchants/{id}/payments/charge       { amount, payerUserId, description?, merchantReference? }
POST   /api/merchants/{id}/payments/{id}/refund
GET    /api/merchants/{id}/payments    ?page=0&size=20
```

### Fraud
```
GET    /api/fraud/profile/me
GET    /api/fraud/history/me           ?page=0&size=20
GET    /api/fraud/alerts               ?page=0&size=20
POST   /api/fraud/alerts/{id}/resolve  { resolution, notes? }
POST   /api/fraud/users/{id}/blacklist
POST   /api/fraud/users/{id}/unblacklist
GET    /api/fraud/users/{id}/profile
```

### Webhooks
```
POST   /api/webhooks/endpoints         { targetUrl, subscribedEvents[] }
GET    /api/webhooks/endpoints
POST   /api/webhooks/endpoints/{id}/pause
DELETE /api/webhooks/endpoints/{id}
GET    /api/webhooks/events
GET    /api/webhooks/events/{id}/deliveries
```

---

## Cross-Service Call Chain

### Send Money
```
POST /api/payments/send
  1. fraud-service     → score transaction           BLOCKED = abort
  2. wallet-service    → debit sender
  3. wallet-service    → credit recipient
  4. ledger-service    → post TRANSFER entry
  5. webhook-service   → fire payment.completed
```

### Stokvel Contribution
```
POST /api/stokvels/{id}/contribute
  1. fraud-service     → score contribution          BLOCKED = abort
  2. wallet-service    → debit member wallet
  3. ledger-service    → post STOKVEL_CONTRIBUTION
  4. webhook-service   → fire stokvel.contribution
```

### ROSCA Payout
```
POST /api/stokvels/{id}/payouts/rosca
  1. wallet-service    → credit recipient
  2. ledger-service    → post STOKVEL_PAYOUT
  3. webhook-service   → fire stokvel.payout
```

### Merchant Charge
```
POST /api/merchants/{id}/payments/charge
  1. wallet-service    → debit customer (full amount)
  2. wallet-service    → credit merchant  (net of 1.5% fee)
  3. ledger            → (planned)
```

---

## Event Types

| Event | Fired by | Trigger |
|-------|---------|---------|
| `payment.completed` | payment-service | Successful P2P transfer |
| `payment.refunded` | payment-service | Refund processed |
| `stokvel.contribution` | stokvel-service | Member contributes to stokvel |
| `stokvel.payout` | stokvel-service | ROSCA rotation or pool withdrawal |

Webhook deliveries are signed with **HMAC-SHA256**.
Verify with: `X-SafiPay-Signature: sha256=<hex>`

---

## Quick Start

### Prerequisites
- Java 17+, Maven 3.8+
- Node.js 18+, Angular CLI 17 (`npm i -g @angular/cli`)

### Backend — start all services

```bash
# Run each in its own terminal in order:
cd backend/user-service     && mvn spring-boot:run   # 8081
cd backend/wallet-service   && mvn spring-boot:run   # 8082
cd backend/payment-service  && mvn spring-boot:run   # 8083
cd backend/stokvel-service  && mvn spring-boot:run   # 8084
cd backend/ledger-service   && mvn spring-boot:run   # 8085
cd backend/merchant-service && mvn spring-boot:run   # 8086
cd backend/fraud-service    && mvn spring-boot:run   # 8087
cd backend/webhook-service  && mvn spring-boot:run   # 8088
cd backend/gateway-service  && mvn spring-boot:run   # 8080 ← start last
```

Or build all at once from the backend root:
```bash
cd backend && mvn clean package -DskipTests
```

### Frontend

```bash
cd frontend/safipay-app
npm install
ng serve
# → http://localhost:4200
```

---

## Environment Config

### Backend `application.yml` (all services share this secret)
```yaml
jwt:
  secret: safipay-super-secret-key-for-jwt-signing-must-be-256-bits-long
  expiration: 86400000        # 24h access token
  refresh-expiration: 604800000  # 7d refresh token
```

> ⚠️ **Change the JWT secret before any production deployment.**

### Frontend `src/environments/environment.ts`
```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'  // Gateway
};
```

### Service URL dependencies
```
gateway-service  → routes to all 8 services
payment-service  → wallet (8082), fraud (8087), ledger (8085), webhook (8088)
stokvel-service  → wallet (8082), fraud (8087), ledger (8085), webhook (8088)
merchant-service → wallet (8082)
ledger-service   → (standalone)
fraud-service    → (standalone)
webhook-service  → (standalone — delivers to external URLs)
```

---

## Fraud Rule Engine

The `FraudRuleEngine` scores every transaction 0–100 using 11 rules:

| Rule | Score Delta | Triggers when |
|------|-------------|---------------|
| `USER_BLACKLISTED` | +100 | User is on blacklist |
| `OPEN_FRAUD_ALERTS` | +20 per alert | User has unresolved alerts |
| `VELOCITY_TX_COUNT` | +25 | ≥10 transactions in last hour |
| `VELOCITY_AMOUNT_HOURLY` | +25 | ≥R10,000 in last hour |
| `VELOCITY_AMOUNT_DAILY` | +20 | ≥R50,000 in last day |
| `AMOUNT_10X_ABOVE_AVERAGE` | +30 | Amount > 10× user's average |
| `AMOUNT_5X_ABOVE_AVERAGE` | +15 | Amount > 5× user's average |
| `LARGE_AMOUNT_50K` | +20 | Single transaction ≥R50,000 |
| `LARGE_AMOUNT_20K` | +10 | Single transaction ≥R20,000 |
| `REPEATED_FAILURES` | +30 | ≥3 blocked transactions in last hour |
| `NEW_DEVICE` | +10 | Device ID changed |
| `NEW_IP_ADDRESS` | +5 | IP address changed |

**Thresholds:** LOW < 30 · MEDIUM 30–59 · HIGH 60–79 · CRITICAL ≥80

**Decisions:** APPROVED → REVIEW → BLOCKED

---

## Roadmap

- [ ] **PostgreSQL** — replace H2 datasources for persistence
- [ ] **Docker Compose** — full stack local dev in one command
- [ ] **Refresh token rotation** — silent token refresh in Angular JWT interceptor
- [ ] **Email verification** — send verification link on registration
- [ ] **Push notifications** — WebSocket or SSE for real-time updates
- [ ] **Stokvel invites** — share invite link by email or phone
- [ ] **Ledger in merchant-service** — post ledger entries on merchant charges
- [ ] **Redis rate limiting** — replace in-memory token bucket in gateway
- [ ] **ML fraud scoring** — replace rule engine with a trained model
- [ ] **Unit tests** — JUnit 5 + Mockito (backend), Jasmine (Angular)
- [ ] **POPIA compliance** — data retention, right to erasure
- [ ] **Production secrets** — Vault or AWS Secrets Manager for JWT secret
