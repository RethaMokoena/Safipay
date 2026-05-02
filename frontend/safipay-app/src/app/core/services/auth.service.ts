import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User, ApiResponse } from '../../shared/models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;

  private _currentUser = signal<User | null>(null);
  private _token = signal<string | null>(localStorage.getItem('safipay_token'));

  readonly currentUser = this._currentUser.asReadonly();
  readonly token = this._token.asReadonly();
  readonly isLoggedIn = computed(() => !!this._token());

  constructor(private http: HttpClient, private router: Router) {
    const savedUser = localStorage.getItem('safipay_user');
    if (savedUser) {
      this._currentUser.set(JSON.parse(savedUser));
    }
  }

  register(request: RegisterRequest) {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/register`, request).pipe(
      tap(res => this.handleAuthSuccess(res.data)),
      catchError(err => throwError(() => err.error?.message || 'Registration failed'))
    );
  }

  login(request: LoginRequest) {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, request).pipe(
      tap(res => this.handleAuthSuccess(res.data)),
      catchError(err => throwError(() => err.error?.message || 'Login failed'))
    );
  }

  logout() {
    this._token.set(null);
    this._currentUser.set(null);
    localStorage.removeItem('safipay_token');
    localStorage.removeItem('safipay_user');
    this.router.navigate(['/']);
  }

  getMe() {
    return this.http.get<ApiResponse<User>>(`${this.apiUrl}/me`).pipe(
      tap(res => {
        this._currentUser.set(res.data);
        localStorage.setItem('safipay_user', JSON.stringify(res.data));
      })
    );
  }

  private handleAuthSuccess(data: AuthResponse) {
    this._token.set(data.accessToken);
    this._currentUser.set(data.user);
    localStorage.setItem('safipay_token', data.accessToken);
    localStorage.setItem('safipay_user', JSON.stringify(data.user));
  }
}
