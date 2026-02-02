import { Component, computed, inject, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { MenubarModule } from 'primeng/menubar';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { MenuItem } from 'primeng/api';
import { ShareDialogComponent } from '../shared/components/share-dialog/share-dialog.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, MenubarModule, ButtonModule, AvatarModule, ShareDialogComponent],
  templateUrl: './app-layout.component.html',
  styleUrls: ['./app-layout.component.scss']
})
export class AppLayoutComponent {
  authService = inject(AuthService);
  router = inject(RouterModule); // To track active route if needed, or stick to simple logic

  sidebarActive = signal(false);

  @ViewChild('shareDialog') shareDialog!: ShareDialogComponent;

  openShareDialog() {
    this.shareDialog.show();
  }

  toggleSidebar() {
    this.sidebarActive.update(v => !v);
  }

  // Helper to check active route roughly
  isActive(path: string): boolean {
    return window.location.pathname.includes(path);
  }

  // Close sidebar on menu click (mobile)
  onMenuClick() {
    if (window.innerWidth < 992) {
      this.sidebarActive.set(false);
    }
  }

  items = computed<MenuItem[]>(() => {
    return [
      { label: 'Rule 37', icon: 'pi pi-calculator', routerLink: '/app/dashboard' },
      { label: 'Settings', icon: 'pi pi-cog', routerLink: '/app/settings/account' }
    ];
  });

  logout() {
    this.authService.logout();
  }
}
