import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { StokvelService } from '../../core/services/stokvel.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { Stokvel, StokvelContribution, StokvelPayout, CreateStokvelRequest } from '../../shared/models/app.models';

type ModalType = 'create' | 'contribute' | 'detail' | null;

@Component({
  selector: 'app-stokvel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './stokvel.component.html',
  styleUrls: ['./stokvel.component.scss'],
})
export class StokvelComponent implements OnInit {
  private stokvelService = inject(StokvelService);
  private toast = inject(ToastService);
  readonly authService = inject(AuthService);
  private fb = inject(FormBuilder);

  myStokvels = signal<Stokvel[]>([]);
  allStokvels = signal<Stokvel[]>([]);
  selectedStokvel = signal<Stokvel | null>(null);
  contributions = signal<StokvelContribution[]>([]);
  payouts = signal<StokvelPayout[]>([]);
  loading = signal(true);
  submitting = signal(false);
  activeModal = signal<ModalType>(null);
  activeTab = signal<'mine' | 'browse'>('mine');

  createForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    type: ['ROSCA' as 'ROSCA' | 'POOL', Validators.required],
    contributionAmount: [null as number | null, [Validators.required, Validators.min(1)]],
    contributionFrequency: ['MONTHLY' as 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY', Validators.required],
    maxMembers: [null as number | null, [Validators.required, Validators.min(2), Validators.max(100)]],
  });

  contributeForm = this.fb.group({
    transactionId: ['TX-' + Date.now(), Validators.required],
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.stokvelService.getMine().subscribe({
      next: res => { this.myStokvels.set(res.data ?? []); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
    this.stokvelService.getAll().subscribe({
      next: res => this.allStokvels.set(res.data ?? []),
    });
  }

  openCreate() { this.createForm.reset({ type: 'ROSCA', contributionFrequency: 'MONTHLY' }); this.activeModal.set('create'); }
  closeModal() { this.activeModal.set(null); this.selectedStokvel.set(null); }

  openDetail(s: Stokvel) {
    this.selectedStokvel.set(s);
    this.activeModal.set('detail');
    this.stokvelService.getContributions(s.id).subscribe({ next: r => this.contributions.set(r.data ?? []) });
    this.stokvelService.getPayouts(s.id).subscribe({ next: r => this.payouts.set(r.data ?? []) });
  }

  openContribute(s: Stokvel) {
    this.selectedStokvel.set(s);
    this.contributeForm.patchValue({ transactionId: 'TX-' + Date.now() });
    this.activeModal.set('contribute');
  }

  submitCreate() {
    if (this.createForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    const v = this.createForm.value as CreateStokvelRequest;
    this.stokvelService.create(v).subscribe({
      next: res => {
        this.myStokvels.update(list => [res.data, ...list]);
        this.toast.success(`"${res.data.name}" created!`);
        this.closeModal();
        this.submitting.set(false);
      },
      error: e => { this.toast.error(e.error?.message ?? 'Create failed'); this.submitting.set(false); },
    });
  }

  submitContribute() {
    if (!this.selectedStokvel() || this.submitting()) return;
    this.submitting.set(true);
    const s = this.selectedStokvel()!;
    this.stokvelService.contribute(s.id, {
      amount: s.contributionAmount,
      transactionId: this.contributeForm.value.transactionId!,
    }).subscribe({
      next: () => {
        this.toast.success(`R${s.contributionAmount} contribution recorded`);
        this.closeModal();
        this.load();
        this.submitting.set(false);
      },
      error: e => { this.toast.error(e.error?.message ?? 'Contribution failed'); this.submitting.set(false); },
    });
  }

  joinStokvel(id: number) {
    this.stokvelService.join(id).subscribe({
      next: res => { this.toast.success(`Joined "${res.data.name}"`); this.load(); },
      error: e => this.toast.error(e.error?.message ?? 'Join failed'),
    });
  }

  activateStokvel(id: number) {
    this.stokvelService.activate(id).subscribe({
      next: () => { this.toast.success('Stokvel activated'); this.load(); this.closeModal(); },
      error: e => this.toast.error(e.error?.message ?? 'Activation failed'),
    });
  }

  triggerPayout(id: number) {
    this.stokvelService.triggerRoscaPayout(id).subscribe({
      next: res => { this.toast.success(`R${res.data.amount} payout processed`); this.openDetail(this.selectedStokvel()!); },
      error: e => this.toast.error(e.error?.message ?? 'Payout failed'),
    });
  }

  isAdmin(s: Stokvel): boolean { return s.adminUserId === this.authService.currentUser()?.id; }
  isMember(s: Stokvel): boolean { return s.members?.some(m => m.userId === this.authService.currentUser()?.id) ?? false; }

  statusClass(status: string): string {
    const m: Record<string, string> = { ACTIVE: 'success', FORMING: 'warning', COMPLETED: 'muted', SUSPENDED: 'danger' };
    return m[status] ?? 'muted';
  }

  frequencyLabel(f: string): string {
    const m: Record<string, string> = { WEEKLY: 'Weekly', BIWEEKLY: 'Bi-weekly', MONTHLY: 'Monthly' };
    return m[f] ?? f;
  }
}
