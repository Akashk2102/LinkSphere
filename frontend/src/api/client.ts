export interface User {
  id: number;
  email: string;
  fullName: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface UrlItem {
  id: number;
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  title?: string;
  isCustomAlias: boolean;
  isActive: boolean;
  isExpired: boolean;
  clickCount: number;
  expiresAt?: string;
  createdAt: string;
}

export interface DistributionItem {
  label: string;
  count: number;
  percentage: number;
}

export interface ClickTimelinePoint {
  date: string;
  clicks: number;
}

export interface AnalyticsData {
  shortUrlId: number;
  shortCode: string;
  originalUrl: string;
  totalClicks: number;
  devices: DistributionItem[];
  browsers: DistributionItem[];
  referrers: DistributionItem[];
  timeline: ClickTimelinePoint[];
}

const API_BASE = '/api/v1';

function getAuthHeader(): Record<string, string> {
  const token = localStorage.getItem('linksphere_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
      ...options.headers,
    },
  });

  if (!res.ok) {
    let errorMsg = 'An error occurred';
    try {
      const errJson = await res.json();
      errorMsg = errJson.message || errJson.error || JSON.stringify(errJson);
    } catch {
      errorMsg = res.statusText || `HTTP ${res.status}`;
    }
    throw new Error(errorMsg);
  }

  if (res.status === 204) {
    return {} as T;
  }

  return res.json();
}

export const api = {
  auth: {
    register: (body: { email: string; password: string; fullName: string }) =>
      request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
    login: (body: { email: string; password: string }) =>
      request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
    getMe: () => request<User>('/auth/me'),
  },
  urls: {
    create: (body: { originalUrl: string; title?: string; customAlias?: string; expiresAt?: string }) =>
      request<UrlItem>('/urls', { method: 'POST', body: JSON.stringify(body) }),
    list: (search?: string, page = 0, size = 20) => {
      const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
      if (search) params.append('search', search);
      return request<{ content: UrlItem[]; totalElements: number; totalPages: number }>(`/urls?${params}`);
    },
    update: (id: number, body: { title?: string; isActive?: boolean; expiresAt?: string }) =>
      request<UrlItem>(`/urls/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
    delete: (id: number) => request<void>(`/urls/${id}`, { method: 'DELETE' }),
  },
  analytics: {
    get: (urlId: number) => request<AnalyticsData>(`/urls/${urlId}/analytics`),
  },
};
