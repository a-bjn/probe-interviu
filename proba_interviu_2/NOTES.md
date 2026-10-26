# Notes

## Rules that are only fully defined in the recording

The spec is right, and incomplete. These are the parts filled in from `reference_recording.jsonl`. Line numbers are the file lines.

- A completion after the due date earns integer half of the base XP, once, however many days late. Medium due 7 Oct, completed 8 Oct, is 12 (line 17). Easy due 10 Oct, completed 12 Oct, is 5 (line 25). Completing on the due date is still full XP (lines 15 and 57).
- The first completion of a calendar day also awards `min(streak, 7) * 2`. A second completion the same day adds only its base XP (lines 15, 18, 25, 29, 36, 47, 66). The bonus is 2, 4, 6, 8, 10, 12, then 14 from a streak of 7 upward (lines 9, 14, 16, 17, 20–24, 27, 31).
- The current streak is the chain ending today, or ending yesterday if today has no completion yet. On 15 Oct at 22:00 the streak is still 10 (line 33). On 16 Oct at 07:00, with nothing on the 15th, it is 0 (line 34). Missing 22 Oct makes the 23 Oct completion start again at 1 (line 61).
- Undo deletes the completion and the whole total is recalculated. Deleting `c17` drops XP from 357 to 339: the 10 base XP, that day’s bonus of 4, and the next day’s bonus falling from 6 to 2 because the chain breaks (lines 45–46). A second delete is 404 `completion not found` (line 39). The quest is no longer done when its only completion is gone (line 41).
- Level `n` is `n * 100` XP wide. Level 2 starts at 100 (line 17, XP 112). Level 3 starts at 300 (line 31, XP 315). At 547 XP the bar is 247 into level 3, with 53 left (line 67).
- The completion day is the calendar date in the user’s zone, not the UTC date. 21 Oct 22:30 in New York stays 21 Oct even though UTC is already 22 Oct 02:30 (line 57). `created_at` is that instant in UTC, to the second, with a `Z` (line 9).
- Changing zone does not rewrite old days or XP (line 51). Romania’s clock change on 25 Oct is just a different offset on the same rule (lines 63–64).
- Completing something that is already complete for that period returns 200 and the same completion, with no extra XP (lines 10 and 48). The first time is 201.
- `GET /quests` and `GET /completions` are newest first (lines 12 and 37). Undone completions disappear from the list (line 60 no longer has `c15` or `c17`).
- Error text, copied exactly: `invalid difficulty` (line 6), `a quest cannot have both due_date and recurrence` (line 7), `invalid title` (line 8), `quest not found` (line 11), `invalid timezone` (line 50).

## What I did

The API is Fastify and TypeScript, as the brief asks. XP, level, and streak come from a pure function over the completions that are still stored. SQLite keeps quests, completions, and the time zone in `data/habit.db`, and `POST /test/reset` returns the empty state from line 1. The page is one Next.js screen: the bar, the level, and the streak are the top of the page, and a level change opens a short full-screen moment. The browser talks to the API through a Next rewrite, so the same page works locally and in Docker.

I verified it by replaying all 68 recorded requests (`npm --prefix api test`) and by creating, completing, undoing, and refreshing a quest in the browser. The replay maps generated ids back to `q1` / `c1` and checks the body, not only the status.

I did not build accounts, editing, deletion of quests, or a design system. The page does not send `X-Now`; the header exists so tests can freeze time.

## Choices where neither source was clear

- A daily quest is `done` only when it has a completion on today’s date in the user’s zone. The recording never fetches the list on the morning after a daily was completed.
- Overdue, in the UI, means a one-off whose due date is before today and which is not done.
- After trim, a title must be 1–100 characters. `recurrence` other than `daily` is `invalid recurrence`. A `due_date` that is not a real `yyyy-mm-dd` is `invalid due_date`. A quest with neither is allowed, matching lines 5, 28, and 65.
- A time zone is valid when `Intl` accepts it. Bare offsets (`+03:00`, `GMT+3`) are rejected.
- An unreadable `X-Now` is 400 `invalid timestamp`. A body that is not JSON is 400 `invalid body`.
- Ids are UUID strings. The replay test maps them; the brief says the graders will too.
- Late XP stays at integer half forever. Two days late is not a quarter, because line 25 would then be 2, not 5.

## Where this is weakest

The late penalty is fixed from two examples that both equal integer half. A different curve that happens to pass through those two points would still match the recording and fail a new one. `done` for a daily quest on the next morning is a choice, not an observation.

## Noticed and left alone

`POST /test/reset` is in the contract and not in the recording. The UI has no control for it.
