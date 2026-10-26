package mapping.classify;

import mapping.ingest.OurCategory;
import mapping.ingest.Product;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryMapperTest {
    private static final List<OurCategory> CATEGORIES = List.of(
            new OurCategory("1", "Food cupboard > Tins > Tinned tomatoes"),
            new OurCategory("2", "Baby & pets > Pets > Dog food"),
            new OurCategory("10", "Food cupboard > Baking > Flour, retail packs (under 5 kg)"),
            new OurCategory("20", "Food cupboard > Baking > Flour, catering packs (5 kg and over)"),
            new OurCategory("100", "Dairy & eggs > Milk > Fresh milk"),
            new OurCategory("200", "Drinks > Wine > Red wine"));

    @Test
    void spellingsShareAnIdAndTheLowerIdWinsATie() {
        List<Product> products = List.of(
                product("a", "Foo", "chopped tomatoes", "1 kg", 10, "1"),
                product("b", "foo ", "chopped tomatoes", "1 kg", 10, "1"),
                product("c", "Tie", "mystery item", "1 kg", 10, "200"),
                product("d", "Tie", "mystery item", "1 kg", 10, "100"));
        MappingResult result = map(products, 0);
        assertEquals("1", idFor(result, "Foo"));
        assertEquals("1", idFor(result, "foo "));
        assertEquals("100", idFor(result, "Tie"));
        assertEquals(List.of("Foo", "foo ", "Tie"), result.rows().stream().map(MappingResult.RawAssignment::rawCategory).toList());
    }

    @Test
    void aDumpDoesNotUseItsOwnNameAsAFeature() {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            products.add(product("t" + i, "Tins", "chopped tomatoes", "1 kg", 10, "1"));
        }
        products.add(product("d1", "Dog food misc", "chopped tomatoes", "1 kg", 50, null));
        MappingResult result = map(products, 0);
        assertEquals("1", idFor(result, "Dog food misc"));
    }

    @Test
    void packSizeMovesFlourAcrossTheFiveKilogramLine() {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            products.add(product("f" + i, "Flour", "plain flour", "1 kg", 10, "10"));
        }
        products.add(product("bulk", "Bulk flour", "strong flour", "10 kg", 40, null));
        products.add(product("small", "Retail flour", "plain flour", "1 kg", 40, null));
        MappingResult result = map(products, 0);
        assertEquals("20", idFor(result, "Bulk flour"));
        assertEquals("10", idFor(result, "Retail flour"));
    }

    @Test
    void aPureGroupKeepsItsLabelAndHoldoutDisagreementIsPriced() {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            products.add(product("a" + i, "Same", "chopped tomatoes", "1 kg", 100, "1"));
        }
        for (int i = 1; i <= 5; i++) {
            products.add(product("b" + i, "Same", "chopped tomatoes", "1 kg", 1, "2"));
        }
        MappingResult result = map(products, 0.2);
        assertEquals("1", idFor(result, "Same"));
        assertTrue(result.trainingSkus().contains("a1") || result.holdoutSkus().contains("a1"));
        assertTrue(java.util.Collections.disjoint(result.trainingSkus(), result.holdoutSkus()));
        MappingResult.ReportRow row = result.report().get(0);
        assertEquals("same", row.normalizedCategory());
        assertEquals(1.0, row.disagreedRevenue(), 0.001);
        assertEquals(101.0, row.holdoutRevenue(), 0.001);
        MappingResult again = map(products, 0.2);
        assertEquals(result.rows(), again.rows());
    }

    @Test
    void aSingleLabelIsNotHeldOut() {
        MappingResult result = map(List.of(product("only", "Solo", "fresh milk", "1 l", 5, "100")), 0.5);
        assertTrue(result.trainingSkus().contains("only"));
        assertTrue(result.holdoutSkus().isEmpty());
        assertEquals("100", idFor(result, "Solo"));
    }

    private static MappingResult map(List<Product> products, double fraction) {
        return CategoryMapper.map(CATEGORIES, products, 19, fraction, 0.15, 5, 5);
    }

    private static String idFor(MappingResult result, String raw) {
        return result.rows().stream()
                .filter(row -> row.rawCategory().equals(raw))
                .map(MappingResult.RawAssignment::ourCategoryId)
                .findFirst()
                .orElseThrow();
    }

    private static Product product(String sku, String category, String name, String pack, double revenue, String label) {
        return new Product(sku, category, name, "", pack, revenue, label);
    }
}
