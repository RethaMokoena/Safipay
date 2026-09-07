import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';

import {
  ApiResponse,
  CreateListingRequest,
  MerchantListing,
  MerchantSummary,
  UpdateListingRequest
} from './merchant-listings.models';

@Injectable({
  providedIn: 'root'
})
export class MerchantListingsService {
  private readonly http = inject(HttpClient);

  private readonly api =
    `${environment.apiUrl}/api/merchants`;

  getMyMerchants():
    Observable<ApiResponse<MerchantSummary[]>> {

    return this.http.get<
      ApiResponse<MerchantSummary[]>
    >(
      `${this.api}/my`
    );
  }

  getManagedListings(
    merchantId: string
  ): Observable<ApiResponse<MerchantListing[]>> {

    return this.http.get<
      ApiResponse<MerchantListing[]>
    >(
      `${this.api}/${merchantId}/listings/manage`
    );
  }

  createListing(
    merchantId: string,
    request: CreateListingRequest
  ): Observable<ApiResponse<MerchantListing>> {

    return this.http.post<
      ApiResponse<MerchantListing>
    >(
      `${this.api}/${merchantId}/listings`,
      request
    );
  }

  updateListing(
    merchantId: string,
    listingId: string,
    request: UpdateListingRequest
  ): Observable<ApiResponse<MerchantListing>> {

    return this.http.put<
      ApiResponse<MerchantListing>
    >(
      `${this.api}/${merchantId}/listings/${listingId}`,
      request
    );
  }

  deactivateListing(
    merchantId: string,
    listingId: string
  ): Observable<ApiResponse<MerchantListing>> {

    return this.http.delete<
      ApiResponse<MerchantListing>
    >(
      `${this.api}/${merchantId}/listings/${listingId}`
    );
  }
}
