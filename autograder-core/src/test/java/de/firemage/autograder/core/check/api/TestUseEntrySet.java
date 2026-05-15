package de.firemage.autograder.core.check.api;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUseEntrySet extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.USE_ENTRY_SET);

    private void assertShouldUseEntrySet(Problem problem, String original, String suggestion) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    "suggest-replacement",
                    Map.of(
                        "original",
                        original,
                        "suggestion",
                        suggestion
                ))
            ),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testUsesKeySetToGetValues() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.*;
                
                enum Color { RED, GREEN, BLUE; }
                
                public class Test {
                    public static void main(String[] args) {
                        Map<Color, Integer> storedColors = new HashMap<>();
                        Map<Color, Integer> result = new HashMap<>();
                
                        for (Color color : storedColors.keySet()) { /*# not ok #*/
                            if (storedColors.get(color) > 0) {
                                result.put(color, storedColors.get(color));
                            }
                            System.out.println(storedColors.get(color));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertShouldUseEntrySet(problems.next(), "storedColors.keySet()", "storedColors.entrySet()");
        problems.assertExhausted();
    }

    // Test taken from https://github.com/SonarSource/sonar-java/blob/4c1473548b7e2e9fe1da212955f532c6fbc64d18/java-checks-test-sources/src/main/java/checks/KeySetInsteadOfEntrySet.java#L3
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            "   Iterable                | Suggestion                    | Accesses                                              ",
            " list                      |                               |                                                       ",
            " list()                    |                               |                                                       ",
            " new String[] {}           |                               |                                                       ",
            " map.keySet()              |                               | map.get(key); map.get(getKey()); map2.get(mapKey);    ",
            " this.map3[0].keySet()     | this.map3[0].entrySet()       | this.map3[0].get(mapKey)                              ",
            " inner.inner.map.keySet()  | inner.inner.map.entrySet()    | inner.inner.map.get(mapKey)                           ",
            " keySet()                  |                               | map3[0].get(mapKey)                                   ",
            " super.inner.map.keySet()  | super.inner.map.entrySet()    | super.inner.map.get(mapKey)                           ",
            " this.inner.map.keySet()   | this.inner.map.entrySet()     | this.inner.map.get(mapKey)                            ",
            " map.entrySet()            |                               | map.get(mapKey)                                       ",
            " keySet()                  | entrySet()                    | get(mapKey)                                           ",
            " this.keySet()             | this.entrySet()               | this.get(mapKey)                                      ",
            " super.keySet()            | super.entrySet()              | super.get(mapKey)                                     ",
            " map.keySet()              | map.entrySet()                | map.get(mapKey)                                       ",
            " super.map.keySet()        | super.map.entrySet()          | super.map.get(mapKey)                                 ",
            " super.map.keySet()        |                               | this.map.get(mapKey)                                  ",
            " this.map.keySet()         | this.map.entrySet()           | this.map.get(mapKey)                                  ",
        }
    )
    void testParametric(String iterable, String suggestion, String accesses) throws LinterException, IOException {
        List<String> body = List.of();
        if (accesses != null) {
            body = Arrays.stream(accesses.split(";", -1)).map("System.out.println(%s);%n"::formatted).toList();
        }

        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.*;

                class KeySetInsteadOfEntrySetCheck extends java.util.HashMap<String, Object> {
                    KeySetInsteadOfEntrySetCheck inner;
                    java.util.HashMap<String, Object> map;
                }

                class Test extends KeySetInsteadOfEntrySetCheck {
                    public class InnerClass {
                        InnerClass inner;
                        java.util.HashMap<String, Object> map;
                    }

                    private String key;
                    private java.util.List<String> list;
                    java.util.HashMap<String, Object> map, map2;
                    java.util.HashMap<String, Object>[] map3;

                    InnerClass inner;

                    private String getKey() {
                        return key;
                    }
                    private java.util.List<String> list() {
                        return list;
                    }

                    public void method() {
                        for (var mapKey : %s) {
                            %s
                            %s
                            %s
                        }
                    }
                }
                """.formatted(iterable, String.join("", body), String.join("", body), String.join("", body))
        ), PROBLEM_TYPES);

        if (suggestion != null) {
            assertShouldUseEntrySet(problems.next(), iterable, suggestion);
        }
        problems.assertExhausted();
    }

    @Test
    void testResolvesGeneric() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            "Test",
            """
                import java.util.Map;
                import java.util.HashMap;

                public class Test {
                    private static <T extends Map<String, String>> void execute(T map) {
                        for (var mapKey : map.keySet()) {
                            System.out.println(map.get(mapKey));
                            System.out.println(map.get(mapKey));
                            System.out.println(map.get(mapKey));
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertShouldUseEntrySet(problems.next(), "map.keySet()", "map.entrySet()");

        problems.assertExhausted();
    }
}
