package cp;

import core.Protocol;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import phy.PhyConfiguration;
import phy.PhyProtocol;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifiziert, dass nach 20 gespeicherten Cookies der 21. Request mit NAK beantwortet wird.
 */
public class CPServerCookieMapTest {

    @Test
    void test21stRequestGetsNAK() throws Exception {
        // Mock PhyProtocol, damit kein echtes UDP benutzt wird
        PhyProtocol phyMock = mock(PhyProtocol.class);

        // Erzeuge CPProtocol als Cookie-Server
        CPProtocol server = new CPProtocol(phyMock, true);

        // Cookie-Processor ist private -> via Reflection aufrufen
        Method cookieProcess = CPProtocol.class.getDeclaredMethod("cookie_process", CPMsg.class);
        cookieProcess.setAccessible(true);

        // Sende 21 unterschiedliche Clients (unterschiedliche Ports)
        for (int i = 0; i < 21; i++) {
            CPCookieRequestMsg req = new CPCookieRequestMsg();
            req.create(null);
            PhyConfiguration conf = new PhyConfiguration(InetAddress.getByName("localhost"),
                    5000 + i, Protocol.proto_id.CP);
            req.setConfiguration(conf);

            // Aufruf der privaten Methode (simuliert Empfang)
            cookieProcess.invoke(server, req);
        }

        // Fange alle gesendeten Nachrichten ab
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(phyMock, times(21)).send(msgCaptor.capture(), any(PhyConfiguration.class));

        List<String> sent = msgCaptor.getAllValues();

        // Erste Nachricht sollte ACK enthalten, 21. (Index 20) sollte NAK enthalten
        assertTrue(sent.get(0).contains("ACK"), "Erste Antwort sollte ACK enthalten");
        assertTrue(sent.get(20).contains("NAK"), "21. Antwort sollte NAK enthalten");
    }
}