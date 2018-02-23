package pebble.shrink;

import java.util.Scanner;
import java.util.zip.CRC32;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This class compress or decompress given input file
 * using deflate or dcrz method.
 */

public class Compression {

    public static final boolean COMPRESSION = true;
    //public static final boolean DECOMPRESSION = false;

    public static final int DEFLATE = 0;
    public static final int DCRZ = 1;

    static {
        System.loadLibrary("dcrz");
    }

    public static int compress(int method, String filePath) {

        if (method == DEFLATE) {

            return Deflate.compressFile(filePath);

        } else if (method == DCRZ) {

            Compression c = new Compression();
            return c.dcrz(COMPRESSION, filePath);

        }

        return -1;
    }

    private native int dcrz(boolean mode, String input);

    public long computeCrc32(String name) {
        long crc = 1;
        try {
            System.out.println(name);
            FileInputStream file = new FileInputStream(name);
            CRC32 obj = new CRC32();
            int cnt;
            while ((cnt = file.read()) != -1) {
                obj.update(cnt);
            }
            crc = obj.getValue();
            System.out.printf("CRc is %x\n", crc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return crc;
    }
}
