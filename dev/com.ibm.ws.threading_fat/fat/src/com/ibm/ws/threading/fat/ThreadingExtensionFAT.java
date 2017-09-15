package com.ibm.ws.threading.fat;

/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class ThreadingExtensionFAT {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.threading_fat_server");
    private static String threadingTestServletURL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/threadingtestapp/ThreadingTestServlet";
    private final Class<?> c = ThreadingExtensionFAT.class;

    @Before
    public void beforeTest() throws Exception {
        final String method = "beforeTest";
        Log.entering(c, method);

        boolean serverWasStarted = false;

        if (server != null && !server.isStarted()) {
            server.startServer();
            serverWasStarted = true;
        }

        Log.exiting(c, method, serverWasStarted);
    }

    @After
    public void afterTest() throws Exception {
        final String method = "afterTest";
        Log.entering(c, method);

        boolean serverWasStopped = false;

        if (server != null && server.isStarted()) {
            server.stopServer();
            serverWasStopped = true;
        }

        Log.exiting(c, method, serverWasStopped);
    }

    /**
     * Verifies that registering a java.util.concurrent.ThreadFactory implementation into the
     * OSGi registry with property "com.ibm.ws.threading.defaultExecutorThreadFactory=true"
     * results in all default executor threads being created by that factory. The test
     * ThreadFactory that we registered prefixes the thread name of every thread it creates
     * with "com.ibm.ws.threading_fat_ThreadFactoryImpl". The test servlet returns the name
     * of the thread it executed on. We just need to invoke the servlet and verify that the
     * returned thread name matches one created by our factory.
     */
    @Test
    public void testThreadFactoryExtension() throws Exception {
        final String method = "testThreadFactoryExtension";
        Log.entering(c, method);

        String line = invokeURL(threadingTestServletURL).readLine();

        Log.info(c, method, "Return data from test servlet: " + line);

        assertTrue("The response did not start with com.ibm.ws.threading_fat_ThreadFactoryImpl-thread-", line.startsWith("com.ibm.ws.threading_fat_ThreadFactoryImpl-thread-"));

        Log.exiting(c, method);
    }

    /**
     * Verifies that registering a com.ibm.wsspi.threading.ExecutorServiceTaskInterceptor implementation
     * into the OSGi service registry results in all tasks submitted to the executor being wrapped by
     * our interceptor. To verify this, we invoke a test URL (just to get work run) and then look in the
     * log for System.out.printlns printed by a task created by our interceptor.
     */
    @Test
    public void testTaskInterceptorExtension() throws Exception {
        final String method = "testTaskInterceptorExtension";
        Log.entering(c, method);

        invokeURL(threadingTestServletURL).readLine();

        // verify that the task interceptor ran by looking for the System.out.printlns it puts in the server log
        assertTrue("Did not find 'com.ibm.ws.threading_fat_beforeTask' in log file", server.findStringsInLogs("com.ibm.ws.threading_fat_beforeTask").size() > 0);
        assertTrue("Did not find 'com.ibm.ws.threading_fat_afterTask' in log file", server.findStringsInLogs("com.ibm.ws.threading_fat_afterTask").size() > 0);

        Log.exiting(c, method);
    }

    /**
     * Invokes the specified URL and returns a BufferedReader to the returned content.
     * 
     * @param urlString The URL to invoke
     * @return a BufferedReader to the content returned from the invoked URL
     * @throws Exception
     */
    private BufferedReader invokeURL(String urlString) throws Exception {
        final String method = "invokeURL";
        Log.entering(c, method, urlString);

        HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

        Log.exiting(c, method, br);
        return br;
    }
}