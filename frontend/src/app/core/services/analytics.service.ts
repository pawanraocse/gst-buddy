import { Injectable, PLATFORM_ID, inject, isDevMode } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly GTM_ID = ''; // Leave empty for now, loaded via env or config later
  private platformId = inject(PLATFORM_ID);

  /**
   * Initializes Google Tag Manager.
   * Ensures it only runs on the client-side (browser) to prevent SSR hydration errors.
   */
  initGTM(): void {
    if (isPlatformBrowser(this.platformId) && this.GTM_ID && !isDevMode()) {
      this.loadGtmScript();
    }
  }

  private loadGtmScript(): void {
    // 1. DataLayer initialization
    const win = window as any;
    win.dataLayer = win.dataLayer || [];
    win.dataLayer.push({
      'gtm.start': new Date().getTime(),
      event: 'gtm.js'
    });

    // 2. Load the GTM Script dynamically
    const script = document.createElement('script');
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtm.js?id=${this.GTM_ID}`;
    document.head.appendChild(script);

    // 3. Fallback noscript iframe
    const noscript = document.createElement('noscript');
    const iframe = document.createElement('iframe');
    iframe.src = `https://www.googletagmanager.com/ns.html?id=${this.GTM_ID}`;
    iframe.height = '0';
    iframe.width = '0';
    iframe.style.display = 'none';
    iframe.style.visibility = 'hidden';
    noscript.appendChild(iframe);
    document.body.prepend(noscript);
  }
}
