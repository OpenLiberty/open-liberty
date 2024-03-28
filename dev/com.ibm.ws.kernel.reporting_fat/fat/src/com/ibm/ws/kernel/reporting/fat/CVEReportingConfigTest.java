package com.ibm.ws.kernel.reporting.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CVEReportingConfigTest extends FATServletClient {

	public static final String SERVER_NAME = "com.ibm.ws.kernel.reporting.server";

	@Server(SERVER_NAME)
	public static LibertyServer server;

	@BeforeClass
	public static void setup() throws Exception {

		server.saveServerConfiguration();
	}

	@After
	public void tearDown() throws Exception {

		if (server.isStarted()) {
			server.stopServer();
		}

		server.restoreServerConfiguration();
	}

	@Test
	public void testIsEnabledByDefault() throws Exception {

		server.startServer();

		assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));

		server.stopServer("CWWKF1704W");
	}

	@Test
	public void testIsDisabled() throws Exception {

		ServerConfiguration config = server.getServerConfiguration();
		config.getCVEReporting().setEnabled(false);
		server.updateServerConfiguration(config);

		server.startServer();

		assertNotNull("The feature is enabled", server.waitForStringInLog("CWWKF1701I:.*"));
	}

	@Test
	public void testIsEnabled() throws Exception {

		ServerConfiguration config = server.getServerConfiguration();
		config.getCVEReporting().setEnabled(true);
		server.updateServerConfiguration(config);

		server.startServer();

		assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));

		server.stopServer("CWWKF1704W");
	}

	@Test
	public void testDynamicUpdate() throws Exception {

		ServerConfiguration config = server.getServerConfiguration();
		config.getCVEReporting().setEnabled(true);
		server.updateServerConfiguration(config);

		server.startServer();

		assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));

		server.setMarkToEndOfLog();

		config.getCVEReporting().setEnabled(false);
		server.updateServerConfiguration(config);

		assertNotNull("The feature is enabled", server.waitForStringInLog("CWWKF1701I:.*"));

		server.setMarkToEndOfLog();

		config.getCVEReporting().setEnabled(true);
		server.updateServerConfiguration(config);

		assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));

		server.stopServer("CWWKF1704W");

	}

}
