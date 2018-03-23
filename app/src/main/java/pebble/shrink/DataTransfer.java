package pebble.shrink;

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
    private static int readBytes = 0;

    public static final int READY = 1;
    public static final int BUFFER_SIZE = 4096;
    public static final int HEADER_SIZE = 9; // freeSpace = 8, battery = 1 && allocatedSpace = 8, algorithm = 1

    private static byte[] buffer = new byte[BUFFER_SIZE];

    public static void initFile(final String input, final String output) throws FileNotFoundException{
        inputFile = new FileInputStream(new File(input));
        outputFile = new FileOutputStream(new File(output));
    }

    public synchronized static void transferChunk(long size, OutputStream out) throws IOException{

        if(size < BUFFER_SIZE){
            inputFile.read(buffer,0,(int)size);
            return ;
        }
        while(size != 0 ){
            readBytes = inputFile.read(buffer,0,BUFFER_SIZE);
            out.write(buffer,0,readBytes);
            out.flush();
            size = size - readBytes;
        }
    }

    public synchronized static void receiveChunk(long size, InputStream in) throws IOException{
        if(size < BUFFER_SIZE){
            readBytes = in.read(buffer,0,(int)size);
            outputFile.write(buffer,0,readBytes);
            return ;
        }
        while(size != 0){
            readBytes = in.read(buffer,0,BUFFER_SIZE);
            outputFile.write(buffer,0,readBytes);
            outputFile.flush();
            size = size - readBytes;
        }
    }

}
