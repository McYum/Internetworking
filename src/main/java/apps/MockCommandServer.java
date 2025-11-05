package apps;

import core.Msg;
import phy.PhyConfiguration;
import phy.PhyProtocol;
import java.io.IOException;

public class MockCommandServer {

    // Port needs to match with client expectations
    protected static final int COMMAND_SERVER_PORT = 2000;

    public static void main(String[] args) {
        // Setup Phyprotocol to listen to the correct port
        PhyProtocol phy = new PhyProtocol(COMMAND_SERVER_PORT);
        System.out.println("MockCommandServer starting listening on Port " + COMMAND_SERVER_PORT);

        while (true) {
            try {
                // Wait for client msg
                Msg receivedMsg = phy.receive();
                String rawData = receivedMsg.getData();
                System.out.println("Msg received: \"" + rawData + "\"");

                // Manually parse msg to get ID
                String[] parts = rawData.split("\\s+");

                // Get ID which is in the second index
                if (parts.length >= 5 && parts[0].equals("cp")) {
                    String messageId = parts[2];
                    System.out.println("Extract Message-ID: " + messageId);

                    // Create repsonse msg
                    // Format: "cp <id> ACK"
                    String responseData = "cp " + messageId + " ACK";

                    // Feedback to the client
                    // Conifuguration gets taken from the received msg
                    PhyConfiguration clientConfig = (PhyConfiguration) receivedMsg.getConfiguration();
                    phy.send(responseData, clientConfig);
                    System.out.println("Response sent: \"" + responseData + "\"");
                } else {
                    System.out.println("[WARNING]: Received msg has a wrong format.");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Unexpected error occured: " + e.getMessage());
            }
        }
    }
}