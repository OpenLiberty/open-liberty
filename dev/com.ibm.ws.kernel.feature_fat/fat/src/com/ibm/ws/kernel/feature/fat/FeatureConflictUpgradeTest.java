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
public class FeatureConflictUpgradeTest {
    private static final Class<?> c = FeatureConflictUpgradeTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.featureConflict");

    private static final String FEATURE_CAPABILITYA_1_0 = "capabilityA-1.0";
    private static final String FEATURE_CAPABILITYA_1_1 = "capabilityA-1.1";
    private static final String FEATURE_CAPABILITYA_2_0 = "capabilityA-2.0";

    private static final String FEATURE_CAPABILITYB_1_0 = "capabilityB-1.0";
    private static final String FEATURE_CAPABILITYB_1_1 = "capabilityB-1.1";
    private static final String FEATURE_CAPABILITYB_2_0 = "capabilityB-2.0";

    private static final String FEATURE_CAPABILITYC_1_0 = "capabilityC-1.0";
    private static final String FEATURE_CAPABILITYD_1_0 = "capabilityD-1.0";

    private static final String FEATURE_COMBO_A10_B10_1_0 = "comboA10B10-1.0";

    private static final String FEATURE_AUTO_A10_B10_1_0 = "auto-A10B10-1.0";
    private static final String FEATURE_AUTO_A11_B11_1_0 = "auto-A11B11-1.0";

    private static final String FEATURE_CACHE = "workarea/platform/feature.cache";

    @BeforeClass
    public static void beforeClass() throws Exception {
        Log.info(c, "beforeClass", "Installing features");
        server.installSystemFeature(FEATURE_CAPABILITYA_1_0);
        server.installSystemFeature(FEATURE_CAPABILITYA_1_1);
        server.installSystemFeature(FEATURE_CAPABILITYA_2_0);
        server.installSystemFeature(FEATURE_CAPABILITYB_1_0);
        server.installSystemFeature(FEATURE_CAPABILITYB_1_1);
        server.installSystemFeature(FEATURE_CAPABILITYB_2_0);
        server.installSystemFeature(FEATURE_CAPABILITYC_1_0);
        server.installSystemFeature(FEATURE_CAPABILITYD_1_0);
        server.installSystemFeature(FEATURE_COMBO_A10_B10_1_0);
        server.installSystemFeature(FEATURE_AUTO_A10_B10_1_0);
        server.installSystemFeature(FEATURE_AUTO_A11_B11_1_0);

        for (int i = 1; i < 8; i++) {
            server.copyFileToLibertyInstallRoot("lib", "bundle" + i + "_1.0.0.jar");
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Log.info(c, "afterClass", "Uninstalling features");
        server.uninstallSystemFeature(FEATURE_CAPABILITYA_1_0);
        server.uninstallSystemFeature(FEATURE_CAPABILITYA_1_1);
        server.uninstallSystemFeature(FEATURE_CAPABILITYA_2_0);
        server.uninstallSystemFeature(FEATURE_CAPABILITYB_1_0);
        server.uninstallSystemFeature(FEATURE_CAPABILITYB_1_1);
        server.uninstallSystemFeature(FEATURE_CAPABILITYB_2_0);
        server.uninstallSystemFeature(FEATURE_CAPABILITYC_1_0);
        server.uninstallSystemFeature(FEATURE_CAPABILITYD_1_0);
        server.uninstallSystemFeature(FEATURE_COMBO_A10_B10_1_0);
        server.uninstallSystemFeature(FEATURE_AUTO_A10_B10_1_0);
        server.uninstallSystemFeature(FEATURE_AUTO_A11_B11_1_0);

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

    /**
     * Tests that two versions of a singleton feature cause a failure if configured directly in server.xml
     * 
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testTwoVersionsSameFeatureConfigured() throws Exception {
        final String m = "testTwoVersionsSameFeature";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_a10a11.xml");
        server.startServer(m + ".log");

        assertNotNull("No message indicating a conflict", server.waitForStringInLog("CWWKF0033E.*" + FEATURE_CAPABILITYA_1_0));

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertFalse("Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Expected capabilityA-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));

        Log.info(c, m, "successful exit");
    }

    /**
     * Tests that the latest version of a feature is loaded even when another feature depends on on older
     * version of that feature.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureOverridesComboFeature() throws Exception {
        final String m = "testFeatureOverridesComboFeature";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_a10b11combo10.xml");
        server.startServer(m + ".log");

        //assertNotNull("No message indicating that the lower version feature was not used", server.waitForStringInLog("CWWKF0031I.*" + FEATURE_CAPABILITYB_1_0));

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertTrue("Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Expected capabilityB-1.1 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        assertFalse("Expected capabilityB-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));

        Log.info(c, m, "successful exit");
    }

    /**
     * Tests that a feature update that adds a newer version of a feature (while a combo feature depends on a
     * lower version of the same feature) results in the lower version of the feature getting removed, and the
     * newer feature installed.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureUpdateUpgradesExistingFeature() throws Exception {
        final String m = "testFeatureUpdateUpgradesExistingFeature";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_a10combo10.xml");
        server.startServer(m + ".log");

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Pre-condition: Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Pre-condition: Expected capabilityB-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertFalse("Pre-condition: Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Pre-condition: Expected capabilityB-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));

        Log.info(c, m, "Feature update - adding:" + FEATURE_CAPABILITYB_1_1);
        server.setServerConfigurationFile("server_a10b11combo10.xml");
        // assertNotNull("No message indicating that the lower version feature was not used", server.waitForStringInLog("CWWKF0031I.*" + FEATURE_CAPABILITYB_1_0));
        assertNotNull("Expected message indicating that feature update completed did not occur", server.waitForStringInLog("CWWKG0017I")); // feature update completed
        assertNotNull("Expected message indicating that capabilityB-1.1 feature was installed did not occur", server.waitForStringInLog("CWWKF0012I.*capabilityB-1.1"));
        assertNotNull("Expected message indicating that capabilityB-1.0 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityB-1.0"));

        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Expected capabilityB-1.1 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        assertFalse("Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Expected capabilityB-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));

        Log.info(c, m, "successful exit");
    }

    /**
     * Tests that a feature update that adds a newer version of a feature (while a combo feature depends on a
     * lower version of the same feature) results in the lower version of the feature getting removed, and the
     * newer feature installed.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureUpdateDowngradesExistingFeature() throws Exception {
        final String m = "testFeatureUpdateUpgradesExistingFeature";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_a10b11combo10.xml");
        server.startServer(m + ".log");

        // assertNotNull("No message indicating that the lower version feature was not used", server.waitForStringInLog("CWWKF0031I.*" + FEATURE_CAPABILITYB_1_0));

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Pre-condition: Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Pre-condition: Expected capabilityB-1.1 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        assertFalse("Pre-condition: Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Pre-condition: Expected capabilityB-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));

        Log.info(c, m, "Feature update - adding:" + FEATURE_CAPABILITYB_1_1);
        server.setServerConfigurationFile("server_a10combo10.xml");
        assertNotNull("Expected message indicating that feature update completed did not occur", server.waitForStringInLog("CWWKG0017I")); // feature update completed
        assertNotNull("Expected message indicating that capabilityB-1.0 feature was installed did not occur", server.waitForStringInLog("CWWKF0012I.*capabilityB-1.0"));
        assertNotNull("Expected message indicating that capabilityB-1.1 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityB-1.1"));

        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Expected capabilityB-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertFalse("Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Expected capabilityB-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));

        Log.info(c, m, "successful exit");
    }

    /**
     * Tests that auto features (features that are loaded automatically when other specified features
     * are explicitly loaded) load correctly. Specifically, this test verifies that an auto feature
     * that depends on capabilityA-1.0 and capabilityB-1.0 is loaded when those features are loaded.
     * Then, when capabilityB is upgraded to 1.1, the auto feature should be unloaded. Then when
     * capabilityA is also upgraded to 1.1, a new auto feature that depends on capabilityA-1.1 and
     * capabilityB-1.1 should be loaded.
     */
    @Test
    public void testAutoFeaturesUpgradeAndDowngradeAppropriately() throws Exception {
        final String m = "testAutoFeaturesUpgradeAndDowngradeAppropriately";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_a10combo10.xml");
        server.startServer(m + ".log");

        // server.xml specifies capabilityA-1.0 and capabilityB-1.0, so
        // we expect auto feature auto-A10B10 to be installed
        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Pre-condition: Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Pre-condition: Expected capabilityB-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertTrue("Pre-condition: Expected auto-A10B10-1.0 auto feature to be installed but was not", installedFeatures.contains(FEATURE_AUTO_A10_B10_1_0));
        assertFalse("Pre-condition: Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Pre-condition: Expected capabilityB-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        assertFalse("Pre-condition: Expected auto-A11B11-1.0 auto feature not to be installed, but it was", installedFeatures.contains(FEATURE_AUTO_A11_B11_1_0));

        Log.info(c, m, "Feature update - adding:" + FEATURE_CAPABILITYB_1_1);
        server.setServerConfigurationFile("server_a10b11combo10.xml");
        //assertNotNull("No message indicating that the lower version feature was not used", server.waitForStringInLog("CWWKF0031I.*" + FEATURE_CAPABILITYB_1_0));
        assertNotNull("Expected message indicating that feature update completed did not occur", server.waitForStringInLog("CWWKG0017I")); // feature update completed
        assertNotNull("Expected message indicating that capabilityB-1.1 feature was installed did not occur", server.waitForStringInLog("CWWKF0012I.*capabilityB-1.1"));
        assertNotNull("Expected message indicating that capabilityB-1.0 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityB-1.0"));

        // Now server.xml specifies a variety of options that should include capabiltyA-1.0,
        // capabilityB-1.0, and capabilityB-1.1 -- the server should not install capabilityB-1.0
        // since capabilityB-1.1 is the upgrade -- that means that auto feature auto-A10B10 should
        // be uninstalled.
        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Expected capabilityB-1.1 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        assertFalse("Expected auto-A10B10-1.0 auto feature not to be installed but it was", installedFeatures.contains(FEATURE_AUTO_A10_B10_1_0));
        assertFalse("Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Expected capabilityB-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertFalse("Expected auto-A11B11-1.0 auto feature not to be installed, but it was", installedFeatures.contains(FEATURE_AUTO_A11_B11_1_0));

        // Test was failing on Sun so add a sleep because
        // some filesystems need about a second for filesystem changes to be detected.
        //Pause for 2 seconds
        Thread.sleep(2000);

        Log.info(c, m, "Feature update - adding:" + FEATURE_CAPABILITYA_1_1);
        server.setServerConfigurationFile("server_a11b11combo10.xml");
        //assertNotNull("No message indicating that the lower version feature was not used", server.waitForStringInLog("CWWKF0031I.*" + FEATURE_CAPABILITYA_1_0));
        assertNotNull("Expected message indicating that feature update completed did not occur", server.waitForStringInLog("CWWKG0017I")); // feature update completed
        assertNotNull("Expected message indicating that capabilityA-1.1 feature was installed did not occur", server.waitForStringInLog("CWWKF0012I.*capabilityA-1.1"));
        assertNotNull("Expected message indicating that capabilityA-1.0 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityA-1.0"));

        // No we've upgraded capabilityA-1.0 to 1.1- so we have A1.0, A1.1, B1.0, and B1.1 specified, and
        // the server should remove A1.0 and B1.0 since the are upgraded by A1.1 and B1.1 respectively.
        // This means that we should see auto feature, auto-A11B11 installed.
        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Expected capabilityA-1.1 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertTrue("Expected capabilityB-1.1 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        assertTrue("Expected auto-A11B11-1.0 auto feature to be installed but was not", installedFeatures.contains(FEATURE_AUTO_A11_B11_1_0));
        assertFalse("Expected capabilityA-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertFalse("Expected capabilityB-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertFalse("Expected auto-A10B10-1.0 auto feature not to be installed, but it was", installedFeatures.contains(FEATURE_AUTO_A10_B10_1_0));

        Log.info(c, m, "successful exit");
    }

    /**
     * Tests that a conflict occurs if two different versions of the same feature are required but there is no toleration
     * that allows for a common version to be selected. The removes the problem feature to ensure a solution can be found
     * 
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testFeatureConflict() throws Exception {
        final String m = "testFeatureConflict";
        Log.info(c, m, "starting test");

        server.setServerConfigurationFile("server_d10a10b10combo10.xml");
        server.startServer(m + ".log");

        assertNotNull("No message indicating a conflict", server.waitForStringInLog("CWWKF0033E.*" + FEATURE_CAPABILITYA_1_0));

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertTrue("Expected comboA10B10-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_COMBO_A10_B10_1_0));
        assertTrue("Expected capabilityB-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertTrue("Expected capabilityC-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYC_1_0));
        assertTrue("Expected capabilityD-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYD_1_0));
        assertFalse("Expected capabilityA-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertFalse("Expected capabilityA-2.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_2_0));

        Log.info(c, m, "Feature update - using only: " + FEATURE_COMBO_A10_B10_1_0);
        server.setServerConfigurationFile("server_a10combo10.xml");
        assertNotNull("Expected message indicating that feature update completed did not occur", server.waitForStringInLog("CWWKG0017I")); // feature update completed
        assertNotNull("Expected message indicating that capabilityA-1.0 feature was installed did not occur", server.waitForStringInLog("CWWKF0012I.*capabilityA-1.0"));
        assertNotNull("Expected message indicating that capabilityC-1.0 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityC-1.0"));
        assertNotNull("Expected message indicating that capabilityD-1.0 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityD-1.0"));

        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Expected capabilityB-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertFalse("Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Expected capabilityB-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        Log.info(c, m, "successful exit");
    }

    /**
     * Tests that a conflict does not occur if two different versions of the same feature are required but tolerates is overridden by bootstrap.properties
     * that allows for a common version to be selected. Then removes the problem feature to ensure a solution can be found again.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureConflictOverrideTolerate() throws Exception {
        final String m = "testFeatureConflicOverrideTolerate";
        Log.info(c, m, "starting test");

        server.renameLibertyServerRootFile("bootstrap.properties", "tmp.bootstrap.properties");
        server.renameLibertyServerRootFile("override_tolerates_bootstrap.properties", "bootstrap.properties");
        server.setServerConfigurationFile("server_d10a10b10combo10.xml");
        server.startServer(m + ".log");

        String installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        assertTrue("Expected comboA10B10-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_COMBO_A10_B10_1_0));
        assertTrue("Expected capabilityA-2.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_2_0));
        assertTrue("Expected capabilityB-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertTrue("Expected capabilityC-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYC_1_0));
        assertTrue("Expected capabilityD-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYD_1_0));
        assertFalse("Expected capabilityA-1.0 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));

        Log.info(c, m, "Feature update - using only: " + FEATURE_COMBO_A10_B10_1_0);
        server.setServerConfigurationFile("server_a10combo10.xml");
        assertNotNull("Expected message indicating that feature update completed did not occur", server.waitForStringInLog("CWWKG0017I")); // feature update completed
        assertNotNull("Expected message indicating that capabilityA-1.0 feature was installed did not occur", server.waitForStringInLog("CWWKF0012I.*capabilityA-1.0"));
        assertNotNull("Expected message indicating that capabilityC-1.0 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityC-1.0"));
        assertNotNull("Expected message indicating that capabilityD-1.0 feature was uninstalled did not occur", server.waitForStringInLog("CWWKF0013I.*capabilityD-1.0"));

        installedFeatures = TestUtils.getInstalledFeatures(server, FEATURE_CACHE);
        Log.info(c, m, "installedFeatures: " + installedFeatures);
        assertTrue("Expected capabilityA-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYA_1_0));
        assertTrue("Expected capabilityB-1.0 feature to be installed but was not", installedFeatures.contains(FEATURE_CAPABILITYB_1_0));
        assertFalse("Expected capabilityA-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYA_1_1));
        assertFalse("Expected capabilityB-1.1 feature not to be installed, but it was", installedFeatures.contains(FEATURE_CAPABILITYB_1_1));
        Log.info(c, m, "successful exit");
    }
}
