# Interview tasks

| Folder | Task |
| --- | --- |
| [proba_interviu_1](proba_interviu_1/) | Map supplier categories to our category ids (Java CLI job) |
| [proba_interviu_2](proba_interviu_2/) | Habit tracker API (Fastify) and UI (Next.js) |


# Evidence

## Proba 1

#### 1. Evidence it works: a test, an output sample, or a before/after.

<img width="802" height="298" alt="image" src="https://github.com/user-attachments/assets/6ae672ec-fe67-4778-a808-819248828fa5" />

#### 2. Half a page covering what you did, how you verified it, and what you deliberately did not do.

A supplier category, ignoring case and extra spaces, gets one of our ids. Each product votes with its revenue: its training label, or else a multinomial Naive Bayes guess from the name, the brand, and the supplier category. The name is counted twice, because a house brand spans many of our categories and was outvoting the product name. The id with the most revenue wins; a tie goes to the smaller id. Flour, sugar, dried pasta, rice, and cooking oil are split per product at 5 kg or 5 litres, and a pack we cannot read stays put. Dump names such as `misc`, `assorted`, and `new lines` do not use their own name as a feature.

Twenty percent of labels are held out, stratified by our category id, seed 19. A class with one label stays in training. Held-out labels do not train or vote. Five abbreviations are fixed (`tns`, `wn`, `chs`, `veg`, `vgtbl`); 101 more are prefixes learned from pure training categories.

Of 4,018 labelled SKUs, 799 are held out. They agree on 85.2% of SKUs and 89.2% of revenue, and outside dump buckets on 87.3% and 90.4%. Dump buckets agree on 40.4% of revenue. Using only the labelled majority, and ignoring the model, agrees on 79.2% of holdout revenue. A second run writes the same bytes. Tests cover the pack parser, a shared id for `Pls/Pulses.gr` and `Pls/Pulses.gr `, and a revenue tie.

The costly rows in `out/mistake_cost.csv` start with `misc lines 8` (€47,709, a dump mapped to ale), `pie/svry-pie` (€44,998), `biscuits_savoury-bisc` (€16,858), `promo lines wk42` (€15,511), and `gondola end spring` (€13,816, low margin). `ce and rt vgtbl` landing on cereals is a straight confusion. Ninety-three categories are weak: margin under 0.15, a dump name, or a vote that is almost only an unsure model.

One id per supplier category leaves the minority wrong. `tns > tinned to` is about €30,000 of korma sauce and €12,000 of tomatoes, so the sauce wins. `Crisps/Crisps` mixes crisps, breadsticks, oatcakes, and crackers. Promo and gondola names are not product categories and were left as ordinary rows. 1,244 of 2,750 normalized categories had no training label. The holdout is our estimate, not the hidden score. No product was relabelled, and nothing calls a model at runtime.

I checked the pack cases, confirmed that spelling variants share an id, that a second run matches, and the figures above.

#### 3. One line on what you used AI for and what you checked by hand.

I used AI for drafting and structuring this write-up and parts of the Java tests; I checked by hand by running `mapping\mvnw.cmd -f mapping\pom.xml test`, confirming the holdout percentages and deterministic output, and spot-checking the top `mistake_cost` rows.


#### 4. Anything you found outside the task and did not fix.

Mixed supplier categories. One normalized supplier category often contains unrelated products (e.g. tns > tinned to mixes korma sauce and chopped tomatoes; Crisps/Crisps mixes crisps, crackers, and breadsticks). The brief requires one our id per supplier category, so the revenue majority can map the whole group to a single leaf that is wrong for part of the assortment. I did not split supplier categories or assign multiple ids.

Promo and fixture names. Rows like promo lines wk42 or gondola end spring are not real product categories. I did not add them to the dump-name list or build a separate promo bucket; they are mapped like any other category.

Labels and scoring. 1,244 of 2,750 normalized categories had no training label in labelled_products.csv. Holdout metrics are our internal estimate; I did not relabel SKUs or tune against a hidden grader set.

Weak mappings. out/mistake_cost.csv flags low-margin votes and dump-like names; I report them but did not manually override those mappings.

