import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse, LoginRequest, LoginResponse } from './auth.models';

const TOKEN_KEY = 'srm_access_token';
const USER_KEY = 'srm_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  login(payload: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>('/api/v1/auth/login', payload).pipe(
      tap((response) => {
        sessionStorage.setItem(TOKEN_KEY, response.data.accessToken);
        sessionStorage.setItem(USER_KEY, JSON.stringify({
          userId: response.data.userId,
          email: response.data.email,
          fullName: response.data.fullName,
          roles: response.data.roles,
        }));
      }),
    );
  }

  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getUser(): { userId: string; email: string; fullName: string; roles: string[] } | null {
    const raw = sessionStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      this.logout();
      return null;
    }
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  }
}
