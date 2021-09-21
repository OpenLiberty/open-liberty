/*******************************************************************************
 * Copyright (c) 2018,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tests.anno.caching;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LooseConfigTest extends AnnoCachingTest {
    private static final Logger LOG = Logger.getLogger(LooseConfigTest.class.getName());

    /**
     * File utility.  Display information about a file.
     *
     * @param file The file which is to be displayed.
     */
    public static void displayStamp(File file) {
        LOG.info("      Name [ " + file.getName() + " ]");
        LOG.info("      Path [ " + file.getPath() + " ]");
        LOG.info("  Abs Path [ " + file.getAbsolutePath() + " ]");
        LOG.info("    Exists [ " + file.exists() + " ]");
        LOG.info("     IsDir [ " + file.isDirectory() + " ]");
        LOG.info("   Updated [ " + file.lastModified() + " ]");
    }

    /**
     * Utility to copy and then update the last modified time of a file.
     *
     * The update of the last modified time is necessary for file notification
     * to detech that the file was changed.
     *
     * @param sourcePath The source file which is being copied.
     * @param targetPath The target file which is receiving the copy.
     *
     * @throws IOException Thrown in case of an error.
     */
    public static void copyAndTouch(String sourcePath, String targetPath) throws IOException {
        File targetFile = new File(targetPath);

        displayStamp(targetFile);

        copyFile(sourcePath, targetPath); // throws IOException
        touch(targetFile); // throws IOException

        displayStamp(targetFile);
    }

    /**
     * Update the last modified time of a target file to now.
     *
     * @param file The target file.
     *
     * @return The current time.
     *
     * @throws IOException Thrown in case of an error.
     */
    public static FileTime touch(File file) throws IOException {
        Path path = file.toPath();
        FileTime now = FileTime.fromMillis( System.currentTimeMillis() );
        Files.setLastModifiedTime(path, now); // throws IOException
        return now;
    }

    private static final String APP_SOURCE = "test-applications/LooseConfig/";

    private static final String LOOSE_APP_DIR = "apps/looseConfig";

    private static String getLooseAppDir() {
        return getLibertyServer().getServerRoot() + '/' + LOOSE_APP_DIR;
    }

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
     * Deploy loose app to wlp/usr/server/<SERVER_NAME>/apps/looseConfig directory
     * 
     * Deploy LooseConfigApp.ear.xml  to wlp/usr/server/<SERVER_NAME>/apps  (maps the loose config app to the virtual EAR)
     * <code>
     *        SOURCE LOCATION (Build tree)
     *        ============================
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
     *    Application URLs
     *    ================
     *     http://localhost:<Port#>/MyLooseWeb1
     *     http://localhost:<Port#>/MyLooseWeb1/Wobbly
     *     http://localhost:<Port#>/MyLooseWeb1/Servlet1
     *     http://localhost:<Port#>/MyLooseWeb2
     *     http://localhost:<Port#>/MyLooseWeb1/Servlet2
     * </code>
     */
    public static void deployLooseApplication() throws Exception {
        String looseAppDir = getLooseAppDir() + '/';

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

        copyAndTouch(APP_SOURCE + earFileName, getApplicationPath());
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
        verifyResponse("/MyLooseWeb2", "Hi, this is loose web2.");
        verifyResponse("/MyLooseWeb2/Servlet2", "Hello From Servlet 2.");        
        verifyBadUrl("/MyLooseWeb2/AddedServlet");  // AddedServlet not added at this point.

        copyAndTouch(APP_SOURCE + "LooseConfig2.ear.xml", getApplicationPath());
        waitForAppUpdate();

        // The second Loose XML un-comments WAR1 and comments WAR2.  Testing removing something
        // and adding something to the loose xml.
        verifyResponse("/MyLooseWeb1", "Hi, this is loose web1.");
        verifyResponse("/MyLooseWeb1/Wobbly", "They call me Wobbly, cause I'm a little loose.");
        verifyResponse("/MyLooseWeb1/Servlet1", "Hello From Servlet 1.");
        verifyBadUrl("/MyLooseWeb2");
        verifyBadUrl("/MyLooseWeb2/Servlet2");
        verifyBadUrl("/MyLooseWeb2/AddedServlet");

        copyAndTouch(APP_SOURCE + "LooseConfig3.ear.xml" , getApplicationPath());
        waitForAppUpdate();

        // The third Loose XML un-comments WAR1 and WAR2
        verifyResponse("/MyLooseWeb1", "Hi, this is loose web1.");
        verifyResponse("/MyLooseWeb1/Wobbly", "They call me Wobbly, cause I'm a little loose.");
        verifyResponse("/MyLooseWeb1/Servlet1", "Hello From Servlet 1.");        
        verifyResponse("/MyLooseWeb2", "Hi, this is loose web2.");
        verifyResponse("/MyLooseWeb2/Servlet2", "Hello From Servlet 2.");
        verifyBadUrl("/MyLooseWeb2/AddedServlet");  // AddedServlet still not added at this point.

        // To this point, we have only changed the loose xml.  Now add a servlet to a
        // location already configured in the loose XML.   Copy the "AddedServlet" into WAR2.
        String looseAppPath = getLooseAppDir() + '/' + "LooseWeb2/classes/looseweb2";
        mkDir(looseAppPath, DELETE_IF_EXISTS);
        copyFolder("build/classes/looseweb2", looseAppPath);
        // Don't need to touch the folder ... the file should be noticed as a new file.
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
