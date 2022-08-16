/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;
import com.ibm.ws.microprofile.config13.variableServerXML.web.VariableServerXMLServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class VariableServerXMLTest extends FATServletClient {

    public static final String SERVER_NAME = "ServerXMLVariableServer";
    public static final String APP_NAME = "variableServerXMLApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = VariableServerXMLServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP50, MicroProfileActions.MP41, MicroProfileActions.MP20);

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultApp(server, APP_NAME, options, "com.ibm.ws.microprofile.config13.variableServerXML.*");
        server.copyFileToLibertyServerRoot("original/variableServerXMLApp.xml");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Copy a server config file to the server root and wait for notification that the server config has been updated
     *
     * @param filename
     * @throws Exception
     */
    private static void copyConfigFileToLibertyServerRoot(String srcFile, String destFile) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(srcFile, destFile);

        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false);

        Thread.sleep(ConfigConstants.DEFAULT_DYNAMIC_REFRESH_INTERVAL * 2); // We need this pause so that the MP config change is picked up through the polling mechanism
    }

    @Before
    public void resetConfigFile() throws Exception {
        copyConfigFileToLibertyServerRoot("original/variableServerXMLApp.xml", "variableServerXMLApp.xml");
    }

    @Test
    public void testChangeVariablesConfig() throws Exception {

        // run the "before" test to check the value of the variable before the server.xml is updated
        test(server, "/variableServerXMLApp/ServerXMLVariableServlet?testMethod=varPropertiesBeforeTest");

        //update the config
        copyConfigFileToLibertyServerRoot("refreshVariables/variableServerXMLApp.xml", "variableServerXMLApp.xml");

        // run the "after" test to check the value of the variable after the server.xml is updated
        test(server, "/variableServerXMLApp/ServerXMLVariableServlet?testMethod=varPropertiesAfterTest");
    }

    @Test
    public void testChangeAppPropertiesConfig() throws Exception {

        // run the "before" test to check the value of the variable before the server.xml is updated
        test(server, "/variableServerXMLApp/ServerXMLVariableServlet?testMethod=appPropertiesBeforeTest");

        //update the config
        copyConfigFileToLibertyServerRoot("refreshAppProperties/variableServerXMLApp.xml", "variableServerXMLApp.xml");

        // run the "after" test to check the value of the variable after the server.xml is updated
        test(server, "/variableServerXMLApp/ServerXMLVariableServlet?testMethod=appPropertiesAfterTest");
    }

    private void test(LibertyServer server, String testUri) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + testUri);
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            assertNotNull(is);

            String output = read(is);
            assertTrue(output, output.trim().startsWith("SUCCESS"));
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
