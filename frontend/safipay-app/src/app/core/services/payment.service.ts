import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/wallet.models';
import { Payment, SendMoneyRequest } from '../../shared/models/app.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly apiUrl = `${environment.apiUrl}/api/payments`;

  constructor(private http: HttpClient) {}

  sendMoney(request: SendMoneyRequest) {
    return this.http.post<ApiResponse<Payment>>(`${this.apiUrl}/send`, request);
  }

  getHistory(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<Payment[]>>(`${this.apiUrl}/history`, { params });
  }
}
