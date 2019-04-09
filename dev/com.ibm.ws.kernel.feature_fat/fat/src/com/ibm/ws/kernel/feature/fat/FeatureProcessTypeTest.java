/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class FeatureProcessTypeTest {
    private static final Class<?> c = FeatureProcessTypeTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.process.type");

    private static final String SERVER_A_1_0 = "serverA-1.0";

    private static final String CLIENT_B_1_0 = "clientB-1.0";

    private static final String FEATURE_CACHE = "workarea/platform/feature.cache";

    @BeforeClass
    public static void beforeClass() throws Exception {
        Log.info(c, "beforeClass", "Installing features");
        server.installSystemFeature(SERVER_A_1_0);
        server.installSystemFeature(CLIENT_B_1_0);

        for (int i = 1; i < 8; i++) {
            server.copyFileToLibertyInstallRoot("lib", "bundle" + i + "_1.0.0.jar");
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Log.info(c, "afterClass", "Uninstalling features");
        server.uninstallSystemFeature(SERVER_A_1_0);
        server.uninstallSystemFeature(CLIENT_B_1_0);

        for (int i = 1; i < 8; i++) {
            server.deleteFileFromLibertyInstallRoot("lib/bundle" + i + "_1.0.0.jar");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (server.isStarted()) {
            ProgramOutput po = server.stopServer();
            Log.info(c, "tearDown", "Stop server " + (po.getReturnCode() == 0 ? "succeeded" : "failed"));
        }
        try {
            RemoteFile tmpBootstrapProps = server.getFileFromLibertyServerRoot("tmp.bootstrap.properties");
            if (tmpBootstrapProps.exists()) {
                server.renameLibertyServerRootFile("bootstrap.properties", "override_tolerates_bootstrap.properties");
                server.renameLibertyServerRootFile("tmp.bootstrap.properties", "bootstrap.properties");
            }
        } catch (FileNotFoundException e) {
            // nothing
        }

    }

    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testClientFeatureConfigured() throws Exception {
        final String m = "testClientFeatureConfigured";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_client.xml");
        server.startServer(m + ".log");

        assertNotNull("No message indicating a conflict", server.waitForStringInLog("CWWKF0034E.*" + CLIENT_B_1_0));

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertFalse("Expected clientB-1.0 feature to not be installed, but it was: " + installedFeatures, installedFeatures.contains(CLIENT_B_1_0));

        Log.info(c, m, "successful exit");
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testServerIncludeClientFeatureConfigured() throws Exception {
        final String m = "testServerIncludeClientFeatureConfigured";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_serverclient.xml");
        server.startServer(m + ".log");

        assertNotNull("No message indicating a conflict", server.waitForStringInLog("CWWKF0035E.*" + CLIENT_B_1_0));

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertTrue("Expected serverA-1.0 feature to be installed, but it was not: " + installedFeatures, installedFeatures.contains(SERVER_A_1_0));

        Log.info(c, m, "successful exit");
    }
}
