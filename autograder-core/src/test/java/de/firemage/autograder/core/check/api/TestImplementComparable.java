package de.firemage.autograder.core.check.api;

import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestImplementComparable extends AbstractCheckTest {
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.IMPLEMENT_COMPARABLE);

    void assertImplement(Problem problem, String name) {
        assertEquals(
            this.linter.translateMessage(new LocalizedMessage(
                "implement-comparable",
                Map.of("name", name)
            )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testMotivation() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "AccountComparator",
                """
                    import java.util.Comparator;
                    
                    public class AccountComparator implements Comparator<Account> {
                        public int compare(Account left, Account right) {
                            return Integer.compare(left.getId(), right.getId());
                        }
                    }
                    """,
                "Account",
                """
                    public class Account {
                        private int id;
                        private int balance;
                        
                        public Account(int id, int balance) {
                            this.id = id;
                            this.balance = balance;
                        }
                        
                        public int getId() {
                            return id;
                        }
                        
                        public int getBalance() {
                            return balance;
                        }
                    }
                    """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testForeignType() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "LocaleComparator",
                """
                    import java.util.Locale;
                    import java.util.Comparator;
                    
                    public class LocaleComparator implements Comparator<Locale> {
                        public int compare(Locale left, Locale right) {
                            return left.toString().compareTo(right.toString());
                        }
                    }
                    """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testHasCompareTo() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "AccountComparator",
                """
                    import java.util.Comparator;
                    
                    public class AccountComparator implements Comparator<Account> {
                        public int compare(Account left, Account right) {
                            return Integer.compare(left.getId(), right.getId());
                        }
                    }
                    """,
                "Account",
                """
                    public class Account implements Comparable<Account> {
                        private int id;
                        private int balance;
                        
                        public Account(int id, int balance) {
                            this.id = id;
                            this.balance = balance;
                        }
                        
                        public int getId() {
                            return id;
                        }
                        
                        public int getBalance() {
                            return balance;
                        }
                        
                        public int compareTo(Account other) {
                            return 0;
                        }
                    }
                    """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testMultipleComparator() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "AccountIdComparator",
                """
                    import java.util.Comparator;
                    
                    public class AccountIdComparator implements Comparator<Account> {
                        public int compare(Account left, Account right) {
                            return Integer.compare(left.getId(), right.getId());
                        }
                    }
                    """,
                "AccountBalanceComparator",
                """
                    import java.util.Comparator;
                    
                    public class AccountBalanceComparator implements Comparator<Account> {
                        public int compare(Account left, Account right) {
                            return Integer.compare(left.getBalance(), right.getBalance());
                        }
                    }
                    """,
                "Account",
                """
                    public class Account {
                        private int id;
                        private int balance;
                        
                        public Account(int id, int balance) {
                            this.id = id;
                            this.balance = balance;
                        }
                        
                        public int getId() {
                            return id;
                        }
                        
                        public int getBalance() {
                            return balance;
                        }
                    }
                    """
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testImplementsComparatorForItself() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            Map.of(
                "AccountIdComparator",
                """
                    import java.util.Comparator;
                    
                    public class AccountIdComparator implements Comparator<Account> {
                        public int compare(Account left, Account right) {
                            return Integer.compare(left.getId(), right.getId());
                        }
                    }
                    """,
                "Account",
                """
                    import java.util.Comparator;

                    public class Account implements Comparator<Account> {
                        private int id;
                        private int balance;
                        
                        public Account(int id, int balance) {
                            this.id = id;
                            this.balance = balance;
                        }
                        
                        public int getId() {
                            return id;
                        }
                        
                        public int getBalance() {
                            return balance;
                        }

                        public int compare(Account left, Account right) {
                            return Integer.compare(left.getBalance(), right.getBalance());
                        }
                    }
                    """
            )
        ), PROBLEM_TYPES);

        assertImplement(problems.next(), "Account");

        problems.assertExhausted();
    }
}


