import React, { useState } from 'react';
import { 
  UtensilsCrossed, 
  Sparkles, 
  CheckCircle2, 
  Plus, 
  PackageCheck,
  ChefHat,
  Bell,
  Filter
} from 'lucide-react';
import { api, MOCK_FOOD_ITEMS } from '../api/apiClient';

const StaffDashboard = () => {
  const [activeStaffTab, setActiveStaffTab] = useState('KOT'); // 'KOT' | 'BUTLER' | 'HOUSEKEEPING' | 'INVENTORY'
  const [stationFilter, setStationFilter] = useState('ALL');
  const [isNewKotModalOpen, setIsNewKotModalOpen] = useState(false);

  // Manual KOT form state
  const [newKotRoom, setNewKotRoom] = useState('101');
  const [newKotItem, setNewKotItem] = useState(MOCK_FOOD_ITEMS[0].name);
  const [newKotQty, setNewKotQty] = useState(1);
  const [newKotNotes, setNewKotNotes] = useState('');

  // Live Kitchen Order Tickets (KOT)
  const [kotOrders, setKotOrders] = useState([
    {
      id: 1,
      orderNumber: 'RSO-20260925-4B47BB11',
      kotNumber: 'KOT-85249',
      roomNumber: '101',
      hotelName: 'The Grand Palace, Mumbai',
      status: 'PREPARING',
      station: 'TANDOOR',
      totalAmount: 1657.5,
      items: [
        { name: 'Paneer Tikka Royale', quantity: 2 },
        { name: 'Grand Butter Chicken', quantity: 1 },
        { name: 'Garlic Butter Naan', quantity: 3 }
      ],
      specialInstructions: 'Less spicy, extra mint chutney & lemon wedges',
      orderedAt: '15:33',
      priority: 'HIGH'
    },
    {
      id: 2,
      orderNumber: 'RSO-20260925-8891C1FA',
      kotNumber: 'KOT-85250',
      roomNumber: '201',
      hotelName: 'The Grand Palace, Mumbai',
      status: 'ORDERED',
      station: 'CURRY',
      totalAmount: 598.5,
      items: [
        { name: 'Nawabi Dum Biryani', quantity: 1 }
      ],
      specialInstructions: 'Deliver by 9 PM with burani raita',
      orderedAt: '15:40',
      priority: 'NORMAL'
    },
    {
      id: 3,
      orderNumber: 'RSO-20260925-1102A7E2',
      kotNumber: 'KOT-85248',
      roomNumber: '301',
      hotelName: 'The Grand Palace, Mumbai',
      status: 'OUT_FOR_DELIVERY',
      station: 'SNACKS',
      totalAmount: 945.0,
      items: [
        { name: 'Classic Club Sandwich', quantity: 2 },
        { name: 'Royal Masala Chai', quantity: 2 }
      ],
      specialInstructions: 'Ring doorbell twice, silver cloche required',
      orderedAt: '15:15',
      priority: 'VIP'
    },
    {
      id: 4,
      orderNumber: 'RSO-20260925-9921D09A',
      kotNumber: 'KOT-85245',
      roomNumber: '102',
      hotelName: 'The Grand Palace, Mumbai',
      status: 'DELIVERED',
      station: 'CURRY',
      totalAmount: 380.0,
      items: [
        { name: 'Dal Makhani Bukhara', quantity: 1 }
      ],
      specialInstructions: 'Delivered to room attendant',
      orderedAt: '14:50',
      priority: 'NORMAL'
    }
  ]);

  // Live Guest Butler & Concierge Dispatch Queue
  const [butlerRequests, setButlerRequests] = useState([
    { id: 1, suite: '101', guest: 'Lord Alexander', request: 'Fresh Egyptian Cotton Towels (x2)', status: 'IN_TRANSIT', time: '15:42', priority: 'NORMAL' },
    { id: 2, suite: '201', guest: 'Lady Eleanor', request: 'Evening Turndown & Aromatherapy Tea', status: 'PENDING', time: '15:48', priority: 'HIGH' },
    { id: 3, suite: '301', guest: 'Diplomat Vance', request: 'Rolls-Royce Valet Vehicle to Portico', status: 'DISPATCHED', time: '15:30', priority: 'VIP' },
  ]);

  // Housekeeping Room Turnaround State
  const [rooms, setRooms] = useState([
    { id: 101, roomNumber: '101', type: 'DELUXE SUITE', floor: 1, status: 'OCCUPIED', housekeeping: 'INSPECTED', guest: 'Lord Alexander' },
    { id: 102, roomNumber: '102', type: 'DELUXE SUITE', floor: 1, status: 'AVAILABLE', housekeeping: 'CLEAN_READY', guest: null },
    { id: 201, roomNumber: '201', type: 'ROYAL SUITE', floor: 2, status: 'OCCUPIED', housekeeping: 'DIRTY_NEEDS_CLEANING', guest: 'Lady Eleanor' },
    { id: 202, roomNumber: '202', type: 'ROYAL SUITE', floor: 2, status: 'AVAILABLE', housekeeping: 'CLEAN_READY', guest: null },
    { id: 301, roomNumber: '301', type: 'PRESIDENTIAL SUITE', floor: 3, status: 'OCCUPIED', housekeeping: 'INSPECTED', guest: 'Diplomat Vance' },
    { id: 302, roomNumber: '302', type: 'PRESIDENTIAL SUITE', floor: 3, status: 'MAINTENANCE', housekeeping: 'DIRTY_NEEDS_CLEANING', guest: null },
  ]);

  // Inventory Restock State
  const [inventory, setInventory] = useState([
    { id: 1, itemCode: 'INV-MUM-LIN-001', name: 'Egyptian Cotton King Sheet', category: 'LINEN', stock: 148, reorder: 30, unit: 'PIECES' },
    { id: 2, itemCode: 'INV-MUM-LIN-002', name: 'Plush Turkish Bath Towel', category: 'LINEN', stock: 200, reorder: 50, unit: 'PIECES' },
    { id: 3, itemCode: 'INV-MUM-TOI-001', name: 'Forest Essentials Shampoo 50ml', category: 'TOILETRIES', stock: 500, reorder: 100, unit: 'PIECES' },
    { id: 4, itemCode: 'INV-MUM-TOI-002', name: 'Bamboo Dental Hygiene Kit', category: 'TOILETRIES', stock: 40, reorder: 50, unit: 'PACKETS', alert: true },
    { id: 5, itemCode: 'INV-MUM-KIT-001', name: 'Aged Basmati Rice 25kg', category: 'KITCHEN_PANTRY', stock: 15, reorder: 5, unit: 'PACKETS' },
    { id: 6, itemCode: 'INV-MUM-CLN-001', name: 'Surface Disinfectant 5L', category: 'CLEANING_SUPPLIES', stock: 30, reorder: 10, unit: 'LITERS' },
  ]);

  // Advance KOT Status
  const advanceOrderStatus = (orderId) => {
    setKotOrders((prev) =>
      prev.map((o) => {
        if (o.id === orderId) {
          let nextStatus = o.status;
          if (o.status === 'ORDERED') nextStatus = 'PREPARING';
          else if (o.status === 'PREPARING') nextStatus = 'OUT_FOR_DELIVERY';
          else if (o.status === 'OUT_FOR_DELIVERY') nextStatus = 'DELIVERED';
          return { ...o, status: nextStatus };
        }
        return o;
      })
    );
  };

  // Advance Butler Request Status
  const advanceButlerStatus = (id) => {
    setButlerRequests((prev) =>
      prev.map((r) => {
        if (r.id === id) {
          const next = r.status === 'PENDING' ? 'DISPATCHED' : r.status === 'DISPATCHED' ? 'IN_TRANSIT' : 'COMPLETED';
          return { ...r, status: next };
        }
        return r;
      })
    );
  };

  // Housekeeping Toggle
  const toggleHousekeeping = (roomId) => {
    setRooms((prev) =>
      prev.map((r) => {
        if (r.id === roomId) {
          const nextHk = r.housekeeping === 'CLEAN_READY' ? 'DIRTY_NEEDS_CLEANING' : 'CLEAN_READY';
          return { ...r, housekeeping: nextHk };
        }
        return r;
      })
    );
  };

  // Inventory Restock
  const handleRestock = async (itemId) => {
    setInventory((prev) =>
      prev.map((item) => {
        if (item.id === itemId) {
          const newStock = item.stock + 50;
          return { ...item, stock: newStock, alert: newStock <= item.reorder };
        }
        return item;
      })
    );
    await api.updateStock(itemId, 'STOCK_IN', 50, 'Staff Portal restock');
  };

  // Manual KOT Submission
  const handleCreateManualKot = (e) => {
    e.preventDefault();
    const newKot = {
      id: Date.now(),
      orderNumber: `RSO-${Date.now().toString().slice(-8)}`,
      kotNumber: `KOT-${Math.floor(80000 + Math.random() * 10000)}`,
      roomNumber: newKotRoom,
      hotelName: 'The Grand Palace, Mumbai',
      status: 'ORDERED',
      station: 'CURRY',
      totalAmount: 480.0,
      items: [{ name: newKotItem, quantity: Number(newKotQty) }],
      specialInstructions: newKotNotes || 'Phone order received via suite intercom',
      orderedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      priority: 'HIGH'
    };
    setKotOrders([newKot, ...kotOrders]);
    setIsNewKotModalOpen(false);
    setNewKotNotes('');
  };

  const filteredOrders = kotOrders.filter((o) => {
    if (stationFilter === 'ALL') return true;
    return o.station === stationFilter;
  });

  return (
    <div style={{ maxWidth: '1440px', margin: '0 auto', padding: '30px 20px 80px' }}>
      
      {/* =========================================================================
          STAFF OPERATIONS COMMAND BANNER
          ========================================================================= */}
      <div className="glass-panel" style={{
        padding: '24px 28px',
        marginBottom: '26px',
        border: '1px solid rgba(56, 189, 248, 0.35)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '20px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{
            width: '52px',
            height: '52px',
            borderRadius: '12px',
            background: 'rgba(56, 189, 248, 0.15)',
            border: '1px solid rgba(56, 189, 248, 0.4)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <ChefHat size={30} color="var(--info)" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#fff' }}>
                Staff & Kitchen Operations Portal
              </h1>
              <span className="badge badge-info" style={{ fontSize: '0.74rem' }}>
                👨‍🍳 Kitchen Station 1 & Concierge Hub
              </span>
            </div>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.82rem', marginTop: '2px' }}>
              Active Staff: <strong>Rajiv Sharma (Supervisor)</strong> • Shift: Morning Operations (07:00 - 15:30)
            </p>
          </div>
        </div>

        {/* Live Station Counters */}
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '8px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Active KOTs</div>
            <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--warning)' }}>
              {kotOrders.filter(o => o.status !== 'DELIVERED').length} Active
            </div>
          </div>
          <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '8px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Butler Requests</div>
            <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--gold-light)' }}>
              {butlerRequests.filter(r => r.status !== 'COMPLETED').length} Pending
            </div>
          </div>
          <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '8px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Suites to Clean</div>
            <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--danger)' }}>
              {rooms.filter(r => r.housekeeping === 'DIRTY_NEEDS_CLEANING').length} Suites
            </div>
          </div>
        </div>
      </div>

      {/* =========================================================================
          OPERATIONAL SUB-TABS & ACTION BAR
          ========================================================================= */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '12px',
        borderBottom: '1px solid var(--border-subtle)',
        paddingBottom: '14px',
        marginBottom: '24px'
      }}>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button
            onClick={() => setActiveStaffTab('KOT')}
            className={activeStaffTab === 'KOT' ? 'btn-gold' : 'btn-ghost'}
            style={{ fontSize: '0.86rem', padding: '8px 16px' }}
          >
            <UtensilsCrossed size={15} />
            <span>Kitchen Order Tickets ({kotOrders.length})</span>
          </button>

          <button
            onClick={() => setActiveStaffTab('BUTLER')}
            className={activeStaffTab === 'BUTLER' ? 'btn-gold' : 'btn-ghost'}
            style={{ fontSize: '0.86rem', padding: '8px 16px' }}
          >
            <Bell size={15} />
            <span>Guest Butler Requests ({butlerRequests.length})</span>
          </button>

          <button
            onClick={() => setActiveStaffTab('HOUSEKEEPING')}
            className={activeStaffTab === 'HOUSEKEEPING' ? 'btn-gold' : 'btn-ghost'}
            style={{ fontSize: '0.86rem', padding: '8px 16px' }}
          >
            <Sparkles size={15} />
            <span>Housekeeping Turnaround</span>
          </button>

          <button
            onClick={() => setActiveStaffTab('INVENTORY')}
            className={activeStaffTab === 'INVENTORY' ? 'btn-gold' : 'btn-ghost'}
            style={{ fontSize: '0.86rem', padding: '8px 16px' }}
          >
            <PackageCheck size={15} />
            <span>Consumables Restock</span>
          </button>
        </div>

        {/* Action button: Create manual KOT */}
        <button
          onClick={() => setIsNewKotModalOpen(true)}
          className="btn-outline-gold"
          style={{ padding: '8px 16px', fontSize: '0.82rem' }}
        >
          <Plus size={15} />
          <span>New Manual KOT (Intercom)</span>
        </button>
      </div>

      {/* =========================================================================
          TAB 1: KITCHEN ORDER TICKETS (KOT) KANBAN WORKFLOW
          ========================================================================= */}
      {activeStaffTab === 'KOT' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          
          {/* Station Filter Bar */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
              <Filter size={14} color="var(--gold-primary)" />
              <span>Station View:</span>
              <button
                onClick={() => setStationFilter('ALL')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  background: stationFilter === 'ALL' ? 'var(--gold-primary)' : 'rgba(255,255,255,0.05)',
                  color: stationFilter === 'ALL' ? '#0B1120' : 'var(--text-muted)'
                }}
              >
                All Stations
              </button>
              <button
                onClick={() => setStationFilter('TANDOOR')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  background: stationFilter === 'TANDOOR' ? 'var(--gold-primary)' : 'rgba(255,255,255,0.05)',
                  color: stationFilter === 'TANDOOR' ? '#0B1120' : 'var(--text-muted)'
                }}
              >
                Tandoor Grill
              </button>
              <button
                onClick={() => setStationFilter('CURRY')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  background: stationFilter === 'CURRY' ? 'var(--gold-primary)' : 'rgba(255,255,255,0.05)',
                  color: stationFilter === 'CURRY' ? '#0B1120' : 'var(--text-muted)'
                }}
              >
                Curry & Rice
              </button>
              <button
                onClick={() => setStationFilter('SNACKS')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  background: stationFilter === 'SNACKS' ? 'var(--gold-primary)' : 'rgba(255,255,255,0.05)',
                  color: stationFilter === 'SNACKS' ? '#0B1120' : 'var(--text-muted)'
                }}
              >
                Snacks & Chai
              </button>
            </div>

            <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>
              Total Active: {filteredOrders.length} tickets
            </span>
          </div>

          {/* 4 Column Workflow Grid with Fixed Height & Scroll Guard */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
            
            {/* Column 1: ORDERED */}
            <div style={{ background: 'rgba(15, 23, 42, 0.65)', borderRadius: 'var(--radius-lg)', padding: '16px', border: '1px solid var(--border-subtle)', display: 'flex', flexDirection: 'column' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--info)', textTransform: 'uppercase' }}>
                  1. New Orders ({filteredOrders.filter(o => o.status === 'ORDERED').length})
                </span>
                <span className="badge badge-info" style={{ fontSize: '0.68rem' }}>ORDERED</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
                {filteredOrders.filter(o => o.status === 'ORDERED').map((o) => (
                  <div key={o.id} className="glass-panel" style={{ padding: '16px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--gold-light)', fontWeight: 700, marginBottom: '2px' }}>
                        <span>{o.kotNumber}</span>
                        <span>Suite {o.roomNumber}</span>
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginBottom: '8px' }}>Received: {o.orderedAt}</div>

                      <div style={{ background: 'rgba(0, 0, 0, 0.25)', padding: '10px', borderRadius: 'var(--radius-sm)', marginBottom: '8px' }}>
                        {o.items.map((it, i) => (
                          <div key={i} style={{ fontSize: '0.82rem', color: '#fff', display: 'flex', justifyContent: 'space-between' }}>
                            <span>{it.quantity}x {it.name}</span>
                          </div>
                        ))}
                      </div>

                      {o.specialInstructions && (
                        <div style={{ fontSize: '0.73rem', color: 'var(--warning)', fontStyle: 'italic', marginBottom: '12px' }}>
                          Note: "{o.specialInstructions}"
                        </div>
                      )}
                    </div>

                    <button
                      onClick={() => advanceOrderStatus(o.id)}
                      className="btn-gold"
                      style={{ width: '100%', justifyContent: 'center', padding: '9px', fontSize: '0.8rem', marginTop: '6px' }}
                    >
                      Start Cooking →
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Column 2: PREPARING (NO OVERFLOW - EXPLICIT PADDING) */}
            <div style={{ background: 'rgba(15, 23, 42, 0.65)', borderRadius: 'var(--radius-lg)', padding: '16px', border: '1px solid var(--border-subtle)', display: 'flex', flexDirection: 'column' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--warning)', textTransform: 'uppercase' }}>
                  2. In Kitchen ({filteredOrders.filter(o => o.status === 'PREPARING').length})
                </span>
                <span className="badge badge-warning" style={{ fontSize: '0.68rem' }}>PREPARING</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
                {filteredOrders.filter(o => o.status === 'PREPARING').map((o) => (
                  <div key={o.id} className="glass-panel" style={{ padding: '16px', border: '1px solid rgba(245, 158, 11, 0.35)', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--gold-light)', fontWeight: 700, marginBottom: '2px' }}>
                        <span>{o.kotNumber}</span>
                        <span>Suite {o.roomNumber}</span>
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginBottom: '8px' }}>Cooking since: {o.orderedAt}</div>

                      <div style={{ background: 'rgba(0, 0, 0, 0.25)', padding: '10px', borderRadius: 'var(--radius-sm)', marginBottom: '8px' }}>
                        {o.items.map((it, i) => (
                          <div key={i} style={{ fontSize: '0.82rem', color: '#fff', display: 'flex', justifyContent: 'space-between', marginBottom: '3px' }}>
                            <span>{it.quantity}x {it.name}</span>
                          </div>
                        ))}
                      </div>

                      {o.specialInstructions && (
                        <div style={{ fontSize: '0.73rem', color: 'var(--warning)', fontStyle: 'italic', marginBottom: '12px' }}>
                          Note: "{o.specialInstructions}"
                        </div>
                      )}
                    </div>

                    <button
                      onClick={() => advanceOrderStatus(o.id)}
                      className="btn-gold"
                      style={{ width: '100%', justifyContent: 'center', padding: '9px', fontSize: '0.8rem', marginTop: '6px' }}
                    >
                      Dispatch with Butler →
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Column 3: OUT_FOR_DELIVERY */}
            <div style={{ background: 'rgba(15, 23, 42, 0.65)', borderRadius: 'var(--radius-lg)', padding: '16px', border: '1px solid var(--border-subtle)', display: 'flex', flexDirection: 'column' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--gold-light)', textTransform: 'uppercase' }}>
                  3. In Transit ({filteredOrders.filter(o => o.status === 'OUT_FOR_DELIVERY').length})
                </span>
                <span className="badge badge-gold" style={{ fontSize: '0.68rem' }}>IN TRANSIT</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
                {filteredOrders.filter(o => o.status === 'OUT_FOR_DELIVERY').map((o) => (
                  <div key={o.id} className="glass-panel" style={{ padding: '16px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--gold-light)', fontWeight: 700, marginBottom: '2px' }}>
                        <span>{o.kotNumber}</span>
                        <span>Suite {o.roomNumber}</span>
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-dim)', marginBottom: '8px' }}>With Floor Butler Corps</div>

                      <div style={{ background: 'rgba(0, 0, 0, 0.25)', padding: '10px', borderRadius: 'var(--radius-sm)', marginBottom: '8px' }}>
                        {o.items.map((it, i) => (
                          <div key={i} style={{ fontSize: '0.82rem', color: '#fff', display: 'flex', justifyContent: 'space-between' }}>
                            <span>{it.quantity}x {it.name}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    <button
                      onClick={() => advanceOrderStatus(o.id)}
                      className="btn-gold"
                      style={{ width: '100%', justifyContent: 'center', padding: '9px', fontSize: '0.8rem', marginTop: '6px' }}
                    >
                      Confirm Delivered ✓
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Column 4: DELIVERED */}
            <div style={{ background: 'rgba(15, 23, 42, 0.65)', borderRadius: 'var(--radius-lg)', padding: '16px', border: '1px solid var(--border-subtle)', display: 'flex', flexDirection: 'column' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--success)', textTransform: 'uppercase' }}>
                  4. Completed ({filteredOrders.filter(o => o.status === 'DELIVERED').length})
                </span>
                <span className="badge badge-success" style={{ fontSize: '0.68rem' }}>COMPLETED</span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1 }}>
                {filteredOrders.filter(o => o.status === 'DELIVERED').map((o) => (
                  <div key={o.id} className="glass-panel" style={{ padding: '16px', opacity: 0.85 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 700, marginBottom: '2px' }}>
                      <span>{o.kotNumber}</span>
                      <span>Suite {o.roomNumber}</span>
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '4px', marginTop: '6px' }}>
                      <CheckCircle2 size={15} /> Handed to Guest in Suite
                    </div>
                  </div>
                ))}
              </div>
            </div>

          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 2: GUEST BUTLER & CONCIERGE DISPATCH QUEUE
          ========================================================================= */}
      {activeStaffTab === 'BUTLER' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#fff' }}>
              Live Guest Butler & Concierge Dispatch Queue
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              Requests submitted by sovereign guests from their Guest Portal. Acknowledge and advance through butler workflow.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '18px' }}>
            {butlerRequests.map((req) => (
              <div key={req.id} className="glass-panel" style={{ padding: '22px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <span className="badge badge-gold">
                      Suite {req.suite}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                      Requested: {req.time}
                    </span>
                  </div>

                  <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                    {req.request}
                  </h3>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '14px' }}>
                    Guest: <strong>{req.guest}</strong>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
                    <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)' }}>Status:</span>
                    <span className={req.status === 'COMPLETED' ? 'badge badge-success' : 'badge badge-warning'}>
                      {req.status}
                    </span>
                  </div>
                </div>

                <button
                  onClick={() => advanceButlerStatus(req.id)}
                  className="btn-gold"
                  style={{ width: '100%', justifyContent: 'center', padding: '9px', fontSize: '0.82rem' }}
                >
                  {req.status === 'PENDING' ? 'Acknowledge & Dispatch Staff →' :
                   req.status === 'DISPATCHED' ? 'Mark In-Transit →' :
                   req.status === 'IN_TRANSIT' ? 'Mark Completed ✓' : 'Fulfillment Complete'}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 3: HOUSEKEEPING & SUITE TURNAROUND
          ========================================================================= */}
      {activeStaffTab === 'HOUSEKEEPING' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#fff' }}>
              Floor Housekeeping & Suite Turnaround
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              Toggle suite cleaning status as housekeeping finishes turndown, sanitization, and inspection.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '18px' }}>
            {rooms.map((room) => (
              <div key={room.id} className="glass-panel" style={{ padding: '22px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                  <div>
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Floor {room.floor}</span>
                    <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff' }}>Suite {room.roomNumber}</h3>
                    <div style={{ fontSize: '0.78rem', color: 'var(--gold-light)' }}>{room.type}</div>
                  </div>

                  <span className={room.status === 'OCCUPIED' ? 'badge badge-warning' : 'badge badge-success'}>
                    {room.status}
                  </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', borderTop: '1px solid var(--border-subtle)', borderBottom: '1px solid var(--border-subtle)', marginBottom: '14px', fontSize: '0.82rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Sanitization Status:</span>
                  <span style={{
                    fontWeight: 700,
                    color: room.housekeeping === 'CLEAN_READY' ? 'var(--success)' : 'var(--danger)'
                  }}>
                    {room.housekeeping === 'CLEAN_READY' ? '✓ Clean & Ready' : '⚠ Dirty / Needs Cleaning'}
                  </span>
                </div>

                <button
                  onClick={() => toggleHousekeeping(room.id)}
                  className={room.housekeeping === 'CLEAN_READY' ? 'btn-outline-gold' : 'btn-gold'}
                  style={{ width: '100%', justifyContent: 'center', padding: '9px', fontSize: '0.82rem' }}
                >
                  {room.housekeeping === 'CLEAN_READY' ? 'Mark as Needs Cleaning' : '✓ Mark Cleaned & Ready'}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 4: INVENTORY RESTOCK
          ========================================================================= */}
      {activeStaffTab === 'INVENTORY' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#fff' }}>
              Consumables & Hotel Inventory Restock
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              Restock linens, toiletries, and kitchen staples with one click.
            </p>
          </div>

          <div className="glass-panel" style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-gold)', color: 'var(--gold-light)' }}>
                  <th style={{ padding: '14px 18px' }}>Code</th>
                  <th style={{ padding: '14px 18px' }}>Item Name</th>
                  <th style={{ padding: '14px 18px' }}>Category</th>
                  <th style={{ padding: '14px 18px' }}>Current Stock</th>
                  <th style={{ padding: '14px 18px' }}>Threshold</th>
                  <th style={{ padding: '14px 18px' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {inventory.map((item) => (
                  <tr key={item.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                    <td style={{ padding: '14px 18px', color: 'var(--text-dim)', fontFamily: 'monospace' }}>{item.itemCode}</td>
                    <td style={{ padding: '14px 18px', fontWeight: 600, color: '#fff' }}>{item.name}</td>
                    <td style={{ padding: '14px 18px', color: 'var(--text-muted)' }}>{item.category}</td>
                    <td style={{ padding: '14px 18px' }}>
                      <span style={{
                        fontWeight: 800,
                        color: item.alert ? 'var(--danger)' : 'var(--success)'
                      }}>
                        {item.stock} {item.unit}
                      </span>
                    </td>
                    <td style={{ padding: '14px 18px', color: 'var(--text-dim)' }}>{item.reorder} {item.unit}</td>
                    <td style={{ padding: '14px 18px' }}>
                      <button
                        onClick={() => handleRestock(item.id)}
                        className="btn-outline-gold"
                        style={{ padding: '6px 12px', fontSize: '0.78rem' }}
                      >
                        <Plus size={13} /> Restock (+50)
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* =========================================================================
          MANUAL KOT CREATION MODAL (INTERCOM PHONE ORDER)
          ========================================================================= */}
      {isNewKotModalOpen && (
        <div className="modal-overlay" onClick={() => setIsNewKotModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '520px', padding: '32px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border-gold)', paddingBottom: '14px', marginBottom: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <ChefHat size={22} color="var(--gold-primary)" />
                <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--gold-light)' }}>
                  Create Intercom KOT Order
                </h3>
              </div>
              <button onClick={() => setIsNewKotModalOpen(false)} className="btn-ghost" style={{ fontSize: '1.2rem', color: 'var(--text-muted)' }}>✕</button>
            </div>

            <form onSubmit={handleCreateManualKot} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                  Suite Number *
                </label>
                <select
                  value={newKotRoom}
                  onChange={(e) => setNewKotRoom(e.target.value)}
                  className="input-luxury"
                >
                  <option value="101">Suite 101 (Lord Alexander)</option>
                  <option value="102">Suite 102</option>
                  <option value="201">Suite 201 (Lady Eleanor)</option>
                  <option value="202">Suite 202</option>
                  <option value="301">Suite 301 (Diplomat Vance)</option>
                  <option value="302">Suite 302</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                  Culinary Dish *
                </label>
                <select
                  value={newKotItem}
                  onChange={(e) => setNewKotItem(e.target.value)}
                  className="input-luxury"
                >
                  {MOCK_FOOD_ITEMS.map((f) => (
                    <option key={f.id} value={f.name}>
                      {f.name} (₹{f.price})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                  Quantity
                </label>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={newKotQty}
                  onChange={(e) => setNewKotQty(e.target.value)}
                  className="input-luxury"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '6px' }}>
                  Guest Special Notes (Spice, Allergies)
                </label>
                <input
                  type="text"
                  placeholder="e.g. Extra mint chutney, deliver by 9 PM"
                  value={newKotNotes}
                  onChange={(e) => setNewKotNotes(e.target.value)}
                  className="input-luxury"
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  onClick={() => setIsNewKotModalOpen(false)}
                  className="btn-ghost"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-gold"
                >
                  Dispatch to Kitchen Pipeline
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};

export default StaffDashboard;
