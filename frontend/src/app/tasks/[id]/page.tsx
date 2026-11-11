import Link from "next/link";
import { notFound } from "next/navigation";
import { TaskDetail } from "@/components/task-history";

export default async function TaskPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!/^[a-f0-9-]{36}$/.test(id)) notFound();
  return <div className="page-shell task-detail-shell"><Link className="back-link" href="/history">← 返回我的作品</Link><h1>制作记录</h1><TaskDetail id={id} /></div>;
}
