/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tests.anno.jandex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.tests.anno.util.AppPackagingHelper;

import componenttest.topology.utils.FileUtils;

/**
 * Common test class for Jandex enablement.
 */
public abstract class JandexAppTest extends LoggingTest {
    /**
     * Answer the server used by this test.
     * 
     * Still abstract: Present to document the continued need of
     * subclasses to provide an implementation.
     */
    @Override
    protected abstract SharedServer getSharedServer();

    //

    /**
     * Common startup: Perform startup steps on the shared server.
     */
    public static void setUp(
        Logger logger,
        SharedServer sharedServer,
        String serverConfigName, String jvmOptionsName) throws Exception {

    	if ( serverConfigName != null ) {
    		install(logger, sharedServer, serverConfigName, "server.xml");
    	}

    	if ( jvmOptionsName != null ) {
    		install(logger, sharedServer, jvmOptionsName, "jvm.options");
    	}

        logger.info("setUp: Add TestServlet40 to the server applications folder");
        AppPackagingHelper.addEarToServerApps(
            sharedServer.getLibertyServer(),
            "TestServlet40.ear", // earName
            true, // addEarResources
            "TestServlet40.war", // warName
            true, // addWarResources
            "TestServlet40.jar", // jarName
            true, // addJarResources
            "testservlet40.war.servlets", "testservlet40.jar.servlets"); // packageNames

        logger.info("setUp: Added TestServlet40 to the server applications folder");

        logger.info("setUp: Launch server");
        sharedServer.startIfNotStarted();
        logger.info("setUp: Launched server");

        logger.info("setUp: Validate application startup");
        sharedServer.getLibertyServer().addInstalledAppForValidation("TestServlet40");
        logger.info("setUp: The application has started");

    }

    public static void tearDown(Logger logger, SharedServer sharedServer) throws Exception {
        logger.info("tearDown: Stop server");
        sharedServer.getLibertyServer().stopServer("CWWKZ0014W");
        logger.info("tearDown: Stoped server");
    }

    //

    public static final String SOURCE_PATH = "publish/servers/annoFat_server/config";

    protected static void install(
        Logger logger,
        SharedServer targetServer,
        String sourceName, String targetName) throws Exception {

        File sourceFile = new File(SOURCE_PATH + "/" + sourceName);
        String sourcePath = sourceFile.getAbsolutePath();

        String targetRootPath = targetServer.getLibertyServer().getServerRoot();
        File targetFile = new File(targetRootPath + "/" + targetName);
        String targetPath = targetFile.getAbsolutePath();

        logger.info("install: Source [ " + sourcePath + " ] target [ " + targetPath + " ]");

        assertTrue("Source does not exist [ " + sourcePath + " ]", sourceFile.exists());

        if ( targetFile.exists() ) {
        	targetFile.delete();
        }
        FileUtils.copyFile(sourceFile, targetFile);
        assertTrue("Target was not created [ " + targetPath + " ]", targetFile.exists());
    }

    //
    
    public static final String CONTEXT_ROOT = "/TestServlet40";
    public static final String SIMPLE_SERVLET_URL = CONTEXT_ROOT + "/SimpleTestServlet";
    public static final String MY_SERVLET_URL = CONTEXT_ROOT + "/MyServlet";

    //

    /**
     * Verify that test application is handling requests.
     */
    public void testServletIsRunning() throws Exception {
        verifyResponse(SIMPLE_SERVLET_URL, "Hello World");
    }

    /**
     * Verify that the test application is using Servlet 3.1.
     */
    public void testServletIsRunning31() throws Exception {
        WebResponse response = verifyResponse(MY_SERVLET_URL, "Hello World");
        response.verifyResponseHeaderEquals("X-Powered-By", false, "Servlet/3.1", true, false);
    }

    /**
     * Verify that the test application answers "4" and "0" as the
     * major and minor Servlet versions.
     */
    public void testServletVersions() throws Exception {
        verifyResponse(MY_SERVLET_URL + "?TestMajorMinorVersion=true", "majorVersion: 3");
        verifyResponse(MY_SERVLET_URL + "?TestMajorMinorVersion=true", "minorVersion: 1");
    }

    public static final boolean DO_EXPECT_JANDEX = true;
    public static final boolean DO_NOT_EXPECT_JANDEX = false;

    /**
     * Verify that jandex function was not trigger: Verify that no "CWWC0092I"
     * messages appear in the server logs.
     */
    public void testJandex(boolean expectJandex) throws Exception {
        // Search for message indicating Jandex is being used.
        //
        // ANNO_JANDEX_USAGE=
        // CWWKC0092I: Read Jandex indexes for {0} out of {1} archives ({2} out of {3} classes) in {4}.     	
        //
        // ANNOCACHE_JANDEX_USAGE=CWWKC0093I: 
        // Jandex coverage for of module {1} in application {0}: 
        // {3} of {2} module locations had Jandex indexes; 
        // {5} of {4} module classes were read from Jandex indexes.

    	// [5/11/19 21:52:18:428 EDT] 00000070 com.ibm.ws.annocache
    	// I CWWKC0093I: Jandex coverage for of module /TestServlet40.war in application TestServlet40:
    	// 1 of 2 module locations had Jandex indexes;
    	// 2 of 2 module classes were read from Jandex indexes.
        List<String> selectedMessages = getSharedServer().getLibertyServer().findStringsInLogs("CWWKC009");

        if ( expectJandex ) {
            assertFalse("Did not find CWWKC0092I or CWWKC0093I, which indicates the use of jandex indexes.", selectedMessages.isEmpty());
        } else {
            assertTrue("Found CWWKC0092I or CWWKC0093I, which indicates the use of jandex indexes", selectedMessages.isEmpty());
        }
    }

    // No longer in use.  Retained for future need.

    protected String parseResponse(WebResponse response, String initialText, String finalText) {
        String responseBody = response.getResponseBody();

        int beginTextIndex = responseBody.indexOf(initialText);
        if ( beginTextIndex < 0 ) {
            return "Initial text [ " + initialText + " ] not found";
        }

        int endTextIndex = responseBody.indexOf(finalText, beginTextIndex);
        if ( endTextIndex < 0 ) {
            return "Final text [ " + finalText + " ]not found";
        }

        String middleText = responseBody.substring(beginTextIndex + initialText.length(), endTextIndex);
        return middleText;
    }
}
