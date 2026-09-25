import React from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Crown, 
  Hotel, 
  UtensilsCrossed, 
  CalendarCheck, 
  ShieldCheck, 
  ShoppingBag, 
  LogIn, 
  LogOut,
  Home, 
  ChefHat 
} from 'lucide-react';

const Navbar = ({ activeTab, setActiveTab, cartCount, openCart }) => {
  const { user, logout, isAdmin, isStaff } = useAuth();

  return (
    <header className="glass-header sticky top-0 z-50 px-4 py-3 flex items-center justify-between">
      <div style={{ maxWidth: '1440px', margin: '0 auto', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
        
        {/* Brand Logo -> Goes to Landing Page (HOME) */}
        <div 
          onClick={() => setActiveTab('HOME')}
          style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer', flexShrink: 0 }}
        >
          <div style={{ 
            width: '38px', 
            height: '38px', 
            borderRadius: '10px', 
            background: 'var(--gold-gradient)', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            boxShadow: 'var(--gold-glow)'
          }}>
            <Crown size={22} color="#0B1120" />
          </div>
          <div>
            <h1 style={{ fontSize: '1.15rem', fontWeight: 800, letterSpacing: '0.04em', color: 'var(--gold-light)', lineHeight: 1.1 }}>
              GRAND LUXE
            </h1>
            <p style={{ fontSize: '0.62rem', color: 'var(--text-dim)', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
              HMS Portals
            </p>
          </div>
        </div>

        {/* Navigation Tabs - Strict RBAC Filtered */}
        <nav style={{ display: 'flex', alignItems: 'center', gap: '4px', flexWrap: 'nowrap', overflowX: 'auto' }}>
          <button 
            onClick={() => setActiveTab('HOME')}
            className={activeTab === 'HOME' ? 'btn-gold' : 'btn-ghost'}
            style={{ fontSize: '0.82rem', padding: '6px 12px', whiteSpace: 'nowrap' }}
          >
            <Home size={14} />
            <span>Home</span>
          </button>

          {/* Suites Catalog: Visible to Guests, Visitors, and Admins */}
          {(!isStaff || isAdmin) && (
            <button 
              onClick={() => setActiveTab('HOTELS')}
              className={activeTab === 'HOTELS' ? 'btn-gold' : 'btn-ghost'}
              style={{ fontSize: '0.82rem', padding: '6px 12px', whiteSpace: 'nowrap' }}
            >
              <Hotel size={14} />
              <span>Suites</span>
            </button>
          )}

          {/* Dining / Kitchen Menu */}
          <button 
            onClick={() => setActiveTab('DINING')}
            className={activeTab === 'DINING' ? 'btn-gold' : 'btn-ghost'}
            style={{ fontSize: '0.82rem', padding: '6px 12px', whiteSpace: 'nowrap' }}
            title={isStaff && !isAdmin ? 'Kitchen Menu & 86\'d Recipe Control' : 'In-Room Gourmet Dining Menu'}
          >
            {isStaff && !isAdmin ? <ChefHat size={14} /> : <UtensilsCrossed size={14} />}
            <span>{isStaff && !isAdmin ? 'Kitchen Menu' : 'Dining'}</span>
          </button>

          {/* Guest Portal: Only available to authenticated Guests and Admins */}
          {user && (!isStaff || isAdmin) && (
            <button 
              onClick={() => setActiveTab('USER_DASHBOARD')}
              className={(activeTab === 'USER_DASHBOARD' || activeTab === 'BOOKINGS') ? 'btn-gold' : 'btn-ghost'}
              style={{ fontSize: '0.82rem', padding: '6px 12px', whiteSpace: 'nowrap' }}
            >
              <CalendarCheck size={14} />
              <span>Guest Portal</span>
            </button>
          )}

          {/* Staff Portal: STRICTLY RESTRICTED to authenticated Staff & Admin only! */}
          {user && (isStaff || isAdmin) && (
            <button 
              onClick={() => setActiveTab('STAFF')}
              className={activeTab === 'STAFF' ? 'btn-gold' : 'btn-ghost'}
              style={{ fontSize: '0.82rem', padding: '6px 12px', whiteSpace: 'nowrap' }}
            >
              <ChefHat size={14} />
              <span>Staff Portal</span>
            </button>
          )}

          {/* Admin Portal: STRICTLY RESTRICTED to Super Admin only! */}
          {user && isAdmin && (
            <button 
              onClick={() => setActiveTab('ADMIN')}
              className={activeTab === 'ADMIN' ? 'btn-gold' : 'btn-ghost'}
              style={{ fontSize: '0.82rem', padding: '6px 12px', whiteSpace: 'nowrap' }}
            >
              <ShieldCheck size={14} />
              <span>Admin Portal</span>
            </button>
          )}
        </nav>

        {/* Right Actions: Cart, User Profile / Sign In, Sign Out */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
          
          {/* In-Room Dining Cart Trigger - Only for Guests, Visitors, and Admins */}
          {(!isStaff || isAdmin) && (
            <button 
              onClick={openCart} 
              className="btn-outline-gold"
              style={{ padding: '6px 12px', position: 'relative', fontSize: '0.8rem' }}
              title="View In-Room Dining Cart"
            >
              <ShoppingBag size={15} />
              <span>Cart</span>
              {cartCount > 0 && (
                <span style={{
                  position: 'absolute',
                  top: '-5px',
                  right: '-5px',
                  background: 'var(--danger)',
                  color: '#fff',
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  width: '18px',
                  height: '18px',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid var(--bg-main)'
                }}>
                  {cartCount}
                </span>
              )}
            </button>
          )}

          {user ? (
            /* Logged In State: Profile Badge + Explicit Logout Button */
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div 
                onClick={() => {
                  if (isAdmin) setActiveTab('ADMIN');
                  else if (isStaff) setActiveTab('STAFF');
                  else setActiveTab('USER_DASHBOARD');
                }}
                className="glass-panel"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '5px 12px',
                  borderRadius: 'var(--radius-full)',
                  cursor: 'pointer',
                  border: '1px solid var(--border-gold)'
                }}
                title="View your portal"
              >
                <div style={{
                  width: '24px',
                  height: '24px',
                  borderRadius: '50%',
                  background: isAdmin ? 'var(--danger)' : isStaff ? 'var(--info)' : 'var(--gold-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#0B1120',
                  fontWeight: 800,
                  fontSize: '0.72rem'
                }}>
                  {user.username.charAt(0).toUpperCase()}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.1 }}>
                  <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#fff' }}>
                    {user.username}
                  </span>
                  <span style={{ 
                    fontSize: '0.62rem', 
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    color: isAdmin ? 'var(--danger)' : isStaff ? 'var(--info)' : 'var(--gold-light)' 
                  }}>
                    {isAdmin ? 'Super Admin' : isStaff ? 'Staff' : 'Guest'}
                  </span>
                </div>
              </div>

              {/* Explicit Sign Out Button */}
              <button
                onClick={() => {
                  logout();
                  setActiveTab('HOME');
                }}
                className="btn-ghost"
                style={{ padding: '6px 10px', fontSize: '0.78rem', color: 'var(--danger)', gap: '4px' }}
                title="Sign out of your session"
              >
                <LogOut size={14} />
                <span>Sign Out</span>
              </button>
            </div>
          ) : (
            /* Logged Out State: Sign In CTA Button */
            <button
              onClick={() => setActiveTab('LOGIN')}
              className={activeTab === 'LOGIN' ? 'btn-gold' : 'btn-outline-gold'}
              style={{ padding: '7px 14px', fontSize: '0.82rem' }}
            >
              <LogIn size={14} />
              <span>Sign In</span>
            </button>
          )}

        </div>

      </div>
    </header>
  );
};

export default Navbar;
