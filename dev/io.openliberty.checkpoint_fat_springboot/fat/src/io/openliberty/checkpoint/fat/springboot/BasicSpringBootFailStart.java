/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

import static io.openliberty.checkpoint.fat.springboot.FATSuite.configureApplication;
import static io.openliberty.checkpoint.fat.springboot.FATSuite.configureBootStrapProperties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
@MinimumJavaLevel(javaLevel = 17)
public class BasicSpringBootFailStart extends FATServletClient {

    @Server("checkpointSpringBoot")
    public static LibertyServer server;

    public static final String APP_NAME = "io.openliberty.checkpoint.springboot.fat30.app-0.0.1-SNAPSHOT.jar";

    @BeforeClass
    public static void setUp() throws Exception {
        configureApplication(server, APP_NAME);
        configureBootStrapProperties(server, Map.of("sprigboot.test.failstart", "true"));
        CheckpointInfo checkpointInfo = new CheckpointInfo(CheckpointPhase.AFTER_APP_START, false, true, true, server -> {
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
        server.setCheckpoint(checkpointInfo);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testFailAppStart() throws Exception {
        ProgramOutput output = server.startServer(testName + ".log");
        int retureCode = output.getReturnCode();
        assertEquals("Wrong return code for failed checkpoint.", 72, retureCode);
    }

}
