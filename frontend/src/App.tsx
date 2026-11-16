import React, { useEffect, useState } from 'react';
import { Navbar } from './components/Navbar';
import { UrlCreateCard } from './components/UrlCreateCard';
import { UrlList } from './components/UrlList';
import { AuthModal } from './components/AuthModal';
import { AnalyticsModal } from './components/AnalyticsModal';
import { api, User, UrlItem } from './api/client';
import { ShieldCheck, Zap, Activity } from 'lucide-react';

export const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [urls, setUrls] = useState<UrlItem[]>([]);
  const [loadingUrls, setLoadingUrls] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [selectedUrlForAnalytics, setSelectedUrlForAnalytics] = useState<UrlItem | null>(null);

  // Load user on mount if token exists
  useEffect(() => {
    const token = localStorage.getItem('linksphere_token');
    if (token) {
      api.auth.getMe()
        .then(setUser)
        .catch(() => {
          localStorage.removeItem('linksphere_token');
          setUser(null);
        });
    }
  }, []);

  // Fetch URLs when user changes or search query changes
  useEffect(() => {
    if (user) {
      fetchUrls();
    } else {
      setUrls([]);
    }
  }, [user, searchQuery]);

  const fetchUrls = async () => {
    setLoadingUrls(true);
    try {
      const res = await api.urls.list(searchQuery);
      setUrls(res.content || []);
    } catch (err) {
      console.error('Failed to fetch URLs', err);
    } finally {
      setLoadingUrls(false);
    }
  };

  const handleUrlCreated = (newUrl: UrlItem) => {
    setUrls((prev) => [newUrl, ...prev]);
  };

  const handleToggleStatus = async (url: UrlItem) => {
    try {
      const updated = await api.urls.update(url.id, { isActive: !url.isActive });
      setUrls((prev) => prev.map((u) => (u.id === url.id ? updated : u)));
    } catch (err: any) {
      alert(err.message || 'Failed to toggle status');
    }
  };

  const handleDeleteUrl = async (id: number) => {
    try {
      await api.urls.delete(id);
      setUrls((prev) => prev.filter((u) => u.id !== id));
    } catch (err: any) {
      alert(err.message || 'Failed to delete link');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('linksphere_token');
    setUser(null);
    setUrls([]);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar
        user={user}
        onOpenAuth={() => setAuthModalOpen(true)}
        onLogout={handleLogout}
      />

      <main style={{ maxWidth: '1000px', margin: '0 auto', padding: '2.5rem 1.5rem', width: '100%', flex: 1 }}>
        {/* Hero Section */}
        <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
          <h1 style={{
            fontSize: '2.5rem',
            fontWeight: 800,
            letterSpacing: '-0.03em',
            marginBottom: '0.75rem',
            background: 'linear-gradient(to right, #ffffff, #93c5fd, #60a5fa)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            Fast, Resilient &amp; Measurable Short Links
          </h1>
          <p style={{ fontSize: '1.05rem', color: '#94a3b8', maxWidth: '600px', margin: '0 auto' }}>
            Transform unwieldy URLs into high-speed shortened links with real-time GDPR-compliant click-stream analytics.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '2rem', marginTop: '1.5rem', color: '#64748b', fontSize: '0.85rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Zap size={16} color="#3b82f6" /> Redis Sub-millisecond Caching
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Activity size={16} color="#8b5cf6" /> Async Click Stream
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <ShieldCheck size={16} color="#10b981" /> Rate Limited &amp; Secure
            </span>
          </div>
        </div>

        {/* URL Creation Form */}
        <UrlCreateCard
          onUrlCreated={handleUrlCreated}
          onRequireAuth={() => setAuthModalOpen(true)}
          isAuthenticated={!!user}
        />

        {/* User URL List */}
        {user ? (
          <UrlList
            urls={urls}
            loading={loadingUrls}
            onToggleStatus={handleToggleStatus}
            onDeleteUrl={handleDeleteUrl}
            onViewAnalytics={(url) => setSelectedUrlForAnalytics(url)}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
          />
        ) : (
          <div className="glass-panel" style={{
            textAlign: 'center',
            padding: '3rem 1.5rem',
            color: '#94a3b8',
          }}>
            <h3 style={{ fontSize: '1.2rem', color: '#ffffff', marginBottom: '0.5rem' }}>
              Track analytics and manage all your links
            </h3>
            <p style={{ fontSize: '0.9rem', marginBottom: '1.5rem', maxWidth: '480px', margin: '0 auto 1.5rem' }}>
              Create an account or sign in to configure custom aliases, toggle links, set expiration dates, and view click statistics.
            </p>
            <button
              onClick={() => setAuthModalOpen(true)}
              style={{
                padding: '0.65rem 1.5rem',
                borderRadius: '6px',
                border: 'none',
                background: '#3b82f6',
                color: '#ffffff',
                fontWeight: 600,
                fontSize: '0.9rem',
              }}
            >
              Sign In to View Dashboard
            </button>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer style={{
        textAlign: 'center',
        padding: '2rem',
        borderTop: '1px solid #1e293b',
        color: '#64748b',
        fontSize: '0.85rem',
      }}>
        LinkSphere &copy; {new Date().getFullYear()} — Enterprise Scalable Architecture with Java 21, Spring Boot 3 &amp; Redis
      </footer>

      {/* Modals */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onSuccess={(loggedUser) => {
          setUser(loggedUser);
          fetchUrls();
        }}
      />

      <AnalyticsModal
        url={selectedUrlForAnalytics}
        onClose={() => setSelectedUrlForAnalytics(null)}
      />
    </div>
  );
};

export default App;
