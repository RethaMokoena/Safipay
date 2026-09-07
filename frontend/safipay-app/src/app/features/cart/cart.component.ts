import {
  Component,
  inject
} from '@angular/core';

import {
  signal
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  RouterLink
} from '@angular/router';

import {
  Router
} from '@angular/router';

import {
  CartService
} from './cart.service';

import {
  CartItem,
  MerchantCartGroup
} from './cart.models';

import {
  CheckoutService
} from './checkout.service';

import {
  switchMap,
  finalize
} from 'rxjs/operators';

@Component({
  selector: 'app-cart',

  standalone: true,

  imports: [
    CommonModule,
    RouterLink
  ],

  templateUrl:
    './cart.component.html',

  styleUrls: [
    './cart.component.scss'
  ]
})
export class CartComponent {

  readonly cartService =
    inject(CartService);

    private readonly checkoutService =
  inject(CheckoutService);

private readonly router =
  inject(Router);

readonly checkoutLoading =
  signal(false);

readonly checkoutError =
  signal<string | null>(null);


  increase(
    item: CartItem
  ): void {

    this.cartService
      .increaseQuantity(
        item.listingId
      );
  }


  decrease(
    item: CartItem
  ): void {

    this.cartService
      .decreaseQuantity(
        item.listingId
      );
  }


  remove(
    item: CartItem
  ): void {

    this.cartService
      .removeItem(
        item.listingId
      );
  }


  clearMerchant(
    group: MerchantCartGroup
  ): void {

    const confirmed =
      window.confirm(
        `Remove all items from ${group.merchantName}?`
      );


    if (!confirmed) {
      return;
    }


    this.cartService
      .clearMerchant(
        group.merchantId
      );
  }


  clearCart(): void {

    const confirmed =
      window.confirm(
        'Clear your entire cart?'
      );


    if (!confirmed) {
      return;
    }


    this.cartService.clear();
  }


checkout(): void {

  if (
    this.cartService.empty() ||
    this.checkoutLoading()
  ) {
    return;
  }


  this.checkoutLoading.set(true);
  this.checkoutError.set(null);


  const request = {
    items:
      this.cartService
        .items()
        .map(item => ({
          listingId:
            item.listingId,

          quantity:
            item.quantity
        }))
  };


  this.checkoutService
    .createCheckout(request)

    .pipe(

      switchMap(response => {

        const checkout =
          response.data;

        if (!checkout?.checkoutId) {

          throw new Error(
            'Checkout was created without an ID.'
          );
        }


        return this.checkoutService
          .payCheckout(
            checkout.checkoutId
          );
      }),

      finalize(() => {

        this.checkoutLoading.set(false);

      })
    )

    .subscribe({

      next: response => {

        const checkout =
          response.data;


        if (
          checkout.status === 'PAID'
        ) {

          /*
           * Only clear the cart after
           * the backend confirms payment.
           */
          this.cartService.clear();


          this.router.navigate([
            '/dashboard/receipt',
            checkout.checkoutId
          ]);

          return;
        }


        if (
          checkout.status ===
          'PARTIALLY_PAID'
        ) {

          this.checkoutError.set(
            'Some merchant payments succeeded while others failed. Your cart has not been cleared.'
          );

          return;
        }


        this.checkoutError.set(
          'Checkout payment was not completed. Your cart has not been cleared.'
        );
      },


      error: error => {

        console.error(
          'Marketplace checkout failed',
          error
        );

        this.checkoutError.set(
          error?.error?.message ||
          error?.error?.error ||
          'Checkout failed. Please try again.'
        );
      }
    });
}


  canIncrease(
    item: CartItem
  ): boolean {

    if (
      item.type === 'SERVICE'
    ) {
      return false;
    }


    if (
      item.stockQuantity === null
    ) {
      return true;
    }


    return (
      item.quantity <
      item.stockQuantity
    );
  }


  trackGroup(
    index: number,
    group: MerchantCartGroup
  ): string {

    return group.merchantId;
  }


  trackItem(
    index: number,
    item: CartItem
  ): string {

    return item.listingId;
  }
}