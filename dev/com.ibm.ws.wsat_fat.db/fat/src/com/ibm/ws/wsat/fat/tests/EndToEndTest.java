/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.wsat.fat.tests;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class EndToEndTest extends WSATTest {
	private static LibertyServer server = LibertyServerFactory
			.getLibertyServer("WSATBasic");
	private static String BASE_URL = "http://" + server.getHostname() + ":"
			+ server.getHttpDefaultPort();
	private final static int REQUEST_TIMEOUT = 60;

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
                        .map(EndToEndTest::getRoot)
                        .collect(toList());

        Log.info(EndToEndTest.class, method, config.getFeatureManager().toString());
        config.getFeatureManager().getFeatures().removeIf(f -> featureRootsList.contains(getRoot(f)));
        Log.info(EndToEndTest.class, method, config.getFeatureManager().toString());
        try {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);
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
            server.waitForConfigUpdateInLogUsingMark(null, true);
        };
    }

    private static String getRoot(String featureName) {
        return featureName.replaceFirst("-\\d\\.\\d$", "").toLowerCase();
    }

	@BeforeClass
	public static void beforeTests() throws Exception {
		ShrinkHelper.defaultDropinApp(server, "oneway", "com.ibm.ws.wsat.oneway.*");
		ShrinkHelper.defaultDropinApp(server, "endtoend", "com.ibm.ws.wsat.endtoend.*");

		FATUtils.startServers(server);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		FATUtils.stopServers(server);
	}

	@Test
	public void testNoOptionalNoTransaction() {
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=noOptionalNoTransaction&baseurl=" + BASE_URL;
			Log.info(getClass(), testName.getMethodName(), "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			Log.info(getClass(), testName.getMethodName(), "Result: " + result);
			assertTrue("Expected \""+WSAT_DETECTED+"\" but got \""+result+"\"",
					result.contains(WSAT_DETECTED));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}

	@Test
	public void testFeatureDynamic() {
		try {
			String urlStr = BASE_URL + "/endtoend/EndToEndClientServlet"
					+ "?type=testTwoServerCommit&baseurl=" + BASE_URL;
			Log.info(getClass(), testName.getMethodName(), "URL: " + urlStr);
			HttpURLConnection con = getHttpConnection(new URL(urlStr),
							HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
			BufferedReader br = HttpUtils.getConnectionStream(con);
			String result = br.readLine();
			Log.info(getClass(), testName.getMethodName(), "First result: " + result);
			assertTrue("Expected \""+FINISH_TWOWAY_MESSAGE+"\" but got \""+result+"\"",
					result.contains(FINISH_TWOWAY_MESSAGE));

			try (AutoCloseable x = withoutFeatures("wsAtomicTransaction")) {
				server.validateAppsLoaded();
		        con = getHttpConnection(new URL(urlStr),
		        		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
		        br = HttpUtils.getConnectionStream(con);
				result = br.readLine();
				Log.info(getClass(), testName.getMethodName(), "Second result: " + result);
				assertTrue("Expected \""+WSAT_NOT_INSTALLED+"\" but got \""+result+"\"",
						result.contains(WSAT_NOT_INSTALLED));
			}

	        con = getHttpConnection(new URL(urlStr),
	        		HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
	        br = HttpUtils.getConnectionStream(con);
			result = br.readLine();
			Log.info(getClass(), testName.getMethodName(), "Third result: " + result);
			assertTrue("Expected \""+FINISH_TWOWAY_MESSAGE+"\" but got \""+result+"\"",
					result.contains(FINISH_TWOWAY_MESSAGE));
		} catch (Exception e) {
			fail("Exception happens: " + e.toString());
		}
	}
}
