import express, { Express } from "express";
import cors from "cors";
import path from "path";
import { chatRouter } from "./routes/chat";
import { menuRouter } from "./routes/menu";
import { ordersRouter } from "./routes/orders";
import { errorHandler, notFoundHandler } from "./middleware/errors";

export function createApp(): Express {
  const app = express();

  app.use(cors());
  app.use(express.json());

  app.get("/health", (_req, res) => {
    res.status(200).json({ status: "ok" });
  });

  app.use("/api/chat", chatRouter);
  app.use("/api/menu", menuRouter);
  app.use("/api/orders", ordersRouter);

  app.use("/admin", express.static(path.join(__dirname, "public")));

  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}
