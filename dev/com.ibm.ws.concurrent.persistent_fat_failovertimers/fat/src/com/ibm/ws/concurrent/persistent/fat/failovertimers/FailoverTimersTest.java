/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.failovertimers;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.PersistentExecutor;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import failovertimers.web.FailoverTimersTestServlet;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class FailoverTimersTest extends FATServletClient {
	private static final String APP_NAME = "failoverTimersApp";
	private static final Set<String> APP_NAMES = Collections.singleton(APP_NAME);

    private static ServerConfiguration originalConfigA;
    private static ServerConfiguration originalConfigB;

    @Server("com.ibm.ws.concurrent.persistent.fat.failovertimers.serverA")
    @TestServlet(servlet = FailoverTimersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverA;

    @Server("com.ibm.ws.concurrent.persistent.fat.failovertimers.serverB")
    @TestServlet(servlet = FailoverTimersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverB;

    private static final ExecutorService testThreads = Executors.newFixedThreadPool(3);

    @BeforeClass
    public static void setUp() throws Exception {
        originalConfigA = serverA.getServerConfiguration();
        ShrinkHelper.defaultApp(serverA, APP_NAME, "failovertimers.web", "failovertimers.ejb.autotimer");

        serverB.useSecondaryHTTPPort();
        originalConfigB = serverB.getServerConfiguration();
        ShrinkHelper.defaultApp(serverB, APP_NAME, "failovertimers.web", "failovertimers.ejb.autotimer");

        testThreads.invokeAll(Arrays.asList(
                () -> serverA.startServer(),
                () -> serverB.startServer()
        )).forEach(f -> {
            try {
                f.get();
            } catch (ExecutionException | InterruptedException x) {
                throw new CompletionException(x);
            }
        });
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            serverA.stopServer();
        } finally {
            serverB.stopServer();
        }
    }

    /**
     * TODO write a useful test now that test bucket has been created
     */
    @Test
    public void testSomething() throws Exception {
        runTestWithResponse(serverA, APP_NAME + "/FailoverTimersTestServlet", "test1&test=testSomething[A]");
        runTestWithResponse(serverB, APP_NAME + "/FailoverTimersTestServlet", "test1&test=testSomething[B]");
    }
}