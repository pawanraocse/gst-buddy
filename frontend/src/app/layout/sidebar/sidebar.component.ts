import { Component, Input, Output, EventEmitter, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { CreditApiService } from '../../core/services/credit-api.service';
import { TooltipModule } from 'primeng/tooltip';
import { RippleModule } from 'primeng/ripple';
import { ChipModule } from 'primeng/chip';
import { DividerModule } from 'primeng/divider';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TooltipModule, RippleModule, ChipModule, DividerModule, BadgeModule, ButtonModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Input() collapsed = false;
  @Output() toggle = new EventEmitter<void>();

  authService = inject(AuthService);

  navItems = computed(() => {
    const items = [
      { label: 'Dashboard', icon: 'pi pi-home', link: '/app/dashboard' },
      { label: 'Audit History', icon: 'pi pi-history', link: '/app/audit/history' },
      { label: 'Reports', icon: 'pi pi-chart-bar', link: '/app/referral', badge: 'NEW' },
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
