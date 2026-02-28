# High-Level Design: GstBuddy

**Version:** 1.1
**Last Updated:** 2026-02-22

**Core Mission:** A streamlined, production-ready foundation for **Multi-Tenant SaaS** applications, focusing on simplicity and ease of use.

---

## 📚 Documentation Map

| Guide | Description |
|-------|-------------|
| 📋 **[Implementation Plan](IMPLEMENTATION_PLAN.md)** | Master plan; phases; Rule 37; clarifying decisions. |
| 📐 **[Phase 1 LLD](docs/PHASE1_LLD.md)** | Phase 1: SOLID, API contract, verification gates. |
| 🚀 **[Quick Start](docs/QUICK_START.md)** | **Start Here!** Prerequisites and deployment. |
| 🔐 **[Authentication](docs/AUTHENTICATION.md)** | Signup, Login, JWT flows, and admin bootstrap. |
| 🛡️ **[Admin Panel](docs/ADMIN_PANEL.md)** | Super-admin dashboard, user/plan/credit management, RBAC. |
| 🗄️ **[Database Schema](docs/DATABASE_SCHEMA.md)** | Service schemas and entity relationships. |
| 💳 **[Billing](docs/BILLING.md)** | Payment integration (Planned). |
| ☁️ **[AWS Deployment](docs/AWS_DEPLOYMENT.md)** | Deployment guides. |
| 🔧 **[Configuration](docs/CONFIGURATION.md)** | Central configuration reference. |
| 🤝 **[Referral System](docs/REFERRAL.md)** | Referral codes, credit rewards, signup integration. |
| 🔍 **[Debugging](docs/DEBUGGING.md)** | Troubleshooting guide. |

---

## 🎯 Architecture

```mermaid
graph TD
    User((User)) -->|HTTPS| CF[CloudFront]
    Admin((Super-Admin)) -->|HTTPS| CF
    CF -->|ALB| Gateway[Spring Cloud Gateway]
    
    subgraph "ECS Cluster"
    Gateway -->|Auth + Admin API| Auth[Auth Service]
    Gateway -->|Business API| Backend[Backend Service]
    Gateway -->|Discovery| Eureka[Eureka Server]
    end
    
    Auth -->|Identity| Cognito[AWS Cognito]
    Auth -->|Shared DB| DB[(PostgreSQL)]
    Backend -->|Shared DB| DB
    
    note right of Auth: RBAC via permissions table
    note right of DB: Discriminated by tenant_id
```

### Key Principles
1. **Multi-Tenancy:** Shared database with discriminator column (`tenant_id`) strategy.
2. **Simplicity:** Single PostgreSQL instance, shared schema for efficiency.
3. **Security:** Gateway-based JWT validation, AOP-based RBAC (`@RequirePermission`).
4. **Admin Panel:** Super-admin role with full platform management (users, credits, plans). See [Admin Panel](docs/ADMIN_PANEL.md).
5. **Infrastructure as Code:** 100% Terraform-managed.
6. **Developer Experience:** Docker Compose for local dev.

## 🚀 Quick Links
- **[Status Tracking](docs/STATUS.md)**
