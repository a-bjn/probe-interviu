package mapping.classify;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class Tokens {
    static final AbbreviationLexicon IDENTITY = new AbbreviationLexicon(Map.of(), 0);
    private static final Pattern SPLIT = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> STOP = Set.of(
            "the", "and", "of", "with", "for", "a", "an", "in", "to", "or", "on", "by", "at", "from");
    private static final Set<String> GENERIC = Set.of(
            "packs", "pack", "catering", "retail", "under", "over", "food", "kg", "kilogram", "kilograms",
            "litre", "litres", "liter", "liters");
    private static final int LEAF_REPEATS = 6;

    private Tokens() {
    }

    public static List<String> words(String text, AbbreviationLexicon lexicon) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] parts = SPLIT.split(text.toLowerCase(Locale.ROOT));
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            String expanded = lexicon.expand(part);
            for (String piece : expanded.split(" ")) {
                if (expanded.equals(part) && part.length() < 3) {
                    continue;
                }
                if (piece.length() < 3 || STOP.contains(piece) || piece.chars().allMatch(Character::isDigit)) {
                    continue;
                }
                words.add(piece);
            }
        }
        return words;
    }

    public static List<String> productFeatures(mapping.ingest.Product product, boolean dump, AbbreviationLexicon lexicon) {
        List<String> features = new ArrayList<>();
        addField(features, product.name(), lexicon);
        addField(features, product.name(), lexicon);
        addField(features, product.brand(), lexicon);
        if (!dump) {
            addField(features, product.rawCategory(), lexicon);
        }
        return features;
    }

    public static List<String> pathPrototype(String path, AbbreviationLexicon lexicon) {
        String[] parts = path.split(">");
        String leaf = parts[parts.length - 1];
        List<String> leafWords = withoutGeneric(words(leaf, lexicon));
        List<String> allWords = withoutGeneric(words(path, lexicon));
        List<String> features = new ArrayList<>();
        for (int repeat = 0; repeat < LEAF_REPEATS; repeat++) {
            features.addAll(leafWords);
        }
        features.addAll(allWords);
        for (int i = 0; i + 1 < leafWords.size(); i++) {
            features.add(leafWords.get(i) + "_" + leafWords.get(i + 1));
        }
        return features;
    }

    private static void addField(List<String> features, String text, AbbreviationLexicon lexicon) {
        List<String> field = words(text, lexicon);
        features.addAll(field);
        for (int i = 0; i + 1 < field.size(); i++) {
            features.add(field.get(i) + "_" + field.get(i + 1));
        }
    }

    private static List<String> withoutGeneric(List<String> words) {
        List<String> kept = new ArrayList<>();
        for (String word : words) {
            if (!GENERIC.contains(word)) {
                kept.add(word);
            }
        }
        return kept;
    }
}
