import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const superAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.getUser()?.roles.includes('SUPER_ADMIN')
    ? true
    : router.createUrlTree(['/dashboard']);
};
