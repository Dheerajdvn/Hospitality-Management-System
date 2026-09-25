import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Activity, 
  Server, 
  TrendingUp, 
  Building2, 
  CreditCard, 
  Zap, 
  Lock, 
  BarChart3
} from 'lucide-react';
import { MOCK_HOTELS } from '../api/apiClient';

const AdminDashboard = () => {
  const [activeAdminTab, setActiveAdminTab] = useState('ANALYTICS'); // 'ANALYTICS' | 'FLEET' | 'TELEMETRY' | 'AUDIT'

  const microservices = [
    { name: 'API Gateway', port: 8080, tech: 'Spring Cloud Gateway / Netty', status: 'ACTIVE', role: 'Perimeter Reverse Proxy & JWT Verification', memory: '184 MB', uptime: '99.99%' },
    { name: 'Auth Service', port: 8081, tech: 'Spring Boot 3.3 / JWT / BCrypt', status: 'HEALTHY', role: 'Security Tokens & User Authentication', memory: '162 MB', uptime: '99.98%' },
    { name: 'Customer Service', port: 8082, tech: 'Spring Boot 3.3 / PostgreSQL', status: 'HEALTHY', role: 'Guest Profiles & KYC Management', memory: '145 MB', uptime: '99.95%' },
    { name: 'Hotel Service', port: 8083, tech: 'Spring Boot 3.3 / Redis 7 Cache', status: 'HEALTHY', role: 'Hotel Catalog & Cache-Aside Layer', memory: '198 MB', uptime: '99.99%' },
    { name: 'Room Service', port: 8084, tech: 'Spring Boot 3.3 / Optimistic Lock', status: 'HEALTHY', role: 'Room Inventory & Versioning', memory: '155 MB', uptime: '99.96%' },
    { name: 'Booking Service', port: 8085, tech: 'Spring Boot 3.3 / Overlap Guard', status: 'HEALTHY', role: 'Reservations & Expiry Sweep Engine', memory: '178 MB', uptime: '99.97%' },
    { name: 'Food Service', port: 8086, tech: 'Spring Boot 3.3 / JPA Spec', status: 'HEALTHY', role: 'Culinary Menus & Batch Resolution', memory: '142 MB', uptime: '99.94%' },
    { name: 'Room Service Mgmt', port: 8087, tech: 'Spring Boot 3.3 / KOT Machine', status: 'HEALTHY', role: 'In-Room Dining Orders & Lifecycles', memory: '168 MB', uptime: '99.95%' },
    { name: 'Billing Service', port: 8088, tech: 'Spring Boot 3.3 / 18% GST Engine', status: 'HEALTHY', role: 'GST Invoicing & Payment Processing', memory: '152 MB', uptime: '99.98%' },
    { name: 'Inventory Service', port: 8089, tech: 'Spring Boot 3.3 / Audit Ledger', status: 'HEALTHY', role: 'Hotel Supplies & Reorder Thresholds', memory: '139 MB', uptime: '99.92%' },
    { name: 'Notification Service', port: 8090, tech: 'Spring Boot 3.3 / Kafka Engine', status: 'HEALTHY', role: 'Email & SMS Dispatchers', memory: '160 MB', uptime: '99.96%' },
  ];

  // Audit Log Events
  const auditLogs = [
    { id: 1, event: 'JWT_AUTH_SUCCESS', principal: 'admin (ROLE_ADMIN)', ip: '127.0.0.1', service: 'auth-service (:8081)', timestamp: 'Just now' },
    { id: 2, event: 'ROOM_BOOKING_RESERVED', principal: 'customer (ROLE_CUSTOMER)', ip: '127.0.0.1', service: 'booking-service (:8085)', timestamp: '4 mins ago' },
    { id: 3, event: 'KOT_TICKET_DISPATCHED', principal: 'staff (ROLE_STAFF)', ip: '127.0.0.1', service: 'room-service-mgmt (:8087)', timestamp: '12 mins ago' },
    { id: 4, event: 'INVENTORY_RESTOCK_COMMITTED', principal: 'ADMIN_PORTAL', ip: '127.0.0.1', service: 'inventory-service (:8089)', timestamp: '28 mins ago' },
    { id: 5, event: 'REDIS_CACHE_SYNC', principal: 'SYSTEM', ip: '127.0.0.1', service: 'hotel-service (:8083)', timestamp: '35 mins ago' },
  ];

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '40px 20px 80px' }}>
      
      {/* =========================================================================
          EXECUTIVE ADMIN HEADER BANNER
          ========================================================================= */}
      <div className="glass-panel" style={{
        padding: '28px 32px',
        marginBottom: '32px',
        border: '1px solid var(--border-gold)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '20px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{
            width: '56px',
            height: '56px',
            borderRadius: '14px',
            background: 'var(--gold-gradient)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: 'var(--gold-glow)'
          }}>
            <ShieldCheck size={32} color="#0B1120" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <h1 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#fff' }}>
                Executive Operations & Analytics
              </h1>
              <span className="badge badge-gold">
                👑 Super Admin
              </span>
            </div>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.86rem', marginTop: '2px' }}>
              Portfolio Revenue • Hotel Fleet Occupancy • 11 Microservices Telemetry
            </p>
          </div>
        </div>

        {/* Global Live Status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <span className="badge badge-success" style={{ padding: '8px 14px', fontSize: '0.82rem' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--success)', display: 'inline-block', marginRight: '6px' }} />
            All 11 Microservices Active
          </span>
          <span className="badge badge-info" style={{ padding: '8px 14px', fontSize: '0.82rem' }}>
            Redis Cache: 94.2% Hit Rate
          </span>
        </div>
      </div>

      {/* =========================================================================
          KEY REVENUE & PORTFOLIO METRICS
          ========================================================================= */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px', marginBottom: '32px' }}>
        
        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Daily Portfolio Revenue</span>
            <TrendingUp size={18} color="var(--gold-primary)" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--gold-light)' }}>
            ₹1,42,850
          </div>
          <div style={{ fontSize: '0.76rem', color: 'var(--success)', marginTop: '4px' }}>
            ↑ +14.6% vs previous week
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Average Occupancy Rate</span>
            <Building2 size={18} color="var(--info)" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#fff' }}>
            84.5%
          </div>
          <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            24 of 28 Suites Reserved
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>RevPAR (Rev / Avail Room)</span>
            <CreditCard size={18} color="var(--success)" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--gold-light)' }}>
            ₹11,920
          </div>
          <div style={{ fontSize: '0.76rem', color: 'var(--success)', marginTop: '4px' }}>
            ADR: ₹14,100
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Gateway Throughput</span>
            <Zap size={18} color="var(--warning)" />
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#fff' }}>
            3,420 <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>req/min</span>
          </div>
          <div style={{ fontSize: '0.76rem', color: 'var(--success)', marginTop: '4px' }}>
            Average Latency: 14.2ms
          </div>
        </div>

      </div>

      {/* =========================================================================
          SUB-TAB NAVIGATION
          ========================================================================= */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        borderBottom: '1px solid var(--border-subtle)',
        marginBottom: '28px'
      }}>
        <button
          onClick={() => setActiveAdminTab('ANALYTICS')}
          className={activeAdminTab === 'ANALYTICS' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <BarChart3 size={16} />
          <span>Executive Analytics</span>
        </button>

        <button
          onClick={() => setActiveAdminTab('FLEET')}
          className={activeAdminTab === 'FLEET' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <Building2 size={16} />
          <span>Hotel & Room Fleet</span>
        </button>

        <button
          onClick={() => setActiveAdminTab('TELEMETRY')}
          className={activeAdminTab === 'TELEMETRY' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <Server size={16} />
          <span>DevOps Telemetry (:8080 - :8090)</span>
        </button>

        <button
          onClick={() => setActiveAdminTab('AUDIT')}
          className={activeAdminTab === 'AUDIT' ? 'btn-gold' : 'btn-ghost'}
          style={{ fontSize: '0.9rem' }}
        >
          <Lock size={16} />
          <span>Security & Audit Stream</span>
        </button>
      </div>

      {/* =========================================================================
          TAB 1: EXECUTIVE ANALYTICS
          ========================================================================= */}
      {activeAdminTab === 'ANALYTICS' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>
          
          <div className="glass-panel" style={{ padding: '26px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--gold-light)', marginBottom: '16px' }}>
              Revenue Contribution by Sanctuary
            </h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '6px' }}>
                  <span>The Grand Palace Resort & Spa (Mumbai)</span>
                  <span style={{ fontWeight: 700, color: 'var(--gold-light)' }}>₹78,400 (55%)</span>
                </div>
                <div style={{ height: '8px', background: 'rgba(255,255,255,0.06)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                  <div style={{ width: '55%', height: '100%', background: 'var(--gold-gradient)' }} />
                </div>
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '6px' }}>
                  <span>Ocean View Coastal Retreat (Goa)</span>
                  <span style={{ fontWeight: 700, color: 'var(--gold-light)' }}>₹36,250 (25%)</span>
                </div>
                <div style={{ height: '8px', background: 'rgba(255,255,255,0.06)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                  <div style={{ width: '25%', height: '100%', background: 'var(--info)' }} />
                </div>
              </div>

              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '6px' }}>
                  <span>Himalayan Heritage Sanctuary (Shimla)</span>
                  <span style={{ fontWeight: 700, color: 'var(--gold-light)' }}>₹28,200 (20%)</span>
                </div>
                <div style={{ height: '8px', background: 'rgba(255,255,255,0.06)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                  <div style={{ width: '20%', height: '100%', background: 'var(--success)' }} />
                </div>
              </div>
            </div>
          </div>

          <div className="glass-panel" style={{ padding: '26px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--gold-light)', marginBottom: '16px' }}>
              Service Revenue Streams
            </h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', fontSize: '0.88rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '10px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>Suite Accommodations (Tariffs):</span>
                <span style={{ fontWeight: 700, color: '#fff' }}>₹1,18,500</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '10px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>In-Room Dining (KOT Food Orders):</span>
                <span style={{ fontWeight: 700, color: '#fff' }}>₹16,420</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '10px', borderBottom: '1px solid var(--border-subtle)' }}>
                <span style={{ color: 'var(--text-muted)' }}>18% GST (CGST + SGST) Collected:</span>
                <span style={{ fontWeight: 700, color: 'var(--gold-light)' }}>₹24,300</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '4px' }}>
                <span style={{ fontWeight: 800, color: 'var(--gold-light)' }}>Net Settled Portfolio Revenue:</span>
                <span style={{ fontWeight: 800, color: 'var(--gold-light)', fontSize: '1.05rem' }}>₹1,59,220</span>
              </div>
            </div>
          </div>

        </div>
      )}

      {/* =========================================================================
          TAB 2: HOTEL & SUITES FLEET
          ========================================================================= */}
      {activeAdminTab === 'FLEET' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '24px' }}>
          {MOCK_HOTELS.map((hotel) => (
            <div key={hotel.id} className="glass-panel" style={{ padding: '24px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                <div>
                  <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fff' }}>{hotel.name}</h3>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', marginTop: '2px' }}>{hotel.address}</div>
                </div>
                <span className="badge badge-gold">{hotel.starRating} ★</span>
              </div>

              <div style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '12px',
                padding: '14px 0',
                borderTop: '1px solid var(--border-subtle)',
                borderBottom: '1px solid var(--border-subtle)',
                marginBottom: '16px',
                fontSize: '0.82rem'
              }}>
                <div>
                  <div style={{ color: 'var(--text-dim)' }}>Occupancy</div>
                  <div style={{ fontWeight: 700, color: 'var(--success)' }}>
                    {hotel.id === 1 ? '92% (11/12 Suites)' : hotel.id === 2 ? '78% (7/9 Suites)' : '83% (6/7 Suites)'}
                  </div>
                </div>
                <div>
                  <div style={{ color: 'var(--text-dim)' }}>Starting Price</div>
                  <div style={{ fontWeight: 700, color: 'var(--gold-light)' }}>₹{hotel.startingPrice.toLocaleString()}/night</div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {hotel.amenities.map((a, i) => (
                  <span key={i} style={{ fontSize: '0.72rem', padding: '2px 8px', borderRadius: 'var(--radius-full)', background: 'rgba(255,255,255,0.04)', color: 'var(--text-dim)' }}>
                    {a}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* =========================================================================
          TAB 3: DEVOPS TELEMETRY (11 MICROSERVICES)
          ========================================================================= */}
      {activeAdminTab === 'TELEMETRY' && (
        <div className="glass-panel" style={{ padding: '28px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '22px' }}>
            <div>
              <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#fff' }}>
                Distributed Microservices Fleet
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Ports :8080 through :8090 with Micrometer Prometheus integration and Spring Boot DevTools.
              </p>
            </div>
            <div className="badge badge-success">
              <Activity size={12} /> 11/11 Instances Operational
            </div>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.84rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-gold)', color: 'var(--gold-light)' }}>
                  <th style={{ padding: '12px 14px' }}>Microservice</th>
                  <th style={{ padding: '12px 14px' }}>Port</th>
                  <th style={{ padding: '12px 14px' }}>Tech Stack</th>
                  <th style={{ padding: '12px 14px' }}>Status</th>
                  <th style={{ padding: '12px 14px' }}>Memory</th>
                  <th style={{ padding: '12px 14px' }}>Uptime</th>
                  <th style={{ padding: '12px 14px' }}>Architectural Role</th>
                </tr>
              </thead>
              <tbody>
                {microservices.map((svc, idx) => (
                  <tr key={idx} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                    <td style={{ padding: '12px 14px', fontWeight: 700, color: '#fff' }}>{svc.name}</td>
                    <td style={{ padding: '12px 14px', color: 'var(--gold-light)', fontFamily: 'monospace' }}>:{svc.port}</td>
                    <td style={{ padding: '12px 14px', color: 'var(--text-dim)', fontSize: '0.78rem' }}>{svc.tech}</td>
                    <td style={{ padding: '12px 14px' }}>
                      <span className="badge badge-success" style={{ fontSize: '0.72rem' }}>
                        {svc.status}
                      </span>
                    </td>
                    <td style={{ padding: '12px 14px', color: 'var(--text-muted)' }}>{svc.memory}</td>
                    <td style={{ padding: '12px 14px', color: 'var(--success)' }}>{svc.uptime}</td>
                    <td style={{ padding: '12px 14px', color: 'var(--text-dim)', fontSize: '0.78rem' }}>{svc.role}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 4: SECURITY & AUDIT STREAM
          ========================================================================= */}
      {activeAdminTab === 'AUDIT' && (
        <div className="glass-panel" style={{ padding: '28px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
            <div>
              <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#fff' }}>
                Real-Time Security & Dispatch Audit
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Authentication tokens, role checks, and distributed transaction events.
              </p>
            </div>
            <div className="badge badge-gold">
              <Lock size={12} /> RBAC Enforced
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {auditLogs.map((log) => (
              <div key={log.id} style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '14px 18px',
                background: 'rgba(255, 255, 255, 0.03)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-subtle)',
                fontSize: '0.84rem'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <span style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    background: log.event.includes('SUCCESS') ? 'var(--success)' : 'var(--gold-primary)'
                  }} />
                  <div>
                    <span style={{ fontWeight: 700, color: 'var(--gold-light)', fontFamily: 'monospace' }}>
                      {log.event}
                    </span>
                    <div style={{ fontSize: '0.76rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                      Actor: {log.principal} • Origin: {log.ip} • Route: {log.service}
                    </div>
                  </div>
                </div>

                <span style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>
                  {log.timestamp}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

    </div>
  );
};

export default AdminDashboard;
