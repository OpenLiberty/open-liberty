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

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import xa.servlets.XAServlet;

@RunWith(FATRunner.class)
public class XATest extends FATServletClient {

    public static final String APP_NAME = "xa";
    public static final String SERVLET_NAME = APP_NAME + "/XAServlet";

    @TestServlet(servlet = XAServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {

        Log.info(XATest.class, "beforeClass", "In BeforeClass of XATest");
        server = LibertyServerFactory.getLibertyServer("transaction_xa");
        setup(server);
    }

    /**
     * @throws CloneNotSupportedException
     * @throws Exception
     */
    protected static void setup(LibertyServer s) throws CloneNotSupportedException, Exception {
        server = s; // For the subclass

        ShrinkHelper.defaultDropinApp(s, APP_NAME, "xa.servlets.*");

        try {
            s.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
            s.startServer();
        } catch (Exception e) {
            Log.error(XATest.class, "setUp", e);
            // Try again
            s.startServer();
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Log.info(XATest.class, "tearDown", "In AfterClass of XATest");
        tearDown(server);
    }

    /**
     * @throws PrivilegedActionException
     */
    protected static void tearDown(LibertyServer s) throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                s.stopServer("WTRN0075W", "WTRN0076W");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    @Test
    public void testSetTransactionTimeoutReturnsTrue() throws Exception {
        server.setMarkToEndOfLog();
        runTest(server, SERVLET_NAME, "testSetTransactionTimeoutReturnsTrue");
        assertNotNull(server.waitForStringInLogUsingMark(XAResourceImpl.class.getCanonicalName() + ".setTransactionTimeout\\([0-9]*\\): TRUE"),
                      "setTransactionTimeout() does not seem to have been called");
    }

    @Test
    public void testSetTransactionTimeoutReturnsFalse() throws Exception {
        server.setMarkToEndOfLog();
        runTest(server, SERVLET_NAME, "testSetTransactionTimeoutReturnsFalse");
        assertNotNull(server.waitForStringInLogUsingMark(XAResourceImpl.class.getCanonicalName() + ".setTransactionTimeout\\([0-9]*\\): FALSE"),
                      "setTransactionTimeout() does not seem to have been called");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testSetTransactionTimeoutThrowsException() throws Exception {
        server.setMarkToEndOfLog();
        runTest(server, SERVLET_NAME, "testSetTransactionTimeoutThrowsException");
        assertNotNull(server.waitForStringInLogUsingMark(XAResourceImpl.class.getCanonicalName() + ".setTransactionTimeout\\([0-9]*\\): javax.transaction.xa.XAException"),
                      "setTransactionTimeout() does not seem to have been called");
    }
}