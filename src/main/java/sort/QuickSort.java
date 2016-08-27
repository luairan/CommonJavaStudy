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


        FileInputStream fis = null;
        fis = new FileInputStream("/Users/luairan/Desktop/a.txt");
        BufferedInputStream bis = new BufferedInputStream(fis);
        InputStreamReader isr = new InputStreamReader(bis);
        BufferedReader br = new BufferedReader(isr);

        String temp = null;
        int[] array = new int[500000];
        int s = 0;
        while ((temp = br.readLine()) != null) {
            array[s] = Integer.parseInt(temp);
            s++;
        }
        br.close();
        isr.close();
        bis.close();
        fis.close();


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
        int i = start + 1;
        int j = end;
        int temp = 0;
        while (i <= j) {
            while (i <= j && a[i] <= a[start])
                i++;
            while (j >= i && a[j] >= a[start])
                j--;
            if (i < j) {
                temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }
        if (start < end) {
            temp = a[start];
            a[start] = a[j];
            a[j] = temp;
            qSort(a, start, j - 1);
            qSort(a, j + 1, end);
        }
    }
}