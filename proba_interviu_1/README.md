# Category mapping

A Java 21 command-line job. It reads the four CSVs in this directory and writes one of our categories for every supplier category.

Spring Boot is only the entry point. The mapping itself does not use Spring.

## Run

Java 21 is required. The Maven wrapper downloads Maven; a system install is not required.

From this directory (`proba_interviu_1`):

```
mapping\mvnw.cmd -f mapping\pom.xml test
mapping\mvnw.cmd -f mapping\pom.xml spring-boot:run
```

On macOS or Linux, use `mapping/mvnw` instead of `mapping\mvnw.cmd`.

`spring-boot:run` uses `mapping/` as its working directory. The paths in `mapping/src/main/resources/application.yml` point at the CSVs one level up.

## Output

`out/category_mapping.csv` has the header `supplier_category,our_category_id` and one row for every distinct supplier category, written as it appears in the file. Values that differ only by case or spacing share an id.

`out/mistake_cost.csv` lists each normalized supplier category with the holdout revenue that landed on a different id, largest first.

Both files are generated. They are not committed.

The measured agreement and the weak spots are in `NOTES.md`.
