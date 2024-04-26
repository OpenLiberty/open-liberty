/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CVEReportingConfigTest extends FATServletClient {

    public static final String SERVER_NAME = "io.openliberty.reporting.server";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        server.saveServerConfiguration();
    }

    @After
    public void tearDown() throws Exception {

        if (server.isStarted()) {
            server.stopServer();
        }

        server.restoreServerConfiguration();
    }

    @Test
    public void testIsEnabledByDefault() throws Exception {
        server.startServer();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));
    }

    @Test
    public void testIsDisabled() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getCVEReporting().setEnabled(false);
        server.updateServerConfiguration(config);

        server.startServer();

        assertNotNull("The feature is enabled", server.waitForStringInLog("CWWKF1701I:.*"));
    }

    @Test
    public void testIsEnabled() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getCVEReporting().setEnabled(true);
        server.updateServerConfiguration(config);

        server.startServer();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));
    }

    @Test
    public void testDynamicUpdate() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getCVEReporting().setEnabled(true);
        server.updateServerConfiguration(config);

        server.startServer();
        server.addIgnoredErrors(Collections.singletonList("CWWKF1704W"));

        assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));

        server.setMarkToEndOfLog();

        config.getCVEReporting().setEnabled(false);
        server.updateServerConfiguration(config);

        assertNotNull("The feature is enabled", server.waitForStringInLog("CWWKF1701I:.*"));

        server.setMarkToEndOfLog();

        config.getCVEReporting().setEnabled(true);
        server.updateServerConfiguration(config);

        assertNotNull("The feature is disabled", server.waitForStringInLog("CWWKF1700I:.*"));

    }

}
