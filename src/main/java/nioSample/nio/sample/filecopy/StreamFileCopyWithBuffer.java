package nioSample.nio.sample.filecopy;

import java.io.*;

/**
 * ��StreamFileCopy.java��ʵ��������ͨ��FileInputStream����д
 * 
 * @author yblin 2010-10-17 ����01:59:05
 */
public class StreamFileCopyWithBuffer extends CommonCopy {

    public StreamFileCopyWithBuffer(String fromFile, String toFile){
        super(fromFile, toFile);
    }

    @Override
    public void run() {
        try {

            File fileIn = new File(fromFile);
            File fileOut = new File(toFile);
            FileInputStream fin = new FileInputStream(fileIn);
            BufferedInputStream bin = new BufferedInputStream(fin);
            FileOutputStream fout = new FileOutputStream(fileOut);
            byte[] buffer = new byte[8192];
            while (bin.read(buffer) != -1) {
                fout.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        flag = true;

    }


}
