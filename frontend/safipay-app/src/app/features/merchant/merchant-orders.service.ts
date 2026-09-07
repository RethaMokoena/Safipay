import {
  inject,
  Injectable
} from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import {
  ApiResponse,
  MerchantOrder,
  MerchantOrderStatus,
  MerchantSummary
} from './merchant-orders.models';

@Injectable({
  providedIn: 'root'
})
export class MerchantOrdersService {

  private readonly http =
    inject(HttpClient);

  private readonly api =
    'http://localhost:8080/api/merchants';


  getMyMerchants():
    Observable<ApiResponse<MerchantSummary[]>> {

    return this.http.get<
      ApiResponse<MerchantSummary[]>
    >(
      `${this.api}/my`
    );
  }


  getOrders(
    merchantId: string
  ): Observable<ApiResponse<MerchantOrder[]>> {

    return this.http.get<
      ApiResponse<MerchantOrder[]>
    >(
      `${this.api}/${merchantId}/orders`
    );
  }


  updateStatus(
    merchantId: string,
    orderId: string,
    status: MerchantOrderStatus
  ): Observable<ApiResponse<MerchantOrder>> {

    return this.http.put<
      ApiResponse<MerchantOrder>
    >(
      `${this.api}/${merchantId}/orders/${orderId}/status`,
      {
        status
      }
    );
  }
}