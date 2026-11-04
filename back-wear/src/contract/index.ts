import { z } from "zod";

export const STATES = ["free", "focus", "class", "call"] as const;
export const SOURCES = ["manual", "timer", "auto_calendar", "auto_motion"] as const;

export const StateSchema = z.enum(STATES);
export const SourceSchema = z.enum(SOURCES);
export type State = z.infer<typeof StateSchema>;
export type Source = z.infer<typeof SourceSchema>;

export const DEFAULT_DURATION_MIN: Record<State, number | null> = {
  free: null,
  focus: 50,
  class: null, // bloque de agenda: exige durationMin
  call: 30,
};

export const PutStatusBodySchema = z.object({
  state: StateSchema,
  durationMin: z.number().int().positive().max(24 * 60).optional(),
  source: SourceSchema,
});
export type PutStatusBody = z.infer<typeof PutStatusBodySchema>;

export interface Status {
  userId: string;
  displayName: string;
  state: State;
  until: string | null;
  source: Source;
  updatedAt: string;
}
