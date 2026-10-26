package mapping.pack;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PackSize(Double kilograms, Double litres) {
    private static final Pattern MULTI = Pattern.compile(
            "(?<q1>\\d+(?:\\.\\d+)?)x(?<q2>\\d+(?:\\.\\d+)?)(?<unit>kilograms|kilogram|kgs|kg|grams|gram|gr|g|litres|litre|liters|liter|ltr|ml|cl|l)");
    private static final Pattern SINGLE = Pattern.compile(
            "(?<q>\\d+(?:\\.\\d+)?)(?<unit>kilograms|kilogram|kgs|kg|grams|gram|gr|g|litres|litre|liters|liter|ltr|ml|cl|l)");

    public static PackSize unknown() {
        return new PackSize(null, null);
    }

    public static PackSize parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return unknown();
        }
        String text = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        text = text.replaceAll("(?<=\\d),(?=\\d)", ".");
        Matcher multi = MULTI.matcher(text);
        if (multi.find()) {
            double amount = Double.parseDouble(multi.group("q1")) * Double.parseDouble(multi.group("q2"));
            return fromUnit(amount, multi.group("unit"));
        }
        Matcher single = SINGLE.matcher(text);
        if (single.find()) {
            return fromUnit(Double.parseDouble(single.group("q")), single.group("unit"));
        }
        return unknown();
    }

    private static PackSize fromUnit(double amount, String unit) {
        return switch (unit) {
            case "kg", "kgs", "kilogram", "kilograms" -> new PackSize(amount, null);
            case "g", "gr", "gram", "grams" -> new PackSize(amount / 1000.0, null);
            case "l", "ltr", "litre", "litres", "liter", "liters" -> new PackSize(null, amount);
            case "ml" -> new PackSize(null, amount / 1000.0);
            case "cl" -> new PackSize(null, amount / 100.0);
            default -> unknown();
        };
    }
}
