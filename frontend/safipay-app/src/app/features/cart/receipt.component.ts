import {
  Component,
  inject,
  signal
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';

import {
  CheckoutService
} from './checkout.service';

import {
  CheckoutResponse
} from './checkout.models';
import { ReceiptPdfService } from './receipt-pdf.service';

import {
  MerchantOrderReceipt
} from './checkout.models';

@Component({
  selector: 'app-receipt',

  standalone: true,

  imports: [
    CommonModule,
    RouterLink
  ],

  templateUrl:
    './receipt.component.html',

  styleUrls: [
    './receipt.component.scss'
  ]
})
export class ReceiptComponent {

  private readonly route =
    inject(ActivatedRoute);

  private readonly checkoutService =
    inject(CheckoutService);

    private readonly pdfService =
  inject(ReceiptPdfService);

  readonly checkout =
    signal<CheckoutResponse | null>(
      null
    );

  readonly loading =
    signal(true);

  readonly error =
    signal<string | null>(
      null
    );


  constructor() {

    this.loadReceipt();
  }


  loadReceipt(): void {

    const checkoutId =
      this.route.snapshot
        .paramMap
        .get('checkoutId');


    if (!checkoutId) {

      this.error.set(
        'Receipt ID is missing.'
      );

      this.loading.set(false);

      return;
    }


    this.loading.set(true);
    this.error.set(null);


    this.checkoutService
      .getCheckout(
        checkoutId
      )
      .subscribe({

        next: response => {

          this.checkout.set(
            response.data
          );

          this.loading.set(false);
        },


        error: error => {

          console.error(
            'Could not load receipt',
            error
          );

          this.error.set(
            'Could not load this receipt.'
          );

          this.loading.set(false);
        }
      });
  }

  downloadOverall(): void {

  const receipt =
    this.checkout();

  if (!receipt) {
    return;
  }

  this.pdfService.downloadOverall(
    receipt
  );
}


downloadMerchant(
  order: MerchantOrderReceipt
): void {

  const receipt =
    this.checkout();

  if (!receipt) {
    return;
  }

  this.pdfService.downloadMerchant(
    receipt,
    order
  );
}


}