import { NextFunction, Request, Response } from "express";
import { ApiErrorBody } from "../types/models";

export class ApiError extends Error {
  public readonly statusCode: number;
  public readonly code: string;

  constructor(statusCode: number, code: string, message: string) {
    super(message);
    this.statusCode = statusCode;
    this.code = code;
    this.name = "ApiError";
  }

  static badRequest(message: string): ApiError {
    return new ApiError(400, "BAD_REQUEST", message);
  }

  static unauthorized(message = "Missing or invalid authentication token."): ApiError {
    return new ApiError(401, "UNAUTHORIZED", message);
  }

  static forbidden(message = "You do not have permission to perform this action."): ApiError {
    return new ApiError(403, "FORBIDDEN", message);
  }

  static notFound(message: string): ApiError {
    return new ApiError(404, "NOT_FOUND", message);
  }

  static internal(message = "An unexpected error occurred."): ApiError {
    return new ApiError(500, "INTERNAL_ERROR", message);
  }
}

/**
 * Express error-handling middleware (4-arg signature is required by Express
 * to be recognized as an error handler). Produces a consistent JSON shape:
 * { error: { message, code } }.
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function errorHandler(
  err: unknown,
  _req: Request,
  res: Response,
  _next: NextFunction,
): void {
  if (err instanceof ApiError) {
    const body: ApiErrorBody = { error: { message: err.message, code: err.code } };
    res.status(err.statusCode).json(body);
    return;
  }

  const message = err instanceof Error ? err.message : "An unexpected error occurred.";
  // eslint-disable-next-line no-console
  console.error("[unhandled error]", err);
  const body: ApiErrorBody = { error: { message, code: "INTERNAL_ERROR" } };
  res.status(500).json(body);
}

export function notFoundHandler(req: Request, res: Response): void {
  const body: ApiErrorBody = {
    error: { message: `No route found for ${req.method} ${req.path}`, code: "NOT_FOUND" },
  };
  res.status(404).json(body);
}

/** Wraps an async route handler so rejected promises reach errorHandler. */
export function asyncHandler(
  fn: (req: Request, res: Response, next: NextFunction) => Promise<void>,
) {
  return (req: Request, res: Response, next: NextFunction): Promise<void> => {
    return fn(req, res, next).catch(next);
  };
}
