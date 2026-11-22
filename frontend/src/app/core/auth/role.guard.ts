import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requiredRoles = route.data['roles'] as string[] | undefined;
  const userRoles = auth.getUser()?.roles ?? [];
  return requiredRoles?.some((role) => userRoles.includes(role))
    ? true
    : router.createUrlTree(['/dashboard']);
};
