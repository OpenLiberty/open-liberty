/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tests.anno.caching;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LooseConfigTest extends AnnoCachingTest {
    private static final Logger LOG = Logger.getLogger(LooseConfigTest.class.getName());
    
    private static final String APP_SOURCE = "test-applications/LooseConfig/";

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("setUp: ENTER: LooseConfigTest");

        // Required initialization of super class
        setEarName("LooseConfig.ear.xml");     // using a Loose Config xml in lieu of an actual EAR.
        setSharedServer();

        // The server.xml defines an application named LooseConfig.ear.  The server will not find that
        // but it will look for LooseConfig.ear.xml which indicates where to find the application files.
        installServerXml("looseConfig_server.xml");  

        // First Clean up from any prior tests that used this app
        deleteApplication();
        //deleteExpandedApplication();

        deployLooseApplication();

        startServerScrub();

        waitForConsole("LISTENER 2 EXITED");

        LOG.info("setUp: RETURN");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOG.info("tearDown: ENTER");
        stopServer();
        LOG.info("tearDown: RETURN");
    }

    /**
     * Deploy Loose App to wlp/usr/server/<SERVER_NAME>/apps/looseConfig directory
     * 
     * Deploy LooseConfigApp.ear.xml  to wlp/usr/server/<SERVER_NAME>/apps  (maps the loose config app to the virtual EAR)
     * 
     *                         SOURCE LOCATION (Build tree)
     *                         ============================
     *        test-applications/looseConfig/earContent              -- META-INF/application.xml for EAR
     *        test-applications/looseConfig/LooseWeb1/WebContent    -- META-INF and index.jsp for LooseWeb1.war
     *        test-applications/looseConfig/LooseWeb2/WebContent    -- META-INF and index.jsp for LooseWeb2.war
     *        build/classes/java/main/looseweb1                     -- classes for WEB-INF/classes for LooseWeb1.war
     *        build/classes/java/main/looseweb2 (dynamically added) -- classes for WEB-INF/classes for LooseWeb2.war
     *        build/classes/java/main/looseservlet1                 -- classes for LooseServlet1.jar
     *        build/classes/java/main/looseservlet2                 -- classes for LooseServlet2.jar
     *           
     *        PHYSICAL LOCATION  (Deployed)                 VIRTUAL EAR
     *        =============================                 ===========
     *        apps/looseConfig
     *                     /earContent                      /
     *                     /LooseWeb1                       LooseWeb1.war
     *                     /LooseWeb1/classes                     WEB-INF/classes
     *                     /LooseServlet1.jar                     WEB-INF/lib/LooseServlet1.jar
     *                     /LooseWeb2                       LooseWeb2.war
     *                     /LooseWeb2/classes                     WEB-INF/classes
     *                     /LooseServlet2.jar                     WEB-INF/lib/LooseServlet2.jar
     *  
     *  
     *    Application URLs
     *    ================
     *     http://localhost:<Port#>/MyLooseWeb1
     *     http://localhost:<Port#>/MyLooseWeb1/Wobbly
     *     http://localhost:<Port#>/MyLooseWeb1/Servlet1
     *     http://localhost:<Port#>/MyLooseWeb2
     *     http://localhost:<Port#>/MyLooseWeb1/Servlet2
     *
     * @throws IOException
     */
    public static void deployLooseApplication() throws IOException {
        String looseAppDir = getLibertyServer().getServerRoot() + "/apps/looseConfig/";

        mkDir(looseAppDir + "earContent", DELETE_IF_EXISTS);
        mkDir(looseAppDir + "LooseWeb1/WebContent", DELETE_IF_EXISTS);
        mkDir(looseAppDir + "LooseWeb1/classes/looseweb1", DELETE_IF_EXISTS);
        mkDir(looseAppDir + "LooseServlet1.jar/looseservlet1", DELETE_IF_EXISTS);
        mkDir(looseAppDir + "LooseWeb2/WebContent", DELETE_IF_EXISTS);
        mkDir(looseAppDir + "LooseWeb2/classes", DELETE_IF_EXISTS);
        mkDir(looseAppDir + "LooseServlet2.jar/looseservlet2", DELETE_IF_EXISTS);
        mkDir(looseAppDir + "earContent", DELETE_IF_EXISTS);

        copyFolder(APP_SOURCE + "earContent", looseAppDir + "earContent");                          // application.xml
        copyFolder(APP_SOURCE + "LooseWeb1.war/WebContent", looseAppDir + "LooseWeb1/WebContent");  // LooseWeb1.war META-INF and index.jsp
        copyFolder("build/classes/looseweb1", looseAppDir + "LooseWeb1/classes/looseweb1");         // LooseWeb1.war WEB-INF/classes
        copyFolder(APP_SOURCE + "LooseWeb2.war/WebContent", looseAppDir + "LooseWeb2/WebContent");  // LooseWeb2.war META-INF and index.jsp

        copyFolder("build/classes/looseservlet1", looseAppDir + "LooseServlet1.jar/looseservlet1");  // classes for LooseServlet1.jar
        copyFolder("build/classes/looseservlet2", looseAppDir + "LooseServlet2.jar/looseservlet2");  // classes for LooseServlet2.jar
        
        copyFolder(APP_SOURCE + "LooseServlet1.jar/resources", looseAppDir + "LooseServlet1.jar"); // META-INF for LooseServlet1.jar
        copyFolder(APP_SOURCE + "LooseServlet2.jar/resources", looseAppDir + "LooseServlet2.jar"); // META-INF for LooseServlet2.jar

        // Copy the loose config XML file to server apps directory.
        copyFile(APP_SOURCE + "" + earFileName,   SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/");
    }

    /**
     * Test updating the Loose XML file to include files that are already in 
     * the physical location.
     * @throws Exception
     */
    @Test
    public void looseConfig_testLooseAppUpdates() throws Exception {
        // Verify the initial state.  The first Loose XML has WAR1 commented out.
        verifyBadUrl("/MyLooseWeb1");
        verifyBadUrl("/MyLooseWeb1/Wobbly");
        verifyBadUrl("/MyLooseWeb1/Servlet1"); 
        verifyResponse("/MyLooseWeb2",          "Hi, this is loose web2.");
        verifyResponse("/MyLooseWeb2/Servlet2", "Hello From Servlet 2.");        
        verifyBadUrl("/MyLooseWeb2/AddedServlet");  // AddedServlet not added at this point.

        // Copy the second loose config XML file to server apps directory.
        copyFile(APP_SOURCE + "LooseConfig2.ear.xml" ,   SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/"  + earFileName);
        waitForAppUpdate();

        // The second Loose XML un-comments WAR1 and comments WAR2.  Testing removing something
        // and adding something to the loose xml.
        verifyResponse("/MyLooseWeb1",          "Hi, this is loose web1.");
        verifyResponse("/MyLooseWeb1/Wobbly",   "They call me Wobbly, cause I'm a little loose.");
        verifyResponse("/MyLooseWeb1/Servlet1", "Hello From Servlet 1.");
        verifyBadUrl("/MyLooseWeb2");
        verifyBadUrl("/MyLooseWeb2/Servlet2");
        verifyBadUrl("/MyLooseWeb2/AddedServlet");

        // Copy the third loose config XML file to server apps directory.
        copyFile(APP_SOURCE + "LooseConfig3.ear.xml" ,   SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/"  + earFileName);
        waitForAppUpdate();

        // The third Loose XML un-comments WAR1 and WAR2
        verifyResponse("/MyLooseWeb1",          "Hi, this is loose web1.");
        verifyResponse("/MyLooseWeb1/Wobbly",   "They call me Wobbly, cause I'm a little loose.");
        verifyResponse("/MyLooseWeb1/Servlet1", "Hello From Servlet 1.");        
        verifyResponse("/MyLooseWeb2",          "Hi, this is loose web2.");
        verifyResponse("/MyLooseWeb2/Servlet2", "Hello From Servlet 2.");
        verifyBadUrl("/MyLooseWeb2/AddedServlet");  // AddedServlet still not added at this point.

        // To this point, we have only changed the loose xml.  Now add a servlet to a
        // location already configured in the loose XML.   Copy the "AddedServlet" into WAR2.
        String looseAppDir = getServerRoot() + "/apps/looseConfig/";
        mkDir(looseAppDir + "LooseWeb2/classes/looseweb2", DELETE_IF_EXISTS);
        copyFolder("build/classes/looseweb2", looseAppDir + "LooseWeb2/classes/looseweb2");
        waitForAppUpdate();

        // All servlets should be available now.
        verifyResponse("/MyLooseWeb1",          "Hi, this is loose web1.");
        verifyResponse("/MyLooseWeb1/Wobbly",   "They call me Wobbly, cause I'm a little loose.");
        verifyResponse("/MyLooseWeb1/Servlet1", "Hello From Servlet 1.");        
        verifyResponse("/MyLooseWeb2",          "Hi, this is loose web2.");
        verifyResponse("/MyLooseWeb2/Servlet2", "Hello From Servlet 2.");
        verifyResponse("/MyLooseWeb2/AddedServlet", "This servlet is not installed at the beginning of the test.  It is added later.");      
    }
}
