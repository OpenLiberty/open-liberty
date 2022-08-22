/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.config.dsprops.testrules.DataSourcePropertiesOnlyRule;
import com.ibm.websphere.simplicity.config.dsprops.testrules.DataSourcePropertiesSkipRule;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.app.AssertionErrorSerializer;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;

/**
 * Utilities for running tests using {@link FATServlet}-style output.
 */
public class FATServletClient {
    public static final String SUCCESS = "SUCCESS";
    public static final String TEST_METHOD = "testMethod";

    @Rule
    public TestName testName = new TestName();

    public static DataSourcePropertiesOnlyRule onlyRule = new DataSourcePropertiesOnlyRule();
    public static DataSourcePropertiesSkipRule skipRule = new DataSourcePropertiesSkipRule();

    @Rule
    public TestRule dataSourcePropertiesSkipRule = skipRule;

    @Rule
    public TestRule dataSourcePropertiesOnlyRule = onlyRule;

    @Before
    public void logBeginTest() {
        Log.info(getClass(), testName.getMethodName(), ">>> BEGIN " + testName.getMethodName());
    }

    @After
    public void logEndTest() {
        Log.info(getClass(), testName.getMethodName(), "<<< END   " + testName.getMethodName());
    }

    /**
     * Runs a servlet test method.
     *
     * @param server   the started server containing the started application
     * @param testName the {@code @Rule public TestName testName} field
     * @param path     the servlet context and path (e.g., {@code "test"})
     */
    public static void runTest(LibertyServer server, String path, TestName testName) throws Exception {
        runTest(server, path, testName.getMethodName());
    }

    /**
     * Runs a servlet test method.
     *
     * @param server   the started server containing the started application
     * @param path     the servlet context and path (e.g., {@code "test"})
     * @param testName the servlet test method name
     */
    public static void runTest(LibertyServer server, String path, String testName) throws Exception {
        //HttpUtils.findStringInReadyUrl(server, getPathAndQuery(path, testName), FATServletClient.SUCCESS);
        String response = HttpUtils.getHttpResponseAsString(server, getPathAndQuery(path, testName));
        if (!response.contains(FATServletClient.SUCCESS)) {
            if (response.contains(AssertionErrorSerializer.START_TAG) &&
                response.contains(AssertionErrorSerializer.END_TAG)) {
                AssertionError error = parseAssertionError(response);
                throw error;
            }
            fail(response);
        }
    }

    /**
     * Parse and deserialize a response string to extract an AssertionError instance.
     * The response string must contain some JSON that represents a serialized AssertionError.
     * The JSON String is wrapped by START_TAG and END_TAG
     *
     * @param  response              The response String that contains the serialized AssertionError as json
     * @return                       an instance of AssertionError
     * @throws IllegalStateException if the START_TAG or END_TAG can not be found in the response string
     */
    private static AssertionError parseAssertionError(String response) {

        int startIdx = response.indexOf(AssertionErrorSerializer.START_TAG);
        int endIdx = response.indexOf(AssertionErrorSerializer.END_TAG);

        if (startIdx < 0 || endIdx < 0) {
            throw new IllegalStateException("AssertionError tags not found in response: " + response);
        }

        String json = response.substring(startIdx + AssertionErrorSerializer.START_TAG.length(), endIdx);

        AssertionError e = AssertionErrorSerializer.deserialize(json);
        return e;
    }

    /**
     * Runs a servlet test method and verifies that the expected response code was returned.
     *
     * @param server             the started server containing the started application
     * @param path               the servlet context and path (e.g., {@code "test"})
     * @param testName           the servlet test method name
     * @param allowedReturnCodes an array of allowed HttpUrlConnection response codes
     */
    public static int runTestForResponseCode(LibertyServer server, String path, String testName) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, getPathAndQuery(path, testName));
        int rc = con.getResponseCode();
        con.disconnect();
        return rc;
    }

    /**
     * Runs a test in the servlet and returns the servlet output.
     *
     * @param  server      the started server containing the started application
     * @param  path        the url path (e.g. myApp/myServlet)
     * @param  queryString query string including at least the test name
     *                         (e.g. testName or testname&key=value&key=value)
     * @return             output of the servlet
     */
    public StringBuilder runTestWithResponse(LibertyServer server, String path, String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + getPathAndQuery(path, queryString));
        Log.info(getClass(), testName.getMethodName(), "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(getClass(), "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf(FATServletClient.SUCCESS) < 0) {
                Log.info(getClass(), testName.getMethodName(), "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }
            return lines;
        } finally {
            con.disconnect();
        }
    }

    /**
     * Returns the test method name without the RepeatTests suffix.
     *
     * For example, when using RepeatTests with EE7_FEATURES, the suffix _EE7_FEATURES is added
     * to provide unique test names for junit reporting purposes. The simple test method name
     * dose not include the suffix.
     *
     * @return test method name without the RepeatTests suffix.
     */
    public String getTestMethodSimpleName() {
        return getTestMethodSimpleName(testName);
    }

    /**
     * Returns the test method name without the RepeatTests suffix.
     *
     * For example, when using RepeatTests with EE7_FEATURES, the suffix _EE7_FEATURES is added
     * to provide unique test names for junit reporting purposes. The simple test method name
     * dose not include the suffix.
     *
     * @return test method name without the RepeatTests suffix.
     */
    public static String getTestMethodSimpleName(TestName testName) {
        String testMethodName = testName.getMethodName();
        String currentAction = RepeatTestFilter.getRepeatActionsAsString();
        if (currentAction != null && testMethodName.endsWith(currentAction)) {
            testMethodName = testMethodName.substring(0, testMethodName.length() - (currentAction.length()));
        }

        return testMethodName;
    }

    /**
     * Return the path and query for a servlet test method URL.
     *
     * @param  path     the servlet context and path (e.g., {@code "test"})
     * @param  testName the test name
     * @return          the path and query (e.g., {@code "/test?testMethod=test"})
     */
    public static String getPathAndQuery(String path, String testName) {
        if (!path.contains("?")) {
            return '/' + path + '?' + FATServletClient.TEST_METHOD + '=' + testName;
        } else {
            return '/' + path + '&' + FATServletClient.TEST_METHOD + '=' + testName;
        }
    }
}
