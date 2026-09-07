import {
  Component,
  computed,
  inject,
  signal
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  ActivatedRoute
} from '@angular/router';

import {
  CreateListingRequest,
  ListingType,
  MerchantListing,
  MerchantSummary,
  UpdateListingRequest
} from './merchant-listings.models';

import {
  MerchantListingsService
} from './merchant-listings.service';

type ListingFilter =
  | 'ALL'
  | 'ACTIVE'
  | 'INACTIVE'
  | 'PRODUCT'
  | 'SERVICE';

@Component({
  selector: 'app-merchant-listings',
  standalone: true,

  imports: [
    CommonModule,
    ReactiveFormsModule
  ],

  templateUrl:
    './merchant-listings.component.html',

  styleUrls: [
    './merchant-listings.component.scss'
  ]
})
export class MerchantListingsComponent {
  private readonly fb =
    inject(FormBuilder);

  private readonly route =
    inject(ActivatedRoute);

  private readonly listingsService =
    inject(MerchantListingsService);

  readonly merchants =
    signal<MerchantSummary[]>([]);

  readonly selectedMerchantId =
    signal('');

  readonly listings =
    signal<MerchantListing[]>([]);

  readonly loading =
    signal(true);

  readonly saving =
    signal(false);

  readonly error =
    signal<string | null>(null);

  readonly modalOpen =
    signal(false);

  readonly editingListing =
    signal<MerchantListing | null>(null);

  readonly changingListingId =
    signal<string | null>(null);

  readonly filter =
    signal<ListingFilter>('ALL');

  readonly activeCount =
    computed(
      () =>
        this.listings()
          .filter(item => item.active)
          .length
    );

  readonly inactiveCount =
    computed(
      () =>
        this.listings()
          .filter(item => !item.active)
          .length
    );

  readonly productCount =
    computed(
      () =>
        this.listings()
          .filter(item => item.type === 'PRODUCT')
          .length
    );

  readonly serviceCount =
    computed(
      () =>
        this.listings()
          .filter(item => item.type === 'SERVICE')
          .length
    );

  readonly visibleListings =
    computed(() => {
      const filter =
        this.filter();

      return this.listings()
        .filter(listing => {
          switch (filter) {
            case 'ACTIVE':
              return listing.active;

            case 'INACTIVE':
              return !listing.active;

            case 'PRODUCT':
              return listing.type === 'PRODUCT';

            case 'SERVICE':
              return listing.type === 'SERVICE';

            default:
              return true;
          }
        });
    });

  readonly listingForm =
    this.fb.group({
      title: [
        '',
        [
          Validators.required,
          Validators.minLength(2)
        ]
      ],

      description: [''],

      price: [
        null as number | null,
        [
          Validators.required,
          Validators.min(0.01)
        ]
      ],

      type: [
        'PRODUCT' as ListingType,
        Validators.required
      ],

      imageUrl: [''],

      stockQuantity: [
        0 as number | null,
        [
          Validators.required,
          Validators.min(0)
        ]
      ]
    });

  constructor() {
    this.listingForm
      .get('type')
      ?.valueChanges
      .subscribe(type => {
        this.configureStockValidation(
          type as ListingType
        );
      });

    this.loadMerchants();
  }

  loadMerchants(): void {
    this.loading.set(true);
    this.error.set(null);

    this.listingsService
      .getMyMerchants()
      .subscribe({
        next: response => {
          const merchants =
            response.data ?? [];

          this.merchants.set(
            merchants
          );

          if (merchants.length === 0) {
            this.loading.set(false);
            return;
          }

          const routeMerchantId =
            this.route.snapshot
              .paramMap
              .get('merchantId');

          const routeMerchant =
            routeMerchantId
              ? merchants.find(
                  merchant =>
                    merchant.id === routeMerchantId
                )
              : null;

          const initialMerchant =
            routeMerchant ??
            merchants[0];

          this.selectedMerchantId.set(
            initialMerchant.id
          );

          this.loadListings();
        },

        error: error => {
          console.error(
            'Could not load merchants',
            error
          );

          this.error.set(
            'Could not load your merchant accounts.'
          );

          this.loading.set(false);
        }
      });
  }

  merchantChanged(
    merchantId: string
  ): void {
    this.selectedMerchantId.set(
      merchantId
    );

    this.closeModal();
    this.loadListings();
  }

  loadListings(): void {
    const merchantId =
      this.selectedMerchantId();

    if (!merchantId) {
      this.listings.set([]);
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.listingsService
      .getManagedListings(
        merchantId
      )
      .subscribe({
        next: response => {
          this.listings.set(
            response.data ?? []
          );

          this.loading.set(false);
        },

        error: error => {
          console.error(
            'Could not load listings',
            error
          );

          this.error.set(
            error?.error?.message ||
            'Could not load merchant listings.'
          );

          this.loading.set(false);
        }
      });
  }

  openCreate(): void {
    const merchant =
      this.selectedMerchant();

    if (!merchant) {
      return;
    }

    if (merchant.status !== 'ACTIVE') {
      this.error.set(
        'The merchant account must be ACTIVE before you can publish listings.'
      );
      return;
    }

    this.error.set(null);
    this.editingListing.set(null);

    this.listingForm.reset({
      title: '',
      description: '',
      price: null,
      type: 'PRODUCT',
      imageUrl: '',
      stockQuantity: 0
    });

    this.configureStockValidation(
      'PRODUCT'
    );

    this.modalOpen.set(true);
  }

  openEdit(
    listing: MerchantListing
  ): void {
    this.error.set(null);

    this.editingListing.set(
      listing
    );

    this.listingForm.reset({
      title: listing.title,
      description:
        listing.description ?? '',
      price: listing.price,
      type: listing.type,
      imageUrl:
        listing.imageUrl ?? '',
      stockQuantity:
        listing.type === 'PRODUCT'
          ? listing.stockQuantity ?? 0
          : null
    });

    this.configureStockValidation(
      listing.type
    );

    this.modalOpen.set(true);
  }

  closeModal(): void {
    if (this.saving()) {
      return;
    }

    this.modalOpen.set(false);
    this.editingListing.set(null);
  }

  saveListing(): void {
    if (
      this.listingForm.invalid ||
      this.saving()
    ) {
      this.listingForm.markAllAsTouched();
      return;
    }

    const merchantId =
      this.selectedMerchantId();

    if (!merchantId) {
      return;
    }

    const raw =
      this.listingForm.getRawValue();

    const type =
      raw.type as ListingType;

    const stockQuantity =
      type === 'PRODUCT'
        ? Number(raw.stockQuantity)
        : null;

    this.saving.set(true);
    this.error.set(null);

    const editing =
      this.editingListing();

    if (editing) {
      const request:
        UpdateListingRequest = {

        title:
          raw.title!.trim(),

        description:
          raw.description?.trim() ?? '',

        price:
          Number(raw.price),

        type,

        imageUrl:
          raw.imageUrl?.trim() ?? '',

        stockQuantity
      };

      this.listingsService
        .updateListing(
          merchantId,
          editing.id,
          request
        )
        .subscribe({
          next: response => {
            this.replaceListing(
              response.data
            );

            this.saving.set(false);
            this.modalOpen.set(false);
            this.editingListing.set(null);
          },

          error: error => {
            console.error(
              'Could not update listing',
              error
            );

            this.error.set(
              error?.error?.message ||
              'Could not update the listing.'
            );

            this.saving.set(false);
          }
        });

      return;
    }

    const request:
      CreateListingRequest = {

      title:
        raw.title!.trim(),

      description:
        raw.description?.trim()
          ? raw.description.trim()
          : null,

      price:
        Number(raw.price),

      type,

      imageUrl:
        raw.imageUrl?.trim()
          ? raw.imageUrl.trim()
          : null,

      stockQuantity
    };

    this.listingsService
      .createListing(
        merchantId,
        request
      )
      .subscribe({
        next: response => {
          this.listings.update(
            listings => [
              response.data,
              ...listings
            ]
          );

          this.saving.set(false);
          this.modalOpen.set(false);
        },

        error: error => {
          console.error(
            'Could not create listing',
            error
          );

          this.error.set(
            error?.error?.message ||
            'Could not create the listing.'
          );

          this.saving.set(false);
        }
      });
  }

  deactivate(
    listing: MerchantListing
  ): void {
    if (
      this.changingListingId()
    ) {
      return;
    }

    const confirmed =
      window.confirm(
        `Deactivate "${listing.title}"? It will no longer appear in the public marketplace.`
      );

    if (!confirmed) {
      return;
    }

    this.changingListingId.set(
      listing.id
    );

    this.error.set(null);

    this.listingsService
      .deactivateListing(
        listing.merchantId,
        listing.id
      )
      .subscribe({
        next: response => {
          if (response?.data) {
            this.replaceListing(
              response.data
            );
          } else {
            this.listings.update(
              listings =>
                listings.map(item =>
                  item.id === listing.id
                    ? {
                        ...item,
                        active: false
                      }
                    : item
                )
            );
          }

          this.changingListingId.set(
            null
          );
        },

        error: error => {
          console.error(
            'Could not deactivate listing',
            error
          );

          this.error.set(
            error?.error?.message ||
            'Could not deactivate the listing.'
          );

          this.changingListingId.set(
            null
          );
        }
      });
  }

  reactivate(
    listing: MerchantListing
  ): void {
    if (
      this.changingListingId()
    ) {
      return;
    }

    const merchant =
      this.selectedMerchant();

    if (
      !merchant ||
      merchant.status !== 'ACTIVE'
    ) {
      this.error.set(
        'The merchant account must be ACTIVE before this listing can be published.'
      );
      return;
    }

    this.changingListingId.set(
      listing.id
    );

    this.error.set(null);

    this.listingsService
      .updateListing(
        listing.merchantId,
        listing.id,
        {
          active: true
        }
      )
      .subscribe({
        next: response => {
          this.replaceListing(
            response.data
          );

          this.changingListingId.set(
            null
          );
        },

        error: error => {
          console.error(
            'Could not reactivate listing',
            error
          );

          this.error.set(
            error?.error?.message ||
            'Could not reactivate the listing.'
          );

          this.changingListingId.set(
            null
          );
        }
      });
  }

  selectedMerchant():
    MerchantSummary | null {

    return (
      this.merchants()
        .find(
          merchant =>
            merchant.id ===
            this.selectedMerchantId()
        ) ??
      null
    );
  }

  isProductForm(): boolean {
    return (
      this.listingForm
        .get('type')
        ?.value === 'PRODUCT'
    );
  }

  stockLabel(
    listing: MerchantListing
  ): string {

    if (
      listing.type === 'SERVICE'
    ) {
      return 'No stock required';
    }

    const stock =
      listing.stockQuantity ?? 0;

    if (stock <= 0) {
      return 'Out of stock';
    }

    if (stock === 1) {
      return '1 in stock';
    }

    return `${stock} in stock`;
  }

  setFilter(
    filter: ListingFilter
  ): void {
    this.filter.set(
      filter
    );
  }

  hideBrokenImage(
    event: Event
  ): void {
    const image =
      event.target as HTMLImageElement;

    image.style.display =
      'none';
  }

  private configureStockValidation(
    type: ListingType
  ): void {
    const control =
      this.listingForm
        .get('stockQuantity');

    if (!control) {
      return;
    }

    if (type === 'PRODUCT') {
      control.setValidators([
        Validators.required,
        Validators.min(0)
      ]);

      if (control.value === null) {
        control.setValue(
          0,
          {
            emitEvent: false
          }
        );
      }
    } else {
      control.clearValidators();

      control.setValue(
        null,
        {
          emitEvent: false
        }
      );
    }

    control.updateValueAndValidity({
      emitEvent: false
    });
  }

  private replaceListing(
    updated: MerchantListing
  ): void {
    this.listings.update(
      listings =>
        listings.map(
          listing =>
            listing.id ===
            updated.id
              ? updated
              : listing
        )
    );
  }
}
