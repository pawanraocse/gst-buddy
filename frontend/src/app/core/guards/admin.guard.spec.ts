import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { provideZonelessChangeDetection, signal, computed } from '@angular/core';
import { adminGuard } from './admin.guard';
import { AuthService } from '../auth.service';

function createAuthServiceStub(authenticated: boolean, superAdmin: boolean) {
  const userSignal = signal(superAdmin ? { role: 'super-admin' } : { role: 'user' });
  return {
    checkAuth: jasmine.createSpy('checkAuth').and.resolveTo(authenticated),
    user: userSignal,
    isAuthenticated: signal(authenticated),
    isSuperAdmin: computed(() => userSignal()?.role === 'super-admin')
  };
}

describe('adminGuard', () => {
  let routerSpy: jasmine.SpyObj<Router>;
  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/app/admin/dashboard' } as RouterStateSnapshot;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
  });

  it('should allow access for authenticated super-admin', async () => {
    const authStub = createAuthServiceStub(true, true);

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authStub },
        { provide: Router, useValue: routerSpy }
      ]
    });

    const result = await TestBed.runInInjectionContext(() => adminGuard(mockRoute, mockState));

    expect(result).toBeTrue();
  });

  it('should redirect unauthenticated users to login', async () => {
    const authStub = createAuthServiceStub(false, false);
    const fakeUrlTree = {} as any;
    routerSpy.createUrlTree.and.returnValue(fakeUrlTree);

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authStub },
        { provide: Router, useValue: routerSpy }
      ]
    });

    const result = await TestBed.runInInjectionContext(() => adminGuard(mockRoute, mockState));

    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/auth/login'], {
      queryParams: { returnUrl: '/app/admin/dashboard' }
    });
    expect(result).toBe(fakeUrlTree);
  });

  it('should redirect authenticated non-admin to dashboard', async () => {
    const authStub = createAuthServiceStub(true, false);
    const fakeUrlTree = {} as any;
    routerSpy.createUrlTree.and.returnValue(fakeUrlTree);

    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        { provide: AuthService, useValue: authStub },
        { provide: Router, useValue: routerSpy }
      ]
    });

    const result = await TestBed.runInInjectionContext(() => adminGuard(mockRoute, mockState));

    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/app/dashboard']);
    expect(result).toBe(fakeUrlTree);
  });
});
