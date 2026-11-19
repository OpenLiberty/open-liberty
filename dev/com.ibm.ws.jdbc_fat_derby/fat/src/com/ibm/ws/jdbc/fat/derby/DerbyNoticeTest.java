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
package com.ibm.ws.jdbc.fat.derby;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Each Derby configuration variant is tested in isolation, because the notice
 * is only issued once per JVM and detection failure of one variant would
 * otherwise be masked by another.
 */
@RunWith(FATRunner.class)
public class DerbyNoticeTest {

    public static final String SERVER_NAME = "com.ibm.ws.jdbc.fat.derby.notice";

    private static final String APP_NAME = "noticeapp";
    private static final String SERVLET_PATH = APP_NAME + "/NoticeServlet";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void createArchives() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.derby.notice.web");
    }

    @Before
    public void setUp() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (server.isStarted())
                server.stopServer("DSRA8071W");
        } finally {
            server.restoreServerConfiguration();
        }
    }

    /**
     * Starts the server in beta or GA mode with only the specified data sources configured,
     * then runs the servlet test method that looks up those data sources, which activates them.
     */
    private void startWithDataSources(boolean beta, String lookupTest, String... dataSourceIds) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        List<String> keep = Arrays.asList(dataSourceIds);
        for (String id : Arrays.asList("derbyEmbedded1", "derbyEmbedded2", "derbyClient", "derbyGeneric"))
            if (!keep.contains(id))
                config.getDataSources().removeById(id);
        server.updateServerConfiguration(config);
        server.setJvmOptions(Collections.singletonList("-Dcom.ibm.ws.beta.edition=" + beta));
        server.startServer();
        FATServletClient.runTest(server, SERVLET_PATH, lookupTest);
    }

    private void assertNotices(int derbyWarnings) throws Exception {
        assertEquals("Unexpected number of DSRA8071W messages", derbyWarnings, server.findStringsInLogs("DSRA8071W").size());
        assertEquals("Expected DSRA8072I notice to not appear in logs", 0, server.findStringsInLogs("DSRA8072I").size());
    }

    /**
     * properties.derby.embedded, with multiple data sources: warning is logged once.
     */
    @Test
    public void testDerbyEmbeddedBetaNotice() throws Exception {
        startWithDataSources(true, "lookupEmbedded", "derbyEmbedded1", "derbyEmbedded2");
        assertNotices(1);
    }

    /**
     * properties.derby.client: warning is logged once.
     */
    @Test
    public void testDerbyClientBetaNotice() throws Exception {
        startWithDataSources(true, "lookupClient", "derbyClient");
        assertNotices(1);
    }

    /**
     * Generic data source using a Derby driver class: warning is logged once.
     */
    @Test
    public void testDerbyGenericBetaNotice() throws Exception {
        startWithDataSources(true, "lookupGeneric", "derbyGeneric");
        assertNotices(1);
    }

    /**
     * GA server (beta property false) with all Derby variants logs neither message.
     */
    @Test
    public void testDerbyGaNoNotice() throws Exception {
        startWithDataSources(false, "lookupAll", "derbyEmbedded1", "derbyEmbedded2", "derbyClient", "derbyGeneric");
        assertNotices(0);
    }
}
