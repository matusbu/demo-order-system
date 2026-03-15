export type OrderStatus =
  | 'CREATED'
  | 'RESERVED'
  | 'PAID'
  | 'RELEASING_RESERVATION'
  | 'RETURNING_PAYMENT'
  | 'READY_TO_SHIP'
  | 'SHIPPING'
  | 'DELIVERED'
  | 'CANCELLED';

export interface Order {
  id: string;
  customerName: string;
  productName: string;
  quantity: number;
  status: OrderStatus;
  createdAt: string;
  updatedAt: string;
}

export interface OrderStatusMessage {
  orderId: string;
  status: OrderStatus;
  updatedAt: string;
}

export interface Product {
  name: string;
  price: number;
}
