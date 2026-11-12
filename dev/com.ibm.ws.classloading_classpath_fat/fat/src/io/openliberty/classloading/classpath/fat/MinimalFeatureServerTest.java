/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.classloading.classpath.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.MINIMAL_FEATURE_SERVER;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Verifies that a minimal Liberty server running a standalone feature
 * (e.g. appSecurity-2.0) with no deployed applications, and therefore without the
 * application lifecycle packages, starts and stops cleanly. In particular
 * com.ibm.ws.classloading must resolve and activate: its optional dependency on
 * ApplicationRecycleCoordinator must never be reflected over by Declarative Services.
 */
@RunWith(FATRunner.class)
public class MinimalFeatureServerTest {

    @Server(MINIMAL_FEATURE_SERVER)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testMinimalFeatureServerStartsCleanly() throws Exception {
        assertTrue("com.ibm.ws.classloading failed to resolve in a minimal server (CWWKE0702E)",
                   server.findStringsInLogs("CWWKE0702E").isEmpty());
        assertTrue("NoClassDefFoundError was logged while starting a minimal server",
                   server.findStringsInLogs("NoClassDefFoundError").isEmpty());
    }
}
