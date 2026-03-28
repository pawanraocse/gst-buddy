import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const SUPPORT_BASE = `${environment.apiUrl}/auth/api/v1/support`;

export interface TicketReplyDto {
  id: string;
  message: string;
  userId: string;
  isAdminReply: boolean;
  createdAt: string;
}

export interface SupportTicketDto {
  id: string;
  subject: string;
  category: string;
  description: string;
  status: string;
  userId?: string;
  email?: string;
  isEnrolledUser: boolean;
  createdAt: string;
  updatedAt: string;
  replies: TicketReplyDto[];
}

export interface CreateSupportTicketRequest {
  subject: string;
  category: string;
  description: string;
  email?: string;
}

export interface AddTicketReplyRequest {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class SupportApiService {
  private readonly http = inject(HttpClient);

  createPublicTicket(request: CreateSupportTicketRequest): Observable<SupportTicketDto> {
    return this.http.post<SupportTicketDto>(`${SUPPORT_BASE}/public/tickets`, request);
  }

  createTicket(request: CreateSupportTicketRequest): Observable<SupportTicketDto> {
    return this.http.post<SupportTicketDto>(`${SUPPORT_BASE}/tickets`, request);
  }

  getMyTickets(): Observable<SupportTicketDto[]> {
    return this.http.get<SupportTicketDto[]>(`${SUPPORT_BASE}/tickets/me`);
  }

  addReply(ticketId: string, request: AddTicketReplyRequest): Observable<SupportTicketDto> {
    return this.http.post<SupportTicketDto>(`${SUPPORT_BASE}/tickets/${ticketId}/reply`, request);
  }

  /**
   * Admin: list all tickets with optional filters.
   * @param isEnrolled null = all, true = registered users, false = guest/anonymous
   */
  getAdminTickets(
    status?: string,
    email?: string,
    category?: string,
    isEnrolled?: boolean | null,
    page = 0,
    size = 10
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (status) params = params.set('status', status);
    if (email) params = params.set('email', email);
    if (category) params = params.set('category', category);
    if (isEnrolled !== null && isEnrolled !== undefined) {
      params = params.set('isEnrolled', String(isEnrolled));
    }
    return this.http.get<any>(`${SUPPORT_BASE}/admin/tickets`, { params });
  }

  addAdminReply(ticketId: string, request: AddTicketReplyRequest): Observable<SupportTicketDto> {
    return this.http.post<SupportTicketDto>(`${SUPPORT_BASE}/admin/tickets/${ticketId}/reply`, request);
  }

  updateTicketStatus(ticketId: string, status: string): Observable<SupportTicketDto> {
    return this.http.put<SupportTicketDto>(`${SUPPORT_BASE}/admin/tickets/${ticketId}/status`, { status });
  }
}

