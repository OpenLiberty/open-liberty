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

import com.ibm.websphere.simplicity.ShrinkHelper;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@RunWith(FATRunner.class)
public class FATReaperVerification{

    @Server(FATReaperVerification.serverName)
    public static LibertyServer server;
    public static final String serverName = "com.ibm.ws.artifact.zipReaper";
    private static final String[] appNames = {
        "jarneeder.war",
        "testServlet1",
        "testServlet2"
    };
    private static final int SECONDS_WAITING_FOR_DUMP = 30;
    private static final int ATTEMPTS_TO_RETRY  = 2;
    private static DumpArchive dump = null;

    private static void logInfo(String methodName, String outputString){
        FATLogging.info(FATReaperVerification.class, methodName, outputString);
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

        logInfo("methodName", String.format("Sent dump action to server going to sleep for %d seconds", SECONDS_WAITING_FOR_DUMP));

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


    public void setUp() throws Exception{
        String methodName = "setUp";
        logInfo(methodName, "Entering: " + methodName);

        Assert.assertNotNull("Server reference is null in test class", server);

        //iterate over servlets and add them to the server
        for(String appName: appNames){
            ShrinkHelper.defaultApp(server, appName, "com.ibm.ws.artifact.fat.servlet");
        }

        if(!server.isStarted()){
            server.startServer();
        }

        logInfo(methodName, "Exiting: " + methodName);
    }


    public void tearDown() throws Exception{
        String methodName = "tearDown";
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

        Assert.assertTrue(DumpArchive.ZIP_CACHING_INTROSPECTOR_FILE_NAME + " was not found in archive: " + dump.getName(), dump.doesZipCachingIntrospectorDumpExist());

        ZipCachingIntrospectorOutput dumpOutput = new ZipCachingIntrospectorOutput(dump.getZipCachingDumpStream()) ;
        
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName,dumpOutput.introspectorDescription);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName,dumpOutput.entryCacheSettings);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName,dumpOutput.zipReaperSettings);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName,dumpOutput.handleIntrospection);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName,dumpOutput.zipEntryCache);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName,dumpOutput.zipReaperValues);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName,dumpOutput.activeAndPendingIntrospection);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName, dumpOutput.pendingQuickIntrospection);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName, dumpOutput.pendingSlowIntrospection);
        logInfo(methodName,"--------------------------------------");
        logInfo(methodName, dumpOutput.completedIntrospection);
        logInfo(methodName,"--------------------------------------");


        logInfo(methodName, "Exiting: " + methodName);
    }

    
}