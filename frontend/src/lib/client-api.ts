export class ApiError extends Error {
  constructor(message: string, public status: number) { super(message); }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...init, credentials: "same-origin", cache: "no-store",
    signal: init.signal ? AbortSignal.any([init.signal, AbortSignal.timeout(25000)]) : AbortSignal.timeout(25000),
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    throw new ApiError(problem?.detail ?? "请求暂时失败，请重试。", response.status);
  }
  return response.status === 204 ? undefined as T : response.json();
}

export function message(error: unknown): string {
  return error instanceof Error && error.name !== "TypeError" && error.name !== "TimeoutError"
    ? error.message : "网络连接异常，请稍后重试。";
}
