/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.embedded;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
public class EmbeddedJCATest {

    private static final String fvtapp = "fvtapp";
    private static final String standaloneapp = "standaloneapp";
    private static final String fvtweb = "fvtweb";
    private static final String fvtweb1 = "fvtweb1";
    private static final String standardWAB = "standardWAB";

    private static LibertyServer server;
    private static String activationError;

    /**
     * Utility method to run a test on JCAFVTServlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
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
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.embeddedrar");
        server.addInstalledAppForValidation(fvtapp);
        server.addInstalledAppForValidation(standaloneapp);
        server.startServer();
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));

        activationError = server.waitForStringInLog("J2CA8810E");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CNTR401(5|6)W: .* (queue1|FVTMessageDrivenBeanOverride)", // EXPECTED: MDB is not in the server.xml for all beans on application
                          "J2CA8811E: .*(topic1|queue1).*fvtapp.tra1", // EXPECTED
                          "J2CA8811E: Resource ims/cf1 from embedded resource adapter fvtapp.ims is available only to application fvtapp", // EXPECTED
                          "J2CA8810E: The endpoint FVTMessageDrivenBeanOverride from embedded resource adapter fvtapp.tra1 can be activated only from application fvtapp.", // EXPECTED
                          "J2CA8809E: .*(topic1|queue1).*fvtapp.tra1", // EXPECTED
                          "J2CA8809E: Resource ims/cf1 from embedded resource adapter fvtapp.ims is available only to application fvtapp.", // EXPECTED
                          "CWWKN0008E: An object could not be obtained for name (ims/cf1|(jms/(queue1|topic1)))", // EXPECTED
                          "CWWKE0701E" // EXPECTED due to access restrictions of embedded resources
        );
    }

    @Test
    public void testNativeLibs() throws Exception {
        final String nativeLib = "Test";

        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        Log.info(getClass(), "testNativeLibs", "os.name: " + os);
        Log.info(getClass(), "testNativeLibs", "os.arch: " + arch);

        if (os.contains("win") || os.contains("linux")) {
            String libName;
            if (arch.contains("x86") || arch.contains("amd")) {
                libName = arch.contains("64") ? nativeLib : nativeLib + "32";
            } else {
                libName = null;
            }

            if (libName != null) {
                // - tra1 (standalone + fvtapp) include native libraries in .rar
                // - tra2 (standalone) uses privateLibraryRef
                int num = server.waitForMultipleStringsInLog(2, "Loaded Native Library " + libName);
                Assert.assertEquals("Did not find expected number of \"Loaded Native Library " + libName + "\"", 2, num);
            }
        }
    }

    @Test
    public void testConfigurationOfEmbeddedResourceAdapter() throws Exception {
        runInServlet("testConfigurationOfEmbeddedResourceAdapter", fvtweb);
    }

    @Test
    public void testEmbeddedConnectionFactory() throws Exception {
        runInServlet("testEmbeddedConnectionFactory", fvtweb);
    }

    @Test
    public void testEmbeddedConnectionFactory10() throws Exception {
        runInServlet("testEmbeddedConnectionFactory10", fvtweb);
    }

    @Test
    public void testEmbeddedActivationSpec() throws Exception {
        runInServlet("testEmbeddedActivationSpec", fvtweb);
        assertNotNull("the MDB should have recieved this message and sent it out to the logs",
                      server.waitForStringInLog("FVTMessageDrivenBean:message:testActivationSpec"));
    }

    @Test
    public void testEmbeddedDestinations() throws Exception {
        runInServlet("testEmbeddedDestinations", fvtweb);
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedConnectionFactoryFromDifferentEAR() throws Exception {
        runInServlet("testEmbeddedConnectionFactoryFromDifferentEAR", fvtweb1);
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedAOFromDifferentEAR() throws Exception {
        runInServlet("testEmbeddedAOFromDifferentEAR", fvtweb1);
    }

    @Test
    public void testEmbeddedConnectionFactoryFromDifferentEARIndirectLookup() throws Exception {
        runInServlet("testEmbeddedConnectionFactoryFromDifferentEARIndirectLookup", fvtweb1);
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedAOFromDifferentEARIndirectLookup() throws Exception {
        runInServlet("testEmbeddedAOFromDifferentEARIndirectLookup", fvtweb1);
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedAOFromDifferentWAB() throws Exception {
        runInServlet("testEmbeddedAOFromDifferentWAB", standardWAB);
    }

    @Test
    public void testEmbeddedEndpointActivationFromDifferentEAR() throws Exception {
        assertNotNull("Endpoint Activation should fail with J2CA8810E",
                      activationError);
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" })
    @Test
    public void testEmbeddedObjectFromDifferentEBA() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("dropins", "jcaOSGi.eba");
        assertNotNull("the jcaOSGi.eba application should have started successfully",
                      server.waitForStringInLogUsingMark("CWWKZ000[13]I"));

        assertNotNull("test failed for ims/cf1", server.waitForStringInLogUsingMark("Lookup failed for ims/cf1"));
        assertNotNull("test failed for jms/queue1", server.waitForStringInLogUsingMark("Lookup failed for jms/queue1"));
        assertNotNull("test failed for jms/topic1", server.waitForStringInLogUsingMark("Lookup failed for jms/topic1"));
        assertNotNull("test failed for javax.resource.cci.ConnectionFactory", server.waitForStringInLogUsingMark("Lookup failed for javax.resource.cci.ConnectionFactory"));

        server.setMarkToEndOfLog();
        server.deleteFileFromLibertyServerRoot("dropins/jcaOSGi.eba");
        assertNotNull("the jcaOSGi.eba application should have stopped successfully",
                      server.waitForStringInLogUsingMark("CWWKZ0009I"));
    }

    @Test
    public void testReauthentication() throws Exception {
        runInServlet("testReauthentication", fvtweb);
    }

    @Test
    public void testConnectionPoolStats() throws Exception {
        runInServlet("testConnectionPoolStats", fvtweb);
    }
}
