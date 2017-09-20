/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.context.fat;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ContextService;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.context.ClassloaderContext;
import com.ibm.websphere.simplicity.config.context.JEEMetadataContext;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for contextService, including tests that make updates to the server
 * configuration while the server is running.
 * A tearDown method runs after each test to restore to the original configuration,
 * so that tests do not interfere with each other.
 */
public class ContextServiceFATTest {
    private static final Set<String> appNames = new TreeSet<String>(Arrays.asList("context"));

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.context.fat");

    // Tests can use this to indicate they don't make any config updates and so don't need to have the original config restored
    private static boolean restoreSavedConfig = true;

    private static ServerConfiguration savedConfig;

    private static final String WAR_NAME = "context";

    @Rule
    public TestName testName = new TestName();

    /**
     * Runs a test in the servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    protected StringBuilder runInServlet(String servlet, String test, String contextSvc) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + servlet + "?test=" + test + "&contextService=" + contextSvc);
        for (int numRetries = 2;; numRetries--) {
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
            } catch (FileNotFoundException x) {
                if (numRetries > 0)
                    try {
                        Log.info(getClass(), "runInServlet", x + " occurred - will retry after 10 seconds");
                        Thread.sleep(10000);
                    } catch (InterruptedException interruption) {
                    }
                else
                    throw x;
            } finally {
                con.disconnect();
                Log.info(getClass(), "runInServlet", "disconnected from servlet");
            }
        }
    }

    /**
     * Before running any tests, start the server and save the original configuration.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        savedConfig = server.getServerConfiguration().clone();
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/contextinternals-1.0.mf");

        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_NAME + ".war");
        war.addPackage("web");
        war.addAsWebInfResource(new File("test-applications/context/resources/WEB-INF/web.xml"));
        ShrinkHelper.exportToServer(server, "dropins", war);

        for (String name : appNames)
            server.addInstalledAppForValidation(name);
    }

    /**
     * Before running each test, restore to the original configuration.
     *
     * @throws Exception
     */
    @Before
    public void setUpPerTest() throws Exception {
        String consoleLogFileName = getClass().getSimpleName() + '.' + testName.getMethodName() + ".log";
        if (!server.isStarted()) {
            server.updateServerConfiguration(savedConfig);
            server.startServer(consoleLogFileName); // clean start
            Log.info(getClass(), "setUpPerTest", "server started, log file is " + consoleLogFileName);
        } else if (restoreSavedConfig) {
            server.stopServer("CWWKG0007W", "WTRN0017W"); // TODO remove CWWKG0007W once 168026 is fixed
            server.updateServerConfiguration(savedConfig);
            server.startServer(consoleLogFileName, false, false); // warm start
            Log.info(getClass(), "setUpPerTest", "server restarted, log file is " + consoleLogFileName);
        }
        restoreSavedConfig = true; // assume all tests make config updates unless they tell us otherwise
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("WTRN0017W");
            server.updateServerConfiguration(savedConfig);
        }
    }

    @Test
    public void testClassloaderContext() throws Exception {
        runInServlet("context", "testClassloaderContext", "java:comp/DefaultContextService");
        restoreSavedConfig = false;
    }

    @Test
    public void testJEEMetadataContext() throws Exception {
        runInServlet("context", "testJEEMetadataContext", "java:comp/DefaultContextService");
        restoreSavedConfig = false;
    }

    @Test
    public void testCreateNewContextService() throws Exception {
        // Add <contextService id="contextSvc1" jndiName="concurrent/contextSvc1"/>
        ServerConfiguration config = server.getServerConfiguration();
        ContextService contextSvc1 = new ContextService();
        contextSvc1.setId("contextSvc1");
        contextSvc1.setJndiName("concurrent/contextSvc1");
        config.getContextServices().add(contextSvc1);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("context", "testNoClassloaderContext", contextSvc1.getJndiName());
        runInServlet("context", "testNoJEEMetadataContext", contextSvc1.getJndiName());

        // transaction context can be either because it is controlled by the app
        runInServlet("context", "testNoTransactionContext", contextSvc1.getJndiName());
        runInServlet("context", "testTransactionContext", contextSvc1.getJndiName());

        // Add <jeeMetadataContext/> to contextSvc1
        contextSvc1.getJEEMetadataContexts().add(new JEEMetadataContext());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("context", "testJEEMetadataContext", contextSvc1.getJndiName());
        runInServlet("context", "testDefaultContextForAllContextTypes", contextSvc1.getJndiName());

        // Add <classloaderContext/> to contextSvc1
        contextSvc1.getClassloaderContexts().add(new ClassloaderContext());
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("context", "testClassloaderContext", contextSvc1.getJndiName());

        // Add <contextService id="contextSvc2" jndiName="concurrent/contextSvc2" baseContextRef="contextSvc1"/>
        ContextService contextSvc2 = new ContextService();
        contextSvc2.setId("contextSvc2");
        contextSvc2.setJndiName("concurrent/contextSvc2");
        contextSvc2.setBaseContextRef(contextSvc1.getId());
        config.getContextServices().add(contextSvc2);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("context", "testClassloaderContext", contextSvc2.getJndiName());
        runInServlet("context", "testJEEMetadataContext", contextSvc2.getJndiName());

        // Remove <classloaderContext/> from contextSvc1
        contextSvc1.getClassloaderContexts().retainAll(Collections.EMPTY_SET);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        //runInServlet("context", "testNoClassloaderContext", contextSvc2.getJndiName()); // TODO: re-enable when bug with unset never being invoked is fixed
        //runInServlet("context", "testNoClassloaderContext", contextSvc1.getJndiName()); // TODO: re-enable when bug with unset never being invoked is fixed

        // Switch contextSvc2's baseContextRef to be the default contextService instance
        contextSvc2.setBaseContextRef("DefaultContextService");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("context", "testClassloaderContext", contextSvc2.getJndiName());
        runInServlet("context", "testJEEMetadataContext", contextSvc2.getJndiName());

        // Remove contextSvc2's baseContextRef
        contextSvc2.setBaseContextRef(null);
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames);

        runInServlet("context", "testNoClassloaderContext", contextSvc2.getJndiName());
        runInServlet("context", "testNoJEEMetadataContext", contextSvc2.getJndiName());
        runInServlet("context", "testTransactionContext", contextSvc2.getJndiName());
    }

    @Test
    @ExpectedFFDC("javax.transaction.NotSupportedException")
    public void testTransactionContext() throws Exception {
        runInServlet("context", "testTransactionContext", "java:comp/DefaultContextService");
        runInServlet("context", "testNoTransactionContext", "java:comp/DefaultContextService");
        runInServlet("context", "testSkipTransactionContext", "java:comp/DefaultContextService");
        restoreSavedConfig = false;
    }
}