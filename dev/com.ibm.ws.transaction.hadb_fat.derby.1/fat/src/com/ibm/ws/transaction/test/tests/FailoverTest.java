/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.fail;

import java.util.List;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxFATServletClient;
import com.ibm.ws.transaction.test.FATSuite;

import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class FailoverTest extends TxFATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = "transaction/FailoverServlet";

    protected static final int START_TIMEOUT = 30000;

    public static LibertyServer[] servers;

    public void runInServletAndCheck(LibertyServer server, String path, String method) throws Exception {
        StringBuilder sb = runInServlet(server, path, method);

        check(sb, server, method);
    }

    public void check(StringBuilder sb, LibertyServer server, String method) throws Exception {
        if (!sb.toString().contains(SUCCESS)) {
            server.resetLogMarks();
            List<String> probableFailure = server.findStringsInLogs("WTRN0107W: Caught SQLException when opening SQL RecoveryLog");
            if (probableFailure != null && !probableFailure.isEmpty()) {
                fail(probableFailure.get(0));
            } else {
                fail(method + " did not return " + SUCCESS + ". Returned: " + sb.toString());
            }
        }
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = FATSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    public SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    protected static void commonSetUp() throws Exception {
        FATSuite.beforeSuite();

        for (LibertyServer server : servers) {
            ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transaction.*");
        }
    }

    protected static void commonCleanup() throws Exception {
        // Clean up XA resource files
        servers[0].deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        servers[0].deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }
}
