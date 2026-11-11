import { proxyCatalog } from "@/lib/backend";

export async function GET() {
  return proxyCatalog("/api/categories");
}
