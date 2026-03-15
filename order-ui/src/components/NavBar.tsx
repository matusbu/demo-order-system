import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import styles from './NavBar.module.css';

export default function NavBar() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className={styles.nav}>
      <div className={styles.logo}>OrderSystem</div>
      <div className={styles.links}>
        <Link to="/shop" className={styles.link}>Shop</Link>
        <Link to="/orders" className={styles.link}>My Orders</Link>
      </div>
      <button className={styles.logoutBtn} onClick={handleLogout}>Logout</button>
    </nav>
  );
}
