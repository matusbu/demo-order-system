import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import NavBar from '../components/NavBar';
import { getOrdersByCustomer, cancelOrder, BASE_URL } from '../api/orderApi';
import { useAuth } from '../context/AuthContext';
import type { Order, OrderStatus, OrderStatusMessage } from '../types';
import styles from './OrdersPage.module.css';

const CANCELLABLE: OrderStatus[] = ['CREATED', 'RESERVED', 'PAID'];

const STATUS_LABELS: Record<OrderStatus, string> = {
  CREATED: 'Created',
  RESERVED: 'Reserved',
  PAID: 'Paid',
  RELEASING_RESERVATION: 'Releasing Reservation',
  RETURNING_PAYMENT: 'Returning Payment',
  READY_TO_SHIP: 'Ready to Ship',
  SHIPPING: 'Shipping',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

const BADGE_CLASS: Record<OrderStatus, string> = {
  CREATED: styles.badgeGray,
  RESERVED: styles.badgeBlue,
  PAID: styles.badgeBlue,
  RELEASING_RESERVATION: styles.badgeOrange,
  RETURNING_PAYMENT: styles.badgeOrange,
  READY_TO_SHIP: styles.badgePurple,
  SHIPPING: styles.badgePurple,
  DELIVERED: styles.badgeGreen,
  CANCELLED: styles.badgeRed,
};

export default function OrdersPage() {
  const { customerName } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState<string | null>(null);
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!customerName) return;

    getOrdersByCustomer(customerName)
      .then(res => setOrders(res.data))
      .finally(() => setLoading(false));

    const client = new Client({
      webSocketFactory: () => new SockJS(`${BASE_URL}/ws/orders`),
      onConnect: () => {
        client.subscribe(`/topic/orders/${customerName}`, frame => {
          const msg: OrderStatusMessage = JSON.parse(frame.body);
          setOrders(prev =>
            prev.map(o =>
              o.id === msg.orderId
                ? { ...o, status: msg.status, updatedAt: msg.updatedAt }
                : o
            )
          );
        });
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [customerName]);

  const handleCancel = async (id: string) => {
    setCancellingId(id);
    try {
      const res = await cancelOrder(id);
      setOrders(prev => prev.map(o => (o.id === id ? res.data : o)));
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <div>
      <NavBar />
      <main className={styles.main}>
        <h2 className={styles.heading}>My Orders</h2>

        {loading ? (
          <p className={styles.empty}>Loading…</p>
        ) : orders.length === 0 ? (
          <p className={styles.empty}>No orders yet. Head to the shop!</p>
        ) : (
          <div className={styles.list}>
            {orders.map(order => (
              <div key={order.id} className={styles.card}>
                <div className={styles.cardHeader}>
                  <span className={styles.orderId}>#{order.id}</span>
                  <span className={`${styles.badge} ${BADGE_CLASS[order.status]}`}>
                    {STATUS_LABELS[order.status]}
                  </span>
                </div>

                <div className={styles.cardBody}>
                  <div className={styles.field}>
                    <span className={styles.fieldLabel}>Product</span>
                    <span className={styles.fieldValue}>{order.productName}</span>
                  </div>
                  <div className={styles.field}>
                    <span className={styles.fieldLabel}>Quantity</span>
                    <span className={styles.fieldValue}>{order.quantity}</span>
                  </div>
                  <div className={styles.field}>
                    <span className={styles.fieldLabel}>Ordered</span>
                    <span className={styles.fieldValue}>
                      {new Date(order.createdAt).toLocaleString()}
                    </span>
                  </div>
                </div>

                {CANCELLABLE.includes(order.status) && (
                  <button
                    className={styles.cancelBtn}
                    onClick={() => handleCancel(order.id)}
                    disabled={cancellingId === order.id}
                  >
                    {cancellingId === order.id ? 'Cancelling…' : 'Cancel Order'}
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
