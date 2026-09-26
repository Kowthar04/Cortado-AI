import { Router, Response } from "express";
import { asyncHandler } from "../middleware/errors";
import { requireAuth, AuthenticatedRequest } from "../middleware/auth";
import { getMenuItems } from "../services/menuService";

export const menuRouter = Router();

menuRouter.get(
  "/",
  requireAuth,
  asyncHandler(async (_req: AuthenticatedRequest, res: Response): Promise<void> => {
    const menuItems = await getMenuItems();
    res.status(200).json({ menuItems });
  }),
);
