import React, { useState, useEffect } from 'react';
import { api } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';
import { 
  UtensilsCrossed, 
  Clock, 
  Plus, 
  Minus, 
  ShoppingBag, 
  CheckCircle2, 
  Sparkles, 
  ChefHat, 
  AlertTriangle,
  ArrowRight,
  Flame,
  Check
} from 'lucide-react';

const DiningMenu = ({ cart = [], setCart, onOrderPlaced, onNavigate, bookings = [] }) => {
  const { isStaff, isAdmin } = useAuth();
  const isStaffMode = isStaff && !isAdmin;

  // Active reservation matching guest's booking
  const activeBooking = bookings?.find((b) => b.status === 'CONFIRMED' || b.paymentStatus === 'PAID') || bookings?.[0];

  const [selectedHotelId, setSelectedHotelId] = useState(1);
  const [menuItems, setMenuItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [dietaryFilter, setDietaryFilter] = useState('ALL');
  const [availabilityFilter, setAvailabilityFilter] = useState('ALL'); // 'ALL' | 'AVAILABLE' | '86'
  const [customRoomNumber, setCustomRoomNumber] = useState('');
  const roomNumber = customRoomNumber || activeBooking?.roomNumber || '101';
  const [specialInstructions, setSpecialInstructions] = useState('');
  const [placedOrder, setPlacedOrder] = useState(null);
  const [toastNotice, setToastNotice] = useState('');

  useEffect(() => {
    const fetchMenu = async () => {
      setLoading(true);
      const cat = categoryFilter === 'ALL' ? '' : categoryFilter;
      const diet = dietaryFilter === 'ALL' ? '' : dietaryFilter;
      const data = await api.getMenu(selectedHotelId, cat, diet);
      setMenuItems(data);
      setLoading(false);
    };
    fetchMenu();
  }, [selectedHotelId, categoryFilter, dietaryFilter]);

  // Guest Cart Actions
  const addToCart = (item) => {
    if (item.isAvailable === false) return;
    setCart((prev) => {
      const existing = prev.find((i) => i.id === item.id);
      if (existing) {
        return prev.map((i) => (i.id === item.id ? { ...i, quantity: i.quantity + 1 } : i));
      }
      return [...prev, { ...item, quantity: 1, notes: '' }];
    });
  };

  const updateQuantity = (itemId, delta) => {
    setCart((prev) => {
      return prev
        .map((i) => {
          if (i.id === itemId) {
            const newQty = i.quantity + delta;
            return newQty > 0 ? { ...i, quantity: newQty } : null;
          }
          return i;
        })
        .filter(Boolean);
    });
  };

  // Staff Availability (86'd) Toggle Action
  const toggleDishAvailability = async (dishId) => {
    const targetDish = menuItems.find((d) => d.id === dishId);
    if (!targetDish) return;
    const newStatus = targetDish.isAvailable === false ? true : false;
    
    // Update local state immediately
    setMenuItems((prev) =>
      prev.map((d) => (d.id === dishId ? { ...d, isAvailable: newStatus } : d))
    );

    // Sync to API / mock storage
    await api.updateDishAvailability(dishId, newStatus);

    setToastNotice(
      newStatus
        ? `🟢 "${targetDish.name}" is now marked IN STOCK & Available for guest ordering.`
        : `🔴 "${targetDish.name}" is now 86'd (Sold Out). Guest orders locked.`
    );
    setTimeout(() => setToastNotice(''), 3500);
  };

  const cartSubtotal = cart.reduce((acc, item) => acc + item.price * item.quantity, 0);
  const cartTax = Math.round(cartSubtotal * 0.05); // 5% GST
  const deliveryFee = cart.length > 0 ? 30 : 0;
  const cartTotal = cartSubtotal + cartTax + deliveryFee;

  const handlePlaceOrder = async () => {
    if (cart.length === 0) return;
    const orderPayload = {
      bookingId: activeBooking?.id || 1,
      hotelId: selectedHotelId,
      roomId: activeBooking?.roomId || 1,
      roomNumber: roomNumber || activeBooking?.roomNumber || '101',
      items: cart.map((i) => ({ foodItemId: i.id, quantity: i.quantity, unitPrice: i.price, foodItemName: i.name, notes: i.notes || '' })),
      specialInstructions,
    };
    const response = await api.createRoomServiceOrder(orderPayload);
    if (response?.data) {
      setPlacedOrder(response.data);
      setCart([]);
      if (onOrderPlaced) {
        onOrderPlaced(response.data);
      }
    }
  };

  const categories = [
    { label: 'All Dishes', value: 'ALL' },
    { label: 'Appetizers', value: 'APPETIZER' },
    { label: 'Main Course', value: 'MAIN_COURSE' },
    { label: 'Breads', value: 'BREADS' },
    { label: 'Desserts', value: 'DESSERT' },
    { label: 'Beverages', value: 'BEVERAGE' },
  ];

  const dietaryTypes = ['ALL', 'VEG', 'NON_VEG', 'VEGAN'];

  // Filter items by availability in staff mode
  const displayedItems = menuItems.filter((dish) => {
    if (!isStaffMode || availabilityFilter === 'ALL') return true;
    if (availabilityFilter === 'AVAILABLE') return dish.isAvailable !== false;
    if (availabilityFilter === '86') return dish.isAvailable === false;
    return true;
  });

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '30px 20px 80px' }}>
      
      {/* Toast Notification Banner for Staff Availability Changes */}
      {toastNotice && (
        <div style={{
          position: 'fixed',
          top: '80px',
          right: '24px',
          zIndex: 9999,
          background: 'rgba(15, 23, 42, 0.95)',
          border: '1px solid var(--border-gold)',
          borderRadius: 'var(--radius-md)',
          padding: '12px 18px',
          boxShadow: 'var(--shadow-lg), var(--gold-glow)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontSize: '0.85rem',
          color: '#fff',
          animation: 'slideIn 0.25s ease'
        }}>
          <ChefHat size={18} color="var(--gold-primary)" />
          <span>{toastNotice}</span>
        </div>
      )}

      {/* Header Bar - Role Specific */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '20px', marginBottom: '26px' }}>
        <div>
          {isStaffMode ? (
            <div>
              <div className="badge badge-info" style={{ marginBottom: '8px' }}>
                <ChefHat size={13} /> Chef & Kitchen Station Console
              </div>
              <h1 style={{ fontSize: '2.1rem', fontWeight: 800, color: '#fff' }}>
                Kitchen Menu & 86'd Recipe Control
              </h1>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>
                Manage live dish availability, 86'd items, recipe prep times, and station assignments.
              </p>
            </div>
          ) : (
            <div>
              <div className="badge badge-gold" style={{ marginBottom: '8px' }}>
                <UtensilsCrossed size={12} /> In-Room Dining & Kitchen Service
              </div>
              <h1 style={{ fontSize: '2.2rem', fontWeight: 800, color: '#fff' }}>
                Gourmet Room Service Menu
              </h1>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                Handcrafted culinary masterworks delivered directly to your private suite.
              </p>
            </div>
          )}
        </div>

        {/* Action Controls & Hotel Selector */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          {isStaffMode && onNavigate && (
            <button
              onClick={() => onNavigate('STAFF')}
              className="btn-gold"
              style={{ padding: '8px 16px', fontSize: '0.82rem' }}
            >
              <Flame size={15} />
              <span>Go to Live KOT Board →</span>
            </button>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-dim)' }}>Property:</span>
            <select 
              value={selectedHotelId} 
              onChange={(e) => setSelectedHotelId(Number(e.target.value))}
              className="input-luxury"
              style={{ width: 'auto', padding: '8px 14px', fontWeight: 600, fontSize: '0.85rem' }}
            >
              <option value={1}>The Grand Palace, Mumbai</option>
              <option value={2}>Ocean View Resort, Goa</option>
              <option value={3}>Himalayan Heritage, Shimla</option>
            </select>
          </div>
        </div>
      </div>

      {/* Guest Placed Order Success Banner (Guests only) */}
      {!isStaffMode && placedOrder && (
        <div className="glass-panel" style={{
          padding: '24px',
          marginBottom: '32px',
          border: '1px solid var(--success)',
          background: 'rgba(16, 185, 129, 0.08)',
          borderRadius: 'var(--radius-lg)'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
              <div style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                background: 'var(--success)',
                color: '#0B1120',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <CheckCircle2 size={28} />
              </div>
              <div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff' }}>
                  Kitchen Order Placed! (KOT: {placedOrder.kotNumber})
                </h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>
                  Order #{placedOrder.orderNumber} for Room {placedOrder.roomNumber} is dispatched to Executive Chef. Estimated delivery: {placedOrder.estimatedDeliveryMinutes || 35} minutes.
                </p>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span className="badge badge-warning" style={{ padding: '6px 14px' }}>
                Status: {placedOrder.status}
              </span>
              <button onClick={() => setPlacedOrder(null)} className="btn-ghost" style={{ fontSize: '0.85rem' }}>
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Main Grid: Dishes (Left) + Role-Specific Sidebar (Right) */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: '32px', alignItems: 'start' }}>
        
        {/* Left: Filter Controls & Dish Cards */}
        <div>
          {/* Category Tabs */}
          <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '12px', marginBottom: '14px' }}>
            {categories.map((cat) => (
              <button
                key={cat.value}
                onClick={() => setCategoryFilter(cat.value)}
                style={{
                  padding: '7px 15px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.84rem',
                  fontWeight: 600,
                  whiteSpace: 'nowrap',
                  border: '1px solid',
                  borderColor: categoryFilter === cat.value ? 'var(--gold-primary)' : 'var(--border-subtle)',
                  background: categoryFilter === cat.value ? 'var(--gold-gradient)' : 'rgba(255, 255, 255, 0.03)',
                  color: categoryFilter === cat.value ? '#0B1120' : 'var(--text-muted)',
                  cursor: 'pointer'
                }}
              >
                {cat.label}
              </button>
            ))}
          </div>

          {/* Secondary Filters: Dietary + Staff Availability Filter */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px', marginBottom: '22px' }}>
            {/* Dietary Filter Pills */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>Dietary:</span>
              {dietaryTypes.map((diet) => (
                <button
                  key={diet}
                  onClick={() => setDietaryFilter(diet)}
                  style={{
                    padding: '3px 10px',
                    borderRadius: 'var(--radius-full)',
                    fontSize: '0.76rem',
                    fontWeight: 600,
                    border: '1px solid var(--border-subtle)',
                    background: dietaryFilter === diet ? 'rgba(212, 175, 55, 0.15)' : 'transparent',
                    color: dietaryFilter === diet ? 'var(--gold-light)' : 'var(--text-dim)',
                    cursor: 'pointer'
                  }}
                >
                  {diet === 'ALL' ? 'All Types' : diet}
                </button>
              ))}
            </div>

            {/* Staff-Only Availability Quick Filter */}
            {isStaffMode && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>Stock Status:</span>
                <button
                  onClick={() => setAvailabilityFilter('ALL')}
                  style={{
                    padding: '3px 9px',
                    borderRadius: 'var(--radius-full)',
                    fontSize: '0.74rem',
                    fontWeight: 600,
                    background: availabilityFilter === 'ALL' ? 'var(--gold-primary)' : 'rgba(255,255,255,0.05)',
                    color: availabilityFilter === 'ALL' ? '#0B1120' : 'var(--text-muted)',
                    cursor: 'pointer'
                  }}
                >
                  All ({menuItems.length})
                </button>
                <button
                  onClick={() => setAvailabilityFilter('AVAILABLE')}
                  style={{
                    padding: '3px 9px',
                    borderRadius: 'var(--radius-full)',
                    fontSize: '0.74rem',
                    fontWeight: 600,
                    background: availabilityFilter === 'AVAILABLE' ? 'var(--success)' : 'rgba(255,255,255,0.05)',
                    color: availabilityFilter === 'AVAILABLE' ? '#0B1120' : 'var(--text-muted)',
                    cursor: 'pointer'
                  }}
                >
                  In Stock ({menuItems.filter((i) => i.isAvailable !== false).length})
                </button>
                <button
                  onClick={() => setAvailabilityFilter('86')}
                  style={{
                    padding: '3px 9px',
                    borderRadius: 'var(--radius-full)',
                    fontSize: '0.74rem',
                    fontWeight: 600,
                    background: availabilityFilter === '86' ? 'var(--danger)' : 'rgba(255,255,255,0.05)',
                    color: availabilityFilter === '86' ? '#fff' : 'var(--text-muted)',
                    cursor: 'pointer'
                  }}
                >
                  86'd ({menuItems.filter((i) => i.isAvailable === false).length})
                </button>
              </div>
            )}
          </div>

          {/* Dishes Grid */}
          {loading ? (
            <div style={{ color: 'var(--gold-primary)', padding: '40px 0', textAlign: 'center' }}>
              Loading culinary catalog...
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '20px' }}>
              {displayedItems.map((dish) => {
                const isOutOfStock = dish.isAvailable === false;

                return (
                  <div 
                    key={dish.id} 
                    className="glass-panel" 
                    style={{ 
                      padding: '18px', 
                      display: 'flex', 
                      flexDirection: 'column', 
                      justifyContent: 'space-between',
                      border: isOutOfStock ? '1px solid rgba(239, 68, 68, 0.35)' : '1px solid var(--border-subtle)',
                      background: isOutOfStock ? 'rgba(239, 68, 68, 0.03)' : undefined
                    }}
                  >
                    <div>
                      {/* Top tags */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <span className={dish.dietaryType === 'VEG' ? 'badge badge-success' : 'badge badge-warning'}>
                            {dish.dietaryType}
                          </span>
                          {isStaffMode && (
                            <span className="badge badge-info" style={{ fontSize: '0.68rem' }}>
                              {dish.category === 'APPETIZER' ? 'Tandoor' :
                               dish.category === 'MAIN_COURSE' ? 'Curry' :
                               dish.category === 'BREADS' ? 'Clay Oven' :
                               dish.category === 'DESSERT' ? 'Pantry' : 'Barista'}
                            </span>
                          )}
                        </div>

                        <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Clock size={12} /> {dish.prepTime || 20}m
                        </span>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px', marginBottom: '6px' }}>
                        <h4 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#fff' }}>
                          {dish.name}
                        </h4>
                        {isOutOfStock && (
                          <span style={{ fontSize: '0.68rem', fontWeight: 800, color: 'var(--danger)', background: 'rgba(239, 68, 68, 0.15)', padding: '2px 6px', borderRadius: '4px' }}>
                            86'D / SOLD OUT
                          </span>
                        )}
                      </div>

                      <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: 1.4, marginBottom: '16px' }}>
                        {dish.description}
                      </p>
                    </div>

                    {/* Operational Action Controls: Staff vs Guest */}
                    <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '12px' }}>
                      {isStaffMode ? (
                        /* STAFF: Availability (86'd) Toggle Console */
                        <div>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px', fontSize: '0.8rem' }}>
                            <span style={{ color: 'var(--text-dim)' }}>Price: <strong style={{ color: 'var(--gold-light)' }}>₹{dish.price}</strong></span>
                            <span style={{ fontSize: '0.74rem', color: isOutOfStock ? 'var(--danger)' : 'var(--success)', fontWeight: 700 }}>
                              {isOutOfStock ? '● 86\'d / Unavailable' : '● Live in Guest Menu'}
                            </span>
                          </div>

                          <button
                            onClick={() => toggleDishAvailability(dish.id)}
                            className={isOutOfStock ? 'btn-gold' : 'btn-outline-gold'}
                            style={{
                              width: '100%',
                              justifyContent: 'center',
                              padding: '8px 12px',
                              fontSize: '0.8rem',
                              fontWeight: 700,
                              borderColor: isOutOfStock ? undefined : 'var(--danger)',
                              color: isOutOfStock ? undefined : 'var(--danger)',
                              background: isOutOfStock ? undefined : 'rgba(239, 68, 68, 0.08)'
                            }}
                            title={isOutOfStock ? 'Restock this dish to make it available to guests' : '86 this dish to stop guest room service orders'}
                          >
                            {isOutOfStock ? (
                              <>
                                <Check size={14} />
                                <span>Restock Dish (Make Available)</span>
                              </>
                            ) : (
                              <>
                                <AlertTriangle size={14} />
                                <span>86'd Dish (Mark Sold Out)</span>
                              </>
                            )}
                          </button>
                        </div>
                      ) : (
                        /* GUEST: Pricing & Add to Cart */
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                          <span style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                            ₹{dish.price}
                          </span>
                          {isOutOfStock ? (
                            <button 
                              disabled 
                              className="btn-ghost" 
                              style={{ padding: '6px 14px', fontSize: '0.82rem', color: 'var(--danger)', opacity: 0.6, cursor: 'not-allowed' }}
                            >
                              Sold Out
                            </button>
                          ) : (
                            <button 
                              onClick={() => addToCart(dish)}
                              className="btn-gold" 
                              style={{ padding: '6px 14px', fontSize: '0.82rem' }}
                            >
                              <Plus size={14} /> Add
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Sidebar: Operational Status for Staff vs Room Service Cart for Guests */}
        {isStaffMode ? (
          /* =========================================================================
             STAFF RIGHT PANEL: KITCHEN OPERATIONS & STATION MONITOR
             ========================================================================= */
          <div className="glass-panel" style={{ padding: '24px', position: 'sticky', top: '90px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px', borderBottom: '1px solid var(--border-gold)', paddingBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ChefHat size={20} color="var(--gold-primary)" />
                <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#fff' }}>Kitchen Station Status</h3>
              </div>
              <span className="badge badge-info" style={{ fontSize: '0.68rem' }}>STAFF ONLY</span>
            </div>

            {/* Quick Metrics */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '20px' }}>
              <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)', textAlign: 'center' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>In Stock</div>
                <div style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--success)', marginTop: '2px' }}>
                  {menuItems.filter((i) => i.isAvailable !== false).length}
                </div>
              </div>
              <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-subtle)', textAlign: 'center' }}>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>86'd Sold Out</div>
                <div style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--danger)', marginTop: '2px' }}>
                  {menuItems.filter((i) => i.isAvailable === false).length}
                </div>
              </div>
            </div>

            {/* Station Breakdown */}
            <div style={{ marginBottom: '22px' }}>
              <h4 style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--gold-light)', textTransform: 'uppercase', marginBottom: '10px', letterSpacing: '0.04em' }}>
                Active Kitchen Stations
              </h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '0.82rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '9px 12px', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)' }}>
                  <span>🔥 Tandoor & Grill Station</span>
                  <span style={{ color: 'var(--success)', fontWeight: 700 }}>Operational</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '9px 12px', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)' }}>
                  <span>🍲 Curry & Biryani Station</span>
                  <span style={{ color: 'var(--success)', fontWeight: 700 }}>Operational</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '9px 12px', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)' }}>
                  <span>🥖 Clay Oven / Breads</span>
                  <span style={{ color: 'var(--success)', fontWeight: 700 }}>Operational</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '9px 12px', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)' }}>
                  <span>☕ Dessert & Beverage Bar</span>
                  <span style={{ color: 'var(--success)', fontWeight: 700 }}>Operational</span>
                </div>
              </div>
            </div>

            {/* Fast Workflow Navigation Buttons */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {onNavigate && (
                <>
                  <button 
                    onClick={() => onNavigate('STAFF')}
                    className="btn-gold" 
                    style={{ width: '100%', justifyContent: 'center', padding: '11px', fontSize: '0.85rem' }}
                  >
                    <UtensilsCrossed size={15} />
                    <span>Open Live KOT Tickets Board</span>
                    <ArrowRight size={14} />
                  </button>
                  <button 
                    onClick={() => onNavigate('STAFF')}
                    className="btn-outline-gold" 
                    style={{ width: '100%', justifyContent: 'center', padding: '10px', fontSize: '0.82rem' }}
                  >
                    <Sparkles size={14} />
                    <span>Butler & Housekeeping Hub</span>
                  </button>
                </>
              )}
            </div>

            <div style={{
              marginTop: '18px',
              paddingTop: '14px',
              borderTop: '1px solid var(--border-subtle)',
              fontSize: '0.72rem',
              color: 'var(--text-dim)',
              textAlign: 'center',
              lineHeight: 1.4
            }}>
              🔒 Role-Guarded: Consumer shopping cart and guest suite deliveries are restricted for staff accounts.
            </div>
          </div>
        ) : (
          /* =========================================================================
             GUEST RIGHT PANEL: LIVE ROOM SERVICE CART
             ========================================================================= */
          <div className="glass-panel" style={{ padding: '24px', position: 'sticky', top: '90px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ShoppingBag size={20} color="var(--gold-primary)" />
                <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#fff' }}>Room Service Cart</h3>
              </div>
              <span className="badge badge-gold">{cart.reduce((a, b) => a + b.quantity, 0)} items</span>
            </div>

            {cart.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 10px', color: 'var(--text-dim)' }}>
                <UtensilsCrossed size={36} style={{ margin: '0 auto 12px', opacity: 0.3 }} />
                <p style={{ fontSize: '0.9rem' }}>Your room dining cart is empty.</p>
                <p style={{ fontSize: '0.78rem' }}>Add dishes from the menu to initiate an in-room dining order.</p>
              </div>
            ) : (
              <div>
                {/* Cart Line Items */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '280px', overflowY: 'auto', marginBottom: '20px', paddingRight: '4px' }}>
                  {cart.map((item) => (
                    <div key={item.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: 'rgba(255,255,255,0.02)', padding: '10px', borderRadius: 'var(--radius-sm)' }}>
                      <div style={{ flex: 1, paddingRight: '8px' }}>
                        <div style={{ fontSize: '0.88rem', fontWeight: 600, color: '#fff' }}>{item.name}</div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--gold-light)' }}>₹{item.price} each</div>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <button 
                          onClick={() => updateQuantity(item.id, -1)}
                          style={{ width: '24px', height: '24px', borderRadius: '4px', background: 'rgba(255,255,255,0.1)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                        >
                          <Minus size={12} />
                        </button>
                        <span style={{ fontSize: '0.88rem', fontWeight: 700, minWidth: '16px', textAlign: 'center' }}>
                          {item.quantity}
                        </span>
                        <button 
                          onClick={() => updateQuantity(item.id, 1)}
                          style={{ width: '24px', height: '24px', borderRadius: '4px', background: 'rgba(255,255,255,0.1)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                        >
                          <Plus size={12} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Room & Instructions */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
                  <div>
                    <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>
                      Deliver to Room Number:
                    </label>
                    <input 
                      type="text" 
                      value={roomNumber} 
                      onChange={(e) => setCustomRoomNumber(e.target.value)}
                      className="input-luxury"
                      style={{ padding: '8px 12px', fontSize: '0.9rem' }}
                      placeholder="e.g. 101, 201"
                    />
                  </div>

                  <div>
                    <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>
                      Chef Notes / Preferences:
                    </label>
                    <input 
                      type="text" 
                      value={specialInstructions} 
                      onChange={(e) => setSpecialInstructions(e.target.value)}
                      className="input-luxury"
                      style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                      placeholder="e.g. extra spicy, deliver by 8 PM"
                    />
                  </div>
                </div>

                {/* Order Calculations */}
                <div style={{ background: 'rgba(11, 17, 32, 0.6)', padding: '14px', borderRadius: 'var(--radius-sm)', marginBottom: '20px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '6px' }}>
                    <span>Dishes Subtotal:</span>
                    <span>₹{cartSubtotal}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '6px' }}>
                    <span>Dining GST (5%):</span>
                    <span>₹{cartTax}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '8px' }}>
                    <span>Room Service Delivery:</span>
                    <span>₹{deliveryFee}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 800, fontSize: '1.05rem', color: '#fff', borderTop: '1px solid var(--border-subtle)', paddingTop: '8px' }}>
                    <span>Total Amount:</span>
                    <span style={{ color: 'var(--gold-light)' }}>₹{cartTotal}</span>
                  </div>
                </div>

                {/* Submit CTA */}
                <button 
                  onClick={handlePlaceOrder}
                  className="btn-gold" 
                  style={{ width: '100%', justifyContent: 'center', padding: '12px' }}
                >
                  <Sparkles size={16} /> Place Kitchen Order (KOT)
                </button>
              </div>
            )}
          </div>
        )}

      </div>

    </div>
  );
};

export default DiningMenu;
