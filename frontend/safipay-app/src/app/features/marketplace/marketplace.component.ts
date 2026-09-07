import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  BusinessCategory,
  MarketplaceMerchant
} from './marketplace.models';
import { MarketplaceService } from './marketplace.service';

@Component({
  selector: 'app-marketplace',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './marketplace.component.html',
  styleUrl: './marketplace.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarketplaceComponent {
  private readonly marketplaceService =
    inject(MarketplaceService);

  readonly merchants =
    signal<MarketplaceMerchant[]>([]);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly searchQuery = signal('');
  readonly selectedCategory =
    signal<BusinessCategory | ''>('');

  readonly categories: {
    value: BusinessCategory;
    label: string;
  }[] = [
    { value: 'RETAIL', label: 'Retail' },
    { value: 'FOOD_BEVERAGE', label: 'Food & Beverage' },
    { value: 'HEALTH_BEAUTY', label: 'Health & Beauty' },
    { value: 'TRANSPORT', label: 'Transport' },
    { value: 'EDUCATION', label: 'Education' },
    { value: 'ENTERTAINMENT', label: 'Entertainment' },
    { value: 'SERVICES', label: 'Services' },
    { value: 'UTILITIES', label: 'Utilities' },
    { value: 'OTHER', label: 'Other' }
  ];

  readonly filteredMerchants = computed(() => {
    const query = this.searchQuery()
      .trim()
      .toLowerCase();

    const category = this.selectedCategory();

    return this.merchants().filter(merchant => {
      if (
        category &&
        merchant.category !== category
      ) {
        return false;
      }

      if (!query) {
        return true;
      }

      const searchable = [
        merchant.businessName,
        merchant.description ?? '',
        this.formatCategory(merchant.category)
      ]
        .join(' ')
        .toLowerCase();

      return searchable.includes(query);
    });
  });

  readonly resultLabel = computed(() => {
    const count = this.filteredMerchants().length;

    return count === 1
      ? '1 merchant'
      : `${count} merchants`;
  });

  constructor() {
    this.loadMerchants();
  }

  loadMerchants(): void {
    this.loading.set(true);
    this.error.set(null);

    this.marketplaceService
      .getMerchants()
      .subscribe({
        next: response => {
          const merchants = Array.isArray(response.data)
            ? response.data.filter(
                merchant => merchant.status === 'ACTIVE'
              )
            : [];

          this.merchants.set(merchants);
          this.loading.set(false);
        },
        error: error => {
          console.error(
            'Failed to load marketplace merchants',
            error
          );

          this.error.set(
            'Could not load marketplace merchants right now.'
          );

          this.loading.set(false);
        }
      });
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.selectedCategory.set('');
  }

  setCategory(value: string): void {
    this.selectedCategory.set(
      value as BusinessCategory | ''
    );
  }

  formatCategory(
    category: BusinessCategory
  ): string {
    return category
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(
        /\b\w/g,
        character => character.toUpperCase()
      );
  }

  merchantInitials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  }

  storeButtonLabel(
    merchant: MarketplaceMerchant
  ): string {
    if (
      merchant.category === 'SERVICES' ||
      merchant.category === 'TRANSPORT' ||
      merchant.category === 'EDUCATION' ||
      merchant.category === 'UTILITIES'
    ) {
      return 'View services';
    }

    return 'View store';
  }

  trackByMerchantId(
    _index: number,
    merchant: MarketplaceMerchant
  ): string {
    return merchant.id;
  }
}
