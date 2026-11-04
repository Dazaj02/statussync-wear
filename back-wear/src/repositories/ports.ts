import type { Status } from "../contract/index.js";

export interface StatusRepository {
  get(userId: string): Promise<Status | null>;
  upsert(status: Status): Promise<Status>;
}
