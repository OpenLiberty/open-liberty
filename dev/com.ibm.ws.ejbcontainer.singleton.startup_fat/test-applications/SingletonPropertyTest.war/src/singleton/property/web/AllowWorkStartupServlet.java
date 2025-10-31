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
 * Test the behavior of the com.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime
 * property for an application that contains Startup Singleton beans.
 *
 * During application startup of an application that contains a Startup Singleton bean,
 * outside work will be blocked. The default time that this work will be blocked before
 * an exception is thrown in 2 minutes. Even if the "blockWorkUnitlAppStarted" property
 * is set to false, outside work will still be blocked if singleton beans exist. However,
 * if the "blockWorkUnitlAppStartedWaitTime" property is set then outside work will be
 * blocked until the application has completed startup processing or until the
 * "blockWorkUntilAppStartedWaitTime" threshold has been met.
 *
 * The following property values have been set in the jvm.options:
 * -Dcom.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime=20
 * -Dcom.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted=false
 */
@SuppressWarnings("serial")
@WebServlet("/AllowWorkStartupServlet")
public class AllowWorkStartupServlet extends FATServlet {
    private static final String CLASSNAME = AllowWorkStartupServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final boolean isMacOSX = System.getProperty("os.name", "unknown").toLowerCase().indexOf("mac os x") >= 0;
    private static final long MAX_BEFORE_START = isMacOSX ? 160000 : 40000;
    private static final long MAX_AFTER_START = isMacOSX ? 145000 : 25000;

    private static final long DURATION_FUDGE_FACTOR = 30;

    private static final String JNDI_STARTUP_HELPER = "java:global/SingletonPropertyStartup/SingletonPropertyStartupBean/StartupHelperSingletonBean";

    private StartupHelperSingleton lookupHelper() throws NamingException {
        svLogger.info("lookupHelper : jndi name = " + JNDI_STARTUP_HELPER);
        StartupHelperSingleton helper = (StartupHelperSingleton) new InitialContext().lookup(JNDI_STARTUP_HELPER);
        svLogger.info("lookupHelper : returning : " + ((helper == null) ? helper : helper.getClass().getName()));
        return helper;
    }

    /**
     * (4.8.1) The container must initialize a startup bean before any client
     * requests are delivered to any clients. Verify that client requests are
     * denied (lookup failure, method block, or method exception) for as long
     * as the startup bean is initializing. In this test the threshold
     * established by the property will expire and an expected
     * exception will be thrown. Calls after initialization completes will
     * then be permitted.
     */
    @Test
    public void testAllowedStartupBlocksClientsWithStartupBean() throws Exception {

        svLogger.info("---> Entering testStartupBlocksClients()");

        // Wait until the startup singleton is running.
        svLogger.info("--->  Waiting for @Startup @Singlton to enter @PostConstruct");
        TestData.awaitStartupBarrier();

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
                assertTrue("Lookup failed outside expected time of 20 to " + (MAX_BEFORE_START / 1000) + " seconds.", (runTime >= 20000 && runTime <= MAX_BEFORE_START));
            }

            // Release the hounds!!!!
            startTime = System.currentTimeMillis();
            svLogger.info("--->  Notify @Startup @Singlton to complete @PostConstruct");
            TestData.awaitStartupBarrier();

            svLogger.info("---> Calling lookup that wait and then succeed.");
            StartupHelperSingleton helperBean = lookupHelper();
            assertTrue("startup bean was not started", helperBean.isPostConstructRun());
            runTime = System.currentTimeMillis() - startTime + DURATION_FUDGE_FACTOR;

            svLogger.info("---> Amount of time this thread waited = " + runTime);

            assertTrue("Lookup did not wait for Startup to complete, or waited to long (10 to " + (MAX_AFTER_START / 1000) + ").",
                       (runTime >= 10000 && runTime <= MAX_AFTER_START));
        } finally {
            TestData.setStartupBarrierEnabled(false);

            svLogger.info("---> Exiting testStartupBlocksClients()");
        }
    }
}
