import { Router, Response } from "express";
import { asyncHandler, ApiError } from "../middleware/errors";
import { requireAuth, AuthenticatedRequest } from "../middleware/auth";
import { getMenuItems } from "../services/menuService";
import { getOrderById, getOrdersByUserId } from "../services/orderService";
import { buildMessages, buildSystemPrompt } from "../services/promptBuilder";
import { askClaude } from "../services/claudeService";
import { ChatRequestBody, ChatResponseBody } from "../types/models";
import { config } from "../config";

export const chatRouter = Router();

chatRouter.post(
  "/",
  requireAuth,
  asyncHandler(async (req: AuthenticatedRequest, res: Response): Promise<void> => {
    const body = req.body as Partial<ChatRequestBody>;

    if (!body.message || typeof body.message !== "string" || body.message.trim().length === 0) {
      throw ApiError.badRequest("Request body must include a non-empty 'message' string.");
    }

    const menuItems = await getMenuItems();

    const orderHistory = body.userId
      ? await getOrdersByUserId(body.userId, config.chatOrderHistoryLimit)
      : undefined;

    const currentOrder = body.orderId ? await getOrderById(body.orderId) : undefined;

    const systemPrompt = buildSystemPrompt({ menuItems, orderHistory, currentOrder });
    const messages = buildMessages(body.conversationHistory, body.message);

    const reply = await askClaude(systemPrompt, messages);

    const response: ChatResponseBody = { reply };
    res.status(200).json(response);
  }),
);
