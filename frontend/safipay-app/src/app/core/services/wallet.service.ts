import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ApiResponse, Wallet, Transaction, TransferRequest, TopUpRequest } from '../../shared/models/wallet.models';

@Injectable({ providedIn: 'root' })
export class WalletService {
  private readonly apiUrl = `${environment.apiUrl}/api/wallets`;

  constructor(private http: HttpClient) {}

  createWallet() {
    return this.http.post<ApiResponse<Wallet>>(this.apiUrl, {});
  }

  getMyWallet() {
    return this.http.get<ApiResponse<Wallet>>(`${this.apiUrl}/me`);
  }

  topUp(request: TopUpRequest) {
    return this.http.post<ApiResponse<Wallet>>(`${this.apiUrl}/top-up`, request);
  }

  transfer(request: TransferRequest) {
    return this.http.post<ApiResponse<Wallet>>(`${this.apiUrl}/transfer`, request);
  }

  getTransactions(page = 0, size = 20) {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<Transaction[]>>(`${this.apiUrl}/transactions`, { params });
  }
}
