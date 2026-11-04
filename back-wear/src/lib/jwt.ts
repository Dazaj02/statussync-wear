import {
  createRemoteJWKSet,
  decodeProtectedHeader,
  jwtVerify,
  type JWTPayload,
  type JWTVerifyGetKey,
} from "jose";
import { AppError } from "./errors.js";

export interface AuthUser {
  userId: string;
  displayName?: string;
}
export type TokenVerifier = (token: string) => Promise<AuthUser>;

export interface VerifierOptions {
  /** Secreto compartido (proyectos Supabase con HS256). */
  secret?: string;
  /** URL del proyecto; se usa para el JWKS (claves asimétricas). */
  supabaseUrl?: string;
  /** Inyección de un resolvedor de claves (tests). */
  jwks?: JWTVerifyGetKey;
}

export function createTokenVerifier(opts: VerifierOptions): TokenVerifier {
  const jwks: JWTVerifyGetKey | undefined =
    opts.jwks ??
    (opts.supabaseUrl
      ? createRemoteJWKSet(new URL("/auth/v1/.well-known/jwks.json", opts.supabaseUrl))
      : undefined);
  const secretKey = opts.secret ? new TextEncoder().encode(opts.secret) : undefined;

  return async (token) => {
    try {
      const { alg } = decodeProtectedHeader(token);
      let payload: JWTPayload;
      if (typeof alg === "string" && alg.startsWith("HS")) {
        if (!secretKey) throw new Error("HS256 no configurado");
        ({ payload } = await jwtVerify(token, secretKey, { algorithms: ["HS256", "HS384", "HS512"] }));
      } else {
        if (!jwks) throw new Error("JWKS no configurado");
        ({ payload } = await jwtVerify(token, jwks, { algorithms: ["ES256", "RS256", "EdDSA"] }));
      }
      if (!payload.sub) throw new Error("sin sub");
      const meta = payload["user_metadata"] as Record<string, unknown> | undefined;
      const name = meta?.["full_name"] ?? meta?.["name"];
      return {
        userId: payload.sub,
        ...(typeof name === "string" && name ? { displayName: name } : {}),
      };
    } catch {
      throw new AppError("unauthorized", "Token inválido o expirado");
    }
  };
}
