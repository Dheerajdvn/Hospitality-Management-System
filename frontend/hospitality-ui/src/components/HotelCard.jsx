import React from 'react';
import { Star, MapPin, Sparkles, CheckCircle2 } from 'lucide-react';

const HotelCard = ({ hotel, onSelect }) => {
  return (
    <div className="glass-panel" style={{ overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
      
      {/* Hotel Image & Overlay Badges */}
      <div style={{ position: 'relative', height: '220px', overflow: 'hidden' }}>
        <img 
          src={hotel.imageUrl} 
          alt={hotel.name}
          style={{ 
            width: '100%', 
            height: '100%', 
            objectFit: 'cover', 
            transition: 'transform 0.4s ease' 
          }}
          onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.05)'}
          onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1.0)'}
        />
        
        {/* Rating Badge */}
        <div style={{ 
          position: 'absolute', 
          top: '14px', 
          right: '14px', 
          background: 'rgba(11, 17, 32, 0.85)', 
          backdropFilter: 'blur(8px)',
          border: '1px solid var(--border-gold)',
          borderRadius: 'var(--radius-full)',
          padding: '4px 10px',
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          color: 'var(--gold-light)',
          fontSize: '0.82rem',
          fontWeight: 700
        }}>
          <Star size={14} fill="#D4AF37" color="#D4AF37" />
          <span>{hotel.starRating}</span>
        </div>

        {/* City Tag */}
        <div style={{ 
          position: 'absolute', 
          bottom: '12px', 
          left: '12px',
          background: 'rgba(0, 0, 0, 0.7)',
          backdropFilter: 'blur(6px)',
          padding: '4px 10px',
          borderRadius: 'var(--radius-sm)',
          fontSize: '0.78rem',
          fontWeight: 600,
          color: '#fff',
          display: 'flex',
          alignItems: 'center',
          gap: '4px'
        }}>
          <MapPin size={13} color="var(--gold-light)" />
          <span>{hotel.city}</span>
        </div>
      </div>

      {/* Hotel Info */}
      <div style={{ padding: '20px', flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
        <div>
          <h2 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '6px', color: 'var(--text-main)' }}>
            {hotel.name}
          </h2>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-muted)', marginBottom: '14px', lineHeight: 1.5 }}>
            {hotel.description}
          </p>

          {/* Amenities Chips */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '18px' }}>
            {hotel.amenities?.slice(0, 4).map((amenity, idx) => (
              <span key={idx} style={{
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-full)',
                padding: '3px 9px',
                fontSize: '0.74rem',
                color: 'var(--text-muted)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px'
              }}>
                <CheckCircle2 size={11} color="var(--gold-primary)" />
                {amenity}
              </span>
            ))}
          </div>
        </div>

        {/* Footer: Price & CTA */}
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'space-between',
          borderTop: '1px solid var(--border-subtle)',
          paddingTop: '14px',
          marginTop: 'auto'
        }}>
          <div>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Starting From
            </span>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '4px' }}>
              <span style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                ₹{hotel.startingPrice?.toLocaleString('en-IN')}
              </span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>/ night</span>
            </div>
          </div>

          <button 
            onClick={() => onSelect(hotel)}
            className="btn-gold"
            style={{ padding: '8px 18px', fontSize: '0.85rem' }}
          >
            <Sparkles size={14} />
            View Rooms
          </button>
        </div>

      </div>

    </div>
  );
};

export default HotelCard;
