import { mkdirSync } from "node:fs";
import { dirname } from "node:path";
import { DatabaseSync } from "node:sqlite";
import { baseXp, isDifficulty, summarize, type Difficulty } from "./xp.ts";
import { localDay } from "./time.ts";

export interface Quest {
  id: string;
  title: string;
  difficulty: Difficulty;
  due_date: string | null;
  recurrence: "daily" | null;
  done: boolean;
}

export interface Completion {
  id: string;
  quest_id: string;
  day: string;
  base_xp: number;
  created_at: string;
}

export interface Me {
  timezone: string;
  xp: number;
  level: number;
  xp_into_level: number;
  xp_to_next_level: number;
  streak: number;
}

interface QuestRow {
  id: string;
  title: string;
  difficulty: string;
  due_date: string | null;
  recurrence: string | null;
  created_at: string;
}

interface CompletionRow {
  id: string;
  quest_id: string;
  day: string;
  base_xp: number;
  created_at: string;
}

export interface NewQuest {
  id: string;
  title: string;
  difficulty: Difficulty;
  dueDate: string | null;
  recurrence: "daily" | null;
  createdAt: string;
}

export class HabitStore {
  private readonly db: DatabaseSync;

  constructor(filename: string) {
    if (filename !== ":memory:") {
      mkdirSync(dirname(filename), { recursive: true });
    }
    this.db = new DatabaseSync(filename);
    this.db.exec(`
      PRAGMA foreign_keys = ON;
      CREATE TABLE IF NOT EXISTS profile (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        timezone TEXT NOT NULL
      );
      INSERT INTO profile (id, timezone)
      SELECT 1, 'Europe/Bucharest'
      WHERE NOT EXISTS (SELECT 1 FROM profile WHERE id = 1);

      CREATE TABLE IF NOT EXISTS quest (
        seq INTEGER PRIMARY KEY AUTOINCREMENT,
        id TEXT NOT NULL UNIQUE,
        title TEXT NOT NULL,
        difficulty TEXT NOT NULL,
        due_date TEXT,
        recurrence TEXT,
        created_at TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS completion (
        seq INTEGER PRIMARY KEY AUTOINCREMENT,
        id TEXT NOT NULL UNIQUE,
        quest_id TEXT NOT NULL,
        day TEXT NOT NULL,
        base_xp INTEGER NOT NULL,
        created_at TEXT NOT NULL,
        UNIQUE (quest_id, day)
      );
    `);
  }

  close(): void {
    this.db.close();
  }

  reset(): void {
    this.db.exec(`
      DELETE FROM completion;
      DELETE FROM quest;
      UPDATE profile SET timezone = 'Europe/Bucharest' WHERE id = 1;
    `);
  }

  timezone(): string {
    const row = this.db.prepare("SELECT timezone FROM profile WHERE id = 1").get() as { timezone: string };
    return row.timezone;
  }

  setTimezone(timezone: string): void {
    this.db.prepare("UPDATE profile SET timezone = ? WHERE id = 1").run(timezone);
  }

  insertQuest(quest: NewQuest): void {
    this.db
      .prepare(
        `INSERT INTO quest (id, title, difficulty, due_date, recurrence, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`,
      )
      .run(quest.id, quest.title, quest.difficulty, quest.dueDate, quest.recurrence, quest.createdAt);
  }

  getQuest(id: string): QuestRow | undefined {
    return this.db.prepare("SELECT id, title, difficulty, due_date, recurrence, created_at FROM quest WHERE id = ?").get(id) as
      | QuestRow
      | undefined;
  }

  listQuests(today: string): Quest[] {
    const rows = this.db
      .prepare(
        "SELECT id, title, difficulty, due_date, recurrence, created_at FROM quest ORDER BY seq DESC",
      )
      .all() as unknown as QuestRow[];
    const completions = this.completions();
    return rows.map((row) => this.toQuest(row, completions, today));
  }

  findActiveCompletion(quest: QuestRow, day: string): Completion | undefined {
    if (quest.recurrence === "daily") {
      return this.completionOn(quest.id, day);
    }
    const row = this.db
      .prepare(
        `SELECT id, quest_id, day, base_xp, created_at
         FROM completion WHERE quest_id = ? ORDER BY seq ASC LIMIT 1`,
      )
      .get(quest.id) as CompletionRow | undefined;
    return row ? toCompletion(row) : undefined;
  }

  insertCompletion(completion: Completion): { completion: Completion; inserted: boolean } {
    this.db.exec("BEGIN IMMEDIATE");
    try {
      this.db
        .prepare(
          `INSERT INTO completion (id, quest_id, day, base_xp, created_at)
           VALUES (?, ?, ?, ?, ?)`,
        )
        .run(completion.id, completion.quest_id, completion.day, completion.base_xp, completion.created_at);
      this.db.exec("COMMIT");
      return { completion, inserted: true };
    } catch (error) {
      try {
        this.db.exec("ROLLBACK");
      } catch (rollbackError) {
        const ended =
          rollbackError instanceof Error && rollbackError.message.toLowerCase().includes("no transaction");
        if (!ended) {
          throw rollbackError;
        }
      }
      if (error instanceof Error && error.message.includes("UNIQUE")) {
        const existing = this.completionOn(completion.quest_id, completion.day);
        if (existing) {
          return { completion: existing, inserted: false };
        }
      }
      throw error;
    }
  }

  listCompletions(): Completion[] {
    const rows = this.db
      .prepare(
        `SELECT id, quest_id, day, base_xp, created_at
         FROM completion ORDER BY created_at DESC, seq DESC`,
      )
      .all() as unknown as CompletionRow[];
    return rows.map(toCompletion);
  }

  deleteCompletion(id: string): boolean {
    const result = this.db.prepare("DELETE FROM completion WHERE id = ?").run(id);
    return Number(result.changes) > 0;
  }

  me(now: Date): Me {
    const timezone = this.timezone();
    const today = localDay(now, timezone);
    const progress = summarize(
      this.completions().map((completion) => ({ day: completion.day, baseXp: completion.base_xp })),
      today,
    );
    return {
      timezone,
      xp: progress.xp,
      level: progress.level,
      xp_into_level: progress.xpIntoLevel,
      xp_to_next_level: progress.xpToNextLevel,
      streak: progress.streak,
    };
  }

  awardBase(quest: QuestRow, day: string): number {
    if (!isDifficulty(quest.difficulty)) {
      throw new Error("stored difficulty is invalid");
    }
    return baseXp(quest.difficulty, quest.due_date, day);
  }

  private completions(): CompletionRow[] {
    return this.db
      .prepare("SELECT id, quest_id, day, base_xp, created_at FROM completion")
      .all() as unknown as CompletionRow[];
  }

  private completionOn(questId: string, day: string): Completion | undefined {
    const row = this.db
      .prepare(
        `SELECT id, quest_id, day, base_xp, created_at
         FROM completion WHERE quest_id = ? AND day = ?`,
      )
      .get(questId, day) as CompletionRow | undefined;
    return row ? toCompletion(row) : undefined;
  }

  private toQuest(row: QuestRow, completions: CompletionRow[], today: string): Quest {
    if (!isDifficulty(row.difficulty)) {
      throw new Error("stored difficulty is invalid");
    }
    const done =
      row.recurrence === "daily"
        ? completions.some((completion) => completion.quest_id === row.id && completion.day === today)
        : completions.some((completion) => completion.quest_id === row.id);
    return {
      id: row.id,
      title: row.title,
      difficulty: row.difficulty,
      due_date: row.due_date,
      recurrence: row.recurrence === "daily" ? "daily" : null,
      done,
    };
  }
}

function toCompletion(row: CompletionRow): Completion {
  return {
    id: row.id,
    quest_id: row.quest_id,
    day: row.day,
    base_xp: row.base_xp,
    created_at: row.created_at,
  };
}
