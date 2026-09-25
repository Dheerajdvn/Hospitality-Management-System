import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/apiClient';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user_profile');
    if (savedUser) {
      try {
        return JSON.parse(savedUser);
      } catch {
        return null;
      }
    }
    // Strict authentication: unauthenticated visitor by default
    return null;
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user?.token) {
      localStorage.setItem('jwt_token', user.token);
      localStorage.setItem('user_profile', JSON.stringify(user));
    } else {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user_profile');
    }
  }, [user]);

  // Strict Login: requires valid username/email and matching password
  const login = async (usernameOrEmail, password) => {
    setLoading(true);
    try {
      const res = await api.login(usernameOrEmail, password);
      if (res?.success && res?.data) {
        const userData = {
          id: res.data.id || 1,
          username: res.data.username,
          email: res.data.email,
          roles: res.data.roles || ['ROLE_CUSTOMER'],
          token: res.data.accessToken,
        };
        setUser(userData);
        return { success: true };
      }
      return { success: false, message: res?.message || 'Invalid username or password.' };
    } catch (err) {
      return { success: false, message: err.message || 'Login failed.' };
    } finally {
      setLoading(false);
    }
  };

  // Register a new customer account
  const register = async (registerData) => {
    setLoading(true);
    try {
      const res = await api.register(registerData);
      if (res?.success) {
        return { success: true, message: res.message || 'Registration successful!' };
      }
      return { success: false, message: res?.message || 'Registration failed.' };
    } catch (err) {
      return { success: false, message: err.message || 'Registration failed.' };
    } finally {
      setLoading(false);
    }
  };

  // Sign out / clear session
  const logout = () => {
    setUser(null);
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_profile');
  };

  const isAdmin = Boolean(user?.roles?.includes('ROLE_ADMIN'));
  const isStaff = Boolean(user?.roles?.includes('ROLE_STAFF'));
  const isGuest = Boolean(user && !isAdmin && !isStaff);

  return (
    <AuthContext.Provider value={{ 
      user, 
      login, 
      register, 
      logout, 
      isAdmin, 
      isStaff, 
      isGuest, 
      loading 
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
