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

import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class ClientIDTest {

	 private static Class<?> c = ClientIDTest.class;
	 
	 private static LibertyClient client = LibertyClientFactory.getLibertyClient("ClientIDTest_com.ibm.ws.clientcontainer.fat.ClientContainerClientID");
	 private static LibertyServer server = LibertyServerFactory.getLibertyServer("ClientIDTest_com.ibm.ws.clientcontainer.fat.ClientContainerServer");
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
		public void testSetClientID()throws Exception {
	        String lookForString = "Test Case: testSetClientID Passed";
	        List<String> strings = client.findStringsInCopiedLogs(lookForString);
	        Log.info(c, lookForString, "Found in logs: " + strings);
	        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
	        
	    }
		
		@Test
		public void testSetClientIDID()throws Exception {
	        String lookForString = "Test Case: testSetClientIDID Passed";
	        List<String> strings = client.findStringsInCopiedLogs(lookForString);
	        Log.info(c, lookForString, "Found in logs: " + strings);
	        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
	        
	    }
		
		
		@Test
		public void testDurableSubscriberClientIDEmpty()throws Exception {
	        String lookForString = "Test Case: testDurableSubscriberClientIDEmpty Passed";
	        List<String> strings = client.findStringsInCopiedLogs(lookForString);
	        Log.info(c, lookForString, "Found in logs: " + strings);
	        assertTrue("Did not find expected method message " + lookForString, strings != null && strings.size() >= 1);
	        
	    }
    public static void setUpShirnkWrap() throws Exception {

        Archive TestClientIDAppClientjar = ShrinkWrap.create(JavaArchive.class, "TestClientIDAppClient.jar")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.ClientIDTestcase")
            .addClass("com.ibm.ws.messaging.clientcontainer.fat.ClientMainClientID")
            .add(new FileAsset(new File("test-applications//TestClientIDAppClient.jar/resources/META-INF/MANIFEST.MF")), "META-INF/MANIFEST.MF")
            .add(new FileAsset(new File("test-applications//TestClientIDAppClient.jar/resources/META-INF/application-client.xml")), "META-INF/application-client.xml");


        Archive TestClientIDAppear = ShrinkWrap.create(EnterpriseArchive.class, "TestClientIDApp.ear")
            .add(new FileAsset(new File("test-applications//TestClientIDApp.ear/resources/META-INF/application.xml")), "META-INF/application.xml")
            .add(new FileAsset(new File("test-applications//TestClientIDApp.ear/resources/META-INF/permissions.xml")), "META-INF/permissions.xml")
            .addAsModule(TestClientIDAppClientjar);

        ShrinkHelper.exportAppToClient(client, TestClientIDAppear, OVERWRITE);
    }
}
