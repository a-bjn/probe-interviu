# Map their categories to ours

## Background

We are a grocery retailer taking on a new wholesale supplier. The supplier sends its
range grouped into its own categories, about 3,000 of them. Our shop has its own category
tree (`our_categories.csv`), and every product must sit on one of our category pages.

We will move products over category by category: every product in a supplier category
goes to the one category of ours that the supplier category is mapped to.

## The task

1. Map every supplier category to **one** of our categories, judging from the products
   inside it.
2. Use `sales.csv` to tell us which of your mistakes would cost us most.
3. Tell us where your mapping is weakest.

Output a CSV with the header `supplier_category,our_category_id`, one line for every
distinct `supplier_category` value in `supplier_products.csv`, written exactly as it
appears there.

```
supplier_category,our_category_id
EXAMPLE CAT 1,1234
Example cat 2,5678
```

The supplier's system ignores letter case and spacing in category names: two values that
differ only in case or spaces are the same category, and must get the same answer. Still
give one line for each value as written.

`labelled_products.csv` lists products our team has already placed by hand. We score your
whole file against answers we keep.

## Files

| File | Rows | Columns |
| --- | --- | --- |
| `our_categories.csv` | one per category of ours | `our_category_id`, `path` |
| `supplier_products.csv` | one per supplier product | `supplier_sku`, `supplier_category`, `product_name`, `brand`, `pack_size` |
| `labelled_products.csv` | a few thousand | `supplier_sku`, `our_category_id` |
| `sales.csv` | one per supplier product | `supplier_sku`, `units`, `revenue` |

`sales.csv` is the supplier's sales of each product last year; revenue is in euros.

## What to send back

1. Evidence it works: a test, an output sample, or a before/after.
2. Half a page covering what you did, how you verified it, and what you deliberately did
   not do.
3. One line on what you used AI for and what you checked by hand.
4. Anything you found outside the task and did not fix.
5. A Github repo with the code, with running instructions and the half page mentioned above
