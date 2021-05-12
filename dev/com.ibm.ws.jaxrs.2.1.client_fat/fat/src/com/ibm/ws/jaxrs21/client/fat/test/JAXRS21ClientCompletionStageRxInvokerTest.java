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
public class JAXRS21ClientCompletionStageRxInvokerTest extends JAXRS21AbstractTest {
    @Server("jaxrs21.client.JAXRS21ClientCompletionStageRxInvokerTest")
    public static LibertyServer server;

    private static final String bookstorewar = "jaxrs21bookstore";

    private final static String completionStageRxInvokerTarget = "jaxrs21bookstore/CompletionStageRxInvokerTestServlet";

    private static final String reactivex = "lib/";

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(bookstorewar, "com.ibm.ws.jaxrs21.fat.JAXRS21bookstore");

        app.addAsLibraries(new File(reactivex).listFiles());
        ShrinkHelper.exportDropinAppToServer(server, app);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W", "CWWKE1102W");
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    /**
     * Test: Test the new Reactive client by simply changing the async.get() to rx.get()
     *
     * Expected Results: No exceptions and the get is successful
     */

    @Test
    public void testCompletionStageRxInvoker_get1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_get1", p, "Good book");
    }

    /**
     * Test: Test the new Reactive client by simply changing the async.get(class) to rx.get(class)
     *
     * Expected Results: No exceptions and the get is successful
     */

    @Test
    public void testCompletionStageRxInvoker_get2WithClass() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_get2WithClass", p, "Good book");
    }

    /**
     * Test: Test the new Reactive client by simply changing the async.get(GenericType) to rx.get(GenericType)
     *
     * Expected Results: No exceptions and the get is successful
     */

    @Test
    public void testCompletionStageRxInvoker_get3WithGenericType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_get3WithGenericType", p, "true");
    }

    /**
     * Test: Test the new Reactive client by using the ExecutorService
     *
     * Expected Results: No exceptions and the get is successful
     */

    @Test
    public void testCompletionStageRxInvoker_get4WithExecutorService() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_get4WithExecutorService", p, "Good book");
    }

    /**
     * Test: Test the new Reactive client by using the completionStage.thenCombine
     *
     * Expected Results: No exceptions and the get is successful
     */

    @Test
    public void testCompletionStageRxInvoker_get5WithThenCombine() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_get5WithThenCombine", p, "Good book");
    }

    /**
     * Test: Test the new Reactive client by simply changing the async.post() to rx.post()
     *
     * Expected Results: No exceptions and the post is successful
     */

    @Test
    public void testCompletionStageRxInvoker_post1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_post1", p, "Test book");
    }

    /**
     * Test: Test the new Reactive client by simply changing the async.post(class) to rx.post(class)
     *
     * Expected Results: No exceptions and the post is successful
     */

    @Test
    public void testCompletionStageRxInvoker_post2WithClass() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_post2WithClass", p, "Test book2");
    }

    /**
     * Test: Test the new Reactive client by simply changing the async.post(GenericType) to rx.post(GenericType)
     *
     * Expected Results: No exceptions and the post is successful
     */

    @Test
    public void testCompletionStageRxInvoker_post3WithGenericType() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_post3WithGenericType", p, "Test book3");
    }

    /**
     * Test: Test the new Reactive client by using the ExecutorService
     *
     * Expected Results: No exceptions and the post is successful
     */

    @Test
    public void testCompletionStageRxInvoker_post4WithExecutorService() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_post4WithExecutorService", p, "Test book4");
    }

    /**
     * Test: Test the new Reactive client by using the completionStage.thenCombine
     *
     * Expected Results: No exceptions and the post is successful
     */

    @Test
    public void testCompletionStageRxInvoker_post5WithThenCombine() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_post5WithThenCombine", p, "Test book6");
    }

    @Test
    public void testCompletionStageRxInvoker_getReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_getReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue https://github.com/OpenLiberty/open-liberty/issues/16651
    public void testCompletionStageRxInvoker_getConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_getConnectionTimeout", p, "Timeout as expected");
    }

    @Test
    public void testCompletionStageRxInvoker_postReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_postReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue https://github.com/OpenLiberty/open-liberty/issues/16651
    public void testCompletionStageRxInvoker_postConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(completionStageRxInvokerTarget, "testCompletionStageRxInvoker_postConnectionTimeout", p, "Timeout as expected");
    }
}
