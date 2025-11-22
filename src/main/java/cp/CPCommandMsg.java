package cp;

import core.Msg;
import exceptions.IWProtocolException;
import exceptions.IllegalMsgException;
import java.util.zip.CRC32;

public class CPCommandMsg extends CPMsg {
    private static int nextId = 0;
    private int cookie;
    private int id;
    private long crc;
    private String command;
    
    public int getId() {
        return id;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void create(String command, int cookie) {
        this.command = command;
        this.cookie = cookie;
        this.id = nextId++;
        
        // a. CRC32-Checksum
        CRC32 crc32 = new CRC32();
        crc32.update(command.getBytes());
        this.crc = crc32.getValue();
        
        // Format msg
        String formattedData = String.format("%d %d %d %s", this.cookie, this.id, this.crc, this.command);

        // Delegate to the common create(String) implementation which will
        // prepend the CP header and set the raw data bytes.
        this.create(formattedData);
    }
    
    @Override
    protected void create(String sentence) {
        // The sentence for CPCommandMsg is expected to be: "<cookie> <id> <crc> <command>"
        // We try to parse these fields so the object reflects the content, then
        // call the superclass to prepend the "cp" header and set data bytes.
        String[] parts = sentence.split("\\s+", 4);
        if (parts.length >= 1) {
            try {
                this.cookie = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                // leave default if parsing fails
            }
        }
        if (parts.length >= 2) {
            try {
                this.id = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // leave default if parsing fails
            }
        }
        if (parts.length >= 3) {
            try {
                this.crc = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                // leave default if parsing fails
            }
        }
        if (parts.length >= 4) {
            this.command = parts[3];
        }

        // Let the superclass assemble the final data string and bytes.
        super.create(sentence);
    }
    
    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        // Expected format: "<cookie> <id> <crc> <command>"
        String[] parts = sentence.split("\\s+", 4);
        if (parts.length < 4) {
            throw new IllegalMsgException();
        }

        try {
            this.cookie = Integer.parseInt(parts[0]);
            this.id = Integer.parseInt(parts[1]);
            this.crc = Long.parseLong(parts[2]);
            this.command = parts[3];
        } catch (NumberFormatException e) {
            throw new IllegalMsgException();
        }

        this.data = sentence;
        return this;
    }
}