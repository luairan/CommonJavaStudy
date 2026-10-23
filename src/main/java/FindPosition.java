public class FindPosition {
    /** Returns the value at the zero-based rank, modifying the input array. */
    public int find(int[] array, int position) {
        java.util.Objects.requireNonNull(array, "array");
        if (position < 0 || position >= array.length) {
            throw new IllegalArgumentException("Rank is outside the array");
        }
        int start = 0, end = array.length - 1;
        while (start <= end) {
            int index = partition(array, start, end);
            if (index == position) return array[index];
            if (index > position) end = index - 1;
            else start = index + 1;
        }
        throw new AssertionError("Unreachable rank");
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
