/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;
import com.ibm.ws.security.fat.common.servers.ServerTracker;

import componenttest.topology.impl.LibertyServer;

public class CommonSecurityFat {

    protected static Class<?> thisClass = CommonSecurityFat.class;

    @Rule
    public final TestName testName = new TestName();

    protected static ServerTracker serverTracker = new ServerTracker();

    protected CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();

    @BeforeClass
    public static void commonBeforeClass() throws Exception {
        serverTracker = new ServerTracker();
    }

    @Before
    public void commonBeforeTest() {
        loggingUtils.printMethodName("STARTING TEST CASE: " + testName.getMethodName());
        logTestCaseInServerLogs("STARTING");
    }

    @After
    public void commonAfterTest() {
        loggingUtils.printMethodName("ENDING TEST CASE: " + testName.getMethodName());
        logTestCaseInServerLogs("ENDING");
    }

    @AfterClass
    public static void commonAfterClass() throws Exception {
        serverTracker.stopAllServers();
    }

    public void logTestCaseInServerLogs(String actionToLog) {
        Set<LibertyServer> testServers = serverTracker.getServers();
        for (LibertyServer server : testServers) {
            if (server != null && !server.isStarted()) {
                continue;
            }
            loggingUtils.logTestCaseInServerLog(server, testName.getMethodName(), actionToLog);
            try {
                server.setMarkToEndOfLog(server.getDefaultLogFile());
            } catch (Exception e) {
                Log.error(thisClass, "Failed to set mark to end of default log file for server " + server.getServerName(), e);
            }
        }
    }

}
