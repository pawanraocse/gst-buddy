import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { AdminUsersComponent } from './admin-users.component';
import { AdminApiService, AdminUserSummary } from '../../core/services/admin-api.service';

describe('AdminUsersComponent', () => {
  let component: AdminUsersComponent;
  let fixture: ComponentFixture<AdminUsersComponent>;
  let adminApiSpy: jasmine.SpyObj<AdminApiService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let messageAddSpy: jasmine.Spy;

  const mockUsers: AdminUserSummary[] = [
    {
      userId: 'u1', email: 'alice@test.com', name: 'Alice', status: 'ACTIVE',
      source: 'COGNITO', tenantId: 'default', roles: ['admin'],
      creditRemaining: 50, createdAt: '2026-01-01T00:00:00Z', lastLoginAt: '2026-02-01T00:00:00Z'
    },
    {
      userId: 'u2', email: 'bob@test.com', name: 'Bob', status: 'DISABLED',
      source: 'COGNITO', tenantId: 'default', roles: [],
      creditRemaining: 0, createdAt: '2026-01-15T00:00:00Z', lastLoginAt: ''
    }
  ];

  beforeEach(async () => {
    adminApiSpy = jasmine.createSpyObj('AdminApiService', [
      'getUsers', 'enableUser', 'suspendUser', 'deleteUser'
    ]);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    adminApiSpy.getUsers.and.returnValue(of(mockUsers));

    await TestBed.configureTestingModule({
      imports: [AdminUsersComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: AdminApiService, useValue: adminApiSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsersComponent);
    component = fixture.componentInstance;

    const msgService = fixture.debugElement.injector.get(MessageService);
    messageAddSpy = spyOn(msgService, 'add');

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load users on init', () => {
    expect(adminApiSpy.getUsers).toHaveBeenCalled();
    expect(component.users().length).toBe(2);
    expect(component.loading()).toBeFalse();
  });

  it('should filter users by search query', () => {
    component.searchQuery = 'alice';
    component.applyFilter();
    expect(component.filteredUsers().length).toBe(1);
    expect(component.filteredUsers()[0].email).toBe('alice@test.com');
  });

  it('should return all users when search is empty', () => {
    component.searchQuery = '';
    component.applyFilter();
    expect(component.filteredUsers().length).toBe(2);
  });

  it('should navigate to user detail on row click', () => {
    routerSpy.navigate.and.returnValue(Promise.resolve(true));
    component.viewUser(mockUsers[0]);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/app/admin/users', 'u1']);
  });

  it('should enable a disabled user', () => {
    adminApiSpy.enableUser.and.returnValue(of(void 0));
    component.enableUser(mockUsers[1]);
    expect(adminApiSpy.enableUser).toHaveBeenCalledWith('u2');
  });

  it('should show error toast when enable fails', () => {
    adminApiSpy.enableUser.and.returnValue(throwError(() => new Error('fail')));
    component.enableUser(mockUsers[1]);
    expect(messageAddSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
  });

  it('should return correct status severity', () => {
    expect(component.statusSeverity('ACTIVE')).toBe('success');
    expect(component.statusSeverity('DISABLED')).toBe('danger');
    expect(component.statusSeverity('SUSPENDED')).toBe('danger');
    expect(component.statusSeverity('INVITED')).toBe('info');
    expect(component.statusSeverity('UNKNOWN')).toBe('info');
  });

  it('should load users with status filter', () => {
    component.statusFilter = 'ACTIVE';
    component.loadUsers();
    expect(adminApiSpy.getUsers).toHaveBeenCalledWith({ status: 'ACTIVE' });
  });

  it('should handle load users error', () => {
    adminApiSpy.getUsers.and.returnValue(throwError(() => new Error('fail')));
    component.loadUsers();
    expect(component.loading()).toBeFalse();
    expect(messageAddSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
  });
});
