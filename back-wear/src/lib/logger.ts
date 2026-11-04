import pino, { type Logger } from "pino";
import type { Env } from "../config/env.js";

export function createLogger(env: Pick<Env, "LOG_LEVEL" | "NODE_ENV">): Logger {
  return pino({
    level: env.NODE_ENV === "test" ? "silent" : env.LOG_LEVEL,
    redact: ["req.headers.authorization", "*.service_role", "*.SUPABASE_SERVICE_ROLE_KEY"],
    ...(env.NODE_ENV === "development" ? { transport: { target: "pino-pretty" } } : {}),
  });
}
