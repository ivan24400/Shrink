package pebble.shrink;

import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * This class compresses or decompress input file
 * using deflate algorithm
 */
public class Deflate {

    private static FileInputStream fin;
    private static FileOutputStream fout;
    private static final int DEFLATE_BUFFER_SIZE = 16000;

    /**
     * Compress a file with given name.
     *
     * @param input Name of the file to compress
     * @return error code
     */
    public static int compressFile(boolean append, String input, String output) {
        try {
            int ret;
            fin = new FileInputStream(input);
            fout = new FileOutputStream(output, append);

            DeflaterOutputStream dout = new DeflaterOutputStream(fout);

            byte[] buffer = new byte[DEFLATE_BUFFER_SIZE];
            while ((ret = fin.read(buffer, 0, DEFLATE_BUFFER_SIZE)) != -1) {
                dout.write(buffer, 0, ret);
            }
            fin.close();
            dout.close();

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    /**
     * Decompress a file with given name.
     *
     * @param input Name of the file to decompress
     * @return error code
     */
    public static long decompressFile(long skip, String input, String output) {
        try {
            fin = new FileInputStream(input);
            fout = new FileOutputStream(output.toString(),true);
            InflaterInputStream iin = new InflaterInputStream(fin);

            int ret;
            long byteCount = 0;
            long skipCountIn = 0;
            byte[] buffer = new byte[DEFLATE_BUFFER_SIZE];
            fin.skip(5);

            while (skip > 0) {
                if(iin.read() != -1) {
                    skip--;
                }
            }

            while ((ret = iin.read(buffer, 0, DEFLATE_BUFFER_SIZE)) != -1) {
                byteCount = byteCount + ret;
                Log.d("Deflate","decompress: read-"+ret+"\tbyteCount-"+byteCount);
                fout.write(buffer, 0, ret);
            }
            iin.close();
            fout.close();
            return byteCount;

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

}
