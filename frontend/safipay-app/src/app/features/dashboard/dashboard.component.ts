import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { WalletService } from '../../core/services/wallet.service';
import { PaymentService } from '../../core/services/payment.service';
import { StokvelService } from '../../core/services/stokvel.service';
import { Wallet } from '../../shared/models/wallet.models';
import { Payment, Stokvel } from '../../shared/models/app.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  private walletService = inject(WalletService);
  private paymentService = inject(PaymentService);
  private stokvelService = inject(StokvelService);

  wallet = signal<Wallet | null>(null);
  recentPayments = signal<Payment[]>([]);
  myStokvels = signal<Stokvel[]>([]);
  walletLoading = signal(true);

  ngOnInit() {
    this.walletService.getMyWallet().subscribe({
      next: res => { this.wallet.set(res.data); this.walletLoading.set(false); },
      error: () => {
        this.walletService.createWallet().subscribe({
          next: res => { this.wallet.set(res.data); this.walletLoading.set(false); },
          error: () => this.walletLoading.set(false),
        });
      },
    });
    this.paymentService.getHistory(0, 5).subscribe({
      next: res => this.recentPayments.set(res.data ?? []),
    });
    this.stokvelService.getMine().subscribe({
      next: res => this.myStokvels.set(res.data ?? []),
    });
  }

  isSent(p: Payment): boolean {
    return p.senderUserId === this.authService.currentUser()?.id;
  }

  totalPoolBalance(): number {
    return this.myStokvels().reduce((sum, s) => sum + (s.totalPoolBalance ?? 0), 0);
  }
}
