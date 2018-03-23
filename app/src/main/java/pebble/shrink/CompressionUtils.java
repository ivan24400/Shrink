package pebble.shrink;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.zip.CRC32;
import java.io.FileInputStream;
import java.io.IOException;


public class CompressionUtils {

    public static final int DEFLATE = 0;
    public static final int DCRZ = 1;
    public static final byte MASK_LAST_CHUNK = (byte)0x80;

    public static String cmethod = "COMPRESSION_METHOD";
    public static String cfile = "COMPRESSION_FILE";

    public static boolean isLocal = false;

    public static String ACTION_COMPRESS_LOCAL = "compressionUtils_compress_local";
    private static String TAG = "CompressionUtils";

    private native static int dcrzCompress(boolean append, boolean isLast, String input, String output);

    public static void writeHeader(int method, String inFile) {
        File in = new File(inFile);
        try {
            if (!in.exists()) {
                throw new FileNotFoundException("Input File not found");
            }
            StringBuilder outFile = new StringBuilder(inFile);
            outFile.append(".dcrz");
            FileOutputStream out = new FileOutputStream(outFile.toString());

            long crc = computeCrc32(inFile);

            if (method == DEFLATE) {
                out.write(CompressionUtils.DEFLATE);
            } else if (method == DCRZ) {
                out.write(CompressionUtils.DCRZ);
            }

            int shift = 0;
            while (shift <= 24) {
                byte data = (byte) ((crc >> shift) & 0xff); // Little Endian
                out.write(data);
                shift = shift + 8;
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int compress(int method, boolean isLast, String inFile) {
        StringBuilder outFile = new StringBuilder(inFile);
        outFile.append(".dcrz");
        if (method == DEFLATE) {
            return Deflate.compressFile(isLocal, inFile, outFile.toString());
        } else if (method == DCRZ) {
            return CompressionUtils.dcrzCompress(isLocal, isLast, inFile, outFile.toString());
        }
        return -1;
    }

    public static long computeCrc32(String name) {
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
            Log.d(TAG,"CRc is "+crc);
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return crc;
    }
}
