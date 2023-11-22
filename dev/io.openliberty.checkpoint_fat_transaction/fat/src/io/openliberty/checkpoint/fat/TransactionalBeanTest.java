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

import static io.openliberty.checkpoint.fat.FATSuite.stopServer;
import static io.openliberty.checkpoint.fat.util.FATUtils.LOG_SEARCH_TIMEOUT;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.transactional.web.TransactionalBeanServlet;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify the server (CDI) maintains transaction boundaries for @Transactional managed
 * beans within servers restored after checkpoint at=applications.
 *
 * The jakarta.transaction.Transactional annotation provides the application the ability
 * to declaratively control transaction boundaries on CDI managed beans, as well as classes
 * defined as managed beans by the Jakarta EE specification, at both the class and method
 * level where method level annotations override those at the class level.
 *
 * This support is provided via an implementation of CDI interceptors that conduct the
 * necessary suspending, resuming, etc. The Transactional interceptor interposes on business
 * method invocations only and not on lifecycle events. Lifecycle methods are invoked in an
 * unspecified transaction context.
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class TransactionalBeanTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionalBean";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EE8FeatureReplacementAction().forServers(SERVER_NAME))
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transactionalbean";
    static final String SERVLET_NAME = APP_NAME + "/transactionalbean";

    @Server(SERVER_NAME)
    @TestServlet(servlet = TransactionalBeanServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    static final String TX_RETRY_INT = "11";

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.cleanAllExportedArchives();

        ShrinkHelper.defaultDropinApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "com.ibm.ws.transactional.web.*");

        Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
            // Env var change that triggers transaction config update at restore
            File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
            try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                serverEnvWriter.println("TX_RETRY_INT=" + TX_RETRY_INT);
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
            assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                          checkpointServer.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
            assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                          checkpointServer.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
        };
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, preRestoreLogic);
        server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        server.startServer();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        stopServer(server, "WTRN0017W");
    }

}
