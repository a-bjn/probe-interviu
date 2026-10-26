package mapping.output;

import mapping.classify.MappingResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class MappingWriter {
    private MappingWriter() {
    }

    public static void writeMapping(Path path, List<MappingResult.RawAssignment> rows) throws IOException {
        write(path, "supplier_category,our_category_id", rows.stream().map(row -> new String[]{row.rawCategory(), row.ourCategoryId()}).toList());
    }

    public static void writeReport(Path path, List<MappingResult.ReportRow> rows) throws IOException {
        List<String[]> lines = rows.stream()
                .map(row -> new String[]{
                        row.normalizedCategory(),
                        row.ourCategoryId(),
                        String.format(Locale.US, "%.2f", row.holdoutRevenue()),
                        String.format(Locale.US, "%.2f", row.disagreedRevenue()),
                        String.format(Locale.US, "%.4f", row.margin()),
                        row.weakReason()
                })
                .toList();
        write(path, "supplier_category_norm,our_category_id,holdout_revenue,disagreed_revenue,margin,weak_reason", lines);
    }

    private static void write(Path path, String header, List<String[]> rows) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(header);
            writer.write('\n');
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) {
                        writer.write(',');
                    }
                    writer.write(field(row[i]));
                }
                writer.write('\n');
            }
        }
    }

    private static String field(String value) {
        String text = value == null ? "" : value;
        if (text.indexOf(',') < 0 && text.indexOf('"') < 0 && text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
