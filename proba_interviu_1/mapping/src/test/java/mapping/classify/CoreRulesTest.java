package mapping.classify;

import mapping.ingest.Product;
import mapping.normalize.CategoryKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRulesTest {
    @Test
    void normalizationKeepsTheRawStringAlone() {
        assertEquals(CategoryKey.of("Pls/Pulses.gr "), CategoryKey.of("PLS/pulses.gr"));
        assertEquals("pls/pulses.gr", CategoryKey.of("  Pls/Pulses.gr  "));
        assertFalse("Pls/Pulses.gr ".equals(CategoryKey.of("Pls/Pulses.gr ")));
        assertTrue(CategoryKey.isDump("misc"));
        assertTrue(CategoryKey.isDump("feature space"));
        assertTrue(CategoryKey.isDump("new lines"));
        assertFalse(CategoryKey.isDump("new potatoes"));
        assertFalse(CategoryKey.isDump("tinned tomatoes"));
    }

    @Test
    void holdoutIsStableAndKeepsATrainer() {
        Map<String, String> labels = Map.of(
                "only", "9",
                "a1", "1",
                "a2", "1",
                "a3", "1",
                "a4", "1",
                "a5", "1");
        Set<String> first = Holdout.select(labels, 19, 0.2);
        assertEquals(first, Holdout.select(labels, 19, 0.2));
        assertFalse(first.contains("only"));
        assertEquals(1, first.size());
        assertTrue(Set.of("a1", "a2", "a3", "a4", "a5").containsAll(first));
    }

    @Test
    void naiveBayesPrefersTheObviousNameAndTheLowerIdOnATie() {
        NaiveBayes bayes = new NaiveBayes();
        bayes.train("cat", List.of("tuna", "fish"));
        bayes.train("cat", List.of("tuna", "fish"));
        bayes.train("dog", List.of("dog", "food"));
        bayes.train("dog", List.of("dog", "food"));
        assertEquals("cat", bayes.predict(List.of("tuna")).id());

        NaiveBayes tie = new NaiveBayes();
        tie.train("20", List.of("apple"));
        tie.train("5", List.of("apple"));
        assertEquals("5", tie.predict(List.of("apple")).id());
    }

    @Test
    void seedAbbreviationsAreFixedAndPrefixesCanBeLearned() {
        AbbreviationLexicon seed = AbbreviationLexicon.learn(Map.of(), Set.of(), Map.of());
        assertEquals("tinned", seed.expand("tns"));
        assertEquals("wine", seed.expand("wn"));
        assertEquals("cheese", seed.expand("chs"));
        assertEquals("vegetables", seed.expand("veg"));
        assertEquals(0, seed.learnedCount());

        Product one = new Product("s1", "Grai bowls", "quinoa", "", "1 kg", 1, "9228");
        Product two = new Product("s2", "Grai bowls", "lentils", "", "1 kg", 1, "9228");
        Product three = new Product("s3", "Grai bowls", "barley", "", "1 kg", 1, "9228");
        AbbreviationLexicon learned = AbbreviationLexicon.learn(
                Map.of("grai bowls", List.of(one, two, three)),
                Set.of(),
                Map.of("9228", "Food cupboard > Pulses & grains > Pulses & grains"));
        assertEquals("grains", learned.expand("grai"));
        assertEquals(1, learned.learnedCount());
    }
}
