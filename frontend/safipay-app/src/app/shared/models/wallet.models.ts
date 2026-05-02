export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface Wallet {
  id: string;
  userId: string;
  balance: number;
  lockedBalance: number;
  availableBalance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  createdAt: string;
}

export interface Transaction {
  id: string;
  walletId: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  type: 'CREDIT' | 'DEBIT';
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  referenceId?: string;
  description?: string;
  counterpartyUserId?: string;
  createdAt: string;
}

export interface TransferRequest {
  recipientUserId: string;
  amount: number;
  description?: string;
}

export interface TopUpRequest {
  amount: number;
  referenceId?: string;
}
