import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { AdminUserDetailComponent } from './admin-user-detail.component';
import { AdminApiService, AdminUserDetail, RoleDto } from '../../core/services/admin-api.service';

describe('AdminUserDetailComponent', () => {
  let component: AdminUserDetailComponent;
  let fixture: ComponentFixture<AdminUserDetailComponent>;
  let adminApiSpy: jasmine.SpyObj<AdminApiService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let messageAddSpy: jasmine.Spy;

  const mockUser: AdminUserDetail = {
    userId: 'u1', email: 'alice@test.com', name: 'Alice', avatarUrl: '',
    status: 'ACTIVE', source: 'COGNITO', tenantId: 'default',
    roles: ['admin'], wallet: { total: 100, used: 30, remaining: 70 },
    firstLoginAt: '2026-01-01', lastLoginAt: '2026-02-01', createdAt: '2025-12-01'
  };

  const mockRoles: RoleDto[] = [
    { id: 'admin', name: 'Admin', description: 'Admin role', scope: 'GLOBAL' },
    { id: 'viewer', name: 'Viewer', description: 'Read-only', scope: 'GLOBAL' }
  ];

  beforeEach(async () => {
    adminApiSpy = jasmine.createSpyObj('AdminApiService', [
      'getUserDetail', 'getRoles', 'getUserTransactions', 'enableUser',
      'suspendUser', 'deleteUser', 'assignRole', 'removeRole',
      'grantCredits', 'revokeCredits'
    ]);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    adminApiSpy.getUserDetail.and.returnValue(of(mockUser));
    adminApiSpy.getRoles.and.returnValue(of(mockRoles));
    adminApiSpy.getUserTransactions.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AdminUserDetailComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: AdminApiService, useValue: adminApiSpy },
        { provide: Router, useValue: routerSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'u1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserDetailComponent);
    component = fixture.componentInstance;

    const msgService = fixture.debugElement.injector.get(MessageService);
    messageAddSpy = spyOn(msgService, 'add');

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load user detail and roles on init', () => {
    expect(adminApiSpy.getUserDetail).toHaveBeenCalledWith('u1');
    expect(adminApiSpy.getRoles).toHaveBeenCalled();
    expect(component.user()?.email).toBe('alice@test.com');
    expect(component.availableRoles().length).toBe(2);
  });

  it('should load transactions on init', () => {
    expect(adminApiSpy.getUserTransactions).toHaveBeenCalledWith('u1');
    expect(component.transactions()).toEqual([]);
    expect(component.txLoading()).toBeFalse();
  });

  it('should enable user', () => {
    adminApiSpy.enableUser.and.returnValue(of(void 0));
    component.enableUser();
    expect(adminApiSpy.enableUser).toHaveBeenCalledWith('u1');
    expect(messageAddSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'success' })
    );
  });

  it('should assign a role', () => {
    adminApiSpy.assignRole.and.returnValue(of(void 0));
    component.selectedRoleId = 'viewer';
    component.assignRole();
    expect(adminApiSpy.assignRole).toHaveBeenCalledWith('u1', 'viewer');
    expect(component.showRoleDialog).toBeFalse();
  });

  it('should not assign role when none selected', () => {
    component.selectedRoleId = null;
    component.assignRole();
    expect(adminApiSpy.assignRole).not.toHaveBeenCalled();
  });

  it('should remove a role', () => {
    adminApiSpy.removeRole.and.returnValue(of(void 0));
    component.removeRole('admin');
    expect(adminApiSpy.removeRole).toHaveBeenCalledWith('u1', 'admin');
  });

  it('should grant credits', () => {
    adminApiSpy.grantCredits.and.returnValue(of(void 0));
    component.creditAmount = 25;
    component.creditDescription = 'bonus';
    component.grantCredits();
    expect(adminApiSpy.grantCredits).toHaveBeenCalledWith('u1', { credits: 25, description: 'bonus' });
    expect(component.showCreditDialog).toBeFalse();
  });

  it('should revoke credits', () => {
    adminApiSpy.revokeCredits.and.returnValue(of(void 0));
    component.creditAmount = 5;
    component.revokeCredits();
    expect(adminApiSpy.revokeCredits).toHaveBeenCalledWith('u1', { credits: 5, description: undefined });
  });

  it('should return correct status severity', () => {
    expect(component.statusSeverity('ACTIVE')).toBe('success');
    expect(component.statusSeverity('DISABLED')).toBe('danger');
    expect(component.statusSeverity('INVITED')).toBe('info');
  });

  it('should handle enable error', () => {
    adminApiSpy.enableUser.and.returnValue(throwError(() => new Error('fail')));
    component.enableUser();
    expect(messageAddSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
  });
});
