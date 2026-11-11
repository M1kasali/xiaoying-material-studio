import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { backendUrl } from "@/lib/backend";
import type { Template } from "@/lib/catalog";

export const dynamic = "force-dynamic";

export default async function TemplateDetail({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const response = await fetch(backendUrl(`/api/templates/${encodeURIComponent(id)}`), {
    cache: "no-store", signal: AbortSignal.timeout(8000),
  });
  if (response.status === 404) notFound();
  if (!response.ok) throw new Error("无法加载模板");
  const template: Template = await response.json();
  return (
    <div className="page-shell">
      <Link className="back-link" href="/">← 返回模板库</Link>
      <article className="detail-layout">
        <div className="detail-cover"><Image src={template.coverUrl} alt={`${template.title}示例封面`} fill sizes="(max-width: 700px) 90vw, 420px" unoptimized priority /></div>
        <div className="detail-copy">
          <p className="eyebrow">{template.category}</p><h1>{template.title}</h1>
          <p className="muted">{template.description}</p>
          <div className="requirements"><h2>素材要求</h2><p>{template.requiredImageCount} 张图片 · JPG / PNG</p><p className="muted">每张不超过 {template.maxFileSizeBytes / 1024 / 1024} MB</p></div>
        </div>
      </article>
    </div>
  );
}
