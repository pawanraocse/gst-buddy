import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ButtonModule } from 'primeng/button';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TooltipModule } from 'primeng/tooltip';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule, ButtonModule, BreadcrumbModule, TooltipModule],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent {
  authService = inject(AuthService);
  
  @Output() toggleMobileSidebar = new EventEmitter<void>();
  
  isDark = signal(false);

  breadcrumbItems: MenuItem[] = [
    { label: 'App', routerLink: '/app' },
    { label: 'Dashboard' }
  ];

  constructor() {
    // Check initial theme
    this.isDark.set(document.documentElement.classList.contains('dark-theme-manual') || 
                  window.matchMedia('(prefers-color-scheme: dark)').matches);
  }

  toggleTheme() {
    this.isDark.update(v => {
      const newVal = !v;
      if (newVal) {
        document.documentElement.classList.add('dark-theme-manual');
        document.documentElement.classList.remove('light-theme-manual');
      } else {
        document.documentElement.classList.add('light-theme-manual');
        document.documentElement.classList.remove('dark-theme-manual');
      }
      return newVal;
    });
  }

  onMobileMenuClick() {
    this.toggleMobileSidebar.emit();
  }
}
