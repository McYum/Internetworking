package apps;

import cp.CPProtocol;
import exceptions.CookieRequestException;
import exceptions.IWProtocolException;
import phy.PhyProtocol;

import java.net.InetAddress;

/**
 * Minimal client to test Task 1 and Task 2 (cookie server only).
 *
 * Usage:
 *   - arg0 = local UDP port for this client (e.g., 5001)
 *   - arg1 = optional: "twice" to request the cookie two times (tests premature renewal policy)
 *
 * How it works:
 *   - Sets up a PhyProtocol on the given local port
 *   - Creates a CPProtocol client bound to localhost and the command server port (not used here)
 *   - Sets the cookie server address/port to localhost and CPCookieServer.COOKIE_SERVER_PORT
 *   - Calls requestCookie() once (or twice) and prints observed cookie value(s)
 */
public class CookieClient {
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("Usage: CookieClient <localPort> [twice]");
                return;
            }
            int localPort = Integer.parseInt(args[0]);
            boolean twice = args.length >= 2 && "twice".equalsIgnoreCase(args[1]);

            // Setup physical layer on the given local UDP port
            PhyProtocol phy = new PhyProtocol(localPort);

            // Setup CP client protocol; command server port is not used for cookie-only tests
            CPProtocol cp = new CPProtocol(InetAddress.getByName("localhost"), MockCommandServer.COMMAND_SERVER_PORT, phy);
            cp.setCookieServer(InetAddress.getByName("localhost"), CPCookieServer.COOKIE_SERVER_PORT);

            // Request cookie once
            try {
                cp.requestCookie();
                System.out.println("First cookie: " + cp.getCookie());
            } catch (CookieRequestException e) {
                System.out.println("First cookie request rejected (NAK)");
                return;
            } catch (IWProtocolException e) {
                System.out.println("Protocol error on first request");
                return;
            } catch (Exception e) {
                System.out.println("Unexpected error on first request: " + e.getMessage());
                return;
            }

            // Optionally test premature renewal: request again and compare
            if (twice) {
                try {
                    cp.requestCookie();
                    System.out.println("Second cookie: " + cp.getCookie());
                    System.out.println("Premature renewal policy OK: same cookie expected.");
                } catch (CookieRequestException e) {
                    System.out.println("Second cookie request rejected (NAK)");
                } catch (IWProtocolException e) {
                    System.out.println("Protocol error on second request");
                } catch (Exception e) {
                    System.out.println("Unexpected error on second request: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
