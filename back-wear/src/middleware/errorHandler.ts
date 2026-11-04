import type { ErrorHandler, NotFoundHandler } from "hono";
import type { Logger } from "pino";
import { AppError, STATUS_BY_CODE, errorBody } from "../lib/errors.js";

export function createErrorHandler(logger: Logger): ErrorHandler {
  return (err, c) => {
    if (err instanceof AppError) {
      return c.json(errorBody(err.code, err.message), STATUS_BY_CODE[err.code]);
    }
    logger.error({ err, requestId: c.get("requestId") }, "unhandled error");
    return c.json(errorBody("internal", "Error interno del servidor"), 500);
  };
}

export const notFoundHandler: NotFoundHandler = (c) =>
  c.json(errorBody("not_found", "Ruta no encontrada"), 404);
