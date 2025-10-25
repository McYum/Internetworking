package apps;

import core.Msg;
import phy.PhyConfiguration;
import phy.PhyProtocol;
import java.io.IOException;

public class MockCommandServer {

    // Der Port muss mit dem Port übereinstimmen, den der Client erwartet.
    protected static final int COMMAND_SERVER_PORT = 2000;

    public static void main(String[] args) {
        // 1. Richten Sie das PhyProtocol ein, um auf dem richtigen Port zu lauschen.
        PhyProtocol phy = new PhyProtocol(COMMAND_SERVER_PORT);
        System.out.println("MockCommandServer gestartet und lauscht auf Port " + COMMAND_SERVER_PORT);

        while (true) {
            try {
                // 2. Warten Sie auf eine eingehende Nachricht vom Client.
                Msg receivedMsg = phy.receive();
                String rawData = receivedMsg.getData();
                System.out.println("Nachricht empfangen: \"" + rawData + "\"");

                // 3. Parsen Sie die Nachricht manuell, um die ID zu erhalten.
                // Erwartetes Format: "cp <cookie> <id> <crc> <command>"
                String[] parts = rawData.split("\\s+");

                // Wir benötigen die ID, die das 3. Element ist (Index 2).
                if (parts.length >= 5 && parts[0].equals("cp")) {
                    String messageId = parts[2];
                    System.out.println("Extrahierte Message-ID: " + messageId);

                    // 4. Erstellen Sie die Antwortnachricht.
                    // Format: "cp <id> ACK"
                    String responseData = "cp " + messageId + " ACK";

                    // 5. Senden Sie die Antwort an den Client zurück.
                    // Die Konfiguration (IP/Port des Clients) wird aus der empfangenen Nachricht übernommen.
                    PhyConfiguration clientConfig = (PhyConfiguration) receivedMsg.getConfiguration();
                    phy.send(responseData, clientConfig);
                    System.out.println("Antwort gesendet: \"" + responseData + "\"");
                } else {
                    System.out.println("WARNUNG: Empfangene Nachricht hat ein unbekanntes Format.");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Ein unerwarteter Fehler ist aufgetreten: " + e.getMessage());
            }
        }
    }
}