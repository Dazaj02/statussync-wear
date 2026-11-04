import { Hono } from "hono";
import { PutStatusBodySchema } from "../contract/index.js";
import { AppError } from "../lib/errors.js";
import type { AuthVariables } from "../middleware/auth.js";
import type { StatusService } from "../services/status.js";

export function statusRoutes(service: StatusService) {
  return new Hono<{ Variables: AuthVariables }>().put("/", async (c) => {
    const raw: unknown = await c.req.json().catch(() => {
      throw new AppError("invalid_input", "El cuerpo debe ser JSON válido");
    });
    const parsed = PutStatusBodySchema.safeParse(raw);
    if (!parsed.success) {
      throw new AppError(
        "invalid_input",
        parsed.error.issues.map((i) => `${i.path.join(".") || "body"}: ${i.message}`).join("; "),
      );
    }
    return c.json(await service.setStatus(c.get("user"), parsed.data));
  });
}
