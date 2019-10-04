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

import org.junit.Assert;

import java.io.File;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.tests.anno.util.AppPackagingHelper;
import com.ibm.ws.tests.anno.util.Ear;
import com.ibm.ws.tests.anno.util.Jar;
import com.ibm.ws.tests.anno.util.Utils;
import com.ibm.ws.tests.anno.util.War;

import componenttest.topology.utils.FileUtils;

/**
 * Cache enablement tests: Verify that the cache is not created if
 * caching is disabled, or if the cache is enabled but is set to read-only.
 * Verify that the cache is create if caching is enabled.
 */
public class CacheEnablementTest extends AnnoCachingTest {
    private static final Logger LOG = Logger.getLogger(CacheEnablementTest.class.getName());

    // Server and application operations ...

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
           AppPackagingHelper.addEarToServerApps(SHARED_SERVER.getLibertyServer(), ear);

        } catch ( Exception e ) {
            LOG.info("Caught exception from addEarToServerApps [ " + e.getMessage() + " ]");
            throw e;
        }
    }

    //

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
     * BASE_FOLDER/A_TestServlet40_A/M_TestServlet40_M/classes
     *
     * This is one of several cache files which are created for each module.
     */
    private void verifyAnnoCacheExists() {
        File annoCacheDir = getAnnoCacheRoot();
        Assert.assertNotNull("Can't find root 'anno' directory", annoCacheDir);

        // Now find the application directory below the anno cache root
        // something like:
        // .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/anno/A_TestServlet40_A
        File annoCacheRootDir = getAnnoCacheAppRoot(EAR_CACHE_NAME);
        Assert.assertNotNull("Can't find application directory", annoCacheRootDir);

        // Test if 'classes' exists.  Assume cache created successfully if exists.
        File classRefsFile = Utils.findFile(annoCacheRootDir, "classes");
        Assert.assertNotNull("Can't find classes file", classRefsFile);
    }

    //

    @BeforeClass
    public static void setUp() throws Exception {
        setSharedServer();
        addAppToServerAppsDir();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        stopServer();
    }

    /**
     * Verify that no cache files are written when the the annotation cache is disabled.
     */
    @Test
    public void cacheEnablement_testDisabledCache() throws Exception {
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");
        installJvmOptions("JvmOptions_Disabled.txt");

        startServer(ServerStartType.DO_SCRUB);
        verifyResponse(TEST_URL_1, "Hello World");

        File annoCacheDir = getAnnoCacheRoot();
        Assert.assertNull("Found annotation cache directory 'anno', but caching is disabled.", annoCacheDir);

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
    public void cacheEnablement_testEnabledCache() throws Exception {
        // Part 1: Clean startup with cache enabled.
        //         Verify that the cache is created.

        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");
        installJvmOptions("JvmOptions_Enabled_Text.txt");

        startServer(ServerStartType.DO_SCRUB);
        verifyResponse(TEST_URL_1, "Hello World");
        stopServer();
        verifyAnnoCacheExists();

        // Part 2: Cache reuse.
        //         Verify that cache data is reused on a non-clean startup.

        // wlp/usr/servers/annoFat_server/workarea/
        //   org.eclipse.osgi/43/data/anno/
        //     A_TestServlet40_A/M_TestServlet40_M/C_seed_C.data

        String targetsPath = selectSeedTargets(EAR_CACHE_NAME, WAR_CACHE_NAME);

        File targetsFile = new File(targetsPath);

        String backupPath = prefixName(targetsPath, "backup.");

        File backupFile = new File(backupPath);

        Utils.logFile("Targets (pre-update)", targetsFile);
        Utils.logFile("Targets (pre-update)", backupFile);

        removeSimpleServlet(targetsFile, backupFile);

        Utils.logFile("Targets (post-update, pre-start-1)", targetsFile);

        startServer(ServerStartType.DO_NOT_SCRUB);
        verifyBadUrl(TEST_URL_1); // Should no longer be available.
        stopServer("CWWKZ0014W", "SRVE0190E"); // Extra parameters are to check for a bad URL message.

        // Part 3: Cache reuse:
        //         Verify a second time that the cache data is reused.

        Utils.logFile("Targets (post-start-1, pre-restore)", targetsFile);

        LOG.info("Restore [ " + targetsPath + " ] from [ " + backupPath + " ]");
        FileUtils.copyFile(backupFile, targetsFile);

        Utils.logFile("Targets (post-restore, pre-start-2)", targetsFile);

        startServer(ServerStartType.DO_NOT_SCRUB);
        verifyResponse(TEST_URL_1, "Hello World"); // Should be there again.
        stopServer();

        Utils.logFile("Targets (post-restore, post-start-2)", targetsFile);
    }

    // Prefix the name value.  That prevents the adjusted
    // name from appearing to be a container.

    private String prefixName(String path, String prefix) {
        int slashLoc = path.lastIndexOf('/');
        if ( slashLoc == -1 ) {
            slashLoc = path.lastIndexOf('\\');
            if ( slashLoc == -1 ) {
                return prefix + path;
            } else {
                // Fall through
            }
        }
        return path.substring(0, slashLoc + 1) + prefix + path.substring(slashLoc + 1);
    }

    private String selectSeedTargets(String earName, String warName) {
        String targetsMerged =
                getAnnoCacheAppRoot(earName).getAbsolutePath() +
                "/M_" + warName + "_M" +
                "/C_seed_C.data";
        
        File targetsFileMerged = new File(targetsMerged);

        Assert.assertTrue(
            "Seed results [ " + targetsFileMerged.getAbsolutePath() + " ] should exist",
            targetsFileMerged.exists());

        return targetsMerged;
    }

    /**
     * Modify the cache targets file by removing "SimpleTestServlet"
     * from the cache data.
     */
    private void removeSimpleServlet(File targets, File backupTargets) throws Exception {
        LOG.info("removeSimpleServlet ENTER");

        FileUtils.copyFile(targets, backupTargets);

        String targetsPath = targets.getPath();
        String modifiedTargetsPath = prefixName(targetsPath, "MissingSimpleServlet.");
        File modifiedTargets = new File(modifiedTargetsPath);

        // Keep [ # Class Annotation Targets: ]
        // Skip [ Class: testservlet40.jar.servlets.SimpleTestServlet ]
        // Skip [   Class Annotation: javax.servlet.annotation.WebServlet ]
        // Keep [ # X==========================================================X ]

        // Limit the transfer to the class annotation targets region when the
        // class targets information is in the same file as the class information.

        Utils.transferExcept(
            targets,
            new String[] { "Class: testservlet40.jar.servlets.SimpleTestServlet" },
            new String[] { "# Class Annotation Targets:" },
            new String[] { "# X==========================================================X" },
            new int[] { 2 }, // Skip the class line plus one additional line
            modifiedTargets );

        Utils.rename(modifiedTargets, targets);

        LOG.info("removeSimpleServlet RETURN");
    }

    /**
     * Verify that when the cache is in read-only mode, that it will
     * generate but not write missing data.
     */
    @Test
    public void cacheEnablement_testReadOnlyCache() throws Exception {
        // Part 1: Verify that the cache is created for usual enablement.

        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");
        installJvmOptions("JvmOptions_Enabled.txt");

        startServer(ServerStartType.DO_SCRUB);
        verifyResponse(TEST_URL_1, "Hello World"); // Make sure the servlet is available.
        stopServer();

        verifyAnnoCacheExists();

        // Step 2: Trim out parts of the cache, then restart the server with
        //         the cache set to read-only mode.  Do NOT do a clean start.
        //
        //         Missing data should be made available to the web container
        //         without the cache being written.

        String warCacheDirName = getAnnoCacheAppRoot(EAR_CACHE_NAME).getAbsolutePath() + "/M_%2F" + WAR_CACHE_NAME + "_M";
        File warCacheDir = new File(warCacheDirName);
        Utils.logFile("WAR cache", warCacheDir);

        FileUtils.recursiveDelete(warCacheDir);
        if ( warCacheDir.exists() ) {
            throw new Exception("Failed to delete WAR cache directory [ " + warCacheDirName + " ] ");
        }

        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");
        installJvmOptions("JvmOptions_Enabled_ReadOnly.txt");

        startServer(ServerStartType.DO_NOT_SCRUB);
        verifyResponse(TEST_URL_2, "Hello World");
        stopServer();

        verifyAnnoCacheExists();
        Assert.assertFalse("WAR Container [ " + warCacheDirName + " ] exists, but should not.", warCacheDir.exists());
    } 
}
