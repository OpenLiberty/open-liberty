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

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.AllowedFFDC;

public class InjectInjectionPointTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12InjectInjectionPointServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    @AllowedFFDC("com.ibm.ws.container.service.state.StateChangeException")
    public void testInjectInjectionPoint() throws Exception {
        SHARED_SERVER.getLibertyServer().findStringsInLogs("CWWKZ0002E(?=.*injectInjectionPoint)(?=.*com.ibm.ws.container.service.state.StateChangeException)(?=.*javax.enterprise.inject.spi.DefinitionException)(?=.*org.jboss.weld.exceptions.IllegalArgumentException)(?=.*WELD-001405)(?=.*BackedAnnotatedField)(?=.*com.ibm.ws.fat.cdi.injectInjectionPoint.InjectInjectionPointServlet.thisShouldFail)");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore CWWKZ0002E which is an error while starting an application
             */
            SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0002E");
        }
    }

}
