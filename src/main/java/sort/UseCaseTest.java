package sort;

import java.io.*;
import java.util.Random;


/**
 * 这个是测试的例子
 */
public class UseCaseTest {
	public static void main(String[] args) throws Exception{
			FileOutputStream fos =new FileOutputStream(new File("/Users/luairan/Desktop/a.txt"));
			BufferedOutputStream bos =new BufferedOutputStream(fos);
			OutputStreamWriter osw=new OutputStreamWriter(bos);
			BufferedWriter bw=new BufferedWriter(osw);
			int i=500000;

		Random rand = new Random();
		while(i>0){
			bw.write(rand.nextInt(500000)+"");
			bw.newLine();
			i--;
		}
		bw.flush();
		bw.close();
		osw.close();
		bos.close();
		fos.close();
	}
}
