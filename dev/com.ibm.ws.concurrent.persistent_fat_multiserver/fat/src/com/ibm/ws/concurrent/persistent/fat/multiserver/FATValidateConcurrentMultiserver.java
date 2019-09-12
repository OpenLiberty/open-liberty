/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.multiserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Class to validate multiple servers can access persistent tasks when running from the same
 * DataSource. Refer to the server.xml for each server to note the setup of the DataSource for the
 * persistent executor.
 */
@RunWith(FATRunner.class)
public class FATValidateConcurrentMultiserver {

    private static final Class<?> c = FATValidateConcurrentMultiserver.class;

    private static final LibertyServer server1 = FATSuite.server1;
    private static final LibertyServer server2 = FATSuite.server2;
    
    private static AtomicInteger taskNumber = new AtomicInteger(1);
    private static final String APP_NAME = "webApps.war";

    /**
     * Start Servers.
     * It is important that Server1 is started first. It creates
     * the persistent store database.
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME)
                .addPackage("com.ibm.test.servlet")
                .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/WEB-INF/web.xml"));
        ShrinkHelper.exportToServer(server1, "apps", app);
        ShrinkHelper.exportToServer(server2, "apps", app);
    	
    	server1.startServer(true);
        server2.startServer(true);
        Log.info(c, "beforeClass", "Successfully started servers");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        try {
            server1.stopServer("CWWKG0011W");
        } finally {
            server2.stopServer("CWWKG0011W");
        }
    }

    /*
     * Test the execution sequentially. Call Servlet in each server which
     * runs a persistent task. Ensure they complete successfully
     */
    @Test
    public void testExecute() throws Exception {
        try {

            Log.info(c, "testExecute", "Begin sequential Persistent test affirm test begin");
            
            String server1Result = runInServlet(server1, "test=testExecute&taskNumber=" + taskNumber.getAndIncrement()).toString();
            String server2Result = runInServlet(server2, "test=testExecute&taskNumber=" + taskNumber.getAndIncrement()).toString();

            Log.info(c, "server 1 result = " + server1Result, " Server 1");
            Log.info(c, "server 2 result = " + server2Result, " Server 2");

            String failure = "";
            if (server1Result.indexOf("COMPLETED SUCCESSFULLY") < 0)
                failure += "\r\n----- SERVER 1 OUTPUT -----\r\n" + server1Result;
            if (server2Result.indexOf("COMPLETED SUCCESSFULLY") < 0)
                failure += "\r\n----- SERVER 2 OUTPUT -----\r\n" + server2Result;

            assertEquals("", failure);

        } catch (Exception e) {
            System.out.println("Error running test " + e.toString());
            throw e;
        }

    }

    /*
     * Execute same test as above only make execution concurrent
     */
    @Test
    public void testConcurrentExecute() throws Exception {

        Log.info(c, "Begin concurrent Persistent test", "affirm test begin");
        ExecutorService executor = null;

        try {
            executor = Executors.newFixedThreadPool(2);
            
            CallableServlet callServ1 = new CallableServlet(server1);
            CallableServlet callServ2 = new CallableServlet(server2);
            
            Collection<CallableServlet> collection = new ArrayList<CallableServlet>();
            collection.add(callServ1);
            collection.add(callServ2);

            List<Future<String>> results = executor.invokeAll(collection, 100L, TimeUnit.SECONDS);

            String failure = "";
            //Make sure both servlet calls are complete
            if (results.size() != 2) {
                throw new Exception("Both servers were not successfully called ");
            } else {
                //Check each result for success message
                int i = 0;
                for (Future<String> result : results) {
                    String output = result.get();
                    if (output.indexOf("COMPLETED SUCCESSFULLY") < 0)
                        failure += "\r\n----- SERVER " + (++i) + " OUTPUT -----\r\n" + output;
                }

            }
            //Make sure we got all success messages
            assertEquals("", failure);

        } catch (Exception e) {
            throw e;
        } finally {
            if (executor != null)
                executor.shutdown();
        }

    }
    
    class CallableServlet implements Callable<String> {
    	LibertyServer server;
    	
    	CallableServlet(LibertyServer server) {this.server = server;}
    	
		@Override
		public String call() throws Exception {
			return runInServlet(server, "test=testExecute&taskNumber=" + taskNumber.getAndIncrement()).toString();
		}
    }
    
    /**
     * Runs a test in the servlet.
     *
     * @param queryString query string including at least the test name
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected StringBuilder runInServlet(LibertyServer server, String queryString) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/multiServer?" + queryString);
        Log.info(getClass(), "runInServlet", "URL is " + url);
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

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(getClass(), "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(getClass(), "runInServlet", "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }

            return lines;
        } finally {
            con.disconnect();
            Log.info(getClass(), "runInServlet", "disconnected from servlet");
        }
    }
}
