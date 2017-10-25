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

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * These tests verify that you can look up the bean manager as per http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#provider
 */
public class BeanManagerLookupTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12BasicServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testbeanManagerLookup() throws Exception {
        this.verifyResponse("/beanManagerLookupApp/",
                            new String[] { "CDI.current().getBeanManager: true",
                                           "BeanManager from CDI.current().getBeanManager found a Bean." });
    }

    @Test
    public void testbeanManagerLookupJndi() throws Exception {
        this.verifyResponse("/beanManagerLookupApp/",
                            new String[] { "BeanManager from jndi found a Bean.",
                                           "Bean manager from JNDI: true" });
    }

    @Test
    public void testbeanManagerLookupInject() throws Exception {
        this.verifyResponse("/beanManagerLookupApp/",
                            new String[] { "BeanManager from injection found a Bean.",
                                           "Bean manager from inject: true" });
    }
}
