import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { WalletService } from '../../core/services/wallet.service';
import { PaymentService } from '../../core/services/payment.service';
import { ToastService } from '../../core/services/toast.service';

import { Wallet, Transaction } from '../../shared/models/wallet.models';

@Component({
  selector: 'app-wallet',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './wallet.component.html',
  styleUrls: ['./wallet.component.scss'],
})
export class WalletComponent implements OnInit {

  private walletService = inject(WalletService);
  private paymentService = inject(PaymentService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  wallet = signal<Wallet | null>(null);
  transactions = signal<Transaction[]>([]);

  loading = signal(true);
  txLoading = signal(true);

  activeModal = signal<'topup' | 'transfer' | null>(null);

  submitting = signal(false);

  page = signal(0);


  // =====================================================
  // TOP UP FORM
  // =====================================================

  topUpForm = this.fb.group({
    amount: [
      null as number | null,
      [
        Validators.required,
        Validators.min(1)
      ]
    ],
  });


  // =====================================================
  // TRANSFER FORM
  // =====================================================

  transferForm = this.fb.group({
    recipientEmail: [
      '',
      [
        Validators.required,
        Validators.email
      ]
    ],

    amount: [
      null as number | null,
      [
        Validators.required,
        Validators.min(0.01)
      ]
    ],

    description: [''],
  });


  // =====================================================
  // INIT
  // =====================================================

  ngOnInit() {
    this.loadWallet();
    this.loadTransactions();
  }


  // =====================================================
  // LOAD WALLET
  // =====================================================

  loadWallet() {

    this.loading.set(true);

    this.walletService.getMyWallet().subscribe({

      next: res => {
        this.wallet.set(res.data);
        this.loading.set(false);
      },

      error: () => {
        this.loading.set(false);
      },

    });
  }


  // =====================================================
  // LOAD TRANSACTIONS
  // =====================================================

  loadTransactions() {

    this.txLoading.set(true);

    this.walletService
      .getTransactions(this.page(), 20)
      .subscribe({

        next: res => {
          this.transactions.set(res.data ?? []);
          this.txLoading.set(false);
        },

        error: () => {
          this.txLoading.set(false);
        },

      });
  }


  // =====================================================
  // MODALS
  // =====================================================

  openModal(type: 'topup' | 'transfer') {

    this.activeModal.set(type);

    this.topUpForm.reset();
    this.transferForm.reset();
  }


  closeModal() {
    this.activeModal.set(null);
  }


  // =====================================================
  // TOP UP AMOUNT HELPER
  // =====================================================

  setTopUpAmount(amt: number) {

    this.topUpForm.patchValue({
      amount: amt as number | null
    });
  }


  // =====================================================
  // TOP UP
  // =====================================================

  submitTopUp() {

    if (
      this.topUpForm.invalid ||
      this.submitting()
    ) {
      return;
    }

    this.submitting.set(true);

    this.walletService
      .topUp({
        amount: this.topUpForm.value.amount!
      })
      .subscribe({

        next: res => {

          this.wallet.set(res.data);

          this.toast.success(
            `R${res.data.balance.toFixed(2)} top-up successful`
          );

          this.closeModal();

          this.loadTransactions();

          this.submitting.set(false);
        },

        error: e => {

          this.toast.error(
            e.error?.message ??
            'Top-up failed'
          );

          this.submitting.set(false);
        },

      });
  }


  // =====================================================
  // SEND MONEY
  // =====================================================

  submitTransfer() {

    if (
      this.transferForm.invalid ||
      this.submitting()
    ) {
      return;
    }

    this.submitting.set(true);

    const {
      recipientEmail,
      description
    } = this.transferForm.value;

    const amount =
      this.transferForm.value.amount!;


    this.paymentService
      .sendMoney({

        recipientEmail: recipientEmail!,

        amount,

        description:
          description ?? undefined,

        referenceNote:
          'Wallet transfer'

      })
      .subscribe({

        next: () => {

          this.toast.success(
            `R${amount.toFixed(2)} sent successfully`
          );

          this.closeModal();

          // Payment response is not a Wallet,
          // so refresh the wallet after payment.
          this.loadWallet();

          this.loadTransactions();

          this.submitting.set(false);
        },

        error: e => {

          this.toast.error(
            e.error?.message ??
            'Transfer failed'
          );

          this.submitting.set(false);
        },

      });
  }


  // =====================================================
  // PAGINATION
  // =====================================================

  nextPage() {

    this.page.update(
      p => p + 1
    );

    this.loadTransactions();
  }


  prevPage() {

    if (this.page() > 0) {

      this.page.update(
        p => p - 1
      );

      this.loadTransactions();
    }
  }


  // =====================================================
  // TRANSACTION DISPLAY
  // =====================================================

  txIcon(tx: Transaction): string {

    return tx.type === 'CREDIT'
      ? '↓'
      : '↑';
  }


  txColor(tx: Transaction): string {

    return tx.type === 'CREDIT'
      ? 'credit'
      : 'debit';
  }

}