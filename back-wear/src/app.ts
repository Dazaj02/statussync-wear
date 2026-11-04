import { Hono } from "hono";
import type { Logger } from "pino";
import type { TokenVerifier } from "./lib/jwt.js";
import { auth } from "./middleware/auth.js";
import { createErrorHandler, notFoundHandler } from "./middleware/errorHandler.js";
import { requestId, type AppVariables } from "./middleware/requestId.js";
import { health } from "./routes/health.js";
import { statusRoutes } from "./routes/status.js";
import type { StatusService } from "./services/status.js";

export interface AppDeps {
  logger: Logger;
  verifyToken: TokenVerifier;
  statusService: StatusService;
}

export function createApp(deps: AppDeps) {
  const app = new Hono<{ Variables: AppVariables }>();
  app.use("*", requestId);
  app.route("/health", health); // alias para el health check del hosting

  const v1 = new Hono();
  v1.route("/health", health);
  v1.use("/status", auth(deps.verifyToken));
  v1.route("/status", statusRoutes(deps.statusService));
  app.route("/v1", v1);

  app.onError(createErrorHandler(deps.logger));
  app.notFound(notFoundHandler);
  return app;
}
