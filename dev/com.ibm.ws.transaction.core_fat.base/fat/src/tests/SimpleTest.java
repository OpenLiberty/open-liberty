/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import transaction.servlets.SimpleServlet;

@RunWith(FATRunner.class)
public class SimpleTest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/SimpleServlet";

    @TestServlet(servlet = SimpleServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {

        Log.info(SimpleTest.class, "beforeClass", "In BeforeClass of SimpleTest");
        server = LibertyServerFactory.getLibertyServer("transaction_base");
        setup(server);
    }

    /**
     * @throws CloneNotSupportedException
     * @throws Exception
     */
    protected static void setup(LibertyServer s) throws CloneNotSupportedException, Exception {
        server = s; // For the subclass

        ShrinkHelper.defaultDropinApp(s, APP_NAME, "transaction.servlets.*");

        try {
            s.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
            s.startServer();
        } catch (Exception e) {
            Log.error(SimpleTest.class, "setUp", e);
            // Try again
            s.startServer();
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Log.info(SimpleTest.class, "tearDown", "In AfterClass of SimpleTest");
        tearDown(server);
    }

    /**
     * @throws PrivilegedActionException
     */
    protected static void tearDown(LibertyServer s) throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                s.stopServer("WTRN0017W");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    @Test
    public void testShowPort() throws Exception {
        // Just testing the debug output really
        @SuppressWarnings("unused")
        ProgramOutput startServerExpectFailure = server.startServerExpectFailure("blabla.log", false, false);
    }

    @Test
    public void testLTCAfterGlobalTranAfterConfigUpdate() throws Exception {
        server.setMarkToEndOfLog();

        ServerConfiguration config = server.getServerConfiguration();

        ServerConfiguration originalConfig = config.clone();

        config.getTransaction().setClientInactivityTimeout("0");

        server.updateServerConfiguration(config);

        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));

        runTest(server, SERVLET_NAME, "testLTCAfterGlobalTran");

        server.setMarkToEndOfLog();

        server.updateServerConfiguration(originalConfig);

        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }
}
