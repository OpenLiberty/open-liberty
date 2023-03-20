/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

package tests;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxFATServletClient;
import com.ibm.ws.transaction.fat.util.TxShrinkHelper;

import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import suite.FATSuite;

public class FailoverTest extends TxFATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/FailoverServlet";
    public static final String APP_PATH = "../../com.ibm.ws.transaction.hadb_fat.derby.1/";

    protected static final int START_TIMEOUT = 30000;

    // This is the server that will be stopped after each test
    protected LibertyServer server;
    protected String[] serverMsgs;

    @After
    public void cleanup() throws Exception {

        if (server != null) {
            if (serverMsgs != null) {
                FATUtils.stopServers(serverMsgs, server);
                serverMsgs = null;
            } else {
                FATUtils.stopServers(server);
            }

            server = null;
        }

        FailoverTest.commonCleanup(this.getClass().getName());
    }

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

    protected static void commonSetUp(String testClassName) throws Exception {
        for (LibertyServer server : LibertyServerFactory.getKnownLibertyServers(testClassName)) {
            TxShrinkHelper.buildDefaultApp(server, APP_NAME, APP_PATH, "web");
        }
    }

    protected static void commonCleanup(String testClassName) throws Exception {
        for (LibertyServer server : LibertyServerFactory.getKnownLibertyServers(testClassName)) {
            // Clean up XA resource files
            server.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

            // Remove tranlog DB
            server.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");

            break;
        }
    }
}
