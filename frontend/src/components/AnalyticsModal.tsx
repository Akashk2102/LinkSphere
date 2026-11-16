import React, { useEffect, useState } from 'react';
import { X, Smartphone, Globe, Compass, ExternalLink, Calendar } from 'lucide-react';
import { api, AnalyticsData, UrlItem } from '../api/client';

interface AnalyticsModalProps {
  url: UrlItem | null;
  onClose: () => void;
}

export const AnalyticsModal: React.FC<AnalyticsModalProps> = ({ url, onClose }) => {
  const [data, setData] = useState<AnalyticsData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!url) return;
    setLoading(true);
    setError(null);
    api.analytics.get(url.id)
      .then(setData)
      .catch((err) => setError(err.message || 'Failed to load analytics'))
      .finally(() => setLoading(false));
  }, [url]);

  if (!url) return null;

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      backdropFilter: 'blur(6px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
      padding: '1.5rem',
    }}>
      <div className="glass-panel" style={{
        width: '100%',
        maxWidth: '750px',
        maxHeight: '90vh',
        overflowY: 'auto',
        padding: '2rem',
        position: 'relative',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.7)',
      }}>
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '1.25rem',
            right: '1.25rem',
            background: 'none',
            border: 'none',
            color: '#94a3b8',
          }}
        >
          <X size={20} />
        </button>

        <div style={{ marginBottom: '1.5rem' }}>
          <h2 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#ffffff', marginBottom: '0.25rem' }}>
            Click Stream Analytics
          </h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#60a5fa', fontSize: '0.9rem' }}>
            <span>{url.shortUrl}</span>
            <span style={{ color: '#64748b' }}>→</span>
            <span style={{ color: '#94a3b8', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '350px' }}>
              {url.originalUrl}
            </span>
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '4rem', color: '#94a3b8' }}>
            Aggregating click metrics...
          </div>
        ) : error ? (
          <div style={{ padding: '1rem', background: 'rgba(239,68,68,0.1)', color: '#fca5a5', borderRadius: '6px' }}>
            {error}
          </div>
        ) : data ? (
          <div>
            {/* Total Metric Card */}
            <div style={{
              background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(139, 92, 246, 0.15))',
              border: '1px solid rgba(59, 130, 246, 0.3)',
              borderRadius: '8px',
              padding: '1.25rem',
              marginBottom: '1.5rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}>
              <div>
                <div style={{ fontSize: '0.8rem', color: '#93c5fd', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Total Lifetime Clicks
                </div>
                <div style={{ fontSize: '2.25rem', fontWeight: 800, color: '#ffffff' }}>
                  {data.totalClicks.toLocaleString()}
                </div>
              </div>
              <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
                Privacy-Preserving (GDPR Salted SHA-256)
              </div>
            </div>

            {/* Timeline Sparkline / Bars (Last 30 Days) */}
            <div style={{
              background: '#0e1422',
              border: '1px solid #1e293b',
              borderRadius: '8px',
              padding: '1.25rem',
              marginBottom: '1.5rem',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '1rem' }}>
                <Calendar size={16} color="#3b82f6" />
                <h4 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#ffffff' }}>Daily Activity (Past 30 Days)</h4>
              </div>

              <div style={{ display: 'flex', alignItems: 'flex-end', gap: '4px', height: '110px', paddingTop: '10px' }}>
                {data.timeline.map((point, idx) => {
                  const maxClicks = Math.max(...data.timeline.map(p => p.clicks), 1);
                  const barHeight = Math.max((point.clicks / maxClicks) * 90, point.clicks > 0 ? 8 : 2);
                  return (
                    <div
                      key={idx}
                      title={`${point.date}: ${point.clicks} clicks`}
                      style={{
                        flex: 1,
                        height: `${barHeight}px`,
                        background: point.clicks > 0 ? '#3b82f6' : '#1e293b',
                        borderRadius: '2px 2px 0 0',
                        transition: 'height 0.3s ease',
                      }}
                    />
                  );
                })}
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: '#64748b', marginTop: '0.5rem' }}>
                <span>30 days ago</span>
                <span>Today</span>
              </div>
            </div>

            {/* Two-Column Distribution (Devices & Browsers) */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem', marginBottom: '1.5rem' }}>
              {/* Devices */}
              <div style={{ background: '#0e1422', border: '1px solid #1e293b', borderRadius: '8px', padding: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.85rem' }}>
                  <Smartphone size={16} color="#8b5cf6" />
                  <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#ffffff' }}>Device Distribution</h4>
                </div>
                {data.devices.length === 0 ? (
                  <div style={{ color: '#64748b', fontSize: '0.85rem' }}>No device data recorded yet</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                    {data.devices.map((dev, i) => (
                      <div key={i}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.2rem' }}>
                          <span style={{ color: '#cbd5e1' }}>{dev.label}</span>
                          <span style={{ color: '#94a3b8' }}>{dev.count} ({dev.percentage}%)</span>
                        </div>
                        <div style={{ height: '6px', background: '#1e293b', borderRadius: '3px', overflow: 'hidden' }}>
                          <div style={{ width: `${dev.percentage}%`, height: '100%', background: '#8b5cf6', borderRadius: '3px' }} />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Browsers */}
              <div style={{ background: '#0e1422', border: '1px solid #1e293b', borderRadius: '8px', padding: '1.25rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.85rem' }}>
                  <Globe size={16} color="#10b981" />
                  <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#ffffff' }}>Top Browsers</h4>
                </div>
                {data.browsers.length === 0 ? (
                  <div style={{ color: '#64748b', fontSize: '0.85rem' }}>No browser data recorded yet</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                    {data.browsers.map((b, i) => (
                      <div key={i}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.2rem' }}>
                          <span style={{ color: '#cbd5e1' }}>{b.label}</span>
                          <span style={{ color: '#94a3b8' }}>{b.count} ({b.percentage}%)</span>
                        </div>
                        <div style={{ height: '6px', background: '#1e293b', borderRadius: '3px', overflow: 'hidden' }}>
                          <div style={{ width: `${b.percentage}%`, height: '100%', background: '#10b981', borderRadius: '3px' }} />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Referrers */}
            <div style={{ background: '#0e1422', border: '1px solid #1e293b', borderRadius: '8px', padding: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.85rem' }}>
                <Compass size={16} color="#f59e0b" />
                <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#ffffff' }}>Traffic Sources (Referrers)</h4>
              </div>
              {data.referrers.length === 0 ? (
                <div style={{ color: '#64748b', fontSize: '0.85rem' }}>No referrer data recorded yet</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {data.referrers.map((ref, i) => (
                    <div key={i} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', padding: '0.35rem 0', borderBottom: '1px solid #1e293b' }}>
                      <span style={{ color: '#cbd5e1' }}>{ref.label}</span>
                      <span style={{ color: '#f59e0b', fontWeight: 500 }}>{ref.count} visits ({ref.percentage}%)</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
};
