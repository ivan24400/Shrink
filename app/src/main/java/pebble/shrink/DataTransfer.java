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
    private static String inputFileName;
    private static String outputFileName;

    private static int readBytes = 0;

    public static final int READY = 1;
    public static final int BUFFER_SIZE = 4096;
    public static final int HEADER_SIZE = 9; // freeSpace = 8, battery = 1 && allocatedSpace = 8, algorithm = 1

    private static byte[] buffer = new byte[BUFFER_SIZE];

    public static void initFiles(boolean append, final String input, final String output) throws FileNotFoundException{
        inputFileName = input;
        outputFileName = output;
        inputFile = new FileInputStream(input);
        outputFile = new FileOutputStream(output,append);
    }

    public synchronized static void transferChunk(long size, OutputStream out) throws IOException{

        if(out == null || size == 0){
            return;
        }
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

        if(in == null || size == 0){
            return;
        }
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

    public static void releaseFiles(){
        try{
            if(inputFile != null) {
                inputFile.close();
            }
            if(outputFile != null){
                outputFile.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void deleteFiles(){
            releaseFiles();
           if( (new File(inputFileName)).delete() || (new File(outputFileName)).delete() ){
               Log.d(TAG,"Delete success");
           }else{
               Log.d(TAG,"Delete failed");
           }
    }
}
