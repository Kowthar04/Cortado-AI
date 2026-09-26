import Anthropic from "@anthropic-ai/sdk";
import { config } from "../config";
import { ApiError } from "../middleware/errors";
import { AnthropicMessage } from "./promptBuilder";

let client: Anthropic | undefined;

function getClient(): Anthropic {
  if (!client) {
    client = new Anthropic({ apiKey: config.anthropicApiKey });
  }
  return client;
}

export async function askClaude(
  systemPrompt: string,
  messages: AnthropicMessage[],
): Promise<string> {
  try {
    const response = await getClient().messages.create({
      model: config.claudeModel,
      max_tokens: 1024,
      system: systemPrompt,
      messages,
    });

    const textBlock = response.content.find((block) => block.type === "text");
    if (!textBlock || textBlock.type !== "text") {
      throw ApiError.internal("Claude returned no text response.");
    }
    return textBlock.text;
  } catch (err) {
    if (err instanceof ApiError) {
      throw err;
    }
    const message = err instanceof Error ? err.message : "Unknown error calling Claude API.";
    throw ApiError.internal(`Failed to get a response from Claude: ${message}`);
  }
}
