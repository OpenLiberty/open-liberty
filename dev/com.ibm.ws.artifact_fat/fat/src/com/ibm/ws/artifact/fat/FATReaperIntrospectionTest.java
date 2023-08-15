/*******************************************************************************
 * Copyright (c) 2019,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class FATReaperIntrospectionTest {
    private static void logInfo(String methodName, String outputString){
        FATLogging.info(FATReaperIntrospectionTest.class, methodName, outputString);
    }

    //

    public static final String SERVER_NAME = "com.ibm.ws.artifact.zipDumps";

    protected static final String[] APP_NAMES = {
        "jarneeder.war",
        "testServlet1",
        "testServlet2"
    };

    /**
     * The names of the several archives which the server is expected to
     * open.  These are the names of the several applications which are
     * configured and the names of utility jars within the applications.
     */
    protected static final String[] ARCHIVE_NAMES = {
        "testServlet2.war",
        "jarneeder.war",
        "testServlet1.war",
        "TestJar.jar"
    };

    //

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static String ZIP_INTROSPECTOR_NAME = "ZipCachingIntrospector.txt";
    private static ZipCacheIntrospection zipIntrospection;

    private static void setIntrospection(File dumpFile) throws Exception {
        // Keep the dump archive open only as long as necessary.        
        try ( LookupZipArchive dumpArchive = new LookupZipArchive(dumpFile) ) {
            Assert.assertTrue(
                    "Zip introspection [ " + ZIP_INTROSPECTOR_NAME + " ]" +
                            " not found in [ " + dumpArchive.getName() + " ]",
                    dumpArchive.hasEntry(ZIP_INTROSPECTOR_NAME) );

            zipIntrospection = new ZipCacheIntrospection(
                    () -> dumpArchive.getInputStream(ZIP_INTROSPECTOR_NAME) );
        }
    }

    private static void unsetIntrospection() {
        zipIntrospection = null;
    }
    
    private static void performDump() throws Exception {
        String methodName = "performDump";
        logInfo(methodName, "Entering: " + methodName);

        server.executeServerScript("dump", null);

        logInfo(methodName, "Exiting: " + methodName);
    }

    protected static File getLatestDumpFile() {
        String methodName = "getDump";

        logInfo(methodName, "Server root [ " + server.getServerRoot() + " ]");

        File dumpFile = LookupZipArchive.getLatestDump(server);
        Assert.assertNotNull("Server dump not found in [ " + server.getServerRoot() + " ]", dumpFile);

        logInfo(methodName, "Located server dump [ " + dumpFile.getAbsolutePath() + " ]");
        return dumpFile;
    }

    protected static void startServer() throws Exception {
        Assert.assertNotNull("Null server: " + SERVER_NAME, server);

        for ( String appName: APP_NAMES ) {
            ShrinkHelper.defaultApp(server, appName, "com.ibm.ws.artifact.fat.servlet");
        }

        if ( !server.isStarted() ) {
            server.startServer();
        }
    }

    protected static void stopServer() throws Exception {
        if ( (server == null) || !server.isStarted() ) {
            return;
        }

        server.stopServer();

        // Turning this off ... not sure why introspector tests need
        // it in particular.

        // In practice, it is nearly impossible to get rid of the windows file lock
        // so we can delete this file. Closing the file is not enough. System.gc()
        // could help but isn't guaranteed. We just need to deal with not having
        // the logs on windows.

        // if ( server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS ) {
        //    server.stopServer(false);
        // }
    }
    
    //
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        String methodName = "setUpClass";
        logInfo(methodName, "Entering: " + methodName);

        startServer();
        performDump();
        setIntrospection( getLatestDumpFile() );
        
        logInfo(methodName, "Exiting: " + methodName);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        String methodName = "tearDownClass";
        logInfo(methodName, "Entering: " + methodName);

        unsetIntrospection();
        stopServer();

        logInfo(methodName, "Exiting: " + methodName);
    }

    //

    /**
     * Case: The zip cache introspection should have the expected archives.
     */    
    @SuppressWarnings("unused")
    @Test
    public void testZipHandles() throws Exception {
        String methodName = "testZipHandles";

        Assert.assertNotNull(
            "Zip Introspector missing ZipFileHandle section",
            zipIntrospection.getActiveAndCached());

        List<String> archiveHandleNames = zipIntrospection.getZipHandleArchiveNames();

        logInfo(methodName, "Zip file handles [ " + archiveHandleNames + " ]");
        List<String> missingArchives = new ArrayList<>();

        for( String archiveName : ARCHIVE_NAMES ) {
            logInfo(methodName, "Removing [ " + archiveName + " ]");
            if ( !archiveHandleNames.remove(archiveName) ) {
                missingArchives.add(archiveName);
            }
        }

        if ( !missingArchives.isEmpty() ) {
            Assert.assertTrue(
                "Zip introspection missing archives [ " + missingArchives + " ]",
                missingArchives.isEmpty() );
        }

        if ( !archiveHandleNames.isEmpty() ) {
            Assert.assertTrue(
                "Zip introspection extra archives [ " + archiveHandleNames + " ]",
                archiveHandleNames.isEmpty() );
        }
    }

    /**
     * Case: The Zip cache should not be waiting if there are zip files
     * in the pending queue.
     */
    @SuppressWarnings("unused")
    @Test
    public void testNotWaitingWithNonEmptyQueue() throws Exception {
        String methodName = "testNotWaitingWithNonEmptyQueue";

        String reaperState = zipIntrospection.getZipReaperThreadState();
        String runnerDelay = zipIntrospection.getZipReaperRunnerDelay();

        if ( reaperState.equals("WAITING") && runnerDelay.equals("INDEFINITE") ) {
            Assert.assertNull(
                "Active and Pending must be empty for the reaper to be in an indefinite wait",
                zipIntrospection.getActiveAndPending());

        } else {
            logInfo(methodName, "Test not applicable, reaper not in an indefinite wait");
        }
    }

    /**
     * Answer the first group from text which matches a pattern.
     * 
     * @param text Text which is to be tested.
     * @param pattern The target pattern.
     * 
     * @return The first group of the pattern, if the text matches
     *    pattern.  Null if the text does not match the pattern.
     */
    private static String findGroup(String text, Pattern pattern) {
        Matcher match = pattern.matcher(text);
        return ( match.find() ? match.group() : null );
    }

    private static final Pattern zipFileDataPattern = Pattern.compile("\\d+");

    /**
     * Case: Verify for each ZipFileData object the number of times opened
     * is equal to #(Pending -> open) + #(Closed -> open)
     */
    @SuppressWarnings("unused")
    @Test
    public void testOpenCounts() throws Exception {
        String methodName = "testOpenCounts";

        List<String> zipFileData = zipIntrospection.getAllZipFileData();
        if ( zipFileData.isEmpty() ) {
            Assert.fail("No zip file data was found.");
            return;
        }
        
        logInfo( methodName, "Zip file data [ " + zipFileData + " ]" );

        zipFileData.forEach( (String datum) -> {
            String[] datumLines = datum.split("\n");
            verifyOpenCounts(datumLines);
        });
    }

//    ZipFile [ .../wlp/usr/servers/com.ibm.ws.artifact.zipDumps/apps/testServlet1.war ]
//            State: [ FULLY_CLOSED ]
//            Request Counts:
//                Open Requests:  [ 000016 ]
//                Close Requests: [ 000016 ]
//                Active Opens:   [ 000000 ]
//            Lifetime:
//                Pre-Open:   [ 000000.254746 (s) ]
//                Open:       [ 000000.404625 (s) ]
//                Pending:    [ 000000.242750 (s) ]
//                Closed:     [ 000024.511253 (s) ]
//                Post-Close: [ 000000.000000 (s) ]
//                Total:      [ 000025.413375 (s) ]
//            Transition Counts:
//                Open:
//                    to Pending: [ 000016 ] [ 000000.404625 (s) ]
//                Pending:
//                    to Open:    [ 000012 ] [ 000000.101808 (s) ]
//                    to Close:   [ 000004 ] [ 000000.140941 (s) ]
//                Close:
//                    to Open:    [ 000004 ] [ 000000.318911 (s) ]
//                    Active:                [ 000024.192342 (s) ]
//            Event Times:
//                Open:
//                    First: [ 000000.254746 (s) ]
//                    Last:  [ 000001.194307 (s) ]
//                Pend:
//                    First: [ 000000.524717 (s) ]
//                    Last:  [ 000001.195421 (s) ]
//                Close:
//                    First: [ 000000.559142 (s) ]
//                    Last:  [ 000001.221033 (s) ]    

    // States:
    //   Open [count]
    //     --> Open, --> Pending
    //   Pending
    //     --> Open, --> Closed
    //   Closed
    //     --> Closed, --> Open
    
    public void verifyOpenCounts(String[] datumLines) {
        String methodName = "verifyOpenCounts";

        String zipFileName = datumLines[0].trim();
        logInfo(methodName, "Zip file [ " + zipFileName + " ]");

        // Use -1 for unset values.
        // The parsed value should never be less than zero.

        int timesOpened = -1;
        int pendingToOpen = -1;
        int closeToOpen = -1;

        for( String datumLine : datumLines ) {
            if ( datumLine.contains("Open Requests:") ) {
                String matchText = findGroup(datumLine, zipFileDataPattern);
                Assert.assertNotNull("Missing count from data line [ " + datumLine + " ]", matchText);
                timesOpened = Integer.parseInt( matchText.trim() );

            } else if ( datumLine.contains("to Open:") ) {
                String matchText = findGroup(datumLine, zipFileDataPattern);
                Assert.assertNotNull("Missing count from data line [ " + datumLine + " ]", matchText);

                // "to Open:" is for both the pending to open and the close to open.
                // Assign them in order.
                int count = Integer.parseInt( matchText.trim() );
                if ( pendingToOpen == -1 ) {
                    pendingToOpen = count;
                } else {
                    closeToOpen = count;
                }

            } else {
                // Ignore this line
            }
        }

        if ( timesOpened == -1 ) {
            Assert.fail("Missing 'Open Requests:' data");
        } else if ( pendingToOpen == -1 ) {
            Assert.fail("Missing pending 'toOpen:' data");
        } else if( closeToOpen == -1 ) {
            Assert.fail("Missing close 'toOpen:' data");
        }

        logInfo(methodName,
            "Opened [ " + timesOpened + " ];" +
            " Pending-to-open [ " + pendingToOpen + " ];" +
            " Closed-to-open [ " + closeToOpen + " ]");

        Assert.assertEquals("Open count does not match to-open sum",
            timesOpened,
            (pendingToOpen + closeToOpen) );
    }
}
