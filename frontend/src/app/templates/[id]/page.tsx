import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { backendUrl } from "@/lib/backend";
import type { Template } from "@/lib/catalog";
import { ProductionForm } from "@/components/production-form";

export const dynamic = "force-dynamic";

export default async function TemplateDetail({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ retry?: string | string[] }> }) {
  const { id } = await params;
  const { retry } = await searchParams;
  const retryKey = typeof retry === "string" && /^[a-f0-9-]{36}$/.test(retry) ? retry : "";
  const response = await fetch(backendUrl(`/api/templates/${encodeURIComponent(id)}`), {
    cache: "no-store", signal: AbortSignal.timeout(8000),
  });
  if (response.status === 404) notFound();
  if (!response.ok) throw new Error("无法加载模板");
  const template: Template = await response.json();
  return (
    <div className="page-shell">
      <Link className="back-link" href="/">← 返回模板库</Link>
      <ProductionForm template={template} retryKey={retryKey}><article className="studio-template">
        <div className="detail-cover"><Image src={template.coverUrl} alt={`${template.title}示例封面`} fill sizes="(max-width: 700px) 90vw, 420px" unoptimized priority /></div>
        <div className="detail-copy">
          <p className="eyebrow">{template.category}</p><h1>{template.title}</h1>
          <p className="muted">{template.description}</p>
          <div className="requirements"><h2>素材要求</h2><p>{template.requiredImageCount} 张图片 · JPG / PNG</p><p className="muted">每张不超过 {template.maxFileSizeBytes / 1024 / 1024} MB</p><p className="demo-downloads">没有现成照片？下载 <a href="/demo/input-01.png" download>演示图片 1</a> / <a href="/demo/input-02.png" download>图片 2</a></p></div>
        </div>
      </article></ProductionForm>
    </div>
  );
}
