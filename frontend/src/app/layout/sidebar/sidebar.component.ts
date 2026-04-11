import { Component, Input, Output, EventEmitter, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { TooltipModule } from 'primeng/tooltip';
import { RippleModule } from 'primeng/ripple';
import { DividerModule } from 'primeng/divider';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TooltipModule, RippleModule, DividerModule, BadgeModule, ButtonModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Input() collapsed = false;
  @Output() toggle = new EventEmitter<void>();

  authService = inject(AuthService);

  navItems = computed(() => {
    interface NavItem {
      label: string;
      icon: string;
      link: string;
      badge?: string;
    }

    const items: NavItem[] = [
      { label: 'Dashboard', icon: 'pi pi-home', link: '/app/dashboard' },
      { label: 'Audit History', icon: 'pi pi-history', link: '/app/audit/history' },
      { label: 'Refer & Earn', icon: 'pi pi-users', link: '/app/referral' },
      { label: 'Settings', icon: 'pi pi-cog', link: '/app/settings/account' },
    ];

    if (this.authService.isSuperAdmin()) {
      items.push({ label: 'Admin', icon: 'pi pi-lock', link: '/app/admin/dashboard' });
    }
    return items;
  });

  onToggle() {
    this.toggle.emit();
  }
}
