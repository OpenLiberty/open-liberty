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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import bval.v20.cdi.web.BeanValCDIServlet;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify a restored server validates basic (built-in) Bean Validation constraints
 * within a web application that uses a CDI managed bean.
 *
 * Temporal constraint validation uses a custom ClockProvider implementation. The
 * impl class is provided by the test and configured in validation.xml. Otherwise,
 * the ValidationFactory has the default configuration.
 *
 * This is an InstantOn bringup test for the Bean Validation feature.
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class ManagedBeanTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointBeanValidation";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "bvalCDIApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = BeanValCDIServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "bval.v20.cdi.web");
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.startServer();
        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            stopServer();
        } finally {
            ShrinkHelper.cleanAllExportedArchives();
        }
    }

    static void stopServer() {
        if (server.isStarted()) {
            try {
                // Ignore ConstraintViolationExceptions thrown by validation tests
                server.stopServer("ConstraintViolationException");
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

}
