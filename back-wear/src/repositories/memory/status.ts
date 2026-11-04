import type { Status } from "../../contract/index.js";
import type { StatusRepository } from "../ports.js";

export class MemoryStatusRepository implements StatusRepository {
  private readonly rows = new Map<string, Status>();

  async get(userId: string) {
    return this.rows.get(userId) ?? null;
  }

  async upsert(status: Status) {
    this.rows.set(status.userId, status);
    return status;
  }
}
