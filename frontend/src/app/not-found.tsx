import Link from "next/link";

export default function NotFound() {
  return <div className="page-shell message-panel"><h1>没有找到这个模板</h1><p className="muted">回到模板库，挑选一个新的灵感。</p><Link className="primary-button" href="/">返回模板库</Link></div>;
}
