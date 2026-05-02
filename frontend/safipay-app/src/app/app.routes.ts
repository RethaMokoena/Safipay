import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing.component').then(m => m.LandingComponent),
  },
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
      { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard-layout.component').then(m => m.DashboardLayoutComponent),
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'wallet',   loadComponent: () => import('./features/wallet/wallet.component').then(m => m.WalletComponent) },
      { path: 'payments', loadComponent: () => import('./features/payments/payments.component').then(m => m.PaymentsComponent) },
      { path: 'stokvel',  loadComponent: () => import('./features/stokvel/stokvel.component').then(m => m.StokvelComponent) },
      { path: 'merchant', loadComponent: () => import('./features/merchant/merchant.component').then(m => m.MerchantComponent) },
    ],
  },
  { path: 'wallet',   redirectTo: 'dashboard/wallet',   pathMatch: 'full' },
  { path: 'payments', redirectTo: 'dashboard/payments', pathMatch: 'full' },
  { path: 'stokvel',  redirectTo: 'dashboard/stokvel',  pathMatch: 'full' },
  { path: 'merchant', redirectTo: 'dashboard/merchant', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
