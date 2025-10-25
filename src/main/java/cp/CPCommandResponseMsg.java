package cp;

import core.Msg;
import exceptions.IWProtocolException;
import exceptions.IllegalMsgException;

public class CPCommandResponseMsg extends CPMsg {
    private int id;
    private boolean success;
    
    public int getId() {
        return id;
    }

    public boolean getSuccess() {
        return success;
    }

    @Override
    protected void create(String data) {
        throw new UnsupportedOperationException("Erstellung von CommandResponseMsg ist auf dem Client nicht implementiert.");
    }

    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        String[] parts = sentence.split("\\s+");
        // Erwartetes Format: <id> ACK/NAK
        if (parts.length != 2) {
            throw new IllegalMsgException();
        }

        try {
            this.id = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalMsgException();
        }

        this.success = parts[1].equals("ACK");
        this.data = sentence;

        return this;
    }
}