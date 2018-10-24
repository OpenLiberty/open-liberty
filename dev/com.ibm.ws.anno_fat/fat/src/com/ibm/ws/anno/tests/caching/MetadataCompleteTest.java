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
package com.ibm.ws.anno.tests.caching;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.anno.tests.util.Ear;
import com.ibm.ws.anno.tests.util.FatHelper;
import com.ibm.ws.anno.tests.util.Jar;
import com.ibm.ws.anno.tests.util.War;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.topology.utils.FileUtils;

/**
   Test that the annotation cache is created, restart the server, and check that the cache is being used.
 */

public class MetadataCompleteTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(MetadataCompleteTest.class.getName());

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    // Not using ClassRule annotation.  So server does NOT start automatically.
    public static final SharedServer SHARED_SERVER = new SharedServer("annoFat_server", false);

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {

    	addAppToServerAppsDir();
    	
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  // Default Jandex settings, and expand the app.
        
        startServer();   // creates annotation cache.
        
        SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0014W");
        
        // Replace the web.xml with the metadata-complete version
        replaceWebXml("web.xml", "web-metadata-complete.xml");
        
        SHARED_SERVER.startIfNotStarted(false, false, false);   // This time do NOT do a clean start
        
        // So after creating the cache we changed the web.xml to be metadata-complete
        // The web.xml now contains all of the servlets and servlet mappings
        // The tests should show that all of the servlets are accessible.
        LOG.info("Complete");
        catWebXml();
    }

    public static void catWebXml() throws Exception {
        //String webInfDirName = SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/expanded/" + APP_NAME + "TestServlet40.war/WEB-INF";
        String webInfDirName = SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/expanded/TestServlet40.ear/TestServlet40.war/WEB-INF/";
        File webXmlFile = new File(webInfDirName + "web.xml");
        
        BufferedReader targetsReader = new BufferedReader(new FileReader(webXmlFile)); 
        try {

            String line; 
            LOG.info(webInfDirName + "web.xml");
            while ((line = targetsReader.readLine()) != null) {

                   LOG.info(line);
            } 

        } catch (IOException ioe) {
            LOG.info(ioe.getMessage());
            throw ioe;

        } finally {
            if (targetsReader != null) {
                targetsReader.close();
            } 
        }
    }
    
    @AfterClass
    public static void testCleanup() throws Exception {

    	LOG.info("testCleanUp : stop server");
    	SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0014W");

    }
    
    protected static void addAppToServerAppsDir() throws Exception {
        LOG.info("Add TestServlet40 to the server if not already present.");
        
        Ear ear = new Ear("TestServlet40.ear");
        War war = new War("TestServlet40.war");
        ear.addWar(war);
        
        Jar jar1 = new Jar("TestServlet40.jar");
        Jar jar2 = new Jar("TestServletA.jar");
        Jar jar3 = new Jar("TestServletB.jar");
        Jar jar4 = new Jar("TestServletC.jar");
        Jar jar5 = new Jar("TestServletD.jar");
        
        jar1.addPackageName("testservlet40.jar.servlets");
        jar2.addPackageName("testservleta.jar.servlets");
        jar3.addPackageName("testservletb.jar.servlets");
        jar4.addPackageName("testservletc.jar.servlets");
        jar5.addPackageName("testservletd.jar.servlets");
        
        war.addJar(jar1);
        war.addJar(jar2);
        war.addJar(jar3);
        war.addJar(jar4);
        war.addJar(jar5);
        
        war.addPackageName("testservlet40.war.servlets");

        try {
        	
           FatHelper.addEarToServerApps(SHARED_SERVER.getLibertyServer(),
            	                                      ear);

        } catch (Exception e) {
        	LOG.info("Caught exception from addEarToServerApps [" + e.getMessage() + "]");
        	throw e;
        }
    }
   
    protected static void startServer() throws Exception {
        SHARED_SERVER.startIfNotStarted();

        LOG.info("Wait for message to indicate app has started");

        SHARED_SERVER.getLibertyServer().addInstalledAppForValidation("TestServlet40");

        LOG.info("SApp has started, or so we believe");
    }
    
    /**
     * Copy the appropriate server.xml (server configuration)  for the test to be executed to
     * the root of the server directory.
     * 
     * @param sourceServerXml
     * @throws Exception
     */
    protected static void installServerXml(String sourceServerXml) throws Exception {

        final String serverRootDir = SHARED_SERVER.getLibertyServer().getServerRoot();
        final File serverXmlFile = new File(serverRootDir + "/server.xml");
        
        if (serverXmlFile.exists()) {
        	serverXmlFile.delete();
        }
        
        File serverConfigurationFile = new File(serverRootDir + "/serverConfigurations/" + sourceServerXml);
        FileUtils.copyFile(serverConfigurationFile, serverXmlFile); 
    }    
    
    /**
     *
     * @throws Exception
     */
    /*
    public void testChangingAppToMetadataComplete() throws Exception {
        
    	startServer();      
        
        // The app contains a servlet.  Verify we can access it.
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
        
        // Stop the server.
        SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0014W");
        
        // Replace the web.xml with the metadata-complete version
        replaceWebXml("web.xml", "web-metadata-complete.xml");
        SHARED_SERVER.startIfNotStarted(false, false, false); 
        //logAnnoTargetsFileModifiedTime();
        
        // The app contains a servlet.  Verify we can access it.
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");        
        
        // Stop the server here instead of in the cleanup method, because we need to allow the SRVE0190E
        // error message  to be in the logs. Meaning we were expecting the servlet access to fail.
        SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0014W", "SRVE0190E");
        
    	// Replace web.xml with the Not-metadata-complete version
    	replaceWebXml("web.xml", "web-NOT-metadata-complete.xml");
    	
    	LOG.info("Start server after restoring anno.targets cache" );
    	SHARED_SERVER.startIfNotStarted(false, false, false);      
    }*/
        
    public static void replaceWebXml(String oldWebXmlName, String newWebXmlName) throws Exception {
        LOG.info("In modifyApp()");

        //String webInfDirName = SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/expanded/" + APP_NAME + "TestServlet40.war/WEB-INF";
        String webInfDirName = SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/expanded/TestServlet40.ear/TestServlet40.war/WEB-INF/";
        File oldWebXml = new File(webInfDirName + oldWebXmlName);
        File newWebXml = new File(webInfDirName + newWebXmlName);
          
    	// Copy new web.xml over old web.xml
        LOG.info("webInfDirName=[ " + webInfDirName + " ]");
    	LOG.info("Replacing file [ " + oldWebXml.getName() + " ] with \n  [ " + newWebXml.getName() + " ]");
    	FileUtils.copyFile(newWebXml, oldWebXml);   
    }

    /**
     * Request a simple servlet.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleServlet() throws Exception {
    	verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
    }

    @Test
    public void testServletA() throws Exception {
    	verifyResponse("/TestServlet40/ServletA", "Hello From Servlet A");
    }

    @Test
    public void testServletB() throws Exception {
    	verifyResponse("/TestServlet40/ServletB", "Hello From Servlet B");
    }

    @Test
    public void testServletC() throws Exception {
    	verifyResponse("/TestServlet40/ServletC", "Hello From Servlet C");
    }

    @Test
    public void testServletD() throws Exception {
    	this.verifyResponse("/TestServlet40/ServletD", "Hello From Servlet D");
    }
}
