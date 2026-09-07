import {
  Component,
  inject,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { CheckoutService } from './checkout.service';
import { ReceiptPdfService } from './receipt-pdf.service';

import {
  CheckoutResponse,
  MerchantOrderReceipt,
  MerchantOrderStatus
} from './checkout.models';

type TimelineStep =
  | 'PAYMENT'
  | 'PROCESSING'
  | 'COMPLETED';

@Component({
  selector: 'app-orders',
  standalone: true,

  imports: [
    CommonModule,
    RouterLink
  ],

  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.scss']
})
export class OrdersComponent {

  private readonly checkoutService =
    inject(CheckoutService);

  private readonly pdfService =
    inject(ReceiptPdfService);


  readonly orders =
    signal<CheckoutResponse[]>([]);

  readonly loading =
    signal(true);

  readonly refreshing =
    signal(false);

  readonly error =
    signal<string | null>(null);

  readonly retryingOrderId =
    signal<string | null>(null);

  readonly retryErrors =
    signal<Record<string, string>>({});


  constructor() {
    this.loadOrders();
  }


  loadOrders(
    refresh = false
  ): void {

    if (refresh) {
      this.refreshing.set(true);
    } else {
      this.loading.set(true);
    }

    this.error.set(null);

    this.checkoutService
      .getMyCheckouts()
      .subscribe({

        next: response => {

          this.orders.set(
            response.data ?? []
          );

          this.loading.set(false);
          this.refreshing.set(false);
        },

        error: error => {

          console.error(
            'Could not load orders',
            error
          );

          this.error.set(
            'Could not load your previous orders.'
          );

          this.loading.set(false);
          this.refreshing.set(false);
        }
      });
  }


  refreshOrders(): void {
    if (
      this.loading() ||
      this.refreshing()
    ) {
      return;
    }

    this.loadOrders(true);
  }


  downloadOverall(
    order: CheckoutResponse
  ): void {

    this.pdfService.downloadOverall(
      order
    );
  }


  itemCount(
    order: CheckoutResponse
  ): number {

    return order.merchantOrders
      .flatMap(
        merchant => merchant.items
      )
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


  retryMerchantPayment(
    checkout: CheckoutResponse,
    merchant: MerchantOrderReceipt
  ): void {

    if (
      merchant.status !== 'PAYMENT_FAILED' ||
      this.retryingOrderId()
    ) {
      return;
    }

    this.retryingOrderId.set(
      merchant.orderId
    );

    this.clearRetryError(
      merchant.orderId
    );

    this.checkoutService
      .retryMerchantPayment(
        checkout.checkoutId,
        merchant.orderId
      )
      .subscribe({

        next: response => {

          const updatedCheckout =
            response.data;

          this.orders.update(
            orders =>
              orders.map(
                order =>
                  order.checkoutId ===
                  updatedCheckout.checkoutId
                    ? updatedCheckout
                    : order
              )
          );

          const updatedMerchant =
            updatedCheckout
              .merchantOrders
              .find(
                item =>
                  item.orderId ===
                  merchant.orderId
              );

          if (
            updatedMerchant?.status ===
            'PAYMENT_FAILED'
          ) {
            this.setRetryError(
              merchant.orderId,
              'Payment could not be completed. Check your wallet balance and try again.'
            );
          }

          this.retryingOrderId.set(
            null
          );
        },

        error: error => {

          console.error(
            'Could not retry merchant payment',
            error
          );

          const message =
            error?.error?.message ||
            error?.error?.error ||
            'Could not retry this payment. Please try again.';

          this.setRetryError(
            merchant.orderId,
            message
          );

          this.retryingOrderId.set(
            null
          );
        }
      });
  }


  retryErrorFor(
    orderId: string
  ): string | null {

    return (
      this.retryErrors()[orderId] ??
      null
    );
  }


  private setRetryError(
    orderId: string,
    message: string
  ): void {

    this.retryErrors.update(
      current => ({
        ...current,
        [orderId]: message
      })
    );
  }


  private clearRetryError(
    orderId: string
  ): void {

    this.retryErrors.update(
      current => {

        const next = {
          ...current
        };

        delete next[orderId];

        return next;
      }
    );
  }


  merchantStatusLabel(
    status: MerchantOrderStatus
  ): string {

    switch (status) {

      case 'PENDING_PAYMENT':
        return 'Awaiting payment';

      case 'PAID':
        return 'Paid';

      case 'PAYMENT_FAILED':
        return 'Payment failed';

      case 'PROCESSING':
        return 'Processing';

      case 'COMPLETED':
        return 'Completed';

      case 'CANCELLED':
        return 'Cancelled';

      case 'REFUNDED':
        return 'Refunded';

      default:
        return status;
    }
  }


  merchantStatusMessage(
    merchant: MerchantOrderReceipt
  ): string {

    switch (merchant.status) {

      case 'PENDING_PAYMENT':
        return 'Payment has not been completed for this merchant order.';

      case 'PAID':
        return `Payment was received. ${merchant.merchantName} has not started processing this order yet.`;

      case 'PROCESSING':
        return `${merchant.merchantName} is processing your order.`;

      case 'COMPLETED':
        return `This order from ${merchant.merchantName} has been completed.`;

      case 'PAYMENT_FAILED':
        return `Payment for this merchant order was not completed.`;

      case 'CANCELLED':
        return `This merchant order was cancelled.`;

      case 'REFUNDED':
        return `This merchant order has been refunded.`;

      default:
        return `Current order status: ${merchant.status}.`;
    }
  }


  isTimelineStatus(
    status: MerchantOrderStatus
  ): boolean {

    return [
      'PAID',
      'PROCESSING',
      'COMPLETED'
    ].includes(status);
  }


  isStepComplete(
    merchant: MerchantOrderReceipt,
    step: TimelineStep
  ): boolean {

    const status =
      merchant.status;

    if (step === 'PAYMENT') {
      return [
        'PAID',
        'PROCESSING',
        'COMPLETED'
      ].includes(status);
    }

    if (step === 'PROCESSING') {
      return [
        'PROCESSING',
        'COMPLETED'
      ].includes(status);
    }

    return status === 'COMPLETED';
  }


  isStepCurrent(
    merchant: MerchantOrderReceipt,
    step: TimelineStep
  ): boolean {

    if (
      merchant.status === 'PAID' &&
      step === 'PROCESSING'
    ) {
      return true;
    }

    if (
      merchant.status === 'PROCESSING' &&
      step === 'COMPLETED'
    ) {
      return true;
    }

    return false;
  }


  isConnectorComplete(
    merchant: MerchantOrderReceipt,
    connector:
      | 'PAYMENT_TO_PROCESSING'
      | 'PROCESSING_TO_COMPLETED'
  ): boolean {

    if (
      connector ===
      'PAYMENT_TO_PROCESSING'
    ) {
      return [
        'PROCESSING',
        'COMPLETED'
      ].includes(
        merchant.status
      );
    }

    return (
      merchant.status ===
      'COMPLETED'
    );
  }


  statusClass(
    status: MerchantOrderStatus
  ): string {

    switch (status) {

      case 'PAID':
        return 'merchant-status--paid';

      case 'PROCESSING':
        return 'merchant-status--processing';

      case 'COMPLETED':
        return 'merchant-status--completed';

      case 'PAYMENT_FAILED':
      case 'CANCELLED':
        return 'merchant-status--failed';

      case 'REFUNDED':
        return 'merchant-status--refunded';

      default:
        return 'merchant-status--pending';
    }
  }
}
