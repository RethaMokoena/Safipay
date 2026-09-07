export type MerchantOrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PAYMENT_FAILED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED';

export interface MerchantOrderItem {
  listingId: string;
  title: string;
  type: 'PRODUCT' | 'SERVICE';
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface MerchantOrder {
  orderId: string;
  checkoutId: string;

  merchantId: string;
  merchantName: string;

  status: MerchantOrderStatus;

  totalAmount: number;

  paymentId: string | null;

  createdAt: string | null;

  items: MerchantOrderItem[];
}

export interface MerchantSummary {
  id: string;
  businessName: string;
  status: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp?: string;
}