/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class ChildAliasTest {

    /**
     * Utility to set the method name as a String before the test
     */
    @Rule
    public TestName name = new TestName();

    public String testName = "";

    @Before
    public void setTestName() {
        // set the current test name
        testName = name.getMethodName();
    }

    private static final String CONTEXT_ROOT = "/childalias";

    private static LibertyServer testServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.childalias");

    @BeforeClass
    public static void setUpForConfigExtensionsTests() throws Exception {
        //copy the extensions tests features into the server features location
        testServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/childAliasTest-1.0.mf");
        testServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/childAliasTestB-1.0.mf");
        testServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/childAliasTestC-1.0.mf");

        // Copy the config fat internal feature
        testServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");

        //copy the extensions tests bundles into the server lib location
        testServer.copyFileToLibertyInstallRoot("lib", "bundles/test.config.childalias.jar");
        testServer.copyFileToLibertyInstallRoot("lib", "bundles/test.config.childalias.b.jar");
        testServer.copyFileToLibertyInstallRoot("lib", "bundles/test.config.childalias.c.jar");

        WebArchive childAliasApp = ShrinkHelper.buildDefaultApp("childalias", "test.server.config.childalias");
        ShrinkHelper.exportAppToServer(testServer, childAliasApp);

        testServer.startServer();
        //make sure the URL is available
        assertNotNull(testServer.waitForStringInLog("CWWKT0016I.*" + CONTEXT_ROOT));
        assertNotNull(testServer.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void shutdown() throws Exception {
        testServer.stopServer("CWWKG0101W");
        testServer.deleteFileFromLibertyInstallRoot("lib/features/childAliasTest-1.0.mf");
        testServer.deleteFileFromLibertyInstallRoot("lib/features/childAliasTestB-1.0.mf");
        testServer.deleteFileFromLibertyInstallRoot("lib/features/childAliasTestC-1.0.mf");
        testServer.deleteFileFromLibertyInstallRoot("lib/test.config.childalias.jar");
        testServer.deleteFileFromLibertyInstallRoot("lib/test.config.childalias.b.jar");
        testServer.deleteFileFromLibertyInstallRoot("lib/test.config.childalias.c.jar");

        testServer.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");
    }

    @Test
    public void testChildAlias1() throws Exception {
        // precondition: regular server.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/server.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);

        test(testServer);
    }

    @Test
    public void testChildAlias2() throws Exception {
        // precondition: regular server.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/server.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testChildAliasSingleton1() throws Exception {
        // precondition: regular server.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/server.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testChildAliasSingleton2() throws Exception {
        // precondition: regular server.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/server.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testBundleOrdering1() throws Exception {
        // Because this test ensures bundle ordering with config elements defined
        // in different bundles, a fresh start of the server with the config is needed
        // so that this test can run in any order while simulating bundle start ordering.
        testServer.stopServer();
        testServer.setServerConfigurationFile("childalias/server.xml");
        testServer.startServer();

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverB.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);

    }

    @Test
    public void testBundleOrdering2() throws Exception {
        // Because this test ensures bundle ordering with config elements defined
        // in different bundles, a fresh start of the server with the config is needed
        // so that this test can run in any order while simulating bundle start ordering.
        testServer.stopServer("CWWKG0101W");
        testServer.setServerConfigurationFile("childalias/serverB.xml");
        testServer.startServer();

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testBundleOrderingAliasConflict() throws Exception {
        // Because this test ensures bundle ordering with config elements defined
        // in different bundles, a fresh start of the server with the config is needed
        // so that this test can run in any order while simulating bundle start ordering.
        testServer.stopServer("CWWKG0101W");
        testServer.setServerConfigurationFile("childalias/serverB.xml");
        testServer.startServer();

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testRemoveChild() throws Exception {
        // precondition: serverC.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC2.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testAddNewChild() throws Exception {
        // precondition: serverC2.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC2.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC3.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testUpdateChild() throws Exception {
        // precondition: serverC3
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC3.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC4.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testRemoveSingletonChild() throws Exception {
        // precondition: serverC4.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC4.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC5.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testAddNewSingletonChild() throws Exception {
        // precondition: serverC5.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC5.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC6.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    @Test
    public void testUpdateSingletonChild() throws Exception {
        // precondition: serverC6.xml
        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC6.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);

        testServer.setMarkToEndOfLog();
        testServer.setServerConfigurationFile("childalias/serverC7.xml");
        testServer.waitForConfigUpdateInLogUsingMark(null);
        test(testServer);
    }

    private void test(LibertyServer server) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                              CONTEXT_ROOT + "/child-alias-test?" + "testName=" + testName);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            System.out.println(output);
            assertTrue(output, output.trim().startsWith("OK"));
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static String read(InputStream in) throws IOException {
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

}
