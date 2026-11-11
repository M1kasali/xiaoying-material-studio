"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api, ApiError, message } from "@/lib/client-api";
import { active, mergeTask, pollDelay, stages, stageLabels, statusLabels, type Task } from "@/lib/task-state";
import { useSession } from "./session-provider";

function restartDraft(event: React.MouseEvent<HTMLAnchorElement>) {
  if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
  event.preventDefault();
  const destination = new URL(event.currentTarget.href);
  // A distinct draft URL also resets repeated retries of the same history entry.
  destination.searchParams.set("retry", crypto.randomUUID());
  window.location.assign(destination.href);
}

export function TaskCard({ id, initial, full = false }: { id: string; initial?: Task; full?: boolean }) {
  const [task, setTask] = useState<Task | null>(initial ?? null);
  const [queryError, setQueryError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const { invalidate } = useSession();
  const simulatedFailure = task?.status === "FAILED" && task.failureReason?.startsWith("模拟失败：");

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    let failures = 0;
    let latest = initial ?? null;
    async function poll() {
      try {
        const next = await api<Task>(`/api/tasks/${encodeURIComponent(id)}`, { signal: controller.signal });
        if (controller.signal.aborted) return;
        latest = mergeTask(latest, next);
        setTask(previous => mergeTask(previous, next));
        setQueryError(null); failures = 0;
        if (!active(latest)) return;
      } catch (err) {
        if (controller.signal.aborted) return;
        if (err instanceof ApiError && err.status === 401) { invalidate(); return; }
        setQueryError(err instanceof ApiError && err.status === 404 ? "任务不存在，或不属于当前账户。" : `进度查询暂时失败：${message(err)} 后台任务可能仍在继续。`);
        if (err instanceof ApiError && err.status === 404) return;
        failures++;
      }
      timer = setTimeout(() => void poll(), pollDelay(failures));
    }
    if (!latest || active(latest)) void poll();
    return () => { controller.abort(); if (timer) clearTimeout(timer); };
  }, [id, initial, attempt, invalidate]);

  return <article className={`task-card ${full ? "task-full" : ""}`} data-task-id={id} data-status={task?.status ?? "LOADING"}>
    <div className="task-heading"><div><p className="eyebrow">{task ? new Date(task.createdAt).toLocaleString("zh-CN", { hour12: false }) : "正在读取任务"}</p><h2>{task ? <Link href={`/tasks/${id}`}>{task.templateTitle}</Link> : "加载制作记录…"}</h2></div>{task && <span className={`status-pill status-${task.status.toLowerCase()}`} role="status">{simulatedFailure ? "模拟失败 · 演示" : statusLabels[task.status]}</span>}</div>
    {queryError && <div className="query-warning" role="alert"><p>{queryError}</p><button className="text-button" onClick={() => setAttempt(value => value + 1)}>重新查询</button></div>}
    {task && <>
      <ol className="task-steps" aria-label="制作步骤">{stages.map((stage, index) => {
        const step = task.steps.find(value => value.stage === stage);
        const label = step?.status === "SUCCEEDED" ? "已完成" : step?.status === "FAILED" ? "失败" : step?.status === "RUNNING" ? "进行中" : task.status === "FAILED" ? "未执行" : "等待";
        return <li key={stage} className={`step-${step?.status.toLowerCase() ?? "waiting"}`}><span className="step-dot">{step?.status === "SUCCEEDED" ? "✓" : index + 1}</span><span>{stageLabels[stage]}<small>{label}</small></span></li>;
      })}</ol>
      {task.status === "QUEUED" && <p className="muted">任务已受理，正在等待空闲制作位置。</p>}
      {task.status === "RUNNING" && <p className="muted">可以离开页面或继续创建新任务，在「我的作品」查看进度。</p>}
      {task.status === "FAILED" && <div className="failure-panel" role="alert"><strong>{task.failureStage ? `${stageLabels[task.failureStage]}失败` : "制作未完成"}</strong>{simulatedFailure && <p>这是主动指定的失败测试，任务已按设置中止。正常制作默认不触发此失败。</p>}<p>{task.failureReason ?? "任务未能完成，请重新制作。"}</p><a className="secondary-button" href={`/templates/${task.templateId}?retry=${encodeURIComponent(task.id)}#production-form`} onClick={restartDraft}>重新制作</a></div>}
      {task.status === "SUCCEEDED" && task.resultUrl && <div className="result-panel">
        {/* Authenticated, original uploaded image; Next image optimization must not cache it publicly. */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={task.resultUrl} alt={`${task.templateTitle}制作结果`} className="result-image" loading="lazy" />
        <div><p>结果已保存</p><p className="muted">演示结果为第一张素材的副本，未进行 AI 图像变换。</p><a className="primary-button" href={task.resultUrl} download>下载结果</a></div>
      </div>}
      <div className="task-meta"><span>任务 {id.slice(0, 8)}</span>{!full && <Link href={`/tasks/${id}`}>查看详情 →</Link>}</div>
    </>}
  </article>;
}
