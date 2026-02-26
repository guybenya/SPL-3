package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StompEncoderDecoder implements MessageEncoderDecoder<String> {

    // Array to hold the incoming bytes before they are decoded into a string
    private byte[] bytes = new byte[1 << 10]; // start with 1k
    
    // Tracks the current length of the valid bytes in the array
    private int len = 0;

    @Override
    public String decodeNextByte(byte nextByte) {
        // In STOMP protocol, frames are terminated by a null character ('\u0000')
        // When we encounter this byte, it indicates the end of the current frame
        if (nextByte == '\u0000') {
            return popString();
        }

        // If it's not the null character, we add it to our buffer
        pushByte(nextByte);
        
        // Return null to indicate that the frame is not yet complete
        return null; 
    }

    @Override
    public byte[] encode(String message) {
        // We convert the string message into bytes using UTF-8 encoding
        // We must append the null character ('\u0000') at the end to signify the end of the STOMP frame
        return (message + '\u0000').getBytes(StandardCharsets.UTF_8);
    }

    private void pushByte(byte nextByte) {
        // If the buffer is full, we double its size to accommodate more bytes
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        // Add the new byte and increment the length counter
        bytes[len++] = nextByte;
    }

    private String popString() {
        // Convert the accumulated bytes into a String using UTF-8 encoding
        // We only use the bytes up to the current length (len)
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        
        // Reset the length counter for the next STOMP frame
        len = 0;
        
        return result;
    }
}