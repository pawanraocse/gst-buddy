import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { adminGuard } from './core/guards/admin.guard';
import { AppLayoutComponent } from './layout/app-layout.component';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'signup/personal',
        loadComponent: () => import('./features/auth/signup-personal.component').then(m => m.SignupPersonalComponent)
      },
      {
        path: 'verify-email',
        loadComponent: () => import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/pwd-reset.component').then(m => m.PasswordResetComponent)
      }
    ]
  },
  {
    path: 'app',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'settings/account',
        loadComponent: () => import('./features/settings/account-settings.component').then(m => m.AccountSettingsComponent)
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          {
            path: 'dashboard',
            loadComponent: () => import('./features/admin/admin-dashboard.component').then(m => m.AdminDashboardComponent)
          },
          {
            path: 'users',
            loadComponent: () => import('./features/admin/admin-users.component').then(m => m.AdminUsersComponent)
          },
          {
            path: 'users/:userId',
            loadComponent: () => import('./features/admin/admin-user-detail.component').then(m => m.AdminUserDetailComponent)
          },
          {
            path: 'plans',
            loadComponent: () => import('./features/admin/admin-plans.component').then(m => m.AdminPlansComponent)
          },
          {
            path: 'credits',
            loadComponent: () => import('./features/admin/admin-credits.component').then(m => m.AdminCreditsComponent)
          }
        ]
      }
    ]
  },
  {
    path: 'auth/callback',
    loadComponent: () => import('./features/auth/callback.component').then(m => m.AuthCallbackComponent)
  },
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  { path: '**', redirectTo: '' }
];
