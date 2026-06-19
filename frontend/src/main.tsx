import React from 'react';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RefreshCw, ShieldCheck } from 'lucide-react';
import './styles.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

type CurrentUserResponse = {
  userId: string;
  claims?: Record<string, unknown>;
  scopes?: string[];
};

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    credentials: 'include',
    ...init,
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const error = new Error(`${response.status} ${response.statusText}`);
    if (response.status === 401) {
      const loginPath = response.headers.get('X-Login-Path');
      if (loginPath) {
        Object.assign(error, { loginPath });
      }
    }
    throw error;
  }

  return response.json() as Promise<T>;
}

function redirectToLogin(loginPath?: string) {
  if (loginPath) {
    window.location.replace(`${API_BASE_URL}${loginPath}`);
    return;
  }
  const target = `${window.location.pathname}${window.location.search}`;
  window.location.replace(`${API_BASE_URL}/oauth/login?target=${encodeURIComponent(target)}`);
}

function LoginInitPage() {
  const [message, setMessage] = React.useState('正在登录，请稍候');

  React.useEffect(() => {
    let cancelled = false;
    const params = new URLSearchParams(window.location.search);
    const target = safeTarget(params.get('target'));

    async function initializeLogin() {
      try {
        await fetchJson<Record<string, unknown>>(`${API_BASE_URL}/api/auth/init`, {
          method: 'POST',
        });
        if (!cancelled) {
          setMessage('登录成功，正在进入系统');
          window.location.replace(target);
        }
      } catch {
        if (!cancelled) {
          setMessage('登录初始化失败，请刷新重试或联系管理员');
        }
      }
    }

    initializeLogin();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main className="shell">
      <section className="status-panel">
        <RefreshCw className="spin" aria-hidden="true" />
        <p>{message}</p>
      </section>
    </main>
  );
}

function safeTarget(value: string | null) {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')) {
    return '/';
  }
  return value;
}

function App() {
  const [currentUser, setCurrentUser] = React.useState<CurrentUserResponse | null>(null);
  const [loadingUser, setLoadingUser] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let cancelled = false;

    async function loadCurrentUser() {
      try {
        const user = await fetchJson<CurrentUserResponse>(`${API_BASE_URL}/api/current-user`);
        if (!cancelled) {
          setCurrentUser(user);
          setLoadingUser(false);
        }
      } catch (err) {
        if (!cancelled) {
          redirectToLogin((err as { loginPath?: string }).loginPath);
        }
      }
    }

    loadCurrentUser();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loadingUser) {
    return (
      <main className="shell">
        <section className="status-panel">
          <RefreshCw className="spin" aria-hidden="true" />
          <p>正在获取当前用户身份</p>
        </section>
      </main>
    );
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">React + Spring Security Demo</span>
          <h1>OAuth2 BFF PKCE</h1>
        </div>
      </header>

      <section className="workspace">
        <div className="identity-strip">
          <ShieldCheck size={22} aria-hidden="true" />
          <div>
            <span>当前用户 ID</span>
            <strong>{currentUser?.userId ?? 'unknown'}</strong>
          </div>
        </div>

        {error && <p className="error">{error}</p>}

        <pre className="result">
          {currentUser ? JSON.stringify(currentUser, null, 2) : '正在跳转到登录接口'}
        </pre>
      </section>
    </main>
  );
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {window.location.pathname === '/auth/init-page' ? <LoginInitPage /> : <App />}
  </StrictMode>,
);
