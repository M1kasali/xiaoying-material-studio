export type Stage = "VALIDATE" | "GENERATE" | "FINALIZE";
export type Status = "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED";
export type Task = {
  id: string; templateId: string; templateTitle: string; status: Status;
  currentStage: Stage | null; createdAt: string; updatedAt: string; finishedAt: string | null;
  resultUrl: string | null; failureStage: Stage | null; failureReason: string | null;
  steps: { stage: Stage; status: string; startedAt: string; finishedAt: string | null; errorMessage: string | null }[];
};
export const stages: Stage[] = ["VALIDATE", "GENERATE", "FINALIZE"];
export const stageLabels: Record<Stage, string> = { VALIDATE: "检查素材", GENERATE: "模拟生成", FINALIZE: "整理结果" };
export const statusLabels: Record<Status, string> = { QUEUED: "排队中", RUNNING: "制作中", SUCCEEDED: "制作完成", FAILED: "制作失败" };
export function active(task: Task) { return task.status === "QUEUED" || task.status === "RUNNING"; }

// A late response must never replace another task or roll a finished task back.
export function mergeTask(previous: Task | null, incoming: Task): Task {
  if (!previous) return incoming;
  if (previous.id !== incoming.id || !active(previous)) return previous;
  if (Date.parse(incoming.updatedAt) < Date.parse(previous.updatedAt)) return previous;
  if (previous.status === "RUNNING" && incoming.status === "QUEUED") return previous;
  return incoming;
}
export function pollDelay(failures: number) { return Math.min(1500 * 2 ** Math.min(failures, 3), 12000); }

export function fileError(file: { type: string; size: number }, accepted: string[], max: number) {
  if (!accepted.includes(file.type)) return "请选择 JPG 或 PNG 图片。";
  if (file.size === 0) return "图片内容为空，请重新选择。";
  if (file.size > max) return `每张图片不能超过 ${max / 1024 / 1024} MB。`;
  return null;
}
