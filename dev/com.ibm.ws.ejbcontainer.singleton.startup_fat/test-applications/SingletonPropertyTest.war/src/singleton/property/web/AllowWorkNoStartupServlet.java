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
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;
import org.junit.Ignore;

import componenttest.app.FATServlet;
import singleton.property.shared.StartupHelperSingleton;
import singleton.property.shared.TestData;

/**
 * Test the behavior of the com.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime and
 * com.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted properties for an application that does
 * not contain any Startup Singleton beans.
 *
 * During application startup of an application that does not contain any Startup Singleton bean, by
 * default, outside work will be blocked. However, if the "blockWorkUnitlAppStarted" property is set
 * to false then outside work will not be blocked
 *
 * The following property values have been set in jvm.options for the server:
 * -Dcom.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime=20
 * -Dcom.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted=false
 */
@SuppressWarnings("serial")
@WebServlet("/AllowWorkNoStartupServlet")
public class AllowWorkNoStartupServlet extends FATServlet {
    private final static String CLASSNAME = AllowWorkNoStartupServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final String JNDI_STARTUP_HELPER = "java:global/SingletonPropertyNoStartup/SingletonPropertyNoStartupBean/NoStartupHelperSingletonBean";

    private StartupHelperSingleton lookupHelper() throws NamingException {
        svLogger.info("lookupHelper : jndi name = " + JNDI_STARTUP_HELPER);
        StartupHelperSingleton helper = (StartupHelperSingleton) new InitialContext().lookup(JNDI_STARTUP_HELPER);
        svLogger.info("lookupHelper : returning : " + ((helper == null) ? helper : helper.getClass().getName()));
        return helper;
    }

    /**
     * This test verifies 2 things. First it will verify, with the blockWorkUntilAppStarted property
     * set to false (disabled), that work will not be blocked since the application does not contain
     * Startup Singletons. And second, it will verify that the postConstruct for non-startup Singleton,
     * with the blockWorkUntilAppStartedWaitTime set to 20 seconds, that the work beans will not run
     * until the bean is first used.
     *
     * To accomplish this the application has a ServletContextListener that uses a
     * java.util.concurrent.CyclicBarrier that allows the processing of the SCL to wait for our test
     * thread to signal it to continue. This allows us to have the application startup processing to
     * be held indefinitely, which allows the test to attempt bean access before startup completes.
     */
    @Test
    @Ignore("com.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted not supported on Liberty") //TODO
    public void testAllowedStartupAllowsClientsWithNoStartupBean() throws Exception {

        svLogger.info("---> Entering testStartupAllowsClients()");

        // Wait until the ServletContextListener is running.
        svLogger.info("--->  Waiting for ServletContextListener initialize");
        TestData.awaitNoStartupBarrier();

        long startTime = System.currentTimeMillis();
        long runTime;

        try {
            try {
                // Look up the helper.  We expect that this call will pass, since work should
                // not not be blocked and post construct on non-startup beans should not have
                // been called.
                svLogger.info("---> Calling lookup that should not block.");
                assertFalse("singleton already started", lookupHelper().isPostConstructRun());
            } catch (Throwable t) {
                // Unexpected exception
                svLogger.info("---> Unexpected exception caught.");
                svLogger.logp(Level.INFO, CLASSNAME, "testStartupAllowsClients", "Ignoring failure: " + t);
                fail("Unexpected exception looking up and calling bean : " + t);
            }

            // Release the hounds!!!!
            startTime = System.currentTimeMillis();
            svLogger.info("--->  Notify ServletContextListener to complete initialization");
            TestData.awaitNoStartupBarrier();

            svLogger.info("---> Calling lookup that wait and then succeed.");
            StartupHelperSingleton helperBean = lookupHelper();
            assertFalse("singleton bean was started", helperBean.isPostConstructRun());
        } finally {
            TestData.setNoStartupBarrierEnabled(false);

            svLogger.info("---> Exiting testStartupAllowsClients()");
        }
    }
}
