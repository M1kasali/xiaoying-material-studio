import { TemplateGallery } from "@/components/template-gallery";

export default function Home() {
  return (
    <div className="page-shell">
      <section className="intro" aria-labelledby="page-title">
        <p className="eyebrow">灵感，从这里开始</p>
        <h1 id="page-title">给你的照片，<br className="mobile-break" />一个新故事。</h1>
        <p className="muted">旅行、日常，还有你在意的小瞬间。选一个模板，留下来。</p>
      </section>
      <TemplateGallery />
    </div>
  );
}
