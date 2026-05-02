import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, TitleCasePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MerchantService } from '../../core/services/merchant.service';
import { ToastService } from '../../core/services/toast.service';
import { Merchant, MerchantApiKey, MerchantPayment, RegisterMerchantRequest, MerchantCategory } from '../../shared/models/app.models';

type ModalType = 'register' | 'keys' | 'payments' | 'newKey' | null;

@Component({
  selector: 'app-merchant',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TitleCasePipe],
  templateUrl: './merchant.component.html',
  styleUrls: ['./merchant.component.scss'],
})
export class MerchantComponent implements OnInit {
  private merchantService = inject(MerchantService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  merchants = signal<Merchant[]>([]);
  apiKeys = signal<MerchantApiKey[]>([]);
  payments = signal<MerchantPayment[]>([]);
  selectedMerchant = signal<Merchant | null>(null);
  newlyCreatedKey = signal<string | null>(null);
  loading = signal(true);
  submitting = signal(false);
  activeModal = signal<ModalType>(null);

  readonly categories: MerchantCategory[] = [
    'RETAIL', 'FOOD_BEVERAGE', 'HEALTH_BEAUTY', 'TRANSPORT',
    'EDUCATION', 'ENTERTAINMENT', 'SERVICES', 'UTILITIES', 'OTHER'
  ];

  registerForm = this.fb.group({
    businessName: ['', [Validators.required, Validators.minLength(2)]],
    category: ['RETAIL' as MerchantCategory, Validators.required],
    businessRegistrationNumber: [''],
    businessEmail: ['', Validators.email],
    businessPhone: [''],
    description: [''],
  });

  newKeyForm = this.fb.group({
    label: ['', Validators.required],
    environment: ['TEST' as 'TEST' | 'LIVE', Validators.required],
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.merchantService.getMine().subscribe({
      next: res => { this.merchants.set(res.data ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openRegister() { this.registerForm.reset({ category: 'RETAIL' }); this.activeModal.set('register'); }
  closeModal() { this.activeModal.set(null); this.newlyCreatedKey.set(null); }

  submitRegister() {
    if (this.registerForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    const v = this.registerForm.value as RegisterMerchantRequest;
    this.merchantService.register(v).subscribe({
      next: res => {
        this.merchants.update(list => [res.data, ...list]);
        this.toast.success(`"${res.data.businessName}" registered — pending verification`);
        this.closeModal();
        this.submitting.set(false);
      },
      error: e => { this.toast.error(e.error?.message ?? 'Registration failed'); this.submitting.set(false); },
    });
  }

  openKeys(merchant: Merchant) {
    this.selectedMerchant.set(merchant);
    this.activeModal.set('keys');
    this.loadKeys(merchant.id);
  }

  loadKeys(merchantId: string) {
    this.merchantService.listApiKeys(merchantId).subscribe({
      next: res => this.apiKeys.set(res.data ?? []),
    });
  }

  openNewKey() { this.newKeyForm.reset({ environment: 'TEST' }); this.activeModal.set('newKey'); }

  submitNewKey() {
    if (this.newKeyForm.invalid || this.submitting() || !this.selectedMerchant()) return;
    this.submitting.set(true);
    const { label, environment } = this.newKeyForm.value;
    this.merchantService.generateApiKey(this.selectedMerchant()!.id, label!, environment!).subscribe({
      next: res => {
        this.newlyCreatedKey.set(res.data.fullKey ?? null);
        this.loadKeys(this.selectedMerchant()!.id);
        this.submitting.set(false);
        this.activeModal.set('keys');
        this.toast.success('API key created — copy it now!');
      },
      error: e => { this.toast.error(e.error?.message ?? 'Key creation failed'); this.submitting.set(false); },
    });
  }

  revokeKey(keyId: string) {
    if (!this.selectedMerchant()) return;
    this.merchantService.revokeApiKey(this.selectedMerchant()!.id, keyId).subscribe({
      next: () => { this.toast.success('Key revoked'); this.loadKeys(this.selectedMerchant()!.id); },
      error: e => this.toast.error(e.error?.message ?? 'Revoke failed'),
    });
  }

  openPayments(merchant: Merchant) {
    this.selectedMerchant.set(merchant);
    this.activeModal.set('payments');
    this.merchantService.getPayments(merchant.id).subscribe({
      next: res => this.payments.set(res.data ?? []),
    });
  }

  refundPayment(paymentId: string) {
    if (!this.selectedMerchant()) return;
    this.merchantService.refundPayment(this.selectedMerchant()!.id, paymentId).subscribe({
      next: () => { this.toast.success('Refund processed'); this.openPayments(this.selectedMerchant()!); },
      error: e => this.toast.error(e.error?.message ?? 'Refund failed'),
    });
  }

  copyKey(key: string) {
    navigator.clipboard.writeText(key).then(() => this.toast.success('Key copied to clipboard'));
  }

  statusClass(status: string): string {
    const m: Record<string, string> = {
      ACTIVE: 'success', PENDING_VERIFICATION: 'warning',
      SUSPENDED: 'danger', REJECTED: 'danger', CLOSED: 'muted'
    };
    return m[status] ?? 'muted';
  }

  categoryLabel(c: string): string {
    return c.replace('_', ' ');
  }
}
