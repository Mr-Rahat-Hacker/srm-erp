import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminUser, ApiResponse, CreateAdminUser } from './admin-user.models';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/v1/admin/users';

  list(): Observable<ApiResponse<AdminUser[]>> {
    return this.http.get<ApiResponse<AdminUser[]>>(this.endpoint);
  }

  create(payload: CreateAdminUser): Observable<ApiResponse<AdminUser>> {
    return this.http.post<ApiResponse<AdminUser>>(this.endpoint, payload);
  }

  updateStatus(userId: string, accountStatus: AdminUser['accountStatus']): Observable<ApiResponse<AdminUser>> {
    return this.http.patch<ApiResponse<AdminUser>>(
      `${this.endpoint}/${userId}/status`, { accountStatus },
    );
  }
}
