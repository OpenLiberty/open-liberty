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
package io.openliberty.checkpoint.fat.springboot;

import static io.openliberty.checkpoint.fat.springboot.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class BasicSpringBootTests extends FATServletClient {

    @Server("checkpointSpringBoot")
    public static LibertyServer server;

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true,
                             server -> {
                                 assertNotNull("'CWWKZ0001I: Application started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I", 0));
                                 assertNotNull("JVM checkpoint/restore callback not registered",
                                               server.waitForStringInLogUsingMark("Registering JVM checkpoint/restore callback for Spring-managed lifecycle beans", 0));
                                 // make sure the web app URL is not logged on checkpoint side
                                 assertNull("'CWWKT0016I: Web application available' found in log.",
                                            server.waitForStringInLogUsingMark("CWWKT0016I", 0));
                                 assertNotNull("Spring-managed lifecycle beans not stopped",
                                               server.waitForStringInLogUsingMark("Stopping Spring-managed lifecycle beans before JVM checkpoint", 0));
                             });
        server.startServer(getTestMethodNameOnly(testName) + ".log");
        assertNotNull("Spring-managed lifecycle beans not restarted after JVM restore",
                      server.waitForStringInLogUsingMark("Restarting Spring-managed lifecycle beans after JVM restore"));
        // make sure the web app URL is logged on restore side
        assertNotNull("'CWWKT0016I: Web application available' not found in log.",
                      server.waitForStringInLogUsingMark("CWWKT0016I"));
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testBasicSpringBootApplication() throws Exception {
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

}
