# Notes

A supplier category, ignoring case and extra spaces, gets one of our ids. Each product votes with its revenue: its training label, or else a multinomial Naive Bayes guess from the name, the brand, and the supplier category. The name is counted twice, because a house brand spans many of our categories and was outvoting the product name. The id with the most revenue wins; a tie goes to the smaller id. Flour, sugar, dried pasta, rice, and cooking oil are split per product at 5 kg or 5 litres, and a pack we cannot read stays put. Dump names such as `misc`, `assorted`, and `new lines` do not use their own name as a feature.

Twenty percent of labels are held out, stratified by our category id, seed 19. A class with one label stays in training. Held-out labels do not train or vote. Five abbreviations are fixed (`tns`, `wn`, `chs`, `veg`, `vgtbl`); 101 more are prefixes learned from pure training categories.

Of 4,018 labelled SKUs, 799 are held out. They agree on 85.2% of SKUs and 89.2% of revenue, and outside dump buckets on 87.3% and 90.4%. Dump buckets agree on 40.4% of revenue. Using only the labelled majority, and ignoring the model, agrees on 79.2% of holdout revenue. A second run writes the same bytes. Tests cover the pack parser, a shared id for `Pls/Pulses.gr` and `Pls/Pulses.gr `, and a revenue tie.

The costly rows in `out/mistake_cost.csv` start with `misc lines 8` (€47,709, a dump mapped to ale), `pie/svry-pie` (€44,998), `biscuits_savoury-bisc` (€16,858), `promo lines wk42` (€15,511), and `gondola end spring` (€13,816, low margin). `ce and rt vgtbl` landing on cereals is a straight confusion. Ninety-three categories are weak: margin under 0.15, a dump name, or a vote that is almost only an unsure model.

One id per supplier category leaves the minority wrong. `tns > tinned to` is about €30,000 of korma sauce and €12,000 of tomatoes, so the sauce wins. `Crisps/Crisps` mixes crisps, breadsticks, oatcakes, and crackers. Promo and gondola names are not product categories and were left as ordinary rows. 1,244 of 2,750 normalized categories had no training label. The holdout is our estimate, not the hidden score. No product was relabelled, and nothing calls a model at runtime.

I checked the pack cases, confirmed that spelling variants share an id, that a second run matches, and the figures above.
