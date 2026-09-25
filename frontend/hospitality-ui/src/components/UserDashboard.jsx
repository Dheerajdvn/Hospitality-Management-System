import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Crown, 
  Calendar, 
  MapPin, 
  CheckCircle2, 
  Clock, 
  Download, 
  UtensilsCrossed, 
  Bell, 
  Key, 
  Sparkles, 
  Car, 
  Luggage, 
  FileText,
  Building2,
  Receipt
} from 'lucide-react';
import { api } from '../api/apiClient';

const UserDashboard = ({ bookings = [], onNavigate }) => {
  const { user } = useAuth();
  const [activeSubTab, setActiveSubTab] = useState('STAYS'); // 'STAYS' | 'DINING' | 'INVOICES' | 'CONCIERGE'
  const [selectedInvoice, setSelectedInvoice] = useState(null);

  // Butler Concierge state
  const [conciergeRequests, setConciergeRequests] = useState([
    { id: 1, type: 'TURNDOWN', title: 'Evening Turndown Service', room: '101', status: 'COMPLETED', requestedAt: '19:30' },
    { id: 2, type: 'LINEN', title: 'Plush Egyptian Cotton Towels (x2)', room: '101', status: 'IN_TRANSIT', requestedAt: '20:15' }
  ]);
  const [conciergeNotice, setConciergeNotice] = useState('');

  // Active User Reservations
  const [activeBookings, setActiveBookings] = useState([
    {
      id: 1,
      bookingNumber: 'BK-20260925-1042',
      hotelName: 'The Grand Palace Resort & Spa',
      city: 'Mumbai',
      roomNumber: '101',
      roomType: 'DELUXE SUITE',
      checkIn: '2026-09-25',
      checkOut: '2026-09-27',
      nights: 2,
      totalAmount: 29500,
      status: 'CONFIRMED',
      paymentStatus: 'PAID',
    },
    ...bookings,
  ]);

  // In-Room Dining Orders for this Guest
  const [guestDiningOrders] = useState([
    {
      id: 101,
      orderNumber: 'RSO-20260925-4B47BB11',
      kotNumber: 'KOT-85249',
      roomNumber: '101',
      hotelName: 'The Grand Palace, Mumbai',
      status: 'PREPARING',
      totalAmount: 1657.5,
      items: [
        { name: 'Paneer Tikka Royale', quantity: 2, price: 350 },
        { name: 'Grand Butter Chicken', quantity: 1, price: 520 },
        { name: 'Garlic Butter Naan', quantity: 3, price: 110 }
      ],
      orderedAt: '15:33',
      eta: '12 mins'
    }
  ]);

  useEffect(() => {
    const loadCustomerBookings = async () => {
      const data = await api.getBookingsByCustomer(user?.id || 3);
      if (data && data.length > 0) {
        const mapped = data.map((b) => ({
          id: b.id,
          bookingNumber: b.bookingNumber,
          hotelName: b.hotelId === 2 ? 'Ocean View Coastal Retreat' : b.hotelId === 3 ? 'Himalayan Heritage Sanctuary' : 'The Grand Palace Resort & Spa',
          city: b.hotelId === 2 ? 'Goa' : b.hotelId === 3 ? 'Shimla' : 'Mumbai',
          roomNumber: b.roomId ? `${b.roomId}01` : '101',
          roomType: 'DELUXE SUITE',
          checkIn: b.checkInDate,
          checkOut: b.checkOutDate,
          nights: 2,
          totalAmount: b.totalAmount || 29500,
          status: b.status || 'CONFIRMED',
          paymentStatus: 'PAID',
        }));
        setActiveBookings(() => {
          const ids = new Set(mapped.map((m) => m.bookingNumber));
          const custom = (bookings || []).filter((b) => !ids.has(b.bookingNumber));
          return [...custom, ...mapped];
        });
      }
    };
    loadCustomerBookings();
  }, [bookings, user]);

  const handleRequestButler = (type, title) => {
    const newReq = {
      id: Date.now(),
      type,
      title,
      room: '101',
      status: 'DISPATCHED',
      requestedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    setConciergeRequests([newReq, ...conciergeRequests]);
    setConciergeNotice(`🛎️ Personal Butler dispatched to Suite 101 for: ${title}`);
    setTimeout(() => setConciergeNotice(''), 4500);
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '40px 20px 80px' }}>
      
      {/* =========================================================================
          GUEST PROFILE & SOVEREIGN STATUS BANNER
          ========================================================================= */}
      <div className="glass-panel" style={{
        padding: '30px',
        marginBottom: '32px',
        border: '1px solid var(--border-gold)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{
          position: 'absolute',
          top: '-60px',
          right: '-40px',
          width: '240px',
          height: '240px',
          background: 'radial-gradient(circle, rgba(212, 175, 55, 0.15) 0%, transparent 70%)',
          pointerEvents: 'none'
        }} />

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '20px' }}>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '18px' }}>
            <div style={{
              width: '64px',
              height: '64px',
              borderRadius: '16px',
              background: 'var(--gold-gradient)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: 'var(--gold-glow)'
            }}>
              <Crown size={36} color="#0B1120" />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--text-main)' }}>
                  Welcome, Sovereign {user?.username || 'Guest'}
                </h1>
                <span className="badge badge-gold">
                  👑 Diamond Patron
                </span>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '4px' }}>
                Active Folio • Suite 101 (The Grand Palace Mumbai) • Butler on Standby
              </p>
            </div>
          </div>

          {/* Quick Metrics Bar */}
          <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
            <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '10px 18px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Loyalty Points</div>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--gold-light)' }}>14,250 PTS</div>
            </div>
            <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '10px 18px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Digital Key</div>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Key size={16} /> Active
              </div>
            </div>
          </div>

        </div>

        {conciergeNotice && (
          <div style={{
            marginTop: '20px',
            padding: '12px 18px',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(212, 175, 55, 0.15)',
            border: '1px solid var(--gold-primary)',
            color: 'var(--gold-light)',
            fontSize: '0.88rem',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            animation: 'fadeIn 0.2s ease-out'
          }}>
            <Bell size={18} color="var(--gold-primary)" />
            <span>{conciergeNotice}</span>
          </div>
        )}
      </div>

      {/* =========================================================================
          SUB-TAB NAVIGATION
          ========================================================================= */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        borderBottom: '1px solid var(--border-subtle)',
        marginBottom: '28px',
        overflowX: 'auto',
        paddingBottom: '4px'
      }}>
        <button
          onClick={() => setActiveSubTab('STAYS')}
          className={activeSubTab === 'STAYS' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <Calendar size={16} />
          <span>My Reservations ({activeBookings.length})</span>
        </button>

        <button
          onClick={() => setActiveSubTab('DINING')}
          className={activeSubTab === 'DINING' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <UtensilsCrossed size={16} />
          <span>In-Room Dining Tracker ({guestDiningOrders.length})</span>
        </button>

        <button
          onClick={() => setActiveSubTab('CONCIERGE')}
          className={activeSubTab === 'CONCIERGE' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <Sparkles size={16} />
          <span>Personal Butler Dispatch</span>
        </button>

        <button
          onClick={() => setActiveSubTab('INVOICES')}
          className={activeSubTab === 'INVOICES' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <Receipt size={16} />
          <span>Folios & Tax Invoices</span>
        </button>
      </div>

      {/* =========================================================================
          TAB 1: RESERVATIONS & STAYS
          ========================================================================= */}
      {activeSubTab === 'STAYS' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)' }}>
              Active & Upcoming Sovereign Stays
            </h2>
            <button
              onClick={() => onNavigate('HOTELS')}
              className="btn-outline-gold"
              style={{ fontSize: '0.85rem' }}
            >
              <Building2 size={15} />
              <span>Book Another Suite</span>
            </button>
          </div>

          {activeBookings.map((b, idx) => (
            <div key={idx} className="glass-panel" style={{ padding: '26px 30px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px', marginBottom: '16px' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--gold-light)', fontWeight: 700, letterSpacing: '0.08em' }}>
                    REFERENCE #{b.bookingNumber}
                  </span>
                  <h3 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#fff', marginTop: '3px' }}>
                    {b.hotelName}
                  </h3>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.84rem', marginTop: '4px' }}>
                    <MapPin size={14} color="var(--gold-primary)" />
                    <span>{b.city || 'Mumbai'}</span>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span className="badge badge-success">
                    <CheckCircle2 size={12} /> {b.status}
                  </span>
                  <span className="badge badge-info">
                    {b.paymentStatus || 'PAID (18% GST)'}
                  </span>
                </div>
              </div>

              {/* Details Grid */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                gap: '16px',
                padding: '18px 0',
                borderTop: '1px solid var(--border-subtle)',
                borderBottom: '1px solid var(--border-subtle)',
                marginBottom: '18px'
              }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Suite Allocation</span>
                  <div style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', marginTop: '2px' }}>
                    Room {b.roomNumber} ({b.roomType})
                  </div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Check-In & Check-Out</span>
                  <div style={{ fontSize: '0.95rem', fontWeight: 600, color: '#fff', marginTop: '2px' }}>
                    {b.checkIn} → {b.checkOut}
                  </div>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Total Paid</span>
                  <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--gold-light)', marginTop: '2px' }}>
                    ₹{b.totalAmount.toLocaleString()}
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <button
                    onClick={() => handleRequestButler('LINEN', 'Fresh Royal Bed Linens')}
                    className="btn-outline-gold"
                    style={{ fontSize: '0.8rem', padding: '8px 14px' }}
                  >
                    <Sparkles size={14} /> Request Butler
                  </button>
                  <button
                    onClick={() => onNavigate('DINING')}
                    className="btn-outline-gold"
                    style={{ fontSize: '0.8rem', padding: '8px 14px' }}
                  >
                    <UtensilsCrossed size={14} /> Room Dining
                  </button>
                </div>

                <button
                  onClick={() => setSelectedInvoice(b)}
                  className="btn-gold"
                  style={{ fontSize: '0.8rem', padding: '8px 16px' }}
                >
                  <FileText size={14} /> View Tax Invoice
                </button>
              </div>

            </div>
          ))}
        </div>
      )}

      {/* =========================================================================
          TAB 2: IN-ROOM DINING TRACKER
          ========================================================================= */}
      {activeSubTab === 'DINING' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)' }}>
              Live In-Room Dining & Kitchen Status
            </h2>
            <button
              onClick={() => onNavigate('DINING')}
              className="btn-gold"
              style={{ fontSize: '0.85rem' }}
            >
              <UtensilsCrossed size={15} />
              <span>Order More Cuisine</span>
            </button>
          </div>

          {guestDiningOrders.map((order) => (
            <div key={order.id} className="glass-panel" style={{ padding: '26px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px', marginBottom: '18px' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--gold-light)', fontWeight: 700 }}>
                    ORDER #{order.orderNumber} • KOT #{order.kotNumber}
                  </span>
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff', marginTop: '2px' }}>
                    Suite {order.roomNumber} ({order.hotelName})
                  </h3>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span className="badge badge-warning" style={{ fontSize: '0.82rem' }}>
                    <Clock size={13} /> {order.status} (ETA ~{order.eta})
                  </span>
                  <span style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                    ₹{order.totalAmount.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Progress Tracker */}
              <div style={{
                background: 'rgba(255, 255, 255, 0.05)',
                borderRadius: 'var(--radius-full)',
                height: '8px',
                width: '100%',
                marginBottom: '20px',
                overflow: 'hidden'
              }}>
                <div style={{
                  background: 'var(--gold-gradient)',
                  height: '100%',
                  width: order.status === 'ORDERED' ? '25%' : order.status === 'PREPARING' ? '65%' : '100%',
                  transition: 'width 0.4s ease'
                }} />
              </div>

              {/* Items List */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
                {order.items.map((item, i) => (
                  <div key={i} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', color: 'var(--text-muted)' }}>
                    <span>{item.quantity}x {item.name}</span>
                    <span style={{ color: '#fff' }}>₹{item.price * item.quantity}</span>
                  </div>
                ))}
              </div>

              <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)', borderTop: '1px solid var(--border-subtle)', paddingTop: '12px' }}>
                Ordered at {order.orderedAt} • Dedicated room service butler will ring suite doorbell upon arrival.
              </div>
            </div>
          ))}
        </div>
      )}

      {/* =========================================================================
          TAB 3: PERSONAL BUTLER CONCIERGE
          ========================================================================= */}
      {activeSubTab === 'CONCIERGE' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '6px' }}>
              Instant Personal Butler Concierge
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              One-click butler dispatch for your Suite 101. Requests are broadcast directly to floor attendants.
            </p>
          </div>

          {/* Quick Request Dispatch Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
            <button
              onClick={() => handleRequestButler('LINEN', 'Fresh Royal Towels & Pillows')}
              className="glass-panel"
              style={{ padding: '20px', textAlign: 'left', cursor: 'pointer', transition: 'all 0.2s ease' }}
            >
              <Sparkles size={24} color="var(--gold-primary)" style={{ marginBottom: '12px' }} />
              <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                Linens & Towels
              </h4>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Request extra Egyptian cotton towels and goose down pillows.
              </p>
            </button>

            <button
              onClick={() => handleRequestButler('TURNDOWN', 'Royal Evening Turndown')}
              className="glass-panel"
              style={{ padding: '20px', textAlign: 'left', cursor: 'pointer', transition: 'all 0.2s ease' }}
            >
              <Clock size={24} color="var(--gold-primary)" style={{ marginBottom: '12px' }} />
              <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                Turndown Service
              </h4>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Essential oils aromatherapy, linen refresh, and bedtime tea.
              </p>
            </button>

            <button
              onClick={() => handleRequestButler('LUGGAGE', 'Luggage Assistance & Chauffeur')}
              className="glass-panel"
              style={{ padding: '20px', textAlign: 'left', cursor: 'pointer', transition: 'all 0.2s ease' }}
            >
              <Luggage size={24} color="var(--gold-primary)" style={{ marginBottom: '12px' }} />
              <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                Luggage Assistance
              </h4>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Porter dispatch for luggage transfer or checkout departure.
              </p>
            </button>

            <button
              onClick={() => handleRequestButler('VALET', 'Rolls-Royce Valet Delivery')}
              className="glass-panel"
              style={{ padding: '20px', textAlign: 'left', cursor: 'pointer', transition: 'all 0.2s ease' }}
            >
              <Car size={24} color="var(--gold-primary)" style={{ marginBottom: '12px' }} />
              <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                Valet Chauffeur
              </h4>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Have your vehicle brought to the grand hotel portico.
              </p>
            </button>
          </div>

          {/* Active Concierge Requests Log */}
          <div className="glass-panel" style={{ padding: '24px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--gold-light)', marginBottom: '16px' }}>
              Active Concierge Dispatches
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {conciergeRequests.map((req) => (
                <div key={req.id} style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '12px 16px',
                  background: 'rgba(255, 255, 255, 0.03)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)'
                }}>
                  <div>
                    <div style={{ fontSize: '0.92rem', fontWeight: 700, color: '#fff' }}>
                      {req.title}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                      Suite {req.room} • Requested at {req.requestedAt}
                    </div>
                  </div>

                  <span className={req.status === 'COMPLETED' ? 'badge badge-success' : 'badge badge-warning'}>
                    {req.status}
                  </span>
                </div>
              ))}
            </div>
          </div>

        </div>
      )}

      {/* =========================================================================
          TAB 4: FOLIOS & TAX INVOICES
          ========================================================================= */}
      {activeSubTab === 'INVOICES' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '6px' }}>
              Verified Guest Folios & Tax Receipts
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              GST compliant invoices with full breakdown of suite tariffs, food & beverage, and taxes.
            </p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {activeBookings.map((b, idx) => (
              <div key={idx} className="glass-panel" style={{
                padding: '20px 24px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '16px'
              }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--gold-light)', fontWeight: 700 }}>
                    TAX INVOICE #INV-20260925-{b.id}04
                  </span>
                  <h4 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#fff', marginTop: '2px' }}>
                    {b.hotelName} ({b.checkIn} to {b.checkOut})
                  </h4>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                    Suite {b.roomNumber} • Payment Status: {b.paymentStatus}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '18px' }}>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>Total Amount</div>
                    <div style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                      ₹{b.totalAmount.toLocaleString()}
                    </div>
                  </div>

                  <button
                    onClick={() => setSelectedInvoice(b)}
                    className="btn-gold"
                    style={{ padding: '8px 16px', fontSize: '0.82rem' }}
                  >
                    <Download size={14} /> Download Folio
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* =========================================================================
          TAX INVOICE MODAL
          ========================================================================= */}
      {selectedInvoice && (
        <div className="modal-overlay" onClick={() => setSelectedInvoice(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '640px', padding: '36px' }}>
            
            {/* Modal Header */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border-gold)', paddingBottom: '16px', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Crown size={24} color="var(--gold-primary)" />
                <div>
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                    GRAND LUXE HOSPITALITY
                  </h3>
                  <p style={{ fontSize: '0.7rem', color: 'var(--text-dim)', letterSpacing: '0.08em' }}>
                    OFFICIAL GST TAX INVOICE • GSTIN: 27AAACG0123M1Z5
                  </p>
                </div>
              </div>
              <button onClick={() => setSelectedInvoice(null)} className="btn-ghost" style={{ fontSize: '1.2rem', color: 'var(--text-muted)' }}>
                ✕
              </button>
            </div>

            {/* Bill To & Invoice Info */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', fontSize: '0.82rem', marginBottom: '20px' }}>
              <div>
                <span style={{ color: 'var(--text-dim)', textTransform: 'uppercase' }}>Billed To:</span>
                <div style={{ fontWeight: 700, color: '#fff', fontSize: '0.95rem' }}>{user?.username || 'Lord Alexander'}</div>
                <div style={{ color: 'var(--text-muted)' }}>{user?.email || 'customer@hospitality.com'}</div>
                <div style={{ color: 'var(--text-muted)' }}>Suite {selectedInvoice.roomNumber}, {selectedInvoice.hotelName}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ color: 'var(--text-dim)', textTransform: 'uppercase' }}>Invoice No:</span>
                <div style={{ fontWeight: 700, color: 'var(--gold-light)' }}>INV-20260925-{selectedInvoice.id}</div>
                <div style={{ color: 'var(--text-muted)' }}>Date: {new Date().toLocaleDateString()}</div>
                <div style={{ color: 'var(--success)', fontWeight: 600 }}>PAID (Verified)</div>
              </div>
            </div>

            {/* Breakdown Table */}
            <div style={{
              background: 'rgba(0, 0, 0, 0.25)',
              borderRadius: 'var(--radius-md)',
              padding: '16px',
              border: '1px solid var(--border-subtle)',
              marginBottom: '20px'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)', fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                <span>Item & Description</span>
                <span>Amount (INR)</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', fontSize: '0.86rem' }}>
                <span>Suite Tariff (2 Nights @ ₹12,500)</span>
                <span>₹25,000.00</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', fontSize: '0.86rem' }}>
                <span>CGST (9%)</span>
                <span>₹2,250.00</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', fontSize: '0.86rem' }}>
                <span>SGST (9%)</span>
                <span>₹2,250.00</span>
              </div>
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                paddingTop: '12px',
                marginTop: '8px',
                borderTop: '1px solid var(--border-gold)',
                fontSize: '1.1rem',
                fontWeight: 800,
                color: 'var(--gold-light)'
              }}>
                <span>Total Amount Paid</span>
                <span>₹{selectedInvoice.totalAmount.toLocaleString()}.00</span>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                onClick={() => {
                  window.print();
                }}
                className="btn-gold"
                style={{ fontSize: '0.88rem' }}
              >
                <Download size={16} /> Print / Save PDF Receipt
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
};

export default UserDashboard;
