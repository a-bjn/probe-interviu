import type { Completion, Me, Quest } from "./types";

export class ApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/backend${path}`, {
    ...init,
    headers: {
      accept: "application/json",
      ...(init?.body ? { "content-type": "application/json" } : {}),
      ...init?.headers,
    },
  });
  if (response.status === 204) {
    return undefined as T;
  }
  const body = (await response.json()) as T & { error?: string };
  if (!response.ok) {
    throw new ApiError(body.error ?? "Something went wrong");
  }
  return body;
}

export function getMe(): Promise<Me> {
  return request("/me");
}

export function getQuests(): Promise<Quest[]> {
  return request("/quests");
}

export function getCompletions(): Promise<Completion[]> {
  return request("/completions");
}

export function createQuest(payload: {
  title: string;
  difficulty: string;
  due_date?: string;
  recurrence?: "daily";
}): Promise<Quest> {
  return request("/quests", { method: "POST", body: JSON.stringify(payload) });
}

export function completeQuest(id: string): Promise<{ completion: Completion; me: Me }> {
  return request(`/quests/${id}/complete`, { method: "POST" });
}

export function undoCompletion(id: string): Promise<void> {
  return request(`/completions/${id}`, { method: "DELETE" });
}

export function setTimezone(timezone: string): Promise<Me> {
  return request("/me", { method: "PUT", body: JSON.stringify({ timezone }) });
}

export function todayIn(timeZone: string): string {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const read = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? "";
  return `${read("year")}-${read("month")}-${read("day")}`;
}
