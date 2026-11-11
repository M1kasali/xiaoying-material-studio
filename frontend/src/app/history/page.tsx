import { TaskHistory } from "@/components/task-history";

export default function HistoryPage() {
  return <div className="page-shell"><div className="intro"><p className="eyebrow">每一份灵感，都有迹可循</p><h1>我的作品</h1><p className="muted">查看制作进度，重拾已经完成的画面。刷新页面，记录依然在这里。</p></div><TaskHistory /></div>;
}
