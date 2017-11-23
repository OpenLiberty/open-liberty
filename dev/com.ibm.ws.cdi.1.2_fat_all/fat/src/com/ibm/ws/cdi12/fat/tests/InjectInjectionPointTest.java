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
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.annotation.AllowedFFDC;

public class InjectInjectionPointTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12InjectInjectionPointServer");

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
