import React, { useState } from 'react';
import { Link, Sparkles, Copy, Check, Calendar, Tag, Globe } from 'lucide-react';
import { api, UrlItem } from '../api/client';

interface UrlCreateCardProps {
  onUrlCreated: (newUrl: UrlItem) => void;
  onRequireAuth: () => void;
  isAuthenticated: boolean;
}

export const UrlCreateCard: React.FC<UrlCreateCardProps> = ({
  onUrlCreated,
  onRequireAuth,
  isAuthenticated,
}) => {
  const [originalUrl, setOriginalUrl] = useState('');
  const [title, setTitle] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdUrl, setCreatedUrl] = useState<UrlItem | null>(null);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      onRequireAuth();
      return;
    }

    setError(null);
    setLoading(true);

    try {
      const payload: any = { originalUrl: originalUrl.trim() };
      if (title.trim()) payload.title = title.trim();
      if (customAlias.trim()) payload.customAlias = customAlias.trim();
      if (expiresAt) payload.expiresAt = new Date(expiresAt).toISOString();

      const created = await api.urls.create(payload);
      setCreatedUrl(created);
      onUrlCreated(created);

      // Reset form
      setOriginalUrl('');
      setTitle('');
      setCustomAlias('');
      setExpiresAt('');
      setShowAdvanced(false);
    } catch (err: any) {
      setError(err.message || 'Failed to shorten URL');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2.5rem', boxShadow: '0 20px 40px rgba(0,0,0,0.4)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '1.25rem' }}>
        <Sparkles size={20} color="#3b82f6" />
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600, color: '#ffffff' }}>Shorten a Long URL</h2>
      </div>

      {error && (
        <div style={{
          padding: '0.75rem',
          borderRadius: '6px',
          backgroundColor: 'rgba(239, 68, 68, 0.15)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          color: '#fca5a5',
          fontSize: '0.85rem',
          marginBottom: '1.25rem',
        }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
          <div style={{ flex: '1 1 340px', position: 'relative' }}>
            <Globe size={18} style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
            <input
              type="url"
              required
              value={originalUrl}
              onChange={(e) => setOriginalUrl(e.target.value)}
              placeholder="Paste a long URL here (e.g., https://example.com/very/long/path)..."
              style={{
                width: '100%',
                padding: '0.85rem 1rem 0.85rem 2.8rem',
                borderRadius: '8px',
                border: '1px solid #334155',
                background: '#0b0f19',
                color: '#ffffff',
                fontSize: '0.95rem',
                outline: 'none',
              }}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              padding: '0.85rem 1.75rem',
              borderRadius: '8px',
              border: 'none',
              background: 'linear-gradient(135deg, #3b82f6, #2563eb)',
              color: '#ffffff',
              fontWeight: 600,
              fontSize: '0.95rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)',
            }}
          >
            <Link size={18} />
            <span>{loading ? 'Shortening...' : 'Shorten'}</span>
          </button>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <button
            type="button"
            onClick={() => setShowAdvanced(!showAdvanced)}
            style={{ background: 'none', border: 'none', color: '#94a3b8', fontSize: '0.85rem', textDecoration: 'underline' }}
          >
            {showAdvanced ? 'Hide options ▲' : 'Custom alias & expiration options ▼'}
          </button>
        </div>

        {showAdvanced && (
          <div style={{
            marginTop: '1.25rem',
            paddingTop: '1.25rem',
            borderTop: '1px solid #1e293b',
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '1rem',
          }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#cbd5e1', marginBottom: '0.35rem' }}>
                Optional Title
              </label>
              <div style={{ position: 'relative' }}>
                <Tag size={16} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="My Portfolio or Product Launch"
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.75rem 0.65rem 2.2rem',
                    borderRadius: '6px',
                    border: '1px solid #334155',
                    background: '#0b0f19',
                    color: '#ffffff',
                    fontSize: '0.85rem',
                    outline: 'none',
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#cbd5e1', marginBottom: '0.35rem' }}>
                Custom Alias (e.g. black-friday)
              </label>
              <input
                type="text"
                value={customAlias}
                onChange={(e) => setCustomAlias(e.target.value)}
                placeholder="my-custom-code"
                style={{
                  width: '100%',
                  padding: '0.65rem 0.75rem',
                  borderRadius: '6px',
                  border: '1px solid #334155',
                  background: '#0b0f19',
                  color: '#ffffff',
                  fontSize: '0.85rem',
                  outline: 'none',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: '#cbd5e1', marginBottom: '0.35rem' }}>
                Expiration Date & Time
              </label>
              <div style={{ position: 'relative' }}>
                <Calendar size={16} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
                <input
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '0.65rem 0.75rem 0.65rem 2.2rem',
                    borderRadius: '6px',
                    border: '1px solid #334155',
                    background: '#0b0f19',
                    color: '#ffffff',
                    fontSize: '0.85rem',
                    outline: 'none',
                  }}
                />
              </div>
            </div>
          </div>
        )}
      </form>

      {createdUrl && (
        <div style={{
          marginTop: '1.5rem',
          padding: '1.25rem',
          borderRadius: '8px',
          background: 'rgba(59, 130, 246, 0.1)',
          border: '1px solid rgba(59, 130, 246, 0.3)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}>
          <div>
            <div style={{ fontSize: '0.75rem', color: '#93c5fd', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>
              Short link created successfully
            </div>
            <a
              href={createdUrl.shortUrl}
              target="_blank"
              rel="noreferrer"
              style={{ fontSize: '1.1rem', fontWeight: 600, color: '#60a5fa', textDecoration: 'none' }}
            >
              {createdUrl.shortUrl}
            </a>
          </div>

          <button
            onClick={() => copyToClipboard(createdUrl.shortUrl)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.6rem 1rem',
              borderRadius: '6px',
              border: 'none',
              background: copied ? '#10b981' : '#2563eb',
              color: '#ffffff',
              fontSize: '0.85rem',
              fontWeight: 500,
            }}
          >
            {copied ? <Check size={16} /> : <Copy size={16} />}
            <span>{copied ? 'Copied!' : 'Copy Link'}</span>
          </button>
        </div>
      )}
    </div>
  );
};
