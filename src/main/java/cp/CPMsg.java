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
            // Better discrimination between command messages and command responses:
            // - Command response format: "<id> ACK|NAK" (2 tokens, second ACK/NAK)
            // - Command request format: "<cookie> <id> <crc> <command>" (>=4 tokens)
            String[] cparts = content.split("\\s+");
            if (cparts.length >= 2 && ("ACK".equals(cparts[1]) || "NAK".equals(cparts[1]))) {
                // Likely a command response (e.g., "42 ACK")
                try {
                    Integer.parseInt(cparts[0]);
                    parsedMsg = new CPCommandResponseMsg();
                } catch (NumberFormatException e) {
                    throw new IllegalMsgException();
                }
            } else if (cparts.length >= 4) {
                // Likely a command message (e.g., "<cookie> <id> <crc> <command>")
                try {
                    Integer.parseInt(cparts[0]); // cookie
                    parsedMsg = new CPCommandMsg();
                } catch (NumberFormatException e) {
                    throw new IllegalMsgException();
                }
            } else {
                throw new IllegalMsgException();
            }
        }

        parsedMsg = (CPMsg) parsedMsg.parse(content);
        return parsedMsg;
    }

}
