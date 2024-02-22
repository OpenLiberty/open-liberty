package tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

public class MultiServerRerouteTest extends MultiServerTest {

	@Server("FourthServer")
	public static LibertyServer server4;

	@BeforeClass
	public static void startServer4() throws Exception {
    	int port = Integer.parseInt(System.getProperty("HTTP_quaternary"));
    	Log.info(MultiServerRerouteTest.class, "startServer4", "Setting port for " + server4.getServerName() + " to " + port);
    	server4.setHttpDefaultPort(port);

    	FATUtils.startServers(server4);
	}

	@AfterClass
	public static void stopServer4() throws Exception {
		FATUtils.stopServers(server4);
	}
}