import { cpSync, mkdirSync } from "node:fs";

// Standalone output omits static files. Include them so pnpm start and Docker
// run the same complete production bundle.
mkdirSync(".next/standalone/.next", { recursive: true });
cpSync("public", ".next/standalone/public", { recursive: true });
cpSync(".next/static", ".next/standalone/.next/static", { recursive: true });
