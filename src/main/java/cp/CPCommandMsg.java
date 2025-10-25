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
        
        // a. CRC32-Prüfsumme berechnen
        CRC32 crc32 = new CRC32();
        crc32.update(command.getBytes());
        this.crc = crc32.getValue();
        
        // Nachricht formatieren
        String formattedData = String.format("%d %d %d %s", this.cookie, this.id, this.crc, this.command);
        
        // Die create-Methode der Superklasse aufrufen, um "cp " voranzustellen
        super.create(formattedData);
    }
    
    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        throw new UnsupportedOperationException("Parsing von CommandMsg ist auf dem Client nicht implementiert.");
    }
}