/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Assert;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.annotation.Server;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

import com.ibm.websphere.simplicity.ShrinkHelper;

import java.io.InputStream;
import java.beans.Transient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




@RunWith(FATRunner.class)
public class FATReaperIntrospectionTest{

    @Server(FATReaperIntrospectionTest.serverName)
    public static LibertyServer server;
    public static final String serverName = "com.ibm.ws.artifact.zipDumps";
    private static final String[] appNames = {
        "jarneeder.war",
        "testServlet1",
        "testServlet2"
    };
    private static final String[] archivesAddedToServer = {
        "testServlet2.war",
        "jarneeder.war",
        "testServlet1.war",
        "TestJar.jar"
    };
    private static final int SECONDS_WAITING_FOR_DUMP = 30;
    private static final int ATTEMPTS_TO_RETRY  = 2;
    private static DumpArchive dump = null;

    private static void logInfo(String methodName, String outputString){
        FATLogging.info(FATReaperIntrospectionTest.class, methodName, outputString);
    }

    private static DumpArchive getRecentServerDump(LibertyServer server) throws Exception{
        String methodName = "getRecentServerDump";
        logInfo(methodName, "Entering: " + methodName);

        DumpArchive dumpRef = DumpArchive.getMostRecentDumpArchive(server);
        int retryCount = 0;

        //retry logic if the server dump takes a long time
        while(dumpRef == null && retryCount < ATTEMPTS_TO_RETRY){
            logInfo(methodName, "Failed to get the server dump (retry #" + ++retryCount + "). Going to sleep.");
            Thread.sleep(1000 *SECONDS_WAITING_FOR_DUMP);

            dumpRef = DumpArchive.getMostRecentDumpArchive(server);
        }

        //if the retry logic still fails then fail the test
        Assert.assertNotNull("Server dump not found in directory: " + server.getServerRoot(), dumpRef);

        logInfo(methodName, "Exiting: " + methodName);

        return dumpRef;
    }

    private static void sendDumpActionToServer(LibertyServer server) throws Exception{
        String methodName = "sendDumpActionToServer";

        logInfo(methodName, "Entering: " + methodName);

        //send the dump action to the server
        server.executeServerScript("dump",null);

        logInfo(methodName, String.format("Sent dump action to server going to sleep for %d seconds", SECONDS_WAITING_FOR_DUMP));

        //wait 30 seconds for the dump to be created in the server directory
        Thread.sleep(1000 * SECONDS_WAITING_FOR_DUMP);

        logInfo(methodName, "Exiting: " + methodName);
    }


    @BeforeClass
    public static void setUpClass() throws Exception{
        String methodName = "setUpClass";
        logInfo(methodName, "Entering: " + methodName);
        
        Assert.assertNotNull("Server reference is null in test class", server);


        //install applications to server for tests
        for(String appName: appNames){
            ShrinkHelper.defaultApp(server,appName, "com.ibm.ws.artifact.fat.servlet");
        }


        if(!server.isStarted()){
            server.startServer();
        }

        //dump the server
        sendDumpActionToServer(server);

        //set the dump archive object
        dump = getRecentServerDump(server);


        logInfo(methodName, "Exiting: " + methodName);
    }

    @AfterClass
    public static void tearDownClass() throws Exception{
        String methodName = "tearDownClass";
        logInfo(methodName, "Entering: " + methodName);

        if(server != null && server.isStarted()){
            server.stopServer();
        }

        if(dump != null){
            dump.close();
        }


        logInfo(methodName, "Exiting: " + methodName);
    }

    /**
     * Case: ZipCachingIntrospector.txt exists in the archived server dump
     * 
     * @throws Exception
     */
    @Test
    public void testServerDumpIncludesZipReader() throws Exception{
        String methodName = "testServerDumpIncludesZipReader";
        logInfo(methodName, "Entering: " + methodName);

        //make sure in the dump archive the file for the zip caching introspector exists
        Assert.assertTrue(DumpArchive.ZIP_CACHING_INTROSPECTOR_FILE_NAME + " was not found in archive: " + dump.getName(), dump.doesZipCachingIntrospectorDumpExist());

        logInfo(methodName, "Exiting: " + methodName);
    }

    /**
     * Case: ZipCachingIntrospector.txt contains entries for each of the applications added to server
     * 
     * @throws Exception
     */
    @Test
    public void testZipReaperDumpContainsAllZips() throws Exception{
        String methodName = "testZipReaperDumpContainsAllZips";
        logInfo(methodName, "Entering: " + methodName);

        //make sure in the dump archive the file for the zip caching introspector exists
        Assert.assertTrue(DumpArchive.ZIP_CACHING_INTROSPECTOR_FILE_NAME + " was not found in archive: " + dump.getName(), dump.doesZipCachingIntrospectorDumpExist());

        //make an introspection output object with the stream of the Zip Caching Introspector output file
        ZipCachingIntrospectorOutput dumpOutput = new ZipCachingIntrospectorOutput(dump.getZipCachingDumpStream()) ;
        
        //make sure there is output for the ZipFileHandles
        Assert.assertNotNull("Zip Reaper Introspector Output does not have ZipFileHandle section",dumpOutput.getActiveAndCachedZipFileHandles());


        //get all the file names for the ZipFileData introspections
        List<String> archiveHandleNames = dumpOutput.getZipHandleArchiveNames();
        List<String> lostArchives = new LinkedList<String>();

        logInfo(methodName, String.format("List of archive names from zip file handles in output: %s",archiveHandleNames.toString()));

        //remove all the expected filenames
        for(String testArchiveName: archivesAddedToServer){
            logInfo(methodName, String.format("Removing [%s]",testArchiveName));
            //remove the expected archive name from the list of output archive names
            if(archiveHandleNames.remove(testArchiveName) == false){
                //if there was a failure to remove then the output is missing an expected archive
                lostArchives.add(testArchiveName);
            }
        }

        //assert that there aren't any missing expected archives from the output
        String verifyNoLostArchivesMsg = String.format("Introspector output is missing archives: %s", lostArchives.toString());
        Assert.assertTrue(verifyNoLostArchivesMsg, lostArchives.isEmpty());

        //assert there aren't any unexpected ZipFileHandle introspections
        //fail message prints out the expected and unexpected file names found in the introspection
        String verifyAppMsg = String.format("Zip File Handle Introspection should only include %s but %s was also found", Arrays.toString(archivesAddedToServer) , archiveHandleNames.toString() );
        Assert.assertTrue(verifyAppMsg,archiveHandleNames.isEmpty());

        logInfo(methodName, "Exiting: " + methodName);
    }

    /**
     * Case: Output of ZipCachingIntrospector should not be waiting if there are zip files in the pending queue
     * 
     * @throws Exception
     */
    @Test
    public void testNotWaitingWithNonEmptyQueue() throws Exception{
        String methodName = "testNotWaitingWithNonEmptyQueue";
        logInfo(methodName, "Entering: " + methodName);

        //make sure in the dump archive the file for the zip caching introspector exists
        Assert.assertTrue(DumpArchive.ZIP_CACHING_INTROSPECTOR_FILE_NAME + " was not found in archive: " + dump.getName(), dump.doesZipCachingIntrospectorDumpExist());

        //make an introspection output object with the stream of the Zip Caching Introspector output file
        ZipCachingIntrospectorOutput dumpOutput = new ZipCachingIntrospectorOutput(dump.getZipCachingDumpStream()) ;

        
        //get the value of the the ZipReaperThread
        String reaperState = dumpOutput.getZipReaperThreadState();
        
        //get the value of the runner next delay
        String runnerDelay = dumpOutput.getZipReaperRunnerDelay();
        
        //state must be waiting and it must be waiting indefinitly for test to be applicable
        if(reaperState.equals("WAITING") && runnerDelay.equals("INDEFINITE")){
            //pending and active must be empty so the value in the dump output should be null
            Assert.assertNull("The Active and Pending queues must be empty for the Zip Reaper to be in an indefinite wait", dumpOutput.getActiveAndPendingIntrospection());

        }
        else{
            //The reaper isn't in a waiting state so no reason to check if the queue is empty
            logInfo(methodName, "Test not applicable, reaper is not in a waiting state and waiting indefinitely");
        }

        logInfo(methodName, "Exiting: " + methodName);
    }

    /**
     * Case: Verify for each ZipFileData object the number of times opened is equal to #(Pending -> open) + #(Closed -> open)
     * 
     * @throws Exception
     */
    @Test
    public void testZipFileToOpenCorrect() throws Exception{
        String methodName = "testZipFileToOpenCorrect";
        logInfo(methodName, "Entering: " + methodName);

        //make sure in the dump archive the file for the zip caching introspector exists
        Assert.assertTrue(DumpArchive.ZIP_CACHING_INTROSPECTOR_FILE_NAME + " was not found in archive: " + dump.getName(), dump.doesZipCachingIntrospectorDumpExist());

        //make an introspection output object with the stream of the Zip Caching Introspector output file
        ZipCachingIntrospectorOutput dumpOutput = new ZipCachingIntrospectorOutput(dump.getZipCachingDumpStream());

         //store the raw output for each ZipFileData introspection
        List<String> zipFileDataIntrospections = dumpOutput.getAllZipFileDataIntrospections();

        

        //if there are zip file data introspections to verify
        if(!zipFileDataIntrospections.isEmpty()){

            logInfo(methodName, zipFileDataIntrospections.toString());

            /////////////////////////////////////////////////////////
            //for each of the zip file data introspections
            for(String zipFileDataIntrospection: zipFileDataIntrospections){

                //initilalize variables to be compared for test
                int timesOpened = -1, pendingToOpen = -1, closeToOpen = -1;
                String failmsg = "";
                String zipFileName = zipFileDataIntrospection.split("\n")[0].trim();
                Pattern p = Pattern.compile("\\d+");

                logInfo(methodName, "Checking: " + zipFileName);

                //for each line in the ZipFileData introspection
                for(String introspectLine : zipFileDataIntrospection.split("\n")){

                    //if the line contains keyword for value then grab the value from the introspection
                    if(introspectLine.contains("Open Requests:")){
                        
                        //will match the digits in the open requests line
                        Matcher m = p.matcher(introspectLine);

                        //set the value of timesOpened
                        if(m.find()){
                            timesOpened = Integer.parseInt(m.group().trim());
                        }
                        else{
                            //case where there isn't a value for Open Requests in introspection
                            failmsg = String.format("Could not find the value of open requets for [%s] introspection", zipFileName);
                            Assert.fail(failmsg);
                        }
                        
                    }

                    //"to Open:" is for both the pending to open and the close to open, assign them in order
                    if(introspectLine.contains("to Open:")){
                        Matcher m = p.matcher(introspectLine);
                        int count;
                        if(m.find()){
                            //m.group() will pull the count value first then the time values after
                            //set pendingToOpen
                            if(pendingToOpen == -1){
                                pendingToOpen = Integer.parseInt(m.group());

                            }
                            //set closeToOpen
                            else{
                                closeToOpen = Integer.parseInt(m.group());
                            }
                        }
                        else{
                            //case where there isn't a value for 'to Open' in the introspection
                            failmsg = String.format("Could not find the value of 'to open' movement for [%s] introspection", zipFileName);
                            Assert.fail(failmsg);
                        }

                    }

                }

                //if any of the values are still -1 then assume the value could not be found and throw a failure
                if(timesOpened == -1){
                    failmsg = String.format("Could not find the number of times [%s] has been opened",zipFileName);
                    Assert.fail(failmsg);
                }

                if(pendingToOpen == -1){
                    failmsg = String.format("Could not find the number of times [%s} has been moved from pending to open", zipFileName);
                    Assert.fail(failmsg);
                }

                if(closeToOpen == -1){
                    failmsg = String.format("Could not find the number of time [%s] has been moved from closed to open", zipFileName);
                    Assert.fail(failmsg);

                }

                //check that the number of time from pending to open and close to open are equal to the number of times it was opened
                failmsg = String.format("Summation of (pending to open) + (close to open) did not equal (times opend) for [%s]",zipFileName);
                Assert.assertEquals(failmsg,timesOpened,(pendingToOpen + closeToOpen));
            }

        }
        else{//should not get here
            
            logInfo(methodName, "Test not applicible, No ZipFileData introspections were found in dump");
            Assert.fail("Could not run test, no ZipFileData introspections were found in dump");
        }

        logInfo(methodName, "Exiting: " + methodName);
    }

}