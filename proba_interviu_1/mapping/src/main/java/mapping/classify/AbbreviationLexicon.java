package mapping.classify;

import mapping.ingest.Product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AbbreviationLexicon {
    static final Map<String, String> SEED = Map.of(
            "tns", "tinned",
            "wn", "wine",
            "chs", "cheese",
            "veg", "vegetables",
            "vgtbl", "vegetables");

    private final Map<String, String> expansions;
    private final int learnedCount;

    AbbreviationLexicon(Map<String, String> expansions, int learnedCount) {
        this.expansions = expansions;
        this.learnedCount = learnedCount;
    }

    public String expand(String token) {
        return expansions.getOrDefault(token, token);
    }

    public int learnedCount() {
        return learnedCount;
    }

    public static AbbreviationLexicon learn(
            Map<String, List<Product>> groups,
            java.util.Set<String> holdout,
            Map<String, String> pathsById) {
        Map<String, Map<String, Integer>> votes = new HashMap<>();
        for (var group : groups.entrySet()) {
            Map<String, Integer> labels = new HashMap<>();
            int training = 0;
            for (Product product : group.getValue()) {
                if (product.label() == null || holdout.contains(product.sku())) {
                    continue;
                }
                labels.merge(product.label(), 1, Integer::sum);
                training++;
            }
            if (labels.size() != 1 || training < 3) {
                continue;
            }
            String label = labels.keySet().iterator().next();
            String path = pathsById.get(label);
            if (path == null) {
                continue;
            }
            List<String> pathTokens = Tokens.words(path, Tokens.IDENTITY);
            List<String> nameTokens = Tokens.words(group.getKey(), Tokens.IDENTITY);
            for (String abbreviation : nameTokens) {
                if (abbreviation.length() < 3 || abbreviation.length() > 5 || SEED.containsKey(abbreviation)) {
                    continue;
                }
                for (String pathToken : pathTokens) {
                    if (pathToken.length() <= abbreviation.length() || pathToken.length() > abbreviation.length() + 8) {
                        continue;
                    }
                    if (pathToken.startsWith(abbreviation)) {
                        votes.computeIfAbsent(abbreviation, key -> new HashMap<>()).merge(pathToken, training, Integer::sum);
                    }
                }
            }
        }
        Map<String, String> learned = new HashMap<>();
        for (var entry : votes.entrySet()) {
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            String best = null;
            int bestCount = 0;
            for (var option : entry.getValue().entrySet()) {
                if (option.getValue() > bestCount) {
                    best = option.getKey();
                    bestCount = option.getValue();
                }
            }
            if (best != null && bestCount >= 2 && bestCount / (double) total >= 0.6) {
                learned.put(entry.getKey(), best);
            }
        }
        Map<String, String> merged = new HashMap<>(learned);
        merged.putAll(SEED);
        return new AbbreviationLexicon(Map.copyOf(merged), learned.size());
    }
}
