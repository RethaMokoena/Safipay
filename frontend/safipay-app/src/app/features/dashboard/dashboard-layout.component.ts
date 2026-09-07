import {
  Component,
  HostListener,
  inject,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

import {
  ToastComponent
} from '../../shared/components/toast/toast.component';


interface DashboardNavItem {
  path: string;
  icon: string;
  label: string;
  exact?: boolean;
}


interface DashboardNavSection {
  label: string;
  items: DashboardNavItem[];
}


@Component({
  selector: 'app-dashboard-layout',
  standalone: true,

  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    ToastComponent
  ],

  templateUrl:
    './dashboard-layout.component.html',

  styleUrls: [
    './dashboard-layout.component.scss'
  ],
})
export class DashboardLayoutComponent {

  readonly authService =
    inject(AuthService);

  readonly sidebarOpen =
    signal(false);


  readonly navSections:
    DashboardNavSection[] = [

    {
      label: 'MONEY',

      items: [
        {
          path: '/dashboard',
          icon: 'fas fa-house',
          label: 'Overview',
          exact: true
        },
        {
          path: '/dashboard/wallet',
          icon: 'fas fa-wallet',
          label: 'Wallet'
        },
        {
          path: '/dashboard/payments',
          icon: 'fas fa-paper-plane',
          label: 'Payments'
        },
        {
          path: '/dashboard/stokvel',
          icon: 'fas fa-handshake',
          label: 'Stokvels'
        }
      ]
    },

    {
      label: 'COMMERCE',

      items: [
        {
          path: '/dashboard/marketplace',
          icon: 'fas fa-store',
          label: 'Marketplace'
        },
        {
          path: '/dashboard/orders',
          icon: 'fas fa-box',
          label: 'My Orders'
        },
        {
          path: '/dashboard/cart',
          icon: 'fas fa-cart-shopping',
          label: 'Cart'
        },
        {
          path: '/dashboard/merchant',
          icon: 'fas fa-briefcase',
          label: 'Merchant'
        }
      ]
    },

    {
      label: 'ASSISTANT',

      items: [
        {
          path: '/dashboard/ai',
          icon: 'fas fa-sparkles',
          label: 'Ask Safi'
        }
      ]
    }
  ];


  toggleSidebar(): void {

    this.sidebarOpen.update(
      open => !open
    );
  }


  closeSidebar(): void {

    this.sidebarOpen.set(
      false
    );
  }


  logout(): void {

    this.authService.logout();
  }


  @HostListener('window:resize')
  onResize(): void {

    if (
      window.innerWidth > 900
    ) {

      this.sidebarOpen.set(
        false
      );
    }
  }
}
