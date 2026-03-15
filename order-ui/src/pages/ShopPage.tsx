import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import NavBar from '../components/NavBar';
import { createOrder } from '../api/orderApi';
import { useAuth } from '../context/AuthContext';
import type { Product } from '../types';
import styles from './ShopPage.module.css';

const PRODUCTS: Product[] = [
  { name: 'iPhone 15 Pro', price: 999 },
  { name: 'MacBook Pro 14"', price: 1999 },
  { name: 'AirPods Pro', price: 249 },
  { name: 'iPad Pro 12.9"', price: 1099 },
  { name: 'Apple Watch Series 9', price: 399 },
];

const PRODUCT_ICON: Record<string, string> = {
  'iPhone 15 Pro': '📱',
  'MacBook Pro 14"': '💻',
  'AirPods Pro': '🎧',
  'iPad Pro 12.9"': '📟',
  'Apple Watch Series 9': '⌚',
};

export default function ShopPage() {
  const { customerName } = useAuth();
  const navigate = useNavigate();
  const [quantities, setQuantities] = useState<Record<string, number>>(
    Object.fromEntries(PRODUCTS.map(p => [p.name, 1]))
  );
  const [notification, setNotification] = useState<{ text: string; ok: boolean } | null>(null);
  const [loading, setLoading] = useState<string | null>(null);

  const setQuantity = (name: string, delta: number) => {
    setQuantities(prev => ({
      ...prev,
      [name]: Math.min(10, Math.max(1, prev[name] + delta)),
    }));
  };

  const showNotification = (text: string, ok: boolean) => {
    setNotification({ text, ok });
    setTimeout(() => setNotification(null), ok ? 1500 : 3000);
  };

  const handleBuy = async (product: Product) => {
    if (!customerName) return;
    setLoading(product.name);
    try {
      await createOrder(customerName, product.name, quantities[product.name]);
      showNotification(`Order placed for ${product.name}!`, true);
      setTimeout(() => navigate('/orders'), 1500);
    } catch {
      showNotification('Failed to place order. Please try again.', false);
    } finally {
      setLoading(null);
    }
  };

  return (
    <div>
      <NavBar />

      {notification && (
        <div className={notification.ok ? styles.notificationOk : styles.notificationErr}>
          {notification.text}
        </div>
      )}

      <main className={styles.main}>
        <h2 className={styles.heading}>Apple Products</h2>
        <div className={styles.grid}>
          {PRODUCTS.map(product => (
            <div key={product.name} className={styles.card}>
              <div className={styles.icon}>{PRODUCT_ICON[product.name]}</div>
              <h3 className={styles.productName}>{product.name}</h3>
              <p className={styles.price}>${product.price.toLocaleString()}</p>

              <div className={styles.quantityRow}>
                <button
                  className={styles.qtyBtn}
                  onClick={() => setQuantity(product.name, -1)}
                  disabled={quantities[product.name] <= 1}
                >
                  −
                </button>
                <span className={styles.qtyValue}>{quantities[product.name]}</span>
                <button
                  className={styles.qtyBtn}
                  onClick={() => setQuantity(product.name, 1)}
                  disabled={quantities[product.name] >= 10}
                >
                  +
                </button>
              </div>

              <button
                className={styles.buyBtn}
                onClick={() => handleBuy(product)}
                disabled={loading === product.name}
              >
                {loading === product.name ? 'Placing order…' : 'Buy Now'}
              </button>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
