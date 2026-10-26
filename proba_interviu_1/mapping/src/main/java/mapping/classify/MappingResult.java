package mapping.classify;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public record MappingResult(
        List<RawAssignment> rows,
        List<ReportRow> report,
        Summary summary,
        Set<String> holdoutSkus,
        Set<String> trainingSkus
) {
    public record RawAssignment(String rawCategory, String ourCategoryId) {
    }

    public record ReportRow(
            String normalizedCategory,
            String ourCategoryId,
            double holdoutRevenue,
            double disagreedRevenue,
            double margin,
            String weakReason
    ) {
    }

    public record Summary(
            int rawCategories,
            int normalizedCategories,
            int labelledSkus,
            int trainingSkus,
            int holdoutSkus,
            int learnedAbbreviations,
            double holdoutSkuAgreement,
            double holdoutRevenueAgreement,
            double nonDumpSkuAgreement,
            double nonDumpRevenueAgreement,
            double dumpSkuAgreement,
            double dumpRevenueAgreement,
            int weakCategories,
            int categoriesWithoutTrainingLabel,
            int categoriesFlippedByModel,
            double labelOnlyHoldoutRevenueAgreement
    ) {
        public String text() {
            return String.format(Locale.US,
                    "Raw supplier categories: %d%n"
                            + "Normalized categories: %d%n"
                            + "Labelled SKUs: %d (training %d, holdout %d)%n"
                            + "Learned abbreviation rules: %d%n"
                            + "Holdout agreement: %.1f%% of SKUs, %.1f%% of revenue%n"
                            + "Outside dump buckets: %.1f%% of SKUs, %.1f%% of revenue%n"
                            + "Dump buckets: %.1f%% of SKUs, %.1f%% of revenue%n"
                            + "Weak categories: %d%n"
                            + "Categories with no training label: %d%n"
                            + "Categories where the model outvoted the labelled majority: %d%n"
                            + "Label-only holdout revenue agreement: %.1f%%%n",
                    rawCategories,
                    normalizedCategories,
                    labelledSkus,
                    trainingSkus,
                    holdoutSkus,
                    learnedAbbreviations,
                    percent(holdoutSkuAgreement),
                    percent(holdoutRevenueAgreement),
                    percent(nonDumpSkuAgreement),
                    percent(nonDumpRevenueAgreement),
                    percent(dumpSkuAgreement),
                    percent(dumpRevenueAgreement),
                    weakCategories,
                    categoriesWithoutTrainingLabel,
                    categoriesFlippedByModel,
                    percent(labelOnlyHoldoutRevenueAgreement));
        }

        private static double percent(double ratio) {
            return ratio * 100.0;
        }
    }
}
