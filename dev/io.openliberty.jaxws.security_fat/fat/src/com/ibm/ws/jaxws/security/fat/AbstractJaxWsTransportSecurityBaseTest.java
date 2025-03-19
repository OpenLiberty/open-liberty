/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * The basic test class for all the transport security test cases classes
 */
abstract public class AbstractJaxWsTransportSecurityBaseTest {
    private static final int REQUEST_TIMEOUT = 10;

    private static final int WAIT_TIME_OUT = 10 * 1000;

    protected static String lastServerConfig;

    @Server("JaxWsTransportSecurityServer")
    public static LibertyServer server;

    protected static final String SERVER_TMP_DIR = "tmp";

    protected static final String WEB_XML_IN_PROVIDER_WAR = "apps/TransportSecurityProvider.ear/TransportSecurityProvider.war/WEB-INF/web.xml";

    protected static final String CUSTOMIZE_BND_FILE = "apps/TransportSecurityClient.war/WEB-INF/ibm-ws-bnd.xml";

    protected String SERVLET_PATH = "/TransportSecurityClient/TestTransportSecurityServlet";

    protected static final List<String> noSSLResps = new ArrayList<String>(2);

    @Rule
    public TestName testName = new TestName();

    // the value is use to switch whether need use dynamic update the server.xml and
    // applications,
    // now, using false as the http chain for ssl is not stable.
    // if is false, will start and stop server for every test case.
    protected static boolean dynamicUpdate = false;

    public static void buildDefaultApps() throws Exception {
        ExplodedShrinkHelper.explodedApp(server, "TransportSecurityClient",
                                         "com.ibm.ws.jaxws.transport.client.security.servlet", "com.ibm.ws.jaxws.transport.security");

        WebArchive providerWar = ExplodedShrinkHelper.explodedApp(server, "TransportSecurityProvider",
                                                                  "com.ibm.ws.jaxws.transport.server.security.impl", "com.ibm.ws.jaxws.transport.server.security");

        ExplodedShrinkHelper.explodedEarApp(server, providerWar, "TransportSecurityProvider",
                                            "TransportSecurityProvider", true, "com.ibm.ws.jaxws.transport.server.security.impl",
                                            "com.ibm.ws.jaxws.transport.server.security");

    }

    public static void buildDefaultDropInApps() throws Exception {
        ExplodedShrinkHelper.explodedDropinApp(server, "TransportSecurityClient",
                                               "com.ibm.ws.jaxws.transport.client.security.servlet", "com.ibm.ws.jaxws.transport.security");

        ExplodedShrinkHelper.explodedDropinApp(server, "TransportSecurityProvider",
                                               "com.ibm.ws.jaxws.transport.server.security.impl", "com.ibm.ws.jaxws.transport.server.security");

    }

    /**
     * Start server
     *
     * @param serverLog
     * @param serverConfigFile
     * @param providerWebXMLFile
     * @param clientBindingFile
     * @throws Exception
     */
    protected static void startServer(String serverLog, String serverConfigFile, String providerWebXMLFile,
                                      String clientBindingFile) throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        // update server.xml
        if (null != serverConfigFile) {
            tryUpdateServerRootFile("server.xml", serverConfigFile);
        }
        // update web.xml in provider application
        tryUpdateServerRootFile(WEB_XML_IN_PROVIDER_WAR, providerWebXMLFile);
        // update ibm-ws-bnd.xml in client application
        tryUpdateServerRootFile(CUSTOMIZE_BND_FILE, clientBindingFile);

        server.startServer(serverLog);
        assertNotNull("The server JaxWsTransportSecurityServer did not appear to have started",
                      server.waitForStringInLog("CWWKF0011I.*JaxWsTransportSecurityServer"));
    }

    /**
     * Try to update the file under server root
     *
     * @param oldFile the relative path to ${wlp.user.output}
     * @param newFile the relative path to
     *            publish/files/JaxWsTransportSecurityServer/
     * @return
     * @throws Exception
     */
    protected static boolean tryUpdateServerRootFile(String oldFile, String newFile) throws Exception {
        if (null == newFile) {
            // try to delete the web.xml
            try {
                RemoteFile file = server.getFileFromLibertyServerRoot(oldFile);
                int count = 0;
                boolean success = false;
                // try delete file 5 times, the wait time is 1s
                while (!(success = file.delete()) && count < 5) {
                    Thread.sleep(1000);
                    ++count;
                    Log.info(AbstractJaxWsTransportSecurityBaseTest.class, "tryUpdateServerFile",
                             "try delete " + oldFile + " in " + count + " times");
                }
                if (!success) {
                    Log.warning(AbstractJaxWsTransportSecurityBaseTest.class, "Could not delete file: " + oldFile);
                    return false;
                } else {
                    Log.info(AbstractJaxWsTransportSecurityBaseTest.class, "tryUpdateServerFile",
                             "Successfully delete file:" + oldFile);
                }
            } catch (FileNotFoundException e) {
                // if no file, need not update the file, so just return false
                Log.info(AbstractJaxWsTransportSecurityBaseTest.class, "tryUpdateServerFile",
                         "The file is inexistent:" + oldFile);
                return false;
            }
        } else {
            updateSingleFileInServerRoot(oldFile, newFile);
        }
        return true;
    }

    /**
     * Update the the web.xml in provider application
     *
     * @param newWebXML the relative path to
     *            publish/files/JaxWsTransportSecurityServer/
     * @throws Exception
     */
    protected static void updateProviderWEBXMLFile(String newWebXML) throws Exception {
        updateProviderWEBXMLFile(newWebXML, true);
    }

    /**
     * Update the the web.xml in provider application
     *
     * @param newWebXML
     * @param warningWhenFail just log the warning message when the update operation
     *            is failed
     * @throws Exception
     */
    protected static void updateProviderWEBXMLFile(String newWebXML, boolean warningWhenFail) throws Exception {
        server.setMarkToEndOfLog();
        if (!tryUpdateServerRootFile(WEB_XML_IN_PROVIDER_WAR, newWebXML)) {
            return;
        }
        // wait for app started/updated message
        boolean isFound = null != server.waitForStringInLogUsingMark(
                                                                     "CWWKZ0001I.*TransportSecurityProvider | CWWKZ0003I.*TransportSecurityProvider", WAIT_TIME_OUT);
        if (!isFound) {
            if (warningWhenFail) {
                Log.warning(AbstractJaxWsTransportSecurityBaseTest.class,
                            "The application TransportSecurityProvider did not appear to have updated.");
                return;
            }
            fail("The application TransportSecurityProvider did not appear to have updated");
        }
    }

    /**
     * Update the binding file in client application
     *
     * @param newBndFile the relative path to
     *            publish/files/JaxWsTransportSecurityServer/
     * @throws Exception
     */
    protected static void updateClientBndFile(String newBndFile) throws Exception {
        updateClientBndFile(newBndFile, true);
    }

    /**
     * Update the binding file in client application
     *
     * @param newBndFile
     * @param warningWhenFail just log the warning message when the update operation
     *            is failed
     * @throws Exception
     */
    protected static void updateClientBndFile(String newBndFile, boolean warningWhenFail) throws Exception {
        server.setMarkToEndOfLog();
        server.deleteFileFromLibertyServerRoot(CUSTOMIZE_BND_FILE);
        if (!tryUpdateServerRootFile(CUSTOMIZE_BND_FILE, newBndFile)) {
            return;
        }
        // wait for app started/updated message
        boolean isFound = null != server.waitForStringInLogUsingMark(
                                                                     "CWWKZ0001I.*TransportSecurityClient | CWWKZ0003I.*TransportSecurityClient", WAIT_TIME_OUT);
        if (!isFound) {
            if (warningWhenFail) {
                Log.warning(AbstractJaxWsTransportSecurityBaseTest.class,
                            "The application TransportSecurityClient did not appear to have updated.");
                return;
            }
            fail("The application TransportSecurityClient did not appear to have updated.");
        }
    }

    /**
     * Update the file in the server root
     *
     * @param destFilePath the relative path to server root, such as
     *            "dropins/test.war.xml"
     * @param originFilePath the relative path to
     *            publish/files/JaxWsTransportSecurityServer/
     * @throws Exception
     */
    protected static void updateSingleFileInServerRoot(String destFilePath, String originFilePath) throws Exception {
        server.copyFileToLibertyServerRoot(SERVER_TMP_DIR, "JaxWsTransportSecurityServer/" + originFilePath);
        RemoteFile tmpAbsFile = null;
        try {
            int index = originFilePath.lastIndexOf("/");
            if (index >= 0) {
                originFilePath = originFilePath.substring(index + 1);
            }
            tmpAbsFile = server.getFileFromLibertyServerRoot(SERVER_TMP_DIR + "/" + originFilePath);
            LibertyFileManager.moveFileIntoLiberty(server.getMachine(), server.getServerRoot(), destFilePath,
                                                   tmpAbsFile.getAbsolutePath());
        } finally {
            if (null != tmpAbsFile) {
                tmpAbsFile.delete();
            }
        }
    }

    /**
     * Run test
     *
     * @param params the request parameters
     * @param expectedResponses the expected responses from connection
     * @param expectedServerInfos the expected server output in log, the info's
     *            order is very important
     * @throws ProtocolException
     * @throws MalformedURLException
     * @throws IOException
     */
    protected void runTest(List<RequestParams> params, List<String> expectedServerInfos) throws Exception, ProtocolException, MalformedURLException, IOException {
//        server.setMarkToEndOfLog();

        for (RequestParams param : params) {
            StringBuilder urlBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append(SERVLET_PATH).append("?user=").append(param.userName).append("&serviceType=").append(param.serviceType).append("&schema=").append(param.schema).append("&port=").append(param.port).append("&path=").append(param.path).append("&testMethod=").append(testName.getMethodName());
            if (null != param.testMode) {
                urlBuilder.append("&testMode=").append(param.testMode.getValue());
            }

            if (null != param.expectedResp) {
                assertTrue(printExpectedResponses(param.expectedResp, false),
                           checkExpectedResponses(urlBuilder.toString(), param.expectedResp, false));
            }
        }

        if (null != expectedServerInfos) {
            for (String info : expectedServerInfos) {
                assertNotNull("The expected output in server log is " + info, server.waitForStringInLog(info));
            }
        }
    }

    private boolean checkExpectedResponses(String servletUrl, List<String> expectedResponses, boolean exact) throws IOException {
        Log.info(this.getClass(), testName.getMethodName(), "Calling Application with URL=" + servletUrl);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(servletUrl), HttpURLConnection.HTTP_OK,
                                                            REQUEST_TIMEOUT);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

        String responseContent = sb.toString();
        if (exact) { // the response content must contain all the expect strings
            for (String expectStr : expectedResponses) {
                if (!responseContent.contains(expectStr)) {
                    return false;
                }
            }
            return true;
        } else { // the response content could contain one of the expect strings
            for (String expectStr : expectedResponses) {
                if (responseContent.contains(expectStr)) {
                    return true;
                }
            }
            return false;
        }
    }

    private String printExpectedResponses(List<String> expectedResponses, boolean exact) {
        StringBuilder sb = new StringBuilder("The expected output in server log is ");
        if (!exact && expectedResponses.size() > 1) {
            sb.append("one of ");
        }
        sb.append("[");
        for (int i = 0; i < expectedResponses.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"" + expectedResponses.get(i) + "\"");
        }
        sb.append("]");
        return sb.toString();
    }

    protected static class RequestParams {
        protected String userName;
        protected TestMode testMode;
        protected String serviceType;
        protected String schema;
        protected int port;
        protected String path;
        protected List<String> expectedResp;

        /**
         * @return the expectedResp
         */
        public List<String> getExpectedResp() {
            return expectedResp;
        }

        /**
         * @param expectedResp the expectedResp to set
         */
        public void setExpectedResp(List<String> expectedResp) {
            this.expectedResp = expectedResp;
        }

        /**
         * @param expectedResp the expectedResp to set
         */
        public void setExpectedResp(String expectedResp) {
            this.expectedResp = new ArrayList<String>();
            this.expectedResp.add(expectedResp);
        }

        /**
         * @param userName
         * @param serviceType
         * @param schema
         * @param port
         * @param path
         */
        public RequestParams(String userName, String serviceType, String schema, int port, String path, String expectedResp) {
            super();
            this.userName = userName;
            this.serviceType = serviceType;
            this.schema = schema;
            this.port = port;
            this.path = path;
            this.expectedResp = new ArrayList<String>();
            this.expectedResp.add(expectedResp);
        }

        /**
         * @param userName
         * @param serviceType
         * @param schema
         * @param port
         * @param path
         */
        public RequestParams(String userName, String serviceType, String schema, int port, String path, List<String> expectedResp) {
            super();
            this.userName = userName;
            this.serviceType = serviceType;
            this.schema = schema;
            this.port = port;
            this.path = path;
            this.expectedResp = expectedResp;
        }

        /**
         * @param userName
         * @param testMode
         * @param serviceType
         * @param schema
         * @param port
         * @param path
         */
        public RequestParams(String userName, TestMode testMode, String serviceType, String schema, int port,
                             String path, String expectedResp) {
            super();
            this.userName = userName;
            this.testMode = testMode;
            this.serviceType = serviceType;
            this.schema = schema;
            this.port = port;
            this.path = path;
            this.expectedResp = new ArrayList<String>();
            this.expectedResp.add(expectedResp);
        }

    }

    protected enum TestMode {
        PORT("port"), DISPATCH("dispatch");

        private final String innerValue;

        private TestMode(String value) {
            this.innerValue = value;
        }

        public String getValue() {
            return this.innerValue;
        }
    }

}
