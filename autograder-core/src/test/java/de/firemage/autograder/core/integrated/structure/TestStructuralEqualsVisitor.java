package de.firemage.autograder.core.integrated.structure;

import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.file.TempLocation;
import de.firemage.autograder.core.file.UploadedFile;
import de.firemage.autograder.core.integrated.DuplicateCodeFinder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.path.CtRole;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TestStructuralEqualsVisitor {
    @SuppressWarnings("unchecked")
    private static <T extends CtStatement> T createStatement(String statement, String arguments) {
        if (arguments == null) {
            arguments = "";
        }

        UploadedFile file;
        try {
            file = UploadedFile.build(StringSourceInfo.fromSourceString(JavaVersion.JAVA_17, "Test", "public class Test { void t(%s) { %s; } }".formatted(
                arguments,
                statement
            )), TempLocation.random(), y -> {}, null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        CtModel ctModel = file.getModel().getModel();

        CtMethod<?> ctMethod = new ArrayList<>(ctModel.getAllTypes()).get(0).getMethodsByName("t").get(0);

        return (T) ctMethod.getBody().getStatements().get(0);
    }

    private static CtRole parseMismatch(String mismatch) {
        return mismatch == null || mismatch.isBlank() ? null : CtRole.valueOf(mismatch);
    }

    private static void checkStructurallyEqual(CtElement left, CtElement right, CtRole mismatch) {
        StructuralEqualsVisitor visitor = new StructuralEqualsVisitor(DuplicateCodeFinder.ALLOWED_DIFFERENCE);
        boolean isEqual = visitor.checkEquals(left, right);

        if (mismatch == null) {
            assertTrue(isEqual, "\"%s\" != \"%s\". Mismatch (%s): left: %s, right: %s".formatted(
                left,
                right,
                visitor.getNotEqualRole(),
                visitor.getNotEqualElement(),
                visitor.getNotEqualOther()
            ));
        } else {
            assertFalse(isEqual, "\"%s\" == \"%s\"".formatted(left, right));
            assertEquals(mismatch, visitor.getNotEqualRole());
        }
    }

    private static void assertStructurallyEqual(String arguments, String left, String right, CtRole mismatch) {
        CtStatement leftStatement = createStatement(left, arguments);
        CtStatement rightStatement = createStatement(right, arguments);

        // for equality the following must hold:
        // 1. if left == right, then right == left
        // 2. if left == other and left == right, then right == other
        // 3. left == left and right == right

        // we only test 1 and 3 here

        checkStructurallyEqual(leftStatement, rightStatement, mismatch);
        checkStructurallyEqual(rightStatement, leftStatement, mismatch);

        checkStructurallyEqual(leftStatement, leftStatement, null);
        checkStructurallyEqual(rightStatement, rightStatement, null);

    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Arguments    | Left             | Right          | Mismatch      ",
            " int a        | /**/ a = 1       | /**/ a = 1     |               ",
            " int a        | /**/ a = 1       | a = 1          |               ",
            " int a        | /* a */ a = 1    | /* b */ a = 1  |               ",
        }
    )
    void testIgnoresComments(String arguments, String left, String right, String mismatch) {
        assertStructurallyEqual(arguments, left, right, parseMismatch(mismatch));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Arguments           | Left                              | Right                                               | Mismatch  ",
            " int a, int b        | a = 1                             | b = 1                                               | NAME      ",
            "                     | int a;                            | int b;                                              |           ",
            "                     | record Position(int x, int y) {}  | record Position(int horizontal, int vertical) {}    | NAME      ",
            "                     | record Position(int x, int y) {}  | record Coordinate(int horizontal, int vertical) {}  | NAME      ",
            "                     | class Position { int x; int y; }  | class Position { int horizontal; int vertical; }    |           ",
        }
    )
    void testDifferentlyNamedVariables(String arguments, String left, String right, String mismatch) {
        assertStructurallyEqual(arguments, left, right, parseMismatch(mismatch));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        useHeadersInDisplayName = true,
        value = {
            " Arguments           | Left                                        | Right                                       | Mismatch  ",
            " char i, int key     | char res = (char) (((i - 65 + (26 - key)) % 26) + 65);  | char res = (char) (((i - 65 + key) % 26) + 65);  |       ",
        }
    )
    void testBinaryOperatorConstExpr(String arguments, String left, String right, String mismatch) {
        assertStructurallyEqual(arguments, left, right, parseMismatch(mismatch));
    }
}
