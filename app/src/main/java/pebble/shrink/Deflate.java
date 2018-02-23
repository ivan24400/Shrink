package pebble.shrink;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * This class compresses or decompress input file
 * using deflate algorithm
 */
public class Deflate {

    private static final byte METHOD = Compression.DEFLATE & 0xFF;
    private static FileInputStream fin;
    private static FileOutputStream fout;

    /**
     * Copy byte-wise data from input to output streams
     * and closes them.
     *
     * @param in  Input stream
     * @param out Output stream
     * @throws IOException I/O related exceptions
     */
    private static void copyData(InputStream in, OutputStream out) throws IOException {
        int data;

        while ((data = in.read()) != -1) {
            out.write(data);
        }
        in.close();
        out.close();
    }

    /**
     * Compress a file with given name.
     *
     * @param input Name of the file to compress
     * @return error code
     */
    public static int compressFile(String input) {
        try {
            fin = new FileInputStream(input);

            int iLength = input.length();
            byte[] header = {METHOD, 0x01, 0x02, 0x03, 0x04};

            StringBuilder output = new StringBuilder(input);

            output.append('.');
            output.append('d');
            output.append('c');
            output.append('r');
            output.append('z');

            fout = new FileOutputStream(output.toString());

            DeflaterOutputStream dout = new DeflaterOutputStream(fout);

            copyData(fin, dout);


        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    /**
     * Decompress a file with given name.
     *
     * @param input Name of the file to decompress
     * @return error code
     */
    public static int decompressFile(String input) {
        try {
            fin = new FileInputStream(input);
            int iLength = input.length();
            byte[] header = new byte[5];

            StringBuilder output;
            if (input.substring(iLength - 4).equals("dcrz")) {
                output = new StringBuilder(input.substring(0, iLength - 5));
            } else {
                output = new StringBuilder(input + "_1");
            }

            fout = new FileOutputStream(output.toString());
            InflaterInputStream iin = new InflaterInputStream(fin);

            copyData(iin, fout);

        } catch (IOException e) {
            e.printStackTrace();
            return 2;
        }
        return 0;
    }

}
