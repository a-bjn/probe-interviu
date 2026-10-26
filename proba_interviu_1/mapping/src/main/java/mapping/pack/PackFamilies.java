package mapping.pack;

import mapping.ingest.OurCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PackFamilies {
    private final Map<String, Family> byId;

    private PackFamilies(Map<String, Family> byId) {
        this.byId = byId;
    }

    public static PackFamilies from(List<OurCategory> categories) {
        Map<Kind, String> catering = new HashMap<>();
        Map<Kind, String> retail = new HashMap<>();
        for (OurCategory category : categories) {
            String path = category.path().toLowerCase(Locale.ROOT);
            boolean cateringPack = path.contains("catering packs");
            boolean retailPack = path.contains("retail packs");
            if (!cateringPack && !retailPack) {
                continue;
            }
            Kind kind = kindOf(path);
            if (kind == null) {
                continue;
            }
            if (cateringPack) {
                catering.put(kind, category.id());
            } else {
                retail.put(kind, category.id());
            }
        }
        Map<String, Family> byId = new HashMap<>();
        for (Kind kind : Kind.values()) {
            String cateringId = catering.get(kind);
            String retailId = retail.get(kind);
            if (cateringId == null || retailId == null) {
                continue;
            }
            Family family = new Family(cateringId, retailId, kind == Kind.OIL);
            byId.put(cateringId, family);
            byId.put(retailId, family);
        }
        return new PackFamilies(byId);
    }

    public String adjust(String categoryId, PackSize pack, double kilogramThreshold, double litreThreshold) {
        Family family = byId.get(categoryId);
        if (family == null) {
            return categoryId;
        }
        if (family.oil()) {
            if (pack.litres() == null) {
                return categoryId;
            }
            return pack.litres() >= litreThreshold ? family.cateringId() : family.retailId();
        }
        if (pack.kilograms() == null) {
            return categoryId;
        }
        return pack.kilograms() >= kilogramThreshold ? family.cateringId() : family.retailId();
    }

    private static Kind kindOf(String path) {
        if (path.contains("flour")) {
            return Kind.FLOUR;
        }
        if (path.contains("sugar")) {
            return Kind.SUGAR;
        }
        if (path.contains("dried pasta")) {
            return Kind.PASTA;
        }
        if (path.contains("cooking oil")) {
            return Kind.OIL;
        }
        if (path.contains("rice,")) {
            return Kind.RICE;
        }
        return null;
    }

    private enum Kind {
        FLOUR, SUGAR, PASTA, RICE, OIL
    }

    private record Family(String cateringId, String retailId, boolean oil) {
    }
}
