package com.ibm.ws.kernel.feature.fat;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test minifying a server that contains features with Icons. Ensure the icons are handled correctly.
 */
public class MinifyIconsTest {
    //    private static LibertyServer server = LibertyServerFactory.getLibertyServer();
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.icons_test");
    private static LibertyServer minifiedServer = null;
    private static MinifiedServerTestUtils minifyUtils = null;
    private static Map<String, IconFeature> iconFeaturesInstalled = new HashMap<String, IconFeature>();
    private static boolean supportedPlatform = true;

    /**
     * Set up the test. This method is called very early on and does almost all of the heavy lifting. That's
     * because minify will be very slow when we have a bunch of features. So instead of re-minifying each
     * test we do it once. But to make sure what we minify is good we need to do a few pre-flight checks.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setup() throws Exception {
        List<String> features = new ArrayList<String>();
        // Any new features in this test should be added to this list
        features.add("badPathTool-1.0");
        features.add("emptyIconHeader-1.0");
        features.add("goldenPathTool-1.0");
        features.add("iconDirectivesTool-1.0");
        features.add("missingIconsTool-1.0");
        features.add("noHeaderTool-1.0");

        // Create an IconFeature class for every string. Makes it easier to manage this way round
        for (String featureName : features) {
            iconFeaturesInstalled.put(featureName, new IconFeature(featureName));
        }

        // Set up expected files
        iconFeaturesInstalled.get("goldenPathTool-1.0").addExpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.get("iconDirectivesTool-1.0").addExpectedIcon("OSGI-INF/toolicon10.png");
        iconFeaturesInstalled.get("iconDirectivesTool-1.0").addExpectedIcon("OSGI-INF/toolicon64.png");
        iconFeaturesInstalled.get("missingIconsTool-1.0").addExpectedIcon("OSGI-INF/toolicon.png");

        // set up unexpectedFiles
        iconFeaturesInstalled.get("badPathTool-1.0").addUnexpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.get("emptyIconHeader-1.0").addUnexpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.get("iconDirectivesTool-1.0").addUnexpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.get("missingIconsTool-1.0").addUnexpectedIcon("OSGI-INF/missingtoolicon.png");
        iconFeaturesInstalled.get("noHeaderTool-1.0").addUnexpectedIcon("OSGI-INF/toolicon.png");

        // Deploy each of these features. We'll use the local bundles, rather than the featureManager, to
        // do this as we'll want to add files that shouldn't get picked up in the minify
        for (IconFeature feature : iconFeaturesInstalled.values()) {
            deployIconFeature(feature);
        }

        // Add wab-1.0 to the list of features because we need it to load the test URLs. Don't add it
        // because we don't want to create an IconFeature class for it
        features.add("wab-1.0");
        server.changeFeatures(features);

        server.startServer();
        // Check all the apps installed ok and work
        for (IconFeature feature : iconFeaturesInstalled.values()) {
            server.waitForStringInLog("CWWKT0016I.*" + feature.getShortName());
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + feature.getURL());
            HttpURLConnection con = getHttpConnection(url);
            BufferedReader br = getConnectionStream(con);
            String line = br.readLine();
            //If there is output then the Application automatically installed correctly
            assertTrue("The output for feature " + feature.getShortName() + "  should contain the feature shortname of " + feature.getShortName(),
                       line.contains(feature.getShortName()));
        }

        server.stopServer();

        // Minify the server. The minified version will be the one we'll want to run tests against
        minifyUtils = new MinifiedServerTestUtils();
        minifyUtils.setup(MinifyIconsTest.class.getName(),
                          "com.ibm.ws.kernel.icons_test",
                          server);
        RemoteFile miniServerPackage = minifyUtils.minifyServer();
        if (miniServerPackage != null) {
            minifiedServer = minifyUtils.useMinifiedServer(miniServerPackage);

            minifiedServer.changeFeatures(features);

            // Due to the dependency of the external test files, need to package and include them into the minfied archive
            RemoteFile fatTestCommon = server.getFileFromLibertyServerRoot("/../fatTestCommon.xml");
            RemoteFile fatTestPorts = server.getFileFromLibertyServerRoot("/../fatTestPorts.xml");
            RemoteFile testPortsProps = server.getFileFromLibertyServerRoot("/../testports.properties");
            RemoteFile serverEnv = server.getFileFromLibertyInstallRoot("/etc/server.env");

            // Put the required test files into the right place for new server..
            fatTestCommon.copyToDest(new RemoteFile(minifiedServer.getMachine(), minifiedServer.getServerRoot() + "/../fatTestCommon.xml"));
            fatTestPorts.copyToDest(new RemoteFile(minifiedServer.getMachine(), minifiedServer.getServerRoot() + "/../fatTestPorts.xml"));
            testPortsProps.copyToDest(new RemoteFile(minifiedServer.getMachine(), minifiedServer.getServerRoot() + "/../testports.properties"));
            if (serverEnv.exists())
                serverEnv.copyToDest(new RemoteFile(minifiedServer.getMachine(), minifiedServer.getInstallRoot() + "/etc/server.env"));

            // We should start the server again and make sure all our features are available
            minifiedServer.startServer();
            for (IconFeature feature : iconFeaturesInstalled.values()) {
                minifiedServer.waitForStringInLog("CWWKT0016I.*" + feature.getShortName());
                URL url = new URL("http://" + minifiedServer.getHostname() + ":" + minifiedServer.getHttpDefaultPort() + feature.getURL());
                HttpURLConnection con = getHttpConnection(url);
                BufferedReader br = getConnectionStream(con);
                String line = br.readLine();
                // If there is output then the Application automatically installed correctly
                assertTrue("The output did not contain the automatically installed message",
                           line.contains(feature.getShortName()));
            }

            // but we can stop the server as it shouldn't need to be running for the tests
            minifiedServer.stopServer();
        } else if (server.getMachine().getOperatingSystem().equals(OperatingSystem.ZOS)) {
            supportedPlatform = false;
        } else {
            Assert.fail("The minified server should not be null");
        }
    }

    /**
     * Deploy the feature file, the feature bundle, and both unexpected and expected icon files
     *
     * @param iconFeature the feature to deploy
     * @throws Exception
     */
    private static void deployIconFeature(IconFeature iconFeature) throws Exception {
        // Note that pathToAutoFVTTestFiles may change when we're on Moonstone
        String rootOfFiles = server.pathToAutoFVTTestFiles + "filesForPreMinify/";

        // Copy the manifest
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib/features",
                                               rootOfFiles + iconFeature.getFeatureManifest());

        // Copy the bundle
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/lib",
                                               rootOfFiles + iconFeature.getFeatureBundle());

        // Copy icons we expect to see in the minified image
        for (String expectedFile : iconFeature.getExpectedIcons()) {
            int delimiterPos = expectedFile.lastIndexOf("/");
            String expectedFolder = "/" + expectedFile;
            if (delimiterPos >= 0) {
                expectedFolder = "/" + expectedFile.substring(0, delimiterPos);
            }
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + expectedFolder,
                                                   rootOfFiles + "/" + expectedFile);
        }

        // Lay down any unexpected icons too, since they shouldn't make it into the minified image
        for (String unexpectedFile : iconFeature.getExpectedIcons()) {
            int delimiterPos = unexpectedFile.lastIndexOf("/");
            String unexpectedFolder = "/" + unexpectedFile;
            if (delimiterPos >= 0) {
                unexpectedFolder = "/" + unexpectedFile.substring(0, delimiterPos);
            }

            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + unexpectedFolder,
                                                   rootOfFiles + "/" + unexpectedFile);
        }

    }

    /**
     * We need to tidy up the minified server at the end of the test, and also ensure that no test files
     * were left behind
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (minifiedServer != null)
            minifiedServer.stopServer();
        // tear down minfiy
        if (minifyUtils != null)
            minifyUtils.tearDown();
        // Remove the files from the runtime
        for (IconFeature feature : iconFeaturesInstalled.values()) {
            removeIconFeature(feature);
        }

    }

    /**
     * Delete all files relating to the feature
     *
     * @param feature
     * @throws Exception
     */
    private static void removeIconFeature(IconFeature feature) throws Exception {
        server.deleteDirectoryFromLibertyInstallRoot(feature.getIconFolder());
        server.deleteFileFromLibertyInstallRoot(feature.getFeatureManifest());
        server.deleteFileFromLibertyInstallRoot(feature.getFeatureBundle());
    }

    /**
     * Verify that a feature with a badly formed Subsystem-Icon header will get its icons ignored
     *
     * @throws Exception
     */
    @Test
    public void minifyBadPath() throws Exception {
        validateFeature("badPathTool-1.0");

    }

    /**
     * Verify that a feature with a Subsystem-Icon header but no content will get no icons
     *
     * @throws Exception
     */
    @Test
    public void minifyEmptyHeader() throws Exception {
        validateFeature("emptyIconHeader-1.0");

    }

    /**
     * Verify that a valid header will get its icons picked up
     *
     * @throws Exception
     */
    @Test
    public void minifyGoldenPath() throws Exception {
        validateFeature("goldenPathTool-1.0");
    }

    /**
     * Verify that directives to not stop the icons being gathered
     *
     * @throws Exception
     */
    @Test
    public void minifyIconDirectives() throws Exception {
        validateFeature("iconDirectivesTool-1.0");
    }

    /**
     * Verify that we can ignore icons that are in the Subsystem-Icon header but not on disk
     *
     * @throws Exception
     */
    @Test
    public void minifyMissingIcons() throws Exception {
        validateFeature("missingIconsTool-1.0");
    }

    /**
     * Verify that if the header is totally missing no icons are gathered but the feature is correctly
     * minified
     *
     * @throws Exception
     */
    @Test
    public void minifyNoHeader() throws Exception {
        validateFeature("noHeaderTool-1.0");
    }

    /**
     * Validate that the passed in feature was installed correctly, its expected icons were laid down and nothing unexpected was laid down.
     *
     * @param featureName The feature to validate
     * @throws Exception
     */
    private void validateFeature(String featureName) throws Exception {
        // Only perform this work if the test setup didn't indicate we are on an unsupported platform
        if (supportedPlatform) {
            // Check that the bundle and the manifest made it in ok.
            // It should be tough for this to fail if the minified
            // server was checked in the BeforeClass
            IconFeature feature = iconFeaturesInstalled.get(featureName);
            Assert.assertNotNull("The icon feature " + featureName + " should be in the map of installed features", feature);
            assertTrue("The manifest file at " + feature.getFeatureManifest() + " should exist in the install root of the minified server",
                       minifiedServer.fileExistsInLibertyInstallRoot(feature.getFeatureManifest()));

            assertTrue("The bundle file " + feature.getFeatureBundle() + " should exist in the install root of the minified server",
                       minifiedServer.fileExistsInLibertyInstallRoot(feature.getFeatureBundle()));

            if (feature.expectedIconFiles.size() > 0) {
                assertTrue("The icons folder " + feature.getIconFolder() + " should exist in the install root of the minified server",
                           minifiedServer.fileExistsInLibertyInstallRoot(feature.getIconFolder()));
            } else {
                Assert.assertFalse("The icons folder " + feature.getIconFolder() + " should not exist in the install root of the minified server",
                                   minifiedServer.fileExistsInLibertyInstallRoot(feature.getIconFolder()));
            }

            for (String expectedIcon : feature.getExpectedIcons()) {
                assertTrue("The icon " + expectedIcon + " should exist in the install root of the minified server",
                           minifiedServer.fileExistsInLibertyInstallRoot(expectedIcon));
            }

            for (String unexpectedIcon : feature.getUnexpectedIcons()) {
                Assert.assertFalse("The icon " + unexpectedIcon + " should not exist in the install root of the minified server",
                                   minifiedServer.fileExistsInLibertyInstallRoot(unexpectedIcon));
            }
        }
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private static BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private static HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

}