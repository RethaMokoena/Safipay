import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class]="'toast--' + toast.type">
          <span class="toast-icon">{{ iconFor(toast.type) }}</span>
          <span class="toast-message">{{ toast.message }}</span>
          <button class="toast-close" (click)="toastService.dismiss(toast.id)">×</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed; bottom: 24px; right: 24px;
      display: flex; flex-direction: column; gap: 10px;
      z-index: 9999; max-width: 360px;
    }
    .toast {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 16px; border-radius: 10px;
      background: #111827; border: 1px solid rgba(0,229,255,0.18);
      color: #e8edf5; font-size: 0.875rem;
      animation: slideIn 0.3s ease;
      box-shadow: 0 8px 24px rgba(0,0,0,0.4);
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateX(20px); }
      to   { opacity: 1; transform: translateX(0); }
    }
    .toast--success { border-color: rgba(0,230,118,0.35); }
    .toast--error   { border-color: rgba(255,82,82,0.35); }
    .toast--warning { border-color: rgba(255,214,0,0.35); }
    .toast--info    { border-color: rgba(0,229,255,0.35); }
    .toast-icon  { font-size: 1rem; flex-shrink: 0; }
    .toast-message { flex: 1; line-height: 1.4; }
    .toast-close {
      background: none; border: none; color: #4a556b;
      cursor: pointer; font-size: 1.1rem; line-height: 1;
      padding: 0 2px; flex-shrink: 0;
      &:hover { color: #e8edf5; }
    }
  `]
})
export class ToastComponent {
  readonly toastService = inject(ToastService);

  iconFor(type: string): string {
    const icons: Record<string, string> = {
      success: '✓', error: '✕', warning: '⚠', info: 'ℹ'
    };
    return icons[type] ?? 'ℹ';
  }
}
