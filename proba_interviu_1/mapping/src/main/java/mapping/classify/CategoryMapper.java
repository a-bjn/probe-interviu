package mapping.classify;

import mapping.ingest.OurCategory;
import mapping.ingest.Product;
import mapping.normalize.CategoryKey;
import mapping.pack.PackFamilies;
import mapping.pack.PackSize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static MappingResult map(
            List<OurCategory> categories,
            List<Product> products,
            long seed,
            double holdoutFraction,
            double marginThreshold,
            double packKilograms,
            double packLitres) {
        Map<String, String> paths = new HashMap<>();
        for (OurCategory category : categories) {
            paths.put(category.id(), category.path());
        }
        PackFamilies families = PackFamilies.from(categories);

        Map<String, String> labelled = new HashMap<>();
        for (Product product : products) {
            if (product.label() != null) {
                labelled.put(product.sku(), product.label());
            }
        }
        Set<String> holdout = Holdout.select(labelled, seed, holdoutFraction);
        Set<String> training = new HashSet<>();
        for (String sku : labelled.keySet()) {
            if (!holdout.contains(sku)) {
                training.add(sku);
            }
        }

        Map<String, List<Product>> groups = new LinkedHashMap<>();
        List<String> rawOrder = new ArrayList<>();
        Map<String, String> rawToKey = new HashMap<>();
        Set<String> seenRaw = new HashSet<>();
        for (Product product : products) {
            String key = CategoryKey.of(product.rawCategory());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(product);
            if (seenRaw.add(product.rawCategory())) {
                rawOrder.add(product.rawCategory());
                rawToKey.put(product.rawCategory(), key);
            }
        }

        AbbreviationLexicon lexicon = AbbreviationLexicon.learn(groups, holdout, paths);
        NaiveBayes bayes = new NaiveBayes();
        for (OurCategory category : categories) {
            bayes.train(category.id(), Tokens.pathPrototype(category.path(), lexicon));
        }
        for (Product product : products) {
            if (product.label() == null || holdout.contains(product.sku())) {
                continue;
            }
            boolean dump = CategoryKey.isDump(CategoryKey.of(product.rawCategory()));
            bayes.train(product.label(), Tokens.productFeatures(product, dump, lexicon));
        }

        Map<String, Decision> decisions = new LinkedHashMap<>();
        int withoutTraining = 0;
        int flipped = 0;
        int weak = 0;
        for (var group : groups.entrySet()) {
            Decision decision = decide(
                    group.getKey(),
                    group.getValue(),
                    holdout,
                    bayes,
                    lexicon,
                    families,
                    marginThreshold,
                    packKilograms,
                    packLitres);
            decisions.put(group.getKey(), decision);
            if (decision.labelWinner() == null) {
                withoutTraining++;
            }
            if (decision.flipped()) {
                flipped++;
            }
            if (!decision.weakReason().isEmpty()) {
                weak++;
            }
        }

        List<MappingResult.RawAssignment> rows = new ArrayList<>();
        for (String raw : rawOrder) {
            rows.add(new MappingResult.RawAssignment(raw, decisions.get(rawToKey.get(raw)).categoryId()));
        }
        List<MappingResult.ReportRow> report = new ArrayList<>();
        for (var entry : decisions.entrySet()) {
            Decision decision = entry.getValue();
            report.add(new MappingResult.ReportRow(
                    entry.getKey(),
                    decision.categoryId(),
                    decision.holdoutRevenue(),
                    decision.disagreedRevenue(),
                    decision.margin(),
                    decision.weakReason()));
        }
        report.sort(Comparator.comparingDouble(MappingResult.ReportRow::disagreedRevenue)
                .reversed()
                .thenComparing(MappingResult.ReportRow::normalizedCategory));

        MappingResult.Summary summary = summarize(
                products,
                decisions,
                rows.size(),
                groups.size(),
                labelled.size(),
                training.size(),
                holdout.size(),
                lexicon.learnedCount(),
                weak,
                withoutTraining,
                flipped,
                holdout);
        return new MappingResult(List.copyOf(rows), List.copyOf(report), summary, Set.copyOf(holdout), Set.copyOf(training));
    }

    private static Decision decide(
            String key,
            List<Product> products,
            Set<String> holdout,
            NaiveBayes bayes,
            AbbreviationLexicon lexicon,
            PackFamilies families,
            double marginThreshold,
            double packKilograms,
            double packLitres) {
        boolean dump = CategoryKey.isDump(key);
        Map<String, Double> revenue = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Double> labelRevenue = new HashMap<>();
        Map<String, Integer> labelCounts = new HashMap<>();
        Map<String, Double> holdoutByLabel = new HashMap<>();
        double total = 0;
        double modelRevenue = 0;
        double probabilitySum = 0;
        int modelVotes = 0;
        double holdoutRevenue = 0;

        for (Product product : products) {
            boolean trainingLabel = product.label() != null && !holdout.contains(product.sku());
            String vote;
            if (trainingLabel) {
                vote = product.label();
            } else {
                NaiveBayes.Prediction prediction = bayes.predict(Tokens.productFeatures(product, dump, lexicon));
                vote = prediction.id();
                modelVotes++;
                modelRevenue += product.revenue();
                probabilitySum += prediction.probability();
            }
            vote = families.adjust(vote, PackSize.parse(product.packSize()), packKilograms, packLitres);
            revenue.merge(vote, product.revenue(), Double::sum);
            counts.merge(vote, 1, Integer::sum);
            total += product.revenue();
            if (trainingLabel) {
                labelRevenue.merge(vote, product.revenue(), Double::sum);
                labelCounts.merge(vote, 1, Integer::sum);
            }
            if (product.label() != null && holdout.contains(product.sku())) {
                holdoutRevenue += product.revenue();
                holdoutByLabel.merge(product.label(), product.revenue(), Double::sum);
            }
        }

        String winner = pick(revenue, counts);
        String labelWinner = labelRevenue.isEmpty() ? null : pick(labelRevenue, labelCounts);
        double top = revenue.getOrDefault(winner, 0.0);
        double second = 0;
        for (var entry : revenue.entrySet()) {
            if (!entry.getKey().equals(winner)) {
                second = Math.max(second, entry.getValue());
            }
        }
        double margin = total <= 0.001 ? 0 : Math.max(0, (top - second) / total);
        double disagreed = 0;
        for (var entry : holdoutByLabel.entrySet()) {
            if (!entry.getKey().equals(winner)) {
                disagreed += entry.getValue();
            }
        }
        double averageProbability = modelVotes == 0 ? 1 : probabilitySum / modelVotes;
        double modelShare = total <= 0 ? 0 : modelRevenue / total;
        List<String> reasons = new ArrayList<>();
        if (margin < marginThreshold) {
            reasons.add("low_margin");
        }
        if (dump) {
            reasons.add("dump");
        }
        if (modelVotes > 0 && modelShare > 0.8 && averageProbability < 0.35) {
            reasons.add("model_only");
        }
        return new Decision(
                winner,
                labelWinner,
                labelWinner != null && !labelWinner.equals(winner),
                margin,
                holdoutRevenue,
                disagreed,
                String.join("|", reasons));
    }

    private static MappingResult.Summary summarize(
            List<Product> products,
            Map<String, Decision> decisions,
            int rawCategories,
            int normalizedCategories,
            int labelledSkus,
            int trainingSkus,
            int holdoutSkus,
            int learnedAbbreviations,
            int weakCategories,
            int categoriesWithoutTrainingLabel,
            int categoriesFlippedByModel,
            Set<String> holdout) {
        int holdoutRows = 0;
        int holdoutAgreed = 0;
        int dumpRows = 0;
        int dumpAgreed = 0;
        int cleanRows = 0;
        int cleanAgreed = 0;
        double holdoutRevenue = 0;
        double holdoutAgreedRevenue = 0;
        double dumpRevenue = 0;
        double dumpAgreedRevenue = 0;
        double cleanRevenue = 0;
        double cleanAgreedRevenue = 0;
        double labelOnlyRevenue = 0;
        double labelOnlyAgreedRevenue = 0;

        for (Product product : products) {
            if (product.label() == null || !holdout.contains(product.sku())) {
                continue;
            }
            String key = CategoryKey.of(product.rawCategory());
            Decision decision = decisions.get(key);
            boolean agreed = decision.categoryId().equals(product.label());
            String labelChoice = decision.labelWinner() == null ? decision.categoryId() : decision.labelWinner();
            boolean labelAgreed = labelChoice.equals(product.label());
            boolean dump = CategoryKey.isDump(key);
            holdoutRows++;
            holdoutRevenue += product.revenue();
            labelOnlyRevenue += product.revenue();
            if (agreed) {
                holdoutAgreed++;
                holdoutAgreedRevenue += product.revenue();
            }
            if (labelAgreed) {
                labelOnlyAgreedRevenue += product.revenue();
            }
            if (dump) {
                dumpRows++;
                dumpRevenue += product.revenue();
                if (agreed) {
                    dumpAgreed++;
                    dumpAgreedRevenue += product.revenue();
                }
            } else {
                cleanRows++;
                cleanRevenue += product.revenue();
                if (agreed) {
                    cleanAgreed++;
                    cleanAgreedRevenue += product.revenue();
                }
            }
        }

        return new MappingResult.Summary(
                rawCategories,
                normalizedCategories,
                labelledSkus,
                trainingSkus,
                holdoutSkus,
                learnedAbbreviations,
                rate(holdoutAgreed, holdoutRows),
                rate(holdoutAgreedRevenue, holdoutRevenue),
                rate(cleanAgreed, cleanRows),
                rate(cleanAgreedRevenue, cleanRevenue),
                rate(dumpAgreed, dumpRows),
                rate(dumpAgreedRevenue, dumpRevenue),
                weakCategories,
                categoriesWithoutTrainingLabel,
                categoriesFlippedByModel,
                rate(labelOnlyAgreedRevenue, labelOnlyRevenue));
    }

    private static String pick(Map<String, Double> revenue, Map<String, Integer> counts) {
        String best = null;
        double bestRevenue = Double.NEGATIVE_INFINITY;
        int bestCount = -1;
        for (var entry : revenue.entrySet()) {
            String id = entry.getKey();
            double money = entry.getValue();
            int count = counts.getOrDefault(id, 0);
            boolean higherMoney = money > bestRevenue + 0.001;
            boolean sameMoney = Math.abs(money - bestRevenue) <= 0.001;
            boolean better = best == null
                    || higherMoney
                    || (sameMoney && count > bestCount)
                    || (sameMoney && count == bestCount && NaiveBayes.idLess(id, best));
            if (better) {
                best = id;
                bestRevenue = money;
                bestCount = count;
            }
        }
        if (best == null) {
            throw new IllegalStateException("A supplier category produced no votes");
        }
        return best;
    }

    private static double rate(double part, double whole) {
        return whole == 0 ? 0 : part / whole;
    }

    private record Decision(
            String categoryId,
            String labelWinner,
            boolean flipped,
            double margin,
            double holdoutRevenue,
            double disagreedRevenue,
            String weakReason
    ) {
    }
}
