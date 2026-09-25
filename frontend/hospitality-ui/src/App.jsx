import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import LandingPage from './components/LandingPage';
import HotelList from './components/HotelList';
import DiningMenu from './components/DiningMenu';
import UserDashboard from './components/UserDashboard';
import StaffDashboard from './components/StaffDashboard';
import AdminDashboard from './components/AdminDashboard';
import RoomBookingModal from './components/RoomBookingModal';
import LoginPage from './components/LoginPage';
import { Sparkles, ShieldAlert, ArrowRight, Lock } from 'lucide-react';

function AppContent() {
  const { user, isAdmin, isStaff } = useAuth();

  const [activeTab, setActiveTab] = useState('HOME'); // 'HOME' by default for stunning landing page
  const [cart, setCart] = useState([]);
  const [selectedHotelForBooking, setSelectedHotelForBooking] = useState(null);
  const [bookings, setBookings] = useState([]);

  // Strict RBAC: unauthorized tab requests automatically resolve to appropriate role dashboard
  const effectiveTab = (() => {
    // Unauthenticated visitors must sign in to view personal or operational portals
    if (!user) {
      if (activeTab === 'USER_DASHBOARD' || activeTab === 'BOOKINGS' || activeTab === 'STAFF' || activeTab === 'ADMIN') {
        return 'LOGIN';
      }
      return activeTab;
    }

    // Staff are hotel operators and only access operational workflows (HOME, STAFF, DINING, LOGIN)
    if (isStaff && !isAdmin) {
      if (activeTab === 'USER_DASHBOARD' || activeTab === 'BOOKINGS' || activeTab === 'HOTELS' || activeTab === 'ADMIN') {
        return 'STAFF';
      }
    }

    // Guests/customers cannot access staff or super admin portals
    if (activeTab === 'STAFF' && !isStaff && !isAdmin) return 'USER_DASHBOARD';
    if (activeTab === 'ADMIN' && !isAdmin) return 'USER_DASHBOARD';
    return activeTab;
  })();

  const handleBookingSuccess = (newBooking) => {
    setBookings((prev) => [newBooking, ...prev]);
  };

  const handleSelectHotel = (hotel) => {
    setSelectedHotelForBooking(hotel);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: 'var(--bg-main)' }}>
      {/* Luxury Navigation Header with Strict RBAC Filtering */}
      <Navbar 
        activeTab={effectiveTab} 
        setActiveTab={setActiveTab} 
        cartCount={cart.reduce((sum, item) => sum + item.quantity, 0)}
        openCart={() => setActiveTab('DINING')}
      />

      {/* Main Content Area */}
      <main style={{ flex: 1 }}>
        {/* World-Class Landing Page */}
        {effectiveTab === 'HOME' && (
          <LandingPage 
            onNavigate={(tab) => setActiveTab(tab)}
            onSelectHotel={handleSelectHotel}
          />
        )}

        {/* Hotels & Suites Catalog */}
        {effectiveTab === 'HOTELS' && (
          <HotelList onSelectHotel={handleSelectHotel} />
        )}

        {/* In-Room Dining Menu (Role-Adaptive for Guest vs Kitchen Staff) */}
        {effectiveTab === 'DINING' && (
          <DiningMenu 
            cart={cart} 
            setCart={setCart} 
            onOrderPlaced={() => {}}
            onNavigate={(tab) => setActiveTab(tab)}
          />
        )}

        {/* Sovereign Guest / User Dashboard */}
        {(effectiveTab === 'USER_DASHBOARD' || effectiveTab === 'BOOKINGS') && (
          <UserDashboard 
            bookings={bookings} 
            onNavigate={(tab) => setActiveTab(tab)}
            onSelectHotel={handleSelectHotel}
          />
        )}

        {/* Staff & Kitchen Operations Board (STRICTLY GUARDED) */}
        {effectiveTab === 'STAFF' && (
          (isStaff || isAdmin) ? (
            <StaffDashboard />
          ) : (
            <div style={{ maxWidth: '580px', margin: '80px auto', padding: '36px', textAlign: 'center' }} className="glass-panel">
              <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: 'rgba(239, 68, 68, 0.15)', border: '1px solid var(--danger)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
                <ShieldAlert size={36} color="var(--danger)" />
              </div>
              <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#fff', marginBottom: '8px' }}>
                Staff & Kitchen Access Restricted
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.6, marginBottom: '24px' }}>
                The Staff Portal is strictly restricted to authorized hotel staff, chefs, and attendants. Your current account (<strong style={{ color: 'var(--gold-light)' }}>{user?.username || 'Guest'}</strong>) is registered as a <span className="badge badge-gold">Sovereign Guest</span>.
              </p>
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
                <button onClick={() => setActiveTab('USER_DASHBOARD')} className="btn-gold">
                  <span>Return to Guest Portal</span>
                  <ArrowRight size={15} />
                </button>
                <button onClick={() => setActiveTab('LOGIN')} className="btn-outline-gold">
                  <Lock size={15} />
                  <span>Sign In as Staff</span>
                </button>
              </div>
            </div>
          )
        )}

        {/* Executive Admin Operations Portal (STRICTLY GUARDED) */}
        {effectiveTab === 'ADMIN' && (
          isAdmin ? (
            <AdminDashboard />
          ) : (
            <div style={{ maxWidth: '580px', margin: '80px auto', padding: '36px', textAlign: 'center' }} className="glass-panel">
              <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: 'rgba(239, 68, 68, 0.15)', border: '1px solid var(--danger)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
                <ShieldAlert size={36} color="var(--danger)" />
              </div>
              <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#fff', marginBottom: '8px' }}>
                Super Admin Access Restricted
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', lineHeight: 1.6, marginBottom: '24px' }}>
                Executive operations, revenue analytics, and telemetry require Super Administrator authorization. Your current account (<strong style={{ color: 'var(--gold-light)' }}>{user?.username || 'Guest'}</strong>) does not have Admin credentials.
              </p>
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
                <button onClick={() => setActiveTab('USER_DASHBOARD')} className="btn-gold">
                  <span>Return to Guest Portal</span>
                  <ArrowRight size={15} />
                </button>
                <button onClick={() => setActiveTab('LOGIN')} className="btn-outline-gold">
                  <Lock size={15} />
                  <span>Sign In as Admin</span>
                </button>
              </div>
            </div>
          )
        )}

        {/* Sovereign Authentication & Account Page */}
        {effectiveTab === 'LOGIN' && (
          <LoginPage onNavigate={(tab) => setActiveTab(tab)} />
        )}
      </main>

      {/* Room Booking Modal */}
      {selectedHotelForBooking && (
        <RoomBookingModal 
          hotel={selectedHotelForBooking} 
          onClose={() => setSelectedHotelForBooking(null)}
          onBookingSuccess={handleBookingSuccess}
        />
      )}

      {/* Luxury Footer with Architecture Stack Info */}
      <footer style={{
        background: 'rgba(11, 17, 32, 0.95)',
        borderTop: '1px solid var(--border-subtle)',
        padding: '36px 20px',
        color: 'var(--text-muted)',
        fontSize: '0.85rem'
      }}>
        <div style={{ maxWidth: '1400px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '18px' }}>
          
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
            <div 
              onClick={() => setActiveTab('HOME')}
              style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}
            >
              <Sparkles size={18} color="var(--gold-primary)" />
              <span style={{ fontWeight: 800, color: 'var(--gold-light)', letterSpacing: '0.05em' }}>
                GRAND LUXE HOSPITALITY MANAGEMENT SYSTEM
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '18px', fontSize: '0.78rem', flexWrap: 'wrap' }}>
              <span>Port :8080 (API Gateway)</span>
              <span>Port :8081 (Auth)</span>
              <span>Port :8083 (Hotels + Redis)</span>
              <span>Port :8085 (Bookings)</span>
              <span>Port :8086 (Food)</span>
              <span>Port :8087 (Room Service KOT)</span>
              <span>Port :8089 (Inventory)</span>
            </div>
          </div>

          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px',
            borderTop: '1px solid rgba(255, 255, 255, 0.05)',
            paddingTop: '18px',
            fontSize: '0.78rem',
            color: 'var(--text-dim)'
          }}>
            <div>
              High-concurrency microservices architecture with PostgreSQL 16 database-per-service isolation, Redis 7 caching, and Kafka KRaft event streams.
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
              <span style={{ color: 'var(--success)' }}>● Netty Reactive Gateway Active</span>
              <span style={{ color: 'var(--gold-primary)' }}>● Optimistic Version Locking Enforced</span>
            </div>
          </div>

        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
