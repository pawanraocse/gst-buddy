
# Quick Start Guide

**Version:** 7.3 (Updated with Config & Debug Docs)
**Last Updated:** 2026-01-21

This guide gets you up and running with the SaaS Foundation template. It covers initial deployment, local development, and how to add your own services.

> **Related Docs:**
> - [CONFIGURATION.md](./CONFIGURATION.md) - Central config reference
> - [DEBUGGING.md](./DEBUGGING.md) - Troubleshooting guide
> - [ARCHITECTURE.md](./ARCHITECTURE.md) - System design

---

## 🚀 Quick Start (Local & Budget)

### Prerequisites
- Java 21+, Maven 3.9+
- Docker & Docker Compose
- Node.js 18+ (for frontend)
- AWS CLI configured with credentials
- Terraform 1.9+

### Step 1: Deploy AWS Infrastructure (Cognito)
Even for local development, we use a real AWS Cognito User Pool.

```bash
cd terraform
terraform init
terraform apply -auto-approve
```
This creates: Cognito User Pool, Lambda triggers, SSM parameters.

### Step 2: Start Backend Services
```bash
# From project root
docker-compose up -d
```
Services available at:
- Gateway: http://localhost:8080
- Frontend: http://localhost:4200
- Eureka Dashboard: http://localhost:8761

### Step 3: Create System Admin
```bash
ENVIRONMENT=local ./scripts/bootstrap-system-admin.sh your-admin@email.com "YourPassword123!"
```

### Step 4: Test the Flow
1. Navigate to http://localhost:4200
2. **Personal Signup (B2C):** Create account → Verify email → Login
3. **Organization Signup (B2B):** Create org → Invite users → Login
4. Access dashboard with your tenant's isolated data

### Troubleshooting
If something goes wrong, check the [DEBUGGING.md](./DEBUGGING.md) guide. Quick commands:

```bash
# Check all service logs
docker-compose logs --tail=100

# Check Eureka registrations
curl http://localhost:8761/eureka/apps

# Check database
docker exec -it GSTbuddies-postgres-dev psql -U postgres -d gst_buddy -c "SELECT * FROM users;"
```

---

## 🔧 Adding Your Own Service

The `backend-service` is a mimic/placeholder. Here's how to create your real domain service (e.g., `inventory-service` or `crm-service`).

### Step 1: Copy Backend-Service
```bash
cp -r backend-service/ my-new-service/
# Update pom.xml: artifactId, name
# Update application.yml: server.port (e.g., 8084)
```

### Step 2: Add Dependencies
Ensure `pom.xml` includes the shared infrastructure:
```xml
<dependency>
    <groupId>com.learning</groupId>
    <artifactId>common-infra</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Step 3: Configure Database
Standard Spring Boot configuration. The shared infrastructure handles the connection.

### Step 4: Register with Eureka
In `application.yml`:
```yaml
spring.application.name: my-new-service
eureka.client.service-url.defaultZone: http://eureka-server:8761/eureka
```

### Step 5: Add to Docker Compose
```yaml
my-new-service:
  build: ./my-new-service
  ports: ["8084:8084"]
  depends_on:
    eureka-server: {condition: service_healthy}
    postgres: {condition: service_healthy}
```

### Step 6: Add Gateway Route
In `gateway-service/application.yml`:
```yaml
- id: my-new-service
  uri: lb://my-new-service
  predicates:
    - Path=/my-new/**
```

---

## 🧪 How to Build Your Service

### 1. Domain Entities
The Backend Service exposes Rule 37 APIs (`/api/v1/ledgers/upload`, `/api/v1/rule37/runs`). See [IMPLEMENTATION_PLAN.md](../IMPLEMENTATION_PLAN.md) and [PHASE1_LLD.md](PHASE1_LLD.md) for design.

To add your own domain (e.g. orders):

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String customerId;
    private BigDecimal totalAmount;
    // Your domain fields...
}
```

### 2. Add Permissions
Protect your endpoints using `@RequirePermission`.

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping
    @RequirePermission(resource = "order", action = "read")
    public List<Order> getOrders() { ... }
    
    @PostMapping
    @RequirePermission(resource = "order", action = "create")
    public Order createOrder(@RequestBody OrderDto dto) { ... }
}
```

### 3. Register Permissions
Add SQL migration in `auth-service/src/main/resources/db/migration/`:

```sql
-- V100__add_order_permissions.sql
INSERT INTO permissions (id, resource, action, description) VALUES
    (gen_random_uuid(), 'order', 'read', 'View orders'),
    (gen_random_uuid(), 'order', 'create', 'Create orders');

-- Assign to admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'admin', id FROM permissions WHERE resource = 'order';
```

---

## 🛠️ Deployment & Configuration Flow

The project includes automated scripts for infrastructure provisioning.

### Automated Deployment Script
**Location:** `scripts/terraform/deploy.sh`

**What it does:**
1. **Deploys AWS Infrastructure**: VPC, RDS, Cognito, ALB, ECS.
2. **Stores Config in SSM**: Pushes all sensitive URLs and IDs to AWS Systems Manager Parameter Store.
3. **Auto-Updates Frontend**: Writes `environment.ts` with the new Cognito User Pool ID.

**Usage:**
```bash
./scripts/terraform/deploy.sh
```

### Backend Configuration (SSM)
Backend services read configuration from SSM at runtime. No hardcoded secrets!
Prefix: `/GSTbuddies/dev/cognito/*`

### Frontend Configuration
Frontend environment files are auto-generated by the deploy script.
Example `environment.ts`:
```typescript
export const environment = {
  production: false,
  cognito: {
    userPoolId: 'us-east-1_jjRFRnxGA',
    clientId: '...',
    region: 'us-east-1'
  }
};
```
