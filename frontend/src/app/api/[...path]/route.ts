import { NextRequest } from "next/server";
import { backendUrl } from "@/lib/backend";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// Only these application resources can reach the fixed Java upstream.
const routes: Record<string, RegExp> = {
  GET: /^(me|tasks|tasks\/[a-f0-9-]{36}(\/result)?|assets\/[a-f0-9-]{36}\/content)$/,
  POST: /^(demo\/session|assets|tasks)$/,
  DELETE: /^session$/,
};

async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const path = (await context.params).path.join("/");
  if (!routes[request.method]?.test(path)) return Response.json({ detail: "接口不存在" }, { status: 404 });
  const headers = new Headers();
  for (const name of ["cookie", "content-type", "origin", "sec-fetch-site"]) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }
  try {
    let body: Uint8Array | undefined;
    if (request.body) {
      const reader = request.body.getReader();
      const parts: Uint8Array[] = [];
      let length = 0;
      for (;;) {
        const { done, value } = await reader.read();
        if (done) break;
        length += value.byteLength;
        if (length > 6 * 1024 * 1024) {
          await reader.cancel();
          return Response.json({ detail: "上传内容过大，每张图片不能超过 5 MB" }, { status: 413 });
        }
        parts.push(value);
      }
      body = new Uint8Array(length);
      let offset = 0;
      for (const part of parts) { body.set(part, offset); offset += part.length; }
    }
    const response = await fetch(backendUrl(`/api/${path}${request.nextUrl.search}`), {
      method: request.method, headers, body: body as BodyInit | undefined,
      cache: "no-store", redirect: "manual",
      signal: AbortSignal.any([request.signal, AbortSignal.timeout(20000)]),
    });
    const output = new Headers({ "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" });
    for (const name of ["content-type", "content-disposition"]) {
      const value = response.headers.get(name);
      if (value) output.set(name, value);
    }
    for (const cookie of response.headers.getSetCookie()) output.append("set-cookie", cookie);
    return new Response(response.body, { status: response.status, headers: output });
  } catch {
    return Response.json({ detail: "服务暂时无法连接，请稍后重试。" }, { status: 503 });
  }
}

export { proxy as GET, proxy as POST, proxy as DELETE };
