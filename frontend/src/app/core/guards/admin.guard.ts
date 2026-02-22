import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../auth.service';

/**
 * Guard that restricts route activation to authenticated super-admin users.
 * Falls back to /app/dashboard for authenticated non-admins or /auth/login for guests.
 */
export const adminGuard: CanActivateFn = async (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthenticated = await authService.checkAuth();
  if (!isAuthenticated) {
    return router.createUrlTree(['/auth/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  if (authService.isSuperAdmin()) {
    return true;
  }

  return router.createUrlTree(['/app/dashboard']);
};
