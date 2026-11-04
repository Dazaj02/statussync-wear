import { Hono } from "hono";
import type { Logger } from "pino";
import { createErrorHandler, notFoundHandler } from "./middleware/errorHandler.js";
import { requestId, type AppVariables } from "./middleware/requestId.js";
import { health } from "./routes/health.js";

export function createApp(deps: { logger: Logger }) {
  const app = new Hono<{ Variables: AppVariables }>();
  app.use("*", requestId);
  app.route("/health", health); // alias para el health check del hosting
  app.route("/v1/health", health);
  app.onError(createErrorHandler(deps.logger));
  app.notFound(notFoundHandler);
  return app;
}
