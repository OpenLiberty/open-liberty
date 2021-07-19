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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.junit.Test;
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
public class AsyncSendCAPITest  {

	 private static Class<?> c = AsyncSendCAPITest.class;
	
	 
	 private static LibertyClient client = LibertyClientFactory.getLibertyClient("AsyncSendCAPITest_com.ibm.ws.clientcontainer.fat.ClientContainerClientCAPI");
	 private static LibertyServer server = LibertyServerFactory.getLibertyServer("AsyncSendCAPITest_com.ibm.ws.clientcontainer.fat.ClientContainerServer");
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
	
	
	@Test
	public void testAsyncSendCAPI()throws Exception {
        String lookForString = "Test Case: testAsyncSendCAPI Passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
       
    }
    
	@Test
	public void testExceptionMessageThreshhold1()throws Exception {
        String lookForString = "Test case : testExceptionMessageThreshhold1 passed. ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testAsyncSendException2()throws Exception {
        String lookForString = "Test Case : testAsyncSendException2 passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testMessageOrderingSingleJMSProducer3()throws Exception {
        String lookForString = "Test case :testMessageOrderingSingleJMSProducer3 passed";
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
	public void testMessageOrderingMultipleSession1() throws Exception {
        String lookForString = "Test case :testMessageOrderingMultipleSession1 passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testcloseSession2() throws Exception {
        String lookForString = "Test case : testCloseSession2 passed. ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testcloseConnection3() throws Exception {
        String lookForString = "Test case : testCloseConnection3 passed. ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testcallingNativeSession() throws Exception {
        String lookForString = "Starting Test :testCallingNativeSession";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testAsyncSendIDE2() throws Exception {
        String lookForString = "Test case :testAsyncSendIDE2 passed.";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testAsyncSendCLN() throws Exception {
        String lookForString = "Test case : testAsyncSendCLN passed";
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
	public void testcallingNativeSession_unrelatedSession3() throws Exception {
        String lookForString = "Test case : testCallingNativeContext_unrelatedContext passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testOnCompletionOnException2() throws Exception {
        String lookForString = "Test case : testOnCompletionOnException2 passed ";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testSessioninCallBackMethodOC1() throws Exception {
        String lookForString = "Test Case: testSessioninCallBackMethodOC1 Passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	//@Test
	public void testSessioninCallBackMethodOE() throws Exception {
        String lookForString = "Test case: testSessioninCallBackMethodOE passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testTimetoLiveVariation() throws Exception {
        String lookForString = "Test case :testTimetoLiveVariation passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	
	@Test
	public void testPriorityVariation() throws Exception {
        String lookForString = "Test case : testPriorityVariation passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	@Test
	public void testPriorityVariation_negative() throws Exception {
        String lookForString = "Test case : testPriorityVariation_negative passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testInvalidDeliveryMode() throws Exception {
        String lookForString = "Test case :testInvalidDeliveryMode passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	@Test
	public void testNullEmptyMessage() throws Exception {
        String lookForString = "Test case : testNullEmptyMessage passed";
        List<String> strings = client.findStringsInCopiedLogs(lookForString);
        Log.info(c, lookForString, "Found in logs: " + strings);
        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
        
    }
	
	
	
    public static void setUpShirnkWrap() throws Exception {

        Archive TestAsyncSendCAPIAppClientjar = ShrinkWrap.create(JavaArchive.class, "TestAsyncSendCAPIAppClient.jar")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListenerVariationCAPI")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.GetJMSResourcesCAPI")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListenerVariationURCAPI")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListenerCAPI")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListenerSessionCAPI")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.ClientMainCAPI")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCompletionListener_MessageOrderingCAPI")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.TestCases_AsyncSendCAPI")
            .add(new FileAsset(new File("test-applications//TestAsyncSendCAPIAppClient.jar/resources/META-INF/MANIFEST.MF")), "META-INF/MANIFEST.MF")
            .add(new FileAsset(new File("test-applications//TestAsyncSendCAPIAppClient.jar/resources/META-INF/application-client.xml")), "META-INF/application-client.xml");


        Archive TestAsyncSendCAPIAppear = ShrinkWrap.create(EnterpriseArchive.class, "TestAsyncSendCAPIApp.ear")
            .add(new FileAsset(new File("test-applications//TestAsyncSendCAPIApp.ear/resources/META-INF/application.xml")), "META-INF/application.xml")
            .add(new FileAsset(new File("test-applications//TestAsyncSendCAPIApp.ear/resources/META-INF/permissions.xml")), "META-INF/permissions.xml")
            .addAsModule(TestAsyncSendCAPIAppClientjar);

        ShrinkHelper.exportAppToClient(client, TestAsyncSendCAPIAppear, OVERWRITE);
    }
}
