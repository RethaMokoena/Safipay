// ── Payments ──────────────────────────────────────────────────────
export interface Payment {
  id: string;
  senderUserId: string;
  recipientUserId: string;
  amount: number;
  currency: string;
  description?: string;
  referenceNote?: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  type: 'SEND_MONEY' | 'REQUEST_MONEY' | 'STOKVEL_CONTRIBUTION' | 'STOKVEL_PAYOUT';
  createdAt: string;
}

export interface SendMoneyRequest {
  recipientUserId: string;
  amount: number;
  description?: string;
  referenceNote?: string;
}

// ── Stokvels ──────────────────────────────────────────────────────
export interface Stokvel {
  id: number;
  name: string;
  description?: string;
  type: 'ROSCA' | 'POOL';
  status: 'FORMING' | 'ACTIVE' | 'COMPLETED' | 'SUSPENDED';
  contributionAmount: number;
  contributionFrequency: 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY';
  maxMembers: number;
  currentMemberCount: number;
  adminUserId: string;
  totalPoolBalance: number;
  currentPayoutIndex: number;
  members: StokvelMember[];
  createdAt: string;
}

export interface StokvelMember {
  id: number;
  userId: string;
  status: 'ACTIVE' | 'INACTIVE' | 'REMOVED';
  payoutOrder: number;
  hasReceivedPayout: boolean;
  joinedAt: string;
}

export interface StokvelContribution {
  id: number;
  stokvelId: number;
  userId: string;
  amount: number;
  status: 'PENDING' | 'CONFIRMED' | 'FAILED';
  transactionId: string;
  cycleNumber: number;
  contributedAt: string;
}

export interface StokvelPayout {
  id: number;
  stokvelId: number;
  recipientUserId: string;
  amount: number;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  type: 'ROSCA_ROTATION' | 'POOL_WITHDRAWAL';
  cycleNumber: number;
  processedAt: string;
}

export interface CreateStokvelRequest {
  name: string;
  description?: string;
  type: 'ROSCA' | 'POOL';
  contributionAmount: number;
  contributionFrequency: 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY';
  maxMembers: number;
}

export interface ContributeRequest {
  amount: number;
  transactionId: string;
}

// ── Merchant ──────────────────────────────────────────────────────
export interface Merchant {
  id: string;
  ownerUserId: string;
  businessName: string;
  businessRegistrationNumber?: string;
  category: MerchantCategory;
  businessEmail?: string;
  businessPhone?: string;
  description?: string;
  logoUrl?: string;
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED' | 'CLOSED';
  walletId?: string;
  createdAt: string;
}

export type MerchantCategory =
  'RETAIL' | 'FOOD_BEVERAGE' | 'HEALTH_BEAUTY' | 'TRANSPORT' |
  'EDUCATION' | 'ENTERTAINMENT' | 'SERVICES' | 'UTILITIES' | 'OTHER';

export interface MerchantApiKey {
  id: string;
  merchantId: string;
  keyPrefix: string;
  fullKey?: string;
  label: string;
  environment: 'TEST' | 'LIVE';
  active: boolean;
  expiresAt?: string;
  createdAt: string;
}

export interface MerchantPayment {
  id: string;
  merchantId: string;
  payerUserId: string;
  amount: number;
  currency: string;
  description?: string;
  merchantReference?: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';
  feeAmount: number;
  netAmount: number;
  createdAt: string;
}

export interface RegisterMerchantRequest {
  businessName: string;
  category: MerchantCategory;
  businessRegistrationNumber?: string;
  businessEmail?: string;
  businessPhone?: string;
  description?: string;
}
