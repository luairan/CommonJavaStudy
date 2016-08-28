public class FindPosition {
	public int find(int [] array ,int postion){
		int start = 0;
		int end = array.length-1;
		int index = partition(array, start, end);
		while(index != postion){
			if(index > postion) {
				index = partition(array, start, index-1);
			}else{
				index = partition(array, index+1, end);
			}
		}
		return array[index];
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