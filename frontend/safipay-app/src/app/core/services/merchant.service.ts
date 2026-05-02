import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/wallet.models';
import { Merchant, MerchantApiKey, MerchantPayment, RegisterMerchantRequest } from '../../shared/models/app.models';

@Injectable({ providedIn: 'root' })
export class MerchantService {
  private readonly apiUrl = `${environment.apiUrl}/api/merchants`;

  constructor(private http: HttpClient) {}

  register(request: RegisterMerchantRequest) {
    return this.http.post<ApiResponse<Merchant>>(this.apiUrl, request);
  }

  getMine() {
    return this.http.get<ApiResponse<Merchant[]>>(`${this.apiUrl}/my`);
  }

  getById(id: string) {
    return this.http.get<ApiResponse<Merchant>>(`${this.apiUrl}/${id}`);
  }

  generateApiKey(merchantId: string, label: string, environment: 'TEST' | 'LIVE') {
    return this.http.post<ApiResponse<MerchantApiKey>>(`${this.apiUrl}/${merchantId}/api-keys`, { label, environment });
  }

  listApiKeys(merchantId: string) {
    return this.http.get<ApiResponse<MerchantApiKey[]>>(`${this.apiUrl}/${merchantId}/api-keys`);
  }

  revokeApiKey(merchantId: string, keyId: string) {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${merchantId}/api-keys/${keyId}`);
  }

  getPayments(merchantId: string, page = 0, size = 20) {
    return this.http.get<ApiResponse<MerchantPayment[]>>(
      `${this.apiUrl}/${merchantId}/payments?page=${page}&size=${size}`
    );
  }

  refundPayment(merchantId: string, paymentId: string) {
    return this.http.post<ApiResponse<MerchantPayment>>(
      `${this.apiUrl}/${merchantId}/payments/${paymentId}/refund`, {}
    );
  }
}
