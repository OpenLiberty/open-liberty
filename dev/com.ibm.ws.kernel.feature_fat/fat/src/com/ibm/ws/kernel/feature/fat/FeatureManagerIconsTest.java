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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests that installing and uninstalling of features using the featureManager will honour the
 * icons.
 * 
 * This test uses the IconFeature class to represent the feature being tested. If any new features are
 * needed for testing, please consult that class's Javadoc as setting up a new feature in the same way
 * may make testing common items much easier.
 */
@Mode(TestMode.FULL)
public class FeatureManagerIconsTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.icons_test");
    private final Class<?> c = FeatureManagerIconsTest.class;

    List<IconFeature> iconFeaturesInstalled = new ArrayList<IconFeature>();

    /**
     * Test that adding a feature where the Icon header is wrong is effectively ignored
     * 
     * @throws Exception
     */
    @Test
    public void installEsaBadPath() throws Exception {
        IconFeature badPath = new IconFeature("badPathTool-1.0");
        badPath.addUnexpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.add(badPath);
        runInstaller(badPath);
    }

    /**
     * Test that a feature with the Subsystem-Icon header specified without any files works
     * 
     * @throws Exception
     */
    @Test
    public void installEsaWithEmptyHeader() throws Exception {
        IconFeature emptyIconHeaderFeature = new IconFeature("emptyIconHeader-1.0");
        emptyIconHeaderFeature.addUnexpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.add(emptyIconHeaderFeature);
        runInstaller(emptyIconHeaderFeature);
    }

    /**
     * Test golden path, a single file referenced in the Subsystem-Icon header
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void installEsaGoldenPath() throws Exception {
        IconFeature goldenPath = new IconFeature("goldenPathTool-1.0");
        goldenPath.addExpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.add(goldenPath);
        runInstaller(goldenPath);
    }

    /**
     * Test adding a directive onto the file path and ensuring the files are still detected correctly
     * 
     * @throws Exception
     */
    @Test
    public void installEsaWithIconDirectives() throws Exception {
        IconFeature iconDirectives = new IconFeature("iconDirectivesTool-1.0");
        iconDirectives.addExpectedIcon("OSGI-INF/toolicon64.png");
        iconDirectives.addExpectedIcon("OSGI-INF/toolicon10.png");
        iconFeaturesInstalled.add(iconDirectives);
        runInstaller(iconDirectives);
    }

    /**
     * Test an ESA with multiple files specified in the header, but where not all the files exist in the ESA.
     * 
     * @throws Exception
     */
    @Test
    public void installEsaWithSomeMissingIcons() throws Exception {
        IconFeature someIconsMissing = new IconFeature("missingIconsTool-1.0");
        someIconsMissing.addUnexpectedIcon("OSGI-INF/missingtoolicon.png");
        someIconsMissing.addExpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.add(someIconsMissing);
        runInstaller(someIconsMissing);
    }

    /**
     * Test an ESA where the Subsystem-Icon header is not provided. Nothing should be installed.
     * 
     * @throws Exception
     */
    @Test
    public void installEsaWithNoHeader() throws Exception {
        IconFeature noHeaderFeature = new IconFeature("noHeaderTool-1.0");
        noHeaderFeature.addUnexpectedIcon("OSGI-INF/toolicon.png");
        iconFeaturesInstalled.add(noHeaderFeature);
        runInstaller(noHeaderFeature);
    }

    /**
     * Test installing a user feature. This should work exactly the same as a product feature, although
     * files should go under usr/extension
     * 
     * @throws Exception
     */
    @Test
    public void installUserFeature() throws Exception {
        IconFeature userFeature = new IconFeature("userIconFeature-1.0");
        userFeature.markAsUserFeature();
        userFeature.addExpectedIcon("OSGI-INF/usertool.png");
        userFeature.addUnexpectedIcon("OSGI-INF/unexpectedIcon.png");
        iconFeaturesInstalled.add(userFeature);
        runInstaller(userFeature);
    }

    /**
     * Run the featureManager install command and verify that, as well as installing the feature correctly,
     * the feature's wab is accessible and only the expected icons are laid down
     * 
     * @param string
     * @throws Exception
     */
    private void runInstaller(IconFeature feature) throws Exception {
        String method = "runInstaller";
        server.stopServer();
        String pathToEsa = feature.getAutoFVTLocation();
        server.copyFileToLibertyInstallRoot(pathToEsa);
        String cmd = server.getInstallRoot() + "/bin/featureManager install " + feature.getEsaFile() + " --acceptLicense";
        ProgramOutput result = server.getMachine().execute(cmd);
        Log.info(c, method, "Got response " + result.getReturnCode());
        if (result.getReturnCode() != 0) {
            for (String stdOutLine : result.getStdout().split("\n")) {
                Log.info(c, method, "Stdout: " + stdOutLine.trim());
            }

            for (String stdErrLine : result.getStderr().split("\n")) {
                Log.info(c, method, "Stderr: " + stdErrLine.trim());
            }
        }

        Assert.assertEquals("The " + feature.getShortName() + " feature should install correctly", 0, result.getReturnCode());
        List<String> features = new ArrayList<String>();
        features.add("wab-1.0");
        features.add(feature.getNamespacedShortName());
        server.changeFeatures(features);
        server.startServer();
        server.waitForStringInLog("CWWKT0016I.*" + feature.getNamespacedShortName());
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + feature.getURL());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly

        assertTrue("The output did not contain the automatically installed message",
                   line.contains(feature.getShortName()));

        for (String expectedIcon : feature.getExpectedIcons()) {
            // Check that the files are laid down on the filesystem
            // Not testing them deeply, as long as they are more than zero bytes
            assertTrue("The icon at " + expectedIcon + " should exist", server.fileExistsInLibertyInstallRoot(expectedIcon));
            assertTrue("The icon " + expectedIcon + " should not be zero bytes", server.getFileFromLibertyInstallRoot(expectedIcon).length() > 0);
        }

        for (String unexpectedIcon : feature.getUnexpectedIcons()) {
            // Check that the file is not laid down on the filesystem
            assertFalse("The icon at " + unexpectedIcon + " should not exist", server.fileExistsInLibertyInstallRoot(unexpectedIcon));
        }

        if (feature.getExpectedIcons().size() == 0) {
            assertFalse("No icons were expected, so the icons folder should not exist", server.fileExistsInLibertyInstallRoot(feature.getIconFolder()));
        }

    }

    /**
     * Both before and after each test we should ensure that none of the features are installed.
     * This should fail if we couldn't uninstall any features
     * 
     * @throws Exception
     */
    @Before
    @After
    public void tearDown() throws Exception {
        Iterator<IconFeature> iterator = iconFeaturesInstalled.iterator();
        List<String> featuresThatDidNotUninstall = new ArrayList<String>();
        while (iterator.hasNext()) {
            IconFeature iconFeature = iterator.next();
            if (removeFeature(iconFeature)) {
                iterator.remove();
            } else {
                featuresThatDidNotUninstall.add(iconFeature.getShortName());
            }
        }

        if (featuresThatDidNotUninstall.size() > 0) {
            Assert.fail("No features should have been left installed, but the following features were " + featuresThatDidNotUninstall);
        }

    }

    /**
     * Remove all files that are part of the feature, with the intention being to clean up the image
     * under test
     * 
     * @param feature the feature to remove
     * @return true if no files were left behind, false otherwise
     * @throws Exception if anything goes wrong
     */
    private boolean removeFeature(IconFeature feature) throws Exception {
        boolean returnCode = true;
        String method = "removeFeature";
        server.stopServer();
        server.deleteFileFromLibertyInstallRoot(feature.getEsaFile());

        // What we should be doing is using featureManager uninstall here, but the fact that its not available in V8.5.5.2 means that's not a good plan.
        server.deleteDirectoryFromLibertyInstallRoot(feature.getIconFolder());
        server.deleteFileFromLibertyInstallRoot(feature.getFeatureBundle());
        server.deleteFileFromLibertyInstallRoot(feature.getFeatureManifest());
// TODO this code should be re-enabled and replace the lines above when the uninstall command on featureManager is re-enabled
//        String cmd = server.getInstallRoot() + "/bin/featureManager uninstall " + feature.getShortName();
//        ProgramOutput result = server.getMachine().execute(cmd);
//        Log.info(c, method, "(" + feature.getShortName() + ") Got response " + result.getReturnCode());
//        if (result.getReturnCode() != 0) {
//            for (String stdOutLine : result.getStdout().split("\n")) {
//                Log.info(c, method, "(" + feature.getShortName() + ") Stdout: " + stdOutLine.trim());
//            }
//
//            for (String stdErrLine : result.getStderr().split("\n")) {
//                Log.info(c, method, "(" + feature.getShortName() + ") Stderr: " + stdErrLine.trim());
//            }
//            returnCode = false;
//        }

        for (String expectedIcon : feature.getExpectedIcons()) {
            // Check that the files that should have been laid down have gone
            // Not testing them deeply, as long as they are more than zero bytes
            if (server.fileExistsInLibertyInstallRoot(expectedIcon)) {
                Log.info(c, method, "The icon at " + expectedIcon + " should not exist after an uninstall");
                returnCode = false;
            }
        }
        if (server.fileExistsInLibertyInstallRoot(feature.getIconFolder())) {
            Log.info(c, method, "The icon folder at " + feature.getIconFolder() + " should not exist after an uninstall");
            returnCode = false;
        }

        return returnCode;
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     * 
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
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
    private HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }
}