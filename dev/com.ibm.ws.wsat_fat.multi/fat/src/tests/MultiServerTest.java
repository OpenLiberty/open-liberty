/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tests;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MultiServerTest extends WSATTest {

	private static LibertyServer server;
	private static String BASE_URL;
	private static LibertyServer server2;
	private static String BASE_URL2;
	private static LibertyServer server3;
	private static String BASE_URL3;

    public static final String[] serverNames = new String[] {
                                                        "WSATBasic",
                                                        "MultiServerTest",
                                                        "ThirdServer",
    };

	@BeforeClass
	public static void beforeTests() throws Exception {

		server = LibertyServerFactory
				.getLibertyServer("WSATBasic");
		BASE_URL = "http://" + server.getHostname() + ":"
				+ server.getHttpDefaultPort();
		server2 = LibertyServerFactory
				.getLibertyServer("MultiServerTest");
		server2.setHttpDefaultPort(9888);
		BASE_URL2 = "http://" + server2.getHostname() + ":" + server2.getHttpDefaultPort();
		server3 = LibertyServerFactory
				.getLibertyServer("ThirdServer");
		server3.setHttpDefaultPort(9988);
		BASE_URL3 = "http://" + server3.getHostname() + ":" + server3.getHttpDefaultPort();

		DBTestBase.initWSATTest(server);
		DBTestBase.initWSATTest(server2);
		DBTestBase.initWSATTest(server3);

		ShrinkHelper.defaultDropinApp(server, "oneway", "web.oneway.*");
		ShrinkHelper.defaultDropinApp(server, "endtoend", "web.endtoend.*");
		ShrinkHelper.defaultDropinApp(server2, "endtoend", "web.endtoend.*");
		ShrinkHelper.defaultDropinApp(server3, "endtoend", "web.endtoend.*");

		FATUtils.startServers(server, server2, server3);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server, server2, server3);

		DBTestBase.cleanupWSATTest(server);
		DBTestBase.cleanupWSATTest(server2);
		DBTestBase.cleanupWSATTest(server3);
	}

	@Test
	public void testOneway() {
		String method = "testOneway";
		try {
			String urlStr = BASE_URL + "/oneway/OnewayClientServlet"
					+ "?baseurl=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testOneway");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			// The fault exception can start with jakarta or jaxa depending
			// on if EE9 or before.
			assertTrue(
					"Cannot get expected exception from server",
					result.contains(".xml.ws.soap.SOAPFaultException:"
							+ " WS-AT can not work on ONE-WAY webservice method"));
			// List<String> errors = new ArrayList<String>();
			// errors.add("WTRN0127E");
			// server.addIgnoredErrors(errors);
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testTwoServerCommit() {
		String method = "testTwoServerCommit";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerCommit");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Finish Twoway message"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	@AllowedFFDC(value = {"javax.transaction.SystemException"})
	public void testTwoServerCommitClientVotingRollback() {
		String method = "testTwoServerCommitClientVotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerCommitClientVotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testTwoServerCommitProviderVotingRollback() {
		String method = "testTwoServerCommitProviderVotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerCommitProviderVotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testTwoServerRollback() {
		String method = "testTwoServerRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Finish Twoway message"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testTwoServerTwoCallCommit() {
		String method = "testTwoServerTwoCallCommit";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2
					+ "&baseurl2=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerTwoCallCommit");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Get expected result in the second call."));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testTwoServerTwoCallRollback() {
		String method = "testTwoServerTwoCallRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2
					+ "&baseurl2=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerTwoCallRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Get expected result in the second call."));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testThreeServerTwoCallCommit() {
		String method = "testThreeServerTwoCallCommit";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2
					+ "&baseurl2=" + BASE_URL3;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testThreeServerTwoCallCommit");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected reply from server, result = '" + result + "'",
					result.contains("Get expected result in the second call."));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testThreeServerTwoCallRollback() {
		String method = "testThreeServerTwoCallRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2
					+ "&baseurl2=" + BASE_URL3;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testThreeServerTwoCallRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected reply from server",
					result.contains("Get expected result in the second call."));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException"})
	public void testTwoServerTwoCallCoordinatorVotingRollback() {
		String method = "testTwoServerTwoCallCoordinatorVotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2 + "&baseurl2=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerTwoCallCoordinatorVotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException"})
	public void testThreeServerTwoCallCoordinatorVotingRollback() {
		String method = "testThreeServerTwoCallCoordinatorVotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2 + "&baseurl2=" + BASE_URL3;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testThreeServerTwoCallCoordinatorVotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testTwoServerTwoCallParticipant1VotingRollback() {
		String method = "testTwoServerTwoCallParticipant1VotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2 + "&baseurl2=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerTwoCallParticipant1VotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testThreeServerTwoCallParticipant1VotingRollback() {
		String method = "testThreeServerTwoCallParticipant1VotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2 + "&baseurl2=" + BASE_URL3;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testThreeServerTwoCallParticipant1VotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	@AllowedFFDC(value = {"javax.transaction.SystemException", "java.lang.IllegalStateException"})
	public void testTwoServerTwoCallParticipant2VotingRollback() {
		String method = "testTwoServerTwoCallParticipant2VotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2 + "&baseurl2=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testTwoServerTwoCallParticipant2VotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
	public void testThreeServerTwoCallParticipant2VotingRollback() {
		String method = "testThreeServerTwoCallParticipant2VotingRollback";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?baseurl=" + BASE_URL2 + "&baseurl2=" + BASE_URL3;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr), 
					HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT,"testThreeServerTwoCallParticipant2VotingRollback");
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			assertNotNull(result);
			Log.info(getClass(), method, "Result : " + result);
			assertTrue("Cannot get expected RollbackException from server, result = '" + result + "'",
					result.contains("Get expect RollbackException"));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testNoOptionalNoTransaction() {
		String method = "testNoOptionalNoTransaction";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=noOptionalNoTransaction&baseurl=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT, method);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			Log.info(getClass(), method, "Result: " + result);
			assertTrue("Expected \""+WSAT_DETECTED+"\" but got \""+result+"\"",
					result.contains(WSAT_DETECTED));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testFeatureDynamic() {
		String method = "testFeatureDynamic";
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=testTwoServerCommit&baseurl=" + BASE_URL;
			Log.info(getClass(), method, "URL: " + urlStr);
			URL url = new URL(urlStr);
			HttpURLConnection con = getHttpConnection(url,
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT, method);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			Log.info(getClass(), method, "First result: " + result);
			assertTrue("Expected \""+FINISH_TWOWAY_MESSAGE+"\" but got \""+result+"\"",
					result.contains(FINISH_TWOWAY_MESSAGE));

			try (AutoCloseable x = withoutFeatures("wsAtomicTransaction")) {
		        con = getHttpConnection(url,
		        		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT, method);
		        br = HttpUtils.getConnectionStream(con);
				result = br.readLine();
				Log.info(getClass(), method, "Second result: " + result);
				assertTrue("Expected \""+WSAT_NOT_INSTALLED+"\" but got \""+result+"\"",
						result.contains(WSAT_NOT_INSTALLED));
			}

	        con = getHttpConnection(url,
	        		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT, method);
	        br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
			Log.info(getClass(), method, "Third result: " + result);
			assertTrue("Expected \""+FINISH_TWOWAY_MESSAGE+"\" but got \""+result+"\"",
					result.contains(FINISH_TWOWAY_MESSAGE));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

    /**
     * Removes all of the listed features from the server.xml and returns an AutoClosable that restores the original configuration.
     * <p>
     * Can be used in a try-with-resources block to remove certain features within the block.
     * <p>
     * Features are matched ignoring the version to make it easier to use when tests are repeated.
     *
     * @param features the feature names to remove
     * @return an AutoClosable which will restore the original server configuration
     * @throws Exception if something goes wrong
     */
    private static AutoCloseable withoutFeatures(String... features) throws Exception {
    	String method = "withoutFeatures";
        ServerConfiguration config = server.getServerConfiguration();
        ServerConfiguration originalConfig = config.clone();
        List<String> featureRootsList = Arrays.stream(features)
                        .map(MultiServerTest::getRoot)
                        .collect(toList());
        int timeout = server.getConfigUpdateTimeout(); 

        Log.info(MultiServerTest.class, method, config.getFeatureManager().toString());
        config.getFeatureManager().getFeatures().removeIf(f -> featureRootsList.contains(getRoot(f)));
        Log.info(MultiServerTest.class, method, config.getFeatureManager().toString());
        try {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.setConfigUpdateTimeout(timeout * 2);
            server.waitForConfigUpdateInLogUsingMark(new HashSet<>(Arrays.asList("oneway", "endtoend")), true);
        } catch (Exception e) {
            try {
                server.updateServerConfiguration(originalConfig);
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }

        return () -> {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(originalConfig);
            server.waitForConfigUpdateInLogUsingMark(new HashSet<>(Arrays.asList("oneway", "endtoend")), true);
            server.setConfigUpdateTimeout(timeout);
        };
    }

    private static String getRoot(String featureName) {
        return featureName.replaceFirst("-\\d\\.\\d$", "").toLowerCase();
    }
}