/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;

public class BeanDiscoveryModeNoneTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12BeanDiscoveryModeNoneServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Mode
    @Test
    @ExpectedFFDC({ "javax.servlet.UnavailableException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testcorrectExceptionThrown() throws Exception {
        this.verifyStatusCode("/beanDiscoveryModeNone/TestServlet", 404);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * Error 404: javax.servlet.UnavailableException: SRVE0319E: For the
             * [com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet] servlet,
             * com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet servlet class
             * was found, but a resource injection failure has occurred. The @Inject
             * java.lang.reflect.Field.testBean1 reference of type com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestBean1
             * for the null component in the beanDiscoveryModeNone.war module of the
             * beanDiscoveryModeNone application cannot be resolved.
             */
            SHARED_SERVER.getLibertyServer().stopServer("SRVE0319E", "CWNEN0035E", "CWOWB1008E");
        }
    }

}
