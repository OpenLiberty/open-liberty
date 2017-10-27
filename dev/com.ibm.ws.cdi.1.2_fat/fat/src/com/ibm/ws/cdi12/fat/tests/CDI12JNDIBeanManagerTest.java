/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
