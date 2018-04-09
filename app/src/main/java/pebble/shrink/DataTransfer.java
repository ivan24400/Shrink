package pebble.shrink;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class DataTransfer {

    private static final String TAG = "DataTransfer";

    private static FileInputStream inputFile;
    private static FileOutputStream outputFile;
    private static String inputFileName,outputFileName;

    private static int readBytes = 0;

    public static final int READY = 1;
    public static final int HEARTBEAT_TIMEOUT = 3000;
    public static final int BUFFER_SIZE = 4096;
    public static final int HEADER_SIZE = 9; // freeSpace = 8, battery = 1 && allocatedSpace = 8, algorithm = 1

    private static byte[] buffer = new byte[BUFFER_SIZE];

    /**
     * Initialize input and output files
     * @param isMaster is master or slave mode
     * @param input input file name
     * @param output output file name
     * @throws IOException
     */
    public static void initFiles(final boolean isMaster, final String input, final String output) throws IOException {
        Log.d(TAG, "input " + input + ", output " + output + " ismaster " + WifiOperations.isMaster);
        inputFileName = input;
        outputFileName = output;
        if (isMaster) {
            inputFile = new FileInputStream(input);
            outputFile = new FileOutputStream(output, true);
        } else {
            File t = new File(input);
            t.getParentFile().mkdirs();
            t.createNewFile();
            inputFile = new FileInputStream(t);
            t = new File(output);
            t.createNewFile();
            outputFile = new FileOutputStream(t);
        }
    }

    /**
     * Send data from file to given output stream
     * @param size number of bytes to send
     * @param out stream to send data
     * @throws IOException
     */
    public synchronized static void transferChunk(long size, OutputStream out) throws IOException {
        Log.d(TAG, "transferChunk " + size);
        if (out == null || size == 0) {
            return;
        }
        if (size < BUFFER_SIZE) {
            inputFile.read(buffer, 0, (int) size);
            return;
        }
        while (size != 0) {
            readBytes = inputFile.read(buffer, 0, BUFFER_SIZE);
            out.write(buffer, 0, readBytes);
            out.flush();
            size = size - readBytes;
        }
    }

    /**
     * Receive data from given stream
     * @param size number of bytes to receive
     * @param in stream to receive data from
     * @throws IOException
     */
    public synchronized static void receiveChunk(long size, InputStream in) throws IOException {
        Log.d(TAG, "receive Chunk " + size);

        if (in == null || size == 0) {
            return;
        }
        if (size < BUFFER_SIZE) {
            readBytes = in.read(buffer, 0, (int) size);
            outputFile.write(buffer, 0, readBytes);
            return;
        }
        while (size != 0) {
            readBytes = in.read(buffer, 0, BUFFER_SIZE);
            outputFile.write(buffer, 0, readBytes);
            outputFile.flush();
            size = size - readBytes;
            Log.d(TAG, "receive Chunk end " + size);
        }
        Log.d(TAG, "receive Chunk end " + size);
    }

    /**
     * Close input and output file streams
     */
    public static void releaseFiles() {
        try {
            if (inputFile != null) {
                inputFile.close();
            }
            if (outputFile != null) {
                outputFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete input and output files
     */
    public static void deleteFiles() {
        releaseFiles();
        /*
           if( (new File(inputFileName)).delete() || (new File(outputFileName)).delete() ){
               Log.d(TAG,"Delete success");
           }else{
               Log.d(TAG,"Delete failed");
           }
           */
    }
}
