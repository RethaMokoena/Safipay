import { Component, inject, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastComponent } from '../../shared/components/toast/toast.component';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, ToastComponent],
  templateUrl: './dashboard-layout.component.html',
  styleUrls: ['./dashboard-layout.component.scss'],
})
export class DashboardLayoutComponent {
  readonly authService = inject(AuthService);
  sidebarOpen = signal(false);

  navItems = [
  { path: '/dashboard',        icon: 'fas fa-home', label: 'Overview' },
{ path: '/dashboard/wallet',   icon: 'fas fa-wallet', label: 'Wallet' },
{ path: '/dashboard/payments', icon: 'fas fa-paper-plane', label: 'Payments' },
{ path: '/dashboard/stokvel',  icon: 'fas fa-handshake', label: 'Stokvels' },
{ path: '/dashboard/merchant', icon: 'fas fa-store', label: 'Merchant' },
  ];

  toggleSidebar() { this.sidebarOpen.update(v => !v); }
  closeSidebar()  { this.sidebarOpen.set(false); }
  logout()        { this.authService.logout(); }

  @HostListener('window:resize')
  onResize() { if (window.innerWidth > 900) this.sidebarOpen.set(false); }
}
