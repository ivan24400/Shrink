package pebble.shrink;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Device {

    public static final int HEADER_SIZE = 35; // deviceName = 30 , freeSpace = 4, battery = 2
    private String deviceName;
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

        // Read Device Name
        StringBuilder dname = null;
        int i = 0;
        while (head[i] != (byte) 0) {
            dname.append(head[i++]);
        }
        deviceName = dname.toString();

        // Read Free Space
        i = 30; // free space base
        int shift = 0;
        while (i <= 33) { // free space length is 4 bytes
            freeSpace = (freeSpace << shift) | head[i++]; // Big Endian
            shift = shift + 8;
        }

        // Read Battery
        battery = (char) head[i]; // battery is 1 byte
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getFreeSpace() {
        return freeSpace;
    }

    public char getBattery() {
        return battery;
    }

}
