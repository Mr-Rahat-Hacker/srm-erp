export interface AdminUser {
  userId: string;
  email: string;
  fullName: string;
  accountStatus: 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';
  roles: string[];
}

export interface CreateAdminUser {
  fullName: string;
  email: string;
  password: string;
  roles: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
