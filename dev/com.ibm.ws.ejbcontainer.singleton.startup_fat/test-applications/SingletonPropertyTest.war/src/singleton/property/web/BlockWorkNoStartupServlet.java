/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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

package singleton.property.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import singleton.property.shared.StartupHelperSingleton;
import singleton.property.shared.TestData;

/**
 * Test the behavior of the com.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime and
 * com.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted properties for an application that does
 * not contain any Startup Singleton beans.
 *
 * During application startup of an application that does not contain any Startup Singleton bean, by
 * default, outside work will be blocked. "blockWorkUntilAppStarted" defaults to "true", so outside
 * work will be blocked until the application has completed startup processing or until the
 * "blockWorkUntilAppStartedWaitTime" threshold has been met.
 *
 * The following property values have been set in jvm.options for the server:
 * -Dcom.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime=20
 */
@SuppressWarnings("serial")
@WebServlet("/BlockWorkNoStartupServlet")
public class BlockWorkNoStartupServlet extends FATServlet {
    private static final String CLASSNAME = BlockWorkNoStartupServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long DURATION_FUDGE_FACTOR = 30;

    private static final String JNDI_STARTUP_HELPER = "java:global/SingletonPropertyNoStartup/SingletonPropertyNoStartupBean/NoStartupHelperSingletonBean";

    private StartupHelperSingleton lookupHelper() throws NamingException {
        svLogger.info("lookupHelper : jndi name = " + JNDI_STARTUP_HELPER);
        StartupHelperSingleton helper = (StartupHelperSingleton) new InitialContext().lookup(JNDI_STARTUP_HELPER);
        svLogger.info("lookupHelper : returning : " + ((helper == null) ? helper : helper.getClass().getName()));
        return helper;
    }

    /**
     * This test verifies 3 things. First it will verify, with the blockWorkUntilAppStarted property
     * not set (defaulted), that work will be blocked even though the application does not contain
     * Startup Singletons. Second, it will verify, with the blockWorkUntilAppStartedWaitTime set to
     * 20 seconds, that the work will wait for 20 seconds before an exception is thrown. And third,
     * it will verify, with the blockWorkUntilAppStartedWaitTime set to 20 seconds, that the work
     * will be successful if it can be released to run prior to the threshold.
     *
     * To accomplish this the application has a ServletContextListener that uses a
     * java.util.concurrent.CyclicBarrier that allows the processing of the SCL to wait for our test
     * thread to signal it to continue. This allows us to have the application startup processing to
     * be held indefinitely, which allows the blockWorkUntilAppStartedWaitTime threshold to be exceeded.
     */
    @Test
    public void testDefaultStartupBlocksClientsNoStartupBean() throws Exception {

        svLogger.info("---> Entering testStartupBlocksClients()");

        // Wait until the ServletContextListener is running.
        svLogger.info("--->  Waiting for ServletContextListener initialize");
        TestData.awaitNoStartupBarrier();

        long startTime = System.currentTimeMillis();
        long runTime;

        try {
            try {
                // Look up the helper.  We expect that this call will
                // result in an exception and that the assert will not
                // be processed.
                svLogger.info("---> Calling lookup that should block until exception.");
                assertFalse("singleton already started", lookupHelper().isPostConstructRun());
                svLogger.info("---> Failure... returned from lookup that should have blocked.");
            } catch (Throwable t) {
                // Expected exception
                svLogger.info("---> Expected exception caught.");
                svLogger.logp(Level.INFO, CLASSNAME, "testStartupBlocksClients", "Ignoring failure: " + t);

                runTime = System.currentTimeMillis() - startTime + DURATION_FUDGE_FACTOR;
                svLogger.info("---> runTime = " + runTime);

                // The default wait time has been adjusted to 20 seconds, so ensure we waited at least 20 seconds.
                assertTrue("Lookup failed outside expected time of 20 to 40 seconds.", (runTime >= 20000 && runTime <= 40000));
            }

            // Release the hounds!!!!
            startTime = System.currentTimeMillis();
            svLogger.info("--->  Notify ServletContextListener to complete initialization");
            TestData.awaitNoStartupBarrier();

            svLogger.info("---> Calling lookup that wait and then succeed.");
            StartupHelperSingleton helperBean = lookupHelper();
            assertFalse("singleton bean was started", helperBean.isPostConstructRun());
            assertFalse("war singleton bean was started", helperBean.isWarPostConstructRun());
            runTime = System.currentTimeMillis() - startTime + DURATION_FUDGE_FACTOR;

            svLogger.info("---> Amount of time this thread waited = " + runTime);

            assertTrue("Lookup did not wait for Startup to complete, or waited to long (10 to 25).", (runTime >= 10000 && runTime <= 25000));
        } finally {
            TestData.setNoStartupBarrierEnabled(false);

            svLogger.info("---> Exiting testStartupBlocksClients()");
        }
    }
}
