import assert from "node:assert/strict";
import { test } from "node:test";
import { localDay } from "../src/time.ts";
import { baseXp, summarize } from "../src/xp.ts";

test("late completions earn integer half once, including two days late", () => {
  assert.equal(baseXp("medium", "2026-10-07", "2026-10-07"), 25);
  assert.equal(baseXp("medium", "2026-10-07", "2026-10-08"), 12);
  assert.equal(baseXp("easy", "2026-10-10", "2026-10-12"), 5);
  assert.equal(baseXp("hard", "2026-10-21", "2026-10-21"), 50);
  assert.equal(baseXp("easy", null, "2026-10-18"), 10);
});

test("level widths grow by 100 XP and the streak bonus caps at 14", () => {
  const days = Array.from({ length: 10 }, (_, index) => {
    const day = String(index + 1).padStart(2, "0");
    return { day: `2026-10-${day}`, baseXp: 10 };
  });
  const onTenth = summarize(days, "2026-10-10");
  assert.equal(onTenth.streak, 10);
  assert.equal(onTenth.xp, 10 * 10 + (2 + 4 + 6 + 8 + 10 + 12 + 14 + 14 + 14 + 14));
  assert.deepEqual(
    { level: onTenth.level, into: onTenth.xpIntoLevel, next: onTenth.xpToNextLevel },
    { level: 2, into: onTenth.xp - 100, next: 200 - (onTenth.xp - 100) },
  );
});

test("missing a day resets the streak, and removing a middle day recomputes later bonuses", () => {
  const kept = [
    { day: "2026-10-16", baseXp: 10 },
    { day: "2026-10-18", baseXp: 10 },
  ];
  const withGap = summarize(kept, "2026-10-18");
  assert.equal(withGap.streak, 1);
  assert.equal(withGap.xp, 24);

  const chain = summarize(
    [
      { day: "2026-10-16", baseXp: 10 },
      { day: "2026-10-17", baseXp: 10 },
      { day: "2026-10-18", baseXp: 10 },
    ],
    "2026-10-18",
  );
  assert.equal(chain.streak, 3);
  assert.equal(chain.xp, 42);
  assert.equal(chain.xp - withGap.xp, 18);
});

test("the calendar day follows the user zone, including the autumn clock change", () => {
  assert.equal(localDay(new Date("2026-10-21T22:30:00-04:00"), "America/New_York"), "2026-10-21");
  assert.equal(localDay(new Date("2026-10-25T08:00:00+02:00"), "Europe/Bucharest"), "2026-10-25");
  assert.equal(localDay(new Date("2026-10-05T09:15:00+03:00"), "Europe/Bucharest"), "2026-10-05");
});
