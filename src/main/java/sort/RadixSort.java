package sort;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

public class RadixSort {
    public static void main(String[] args) throws Exception {
        int[] array = SortInput.read(args);

        RadixSort sss = new RadixSort();
        long time = System.nanoTime();
        sss.radixSort(array, 10);
        long time1 = System.nanoTime();
        System.out.println(time + "    " + time1 + "\t\t" + (time1 - time));
//		for (int te : array) {
//			System.out.println(te + "\t");
//		}
//		System.out.println();
    }

    /** Stable decimal LSD sort supporting the full signed int range. */
    public void radixSort(int[] array, int maxbit) {
        java.util.Objects.requireNonNull(array, "array");
        if (maxbit < 1 || maxbit > 10) {
            throw new IllegalArgumentException("maxbit must be between 1 and 10");
        }
        long max = 0;
        for (int value : array) max = Math.max(max, Math.abs((long) value));
        long capacity = 1;
        for (int i = 0; i < maxbit; i++) capacity *= 10;
        if (max >= capacity) throw new IllegalArgumentException("maxbit cannot represent input values");
        int[] output = new int[array.length];
        for (long divisor = 1; divisor <= max; divisor *= 10) {
            int[] counts = new int[19]; // signed digits -9 through 9
            for (int value : array) counts[(int) ((value / divisor) % 10) + 9]++;
            for (int i = 1; i < counts.length; i++) counts[i] += counts[i - 1];
            for (int i = array.length - 1; i >= 0; i--) {
                int bucket = (int) ((array[i] / divisor) % 10) + 9;
                output[--counts[bucket]] = array[i];
            }
            System.arraycopy(output, 0, array, 0, array.length);
        }
    }

    public void toArray(int[][] array, int[] position, int a[]) {
        int k = 0;
        for (int i = 0; i < position.length; i++) {
            for (int j = 0; j < position[i]; j++) {
                a[k] = array[i][j];
                k++;
            }
        }
    }
}
