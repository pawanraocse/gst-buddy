-- V1: Simplified Authorization Schema
-- Predefined Role Bundles + Resource ACLs
-- ============================================================================

-- ============================================================================
-- 1. ROLES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    name VARCHAR(100) NOT NULL,
    description TEXT,
    scope VARCHAR(32) NOT NULL CHECK (scope IN ('PLATFORM', 'TENANT')),
    access_level VARCHAR(32), -- admin, editor, viewer - for custom roles
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_roles_tenant ON roles(tenant_id);
CREATE INDEX idx_roles_scope ON roles(scope);

COMMENT ON TABLE roles IS 'Predefined organization roles';
COMMENT ON COLUMN roles.scope IS 'PLATFORM for super-admin, TENANT for tenant roles';
COMMENT ON COLUMN roles.access_level IS 'Access level (admin, editor, viewer) - for custom roles';

-- ============================================================================
-- 2. USER_ROLES TABLE (User Role Assignments)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    user_id VARCHAR(255) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    assigned_by VARCHAR(255),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    UNIQUE(tenant_id, user_id, role_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_tenant ON user_roles(tenant_id);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

COMMENT ON TABLE user_roles IS 'Assigns roles to users';
COMMENT ON COLUMN user_roles.user_id IS 'Cognito user ID (sub claim)';

-- ============================================================================
-- 3. USERS TABLE (Tenant User Registry)
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    avatar_url TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INVITED', 'DISABLED')),
    source VARCHAR(32) NOT NULL DEFAULT 'COGNITO' CHECK (source IN ('COGNITO', 'SAML', 'OIDC', 'MANUAL', 'INVITATION')),
    first_login_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant_email ON users(tenant_id, email);
CREATE INDEX idx_users_status ON users(status);

COMMENT ON TABLE users IS 'Registry of all users in the tenant';

-- ============================================================================
-- 4. INVITATIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    email VARCHAR(255) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    invited_by VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

CREATE INDEX idx_invitations_tenant ON invitations(tenant_id);
CREATE INDEX idx_invitations_email ON invitations(email);
CREATE INDEX idx_invitations_tenant_email ON invitations(tenant_id, email);
CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_status ON invitations(status);

-- ============================================================================
-- 5. GROUP_ROLE_MAPPINGS TABLE (SSO Group to Role Mapping)
-- ============================================================================
CREATE TABLE IF NOT EXISTS group_role_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    external_group_id VARCHAR(512) NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    priority INTEGER DEFAULT 0,
    auto_assign BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    UNIQUE(tenant_id, external_group_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

CREATE INDEX idx_grm_tenant ON group_role_mappings(tenant_id);
CREATE INDEX idx_grm_role ON group_role_mappings(role_id);
CREATE INDEX idx_grm_group ON group_role_mappings(external_group_id);

COMMENT ON TABLE group_role_mappings IS 'Maps SSO groups to roles for auto-assignment';

-- ============================================================================
-- 6. ACL_ENTRIES TABLE (Resource-Level Permissions)
-- ============================================================================
-- Google Drive style sharing for folders/files
CREATE TABLE IF NOT EXISTS acl_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    resource_id UUID NOT NULL,
    resource_type VARCHAR(64) NOT NULL,       -- FOLDER, FILE, PROJECT
    principal_type VARCHAR(32) NOT NULL,      -- USER, GROUP, PUBLIC
    principal_id VARCHAR(255),                -- User/Group ID or null for PUBLIC
    role_bundle VARCHAR(32) NOT NULL,         -- VIEWER, CONTRIBUTOR, EDITOR, MANAGER
    granted_by VARCHAR(255),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    UNIQUE(tenant_id, resource_id, principal_type, principal_id)
);

CREATE INDEX idx_acl_tenant ON acl_entries(tenant_id);
CREATE INDEX idx_acl_resource ON acl_entries(resource_id);
CREATE INDEX idx_acl_tenant_resource ON acl_entries(tenant_id, resource_id);
CREATE INDEX idx_acl_principal ON acl_entries(principal_id) WHERE principal_id IS NOT NULL;
CREATE INDEX idx_acl_resource_type ON acl_entries(resource_type);

COMMENT ON TABLE acl_entries IS 'Resource-level ACL for folder/file sharing';
COMMENT ON COLUMN acl_entries.role_bundle IS 'VIEWER=read, CONTRIBUTOR=read+upload, EDITOR=edit, MANAGER=full+share';

-- ============================================================================
-- 7. SEED DATA - PREDEFINED ROLES
-- ============================================================================
INSERT INTO roles (id, name, description, scope, access_level) VALUES
('super-admin', 'SUPER_ADMIN', 'Platform administrator with full system access', 'PLATFORM', 'admin'),
('admin', 'ADMIN', 'Tenant administrator with full tenant access', 'TENANT', 'admin'),
('editor', 'EDITOR', 'Can read, edit, delete, and share resources', 'TENANT', 'editor'),
('viewer', 'VIEWER', 'Read-only access to resources', 'TENANT', 'viewer'),
('guest', 'GUEST', 'Limited access to shared resources only', 'TENANT', 'guest')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 8. PERMISSIONS TABLE (Fine-grained permissions)
-- ============================================================================
CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, resource, action)
);

CREATE INDEX idx_permissions_tenant ON permissions(tenant_id);
CREATE INDEX idx_permissions_resource ON permissions(resource);

COMMENT ON TABLE permissions IS 'Defines all available permissions in the system';
COMMENT ON COLUMN permissions.id IS 'Format: resource:action (e.g., entry:read)';

-- ============================================================================
-- 9. ROLE_PERMISSIONS TABLE (Role to Permission mapping)
-- ============================================================================
CREATE TABLE IF NOT EXISTS role_permissions (
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    role_id VARCHAR(64) NOT NULL,
    permission_id VARCHAR(128) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);


CREATE INDEX idx_role_permissions_tenant ON role_permissions(tenant_id);
CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

COMMENT ON TABLE role_permissions IS 'Maps roles to their granted permissions';

-- ============================================================================
-- 10. SEED DATA - PERMISSIONS
-- ============================================================================
INSERT INTO permissions (id, resource, action, description) VALUES
-- Entry permissions
('entry:create', 'entry', 'create', 'Create new entries'),
('entry:read', 'entry', 'read', 'View entries'),
('entry:update', 'entry', 'update', 'Edit existing entries'),
('entry:delete', 'entry', 'delete', 'Delete entries'),
-- User permissions
('user:read', 'user', 'read', 'View user list'),
('user:invite', 'user', 'invite', 'Invite new users'),
('user:manage', 'user', 'manage', 'Manage user roles and permissions'),
-- Tenant permissions
('tenant:settings', 'tenant', 'settings', 'Manage tenant settings'),
-- SSO permissions
('sso:read', 'sso', 'read', 'View SSO configuration'),
('sso:manage', 'sso', 'manage', 'Configure SSO identity providers'),
-- Group permissions
('group:read', 'group', 'read', 'View IdP group mappings'),
('group:manage', 'group', 'manage', 'Manage group-to-role mappings'),
-- Admin / Platform permissions
('admin:dashboard', 'admin', 'dashboard', 'View platform admin dashboard'),
('credit:read', 'credit', 'read', 'View all user wallets and transactions'),
('credit:manage', 'credit', 'manage', 'Grant or revoke credits for any user'),
('plan:manage', 'plan', 'manage', 'Create, update, or deactivate pricing plans'),
('account:suspend', 'account', 'suspend', 'Suspend (disable) any user account'),
('account:delete', 'account', 'delete', 'Permanently delete any user account')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 11. SEED DATA - ROLE PERMISSION MAPPINGS
-- ============================================================================
-- Admin role - all permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('admin', 'entry:create'),
('admin', 'entry:read'),
('admin', 'entry:update'),
('admin', 'entry:delete'),
('admin', 'user:read'),
('admin', 'user:invite'),
('admin', 'user:manage'),
('admin', 'tenant:settings'),
('admin', 'sso:read'),
('admin', 'sso:manage'),
('admin', 'group:read'),
('admin', 'group:manage')
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;

-- Editor role - entry CRUD + user read
INSERT INTO role_permissions (role_id, permission_id) VALUES
('editor', 'entry:create'),
('editor', 'entry:read'),
('editor', 'entry:update'),
('editor', 'entry:delete'),
('editor', 'user:read')
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;

-- Viewer role - read only
INSERT INTO role_permissions (role_id, permission_id) VALUES
('viewer', 'entry:read'),
('viewer', 'user:read')
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;

-- Guest role - minimal read access
INSERT INTO role_permissions (role_id, permission_id) VALUES
('guest', 'entry:read')
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;

-- Super-admin role - ALL permissions (platform-wide access)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'super-admin', id FROM permissions
ON CONFLICT (tenant_id, role_id, permission_id) DO NOTHING;

-- ============================================================================
-- 12. AUDIT TRIGGER
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invitations_updated_at
BEFORE UPDATE ON invitations
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 13. PLANS TABLE (Credit-based pricing)
-- ============================================================================
CREATE TABLE IF NOT EXISTS plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    price_inr DECIMAL(10,2) NOT NULL DEFAULT 0,
    credits INTEGER NOT NULL,
    is_trial BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    description TEXT,
    validity_days INTEGER,           -- NULL = no expiry (future-ready)
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_plans_active ON plans(is_active, sort_order);

COMMENT ON TABLE plans IS 'Configurable credit-based pricing plans';
COMMENT ON COLUMN plans.validity_days IS 'NULL = no expiry. Future: set to e.g. 90 for 3-month validity';
COMMENT ON COLUMN plans.credits IS '1 credit = 1 ledger analysis';

-- ============================================================================
-- 14. USER CREDIT WALLETS (Per-user balance with optimistic locking)
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_credit_wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    total_credits INTEGER NOT NULL DEFAULT 0,
    consumed_credits INTEGER NOT NULL DEFAULT 0,
    has_used_trial BOOLEAN NOT NULL DEFAULT false,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, tenant_id)
);

CREATE INDEX idx_wallets_user_tenant ON user_credit_wallets(user_id, tenant_id);

COMMENT ON TABLE user_credit_wallets IS 'Per-user credit balance. version column for optimistic locking';
COMMENT ON COLUMN user_credit_wallets.has_used_trial IS 'Prevents trial abuse: only one trial per user';

CREATE TRIGGER update_wallets_updated_at
BEFORE UPDATE ON user_credit_wallets
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 15. CREDIT TRANSACTIONS (Immutable audit ledger)
-- ============================================================================
CREATE TABLE IF NOT EXISTS credit_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    type VARCHAR(20) NOT NULL CHECK (type IN ('GRANT', 'CONSUME', 'REFUND', 'ADJUSTMENT')),
    credits INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    reference_type VARCHAR(30) NOT NULL,  -- TRIAL, PLAN_PURCHASE, ANALYSIS, ADMIN_GRANT, PROMO, REFUND
    reference_id VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_user ON credit_transactions(user_id, created_at DESC);
CREATE INDEX idx_txn_idempotency ON credit_transactions(idempotency_key);
CREATE INDEX idx_txn_reference ON credit_transactions(reference_type, reference_id);

COMMENT ON TABLE credit_transactions IS 'Immutable audit ledger for all credit mutations';
COMMENT ON COLUMN credit_transactions.idempotency_key IS 'Prevents duplicate transactions (e.g. double-charge)';
COMMENT ON COLUMN credit_transactions.reference_type IS 'TRIAL | PLAN_PURCHASE | ANALYSIS | ADMIN_GRANT | PROMO | REFUND';

-- ============================================================================
-- 16. SEED DATA - PRICING PLANS
-- ============================================================================
INSERT INTO plans (name, display_name, price_inr, credits, is_trial, sort_order, description) VALUES
('trial', 'Trial', 0, 2, true, 1, 'Free starter — 2 ledger analyses'),
('pro', 'Pro', 1000, 5, false, 2, '5 ledger analyses'),
('ultra', 'Ultra', 3000, 30, false, 3, '30 ledger analyses')
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- 17. SEED DATA - SYSTEM ADMIN USER
-- ============================================================================
-- Placeholder system admin. The bootstrap script creates this user in Cognito,
-- then calls /api/v1/admin/bootstrap to replace the placeholder user_id with
-- the real Cognito sub.
-- ============================================================================
INSERT INTO users (user_id, tenant_id, email, name, status, source, created_at, updated_at) VALUES
('SYSTEM_ADMIN_PLACEHOLDER', 'default', 'system-admin@gst-buddy.local', 'System Admin', 'ACTIVE', 'MANUAL', NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_roles (tenant_id, user_id, role_id, assigned_by, assigned_at) VALUES
('default', 'SYSTEM_ADMIN_PLACEHOLDER', 'super-admin', 'SYSTEM', NOW())
ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;

-- ============================================================================
-- SCHEMA COMPLETE - Full Permission + Credit Model
-- ============================================================================
-- Org Roles: admin (full access), editor, viewer, guest (predefined capabilities)
-- Permissions: Fine-grained resource:action pairs mapped to roles
-- Resource ACLs: Fine-grained sharing via acl_entries table
-- Credit System: Plans → Wallets → Transactions (audit-safe, idempotent)
-- System Admin: Seeded with super-admin role; bootstrap script links Cognito sub
-- ============================================================================

