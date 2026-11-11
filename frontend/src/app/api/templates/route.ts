import { proxyCatalog } from "@/lib/backend";

export async function GET(request: Request) {
  const category = new URL(request.url).searchParams.get("category");
  const query = category ? `?${new URLSearchParams({ category })}` : "";
  return proxyCatalog(`/api/templates${query}`);
}
