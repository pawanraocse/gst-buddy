import { Component, inject, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SidebarComponent } from './sidebar/sidebar.component';
import { TopbarComponent } from './topbar/topbar.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent, TopbarComponent],
  templateUrl: './app-layout.component.html',
  styleUrls: ['./app-layout.component.scss']
})
export class AppLayoutComponent {
  sidebarCollapsed = signal(false);
  mobileSidebarActive = signal(false);

  constructor() {
    const savedState = localStorage.getItem('sidebar_collapsed');
    if (savedState) {
      this.sidebarCollapsed.set(savedState === 'true');
    }
  }

  toggleSidebar() {
    this.sidebarCollapsed.update(v => {
      const newState = !v;
      localStorage.setItem('sidebar_collapsed', String(newState));
      return newState;
    });
  }

  toggleMobileSidebar() {
    this.mobileSidebarActive.update(v => !v);
  }

  closeMobileSidebar() {
    this.mobileSidebarActive.set(false);
  }

  @HostListener('window:resize')
  onResize() {
    if (window.innerWidth >= 992) {
      this.mobileSidebarActive.set(false);
    }
  }
}
