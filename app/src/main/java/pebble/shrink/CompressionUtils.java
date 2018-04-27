package pebble.shrink;

import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.io.FileInputStream;
import java.io.IOException;


public class CompressionUtils {
    private static String TAG = "CompressionUtils";

    public static final int ERR_INVALID_CRC = -24000;
    public static final int DEFLATE = 0;
    public static final int DCRZ = 1;
    public static final byte MASK_LAST_CHUNK = (byte) 0x80;

    public static final String cmethod = "COMPRESSION_METHOD";
    public static final String cfile = "COMPRESSION_FILE";
    public static long crc = 0;
    public static boolean isLocal = false;

    public static final String ACTION_COMPRESS_LOCAL = "compressionUtils_compress_local";
    public static final String ACTION_DECOMPRESS_LOCAL = "compressionUtils_decompress_local";


    private static final int bufferSize = 4096;
    private static byte[] buffer = new byte[bufferSize];

    /**
     * Compress file using dcrz method
     * @param append to either append or overwrite output file
     * @param isLast used by slave device to specify if last chunk of input file
     * @param input input file name
     * @param output output file name
     * @return error status
     */
    private native static int dcrzCompress(boolean append, boolean isLast, String input, String output);

    /**
     * Decompress a dcrz compressed file
     * @param input input file name
     * @param output output file name
     */
    private native static int dcrzDecompress(String input, String output);

    /**
     * Writes header information to output file of compression
     * @param method compression method
     * @param inFile name of file to write header
     */
    public static void writeHeader(int method, String inFile) {
        File in = new File(inFile);
        try {
            if (!in.exists()) {
                throw new FileNotFoundException("Input File not found");
            }
            StringBuilder outFile = new StringBuilder(inFile);
            outFile.append(".dcrz");
            FileOutputStream out = new FileOutputStream(outFile.toString());

            crc = computeCrc32(inFile);

            if (method == DEFLATE) {
                out.write(CompressionUtils.DEFLATE);
            } else if (method == DCRZ) {
                out.write(CompressionUtils.DCRZ);
            }

            int shift = 24;
            while (shift >= 0) {
                byte data = (byte) ((crc >> shift) & 0xff); // Big Endian
                out.write(data);
                shift = shift - 8;
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compress a file
     * @param method compression method
     * @param isLast is last chunk of the original file
     * @param inFile file to be compressed
     * @return error status
     */
    public static int compress(int method, boolean isLast, String inFile) {
        Log.d(TAG,"compress: isLocal "+isLocal+" isLast "+isLast);
        StringBuilder outFile = new StringBuilder(inFile);
        outFile.append(".dcrz");
        if (method == DEFLATE) {
            return Deflate.compressFile(isLocal, inFile, outFile.toString());
        } else if (method == DCRZ) {
            return CompressionUtils.dcrzCompress(isLocal, isLast, inFile, outFile.toString());
        }
        return -1;
    }

    /**
     * Decompress a dcrz file
     * @param infile file to decompress
     * @return error status
     * @throws IOException
     */
    public static int decompress(String infile) throws IOException {
        int result=-24;

        StringBuilder outFile = new StringBuilder(infile);
        outFile.delete(infile.length() - 5, infile.length());
        String outFileExt = outFile.substring(outFile.lastIndexOf("."));
        String outFileName = outFile.substring(0,outFile.lastIndexOf("."));
        String outFileNameT = outFileName;

        boolean isFileExist = true;
        int count = 0;
        while(isFileExist){
            if (new File(outFileNameT + outFileExt).exists()) {
                count++;
                outFileNameT = outFileName + "_"+count;
            }else{
                isFileExist = false;
            }
        }

        FileInputStream input = new FileInputStream(infile);
        int algorithm = input.read();
        int i = 0;
        while (i < 4) {
            crc = (crc << 8) | (long) input.read();
            i++;
        }

        if (algorithm == DEFLATE) {
            result = Deflate.decompressFile(infile, outFileNameT + outFileExt);
        } else if (algorithm == DCRZ) {
            result = dcrzDecompress(infile,outFileNameT + outFileExt);
        }

        long crcOutput = computeCrc32(outFileNameT + outFileExt);
        Log.d(TAG, "decompress crcInput = " + Long.toHexString(crc)+"\tdecompress crcOutput = "+ Long.toHexString(crcOutput));
        if(crc != crcOutput){
            result =0;
        }
        return result;
    }

    /**
     * Compute CRC32 of a file
     * @param name file name whose crc32 has to generated
     * @return crc32
     */
    public static long computeCrc32(String name) {
        long crc = 1;
        try {
            System.out.println(name);
            BufferedInputStream file = new BufferedInputStream(new FileInputStream(name));
            CRC32 obj = new CRC32();
            int cnt;
            while ((cnt = file.read(buffer, 0, bufferSize)) != -1) {
                obj.update(buffer, 0, cnt);
            }
            crc = obj.getValue();
            Log.d(TAG, "compressUtils crc = " + Long.toHexString(crc));
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return crc;
    }

    /**
     * Compute CRC32 of a file
     * @param file InputStream whose crc32 is to be generated
     * @return crc32
     * @throws IOException
     */
    public static long computeCRC32(InputStream file) throws IOException{
        CRC32 obj = new CRC32();
        int cnt;
        while ((cnt = file.read(buffer, 0, bufferSize)) != -1) {
            obj.update(buffer, 0, cnt);
        }
        return obj.getValue();
    }
}
