import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiResponse } from '../marketplace/marketplace.models';

import {
  CheckoutRequest,
  CheckoutResponse
} from './checkout.models';

@Injectable({
  providedIn: 'root'
})
export class CheckoutService {

  private readonly http =
    inject(HttpClient);

  private readonly api =
    'http://localhost:8080/api/merchants/checkout';


  createCheckout(
    request: CheckoutRequest
  ): Observable<ApiResponse<CheckoutResponse>> {

    return this.http.post<
      ApiResponse<CheckoutResponse>
    >(
      this.api,
      request
    );
  }


  payCheckout(
    checkoutId: string
  ): Observable<ApiResponse<CheckoutResponse>> {

    return this.http.post<
      ApiResponse<CheckoutResponse>
    >(
      `${this.api}/${checkoutId}/pay`,
      {}
    );
  }


  retryMerchantPayment(
    checkoutId: string,
    orderId: string
  ): Observable<ApiResponse<CheckoutResponse>> {

    return this.http.post<
      ApiResponse<CheckoutResponse>
    >(
      `${this.api}/${checkoutId}/orders/${orderId}/retry-payment`,
      {}
    );
  }


  getCheckout(
    checkoutId: string
  ): Observable<ApiResponse<CheckoutResponse>> {

    return this.http.get<
      ApiResponse<CheckoutResponse>
    >(
      `${this.api}/${checkoutId}`
    );
  }

  getMyCheckouts():
  Observable<ApiResponse<CheckoutResponse[]>> {

  return this.http.get<
    ApiResponse<CheckoutResponse[]>
  >(
    'http://localhost:8080/api/merchants/checkouts/my'
  );
}
}