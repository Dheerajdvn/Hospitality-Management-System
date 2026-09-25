import React, { useState, useEffect } from 'react';
import HotelCard from './HotelCard';
import { api } from '../api/apiClient';
import { Search, Sparkles, Filter } from 'lucide-react';

const HotelList = ({ onSelectHotel }) => {
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCity, setSelectedCity] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const loadHotels = async () => {
      setLoading(true);
      const data = await api.getHotels();
      setHotels(data);
      setLoading(false);
    };
    loadHotels();
  }, []);

  const cities = ['ALL', 'Mumbai', 'Goa', 'Shimla'];

  const filteredHotels = hotels.filter((h) => {
    const matchesCity = selectedCity === 'ALL' || h.city.toLowerCase() === selectedCity.toLowerCase();
    const matchesQuery = h.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         h.city.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         h.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCity && matchesQuery;
  });

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '30px 20px 80px' }}>
      
      {/* Hero Showcase Banner */}
      <div style={{
        position: 'relative',
        borderRadius: 'var(--radius-xl)',
        overflow: 'hidden',
        padding: '60px 40px',
        marginBottom: '40px',
        background: 'linear-gradient(135deg, rgba(19, 29, 51, 0.95) 0%, rgba(11, 17, 32, 0.98) 100%)',
        border: '1px solid var(--border-gold)',
        boxShadow: 'var(--shadow-lg), var(--gold-glow)'
      }}>
        <div style={{ maxWidth: '750px', position: 'relative', zIndex: 2 }}>
          <div className="badge badge-gold" style={{ marginBottom: '16px' }}>
            <Sparkles size={12} />
            Five-Star Sovereign Hospitality
          </div>
          <h1 style={{ fontSize: '2.8rem', fontWeight: 800, lineHeight: 1.15, marginBottom: '16px', color: '#fff' }}>
            Immerse in Unrivaled Luxury & Royal Opulence
          </h1>
          <p style={{ fontSize: '1.05rem', color: 'var(--text-muted)', lineHeight: 1.6, marginBottom: '28px' }}>
            Experience bespoke hospitality, Michelin-inspired in-room dining, and tranquil sanctuary escapes across India's most breathtaking premier destinations.
          </p>

          {/* Interactive Search & Filter Bar */}
          <div style={{
            background: 'rgba(15, 23, 42, 0.85)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-lg)',
            padding: '10px 16px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            boxShadow: 'var(--shadow-md)'
          }}>
            <Search size={20} color="var(--gold-primary)" />
            <input 
              type="text"
              placeholder="Search by hotel name, city, or landmark..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                flex: 1,
                background: 'transparent',
                border: 'none',
                color: '#fff',
                fontSize: '0.95rem',
                outline: 'none'
              }}
            />
            {searchQuery && (
              <button 
                onClick={() => setSearchQuery('')}
                style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}
              >
                Clear
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Destination Pills Filter */}
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between', 
        flexWrap: 'wrap', 
        gap: '16px', 
        marginBottom: '30px' 
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Filter size={16} color="var(--gold-primary)" />
          <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginRight: '6px' }}>Destinations:</span>
          {cities.map((city) => (
            <button
              key={city}
              onClick={() => setSelectedCity(city)}
              style={{
                padding: '6px 16px',
                borderRadius: 'var(--radius-full)',
                fontSize: '0.82rem',
                fontWeight: 600,
                border: '1px solid',
                borderColor: selectedCity === city ? 'var(--gold-primary)' : 'var(--border-subtle)',
                background: selectedCity === city ? 'var(--gold-gradient)' : 'rgba(255, 255, 255, 0.04)',
                color: selectedCity === city ? '#0B1120' : 'var(--text-muted)',
                transition: 'all 0.2s ease',
                cursor: 'pointer'
              }}
            >
              {city === 'ALL' ? 'All Properties' : city}
            </button>
          ))}
        </div>

        <div style={{ fontSize: '0.88rem', color: 'var(--text-dim)' }}>
          Showing <span style={{ color: 'var(--gold-light)', fontWeight: 700 }}>{filteredHotels.length}</span> luxury properties
        </div>
      </div>

      {/* Hotel Cards Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--gold-primary)' }}>
          Loading luxury destinations...
        </div>
      ) : filteredHotels.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '60px 20px', borderRadius: 'var(--radius-lg)' }}>
          <p style={{ color: 'var(--text-muted)', fontSize: '1.1rem' }}>No hotels found matching your search criteria.</p>
          <button 
            onClick={() => { setSelectedCity('ALL'); setSearchQuery(''); }}
            className="btn-outline-gold" 
            style={{ marginTop: '16px' }}
          >
            Reset Filters
          </button>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(380px, 1fr))',
          gap: '28px'
        }}>
          {filteredHotels.map((hotel) => (
            <HotelCard 
              key={hotel.id} 
              hotel={hotel} 
              onSelect={onSelectHotel} 
            />
          ))}
        </div>
      )}

    </div>
  );
};

export default HotelList;
