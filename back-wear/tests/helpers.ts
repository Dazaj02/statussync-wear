import pino from "pino";
import { SignJWT, exportJWK, generateKeyPair, createLocalJWKSet } from "jose";
import { createApp } from "../src/app.js";
import { createTokenVerifier } from "../src/lib/jwt.js";
import { MemoryStatusRepository } from "../src/repositories/memory/status.js";
import { createStatusService } from "../src/services/status.js";

export const SECRET = "test-secret-test-secret-test-secret-123";
export const logger = pino({ level: "silent" });

export async function makeEnv(clock: { now: Date } = { now: new Date("2026-01-01T10:00:00Z") }) {
  const { publicKey, privateKey } = await generateKeyPair("ES256");
  const jwk = { ...(await exportJWK(publicKey)), kid: "k1", alg: "ES256", use: "sig" };
  const verifyToken = createTokenVerifier({
    secret: SECRET,
    jwks: createLocalJWKSet({ keys: [jwk] }),
  });
  const repo = new MemoryStatusRepository();
  const statusService = createStatusService({ repo, now: () => clock.now });
  const app = createApp({ logger, verifyToken, statusService });

  const hsToken = (sub: string, extra: Record<string, unknown> = {}, exp: string | number = "1h") =>
    new SignJWT(extra)
      .setProtectedHeader({ alg: "HS256" })
      .setSubject(sub)
      .setExpirationTime(exp)
      .sign(new TextEncoder().encode(SECRET));
  const esToken = (sub: string) =>
    new SignJWT({})
      .setProtectedHeader({ alg: "ES256", kid: "k1" })
      .setSubject(sub)
      .setExpirationTime("1h")
      .sign(privateKey);
  const put = (token: string | null, body: unknown) =>
    app.request("/v1/status", {
      method: "PUT",
      headers: {
        "content-type": "application/json",
        ...(token ? { authorization: `Bearer ${token}` } : {}),
      },
      body: typeof body === "string" ? body : JSON.stringify(body),
    });
  return { app, repo, clock, hsToken, esToken, put };
}
