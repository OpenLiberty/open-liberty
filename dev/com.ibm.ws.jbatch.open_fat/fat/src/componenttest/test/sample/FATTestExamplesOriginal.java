/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.test.sample;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class FATTestExamplesOriginal {
    //    private static LibertyServer server = LibertyServerFactory.getLibertyServer();
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("exampleTestServer");
    private final Class<?> c = FATTestExamplesOriginal.class;

    /**
     * TestDescription:
     * This test is testing that the auto-install feature of the test Framework is
     * working correctly. The application "auto" was placed in the autoinstall folder
     * and so should be already be installed. So to check this we start the server,
     * then attempt to access the application. At the end we uninstall the application
     *
     * The @Test annotation has been left so this test case will run under FATSuiteLite
     */
    @Test
    public void testAutoInstall() throws Exception {
        server.startServer();
        // Pause for application to start properly and server to say it's listening on ports
        server.waitForStringInLog("port " + server.getHttpDefaultPort());
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/auto");
        Log.info(c, "testAutoInstall",
                 "Calling Auto Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly

        assertTrue("The output did not contain the automatically installed message",
                   line.contains("This is AutoServlet"));
    }

    /**
     * TestDescription:
     * This test is simply testing that the server can be started and then stopped.
     * To start a server you run server.startServer() if you want the output to go
     * to some other file that the default console.SNAPSHOT.log you can specify it
     * as a String parameter. e.g. server.startServer("alternativeOutput.log")
     *
     * The @Test annotation has been left so this test case will run under FATSuiteLite
     */
    @Test
    public void testStartStop() throws Exception {
        try {
            server.startServer();
        } finally {
            server.stopServer();
        }
    }

    /**
     * TestDescription:
     * This test that you can change the server.xml file mid-test using the
     * setServerConfigurationFile() method. Because a server.xml
     * was in the files folder it got loaded on server Creation. As such the
     * current server.xml has a hidden message, which we look for now.
     * Then we upload the new server.xml from the folder directory tree
     * and check again for the newer message.
     */
    //annotated for use in Full test only
    @Test
    @Mode(TestMode.FULL)
    public void testConfigFileChange() throws Exception {
        //You can validate any file for a specific string.
        //There are various get methods (getServerConfigurationFile(), getDefaultLogFile())
        //to get specific Liberty files.
        final String uploadMessage = server.waitForStringInLog("TESTING THE UPLOAD!", server.getServerConfigurationFile());
        assertNotNull("Could not find the upload message in the file", uploadMessage);

        server.setServerConfigurationFile("server.xml");
        // Check for the Uploaded message and fail if it's not found
        final String changedMessageFromLog = server.waitForStringInLog("NOW THIS MESSAGE CHANGED", server.getServerConfigurationFile());
        assertNotNull("Could not find the upload message in the new file", changedMessageFromLog);

    }

    /**
     * TestDescription:
     * This test tests that the serverDirectoryContents correctly returns a list
     * of the contents of a directory. We request the contents of the defaultServer
     * directory, as the server has already been started (probably more than once)
     * it should have a workarea directory. Once we have the list we check that
     * the list contains it and also contains a server.xml
     */
    //annotated for use in Full test only
    @Test
    @Mode(TestMode.FULL)
    public void testServerDirectoryContents() throws Exception {
        try { /* starting here again in case test order is not preserved */
            server.startServer();
        } finally {
            server.stopServer();
        }
        ArrayList<String> contents = server.listLibertyServerRoot(null, null);
        assertTrue("The Server does not have a workarea directory",
                   contents.contains("workarea"));

        assertTrue("The Server does not have a server.xml file", contents.contains("server.xml"));
    }

    /**
     * TestDescription:
     * This tests that you can update the features that are defined in the server.xml
     * using the changeFeatures() method. At the start we should have servlet-3.0
     * only. Afterwards we change the features by first removing and then adding new ones
     * before returning back to the start of servlet-3.0.
     */
    //annotated for use in Full test only, can shortcut to @Mode for @Mode(TestMode.FULL)
    @Test
    @Mode
    public void testUpdateFeatures() throws Exception {
        //Rather than look for a specifc string in the server config file
        //you can call validateServerConfigIncludesFeatures() with a list of features you are expecting
        assertTrue("The server.xml should contain the servlet-3.0 feature",
                   doesServerConfigIncludeFeature("servlet-3.0"));

        assertFalse("The server.xml shouldn't contain the ssl-1.0 feature", doesServerConfigIncludeFeature("ssl-1.0"));

        List<String> toChangeTo = new ArrayList<String>();
        toChangeTo.add("httptransport-1.0");
        server.changeFeatures(toChangeTo);
        assertTrue("The server.xml did did contain the httptransport-1.0 feature",
                   doesServerConfigIncludeFeature("httptransport-1.0"));

        assertFalse("The server.xml did not contain the ssl-1.0 feature",
                    doesServerConfigIncludeFeature("ssl-1.0"));

        assertFalse("The server.xml did not contain the servlet-3.0 feature",
                    doesServerConfigIncludeFeature("servlet-3.0"));

        List<String> toChangeToAgain = new ArrayList<String>();
        toChangeToAgain.add("httptransport-1.0");
        toChangeToAgain.add("ssl-1.0");
        toChangeToAgain.add("jpa-2.0");
        server.changeFeatures(toChangeToAgain);
        assertTrue("The server.xml did contain the httptransport-1.0 feature",
                   doesServerConfigIncludeFeature("httptransport-1.0"));
        assertTrue("The server.xml did contain the jpa-2.0 feature",
                   doesServerConfigIncludeFeature("jpa-2.0"));
        assertTrue("The server.xml did contain the ssl-1.0 feature",
                   doesServerConfigIncludeFeature("ssl-1.0"));

        assertFalse("The server.xml contained the servlet-3.0 feature",
                    doesServerConfigIncludeFeature("servlet-3.0"));

        List<String> defaults = new ArrayList<String>();
        defaults.add("ssl-1.0");
        defaults.add("servlet-3.0");
        server.changeFeatures(defaults);
    }

    /**
     * @param serverConfigurationFile
     * @param refreshTimeout
     * @return
     * @throws Exception
     */
    private boolean doesServerConfigIncludeFeature(String feature) throws Exception {
        final RemoteFile serverConfigurationFile = server.getServerConfigurationFile();
        // Give the server 100 ms to update files
        int refreshTimeout = 100;
        return (server.waitForStringInLog(feature, refreshTimeout, serverConfigurationFile) != null);
    }

    /**
     * TestDescription:
     * This test tests that you can install an application using the installApplication
     * later rather than using the automatic install feature at creation. Because in
     * previous tests we have been changing the features we ensure that the features
     * area correct for the snoop application. Then we start the server before installing
     * the application and attempting to access it.
     */
    //annotated for use in Full test only, can shortcut to @Mode for @Mode(TestMode.FULL)
    @Test
    @Mode
    public void testInstallApplication() throws Exception {
        final String method = "testInstallApplication";
        List<String> defaults = new ArrayList<String>();
        defaults.add("servlet-3.0");
        server.changeFeatures(defaults);

        //server.installApp("snoop.zip");
        server.startServer();
        // Make sure the application has come up before proceeding
        // (Hardcode the message code and application name, but leave the rest flexible)
        assertNotNull("The application snoop did not appear to have been installed.", server.waitForStringInLog("CWWKZ0001I.* snoop"));
        // TODO this assertion fails because of a snoop error, but I think the test isn't very valid assertNotNull("The application snoop did not appear to have started.", server.waitForStringInLog("CWWKZ0001I.* snoop"));
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
        Log.info(c, method, "Calling Snoop Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        assertTrue("The response did not contain the \'Snoop Servlet\'",
                   line.contains("Snoop Servlet"));
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
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