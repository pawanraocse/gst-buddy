# AWS Cognito Terraform - Production Ready

Production-ready Terraform configuration for AWS Cognito User Pool with **Modern Managed Login UI (v2)** and **Pre-Token Generation Lambda Trigger** for multi-tenant support.

---

## 🎨 Features

- ✅ **Modern Managed Login UI v2** - Beautiful, responsive login interface
- ✅ **Lambda Pre-Token Generation Trigger** - Injects `tenantId` into JWT tokens
- ✅ **Multi-tenant support** with custom attributes (`custom:tenantId`, `custom:role`)
- ✅ **OAuth 2.0 / OIDC** compliant
- ✅ **MFA support** (TOTP - Software Token, FREE)
- ✅ **User groups** (admin, admin, user)
- ✅ **SSM Parameter Store** for secure configuration
- ✅ **AWS Free Tier** optimized
- ✅ **Terraform 1.9+** and AWS Provider 6.17+

---

## 📋 Prerequisites

- Terraform >= 1.9.0
- AWS CLI configured with `personal` profile
- Node.js 20.x (for Lambda function)

---

## 🚀 Quick Start

### 1. Configure Variables

Edit `/terraform.tfvars`:

```hcl
aws_region   = "us-east-1"
project_name = "GSTbuddies-lite"
environment  = "dev"

callback_urls = [
  "http://localhost:8081/auth/login/oauth2/code/cognito",
  "http://localhost:3000/callback"
]

logout_urls = [
  "http://localhost:8081/auth/logged-out",
  "http://localhost:3000"
]

# Token validity
access_token_validity  = 60   # minutes
id_token_validity      = 60   # minutes
refresh_token_validity = 30   # days
```

### 2. Deploy

```bash
cd terraform
./deploy.sh
```

The script will:
1. ✅ Validate AWS credentials (using `personal` profile)
2. ✅ Initialize Terraform  
3. ✅ Create 23 resources including Lambda function
4. ✅ Export configuration to SSM Parameter Store
5. ✅ Display URLs and configuration

### 3. Access the Modern UI

After deployment:
```
https://GSTbuddies-lite-dev-XXXXXXXX.auth.us-east-1.amazoncognito.com/oauth2/authorize?...
```

---

## 📦 What Gets Deployed

| Resource | Name | Description |
|----------|------|-------------|
| **User Pool** | `GSTbuddies-lite-dev-user-pool` | Cognito user pool with custom attributes |
| **Lambda Function** | `GSTbuddies-lite-dev-pre-token-generation` | Injects tenantId into JWT tokens |
| **User Pool Domain** | `GSTbuddies-lite-dev-XXXXXXXX` | Hosted UI domain (Modern v2) |
| **User Pool Client** | `GSTbuddies-lite-dev-native-client` | OAuth2 client (with secret) |
| **User Groups** | admin, admin, user | Role-based groups |
| **SSM Parameters** | 11 parameters | Secure configuration storage |

---

##  🔧 Lambda Trigger Details

### Pre-Token Generation Lambda

The Lambda function automatically injects the user's `tenantId` from custom attributes into JWT tokens during authentication.

**Function:**
- **Name:** `GSTbuddies-lite-dev-pre-token-generation`
- **Runtime:** Node.js 20.x
- **Memory:** 128 MB
- **Timeout:** 3 seconds
- **Source:** `terraform/lambda/pre-token-generation/index.mjs`

**What it does:**
1. Reads `custom:tenantId` from user attributes
2. Injects into ID token and access token
3. Returns modified event to Cognito

**JWT Claims Added:**
```json
{
  "tenantId": "tenant-123",
  "custom:tenantId": "tenant-123"
}
```

---

## 📊 SSM Parameters

All configuration is stored in AWS Systems Manager Parameter Store for secure access.

### Current Deployment Paths

Replace `GSTbuddies-lite` and `dev` with your `project_name` and `environment`:

| Parameter | Path | Type |
|-----------|------|------|
| User Pool ID | `/GSTbuddies-lite/dev/cognito/user_pool_id` | String |
| Client ID | `/GSTbuddies-lite/dev/cognito/client_id` | String |
| Client Secret | `/GSTbuddies-lite/dev/cognito/client_secret` | SecureString |
| Issuer URI | `/GSTbuddies-lite/dev/cognito/issuer_uri` | String |
| JWKS URI | `/GSTbuddies-lite/dev/cognito/jwks_uri` | String |
| Domain | `/GSTbuddies-lite/dev/cognito/domain` | String |
| Hosted UI URL | `/GSTbuddies-lite/dev/cognito/hosted_ui_url` | String |
| Branding ID | `/GSTbuddies-lite/dev/cognito/branding_id` | String |
| Callback URL | `/GSTbuddies-lite/dev/cognito/callback_url` | String |
| Logout Redirect URL | `/GSTbuddies-lite/dev/cognito/logout_redirect_url` | String |
| AWS Region | `/GSTbuddies-lite/dev/aws/region` | String |

### Accessing SSM Parameters

```bash
# List all Cognito parameters
aws ssm get-parameters-by-path \
  --path "/GSTbuddies-lite/dev/cognito" \
  --region us-east-1 \
  --profile personal

# Get specific parameter
aws ssm get-parameter \
  --name "/GSTbuddies-lite/dev/cognito/user_pool_id" \
  --region us-east-1 \
  --profile personal

# Get client secret (encrypted)
aws ssm get-parameter \
  --name "/GSTbuddies-lite/dev/cognito/client_secret" \
  --with-decryption \
  --region us-east-1 \
  --profile personal

# View all parameters in table format
aws ssm get-parameters-by-path \
  --path "/GSTbuddies-lite/dev/cognito" \
  --with-decryption \
  --query 'Parameters[*].{Name:Name,Value:Value}' \
  --output table \
  --region us-east-1 \
  --profile personal
```

---

## 🧪 Testing the Lambda Trigger

### 1. Create Test User with Tenant ID

```bash
aws cognito-idp admin-create-user \
  --user-pool-id us-east-1_6RGxkqTmA \
  --username testuser@example.com \
  --user-attributes \
    Name=email,Value=testuser@example.com \
    Name=email_verified,Value=true \
    Name=custom:tenantId,Value=tenant-123 \
    Name=custom:role,Value=admin \
  --temporary-password "TempPass123!" \
  --profile personal \
  --region us-east-1
```

### 2. Set Permanent Password

```bash
aws cognito-idp admin-set-user-password \
  --user-pool-id us-east-1_6RGxkqTmA \
  --username testuser@example.com \
  --password "MySecurePass123!" \
  --permanent \
  --profile personal \
  --region us-east-1
```

### 3. Login and Get Token

Use the Hosted UI URL or test via CLI:
```bash
# Get the hosted UI URL
terraform output hosted_ui_url
```

### 4. Verify JWT Contains Tenant ID

Decode the access token at https://jwt.io

**Expected claims:**
```json
{
  "sub": "...",
  "email": "testuser@example.com",
  "tenantId": "tenant-123",
  "custom:tenantId": "tenant-123",
  "cognito:groups": ["admin"],
  ...
}
```

### 5. Check Lambda Logs

```bash
aws logs tail /aws/lambda/GSTbuddies-lite-dev-pre-token-generation \
  --follow \
  --profile personal \
  --region us-east-1
```

---

## 🔗 Spring Boot Integration

### Application Configuration

Update your `application.yml` to use SSM parameters:

```yaml
spring:
  cloud:
    aws:
      paramstore:
        enabled: true
        prefix: /GSTbuddies-lite
        profile-separator: /
        default-context: dev/cognito
  
  security:
    oauth2:
      client:
        registration:
          cognito:
            client-id: ${COGNITO_CLIENT_ID}
            client-secret: ${COGNITO_CLIENT_SECRET}
            scope: openid,email,profile,phone,aws.cognito.signin.user.admin
            redirect-uri: http://localhost:8081/auth/login/oauth2/code/cognito
            authorization-grant-type: authorization_code
        provider:
          cognito:
            issuer-uri: ${COGNITO_ISSUER_URI}
      resourceserver:
        jwt:
          issuer-uri: ${COGNITO_ISSUER_URI}
          jwk-set-uri: ${COGNITO_JWKS_URI}
```

### Fetching Tenant ID from JWT

```java
@GetMapping("/api/user/info")
public Map<String, Object> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
    String tenantId = jwt.getClaim("tenantId");
    String email = jwt.getClaim("email");
    
    return Map.of(
        "tenantId", tenantId,
        "email", email
    );
}
```

---

## 🗑️ Cleanup

To destroy all resources:

```bash
./destroy.sh
```

Or manually:
```bash
terraform destroy
```

**Note:** Both scripts use the `personal` AWS profile for safety.

---

## 🔐 Security Features

- ✅ Strong password policy (12+ chars, mixed case, numbers, symbols)
- ✅ Email verification required
- ✅ MFA support (Software Token - FREE)
- ✅ Token revocation enabled
- ✅ Device tracking
- ✅ Prevent user enumeration attacks
- ✅ Secure SSM storage for secrets
- ✅ Lambda basic execution role (least privilege)

---

## 💰 Cost Optimization (Free Tier)

All resources stay within AWS Free Tier:

| Service | Free Tier | Typical Usage |
|---------|-----------|---------------|
| Cognito | 50,000 MAUs/month | <1,000 |
| Lambda | 1M requests/month | <10,000 |
| CloudWatch Logs | 5GB storage | <100MB |
| SSM Parameters | 10,000 parameters | 11 |

**Estimated Monthly Cost: $0** 💵

---

## 📁 File Structure

```
terraform/
├── main.tf                              # Main Cognito configuration
├── lambda.tf                            # Lambda function resources
├── variables.tf                         # Input variables
├── outputs.tf                           # Output values
├── terraform.tfvars                     # Your configuration
├── deploy.sh                            # Deployment script (uses 'personal' profile)
├── destroy.sh                           # Cleanup script (uses 'personal' profile)
├── lambda/
│   └── pre-token-generation/
│       └── index.mjs                    # Lambda handler code
└── README.md                            # This file
```

---

## 🐛 Troubleshooting

### Issue: SSM Parameters Not Found

**Solution:** Ensure you're using the correct region and profile:
```bash
aws ssm get-parameter \
  --name "/GSTbuddies-lite/dev/cognito/user_pool_id" \
  --region us-east-1 \
  --profile personal
```

### Issue: Lambda Not Triggering

**Check Lambda logs:**
```bash
aws logs tail /aws/lambda/GSTbuddies-lite-dev-pre-token-generation --follow --profile personal --region us-east-1
```

### Issue: JWT Missing Tenant ID

**Verify user has custom attribute:**
```bash
aws cognito-idp admin-get-user \
  --user-pool-id us-east-1_6RGxkqTmA \
  --username testuser@example.com \
  --profile personal \
  --region us-east-1
```

---

## 📚 Resources

- **AWS Console Links** (Replace with your resources):
  - [Lambda Function](https://console.aws.amazon.com/lambda/home?region=us-east-1#/functions/GSTbuddies-lite-dev-pre-token-generation)
  - [Cognito User Pool](https://console.aws.amazon.com/cognito/v2/idp/user-pools/us-east-1_6RGxkqTmA)
  - [CloudWatch Logs](https://console.aws.amazon.com/cloudwatch/home?region=us-east-1#logsV2:log-groups)

- **Documentation:**
  - [AWS Cognito Docs](https://docs.aws.amazon.com/cognito/)
  - [Lambda Triggers](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-identity-pools-working-with-aws-lambda-triggers.html)
  - [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)

---

**Version:** 3.0.0  
**Last Updated:** 2025-11-25  
**Terraform:** >= 1.9.0  
**AWS Provider:** ~> 6.17  
**Features:** Modern UI v2 + Lambda Triggers ✨
