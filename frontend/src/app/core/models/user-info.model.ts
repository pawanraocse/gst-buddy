/**
 * Authenticated user information from JWT token.
 */
export interface UserInfo {
    /** Cognito user ID (sub) */
    userId: string;

    /** User's email address */
    email: string;

    /** Whether the user's email is verified */
    emailVerified?: boolean;

    /** Tenant type (ORGANIZATION or PERSONAL) */
    tenantType?: string;

    /** User's full name */
    name?: string;

    /** Role from JWT custom:role claim (e.g. 'super-admin', 'admin') */
    role?: string;
}
