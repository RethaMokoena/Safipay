import { Routes } from "@angular/router";

import { authGuard, guestGuard } from "./core/guards/auth.guard";

import { AiChatComponent } from "./features/ai/ai-chat.component";

export const routes: Routes = [
  {
    path: "",
    loadComponent: () =>
      import("./features/landing/landing.component").then(
        (m) => m.LandingComponent,
      ),
  },
  {
    path: "auth",
    canActivate: [guestGuard],
    children: [
      {
        path: "login",
        loadComponent: () =>
          import("./features/auth/login/login.component").then(
            (m) => m.LoginComponent,
          ),
      },
      {
        path: "register",
        loadComponent: () =>
          import("./features/auth/register/register.component").then(
            (m) => m.RegisterComponent,
          ),
      },
      {
        path: "",
        redirectTo: "login",
        pathMatch: "full",
      },
    ],
  },
  {
    path: "dashboard",
    canActivate: [authGuard],
    loadComponent: () =>
      import("./features/dashboard/dashboard-layout.component").then(
        (m) => m.DashboardLayoutComponent,
      ),
    children: [
      {
        path: "",
        loadComponent: () =>
          import("./features/dashboard/dashboard.component").then(
            (m) => m.DashboardComponent,
          ),
      },
      {
        path: "wallet",
        loadComponent: () =>
          import("./features/wallet/wallet.component").then(
            (m) => m.WalletComponent,
          ),
      },
      {
        path: "payments",
        loadComponent: () =>
          import("./features/payments/payments.component").then(
            (m) => m.PaymentsComponent,
          ),
      },
      {
        path: "stokvel",
        loadComponent: () =>
          import("./features/stokvel/stokvel.component").then(
            (m) => m.StokvelComponent,
          ),
      },
      {
        path: "merchant",
        loadComponent: () =>
          import("./features/merchant/merchant.component").then(
            (m) => m.MerchantComponent,
          ),
      },
      {
        path: "marketplace",
        loadComponent: () =>
          import("./features/marketplace/marketplace.component").then(
            (m) => m.MarketplaceComponent,
          ),
      },
      {
        path: "marketplace/:merchantId",
        loadComponent: () =>
          import("./features/marketplace/merchant-store.component").then(
            (m) => m.MerchantStoreComponent,
          ),
      },
      {
        path: "cart",
        loadComponent: () =>
          import("./features/cart/cart.component").then((m) => m.CartComponent),
      },
      {
        path: "receipt/:checkoutId",
        loadComponent: () =>
          import("./features/cart/receipt.component").then(
            (m) => m.ReceiptComponent,
          ),
      },
      {
        path: "orders",

        loadComponent: () =>
          import("./features/cart/orders.component").then(
            (m) => m.OrdersComponent,
          ),
      },
      {
  path: 'merchant/orders',
  loadComponent: () =>
    import(
      './features/merchant/merchant-orders.component'
    ).then(
      m => m.MerchantOrdersComponent
    ),
},
// Add BOTH of these inside the existing /dashboard children array.

{
  path: 'merchant/listings',
  loadComponent: () =>
    import(
      './features/merchant/listings/merchant-listings.component'
    ).then(
      m => m.MerchantListingsComponent
    ),
},

{
  path: 'merchant/listings/:merchantId',
  loadComponent: () =>
    import(
      './features/merchant/listings/merchant-listings.component'
    ).then(
      m => m.MerchantListingsComponent
    ),
},

      {
        path: "ai",
        component: AiChatComponent,
      },
    ],
  },
  {
    path: "wallet",
    redirectTo: "dashboard/wallet",
    pathMatch: "full",
  },
  {
    path: "payments",
    redirectTo: "dashboard/payments",
    pathMatch: "full",
  },
  {
    path: "stokvel",
    redirectTo: "dashboard/stokvel",
    pathMatch: "full",
  },
  {
    path: "merchant",
    redirectTo: "dashboard/merchant",
    pathMatch: "full",
  },
  {
    path: "marketplace",
    redirectTo: "dashboard/marketplace",
    pathMatch: "full",
  },
  {
    path: "ai",
    redirectTo: "dashboard/ai",
    pathMatch: "full",
  },
  {
    path: "**",
    redirectTo: "",
  },
];
