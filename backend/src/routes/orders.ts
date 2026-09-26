import { Router, Response } from "express";
import { asyncHandler, ApiError } from "../middleware/errors";
import { requireAuth, requireAdmin, AuthenticatedRequest } from "../middleware/auth";
import { getOrderById, listOrders } from "../services/orderService";

export const ordersRouter = Router();

// Admin-only: list all orders.
ordersRouter.get(
  "/",
  requireAuth,
  requireAdmin,
  asyncHandler(async (_req: AuthenticatedRequest, res: Response): Promise<void> => {
    const orders = await listOrders();
    res.status(200).json({ orders });
  }),
);

// Any authenticated user: fetch a single order's status.
ordersRouter.get(
  "/:id",
  requireAuth,
  asyncHandler(async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    const order = await getOrderById(req.params.id);
    if (!order) {
      throw ApiError.notFound(`No order found with id '${req.params.id}'.`);
    }
    res.status(200).json({ order });
  }),
);
