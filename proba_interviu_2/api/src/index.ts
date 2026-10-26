import { fileURLToPath } from "node:url";
import { buildApp } from "./app.ts";

const dbPath = process.env.HABIT_DB ?? fileURLToPath(new URL("../data/habit.db", import.meta.url));
const port = Number(process.env.PORT ?? 3001);
const host = process.env.HOST ?? "0.0.0.0";

const app = buildApp(dbPath);

const shutdown = async () => {
  await app.close();
};

process.on("SIGINT", () => {
  void shutdown();
});
process.on("SIGTERM", () => {
  void shutdown();
});

await app.listen({ port, host });
