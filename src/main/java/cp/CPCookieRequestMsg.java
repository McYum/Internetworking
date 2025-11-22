package cp;

import core.Msg;
import exceptions.IWProtocolException;
import exceptions.IllegalMsgException;

public class CPCookieRequestMsg extends CPMsg {
    protected static final String CP_CREQ_HEADER = "cookie_request";

    @Override
    public void create(String ignored) {
        // The message is always just the header.
        super.create(CP_CREQ_HEADER);
    }

    @Override
    protected Msg parse(String sentence) throws IWProtocolException {
        if (!sentence.equals(CP_CREQ_HEADER)) {
            throw new IllegalMsgException();
        }
        this.data = sentence;
        return this;
    }
}