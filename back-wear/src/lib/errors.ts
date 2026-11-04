export type ErrorCode =
  | "invalid_input"
  | "unauthorized"
  | "forbidden"
  | "not_found"
  | "rate_limited"
  | "internal";

export const STATUS_BY_CODE: Record<ErrorCode, 400 | 401 | 403 | 404 | 429 | 500> = {
  invalid_input: 400,
  unauthorized: 401,
  forbidden: 403,
  not_found: 404,
  rate_limited: 429,
  internal: 500,
};

export class AppError extends Error {
  constructor(
    public readonly code: ErrorCode,
    message: string,
  ) {
    super(message);
    this.name = "AppError";
  }
}

export function errorBody(code: ErrorCode, message: string) {
  return { error: { code, message } };
}
