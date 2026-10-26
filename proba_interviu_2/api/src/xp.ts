export type Difficulty = "easy" | "medium" | "hard";

const BASE_XP: Record<Difficulty, number> = {
  easy: 10,
  medium: 25,
  hard: 50,
};

export interface DayCompletion {
  day: string;
  baseXp: number;
}

export interface Progress {
  xp: number;
  level: number;
  xpIntoLevel: number;
  xpToNextLevel: number;
  streak: number;
}

export function isDifficulty(value: unknown): value is Difficulty {
  return value === "easy" || value === "medium" || value === "hard";
}

export function baseXp(difficulty: Difficulty, dueDate: string | null, day: string): number {
  const base = BASE_XP[difficulty];
  if (dueDate !== null && day > dueDate) {
    return Math.floor(base / 2);
  }
  return base;
}

export function levelFor(xp: number): Pick<Progress, "level" | "xpIntoLevel" | "xpToNextLevel"> {
  let level = 1;
  for (;;) {
    const start = (100 * (level - 1) * level) / 2;
    const width = level * 100;
    if (xp < start + width) {
      const xpIntoLevel = xp - start;
      return { level, xpIntoLevel, xpToNextLevel: width - xpIntoLevel };
    }
    level += 1;
  }
}

function streakBonus(streak: number): number {
  return Math.min(streak, 7) * 2;
}

function shiftDay(iso: string, days: number): string {
  const [year, month, day] = iso.split("-").map(Number);
  const shifted = new Date(Date.UTC(year, month - 1, day + days));
  return shifted.toISOString().slice(0, 10);
}

export function summarize(completions: DayCompletion[], today: string): Progress {
  const baseByDay = new Map<string, number>();
  for (const completion of completions) {
    baseByDay.set(completion.day, (baseByDay.get(completion.day) ?? 0) + completion.baseXp);
  }

  const days = [...baseByDay.keys()].sort();
  const streakAt = new Map<string, number>();
  let xp = 0;
  let previous: string | null = null;
  for (const day of days) {
    const streak = previous !== null && shiftDay(previous, 1) === day ? streakAt.get(previous)! + 1 : 1;
    streakAt.set(day, streak);
    xp += baseByDay.get(day)! + streakBonus(streak);
    previous = day;
  }

  let streak = 0;
  if (streakAt.has(today)) {
    streak = streakAt.get(today)!;
  } else if (streakAt.has(shiftDay(today, -1))) {
    streak = streakAt.get(shiftDay(today, -1))!;
  }

  return { xp, streak, ...levelFor(xp) };
}
