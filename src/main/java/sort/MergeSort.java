package sort;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class MergeSort {
    public static void main(String[] args) throws Exception {
//		MergeSort mergeSort = new MergeSort();
//		int [] array = {4,5,3,2,1};
//		mergeSort.mergeSort(array, 0, array.length-1);
//		for(int temp:array){
//			System.out.print(temp+"\t");
//		}
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


        MergeSort mergeSort = new MergeSort();
        long time = System.nanoTime();
        mergeSort.mergeSort(array, 0, array.length - 1);
        long time1 = System.nanoTime();
        System.out.println(time + "    " + time1 + "\t\t" + (time1 - time));
//			for(int te:array){
//				System.out.println(te+"\t");
//			}

    }

    public void mergeSort(int[] array, int start, int end) {
        if (start < end) {
            int center = (start + end) / 2;
            mergeSort(array, start, center);
            mergeSort(array, center + 1, end);
            merge(array, start, center, end);
        }
    }

    public void merge(int[] array, int start, int center, int end) {
        int[] newarray = new int[end - start + 1];
        int position = 0;
        int starta = start, startb = center + 1;
        while (starta <= center && startb <= end) {
            if (array[starta] < array[startb]) {
                newarray[position] = array[starta];
                position++;
                starta++;
            } else {
                newarray[position] = array[startb];
                position++;
                startb++;
            }
        }
        while (starta <= center) {
            newarray[position] = array[starta];
            position++;
            starta++;
        }
        while (startb <= end) {
            newarray[position] = array[startb];
            position++;
            startb++;
        }
        System.arraycopy(newarray, 0, array, start, newarray.length);
    }

}