package mapping.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvTables {
    public record Loaded(List<OurCategory> categories, List<Product> products) {
    }

    private CsvTables() {
    }

    public static Loaded load(Path products, Path categories, Path labels, Path sales) throws IOException {
        Map<String, String> labelBySku = readColumnPairs(labels, "supplier_sku", "our_category_id");
        Map<String, Double> revenueBySku = readRevenue(sales);
        List<OurCategory> ourCategories = new ArrayList<>();
        for (String[] row : readRows(categories, "our_category_id", "path")) {
            ourCategories.add(new OurCategory(row[0].trim(), row[1].trim()));
        }
        List<Product> catalogue = new ArrayList<>();
        for (String[] row : readRows(products, "supplier_sku", "supplier_category", "product_name", "brand", "pack_size")) {
            String sku = row[0].trim();
            if (sku.isEmpty()) {
                continue;
            }
            String label = labelBySku.get(sku);
            catalogue.add(new Product(
                    sku,
                    row[1],
                    row[2].trim(),
                    row[3].trim(),
                    row[4].trim(),
                    revenueBySku.getOrDefault(sku, 0.0),
                    label == null || label.isBlank() ? null : label));
        }
        return new Loaded(List.copyOf(ourCategories), catalogue);
    }

    private static Map<String, String> readColumnPairs(Path path, String keyColumn, String valueColumn) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String[] row : readRows(path, keyColumn, valueColumn)) {
            if (!row[0].isBlank()) {
                values.put(row[0].trim(), row[1].trim());
            }
        }
        return values;
    }

    private static Map<String, Double> readRevenue(Path path) throws IOException {
        Map<String, Double> revenue = new HashMap<>();
        for (String[] row : readRows(path, "supplier_sku", "revenue")) {
            if (row[0].isBlank()) {
                continue;
            }
            try {
                revenue.merge(row[0].trim(), Double.parseDouble(row[1].trim()), Double::sum);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Bad revenue for " + row[0] + " in " + path, ex);
            }
        }
        return revenue;
    }

    private static List<String[]> readRows(Path path, String... columns) throws IOException {
        try (BufferedReader reader = open(path); CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            Iterator<CSVRecord> records = parser.iterator();
            if (!records.hasNext()) {
                throw new IllegalArgumentException("Empty CSV: " + path);
            }
            Map<String, Integer> header = index(records.next());
            int[] indexes = new int[columns.length];
            for (int i = 0; i < columns.length; i++) {
                Integer index = header.get(columns[i]);
                if (index == null) {
                    throw new IllegalArgumentException("Missing column '" + columns[i] + "' in " + path + " (headers: " + header.keySet() + ")");
                }
                indexes[i] = index;
            }
            List<String[]> rows = new ArrayList<>();
            while (records.hasNext()) {
                CSVRecord record = records.next();
                String[] values = new String[columns.length];
                for (int i = 0; i < indexes.length; i++) {
                    values[i] = indexes[i] < record.size() ? record.get(indexes[i]) : "";
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private static BufferedReader open(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Missing CSV: " + path);
        }
        BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        reader.mark(1);
        if (reader.read() != '\uFEFF') {
            reader.reset();
        }
        return reader;
    }

    private static Map<String, Integer> index(CSVRecord header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i).replace("\uFEFF", "").trim();
            columns.put(name, i);
        }
        return columns;
    }
}
