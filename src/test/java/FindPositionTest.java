import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class FindPositionTest {
    @Test @Timeout(5)
    void largeIdenticalArray() {
        int[] values = new int[1_000_000];
        Arrays.fill(values, 7);
        assertEquals(7, new FindPosition().find(values, values.length / 2));
    }

    @Test @Timeout(5)
    void largeOrderedArrays() {
        int size = 1_000_000;
        for (boolean reverse : new boolean[]{false, true}) {
            int[] values = new int[size];
            for (int i = 0; i < size; i++) values[i] = reverse ? size - i - 1 : i;
            assertEquals(size / 2, new FindPosition().find(values, size / 2));
        }
    }

    @Test void ranksMatchSortingIncludingDuplicatesAndExtremeValues() {
        Random random = new Random(20261024);
        for (int trial = 0; trial < 100; trial++) {
            int[] values = new int[1 + random.nextInt(100)];
            for (int i = 0; i < values.length; i++) values[i] = random.nextInt(15) - 7;
            values[0] = Integer.MIN_VALUE;
            values[values.length - 1] = Integer.MAX_VALUE;
            int[] sorted = values.clone();
            Arrays.sort(sorted);
            for (int rank = 0; rank < values.length; rank++) {
                int[] actual = values.clone();
                assertEquals(sorted[rank], new FindPosition().find(actual, rank));
                Arrays.sort(actual);
                assertArrayEquals(sorted, actual);
            }
        }
    }

    @Test void invalidRanksAreRejected() {
        FindPosition finder = new FindPosition();
        assertThrows(IllegalArgumentException.class, () -> finder.find(new int[0], 0));
        assertThrows(IllegalArgumentException.class, () -> finder.find(new int[]{1}, -1));
        assertThrows(IllegalArgumentException.class, () -> finder.find(new int[]{1}, 1));
        assertThrows(NullPointerException.class, () -> finder.find(null, 0));
    }
}
