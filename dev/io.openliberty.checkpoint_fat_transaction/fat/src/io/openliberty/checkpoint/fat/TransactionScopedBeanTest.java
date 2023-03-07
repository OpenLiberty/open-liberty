/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertNotNull;

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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
//@Mode(TestMode.FULL)
public class TransactionScopedBeanTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionScopedBean";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() //
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly()) //
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transactionscopedbean";
    static final String SECOND_APP_NAME = "transactionscopedbeantwo";
    static final String SERVLET_NAME = APP_NAME + "/transactionscopedbean";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    final int instances = 100;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "transactionscopedtest.*");

        // Default app uses the app name to find resource files; manually deploy the resource for this duplicate app.
        WebArchive appTwo = ShrinkWrap.create(WebArchive.class, SECOND_APP_NAME + ".war")
                        .addPackage("transactionscopedtest")
                        .add(new FileAsset(new File("test-applications/transactionscopedbean/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        ShrinkHelper.exportAppToServer(server, appTwo);
        server.addInstalledAppForValidation(SECOND_APP_NAME);

        server.setCheckpoint(CheckpointPhase.APPLICATIONS, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.setServerStartTimeout(300000);
        server.startServer();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        stopServer();
        ShrinkHelper.cleanAllExportedArchives();
    }

    static void stopServer() {
        if (server.isStarted()) {
            try {
                server.stopServer();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Tests are here rather than servlet so they don't run twice because we have the app installed twice

    @Test
    public void testTransactionScopedBean001() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS001"), FATServletClient.SUCCESS);
    }

    @Test
    public void testTransactionScopedBean002() throws Exception {

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
    public void testTransactionScopedBean003() throws Exception {
        // run the test multiple times sequentially
        for (int i = 0; i < instances; i++) {
            HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS001"), FATServletClient.SUCCESS);
        }
    }

    @Test
    public void testTransactionScopedBean004() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS004"), FATServletClient.SUCCESS);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testTransactionScopedBean005() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS005"), FATServletClient.SUCCESS);
    }

    @Test
    public void testTransactionScopedBean006() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS006"), FATServletClient.SUCCESS);
    }

    @Test
    public void testTransactionScopedBean007() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS007"), FATServletClient.SUCCESS);
    }

    @Test
    @ExpectedFFDC(value = { "java.lang.RuntimeException" })
    public void testTransactionScopedBean008() throws Exception {
        HttpUtils.findStringInReadyUrl(server, FATServletClient.getPathAndQuery(SERVLET_NAME, "testTS008"), FATServletClient.SUCCESS);
    }
}
