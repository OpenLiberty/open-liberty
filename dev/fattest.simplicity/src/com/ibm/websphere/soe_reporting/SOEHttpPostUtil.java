/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.soe_reporting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.websphere.simplicity.log.Log;

/* The SOE team run several web servlets which are used to collect build data for analytics purposes.
 * They/we have added a new kind of datum for reporting soft/transient failures, 
 * DB access for storing and retrieving those reports is provided via those servlets.
 * 
 * This class acts as a reporting client for to that system. 
 * Its logic is adapted from that used by several custom Ant tasks in the build to send data 
 * to the SOE servlets via an HTTP POST request.
 * 
 * At the time I'm writing this, the only reports Simplicity is submitting through this channel
 * are "soft timeouts" from log scraping actions in LibertyServer.
 * 
 * @see LibertyServer.waitForStringInLog(String regexp, long intendedTimeout, long extendedTimeout, RemoteFile outputFile)
 */
public class SOEHttpPostUtil {
    private static final Class<?> thisClass = SOEHttpPostUtil.class;
    private static final String CLASS_NAME = thisClass.getName();
    private static Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final int CONNECTION_TIMEOUT_MILLISECONDS = 10000;
    private static final String UTF8 = "UTF-8";

    // Test server: "http://ltestw40.hursley.ibm.com:9080/soe_LogScrapeTimeout/CollectLogScrapeTimeout"
    // Production server: "http://howler.hursley.ibm.com:9080/LogScrapeTimeoutCollection/CollectLogScrapeTimeout"
    // Note: bnd wants property names to be in lowercase. 
    private static final String SOE_SOFTFAILURE_REPORT_SERVER =
                    System.getProperty("soe.softfailure.report.server");
    private static final String LOGFAILURE_REPORT_SERVER =
                    (SOE_SOFTFAILURE_REPORT_SERVER == null)
                                    ? null
                                    : SOE_SOFTFAILURE_REPORT_SERVER + "/LogScrapeTimeoutCollection/CollectLogScrapeTimeout";

    protected HttpPostResponse send(String urlString, Map<String, String> parameters) {

        HttpPostResponse response = null;
        try {
            URL url = new URL(urlString);
            String encodedParameterString = createParameterContentString(parameters);
            response = null;

            HttpURLConnection connection = connect(url, encodedParameterString);
            if (connection != null) {
                writeParameters(connection, encodedParameterString);
                response = readResponse(connection);
            }
        } catch (IOException e) {
            Log.error(thisClass, "send", e);
            e.printStackTrace();
            return null;
        }

        return response;
    }

    private HttpURLConnection connect(URL url, String encodedParameterString) throws IOException {
        HttpURLConnection connection = createConnection(url, encodedParameterString);
        return connection;
    }

    private HttpURLConnection createConnection(URL url, String encodedParameterString) throws IOException {
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        connection.setRequestProperty("Content-Length", "" + encodedParameterString.length());
        return connection;
    }

    private void writeParameters(HttpURLConnection connection, String encodedParameterString) throws IOException {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(encodedParameterString);
            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private HttpPostResponse readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();

        StringBuilder responseContentBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line = null;

        while ((line = br.readLine()) != null) {
            responseContentBuilder.append(line);
        }

        return new HttpPostResponse(responseCode, responseMessage, responseContentBuilder.toString());
    }

    private String createParameterContentString(Map<String, String> parameters) throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append('&');
            }
            stringBuilder.append(encodeStringUTF8(entry.getKey()));
            stringBuilder.append('=');
            stringBuilder.append(encodeStringUTF8(entry.getValue()));
        }

        Log.debug(thisClass, "Sending String: " + stringBuilder.toString());
        return stringBuilder.toString();
    }

    private String encodeStringUTF8(String string) throws UnsupportedEncodingException {
        return URLEncoder.encode(string, UTF8);
    }

    protected static final class HttpPostResponse {

        private final int responseCode;
        private final String responseMessage;
        private final String content;

        public HttpPostResponse(int responseCode, String returnMessage, String content) {
            this.responseCode = responseCode;
            this.responseMessage = returnMessage;
            this.content = content;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * This can be called to send reports of "soft failures" to the SOE server, for use in finer-grained analysis of builds.
     * It's defined in conjunction with the infra.buildtasks folks; see RTC Work Items 116585 and 122141
     */

    // Environmental "constants" for a given test run
    // If any is absent (ie, if we are on a developer's personal machine), do not log the soft timeout.
    static final String _buildEngineName = System.getProperty("build.engine.name");
    static final String _operatingSystem = System.getProperty("os.name");
    static final String _buildLabel = System.getProperty("buildServerLabel");
    static final String _workspaceName = System.getProperty("team.scm.workspace");
    static final String _personalBuild = (null != System.getProperty("personalBuild")) ? "1" : "0";
    static final String _osArch = System.getProperty("os.arch");
    static final String _jvmVersion = System.getProperty("java.version");
    static final String _jvmVendor = System.getProperty("java.vendor");
    static final boolean _enableReport =
                    null != _buildEngineName && null != _operatingSystem && null != _buildLabel &&
                                    null != _workspaceName && null != _osArch && null != _jvmVersion && null != _jvmVendor;

    public static void reportSoftLogTimeoutToSOE(String testClass, String testMethod, int testLine, long timeWaited_msec, String regEx) {
        if (!_enableReport)
            return; // No-op on developer machine.

        SOEHttpPostUtil post = new SOEHttpPostUtil();

        Map<String, String> params = new HashMap<String, String>();

        // @formatter:off                                                                          // INTERNAL REPRESENTATIONS/CONSTRAINTS
        // Environment constants
        params.put(ResultCollectionConstants.PARAM_BUILD_ENGINE_NAME,     _buildEngineName);               // String; max length 100.
        params.put(ResultCollectionConstants.PARAM_WORKSPACE,             _workspaceName);                 // String; max length 50.
        params.put(ResultCollectionConstants.PARAM_IS_PERSONAL_BUILD,     _personalBuild);                 // short
        params.put(ResultCollectionConstants.PARAM_JVM_ARCHITECTURE,      _osArch);                        // String; max length 20.
        params.put(ResultCollectionConstants.PARAM_JVM_VENDOR,            _jvmVendor);                     // String; max length 50.
        params.put(ResultCollectionConstants.PARAM_JVM_VERSION,           _jvmVersion);                    // String; max length 50.
        params.put(ResultCollectionConstants.PARAM_OS_ARCHITECTURE,       _osArch);                        // String; max length 100.
        params.put(ResultCollectionConstants.PARAM_OS_NAME,               _operatingSystem);               // String; max length 100.
        params.put(ResultCollectionConstants.PARAM_TEST_BUILD_LABEL,      _buildLabel);                    // String; max length 200.

        // Event type (fixed, in this utility function)
        params.put(ResultCollectionConstants.PARAM_FAILURE_TYPE,          "SoftLogScrapeTimeout");         // String; max length 50.

        // Event properties (params and runtime context)
        params.put(ResultCollectionConstants.PARAM_TIME_OCCURRED,         Long.toString(System.currentTimeMillis())); // long (converted to java.sql.Timestamp when stored). 
        params.put(ResultCollectionConstants.PARAM_TEST_CLASS,            testClass);                      // String; max length 100.     
        params.put(ResultCollectionConstants.PARAM_TEST_LINE,             Integer.toString(testLine));     // int
        params.put(ResultCollectionConstants.PARAM_TEST_METHOD,           testMethod);                     // String; max length 100.
        params.put(ResultCollectionConstants.PARAM_SEARCH_REGEX,          regEx);                          // String; max length 100.
        params.put(ResultCollectionConstants.PARAM_TIME_WAITED_MILLIS,    Long.toString(timeWaited_msec)); // int
        // @formatter:on

        HttpPostResponse response = post.send(LOGFAILURE_REPORT_SERVER, params);

        Log.debug(thisClass, "Reported to " + LOGFAILURE_REPORT_SERVER + " that " + testClass + "." + testMethod + "[" + testLine + "]: exceeded " + timeWaited_msec
                             + " finding "
                             + regEx);
        Log.debug(thisClass, "Got response code: " + response.getResponseCode());
        Log.debug(thisClass, "Got response message: " + response.getResponseMessage());
    }

}