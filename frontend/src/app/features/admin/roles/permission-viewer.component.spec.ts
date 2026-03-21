import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PermissionViewerComponent } from './permission-viewer.component';
import { RoleService } from '../../../core/services/role.service';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideZonelessChangeDetection } from '@angular/core';

describe('PermissionViewerComponent', () => {
    let component: PermissionViewerComponent;
    let fixture: ComponentFixture<PermissionViewerComponent>;

    beforeEach(async () => {
        const rSpy = jasmine.createSpyObj('RoleService', ['getPermissions']);
        rSpy.getPermissions.and.returnValue(of([]));

        await TestBed.configureTestingModule({
            imports: [PermissionViewerComponent],
            providers: [
                provideZonelessChangeDetection(),
                provideHttpClient(),
                provideHttpClientTesting(),
                provideAnimations(),
                { provide: RoleService, useValue: rSpy },
                { provide: DynamicDialogConfig, useValue: { data: { roleId: 'admin' } } },
                { provide: DynamicDialogRef, useValue: { close: jasmine.createSpy('close') } }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(PermissionViewerComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should map roleId from config to the correct capabilities on init', () => {
        // Component resolves capabilities via internal switch — no RoleService call
        expect(component.roleCapabilities.name).toBe('ADMIN');
        expect(component.roleCapabilities.color).toBe('danger');
        expect(component.roleCapabilities.capabilities.length).toBeGreaterThan(0);
    });

    it('getCapabilitiesForRole — should return correct role for viewer', () => {
        const caps = component.getCapabilitiesForRole('viewer');
        expect(caps.name).toBe('VIEWER');
        expect(caps.color).toBe('info');
    });

    it('getCapabilitiesForRole — should return custom caps for unknown roleId', () => {
        const caps = component.getCapabilitiesForRole('mystery-role');
        expect(caps.name).toBe('MYSTERY-ROLE');
        expect(caps.color).toBe('info');
    });
});
