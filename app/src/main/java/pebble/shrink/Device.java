package pebble.shrink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Device {

    public static final int HEADER_SIZE = 5; // freeSpace = 4, battery = 1

    private int freeSpace;
    private char battery;
    private InputStream in;
    private OutputStream out;
    private Socket client;

    public Device(Socket s) throws IOException {
        this.client = s;
        in = s.getInputStream();
        out = s.getOutputStream();

        byte[] head = new byte[HEADER_SIZE];
        in.read(head);

        // Read Free Space
        int i = 0; // free space base
        int shift = 0;
        while (i < 4) { // free space length is 4 bytes
            freeSpace = (freeSpace << shift) | head[i++]; // Big Endian
            shift = shift + 8;
        }

        // Read Battery
        battery = (char) head[i]; // battery is 1 byte
    }

    public int getFreeSpace() {
        return freeSpace;
    }

    public char getBattery() {
        return battery;
    }

    public InputStream getInputStream(){ return in; }

    public OutputStream getOutputStream(){ return out; }
}
