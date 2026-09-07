export type ListingType = 'PRODUCT' | 'SERVICE';

export type MerchantStatus =
  | 'PENDING'
  | 'ACTIVE'
  | 'SUSPENDED'
  | string;

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp?: string;
}

export interface MerchantSummary {
  id: string;
  businessName: string;
  status: MerchantStatus;
  category?: string;
}

export interface MerchantListing {
  id: string;
  merchantId: string;
  merchantName: string;
  title: string;
  description: string | null;
  price: number;
  type: ListingType;
  imageUrl: string | null;
  stockQuantity: number | null;
  active: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface CreateListingRequest {
  title: string;
  description: string | null;
  price: number;
  type: ListingType;
  imageUrl: string | null;
  stockQuantity: number | null;
}

export interface UpdateListingRequest {
  title?: string;
  description?: string;
  price?: number;
  type?: ListingType;
  imageUrl?: string;
  stockQuantity?: number | null;
  active?: boolean;
}
