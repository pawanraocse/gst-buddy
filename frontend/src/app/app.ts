import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SeoService } from './core/services/seo.service';
import { AnalyticsService } from './core/services/analytics.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastModule, ConfirmDialogModule],
  template: `
    <p-toast position="top-right"></p-toast>
    <p-confirmDialog></p-confirmDialog>
    <router-outlet />
  `,
  styleUrls: ['./app.scss']
})
export class App implements OnInit {
  private seo = inject(SeoService);
  private analytics = inject(AnalyticsService);

  ngOnInit() {
    this.seo.initGlobalListener();
    this.analytics.initGTM();
  }
}
