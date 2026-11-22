import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const approvalPolicyGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = auth.getUser()?.roles ?? [];
  return roles.some((role) => ['SRM_ADMIN', 'SUPER_ADMIN'].includes(role))
    ? true
    : router.createUrlTree(['/dashboard']);
};
