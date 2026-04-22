import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { Router, NavigationEnd, ActivatedRoute, Data } from '@angular/router';
import { filter, map, mergeMap } from 'rxjs/operators';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class SeoService {
  private title = inject(Title);
  private meta = inject(Meta);
  private router = inject(Router);
  private activatedRoute = inject(ActivatedRoute);
  private document = inject(DOCUMENT);
  private platformId = inject(PLATFORM_ID);

  /**
   * Initialize Global SEO listener.
   * Call this once in the root app component.
   */
  initGlobalListener() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => this.activatedRoute),
      map((route: ActivatedRoute) => {
        while (route.firstChild) {
          route = route.firstChild;
        }
        return route;
      }),
      filter((route: ActivatedRoute) => route.outlet === 'primary'),
      mergeMap((route: ActivatedRoute) => route.data)
    ).subscribe((data: Data) => {
      const seoData = data as { title?: string; description?: string; image?: string; url?: string };
      if (seoData.title) {
        this.updateMeta({
          title: seoData.title,
          description: seoData.description || 'Automated GST Audit and Rule 37 ITC Reversal calculator for Indian businesses.',
          image: seoData.image || 'https://gstbuddies.com/assets/seo/og-preview.png',
          url: seoData.url || `https://gstbuddies.com${this.router.url}`
        });
      }
    });
  }

  /**
   * Updates the page title and associated meta tags for SEO.
   */
  updateMeta(config: {
    title: string;
    description: string;
    image?: string;
    url?: string;
  }) {
    // Determine if we need to append the brand wrapper
    const fullTitle = config.title.includes('GSTBuddies') ? config.title : `${config.title} | GSTBuddies`;
    this.title.setTitle(fullTitle);

    // Standard Meta
    this.meta.updateTag({ name: 'description', content: config.description });

    // Open Graph
    this.meta.updateTag({ property: 'og:title', content: fullTitle });
    this.meta.updateTag({ property: 'og:description', content: config.description });
    if (config.image) {
      this.meta.updateTag({ property: 'og:image', content: config.image });
    }
    if (config.url) {
      this.meta.updateTag({ property: 'og:url', content: config.url });
    }

    // Twitter
    this.meta.updateTag({ name: 'twitter:title', content: fullTitle });
    this.meta.updateTag({ name: 'twitter:description', content: config.description });
    if (config.image) {
      this.meta.updateTag({ name: 'twitter:image', content: config.image });
    }
  }

  /**
   * Inject structured data (JSON-LD) into the page.
   */
  setSchema(schema: any) {
    if (isPlatformBrowser(this.platformId)) {
      let script = this.document.getElementById('seo-schema') as HTMLScriptElement;
      if (!script) {
        script = this.document.createElement('script');
        script.setAttribute('id', 'seo-schema');
        script.setAttribute('type', 'application/ld+json');
        this.document.head.appendChild(script);
      }
      script.textContent = JSON.stringify(schema);
    }
  }
}
