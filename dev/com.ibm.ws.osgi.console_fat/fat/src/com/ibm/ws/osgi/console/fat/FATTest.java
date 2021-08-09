package com.ibm.ws.osgi.console.fat;

/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.SocketTimeoutException;

import org.apache.commons.net.telnet.TelnetClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class FATTest {
	/**  */
	private static final String NEWLINE = System.getProperty("line.separator");
	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("com.ibm.ws.osgi.console.fat");
	private static BufferedReader out;
	private static PrintStream in;
	private static TelnetClient telnet;

	@BeforeClass
	public static void setUp() throws Exception {
		server.startServer();

		telnet = new TelnetClient();

		String portProperty = System.getProperty("osgiConsolePort");
		assertNotNull(
				"The test hasn't been set up quite right, so we couldn't get a system property to work out the osgi console port number.",
				portProperty);
		final int consolePort = Integer.valueOf(portProperty);

		System.out.println("Connecting to server using port " + consolePort);

		try {

			telnet.connect("localhost", consolePort);
			assertTrue(
					"We should be able to make a connection on the osgi console port.",
					telnet.isConnected());
		} catch (IOException e) {
			fail("We should be able to establish a telnet connection to the osgi console: "
					+ e);
		}

		// When machines run slow, our reads can time out before we get the
		// results of our commands back.
		// Allow a generous timeout, which will slow the test down overall,
		// since the timeout pops for every test method, but give more tolerance
		// to slow hardware
		telnet.setSoTimeout(2500);
		out = new BufferedReader(new InputStreamReader(telnet.getInputStream()));
		in = new PrintStream(telnet.getOutputStream());

	}

	@Test
	public void testConsoleHasEquinoxSSCommands() throws Exception {
		// Send an 'ss' command
		String ss = "ss";
		in.println(ss);
		in.flush();
		validateListing(ss, "bundles", "com.ibm.ws.");
	}

	@Test
	public void testConsoleHasGogoBundleListCommands() throws Exception {
		// Send an 'lb' command, which is the felix bundle list
		String list = "lb";
		in.println(list);
		in.flush();
		validateListing(list, "bundles", "WebSphere");
	}

	@Test
	public void testConsoleHasFullyQualifiedGogoBundleListCommands()
			throws Exception {
		// Send an 'lb' command, which is the fully qualified felix bundle list
		String list = "felix:lb";
		in.println(list);
		in.flush();
		validateListing(list, "bundles", "WebSphere");
	}

	@Test
	public void testConsoleHasScrListCommands() throws Exception {
		// Send an 'scr:list' command
		String list = "scr:list";
		in.println(list);
		in.flush();
		// We're actually expecting a list of services back
		validateListing(list, "services", "com.ibm.ws.");
	}

	/**
	 * Checks through output from the server and makes sure it includes a
	 * reasonable number of things that look like they belong to Liberty.
	 *
	 * @param marker
	 *            TODO
	 */
	private void validateListing(String commandName, String type, String marker)
			throws IOException {
		int ourBundleCount = 0;
		boolean gotPassingCountOfOutputLines = false;
		StringBuilder commandOutput = new StringBuilder();
		int retry = 0;
		String line;
		// Make sure not to block while reading the output
		// Null is end of stream. But are we really sure the response has
		// ended, vs. our just getting ahead of the writes?
		while (true) { // (line != null)
			try {
				line = out.readLine();
				commandOutput.append(line);
				commandOutput.append(NEWLINE);
				if (line.contains(marker)) {
					ourBundleCount++;
					if (ourBundleCount > 3) {
						gotPassingCountOfOutputLines = true;
					}
				}
			} catch (SocketTimeoutException e) {
				System.out.println("Got socket timeout on telnet read. Retry "
						+ retry + ". " + e);
				// seems this test can fail with no command output, apparently
				// due to a socket timeout. So do a couple of retries to allow
				// more time for telnet to come back. Doing retries this way
				// will not slow passing tests.
				if (gotPassingCountOfOutputLines || retry > 2) {
					break;
				}
				retry++;
			}
		}
		assertTrue("The " + commandName
				+ " command should list a good number of  " + type
				+ " that look like Liberty " + type
				+ ", but instead it returned: " + commandOutput,
				gotPassingCountOfOutputLines);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		telnet.disconnect();
		if (server != null && server.isStarted()) {
			server.stopServer();
		}
	}

}
