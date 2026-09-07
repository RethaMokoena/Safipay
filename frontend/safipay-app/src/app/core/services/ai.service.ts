import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface AiAskResponse {
  answer: string;
  sources: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/ai`;

  ask(question: string): Observable<AiAskResponse> {
    return this.http.post<AiAskResponse>(`${this.apiUrl}/ask`, { question });
  }
}
