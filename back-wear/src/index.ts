import { serve } from "@hono/node-server";
import { createApp } from "./app.js";
import { loadEnv } from "./config/env.js";
import { createTokenVerifier } from "./lib/jwt.js";
import { createLogger } from "./lib/logger.js";
import { MemoryStatusRepository } from "./repositories/memory/status.js";
import { createStatusService } from "./services/status.js";

const env = loadEnv();
const logger = createLogger(env);

const verifyToken = createTokenVerifier({
  ...(env.SUPABASE_JWT_SECRET ? { secret: env.SUPABASE_JWT_SECRET } : {}),
  ...(env.SUPABASE_URL ? { supabaseUrl: env.SUPABASE_URL } : {}),
});
if (!env.SUPABASE_JWT_SECRET && !env.SUPABASE_URL) {
  logger.warn("Sin SUPABASE_JWT_SECRET ni SUPABASE_URL: toda petición autenticada será rechazada");
}

// B1: repositorio en memoria. El adaptador Supabase llega en B4.
const statusService = createStatusService({ repo: new MemoryStatusRepository() });
const app = createApp({ logger, verifyToken, statusService });

serve({ fetch: app.fetch, port: env.PORT }, (info) => {
  logger.info(`back-wear escuchando en el puerto ${info.port}`);
});
