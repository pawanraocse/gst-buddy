# High-Level Design: GstBuddy

**Version:** 1.0 (Lite)
**Last Updated:** 2026-01-22

**Core Mission:** A streamlined, production-ready foundation for **Multi-Tenant SaaS** applications, focusing on simplicity and ease of use.

---

## 📚 Documentation Map

| Guide | Description |
|-------|-------------|
| 🚀 **[Quick Start](docs/QUICK_START.md)** | **Start Here!** Prerequisites and deployment. |
| 🔐 **[Authentication](docs/AUTHENTICATION.md)** | Signup, Login, and JWT flows. |
| 🗄️ **[Database Schema](docs/DATABASE_SCHEMA.md)** | Service schemas and entity relationships. |
| 💳 **[Billing](docs/BILLING.md)** | Payment integration (Planned). |
| ☁️ **[AWS Deployment](docs/AWS_DEPLOYMENT.md)** | Deployment guides. |
| 🔧 **[Configuration](docs/CONFIGURATION.md)** | Central configuration reference. |
| 🔍 **[Debugging](docs/DEBUGGING.md)** | Troubleshooting guide. |

---

## 🎯 Architecture

```mermaid
graph TD
    User((User)) -->|HTTPS| CF[CloudFront]
    CF -->|ALB| Gateway[Spring Cloud Gateway]
    
    subgraph "ECS Cluster"
    Gateway -->|Auth| Auth[Auth Service]
    Gateway -->|API| Backend[Backend Service]
    Gateway -->|Discovery| Eureka[Eureka Server]
    end
    
    Auth -->|Identity| Cognito[AWS Cognito]
    Auth -->|Shared DB| DB[(PostgreSQL)]
    Backend -->|Shared DB| DB
    
    note right of DB: Discriminated by tenant_id
```

### Key Principles
1. **Multi-Tenancy:** Shared database with discriminator column (`tenant_id`) strategy.
2. **Simplicity:** Single PostgreSQL instance, shared schema for efficiency.
3. **Security:** Gateway-based JWT validation, standard Spring Security.
3. **Infrastructure as Code:** 100% Terraform-managed.
4. **Developer Experience:** Docker Compose for local dev.

## 🚀 Quick Links
- **[Status Tracking](docs/STATUS.md)**
