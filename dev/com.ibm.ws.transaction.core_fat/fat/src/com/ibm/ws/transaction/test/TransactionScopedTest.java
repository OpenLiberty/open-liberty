/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import transactionscopedtest.TransactionScopedTestServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class TransactionScopedTest extends FATServletClient {
    final int instances = 100;

    public static final String APP_NAME = "transactionscoped";
    public static final String SECOND_APP_NAME = "transactionscopedtwo";

    private static final String SERVLET_NAME = APP_NAME + "/transactionscoped";

    @Server("com.ibm.ws.transaction_cdi")
    @TestServlets({ @TestServlet(servlet = TransactionScopedTestServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = TransactionScopedTestServlet.class, contextRoot = SECOND_APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "transactionscopedtest.*");
        //Default app uses the app name to find resource files, since I'm just duplicating an app I'll manually deploy it.
        WebArchive appTwo = ShrinkWrap.create(WebArchive.class, SECOND_APP_NAME + ".war")
                        .addPackage("transactionscopedtest")
                        .add(new FileAsset(new File("test-applications/transactionscoped/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        ShrinkHelper.exportAppToServer(server, appTwo);
        server.addInstalledAppForValidation(SECOND_APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    // Tests are up here so they don't run twice because we have the app installed twice

    @Test
    public void testTS001() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS001"), FATServletClient.SUCCESS);
    }

    @Test
    public void testTS002() throws Exception {

        final ExecutorService executor = Executors.newFixedThreadPool(instances);
        final Collection<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();

        // run the test multiple times concurrently
        for (int i = 0; i < instances; i++) {
            tasks.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS001"), FATServletClient.SUCCESS);
                    return true;
                }
            }));
        }

        // check runs completed successfully
        for (Future<Boolean> task : tasks) {
            try {
                if (!task.get())
                    throw new Exception("0");
            } catch (Exception e) {
                throw new Exception("1", e);
            }
        }

    }

    @Test
    public void testTS003() throws Exception {
        // run the test multiple times sequentially
        for (int i = 0; i < instances; i++) {
            HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS001"), FATServletClient.SUCCESS);
        }
    }

    @Test
    public void testTS004() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS004"), FATServletClient.SUCCESS);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testTS005() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS005"), FATServletClient.SUCCESS);
    }

    @Test
    public void testTS006() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS006"), FATServletClient.SUCCESS);
    }

    @Test
    public void testTS007() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS007"), FATServletClient.SUCCESS);
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.RuntimeException" })
    public void testTS008() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS008"), FATServletClient.SUCCESS);
    }
}
