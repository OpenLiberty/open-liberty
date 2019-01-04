/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.anno.tests.caching;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * These tests to verify that updates to the <absolute-ordering> element in the web.xml
 * are noticed after server restarts.
 *
 */
public class AbsoluteOrderingTest extends CachingTest {
    private static final Logger LOG = Logger.getLogger(AbsoluteOrderingTest.class.getName());

    enum ServerStartType 
    { 
        NONE, CLEAN, DIRTY; 
    }
    //

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("setUp AbsoluteOrderingTest");
       
        setEarName("TestServlet40.ear");
        setSharedServer();
        
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  // Default Jandex settings.  NOT using Jandex.
        
        // First Clean up from any prior tests that used this app
        deleteApplicationFile();
        deleteExpandedApplication();
        
        // Copy the test application to the server.
        addAppToServerAppsDir( createApp() );
        
        // Here we could expand the application and insert the web.xml that we
        // want to start the test with, but instead we will start and stop the
        // server which will expand the application
        
        // Starting and stopping the server will expand the application
        //     expandApplication(EAR_FILE_NAME);

        long lStartTime = System.nanoTime();
        startServerClean();

        long lEndTime = System.nanoTime();        
        long elapsed = lEndTime - lStartTime;
        LOG.info("Server started in milliseconds: " + elapsed / 1000000);        

        stopServer();

        LOG.info("setUp exit");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        LOG.info("cleanUp");
        stopServer();
        LOG.info("cleanUp complete");
    }
    
    /**
     * Test various changes to the web.xml that affect absolute ordering.  Restart the server
     * after each web.xml update.
     * @throws Exception
     */
    @Test  /*  Expensive test because of the server estarts */
    public void testAbsoluteOrdering() throws Exception {

        // Start with a web.xml with absolute-ordering containing Fragments A & B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.CLEAN, "Initial Test with web.xml containing Fragments A & B");       
        
        // Now replace web.xml, adding "JAR C" to the absolute-ordering element, and start the server
        // dirty to see if the anno cache gets updated properly.
        tryWebXmlWithAbsoluteOrder_A_B_C(ServerStartType.DIRTY, "Test adding Fragment C to web.xml");
          
        // Now go back to using the previous web.xml.  The effect is that "Fragment C" is removed from the
        // absolute order.  So it's servlet should no longer respond.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.DIRTY, "Test removing Fragment C from web.xml");
        
        // Add an others element.  Now all of the servlets in the fragments should respond.
        tryWebXmlWithAbsoluteOrder_A_B_Others(ServerStartType.DIRTY, "Test with web.xml with ordering A B others");

        // Move C to the front.  All of the servlets in the fragments should respond.
        tryWebXmlWithAbsoluteOrder_C_D_A_B_Others(ServerStartType.DIRTY, "Test with web.xml with ordering C D A B others");
        
        tryRemovingAndAddingAJar(ServerStartType.DIRTY, "Test removing TestServletD.jar from app.");     
    }
    
    @Test
    public void testCacheValidate() throws Exception {
        installJvmOptions("JvmOptions_AnnoCacheValidate_True.txt");
        startServerClean();
        
        // Start with a web.xml with absolute-ordering containing Fragments A & B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.NONE, "Initial Test with web.xml containing Fragments A & B");       
  
        LOG.info("Stopping server");
        stopServer("CWWKZ0014W", "SRVE0190E");      
    }
    
    @Test
    public void testReducedScanThreads() throws Exception {
        installJvmOptions("JvmOptions_ComIbmWsAnnoScanThreads_1.txt");
        startServerClean();
        
        // Start with a web.xml with absolute-ordering containing Fragments A & B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.NONE, "Test reducing number of scan threads to 1");       
  
        LOG.info("Stopping server");
        stopServer("CWWKZ0014W", "SRVE0190E");      
        
        //-----------------------------------------------------------------------------
        
        installJvmOptions("JvmOptions_ComIbmWsAnnoScanThreads_-1.txt");
        startServerClean();
        
        // Start with a web.xml with absolute-ordering containing Fragments A & B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.NONE, "Test reducing number of scan threads to -1");    
  
        LOG.info("Stopping server");
        stopServer("CWWKZ0014W", "SRVE0190E"); 
    }
    
    @Test
    public void testReducedWriteThreads() throws Exception {
        installJvmOptions("JvmOptions_AnnoCacheWriteThreads_1.txt");
        startServerClean();
        
        // Start with a web.xml with absolute-ordering containing Fragments A & B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.NONE, "Test reducing number of write threads to 1");       
  
        LOG.info("Stopping server");
        stopServer("CWWKZ0014W", "SRVE0190E"); 
        
        //----------------------------------------------------------------------
        
        installJvmOptions("JvmOptions_AnnoCacheWriteThreads_-1.txt");
        startServerClean();
        
        // Start with a web.xml with absolute-ordering containing Fragments A & B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.NONE, "Test reducing number of write threads to -1");    
  
        LOG.info("Stopping server");
        stopServer("CWWKZ0014W", "SRVE0190E"); 
    }
    
    /**
     * Test various changes to the web.xml that affect absolute ordering.  Rely on Liberty to detect
     * the change in the app WITHOUT restarting the server.
     * @throws Exception
     */
    @Test    
    public void testAbsoluteOrderingNoRestart() throws Exception {

        // Since the individual test methods ("try" methods) do not start and stop the server, we
        // start and stop the server at the beginning and end of this method.
        startServerClean();
        
        // Start with a web.xml with absolute-ordering containing Fragments A & B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.NONE, "Initial Test with web.xml containing Fragments A & B");       
        
        // Now replace web.xml, adding "JAR C" to the absolute-ordering element, and start the server
        // dirty to see if the anno cache gets updated properly.
        tryWebXmlWithAbsoluteOrder_A_B_C(ServerStartType.NONE, "Test adding Fragment C to web.xml");
          
        // Now go back to using the previous web.xml.  The effect is that "Fragment C" is removed from the
        // absolute order.  So it's servlet should no longer respond.
        tryWebXmlWithAbsoluteOrder_A_B(ServerStartType.NONE, "Test removing Fragment C from web.xml");
        
        // Add an others element.  Now all of the servlets in the fragments should respond.
        tryWebXmlWithAbsoluteOrder_A_B_Others(ServerStartType.NONE, "Test with web.xml with ordering A B others");

        // Move C to the front.  All of the servlets in the fragments should respond.
        tryWebXmlWithAbsoluteOrder_C_D_A_B_Others(ServerStartType.NONE, "Test with web.xml with ordering C D A B others");
        
        tryRemovingAndAddingAJar(ServerStartType.NONE, "Test removing TestServletD.jar from app.");
        
        LOG.info("Stopping server");
        stopServer("CWWKZ0014W", "SRVE0190E");
    }
    
    /**
     * Absolute ordering of only A & B must hide all other JARs and the servlets therein.
     * 
     * @param serverStartType - clean, dirty, or none
     * @throws Exception
     */
    private void tryWebXmlWithAbsoluteOrder_A_B(ServerStartType serverStartType, String msg) throws Exception {

        logBlock(msg + " - Server start type -> " +  serverStartType);        
        
        // Start with an initial web.xml has absolute-ordering that specifies only JARs A & B
        replaceWebXmlInExpandedApp("web-absolute-ordering-a-b.xml");
        
        // When the server restarts, it re-expands the .ear file into the expanded app directory.
        // So it will wipe out the change we just made.  To prevent that we need to 
        // replace the .ear file file with the contents of the expanded app directory tree.
        replaceApplicationFileFromExpandedApp();
              
        switch (serverStartType) {
        case CLEAN: 
            startServerClean();
        case DIRTY:
            startServerDirty();
        case NONE:
            waitForAppUpdateToBeNoticed();
        }
        
        displayWebXml();
        
        // MyServlet should work because WEB-INF/classes is processed before the absolute order is considered
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};
        
        /////// Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!"; 
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ AB ]";
        
        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);
             
        //////// Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!"; 
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ AB ]";
        
        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);
        
        // All others are excluded, and should not be found.
        verifyBadUrl("/TestServlet40/ServletC");
        verifyBadUrl("/TestServlet40/ServletD");
        verifyBadUrl("/TestServlet40/SimpleTestServlet");
        
        if (serverStartType != ServerStartType.NONE) {
            LOG.info("Stopping server");
            stopServer("CWWKZ0014W", "SRVE0190E");
        }
        
        LOG.info("RETURN");
    }
    
    
    /**
     * Absolute ordering of only A, B, & C must hide all other JARs and the servlets therein.
     * @param startServerClean - if true, do a clean server start which will rebuild the anno cache from scratch.
     * @throws Exception
     */
    private void tryWebXmlWithAbsoluteOrder_A_B_C(ServerStartType serverStartType, String msg) throws Exception {

        logBlock(msg + " - Server start type -> " +  serverStartType);

        replaceWebXmlInExpandedApp("web-absolute-ordering-a-b-c.xml");
        
        // When the server restarts, it re-expands the .ear file into the expanded app.
        // So it will wipe out the change we just made.  To prevent that we need to 
        // replace the .ear file file with the contents of the expanded app directory tree.
        replaceApplicationFileFromExpandedApp();
        
        switch (serverStartType) {
        case CLEAN: 
            startServerClean();
        case DIRTY:
            startServerDirty();
        case NONE:
            waitForAppUpdateToBeNoticed();       
        }
        
        displayWebXml();

        // MyServlet is not specified in the absolute-ordering, but since it is in WEB-INF/classes, it should work.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");
        
        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};
        
        /////// Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!";
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ ABC ]";
     
        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);
        
        //////// Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!";
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ ABC ]";
        
        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);
        
        ////////Servlet C
        expectedResponses[0] = "Hello From Servlet C";
        expectedResponses[1] = "SCI C actually ran!";
        expectedResponses[2] = "Listener C actually ran!";
        expectedResponses[3] = "Listener order [ ABC ]";

        verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);
        
        ///////// Everything else is EXCLUDED by the absolute-ordering
        verifyBadUrl("/TestServlet40/ServletD");
        verifyBadUrl("/TestServlet40/SimpleTestServlet");

        // Expecting the additional Error message in the logs due to the missing servlets in these test case.
        if (serverStartType != ServerStartType.NONE) {
            LOG.info("Stopping server");
            stopServer("CWWKZ0014W", "SRVE0190E");
        }
        
        LOG.info("RETURN");
    }
    
    /**
     * Absolute ordering of only A & B but inclusion of "others" makes all of the JARs and the servlets therein
     * visible.  
     * 
     * @param startServerClean - if true, do a clean server start which will rebuild the anno cache from scratch.
     * @throws Exception
     */
    private void tryWebXmlWithAbsoluteOrder_A_B_Others(ServerStartType serverStartType, String msg) throws Exception {
        
        logBlock(msg + " - Server start type -> " +  serverStartType);
         
        // Replace web.xml, Order A then B and then others
        replaceWebXmlInExpandedApp("web-absolute-ordering-a-b-others.xml");
        
        // When the server restarts, it re-expands the .ear file into the expanded app.
        // So it will wipe out the change we just made.  To prevent that we need to 
        // replace the .ear file file with the contents of the expanded app directory tree.
        replaceApplicationFileFromExpandedApp();

        switch (serverStartType) {
        case CLEAN: 
            startServerClean();
        case DIRTY:
            startServerDirty();
        case NONE:
            waitForAppUpdateToBeNoticed();
        }
        
        displayWebXml();

        // MyServlet is not specified in the absolute-ordering, but since it is in WEB-INF/classes, it should work.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};
        
        /////// Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!";
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        
        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);
               
        //////// Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!";
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        
        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);
        
        ////////Servlet C
        expectedResponses[0] = "Hello From Servlet C";
        expectedResponses[1] = "SCI C actually ran!";
        expectedResponses[2] = "Listener C actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);
        
        ////////Servlet D
        expectedResponses[0] = "Hello From Servlet D";
        expectedResponses[1] = "SCI D actually ran!";
        expectedResponses[2] = "Listener D actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        verifyResponse("/TestServlet40/ServletD",  expectedResponses, unExpectedResponses);   
        
        ////////SimpleTestServlet
        expectedResponses[0] = "Hello World";
        expectedResponses[1] = "SCI says Hi";
        expectedResponses[2] = "";
        expectedResponses[3] = "";
        verifyResponse("/TestServlet40/SimpleTestServlet",  expectedResponses, unExpectedResponses);         

        // Expecting the additional Error message in the logs due to the missing servlets in these test case.
        if (serverStartType != ServerStartType.NONE) {
            LOG.info("Stopping server");
            stopServer("CWWKZ0014W", "SRVE0190E");
        }
        
        LOG.info("RETURN");
    }
    
    /**
     * Absolute ordering of only C, D, A & B with the inclusion of others makes all of the JARs and servlets therein visible.
     * The execution order of the Listeners is tested to be CDAB.
     * 
     * @param startServerClean - if true, do a clean server start which will rebuild the anno cache from scratch.
     * @throws Exception
     */
    private void tryWebXmlWithAbsoluteOrder_C_D_A_B_Others(ServerStartType serverStartType, String msg) throws Exception {
        
        logBlock(msg + " - Server start type -> " +  serverStartType);
         
        replaceWebXmlInExpandedApp("web-absolute-ordering-c-d-a-b-others.xml");
        
        // When the server restarts, it re-expands the .ear file into the expanded app.
        // So it will wipe out the change we just made.  To prevent that we need to 
        // replace the .ear file file with the contents of the expanded app directory tree.
        replaceApplicationFileFromExpandedApp();

        switch (serverStartType) {
        case CLEAN: 
            startServerClean();
        case DIRTY:
            startServerDirty();
        case NONE:
            waitForAppUpdateToBeNoticed();
        }
        
        displayWebXml();

        // MyServlet is not specified in the absolute-ordering, but since it is in WEB-INF/classes, it should work.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};
        
        /////// Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!";
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";
        
        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);
        
        //////// Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!";
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";
        
        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);
        
        ////////Servlet C
        expectedResponses[0] = "Hello From Servlet C";
        expectedResponses[1] = "SCI C actually ran!";
        expectedResponses[2] = "Listener C actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";
        verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);
        
        ////////Servlet D
        expectedResponses[0] = "Hello From Servlet D";
        expectedResponses[1] = "SCI D actually ran!";
        expectedResponses[2] = "Listener D actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";
        verifyResponse("/TestServlet40/ServletD",  expectedResponses, unExpectedResponses);   
        
        ////////SimpleTestServlet
        expectedResponses[0] = "Hello World";
        expectedResponses[1] = "SCI says Hi";
        expectedResponses[2] = "";
        expectedResponses[3] = "";
        verifyResponse("/TestServlet40/SimpleTestServlet",  expectedResponses, unExpectedResponses);  

        // Expecting the additional Error message in the logs due to the missing servlets in these test case.
        if (serverStartType != ServerStartType.NONE) {
            LOG.info("Stopping server");
            stopServer("CWWKZ0014W", "SRVE0190E");
        }
        
        LOG.info("RETURN");
    }
    
    /**
     * If a JAR is removed, it's servlets should no longer be available.
     * When a JAR is added to the application, it's servlets should become available.
     * After removing TestServletD.JAR, we do a modified version of the AB others ordering test, the difference
     * being that TestServletD should not be available. 
     * Then we restore TestServletD.JAR to the app, and run the un-modified  version of the AB others test.
     * 
     * @param startServerClean - if true, do a clean server start which will rebuild the anno cache from scratch.
     * @throws Exception
     */
    private void tryRemovingAndAddingAJar(ServerStartType serverStartType, String msg) throws Exception {

        logBlock(msg + " - Server start type -> " +  serverStartType);        
        
        // Start with an initial web.xml has absolute-ordering that specifies only JARs A & B

        renameJarFileInApplication("TestServlet40.war", "TestServletD.jar", "TestServletD.jar_backup");
        replaceWebXmlInExpandedApp("web-absolute-ordering-a-b-others.xml");
        
        // When the server restarts, it re-expands the .ear file into the expanded app.
        // So it will wipe out the change we just made.  To prevent that we need to 
        // replace the .ear file file with the contents of the expanded app directory tree.
        replaceApplicationFileFromExpandedApp();
        
        switch (serverStartType) {
        case CLEAN: 
            startServerClean();
        case DIRTY:
            startServerDirty();
        case NONE:
            waitForAppUpdateToBeNoticed();
        }
        
        displayWebXml();

        // MyServlet is not specified in the absolute-ordering, but since it is in WEB-INF/classes, it should work.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};
        
        /////// Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!";
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        
        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);
               
        //////// Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!";
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        
        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);
        
        ////////Servlet C
        expectedResponses[0] = "Hello From Servlet C";
        expectedResponses[1] = "SCI C actually ran!";
        expectedResponses[2] = "Listener C actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);
        
        ////////Servlet D
        verifyBadUrl("/TestServlet40/ServletD");   
        
        ////////SimpleTestServlet
        expectedResponses[0] = "Hello World";
        expectedResponses[1] = "SCI says Hi";
        expectedResponses[2] = "";
        expectedResponses[3] = "";
        verifyResponse("/TestServlet40/SimpleTestServlet",  expectedResponses, unExpectedResponses);         

        // Expecting the additional Error message in the logs due to the missing servlets in these test case.
        if (serverStartType != ServerStartType.NONE) {
            LOG.info("Stopping server");
            stopServer("CWWKZ0014W", "SRVE0190E");
        }
        
        // Put things back the way they were, and then rerun the test.  Servlet D should be available this time.
        logBlock("Restoring TestServletD.jar to the application"); 
        renameJarFileInApplication("TestServlet40.war", "TestServletD.jar_backup", "TestServletD.jar");
        tryWebXmlWithAbsoluteOrder_A_B_Others(serverStartType, "Test with web.xml with ordering A B others");
 
        LOG.info("RETURN");
    }
}


