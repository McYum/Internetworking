package cp;

import core.Msg;
import exceptions.IWProtocolException;
import exceptions.IllegalMsgException;

public class CPCookieResponseMsg extends CPMsg {
    protected static final String CP_CRES_HEADER = "cres";
    private int cookie;
    private boolean success;

    public int getCookie() {
        return cookie;
    }

    public boolean getSuccess() {
        return success;
    }

    /**
     * Creates a cookie response message.
     * @param cookie The cookie value to send.
     * @param success True if the request was successful, false otherwise.
     */
    public void create(int cookie, boolean success) {
        this.success = success;
        this.cookie = cookie;
        String content = success ? CP_CRES_HEADER + " " + cookie : CP_CRES_HEADER + " NAK";
        super.create(content);
    }

    @Override
    protected void create(String data) {
        // This method is not meant to be used directly for this message type.
        throw new UnsupportedOperationException("Use create(int cookie, boolean success) instead.");
    }

    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        // Expected format: "cres <cookie>" or "cres NAK"
        String[] parts = sentence.split("\\s+");
        if (parts.length != 2 || !parts[0].equals(CP_CRES_HEADER)) {
            throw new IllegalMsgException();
        }

        if (parts[1].equals("NAK")) {
            this.success = false;
            this.cookie = -1;
        } else {
            try {
                this.cookie = Integer.parseInt(parts[1]);
                this.success = true;
            } catch (NumberFormatException e) {
                throw new IllegalMsgException();
            }
        }
        this.data = sentence;
        return this;
    }
}