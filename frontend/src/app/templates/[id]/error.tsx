"use client";

import Link from "next/link";

export default function ErrorPage({ reset }: { reset: () => void }) {
  return <div className="page-shell message-panel" role="alert"><h1>暂时无法加载模板</h1><p className="muted">请检查连接后重试，或返回模板库选择其他模板。</p><button className="primary-button" onClick={reset}>重试</button> <Link className="back-link" href="/">返回模板库</Link></div>;
}
