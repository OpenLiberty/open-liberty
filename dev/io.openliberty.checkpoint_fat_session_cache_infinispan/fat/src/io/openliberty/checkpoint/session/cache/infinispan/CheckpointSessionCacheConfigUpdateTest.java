/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.session.cache.infinispan;

import static org.junit.Assert.assertNotNull;

import java.time.ZonedDateTime;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpSessionCache;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class CheckpointSessionCacheConfigUpdateTest extends FATServletClient {
    public static final String checkpointServer = "com.ibm.ws.session.cache.config.fat.infinispan.checkpointServer";

    @Server(checkpointServer)
    public static LibertyServer server;

    public static String[] servers = new String[] { checkpointServer };

    @BeforeClass
    public static void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SESN0312W");
    }

    @Test
    public void testUpdateInConfigAfterCheckpoint() throws Exception {
        int hour = ZonedDateTime.now().getHour();
        int hour1 = (hour + 8) % 24;
        int hour2 = (hour + 16) % 24;
        // Reconfigure server
        ServerConfiguration config = server.getServerConfiguration();
        HttpSessionCache httpSessionCache = config.getHttpSessionCaches().get(0);
        httpSessionCache.setWriteContents("GET_AND_SET_ATTRIBUTES");
        httpSessionCache.setWriteFrequency("TIME_BASED_WRITE");
        httpSessionCache.setWriteInterval("15s");
        httpSessionCache.setScheduleInvalidationFirstHour(Integer.toString(hour1));
        httpSessionCache.setScheduleInvalidationSecondHour(Integer.toString(hour2));
        server.updateServerConfiguration(config);
        server.checkpointRestore();
        assertNotNull("'SESN0312W: The writeContents HTTP Session Cache configuration attribute changed' not found in log.",
                      server.waitForStringInLog("SESN0312W: .*writeContents*"));
        assertNotNull("'SESN0312W: The writeFrequency HTTP Session Cache configuration attribute changed' not found in log.",
                      server.waitForStringInLog("SESN0312W: .*writeFrequency*"));
        assertNotNull("'SESN0312W: The writeInterval HTTP Session Cache configuration attribute changed' not found in log' not found in log.",
                      server.waitForStringInLog("SESN0312W: .*writeInterval*"));
        assertNotNull("'SESN0312W: The scheduleInvalidationFirstHour HTTP Session Cache configuration attribute changed' not found in log' not found in log.",
                      server.waitForStringInLog("SESN0312W: .*scheduleInvalidationFirstHour*"));
        assertNotNull("'SESN0312W: The scheduleInvalidationSecondHour HTTP Session Cache configuration attribute changed' not found in log' not found in log.",
                      server.waitForStringInLog("SESN0312W: .*scheduleInvalidationSecondHour*"));

    }
}
