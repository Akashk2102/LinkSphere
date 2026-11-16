import React, { useState } from 'react';
import { Copy, Check, BarChart2, Trash2, Power, ExternalLink, Search, Clock } from 'lucide-react';
import { UrlItem } from '../api/client';

interface UrlListProps {
  urls: UrlItem[];
  loading: boolean;
  onToggleStatus: (url: UrlItem) => void;
  onDeleteUrl: (id: number) => void;
  onViewAnalytics: (url: UrlItem) => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

export const UrlList: React.FC<UrlListProps> = ({
  urls,
  loading,
  onToggleStatus,
  onDeleteUrl,
  onViewAnalytics,
  searchQuery,
  onSearchChange,
}) => {
  const [copiedId, setCopiedId] = useState<number | null>(null);

  const handleCopy = (url: UrlItem) => {
    navigator.clipboard.writeText(url.shortUrl);
    setCopiedId(url.id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div className="glass-panel" style={{ padding: '1.75rem' }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '1rem',
        marginBottom: '1.5rem',
      }}>
        <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#ffffff' }}>
          Your Shortened Links ({urls.length})
        </h3>

        <div style={{ position: 'relative', width: '280px' }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search title, URL or alias..."
            style={{
              width: '100%',
              padding: '0.55rem 0.75rem 0.55rem 2.3rem',
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

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#94a3b8' }}>
          Loading your links...
        </div>
      ) : urls.length === 0 ? (
        <div style={{
          textAlign: 'center',
          padding: '3rem 1rem',
          color: '#94a3b8',
          border: '1px dashed #334155',
          borderRadius: '8px',
        }}>
          {searchQuery ? 'No links match your search query.' : 'No shortened links yet. Create your first short link above!'}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
          {urls.map((url) => (
            <div
              key={url.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '1rem 1.25rem',
                borderRadius: '8px',
                background: '#0e1422',
                border: '1px solid #1e293b',
                flexWrap: 'wrap',
                gap: '1rem',
              }}
            >
              <div style={{ flex: '1 1 320px', minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                  <span style={{ fontWeight: 600, color: '#ffffff', fontSize: '0.95rem' }}>
                    {url.title || url.shortCode}
                  </span>
                  {url.isCustomAlias && (
                    <span style={{ fontSize: '0.7rem', padding: '1px 6px', borderRadius: '4px', background: '#312e81', color: '#c7d2fe' }}>
                      Custom Alias
                    </span>
                  )}
                  {url.isExpired && (
                    <span style={{ fontSize: '0.7rem', padding: '1px 6px', borderRadius: '4px', background: '#7f1d1d', color: '#fecaca' }}>
                      Expired
                    </span>
                  )}
                  {!url.isActive && !url.isExpired && (
                    <span style={{ fontSize: '0.7rem', padding: '1px 6px', borderRadius: '4px', background: '#374151', color: '#d1d5db' }}>
                      Disabled
                    </span>
                  )}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.35rem' }}>
                  <a
                    href={url.shortUrl}
                    target="_blank"
                    rel="noreferrer"
                    style={{
                      color: '#3b82f6',
                      fontSize: '0.85rem',
                      fontFamily: 'var(--font-mono)',
                      textDecoration: 'none',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.25rem',
                    }}
                  >
                    {url.shortUrl}
                    <ExternalLink size={12} />
                  </a>
                </div>

                <div style={{
                  color: '#64748b',
                  fontSize: '0.8rem',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  maxWidth: '500px',
                }}>
                  ↳ {url.originalUrl}
                </div>

                {url.expiresAt && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.75rem', color: '#94a3b8', marginTop: '0.35rem' }}>
                    <Clock size={12} />
                    <span>Expires: {new Date(url.expiresAt).toLocaleDateString()}</span>
                  </div>
                )}
              </div>

              {/* Actions & Metrics */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  padding: '0.4rem 0.75rem',
                  borderRadius: '6px',
                  background: '#1e293b',
                  fontSize: '0.85rem',
                  fontWeight: 500,
                  color: '#cbd5e1',
                }}>
                  <BarChart2 size={15} color="#3b82f6" />
                  <span>{url.clickCount.toLocaleString()} clicks</span>
                </div>

                <button
                  onClick={() => handleCopy(url)}
                  title="Copy short link"
                  style={{
                    padding: '0.45rem',
                    borderRadius: '6px',
                    border: '1px solid #334155',
                    background: copiedId === url.id ? '#10b981' : '#1e293b',
                    color: '#ffffff',
                  }}
                >
                  {copiedId === url.id ? <Check size={16} /> : <Copy size={16} />}
                </button>

                <button
                  onClick={() => onViewAnalytics(url)}
                  title="View detailed analytics"
                  style={{
                    padding: '0.45rem 0.75rem',
                    borderRadius: '6px',
                    border: '1px solid #334155',
                    background: '#1e293b',
                    color: '#60a5fa',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    fontSize: '0.85rem',
                  }}
                >
                  <BarChart2 size={16} />
                  <span>Analytics</span>
                </button>

                <button
                  onClick={() => onToggleStatus(url)}
                  title={url.isActive ? 'Disable link' : 'Enable link'}
                  style={{
                    padding: '0.45rem',
                    borderRadius: '6px',
                    border: '1px solid #334155',
                    background: url.isActive ? '#1e293b' : '#374151',
                    color: url.isActive ? '#10b981' : '#9ca3af',
                  }}
                >
                  <Power size={16} />
                </button>

                <button
                  onClick={() => {
                    if (window.confirm('Are you sure you want to delete this shortened link?')) {
                      onDeleteUrl(url.id);
                    }
                  }}
                  title="Delete link"
                  style={{
                    padding: '0.45rem',
                    borderRadius: '6px',
                    border: '1px solid rgba(239, 68, 68, 0.3)',
                    background: 'rgba(239, 68, 68, 0.1)',
                    color: '#f87171',
                  }}
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
