import {
  computed,
  Injectable,
  signal
} from '@angular/core';

import {
  MarketplaceListing
} from '../marketplace/marketplace.models';

import {
  AddToCartResult,
  CartItem,
  MerchantCartGroup
} from './cart.models';

@Injectable({
  providedIn: 'root'
})
export class CartService {

  private readonly storageKey =
    'safipay_marketplace_cart';

  private readonly _items =
    signal<CartItem[]>(this.loadCart());


  readonly items =
    this._items.asReadonly();


  readonly empty = computed(() =>
    this._items().length === 0
  );


  readonly itemCount = computed(() =>
    this._items().reduce(
      (total, item) =>
        total + item.quantity,
      0
    )
  );


  readonly subtotal = computed(() =>
    this._items().reduce(
      (total, item) =>
        total +
        Number(item.price) *
        item.quantity,
      0
    )
  );


  readonly merchantCount = computed(() => {

    const merchantIds =
      new Set(
        this._items().map(
          item => item.merchantId
        )
      );

    return merchantIds.size;
  });


  readonly groups = computed<
    MerchantCartGroup[]
  >(() => {

    const grouped =
      new Map<
        string,
        MerchantCartGroup
      >();

    for (
      const item of this._items()
    ) {

      const existing =
        grouped.get(
          item.merchantId
        );

      if (existing) {

        existing.items.push(item);

        existing.itemCount +=
          item.quantity;

        existing.subtotal +=
          Number(item.price) *
          item.quantity;

        continue;
      }


      grouped.set(
        item.merchantId,
        {
          merchantId:
            item.merchantId,

          merchantName:
            item.merchantName,

          items: [item],

          itemCount:
            item.quantity,

          subtotal:
            Number(item.price) *
            item.quantity
        }
      );
    }


    return Array.from(
      grouped.values()
    );
  });


  addItem(
    listing: MarketplaceListing
  ): AddToCartResult {

    if (!listing.active) {
      return 'INACTIVE';
    }


    if (
      listing.type === 'PRODUCT' &&
      (
        listing.stockQuantity === null ||
        listing.stockQuantity <= 0
      )
    ) {
      return 'OUT_OF_STOCK';
    }


    const currentItems =
      this._items();


    const existing =
      currentItems.find(
        item =>
          item.listingId ===
            listing.id
      );


    /*
     * Services remain quantity 1
     * for V1.
     */
    if (
      existing &&
      listing.type === 'SERVICE'
    ) {
      return 'ADDED';
    }


    if (existing) {

      const nextQuantity =
        existing.quantity + 1;


      if (
        listing.type === 'PRODUCT' &&
        listing.stockQuantity !== null &&
        nextQuantity >
          listing.stockQuantity
      ) {
        return 'MAX_STOCK';
      }


      const updated =
        currentItems.map(
          item =>
            item.listingId ===
              listing.id

              ? {
                  ...item,
                  quantity:
                    nextQuantity
                }

              : item
        );


      this.setItems(updated);

      return 'ADDED';
    }


    const newItem: CartItem = {

      listingId:
        listing.id,

      merchantId:
        listing.merchantId,

      merchantName:
        listing.merchantName,

      title:
        listing.title,

      price:
        Number(listing.price),

      type:
        listing.type,

      imageUrl:
        listing.imageUrl,

      stockQuantity:
        listing.stockQuantity,

      quantity: 1
    };


    this.setItems([
      ...currentItems,
      newItem
    ]);


    return 'ADDED';
  }


  increaseQuantity(
    listingId: string
  ): void {

    const current =
      this._items();


    const item =
      current.find(
        value =>
          value.listingId ===
            listingId
      );


    if (!item) {
      return;
    }


    if (
      item.type === 'SERVICE'
    ) {
      return;
    }


    const nextQuantity =
      item.quantity + 1;


    if (
      item.stockQuantity !== null &&
      nextQuantity >
        item.stockQuantity
    ) {
      return;
    }


    this.setItems(
      current.map(
        value =>
          value.listingId ===
            listingId

            ? {
                ...value,
                quantity:
                  nextQuantity
              }

            : value
      )
    );
  }


  decreaseQuantity(
    listingId: string
  ): void {

    const current =
      this._items();


    const item =
      current.find(
        value =>
          value.listingId ===
            listingId
      );


    if (!item) {
      return;
    }


    if (
      item.quantity <= 1
    ) {

      this.removeItem(
        listingId
      );

      return;
    }


    this.setItems(
      current.map(
        value =>
          value.listingId ===
            listingId

            ? {
                ...value,
                quantity:
                  value.quantity - 1
              }

            : value
      )
    );
  }


  removeItem(
    listingId: string
  ): void {

    this.setItems(
      this._items().filter(
        item =>
          item.listingId !==
            listingId
      )
    );
  }


  clearMerchant(
    merchantId: string
  ): void {

    this.setItems(
      this._items().filter(
        item =>
          item.merchantId !==
            merchantId
      )
    );
  }


  clear(): void {

    this.setItems([]);
  }


  private setItems(
    items: CartItem[]
  ): void {

    this._items.set(items);


    try {

      localStorage.setItem(
        this.storageKey,
        JSON.stringify(items)
      );

    } catch {

      /*
       * Cart still works in memory
       * if storage is unavailable.
       */
    }
  }


  private loadCart(): CartItem[] {

    try {

      const stored =
        localStorage.getItem(
          this.storageKey
        );


      if (!stored) {
        return [];
      }


      const parsed =
        JSON.parse(stored);


      if (!Array.isArray(parsed)) {
        return [];
      }


      return parsed;

    } catch {

      return [];
    }
  }
}