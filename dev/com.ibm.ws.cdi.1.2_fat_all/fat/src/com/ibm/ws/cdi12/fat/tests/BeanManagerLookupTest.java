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
