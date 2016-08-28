package nio.sample.filecopy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedFileCopy extends CommonCopy {

    public MappedFileCopy(String fromFile, String toFile){
        super(fromFile, toFile);
    }

    @Override
    public void run() {
        try {

            File fileIn = new File(fromFile);
            File fileOut = new File(toFile);

            RandomAccessFile raf = new RandomAccessFile(fileIn, "rw");
            // FileInputStream fin = new FileInputStream(fileIn);
            FileOutputStream fout = new FileOutputStream(fileOut);

            FileChannel fcIn = raf.getChannel();
            FileChannel fcOut = fout.getChannel();

            MappedByteBuffer mbb = fcIn.map(FileChannel.MapMode.READ_WRITE, 0, 8192);

            while (fcIn.read(mbb) != -1) {
                mbb.flip();
                fcOut.write(mbb);
                mbb.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        flag = true;

    }

}
