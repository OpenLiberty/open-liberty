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
package com.ibm.ws.messaging.comms.fat;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class WasJmsOutBoundTest {

	 private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("WasJmsOutBoundTest_com.ibm.ws.messaging.comms.WJO.Client");
	 private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("WasJmsOutBoundTest_com.ibm.ws.messaging.comms.WJO.Server");
	 
		 
	 private StringBuilder runInServlet(String test) throws IOException {
	        URL url = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/CommsLP?test=" + test);
	        System.out.println("URL is : " + url.toString());
	        HttpURLConnection con = (HttpURLConnection) url.openConnection();
	        try {
	            con.setDoInput(true);
	            con.setDoOutput(true);
	            con.setUseCaches(false);
	            con.setRequestMethod("GET");

	            InputStream is = con.getInputStream();
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);

	            String sep = System.getProperty("line.separator");
	            StringBuilder lines = new StringBuilder();
	            for (String line = br.readLine(); line != null; line = br.readLine())
	                lines.append(line).append(sep);

	            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
	                fail("Missing success message in output. " + lines);

	            return lines;
	        } finally {
	            con.disconnect();
	        }
	    }

	 
	 @BeforeClass
	    public static void setUpBeforeClass() throws Exception {
            setUpShrinkWrap();

	        
		 	server2.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
		 	server2.copyFileToLibertyServerRoot("resources/security", "serverLTPAKeys/ltpa.keys");
		 	server2.copyFileToLibertyServerRoot("resources/security", "serverLTPAKeys/serverKeynew.jks");
		 	
	        server1.copyFileToLibertyInstallRoot("lib/features", "features/testjmsinternals-1.0.mf");
	        server1.copyFileToLibertyServerRoot("resources/security", "clientLTPAKeys/clientKey.jks");
	        
	        server2.startServer();
	        server1.startServer();
	        
	        String uploadMessage = server1.waitForStringInLog("CWWKF0011I:.*", server1.getMatchingLogFile("trace.log"));
	        assertNotNull("Could not find the server start info message in trace file", uploadMessage);
	        
	        uploadMessage = server2.waitForStringInLog("CWWKF0011I:.*", server2.getMatchingLogFile("trace.log"));
	        assertNotNull("Could not find the server start info message in the trace file", uploadMessage);
	        
	        uploadMessage = server2.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint.*", server2.getMatchingLogFile("trace.log"));
	        assertNotNull("Could not find the SSL port ready message in the trace file", uploadMessage);
	        
	        uploadMessage = server2.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint-ssl.*", server2.getMatchingLogFile("trace.log"));
	        assertNotNull("Could not find the SSL port ready message in the trace file", uploadMessage);
	        
	                             
	    }
	 
	 
	 @AfterClass
	 public static void tearDown() throws Exception {
		 server1.stopServer();
		 server2.stopServer();
	 }
	 
	 @Test
	 public void testWJOSendReceive2LP() throws Exception {
		 runInServlet("testQueueSendMessage");
		 runInServlet("testQueueReceiveMessages");
		 
		 String msg = server1.waitForStringInLog("Queue Message", server1.getMatchingLogFile("trace.log"));
	     assertNotNull("Could not find the queue message in the trace file", msg);
	 }
	
    public static void setUpShrinkWrap() throws Exception {
		JavaArchive utils = ShrinkWrap.create(JavaArchive.class, "utilLib.jar")
				.addPackages(true, "test.util");

		Archive CommsLPwar = ShrinkWrap.create(WebArchive.class, "CommsLP.war")
				.addClass("web.CommsLPServlet")
				.add(new FileAsset(new File("test-applications/CommsLP.war/resources/WEB-INF/web.xml")), "WEB-INF/web.xml")
				.add(new FileAsset(new File("test-applications/CommsLP.war/resources/META-INF/permissions.xml")), "META-INF/permissions.xml")
				.addAsLibrary(utils);

        ShrinkHelper.exportDropinAppToServer(server2, CommsLPwar, OVERWRITE);

        ShrinkHelper.exportDropinAppToServer(server1, CommsLPwar, OVERWRITE);
    }
}
