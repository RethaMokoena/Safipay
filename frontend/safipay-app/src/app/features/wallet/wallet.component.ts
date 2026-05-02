import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WalletService } from '../../core/services/wallet.service';
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
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  wallet = signal<Wallet | null>(null);
  transactions = signal<Transaction[]>([]);
  loading = signal(true);
  txLoading = signal(true);
  activeModal = signal<'topup' | 'transfer' | null>(null);
  submitting = signal(false);
  page = signal(0);

  topUpForm = this.fb.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  transferForm = this.fb.group({
    recipientUserId: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    description: [''],
  });

  ngOnInit() {
    this.loadWallet();
    this.loadTransactions();
  }

  loadWallet() {
    this.loading.set(true);
    this.walletService.getMyWallet().subscribe({
      next: res => { this.wallet.set(res.data); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }

  loadTransactions() {
    this.txLoading.set(true);
    this.walletService.getTransactions(this.page(), 20).subscribe({
      next: res => { this.transactions.set(res.data ?? []); this.txLoading.set(false); },
      error: () => this.txLoading.set(false),
    });
  }

  openModal(type: 'topup' | 'transfer') {
    this.activeModal.set(type);
    this.topUpForm.reset();
    this.transferForm.reset();
  }

  closeModal() { this.activeModal.set(null); }

  // FIX 2: helper method so the template doesn't call patchValue inline with a plain number,
  // which fails strict type checking because the control is typed number | null
  setTopUpAmount(amt: number) {
    this.topUpForm.patchValue({ amount: amt as number | null });
  }

  submitTopUp() {
    if (this.topUpForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.walletService.topUp({ amount: this.topUpForm.value.amount! }).subscribe({
      next: res => {
        this.wallet.set(res.data);
        this.toast.success(`R${res.data.balance.toFixed(2)} top-up successful`);
        this.closeModal();
        this.loadTransactions();
        this.submitting.set(false);
      },
      error: (e) => {
        this.toast.error(e.error?.message ?? 'Top-up failed');
        this.submitting.set(false);
      },
    });
  }

  submitTransfer() {
    if (this.transferForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    // FIX 3: extract amount separately as number to avoid 'never' narrowing after the ! assertion
    const { recipientUserId, description } = this.transferForm.value;
    const amount = this.transferForm.value.amount!;
    this.walletService.transfer({
      recipientUserId: recipientUserId!,
      amount,
      description: description ?? undefined,
    }).subscribe({
      next: res => {
        this.wallet.set(res.data);
        this.toast.success(`R${amount.toFixed(2)} sent successfully`);
        this.closeModal();
        this.loadTransactions();
        this.submitting.set(false);
      },
      error: (e) => {
        this.toast.error(e.error?.message ?? 'Transfer failed');
        this.submitting.set(false);
      },
    });
  }

  nextPage() { this.page.update(p => p + 1); this.loadTransactions(); }
  prevPage() { if (this.page() > 0) { this.page.update(p => p - 1); this.loadTransactions(); } }

  txIcon(tx: Transaction): string {
    return tx.type === 'CREDIT' ? '↓' : '↑';
  }

  txColor(tx: Transaction): string {
    return tx.type === 'CREDIT' ? 'credit' : 'debit';
  }
}