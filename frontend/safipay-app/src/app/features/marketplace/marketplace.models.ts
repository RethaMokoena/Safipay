export type ListingType = 'PRODUCT' | 'SERVICE';

export type BusinessCategory =
  | 'RETAIL'
  | 'FOOD_BEVERAGE'
  | 'HEALTH_BEAUTY'
  | 'TRANSPORT'
  | 'EDUCATION'
  | 'ENTERTAINMENT'
  | 'SERVICES'
  | 'UTILITIES'
  | 'OTHER';

export interface MarketplaceMerchant {
  id: string;
  businessName: string;
  category: BusinessCategory;
  description: string | null;
  logoUrl: string | null;
  status: string;
}

export interface MarketplaceListing {
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

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}