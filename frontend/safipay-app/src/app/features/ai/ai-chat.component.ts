import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AiService } from '../../core/services/ai.service';

type ChatRole = 'user' | 'assistant';

interface ChatMessage {
  role: ChatRole;
  text: string;
  sources?: string[];
}

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ai-chat.component.html',
  styleUrl: './ai-chat.component.scss'
})
export class AiChatComponent {
  private readonly fb = inject(FormBuilder);
  private readonly aiService = inject(AiService);

  @ViewChild('messagesContainer')
  private messagesContainer?: ElementRef<HTMLDivElement>;

  readonly loading = signal(false);

  readonly messages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      text: 'Hi, I’m Safi. Ask me about your wallet, payments, stokvels, or businesses.'
    }
  ]);

  readonly suggestions = [
    'What is my wallet balance?',
    'What are my recent transactions?',
    'What stokvels am I part of?',
    'What businesses do I own?'
  ];

  readonly form = this.fb.group({
    question: ['', [Validators.required, Validators.maxLength(1000)]]
  });

  send(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    const question = this.form.controls.question.value?.trim();
    if (!question) return;

    this.messages.update(messages => [
      ...messages,
      { role: 'user', text: question }
    ]);

    this.form.reset();
    this.loading.set(true);
    this.scrollToBottom();

    this.aiService.ask(question).subscribe({
      next: response => {
        this.messages.update(messages => [
          ...messages,
          {
            role: 'assistant',
            text: response.answer,
            sources: response.sources
          }
        ]);

        this.loading.set(false);
        this.scrollToBottom();
      },
      error: error => {
        let text = 'I could not answer that right now. Please try again.';

        if (error.status === 401 || error.status === 403) {
          text = 'Your session is no longer valid. Please sign in again.';
        } else if (error.status === 502) {
          text = 'The AI service is temporarily unavailable. Please try again shortly.';
        } else if (error.error?.detail) {
          text = error.error.detail;
        } else if (error.error?.message) {
          text = error.error.message;
        }

        this.messages.update(messages => [
          ...messages,
          { role: 'assistant', text }
        ]);

        this.loading.set(false);
        this.scrollToBottom();
      }
    });
  }

  useSuggestion(question: string): void {
    this.form.controls.question.setValue(question);
    this.send();
  }

  clearChat(): void {
    this.messages.set([
      {
        role: 'assistant',
        text: 'Chat cleared. What would you like to know about your SafiPay account?'
      }
    ]);
  }

  trackMessage(index: number): number {
    return index;
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const element = this.messagesContainer?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    }, 0);
  }
}
