import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "拾光 · 素材工作台",
  description: "从一张照片开始，记录你的灵感。",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>
        <a className="skip-link" href="#main">跳转到主要内容</a>
        <header className="site-header">
          <Link className="brand" href="/" aria-label="拾光首页">
            <span className="brand-mark" aria-hidden="true">拾</span>
            <span>拾光 <span className="brand-subtitle">素材工作台</span></span>
          </Link>
          <nav aria-label="主导航"><Link className="nav-link" href="/">发现模板</Link></nav>
        </header>
        <main id="main">{children}</main>
        <footer className="site-footer">把普通的一天，留成喜欢的画面。</footer>
      </body>
    </html>
  );
}
