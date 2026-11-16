import React from 'react';
import { Link2, LogOut, User as UserIcon, LogIn } from 'lucide-react';
import { User } from '../api/client';

interface NavbarProps {
  user: User | null;
  onOpenAuth: () => void;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, onOpenAuth, onLogout }) => {
  return (
    <nav style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '1rem 2rem',
      borderBottom: '1px solid #1e293b',
      background: 'rgba(11, 15, 25, 0.95)',
      position: 'sticky',
      top: 0,
      zIndex: 50,
      backdropFilter: 'blur(8px)'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <div style={{
          width: '38px',
          height: '38px',
          borderRadius: '8px',
          background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 15px rgba(59, 130, 246, 0.5)'
        }}>
          <Link2 size={22} color="#ffffff" />
        </div>
        <div>
          <span style={{ fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.025em', color: '#ffffff' }}>
            Link<span style={{ color: '#3b82f6' }}>Sphere</span>
          </span>
          <span style={{ marginLeft: '0.5rem', fontSize: '0.75rem', padding: '2px 6px', borderRadius: '4px', background: '#1e293b', color: '#94a3b8' }}>
            v1.0
          </span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        {user ? (
          <>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#94a3b8', fontSize: '0.9rem' }}>
              <UserIcon size={16} />
              <span>{user.fullName}</span>
            </div>
            <button
              onClick={onLogout}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
                padding: '0.5rem 0.85rem',
                borderRadius: '6px',
                border: '1px solid #334155',
                background: 'transparent',
                color: '#cbd5e1',
                fontSize: '0.85rem',
              }}
            >
              <LogOut size={15} />
              <span>Logout</span>
            </button>
          </>
        ) : (
          <button
            onClick={onOpenAuth}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.5rem 1.1rem',
              borderRadius: '6px',
              border: 'none',
              background: '#3b82f6',
              color: '#ffffff',
              fontSize: '0.9rem',
              fontWeight: 500,
            }}
          >
            <LogIn size={16} />
            <span>Sign In</span>
          </button>
        )}
      </div>
    </nav>
  );
};
