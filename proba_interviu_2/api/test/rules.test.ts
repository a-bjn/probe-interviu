import assert from "node:assert/strict";
import { test } from "node:test";
import { buildApp } from "../src/app.ts";

async function app() {
  return buildApp(":memory:");
}

test("choices the recording does not show", async () => {
  const api = await app();
  try {
    const longTitle = await api.inject({
      method: "POST",
      url: "/quests",
      headers: { "x-now": "2026-10-05T08:00:00+03:00" },
      payload: { title: "x".repeat(101), difficulty: "easy" },
    });
    assert.equal(longTitle.statusCode, 400);
    assert.deepEqual(longTitle.json(), { error: "invalid title" });

    const weekly = await api.inject({
      method: "POST",
      url: "/quests",
      headers: { "x-now": "2026-10-05T08:00:00+03:00" },
      payload: { title: "Stretch", difficulty: "easy", recurrence: "weekly" },
    });
    assert.equal(weekly.statusCode, 400);
    assert.deepEqual(weekly.json(), { error: "invalid recurrence" });

    const created = await api.inject({
      method: "POST",
      url: "/quests",
      headers: { "x-now": "2026-10-05T08:00:00+03:00" },
      payload: { title: "Write the letter", difficulty: "easy" },
    });
    const questId = created.json().id as string;
    const first = await api.inject({
      method: "POST",
      url: `/quests/${questId}/complete`,
      headers: { "x-now": "2026-10-05T10:00:00+03:00" },
    });
    assert.equal(first.statusCode, 201);
    const again = await api.inject({
      method: "POST",
      url: `/quests/${questId}/complete`,
      headers: { "x-now": "2026-10-09T10:00:00+03:00" },
    });
    assert.equal(again.statusCode, 200);
    assert.equal(again.json().completion.id, first.json().completion.id);
    assert.equal(again.json().me.xp, first.json().me.xp);

    const daily = await api.inject({
      method: "POST",
      url: "/quests",
      headers: { "x-now": "2026-10-05T11:00:00+03:00" },
      payload: { title: "Make the bed", difficulty: "easy", recurrence: "daily" },
    });
    const dailyId = daily.json().id as string;
    await api.inject({
      method: "POST",
      url: `/quests/${dailyId}/complete`,
      headers: { "x-now": "2026-10-05T12:00:00+03:00" },
    });
    const nextMorning = await api.inject({
      method: "GET",
      url: "/quests",
      headers: { "x-now": "2026-10-06T07:00:00+03:00" },
    });
    const bed = nextMorning.json().find((quest: { id: string }) => quest.id === dailyId);
    assert.equal(bed.done, false);

    const reset = await api.inject({
      method: "POST",
      url: "/test/reset",
      headers: { "x-now": "2026-10-06T08:00:00+03:00" },
    });
    assert.equal(reset.statusCode, 204);
    assert.equal(reset.body, "");
    const me = await api.inject({
      method: "GET",
      url: "/me",
      headers: { "x-now": "2026-10-06T08:00:00+03:00" },
    });
    assert.deepEqual(me.json(), {
      timezone: "Europe/Bucharest",
      xp: 0,
      level: 1,
      xp_into_level: 0,
      xp_to_next_level: 100,
      streak: 0,
    });
  } finally {
    await api.close();
  }
});
