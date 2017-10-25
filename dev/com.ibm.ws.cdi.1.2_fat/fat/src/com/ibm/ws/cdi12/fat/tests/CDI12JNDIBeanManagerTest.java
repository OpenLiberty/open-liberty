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

public class CDI12JNDIBeanManagerTest extends LoggingTest {

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

    /**
     * Test bean manager can be looked up via java:comp/BeanManager
     *
     * @throws Exception
     */
    @Test
    public void testHelloWorldJNDIBeanManagerServlet() throws Exception {
        this.verifyResponse("/cdi12helloworldtest/hello", "Hello World CDI 1.2! JNDI BeanManager PASSED! JNDI BeanManager from Servlet PASSED!");
    }

}
