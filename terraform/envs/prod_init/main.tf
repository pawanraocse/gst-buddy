# AWS deployment with RDS and ElastiCache (~$15-30/month)
# Uses Free Tier where possible

terraform {
  required_version = ">= 1.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0.0"
    }
  }

  backend "s3" {
    bucket         = "gstbuddies-terraform-state-045084720221-ap-south-1"
    key            = "prod_init/terraform.tfstate"
    region         = "ap-south-1"
    dynamodb_table = "gstbuddies-terraform-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
      CostCenter  = "Budget"
    }
  }
}

# Aliased provider strictly for ACM Certificates required by CloudFront
provider "aws" {
  alias  = "us-east-1"
  region = "us-east-1"

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
      CostCenter  = "Budget"
    }
  }
}

# =============================================================================
# Local Variables
# =============================================================================

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

data "aws_caller_identity" "current" {}

# =============================================================================
# Route53 & ACM (SSL)
# =============================================================================

data "aws_route53_zone" "main" {
  name         = var.domain_name
  private_zone = false
}

resource "aws_acm_certificate" "wildcard" {
  provider                  = aws.us-east-1
  domain_name               = "*.${var.domain_name}"
  subject_alternative_names = [var.domain_name]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "validation" {
  for_each = {
    for dvo in aws_acm_certificate.wildcard.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = data.aws_route53_zone.main.zone_id
}

resource "aws_acm_certificate_validation" "main" {
  provider                = aws.us-east-1
  certificate_arn         = aws_acm_certificate.wildcard.arn
  validation_record_fqdns = [for record in aws_route53_record.validation : record.fqdn]
}

# =============================================================================
# VPC Module
# =============================================================================

module "vpc" {
  source = "../../modules/vpc"

  project_name = var.project_name
  environment  = var.environment

  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones

  # Budget: No NAT Gateway (EC2 in public subnet has direct internet access)
  enable_nat_gateway = false

  # Enabled for security monitoring
  enable_flow_logs = true
}




# =============================================================================
# RDS PostgreSQL (Free Tier: db.t3.micro)
# =============================================================================

module "rds" {
  source = "../../modules/rds"

  project_name = var.project_name
  environment  = var.environment

  vpc_id               = module.vpc.vpc_id
  db_subnet_group_name = module.vpc.db_subnet_group_name

  # Free Tier eligible!
  use_aurora     = false
  instance_class = "db.t3.micro"

  # Minimal storage
  allocated_storage     = 20
  max_allocated_storage = 0 # Disable autoscaling

  # Database settings
  database_name   = var.database_name
  master_username = var.database_username

  # Live Use: Protected from accidental deletion
  multi_az            = false
  deletion_protection = true
  skip_final_snapshot = false

  # Allow from EC2 bastion
  allowed_security_groups = [module.bastion.security_group_id]
}

# =============================================================================
# Bastion/EC2 Host (runs services via Docker Compose)
# =============================================================================

module "bastion" {
  source = "../../modules/bastion"

  project_name = var.project_name
  environment  = var.environment

  vpc_id    = module.vpc.vpc_id
  subnet_id = module.vpc.public_subnet_ids[0]

  instance_type           = var.ec2_instance_type
  allowed_ssh_cidr_blocks = var.allowed_ssh_cidr_blocks
  ssh_public_key          = var.ssh_public_key

  # Use Elastic IP for consistent address
  create_eip = true

  # Allow Lambda to access internal services (8081, 8083)
  # Note: Lambda SG must be created first, handled by depends_on in Lambda module
  lambda_security_group_id = module.cognito_pre_token_generation.security_group_id

  # Cognito permissions for auth-service (signup flow, SSO management)
  cognito_user_pool_arn = module.cognito_user_pool.user_pool_arn
}

# =============================================================================
# CloudFront (HTTPS Termination for EC2)
# =============================================================================

module "cloudfront" {
  source = "../../modules/cloudfront"

  origin_domain_name = module.bastion.public_dns
  origin_id          = "EC2-${module.bastion.instance_id}"
  comment            = "GSTbuddies API Gateway (HTTPS)"

  # Production Domain Settings
  aliases             = ["${var.api_subdomain}.${var.domain_name}"]
  acm_certificate_arn = aws_acm_certificate_validation.main.certificate_arn
}

resource "aws_route53_record" "api" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "${var.api_subdomain}.${var.domain_name}"
  type    = "A"

  alias {
    name                   = module.cloudfront.domain_name
    zone_id                = module.cloudfront.hosted_zone_id
    evaluate_target_health = false
  }
}

# =============================================================================
# ECR (Docker Image Registry)
# =============================================================================

module "ecr" {
  source = "../../modules/ecr"

  project_name = var.project_name
  environment  = var.environment

  services = [
    "gateway-service",
    "auth-service",
    "backend-service",
    "eureka-server"
  ]

  # Budget: Minimal image retention
  max_image_count      = 3
  untagged_image_days  = 7
  image_tag_mutability = "MUTABLE"
  scan_on_push         = true # Enabled for live security scanning
}

# Store ECR registry URL in SSM for easy access
resource "aws_ssm_parameter" "ecr_registry" {
  name  = "/${var.project_name}/${var.environment}/ecr/registry"
  type  = "String"
  value = split("/", module.ecr.repository_urls["gateway-service"])[0]
}


# =============================================================================
# Amplify (Frontend Hosting - Free Tier)
# =============================================================================

module "amplify" {
  source = "../../modules/amplify"

  project_name = var.project_name
  environment  = var.environment

  repository_url      = var.frontend_repository_url
  github_access_token = var.github_access_token
  branch_name         = var.frontend_branch
  app_name            = "frontend"
  app_root            = "" # Disable monorepo mode to avoid amplify.yml conflict

  environment_variables = {
    ANGULAR_APP_API_URL           = "https://${module.cloudfront.domain_name}"
    ANGULAR_APP_COGNITO_POOL_ID   = module.cognito_user_pool.user_pool_id
    ANGULAR_APP_COGNITO_CLIENT_ID = module.cognito_user_pool.spa_client_id
    ANGULAR_APP_COGNITO_DOMAIN    = "${module.cognito_user_pool.domain}.auth.${var.aws_region}.amazoncognito.com"
    ANGULAR_APP_COGNITO_REGION    = var.aws_region
  }

  enable_auto_build = var.enable_amplify_auto_build

  # Build spec with environment generation script
  build_spec = <<-EOT
    version: 1
    frontend:
      phases:
        preBuild:
          commands:
            - cd frontend
            - npm install
            - node scripts/generate-env.js
        build:
          commands:
            - npm run build
      artifacts:
        baseDirectory: frontend/dist/gstbuddies/browser
        files:
          - '**/*'
      cache:
        paths:
          - frontend/node_modules/**/*
    customHeaders:
      - pattern: '**/*'
        headers:
          - key: 'Cache-Control'
            value: 'public, max-age=31536000, immutable'
      - pattern: 'index.html'
        headers:
          - key: 'Cache-Control'
            value: 'no-cache, no-store, must-revalidate'
  EOT
}

# =============================================================================
# Amplify Domain Association
# =============================================================================

resource "aws_amplify_domain_association" "main" {
  app_id      = module.amplify.app_id
  domain_name = var.domain_name

  # https://gstbuddies.com
  sub_domain {
    branch_name = module.amplify.branch_name
    prefix      = ""
  }

  # https://www.gstbuddies.com
  sub_domain {
    branch_name = module.amplify.branch_name
    prefix      = "www"
  }

  wait_for_verification = false
}

# =============================================================================
# Cognito User Pool
# =============================================================================

module "cognito_user_pool" {
  source = "../../modules/cognito-user-pool"

  project_name   = var.project_name
  environment    = var.environment
  aws_region     = var.aws_region
  aws_account_id = data.aws_caller_identity.current.account_id

  # Cognito requires HTTPS or localhost - using localhost for budget
  callback_urls = ["http://localhost:4200/auth/callback", var.callback_url]
  logout_urls   = ["http://localhost:4200", var.logout_redirect_url]

  enable_google_social_login = var.enable_google_social_login
  google_client_id           = var.google_client_id
  google_client_secret       = var.google_client_secret
}

# =============================================================================
# Cognito Post-Confirmation Lambda
# =============================================================================

module "cognito_post_confirmation" {
  source = "../../modules/cognito-post-confirmation"

  project_name   = var.project_name
  environment    = var.environment
  aws_region     = var.aws_region
  aws_account_id = data.aws_caller_identity.current.account_id

  user_pool_id  = module.cognito_user_pool.user_pool_id
  user_pool_arn = module.cognito_user_pool.user_pool_arn
}

# =============================================================================
# Cognito Pre-Token-Generation Lambda
# =============================================================================

module "cognito_pre_token_generation" {
  source = "../../modules/cognito-pre-token-generation"

  project_name   = var.project_name
  environment    = var.environment
  aws_region     = var.aws_region
  aws_account_id = data.aws_caller_identity.current.account_id

  user_pool_id  = module.cognito_user_pool.user_pool_id
  user_pool_arn = module.cognito_user_pool.user_pool_arn

  # VPC mode: Lambda runs in VPC for private EC2 access
  enable_vpc_mode = true
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.public_subnet_ids # Public subnet (no NAT needed)


}

# =============================================================================
# Configure Cognito Lambda Triggers (via CLI to break circular dependency)
# =============================================================================

resource "null_resource" "configure_cognito_trigger" {
  triggers = {
    post_confirmation_arn    = module.cognito_post_confirmation.lambda_function_arn
    pre_token_generation_arn = module.cognito_pre_token_generation.lambda_arn
    user_pool_id             = module.cognito_user_pool.user_pool_id
  }

  provisioner "local-exec" {
    command = <<-EOT
      set -e
      aws cognito-idp update-user-pool \
        --user-pool-id ${module.cognito_user_pool.user_pool_id} \
        --auto-verified-attributes email \
        --lambda-config 'PostConfirmation=${module.cognito_post_confirmation.lambda_function_arn},PreTokenGenerationConfig={LambdaVersion=V2_0,LambdaArn=${module.cognito_pre_token_generation.lambda_arn}}' \
        --region ${var.aws_region}
      echo "✅ Cognito User Pool updated with Lambda triggers (PreToken V2_0)"
    EOT
  }

  depends_on = [
    module.cognito_user_pool,
    module.cognito_post_confirmation,
    module.cognito_pre_token_generation
  ]
}

# =============================================================================
# Cognito Identity Provider (if enabled)
# =============================================================================

module "cognito_identity_provider" {
  source = "../../modules/cognito-identity-provider"
  count  = var.enable_google_social_login ? 1 : 0

  project_name = var.project_name
  environment  = var.environment

  user_pool_id = module.cognito_user_pool.user_pool_id

  google_client_id     = var.google_client_id
  google_client_secret = var.google_client_secret
}

# =============================================================================
# SSM Parameters for Application Configuration
# =============================================================================

module "ssm_parameters" {
  source = "../../modules/ssm-parameters"

  project_name = var.project_name
  environment  = var.environment
  aws_region   = var.aws_region

  # Cognito configuration - using correct output names from local setup
  user_pool_id        = module.cognito_user_pool.user_pool_id
  client_id           = module.cognito_user_pool.native_client_id
  spa_client_id       = module.cognito_user_pool.spa_client_id
  client_secret       = module.cognito_user_pool.native_client_secret
  issuer_uri          = module.cognito_user_pool.issuer_uri
  jwks_uri            = module.cognito_user_pool.jwks_uri
  domain              = module.cognito_user_pool.domain
  hosted_ui_url       = module.cognito_user_pool.hosted_ui_url
  branding_id         = module.cognito_user_pool.branding_id
  callback_url        = var.callback_url
  logout_redirect_url = var.logout_redirect_url

  # SES configuration
  ses_from_email = var.ses_from_email
  ses_enabled    = false # Budget uses Cognito's built-in email

  depends_on = [module.cognito_user_pool]
}

# =============================================================================
# SSM Parameters for Docker Compose Configuration
# =============================================================================

resource "aws_ssm_parameter" "ec2_public_ip" {
  name        = "/${var.project_name}/${var.environment}/ec2/public_ip"
  description = "EC2 instance public IP"
  type        = "String"
  value       = module.bastion.public_ip

  tags = { Module = "budget" }
}

resource "aws_ssm_parameter" "api_url" {
  name        = "/${var.project_name}/${var.environment}/api/url"
  description = "API URL for frontend"
  type        = "String"
  value       = "https://${var.api_subdomain}.${var.domain_name}"

  tags = { Module = "prod_init" }
}

resource "random_password" "internal_api_key" {
  length           = 32
  special          = true
  override_special = "-_~"
}

resource "aws_ssm_parameter" "api_internal_key" {
  name        = "/${var.project_name}/${var.environment}/api/internal_key"
  description = "Internal API Key for backend bootstrapping"
  type        = "SecureString"
  value       = random_password.internal_api_key.result

  tags = { Module = "budget" }
}

# =============================================================================
# Razorpay Live Payment Keys (Managed manually via Console for security)
# =============================================================================

resource "aws_ssm_parameter" "razorpay_key_id" {
  name        = "/${var.project_name}/${var.environment}/razorpay/key_id"
  description = "Razorpay Live Key ID"
  type        = "SecureString"
  value       = "CHANGE_ME" # Set manually in console
  overwrite   = true

  lifecycle {
    ignore_changes = [value]
  }

  tags = { Module = "prod_init" }
}

resource "aws_ssm_parameter" "razorpay_key_secret" {
  name        = "/${var.project_name}/${var.environment}/razorpay/key_secret"
  description = "Razorpay Live Key Secret"
  type        = "SecureString"
  value       = "CHANGE_ME" # Set manually in console
  overwrite   = true

  lifecycle {
    ignore_changes = [value]
  }

  tags = { Module = "prod_init" }
}

# =============================================================================
# EC2 Instance Connect Endpoint (Secure Console Access via Private IP)
# =============================================================================

resource "aws_ec2_instance_connect_endpoint" "main" {
  subnet_id          = module.vpc.public_subnet_ids[0]
  security_group_ids = [module.bastion.security_group_id]

  tags = {
    Name   = "${local.name_prefix}-eice"
    Module = "prod_init"
  }
}
