import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { CreditApiService, WalletDto } from '../../core/services/credit-api.service';

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, CardModule, ButtonModule, DialogModule, InputTextModule, ToastModule, ToggleSwitchModule],
  providers: [MessageService],
  templateUrl: './account-settings.component.html',
  styleUrls: ['./account-settings.component.scss']
})
export class AccountSettingsComponent implements OnInit {
  authService = inject(AuthService);
  private http = inject(HttpClient);
  private router = inject(Router);
  private messageService = inject(MessageService);
  private creditApi = inject(CreditApiService);

  deleting = signal(false);
  showDeleteDialog = false;
  confirmationText = '';
  wallet = signal<WalletDto | null>(null);
  walletLoading = signal(true);

  // Mock preferences for UI
  preferences = {
    email_notifications: true,
    beta_features: false
  };

  ngOnInit(): void {
    this.creditApi.getWallet().subscribe({
      next: (w) => { this.wallet.set(w); this.walletLoading.set(false); },
      error: () => { this.walletLoading.set(false); }
    });
  }

  deleteAccount(): void {
    if (this.confirmationText !== 'DELETE') return;

    this.deleting.set(true);
    this.http.post(`${environment.apiUrl}/auth/api/v1/account/delete`, {
      confirmation: this.confirmationText
    }).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Account Deleted', detail: 'Redirecting...' });
        setTimeout(() => this.authService.logout(), 2000);
      },
      error: () => {
        this.deleting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete account.' });
      }
    });
  }
}
