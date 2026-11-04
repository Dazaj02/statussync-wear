import { z } from "zod";

const schema = z.object({
  PORT: z.coerce.number().int().positive().default(8787),
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  LOG_LEVEL: z.enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"]).default("info"),
  SUPABASE_URL: z.string().url().optional(),
  SUPABASE_JWT_SECRET: z.string().min(1).optional(),
  SUPABASE_SERVICE_ROLE_KEY: z.string().min(1).optional(),
});

export type Env = z.infer<typeof schema>;

export function loadEnv(source: Record<string, string | undefined> = process.env): Env {
  const clean = Object.fromEntries(Object.entries(source).filter(([, v]) => v !== "" && v !== undefined));
  const parsed = schema.safeParse(clean);
  if (!parsed.success) {
    const detail = parsed.error.issues.map((i) => `${i.path.join(".")}: ${i.message}`).join("; ");
    throw new Error(`Configuración inválida: ${detail}`);
  }
  return parsed.data;
}
