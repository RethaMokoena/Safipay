import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/wallet.models';
import {
  Stokvel, StokvelContribution, StokvelPayout,
  CreateStokvelRequest, ContributeRequest
} from '../../shared/models/app.models';

@Injectable({ providedIn: 'root' })
export class StokvelService {
  private readonly apiUrl = `${environment.apiUrl}/api/stokvels`;

  constructor(private http: HttpClient) {}

  create(request: CreateStokvelRequest) {
    return this.http.post<ApiResponse<Stokvel>>(this.apiUrl, request);
  }

  getAll() {
    return this.http.get<ApiResponse<Stokvel[]>>(this.apiUrl);
  }

  getMine() {
    return this.http.get<ApiResponse<Stokvel[]>>(`${this.apiUrl}/my`);
  }

  getById(id: number) {
    return this.http.get<ApiResponse<Stokvel>>(`${this.apiUrl}/${id}`);
  }

  join(id: number) {
    return this.http.post<ApiResponse<Stokvel>>(`${this.apiUrl}/${id}/join`, {});
  }

  activate(id: number) {
    return this.http.post<ApiResponse<Stokvel>>(`${this.apiUrl}/${id}/activate`, {});
  }

  contribute(id: number, request: ContributeRequest) {
    return this.http.post<ApiResponse<StokvelContribution>>(`${this.apiUrl}/${id}/contribute`, request);
  }

  triggerRoscaPayout(id: number) {
    return this.http.post<ApiResponse<StokvelPayout>>(`${this.apiUrl}/${id}/payouts/rosca`, {});
  }

  getContributions(id: number) {
    return this.http.get<ApiResponse<StokvelContribution[]>>(`${this.apiUrl}/${id}/contributions`);
  }

  getPayouts(id: number) {
    return this.http.get<ApiResponse<StokvelPayout[]>>(`${this.apiUrl}/${id}/payouts`);
  }
}
