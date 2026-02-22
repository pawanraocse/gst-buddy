import { computed, inject, Injectable, signal } from '@angular/core';
import { fetchAuthSession, getCurrentUser, signIn, SignInOutput, signOut } from 'aws-amplify/auth';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { UserInfo } from './models';
import { environment } from '../../environments/environment';

/**
 * Authentication service
 *
 * Handles:
 * - Standard login (Email/Password)
 * - Social login (Google)
 * - Session state management
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  /** Current authenticated user */
  readonly user = signal<UserInfo | null>(null);

  /** Authentication state */
  readonly isAuthenticated = signal<boolean>(false);

  /** Whether current user has super-admin role */
  readonly isSuperAdmin = computed(() => this.user()?.role === 'super-admin');

  constructor() {
    this.checkAuth();
  }

  /**
   * Check current authentication state and load user info.
   */
  async checkAuth(): Promise<boolean> {
    try {
      const currentUser = await getCurrentUser();
      const session = await fetchAuthSession();
      const idToken = session.tokens?.idToken?.payload;

      if (idToken) {
        this.setUserInfo({
          userId: currentUser.userId,
          email: (idToken['email'] as string) || '',
          emailVerified: Boolean(idToken['email_verified']),
          tenantType: (idToken['custom:tenant_type'] as string) || (idToken['tenant_type'] as string),
          name: (idToken['name'] as string) || (idToken['given_name'] as string),
          role: (idToken['custom:role'] as string) || undefined
        });
        this.syncUserToBackend();
        return true;
      }
      return false;
    } catch (err) {
      this.clearAuth();
      return false;
    }
  }

  /**
   * Sign in with username (email) and password.
   */
  async login(email: string, password: string): Promise<SignInOutput> {
    try {
      const result = await signIn({ username: email, password });
      if (result.isSignedIn) {
        await this.checkAuth();
        this.router.navigate(['/app']);
      }
      return result;
    } catch (error) {
      console.error('Login failed', error);
      throw error;
    }
  }

  /**
   * Login with a social identity provider (e.g., Google).
   */
  async loginWithSocialProvider(provider: string): Promise<void> {
    console.log(`[Auth] Initiating ${provider} social login`);
    try {
      const { signInWithRedirect } = await import('aws-amplify/auth');
      await signInWithRedirect({
        provider: { custom: provider }
      });
    } catch (err) {
      console.error(`[Auth] ${provider} login failed:`, err);
      // Fallback
      this.fallbackSocialLogin(provider);
    }
  }

  // Fallback for manual social login redirect
  private fallbackSocialLogin(provider: string): void {
    const cognitoDomain = environment.cognito.domain;
    const clientId = environment.cognito.userPoolWebClientId;
    const redirectUri = encodeURIComponent(`${window.location.origin}/auth/callback`);

    const socialUrl = `https://${cognitoDomain}/oauth2/authorize` +
      `?identity_provider=${encodeURIComponent(provider)}` +
      `&response_type=code` +
      `&client_id=${clientId}` +
      `&redirect_uri=${redirectUri}` +
      `&scope=openid+email+profile`;

    window.location.href = socialUrl;
  }


  /**
   * Sign out and clear state.
   */
  async logout(): Promise<void> {
    try {
      await signOut();
    } catch (error) {
      console.error('Logout error', error);
    } finally {
      this.clearAuth();
      this.router.navigate(['/auth/login']);
    }
  }


  // ========== Email Verification ==========

  async resendVerificationEmail(email: string): Promise<void> {
    try {
      const { resendSignUpCode } = await import('aws-amplify/auth');
      await resendSignUpCode({ username: email });
    } catch (error) {
      throw error;
    }
  }

  async confirmSignUp(email: string, code: string): Promise<void> {
    try {
      const { confirmSignUp } = await import('aws-amplify/auth');
      await confirmSignUp({ username: email, confirmationCode: code });
    } catch (error) {
      throw error;
    }
  }

  // ========== Password Management ==========

  async forgotPassword(email: string): Promise<void> {
    try {
      const { resetPassword } = await import('aws-amplify/auth');
      await resetPassword({ username: email });
    } catch (error) {
      throw error;
    }
  }

  async resetPassword(email: string, code: string, newPassword: string): Promise<void> {
    try {
      const { confirmResetPassword } = await import('aws-amplify/auth');
      await confirmResetPassword({ username: email, confirmationCode: code, newPassword });
    } catch (error) {
      throw error;
    }
  }

  // ========== Private Helpers ==========

  /**
   * Fire-and-forget call to /auth/me which triggers upsertOnLogin
   * on the backend, ensuring the user exists in the DB.
   */
  private syncUserToBackend(): void {
    firstValueFrom(
      this.http.get(`${environment.apiUrl}/auth/api/v1/auth/me`)
    ).catch(() => { /* non-blocking */ });
  }

  private setUserInfo(info: UserInfo) {
    this.user.set(info);
    this.isAuthenticated.set(true);
  }

  private clearAuth() {
    this.user.set(null);
    this.isAuthenticated.set(false);
  }
}
