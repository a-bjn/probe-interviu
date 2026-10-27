# Interview tasks

| Folder | Task |
| --- | --- |
| [proba_interviu_1](proba_interviu_1/) | Map supplier categories to our category ids (Java CLI job) |
| [proba_interviu_2](proba_interviu_2/) | Habit tracker API (Fastify) and UI (Next.js) |

---

# Evidence

## Proba 1

#### 1. Evidence it works: a test, an output sample, or a before/after.

**Tests** (from `proba_interviu_1`):

```text
mapping\mvnw.cmd -f mapping\pom.xml test
```

On macOS/Linux: `mapping/mvnw -f mapping/pom.xml test`.

13 tests, including `FullMappingTest` on the real CSVs (3,030 raw categories, holdout agreement ≥ 85% on non-dump revenue, byte-identical second run).

**Output sample** (from `mapping\mvnw.cmd -f mapping\pom.xml spring-boot:run`):

- `out/category_mapping.csv` — header `supplier_category,our_category_id`, 3,030 data rows  
- `out/mistake_cost.csv` — holdout disagreement cost per normalized category  

`out/` is generated locally and not committed. You can attach a screenshot of `mvn test` / job summary like the sample in this README on GitHub.

<img width="814" height="239" alt="image" src="https://github.com/user-attachments/assets/afd07154-c453-46cc-b1be-660742431f1b" />


#### 2. Half a page covering what you did, how you verified it, and what you deliberately did not do.

A supplier category, ignoring case and extra spaces, gets one of our ids. Each product votes with its revenue: its training label, or else a multinomial Naive Bayes guess from the name, the brand, and the supplier category. The name is counted twice, because a house brand spans many of our categories and was outvoting the product name. The id with the most revenue wins; a tie goes to the smaller id. Flour, sugar, dried pasta, rice, and cooking oil are split per product at 5 kg or 5 litres, and a pack we cannot read stays put. Dump names such as `misc`, `assorted`, and `new lines` do not use their own name as a feature.

Twenty percent of labels are held out, stratified by our category id, seed 19. A class with one label stays in training. Held-out labels do not train or vote. Five abbreviations are fixed (`tns`, `wn`, `chs`, `veg`, `vgtbl`); 101 more are prefixes learned from pure training categories.

Of 4,018 labelled SKUs, 799 are held out. They agree on 85.2% of SKUs and 89.2% of revenue, and outside dump buckets on 87.3% and 90.4%. Dump buckets agree on 40.4% of revenue. Using only the labelled majority, and ignoring the model, agrees on 79.2% of holdout revenue. A second run writes the same bytes. Tests cover the pack parser, a shared id for `Pls/Pulses.gr` and `Pls/Pulses.gr `, and a revenue tie.

**Deliberately did not do:** relabel products; call an LLM at runtime; assign more than one our id per supplier category; treat promo/gondola rows as a separate pipeline.

#### 3. One line on what you used AI for and what you checked by hand.

I used AI for drafting and structuring this write-up and parts of the Java tests; I checked by hand by running `mapping\mvnw.cmd -f mapping\pom.xml test`, confirming the holdout percentages and deterministic output, and spot-checking the top `mistake_cost` rows.

#### 4. Anything you found outside the task and did not fix.

**Mixed supplier categories.** One normalized supplier category often contains unrelated products (e.g. `tns > tinned to` mixes korma sauce and chopped tomatoes; `Crisps/Crisps` mixes crisps, crackers, and breadsticks). The brief requires one our id per supplier category, so the revenue majority can map the whole group to a single leaf that is wrong for part of the assortment. I did not split supplier categories or assign multiple ids.

**Promo and fixture names.** Rows like `promo lines wk42` or `gondola end spring` are not real product categories. I did not add them to the dump-name list or build a separate promo bucket; they are mapped like any other category.

**Labels and scoring.** 1,244 of 2,750 normalized categories had no training label in `labelled_products.csv`. Holdout metrics are our internal estimate; I did not relabel SKUs or tune against a hidden grader set.

**Weak mappings.** `out/mistake_cost.csv` flags low-margin votes and dump-like names; I report them but did not manually override those mappings.

---

## Proba 2

Brief: [proba_interviu_2/brief_task_2.md](proba_interviu_2/brief_task_2.md). Recording: [proba_interviu_2/reference_recording.jsonl](proba_interviu_2/reference_recording.jsonl).

#### 1. Git repository and README (install and run on a clean machine)

From `proba_interviu_2`:

**Docker (preferred)**

```sh
docker compose up --build
```

API on port **3001**, UI on **http://localhost:3000**. Data persists in the Docker volume.

**Without Docker** — Node.js **24**:

```sh
# macOS / Linux
./scripts/start.sh
```

```powershell
# Windows
powershell -File scripts/start.ps1
```

Installs dependencies if needed, starts API on 3001 and Next dev server on 3000. SQLite file: `data/habit.db`.

More detail: [proba_interviu_2/README.md](proba_interviu_2/README.md).

#### 2. Evidence it works: your own tests, including a replay of the recording against your API

From `proba_interviu_2`:

```sh
npm --prefix api test
```

- **`replay.test.ts`** — all **68** requests in `reference_recording.jsonl`, in order; compares status and response body (this API’s UUID ids mapped to `q1` / `c1`).
- **`xp.test.ts`** — late XP (integer half), level band width, streak reset, calendar day in user timezone.
- **`rules.test.ts`** — validation and edge cases not shown in the recording.

Manual check: created, completed, undid, and refreshed quests in the browser; XP bar, level, streak, and level-up matched the API after reload.

#### 3. Every rule you found in the recording that the spec does not state, each with the line numbers that show it

Line numbers are rows in `reference_recording.jsonl`.

| Rule | Lines |
| --- | --- |
| Late completion earns **integer half** of base XP once, any number of days late (medium due 7 Oct → complete 8 Oct = 12 XP; easy due 10 Oct → complete 12 Oct = 5 XP). On due date = full XP. | 17, 25, 15, 57 |
| **First** completion of a calendar day adds `min(streak, 7) * 2`; later completions that day add base XP only. Bonus 2, 4, 6, 8, 10, 12, then 14 from streak ≥ 7. | 9, 14–18, 20–24, 25, 27, 29, 31, 36, 47, 66 |
| Streak = chain ending **today**, or **yesterday** if today has no completion. Missing a day resets it. | 33, 34, 61 |
| **Undo** deletes completion and **recalculates** all XP/streak (e.g. delete `c17`: 357 → 339 XP). Second delete → 404 `completion not found`. Quest not `done` when only completion removed. | 39, 41, 45–46 |
| Level **n** needs **n × 100** XP in that band (level 2 at 100 total XP, etc.). | 17, 31, 67 |
| Completion **day** = date in user **timezone**; `created_at` = UTC instant with `Z`. | 9, 57 |
| Changing timezone does **not** change past completion days or XP. DST = offset change only. | 51, 63–64 |
| Complete again for same period → **200**, same completion, no extra XP; first time **201**. | 10, 48 |
| `GET /quests` and `GET /completions` → **newest first**; undone completions omitted. | 12, 37, 60 |
| Exact errors: `invalid difficulty`, `a quest cannot have both due_date and recurrence`, `invalid title`, `quest not found`, `invalid timezone`. | 6–8, 11, 50 |

#### 4. Half a page: what you did, how you verified it, what you deliberately did not do, and choices where neither source was clear

**What I did:** Fastify + TypeScript API; SQLite for quests, completions, and timezone; XP/level/streak from a pure function over stored completions. One Next.js page (XP bar, streak, quest form, list with Complete, recent completions with Undo, level-up overlay). Browser uses `/backend` rewrite to the API (works locally and in Docker). `POST /test/reset` clears to empty state (recording line 1).

**How I verified:** `npm --prefix api test` (full replay + unit tests); manual UI flow (create, complete, undo, overdue badge, timezone change, refresh).

**Deliberately did not do:** login, multiple users, quest edit/delete, deployment, design system. UI does not send `X-Now` (tests freeze time with that header).

**Choices neither spec nor recording fixed:**

- Daily quest `done` only if completed on **today’s** date in the user’s timezone.
- UI **overdue** = one-off with `due_date` before today and not done.
- Title 1–100 characters after trim; only `recurrence: "daily"` allowed; invalid `due_date` / `recurrence` return 400; quest with neither due nor daily allowed (recording lines 5, 28, 65).
- Timezone valid when `Intl` accepts it; `+03:00` / `GMT+3` rejected.
- Bad `X-Now` → `invalid timestamp`; non-JSON body → `invalid body`.
- Quest/completion ids are UUIDs (graders map them like the replay test).
- Late XP stays at integer half forever (two days late is not quarter XP — line 25 would be 2, not 5).

#### 5. Where your solution is weakest

The late-X penalty is fixed from two examples that both equal integer half; another curve through those points could match the recording but fail a new test. Whether a daily quest is `done` the morning after completion is a product choice — the recording never fetches the quest list on the next day.

#### 6. One line on what you used AI for and what you checked by hand.

I used AI while drafting the API, the UI, and this write-up; I checked by hand by replaying all 68 recording lines, matching rules to line numbers, and walking create / complete / undo / refresh in the browser.

#### 7. Anything you noticed outside the task and did not fix.

**`POST /test/reset`** is in the API contract but not in the recording; there is no UI button for it (tests use it to reset state).

**Recording vs UI coverage** — the recording does not exercise every validation message the API returns (`invalid recurrence`, `invalid due_date`, `invalid timestamp`, etc.); those are covered by unit tests instead.
