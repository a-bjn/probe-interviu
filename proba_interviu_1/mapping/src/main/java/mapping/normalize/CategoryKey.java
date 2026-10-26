package mapping.normalize;

import java.util.Locale;
import java.util.regex.Pattern;

public final class CategoryKey {
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static final Pattern DUMP = Pattern.compile(
            "(^|[^a-z])(miscellaneous|misc|assorted|feature space|launches|new lines|new line|new in|new products|pending)([^a-z]|$)");

    private CategoryKey() {
    }

    public static String of(String raw) {
        if (raw == null) {
            return "";
        }
        return SPACE.matcher(raw.trim().toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    public static boolean isDump(String normalized) {
        return normalized != null && DUMP.matcher(normalized).find();
    }
}
