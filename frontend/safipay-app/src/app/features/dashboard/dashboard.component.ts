import {
  Component,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { PaymentService } from '../../core/services/payment.service';
import { StokvelService } from '../../core/services/stokvel.service';

import { Wallet } from '../../shared/models/wallet.models';
import {
  Payment,
  Stokvel
} from '../../shared/models/app.models';

import { CheckoutService } from '../cart/checkout.service';

import {
  CheckoutResponse,
  MerchantOrderReceipt,
  MerchantOrderStatus
} from '../cart/checkout.models';


interface DashboardMerchantOrder {
  checkoutId: string;
  checkoutCreatedAt: string | null;
  merchant: MerchantOrderReceipt;
}


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent
  implements OnInit {

  readonly authService =
    inject(AuthService);

  private readonly walletService =
    inject(WalletService);

  private readonly paymentService =
    inject(PaymentService);

  private readonly stokvelService =
    inject(StokvelService);

  private readonly checkoutService =
    inject(CheckoutService);


  readonly wallet =
    signal<Wallet | null>(null);

  readonly recentPayments =
    signal<Payment[]>([]);

  readonly myStokvels =
    signal<Stokvel[]>([]);

  readonly checkouts =
    signal<CheckoutResponse[]>([]);


  readonly walletLoading =
    signal(true);

  readonly ordersLoading =
    signal(true);


  readonly orderCount =
    computed(
      () =>
        this.checkouts()
          .reduce(
            (total, checkout) =>
              total +
              checkout.merchantOrders.length,
            0
          )
    );


  readonly failedOrderCount =
    computed(
      () =>
        this.checkouts()
          .flatMap(
            checkout =>
              checkout.merchantOrders
          )
          .filter(
            order =>
              order.status ===
              'PAYMENT_FAILED'
          )
          .length
    );


  readonly latestMerchantOrders =
    computed<DashboardMerchantOrder[]>(
      () =>
        this.checkouts()
          .flatMap(
            checkout =>
              checkout.merchantOrders
                .map(
                  merchant => ({
                    checkoutId:
                      checkout.checkoutId,

                    checkoutCreatedAt:
                      checkout.createdAt,

                    merchant
                  })
                )
          )
          .slice(0, 4)
    );


  ngOnInit(): void {

    this.walletService
      .getMyWallet()
      .subscribe({

        next: response => {

          this.wallet.set(
            response.data
          );

          this.walletLoading.set(
            false
          );
        },

        error: () => {

          this.walletService
            .createWallet()
            .subscribe({

              next: response => {

                this.wallet.set(
                  response.data
                );

                this.walletLoading.set(
                  false
                );
              },

              error: () =>
                this.walletLoading.set(
                  false
                )
            });
        }
      });


    this.paymentService
      .getHistory(
        0,
        5
      )
      .subscribe({

        next: response =>
          this.recentPayments.set(
            response.data ?? []
          )
      });


    this.stokvelService
      .getMine()
      .subscribe({

        next: response =>
          this.myStokvels.set(
            response.data ?? []
          )
      });


    this.checkoutService
      .getMyCheckouts()
      .subscribe({

        next: response => {

          this.checkouts.set(
            response.data ?? []
          );

          this.ordersLoading.set(
            false
          );
        },

        error: () =>
          this.ordersLoading.set(
            false
          )
      });
  }


  isSent(
    payment: Payment
  ): boolean {

    return (
      payment.senderUserId ===
      this.authService
        .currentUser()
        ?.id
    );
  }


  totalPoolBalance(): number {

    return this.myStokvels()
      .reduce(
        (sum, stokvel) =>
          sum +
          (
            stokvel.totalPoolBalance ??
            0
          ),
        0
      );
  }


  orderStatusLabel(
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


  orderStatusClass(
    status: MerchantOrderStatus
  ): string {

    switch (status) {

      case 'PAID':
        return 'paid';

      case 'PROCESSING':
        return 'processing';

      case 'COMPLETED':
        return 'completed';

      case 'PAYMENT_FAILED':
        return 'failed';

      case 'REFUNDED':
        return 'refunded';

      case 'CANCELLED':
        return 'cancelled';

      default:
        return 'pending';
    }
  }
}
