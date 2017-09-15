/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.mbeans.ServerXMLConfigurationMBean;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests that ServerXMLConfigurationMBean can be accessed through a proxy. Note that the
 * BVT tests that the configuration files can be downloaded through the FileTransferMBean
 * so not going to include a redundant test for that here.
 */
public class ServerXMLConfigurationMBeanTest {

    private static Class<?> logClass = ServerXMLConfigurationMBeanTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.schemaGen.mbean");

    private static String outputDir;
    private static MBeanServerConnection connection;
    private static JMXConnector jmxConnector;

    private static ObjectName serverXMLConfigurationMBeanObjName;

    @BeforeClass
    public static void setUp() throws Exception {
        final String methodName = "setUp";
        Log.entering(logClass, methodName);

        outputDir = server.getServerRoot();
        Log.info(logClass, methodName, "serverRoot=" + outputDir);

        Log.info(logClass, methodName, "Starting server=" + server.getServerName());
        server.startServer();

        Log.info(logClass, methodName, "Waiting for 'CWWKT0016I.*IBMJMXConnectorREST'");
        assertNotNull("'CWWKT0016I.*IBMJMXConnectorREST' was not received on server",
                      server.waitForStringInLog("CWWKT0016I.*IBMJMXConnectorREST"));

        Log.info(logClass, methodName, "Waiting for 'CWWKO0219I.*ssl'");
        assertNotNull("'CWWKO0219I.*ssl' was not recieved on server",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));

        Log.info(logClass, methodName, "Waiting for 'CWPKI0803A.*ssl'");
        assertNotNull("'CWPKI0803A.*ssl' was not generated on server",
                      server.waitForStringInLog("CWPKI0803A"));

        Log.info(logClass, methodName, "Waiting for 'CWWKS0008I: The security service is ready'");
        assertNotNull("'CWWKS0008I: The security service is ready' was not generated on server",
                      server.waitForStringInLog("CWWKS0008I"));

        Log.info(logClass, methodName, "Waiting for 'CWWKS4105I: LTPA configuration is ready'");
        assertNotNull("'CWWKS4105I: LTPA configuration is ready' was not generated on server",
                      server.waitForStringInLog("CWWKS4105I"));

        // Set up the trust store
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
        environment.put(JMXConnector.CREDENTIALS, new String[] { "theUser", "thePassword" });
        environment.put(ClientProvider.DISABLE_HOSTNAME_VERIFICATION, true);
        environment.put(ClientProvider.READ_TIMEOUT, 2 * 60 * 1000);
        JMXServiceURL url = new JMXServiceURL("REST", "localhost", getSSLPort(), "/IBMJMXConnectorREST");
        Log.info(logClass, methodName, "JMXServiceURL=" + url.toString());
        jmxConnector = JMXConnectorFactory.connect(url, environment);
        assertNotNull("JMXConnector should not be null", jmxConnector);
        connection = jmxConnector.getMBeanServerConnection();
        assertNotNull("MBeanServerConnection should not be null", connection);

        serverXMLConfigurationMBeanObjName = new ObjectName(ServerXMLConfigurationMBean.OBJECT_NAME);

        Log.exiting(logClass, "setUp");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        String methodName = "tearDown";
        Log.entering(logClass, methodName);

        if (server != null && server.isStarted()) {
            Log.finer(logClass, methodName, "Server is up, stopping it");
            jmxConnector.close();
            server.stopServer();
        }
        Log.exiting(logClass, methodName);
    }

    @Test
    public void testServerXMLConfigurationMBeanCallAPI() throws Exception {
        ServerXMLConfigurationMBean serverXMLConfigMBean = JMX.newMBeanProxy(connection, serverXMLConfigurationMBeanObjName, ServerXMLConfigurationMBean.class);
        assertNotNull("ServerXMLConfigurationMBean is unexpectedly null.", serverXMLConfigMBean);

        Collection<String> configFilePaths = serverXMLConfigMBean.fetchConfigurationFilePaths();
        assertNotNull("Configuration file path collection should not be null.", configFilePaths);

        // Check that the collection contains the expected file paths.
        assertEquals("Config file path collection size is not 3.", 3, configFilePaths.size());
        assertTrue("server.xml is missing from the collection.",
                   configFilePaths.contains("${server.config.dir}/server.xml"));
        assertTrue("fatTestPorts.xml is missing from the collection.",
                   configFilePaths.contains("${wlp.user.dir}/servers/fatTestPorts.xml"));
        assertTrue("fatTestCommon.xml is missing from the collection.",
                   configFilePaths.contains("${wlp.user.dir}/servers/fatTestCommon.xml"));
    }

    private static int getSSLPort() {
        return Integer.valueOf(System.getProperty("HTTP_default.secure", "8020"));
    }
}
