/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.junit.BeforeClass;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import com.ibm.websphere.simplicity.ShrinkHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;


@Mode(TestMode.FULL)
public class AsyncSendSAPITest  {

	 private static Class<?> c = AsyncSendSAPITest.class;
	 
	 private static LibertyClient client = LibertyClientFactory.getLibertyClient("AsyncSendSAPITest_com.ibm.ws.clientcontainer.fat.ClientContainerClient");
	 private static LibertyServer server = LibertyServerFactory.getLibertyServer("AsyncSendSAPITest_com.ibm.ws.clientcontainer.fat.ClientContainerServer");
	 @BeforeClass
	    public static void beforeClass() throws Exception {
            setUpShirnkWrap();

		 server.copyFileToLibertyInstallRoot("lib/features",
                 "features/testjmsinternals-1.0.mf");
	        ProgramOutput po = server.startServer();
	        assertEquals("server did not start correctly", 0, po.getReturnCode());
	        
	        client.startClient();
	    }
	    
	    @AfterClass
	    public static void afterClass() throws Exception {
	        server.stopServer();
	    }
	
	@Mode(TestMode.LITE)
	@Test
	public void testSetAsyncOn()throws Exception {
        String lookForString = "Test Case: testSetAsyncOn Passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
       
    }
    
	
	@Test
	public void testSetAsyncOff()throws Exception {
        String lookForString = "Test Case: testSetAsyncOff Passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	

	@Test
	public void testExceptionMessageThreshhold()throws Exception {
        String lookForString = "Test case : testExceptionMessageThreshhold passed. ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testExceptionNEQueue()throws Exception {
        String lookForString = "Test case : testExceptionNEQueue passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testMessageOrderingSingleJMSProducer() throws Exception {
        String lookForString = "Test case :testMessageOrderingSingleJMSProducer passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testMessageOrderingMultipleJMSProducers() throws Exception {
        String lookForString = "Test case :testMessageOrderingMultipleJMSProducers passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testMessageOrderingSyncAsyncMix() throws Exception {
        String lookForString = "Test case :testMessageOrderingSyncAsyncMix passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testMessageOrderingMultipleJMSContexts() throws Exception {
        String lookForString = "Test case :testMessageOrderingMultipleContexts passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testClose() throws Exception {
        String lookForString = "Test case : testClose passed. ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testCommit() throws Exception {
        String lookForString = "Test case : testCommit passed. ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testRollBack() throws Exception {
        String lookForString = "Test case : testRollBack passed. ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testCallingNativeContext() throws Exception {
        String lookForString = "Test case :testCallingNativeContext passed.";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testStreamMessageTypesAOC() throws Exception {
        String lookForString = "Test case : testStreamMessageTypesAOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testObjectMessageTypesAOC() throws Exception {
        String lookForString = "Test case : testObjectMessageTypesAOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testMapMessageTypesAOC() throws Exception {
        String lookForString = "Test case : testMapMessageTypesAOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testBytesMessageTypesAOC() throws Exception {
        String lookForString = "Test case : testBytesMessageTypesAOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testTextMessageTypesAOC() throws Exception {
        String lookForString = "Test case : testTextMessageTypesAOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testStreamMessageTypesBOC() throws Exception {
        String lookForString = "Test case : testStreamMessageTypesBOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	
	@Test
	public void testObjectMessageTypesBOC() throws Exception {
        String lookForString = "Test case : testObjectMessageTypesBOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testMapMessageTypesBOC() throws Exception {
        String lookForString = "Test case : testStreamMessageTypesBOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testBytesMessageTypesBOC() throws Exception {
        String lookForString = "Test case : testBytesMessageTypesBOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testTextMessageTypesBOC() throws Exception {
        String lookForString = "Test case : testTextMessageTypesBOC passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testMessageOnException() throws Exception {
        String lookForString = "Test case : testMessageOnException passed.";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testOnCompletionOnException() throws Exception {
        String lookForString = "Test case : testOnCompletionOnException passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testCallingNativeContext_unrelatedContext() throws Exception {
        String lookForString = "Test case : testCallingNativeContext_unrelatedContext passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testGetAsync() throws Exception {
        String lookForString = "Test case :testGetAsync passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testJMSContextinCallBackMethodOC() throws Exception {
        String lookForString = "Test Case: testJMSContextinCallBackMethodOC Passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testAsyncSendNDE1() throws Exception {
        String lookForString = "Test case :testAsyncSendNDE1 passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	@Test
	public void testAsyncSendNDE2() throws Exception {
        String lookForString = "Test case :testAsyncSendNDE2 passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	@Test
	public void testAsyncSendNDE3() throws Exception {
        String lookForString = "Test case :testAsyncSendNDE3 passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	@Test
	public void testAsyncSendNDE4() throws Exception {
        String lookForString = "Test case :testAsyncSendNDE4 passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testDCF() throws Exception {
        String lookForString = "Test case :testDCF passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testDCFVariation() throws Exception {
        String lookForString = "Test case :testDCFVariation passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
    public static void setUpShirnkWrap() throws Exception {

        Archive TestAsyncSendAppClientjar = ShrinkWrap.create(JavaArchive.class, "TestAsyncSendAppClient.jar")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListenerVariation")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListenerVariationUR")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.ClientMain")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListener")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCases_AsyncSend")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListenerContext")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.GetJMSResources")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListener_MessageOrdering")
            .add(new FileAsset(new File("test-applications//TestAsyncSendAppClient.jar/resources/META-INF/MANIFEST.MF")), "META-INF/MANIFEST.MF")
            .add(new FileAsset(new File("test-applications//TestAsyncSendAppClient.jar/resources/META-INF/application-client.xml")), "META-INF/application-client.xml");


        Archive TestAsyncSendAppear = ShrinkWrap.create(EnterpriseArchive.class, "TestAsyncSendApp.ear")
            .add(new FileAsset(new File("test-applications//TestAsyncSendApp.ear/resources/META-INF/application.xml")), "META-INF/application.xml")
            .add(new FileAsset(new File("test-applications//TestAsyncSendApp.ear/resources/META-INF/permissions.xml")), "META-INF/permissions.xml")
            .addAsModule(TestAsyncSendAppClientjar);

        ShrinkHelper.exportAppToClient(client, TestAsyncSendAppear, OVERWRITE);
    }
}
