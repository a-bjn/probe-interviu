package mapping.ingest;

public record Product(
        String sku,
        String rawCategory,
        String name,
        String brand,
        String packSize,
        double revenue,
        String label
) {
}
