import React, { useState } from 'react';
import { 
  Crown, 
  MapPin, 
  Star, 
  Sparkles, 
  ArrowRight, 
  ShieldCheck, 
  UtensilsCrossed, 
  Building2, 
  Users, 
  Calendar, 
  Clock, 
  ChevronRight,
  Zap
} from 'lucide-react';
import { MOCK_HOTELS, MOCK_FOOD_ITEMS } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

const LandingPage = ({ onNavigate, onSelectHotel }) => {
  const { isStaff, isAdmin } = useAuth();
  const [selectedDestination, setSelectedDestination] = useState('ALL');
  const [searchDates, setSearchDates] = useState({
    checkIn: '2026-09-26',
    checkOut: '2026-09-28'
  });
  const [guestCount, setGuestCount] = useState('2');

  const handleSearchSuites = (e) => {
    e.preventDefault();
    if (onNavigate) {
      onNavigate('HOTELS');
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-main)', color: 'var(--text-main)', overflowX: 'hidden' }}>
      
      {/* =========================================================================
          HERO SECTION WITH LUXURY BACKDROP & QUICK BOOKING BAR
          ========================================================================= */}
      <section style={{
        position: 'relative',
        padding: '70px 20px 100px',
        background: 'radial-gradient(ellipse at 50% 20%, rgba(212, 175, 55, 0.12) 0%, rgba(11, 17, 32, 0.98) 70%)',
        borderBottom: '1px solid var(--border-gold)',
        overflow: 'hidden'
      }}>
        {/* Shimmer background accents */}
        <div style={{
          position: 'absolute',
          top: '-120px',
          left: '50%',
          transform: 'translateX(-50%)',
          width: '700px',
          height: '400px',
          background: 'radial-gradient(circle, rgba(212, 175, 55, 0.15) 0%, transparent 70%)',
          filter: 'blur(50px)',
          pointerEvents: 'none'
        }} />

        <div style={{ maxWidth: '1280px', margin: '0 auto', textAlign: 'center', position: 'relative', zIndex: 2 }}>
          
          {/* Royal Seal Badge */}
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '6px 18px',
            borderRadius: 'var(--radius-full)',
            background: 'rgba(212, 175, 55, 0.12)',
            border: '1px solid var(--border-gold)',
            marginBottom: '22px',
            boxShadow: 'var(--gold-glow)'
          }}>
            <Crown size={18} color="var(--gold-primary)" />
            <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--gold-light)', letterSpacing: '0.14em', textTransform: 'uppercase' }}>
              ROYAL HERITAGE & MODERN REFINEMENT
            </span>
          </div>

          <h1 style={{
            fontSize: 'clamp(2.5rem, 5.5vw, 4.2rem)',
            fontWeight: 800,
            lineHeight: 1.15,
            letterSpacing: '-0.02em',
            color: 'var(--text-main)',
            maxWidth: '920px',
            margin: '0 auto 20px'
          }}>
            Where Timeless Grandeur Meets <span style={{
              background: 'var(--gold-gradient)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              display: 'inline-block'
            }}>Unrivaled Luxury</span>
          </h1>

          <p style={{
            fontSize: 'clamp(1rem, 1.8vw, 1.25rem)',
            color: 'var(--text-muted)',
            maxWidth: '780px',
            margin: '0 auto 36px',
            lineHeight: 1.6
          }}>
            Experience bespoke 5-star hospitality across India's most prestigious sanctuaries. Indulge in royal suite living, 24/7 personal butler care, and Michelin-caliber in-room gastronomy.
          </p>

          {/* Quick Action Navigation CTAs */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px', flexWrap: 'wrap', marginBottom: '50px' }}>
            <button
              onClick={() => onNavigate('HOTELS')}
              className="btn-gold"
              style={{ padding: '14px 32px', fontSize: '1.02rem', fontWeight: 700 }}
            >
              <Building2 size={20} />
              <span>Explore Royal Sanctuaries</span>
              <ArrowRight size={18} />
            </button>
            <button
              onClick={() => onNavigate('DINING')}
              className="btn-outline-gold"
              style={{ padding: '14px 28px', fontSize: '1.02rem' }}
            >
              <UtensilsCrossed size={20} />
              <span>In-Room Royal Dining</span>
            </button>
            <button
              onClick={() => onNavigate('LOGIN')}
              className="btn-ghost"
              style={{ padding: '14px 24px', fontSize: '1rem', color: 'var(--gold-light)' }}
            >
              <Crown size={18} />
              <span>Sovereign Sign In</span>
            </button>
          </div>

          {/* Luxury Reservation Quick Search Bar */}
          <div className="glass-panel" style={{
            padding: '24px 28px',
            maxWidth: '1060px',
            margin: '0 auto',
            border: '1px solid var(--border-gold)',
            boxShadow: 'var(--shadow-lg), var(--gold-glow)'
          }}>
            <form onSubmit={handleSearchSuites} style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '16px',
              alignItems: 'end'
            }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--gold-light)', textTransform: 'uppercase', marginBottom: '8px', textAlign: 'left' }}>
                  <MapPin size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: '-2px' }} />
                  Destination Sanctuary
                </label>
                <select
                  value={selectedDestination}
                  onChange={(e) => setSelectedDestination(e.target.value)}
                  className="input-luxury"
                  style={{ background: 'rgba(15, 23, 42, 0.9)', cursor: 'pointer' }}
                >
                  <option value="ALL">All Sanctuaries (Mumbai, Goa, Shimla)</option>
                  <option value="1">The Grand Palace • Mumbai</option>
                  <option value="2">Ocean View Coastal Retreat • Goa</option>
                  <option value="3">Himalayan Heritage • Shimla</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--gold-light)', textTransform: 'uppercase', marginBottom: '8px', textAlign: 'left' }}>
                  <Calendar size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: '-2px' }} />
                  Check-In Date
                </label>
                <input
                  type="date"
                  value={searchDates.checkIn}
                  onChange={(e) => setSearchDates({ ...searchDates, checkIn: e.target.value })}
                  className="input-luxury"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--gold-light)', textTransform: 'uppercase', marginBottom: '8px', textAlign: 'left' }}>
                  <Calendar size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: '-2px' }} />
                  Check-Out Date
                </label>
                <input
                  type="date"
                  value={searchDates.checkOut}
                  onChange={(e) => setSearchDates({ ...searchDates, checkOut: e.target.value })}
                  className="input-luxury"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--gold-light)', textTransform: 'uppercase', marginBottom: '8px', textAlign: 'left' }}>
                  <Users size={14} style={{ display: 'inline', marginRight: '4px', verticalAlign: '-2px' }} />
                  Guests & Suite
                </label>
                <select
                  value={guestCount}
                  onChange={(e) => setGuestCount(e.target.value)}
                  className="input-luxury"
                  style={{ background: 'rgba(15, 23, 42, 0.9)', cursor: 'pointer' }}
                >
                  <option value="1">1 Sovereign Guest</option>
                  <option value="2">2 Guests (King Suite)</option>
                  <option value="3">3 Guests (Deluxe Suite)</option>
                  <option value="4">4+ Guests (Presidential)</option>
                </select>
              </div>

              <div>
                <button
                  type="submit"
                  className="btn-gold"
                  style={{
                    width: '100%',
                    justifyContent: 'center',
                    padding: '13px',
                    fontSize: '0.96rem',
                    fontWeight: 700
                  }}
                >
                  <Sparkles size={17} />
                  <span>Check Availability</span>
                </button>
              </div>
            </form>
          </div>

        </div>
      </section>


      {/* =========================================================================
          SIGNATURE PILLARS (THE GRAND LUXE TOUCH)
          ========================================================================= */}
      <section style={{ padding: '80px 20px', maxWidth: '1280px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '48px' }}>
          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--gold-primary)', letterSpacing: '0.12em', textTransform: 'uppercase' }}>
            Unrivaled Hospitality Pillars
          </span>
          <h2 style={{ fontSize: '2.4rem', fontWeight: 800, marginTop: '8px' }}>
            The Sovereign Standard of Living
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '1rem', maxWidth: '600px', margin: '10px auto 0' }}>
            Every moment curated with meticulous attention to detail, heritage charm, and cutting-edge digital service.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '24px' }}>
          
          <div className="glass-panel" style={{ padding: '30px', textAlign: 'left' }}>
            <div style={{
              width: '50px',
              height: '50px',
              borderRadius: '12px',
              background: 'rgba(212, 175, 55, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '20px',
              border: '1px solid var(--border-gold)'
            }}>
              <Crown size={26} color="var(--gold-primary)" />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '10px', color: 'var(--text-main)' }}>
              24/7 Sovereign Butler
            </h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
              A dedicated personal butler anticipates your every comfort, from unpacking luggage and garment steaming to bespoke itinerary planning.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '30px', textAlign: 'left' }}>
            <div style={{
              width: '50px',
              height: '50px',
              borderRadius: '12px',
              background: 'rgba(212, 175, 55, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '20px',
              border: '1px solid var(--border-gold)'
            }}>
              <UtensilsCrossed size={26} color="var(--gold-primary)" />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '10px', color: 'var(--text-main)' }}>
              Michelin-Caliber Dining
            </h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
              Savor slow-simmered Nawabi recipes, charcoal tandoori delicacies, and international pastries crafted by renowned master chefs.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '30px', textAlign: 'left' }}>
            <div style={{
              width: '50px',
              height: '50px',
              borderRadius: '12px',
              background: 'rgba(212, 175, 55, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '20px',
              border: '1px solid var(--border-gold)'
            }}>
              <Sparkles size={26} color="var(--gold-primary)" />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '10px', color: 'var(--text-main)' }}>
              Ayurvedic Spa & Jacuzzi
            </h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
              Revitalize with signature botanical treatments, Himalayan cedar-wood saunas, and private oceanfront temperature-controlled jacuzzis.
            </p>
          </div>

          <div className="glass-panel" style={{ padding: '30px', textAlign: 'left' }}>
            <div style={{
              width: '50px',
              height: '50px',
              borderRadius: '12px',
              background: 'rgba(212, 175, 55, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '20px',
              border: '1px solid var(--border-gold)'
            }}>
              <Zap size={26} color="var(--gold-primary)" />
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '10px', color: 'var(--text-main)' }}>
              Instant Concierge Dispatch
            </h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
              One-touch digital dispatch for fresh linens, luggage transport, airport Rolls-Royce transfers, and rooftop helipad reception.
            </p>
          </div>

        </div>
      </section>


      {/* =========================================================================
          FEATURED SOVEREIGN SANCTUARIES (HOTELS SHOWCASE)
          ========================================================================= */}
      <section style={{
        padding: '80px 20px',
        background: 'rgba(19, 29, 51, 0.45)',
        borderTop: '1px solid var(--border-subtle)',
        borderBottom: '1px solid var(--border-subtle)'
      }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
          
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', flexWrap: 'wrap', gap: '20px', marginBottom: '40px' }}>
            <div>
              <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--gold-primary)', letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                Flagship Destinations
              </span>
              <h2 style={{ fontSize: '2.4rem', fontWeight: 800, marginTop: '8px' }}>
                Explore Our Royal Sanctuaries
              </h2>
            </div>
            <button
              onClick={() => onNavigate('HOTELS')}
              className="btn-outline-gold"
              style={{ gap: '8px' }}
            >
              <span>View All 3 Sanctuaries</span>
              <ArrowRight size={16} />
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '30px' }}>
            {MOCK_HOTELS.map((hotel) => (
              <div key={hotel.id} className="glass-panel" style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                
                {/* Hotel Image with Star Rating & City Badge */}
                <div style={{ position: 'relative', height: '220px', overflow: 'hidden' }}>
                  <img
                    src={hotel.imageUrl}
                    alt={hotel.name}
                    style={{ width: '100%', height: '100%', objectFit: 'cover', transition: 'transform 0.5s ease' }}
                    onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
                    onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                  />
                  <div style={{ position: 'absolute', top: '14px', right: '14px', background: 'rgba(11, 17, 32, 0.85)', backdropFilter: 'blur(8px)', padding: '4px 10px', borderRadius: 'var(--radius-full)', border: '1px solid var(--border-gold)', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.8rem', fontWeight: 700, color: 'var(--gold-light)' }}>
                    <Star size={13} fill="var(--gold-primary)" color="var(--gold-primary)" />
                    <span>{hotel.starRating}</span>
                  </div>
                  <div style={{ position: 'absolute', bottom: '14px', left: '14px', background: 'rgba(11, 17, 32, 0.85)', backdropFilter: 'blur(8px)', padding: '4px 10px', borderRadius: 'var(--radius-full)', border: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.78rem', color: '#fff' }}>
                    <MapPin size={12} color="var(--gold-primary)" />
                    <span>{hotel.city}</span>
                  </div>
                </div>

                {/* Content */}
                <div style={{ padding: '24px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <h3 style={{ fontSize: '1.3rem', fontWeight: 800, color: '#fff', marginBottom: '8px' }}>
                      {hotel.name}
                    </h3>
                    <p style={{ color: 'var(--text-muted)', fontSize: '0.86rem', lineHeight: 1.5, marginBottom: '16px' }}>
                      {hotel.description}
                    </p>

                    {/* Amenities tags */}
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '20px' }}>
                      {hotel.amenities.slice(0, 4).map((amenity, idx) => (
                        <span key={idx} style={{
                          fontSize: '0.74rem',
                          padding: '3px 9px',
                          borderRadius: 'var(--radius-full)',
                          background: 'rgba(255, 255, 255, 0.05)',
                          border: '1px solid var(--border-subtle)',
                          color: 'var(--text-dim)'
                        }}>
                          {amenity}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '16px', borderTop: '1px solid var(--border-subtle)' }}>
                    <div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Starting from</div>
                      <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                        ₹{hotel.startingPrice.toLocaleString()} <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 400 }}>/ night</span>
                      </div>
                    </div>

                    <button
                      onClick={() => {
                        if (onSelectHotel) onSelectHotel(hotel);
                        else if (onNavigate) onNavigate('HOTELS');
                      }}
                      className="btn-gold"
                      style={{ padding: '8px 18px', fontSize: '0.85rem' }}
                    >
                      <span>Reserve Suite</span>
                      <ArrowRight size={14} />
                    </button>
                  </div>

                </div>

              </div>
            ))}
          </div>

        </div>
      </section>


      {/* =========================================================================
          GASTRONOMY HIGHLIGHTS (ROYAL IN-ROOM DINING)
          ========================================================================= */}
      <section style={{ padding: '80px 20px', maxWidth: '1280px', margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '20px', marginBottom: '40px' }}>
          <div>
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--gold-primary)', letterSpacing: '0.12em', textTransform: 'uppercase' }}>
              Culinary Excellence
            </span>
            <h2 style={{ fontSize: '2.4rem', fontWeight: 800, marginTop: '8px' }}>
              Royal In-Room Gastronomy
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', marginTop: '6px' }}>
              Delivered piping hot to your suite with live kitchen order ticket (KOT) tracking.
            </p>
          </div>
          <button
            onClick={() => onNavigate('DINING')}
            className="btn-gold"
            style={{ gap: '8px' }}
          >
            <UtensilsCrossed size={16} />
            <span>Open Dining Room</span>
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '20px' }}>
          {MOCK_FOOD_ITEMS.slice(0, 4).map((food) => (
            <div key={food.id} className="glass-panel" style={{ padding: '22px', position: 'relative' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                <span style={{
                  fontSize: '0.72rem',
                  fontWeight: 700,
                  padding: '3px 8px',
                  borderRadius: 'var(--radius-full)',
                  background: food.dietaryType === 'VEG' ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                  color: food.dietaryType === 'VEG' ? '#10B981' : '#EF4444',
                  border: `1px solid ${food.dietaryType === 'VEG' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
                }}>
                  {food.dietaryType === 'VEG' ? '🟢 Pure Veg' : '🔴 Non-Veg'}
                </span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', display: 'flex', alignItems: 'center', gap: '3px' }}>
                  <Clock size={12} /> {food.prepTime} min
                </span>
              </div>

              <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                {food.name}
              </h4>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.5, marginBottom: '16px' }}>
                {food.description}
              </p>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '12px', borderTop: '1px solid var(--border-subtle)' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                  ₹{food.price}
                </span>
                <button
                  onClick={() => onNavigate('DINING')}
                  className="btn-outline-gold"
                  style={{ padding: '5px 12px', fontSize: '0.75rem' }}
                >
                  Order
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>


      {/* =========================================================================
          DISTRIBUTED MICROSERVICES ARCHITECTURE BLUEPRINT
          ========================================================================= */}
      <section style={{
        padding: '70px 20px',
        background: 'rgba(11, 17, 32, 0.85)',
        borderTop: '1px solid var(--border-gold)',
        borderBottom: '1px solid var(--border-gold)'
      }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
          
          <div style={{ textAlign: 'center', marginBottom: '40px' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--gold-primary)', letterSpacing: '0.12em', textTransform: 'uppercase' }}>
              High-Concurreny Distributed Backend
            </span>
            <h2 style={{ fontSize: '2.2rem', fontWeight: 800, marginTop: '8px' }}>
              Engineered with Enterprise Precision
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem', maxWidth: '640px', margin: '8px auto 0' }}>
              11 Spring Boot microservices, Kafka KRaft streaming, Redis 7 caching, and PostgreSQL 16 database-per-service isolation.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '20px' }}>
            
            <div className="glass-panel" style={{ padding: '24px', textAlign: 'center' }}>
              <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--gold-light)', marginBottom: '4px' }}>
                99.99%
              </div>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                Gateway Availability
              </div>
              <p style={{ fontSize: '0.76rem', color: 'var(--text-dim)' }}>
                Reactive Netty reverse proxy with JWT filter routing on port :8080.
              </p>
            </div>

            <div className="glass-panel" style={{ padding: '24px', textAlign: 'center' }}>
              <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--success)', marginBottom: '4px' }}>
                &lt; 15ms
              </div>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                Redis 7 Cache Speed
              </div>
              <p style={{ fontSize: '0.76rem', color: 'var(--text-dim)' }}>
                Cache-aside pattern for hotel catalog and room availability lookups.
              </p>
            </div>

            <div className="glass-panel" style={{ padding: '24px', textAlign: 'center' }}>
              <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--info)', marginBottom: '4px' }}>
                5 Topics
              </div>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                Apache Kafka KRaft
              </div>
              <p style={{ fontSize: '0.76rem', color: 'var(--text-dim)' }}>
                Event-driven asynchronous messaging for bookings, KOT, and invoices.
              </p>
            </div>

            <div className="glass-panel" style={{ padding: '24px', textAlign: 'center' }}>
              <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--gold-primary)', marginBottom: '4px' }}>
                10 DBs
              </div>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                PostgreSQL Isolation
              </div>
              <p style={{ fontSize: '0.76rem', color: 'var(--text-dim)' }}>
                Strict database-per-service pattern avoiding distributed transaction locks.
              </p>
            </div>

          </div>

        </div>
      </section>


      {/* =========================================================================
          ROLE SEPARATION PORTAL GATEWAY (GUEST, STAFF, ADMIN)
          ========================================================================= */}
      <section style={{ padding: '80px 20px', maxWidth: '1280px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '48px' }}>
          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--gold-primary)', letterSpacing: '0.12em', textTransform: 'uppercase' }}>
            Role-Based Portals
          </span>
          <h2 style={{ fontSize: '2.4rem', fontWeight: 800, marginTop: '8px' }}>
            Choose Your Operational Portal
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', maxWidth: '640px', margin: '8px auto 0' }}>
            Experience the system through distinct role personas with dedicated views tailored for guests, hotel staff, and executive leadership.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '26px' }}>
          
          {/* Guest Portal Card */}
          <div className="glass-panel" style={{ padding: '32px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', border: '1px solid var(--border-gold)' }}>
            <div>
              <div style={{
                width: '46px',
                height: '46px',
                borderRadius: '12px',
                background: 'rgba(212, 175, 55, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '18px'
              }}>
                <Crown size={24} color="var(--gold-primary)" />
              </div>
              <span className="badge badge-gold" style={{ marginBottom: '8px' }}>Sovereign Guest</span>
              <h3 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '10px' }}>
                Guest Portal & Folios
              </h3>
              <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6, marginBottom: '20px' }}>
                View upcoming reservations, live in-room dining orders, tax invoice breakdown, and instant personal butler requests.
              </p>
            </div>
            <button
              onClick={() => onNavigate('USER_DASHBOARD')}
              className="btn-gold"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              <span>Enter Guest Portal</span>
              <ChevronRight size={16} />
            </button>
          </div>

          {/* Staff & Kitchen Portal Card */}
          <div className="glass-panel" style={{ padding: '32px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
            <div>
              <div style={{
                width: '46px',
                height: '46px',
                borderRadius: '12px',
                background: 'rgba(56, 189, 248, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '18px'
              }}>
                <UtensilsCrossed size={24} color="var(--info)" />
              </div>
              <span className="badge badge-info" style={{ marginBottom: '8px' }}>Kitchen & Housekeeping</span>
              <h3 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '10px' }}>
                Staff & KOT Operations
              </h3>
              <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6, marginBottom: '20px' }}>
                Manage live kitchen order tickets (ORDERED → PREPARING → DELIVERED), room cleaning turnaround, and inventory restocking.
              </p>
            </div>
            <button
              onClick={() => onNavigate(isStaff || isAdmin ? 'STAFF' : 'LOGIN')}
              className="btn-outline-gold"
              style={{ width: '100%', justifyContent: 'center', borderColor: 'var(--info)', color: 'var(--info)' }}
            >
              <span>{isStaff || isAdmin ? 'Access Staff Portal' : 'Sign In as Staff'}</span>
              <ChevronRight size={16} />
            </button>
          </div>

          {/* Executive Admin Card */}
          <div className="glass-panel" style={{ padding: '32px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
            <div>
              <div style={{
                width: '46px',
                height: '46px',
                borderRadius: '12px',
                background: 'rgba(16, 185, 129, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: '18px'
              }}>
                <ShieldCheck size={24} color="var(--success)" />
              </div>
              <span className="badge badge-success" style={{ marginBottom: '8px' }}>Executive Management</span>
              <h3 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '10px' }}>
                Executive Operations Portal
              </h3>
              <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.6, marginBottom: '20px' }}>
                Executive analytics, RevPAR, portfolio room occupancy rates, and real-time 11-microservice Prometheus telemetry.
              </p>
            </div>
            <button
              onClick={() => onNavigate(isAdmin ? 'ADMIN' : 'LOGIN')}
              className="btn-gold"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              <span>{isAdmin ? 'Open Admin Dashboard' : 'Sign In as Admin'}</span>
              <ChevronRight size={16} />
            </button>
          </div>

        </div>
      </section>

    </div>
  );
};

export default LandingPage;
