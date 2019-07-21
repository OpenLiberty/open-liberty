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

import java.beans.Transient;
import java.io.File;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Assert;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.impl.LibertyServer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

@RunWith(FATRunner.class)
public class FATReaperMultipleIntrospectionTest{

    @Server(FATReaperMultipleIntrospectionTest.serverName)
    public static LibertyServer server;
    public static final String serverName = "com.ibm.ws.artifact.zipIntrospections";
    private static final ExecutorService pool = Executors.newFixedThreadPool(3);

    private static class GETCallable implements Callable<String>{

        LibertyServer server;
        String applicationName;
        int holdFor;

        public GETCallable(LibertyServer server, String applicationName, int holdFor){
            this.server= server;
            this.applicationName = applicationName;
            this.holdFor = holdFor;
        }

        @Override
        public String call() throws Exception {
            if(holdFor > 0){
                return HttpUtils.getHttpResponseAsString(server, applicationName + "/?hold=" + holdFor).trim();
            }
            else{
                return HttpUtils.getHttpResponseAsString(server, applicationName + "/").trim();
            }
            
        }

    }

    private static Future<String> asyncGET(LibertyServer server, String applicationName){
        return asyncGET(server, applicationName, 0);
    }

    private static Future<String> asyncGET(LibertyServer server, String applicationName, int hold){
        //HttpUtils.getHttpResponseAsString(server, "app1/?hold=30")
        GETCallable thisCall = new GETCallable(server, applicationName, hold);
        return pool.submit(thisCall);

    } 

    private static void logInfo(String methodName, String outputString){
        FATLogging.info(FATReaperMultipleIntrospectionTest.class, methodName, outputString);
    }

    private static WebArchive makeHoldingWebModule(String moduleName) throws Exception{
        
        if(!moduleName.endsWith(".war")){
            moduleName = moduleName.concat(".war");
        }

        WebArchive retValue = ShrinkWrap.create(WebArchive.class, moduleName);

        //String packageOfHolderServletClass = "com.ibm.ws.artifact.fat.zip";

        Class holdingServletClass = FATReaperMultipleIntrospectionTest.class.getClassLoader().loadClass("com.ibm.ws.artifact.fat.zip.HoldingServlet");


        if(holdingServletClass == null){
            //should be unreachable since loadClass will throw ClassNotFoundException if class cannot be loaded
            return null;
        }

        retValue.addClass(holdingServletClass);

        String holdingAppResourceDir = "test-applications/HoldingServlet/resources/";

        //could potentially throw Security Exception see java.io.File.exists();
        if((new File(holdingAppResourceDir)).exists()){
            retValue = (WebArchive) ShrinkHelper.addDirectory(retValue, holdingAppResourceDir);
        }
        else{
            throw new FileSystemNotFoundException(String.format("Direcory [%s] with servlet resources could not be found", holdingAppResourceDir));
        }

        return retValue;
    }

    private static void addWebModuleToServer(LibertyServer server, WebArchive application) throws Exception{
        String appName = application.getName();
        if(appName.endsWith(".war")){
            appName = appName.substring(0, appName.length() - 4);
        }
        ShrinkHelper.exportAppToServer(server, application);
        server.addInstalledAppForValidation(appName);
        ServerConfiguration startingConfig = server.getServerConfiguration();
        startingConfig.addApplication(appName, server.getServerRoot() + "/apps/" + application.getName(), "war");
        server.updateServerConfiguration(startingConfig);
        
    }

    @BeforeClass
    public static void setUpClass() throws Exception{
        server.saveServerConfiguration();
    }

    @After
    public void tearDown() throws Exception{
        String methodName = "tearDown";
        if(server.isStarted()){
            server.stopServer();
        }

        //remove the apps on the server
        server.deleteDirectoryFromLibertyServerRoot("apps");

        //recreate the apps directory in the server
        if(new File(server.getServerRoot() + "/apps").mkdir()){
            logInfo(methodName, String.format("Recreated the apps directory for %s",server.getServerName()));
        }
        
        
        server.removeAllInstalledAppsForValidation();
        logInfo(methodName, "Removed all installed apps for validation");

        //set the original server configuration
        server.restoreServerConfiguration();
        logInfo(methodName, "Restored the original server configuration");
    }

    /**
     * Case: Verify the correct number of archives are handled by the reaper when adding many applications
     * 
     * @throws Exception
     */
    @Test
    public void checkManyApps() throws Exception{
        String methodName = "checkManyApps";

        logInfo(methodName, "Entering: " + methodName);

        List<String> appsToAdd = new LinkedList<String>();
        int numberOfApps = 45;
        for(int i = 1; i <= numberOfApps; ++i){
            appsToAdd.add("app" + i);
        }
        
        for(String appName: appsToAdd){
            WebArchive temp = makeHoldingWebModule(appName);
            addWebModuleToServer(server,temp); 
        }

        server.startServer();

        Thread.sleep(5000);

        server.executeServerScript("dump",null);

        DumpArchive firstDump = DumpArchive.getMostRecentDumpArchive(server);

        if(! firstDump.doesZipCachingIntrospectorDumpExist()){
            Assert.fail("Could not find introspector output in dump archive");
        }

        ZipCachingIntrospectorOutput output = new ZipCachingIntrospectorOutput(firstDump.getZipCachingDumpStream());

        List<String> archiveNames = output.getZipHandleArchiveNames();
        logInfo(methodName, archiveNames.toString());
        for(int i = 1; i <= numberOfApps; ++i){
            //check each app name exists and remove it
            if(archiveNames.remove("app" + i + ".war")){
                //should be able to remove a test jar since there is one bundled with each application
                if(!archiveNames.remove("TestJar.jar")){
                    Assert.fail("Missing a TestJar.jar archive in one of the applications added during test");
                } 

            }
            else{
                Assert.fail(String.format("Failed to find application %s in the ZipCachingIntrospector output of %s", "app"+i, firstDump.getName()));
            }
        }

        logInfo(methodName, "Exiting: " + methodName);
    }
    
    /**
     * Case: Verify archives are moved between open closed states
     * 
     * @throws Exception
     * 
     */
    @Test
    public void testMoveToOpen() throws Exception{
        String methodName = "testMoveToOpen";
        List<String> appsToAdd = new LinkedList<String>();
        int numberOfApps = 2;

        for(int i = 1; i <= numberOfApps; ++i){
            appsToAdd.add("app" + i);
        }
        
        for(String appName: appsToAdd){
            WebArchive temp = makeHoldingWebModule(appName);
            addWebModuleToServer(server,temp); 
        }

        server.startServer();

        Thread.sleep(5000);

        DumpArchive recent;
        ZipCachingIntrospectorOutput recentOutput;
        List<String> recentActiveAndPending;
        String assertMsg;
        int secondsToWait = 90;

        for(String appName : appsToAdd){
            //make and pull in the dump which should have everything closed
            server.executeServerScript("dump",null);
            recent = DumpArchive.getMostRecentDumpArchive(server);
            recentOutput = new ZipCachingIntrospectorOutput(recent.getZipCachingDumpStream());
            recentActiveAndPending = recentOutput.getOpenAndActiveArchiveNames();

            //first dump should have everything fully closed
            assertMsg = String.format("Dump [%s] is not supposed to have any active and pending archives. Found %s",recent.getName(),recentActiveAndPending.toString());
            Assert.assertTrue(assertMsg,recentActiveAndPending.isEmpty());

            //hold the app resources open
            Future<String> response = asyncGET(server,appName,secondsToWait);

            //get another dump, supposed to have the current app resources open
            server.executeServerScript("dump", null);

            //make sure the resources were help during the dump
            assertMsg = String.format("Response from the holding servlet was not a success");
            String responseMsg = response.get();
            String expected = "Waited for " + secondsToWait * 1000 + " ms";
            Assert.assertEquals(assertMsg,expected,responseMsg);

            //pull in the dump which has the newly opened resources
            recent = DumpArchive.getMostRecentDumpArchive(server);
            recentOutput = new ZipCachingIntrospectorOutput(recent.getZipCachingDumpStream());
            recentActiveAndPending = recentOutput.getOpenAndActiveArchiveNames();

            //verify that the two expected archives are open
            assertMsg = String.format("Expected 2 archives to be open in dump [%s]",recent.getName());
            Assert.assertFalse(assertMsg, recentActiveAndPending.isEmpty());

            //last dump should have the archives back to fully closed
            server.executeServerScript("dump", null);

            //pull in the information on the last dump
            recent = DumpArchive.getMostRecentDumpArchive(server);
            recentOutput = new ZipCachingIntrospectorOutput(recent.getZipCachingDumpStream());
            recentActiveAndPending = recentOutput.getOpenAndActiveArchiveNames();

            //all the archives should be closed in the last dump
            assertMsg = String.format("Expected 0 archives to be open in dump [%s] but found %s", recent.getName(), recentActiveAndPending.toString());
            Assert.assertTrue(assertMsg,recentActiveAndPending.isEmpty());

            logInfo(methodName, "Finished test for " + appName);
        }
        
    }



}