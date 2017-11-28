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
 * Tests for CDI from shared libraries
 */

public class SharedLibraryTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12SharedLibraryServer");

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
    public void testSharedLibraryNoInjection() throws Exception {
        // Sanity check test, tests that the shared library exists and is available without CDI being involved
        this.verifyResponse("/sharedLibraryNoInjectionApp/noinjection",
                            "Hello from shared library class? :Hello from a non injected class name: Iain");

    }

    @Test
    public void testSharedLibraryWithCDI() throws Exception {

        // Now with CDI
        this.verifyResponse("/sharedLibraryAppWeb1/",
                            "Can i get to HelloC? :Hello from an InjectedHello, I am here: Iain");

    }

}
