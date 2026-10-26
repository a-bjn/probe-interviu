import { randomUUID } from "node:crypto";
import Fastify, { type FastifyInstance, type FastifyReply, type FastifyRequest } from "fastify";
import { HabitStore } from "./store.ts";
import { isIsoDate, isValidTimeZone, localDay, parseNow, utcSeconds } from "./time.ts";
import { isDifficulty } from "./xp.ts";

declare module "fastify" {
  interface FastifyRequest {
    now: Date;
  }
}

interface QuestBody {
  title?: unknown;
  difficulty?: unknown;
  due_date?: unknown;
  recurrence?: unknown;
}

function nowFrom(request: FastifyRequest): Date {
  return request.now;
}

function sendError(reply: FastifyReply, status: number, error: string) {
  return reply.code(status).send({ error });
}

export function buildApp(dbPath: string): FastifyInstance {
  const app = Fastify({ logger: false });
  const store = new HabitStore(dbPath);

  app.addHook("onClose", async () => {
    store.close();
  });

  app.addHook("onRequest", async (request, reply) => {
    const header = request.headers["x-now"];
    const raw = Array.isArray(header) ? header[0] : header;
    if (raw === undefined || raw.trim() === "") {
      request.now = new Date();
      return;
    }
    const parsed = parseNow(raw);
    if (!parsed) {
      return sendError(reply, 400, "invalid timestamp");
    }
    request.now = parsed;
  });

  app.setNotFoundHandler((request, reply) => {
    return sendError(reply, 404, "not found");
  });

  app.setErrorHandler((error, request, reply) => {
    const code = "code" in error ? String(error.code) : "";
    if (code === "FST_ERR_CTP_INVALID_JSON" || code === "FST_ERR_CTP_INVALID_MEDIA_TYPE") {
      return sendError(reply, 400, "invalid body");
    }
    request.log.error(error);
    return sendError(reply, 500, "internal error");
  });

  app.get("/me", async (request) => store.me(nowFrom(request)));

  app.put("/me", async (request, reply) => {
    const body = request.body as { timezone?: unknown } | undefined;
    if (!isValidTimeZone(body?.timezone)) {
      return sendError(reply, 400, "invalid timezone");
    }
    store.setTimezone(body.timezone);
    return store.me(nowFrom(request));
  });

  app.post("/quests", async (request, reply) => {
    const body = request.body as QuestBody | undefined;
    if (!body || typeof body !== "object" || Array.isArray(body)) {
      return sendError(reply, 400, "invalid body");
    }
    if (typeof body.title !== "string" || body.title.trim().length < 1 || body.title.trim().length > 100) {
      return sendError(reply, 400, "invalid title");
    }
    if (!isDifficulty(body.difficulty)) {
      return sendError(reply, 400, "invalid difficulty");
    }
    const hasDue = body.due_date !== undefined && body.due_date !== null;
    const hasRecurrence = body.recurrence !== undefined && body.recurrence !== null;
    if (hasDue && hasRecurrence) {
      return sendError(reply, 400, "a quest cannot have both due_date and recurrence");
    }
    if (hasRecurrence && body.recurrence !== "daily") {
      return sendError(reply, 400, "invalid recurrence");
    }
    if (hasDue && !isIsoDate(body.due_date)) {
      return sendError(reply, 400, "invalid due_date");
    }

    const now = nowFrom(request);
    const id = randomUUID();
    store.insertQuest({
      id,
      title: body.title.trim(),
      difficulty: body.difficulty,
      dueDate: hasDue ? body.due_date : null,
      recurrence: hasRecurrence ? "daily" : null,
      createdAt: utcSeconds(now),
    });
    const quest = store.listQuests(localDay(now, store.timezone())).find((item) => item.id === id);
    return reply.code(201).send(quest);
  });

  app.get("/quests", async (request) => {
    const now = nowFrom(request);
    return store.listQuests(localDay(now, store.timezone()));
  });

  app.post("/quests/:id/complete", async (request, reply) => {
    const { id } = request.params as { id: string };
    const quest = store.getQuest(id);
    if (!quest) {
      return sendError(reply, 404, "quest not found");
    }
    const now = nowFrom(request);
    const day = localDay(now, store.timezone());
    const existing = store.findActiveCompletion(quest, day);
    if (existing) {
      return reply.code(200).send({ completion: existing, me: store.me(now) });
    }
    const saved = store.insertCompletion({
      id: randomUUID(),
      quest_id: quest.id,
      day,
      base_xp: store.awardBase(quest, day),
      created_at: utcSeconds(now),
    });
    return reply.code(saved.inserted ? 201 : 200).send({ completion: saved.completion, me: store.me(now) });
  });

  app.get("/completions", async () => store.listCompletions());

  app.delete("/completions/:id", async (request, reply) => {
    const { id } = request.params as { id: string };
    if (!store.deleteCompletion(id)) {
      return sendError(reply, 404, "completion not found");
    }
    return reply.code(204).send();
  });

  app.post("/test/reset", async (request, reply) => {
    store.reset();
    return reply.code(204).send();
  });

  return app;
}
