"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { api, ApiError, message } from "@/lib/client-api";
import type { Task } from "@/lib/task-state";
import { AccountGate, useSession } from "./session-provider";
import { TaskCard } from "./task-card";

export function HistoryList({ recent = [] }: { recent?: Task[] }) {
  const [items, setItems] = useState<Task[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pending = useRef<AbortController | null>(null);
  const { invalidate, user } = useSession();

  const load = useCallback(async (next?: string) => {
    pending.current?.abort();
    const controller = new AbortController(); pending.current = controller;
    setLoading(true); setError(null);
    try {
      const result = await api<{ items: Task[]; nextCursor: string | null }>(`/api/tasks?${new URLSearchParams({ limit: "12", ...(next ? { cursor: next } : {}) })}`, { signal: controller.signal });
      if (!controller.signal.aborted) {
        setItems(previous => next ? [...previous, ...result.items.filter(task => !previous.some(value => value.id === task.id))] : result.items);
        setCursor(result.nextCursor);
      }
    } catch (err) {
      if (controller.signal.aborted) return;
      if (err instanceof ApiError && err.status === 401) { invalidate(); return; }
      setError(message(err));
    } finally { if (!controller.signal.aborted) setLoading(false); }
  }, [invalidate]);

  // Fetch persisted history on mount; the initial loading flag is already true.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void load(); return () => pending.current?.abort(); }, [load]);
  return <section aria-label="个人制作历史">
    <div className="section-heading"><p className="muted">{user?.displayName}的作品 · 未完成任务会自动更新</p><button className="secondary-button" disabled={loading} onClick={() => void load()}>刷新列表</button></div>
    {error && <div className="query-warning" role="alert"><p>历史加载失败：{error}</p><button className="text-button" onClick={() => void load()}>重新加载</button></div>}
    {!loading && !error && !items.length && !recent.length && <div className="message-panel"><h2>还没有作品</h2><p className="muted">选一个喜欢的模板，让第一张照片成为作品。</p><Link className="primary-button" href="/">去选模板</Link></div>}
    <div className="history-grid">{[...recent, ...items.filter(task => !recent.some(value => value.id === task.id))].map(task => <TaskCard key={task.id} id={task.id} initial={task} />)}</div>
    {loading && <p role="status" className="muted">正在加载作品…</p>}
    {cursor && <button className="secondary-button load-more" disabled={loading} onClick={() => void load(cursor)}>加载更多作品</button>}
  </section>;
}
export function TaskHistory() { return <AccountGate><HistoryList /></AccountGate>; }
export function TaskDetail({ id }: { id: string }) { return <AccountGate><TaskCard key={id} id={id} full /></AccountGate>; }
