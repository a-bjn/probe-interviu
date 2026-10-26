package mapping.pack;

import mapping.ingest.OurCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PackSizeTest {
    @Test
    void parsesTheCasesFromTheBrief() {
        assertEquals(0.25, PackSize.parse("0,25 kg").kilograms(), 1e-9);
        assertEquals(0.1, PackSize.parse("0,1 kg").kilograms(), 1e-9);
        assertEquals(0.6, PackSize.parse("2x300 gr").kilograms(), 1e-9);
        assertEquals(18.0, PackSize.parse("12 x 1.5 litre").litres(), 1e-9);
        assertNull(PackSize.parse("x12").kilograms());
        assertNull(PackSize.parse("x12").litres());
        assertNull(PackSize.parse("each").kilograms());
        assertEquals(1.0, PackSize.parse("1000 gr").kilograms(), 1e-9);
        assertEquals(0.5, PackSize.parse("0.5 KG").kilograms(), 1e-9);
        assertEquals(5.0, PackSize.parse("5 kg").kilograms(), 1e-9);
        assertEquals(5.0, PackSize.parse("5000ml").litres(), 1e-9);
        assertEquals(6.0, PackSize.parse("12x500 ml").litres(), 1e-9);
    }

    @Test
    void fiveKilogramsAndFiveLitresAreCatering() {
        PackFamilies families = PackFamilies.from(List.of(
                new OurCategory("10", "Food cupboard > Baking > Flour, retail packs (under 5 kg)"),
                new OurCategory("20", "Food cupboard > Baking > Flour, catering packs (5 kg and over)"),
                new OurCategory("30", "Food cupboard > Oils > Cooking oil, retail packs (under 5 litres)"),
                new OurCategory("40", "Food cupboard > Oils > Cooking oil, catering packs (5 litres and over)"),
                new OurCategory("50", "Fruit & veg > Vegetables > Tomatoes")));

        assertEquals("20", families.adjust("10", PackSize.parse("5 kg"), 5, 5));
        assertEquals("20", families.adjust("20", PackSize.parse("10 kg"), 5, 5));
        assertEquals("10", families.adjust("20", PackSize.parse("4.999 kg"), 5, 5));
        assertEquals("40", families.adjust("30", PackSize.parse("5 l"), 5, 5));
        assertEquals("30", families.adjust("40", PackSize.parse("4.9 litre"), 5, 5));
        assertEquals("10", families.adjust("10", PackSize.parse("each"), 5, 5));
        assertEquals("50", families.adjust("50", PackSize.parse("10 kg"), 5, 5));
    }
}
