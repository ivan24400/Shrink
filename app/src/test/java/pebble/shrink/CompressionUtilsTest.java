package pebble.shrink;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

public class CompressionUtilsTest {

    /**
     * expected crc32 from https://www.freecodeformat.com/
     */
    @Test
    public  void computeCRC32() throws Exception{
        String input="pic.jpg";
        long output;
        long expected = 0x6e0cd8b9; // base 10 = 1846335673

        InputStream in = this.getClass().getClassLoader().getResourceAsStream(input);
        output = CompressionUtils.computeCRC32(in);

        assertEquals(expected,output);
    }
}