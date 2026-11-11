"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { api, ApiError, message } from "@/lib/client-api";

type User = { id: string; displayName: string };
type Session = { user: User | null; loading: boolean; error: string | null; login: (account: string) => void; logout: () => void; invalidate: () => void; refresh: () => void };
const Context = createContext<Session | null>(null);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pending = useRef<AbortController | null>(null);
  const mutating = useRef(false);
  const channel = useRef<BroadcastChannel | null>(null);
  const invalidate = useCallback(() => { setUser(null); setError("会话已失效，请重新选择演示账户。"); }, []);

  const refresh = useCallback(async () => {
    if (mutating.current) return;
    pending.current?.abort();
    const controller = new AbortController(); pending.current = controller;
    try {
      const result = await api<User>("/api/me", { signal: controller.signal });
      if (!controller.signal.aborted) { setUser(result); setError(null); }
    } catch (err) {
      if (!controller.signal.aborted) {
        setUser(null);
        setError(err instanceof ApiError && err.status === 401 ? null : message(err));
      }
    } finally { if (!controller.signal.aborted) setLoading(false); }
  }, []);

  useEffect(() => {
    // Reconcile the HttpOnly session with the server on mount; updates follow the network response.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void refresh();
    const onFocus = () => { void refresh(); };
    window.addEventListener("focus", onFocus);
    if (typeof BroadcastChannel !== "undefined") {
      channel.current = new BroadcastChannel("studio-session");
      channel.current.onmessage = () => { setUser(null); setLoading(true); void refresh(); };
    }
    return () => { pending.current?.abort(); window.removeEventListener("focus", onFocus); channel.current?.close(); };
  }, [refresh]);

  async function change(account?: string) {
    if (mutating.current) return;
    mutating.current = true;
    pending.current?.abort();
    const controller = new AbortController(); pending.current = controller;
    setUser(null); setLoading(true); setError(null);
    try {
      const result = account
        ? await api<User>("/api/demo/session", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ account }), signal: controller.signal })
        : await api<void>("/api/session", { method: "DELETE", signal: controller.signal });
      if (!controller.signal.aborted) { setUser(result ?? null); channel.current?.postMessage("changed"); }
    } catch (err) {
      if (!controller.signal.aborted) setError(message(err));
    } finally { mutating.current = false; if (!controller.signal.aborted) setLoading(false); }
  }

  return <Context.Provider value={{ user, loading, error, login: account => void change(account), logout: () => void change(), invalidate, refresh: () => void refresh() }}>{children}</Context.Provider>;
}

export function useSession() {
  const session = useContext(Context);
  if (!session) throw new Error("SessionProvider missing");
  return session;
}

export function AccountMenu() {
  const { user, loading, login, logout } = useSession();
  return <div className="account-menu">
    <label className="sr-only" htmlFor="account">演示账户</label>
    <select id="account" aria-label="演示账户" disabled={loading} value={user ? (user.displayName === "小拾" ? "alice" : "bob") : ""} onChange={event => event.target.value ? login(event.target.value) : logout()}>
      <option value="">{loading ? "连接中…" : "选择演示账户"}</option><option value="alice">小拾 · 账户 A</option><option value="bob">小光 · 账户 B</option>
    </select>
    {user && <button className="text-button" onClick={logout}>退出</button>}
  </div>;
}

export function AccountGate({ children }: { children: React.ReactNode }) {
  const { user, loading, error, login, refresh } = useSession();
  if (loading) return <div className="message-panel" role="status">正在确认账户…</div>;
  if (!user) return <div className="message-panel account-gate">
    <h2>选择一个演示账户，开始制作</h2><p className="muted">两个账户的素材和作品分别保存。演示无需密码。</p>
    {error && <p role="alert" className="error-text">{error}</p>}
    <div className="button-row"><button className="primary-button" onClick={() => login("alice")}>使用小拾 · 账户 A</button><button className="secondary-button" onClick={() => login("bob")}>使用小光 · 账户 B</button>{error && <button className="text-button" onClick={refresh}>重新连接</button>}</div>
  </div>;
  return <div key={user.id}>{children}</div>;
}
