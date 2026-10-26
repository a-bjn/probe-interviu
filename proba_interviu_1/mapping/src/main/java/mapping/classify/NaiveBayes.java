package mapping.classify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NaiveBayes {
    private final Map<String, Integer> documents = new HashMap<>();
    private final Map<String, Map<String, Integer>> counts = new HashMap<>();
    private final Map<String, Integer> totals = new HashMap<>();
    private final Map<String, Integer> vocabulary = new HashMap<>();
    private int documentTotal;

    public void train(String classId, List<String> features) {
        documents.merge(classId, 1, Integer::sum);
        documentTotal++;
        Map<String, Integer> bag = counts.computeIfAbsent(classId, key -> new HashMap<>());
        int seen = 0;
        for (String feature : features) {
            bag.merge(feature, 1, Integer::sum);
            vocabulary.putIfAbsent(feature, vocabulary.size());
            seen++;
        }
        totals.merge(classId, seen, Integer::sum);
    }

    public Prediction predict(List<String> features) {
        if (documents.isEmpty()) {
            throw new IllegalStateException("The classifier has no training data");
        }
        int vocabularySize = Math.max(vocabulary.size(), 1);
        String bestId = null;
        double bestLog = Double.NEGATIVE_INFINITY;
        double maxLog = Double.NEGATIVE_INFINITY;
        Map<String, Double> logs = new HashMap<>();
        for (var entry : documents.entrySet()) {
            String classId = entry.getKey();
            double log = Math.log(entry.getValue() / (double) documentTotal);
            int total = totals.getOrDefault(classId, 0);
            Map<String, Integer> bag = counts.getOrDefault(classId, Map.of());
            double denominator = total + vocabularySize;
            for (String feature : features) {
                int count = bag.getOrDefault(feature, 0);
                log += Math.log((count + 1.0) / denominator);
            }
            logs.put(classId, log);
            if (log > maxLog) {
                maxLog = log;
            }
            if (bestId == null || log > bestLog + 1e-9 || (Math.abs(log - bestLog) <= 1e-9 && idLess(classId, bestId))) {
                bestId = classId;
                bestLog = log;
            }
        }
        double sum = 0;
        for (double log : logs.values()) {
            sum += Math.exp(log - maxLog);
        }
        double probability = Math.exp(bestLog - maxLog) / sum;
        return new Prediction(bestId, probability);
    }

    public static boolean idLess(String left, String right) {
        try {
            return Integer.parseInt(left) < Integer.parseInt(right);
        } catch (NumberFormatException ex) {
            return left.compareTo(right) < 0;
        }
    }

    public record Prediction(String id, double probability) {
    }
}
