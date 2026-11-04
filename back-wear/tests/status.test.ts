import { describe, expect, it } from "vitest";
import { SignJWT } from "jose";
import { makeEnv } from "./helpers.js";

describe("auth", () => {
  it("rechaza sin token", async () => {
    const { put } = await makeEnv();
    const res = await put(null, { state: "focus", source: "manual" });
    expect(res.status).toBe(401);
    expect((await res.json()).error.code).toBe("unauthorized");
  });

  it("rechaza token con firma inválida", async () => {
    const { put } = await makeEnv();
    const bad = await new SignJWT({})
      .setProtectedHeader({ alg: "HS256" })
      .setSubject("u1")
      .setExpirationTime("1h")
      .sign(new TextEncoder().encode("otro-secreto-otro-secreto-otro-secreto"));
    expect((await put(bad, { state: "focus", source: "manual" })).status).toBe(401);
  });

  it("rechaza token expirado", async () => {
    const { put, hsToken } = await makeEnv();
    const expired = await hsToken("u1", {}, Math.floor(Date.now() / 1000) - 60);
    expect((await put(expired, { state: "focus", source: "manual" })).status).toBe(401);
  });

  it("rechaza basura que no es JWT", async () => {
    const { put } = await makeEnv();
    expect((await put("no-es-jwt", { state: "focus", source: "manual" })).status).toBe(401);
  });

  it("acepta HS256", async () => {
    const { put, hsToken } = await makeEnv();
    const res = await put(await hsToken("u1"), { state: "focus", source: "manual" });
    expect(res.status).toBe(200);
  });

  it("acepta clave asimétrica vía JWKS", async () => {
    const { put, esToken } = await makeEnv();
    const res = await put(await esToken("u2"), { state: "call", source: "manual" });
    expect(res.status).toBe(200);
    expect((await res.json()).userId).toBe("u2");
  });
});

describe("PUT /v1/status", () => {
  it("usa la duración por defecto del contrato y calcula until", async () => {
    const { put, hsToken } = await makeEnv();
    const t = await hsToken("u1", { user_metadata: { full_name: "Ana" } });
    const focus = await (await put(t, { state: "focus", source: "manual" })).json();
    expect(focus).toMatchObject({
      userId: "u1",
      displayName: "Ana",
      state: "focus",
      source: "manual",
      until: "2026-01-01T10:50:00.000Z",
      updatedAt: "2026-01-01T10:00:00.000Z",
    });
    const call = await (await put(t, { state: "call", source: "manual" })).json();
    expect(call.until).toBe("2026-01-01T10:30:00.000Z");
  });

  it("respeta durationMin", async () => {
    const { put, hsToken } = await makeEnv();
    const res = await (await put(await hsToken("u1"), { state: "focus", durationMin: 10, source: "manual" })).json();
    expect(res.until).toBe("2026-01-01T10:10:00.000Z");
  });

  it("free no lleva until", async () => {
    const { put, hsToken } = await makeEnv();
    const res = await (await put(await hsToken("u1"), { state: "free", source: "manual" })).json();
    expect(res.until).toBeNull();
  });

  it("class exige durationMin", async () => {
    const { put, hsToken } = await makeEnv();
    const t = await hsToken("u1");
    const bad = await put(t, { state: "class", source: "auto_calendar" });
    expect(bad.status).toBe(400);
    const ok = await (await put(t, { state: "class", durationMin: 90, source: "auto_calendar" })).json();
    expect(ok.until).toBe("2026-01-01T11:30:00.000Z");
  });

  it.each([
    ["estado desconocido", { state: "sleep", source: "manual" }],
    ["source desconocido", { state: "focus", source: "bot" }],
    ["duración negativa", { state: "focus", durationMin: -5, source: "manual" }],
    ["sin source", { state: "focus" }],
  ])("invalid_input: %s", async (_n, body) => {
    const { put, hsToken } = await makeEnv();
    const res = await put(await hsToken("u1"), body);
    expect(res.status).toBe(400);
    expect((await res.json()).error.code).toBe("invalid_input");
  });

  it("invalid_input con JSON roto", async () => {
    const { put, hsToken } = await makeEnv();
    const res = await put(await hsToken("u1"), "{no json");
    expect(res.status).toBe(400);
  });
});

describe("precedencia manual sobre auto", () => {
  it("auto_* no pisa un manual vigente", async () => {
    const { put, hsToken } = await makeEnv();
    const t = await hsToken("u1");
    await put(t, { state: "focus", source: "manual" });
    const res = await (await put(t, { state: "class", durationMin: 60, source: "auto_calendar" })).json();
    expect(res.state).toBe("focus");
    expect(res.source).toBe("manual");
    const motion = await (await put(t, { state: "free", source: "auto_motion" })).json();
    expect(motion.state).toBe("focus");
  });

  it("auto_* sí aplica cuando el manual ya venció", async () => {
    const clock = { now: new Date("2026-01-01T10:00:00Z") };
    const { put, hsToken } = await makeEnv(clock);
    const t = await hsToken("u1");
    await put(t, { state: "focus", source: "manual" });
    clock.now = new Date("2026-01-01T11:00:00Z");
    const res = await (await put(t, { state: "class", durationMin: 60, source: "auto_calendar" })).json();
    expect(res.state).toBe("class");
    expect(res.source).toBe("auto_calendar");
  });

  it("un manual siempre gana a un auto previo", async () => {
    const { put, hsToken } = await makeEnv();
    const t = await hsToken("u1");
    await put(t, { state: "class", durationMin: 60, source: "auto_calendar" });
    const res = await (await put(t, { state: "free", source: "manual" })).json();
    expect(res).toMatchObject({ state: "free", source: "manual" });
  });

  it("los usuarios no se afectan entre sí", async () => {
    const { put, hsToken } = await makeEnv();
    await put(await hsToken("u1"), { state: "focus", source: "manual" });
    const res = await (await put(await hsToken("u2"), { state: "class", durationMin: 30, source: "auto_calendar" })).json();
    expect(res.state).toBe("class");
  });
});
