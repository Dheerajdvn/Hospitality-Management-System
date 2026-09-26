# Grand Luxe — Luxury Hospitality Frontend Portal
### React 18 + Vite + Glassmorphism Luxury Design System

---

## 1. Overview
The **Grand Luxe Frontend Portal** is a high-performance Single Page Application (SPA) built with React 18 and Vite. It serves as both the guest reservation portal and the back-office operations management dashboard for the Grand Luxe 5-star hotel chain.

- **Local Port**: `http://localhost:5173` (or `http://127.0.0.1:5173`)
- **Backend API Gateway**: `http://localhost:8080` (Spring Cloud Gateway with Eureka dynamic discovery)
- **Service Registry**: `http://localhost:8761` (Spring Cloud Netflix Eureka Dashboard)

---

## 2. Core Capabilities
1. **Guest Portal**:
   - Browse 5-star luxury hotels, search by location and amenities.
   - Real-time room inventory lookup with suite details and pricing.
   - Mathematical date-overlap reservation engine with 15-minute hold timer.
   - In-room dining catalog ordering with live Kitchen Order Ticket (KOT) tracking.
   - GST-compliant 18% tax invoice calculation and simulated card payments.
2. **Operations Dashboard**:
   - Live Room Service & Housekeeping Kanban dispatch board.
   - Inventory stock movement audit ledger and low-stock reorder thresholds.
   - Room status transition management (Clean, Inspected, Occupied, Maintenance).

---

## 3. Demo Credentials
- **Admin**: `admin` / `admin123`
- **Guest / Customer**: `customer` / `customer123`

---

## 4. Development Commands
```bash
# Install dependencies
npm install

# Start development server on port 5173
npm run dev

# Build production bundle
npm run build
```
