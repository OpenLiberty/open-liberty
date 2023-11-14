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

import bval.v20.web.BeanVal20TestServlet;
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
 * Verify the clock provided by the default validation configuration tracks
 * time as expected, and that time zone does not adjust, from checkpoint to
 * restore.
 *
 * Verify the validation of basic temporal constraints for a java bean within
 * checkpoint and restore.
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class ClockProviderTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointBeanValidationUTC";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "bvalApp";

    @Server(SERVER_NAME)
    @TestServlet(servlet = BeanVal20TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "bval.v20.web");
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false,
                             server -> {
                                 assertNotNull("BeanVal20TestServlet init message not found in log.",
                                               server.waitForStringInLogUsingMark("BeanVal20TestServlet init now", 0));
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
