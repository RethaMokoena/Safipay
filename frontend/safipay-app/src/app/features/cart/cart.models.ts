import { ListingType } from '../marketplace/marketplace.models';

export interface CartItem {
  listingId: string;

  merchantId: string;
  merchantName: string;

  title: string;
  price: number;

  type: ListingType;

  imageUrl: string | null;
  stockQuantity: number | null;

  quantity: number;
}

export interface MerchantCartGroup {
  merchantId: string;
  merchantName: string;

  items: CartItem[];

  itemCount: number;
  subtotal: number;
}

export type AddToCartResult =
  | 'ADDED'
  | 'OUT_OF_STOCK'
  | 'MAX_STOCK'
  | 'INACTIVE';