/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.artifact.fat_bvt.test.utils.FATFeatureDef;
import com.ibm.ws.artifact.fat_bvt.test.utils.FATLogging;
import com.ibm.ws.artifact.fat_bvt.test.utils.FATServerUtils;

import componenttest.topology.impl.LibertyServer;

/**
 * <p>Liberty artifact file system BVT tests.</p> 
 */
public class FATArtifactBVT {
    private static final Class<?> TEST_CLASS = FATArtifactBVT.class;
    // private static final String CLASS_NAME = TEST_CLASS.getSimpleName();

    public static void info(String methodName, String text) {
        FATLogging.info(TEST_CLASS, methodName, text);
    }

    public static void info(String methodName, String text, String value) {
        FATLogging.info(TEST_CLASS, methodName, text, value);
    }

    // Artifact FAT BVT server ...

    private static LibertyServer server;

    public static void setUpServer() throws Exception {
        server = FATServerUtils.prepareServerAndWar(
            FATArtifactBVTServer.SERVER_NAME,
            FATArtifactBVTServer.WAR_DEF,
            new FATFeatureDef[] { FATArtifactBVTServer.FEATURE_DEF },
            null );
    }

    public static void tearDownServer() throws Exception {
        server = null;
    }

    public static LibertyServer getServer() {
        return server;
    }

    public static String getHostName() {
        return server.getHostname();
    }

    public static int getPortNumber() {
        return server.getHttpDefaultPort();
    }

    public static void startServer() throws Exception {
        getServer().startServer(); // 'startServer' throws Exception
    }

    public static void stopServer() throws Exception {
        getServer().stopServer(); // 'stopServer' throws Exception
    }

    public static URL getTestUrl(String suffix) throws MalformedURLException {
        return FATServerUtils.getRequestUrl(
            getServer(),
            FATArtifactBVTServer.CONTEXT_ROOT,
            suffix );
        // 'getRequestUrl' throws MalformedURLException
    }

    public static List<String> getResponse(URL url) throws Exception {
        return FATServerUtils.getResponse( getServer(), url );
        // 'getResponse' throws Exception
    }

    // Test setup ...

    @BeforeClass
    public static void setUp() throws Exception {
        setUpServer(); // throws Exception
        startServer(); // throws Exception
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer(); // throws Exception
        tearDownServer(); // throws Exception
    }

    //

    @Test
    public void testDir() throws Exception {
        validateResponse( "testDir", getTestUrl("?testDir") );
    }

    @Test
    public void testJar() throws Exception {
        validateResponse( "testJar", getTestUrl("?testJar") );
    }

    @Test
    public void testRar() throws Exception {
        validateResponse( "testRar", getTestUrl("?testRar") );
    }

    @Test
    public void testDirMedium() throws Exception {
        validateResponse( "testDirMedium", getTestUrl("?testDirMedium") );
    }

    @Test
    public void testJarMedium() throws Exception {
        validateResponse( "testJarMedium", getTestUrl("?testJarMedium") );
    }

    @Test
    public void testJarNested() throws Exception {
        validateResponse( "testJarNested", getTestUrl("?testJarNested") );
    }

    @Test
    public void testDirNavigation() throws Exception {
        validateResponse( "testDirNavigation", getTestUrl("?testDirNavigation") );
    }

    @Test
    public void testZipNavigation() throws Exception {
        validateResponse( "testZipNavigation", getTestUrl("?testZipNavigation") );
    }

    @Test
    public void testInterpretedAdaptable() throws Exception {
        validateResponse( "testInterpretedAdaptable", getTestUrl("?testInterpretedAdaptable") );
    }

    @Test
    public void testInterpretedAdaptableRoots() throws Exception {
        validateResponse( "interpretedAdaptableTestRoots", getTestUrl("?testInterpretedAdaptableRoots") );
    }

    @Test
    public void testAddEntryToOverlay() throws Exception {
        validateResponse( "testAddEntryToOverlay", getTestUrl("?testAddEntryToOverlay") );
    }

    @Test
    public void testUnableToAdapt() throws Exception {
        validateResponse( "testUnableToAdapt", getTestUrl("?unableToAdapt") );
    }

    @Test
    public void testDirOverlay() throws Exception {
        validateResponse( "testDirOverlay", getTestUrl("?testDirOverlay") );
    }

    @Test
    public void testAdapt() throws Exception {
        validateResponse( "testAdapt", getTestUrl("?testAdapt") );
    }

    @Test
    public void testPhysicalPath() throws Exception {
        validateResponse( "testPhysicalPath", getTestUrl("?testPhysicalPath") );
    }

    @Test
    public void testLooseRead() throws Exception {
        validateResponse( "testLooseRead", getTestUrl("?testLooseRead") );
    }

    @Test
    public void testNotify() throws Exception {
        validateResponse( "testNotify", getTestUrl("?testNotify") );
    }

    @Test
    public void testLooseCaseSensitivity() throws Exception {
        validateResponse( "testLooseCaseSensitivity", getTestUrl("?testLooseCaseSensitivity") );
    }

    @Test
    public void testFileCaseSensitivity() throws Exception {
        validateResponse( "testFileCaseSensitivity", getTestUrl("?testFileCaseSensitivity") );
    }

    @Test
    public void testFileSystem() throws Exception {
        validateResponse( "testFileSystem", getTestUrl("?testFileSystem") );
    }

    @Test
    public void testDotDotPath() throws Exception {
        validateResponse( "testDotDotPath", getTestUrl("?testDotDotPath") );
    }

    @Test
    public void testImpliedZipDir() throws Exception {
        validateResponse( "testImpliedZipDir", getTestUrl("?testImpliedZipDir") );
    }

    @Test
    public void testSimpleBundleArtifactApi() throws Exception {
        validateResponse( "testSimpleBundleArtifactApi", getTestUrl("?testSimpleBundleArtifactApi") );
    }

    @Test
    public void testZipCachingService() throws Exception {
        validateResponse( "testZipCachingService", getTestUrl("?testZipCachingService") );
    }

    // Test for 54588.
    @Test
    public void testGetEnclosingContainerOnBundle() throws Exception {
        validateResponse( "testGetEnclosingContainerOnBundle", getTestUrl("?testGetEnclosingContainerOnBundle") );
    }

    // Test for 100419
    @Test
    public void testZipMulti() throws Exception {
        validateResponse( "testZipMulti", getTestUrl("?testZipMulti") );
    }

    // Test for 160622 / 153698
    @Test
    public void testCustomContainer() throws Exception {
        validateResponse( "testCustomContainer", getTestUrl("?testCustomContainer") );
    }

    // Test for 842145
    @Test
    public void testBadBundlePathIteration() throws Exception {
        validateResponse( "testBadBundlePathiteration", getTestUrl("?testBadBundlePathIteration") );
    }

    //

    public void validateResponse(String testName, URL url) throws Exception {
        String methodName = "validateResponse";
        info(methodName, "Test [ " + testName + " ]");

        List<String> responseLines = getResponse(url); // throws Exception

        // *** There must be at least one response line. ***

        Assert.assertTrue("Empty response", !responseLines.isEmpty());

        // *** And the first response line must be correct. ***

        Assert.assertEquals("Incorrest responst",
                            FATArtifactBVTServer.STANDARD_RESPONSE,
                            responseLines.get(0));

        int failCount = 0;
        int passCount = 0;

        for ( String responseLine : responseLines ) {
            if ( responseLine.startsWith("FAIL: ") ) {
                info(methodName, "Failure", responseLine);
                failCount++;
            } else if ( responseLine.startsWith("PASS") ) {
                info(methodName, "Success", responseLine);
                passCount++;
            }
        }

        // *** There must be no failure messages, and at least one pass message. ***

        String failureMessage;
        if ( failCount > 0 ) {
            failureMessage = "[ FAIL ] detected";
        } else if ( passCount == 0 ) {
            failureMessage = "No [ PASS ] detected";
        } else {
            failureMessage = null;
        }

        if ( failureMessage != null ) {
            Assert.fail( failureMessage + " [ " + FATLogging.asText(responseLines) + " ]" );
        }
    }
}
