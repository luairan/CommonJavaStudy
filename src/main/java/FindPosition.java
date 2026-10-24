public class FindPosition {
    /** Returns the value at the zero-based rank, modifying the input array. */
    public int find(int[] array, int position) {
        java.util.Objects.requireNonNull(array, "array");
        if (position < 0 || position >= array.length) {
            throw new IllegalArgumentException("Rank is outside the array");
        }
        int start = 0, end = array.length - 1;
        while (start <= end) {
            int pivot = array[java.util.concurrent.ThreadLocalRandom.current().nextInt(start, end + 1)];
            int less = start, scan = start, greater = end;
            while (scan <= greater) {
                if (array[scan] < pivot) swap(array, less++, scan++);
                else if (array[scan] > pivot) swap(array, scan, greater--);
                else scan++;
            }
            if (position < less) end = less - 1;
            else if (position > greater) start = greater + 1;
            else return pivot;
        }
        throw new AssertionError("Unreachable rank");
    }
    private static void swap(int[] array, int first, int second) {
        int value = array[first];
        array[first] = array[second];
        array[second] = value;
    }

	public int partition (int [] array ,int start ,int end){
		int i=start+1;
		int j=end;
		int temp;
		while (i<=j){
			while(i<=j && array [i] <= array[start]) i++;
			while(i<=j && array [j] >= array[start]) j--;
			if (i < j) {
				temp = array[i];
				array[i] = array[j];
				array[j] = temp;
			}
		}
		temp = array[start];
		array[start] = array[j];
		array[j] = temp;
		return j;
	}
	public static void main(String[] args) {
		FindPosition s=new FindPosition();
		int [] array = {5,4,2,3,1,6};
		System.out.println(s.find(array, 3));
	}
}
