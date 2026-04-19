import { Routes } from '@angular/router';

export const gstr1LateFeeRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./upload/gstr1-upload.component').then(m => m.Gstr1UploadComponent)
  },
  {
    path: 'results',
    loadComponent: () =>
      import('./results/gstr1-results.component').then(m => m.Gstr1ResultsComponent)
  }
];
