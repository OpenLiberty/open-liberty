/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * The basic test class for all the transport security test cases classes
 */
abstract public class AbstractJaxWsTransportSecurityTest {
    private static final int REQUEST_TIMEOUT = 10;

    private static final int WAIT_TIME_OUT = 10 * 1000;

    private static final Set<String> SERVER_CONFIG_WITH_SSL = new HashSet<String>();

    private static final Set<String> SERVER_CONFIG_WITHOUT_SSL = new HashSet<String>();

    protected static String lastServerConfig;

    @Server("JaxWsTransportSecurityServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("JaxWsTransportSecurityServer");

    protected static final String SERVER_TMP_DIR = "tmp";

    protected static final String WEB_XML_IN_PROVIDER_WAR = "apps/TransportSecurityProvider.ear/TransportSecurityProvider.war/WEB-INF/web.xml";

    protected static final String CUSTOMIZE_BND_FILE = "apps/TransportSecurityClient.war/WEB-INF/ibm-ws-bnd.xml";

    protected String SERVLET_PATH = "/TransportSecurityClient/TestTransportSecurityServlet";

    // the value is use to switch whether need use dynamic update the server.xml and applications,
    // now, using false as the http chain for ssl is not stable.
    // if is false, will start and stop server for every test case.
    protected static boolean dynamicUpdate = false;

    static {
        SERVER_CONFIG_WITHOUT_SSL.add("serverConfigs/basicAuthWithoutSSL.xml");
        SERVER_CONFIG_WITHOUT_SSL.add("serverConfigs/noAppSecurityFeature.xml");
        SERVER_CONFIG_WITHOUT_SSL.add("serverConfigs/noSSLConfiguration.xml");

        SERVER_CONFIG_WITH_SSL.add("serverConfigs/basicAuthWithSSL.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/clientCertFailOverToBasicAuthConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/customizeSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/defaultClientCertConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/defaultSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/noTrustStoreInCustomizeSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/noTrustStoreInDefaultSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/noValidTrustCertInCustomizeSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/patchyServerTrustStoreConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/sslRefInSSLDefaultConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/withAliasClientCertConfiguration.xml");

    }
    @Rule
    public TestName testName = new TestName();

    /**
     * Start server
     * 
     * @param serverLog
     * @param serverConfigFile
     * @param providerWebXMLFile
     * @param clientBindingFile
     * @throws Exception
     */
    protected static void startServer(String serverLog, String serverConfigFile, String providerWebXMLFile, String clientBindingFile) throws Exception {
        // update server.xml
        if (null != serverConfigFile) {
            tryUpdateServerRootFile("server.xml", serverConfigFile);
        }
        // update web.xml in provider application
        tryUpdateServerRootFile(WEB_XML_IN_PROVIDER_WAR, providerWebXMLFile);
        // update ibm-ws-bnd.xml in client application
        tryUpdateServerRootFile(CUSTOMIZE_BND_FILE, clientBindingFile);

        server.startServer(serverLog);
        server.setMarkToEndOfLog();
        assertNotNull("The server JaxWsTransportSecurityServer did not appear to have started",
                      server.waitForStringInLog("CWWKF0011I.*JaxWsTransportSecurityServer"));
    }

    /**
     * Try to update the file under server root
     * 
     * @param oldFile the relative path to ${wlp.user.output}
     * @param newFile the relative path to publish/files/JaxWsTransportSecurityServer/
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
                    Log.info(AbstractJaxWsTransportSecurityTest.class, "tryUpdateServerFile", "try delete " + oldFile + " in " + count + " times");
                }
                if (!success) {
                    Log.warning(AbstractJaxWsTransportSecurityTest.class, "Could not delete file: " + oldFile);
                    return false;
                } else {
                    Log.info(AbstractJaxWsTransportSecurityTest.class, "tryUpdateServerFile", "Successfully delete file:" + oldFile);
                }
            } catch (FileNotFoundException e) {
                // if no file, need not update the file, so just return false
                Log.info(AbstractJaxWsTransportSecurityTest.class, "tryUpdateServerFile", "The file is inexistent:" + oldFile);
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
     * @param newWebXML the relative path to publish/files/JaxWsTransportSecurityServer/
     * @throws Exception
     */
    protected static void updateProviderWEBXMLFile(WebArchive webApp, String newWebXML) throws Exception {
        updateProviderWEBXMLFile(webApp, newWebXML, true);
    }

    /**
     * Update the the web.xml in provider application
     * 
     * @param newWebXML
     * @param warningWhenFail just log the warning message when the update operation is failed
     * @throws Exception
     */
    protected static void updateProviderWEBXMLFile(WebArchive webApp, String newWebXML, boolean warningWhenFail) throws Exception {
        server.setMarkToEndOfLog();
        //if (!tryUpdateServerRootFile(WEB_XML_IN_PROVIDER_WAR, newWebXML)) {
        //    return;
        //}
        webApp.addAsWebInfResource(new File("lib/LibertyFATTestFiles", newWebXML), "web.xml");
        ShrinkHelper.exportAppToServer(server, webApp, DeployOptions.OVERWRITE);
        // wait for app started/updated message
        boolean isFound = null != server.waitForStringInLogUsingMark("CWWKZ0001I.*TransportSecurityProvider | CWWKZ0003I.*TransportSecurityProvider", WAIT_TIME_OUT);
        if (!isFound) {
            if (warningWhenFail) {
                Log.warning(AbstractJaxWsTransportSecurityTest.class, "The application TransportSecurityProvider did not appear to have updated.");
                return;
            }
            fail("The application TransportSecurityProvider did not appear to have updated");
        }
    }

    /**
     * Update the binding file in client application
     * 
     * @param newBndFile the relative path to publish/files/JaxWsTransportSecurityServer/
     * @throws Exception
     */
    protected static void updateClientBndFile(WebArchive webApp, String newBndFile) throws Exception {
        updateClientBndFile(webApp, newBndFile, true);
    }

    /**
     * Update the binding file in client application
     * 
     * @param newBndFile
     * @param warningWhenFail just log the warning message when the update operation is failed
     * @throws Exception
     */
    protected static void updateClientBndFile(WebArchive webApp, String newBndFile, boolean warningWhenFail) throws Exception {
        server.setMarkToEndOfLog();
        //if (!tryUpdateServerRootFile(CUSTOMIZE_BND_FILE, newBndFile)) {
        //    return;
        //}
        webApp.addAsWebInfResource(new File("lib/LibertyFATTestFiles", newBndFile), "ibm-ws-bnd.xml");
        ShrinkHelper.exportAppToServer(server, webApp, DeployOptions.OVERWRITE);
        // wait for app started/updated message
        boolean isFound = null != server.waitForStringInLogUsingMark("CWWKZ0001I.*TransportSecurityClient | CWWKZ0003I.*TransportSecurityClient", WAIT_TIME_OUT);
        if (!isFound) {
            if (warningWhenFail) {
                Log.warning(AbstractJaxWsTransportSecurityTest.class, "The application TransportSecurityClient did not appear to have updated.");
                return;
            }
            fail("The application TransportSecurityClient did not appear to have updated.");
        }
    }

    /**
     * Update the server.xml
     * 
     * @param newServerConfigFile the relative path to publish/files/JaxWsTransportSecurityServer/
     * @throws Exception
     */
    protected static void updateServerConfigFile(String newServerConfigFile) throws Exception {
        updateServerConfigFile(newServerConfigFile, true);

    }

    /**
     * Update the server.xml
     * 
     * @param newServerConfigFile
     * @param checkAppUpdate only check for Provider app update when specified
     * @throws Exception
     */
    protected static void updateServerConfigFile(String newServerConfigFile, boolean checkAppUpdate) throws Exception {
        // just log the warning message when the update operation is failed
        boolean warningWhenFail = true;

        if (null == newServerConfigFile) {
            Log.warning(AbstractJaxWsTransportSecurityTest.class, "The server configuration could not be updated as the new configuration file is Null.");
            return;
        }
        try {
            updateSingleFileInServerRoot("server.xml", newServerConfigFile);

            boolean isFound = null != server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*");
            if (!isFound) {
                if (warningWhenFail) {
                    Log.warning(AbstractJaxWsTransportSecurityTest.class, "The server configuration does not update.");
                } else {
                    fail("The server configuration does not update.");
                }
            }

            if (null == lastServerConfig) {// The first time to update server config file
                // Make sure the TransportSecurityProvider app is started/updated
                server.waitForStringInLogUsingMark("CWWKZ0001I.*TransportSecurityProvider | CWWKZ0003I.*TransportSecurityProvider", WAIT_TIME_OUT);

                // Current configuration has SSL feature, check if the ssl port is opened
                if (SERVER_CONFIG_WITH_SSL.contains(newServerConfigFile)) {
                    Log.info(AbstractJaxWsTransportSecurityTest.class, "updateServerConfigFile", "Wait for ssl port open.");
                    isFound = null != server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl");
                }
            } else if (!newServerConfigFile.equals(lastServerConfig)) {// The last server config file is not the same as the current one
                Log.info(AbstractJaxWsTransportSecurityTest.class, "updateServerConfigFile", "old: -" + lastServerConfig + "-, new: -" + newServerConfigFile + "-");
                if (checkAppUpdate) {
                    // Make sure the TransportSecurityProvider app is started/updated when changing config
                    server.waitForStringInLogUsingMark("CWWKZ0001I.*TransportSecurityProvider | CWWKZ0003I.*TransportSecurityProvider", WAIT_TIME_OUT);
                }

                // Current configuration has SSL feature, but last configuration has no SSL feature, should check if the ssl port is opened.
                if (SERVER_CONFIG_WITH_SSL.contains(newServerConfigFile) && SERVER_CONFIG_WITHOUT_SSL.contains(lastServerConfig)) {
                    Log.info(AbstractJaxWsTransportSecurityTest.class, "updateServerConfigFile", "Wait for ssl port open.");
                    isFound = null != server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl");
                }
            }

            if (!isFound) {
                if (warningWhenFail) {
                    Log.warning(AbstractJaxWsTransportSecurityTest.class, "The SSL port is not opened!");
                } else {
                    fail("The SSL port is not opened!");
                }
            }
        } finally {
            Log.info(AbstractJaxWsTransportSecurityTest.class, "updateServerConfigFile", "The last sever configuration file is: " + lastServerConfig);
            lastServerConfig = newServerConfigFile;
        }
    }

    /**
     * Update the file in the server root
     * 
     * @param destFilePath the relative path to server root, such as "dropins/test.war.xml"
     * @param originFilePath the relative path to publish/files/JaxWsTransportSecurityServer/
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
            LibertyFileManager.moveFileIntoLiberty(server.getMachine(), server.getServerRoot(), destFilePath, tmpAbsFile.getAbsolutePath());
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
     * @param expectedResp the expected response from connection
     * @param expectedServerInfos the expected server output in log, the info's order is very important
     * @throws ProtocolException
     * @throws MalformedURLException
     * @throws IOException
     */
    protected void runTest(RequestParams params, String expectedResp, List<String> expectedServerInfos) throws Exception, ProtocolException, MalformedURLException, IOException {
        List<String> expectedResps = new ArrayList<String>(1);
        expectedResps.add(expectedResp);
        runTest(params, expectedResps, expectedServerInfos);
    }

    /**
     * Run test
     * 
     * @param params the request parameters
     * @param expectedResponses the expected responses from connection
     * @param expectedServerInfos the expected server output in log, the info's order is very important
     * @throws ProtocolException
     * @throws MalformedURLException
     * @throws IOException
     */
    protected void runTest(RequestParams params, List<String> expectedResponses, List<String> expectedServerInfos) throws Exception, ProtocolException, MalformedURLException, IOException {
        server.setMarkToEndOfLog();

        StringBuilder urlBuilder = new StringBuilder("http://")
                        .append(server.getHostname())
                        .append(":")
                        .append(server.getHttpDefaultPort())
                        .append(SERVLET_PATH)
                        .append("?user=").append(params.userName)
                        .append("&serviceType=").append(params.serviceType)
                        .append("&schema=").append(params.schema)
                        .append("&port=").append(params.port)
                        .append("&path=").append(params.path)
                        .append("&testMethod=").append(testName.getMethodName());
        if (null != params.testMode) {
            urlBuilder.append("&testMode=").append(params.testMode.getVale());
        }

        if (null != expectedResponses) {
            assertTrue(printExpectedResponses(expectedResponses, false), checkExpectedResponses(urlBuilder.toString(), expectedResponses, false));
        }

        if (null != expectedServerInfos) {
            for (String info : expectedServerInfos) {
                assertNotNull("The expected output in server log is " + info, server.waitForStringInLogUsingMark(info));
            }
        }
    }

    private boolean checkExpectedResponses(String servletUrl, List<String> expectedResponses, boolean exact) throws IOException {
        Log.info(this.getClass(), testName.getMethodName(), "Calling Application with URL=" + servletUrl);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(servletUrl), HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
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
        Log.info(AbstractJaxWsTransportSecurityTest.class, "checkExpectedResponses", "responseContent = " + responseContent);
        if (exact) { //the response content must contain all the expect strings
            for (String expectStr : expectedResponses) {
                if (!responseContent.contains(expectStr)) {
                    return false;
                }
            }
            return true;
        } else { //the response content could contain one of the expect strings
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

        /**
         * @param userName
         * @param serviceType
         * @param schema
         * @param port
         * @param path
         */
        public RequestParams(String userName, String serviceType, String schema, int port, String path) {
            super();
            this.userName = userName;
            this.serviceType = serviceType;
            this.schema = schema;
            this.port = port;
            this.path = path;
        }

        /**
         * @param userName
         * @param testMode
         * @param serviceType
         * @param schema
         * @param port
         * @param path
         */
        public RequestParams(String userName, TestMode testMode, String serviceType, String schema, int port, String path) {
            super();
            this.userName = userName;
            this.testMode = testMode;
            this.serviceType = serviceType;
            this.schema = schema;
            this.port = port;
            this.path = path;
        }
    }

    protected enum TestMode {
        PORT("port"), DISPATCH("dispatch");

        private final String innerValue;

        private TestMode(String value) {
            this.innerValue = value;
        }

        public String getVale() {
            return this.innerValue;
        }
    }
}
