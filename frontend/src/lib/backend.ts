import "server-only";

export function backendUrl(path: string) {
  return `${process.env.BACKEND_URL ?? "http://localhost:8080"}${path}`;
}

// Only fixed catalog paths are proxied; clients cannot choose an upstream host.
export async function proxyCatalog(path: string) {
  try {
    const response = await fetch(backendUrl(path), {
      cache: "no-store",
      signal: AbortSignal.timeout(8000),
    });
    return new Response(await response.text(), {
      status: response.status,
      headers: {
        "Content-Type": response.headers.get("Content-Type") ?? "application/json",
        "Cache-Control": "no-store",
      },
    });
  } catch {
    return Response.json({ detail: "暂时无法获取模板，请稍后重试。" }, { status: 503 });
  }
}
