import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ApiResponse,
  BusinessCategory,
  MarketplaceListing,
  MarketplaceMerchant
} from './marketplace.models';

@Injectable({
  providedIn: 'root'
})
export class MarketplaceService {
  private readonly http = inject(HttpClient);

  private readonly merchantApi =
    'http://localhost:8080/api/merchants';

  getMerchants(
    category?: BusinessCategory | ''
  ): Observable<ApiResponse<MarketplaceMerchant[]>> {
    let params = new HttpParams();

    if (category) {
      params = params.set('category', category);
    }

    return this.http.get<ApiResponse<MarketplaceMerchant[]>>(
      `${this.merchantApi}/discover`,
      { params }
    );
  }

  getMerchantListings(
    merchantId: string
  ): Observable<ApiResponse<MarketplaceListing[]>> {
    return this.http.get<ApiResponse<MarketplaceListing[]>>(
      `${this.merchantApi}/${merchantId}/listings`
    );
  }
}