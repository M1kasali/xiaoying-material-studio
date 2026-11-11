"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import type { Template } from "@/lib/catalog";

type GalleryState = { templates: Template[]; loading: boolean; error: string | null };

export function TemplateGallery() {
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState<GalleryState>({ templates: [], loading: true, error: null });

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      try {
        const query = category ? `?${new URLSearchParams({ category })}` : "";
        const [templateResponse, categoryResponse] = await Promise.all([
          fetch(`/api/templates${query}`, { signal: controller.signal }),
          fetch("/api/categories", { signal: controller.signal }),
        ]);
        if (!templateResponse.ok || !categoryResponse.ok) throw new Error("暂时无法加载模板，请重试。");
        const [templates, nextCategories] = await Promise.all([templateResponse.json(), categoryResponse.json()]);
        if (!controller.signal.aborted) {
          setCategories(nextCategories);
          setState({ templates, loading: false, error: null });
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          setState(previous => ({ ...previous, loading: false, error: error instanceof Error ? error.message : "加载失败，请重试。" }));
        }
      }
    }
    void load();
    return () => controller.abort();
  }, [category, attempt]);

  function selectCategory(next: string) {
    if (next === category) return;
    setState(previous => ({ ...previous, loading: true, error: null }));
    setCategory(next);
  }

  return (
    <section aria-label="模板库">
      <div className="catalog-toolbar">
        <div className="category-list" aria-label="按分类筛选">
          {["", ...categories].map(value => (
            <button key={value} type="button" className="category-button" aria-pressed={category === value} onClick={() => selectCategory(value)}>
              {value || "全部模板"}
            </button>
          ))}
        </div>
        <span className="catalog-count" aria-live="polite">{state.loading ? "加载中…" : `${state.templates.length} 个模板`}</span>
      </div>
      {state.error && (
        <div className="message-panel" role="alert">
          <p>{state.error}</p>
          <button className="primary-button" onClick={() => {
            setState(previous => ({ ...previous, loading: true, error: null }));
            setAttempt(value => value + 1);
          }}>重新加载</button>
        </div>
      )}
      {state.loading ? (
        <div className="template-grid" aria-label="正在加载模板" aria-busy="true">
          {Array.from({ length: 6 }, (_, index) => <div className="card-skeleton" key={index} />)}
        </div>
      ) : !state.error && state.templates.length === 0 ? (
        <div className="message-panel"><p>这个分类暂时没有模板，试试其他分类吧。</p></div>
      ) : !state.error && (
        <div className="template-grid">
          {state.templates.map(template => (
            <Link key={template.id} className="template-card" href={`/templates/${template.id}`}>
              <div className="card-image">
                <Image src={template.coverUrl} alt={`${template.title}示例封面`} fill sizes="(max-width: 600px) 50vw, (max-width: 1000px) 33vw, 25vw" unoptimized />
                <span className="image-count">{template.requiredImageCount} 张照片</span>
              </div>
              <div className="card-caption"><span className="card-category">{template.category}</span><h2>{template.title}</h2></div>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}
