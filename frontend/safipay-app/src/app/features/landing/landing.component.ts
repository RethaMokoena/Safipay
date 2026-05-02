import { Component, OnInit, OnDestroy, signal, AfterViewInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, NavbarComponent],
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.scss'],
})
export class LandingComponent implements OnInit, OnDestroy, AfterViewInit {
  typingText = signal('Send money instantly');
  typingDone = signal(false);

  private typingPhrases = [
    'Send money instantly',
    'Save with your stokvel',
    'Pay anyone, anywhere',
    'Build wealth together',
  ];
  private phraseIndex = 0;
  private charIndex = 0;
  private deleting = false;
  private typingTimer: ReturnType<typeof setTimeout> | null = null;

  activeFaq = signal<number | null>(null);

  faqs = [
    { q: 'Is SafiPay safe to use?', a: 'Yes. All transactions are encrypted end-to-end and we comply with the South African Payment Association (SAPA) standards. Your funds are protected by our security guarantee.' },
    { q: 'What is a stokvel and how does it work?', a: 'A stokvel is a rotating savings club. Members contribute a fixed amount each period and one member receives the full pot. SafiPay automates contributions, payouts, and keeps a full audit trail for your group.' },
    { q: 'What are the fees?', a: 'Sending money between SafiPay users is free. External withdrawals and card loads may carry a small fee (from R2). Full fee schedule is available in the app.' },
    { q: 'How quickly do transfers arrive?', a: 'Transfers between SafiPay wallets are instant. Bank transfers typically clear within 1–2 business days depending on your bank.' },
    { q: 'Can I use SafiPay without a bank account?', a: 'Yes. You just need a valid South African ID and phone number to create a wallet and start transacting.' },
  ];

  features = [
    { icon: '⚡', title: 'Instant Transfers', desc: 'Send money to anyone with a SafiPay wallet in seconds — 24/7, no bank required.', benefit: '→ Zero transfer fees between users' },
    { icon: '🏦', title: 'Smart Wallet', desc: 'Track spending, set savings goals, and earn rewards on every transaction you make.', benefit: '→ Full transaction history' },
    { icon: '🤝', title: 'Stokvel Groups', desc: 'Create or join a rotating savings group. We automate contributions, payouts, and notifications.', benefit: '→ Supports ROSCA & pool models' },
    { icon: '🔒', title: 'Bank-Grade Security', desc: '256-bit encryption, biometric auth, and real-time fraud monitoring on every account.', benefit: '→ POPIA compliant' },
    { icon: '📊', title: 'Spending Insights', desc: 'AI-powered breakdowns of where your money goes with actionable saving suggestions.', benefit: '→ Monthly reports' },
    { icon: '🌍', title: 'Pay Anyone', desc: 'Send to any South African bank account. No need for recipients to have a SafiPay account.', benefit: '→ All major SA banks supported' },
  ];

  steps = [
    { n: 1, icon: '📱', title: 'Create Your Account', desc: 'Sign up in under 2 minutes with your South African ID and phone number.', time: '< 2 min' },
    { n: 2, icon: '💳', title: 'Fund Your Wallet', desc: 'Top up via EFT, card, or at any participating retailer nationwide.', time: 'Instant' },
    { n: 3, icon: '💸', title: 'Send & Receive', desc: 'Transfer money, join stokvels, and track every rand in real time.', time: '24/7' },
  ];

  testimonials = [
    { stars: '★★★★★', quote: 'SafiPay changed the way our stokvel runs. No more chasing people for contributions — it\'s all automated and transparent.', name: 'Thandi M.', role: 'Stokvel Admin, Soweto', avatar: '👩🏾', featured: true },
    { stars: '★★★★★', quote: 'Sending money to my family in Limpopo used to take days. Now it\'s instant and I can see exactly when they receive it.', name: 'Sipho K.', role: 'Uber Driver, Johannesburg', avatar: '👨🏾', featured: false },
    { stars: '★★★★☆', quote: 'The savings goals feature is amazing. I saved for my daughter\'s school fees in 4 months without even noticing.', name: 'Nomsa D.', role: 'Nurse, Durban', avatar: '👩🏽', featured: false },
  ];

  ngOnInit() {
    this.startTyping();
  }

  ngAfterViewInit() {
    this.initScrollReveal();
  }

  ngOnDestroy() {
    if (this.typingTimer) clearTimeout(this.typingTimer);
  }

  scrollTo(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }

  toggleFaq(index: number) {    this.activeFaq.update(v => v === index ? null : index);
  }

  private startTyping() {
    const phrase = this.typingPhrases[this.phraseIndex];

    if (!this.deleting) {
      this.charIndex++;
      this.typingText.set(phrase.substring(0, this.charIndex));
      if (this.charIndex === phrase.length) {
        this.typingDone.set(true);
        this.typingTimer = setTimeout(() => {
          this.deleting = true;
          this.typingDone.set(false);
          this.startTyping();
        }, 2200);
        return;
      }
    } else {
      this.charIndex--;
      this.typingText.set(phrase.substring(0, this.charIndex));
      if (this.charIndex === 0) {
        this.deleting = false;
        this.phraseIndex = (this.phraseIndex + 1) % this.typingPhrases.length;
      }
    }

    this.typingTimer = setTimeout(() => this.startTyping(), this.deleting ? 45 : 90);
  }

  private initScrollReveal() {
    const observer = new IntersectionObserver(
      entries => entries.forEach(e => { if (e.isIntersecting) e.target.classList.add('in-view'); }),
      { threshold: 0.12 }
    );
    document.querySelectorAll('.reveal').forEach(el => observer.observe(el));
  }
}
