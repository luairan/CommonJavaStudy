package sort;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

public class RadixSort {
    public static void main(String[] args) throws Exception {
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


//		int[] array = { 14, 3, 1, 12, 6, 4, 7 };
        RadixSort sss = new RadixSort();
        long time = System.nanoTime();
        sss.radixSort(array, 6);
        long time1 = System.nanoTime();
        System.out.println(time + "    " + time1 + "\t\t" + (time1 - time));
//		for (int te : array) {
//			System.out.println(te + "\t");
//		}
//		System.out.println();
    }

    public void radixSort(int[] array, int maxbit) {
        int[] bitposition = new int[10];
        int[][] a = new int[10][array.length];
        int bit = 1;
        for (int i = 0; i < maxbit; i++) {
            for (int temp : array) {
                int po = (temp / bit) % 10;
                int position = bitposition[po];
                a[po][position] = temp;
                bitposition[po]++;
            }
            bit *= 10;
            toArray(a, bitposition, array);
            Arrays.fill(bitposition, 0);
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