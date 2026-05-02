import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

function passwordMatch(control: AbstractControl) {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);
  showPassword = signal(false);
  step = signal<1 | 2>(1);

  form = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName:  ['', [Validators.required, Validators.minLength(2)]],
    email:     ['', [Validators.required, Validators.email]],
    phoneNumber: [''],
    password:  ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordMatch });

  get f() { return this.form.controls; }

  togglePassword() { this.showPassword.update(v => !v); }
  goBack() { this.step.set(1); }

  nextStep() {
    this.f['firstName'].markAsTouched();
    this.f['lastName'].markAsTouched();
    this.f['email'].markAsTouched();
    if (this.f['firstName'].valid && this.f['lastName'].valid && this.f['email'].valid) {
      this.step.set(2);
    }
  }

  submit() {
    if (this.form.invalid || this.loading()) return;
    this.error.set(null);
    this.loading.set(true);

    const { confirmPassword, ...payload } = this.form.value;

    this.authService.register(payload as any).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (msg: string) => {
        this.error.set(msg);
        this.loading.set(false);
      },
    });
  }
}
