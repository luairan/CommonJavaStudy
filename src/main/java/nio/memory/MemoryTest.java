package memory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class MemoryTest {
	public static void main(String[] args) throws IOException {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		
		ObjectOutputStream oos=new ObjectOutputStream(baos);
		
		oos.writeObject(new String("11111111111111111111111"));
		
		oos.flush();
		
		byte [] bytes =baos.toByteArray();
		
		ByteBuffer bb=ByteBuffer.allocateDirect(bytes.length);
		
		oos.close();
		
		baos.close();
		
		bb.put(bytes);
		
		ByteArrayInputStream bais =new ByteArrayInputStream(bytes);
		
		ObjectInputStream ois= new ObjectInputStream(bais);
		
		Object obj=null;
		try {
			obj = ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		ois.close();
		
		bais.close();
		
		
		System.out.println((String)obj);
		
		
		
		}
}
