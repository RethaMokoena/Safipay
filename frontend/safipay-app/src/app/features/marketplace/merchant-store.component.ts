import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';
import { forkJoin } from 'rxjs';

import {
  MarketplaceListing,
  MarketplaceMerchant
} from './marketplace.models';
import { MarketplaceService } from './marketplace.service';
import { CartService } from '../cart/cart.service';

@Component({
  selector: 'app-merchant-store',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './merchant-store.component.html',
  styleUrl: './merchant-store.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MerchantStoreComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly cartService = inject(CartService);

  private readonly marketplaceService =
    inject(MarketplaceService);

  readonly merchant =
    signal<MarketplaceMerchant | null>(null);

  readonly listings =
    signal<MarketplaceListing[]>([]);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly merchantId =
    this.route.snapshot.paramMap.get('merchantId');

  readonly productCount = computed(() =>
    this.listings().filter(
      listing => listing.type === 'PRODUCT'
    ).length
  );

  readonly serviceCount = computed(() =>
    this.listings().filter(
      listing => listing.type === 'SERVICE'
    ).length
  );

  constructor() {
    if (!this.merchantId) {
      this.router.navigate([
        '/dashboard/marketplace'
      ]);

      return;
    }

    this.loadStore();
  }

  loadStore(): void {
    if (!this.merchantId) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      merchants:
        this.marketplaceService.getMerchants(),

      listings:
        this.marketplaceService
          .getMerchantListings(this.merchantId)
    }).subscribe({
      next: result => {
        const merchants =
          Array.isArray(result.merchants.data)
            ? result.merchants.data
            : [];

        const merchant =
          merchants.find(
            item => item.id === this.merchantId
          ) ?? null;

        const listings =
          Array.isArray(result.listings.data)
            ? result.listings.data.filter(
                listing => listing.active
              )
            : [];

        this.merchant.set(merchant);
        this.listings.set(listings);
        this.loading.set(false);
      },
      error: error => {
        console.error(
          'Failed to load merchant store',
          error
        );

        this.error.set(
          'Could not load this merchant right now.'
        );

        this.loading.set(false);
      }
    });
  }

  isAvailable(
    listing: MarketplaceListing
  ): boolean {
    if (!listing.active) {
      return false;
    }

    if (listing.type === 'SERVICE') {
      return true;
    }

    return (listing.stockQuantity ?? 0) > 0;
  }

  merchantInitials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  }

  trackByListingId(
    _index: number,
    listing: MarketplaceListing
  ): string {
    return listing.id;
  }


  addToCart(
  listing: MarketplaceListing
): void {

  const result =
    this.cartService
      .addItem(listing);


  switch (result) {

    case 'ADDED':

      window.alert(
        `${listing.title} added to cart.`
      );

      break;


    case 'OUT_OF_STOCK':

      window.alert(
        'This product is out of stock.'
      );

      break;


    case 'MAX_STOCK':

      window.alert(
        'You already have the maximum available quantity in your cart.'
      );

      break;


    case 'INACTIVE':

      window.alert(
        'This listing is currently unavailable.'
      );

      break;
  }
}
}
