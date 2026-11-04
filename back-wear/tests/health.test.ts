import { describe, expect, it } from "vitest";
import pino from "pino";
import { createApp } from "../src/app.js";
import { loadEnv } from "../src/config/env.js";
import { AppError } from "../src/lib/errors.js";

const logger = pino({ level: "silent" });

describe("health", () => {
  const app = createApp({ logger });

  it.each(["/v1/health", "/health"])("GET %s responde ok", async (path) => {
    const res = await app.request(path);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ ok: true });
    expect(res.headers.get("x-request-id")).toBeTruthy();
  });

  it("ruta desconocida devuelve not_found con formato del contrato", async () => {
    const res = await app.request("/v1/nada");
    expect(res.status).toBe(404);
    expect((await res.json()).error.code).toBe("not_found");
  });

  it("AppError se traduce a su código HTTP", async () => {
    const a = createApp({ logger });
    a.get("/boom", () => {
      throw new AppError("forbidden", "no");
    });
    const res = await a.request("/boom");
    expect(res.status).toBe(403);
    expect(await res.json()).toEqual({ error: { code: "forbidden", message: "no" } });
  });

  it("error inesperado devuelve internal sin filtrar detalles", async () => {
    const a = createApp({ logger });
    a.get("/boom", () => {
      throw new Error("secreto");
    });
    const res = await a.request("/boom");
    expect(res.status).toBe(500);
    const body = await res.json();
    expect(body.error.code).toBe("internal");
    expect(JSON.stringify(body)).not.toContain("secreto");
  });
});

describe("loadEnv", () => {
  it("aplica valores por defecto", () => {
    expect(loadEnv({}).PORT).toBe(8787);
  });
  it("rechaza valores inválidos", () => {
    expect(() => loadEnv({ PORT: "abc" })).toThrow(/Configuración inválida/);
  });
});
