package mapping.ingest;

import mapping.classify.MappingResult;
import mapping.output.MappingWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvRoundTripTest {
    @TempDir
    Path temp;

    @Test
    void keepsATrailingSpaceAndACommaInsideTheCategory() throws Exception {
        Path products = temp.resolve("supplier_products.csv");
        Path categories = temp.resolve("our_categories.csv");
        Path labels = temp.resolve("labelled_products.csv");
        Path sales = temp.resolve("sales.csv");
        Files.writeString(categories, "our_category_id,path\n1,\"Food cupboard > Sauces > Pasta sauces, other\"\n");
        Files.writeString(products, "supplier_sku,supplier_category,product_name,brand,pack_size\n"
                + "S1,Pls/Pulses.gr ,Quinoa,Ostdale,1 kg\n"
                + "S2,\"Foo, bar\",Oil,Brand,1 l\n");
        Files.writeString(labels, "supplier_sku,our_category_id\nS1,1\n");
        Files.writeString(sales, "supplier_sku,units,revenue\nS1,1,2.50\nS2,1,3.00\n");

        CsvTables.Loaded loaded = CsvTables.load(products, categories, labels, sales);
        assertEquals("Pls/Pulses.gr ", loaded.products().get(0).rawCategory());
        assertEquals("Foo, bar", loaded.products().get(1).rawCategory());
        assertEquals("Food cupboard > Sauces > Pasta sauces, other", loaded.categories().get(0).path());
        assertEquals(2.5, loaded.products().get(0).revenue(), 0.001);
        assertEquals("1", loaded.products().get(0).label());

        Path out = temp.resolve("out.csv");
        MappingWriter.writeMapping(out, List.of(
                new MappingResult.RawAssignment("Pls/Pulses.gr ", "1"),
                new MappingResult.RawAssignment("Foo, bar", "2")));
        String csv = Files.readString(out);
        assertTrue(csv.startsWith("supplier_category,our_category_id\n"));
        assertTrue(csv.contains("Pls/Pulses.gr ,1\n"));
        assertTrue(csv.contains("\"Foo, bar\",2\n"));
    }
}
