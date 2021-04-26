/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.fat.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS21ClientJerseyRxInvokerTest extends JAXRS21AbstractTest {
    @Server("jaxrs21.client.JAXRS21ClientJerseyRxInvokerTest")
    public static LibertyServer server;

    private static final String bookstorewar = "jaxrs21bookstore";

    private final static String jerseyRxInvokerTarget = "jaxrs21bookstore/JerseyRxInvokerTestServlet";

    private static final String reactivex = "lib/";

    private static final String jersey = "lib/";

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(bookstorewar, "com.ibm.ws.jaxrs21.fat.JAXRS21bookstore");

        app.addAsLibraries(new File(reactivex).listFiles());
        app.addAsLibraries(new File(jersey).listFiles());
        ShrinkHelper.exportDropinAppToServer(server, app);


        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on server",
                      server.waitForStringInLog("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testRxObservableInvoker_get1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_get1", p, "Good book");
    }

    @Test
    public void testRxFlowableInvoker_get1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_get1", p, "Good book");
    }

    @Test
    public void testRxFlowableToObservableInvoker_get1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableToObservableInvoker_get1", p, "Good book");
    }

    @Test
    public void testRxObservableInvoker_get2WithClass() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_get2WithClass", p, "Good book");
    }

    @Test
    public void testRxFlowableInvoker_get2WithClass() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_get2WithClass", p, "Good book");
    }

    @Test
    public void testRxObservableInvoker_get3WithGenericType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_get3WithGenericType", p, "true");
    }

    @Test
    public void testRxFlowableInvoker_get3WithGenericType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_get3WithGenericType", p, "true");
    }

    @Test
    public void testRxObservableInvoker_get5WithZip() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_get5WithZip", p, "Good book");
    }

    @Test
    public void testRxFlowableInvoker_get5WithZip() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_get5WithZip", p, "Good book");
    }

    @Test
    public void testRxFlowableToObservableInvoker_get5WithZip() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableToObservableInvoker_get5WithZip", p, "Good book");
    }

    @Test
    public void testRxObservableInvoker_post1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_post1", p, "Test book");
    }

    @Test
    public void testRxFlowableInvoker_post1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_post1", p, "Test book");
    }

    @Test
    public void testRxFlowableToObservableInvoker_post1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableToObservableInvoker_post1", p, "Test book");
    }

    @Test
    public void testRxObservableInvoker_post2WithClass() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_post2WithClass", p, "Test book2");
    }

    @Test
    public void testRxFlowableInvoker_post2WithClass() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_post2WithClass", p, "Test book2");
    }

    @Test
    public void testRxObservableInvoker_post3WithGenericType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_post3WithGenericType", p, "Test book3");
    }

    @Test
    public void testRxFlowableInvoker_post3WithGenericType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_post3WithGenericType", p, "Test book3");
    }

    @Test
    public void testRxObservableInvoker_post5WithZip() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_post5WithZip", p, "Test book6");
    }

    @Test
    public void testRxFlowableInvoker_post5WithZip() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_post5WithZip", p, "Test book6");
    }

    @Test
    public void testRxFlowableToObservableInvoker_post5WithZip() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableToObservableInvoker_post5WithZip", p, "Test book6");
    }

    @Test
    public void testRxObservableInvokerOnError() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvokerOnError", p, "true");
    }

    @Test
    public void testRxFlowableInvokerOnError() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvokerOnError", p, "true");
    }

    @Test
    public void testRxFlowableToObservableInvokerOnError() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableToObservableInvokerOnError", p, "true");
    }

    @Test
    public void testRxObservableInvoker_getReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_getReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    public void testRxFlowableInvoker_getReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_getReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue  https://github.com/OpenLiberty/open-liberty/issues/16648
    public void testRxObservableInvoker_getConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_getConnectionTimeout", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue  https://github.com/OpenLiberty/open-liberty/issues/16648
    public void testRxFlowableInvoker_getConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_getConnectionTimeout", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue  https://github.com/OpenLiberty/open-liberty/issues/16648
    public void testRxObservableInvoker_postReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_postReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue  https://github.com/OpenLiberty/open-liberty/issues/16648
    public void testRxFlowableInvoker_postReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_postReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    public void testRxObservableInvoker_postConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxObservableInvoker_postConnectionTimeout", p, "Timeout as expected");
    }

    @Test
    public void testRxFlowableInvoker_postConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(jerseyRxInvokerTarget, "testRxFlowableInvoker_postConnectionTimeout", p, "Timeout as expected");
    }
}
