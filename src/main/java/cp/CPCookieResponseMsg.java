package cp;

import core.Msg;
import exceptions.IWProtocolException;
import exceptions.IllegalMsgException;

public class CPCookieResponseMsg extends CPMsg {
    protected static final String CP_CRES_HEADER = "cookie_response";
    private int cookie;
    private boolean success;

    public int getCookie() {
        return cookie;
    }

    public boolean getSuccess() {
        return success;
    }

    // Additional constructor to satisfy tests: success decided at construction
    public CPCookieResponseMsg() { }

    public CPCookieResponseMsg(boolean success) {
        this.success = success;
    }

    // Test API: build message body depending on success flag
    @Override
    public void create(String payload) {
        String content;
        if (this.success) {
            // Expected by tests: "cp cookie_response ACK <cookie>"
            content = CP_CRES_HEADER + " ACK " + payload;
            try {
                this.cookie = Integer.parseInt(payload);
            } catch (NumberFormatException e) {
                // leave cookie default; parse() will validate during receive-side usage
            }
        } else {
            // Expected by tests: "cp cookie_response NAK <reason>"
            content = CP_CRES_HEADER + " NAK " + payload;
        }
        super.create(content);
    }

    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        // Expected format: "cookie_response ACK <cookie>" or "cookie_response NAK <reason>"
        String[] parts = sentence.split("\\s+");
        if (parts.length < 2 || !parts[0].equals(CP_CRES_HEADER)) {
            throw new IllegalMsgException();
        }

        if (parts[1].equals("NAK")) {
            // NAK may have an explanation after, which we ignore functionally
            this.success = false;
            this.cookie = -1;
            if (parts.length < 3) {
                // allow "cookie_response NAK" without reason as minimal valid? Tests use reason text.
                // We'll accept presence or absence of reason.
            }
        } else if (parts[1].equals("ACK")) {
            if (parts.length < 3) {
                throw new IllegalMsgException();
            }
            try {
                this.cookie = Integer.parseInt(parts[2]);
                this.success = true;
            } catch (NumberFormatException e) {
                throw new IllegalMsgException();
            }
        } else {
            throw new IllegalMsgException();
        }
        this.data = sentence;
        return this;
    }
}