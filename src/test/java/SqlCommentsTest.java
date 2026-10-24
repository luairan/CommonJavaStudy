import org.junit.jupiter.api.Test;
import util.SQLFormater;

import static org.junit.jupiter.api.Assertions.*;

class SqlCommentsTest {
    private final SQLFormater formatter = new SQLFormater();

    @Test void lineCommentDoesNotConsumeFollowingStatement() {
        String formatted = formatter.format("select 1 -- from 'unmatched (\nfrom example");
        assertTrue(formatted.contains("-- from 'unmatched (\n"));
        assertTrue(formatted.substring(formatted.indexOf("--")).matches("(?s)--[^\\n]*\\n.*from.*example.*"));
    }

    @Test void blockCommentPreservesContentsAndIgnoresSqlSyntax() {
        String comment = "/* select 'unmatched )\n from -- text */";
        String formatted = formatter.format("select " + comment + " 1 from example");
        assertTrue(formatted.contains(comment));
        assertTrue(formatted.endsWith("example"));
    }

    @Test void commentMarkersInsideQuotesRemainLiteral() {
        for (String quoted : new String[]{"'-- text /*'", "\"-- column /*\"", "`/* column --`", "[/* column --]"}) {
            assertTrue(formatter.format("select " + quoted + " from example").contains(quoted));
        }
    }

    @Test void lineCommentsSupportCrLfAndEndOfInput() {
        assertTrue(formatter.format("select 1 -- comment\r\nfrom example").contains("from"));
        assertTrue(formatter.format("select 1 -- trailing ' (").contains("-- trailing ' ("));
    }

    @Test void unterminatedBlockCommentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> formatter.format("select 1 /* unfinished"));
    }

    @Test void operatorsAndAdjacentCommentsRemainDistinct() {
        String formatted = formatter.format("select 8/2-1,/* one *//* two */3 from example");
        assertTrue(formatted.contains("8/2-1"));
        assertTrue(formatted.contains("/* one *//* two */"));
    }
}
