package thread.exam;

public class Mytest {
	public void print (TreeNode root ,int sum){
		int [] path =new int [100];
		helper(root, sum, path, 0);
	}
	public void helper(TreeNode root ,int sum,int [] path,int top){
        if (root == null) return;
        if (top == path.length) path = java.util.Arrays.copyOf(path, Math.max(1, top * 2));
		path[top++] = root.num;
		sum = sum - root.num;
		if(root.left==null&&root.right ==null){
			if(sum == 0)
			{
				for(int i = 0; i < top; i++){
					System.out.print(path[i]+"\t");
				}
				System.out.println();
			}
		}else{
			if(root.left!=null) helper(root.left, sum, path, top);
			if(root.right!=null) helper(root.right, sum, path, top);
		}
		top--;
		sum+=root.num;
	}
	public static void main(String[] args) {
		TreeNode root = new TreeNode(10);
		root.left = new TreeNode(5);
		root.left.left = new TreeNode(4);
		root.right = new TreeNode(12);
		Mytest t=new Mytest();
		t.print(root, 22);

	}
}

class TreeNode{
	public TreeNode(int num){
		this.num = num;
	}
	public int num;
	public TreeNode left;
	public TreeNode right;
}
