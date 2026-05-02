import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { PaymentService } from '../../core/services/payment.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { Payment } from '../../shared/models/app.models';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './payments.component.html',
  styleUrls: ['./payments.component.scss'],
})
export class PaymentsComponent implements OnInit {
  private paymentService = inject(PaymentService);
  private toast = inject(ToastService);
  readonly authService = inject(AuthService);
  private fb = inject(FormBuilder);

  payments = signal<Payment[]>([]);
  loading = signal(true);
  showSendModal = signal(false);
  submitting = signal(false);
  page = signal(0);
  activeFilter = signal<'ALL' | 'SENT' | 'RECEIVED'>('ALL');

  // FIX 1: explicitly typed so @for loop infers 'ALL' | 'SENT' | 'RECEIVED', not string
  filters: ('ALL' | 'SENT' | 'RECEIVED')[] = ['ALL', 'SENT', 'RECEIVED'];

  sendForm = this.fb.group({
    recipientUserId: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    description: [''],
    referenceNote: [''],
  });

  ngOnInit() { this.loadPayments(); }

  loadPayments() {
    this.loading.set(true);
    this.paymentService.getHistory(this.page(), 20).subscribe({
      next: res => { this.payments.set(res.data ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  get filteredPayments() {
    const uid = this.authService.currentUser()?.id;
    const all = this.payments();
    if (this.activeFilter() === 'SENT') return all.filter(p => p.senderUserId === uid);
    if (this.activeFilter() === 'RECEIVED') return all.filter(p => p.recipientUserId === uid);
    return all;
  }

  openSend() { this.showSendModal.set(true); this.sendForm.reset(); }
  closeSend() { this.showSendModal.set(false); }

  submitSend() {
    if (this.sendForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    // FIX 2: extract amount separately as number to avoid 'never' narrowing after the ! assertion
    const { recipientUserId, description, referenceNote } = this.sendForm.value;
    const amount = this.sendForm.value.amount as number;
    this.paymentService.sendMoney({
      recipientUserId: recipientUserId!,
      amount,
      description: description ?? undefined,
      referenceNote: referenceNote ?? undefined,
    }).subscribe({
      next: () => {
        this.toast.success(`R${amount.toFixed(2)} sent successfully`);
        this.closeSend();
        this.loadPayments();
        this.submitting.set(false);
      },
      error: (e) => {
        this.toast.error(e.error?.message ?? 'Payment failed');
        this.submitting.set(false);
      },
    });
  }

  isSent(p: Payment): boolean {
    return p.senderUserId === this.authService.currentUser()?.id;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      COMPLETED: 'success',
      FAILED: 'danger',
      PENDING: 'warning',
      REVERSED: 'muted',
    };
    return map[status] ?? 'muted';
  }

  nextPage() { this.page.update(p => p + 1); this.loadPayments(); }
  prevPage() { if (this.page() > 0) { this.page.update(p => p - 1); this.loadPayments(); } }
}