"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import type { Template } from "@/lib/catalog";
import { api, ApiError, message } from "@/lib/client-api";
import { fileError, stageLabels, stages, type Stage, type Task } from "@/lib/task-state";
import { AccountGate, useSession } from "./session-provider";
import { HistoryList } from "./task-history";

type Asset = { id: string; name: string; previewUrl: string };
type Slot = { status: "empty" | "uploading" | "ready" | "error"; asset?: Asset; name?: string; preview?: string; error?: string };

function UploadSlot({ index, slot, template, locked, onChange }: { index: number; slot: Slot; template: Template; locked: boolean; onChange: (slot: Slot) => void }) {
  const pending = useRef<AbortController | null>(null);
  const preview = useRef<string | null>(null);
  const { invalidate } = useSession();
  useEffect(() => () => { pending.current?.abort(); if (preview.current) URL.revokeObjectURL(preview.current); }, []);

  function clear() {
    pending.current?.abort();
    if (preview.current) URL.revokeObjectURL(preview.current);
    preview.current = null;
  }
  async function select(file: File) {
    clear();
    const invalid = fileError(file, template.acceptedMimeTypes, template.maxFileSizeBytes);
    if (invalid) { onChange({ status: "error", name: file.name, error: invalid }); return; }
    const controller = new AbortController(); pending.current = controller;
    const url = URL.createObjectURL(file); preview.current = url;
    onChange({ status: "uploading", name: file.name, preview: url });
    const body = new FormData(); body.append("file", file);
    try {
      const asset = await api<Asset>("/api/assets", { method: "POST", body, signal: controller.signal });
      if (!controller.signal.aborted) onChange({ status: "ready", name: file.name, preview: url, asset });
    } catch (err) {
      if (controller.signal.aborted) return;
      if (err instanceof ApiError && err.status === 401) { invalidate(); return; }
      onChange({ status: "error", name: file.name, preview: url, error: `上传失败：${message(err)}` });
    }
  }

  return <div className={`upload-slot upload-${slot.status}`}>
    <label className="upload-picker">
      <input type="file" accept={template.acceptedMimeTypes.join(",")} aria-label={`选择第 ${index + 1} 张图片`} disabled={locked} onChange={event => { const file = event.target.files?.[0]; event.target.value = ""; if (file) void select(file); }} />
      {slot.preview ? (
        // Local object URLs cannot use server-side image optimization.
        // eslint-disable-next-line @next/next/no-img-element
        <img src={slot.preview} alt={`素材 ${index + 1} 预览`} />
      ) : <span className="upload-plus" aria-hidden="true">＋</span>}
      <span className="upload-label">{slot.preview ? `替换图片 ${index + 1}` : `添加图片 ${index + 1}`}</span>
    </label>
    <div className="upload-caption"><span className="file-name" title={slot.name}>{slot.name ?? "JPG / PNG · 最大 5 MB"}</span><span role="status">{slot.status === "uploading" ? "上传中…" : slot.status === "ready" ? "✓ 上传完成" : slot.status === "error" ? "需要重新选择" : "等待选择"}</span></div>
    {slot.error && <p className="error-text" role="alert">{slot.error}</p>}
    {slot.status !== "empty" && <button type="button" className="text-button" disabled={locked} onClick={() => { clear(); onChange({ status: "empty" }); }}>移除图片 {index + 1}</button>}
  </div>;
}

function Form({ template, onCreated, hasCreated }: { template: Template; onCreated: (task: Task) => void; hasCreated: boolean }) {
  const [slots, setSlots] = useState<Slot[]>(() => Array.from({ length: template.requiredImageCount }, () => ({ status: "empty" })));
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [delay, setDelay] = useState(800);
  const [failAt, setFailAt] = useState<Stage | "">("");
  const pending = useRef<AbortController | null>(null);
  const busy = useRef(false);
  const { invalidate } = useSession();
  useEffect(() => () => pending.current?.abort(), []);
  const ready = slots.every(slot => slot.status === "ready" && slot.asset);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!ready || busy.current) return;
    busy.current = true; setSubmitting(true); setSubmitError(null);
    const controller = new AbortController(); pending.current = controller;
    try {
      const task = await api<Task>("/api/tasks", { method: "POST", headers: { "Content-Type": "application/json" }, signal: controller.signal,
        body: JSON.stringify({ templateId: template.id, assetIds: slots.map(slot => slot.asset!.id), simulation: { delayMs: delay, ...(failAt ? { failAt } : {}) } }),
      });
      if (!controller.signal.aborted) onCreated(task);
    } catch (err) {
      if (controller.signal.aborted) return;
      if (err instanceof ApiError && err.status === 401) { invalidate(); return; }
      setSubmitError(err instanceof ApiError && err.status < 500 ? message(err) : "未能确认任务是否已受理。请先查看「我的作品」，确认没有新任务后再提交，避免重复制作。");
    } finally { busy.current = false; if (!controller.signal.aborted) setSubmitting(false); }
  }

  return <div className="production-area">
    <form onSubmit={event => void submit(event)}>
      <div className="section-heading"><h2>上传你的照片</h2><span className="muted">{slots.filter(slot => slot.status === "ready").length} / {template.requiredImageCount} 已上传</span></div>
      <div className="upload-grid">{slots.map((slot, index) => <UploadSlot key={index} index={index} slot={slot} template={template} locked={submitting} onChange={next => setSlots(previous => previous.map((value, i) => i === index ? next : value))} />)}</div>
      <details className="simulation-controls"><summary>演示设置 · 可调整速度与指定失败阶段</summary><div className="simulation-fields">
        <label>每阶段耗时<select aria-label="每阶段耗时" value={delay} disabled={submitting} onChange={event => setDelay(Number(event.target.value))}><option value={800}>快速 · 0.8 秒</option><option value={2500}>标准 · 2.5 秒</option><option value={8000}>慢速 · 8 秒</option></select></label>
        <label>模拟执行结果<select aria-label="模拟执行结果" value={failAt} disabled={submitting} onChange={event => setFailAt(event.target.value as Stage | "")}><option value="">全部成功</option>{stages.map(stage => <option key={stage} value={stage}>{stageLabels[stage]}失败</option>)}</select></label>
      </div></details>
      <p className="muted simulation-note">此为流程演示，结果使用第一张素材的副本，不调用 AI。</p>
      {submitError && <div className="query-warning" role="alert"><p>{submitError}</p><Link href="/history">查看我的作品 →</Link></div>}
      <div className="button-row"><button className="primary-button" type="submit" disabled={!ready || submitting}>{submitting ? "正在提交…" : hasCreated ? "再创建一个任务" : "开始制作"}</button><span className="muted">{submitting ? "等待服务器确认受理" : !ready ? `请先上传 ${template.requiredImageCount} 张图片` : "素材已就绪"}</span></div>
      {hasCreated && <p className="accepted-note" role="status">任务已受理，可继续创建。刷新后仍可查看制作记录。</p>}
    </form>
  </div>;
}

function Studio({ template, children }: { template: Template; children: React.ReactNode }) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const { user, loading } = useSession();
  return <div className="studio-layout">
    <section className="studio-input">{children}<AccountGate><Form template={template} hasCreated={tasks.length > 0} onCreated={task => setTasks(previous => [task, ...previous])} /></AccountGate></section>
    <section className="studio-history"><div className="section-heading"><h2>制作记录</h2><Link href="/history">查看全部作品 →</Link></div>{user ? <HistoryList recent={tasks} /> : <div className="message-panel"><p className="muted">{loading ? "正在读取账户…" : "选择演示账户后，这里会显示你的制作进度与历史结果。"}</p></div>}</section>
  </div>;
}

export function ProductionForm({ template, children }: { template: Template; children: React.ReactNode }) {
  const { user } = useSession();
  return <Studio key={`${template.id}:${user?.id ?? "guest"}`} template={template}>{children}</Studio>;
}
