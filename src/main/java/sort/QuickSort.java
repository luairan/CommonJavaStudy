package sort;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class QuickSort {
    public static void main(String[] args) throws Exception {
//		QuickSort qs = new QuickSort();
//		int[] array = { 4, 3, 1, 2, 6, 4, 7 };
//		qs.qSort(array, 0, array.length - 1);
//		for (int temp : array) {
//			System.out.print(temp + "\t");
//		}
//		System.out.println();


        int[] array = SortInput.read(args);

        QuickSort qs = new QuickSort();
        long time = System.nanoTime();
        qs.qSort(array, 0, array.length - 1);
        long time1 = System.nanoTime();
        System.out.println(time + "    " + time1 + "\t\t" + (time1 - time));
//			for (int te : array) {
//				System.out.println(te + "\t");
//			}
    }

    public void qSort(int[] a, int start, int end) {
        while (start < end) {
            int pivot = a[java.util.concurrent.ThreadLocalRandom.current()
                    .nextInt(start, end + 1)];
            int less = start, scan = start, greater = end;
            while (scan <= greater) {
                if (a[scan] < pivot) swap(a, less++, scan++);
                else if (a[scan] > pivot) swap(a, scan, greater--);
                else scan++;
            }
            // Recurse into the smaller partition to keep stack usage O(log n).
            if (less - start < end - greater) {
                qSort(a, start, less - 1);
                start = greater + 1;
            } else {
                qSort(a, greater + 1, end);
                end = less - 1;
            }
        }
    }

    private void swap(int[] a, int i, int j) {
        int value = a[i];
        a[i] = a[j];
        a[j] = value;
    }
}
