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
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transactional.web.TransactionalBeanServlet;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
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
@SkipIfCheckpointNotSupported
public class TransactionalBeanTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionalBean";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transactionalbean";
    static final String SERVLET_NAME = APP_NAME + "/transactionalbean";

    @Server(SERVER_NAME)
    @TestServlet(servlet = TransactionalBeanServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "com.ibm.ws.transactional.web.*");
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
        stopServer(server, "WTRN0017W");
        ShrinkHelper.cleanAllExportedArchives();
    }

}
