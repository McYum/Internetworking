package cp;

import core.*;
import exceptions.*;
import phy.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CPProtocol extends Protocol {
    private static final int CP_TIMEOUT = 2000;
    private static final int CP_HASHMAP_SIZE = 20;
    private int cookie;
    private int id;
    private PhyConfiguration PhyConfigCommandServer;
    private PhyConfiguration PhyConfigCookieServer;
    private CPCommandMsg lastSentCommand;
    private final PhyProtocol PhyProto;
    private final cp_role role;
    HashMap<PhyConfiguration, Cookie> cookieMap;
    ArrayList<CPCommandMsg> pendingCommands;
    Random rnd;

    private enum cp_role {
        CLIENT, COOKIE, COMMAND
    }

    // Constructor for clients
    public CPProtocol(InetAddress rname, int rp, PhyProtocol phyP) throws UnknownHostException {
        this.PhyConfigCommandServer = new PhyConfiguration(rname, rp, proto_id.CP);
        this.PhyProto = phyP;
        this.role = cp_role.CLIENT;
        this.cookie = -1;
    }

    // Constructor for servers
    public CPProtocol(PhyProtocol phyP, boolean isCookieServer) {
        this.PhyProto = phyP;
        if (isCookieServer) {
            this.role = cp_role.COOKIE;
            this.cookieMap = new HashMap<>();
            this.rnd = new Random();
        } else {
            this.role = cp_role.COMMAND;
            this.pendingCommands = new ArrayList<>();
        }
    }

    public void setCookieServer(InetAddress rname, int rp) throws UnknownHostException {
        this.PhyConfigCookieServer = new PhyConfiguration(rname, rp, proto_id.CP);
    }

    // Expose cookie value for testing and observability
    public int getCookie() {
        return this.cookie;
    }

    @Override
    public void send(String s, Configuration config) throws IOException, IWProtocolException {
        if (this.role != cp_role.CLIENT) {
            throw new UnsupportedOperationException("Send is only supported for clients.");
        }

        if (cookie < 0) {
            requestCookie();
        }

        CPCommandMsg msg = new CPCommandMsg();
        msg.create(s, this.cookie);

        this.lastSentCommand = msg;
        this.PhyProto.send(new String(msg.getDataBytes()), this.PhyConfigCommandServer);
    }

    @Override
    public Msg receive() throws IOException, IWProtocolException {
        // This method now acts as a dispatcher based on the protocol's role.
        switch (this.role) {
            case CLIENT:
                return client_receive();
            case COOKIE:
                server_receive_dispatcher();
                return null; // For servers, loop indefinitely without returning.
            case COMMAND:
                server_receive_dispatcher();
                return null; // For servers, loop indefinitely without returning.
            default:
                throw new IllegalStateException("Unknown role");
        }
    }

    // The original receive logic, now dedicated to the client.
    private Msg client_receive() throws IOException, IWProtocolException {
        final int TIMEOUT = 3000;

        while (true) {
            try {
                Msg in = this.PhyProto.receive(TIMEOUT);

                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP) {
                    continue;
                }

                CPMsg cpmIn = new CPMsg();
                cpmIn = (CPMsg) cpmIn.parse(in.getData());

                if (!(cpmIn instanceof CPCommandResponseMsg)) {
                    continue;
                }

                CPCommandResponseMsg response = (CPCommandResponseMsg) cpmIn;

                if (response.getId() != this.lastSentCommand.getId()) {
                    System.out.println("Warning: Outdated response received. Ignoring...");
                    continue;
                }

                if (!response.getSuccess()) {
                    throw new IWProtocolException();
                }
                return response;

            } catch (SocketTimeoutException e) {
                throw new IWProtocolException();
            } catch (IllegalMsgException e) {
                System.out.println("Error: Invalid message received. Ignoring...");
            }
        }
    }

    // A generic receive loop for servers that dispatches to the correct processor.
    private void server_receive_dispatcher() throws IOException, IWProtocolException {
        while (true) {
            try {
                Msg in = this.PhyProto.receive(); // Servers block and wait for messages.
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP) {
                    continue; // Ignore non-CP messages.
                }

                CPMsg cpmIn = new CPMsg();
                cpmIn = (CPMsg) cpmIn.parse(in.getData());
                cpmIn.setConfiguration(in.getConfiguration()); // IMPORTANT: Preserve sender info.

                // Dispatch to the correct processor based on role and message type.
                if (this.role == cp_role.COOKIE && cpmIn instanceof CPCookieRequestMsg) {
                    cookie_process(cpmIn);
                } else if (this.role == cp_role.COMMAND && cpmIn instanceof CPCommandMsg) {
                    command_process(cpmIn);
                } else {
                    System.out.println("Warning: Received an unexpected message type for this server role. Ignoring.");
                }

            } catch (IllegalMsgException e) {
                System.out.println("Error: Received a malformed message. Ignoring.");
            }
            // A server should not exit on IWProtocolException, so we catch and continue.
        }
    }


    // CookieServer processing of incoming messages
    private Msg command_process(CPMsg cpmIn) throws IWProtocolException {
        // Logic for the command server would go here.
        System.out.println("Command server processing not yet implemented.");
        return null;
    }

    // Processing of the CookieRequestMsg
    private void cookie_process(CPMsg cpmIn) throws IWProtocolException, IOException {
        System.out.println("Received cookie request from: " + cpmIn.getConfiguration());

        PhyConfiguration clientConf = (PhyConfiguration) cpmIn.getConfiguration();
        
        // Check if the client already has a cookie
        if (cookieMap.containsKey(clientConf)) {
            Cookie existing = cookieMap.get(clientConf);
            int existingCookieValue = existing.getCookieValue();
            System.out.println("Client already has cookie " + existingCookieValue + ", returning existing cookie.");

            CPCookieResponseMsg response = new CPCookieResponseMsg(true);
            response.create(String.valueOf(existingCookieValue));
            PhyProto.send(new String(response.getDataBytes()), cpmIn.getConfiguration());
            return;
        }

        // Enforce maximum number of stored cookies
        if (cookieMap.size() >= CP_HASHMAP_SIZE) {
            System.out.println("Cookie map full (" + cookieMap.size() + ") - rejecting new cookie request from " + clientConf);
            // Send a NAK - CPCookieResponseMsg will format "cookie_response NAK <reason>"
            CPCookieResponseMsg response = new CPCookieResponseMsg(false);
            response.create("no resources");
            PhyProto.send(new String(response.getDataBytes()), cpmIn.getConfiguration());
            return;
        }

        // Issue a new cookie make sure its unique
        int newCookieValue;
        boolean isUnique;
        
        do {
            newCookieValue = rnd.nextInt(Integer.MAX_VALUE);
            isUnique = true;
            for (Cookie c : cookieMap.values()) {
                if (c.getCookieValue() == newCookieValue) {
                    isUnique = false;
                    break;
                }
            }
        } while (!isUnique);
        
        Cookie newCookie = new Cookie(System.currentTimeMillis(), newCookieValue);

        cookieMap.put(clientConf, newCookie);
        System.out.println("Generated new cookie " + newCookieValue + " for " + clientConf);

        CPCookieResponseMsg response = new CPCookieResponseMsg(true);
        response.create(String.valueOf(newCookieValue));

        PhyProto.send(new String(response.getDataBytes()), cpmIn.getConfiguration());
        System.out.println("Sent cookie response to client.");
    }

    public void requestCookie() throws IOException, IWProtocolException {
        CPCookieRequestMsg reqMsg = new CPCookieRequestMsg();
        reqMsg.create(null);
        Msg resMsg = new CPMsg();

        boolean waitForResp = true;
        int count = 0;
        while (waitForResp && count < 3) {
            this.PhyProto.send(new String(reqMsg.getDataBytes()), this.PhyConfigCookieServer);

            try {
                Msg in = this.PhyProto.receive(CP_TIMEOUT);
                if (((PhyConfiguration) in.getConfiguration()).getPid() != proto_id.CP)
                    continue;
                resMsg = ((CPMsg) resMsg).parse(in.getData());
                if (resMsg instanceof CPCookieResponseMsg)
                    waitForResp = false;
            } catch (SocketTimeoutException e) {
                count += 1;
            } catch (IWProtocolException ignored) {
            }
        }

        if (count == 3)
            throw new CookieRequestException();
        if (resMsg instanceof CPCookieResponseMsg && !((CPCookieResponseMsg) resMsg).getSuccess()) {
            throw new CookieRequestException();
        }
        assert resMsg instanceof CPCookieResponseMsg;
        this.cookie = ((CPCookieResponseMsg) resMsg).getCookie();
    }
}

class Cookie {
    private final long timeOfCreation;
    private final int cookieValue;

    public Cookie(long toc, int c) {
        this.timeOfCreation = toc;
        this.cookieValue = c;
    }

    public long getTimeOfCreation() {
        return timeOfCreation;
    }

    public int getCookieValue() { return cookieValue;}
}

