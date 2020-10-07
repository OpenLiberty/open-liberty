/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.tests.anno.caching;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.utils.FileUtils;

/**
 * These tests to verify that updates to the absolute-ordering element in the web.xml
 * are noticed after server restarts.
 */
public class FragmentOrderTest extends AnnoCachingTest {
    private static final Logger LOG = Logger.getLogger(FragmentOrderTest.class.getName());

    enum ServerStartType { 
        NONE, CLEAN, DIRTY; 
    }
    
    public static boolean UPDATE_APP = true;
    public static boolean DONT_UPDATE_APP = false;
    public static String tempExpandedEarPath;      // Path of an expanded copy of the EAR
    public static String tempWarPath;              // Path to the WAR file inside the expanded copy of the EAR
    public static String backupEarPath;            // Path to a backup copy of the EAR

    /**
     * Optionally updates the app, then depending on the start type, it either starts the app
     * or waits for start already in progress to finish.
     * 
     * @param altWebXml
     * @param startType
     * @throws Exception
     */
    private void handleStart(String altWebXml, ServerStartType startType, boolean updateApp) throws Exception {
    	
    	if (!updateApp && (altWebXml != null) ) {
    		LOG.info("handleStart: ");
    		throw new IllegalStateException("Don't need to update app, but altWebXml is supplied - Probably not what was intended: [" + altWebXml + "]");
    		
    	} else if (updateApp && (altWebXml == null) ) {
    		LOG.info("handleStart: ");
    		throw new IllegalStateException("Want to update app, but altWebXml not supplied.");		
    	} 
    	
        if (updateApp) {
        	displayWebXmlFromExpandedApp(altWebXml);
    		copyEntryWithinZip("/WEB-INF/" + altWebXml, "/WEB-INF/web.xml", tempWarPath);
    		replaceEarFromExpandedEar(tempExpandedEarPath, getApplicationPath());
    	}

        if ( startType == ServerStartType.CLEAN ) {
        	startServerScrub();
        } else if ( startType == ServerStartType.DIRTY ) {
        	startServer();
        } else if ( startType == ServerStartType.NONE ) {
        	waitForAppUpdate();
        } else {
        	throw new IllegalStateException("Unknown server start type: " + startType);
        }
    }

    private void handleStop(ServerStartType startType) throws Exception {
        if ( startType != ServerStartType.NONE ) {
            stopServer("CWWKZ0014W", "SRVE0190E");
        }
    }

    // Setup for all absolute ordering tests ...

    /**
     * Setup to run absolute ordering tests.
     *
     * The test uses the shared server "annoFat_Server", as
     * defined by "CachingTest", and uses FAT test application "TestServlet40.ear".
     *
     * Jandex use is not enabled.  Application auto-expand is enabled.
     *
     * Setup removes any prior copy of the application, including expanded
     * application files, then copies in a fresh copy of the application.
     *
     * A single startup and shutdown sequence is performed.  This causes application
     * files to be expanded, and causes the annotation cache to be written.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("setUp ENTRY");

        setSharedServer();
        installJvmOptions("JvmOptions_Enabled.txt");

        setEarName("TestServlet40.ear");
        deleteApplication();
        deleteExpandedApplication();
        addToAppsDir( createApp() );

        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  // Default: Do NOT use Jandex.

        long lStartTime = System.nanoTime();
        startServerScrub();
        long lEndTime = System.nanoTime();        
        long elapsed = lEndTime - lStartTime;
        LOG.info("Server started in milliseconds: " + elapsed / 1000000);        

        stopServer();
        
        // Create a working directory for updating app for each test.  Flow will be to update
        // the temp expanded EAR and then zip to replace the installed EAR file.
        // Why not just update in .../apps/expanded folder? Changes to the expanded app might
        // be overwritten, depending on the autoExpand setting, when the application restarts.
        // So we just replace the ear, and don't worry about whether it auto expands.
		tempExpandedEarPath = getTempDir() + "/expanded/TestServlet40.ear";
        mkDir(tempExpandedEarPath, DELETE_IF_EXISTS);
        
    	tempWarPath = tempExpandedEarPath + "/TestServlet40.war";
        
        backupEarPath = getTempDir() + "/expanded/TestServlet40.original.ear";
        makeBackupCopyOfEar(getApplicationPath(), backupEarPath);		

        LOG.info("setUp EXIT");
    }
    
    /**
     * Replace the application EAR from the backup copy before each test
     * @throws Exception
     */
    @Before
    public void setupBeforeEach() throws Exception {    
    	expandEarToTemp(backupEarPath, tempExpandedEarPath, DELETE_IF_EXISTS);
    }
    	
    @AfterClass
    public static void cleanUp() throws Exception {
        LOG.info("cleanUp ENTRY");
        stopServer();
        LOG.info("cleanUp EXIT");
    }

    //

    /**
     * Test various changes to the web.xml that affect absolute ordering.  Each test starts
     * and then stops the server.  There are six (6) different ordering combinations which
     * are tested, making this a fairly expensive test due to the server restarts.
     */
    @Test
    public void fragmentOrder_testAbsoluteOrdering() throws Exception {
        // Run the variations with a server start and stop for each
        // variation.  Do not clear the annotation cache between variations
        // (but do clear the cache for the first variation).

        tryStandardVariations(ServerStartType.DIRTY);
    }

    /**
     * A standard sequence of order variations.
     *
     * The first variation does a clean startup.  Second and subsequent variations
     * use the start mode as specified.
     *
     * @param startType The start mode for the second and subsequent variations.
     *
     * @throws Exception Thrown in case of an unexpected test failure.
     */
    private void tryStandardVariations(ServerStartType startType) throws Exception {
        // Base sequence: Set the order to "A B", then change the order to "A B C",
        // then back to "A B".

        // 'startType' will be either 'DIRTY' (which does starts and stops)
        // or 'none', which does an initial start and a final stop.
        //
        // In either case, 'startType' correctly selects the stop action for
        // the first variation.

        // Start with a web.xml with absolute-ordering containing Fragments A and B
        // Start the server cleanly and verify we get the expected response 
        // (or expected lack of a response) from the servlets.
        tryWith_A_B(ServerStartType.CLEAN, startType, "Initial test: Fragments A and B");
        
        // Now replace web.xml, adding "JAR C" to the absolute-ordering element, and start the server
        // dirty to see if the anno cache gets updated properly.
        tryWith_A_B_C(startType, "Second test: Add fragment C");
          
        // Now go back to using the previous web.xml.  The effect is that "Fragment C" is removed from the
        // absolute order.  So it's servlet should no longer respond.
        tryWith_A_B(startType, "Third test: Remove fragment C");

        // "Others" sequence: "A B others" then "C, D, A, B, Others".

        // Add an others element.  Now all of the servlets in the fragments should respond.
        tryWith_A_B_Others(startType, "Fourth test: add others");

        // Move C to the front.  All of the servlets in the fragments should respond.
        tryWith_C_D_A_B_Others(startType, "Fifth test: Fragments C, D, A, B, and others");

        // "JAR" sequence: Remove and add a fragment JAR, while "others" is present in the ordering.

        tryRemovingAndAddingAJar(startType, "Sixth test: Remove and add TestServletD.jar");

        // When the start type is set to NONE, none of the variations stops the server.
        // An explicit stop must be added at the end.

        if ( startType == ServerStartType.NONE ) {
            stopServer("CWWKZ0014W", "SRVE0190E");
        }
    }

    /**
     * Test a clean startup with cache validation enabled.
     */
    @Test
    public void fragmentOrder_testCacheValidate() throws Exception {
        installJvmOptions("JvmOptions_Enabled_Validate.txt");
        tryWith_A_B(ServerStartType.CLEAN, "Initial Test with web.xml containing Fragments A & B");       
    }
  
    @Test
    public void fragmentOrder_testScanThreads() throws Exception {
        installJvmOptions("JvmOptions_Enabled_ScanMulti.txt");
        tryWith_A_B(ServerStartType.CLEAN, "Test scan threads set to many");

        installJvmOptions("JvmOptions_Enabled_ScanUnlimited.txt");
        tryWith_A_B(ServerStartType.CLEAN, "Test scan threads set to unlimited");
    }

    @Test
    public void fragmentOrder_testWriteThreads() throws Exception {
        installJvmOptions("JvmOptions_Enabled_WriteMulti.txt");
        tryWith_A_B(ServerStartType.CLEAN, "Test write threads set to many");

        installJvmOptions("JvmOptions_Enabled_WriteUnlimited.txt");
        tryWith_A_B(ServerStartType.CLEAN, "Test set write threads to unlimited");
    }
    
    /**
     * Test various changes to the web.xml that affect absolute ordering.
     * Rely on Liberty to detect the change in the app WITHOUT restarting
     * the server.
     */
    @Test
    public void fragmentOrder_testAbsoluteOrderingNoRestart() throws Exception {
        // Run the variations with only a single server start and stop for
        // all of the variations.  Do clear the annotation class for the
        // first start.

        tryStandardVariations(ServerStartType.NONE);
    }

    // Ordering variation helpers ...

    /**
     * Perform the "A" "B" variation with the same start and stop modes.
     */
    private void tryWith_A_B(ServerStartType serverStartType, String msg) throws Exception {
        tryWith_A_B(serverStartType, serverStartType, msg);
    }

    /**
     * Test with only "A" and "B" in the absolute ordering.  Other fragments are
     * excluded and should not be visibile to the web container.  The "classes"
     * folder is not excluded.
     *
     * The ordering of listeners must be "classes", "A", then "B".
     *
     * A start mode and a stop mode must be provided: When this test variation occurs first
     * in a sequence of variations, the variation will have a CLEAN start mode and a NONE
     * stop mode.
     */
    private void tryWith_A_B(ServerStartType serverStartType, ServerStartType stopType, String msg) throws Exception {
        logBlock(msg + " - Server start type -> " +  serverStartType);        

        handleStart("web-absolute-ordering-a-b.xml", serverStartType, UPDATE_APP );

        // MyServlet should work because WEB-INF/classes is never excluded.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};
        
        // Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!"; 
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ AB ]";

        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);

        // Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!"; 
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ AB ]";

        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);

        // All others are excluded, and should not be found.
        verifyBadUrl("/TestServlet40/ServletC");
        verifyBadUrl("/TestServlet40/ServletD");
        verifyBadUrl("/TestServlet40/SimpleTestServlet");

        handleStop(stopType);

        LOG.info("RETURN");
    }
    
    /**
     * Test with only "A" and "B" and "C" in the absolute ordering.  Other fragments
     * are excluded and should not be visibile to the web container.  The "classes"
     * folder is not excluded.
     *
     * The ordering of listeners must be "classes", "A", then "B", then "C".
     */
    private void tryWith_A_B_C(ServerStartType serverStartType, String msg) throws Exception {
        logBlock(msg + " - Server start type -> " +  serverStartType);

        handleStart("web-absolute-ordering-a-b-c.xml", serverStartType, UPDATE_APP);

        // MyServlet should work because WEB-INF/classes is never excluded.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};

        // Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!";
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ ABC ]";

        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);

        // Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!";
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ ABC ]";

        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);

        // Servlet C
        expectedResponses[0] = "Hello From Servlet C";
        expectedResponses[1] = "SCI C actually ran!";
        expectedResponses[2] = "Listener C actually ran!";
        expectedResponses[3] = "Listener order [ ABC ]";

        verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);

        // Everything else is EXCLUDED by the absolute-ordering
        verifyBadUrl("/TestServlet40/ServletD");
        verifyBadUrl("/TestServlet40/SimpleTestServlet");

        handleStop(serverStartType);

        LOG.info("RETURN");
    }
    
    /**
     * Test with "A" and "B" and "others" in the absolute ordering.  No fragment is
     * excluded.
     *
     * The ordering of listeners must be "classes", "A", then "B", then others, in
     * any order.
     */
    private void tryWith_A_B_Others(ServerStartType serverStartType, String msg) throws Exception {
        logBlock(msg + " - Server start type -> " +  serverStartType);

        handleStart("web-absolute-ordering-a-b-others.xml", serverStartType, UPDATE_APP);

        // MyServlet should work because WEB-INF/classes is never excluded.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};

        // Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!";
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.

        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);

        // Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!";
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.

        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);

        // Servlet C
        expectedResponses[0] = "Hello From Servlet C";
        expectedResponses[1] = "SCI C actually ran!";
        expectedResponses[2] = "Listener C actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);

        // Servlet D
        expectedResponses[0] = "Hello From Servlet D";
        expectedResponses[1] = "SCI D actually ran!";
        expectedResponses[2] = "Listener D actually ran!";
        expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
        verifyResponse("/TestServlet40/ServletD",  expectedResponses, unExpectedResponses);   

        // SimpleTestServlet
        expectedResponses[0] = "Hello World";
        expectedResponses[1] = "SCI says Hi";
        expectedResponses[2] = "";
        expectedResponses[3] = "";
        verifyResponse("/TestServlet40/SimpleTestServlet",  expectedResponses, unExpectedResponses);         

        handleStop(serverStartType);

        LOG.info("RETURN");
    }

    /**
     * Test with "C", "D", "A", "B", and "others" in the absolute ordering.  No fragment is
     * excluded.
     *
     * The ordering of listeners must be "classes", "C", "D", "A", "B", then others, in
     * any order.
     */
    private void tryWith_C_D_A_B_Others(ServerStartType serverStartType, String msg) throws Exception {
        logBlock(msg + " - Server start type -> " +  serverStartType);

        handleStart("web-absolute-ordering-c-d-a-b-others.xml", serverStartType, UPDATE_APP );

        // MyServlet should work because WEB-INF/classes is never excluded.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        String[] expectedResponses = new String[4];
        String[] unExpectedResponses = {};

        // Servlet A
        expectedResponses[0] = "Hello From Servlet A"; 
        expectedResponses[1] = "SCI A actually ran!";
        expectedResponses[2] = "Listener A actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";

        verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);

        // Servlet B
        expectedResponses[0] = "Hello From Servlet B"; 
        expectedResponses[1] = "SCI B actually ran!";
        expectedResponses[2] = "Listener B actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";

        verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);

        // Servlet C
        expectedResponses[0] = "Hello From Servlet C";
        expectedResponses[1] = "SCI C actually ran!";
        expectedResponses[2] = "Listener C actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";

        verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);

        // Servlet D
        expectedResponses[0] = "Hello From Servlet D";
        expectedResponses[1] = "SCI D actually ran!";
        expectedResponses[2] = "Listener D actually ran!";
        expectedResponses[3] = "Listener order [ CDAB ]";

        verifyResponse("/TestServlet40/ServletD",  expectedResponses, unExpectedResponses);   

        // SimpleTestServlet
        expectedResponses[0] = "Hello World";
        expectedResponses[1] = "SCI says Hi";
        expectedResponses[2] = "";
        expectedResponses[3] = "";

        verifyResponse("/TestServlet40/SimpleTestServlet",  expectedResponses, unExpectedResponses);  

        handleStop(serverStartType);

        LOG.info("RETURN");
    }
    
    /**
     * Test with "A", "B", and "others".  Start by removing the jar for "D", which
     * would be handled under "others".  Then re-add the jar for "D".  The content from
     * servlet "D" should disappear when its jar is removed, then re-appear when its jar is
     * re-added.
     */
    private void tryRemovingAndAddingAJar(ServerStartType serverStartType, String msg) throws Exception {
    	logBlock(msg + " - Server start type -> " +  serverStartType);        
    	String altWebXml = "web-absolute-ordering-a-b-others.xml";
    	LOG.info("Remove TestServletD.jar from the application");
    	displayWebXmlFromExpandedApp(altWebXml);

    	copyEntryWithinZip("/WEB-INF/" + altWebXml, "/WEB-INF/web.xml", tempWarPath);
    	renameEntryInZip("/WEB-INF/lib/TestServletD.jar", "/WEB-INF/lib/TestServletD.jar_backup", tempWarPath);
    	replaceEarFromExpandedEar(tempExpandedEarPath, getApplicationPath());

    	handleStart(null, serverStartType, DONT_UPDATE_APP );

    	// We can't use the standard "A", "B", "others" variation, since
    	// that expects "D", which is not present.

    	// MyServlet should work because WEB-INF/classes is never excluded.
    	verifyResponse("/TestServlet40/MyServlet", "Hello World");

    	String[] expectedResponses = new String[4];
    	String[] unExpectedResponses = {};

    	// Servlet A
    	expectedResponses[0] = "Hello From Servlet A"; 
    	expectedResponses[1] = "SCI A actually ran!";
    	expectedResponses[2] = "Listener A actually ran!";
    	expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.

    	verifyResponse("/TestServlet40/ServletA", expectedResponses, unExpectedResponses);

    	// Servlet B
    	expectedResponses[0] = "Hello From Servlet B"; 
    	expectedResponses[1] = "SCI B actually ran!";
    	expectedResponses[2] = "Listener B actually ran!";
    	expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.

    	verifyResponse("/TestServlet40/ServletB",  expectedResponses, unExpectedResponses);

    	// Servlet C
    	expectedResponses[0] = "Hello From Servlet C";
    	expectedResponses[1] = "SCI C actually ran!";
    	expectedResponses[2] = "Listener C actually ran!";
    	expectedResponses[3] = "Listener order [ AB";  // C&D are included by "others".  Order is undefined for those.
    	verifyResponse("/TestServlet40/ServletC",  expectedResponses, unExpectedResponses);

    	// Servlet D
    	verifyBadUrl("/TestServlet40/ServletD");   

    	// SimpleTestServlet
    	expectedResponses[0] = "Hello World";
    	expectedResponses[1] = "SCI says Hi";
    	expectedResponses[2] = "";
    	expectedResponses[3] = "";
    	verifyResponse("/TestServlet40/SimpleTestServlet",  expectedResponses, unExpectedResponses);         

    	handleStop(serverStartType);


    	LOG.info("Add TestServletD.jar back into the application");

    	// This will just restore the TestServletD.jar in the temp expanded EAR.   Then we call the
    	// tryWith_A_B_Others which will zip the temp expanded EAR, replacing the installed EAR.
    	renameEntryInZip("/WEB-INF/lib/TestServletD.jar_backup", "/WEB-INF/lib/TestServletD.jar", tempWarPath);

    	// We should be back to the standard "A", "B", "others" variation.

    	tryWith_A_B_Others(serverStartType, "Test with web.xml with ordering A B others");


    	LOG.info("RETURN");
    }
}
