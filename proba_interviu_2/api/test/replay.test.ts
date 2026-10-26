import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { buildApp } from "../src/app.ts";

interface RecordingRow {
  x_now: string;
  method: string;
  path: string;
  body?: unknown;
  status: number;
  response?: unknown;
}

test("replays reference_recording.jsonl", async () => {
  const app = buildApp(":memory:");
  const recordingPath = fileURLToPath(new URL("../../reference_recording.jsonl", import.meta.url));
  const rows = readFileSync(recordingPath, "utf8")
    .trim()
    .split(/\r?\n/)
    .map((line) => JSON.parse(line) as RecordingRow);
  const questIds = new Map<string, string>();
  const completionIds = new Map<string, string>();

  try {
    assert.equal(rows.length, 68);
    for (const [index, row] of rows.entries()) {
      const line = index + 1;
      const response = await app.inject({
        method: row.method as "GET",
        url: rewritePath(row.path, questIds, completionIds),
        headers: { "x-now": row.x_now },
        payload: row.body,
      });
      assert.equal(
        response.statusCode,
        row.status,
        `line ${line} ${row.method} ${row.path} status ${response.statusCode}: ${response.body}`,
      );
      if (row.response === undefined) {
        assert.equal(response.body, "", `line ${line} should have an empty body`);
        continue;
      }
      const actual = response.json();
      rememberIds(row, actual, questIds, completionIds);
      const normalized = renameIds(actual, invert(questIds), invert(completionIds));
      assert.deepEqual(normalized, row.response, `line ${line} body mismatch`);
    }
  } finally {
    await app.close();
  }
});

function rewritePath(path: string, questIds: Map<string, string>, completionIds: Map<string, string>): string {
  const complete = /^\/quests\/([^/]+)\/complete$/.exec(path);
  if (complete) {
    return `/quests/${questIds.get(complete[1]) ?? complete[1]}/complete`;
  }
  const completion = /^\/completions\/([^/]+)$/.exec(path);
  if (completion) {
    return `/completions/${completionIds.get(completion[1]) ?? completion[1]}`;
  }
  return path;
}

function rememberIds(
  row: RecordingRow,
  actual: unknown,
  questIds: Map<string, string>,
  completionIds: Map<string, string>,
): void {
  if (row.status === 201 && row.path === "/quests" && isRecord(row.response) && isRecord(actual)) {
    questIds.set(String(row.response.id), String(actual.id));
  }
  if (row.status === 201 && row.path.endsWith("/complete") && isRecord(row.response) && isRecord(actual)) {
    const expected = row.response.completion;
    const received = actual.completion;
    if (isRecord(expected) && isRecord(received)) {
      completionIds.set(String(expected.id), String(received.id));
    }
  }
}

function renameIds(value: unknown, questIds: Map<string, string>, completionIds: Map<string, string>): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => renameIds(item, questIds, completionIds));
  }
  if (!isRecord(value)) {
    return value;
  }
  const renamed: Record<string, unknown> = {};
  for (const [key, child] of Object.entries(value)) {
    if ((key === "id" || key === "quest_id") && typeof child === "string") {
      renamed[key] = questIds.get(child) ?? completionIds.get(child) ?? child;
    } else {
      renamed[key] = renameIds(child, questIds, completionIds);
    }
  }
  return renamed;
}

function invert(ids: Map<string, string>): Map<string, string> {
  return new Map([...ids].map(([recordingId, actualId]) => [actualId, recordingId]));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
