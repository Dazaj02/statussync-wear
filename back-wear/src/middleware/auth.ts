import type { MiddlewareHandler } from "hono";
import { AppError } from "../lib/errors.js";
import type { AuthUser, TokenVerifier } from "../lib/jwt.js";
import type { AppVariables } from "./requestId.js";

export type AuthVariables = AppVariables & { user: AuthUser };

export function auth(verify: TokenVerifier): MiddlewareHandler<{ Variables: AuthVariables }> {
  return async (c, next) => {
    const header = c.req.header("authorization") ?? "";
    const match = /^Bearer\s+(.+)$/i.exec(header);
    if (!match?.[1]) throw new AppError("unauthorized", "Falta el token de autenticación");
    c.set("user", await verify(match[1]));
    await next();
  };
}
