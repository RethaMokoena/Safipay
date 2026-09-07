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
  FormsModule
} from '@angular/forms';

import {
  MerchantOrder,
  MerchantSummary
} from './merchant-orders.models';

import {
  MerchantOrdersService
} from './merchant-orders.service';

@Component({
  selector: 'app-merchant-orders',

  standalone: true,

  imports: [
    CommonModule,
    FormsModule
  ],

  templateUrl:
    './merchant-orders.component.html',

  styleUrls: [
    './merchant-orders.component.scss'
  ]
})
export class MerchantOrdersComponent {

  private readonly ordersService =
    inject(MerchantOrdersService);


  readonly merchants =
    signal<MerchantSummary[]>([]);

  readonly selectedMerchantId =
    signal<string>('');

  readonly orders =
    signal<MerchantOrder[]>([]);

  readonly loading =
    signal(true);

  readonly error =
    signal<string | null>(null);

  readonly updatingOrderId =
    signal<string | null>(null);


  readonly paidCount =
    computed(
      () =>
        this.orders()
          .filter(
            order =>
              order.status === 'PAID'
          )
          .length
    );


  readonly processingCount =
    computed(
      () =>
        this.orders()
          .filter(
            order =>
              order.status === 'PROCESSING'
          )
          .length
    );


  readonly completedCount =
    computed(
      () =>
        this.orders()
          .filter(
            order =>
              order.status === 'COMPLETED'
          )
          .length
    );


  constructor() {
    this.loadMerchants();
  }


  loadMerchants(): void {

    this.loading.set(true);
    this.error.set(null);


    this.ordersService
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


          this.selectedMerchantId.set(
            merchants[0].id
          );


          this.loadOrders();
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

    this.loadOrders();
  }


  loadOrders(): void {

    const merchantId =
      this.selectedMerchantId();

    if (!merchantId) {
      return;
    }


    this.loading.set(true);
    this.error.set(null);


    this.ordersService
      .getOrders(
        merchantId
      )
      .subscribe({

        next: response => {

          this.orders.set(
            response.data ?? []
          );

          this.loading.set(false);
        },


        error: error => {

          console.error(
            'Could not load merchant orders',
            error
          );

          this.error.set(
            'Could not load merchant orders.'
          );

          this.loading.set(false);
        }
      });
  }


  startProcessing(
    order: MerchantOrder
  ): void {

    this.changeStatus(
      order,
      'PROCESSING'
    );
  }


  markCompleted(
    order: MerchantOrder
  ): void {

    this.changeStatus(
      order,
      'COMPLETED'
    );
  }


  private changeStatus(
    order: MerchantOrder,
    status: 'PROCESSING' | 'COMPLETED'
  ): void {

    if (this.updatingOrderId()) {
      return;
    }


    this.updatingOrderId.set(
      order.orderId
    );

    this.error.set(null);


    this.ordersService
      .updateStatus(
        order.merchantId,
        order.orderId,
        status
      )
      .subscribe({

        next: response => {

          const updated =
            response.data;

          this.orders.update(
            orders =>
              orders.map(
                existing =>
                  existing.orderId ===
                  updated.orderId
                    ? updated
                    : existing
              )
          );

          this.updatingOrderId.set(
            null
          );
        },


        error: error => {

          console.error(
            'Could not update order',
            error
          );

          this.error.set(
            error?.error?.message ||
            'Could not update the order status.'
          );

          this.updatingOrderId.set(
            null
          );
        }
      });
  }


  itemCount(
    order: MerchantOrder
  ): number {

    return order.items
      .reduce(
        (total, item) =>
          total + item.quantity,
        0
      );
  }


  shortId(
    id: string
  ): string {

    return id
      .substring(0, 8)
      .toUpperCase();
  }
}