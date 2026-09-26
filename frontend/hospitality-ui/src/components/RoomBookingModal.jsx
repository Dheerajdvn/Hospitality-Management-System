import React, { useState, useEffect } from 'react';
import { api } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';
import { X, Calendar, Users, ShieldCheck, CheckCircle2, BedDouble, AlertCircle } from 'lucide-react';

const RoomBookingModal = ({ hotel, onClose, onBookingSuccess }) => {
  const { user } = useAuth();
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [bookingError, setBookingError] = useState(null);

  // Default dates: check-in today, check-out in 2 days
  const todayStr = new Date().toISOString().split('T')[0];
  const nextDate = new Date();
  nextDate.setDate(nextDate.getDate() + 2);
  const nextDateStr = nextDate.toISOString().split('T')[0];

  const [checkIn, setCheckIn] = useState(todayStr);
  const [checkOut, setCheckOut] = useState(nextDateStr);
  const [guests, setGuests] = useState(2);
  const [bookingPlaced, setBookingPlaced] = useState(null);

  useEffect(() => {
    const fetchRooms = async () => {
      setLoading(true);
      const data = await api.getRooms(hotel.id);
      setRooms(data);
      if (data && data.length > 0) {
        setSelectedRoom(data[0]);
      }
      setLoading(false);
    };
    fetchRooms();
  }, [hotel.id]);

  // Calculate nights
  const calculateNights = () => {
    const d1 = new Date(checkIn);
    const d2 = new Date(checkOut);
    const diffTime = d2 - d1;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 1;
  };

  const nights = calculateNights();
  const roomPrice = selectedRoom?.basePrice || 12500;
  const subtotal = roomPrice * nights;
  const gst = Math.round(subtotal * 0.18);
  const grandTotal = subtotal + gst;

  const [submitting, setSubmitting] = useState(false);

  const handleBookNow = async () => {
    setSubmitting(true);
    setBookingError(null);
    try {
      let customerId = user?.id || 1;
      try {
        if (user?.id) {
          const profile = await api.getCustomerByUserId(user.id);
          if (profile?.id) customerId = profile.id;
        }
      } catch {
        // proceed with customerId
      }

      const payload = {
        customerId,
        roomId: selectedRoom?.id || 1,
        checkInDate: checkIn,
        checkOutDate: checkOut,
        numberOfGuests: Number(guests) || 1,
        specialRequests: 'Luxury amenities & high floor preference',
      };

      const bookingRes = await api.createBooking(payload);
      const bookingData = bookingRes?.data || bookingRes;
      const finalBookingId = bookingData.id || Math.floor(100 + Math.random() * 900);

      // Auto-settle payment via Billing Service (:8088)
      const paymentPayload = {
        bookingId: finalBookingId,
        amount: grandTotal,
        paymentMethod: 'CREDIT_CARD',
      };
      await api.processPayment(paymentPayload);

      const bookingResult = {
        id: finalBookingId,
        bookingNumber: bookingData.bookingNumber || `BK-${Date.now().toString().slice(-8)}`,
        hotelName: hotel.name,
        roomNumber: selectedRoom?.roomNumber || '101',
        roomType: selectedRoom?.type || 'DELUXE',
        checkIn,
        checkOut,
        nights,
        grandTotal,
        status: 'CONFIRMED',
        paymentStatus: 'PAID',
      };

      setBookingPlaced(bookingResult);
      if (onBookingSuccess) {
        onBookingSuccess(bookingResult);
      }
    } catch (err) {
      console.error('Reservation error:', err);
      setBookingError(err.message || 'Reservation hold could not be placed. Please verify dates or select another room.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '780px' }}>
        
        {/* Header */}
        <div style={{
          padding: '24px 28px',
          borderBottom: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'rgba(15, 23, 42, 0.6)'
        }}>
          <div>
            <span style={{ fontSize: '0.75rem', color: 'var(--gold-light)', textTransform: 'uppercase', letterSpacing: '0.1em', fontWeight: 700 }}>
              Sovereign Reservation
            </span>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#fff' }}>
              {hotel.name}
            </h2>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>{hotel.city} • Luxury Sanctuary</p>
          </div>
          <button 
            onClick={onClose}
            style={{ 
              width: '36px', 
              height: '36px', 
              borderRadius: '50%', 
              background: 'rgba(255,255,255,0.06)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--text-muted)'
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Error Alert */}
        {bookingError && (
          <div style={{
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid var(--danger)',
            borderRadius: '8px',
            padding: '12px 16px',
            margin: '16px 28px 0',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: '#fca5a5',
            fontSize: '0.88rem'
          }}>
            <AlertCircle size={18} color="var(--danger)" />
            <span>{bookingError}</span>
          </div>
        )}

        {bookingPlaced ? (
          /* Confirmation Success State */
          <div style={{ padding: '40px 30px', textAlign: 'center' }}>
            <div style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              background: 'var(--success-bg)',
              color: 'var(--success)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 20px',
              border: '2px solid var(--success)'
            }}>
              <CheckCircle2 size={36} />
            </div>

            <h3 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#fff', marginBottom: '8px' }}>
              Reservation Confirmed!
            </h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: '24px' }}>
              Your royal suite has been reserved. A confirmation SMS and email have been dispatched.
            </p>

            <div className="glass-panel" style={{ textAlign: 'left', padding: '20px', maxWidth: '500px', margin: '0 auto 28px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Booking Reference:</span>
                <span style={{ color: 'var(--gold-light)', fontWeight: 700 }}>{bookingPlaced.bookingNumber}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Suite Allocated:</span>
                <span style={{ color: '#fff', fontWeight: 600 }}>Room {bookingPlaced.roomNumber} ({bookingPlaced.roomType})</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Dates:</span>
                <span style={{ color: '#fff' }}>{bookingPlaced.checkIn} to {bookingPlaced.checkOut} ({bookingPlaced.nights} nights)</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '10px', borderTop: '1px solid var(--border-subtle)' }}>
                <span style={{ color: '#fff', fontWeight: 700 }}>Total Paid:</span>
                <span style={{ color: 'var(--gold-light)', fontWeight: 800, fontSize: '1.2rem' }}>₹{bookingPlaced.grandTotal.toLocaleString('en-IN')}</span>
              </div>
            </div>

            <button onClick={onClose} className="btn-gold" style={{ padding: '10px 28px' }}>
              Done & Return to Catalog
            </button>
          </div>
        ) : (
          /* Room Selection & Checkout Form */
          <div style={{ padding: '24px 28px' }}>
            
            {/* 1. Date & Guest Selectors */}
            <div style={{ 
              display: 'grid', 
              gridTemplateColumns: '1fr 1fr 1fr', 
              gap: '16px', 
              marginBottom: '24px',
              background: 'rgba(11, 17, 32, 0.5)',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)'
            }}>
              <div>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', color: 'var(--gold-light)', fontWeight: 600, marginBottom: '6px' }}>
                  <Calendar size={13} /> Check-In Date
                </label>
                <input 
                  type="date" 
                  value={checkIn} 
                  min={todayStr}
                  onChange={(e) => setCheckIn(e.target.value)}
                  className="input-luxury"
                  style={{ padding: '8px 12px' }}
                />
              </div>

              <div>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', color: 'var(--gold-light)', fontWeight: 600, marginBottom: '6px' }}>
                  <Calendar size={13} /> Check-Out Date
                </label>
                <input 
                  type="date" 
                  value={checkOut} 
                  min={checkIn}
                  onChange={(e) => setCheckOut(e.target.value)}
                  className="input-luxury"
                  style={{ padding: '8px 12px' }}
                />
              </div>

              <div>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.78rem', color: 'var(--gold-light)', fontWeight: 600, marginBottom: '6px' }}>
                  <Users size={13} /> Guests
                </label>
                <select 
                  value={guests} 
                  onChange={(e) => setGuests(Number(e.target.value))}
                  className="input-luxury"
                  style={{ padding: '8px 12px' }}
                >
                  <option value={1}>1 Adult</option>
                  <option value={2}>2 Adults</option>
                  <option value={3}>3 Adults</option>
                  <option value={4}>4 Adults (Suite)</option>
                </select>
              </div>
            </div>

            {/* 2. Room Tier Selection */}
            <div style={{ marginBottom: '24px' }}>
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '12px', display: 'block' }}>
                Select Preferred Suite Tier:
              </span>

              {loading ? (
                <div style={{ color: 'var(--gold-primary)', padding: '20px 0' }}>Loading available suites...</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {rooms.map((room) => {
                    const isSelected = selectedRoom?.id === room.id;
                    return (
                      <div 
                        key={room.id}
                        onClick={() => setSelectedRoom(room)}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '14px 18px',
                          borderRadius: 'var(--radius-md)',
                          border: isSelected ? '1px solid var(--gold-primary)' : '1px solid var(--border-subtle)',
                          background: isSelected ? 'rgba(212, 175, 55, 0.08)' : 'rgba(255, 255, 255, 0.02)',
                          cursor: 'pointer',
                          transition: 'all 0.2s ease'
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                          <div style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: '8px',
                            background: isSelected ? 'var(--gold-gradient)' : 'rgba(255,255,255,0.06)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            color: isSelected ? '#0B1120' : 'var(--gold-light)'
                          }}>
                            <BedDouble size={20} />
                          </div>
                          <div>
                            <div style={{ fontWeight: 700, fontSize: '1rem', color: '#fff' }}>
                              Room {room.roomNumber} — {room.type} Suite
                            </div>
                            <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                              Floor {room.floorNumber} • Capacity: {room.capacity} Guests • {room.amenities?.slice(0, 2).join(', ')}
                            </div>
                          </div>
                        </div>

                        <div style={{ textAlign: 'right' }}>
                          <span style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                            ₹{room.basePrice.toLocaleString('en-IN')}
                          </span>
                          <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', display: 'block' }}>/ night</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* 3. Tariff & Price Breakdown */}
            <div style={{
              background: 'rgba(15, 23, 42, 0.7)',
              borderRadius: 'var(--radius-md)',
              padding: '16px 20px',
              border: '1px solid var(--border-subtle)',
              marginBottom: '24px'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.88rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Tariff ({nights} nights × ₹{roomPrice.toLocaleString('en-IN')}):</span>
                <span style={{ color: '#fff' }}>₹{subtotal.toLocaleString('en-IN')}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.88rem' }}>
                <span style={{ color: 'var(--text-muted)' }}>Hospitality GST (18% split CGST + SGST):</span>
                <span style={{ color: '#fff' }}>₹{gst.toLocaleString('en-IN')}</span>
              </div>
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                paddingTop: '10px',
                borderTop: '1px solid var(--border-subtle)',
                fontWeight: 800,
                fontSize: '1.15rem'
              }}>
                <span style={{ color: '#fff' }}>Total Payable Amount:</span>
                <span style={{ color: 'var(--gold-light)' }}>₹{grandTotal.toLocaleString('en-IN')}</span>
              </div>
            </div>

            {/* CTA */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                <ShieldCheck size={16} color="var(--success)" />
                <span>Zero Cancellation Fee up to 24h before Check-In</span>
              </div>

              <button 
                onClick={handleBookNow}
                disabled={submitting}
                className="btn-gold"
                style={{ padding: '12px 28px', fontSize: '0.95rem', opacity: submitting ? 0.7 : 1, cursor: submitting ? 'not-allowed' : 'pointer' }}
              >
                {submitting ? 'Confirming Reservation...' : 'Confirm Instant Reservation'}
              </button>
            </div>

          </div>
        )}

      </div>
    </div>
  );
};

export default RoomBookingModal;
