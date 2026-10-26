package mapping;

import mapping.classify.CategoryMapper;
import mapping.classify.MappingResult;
import mapping.ingest.CsvTables;
import mapping.normalize.CategoryKey;
import mapping.output.MappingWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FullMappingTest {
    @Test
    @Timeout(value = 4, unit = TimeUnit.MINUTES)
    void mapsTheSuppliedCatalogueTwiceTheSameWay() throws Exception {
        Path root = dataRoot();
        assumeTrue(root != null, "supplier CSVs are not next to the module");
        CsvTables.Loaded loaded = CsvTables.load(
                root.resolve("supplier_products.csv"),
                root.resolve("our_categories.csv"),
                root.resolve("labelled_products.csv"),
                root.resolve("sales.csv"));
        assertEquals(150, loaded.categories().size());

        MappingResult first = CategoryMapper.map(loaded.categories(), loaded.products(), 19, 0.2, 0.15, 5, 5);
        MappingResult second = CategoryMapper.map(loaded.categories(), loaded.products(), 19, 0.2, 0.15, 5, 5);
        System.out.print(first.summary().text());
        first.report().stream().limit(12).forEach(row -> System.out.printf(
                "  %.2f  %s -> %s  %s%n",
                row.disagreedRevenue(), row.normalizedCategory(), row.ourCategoryId(), row.weakReason()));

        assertEquals(3030, first.rows().size());
        assertEquals(2750, first.summary().normalizedCategories());
        assertEquals(first.rows(), second.rows());
        assertTrue(java.util.Collections.disjoint(first.holdoutSkus(), first.trainingSkus()));

        Set<String> knownIds = new HashSet<>();
        loaded.categories().forEach(category -> knownIds.add(category.id()));
        Map<String, String> idByKey = new HashMap<>();
        Set<String> raws = new HashSet<>();
        for (MappingResult.RawAssignment row : first.rows()) {
            assertTrue(raws.add(row.rawCategory()));
            assertTrue(knownIds.contains(row.ourCategoryId()));
            String key = CategoryKey.of(row.rawCategory());
            String previous = idByKey.putIfAbsent(key, row.ourCategoryId());
            if (previous != null) {
                assertEquals(previous, row.ourCategoryId());
            }
        }
        assertEquals(3030, raws.size());
        assertTrue(idByKey.size() < raws.size(), "spelling variants should share a category");

        Path outDir = Path.of("target", "mapping-check");
        Path one = outDir.resolve("a.csv");
        Path two = outDir.resolve("b.csv");
        MappingWriter.writeMapping(one, first.rows());
        MappingWriter.writeMapping(two, second.rows());
        assertEquals(Files.readString(one), Files.readString(two));
        assertTrue(first.summary().nonDumpRevenueAgreement() >= 0.85,
                "non-dump holdout revenue agreement was " + first.summary().nonDumpRevenueAgreement());
    }

    private static Path dataRoot() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 4 && dir != null; i++) {
            if (Files.exists(dir.resolve("supplier_products.csv"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }
}
