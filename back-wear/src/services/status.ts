import {
  DEFAULT_DURATION_MIN,
  type PutStatusBody,
  type Status,
} from "../contract/index.js";
import { AppError } from "../lib/errors.js";
import type { AuthUser } from "../lib/jwt.js";
import type { StatusRepository } from "../repositories/ports.js";

export interface StatusService {
  setStatus(user: AuthUser, body: PutStatusBody): Promise<Status>;
}

export function createStatusService(deps: {
  repo: StatusRepository;
  now?: () => Date;
}): StatusService {
  const now = deps.now ?? (() => new Date());

  return {
    async setStatus(user, body) {
      const current = await deps.repo.get(user.userId);
      const at = now();

      // Un cambio manual vigente gana sobre cualquier auto_* del bloque en curso.
      const isAuto = body.source === "auto_calendar" || body.source === "auto_motion";
      if (
        isAuto &&
        current?.source === "manual" &&
        current.state !== "free" &&
        current.until !== null &&
        new Date(current.until) > at
      ) {
        return current;
      }

      let until: string | null = null;
      if (body.state !== "free") {
        const minutes = body.durationMin ?? DEFAULT_DURATION_MIN[body.state];
        if (minutes === null || minutes === undefined) {
          throw new AppError("invalid_input", "El estado 'class' requiere durationMin");
        }
        until = new Date(at.getTime() + minutes * 60_000).toISOString();
      }

      return deps.repo.upsert({
        userId: user.userId,
        displayName: user.displayName ?? current?.displayName ?? "Usuario",
        state: body.state,
        until,
        source: body.source,
        updatedAt: at.toISOString(),
      });
    },
  };
}
