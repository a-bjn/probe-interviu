package mapping;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mapping")
public class MappingProperties {
    private String products = "../supplier_products.csv";
    private String categories = "../our_categories.csv";
    private String labels = "../labelled_products.csv";
    private String sales = "../sales.csv";
    private String output = "../out/category_mapping.csv";
    private String report = "../out/mistake_cost.csv";
    private double holdoutFraction = 0.2;
    private long seed = 19;
    private double marginThreshold = 0.15;
    private double packKilograms = 5;
    private double packLitres = 5;

    public String getProducts() {
        return products;
    }

    public void setProducts(String products) {
        this.products = products;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getSales() {
        return sales;
    }

    public void setSales(String sales) {
        this.sales = sales;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public double getHoldoutFraction() {
        return holdoutFraction;
    }

    public void setHoldoutFraction(double holdoutFraction) {
        this.holdoutFraction = holdoutFraction;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public double getMarginThreshold() {
        return marginThreshold;
    }

    public void setMarginThreshold(double marginThreshold) {
        this.marginThreshold = marginThreshold;
    }

    public double getPackKilograms() {
        return packKilograms;
    }

    public void setPackKilograms(double packKilograms) {
        this.packKilograms = packKilograms;
    }

    public double getPackLitres() {
        return packLitres;
    }

    public void setPackLitres(double packLitres) {
        this.packLitres = packLitres;
    }
}
