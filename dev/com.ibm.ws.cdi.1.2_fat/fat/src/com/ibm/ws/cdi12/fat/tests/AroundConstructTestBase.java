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

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for the <code>@AroundConstruct</code> lifecycle callback, defined in Interceptors 1.2.
 */
public abstract class AroundConstructTestBase extends LoggingTest {

    protected abstract String getServletName();

    protected static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12EJB32Server");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return null;
    }

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Rule
    public final TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("aroundConstructApp").andServlet(getServletName());

    @Test
    public void testBasicAroundConstruct() {}

    /**
     * Test that AroundConstruct works for Non CDI interceptors
     */
    @Test
    public void testNonCdiAroundConstruct() {}

    /**
     * Interceptors 1.2 - "AroundConstruct lifecycle callback interceptor methods may be defined on superclasses of interceptor
     * classes."
     */
    @Test
    public void testAroundConstructInSuperClass() {}

    /**
     * Test that intercepting a constructor annotated with <code>@Inject</code> works.
     */
    @Test
    public void testInjectionConstructorIsCalled() {}

    /**
     * Interceptors 1.2 - "The getConstructor method returns the constructor of the target class for which the AroundConstruct
     * interceptor was invoked."
     */
    @Test
    public void testGetConstructor() {}

    /**
     * Interceptors 1.2 - "The getTarget method returns the associated target instance. For the AroundConstruct lifecycle
     * callback interceptor method, getTarget returns null if called before the proceed method returns."
     */
    @Test
    public void testGetTarget() {}

    /**
     * Test that we can apply an interceptor binding annotation directly to a constructor rather than the class.
     */
    @Test
    public void testBindingInterceptorToConstructor() {}

    /**
     * Interceptors should be called in the correct order as determined by the @Priority annotation and the order declared in the beans.xml
     */
    @Test
    public void testInterceptorOrder() {}

    /**
     * Interceptors should only be called once for each constructor
     */
    @Test
    public void testInterceptorNotCalledTwice() {}

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
