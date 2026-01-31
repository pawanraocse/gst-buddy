# Authentication & Security

## Overview
Cloud-Infra-Lite uses **AWS Cognito** for identity management and **Spring Cloud Gateway** for request validation. The architecture is simplified for a single-tenant SaaS application.

## Authentication Flow

1. **Signup/Login**: Users authenticate directly with AWS Cognito (User Pool) via the Frontend.
2. **Token Issuance**: Cognito returns an ID Token and Access Token.
3. **API Requests**: Frontend sends the **ID Token** in the `Authorization` header (`Bearer <token>`).
4. **Gateway Validation**:
   - Validates JWT signature against Cognito JWKS.
   - Checks token expiration.
   - Filters out spoofed headers.
   - Forwards request to downstream services (Auth Service, Backend Service).

```mermaid
graph LR
    User[Frontend User] -->|1. Login| Cognito[AWS Cognito]
    Cognito -->|2. JWT| User
    User -->|3. API Request| Gateway[API Gateway]
    Gateway -->|4. Validate JWT| Gateway
    Gateway -->|5. Forward| Backend[Backend/Auth Service]
    Backend -->|6. Data Access| DB[(PostgreSQL)]
```

## Security Components

### 1. API Gateway
- **JWT Validation**: Uses Spring Security OAuth2 Resource Server to validate tokens.
- **Header Sanitization**: Removes unsafe headers (`X-User-Id`, `X-Email`) from incoming requests to prevent spoofing.
- **Context Injection**: Extracts user claims (Subject, Email) from the verified JWT and injects them as trusted headers for downstream services.

### 2. Downstream Services (Backend/Auth)
- **Trust Boundary**: Accepts requests only from the Gateway (ensured via network policy/security groups).
- **User Context**: Reads `X-User-Id` and `X-Email` headers to identify the calling user.
- **Stateless**: No session state is maintained in the services.

## Configuration

### Cognito Setup (Terraform)
- **User Pool**: Manages users and groups.
- **App Client**: Used by the frontend for authentication.
- **Domain**: Hosted UI for login/signup (optional, if not using custom UI).

### Service Configuration
Services load Cognito configuration from AWS Systems Manager (SSM) Parameter Store at startup:
- `/cloud-infra-lite/<env>/cognito/user_pool_id`
- `/cloud-infra-lite/<env>/cognito/issuer_uri`
- `/cloud-infra-lite/<env>/cognito/jwks_uri`

## Testing
- **Local Development**: Use the `scripts/create-admin-user.sh` script to create a test user in Cognito.
- **Postman**: Obtain a JWT from Cognito (via AWS CLI or helper script) and send it in the `Authorization` header.
