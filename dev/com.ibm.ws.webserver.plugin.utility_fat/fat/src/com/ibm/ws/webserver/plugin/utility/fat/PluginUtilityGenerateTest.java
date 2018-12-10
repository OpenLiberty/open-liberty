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
package com.ibm.ws.webserver.plugin.utility.fat;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;


/**
 * This class will test the Plugin utility generate functions where the inputs provided are
 * valid for connection to an app server.
 */
@RunWith(FATRunner.class)
public class PluginUtilityGenerateTest {

    private static final Class<?> c = PluginUtilityGenerateTest.class;

    static LibertyServer localAccessServer = LibertyServerFactory.getLibertyServer("localAccessServer");
    static LibertyServer remoteAccessServer = LibertyServerFactory.getLibertyServer("remoteAccessServer");

    
    public static final String SERVER_USER = "mrAdmin";
    public static final String SERVER_PASSWORD = "mrPassword";
    
    public static final String PLUGIN_CFG_FILENAME = "plugin-cfg.xml";
    
    @BeforeClass
    public static void setUp() throws Exception {

        localAccessServer.startServer();

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed",
                localAccessServer.waitForStringInLog("CWWKF0011I"));
        
        

        remoteAccessServer.setHttpDefaultPort(Integer.getInteger("HTTP_secondary"));
        remoteAccessServer.setHttpDefaultSecurePort(Integer.getInteger("HTTP_secondary.secure"));
        
        remoteAccessServer.startServer();

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed",
                remoteAccessServer.waitForStringInLog("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (localAccessServer.isStarted()) {
                localAccessServer.dumpServer("localAccessServer");
                localAccessServer.stopServer();
            }
            if (remoteAccessServer.isStarted()) {
                remoteAccessServer.dumpServer("remoteAccessServer");
                remoteAccessServer.stopServer();
            }
        } catch (Exception e) {
            Log.info(c, "tearDown", "Fail to stop collective controller: " + e);
        }   
    }

    /*
     * Test plugin-cfg.xml generation for a local app server where no targetPath is specified
     * so file is copied to current directory
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPluginUtilityLocalGenerate() throws Exception {

        final String methodName = "testPluginUtilityLocalGenerate";
        Log.entering(c, methodName);
        
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,localAccessServer,null);
        
        assertTrue("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateLocalPluginXmlFile(PcfgFile, localAccessServer, null, null));

        Log.exiting(c, methodName);
    }
    
    /*
     * Test plugin-cfg.xml generation for a local app server where the targetPath is specified
     * as a directory.
     * In cluster tests the TragetDir test is LITE and the TargetFile test is FULL, so doing the opposite in this bucket.
     */    
    @Mode(TestMode.FULL)
    @Test
    public void testPluginUtilityLocalGenerateTargetDir() throws Exception {

        final String methodName = "testPluginUtilityLocalGenerateTargetDir";
        Log.entering(c, methodName);
        
        String targetPath = localAccessServer.getInstallRoot() + "/usr/servers/" + localAccessServer.getServerName() + "/local";
        
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,localAccessServer,targetPath);
        
        assertTrue("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateLocalPluginXmlFile(PcfgFile, localAccessServer, null, targetPath));

        Log.exiting(c, methodName);
    }
  
    /*
     * Test plugin-cfg.xml generation for a local app server where the targetPath is specified
     * as a file
     * In cluster tests the TragetDir test is LITE and the TargetFile test is FULL, so doing the opposite in this bucket.
     */    
    @Mode(TestMode.LITE)
    @Test
    public void testPluginUtilityLocalGenerateTargetFile() throws Exception {
        final String methodName = "testPluginUtilityLocalGenerateTargetFile";
        Log.entering(c, methodName);
        
        String targetPath = localAccessServer.getInstallRoot() + "/usr/servers/" + localAccessServer.getServerName() + "/local/test_"+PLUGIN_CFG_FILENAME;
        
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,localAccessServer,targetPath);
        
        assertTrue("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateLocalPluginXmlFile(PcfgFile, localAccessServer, null, targetPath));

        Log.exiting(c, methodName);
    }
    
    /*
     * Test a request to get a merged cluster plugin-cfg.xml on a local app server. An error expected because the server is 
     * is not a collective controller.
     */    
    @Mode(TestMode.FULL)
    @Test
    public void testPluginUtilityLocalBadClusterRequest() throws Exception {
        final String methodName = "testPluginUtilityLocalGenerateTargetFile";
        Log.entering(c, methodName);
                
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,localAccessServer,null);
        
        localAccessServer.addIgnoredErrors(Arrays.asList("CWWKU0101E"));
        
        assertFalse("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateLocalPluginXmlFile(PcfgFile, localAccessServer, "myCluster", null));
        
        //check for expected error messaqe
        assertNotNull("CWWKU0101E ws not found in the logs", localAccessServer.waitForStringInLog("CWWKU0101E"));
        
        Log.exiting(c, methodName);
    }

    /*
     * Test plugin-cfg.xml generation for a remote app server where no targetPath is specified
     * so file is copied to current directory
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPluginUtilityRemoteGenerate() throws Exception {

        final String methodName = "testPluginUtilityRemoteGenerate";
        Log.entering(c, methodName);
        
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,remoteAccessServer,null);
        
        assertTrue("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateRemotePluginXmlFile(PcfgFile, remoteAccessServer, null, null));

        Log.exiting(c, methodName);
    }
    
    /*
     * Test plugin-cfg.xml generation for a remote app server where the targetPath is specified
     * as a directory
     * In cluster tests the TragetDir test is LITE and the TargetFile test is FULL, so doing the opposite in this bucket.
     */
    @Mode(TestMode.FULL)
    @Test
    public void testPluginUtilityRemoteGenerateTargetDir() throws Exception {

        final String methodName = "testPluginUtilityGenerateTargetDir";
        Log.entering(c, methodName);
        
        String targetPath = remoteAccessServer.getInstallRoot() + "/usr/servers/" + remoteAccessServer.getServerName() + "/remote";
        
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,remoteAccessServer,targetPath);
        
        assertTrue("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateRemotePluginXmlFile(PcfgFile, remoteAccessServer, null, targetPath));

        Log.exiting(c, methodName);
    }

    /*
     * Test plugin-cfg.xml generation for a remote app server where the targetPath is specified
     * as a file
     * In cluster tests the TragetDir test is LITE and the TargetFile test is FULL, so doing the opposite in this bucket.
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPluginUtilityRemoteGenerateTargetFile() throws Exception {

        final String methodName = "testPluginUtilityGenerateTargeFile";
        Log.entering(c, methodName);
        
        String targetPath = remoteAccessServer.getInstallRoot() + "/usr/servers/" + remoteAccessServer.getServerName() + "/remote/test_"+PLUGIN_CFG_FILENAME;
        
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,remoteAccessServer,targetPath);
        
        assertTrue("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateRemotePluginXmlFile(PcfgFile, remoteAccessServer, null, targetPath));

        Log.exiting(c, methodName);
    }
    
    
    /*
     * Test a request to get a merged cluster plugin-cfg.xml on a remote app server. An error expected because the server is 
     * is not a collective controller.
     */    
    @Mode(TestMode.FULL)
    @Test
    public void testPluginUtilityRemoteBadClusterRequest() throws Exception {
        final String methodName = "testPluginUtilityRemoteBadClusterRequest";
        Log.entering(c, methodName);
                
        File PcfgFile = eraseExistingFiles(PLUGIN_CFG_FILENAME,remoteAccessServer,null);
        
        remoteAccessServer.addIgnoredErrors(Arrays.asList("CWWKU0101E"));
        
        assertFalse("plugin-cfg.xml not created :" + PcfgFile.getAbsolutePath() , this.generateRemotePluginXmlFile(PcfgFile, remoteAccessServer, "myCluster", null));
        
        //check for expected error messaqe
        assertNotNull("CWWKU0101E ws not found in the logs", remoteAccessServer.waitForStringInLog("CWWKU0101E"));
        
        Log.exiting(c, methodName);
    }
    
    /*
     * Test sending a request for a cluster merged plugin-cfg.xml file to a cluster member. The MBean should return false.
     */
    private boolean generateLocalPluginXmlFile(File cfgFile, LibertyServer server, String cluster, String targetPath) throws Exception {

        Log.info(c, "generateLocalPluginXmlFile", "Call Utility to generate config...");

        runCommand(server, server.getServerName(), cluster, targetPath, null);

        return cfgFile.exists();
    }

    /*
     * Test sending a request for a cluster merged plugin-cfg.xml file to a cluster member. The MBean should return false.
     */
    private boolean generateRemotePluginXmlFile(File cfgFile, LibertyServer server, String cluster, String targetPath) throws Exception {

        Log.info(c, "generateRemotePluginXmlFile", "Call Utility to generate config file, targetPath = " + targetPath);
        String serverId = SERVER_USER + ":" + SERVER_PASSWORD + "@" + server.getHostname() + ":" + server.getHttpDefaultSecurePort();

        Properties env = new Properties();
        env.put("JVM_ARGS", "-Dcom.ibm.webserver.plugin.utility.autoAcceptCertificates=true");
        env.put("JAVA_HOME", server.getMachineJavaJDK());

        runCommand(server, serverId, cluster, targetPath, env);

        return cfgFile.exists();
    }

    
    
    private void runCommand(LibertyServer server, String serverId, String cluster,String targetPath, Properties env) throws Exception {

        String pluginUtilityCommand = server.getInstallRoot() + "/bin/pluginUtility";

        ArrayList<String> argList = new ArrayList<String>();
        argList.add("generate");
        argList.add("--server=" + serverId);
        if (cluster!=null) {
            argList.add("--cluster="+cluster);
        }
        if (targetPath != null) {
            argList.add("--targetPath=" + targetPath);
        }

        String[] args = new String[argList.size()];

        ProgramOutput po;
        if (null == env) {
            po = server.getMachine().execute(pluginUtilityCommand, argList.toArray(args));
        } else {
            po = server.getMachine().execute(pluginUtilityCommand, argList.toArray(args), env);
        }

        Log.info(c, "runCommand", "command : " + pluginUtilityCommand);
        for (String arg : args) {
            Log.info(c, "runCommand", "argument :" + arg);
        }

        Log.info(c, "runCommand", "Resulting output : \n" + po.getStdout());

    }

    private File eraseExistingFiles(String fileName, LibertyServer server, String targetPath) {

        // delete an existing generated file if it exists
        if (server != null) {
            String generatedFile = server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/logs/state/" + fileName;
            File genFile = new File(generatedFile);
            if (genFile.exists()) {
                genFile.delete();
            }
        }

        // delete an existing target file if it exists
        File cfgFile;
        if (targetPath == null) {
            cfgFile = new File(server.getInstallRoot() + "/" + fileName);
        } else {
            cfgFile = new File(targetPath);
            if (cfgFile.isDirectory()) {
                cfgFile = new File(targetPath + "/" +fileName);
            }
        }
        if (cfgFile.exists()) {
            cfgFile.delete();
        }
        return cfgFile;
    }

}
