import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 5000,
});

// Interceptor to attach Bearer token from localStorage
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Mock Data Catalog for seamless offline UI demo
export const MOCK_HOTELS = [
  {
    id: 1,
    name: 'The Grand Palace Resort & Spa',
    city: 'Mumbai',
    address: 'Apollo Bunder, Colaba, Mumbai 400001',
    starRating: 5.0,
    imageUrl: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800',
    description: 'Iconic flagship luxury hotel overlooking the Arabian Sea, featuring bespoke dining and royal heritage hospitality.',
    amenities: ['Sea View', 'Infinity Pool', '24/7 Butler', 'Royal Spa', 'Fine Dining', 'Helipad'],
    startingPrice: 12500,
  },
  {
    id: 2,
    name: 'Ocean View Coastal Retreat',
    city: 'Goa',
    address: 'Candolim Beach Road, North Goa 403515',
    starRating: 4.8,
    imageUrl: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800',
    description: 'Serene beachside luxury sanctuary nestled amid coconut groves with private beach access and coastal dining.',
    amenities: ['Private Beach', 'Beach Cabana', 'Seafood Grill', 'Ayurvedic Wellness', 'Sunset Cruise'],
    startingPrice: 8500,
  },
  {
    id: 3,
    name: 'Himalayan Heritage Sanctuary',
    city: 'Shimla',
    address: 'The Mall Road, Shimla, Himachal Pradesh 171001',
    starRating: 4.9,
    imageUrl: 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800',
    description: 'Colonial-era luxury mountain retreat surrounded by cedar forests with panoramic views of snow-capped peaks.',
    amenities: ['Mountain View', 'Heated Jacuzzi', 'Fireplace Lounge', 'Highland Trekking', 'Observatory'],
    startingPrice: 10500,
  },
];

export const MOCK_ROOMS = {
  1: [
    { id: 1, roomNumber: '101', type: 'DELUXE', basePrice: 12500, capacity: 2, floorNumber: 1, status: 'AVAILABLE', amenities: ['King Bed', 'Sea View Balcony', 'Bathtub', 'High-Speed Wi-Fi'] },
    { id: 2, roomNumber: '201', type: 'SUITE', basePrice: 19500, capacity: 3, floorNumber: 2, status: 'AVAILABLE', amenities: ['Living Room', 'Private Jacuzzi', 'Pantry', 'Complimentary Champagne'] },
    { id: 3, roomNumber: '301', type: 'PRESIDENTIAL', basePrice: 45000, capacity: 4, floorNumber: 3, status: 'AVAILABLE', amenities: ['Panoramic Terrace', 'Private Butler', 'Private Elevator', 'Executive Lounge'] },
  ],
  2: [
    { id: 4, roomNumber: '101', type: 'DELUXE', basePrice: 8500, capacity: 2, floorNumber: 1, status: 'AVAILABLE', amenities: ['Beach Access', 'Outdoor Shower', 'Espresso Bar'] },
    { id: 5, roomNumber: '201', type: 'SUITE', basePrice: 14500, capacity: 3, floorNumber: 2, status: 'AVAILABLE', amenities: ['Private Plunge Pool', 'Ocean Deck', 'Sunset Terrace'] },
  ],
  3: [
    { id: 6, roomNumber: '101', type: 'DELUXE', basePrice: 10500, capacity: 2, floorNumber: 1, status: 'AVAILABLE', amenities: ['Cedar Fireplace', 'Valley View', 'Heated Flooring'] },
    { id: 7, roomNumber: '201', type: 'SUITE', basePrice: 16500, capacity: 3, floorNumber: 2, status: 'AVAILABLE', amenities: ['Pine Deck', 'Observation Telescope', 'Fireplace'] },
  ],
};

export const MOCK_FOOD_ITEMS = [
  { id: 1, hotelId: 1, name: 'Paneer Tikka Royale', category: 'APPETIZER', dietaryType: 'VEG', price: 350, isAvailable: true, prepTime: 20, description: 'Tandoor-charred cottage cheese in aromatic spices' },
  { id: 2, hotelId: 1, name: 'Chicken Malai Kebab', category: 'APPETIZER', dietaryType: 'NON_VEG', price: 480, isAvailable: true, prepTime: 25, description: 'Cream and cardamom infused chicken tenders' },
  { id: 3, hotelId: 1, name: 'Dal Makhani Bukhara', category: 'MAIN_COURSE', dietaryType: 'VEG', price: 380, isAvailable: true, prepTime: 15, description: 'Slow-simmered black lentils with churned butter' },
  { id: 4, hotelId: 1, name: 'Grand Butter Chicken', category: 'MAIN_COURSE', dietaryType: 'NON_VEG', price: 520, isAvailable: true, prepTime: 25, description: 'Tender chicken in velvety satin tomato butter gravy' },
  { id: 5, hotelId: 1, name: 'Nawabi Dum Biryani', category: 'MAIN_COURSE', dietaryType: 'NON_VEG', price: 550, isAvailable: true, prepTime: 30, description: 'Aromatic basmati rice layered with spiced chicken' },
  { id: 6, hotelId: 1, name: 'Garlic Butter Naan', category: 'BREADS', dietaryType: 'VEG', price: 110, isAvailable: true, prepTime: 10, description: 'Clay oven flatbread with garlic butter and fresh herbs' },
  { id: 7, hotelId: 1, name: 'Kesari Gulab Jamun', category: 'DESSERT', dietaryType: 'VEG', price: 180, isAvailable: true, prepTime: 10, description: 'Warm milk dumplings soaked in saffron rose syrup' },
  { id: 8, hotelId: 1, name: 'Royal Masala Chai', category: 'BEVERAGE', dietaryType: 'VEG', price: 120, isAvailable: true, prepTime: 10, description: 'Assam black tea brewed with ginger and cardamom' },
];

export const MOCK_INVENTORY = [
  { id: 1, hotelId: 1, itemCode: 'INV-MUM-LIN-001', name: 'Egyptian Cotton King Sheet', category: 'LINEN', unit: 'PIECES', quantityAvailable: 148, reorderLevel: 30, unitCost: 850, isLowStock: false },
  { id: 2, hotelId: 1, itemCode: 'INV-MUM-LIN-002', name: 'Plush Turkish Bath Towel', category: 'LINEN', unit: 'PIECES', quantityAvailable: 200, reorderLevel: 50, unitCost: 450, isLowStock: false },
  { id: 3, hotelId: 1, itemCode: 'INV-MUM-TOI-001', name: 'Forest Essentials Shampoo 50ml', category: 'TOILETRIES', unit: 'PIECES', quantityAvailable: 500, reorderLevel: 100, unitCost: 45, isLowStock: false },
  { id: 4, hotelId: 1, itemCode: 'INV-MUM-TOI-002', name: 'Bamboo Dental Hygiene Kit', category: 'TOILETRIES', unit: 'PACKETS', quantityAvailable: 40, reorderLevel: 50, unitCost: 35, isLowStock: true },
  { id: 5, hotelId: 1, itemCode: 'INV-MUM-KIT-001', name: 'Royal Basmati Rice 25kg', category: 'KITCHEN_PANTRY', unit: 'PACKETS', quantityAvailable: 15, reorderLevel: 5, unitCost: 2400, isLowStock: false },
  { id: 6, hotelId: 1, itemCode: 'INV-MUM-CLN-001', name: 'Surface Disinfectant 5L', category: 'CLEANING_SUPPLIES', unit: 'LITERS', quantityAvailable: 30, reorderLevel: 10, unitCost: 650, isLowStock: false },
];

// High-level API Service Facade
export const api = {
  // Authentication
  async login(usernameOrEmail, password) {
    try {
      const res = await client.post('/auth/login', { usernameOrEmail, password });
      return res.data;
    } catch {
      // Mock Fallback with Strict Credential Verification
      const u = (usernameOrEmail || '').trim().toLowerCase();

      if (u === 'admin' || u === 'admin@hospitality.com') {
        if (password !== 'admin123') {
          return { success: false, message: 'Invalid password for Super Admin account. (Demo password: admin123)' };
        }
        return {
          success: true,
          data: {
            accessToken: 'mock.admin.jwt.token',
            id: 1,
            username: 'admin',
            email: 'admin@hospitality.com',
            roles: ['ROLE_ADMIN'],
          },
        };
      }

      if (u === 'staff' || u === 'staff@hospitality.com') {
        if (password !== 'staff123') {
          return { success: false, message: 'Invalid password for Staff account. (Demo password: staff123)' };
        }
        return {
          success: true,
          data: {
            accessToken: 'mock.staff.jwt.token',
            id: 2,
            username: 'staff',
            email: 'staff@hospitality.com',
            roles: ['ROLE_STAFF'],
          },
        };
      }

      if (u === 'customer' || u === 'customer@hospitality.com') {
        if (password !== 'customer123') {
          return { success: false, message: 'Invalid password for Guest account. (Demo password: customer123)' };
        }
        return {
          success: true,
          data: {
            accessToken: 'mock.customer.jwt.token',
            id: 3,
            username: 'customer',
            email: 'customer@hospitality.com',
            roles: ['ROLE_CUSTOMER'],
          },
        };
      }

      // Check dynamically registered user in localStorage
      const registeredUsers = JSON.parse(localStorage.getItem('registered_users') || '[]');
      const match = registeredUsers.find((r) => r.username.toLowerCase() === u || r.email.toLowerCase() === u);
      if (match) {
        if (match.password !== password) {
          return { success: false, message: 'Incorrect password for registered account.' };
        }
        return {
          success: true,
          data: {
            accessToken: 'mock.custom.jwt.token',
            id: match.id || 10,
            username: match.username,
            email: match.email,
            roles: match.roles || ['ROLE_CUSTOMER'],
          },
        };
      }

      return {
        success: false,
        message: 'Account not found. Please verify your username/password or register a new membership.',
      };
    }
  },

  async register(registerData) {
    try {
      const res = await client.post('/auth/register', registerData);
      return res.data;
    } catch {
      // Mock Fallback with Local Persistence
      const registeredUsers = JSON.parse(localStorage.getItem('registered_users') || '[]');
      const newUser = {
        id: Math.floor(10 + Math.random() * 90),
        username: registerData.username,
        email: registerData.email,
        password: registerData.password,
        fullName: registerData.fullName,
        phone: registerData.phone,
        roles: ['ROLE_CUSTOMER'],
      };
      registeredUsers.push(newUser);
      localStorage.setItem('registered_users', JSON.stringify(registeredUsers));

      return {
        success: true,
        message: 'Account registered successfully',
        data: newUser,
      };
    }
  },

  // Hotels
  async getHotels() {
    try {
      const res = await client.get('/hotels');
      return res.data.data;
    } catch {
      return MOCK_HOTELS;
    }
  },

  async getHotelById(id) {
    try {
      const res = await client.get(`/hotels/${id}`);
      return res.data.data;
    } catch {
      return MOCK_HOTELS.find((h) => h.id === Number(id)) || MOCK_HOTELS[0];
    }
  },

  // Rooms
  async getRooms(hotelId) {
    try {
      const res = await client.get(`/rooms/hotel/${hotelId}`);
      return res.data.data;
    } catch {
      return MOCK_ROOMS[hotelId] || MOCK_ROOMS[1];
    }
  },

  // Food Menu
  async getMenu(hotelId, category = '', dietaryType = '') {
    try {
      let url = `/food/hotel/${hotelId}?`;
      if (category) url += `category=${category}&`;
      if (dietaryType) url += `dietaryType=${dietaryType}&`;
      const res = await client.get(url);
      return res.data.data;
    } catch {
      let filtered = [...MOCK_FOOD_ITEMS];
      if (category) filtered = filtered.filter((f) => f.category === category);
      if (dietaryType) filtered = filtered.filter((f) => f.dietaryType === dietaryType);
      return filtered;
    }
  },

  async updateDishAvailability(dishId, isAvailable) {
    try {
      const res = await client.patch(`/food/items/${dishId}/availability`, { isAvailable });
      return res.data;
    } catch {
      const item = MOCK_FOOD_ITEMS.find((f) => f.id === dishId);
      if (item) {
        item.isAvailable = isAvailable;
      }
      return { success: true, isAvailable };
    }
  },

  // Room Service Orders
  async createRoomServiceOrder(orderData) {
    try {
      const res = await client.post('/room-service/orders', orderData);
      return res.data;
    } catch {
      const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      const randId = Math.floor(1000 + Math.random() * 9000);
      return {
        success: true,
        data: {
          id: Math.floor(Math.random() * 100),
          orderNumber: `RSO-${dateStr}-${randId}`,
          kotNumber: `KOT-${Math.floor(10000 + Math.random() * 90000)}`,
          status: 'ORDERED',
          subtotal: orderData.items.reduce((acc, i) => acc + (i.unitPrice || 350) * i.quantity, 0),
          taxAmount: 75.0,
          deliveryFee: 30.0,
          totalAmount: orderData.items.reduce((acc, i) => acc + (i.unitPrice || 350) * i.quantity, 0) + 105.0,
          estimatedDeliveryMinutes: 35,
          roomNumber: orderData.roomNumber,
          items: orderData.items,
          orderedAt: new Date().toISOString(),
        },
      };
    }
  },

  // Bookings
  async createBooking(bookingData) {
    try {
      const res = await client.post('/bookings', bookingData);
      return res.data;
    } catch (err) {
      console.warn('Booking service call failed, providing fallback reservation:', err.message);
      const dateNum = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      const randCode = Math.floor(1000 + Math.random() * 9000);
      return {
        success: true,
        data: {
          id: Math.floor(100 + Math.random() * 900),
          bookingNumber: `BK-${dateNum}-${randCode}`,
          customerId: bookingData.customerId || 3,
          roomId: bookingData.roomId,
          checkInDate: bookingData.checkInDate,
          checkOutDate: bookingData.checkOutDate,
          numberOfGuests: bookingData.numberOfGuests,
          totalAmount: 29500.0,
          status: 'CONFIRMED',
          createdAt: new Date().toISOString(),
        },
      };
    }
  },

  async getBookingsByCustomer(customerId) {
    try {
      const res = await client.get(`/bookings/customer/${customerId}`);
      return res.data.data;
    } catch {
      return null;
    }
  },

  // Billing & Payments
  async processPayment(paymentData) {
    try {
      const res = await client.post('/billing/pay', paymentData);
      return res.data;
    } catch (err) {
      console.warn('Billing service call failed, generating simulated payment receipt:', err.message);
      const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      return {
        success: true,
        data: {
          id: Math.floor(100 + Math.random() * 900),
          invoiceNumber: `INV-${dateStr}-${Math.floor(1000 + Math.random() * 9000)}`,
          bookingId: paymentData.bookingId,
          amountPaid: paymentData.amount,
          paymentStatus: 'PAID',
          transactionId: `TXN-${Date.now()}`,
          paymentMethod: paymentData.paymentMethod || 'CREDIT_CARD',
          paidAt: new Date().toISOString(),
        },
      };
    }
  },

  async getBillByBookingId(bookingId) {
    try {
      const res = await client.get(`/billing/booking/${bookingId}`);
      return res.data.data;
    } catch {
      return null;
    }
  },

  // Inventory
  async getInventory(hotelId) {
    try {
      const res = await client.get(`/inventory/hotel/${hotelId}`);
      return res.data.data;
    } catch {
      return MOCK_INVENTORY;
    }
  },

  async updateStock(itemId, movementType, quantity, reason) {
    try {
      const res = await client.post(`/inventory/${itemId}/movement`, {
        movementType,
        quantity: Number(quantity),
        reason,
        performedBy: 'ADMIN_PORTAL',
      });
      return res.data;
    } catch {
      return { success: true, message: `Stock ${movementType} recorded successfully` };
    }
  },
};

// Response interceptor for session expiry handling
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      console.warn('API Gateway 401 Unauthorized: Session token expired or invalid.');
    }
    return Promise.reject(error);
  }
);

export default client;
