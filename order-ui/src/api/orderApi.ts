import axios from 'axios';
import type { Order } from '../types';

export const BASE_URL = import.meta.env.VITE_ORDER_ENGINE_URL ?? 'http://localhost:8080';

const api = axios.create({ baseURL: BASE_URL });

export const createOrder = (customerName: string, productName: string, quantity: number) =>
  api.post<Order>('/orders', { customerName, productName, quantity });

export const getOrdersByCustomer = (customerName: string) =>
  api.get<Order[]>('/orders', { params: { customerName } });

export const cancelOrder = (id: string) =>
  api.delete<Order>(`/orders/${id}/cancel`);
