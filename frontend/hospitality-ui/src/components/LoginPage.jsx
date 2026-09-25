import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Crown, 
  Lock, 
  User, 
  Eye, 
  EyeOff, 
  Sparkles, 
  ArrowRight, 
  AlertCircle, 
  CheckCircle2, 
  LogOut, 
  KeyRound, 
  ShieldCheck, 
  UtensilsCrossed 
} from 'lucide-react';

const LoginPage = ({ onNavigate }) => {
  const { user, login, register, logout, isAdmin, isStaff, loading } = useAuth();

  const [activeTab, setActiveTab] = useState('LOGIN'); // 'LOGIN' | 'REGISTER'
  
  // Login Form State - Starts empty to require explicit authentication
  const [loginIdentifier, setLoginIdentifier] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [showLoginPassword, setShowLoginPassword] = useState(false);
  const [loginError, setLoginError] = useState('');
  const [loginSuccess, setLoginSuccess] = useState('');

  // Register Form State
  const [regFullName, setRegFullName] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [showRegPassword, setShowRegPassword] = useState(false);
  const [regError, setRegError] = useState('');
  const [regSuccess, setRegSuccess] = useState('');

  // Autofill Helper: Populates credentials into form fields ONLY (does NOT auto-login)
  const handleAutofillDemo = (type) => {
    setActiveTab('LOGIN');
    setLoginError('');

    if (type === 'ADMIN') {
      setLoginIdentifier('admin');
      setLoginPassword('admin123');
      setLoginSuccess('Demo Super Admin credentials loaded into form. Click "Sign In" below.');
    } else if (type === 'STAFF') {
      setLoginIdentifier('staff');
      setLoginPassword('staff123');
      setLoginSuccess('Demo Staff & Chef credentials loaded into form. Click "Sign In" below.');
    } else {
      setLoginIdentifier('customer');
      setLoginPassword('customer123');
      setLoginSuccess('Demo Sovereign Guest credentials loaded into form. Click "Sign In" below.');
    }
  };

  // Manual Login Submission
  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setLoginError('');
    setLoginSuccess('');

    if (!loginIdentifier.trim() || !loginPassword.trim()) {
      setLoginError('Please enter both your username/email and password.');
      return;
    }

    const res = await login(loginIdentifier.trim(), loginPassword.trim());
    if (res?.success) {
      const idLower = loginIdentifier.toLowerCase();
      let target = 'USER_DASHBOARD';
      if (idLower.includes('admin')) target = 'ADMIN';
      else if (idLower.includes('staff')) target = 'STAFF';

      setLoginSuccess(`Welcome back, ${loginIdentifier}! Access granted.`);
      setTimeout(() => {
        if (onNavigate) onNavigate(target);
      }, 700);
    } else {
      setLoginError(res?.message || 'Authentication failed. Please verify credentials.');
    }
  };

  // Registration Submission
  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setRegError('');
    setRegSuccess('');

    if (!regUsername.trim() || !regEmail.trim() || !regPassword.trim() || !regFullName.trim()) {
      setRegError('Please fill all required fields.');
      return;
    }

    if (regPassword !== regConfirmPassword) {
      setRegError('Passwords do not match. Please verify your entries.');
      return;
    }

    if (regPassword.length < 6) {
      setRegError('Password must be at least 6 characters long.');
      return;
    }

    const res = await register({
      username: regUsername.trim(),
      email: regEmail.trim(),
      password: regPassword,
      fullName: regFullName.trim(),
      phone: regPhone.trim() || '+91 98765 43210'
    });

    if (res?.success) {
      setRegSuccess('Your Sovereign Membership has been created! Signing you in...');
      setLoginIdentifier(regUsername.trim());
      setLoginPassword(regPassword);
      setTimeout(async () => {
        await login(regUsername.trim(), regPassword);
        if (onNavigate) onNavigate('USER_DASHBOARD');
      }, 1000);
    } else {
      setRegError(res?.message || 'Registration failed. Username or email may already exist.');
    }
  };

  return (
    <div style={{
      minHeight: 'calc(100vh - 160px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px 16px 40px',
      background: 'radial-gradient(ellipse at 50% 30%, rgba(212, 175, 55, 0.08) 0%, rgba(11, 17, 32, 0.98) 75%)'
    }}>
      <div style={{ width: '100%', maxWidth: '470px' }}>

        {/* Existing Active Session Alert (if already logged in) */}
        {user && (
          <div style={{
            background: 'rgba(19, 29, 51, 0.9)',
            border: '1px solid var(--border-gold)',
            borderRadius: 'var(--radius-lg)',
            padding: '12px 16px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
            fontSize: '0.82rem'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Crown size={16} color="var(--gold-primary)" />
              <span style={{ color: 'var(--text-muted)' }}>
                Active: <strong style={{ color: 'var(--gold-light)' }}>{user.username}</strong> ({isAdmin ? 'Admin' : isStaff ? 'Staff' : 'Guest'})
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <button
                onClick={() => {
                  if (isAdmin) onNavigate('ADMIN');
                  else if (isStaff) onNavigate('STAFF');
                  else onNavigate('USER_DASHBOARD');
                }}
                className="btn-outline-gold"
                style={{ padding: '4px 10px', fontSize: '0.74rem' }}
              >
                Go to Portal →
              </button>
              <button
                onClick={logout}
                style={{ color: 'var(--danger)', fontSize: '0.74rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '3px' }}
                title="Sign out"
              >
                <LogOut size={13} />
              </button>
            </div>
          </div>
        )}

        {/* Majestic Centered Card */}
        <div className="glass-panel" style={{
          padding: '30px 28px',
          border: '1px solid var(--border-gold)',
          boxShadow: 'var(--shadow-lg), var(--gold-glow)',
          borderRadius: 'var(--radius-xl)',
          position: 'relative'
        }}>

          {/* Emblem Header */}
          <div style={{ textAlign: 'center', marginBottom: '22px' }}>
            <div style={{
              width: '48px',
              height: '48px',
              borderRadius: '12px',
              background: 'var(--gold-gradient)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: 'var(--gold-glow)',
              marginBottom: '12px'
            }}>
              <Crown size={26} color="#0B1120" />
            </div>
            <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '4px' }}>
              Sovereign Access
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>
              Grand Luxe Hospitality Authentication & Concierge
            </p>
          </div>

          {/* Demo Credentials Autofill Helper Bar (Fills form fields only - requires clicking Sign In) */}
          <div style={{
            background: 'rgba(255, 255, 255, 0.03)',
            border: '1px dashed var(--border-gold)',
            borderRadius: 'var(--radius-md)',
            padding: '12px',
            marginBottom: '20px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span style={{ fontSize: '0.74rem', fontWeight: 700, color: 'var(--gold-light)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                📋 Demo Accounts (Click to Fill)
              </span>
              <span style={{ fontSize: '0.68rem', color: 'var(--text-dim)' }}>Fills credentials</span>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '6px' }}>
              <button
                type="button"
                onClick={() => handleAutofillDemo('ADMIN')}
                className="btn-outline-gold"
                style={{ padding: '6px 4px', fontSize: '0.74rem', justifyContent: 'center', borderRadius: 'var(--radius-sm)' }}
                title="Fill Super Admin credentials (admin / admin123)"
              >
                <ShieldCheck size={13} />
                Admin
              </button>
              <button
                type="button"
                onClick={() => handleAutofillDemo('STAFF')}
                className="btn-outline-gold"
                style={{ padding: '6px 4px', fontSize: '0.74rem', justifyContent: 'center', borderRadius: 'var(--radius-sm)' }}
                title="Fill Staff credentials (staff / staff123)"
              >
                <UtensilsCrossed size={13} />
                Staff
              </button>
              <button
                type="button"
                onClick={() => handleAutofillDemo('GUEST')}
                className="btn-outline-gold"
                style={{ padding: '6px 4px', fontSize: '0.74rem', justifyContent: 'center', borderRadius: 'var(--radius-sm)' }}
                title="Fill Guest credentials (customer / customer123)"
              >
                <Sparkles size={13} />
                Guest
              </button>
            </div>
          </div>

          {/* Tab Selector: Sign In vs Create Account */}
          <div style={{
            display: 'flex',
            borderBottom: '1px solid var(--border-subtle)',
            marginBottom: '20px'
          }}>
            <button
              onClick={() => { setActiveTab('LOGIN'); setLoginError(''); setLoginSuccess(''); }}
              style={{
                flex: 1,
                padding: '10px 12px',
                textAlign: 'center',
                fontWeight: 700,
                fontSize: '0.88rem',
                color: activeTab === 'LOGIN' ? 'var(--gold-primary)' : 'var(--text-muted)',
                borderBottom: activeTab === 'LOGIN' ? '2px solid var(--gold-primary)' : '2px solid transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                transition: 'all 0.2s ease',
                cursor: 'pointer'
              }}
            >
              <KeyRound size={15} />
              Sign In
            </button>
            <button
              onClick={() => { setActiveTab('REGISTER'); setRegError(''); setRegSuccess(''); }}
              style={{
                flex: 1,
                padding: '10px 12px',
                textAlign: 'center',
                fontWeight: 700,
                fontSize: '0.88rem',
                color: activeTab === 'REGISTER' ? 'var(--gold-primary)' : 'var(--text-muted)',
                borderBottom: activeTab === 'REGISTER' ? '2px solid var(--gold-primary)' : '2px solid transparent',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                transition: 'all 0.2s ease',
                cursor: 'pointer'
              }}
            >
              <User size={15} />
              Create Account
            </button>
          </div>

          {/* SIGN IN FORM */}
          {activeTab === 'LOGIN' && (
            <form onSubmit={handleLoginSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {loginError && (
                <div style={{
                  background: 'var(--danger-bg)',
                  border: '1px solid var(--danger)',
                  color: 'var(--danger)',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.8rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <AlertCircle size={16} />
                  <span>{loginError}</span>
                </div>
              )}

              {loginSuccess && (
                <div style={{
                  background: 'var(--success-bg)',
                  border: '1px solid var(--success)',
                  color: 'var(--success)',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.8rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <CheckCircle2 size={16} />
                  <span>{loginSuccess}</span>
                </div>
              )}

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '6px' }}>
                  Username or Sovereign Email
                </label>
                <div style={{ position: 'relative' }}>
                  <User size={16} color="var(--text-dim)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                  <input
                    type="text"
                    required
                    value={loginIdentifier}
                    onChange={(e) => setLoginIdentifier(e.target.value)}
                    placeholder="admin, staff, or customer"
                    className="input-luxury"
                    style={{ paddingLeft: '38px', fontSize: '0.88rem', padding: '10px 12px 10px 38px' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '6px' }}>
                  Master Key Password
                </label>
                <div style={{ position: 'relative' }}>
                  <Lock size={16} color="var(--text-dim)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                  <input
                    type={showLoginPassword ? 'text' : 'password'}
                    required
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    placeholder="Enter password..."
                    className="input-luxury"
                    style={{ paddingLeft: '38px', paddingRight: '40px', fontSize: '0.88rem', padding: '10px 40px 10px 38px' }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowLoginPassword(!showLoginPassword)}
                    style={{ position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dim)' }}
                    title={showLoginPassword ? 'Hide password' : 'Show password'}
                  >
                    {showLoginPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
                  <input type="checkbox" defaultChecked style={{ accentColor: 'var(--gold-primary)' }} />
                  Remember session
                </label>
                <a
                  href="#quick"
                  onClick={(e) => {
                    e.preventDefault();
                    setLoginIdentifier('admin');
                    setLoginPassword('admin123');
                  }}
                  style={{ color: 'var(--gold-light)' }}
                >
                  Use Admin Demo Creds
                </a>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn-gold"
                style={{
                  width: '100%',
                  justifyContent: 'center',
                  padding: '12px',
                  fontSize: '0.92rem',
                  fontWeight: 700,
                  marginTop: '4px',
                  cursor: loading ? 'wait' : 'pointer'
                }}
              >
                {loading ? 'Authenticating...' : 'Sign In to Sovereign Portal'}
                {!loading && <ArrowRight size={16} />}
              </button>
            </form>
          )}

          {/* REGISTER FORM */}
          {activeTab === 'REGISTER' && (
            <form onSubmit={handleRegisterSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {regError && (
                <div style={{
                  background: 'var(--danger-bg)',
                  border: '1px solid var(--danger)',
                  color: 'var(--danger)',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.8rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <AlertCircle size={16} />
                  <span>{regError}</span>
                </div>
              )}

              {regSuccess && (
                <div style={{
                  background: 'var(--success-bg)',
                  border: '1px solid var(--success)',
                  color: 'var(--success)',
                  padding: '10px 14px',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.8rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <CheckCircle2 size={16} />
                  <span>{regSuccess}</span>
                </div>
              )}

              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                  Full Sovereign Name *
                </label>
                <input
                  type="text"
                  required
                  value={regFullName}
                  onChange={(e) => setRegFullName(e.target.value)}
                  placeholder="e.g. Lord Alexander"
                  className="input-luxury"
                  style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                    Username *
                  </label>
                  <input
                    type="text"
                    required
                    value={regUsername}
                    onChange={(e) => setRegUsername(e.target.value)}
                    placeholder="alexander"
                    className="input-luxury"
                    style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                    Phone Number
                  </label>
                  <input
                    type="tel"
                    value={regPhone}
                    onChange={(e) => setRegPhone(e.target.value)}
                    placeholder="+91 98765 43210"
                    className="input-luxury"
                    style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                  Email Address *
                </label>
                <input
                  type="email"
                  required
                  value={regEmail}
                  onChange={(e) => setRegEmail(e.target.value)}
                  placeholder="alexander@domain.com"
                  className="input-luxury"
                  style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                    Password *
                  </label>
                  <input
                    type={showRegPassword ? 'text' : 'password'}
                    required
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    placeholder="Min 6 chars"
                    className="input-luxury"
                    style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>
                    Confirm Password *
                  </label>
                  <input
                    type={showRegPassword ? 'text' : 'password'}
                    required
                    value={regConfirmPassword}
                    onChange={(e) => setRegConfirmPassword(e.target.value)}
                    placeholder="Repeat password"
                    className="input-luxury"
                    style={{ padding: '8px 12px', fontSize: '0.85rem' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '-4px' }}>
                <button
                  type="button"
                  onClick={() => setShowRegPassword(!showRegPassword)}
                  style={{ fontSize: '0.72rem', color: 'var(--gold-light)', display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer' }}
                >
                  {showRegPassword ? <EyeOff size={13} /> : <Eye size={13} />}
                  {showRegPassword ? 'Hide Passwords' : 'Show Passwords'}
                </button>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn-gold"
                style={{
                  width: '100%',
                  justifyContent: 'center',
                  padding: '12px',
                  fontSize: '0.92rem',
                  fontWeight: 700,
                  marginTop: '6px',
                  cursor: loading ? 'wait' : 'pointer'
                }}
              >
                {loading ? 'Creating Sovereign Account...' : 'Register Sovereign Membership'}
                {!loading && <ArrowRight size={16} />}
              </button>
            </form>
          )}

          {/* Security Trust Footnote */}
          <div style={{
            marginTop: '22px',
            paddingTop: '16px',
            borderTop: '1px solid var(--border-subtle)',
            textAlign: 'center',
            fontSize: '0.72rem',
            color: 'var(--text-dim)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px'
          }}>
            <Lock size={12} color="var(--gold-primary)" />
            <span>Spring Security 6.3 Stateless JWT • BCrypt Strength 12 • 256-Bit SSL</span>
          </div>

        </div>

      </div>
    </div>
  );
};

export default LoginPage;
