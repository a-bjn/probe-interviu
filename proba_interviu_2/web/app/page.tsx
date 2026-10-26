"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  completeQuest,
  createQuest,
  getCompletions,
  getMe,
  getQuests,
  setTimezone,
  todayIn,
  undoCompletion,
} from "../lib/api";
import type { Completion, Difficulty, Me, Quest } from "../lib/types";

const ZONES = ["Europe/Bucharest", "Europe/London", "America/New_York", "America/Los_Angeles", "Asia/Tokyo", "UTC"];
const XP: Record<Difficulty, number> = { easy: 10, medium: 25, hard: 50 };

type Schedule = "daily" | "due" | "open";

export default function HomePage() {
  const [me, setMe] = useState<Me | null>(null);
  const [quests, setQuests] = useState<Quest[]>([]);
  const [completions, setCompletions] = useState<Completion[]>([]);
  const [error, setError] = useState("");
  const [title, setTitle] = useState("");
  const [difficulty, setDifficulty] = useState<Difficulty>("easy");
  const [schedule, setSchedule] = useState<Schedule>("daily");
  const [dueDate, setDueDate] = useState("");
  const [busy, setBusy] = useState("");
  const [levelUp, setLevelUp] = useState<number | null>(null);
  const titleRef = useRef<HTMLInputElement>(null);
  const errorRef = useRef<HTMLParagraphElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);

  async function load() {
    const [nextMe, nextQuests, nextCompletions] = await Promise.all([getMe(), getQuests(), getCompletions()]);
    setMe(nextMe);
    setQuests(nextQuests);
    setCompletions(nextCompletions);
    return nextMe;
  }

  useEffect(() => {
    load().catch((cause: Error) => showError(cause.message));
  }, []);

  useEffect(() => {
    if (!title.trim() && !dueDate) return;
    const onLeave = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    window.addEventListener("beforeunload", onLeave);
    return () => window.removeEventListener("beforeunload", onLeave);
  }, [title, dueDate]);

  useEffect(() => {
    if (levelUp === null) return;
    const previous = document.activeElement;
    dialogRef.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setLevelUp(null);
    };
    window.addEventListener("keydown", onKey);
    const timer = window.setTimeout(() => setLevelUp(null), 3200);
    return () => {
      window.removeEventListener("keydown", onKey);
      window.clearTimeout(timer);
      if (previous instanceof HTMLElement) previous.focus();
    };
  }, [levelUp]);

  const today = me ? todayIn(me.timezone) : "";
  const width = me ? me.xp_into_level + me.xp_to_next_level : 100;
  const fraction = me && width > 0 ? me.xp_into_level / width : 0;
  const zones = useMemo(() => (me && !ZONES.includes(me.timezone) ? [me.timezone, ...ZONES] : ZONES), [me]);

  function showError(message: string) {
    const text = explain(message);
    setError(text);
    window.setTimeout(() => {
      if (text.toLowerCase().includes("title")) titleRef.current?.focus();
      else errorRef.current?.focus();
    }, 0);
  }

  function applyMe(next: Me) {
    setMe((current) => {
      if (current && next.level > current.level) setLevelUp(next.level);
      return next;
    });
  }

  async function onCreate(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    setBusy("create");
    try {
      const payload: { title: string; difficulty: Difficulty; due_date?: string; recurrence?: "daily" } = {
        title,
        difficulty,
      };
      if (schedule === "daily") payload.recurrence = "daily";
      if (schedule === "due") payload.due_date = dueDate;
      await createQuest(payload);
      setTitle("");
      setDueDate("");
      await load();
      titleRef.current?.focus();
    } catch (cause) {
      showError(cause instanceof Error ? cause.message : "Could not create the quest");
    } finally {
      setBusy("");
    }
  }

  async function onComplete(quest: Quest) {
    setError("");
    setBusy(quest.id);
    try {
      const result = await completeQuest(quest.id);
      applyMe(result.me);
      const [nextQuests, nextCompletions] = await Promise.all([getQuests(), getCompletions()]);
      setQuests(nextQuests);
      setCompletions(nextCompletions);
    } catch (cause) {
      showError(cause instanceof Error ? cause.message : "Could not complete the quest");
      await load().catch(() => undefined);
    } finally {
      setBusy("");
    }
  }

  async function onUndo(id: string) {
    setError("");
    setBusy(id);
    try {
      await undoCompletion(id);
      await load();
    } catch (cause) {
      showError(cause instanceof Error ? cause.message : "Could not undo that completion. Try again.");
    } finally {
      setBusy("");
    }
  }

  async function onZone(timezone: string) {
    setError("");
    try {
      applyMe(await setTimezone(timezone));
      setQuests(await getQuests());
    } catch (cause) {
      showError(cause instanceof Error ? cause.message : "Could not change the time zone. Try again.");
    }
  }

  return (
    <>
      <a className="skip" href="#content">
        Skip to Content
      </a>
      <main className="app" id="content">
        <header className="masthead">
          <div>
            <p className="eyebrow">Habit Quests</p>
            <h1>Show Up</h1>
            <p className="today">{me ? longDate(today) : "Loading…"}</p>
          </div>
          <div className="streak">
            <b>{me?.streak ?? 0}</b>
            <span>Day Streak</span>
          </div>
        </header>

        <section className="hero" aria-label="Progress">
          <div className="level-row">
            <strong>Level {me?.level ?? 1}</strong>
            <span>{me ? `${me.xp_into_level} / ${width} XP` : "…"}</span>
          </div>
          <div
            className="bar"
            role="meter"
            aria-label="Experience toward the next level"
            aria-valuemin={0}
            aria-valuemax={width}
            aria-valuenow={me?.xp_into_level ?? 0}
            aria-valuetext={me ? `${me.xp_into_level} of ${width} experience points` : "Loading"}
          >
            <span aria-hidden="true" style={{ transform: `scaleX(${fraction})` }} />
          </div>
          <p className="bar-caption" aria-live="polite">
            {me ? `${me.xp_to_next_level} XP to Level ${me.level + 1}. ${me.xp} XP total.` : "Waiting…"}
          </p>
        </section>

        {error ? (
          <p className="banner" role="alert" tabIndex={-1} ref={errorRef}>
            {error}
          </p>
        ) : null}

        <section className="panel">
          <h2>New Quest</h2>
          <form className="composer" onSubmit={onCreate} aria-busy={busy === "create"}>
            <label>
              Title
              <input
                ref={titleRef}
                name="title"
                autoComplete="off"
                value={title}
                maxLength={100}
                required
                onChange={(event) => setTitle(event.target.value)}
                placeholder="Drink 2 litres of water…"
              />
            </label>
            <div className="choices" role="group" aria-label="Difficulty">
              {(["easy", "medium", "hard"] as Difficulty[]).map((option) => (
                <button key={option} type="button" aria-pressed={difficulty === option} onClick={() => setDifficulty(option)}>
                  {labelCase(option)} · {XP[option]} XP
                </button>
              ))}
            </div>
            <div className="schedule" role="group" aria-label="Schedule">
              <button type="button" aria-pressed={schedule === "daily"} onClick={() => setSchedule("daily")}>
                Daily
              </button>
              <button type="button" aria-pressed={schedule === "due"} onClick={() => setSchedule("due")}>
                Due Date
              </button>
              <button type="button" aria-pressed={schedule === "open"} onClick={() => setSchedule("open")}>
                Whenever
              </button>
            </div>
            {schedule === "due" ? (
              <label>
                Due Date
                <input
                  type="date"
                  name="due_date"
                  autoComplete="off"
                  required
                  value={dueDate}
                  onChange={(event) => setDueDate(event.target.value)}
                />
              </label>
            ) : null}
            <button className="primary" type="submit" disabled={busy === "create"}>
              {busy === "create" ? "Adding…" : "Add Quest"}
            </button>
          </form>
        </section>

        <section className="panel">
          <h2>Quests</h2>
          {quests.length === 0 ? <p className="empty">Nothing here yet. Add one small thing you can finish.</p> : null}
          {quests.map((quest) => {
            const overdue = Boolean(quest.due_date && !quest.recurrence && !quest.done && today && quest.due_date < today);
            return (
              <article key={quest.id} className={quest.done ? "quest done" : "quest"}>
                <div>
                  <h3>{quest.title}</h3>
                  <div className="meta">
                    <span className="pill">{labelCase(quest.difficulty)}</span>
                    <span className="pill">
                      {quest.recurrence === "daily" ? "Daily" : quest.due_date ? `Due ${quest.due_date}` : "One-off"}
                    </span>
                    {quest.done ? <span className="pill done">Done</span> : null}
                    {overdue ? <span className="pill overdue">Overdue</span> : null}
                  </div>
                </div>
                <button
                  className="ghost"
                  type="button"
                  disabled={quest.done || busy === quest.id}
                  aria-label={quest.done ? `${quest.title} is done` : `Complete ${quest.title}`}
                  onClick={() => onComplete(quest)}
                >
                  {quest.done ? "Done" : busy === quest.id ? "Saving…" : "Complete"}
                </button>
              </article>
            );
          })}
        </section>

        <section className="panel">
          <h2>Recent Completions</h2>
          {completions.length === 0 ? <p className="empty">Completed quests land here. You can undo any of them.</p> : null}
          {completions.map((completion) => {
            const quest = quests.find((item) => item.id === completion.quest_id);
            const name = quest?.title ?? "Quest";
            return (
              <article key={completion.id} className="completion">
                <div>
                  <strong>{name}</strong>
                  <div className="meta">
                    <span className="pill">{completion.day}</span>
                    <span className="pill">{completion.base_xp} XP</span>
                  </div>
                </div>
                <button
                  className="undo"
                  type="button"
                  disabled={busy === completion.id}
                  aria-label={`Undo ${name}`}
                  onClick={() => onUndo(completion.id)}
                >
                  {busy === completion.id ? "Undoing…" : "Undo"}
                </button>
              </article>
            );
          })}
        </section>

        <section className="panel zone">
          <label>
            Time Zone
            <select
              name="timezone"
              autoComplete="off"
              value={me?.timezone ?? "Europe/Bucharest"}
              onChange={(event) => onZone(event.target.value)}
            >
              {zones.map((zone) => (
                <option key={zone} value={zone}>
                  {zone}
                </option>
              ))}
            </select>
          </label>
        </section>

        {levelUp !== null ? (
          <div className="overlay" role="presentation" onClick={() => setLevelUp(null)}>
            <div
              role="dialog"
              aria-modal="true"
              aria-labelledby="level-up-title"
              tabIndex={-1}
              ref={dialogRef}
              onClick={(event) => event.stopPropagation()}
            >
              <p id="level-up-title">Level Up</p>
              <strong>{levelUp}</strong>
              <button type="button" onClick={() => setLevelUp(null)}>
                Keep Going
              </button>
            </div>
          </div>
        ) : null}
      </main>
    </>
  );
}

function labelCase(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function explain(message: string): string {
  switch (message) {
    case "invalid title":
      return "Enter a title between 1 and 100 characters.";
    case "invalid difficulty":
      return "Choose Easy, Medium, or Hard.";
    case "a quest cannot have both due_date and recurrence":
      return "Choose either a due date or a daily repeat.";
    case "invalid recurrence":
      return "Daily is the only repeat. Pick Daily, or clear it.";
    case "invalid due_date":
      return "Enter a real date.";
    case "invalid timezone":
      return "Choose a time zone from the list.";
    case "quest not found":
      return "That quest is gone. Refresh the page.";
    case "completion not found":
      return "That completion is already undone.";
    default:
      return message.endsWith(".") ? message : `${message}. Try again.`;
  }
}

function longDate(iso: string): string {
  const [year, month, day] = iso.split("-").map(Number);
  return new Intl.DateTimeFormat("en-GB", {
    timeZone: "UTC",
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(new Date(Date.UTC(year, month - 1, day)));
}
