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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.anno.tests.util.Ear;
import com.ibm.ws.anno.tests.util.FatHelper;
import com.ibm.ws.anno.tests.util.Jar;
import com.ibm.ws.anno.tests.util.Utils;
import com.ibm.ws.anno.tests.util.War;
import com.ibm.ws.anno.tests.util.Utils.*;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.topology.utils.FileUtils;

/**
 * Test that the annotation cache is created, restart the server, and check that the cache is being used.
 */
public class MetadataCompleteMissingServletsTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(MetadataCompleteMissingServletsTest.class.getName());

    //

    private static final String EAR_FILE_NAME = "TestServlet40.ear";

    // Not using ClassRule annotation.  So server does NOT start automatically.
    public static final SharedServer SHARED_SERVER = new SharedServer("annoFat_server", false);

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    protected static String getServerRoot() throws Exception {
        return SHARED_SERVER.getLibertyServer().getServerRoot();
    }

    protected static Ear createApp() throws Exception {
        Jar jar1 = new Jar("TestServlet40.jar");
        jar1.addPackageName("testservlet40.jar.servlets");

        Jar jar2 = new Jar("TestServletA.jar");
        jar2.addPackageName("testservleta.jar.servlets");

        Jar jar3 = new Jar("TestServletB.jar");
        jar3.addPackageName("testservletb.jar.servlets");

        Jar jar4 = new Jar("TestServletC.jar");
        jar4.addPackageName("testservletc.jar.servlets");

        Jar jar5 = new Jar("TestServletD.jar");
        jar5.addPackageName("testservletd.jar.servlets");
        
        War war = new War("TestServlet40.war");
        war.addPackageName("testservlet40.war.servlets");
        war.addJar(jar1);
        war.addJar(jar2);
        war.addJar(jar3);
        war.addJar(jar4);
        war.addJar(jar5);

        Ear ear = new Ear(EAR_FILE_NAME);
        ear.addWar(war);

        return ear;
    }

    protected static void addAppToServerAppsDir(Ear ear) throws Exception {
        LOG.info("Add TestServlet40 to the server if not already present.");

        try {
           FatHelper.addEarToServerApps(SHARED_SERVER.getLibertyServer(), ear);

        } catch (Exception e) {
            LOG.info("Caught exception from addEarToServerApps [" + e.getMessage() + "]");
            throw e;
        }
    }

    // LOG.info("Wait for message to indicate app has started");
    // SHARED_SERVER.getLibertyServer().addInstalledAppForValidation("TestServlet40");
    // LOG.info("App has started, or so we believe");

    protected static void startServerClean() throws Exception {
        LOG.info("startServerClean : starting server");
        SHARED_SERVER.startIfNotStarted();
        LOG.info("startServerClean : started server");
    }

    protected static void startServerDirty() throws Exception {
        LOG.info("startServerDirty : starting server");
        SHARED_SERVER.startIfNotStarted(false, false, false);  
        LOG.info("startServerDirty : started server");
    }

    protected static void stopServer() throws Exception {    
        stopServer("CWWKZ0014W");
    }

    protected static void stopServer(String... expectedMessages) throws Exception {
        LOG.info("stopServer : stopping server");
        for ( String message : expectedMessages ) {
            LOG.info("stopServer : expecting [ " + message + " ]");
        }

        SHARED_SERVER.getLibertyServer().stopServer(expectedMessages);

        LOG.info("stopServer : stopped server");
    }

    // This must be done after starting the server.  The copy of web.xml which is
    // displayed is from the expanded applications folder, which is populated when
    // the server starts.

    public static void catWebXml() throws Exception {
        String webInfDirName = getServerRoot() + "/apps/expanded/" + EAR_FILE_NAME + "/TestServlet40.war/WEB-INF/";
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

    // This must be done after each clean server startup.  The copy of web.xml
    // which is  replaced is in the expanded applications folder, which is populated
    // during clean server starts.

    public static void replaceWebXml(String newWebXmlName) throws Exception {
        LOG.info("In replaceWebXml");

        String webInfDirName = getServerRoot() + "/apps/expanded/" + EAR_FILE_NAME + "/TestServlet40.war/WEB-INF/";
        File webXml = new File(webInfDirName + "web.xml");
        File newWebXml = new File(webInfDirName + newWebXmlName);
          
        // Copy new web.xml over old web.xml
        LOG.info("webInfDirName=[ " + webInfDirName + " ]");
        LOG.info("Replacing file [ " + webXml.getName() + " ] with [ " + newWebXml.getName() + " ]");
        FileUtils.copyFile(newWebXml, webXml);   
    }

    //

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("setUp");

        // Copy the test application to the server.
        addAppToServerAppsDir( createApp() );

        // Add the server.xml to the server.  This one used default Jandex settings and expands the app.
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  

        LOG.info("setUp Complete");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        LOG.info("cleanUp");
        stopServer();
        LOG.info("cleanUp complete");
    }

    //
    
    /**
     * Copy a server.xml from the server configuration to the shared server.
     */
    protected static void installServerXml(String sourceServerXml) throws Exception {
        String serverRootDir = getServerRoot();
        File serverXmlFile = new File(serverRootDir + "/server.xml");

        if (serverXmlFile.exists()) {
            serverXmlFile.delete();
        }

        File serverConfigurationFile = new File(serverRootDir + "/serverConfigurations/" + sourceServerXml);
        FileUtils.copyFile(serverConfigurationFile, serverXmlFile); 
    }    

    //

    @Test
    public void testUpdateToMetadataComplete() throws Exception {
        // Do a clean start and stop of the server.  This expands the app and generates annotation cache.

        // The initial web.xml *is not* metadata complete.  'SimpleTestServlet', from web.xml
        // should be available, as well as the four annotation defined servlets:

        startServerClean();

        catWebXml();

        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        verifyResponse("/TestServlet40/ServletA", "Hello From Servlet A");
        verifyResponse("/TestServlet40/ServletB", "Hello From Servlet B");
        verifyResponse("/TestServlet40/ServletC", "Hello From Servlet C");
        verifyResponse("/TestServlet40/ServletD", "Hello From Servlet D");

        stopServer();
        
        // Replace the web.xml with the metadata-complete version that is missing some servlets.       
        // The modification is to the apps/expanded/<APP_NAME> directory

        // The replacement web.xml *is* metadata complete.  'SimpleTestServlet', from web.xml
        // should still be available.  The four annotation defined servlets *should not* be
        // available.

        Utils.replaceWebXml(SHARED_SERVER, EAR_FILE_NAME, "web-metadata-complete.xml");

        // Now we need to replace the EAR file with the contents of the expanded app.
        // We replace the EAR file because the EAR gets expanded each time the server starts.
        // So it does no good to just update the expanded directory, if you are restarting the server.

        Utils.backupApplicationFile(SHARED_SERVER, EAR_FILE_NAME);
        Utils.deleteApplicationFile(SHARED_SERVER, EAR_FILE_NAME);
        Utils.replaceApplicationFileFromExpandedApp(SHARED_SERVER, EAR_FILE_NAME);

        // Start the server, but don't do a clean start.  A clean start would wipe out the cache.
        // We are trying to see if the existing cache gets updated to reflect the change in web.xml.

        // After creating the cache we changed the web.xml to be metadata-complete.
        // However, the web.xml is missing the A, B, C, D servlets.
        // The tests should show that those servlets are NOT accessible and
        // the SimpleTestServlet is accessible because it is in the web.xml.

        startServerDirty();

        catWebXml();

        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        verifyBadUrl("/TestServlet40/ServletA");
        verifyBadUrl("/TestServlet40/ServletB");
        verifyBadUrl("/TestServlet40/ServletC");
        verifyBadUrl("/TestServlet40/ServletD");

        // Expecting the additional Error message in the logs due to the missing servlets in these test case.
        stopServer("CWWKZ0014W", "SRVE0190E");
    }
}
