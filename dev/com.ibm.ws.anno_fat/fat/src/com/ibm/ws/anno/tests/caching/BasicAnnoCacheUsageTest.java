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
 * Test that the annotation cache is created, restart the server, and check that the cache is being used.
 *
 * <code>
 * public void testAnnotationCacheNotCreatedWhenDisabled() throws Exception;
 * public void testAnnotationCacheCreatedAndIsActuallyUsed() throws Exception;
 * </code>
 */
public class BasicAnnoCacheUsageTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(BasicAnnoCacheUsageTest.class.getName());

    // Server and application operations ...

    // Not using ClassRule annotation.  So server does NOT start automatically.

    public static final String SERVER_NAME = "annoFat_server";

    public static SharedServer SHARED_SERVER =
        new SharedServer(SERVER_NAME, false); // false: don't wait for security;

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    protected static String serverRoot;

    protected static String getServerRoot() throws Exception {
        if ( serverRoot == null ) {
            serverRoot = SHARED_SERVER.getLibertyServer().getServerRoot();
            LOG.info("getServerRoot: " + serverRoot);
        }
        return serverRoot;
    }

    public static final String EAR_NAME = "TestServlet40.ear";
    public static final String EAR_CACHE_NAME = "TestServlet40";

    public static final String WAR_NAME = "TestServlet40.war";
    public static final String WAR_CACHE_NAME = "TestServlet40";

    public static final String TEST_URL_1 = "/TestServlet40/SimpleTestServlet";
    public static final String TEST_URL_2 = "/TestServlet40/MyServlet";

    protected static void addAppToServerAppsDir() throws Exception {
        LOG.info("Add " + EAR_NAME + " to the server");

        Ear ear = new Ear(EAR_NAME);

        War war = new War(WAR_NAME);
        ear.addWar(war);
        
        Jar jar1 = new Jar("TestServlet40.jar");
        jar1.addPackageName("testservlet40.jar.servlets");
        jar1.addPackageName("testservlet40.jar.util");

        Jar jar2 = new Jar("TestServletA.jar");
        jar2.addPackageName("testservleta.jar.servlets");
        jar2.addPackageName("testservleta.jar.util");

        Jar jar3 = new Jar("TestServletB.jar");
        jar3.addPackageName("testservletb.jar.servlets");
        jar3.addPackageName("testservletb.jar.util");

        Jar jar4 = new Jar("TestServletC.jar");
        jar4.addPackageName("testservletc.jar.servlets");
        jar4.addPackageName("testservletc.jar.util");

        Jar jar5 = new Jar("TestServletD.jar");
        jar5.addPackageName("testservletd.jar.servlets");
        jar5.addPackageName("testservletd.jar.util");

        war.addJar(jar1);
        war.addJar(jar2);
        war.addJar(jar3);
        war.addJar(jar4);
        war.addJar(jar5);

        war.addPackageName("testservlet40.war.servlets");

        try {
           FatHelper.addEarToServerApps(SHARED_SERVER.getLibertyServer(), ear);

        } catch ( Exception e ) {
            LOG.info("Caught exception from addEarToServerApps [ " + e.getMessage() + " ]");
            throw e;
        }
    }

    protected static void installServerXml(String sourceServerXml) throws Exception {
        LOG.info("installServerXml: " + sourceServerXml);

        String serverRootDir = getServerRoot();
        File serverXmlFile = new File(serverRootDir + "/server.xml");

        if ( serverXmlFile.exists() ) {
            serverXmlFile.delete();
        }

        File serverConfigurationFile = new File(serverRootDir + "/serverConfigurations/" + sourceServerXml);
        FileUtils.copyFile(serverConfigurationFile, serverXmlFile); 
    }

    protected static void installJvmOptions(String sourceJvmOptions) throws Exception {
        LOG.info("installJvmOptions: " + sourceJvmOptions);

        String serverRootDir = getServerRoot();
        File jvmOptionsFile = new File(serverRootDir + "/jvm.options");

        if ( jvmOptionsFile.exists() ) {
            jvmOptionsFile.delete();
        }

        // if sourceJvmOptions is null, then the assumption is that we just want to delete any existing jvm.options
        if ( sourceJvmOptions != null ) {
            File serverJvmOptionsFile = new File(serverRootDir + "/serverConfigurations/" + sourceJvmOptions);
            FileUtils.copyFile(serverJvmOptionsFile, jvmOptionsFile); 
        }
    }

    public static void displayJvmOptions() throws Exception {
        String serverDirName = getServerRoot();
        File jvmOptionsFile = new File(serverDirName + "/jvm.options");

        LOG.info("jvm.options:\n" + serverDirName + "/jvm.options");

        if ( !jvmOptionsFile.exists() ) {
            LOG.info("  ** DOES NOT EXIST **");
            return;
        }

        BufferedReader targetsReader = new BufferedReader( new FileReader(jvmOptionsFile) ); 
        try {
            String line; 
            while ( (line = targetsReader.readLine()) != null ) {
                LOG.info(line);
            } 
        } catch ( IOException e ) {
            LOG.info( e.getMessage() );
            throw e;
        } finally {
            if ( targetsReader != null ) {
                targetsReader.close();
            } 
        }
    }
    
    public static final boolean DO_CLEAN = true;
    public static final boolean DO_NOT_CLEAN = false;

    protected static void startServer(boolean doClean) throws Exception {
        LOG.info("startServer: starting server");

        SHARED_SERVER.startIfNotStarted(false,   // Don't preClean 
                                        doClean,
                                        false);  // Don't validate apps have started

        LOG.info("startServer: started server");
    }    

    protected static void stopServer() throws Exception {    
        stopServer("CWWKZ0014W");
    }

    protected static void stopServer(String... expectedMessages) throws Exception {
        LOG.info("stopServer: stopping server");
        for ( String message : expectedMessages ) {
            LOG.info("stopServer: expecting [ " + message + " ]");
        }

        SHARED_SERVER.getLibertyServer().stopServer(expectedMessages);

        LOG.info("stopServer: stopped server");
    }

    // Cache access ...

    public boolean isSetCacheDir;
    public File cacheDir;

    public File getAnnoCacheRoot() {
        if ( !isSetCacheDir ) {
            String osgiWorkAreaRoot = SHARED_SERVER.getLibertyServer().getOsgiWorkAreaRoot();
            LOG.info("getAnnoCacheDirectory: OSGI workarea [ " + osgiWorkAreaRoot + " ]");

            File annoCacheDir = findDirectory(new File(osgiWorkAreaRoot), AnnoCacheLocations.CACHE_NAME);
            LOG.info("getAnnoCacheDirectory: annoCache Dir [ " + annoCacheDir + " ]");

            if ( annoCacheDir != null ) {
                isSetCacheDir = true;
                cacheDir = annoCacheDir;
            }
        }
        return cacheDir;
    }
    
    public File getAnnoCacheAppRoot() {
        File annoCacheDir = getAnnoCacheRoot();
        if ( annoCacheDir == null ) {
            LOG.info("getAppDirectory: Failed; Cache root 'anno' not found");
            return null;
        }

        File appDir = findDirectory(annoCacheDir, AnnoCacheLocations.APP_PREFIX + EAR_CACHE_NAME);
        LOG.info("getAppDirectory: App Directory[" + appDir + "]");
        return appDir;
    }    

    /**
     * Verify the cache exists by looking for key cache files.
     *
     * The base folder is:
     *
     * SERVER_ROOT/workarea/org.eclipse.osgi/BUNDLE_NUMBER/data/anno
     *
     * Where "BUNDLE_NUMBER" is the number assigned to the annotations
     * bundle.  The exact number varies.
     *
     * The key cache file is:
     *
     * BASE_FOLDER/A_TestServlet40/M_%2FTestServlet40/classes
     *
     * This is one of several cache files which are created for each module.
     */
    private void verifyAnnoCacheExists() {
        File annoCacheDir = getAnnoCacheRoot();
        assertNotNull("Can't find root 'anno' directory", annoCacheDir);
        
        // Now find the application directory below the anno cache root
        // something like:  .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/anno/A_TestServlet40
        File annoCacheRootDir = getAnnoCacheAppRoot();
        assertNotNull("Can't find application directory", annoCacheRootDir);

        // Test if 'classes' exists.  Assume cache created successfully if exists.
        File classRefsFile = findFile(annoCacheRootDir, "classes");
        assertNotNull("Can't find classes file", classRefsFile);
    }
    
    /**
     * Modify the cache targets file by removing "SimpleTestServlet" from the cache.
     */
    public void modifyTargets(File targets, File backupTargets) throws Exception {
        LOG.info("modifyTargets ENTER");

        FileUtils.copyFile(targets, backupTargets);

        String targetsName = targets.getName();
        String modifiedTargetsName = targetsName + ".MissingSimpleServlet";
        File modifiedTargets = new File(modifiedTargetsName);

        transferExcept( targets,
                        new String[] { "Class: testservlet40.jar.servlets.SimpleTestServlet" },
                        new int[] { 2 }, // Skip the class line plus one additional line
                        modifiedTargets );
        renameFile(modifiedTargets, targets);

        LOG.info("modifyTargets RETURN");
    }

    private void logFile(String title, File file) {
        String logText;

        if ( !file.exists() ) {
            logText = ( title + ": " +
                        "Path [ " + file.getPath() + " ] "+
                        "[ ** DOES NOT EXIST ** ]");

        } else if ( file.isDirectory() ) {
            logText = ( title + ": " +
                        "Path [ " + file.getPath() + " ] "+
                        "[ ** DIRECTORY ** ]");

        } else {
            logText = ( title + ": " +
                        "Path [ " + file.getPath() + " ] " +
                        "Size [ " + file.length() + " ] " +
                        "Last update [ " + convertTime(file.lastModified()) + " ]" );
        }

        LOG.info(logText);
    }


    //

    @BeforeClass
    public static void setUp() throws Exception {
        addAppToServerAppsDir();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        stopServer();
    }

    /**
     * Verify that no cache files are written when the the annotation cache is disabled.
     *
     * Start by removing any previous cache.
     */
    // @Test
    public void testAnnotationCacheNotCreatedWhenDisabled() throws Exception {
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml"); // Default: Do NOT use Jandex.
        installJvmOptions("JvmOptions_AnnoCacheDisabled_True.txt"); // Set JVM options to disable the annotation cache.
        displayJvmOptions();

        startServer(DO_CLEAN);
        verifyResponse(TEST_URL_1, "Hello World"); // Make sure the servlet is available.

        // A clean server start with the cache disabled:
        // No cache files should be present.

        File annoCacheDir = getAnnoCacheRoot();
        assertNull("Found annotation cache directory 'anno', but caching is disabled.", annoCacheDir);

        stopServer();
    }    

    /**
     * Verify that cache files are written and is used on a restart.
     *
     * Start by removing any previous cache.  Run a simple servlet test,
     * stop the server, edit the cache by removing the servlet
     * annotations, then restart the server.  The modified cache should
     * effectively cause the servlet to disappear from the application.
     */
    @Test
    public void testAnnotationCacheCreatedAndIsActuallyUsed() throws Exception {
        // Step 1: Start the server using full default and with the "--clean" option.
        //
        //         The annotation cache should be created.

        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");  // Default: Do NOT use Jandex.

        installJvmOptions(null); // Do NOT use any JVM options: Annotation caching is enabled.
        displayJvmOptions();

        startServer(DO_CLEAN);
        verifyResponse(TEST_URL_1, "Hello World"); // Make sure the servlet is available.

        stopServer();

        verifyAnnoCacheExists();

        // Step 2: Hack the cache by removing a servlet target, then start
        //         the server using the annotation cache.
        //
        //         Verify that the modified cache values are used.
        //         The servlet class is still present, but should not
        //         be visible to the web container.
        //
        //         Then restore the modified file and restart the server.
        //         The servlet should again be visible to the web container.

        String targetsName = getAnnoCacheAppRoot().getCanonicalPath() + "/M_%2F" + WAR_NAME + "/C_seed/targets";
        File targets = new File(targetsName);

        String backupName =  targetsName + ".backup";
        File backup = new File(backupName);

        logFile("Targets (pre-update)", targets);

        modifyTargets(targets, backup);

        logFile("Targets (post-update, pre-start-1)", targets);

        startServer(DO_NOT_CLEAN);
        verifyBadUrl(TEST_URL_1); // Should NOT be there.

        stopServer("CWWKZ0014W", "SRVE0190E"); // Extra parameters are to check for a bad URL message.

        logFile("Targets (post-start-1, pre-restore)", targets);

        LOG.info("Restore [ " + targetsName + " ] from [ " + backupName + " ]");
        FileUtils.copyFile(backup, targets);

        logFile("Targets (post-restore, pre-start-2)", targets);

        startServer(DO_NOT_CLEAN);
        verifyResponse(TEST_URL_1, "Hello World"); // Should be there again.

        stopServer();

        logFile("Targets (post-restore, post-start-2)", targets);
    }
    
    /**
     * Verify that if you have a read only cache, it should be used, but not written to.
     */
    // @Test
    public void verifyReadOnlyCacheIsNotWrittenTo() throws Exception {
        // Step 1: Start the server using full default and with the "--clean" option.
        //
        //         The annotation cache should be created.

        installServerXml("jandexDefaultsAutoExpandTrue_server.xml"); // Default: Do NOT use Jandex.

        installJvmOptions(null); // Do NOT use any JVM options: Annotation caching is enabled.
        displayJvmOptions();

        startServer(DO_CLEAN);
        verifyResponse(TEST_URL_1, "Hello World"); // Make sure the servlet is available.

        stopServer();

        verifyAnnoCacheExists();

        // Step 2: Trim out parts of the cache, then restart the server with
        //         the cache set to read-only mode.  Do NOT do a clean start.
        //
        //         Missing data should be made available to the web container
        //         without the cache being written.

        String warCacheDirName = getAnnoCacheAppRoot().getCanonicalPath() + "/M_%2F" + WAR_CACHE_NAME;
        File warCacheDir = new File(warCacheDirName);
        LOG.info("File [ " + warCacheDirName + " ] last modified [ " + convertTime(warCacheDir.lastModified()) + " ]");
        FileUtils.recursiveDelete(warCacheDir);        
        if ( warCacheDir.exists() ) {
            throw new Exception("Failed to delete WAR cache directory [ " + warCacheDirName + " ] ");
        }
        
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml"); // Default: Do NOT use Jandex.
        installJvmOptions("JvmOptions_AnnoCacheReadOnly_True.txt"); // Set the cache to read-only mode.
        displayJvmOptions();
        
        startServer(DO_NOT_CLEAN);

        verifyResponse(TEST_URL_2, "Hello World");

        stopServer();

        verifyAnnoCacheExists();
        assertFalse("WAR Container [ " + warCacheDirName + " ] exists, but should not.", warCacheDir.exists());
    } 
    
    // File primitives ...

    /**
     * Search for a simple file starting with a specified directory or file.
     *
     * Search recursively until a match is found.  Answer the first match which
     * is found.  (Which match is first depends on the ordering used by
     * {@link File#listFiles}, which is ambiguous.)
     *
     * Do not match directories: The target of the search is a simple file.
     *
     * @param file The initial file or directory to test.
     * @param searchFileName The name of the target simple file.
     *
     * @return The simple file matching the target name.  Null if no match is found.
     */
    private File findFile(File file, String searchFileName) {
        if ( file.isDirectory() ) {
            // Do NOT match directories against the target file name.

            // Doesn't match THIS directory, but may match a CHILD simple file.
            File[] arr = file.listFiles();
            for ( File f : arr ) {
                File found = findFile(f, searchFileName);
                if ( found != null ) {
                    return found;
                }
            }
        } else {
            // DO match simple files against the target file name.
            if ( file.getName().equals(searchFileName) ) {
                return file;
            }
        }
        return null;
    }
    
    /**
     * Search for a directory starting with a specified directory or file.
     *
     * Search recursively until a match is found.  Answer the first match which
     * is found.  (Which match is first depends on the ordering used by
     * {@link File#listFiles}, which is ambiguous.)
     *
     * Do not match simple files: The target of the search is a directory.
     *
     * @param file The initial file or directory to test.
     * @param searchFileName The name of the target directory file.
     *
     * @return The directory matching the target name.  Null if no match is found.
     */
    private File findDirectory(File file, String searchFileName) {
        if ( file.isDirectory() ) {
            // DO match directories against the target file name.
            if ( file.getName().equals(searchFileName) ) {
                return file;
            }
            // Didn't match the THIS directory, but may match a CHILD directory.
            File[] arr = file.listFiles();
            for ( File f : arr ) {
                File found = findDirectory(f, searchFileName);
                if ( found != null ) {
                    return found;
                }
            }
        } else {
            // Do NOT match simple files against the target file name.
        }
        return null;
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
        LOG.info("Renaming [ " + oldFile.getPath() + " ] to [ " + newFile.getPath() + " ]");

        if ( newFile.exists() ) {
            newFile.delete();
            if ( newFile.exists() ) {
                throw new IOException("Could not delete [ " + newFile.getAbsolutePath() + " ]");
            } else {
                LOG.info("Deleted [ " + newFile.getPath()  + " ]");
            }
        } else {
            LOG.info("Strange: Target does not exist [ " + newFile.getPath() + " ]");
        }

        if ( oldFile.renameTo(newFile) ) {
            LOG.info("Renamed [" + oldFile.getPath() + "] to [" + newFile.getPath() + "]");

        } else {
            String message = "Failed to rename [ " + oldFile.getAbsolutePath() + " ] to [ " + newFile.getAbsolutePath()  + " ]";
            LOG.info(message);
            throw new IOException(message);
        }
    }
}
