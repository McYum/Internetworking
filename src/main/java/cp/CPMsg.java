package cp;

import core.Msg;
import exceptions.IWProtocolException;
import exceptions.IllegalMsgException;

class CPMsg extends Msg {
    protected static final String CP_HEADER = "cp";
    @Override
    protected void create(String sentence) {
        data = CP_HEADER + " " + sentence;
        this.dataBytes = data.getBytes();
    }

    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        CPMsg parsedMsg;
        if (!sentence.startsWith(CP_HEADER))
            throw new IllegalMsgException();

        String[] parts = sentence.split("\\s+", 2);
        if (parts.length < 2)
            throw new IllegalMsgException();

        String content = parts[1];

        if (content.startsWith(CPCookieRequestMsg.CP_CREQ_HEADER)) {
            parsedMsg = new CPCookieRequestMsg();
        } else if (content.startsWith(CPCookieResponseMsg.CP_CRES_HEADER)) {
            parsedMsg = new CPCookieResponseMsg();
        } else {
            // Annahme: Wenn es keine der bekannten Antworten ist, muss es eine Befehlsantwort sein.
            // Das Format ist <id> ACK/NAK. Wir prüfen, ob der erste Teil eine Zahl ist.
            try {
                Integer.parseInt(content.split("\\s+")[0]);
                parsedMsg = new CPCommandResponseMsg(); // Es ist eine Befehlsantwort
            } catch (NumberFormatException e) {
                throw new IllegalMsgException();
            }
        }

        parsedMsg = (CPMsg) parsedMsg.parse(content);
        return parsedMsg;
    }

}
