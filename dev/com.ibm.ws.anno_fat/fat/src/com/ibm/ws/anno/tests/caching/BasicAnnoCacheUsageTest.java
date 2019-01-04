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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
public class BasicAnnoCacheUsageTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(BasicAnnoCacheUsageTest.class.getName());

    // Test API extensions ...

    // Not using ClassRule annotation.  So server does NOT start automatically.
    public static SharedServer SHARED_SERVER = new SharedServer("annoFat_server", false); // false: don't wait for security;

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    // 'addInstalledAppForValidation' adds the app, and, if the server
    // is started, verifies that the app was started.
    //
    // This is not the required behavior: These usage tests need to add the
    // application once, and do a verification on each startup.
    //
    // A (small) refactoring of LibertyServer is needed to enable the required
    // behavior.
    //
    // LOG.info("Wait for message to indicate app has started");
    // SHARED_SERVER.getLibertyServer().addInstalledAppForValidation("TestServlet40");
    // LOG.info("App has started, or so we believe");

    protected static void startServer() throws Exception {
        
        LOG.info("startServer : starting server");
        SHARED_SERVER.startIfNotStarted(false,   // Don't preClean 
                                        true,    // Do clean start
                                        false);  // Don't validate apps have started
        

        LOG.info("startServer : started server");
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

    protected static String serverRoot;

    protected static String getServerRoot() throws Exception {
        if ( serverRoot == null ) {
            serverRoot = SHARED_SERVER.getLibertyServer().getServerRoot();
            LOG.info("getServerRoot : " + serverRoot);
        }
        return serverRoot;
    }

    /**
     * Copy a "server.xml" from the test server configuration folder to the server directory.
     * @param sourceServerXml  - File to copy to server.xml in the server directory.
     * @throws Exception
     */
    protected static void installServerXml(String sourceServerXml) throws Exception {
        LOG.info("installServerXml : " + sourceServerXml);

        String serverRootDir = getServerRoot();
        File serverXmlFile = new File(serverRootDir + "/server.xml");

        if (serverXmlFile.exists()) {
            serverXmlFile.delete();
        }

        File serverConfigurationFile = new File(serverRootDir + "/serverConfigurations/" + sourceServerXml);
        FileUtils.copyFile(serverConfigurationFile, serverXmlFile); 
    }    
    
    /**
     * Copy a "jvm.options" from the test server configuration folder to the server directory.
     * @param sourceJvmOptions  - File to copy to jvm.options in the server directory.
     * @throws Exception
     */
    protected static void installJvmOptions(String sourceJvmOptions) throws Exception {
        LOG.info("installJvmOptions : " + sourceJvmOptions);

        String serverRootDir = getServerRoot();
        File jvmOptionsFile = new File(serverRootDir + "/jvm.options");

        if (jvmOptionsFile.exists()) {
            jvmOptionsFile.delete();
        }

        // if sourceJvmOptions is null, then the assumption is that we just want to delete any existing jvm.options
        if (sourceJvmOptions != null) {
            File serverJvmOptionsFile = new File(serverRootDir + "/serverConfigurations/" + sourceJvmOptions);
            FileUtils.copyFile(serverJvmOptionsFile, jvmOptionsFile); 
        }
    }

    // Test API ...

    @BeforeClass
    public static void setUp() throws Exception {
        addAppToServerAppsDir();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        stopServer();
    }

    protected static void addAppToServerAppsDir() throws Exception {
        LOG.info("Add TestServlet40 to the server");

        Ear ear = new Ear("TestServlet40.ear");

        War war = new War("TestServlet40.war");
        ear.addWar(war);
        
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
        
        war.addJar(jar1);
        war.addJar(jar2);
        war.addJar(jar3);
        war.addJar(jar4);
        war.addJar(jar5);

        war.addPackageName("testservlet40.war.servlets");

        try {
           FatHelper.addEarToServerApps(SHARED_SERVER.getLibertyServer(), ear);

        } catch (Exception e) {
            LOG.info("Caught exception from addEarToServerApps [" + e.getMessage() + "]");
            throw e;
        }
    }

    //

    public boolean isSetCacheDir;
    public File cacheDir;

    public File getAnnoCacheRoot() {
        if ( !isSetCacheDir ) {
            String osgiWorkAreaRoot = SHARED_SERVER.getLibertyServer().getOsgiWorkAreaRoot();
            LOG.info("getAnnoCacheDirectory: OSGI workarea[" + osgiWorkAreaRoot + "]");

            File annoCacheDir = findDirectory(new File(osgiWorkAreaRoot), AnnoCacheLocations.CACHE_NAME);
            LOG.info("getAnnoCacheDirectory: annoCache Dir[" + annoCacheDir + "]");

            isSetCacheDir = true;
            cacheDir = annoCacheDir;
        }
        return cacheDir;
    }
    
    public File getAnnoCacheAppRoot() {
        File annoCacheDir = getAnnoCacheRoot();
        if ( annoCacheDir == null ) {
            LOG.info("getAppDirectory:       Failed; Cache root 'anno' not found");
            return null;
        }

        File appDir = findDirectory(annoCacheDir, AnnoCacheLocations.APP_PREFIX + "TestServlet40");
        LOG.info("getAppDirectory:       App Directory[" + appDir + "]");
        return appDir;
    }    
    
    public File findFile(File file, String searchFileName) {
        if (file.isDirectory()) {
            File[] arr = file.listFiles();
            for (File f : arr) {
                File found = findFile(f, searchFileName);
                if (found != null)
                    return found;
            }
        } else {
            if (file.getName().equals(searchFileName) ) {
                return file;
            }
        }
        return null;
    }
    
    public File findDirectory(File file, String searchFileName) {
        if (file.isDirectory()) {
            if (file.getName().equals(searchFileName) ) {
                return file;
            }
            File[] arr = file.listFiles();
            for (File f : arr) {
                File found = findDirectory(f, searchFileName);
                if (found != null)
                    return found;
            }
        } else {
            // File ignored.  We are looking for a directory.
        }
        
        return null;
    }

    //

    @Test
    public void testAnnotationCacheCreatedAndIsActuallyUsed() throws Exception {
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  // Default Jandex settings.  NOT using Jandex.
        installJvmOptions(null);  // No server.env -- default properties

        startServer();

        // Looking for a dir something like this: .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/anno
        verifyCacheExists();

        // The app contains a servlet.  Verify we can access it.
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");

        stopServer();

        // Remove the servlet from the cache, and restart the server.
        //
        // Note that the servlet is still present in the server:
        // This test directly changes the cache data to give the appearance
        // that the servlet is no longer present.

        String annoTargetsFileName = getAnnoCacheAppRoot().getCanonicalPath() + "/M_%2FTestServlet40.war/C_seed/targets";
        File annoTargetsFile = new File(annoTargetsFileName);
        String backupFileName =  annoTargetsFileName + ".backup";
        File backupFile = new File(backupFileName);
        modifyCache_AnnoTargets(annoTargetsFile, backupFile);

        SHARED_SERVER.startIfNotStarted(false, false, false);

        logAnnoTargetsFileModifiedTime();

        // If the server is using the annotation cache (as it is supposed to), then it should
        // not be able to access the url, because we removed it from the cache.  The server
        // shouldn't be aware of the servlet.
        verifyBadUrl("/TestServlet40/SimpleTestServlet");

        // Stop the server, including an extra check for a bad URL error message.
        stopServer("CWWKZ0014W", "SRVE0190E");

        LOG.info("Restoring file [ " + annoTargetsFileName + " ] from \n  [ " + backupFileName + " ]");
        FileUtils.copyFile(backupFile, annoTargetsFile);

        LOG.info("Start server after restoring targets cache" );
        startServer();

        // The servlet should again be available.
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
        stopServer();
        
        verifyReadOnlyCacheIsNotWrittenTo();
        
    }
    
    private void verifyCacheExists() {
        File annoCacheDir = getAnnoCacheRoot();
        assertNotNull("Can't find root 'anno' directory", annoCacheDir);
        
        // Now find the application directory below the anno cache root
        // something like:  .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/anno/A_TestServlet40
        File annoCacheRootDir = getAnnoCacheAppRoot();
        assertNotNull("Can't find application directory 'A_TestServlet40'", annoCacheRootDir);

        // Test if 'classes' exists.  Assume cache created successfully if exists.
        File classRefsFile = findFile(annoCacheRootDir, "classes");
        assertNotNull("Can't find classes file 'classes'", classRefsFile);
    }
    
    /**
     * Verify that if you have a read only cache, it should be used, but not written to.
     * 
     * This test is not called directly from the framework because it needs some additional set up.
     * It needs to have a preexisting cache.  So it is called from a test that has already
     * created the cache.
     * 
     * @throws Exception
     */
    private void verifyReadOnlyCacheIsNotWrittenTo() throws Exception {

        startServer();

        // Looking for a dir something like this: .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/anno
        verifyCacheExists();

        // The app contains a servlet.  Verify we can access it.
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");

        stopServer();
        
        // Create READ-ONLY settings
        // Remove the WAR directory from the cache
        // Restart the server (dirty) - not removing the rest of the cache.
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  // Default Jandex settings.  NOT using Jandex.
        installJvmOptions("JvmOptions_AnnoCacheReadOnly_True.txt");
        
        String warCacheDirName = getAnnoCacheAppRoot().getCanonicalPath() + "/M_%2FTestServlet40.war";
        File warCacheDir = new File(warCacheDirName);
        LOG.info("File [ " + warCacheDirName + " ] last modified [ " + convertTime(warCacheDir.lastModified()) + " ]");
        FileUtils.recursiveDelete(warCacheDir);        
        if (warCacheDir.exists()) {
            throw new Exception("Unable to delete WAR cache directory [ " + warCacheDirName + " ] ");
        }
        displayJvmOptions();
        
        SHARED_SERVER.startIfNotStarted(false, false, false);
        displayJvmOptions();
        
        // The cache should still exist, but a piece should be missing.
        verifyCacheExists();
        assertFalse("WAR Container [ " + warCacheDirName + " ] exists, but should not.", warCacheDir.exists());
        
        // The War contains a servlet, MyServlet.  Verify we can access it.
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        stopServer();
    } 
    
    /**
     * Display the web.xml file in the logs
     * @throws Exception
     */
    public static void displayJvmOptions() throws Exception {
        String serverDirName = getServerRoot();
        File jvmOptionsFile = new File(serverDirName + "/jvm.options");
        
        BufferedReader targetsReader = new BufferedReader(new FileReader(jvmOptionsFile)); 
        try {
            String line; 
            LOG.info("jvm.options:\n" + serverDirName + "/jvm.options");
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
    
    @Test
    public void testAnnotationCacheNotCreatedWhenDisabled() throws Exception {
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  // Default Jandex settings.  NOT using Jandex.
        installJvmOptions("JvmOptions_AnnoCacheDisabled_True.txt");
        startServer();

        // Looking for a dir something like this: .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/anno
        File annoCacheDir = getAnnoCacheRoot();
        assertNull("Found annotation cache directory 'anno', but caching is disabled.", annoCacheDir);
        
        // The app contains a servlet.  Verify we can access it.
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");

        stopServer();
    }    
       
    /**
     * Log the time that the targets file was created under the WAR Module directory. (for debugging)
     */
    public void logAnnoTargetsFileModifiedTime() throws Exception {
        File applicationWorkArea = getAnnoCacheAppRoot();
        String annoTargetsFileName = applicationWorkArea.getCanonicalPath() + "/M_%2FTestServlet40.war/C_seed/targets";
        File annoTargetsFile = new File(annoTargetsFileName);
        LOG.info("File [ " + annoTargetsFile.getName() + " ] last modified [ " + convertTime(annoTargetsFile.lastModified()) + " ]");
    }

    // The targets file will be modified by removing a servlet.

    public void modifyCache_AnnoTargets(File targetsFile, File backupTargetsFile) throws Exception {
        LOG.info("In modifyCache_AnnoTargets");

        LOG.info("Targets " + getFileData(targetsFile));
        LOG.info("Targets backup " + getFileData(backupTargetsFile));

        FileUtils.copyFile(targetsFile, backupTargetsFile);

        String targetsFileName = targetsFile.getName();
        String modifiedTargetsFileName = targetsFileName + ".MissingSimpleServlet";
        File modifiedTargetsFile = new File(modifiedTargetsFileName);

        transferExcept( targetsFile,
                        new String[] { "Class: testservlet40.jar.servlets.SimpleTestServlet" }, // Remove data for SimpleTestServlet
                        new int[] { 2 }, // Skip the class line plus one additional line
                        modifiedTargetsFile );

        renameFile(modifiedTargetsFile, targetsFile);

        LOG.info("Modified targets " + getFileData(targetsFile));

        LOG.info("RETURN");
    }

    private String getFileData(File file) {
        return "[ " + file.getPath() + " ] [ " + convertTime(file.lastModified()) + " ]";
    }

    public void transferExcept(File inputFile, String[] exceptLines, int[] regionSizes, File outputFile) throws IOException {
        LOG.info("In readExcept");

        LOG.info("Input [ " + inputFile.getPath() + " ]");
        for ( int regionNo = 0; regionNo < exceptLines.length; regionNo++ ) {
            LOG.info("  Skip [ " + exceptLines[regionNo] + " ] Count [ " + regionSizes[regionNo] + " ]");
        }
        LOG.info("Output [ " + outputFile.getPath() + " ]");

        BufferedReader inputReader = null;
        BufferedWriter outputWriter = null;

        try {
            inputReader = new BufferedReader( new FileReader(inputFile) ); 
            outputWriter = new BufferedWriter( new FileWriter(outputFile) );

            String line; 
            while ( (line = inputReader.readLine()) != null ) {
                boolean ignore = false;

                String trimLine = line.trim();
                for ( int regionNo = 0; regionNo < exceptLines.length; regionNo++ ) {
                    if ( trimLine.equals(exceptLines[regionNo]) ) {
                        LOG.info("Skip [ " + line + " ]");

                        int regionSize = regionSizes[regionNo];
                        for ( int ignoreNo = 1; ignoreNo < regionSize; ignoreNo++ ) {
                            LOG.info("Skip [ " + inputReader.readLine() + " ]");
                        }

                        ignore = true;
                        break;
                    }
                }

                if ( !ignore ) {
                    LOG.info("Keep [ " + line + " ]");
                    outputWriter.write(line);
                    outputWriter.write("\n");
                }
            } 

        } catch ( IOException e ) {
            LOG.info( e.getMessage() );
            throw e;

        } finally {
            if ( outputWriter != null ) {
                try {
                    outputWriter.close();
                } catch ( IOException e ) {
                    LOG.info( e.getMessage() );
                    throw e;
                }
            }  

            if ( inputReader != null ) {
                try {
                    inputReader.close();
                } catch ( IOException e ) {
                    LOG.info( e.getMessage() );
                    throw e;
                }
            }
        }
    }
    
    private static final Format SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy MM dd HH:mm:ss");

    public String convertTime(long time){
        return SIMPLE_DATE_FORMAT.format( new Date(time) );
    }
    
    public void renameFile(File oldFile, File newFile) throws Exception {
        if (newFile.exists()) {
            newFile.delete();
            if (newFile.exists()) {       
                throw new IOException("Could not delete [" + newFile.getAbsolutePath() + "]");
            } else {
                LOG.info("Deleted [" + newFile.getPath()  + "]");
            }
        } else {
            LOG.info("Cannot delete [ " + newFile.getPath() + " ].  It does not exist.  Suspicious, but we were going to delete it anyway.");
        }

        if ( oldFile.renameTo(newFile) ) {
            LOG.info("Successfully renamed [" + oldFile.getPath() + "] to [" + newFile.getPath() + "]");

        } else {
            String message = "Could not rename [ " + oldFile.getAbsolutePath() + " ] to [ " + newFile.getAbsolutePath()  + " ]";
            LOG.info(message);

            throw new IOException(message);
        }
    }
}
