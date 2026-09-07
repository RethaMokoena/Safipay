export interface CheckoutRequest {
  items: CheckoutRequestItem[];
}

export interface CheckoutRequestItem {
  listingId: string;
  quantity: number;
}

export interface CheckoutResponse {
  checkoutId: string;
  status: CheckoutStatus;
  grandTotal: number;
  createdAt: string | null;
  merchantOrders: MerchantOrderReceipt[];
}

export interface MerchantOrderReceipt {
  orderId: string;
  merchantId: string;
  merchantName: string;
  status: MerchantOrderStatus;
  totalAmount: number;
  paymentId: string | null;
  items: OrderItemReceipt[];
}

export interface OrderItemReceipt {
  listingId: string;
  title: string;
  type: 'PRODUCT' | 'SERVICE';
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export type CheckoutStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'PAID'
  | 'PARTIALLY_PAID'
  | 'FAILED'
  | 'CANCELLED';

export type MerchantOrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PAYMENT_FAILED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED';
