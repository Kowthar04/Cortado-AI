import { GoogleGenerativeAI } from "@google/generative-ai";
import { config } from "../config";
import { ApiError } from "../middleware/errors";
import { ChatMessage } from "./promptBuilder";

let client: GoogleGenerativeAI | undefined;

function getClient(): GoogleGenerativeAI {
  if (!client) {
    client = new GoogleGenerativeAI(config.geminiApiKey);
  }
  return client;
}

/** Gemini's chat history uses "model" where our internal shape uses "assistant". */
function toGeminiRole(role: ChatMessage["role"]): "user" | "model" {
  return role === "assistant" ? "model" : "user";
}

export async function askGemini(systemPrompt: string, messages: ChatMessage[]): Promise<string> {
  const lastMessage = messages[messages.length - 1];
  if (!lastMessage) {
    throw ApiError.internal("No message provided to send to Gemini.");
  }

  try {
    const model = getClient().getGenerativeModel({
      model: config.geminiModel,
      systemInstruction: systemPrompt,
    });

    const history = messages.slice(0, -1).map((turn) => ({
      role: toGeminiRole(turn.role),
      parts: [{ text: turn.content }],
    }));

    const chat = model.startChat({ history });
    const result = await chat.sendMessage(lastMessage.content);
    const text = result.response.text();

    if (!text) {
      throw ApiError.internal("Gemini returned no text response.");
    }
    return text;
  } catch (err) {
    if (err instanceof ApiError) {
      throw err;
    }
    const message = err instanceof Error ? err.message : "Unknown error calling Gemini API.";
    throw ApiError.internal(`Failed to get a response from Gemini: ${message}`);
  }
}
