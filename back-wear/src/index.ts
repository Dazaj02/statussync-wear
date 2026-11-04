import { serve } from "@hono/node-server";
import { createApp } from "./app.js";
import { loadEnv } from "./config/env.js";
import { createLogger } from "./lib/logger.js";

const env = loadEnv();
const logger = createLogger(env);
const app = createApp({ logger });

serve({ fetch: app.fetch, port: env.PORT }, (info) => {
  logger.info(`back-wear escuchando en el puerto ${info.port}`);
});
