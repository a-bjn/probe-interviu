package mapping.classify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class Holdout {
    private Holdout() {
    }

    public static Set<String> select(Map<String, String> skuToLabel, long seed, double fraction) {
        Map<String, List<String>> byClass = new HashMap<>();
        skuToLabel.forEach((sku, label) -> byClass.computeIfAbsent(label, key -> new ArrayList<>()).add(sku));
        Set<String> holdout = new HashSet<>();
        for (var entry : byClass.entrySet()) {
            List<String> skus = new ArrayList<>(entry.getValue());
            Collections.sort(skus);
            Collections.shuffle(skus, new Random(mix(seed, entry.getKey())));
            int take = (int) Math.round(skus.size() * fraction);
            if (skus.size() <= 1) {
                take = 0;
            } else if (take >= skus.size()) {
                take = skus.size() - 1;
            }
            holdout.addAll(skus.subList(0, Math.max(take, 0)));
        }
        return Set.copyOf(holdout);
    }

    private static long mix(long seed, String label) {
        long mixed = seed;
        for (int i = 0; i < label.length(); i++) {
            mixed = mixed * 31 + label.charAt(i);
        }
        return mixed;
    }
}
