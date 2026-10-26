package mapping;

import mapping.classify.CategoryMapper;
import mapping.classify.MappingResult;
import mapping.ingest.CsvTables;
import mapping.output.MappingWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

@Component
public class MappingJob {
    private final MappingProperties properties;

    public MappingJob(MappingProperties properties) {
        this.properties = properties;
    }

    public Path outputPath() {
        return resolve(properties.getOutput());
    }

    public MappingResult execute() throws IOException {
        Path products = resolve(properties.getProducts());
        Path categories = resolve(properties.getCategories());
        Path labels = resolve(properties.getLabels());
        Path sales = resolve(properties.getSales());
        requireFile(products);
        requireFile(categories);
        requireFile(labels);
        requireFile(sales);
        CsvTables.Loaded loaded = CsvTables.load(products, categories, labels, sales);
        MappingResult result = CategoryMapper.map(
                loaded.categories(),
                loaded.products(),
                properties.getSeed(),
                properties.getHoldoutFraction(),
                properties.getMarginThreshold(),
                properties.getPackKilograms(),
                properties.getPackLitres());
        MappingWriter.writeMapping(resolve(properties.getOutput()), result.rows());
        MappingWriter.writeReport(resolve(properties.getReport()), result.report());
        return result;
    }

    private static Path resolve(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Path.of("").toAbsolutePath().resolve(path).normalize();
    }

    private static void requireFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Missing CSV: " + path);
        }
    }
}
