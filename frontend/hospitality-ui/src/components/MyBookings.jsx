import React, { useState, useEffect } from 'react';
import { Calendar, CheckCircle2, Download, ShieldCheck, MapPin } from 'lucide-react';
import { api } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

const MyBookings = ({ bookings = [] }) => {
  const { user } = useAuth();
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

  const [selectedInvoice, setSelectedInvoice] = useState(null);

  useEffect(() => {
    const loadCustomerBookings = async () => {
      let customerId = user?.id || 1;
      try {
        if (user?.id) {
          const profile = await api.getCustomerByUserId(user.id);
          if (profile?.id) customerId = profile.id;
        }
      } catch {
        // use customerId
      }
      const data = await api.getBookingsByCustomer(customerId);
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

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '30px 20px 80px' }}>
      
      <div style={{ marginBottom: '32px' }}>
        <div className="badge badge-gold" style={{ marginBottom: '8px' }}>
          <Calendar size={12} /> Guest Folio & Reservations
        </div>
        <h1 style={{ fontSize: '2.2rem', fontWeight: 800, color: '#fff' }}>
          My Luxury Reservations
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Review your upcoming stays, suite allocations, and verified tax invoices.
        </p>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {activeBookings.map((b, idx) => (
          <div key={idx} className="glass-panel" style={{ padding: '24px 28px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px', marginBottom: '16px' }}>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--gold-light)', fontWeight: 700, letterSpacing: '0.08em' }}>
                  REFERENCE #{b.bookingNumber}
                </span>
                <h3 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#fff', marginTop: '2px' }}>
                  {b.hotelName}
                </h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--text-muted)', fontSize: '0.82rem', marginTop: '4px' }}>
                  <MapPin size={13} color="var(--gold-primary)" />
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

            {/* Details Row */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '16px',
              background: 'rgba(11, 17, 32, 0.5)',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
              marginBottom: '18px'
            }}>
              <div>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', display: 'block', textTransform: 'uppercase' }}>Suite Allocated</span>
                <strong style={{ color: '#fff', fontSize: '0.95rem' }}>Room {b.roomNumber} ({b.roomType})</strong>
              </div>
              <div>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', display: 'block', textTransform: 'uppercase' }}>Check-In</span>
                <strong style={{ color: '#fff', fontSize: '0.95rem' }}>{b.checkIn}</strong>
              </div>
              <div>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', display: 'block', textTransform: 'uppercase' }}>Check-Out</span>
                <strong style={{ color: '#fff', fontSize: '0.95rem' }}>{b.checkOut} ({b.nights} nights)</strong>
              </div>
              <div>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', display: 'block', textTransform: 'uppercase' }}>Total Invoiced</span>
                <strong style={{ color: 'var(--gold-light)', fontSize: '1.1rem' }}>₹{b.totalAmount?.toLocaleString('en-IN') || '29,500'}</strong>
              </div>
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--border-subtle)', paddingTop: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                <ShieldCheck size={15} color="var(--success)" />
                <span>Verified by Spring Cloud API Gateway (:8080) & Billing Service (:8088)</span>
              </div>

              <button 
                onClick={() => setSelectedInvoice(b)}
                className="btn-outline-gold" 
                style={{ padding: '7px 16px', fontSize: '0.82rem' }}
              >
                <Download size={14} /> View Tax Invoice
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Invoice Modal Preview */}
      {selectedInvoice && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '600px', padding: '32px' }}>
            <div style={{ textAlign: 'center', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '20px', marginBottom: '20px' }}>
              <div className="badge badge-gold" style={{ marginBottom: '8px' }}>TAX INVOICE / GUEST FOLIO</div>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#fff' }}>GRAND LUXE HOSPITALITY</h2>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>GSTIN: 27AABCG1234F1Z5 • FSSAI Lic. 11517012000341</p>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '14px' }}>
              <span style={{ color: 'var(--text-muted)' }}>Invoice No:</span>
              <strong style={{ color: '#fff' }}>INV-{selectedInvoice.bookingNumber}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '14px' }}>
              <span style={{ color: 'var(--text-muted)' }}>Guest Name:</span>
              <strong style={{ color: '#fff' }}>John Doe (Guest)</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '14px' }}>
              <span style={{ color: 'var(--text-muted)' }}>Property:</span>
              <strong style={{ color: '#fff' }}>{selectedInvoice.hotelName} (Room {selectedInvoice.roomNumber})</strong>
            </div>

            <div style={{ background: 'rgba(255,255,255,0.02)', padding: '16px', borderRadius: 'var(--radius-sm)', margin: '20px 0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '8px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Suite Tariff ({selectedInvoice.nights} Nights):</span>
                <span style={{ color: '#fff' }}>₹{(selectedInvoice.totalAmount / 1.18).toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '8px' }}>
                <span style={{ color: 'var(--text-muted)' }}>CGST (9%):</span>
                <span style={{ color: '#fff' }}>₹{((selectedInvoice.totalAmount / 1.18) * 0.09).toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '8px' }}>
                <span style={{ color: 'var(--text-muted)' }}>SGST (9%):</span>
                <span style={{ color: '#fff' }}>₹{((selectedInvoice.totalAmount / 1.18) * 0.09).toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.1rem', fontWeight: 800, color: 'var(--gold-light)', borderTop: '1px solid var(--border-subtle)', paddingTop: '10px' }}>
                <span>Total Paid:</span>
                <span>₹{selectedInvoice.totalAmount?.toLocaleString('en-IN') || '29,500'}</span>
              </div>
            </div>

            <div style={{ textAlign: 'center' }}>
              <button onClick={() => setSelectedInvoice(null)} className="btn-gold" style={{ padding: '8px 24px' }}>
                Close Receipt
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default MyBookings;
